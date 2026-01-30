package fr.ynryo.ouestcefdpdetram.apiResponses.region;

import androidx.annotation.NonNull;

import java.net.URI;

public class RegionData {
    private int id;
    private String name;
    private String authority;
    private URI logoHref;
    private int regionId;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAuthority() {
        return authority;
    }

    public URI getLogoHref() {
        return logoHref;
    }

    public int getRegionId() {
        return regionId;
    }

    @NonNull
    @Override
    public String toString() {
        return "RegionData{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", authority='" + authority + '\'' +
                ", logoHref=" + logoHref +
                ", regionId=" + regionId +
                '}';
    }
}
