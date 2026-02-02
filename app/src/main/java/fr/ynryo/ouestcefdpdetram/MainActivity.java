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
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {
    private GoogleMap mMap;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private final Map<String, Marker> activeMarkers = new HashMap<>();
    private final FetchingManager fetcher = new FetchingManager(this);
    private RouteArtist routeArtist;
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            fetcher.fetchMarkers(new FetchingManager.OnMarkersListener() {
                @Override
                public void onMarkersReceived(List<MarkerData> markers) {
                    showMarkers(markers);
                }

                @Override
                public void onError(String error) {
                    Log.e("MainActivity", "Erreur lors de la récupération des données markers");
                }
            });
            handler.postDelayed(this, 5000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DynamicColors.applyToActivitiesIfAvailable(this.getApplication());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        FloatingActionButton fab = findViewById(R.id.fab_center_location);
        fab.setOnClickListener(view -> centerMapOnUserLocation());
        routeArtist = new RouteArtist(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // Position par défaut sur Nantes
        LatLng nantes = new LatLng(47.218371, -1.553621);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nantes, 15.0f));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
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
    public boolean onMarkerClick(@NonNull Marker marker) {
        MarkerData data = (MarkerData) marker.getTag();
        if (data != null) {
            new VehicleDetailsActivity(this).init(data);

            //draw route
            if (data.getId().contains("SNCF")) {
                routeArtist.drawVehicleRoute(data);
            }
        }
        return true; // true pour indiquer qu'on gère l'événement
    }

    @Override
    public void onCameraIdle() {
        // Refresh des marqueurs quand la carte s'arrête de bouger
        fetcher.fetchMarkers(new FetchingManager.OnMarkersListener() {
            @Override
            public void onMarkersReceived(List<MarkerData> markers) {
                showMarkers(markers);
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Erreur lors de la récupération des données markers");
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

    private void centerMapOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15.0f));
                    }
                });
    }

    private void showMarkers(List<MarkerData> markersFetched) {
        if (mMap == null || markersFetched == null) return;

        Set<String> newMarkerIds = new HashSet<>();
        for (MarkerData d : markersFetched) {
            newMarkerIds.add(d.getId());
        }

        // Nettoyage des marqueurs obsolètes
        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            if(!newMarkerIds.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        float mapRotation = mMap.getCameraPosition().bearing;
        for (MarkerData markerData : markersFetched) {
            LatLng position = new LatLng(markerData.getPosition().getLatitude(), markerData.getPosition().getLongitude());

            if (activeMarkers.containsKey(markerData.getId())) {
                Marker existingMarker = activeMarkers.get(markerData.getId());
                if (existingMarker != null) {
                    existingMarker.setPosition(position);
                    existingMarker.setIcon(createCustomMarker(this, markerData, mapRotation));
                    existingMarker.setTag(markerData);
                }
            } else {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(createCustomMarker(this, markerData, mapRotation))
                        .anchor(0.5f, 0.3f));
                if (newMarker != null) {
                    newMarker.setTag(markerData);
                    activeMarkers.put(markerData.getId(), newMarker);
                }
            }
        }
    }

    public static BitmapDescriptor createCustomMarker(Context context, MarkerData markerData, float mapRotation) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.custom_marker, null);
        ImageView markerCircle = markerLayout.findViewById(R.id.marker_circle);
        TextView lineNumberView = markerLayout.findViewById(R.id.line_number);

        int fillColor = Color.parseColor(markerData.getFillColor() != null ? markerData.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerData.getColor() != null ? markerData.getColor() : "#FFFFFF");
        float bearing = markerData.getPosition().getBearing();

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        if (drawable != null) {
            LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
            Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);
            Drawable arrowPart = layerDrawable.findDrawableByLayerId(R.id.marker_arrow);

            if (backgroundPart != null) DrawableCompat.setTint(backgroundPart, fillColor);
            if (arrowPart != null) {
                DrawableCompat.setTint(arrowPart, fillColor);
                arrowPart.setAlpha(bearing == 0 ? 0 : 255);
            }
            markerCircle.setImageDrawable(layerDrawable);
        }

        if (markerData.getId().contains("SNCF")) {
            lineNumberView.setText(String.valueOf(markerData.getVehicleNumber() != null ? markerData.getVehicleNumber() : "0"));
        } else {
            lineNumberView.setText(String.valueOf(markerData.getLineNumber() != null ? markerData.getLineNumber() : "0"));
        }
        lineNumberView.setTextColor(textColor);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(10);
        lineNumberView.setBackground(gd);

        markerCircle.setRotation(bearing - mapRotation);

        markerLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerLayout.layout(0, 0, markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerLayout.getMeasuredWidth(), markerLayout.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerLayout.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public GoogleMap getMap() { return mMap; }

    public FetchingManager getFetcher() { return fetcher; }
}