package com.osrsflipperv2.runelite;

public final class DashboardSlotSnapshot
{
    private final String id;
    private final String label;
    private final boolean occupied;
    private final boolean locked;
    private final String itemName;

    public DashboardSlotSnapshot(String id, String label, boolean occupied, boolean locked, String itemName)
    {
        this.id = id;
        this.label = label;
        this.occupied = occupied;
        this.locked = locked;
        this.itemName = itemName;
    }

    public String id() { return id; }
    public String label() { return label; }
    public boolean occupied() { return occupied; }
    public boolean locked() { return locked; }
    public String itemName() { return itemName; }
}
