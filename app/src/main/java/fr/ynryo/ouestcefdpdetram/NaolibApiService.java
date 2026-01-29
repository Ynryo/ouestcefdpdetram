package fr.ynryo.ouestcefdpdetram;

import fr.ynryo.ouestcefdpdetram.apiResponses.network.NetworkData;
import fr.ynryo.ouestcefdpdetram.apiResponses.vehicle.VehicleData;
import fr.ynryo.ouestcefdpdetram.apiResponses.markers.MarkerDataResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface NaolibApiService {
    @GET("markers")
    Call<MarkerDataResponse> getVehicleMarkers(
            @Query("swLat") double swLat,
            @Query("swLon") double swLon,
            @Query("neLat") double neLat,
            @Query("neLon") double neLon
    );

    @GET("{vehicleId}")
    Call<VehicleData> getVehicleDetails(
            @Path(value = "vehicleId", encoded = true) String vehicleId
    );

    @GET("{networkId}")
    Call<NetworkData> getNetworkInformations(
            @Path(value = "networkId", encoded = true) int networkId
    );
}
