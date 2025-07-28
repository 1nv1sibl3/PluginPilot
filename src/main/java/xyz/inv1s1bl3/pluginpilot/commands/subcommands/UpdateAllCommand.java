package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
        plugin.getMessageFormatter().sendMessage(sender, "loading");
        
        // Implementation would update all managed plugins
        sender.sendMessage("§6Updating all managed plugins...");
        sender.sendMessage("§7This feature is under development");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}