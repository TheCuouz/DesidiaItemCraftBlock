package info.desidia;

import info.desidia.commands.DIBCommand;
import info.desidia.hooks.PlaceholderAPIHook;
import info.desidia.hooks.WorldGuardHook;
import info.desidia.listeners.CraftListener;
import info.desidia.managers.BlockManager;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DesidiaItemCraftBlock extends JavaPlugin {

    private ConfigManager configManager;
    private LocaleManager localeManager;
    private StatsManager statsManager;
    private BlockManager blockManager;
    private WorldGuardHook worldGuardHook;
    private PlaceholderAPIHook placeholderAPIHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        localeManager = new LocaleManager(this, configManager.getLocale());
        statsManager = new StatsManager(this);

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            worldGuardHook.register();
            getLogger().info("[DIB] WorldGuard detected - region blocking enabled.");
        }

        blockManager = new BlockManager(configManager, worldGuardHook);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIHook = new PlaceholderAPIHook(this);
            placeholderAPIHook.register();
            getLogger().info("[DIB] PlaceholderAPI detected - placeholders registered.");
        }

        getServer().getPluginManager().registerEvents(new CraftListener(this), this);

        DIBCommand cmd = new DIBCommand(this);
        getCommand("dib").setExecutor(cmd);
        getCommand("dib").setTabCompleter(cmd);

        // Stats autosave task
        int intervalTicks = configManager.getStatsAutosaveInterval() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, statsManager::save, intervalTicks, intervalTicks);

        getLogger().info("[DIB] DesidiaItemCraftBlock v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (statsManager != null) statsManager.save();
        getLogger().info("[DIB] Stats saved. Plugin disabled.");
    }

    public void reload() {
        configManager.reload();
        localeManager.reload(configManager.getLocale());
    }

    public ConfigManager getConfigManager() { return configManager; }
    public LocaleManager getLocaleManager() { return localeManager; }
    public StatsManager getStatsManager() { return statsManager; }
    public BlockManager getBlockManager() { return blockManager; }
    public WorldGuardHook getWorldGuardHook() { return worldGuardHook; }
    public PlaceholderAPIHook getPlaceholderAPIHook() { return placeholderAPIHook; }
}
