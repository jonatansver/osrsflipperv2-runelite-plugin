package com.osrsflipperv2.runelite;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JsonStrings
{
    private JsonStrings()
    {
    }

    public static String escape(String rawValue)
    {
        if (rawValue == null)
        {
            return "";
        }

        StringBuilder builder = new StringBuilder(rawValue.length());
        for (int i = 0; i < rawValue.length(); i++)
        {
            char c = rawValue.charAt(i);
            switch (c)
            {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\b':
                    builder.append("\\b");
                    break;
                case '\f':
                    builder.append("\\f");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20)
                    {
                        builder.append(String.format("\\u%04x", (int) c));
                    }
                    else
                    {
                        builder.append(c);
                    }
                    break;
            }
        }
        return builder.toString();
    }

    public static String extractString(String json, String fieldName)
    {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find())
        {
            throw new IllegalArgumentException("Missing JSON field: " + fieldName);
        }

        return unescape(matcher.group(1));
    }

    private static String unescape(String rawValue)
    {
        StringBuilder builder = new StringBuilder(rawValue.length());
        for (int i = 0; i < rawValue.length(); i++)
        {
            char c = rawValue.charAt(i);
            if (c != '\\')
            {
                builder.append(c);
                continue;
            }

            if (i + 1 >= rawValue.length())
            {
                throw new IllegalArgumentException("Invalid JSON escape sequence");
            }

            char next = rawValue.charAt(++i);
            switch (next)
            {
                case '"':
                case '\\':
                case '/':
                    builder.append(next);
                    break;
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    if (i + 4 >= rawValue.length())
                    {
                        throw new IllegalArgumentException("Invalid JSON unicode escape");
                    }
                    String hex = rawValue.substring(i + 1, i + 5);
                    builder.append((char) Integer.parseInt(hex, 16));
                    i += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported JSON escape: \\" + next);
            }
        }
        return builder.toString();
    }
}
