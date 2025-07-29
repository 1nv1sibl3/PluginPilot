package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.LogEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.sql.SQLException;

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
        return Arrays.asList("log", "debug", "history");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.debug";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "view":
                viewLogs(sender, args.length > 1 ? args[1] : null, args.length > 2 ? Integer.parseInt(args[2]) : 20);
                break;
            case "export":
                exportLogs(sender, args.length > 1 ? args[1] : null);
                break;
            case "clear":
                clearLogs(sender);
                break;
            default:
                showHelp(sender);
                break;
        }
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageFormatter().colorize("&6=== PluginPilot Logs Help ==="));
        sender.sendMessage(plugin.getMessageFormatter().getMessage("help-logs-view"));
        sender.sendMessage(plugin.getMessageFormatter().getMessage("help-logs-export"));
        sender.sendMessage(plugin.getMessageFormatter().getMessage("help-logs-clear"));
    }
    
    private void viewLogs(CommandSender sender, String pluginName, int limit) {
        CompletableFuture.runAsync(() -> {
            try {
                List<LogEntry> logs;
                if (pluginName != null) {
                    logs = plugin.getDatabaseManager().getLogsByPlugin(pluginName, limit);
                    sender.sendMessage(plugin.getMessageFormatter().colorize("&6Recent logs for plugin &e" + pluginName + "&6:")); 
                } else {
                    logs = plugin.getDatabaseManager().getRecentLogs(limit);
                    sender.sendMessage(plugin.getMessageFormatter().colorize("&6Recent logs:")); 
                }
                
                if (logs.isEmpty()) {
                    sender.sendMessage(plugin.getMessageFormatter().getMessage("no-logs-found"));
                    return;
                }
                
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                
                for (LogEntry log : logs) {
                    String timestamp = dateFormat.format(new Date(log.getTimestamp()));
                    String type = log.getType();
                    String message = log.getMessage();
                    String pluginInfo = log.getPluginName() != null ? " [" + log.getPluginName() + "]" : "";
                    
                    String logColor = switch (type) {
                        case "ERROR" -> "&c";
                        case "WARNING" -> "&e";
                        case "INFO" -> "&a";
                        default -> "&7";
                    };
                    
                    sender.sendMessage(plugin.getMessageFormatter().colorize(
                            "&7[" + timestamp + "] " + logColor + type + "&7" + pluginInfo + ": " + message));
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.getMessageFormatter().getMessage("logs-view-failed", e.getMessage()));
                plugin.getLogger().severe("Error retrieving logs: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void exportLogs(CommandSender sender, String pluginName) {
        CompletableFuture.runAsync(() -> {
            try {
                // Create logs directory if it doesn't exist
                Path logsDir = Paths.get(plugin.getDataFolder().getAbsolutePath(), "logs");
                if (!Files.exists(logsDir)) {
                    Files.createDirectories(logsDir);
                }
                
                // Generate filename with timestamp
                SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                String timestamp = fileFormat.format(new Date());
                String fileName = pluginName != null ? 
                        pluginName + "_logs_" + timestamp + ".txt" : 
                        "pluginpilot_logs_" + timestamp + ".txt";
                
                File logFile = new File(logsDir.toFile(), fileName);
                
                // Get logs from database
                List<LogEntry> logs;
                try {
                    if (pluginName != null) {
                        logs = plugin.getDatabaseManager().getLogsByPlugin(pluginName, 1000); // Limit to 1000 entries
                    } else {
                        logs = plugin.getDatabaseManager().getRecentLogs(1000); // Limit to 1000 entries
                    }
                } catch (SQLException e) {
                    sender.sendMessage(plugin.getMessageFormatter().getMessage("logs-export-failed", "Database error: " + e.getMessage()));
                    plugin.getLogger().severe("Error retrieving logs from database: " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
                
                if (logs.isEmpty()) {
                    sender.sendMessage(plugin.getMessageFormatter().getMessage("no-logs-found"));
                    return;
                }
                
                // Write logs to file
                try (FileWriter writer = new FileWriter(logFile)) {
                    writer.write("PluginPilot Logs Export\n");
                    writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                    writer.write("Plugin: " + (pluginName != null ? pluginName : "All") + "\n");
                    writer.write("Created by: " + (sender instanceof Player ? ((Player) sender).getName() : "Console") + "\n");
                    writer.write("==========================================================\n\n");
                    
                    SimpleDateFormat logDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    
                    for (LogEntry log : logs) {
                        String formattedTime = logDateFormat.format(new Date(log.getTimestamp()));
                        String type = log.getType();
                        String message = log.getMessage();
                        String pluginInfo = log.getPluginName() != null ? " [" + log.getPluginName() + "]" : "";
                        
                        writer.write("[" + formattedTime + "] " + type + pluginInfo + ": " + message + "\n");
                    }
                }
                
                sender.sendMessage(plugin.getMessageFormatter().getMessage("logs-exported", logFile.getName()));
                sender.sendMessage(plugin.getMessageFormatter().colorize("&7File location: &f" + logFile.getAbsolutePath()));
                
                // Log the export action
                try {
                    plugin.getDatabaseManager().logAction("LOGS_EXPORT", "Logs exported to file", pluginName);
                } catch (SQLException sqlEx) {
                    plugin.getLogger().severe("Error logging export action: " + sqlEx.getMessage());
                }
                
            } catch (IOException e) {
                sender.sendMessage(plugin.getMessageFormatter().getMessage("logs-export-failed", e.getMessage()));
                plugin.getLogger().severe("Error exporting logs: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void clearLogs(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                int count = plugin.getDatabaseManager().clearLogs();
                sender.sendMessage(plugin.getMessageFormatter().getMessage("logs-cleared", String.valueOf(count)));
                
                // Log the clear action
                try {
                    plugin.getDatabaseManager().logAction("LOGS_CLEAR", "Logs cleared by user", null);
                } catch (SQLException sqlEx) {
                    plugin.getLogger().severe("Error logging clear action: " + sqlEx.getMessage());
                }
                
            } catch (SQLException e) {
                sender.sendMessage(plugin.getMessageFormatter().colorize("&cError clearing logs: " + e.getMessage()));
                plugin.getLogger().severe("Error clearing logs: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                sender.sendMessage(plugin.getMessageFormatter().colorize("&cError clearing logs: " + e.getMessage()));
                plugin.getLogger().severe("Error clearing logs: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> options = Arrays.asList("view", "export", "clear");
            return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("view") || args[0].equalsIgnoreCase("export"))) {
            // Tab complete with plugin names from the database
            try {
                List<String> plugins = plugin.getDatabaseManager().getLoggedPluginNames();
                return plugins.stream()
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .toList();
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }
        
        return new ArrayList<>();
    }
}