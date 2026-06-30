package com.osrsflipperv2.runelite;

import java.time.Instant;
import java.util.UUID;

public final class HistoryFlipSnapshot
{
    private final UUID id;
    private final int itemId;
    private final String itemName;
    private final long plannedBuyPriceGp;
    private final Long actualGpSpent;
    private final Long actualGpReceived;
    private final long profitGp;
    private final int plannedQuantity;
    private final int actualBoughtQuantity;
    private final int actualSoldQuantity;
    private final Instant boughtAtUtc;
    private final Instant soldAtUtc;
    private final Instant completedAtUtc;
    private final Instant createdAtUtc;
    private final String status;

    public HistoryFlipSnapshot(
        UUID id,
        int itemId,
        String itemName,
        long plannedBuyPriceGp,
        Long actualGpSpent,
        Long actualGpReceived,
        long profitGp,
        int plannedQuantity,
        int actualBoughtQuantity,
        int actualSoldQuantity,
        Instant boughtAtUtc,
        Instant soldAtUtc,
        Instant completedAtUtc,
        Instant createdAtUtc,
        String status)
    {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.plannedBuyPriceGp = plannedBuyPriceGp;
        this.actualGpSpent = actualGpSpent;
        this.actualGpReceived = actualGpReceived;
        this.profitGp = profitGp;
        this.plannedQuantity = plannedQuantity;
        this.actualBoughtQuantity = actualBoughtQuantity;
        this.actualSoldQuantity = actualSoldQuantity;
        this.boughtAtUtc = boughtAtUtc;
        this.soldAtUtc = soldAtUtc;
        this.completedAtUtc = completedAtUtc;
        this.createdAtUtc = createdAtUtc;
        this.status = status;
    }

    public UUID id() { return id; }
    public int itemId() { return itemId; }
    public String itemName() { return itemName; }
    public long plannedBuyPriceGp() { return plannedBuyPriceGp; }
    public Long actualGpSpent() { return actualGpSpent; }
    public Long actualGpReceived() { return actualGpReceived; }
    public long profitGp() { return profitGp; }
    public int plannedQuantity() { return plannedQuantity; }
    public int actualBoughtQuantity() { return actualBoughtQuantity; }
    public int actualSoldQuantity() { return actualSoldQuantity; }
    public Instant boughtAtUtc() { return boughtAtUtc; }
    public Instant soldAtUtc() { return soldAtUtc; }
    public Instant completedAtUtc() { return completedAtUtc; }
    public Instant createdAtUtc() { return createdAtUtc; }
    public String status() { return status; }
}
