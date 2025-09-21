package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class ReloadCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public ReloadCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "reload")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "reload"), true);
            return;
        }
        
        // Acknowledge interaction and send initial message
        event.deferReply().queue();
        
        String restartMessage = plugin.getConfigManager().getRestartMessage();
        event.getHook().editOriginalEmbeds(embedUtils.createWarningEmbed("Restart Pushed", restartMessage)).queue();
        
        // Process reload with delay
        long delay = plugin.getConfigManager().getRestartDelay();
        
        plugin.getAsyncTaskRunner().runAsync(new Runnable() {
            @Override
            public void run() {
                executeReload(event, delay);
            }
        });
    }
    
    private void executeReload(SlashCommandInteractionEvent event, long delay) {
        try {
            // Wait for the specified delay
            Thread.sleep(delay);
            
            // Send success message BEFORE reloading to avoid JDA shutdown issues
            try {
                event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed(
                        "Plugin Reloading",
                        "Plugin reload initiated... Please check console for completion status."
                )).queue();
            } catch (Exception e) {
                // Ignore Discord message errors during reload
                plugin.getLogger().info("Could not send Discord reload message (expected during reload): " + e.getMessage());
            }
            
            // Small delay to ensure message is sent before shutdown
            Thread.sleep(1000);
            
            // Log action BEFORE reloading
            plugin.getLogger().info("[DISCORD RELOAD] Plugin reload initiated by " + event.getUser().getAsTag());
            
            // Actually reload the plugin (this will shutdown JDA)
            plugin.reloadPlugin();
            
            // Note: Any code after reloadPlugin() may not execute reliably
            // as the plugin is being reloaded
            
        } catch (InterruptedException e) {
            plugin.getLogger().warning("Reload delay interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            // Don't try to send Discord message here as JDA might be shut down
        } catch (Exception e) {
            plugin.getLogger().severe("Error reloading plugin: " + e.getMessage());
            // Don't try to send Discord message here as JDA might be shut down
        }
    }
    
    // Utility methods
    private void replyError(SlashCommandInteractionEvent event, String title, String description, boolean ephemeral) {
        try {
            event.replyEmbeds(embedUtils.createErrorEmbed(title, description)).setEphemeral(ephemeral).queue();
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send Discord error message: " + e.getMessage());
        }
    }
}