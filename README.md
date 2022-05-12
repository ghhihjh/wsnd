YuiServer是基于Grasscutter构建的Genshin Impact Private Server，同时也将作为Grasscutter的备胎，并会与Grasscutter的代码保持同步。因此，如果你遇到了什么问题，Grasscutter的解决方案一般情况下都是可以直接套用到YuiServer上的，
YuiServer是一个免费公益性质，非盈利的私人服务器，请确保你没有通过付费进入，如有付费渠道，请举报反馈哦~ 

此项目是YuiServer服务器“星悦梦璃”的镜像，服务器的所有改动、内容都将同步此镜像。
克隆本镜像即可达到与YuiServer完全一摸一样的体验（包含bug

不定期更新data文件，例如在杂货店添加材料、修改卡池，也可能会更新资源文件夹。

如果你不想搭建，可以直接游玩YuiServer.
本服务器支持PC Android iOS
请求各位大佬不要Ddos，本服无任何内部群、圈钱群、支付X元进群等等。都是被倒卖的。
近期被DDOS严重，已经逐渐崩坏了，求求了

加入YuiServer:
https://yuina.cn/yuiserver/


EN | [中文](README_zh-CN.md)

**Attention:** We always welcome contributors to the project. Before adding your contribution, please carefully read our [Code of Conduct](https://github.com/Grasscutters/Grasscutter/blob/stable/CONTRIBUTING.md).

## Current features

* Logging in
* Combat
* Friends list
* Teleportation
* Gacha system
* Co-op *partially* works
* Spawning monsters via console
* Inventory features (recieving items/characters, upgrading items/characters, etc)

## Quick setup guide

**Note:** For support please join our [Discord](https://discord.gg/T5vZU6UyeG).

### Requirements

* Java SE - 17 ([link](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html))

  **Note:** If you just want to **run it**, then **jre** only is fine.

* MongoDB (recommended 4.0+)

* Proxy daemon: mitmproxy (mitmdump, recommended), Fiddler Classic, etc.

### Running

**Note:** If you updated from an older version, delete `config.json` to regenerate it.

1. Get `grasscutter.jar`
   - Download from [actions](https://nightly.link/Grasscutters/Grasscutter/workflows/build/stable/Grasscutter.zip)
   - [Build by yourself](#Building)
2. Create a `resources` folder in the directory where grasscutter.jar is located and move your `BinOutput` and `ExcelBinOutput` folders there *(Check the [wiki](https://github.com/Grasscutters/Grasscutter/wiki) for more details how to get those.)*
3. Run Grasscutter with `java -jar grasscutter.jar`. **Make sure mongodb service is running as well.**

### Connecting with the client

½. Create an account using [server console command](#Commands).

1. Redirect traffic: (choose one)
    - mitmdump: `mitmdump -s proxy.py -k`
    
      Trust CA certificate:
    
      ​	**Note:**The CA certificate is usually stored in `% USERPROFILE%\ .mitmproxy`, or you can download it from `http://mitm.it`
    
      ​	Double click for [install](https://docs.microsoft.com/en-us/skype-sdk/sdn/articles/installing-the-trusted-root-certificate#installing-a-trusted-root-certificate) or ...
    
      - Via command line
    
        ```shell
        certutil -addstore root %USERPROFILE%\.mitmproxy\mitmproxy-ca-cert.cer
        ```
    
    - Fiddler Classic: Run Fiddler Classic, turn on `Decrypt https traffic` in setting and change the default port there (Tools -> Options -> Connections) to anything other than `8888`, and load [this script](https://github.lunatic.moe/fiddlerscript).
      
    - [Hosts file](https://github.com/Melledy/Grasscutter/wiki/Running#traffic-route-map)
    
2. Set network proxy to `127.0.0.1:8080` or the proxy port you specified.

**you can also use `start.cmd` to start servers and proxy daemons automatically**

### Building

Grasscutter uses Gradle to handle dependencies & building.

**Requirements:**

- Java SE Development Kits - 17
- Git

##### Windows

```shell
git clone https://github.com/Grasscutters/Grasscutter.git
cd Grasscutter
.\gradlew.bat # Setting up environments
.\gradlew jar # Compile
```

##### Linux

```bash
git clone https://github.com/Grasscutters/Grasscutter.git
cd Grasscutter
chmod +x gradlew
./gradlew jar # Compile
```

You can find the output jar in the root of the project folder.

## Commands

You might want to use this command (`java -jar grasscutter.jar -handbook`) in a cmd that is in the grasscutter folder. It will create a handbook file (GM Handbook.txt) where you can find the item IDs for stuff you want

You may want to use this command (`java -jar grasscutter.jar -gachamap`) to generate a mapping file for the gacha record subsystem. The file will be generated to `GRASSCUTTER_RESOURCE/gcstatic` folder. Otherwise you may only see number IDs in the gacha record page.

There is a dummy user named "Server" in every player's friends list that you can message to use commands. Commands also work in other chat rooms, such as private/team chats. to run commands ingame, you need to add prefix `/` or `!` such as `/pos`

| Commands       | Usage                                             | Permission node           | Availability | description                                                  | Alias                                           |
| -------------- | ------------------------------------------------- | ------------------------- | ------------ | ------------------------------------------------------------ | ----------------------------------------------- |
| account        | account <create\|delete> \<username> [UID]         |                           | Server only  | Creates an account with the specified username and the in-game UID for that account. The UID will be auto generated if not set. |                                                 |
| broadcast      | broadcast \<message>                               | server.broadcast          | Both side    | Sends a message to all the players.                          | b                                               |
| coop           | coop \<playerId> \<target playerId>                 | server.coop               | Both side    | Forces someone to join the world of others.                  |                                               |
| changescene    | changescene \<scene id>                            | player.changescene        | Client only  | Switch scenes by scene ID.                                   | scene                                           |
| clear          | clear <all\|wp\|art\|mat> [UID]                     | player.clearinv         | Client only  | Deletes all unequipped and unlocked level 0 artifacts(art)/weapons(wp)/material(all) or all, including 5-star rarity ones from your inventory. | clear                                        |
| drop           | drop <itemID\|itemName> [amount]                  | server.drop               | Client only  | Drops an item around you.                                    | `d` `dropitem`                                  |
| enterdungeon   | enterdungeon \<dungeon id>                        | player.enterdungeon       | Client only  | Enter a dungeon by dungeon ID                                |                                                 |
| give           | give [player] <itemId\|itemName> [amount] [level] [finement] | player.give    | Both side    | Gives item(s) to you or the specified player. (finement option only weapon.) | `g` `item` `giveitem`           |
| givechar       | givechar \<uid> \<avatarId>                 | player.givechar           | Both side    | Gives the player a specified character.                      | givec                                           |
| giveart        | giveart [player] \<artifactId> \<mainPropId> [\<appendPropId>[,\<times>]]... [level] | player.giveart            | Both side    | Gives the player a specified artifact.                      | gart                                           |
| giveall       | giveall [uid] [amount]             | player.giveall      | Both side    | Gives all items.      | givea                                         |
| godmode        | godmode [uid]                                     | player.godmode            | Client only  | Prevents you from taking damage.                             |                                                 |
| heal           | heal                                              | player.heal               | Client only  | Heals all characters in your current team.                    | h                                               |
| help           | help [command]                                    |                           | Both side    | Sends the help message or shows information about a specified command. |                                                 |
| kick           | kick \<player>                                     | server.kick               | Both side    | Kicks the specified player from the server. (WIP)            | k                                               |
| killall        | killall [playerUid] [sceneId]                     | server.killall            | Both side    | Kills all entities in the current scene or specified scene of the corresponding player. |                                                 |
| list           | list                                              |                           | Both side    | Lists online players.                                         |                                                 |
| permission     | permission <add\|remove> \<UID> \<permission>  | *                         | Both side    | Grants or removes a permission for a user.                   |                                                 |
| position       | position                                          |                           | Client only  | Sends your current coordinates.                                             | pos                                             |
| reload         | reload                                            | server.reload             | Both side    | Reloads the server config                                         |                                                 |
| resetconst     | resetconst [all]                                  | player.resetconstellation | Client only  | Resets the constellation level on your currently selected character, will need to relog after using the command to see any changes. | resetconstellation                              |
| restart        |                                                   |                           | Both side    | Restarts the current session                                 |                                                 |
| say            | say \<player> \<message>                            | server.sendmessage        | Both side    | Sends a message to a player as the server                    | `sendservmsg` `sendservermessage` `sendmessage` |
| setfetterlevel | setfetterlevel \<level>                            | player.setfetterlevel     | Client only  | Sets the friendship level for your currently selected character     | setfetterlvl                                    |
| setstats       | setstats \<stat> \<value>                           | player.setstats           | Client only  | Sets a stat for your currently selected character         | stats                                           |
| setworldlevel  | setworldlevel \<level>                             | player.setworldlevel      | Client only  | Sets your world level (Relog to see proper effects)          | setworldlvl                                     |
| spawn          | spawn \<entityId> [amount] [level(monster only)]  | server.spawn              | Client only  | Spawns some entities around you                              |                                                 |
| stop           | stop                                              | server.stop               | Both side    | Stops the server                                             |                                                 |
| talent         | talent \<talentID> \<value>                         | player.settalent          | Client only  | Sets talent level for your currently selected character           |                                                 |
| teleport       | teleport [@playerUid] \<x> \<y> \<z> [sceneId]                             | player.teleport           | Both side  | Change the player's position.                                | tp                                              |
| tpall          |                                                   | player.tpall              | Client only  | Teleports all players in your world to your position         |                                                 |
| unlocktower    |                                                                                      | player.tower                          | Client only  | Unlock the all floors of abyss                                                                                                      | ut                                                                                                                              |
| weather        | weather \<weatherID> \<climateID>                   | player.weather            | Client only  | Changes the weather                                          | w                                               |

### Bonus

- Teleporting
  - When you want to teleport to somewhere, use the in-game marking function on Map.
    - Mark a point on the map using the fish hook marking (the last one.)
    - (Optional) rename the map marker to a number to override the default Y coordinate (height, default 300.)
    - Confirm and close the map.
    - You will see your character falling from a very high destination, exact location that you marked.
 
# Quick Troubleshooting

* If compiling wasn't successful, please check your JDK installation (JDK 17 and validated JDK's bin PATH variable)
* My client doesn't connect, doesn't login, 4206, etc... - Mostly your proxy daemon setup is *the issue*, if using
  Fiddler make sure it running on another port except 8888

* Startup sequence: Mongodb > Grasscutter > Proxy daemon (mitmdump, fiddler, etc.) > Game
