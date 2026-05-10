package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.LocaleManager;
import info.desidia.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsCommand implements SubCommand {

    private final StatsManager stats;
    private final LocaleManager locale;

    public StatsCommand(DesidiaItemCraftBlock plugin) {
        this.stats = plugin.getStatsManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "stats"; }
    @Override public String getPermission() { return "dib.stats"; }
    @Override public String getUsage() { return "/dib stats [player|reset [player]]"; }
    @Override public String getDescription() { return "Estadisticas de crafteo bloqueado"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            if (!sender.hasPermission("dib.stats.reset")) {
                sender.sendMessage(locale.format("no-permission")); return;
            }
            if (args.length > 1) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(locale.format("item-not-found", "item", args[1])); return; }
                stats.resetPlayer(target.getUniqueId());
                sender.sendMessage(locale.format("stats-reset-success", "player", target.getName()));
            } else {
                stats.resetAll();
                sender.sendMessage(locale.format("stats-reset-global"));
            }
            return;
        }

        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(locale.format("item-not-found", "item", args[0])); return; }
            String topItem = stats.getTopItem(target.getUniqueId());
            sender.sendMessage(locale.format("stats-header", "player", target.getName()));
            sender.sendMessage(locale.format("stats-total", "total", stats.getTotalBlocked(target.getUniqueId())));
            sender.sendMessage(locale.format("stats-top-item", "item", topItem,
                    "count", stats.getItemCount(target.getUniqueId(), topItem)));
            sender.sendMessage(locale.format("stats-last", "world", stats.getLastWorld(target.getUniqueId()),
                    "region", stats.getLastRegion(target.getUniqueId()),
                    "time", stats.getTimeSinceLastAttempt(target.getUniqueId())));
        } else {
            sender.sendMessage(locale.format("stats-global-header"));
            sender.sendMessage(locale.format("stats-total", "total", stats.getGlobalTotal()));
            List<Map.Entry<String, Integer>> topItems = stats.getGlobalTopItems(3);
            if (!topItems.isEmpty()) {
                String items = topItems.stream().map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
                sender.sendMessage(locale.format("stats-top-items", "items", items));
            }
            List<Map.Entry<String, Integer>> topPlayers = stats.getGlobalTopPlayers(3);
            if (!topPlayers.isEmpty()) {
                String players = topPlayers.stream().map(e -> e.getKey() + " (" + e.getValue() + ")").collect(Collectors.joining(", "));
                sender.sendMessage(locale.format("stats-top-players", "players", players));
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            opts.add("reset");
            Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
            return opts.stream().filter(o -> o.startsWith(args[0])).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).filter(n -> n.startsWith(args[1])).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
