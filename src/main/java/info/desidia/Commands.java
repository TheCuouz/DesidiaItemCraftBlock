package info.desidia;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {

	private final DesidiaItemCraftBlock plugin;

	public Commands(DesidiaItemCraftBlock plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (label.equalsIgnoreCase("dib") && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("desidia.reload")) {
				sender.sendMessage("You don't have the required permission.");
				return true;
			}

			plugin.reloadConfiguration();
			sender.sendMessage("Config reloaded.");
			return true;
		}
		return false;
	}
}