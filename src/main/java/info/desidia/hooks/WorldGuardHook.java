package info.desidia.hooks;

import info.desidia.managers.BlockDecision;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WorldGuardHook {
    public void register() {}
    public boolean isAvailable() { return false; }
    public BlockDecision evaluate(Player player, Material material, String region) { return null; }
    public String getRegionName(Player player) { return null; }
}
