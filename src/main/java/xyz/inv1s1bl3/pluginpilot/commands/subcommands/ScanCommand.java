package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScanCommand extends SubCommand {
    
    public ScanCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "scan";
    }
    
    @Override
    public String getDescription() {
        return "Scan a plugin for malware";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("security", "check");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.security";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp scan <plugin>");
            return;
        }
        
        String pluginName = args[0];
        plugin.getMessageFormatter().sendMessage(sender, "scan-started", pluginName);
        
        // Implementation would scan the specified plugin for malware
        sender.sendMessage("§6Scanning plugin: §e" + pluginName);
        sender.sendMessage("§7This feature is under development");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            try {
                return plugin.getDatabaseManager().getInstalledPluginNames().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .limit(10)
                        .toList();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>();
    }
}