package fr.ynryo.ouestcetram.artists;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcetram.GenericMarkerDatas.MarkerDataStop;
import fr.ynryo.ouestcetram.MainActivity;
import fr.ynryo.ouestcetram.R;
import fr.ynryo.ouestcetram.apiResponsesPOJO.bus.BusGeometry;
import fr.ynryo.ouestcetram.managers.FetchingManager;

public class RouteArtist {
    private final static String TAG = "RouteArtist";
    private final MainActivity context;
    private String currentMarkerId;
    private Polyline currentRoutePolyline;
    private final List<Marker> stopMarkers = new ArrayList<>();

    public RouteArtist(MainActivity context) {
        this.context = context;
    }

    public void drawVehicleRoute(MarkerDataStandardized mData) {
        if (mData == null) return;
        if (mData.isTrain()) {
            PolylineOptions options = new PolylineOptions()
                    .width(12)
                    .color(Color.parseColor(mData.getFillColor()))
                    .geodesic(true)
                    .zIndex(2.0f);

            boolean pointsAdded = false;

            this.remove();
            try {
                List<List<Double>> allPoints;
                Object geometry = mData.getMarkerDataRoute();
                if (geometry instanceof List) {
                    allPoints = (List<List<Double>>) geometry;
                    for (List<Double> point : allPoints) {
                        options.add(new LatLng(point.get(1), point.get(0))); // switch lat long
                        pointsAdded = true;
                    }
                }
            } catch (ClassCastException e) {
                this.remove();
                Log.e(TAG, "Format de coordonnées invalide pour LineString");
            }
            if (pointsAdded) {
                currentMarkerId = mData.getId();
                currentRoutePolyline = context.getMap().addPolyline(options);
                drawStopCircles(mData, mData.getFillColor() != null ? Color.parseColor(mData.getFillColor()) : Color.parseColor("#424242")); // ← ajouter ici
            }
        } else if (mData.isVehicle() || mData.getPathRef() != null) {
            context.getFetcher().fetchBusLine(mData, new FetchingManager.OnRouteLineListener() {
                @Override
                public void onResponseRouteLineListener(MarkerDataStandardized mData) {
                    if (mData.getMarkerDataRoute() != null) {
                        PolylineOptions options = new PolylineOptions()
                                .width(12)
                                .color(Color.parseColor(mData.getFillColor()))
                                .geodesic(true)
                                .zIndex(2.0f);

                        boolean pointsAdded = false;

                        remove();
                        try {
                            List<List<Double>> allPoints;
                            Object geometry = ((BusGeometry) mData.getMarkerDataRoute()).getGeometry();
                            if (geometry instanceof List) {
                                allPoints = (List<List<Double>>) geometry;
                                for (List<Double> point : allPoints) {
                                    options.add(new LatLng(point.get(0), point.get(1)));
                                    pointsAdded = true;
                                }
                            }
                        } catch (ClassCastException e) {
                            remove();
                            Log.e(TAG, "Format de coordonnées invalide pour LineString");
                        }
                        if (pointsAdded) {
                            currentMarkerId = mData.getId();
                            currentRoutePolyline = context.getMap().addPolyline(options);
                            drawStopCircles(mData, mData.getFillColor() != null ? Color.parseColor(mData.getFillColor()) : Color.parseColor("#424242"));
                        }
                    }
                }

                @Override
                public void onErrorRouteLineListener(String error) {
                    remove();
                    Log.e(TAG, "Erreur lors de la récuperation du tracé\n" + error);
                }
            });
        }
    }

    private void drawStopCircles(MarkerDataStandardized mData, int color) {
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();

        List<MarkerDataStop> stops = mData.getStops();
        if (stops == null || stops.isEmpty()) return;

        for (MarkerDataStop stop : stops) {
            if (stop.getLatitude() == 0 && stop.getLongitude() == 0) continue;

            BitmapDescriptor icon = createStopIcon(stop.getStopName(), color);

            Marker marker = context.getMap().addMarker(new MarkerOptions()
                    .position(new LatLng(stop.getLatitude(), stop.getLongitude()))
                    .icon(icon)
                    .anchor(0.0f, 0.5f) // label à droite du point
                    .flat(false)
                    .zIndex(3.0f));
            if (marker != null) stopMarkers.add(marker);
        }
    }

    private BitmapDescriptor createStopIcon(String stopName, int lineColor) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stop_marker, null);

        //point
        View dot = view.findViewById(R.id.stop_dot);
        GradientDrawable dotDrawable = new GradientDrawable();
        dotDrawable.setShape(GradientDrawable.OVAL);
        dotDrawable.setColor(Color.WHITE);
        dotDrawable.setStroke((int) (3 * context.getResources().getDisplayMetrics().density), lineColor);
        dot.setBackground(dotDrawable);

        //background label
        TextView tvName = view.findViewById(R.id.stop_name);
        tvName.setText(stopName);
        GradientDrawable labelDrawable = new GradientDrawable();
        labelDrawable.setShape(GradientDrawable.RECTANGLE);
        labelDrawable.setColor(lineColor);
        labelDrawable.setAlpha(220);
        labelDrawable.setCornerRadius(8);
        tvName.setBackground(labelDrawable);

        //to bitmap
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(
                view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void remove() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
            currentRoutePolyline = null;
        }
        for (Marker m : stopMarkers) m.remove();
        stopMarkers.clear();
        currentMarkerId = null;
    }

    public boolean isDrew(String markerId) {
        return currentMarkerId != null && currentMarkerId.equals(markerId);
    }

    public String getCurrentMarkerId() {
        return currentMarkerId;
    }
}