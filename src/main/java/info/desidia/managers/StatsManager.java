package info.desidia.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatsManager {

    private final Plugin plugin;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();

    public StatsManager(Plugin plugin) {
        this.plugin = plugin;
        if (plugin != null) load();
    }

    public void record(UUID uuid, String name, String material, String world, String region) {
        PlayerStats ps = playerStats.computeIfAbsent(uuid, k -> new PlayerStats(name));
        ps.name = name;
        ps.totalBlocked++;
        ps.itemCounts.merge(material, 1, Integer::sum);
        ps.worldCounts.merge(world, 1, Integer::sum);
        ps.lastItem = material;
        ps.lastWorld = world;
        ps.lastRegion = region == null ? "" : region;
        ps.lastAttemptMs = System.currentTimeMillis();
    }

    public int getTotalBlocked(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? 0 : ps.totalBlocked;
    }

    public String getTopItem(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        if (ps == null || ps.itemCounts.isEmpty()) return "N/A";
        return ps.itemCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("N/A");
    }

    public int getItemCount(UUID uuid, String material) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? 0 : ps.itemCounts.getOrDefault(material, 0);
    }

    public String getLastItem(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "N/A" : ps.lastItem;
    }

    public String getLastWorld(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "" : ps.lastWorld;
    }

    public String getLastRegion(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        return ps == null ? "" : ps.lastRegion;
    }

    public String getTimeSinceLastAttempt(UUID uuid) {
        PlayerStats ps = playerStats.get(uuid);
        if (ps == null || ps.lastAttemptMs == 0) return "never";
        long mins = (System.currentTimeMillis() - ps.lastAttemptMs) / 60000;
        if (mins < 1) return "just now";
        return mins + " min";
    }

    public int getGlobalTotal() {
        return playerStats.values().stream().mapToInt(ps -> ps.totalBlocked).sum();
    }

    public List<Map.Entry<String, Integer>> getGlobalTopItems(int limit) {
        Map<String, Integer> totals = new HashMap<>();
        for (PlayerStats ps : playerStats.values()) {
            ps.itemCounts.forEach((item, count) -> totals.merge(item, count, Integer::sum));
        }
        return totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Map.Entry<String, Integer>> getGlobalTopPlayers(int limit) {
        return playerStats.entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getValue().name, e.getValue().totalBlocked))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void resetPlayer(UUID uuid) { playerStats.remove(uuid); }

    public void resetAll() { playerStats.clear(); }

    public void save() {
        if (plugin == null) return;
        File file = new File(plugin.getDataFolder(), "stats.yml");
        YamlConfiguration yml = new YamlConfiguration();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Map.Entry<UUID, PlayerStats> e : playerStats.entrySet()) {
            String base = "players." + e.getKey();
            PlayerStats ps = e.getValue();
            yml.set(base + ".name", ps.name);
            yml.set(base + ".blocked-attempts", ps.totalBlocked);
            yml.set(base + ".last-item", ps.lastItem);
            yml.set(base + ".last-world", ps.lastWorld);
            if (ps.lastAttemptMs > 0) yml.set(base + ".last-attempt", sdf.format(new Date(ps.lastAttemptMs)));
            ps.itemCounts.forEach((item, count) -> yml.set(base + ".top-items." + item, count));
        }
        try { yml.save(file); } catch (IOException ignored) {}
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        if (!yml.isConfigurationSection("players")) return;
        for (String uuidStr : yml.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                String base = "players." + uuidStr;
                PlayerStats ps = new PlayerStats(yml.getString(base + ".name", "Unknown"));
                ps.totalBlocked = yml.getInt(base + ".blocked-attempts", 0);
                ps.lastItem = yml.getString(base + ".last-item", "");
                ps.lastWorld = yml.getString(base + ".last-world", "");
                if (yml.isConfigurationSection(base + ".top-items")) {
                    yml.getConfigurationSection(base + ".top-items").getKeys(false)
                            .forEach(item -> ps.itemCounts.put(item, yml.getInt(base + ".top-items." + item, 0)));
                }
                playerStats.put(uuid, ps);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private static class PlayerStats {
        String name;
        int totalBlocked = 0;
        String lastItem = "";
        String lastWorld = "";
        String lastRegion = "";
        long lastAttemptMs = 0;
        final Map<String, Integer> itemCounts = new HashMap<>();
        final Map<String, Integer> worldCounts = new HashMap<>();

        PlayerStats(String name) { this.name = name; }
    }
}
