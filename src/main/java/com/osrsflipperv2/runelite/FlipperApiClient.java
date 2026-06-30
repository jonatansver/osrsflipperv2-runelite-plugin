package com.osrsflipperv2.runelite;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

public final class FlipperApiClient
{
    private static final java.time.Duration REQUEST_TIMEOUT = java.time.Duration.ofSeconds(10);
    private final HttpClient httpClient;

    @Inject
    public FlipperApiClient()
    {
        this(HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build());
    }

    public FlipperApiClient(HttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public DashboardSnapshot getDashboard(URI baseUri, String deviceToken) throws IOException, InterruptedException
    {
        JsonObject json = sendJson("GET", BackendEndpoints.resolveEndpoint(baseUri, "/api/dashboard"), deviceToken, null);
        return parseDashboard(json);
    }

    public HistorySnapshot getHistory(URI baseUri, String deviceToken, int days, int limit) throws IOException, InterruptedException
    {
        JsonObject json = sendJson("GET", BackendEndpoints.resolveEndpoint(baseUri, "/api/history?days=" + days + "&limit=" + limit), deviceToken, null);
        return parseHistory(json);
    }

    public List<ActiveFlipSnapshot> reorderActiveFlips(URI baseUri, String deviceToken, List<ReorderItem> items, String source, String clientEventId, String lastKnownUpdatedAtUtc) throws IOException, InterruptedException
    {
        return parseActiveFlipList(sendJson("PATCH", BackendEndpoints.resolveEndpoint(baseUri, "/api/active-flips/reorder"), deviceToken, new ReorderRequest(items, source, clientEventId, lastKnownUpdatedAtUtc)));
    }

    public List<ActiveFlipSnapshot> updateActiveFlipBuy(URI baseUri, String deviceToken, UUID activeFlipId, UpdateBuyRequest request) throws IOException, InterruptedException
    {
        return parseActiveFlipList(sendJson("PATCH", BackendEndpoints.resolveEndpoint(baseUri, "/api/active-flips/" + activeFlipId + "/buy"), deviceToken, request));
    }

    public List<ActiveFlipSnapshot> cancelActiveFlipBuy(URI baseUri, String deviceToken, UUID activeFlipId, CancelBuyRequest request) throws IOException, InterruptedException
    {
        return parseActiveFlipList(sendJson("PATCH", BackendEndpoints.resolveEndpoint(baseUri, "/api/active-flips/" + activeFlipId + "/cancel-buy"), deviceToken, request));
    }

    public List<ActiveFlipSnapshot> refreshActiveFlipSellPrice(URI baseUri, String deviceToken, UUID activeFlipId, RefreshSellPriceRequest request) throws IOException, InterruptedException
    {
        return parseActiveFlipList(sendJson("PATCH", BackendEndpoints.resolveEndpoint(baseUri, "/api/active-flips/" + activeFlipId + "/sell-refresh"), deviceToken, request));
    }

    public List<ActiveFlipSnapshot> updateActiveFlipSell(URI baseUri, String deviceToken, UUID activeFlipId, UpdateSellRequest request) throws IOException, InterruptedException
    {
        return parseActiveFlipList(sendJson("PATCH", BackendEndpoints.resolveEndpoint(baseUri, "/api/active-flips/" + activeFlipId + "/sell"), deviceToken, request));
    }

    private JsonObject sendJson(String method, URI uri, String deviceToken, Object body) throws IOException, InterruptedException
    {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer " + deviceToken)
            .header("Accept", "application/json");

        if (body != null)
        {
            builder.header("Content-Type", "application/json");
        }

        switch (method)
        {
            case "GET":
                builder.GET();
                break;
            case "PATCH":
                builder.method("PATCH", HttpRequest.BodyPublishers.ofString(JsonBody.toJson(body), StandardCharsets.UTF_8));
                break;
            case "POST":
                builder.POST(body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JsonBody.toJson(body), StandardCharsets.UTF_8));
                break;
            case "PUT":
                builder.PUT(HttpRequest.BodyPublishers.ofString(JsonBody.toJson(body), StandardCharsets.UTF_8));
                break;
            case "DELETE":
                builder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 401)
        {
            throw new PluginAuthException("The plugin token is no longer authorized for the backend.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300)
        {
            throw new IOException("Unexpected response during " + method + " " + uri + ": " + response.statusCode() + " " + response.body());
        }

        String bodyText = response.body();
        if (bodyText == null || bodyText.isBlank())
        {
            return new JsonObject();
        }

        JsonElement element = new JsonParser().parse(bodyText);
        return element.isJsonObject() ? element.getAsJsonObject() : wrapArrayResponse(element.getAsJsonArray());
    }

    private static JsonObject wrapArrayResponse(JsonArray array)
    {
        JsonObject wrapper = new JsonObject();
        wrapper.add("items", array);
        return wrapper;
    }

    private static DashboardSnapshot parseDashboard(JsonObject json)
    {
        List<ActiveFlipSnapshot> activeFlips = new ArrayList<>();
        JsonArray activeFlipArray = json.getAsJsonArray("activeFlips");
        if (activeFlipArray != null)
        {
            for (JsonElement element : activeFlipArray)
            {
                activeFlips.add(parseActiveFlip(element.getAsJsonObject()));
            }
        }

        List<DashboardSlotSnapshot> availableSlots = new ArrayList<>();
        JsonArray slotArray = json.getAsJsonArray("availableSlots");
        if (slotArray != null)
        {
            for (JsonElement element : slotArray)
            {
                JsonObject slot = element.getAsJsonObject();
                availableSlots.add(new DashboardSlotSnapshot(
                    getString(slot, "id", ""),
                    getString(slot, "label", ""),
                    getBoolean(slot, "occupied", false),
                    getBoolean(slot, "locked", false),
                    getNullableString(slot, "itemName")));
            }
        }

        JsonObject defaults = json.getAsJsonObject("requestDefaults");
        long budget = defaults == null ? 0L : getLong(defaults, "totalBudgetGp", 0L);
        short buyLimitWindows = defaults == null ? 1 : (short) getLong(defaults, "buyLimitWindows", 1L);

        return new DashboardSnapshot(activeFlips, availableSlots, budget, buyLimitWindows, Instant.now().toEpochMilli());
    }

    private static HistorySnapshot parseHistory(JsonObject json)
    {
        List<HistoryFlipSnapshot> flips = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("Flips");
        if (array != null)
        {
            for (JsonElement element : array)
            {
                flips.add(parseHistoryFlip(element.getAsJsonObject()));
            }
        }

        JsonObject summary = json.getAsJsonObject("Summary");
        long totalProfit = summary == null ? 0L : getLong(summary, "totalProfitGp", 0L);
        int totalFlips = summary == null ? 0 : (int) getLong(summary, "totalFlips", 0L);
        int cancelledFlips = summary == null ? 0 : (int) getLong(summary, "cancelledFlips", 0L);
        return new HistorySnapshot(flips, totalProfit, totalFlips, cancelledFlips);
    }

    private static List<ActiveFlipSnapshot> parseActiveFlipList(JsonObject json)
    {
        List<ActiveFlipSnapshot> activeFlips = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("items");
        if (array != null)
        {
            for (JsonElement element : array)
            {
                activeFlips.add(parseActiveFlip(element.getAsJsonObject()));
            }
        }
        return activeFlips;
    }

    private static ActiveFlipSnapshot parseActiveFlip(JsonObject json)
    {
        return new ActiveFlipSnapshot(
            UUID.fromString(getString(json, "id", "00000000-0000-0000-0000-000000000000")),
            (short) getLong(json, "slotIndex", 1L),
            getString(json, "slotLabel", ""),
            getString(json, "itemName", ""),
            getNullableString(json, "imageUrl"),
            getString(json, "status", ""),
            (int) getLong(json, "plannedQuantity", 0L),
            (int) getLong(json, "actualBoughtQuantity", 0L),
            (int) getLong(json, "actualSoldQuantity", 0L),
            getLong(json, "plannedBuyPriceGp", 0L),
            getLong(json, "plannedSellPriceGp", 0L),
            getNullableLong(json, "reevaluatedSellPriceGp"),
            getNullableLong(json, "actualBuySpendGp"),
            getNullableLong(json, "actualSellReceiveGp"),
            (short) getLong(json, "buyLimitWindows", 1L),
            getNullableLong(json, "originalThresholdMaxMarketBuyGp"),
            getNullableLong(json, "originalThresholdMinMarketSellGp"),
            getNullableInstant(json, "boughtAtUtc"),
            getNullableInstant(json, "soldAtUtc"),
            getNullableInstant(json, "cancelledAtUtc"),
            getInstant(json, "updatedAtUtc"),
            getLong(json, "projectedProfitGp", 0L));
    }

    private static HistoryFlipSnapshot parseHistoryFlip(JsonObject json)
    {
        return new HistoryFlipSnapshot(
            UUID.fromString(getString(json, "id", "00000000-0000-0000-0000-000000000000")),
            (int) getLong(json, "itemId", 0L),
            getString(json, "itemName", ""),
            getLong(json, "plannedBuyPriceGp", 0L),
            getNullableLong(json, "actualGpSpent"),
            getNullableLong(json, "actualGpReceived"),
            getLong(json, "profitGp", 0L),
            (int) getLong(json, "plannedQuantity", 0L),
            (int) getLong(json, "actualBoughtQuantity", 0L),
            (int) getLong(json, "actualSoldQuantity", 0L),
            getNullableInstant(json, "boughtAtUtc"),
            getNullableInstant(json, "soldAtUtc"),
            getNullableInstant(json, "completedAtUtc"),
            getInstant(json, "createdAtUtc"),
            getString(json, "status", ""));
    }

    private static String getString(JsonObject json, String name, String fallback)
    {
        JsonElement element = json.get(name);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static String getNullableString(JsonObject json, String name)
    {
        JsonElement element = json.get(name);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static long getLong(JsonObject json, String name, long fallback)
    {
        JsonElement element = json.get(name);
        return element == null || element.isJsonNull() ? fallback : element.getAsLong();
    }

    private static Long getNullableLong(JsonObject json, String name)
    {
        JsonElement element = json.get(name);
        return element == null || element.isJsonNull() ? null : element.getAsLong();
    }

    private static boolean getBoolean(JsonObject json, String name, boolean fallback)
    {
        JsonElement element = json.get(name);
        return element == null || element.isJsonNull() ? fallback : element.getAsBoolean();
    }

    private static Instant getInstant(JsonObject json, String name)
    {
        String value = getString(json, name, Instant.EPOCH.toString());
        return Instant.parse(value);
    }

    private static Instant getNullableInstant(JsonObject json, String name)
    {
        String value = getNullableString(json, name);
        return value == null ? null : Instant.parse(value);
    }

    public static final class ReorderItem
    {
        private final String activeFlipId;
        private final short slotIndex;

        public ReorderItem(String activeFlipId, short slotIndex)
        {
            this.activeFlipId = activeFlipId;
            this.slotIndex = slotIndex;
        }
    }

    public static final class ReorderRequest
    {
        private final List<ReorderItem> items;
        private final String source;
        private final String clientEventId;
        private final String lastKnownUpdatedAtUtc;

        public ReorderRequest(List<ReorderItem> items, String source, String clientEventId, String lastKnownUpdatedAtUtc)
        {
            this.items = items;
            this.source = source;
            this.clientEventId = clientEventId;
            this.lastKnownUpdatedAtUtc = lastKnownUpdatedAtUtc;
        }
    }

    public static final class UpdateBuyRequest
    {
        private final int actualBoughtQuantity;
        private final long actualBuySpendGp;
        private final String boughtAtUtc;
        private final Long reevaluatedSellPriceGp;
        private final boolean concludeBuy;
        private final String source;
        private final String clientEventId;
        private final String lastKnownUpdatedAtUtc;

        public UpdateBuyRequest(int actualBoughtQuantity, long actualBuySpendGp, String boughtAtUtc, Long reevaluatedSellPriceGp, boolean concludeBuy, String source, String clientEventId, String lastKnownUpdatedAtUtc)
        {
            this.actualBoughtQuantity = actualBoughtQuantity;
            this.actualBuySpendGp = actualBuySpendGp;
            this.boughtAtUtc = boughtAtUtc;
            this.reevaluatedSellPriceGp = reevaluatedSellPriceGp;
            this.concludeBuy = concludeBuy;
            this.source = source;
            this.clientEventId = clientEventId;
            this.lastKnownUpdatedAtUtc = lastKnownUpdatedAtUtc;
        }
    }

    public static final class CancelBuyRequest
    {
        private final String source;
        private final String clientEventId;
        private final String lastKnownUpdatedAtUtc;

        public CancelBuyRequest(String source, String clientEventId, String lastKnownUpdatedAtUtc)
        {
            this.source = source;
            this.clientEventId = clientEventId;
            this.lastKnownUpdatedAtUtc = lastKnownUpdatedAtUtc;
        }
    }

    public static final class RefreshSellPriceRequest
    {
        private final String source;
        private final String clientEventId;
        private final String lastKnownUpdatedAtUtc;

        public RefreshSellPriceRequest(String source, String clientEventId, String lastKnownUpdatedAtUtc)
        {
            this.source = source;
            this.clientEventId = clientEventId;
            this.lastKnownUpdatedAtUtc = lastKnownUpdatedAtUtc;
        }
    }

    public static final class UpdateSellRequest
    {
        private final int actualSoldQuantity;
        private final long actualSellReceiveGp;
        private final String soldAtUtc;
        private final boolean concludeSell;
        private final String source;
        private final String clientEventId;
        private final String lastKnownUpdatedAtUtc;

        public UpdateSellRequest(int actualSoldQuantity, long actualSellReceiveGp, String soldAtUtc, boolean concludeSell, String source, String clientEventId, String lastKnownUpdatedAtUtc)
        {
            this.actualSoldQuantity = actualSoldQuantity;
            this.actualSellReceiveGp = actualSellReceiveGp;
            this.soldAtUtc = soldAtUtc;
            this.concludeSell = concludeSell;
            this.source = source;
            this.clientEventId = clientEventId;
            this.lastKnownUpdatedAtUtc = lastKnownUpdatedAtUtc;
        }
    }
}

final class JsonBody
{
    private static final com.google.gson.Gson GSON = new com.google.gson.GsonBuilder().serializeNulls().create();

    private JsonBody()
    {
    }

    static String toJson(Object body)
    {
        return GSON.toJson(body);
    }
}
