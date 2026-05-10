package info.desidia.api;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.BlockDecision;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Public API for other plugins to query DIB blocking decisions.
 * Usage: DIBApi.get().isBlocked(player, Material.DIAMOND_SWORD)
 */
public class DIBApi {

    private static DIBApi instance;
    private final DesidiaItemCraftBlock plugin;

    private DIBApi(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
    }

    public static void init(DesidiaItemCraftBlock plugin) {
        instance = new DIBApi(plugin);
    }

    public static DIBApi get() {
        if (instance == null) throw new IllegalStateException("DIBApi not initialized");
        return instance;
    }

    public boolean isBlocked(Player player, Material material) {
        String worldName = player.getWorld().getName();
        String regionName = plugin.getWorldGuardHook() != null
                ? plugin.getWorldGuardHook().getRegionName(player) : null;
        BlockDecision decision = plugin.getBlockManager().evaluate(player, material, worldName, regionName);
        return decision.isBlocked();
    }

    public boolean isEnabled() {
        return plugin.getConfigManager().isEnabled();
    }

    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
}
