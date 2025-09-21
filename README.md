# 🌉 MCDiscordBridge

<div align="center">

[![Java](https://img.shields.io/badge/Java-8-orange.svg)](https://www.oracle.com/java/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.8.8-green.svg)](https://minecraft.net)
[![Discord](https://img.shields.io/badge/Discord-JDA%205.0-blue.svg)](https://github.com/DV8FromTheWorld/JDA)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

*Powerful Discord-Minecraft integration for seamless server moderation*

</div>

## ✨ Features

🚫 **Ban/Unban** with LiteBans integration  
🔇 **Mute/Unmute** with duration control  
👢 **Kick** players instantly  
📊 **List** online players  
📢 **Broadcast** server messages  
🔄 **Hot Reload** configuration  
🌍 **Multi-Guild** support with role permissions  

## 📋 Requirements

- **Java 8+** and **Minecraft 1.8.8** (CarbonSpigot/Paper)
- **Discord Bot** with slash command permissions
- **LiteBans** plugin (for ban/unban functionality)

## 🚀 Quick Start

### 1. Create Discord Bot
1. Go to [Discord Developer Portal](https://discord.com/developers/applications)
2. Create new application → Bot → Copy token
3. Enable "Server Members Intent"
4. Invite bot with `bot` + `applications.commands` scopes

### 2. Install Plugin
```bash
# Build from source
mvn clean package

# Copy JAR to plugins folder
cp target/MCDiscordBridge-1.0.jar /path/to/server/plugins/
```

### 3. Configure
Edit `plugins/MCDiscordBridge/config.yml`:
```yaml
discord:
  token: "YOUR_BOT_TOKEN_HERE"
  guilds:
    guild1:
      id: "YOUR_DISCORD_SERVER_ID"
      name: "Main Server"
      permissions:
        ban: ["ADMIN_ROLE_ID", "MOD_ROLE_ID"]
        unban: ["ADMIN_ROLE_ID", "MOD_ROLE_ID"]
        mute: ["ADMIN_ROLE_ID", "MOD_ROLE_ID"]
        kick: ["ADMIN_ROLE_ID", "MOD_ROLE_ID"]
        list: ["STAFF_ROLE_ID"]
        broadcast: ["ADMIN_ROLE_ID"]
        reload: ["ADMIN_ROLE_ID"]
        help: []  # Everyone
```

### 4. Start Server
Plugin will automatically register Discord slash commands!

## 🎮 Commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/mc-ban` | Ban player with LiteBans | `player:username duration:7d reason:text ip-ban:false` |
| `/mc-unban` | Unban player | `player:username reason:text` |
| `/mc-mute` | Mute player | `player:username duration:1h reason:text` |
| `/mc-unmute` | Unmute player | `player:username reason:text` |
| `/mc-kick` | Kick player | `player:username reason:text` |
| `/list` | Show online players | - |
| `/brc` | Broadcast message | `text:message` |
| `/mc-dc-reload` | Reload config | - |
| `/help` | Show commands | - |

**Duration formats:** `30s`, `5m`, `2h`, `7d`, `1w`, `1mo` (can combine: `1d2h30m`)

## 🔧 Key Features

- **🚀 Async Processing** - Zero server lag
- **🛡️ LiteBans Integration** - Professional ban system
- **🎨 Rich Embeds** - Beautiful Discord messages
- **🔒 Role Permissions** - Granular access control
- **🌍 Multi-Guild** - Multiple Discord servers
- **📝 Discord Safe** - Proper name formatting (fixes `_user_` → *user* issue)
- **🔄 Hot Reload** - "Restart Pushed" message with live config reload

## 🔍 Getting Discord IDs

1. Enable Developer Mode: Discord Settings → Advanced → Developer Mode
2. **Server ID:** Right-click server name → Copy ID
3. **Role ID:** Server Settings → Roles → Right-click role → Copy ID

## 🚨 Troubleshooting

- **Bot not responding:** Check token and guild IDs in config
- **Permission denied:** Verify role IDs and user roles
- **Ban/unban fails:** Install LiteBans plugin
- **Names italic in Discord:** Plugin auto-fixes this

## 🤝 Support

**Developer:** dreamrela  
**Issues:** [GitHub Issues](https://github.com/dreamrela/MCDiscordBridge/issues)

---

<div align="center">

**Made with ❤️ by [dreamrela](https://github.com/dreamrela)**

*⭐ Star this repo if you find it helpful!*

</div>
