package com.osrsflipperv2.runelite;

public final class ConnectionSnapshot
{
    private final String backendEnvironment;
    private final String backendBaseUrl;
    private final String deviceName;
    private final String deviceId;
    private final boolean paired;
    private final String statusMessage;

    public ConnectionSnapshot(
        String backendEnvironment,
        String backendBaseUrl,
        String deviceName,
        String deviceId,
        boolean paired,
        String statusMessage)
    {
        this.backendEnvironment = backendEnvironment;
        this.backendBaseUrl = backendBaseUrl;
        this.deviceName = deviceName;
        this.deviceId = deviceId;
        this.paired = paired;
        this.statusMessage = statusMessage;
    }

    public String backendEnvironment()
    {
        return backendEnvironment;
    }

    public String backendBaseUrl()
    {
        return backendBaseUrl;
    }

    public String deviceName()
    {
        return deviceName;
    }

    public String deviceId()
    {
        return deviceId;
    }

    public boolean paired()
    {
        return paired;
    }

    public String statusMessage()
    {
        return statusMessage;
    }
}
