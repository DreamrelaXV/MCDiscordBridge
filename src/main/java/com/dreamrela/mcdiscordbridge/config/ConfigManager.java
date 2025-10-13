package com.dreamrela.mcdiscordbridge.config;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class ConfigManager {
    
    private final MCDiscordBridge plugin;
    private FileConfiguration config;
    
    public ConfigManager(MCDiscordBridge plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }
    
    // Discord configuration
    public String getDiscordToken() {
        return config.getString("discord.token", "");
    }
    
    public String getActivityType() {
        return config.getString("discord.activity.type", "WATCHING");
    }
    
    public String getActivityText() {
        return config.getString("discord.activity.text", "Minecraft Server");
    }
    
    // Root roles system
    public Map<String, String> getRootRoles() {
        Map<String, String> rootRoles = new HashMap<String, String>();
        ConfigurationSection rootRolesSection = config.getConfigurationSection("discord.root_roles");
        if (rootRolesSection != null) {
            for (String roleAlias : rootRolesSection.getKeys(false)) {
                String roleId = rootRolesSection.getString(roleAlias);
                if (roleId != null && !roleId.isEmpty()) {
                    rootRoles.put(roleAlias.toLowerCase(), roleId);
                }
            }
        }
        return rootRoles;
    }
    
    // Guild configuration
    public List<String> getGuildIds() {
        List<String> guildIds = new ArrayList<String>();
        ConfigurationSection guildsSection = config.getConfigurationSection("discord.guilds");
        if (guildsSection != null) {
            for (String guildKey : guildsSection.getKeys(false)) {
                String guildId = guildsSection.getString(guildKey + ".id");
                if (guildId != null && !guildId.isEmpty()) {
                    guildIds.add(guildId);
                }
            }
        }
        return guildIds;
    }
    
    public String getGuildName(String guildId) {
        ConfigurationSection guildsSection = config.getConfigurationSection("discord.guilds");
        if (guildsSection != null) {
            for (String guildKey : guildsSection.getKeys(false)) {
                if (guildId.equals(guildsSection.getString(guildKey + ".id"))) {
                    return guildsSection.getString(guildKey + ".name", "Unknown Guild");
                }
            }
        }
        return "Unknown Guild";
    }
    
    private Map<String, String> getGuildRoleOverrides(String guildId) {
        Map<String, String> overrides = new HashMap<String, String>();
        ConfigurationSection guildsSection = config.getConfigurationSection("discord.guilds");
        if (guildsSection != null) {
            for (String guildKey : guildsSection.getKeys(false)) {
                if (guildId.equals(guildsSection.getString(guildKey + ".id"))) {
                    ConfigurationSection overridesSection = guildsSection.getConfigurationSection(guildKey + ".role_overrides");
                    if (overridesSection != null) {
                        for (String roleAlias : overridesSection.getKeys(false)) {
                            String roleId = overridesSection.getString(roleAlias);
                            if (roleId != null && !roleId.isEmpty()) {
                                overrides.put(roleAlias.toLowerCase(), roleId);
                            }
                        }
                    }
                    break;
                }
            }
        }
        return overrides;
    }
    
    public List<String> getCommandPermissions(String guildId, String command) {
        ConfigurationSection guildsSection = config.getConfigurationSection("discord.guilds");
        if (guildsSection != null) {
            for (String guildKey : guildsSection.getKeys(false)) {
                if (guildId.equals(guildsSection.getString(guildKey + ".id"))) {
                    List<String> rawPermissions = guildsSection.getStringList(guildKey + ".permissions." + command);
                    return resolveRoleAliases(guildId, rawPermissions);
                }
            }
        }
        return new ArrayList<String>();
    }
    
    private List<String> resolveRoleAliases(String guildId, List<String> rawPermissions) {
        List<String> resolvedRoles = new ArrayList<String>();
        Map<String, String> rootRoles = getRootRoles();
        Map<String, String> guildOverrides = getGuildRoleOverrides(guildId);
        
        for (String permission : rawPermissions) {
            if (permission == null || permission.isEmpty()) {
                continue;
            }
            
            String lowerPermission = permission.toLowerCase();
            
            // Check if it's a role alias
            if (guildOverrides.containsKey(lowerPermission)) {
                // Guild-specific override takes priority
                resolvedRoles.add(guildOverrides.get(lowerPermission));
                if (isDebugEnabled()) {
                    plugin.getLogger().info("Resolved role alias '" + permission + "' to guild override: " + guildOverrides.get(lowerPermission));
                }
            } else if (rootRoles.containsKey(lowerPermission)) {
                // Use root role mapping
                resolvedRoles.add(rootRoles.get(lowerPermission));
                if (isDebugEnabled()) {
                    plugin.getLogger().info("Resolved role alias '" + permission + "' to root role: " + rootRoles.get(lowerPermission));
                }
            } else if (isValidRoleId(permission)) {
                // Direct role ID - use as-is
                resolvedRoles.add(permission);
                if (isDebugEnabled()) {
                    plugin.getLogger().info("Using direct role ID: " + permission);
                }
            } else {
                // Unknown role alias - log warning and skip
                plugin.getLogger().warning("Unknown role alias '" + permission + "' in guild " + getGuildName(guildId) + 
                                         ". Please check your root_roles configuration or use a direct role ID.");
            }
        }
        
        return resolvedRoles;
    }
    
    private boolean isValidRoleId(String roleId) {
        // Discord role IDs are typically 17-19 digits long and numeric
        return roleId.matches("\\d{17,19}");
    }
    
    public String resolveRoleAlias(String guildId, String roleAlias) {
        Map<String, String> guildOverrides = getGuildRoleOverrides(guildId);
        Map<String, String> rootRoles = getRootRoles();
        String lowerAlias = roleAlias.toLowerCase();
        
        if (guildOverrides.containsKey(lowerAlias)) {
            return guildOverrides.get(lowerAlias);
        } else if (rootRoles.containsKey(lowerAlias)) {
            return rootRoles.get(lowerAlias);
        }
        
        return null; // Role alias not found
    }
    
    // Plugin Integration Settings
    public boolean isLiteBansEnabled() {
        return config.getBoolean("plugins.litebans.enabled", true);
    }
    
    public String getLiteBansBanCommand() {
        return config.getString("plugins.litebans.ban_command", "litebans:ban {player} {duration} {reason}");
    }
    
    public String getLiteBansUnbanCommand() {
        return config.getString("plugins.litebans.unban_command", "litebans:unban {player}");
    }
    
    public String getLiteBansTempBanCommand() {
        return config.getString("plugins.litebans.tempban_command", "litebans:tempban {player} {duration} {reason}");
    }
    
    public String getLiteBansBanIPCommand() {
        return config.getString("plugins.litebans.banip_command", "litebans:banip {player} {duration} {reason}");
    }
    
    public String getLiteBansUnbanIPCommand() {
        return config.getString("plugins.litebans.unbanip_command", "litebans:unbanip {player}");
    }
    
    public String getLiteBansUnbanAllCommand() {
        return config.getString("plugins.litebans.unbanall_command", "litebans:unbanall {player}");
    }
    
    public String getLiteBansHistoryCommand() {
        return config.getString("plugins.litebans.history_command", "litebans:history {player}");
    }
    
    public boolean isMuteEnabled() {
        return config.getBoolean("plugins.mute.enabled", true);
    }
    
    public String getMuteCommand() {
        return config.getString("plugins.mute.mute_command", "mute {player} {duration} {reason}");
    }
    
    public String getUnmuteCommand() {
        return config.getString("plugins.mute.unmute_command", "unmute {player}");
    }
    
    // Ban settings
    public int getMaxBanDurationDays() {
        return config.getInt("ban.max_duration_days", 0); // 0 = no limit
    }
    
    public int getLongBanWarningDays() {
        return config.getInt("ban.long_ban_warning_days", 365);
    }
    
    // Reload settings
    public String getRestartMessage() {
        return config.getString("reload.restart_message", "🔄 **Restart Pushed** - Plugin is reloading, please wait...");
    }
    
    public long getRestartDelay() {
        return config.getLong("reload.restart_delay", 2000);
    }
    
    // Help configuration
    public String getHelpTitle() {
        return config.getString("help.title", "MCDiscordBridge Commands");
    }
    
    public String getHelpDescription() {
        return config.getString("help.description", "Available commands for managing the Minecraft server");
    }
    
    public int getHelpColor() {
        return config.getInt("help.color", 0x00FF00);
    }
    
    public List<Map<String, String>> getHelpCommands() {
        List<Map<String, String>> commands = new ArrayList<Map<String, String>>();
        List<?> commandsList = config.getList("help.commands");
        
        if (commandsList != null) {
            for (Object cmdObj : commandsList) {
                if (cmdObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cmdMap = (Map<String, Object>) cmdObj;
                    Map<String, String> command = new HashMap<String, String>();
                    command.put("name", String.valueOf(cmdMap.get("name")));
                    command.put("description", String.valueOf(cmdMap.get("description")));
                    command.put("usage", String.valueOf(cmdMap.get("usage")));
                    commands.add(command);
                }
            }
        }
        
        return commands;
    }
    
    // Colors
    public int getColor(String type) {
        return config.getInt("colors." + type, 0x0099FF);
    }
    
    // Performance settings
    public int getTimeout() {
        return config.getInt("performance.timeout", 5000);
    }
    
    public boolean isDebugEnabled() {
        return config.getBoolean("performance.debug", false);
    }
    
    public int getThreadPoolSize() {
        return config.getInt("performance.thread_pool_size", 4);
    }
    
    // Messages
    public String getMessage(String key) {
        return config.getString("messages." + key, "Message not found: " + key);
    }
    
    public String getFormattedMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return message;
    }
}
