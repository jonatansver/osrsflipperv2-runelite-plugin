package com.osrsflipperv2.runelite;

import java.util.Objects;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.VarClientInt;
import net.runelite.api.VarClientStr;
import net.runelite.api.Varbits;
import net.runelite.api.VarPlayer;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeChatboxAssistant
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeChatboxAssistant.class);
    private static final int CHATBOX_SHIFT_PIXELS = 7;
    private static final int SEARCH_WIDGET_INDEX = 0;
    private static final int SEARCH_TEXT_WIDGET_INDEX = 1;
    private static final int SEARCH_NAME_WIDGET_INDEX = 2;
    private static final int SEARCH_ITEM_WIDGET_INDEX = 3;
    private static final int SUGGESTION_WIDGET_INDEX = 0;

    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final GeInputTyper geInputTyper;
    private final Supplier<DashboardSnapshot> dashboardSupplier;
    private final Supplier<ActiveFlipSnapshot> focusedFlipSupplier;

    private boolean chatboxShifted;

    @Inject
    public GeChatboxAssistant(
        Client client,
        ClientThread clientThread,
        ItemManager itemManager,
        GeInputTyper geInputTyper,
        Supplier<DashboardSnapshot> dashboardSupplier,
        Supplier<ActiveFlipSnapshot> focusedFlipSupplier)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        this.geInputTyper = geInputTyper;
        this.dashboardSupplier = dashboardSupplier;
        this.focusedFlipSupplier = focusedFlipSupplier;
    }

    public void refresh()
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            clear();
            return;
        }

        ActiveFlipSnapshot flip = resolveCandidateFlip();
        if (flip == null)
        {
            clear();
            return;
        }

        if (isSearchDialogOpen())
        {
            showSearchSuggestion(flip);
        }
        else
        {
            clearSearchSuggestion();
        }

        if (isQuantityDialogOpen())
        {
            showQuantitySuggestion(flip);
        }
        else if (isPriceDialogOpen())
        {
            showPriceSuggestion(flip);
        }
        else
        {
            clearInputSuggestion();
        }
    }

    public void clear()
    {
        clearSearchSuggestion();
        clearInputSuggestion();
    }

    private ActiveFlipSnapshot resolveCandidateFlip()
    {
        DashboardSnapshot dashboard = dashboardSupplier.get();
        if (dashboard == null)
        {
            return focusedFlipSupplier.get();
        }

        ActiveFlipSnapshot focused = focusedFlipSupplier.get();
        LOGGER.info(String.format(
            "GE assistant refresh openSearch=%s openQty=%s openPrice=%s currentGeItem=%d focused=%s",
            isSearchDialogOpen(),
            isQuantityDialogOpen(),
            isPriceDialogOpen(),
            client.getVarpValue(VarPlayer.CURRENT_GE_ITEM),
            focused == null ? "null" : focused.slotIndex() + ":" + focused.itemName() + ":" + focused.effectiveStatusLabel()));
        LOGGER.info("GE assistant active flips: " + dashboard.activeFlips().stream()
            .map(flip -> flip.slotIndex() + ":" + flip.itemName() + ":" + flip.effectiveStatusLabel())
            .reduce((a, b) -> a + " | " + b)
            .orElse("none"));

        if (matchesCurrentGeItem(focused))
        {
            return focused;
        }

        if (isSearchDialogOpen())
        {
            ActiveFlipSnapshot candidate = isSearchCandidate(focused)
                ? focused
                : dashboard.activeFlips().stream()
                    .filter(this::isSearchCandidate)
                    .findFirst()
                    .orElse(null);
            LOGGER.info("GE assistant search candidate=" + (candidate == null ? "null" : candidate.slotIndex() + ":" + candidate.itemName()));
            return candidate;
        }

        if (isQuantityDialogOpen() || isPriceDialogOpen())
        {
            ActiveFlipSnapshot candidate = isInputCandidate(focused)
                ? focused
                : dashboard.activeFlips().stream()
                    .filter(this::isInputCandidate)
                    .findFirst()
                    .orElse(null);
            LOGGER.info("GE assistant input candidate=" + (candidate == null ? "null" : candidate.slotIndex() + ":" + candidate.itemName()));
            return candidate;
        }

        return focused;
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

    private boolean isSearchCandidate(ActiveFlipSnapshot flip)
    {
        return flip != null && flip.isBuyPhase() && isEmptySlot(flip);
    }

    private boolean isInputCandidate(ActiveFlipSnapshot flip)
    {
        if (flip == null)
        {
            return false;
        }

        if (isSellFlow())
        {
            return flip.isSellPhase() && !isEmptySlot(flip);
        }

        return flip.isBuyPhase() && isEmptySlot(flip);
    }

    private boolean matchesCurrentGeItem(ActiveFlipSnapshot flip)
    {
        if (flip == null)
        {
            return false;
        }

        int currentItemId = client.getVarpValue(VarPlayer.CURRENT_GE_ITEM);
        if (currentItemId <= 0)
        {
            return false;
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

    private boolean isSearchDialogOpen()
    {
        return client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS) != null;
    }

    private boolean isQuantityDialogOpen()
    {
        Widget title = client.getWidget(WidgetInfo.CHATBOX_TITLE);
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
        Widget title = client.getWidget(WidgetInfo.CHATBOX_TITLE);
        if (title == null)
        {
            return false;
        }

        String text = title.getText();
        return Objects.equals(text, "Set a price for each item:");
    }

    private boolean isSellFlow()
    {
        return client.getVarbitValue(Varbits.GE_OFFER_CREATION_TYPE) == 1;
    }

    private void showSearchSuggestion(ActiveFlipSnapshot flip)
    {
        Widget parent = client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS);
        if (parent == null)
        {
            return;
        }

        Widget widget = ensureChild(parent, SEARCH_WIDGET_INDEX, WidgetType.RECTANGLE);
        widget.setTextColor(0xFFFFFF);
        widget.setOpacity(255);
        widget.setFilled(true);
        widget.setOriginalX(114);
        widget.setOriginalY(0);
        widget.setOriginalWidth(256);
        widget.setOriginalHeight(32);
        widget.setHasListener(true);
        widget.setName("<col=ff9040>" + flip.itemName() + "</col>");
        widget.setAction(0, "Select");
        widget.setOnOpListener((JavaScriptCallback) ev -> geInputTyper.pasteTextAndEnter(flip.itemName()));
        widget.setOnKeyListener(754, resolveItemId(flip.itemName()), -2147483640);
        widget.setOnMouseOverListener((JavaScriptCallback) ev -> widget.setOpacity(200));
        widget.setOnMouseLeaveListener((JavaScriptCallback) ev -> widget.setOpacity(255));
        widget.revalidate();

        Widget label = ensureChild(parent, SEARCH_TEXT_WIDGET_INDEX, WidgetType.TEXT);
        label.setText("Copilot item:");
        label.setFontId(495);
        label.setOriginalX(114);
        label.setOriginalY(0);
        label.setOriginalWidth(95);
        label.setOriginalHeight(32);
        label.setYTextAlignment(1);
        label.revalidate();

        Widget name = ensureChild(parent, SEARCH_NAME_WIDGET_INDEX, WidgetType.TEXT);
        name.setText(flip.itemName());
        name.setFontId(495);
        name.setOriginalX(254);
        name.setOriginalY(0);
        name.setOriginalWidth(116);
        name.setOriginalHeight(32);
        name.setYTextAlignment(1);
        name.revalidate();

        Widget item = ensureChild(parent, SEARCH_ITEM_WIDGET_INDEX, WidgetType.GRAPHIC);
        item.setItemId(resolveItemId(flip.itemName()));
        item.setItemQuantity(1);
        item.setItemQuantityMode(0);
        item.setRotationX(550);
        item.setModelZoom(1031);
        item.setBorderType(1);
        item.setOriginalX(214);
        item.setOriginalY(0);
        item.setOriginalWidth(36);
        item.setOriginalHeight(32);
        item.revalidate();
    }

    private void clearSearchSuggestion()
    {
        Widget parent = client.getWidget(WidgetInfo.CHATBOX_GE_SEARCH_RESULTS);
        if (parent == null)
        {
            return;
        }

        Widget widget = parent.getChild(SEARCH_WIDGET_INDEX);
        if (widget != null)
        {
            widget.setHasListener(false);
            widget.setName("");
            widget.setText("");
            widget.setAction(0, "");
            widget.setItemId(-1);
            widget.setOpacity(255);
            widget.revalidate();
        }
    }

    private void showQuantitySuggestion(ActiveFlipSnapshot flip)
    {
        adjustChatboxShift(true);
        Widget parent = client.getWidget(WidgetInfo.CHATBOX_CONTAINER);
        if (parent == null)
        {
            return;
        }

        Widget widget = ensureChild(parent, SUGGESTION_WIDGET_INDEX, WidgetType.TEXT);
        widget.setTextColor(0x40FFFF);
        widget.setFontId(495);
        widget.setOriginalX(10);
        widget.setOriginalY(40);
        widget.setOriginalWidth(230);
        widget.setOriginalHeight(20);
        widget.setXTextAlignment(WidgetTextAlignment.LEFT);
        widget.setYTextAlignment(1);
        widget.setText("Copilot quantity: " + quantityFor(flip));
        widget.setAction(0, "Set quantity");
        widget.setHasListener(true);
        widget.setOnOpListener((JavaScriptCallback) ev -> setChatboxValue(quantityFor(flip)));
        widget.revalidate();
    }

    private void showPriceSuggestion(ActiveFlipSnapshot flip)
    {
        adjustChatboxShift(true);
        Widget parent = client.getWidget(WidgetInfo.CHATBOX_CONTAINER);
        if (parent == null)
        {
            return;
        }

        long price = priceFor(flip);
        Widget widget = ensureChild(parent, SUGGESTION_WIDGET_INDEX, WidgetType.TEXT);
        widget.setTextColor(0x40A0FF);
        widget.setFontId(495);
        widget.setOriginalX(10);
        widget.setOriginalY(40);
        widget.setOriginalWidth(230);
        widget.setOriginalHeight(20);
        widget.setXTextAlignment(WidgetTextAlignment.LEFT);
        widget.setYTextAlignment(1);
        widget.setText("Copilot price: " + String.format("%,d gp", price));
        widget.setAction(0, "Set price");
        widget.setHasListener(true);
        widget.setOnOpListener((JavaScriptCallback) ev -> setChatboxValue(price));
        widget.revalidate();
    }

    private void clearInputSuggestion()
    {
        adjustChatboxShift(false);
        Widget parent = client.getWidget(WidgetInfo.CHATBOX_CONTAINER);
        if (parent == null)
        {
            return;
        }

        Widget widget = parent.getChild(SUGGESTION_WIDGET_INDEX);
        if (widget != null)
        {
            widget.setText("");
            widget.setAction(0, "");
            widget.setHasListener(false);
            widget.revalidate();
        }
    }

    private void adjustChatboxShift(boolean shouldShift)
    {
        Widget title = client.getWidget(WidgetInfo.CHATBOX_TITLE);
        if (title == null)
        {
            return;
        }

        if (shouldShift && !chatboxShifted)
        {
            title.setOriginalY(title.getOriginalY() + CHATBOX_SHIFT_PIXELS);
            title.revalidate();
            chatboxShifted = true;
        }
        else if (!shouldShift && chatboxShifted)
        {
            title.setOriginalY(title.getOriginalY() - CHATBOX_SHIFT_PIXELS);
            title.revalidate();
            chatboxShifted = false;
        }
    }

    private long priceFor(ActiveFlipSnapshot flip)
    {
        if (isSellFlow())
        {
            return flip.reevaluatedSellPriceGp() != null ? flip.reevaluatedSellPriceGp() : flip.plannedSellPriceGp();
        }

        return flip.plannedBuyPriceGp();
    }

    private int quantityFor(ActiveFlipSnapshot flip)
    {
        return isSellFlow() ? Math.max(flip.actualBoughtQuantity(), flip.plannedQuantity()) : flip.plannedQuantity();
    }

    private void setChatboxValue(long value)
    {
        Widget inputWidget = client.getWidget(WidgetInfo.CHATBOX_FULL_INPUT);
        if (inputWidget != null)
        {
            inputWidget.setText(value + "*");
            inputWidget.revalidate();
        }

        client.setVarcStrValue(VarClientStr.INPUT_TEXT, String.valueOf(value));
        client.setVarcStrValue(VarClientStr.CHATBOX_TYPED_TEXT, String.valueOf(value));
    }

    private int resolveItemId(String itemName)
    {
        String target = Text.standardize(itemName);
        List<net.runelite.http.api.item.ItemPrice> matches = itemManager.search(itemName);
        int fallback = -1;
        for (net.runelite.http.api.item.ItemPrice match : matches)
        {
            if (fallback == -1)
            {
                fallback = match.getId();
            }

            String candidate = Text.standardize(match.getName());
            if (candidate.equals(target))
            {
                return match.getId();
            }
        }
        return fallback;
    }

    private Widget ensureChild(Widget parent, int index, int type)
    {
        Widget child = parent.getChild(index);
        if (child != null)
        {
            return child;
        }

        return parent.createChild(index, type);
    }
}
