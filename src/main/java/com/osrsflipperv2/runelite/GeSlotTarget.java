package com.osrsflipperv2.runelite;

import java.awt.Rectangle;

public final class GeSlotTarget
{
    private final int slotIndex;
    private final Rectangle bounds;

    public GeSlotTarget(int slotIndex, Rectangle bounds)
    {
        this.slotIndex = slotIndex;
        this.bounds = bounds;
    }

    public int slotIndex()
    {
        return slotIndex;
    }

    public Rectangle bounds()
    {
        return bounds;
    }
}
