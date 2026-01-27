package fr.ynryo.ouestcefdpdetram;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.net.URLEncoder;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private MediaPlayer mediaPlayer;
    private static final String TAG = "MainActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String BASE_URL = "https://bus-tracker.fr/api/vehicle-journeys/";
    private final int UPDATE_INTERVAL = 5000; // 5 seconds
    private final int COLOR_GREEN = Color.rgb(15, 150, 40);
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private List<MarkerData> markersData = new ArrayList<MarkerData>();
    private Map<String, Marker> activeMarkers = new HashMap<>();
    private final Runnable vehicleUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            fullReloadMarkers();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    // init de la carte
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

    public void fullReloadMarkers() {
        fetchMarkersFromAPI();
    }

    private void fetchMarkersFromAPI() {
        if (mMap == null) return; // si pas de map return
        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        NaolibApiService service = retrofit.create(NaolibApiService.class);
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        Call<VehicleJourneyResponse> call = service.getVehicleMarkers(bounds.southwest.latitude, bounds.southwest.longitude, bounds.northeast.latitude, bounds.northeast.longitude);
        call.enqueue(new Callback<VehicleJourneyResponse>() {
            @Override
            public void onResponse(@NonNull Call<VehicleJourneyResponse> call, Response<VehicleJourneyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MarkerData> markersDataResponse = response.body().getItems(); // liste des marqueurs (que les datas)
                    markersData = markersDataResponse;
                    showMarkers(markersDataResponse);

                } else {
                    Log.e(TAG, "API call failed: " + response.code() + " " + response.errorBody());
                }
            }

            @Override
            public void onFailure(@NonNull Call<VehicleJourneyResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API call error", t);
            }
        });
    }

    private void fetchVehicleDetails(MarkerData marker) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);

        //set header text et color
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        tvLigne.setText(String.valueOf(marker.getLineNumber()));
        String fillColorString = marker.getFillColor();
        tvLigne.setBackgroundColor(Color.parseColor(fillColorString != null && !fillColorString.isEmpty() ? fillColorString : "#424242"));
        String textColorString = marker.getColor();
        tvLigne.setTextColor(Color.parseColor(textColorString != null && !textColorString.isEmpty() ? textColorString : "#FFFFFF"));

        //affichage du loader
        view.findViewById(R.id.loader).setVisibility(View.VISIBLE);
        view.findViewById(R.id.scrollStops).setVisibility(View.INVISIBLE);
        bottomSheetDialog.show();

        // Appel API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        NaolibApiService service = retrofit.create(NaolibApiService.class);

        try {
            String encodedId = URLEncoder.encode(marker.getId(), "UTF-8");
            Call<VehicleDetails> call = service.getVehicleDetails(encodedId);

            call.enqueue(new Callback<VehicleDetails>() {
                @Override
                public void onResponse(Call<VehicleDetails> call, Response<VehicleDetails> response) {
                    //loader off
                    view.findViewById(R.id.loader).setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                        showVehicleDetails(view, response.body());
                    } else {
                        Log.e(TAG, "API Error: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<VehicleDetails> call, Throwable t) {
                    view.findViewById(R.id.loader).setVisibility(View.GONE);
                    Log.e(TAG, "Network Error", t);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Encoding error", e);
        }
    }

    private void showVehicleDetails(View rootView, VehicleDetails details) {
        TextView tvDestination = rootView.findViewById(R.id.tvDestination);
        LinearLayout stopsContainer = rootView.findViewById(R.id.stopsContainer);
        View scrollStops = rootView.findViewById(R.id.scrollStops);

        tvDestination.setText(details.getDestination());
        stopsContainer.removeAllViews();

        if (details.getCalls() != null) {
            for (fr.ynryo.ouestcefdpdetram.Call stop : details.getCalls()) {
                LinearLayout.LayoutParams paramsMRight = new LinearLayout.LayoutParams(0, -1);
                // row create
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 16, 0, 16);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                // 1 nom de l'arrêt
                TextView tvStopName = new TextView(this);
                tvStopName.setText(stop.getStopName());
                tvStopName.setTextColor(Color.BLACK);
                tvStopName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

                // 2 icône de montée/descente (calls flags)
                ImageView inOutIcon = new ImageView(this);
                if (stop.getFlags().contains("NO_PICKUP")) {
                    inOutIcon.setImageResource(R.drawable.logout_24px);
                } else if (stop.getFlags().contains("NO_DROP_OFF")) {
                    inOutIcon.setImageResource(R.drawable.login_24px);
                }
                inOutIcon.setPadding(8, 0, 0, 0);
                inOutIcon.setColorFilter(Color.BLACK);

                // 3 espace vide flexible (Spacer)
                View spacer = new View(this);
                spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1f));

                // 4 icône expected time
                ImageView expectedTimeIcon = new ImageView(this);
                boolean isExpectedTime = stop.getExpectedTime() != null;
                if (isExpectedTime) {
                    expectedTimeIcon.setImageResource(R.drawable.sensors_24px);
                    expectedTimeIcon.setPadding(0, 0, 8, 0);
                    expectedTimeIcon.setColorFilter(COLOR_GREEN);
                }

                TextView tvDelay = new TextView(this);
                if (stop.getExpectedTime() != null && stop.getAimedTime() != null) {
                    try {
                        ZonedDateTime expected = ZonedDateTime.parse(stop.getExpectedTime());
                        ZonedDateTime aimed = ZonedDateTime.parse(stop.getAimedTime());

                        long diff = java.time.temporal.ChronoUnit.MINUTES.between(aimed, expected);

                        if (diff > 0) {
                            // Retard : on affiche "+X min"
                            tvDelay.setText("+" + diff + " min");
                            tvDelay.setTextColor(Color.RED);
                        } else if (diff < 0) {
                            // En avance : on affiche "-X min"
                            tvDelay.setText(diff + " min");
                            tvDelay.setTextColor(Color.BLUE);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur calcul retard", e);
                    }
                }


                // 5 heure
                TextView tvStopTime = new TextView(this);
                formatStopAndSetTime(tvStopTime, stop, isExpectedTime);
                tvStopTime.setTypeface(null, Typeface.BOLD);

                // row build
                row.addView(tvStopName);
                row.addView(inOutIcon);
                row.addView(spacer);
                row.addView(tvDelay, paramsMRight);
                row.addView(expectedTimeIcon);
                row.addView(tvStopTime);

                stopsContainer.addView(row);
            }
        }
        // On affiche enfin le contenu
        scrollStops.setVisibility(View.VISIBLE);
    }

    private void formatStopAndSetTime(TextView textView, fr.ynryo.ouestcefdpdetram.Call stop, boolean isExpected) {
        try {
            String rawTime = isExpected ? stop.getExpectedTime() : stop.getAimedTime();
            if (rawTime != null) {
                ZonedDateTime zdt = ZonedDateTime.parse(rawTime);
                String formatted = zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
                textView.setText(formatted);
                textView.setTextColor(isExpected ? COLOR_GREEN : Color.DKGRAY);
            } else {
                textView.setText("??:??");
                textView.setTextColor(Color.RED);
            }
        } catch (Exception e) {
            textView.setText("??:??");
        }
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
        MarkerData tag = (MarkerData) marker.getTag();
        String markerId = (String) tag.getId();
        Log.d(TAG, "Marker clicked with ID: " + markerId);
        fetchVehicleDetails(tag);
        // Retourne false pour permettre l'affichage de la fenêtre d'information par défaut
        return false;
    }

    @Override
    public void onCameraIdle() {
        fullReloadMarkers();
    }
}