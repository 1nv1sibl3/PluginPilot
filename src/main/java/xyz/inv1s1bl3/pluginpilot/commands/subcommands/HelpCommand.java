package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HelpCommand extends SubCommand {
    
    private final Map<String, SubCommand> subCommands;
    
    public HelpCommand(PluginPilot plugin, Map<String, SubCommand> subCommands) {
        super(plugin);
        this.subCommands = subCommands;
    }
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Show help information";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("?", "h");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.use";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getMessageFormatter().sendRawMessage(sender, "help-header");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-show");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-install");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-update");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-updateall");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-remove");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-backup");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-restore");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-scan");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-list");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-detect");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-logs");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-sources");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}