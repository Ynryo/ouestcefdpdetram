package fr.ynryo.ouestcefdpdetram.artists;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStop;
import fr.ynryo.ouestcefdpdetram.MainActivity;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.bus.BusGeometry;
import fr.ynryo.ouestcefdpdetram.managers.FetchingManager;

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
                    .flat(true)
                    .zIndex(3.0f));
            if (marker != null) stopMarkers.add(marker);
        }
    }

    private BitmapDescriptor createStopIcon(String stopName, int lineColor) {
        int dotSize = 16;
        float density = context.getResources().getDisplayMetrics().density;
        int dotPx = (int) (dotSize * density);
        int padding = (int) (6 * density);
        int textSize = (int) (13 * density);
        int cornerRadius = (int) (4 * density);

        // mesurer txt
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        float textWidth = textPaint.measureText(stopName);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        int textHeight = (int) (fm.descent - fm.ascent);

        int labelWidth = (int) (textWidth + padding * 2);
        int labelHeight = (int) (textHeight + padding);
        int totalWidth = dotPx + (int) (4 * density) + labelWidth;
        int totalHeight = Math.max(dotPx, labelHeight);

        Bitmap bitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        //draw circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.WHITE);
        circlePaint.setStyle(Paint.Style.FILL);
        float cy = totalHeight / 2f;
        canvas.drawCircle(dotPx / 2f, cy, dotPx / 2f, circlePaint);

        //circle border
        circlePaint.setColor(lineColor);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(3 * density);
        canvas.drawCircle(dotPx / 2f, cy, dotPx / 2f - (1.5f * density), circlePaint);

        //fond du label
        int labelLeft = dotPx + (int) (4 * density);
        RectF labelRect = new RectF(
                labelLeft, cy - labelHeight / 2f,
                labelLeft + labelWidth, cy + labelHeight / 2f);

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(lineColor);
        bgPaint.setAlpha(220);
        canvas.drawRoundRect(labelRect, cornerRadius, cornerRadius, bgPaint);

        //texte
        float textX = labelLeft + padding;
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(stopName, textX, textY, textPaint);

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