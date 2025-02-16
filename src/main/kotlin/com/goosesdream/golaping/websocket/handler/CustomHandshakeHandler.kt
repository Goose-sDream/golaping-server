package com.goosesdream.golaping.websocket.handler

import com.goosesdream.golaping.common.base.BaseException
import com.goosesdream.golaping.common.enums.BaseResponseStatus.MISSING_SESSION_ID
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal

@Component
class CustomHandshakeHandler : DefaultHandshakeHandler() {

    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal {

        // 쿠키에서 sessionId 추출, 쿠키가 없으면 웹소켓 연결 거부
        val cookies = (request as? ServletServerHttpRequest)?.servletRequest?.cookies
        val sessionId = cookies?.firstOrNull { it.name == "sessionId" }?.value
            ?.takeIf { it.isNotBlank() }
            ?: throw BaseException(MISSING_SESSION_ID)

        return StompPrincipal(sessionId)
    }
}

// Principal 구현체
data class StompPrincipal(
    private val name: String
) : Principal {
    override fun getName(): String = name
}
