package xyz.inv1s1bl3.pluginpilot.models;

import java.sql.Timestamp;

public class LogEntry {
    
    private int id;
    private long timestamp;
    private String type;
    private String message;
    private String pluginName;
    
    public LogEntry() {}
    
    public LogEntry(int id, long timestamp, String type, String message, String pluginName) {
        this.id = id;
        this.timestamp = timestamp;
        this.type = type;
        this.message = message;
        this.pluginName = pluginName;
    }
    
    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getPluginName() { return pluginName; }
    public void setPluginName(String pluginName) { this.pluginName = pluginName; }
    
    @Override
    public String toString() {
        return "LogEntry{" +
                "id=" + id +
                ", timestamp=" + new Timestamp(timestamp) +
                ", type='" + type + '\'' +
                ", message='" + message + '\'' +
                ", pluginName='" + pluginName + '\'' +
                '}';
    }
}