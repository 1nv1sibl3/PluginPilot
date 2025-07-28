package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShowCommand extends SubCommand {
    
    public ShowCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "show";
    }
    
    @Override
    public String getDescription() {
        return "Show plugin information";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("info", "preview");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.view";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", "/pp show <plugin>");
            return;
        }
        
        String pluginName = args[0];
        plugin.getMessageFormatter().sendMessage(sender, "loading");
        
        // Redirect to info command for now
        InfoCommand infoCommand = new InfoCommand(plugin);
        infoCommand.execute(sender, args);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        // This will be handled by AsyncTabCompleter
        return new ArrayList<>();
    }
}