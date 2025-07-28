package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClearCacheCommand extends SubCommand {
    
    public ClearCacheCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "clearcache";
    }
    
    @Override
    public String getDescription() {
        return "Clear all cached plugin data";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("cc", "clear-cache");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.debug";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        try {
            plugin.getDatabaseManager().clearCache();
            sender.sendMessage("§aSuccessfully cleared all cached plugin data!");
            plugin.getLogger().info("Cache cleared by " + sender.getName());
        } catch (Exception e) {
            sender.sendMessage("§cFailed to clear cache: " + e.getMessage());
            plugin.getLogger().severe("Error clearing cache: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}