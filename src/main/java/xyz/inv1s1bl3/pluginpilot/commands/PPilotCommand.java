package xyz.inv1s1bl3.pluginpilot.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.subcommands.*;
import xyz.inv1s1bl3.pluginpilot.util.AsyncTabCompleter;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PPilotCommand implements CommandExecutor, TabCompleter {
    
    private final PluginPilot plugin;
    private final Map<String, SubCommand> subCommands;
    private final AsyncTabCompleter asyncTabCompleter;
    
    public PPilotCommand(PluginPilot plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        this.asyncTabCompleter = new AsyncTabCompleter(plugin);
        
        registerSubCommands();
    }
    
    private void registerSubCommands() {
        // Register all subcommands
        registerSubCommand(new ShowCommand(plugin));
        registerSubCommand(new InstallCommand(plugin));
        registerSubCommand(new UpdateCommand(plugin));
        registerSubCommand(new UpdateAllCommand(plugin));
        registerSubCommand(new RemoveCommand(plugin));
        registerSubCommand(new BackupCommand(plugin));
        registerSubCommand(new RestoreCommand(plugin));
        registerSubCommand(new ScanCommand(plugin));
        registerSubCommand(new ListCommand(plugin));
        registerSubCommand(new DetectCommand(plugin));
        registerSubCommand(new LogsCommand(plugin));
        registerSubCommand(new SourcesCommand(plugin));
        registerSubCommand(new HelpCommand(plugin, subCommands));
        registerSubCommand(new ClearCacheCommand(plugin));
        registerSubCommand(new InfoCommand(plugin));
        registerSubCommand(new DeleteCommand(plugin));
        registerSubCommand(new DiscoverCommand(plugin));
        registerSubCommand(new ImportCommand(plugin));
    }
    
    private void registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(), subCommand);
        }
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show help by default
            subCommands.get("help").execute(sender, new String[0]);
            return true;
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            plugin.getMessageFormatter().sendMessage(sender, "invalid-usage", label);
            return true;
        }
        
        // Check permissions
        if (!subCommand.hasPermission(sender)) {
            plugin.getMessageFormatter().sendMessage(sender, "no-permission");
            return true;
        }
        
        // Execute subcommand
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subCommand.execute(sender, subArgs);
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommands
            return subCommands.keySet().stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .filter(cmd -> {
                        SubCommand subCmd = subCommands.get(cmd);
                        return subCmd.hasPermission(sender);
                    })
                    .sorted()
                    .distinct()
                    .limit(10)
                    .toList();
        }
        
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            
            if (subCommand != null && subCommand.hasPermission(sender)) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                
                // Use async tab completion for plugin searches
                if (needsAsyncCompletion(subCommandName, subArgs)) {
                    return asyncTabCompleter.getAsyncCompletions(sender, subCommandName, subArgs);
                }
                
                return subCommand.onTabComplete(sender, subArgs);
            }
        }
        
        return new ArrayList<>();
    }
    
    private boolean needsAsyncCompletion(String subCommand, String[] args) {
        // Commands that need async plugin name completion
        return switch (subCommand) {
            case "install", "add" -> args.length == 1;
            case "show", "info", "update", "remove", "backup", "restore", "scan" -> args.length == 1;
            default -> false;
        };
    }
}