package fr.ynryo.ouestcefdpdetram;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String BASE_URL = "https://bus-tracker.fr/api/vehicle-journeys/";
    private static final String TAG = "MainActivity";

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

        // Initialiser la carte avec une localisation par défaut (par exemple, Nantes)
        LatLng nantes = new LatLng(47.218371, -1.553621);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nantes, 12.0f)); // Zoom level 12

        // Charger les marqueurs des trams
        loadTramMarkers();
    }

    private void loadTramMarkers() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NaolibApiService service = retrofit.create(NaolibApiService.class);

        // Définir les coordonnées pour la requête (elles doivent correspondre à la vue actuelle de la carte)
        // Pour l'instant, on utilise des valeurs fixes pour Nantes.
        // Idéalement, il faudrait les obtenir dynamiquement lorsque la caméra bouge.
        double swLat = 47.238964026816376;
        double swLon = -1.6082083862799834;
        double neLat = 47.270766383709685;
        double neLon = -1.4948706842746446;

        Call<List<MarkerData>> call = service.getVehicleMarkers(swLat, swLon, neLat, neLon);

        call.enqueue(new Callback<List<MarkerData>>() {
            @Override
            public void onResponse(Call<List<MarkerData>> call, Response<List<MarkerData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MarkerData> markers = response.body();
                    for (MarkerData marker : markers) {
                        LatLng position = new LatLng(marker.getLat(), marker.getLon());
                        mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(marker.getTitle()));
                        Log.d(TAG, "Marker added: " + marker.getTitle() + " at " + position.toString());
                    }
                } else {
                    // Correction de Log.error qui n'existe pas, utilisation de Log.e
                    try {
                        Log.e(TAG, "API call failed: " + response.code() + " " + (response.errorBody() != null ? response.errorBody().string() : "Unknown error"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        // Si on veut logguer le corps de la réponse d'erreur
                        if (response.errorBody() != null) {
                            Log.e(TAG, "Error Body: " + response.errorBody().string());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Exception reading error body: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<MarkerData>> call, Throwable t) {
                // Correction de Log.error qui n'existe pas, utilisation de Log.e
                Log.e(TAG, "API call error: " + t.getMessage());
            }
        });
    }
}
