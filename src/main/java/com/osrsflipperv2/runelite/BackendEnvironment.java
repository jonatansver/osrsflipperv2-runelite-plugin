package com.osrsflipperv2.runelite;

import java.net.URI;

public enum BackendEnvironment
{
    STAGING("Staging", URI.create("https://api-staging.osrsflipperv2.com/")),
    PRODUCTION("Production", URI.create("https://api.osrsflipperv2.com/"));

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
