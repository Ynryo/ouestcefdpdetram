package fr.ynryo.ouestcefdpdetram;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

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

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.version.VersionResponse;
import fr.ynryo.ouestcefdpdetram.artists.MarkerArtist;
import fr.ynryo.ouestcefdpdetram.managers.CompassManager;
import fr.ynryo.ouestcefdpdetram.managers.FavoriteManager;
import fr.ynryo.ouestcefdpdetram.managers.FetchingManager;
import fr.ynryo.ouestcefdpdetram.managers.FollowManager;
import fr.ynryo.ouestcefdpdetram.managers.SaveManager;

/**
 * Classe principale, gère la vue et les managers
 * @author Ynryo
 * @version 1.2.3
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener, GoogleMap.OnCameraMoveStartedListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final float DEFAULT_ZOOM = 13f;
    private static final LatLng PARIS = new LatLng(48.8566, 2.3522);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            handler.postDelayed(this, 5000);
            fetchMarkers();
        }
    };

    private LateralDrawerActivity lateralDrawerActivity;
    private FetchingManager fetcher;
    private MarkerArtist markerArtist;
    private CompassManager compassManager;
    private FollowManager followManager;
    private FavoriteManager favoriteManager;
    private SaveManager saveManager;

    private boolean isFetching = false;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        saveManager = new SaveManager(this);
        fetcher = new FetchingManager(this);
        lateralDrawerActivity = new LateralDrawerActivity(this, saveManager);
        compassManager = new CompassManager(this);
        followManager = new FollowManager(this);
        favoriteManager = new FavoriteManager(this, saveManager);
        markerArtist = new MarkerArtist(this, followManager, lateralDrawerActivity);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        markerArtist.setCachedMarkerView(LayoutInflater.from(this).inflate(R.layout.custom_marker, null));

        fetcher.fetchLatestVersion(new FetchingManager.OnVersionListener() {
            @Override
            public void onResponseVersionListener(VersionResponse version) {
                try {
                    PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    int latestVersionCode = version.getVersion().getVersionCode();
                    long localVersionCode = pInfo.getLongVersionCode();
                    Log.d("MainActivity", "Version locale: " + localVersionCode + ", version réseau: " + latestVersionCode);
                    if (latestVersionCode > localVersionCode) {
                        Toast.makeText(MainActivity.this, "Une nouvelle version est disponible", Toast.LENGTH_LONG).show();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("VersionChecker", e.toString());
                }
            }

            @Override
            public void onErrorVersionListener(String error) {
                Log.e("MainActivity", "Erreur de l'API: " + error);
            }
        });

        findViewById(R.id.btn_open_menu).setOnClickListener(view -> lateralDrawerActivity.open());
        findViewById(R.id.fab_center_location).setOnClickListener(view -> {
            MediaPlayer mp = MediaPlayer.create(this, R.raw.avion);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
            centerMapOnUserLocation();
        });
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        markerArtist.setGoogleMap(this.googleMap);
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(PARIS, DEFAULT_ZOOM));
        this.googleMap.setOnCameraIdleListener(this);
        this.googleMap.setOnMarkerClickListener(this);
        this.googleMap.setOnCameraMoveListener(this);
        this.googleMap.setOnCameraMoveStartedListener(this);
        this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        this.googleMap.getUiSettings().setCompassEnabled(false);
        this.googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            this.googleMap.setMyLocationEnabled(true);
        }

        centerMapOnUserLocation();
        fetchMarkers();
        handler.post(vehicleUpdateRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleMap != null) {
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
        markerArtist.onMarkerClick(marker);
        return true;
    }

    @Override
    public void onCameraIdle() {
        fetchMarkers();
        markerArtist.updateMarkerRotations();
    }

    @Override
    public void onCameraMove() {
        compassManager.updateAzimuth(googleMap.getCameraPosition().bearing);
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
            googleMap.setMyLocationEnabled(true);
            centerMapOnUserLocation();
        }
    }

    private void centerMapOnUserLocation() {
        if (googleMap == null) return;

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
                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(userLocation)
                                    .zoom(15f)
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
        if (marker != null && googleMap != null) {
            MarkerDataStandardized data = (MarkerDataStandardized) marker.getTag();
            float bearing = data != null ? data.getBearing() : 0f;

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                    new CameraPosition.Builder()
                            .target(marker.getPosition())
                            .bearing(bearing)
                            .tilt(60f)
                            .zoom(17f)
                            .build()
            ), 1000, null);
        }
    }

    private void fetchMarkers() {
        if (isFetching) return;
        isFetching = true;

        fetcher.fetchMarkers(new FetchingManager.OnMarkersListener() {
            @Override
            public void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList) {
                isFetching = false;
                markerArtist.showMarkers(markerDataStandardizedList);
                if (markerArtist.getMarkerIconCache().size() > 200) {
                    markerArtist.getMarkerIconCache().clear();
                }
            }

            @Override
            public void onErrorMarkersListener(String error) {
                isFetching = false;
                Log.e("MainActivity", "Erreur markers: " + error);
            }
        });
    }

    public GoogleMap getMap() {
        return googleMap;
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

    public FavoriteManager getFavoriteManager() {
        return favoriteManager;
    }
}