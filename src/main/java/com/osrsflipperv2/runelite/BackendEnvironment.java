package com.osrsflipperv2.runelite;

import java.net.URI;

public enum BackendEnvironment
{
    STAGING("Staging", URI.create("https://osrsflipper-staging-web-03.proudgrass-84e31ac2.centralus.azurecontainerapps.io/")),
    PRODUCTION("Production", URI.create("https://osrsflipper-prod-web-04.azurecontainerapps.io/"));

    private final String displayName;
    private final URI baseUri;

    BackendEnvironment(String displayName, URI baseUri)
    {
        this.displayName = displayName;
        this.baseUri = baseUri;
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    public URI baseUri()
    {
        return baseUri;
    }
}
