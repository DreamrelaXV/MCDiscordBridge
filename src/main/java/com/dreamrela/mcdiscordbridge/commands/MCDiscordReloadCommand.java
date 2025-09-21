package com.dreamrela.mcdiscordbridge.commands;

import com.dreamrela.mcdiscordbridge.MCDiscordBridge;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class MCDiscordReloadCommand implements CommandExecutor {
    
    private final MCDiscordBridge plugin;
    
    public MCDiscordReloadCommand(MCDiscordBridge plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mcdiscordbridge.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "Reloading MCDiscordBridge plugin...");
        
        plugin.getAsyncTaskRunner().runAsync(() -> {
            try {
                plugin.reloadPlugin();
                
                // Send success message on main thread
                plugin.getAsyncTaskRunner().runSync(() -> {
                    sender.sendMessage(ChatColor.GREEN + "MCDiscordBridge plugin reloaded successfully!");
                });
            } catch (Exception e) {
                // Send error message on main thread
                plugin.getAsyncTaskRunner().runSync(() -> {
                    sender.sendMessage(ChatColor.RED + "Failed to reload plugin: " + e.getMessage());
                });
                plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
            }
        });
        
        return true;
    }
}