package com.osrsflipperv2.runelite;

import java.util.Locale;

public enum BackendEnvironment
{
    LOCAL,
    STAGING,
    PRODUCTION,
    CUSTOM;

    public static BackendEnvironment fromConfig(String rawValue)
    {
        if (rawValue == null || rawValue.isBlank())
        {
            return LOCAL;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        if ("local".equals(normalized) || "development".equals(normalized) || "dev".equals(normalized))
        {
            return LOCAL;
        }

        if ("staging".equals(normalized) || "stage".equals(normalized))
        {
            return STAGING;
        }

        if ("production".equals(normalized) || "prod".equals(normalized))
        {
            return PRODUCTION;
        }

        return CUSTOM;
    }
}
