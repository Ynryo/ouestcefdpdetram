package fr.ynryo.ouestcefdpdetram.apiResponses.vehicle;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.Position;

public class VehicleData {
    private String id;
    private int lineId;
    private String direction;
    private String destination;
    private List<Call> calls;
    private Position position;
    private int networkId;
    private String serviceDate;
    private String updatedAt;
    private Context context;

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

    @NonNull
    @Override
    public String toString() {
        return "VehicleDetails{" +
                "id='" + id + '\'' +
                ", lineId=" + lineId +
                ", direction='" + direction + '\'' +
                ", destination='" + destination + '\'' +
                ", calls=" + calls +
                ", position=" + position +
                ", networkId=" + networkId +
                ", serviceDate='" + serviceDate + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", context=" + context +
                '}';
    }
}
