package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class BroadcastCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public BroadcastCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "broadcast")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "broadcast"), true);
            return;
        }
        
        // Acknowledge interaction and get options
        event.deferReply().queue();
        
        String text = getStringOption(event, "text");
        
        // Validate required inputs
        if (StringUtils.isBlank(text)) {
            editError(event, "Invalid Input", "Broadcast message cannot be empty.");
            return;
        }
        
        // Process broadcast on main thread
        plugin.getAsyncTaskRunner().runSync(new Runnable() {
            @Override
            public void run() {
                executeBroadcast(event, text);
            }
        });
    }
    
    private void executeBroadcast(SlashCommandInteractionEvent event, String text) {
        try {
            // Format and broadcast message
            String message = ChatColor.translateAlternateColorCodes('&', "&7[&bDiscord&7] &r" + text);
            Bukkit.broadcastMessage(message);
            
            // Create success response
            event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed(
                    "Message Broadcasted",
                    plugin.getConfigManager().getMessage("broadcast_success") + "\n\n**Message:** " + text
            )).queue();
            
            // Log action
            plugin.getLogger().info(String.format("[DISCORD BROADCAST] %s broadcasted: %s",
                                                 event.getUser().getAsTag(), text));
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing broadcast command: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while broadcasting the message.");
        }
    }
    
    // Utility methods
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