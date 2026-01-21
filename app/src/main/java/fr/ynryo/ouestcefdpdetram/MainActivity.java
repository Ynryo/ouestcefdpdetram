package fr.ynryo.ouestcefdpdetram;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MediaPlayer mediaPlayer; // CHAMP AJOUTÉ

    // thread pour mettre à jour les marqueurs
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            loadTramMarkers();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // init de la carte
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.hub_intro_sound);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(false); // Ne pas répéter
                mediaPlayer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de la lecture du fichier audio", e);
        }

        // initialiser le client de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // get la map

        FloatingActionButton fab = findViewById(R.id.fab_center_location); // button center loc
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                centerMapOnUserLocation();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Désactive le bouton de localisation par défaut
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE); // Ajouté: Met la carte en mode satellite

        // initialiser la carte avec une localisation par défaut (ici Nantes)
        LatLng nantes = new LatLng(47.218371, -1.553621);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f)); // Zoom level 12

        // demander la permission de géolocalisation si elle n'est pas accordée
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // si la permission n'est pas accordée, demander la permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // si la permission est accordée, activer la géolocalisation
            mMap.setMyLocationEnabled(true);
        }
    }

    // centrer la carte sur la localisation de l'utilisateur
    private void centerMapOnUserLocation() {
        // si on a pas la perm de loc, on fait rien
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f));
                        }
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    // mise à jour des marqueurs
    @Override
    protected void onResume() {
        super.onResume();
        handler.post(vehicleUpdateRunnable);
    }

    // suppression des marqueurs
    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(vehicleUpdateRunnable);
    }

    // Libérer le MediaPlayer quand l'activité est détruite pour éviter les fuites de mémoire.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // mise à jour des marqueurs
    @Override
    public void onCameraIdle() {
        loadTramMarkers();
    }

    // chargement des marqueurs
    private void loadTramMarkers() {
        if (mMap == null) return; // si pas de map return
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();

        NaolibApiService service = retrofit.create(NaolibApiService.class);

        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        Call<VehicleJourneyResponse> call = service.getVehicleMarkers(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude);

        call.enqueue(new Callback<VehicleJourneyResponse>() { // appel asynchrone
            @Override
            public void onResponse(Call<VehicleJourneyResponse> call, Response<VehicleJourneyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mMap.clear();
                    List<MarkerData> markers = response.body().getItems();
                    if (markers != null) {
                        for (MarkerData marker : markers) {
                            if (marker.getPosition() != null && marker.getFillColor() != null && marker.getColor() != null) {
                                LatLng position = new LatLng(marker.getPosition().getLatitude(), marker.getPosition().getLongitude());
                                int fillColor = Color.parseColor(marker.getFillColor());
                                int textColor = Color.parseColor(marker.getColor());

                                mMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .icon(createCustomMarker(MainActivity.this, marker.getLineNumber(), fillColor, textColor, marker.getPosition().getBearing()))
                                        .anchor(0.5f, 0.5f)); // mid du xml (milieu du cercle)

                                Log.d(TAG, "Marker added: " + marker.getLineNumber() + " at " + position.toString());
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

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static BitmapDescriptor createCustomMarker(Context context, String lineNumberText, int fillColor, int textColor, float bearing) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.custom_marker, null);

        ImageView markerCircle = markerLayout.findViewById(R.id.marker_circle);
        TextView lineNumberView = markerLayout.findViewById(R.id.line_number);

        // ici on teinte
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        if (drawable instanceof LayerDrawable) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();

            // On cherche la couche du fond (le triangle et le cercle extérieur) par son ID
            Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);

            if (backgroundPart != null) {
                DrawableCompat.setTint(backgroundPart, fillColor);
            }
            markerCircle.setImageDrawable(layerDrawable);
        } else {
            // Fallback au cas où le XML ne serait pas un LayerDrawable
            markerCircle.setImageDrawable(drawable);
        }

        // 4. Configuration du texte (numéro de ligne)
        lineNumberView.setText(lineNumberText);
        lineNumberView.setTextColor(textColor);
        int paddingPx = dpToPx(context, 5);
        lineNumberView.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);


        // Applique le fond avec des coins arrondis
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setColor(fillColor);
        gradientDrawable.setCornerRadius(10);

        lineNumberView.setBackground(gradientDrawable);

        markerCircle.setRotation(bearing);

        // 6. Rendu du Layout en Bitmap pour Google Maps
        markerLayout.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerLayout.getMeasuredWidth(),
                markerLayout.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        markerLayout.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}