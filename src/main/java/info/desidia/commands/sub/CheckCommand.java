package info.desidia.commands.sub;

import info.desidia.DesidiaItemCraftBlock;
import info.desidia.commands.SubCommand;
import info.desidia.managers.BlockDecision;
import info.desidia.managers.BlockManager;
import info.desidia.managers.LocaleManager;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CheckCommand implements SubCommand {

    private final DesidiaItemCraftBlock plugin;
    private final BlockManager blockManager;
    private final LocaleManager locale;

    public CheckCommand(DesidiaItemCraftBlock plugin) {
        this.plugin = plugin;
        this.blockManager = plugin.getBlockManager();
        this.locale = plugin.getLocaleManager();
    }

    @Override public String getName() { return "check"; }
    @Override public String getPermission() { return "dib.check"; }
    @Override public String getUsage() { return "/dib check <item>"; }
    @Override public String getDescription() { return "Comprueba si un item esta bloqueado"; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(locale.format("player-only")); return; }
        if (args.length == 0) { sender.sendMessage(locale.format("unknown-command")); return; }
        Player player = (Player) sender;
        String matName = args[0].toUpperCase();
        Material material;
        try { material = Material.valueOf(matName); } catch (IllegalArgumentException e) {
            sender.sendMessage(locale.format("item-not-found", "item", matName)); return;
        }
        String worldName = player.getWorld().getName();
        String regionName = plugin.getWorldGuardHook() != null ? plugin.getWorldGuardHook().getRegionName(player) : null;
        BlockDecision d = blockManager.evaluate(player, material, worldName, regionName);
        sender.sendMessage(locale.format(d.isBlocked() ? "check-blocked" : "check-allowed", "item", matName));
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
