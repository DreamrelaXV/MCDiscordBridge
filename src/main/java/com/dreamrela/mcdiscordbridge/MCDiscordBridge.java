package com.dreamrela.mcdiscordbridge;

import com.dreamrela.mcdiscordbridge.commands.MCDiscordReloadCommand;
import com.dreamrela.mcdiscordbridge.config.ConfigManager;
import com.dreamrela.mcdiscordbridge.discord.DiscordBot;
import com.dreamrela.mcdiscordbridge.utils.AsyncTaskRunner;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class MCDiscordBridge extends JavaPlugin {

    private static MCDiscordBridge instance;
    private ConfigManager configManager;
    private DiscordBot discordBot;
    private AsyncTaskRunner asyncTaskRunner;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize configuration
        this.configManager = new ConfigManager(this);

        // Initialize async task runner
        this.asyncTaskRunner = new AsyncTaskRunner(this);

        // Register commands
        registerCommands();

        // Initialize Discord bot
        initializeDiscordBot();

        getLogger().info("MCDiscordBridge v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (discordBot != null) {
            getLogger().info("Shutting down Discord bot...");
            discordBot.shutdown();
        }

        if (asyncTaskRunner != null) {
            asyncTaskRunner.shutdown();
        }

        getLogger().info("MCDiscordBridge has been disabled!");
    }

    private void registerCommands() {
        getCommand("mc-dc-reload").setExecutor(new MCDiscordReloadCommand(this));
    }

    private void initializeDiscordBot() {
        try {
            this.discordBot = new DiscordBot(this);
            asyncTaskRunner.runAsync(() -> {
                try {
                    discordBot.initialize();
                    getLogger().info("Discord bot initialized successfully!");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to initialize Discord bot!", e);
                }
            });
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to create Discord bot instance!", e);
        }
    }

    public void reloadPlugin() {
        try {
            // Shutdown current Discord bot
            if (discordBot != null) {
                discordBot.shutdown();
            }

            // Reload configuration
            configManager.reloadConfig();

            // Reinitialize Discord bot
            initializeDiscordBot();

            getLogger().info("Plugin reloaded successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to reload plugin!", e);
            throw new RuntimeException("Failed to reload plugin", e);
        }
    }

    // Getters
    public static MCDiscordBridge getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }

    public AsyncTaskRunner getAsyncTaskRunner() {
        return asyncTaskRunner;
    }
}