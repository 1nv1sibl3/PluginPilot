package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RestoreCommand extends SubCommand {
    
    public RestoreCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "restore";
    }
    
    @Override
    public String getDescription() {
        return "Restore a plugin from backup";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("rollback");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.restore";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp restore <plugin> [version]");
            return;
        }
        
        String pluginName = args[0];
        String version = args.length > 1 ? args[1] : null;
        
        // Implementation would restore the specified plugin from backup
        sender.sendMessage("§6Restoring plugin: §e" + pluginName + 
                          (version != null ? " §6to version: §e" + version : ""));
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