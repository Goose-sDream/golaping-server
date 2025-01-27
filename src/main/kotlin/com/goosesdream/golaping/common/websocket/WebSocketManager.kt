package com.goosesdream.golaping.common.websocket

import com.goosesdream.golaping.redis.service.RedisService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class WebSocketManager(
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisService: RedisService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val webSocketTimers = mutableMapOf<String, Timer>() // 타이머 관리
    private val webSocketSessions = mutableMapOf<String, String>()  // 메모리 내 웹소켓 세션 관리

    private val voteExpirationPrefix = "vote:expiration:"
    private val voteSessionPrefix = "vote:session:"

    // 투표 UUID에 대한 WebSocket 세션 시작
    fun startWebSocketForVote(voteUuid: String, timeLimit: Int) {
        val expirationTime = getChannelExpirationTime(voteUuid)

        if (expirationTime != null) {
            val remainingTimeMillis = expirationTime - System.currentTimeMillis()

            if (remainingTimeMillis > 0) {
                setWebSocketTimer(voteUuid, remainingTimeMillis) // 타이머 설정
            } else { // 이미 만료된 경우 바로 종료 처리
                stopWebSocketForVote(voteUuid)
            }
        } else {
            stopWebSocketForVote(voteUuid)
        }
    }

    // WebSocket 타이머 설정
    fun setWebSocketTimer(voteUuid: String, remainingTimeMillis: Long) {
        if (webSocketTimers.containsKey(voteUuid)) {
            return // 이미 타이머가 설정되어 있으면 중복 생성 방지
        }

        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                stopWebSocketForVote(voteUuid)
            }
        }, remainingTimeMillis)

        webSocketTimers[voteUuid] = timer
    }

    // 채널 종료
    fun stopWebSocketForVote(voteUuid: String) {
        redisService.delete(voteSessionPrefix + voteUuid)

        webSocketTimers[voteUuid]?.cancel()
        webSocketTimers.remove(voteUuid)

        val webSocketSessionId = webSocketSessions.remove(voteUuid)

        if (webSocketSessionId != null) {
            sendDisconnectMessage(webSocketSessionId)  // 연결 종료 메시지 전송
        }
    }

    // 세션 종료를 위한 메시지 전송
    fun sendDisconnectMessage(webSocketSessionId: String) {
        try {
            messagingTemplate.convertAndSend("/topic/vote/$webSocketSessionId", "투표 세션이 만료되어 웹소켓 연결이 끊어졌습니다.")
            messagingTemplate.convertAndSend("/topic/vote/$webSocketSessionId", "close")
        } catch (e: Exception) {
            println("연결 종료 메세지 전송 실패: ${e.message}")
        }
    }

    // 채널 만료 시간 조회
    fun getChannelExpirationTime(voteUuid: String): Long? {
        val redisKey = voteExpirationPrefix + voteUuid
        val ttlInSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS)

        return if (ttlInSeconds > 0) {
            System.currentTimeMillis() + ttlInSeconds * 1000L
        } else {
            null
        }
    }

    // 세션 Id 저장
    fun saveWebSocketSession(voteUuid: String, sessionId: String) {
        val expirationTime = getChannelExpirationTime(voteUuid)

        val ttlInSeconds = expirationTime?.let {
            (it - System.currentTimeMillis()) / 1000  // 남은 시간을 초 단위로
        }

        ttlInSeconds?.let {
            redisService.save(voteSessionPrefix + voteUuid, sessionId, it)
        }
        webSocketSessions[voteUuid] = sessionId
    }
}
