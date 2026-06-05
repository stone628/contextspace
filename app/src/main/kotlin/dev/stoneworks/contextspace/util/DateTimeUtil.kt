package dev.stoneworks.contextspace.util

import java.sql.Date
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object DateTimeUtil {
    fun now() = LocalDateTime.now(ZoneOffset.UTC)

    fun toInstant(dt: LocalDateTime) = dt.toInstant(ZoneOffset.UTC)
    fun fromInstant(i: Instant) = LocalDateTime.ofInstant(i, ZoneOffset.UTC)

    fun toDate(dt: LocalDateTime) = Date.from(dt.toInstant(ZoneOffset.UTC))
    fun fromDate(dt: Date) = LocalDateTime.ofInstant(dt.toInstant(), ZoneOffset.UTC)

    fun toEpochSecond(dt: LocalDateTime) = dt.toEpochSecond(ZoneOffset.UTC)
    fun fromEpochSecond(sec: Long) = LocalDateTime.ofInstant(Instant.ofEpochSecond(sec), ZoneOffset.UTC)

    fun toEpochMillis(dt: LocalDateTime) = dt.toInstant(ZoneOffset.UTC).toEpochMilli()
    fun fromEpochMillis(millis: Long) = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC)
}