package xyz.inv1s1bl3.pluginpilot.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import xyz.inv1s1bl3.pluginpilot.PluginPilot;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

public class MessageFormatter {
    
    private final PluginPilot plugin;
    private FileConfiguration messages;
    private String prefix;
    
    public MessageFormatter(PluginPilot plugin) {
        this.plugin = plugin;
        loadMessages();
    }
    
    private void loadMessages() {
        // Save default messages.yml if it doesn't exist
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load messages from file
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Load defaults from jar
        InputStream defConfigStream = plugin.getResource("messages.yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            messages.setDefaults(defConfig);
        }
        
        // Get prefix
        prefix = colorize(messages.getString("prefix", "&6[PluginPilot]&r "));
    }
    
    public void sendMessage(CommandSender sender, String key, Object... args) {
        String message = getMessage(key, args);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(prefix + message);
        }
    }
    
    public void sendRawMessage(CommandSender sender, String key, Object... args) {
        String message = getMessage(key, args);
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(message);
        }
    }
    
    public String getMessage(String key, Object... args) {
        String message = messages.getString(key);
        if (message == null) {
            plugin.getLogger().warning("Missing message key: " + key);
            return "Missing message: " + key;
        }
        
        // Format message with arguments
        if (args.length > 0) {
            message = MessageFormat.format(message, args);
        }
        
        return colorize(message);
    }
    
    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public void reloadMessages() {
        loadMessages();
    }
}