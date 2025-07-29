package xyz.inv1s1bl3.pluginpilot.tasks;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.File;

public class PluginDetectionTask extends BukkitRunnable {
    
    private final PluginPilot plugin;
    private Set<String> lastKnownPlugins;
    private int taskId = -1;
    
    public PluginDetectionTask(PluginPilot plugin) {
        this.plugin = plugin;
        this.lastKnownPlugins = new HashSet<>();
        updateKnownPlugins();
    }
    
    public void start(int intervalMinutes) {
        if (taskId != -1) {
            stop();
        }
        
        long intervalTicks = intervalMinutes * 60L * 20L;
        taskId = runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks).getTaskId();
        
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            plugin.getLogger().info("Plugin detection task started with " + intervalMinutes + " minute interval");
        }
    }
    
    public void stop() {
        if (taskId != -1) {
            cancel();
            taskId = -1;
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("Plugin detection task stopped");
            }
        }
    }
    
    @Override
    public void run() {
        try {
            Set<String> currentPlugins = getCurrentPlugins();
            
            // Find newly added plugins
            Set<String> newPlugins = new HashSet<>(currentPlugins);
            newPlugins.removeAll(lastKnownPlugins);
            
            // Find removed plugins
            Set<String> removedPlugins = new HashSet<>(lastKnownPlugins);
            removedPlugins.removeAll(currentPlugins);
            
            // Process new plugins
            for (String pluginName : newPlugins) {
                if (pluginName.equals("PluginPilot")) continue;
                
                try {
                    // Check if already managed
                    if (!plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                        handleNewPlugin(pluginName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing new plugin " + pluginName + ": " + e.getMessage());
                }
            }
            
            // Process removed plugins
            for (String pluginName : removedPlugins) {
                if (pluginName.equals("PluginPilot")) continue;
                
                try {
                    // Check if was managed
                    if (plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                        handleRemovedPlugin(pluginName);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error processing removed plugin " + pluginName + ": " + e.getMessage());
                }
            }
            
            // Update known plugins
            lastKnownPlugins = currentPlugins;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during plugin detection: " + e.getMessage());
        }
    }
    
    private Set<String> getCurrentPlugins() {
        Set<String> plugins = new HashSet<>();
        
        // Get currently loaded plugins
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            plugins.add(p.getName());
        }
        
        // Also check for JAR files in the plugins directory that might not be loaded yet
        File pluginsDir = new File("plugins");
        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    try {
                        // Try to extract plugin name from JAR file
                        String pluginName = extractPluginNameFromJar(jarFile);
                        if (pluginName != null && !pluginName.isEmpty()) {
                            plugins.add(pluginName);
                        }
                    } catch (Exception e) {
                        // Ignore errors when trying to read JAR files
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().warning("Could not read plugin info from JAR: " + jarFile.getName());
                        }
                    }
                }
            }
        }
        
        return plugins;
    }
    
    private void updateKnownPlugins() {
        lastKnownPlugins = getCurrentPlugins();
    }
    
    private void handleNewPlugin(String pluginName) {
        try {
            Plugin loadedPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
            if (loadedPlugin == null) return;
            
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("Detected new plugin: " + pluginName);
            }
            
            // Try to find plugin in repositories
            List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(pluginName);
            PluginSearchResult matchedPlugin = results.stream()
                    .filter(r -> r.getName().equalsIgnoreCase(pluginName))
                    .findFirst()
                    .orElse(null);
            
            if (matchedPlugin != null) {
                // Found in repositories - add to management
                PluginVersion currentVersion = new PluginVersion();
                currentVersion.setVersion(loadedPlugin.getDescription().getVersion());
                currentVersion.setId("auto-detected-" + System.currentTimeMillis());
                
                plugin.getDatabaseManager().saveInstalledPlugin(matchedPlugin, currentVersion);
                plugin.getDatabaseManager().logAction("AUTO_DETECT", "Auto-detected and added to management", pluginName);
                
                plugin.getLogger().info("Auto-detected plugin " + pluginName + " and added to PluginPilot management");
                
                // Notify admins if enabled
                if (plugin.getConfig().getBoolean("auto-detection.notify-admins", true)) {
                    notifyAdmins("§a[PluginPilot] Auto-detected new plugin: §e" + pluginName + " §a(now managed)");
                }
                
            } else {
                // Not found in repositories - add as custom
                PluginSearchResult customPlugin = new PluginSearchResult();
                customPlugin.setId("custom-" + pluginName.toLowerCase());
                customPlugin.setName(loadedPlugin.getName());
                customPlugin.setDescription(loadedPlugin.getDescription().getDescription());
                customPlugin.setAuthor(String.join(", ", loadedPlugin.getDescription().getAuthors()));
                customPlugin.setSourceType("custom");
                customPlugin.setSourceId("auto-detected");
                
                PluginVersion currentVersion = new PluginVersion();
                currentVersion.setVersion(loadedPlugin.getDescription().getVersion());
                currentVersion.setId("auto-detected-" + System.currentTimeMillis());
                
                plugin.getDatabaseManager().saveInstalledPlugin(customPlugin, currentVersion);
                plugin.getDatabaseManager().logAction("AUTO_DETECT", "Auto-detected as custom plugin", pluginName);
                
                plugin.getLogger().info("Auto-detected custom plugin " + pluginName + " and added to PluginPilot management");
                
                // Notify admins if enabled
                if (plugin.getConfig().getBoolean("auto-detection.notify-admins", true)) {
                    notifyAdmins("§6[PluginPilot] Auto-detected custom plugin: §e" + pluginName + " §6(limited management)");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling new plugin " + pluginName + ": " + e.getMessage());
        }
    }
    
    private void handleRemovedPlugin(String pluginName) {
        try {
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("Detected removed plugin: " + pluginName);
            }
            
            // Update database to mark as removed
            plugin.getDatabaseManager().removePlugin(pluginName);
            plugin.getDatabaseManager().logAction("AUTO_DETECT", "Auto-detected plugin removal", pluginName);
            
            plugin.getLogger().info("Auto-detected removal of plugin " + pluginName + " and updated database");
            
            // Notify admins if enabled
            if (plugin.getConfig().getBoolean("auto-detection.notify-admins", true)) {
                notifyAdmins("§c[PluginPilot] Auto-detected plugin removal: §e" + pluginName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error handling removed plugin " + pluginName + ": " + e.getMessage());
        }
    }
    
    private void notifyAdmins(String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("pluginpilot.debug"))
                .forEach(player -> player.sendMessage(message));
    }
    
    /**
     * Extracts the plugin name from a JAR file by reading the plugin.yml
     * 
     * @param jarFile The JAR file to extract the plugin name from
     * @return The plugin name, or null if it couldn't be extracted
     */
    private String extractPluginNameFromJar(File jarFile) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry != null) {
                try (java.io.InputStream input = jar.getInputStream(entry)) {
                    org.bukkit.configuration.file.YamlConfiguration config = 
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(input));
                    return config.getString("name");
                }
            }
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().warning("Error reading plugin.yml from " + jarFile.getName() + ": " + e.getMessage());
            }
        }
        
        // If we couldn't extract the name from plugin.yml, use the JAR filename without extension
        String fileName = jarFile.getName();
        if (fileName.toLowerCase().endsWith(".jar")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        
        return null;
    }
}