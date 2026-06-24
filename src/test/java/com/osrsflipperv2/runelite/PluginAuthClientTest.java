package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginAuthClientTest
{
    private HttpServer server;
    private URI baseUri;

    @BeforeEach
    void setUp() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(0), 0);
    }

    @AfterEach
    void tearDown()
    {
        if (server != null)
        {
            server.stop(0);
        }
    }

    @Test
    void exchangesPairingTokenAndPostsHeartbeat() throws Exception
    {
        AtomicReference<String> exchangeBody = new AtomicReference<>();
        AtomicReference<String> exchangeContentType = new AtomicReference<>();
        AtomicReference<String> heartbeatAuthorization = new AtomicReference<>();

        server.createContext("/api/plugin/auth/exchange", exchange -> {
            exchangeBody.set(readBody(exchange));
            exchangeContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            respond(exchange, 200, "{\"deviceId\":\"11111111-2222-3333-4444-555555555555\",\"deviceToken\":\"device-token-abc\",\"createdAtUtc\":\"2026-06-24T07:29:00Z\"}");
        });

        server.createContext("/api/plugin/heartbeat", exchange -> {
            heartbeatAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 204, "");
        });

        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");

        PluginAuthClient client = new PluginAuthClient(HttpClient.newHttpClient());
        PluginAuthExchangeResult result = client.exchangePairingToken(baseUri, "pairing-token-123", "RuneLite");
        assertEquals(UUID.fromString("11111111-2222-3333-4444-555555555555"), result.deviceId());
        assertEquals("device-token-abc", result.deviceToken());
        assertEquals("2026-06-24T07:29:00Z", result.createdAtUtc().toString());

        client.heartbeat(baseUri, result.deviceToken());

        assertEquals("{\"pairingToken\":\"pairing-token-123\",\"deviceName\":\"RuneLite\"}", exchangeBody.get());
        assertEquals("application/json", exchangeContentType.get());
        assertEquals("Bearer device-token-abc", heartbeatAuthorization.get());
    }

    private static String readBody(HttpExchange exchange) throws IOException
    {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, statusCode == 204 ? -1 : bytes.length);
        try (OutputStream output = exchange.getResponseBody())
        {
            if (bytes.length > 0)
            {
                output.write(bytes);
            }
        }
    }
}
