package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final LocaleManager locale;

    public ReloadCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "reload"; }
    @Override public String getPermission() { return "dib.reload"; }
    @Override public String getUsage() { return "/dib reload"; }
    @Override public String getDescription() { return "Recarga la configuracion"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.reload();
        sender.sendMessage(locale.format("reload-success"));
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
