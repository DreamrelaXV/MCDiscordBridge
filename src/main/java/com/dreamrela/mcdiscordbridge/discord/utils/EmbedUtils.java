package com.dreamrela.mcdiscordbridge.discord.utils;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.StringUtils;

import java.awt.Color;
import java.time.Instant;
import java.util.Map;

public class EmbedUtils {
    
    private final MCDiscordBridge plugin;
    private static final int MAX_EMBED_LENGTH = 6000;
    private static final int MAX_FIELD_VALUE_LENGTH = 1024;
    private static final int MAX_DESCRIPTION_LENGTH = 4096;
    private static final int MAX_TITLE_LENGTH = 256;
    
    public EmbedUtils(MCDiscordBridge plugin) {
        this.plugin = plugin;
    }
    
    public MessageEmbed createSuccessEmbed(String title, String description) {
        return createEmbed("✅ " + title, description, plugin.getConfigManager().getColor("success"));
    }
    
    public MessageEmbed createErrorEmbed(String title, String description) {
        return createEmbed("❌ " + title, description, plugin.getConfigManager().getColor("error"));
    }
    
    public MessageEmbed createWarningEmbed(String title, String description) {
        return createEmbed("⚠️ " + title, description, plugin.getConfigManager().getColor("warning"));
    }
    
    public MessageEmbed createInfoEmbed(String title, String description) {
        return createEmbed("ℹ️ " + title, description, plugin.getConfigManager().getColor("info"));
    }
    
    public MessageEmbed createCustomEmbed(String title, String description, String colorType) {
        return createEmbed(title, description, plugin.getConfigManager().getColor(colorType));
    }
    
    private MessageEmbed createEmbed(String title, String description, int colorValue) {
        // Sanitize and truncate inputs
        title = sanitizeText(title, MAX_TITLE_LENGTH);
        description = sanitizeText(description, MAX_DESCRIPTION_LENGTH);
        
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(new Color(colorValue))
                .setTimestamp(Instant.now())
                .setFooter("MCDiscordBridge by Dreamrela", null);
        
        return builder.build();
    }
    
    public MessageEmbed createHelpEmbed() {
        String title = sanitizeText(plugin.getConfigManager().getHelpTitle(), MAX_TITLE_LENGTH);
        String description = sanitizeText(plugin.getConfigManager().getHelpDescription(), MAX_DESCRIPTION_LENGTH);
        
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(new Color(plugin.getConfigManager().getHelpColor()))
                .setTimestamp(Instant.now())
                .setFooter("MCDiscordBridge by Dreamrela", null);
        
        // Add command fields with proper length checking
        int fieldCount = 0;
        for (Map<String, String> command : plugin.getConfigManager().getHelpCommands()) {
            if (fieldCount >= 25) { // Discord limit
                builder.addField("...", "More commands available - see documentation", false);
                break;
            }
            
            String name = sanitizeText(command.get("name"), MAX_TITLE_LENGTH);
            String cmdDescription = sanitizeText(command.get("description"), 500);
            String usage = sanitizeText(command.get("usage"), 500);
            
            String fieldValue = cmdDescription + "\n`" + usage + "`";
            fieldValue = sanitizeText(fieldValue, MAX_FIELD_VALUE_LENGTH);
            
            builder.addField(name, fieldValue, false);
            fieldCount++;
        }
        
        // Check total embed length and truncate if necessary
        MessageEmbed embed = builder.build();
        if (getEmbedLength(embed) > MAX_EMBED_LENGTH) {
            // If too long, create a simpler version
            return createSimplifiedHelpEmbed();
        }
        
        return embed;
    }
    
    private MessageEmbed createSimplifiedHelpEmbed() {
        return new EmbedBuilder()
                .setTitle("MCDiscordBridge Commands")
                .setDescription("**Available Commands:**\n" +
                               "• `/mc-ban` - Ban a player\n" +
                               "• `/mc-unban` - Unban a player\n" +
                               "• `/mc-mute` - Mute a player\n" +
                               "• `/mc-unmute` - Unmute a player\n" +
                               "• `/mc-kick` - Kick a player\n" +
                               "• `/list` - Show online players\n" +
                               "• `/brc` - Broadcast a message\n" +
                               "• `/mc-dc-reload` - Reload config\n" +
                               "• `/help` - Show this help")
                .setColor(new Color(plugin.getConfigManager().getHelpColor()))
                .setTimestamp(Instant.now())
                .setFooter("MCDiscordBridge by Dreamrela", null)
                .build();
    }
    
    public MessageEmbed createPlayerListEmbed(String playerList, int onlineCount, int maxPlayers) {
        String title = "Online Players (" + onlineCount + "/" + maxPlayers + ")";

        if (StringUtils.isBlank(playerList)) {
            return createInfoEmbed(title, "No players are currently online.");
        }

        // Convert comma-separated list to bullet points in description (avoid fields)
        String[] players = playerList.split(", ");
        StringBuilder desc = new StringBuilder("**Players:**\n");

        for (String p : players) {
            String line = "• " + p + "\n";
            if (desc.length() + line.length() > MAX_DESCRIPTION_LENGTH) {
                // Truncate safely if too long for single-embed variant
                desc.append("…");
                break;
            }
            desc.append(line);
        }

        return new EmbedBuilder()
                .setTitle("🎮 " + title)
                .setDescription(desc.toString())
                .setColor(new Color(plugin.getConfigManager().getColor("info")))
                .setTimestamp(Instant.now())
                .setFooter("MCDiscordBridge by Dreamrela", null)
                .build();
    }

    public java.util.List<MessageEmbed> createPlayerListEmbeds(java.util.List<String> playerNames, int onlineCount, int maxPlayers) {
        String baseTitle = "Online Players (" + onlineCount + "/" + maxPlayers + ")";

        if (playerNames == null || playerNames.isEmpty()) {
            return java.util.Collections.singletonList(createInfoEmbed(baseTitle, "No players are currently online."));
        }

        java.util.List<MessageEmbed> pages = new java.util.ArrayList<MessageEmbed>();
        java.util.List<String> currentLines = new java.util.ArrayList<String>();
        int currentLen = "**Players:**\n".length();

        for (String name : playerNames) {
            String safeName = name == null ? "Unknown" : name;
            String line = "• " + safeName + "\n";
            if (currentLen + line.length() > MAX_DESCRIPTION_LENGTH) {
                // finalize current page
                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("🎮 " + baseTitle)
                        .setColor(new Color(plugin.getConfigManager().getColor("info")))
                        .setTimestamp(Instant.now())
                        .setFooter("MCDiscordBridge by Dreamrela", null);
                StringBuilder desc = new StringBuilder("**Players:**\n");
                for (String l : currentLines) desc.append(l);
                builder.setDescription(desc.toString());
                pages.add(builder.build());

                // start new page
                currentLines.clear();
                currentLen = "**Players:**\n".length();
            }
            currentLines.add(line);
            currentLen += line.length();
        }

        if (!currentLines.isEmpty()) {
            EmbedBuilder builder = new EmbedBuilder()
                    .setTitle("🎮 " + baseTitle)
                    .setColor(new Color(plugin.getConfigManager().getColor("info")))
                    .setTimestamp(Instant.now())
                    .setFooter("MCDiscordBridge by Dreamrela", null);
            StringBuilder desc = new StringBuilder("**Players:**\n");
            for (String l : currentLines) desc.append(l);
            builder.setDescription(desc.toString());
            pages.add(builder.build());
        }

        // If more than one page, annotate titles with page numbers
        if (pages.size() > 1) {
            java.util.List<MessageEmbed> numbered = new java.util.ArrayList<MessageEmbed>(pages.size());
            int total = pages.size();
            for (int i = 0; i < total; i++) {
                MessageEmbed e = pages.get(i);
                EmbedBuilder b = new EmbedBuilder(e);
                b.setTitle("🎮 " + baseTitle + " — Page " + (i + 1) + "/" + total);
                numbered.add(b.build());
            }
            return numbered;
        }

        return pages;
    }
    
    private String sanitizeText(String text, int maxLength) {
        if (StringUtils.isBlank(text)) {
            return "";
        }
        
        // Remove or replace problematic characters
        text = text.replaceAll("[\\p{C}&&[^\n\r\t]]", "");
        
        // Truncate if too long
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength - 3) + "...";
        }
        
        return text;
    }
    
    private int getEmbedLength(MessageEmbed embed) {
        int length = 0;
        
        if (embed.getTitle() != null) {
            length += embed.getTitle().length();
        }
        
        if (embed.getDescription() != null) {
            length += embed.getDescription().length();
        }
        
        for (MessageEmbed.Field field : embed.getFields()) {
            if (field.getName() != null) {
                length += field.getName().length();
            }
            if (field.getValue() != null) {
                length += field.getValue().length();
            }
        }
        
        if (embed.getFooter() != null && embed.getFooter().getText() != null) {
            length += embed.getFooter().getText().length();
        }
        
        if (embed.getAuthor() != null && embed.getAuthor().getName() != null) {
            length += embed.getAuthor().getName().length();
        }
        
        return length;
    }
    
    public MessageEmbed createCommandResultEmbed(String commandName, String result, boolean success) {
        String emoji = success ? "✅" : "❌";
        String colorType = success ? "success" : "error";
        
        return createEmbed(
            emoji + " " + commandName + " Result",
            result,
            plugin.getConfigManager().getColor(colorType)
        );
    }
    
    public MessageEmbed createPlayerActionEmbed(String action, String playerName, String reason, String duration, boolean success) {
        String emoji = success ? "✅" : "❌";
        String title = emoji + " Player " + action;
        String colorType = success ? action.toLowerCase() : "error";
        
        StringBuilder description = new StringBuilder();
        
        if (success) {
            description.append("**Player:** ").append(playerName).append("\n");
            
            if (StringUtils.isNotBlank(duration)) {
                description.append("**Duration:** ").append(duration).append("\n");
            }
            
            if (StringUtils.isNotBlank(reason)) {
                description.append("**Reason:** ").append(reason);
            }
        } else {
            description.append("Failed to ").append(action.toLowerCase()).append(" player ").append(playerName);
            if (StringUtils.isNotBlank(reason)) {
                description.append("\n**Error:** ").append(reason);
            }
        }
        
        return createEmbed(title, description.toString(), plugin.getConfigManager().getColor(colorType));
    }
}