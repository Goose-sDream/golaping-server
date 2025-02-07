package com.goosesdream.golaping.websocket.controller

import com.goosesdream.golaping.common.constants.Status.Companion.ACTIVE
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus.*
import com.goosesdream.golaping.common.exception.WebSocketErrorResponse
import com.goosesdream.golaping.websocket.dto.WebSocketResponse
import com.goosesdream.golaping.websocket.dto.AddVoteOptionRequest
import com.goosesdream.golaping.websocket.dto.VoteRequest
import com.goosesdream.golaping.websocket.dto.WebSocketInitialResponse
import com.goosesdream.golaping.websocket.service.WebSocketManager
import com.goosesdream.golaping.vote.dto.VoteResultResponse
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SendToUser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Controller
class VoteWebSocketController(
    private val webSocketManager: WebSocketManager,
    private val voteService: VoteService) {

    // WebSocket 연결 후 실행
    @MessageMapping("/vote/{voteUuid}/connect")
    @SendToUser("/queue/initialResponse") // 사용자 별로 응답 전송
    fun connectToVote(
        @DestinationVariable voteUuid: String,
        session: SimpMessageHeaderAccessor): WebSocketResponse<Any> {
        val expirationTime = webSocketManager.getChannelExpirationTime(voteUuid) ?: throw IllegalStateException("EXPIRED_VOTE")
        val expirationDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(expirationTime), ZoneId.of("Asia/Seoul"))

        if (expirationTime <= System.currentTimeMillis()) {
            webSocketManager.stopWebSocketForVote(voteUuid)
            webSocketManager.sendUserDisconnectMessage(voteUuid)
            throw IllegalStateException("EXPIRED_VOTE")
        }

        val remainingTimeMillis = expirationTime - System.currentTimeMillis()
        webSocketManager.setWebSocketTimer(voteUuid, remainingTimeMillis)
        webSocketManager.startWebSocketForVote(voteUuid, (remainingTimeMillis / 1000 / 60).toInt())

        val nickname = session.sessionAttributes?.get("nickname") as? String
            ?: throw IllegalStateException("MISSING_NICKNAME")

        val webSocketSessionId = session.sessionId ?: throw IllegalStateException("MISSING_WEBSOCKET_SESSION_ID")
        webSocketManager.saveWebSocketSession(voteUuid, webSocketSessionId)

        val voteLimit = voteService.getVoteLimit(voteUuid)
        val previousVotes = voteService.getPreviousVoteData(voteUuid, nickname)

        val initialWebSocketResponse = WebSocketInitialResponse(
            voteLimit,
            expirationDateTime,
            webSocketSessionId,
            previousVotes
        )
        return WebSocketResponse("연결에 성공했습니다.", initialWebSocketResponse)
    }

    // 투표 옵션 추가
    @MessageMapping("/vote/{voteUuid}/addOption")
    @SendTo("/topic/vote/{voteUuid}/addOption")
    fun handleAddOption(
        @DestinationVariable voteUuid: String,
        headers: SimpMessageHeaderAccessor,
        message: AddVoteOptionRequest
    ): WebSocketResponse<Any> {
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")

        val newOption = voteService.addOption(voteUuid, nickname, message.optionText, message.optionColor)
        return WebSocketResponse("새로운 옵션이 추가되었습니다.", newOption)
    }

    // 투표/투표취소
    @MessageMapping("/vote/{voteUuid}")
    @SendTo("/topic/vote/{voteUuid}")
    fun handleVoteToggle(
        @DestinationVariable voteUuid: String,
        headers: SimpMessageHeaderAccessor,
        message: VoteRequest
    ): WebSocketResponse<Any> {
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")
        val selectedOptionId = message.optionId ?: throw IllegalArgumentException("MISSING_SELECTED_OPTION")

        val vote = voteService.getVote(voteUuid) ?: throw IllegalStateException("VOTE_NOT_FOUND")
        val isUnlimited = vote.userVoteLimit == null

        if (isUnlimited) { // 무제한 투표
            val voteOption = voteService.getVoteOption(selectedOptionId).orElseThrow { IllegalStateException("VOTE_OPTION_NOT_FOUND") }
            voteService.vote(vote, nickname, voteOption)
        } else { // 제한 투표 - 투표/투표취소
            val userVote = voteService.getUserVote(voteUuid, nickname, selectedOptionId)
            if (userVote != null) { // 해당 옵션에 이미 투표한 경우
                if (userVote.status == ACTIVE) {
                    voteService.deactivateVote(userVote)
                } else {
                    validateVoteCountLimit(voteUuid, nickname)
                    voteService.activateVote(userVote)
                }
            } else { // 처음 투표하는 경우
                validateVoteCountLimit(voteUuid, nickname)
                val voteOption = voteService.getVoteOption(selectedOptionId).orElseThrow { IllegalStateException("VOTE_OPTION_NOT_FOUND") }
                voteService.vote(vote, nickname, voteOption)
            }
        }
        val updatedVoteCounts = voteService.getCurrentVoteCounts(voteUuid, nickname)
        return WebSocketResponse("투표가 완료되었습니다.", updatedVoteCounts)
    }

    private fun validateVoteCountLimit(voteUuid: String, nickname: String) {
        val userVotes = voteService.getUserVoteOptionIds(voteUuid, nickname)
        val userVoteLimit = voteService.getVoteLimit(voteUuid)

        if (userVoteLimit != null && userVotes.size >= userVoteLimit)
            throw IllegalStateException("USER_VOTE_LIMIT_EXCEEDED")
    }

    // [생성자] 투표 제한 시간 도달 전 미리 투표 종료
    @MessageMapping("/vote/{voteUuid}/close")
    @SendTo("/topic/vote/{voteUuid}/closed")
    fun closeVote(
        @DestinationVariable voteUuid: String,
        headers: SimpMessageHeaderAccessor): WebSocketResponse<Any> {
        val vote = voteService.getVote(voteUuid) ?: throw IllegalStateException("VOTE_NOT_FOUND")
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")

        val voteResults = voteService.closeVote(vote, nickname)
        webSocketManager.stopWebSocketForVote(voteUuid)

        val broadcastMessage = VoteResultResponse(vote.title, voteResults)
        webSocketManager.broadcastVoteClosed(voteUuid, broadcastMessage)

        return WebSocketResponse("투표가 종료되었습니다.", broadcastMessage)
    }

    // 공통 예외 처리 핸들러
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    fun handleException(e: Exception): WebSocketErrorResponse {
        return when (e) {
            is IllegalArgumentException -> {
                when (e.message) {
                    "INVALID_VOTE_UUID" -> WebSocketErrorResponse.fromStatus(INVALID_VOTE_UUID)
                    "MISSING_NICKNAME" -> WebSocketErrorResponse.fromStatus(MISSING_NICKNAME)
                    "MISSING_VOTE_UUID" -> WebSocketErrorResponse.fromStatus(MISSING_VOTE_UUID)
                    "MISSING_SELECTED_OPTION" -> WebSocketErrorResponse.fromStatus(MISSING_SELECTED_OPTION)
                    else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
                }
            }
            is IllegalStateException -> {
                when (e.message) {
                    "EXPIRED_VOTE" -> WebSocketErrorResponse.fromStatus(EXPIRED_VOTE)
                    "MISSING_WEBSOCKET_SESSION_ID" -> WebSocketErrorResponse.fromStatus(MISSING_WEBSOCKET_SESSION_ID)
                    "MISSING_NICKNAME" -> WebSocketErrorResponse.fromStatus(MISSING_NICKNAME)
                    "VOTE_NOT_FOUND" -> WebSocketErrorResponse.fromStatus(VOTE_NOT_FOUND)
                    "VOTE_OPTION_NOT_FOUND" -> WebSocketErrorResponse.fromStatus(VOTE_OPTION_NOT_FOUND)
                    "USER_VOTE_LIMIT_EXCEEDED" -> WebSocketErrorResponse.fromStatus(USER_VOTE_LIMIT_EXCEEDED)
                    else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
                }
            }
            else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
        }
    }

    // WebSocket 세션 오류 처리
    @MessageMapping("/vote/transportError")
    fun handleTransportError(session: SimpMessageHeaderAccessor, exception: Throwable) {
        val webSocketSessionId = session.sessionId
        val voteUuid = session.sessionAttributes?.get("voteUuid") as? String
        if (webSocketSessionId != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
            webSocketManager.sendUserDisconnectMessage(webSocketSessionId)
        }
    }

    // WebSocket 연결 종료 후 처리
    @MessageMapping("/vote/disconnect")
    fun afterConnectionClosed(session: SimpMessageHeaderAccessor) {
        val webSocketSessionId = session.sessionId
        val voteUuid = session.sessionAttributes?.get("voteUuid") as? String
        if (webSocketSessionId != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
            webSocketManager.sendUserDisconnectMessage(webSocketSessionId)
        }
    }
}

