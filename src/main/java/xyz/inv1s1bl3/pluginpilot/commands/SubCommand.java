package xyz.inv1s1bl3.pluginpilot.commands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;

import java.util.List;

public abstract class SubCommand {
    
    protected final PluginPilot plugin;
    
    public SubCommand(PluginPilot plugin) {
        this.plugin = plugin;
    }
    
    public abstract String getName();
    
    public abstract String getDescription();
    
    public abstract List<String> getAliases();
    
    public abstract String getPermission();
    
    public abstract void execute(CommandSender sender, String[] args);
    
    public abstract List<String> onTabComplete(CommandSender sender, String[] args);
    
    public boolean hasPermission(CommandSender sender) {
        String permission = getPermission();
        return permission == null || sender.hasPermission(permission);
    }
}