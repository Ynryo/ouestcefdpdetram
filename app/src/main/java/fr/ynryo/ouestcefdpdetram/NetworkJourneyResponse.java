package fr.ynryo.ouestcefdpdetram;

import java.net.URI;

public class NetworkJourneyResponse {
    private int id;
    private String ref;
    private String name;
    private URI logoHref;

    public int getId() {
        return id;
    }

    public String getRef() {
        return ref;
    }

    public String getName() {
        return name;
    }

    public URI getLogoHref() {
        return logoHref;
    }
}
