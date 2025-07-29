package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoadCommand extends SubCommand {
    
    public LoadCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "load";
    }
    
    @Override
    public String getDescription() {
        return "Load a plugin from the plugins directory";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("enable");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.load";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "pp load <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        // Check if plugin is already loaded
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin != null && targetPlugin.isEnabled()) {
            plugin.getMessageFormatter().sendMessage(sender, "plugin-already-loaded", pluginName);
            return;
        }
        
        sender.sendMessage(plugin.getMessageFormatter().colorize("&7Attempting to load plugin &e" + pluginName + "&7..."));
        
        CompletableFuture.runAsync(() -> {
            try {
                File pluginsDir = new File("plugins");
                File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
                
                if (files == null) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-load-failed", pluginName, "Could not access plugins directory");
                    return;
                }
                
                File pluginFile = null;
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.equalsIgnoreCase(pluginName + ".jar")) {
                        pluginFile = file;
                        break;
                    }
                }
                
                if (pluginFile == null) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
                    return;
                }
                
                PluginManager pluginManager = plugin.getServer().getPluginManager();
                Plugin loadedPlugin = null;
                
                try {
                    loadedPlugin = pluginManager.loadPlugin(pluginFile);
                } catch (InvalidPluginException | InvalidDescriptionException e) {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-load-failed", pluginName, e.getMessage());
                    plugin.getLogger().severe("Failed to load plugin " + pluginName + ": " + e.getMessage());
                    return;
                }
                
                if (loadedPlugin != null) {
                    pluginManager.enablePlugin(loadedPlugin);
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-loaded", loadedPlugin.getName());
                    plugin.getDatabaseManager().logAction("LOAD", "Plugin loaded via command", loadedPlugin.getName());
                } else {
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-load-failed", pluginName, "Unknown error");
                }
                
            } catch (Exception e) {
                plugin.getMessageFormatter().sendMessage(sender, "plugin-load-failed", pluginName, e.getMessage());
                plugin.getLogger().severe("Error loading plugin " + pluginName + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            File pluginsDir = new File("plugins");
            File[] files = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    if (fileName.toLowerCase().endsWith(".jar")) {
                        String pluginName = fileName.substring(0, fileName.length() - 4);
                        
                        // Only suggest plugins that aren't already loaded
                        Plugin loadedPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
                        if (loadedPlugin == null || !loadedPlugin.isEnabled()) {
                            if (pluginName.toLowerCase().startsWith(args[0].toLowerCase())) {
                                suggestions.add(pluginName);
                            }
                        }
                    }
                }
            }
            
            return suggestions;
        }
        
        return new ArrayList<>();
    }
}