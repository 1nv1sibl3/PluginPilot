package xyz.inv1s1bl3.pluginpilot.integration;

import xyz.inv1s1bl3.pluginpilot.models.PluginSearchResult;
import xyz.inv1s1bl3.pluginpilot.models.PluginVersion;

import java.util.List;

public interface PluginSource {
    
    String getSourceName();
    
    List<PluginSearchResult> searchPlugins(String query) throws Exception;
    
    List<PluginVersion> getPluginVersions(PluginSearchResult plugin) throws Exception;
    
    boolean downloadPlugin(PluginSearchResult plugin, PluginVersion version) throws Exception;
}