package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogsCommand extends SubCommand {
    
    public LogsCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "logs";
    }
    
    @Override
    public String getDescription() {
        return "View or upload plugin logs";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("log", "debug");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.debug";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6Plugin logs:");
        sender.sendMessage("§7This feature is under development");
        sender.sendMessage("§7Future features will include:");
        sender.sendMessage("§7- View recent PluginPilot logs");
        sender.sendMessage("§7- Upload logs to pastebin services");
        sender.sendMessage("§7- Filter logs by plugin or time");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}