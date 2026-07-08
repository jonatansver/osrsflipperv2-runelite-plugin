package com.osrsflipperv2.runelite;

import java.util.Collections;
import java.util.List;

public final class DashboardSnapshot
{
    private final List<ActiveFlipSnapshot> activeFlips;
    private final List<DashboardSlotSnapshot> availableSlots;
    private final long requestDefaultBudgetGp;
    private final short requestDefaultBuyLimitWindows;
    private final long capturedAtEpochMs;

    public DashboardSnapshot(
        List<ActiveFlipSnapshot> activeFlips,
        List<DashboardSlotSnapshot> availableSlots,
        long requestDefaultBudgetGp,
        short requestDefaultBuyLimitWindows,
        long capturedAtEpochMs)
    {
        this.activeFlips = activeFlips == null ? Collections.emptyList() : Collections.unmodifiableList(activeFlips);
        this.availableSlots = availableSlots == null ? Collections.emptyList() : Collections.unmodifiableList(availableSlots);
        this.requestDefaultBudgetGp = requestDefaultBudgetGp;
        this.requestDefaultBuyLimitWindows = requestDefaultBuyLimitWindows;
        this.capturedAtEpochMs = capturedAtEpochMs;
    }

    public List<ActiveFlipSnapshot> activeFlips() { return activeFlips; }
    public List<DashboardSlotSnapshot> availableSlots() { return availableSlots; }
    public long requestDefaultBudgetGp() { return requestDefaultBudgetGp; }
    public short requestDefaultBuyLimitWindows() { return requestDefaultBuyLimitWindows; }
    public long capturedAtEpochMs() { return capturedAtEpochMs; }
}
