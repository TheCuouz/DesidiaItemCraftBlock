package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.gui.ItemManagementGUI;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class GuiCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final LocaleManager locale;

    public GuiCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "gui"; }
    @Override public String getPermission() { return "dib.gui"; }
    @Override public String getUsage() { return "/dib gui"; }
    @Override public String getDescription() { return "Abre el inventario de gestion visual"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(locale.format("player-only")); return; }
        new ItemManagementGUI(plugin, (Player) sender, "default").open();
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
