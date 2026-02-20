package fr.ynryo.ouestcefdpdetram;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;

public class MarkerArtist {
    private View cachedMarkerView;

    private final MainActivity context;
    private GoogleMap mMap;
    private final FollowManager followManager;
    private final NetworkFilterDrawer networkFilterDrawer;
    private final Map<String, BitmapDescriptor> markerIconCache = new HashMap<>();
    private final Map<String, Marker> activeMarkers = new HashMap<>();
    private final Map<String, ValueAnimator> activeAnimators = new HashMap<>();

    private float oldMapRotation = 0;

    public MarkerArtist(MainActivity context, FollowManager followManager, NetworkFilterDrawer networkFilterDrawer) {
        this.context = context;
        this.followManager = followManager;
        this.networkFilterDrawer = networkFilterDrawer;
    }

    public void showMarkers(List<MarkerData> markersFetched) {
        if (mMap == null || markersFetched == null) return;

        Set<String> fetchedMarkerIds = new HashSet<>();
        for (MarkerData fetchedMarkerData : markersFetched) { //on met tout les markers dans une liste
            fetchedMarkerIds.add(fetchedMarkerData.getId());
        }

        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) { //pour chaque marker fetched
            Map.Entry<String, Marker> entry = iterator.next();
            if(!fetchedMarkerIds.contains(entry.getKey())) { //si le marker n'est pas dans la liste des markers fetched
                if (entry.getKey().equals(followManager.getFollowedMarkerId())) {
                    followManager.setFollowedMarkerId(null);
                }
                entry.getValue().remove();
                iterator.remove();
            }
        }

        for (MarkerData fetchedMarkerData : markersFetched) { //pour chaque marker
            if (!networkFilterDrawer.isNetworkVisible(fetchedMarkerData.getNetworkRef())) { //si le network n'est pas autorisé d'affichage
                if (activeMarkers.containsKey(fetchedMarkerData.getId())) { //s'il était affiché, c'est ciao
                    if (fetchedMarkerData.getId().equals(followManager.getFollowedMarkerId())) {
                        followManager.setFollowedMarkerId(null);
                    }
                    activeMarkers.get(fetchedMarkerData.getId()).remove();
                    activeMarkers.remove(fetchedMarkerData.getId());
                }
                continue; //si il était pas là, chill
            }

            float mapRotation = mMap.getCameraPosition().bearing;
            LatLng position = new LatLng(fetchedMarkerData.getPosition().getLatitude(), fetchedMarkerData.getPosition().getLongitude());

            if (activeMarkers.containsKey(fetchedMarkerData.getId())) {
                Marker existingMarker = activeMarkers.get(fetchedMarkerData.getId());
                if (existingMarker != null) {
                    animateMarker(existingMarker, position, followManager.isFollowing(fetchedMarkerData.getId()));

                    //màj qui si nécéssaire
                    MarkerData oldData = (MarkerData) existingMarker.getTag();
                    if (oldData == null ||
                            !Objects.equals(oldData.getFillColor(), fetchedMarkerData.getFillColor()) ||
                            !Objects.equals(oldData.getLineNumber(), fetchedMarkerData.getLineNumber()) ||
                            Math.abs(oldData.getPosition().getBearing() - fetchedMarkerData.getPosition().getBearing()) > 5) {

                        existingMarker.setIcon(createCustomMarker(fetchedMarkerData, mapRotation, followManager.isFollowing(fetchedMarkerData.getId())));
                    }

                    existingMarker.setTag(fetchedMarkerData);
                }
            } else {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(createCustomMarker(fetchedMarkerData, mapRotation, followManager.isFollowing(fetchedMarkerData.getId())))
                        .anchor(0.5f, 0.3f));
                if (newMarker != null) {
                    newMarker.setTag(fetchedMarkerData);
                    activeMarkers.put(fetchedMarkerData.getId(), newMarker);
                }
            }
        }
    }

    public BitmapDescriptor createCustomMarker(MarkerData markerData, float mapRotation, boolean shouldFollow) {
        String cacheKey = markerData.getFillColor() + "_" + markerData.getLineNumber() + "_" + (int) (markerData.getPosition().getBearing() - mapRotation);

        if (markerIconCache.containsKey(cacheKey)) {
            return markerIconCache.get(cacheKey);
        }

        ImageView markerCircle = cachedMarkerView.findViewById(R.id.marker_circle);
        TextView lineNumberView = cachedMarkerView.findViewById(R.id.line_number);

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

        markerCircle.setRotation(shouldFollow ? 0 : bearing - mapRotation);

        cachedMarkerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        cachedMarkerView.layout(0, 0, cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(cachedMarkerView.getMeasuredWidth(), cachedMarkerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cachedMarkerView.draw(canvas);

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIconCache.put(cacheKey, descriptor);

        return descriptor;
    }

    public void updateMarkerRotations() {
        if (mMap == null) return;

        float mapRotation = mMap.getCameraPosition().bearing;
        if (mapRotation == oldMapRotation) return;
        oldMapRotation = mapRotation;

        for (Map.Entry<String, Marker> entry : activeMarkers.entrySet()) {
            Marker marker = entry.getValue();
            MarkerData data = (MarkerData) marker.getTag();
            if (data != null) {
                marker.setIcon(createCustomMarker(data, mapRotation, followManager.isFollowing(data.getId())));
            }
        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition, boolean shouldFollow) {
        final LatLng startPosition = marker.getPosition();
        String markerId = marker.getTag() != null ? ((MarkerData) marker.getTag()).getId() : "";

        ValueAnimator existing = activeAnimators.get(markerId);
        if (existing != null && existing.isRunning()) {
            existing.cancel();
        }

        if (shouldFollow && mMap != null) {
            MarkerData data = (MarkerData) marker.getTag();
            float bearing = data != null ? data.getPosition().getBearing() : 0f;

            mMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(toPosition)
                                    .bearing(bearing)
                                    .tilt(60f)
                                    .zoom(17f)
                                    .build()
                    ),
                    2000,
                    null
            );
        }

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(2000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();

            double lng = v * toPosition.longitude + (1 - v) * startPosition.longitude;
            double lat = v * toPosition.latitude + (1 - v) * startPosition.latitude;
            LatLng newPos = new LatLng(lat, lng);
            marker.setPosition(newPos);
        });

        activeAnimators.put(markerId, valueAnimator);
        valueAnimator.start();
    }
    public void setCachedMarkerView(View cachedMarkerView) {
        this.cachedMarkerView = cachedMarkerView;
    }

    public void setmMap(GoogleMap mMap) {
        this.mMap = mMap;
    }

    public Map<String, Marker> getActiveMarkers() {
        return activeMarkers;
    }

    public Map<String, BitmapDescriptor> getMarkerIconCache() {
        return markerIconCache;
    }
}
