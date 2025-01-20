package com.goosesdream.golaping.session.service

import com.goosesdream.golaping.redis.service.RedisService
import org.springframework.stereotype.Service

@Service
class SessionService(
    private val redisService: RedisService,
) {
    private val nicknamePrefix = "session:nickname:"

    fun saveCreatorNicknameToSession(sessionId: String, nickname: String, timeLimit: Int) {
        val redisKey = nicknamePrefix + sessionId
        val ttlInSeconds = timeLimit * 60L
        redisService.save(redisKey, nickname, ttlInSeconds)
    }

    fun saveNicknameToSession(sessionId: String, nickname: String, timeLimit: Int) {
        val redisKey = nicknamePrefix + sessionId
        val ttlInSeconds = timeLimit * 60L
        redisService.save(redisKey, nickname, ttlInSeconds)
    }

    fun getNicknameFromSession(sessionId: String): String? {
        val redisKey = nicknamePrefix + sessionId
        return redisService.get(redisKey)
    }

    fun deleteSession(sessionId: String) {
        val redisKey = nicknamePrefix + sessionId
        redisService.delete(redisKey)
    }
}