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

public class HangarSource implements PluginSource {
    
    private final PluginPilot plugin;
    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl = "https://hangar.papermc.io/api/v1";
    
    public HangarSource(PluginPilot plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    @Override
    public String getSourceName() {
        return "hangar";
    }
    
    @Override
    public List<PluginSearchResult> searchPlugins(String query) throws Exception {
        String url = baseUrl + "/projects?q=" + query + "&limit=20&category=admin_tools,chat,developer_tools,economy,gameplay,protection,role_playing,world_management";
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "PluginPilot/1.0.0")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Hangar API error: " + response.code());
            }
            
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray projects = json.getAsJsonArray("result");
            
            List<PluginSearchResult> results = new ArrayList<>();
            
            for (JsonElement projectElement : projects) {
                JsonObject project = projectElement.getAsJsonObject();
                
                PluginSearchResult result = new PluginSearchResult();
                result.setId(project.get("name").getAsString());
                result.setName(project.get("name").getAsString());
                result.setDescription(project.get("description").getAsString());
                result.setAuthor(project.get("owner").getAsString());
                result.setSourceType("hangar");
                result.setSourceId(project.get("name").getAsString());
                
                // Set server type to paper since Hangar is primarily for Paper plugins
                result.setServerType("paper");
                
                if (project.has("stats") && project.get("stats").isJsonObject()) {
                    JsonObject stats = project.getAsJsonObject("stats");
                    if (stats.has("downloads")) {
                        result.setDownloads(stats.get("downloads").getAsInt());
                    }
                    if (stats.has("stars")) {
                        result.setRating(stats.get("stars").getAsDouble());
                    }
                }
                
                results.add(result);
            }
            
            return results;
        }
    }
    
    @Override
    public List<PluginVersion> getPluginVersions(PluginSearchResult plugin) throws Exception {
        String url = baseUrl + "/projects/" + plugin.getSourceId() + "/versions";
        
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "PluginPilot/1.0.0")
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("Hangar API error: " + response.code());
            }
            
            String body = response.body().string();
            JsonObject json = gson.fromJson(body, JsonObject.class);
            JsonArray versions = json.getAsJsonArray("result");
            
            List<PluginVersion> results = new ArrayList<>();
            
            for (JsonElement versionElement : versions) {
                JsonObject version = versionElement.getAsJsonObject();
                
                PluginVersion pv = new PluginVersion();
                pv.setId(version.get("name").getAsString());
                pv.setVersion(version.get("name").getAsString());
                pv.setChangelog(version.has("description") ? version.get("description").getAsString() : "");
                
                // Parse date
                if (version.has("createdAt")) {
                    String dateStr = version.get("createdAt").getAsString();
                    pv.setReleaseDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME));
                }
                
                // Set platform versions
                if (version.has("platformDependencies") && version.get("platformDependencies").isJsonObject()) {
                    JsonObject platforms = version.getAsJsonObject("platformDependencies");
                    if (platforms.has("PAPER") && platforms.get("PAPER").isJsonArray()) {
                        JsonArray paperVersions = platforms.getAsJsonArray("PAPER");
                        List<String> mcVersions = new ArrayList<>();
                        for (JsonElement pv_element : paperVersions) {
                            mcVersions.add(pv_element.getAsString());
                        }
                        pv.setMinecraftVersions(String.join(", ", mcVersions));
                    }
                }
                
                // Set download URL
                String downloadUrl = baseUrl + "/projects/" + plugin.getSourceId() + "/versions/" + pv.getVersion() + "/download";
                pv.setDownloadUrl(downloadUrl);
                
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
            
            this.plugin.getLogger().info("Downloaded " + plugin.getName() + " v" + version.getVersion() + " from Hangar");
            return true;
        }
    }
}