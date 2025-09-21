package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import com.dreamrela.mcdiscordbridge.utils.PlayerUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bukkit.Bukkit;

public class ListCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public ListCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "list")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "list"), true);
            return;
        }
        
        // Acknowledge interaction
        event.deferReply().queue();
        
        // Process player list on main thread
        plugin.getAsyncTaskRunner().runSync(new Runnable() {
            @Override
            public void run() {
                executeList(event);
            }
        });
    }
    
    private void executeList(SlashCommandInteractionEvent event) {
        try {
            int onlineCount = Bukkit.getOnlinePlayers().size();
            int maxPlayers = Bukkit.getMaxPlayers();
            
            String playerNames = onlineCount == 0 ? "" : PlayerUtils.formatPlayerListForDiscord(Bukkit.getOnlinePlayers());
            
            // Create and send player list embed
            event.getHook().editOriginalEmbeds(embedUtils.createPlayerListEmbed(
                    playerNames, onlineCount, maxPlayers
            )).queue();
            
            // Log action
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format("[DISCORD LIST] %s requested player list (%d/%d online)",
                                                     event.getUser().getAsTag(), onlineCount, maxPlayers));
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing list command: " + e.getMessage());
            editError(event, "Command Error", "An unexpected error occurred while getting the player list.");
        }
    }
    
    // Utility methods
    private void replyError(SlashCommandInteractionEvent event, String title, String description, boolean ephemeral) {
        event.replyEmbeds(embedUtils.createErrorEmbed(title, description)).setEphemeral(ephemeral).queue();
    }
    
    private void editError(SlashCommandInteractionEvent event, String title, String description) {
        event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed(title, description)).queue();
    }
}