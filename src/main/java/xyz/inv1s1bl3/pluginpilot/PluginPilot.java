package xyz.inv1s1bl3.pluginpilot;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.inv1s1bl3.pluginpilot.commands.PPilotCommand;
import xyz.inv1s1bl3.pluginpilot.persistence.DatabaseManager;
import xyz.inv1s1bl3.pluginpilot.integration.PluginSourceManager;
import xyz.inv1s1bl3.pluginpilot.util.MessageFormatter;
import xyz.inv1s1bl3.pluginpilot.tasks.AutoUpdateTask;

import java.util.logging.Level;

public final class PluginPilot extends JavaPlugin {
    
    private static PluginPilot instance;
    private DatabaseManager databaseManager;
    private PluginSourceManager sourceManager;
    private MessageFormatter messageFormatter;
    private AutoUpdateTask autoUpdateTask;
    
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
        
        getLogger().info("PluginPilot has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // Stop auto-update task
        if (autoUpdateTask != null) {
            autoUpdateTask.stop();
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
}