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
    
    public List<String> getCommandPermissions(String guildId, String command) {
        ConfigurationSection guildsSection = config.getConfigurationSection("discord.guilds");
        if (guildsSection != null) {
            for (String guildKey : guildsSection.getKeys(false)) {
                if (guildId.equals(guildsSection.getString(guildKey + ".id"))) {
                    return guildsSection.getStringList(guildKey + ".permissions." + command);
                }
            }
        }
        return new ArrayList<String>();
    }
    
    // Plugin Integration Settings
    public boolean isLiteBansEnabled() {
        return config.getBoolean("plugins.litebans.enabled", true);
    }
    
    public String getLiteBansBanCommand() {
        return config.getString("plugins.litebans.ban_command", "litebans:ban {player} {duration} {reason} -s");
    }
    
    public String getLiteBansUnbanCommand() {
        return config.getString("plugins.litebans.unban_command", "litebans:unban {player} {reason} -s");
    }
    
    public String getLiteBansTempBanCommand() {
        return config.getString("plugins.litebans.tempban_command", "litebans:tempban {player} {duration} {reason} -s");
    }
    
    public String getLiteBansBanIPCommand() {
        return config.getString("plugins.litebans.banip_command", "litebans:banip {player} {duration} {reason} -s");
    }
    
    public String getLiteBansUnbanIPCommand() {
        return config.getString("plugins.litebans.unbanip_command", "litebans:unbanip {player} {reason} -s");
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