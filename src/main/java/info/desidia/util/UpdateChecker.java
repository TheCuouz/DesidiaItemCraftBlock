package info.desidia.util;

import info.desidia.DesidiaItemCraftBlock;
import org.bukkit.Bukkit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker {

    // SpigotMC resource ID — update this when the plugin is published
    private static final int SPIGOT_RESOURCE_ID = 0;
    private static final String API_URL =
            "https://api.spigotmc.org/legacy/update.php?resource=" + SPIGOT_RESOURCE_ID;

    private final DesidiaItemCraftBlock plugin;

    public UpdateChecker(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
    }

    public void checkAsync() {
        if (SPIGOT_RESOURCE_ID == 0) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                String latestVersion;
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()))) {
                    latestVersion = reader.readLine();
                }
                String current = plugin.getDescription().getVersion();
                if (latestVersion != null && !latestVersion.equals(current)) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.getLogger().warning(
                                    "[DIB] Nueva version disponible: " + latestVersion
                                            + " (actual: " + current + ")")
                    );
                }
            } catch (Exception ignored) {
                // No network or resource not published yet — silently skip
            }
        });
    }
}
