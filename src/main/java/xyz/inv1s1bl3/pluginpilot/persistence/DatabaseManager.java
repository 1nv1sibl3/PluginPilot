package xyz.inv1s1bl3.pluginpilot.persistence;

import xyz.inv1s1bl3.pluginpilot.PluginPilot;
import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;
import xyz.inv1s1bl3.pluginpilot.models.BackupRecord;
import xyz.inv1s1bl3.pluginpilot.models.LogEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    
    private final PluginPilot plugin;
    private Connection connection;
    private final String databasePath;
    
    public DatabaseManager(PluginPilot plugin) {
        this.plugin = plugin;
        this.databasePath = new File(plugin.getDataFolder(), 
                plugin.getConfig().getString("database.filename", "pluginpilot.db")).getAbsolutePath();
    }
    
    public void initialize() throws SQLException {
        // Create data folder if it doesn't exist
        plugin.getDataFolder().mkdirs();
        
        // Connect to database
        connect();
        
        // Create tables
        createTables();
    }
    
    private void connect() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        connection.setAutoCommit(true);
    }
    
    private void createTables() throws SQLException {
        // Plugins table
        String createPluginsTable = """
            CREATE TABLE IF NOT EXISTS plugins (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                version TEXT NOT NULL,
                source_type TEXT NOT NULL,
                source_id TEXT NOT NULL,
                install_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                managed_by_pilot BOOLEAN DEFAULT TRUE,
                status TEXT DEFAULT 'active',
                description TEXT,
                author TEXT,
                download_url TEXT,
                file_path TEXT
            )
        """;
        
        // Backups table
        String createBackupsTable = """
            CREATE TABLE IF NOT EXISTS backups (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                plugin_name TEXT NOT NULL,
                version TEXT NOT NULL,
                backup_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                file_path TEXT NOT NULL,
                FOREIGN KEY (plugin_name) REFERENCES plugins(name)
            )
        """;
        
        // Logs table
        String createLogsTable = """
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                type TEXT NOT NULL,
                message TEXT NOT NULL,
                plugin_name TEXT
            )
        """;
        
        // Settings table
        String createSettingsTable = """
            CREATE TABLE IF NOT EXISTS settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """;
        
        // Cache tables
        String createSearchCacheTable = """
            CREATE TABLE IF NOT EXISTS search_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                query TEXT NOT NULL,
                plugin_id TEXT NOT NULL,
                plugin_name TEXT NOT NULL,
                description TEXT,
                author TEXT,
                source_type TEXT NOT NULL,
                source_id TEXT NOT NULL,
                downloads INTEGER DEFAULT 0,
                rating REAL DEFAULT 0.0,
                latest_version TEXT,
                icon_url TEXT,
                cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        String createVersionCacheTable = """
            CREATE TABLE IF NOT EXISTS version_cache (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                plugin_id TEXT NOT NULL,
                version_id TEXT NOT NULL,
                version_number TEXT NOT NULL,
                download_url TEXT,
                changelog TEXT,
                release_date TIMESTAMP,
                is_stable BOOLEAN DEFAULT TRUE,
                minecraft_versions TEXT,
                file_size INTEGER DEFAULT 0,
                sha256_hash TEXT,
                cached_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPluginsTable);
            stmt.execute(createBackupsTable);
            stmt.execute(createLogsTable);
            stmt.execute(createSettingsTable);
            stmt.execute(createSearchCacheTable);
            stmt.execute(createVersionCacheTable);
        }
    }
    
    public void saveInstalledPlugin(PluginSearchResult plugin, PluginVersion version) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO plugins 
            (name, version, source_type, source_id, description, author, download_url, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, plugin.getName());
            stmt.setString(2, version.getVersion());
            stmt.setString(3, plugin.getSourceType());
            stmt.setString(4, plugin.getSourceId());
            stmt.setString(5, plugin.getDescription());
            stmt.setString(6, plugin.getAuthor());
            stmt.setString(7, version.getDownloadUrl());
            stmt.executeUpdate();
        }
    }
    
    public boolean isPluginInstalled(String pluginName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM plugins WHERE name = ? AND status = 'active'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    public List<String> getInstalledPluginNames() throws SQLException {
        List<String> plugins = new ArrayList<>();
        String sql = "SELECT name FROM plugins WHERE status = 'active' ORDER BY name";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                plugins.add(rs.getString("name"));
            }
        }
        
        return plugins;
    }
    
    public void updatePluginVersion(String pluginName, String newVersion) throws SQLException {
        String sql = """
            UPDATE plugins 
            SET version = ?, last_update = CURRENT_TIMESTAMP 
            WHERE name = ? AND status = 'active'
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newVersion);
            stmt.setString(2, pluginName);
            stmt.executeUpdate();
        }
    }
    
    public void removePlugin(String pluginName) throws SQLException {
        String sql = "UPDATE plugins SET status = 'removed' WHERE name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            stmt.executeUpdate();
        }
        
        // Log the removal
        logAction("remove", "Plugin removed", pluginName);
    }
    
    public String getPluginVersion(String pluginName) throws SQLException {
        String sql = "SELECT version FROM plugins WHERE name = ? AND status = 'active'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("version");
                }
            }
        }
        return null;
    }
    
    public String getPluginFilePath(String pluginName) throws SQLException {
        String sql = "SELECT file_path FROM plugins WHERE name = ? AND status = 'active'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_path");
                }
            }
        }
        return null;
    }
    
    public List<BackupRecord> getPluginBackups(String pluginName) throws SQLException {
        String sql = "SELECT * FROM backups WHERE plugin_name = ? ORDER BY backup_date DESC";
        List<BackupRecord> backups = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BackupRecord backup = new BackupRecord();
                    backup.setId(rs.getInt("id"));
                    backup.setPluginName(rs.getString("plugin_name"));
                    backup.setVersion(rs.getString("version"));
                    backup.setBackupDate(rs.getTimestamp("backup_date").toLocalDateTime());
                    backup.setFilePath(rs.getString("file_path"));
                    backups.add(backup);
                }
            }
        }
        
        return backups;
    }
    
    
    public int getCacheCount(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }
    
    public List<String> getRecentCacheEntries(String tableName, int limit) throws SQLException {
        String sql;
        if (tableName.equals("search_cache")) {
            sql = "SELECT DISTINCT query FROM search_cache ORDER BY cached_at DESC LIMIT ?";
        } else {
            sql = "SELECT DISTINCT plugin_id FROM version_cache ORDER BY cached_at DESC LIMIT ?";
        }
        
        List<String> entries = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    entries.add(rs.getString(1));
                }
            }
        }
        
        return entries;
    }
    
    public void saveBackup(String pluginName, String version, String filePath) throws SQLException {
        String sql = """
            INSERT INTO backups (plugin_name, version, file_path)
            VALUES (?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            stmt.setString(2, version);
            stmt.setString(3, filePath);
            stmt.executeUpdate();
        }
    }
    
    public void createBackupBeforeRemoval(String pluginName, String version, String filePath) throws SQLException {
        try {
            // Create backup directory if it doesn't exist
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // Create a backup file with timestamp
            File sourceFile = new File(filePath);
            if (sourceFile.exists()) {
                String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
                File backupFile = new File(backupDir, pluginName + "_" + version + "_" + timestamp + ".jar");
                
                // Copy the file
                Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                // Save backup record in database
                saveBackup(pluginName, version, backupFile.getAbsolutePath());
                
                plugin.getLogger().info("Created backup of " + pluginName + " v" + version + ": " + backupFile.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup for " + pluginName + ": " + e.getMessage());
            throw new SQLException("Failed to create backup: " + e.getMessage());
        }
    }
    
    public void logAction(String type, String message, String pluginName) throws SQLException {
        String sql = """
            INSERT INTO logs (type, message, plugin_name)
            VALUES (?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, type);
            stmt.setString(2, message);
            stmt.setString(3, pluginName);
            stmt.executeUpdate();
        }
    }
    
    // Cache management methods
    public void cacheSearchResults(String query, List<PluginSearchResult> results) throws SQLException {
        // First clear old cache for this query
        String deleteSql = "DELETE FROM search_cache WHERE query = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setString(1, query.toLowerCase());
            stmt.executeUpdate();
        }
        
        // Insert new results
        String insertSql = """
            INSERT INTO search_cache 
            (query, plugin_id, plugin_name, description, author, source_type, source_id, downloads, rating, latest_version, icon_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            for (PluginSearchResult result : results) {
                stmt.setString(1, query.toLowerCase());
                stmt.setString(2, result.getId());
                stmt.setString(3, result.getName());
                stmt.setString(4, result.getDescription());
                stmt.setString(5, result.getAuthor());
                stmt.setString(6, result.getSourceType());
                stmt.setString(7, result.getSourceId());
                stmt.setInt(8, result.getDownloads());
                stmt.setDouble(9, result.getRating());
                stmt.setString(10, result.getLatestVersion());
                stmt.setString(11, result.getIconUrl());
                stmt.executeUpdate();
            }
        }
    }
    
    public List<PluginSearchResult> getCachedSearchResults(String query) throws SQLException {
        String sql = """
            SELECT * FROM search_cache 
            WHERE query = ? AND datetime(cached_at, '+30 minutes') > datetime('now')
            ORDER BY downloads DESC
        """;
        
        List<PluginSearchResult> results = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, query.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PluginSearchResult result = new PluginSearchResult();
                    result.setId(rs.getString("plugin_id"));
                    result.setName(rs.getString("plugin_name"));
                    result.setDescription(rs.getString("description"));
                    result.setAuthor(rs.getString("author"));
                    result.setSourceType(rs.getString("source_type"));
                    result.setSourceId(rs.getString("source_id"));
                    result.setDownloads(rs.getInt("downloads"));
                    result.setRating(rs.getDouble("rating"));
                    result.setLatestVersion(rs.getString("latest_version"));
                    result.setIconUrl(rs.getString("icon_url"));
                    results.add(result);
                }
            }
        }
        
        return results.isEmpty() ? null : results;
    }
    
    public void cachePluginVersions(String pluginId, List<PluginVersion> versions) throws SQLException {
        // First clear old cache for this plugin
        String deleteSql = "DELETE FROM version_cache WHERE plugin_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setString(1, pluginId);
            stmt.executeUpdate();
        }
        
        // Insert new versions
        String insertSql = """
            INSERT INTO version_cache 
            (plugin_id, version_id, version_number, download_url, changelog, release_date, is_stable, minecraft_versions, file_size, sha256_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
            for (PluginVersion version : versions) {
                stmt.setString(1, pluginId);
                stmt.setString(2, version.getId());
                stmt.setString(3, version.getVersion());
                stmt.setString(4, version.getDownloadUrl());
                stmt.setString(5, version.getChangelog());
                stmt.setTimestamp(6, version.getReleaseDate() != null ? 
                    Timestamp.valueOf(version.getReleaseDate()) : null);
                stmt.setBoolean(7, version.isStable());
                stmt.setString(8, version.getMinecraftVersions());
                stmt.setLong(9, version.getFileSize());
                stmt.setString(10, version.getSha256Hash());
                stmt.executeUpdate();
            }
        }
    }
    
    public List<PluginVersion> getCachedPluginVersions(String pluginId) throws SQLException {
        String sql = """
            SELECT * FROM version_cache 
            WHERE plugin_id = ? AND datetime(cached_at, '+60 minutes') > datetime('now')
            ORDER BY release_date DESC
        """;
        
        List<PluginVersion> versions = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PluginVersion version = new PluginVersion();
                    version.setId(rs.getString("version_id"));
                    version.setVersion(rs.getString("version_number"));
                    version.setDownloadUrl(rs.getString("download_url"));
                    version.setChangelog(rs.getString("changelog"));
                    
                    Timestamp releaseDate = rs.getTimestamp("release_date");
                    if (releaseDate != null) {
                        version.setReleaseDate(releaseDate.toLocalDateTime());
                    }
                    
                    version.setStable(rs.getBoolean("is_stable"));
                    version.setMinecraftVersions(rs.getString("minecraft_versions"));
                    version.setFileSize(rs.getLong("file_size"));
                    version.setSha256Hash(rs.getString("sha256_hash"));
                    versions.add(version);
                }
            }
        }
        
        return versions.isEmpty() ? null : versions;
    }
    
    public void clearCache() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM search_cache");
            stmt.execute("DELETE FROM version_cache");
        }
    }
    
    /**
     * Retrieves recent logs from the database
     * @param limit Maximum number of logs to retrieve
     * @return List of log entries
     */
    public List<LogEntry> getRecentLogs(int limit) throws SQLException {
        String sql = "SELECT * FROM logs ORDER BY timestamp DESC LIMIT ?";
        List<LogEntry> logs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry log = new LogEntry();
                    log.setId(rs.getInt("id"));
                    log.setTimestamp(rs.getTimestamp("timestamp").getTime());
                    log.setType(rs.getString("type"));
                    log.setMessage(rs.getString("message"));
                    log.setPluginName(rs.getString("plugin_name"));
                    logs.add(log);
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Retrieves logs for a specific plugin
     * @param pluginName Name of the plugin
     * @param limit Maximum number of logs to retrieve
     * @return List of log entries for the specified plugin
     */
    public List<LogEntry> getLogsByPlugin(String pluginName, int limit) throws SQLException {
        String sql = "SELECT * FROM logs WHERE plugin_name = ? ORDER BY timestamp DESC LIMIT ?";
        List<LogEntry> logs = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, pluginName);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry log = new LogEntry();
                    log.setId(rs.getInt("id"));
                    log.setTimestamp(rs.getTimestamp("timestamp").getTime());
                    log.setType(rs.getString("type"));
                    log.setMessage(rs.getString("message"));
                    log.setPluginName(rs.getString("plugin_name"));
                    logs.add(log);
                }
            }
        }
        
        return logs;
    }
    
    /**
     * Clears all logs from the database
     * @return Number of logs deleted
     */
    public int clearLogs() throws SQLException {
        String sql = "DELETE FROM logs";
        
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
    
    /**
     * Gets a list of all plugin names that have logs in the database
     * @return List of plugin names
     */
    public List<String> getLoggedPluginNames() throws SQLException {
        String sql = "SELECT DISTINCT plugin_name FROM logs WHERE plugin_name IS NOT NULL ORDER BY plugin_name";
        List<String> plugins = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                plugins.add(rs.getString("plugin_name"));
            }
        }
        
        return plugins;
    }
    
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().warning("Error closing database connection: " + e.getMessage());
            }
        }
    }
}