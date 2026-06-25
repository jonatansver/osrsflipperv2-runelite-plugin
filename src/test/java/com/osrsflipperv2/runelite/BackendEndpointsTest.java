package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;

class BackendEndpointsTest
{
    @Test
    void resolvesStagingBaseUrl()
    {
        assertEquals(URI.create("https://api-staging.osrsflipperv2.com/"), BackendEndpoints.resolveBaseUri(BackendEnvironment.STAGING));
    }

    @Test
    void resolvesProductionBaseUrl()
    {
        assertEquals(URI.create("https://api.osrsflipperv2.com/"), BackendEndpoints.resolveBaseUri(BackendEnvironment.PRODUCTION));
    }
}
