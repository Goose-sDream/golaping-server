package com.goosesdream.golaping.common.websocket

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus.*
import com.goosesdream.golaping.session.service.SessionService
import com.goosesdream.golaping.vote.service.VoteService
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.server.HandshakeInterceptor
import java.util.UUID

/**
 * WebSocket 연결 단계에서 handshake validation check, 필요한 정보를 WebSocketSession 속성에 추가
 */
@Component
class WebSocketInterceptor(
    private val sessionService: SessionService,
    private val voteService: VoteService
) : HandshakeInterceptor {

    override fun beforeHandshake( // WebSocket 연결 전 실행되는 HTTP 요청
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        // 쿠키에서 sessionId 추출
        val cookies = (request as? ServletServerHttpRequest)?.servletRequest?.cookies
        val sessionId = cookies?.firstOrNull { it.name == "SESSIONID" }?.value

        // 처음 접속해서 sessionId가 없는 경우
        if (sessionId.isNullOrBlank()) throw BaseException(MISSING_SESSION_ID)

        // voteUuid validation
        val voteUuid = request.uri.path.split("/").lastOrNull()
        if (voteUuid.isNullOrBlank() || !isValidVoteUuid(voteUuid)) throw BaseException(INVALID_VOTE_UUID)

        // vote status
        val isVoteEnded = voteService.checkVoteEnded(voteUuid)

        val nickname = if (!isVoteEnded) { // 투표 진행 중: nickname validation, 종료: nickname 없어도 투표 결과 확인 가능
            sessionService.getNicknameFromSession(sessionId)?: throw BaseException(UNAUTHORIZED)
        } else null

        attributes["sessionId"] = sessionId
        attributes["nickname"] = nickname ?: ""
        attributes["voteUuid"] = voteUuid
        attributes["isVoteEnded"] = isVoteEnded

        return true
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        val session = request.attributes["session"] as? WebSocketSession
        val voteUuid = session?.attributes?.get("voteUuid") as? String

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
