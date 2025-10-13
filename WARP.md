# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

Project overview
- Java 8+ Bukkit/Spigot plugin that bridges Discord and Minecraft
- Discord integration via JDA 5; Minecraft server moderation features rely on LiteBans (required) and optionally EssentialsX; Vault is a soft dependency
- Multi-guild support with role aliasing and per-guild overrides configured in config.yml
- Asynchronous, non-blocking design with safe handoffs to the Bukkit main thread

Prerequisites
- Java 8 (target) and Maven 3.x
- Build produces a shaded plugin JAR at target/MCDiscordBridge-1.0.jar

Commands
- Build (CI/local)
  ```pwsh path=null start=null
  mvn --no-transfer-progress clean package
  ```
- Run tests (if present)
  ```pwsh path=null start=null
  mvn --no-transfer-progress test
  ```
- Run a single test class or method (if present)
  ```pwsh path=null start=null
  # Class
  mvn --no-transfer-progress -Dtest=SomeTest test

  # Specific method
  mvn --no-transfer-progress -Dtest=SomeTest#methodName test
  ```
- Lint/format
  - No Maven linting/formatting plugins are configured in pom.xml (e.g., Checkstyle/SpotBugs/Formatter are not present)
- Runtime admin (once deployed to a server)
  - Bukkit command (in-game console): /mc-dc-reload
  - Discord slash: /mc-dc-reload (reloads plugin configuration and restarts the bot)

Important configuration
- The packaged default config is at src/main/resources/config.yml; the runtime server will generate plugins/MCDiscordBridge/config.yml on first start
- Set discord.token and configure discord.guilds with role aliases and per-command permissions
- Role system
  - discord.root_roles: global alias→role ID mappings
  - discord.guilds.<name>.role_overrides: per-guild alias overrides
  - discord.guilds.<name>.permissions.<command>: array of aliases or raw role IDs
- Plugin integrations (LiteBans, mute plugin) and messages are configurable and used by command handlers

High-level architecture
- Entry plugin (Bukkit): com.dreamrela.mcdiscordbridge.MCDiscordBridge
  - Lifecycle: onEnable initializes ConfigManager, AsyncTaskRunner, registers the Bukkit /mc-dc-reload command, and asynchronously initializes the Discord bot; onDisable shuts down bot and executor
  - Reload flow: reloadPlugin() shuts down the bot, reloads config, and reinitializes the bot
- Discord bot orchestration: com.dreamrela.mcdiscordbridge.discord.DiscordBot
  - Initializes JDA with required gateway intents and minimal cache flags
  - Sets presence activity based on config (discord.activity)
  - Registers slash commands per configured guild IDs
  - Handles SlashCommandInteractionEvent and delegates to command-specific handlers:
    - BanCommand, UnbanCommand, MuteCommand, UnmuteCommand, KickCommand, ListCommand, BroadcastCommand, PunishmentListCommand, ReloadCommand, HelpCommand
  - Logging and readiness notifications are emitted to the Bukkit logger
- Configuration access: com.dreamrela.mcdiscordbridge.config.ConfigManager
  - Reads and reloads Bukkit config.yml; exposes typed getters for Discord token/activity, role alias maps, guild overrides, per-command permissions, help content, colors, performance, messages
  - Resolves permissions by converting aliases (root_roles and per-guild overrides) into concrete role IDs; supports direct role IDs
- Permission enforcement for Discord: com.dreamrela.mcdiscordbridge.discord.utils.PermissionUtils
  - Validates guild membership, grants guild owners/administrators universal access, and checks whether a member has any of the required roles for a command in the given guild
  - Provides human-readable required role strings for error/help responses
- Async execution: com.dreamrela.mcdiscordbridge.utils.AsyncTaskRunner
  - Fixed thread pool size from config; runAsync for background work, runSync/runSyncDelayed for safe execution on Bukkit main thread; graceful shutdown on plugin disable
- Command flow pattern (example: BanCommand)
  - Validate guild configuration and permissions
  - Defer Discord reply; parse and validate inputs (player, duration, reason)
  - Perform heavy/IO work asynchronously; switch to Bukkit main thread for in-server actions (LiteBans)
  - Respond with rich embeds (EmbedUtils) and log actions

Key files to know
- pom.xml: Maven build with maven-shade-plugin; relocations ensure shaded dependencies don’t clash at runtime; default goal is clean package
- src/main/resources/plugin.yml: Bukkit plugin metadata, Bukkit command, and permission defaults
- src/main/resources/config.yml: Default configuration template (token, activity, role system, per-guild permissions, integrations, messages)
- src/main/java/com/dreamrela/mcdiscordbridge/...: Core plugin, bot, config, utils, and command handlers

Notes from README
- Requirements: Java 8+, Minecraft 1.8.8+ (Spigot/Paper/CarbonSpigot), Discord bot with proper permissions, Internet connectivity
- Dependencies: LiteBans required; EssentialsX and Vault optional but supported; commands and duration formats are documented
- Build/install path: mvn clean package creates target/MCDiscordBridge-1.0.jar for deployment to your server’s plugins directory

Operational tips
- To apply config changes in production, use /mc-dc-reload (Bukkit) or /mc-dc-reload (Discord) to reload without restarting the server; the bot is shut down and reinitialized using the new configuration
- Slash commands are registered per configured guild; ensure discord.guilds entries are accurate and the bot is present in those servers
