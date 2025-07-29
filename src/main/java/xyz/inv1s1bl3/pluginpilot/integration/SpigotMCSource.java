package xyz.inv1s1bl3.pluginpilot.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SpigotMCSource implements PluginSource {
    
    private final PluginPilot plugin;
    private final OkHttpClient client;
    private final Gson gson;
    
    public SpigotMCSource(PluginPilot plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    @Override
    public String getSourceName() {
        return "spigotmc";
    }
    
    @Override
    public List<PluginSearchResult> searchPlugins(String query) throws Exception {
        List<PluginSearchResult> results = new ArrayList<>();
        
        // Filter by server type (Bukkit, Paper, Spigot) based on config
        List<String> serverTypes = plugin.getConfig().getStringList("allowed-server-types");
        if (serverTypes.isEmpty()) {
            // Default to all server types if not specified
            serverTypes = Arrays.asList("bukkit", "spigot", "paper");
        }
        
        try {
            // Using the SpigotMC API as documented in XenforoResourceManagerAPI
            // Base URL for the SpigotMC API
            String apiUrl = "https://api.spigotmc.org/simple/0.2/index.php";
            
            // Build the request URL for listing resources
            // We'll use the listResources action with pagination
            StringBuilder urlBuilder = new StringBuilder(apiUrl);
            urlBuilder.append("?action=listResources");
            
            // Add category parameter if needed (optional)
            // Categories can be found using the listResourceCategories action
            // urlBuilder.append("&category=4"); // Example: category 4
            
            // Add page parameter (default is 1)
            urlBuilder.append("&page=1");
            
            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to search SpigotMC: " + response.code());
                }
                
                String responseBody = response.body().string();
                
                // Parse the JSON array response
                com.google.gson.JsonArray jsonArray = gson.fromJson(responseBody, com.google.gson.JsonArray.class);
                
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject resourceObj = jsonArray.get(i).getAsJsonObject();
                    
                    // Extract resource information
                    String id = resourceObj.get("id").getAsString();
                    String name = resourceObj.get("title").getAsString();
                    String description = resourceObj.has("tag") ? resourceObj.get("tag").getAsString() : "";
                    
                    // Get author information
                    JsonObject authorObj = resourceObj.getAsJsonObject("author");
                    String author = authorObj.get("username").getAsString();
                    
                    // Create the search result
                    PluginSearchResult result = new PluginSearchResult(id, name, description, author, getSourceName(), id);
                    
                    // Set additional information if available
                    if (resourceObj.has("current_version")) {
                        result.setLatestVersion(resourceObj.get("current_version").getAsString());
                    }
                    
                    if (resourceObj.has("icon_link")) {
                        result.setIconUrl(resourceObj.get("icon_link").getAsString());
                    }
                    
                    // Set stats if available
                    if (resourceObj.has("stats")) {
                        JsonObject statsObj = resourceObj.getAsJsonObject("stats");
                        
                        if (statsObj.has("downloads")) {
                            result.setDownloads(statsObj.get("downloads").getAsInt());
                        }
                        
                        if (statsObj.has("rating")) {
                            result.setRating(statsObj.get("rating").getAsDouble());
                        }
                    }
                    
                    // Set server type - SpigotMC resources are primarily for Spigot
                    // but many work on Bukkit and Paper as well
                    result.setServerType("spigot");
                    
                    // Filter by server type
                    if (serverTypes.contains(result.getServerType())) {
                        results.add(result);
                    }
                }
            }
            
            // If query is not empty, filter results by name or description
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                results = results.stream()
                        .filter(result -> 
                            result.getName().toLowerCase().contains(lowerQuery) || 
                            (result.getDescription() != null && result.getDescription().toLowerCase().contains(lowerQuery)))
                        .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error searching SpigotMC: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    @Override
    public List<PluginVersion> getPluginVersions(PluginSearchResult plugin) throws Exception {
        List<PluginVersion> versions = new ArrayList<>();
        
        try {
            // Using the SpigotMC API as documented in XenforoResourceManagerAPI
            // Base URL for the SpigotMC API
            String apiUrl = "https://api.spigotmc.org/simple/0.2/index.php";
            
            // Build the request URL for getting resource updates
            // We'll use the getResourceUpdates action with the resource ID
            StringBuilder urlBuilder = new StringBuilder(apiUrl);
            urlBuilder.append("?action=getResourceUpdates");
            urlBuilder.append("&resource_id=").append(plugin.getSourceId());
            
            Request request = new Request.Builder()
                    .url(urlBuilder.toString())
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to get SpigotMC versions: " + response.code());
                }
                
                String responseBody = response.body().string();
                
                // Parse the JSON array response
                com.google.gson.JsonArray jsonArray = gson.fromJson(responseBody, com.google.gson.JsonArray.class);
                
                for (int i = 0; i < jsonArray.size(); i++) {
                    JsonObject updateObj = jsonArray.get(i).getAsJsonObject();
                    
                    // Extract update information
                    String id = updateObj.get("id").getAsString();
                    String title = updateObj.get("title").getAsString();
                    String message = updateObj.has("message") ? updateObj.get("message").getAsString() : "";
                    
                    // Create the version object
                    // Note: SpigotMC API doesn't provide direct download URLs
                    // We'll use a placeholder URL that will be handled in the download method
                    PluginVersion version = new PluginVersion(id, title, "spigotmc://" + plugin.getSourceId() + "/" + id);
                    
                    // Set changelog from message
                    version.setChangelog(message);
                    
                    // Set as stable version (assuming all versions from SpigotMC are stable)
                    version.setStable(true);
                    
                    versions.add(version);
                }
            }
            
            // Also get the resource information to ensure we have the latest version
            StringBuilder resourceUrlBuilder = new StringBuilder(apiUrl);
            resourceUrlBuilder.append("?action=getResource");
            resourceUrlBuilder.append("&id=").append(plugin.getSourceId());
            
            Request resourceRequest = new Request.Builder()
                    .url(resourceUrlBuilder.toString())
                    .build();
            
            try (Response resourceResponse = client.newCall(resourceRequest).execute()) {
                if (resourceResponse.isSuccessful()) {
                    String responseBody = resourceResponse.body().string();
                    JsonObject resourceObj = gson.fromJson(responseBody, JsonObject.class);
                    
                    if (resourceObj.has("current_version")) {
                        String currentVersion = resourceObj.get("current_version").getAsString();
                        
                        // Check if we already have this version in our list
                        boolean versionExists = versions.stream()
                                .anyMatch(v -> v.getVersion().equals(currentVersion));
                        
                        if (!versionExists) {
                            // Add the current version if it's not in our list
                            PluginVersion latestVersion = new PluginVersion(
                                    "latest", 
                                    currentVersion, 
                                    "spigotmc://" + plugin.getSourceId() + "/latest");
                            latestVersion.setStable(true);
                            versions.add(0, latestVersion); // Add at the beginning of the list
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            this.plugin.getLogger().warning("Error getting SpigotMC versions: " + e.getMessage());
            if (this.plugin.getConfig().getBoolean("debug-mode", false)) {
                e.printStackTrace();
            }
        }
        
        return versions;
    }
    
    @Override
    public boolean downloadPlugin(PluginSearchResult plugin, PluginVersion version) throws Exception {
        // SpigotMC doesn't provide direct download URLs through their API
        // We need to inform the user that they need to download manually
        
        // Get the resource URL for manual download
        String resourceUrl = "https://www.spigotmc.org/resources/" + plugin.getSourceId();
        
        // Log a message with the manual download URL
        this.plugin.getLogger().info("SpigotMC doesn't support automated downloads.");
        this.plugin.getLogger().info("Please download " + plugin.getName() + " v" + version.getVersion() + " manually from: " + resourceUrl);
        
        // Throw an exception with instructions
        throw new Exception("SpigotMC doesn't support automated downloads. Please download manually from: " + resourceUrl);
    }
}