package com.osrsflipperv2.runelite;

import java.time.Instant;
import java.util.UUID;

public final class ActiveFlipSnapshot
{
    private final UUID id;
    private final short slotIndex;
    private final String slotLabel;
    private final String itemName;
    private final String imageUrl;
    private final String status;
    private final int plannedQuantity;
    private final int actualBoughtQuantity;
    private final int actualSoldQuantity;
    private final long plannedBuyPriceGp;
    private final long plannedSellPriceGp;
    private final Long reevaluatedSellPriceGp;
    private final Long actualBuySpendGp;
    private final Long actualSellReceiveGp;
    private final short buyLimitWindows;
    private final Long originalThresholdMaxMarketBuyGp;
    private final Long originalThresholdMinMarketSellGp;
    private final Instant boughtAtUtc;
    private final Instant soldAtUtc;
    private final Instant cancelledAtUtc;
    private final Instant updatedAtUtc;
    private final long projectedProfitGp;

    public ActiveFlipSnapshot(
        UUID id,
        short slotIndex,
        String slotLabel,
        String itemName,
        String imageUrl,
        String status,
        int plannedQuantity,
        int actualBoughtQuantity,
        int actualSoldQuantity,
        long plannedBuyPriceGp,
        long plannedSellPriceGp,
        Long reevaluatedSellPriceGp,
        Long actualBuySpendGp,
        Long actualSellReceiveGp,
        short buyLimitWindows,
        Long originalThresholdMaxMarketBuyGp,
        Long originalThresholdMinMarketSellGp,
        Instant boughtAtUtc,
        Instant soldAtUtc,
        Instant cancelledAtUtc,
        Instant updatedAtUtc,
        long projectedProfitGp)
    {
        this.id = id;
        this.slotIndex = slotIndex;
        this.slotLabel = slotLabel;
        this.itemName = itemName;
        this.imageUrl = imageUrl;
        this.status = status;
        this.plannedQuantity = plannedQuantity;
        this.actualBoughtQuantity = actualBoughtQuantity;
        this.actualSoldQuantity = actualSoldQuantity;
        this.plannedBuyPriceGp = plannedBuyPriceGp;
        this.plannedSellPriceGp = plannedSellPriceGp;
        this.reevaluatedSellPriceGp = reevaluatedSellPriceGp;
        this.actualBuySpendGp = actualBuySpendGp;
        this.actualSellReceiveGp = actualSellReceiveGp;
        this.buyLimitWindows = buyLimitWindows;
        this.originalThresholdMaxMarketBuyGp = originalThresholdMaxMarketBuyGp;
        this.originalThresholdMinMarketSellGp = originalThresholdMinMarketSellGp;
        this.boughtAtUtc = boughtAtUtc;
        this.soldAtUtc = soldAtUtc;
        this.cancelledAtUtc = cancelledAtUtc;
        this.updatedAtUtc = updatedAtUtc;
        this.projectedProfitGp = projectedProfitGp;
    }

    public UUID id() { return id; }
    public short slotIndex() { return slotIndex; }
    public String slotLabel() { return slotLabel; }
    public String itemName() { return itemName; }
    public String imageUrl() { return imageUrl; }
    public String status() { return status; }
    public int plannedQuantity() { return plannedQuantity; }
    public int actualBoughtQuantity() { return actualBoughtQuantity; }
    public int actualSoldQuantity() { return actualSoldQuantity; }
    public long plannedBuyPriceGp() { return plannedBuyPriceGp; }
    public long plannedSellPriceGp() { return plannedSellPriceGp; }
    public Long reevaluatedSellPriceGp() { return reevaluatedSellPriceGp; }
    public Long actualBuySpendGp() { return actualBuySpendGp; }
    public Long actualSellReceiveGp() { return actualSellReceiveGp; }
    public short buyLimitWindows() { return buyLimitWindows; }
    public Long originalThresholdMaxMarketBuyGp() { return originalThresholdMaxMarketBuyGp; }
    public Long originalThresholdMinMarketSellGp() { return originalThresholdMinMarketSellGp; }
    public Instant boughtAtUtc() { return boughtAtUtc; }
    public Instant soldAtUtc() { return soldAtUtc; }
    public Instant cancelledAtUtc() { return cancelledAtUtc; }
    public Instant updatedAtUtc() { return updatedAtUtc; }
    public long projectedProfitGp() { return projectedProfitGp; }

    public String effectiveStatusLabel()
    {
        switch (status)
        {
            case "planned_buy":
            case "partial_buy":
            case "buying":
                return "buying";
            case "bought_waiting_sell":
            case "bought":
                return "bought";
            case "partial_sell":
            case "selling":
                return "selling";
            case "completed":
            case "sold":
                return "sold";
            case "cancelled":
            case "abort_buy":
                return "abort buy";
            case "abort_sell":
                return "abort sell";
            default:
                return status;
        }
    }

    public boolean isBuyPhase()
    {
        return "planned_buy".equals(status) || "partial_buy".equals(status) || "buying".equals(status);
    }

    public boolean isSellPhase()
    {
        return "bought_waiting_sell".equals(status) || "partial_sell".equals(status) || "selling".equals(status) || "sold".equals(status);
    }
}
