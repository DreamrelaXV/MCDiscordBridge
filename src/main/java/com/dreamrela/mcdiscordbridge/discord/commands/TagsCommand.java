package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

public class TagsCommand {

    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;

    public TagsCommand(MCDiscordBridge plugin) {
        this.plugin = plugin;
        this.embedUtils = new EmbedUtils(plugin);
        this.permissionUtils = new PermissionUtils(plugin);
    }

    public void handleAdd(SlashCommandInteractionEvent event) {
        if (!permissionUtils.isValidGuild(event)) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Invalid Server", "This bot is not configured for this server.")).setEphemeral(true).queue();
            return;
        }
        if (!permissionUtils.hasPermission(event, "tags-add")) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "tags-add"))).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        String player = getString(event, "player");
        String tag = getString(event, "tag");

        if (StringUtils.isAnyBlank(player, tag)) {
            event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Invalid Input", "Player and tag are required.")).queue();
            return;
        }

        plugin.getAsyncTaskRunner().runSync(() -> {
            try {
                String cmd = plugin.getConfig().getString("plugins.tags.add_command", "tags add {player} {tag}");
                cmd = cmd.replace("{player}", player).replace("{tag}", tag);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed("Tag Added", "Added tag '" + tag + "' to " + player + ".")).queue();
            } catch (Exception ex) {
                plugin.getLogger().severe("Error executing tags add: " + ex.getMessage());
                event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Command Error", "Failed to add tag.")).queue();
            }
        });
    }

    public void handleRemove(SlashCommandInteractionEvent event) {
        if (!permissionUtils.isValidGuild(event)) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Invalid Server", "This bot is not configured for this server.")).setEphemeral(true).queue();
            return;
        }
        if (!permissionUtils.hasPermission(event, "tags-remove")) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "tags-remove"))).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        String player = getString(event, "player");
        String tag = getString(event, "tag");
        if (StringUtils.isAnyBlank(player, tag)) {
            event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Invalid Input", "Player and tag are required.")).queue();
            return;
        }

        plugin.getAsyncTaskRunner().runSync(() -> {
            try {
                String cmd = plugin.getConfig().getString("plugins.tags.remove_command", "tags remove {player} {tag}");
                cmd = cmd.replace("{player}", player).replace("{tag}", tag);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed("Tag Removed", "Removed tag '" + tag + "' from " + player + ".")).queue();
            } catch (Exception ex) {
                plugin.getLogger().severe("Error executing tags remove: " + ex.getMessage());
                event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Command Error", "Failed to remove tag.")).queue();
            }
        });
    }

    private String getString(SlashCommandInteractionEvent event, String option) {
        OptionMapping opt = event.getOption(option);
        return opt != null ? opt.getAsString().trim() : "";
    }
}