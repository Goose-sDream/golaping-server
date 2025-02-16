package com.goosesdream.golaping.websocket.interceptor

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID

/**
 * WebSocket 연결 단계에서 handshake validation check, 필요한 정보를 WebSocketSession 속성에 추가
 */
@Component
class WebSocketInterceptor(
    private val sessionService: SessionService,
    private val voteService: VoteService) : HandshakeInterceptor {

    override fun beforeHandshake( // WebSocket 연결 전 실행되는 HTTP 요청
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any?>
    ): Boolean {
        val cookies = (request as? ServletServerHttpRequest)?.servletRequest?.cookies

        val sessionId = cookies?.firstOrNull { it.name == "sessionId" }?.value
            ?.takeIf { it.isNotBlank() }
            ?: throw BaseException(MISSING_SESSION_ID)

        val voteUuid = cookies.firstOrNull { it.name == "voteUuid" }?.value
            ?.takeIf { it.isNotBlank() && isValidVoteUuid(it) }
            ?: throw BaseException(MISSING_VOTE_UUID)

        val isVoteEnded = voteService.checkVoteEnded(voteUuid)

        val nickname = if (!isVoteEnded) {
            sessionService.getNicknameFromSession(sessionId) ?: throw BaseException(UNAUTHORIZED)
        } else null

        attributes["sessionId"] = sessionId
        attributes["voteUuid"] = voteUuid
        attributes["nickname"] = nickname
        attributes["isVoteEnded"] = isVoteEnded

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        val voteUuid = request.attributes["voteUuid"] as? String
        println("Handshake completed for voteUuid: $voteUuid")
    }

    // UUID validation
    private fun isValidVoteUuid(voteUuid: String): Boolean {
        return try {
            UUID.fromString(voteUuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
