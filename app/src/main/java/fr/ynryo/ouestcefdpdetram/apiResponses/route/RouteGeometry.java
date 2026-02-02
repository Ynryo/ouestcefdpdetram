package fr.ynryo.ouestcefdpdetram.apiResponses.route;

import androidx.annotation.NonNull;

public class RouteGeometry {
    private String type;
    private Object coordinates;

    public String getType() {
        return type;
    }

    public Object getCoordinates() {
        return coordinates;
    }

    @NonNull
    @Override
    public String toString() {
        return "RouteGeometry{" +
                "type='" + type + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}
