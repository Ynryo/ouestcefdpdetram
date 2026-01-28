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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {
    private GoogleMap mMap;
    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private Map<String, Marker> activeMarkers = new HashMap<>();
    private final FetchingManager fetcher = new FetchingManager(this);
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            List<MarkerData> markersData = fetcher.fetchMarkers();
            showMarkers(markersData);
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());

        // initialiser le client de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this); // get la map

        FloatingActionButton fab = findViewById(R.id.fab_center_location); // button center loc
        fab.setOnClickListener(view -> centerMapOnUserLocation());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // initialiser la carte avec une localisation par défaut (ici Nantes)
        LatLng nantes = new LatLng(47.218371, -1.553621);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f)); // Zoom level 15

        // demander la permission de géolocalisation si elle n'est pas accordée
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // si la permission n'est pas accordée, demander la permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // si la permission est accordée, activer la géolocalisation
            mMap.setMyLocationEnabled(true);
//            centerMapOnUserLocation();
        }
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
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        MarkerData data = (MarkerData) marker.getTag(); //données déjà sauvegardées
        VehicleDetailsActivity vehicleDetailsActivity = new VehicleDetailsActivity(this);
        vehicleDetailsActivity.init(data);
        return false;
    }

    @Override
    public void onCameraIdle() {
        List<MarkerData> markersData = fetcher.fetchMarkers();
        showMarkers(markersData);
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

    private void centerMapOnUserLocation() {
        // si on a pas la perm de loc, on fait rien
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f));
                    }
                });
    }

    private void showMarkers(List<MarkerData> markersFetched) {
        if (mMap == null) return; // si pas de map return
        if (markersFetched == null) return;
        // check if marker already exist
        Set<String> newMarkerIds = new HashSet<>();
        for (MarkerData d : markersFetched) {
            newMarkerIds.add(d.getId());
        }

        // on suppr les markers qui n'existent plus dans la nouvelle map fetched
        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator(); // itérateur sur les clés des markers existants
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            String markerId = entry.getKey();
            if(!newMarkerIds.contains(markerId)) {
                entry.getValue().remove(); //remove de la map
                iterator.remove();
            }
        }

        float mapRotation = mMap.getCameraPosition().bearing;
        for (MarkerData markerData : markersFetched) {
            LatLng position = new LatLng(markerData.getPosition().getLatitude(), markerData.getPosition().getLongitude());

            if (activeMarkers.containsKey(markerData.getId())) {
                Marker existingMarker = activeMarkers.get(markerData.getId());
                existingMarker.setPosition(position);
                existingMarker.setIcon(createCustomMarker(MainActivity.this, markerData, mapRotation));
                existingMarker.setTag(markerData);
            } else {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(createCustomMarker(MainActivity.this, markerData, mapRotation))
                        .anchor(0.5f, 0.5f));
                newMarker.setTag(markerData);
                activeMarkers.put(markerData.getId(), newMarker);
            }
        }
    }

    public static BitmapDescriptor createCustomMarker(Context context, MarkerData markerData, float mapRotation) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.custom_marker, null);
        ImageView markerCircle = markerLayout.findViewById(R.id.marker_circle);
        TextView lineNumberView = markerLayout.findViewById(R.id.line_number);
        String fillColorString = markerData.getFillColor();
        int fillColor = Color.parseColor(fillColorString != null && !fillColorString.isEmpty() ? fillColorString : "#424242");
        String textColorString = markerData.getColor();
        int textColor = Color.parseColor(textColorString != null && !textColorString.isEmpty() ? textColorString : "#FFFFFF");
        float bearing = markerData.getPosition().getBearing();

        // ici on teinte
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
        Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);
        Drawable arrowPart = layerDrawable.findDrawableByLayerId(R.id.marker_arrow);

        if (backgroundPart != null) {
            DrawableCompat.setTint(backgroundPart, fillColor);
            DrawableCompat.setTint(arrowPart, fillColor);
        }

        if (bearing == 0) {
            arrowPart.setAlpha(0);
        } else {
            arrowPart.setAlpha(255);
        }

        markerCircle.setImageDrawable(layerDrawable);

        // 4. Configuration du texte (numéro de ligne)
        lineNumberView.setText(markerData.getLineNumber() != null ? markerData.getLineNumber() : "INCONNU");
        lineNumberView.setTextColor(textColor);
        lineNumberView.setPadding(10, 5, 10, 5);


        // Applique le fond avec des coins arrondis
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setColor(fillColor);
        gradientDrawable.setCornerRadius(10);
        lineNumberView.setBackground(gradientDrawable);

        markerCircle.setRotation(bearing - mapRotation);

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

    public GoogleMap getMap() {
        return mMap;
    }

    public FetchingManager getFetcher() {
        return fetcher;
    }
//    public void fetchNetworkDatas(int networkId) {
//        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
//        NaolibApiService service = retrofit.create(NaolibApiService.class);
//
//        try {
//            Call<NetworkJourneyResponse> call = service.getNetworkInfomations(networkId);
//            call.enqueue(new Callback<NetworkJourneyResponse>() {
//                @Override
//                public void onResponse(@NonNull Call<NetworkJourneyResponse> call, @NonNull Response<NetworkJourneyResponse> response) {
//                    if (response.isSuccessful() && response.body() != null) {
//                        List<NetworkData> networks = response.body();
//                    }
//                }
//
//                @Override
//                public void onFailure(@NonNull Call<NetworkJourneyResponse> call, @NonNull Throwable t) {
//                    Log.e(TAG, "Erreur réseau", t);
//                }
//            });
//        } catch (Exception e) {
//            Log.e(TAG, "Erreur lors de la récupération des détails du réseau", e);
//        }
//    }
}