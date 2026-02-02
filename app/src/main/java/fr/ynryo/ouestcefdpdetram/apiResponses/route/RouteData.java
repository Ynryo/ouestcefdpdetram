package fr.ynryo.ouestcefdpdetram.apiResponses.route;

import androidx.annotation.NonNull;

import java.util.List;

public class RouteData {
    private String type;
    private List<RouteFeature> features;

    public String getType() {
        return type;
    }

    public List<RouteFeature> getRouteFeatures() {
        return features;
    }

    @NonNull
    @Override
    public String toString() {
        return "RouteData{" +
                "type='" + type + '\'' +
                ", features=" + features +
                '}';
    }
}
