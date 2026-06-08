package dev.stoneworks.common.util

import dev.stoneworks.common.component.RedisConfig

object RateLimiter {
    fun isLimited(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val conn = RedisConfig.writeConnection() ?: return false
        val sync = conn.sync()
        return try {
            val count = sync.incr(key)
            if (count == 1L) {
                sync.expire(key, windowSeconds)
            }
            count > maxRequests
        } catch (_: Exception) {
            false
        }
    }
}
