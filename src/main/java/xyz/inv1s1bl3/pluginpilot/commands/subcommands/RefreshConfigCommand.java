package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RefreshConfigCommand extends SubCommand {
    
    public RefreshConfigCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "refreshconfig";
    }
    
    @Override
    public String getDescription() {
        return "Reload the plugin configuration";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("reloadconfig", "configreload");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.reload";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(plugin.getMessageFormatter().colorize("&6Reloading PluginPilot configuration..."));
        
        try {
            // Call the reload method in the main plugin class
            plugin.reload();
            
            // Inform the user about the active sources after reload
            List<String> enabledSources = plugin.getSourceManager().getEnabledSources();
            
            sender.sendMessage(plugin.getMessageFormatter().colorize("&aConfiguration reloaded successfully!"));
            sender.sendMessage(plugin.getMessageFormatter().colorize("&7Active plugin sources: &f" + 
                    String.join(", ", enabledSources)));
            
            // Show debug info if debug mode is enabled
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7Debug: Config values:"));
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7- SpigotMC: " + 
                        (plugin.getConfig().getBoolean("repositories.0.enabled", false) ? "&aEnabled" : "&cDisabled")));
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7- Modrinth: " + 
                        (plugin.getConfig().getBoolean("repositories.1.enabled", true) ? "&aEnabled" : "&cDisabled")));
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7- Polymart: " + 
                        (plugin.getConfig().getBoolean("repositories.2.enabled", false) ? "&aEnabled" : "&cDisabled")));
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7- Hangar: " + 
                        (plugin.getConfig().getBoolean("repositories.3.enabled", true) ? "&aEnabled" : "&cDisabled")));
            }
            
            // Log the action
            plugin.getDatabaseManager().logAction("CONFIG_RELOAD", "Configuration reloaded via command", null);
            
        } catch (Exception e) {
            sender.sendMessage(plugin.getMessageFormatter().colorize("&cError reloading configuration: " + e.getMessage()));
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // No tab completion options for reload command
        return new ArrayList<>();
    }
}