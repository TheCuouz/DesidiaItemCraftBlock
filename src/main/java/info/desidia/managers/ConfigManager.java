package info.desidia.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.regex.Pattern;

public class ConfigManager {

    private final Plugin plugin;

    private boolean enabled;
    private String globalMode;
    private String locale;
    private boolean notifyAdmins;
    private boolean logAttempts;
    private boolean updateChecker;
    private int statsAutosaveInterval;

    private boolean messageEnabled;
    private String messageText;
    private String messageType;
    private String messageSound;
    private int messageCooldown;

    private final Map<String, WorldConfig> worldConfigs = new LinkedHashMap<>();
    private final Map<String, RegionConfig> regionConfigs = new LinkedHashMap<>();

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        enabled = c.getBoolean("settings.enabled", true);
        globalMode = c.getString("settings.mode", "blacklist");
        locale = c.getString("settings.locale", "en");
        notifyAdmins = c.getBoolean("settings.notify-admins", false);
        logAttempts = c.getBoolean("settings.log-attempts", false);
        updateChecker = c.getBoolean("settings.update-checker", true);
        statsAutosaveInterval = c.getInt("settings.stats-autosave-interval", 5);

        messageEnabled = c.getBoolean("message.enabled", true);
        messageText = c.getString("message.text", "&cYou can't craft {item} here!");
        messageType = c.getString("message.type", "CHAT");
        messageSound = c.getString("message.sound", "ENTITY_VILLAGER_NO");
        messageCooldown = c.getInt("message.cooldown", 3);

        worldConfigs.clear();
        ConfigurationSection worlds = c.getConfigurationSection("worlds");
        if (worlds != null) {
            for (String key : worlds.getKeys(false)) {
                worldConfigs.put(key, WorldConfig.load(worlds.getConfigurationSection(key)));
            }
        }

        regionConfigs.clear();
        ConfigurationSection regions = c.getConfigurationSection("regions");
        if (regions != null) {
            for (String key : regions.getKeys(false)) {
                regionConfigs.put(key, RegionConfig.load(regions.getConfigurationSection(key)));
            }
        }
    }

    public void setEnabled(boolean val) {
        this.enabled = val;
        plugin.getConfig().set("settings.enabled", val);
        plugin.saveConfig();
    }

    public WorldConfig getWorldConfig(String world) {
        if (worldConfigs.containsKey(world)) return worldConfigs.get(world);
        return worldConfigs.get("default");
    }

    public RegionConfig getRegionConfig(String region) {
        return regionConfigs.get(region);
    }

    public void addBlockedItem(String world, String material) {
        WorldConfig wc = worldConfigs.computeIfAbsent(world, k -> new WorldConfig());
        wc.blocked.add(material.toUpperCase());
        plugin.getConfig().set("worlds." + world + ".blocked", new ArrayList<>(wc.blocked));
        plugin.saveConfig();
    }

    public void removeBlockedItem(String world, String material) {
        WorldConfig wc = worldConfigs.get(world);
        if (wc == null) return;
        wc.blocked.remove(material.toUpperCase());
        wc.allowed.remove(material.toUpperCase());
        plugin.getConfig().set("worlds." + world + ".blocked", new ArrayList<>(wc.blocked));
        plugin.getConfig().set("worlds." + world + ".allowed", new ArrayList<>(wc.allowed));
        plugin.saveConfig();
    }

    public boolean isEnabled() { return enabled; }
    public String getGlobalMode() { return globalMode; }
    public String getLocale() { return locale; }
    public boolean isNotifyAdmins() { return notifyAdmins; }
    public boolean isLogAttempts() { return logAttempts; }
    public boolean isUpdateChecker() { return updateChecker; }
    public int getStatsAutosaveInterval() { return statsAutosaveInterval; }
    public boolean isMessageEnabled() { return messageEnabled; }
    public String getMessageText() { return messageText; }
    public String getMessageType() { return messageType; }
    public String getMessageSound() { return messageSound; }
    public int getMessageCooldown() { return messageCooldown; }
    public Map<String, WorldConfig> getWorldConfigs() { return Collections.unmodifiableMap(worldConfigs); }

    public static boolean matchesPattern(String pattern, String material) {
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return pattern.equalsIgnoreCase(material);
        }
        String regex = "^" + Pattern.quote(pattern.toUpperCase()).replace("\\*", ".*").replace("\\?", ".") + "$";
        return material.toUpperCase().matches(regex);
    }

    public static boolean isInList(Set<String> patterns, String material) {
        for (String p : patterns) {
            if (matchesPattern(p, material)) return true;
        }
        return false;
    }

    // ---- Inner classes ----

    public static class WorldConfig {
        public boolean enabled = true;
        public String mode = "blacklist";
        public final Set<String> blocked = new LinkedHashSet<>();
        public final Set<String> allowed = new LinkedHashSet<>();
        public final Map<String, String> customMessages = new HashMap<>();

        public static WorldConfig load(ConfigurationSection s) {
            WorldConfig wc = new WorldConfig();
            if (s == null) return wc;
            wc.enabled = s.getBoolean("enabled", true);
            wc.mode = s.getString("mode", "blacklist");
            if (s.isList("blocked")) upper(s.getStringList("blocked")).forEach(wc.blocked::add);
            if (s.isList("allowed")) upper(s.getStringList("allowed")).forEach(wc.allowed::add);
            ConfigurationSection cm = s.getConfigurationSection("custom-messages");
            if (cm != null) cm.getKeys(false).forEach(k -> wc.customMessages.put(k.toUpperCase(), cm.getString(k)));
            return wc;
        }
    }

    public static class RegionConfig {
        public String mode = "blacklist";
        public final Set<String> blocked = new LinkedHashSet<>();
        public final Set<String> allowed = new LinkedHashSet<>();
        public final Map<String, String> customMessages = new HashMap<>();

        public static RegionConfig load(ConfigurationSection s) {
            RegionConfig rc = new RegionConfig();
            if (s == null) return rc;
            rc.mode = s.getString("mode", "blacklist");
            if (s.isList("blocked")) upper(s.getStringList("blocked")).forEach(rc.blocked::add);
            if (s.isList("allowed")) upper(s.getStringList("allowed")).forEach(rc.allowed::add);
            ConfigurationSection cm = s.getConfigurationSection("custom-messages");
            if (cm != null) cm.getKeys(false).forEach(k -> rc.customMessages.put(k.toUpperCase(), cm.getString(k)));
            return rc;
        }
    }

    private static List<String> upper(List<String> list) {
        List<String> r = new ArrayList<>();
        for (String s : list) r.add(s.toUpperCase());
        return r;
    }
}
