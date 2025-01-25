package com.goosesdream.golaping.common.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus
import com.goosesdream.golaping.common.enums.WebSocketResponseStatus.*
import com.goosesdream.golaping.common.exception.WebSocketErrorResponse
import com.goosesdream.golaping.common.websocket.dto.WebSocketInitialResponse
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.stereotype.Component
import org.springframework.web.socket.*

@Component
class GlobalWebSocketHandler(
    private val webSocketManager: WebSocketManager,
    private val objectMapper: ObjectMapper,
    private val voteService: VoteService
) : WebSocketHandler{

    override fun afterConnectionEstablished(session: WebSocketSession) { // WebSocket 연결 성립된 후 실행
        val voteUuid = session.attributes["voteUuid"] as? String

        if (voteUuid != null) {
            val expirationTime = webSocketManager.getChannelExpirationTime(voteUuid)

            if (expirationTime == null || expirationTime <= System.currentTimeMillis()) { // 종료된 투표 또는 유효하지 않는 투표면
                handleErrorResponse(session, EXPIRED_VOTE)
                webSocketManager.stopWebSocketForVote(voteUuid)
                return
            }

            val remainingTimeMillis = expirationTime - System.currentTimeMillis()

            if (webSocketManager.restoreWebSocketSession(voteUuid) == null) { // 기존 세션이 없는 경우(첫 접속 유저) 타이머 설정 및 채널 초기화
                if (remainingTimeMillis > 0) {
                    webSocketManager.setWebSocketTimer(voteUuid, remainingTimeMillis)
                    webSocketManager.startWebSocketForVote(voteUuid, (remainingTimeMillis / 1000 / 60).toInt())
                } else {
                    handleErrorResponse(session, EXPIRED_VOTE)
                    return
                }
            }

            // 새로운 세션 저장
            webSocketManager.saveWebSocketSession(voteUuid, session)

            val voteLimit = voteService.getVoteLimit(voteUuid)
            val initialWebSocketResponse = WebSocketInitialResponse(
                voteLimit = voteLimit,
                voteEndTime = expirationTime
            )
            sendResponse(session, WebSocketResponse("연결에 성공했습니다.", initialWebSocketResponse))
        } else {
            handleErrorResponse(session, INVALID_VOTE_UUID)
            return
        }
    }

    private fun sendResponse(session: WebSocketSession, response: Any) {
        val jsonResponse = objectMapper.writeValueAsString(response)
        session.sendMessage(TextMessage(jsonResponse))
    }

    private fun handleErrorResponse(session: WebSocketSession, errorStatus: WebSocketResponseStatus) {
        sendResponse(session, WebSocketErrorResponse.fromStatus(errorStatus))
        session.close()
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        println("Message received: ${message.payload}")
        // TODO: Redis 세션이 없는 경우, 닉네임 기반으로 Vote 테이블에서 해당 유저의 기록을 조회해 복원
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val voteUuid = session.attributes["voteUuid"] as? String
        if (voteUuid != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        val voteUuid = session.attributes["voteUuid"] as? String
        if (voteUuid != null) {
            webSocketManager.stopWebSocketForVote(voteUuid)
        }
    }

    override fun supportsPartialMessages(): Boolean = false
}
