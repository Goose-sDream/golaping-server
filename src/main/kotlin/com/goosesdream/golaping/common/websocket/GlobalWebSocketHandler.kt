package com.goosesdream.golaping.common.websocket

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession

@Component
class GlobalWebSocketHandler(
    private val webSocketManager: WebSocketManager,
    private val objectMapper: ObjectMapper
) : WebSocketHandler{

    override fun afterConnectionEstablished(session: WebSocketSession) { // WebSocket 연결 성립된 후 실행
        val voteUuid = session.attributes["voteUuid"] as? String

        if (voteUuid != null) {
            val expirationTime = webSocketManager.getChannelExpirationTime(voteUuid)

            if (expirationTime == null || expirationTime <= System.currentTimeMillis()) { // 종료된 투표 또는 유효하지 않는 투표면
                webSocketManager.stopWebSocketForVote(voteUuid)
                return
            }

            val remainingTimeMillis = expirationTime - System.currentTimeMillis()

            if (webSocketManager.restoreWebSocketSession(voteUuid) == null) { // 기존 세션이 없는 경우(첫 접속 유저) 타이머 설정 및 채널 초기화
                webSocketManager.setWebSocketTimer(voteUuid, remainingTimeMillis)
                webSocketManager.startWebSocketForVote(voteUuid, (remainingTimeMillis / 1000 / 60).toInt())
            }

            // 새로운 세션 저장
            webSocketManager.saveWebSocketSession(voteUuid, session)
        }
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        println("Message received: ${message.payload}")
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
