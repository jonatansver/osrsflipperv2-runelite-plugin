package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FlipperApiClientTest
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
    void loadsDashboardHistoryAndPostsBuyMutation() throws Exception
    {
        AtomicReference<String> dashboardAuth = new AtomicReference<>();
        AtomicReference<String> historyAuth = new AtomicReference<>();
        AtomicReference<String> buyAuth = new AtomicReference<>();
        AtomicReference<String> buyBody = new AtomicReference<>();

        server.createContext("/api/dashboard", exchange -> {
            dashboardAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200,
                "{\n" +
                "  \"activeFlips\": [\n" +
                "    {\n" +
                "      \"id\": \"11111111-2222-3333-4444-555555555555\",\n" +
                "      \"slotIndex\": 1,\n" +
                "      \"slotLabel\": \"GE Slot 1\",\n" +
                "      \"itemName\": \"Abyssal whip\",\n" +
                "      \"imageUrl\": null,\n" +
                "      \"status\": \"planned_buy\",\n" +
                "      \"plannedQuantity\": 2,\n" +
                "      \"actualBoughtQuantity\": 0,\n" +
                "      \"actualSoldQuantity\": 0,\n" +
                "      \"plannedBuyPriceGp\": 1200000,\n" +
                "      \"plannedSellPriceGp\": 1250000,\n" +
                "      \"reevaluatedSellPriceGp\": null,\n" +
                "      \"actualBuySpendGp\": null,\n" +
                "      \"actualSellReceiveGp\": null,\n" +
                "      \"buyLimitWindows\": 1,\n" +
                "      \"originalThresholdMaxMarketBuyGp\": null,\n" +
                "      \"originalThresholdMinMarketSellGp\": null,\n" +
                "      \"boughtAtUtc\": null,\n" +
                "      \"soldAtUtc\": null,\n" +
                "      \"cancelledAtUtc\": null,\n" +
                "      \"updatedAtUtc\": \"2026-06-28T06:00:00Z\",\n" +
                "      \"projectedProfitGp\": 100000\n" +
                "    }\n" +
                "  ],\n" +
                "  \"availableSlots\": [\n" +
                "    { \"id\": \"slot-1\", \"label\": \"GE Slot 1\", \"occupied\": true, \"locked\": false, \"itemName\": \"Abyssal whip\" }\n" +
                "  ],\n" +
                "  \"requestDefaults\": { \"totalBudgetGp\": 2500000, \"slotIds\": [\"slot-2\"], \"buyLimitWindows\": 1 }\n" +
                "}\n");
        });

        server.createContext("/api/history", exchange -> {
            historyAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200,
                "{\n" +
                "  \"Flips\": [\n" +
                "    {\n" +
                "      \"id\": \"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee\",\n" +
                "      \"itemId\": 4151,\n" +
                "      \"itemName\": \"Abyssal whip\",\n" +
                "      \"plannedBuyPriceGp\": 1200000,\n" +
                "      \"actualGpSpent\": 1200000,\n" +
                "      \"actualGpReceived\": 1250000,\n" +
                "      \"profitGp\": 50000,\n" +
                "      \"plannedQuantity\": 2,\n" +
                "      \"actualBoughtQuantity\": 2,\n" +
                "      \"actualSoldQuantity\": 2,\n" +
                "      \"boughtAtUtc\": \"2026-06-28T05:00:00Z\",\n" +
                "      \"soldAtUtc\": \"2026-06-28T06:00:00Z\",\n" +
                "      \"completedAtUtc\": \"2026-06-28T06:00:00Z\",\n" +
                "      \"createdAtUtc\": \"2026-06-28T04:30:00Z\",\n" +
                "      \"status\": \"completed\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"Summary\": { \"totalProfitGp\": 50000, \"totalFlips\": 1, \"cancelledFlips\": 0, \"byItem\": [] }\n" +
                "}\n");
        });

        server.createContext("/api/active-flips/11111111-2222-3333-4444-555555555555/buy", exchange -> {
            buyAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            buyBody.set(readBody(exchange));
            respond(exchange, 200,
                "[\n" +
                "  {\n" +
                "    \"id\": \"11111111-2222-3333-4444-555555555555\",\n" +
                "    \"slotIndex\": 1,\n" +
                "    \"slotLabel\": \"GE Slot 1\",\n" +
                "    \"itemName\": \"Abyssal whip\",\n" +
                "    \"imageUrl\": null,\n" +
                "    \"status\": \"bought_waiting_sell\",\n" +
                "    \"plannedQuantity\": 2,\n" +
                "    \"actualBoughtQuantity\": 2,\n" +
                "    \"actualSoldQuantity\": 0,\n" +
                "    \"plannedBuyPriceGp\": 1200000,\n" +
                "    \"plannedSellPriceGp\": 1250000,\n" +
                "    \"reevaluatedSellPriceGp\": null,\n" +
                "    \"actualBuySpendGp\": 2400000,\n" +
                "    \"actualSellReceiveGp\": null,\n" +
                "    \"buyLimitWindows\": 1,\n" +
                "    \"originalThresholdMaxMarketBuyGp\": null,\n" +
                "    \"originalThresholdMinMarketSellGp\": null,\n" +
                "    \"boughtAtUtc\": \"2026-06-28T06:10:00Z\",\n" +
                "    \"soldAtUtc\": null,\n" +
                "    \"cancelledAtUtc\": null,\n" +
                "    \"updatedAtUtc\": \"2026-06-28T06:10:00Z\",\n" +
                "    \"projectedProfitGp\": 100000\n" +
                "  }\n" +
                "]\n");
        });

        server.start();
        baseUri = URI.create("http://localhost:" + server.getAddress().getPort() + "/");

        FlipperApiClient client = new FlipperApiClient(HttpClient.newHttpClient());
        DashboardSnapshot dashboard = client.getDashboard(baseUri, "device-token-123");
        HistorySnapshot history = client.getHistory(baseUri, "device-token-123", 30, 50);
        List<ActiveFlipSnapshot> updated = client.updateActiveFlipBuy(
            baseUri,
            "device-token-123",
            UUID.fromString("11111111-2222-3333-4444-555555555555"),
            new FlipperApiClient.UpdateBuyRequest(
                2,
                2400000L,
                "2026-06-28T06:10:00Z",
                null,
                true,
                "runelite",
                "event-1",
                "2026-06-28T06:00:00Z"));

        assertEquals("Bearer device-token-123", dashboardAuth.get());
        assertEquals("Bearer device-token-123", historyAuth.get());
        assertEquals("Bearer device-token-123", buyAuth.get());
        assertTrue(buyBody.get().contains("\"concludeBuy\":true"));
        assertTrue(buyBody.get().contains("\"clientEventId\":\"event-1\""));

        assertEquals(1, dashboard.activeFlips().size());
        assertEquals("Abyssal whip", dashboard.activeFlips().get(0).itemName());
        assertEquals(1, dashboard.availableSlots().size());
        assertEquals(1, history.flips().size());
        assertFalse(updated.isEmpty());
        assertNotNull(updated.get(0).boughtAtUtc());
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
