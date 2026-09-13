package com.example.echowithin.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeUtils {

    private val fullDateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy, h:mm a", Locale.getDefault())
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    /**
     * Parses an ISO-8601 string to a java.time.Instant.
     * Supports:
     * - UTC Zulu ("2026-09-13T10:14:22Z")
     * - Microsecond precision ("2026-09-13T10:14:22.123456Z" or "...123456+00:00")
     * - Millisecond precision ("2026-09-13T10:14:22.123Z")
     * - Timezone offsets ("2026-09-13T10:14:22+03:00")
     * - Naive strings ("2026-09-13 10:14:22" or "2026-09-13T10:14:22"), interpreting as UTC.
     */
    fun parseIsoToInstant(raw: String?): Instant? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        // 1. Fast path for standard Instant strings
        try {
            if (trimmed.endsWith("Z", ignoreCase = true)) {
                return Instant.parse(trimmed)
            }
        } catch (_: Exception) {}

        // 2. Parse using ISO_DATE_TIME
        try {
            val accessor = DateTimeFormatter.ISO_DATE_TIME.parseBest(
                trimmed,
                Instant::from,
                OffsetDateTime::from,
                ZonedDateTime::from,
                LocalDateTime::from
            )
            return when (accessor) {
                is Instant -> accessor
                is OffsetDateTime -> accessor.toInstant()
                is ZonedDateTime -> accessor.toInstant()
                is LocalDateTime -> accessor.atZone(ZoneId.of("UTC")).toInstant()
                else -> null
            }
        } catch (_: Exception) {}

        // 3. Fallback normalization
        try {
            var normalized = trimmed.replace(" ", "T")
            if (!normalized.endsWith("Z") && !normalized.contains("+") && !Regex("-\\d{2}:?\\d{2}$").containsMatchIn(normalized)) {
                normalized += "Z"
            }
            return Instant.parse(normalized)
        } catch (_: Exception) {}

        return null
    }

    /**
     * Converts an ISO timestamp string into a ZonedDateTime in the device's default timezone.
     */
    fun toLocalZonedDateTime(raw: String?): ZonedDateTime? {
        val instant = parseIsoToInstant(raw) ?: return null
        return instant.atZone(ZoneId.systemDefault())
    }

    /**
     * Formats an ISO string into full date & time in local timezone:
     * e.g. "Sep 13, 2026, 1:14 PM"
     */
    fun formatFullDateTime(raw: String?): String {
        val local = toLocalZonedDateTime(raw) ?: return raw.orEmpty().take(16).replace("T", " ")
        return local.format(fullDateTimeFormatter)
    }

    /**
     * Formats an ISO string into local date:
     * e.g. "Sep 13, 2026"
     */
    fun formatDate(raw: String?): String {
        val local = toLocalZonedDateTime(raw) ?: return raw.orEmpty().take(10)
        return local.format(dateFormatter)
    }

    /**
     * Formats an ISO string into local time:
     * e.g. "1:14 PM"
     */
    fun formatTime(raw: String?): String {
        val local = toLocalZonedDateTime(raw) ?: return ""
        return local.format(timeFormatter)
    }

    /**
     * Formats an ISO string into relative time string:
     * "Just now", "5m ago", "2h ago", "Yesterday", "4d ago", or local date format "Sep 13, 2026"
     */
    fun formatRelativeTime(raw: String?): String {
        val instant = parseIsoToInstant(raw) ?: return raw.orEmpty().take(10)
        val now = Instant.now()
        val diffMs = now.toEpochMilli() - instant.toEpochMilli()
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        return when {
            diffMs < 0 -> "Just now"
            diffSec < 60 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHour < 24 -> "${diffHour}h ago"
            diffDay == 1L -> "Yesterday"
            diffDay < 7L -> "${diffDay}d ago"
            else -> formatDate(raw)
        }
    }

    /**
     * Returns the current time in UTC formatted as strict ISO-8601 string ending with 'Z'.
     */
    fun nowUtcIso(): String = Instant.now().toString()
}
