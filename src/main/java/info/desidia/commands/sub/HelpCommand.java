package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.DIBCommand;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements SubCommand {

    private final LocaleManager locale;
    private final DIBCommand dispatcher;
    private final String version;

    public HelpCommand(DesidiaItemCraftBlock plugin, DIBCommand dispatcher) {
        this.locale = plugin.getLocaleManager();
        this.dispatcher = dispatcher;
        this.version = plugin.getDescription().getVersion();
    }

    @Override public String getName() { return "help"; }
    @Override public String getPermission() { return "dib.help"; }
    @Override public String getUsage() { return "/dib help"; }
    @Override public String getDescription() { return "Muestra todos los comandos"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(locale.format("help-header", "version", version));
        for (SubCommand sub : dispatcher.getSubCommands()) {
            if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(locale.format("help-entry", "cmd", sub.getUsage(), "desc", sub.getDescription()));
            }
        }
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
