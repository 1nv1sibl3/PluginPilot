package xyz.inv1s1bl3.pluginpilot.models;

import java.time.LocalDateTime;

public class PluginVersion {
    
    private String id;
    private String version;
    private String downloadUrl;
    private String changelog;
    private LocalDateTime releaseDate;
    private boolean isStable;
    private String minecraftVersions;
    private long fileSize;
    private String sha256Hash;
    
    public PluginVersion() {}
    
    public PluginVersion(String id, String version, String downloadUrl) {
        this.id = id;
        this.version = version;
        this.downloadUrl = downloadUrl;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    
    public String getChangelog() { return changelog; }
    public void setChangelog(String changelog) { this.changelog = changelog; }
    
    public LocalDateTime getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDateTime releaseDate) { this.releaseDate = releaseDate; }
    
    public boolean isStable() { return isStable; }
    public void setStable(boolean stable) { isStable = stable; }
    
    public String getMinecraftVersions() { return minecraftVersions; }
    public void setMinecraftVersions(String minecraftVersions) { this.minecraftVersions = minecraftVersions; }
    
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    
    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }
}