package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

class BackendEndpointsTest
{
    @Test
    void resolvesStagingBaseUrl()
    {
        assertEquals(URI.create("https://osrsflipper-staging-web-03.proudgrass-84e31ac2.centralus.azurecontainerapps.io/"), BackendEndpoints.resolveBaseUri(BackendEnvironment.STAGING));
    }

    @Test
    void resolvesProductionBaseUrl()
    {
        assertEquals(URI.create("https://osrsflipper-prod-web-04.azurecontainerapps.io/"), BackendEndpoints.resolveBaseUri(BackendEnvironment.PRODUCTION));
    }
}
