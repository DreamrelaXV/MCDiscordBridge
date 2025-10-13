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
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MuteCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public MuteCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "mute")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "mute"), true);
            return;
        }
        
        // Acknowledge interaction and get options
        event.deferReply().queue();
        
        String playerInput = getStringOption(event, "player");
        String durationInput = getStringOption(event, "duration");
        String reason = getStringOption(event, "reason");
        
        // Validate required inputs
        if (!validateInputs(event, playerInput, durationInput, reason)) {
            return;
        }
        
        // Process mute asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeMute(event, playerInput, durationInput, reason);
            }
        });
    }
    
    private void executeMute(SlashCommandInteractionEvent event, String playerInput, String durationInput, String reason) {
        try {
            // Parse and validate duration
            long durationMillis = PlayerUtils.parseDuration(durationInput);
            if (durationMillis <= 0) {
                editError(event, "Invalid Duration", "Invalid duration format. Use examples like: `1h`, `30m`, `2d`");
                return;
            }
            
            // Check duration limits
            long maxDuration = TimeUnit.DAYS.toMillis(30);
            if (durationMillis > maxDuration) {
                editError(event, "Duration Too Long", "Maximum mute duration is 30 days. Please use a shorter duration.");
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
            String fullReason = "Muted on discord: " + reason;
            
            // Execute mute on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executeMuteSync(event, player, playerName, fullReason, reason, durationMillis, source);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in mute command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while processing the mute.");
        }
    }
    
    private void executeMuteSync(SlashCommandInteractionEvent event, OfflinePlayer player, String playerName, 
                                String fullReason, String reason, long durationMillis, String source) {
        try {
            boolean success = mutePlayer(player, fullReason, durationMillis);
            
            if (success) {
                // Create success response
                event.getHook().editOriginalEmbeds(embedUtils.createPlayerActionEmbed(
                        "Muted", playerName, reason, PlayerUtils.formatDuration(durationMillis), true
                )).queue();
                
                // Log action
                plugin.getLogger().info(String.format(
                        "[DISCORD MUTE] %s muted %s for %s - Reason: %s",
                        event.getUser().getAsTag(), playerName, 
                        PlayerUtils.formatDuration(durationMillis), reason
                ));
                
            } else {
                editError(event, "Mute Failed", 
                         "Failed to mute player **" + playerName + "**. Please check server logs for details.");
                plugin.getLogger().warning("Failed to mute player " + playerName + " requested by " + event.getUser().getAsTag());
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing mute for player " + playerName + ": " + e.getMessage());
            editError(event, "Mute Error", "An error occurred while muting **" + playerName + "**.");
        }
    }
    
    private boolean mutePlayer(OfflinePlayer player, String reason, long durationMillis) {
        try {
            String playerName = PlayerUtils.getDisplayName(player);
            String durationString = PlayerUtils.formatDuration(durationMillis);
            
            // Use configured mute command
            String muteCommand = plugin.getConfigManager().getMuteCommand()
                    .replace("{player}", playerName)
                    .replace("{duration}", durationString)
                    .replace("{reason}", reason);
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), muteCommand);
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to mute player " + player.getName() + ": " + e.getMessage());
            return false;
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
}