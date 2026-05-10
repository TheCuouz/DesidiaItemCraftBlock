package info.desidia.managers;

import info.desidia.api.events.BlockReason;
import info.desidia.hooks.WorldGuardHook;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;

public class BlockManager {

    private final ConfigManager config;
    private WorldGuardHook worldGuard;

    public BlockManager(ConfigManager config, WorldGuardHook worldGuard) {
        this.config = config;
        this.worldGuard = worldGuard;
    }

    public void setWorldGuardHook(WorldGuardHook wg) {
        this.worldGuard = wg;
    }

    public BlockDecision evaluate(Player player, Material material, String worldName, String regionName) {
        String mat = material.name();

        // 1. Full bypass
        if (player.hasPermission("dib.bypass")) return BlockDecision.allow();

        // 2. Item-specific bypass
        if (player.hasPermission("dib.bypass.item." + mat.toLowerCase())) return BlockDecision.allow();

        // 3. Global toggle
        if (!config.isEnabled()) return BlockDecision.allow();

        // 4. WorldGuard region flags
        if (worldGuard != null && worldGuard.isAvailable() && regionName != null) {
            BlockDecision wgDecision = worldGuard.evaluate(player, material, regionName);
            if (wgDecision != null) return wgDecision;
        }

        // 5. Config regions (fallback when WG not available)
        if ((worldGuard == null || !worldGuard.isAvailable()) && regionName != null) {
            ConfigManager.RegionConfig rc = config.getRegionConfig(regionName);
            if (rc != null) {
                if (player.hasPermission("dib.bypass.region." + regionName.toLowerCase()))
                    return BlockDecision.allow();
                String customMsg = rc.customMessages.get(mat);
                return evaluateList(rc.mode, rc.blocked, rc.allowed, mat, BlockReason.REGION, regionName, customMsg);
            }
        }

        // 6. World config
        if (player.hasPermission("dib.bypass.world." + worldName.toLowerCase())) return BlockDecision.allow();
        ConfigManager.WorldConfig wc = config.getWorldConfig(worldName);
        if (wc != null && wc.enabled) {
            String customMsg = wc.customMessages.get(mat);
            return evaluateList(wc.mode, wc.blocked, wc.allowed, mat, BlockReason.WORLD, worldName, customMsg);
        }

        // 7. Global default
        return evaluateList(config.getGlobalMode(), new LinkedHashSet<>(), new LinkedHashSet<>(),
                mat, BlockReason.GLOBAL, "global", null);
    }

    private BlockDecision evaluateList(String mode, java.util.Set<String> blocked,
                                       java.util.Set<String> allowed, String material,
                                       BlockReason reason, String context, String customMsg) {
        if ("blacklist".equalsIgnoreCase(mode)) {
            if (ConfigManager.isInList(blocked, material))
                return BlockDecision.block(reason, context, customMsg);
            return BlockDecision.allow();
        } else {
            if (ConfigManager.isInList(allowed, material)) return BlockDecision.allow();
            return BlockDecision.block(reason, context, customMsg);
        }
    }
}
