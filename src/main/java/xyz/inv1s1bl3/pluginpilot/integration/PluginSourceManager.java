package xyz.inv1s1bl3.pluginpilot.integration;

import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PluginSourceManager {
    
    private final PluginPilot plugin;
    private final List<PluginSource> sources;
    
    public PluginSourceManager(PluginPilot plugin) {
        this.plugin = plugin;
        this.sources = new ArrayList<>();
        
        initializeSources();
    }
    
    private void initializeSources() {
        // Initialize API clients for different sources
        if (plugin.getConfig().getBoolean("repositories.0.enabled", true)) {
            sources.add(new SpigotMCSource(plugin));
        }
        
        if (plugin.getConfig().getBoolean("repositories.1.enabled", true)) {
            sources.add(new ModrinthSource(plugin));
        }
        
        if (plugin.getConfig().getBoolean("repositories.2.enabled", true)) {
            sources.add(new PolymartSource(plugin));
        }
        
        if (plugin.getConfig().getBoolean("repositories.3.enabled", true)) {
            sources.add(new HangarSource(plugin));
        }
    }
    
    public List<PluginSearchResult> searchPlugins(String query) throws Exception {
        // Check database cache first
        List<PluginSearchResult> cached = plugin.getDatabaseManager().getCachedSearchResults(query);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        List<CompletableFuture<List<PluginSearchResult>>> futures = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return source.searchPlugins(query);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error searching " + source.getSourceName() + ": " + e.getMessage());
                        return new ArrayList<PluginSearchResult>();
                    }
                }))
                .toList();
        
        // Wait for all searches to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        
        try {
            allFutures.get(); // Wait for completion
        } catch (Exception e) {
            plugin.getLogger().warning("Error during plugin search: " + e.getMessage());
        }
        
        // Combine results
        List<PluginSearchResult> allResults = new ArrayList<>();
        for (CompletableFuture<List<PluginSearchResult>> future : futures) {
            try {
                allResults.addAll(future.get());
            } catch (Exception e) {
                // Skip failed sources
            }
        }
        
        // Remove duplicates and limit results
        List<PluginSearchResult> finalResults = allResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getName().toLowerCase(),
                        result -> result,
                        (existing, replacement) -> existing // Keep first occurrence
                ))
                .values()
                .stream()
                .limit(plugin.getConfig().getInt("cache.max-search-results", 50))
                .collect(Collectors.toList());
        
        // Cache the results in database
        try {
            plugin.getDatabaseManager().cacheSearchResults(query, finalResults);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache search results: " + e.getMessage());
        }
        
        return finalResults;
    }
    
    public List<PluginVersion> getPluginVersions(PluginSearchResult pluginResult) throws Exception {
        // Check cache first
        List<PluginVersion> cached = plugin.getDatabaseManager().getCachedPluginVersions(pluginResult.getId());
        if (cached != null) {
            return cached;
        }
        
        // Find the appropriate source
        PluginSource source = sources.stream()
                .filter(s -> s.getSourceName().equalsIgnoreCase(pluginResult.getSourceType()))
                .findFirst()
                .orElse(null);
        
        if (source == null) {
            throw new Exception("Unknown plugin source: " + pluginResult.getSourceType());
        }
        
        List<PluginVersion> versions = source.getPluginVersions(pluginResult);
        
        // Cache the results
        try {
            plugin.getDatabaseManager().cachePluginVersions(pluginResult.getId(), versions);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache plugin versions: " + e.getMessage());
        }
        
        return versions;
    }
    
    public boolean downloadAndInstallPlugin(PluginSearchResult pluginResult, PluginVersion version) throws Exception {
        // Find the appropriate source
        PluginSource source = sources.stream()
                .filter(s -> s.getSourceName().equalsIgnoreCase(pluginResult.getSourceType()))
                .findFirst()
                .orElse(null);
        
        if (source == null) {
            throw new Exception("Unknown plugin source: " + pluginResult.getSourceType());
        }
        
        return source.downloadPlugin(pluginResult, version);
    }
}