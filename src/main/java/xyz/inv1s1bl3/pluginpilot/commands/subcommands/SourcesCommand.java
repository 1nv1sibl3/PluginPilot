package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SourcesCommand extends SubCommand {
    
    public SourcesCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "sources";
    }
    
    @Override
    public String getDescription() {
        return "Manage plugin sources";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("repos", "repositories");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.sources";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        // If argument is "refresh", refresh the sources
        if (args.length > 0 && args[0].equalsIgnoreCase("refresh")) {
            plugin.getSourceManager().refreshSources();
            sender.sendMessage("§a✓ Plugin sources refreshed from config.yml");
        }
        
        sender.sendMessage("§6=== Plugin Sources ===");
        
        // Get enabled sources from the plugin source manager
        List<String> enabledSources = plugin.getSourceManager().getEnabledSources();
        
        // Check each repository in config
        boolean spigotEnabled = plugin.getConfig().getBoolean("repositories.0.enabled", false);
        boolean modrinthEnabled = plugin.getConfig().getBoolean("repositories.1.enabled", true);
        boolean polymartEnabled = plugin.getConfig().getBoolean("repositories.2.enabled", false);
        boolean hangarEnabled = plugin.getConfig().getBoolean("repositories.3.enabled", true);
        
        // Display status of each source with development status
        sender.sendMessage(spigotEnabled ? 
            "§a✓ SpigotMC §7- Enabled" : 
            "§c✗ SpigotMC §7- Disabled §8(Under Development)");
            
        sender.sendMessage(modrinthEnabled ? 
            "§a✓ Modrinth §7- Enabled" : 
            "§c✗ Modrinth §7- Disabled");
            
        sender.sendMessage(polymartEnabled ? 
            "§a✓ Polymart §7- Enabled" : 
            "§c✗ Polymart §7- Disabled §8(Under Development)");
            
        sender.sendMessage(hangarEnabled ? 
            "§a✓ Hangar §7- Enabled" : 
            "§c✗ Hangar §7- Disabled");
        
        sender.sendMessage("§7");
        sender.sendMessage("§7To change enabled sources, edit config.yml and use §f/ppilot sources refresh");
        
        // Debug information
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            sender.sendMessage("§7");
            sender.sendMessage("§7Debug: Active sources from SourceManager: " + String.join(", ", enabledSources));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("refresh", "list").stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return new ArrayList<>();
    }
}