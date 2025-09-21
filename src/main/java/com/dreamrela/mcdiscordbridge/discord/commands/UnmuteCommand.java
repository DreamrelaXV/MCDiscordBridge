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

public class UnmuteCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public UnmuteCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "unmute")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "unmute"), true);
            return;
        }
        
        // Acknowledge interaction and get options
        event.deferReply().queue();
        
        String playerInput = getStringOption(event, "player");
        String reason = getStringOption(event, "reason");
        
        // Validate required inputs
        if (!validateInputs(event, playerInput, reason)) {
            return;
        }
        
        // Process unmute asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeUnmute(event, playerInput, reason);
            }
        });
    }
    
    private void executeUnmute(SlashCommandInteractionEvent event, String playerInput, String reason) {
        try {
            // Get and validate player
            OfflinePlayer player = PlayerUtils.getOfflinePlayer(playerInput);
            if (player == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerInput);
                editError(event, "Player Not Found", plugin.getConfigManager().getFormattedMessage("player_not_found", placeholders));
                return;
            }
            
            String playerName = PlayerUtils.getDisplayNameForDiscord(player);
            
            // Execute unmute on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executeUnmuteSync(event, player, playerName, reason);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in unmute command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while processing the unmute.");
        }
    }
    
    private void executeUnmuteSync(SlashCommandInteractionEvent event, OfflinePlayer player, String playerName, String reason) {
        try {
            boolean success = unmutePlayer(player);
            
            if (success) {
                // Create success response
                event.getHook().editOriginalEmbeds(embedUtils.createPlayerActionEmbed(
                        "Unmuted", playerName, reason, null, true
                )).queue();
                
                // Log action
                plugin.getLogger().info(String.format("[DISCORD UNMUTE] %s unmuted %s - Reason: %s",
                                                     event.getUser().getAsTag(), playerName, reason));
                
            } else {
                editError(event, "Unmute Failed", "Failed to unmute player **" + playerName + "**.");
                plugin.getLogger().warning("Failed to unmute player " + playerName + " requested by " + event.getUser().getAsTag());
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing unmute for player " + playerName + ": " + e.getMessage());
            editError(event, "Unmute Error", "An error occurred while unmuting **" + playerName + "**.");
        }
    }
    
    private boolean unmutePlayer(OfflinePlayer player) {
        try {
            String playerName = PlayerUtils.getDisplayName(player);
            
            // Use configured unmute command
            String unmuteCommand = plugin.getConfigManager().getUnmuteCommand()
                    .replace("{player}", playerName);
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), unmuteCommand);
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to unmute player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    // Utility methods
    private boolean validateInputs(SlashCommandInteractionEvent event, String playerInput, String reason) {
        if (StringUtils.isBlank(playerInput)) {
            editError(event, "Invalid Input", "Player name/UUID cannot be empty.");
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