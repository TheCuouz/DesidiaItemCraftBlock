package info.desidia.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WorldGuardHook {

    public static StateFlag DENY_CRAFT_FLAG;
    public static StringFlag DENY_CRAFT_ITEMS_FLAG;
    public static StringFlag ALLOW_CRAFT_ITEMS_FLAG;

    private static final long CACHE_TTL_MS = 2000L;

    private static class CachedResult {
        final String regionName;
        final long timestamp;
        CachedResult(String regionName) {
            this.regionName = regionName;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() { return System.currentTimeMillis() - timestamp > CACHE_TTL_MS; }
    }

    private final Map<String, CachedResult> regionCache = new ConcurrentHashMap<>();
    private final boolean available;

    public WorldGuardHook(boolean available) {
        this.available = available;
    }

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        DENY_CRAFT_FLAG = registerState(registry, "deny-craft");
        DENY_CRAFT_ITEMS_FLAG = registerString(registry, "deny-craft-items");
        ALLOW_CRAFT_ITEMS_FLAG = registerString(registry, "allow-craft-items");
    }

    private static StateFlag registerState(FlagRegistry registry, String name) {
        try {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) return (StateFlag) existing;
            StateFlag flag = new StateFlag(name, false);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag) return (StateFlag) existing;
            return new StateFlag(name, false);
        }
    }

    private static StringFlag registerString(FlagRegistry registry, String name) {
        try {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StringFlag) return (StringFlag) existing;
            StringFlag flag = new StringFlag(name);
            registry.register(flag);
            return flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get(name);
            if (existing instanceof StringFlag) return (StringFlag) existing;
            return new StringFlag(name);
        }
    }

    public boolean isAvailable() { return available; }

    public String getRegionName(Player player) {
        if (!available) return null;
        String cacheKey = player.getUniqueId() + ":" + player.getLocation().getBlockX()
                + ":" + player.getLocation().getBlockZ();
        CachedResult cached = regionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.regionName;

        String regionName = resolveRegionName(player.getLocation());
        regionCache.put(cacheKey, new CachedResult(regionName));
        return regionName;
    }

    private String resolveRegionName(Location location) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
            if (manager == null) return null;
            Set<ProtectedRegion> regions = manager.getApplicableRegions(
                    BukkitAdapter.asBlockVector(location)).getRegions();
            if (regions.isEmpty()) return null;
            return regions.stream()
                    .max((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
                    .map(ProtectedRegion::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean evaluateDenyCraftFlag(Player player) {
        if (!available || DENY_CRAFT_FLAG == null) return null;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldguard.LocalPlayer wgPlayer =
                    WorldGuardPlugin.inst().wrapPlayer(player);
            StateFlag.State state = query.queryState(
                    BukkitAdapter.adapt(player.getLocation()), wgPlayer, DENY_CRAFT_FLAG);
            if (state == null) return null;
            return state == StateFlag.State.DENY;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDeniedByItemFlag(Player player, String materialName) {
        if (!available || DENY_CRAFT_ITEMS_FLAG == null) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldguard.LocalPlayer wgPlayer =
                    WorldGuardPlugin.inst().wrapPlayer(player);
            String value = query.queryValue(
                    BukkitAdapter.adapt(player.getLocation()), wgPlayer, DENY_CRAFT_ITEMS_FLAG);
            if (value == null || value.isEmpty()) return false;
            return matchesItemList(value, materialName);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAllowedByItemFlag(Player player, String materialName) {
        if (!available || ALLOW_CRAFT_ITEMS_FLAG == null) return false;
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            com.sk89q.worldguard.LocalPlayer wgPlayer =
                    WorldGuardPlugin.inst().wrapPlayer(player);
            String value = query.queryValue(
                    BukkitAdapter.adapt(player.getLocation()), wgPlayer, ALLOW_CRAFT_ITEMS_FLAG);
            if (value == null || value.isEmpty()) return false;
            return matchesItemList(value, materialName);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchesItemList(String flagValue, String materialName) {
        String upper = materialName.toUpperCase();
        for (String entry : flagValue.split(",")) {
            String pattern = entry.trim().toUpperCase();
            if (pattern.isEmpty()) continue;
            if (pattern.contains("*")) {
                String regex = pattern.replace("*", ".*");
                if (upper.matches(regex)) return true;
            } else if (pattern.equals(upper)) {
                return true;
            }
        }
        return false;
    }

    public void invalidateCache(Player player) {
        String prefix = player.getUniqueId().toString();
        regionCache.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
    }
}
