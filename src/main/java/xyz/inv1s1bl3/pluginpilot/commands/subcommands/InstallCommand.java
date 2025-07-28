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

public class InstallCommand extends SubCommand {
    
    public InstallCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "install";
    }
    
    @Override
    public String getDescription() {
        return "Install a plugin";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("add", "get");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.install";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp install <plugin> [version]");
            return;
        }
        
        String pluginName = args[0];
        String version = args.length > 1 ? args[1] : null;
        
        plugin.getMessageFormatter().sendMessage(sender, "loading");
        
        // Run installation asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                // Search for the plugin
                List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(pluginName);
                
                if (results.isEmpty()) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
                    return;
                }
                
                // Find exact match or closest match
                PluginSearchResult targetPlugin = findBestMatch(results, pluginName);
                
                if (targetPlugin == null) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
                    return;
                }
                
                // Get available versions
                List<PluginVersion> versions = plugin.getSourceManager().getPluginVersions(targetPlugin);
                
                if (versions.isEmpty()) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-install-failed", pluginName, "No versions available");
                    return;
                }
                
                // Select version
                PluginVersion targetVersion;
                if (version != null) {
                    targetVersion = versions.stream()
                            .filter(v -> v.getVersion().equals(version))
                            .findFirst()
                            .orElse(null);
                    
                    if (targetVersion == null) {
                        plugin.getMessageFormatter().sendMessage(sender, "plugin-install-failed", pluginName, "Version " + version + " not found");
                        return;
                    }
                } else {
                    // Use latest version
                    targetVersion = versions.get(0);
                }
                
                // Check if already installed
                if (plugin.getDatabaseManager().isPluginInstalled(targetPlugin.getName())) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-already-exists", targetPlugin.getName());
                    return;
                }
                
                // Download and install
                boolean success = plugin.getSourceManager().downloadAndInstallPlugin(targetPlugin, targetVersion);
                
                if (success) {
                    // Save to database
                    plugin.getDatabaseManager().saveInstalledPlugin(targetPlugin, targetVersion);
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-installed", targetPlugin.getName(), targetVersion.getVersion());
                } else {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-install-failed", pluginName, "Download failed");
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error installing plugin " + pluginName + ": " + e.getMessage());
                plugin.getMessageFormatter().sendMessage(sender, "plugin-install-failed", pluginName, e.getMessage());
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
            // This will be handled by AsyncTabCompleter
            return new ArrayList<>();
        } else if (args.length == 2) {
            // Tab complete versions for the selected plugin
            String pluginName = args[0];
            
            try {
                // First try to get cached search results
                List<PluginSearchResult> results = plugin.getDatabaseManager().getCachedSearchResults(pluginName);
                if (results != null && !results.isEmpty()) {
                    PluginSearchResult targetPlugin = findBestMatch(results, pluginName);
                    if (targetPlugin != null) {
                        // Try to get cached versions first
                        List<PluginVersion> versions = this.plugin.getDatabaseManager().getCachedPluginVersions(targetPlugin.getId());
                        if (versions == null) {
                            // If not cached, fetch from API
                            versions = this.plugin.getSourceManager().getPluginVersions(targetPlugin);
                        }
                        return versions.stream()
                                .map(PluginVersion::getVersion)
                                .filter(v -> v.toLowerCase().startsWith(args[1].toLowerCase()))
                                .limit(10)
                                .toList();
                    }
                }
            } catch (Exception e) {
                // Ignore and return empty list
            }
        }
        
        return new ArrayList<>();
    }
}