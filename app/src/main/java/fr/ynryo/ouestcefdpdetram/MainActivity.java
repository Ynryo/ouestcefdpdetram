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
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnCameraMoveListener {

    private GoogleMap mMap;
    private static final String BASE_URL = "https://bus-tracker.fr/api/vehicle-journeys/";
    private static final String TAG = "MainActivity";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int UPDATE_INTERVAL = 5000; // 5 seconds

    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MediaPlayer mediaPlayer;
    private List<Marker> listMarkers = new ArrayList<Marker>();
    private List<MarkerData> markersData = new ArrayList<MarkerData>();
    private Map<String, Marker> activeMarkers = new HashMap<>();


    // thread pour mettre à jour les marqueurs
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

//        try {
//            mediaPlayer = MediaPlayer.create(this, R.raw.hub_intro_sound);
//            if (mediaPlayer != null) {
//                mediaPlayer.setLooping(false); // Ne pas répéter
//                mediaPlayer.start();
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "Erreur lors de la lecture du fichier audio", e);
//        }

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
        mMap.setOnCameraMoveListener(this);
        mMap.getUiSettings().setMyLocationButtonEnabled(false); // Désactive le bouton de localisation par défaut
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

    // mise à jour des marqueurs_çucdbg
    @Override
    public void onCameraIdle() {
        fullReloadMarkers();
    }

    @Override
    public void onCameraMove() {
        showMarkers(markersData);
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
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

    private void showMarkers(List<MarkerData> markersFetched) {
        if (mMap == null) return; // si pas de map return
        if (markersFetched == null) return;
        // TODO: ne pas clear les marqueurs qui sont dans la nouvelle requete
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

    private void fetchVehicleDetails(MarkerData marker) {
        // création de la BottomSheet
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.vehicule_details, null);
        bottomSheetDialog.setContentView(view);

        // récupération des vues du layout
        ProgressBar loader = view.findViewById(R.id.loader);
        View scrollStops = view.findViewById(R.id.scrollStops);
        TextView tvLigne = view.findViewById(R.id.tvLigneNumero);
        TextView tvDestination = view.findViewById(R.id.tvDestination);
        LinearLayout stopsContainer = view.findViewById(R.id.stopsContainer);

        tvLigne.setText(String.valueOf(marker.getLineNumber()));
        tvLigne.setBackgroundColor(Color.parseColor(marker.getFillColor()));
        tvLigne.setTextColor(Color.parseColor(marker.getColor()));

        // on montre le loader, on cache la liste
        loader.setVisibility(View.VISIBLE);
        scrollStops.setVisibility(View.INVISIBLE);
        bottomSheetDialog.show();

        Retrofit retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        NaolibApiService service = retrofit.create(NaolibApiService.class);
        try {
            String encodedId = URLEncoder.encode(marker.getId(), "UTF-8");
            Log.i(TAG, "Encodage de l'ID: " + encodedId);
            Call<VehicleDetails> call = service.getVehicleDetails(encodedId);

            call.enqueue(new Callback<VehicleDetails>() {
                @Override
                public void onResponse(Call<VehicleDetails> call, Response<VehicleDetails> response) {
                    loader.setVisibility(View.GONE); // invisible et ne prends pas d'espace (display none)
                    if (response.isSuccessful() && response.body() != null) {
                        VehicleDetails details = response.body();

                        tvDestination.setText(details.getDestination());

                        // remplissage dynamique des arrêts
                        stopsContainer.removeAllViews(); // remove exemples du xml

                        if (details.getCalls() != null) {
                            for (fr.ynryo.ouestcefdpdetram.Call stop : details.getCalls()) {
                                // Création d'une ligne d'arrêt (Row)
                                LinearLayout row = new LinearLayout(MainActivity.this);
                                row.setOrientation(LinearLayout.HORIZONTAL);
                                row.setPadding(0, 16, 0, 16); // Un peu d'espacement

                                // Nom de l'arrêt
                                TextView tvStopName = new TextView(MainActivity.this);
                                tvStopName.setText(stop.getStopName());
                                tvStopName.setTextColor(Color.BLACK);
                                tvStopName.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                                // Heure de l'arrêt (Formatage de 2026-01-22T18:43:00+01:00 vers 18:43)
                                TextView tvStopTime = new TextView(MainActivity.this);
                                try {
                                    ZonedDateTime zdt;
                                    String formattedTime;
                                    if (stop.getExpectedTime() != null) {
                                        zdt = ZonedDateTime.parse(stop.getExpectedTime());
                                        formattedTime = zdt.format(DateTimeFormatter.ofPattern("HH:mm"));
                                        tvStopTime.setTextColor(Color.rgb(15, 150, 40));
                                        tvStopTime.setText(formattedTime);
                                    } else if (stop.getAimedTime() != null){
                                        zdt = ZonedDateTime.parse(stop.getAimedTime());
                                        formattedTime = zdt.format(DateTimeFormatter.ofPattern("HH:mm")) + " (prévue)";

                                        SpannableString spanString = new SpannableString(formattedTime);
                                        spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);

                                        tvStopTime.setTextColor(Color.rgb(255, 156, 56));
                                        tvStopTime.setText(spanString);
                                    } else {
                                        tvStopTime.setTextColor(Color.rgb(245, 74, 69));
                                        SpannableString spanString = new SpannableString("??:??");
                                        spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
                                        tvStopTime.setText(spanString);
                                    }
                                } catch (Exception e) {
                                    tvStopTime.setText("??:??");
                                    SpannableString spanString = new SpannableString("");
                                    spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
                                    tvStopTime.setText(spanString);
                                }
                                tvStopTime.setTypeface(null, Typeface.BOLD);

                                // Ajout des textes dans la ligne
                                row.addView(tvStopName);
                                row.addView(tvStopTime);

                                // Ajout la ligne dans le conteneur
                                stopsContainer.addView(row);
                            }
                        }

                        // afficher la fenêtre
                        scrollStops.setVisibility(View.VISIBLE);
                        bottomSheetDialog.show();

                    } else {
                        String errorBody = "Unknown error";
                        try {
                            if (response.errorBody() != null) {
                                errorBody = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading error body for vehicle details", e);
                        }
                        Log.e(TAG, "API call failed for details (" + marker.getId() + "): " + response.code() + " " + errorBody);
                    }
                    return;
                }

                @Override
                public void onFailure(Call<VehicleDetails> call, Throwable t) {
                    Log.e(TAG, "API call error for details (" + marker.getId() + ")", t);
                }
            });
        }catch (Exception e) {
            Log.e(TAG, "Erreur d'encodage de l'ID", e);
        }

    }

    //create marker
    public static BitmapDescriptor createCustomMarker(Context context, MarkerData markerData, float mapRotation) {
        View markerLayout = LayoutInflater.from(context).inflate(R.layout.custom_marker, null);
        ImageView markerCircle = markerLayout.findViewById(R.id.marker_circle);
        TextView lineNumberView = markerLayout.findViewById(R.id.line_number);
        int fillColor = Color.parseColor(markerData.getFillColor());
        int textColor = Color.parseColor(markerData.getColor());
        float bearing = markerData.getPosition().getBearing();

        // ici on teinte
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        LayerDrawable layerDrawable = (LayerDrawable) drawable.mutate();
        Drawable backgroundPart = layerDrawable.findDrawableByLayerId(R.id.marker_background);
        if (backgroundPart != null) {
            DrawableCompat.setTint(backgroundPart, fillColor);
        }
        markerCircle.setImageDrawable(layerDrawable);

        // 4. Configuration du texte (numéro de ligne)
        lineNumberView.setText(markerData.getLineNumber());
        lineNumberView.setTextColor(textColor);
        int paddingPx = dpToPx(context, 5);
        lineNumberView.setPadding(paddingPx, paddingPx / 2, paddingPx, paddingPx / 2);


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
}