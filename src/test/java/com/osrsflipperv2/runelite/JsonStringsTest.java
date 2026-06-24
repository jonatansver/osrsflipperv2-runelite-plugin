package com.osrsflipperv2.runelite;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JsonStringsTest
{
    @Test
    void escapesAndUnescapesJsonStrings()
    {
        String original = "line1\nline2\"\\";
        String escaped = JsonStrings.escape(original);
        assertEquals(original, JsonStrings.extractString("{\"value\":\"" + escaped + "\"}", "value"));
    }
}
