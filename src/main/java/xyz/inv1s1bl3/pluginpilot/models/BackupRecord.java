package xyz.inv1s1bl3.pluginpilot.models;

import java.time.LocalDateTime;

public class BackupRecord {
    
    private int id;
    private String pluginName;
    private String version;
    private LocalDateTime backupDate;
    private String filePath;
    
    public BackupRecord() {}
    
    public BackupRecord(String pluginName, String version, String filePath) {
        this.pluginName = pluginName;
        this.version = version;
        this.filePath = filePath;
        this.backupDate = LocalDateTime.now();
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getPluginName() { return pluginName; }
    public void setPluginName(String pluginName) { this.pluginName = pluginName; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public LocalDateTime getBackupDate() { return backupDate; }
    public void setBackupDate(LocalDateTime backupDate) { this.backupDate = backupDate; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}