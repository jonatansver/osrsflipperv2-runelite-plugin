package com.osrsflipperv2.runelite;

import java.time.Instant;
import java.util.UUID;

public final class PluginAuthExchangeResult
{
    private final UUID deviceId;
    private final String deviceToken;
    private final Instant createdAtUtc;

    public PluginAuthExchangeResult(UUID deviceId, String deviceToken, Instant createdAtUtc)
    {
        this.deviceId = deviceId;
        this.deviceToken = deviceToken;
        this.createdAtUtc = createdAtUtc;
    }

    public UUID deviceId()
    {
        return deviceId;
    }

    public String deviceToken()
    {
        return deviceToken;
    }

    public Instant createdAtUtc()
    {
        return createdAtUtc;
    }

    public static PluginAuthExchangeResult fromJson(String json)
    {
        UUID deviceId = UUID.fromString(JsonStrings.extractString(json, "deviceId"));
        String deviceToken = JsonStrings.extractString(json, "deviceToken");
        Instant createdAtUtc = Instant.parse(JsonStrings.extractString(json, "createdAtUtc"));
        return new PluginAuthExchangeResult(deviceId, deviceToken, createdAtUtc);
    }
}
