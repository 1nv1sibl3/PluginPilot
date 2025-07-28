package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SourcesCommand extends SubCommand {
    
    public SourcesCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "sources";
    }
    
    @Override
    public String getDescription() {
        return "Manage plugin sources";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("repos", "repositories");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.sources";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6=== Plugin Sources ===");
        sender.sendMessage("§a✓ Modrinth §7- Enabled");
        sender.sendMessage("§a✓ Hangar §7- Enabled");
        sender.sendMessage("§c✗ SpigotMC §7- Limited (no public API)");
        sender.sendMessage("§c✗ Polymart §7- Not implemented");
        sender.sendMessage("§7");
        sender.sendMessage("§7Custom source management is under development");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("list", "add", "remove", "enable", "disable").stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return new ArrayList<>();
    }
}