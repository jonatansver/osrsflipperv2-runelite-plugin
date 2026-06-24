package com.osrsflipperv2.runelite;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.inject.Inject;

public final class PluginAuthClient
{
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private final HttpClient httpClient;

    @Inject
    public PluginAuthClient()
    {
        this(HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build());
    }

    public PluginAuthClient(HttpClient httpClient)
    {
        this.httpClient = httpClient;
    }

    public PluginAuthExchangeResult exchangePairingToken(URI baseUri, String pairingToken, String deviceName) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(BackendEndpoints.resolveEndpoint(baseUri, "/api/plugin/auth/exchange"))
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(buildExchangeBody(pairingToken, deviceName), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 401)
        {
            throw new PluginAuthException("The pairing token was rejected by the backend.");
        }

        ensureSuccess(response, "pairing exchange");
        return PluginAuthExchangeResult.fromJson(response.body());
    }

    public void heartbeat(URI baseUri, String deviceToken) throws IOException, InterruptedException
    {
        HttpRequest request = HttpRequest.newBuilder(BackendEndpoints.resolveEndpoint(baseUri, "/api/plugin/heartbeat"))
            .timeout(REQUEST_TIMEOUT)
            .header("Authorization", "Bearer " + deviceToken)
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() == 401)
        {
            throw new PluginAuthException("The stored device token is no longer valid.");
        }

        ensureSuccess(response, "heartbeat");
    }

    private static String buildExchangeBody(String pairingToken, String deviceName)
    {
        return "{\"pairingToken\":\"" + JsonStrings.escape(pairingToken) + "\",\"deviceName\":\"" + JsonStrings.escape(deviceName) + "\"}";
    }

    private static void ensureSuccess(HttpResponse<?> response, String operation) throws PluginAuthException
    {
        int status = response.statusCode();
        if (status < 200 || status >= 300)
        {
            throw new PluginAuthException("Unexpected response during " + operation + ": " + status);
        }
    }
}
