package fr.ynryo.ouestcefdpdetram;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLngBounds;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerDataStandardized;
import fr.ynryo.ouestcefdpdetram.GenericMarkerDatas.MarkerType;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkerData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.markers.MarkersList;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.region.RegionData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponsesPOJO.version.VersionResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FetchingManager {
    private static final String BASE_URL_BUS_TRACKER = "https://bus-tracker.fr/api/";
    private static final String BASE_URL_CARTO_TCHOO = "https://api.tchoo.net/api/";
    private static final String BASE_URL_DL_YNRYO = "https://dl.ynryo.fr/api/ouestcefdpdetram/";

    private final MainActivity context;
    private static ApiService busTrackerService;
    private static ApiService cartoTchooService;
    private static ApiService dlYnryoService;

    public FetchingManager(MainActivity context) {
        this.context = context;
        if (busTrackerService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL_BUS_TRACKER)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            busTrackerService = retrofit.create(ApiService.class);
        }

        if (cartoTchooService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL_CARTO_TCHOO)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            cartoTchooService = retrofit.create(ApiService.class);
        }

        if (dlYnryoService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL_DL_YNRYO)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            dlYnryoService = retrofit.create(ApiService.class);
        }
    }

    // ==================== LISTENERS ====================
    public interface OnMarkersListener {
        /**
         * Appelé quand le fetch est réussi
         * @param markers List<MarkerDataStandardized> prêts à l'emploi
         */
        void onResponseMarkersListener(List<MarkerDataStandardized> markerDataStandardizedList);
        void onErrorMarkersListener(String error);
    }

    public interface OnVehicleDetailsListener {
        void onResponseVehicleDetailsListener(MarkerDataStandardized markerDataStandardized);
        void onErrorVehicleDetailsListener(String error);
    }

    public interface OnNetworkDataListener {
        void onResponseNetworkDataListener(NetworkData data);
        void onErrorNetworkDataListener(String error);
    }

    public interface OnRouteLineListener {
        void onResponseRouteLineListener(VehicleData data);
        void onErrorRouteLineListener(String error);
    }

    public interface OnNetworkListener {
        void onResponseNetworkListener(List<NetworkData> data);
        void onErrorNetworkListener(String error);
    }

    public interface OnRegionsListener {
        void onResponseRegionsListener(List<RegionData> regions);
        void onErrorRegionsListener(String error);
    }

    public interface OnVersionListener {
        void onResponseVersionListener(VersionResponse version);
        void onErrorVersionListener(String error);
    }

    private ApiService getService(String baseUrl) {
        switch (baseUrl) {
            case BASE_URL_BUS_TRACKER:
                return busTrackerService;

            case BASE_URL_CARTO_TCHOO:
                return cartoTchooService;

            case BASE_URL_DL_YNRYO:
                return dlYnryoService;

            default:
                return null;
        }
    }

    // ==================== FETCH MARKERS (PRINCIPAL) ====================

    /**
     * Fetch les marqueurs visibles dans les bounds de la caméra.
     * Convertit automatiquement MarkerData → MarkerDataStandardized
     *
     * @param listener Reçoit List<MarkerDataStandardized>
     */
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
                    // Convertir MarkerData en MarkerDataStandardized
                    List<MarkerDataStandardized> standardizedMarkers = convertMarkerDataList(
                            response.body().getItems()
                    );
                    listener.onResponseMarkersListener(standardizedMarkers);
                } else {
                    listener.onErrorMarkersListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<MarkersList> call, @NonNull Throwable t) {
                listener.onErrorMarkersListener(t.getMessage());
            }
        });
    }

    // ==================== FETCH VEHICLE DETAILS ====================
    public void fetchVehicleStopsInfo(MarkerDataStandardized markerDataStandardized, OnVehicleDetailsListener listener) {
        try {
            String encodedId = URLEncoder.encode(markerDataStandardized.getId(), "UTF-8");
            getService(BASE_URL_BUS_TRACKER).getVehicleDetails(encodedId).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<VehicleData> call, @NonNull Response<VehicleData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        VehicleData vehicleData = response.body();
                        markerDataStandardized.setVehicleDetails(vehicleData);
                        listener.onResponseVehicleDetailsListener(markerDataStandardized);
                    } else {
                        listener.onErrorVehicleDetailsListener(String.valueOf(response.code()));
                    }
                }

                @Override
                public void onFailure(@NonNull Call<VehicleData> call, @NonNull Throwable t) {
                    listener.onErrorVehicleDetailsListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorVehicleDetailsListener(e.getMessage());
        }
    }

    // ==================== FETCH NETWORK DATA ====================
    public void fetchNetworkData(int networkId, OnNetworkDataListener listener) {
        getService(BASE_URL_BUS_TRACKER).getNetworkData(networkId).enqueue(new Callback<NetworkData>() {
            @Override
            public void onResponse(@NonNull Call<NetworkData> call, @NonNull Response<NetworkData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    listener.onResponseNetworkDataListener(response.body());
                } else {
                    listener.onErrorNetworkDataListener("Erreur réponse: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<NetworkData> call, @NonNull Throwable t) {
                listener.onErrorNetworkDataListener(t.getMessage());
            }
        });
    }

    // ==================== FETCH ROUTE LINE ====================
    public void fetchRouteLine(String routeId, OnRouteLineListener listener) {
        try {
            getService(BASE_URL_CARTO_TCHOO).getRouteLine(Integer.parseInt(routeId)).enqueue(new Callback<VehicleData>() {
                @Override
                public void onResponse(@NonNull Call<VehicleData> call, @NonNull Response<VehicleData> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseRouteLineListener(response.body());
                    } else {
                        listener.onErrorRouteLineListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<VehicleData> call, @NonNull Throwable t) {
                    listener.onErrorRouteLineListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorRouteLineListener(e.getMessage());
        }
    }

    // ==================== FETCH NETWORKS ====================
    public void fetchNetworks(OnNetworkListener listener) {
        try {
            getService(BASE_URL_BUS_TRACKER).getNetworks().enqueue(new Callback<List<NetworkData>>() {
                @Override
                public void onResponse(@NonNull Call<List<NetworkData>> call, @NonNull Response<List<NetworkData>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseNetworkListener(response.body());
                    } else {
                        listener.onErrorNetworkListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<NetworkData>> call, @NonNull Throwable t) {
                    listener.onErrorNetworkListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorNetworkListener(e.getMessage());
        }
    }

    // ==================== FETCH REGIONS ====================
    public void fetchRegions(OnRegionsListener listener) {
        try {
            getService(BASE_URL_BUS_TRACKER).getRegions().enqueue(new Callback<List<RegionData>>() {
                @Override
                public void onResponse(@NonNull Call<List<RegionData>> call, @NonNull Response<List<RegionData>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseRegionsListener(response.body());
                    } else {
                        listener.onErrorRegionsListener("Erreur régions: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<RegionData>> call, @NonNull Throwable t) {
                    listener.onErrorRegionsListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorRegionsListener(e.getMessage());
        }
    }

    // ==================== FETCH VERSION ====================
    public void fetchLatestVersion(OnVersionListener listener) {
        try {
            getService(BASE_URL_DL_YNRYO).getLatestVersion().enqueue(new Callback<VersionResponse>() {
                @Override
                public void onResponse(@NonNull Call<VersionResponse> call, @NonNull Response<VersionResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        listener.onResponseVersionListener(response.body());
                    } else {
                        listener.onErrorVersionListener("Code erreur: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<VersionResponse> call, @NonNull Throwable t) {
                    listener.onErrorVersionListener(t.getMessage());
                }
            });
        } catch (Exception e) {
            listener.onErrorVersionListener(e.getMessage());
        }
    }

    // ==================== HELPER METHODS (CONVERSION) ====================
    private List<MarkerDataStandardized> convertMarkerDataList(List<MarkerData> markerDataList) {
        List<MarkerDataStandardized> result = new ArrayList<>();

        if (markerDataList == null || markerDataList.isEmpty()) {
            return result;
        }

        for (MarkerData markerData : markerDataList) {
            try {
                // Déterminer le type automatiquement
                MarkerType type = MarkerType.fromMarkerId(markerData.getId());

                // Créer le marqueur standardisé
                MarkerDataStandardized standardized = MarkerDataStandardized.from(markerData, type);

                result.add(standardized);
            } catch (Exception e) {
                // Si la conversion échoue pour un marqueur, on skip et on continue
                Log.e("FetchingManager", "Erreur conversion MarkerData -> MarkerDataStandardized: " + e.getMessage());
            }
        }

        return result;
    }
}