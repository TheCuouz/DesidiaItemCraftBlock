package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ListCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public ListCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "list"; }
    @Override public String getPermission() { return "dib.list"; }
    @Override public String getUsage() { return "/dib list [world]"; }
    @Override public String getDescription() { return "Lista items bloqueados/permitidos"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String worldName = args.length > 0 ? args[0] : "default";
        ConfigManager.WorldConfig wc = config.getWorldConfig(worldName);
        if (wc == null) {
            sender.sendMessage(locale.format("item-not-found", "item", worldName));
            return;
        }
        sender.sendMessage(locale.format("list-header", "world", worldName, "mode", wc.mode));
        Set<String> list = "blacklist".equals(wc.mode) ? wc.blocked : wc.allowed;
        if (list.isEmpty()) {
            sender.sendMessage(locale.format("list-empty"));
        } else {
            list.forEach(item -> sender.sendMessage(locale.format("list-entry", "item", item)));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Bukkit.getWorlds().stream().map(w -> w.getName()).collect(Collectors.toList());
    }
}
