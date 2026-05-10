package info.desidia.commands;

import org.bukkit.command.CommandSender;
import java.util.List;

public interface SubCommand {
    String getName();
    String getPermission();
    String getUsage();
    String getDescription();
    void execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
}
