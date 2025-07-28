package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiscoverCommand extends SubCommand {
    
    public DiscoverCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "discover";
    }
    
    @Override
    public String getDescription() {
        return "Discover and manage unmanaged plugins";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("find", "detect", "scan");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.view";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6Discovering unmanaged plugins...");
        
        CompletableFuture.runAsync(() -> {
            try {
                // Get all installed plugins
                Plugin[] allPlugins = plugin.getServer().getPluginManager().getPlugins();
                List<String> managedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
                
                List<String> unmanagedPlugins = new ArrayList<>();
                
                for (Plugin p : allPlugins) {
                    String pluginName = p.getName();
                    
                    // Skip PluginPilot itself
                    if (pluginName.equals("PluginPilot")) {
                        continue;
                    }
                    
                    // Check if plugin is managed
                    boolean isManaged = managedPlugins.stream()
                            .anyMatch(managed -> managed.equalsIgnoreCase(pluginName));
                    
                    if (!isManaged) {
                        unmanagedPlugins.add(pluginName);
                    }
                }
                
                if (unmanagedPlugins.isEmpty()) {
                    sender.sendMessage("§aAll plugins are managed by PluginPilot!");
                    return;
                }
                
                sender.sendMessage("§6=== Unmanaged Plugins Found (" + unmanagedPlugins.size() + ") ===");
                
                for (String unmanagedPlugin : unmanagedPlugins) {
                    Plugin p = plugin.getServer().getPluginManager().getPlugin(unmanagedPlugin);
                    if (p != null) {
                        sender.sendMessage("§e• " + p.getName() + " §7v" + p.getDescription().getVersion() + 
                                         " §7by " + String.join(", ", p.getDescription().getAuthors()));
                    }
                }
                
                sender.sendMessage("§7");
                sender.sendMessage("§7Use §e/pp import <plugin> §7to add these plugins to PluginPilot management.");
                sender.sendMessage("§7Note: Imported plugins may have limited update capabilities.");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error discovering plugins: " + e.getMessage());
                sender.sendMessage("§cError discovering plugins: " + e.getMessage());
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}