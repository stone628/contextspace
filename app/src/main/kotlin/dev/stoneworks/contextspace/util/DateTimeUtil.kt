package dev.stoneworks.contextspace.util

import java.time.LocalDateTime
import java.time.ZoneOffset

object DateTimeUtil {
    fun now() = LocalDateTime.now(ZoneOffset.UTC)
}