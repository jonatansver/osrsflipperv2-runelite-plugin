package com.osrsflipperv2.runelite;

import java.net.URI;

public final class BackendEndpoints
{
    private BackendEndpoints()
    {
    }

    public static URI resolveBaseUri(BackendEnvironment environment)
    {
        return environment.baseUri();
    }

    public static URI resolveEndpoint(URI baseUri, String path)
    {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUri.resolve(normalizedPath);
    }
}
