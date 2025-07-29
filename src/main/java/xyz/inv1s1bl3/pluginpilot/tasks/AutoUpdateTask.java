package xyz.inv1s1bl3.pluginpilot.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.io.File;
import java.util.List;

public class AutoUpdateTask extends BukkitRunnable {
    
    private final PluginPilot plugin;
    private int taskId = -1;
    
    public AutoUpdateTask(PluginPilot plugin) {
        this.plugin = plugin;
    }
    
    public void start(int intervalMinutes) {
        if (taskId != -1) {
            stop(); // Stop existing task
        }
        
        // Convert minutes to ticks (20 ticks = 1 second)
        long intervalTicks = intervalMinutes * 60L * 20L;
        
        taskId = runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks).getTaskId();
        plugin.getLogger().info("Auto-update task started with " + intervalMinutes + " minute interval");
    }
    
    public void stop() {
        if (taskId != -1) {
            cancel();
            taskId = -1;
            plugin.getLogger().info("Auto-update task stopped");
        }
    }
    
    @Override
    public void run() {
        try {
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("Running auto-update check...");
            }
            
            // Get list of installed plugins
            var installedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
            List<String> exceptionList = plugin.getConfig().getStringList("auto-update.exception-list");
            
            if (installedPlugins.isEmpty()) {
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().info("No managed plugins found for auto-update");
                }
                return;
            }
            
            int updatesFound = 0;
            
            for (String pluginName : installedPlugins) {
                // Skip plugins in exception list
                if (exceptionList.contains(pluginName)) {
                    if (plugin.getConfig().getBoolean("debug-mode", false)) {
                        plugin.getLogger().info("Skipping " + pluginName + " (in exception list)");
                    }
                    continue;
                }
                
                try {
                    // Get current version from database
                    String currentVersion = plugin.getDatabaseManager().getPluginVersion(pluginName);
                    if (currentVersion == null) {
                        continue;
                    }
                    
                    // Search for plugin
                    List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(pluginName);
                    PluginSearchResult targetPlugin = results.stream()
                            .filter(r -> r.getName().equalsIgnoreCase(pluginName))
                            .findFirst()
                            .orElse(null);
                    
                    if (targetPlugin == null) {
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().info("Plugin " + pluginName + " not found in repositories");
                        }
                        continue;
                    }
                    
                    // Get latest version
                    List<PluginVersion> versions = plugin.getSourceManager().getPluginVersions(targetPlugin);
                    if (versions.isEmpty()) {
                        continue;
                    }
                    
                    PluginVersion latestVersion = versions.get(0);
                    
                    // Compare versions
                    if (!currentVersion.equals(latestVersion.getVersion())) {
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().info("Update available for " + pluginName + ": " + 
                                currentVersion + " -> " + latestVersion.getVersion());
                        }
                        
                        // Create backup before update
                        if (plugin.getConfig().getBoolean("backups.auto-backup-before-update", true)) {
                            createBackup(pluginName);
                        }
                        
                        // Find the old plugin file to remove it after update
                        File pluginsDir = new File(plugin.getServer().getWorldContainer(), "plugins");
                        File oldPluginFile = findPluginFile(pluginsDir, pluginName);
                        
                        // Download and install update
                        boolean success = plugin.getSourceManager().downloadAndInstallPlugin(targetPlugin, latestVersion);
                        
                        if (success) {
                            // Remove old plugin file if it exists and is different from the new one
                            if (oldPluginFile != null && oldPluginFile.exists()) {
                                File newPluginFile = findPluginFile(pluginsDir, pluginName);
                                if (newPluginFile != null && !newPluginFile.getAbsolutePath().equals(oldPluginFile.getAbsolutePath())) {
                                    try {
                                        oldPluginFile.delete();
                                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                            plugin.getLogger().info("Removed old plugin file: " + oldPluginFile.getName());
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().warning("Failed to remove old plugin file: " + oldPluginFile.getName());
                                    }
                                }
                            }
                            
                            plugin.getDatabaseManager().updatePluginVersion(pluginName, latestVersion.getVersion());
                            plugin.getDatabaseManager().logAction("AUTO_UPDATE", 
                                "Auto-updated from " + currentVersion + " to " + latestVersion.getVersion(), pluginName);
                            
                            plugin.getLogger().info("Auto-updated " + pluginName + " from " + currentVersion + 
                                " to " + latestVersion.getVersion());
                            updatesFound++;
                            
                            // Notify online admins
                            notifyAdmins("§a[PluginPilot] Auto-updated §e" + pluginName + " §ato §e" + latestVersion.getVersion());
                        }
                    }
                    
                    if (plugin.getConfig().getBoolean("debug-mode", false)) {
                        plugin.getLogger().fine("Checked updates for " + pluginName);
                    }
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking updates for " + pluginName + ": " + e.getMessage());
                }
            }
            
            if (updatesFound > 0) {
                plugin.getLogger().info("Auto-update completed: " + updatesFound + " plugin(s) updated");
                notifyAdmins("§6[PluginPilot] Auto-update completed: §e" + updatesFound + " §6plugin(s) updated. Restart recommended.");
            } else {
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().info("Auto-update completed: All plugins are up to date");
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during auto-update: " + e.getMessage());
        }
    }
    
    private void createBackup(String pluginName) {
        try {
            File pluginsDir = new File(plugin.getServer().getWorldContainer(), "plugins");
            File pluginFile = findPluginFile(pluginsDir, pluginName);
            
            if (pluginFile != null && pluginFile.exists()) {
                File backupDir = new File(plugin.getDataFolder(), "backups");
                backupDir.mkdirs();
                
                String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                String backupName = pluginName + "-auto-backup-" + timestamp + ".jar";
                File backupFile = new File(backupDir, backupName);
                
                java.nio.file.Files.copy(pluginFile.toPath(), backupFile.toPath());
                
                String currentVersion = plugin.getDatabaseManager().getPluginVersion(pluginName);
                plugin.getDatabaseManager().saveBackup(pluginName, currentVersion, backupFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create auto-backup for " + pluginName + ": " + e.getMessage());
        }
    }
    
    private File findPluginFile(File pluginsDir, String pluginName) {
        File[] files = pluginsDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jar") && 
            name.toLowerCase().contains(pluginName.toLowerCase()));
        
        if (files != null && files.length > 0) {
            return files[0];
        }
        return null;
    }
    
    private void notifyAdmins(String message) {
        plugin.getServer().getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("pluginpilot.debug"))
                .forEach(player -> player.sendMessage(message));
    }
}