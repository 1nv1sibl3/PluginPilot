package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends SubCommand {
    
    public ReloadCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "reload";
    }
    
    @Override
    public String getDescription() {
        return "Reload a specific plugin";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("reloadplugin", "restart");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.reload";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "pp reload <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        // Check if plugin exists and is loaded
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
            return;
        }
        
        // Prevent reloading PluginPilot itself to avoid issues
        if (targetPlugin.getName().equals("PluginPilot")) {
            sender.sendMessage(plugin.getMessageFormatter().colorize("&cYou cannot reload PluginPilot while it's running!"));
            sender.sendMessage(plugin.getMessageFormatter().colorize("&cUse &e/pp refreshconfig &cto reload the configuration instead."));
            return;
        }
        
        sender.sendMessage(plugin.getMessageFormatter().colorize("&7Reloading plugin &e" + targetPlugin.getName() + "&7..."));
        
        CompletableFuture.runAsync(() -> {
            try {
                // Get the plugin file
                File pluginsDir = new File("plugins");
                File pluginFile = new File(pluginsDir, targetPlugin.getName() + ".jar");
                
                if (!pluginFile.exists()) {
                    // Try to find the plugin file by scanning the directory
                    File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().equalsIgnoreCase(targetPlugin.getName() + ".jar")) {
                                pluginFile = file;
                                break;
                            }
                        }
                    }
                    
                    if (!pluginFile.exists()) {
                        plugin.getMessageFormatter().sendMessage(sender, "plugin-reload-failed", targetPlugin.getName(), "Plugin file not found");
                        return;
                    }
                }
                
                // Disable the plugin
                PluginManager pluginManager = plugin.getServer().getPluginManager();
                pluginManager.disablePlugin(targetPlugin);
                
                try {
                    // Load and enable the plugin again
                    Plugin reloadedPlugin = pluginManager.loadPlugin(pluginFile);
                    if (reloadedPlugin != null) {
                        pluginManager.enablePlugin(reloadedPlugin);
                        plugin.getMessageFormatter().sendMessage(sender, "plugin-reloaded", reloadedPlugin.getName());
                        plugin.getDatabaseManager().logAction("RELOAD", "Plugin reloaded via command", reloadedPlugin.getName());
                    } else {
                        plugin.getMessageFormatter().sendMessage(sender, "plugin-reload-failed", targetPlugin.getName(), "Failed to load plugin");
                    }
                } catch (Exception e) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-reload-failed", targetPlugin.getName(), e.getMessage());
                    plugin.getLogger().severe("Error reloading plugin " + targetPlugin.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                    
                    // Try to re-enable the original plugin
                    try {
                        pluginManager.enablePlugin(targetPlugin);
                        sender.sendMessage(plugin.getMessageFormatter().colorize("&eAttempted to re-enable the original plugin."));
                    } catch (Exception ex) {
                        plugin.getLogger().severe("Failed to re-enable the original plugin: " + ex.getMessage());
                    }
                }
                
            } catch (Exception e) {
                plugin.getMessageFormatter().sendMessage(sender, "plugin-reload-failed", targetPlugin.getName(), e.getMessage());
                plugin.getLogger().severe("Error reloading plugin " + targetPlugin.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            
            for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
                // Don't suggest PluginPilot itself
                if (!p.getName().equals("PluginPilot") && p.isEnabled()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                        suggestions.add(p.getName());
                    }
                }
            }
            
            return suggestions;
        }
        
        return new ArrayList<>();
    }
}