package fr.ynryo.ouestcefdpdetram;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.material.bottomsheet.BottomSheetDialog;

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
    private List<MarkerData> markersData;
    private VehicleDetails vehicleDetails = new VehicleDetails();


    public FetchingManager(MainActivity context) {
        this.context = context;
    }

    public List<MarkerData> fetchMarkers () {
        if (context.getMap() == null) return null; // si pas de map return
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        NaolibApiService service = retrofit.create(NaolibApiService.class);
        LatLngBounds bounds = context.getMap().getProjection().getVisibleRegion().latLngBounds;
        Call<VehicleJourneyResponse> call = service.getVehicleMarkers(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<VehicleJourneyResponse> call, @NonNull Response<VehicleJourneyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // liste des marqueurs (que les datas)
                    markersData = response.body().getItems();
                } else {
                    Log.e(this.toString(), "API call failed: " + response.code() + " " + response.errorBody());
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehicleJourneyResponse> call, @NonNull Throwable t) {
                Log.e(this.toString(), "API call error", t);
            }
        });
        return markersData;
    }

    public VehicleDetails fetchVehicleStopsInfo(MarkerData marker, View view) {
        // Appel API
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        NaolibApiService service = retrofit.create(NaolibApiService.class);
        String encodedId = "";
        try {
            encodedId = URLEncoder.encode(marker.getId(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e("MainActivity", "Encoding error", e);
        }
        Call<VehicleDetails> call = service.getVehicleDetails(encodedId);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<VehicleDetails> call, @NonNull Response<VehicleDetails> response) {
                //loader off
                view.findViewById(R.id.loader).setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    vehicleDetails = response.body();
                } else {
                    Log.e("MainActivity", "API Error: " + response.code());
                }
            }
            @Override
            public void onFailure(@NonNull Call<VehicleDetails> call, @NonNull Throwable t) {
                view.findViewById(R.id.loader).setVisibility(View.GONE);
                Log.e("MainActivity", "Network Error", t);
            }
        });

        if (vehicleDetails == null) {
            Log.e("FetchingManager", "Erreur de kk");
        }
        Log.e("dsfdsf", vehicleDetails.toString());
        return vehicleDetails;
    }
}
