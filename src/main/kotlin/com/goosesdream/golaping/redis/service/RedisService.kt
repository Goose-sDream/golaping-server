package com.goosesdream.golaping.redis.service

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RedisService(
    private val redisTemplate: RedisTemplate<String, String>
) {
    fun save(key: String, value: String, expirationTime: Long) {
        redisTemplate.opsForValue().set(key, value, expirationTime, TimeUnit.SECONDS)
    }

    fun get(key: String): String? {
        return redisTemplate.opsForValue().get(key)
    }

    fun delete(key: String) {
        redisTemplate.delete(key)
    }

    fun exists(key: String): Boolean {
        return redisTemplate.hasKey(key)
    }
}
