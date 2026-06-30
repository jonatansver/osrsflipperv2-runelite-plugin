package com.osrsflipperv2.runelite;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.MenuAction;
import net.runelite.api.ScriptID;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
    name = "Osrs Flipper V2",
    description = "Pairs RuneLite with the OsrsFlipperV2 backend and shows connection status.",
    tags = {"flipping", "pairing", "ge", "sidebar"}
)
public class OsrsFlipperV2Plugin extends Plugin implements OsrsFlipperV2Panel.Controller
{
    private static final Logger LOGGER = LoggerFactory.getLogger(OsrsFlipperV2Plugin.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofMinutes(5);
    private static final Duration POLL_INTERVAL = Duration.ofSeconds(30);
    private static final Duration LOGIN_RECONCILE_DELAY = Duration.ofSeconds(3);

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private ConfigManager configManager;

    @Inject
    private OsrsFlipperV2Config config;

    @Inject
    private PluginAuthClient authClient;

    @Inject
    private FlipperApiClient flipperApiClient;

    @Inject
    private GeInputTyper geInputTyper;

    @Inject
    private ItemManager itemManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private EventBus eventBus;

    private GeChatboxAssistant chatboxAssistant;

    @Provides
    OsrsFlipperV2Config provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(OsrsFlipperV2Config.class);
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable, "osrsflipperv2-sync");
            thread.setDaemon(true);
            return thread;
        }
    });
    private final AtomicReference<DashboardSnapshot> dashboardRef = new AtomicReference<>();
    private final AtomicReference<HistorySnapshot> historyRef = new AtomicReference<>();
    private final AtomicReference<UUID> focusedFlipId = new AtomicReference<>();
    private final AtomicReference<Integer> focusedSlotIndex = new AtomicReference<>();
    private final AtomicReference<Integer> currentSetupItemId = new AtomicReference<>(-1);
    private final AtomicReference<String> syncStatus = new AtomicReference<>("Not paired");
    private final AtomicReference<Boolean> loginReconcileScheduled = new AtomicReference<>(false);

    private OsrsFlipperV2Panel panel;
    private NavigationButton navigationButton;
    private GeAssistOverlay geAssistOverlay;
    private MouseListener geMouseListener;
    private final List<EventBus.Subscriber> eventSubscribers = new ArrayList<>();
    private ScheduledFuture<?> helperRefreshFuture;
    private ScheduledFuture<?> heartbeatFuture;
    private ScheduledFuture<?> pollFuture;

    @Override
    protected void startUp()
    {
        LOGGER.info("OsrsFlipperV2 startUp: initializing panel, overlay, and event handlers");
        panel = new OsrsFlipperV2Panel();
        panel.setController(this);
        panel.setDeviceName(config.deviceName());
        panel.setPairingToken("");
        panel.appendLog("Plugin started at " + Instant.now());
        clientToolbar.addNavigation(buildNavigationButton());
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                clientToolbar.openPanel(navigationButton);
            }
        });

        geAssistOverlay = new GeAssistOverlay(
            client,
            itemManager,
            new Supplier<DashboardSnapshot>()
            {
                @Override
                public DashboardSnapshot get()
                {
                    return dashboardRef.get();
                }
            },
            new Supplier<ActiveFlipSnapshot>()
            {
                @Override
                public ActiveFlipSnapshot get()
                {
                    return getFocusedFlip();
                }
            },
            new Supplier<List<GeSlotTarget>>()
            {
                @Override
                public List<GeSlotTarget> get()
                {
                    return getGeSlotTargets();
                }
            },
            new Supplier<Integer>()
            {
                @Override
                public Integer get()
                {
                    return getCurrentSetupItemId();
                }
            },
            new Supplier<String>()
            {
                @Override
                public String get()
                {
                    return syncStatus.get();
                }
            },
            new Consumer<GeAssistAction>()
            {
                @Override
                public void accept(GeAssistAction action)
                {
                    onHelperActionRequested(action);
                }
            });
        overlayManager.add(geAssistOverlay);

        registerEventHandlers();

        chatboxAssistant = new GeChatboxAssistant(
            client,
            clientThread,
            itemManager,
            geInputTyper,
            new Supplier<DashboardSnapshot>()
            {
                @Override
                public DashboardSnapshot get()
                {
                    return dashboardRef.get();
                }
            },
            new Supplier<ActiveFlipSnapshot>()
            {
                @Override
                public ActiveFlipSnapshot get()
                {
                    return getFocusedFlip();
                }
            });
        geMouseListener = new MouseListener()
        {
            @Override
            public java.awt.event.MouseEvent mouseClicked(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }

            @Override
            public java.awt.event.MouseEvent mousePressed(java.awt.event.MouseEvent mouseEvent)
            {
                if (mouseEvent.getButton() == java.awt.event.MouseEvent.BUTTON1)
                {
                    clientThread.invokeLater(() -> handleGeCanvasClick(mouseEvent.getPoint()));
                }
                return mouseEvent;
            }

            @Override
            public java.awt.event.MouseEvent mouseReleased(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }

            @Override
            public java.awt.event.MouseEvent mouseEntered(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }

            @Override
            public java.awt.event.MouseEvent mouseExited(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }

            @Override
            public java.awt.event.MouseEvent mouseDragged(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }

            @Override
            public java.awt.event.MouseEvent mouseMoved(java.awt.event.MouseEvent mouseEvent) { return mouseEvent; }
        };
        mouseManager.registerMouseListener(geMouseListener);
        scheduleHelperRefreshLoop();
        renderCurrentState("Ready to pair");
        schedulePollingLoop();
        refreshFromStoredState(false);
        LOGGER.info("OsrsFlipperV2 startUp: initialization complete");
    }

    @Override
    protected void shutDown()
    {
        cancelHeartbeatLoop();
        cancelPollingLoop();
        cancelHelperRefreshLoop();
        executor.shutdownNow();
        if (geAssistOverlay != null)
        {
            overlayManager.remove(geAssistOverlay);
        }
        for (EventBus.Subscriber subscriber : eventSubscribers)
        {
            eventBus.unregister(subscriber);
        }
        eventSubscribers.clear();
        if (geMouseListener != null)
        {
            mouseManager.unregisterMouseListener(geMouseListener);
        }
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }

        navigationButton = null;
        geAssistOverlay = null;
        geMouseListener = null;
        chatboxAssistant = null;
        panel = null;
        dashboardRef.set(null);
        historyRef.set(null);
        focusedFlipId.set(null);
        focusedSlotIndex.set(null);
        syncStatus.set("Not paired");
    }

    @Override
    public void onPairRequested(String deviceName, String pairingToken)
    {
        executor.submit(() ->
        {
            setBusy(true);
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
                postLog("Paired device " + result.deviceId() + ". Starting sync.");
                renderCurrentState("Paired");
                scheduleHeartbeatLoop();
                runHeartbeat(false);
                refreshFromServer(true);
            }
            catch (PluginAuthException ex)
            {
                LOGGER.warn("Pairing failed", ex);
                postLog(ex.getMessage());
                renderCurrentState("Pairing failed");
            }
            catch (IOException ex)
            {
                LOGGER.error("Pairing failed", ex);
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
                setBusy(false);
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
            dashboardRef.set(null);
            historyRef.set(null);
            focusedFlipId.set(null);
            focusedSlotIndex.set(null);
            requestChatboxRefresh();
            renderCurrentState("Device forgotten");
            postLog("Cleared the stored device token.");
        });
    }

    @Override
    public void onRefreshRequested()
    {
        executor.submit(() -> refreshFromServer(true));
    }

    @Override
    public void onReconcileRequested()
    {
        executor.submit(() -> refreshFromServer(true));
    }

    private void registerEventHandlers()
    {
        eventSubscribers.add(eventBus.register(GameStateChanged.class, new Consumer<GameStateChanged>()
        {
            @Override
            public void accept(GameStateChanged event)
            {
                onGameStateChanged(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(GrandExchangeOfferChanged.class, new Consumer<GrandExchangeOfferChanged>()
        {
            @Override
            public void accept(GrandExchangeOfferChanged event)
            {
                onGrandExchangeOfferChanged(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(VarClientIntChanged.class, new Consumer<VarClientIntChanged>()
        {
            @Override
            public void accept(VarClientIntChanged event)
            {
                onVarClientIntChanged(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(VarbitChanged.class, new Consumer<VarbitChanged>()
        {
            @Override
            public void accept(VarbitChanged event)
            {
                onVarbitChanged(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(MenuOptionClicked.class, new Consumer<MenuOptionClicked>()
        {
            @Override
            public void accept(MenuOptionClicked event)
            {
                onMenuOptionClicked(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(WidgetLoaded.class, new Consumer<WidgetLoaded>()
        {
            @Override
            public void accept(WidgetLoaded event)
            {
                onWidgetLoaded(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(WidgetClosed.class, new Consumer<WidgetClosed>()
        {
            @Override
            public void accept(WidgetClosed event)
            {
                onWidgetClosed(event);
            }
        }, 0f));
        eventSubscribers.add(eventBus.register(ScriptPostFired.class, new Consumer<ScriptPostFired>()
        {
            @Override
            public void accept(ScriptPostFired event)
            {
                onScriptPostFired(event);
            }
        }, 0f));
        LOGGER.info("Registered OsrsFlipperV2 event handlers: " + eventSubscribers.size());
    }

    @Override
    public void onHelperActionRequested(GeAssistAction action)
    {
        executor.submit(() -> performHelperAction(action));
    }

    public void onGameStateChanged(GameStateChanged event)
    {
        LOGGER.info("GE event: game state changed to " + event.getGameState());
        if (event.getGameState() == GameState.LOGGED_IN)
        {
            scheduleLoginReconcile();
        }
    }

    public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
    {
        LOGGER.info("GE event: offer changed slot=" + event.getSlot() + ", state=" + event.getOffer().getState()
            + ", itemId=" + event.getOffer().getItemId());
        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard != null)
        {
            dashboard.activeFlips().stream()
                .filter(flip -> flip.slotIndex() == event.getSlot() + 1)
                .findFirst()
                .ifPresent(flip -> setFocusedFlip(flip.id()));
        }

        executor.submit(() -> syncGrandExchangeOffer(event.getSlot(), event.getOffer()));
        requestChatboxRefresh();
    }

    public void onVarClientIntChanged(VarClientIntChanged event)
    {
        if (event.getIndex() == net.runelite.api.VarClientInt.INPUT_TYPE)
        {
            LOGGER.info("GE event: client input type changed");
            requestChatboxRefresh();
        }
    }

    public void onVarbitChanged(VarbitChanged event)
    {
        if (event.getVarbitId() == net.runelite.api.Varbits.GE_OFFER_CREATION_TYPE)
        {
            LOGGER.info("GE event: offer creation type changed");
            requestChatboxRefresh();
        }
    }

    public void onMenuOptionClicked(MenuOptionClicked event)
    {
        LOGGER.info(String.format(
            "GE click observed option=%s target=%s action=%s widgetId=%d param0=%d param1=%d actionParam=%d itemId=%d",
            Text.removeTags(event.getMenuOption()),
            event.getMenuTarget(),
            event.getMenuAction(),
            event.getWidgetId(),
            event.getParam0(),
            event.getParam1(),
            event.getActionParam(),
            event.getItemId()));
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        String option = Text.removeTags(event.getMenuOption());
        if (!"Buy".equalsIgnoreCase(option) && !"Sell".equalsIgnoreCase(option))
        {
            return;
        }

        LOGGER.info(String.format(
            "GE click option=%s target=%s action=%s widgetId=%d param0=%d param1=%d actionParam=%d itemId=%d widgetIndex=%s",
            option,
            event.getMenuTarget(),
            event.getMenuAction(),
            event.getWidgetId(),
            event.getParam0(),
            event.getParam1(),
            event.getActionParam(),
            event.getItemId(),
            event.getWidget() == null ? "null" : Integer.toString(event.getWidget().getIndex())));

        if (event.getMenuAction() != MenuAction.CC_OP && event.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
        {
            return;
        }

        int slotIndex = resolveGeSlotIndex(event);
        if (slotIndex <= 0)
        {
            return;
        }

        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard == null)
        {
            return;
        }

        dashboard.activeFlips().stream()
            .filter(flip -> flip.slotIndex() == slotIndex)
            .findFirst()
            .ifPresent(flip ->
            {
                setFocusedFlip(flip.id());
                focusedSlotIndex.set(slotIndex);
                syncStatus.set("GE slot " + slotIndex + " selected");
                requestChatboxRefresh();
                postLog("Selected GE slot " + slotIndex + " -> " + flip.itemName());
            });
    }

    public void onWidgetLoaded(WidgetLoaded event)
    {
        LOGGER.info("GE event: widget loaded " + event.getGroupId());
            requestChatboxRefresh();
    }

    public void onWidgetClosed(WidgetClosed event)
    {
            LOGGER.info("GE event: widget closed " + event.getGroupId());
            requestChatboxRefresh();
    }

    public void onScriptPostFired(ScriptPostFired event)
    {
        if (event.getScriptId() != ScriptID.GE_OFFERS_SETUP_BUILD)
        {
            return;
        }

        int itemId = client.getVarpValue(net.runelite.api.gameval.VarPlayerID.TRADINGPOST_SEARCH);
        currentSetupItemId.set(itemId);
        LOGGER.info("GE event: setup build fired, cached itemId=" + itemId
            + ", offerType=" + client.getVarbitValue(net.runelite.api.gameval.VarbitID.GE_NEWOFFER_TYPE));
        requestChatboxRefresh();
    }

    private void scheduleLoginReconcile()
    {
        if (loginReconcileScheduled.compareAndSet(false, true))
        {
            executor.schedule(() ->
            {
                try
                {
                    refreshFromServer(true);
                }
                finally
                {
                    loginReconcileScheduled.set(false);
                }
            }, LOGIN_RECONCILE_DELAY.toSeconds(), TimeUnit.SECONDS);
        }
    }

    private void refreshFromStoredState(boolean triggerRefresh)
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
                if (triggerRefresh)
                {
                    refreshFromServer(true);
                }
                return;
            }

            renderCurrentState("Not paired");
        });
    }

    private void refreshFromServer(boolean reconcileAfterRefresh)
    {
        if (!hasStoredPairing())
        {
            renderCurrentState("Not paired");
            return;
        }

        try
        {
            URI baseUri = resolveBaseUri();
            DashboardSnapshot dashboard = flipperApiClient.getDashboard(baseUri, config.deviceToken());
            HistorySnapshot history = flipperApiClient.getHistory(baseUri, config.deviceToken(), 30, 50);
            dashboardRef.set(dashboard);
            historyRef.set(history);
            selectFocusedFlip(dashboard);
            panel.renderDashboard(dashboard);
            requestChatboxRefresh();
            renderCurrentState("Synced " + dashboard.activeFlips().size() + " active flips");
            postLog("Loaded dashboard with " + dashboard.activeFlips().size() + " active flips and " + history.flips().size() + " history entries.");
            if (reconcileAfterRefresh)
            {
                reconcileFromLiveClientState();
            }
        }
        catch (PluginAuthException ex)
        {
            LOGGER.warn("Sync rejected", ex);
            postLog(ex.getMessage());
            clearStoredPairing();
            cancelHeartbeatLoop();
            dashboardRef.set(null);
            historyRef.set(null);
            requestChatboxRefresh();
            renderCurrentState("Token rejected");
        }
        catch (IOException ex)
        {
            LOGGER.error("Sync failed", ex);
            postLog("Sync failed: " + ex.getMessage());
            renderCurrentState("Sync failed");
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            postLog("Sync was interrupted.");
            renderCurrentState("Sync interrupted");
        }
    }

    private void reconcileFromLiveClientState()
    {
        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard == null || dashboard.activeFlips().isEmpty() || client.getGrandExchangeOffers() == null)
        {
            return;
        }

        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        for (ActiveFlipSnapshot flip : dashboard.activeFlips())
        {
            int offerIndex = Math.max(0, flip.slotIndex() - 1);
            if (offerIndex >= offers.length)
            {
                continue;
            }

            GrandExchangeOffer offer = offers[offerIndex];
            if (offer == null)
            {
                continue;
            }

            syncGrandExchangeOffer(offerIndex, offer);
        }
    }

    private void syncGrandExchangeOffer(int slotIndexZeroBased, GrandExchangeOffer offer)
    {
        if (!hasStoredPairing())
        {
            return;
        }

        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard == null)
        {
            return;
        }

        Optional<ActiveFlipSnapshot> maybeFlip = dashboard.activeFlips().stream()
            .filter(flip -> flip.slotIndex() == slotIndexZeroBased + 1)
            .findFirst();
        if (!maybeFlip.isPresent())
        {
            return;
        }

        ActiveFlipSnapshot flip = maybeFlip.get();
        setFocusedFlip(flip.id());
        syncStatus.set("GE slot " + flip.slotIndex() + " => " + flip.effectiveStatusLabel());

        try
        {
            switch (offer.getState())
            {
                case BUYING:
                    syncBuyProgress(flip, offer, false);
                    break;
                case BOUGHT:
                    syncBuyProgress(flip, offer, true);
                    break;
                case CANCELLED_BUY:
                    if (offer.getQuantitySold() > 0)
                    {
                        syncBuyProgress(flip, offer, true);
                    }
                    else
                    {
                        cancelBuy(flip);
                    }
                    break;
                case SELLING:
                    syncSellProgress(flip, offer, false);
                    break;
                case SOLD:
                    syncSellProgress(flip, offer, true);
                    break;
                case CANCELLED_SELL:
                    if (offer.getQuantitySold() > 0)
                    {
                        syncSellProgress(flip, offer, false);
                    }
                    else
                    {
                        refreshSellPrice(flip);
                    }
                    break;
                default:
                    return;
            }
        }
        catch (PluginAuthException ex)
        {
            LOGGER.warn("GE sync rejected", ex);
            postLog(ex.getMessage());
            clearStoredPairing();
            cancelHeartbeatLoop();
            renderCurrentState("Token rejected");
        }
        catch (IOException ex)
        {
            LOGGER.warn("GE sync failed", ex);
            postLog("GE sync failed: " + ex.getMessage());
            renderCurrentState("GE sync failed");
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            postLog("GE sync was interrupted.");
            renderCurrentState("GE sync interrupted");
        }
    }

    private void syncBuyProgress(ActiveFlipSnapshot flip, GrandExchangeOffer offer, boolean concludeBuy) throws IOException, InterruptedException
    {
        int boughtQuantity = Math.max(offer.getQuantitySold(), 0);
        if (boughtQuantity <= 0 && !concludeBuy)
        {
            return;
        }

        long spendGp = Math.max(offer.getSpent(), 0);
        DashboardSnapshot dashboard = dashboardRef.get();
        String lastKnown = (dashboard == null ? Instant.now() : flip.updatedAtUtc()).toString();
        URI baseUri = resolveBaseUri();
        List<ActiveFlipSnapshot> updated = flipperApiClient.updateActiveFlipBuy(
            baseUri,
            config.deviceToken(),
            flip.id(),
            new FlipperApiClient.UpdateBuyRequest(
                boughtQuantity,
                spendGp,
                Instant.now().toString(),
                null,
                concludeBuy,
                "runelite",
                UUID.randomUUID().toString(),
                lastKnown));
        updateDashboardAfterMutation(updated);
        postLog("Saved buy progress for " + flip.itemName() + " (" + boughtQuantity + "/" + flip.plannedQuantity() + ")");
    }

    private void cancelBuy(ActiveFlipSnapshot flip) throws IOException, InterruptedException
    {
        URI baseUri = resolveBaseUri();
        List<ActiveFlipSnapshot> updated = flipperApiClient.cancelActiveFlipBuy(
            baseUri,
            config.deviceToken(),
            flip.id(),
            new FlipperApiClient.CancelBuyRequest("runelite", UUID.randomUUID().toString(), flip.updatedAtUtc().toString()));
        updateDashboardAfterMutation(updated);
        postLog("Cancelled buy for " + flip.itemName());
    }

    private void syncSellProgress(ActiveFlipSnapshot flip, GrandExchangeOffer offer, boolean concludeSell) throws IOException, InterruptedException
    {
        int soldQuantity = Math.max(offer.getQuantitySold(), 0);
        if (soldQuantity <= 0 && !concludeSell)
        {
            return;
        }

        long receiveGp = Math.max(offer.getSpent(), 0);
        URI baseUri = resolveBaseUri();
        List<ActiveFlipSnapshot> updated = flipperApiClient.updateActiveFlipSell(
            baseUri,
            config.deviceToken(),
            flip.id(),
            new FlipperApiClient.UpdateSellRequest(
                soldQuantity,
                receiveGp,
                Instant.now().toString(),
                concludeSell,
                "runelite",
                UUID.randomUUID().toString(),
                flip.updatedAtUtc().toString()));
        updateDashboardAfterMutation(updated);
        postLog("Saved sell progress for " + flip.itemName() + " (" + soldQuantity + "/" + currentSellQuantity(flip) + ")");
    }

    private void refreshSellPrice(ActiveFlipSnapshot flip) throws IOException, InterruptedException
    {
        URI baseUri = resolveBaseUri();
        List<ActiveFlipSnapshot> updated = flipperApiClient.refreshActiveFlipSellPrice(
            baseUri,
            config.deviceToken(),
            flip.id(),
            new FlipperApiClient.RefreshSellPriceRequest("runelite", UUID.randomUUID().toString(), flip.updatedAtUtc().toString()));
        updateDashboardAfterMutation(updated);
        postLog("Refreshed sell price for " + flip.itemName());
    }

    private void performHelperAction(GeAssistAction action)
    {
        ActiveFlipSnapshot flip = getFocusedFlip();
        if (flip == null)
        {
            postLog("No active flip is focused.");
            return;
        }

        postLog("Helper action " + action + " using flip slot " + flip.slotIndex() + " item " + flip.itemName()
            + " status=" + flip.effectiveStatusLabel()
            + " currentGeItem=" + client.getVarpValue(net.runelite.api.VarPlayer.CURRENT_GE_ITEM));

        switch (action)
        {
            case SEARCH_ITEM:
                geInputTyper.pasteTextAndEnter(flip.itemName());
                postLog("Pasted search term for " + flip.itemName());
                return;
            case FILL_BUY_PRICE:
                geInputTyper.pasteTextAndEnter(Long.toString(flip.plannedBuyPriceGp()));
                postLog("Pasted buy price for " + flip.itemName());
                return;
            case FILL_BUY_QUANTITY:
                geInputTyper.pasteTextAndEnter(Integer.toString(flip.plannedQuantity()));
                postLog("Pasted buy quantity for " + flip.itemName());
                return;
            case FILL_SELL_PRICE:
                long sellPrice = flip.reevaluatedSellPriceGp() != null ? flip.reevaluatedSellPriceGp() : flip.plannedSellPriceGp();
                geInputTyper.pasteTextAndEnter(Long.toString(sellPrice));
                postLog("Pasted sell price for " + flip.itemName());
                return;
            case FILL_SELL_QUANTITY:
                geInputTyper.pasteTextAndEnter(Integer.toString(currentSellQuantity(flip)));
                postLog("Pasted sell quantity for " + flip.itemName());
                return;
            case CANCEL_BUY:
                executor.submit(() ->
                {
                    try
                    {
                        cancelBuy(flip);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.warn("Failed to cancel buy", ex);
                        postLog("Failed to cancel buy: " + ex.getMessage());
                    }
                });
                return;
            case CONCLUDE_BUY:
                executor.submit(() ->
                {
                    try
                    {
                        syncBuyProgress(flip, fakeConcludedBuyOffer(flip), true);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.warn("Failed to conclude buy", ex);
                        postLog("Failed to conclude buy: " + ex.getMessage());
                    }
                });
                return;
            case REFRESH_SELL_PRICE:
                executor.submit(() ->
                {
                    try
                    {
                        refreshSellPrice(flip);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.warn("Failed to refresh sell price", ex);
                        postLog("Failed to refresh sell price: " + ex.getMessage());
                    }
                });
                return;
            case CONCLUDE_SELL:
                executor.submit(() ->
                {
                    try
                    {
                        syncSellProgress(flip, fakeConcludedSellOffer(flip), true);
                    }
                    catch (Exception ex)
                    {
                        LOGGER.warn("Failed to conclude sell", ex);
                        postLog("Failed to conclude sell: " + ex.getMessage());
                    }
                });
                return;
            case REORDER:
                postLog("Reorder helper is handled from the web app for now.");
                return;
            default:
        }
    }

    private GrandExchangeOffer fakeConcludedBuyOffer(ActiveFlipSnapshot flip)
    {
        return new GrandExchangeOffer()
        {
            @Override
            public int getQuantitySold()
            {
                return flip.plannedQuantity();
            }

            @Override
            public int getItemId()
            {
                return 0;
            }

            @Override
            public int getTotalQuantity()
            {
                return flip.plannedQuantity();
            }

            @Override
            public int getPrice()
            {
                return (int) flip.plannedBuyPriceGp();
            }

            @Override
            public int getSpent()
            {
                long total = flip.actualBuySpendGp() != null ? flip.actualBuySpendGp() : flip.plannedBuyPriceGp() * flip.plannedQuantity();
                return (int) Math.min(Integer.MAX_VALUE, total);
            }

            @Override
            public GrandExchangeOfferState getState()
            {
                return GrandExchangeOfferState.BOUGHT;
            }
        };
    }

    private GrandExchangeOffer fakeConcludedSellOffer(ActiveFlipSnapshot flip)
    {
        return new GrandExchangeOffer()
        {
            @Override
            public int getQuantitySold()
            {
                return currentSellQuantity(flip);
            }

            @Override
            public int getItemId()
            {
                return 0;
            }

            @Override
            public int getTotalQuantity()
            {
                return currentSellQuantity(flip);
            }

            @Override
            public int getPrice()
            {
                long price = flip.reevaluatedSellPriceGp() != null ? flip.reevaluatedSellPriceGp() : flip.plannedSellPriceGp();
                return (int) Math.min(Integer.MAX_VALUE, price);
            }

            @Override
            public int getSpent()
            {
                long received = flip.actualSellReceiveGp() != null ? flip.actualSellReceiveGp() : 0L;
                return (int) Math.min(Integer.MAX_VALUE, received);
            }

            @Override
            public GrandExchangeOfferState getState()
            {
                return GrandExchangeOfferState.SOLD;
            }
        };
    }

    private int currentSellQuantity(ActiveFlipSnapshot flip)
    {
        return flip.actualBoughtQuantity() > 0 ? flip.actualBoughtQuantity() : flip.plannedQuantity();
    }

    private void updateDashboardAfterMutation(List<ActiveFlipSnapshot> activeFlips)
    {
        DashboardSnapshot current = dashboardRef.get();
        if (current == null)
        {
            return;
        }

        DashboardSnapshot updated = new DashboardSnapshot(
            activeFlips,
            current.availableSlots(),
            current.requestDefaultBudgetGp(),
            current.requestDefaultBuyLimitWindows(),
            Instant.now().toEpochMilli());
        dashboardRef.set(updated);
        panel.renderDashboard(updated);
        requestChatboxRefresh();
        selectFocusedFlip(updated);
        renderCurrentState("Synced " + activeFlips.size() + " active flips");
    }

    private void schedulePollingLoop()
    {
        cancelPollingLoop();
        pollFuture = executor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                if (hasStoredPairing())
                {
                    refreshFromServer(false);
                }
            }
        }, POLL_INTERVAL.toSeconds(), POLL_INTERVAL.toSeconds(), TimeUnit.SECONDS);
    }

    private void scheduleHelperRefreshLoop()
    {
        cancelHelperRefreshLoop();
        helperRefreshFuture = executor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                requestChatboxRefresh();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void cancelHelperRefreshLoop()
    {
        if (helperRefreshFuture != null)
        {
            helperRefreshFuture.cancel(false);
            helperRefreshFuture = null;
        }
    }

    private void cancelPollingLoop()
    {
        if (pollFuture != null)
        {
            pollFuture.cancel(false);
            pollFuture = null;
        }
    }

    private void setBusy(boolean busy)
    {
        if (panel != null)
        {
            panel.setBusy(busy);
        }
    }

    private void scheduleHeartbeatLoop()
    {
        cancelHeartbeatLoop();
        heartbeatFuture = executor.scheduleAtFixedRate(new Runnable()
        {
            @Override
            public void run()
            {
                runHeartbeat(false);
            }
        }, HEARTBEAT_INTERVAL.toMinutes(), HEARTBEAT_INTERVAL.toMinutes(), TimeUnit.MINUTES);
    }

    private void cancelHeartbeatLoop()
    {
        if (heartbeatFuture != null)
        {
            heartbeatFuture.cancel(false);
            heartbeatFuture = null;
        }
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
            LOGGER.warn("Heartbeat rejected", ex);
            postLog(ex.getMessage());
            clearStoredPairing();
            cancelHeartbeatLoop();
            renderCurrentState("Token rejected");
        }
        catch (IOException ex)
        {
            LOGGER.error("Heartbeat failed", ex);
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

    private void selectFocusedFlip(DashboardSnapshot dashboard)
    {
        UUID selectedId = focusedFlipId.get();
        if (selectedId != null && dashboard.activeFlips().stream().anyMatch(flip -> flip.id().equals(selectedId)))
        {
            return;
        }

        Integer slotIndex = focusedSlotIndex.get();
        if (slotIndex != null && dashboard.activeFlips().stream().anyMatch(flip -> flip.slotIndex() == slotIndex))
        {
            dashboard.activeFlips().stream()
                .filter(flip -> flip.slotIndex() == slotIndex)
                .findFirst()
                .ifPresent(flip -> focusedFlipId.set(flip.id()));
            return;
        }
    }

    private ActiveFlipSnapshot getFocusedFlip()
    {
        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard == null || dashboard.activeFlips().isEmpty())
        {
            return null;
        }

        UUID selectedId = focusedFlipId.get();
        if (selectedId != null)
        {
            for (ActiveFlipSnapshot flip : dashboard.activeFlips())
            {
                if (selectedId.equals(flip.id()))
                {
                    return flip;
                }
            }
        }

        Integer slotIndex = focusedSlotIndex.get();
        if (slotIndex != null)
        {
            for (ActiveFlipSnapshot flip : dashboard.activeFlips())
            {
                if (flip.slotIndex() == slotIndex)
                {
                    return flip;
                }
            }
        }

        return dashboard.activeFlips().stream().findFirst().orElse(null);
    }

    private void setFocusedFlip(UUID flipId)
    {
        focusedFlipId.set(flipId);
    }

    private int getCurrentSetupItemId()
    {
        return currentSetupItemId.get();
    }

    private void handleGeCanvasClick(java.awt.Point point)
    {
        if (point == null)
        {
            return;
        }

        DashboardSnapshot dashboard = dashboardRef.get();
        if (dashboard == null)
        {
            return;
        }

        List<GeSlotTarget> targets = getGeSlotTargets();
        for (GeSlotTarget target : targets)
        {
            if (target.bounds() != null && target.bounds().contains(point))
            {
                focusedSlotIndex.set(target.slotIndex());
                dashboard.activeFlips().stream()
                    .filter(flip -> flip.slotIndex() == target.slotIndex())
                    .findFirst()
                    .ifPresent(flip ->
                    {
                        focusedFlipId.set(flip.id());
                        syncStatus.set("GE slot " + target.slotIndex() + " selected");
                        requestChatboxRefresh();
                        LOGGER.info("GE slot selected from canvas click: slot=" + target.slotIndex() + ", item=" + flip.itemName());
                        postLog("Selected GE slot " + target.slotIndex() + " -> " + flip.itemName());
                    });
                return;
            }
        }
    }

    private List<GeSlotTarget> getGeSlotTargets()
    {
        List<GeSlotTarget> targets = new java.util.ArrayList<>();
        net.runelite.api.widgets.Widget root = client.getWidget(WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER);
        if (root == null || root.isHidden())
        {
            root = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
        }
        if (root == null || root.isHidden())
        {
            return targets;
        }

        java.awt.Rectangle bounds = root.getBounds();
        if (bounds == null)
        {
            return targets;
        }

        int columns = 4;
        int rows = 2;
        int paddingX = Math.max(8, bounds.width / 50);
        int paddingY = Math.max(16, bounds.height / 10);
        int gapX = Math.max(6, bounds.width / 80);
        int gapY = Math.max(6, bounds.height / 80);
        int slotWidth = Math.max(1, (bounds.width - (paddingX * 2) - (gapX * (columns - 1))) / columns);
        int slotHeight = Math.max(1, (bounds.height - paddingY - (gapY * (rows - 1))) / rows);
        int startX = bounds.x + paddingX;
        int startY = bounds.y + paddingY;

        for (int row = 0; row < rows; row++)
        {
            for (int column = 0; column < columns; column++)
            {
                int x = startX + (column * (slotWidth + gapX));
                int y = startY + (row * (slotHeight + gapY));
                targets.add(new GeSlotTarget((row * columns) + column + 1, new java.awt.Rectangle(x, y, slotWidth, slotHeight)));
            }
        }

        return targets;
    }

    private boolean isEmptySlot(ActiveFlipSnapshot flip)
    {
        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
        if (offers == null)
        {
            return false;
        }

        int offerIndex = Math.max(0, flip.slotIndex() - 1);
        if (offerIndex >= offers.length)
        {
            return true;
        }

        GrandExchangeOffer offer = offers[offerIndex];
        return offer == null || offer.getState() == GrandExchangeOfferState.EMPTY;
    }

    private int resolveGeSlotIndex(MenuOptionClicked event)
    {
        if (event.getWidget() != null)
        {
            int widgetIndex = event.getWidget().getIndex();
            if (widgetIndex >= 0 && widgetIndex < 8)
            {
                return widgetIndex + 1;
            }
        }

        int param0 = event.getParam0();
        if (param0 >= 0 && param0 < 8)
        {
            return param0 + 1;
        }

        int param1 = event.getParam1();
        if (param1 >= 0 && param1 < 8)
        {
            return param1 + 1;
        }

        int actionParam = event.getActionParam();
        if (actionParam >= 0 && actionParam < 8)
        {
            return actionParam + 1;
        }

        return -1;
    }

    private void renderCurrentState(String statusMessage)
    {
        syncStatus.set(statusMessage);
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

    private void requestChatboxRefresh()
    {
        if (chatboxAssistant == null)
        {
            return;
        }

        clientThread.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                chatboxAssistant.refresh();
            }
        });
    }
}
