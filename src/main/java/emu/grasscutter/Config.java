package emu.grasscutter;

import java.util.Locale;
import emu.grasscutter.Grasscutter.ServerDebugMode;
import emu.grasscutter.Grasscutter.ServerRunMode;
import emu.grasscutter.game.mail.Mail;

public final class Config {
	public String DatabaseUrl = "mongodb://localhost:27017";
	public String DatabaseCollection = "grasscutter";

	public String RESOURCE_FOLDER = "./resources/";
	public String DATA_FOLDER = "./data/";
	public String PACKETS_FOLDER = "./packets/";
	public String DUMPS_FOLDER = "./dumps/";
	public String KEY_FOLDER = "./keys/";
	public String SCRIPTS_FOLDER = "./resources/Scripts/";
	public String PLUGINS_FOLDER = "./plugins/";
	public String LANGUAGE_FOLDER = "./languages/";

	public ServerDebugMode DebugMode = ServerDebugMode.NONE; // ALL, MISSING, NONE
	public ServerRunMode RunMode = ServerRunMode.HYBRID; // HYBRID, DISPATCH_ONLY, GAME_ONLY
	public GameServerOptions GameServer = new GameServerOptions();
	public DispatchServerOptions DispatchServer = new DispatchServerOptions();
	public Locale LocaleLanguage = Locale.getDefault();
	public Locale DefaultLanguage = Locale.US;

	public Boolean OpenStamina = true;
	public GameServerOptions getGameServerOptions() {
		return GameServer;
	}

	public DispatchServerOptions getDispatchOptions() { return DispatchServer; }

	public static class DispatchServerOptions {
		public String Ip = "0.0.0.0";
		public String PublicIp = "127.0.0.1";
		public int Port = 443;
		public int PublicPort = 0;
		public String KeystorePath = "./keystore.p12";
		public String KeystorePassword = "123456";
		public Boolean UseSSL = true;
		public Boolean FrontHTTPS = true;
		public Boolean CORS = false;
		public String[] CORSAllowedOrigins = new String[] { "*" };

		public boolean AutomaticallyCreateAccounts = false;
		public String[] defaultPermissions = new String[] { "" };

		public RegionInfo[] GameServers = {};

		public RegionInfo[] getGameServers() {
			return GameServers;
		}

		public static class RegionInfo {
			public String Name = "os_usa";
			public String Title = "Test";
			public String Ip = "127.0.0.1";
			public int Port = 22102;
		}
	}

	public static class GameServerOptions {
		public String Name = "Test";
		public String Ip = "0.0.0.0";
		public String PublicIp = "127.0.0.1";
		public int Port = 22102;
		public int PublicPort = 0;

		public String DispatchServerDatabaseUrl = "mongodb://localhost:27017";
		public String DispatchServerDatabaseCollection = "grasscutter";

		public int InventoryLimitWeapon = 2000;
		public int InventoryLimitRelic = 2000;
		public int InventoryLimitMaterial = 2000;
		public int InventoryLimitFurniture = 2000;
		public int InventoryLimitAll = 30000;
		public int MaxAvatarsInTeam = 4;
		public int MaxAvatarsInTeamMultiplayer = 4;
		public int MaxEntityLimit = 1000; // Max entity limit per world. // TODO: Enforce later.
		public boolean WatchGacha = false;
		public String ServerNickname = "YuiServer";
		public int ServerAvatarId = 10000007;
		public int ServerNameCardId = 210001;
		public int ServerLevel = 1;
		public int ServerWorldLevel = 1;
		public String ServerSignature = "Server Signature";
		public int[] WelcomeEmotes = {2007, 1002, 4010};
		public String WelcomeMotd = "Welcome to YuiServer-星悦梦璃";
		public String WelcomeMailTitle = "Welcome to YuiServer-星悦梦璃!";
		public String WelcomeMailSender = "Yui";
		public String WelcomeMailContent = "Hi There!\r\n在一切之前，我想先跟大家说：欢迎来到YuiServer。YuiServer是基于Grasscutter构建的Genshin Impact Private Server，同时也将作为Grasscutter的备胎，并会与Grasscutter的代码保持同步。因此，如果你遇到了什么问题，Grasscutter的解决方案一般情况下都是可以直接套用到YuiServer上的，YuiServer是一个免费公益性质，非盈利的私人服务器，请确保你没有通过付费进入，如有付费渠道，请举报反馈哦~ \r\n\r\n链接：\r\n\u003ctype\u003d\"browser\" text\u003d\"YuiServer的QQ群\" href\u003d\"https://jq.qq.com/?_wv\u003d1027\u0026k\u003djBHOcuWI\"/\u003e \u003ctype\u003d\"browser\" text\u003d\"YuiServer主页\" href\u003d\"https://yuina.cn/ys\"/\u003e \u003ctype\u003d\"browser\" text\u003d\"YuiServer的Github开源页面\" href\u003d\"https://github.com/Searchstars/YuiServer\"/\u003e";
		public Mail.MailItem[] WelcomeMailItems = {
				new Mail.MailItem(223, 1000, 1),
		};

		public boolean EnableOfficialShop = true;

		public GameRates Game = new GameRates();

		public GameRates getGameRates() { return Game; }

		public static class GameRates {
			public float ADVENTURE_EXP_RATE = 1.0f;
			public float MORA_RATE = 1.0f;
			public float DOMAIN_DROP_RATE = 1.0f;
		}
	}
}
