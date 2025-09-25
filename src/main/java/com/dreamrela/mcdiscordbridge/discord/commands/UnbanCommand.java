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

public class UnbanCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public UnbanCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "unban")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "unban"), true);
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
        String reason = getStringOption(event, "reason");
        
        // Validate required inputs
        if (!validateInputs(event, playerInput, reason)) {
            return;
        }
        
        // Process unban asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeUnban(event, playerInput, reason);
            }
        });
    }
    
    private void executeUnban(SlashCommandInteractionEvent event, String playerInput, String reason) {
        try {
            // Get and validate player
            OfflinePlayer player = PlayerUtils.getOfflinePlayer(playerInput);
            if (player == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerInput);
                editError(event, "Player Not Found", plugin.getConfigManager().getFormattedMessage("player_not_found", placeholders));
                return;
            }
            
            String playerName = PlayerUtils.getDisplayNameForDiscord(player);
            String source = "Discord (" + event.getUser().getAsTag() + ")";
            
            // Execute unban on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executeUnbanSync(event, player, playerName, reason, source);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in unban command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while processing the unban.");
        }
    }
    
    private void executeUnbanSync(SlashCommandInteractionEvent event, OfflinePlayer player, 
                                 String playerName, String reason, String source) {
        try {
            // Use the improved unbanall command which unbans all types
            boolean success = PlayerUtils.unbanPlayerWithLiteBans(player, reason, source);
            
            if (success) {
                // Create success response
                event.getHook().editOriginalEmbeds(embedUtils.createPlayerActionEmbed(
                        "Unbanned", playerName, reason, null, true
                )).queue();
                
                // Log action
                plugin.getLogger().info(String.format(
                        "[DISCORD UNBAN - LITEBANS] %s unbanned %s (All ban types) - Reason: %s",
                        event.getUser().getAsTag(), playerName, reason
                ));
                
            } else {
                editError(event, "Unban Failed", 
                         "Failed to unban player **" + playerName + "** using LiteBans. Please check server logs for details.");
                plugin.getLogger().warning("Failed to unban player " + playerName + " with LiteBans, requested by " + event.getUser().getAsTag());
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing LiteBans unban for player " + playerName + ": " + e.getMessage());
            editError(event, "Unban Error", "An error occurred while unbanning **" + playerName + "** using LiteBans.");
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
