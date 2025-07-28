package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListCommand extends SubCommand {
    
    public ListCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "list";
    }
    
    @Override
    public String getDescription() {
        return "List managed plugins";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("ls", "plugins");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.view";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            List<String> installedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
            
            if (installedPlugins.isEmpty()) {
                sender.sendMessage("§6No managed plugins found.");
                return;
            }
            
            sender.sendMessage("§6=== Managed Plugins (" + installedPlugins.size() + ") ===");
            for (String pluginName : installedPlugins) {
                sender.sendMessage("§e• " + pluginName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error listing plugins: " + e.getMessage());
            sender.sendMessage("§cError retrieving plugin list.");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}