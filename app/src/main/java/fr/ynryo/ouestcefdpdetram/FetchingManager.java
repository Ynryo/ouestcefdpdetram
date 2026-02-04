package fr.ynryo.ouestcefdpdetram;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkersList;
import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.route.RouteData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchingManager {
    private static final String BASE_URL_BUS_TRACKER = "https://bus-tracker.fr/api/";
    private static final String BASE_URL_CARTO_TCHOO = "https://api.tchoo.net/api/";
    private final MainActivity context;

    public FetchingManager(MainActivity context) {
        this.context = context;
    }

    public interface OnMarkersListener {
        void onMarkersReceived(List<MarkerData> markers);
        void onError(String error);
    }

    public interface OnVehicleDetailsListener {
        void onDetailsReceived(VehicleData details);
        void onError(String error);
    }

    public interface OnNetworkDataListener {
        void onDetailsReceived(NetworkData data);
        void onError(String error);
    }

    public interface OnRouteLineListener {
        void onDetailsReceived(RouteData data);
        void onError(String error);
    }

    private ApiService getService(String base_url) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(base_url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(ApiService.class);
    }

    public void fetchMarkers(OnMarkersListener listener) {
        if (context.getMap() == null) return;

        LatLngBounds bounds = context.getMap().getProjection().getVisibleRegion().latLngBounds;
        getService(BASE_URL_BUS_TRACKER).getVehicleMarkers(
                bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude
        ).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<MarkersList> call, @NonNull Response<MarkersList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onMarkersReceived(response.body().getItems());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarkersList> call, @NonNull Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void fetchVehicleStopsInfo(MarkerData marker, OnVehicleDetailsListener listener) {
        try {
            String encodedId = URLEncoder.encode(marker.getId(), StandardCharsets.UTF_8);
            getService(BASE_URL_BUS_TRACKER).getVehicleDetails(encodedId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<VehicleData> call, @NonNull Response<VehicleData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onDetailsReceived(response.body());
                    } else {
                        listener.onError("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<VehicleData> call, @NonNull Throwable t) {
                    listener.onError(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onError(e.getMessage());
        }
    }

    public void fetchNetworkData(int networkId, OnNetworkDataListener listener) {
        getService(BASE_URL_BUS_TRACKER).getNetworkData(networkId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<NetworkData> call, @NonNull Response<NetworkData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onDetailsReceived(response.body());
                } else {
                    listener.onError("Code erreur: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<NetworkData> call, @NonNull Throwable t) {
                listener.onError(t.getMessage());
            }
        });
    }

    public void fetchRouteLine(String routeId, OnRouteLineListener listener) {
        try {
            getService(BASE_URL_CARTO_TCHOO).getRouteLine(Integer.parseInt(routeId)).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<RouteData> call, @NonNull Response<RouteData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onDetailsReceived(response.body());
                    } else {
                        listener.onError("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<RouteData> call, @NonNull Throwable t) {
                    listener.onError(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onError(e.getMessage());
        }
    }
}