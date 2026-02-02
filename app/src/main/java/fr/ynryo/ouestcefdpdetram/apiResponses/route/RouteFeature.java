package fr.ynryo.ouestcefdpdetram.apiResponses.route;

import androidx.annotation.NonNull;

public class RouteFeature {
    private String type;
    private RouteGeometry geometry;

    public String getType() {
        return type;
    }

    public RouteGeometry getRouteGeometry() {
        return geometry;
    }

    @NonNull
    @Override
    public String toString() {
        return "RouteFeature{" +
                "type='" + type + '\'' +
                ", geometry=" + geometry +
                '}';
    }
}
