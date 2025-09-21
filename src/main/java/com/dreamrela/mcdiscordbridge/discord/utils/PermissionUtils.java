package com.dreamrela.mcdiscordbridge.discord.utils;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    
    private final MCDiscordBridge plugin;
    
    public PermissionUtils(MCDiscordBridge plugin) {
        this.plugin = plugin;
    }
    
    public boolean hasPermission(SlashCommandInteractionEvent event, String command) {
        // Only allow commands in guilds (servers), not DMs
        if (event.getGuild() == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Command " + command + " denied: not in guild");
            }
            return false;
        }
        
        String guildId = event.getGuild().getId();
        Member member = event.getMember();
        
        if (member == null) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Command " + command + " denied: member is null");
            }
            return false;
        }
        
        // Check if user is guild owner - owners can use any command
        if (member.isOwner()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Command " + command + " allowed: user is guild owner");
            }
            return true;
        }
        
        // Check if user has administrator permission - admins can use any command
        if (member.hasPermission(Permission.ADMINISTRATOR)) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Command " + command + " allowed: user has administrator permission");
            }
            return true;
        }
        
        // Get required roles for this command in this guild
        List<String> requiredRoles = plugin.getConfigManager().getCommandPermissions(guildId, command);
        
        // If no roles are required, everyone can use the command
        if (requiredRoles.isEmpty()) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Command " + command + " allowed: no roles required");
            }
            return true;
        }
        
        // Check if user has any of the required roles
        List<Role> memberRoles = member.getRoles();
        for (String roleId : requiredRoles) {
            if (StringUtils.isBlank(roleId)) {
                continue;
            }
            
            Role role = event.getGuild().getRoleById(roleId);
            if (role != null && memberRoles.contains(role)) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Command " + command + " allowed: user has required role " + role.getName());
                }
                return true;
            }
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Command " + command + " denied: user lacks required roles");
        }
        
        return false;
    }
    
    public String getRequiredRolesString(SlashCommandInteractionEvent event, String command) {
        if (event.getGuild() == null) {
            return "Server Only";
        }
        
        String guildId = event.getGuild().getId();
        List<String> requiredRoles = plugin.getConfigManager().getCommandPermissions(guildId, command);
        
        if (requiredRoles.isEmpty()) {
            return "Everyone";
        }
        
        List<String> roleNames = new ArrayList<String>();
        Guild guild = event.getGuild();
        
        for (String roleId : requiredRoles) {
            if (StringUtils.isBlank(roleId)) {
                continue;
            }
            
            Role role = guild.getRoleById(roleId);
            if (role != null) {
                roleNames.add(role.getAsMention());
            } else {
                roleNames.add("@unknown-role (" + roleId + ")");
                
                // Log missing role for debugging
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().warning("Role ID " + roleId + " not found in guild " + guild.getName());
                }
            }
        }
        
        if (roleNames.isEmpty()) {
            return "@unknown-roles";
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < roleNames.size(); i++) {
            result.append(roleNames.get(i));
            if (i < roleNames.size() - 1) {
                result.append(", ");
            }
        }
        
        return result.toString();
    }
    
    public List<String> getRequiredRoleNames(SlashCommandInteractionEvent event, String command) {
        if (event.getGuild() == null) {
            return new ArrayList<String>();
        }
        
        String guildId = event.getGuild().getId();
        List<String> requiredRoleIds = plugin.getConfigManager().getCommandPermissions(guildId, command);
        Guild guild = event.getGuild();
        List<String> roleNames = new ArrayList<String>();
        
        for (String roleId : requiredRoleIds) {
            if (StringUtils.isNotBlank(roleId)) {
                Role role = guild.getRoleById(roleId);
                if (role != null) {
                    roleNames.add(role.getName());
                }
            }
        }
        
        return roleNames;
    }
    
    public boolean isValidGuild(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            return false;
        }
        
        List<String> configuredGuilds = plugin.getConfigManager().getGuildIds();
        return configuredGuilds.contains(event.getGuild().getId());
    }
    
    public String getPermissionDeniedMessage(SlashCommandInteractionEvent event, String command) {
        StringBuilder message = new StringBuilder();
        message.append(plugin.getConfigManager().getMessage("permission_denied"));
        
        if (event.getGuild() == null) {
            message.append("\n\n**Note:** This command can only be used in servers, not DMs.");
        } else if (!isValidGuild(event)) {
            message.append("\n\n**Note:** This bot is not configured for this server.");
        } else {
            String requiredRoles = getRequiredRolesString(event, command);
            message.append("\n\n**Required Roles:** ").append(requiredRoles);
            
            // Add helpful information
            Member member = event.getMember();
            if (member != null) {
                List<Role> memberRoles = member.getRoles();
                if (memberRoles.isEmpty()) {
                    message.append("\n**Your Roles:** None");
                } else {
                    StringBuilder userRoles = new StringBuilder();
                    for (int i = 0; i < memberRoles.size(); i++) {
                        userRoles.append(memberRoles.get(i).getAsMention());
                        if (i < memberRoles.size() - 1) {
                            userRoles.append(", ");
                        }
                    }
                    message.append("\n**Your Roles:** ").append(userRoles.toString());
                }
            }
        }
        
        return message.toString();
    }
    
    public boolean canUseAnyCommand(Member member, String guildId) {
        if (member == null) {
            return false;
        }
        
        // Guild owners and administrators can use any command
        if (member.isOwner() || member.hasPermission(Permission.ADMINISTRATOR)) {
            return true;
        }
        
        // Check if user has any roles that grant command access
        List<Role> memberRoles = member.getRoles();
        String[] commands = {"ban", "unban", "mute", "unmute", "kick", "list", "broadcast", "reload", "help"};
        
        for (String command : commands) {
            List<String> requiredRoles = plugin.getConfigManager().getCommandPermissions(guildId, command);
            
            // If no roles required for this command, user can use it
            if (requiredRoles.isEmpty()) {
                return true;
            }
            
            // Check if user has any required role for this command
            for (String roleId : requiredRoles) {
                if (StringUtils.isBlank(roleId)) {
                    continue;
                }
                
                Role role = member.getGuild().getRoleById(roleId);
                if (role != null && memberRoles.contains(role)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}