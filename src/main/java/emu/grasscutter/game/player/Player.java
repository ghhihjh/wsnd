package emu.grasscutter.game.player;

import dev.morphia.annotations.*;
import emu.grasscutter.GameConstants;
import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.def.PlayerLevelData;
import emu.grasscutter.database.DatabaseHelper;
import emu.grasscutter.game.Account;
import emu.grasscutter.game.CoopRequest;
import emu.grasscutter.game.avatar.Avatar;
import emu.grasscutter.game.avatar.AvatarProfileData;
import emu.grasscutter.game.avatar.AvatarStorage;
import emu.grasscutter.game.entity.EntityItem;
import emu.grasscutter.game.entity.GameEntity;
import emu.grasscutter.game.friends.FriendsList;
import emu.grasscutter.game.friends.PlayerProfile;
import emu.grasscutter.game.gacha.PlayerGachaInfo;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.inventory.Inventory;
import emu.grasscutter.game.mail.Mail;
import emu.grasscutter.game.props.ActionReason;
import emu.grasscutter.game.props.PlayerProperty;
import emu.grasscutter.game.shop.ShopLimit;
import emu.grasscutter.game.world.Scene;
import emu.grasscutter.game.world.World;
import emu.grasscutter.net.packet.BasePacket;
import emu.grasscutter.net.proto.AbilityInvokeEntryOuterClass.AbilityInvokeEntry;
import emu.grasscutter.net.proto.CombatInvokeEntryOuterClass.CombatInvokeEntry;
import emu.grasscutter.net.proto.InteractTypeOuterClass.InteractType;
import emu.grasscutter.net.proto.MpSettingTypeOuterClass.MpSettingType;
import emu.grasscutter.net.proto.OnlinePlayerInfoOuterClass.OnlinePlayerInfo;
import emu.grasscutter.net.proto.PlayerApplyEnterMpResultNotifyOuterClass;
import emu.grasscutter.net.proto.PlayerLocationInfoOuterClass.PlayerLocationInfo;
import emu.grasscutter.net.proto.PlayerWorldLocationInfoOuterClass;
import emu.grasscutter.net.proto.ShowAvatarInfoOuterClass;
import emu.grasscutter.net.proto.ProfilePictureOuterClass.ProfilePicture;
import emu.grasscutter.net.proto.SocialDetailOuterClass.SocialDetail;
import emu.grasscutter.net.proto.SocialShowAvatarInfoOuterClass;
import emu.grasscutter.server.event.player.PlayerJoinEvent;
import emu.grasscutter.server.event.player.PlayerQuitEvent;
import emu.grasscutter.server.event.player.PlayerReceiveMailEvent;
import emu.grasscutter.server.game.GameServer;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.*;
import emu.grasscutter.utils.DateHelper;
import emu.grasscutter.utils.Position;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.time.Instant;
import java.util.*;

@Entity(value = "players", useDiscriminator = false)
public class Player {
	@Id private int id;
	@Indexed(options = @IndexOptions(unique = true)) private String accountId;

	@Transient private Account account;
	private String nickname;
	private String signature;
	private int headImage;
	private int nameCardId = 210001;
	private Position pos;
	private Position rotation;
	private PlayerBirthday birthday;

	private Map<Integer, Integer> properties;
	private Set<Integer> nameCardList;
	private Set<Integer> flyCloakList;
	private Set<Integer> costumeList;

	@Transient private long nextGuid = 0;
	@Transient private int peerId;
	@Transient private World world;
	@Transient private Scene scene;
	@Transient private GameSession session;
	@Transient private AvatarStorage avatars;
	@Transient private Inventory inventory;
	@Transient private FriendsList friendsList;

	private TeamManager teamManager;
	private PlayerGachaInfo gachaInfo;
	private PlayerProfile playerProfile;
	private boolean showAvatar;
	private ArrayList<AvatarProfileData> shownAvatars;
	private Set<Integer> rewardedLevels;
	private ArrayList<Mail> mail;
	private ArrayList<ShopLimit> shopLimit;

	private int sceneId;
	private int regionId;
	private int mainCharacterId;
	private boolean godmode;

	private boolean moonCard;
	private Date moonCardStartTime;
	private int moonCardDuration;
	private Set<Date> moonCardGetTimes;

	private List<Integer> showAvatarList;
	private boolean showAvatars;

	@Transient private boolean paused;
	@Transient private int enterSceneToken;
	@Transient private SceneLoadState sceneState;
	@Transient private boolean hasSentAvatarDataNotify;
	@Transient private long nextSendPlayerLocTime = 0;

	@Transient private final Int2ObjectMap<CoopRequest> coopRequests;
	@Transient private final InvokeHandler<CombatInvokeEntry> combatInvokeHandler;
	@Transient private final InvokeHandler<AbilityInvokeEntry> abilityInvokeHandler;
	@Transient private final InvokeHandler<AbilityInvokeEntry> clientAbilityInitFinishHandler;

	@Deprecated
	@SuppressWarnings({"rawtypes", "unchecked"}) // Morphia only!
	public Player() {
		this.inventory = new Inventory(this);
		this.avatars = new AvatarStorage(this);
		this.friendsList = new FriendsList(this);
		this.pos = new Position();
		this.rotation = new Position();
		this.properties = new HashMap<>();
		for (PlayerProperty prop : PlayerProperty.values()) {
			if (prop.getId() < 10000) {
				continue;
			}
			this.properties.put(prop.getId(), 0);
		}

		this.gachaInfo = new PlayerGachaInfo();
		this.nameCardList = new HashSet<>();
		this.flyCloakList = new HashSet<>();
		this.costumeList = new HashSet<>();

		this.mail = new ArrayList<>();

		this.setSceneId(3);
		this.setRegionId(1);
		this.sceneState = SceneLoadState.NONE;

		this.coopRequests = new Int2ObjectOpenHashMap<>();
		this.combatInvokeHandler = new InvokeHandler(PacketCombatInvocationsNotify.class);
		this.abilityInvokeHandler = new InvokeHandler(PacketAbilityInvocationsNotify.class);
		this.clientAbilityInitFinishHandler = new InvokeHandler(PacketClientAbilityInitFinishNotify.class);

		this.birthday = new PlayerBirthday();
		this.rewardedLevels = new HashSet<>();
		this.moonCardGetTimes = new HashSet<>();

		this.shopLimit = new ArrayList<>();
	}

	// On player creation
	public Player(GameSession session) {
		this();
		this.account = session.getAccount();
		this.accountId = this.getAccount().getId();
		this.session = session;
		this.nickname = "Traveler";
		this.signature = "";
		this.teamManager = new TeamManager(this);
		this.birthday = new PlayerBirthday();
		this.setProperty(PlayerProperty.PROP_PLAYER_LEVEL, 1);
		this.setProperty(PlayerProperty.PROP_IS_SPRING_AUTO_USE, 1);
		this.setProperty(PlayerProperty.PROP_SPRING_AUTO_USE_PERCENT, 50);
		this.setProperty(PlayerProperty.PROP_IS_FLYABLE, 1);
		this.setProperty(PlayerProperty.PROP_IS_TRANSFERABLE, 1);
		this.setProperty(PlayerProperty.PROP_MAX_STAMINA, 24000);
		this.setProperty(PlayerProperty.PROP_CUR_PERSIST_STAMINA, 24000);
		this.setProperty(PlayerProperty.PROP_PLAYER_RESIN, 160);
		this.getFlyCloakList().add(140001);
		this.getNameCardList().add(210001);
		this.getPos().set(GameConstants.START_POSITION);
		this.getRotation().set(0, 307, 0);
	}

	public int getUid() {
		return id;
	}

	public void setUid(int id) {
		this.id = id;
	}

	public long getNextGameGuid() {
		long nextId = ++this.nextGuid;
		return ((long) this.getUid() << 32) + nextId;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
		this.account.setPlayerId(getUid());
	}

	public GameSession getSession() {
		return session;
	}

	public void setSession(GameSession session) {
		this.session = session;
	}

	public boolean isOnline() {
		return this.getSession() != null && this.getSession().isActive();
	}

	public GameServer getServer() {
		return this.getSession().getServer();
	}

	public synchronized World getWorld() {
		return this.world;
	}

	public synchronized void setWorld(World world) {
		this.world = world;
	}

	public synchronized Scene getScene() {
		return scene;
	}

	public synchronized void setScene(Scene scene) {
		this.scene = scene;
	}

	public int getGmLevel() {
		return 1;
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickName) {
		this.nickname = nickName;
		this.updateProfile();
	}

	public int getHeadImage() {
		return headImage;
	}

	public void setHeadImage(int picture) {
		this.headImage = picture;
		this.updateProfile();
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
		this.updateProfile();
	}

	public Position getPos() {
		return pos;
	}

	public Position getRotation() {
		return rotation;
	}

	public int getLevel() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_LEVEL);
	}

	public int getExp() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_EXP);
	}

	public int getWorldLevel() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_WORLD_LEVEL);
	}
	
	public void setWorldLevel(int level) {
		this.setProperty(PlayerProperty.PROP_PLAYER_WORLD_LEVEL, level);
		this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_WORLD_LEVEL));
	}

	public int getPrimogems() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_HCOIN);
	}

	public void setPrimogems(int primogem) {
		this.setProperty(PlayerProperty.PROP_PLAYER_HCOIN, primogem);
		this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_HCOIN));
	}

	public int getMora() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_SCOIN);
	}

	public void setMora(int mora) {
		this.setProperty(PlayerProperty.PROP_PLAYER_SCOIN, mora);
		this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_SCOIN));
	}
	
	public int getCrystals() {
		return this.getProperty(PlayerProperty.PROP_PLAYER_MCOIN);
	}

	public void setCrystals(int crystals) {
		this.setProperty(PlayerProperty.PROP_PLAYER_MCOIN, crystals);
		this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_MCOIN));
	}

	private int getExpRequired(int level) {
		PlayerLevelData levelData = GameData.getPlayerLevelDataMap().get(level);
		return levelData != null ? levelData.getExp() : 0;
	}

	private float getExpModifier() {
		return Grasscutter.getConfig().getGameServerOptions().getGameRates().ADVENTURE_EXP_RATE;
	}

	// Affected by exp rate
	public void earnExp(int exp) {
		addExpDirectly((int) (exp * getExpModifier()));
	}

	// Directly give player exp
	public void addExpDirectly(int gain) {
		boolean hasLeveledUp = false;
		int level = getLevel();
		int exp = getExp();
		int reqExp = getExpRequired(level);

		exp += gain;

		while (exp >= reqExp && reqExp > 0) {
			exp -= reqExp;
			level += 1;
			reqExp = getExpRequired(level);
			hasLeveledUp = true;
		}

		if (hasLeveledUp) {
			// Set level property
			this.setProperty(PlayerProperty.PROP_PLAYER_LEVEL, level);
			// Update social status
			this.updateProfile();
			// Update player with packet
			this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_LEVEL));
		}

		// Set exp
		this.setProperty(PlayerProperty.PROP_PLAYER_EXP, exp);

		// Update player with packet
		this.sendPacket(new PacketPlayerPropNotify(this, PlayerProperty.PROP_PLAYER_EXP));
	}

	private void updateProfile() {
		getProfile().syncWithCharacter(this);
	}

	public boolean isFirstLoginEnterScene() {
		return !this.hasSentAvatarDataNotify;
	}

	public TeamManager getTeamManager() {
		return this.teamManager;
	}

	public PlayerGachaInfo getGachaInfo() {
		return gachaInfo;
	}

	public PlayerProfile getProfile() {
		if (this.playerProfile == null) {
			this.playerProfile = new PlayerProfile(this);
		}
		return playerProfile;
	}

	public Map<Integer, Integer> getProperties() {
		return properties;
	}

	public void setProperty(PlayerProperty prop, int value) {
		getProperties().put(prop.getId(), value);
	}

	public int getProperty(PlayerProperty prop) {
		return getProperties().get(prop.getId());
	}

	public Set<Integer> getFlyCloakList() {
		return flyCloakList;
	}

	public Set<Integer> getCostumeList() {
		return costumeList;
	}

	public Set<Integer> getNameCardList() {
		return this.nameCardList;
	}

	public MpSettingType getMpSetting() {
		return MpSettingType.MP_SETTING_ENTER_AFTER_APPLY; // TEMP
	}

	public synchronized Int2ObjectMap<CoopRequest> getCoopRequests() {
		return coopRequests;
	}

	public InvokeHandler<CombatInvokeEntry> getCombatInvokeHandler() {
		return this.combatInvokeHandler;
	}

	public InvokeHandler<AbilityInvokeEntry> getAbilityInvokeHandler() {
		return this.abilityInvokeHandler;
	}

	public InvokeHandler<AbilityInvokeEntry> getClientAbilityInitFinishHandler() {
		return clientAbilityInitFinishHandler;
	}

	public AvatarStorage getAvatars() {
		return avatars;
	}

	public Inventory getInventory() {
		return inventory;
	}

	public FriendsList getFriendsList() {
		return this.friendsList;
	}

	public int getEnterSceneToken() {
		return enterSceneToken;
	}

	public void setEnterSceneToken(int enterSceneToken) {
		this.enterSceneToken = enterSceneToken;
	}

	public int getNameCardId() {
		return nameCardId;
	}

	public void setNameCardId(int nameCardId) {
		this.nameCardId = nameCardId;
		this.updateProfile();
	}

	public int getMainCharacterId() {
		return mainCharacterId;
	}

	public void setMainCharacterId(int mainCharacterId) {
		this.mainCharacterId = mainCharacterId;
	}

	public int getPeerId() {
		return peerId;
	}

	public void setPeerId(int peerId) {
		this.peerId = peerId;
	}

	public int getClientTime() {
		return session.getClientTime();
	}

	public long getLastPingTime() {
		return session.getLastPingTime();
	}

	public boolean isPaused() {
		return paused;
	}

	public void setPaused(boolean newPauseState) {
		boolean oldPauseState = this.paused;
		this.paused = newPauseState;

		if (newPauseState && !oldPauseState) {
			this.onPause();
		} else if (oldPauseState && !newPauseState) {
			this.onUnpause();
		}
	}

	public SceneLoadState getSceneLoadState() {
		return sceneState;
	}

	public void setSceneLoadState(SceneLoadState sceneState) {
		this.sceneState = sceneState;
	}

	public boolean isInMultiplayer() {
		return this.getWorld() != null && this.getWorld().isMultiplayer();
	}

	public int getSceneId() {
		return sceneId;
	}

	public void setSceneId(int sceneId) {
		this.sceneId = sceneId;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public void setShowAvatars(boolean showAvatars) {
		this.showAvatars = showAvatars;
	}

	public boolean isShowAvatars() {
		return showAvatars;
	}

	public void setShowAvatarList(List<Integer> showAvatarList) {
		this.showAvatarList = showAvatarList;
	}

	public List<Integer> getShowAvatarList() {
		return showAvatarList;
	}

	public boolean inMoonCard() {
		return moonCard;
	}

	public void setMoonCard(boolean moonCard) {
		this.moonCard = moonCard;
	}

	public void addMoonCardDays(int days) {
		this.moonCardDuration += days;
	}

	public int getMoonCardDuration() {
		return moonCardDuration;
	}

	public void setMoonCardDuration(int moonCardDuration) {
		this.moonCardDuration = moonCardDuration;
	}

	public Date getMoonCardStartTime() {
		return moonCardStartTime;
	}

	public void setMoonCardStartTime(Date moonCardStartTime) {
		this.moonCardStartTime = moonCardStartTime;
	}

	public Set<Date> getMoonCardGetTimes() {
		return moonCardGetTimes;
	}

	public void setMoonCardGetTimes(Set<Date> moonCardGetTimes) {
		this.moonCardGetTimes = moonCardGetTimes;
	}

	public int getMoonCardRemainDays() {
		Calendar remainCalendar = Calendar.getInstance();
		remainCalendar.setTime(moonCardStartTime);
		remainCalendar.add(Calendar.DATE, moonCardDuration);
		Date theLastDay = remainCalendar.getTime();
		Date now = DateHelper.onlyYearMonthDay(new Date());
		return (int) ((theLastDay.getTime() - now.getTime()) / (24 * 60 * 60 * 1000)); // By copilot 
	}

	public void rechargeMoonCard() {
		inventory.addItem(new GameItem(203, 300));
		if (!moonCard) {
			moonCard = true;
			Date now = new Date();
			moonCardStartTime = DateHelper.onlyYearMonthDay(now);
			moonCardDuration = 30;
		} else {
			moonCardDuration += 30;
		}
		if (!moonCardGetTimes.contains(moonCardStartTime)) {
			moonCardGetTimes.add(moonCardStartTime);
		}
	}

	public void getTodayMoonCard() {
		if (!moonCard) {
			return;
		}
		Date now = DateHelper.onlyYearMonthDay(new Date());
		if (moonCardGetTimes.contains(now)) {
			return;
		}
		Date stopTime = new Date();
		Calendar stopCalendar = Calendar.getInstance();
		stopCalendar.setTime(stopTime);
		stopCalendar.add(Calendar.DATE, moonCardDuration);
		stopTime = stopCalendar.getTime();
		if (now.after(stopTime)) {
			moonCard = false;
			return;
		}
		moonCardGetTimes.add(now);
		addMoonCardDays(1);
		GameItem item = new GameItem(201, 90);
		getInventory().addItem(item, ActionReason.BlessingRedeemReward);
		session.send(new PacketCardProductRewardNotify(getMoonCardRemainDays()));
	}

	public List<ShopLimit> getShopLimit() {
		return shopLimit;
	}

	public ShopLimit getGoodsLimit(int goodsId) {
		Optional<ShopLimit> shopLimit = this.shopLimit.stream().filter(x -> x.getShopGoodId() == goodsId).findFirst();
		if (shopLimit.isEmpty())
			return null;
		return shopLimit.get();
	}

	public void addShopLimit(int goodsId, int boughtCount, int nextRefreshTime) {
		ShopLimit target = getGoodsLimit(goodsId);
		if (target != null) {
			target.setHasBought(target.getHasBought() + boughtCount);
			target.setHasBoughtInPeriod(target.getHasBoughtInPeriod() + boughtCount);
			target.setNextRefreshTime(nextRefreshTime);
		} else {
			ShopLimit sl = new ShopLimit();
			sl.setShopGoodId(goodsId);
			sl.setHasBought(boughtCount);
			sl.setHasBoughtInPeriod(boughtCount);
			sl.setNextRefreshTime(nextRefreshTime);
			getShopLimit().add(sl);
		}
		this.save();
	}

	public boolean inGodmode() {
		return godmode;
	}

	public void setGodmode(boolean godmode) {
		this.godmode = godmode;
	}

	public boolean hasSentAvatarDataNotify() {
		return hasSentAvatarDataNotify;
	}

	public void setHasSentAvatarDataNotify(boolean hasSentAvatarDataNotify) {
		this.hasSentAvatarDataNotify = hasSentAvatarDataNotify;
	}

	public void addAvatar(Avatar avatar) {
		boolean result = getAvatars().addAvatar(avatar);

		if (result) {
			// Add starting weapon
			getAvatars().addStartingWeapon(avatar);

			// Done
			if (hasSentAvatarDataNotify()) {
				// Recalc stats
				avatar.recalcStats();
				// Packet
				sendPacket(new PacketAvatarAddNotify(avatar, false));
			}
		} else {
			// Failed adding avatar
		}
	}

	public void addFlycloak(int flycloakId) {
		this.getFlyCloakList().add(flycloakId);
		this.sendPacket(new PacketAvatarGainFlycloakNotify(flycloakId));
	}

	public void addCostume(int costumeId) {
		this.getCostumeList().add(costumeId);
		this.sendPacket(new PacketAvatarGainCostumeNotify(costumeId));
	}

	public void addNameCard(int nameCardId) {
		this.getNameCardList().add(nameCardId);
		this.sendPacket(new PacketUnlockNameCardNotify(nameCardId));
	}

	public void setNameCard(int nameCardId) {
		if (!this.getNameCardList().contains(nameCardId)) {
			return;
		}

		this.setNameCardId(nameCardId);

		this.sendPacket(new PacketSetNameCardRsp(nameCardId));
	}

	public void dropMessage(Object message) {
		this.sendPacket(new PacketPrivateChatNotify(GameConstants.SERVER_CONSOLE_UID, getUid(), message.toString()));
	}

	/**
	 * Sends a message to another player.
	 *
	 * @param sender  The sender of the message.
	 * @param message The message to send.
	 */
	public void sendMessage(Player sender, Object message) {
		this.sendPacket(new PacketPrivateChatNotify(sender.getUid(), this.getUid(), message.toString()));
	}

	// ---------------------MAIL------------------------

	public List<Mail> getAllMail() { return this.mail; }

	public void sendMail(Mail message) {
		// Call mail receive event.
		PlayerReceiveMailEvent event = new PlayerReceiveMailEvent(this, message); event.call();
		if(event.isCanceled()) return; message = event.getMessage();
		
		this.mail.add(message);
		this.save();
		Grasscutter.getLogger().debug("Mail sent to user [" + this.getUid()  + ":" + this.getNickname() + "]!");
		if(this.isOnline()) {
			this.sendPacket(new PacketMailChangeNotify(this, message));
		} // TODO: setup a way for the mail notification to show up when someone receives mail when they were offline
	}

	public boolean deleteMail(int mailId) {
		Mail message = getMail(mailId);

		if(message != null) {
			int index = getMailId(message);
			message.expireTime = (int) Instant.now().getEpochSecond(); // Just set the mail as expired for now. I don't want to implement a counter specifically for an account...
			this.replaceMailByIndex(index, message);
			return true;
		}

		return false;
	}

	public Mail getMail(int index) { return this.mail.get(index); }
	public int getMailId(Mail message) {
		return this.mail.indexOf(message);
	}

	public boolean replaceMailByIndex(int index, Mail message) {
		if(getMail(index) != null) {
			this.mail.set(index, message);
			this.save();
			return true;
		} else {
			return false;
		}
	}
	
	public void interactWith(int gadgetEntityId) {
		GameEntity entity = getScene().getEntityById(gadgetEntityId);

		if (entity == null) {
			return;
		}

		// Handle
		if (entity instanceof EntityItem) {
			// Pick item
			EntityItem drop = (EntityItem) entity;
			if (!drop.isShare()) // check drop owner to avoid someone picked up item in others' world
			{
				int dropOwner = (int)(drop.getGuid() >> 32);
				if (dropOwner != getUid())
					return;
			}
			entity.getScene().removeEntity(entity);
			GameItem item = new GameItem(drop.getItemData(), drop.getCount());
			// Add to inventory
			boolean success = getInventory().addItem(item, ActionReason.SubfieldDrop);
			if (success) {

				if (!drop.isShare()) // not shared drop
					this.sendPacket(new PacketGadgetInteractRsp(drop, InteractType.INTERACT_PICK_ITEM));
				else
					this.getScene().broadcastPacket(new PacketGadgetInteractRsp(drop, InteractType.INTERACT_PICK_ITEM));
			}
		} else {
			// Delete directly
			entity.getScene().removeEntity(entity);
		}

		return;
	}

	public void onPause() {

	}

	public void onUnpause() {

	}

	public void sendPacket(BasePacket packet) {
		if (this.hasSentAvatarDataNotify) {
			this.getSession().send(packet);
		}
	}

	public OnlinePlayerInfo getOnlinePlayerInfo() {
		OnlinePlayerInfo.Builder onlineInfo = OnlinePlayerInfo.newBuilder()
				.setUid(this.getUid())
				.setNickname(this.getNickname())
				.setPlayerLevel(this.getLevel())
				.setMpSettingType(this.getMpSetting())
				.setNameCardId(this.getNameCardId())
				.setSignature(this.getSignature())
				.setProfilePicture(ProfilePicture.newBuilder().setAvatarId(this.getHeadImage()));

		if (this.getWorld() != null) {
			onlineInfo.setCurPlayerNumInWorld(getWorld().getPlayerCount());
		} else {
			onlineInfo.setCurPlayerNumInWorld(1);
		}

		return onlineInfo.build();
	}

	public PlayerBirthday getBirthday() {
		return this.birthday;
	}

	public void setBirthday(int d, int m) {
		this.birthday = new PlayerBirthday(d, m);
		this.updateProfile();
	}

	public boolean hasBirthday() {
		return this.birthday.getDay() > 0;
	}

	public Set<Integer> getRewardedLevels() {
		return rewardedLevels;
	}

	public void setRewardedLevels(Set<Integer> rewardedLevels) {
		this.rewardedLevels = rewardedLevels;
	}

	public SocialDetail.Builder getSocialDetail() {
		List<SocialShowAvatarInfoOuterClass.SocialShowAvatarInfo> socialShowAvatarInfoList = new ArrayList<>();
		if (this.isOnline()) {
			if (this.getShowAvatarList() != null) {
				for (int avatarId : this.getShowAvatarList()) {
					socialShowAvatarInfoList.add(
							socialShowAvatarInfoList.size(),
							SocialShowAvatarInfoOuterClass.SocialShowAvatarInfo.newBuilder()
									.setAvatarId(avatarId)
									.setLevel(getAvatars().getAvatarById(avatarId).getLevel())
									.setCostumeId(getAvatars().getAvatarById(avatarId).getCostume())
									.build()
					);
				}
			}
		} else {
			List<Integer> showAvatarList = DatabaseHelper.getPlayerById(id).getShowAvatarList();
			AvatarStorage avatars = DatabaseHelper.getPlayerById(id).getAvatars();
			avatars.loadFromDatabase();
			if (showAvatarList != null) {
				for (int avatarId : showAvatarList) {
					socialShowAvatarInfoList.add(
							socialShowAvatarInfoList.size(),
							SocialShowAvatarInfoOuterClass.SocialShowAvatarInfo.newBuilder()
									.setAvatarId(avatarId)
									.setLevel(avatars.getAvatarById(avatarId).getLevel())
									.setCostumeId(avatars.getAvatarById(avatarId).getCostume())
									.build()
					);
				}
			}
		}

		SocialDetail.Builder social = SocialDetail.newBuilder()
				.setUid(this.getUid())
				.setProfilePicture(ProfilePicture.newBuilder().setAvatarId(this.getHeadImage()))
				.setNickname(this.getNickname())
				.setSignature(this.getSignature())
				.setLevel(this.getLevel())
				.setBirthday(this.getBirthday().getFilledProtoWhenNotEmpty())
				.setWorldLevel(this.getWorldLevel())
				.setNameCardId(this.getNameCardId())
				.setIsShowAvatar(this.isShowAvatars())
				.addAllShowAvatarInfoList(socialShowAvatarInfoList)
				.setFinishAchievementNum(0);
		return social;
	}

	public List<ShowAvatarInfoOuterClass.ShowAvatarInfo> getShowAvatarInfoList() {
		List<ShowAvatarInfoOuterClass.ShowAvatarInfo> showAvatarInfoList = new ArrayList<>();

		Player player;
		boolean shouldRecalc;
		if (this.isOnline()) {
			player = this;
			shouldRecalc = false;
		} else {
			player = DatabaseHelper.getPlayerById(id);
			player.getAvatars().loadFromDatabase();
			player.getInventory().loadFromDatabase();
			shouldRecalc = true;
		}

		List<Integer> showAvatarList = player.getShowAvatarList();
		AvatarStorage avatars = player.getAvatars();
		if (showAvatarList != null) {
			for (int avatarId : showAvatarList) {
				Avatar avatar = avatars.getAvatarById(avatarId);
				if (shouldRecalc) {
					avatar.recalcStats();
				}
				showAvatarInfoList.add(avatar.toShowAvatarInfoProto());
			}
		}
		return showAvatarInfoList;
	}
	
	public PlayerWorldLocationInfoOuterClass.PlayerWorldLocationInfo getWorldPlayerLocationInfo() {
		return PlayerWorldLocationInfoOuterClass.PlayerWorldLocationInfo.newBuilder()
				.setSceneId(this.getSceneId())
				.setPlayerLoc(this.getPlayerLocationInfo())
				.build();
	}

	public PlayerLocationInfo getPlayerLocationInfo() {
		return PlayerLocationInfo.newBuilder()
				.setUid(this.getUid())
				.setPos(this.getPos().toProto())
				.setRot(this.getRotation().toProto())
				.build();
	}

	public synchronized void onTick() {
		// Check ping
		if (this.getLastPingTime() > System.currentTimeMillis() + 60000) {
			this.getSession().close();
			return;
		}
		// Check co-op requests
		Iterator<CoopRequest> it = this.getCoopRequests().values().iterator();
		while (it.hasNext()) {
			CoopRequest req = it.next();
			if (req.isExpired()) {
				req.getRequester().sendPacket(new PacketPlayerApplyEnterMpResultNotify(this, false, PlayerApplyEnterMpResultNotifyOuterClass.PlayerApplyEnterMpResultNotify.Reason.SYSTEM_JUDGE));
				it.remove();
			}
		}
		// Ping
		if (this.getWorld() != null) {
			// RTT notify - very important to send this often
			this.sendPacket(new PacketWorldPlayerRTTNotify(this.getWorld()));

			// Update player locations if in multiplayer every 5 seconds
			long time = System.currentTimeMillis();
			if (this.getWorld().isMultiplayer() && this.getScene() != null && time > nextSendPlayerLocTime) {
				this.sendPacket(new PacketWorldPlayerLocationNotify(this.getWorld()));
				this.sendPacket(new PacketScenePlayerLocationNotify(this.getScene()));
				this.resetSendPlayerLocTime();
			}
		}
	}

	public void resetSendPlayerLocTime() {
		this.nextSendPlayerLocTime = System.currentTimeMillis() + 5000;
	}

	@PostLoad
	private void onLoad() {
		this.getTeamManager().setPlayer(this);
	}

	public void save() {
		DatabaseHelper.savePlayer(this);
	}

	public void onLogin() {
		// Make sure these exist
		if (this.getTeamManager() == null) {
			this.teamManager = new TeamManager(this);
		}
		if (this.getProfile().getUid() == 0) {
			this.getProfile().syncWithCharacter(this);
		}

		// Check if player object exists in server
		// TODO - optimize
		Player exists = this.getServer().getPlayerByUid(getUid());
		if (exists != null) {
			exists.getSession().close();
		}

		// Load from db
		this.getAvatars().loadFromDatabase();
		this.getInventory().loadFromDatabase();
		this.getAvatars().postLoad();

		this.getFriendsList().loadFromDatabase();

		// Create world
		World world = new World(this);
		world.addPlayer(this);

		// Add to gameserver
		if (getSession().isActive()) {
			getServer().registerPlayer(this);
			getProfile().setPlayer(this); // Set online
		}

		// Multiplayer setting
		this.setProperty(PlayerProperty.PROP_PLAYER_MP_SETTING_TYPE, this.getMpSetting().getNumber());
		this.setProperty(PlayerProperty.PROP_IS_MP_MODE_AVAILABLE, 1);

		// Packets
		session.send(new PacketPlayerDataNotify(this)); // Player data
		session.send(new PacketStoreWeightLimitNotify());
		session.send(new PacketPlayerStoreNotify(this));
		session.send(new PacketAvatarDataNotify(this));

		getTodayMoonCard(); // The timer works at 0:0, some users log in after that, use this method to check if they have received a reward today or not. If not, send the reward.

		session.send(new PacketPlayerEnterSceneNotify(this)); // Enter game world
		session.send(new PacketPlayerLevelRewardUpdateNotify(rewardedLevels));
		session.send(new PacketOpenStateUpdateNotify());

		// First notify packets sent
		this.setHasSentAvatarDataNotify(true);

		// Call join event.
		PlayerJoinEvent event = new PlayerJoinEvent(this); event.call();
		if(event.isCanceled()) // If event is not cancelled, continue.
			session.close();
	}

	public void onLogout() {
		// Leave world
		if (this.getWorld() != null) {
			this.getWorld().removePlayer(this);
		}

		// Status stuff
		this.getProfile().syncWithCharacter(this);
		this.getProfile().setPlayer(null); // Set offline

		this.getCoopRequests().clear();

		// Save to db
		this.save();
		this.getTeamManager().saveAvatars();
		this.getFriendsList().save();
		
		// Call quit event.
		PlayerQuitEvent event = new PlayerQuitEvent(this); event.call();
	}

	public enum SceneLoadState {
		NONE(0), LOADING(1), INIT(2), LOADED(3);

		private final int value;

		private SceneLoadState(int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}
}
