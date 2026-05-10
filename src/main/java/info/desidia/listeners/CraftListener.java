package info.desidia.listeners;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.api.events.CraftBlockedEvent;
import info.desidia.managers.BlockDecision;
import info.desidia.managers.BlockManager;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import info.desidia.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CraftListener implements Listener {

    private final DesidiaItemCraftBlock plugin;
    private final BlockManager blockManager;
    private final ConfigManager config;
    private final LocaleManager locale;
    private final StatsManager stats;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public CraftListener(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.blockManager = plugin.getBlockManager();
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
        this.stats = plugin.getStatsManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Material material = event.getRecipe().getResult().getType();
        String worldName = player.getWorld().getName();
        String regionName = plugin.getWorldGuardHook() != null
                ? plugin.getWorldGuardHook().getRegionName(player) : null;

        BlockDecision decision = blockManager.evaluate(player, material, worldName, regionName);
        if (!decision.isBlocked()) return;

        // Fire cancellable API event
        CraftBlockedEvent apiEvent = new CraftBlockedEvent(
                player, material, event.getRecipe(), worldName, regionName, decision.getReason());
        Bukkit.getPluginManager().callEvent(apiEvent);
        if (apiEvent.isCancelled()) return;

        event.setCancelled(true);

        stats.record(player.getUniqueId(), player.getName(), material.name(), worldName, regionName);

        if (config.isLogAttempts()) {
            plugin.getLogger().info("[DIB] " + player.getName() + " tried to craft "
                    + material.name() + " in " + worldName);
        }

        if (config.isNotifyAdmins()) {
            String msg = locale.format("admin-notify",
                    "player", player.getName(), "item", material.name(),
                    "world", worldName, "region", regionName == null ? "" : regionName);
            Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.hasPermission("dib.notify"))
                    .forEach(p -> p.sendMessage(msg));
        }

        if (!config.isMessageEnabled()) return;

        long now = System.currentTimeMillis();
        if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < config.getMessageCooldown() * 1000L) return;
        cooldowns.put(player.getUniqueId(), now);

        String rawMsg = decision.getCustomMessage() != null
                ? decision.getCustomMessage()
                : locale.format("craft-blocked", "item", material.name(), "player", player.getName(),
                "world", worldName, "region", regionName == null ? "" : regionName);

        sendMessage(player, ColorUtil.color(rawMsg));

        try {
            Sound sound = Sound.valueOf(config.getMessageSound());
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (IllegalArgumentException ignored) {}
    }

    private void sendMessage(Player player, String msg) {
        switch (config.getMessageType().toUpperCase()) {
            case "ACTIONBAR":
                player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
                break;
            case "TITLE":
                player.sendTitle(msg, "", 10, 40, 10);
                break;
            default:
                player.sendMessage(msg);
        }
    }
}
