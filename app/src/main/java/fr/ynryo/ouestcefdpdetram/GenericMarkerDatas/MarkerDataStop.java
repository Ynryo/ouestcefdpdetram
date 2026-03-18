package fr.ynryo.ouestcefdpdetram.GenericMarkerDatas;

import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MarkerDataStop {
    private String stopRef;              // Identifiant unique de l'arrêt
    private String stopName;             // Nom de l'arrêt
    private String platformName;                // Quai/Platform (ex: "A3", "Voie 2")
    private String arrivalTimeRaw;             // Heure d'arrivée (format brut - stockée pour flexibilité)
    private String departureTimeRaw;            // Heure de départ (format brut)
    private long atStopDurationMs;       // Durée d'arrêt en millisecondes TODO: REMOVE ce truc kk
    private Long delay;                  // Retard/décalage par rapport à l'horaire prévu
    private StopType stopType;           // Type d'arrêt (PICKUP, DROPOFF)
    private int stopOrder;               // Position dans la liste des arrêts (0, 1, 2, ...)
    private boolean isOnLive;           // Statut de l'appel (EXPECTED, ACTUAL, etc.)
    private boolean isDestinationStop = false;
    private boolean isDepartureStop = false;
    private MarkerDataStandardized vehicle; // Véhicle parent

    private final static String TAG = "MarkerDataStop";

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStop() {
        this.atStopDurationMs = 0;
        this.stopType = StopType.BOTH;
    }

    public MarkerDataStop(String stopRef, String stopName, String arrivingTime, String departureTime) {
        this.stopRef = stopRef;
        this.stopName = stopName;
        this.arrivalTimeRaw = arrivingTime;
        this.departureTimeRaw = departureTime;
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

    @Nullable
    public LocalTime getArrivalTime() {
        return parseToLocalTime(arrivalTimeRaw);
    }

    @Nullable
    public LocalTime getDepartureTime() {
        return parseToLocalTime(departureTimeRaw);
    }

    @Nullable
    public Long getAtStopTime() {
        LocalTime arrival = getArrivalTime();
        LocalTime departure = getDepartureTime();

        if (arrival == null || departure == null) {
            return null;
        }

        return ChronoUnit.MINUTES.between(arrival, departure);
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

    public MarkerDataStandardized getVehicle() {
        return vehicle;
    }

    public boolean isOnLive() {
        return isOnLive;
    }

    public boolean isDepartureStop() {
        return isDepartureStop;
    }

    public boolean isDestinationStop() {
        return isDestinationStop;
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

    public void setArrivalTime(String timeString) {
        this.arrivalTimeRaw = timeString;
    }

    public void setDepartureTime(String timeString) {
        this.departureTimeRaw = timeString;
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

    public void setDestinationStop(boolean isDestinationStop) {
        this.isDestinationStop = isDestinationStop;
    }

    public void setDepartureStop(boolean isDepartureStop) {
        this.isDepartureStop = isDepartureStop;
    }

    public void setVehicle(MarkerDataStandardized markerDataStandardized) {
        this.vehicle = markerDataStandardized;
    }

    // ==================== MÉTHODES UTILITAIRES ====================
    @Nullable
    private static LocalTime parseToLocalTime(@Nullable String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        try {
            // Format simple HH:mm:ss
            if (timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
                return LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }

            // Format ISO 8601 avec timezone (Z, +, ou -)
            if (timeString.contains("T")) {
                // Parse comme ZonedDateTime puis extrait juste le LocalTime
                ZonedDateTime zdt = ZonedDateTime.parse(timeString);
                return zdt.toLocalTime();
            }

        } catch (Exception e) {
            // Silencieusement, retourne null si parsing échoue
            return null;
        }

        return null;
    }

    @NonNull
    private static String formatLocalTime(@Nullable LocalTime time) {
        if (time == null) {
            return "—";
        }
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @NonNull
    private static String formatDuration(@Nullable Long minutes) {
        if (minutes == null || minutes < 0) {
            return "—";
        }

        if (minutes == 0) {
            return "0m";
        }

        return minutes + "m";
    }

    public String getDelayText() {
        if (delay == null) {
            return "";
        }

        if (delay == 0) {
            return "À l'heure";
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

    public boolean cantDropoff() {
        return stopType == StopType.NO_DROPOFF;
    }

    public boolean cantPickup() {
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

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStop{" +
                "stopRef='" + stopRef + '\'' +
                ", stopName='" + stopName + '\'' +
                ", platformName='" + platformName + '\'' +
                ", arrivalTimeRaw='" + arrivalTimeRaw + '\'' +
                ", departureTimeRaw='" + departureTimeRaw + '\'' +
                ", arrivalTime='" + getArrivalTime() + '\'' +
                ", departureTime='" + getDepartureTime() + '\'' +
                ", atStopDurationMs=" + atStopDurationMs +
                ", delay=" + delay +
                ", stopType=" + stopType +
                ", stopOrder=" + stopOrder +
                ", isOnLive=" + isOnLive +
                ", isDestinationStop=" + isDestinationStop +
                ", isDepartureStop=" + isDepartureStop +
                '}';
    }
}