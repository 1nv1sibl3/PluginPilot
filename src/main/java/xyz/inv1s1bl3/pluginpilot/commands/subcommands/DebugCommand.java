package xyz.inv1s1bl3.pluginpilot.commands.subcommands;

import org.bukkit.command.CommandSender;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.commands.SubCommand;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DebugCommand extends SubCommand {
    
    public DebugCommand(PluginPilot plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "debug";
    }
    
    @Override
    public String getDescription() {
        return "Debug plugin search and API issues";
    }
    
    @Override
    public List<String> getAliases() {
        return Arrays.asList("test", "troubleshoot");
    }
    
    @Override
    public String getPermission() {
        return "pluginpilot.debug";
    }
    
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§6=== PluginPilot Debug Info ===");
            sender.sendMessage("§e• Debug Mode: §f" + plugin.getConfig().getBoolean("debug-mode", false));
            sender.sendMessage("§e• Database: §f" + (plugin.getDatabaseManager() != null ? "Connected" : "Disconnected"));
            sender.sendMessage("§e• Sources: §f" + plugin.getSourceManager().getEnabledSources().size() + " enabled");
            sender.sendMessage("§e• Cache TTL: §fSearch=" + plugin.getConfig().getInt("cache.search-results-ttl-minutes", 30) + 
                             "min, Versions=" + plugin.getConfig().getInt("cache.plugin-info-ttl-minutes", 60) + "min");
            sender.sendMessage("§7");
            sender.sendMessage("§7Usage: §e/pp debug <plugin-name> §7- Test plugin search");
            sender.sendMessage("§7Usage: §e/pp debug sources §7- Test all sources");
            sender.sendMessage("§7Usage: §e/pp debug cache §7- Show cache statistics");
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "sources" -> testSources(sender);
            case "cache" -> showCacheStats(sender);
            default -> testPluginSearch(sender, args[0]);
        }
    }
    
    private void testSources(CommandSender sender) {
        sender.sendMessage("§6Testing all plugin sources...");
        
        CompletableFuture.runAsync(() -> {
            List<String> enabledSources = plugin.getSourceManager().getEnabledSources();
            
            for (String sourceName : enabledSources) {
                try {
                    sender.sendMessage("§7Testing §e" + sourceName + "§7...");
                    
                    // Test with a common plugin
                    List<PluginSearchResult> results = plugin.getSourceManager().searchPluginsFromSource(sourceName, "essentials");
                    
                    if (results.isEmpty()) {
                        sender.sendMessage("§c✗ " + sourceName + " - No results for 'essentials'");
                    } else {
                        sender.sendMessage("§a✓ " + sourceName + " - Found " + results.size() + " results");
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            sender.sendMessage("§7  First result: §f" + results.get(0).getName());
                        }
                    }
                    
                } catch (Exception e) {
                    sender.sendMessage("§c✗ " + sourceName + " - Error: " + e.getMessage());
                    if (plugin.getConfig().getBoolean("debug-mode", false)) {
                        plugin.getLogger().warning("Source test error for " + sourceName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            sender.sendMessage("§6Source testing completed.");
        });
    }
    
    private void testPluginSearch(CommandSender sender, String pluginName) {
        sender.sendMessage("§6Testing search for: §e" + pluginName);
        sender.sendMessage("§7This will test all enabled sources and show detailed results...");
        
        CompletableFuture.runAsync(() -> {
            try {
                List<String> enabledSources = plugin.getSourceManager().getEnabledSources();
                
                for (String sourceName : enabledSources) {
                    try {
                        sender.sendMessage("§7Searching §e" + sourceName + " §7for §e" + pluginName + "§7...");
                        
                        List<PluginSearchResult> results = plugin.getSourceManager().searchPluginsFromSource(sourceName, pluginName);
                        
                        if (results.isEmpty()) {
                            sender.sendMessage("§c  No results from " + sourceName);
                        } else {
                            sender.sendMessage("§a  Found " + results.size() + " results from " + sourceName + ":");
                            
                            for (int i = 0; i < Math.min(results.size(), 3); i++) {
                                PluginSearchResult result = results.get(i);
                                sender.sendMessage("§7    " + (i + 1) + ". §f" + result.getName() + 
                                                 " §7by §f" + result.getAuthor() + 
                                                 " §7(Downloads: §f" + result.getDownloads() + "§7)");
                                
                                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                                    sender.sendMessage("§7       ID: §f" + result.getId());
                                    sender.sendMessage("§7       Source ID: §f" + result.getSourceId());
                                    if (result.getDescription() != null && result.getDescription().length() > 0) {
                                        String desc = result.getDescription();
                                        if (desc.length() > 50) desc = desc.substring(0, 50) + "...";
                                        sender.sendMessage("§7       Desc: §f" + desc);
                                    }
                                }
                            }
                            
                            if (results.size() > 3) {
                                sender.sendMessage("§7    ... and " + (results.size() - 3) + " more results");
                            }
                        }
                        
                    } catch (Exception e) {
                        sender.sendMessage("§c  Error searching " + sourceName + ": " + e.getMessage());
                        if (plugin.getConfig().getBoolean("debug-mode", false)) {
                            plugin.getLogger().warning("Debug search error for " + sourceName + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
                
                // Test combined search
                sender.sendMessage("§7");
                sender.sendMessage("§7Testing combined search...");
                
                List<PluginSearchResult> combinedResults = plugin.getSourceManager().searchPlugins(pluginName);
                sender.sendMessage("§6Combined search found §e" + combinedResults.size() + " §6total results");
                
                if (!combinedResults.isEmpty()) {
                    sender.sendMessage("§7Top result: §f" + combinedResults.get(0).getName() + 
                                     " §7from §f" + combinedResults.get(0).getSourceType());
                }
                
            } catch (Exception e) {
                sender.sendMessage("§cError during debug search: " + e.getMessage());
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().severe("Debug command error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }
    
    private void showCacheStats(CommandSender sender) {
        CompletableFuture.runAsync(() -> {
            try {
                int searchCacheCount = plugin.getDatabaseManager().getCacheCount("search_cache");
                int versionCacheCount = plugin.getDatabaseManager().getCacheCount("version_cache");
                
                sender.sendMessage("§6=== Cache Statistics ===");
                sender.sendMessage("§e• Search Cache: §f" + searchCacheCount + " entries");
                sender.sendMessage("§e• Version Cache: §f" + versionCacheCount + " entries");
                sender.sendMessage("§e• Search TTL: §f" + plugin.getConfig().getInt("cache.search-results-ttl-minutes", 30) + " minutes");
                sender.sendMessage("§e• Version TTL: §f" + plugin.getConfig().getInt("cache.plugin-info-ttl-minutes", 60) + " minutes");
                
                // Show recent cache entries
                List<String> recentSearches = plugin.getDatabaseManager().getRecentCacheEntries("search_cache", 5);
                if (!recentSearches.isEmpty()) {
                    sender.sendMessage("§7Recent searches: §f" + String.join(", ", recentSearches));
                }
                
            } catch (Exception e) {
                sender.sendMessage("§cError retrieving cache stats: " + e.getMessage());
            }
        });
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("sources", "cache").stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return new ArrayList<>();
    }
}