package fr.ynryo.ouestcetram.apiResponsesPOJO.train;

import androidx.annotation.NonNull;

import java.util.List;

public class TrainData {
    private String type;
    private List<TrainFeature> features;

    public String getType() {
        return type;
    }

    public List<TrainFeature> getRouteFeatures() {
        return features;
    }

    public String getDestination() {
        if (features == null) return null;

        for (int i = features.size() - 1; i > 0; i--) {
            TrainProperties trainProperties = features.get(i).getProperties();
            if (!trainProperties.isStop() || trainProperties.isRoute()) continue;
            return trainProperties.getLocalite();
        }

        return null;
    }

    public boolean isDestinationStop(String destination) {
        return destination.equals(getDestination());
    }

    public String getDepartureStop() {
        if (features == null) return null;

        for (TrainFeature trainFeature : features) {
            TrainProperties trainProperties = trainFeature.getProperties();
            if (!trainProperties.isStop() || trainProperties.isRoute()) continue;
            return trainProperties.getLocalite();
        }

        return null;
    }

    public boolean isDepartureStop(String departure) {
        return departure.equals(getDepartureStop());
    }

    @NonNull
    @Override
    public String toString() {
        return "TrainData{" +
                "type='" + type + '\'' +
                ", features=" + features +
                ", departure=" + getDepartureStop() +
                ", destination=" + getDestination() +
                '}';
    }
}
