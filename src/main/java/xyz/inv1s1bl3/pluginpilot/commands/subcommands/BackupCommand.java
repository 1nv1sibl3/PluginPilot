package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BackupCommand extends SubCommand {
    
    public BackupCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "backup";
    }
    
    @Override
    public String getDescription() {
        return "Backup a plugin";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("bak");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.backup";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp backup <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        CompletableFuture.runAsync(() -> {
            try {
                // Check if plugin is managed
                if (!plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                    sender.sendMessage("§cPlugin §e" + pluginName + " §cis not managed by PluginPilot!");
                    return;
                }
                
                // Find plugin file
                File pluginsDir = new File(plugin.getServer().getWorldContainer(), "plugins");
                File pluginFile = findPluginFile(pluginsDir, pluginName);
                
                if (pluginFile == null || !pluginFile.exists()) {
                    sender.sendMessage("§cPlugin file not found for: §e" + pluginName);
                    return;
                }
                
                // Create backup
                File backupDir = new File(plugin.getDataFolder(), "backups");
                backupDir.mkdirs();
                
                String timestamp = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                String backupName = pluginName + "-" + timestamp + ".jar";
                File backupFile = new File(backupDir, backupName);
                
                // Copy file
                java.nio.file.Files.copy(pluginFile.toPath(), backupFile.toPath());
                
                // Get current version
                String currentVersion = "unknown";
                org.bukkit.plugin.Plugin loadedPlugin = plugin.getServer().getPluginManager().getPlugin(pluginName);
                if (loadedPlugin != null) {
                    currentVersion = loadedPlugin.getDescription().getVersion();
                }
                
                // Save backup record
                plugin.getDatabaseManager().saveBackup(pluginName, currentVersion, backupFile.getAbsolutePath());
                
                // Log action
                plugin.getDatabaseManager().logAction("BACKUP", "Manual backup created for " + pluginName, pluginName);
                
                sender.sendMessage("§aSuccessfully created backup for §e" + pluginName + "§a!");
                sender.sendMessage("§7Backup saved as: §e" + backupName);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error creating backup for " + pluginName + ": " + e.getMessage());
                sender.sendMessage("§cError creating backup: " + e.getMessage());
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