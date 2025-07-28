package xyz.inv1s1bl3.pluginpilot.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class AsyncTabCompleter {
    
    private final PluginPilot plugin;
    private final Map<String, CompletableFuture<List<String>>> activeSearches;
    private final Map<String, Long> lastSearchTime;
    private final long searchDelayMs;
    
    public AsyncTabCompleter(PluginPilot plugin) {
        this.plugin = plugin;
        this.activeSearches = new ConcurrentHashMap<>();
        this.lastSearchTime = new ConcurrentHashMap<>();
        this.searchDelayMs = plugin.getConfig().getLong("tab-completion.search-delay-ms", 500);
    }
    
    public List<String> getAsyncCompletions(CommandSender sender, String subCommand, String[] args) {
        if (args.length != 1) {
            return new ArrayList<>();
        }
        
        String query = args[0].toLowerCase();
        String cacheKey = sender.getName() + ":" + query;
        
        // Check if we have cached results
        List<String> cached = getCachedCompletions(query);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream()
                    .filter(name -> name.toLowerCase().startsWith(query))
                    .limit(plugin.getConfig().getInt("tab-completion.max-suggestions", 10))
                    .toList();
        }
        
        // Check if search is already in progress
        CompletableFuture<List<String>> existingSearch = activeSearches.get(cacheKey);
        if (existingSearch != null && !existingSearch.isDone()) {
            // Return loading message
            return Arrays.asList(plugin.getConfig().getString("tab-completion.loading-message", "§7Loading..."));
        }
        
        // Check rate limiting
        long currentTime = System.currentTimeMillis();
        Long lastSearch = lastSearchTime.get(cacheKey);
        if (lastSearch != null && (currentTime - lastSearch) < searchDelayMs) {
            return Arrays.asList(plugin.getConfig().getString("tab-completion.loading-message", "§7Loading..."));
        }
        
        // Start new search
        lastSearchTime.put(cacheKey, currentTime);
        
        CompletableFuture<List<String>> searchFuture = CompletableFuture.supplyAsync(() -> {
            try {
                List<PluginSearchResult> results = plugin.getSourceManager().searchPlugins(query);
                
                List<String> completions = results.stream()
                        .map(PluginSearchResult::getName)
                        .filter(name -> name.toLowerCase().startsWith(query))
                        .limit(plugin.getConfig().getInt("tab-completion.max-suggestions", 10))
                        .toList();
                
                // Cache results
                try {
                    plugin.getDatabaseManager().cacheSearchResults(query, results);
                } catch (Exception cacheError) {
                    // Ignore cache errors
                }
                
                return completions;
                
            } catch (Exception e) {
                plugin.getLogger().warning("Error during async tab completion search: " + e.getMessage());
                return new ArrayList<>();
            }
        });
        
        // Store the future
        activeSearches.put(cacheKey, searchFuture);
        
        // Clean up completed searches after a delay
        searchFuture.whenComplete((result, throwable) -> {
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                activeSearches.remove(cacheKey);
                lastSearchTime.remove(cacheKey);
            });
        });
        
        // Show loading message initially
        return Arrays.asList(plugin.getConfig().getString("tab-completion.loading-message", "§7Loading..."));
    }
    
    private List<String> getCachedCompletions(String query) {
        try {
            List<PluginSearchResult> cached = plugin.getDatabaseManager().getCachedSearchResults(query);
            if (cached != null) {
                return cached.stream()
                        .map(PluginSearchResult::getName)
                        .toList();
            }
        } catch (Exception e) {
            // Ignore cache errors
        }
        return null;
    }
}