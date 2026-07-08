package com.osrsflipperv2.runelite;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeInputTyper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GeInputTyper.class);
    private Robot robot;

    @Inject
    public GeInputTyper()
    {
    }

    public void pasteText(String text)
    {
        try
        {
            ensureRobot();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            robot.delay(50);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        }
        catch (Exception ex)
        {
            LOGGER.warn("Failed to paste text into GE input", ex);
        }
    }

    public void pasteTextAndEnter(String text)
    {
        pasteText(text);
        if (robot != null)
        {
            robot.delay(50);
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        }
    }

    private void ensureRobot() throws AWTException
    {
        if (robot == null)
        {
            robot = new Robot();
            robot.setAutoDelay(25);
        }
    }
}
