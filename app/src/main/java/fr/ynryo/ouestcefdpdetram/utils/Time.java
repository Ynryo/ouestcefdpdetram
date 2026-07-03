package fr.ynryo.ouestcefdpdetram.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class Time {
    // ==================== MÉTHODES UTILITAIRES ====================
    @Nullable
    public static LocalTime parseToLocalTime(@Nullable String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        try {
            // Format ISO 8601 avec timezone (Z, +, ou -)
            if (timeString.contains("T")) {
                try {
                    return ZonedDateTime.parse(timeString).toLocalTime();
                } catch (Exception e) {
                    return LocalDateTime.parse(timeString).toLocalTime();
                }
            }

            // Format simple HH:mm:ss
            if (timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }

            // Format simple HH:mm
            if (timeString.matches("\\d{2}:\\d{2}")) {
                return LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
            }

        } catch (Exception e) {
            return null;
        }

        return null;
    }

    @NonNull
    public static String formatLocalTime(@Nullable LocalTime time) {
        if (time == null) {
            return "—";
        }
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @NonNull
    public static String formatDuration(@Nullable Long minutes) {
        if (minutes == null || minutes < 0) {
            return "—";
        }

        if (minutes == 0) {
            return "0m";
        }

        return minutes + "m";
    }
}
