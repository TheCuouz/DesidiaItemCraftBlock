package info.desidia.commands;

import info.desidia.DesidiaItemCraftBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

// Full implementation in Task 10
public class DIBCommand implements CommandExecutor, TabCompleter {
    public DIBCommand(DesidiaItemCraftBlock plugin) {}
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) { return true; }
    @Override public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) { return Collections.emptyList(); }
}
