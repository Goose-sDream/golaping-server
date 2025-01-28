package com.goosesdream.golaping.common.websocket

import com.goosesdream.golaping.common.constants.Status.Companion.ACTIVE
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.stereotype.Controller
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus.*
import com.goosesdream.golaping.common.exception.WebSocketErrorResponse
import com.goosesdream.golaping.common.websocket.dto.AddVoteOptionRequest
import com.goosesdream.golaping.common.websocket.dto.VoteRequest
import com.goosesdream.golaping.common.websocket.dto.WebSocketInitialResponse
import com.goosesdream.golaping.common.websocket.dto.WebSocketRequest
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.annotation.SendToUser

@Controller
class VoteWebSocketController(
    private val webSocketManager: WebSocketManager,
    private val voteService: VoteService
) {

    // WebSocket 연결 후 실행
    @MessageMapping("/vote/connect")
    @SendToUser("/queue/initialResponse") // 사용자 별로 응답 전송
    fun connectToVote(session: SimpMessageHeaderAccessor, message: WebSocketRequest): WebSocketResponse<Any> {
        val voteUuid = message.voteUuid ?: throw IllegalArgumentException("INVALID_VOTE_UUID")
        val expirationTime = webSocketManager.getChannelExpirationTime(voteUuid) ?: throw IllegalStateException("EXPIRED_VOTE")

        if (expirationTime <= System.currentTimeMillis()) {
            webSocketManager.stopWebSocketForVote(voteUuid)
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
        val previousVotes = voteService.getCurrentVoteCounts(voteUuid, nickname)

        val initialWebSocketResponse = WebSocketInitialResponse(
            voteLimit,
            expirationTime,
            webSocketSessionId,
            previousVotes
        )
        return WebSocketResponse("연결에 성공했습니다.", initialWebSocketResponse)
    }

    // 투표 옵션 추가
    @MessageMapping("/vote/{voteUuid}/addOption")
    @SendTo("/topic/vote/{voteUuid}/addOption")
    fun handleAddOption(headers: SimpMessageHeaderAccessor, message: AddVoteOptionRequest): WebSocketResponse<Any> {
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")
        val voteUuid = message.voteUuid ?: throw IllegalArgumentException("MISSING_VOTE_UUID")

        val newOption = voteService.addOption(voteUuid, nickname, message.optionText, message.optionColor)
        return WebSocketResponse("새로운 옵션이 추가되었습니다.", newOption)
    }

    // 투표/투표취소
    @MessageMapping("/vote/{voteUuid}")
    @SendTo("/topic/vote/{voteUuid}")
    fun handleVoteToggle(headers: SimpMessageHeaderAccessor, message: VoteRequest): WebSocketResponse<Any> {
        val nickname = headers.sessionAttributes?.get("nickname") as? String ?: throw IllegalArgumentException("MISSING_NICKNAME")
        val voteUuid = message.voteUuid ?: throw IllegalArgumentException("MISSING_VOTE_UUID")
        val selectedOptionId = message.optionId ?: throw IllegalArgumentException("MISSING_SELECTED_OPTION")

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

            val vote = voteService.getVote(voteUuid) ?: throw IllegalStateException("VOTE_NOT_FOUND")
            val voteOption = voteService.getVoteOption(selectedOptionId).orElseThrow { IllegalStateException("VOTE_OPTION_NOT_FOUND") }
            voteService.vote(vote, nickname, voteOption)
        }
        // TODO: 해당 투표의 생성자 여부 같이 반환

        val updatedVoteCounts = voteService.getCurrentVoteCounts(voteUuid, nickname)
        return WebSocketResponse("투표가 완료되었습니다.", updatedVoteCounts)
    }

    private fun validateVoteCountLimit(voteUuid: String, nickname: String) {
        val userVotes = voteService.getUserVoteOptionIds(voteUuid, nickname)
        val userVoteLimit = voteService.getVoteLimit(voteUuid)
        if (userVotes.size >= userVoteLimit)
            throw IllegalStateException("USER_VOTE_LIMIT_EXCEEDED")
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
        val voteUuid = session.sessionId
        if (voteUuid != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
        }
    }

    // WebSocket 연결 종료 후 처리
    @MessageMapping("/vote/disconnect")
    fun afterConnectionClosed(session: SimpMessageHeaderAccessor) {
        val voteUuid = session.sessionId
        if (voteUuid != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
        }
    }
}

