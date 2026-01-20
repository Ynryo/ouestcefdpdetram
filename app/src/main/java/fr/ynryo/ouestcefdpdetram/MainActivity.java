package fr.ynryo.ouestcefdpdetram;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private GoogleMap mMap;
    private static final String BASE_URL = "https://bus-tracker.fr/api/vehicle-journeys/";
    private static final String TAG = "MainActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int UPDATE_INTERVAL = 5000; // 5 seconds

    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            loadTramMarkers();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);

        // Initialiser la carte avec une localisation par défaut (par exemple, Nantes)
        LatLng nantes = new LatLng(47.218371, -1.553621);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nantes, 12.0f)); // Zoom level 12
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(vehicleUpdateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(vehicleUpdateRunnable);
    }

    @Override
    public void onCameraIdle() {
        loadTramMarkers();
    }

    private void loadTramMarkers() {
        if (mMap == null) return;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaolibApiService service = retrofit.create(NaolibApiService.class);

        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        Call<VehicleJourneyResponse> call = service.getVehicleMarkers(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude);

        call.enqueue(new Callback<VehicleJourneyResponse>() {
            @Override
            public void onResponse(Call<VehicleJourneyResponse> call, Response<VehicleJourneyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mMap.clear();
                    float currentZoom = mMap.getCameraPosition().zoom;
                    List<MarkerData> markers = response.body().getItems();
                    if (markers != null) {
                        for (MarkerData marker : markers) {
                            if (marker.getPosition() != null && marker.getFillColor() != null) {
                                LatLng position = new LatLng(marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
                                int color = Color.parseColor(marker.getFillColor());

                                double metersPerPixel = (156543.03392 * Math.cos(position.latitude * Math.PI / 180) / Math.pow(2, currentZoom));
                                double radiusInMeters = 8 * metersPerPixel;

                                mMap.addCircle(new CircleOptions()
                                        .center(position)
                                        .radius(radiusInMeters) // Radius in meters
                                        .fillColor(color)
                                        .strokeWidth(0));

                                mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(marker.getLineNumber())
                                        .alpha(0.0f)
                                        .anchor(0.5f, 0.5f));

                                Log.d(TAG, "Circle added: " + marker.getLineNumber() + " at " + position.toString());
                            }
                        }
                    }
                } else {
                    String errorBody = "Unknown error";
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body", e);
                        }
                    }
                    Log.e(TAG, "API call failed: " + response.code() + " " + errorBody);
                }
            }

            @Override
            public void onFailure(Call<VehicleJourneyResponse> call, Throwable t) {
                Log.e(TAG, "API call error", t);
            }
        });
    }
}
