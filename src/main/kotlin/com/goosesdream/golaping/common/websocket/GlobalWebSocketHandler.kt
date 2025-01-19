package com.goosesdream.golaping.common.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession

@Component
class GlobalWebSocketHandler(
    private val webSocketManager: WebSocketManager
) : WebSocketHandler{

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val voteUuid = session.attributes["voteUuid"] as? String

        if (voteUuid != null) {
            var expirationTime = webSocketManager.getChannelExpirationTime(voteUuid)

            if (expirationTime != null) {
                val remainingTimeMillis = expirationTime - System.currentTimeMillis()

                // 세션이 없으면(처음 접속하는 유저) 새로운 타이머 설정
                val existingSession = webSocketManager.restoreWebSocketSession(voteUuid)
                if (existingSession == null) {
                    if (remainingTimeMillis > 0) {
                        webSocketManager.setWebSocketTimer(voteUuid, remainingTimeMillis)
                    } else {
                        webSocketManager.stopWebSocketForVote(voteUuid)
                    }
                }
            } else { // 이미 종료된 투표 또는 유효하지 않은 상태인 경우
                webSocketManager.stopWebSocketForVote(voteUuid)
            }

            // 세션이 없으면(처음 접속하는 유저) 새로운 채널 시작
            val existingSession = webSocketManager.restoreWebSocketSession(voteUuid)
            if (existingSession == null) {
                expirationTime = webSocketManager.getChannelExpirationTime(voteUuid)
                val timeLimit = expirationTime?.let { // 투표 남은 시간만큼 웹소켓 세션 timeLimit 설정
                    val remainingTimeMillis = it - System.currentTimeMillis()
                    if (remainingTimeMillis > 0) {
                        (remainingTimeMillis / 1000 / 60).toInt()
                    } else {
                        return@let null
                    }
                }

                if (timeLimit == null) {
                    webSocketManager.stopWebSocketForVote(voteUuid)
                } else { // 새로운 채널 시작
                    webSocketManager.startWebSocketForVote(voteUuid, timeLimit)
                }
            }
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
