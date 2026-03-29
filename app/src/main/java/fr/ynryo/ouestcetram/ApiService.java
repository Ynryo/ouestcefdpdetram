package fr.ynryo.ouestcetram;

import java.util.List;

import fr.ynryo.ouestcetram.apiResponsesPOJO.bus.BusGeometry;
import fr.ynryo.ouestcetram.apiResponsesPOJO.markers.MarkersList;
import fr.ynryo.ouestcetram.apiResponsesPOJO.network.NetworkData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.region.RegionData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.train.TrainData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.vehicle.VehicleData;
import fr.ynryo.ouestcetram.apiResponsesPOJO.version.VersionResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface de l'API, gère les requêtes et les réponses
 */
public interface ApiService {
    @GET("vehicle-journeys/markers")
    Call<MarkersList> getVehicleMarkers(
        @Query("swLat") double swLat,
        @Query("swLon") double swLon,
        @Query("neLat") double neLat,
        @Query("neLon") double neLon,
        @Query("lineId") String lineId
    );

    @GET("vehicle-journeys/{vehicleId}")
    Call<VehicleData> getVehicleDetails(
        @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("carto.php?action=train")
    Call<TrainData> getVehicleDetails(
        @Query("numero") int vehicleNumber
    );

    @GET("regions")
    Call<List<RegionData>> getRegions();

    @GET("networks")
    Call<List<NetworkData>> getNetworks();

    @GET("networks/{networkId}?withDetails=true")
    Call<NetworkData> getNetworkData(
        @Path(value = "networkId") int networkId
    );

    @GET("paths/{pathRef}")
    Call<BusGeometry> getBusLine(
        @Path(value = "pathRef", encoded = true) String pathRef
    );

    @GET("version/latest")
    Call<VersionResponse> getLatestVersion();
}
