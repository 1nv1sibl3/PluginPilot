package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InfoCommand extends SubCommand {
    
    public InfoCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Show detailed plugin information";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("details", "about");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.view";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp info <plugin>");
            return;
        }
        
        String pluginName = args[0];
        plugin.getMessageFormatter().sendMessage(sender, "loading");
        
        CompletableFuture.runAsync(() -> {
            try {
                List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(pluginName);
                
                if (results.isEmpty()) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
                    return;
                }
                
                PluginSearchResult targetPlugin = findBestMatch(results, pluginName);
                if (targetPlugin == null) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
                    return;
                }
                
                // Get versions
                List<PluginVersion> versions = plugin.getSourceManager().getPluginVersions(targetPlugin);
                
                // Display detailed information
                sender.sendMessage("§6=== Plugin Information ===");
                sender.sendMessage("§e• Name: §f" + targetPlugin.getName());
                sender.sendMessage("§e• Author: §f" + targetPlugin.getAuthor());
                sender.sendMessage("§e• Source: §f" + targetPlugin.getSourceType());
                sender.sendMessage("§e• Downloads: §f" + targetPlugin.getDownloads());
                if (targetPlugin.getRating() > 0) {
                    sender.sendMessage("§e• Rating: §f" + String.format("%.1f", targetPlugin.getRating()));
                }
                sender.sendMessage("§e• Latest Version: §f" + targetPlugin.getLatestVersion());
                sender.sendMessage("§e• Description: §f" + targetPlugin.getDescription());
                
                if (!versions.isEmpty()) {
                    sender.sendMessage("§e• Available Versions: §f" + Math.min(versions.size(), 5) + 
                                     (versions.size() > 5 ? " (showing first 5)" : ""));
                    for (int i = 0; i < Math.min(versions.size(), 5); i++) {
                        PluginVersion version = versions.get(i);
                        sender.sendMessage("  §7- §f" + version.getVersion() + 
                                         (version.getMinecraftVersions() != null ? 
                                          " §7(MC: " + version.getMinecraftVersions() + ")" : ""));
                    }
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error getting plugin info for " + pluginName + ": " + e.getMessage());
                sender.sendMessage("§cError retrieving plugin information: " + e.getMessage());
            }
        });
    }
    
    private PluginSearchResult findBestMatch(List<PluginSearchResult> results, String query) {
        // First try exact match
        for (PluginSearchResult result : results) {
            if (result.getName().equalsIgnoreCase(query)) {
                return result;
            }
        }
        
        // Then try case-insensitive contains
        for (PluginSearchResult result : results) {
            if (result.getName().toLowerCase().contains(query.toLowerCase())) {
                return result;
            }
        }
        
        // Return first result as fallback
        return results.isEmpty() ? null : results.get(0);
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