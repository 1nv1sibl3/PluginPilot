# PluginPilot

A comprehensive plugin manager for Minecraft servers that supports multiple plugin repositories and provides advanced features for plugin management.

## Features

### Core Features
- **Multi-Source Installation**: Install plugins from Modrinth, Hangar, SpigotMC, Polymart, and custom sources
- **Live Search**: Real-time plugin search with caching and intelligent tab completion
- **Version Management**: Install specific versions, update plugins, and automatic updates
- **Advanced Tab Completion**: Dynamic tab completion with live API searching and "loading..." feedback
- **Plugin Backups**: Automatic backups before updates with rollback capability
- **Security Scanning**: Malware scanning integration (configurable)
- **Database Storage**: SQLite-based persistent storage for plugin metadata

### Command System
All commands support extensive tab completion:
- `/pp install <plugin> [version]` - Install plugins with live search completion
- `/pp show <plugin>` - Preview plugin information
- `/pp update <plugin>` - Update individual plugins
- `/pp updateall` - Update all managed plugins
- `/pp remove <plugin>` - Uninstall plugins
- `/pp backup <plugin>` - Create plugin backups
- `/pp restore <plugin> [version]` - Restore from backups
- `/pp scan <plugin>` - Security scan plugins
- `/pp list` - List all managed plugins
- `/pp detect` - Find unmanaged plugins
- `/pp logs` - View/upload debug logs
- `/pp sources` - Manage plugin repositories

### Advanced Tab Completion
The plugin features intelligent async tab completion that:
- Shows "Loading..." while searching APIs
- Caches results to reduce API calls
- Provides real-time plugin name suggestions
- Supports version completion for selected plugins
- Rate limits searches to prevent API spam

## Technical Details

### Requirements
- **Minecraft Version**: 1.21.4 (compatible up to 1.21.8)
- **Java Version**: 17+
- **API Version**: 1.21

### Architecture
- **Modular Design**: Clean separation of concerns with dedicated packages
- **Async Operations**: Non-blocking API calls and downloads
- **Caching System**: Intelligent caching with TTL for performance
- **Database Integration**: SQLite for reliable data persistence
- **Plugin Sources**: Extensible source system supporting multiple APIs

### Supported Sources
- ✅ **Modrinth**: Full API integration with search, versions, and downloads
- ✅ **Hangar**: PaperMC's plugin repository with complete support
- ⚠️ **SpigotMC**: Limited (no public API for automated downloads)
- 🚧 **Polymart**: Planned implementation
- 🚧 **Custom Sources**: GitHub, Jenkins, direct URLs (planned)

## Installation

1. Download the PluginPilot JAR file
2. Place it in your server's `plugins` folder
3. Start/restart your server
4. Configure the plugin in `plugins/PluginPilot/config.yml`
5. Use `/pp help` to see available commands

## Configuration

The plugin is highly configurable through `config.yml`:
- Enable/disable specific plugin sources
- Configure auto-update intervals
- Set cache TTL values
- Configure security scanning
- Customize tab completion behavior

## Permissions

- `pluginpilot.*` - All permissions
- `pluginpilot.install` - Install plugins
- `pluginpilot.update` - Update plugins
- `pluginpilot.uninstall` - Remove plugins
- `pluginpilot.backup` - Backup/restore plugins
- `pluginpilot.security` - Security scanning
- `pluginpilot.sources` - Manage sources
- `pluginpilot.debug` - Access logs and debug info

## Development

Built with Maven using modern Java 17 features:
- Async/CompletableFuture for non-blocking operations
- Stream API for efficient data processing
- Modern HTTP client (OkHttp) for API calls
- GSON for JSON processing
- SQLite for data persistence

## Contributing

This plugin uses a modular architecture making it easy to:
- Add new plugin sources
- Extend command functionality
- Improve the caching system
- Add new security features