package com.dreamrela.mcdiscordbridge.discord;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import com.dreamrela.mcdiscordbridge.discord.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DiscordBot extends ListenerAdapter {
    
    private final MCDiscordBridge plugin;
    private JDA jda;
    
    // Command handlers
    private final BanCommand banCommand;
    private final UnbanCommand unbanCommand;
    private final MuteCommand muteCommand;
    private final UnmuteCommand unmuteCommand;
    private final KickCommand kickCommand;
    private final ListCommand listCommand;
    private final BroadcastCommand broadcastCommand;
    private final ReloadCommand reloadCommand;
    private final HelpCommand helpCommand;
    private final PunishmentListCommand punishmentListCommand;
    private final RankCommand rankCommand;
    private final TagsCommand tagsCommand;
    
    public DiscordBot(MCDiscordBridge plugin) {
        this.plugin = plugin;
        this.banCommand = new BanCommand(plugin);
        this.unbanCommand = new UnbanCommand(plugin);
        this.muteCommand = new MuteCommand(plugin);
        this.unmuteCommand = new UnmuteCommand(plugin);
        this.kickCommand = new KickCommand(plugin);
        this.listCommand = new ListCommand(plugin);
        this.broadcastCommand = new BroadcastCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.helpCommand = new HelpCommand(plugin);
        this.punishmentListCommand = new PunishmentListCommand(plugin);
        this.rankCommand = new RankCommand(plugin);
        this.tagsCommand = new TagsCommand(plugin);
    }
    
    public void initialize() throws Exception {
        String token = plugin.getConfigManager().getDiscordToken();
        
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            throw new IllegalArgumentException("Discord bot token not configured!");
        }
        
        try {
            // Build JDA with necessary intents
            this.jda = JDABuilder.createDefault(token)
                    .enableIntents(EnumSet.of(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                    ))
                    .disableCache(EnumSet.of(
                        CacheFlag.VOICE_STATE,
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
                        CacheFlag.SCHEDULED_EVENTS
                    ))
                    .addEventListeners(this)
                    .setLargeThreshold(50)
                    .build();
            
            // Wait for the bot to be ready
            jda.awaitReady();
            
            // Set activity
            setActivity();
            
            // Register slash commands for all configured guilds
            registerSlashCommands();
            
            plugin.getLogger().info("Discord bot logged in as: " + jda.getSelfUser().getAsTag());
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
            throw e;
        }
    }
    
    private void setActivity() {
        try {
            String activityType = plugin.getConfigManager().getActivityType();
            String activityText = plugin.getConfigManager().getActivityText();
            
            Activity.ActivityType type;
            try {
                type = Activity.ActivityType.valueOf(activityType.toUpperCase());
            } catch (IllegalArgumentException e) {
                type = Activity.ActivityType.WATCHING;
                plugin.getLogger().warning("Invalid activity type '" + activityType + "', using WATCHING");
            }
            
            Activity activity = Activity.of(type, activityText);
            jda.getPresence().setActivity(activity);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to set bot activity: " + e.getMessage());
        }
    }
    
    private void registerSlashCommands() {
        List<String> guildIds = plugin.getConfigManager().getGuildIds();
        
        if (guildIds.isEmpty()) {
            plugin.getLogger().warning("No guilds configured! Slash commands will not be registered.");
            return;
        }
        
        List<CommandData> commands = createSlashCommands();
        
        for (String guildId : guildIds) {
            try {
                if (jda.getGuildById(guildId) != null) {
                    jda.getGuildById(guildId).updateCommands().addCommands(commands).queue(
                        success -> plugin.getLogger().info("Registered " + commands.size() + " slash commands for guild: " + plugin.getConfigManager().getGuildName(guildId)),
                        error -> plugin.getLogger().severe("Failed to register commands for guild " + guildId + ": " + error.getMessage())
                    );
                } else {
                    plugin.getLogger().warning("Guild not found or bot not in guild: " + guildId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register commands for guild " + guildId + ": " + e.getMessage());
            }
        }
    }
    
    private List<CommandData> createSlashCommands() {
        List<CommandData> commands = new ArrayList<CommandData>();
        
        // Ban command
        commands.add(Commands.slash("mc-ban", "Ban a player from the server")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "duration", "Ban duration (e.g., 7d, 2h, 30m)", true)
                                .setMaxLength(20),
                        new OptionData(OptionType.STRING, "reason", "Reason for the ban", true)
                                .setMaxLength(200)
                ));
        
        // Unban command
        commands.add(Commands.slash("mc-unban", "Unban a player from the server")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "reason", "Reason for the unban", true)
                                .setMaxLength(200)
                ));
        
        // Mute command
        commands.add(Commands.slash("mc-mute", "Mute a player on the server")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "duration", "Mute duration (e.g., 1h, 30m)", true)
                                .setMaxLength(20),
                        new OptionData(OptionType.STRING, "reason", "Reason for the mute", true)
                                .setMaxLength(200)
                ));
        
        // Unmute command
        commands.add(Commands.slash("mc-unmute", "Unmute a player on the server")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "reason", "Reason for the unmute", true)
                                .setMaxLength(200)
                ));
        
        // Kick command
        commands.add(Commands.slash("mc-kick", "Kick a player from the server")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "reason", "Reason for the kick", true)
                                .setMaxLength(200)
                ));
        
        // Punishment list command
        commands.add(Commands.slash("punishment-list", "View player punishment history")
                .addOptions(
                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true)
                                .setMaxLength(36),
                        new OptionData(OptionType.STRING, "punishment", "Type of punishment to view", true)
                                .addChoice("All Punishments", "all")
                                .addChoice("Bans", "bans")
                                .addChoice("Mutes", "mutes")
                                .addChoice("Kicks", "kicks")
                                .addChoice("Warnings", "warnings")
                ));
        
        // Rank management (LuckPerms)
        commands.add(Commands.slash("rank", "Manage LuckPerms ranks")
                .addSubcommands(
                        new SubcommandData("add", "Add a rank to a player (permanent or temporary)")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true).setMaxLength(36),
                                        new OptionData(OptionType.STRING, "rank", "LuckPerms group name", true).setMaxLength(64),
                                        new OptionData(OptionType.STRING, "duration", "'permanent' or duration like 7d/12h", true).setMaxLength(32)
                                ),
                        new SubcommandData("remove", "Remove a rank from a player")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true).setMaxLength(36),
                                        new OptionData(OptionType.STRING, "rank", "LuckPerms group name", true).setMaxLength(64)
                                )
                ));
        
        // Tags management (dispatches to server /tags)
        commands.add(Commands.slash("tags", "Manage player tags")
                .addSubcommands(
                        new SubcommandData("add", "Add a tag to a player")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true).setMaxLength(36),
                                        new OptionData(OptionType.STRING, "tag", "Tag identifier", true).setMaxLength(64)
                                ),
                        new SubcommandData("remove", "Remove a tag from a player")
                                .addOptions(
                                        new OptionData(OptionType.STRING, "player", "Player name or UUID", true).setMaxLength(36),
                                        new OptionData(OptionType.STRING, "tag", "Tag identifier", true).setMaxLength(64)
                                )
                ));
        
        // Reload command
        commands.add(Commands.slash("mc-dc-reload", "Reload the plugin configuration"));
        
        // Help command
        commands.add(Commands.slash("help", "Show available commands"));
        
        return commands;
    }
    
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Log command usage if debug is enabled
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Command used: /" + event.getName() + " by " + event.getUser().getAsTag() + 
                                  " in guild: " + (event.getGuild() != null ? event.getGuild().getName() : "DM"));
        }
        
        String commandName = event.getName();
        
        try {
            // Handle commands
            switch (commandName) {
                case "mc-ban":
                    banCommand.handle(event);
                    break;
                case "mc-unban":
                    unbanCommand.handle(event);
                    break;
                case "mc-mute":
                    muteCommand.handle(event);
                    break;
                case "mc-unmute":
                    unmuteCommand.handle(event);
                    break;
                case "mc-kick":
                    kickCommand.handle(event);
                    break;
                case "list":
                    listCommand.handle(event);
                    break;
                case "brc":
                    broadcastCommand.handle(event);
                    break;
                case "rank":
                    String rankSub = event.getSubcommandName();
                    if ("add".equalsIgnoreCase(rankSub)) {
                        rankCommand.handleAdd(event);
                    } else if ("remove".equalsIgnoreCase(rankSub)) {
                        rankCommand.handleRemove(event);
                    } else {
                        event.reply("❌ Unknown subcommand for /rank").setEphemeral(true).queue();
                    }
                    break;
                case "tags":
                    String tagsSub = event.getSubcommandName();
                    if ("add".equalsIgnoreCase(tagsSub)) {
                        tagsCommand.handleAdd(event);
                    } else if ("remove".equalsIgnoreCase(tagsSub)) {
                        tagsCommand.handleRemove(event);
                    } else {
                        event.reply("❌ Unknown subcommand for /tags").setEphemeral(true).queue();
                    }
                    break;
                case "mc-dc-reload":
                    reloadCommand.handle(event);
                    break;
                case "help":
                    helpCommand.handle(event);
                    break;
                default:
                    event.reply("❌ Unknown command: " + commandName).setEphemeral(true).queue();
                    plugin.getLogger().warning("Unknown command received: " + commandName);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling command: " + commandName + " - " + e.getMessage());
            
            if (event.isAcknowledged()) {
                event.getHook().editOriginal("❌ An error occurred while processing the command.").queue();
            } else {
                event.reply("❌ An error occurred while processing the command.").setEphemeral(true).queue();
            }
        }
    }
    
    @Override
    public void onReady(ReadyEvent event) {
        plugin.getLogger().info("Discord bot is ready! Logged in as: " + event.getJDA().getSelfUser().getAsTag());
        plugin.getLogger().info("Bot is in " + event.getGuildAvailableCount() + " guilds");
        
        // Log configured guilds
        List<String> configuredGuilds = plugin.getConfigManager().getGuildIds();
        plugin.getLogger().info("Configured for " + configuredGuilds.size() + " guilds");
    }
    
    public void shutdown() {
        if (jda != null) {
            plugin.getLogger().info("Shutting down Discord bot...");
            jda.shutdown();
            try {
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    plugin.getLogger().warning("Discord bot did not shutdown gracefully, forcing shutdown...");
                    jda.shutdownNow();
                }
            } catch (InterruptedException e) {
                plugin.getLogger().warning("Interrupted while waiting for Discord bot shutdown");
                jda.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public JDA getJDA() {
        return jda;
    }
    
    public boolean isReady() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }
}
