package fr.ynryo.ouestcefdpdetram.apiResponses.markers;

import androidx.annotation.NonNull;

public class MarkerData {
    private String id;
    private String lineNumber;
    private int vehicleNumber;
    private Position position;
    private String fillColor;
    private String color;
    private int networkId;

    public String getId() {
        return id;
    }

    public String getLineNumber() {
        return lineNumber;
    }

    public int getVehicleNumber() {
        return vehicleNumber;
    }

    public Position getPosition() {
        return position;
    }

    public String getFillColor() {
        return fillColor;
    }

    public String getColor() {
        return color;
    }

    public int getNetworkId() {
        return networkId;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerData{" +
                "id='" + id + '\'' +
                ", lineNumber='" + lineNumber + '\'' +
                ", position=" + position +
                ", fillColor='" + fillColor + '\'' +
                ", color='" + color + '\'' +
                ", networkId=" + networkId +
                '}';
    }
}
