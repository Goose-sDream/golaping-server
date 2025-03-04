package com.goosesdream.golaping.websocket.controller

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.constants.Status.Companion.ACTIVE
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus.*
import com.goosesdream.golaping.common.exception.WebSocketErrorResponse
import com.goosesdream.golaping.common.util.logger
import com.goosesdream.golaping.websocket.service.WebSocketManager
import com.goosesdream.golaping.vote.dto.VoteResultResponse
import com.goosesdream.golaping.vote.service.VoteService
import com.goosesdream.golaping.websocket.dto.*
import com.goosesdream.golaping.websocket.dto.addOption.AddVoteOptionRequest
import com.goosesdream.golaping.websocket.dto.voteToggle.VoteRequest
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.annotation.SendToUser
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Controller
class VoteWebSocketController(
    private val webSocketManager: WebSocketManager,
    private val voteService: VoteService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = logger()

    // WebSocket 연결 후 실행
    @MessageMapping("/vote/connect")
    fun connectToVote(
        headers: SimpMessageHeaderAccessor): WebSocketResponse<Any> {
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String ?: throw IllegalStateException("MISSING_VOTE_UUID")
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalStateException("MISSING_NICKNAME")

        // CustomHandshakeHandler에서 설정한 principal로부터 sessionId 추출
        val principal = headers.user ?: throw IllegalStateException("MISSING_PRINCIPAL")

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

        val webSocketSessionId = headers.sessionId ?: throw IllegalStateException("MISSING_WEBSOCKET_SESSION_ID")
        webSocketManager.saveWebSocketSession(voteUuid, webSocketSessionId)

        val voteLimit = voteService.getVoteLimit(voteUuid)
        val previousVotes = voteService.getPreviousVoteData(voteUuid, nickname)

        val initialWebSocketResponse = WebSocketInitialResponse(
            voteLimit,
            expirationDateTime,
            webSocketSessionId,
            previousVotes
        )

        // 여러 탭/브라우저에서 메세지를 동일하게 받도록
        messagingTemplate.convertAndSendToUser(principal.name, "/queue/initialResponse", initialWebSocketResponse)
        return WebSocketResponse("연결에 성공했습니다.", initialWebSocketResponse)
    }

    // 투표 옵션 추가
    @MessageMapping("/vote/addOption")
    fun handleAddOption(
        headers: SimpMessageHeaderAccessor,
        message: AddVoteOptionRequest
    ) {
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String ?: throw IllegalStateException("MISSING_VOTE_UUID")
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")

        val newOption = voteService.addOption(voteUuid, nickname, message.optionText, message.optionColor)

        messagingTemplate.convertAndSend("/topic/vote/$voteUuid/addOption", newOption)
    }

    // 투표/투표취소
    @MessageMapping("/vote")
    fun handleVoteToggle(
        headers: SimpMessageHeaderAccessor,
        message: VoteRequest
    ): WebSocketResponse<Any> {
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String ?: throw IllegalStateException("MISSING_VOTE_UUID")
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
        val updatedVoteDataForUser = voteService.getChangedVoteOption(voteUuid, nickname, selectedOptionId)
        val updatedVoteDataForBroadcast = voteService.getChangedVoteOptionForBroadcast(voteUuid, selectedOptionId)

        messagingTemplate.convertAndSend("/topic/vote/$voteUuid", updatedVoteDataForBroadcast)

        return WebSocketResponse("투표가 완료되었습니다.", updatedVoteDataForUser)
    }

    private fun validateVoteCountLimit(voteUuid: String, nickname: String) {
        val userVotes = voteService.getUserVoteOptionIds(voteUuid, nickname)
        val userVoteLimit = voteService.getVoteLimit(voteUuid)

        if (userVoteLimit != null && userVotes.size >= userVoteLimit)
            throw IllegalStateException("USER_VOTE_LIMIT_EXCEEDED")
    }

    // [생성자] 투표 제한 시간 도달 전 미리 투표 종료
    @MessageMapping("/vote/close")
    fun closeVote(
        headers: SimpMessageHeaderAccessor): WebSocketResponse<Any> {
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String ?: throw IllegalStateException("MISSING_VOTE_UUID")
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")

        val vote = voteService.getVote(voteUuid) ?: throw IllegalStateException("VOTE_NOT_FOUND")

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
        log.error("WebSocket error: {} - {}", e.javaClass.name, e.message, e)

        return when (e) {
            is IllegalArgumentException -> {
                when (e.message) {
                    "INVALID_VOTE_UUID" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.INVALID_VOTE_UUID)
                    "MISSING_NICKNAME" -> WebSocketErrorResponse.fromStatus(MISSING_NICKNAME)
                    "MISSING_VOTE_UUID" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.MISSING_VOTE_UUID)
                    "MISSING_SELECTED_OPTION" -> WebSocketErrorResponse.fromStatus(MISSING_SELECTED_OPTION)
                    else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
                }
            }
            is IllegalStateException -> {
                when (e.message) {
                    "EXPIRED_VOTE" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.EXPIRED_VOTE)
                    "MISSING_WEBSOCKET_SESSION_ID" -> WebSocketErrorResponse.fromStatus(MISSING_WEBSOCKET_SESSION_ID)
                    "MISSING_NICKNAME" -> WebSocketErrorResponse.fromStatus(MISSING_NICKNAME)
                    "MISSING_VOTE_UUID" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.MISSING_VOTE_UUID)
                    "MISSING_PRINCIPAL" -> WebSocketErrorResponse.fromStatus(MISSING_PRINCIPAL)
                    "VOTE_NOT_FOUND" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.VOTE_NOT_FOUND)
                    "VOTE_OPTION_NOT_FOUND" -> WebSocketErrorResponse.fromStatus(WebSocketResponseStatus.VOTE_OPTION_NOT_FOUND)
                    "USER_VOTE_LIMIT_EXCEEDED" -> WebSocketErrorResponse.fromStatus(USER_VOTE_LIMIT_EXCEEDED)
                    else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
                }
            }
            is BaseException -> {
                WebSocketErrorResponse.fromBaseStatus(e.status)
            }
            else -> WebSocketErrorResponse.fromStatus(GENERAL_ERROR)
        }
    }

    // WebSocket 세션 오류 처리
    @MessageMapping("/vote/transportError")
    fun handleTransportError(headers: SimpMessageHeaderAccessor, exception: Throwable) {
        val webSocketSessionId = headers.sessionId
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String
        val sessionId = headers.sessionAttributes?.get("sessionId") as? String

        if (webSocketSessionId != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
            webSocketManager.sendUserDisconnectMessage(webSocketSessionId)

            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/disconnect",
                    WebSocketResponse("DISCONNECTED: 연결이 끊어졌습니다.")
                )
            }
        }
    }

    // WebSocket 연결 종료 후 처리
    @MessageMapping("/vote/disconnect")
    fun afterConnectionClosed(headers: SimpMessageHeaderAccessor) {
        val webSocketSessionId = headers.sessionId
        val voteUuid = headers.sessionAttributes?.get("voteUuid") as? String
        val sessionId = headers.sessionAttributes?.get("sessionId") as? String

        if (webSocketSessionId != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
            webSocketManager.sendUserDisconnectMessage(webSocketSessionId)

            if (sessionId != null) {
                messagingTemplate.convertAndSendToUser(
                    sessionId,
                    "/queue/disconnect",
                    WebSocketResponse("CLOSED: 연결이 종료되었습니다.")
                )
            }
        }
    }
}

