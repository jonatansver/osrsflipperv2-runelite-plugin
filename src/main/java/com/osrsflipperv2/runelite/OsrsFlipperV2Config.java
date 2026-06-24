package com.osrsflipperv2.runelite;

import java.util.Locale;
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
        description = "Selects the backend profile the plugin should use."
    )
    default String backendEnvironment()
    {
        return BackendEnvironment.LOCAL.name().toLowerCase(Locale.ROOT);
    }

    @ConfigItem(
        keyName = "backendBaseUrl",
        name = "Backend base URL",
        description = "Root URL of the OsrsFlipperV2 backend. Leave blank for the local default."
    )
    default String backendBaseUrl()
    {
        return "";
    }

    @ConfigItem(
        keyName = "deviceName",
        name = "Device name",
        description = "Friendly name sent during pairing."
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
