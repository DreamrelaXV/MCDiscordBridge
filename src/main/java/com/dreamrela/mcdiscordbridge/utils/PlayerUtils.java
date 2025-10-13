package com.dreamrela.mcdiscordbridge.utils;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class PlayerUtils {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    /**
     * Gets an OfflinePlayer by name or UUID
     */
    public static OfflinePlayer getOfflinePlayer(String playerInput) {
        if (isUUID(playerInput)) {
            try {
                UUID uuid = UUID.fromString(playerInput);
                return Bukkit.getOfflinePlayer(uuid);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            // Try to get by name
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerInput);
            return player.hasPlayedBefore() ? player : null;
        }
    }
    
    /**
     * Gets an online Player by name or UUID
     */
    public static Player getOnlinePlayer(String playerInput) {
        if (isUUID(playerInput)) {
            try {
                UUID uuid = UUID.fromString(playerInput);
                return Bukkit.getPlayer(uuid);
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return Bukkit.getPlayer(playerInput);
        }
    }
    
    /**
     * Checks if a string is a valid UUID
     */
    public static boolean isUUID(String input) {
        return UUID_PATTERN.matcher(input).matches();
    }
    
    /**
     * Parses duration string to milliseconds
     */
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }
        
        duration = duration.toLowerCase().trim();
        
        try {
            // Remove any spaces
            duration = duration.replaceAll("\\s+", "");
            
            // Extract number and unit
            String numberPart = duration.replaceAll("[^0-9.]", "");
            String unitPart = duration.replaceAll("[0-9.]", "");
            
            if (numberPart.isEmpty()) {
                return 0;
            }
            
            double number = Double.parseDouble(numberPart);
            
            switch (unitPart) {
                case "s":
                case "sec":
                case "second":
                case "seconds":
                    return (long) (number * 1000);
                case "m":
                case "min":
                case "minute":
                case "minutes":
                    return (long) (number * TimeUnit.MINUTES.toMillis(1));
                case "h":
                case "hr":
                case "hour":
                case "hours":
                    return (long) (number * TimeUnit.HOURS.toMillis(1));
                case "d":
                case "day":
                case "days":
                    return (long) (number * TimeUnit.DAYS.toMillis(1));
                case "w":
                case "week":
                case "weeks":
                    return (long) (number * TimeUnit.DAYS.toMillis(7));
                case "mo":
                case "month":
                case "months":
                    return (long) (number * TimeUnit.DAYS.toMillis(30));
                case "y":
                case "year":
                case "years":
                    return (long) (number * TimeUnit.DAYS.toMillis(365));
                default:
                    // If no unit specified, assume minutes
                    return (long) (number * TimeUnit.MINUTES.toMillis(1));
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Converts duration in milliseconds to LiteBans format
     */
    public static String formatDurationForLiteBans(long milliseconds) {
        if (milliseconds <= 0) {
            return "perm";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d";
        } else if (hours > 0) {
            return hours + "h";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Formats duration from milliseconds to human-readable string
     */
    public static String formatDuration(long milliseconds) {
        if (milliseconds <= 0) {
            return "Permanent";
        }
        
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Bans a player using LiteBans
     */
    public static boolean banPlayerWithLiteBans(OfflinePlayer player, String reason, long durationMillis, String source) {
        try {
            MCDiscordBridge plugin = MCDiscordBridge.getInstance();
            String playerName = getDisplayName(player);
            String duration = formatDurationForLiteBans(durationMillis);
            
            String command = plugin.getConfigManager().getLiteBansTempBanCommand()
                    .replace("{player}", playerName)
                    .replace("{duration}", duration)
                    .replace("{reason}", reason);
            
            // Check if LiteBans is available
            if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
                plugin.getLogger().warning("LiteBans plugin not found! Cannot execute ban command.");
                return false;
            }
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            plugin.getLogger().info("LiteBans command executed: " + command);
            return true;
        } catch (Exception e) {
            MCDiscordBridge.getInstance().getLogger().severe("Failed to ban player with LiteBans: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * IP bans a player using LiteBans
     */
    public static boolean banPlayerIPWithLiteBans(OfflinePlayer player, String reason, long durationMillis, String source) {
        try {
            MCDiscordBridge plugin = MCDiscordBridge.getInstance();
            String playerName = getDisplayName(player);
            String duration = formatDurationForLiteBans(durationMillis);
            
            String command = plugin.getConfigManager().getLiteBansBanIPCommand()
                    .replace("{player}", playerName)
                    .replace("{duration}", duration)
                    .replace("{reason}", reason);
            
            // Check if LiteBans is available
            if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
                plugin.getLogger().warning("LiteBans plugin not found! Cannot execute IP ban command.");
                return false;
            }
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            plugin.getLogger().info("LiteBans IP ban command executed: " + command);
            return true;
        } catch (Exception e) {
            MCDiscordBridge.getInstance().getLogger().severe("Failed to IP ban player with LiteBans: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Unbans a player using LiteBans
     */
    public static boolean unbanPlayerWithLiteBans(OfflinePlayer player, String reason, String source) {
        try {
            MCDiscordBridge plugin = MCDiscordBridge.getInstance();
            String playerName = getDisplayName(player);
            
            String command = plugin.getConfigManager().getLiteBansUnbanCommand()
                    .replace("{player}", playerName)
                    .replace("{reason}", reason);
            
            // Check if LiteBans is available
            if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
                plugin.getLogger().warning("LiteBans plugin not found! Cannot execute unban command.");
                return false;
            }
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            plugin.getLogger().info("LiteBans unban command executed: " + command);
            return true;
        } catch (Exception e) {
            MCDiscordBridge.getInstance().getLogger().severe("Failed to unban player with LiteBans: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Unbans a player's IP using LiteBans
     */
    public static boolean unbanPlayerIPWithLiteBans(OfflinePlayer player, String reason, String source) {
        try {
            MCDiscordBridge plugin = MCDiscordBridge.getInstance();
            String playerName = getDisplayName(player);
            
            String command = plugin.getConfigManager().getLiteBansUnbanIPCommand()
                    .replace("{player}", playerName)
                    .replace("{reason}", reason);
            
            // Check if LiteBans is available
            if (Bukkit.getPluginManager().getPlugin("LiteBans") == null) {
                plugin.getLogger().warning("LiteBans plugin not found! Cannot execute IP unban command.");
                return false;
            }
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            
            plugin.getLogger().info("LiteBans IP unban command executed: " + command);
            return true;
        } catch (Exception e) {
            MCDiscordBridge.getInstance().getLogger().severe("Failed to unban player IP with LiteBans: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Legacy ban methods (kept for compatibility)
     */
    public static boolean banPlayer(OfflinePlayer player, String reason, Date expires, String source) {
        try {
            BanList banList = Bukkit.getBanList(BanList.Type.NAME);
            banList.addBan(player.getName(), reason, expires, source);
            
            // Kick if online
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.kickPlayer("Banned on discord: " + reason);
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean banPlayerIP(OfflinePlayer player, String reason, Date expires, String source) {
        try {
            Player onlinePlayer = player.getPlayer();
            String ipAddress = null;
            
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
            }
            
            if (ipAddress == null) {
                return banPlayer(player, reason, expires, source);
            }
            
            BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            ipBanList.addBan(ipAddress, reason, expires, source);
            
            banPlayer(player, reason, expires, source);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean unbanPlayer(OfflinePlayer player, String source) {
        try {
            BanList banList = Bukkit.getBanList(BanList.Type.NAME);
            banList.pardon(player.getName());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Checks if a player is banned
     */
    public static boolean isBanned(OfflinePlayer player) {
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        return banList.isBanned(player.getName());
    }
    
    /**
     * Gets the player's display name or name, escaping underscores for Discord
     */
    public static String getDisplayName(OfflinePlayer player) {
        if (player == null) {
            return "Unknown";
        }
        
        Player onlinePlayer = player.getPlayer();
        String name;
        
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            name = onlinePlayer.getDisplayName();
        } else {
            name = player.getName() != null ? player.getName() : "Unknown";
        }
        
        // Escape underscores to prevent Discord markdown formatting
        return escapeDiscordFormatting(name);
    }
    
    /**
     * Gets the player's display name for Discord lists, with proper escaping
     */
    public static String getDisplayNameForDiscord(OfflinePlayer player) {
        return getDisplayName(player);
    }
    
    /**
     * Escapes Discord markdown formatting characters
     */
    public static String escapeDiscordFormatting(String text) {
        if (text == null) {
            return "Unknown";
        }
        
        // Escape Discord markdown characters
        return text
                .replace("\\", "\\\\")  // Escape backslashes first
                .replace("_", "\\_")    // Escape underscores (prevents italic)
                .replace("*", "\\*")    // Escape asterisks (prevents bold/italic)
                .replace("`", "\\`")    // Escape backticks (prevents code)
                .replace("~", "\\~")    // Escape tildes (prevents strikethrough)
                .replace("|", "\\|");   // Escape pipes (prevents spoilers)
    }
    
    /**
     * Formats a player list for Discord with proper escaping
     */
    public static String formatPlayerListForDiscord(java.util.Collection<? extends Player> players) {
        if (players.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        boolean first = true;
        
        for (Player player : players) {
            if (!first) {
                result.append(", ");
            }
            result.append(escapeDiscordFormatting(player.getDisplayName()));
            first = false;
        }
        
        return result.toString();
    }
}
