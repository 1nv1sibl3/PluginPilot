package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DeleteCommand extends SubCommand {
    
    public DeleteCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "delete";
    }
    
    @Override
    public String getDescription() {
        return "Permanently delete a plugin and its data";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("del", "purge");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.uninstall";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp delete <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        CompletableFuture.runAsync(() -> {
            try {
                // Check if plugin is managed by PluginPilot
                if (!plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                    sender.sendMessage("§cPlugin §e" + pluginName + " §cis not managed by PluginPilot!");
                    return;
                }
                
                // Create backup before deletion
                try {
                    File pluginsDir = new File(plugin.getServer().getWorldContainer(), "plugins");
                    File pluginFile = findPluginFile(pluginsDir, pluginName);
                    
                    if (pluginFile != null && pluginFile.exists()) {
                        // Create backup
                        File backupDir = new File(plugin.getDataFolder(), "backups");
                        backupDir.mkdirs();
                        
                        String backupName = pluginName + "-" + System.currentTimeMillis() + ".jar";
                        File backupFile = new File(backupDir, backupName);
                        
                        java.nio.file.Files.copy(pluginFile.toPath(), backupFile.toPath());
                        
                        // Save backup record
                        plugin.getDatabaseManager().saveBackup(pluginName, "pre-delete", backupFile.getAbsolutePath());
                        
                        // Delete the plugin file
                        if (pluginFile.delete()) {
                            sender.sendMessage("§aPlugin file deleted: §e" + pluginFile.getName());
                        } else {
                            sender.sendMessage("§cFailed to delete plugin file: §e" + pluginFile.getName());
                        }
                    } else {
                        sender.sendMessage("§cPlugin file not found for: §e" + pluginName);
                    }
                    
                } catch (Exception e) {
                    sender.sendMessage("§cError during plugin deletion: " + e.getMessage());
                    plugin.getLogger().severe("Error deleting plugin " + pluginName + ": " + e.getMessage());
                    return;
                }
                
                // Remove from database
                plugin.getDatabaseManager().removePlugin(pluginName);
                
                // Log the action
                plugin.getDatabaseManager().logAction("DELETE", "Plugin deleted: " + pluginName, pluginName);
                
                sender.sendMessage("§aSuccessfully deleted plugin: §e" + pluginName);
                sender.sendMessage("§7A backup was created before deletion.");
                sender.sendMessage("§cNote: Server restart required to fully unload the plugin.");
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error deleting plugin " + pluginName + ": " + e.getMessage());
                sender.sendMessage("§cError deleting plugin: " + e.getMessage());
            }
        });
    }
    
    private File findPluginFile(File pluginsDir, String pluginName) {
        File[] files = pluginsDir.listFiles((dir, name) -> 
            name.toLowerCase().endsWith(".jar") && 
            name.toLowerCase().contains(pluginName.toLowerCase()));
        
        if (files != null && files.length > 0) {
            // Return the first match
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