package com.goosesdream.golaping.common.websocket

import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession

@Component
class GlobalWebSocketHandler : WebSocketHandler {

    private val sessions = mutableMapOf<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {

        val voteUuid = session.attributes["voteUuid"] as? String
        val nickname = session.attributes["nickname"] as? String // 투표 종료 상태면 empty string
        val isVoteEnded = session.attributes["isVoteEnded"] as? Boolean

        if (voteUuid != null) {
            sessions[voteUuid] = session
            println("New session for vote: $voteUuid, Nickname: $nickname, VoteEnded: $isVoteEnded")
        }
    }

    override fun handleMessage(session: WebSocketSession, message: WebSocketMessage<*>) {
        println("Message received: ${message.payload}")
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        val voteUuid = session.attributes["voteUuid"] as? String
        if (voteUuid != null) {
            sessions.remove(voteUuid)
            println("Transport error for vote: $voteUuid")
        }
        println("Error occurred: ${exception.message}")
        exception.printStackTrace()
    }

    override fun afterConnectionClosed(session: WebSocketSession, closeStatus: CloseStatus) {
        val voteUuid = session.attributes["voteUuid"] as? String
        if (voteUuid != null) {
            sessions.remove(voteUuid)
            println("Session closed for vote: $voteUuid")
        }
    }

    override fun supportsPartialMessages(): Boolean = false
}
