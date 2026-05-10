package info.desidia;

import org.bukkit.plugin.java.JavaPlugin;

public final class DesidiaItemCraftBlock extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("DesidiaItemCraftBlock enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DesidiaItemCraftBlock disabled!");
    }
}
