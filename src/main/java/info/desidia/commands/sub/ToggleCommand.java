package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToggleCommand implements SubCommand {

    private final ConfigManager config;
    private final LocaleManager locale;

    public ToggleCommand(DesidiaItemCraftBlock plugin) {
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "toggle"; }
    @Override public String getPermission() { return "dib.toggle"; }
    @Override public String getUsage() { return "/dib toggle [world]"; }
    @Override public String getDescription() { return "Activa/desactiva el bloqueo"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            ConfigManager.WorldConfig wc = config.getWorldConfigs().get(worldName);
            if (wc == null) {
                sender.sendMessage(locale.format("item-not-found", "item", worldName));
                return;
            }
            wc.enabled = !wc.enabled;
            String key = wc.enabled ? "toggle-world-enabled" : "toggle-world-disabled";
            sender.sendMessage(locale.format(key, "world", worldName));
        } else {
            boolean newState = !config.isEnabled();
            config.setEnabled(newState);
            sender.sendMessage(locale.format(newState ? "toggle-enabled" : "toggle-disabled"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getWorlds().stream()
                    .map(w -> w.getName())
                    .filter(n -> n.startsWith(args[0]))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
