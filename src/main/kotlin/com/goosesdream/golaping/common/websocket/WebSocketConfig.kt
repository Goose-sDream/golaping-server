package com.goosesdream.golaping.common.websocket

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompSessionHandler
import org.springframework.web.socket.config.annotation.*

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketInterceptor: WebSocketInterceptor
): WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws/votes/{voteUuid}")
            .setAllowedOrigins("http://localhost:3300", "http://localhost:8080", "http://golping.site", "http://golping.site/")
            .addInterceptors(webSocketInterceptor)
            .withSockJS()
    }

    override fun configureMessageBroker(configurer: MessageBrokerRegistry) {
        configurer.enableSimpleBroker("/topic") // 클라이언트가 구독할 경로
        configurer.setApplicationDestinationPrefixes("/app") // 클라이언트가 요청할 경로
    }
}