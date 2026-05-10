package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.ConfigManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class StatusCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final ConfigManager config;
    private final LocaleManager locale;

    public StatusCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "status"; }
    @Override public String getPermission() { return "dib.status"; }
    @Override public String getUsage() { return "/dib status"; }
    @Override public String getDescription() { return "Muestra el estado del plugin"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(locale.format("status-header", "version", plugin.getDescription().getVersion()));
        sender.sendMessage(locale.format(config.isEnabled() ? "status-enabled" : "status-disabled"));
        sender.sendMessage(locale.format("status-mode", "mode", config.getGlobalMode()));
        sender.sendMessage(locale.format("status-wg", "status",
                plugin.getWorldGuardHook() != null ? "activo" : "no instalado"));
        sender.sendMessage(locale.format("status-papi", "status",
                plugin.getPlaceholderAPIHook() != null ? "activo" : "no instalado"));
    }

    @Override public List<String> tabComplete(CommandSender sender, String[] args) { return Collections.emptyList(); }
}
