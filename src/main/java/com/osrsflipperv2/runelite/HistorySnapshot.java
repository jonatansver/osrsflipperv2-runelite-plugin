package com.osrsflipperv2.runelite;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class HistorySnapshot
{
    private final List<HistoryFlipSnapshot> flips;
    private final long totalProfitGp;
    private final int totalFlips;
    private final int cancelledFlips;

    public HistorySnapshot(List<HistoryFlipSnapshot> flips, long totalProfitGp, int totalFlips, int cancelledFlips)
    {
        this.flips = flips == null ? Collections.emptyList() : Collections.unmodifiableList(flips);
        this.totalProfitGp = totalProfitGp;
        this.totalFlips = totalFlips;
        this.cancelledFlips = cancelledFlips;
    }

    public List<HistoryFlipSnapshot> flips() { return flips; }
    public long totalProfitGp() { return totalProfitGp; }
    public int totalFlips() { return totalFlips; }
    public int cancelledFlips() { return cancelledFlips; }
}
