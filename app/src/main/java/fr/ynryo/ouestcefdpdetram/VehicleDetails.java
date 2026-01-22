package fr.ynryo.ouestcefdpdetram;

import java.util.List;

public class VehicleDetails {
    private String id;
    private int lineId;
    private String direction;
    private String destination;
    private List<Call> calls; // Correspond à la clé "calls" du JSON
    private Position position; // Réutilisation de la classe Position existante
    private int networkId;
    private String serviceDate;
    private String updatedAt;

    public String getId() {
        return id;
    }

    public int getLineId() {
        return lineId;
    }

    public String getDirection() {
        return direction;
    }

    public String getDestination() {
        return destination;
    }

    public List<Call> getCalls() {
        return calls;
    }

    public Position getPosition() {
        return position;
    }

    public int getNetworkId() {
        return networkId;
    }

    public String getServiceDate() {
        return serviceDate;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }
}
