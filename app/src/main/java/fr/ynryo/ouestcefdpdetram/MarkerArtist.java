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

import androidx.annotation.NonNull;
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

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;

public class MarkerArtist {
    private static final String TAG = "MarkerArtist";
    private View cachedMarkerView;
    private final MainActivity context;
    private GoogleMap mMap;
    private final FollowManager followManager;
    private final NetworkFilterDrawer networkFilterDrawer;
    private final RouteArtist routeArtist;
    private final MarkerStopsDetailActivity markerStopsDetailActivity;
    private final Map<String, BitmapDescriptor> markerIconCache = new HashMap<>();
    private final Map<String, Marker> activeMarkers = new HashMap<>();
    private final Map<String, ValueAnimator> activeAnimators = new HashMap<>();

    private float oldMapRotation = 0;

    public MarkerArtist(MainActivity context, FollowManager followManager, NetworkFilterDrawer networkFilterDrawer) {
        this.context = context;
        this.followManager = followManager;
        this.networkFilterDrawer = networkFilterDrawer;
        this.routeArtist = new RouteArtist(context);
        this.markerStopsDetailActivity = new MarkerStopsDetailActivity(context);
    }

    public void showMarkers(List<MarkerDataStandardized> markerDataStandardizedList) {
        if (mMap == null || markerDataStandardizedList == null) return;

        Set<String> fetchedMarkerIds = new HashSet<>();
        for (MarkerDataStandardized fetchedMarkerDataStandardized : markerDataStandardizedList) { //on met tout les markers dans une liste
            fetchedMarkerIds.add(fetchedMarkerDataStandardized.getId());
        }

        Iterator<Map.Entry<String, Marker>> iterator = activeMarkers.entrySet().iterator();
        while (iterator.hasNext()) { //pour chaque marker fetched
            Map.Entry<String, Marker> entry = iterator.next();
            if(!fetchedMarkerIds.contains(entry.getKey())) { //si le marker n'est pas dans la liste des markers fetched
                if (entry.getKey().equals(followManager.getFollowedMarkerId())) followManager.disableFollow(false);
                if (entry.getKey().equals(routeArtist.getCurrentMarkerId())) routeArtist.remove();
                if (entry.getKey().equals(markerStopsDetailActivity.getCurrentVehicleId())) markerStopsDetailActivity.close();
                entry.getValue().remove();
                iterator.remove();
            }
        }

        for (MarkerDataStandardized fetchedMarkerDataStandardized : markerDataStandardizedList) { //pour chaque marker
            if (!networkFilterDrawer.isNetworkVisible(fetchedMarkerDataStandardized.getNetworkRef())) { //si le network n'est pas autorisé d'affichage
                if (activeMarkers.containsKey(fetchedMarkerDataStandardized.getId())) { //s'il était affiché, c'est ciao
                    if (fetchedMarkerDataStandardized.getId().equals(followManager.getFollowedMarkerId())) followManager.disableFollow(false);
                    if (fetchedMarkerDataStandardized.getId().equals(routeArtist.getCurrentMarkerId())) routeArtist.remove();
                    if (fetchedMarkerDataStandardized.getId().equals(markerStopsDetailActivity.getCurrentVehicleId())) markerStopsDetailActivity.close();
                    activeMarkers.get(fetchedMarkerDataStandardized.getId()).remove();
                    activeMarkers.remove(fetchedMarkerDataStandardized.getId());
                }
                continue; //si il était pas là, chill
            }

            float mapRotation = mMap.getCameraPosition().bearing;
            LatLng position = new LatLng(fetchedMarkerDataStandardized.getLatitude(), fetchedMarkerDataStandardized.getLongitude());

            if (activeMarkers.containsKey(fetchedMarkerDataStandardized.getId())) {
                Marker existingMarker = activeMarkers.get(fetchedMarkerDataStandardized.getId());
                if (existingMarker != null) {
                    animateMarker(existingMarker, position, followManager.isFollowing(fetchedMarkerDataStandardized.getId()));

                    //màj qui si nécéssaire
                    MarkerDataStandardized oldData = (MarkerDataStandardized) existingMarker.getTag();
                    if (oldData == null ||
                            !Objects.equals(oldData.getFillColor(), fetchedMarkerDataStandardized.getFillColor()) ||
                            !Objects.equals(oldData.getLineId(), fetchedMarkerDataStandardized.getLineId()) ||
                            Math.abs(oldData.getBearing() - fetchedMarkerDataStandardized.getBearing()) > 5) {

                        existingMarker.setIcon(createCustomMarker(fetchedMarkerDataStandardized, mapRotation, followManager.isFollowing(fetchedMarkerDataStandardized.getId())));
                    }

                    existingMarker.setTag(fetchedMarkerDataStandardized);
                }
            } else {
                Marker newMarker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(createCustomMarker(fetchedMarkerDataStandardized, mapRotation, followManager.isFollowing(fetchedMarkerDataStandardized.getId())))
                        .anchor(0.5f, 0.3f));
                if (newMarker != null) {
                    newMarker.setTag(fetchedMarkerDataStandardized);
                    activeMarkers.put(fetchedMarkerDataStandardized.getId(), newMarker);
                }
            }
        }
    }

    public BitmapDescriptor createCustomMarker(MarkerDataStandardized markerDataStandardized, float mapRotation, boolean shouldFollow) {
        String cacheKey = markerDataStandardized.getFillColor() + "_" + markerDataStandardized.getLineId() + "_" + (int) (markerDataStandardized.getBearing() - mapRotation);

        if (markerIconCache.containsKey(cacheKey)) {
            return markerIconCache.get(cacheKey);
        }

        ImageView markerCircle = cachedMarkerView.findViewById(R.id.marker_circle);
        TextView lineNumberView = cachedMarkerView.findViewById(R.id.line_number);

        int fillColor = Color.parseColor(markerDataStandardized.getFillColor() != null ? markerDataStandardized.getFillColor() : "#424242");
        int textColor = Color.parseColor(markerDataStandardized.getTextColor() != null ? markerDataStandardized.getTextColor() : "#FFFFFF");
        float bearing = markerDataStandardized.getBearing();

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

        lineNumberView.setText(markerDataStandardized.getLineId() != null ? markerDataStandardized.getLineId() : "BD");
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
            MarkerDataStandardized data = (MarkerDataStandardized) marker.getTag();
            if (data != null) {
                marker.setIcon(createCustomMarker(data, mapRotation, followManager.isFollowing(data.getId())));
            }
        }
    }

    public void onMarkerClick(@NonNull Marker marker) {
        MarkerDataStandardized mData = (MarkerDataStandardized) marker.getTag();
        if (mData != null) {
            markerStopsDetailActivity.open(mData);

            if (mData.isTrain()) {
//                routeArtist.drawVehicleRoute(mData);
            } else {
                routeArtist.remove();
            }
        }
    }

    public void animateMarker(final Marker marker, final LatLng toPosition, boolean shouldFollow) {
        final LatLng startPosition = marker.getPosition();
        String markerId = marker.getTag() != null ? ((MarkerDataStandardized) marker.getTag()).getId() : "";

        ValueAnimator existing = activeAnimators.get(markerId);
        if (existing != null && existing.isRunning()) {
            existing.cancel();
        }

        if (shouldFollow && mMap != null) {
            MarkerDataStandardized data = (MarkerDataStandardized) marker.getTag();
            float bearing = data != null ? data.getBearing() : 0f;

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

    public MarkerStopsDetailActivity getVehicleDetailsManager() {
        return markerStopsDetailActivity;
    }
}
