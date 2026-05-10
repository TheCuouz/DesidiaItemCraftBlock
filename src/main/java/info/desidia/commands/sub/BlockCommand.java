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

public class BlockCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public BlockCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "block"; }
    @Override public String getPermission() { return "dib.block"; }
    @Override public String getUsage() { return "/dib block <item> [world]"; }
    @Override public String getDescription() { return "Bloquea un item"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        String matName = args[0].toUpperCase();
        try { Material.valueOf(matName); } catch (IllegalArgumentException e) {
            sender.sendMessage(locale.format("item-not-found", "item", matName)); return;
        }
        String world = args.length > 1 ? args[1] : "default";
        config.addBlockedItem(world, matName);
        sender.sendMessage(locale.format("block-success", "item", matName, "world", world));
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
