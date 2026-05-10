package info.desidia.gui;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.managers.ConfigManager;
import info.desidia.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ItemManagementGUI implements Listener {

    private static final int PAGE_SIZE = 45;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_NEXT = 53;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_ADD_HELD = 47;
    private static final int SLOT_WORLD_INFO = 4;

    private final DesidiaItemCraftBlock plugin;
    private final Player player;
    private final String world;
    private final ConfigManager config;

    private Inventory inventory;
    private int page = 0;
    private List<String> currentItems;

    public ItemManagementGUI(DesidiaItemCraftBlock plugin, Player player, String world) {
        this.plugin = plugin;
        this.player = player;
        this.world = world;
        this.config = plugin.getConfigManager();
    }

    public void open() {
        refreshItemList();
        inventory = Bukkit.createInventory(null, 54,
                ColorUtil.color("&8[DIB] &eGestion: &f" + world));
        populate();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        player.openInventory(inventory);
    }

    private void refreshItemList() {
        ConfigManager.WorldConfig wc = config.getWorldConfig(world);
        Set<String> blocked = wc != null ? wc.blocked : config.getWorldConfig("default") != null
                ? config.getWorldConfig("default").blocked : new java.util.LinkedHashSet<>();
        currentItems = new ArrayList<>(blocked);
    }

    private void populate() {
        inventory.clear();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, currentItems.size());

        for (int i = start; i < end; i++) {
            String matName = currentItems.get(i);
            Material mat = parseMaterial(matName);
            ItemStack item = mat != null ? new ItemStack(mat) : new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.color("&c" + matName));
                meta.setLore(Arrays.asList(
                        ColorUtil.color("&7Click izquierdo: &fDesbloquear"),
                        ColorUtil.color("&7Mundo: &f" + world)
                ));
                item.setItemMeta(meta);
            }
            inventory.setItem(i - start, item);
        }

        // Navigation row
        if (page > 0) inventory.setItem(SLOT_PREV, makeControl(Material.ARROW, "&aPagina anterior", "&7Pagina " + page));
        if (end < currentItems.size()) inventory.setItem(SLOT_NEXT, makeControl(Material.ARROW, "&aSiguiente pagina", "&7Pagina " + (page + 2)));
        inventory.setItem(SLOT_CLOSE, makeControl(Material.BARRIER, "&cCerrar", ""));
        inventory.setItem(SLOT_ADD_HELD, makeControl(Material.EMERALD, "&aAnadir item en mano", "&7Bloquea el item que llevas"));
        inventory.setItem(SLOT_WORLD_INFO, makeControl(Material.BOOK, "&eMundo: &f" + world,
                "&7Items bloqueados: &f" + currentItems.size()));
    }

    private Material parseMaterial(String name) {
        if (name.contains("*")) return null;
        try { return Material.valueOf(name.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    private ItemStack makeControl(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            if (!lore.isEmpty()) meta.setLore(Arrays.asList(ColorUtil.color(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!event.getWhoClicked().equals(player)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        if (slot == SLOT_CLOSE) {
            player.closeInventory();
            return;
        }
        if (slot == SLOT_PREV && page > 0) {
            page--;
            populate();
            return;
        }
        if (slot == SLOT_NEXT && (page + 1) * PAGE_SIZE < currentItems.size()) {
            page++;
            populate();
            return;
        }
        if (slot == SLOT_ADD_HELD) {
            addHeldItem();
            return;
        }
        if (slot < PAGE_SIZE) {
            int index = page * PAGE_SIZE + slot;
            if (index < currentItems.size()) {
                String matName = currentItems.get(index);
                config.removeBlockedItem(world, matName);
                player.sendMessage(plugin.getLocaleManager().format("unblock-success",
                        "item", matName, "world", world));
                refreshItemList();
                populate();
            }
        }
    }

    private void addHeldItem() {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            player.sendMessage(plugin.getLocaleManager().format("gui-no-item-in-hand"));
            return;
        }
        String matName = hand.getType().name();
        config.addBlockedItem(world, matName);
        player.sendMessage(plugin.getLocaleManager().format("block-success", "item", matName, "world", world));
        refreshItemList();
        populate();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().equals(inventory) && event.getPlayer().equals(player)) {
            HandlerList.unregisterAll(this);
        }
    }
}
