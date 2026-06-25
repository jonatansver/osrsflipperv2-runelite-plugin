package com.osrsflipperv2.runelite;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
    name = "Osrs Flipper V2",
    description = "Pairs RuneLite with the OsrsFlipperV2 backend and shows connection status.",
    tags = {"flipping", "pairing", "ge", "sidebar"}
)
public final class OsrsFlipperV2Plugin extends Plugin implements OsrsFlipperV2Panel.Controller
{
    private static final Logger LOGGER = Logger.getLogger(OsrsFlipperV2Plugin.class.getName());
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(5);

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OsrsFlipperV2Config config;

    @Inject
    private PluginAuthClient authClient;

    @Provides
    OsrsFlipperV2Config provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OsrsFlipperV2Config.class);
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable ->
    {
        Thread thread = new Thread(runnable, "osrsflipperv2-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    private OsrsFlipperV2Panel panel;
    private NavigationButton navigationButton;
    private ScheduledFuture<?> heartbeatFuture;

    @Override
    protected void startUp()
    {
        panel = new OsrsFlipperV2Panel();
        panel.setController(this);
        panel.setDeviceName(config.deviceName());
        panel.setPairingToken("");
        panel.appendLog("Plugin started at " + Instant.now());
        clientToolbar.addNavigation(buildNavigationButton());

        renderCurrentState("Ready to pair");
        refreshFromStoredState(true);
    }

    @Override
    protected void shutDown()
    {
        cancelHeartbeatLoop();
        executor.shutdownNow();
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }

        navigationButton = null;
        panel = null;
    }

    @Override
    public void onPairRequested(String deviceName, String pairingToken)
    {
        executor.submit(() ->
        {
            if (panel != null)
            {
                panel.setBusy(true);
            }

            try
            {
                String normalizedDeviceName = normalizeDeviceName(deviceName);
                String normalizedToken = pairingToken == null ? "" : pairingToken.trim();
                if (normalizedToken.isEmpty())
                {
                    postLog("Pairing token is empty.");
                    renderCurrentState("Waiting for a pairing token");
                    return;
                }

                URI baseUri = resolveBaseUri();
                PluginAuthExchangeResult result = authClient.exchangePairingToken(baseUri, normalizedToken, normalizedDeviceName);
                persistPairing(normalizedDeviceName, result);
                postLog("Paired device " + result.deviceId() + ". Starting heartbeat loop.");
                renderCurrentState("Paired and heartbeating");
                scheduleHeartbeatLoop();
                runHeartbeat(false);
            }
            catch (PluginAuthException ex)
            {
                LOGGER.log(Level.WARNING, "Pairing failed", ex);
                postLog(ex.getMessage());
                renderCurrentState("Pairing failed");
            }
            catch (IOException ex)
            {
                LOGGER.log(Level.SEVERE, "Pairing failed", ex);
                postLog("Pairing failed: " + ex.getMessage());
                renderCurrentState("Pairing failed");
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                postLog("Pairing was interrupted.");
                renderCurrentState("Pairing interrupted");
            }
            finally
            {
                if (panel != null)
                {
                    panel.setBusy(false);
                }
            }
        });
    }

    @Override
    public void onHeartbeatRequested()
    {
        executor.submit(() -> runHeartbeat(true));
    }

    @Override
    public void onForgetRequested()
    {
        executor.submit(() ->
        {
            clearStoredPairing();
            cancelHeartbeatLoop();
            renderCurrentState("Device forgotten");
            postLog("Cleared the stored device token.");
        });
    }

    private void refreshFromStoredState(boolean triggerHeartbeat)
    {
        executor.submit(() ->
        {
            if (panel == null)
            {
                return;
            }

            panel.setDeviceName(config.deviceName());
            panel.setPairingToken("");

            if (hasStoredPairing())
            {
                renderCurrentState("Paired device loaded from config");
                scheduleHeartbeatLoop();
                if (triggerHeartbeat)
                {
                    runHeartbeat(false);
                }
                return;
            }

            renderCurrentState("Not paired");
        });
    }

    private void runHeartbeat(boolean logBeforeRun)
    {
        if (logBeforeRun)
        {
            postLog("Sending heartbeat...");
        }

        try
        {
            if (!hasStoredPairing())
            {
                postLog("No stored device token is available.");
                renderCurrentState("Not paired");
                cancelHeartbeatLoop();
                return;
            }

            URI baseUri = resolveBaseUri();
            authClient.heartbeat(baseUri, config.deviceToken());
            renderCurrentState("Heartbeat OK");
            postLog("Heartbeat succeeded against " + baseUri);
        }
        catch (PluginAuthException ex)
        {
            LOGGER.log(Level.WARNING, "Heartbeat rejected", ex);
            postLog(ex.getMessage());
            clearStoredPairing();
            cancelHeartbeatLoop();
            renderCurrentState("Token rejected");
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.SEVERE, "Heartbeat failed", ex);
            postLog("Heartbeat failed: " + ex.getMessage());
            renderCurrentState("Heartbeat failed");
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            postLog("Heartbeat was interrupted.");
            renderCurrentState("Heartbeat interrupted");
        }
    }

    private void persistPairing(String deviceName, PluginAuthExchangeResult result)
    {
        configManager.setConfiguration(OsrsFlipperV2Config.GROUP, "deviceName", deviceName);
        configManager.setConfiguration(OsrsFlipperV2Config.GROUP, "deviceId", result.deviceId().toString());
        configManager.setConfiguration(OsrsFlipperV2Config.GROUP, "deviceToken", result.deviceToken());
    }

    private void clearStoredPairing()
    {
        configManager.unsetConfiguration(OsrsFlipperV2Config.GROUP, "deviceId");
        configManager.unsetConfiguration(OsrsFlipperV2Config.GROUP, "deviceToken");
    }

    private boolean hasStoredPairing()
    {
        return !config.deviceToken().isBlank();
    }

    private URI resolveBaseUri()
    {
        return BackendEndpoints.resolveBaseUri(config.backendEnvironment());
    }

    private String normalizeDeviceName(String deviceName)
    {
        String trimmed = deviceName == null ? "" : deviceName.trim();
        return trimmed.isEmpty() ? config.deviceName() : trimmed;
    }

    private NavigationButton buildNavigationButton()
    {
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("Osrs Flipper V2")
            .icon(icon)
            .priority(5)
            .panel(panel)
            .build();
        return navigationButton;
    }

    private void scheduleHeartbeatLoop()
    {
        cancelHeartbeatLoop();
        heartbeatFuture = executor.scheduleAtFixedRate(() -> runHeartbeat(false), HEARTBEAT_INTERVAL.toMinutes(), HEARTBEAT_INTERVAL.toMinutes(), TimeUnit.MINUTES);
    }

    private void cancelHeartbeatLoop()
    {
        if (heartbeatFuture != null)
        {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
    }

    private void renderCurrentState(String statusMessage)
    {
        if (panel == null)
        {
            return;
        }

        panel.renderSnapshot(new ConnectionSnapshot(
            config.backendEnvironment().toString(),
            safeBaseUrlLabel(),
            config.deviceName(),
            config.deviceId(),
            hasStoredPairing(),
            statusMessage));
    }

    private String safeBaseUrlLabel()
    {
        return resolveBaseUri().toString();
    }

    private void postLog(String message)
    {
        if (panel != null)
        {
            panel.appendLog(message);
        }
        LOGGER.info(message);
    }
}
