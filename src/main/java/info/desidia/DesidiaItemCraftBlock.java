package info.desidia;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.io.File;

public final class DesidiaItemCraftBlock extends SimplePlugin implements Listener {

	private boolean enableCraftMessage;
	private String craftMessage;

	@Override
	protected void onPluginStart() {
		System.out.println("@Este plugin se ha iniciado correctamente!");
		System.out.println("@This plugin enabled successfully!");
		System.out.println("@Cuouz firma!");

		if (!getDataFolder().exists()) {
			getDataFolder().mkdirs();
		}

		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) {
			saveResource("config.yml", false);
		}

		reloadConfiguration();

		getServer().getPluginManager().registerEvents(this, this);
		getCommand("dib").setExecutor(new Commands(this));
	}

	public void reloadConfiguration() {
		reloadConfig();
		enableCraftMessage = getConfig().getBoolean("EnableCraftMessage", false);
		craftMessage = getConfig().getString("Message", "¡No, no, no! ¡You can't craft this item!");
	}

	@EventHandler
	public void onCrafting(CraftItemEvent event) {
		if (!event.isCancelled()) {
			event.setCancelled(true);

			if (enableCraftMessage) {
				event.getWhoClicked().sendMessage(craftMessage);
			}
		}
	}
}