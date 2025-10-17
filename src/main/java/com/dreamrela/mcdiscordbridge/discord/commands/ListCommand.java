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
            
            if (onlineCount == 0) {
                event.getHook().editOriginalEmbeds(embedUtils.createPlayerListEmbed("", onlineCount, maxPlayers)).queue();
                return;
            }

            java.util.List<String> names = new java.util.ArrayList<String>();
            for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
                names.add(PlayerUtils.getDisplayNameForDiscord(Bukkit.getOfflinePlayer(p.getUniqueId())));
            }

            java.util.List<net.dv8tion.jda.api.entities.MessageEmbed> pages = embedUtils.createPlayerListEmbeds(names, onlineCount, maxPlayers);

            // Send first page by editing original reply
            event.getHook().editOriginalEmbeds(pages.get(0)).queue();

            // Send remaining pages as followups to avoid field limits and description overflows
            for (int i = 1; i < pages.size(); i++) {
                event.getHook().sendMessageEmbeds(pages.get(i)).queue();
            }
            
            // Log action
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info(String.format("[DISCORD LIST] %s requested player list (%d/%d online)%s",
                                                     event.getUser().getAsTag(), onlineCount, maxPlayers,
                                                     pages.size() > 1 ? " - " + pages.size() + " pages" : ""));
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