package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DetectCommand extends SubCommand {
    
    public DetectCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "detect";
    }
    
    @Override
    public String getDescription() {
        return "Detect unmanaged plugins";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("find", "discover");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.view";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6Detecting unmanaged plugins...");
        
        // Implementation would detect plugins not managed by PluginPilot
        sender.sendMessage("§7This feature is under development");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}