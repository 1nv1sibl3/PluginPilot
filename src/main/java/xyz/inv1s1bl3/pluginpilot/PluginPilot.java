package xyz.inv1s1bl3.pluginpilot;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.inv1s1bl3.pluginpilot.commands.PPilotCommand;
import xyz.inv1s1bl3.pluginpilot.persistence.DatabaseManager;
import xyz.inv1s1bl3.pluginpilot.integration.PluginSourceManager;
import xyz.inv1s1bl3.pluginpilot.util.MessageFormatter;
import xyz.inv1s1bl3.pluginpilot.tasks.AutoUpdateTask;
import xyz.inv1s1bl3.pluginpilot.tasks.PluginDetectionTask;

import java.util.logging.Level;

public final class PluginPilot extends JavaPlugin {
    
    private static PluginPilot instance;
    private DatabaseManager databaseManager;
    private PluginSourceManager sourceManager;
    private MessageFormatter messageFormatter;
    private AutoUpdateTask autoUpdateTask;
    private PluginDetectionTask detectionTask;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize components
        initializeComponents();
        
        // Register commands
        registerCommands();
        
        // Start auto-update task if enabled
        startAutoUpdateTask();
        
        // Start plugin detection task if enabled
        startDetectionTask();
        
        // Display aesthetic enable message with GitHub attribution
        getLogger().info("\n" +
                "  ╔═══════════════════════════════════════════════╗\n" +
                "  ║             ✦ PluginPilot v" + getDescription().getVersion() + " ✦            ║\n" +
                "  ║                                               ║\n" +
                "  ║  Successfully enabled and ready to serve!     ║\n" +
                "  ║  Created by: inv1s1bl3 (GitHub: @1nv1sibl3)   ║\n" +
                "  ╚═══════════════════════════════════════════════╝");
    }
    
    @Override
    public void onDisable() {
        // Stop auto-update task
        if (autoUpdateTask != null) {
            autoUpdateTask.stop();
        }
        
        // Stop detection task
        if (detectionTask != null) {
            detectionTask.stop();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.close();
        }
        
        getLogger().info("PluginPilot has been disabled.");
    }
    
    private void initializeComponents() {
        try {
            // Initialize message formatter
            messageFormatter = new MessageFormatter(this);
            
            // Initialize database
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            
            // Initialize plugin source manager
            sourceManager = new PluginSourceManager(this);
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize PluginPilot components", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }
    
    private void registerCommands() {
        PPilotCommand commandHandler = new PPilotCommand(this);
        getCommand("pluginpilot").setExecutor(commandHandler);
        getCommand("pluginpilot").setTabCompleter(commandHandler);
    }
    
    private void startAutoUpdateTask() {
        if (getConfig().getBoolean("auto-update.enabled", true)) {
            int intervalMinutes = getConfig().getInt("auto-update.check-interval-minutes", 60);
            autoUpdateTask = new AutoUpdateTask(this);
            autoUpdateTask.start(intervalMinutes);
        }
    }
    
    private void startDetectionTask() {
        if (getConfig().getBoolean("auto-detection.enabled", true)) {
            int intervalMinutes = getConfig().getInt("auto-detection.check-interval-minutes", 1);
            detectionTask = new PluginDetectionTask(this);
            detectionTask.start(intervalMinutes);
            
            if (getConfig().getBoolean("debug-mode", false)) {
                getLogger().info("Plugin detection task started with " + intervalMinutes + " minute interval");
            }
        }
    }
    
    // Getters for components
    public static PluginPilot getInstance() {
        return instance;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public PluginSourceManager getSourceManager() {
        return sourceManager;
    }
    
    public MessageFormatter getMessageFormatter() {
        return messageFormatter;
    }
    
    /**
     * Reloads the plugin configuration and refreshes all components.
     * This allows changes to config.yml to take effect without requiring a server restart.
     */
    public void reload() {
        getLogger().info("Reloading PluginPilot configuration...");
        
        // Reload config.yml
        reloadConfig();
        
        // Refresh sources based on new config
        if (sourceManager != null) {
            sourceManager.refreshSources();
        }
        
        // Restart tasks with new intervals if needed
        if (autoUpdateTask != null && getConfig().getBoolean("auto-update.enabled", true)) {
            autoUpdateTask.stop();
            int intervalMinutes = getConfig().getInt("auto-update.check-interval-minutes", 60);
            autoUpdateTask.start(intervalMinutes);
        }
        
        if (detectionTask != null && getConfig().getBoolean("auto-detection.enabled", true)) {
            detectionTask.stop();
            int intervalMinutes = getConfig().getInt("auto-detection.check-interval-minutes", 1);
            detectionTask.start(intervalMinutes);
        }
        
        getLogger().info("PluginPilot configuration reloaded successfully!");
    }
}