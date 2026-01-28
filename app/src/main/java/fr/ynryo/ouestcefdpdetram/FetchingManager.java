package fr.ynryo.ouestcefdpdetram;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchingManager {
    private static final String BASE_URL = "https://bus-tracker.fr/api/vehicle-journeys/";
    private final MainActivity context;

    // INTERFACES pour gérer l'asynchrone
    public interface OnMarkersListener {
        void onMarkersReceived(List<MarkerData> markers);
    }

    public interface OnVehicleDetailsListener {
        void onDetailsReceived(VehicleDetails details);
        void onError(String error);
    }

    public FetchingManager(MainActivity context) {
        this.context = context;
    }

    private NaolibApiService getService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(NaolibApiService.class);
    }

    public void fetchMarkers(OnMarkersListener listener) {
        if (context.getMap() == null) return;

        LatLngBounds bounds = context.getMap().getProjection().getVisibleRegion().latLngBounds;
        getService().getVehicleMarkers(
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude
        ).enqueue(new Callback<VehicleJourneyResponse>() {
            @Override
            public void onResponse(@NonNull Call<VehicleJourneyResponse> call, @NonNull Response<VehicleJourneyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onMarkersReceived(response.body().getItems());
                }
            }
            @Override
            public void onFailure(@NonNull Call<VehicleJourneyResponse> call, @NonNull Throwable t) {
                Log.e("FetchingManager", "Markers failure", t);
            }
        });
    }

    public void fetchVehicleStopsInfo(MarkerData marker, OnVehicleDetailsListener listener) {
        try {
            String encodedId = URLEncoder.encode(marker.getId(), StandardCharsets.UTF_8.toString());
            getService().getVehicleDetails(encodedId).enqueue(new Callback<VehicleDetails>() {
                @Override
                public void onResponse(@NonNull Call<VehicleDetails> call, @NonNull Response<VehicleDetails> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // ON PRÉVIENT LE LISTENER QUE C'EST PRÊT
                        listener.onDetailsReceived(response.body());
                    } else {
                        listener.onError("Code erreur: " + response.code());
                    }
                }
                @Override
                public void onFailure(@NonNull Call<VehicleDetails> call, @NonNull Throwable t) {
                    listener.onError(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onError("Encoding error");
        }
    }
}