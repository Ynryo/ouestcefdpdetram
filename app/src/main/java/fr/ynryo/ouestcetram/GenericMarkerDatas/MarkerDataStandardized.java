package fr.ynryo.ouestcetram.GenericMarkerDatas;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcetram.apiResponsesPOJO.markers.MarkerData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.train.TrainData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.train.TrainFeature;
import fr.ynryo.ouestcetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.vehicle.VehicleStop;

public class MarkerDataStandardized {

    // ==================== DONNÉES D'IDENTIFICATION ====================
    private MarkerType markerType; // Type du véhicule (train ou bus/tram)
    private String id; // ID unique (numéro train ou id bus tracker)
    private int lineId; // Numéro de ligne (vehicleNumber pour train et lineNumber pour le reste)
    private String lineNumber; // Numéro de ligne pour l'affichage
    private String networkRef; // Référence réseau (ex: "SNCF", "RATP")
    private int networkId; // ID numérique du réseau (pour fetch logo)

    // ==================== DONNÉES D'AFFICHAGE ====================
    private String fillColor; // Couleur de remplissage du marqueur
    private String textColor; // Couleur du texte (numéro de ligne)

    // ==================== POSITION ET DIRECTION ====================
    private double latitude; // Latitude actuelle
    private double longitude; // Longitude actuelle
    private float bearing; // Azimut/direction du véhicule (0-360°)
    private String pathRef;
    private Object markerDataRoute; // Liste des points du tracé

    // ==================== DONNÉES DE VOYAGE ====================
    private String destination; // Destination finale
    private List<MarkerDataStop> stops; // Liste des arrêts à venir

    // ==================== MÉTADONNÉES ====================
    private boolean isFollowed; // Est-ce que l'utilisateur suit ce véhicule?
    private Instant createdAt; // Quand ce marqueur a été créé
    private Instant lastUpdatedAt; // Quand la position a été mise à jour
    private boolean detailsLoaded; // Les infos détaillées (stops) ont-elles été fetched?

    private final static String TAG = "MarkerDataStandardized";
    private final static int NETWORK_ID_SNCF = 17;

    // ==================== CONSTRUCTEURS ====================
    public MarkerDataStandardized() {
        this.stops = new ArrayList<>();
        this.createdAt = Instant.now();
        this.lastUpdatedAt = Instant.now();
        this.isFollowed = false;
        this.detailsLoaded = false;
    }

    // ==================== CONVERSION ====================
    public static MarkerDataStandardized from(@NonNull MarkerData markerData, @NonNull MarkerType type) {
        MarkerDataStandardized marker = new MarkerDataStandardized();

        marker.markerType = type;
        marker.id = markerData.getId();
        marker.lineId = marker.isTrain() ? Integer.parseInt(markerData.getVehicleNumber()) : 0;
        marker.lineNumber = marker.isTrain() ? markerData.getVehicleNumber() : markerData.getLineNumber();
        marker.networkRef = markerData.getNetworkRef();
        marker.fillColor = markerData.getFillColor();
        marker.textColor = markerData.getColor();
        marker.latitude = markerData.getPosition().getLatitude();
        marker.longitude = markerData.getPosition().getLongitude();
        marker.bearing = markerData.getPosition().getBearing();
        marker.createdAt = Instant.now();
        marker.lastUpdatedAt = Instant.now();
        marker.detailsLoaded = false;

        return marker;
    }

    public static MarkerDataStandardized from(@NonNull MarkerData markerData, @NonNull VehicleData vehicleData, @NonNull MarkerType type) {
        MarkerDataStandardized marker = from(markerData, type);
        marker.setVehicleDetailsVehicleData(vehicleData);
        return marker;
    }

    // à la priorité sur les datas (bus tracker api)
    public void setVehicleDetailsVehicleData(@NonNull VehicleData vehicleData) {
        this.lineId = vehicleData.getLineId();
        this.destination = vehicleData.getDestination();
        this.networkId = vehicleData.getNetworkId();
        this.pathRef = vehicleData.getPathRef();

        if (vehicleData.getCalls() != null && !vehicleData.getCalls().isEmpty()) {
            this.stops = new ArrayList<>();
            for (int i = 0; i < vehicleData.getCalls().size(); i++) {
                VehicleStop vehicleStop = vehicleData.getCalls().get(i);
                MarkerDataStop stop = new MarkerDataStop();

                stop.setStopRef(vehicleStop.getStopRef());
                stop.setStopName(vehicleStop.getStopName());
                stop.setPlatformName(vehicleStop.getPlatformName());
                stop.setOnLive(vehicleStop.getExpectedTime() != null);
                if (stop.isOnLive()) {
                    ZonedDateTime expected = ZonedDateTime.parse(vehicleStop.getExpectedTime());
                    ZonedDateTime aimed = ZonedDateTime.parse(vehicleStop.getAimedTime());
                    Long delay = ChronoUnit.MINUTES.between(aimed, expected);
                    stop.setDelay(delay);
                }
                stop.setDepartureTime(vehicleStop.getExpectedTime() != null ? vehicleStop.getExpectedTime() : vehicleStop.getAimedTime());
                stop.setStopOrder(vehicleStop.getStopOrder());
                stop.setLongitude(vehicleStop.getLongitude());
                stop.setLatitude(vehicleStop.getLatitude());
                stop.setDistanceTraveled(vehicleStop.getDistanceTraveled());
                stop.setVehicle(this);

                if (vehicleStop.getFlags().contains("NO_PICKUP")) {
                    stop.setStopType(StopType.NO_PICKUP);
                } else if (vehicleStop.getFlags().contains("NO_DROPOFF")) {
                    stop.setStopType(StopType.NO_DROPOFF);
                } else {
                    stop.setStopType(StopType.BOTH);
                }
                Log.i(TAG, stop.toString());
                this.stops.add(stop);
            }
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = Instant.now();
    }

    // n'a pas la priorité, complete juste (trains)
    public void setVehicleDetailsTrainData(@NonNull TrainData trainData) throws ParseException {
        List<TrainFeature> trainFeatureList = trainData.getRouteFeatures();
        if (trainFeatureList != null && !trainFeatureList.isEmpty()) {
            this.destination = trainData.getDestination();
            this.networkId = NETWORK_ID_SNCF;
            this.stops = new ArrayList<>();
            for (int i = 0; i < trainFeatureList.size(); i++) {
                TrainFeature trainFeature = trainFeatureList.get(i);

                if (trainFeature.getProperties().isRoute()) {
                    this.markerDataRoute = trainFeature.getRouteGeometry().getCoordinates();
                    continue;
                }
                if (!trainFeature.getProperties().isStop()) continue; // si c'est pas un arrêt c'est oust

                MarkerDataStop stop = new MarkerDataStop();
                try {
                    stop.setStopOrder(trainFeature.getProperties().getEtape());
                    stop.setStopRef(trainFeature.getProperties().getUic());
                    stop.setStopName(trainFeature.getProperties().getLocalite());
//                stop.setPlatformName(trainFeature.getPlatformName()); // TODO: à ajouter avec l'api carto tchoo guestplatform
                    stop.setOnLive(true);
                    stop.setDelay((long) trainFeature.getProperties().getDelay());
                    stop.setArrivalTime(trainFeature.getProperties().getDebut().toString());
                    stop.setDepartureTime(trainFeature.getProperties().getFin().toString());
                    stop.setDestinationStop(trainData.isDestinationStop(trainFeature.getProperties().getLocalite()));
                    stop.setDepartureStop(trainData.isDepartureStop(trainFeature.getProperties().getLocalite()));
                    stop.setVehicle(this);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                Log.i(TAG, stop.toString());
                this.stops.add(stop);
            }
        }

        this.detailsLoaded = true;
        this.lastUpdatedAt = Instant.now();
    }

    // ==================== GETTERS ====================
    public MarkerType getMarkerType() {
        return markerType;
    }

    public String getId() {
        return id;
    }

    public int getLineId() {
        return lineId;
    }

    public String getLineNumber() {
        return lineNumber;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public String getPathRef() {
        return pathRef;
    }

    public Object getMarkerDataRoute() {
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

    public void setLineId(int lineId) {
        this.lineId = lineId;
    }

    public void setLineNumber(String lineNumber) {
        this.lineNumber = lineNumber;
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
        this.lastUpdatedAt = Instant.now();
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.lastUpdatedAt = Instant.now();
    }

    public void setBearing(float bearing) {
        this.bearing = bearing;
        this.lastUpdatedAt = Instant.now();
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public void setDetailsLoaded(boolean detailsLoaded) {
        this.detailsLoaded = detailsLoaded;
    }

    public void setMarkerDataRoute(Object markerDataRoute) {
        this.markerDataRoute = markerDataRoute;
    }

    // ==================== MÉTHODES UTILITAIRES ====================
    public boolean isTrain() {
        return markerType == MarkerType.TRAIN;
    }

    public boolean isVehicle() {
        return markerType == MarkerType.BUS_TRAM;
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
        this.lastUpdatedAt = Instant.now();
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataStandardized{" +
                "markerType=" + markerType +
                ", id='" + id + '\'' +
                ", lineId='" + lineId + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                ", networkRef='" + networkRef + '\'' +
                ", networkId=" + networkId +
                ", fillColor='" + fillColor + '\'' +
                ", textColor='" + textColor + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", bearing=" + bearing +
                ", markerDataRoute=" + markerDataRoute +
                ", destination='" + destination + '\'' +
                ", stops=" + stops +
                ", isFollowed=" + isFollowed +
                ", createdAt=" + createdAt +
                ", lastUpdatedAt=" + lastUpdatedAt +
                ", detailsLoaded=" + detailsLoaded +
                '}';
    }
}