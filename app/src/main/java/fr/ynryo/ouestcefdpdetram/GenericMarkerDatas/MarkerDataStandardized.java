package fr.ynryo.ouestcefdpdetram.GenericMarkerDatas;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleStop;

public class MarkerDataStandardized {

    // ==================== DONNÉES D'IDENTIFICATION ====================
    private MarkerType markerType;              // Type du véhicule (train ou bus/tram)
    private String id;                          // ID unique (numéro train ou id bus tracker)
    private String lineId;                      // Numéro de ligne (vehicleNumber pour train et lineNumber pour le reste)
    private String networkRef;                  // Référence réseau (ex: "SNCF", "RATP")
    private int networkId;                      // ID numérique du réseau (pour fetch logo)

    // ==================== DONNÉES D'AFFICHAGE ====================
    private String fillColor;                   // Couleur de remplissage du marqueur
    private String textColor;                   // Couleur du texte (numéro de ligne)

    // ==================== POSITION ET DIRECTION ====================
    private double latitude;                    // Latitude actuelle
    private double longitude;                   // Longitude actuelle
    private float bearing;                      // Azimut/direction du véhicule (0-360°)
    private List<Object> markerDataRoute;       // Liste des points du tracé

    // ==================== DONNÉES DE VOYAGE ====================
    private String destination;                 // Destination finale
    private List<MarkerDataStop> stops;         // Liste des arrêts à venir

    // ==================== MÉTADONNÉES ====================
    private boolean isFollowed;                 // Est-ce que l'utilisateur suit ce véhicule?
    private Date createdAt;                     // Quand ce marqueur a été créé
    private Date lastUpdatedAt;                 // Quand la position a été mise à jour
    private boolean detailsLoaded;              // Les infos détaillées (stops) ont-elles été fetched?

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStandardized() {
        this.stops = new ArrayList<>();
        this.createdAt = new Date();
        this.lastUpdatedAt = new Date();
        this.isFollowed = false;
        this.detailsLoaded = false;
    }

    // ==================== BUILDER PATTERN ====================
    public static MarkerDataStandardized from(@NonNull MarkerData markerData, @NonNull MarkerType type) {
        MarkerDataStandardized marker = new MarkerDataStandardized();

        marker.markerType = type;
        marker.id = markerData.getId();
        marker.lineId = marker.isTrain() ? markerData.getVehicleNumber() : markerData.getLineNumber();
        marker.networkRef = markerData.getNetworkRef();
        marker.fillColor = markerData.getFillColor();
        marker.textColor = markerData.getColor();
        marker.latitude = markerData.getPosition().getLatitude();
        marker.longitude = markerData.getPosition().getLongitude();
        marker.bearing = markerData.getPosition().getBearing();
        marker.createdAt = new Date();
        marker.lastUpdatedAt = new Date();
        marker.detailsLoaded = false;

        return marker;
    }

    public static MarkerDataStandardized from(
            @NonNull MarkerData markerData,
            @NonNull VehicleData vehicleData,
            @NonNull MarkerType type) {

        MarkerDataStandardized marker = from(markerData, type);
        marker.setVehicleDetails(vehicleData);
        return marker;
    }

    public void setVehicleDetails(@NonNull VehicleData vehicleData) {
        this.destination = vehicleData.getDestination();
        this.networkId = vehicleData.getNetworkId();

        // Convertir les VehicleStop en MarkerDataStop
        if (vehicleData.getCalls() != null && !vehicleData.getCalls().isEmpty()) {
            this.stops = new ArrayList<>();
            for (int i = 0; i < vehicleData.getCalls().size(); i++) {
                VehicleStop vehicleStop = vehicleData.getCalls().get(i);
                MarkerDataStop stop = new MarkerDataStop();

                stop.setStopRef(vehicleStop.getStopRef());
                stop.setStopName(vehicleStop.getStopName());
                stop.setPlatformName(vehicleStop.getPlatformName());
                stop.setOnLive(vehicleStop.getExpectedTime() != null);
//                stop.setArrivingTime(vehicleStop.getAimedTime() != null ? vehicleStop.getAimedTime() : vehicleStop.getExpectedTime());
                if (stop.isOnLive()) {
                    ZonedDateTime expected = ZonedDateTime.parse(vehicleStop.getExpectedTime());
                    ZonedDateTime aimed = ZonedDateTime.parse(vehicleStop.getAimedTime());
                    Long delay = ChronoUnit.MINUTES.between(aimed, expected);
                    stop.setDelay(delay);
                }
                stop.setDepartureTime(vehicleStop.getExpectedTime() != null ? vehicleStop.getExpectedTime() : vehicleStop.getAimedTime());
                stop.setStopOrder(vehicleStop.getStopOrder());

                if (vehicleStop.getFlags().contains("NO_PICKUP")) {
                    stop.setStopType(StopType.NO_PICKUP);
                } else if (vehicleStop.getFlags().contains("NO_DROPOFF")) {
                    stop.setStopType(StopType.NO_DROPOFF);
                } else {
                    stop.setStopType(StopType.BOTH);
                }

                this.stops.add(stop);
            }
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = new Date();
    }

    // ==================== GETTERS ====================
    public MarkerType getMarkerType() {
        return markerType;
    }

    public String getId() {
        return id;
    }

    public String getLineId() {
        return lineId;
    }

    public String getNetworkRef() {
        return networkRef;
    }

    public int getNetworkId() {
        return networkId;
    }

    public String getFillColor() {
        return fillColor;
    }

    public String getTextColor() {
        return textColor;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getBearing() {
        return bearing;
    }

    public String getDestination() {
        return destination;
    }

    public List<MarkerDataStop> getStops() {
        return stops != null ? stops : new ArrayList<>();
    }

    public boolean isFollowed() {
        return isFollowed;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public List<Object> getMarkerDataRoute() {
        return markerDataRoute;
    }

    public boolean isDetailsLoaded() {
        return detailsLoaded;
    }

    // ==================== SETTERS ====================
    public void setMarkerType(MarkerType markerType) {
        this.markerType = markerType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public void setNetworkRef(String networkRef) {
        this.networkRef = networkRef;
    }

    public void setNetworkId(int networkId) {
        this.networkId = networkId;
    }

    public void setFillColor(String fillColor) {
        this.fillColor = fillColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.lastUpdatedAt = new Date();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.lastUpdatedAt = new Date();
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
        this.lastUpdatedAt = new Date();
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setStops(List<MarkerDataStop> stops) {
        this.stops = stops;
        this.detailsLoaded = (stops != null && !stops.isEmpty());
    }

    public void setFollowed(boolean followed) {
        isFollowed = followed;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdatedAt(Date lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void setDetailsLoaded(boolean detailsLoaded) {
        this.detailsLoaded = detailsLoaded;
    }

    // ==================== MÉTHODES UTILITAIRES ====================
    public boolean isTrain() {
        return markerType == MarkerType.CT_TRAIN;
    }

    public boolean isVehicle() {
        return markerType == MarkerType.BT_VEHICLE;
    }

    @Nullable
    public MarkerDataStop getNextStop() {
        if (stops != null && !stops.isEmpty()) {
            return stops.get(0);
        }
        return null;
    }

    public int getRemainingStopsCount() {
        return stops != null ? stops.size() : 0;
    }

    public void updatePosition(double newLatitude, double newLongitude, float newBearing) {
        this.latitude = newLatitude;
        this.longitude = newLongitude;
        this.bearing = newBearing;
        this.lastUpdatedAt = new Date();
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStandardized{" +
                "markerType=" + markerType +
                ", id='" + id + '\'' +
                ", lineId='" + lineId + '\'' +
                ", networkRef='" + networkRef + '\'' +
                ", destination='" + destination + '\'' +
                ", position=(" + latitude + "," + longitude + ")" +
                ", bearing=" + bearing +
                ", stopsCount=" + getRemainingStopsCount() +
                ", isFollowed=" + isFollowed +
                ", detailsLoaded=" + detailsLoaded +
                '}';
    }
}