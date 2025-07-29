package xyz.inv1s1bl3.pluginpilot.integration;

import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.ArrayList;
import java.util.Arrays;
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
        // Clear any existing sources to ensure we respect the current config
        sources.clear();
        
        // Initialize API clients for different sources based on config
        boolean spigotEnabled = plugin.getConfig().getBoolean("repositories.0.enabled", false);
        boolean modrinthEnabled = plugin.getConfig().getBoolean("repositories.1.enabled", true);
        boolean polymartEnabled = plugin.getConfig().getBoolean("repositories.2.enabled", false);
        boolean hangarEnabled = plugin.getConfig().getBoolean("repositories.3.enabled", true);
        
        // Log which sources are being initialized
        plugin.getLogger().info("Initializing plugin sources:");
        
        if (spigotEnabled) {
            sources.add(new SpigotMCSource(plugin));
            plugin.getLogger().info(" - SpigotMC: Enabled");
        } else {
            plugin.getLogger().info(" - SpigotMC: Disabled (marked as under development)");
        }
        
        if (modrinthEnabled) {
            sources.add(new ModrinthSource(plugin));
            plugin.getLogger().info(" - Modrinth: Enabled");
        } else {
            plugin.getLogger().info(" - Modrinth: Disabled");
        }
        
        if (polymartEnabled) {
            sources.add(new PolymartSource(plugin));
            plugin.getLogger().info(" - Polymart: Enabled");
        } else {
            plugin.getLogger().info(" - Polymart: Disabled (marked as under development)");
        }
        
        if (hangarEnabled) {
            sources.add(new HangarSource(plugin));
            plugin.getLogger().info(" - Hangar: Enabled");
        } else {
            plugin.getLogger().info(" - Hangar: Disabled");
        }
    }
    
    public List<PluginSearchResult> searchPlugins(String query) throws Exception {
        // Check database cache first
        List<PluginSearchResult> cached = plugin.getDatabaseManager().getCachedSearchResults(query);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }
        
        // Send the same query to all enabled sources
        List<CompletableFuture<List<PluginSearchResult>>> futures = sources.stream()
                .map(source -> CompletableFuture.supplyAsync(() -> {
                    try {
                        plugin.getLogger().info("Searching " + source.getSourceName() + " for: " + query);
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
        
        // Get configured server types
        final List<String> serverTypes = new ArrayList<>(plugin.getConfig().getStringList("allowed-server-types"));
        if (serverTypes.isEmpty()) {
            // Default to all server types if not specified
            serverTypes.addAll(Arrays.asList("bukkit", "spigot", "paper"));
        }
        
        // Filter by server type, handle duplicates intelligently, and limit results
        List<PluginSearchResult> finalResults = allResults.stream()
                // Filter by server type if the plugin has server type information
                .filter(result -> {
                    // If server type is not specified, include it
                    if (result.getServerType() == null || result.getServerType().isEmpty()) {
                        return true;
                    }
                    // Check if any of the plugin's server types match our configured types
                    for (String type : serverTypes) {
                        if (result.getServerType().toLowerCase().contains(type.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                // Group by name (case-insensitive) to handle duplicates
                .collect(Collectors.groupingBy(result -> result.getName().toLowerCase()))
                .values().stream()
                .flatMap(duplicates -> {
                    // For each group of plugins with the same name, keep all of them
                    // This ensures plugins with the same name from different sources are all included
                    return duplicates.stream();
                })
                .limit(plugin.getConfig().getInt("cache.max-search-results", 50))
                .collect(Collectors.toList());
                
        if (plugin.getConfig().getBoolean("debug-mode", false)) {
            plugin.getLogger().info("Found " + finalResults.size() + " results for query: " + query);
            for (PluginSearchResult result : finalResults) {
                plugin.getLogger().info("  - " + result.getName() + " from " + result.getSourceType());
            }
        }
        
        // Cache the results in database
        try {
            plugin.getDatabaseManager().cacheSearchResults(query, finalResults);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cache search results: " + e.getMessage());
        }
        
        return finalResults;
    }
    
    public List<PluginSearchResult> searchPluginsFromSource(String sourceName, String query) throws Exception {
        PluginSource source = sources.stream()
                .filter(s -> s.getSourceName().equalsIgnoreCase(sourceName))
                .findFirst()
                .orElse(null);
        
        if (source == null) {
            throw new Exception("Unknown plugin source: " + sourceName);
        }
        
        return source.searchPlugins(query);
    }
    
    public List<String> getEnabledSources() {
        return sources.stream()
                .map(PluginSource::getSourceName)
                .toList();
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
    
    /**
     * Refreshes the plugin sources based on the current configuration.
     * This allows changes to the config.yml to take effect without requiring a server restart.
     */
    public void refreshSources() {
        plugin.getLogger().info("Refreshing plugin sources from configuration...");
        initializeSources();
        plugin.getLogger().info("Plugin sources refreshed. Active sources: " + 
                String.join(", ", getEnabledSources()));
    }
}