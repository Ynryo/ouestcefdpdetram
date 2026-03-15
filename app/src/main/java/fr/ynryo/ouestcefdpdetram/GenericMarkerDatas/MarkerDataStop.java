package fr.ynryo.ouestcefdpdetram.GenericMarkerDatas;

import android.graphics.Color;

import androidx.annotation.NonNull;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MarkerDataStop {
    private String stopRef;              // Identifiant unique de l'arrêt
    private String stopName;             // Nom de l'arrêt
    private String platformName;         // Quai/Platform (ex: "A3", "Voie 2")
    private String arrivingTime;         // Heure d'arrivée (format normalisé)
    private String departureTime;        // Heure de départ (format normalisé)
    private long atStopDurationMs;       // Durée d'arrêt en millisecondes
    private Long delay;                  // Retard/décalage par rapport à l'horaire prévu
    private StopType stopType;           // Type d'arrêt (PICKUP, DROPOFF)
    private int stopOrder;               // Position dans la liste des arrêts (0, 1, 2, ...)
    private boolean isOnLive;           // Statut de l'appel (EXPECTED, ACTUAL, etc.)

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStop() {
        this.atStopDurationMs = 0;
        this.stopType = StopType.BOTH;
    }

    public MarkerDataStop(String stopRef, String stopName, String arrivingTime, String departureTime) {
        this.stopRef = stopRef;
        this.stopName = stopName;
        this.arrivingTime = arrivingTime;
        this.departureTime = departureTime;
        this.atStopDurationMs = calculateAtStopDuration();
        this.stopType = StopType.BOTH;
    }

    // ==================== GETTERS ====================
    public String getStopRef() {
        return stopRef;
    }

    public String getStopName() {
        return stopName;
    }

    public String getPlatformName() {
        return platformName;
    }

    public String getArrivingTime() {
        return arrivingTime;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public long getAtStopDurationMs() {
        if (atStopDurationMs == 0 && arrivingTime != null && departureTime != null) {
            atStopDurationMs = calculateAtStopDuration();
        }
        return atStopDurationMs;
    }

    public Long getDelay() {
        return delay;
    }

    public StopType getStopType() {
        return stopType;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    // ==================== SETTERS ====================

    public void setStopRef(String stopRef) {
        this.stopRef = stopRef;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }

    public void setArrivingTime(String arrivingTime) {
        this.arrivingTime = arrivingTime;
        this.atStopDurationMs = calculateAtStopDuration();
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
        this.atStopDurationMs = calculateAtStopDuration();
    }

    public void setAtStopDurationMs(long durationMs) {
        this.atStopDurationMs = durationMs;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public void setStopType(StopType stopType) {
        this.stopType = stopType;
    }

    public void setStopOrder(int stopOrder) {
        this.stopOrder = stopOrder;
    }

    public void setOnLive(boolean onLive) {
        this.isOnLive = onLive;
    }

    // ==================== MÉTHODES UTILITAIRES ====================
    private long calculateAtStopDuration() {
        if (arrivingTime == null || departureTime == null) {
            return 0;
        }

        try {
            long arrivalMs = parseTimeToMillis(arrivingTime);
            long departMs = parseTimeToMillis(departureTime);

            if (arrivalMs > 0 && departMs > 0) {
                return Math.max(0, departMs - arrivalMs);
            }
        } catch (Exception e) {
            // Silencieusement, on retourne 0 si le parsing échoue
        }

        return 0;
    }

    private long parseTimeToMillis(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }

        // Format simple HH:mm:ss (trains)
        if (timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
            try {
                LocalTime time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                return time.toSecondOfDay() * 1000L;
            } catch (Exception e) {
                return 0;
            }
        }

        // Format ISO 8601 (bus) - 2024-03-14T14:30:00Z ou 2024-03-14T14:30:00+02:00
        if (timeString.contains("T")) {
            try {
                // Extraire juste la partie HH:mm:ss
                String timeOnly = timeString.split("T")[1].split("Z|\\+|-")[0];
                LocalTime time = LocalTime.parse(timeOnly, DateTimeFormatter.ofPattern("HH:mm:ss"));
                return time.toSecondOfDay() * 1000L;
            } catch (Exception e) {
                return 0;
            }
        }

        return 0;
    }

    public String getAtStopDurationFormatted() {
        long durationMs = getAtStopDurationMs();
        if (durationMs == 0) {
            return "—";
        }

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public String getDelayText() {
        if (delay == null) {
            return "";
        }

        if (delay == 0) {
            return "À l'heure ✓";
        }

        if (delay > 0) {
            return "Retard " + delay + " min";
        } else {
            return "Avance " + Math.abs(delay) + " min";
        }
    }

    public int getDelayColor() {
        if (delay == null) {
            return Color.GRAY;
        }

        if (delay == 0) {
            return Color.rgb(15, 150, 40);  // Vert
        }

        if (delay <= 5) {
            return Color.rgb(224, 159, 7);  // Orange clair
        }

        if (delay <= 15) {
            return Color.rgb(224, 112, 7);  // Orange foncé
        }

        return Color.RED;
    }

    public boolean canPickup() {
        return stopType == StopType.NO_DROPOFF;
    }

    public boolean canDropoff() {
        return stopType == StopType.NO_PICKUP;
    }

    public boolean isLate() {
        return delay != null && delay > 0;
    }

    public boolean isEarly() {
        return delay != null && delay < 0;
    }

    public boolean isOnTime() {
        return delay != null && delay == 0;
    }

    public boolean isOnLive() {
        return isOnLive;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStop{" +
                "stopName='" + stopName + '\'' +
                ", arrivingTime='" + arrivingTime + '\'' +
                ", departureTime='" + departureTime + '\'' +
                ", atStopDurationMs=" + atStopDurationMs +
                ", stopType=" + stopType +
                '}';
    }
}