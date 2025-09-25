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

public class PunishmentListCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public PunishmentListCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "punishment-list")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "punishment-list"), true);
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
        String punishmentType = getStringOption(event, "punishment");
        
        // Validate required inputs
        if (!validateInputs(event, playerInput, punishmentType)) {
            return;
        }
        
        // Process punishment list request asynchronously
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executePunishmentList(event, playerInput, punishmentType);
            }
        });
    }
    
    private void executePunishmentList(SlashCommandInteractionEvent event, String playerInput, String punishmentType) {
        try {
            // Get and validate player
            OfflinePlayer player = PlayerUtils.getOfflinePlayer(playerInput);
            if (player == null) {
                Map<String, String> placeholders = createPlaceholders("player", playerInput);
                editError(event, "Player Not Found", plugin.getConfigManager().getFormattedMessage("player_not_found", placeholders));
                return;
            }
            
            String playerName = PlayerUtils.getDisplayNameForDiscord(player);
            
            // Execute punishment list check on main thread
            plugin.getAsyncTaskRunner().runSync(new Runnable() {
                @Override
                public void run() {
                    executePunishmentListSync(event, player, playerName, punishmentType);
                }
            });
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in punishment list command execution: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while fetching punishment history.");
        }
    }
    
    private void executePunishmentListSync(SlashCommandInteractionEvent event, OfflinePlayer player, 
                                          String playerName, String punishmentType) {
        try {
            // Execute LiteBans history command to get punishment data
            String command = plugin.getConfigManager().getLiteBansHistoryCommand()
                    .replace("{player}", PlayerUtils.getDisplayName(player));
            
            // Note: This is a simplified implementation. In a real scenario, you'd need to:
            // 1. Capture the command output 
            // 2. Parse the LiteBans response
            // 3. Filter by punishment type
            // For now, we'll just execute the command and provide a general response
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            // Create response embed
            String description = buildPunishmentListDescription(playerName, punishmentType);
            
            event.getHook().editOriginalEmbeds(embedUtils.createCustomEmbed(
                    "📋 Punishment History",
                    description,
                    "punishment"
            )).queue();
            
            // Log action
            plugin.getLogger().info(String.format(
                    "[DISCORD PUNISHMENT-LIST] %s requested %s history for %s",
                    event.getUser().getAsTag(), punishmentType, playerName
            ));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing punishment list for player " + playerName + ": " + e.getMessage());
            editError(event, "Punishment List Error", 
                     "An error occurred while fetching punishment history for **" + playerName + "**.");
        }
    }
    
    private String buildPunishmentListDescription(String playerName, String punishmentType) {
        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(playerName).append("\n");
        description.append("**Punishment Type:** ").append(StringUtils.capitalize(punishmentType)).append("\n\n");
        
        // Note: This is a placeholder implementation
        // In a real implementation, you would:
        // 1. Query LiteBans database directly
        // 2. Or capture and parse command output
        // 3. Format the punishment history properly
        
        description.append("⚠️ **Note:** Detailed punishment history has been logged to console.\n");
        description.append("Check the server console or LiteBans web interface for complete details.\n\n");
        description.append("**Command executed:** `/litebans:history ").append(PlayerUtils.getDisplayName(Bukkit.getOfflinePlayer(playerName))).append("`");
        
        return description.toString();
    }
    
    // Utility methods
    private boolean validateInputs(SlashCommandInteractionEvent event, String playerInput, String punishmentType) {
        if (StringUtils.isBlank(playerInput)) {
            editError(event, "Invalid Input", "Player name/UUID cannot be empty.");
            return false;
        }
        if (StringUtils.isBlank(punishmentType)) {
            editError(event, "Invalid Input", "Punishment type cannot be empty.");
            return false;
        }
        
        // Validate punishment type
        String[] validTypes = {"bans", "mutes", "kicks", "warnings", "all"};
        boolean validType = false;
        for (String type : validTypes) {
            if (type.equalsIgnoreCase(punishmentType)) {
                validType = true;
                break;
            }
        }
        
        if (!validType) {
            editError(event, "Invalid Punishment Type", 
                     "Valid punishment types: `bans`, `mutes`, `kicks`, `warnings`, `all`");
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
