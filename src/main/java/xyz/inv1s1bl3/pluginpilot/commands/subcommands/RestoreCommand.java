package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.BackupRecord;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RestoreCommand extends SubCommand {
    
    public RestoreCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "restore";
    }
    
    @Override
    public String getDescription() {
        return "Restore a plugin from backup";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("rollback");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.restore";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp restore <plugin> [version]");
            return;
        }
        
        String pluginName = args[0];
        String version = args.length > 1 ? args[1] : null;
        
        CompletableFuture.runAsync(() -> {
            try {
                // Get available backups
                List<BackupRecord> backups = plugin.getDatabaseManager().getPluginBackups(pluginName);
                
                if (backups.isEmpty()) {
                    sender.sendMessage("§cNo backups found for plugin §e" + pluginName + "§c!");
                    return;
                }
                
                // Select backup
                BackupRecord targetBackup = null;
                if (version != null) {
                    targetBackup = backups.stream()
                            .filter(b -> b.getVersion().equals(version))
                            .findFirst()
                            .orElse(null);
                    
                    if (targetBackup == null) {
                        sender.sendMessage("§cBackup version §e" + version + " §cnot found for " + pluginName + "!");
                        return;
                    }
                } else {
                    // Use latest backup
                    targetBackup = backups.get(0);
                }
                
                // Check if backup file exists
                File backupFile = new File(targetBackup.getFilePath());
                if (!backupFile.exists()) {
                    sender.sendMessage("§cBackup file not found: §e" + backupFile.getName());
                    return;
                }
                
                // Restore backup
                File pluginsDir = new File(plugin.getServer().getWorldContainer(), "plugins");
                File currentPluginFile = findPluginFile(pluginsDir, pluginName);
                
                if (currentPluginFile != null && currentPluginFile.exists()) {
                    // Create backup of current version before restore
                    String timestamp = java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                    File preRestoreBackup = new File(pluginsDir, pluginName + "-pre-restore-" + timestamp + ".jar");
                    java.nio.file.Files.copy(currentPluginFile.toPath(), preRestoreBackup.toPath());
                    
                    // Delete current file
                    currentPluginFile.delete();
                }
                
                // Copy backup to plugins folder
                String restoredFileName = pluginName + ".jar";
                File restoredFile = new File(pluginsDir, restoredFileName);
                java.nio.file.Files.copy(backupFile.toPath(), restoredFile.toPath());
                
                // Update database
                plugin.getDatabaseManager().updatePluginVersion(pluginName, targetBackup.getVersion());
                
                // Log action
                plugin.getDatabaseManager().logAction("RESTORE", 
                    "Restored " + pluginName + " to version " + targetBackup.getVersion(), pluginName);
                
                sender.sendMessage("§aSuccessfully restored §e" + pluginName + " §ato version §e" + targetBackup.getVersion() + "§a!");
                sender.sendMessage("§cServer restart required to load the restored plugin.");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error restoring plugin " + pluginName + ": " + e.getMessage());
                sender.sendMessage("§cError restoring plugin: " + e.getMessage());
            }
        });
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
        if (args.length == 1) {
            try {
                return plugin.getDatabaseManager().getInstalledPluginNames().stream()
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