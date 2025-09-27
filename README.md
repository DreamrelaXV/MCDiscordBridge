# 🌉 MCDiscordBridge

<div align="center">

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8-green.svg)](https://minecraft.net)
[![Discord](https://img.shields.io/badge/Discord-JDA%205.0-blue.svg)](https://github.com/DV8FromTheWorld/JDA)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*Powerful Discord-Minecraft integration for seamless server moderation*

</div>

## ✨ Features

🚫 **Ban/Unban** with LiteBans integration and unlimited duration  
🔇 **Mute/Unmute** with duration control and EssentialsX support  
👢 **Kick** players instantly from Discord  
📊 **List** online players with Discord-safe formatting  
📢 **Broadcast** server messages to all players  
📋 **Punishment History** - View detailed player ban/mute records  
🔄 **Hot Reload** configuration with "Restart Pushed" notification  
🌍 **Multi-Guild** support with per-server configurations  
🎭 **Smart Role System** - Use meaningful aliases instead of role IDs  
⚡ **Async Processing** - Zero server performance impact  

## 📋 Requirements

### 🖥️ **Server Requirements**
- **Java 8+** and **Minecraft 1.8.8+** (CarbonSpigot/Paper/Spigot)
- **Discord Bot** with proper permissions
- **Internet connection** for Discord API

### 🔌 **Required Dependencies**
- **[LiteBans](https://www.spigotmc.org/resources/litebans.3715/)** - Essential for ban/unban functionality

### 🔌 **Optional Dependencies**
- **[EssentialsX](https://www.spigotmc.org/resources/essentialsx.9089/)** - Enhanced mute/unmute support
- **[Vault](https://www.spigotmc.org/resources/vault.34315/)** - Permission system integration

## 🚀 Installation Guide

### Step 1: Create Discord Bot
1. Visit [Discord Developer Portal](https://discord.com/developers/applications)
2. Click "New Application" → Enter name → Create
3. Navigate to "Bot" section → "Add Bot"
4. Copy the **Bot Token** (keep it secure!)
5. Enable these **Privileged Gateway Intents**:
   - ✅ Server Members Intent
   - ✅ Message Content Intent (if needed)
6. Go to OAuth2 → URL Generator:
   - **Scopes:** `bot` + `applications.commands`
   - **Bot Permissions:** `Send Messages`, `Use Slash Commands`, `Embed Links`
7. Use generated URL to invite bot to your Discord server

### Step 2: Install Dependencies
**Install these plugins on your Minecraft server:**
```bash
# Required - Download and install LiteBans
wget https://ci.litebans.cf/job/LiteBans/lastStableBuild/artifact/litebans-bukkit.jar
mv litebans-bukkit.jar plugins/

# Optional but recommended - EssentialsX
# Download from: https://essentialsx.net/downloads.html
```

### Step 3: Install MCDiscordBridge
```bash
# Build from source (if you have the source code)
git clone https://github.com/dreamrela/MCDiscordBridge.git
cd MCDiscordBridge
mvn clean package

# Copy to plugins folder
cp target/MCDiscordBridge-1.0.jar /path/to/minecraft/server/plugins/
```

### Step 4: Configuration Setup
Start your server once to generate the config file, then edit `plugins/MCDiscordBridge/config.yml`:

```yaml
# MCDiscordBridge Configuration
discord:
  # Your Discord bot token (KEEP THIS SECURE!)
  token: "YOUR_BOT_TOKEN_HERE"
  
  # Global role definitions - Use meaningful names!
  root_roles:
    # Server ownership & management
    owner: "832456789012345678"        # Server owner role ID
    co_owner: "123456789012345678"     # Co-owner role ID
    manager: "234567890123456789"      # Server manager role ID
    
    # Administrative roles
    admin: "345678901234567890"        # Administrator role ID
    senior_admin: "456789012345678901" # Senior admin role ID
    
    # Moderation roles
    head_mod: "567890123456789012"     # Head moderator role ID
    senior_mod: "678901234567890123"   # Senior moderator role ID
    moderator: "789012345678901234"    # Regular moderator role ID
    trial_mod: "890123456789012345"    # Trial moderator role ID
    
    # Support & helper roles
    senior_helper: "901234567890123456" # Senior helper role ID
    helper: "012345678901234567"        # Helper role ID
    
    # Special roles
    staff: "123456789012345679"         # General staff role ID
    vip: "234567890123456780"           # VIP members role ID
    donator: "345678901234567891"       # Donator role ID

  # Discord server configurations
  guilds:
    # Primary Discord server
    main_server:
      id: "YOUR_MAIN_DISCORD_SERVER_ID"
      name: "Main Community Server"
      
      # Override specific roles for this guild (optional)
      role_overrides:
        # admin: "DIFFERENT_ADMIN_ROLE_ID_FOR_THIS_SERVER"
        # moderator: "DIFFERENT_MOD_ROLE_ID_FOR_THIS_SERVER"
      
      # Command permissions using role aliases
      permissions:
        # Punishment commands - High authority required
        ban: ["owner", "co_owner", "manager", "admin", "senior_admin"]
        unban: ["owner", "co_owner", "manager", "admin", "senior_admin"]
        
        # Moderation commands - Moderator level and above
        mute: ["owner", "admin", "head_mod", "senior_mod", "moderator"]
        unmute: ["owner", "admin", "head_mod", "senior_mod", "moderator"]
        kick: ["owner", "admin", "head_mod", "senior_mod", "moderator", "trial_mod"]
        
        # Information commands - Helper level and above
        list: ["staff", "helper", "vip"]  # Even VIPs can see online players
        punishment-list: ["owner", "admin", "head_mod", "senior_mod", "moderator"]
        
        # Server management - Admin level only
        broadcast: ["owner", "co_owner", "manager", "admin"]
        reload: ["owner", "co_owner"]  # Most restricted command
        
        # Help command - Available to everyone
        help: []  # Empty array = everyone can use
    
    # Secondary Discord server (example)
    # secondary_server:
    #   id: "YOUR_SECONDARY_DISCORD_SERVER_ID"
    #   name: "Secondary Server"
    #   role_overrides:
    #     admin: "DIFFERENT_ADMIN_ID"
    #   permissions:
    #     ban: ["admin"]
    #     list: ["helper"]

# Plugin settings
settings:
  # Command response settings
  ephemeral_responses: true  # Only command user sees the response
  
  # Punishment settings
  default_ban_reason: "Violating server rules"
  default_mute_reason: "Inappropriate behavior"
  default_kick_reason: "Rule violation"
  
  # Player list settings
  max_players_shown: 20  # Prevent spam in Discord
  
  # Debug mode (set to false in production)
  debug: false
```

### Step 5: Start Your Server
The plugin will automatically register Discord slash commands on startup!

## 🎮 Available Commands

| Command | Description | Required Parameters | Optional Parameters | Example Usage |
|---------|-------------|-------------------|-------------------|---------------|
| `/mc-ban` | Ban player with LiteBans | `player` | `duration`, `reason`, `ip-ban` | `/mc-ban player:Notch duration:7d reason:Cheating ip-ban:true` |
| `/mc-unban` | Remove all bans from player | `player` | `reason` | `/mc-unban player:Notch reason:Appeal approved` |
| `/mc-mute` | Mute player (chat/commands) | `player` | `duration`, `reason` | `/mc-mute player:Notch duration:1h reason:Spam` |
| `/mc-unmute` | Remove mute from player | `player` | `reason` | `/mc-unmute player:Notch reason:Time served` |
| `/mc-kick` | Kick player from server | `player` | `reason` | `/mc-kick player:Notch reason:Calm down` |
| `/list` | Show online players | None | None | `/list` |
| `/brc` | Broadcast message to server | `message` | None | `/brc message:Server restarting in 5 minutes!` |
| `/punishment-list` | View player punishment history | `player` | `punishment` | `/punishment-list player:Notch punishment:bans` |
| `/mc-dc-reload` | Reload plugin configuration | None | None | `/mc-dc-reload` |
| `/help` | Show available commands | None | None | `/help` |

### 📅 Duration Format Examples
You can combine multiple time units for precise durations:

- **Seconds:** `30s`, `45s`
- **Minutes:** `5m`, `30m`  
- **Hours:** `1h`, `2h`, `12h`
- **Days:** `1d`, `7d`, `30d`
- **Weeks:** `1w`, `2w`
- **Months:** `1mo`, `6mo`
- **Years:** `1y`, `5y`
- **Combined:** `1d12h30m` (1 day, 12 hours, 30 minutes)
- **Permanent:** Leave duration empty or use `perm`

### 📋 Punishment List Options
The `/punishment-list` command supports these punishment types:
- `bans` - Show only ban history
- `mutes` - Show only mute history  
- `kicks` - Show only kick history
- `warnings` - Show only warnings (if supported)
- `all` - Show complete punishment history (default)

## 🎭 Smart Role Management

### Why Use Role Aliases?
Instead of remembering long Discord role IDs like `832456789012345678`, use meaningful names:

❌ **Old way (confusing):**
```yaml
permissions:
  ban: ["832456789012345678", "123456789012345678"]  # Which role is which?
```

✅ **New way (clear):**
```yaml
root_roles:
  owner: "832456789012345678"
  admin: "123456789012345678"

permissions:
  ban: ["owner", "admin"]  # Much clearer!
```

### Setting Up Role Hierarchy
```yaml
root_roles:
  # Define from highest to lowest authority
  owner: "OWNER_ROLE_ID"           # Full access
  manager: "MANAGER_ROLE_ID"       # Server management
  admin: "ADMIN_ROLE_ID"           # Administrative tasks
  senior_mod: "SENIOR_MOD_ID"      # Advanced moderation
  moderator: "MODERATOR_ID"        # Basic moderation
  helper: "HELPER_ID"              # Support tasks
  
permissions:
  # Higher roles inherit lower role permissions automatically
  ban: ["owner", "manager", "admin"]
  mute: ["owner", "admin", "senior_mod", "moderator"]  
  list: ["helper"]  # Everyone above helper can also use this
```

### Per-Guild Role Overrides
Different Discord servers can use different role IDs for the same position:

```yaml
guilds:
  main_server:
    id: "111111111111111111"
    permissions:
      ban: ["admin"]  # Uses global admin role ID
  
  secondary_server:
    id: "222222222222222222"
    role_overrides:
      admin: "DIFFERENT_ADMIN_ROLE_ID"  # Override for this server only
    permissions:
      ban: ["admin"]  # Uses the overridden admin role ID
```

## 🔍 Getting Discord IDs

### Enable Developer Mode
1. Open Discord → User Settings (gear icon)
2. Go to **Advanced** → Toggle **Developer Mode** ON

### Copy IDs
- **Server ID:** Right-click server name → "Copy Server ID"
- **Role ID:** Server Settings → Roles → Right-click role → "Copy Role ID"  
- **User ID:** Right-click username → "Copy User ID"
- **Channel ID:** Right-click channel → "Copy Channel ID"

## ⚡ Advanced Features

### 🛡️ **LiteBans Integration**
- Professional punishment system with proper database storage
- Ban appeals and punishment tracking
- IP banning support with `ip-ban:true` parameter
- Unlimited ban duration support (years, decades, permanent)

### 🎨 **Rich Discord Embeds**
- Color-coded punishment messages (red for bans, yellow for mutes, etc.)
- Professional formatting with timestamps
- Player avatar integration when possible
- Success/error status indicators

### 🚀 **Async Processing**  
- All Discord operations run asynchronously
- Zero impact on Minecraft server performance
- Prevents server lag during Discord API calls

### 📝 **Discord-Safe Formatting**
- Automatically fixes Discord formatting issues
- Converts `_username_` to proper display (prevents italics)
- Handles special characters in player names
- Escapes Discord markdown in messages

### 🔄 **Hot Configuration Reload**
- Reload config without server restart using `/mc-dc-reload`
- Shows "Restart Pushed" confirmation message
- Updates all permissions and settings immediately
- Validates configuration before applying changes

## 🚨 Troubleshooting

### Common Issues & Solutions

**🤖 Bot Not Responding**
- ✅ Verify bot token is correct in config.yml
- ✅ Check if bot is online in Discord server
- ✅ Ensure bot has proper permissions in Discord server
- ✅ Confirm guild IDs are correct

**🚫 Permission Denied Errors**
- ✅ Check user has required Discord role
- ✅ Verify role IDs in `root_roles` section are correct
- ✅ Confirm role aliases are spelled correctly
- ✅ Check if `role_overrides` are interfering

**⚖️ Ban/Unban Commands Failing**
- ✅ Install LiteBans plugin (required dependency)
- ✅ Ensure LiteBans database is configured properly
- ✅ Check if player name exists/is correct
- ✅ Verify LiteBans is working with `/litebans` command in-game

**🔇 Mute/Unmute Not Working**
- ✅ Install EssentialsX for better mute support
- ✅ Check if player is actually online
- ✅ Verify mute duration format is correct
- ✅ Test manual mute commands in-game first

**📝 Names Appearing Italic in Discord**
- ✅ Plugin automatically fixes this - no action needed
- ✅ If still occurring, check for plugin conflicts

**🔧 Plugin Won't Load**
- ✅ Verify Java version (8+ required)
- ✅ Check server version compatibility (1.8.8+)
- ✅ Review server logs for specific error messages
- ✅ Ensure all dependencies are installed

**❓ "Unknown Role Alias" Error**
- ✅ Check `root_roles` section for typos
- ✅ Verify role alias exists in configuration
- ✅ Ensure role IDs are valid Discord role IDs

### Debug Mode
Enable debug mode for detailed logging:
```yaml
settings:
  debug: true
```
This will show detailed information about:
- Discord API calls
- Permission checks
- Command processing
- Error details

## 📊 Permissions Matrix

| Role Level | Ban | Unban | Mute | Unmute | Kick | List | Broadcast | Punishment List | Reload |
|------------|-----|-------|------|--------|------|------|-----------|----------------|---------|
| Owner | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Admin | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| Senior Mod | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| Moderator | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| Helper | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |

*This is a sample matrix - customize permissions in your config.yml*

## 🤝 Support & Contributing

**🐛 Found a Bug?**
- Create an issue on [GitHub Issues](https://github.com/dreamrela/MCDiscordBridge/issues)
- Include server version, plugin version, and error logs
- Describe steps to reproduce the issue

**💡 Feature Request?**
- Open a feature request on GitHub
- Describe the feature and its benefits
- Include example usage scenarios

**🔧 Want to Contribute?**
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

**📞 Need Help?**
- Check this README thoroughly
- Search existing GitHub issues
- Join our Discord community (if available)
- Contact developer: dreamrela

## 📄 License

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

## 🏆 Credits

**Developer:** [dreamrela](https://github.com/dreamrela)  
**Special Thanks:**
- LiteBans team for the excellent punishment system
- EssentialsX team for moderation tools
- JDA team for Discord integration
- Spigot/Paper community for server support

---

<div align="center">

# **Made with ❤️ by [Dreamrela](https://github.com/dreamrela)**

*⭐ Star this repo if MCDiscordBridge helps your server!*

[![GitHub stars](https://img.shields.io/github/stars/dreamrela/MCDiscordBridge?style=social)](https://github.com/dreamrela/MCDiscordBridge/stars)
[![GitHub forks](https://img.shields.io/github/forks/dreamrela/MCDiscordBridge?style=social)](https://github.com/dreamrela/MCDiscordBridge/fprks)

</div>
