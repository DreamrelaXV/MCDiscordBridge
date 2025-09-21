package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class HelpCommand {
    
    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;
    
    public HelpCommand(MCDiscordBridge plugin) {
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
        if (!permissionUtils.hasPermission(event, "help")) {
            replyError(event, "Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "help"), true);
            return;
        }
        
        // Send help embed directly (no need to defer for help)
        event.replyEmbeds(embedUtils.createHelpEmbed()).queue();
        
        // Log action if debug enabled
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("[DISCORD HELP] " + event.getUser().getAsTag() + " requested help");
        }
    }
    
    // Utility methods
    private void replyError(SlashCommandInteractionEvent event, String title, String description, boolean ephemeral) {
        event.replyEmbeds(embedUtils.createErrorEmbed(title, description)).setEphemeral(ephemeral).queue();
    }
}