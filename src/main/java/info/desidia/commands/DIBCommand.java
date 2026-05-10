package info.desidia.commands;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.sub.*;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;

public class DIBCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new LinkedHashMap<>();
    private final LocaleManager locale;

    public DIBCommand(DesidiaItemCraftBlock plugin) {
        this.locale = plugin.getLocaleManager();
        register(new HelpCommand(plugin, this));
        register(new ReloadCommand(plugin));
        register(new ToggleCommand(plugin));
        register(new StatusCommand(plugin));
        register(new ListCommand(plugin));
        register(new BlockCommand(plugin));
        register(new UnblockCommand(plugin));
        register(new CheckCommand(plugin));
        register(new StatsCommand(plugin));
        register(new GuiCommand(plugin));
    }

    private void register(SubCommand cmd) {
        subCommands.put(cmd.getName().toLowerCase(), cmd);
    }

    public Collection<SubCommand> getSubCommands() {
        return subCommands.values();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            subCommands.get("help").execute(sender, args);
            return true;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub == null) {
            sender.sendMessage(locale.format("unknown-command"));
            return true;
        }
        if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
            sender.sendMessage(locale.format("no-permission"));
            return true;
        }
        sub.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (SubCommand sub : subCommands.values()) {
                if (sub.getPermission() == null || sender.hasPermission(sub.getPermission())) {
                    if (sub.getName().startsWith(args[0].toLowerCase())) names.add(sub.getName());
                }
            }
            return names;
        }
        SubCommand sub = subCommands.get(args[0].toLowerCase());
        if (sub != null && (sub.getPermission() == null || sender.hasPermission(sub.getPermission()))) {
            return sub.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        return Collections.emptyList();
    }
}
