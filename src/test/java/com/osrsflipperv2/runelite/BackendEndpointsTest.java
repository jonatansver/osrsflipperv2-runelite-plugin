package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import org.junit.jupiter.api.Test;

class BackendEndpointsTest
{
    @Test
    void resolvesLocalDefault()
    {
        assertEquals(URI.create("http://localhost:5000/"), BackendEndpoints.resolveBaseUri("local", ""));
    }

    @Test
    void normalizesExplicitBaseUrl()
    {
        assertEquals(URI.create("http://example.test/api/"), BackendEndpoints.resolveBaseUri("staging", "http://example.test/api"));
    }

    @Test
    void requiresExplicitBaseUrlOutsideLocal()
    {
        assertThrows(IllegalStateException.class, () -> BackendEndpoints.resolveBaseUri("production", ""));
    }
}
