package xyz.inv1s1bl3.pluginpilot.integration;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModrinthSource implements PluginSource {
    
    private final PluginPilot plugin;
    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl = "https://api.modrinth.com/v2";
    
    public ModrinthSource(PluginPilot plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    @Override
    public String getSourceName() {
        return "modrinth";
    }
    
    @Override
    public List<PluginSearchResult> searchPlugins(String query) throws Exception {
        // Fix the search URL - use proper encoding and search for plugins only
        String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
        
        // Try searching in each server type category sequentially until we find results
        List<String> serverTypesToSearch = Arrays.asList("bukkit", "spigot", "paper");
        List<PluginSearchResult> results = new ArrayList<>();
        
        for (String serverType : serverTypesToSearch) {
            String url = baseUrl + "/search?query=" + encodedQuery + 
                        "&facets=[[\"project_type:plugin\"],[\"categories:" + serverType + "\"]]&limit=20";
            
            if (plugin.getConfig().getBoolean("debug-mode", false)) {
                plugin.getLogger().info("Modrinth search URL for " + serverType + ": " + url);
            }
            
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "PluginPilot/1.0.0")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (plugin.getConfig().getBoolean("debug-mode", false)) {
                        plugin.getLogger().warning("Modrinth API error " + response.code() + ": " + response.message());
                    }
                    continue; // Try next server type instead of throwing exception
                }
                
                String body = response.body().string();
                JsonObject json = gson.fromJson(body, JsonObject.class);
                JsonArray hits = json.getAsJsonArray("hits");
                
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().info("Modrinth returned " + hits.size() + " results for " + serverType + " query: " + query);
                    plugin.getLogger().info("Response body preview: " + body.substring(0, Math.min(200, body.length())));
                }
                
                // If we found results, process them and stop searching
                if (hits.size() > 0) {
                    for (JsonElement hit : hits) {
                        JsonObject project = hit.getAsJsonObject();
                        
                        PluginSearchResult result = new PluginSearchResult();
                        result.setId(project.get("project_id").getAsString());
                        result.setName(project.get("title").getAsString());
                        result.setDescription(project.get("description").getAsString());
                        result.setAuthor(project.get("author").getAsString());
                        result.setSourceType("modrinth");
                        result.setSourceId(project.get("project_id").getAsString());
                        result.setDownloads(project.get("downloads").getAsInt());
                        
                        // Set server type based on current search category
                        result.setServerType(serverType);
                        
                        // Also check categories for more specific server type info
                        if (project.has("categories") && project.get("categories").isJsonArray()) {
                            JsonArray categories = project.getAsJsonArray("categories");
                            for (JsonElement category : categories) {
                                String categoryStr = category.getAsString().toLowerCase();
                                if (categoryStr.contains("bukkit")) {
                                    result.setServerType("bukkit");
                                    break;
                                } else if (categoryStr.contains("spigot")) {
                                    result.setServerType("spigot");
                                    break;
                                } else if (categoryStr.contains("paper")) {
                                    result.setServerType("paper");
                                    break;
                                }
                            }
                        }
                        
                        if (project.has("icon_url") && !project.get("icon_url").isJsonNull()) {
                            result.setIconUrl(project.get("icon_url").getAsString());
                        }
                        
                        if (project.has("versions") && project.get("versions").isJsonArray()) {
                            JsonArray versions = project.getAsJsonArray("versions");
                            if (versions.size() > 0) {
                                result.setLatestVersion(versions.get(0).getAsString());
                            }
                        }
                        
                        results.add(result);
                    }
                    
                    // If we found results, stop searching through server types
                    if (!results.isEmpty()) {
                        break;
                    }
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("debug-mode", false)) {
                    plugin.getLogger().warning("Error searching Modrinth with " + serverType + ": " + e.getMessage());
                }
                // Continue to next server type on error
            }
        }
        
        return results;
    }
    
    @Override
    public List<PluginVersion> getPluginVersions(PluginSearchResult plugin) throws Exception {
        String url = baseUrl + "/project/" + plugin.getSourceId() + "/version";
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "PluginPilot/1.0.0")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Modrinth API error: " + response.code());
            }
            
            String body = response.body().string();
            JsonArray versions = gson.fromJson(body, JsonArray.class);
            
            List<PluginVersion> results = new ArrayList<>();
            
            for (JsonElement versionElement : versions) {
                JsonObject version = versionElement.getAsJsonObject();
                
                PluginVersion pv = new PluginVersion();
                pv.setId(version.get("id").getAsString());
                pv.setVersion(version.get("version_number").getAsString());
                pv.setChangelog(version.has("changelog") ? version.get("changelog").getAsString() : "");
                
                // Parse date
                if (version.has("date_published")) {
                    String dateStr = version.get("date_published").getAsString();
                    pv.setReleaseDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                }
                
                // Get download URL from files
                if (version.has("files") && version.get("files").isJsonArray()) {
                    JsonArray files = version.getAsJsonArray("files");
                    if (files.size() > 0) {
                        JsonObject file = files.get(0).getAsJsonObject();
                        pv.setDownloadUrl(file.get("url").getAsString());
                        pv.setFileSize(file.get("size").getAsLong());
                        
                        if (file.has("hashes") && file.get("hashes").isJsonObject()) {
                            JsonObject hashes = file.getAsJsonObject("hashes");
                            if (hashes.has("sha256")) {
                                pv.setSha256Hash(hashes.get("sha256").getAsString());
                            }
                        }
                    }
                }
                
                // Set minecraft versions
                if (version.has("game_versions") && version.get("game_versions").isJsonArray()) {
                    JsonArray gameVersions = version.getAsJsonArray("game_versions");
                    List<String> mcVersions = new ArrayList<>();
                    for (JsonElement gv : gameVersions) {
                        mcVersions.add(gv.getAsString());
                    }
                    pv.setMinecraftVersions(String.join(", ", mcVersions));
                }
                
                results.add(pv);
            }
            
            return results;
        }
    }
    
    @Override
    public boolean downloadPlugin(PluginSearchResult plugin, PluginVersion version) throws Exception {
        if (version.getDownloadUrl() == null || version.getDownloadUrl().isEmpty()) {
            throw new Exception("No download URL available for " + plugin.getName());
        }
        
        Request request = new Request.Builder()
                .url(version.getDownloadUrl())
                .header("User-Agent", "PluginPilot/1.0.0")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Download failed: " + response.code());
            }
            
            // Create plugins folder if it doesn't exist
            File pluginsDir = new File(this.plugin.getServer().getWorldContainer(), "plugins");
            if (!pluginsDir.exists()) {
                pluginsDir.mkdirs();
            }
            
            // Download file
            String fileName = plugin.getName() + "-" + version.getVersion() + ".jar";
            File outputFile = new File(pluginsDir, fileName);
            
            try (InputStream is = response.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(outputFile)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            this.plugin.getLogger().info("Downloaded " + plugin.getName() + " v" + version.getVersion() + " from Modrinth");
            return true;
        }
    }
}