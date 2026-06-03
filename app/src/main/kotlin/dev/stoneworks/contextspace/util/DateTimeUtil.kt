package dev.stoneworks.contextspace.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object DateTimeUtil {
    fun now() = LocalDateTime.now(ZoneOffset.UTC)

    fun toEpochSecond(dt: LocalDateTime) = dt.toEpochSecond(ZoneOffset.UTC)
    fun fromEpochSecond(sec: Long) = LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneOffset.UTC)

    fun toEpochMillis(dt: LocalDateTime) = dt.toInstant(ZoneOffset.UTC).toEpochMilli()
    fun fromEpochMillis(millis: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
}