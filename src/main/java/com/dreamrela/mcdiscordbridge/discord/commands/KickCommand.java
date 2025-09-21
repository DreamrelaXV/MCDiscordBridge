package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import com.dreamrela.mcdiscordbridge.utils.PlayerUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class KickCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public KickCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "kick")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "kick"), true);
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
        
        // Process kick asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeKick(event, playerInput, reason);
            }
        });
    }
    
    private void executeKick(SlashCommandInteractionEvent event, String playerInput, String reason) {
        try {
            // Get and validate online player
            Player player = PlayerUtils.getOnlinePlayer(playerInput);
            if (player == null) {
                editError(event, "Player Not Online", 
                         "❌ Player **" + PlayerUtils.escapeDiscordFormatting(playerInput) + "** is not online!");
                return;
            }
            
            String playerName = PlayerUtils.getDisplayNameForDiscord(Bukkit.getOfflinePlayer(player.getUniqueId()));
            String fullReason = "Kicked from discord: " + reason;
            
            // Execute kick on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executeKickSync(event, player, playerName, fullReason, reason);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in kick command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while processing the kick.");
        }
    }
    
    private void executeKickSync(SlashCommandInteractionEvent event, Player player, String playerName, String fullReason, String reason) {
        try {
            player.kickPlayer(fullReason);
            
            // Create success response
            event.getHook().editOriginalEmbeds(embedUtils.createPlayerActionEmbed(
                    "Kicked", playerName, reason, null, true
            )).queue();
            
            // Log action
            plugin.getLogger().info(String.format("[DISCORD KICK] %s kicked %s - Reason: %s",
                                                 event.getUser().getAsTag(), playerName, reason));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing kick for player " + playerName + ": " + e.getMessage());
            editError(event, "Kick Failed", "Failed to kick player **" + playerName + "**.");
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
    
    private void replyError(SlashCommandInteractionEvent event, String title, String description, boolean ephemeral) {
        event.replyEmbeds(embedUtils.createErrorEmbed(title, description)).setEphemeral(ephemeral).queue();
    }
    
    private void editError(SlashCommandInteractionEvent event, String title, String description) {
        event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed(title, description)).queue();
    }
}