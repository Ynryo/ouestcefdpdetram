package fr.ynryo.ouestcefdpdetram;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.view.animation.LinearInterpolator;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {
    private GoogleMap mMap;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private final Map<String, Marker> activeMarkers = new HashMap<>();
    private FetchingManager fetcher;
    private RouteArtist routeArtist;
    private NetworkFilterDrawer filterDrawer;

    private View cachedMarkerView;
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

        fetcher = new FetchingManager(this);
        routeArtist = new RouteArtist(this);
        filterDrawer = new NetworkFilterDrawer(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        cachedMarkerView = LayoutInflater.from(this).inflate(R.layout.custom_marker, null);
        fetcher.fetchNetworks(new FetchingManager.OnNetworkListener() { //demande la liste des networks
            @Override
            public void onDetailsReceived(List<NetworkData> data) {
                Log.d("MainActivity", data.toString());
                filterDrawer.populateNetworks(data);
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Erreur lors de la récupération des réseaux" + error);
            }
        });

        findViewById(R.id.btn_open_menu).setOnClickListener(v -> filterDrawer.open());
        findViewById(R.id.fab_center_location).setOnClickListener(view -> centerMapOnUserLocation());
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        // MarkerPosition par défaut sur Nantes
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
        //TODO: bring marker to front
        MarkerData data = (MarkerData) marker.getTag();
        if (data != null) {
            new VehicleDetailsManager(this).init(data);

            //draw route
            if (data.getId().contains("SNCF")) {
                routeArtist.drawVehicleRoute(data);
            } else {
                routeArtist.clear();
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
                Log.e("MainActivity", "Erreur lors de la récupération des données markers" + error);
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

    void showMarkers(List<MarkerData> markersFetched) {
        if (mMap == null || markersFetched == null) return;

        Set<String> newMarkerIds = new HashSet<>();
        for (MarkerData d : markersFetched) {
            newMarkerIds.add(d.getId());
        }

        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Marker> entry = iterator.next();
            if(!newMarkerIds.contains(entry.getKey())) {
                entry.getValue().remove();
                iterator.remove();
            }
        }

        float mapRotation = mMap.getCameraPosition().bearing;
        for (MarkerData markerData : markersFetched) { //pour chaque marker

            if (!filterDrawer.isNetworkVisible(markerData.getNetworkRef())) {
                if (activeMarkers.containsKey(markerData.getId())) { //si il était là, il part
                    activeMarkers.get(markerData.getId()).remove();
                    activeMarkers.remove(markerData.getId());
                }
                continue; //si il était pas là, chill
            }

            LatLng position = new LatLng(markerData.getPosition().getLatitude(), markerData.getPosition().getLongitude());

            if (activeMarkers.containsKey(markerData.getId())) {
                Marker existingMarker = activeMarkers.get(markerData.getId());
                if (existingMarker != null) {
                    animateMarker(existingMarker, position);

                    MarkerData oldData = (MarkerData) existingMarker.getTag();
                    boolean needUpdate = false;

                    if (oldData != null) {
                        float oldBearing = oldData.getPosition().getBearing();
                        float newBearing = markerData.getPosition().getBearing();
                        if (Math.abs(oldBearing - newBearing) > 2.0f) {
                            needUpdate = true;
                        }
                    } else {
                        needUpdate = true;
                    }

                    if (needUpdate) {
                        existingMarker.setIcon(createCustomMarker(markerData, mapRotation));
                    }

                    existingMarker.setTag(markerData);
                }
            } else {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(createCustomMarker(markerData, mapRotation))
                        .anchor(0.5f, 0.3f));
                if (newMarker != null) {
                    newMarker.setTag(markerData);
                    activeMarkers.put(markerData.getId(), newMarker);
                }
            }
        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition) {
        final LatLng startPosition = marker.getPosition();
        final long duration = 2000;

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(duration);
        valueAnimator.setInterpolator(new LinearInterpolator()); // Vitesse constante

        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();

            double lng = v * toPosition.longitude + (1 - v) * startPosition.longitude;
            double lat = v * toPosition.latitude + (1 - v) * startPosition.latitude;

            marker.setPosition(new LatLng(lat, lng));
        });
        valueAnimator.start();
    }

    public BitmapDescriptor createCustomMarker(MarkerData markerData, float mapRotation) {
        ImageView markerCircle = cachedMarkerView.findViewById(R.id.marker_circle);
        TextView lineNumberView = cachedMarkerView.findViewById(R.id.line_number);

        int fillColor = Color.parseColor(markerData.getFillColor() != null ? markerData.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerData.getColor() != null ? markerData.getColor() : "#FFFFFF");
        float bearing = markerData.getPosition().getBearing();

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.marker_circle);
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

        if (markerData.getId().startsWith("SNCF")) {
            lineNumberView.setText(markerData.getVehicleNumber() != null ? markerData.getVehicleNumber() : "ND");
        } else {
            lineNumberView.setText(markerData.getLineNumber() != null ? markerData.getLineNumber() : "BD");
        }
        lineNumberView.setTextColor(textColor);

        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(10);
        lineNumberView.setBackground(gd);

        markerCircle.setRotation(bearing - mapRotation);

        cachedMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        cachedMarkerView.layout(0, 0, cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cachedMarkerView.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public GoogleMap getMap() { return mMap; }

    public FetchingManager getFetcher() { return fetcher; }
}