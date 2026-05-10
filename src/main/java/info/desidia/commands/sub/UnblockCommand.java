package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class UnblockCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public UnblockCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "unblock"; }
    @Override public String getPermission() { return "dib.unblock"; }
    @Override public String getUsage() { return "/dib unblock <item> [world]"; }
    @Override public String getDescription() { return "Desbloquea un item"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        String matName = args[0].toUpperCase();
        String world = args.length > 1 ? args[1] : "default";
        config.removeBlockedItem(world, matName);
        sender.sendMessage(locale.format("unblock-success", "item", matName, "world", world));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(n -> n.startsWith(args[0].toUpperCase()))
                    .limit(20).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
