package com.osrsflipperv2.runelite;

import java.io.IOException;

public class PluginAuthException extends IOException
{
    public PluginAuthException(String message)
    {
        super(message);
    }

    public PluginAuthException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
