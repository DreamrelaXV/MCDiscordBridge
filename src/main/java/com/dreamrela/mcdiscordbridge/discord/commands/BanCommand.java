package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import com.dreamrela.mcdiscordbridge.utils.PlayerUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BanCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public BanCommand(MCDiscordBridge plugin) {
        this.plugin = plugin;
        this.embedUtils = new EmbedUtils(plugin);
        this.permissionUtils = new PermissionUtils(plugin);
    }
    
    public void handle(SlashCommandInteractionEvent event) {
        // Validate guild configuration
        if (!permissionUtils.isValidGuild(event)) {
            replyError(event, "Invalid Server", "This bot is not configured for this server.", true);
            return;
        }
        
        // Check user permissions
        if (!permissionUtils.hasPermission(event, "ban")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "ban"), true);
            return;
        }
        
        // Check if LiteBans is available
        if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
            replyError(event, "LiteBans Not Found", plugin.getConfigManager().getMessage("litebans_not_found"), true);
            return;
        }
        
        // Acknowledge interaction and get options
        event.deferReply().queue();
        
        String playerInput = getStringOption(event, "player");
        String durationInput = getStringOption(event, "duration");
        String reason = getStringOption(event, "reason");
        boolean ipBan = getBooleanOption(event, "ip-ban");
        
        // Validate required inputs
        if (!validateInputs(event, playerInput, durationInput, reason)) {
            return;
        }
        
        // Process ban asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeBan(event, playerInput, durationInput, reason, ipBan);
            }
        });
    }
    
    private void executeBan(SlashCommandInteractionEvent event, String playerInput, String durationInput, String reason, boolean ipBan) {
        try {
            // Parse and validate duration
            long durationMillis = PlayerUtils.parseDuration(durationInput);
            if (durationMillis <= 0) {
                editError(event, "Invalid Duration", 
                         "Invalid duration format. Use examples like: `7d`, `2h30m`, `45s`\n\n" +
                         "**Supported units:** s(econds), m(inutes), h(ours), d(ays), w(eeks), mo(nths), y(ears)");
                return;
            }
            
            // Check duration limits
            long maxDuration = TimeUnit.DAYS.toMillis(365);
            if (durationMillis > maxDuration) {
                editError(event, "Duration Too Long", "Maximum ban duration is 1 year. Please use a shorter duration.");
                return;
            }
            
            // Get and validate player
            OfflinePlayer player = PlayerUtils.getOfflinePlayer(playerInput);
            if (player == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerInput);
                editError(event, "Player Not Found", plugin.getConfigManager().getFormattedMessage("player_not_found", placeholders));
                return;
            }
            
            String playerName = PlayerUtils.getDisplayNameForDiscord(player);
            String source = "Discord (" + event.getUser().getAsTag() + ")";
            
            // Execute ban on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executeBanSync(event, player, playerName, reason, durationMillis, ipBan, source);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in ban command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while processing the ban.");
        }
    }
    
    private void executeBanSync(SlashCommandInteractionEvent event, OfflinePlayer player, String playerName, 
                               String reason, long durationMillis, boolean ipBan, String source) {
        try {
            boolean success;
            
            if (ipBan) {
                success = PlayerUtils.banPlayerIPWithLiteBans(player, reason, durationMillis, source);
                PlayerUtils.unbanPlayerIPWithLiteBans(player, "Applying new IP ban", source);
            } else {
                success = PlayerUtils.banPlayerWithLiteBans(player, reason, durationMillis, source);
            }
            
            if (success) {
                // Create success response
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("player", playerName);
                placeholders.put("duration", PlayerUtils.formatDuration(durationMillis));
                placeholders.put("reason", reason);
                placeholders.put("ipban", ipBan ? "Yes" : "No");
                
                event.getHook().editOriginalEmbeds(embedUtils.createPlayerActionEmbed(
                        "Banned", playerName, reason, PlayerUtils.formatDuration(durationMillis), true
                )).queue();
                
                // Log action
                logAction("BAN - LITEBANS", event.getUser().getAsTag(), playerName, 
                         PlayerUtils.formatDuration(durationMillis), String.valueOf(ipBan), reason);
                
                if (ipBan) {
                    plugin.getLogger().info("LiteBans IP ban applied for player: " + playerName);
                }
                
            } else {
                editError(event, "Ban Failed", "Failed to ban player **" + playerName + "** using LiteBans. Please check server logs for details.");
                plugin.getLogger().warning("Failed to ban player " + playerName + " with LiteBans, requested by " + event.getUser().getAsTag());
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing LiteBans ban for player " + playerName + ": " + e.getMessage());
            editError(event, "Ban Error", "An error occurred while banning **" + playerName + "** using LiteBans.");
        }
    }
    
    // Utility methods
    private boolean validateInputs(SlashCommandInteractionEvent event, String playerInput, String durationInput, String reason) {
        if (StringUtils.isBlank(playerInput)) {
            editError(event, "Invalid Input", "Player name/UUID cannot be empty.");
            return false;
        }
        if (StringUtils.isBlank(durationInput)) {
            editError(event, "Invalid Input", "Duration cannot be empty.");
            return false;
        }
        if (StringUtils.isBlank(reason)) {
            editError(event, "Invalid Input", "Reason cannot be empty.");
            return false;
        }
        return true;
    }
    
    private String getStringOption(SlashCommandInteractionEvent event, String name) {
        OptionMapping option = event.getOption(name);
        return option != null ? option.getAsString().trim() : "";
    }
    
    private boolean getBooleanOption(SlashCommandInteractionEvent event, String name) {
        OptionMapping option = event.getOption(name);
        return option != null && option.getAsBoolean();
    }
    
    private Map<String, String> createPlaceholders(String key, String value) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put(key, value);
        return placeholders;
    }
    
    private void replyError(SlashCommandInteractionEvent event, String title, String description, boolean ephemeral) {
        event.replyEmbeds(embedUtils.createErrorEmbed(title, description)).setEphemeral(ephemeral).queue();
    }
    
    private void editError(SlashCommandInteractionEvent event, String title, String description) {
        event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed(title, description)).queue();
    }
    
    private void logAction(String action, String user, String player, String duration, String extra, String reason) {
        plugin.getLogger().info(String.format("[DISCORD %s] %s -> %s for %s (%s) - Reason: %s", 
                                             action, user, player, duration, extra, reason));
    }
}