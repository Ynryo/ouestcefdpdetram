package fr.ynryo.ouestcefdpdetram.artists;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.MainActivity;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.bus.BusGeometry;
import fr.ynryo.ouestcefdpdetram.managers.FetchingManager;

public class RouteArtist {
    private final static String TAG = "RouteArtist";
    private final MainActivity context;
    private String currentMarkerId;
    private Polyline currentRoutePolyline;

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
                    .zIndex(1.0f);

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
                                .zIndex(1.0f);

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

    public void remove() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }
    }

    public boolean isDrew(String markerId) {
        return currentMarkerId != null && currentMarkerId.equals(markerId);
    }

    public String getCurrentMarkerId() {
        return currentMarkerId;
    }
}