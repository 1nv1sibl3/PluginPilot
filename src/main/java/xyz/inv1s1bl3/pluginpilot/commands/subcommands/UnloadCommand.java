package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class UnloadCommand extends SubCommand {
    
    public UnloadCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "unload";
    }
    
    @Override
    public String getDescription() {
        return "Unload a plugin without removing its files";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("disable");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.unload";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "pp unload <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        // Check if plugin exists and is loaded
        Plugin targetPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
        if (targetPlugin == null) {
            plugin.getMessageFormatter().sendMessage(sender, "plugin-not-found", pluginName);
            return;
        }
        
        if (!targetPlugin.isEnabled()) {
            plugin.getMessageFormatter().sendMessage(sender, "plugin-not-loaded", pluginName);
            return;
        }
        
        // Prevent unloading PluginPilot itself
        if (targetPlugin.getName().equals("PluginPilot")) {
            sender.sendMessage(plugin.getMessageFormatter().colorize("&cYou cannot unload PluginPilot while it's running!"));
            return;
        }
        
        sender.sendMessage(plugin.getMessageFormatter().colorize("&7Attempting to unload plugin &e" + targetPlugin.getName() + "&7..."));
        
        CompletableFuture.runAsync(() -> {
            try {
                // First disable the plugin
                PluginManager pluginManager = plugin.getServer().getPluginManager();
                pluginManager.disablePlugin(targetPlugin);
                
                // Use reflection to remove the plugin from the PluginManager's lookup maps
                // This is necessary for a complete unload
                try {
                    Field lookupNamesField = pluginManager.getClass().getDeclaredField("lookupNames");
                    lookupNamesField.setAccessible(true);
                    Map<String, Plugin> lookupNames = (Map<String, Plugin>) lookupNamesField.get(pluginManager);
                    lookupNames.remove(targetPlugin.getName().toLowerCase());
                    
                    Field pluginsField = pluginManager.getClass().getDeclaredField("plugins");
                    pluginsField.setAccessible(true);
                    List<Plugin> plugins = (List<Plugin>) pluginsField.get(pluginManager);
                    plugins.remove(targetPlugin);
                    
                    // Attempt to unload plugin classes to prevent memory leaks
                    if (targetPlugin instanceof JavaPlugin) {
                        Field classLoaderField = JavaPlugin.class.getDeclaredField("classLoader");
                        classLoaderField.setAccessible(true);
                        ClassLoader pluginClassLoader = (ClassLoader) classLoaderField.get(targetPlugin);
                        
                        // Close the classloader if it has a close method (Java 9+)
                        try {
                            pluginClassLoader.getClass().getMethod("close").invoke(pluginClassLoader);
                        } catch (NoSuchMethodException ignored) {
                            // Java 8 or earlier doesn't have ClassLoader.close()
                        }
                    }
                    
                    plugin.getMessageFormatter().sendMessage(sender, "plugin-unloaded", targetPlugin.getName());
                    plugin.getDatabaseManager().logAction("UNLOAD", "Plugin unloaded via command", targetPlugin.getName());
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not fully unload plugin (partial unload only): " + e.getMessage());
                    sender.sendMessage(plugin.getMessageFormatter().colorize("&ePlugin disabled, but could not be fully unloaded: " + e.getMessage()));
                    sender.sendMessage(plugin.getMessageFormatter().colorize("&eA server restart may be required for a complete unload."));
                }
                
                // Suggest running garbage collection
                System.gc();
                
            } catch (Exception e) {
                plugin.getMessageFormatter().sendMessage(sender, "plugin-unload-failed", targetPlugin.getName(), e.getMessage());
                plugin.getLogger().severe("Error unloading plugin " + targetPlugin.getName() + ": " + e.getMessage());
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