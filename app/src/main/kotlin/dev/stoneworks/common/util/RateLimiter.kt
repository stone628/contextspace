package dev.stoneworks.common.util

import dev.stoneworks.contextspace.RedisConfig

object RateLimiter {
    fun isLimited(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val conn = RedisConfig.connection() ?: return false
        val sync = conn.sync()
        val count = sync.incr(key)
        if (count == 1L) {
            sync.expire(key, windowSeconds)
        }
        return count > maxRequests
    }
}
