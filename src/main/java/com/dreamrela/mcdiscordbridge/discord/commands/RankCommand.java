package com.dreamrela.mcdiscordbridge.discord.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.utils.EmbedUtils;
import com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

public class RankCommand {

    private final MCDiscordBridge plugin;
    private final EmbedUtils embedUtils;
    private final PermissionUtils permissionUtils;

    public RankCommand(MCDiscordBridge plugin) {
        this.plugin = plugin;
        this.embedUtils = new EmbedUtils(plugin);
        this.permissionUtils = new PermissionUtils(plugin);
    }

    public void handleAdd(SlashCommandInteractionEvent event) {
        if (!permissionUtils.isValidGuild(event)) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Invalid Server", "This bot is not configured for this server.")).setEphemeral(true).queue();
            return;
        }
        if (!permissionUtils.hasPermission(event, "rank-add")) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "rank-add"))).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        String player = getString(event, "player");
        String rank = getString(event, "rank");
        String duration = getString(event, "duration");

        if (StringUtils.isAnyBlank(player, rank, duration)) {
            event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Invalid Input", "Player, rank, and duration are required.")).queue();
            return;
        }

        plugin.getAsyncTaskRunner().runSync(() -> {
            try {
                String cmd;
                if ("permanent".equalsIgnoreCase(duration)) {
                    cmd = plugin.getConfig().getString("plugins.luckperms.add_parent_command", "lp user {player} parent add {rank}");
                } else {
                    cmd = plugin.getConfig().getString("plugins.luckperms.addtemp_parent_command", "lp user {player} parent addtemp {rank} {duration}");
                }
                cmd = cmd.replace("{player}", player).replace("{rank}", rank).replace("{duration}", duration);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed("Rank Added", "Applied rank '" + rank + "' to " + player + ("permanent".equalsIgnoreCase(duration) ? " permanently." : " for " + duration + "."))).queue();
            } catch (Exception ex) {
                plugin.getLogger().severe("Error executing rank add: " + ex.getMessage());
                event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Command Error", "Failed to add rank.")).queue();
            }
        });
    }

    public void handleRemove(SlashCommandInteractionEvent event) {
        if (!permissionUtils.isValidGuild(event)) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Invalid Server", "This bot is not configured for this server.")).setEphemeral(true).queue();
            return;
        }
        if (!permissionUtils.hasPermission(event, "rank-remove")) {
            event.replyEmbeds(embedUtils.createErrorEmbed("Permission Denied", permissionUtils.getPermissionDeniedMessage(event, "rank-remove"))).setEphemeral(true).queue();
            return;
        }

        event.deferReply().queue();

        String player = getString(event, "player");
        String rank = getString(event, "rank");
        if (StringUtils.isAnyBlank(player, rank)) {
            event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Invalid Input", "Player and rank are required.")).queue();
            return;
        }

        plugin.getAsyncTaskRunner().runSync(() -> {
            try {
                String cmd = plugin.getConfig().getString("plugins.luckperms.remove_parent_command", "lp user {player} parent remove {rank}");
                cmd = cmd.replace("{player}", player).replace("{rank}", rank);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                event.getHook().editOriginalEmbeds(embedUtils.createSuccessEmbed("Rank Removed", "Removed rank '" + rank + "' from " + player + ".")).queue();
            } catch (Exception ex) {
                plugin.getLogger().severe("Error executing rank remove: " + ex.getMessage());
                event.getHook().editOriginalEmbeds(embedUtils.createErrorEmbed("Command Error", "Failed to remove rank.")).queue();
            }
        });
    }

    private String getString(SlashCommandInteractionEvent event, String option) {
        OptionMapping opt = event.getOption(option);
        return opt != null ? opt.getAsString().trim() : "";
    }
}