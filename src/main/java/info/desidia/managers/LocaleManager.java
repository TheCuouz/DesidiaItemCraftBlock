package info.desidia.managers;

import info.desidia.util.ColorUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocaleManager {

    private final Plugin plugin;
    private final Map<String, String> messages = new HashMap<>();

    public LocaleManager(Plugin plugin, String locale) {
        this.plugin = plugin;
        load(locale);
    }

    private void load(String locale) {
        messages.clear();
        loadFromJar("messages_en.yml");
        if (!locale.equals("en")) {
            loadFromJar("messages_" + locale + ".yml");
        }
        if (plugin != null) {
            File file = new File(plugin.getDataFolder(), "messages_" + locale + ".yml");
            if (file.exists()) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
                for (String key : yml.getKeys(false)) {
                    messages.put(key, yml.getString(key, ""));
                }
            }
        }
    }

    private void loadFromJar(String resourceName) {
        if (plugin == null) return;
        InputStream stream = plugin.getResource(resourceName);
        if (stream == null) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        for (String key : yml.getKeys(false)) {
            messages.put(key, yml.getString(key, ""));
        }
    }

    public String get(String key) {
        return messages.getOrDefault(key, "[" + key + "]");
    }

    public String getOrDefault(String key, String def) {
        return messages.getOrDefault(key, def);
    }

    public String format(String key, Object... kvPairs) {
        return ColorUtil.replacePlaceholders(get(key), kvPairs);
    }

    public void reload(String locale) {
        load(locale);
    }
}
