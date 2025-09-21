package com.dreamrela.mcdiscordbridge.utils;

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
            // First try to get online player
            Player onlinePlayer = Bukkit.getPlayerExact(playerInput);
            if (onlinePlayer != null) {
                return onlinePlayer;
            }
            
            // Then try offline players who have played before
            OfflinePlayer[] offlinePlayers = Bukkit.getOfflinePlayers();
            for (OfflinePlayer offlinePlayer : offlinePlayers) {
                if (offlinePlayer.getName() != null && 
                    offlinePlayer.getName().equalsIgnoreCase(playerInput) && 
                    offlinePlayer.hasPlayedBefore()) {
                    return offlinePlayer;
                }
            }
            
            return null;
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
            return Bukkit.getPlayerExact(playerInput);
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
     * Bans a player
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
    
    /**
     * IP bans a player
     */
    public static boolean banPlayerIP(OfflinePlayer player, String reason, Date expires, String source) {
        try {
            Player onlinePlayer = player.getPlayer();
            String ipAddress = null;
            
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                ipAddress = onlinePlayer.getAddress().getAddress().getHostAddress();
            }
            
            // Also try to get IP from previous sessions if available
            if (ipAddress == null) {
                // This would require storing IP addresses, which might not be available in 1.8.8
                // For now, we'll just ban by name and log a warning
                return banPlayer(player, reason, expires, source);
            }
            
            BanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            ipBanList.addBan(ipAddress, reason, expires, source);
            
            // Also ban by name
            banPlayer(player, reason, expires, source);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Unbans a player
     */
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
     * Gets the player's display name or name
     */
    public static String getDisplayName(OfflinePlayer player) {
        if (player == null) {
            return "Unknown";
        }
        
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            // Use getName() instead of getDisplayName() to avoid deprecation
            String customName = onlinePlayer.getPlayerListName();
            if (customName != null && !customName.isEmpty()) {
                return customName;
            }
            return onlinePlayer.getName();
        }
        
        return player.getName() != null ? player.getName() : "Unknown";
    }
    
    /**
     * Gets the player's display name formatted for Discord
     */
    public static String getDisplayNameForDiscord(OfflinePlayer player) {
        String name = getDisplayName(player);
        return escapeDiscordFormatting(name);
    }
    
    /**
     * Escapes Discord formatting characters in a string
     */
    public static String escapeDiscordFormatting(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("*", "\\*")
                  .replace("_", "\\_")
                  .replace("`", "\\`")
                  .replace("~", "\\~")
                  .replace("|", "\\|");
    }
    
    /**
     * Formats a collection of players for Discord display
     */
    public static String formatPlayerListForDiscord(java.util.Collection<? extends Player> players) {
        if (players == null || players.isEmpty()) {
            return "No players online";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (Player player : players) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(escapeDiscordFormatting(player.getName()));
            first = false;
        }
        
        return sb.toString();
    }
    
    /**
     * Bans a player using LiteBans
     */
    public static boolean banPlayerWithLiteBans(OfflinePlayer player, String reason, long durationMillis, String source) {
        try {
            // Convert milliseconds to LiteBans duration format
            String duration = formatDurationForLiteBans(durationMillis);
            String command = String.format("ban %s %s %s -s", 
                player.getName(), duration, reason);
            
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * IP bans a player using LiteBans
     */
    public static boolean banPlayerIPWithLiteBans(OfflinePlayer player, String reason, long durationMillis, String source) {
        try {
            // Convert milliseconds to LiteBans duration format
            String duration = formatDurationForLiteBans(durationMillis);
            String command = String.format("banip %s %s %s -s", 
                player.getName(), duration, reason);
            
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Unbans a player using LiteBans
     */
    public static boolean unbanPlayerWithLiteBans(OfflinePlayer player, String reason, String source) {
        try {
            String command = String.format("unban %s -s", player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Unbans a player's IP using LiteBans
     */
    public static boolean unbanPlayerIPWithLiteBans(OfflinePlayer player, String reason, String source) {
        try {
            String command = String.format("unbanip %s -s", player.getName());
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Formats duration in milliseconds to LiteBans format
     */
    private static String formatDurationForLiteBans(long milliseconds) {
        if (milliseconds <= 0) {
            return "0s"; // Permanent
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
}