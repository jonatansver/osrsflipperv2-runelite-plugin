package com.osrsflipperv2.runelite;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(OsrsFlipperV2Config.GROUP)
public interface OsrsFlipperV2Config extends Config
{
    String GROUP = "osrsflipperv2";

    @ConfigItem(
        keyName = "backendEnvironment",
        name = "Backend environment",
        description = "Selects the fixed backend hostname the plugin should use.",
        position = 1
    )
    default BackendEnvironment backendEnvironment()
    {
        return BackendEnvironment.STAGING;
    }

    @ConfigItem(
        keyName = "deviceName",
        name = "Device name",
        description = "Friendly name sent during pairing.",
        position = 2
    )
    default String deviceName()
    {
        return "RuneLite";
    }

    @ConfigItem(
        keyName = "deviceId",
        name = "",
        description = "",
        hidden = true
    )
    default String deviceId()
    {
        return "";
    }

    @ConfigItem(
        keyName = "deviceToken",
        name = "",
        description = "",
        hidden = true
    )
    default String deviceToken()
    {
        return "";
    }
}
