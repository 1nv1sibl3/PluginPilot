package xyz.inv1s1bl3.pluginpilot.models;

public class PluginSearchResult {
    
    private String id;
    private String name;
    private String description;
    private String author;
    private String iconUrl;
    private String sourceType;
    private String sourceId;
    private int downloads;
    private double rating;
    private String latestVersion;
    
    public PluginSearchResult() {}
    
    public PluginSearchResult(String id, String name, String description, String author, 
                            String sourceType, String sourceId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.author = author;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    
    public int getDownloads() { return downloads; }
    public void setDownloads(int downloads) { this.downloads = downloads; }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
}