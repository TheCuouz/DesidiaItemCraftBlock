package info.desidia.hooks;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final DesidiaItemCraftBlock plugin;

    public PlaceholderAPIHook(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "dib"; }
    @Override public @NotNull String getAuthor() { return "Desidia"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        StatsManager stats = plugin.getStatsManager();

        switch (params.toLowerCase()) {
            case "enabled":
                return String.valueOf(plugin.getConfigManager().isEnabled());
            case "mode":
                return plugin.getConfigManager().getGlobalMode();
            case "total_blocked":
                return String.valueOf(stats.getTotalBlocked(player.getUniqueId()));
            case "top_item":
                return stats.getTopItem(player.getUniqueId());
            case "top_item_count": {
                String top = stats.getTopItem(player.getUniqueId());
                return String.valueOf(stats.getItemCount(player.getUniqueId(), top));
            }
            case "last_world":
                return nullToEmpty(stats.getLastWorld(player.getUniqueId()));
            case "last_region":
                return nullToEmpty(stats.getLastRegion(player.getUniqueId()));
            case "time_since_last":
                return stats.getTimeSinceLastAttempt(player.getUniqueId());
            case "wg_available":
                return String.valueOf(plugin.getWorldGuardHook() != null
                        && plugin.getWorldGuardHook().isAvailable());
            case "global_total":
                return String.valueOf(stats.getGlobalTotal());
            default:
                return null;
        }
    }

    private String nullToEmpty(String s) { return s == null ? "" : s; }
}
