package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UpdateAllCommand extends SubCommand {
    
    public UpdateAllCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "updateall";
    }
    
    @Override
    public String getDescription() {
        return "Update all plugins";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("upgradeall", "update-all");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.update";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6Starting update check for all managed plugins...");
        
        CompletableFuture.runAsync(() -> {
            try {
                List<String> installedPlugins = plugin.getDatabaseManager().getInstalledPluginNames();
                
                if (installedPlugins.isEmpty()) {
                    sender.sendMessage("§cNo managed plugins found to update.");
                    return;
                }
                
                int updatesFound = 0;
                int totalChecked = 0;
                
                for (String pluginName : installedPlugins) {
                    try {
                        totalChecked++;
                        sender.sendMessage("§7Checking updates for §e" + pluginName + "§7... (" + totalChecked + "/" + installedPlugins.size() + ")");
                        
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
                                plugin.getDatabaseManager().logAction("UPDATE", 
                                    "Updated from " + currentVersion + " to " + latestVersion.getVersion(), pluginName);
                                
                                sender.sendMessage("§a✓ Updated §e" + pluginName + " §afrom §7" + currentVersion + " §ato §e" + latestVersion.getVersion());
                                updatesFound++;
                            } else {
                                sender.sendMessage("§c✗ Failed to update §e" + pluginName);
                            }
                        }
                        
                    } catch (Exception e) {
                        sender.sendMessage("§c✗ Error updating §e" + pluginName + "§c: " + e.getMessage());
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().warning("Update error for " + pluginName + ": " + e.getMessage());
                        }
                    }
                }
                
                sender.sendMessage("§6=== Update Summary ===");
                sender.sendMessage("§7Plugins checked: §e" + totalChecked);
                sender.sendMessage("§7Updates applied: §a" + updatesFound);
                
                if (updatesFound > 0) {
                    sender.sendMessage("§cServer restart recommended to load updated plugins.");
                } else {
                    sender.sendMessage("§aAll plugins are up to date!");
                }
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error during update all: " + e.getMessage());
                sender.sendMessage("§cError during update process: " + e.getMessage());
            }
        });
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
                String backupName = pluginName + "-pre-update-" + timestamp + ".jar";
                File backupFile = new File(backupDir, backupName);
                
                java.nio.file.Files.copy(pluginFile.toPath(), backupFile.toPath());
                
                String currentVersion = "unknown";
                org.bukkit.plugin.Plugin loadedPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
                if (loadedPlugin != null) {
                    currentVersion = loadedPlugin.getDescription().getVersion();
                }
                
                plugin.getDatabaseManager().saveBackup(pluginName, currentVersion, backupFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create backup for " + pluginName + ": " + e.getMessage());
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}