package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ImportCommand extends SubCommand {
    
    public ImportCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "import";
    }
    
    @Override
    public String getDescription() {
        return "Import an existing plugin into PluginPilot management";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("add-existing", "manage");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.install";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp import <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        CompletableFuture.runAsync(() -> {
            try {
                // Check if plugin exists on server
                Plugin existingPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
                if (existingPlugin == null) {
                    sender.sendMessage("§cPlugin §e" + pluginName + " §cis not installed on this server!");
                    return;
                }
                
                // Check if already managed
                if (plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                    sender.sendMessage("§cPlugin §e" + pluginName + " §cis already managed by PluginPilot!");
                    return;
                }
                
                // Try to find the plugin in repositories
                List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(pluginName);
                PluginSearchResult matchedPlugin = null;
                
                for (PluginSearchResult result : results) {
                    if (result.getName().equalsIgnoreCase(pluginName)) {
                        matchedPlugin = result;
                        break;
                    }
                }
                
                if (matchedPlugin != null) {
                    // Found in repositories - import with full metadata
                    PluginVersion currentVersion = new PluginVersion();
                    currentVersion.setVersion(existingPlugin.getDescription().getVersion());
                    currentVersion.setId("imported-" + System.currentTimeMillis());
                    
                    plugin.getDatabaseManager().saveInstalledPlugin(matchedPlugin, currentVersion);
                    
                    sender.sendMessage("§aSuccessfully imported §e" + pluginName + " §av" + 
                                     existingPlugin.getDescription().getVersion() + " §ainto PluginPilot!");
                    sender.sendMessage("§7Plugin found in §e" + matchedPlugin.getSourceType() + " §7repository.");
                    sender.sendMessage("§7Updates and management features are now available.");
                    
                } else {
                    // Not found in repositories - import as custom
                    PluginSearchResult customPlugin = new PluginSearchResult();
                    customPlugin.setId("custom-" + pluginName.toLowerCase());
                    customPlugin.setName(existingPlugin.getName());
                    customPlugin.setDescription(existingPlugin.getDescription().getDescription());
                    customPlugin.setAuthor(String.join(", ", existingPlugin.getDescription().getAuthors()));
                    customPlugin.setSourceType("custom");
                    customPlugin.setSourceId("imported");
                    
                    PluginVersion currentVersion = new PluginVersion();
                    currentVersion.setVersion(existingPlugin.getDescription().getVersion());
                    currentVersion.setId("imported-" + System.currentTimeMillis());
                    
                    plugin.getDatabaseManager().saveInstalledPlugin(customPlugin, currentVersion);
                    
                    sender.sendMessage("§aSuccessfully imported §e" + pluginName + " §av" + 
                                     existingPlugin.getDescription().getVersion() + " §ainto PluginPilot!");
                    sender.sendMessage("§7Plugin not found in repositories - imported as custom plugin.");
                    sender.sendMessage("§7Limited management features available (no automatic updates).");
                }
                
                // Log the action
                plugin.getDatabaseManager().logAction("IMPORT", "Plugin imported: " + pluginName, pluginName);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error importing plugin " + pluginName + ": " + e.getMessage());
                sender.sendMessage("§cError importing plugin: " + e.getMessage());
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // Get unmanaged plugins
            try {
                Plugin[] allPlugins = plugin.getServer().getPluginManager().getPlugins();
                List<String> managedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
                
                List<String> unmanagedPlugins = new ArrayList<>();
                for (Plugin p : allPlugins) {
                    if (!p.getName().equals("PluginPilot") && 
                        managedPlugins.stream().noneMatch(managed -> managed.equalsIgnoreCase(p.getName()))) {
                        unmanagedPlugins.add(p.getName());
                    }
                }
                
                return unmanagedPlugins.stream()
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