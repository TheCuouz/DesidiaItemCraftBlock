package info.desidia.util;

import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {}

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String replace(String text, String key, String value) {
        if (text == null) return "";
        return text.replace("{" + key + "}", value == null ? "" : value);
    }

    public static String replacePlaceholders(String text, Object... keyValuePairs) {
        if (text == null) return "";
        for (int i = 0; i + 1 < keyValuePairs.length; i += 2) {
            text = replace(text, String.valueOf(keyValuePairs[i]), String.valueOf(keyValuePairs[i + 1]));
        }
        return color(text);
    }
}
