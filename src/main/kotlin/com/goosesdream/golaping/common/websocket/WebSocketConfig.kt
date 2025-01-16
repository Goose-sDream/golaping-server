package com.goosesdream.golaping.common.websocket

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val webSocketInterceptor: WebSocketInterceptor,
    private val globalWebSocketHandler: WebSocketHandler
): WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(globalWebSocketHandler, "/votes/{voteUuid}")
            .setAllowedOrigins("*")
            .addInterceptors(webSocketInterceptor)
    }
}