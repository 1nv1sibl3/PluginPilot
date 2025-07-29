package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoveCommand extends SubCommand {
    
    public RemoveCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "remove";
    }
    
    @Override
    public String getDescription() {
        return "Remove a plugin";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("uninstall", "delete");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.uninstall";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp remove <plugin>");
            return;
        }
        
        String pluginName = args[0];
        
        try {
            // Check if plugin is installed
            if (!plugin.getDatabaseManager().isPluginInstalled(pluginName)) {
                plugin.getMessageFormatter().sendMessage(sender, "plugin-not-installed", pluginName);
                return;
            }
            
            // Get plugin file path
            String filePath = plugin.getDatabaseManager().getPluginFilePath(pluginName);
            
            // Create backup before removing
            String version = plugin.getDatabaseManager().getPluginVersion(pluginName);
            if (version != null && filePath != null) {
                plugin.getDatabaseManager().createBackupBeforeRemoval(pluginName, version, filePath);
            }
            
            // Remove from database
            plugin.getDatabaseManager().removePlugin(pluginName);
            
            // Delete the file if it exists
            if (filePath != null) {
                File pluginFile = new File(filePath);
                if (pluginFile.exists()) {
                    boolean deleted = pluginFile.delete();
                    if (deleted) {
                        sender.sendMessage("§aSuccessfully removed plugin file: §e" + pluginFile.getName());
                    } else {
                        // If file couldn't be deleted, try to mark it for deletion on server exit
                        try {
                            pluginFile.deleteOnExit();
                            sender.sendMessage("§ePlugin marked for deletion on server restart: §e" + pluginFile.getName());
                        } catch (Exception ex) {
                            sender.sendMessage("§ePlugin marked as removed, but could not delete the file. Server restart required.");
                            plugin.getLogger().warning("Could not delete plugin file: " + pluginFile.getAbsolutePath() + ". Error: " + ex.getMessage());
                        }
                    }
                } else {
                    sender.sendMessage("§ePlugin file not found at: §e" + filePath);
                    plugin.getLogger().warning("Plugin file not found at: " + filePath);
                }
            } else {
                sender.sendMessage("§ePlugin removed from database, but no file path was recorded.");
            }
            
            sender.sendMessage("§aSuccessfully removed plugin: §e" + pluginName);
            plugin.getLogger().info("Plugin removed: " + pluginName + " by " + sender.getName());
            
        } catch (Exception e) {
            sender.sendMessage("§cError removing plugin: " + e.getMessage());
            plugin.getLogger().severe("Error removing plugin " + pluginName + ": " + e.getMessage());
        }
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