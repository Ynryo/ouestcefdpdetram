package fr.ynryo.ouestcefdpdetram;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.region.RegionData;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener {
    private FetchingManager fetcher;
    private MarkerArtist markerArtist;
    private RouteArtist routeArtist;
    private NetworkFilterDrawer networkFilterDrawer;
    private CompassManager compassManager;
    private FollowManager followManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 13f;
    private static final LatLng PARIS = new LatLng(48.8566, 2.3522);

    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean isMapReady = false;
    private boolean isDataReady = false;
    private List<RegionData> pendingRegions;
    private List<NetworkData> pendingNetworks;

    private boolean isFetching = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, 5000);
            fetchMarkers();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fetcher = new FetchingManager(this);
        routeArtist = new RouteArtist(this);
        networkFilterDrawer = new NetworkFilterDrawer(this);
        compassManager = new CompassManager(this);
        followManager = new FollowManager(this);
        markerArtist = new MarkerArtist(this, followManager, networkFilterDrawer);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        markerArtist.setCachedMarkerView(LayoutInflater.from(this).inflate(R.layout.custom_marker, null));

        fetcher.fetchRegions(new FetchingManager.OnRegionsListener() {
            @Override
            public void onRegionsReceived(List<RegionData> regions) {
                fetcher.fetchNetworks(new FetchingManager.OnNetworkListener() {
                    @Override
                    public void onDetailsReceived(List<NetworkData> data) {
                        pendingRegions = regions;
                        pendingNetworks = data;
                        isDataReady = true;
                        onEverythingReady();
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("MainActivity", "Erreur réseaux: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e("MainActivity", "Erreur régions: " + error);
            }
        });

        findViewById(R.id.btn_open_menu).setOnClickListener(v -> networkFilterDrawer.open());
        findViewById(R.id.fab_center_location).setOnClickListener(view -> centerMapOnUserLocation());
    }

    private void onEverythingReady() {
        if (!isMapReady || !isDataReady) return;

        networkFilterDrawer.populateNetworks(pendingRegions, pendingNetworks);
        centerMapOnUserLocation();
        fetchMarkers();
        handler.post(vehicleUpdateRunnable);
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        markerArtist.setmMap(mMap);
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PARIS, DEFAULT_ZOOM));
        mMap.setOnCameraIdleListener(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnCameraMoveListener(this);
        mMap.setOnCameraMoveStartedListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            mMap.setMyLocationEnabled(true);
        }

        isMapReady = true;
        onEverythingReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMapReady && isDataReady) {
            handler.post(vehicleUpdateRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        followManager.disableFollow(false);
        handler.removeCallbacks(vehicleUpdateRunnable);
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker) {
        MarkerData data = (MarkerData) marker.getTag();
        if (data != null) {
            new VehicleDetailsManager(this).init(data);

            if (data.getId().contains("SNCF")) {
                routeArtist.drawVehicleRoute(data);
            } else {
                routeArtist.clear();
            }
        }
        return true;
    }

    @Override
    public void onCameraIdle() {
        fetchMarkers();
        markerArtist.updateMarkerRotations();
    }

    @Override
    public void onCameraMove() {
        compassManager.updateAzimuth(mMap.getCameraPosition().bearing);
    }

    @Override
    public void onCameraMoveStarted(int reason) {
        if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && followManager.getFollowedMarkerId() != null) {
            followManager.disableFollow(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            centerMapOnUserLocation();
        }
    }

    private void centerMapOnUserLocation() {
        if (mMap == null) return;

        //no position go paris
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("MainActivity", "Pas de permission - reste à Paris");
            return;
        }

        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Log.d("MainActivity", "Position trouvée: " + location.getLatitude() + ", " + location.getLongitude());
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(userLocation)
                                    .zoom(14f)
                                    .tilt(0)
                                    .build()
                    ), 1000, null);
                } else {
                    Log.d("MainActivity", "getLastLocation() retourne null - reste à Paris"); //reste sur paris
                }
            }).addOnFailureListener(e -> Log.e("MainActivity", "Erreur getLastLocation: " + e.getMessage())); //reste sur paris
        } catch (Exception e) {
            Log.e("MainActivity", "Exception centerMapOnUserLocation: " + e.getMessage());
        }
    }

    public void centerOnMarker(String markerId) {
        Marker marker = markerArtist.getActiveMarkers().get(markerId);
        if (marker != null && mMap != null) {
            MarkerData data = (MarkerData) marker.getTag();
            float bearing = data != null ? data.getPosition().getBearing() : 0f;

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(marker.getPosition())
                            .bearing(bearing)
                            .tilt(60f)
                            .zoom(17f)
                            .build()
            ));
        }
    }

    public void fetchMarkers() {
        if (isFetching) return;
        isFetching = true;

        fetcher.fetchMarkers(new FetchingManager.OnMarkersListener() {
            @Override
            public void onMarkersReceived(List<MarkerData> markers) {
                isFetching = false;
                markerArtist.showMarkers(markers);
                if (markerArtist.getMarkerIconCache().size() > 200) {
                    markerArtist.getMarkerIconCache().clear();
                }
            }

            @Override
            public void onError(String error) {
                isFetching = false;
                Log.e("MainActivity", "Erreur markers: " + error);
            }
        });
    }

    public GoogleMap getMap() {
        return mMap;
    }

    public FetchingManager getFetcher() {
        return fetcher;
    }

    public FollowManager getFollowManager() {
        return followManager;
    }

    public MarkerArtist getMarkerArtist() {
        return markerArtist;
    }
}