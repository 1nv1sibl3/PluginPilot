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
        
        // Plugin Installation & Management
        plugin.getMessageFormatter().sendRawMessage(sender, "help-category-install");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-show");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-download");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-install");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-update");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-updateall");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-remove");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-delete");
        
        // Plugin Loading & Unloading
        plugin.getMessageFormatter().sendRawMessage(sender, "help-category-loading");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-load");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-unload");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-reload");
        
        // Backup & Security
        plugin.getMessageFormatter().sendRawMessage(sender, "help-category-backup");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-backup");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-restore");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-scan");
        
        // Information & Discovery
        plugin.getMessageFormatter().sendRawMessage(sender, "help-category-info");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-list");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-detect");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-discover");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-info");
        
        // System & Configuration
        plugin.getMessageFormatter().sendRawMessage(sender, "help-category-system");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-logs");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-sources");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-refreshconfig");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-clearcache");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-debug");
        plugin.getMessageFormatter().sendRawMessage(sender, "help-import");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}