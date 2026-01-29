package fr.ynryo.ouestcefdpdetram.apiResponses.markers;

import androidx.annotation.NonNull;

import java.util.List;

public class MarkerDataResponse {
    private List<MarkerData> items;

    // on a une classe à part parce que y'a que items dans la response api
    public List<MarkerData> getItems() {
        return items;
    }

    @NonNull
    @Override
    public String toString() {
        return "MarkerDataResponse{" +
                "items=" + items +
                '}';
    }
}
