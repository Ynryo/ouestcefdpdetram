package fr.ynryo.ouestcefdpdetram;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.route.RouteData;
import fr.ynryo.ouestcefdpdetram.apiResponses.route.RouteFeature;
import fr.ynryo.ouestcefdpdetram.apiResponses.route.RouteGeometry;

public class RouteArtist {
    private final MainActivity context;
    private Polyline currentRoutePolyline;

    public RouteArtist(MainActivity context) {
        this.context = context;
    }

    public void drawVehicleRoute(MarkerData mData) {
        context.getFetcher().fetchRouteLine(mData.getVehicleNumber(), new FetchingManager.OnRouteLineListener() {
            @Override
            public void onDetailsReceived(RouteData rData) {
                if (rData.getRouteFeatures() != null && !rData.getRouteFeatures().isEmpty()) {
                    PolylineOptions options = new PolylineOptions()
                            .width(12)
                            .color(Color.parseColor(mData.getFillColor()))
                            .geodesic(true)
                            .zIndex(1.0f);

                    boolean pointsAdded = false;

                    clear();
                    for (RouteFeature feature : rData.getRouteFeatures()) {
                        RouteGeometry geometry = feature.getRouteGeometry();

                        if (geometry != null && "LineString".equals(geometry.getType())) {
                            try {
                                List<List<Double>> allPoints;
                                if (geometry.getCoordinates() instanceof List) {
                                    allPoints = (List<List<Double>>) geometry.getCoordinates();
                                    for (List<Double> point : allPoints) {
                                        options.add(new LatLng(point.get(1), point.get(0))); //switch lat long
                                        pointsAdded = true;
                                    }
                                }
                            } catch (ClassCastException e) {
                                clear();
                                Log.e("RouteArtist", "Format de coordonnées invalide pour LineString");
                            }
                        }
                    }
                    if (pointsAdded) {
                        currentRoutePolyline = context.getMap().addPolyline(options);
                    }
                }
            }

            @Override
            public void onError(String error) {
                clear();
                Log.e("RouteArtist", "Erreur lors de la récuperation du tracé\n" + error);
            }
        });
    }

    public void clear() {
        if (currentRoutePolyline != null) {
            currentRoutePolyline.remove();
        }
    }
}