package com.osrsflipperv2.runelite;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.ItemContainer;
import net.runelite.api.VarClientStr;
import net.runelite.api.VarPlayer;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeAssistOverlay extends Overlay
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeAssistOverlay.class);
    private static final Color BACKGROUND = new Color(0, 0, 0, 170);
    private static final Color BORDER = new Color(255, 255, 255, 70);
    private static final Color TEXT = new Color(240, 240, 240);
    private static final Color HIGHLIGHT = new Color(51, 160, 250, 90);
    private static final Color BUY_HIGHLIGHT = new Color(51, 160, 250, 110);
    private static final Color SELL_HIGHLIGHT = new Color(255, 160, 51, 110);
    private static final Color INPUT_HIGHLIGHT = new Color(80, 255, 255, 110);
    private static final Color CONFIRM_HIGHLIGHT = new Color(120, 255, 120, 120);

    private final Client client;
    private final ItemManager itemManager;
    private final Supplier<DashboardSnapshot> dashboardSupplier;
    private final Supplier<ActiveFlipSnapshot> focusedFlipSupplier;
    private final Supplier<Integer> focusedSlotIndexSupplier;
    private final Supplier<List<GeSlotTarget>> slotTargetsSupplier;
    private final Supplier<Integer> currentSetupItemIdSupplier;
    private final Supplier<String> statusSupplier;
    private final Consumer<GeAssistAction> actionConsumer;
    private String lastDebugKey = "";

    public GeAssistOverlay(
        Client client,
        ItemManager itemManager,
        Supplier<DashboardSnapshot> dashboardSupplier,
        Supplier<ActiveFlipSnapshot> focusedFlipSupplier,
        Supplier<Integer> focusedSlotIndexSupplier,
        Supplier<List<GeSlotTarget>> slotTargetsSupplier,
        Supplier<Integer> currentSetupItemIdSupplier,
        Supplier<String> statusSupplier,
        Consumer<GeAssistAction> actionConsumer)
    {
        this.client = client;
        this.itemManager = itemManager;
        this.dashboardSupplier = dashboardSupplier;
        this.focusedFlipSupplier = focusedFlipSupplier;
        this.focusedSlotIndexSupplier = focusedSlotIndexSupplier;
        this.slotTargetsSupplier = slotTargetsSupplier;
        this.currentSetupItemIdSupplier = currentSetupItemIdSupplier;
        this.statusSupplier = statusSupplier;
        this.actionConsumer = actionConsumer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGHEST);

        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Search item", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.SEARCH_ITEM);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Fill buy price", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.FILL_BUY_PRICE);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Fill buy quantity", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.FILL_BUY_QUANTITY);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Fill sell price", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.FILL_SELL_PRICE);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Fill sell quantity", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.FILL_SELL_QUANTITY);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Cancel buy", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.CANCEL_BUY);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Conclude buy", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.CONCLUDE_BUY);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Refresh sell price", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.REFRESH_SELL_PRICE);
            }
        });
        addMenuEntry(net.runelite.api.MenuAction.RUNELITE_OVERLAY, "Conclude sell", "Osrs Flipper V2", new Consumer<net.runelite.api.MenuEntry>()
        {
            @Override
            public void accept(net.runelite.api.MenuEntry e)
            {
                actionConsumer.accept(GeAssistAction.CONCLUDE_SELL);
            }
        });
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        DashboardSnapshot dashboard = dashboardSupplier.get();
        if (dashboard == null || client.getGameState() != net.runelite.api.GameState.LOGGED_IN)
        {
            return null;
        }

        if (isOfferSetupOpen())
        {
            LOGGER.info("GE overlay: setup render open; focused=" + describeFlip(focusedFlipSupplier.get())
                + ", focusedSlotIndex=" + focusedSlotIndexSupplier.get()
                + ", setupItemId=" + currentSetupItemIdSupplier.get()
                + ", status=" + statusSupplier.get());
            renderTradeSetupHighlights(graphics, dashboard);
        }
        else
        {
            logOnce("setup-closed", "GE overlay: offer window closed; rendering slot overlays");
            renderSlotButtonHighlights(graphics, dashboard);
            renderSlotTextOverlays(graphics, dashboard);
        }

        return null;
    }

    private void renderSlotButtonHighlights(Graphics2D graphics, DashboardSnapshot dashboard)
    {
        for (ActiveFlipSnapshot flip : dashboard.activeFlips())
        {
            if (flip == null || isSlotLocked(dashboard, flip))
            {
                continue;
            }

            Widget slotWidget = getSlotWidget(flip.slotIndex());
            if (slotWidget == null || slotWidget.isHidden())
            {
                continue;
            }

            Rectangle bounds = slotWidget.getBounds();
            if (bounds == null)
            {
                continue;
            }

            Widget buttonWidget = findWidgetByNeedles(slotWidget, flip.isBuyPhase() ? "buy" : "sell");
            Rectangle buttonBounds = buttonWidget != null && buttonWidget.getBounds() != null
                ? buttonWidget.getBounds()
                : getButtonBounds(bounds, flip.isBuyPhase());
            graphics.setColor(flip.isBuyPhase() ? BUY_HIGHLIGHT : SELL_HIGHLIGHT);
            graphics.fillRoundRect(buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height, 8, 8);
            graphics.setColor(flip.isBuyPhase() ? new Color(51, 160, 250, 220) : new Color(255, 160, 51, 220));
            graphics.drawRoundRect(buttonBounds.x, buttonBounds.y, buttonBounds.width, buttonBounds.height, 8, 8);
        }
    }

    private void renderSlotTextOverlays(Graphics2D graphics, DashboardSnapshot dashboard)
    {
        for (int slotIndex = 1; slotIndex <= 8; slotIndex++)
        {
            Widget slotWidget = getSlotWidget((short) slotIndex);
            if (slotWidget == null || slotWidget.isHidden() || slotWidget.getBounds() == null)
            {
                continue;
            }

            Rectangle bounds = slotWidget.getBounds();
            drawSlotAnchorDot(graphics, bounds);
            drawSlotTextOverlay(graphics, bounds, "test", new Color(51, 160, 250), TEXT, slotIndex);
        }
    }

    private void renderTradeSetupHighlights(Graphics2D graphics, DashboardSnapshot dashboard)
    {
        int cachedSetupItemId = currentSetupItemIdSupplier.get() == null ? -1 : currentSetupItemIdSupplier.get();

        Widget setupRoot = getSetupWidget();
        if (setupRoot == null || setupRoot.isHidden())
        {
            logOnce("setup-root-missing", "GE overlay: setup widget missing; focusedFlip=" + describeFlip(focusedFlipSupplier.get())
                + ", cachedSetupItemId=" + cachedSetupItemId);
            return;
        }

        int setupItemId = resolveSetupItemId(setupRoot);
        if (setupItemId <= 0)
        {
            setupItemId = cachedSetupItemId;
        }
        if (setupItemId <= 0)
        {
            logOnce("setup-item-missing", "GE overlay: setup item id could not be resolved; cachedSetupItemId="
                + cachedSetupItemId + ", focusedFlip=" + describeFlip(focusedFlipSupplier.get()) + ", root=" + describeWidget(setupRoot));
            dumpSetupWidgetActions(setupRoot);
            return;
        }

        ActiveFlipSnapshot setupFlip = resolveSetupFlip(dashboard, setupItemId);
        if (setupFlip == null)
        {
            logOnce("setup-flip-missing", "GE overlay: no flip resolved for setup itemId=" + setupItemId
                + ", cachedSetupItemId=" + cachedSetupItemId + ", focusedFlip=" + describeFlip(focusedFlipSupplier.get())
                + ", root=" + describeWidget(setupRoot));
            dumpSetupWidgetActions(setupRoot);
        }

        ActiveFlipSnapshot targetFlip = null;
        Integer focusedSlotIndex = focusedSlotIndexSupplier.get();
        if (dashboard != null && focusedSlotIndex != null)
        {
            targetFlip = resolveFlipForSlot(dashboard, focusedSlotIndex);
        }
        if (targetFlip == null)
        {
            targetFlip = focusedFlipSupplier.get();
        }

        logOnce("setup-target:" + setupItemId,
            "GE overlay: setup target flip=" + describeFlip(targetFlip)
                + ", setupFlip=" + describeFlip(setupFlip)
                + ", focusedFlip=" + describeFlip(focusedFlipSupplier.get())
                + ", focusedSlotIndex=" + focusedSlotIndex
                + ", root=" + describeWidget(setupRoot));

        if (targetFlip == null)
        {
            LOGGER.info("GE overlay: setup target missing; focusedFlip=" + describeFlip(focusedFlipSupplier.get())
                + ", focusedSlotIndex=" + focusedSlotIndex + ", setupItemId=" + setupItemId);
            return;
        }

        boolean itemSelected = isExpectedItemSelected(targetFlip);
        long expectedQuantity = quantityFor(targetFlip);
        long expectedPrice = priceFor(targetFlip);
        long currentQuantity = client.getVarbitValue(net.runelite.api.gameval.VarbitID.GE_NEWOFFER_QUANTITY);
        long currentPrice = client.getVarbitValue(net.runelite.api.gameval.VarbitID.GE_NEWOFFER_PRICE);
        boolean quantityMatches = expectedQuantity > 0 && currentQuantity == expectedQuantity;
        boolean priceMatches = expectedPrice > 0 && currentPrice == expectedPrice;
        boolean showQuantityOverlay = itemSelected && !quantityMatches;
        boolean showPriceOverlay = itemSelected && !priceMatches;
        boolean showConfirmOverlay = itemSelected && quantityMatches && priceMatches;

        Widget quantityWidget = firstNonNull(
            findWidgetByNeedles(setupRoot, "enter quantity", "set quantity"),
            client.getWidget(InterfaceID.GeOffers.SETUP_FEE));
        Widget priceWidget = firstNonNull(
            findWidgetByNeedles(setupRoot, "enter price", "custom price", "set price"),
            client.getWidget(InterfaceID.GeOffers.SETUP_MARKETPRICE));
        Widget confirmWidget = firstNonNull(
            findWidgetByNeedles(setupRoot, "confirm"),
            client.getWidget(InterfaceID.GeOffers.SETUP_CONFIRM));

        if (quantityWidget == null || priceWidget == null || confirmWidget == null)
        {
            logOnce("setup-widgets", "GE overlay: setup widgets resolved quantity=" + describeWidget(quantityWidget)
                + ", price=" + describeWidget(priceWidget) + ", confirm=" + describeWidget(confirmWidget)
                + ", root=" + describeWidget(setupRoot));
            dumpSetupWidgetActions(setupRoot);
        }

        logOnce("setup-status:" + setupItemId + ":" + currentQuantity + ":" + currentPrice + ":" + showQuantityOverlay + ":" + showPriceOverlay + ":" + showConfirmOverlay,
            "GE overlay: setup highlight itemId=" + setupItemId
            + ", flip=" + describeFlip(setupFlip)
            + ", expectedQuantity=" + expectedQuantity
            + ", currentQuantity=" + currentQuantity
            + ", quantityMatches=" + quantityMatches
            + ", expectedPrice=" + expectedPrice
            + ", currentPrice=" + currentPrice
            + ", priceMatches=" + priceMatches
            + ", itemSelected=" + itemSelected
            + ", showQuantityOverlay=" + showQuantityOverlay
            + ", showPriceOverlay=" + showPriceOverlay
            + ", showConfirmOverlay=" + showConfirmOverlay
            + ", quantityWidget=" + describeWidget(quantityWidget)
            + ", priceWidget=" + describeWidget(priceWidget)
            + ", confirmWidget=" + describeWidget(confirmWidget));

        if (showQuantityOverlay)
        {
            drawWidgetOverlay(graphics, quantityWidget, INPUT_HIGHLIGHT, true);
        }

        if (showPriceOverlay)
        {
            drawWidgetOverlay(graphics, priceWidget, INPUT_HIGHLIGHT, true);
        }

        if (showConfirmOverlay)
        {
            drawWidgetOverlay(graphics, confirmWidget, CONFIRM_HIGHLIGHT, true);
        }

        if (targetFlip.isSellPhase() && !itemSelected)
        {
            LOGGER.info("GE overlay: invoking sell highlight; targetFlip=" + describeFlip(targetFlip)
                + ", setupItemId=" + setupItemId
                + ", focusedSlotIndex=" + focusedSlotIndex
                + ", itemSelected=" + itemSelected
                + ", currentQuantity=" + currentQuantity
                + ", currentPrice=" + currentPrice);
            renderSellInventoryHighlight(graphics, targetFlip, setupItemId);
        }
    }

    private ActiveFlipSnapshot resolveFlipForSlot(DashboardSnapshot dashboard, int slotIndex)
    {
        if (dashboard == null || slotIndex <= 0)
        {
            return null;
        }

        for (ActiveFlipSnapshot candidate : dashboard.activeFlips())
        {
            if (candidate != null && candidate.slotIndex() == slotIndex)
            {
                return candidate;
            }
        }

        return null;
    }

    private ActiveFlipSnapshot resolveSetupFlip(DashboardSnapshot dashboard, int setupItemId)
    {
        if (dashboard == null)
        {
            return null;
        }

        String setupItemName = Objects.toString(client.getItemDefinition(setupItemId).getName(), "");
        if (setupItemName.isBlank())
        {
            return null;
        }

        String standardizedSetupName = Text.standardize(setupItemName);
        for (ActiveFlipSnapshot candidate : dashboard.activeFlips())
        {
            if (candidate == null)
            {
                continue;
            }

            if (Objects.equals(Text.standardize(candidate.itemName()), standardizedSetupName))
            {
                return candidate;
            }
        }

        return null;
    }

    private void drawWidgetOverlay(Graphics2D graphics, Widget widget, Color color)
    {
        drawWidgetOverlay(graphics, widget, color, false);
    }

    private void drawWidgetOverlay(Graphics2D graphics, Widget widget, Color color, boolean preferInnerControl)
    {
        if (widget == null || widget.isHidden())
        {
            return;
        }

        Rectangle bounds = preferInnerControl ? resolveInnerControlBounds(widget) : resolveOverlayBounds(widget);
        if (bounds == null)
        {
            return;
        }

        if (preferInnerControl)
        {
            bounds = tightenInnerControlBounds(bounds);
        }

        graphics.setColor(color);
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
        graphics.setColor(color.darker());
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
    }

    private Rectangle resolveInnerControlBounds(Widget widget)
    {
        if (widget == null)
        {
            return null;
        }

        Rectangle bounds = widget.getBounds();
        if (bounds == null)
        {
            return null;
        }

        Rectangle best = bounds;
        best = chooseMoreSpecificBounds(best, resolveBestChildBounds(widget.getChildren()));
        best = chooseMoreSpecificBounds(best, resolveBestChildBounds(widget.getDynamicChildren()));
        best = chooseMoreSpecificBounds(best, resolveBestChildBounds(widget.getStaticChildren()));
        best = chooseMoreSpecificBounds(best, resolveBestChildBounds(widget.getNestedChildren()));
        return best;
    }

    private Rectangle resolveBestChildBounds(Widget[] widgets)
    {
        if (widgets == null)
        {
            return null;
        }

        Rectangle best = null;
        for (Widget child : widgets)
        {
            if (child == null || child.isHidden())
            {
                continue;
            }

            Rectangle candidate = child.getBounds();
            if (candidate == null)
            {
                continue;
            }

            best = chooseMoreSpecificBounds(best, candidate);
            Rectangle descendant = resolveBestChildBounds(child.getChildren());
            best = chooseMoreSpecificBounds(best, descendant);
            descendant = resolveBestChildBounds(child.getDynamicChildren());
            best = chooseMoreSpecificBounds(best, descendant);
            descendant = resolveBestChildBounds(child.getStaticChildren());
            best = chooseMoreSpecificBounds(best, descendant);
            descendant = resolveBestChildBounds(child.getNestedChildren());
            best = chooseMoreSpecificBounds(best, descendant);
        }

        return best;
    }

    private Rectangle chooseMoreSpecificBounds(Rectangle current, Rectangle candidate)
    {
        if (candidate == null)
        {
            return current;
        }

        if (current == null)
        {
            return candidate;
        }

        long currentArea = (long) current.width * current.height;
        long candidateArea = (long) candidate.width * candidate.height;
        if (candidateArea <= 0 || candidateArea >= currentArea)
        {
            return current;
        }

        boolean candidateLooksLikeInnerControl = candidate.width <= current.width
            && candidate.height < current.height
            && candidate.width >= Math.max(1, current.width / 2);
        return candidateLooksLikeInnerControl ? candidate : current;
    }

    private Rectangle tightenInnerControlBounds(Rectangle bounds)
    {
        if (bounds == null)
        {
            return null;
        }

        int width = bounds.width;
        int height = Math.max(1, (int) Math.round(width * 0.68));
        if (height >= bounds.height)
        {
            return bounds;
        }

        int x = bounds.x;
        int y = bounds.y + Math.max(0, (bounds.height - height) / 2);
        return new Rectangle(x, y, width, height);
    }

    private Rectangle resolveOverlayBounds(Widget widget)
    {
        Rectangle bounds = widget.getBounds();
        if (bounds == null)
        {
            return null;
        }

        if (bounds.width >= 28 && bounds.width <= 40 && bounds.height >= 28 && bounds.height <= 40)
        {
            Widget parent = widget.getParent();
            if (parent != null && !parent.isHidden())
            {
                Rectangle parentBounds = parent.getBounds();
                if (parentBounds != null)
                {
                    long widgetArea = (long) bounds.width * bounds.height;
                    long parentArea = (long) parentBounds.width * parentBounds.height;
                    if (parentBounds.width > bounds.width
                        && parentBounds.height >= bounds.height
                        && parentArea > widgetArea
                        && parentArea <= widgetArea * 10L)
                    {
                        return parentBounds;
                    }
                }
            }
        }

        return bounds;
    }

    private boolean isOfferSetupOpen()
    {
        Widget widget = getSetupWidget();
        return widget != null && !widget.isHidden();
    }

    private boolean isQuantityDialogOpen()
    {
        Widget title = client.getWidget(net.runelite.api.widgets.WidgetInfo.CHATBOX_TITLE);
        if (title == null)
        {
            return false;
        }

        String text = title.getText();
        return Objects.equals(text, "How many do you wish to buy?")
            || Objects.equals(text, "How many do you wish to sell?");
    }

    private boolean isPriceDialogOpen()
    {
        Widget title = client.getWidget(net.runelite.api.widgets.WidgetInfo.CHATBOX_TITLE);
        if (title == null)
        {
            return false;
        }

        return Objects.equals(title.getText(), "Set a price for each item:");
    }

    private Widget getSetupWidget()
    {
        Widget widget = client.getWidget(InterfaceID.GeOffers.SETUP);
        if (widget != null && !widget.isHidden())
        {
            return widget;
        }

        return client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
    }

    private Widget getChatboxInputWidget()
    {
        Widget widget = client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT);
        if (widget != null)
        {
            return widget;
        }

        return client.getWidget(WidgetInfo.CHATBOX_INPUT);
    }

    private long parseWidgetNumber(Widget widget)
    {
        if (widget == null)
        {
            return -1L;
        }

        String text = Objects.toString(widget.getText(), "");
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty())
        {
            return -1L;
        }

        try
        {
            return Long.parseLong(digits);
        }
        catch (NumberFormatException ex)
        {
            return -1L;
        }
    }

    private boolean isExpectedItemSelected(ActiveFlipSnapshot flip)
    {
        Widget setupRoot = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER);
        if (setupRoot != null && !setupRoot.isHidden())
        {
            int setupItemId = resolveSetupItemId(setupRoot);
            if (setupItemId > 0)
            {
                if (isExpectedItemSelected(flip, setupItemId))
                {
                    return true;
                }

                logOnce("setup-item-mismatch", "GE overlay: setup item mismatch; resolvedItemId=" + setupItemId
                    + ", expectedFlip=" + describeFlip(flip) + ", root=" + describeWidget(setupRoot));
            }
            else
            {
                logOnce("setup-item-missing", "GE overlay: setup root visible but item id could not be resolved; root="
                    + describeWidget(setupRoot));
            }
        }

        int currentItemId = client.getVarpValue(net.runelite.api.gameval.VarPlayerID.TRADINGPOST_SEARCH);
        if (currentItemId <= 0)
        {
            currentItemId = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
        }

        if (currentItemId <= 0)
        {
            return false;
        }

        return isExpectedItemSelected(flip, currentItemId);
    }

    private boolean isExpectedItemSelected(ActiveFlipSnapshot flip, int currentItemId)
    {
        if (flip == null || currentItemId <= 0)
        {
            return false;
        }

        Widget offerTextWidget = client.getWidget(WidgetInfo.GRAND_EXCHANGE_OFFER_TEXT);
        String offerText = offerTextWidget == null ? "" : Objects.toString(offerTextWidget.getText(), "");
        if (!offerText.isBlank() && Text.standardize(offerText).contains(Text.standardize(flip.itemName())))
        {
            return true;
        }

        List<net.runelite.http.api.item.ItemPrice> matches = itemManager.search(flip.itemName());
        for (net.runelite.http.api.item.ItemPrice match : matches)
        {
            if (match.getId() == currentItemId)
            {
                return true;
            }
        }

        return false;
    }

    private int resolveSetupItemId(Widget root)
    {
        if (root == null || root.isHidden())
        {
            return -1;
        }

        int searchedItemId = client.getVarpValue(net.runelite.api.gameval.VarPlayerID.TRADINGPOST_SEARCH);
        if (searchedItemId > 0)
        {
            return searchedItemId;
        }

        if (root.getItemId() > 0)
        {
            return root.getItemId();
        }

        Widget[] children = root.getChildren();
        if (children != null)
        {
            for (Widget child : children)
            {
                int itemId = resolveSetupItemId(child);
                if (itemId > 0)
                {
                    return itemId;
                }
            }
        }

        Widget[] dynamicChildren = root.getDynamicChildren();
        if (dynamicChildren != null)
        {
            for (Widget child : dynamicChildren)
            {
                int itemId = resolveSetupItemId(child);
                if (itemId > 0)
                {
                    return itemId;
                }
            }
        }

        Widget[] staticChildren = root.getStaticChildren();
        if (staticChildren != null)
        {
            for (Widget child : staticChildren)
            {
                int itemId = resolveSetupItemId(child);
                if (itemId > 0)
                {
                    return itemId;
                }
            }
        }

        Widget[] nestedChildren = root.getNestedChildren();
        if (nestedChildren != null)
        {
            for (Widget child : nestedChildren)
            {
                int itemId = resolveSetupItemId(child);
                if (itemId > 0)
                {
                    return itemId;
                }
            }
        }

        return -1;
    }

    private void dumpSetupWidgetActions(Widget root)
    {
        if (root == null)
        {
            return;
        }

        logOnce("setup-dump", "GE overlay: dumping setup widget actions for " + describeWidget(root));
        dumpSetupWidgetActionsRecursive(root, 0);
    }

    private void dumpSetupWidgetActionsRecursive(Widget widget, int depth)
    {
        if (widget == null || widget.isHidden() || depth > 4)
        {
            return;
        }

        String[] actions = widget.getActions();
        if (actions != null)
        {
            StringBuilder builder = new StringBuilder();
            for (String action : actions)
            {
                if (action == null || action.isBlank())
                {
                    continue;
                }
                if (builder.length() > 0)
                {
                    builder.append(", ");
                }
                builder.append(action);
            }

            if (builder.length() > 0)
            {
                LOGGER.info("GE overlay widget depth=" + depth + " actions=" + builder
                    + " widget=" + describeWidget(widget));
            }
        }

        Widget[] children = widget.getChildren();
        if (children != null)
        {
            for (Widget child : children)
            {
                dumpSetupWidgetActionsRecursive(child, depth + 1);
            }
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null)
        {
            for (Widget child : dynamicChildren)
            {
                dumpSetupWidgetActionsRecursive(child, depth + 1);
            }
        }

        Widget[] staticChildren = widget.getStaticChildren();
        if (staticChildren != null)
        {
            for (Widget child : staticChildren)
            {
                dumpSetupWidgetActionsRecursive(child, depth + 1);
            }
        }

        Widget[] nestedChildren = widget.getNestedChildren();
        if (nestedChildren != null)
        {
            for (Widget child : nestedChildren)
            {
                dumpSetupWidgetActionsRecursive(child, depth + 1);
            }
        }
    }

    private long readCurrentInputValue()
    {
        Widget inputWidget = getChatboxInputWidget();
        String input = inputWidget == null ? "" : Objects.toString(inputWidget.getText(), "");
        if (input == null || input.isBlank())
        {
            input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
        }

        if (input == null || input.isBlank())
        {
            input = client.getVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT);
        }

        if (input == null)
        {
            return -1L;
        }

        String digits = input.replaceAll("[^0-9]", "");
        if (digits.isEmpty())
        {
            return -1L;
        }

        try
        {
            return Long.parseLong(digits);
        }
        catch (NumberFormatException ex)
        {
            return -1L;
        }
    }

    private long priceFor(ActiveFlipSnapshot flip)
    {
        if (flip.isBuyPhase())
        {
            return flip.plannedBuyPriceGp();
        }

        return flip.reevaluatedSellPriceGp() != null ? flip.reevaluatedSellPriceGp() : flip.plannedSellPriceGp();
    }

    private long quantityFor(ActiveFlipSnapshot flip)
    {
        return flip.isSellPhase() ? Math.max(flip.actualBoughtQuantity(), flip.plannedQuantity()) : Math.max(flip.plannedQuantity(), 0);
    }

    private void renderSellInventoryHighlight(Graphics2D graphics, ActiveFlipSnapshot flip, int setupItemId)
    {
        Widget inventoryRoot = client.getWidget(WidgetInfo.GRAND_EXCHANGE_INVENTORY_ITEMS_CONTAINER);
        if (inventoryRoot == null || inventoryRoot.isHidden())
        {
            inventoryRoot = client.getWidget(WidgetInfo.INVENTORY);
        }
        if (inventoryRoot == null || inventoryRoot.isHidden())
        {
            logOnce("sell-inventory-root-missing", "GE overlay: inventory root missing while trying to highlight sell item; flip="
                + describeFlip(flip) + ", setupItemId=" + setupItemId);
            return;
        }

        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        int inventoryItemCount = inventory == null || inventory.getItems() == null ? -1 : inventory.getItems().length;
        List<Widget> inventoryItems = new ArrayList<>();
        collectInventoryItemWidgets(inventoryRoot, inventoryItems);
        List<net.runelite.http.api.item.ItemPrice> matches = itemManager.search(flip.itemName());

        logOnce(
            "sell-inventory-state:" + setupItemId + ":" + inventoryItemCount + ":" + inventoryItems.size(),
            "GE overlay: sell inventory scan flip=" + describeFlip(flip)
                + ", setupItemId=" + setupItemId
                + ", focusedSlotIndex=" + focusedSlotIndexSupplier.get()
                + ", inventoryRoot=" + describeWidget(inventoryRoot)
                + ", inventoryContainerItems=" + inventoryItemCount
                + ", collectedWidgets=" + inventoryItems.size()
                + ", matchedItemIds=" + matches.size());

        boolean highlighted = false;
        if (!inventoryItems.isEmpty())
        {
            for (Widget itemWidget : inventoryItems)
            {
                if (itemWidget == null || itemWidget.isHidden())
                {
                    continue;
                }

                int itemId = itemWidget.getItemId();
                int itemQuantity = itemWidget.getItemQuantity();
                if (itemId <= 0)
                {
                    continue;
                }

                boolean itemMatches = itemId == setupItemId || matches.stream().anyMatch(match -> match.getId() == itemId);
                if (!itemMatches)
                {
                    continue;
                }

                Rectangle bounds = itemWidget.getBounds();
                if (bounds == null)
                {
                    continue;
                }

                logOnce("sell-inventory-hit:" + setupItemId + ":" + itemId,
                    "GE overlay: highlighting inventory widget for sell flip=" + describeFlip(flip)
                        + ", itemId=" + itemId
                        + ", itemQuantity=" + itemQuantity
                        + ", widget=" + describeWidget(itemWidget));
                graphics.setColor(new Color(80, 255, 80, 95));
                graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
                graphics.setColor(new Color(80, 255, 80, 220));
                graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
                highlighted = true;
            }
        }

        if (!highlighted && inventory != null && inventory.getItems() != null)
        {
            int[] matchIds = matches.stream().mapToInt(net.runelite.http.api.item.ItemPrice::getId).toArray();
            for (int slot = 0; slot < inventory.getItems().length; slot++)
            {
                net.runelite.api.Item item = inventory.getItems()[slot];
                if (item == null || item.getId() <= 0)
                {
                    continue;
                }

                boolean itemMatches = item.getId() == setupItemId;
                if (!itemMatches)
                {
                    for (int matchId : matchIds)
                    {
                        if (matchId == item.getId())
                        {
                            itemMatches = true;
                            break;
                        }
                    }
                }

                if (!itemMatches)
                {
                    continue;
                }

                Rectangle bounds = resolveInventorySlotBounds(inventoryRoot.getBounds(), slot);
                if (bounds == null)
                {
                    continue;
                }

                logOnce("sell-inventory-grid-hit:" + setupItemId + ":" + slot,
                    "GE overlay: highlighting inferred inventory slot for sell flip=" + describeFlip(flip)
                        + ", slot=" + slot
                        + ", itemId=" + item.getId()
                        + ", itemQuantity=" + item.getQuantity()
                        + ", slotBounds=" + bounds);
                graphics.setColor(new Color(80, 255, 80, 95));
                graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
                graphics.setColor(new Color(80, 255, 80, 220));
                graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
                highlighted = true;
                break;
            }
        }

        if (!highlighted)
        {
            logOnce("sell-inventory-nohit:" + setupItemId,
                "GE overlay: no inventory highlight matched sell flip=" + describeFlip(flip)
                    + ", setupItemId=" + setupItemId
                    + ", inventoryRoot=" + describeWidget(inventoryRoot)
                    + ", inventoryContainer=" + (inventory == null ? "null" : Integer.toString(inventory.getItems().length)));
        }
    }

    private void collectInventoryItemWidgets(Widget widget, List<Widget> matches)
    {
        if (widget == null || widget.isHidden())
        {
            return;
        }

        if (widget.getItemId() > 0 && widget.getBounds() != null)
        {
            matches.add(widget);
        }

        Widget[] children = widget.getChildren();
        if (children != null)
        {
            for (Widget child : children)
            {
                collectInventoryItemWidgets(child, matches);
            }
        }

        Widget[] dynamicChildren = widget.getDynamicChildren();
        if (dynamicChildren != null)
        {
            for (Widget child : dynamicChildren)
            {
                collectInventoryItemWidgets(child, matches);
            }
        }

        Widget[] staticChildren = widget.getStaticChildren();
        if (staticChildren != null)
        {
            for (Widget child : staticChildren)
            {
                collectInventoryItemWidgets(child, matches);
            }
        }

        Widget[] nestedChildren = widget.getNestedChildren();
        if (nestedChildren != null)
        {
            for (Widget child : nestedChildren)
            {
                collectInventoryItemWidgets(child, matches);
            }
        }
    }

    private Rectangle resolveInventorySlotBounds(Rectangle inventoryBounds, int slotIndex)
    {
        if (inventoryBounds == null || slotIndex < 0)
        {
            return null;
        }

        int columns = 4;
        int rows = 7;
        int slotWidth = Math.max(1, inventoryBounds.width / columns);
        int slotHeight = Math.max(1, inventoryBounds.height / rows);
        int column = slotIndex % columns;
        int row = slotIndex / columns;
        if (row >= rows)
        {
            return null;
        }

        return new Rectangle(
            inventoryBounds.x + (column * slotWidth),
            inventoryBounds.y + (row * slotHeight),
            slotWidth,
            slotHeight);
    }

    private void drawSlotTextOverlay(Graphics2D graphics, Rectangle slotBounds, String text, Color background, Color foreground)
    {
        drawSlotTextOverlay(graphics, slotBounds, text, background, foreground, -1);
    }

    private void drawSlotTextOverlay(Graphics2D graphics, Rectangle slotBounds, String text, Color background, Color foreground, int slotIndex)
    {
        if (slotBounds == null || text == null || text.isBlank())
        {
            return;
        }

        FontMetrics metrics = graphics.getFontMetrics();
        int width = Math.max(1, slotBounds.width);
        int height = 22;
        int x = slotBounds.x;
        int y = slotBounds.y;
        int textX = x + ((width - metrics.stringWidth(text)) / 2);
        int textY = y + ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        graphics.setColor(background);
        graphics.fillRoundRect(x, y, width, height, 8, 8);
        graphics.setColor(background.darker());
        graphics.drawRoundRect(x, y, width, height, 8, 8);
        graphics.setColor(foreground);
        graphics.drawString(text, textX, textY);
    }

    private void drawSlotAnchorDot(Graphics2D graphics, Rectangle slotBounds)
    {
        if (slotBounds == null)
        {
            return;
        }

        int dotSize = 4;
        int x = slotBounds.x + 1;
        int y = slotBounds.y + 1;
        graphics.setColor(new Color(255, 255, 0, 220));
        graphics.fillOval(x, y, dotSize, dotSize);
        graphics.setColor(new Color(120, 120, 0, 220));
        graphics.drawOval(x, y, dotSize, dotSize);
    }

    private Widget getSlotWidget(short slotIndex)
    {
        int index = slotIndex - 1;
        if (index < 0 || index > 7)
        {
            return null;
        }

        return client.getWidget(InterfaceID.GeOffers.INDEX_0 + index);
    }

    private boolean isSlotLocked(DashboardSnapshot dashboard, ActiveFlipSnapshot flip)
    {
        String slotLabel = flip.slotLabel();
        String slotIndex = Short.toString(flip.slotIndex());
        for (DashboardSlotSnapshot slot : dashboard.availableSlots())
        {
            if (!slot.locked())
            {
                continue;
            }

            if (slotMatches(slot, slotLabel, slotIndex))
            {
                return true;
            }
        }

        return false;
    }

    private boolean slotMatches(DashboardSlotSnapshot slot, String slotLabel, String slotIndex)
    {
        String label = slot.label();
        String id = slot.id();
        return (label != null && (label.equalsIgnoreCase(slotLabel) || label.contains(slotIndex)))
            || (id != null && (id.equalsIgnoreCase(slotIndex) || id.equalsIgnoreCase(slotLabel)));
    }

    private Rectangle getButtonBounds(Rectangle slotBounds, boolean buy)
    {
        int width = Math.max(1, (int) (slotBounds.width * 0.52));
        int height = Math.max(1, (int) (slotBounds.height * 0.18));
        int x = slotBounds.x + ((slotBounds.width - width) / 2);
        int y = slotBounds.y + (int) (slotBounds.height * 0.72);
        return new Rectangle(x, y, width, height);
    }

    private Widget firstNonNull(Widget first, Widget second)
    {
        return first != null ? first : second;
    }

    private Widget findWidgetByNeedles(Widget root, String... needles)
    {
        if (root == null)
        {
            return null;
        }

        if (widgetContainsNeedles(root, needles))
        {
            return root;
        }

        Widget match = findInChildren(root.getChildren(), needles);
        if (match != null)
        {
            return match;
        }

        match = findInChildren(root.getDynamicChildren(), needles);
        if (match != null)
        {
            return match;
        }

        match = findInChildren(root.getStaticChildren(), needles);
        if (match != null)
        {
            return match;
        }

        return findInChildren(root.getNestedChildren(), needles);
    }

    private Widget findInChildren(Widget[] widgets, String... needles)
    {
        if (widgets == null)
        {
            return null;
        }

        for (Widget child : widgets)
        {
            Widget match = findWidgetByNeedles(child, needles);
            if (match != null)
            {
                return match;
            }
        }

        return null;
    }

    private boolean widgetContainsNeedles(Widget widget, String... needles)
    {
        String text = Objects.toString(widget.getText(), "");
        String name = Objects.toString(widget.getName(), "");
        String actions = java.util.Arrays.toString(widget.getActions() == null ? new String[0] : widget.getActions());
        String lowerText = text.toLowerCase();
        String lowerName = name.toLowerCase();
        String lowerActions = actions.toLowerCase();
        for (String needle : needles)
        {
            String lower = needle.toLowerCase();
            if (lowerText.contains(lower) || lowerName.contains(lower) || lowerActions.contains(lower))
            {
                return true;
            }
        }

        return false;
    }

    private void logOnce(String key, String message)
    {
        if (Objects.equals(lastDebugKey, key))
        {
            return;
        }

        lastDebugKey = key;
        LOGGER.info(message);
    }

    private String describeFlip(ActiveFlipSnapshot flip)
    {
        if (flip == null)
        {
            return "null";
        }

        return "slot=" + flip.slotIndex() + ", item=" + flip.itemName() + ", phase=" + flip.effectiveStatusLabel();
    }

    private String describeWidget(Widget widget)
    {
        if (widget == null)
        {
            return "null";
        }

        Rectangle bounds = widget.getBounds();
        return "id=" + widget.getId() + ", itemId=" + widget.getItemId() + ", text=" + Objects.toString(widget.getText(), "")
            + ", actions=" + java.util.Arrays.toString(widget.getActions())
            + ", bounds=" + (bounds == null ? "null" : bounds.x + "," + bounds.y + "," + bounds.width + "x" + bounds.height);
    }
}
