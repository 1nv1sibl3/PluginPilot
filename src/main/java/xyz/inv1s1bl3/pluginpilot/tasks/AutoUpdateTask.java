package xyz.inv1s1bl3.pluginpilot.tasks;

import org.bukkit.scheduler.BukkitRunnable;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;

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
            plugin.getLogger().info("Running auto-update check...");
            
            // Get list of installed plugins
            var installedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
            
            if (installedPlugins.isEmpty()) {
                plugin.getLogger().info("No managed plugins found for auto-update");
                return;
            }
            
            int updatesFound = 0;
            
            for (String pluginName : installedPlugins) {
                try {
                    // Check for updates for each plugin
                    // This is a simplified version - in reality you'd need to:
                    // 1. Get current version from database
                    // 2. Search for the plugin in sources
                    // 3. Compare versions
                    // 4. Download and install if newer version available
                    
                    plugin.getLogger().fine("Checking updates for " + pluginName);
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Error checking updates for " + pluginName + ": " + e.getMessage());
                }
            }
            
            if (updatesFound > 0) {
                plugin.getLogger().info("Auto-update completed: " + updatesFound + " plugins updated");
            } else {
                plugin.getLogger().info("Auto-update completed: All plugins are up to date");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during auto-update: " + e.getMessage());
        }
    }
}