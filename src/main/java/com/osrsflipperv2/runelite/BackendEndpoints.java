package com.osrsflipperv2.runelite;

import java.net.URI;

public final class BackendEndpoints
{
    public static final String DEFAULT_LOCAL_BASE_URL = "http://localhost:5000/";

    private BackendEndpoints()
    {
    }

    public static URI resolveBaseUri(String environmentValue, String configuredBaseUrl)
    {
        String explicitBaseUrl = normalizeBaseUrl(configuredBaseUrl);
        if (!explicitBaseUrl.isBlank())
        {
            return URI.create(explicitBaseUrl);
        }

        BackendEnvironment environment = BackendEnvironment.fromConfig(environmentValue);
        if (environment == BackendEnvironment.LOCAL)
        {
            return URI.create(DEFAULT_LOCAL_BASE_URL);
        }

        throw new IllegalStateException("A backend base URL must be configured when using " + environment.name().toLowerCase() + " mode.");
    }

    public static URI resolveEndpoint(URI baseUri, String path)
    {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        return baseUri.resolve(normalizedPath);
    }

    static String normalizeBaseUrl(String rawValue)
    {
        if (rawValue == null)
        {
            return "";
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty())
        {
            return "";
        }

        URI uri = URI.create(trimmed);
        String normalized = uri.toString();
        if (!normalized.endsWith("/"))
        {
            normalized += "/";
        }

        return normalized;
    }
}
