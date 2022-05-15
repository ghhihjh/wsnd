package emu.grasscutter.utils;

import com.google.gson.JsonObject;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.Grasscutter.ServerDebugMode;
import emu.grasscutter.Grasscutter.ServerRunMode;

import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Locale;

import static emu.grasscutter.Grasscutter.config;

/**
 * *when your JVM fails*
 */
public class ConfigContainer {
    private static int version() {
        return 3;
    }

    /**
     * Attempts to update the server's existing configuration to the latest 
     */
    public static void updateConfig() {
        try { // Check if the server is using a legacy config.
            JsonObject configObject = Grasscutter.getGsonFactory()
                    .fromJson(new FileReader(Grasscutter.configFile), JsonObject.class);
            if(!configObject.has("version")) {
                Grasscutter.getLogger().info("Updating legacy ..");
                Grasscutter.saveConfig(null);
            }
        } catch (Exception ignored) { }

        var existing = config.version;
        var latest = version();

        if(existing == latest)
            return;

        // Create a new configuration instance.
        ConfigContainer updated = new ConfigContainer();
        // Update all configuration fields.
        Field[] fields = ConfigContainer.class.getDeclaredFields();
        Arrays.stream(fields).forEach(field -> {
            try {
                field.set(updated, field.get(config));
            } catch (Exception exception) {
                Grasscutter.getLogger().error("Failed to update a configuration field.", exception);
            }
        }); updated.version = version();

        try { // Save configuration & reload.
            Grasscutter.saveConfig(updated);
            Grasscutter.loadConfig();
        } catch (Exception exception) {
            Grasscutter.getLogger().warn("Failed to inject the updated ", exception);
        }
    }
    
    public Structure folderStructure = new Structure();
    public Database databaseInfo = new Database();
    public Language language = new Language();
    public Account account = new Account();
    public Server server = new Server();

    // DO NOT. TOUCH. THE VERSION NUMBER.
    public int version = version();

    /* Option containers. */

    public static class Database {
        public DataStore server = new DataStore();
        public DataStore game = new DataStore();
        
        public static class DataStore {
            public String connectionUri = "mongodb://localhost:27017";
            public String collection = "grasscutter";
        }
    }

    public static class Structure {
        public String resources = "./resources/";
        public String data = "./data/";
        public String packets = "./packets/";
        public String keys = "./keys/";
        public String scripts = "./resources/scripts/";
        public String plugins = "./plugins/";

        // UNUSED (potentially added later?)
        // public String dumps = "./dumps/";
    }

    public static class Server {
        public ServerDebugMode debugLevel = ServerDebugMode.NONE;
        public ServerRunMode runMode = ServerRunMode.HYBRID;

        public HTTP http = new HTTP();
        public Game game = new Game();
        
        public Dispatch dispatch = new Dispatch();
    }

    public static class Language {
        public Locale language = Locale.getDefault();
        public Locale fallback = Locale.US;
    }

    public static class Account {
        public boolean autoCreate = false;
        public String[] defaultPermissions = {};
    }

    /* Server options. */
    
    public static class HTTP {
        public String bindAddress = "0.0.0.0";
        /* This is the address used in URLs. */
        public String accessAddress = "127.0.0.1";

        public int bindPort = 443;
        /* This is the port used in URLs. */
        public int accessPort = 0;
        
        public Encryption encryption = new Encryption();
        public Policies policies = new Policies();
        public Files files = new Files();
    }

    public static class Game {
        public String bindAddress = "0.0.0.0";
        /* This is the address used in the default region. */
        public String accessAddress = "127.0.0.1";

        public int bindPort = 22102;
        /* This is the port used in the default region. */
        public int accessPort = 0;

        public GameOptions gameOptions = new GameOptions();
        public JoinOptions joinOptions = new JoinOptions();
        public ConsoleAccount serverAccount = new ConsoleAccount();
    }

    /* Data containers. */

    public static class Dispatch {
        public Region[] regions = {};

        public String defaultName = "Grasscutter";
    }

    public static class Encryption {
        public boolean useEncryption = true;
        /* Should 'https' be appended to URLs? */
        public boolean useInRouting = true;
        public String keystore = "./keystore.p12";
        public String keystorePassword = "123456";
    }

    public static class Policies {
        public Policies.CORS cors = new Policies.CORS();

        public static class CORS {
            public boolean enabled = false;
            public String[] allowedOrigins = new String[]{"*"};
        }
    }

    public static class GameOptions {
        public InventoryLimits inventoryLimits = new InventoryLimits();
        public AvatarLimits avatarLimits = new AvatarLimits();
        public int worldEntityLimit = 1000; // Unenforced. TODO: Implement.

        public boolean watchGachaConfig = false;
        public boolean enableShopItems = true;
        public boolean staminaUsage = true;
        public Rates rates = new Rates();

        public static class InventoryLimits {
            public int weapons = 2000;
            public int relics = 2000;
            public int materials = 2000;
            public int furniture = 2000;
            public int all = 30000;
        }

        public static class AvatarLimits {
            public int singlePlayerTeam = 4;
            public int multiplayerTeam = 4;
        }

        public static class Rates {
            public float adventureExp = 1.0f;
            public float mora = 1.0f;
            public float leyLines = 1.0f;
        }
    }

    public static class JoinOptions {
        public int[] welcomeEmotes = {2007, 1002, 4010};
        public String welcomeMessage = "Welcome to YuiServer-<color=#9795f0>星</color><color=#0099CC>悦</color><color=#FF99CC>梦</color><color=#66CC66>璃</color>";
        public JoinOptions.Mail welcomeMail = new JoinOptions.Mail();

        public static class Mail {
            public String title = "Welcome to YuiServer-<color=#9795f0>星悦</color><color=#fbc8d4>梦璃</color>!";
            public String content = """
                    Hi there!\r
                    在一切之前，我想先跟大家说：欢迎来到YuiServer。\r
                    YuiServer是基于Grasscutter构建的Genshin Impact Private Server，同时也将作为Grasscutter的备胎，并会与Grasscutter的代码保持同步。\r
                    因此，如果你遇到了什么问题，Grasscutter的解决方案一般情况下都是可以直接套用到YuiServer上的\r
                    YuiServer是一个免费公益性质，非盈利的私人服务器，请确保你没有通过付费进入，如有付费渠道，请举报反馈哦~ \r\n
                    \r
                    链接：\r
                    <type="browser" text="QQ群" href="https://jq.qq.com/?_wv=1027&k=Ydgq0Bhk"/>
                    """;
            public String sender = "<color=#FF9999>悦酱</color>";
            public emu.grasscutter.game.mail.Mail.MailItem[] items = {
                    new emu.grasscutter.game.mail.Mail.MailItem(223, 1000, 1)
            };
        }
    }

    public static class ConsoleAccount {
        public int avatarId = 10000054;
        public int nameCardId = 210097;
        public int adventureRank = 60;
        public int worldLevel = 8;

        public String nickName = "<color=#9795f0>Yui</color><color=#0099CC>Server</color>-<color=#FF99CC>星悦</color><color=#66CC66>梦璃</color>";
        public String signature = "可可爱爱的YuiServer酱~";
    }
    
    public static class Files {
        public String indexFile = "./index.html";
        public String errorFile = "./404.html";
    }

    /* Objects. */

    public static class Region {
        public Region() { }
        
        public Region(
                String name, String title,
                String address, int port
        ) {
            this.Name = name;
            this.Title = title;
            this.Ip = address;
            this.Port  = port;
        }
        
        public String Name = "os_usa";
        public String Title = "Grasscutter";
        public String Ip = "127.0.0.1";
        public int Port = 22102;
    }
}
