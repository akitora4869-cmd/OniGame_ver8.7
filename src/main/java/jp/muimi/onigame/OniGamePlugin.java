package jp.muimi.onigame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Light;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;
import java.time.Duration;
import java.io.File;

public final class OniGamePlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private GameState state=GameState.WAITING;
    // v0.40.24 第三勢力「異獣（イキモノ）」陣営バフ / Bot敵対AI
    private final Set<UUID> ikimonoIds=new HashSet<>();
    private BukkitTask ikimonoTicker;
    private final Map<UUID,UUID> ikimonoLastAttacker=new HashMap<>();
    // v0.40.48 異獣討伐貢献度（異獣UUID -> 参加者UUID -> point）
    private final Map<UUID,Map<UUID,Double>> ikimonoContribution=new HashMap<>();
    private final Map<UUID,Deque<String>> ikimonoBotBuffs=new HashMap<>();
    private final Map<String,Long> ikimonoFactionBuffUntil=new HashMap<>();
    private final Map<UUID,Location> ikimonoLightLocations=new HashMap<>();
    private Location azakujiLightLocation;
    private final Set<UUID> uneiPlayNextMatch=new HashSet<>();
    private final Set<UUID> participants=new HashSet<>(), players=new HashSet<>(), escaped=new HashSet<>(), dead=new HashSet<>(), playerBots=new HashSet<>(), escapedPlayerBots=new HashSet<>(), deadPlayerBots=new HashSet<>();
    private final Map<UUID,PlayerSkill> selectedSkill=new HashMap<>();
    private final Map<UUID,String> selectedPresetNames=new HashMap<>();
    private final Map<UUID,LinkedHashSet<PassiveSkill>> selectedPassives=new HashMap<>();
    private final LinkedHashSet<OniPassiveSkill> selectedOniPassives=new LinkedHashSet<>();
    private final LinkedHashSet<OniPassiveSkill> activeOniPassives=new LinkedHashSet<>();
    private final Map<String,Long> cooldowns=new HashMap<>();
    private final Map<String,Integer> heartHp=new HashMap<>();
    private final Map<String,UUID> oniHeartGlowMarkers=new HashMap<>();
    private long shikkiFrenzyUntil=0L;
    private final Set<UUID> shikkiChainVictims=new HashSet<>();
    private boolean shikkiFinisherReady=false;
    private final Map<UUID,Long> shikkiLeapAttackUntil=new HashMap<>();
    // v0.40.7 跳躍狩り: 空中追加入力による最大3連跳躍
    private final Map<UUID,Integer> shikkiLeapChainCount=new HashMap<>();
    private final Map<UUID,Long> shikkiLeapChainUntil=new HashMap<>();
    // v0.40.76 最大連鎖後は着地するまで跳躍狩りを再使用できない
    private final Set<UUID> shikkiLeapAwaitLanding=new HashSet<>();
    private final Set<UUID> yuukiVeiled=new HashSet<>();
    private final Map<UUID,Long> yuukiAmbushUntil=new HashMap<>();
    private final Map<UUID,Long> yuukiDivineHideUntil=new HashMap<>();
    private final Map<String,Material> fakeHeartOriginals=new HashMap<>();
    private final Set<String> fakeHeartKeys=new HashSet<>();

    private final Map<String,Long> heartAlertAt=new HashMap<>();
    private final Map<UUID,String> repairingHeart=new HashMap<>();
    private final Map<UUID,Long> repairProgressAt=new HashMap<>(), nextSkillCheckAt=new HashMap<>();
    // Queue lobby utility GUIs before protection plugins can swallow the interaction.
    private final Set<UUID> lobbyUtilityGuiQueued=new HashSet<>();
    private final Map<UUID,Long> playerHitGraceUntil=new HashMap<>(), downEscapeUntil=new HashMap<>();
    private final Set<UUID> extraLifeConsumed=new HashSet<>();
    private final Map<UUID,ItemStack[]> playerBotEscapeEquipment=new HashMap<>();
    private final Map<UUID,Double> heartProgressCarry=new HashMap<>();
    private final Map<UUID,Integer> doubleStakesGreatStreak=new HashMap<>();
    private final Map<UUID,SkillCheck> skillChecks=new HashMap<>();
    private final Map<UUID,String> playerBotWorkingHeart=new HashMap<>();
    private final Map<UUID,Long> nextPlayerBotSkillCheck=new HashMap<>();
    private final Map<UUID,Long> playerBotNextAttackAt=new HashMap<>();
    private final Map<UUID,PlayerSkill> playerBotSkills=new HashMap<>();
    private final Map<UUID,LinkedHashSet<PassiveSkill>> playerBotPassives=new HashMap<>();
    // v0.40.49: DUO相方鬼Botも独自パッシブ構成を持つ
    private final Map<UUID,LinkedHashSet<OniPassiveSkill>> duoOniBotPassives=new HashMap<>();
    private final Map<UUID,String> playerBotPresetNames=new HashMap<>();
    private final Map<UUID,Long> playerBotSkillReadyAt=new HashMap<>(), playerBotSprintUntil=new HashMap<>();
    // v0.40.52: 鬼ごっこモード専用ぷれいやーBot AI
    private final Map<UUID,Location> tagBotRoamTarget=new HashMap<>();
    private final Map<UUID,Long> tagBotNextRetargetAt=new HashMap<>();
    private final Map<UUID,Set<String>> tagBotVisitedChests=new HashMap<>();
    // v0.40.73: TAG objective AI state
    private final Map<UUID,Integer> tagBotGateChannelTicks=new HashMap<>();
    private final Map<UUID,Long> tagBotGemNextCheckAt=new HashMap<>();
    // v0.40.55: 鬼ごっこ Soul Point / ranking / block shops
    private final Map<UUID,Integer> soulPoints=new HashMap<>(), matchEarnedSoulPoints=new HashMap<>();
    private final Map<UUID,Double> tagSpHeartDamageCarry=new HashMap<>();
    private final Map<UUID,Boolean> spRankingSingleMode=new HashMap<>();
    // v0.40.57: TAG revival parkour / discovered-area navigation
    private final Set<UUID> tagParkourPlayers=new HashSet<>();
    private final Set<UUID> tagParkourGrandfathered=new HashSet<>();
    // v0.40.61: TAG stock lives. Normal=1, streamer=2; parkour clear restores the stock.
    private final Map<UUID,Integer> tagLives=new HashMap<>();
    // v0.40.63: Terra objective - four elemental gems charge each player's talisman independently.
    private final Map<UUID,LinkedHashSet<String>> tagTerraGems=new HashMap<>();
    // v0.40.64: personal Terra gem discovery bonus (50 block radius, once per gem per match).
    private final Map<UUID,LinkedHashSet<String>> tagTerraDiscoveredGems=new HashMap<>();
    // v0.40.71: Terra gems use a personal mini-heart attunement with skill checks.
    private final Map<UUID,String> tagTerraAttuningGem=new HashMap<>();
    private final Map<UUID,Map<String,Double>> tagTerraGemProgress=new HashMap<>();
    private final Map<UUID,Long> tagTerraNextSkillCheckAt=new HashMap<>();
    private final Map<UUID,SkillCheck> tagTerraSkillChecks=new HashMap<>();
    // v0.40.65: Terra escape gates open at 5:00 remaining.
    private boolean tagTerraGatesOpened=false;
    // v0.40.74: TAG broadcast events / Hyakki Yagyo
    private int tagInitialSeconds=0, tagNextRandomEventAt=-1;
    private boolean tagHyakkiTriggered=false;
    private long tagHyakkiUntil=0L, tagReimyakuUntil=0L, tagSoulRushUntil=0L;
    // v0.40.79: match-wide resonance generated by the whole TAG player side.
    private double tagResonanceGreat=0.0, tagResonanceFail=0.0;
    private long tagResonanceCooldownUntil=0L;
    private final Deque<Long> tagRecentGreats=new ArrayDeque<>(), tagRecentFails=new ArrayDeque<>();
    // v0.40.66: gate escape is a 5-second sneak channel inside the gate radius.
    // v0.40.67: TAG oni always see the timer; players see it once 5:00 remains.
    private final Map<UUID,Integer> tagTerraGateSneakTicks=new HashMap<>();
    private final Map<UUID,LinkedHashSet<String>> tagDiscoveredAreas=new HashMap<>();
    private final Map<UUID,String> tagSelectedArea=new HashMap<>();
    private final Map<UUID,Long> playerBotHitGraceUntil=new HashMap<>();
    private UUID azakujiAllyId;
    private long azakujiFightUntil, azakujiEscapeUntil, azakujiTauntUntil, azakujiNextLeapAt, azakujiNextMeleeAt, azakujiLastCombatAt, azakujiDivineUntil;
    private long azakujiSpawnedAt, azakujiLeapInvulnerableUntil, azakujiNextDivineThunderAt, azakujiNextBlinkAt, azakujiOniBotRetaliateUntil, azakujiNextPatrolRetargetAt;
    private Location azakujiPatrolTarget;
    private int azakujiDivineThunderUses;
    private boolean azakujiDivinePossessionUsed;
    private boolean azakujiForcedAttrition;

    private final Map<UUID,Double> playerBotStamina=new HashMap<>();
    private final Map<UUID,String> playerBotTargetHeart=new HashMap<>();
    private final Map<UUID,Location> playerBotLastLocation=new HashMap<>();
    private final Map<UUID,Integer> playerBotStuckTicks=new HashMap<>();
    private final Map<UUID,Location> safetyLastLocation=new HashMap<>(), safetyLastGoodLocation=new HashMap<>();
    private final Map<UUID,Integer> safetyStuckChecks=new HashMap<>();
    private final Map<UUID,Long> safetyRescueCooldownUntil=new HashMap<>(), safetyAbuseWindowStartedAt=new HashMap<>(), safetyAbuseLockUntil=new HashMap<>(), lastCombatAt=new HashMap<>();
    private final Map<UUID,Integer> safetyAbuseAttempts=new HashMap<>();
    private final Map<UUID,BotSkillCheck> playerBotSkillChecks=new HashMap<>();
    private final Map<UUID,HealingTask> healingTasks=new HashMap<>();
    private final Map<UUID,Long> lastSeenAt=new HashMap<>(), heartbeatAt=new HashMap<>();
    private final Map<UUID,Double> stamina=new HashMap<>();
    private final Map<UUID,Long> dashJumpPenaltyAt=new HashMap<>();
    private final Map<UUID,Long> passiveLeapReadyAt=new HashMap<>();
    private final Map<UUID,Long> sneakStartedAt=new HashMap<>(),parrySneakAt=new HashMap<>(),repairStartedAt=new HashMap<>(),reversalAt=new HashMap<>(),fakeNoiseUntil=new HashMap<>();
    private final Map<UUID,Vector> decoyProjectileDirections=new HashMap<>();
    private final Map<UUID,UUID> decoyOwners=new HashMap<>();
    private final Set<UUID> decoyRewarded=new HashSet<>();
    private final Map<String,Long> technicalPassiveReadyAt=new HashMap<>();
    private final Map<String,Long> timedSkillUntil=new HashMap<>();
    private final Map<UUID,Location> sealingCircles=new HashMap<>();
    private final Map<UUID,Long> sealingCircleUntil=new HashMap<>();
    private final Map<UUID,Vector> lastMoveDirection=new HashMap<>();
    private final Map<UUID,Boolean> wasSprinting=new HashMap<>();
    private final Set<UUID> aftermindPrimed=new HashSet<>();
    private final Set<UUID> trainingPlayers=new HashSet<>();
    private UUID trainingDummy;
    private final Set<UUID> chased=new HashSet<>();
    private final Set<UUID> predationTriggered=new HashSet<>();
    private final Set<UUID> heavenlyArrival=new HashSet<>();
    private final Set<UUID> bugeiActive=new HashSet<>(); private final Set<UUID> jakutsukiPierceCharging=new HashSet<>();
    private final Map<UUID,Location> blackMirrorLocations=new HashMap<>();
    private final Map<UUID,UUID> blackMirrorEntities=new HashMap<>();
    private final Set<UUID> jakutsukiSnakes=new HashSet<>();
    private long exitSealedUntil=0L;
    private final Map<String,Material> temporaryTestHeartOriginals=new HashMap<>();
    private final Map<String,Material> temporaryLootChestOriginals=new HashMap<>();
    private final Set<String> lootChestKeys=new HashSet<>();
    // v0.40.69: TAG-only skill chests, separate from normal item/equipment loot.
    private final Set<String> tagSkillChestKeys=new HashSet<>();
    private final Map<UUID,Set<String>> openedLootChests=new HashMap<>();
    private final Map<String,Inventory> personalLootInventories=new HashMap<>();
    private final Map<UUID,ChestOpeningTask> chestOpeningTasks=new HashMap<>();
    // v0.40.26 EXTRA AI: 鬼Botのチェイス採算判断
    private long oniBotTargetSince=0L; private double oniBotTargetStartDistance=-1, oniBotTargetBestDistance=Double.MAX_VALUE;
    private UUID oni, testParticipant, oniBot, forcedOni, configuredFriendlyNpcId, oniBotTacticalTarget; private final Set<UUID> oniTeam=new HashSet<>(); private final Map<UUID,String> rolePreferences=new HashMap<>(); private final Map<UUID,Long> disconnectGraceUntil=new HashMap<>(); private final Map<UUID,UUID> disconnectPlayerProxy=new HashMap<>(), disconnectProxyOwner=new HashMap<>(), disconnectOniProxy=new HashMap<>(), disconnectOniProxyOwner=new HashMap<>(); private final Map<UUID,OniType> disconnectOniProxyType=new HashMap<>(); private final Map<UUID,Long> disconnectOniProxySkillReadyAt=new HashMap<>(); private final Map<UUID,Integer> disconnectOniProxySkillCycle=new HashMap<>(); private OniType oniType=OniType.DAKKO, selectedOniType; private int brokenHearts, totalHearts, requiredHearts, secondsLeft; private BukkitTask ticker, sidebarTicker, chaseTicker, repairTicker, staminaTicker, botTicker, bgmTicker, gameStartBgmTicker, dawnTicker, crimsonTicker, finalEffectTestTicker, worldTimeTicker, tagReleaseTask; private UUID finalEffectTestPlayer; private boolean finalPhase, playerSideTest, duoBotMatchStart; private boolean pvFrozen=false; private long pvFixedTime=6000L; private boolean oniPowerActivated, finalFrenzy60Announced, finalFrenzy30Announced, supportDropTriggered; private final Set<UUID> duoBotSyntheticOwners=new HashSet<>(); private int escapePhase=1, escapeEventsCompleted=0, escapeEventsRequired=5; private String activeBgmSound, activeGameStartBgmSound;
    private final Set<UUID> dakkoCloneBots=new HashSet<>();
    private final Set<UUID> eliteBotMatchIds=new HashSet<>();
    private final Set<UUID> lobbyGuideDisplays=new HashSet<>();
    private final Map<UUID,Long> dakkoCloneExpiresAt=new HashMap<>(), dakkoCloneNextAttackAt=new HashMap<>(), dakkoCloneTargetSince=new HashMap<>();
    private final Map<UUID,UUID> dakkoCloneOwner=new HashMap<>(), dakkoCloneTarget=new HashMap<>();
    private final Map<UUID,Double> dakkoCloneTargetStartDistance=new HashMap<>();
    private final Map<UUID,BotDifficulty> dakkoCloneDifficulty=new HashMap<>();
    // v0.40.2 堕狐: 狐印と熟練操作
    private final Map<UUID,Long> dakkoFoxMarkUntil=new HashMap<>();
    private final Map<UUID,Integer> dakkoFoxChain=new HashMap<>(); private final Map<UUID,Long> dakkoFoxChainUntil=new HashMap<>();
    private final Set<UUID> dakkoFoxLamps=new LinkedHashSet<>(); private int dakkoFoxLampsLit=0; private long dakkoFoxFeastUntil=0L;
    private enum BotDifficulty{NORMAL,ELITE,EXTRA;static BotDifficulty parse(String s){try{return valueOf(s.toUpperCase(Locale.ROOT));}catch(Exception e){return null;}}}
    private BotDifficulty botDifficulty=BotDifficulty.ELITE;
    private final Set<UUID> chaseBgmPlayers=new HashSet<>();
    private final Map<UUID,BukkitTask> chaseReleaseTasks=new HashMap<>();
    private final Map<UUID,List<BukkitTask>> normalBgmFadeTasks=new HashMap<>();
    private Location activeLobby, activeExit, activeExit2, activePlayerSpawn; private long botNextAbilityAt, botNextMeleeAt, botRestUntil, oniChaseStartedAt, nextBloodScentAt; private double botStamina=20; private int botDakkoSkillCycle;
    private SavedWorldBorder savedTestWorldBorder;
    private NamespacedKey actionKey, equipmentUsesKey, equipmentIdKey;
    private final Map<String,EquipmentDef> equipmentRegistry=new LinkedHashMap<>();
    private record EquipmentDef(String id, Material material, int customModelData, String name, List<String> lore) {}
    private record SkillCheck(long startedAt,long targetAt,long endsAt){}
    private record BotSkillCheck(String heartKey,long resolveAt,boolean success,boolean great){}
    private record HealingTask(Location start,long startedAt,long endsAt,double amount,String consumableAction){}
    private record ChestOpeningTask(String chestKey,Location start,long startedAt,long endsAt){}
    private record TestArenaLayout(Location playerSpawn,Location oniSpawn,Location exit,Location exit2,List<Location> hearts,List<Location> lootChests){}
    private record SavedWorldBorder(World world,Location center,double size,int warningDistance,int warningTime,double damageAmount,double damageBuffer){}
    private static final class MenuHolder implements InventoryHolder{private final String type;private Inventory inventory;private MenuHolder(String type){this.type=type;}@Override public Inventory getInventory(){return inventory;}}

    // v0.36.1: token passive runtime state
    private final Map<UUID,Integer> threePhaseMask=new HashMap<>();
    private final Map<UUID,Integer> footstepTokens=new HashMap<>();
    private final Map<UUID,Integer> deadlineTokens=new HashMap<>();
    private final Map<UUID,Integer> bloodMarkTokens=new HashMap<>();
    private final Map<UUID,Long> footstepNextGainAt=new HashMap<>();
    private final Map<UUID,Long> deadlineNearSince=new HashMap<>();
    private final Map<UUID,Long> deadlineNextGainAt=new HashMap<>();
    private final Set<UUID> threePhaseReady=new HashSet<>();
    private final Set<UUID> huntRecordVictims=new HashSet<>();
    private final Set<UUID> grudgeResistReady=new HashSet<>();
    private final Map<UUID,Integer> grudgeTokens=new HashMap<>();
    private final Map<UUID,Long> oniChaseLostAt=new HashMap<>(), oniLastHitAt=new HashMap<>();
    private final Map<UUID,Location> oniScentTrailLocation=new HashMap<>();
    private final Map<UUID,Long> oniScentTrailUntil=new HashMap<>(), oniSkillSealUntil=new HashMap<>();
    private final Map<UUID,Integer> oniBloodFrenzyHits=new HashMap<>(), oniDisruptionStacks=new HashMap<>();
    private long oniLastAnyChaseAt=0L, oniHuntersInstinctAt=0L, oniHuntingGroundSince=0L;
    private Location oniHuntingGroundAnchor;
    // v0.38.4: client-render diagnostic breadcrumbs (server cannot read OpenGL state directly)
    private boolean debugTraceEnabled=false;
    private boolean errorNotifyEnabled=true;
    private String debugTraceCategory="all";
    // v0.40.44: OPを保持したままOniGame内だけ一般参加者として扱うテストモード
    private final Set<UUID> playerTestMode=new HashSet<>();
    // v0.39.8 Disconnect Proxy Phase 2C / Duo OniBot補充修正 / 鬼王・鬼神名称互換維持
    private final Set<UUID> kankiSummons=new HashSet<>();
    private final Map<UUID,BukkitTask> trainingLeaveTasks=new HashMap<>();
    // v0.40.4 Area Action Points: map movement gimmicks + safe autogen candidates
    private enum AreaPointType{UPDRAFT,JUMP_PAD,SPIRIT_ROAD,SAFE_DROP;static AreaPointType parse(String s){try{return valueOf(s.toUpperCase(Locale.ROOT).replace('-','_'));}catch(Exception e){return null;}}}
    private record AreaCandidate(int number,AreaPointType type,Location location,double power,double extra){}
    private final List<AreaCandidate> areaCandidates=new ArrayList<>();
    private final Map<UUID,Long> areaPointCooldownUntil=new HashMap<>(), areaFallProtectionUntil=new HashMap<>();
    private BukkitTask areaTicker;
    private int oniAwakeningStage=0;
    // v0.40.1 鬼域 Phase 2A/2B: 共通領域 + 疾鬼「狩場」 + 堕狐「狐境」
    private Location oniDomainCenter;
    private OniType oniDomainType;
    private UUID oniDomainOwner;
    private long oniDomainUntil=0L, oniDomainReadyAt=0L;
    private final Map<UUID,Integer> shikkiDomainAirJumps=new HashMap<>(), shikkiDomainWallKicks=new HashMap<>();
    private final Set<UUID> dakkoDomainSpirits=new HashSet<>();
    private final Map<UUID,OniType> kankiLastSummon=new HashMap<>();
    private final Map<UUID,OniType> kankiSelectedSummon=new HashMap<>();
    private final Map<UUID,BukkitTask> kankiSkillTasks=new HashMap<>();

    private void reportError(String context, Throwable error){
        String type=error==null?"Unknown":error.getClass().getSimpleName();
        String detail=error==null||error.getMessage()==null?"(no message)":error.getMessage();
        getLogger().log(java.util.logging.Level.SEVERE,"[OniGame ERROR]["+context+"] "+type+": "+detail,error);
        if(!errorNotifyEnabled)return;
        String chat=cc("&8[&4OniGame ERROR&8] &c"+context+" &7- &f"+type+"&7: "+detail+" &8(詳細はconsole)");
        for(Player viewer:Bukkit.getOnlinePlayers())if(viewer.isOp()&&!isPlayerTest(viewer))viewer.sendMessage(chat);
    }

    private void reportAdminError(String context, String detail, Throwable error, Location location){
        StringBuilder full=new StringBuilder(detail==null?"(no detail)":detail);
        if(location!=null&&location.getWorld()!=null){
            full.append(" @ ").append(location.getWorld().getName())
                .append(" ").append(location.getBlockX())
                .append(",").append(location.getBlockY())
                .append(",").append(location.getBlockZ());
        }
        Throwable cause=error!=null?error:new IllegalStateException(full.toString());
        reportError(context,cause);
    }

    private void debugTrace(String category,String code,String detail){
        if(!debugTraceEnabled)return;
        if(!"all".equals(debugTraceCategory)&&!debugTraceCategory.equalsIgnoreCase(category))return;
        String plain="[OniGame Debug]["+code+"]["+category+"] "+detail;
        getLogger().info(plain);
        String chat=cc("&8[&eOG Debug&8] &7[&f"+code+"&7/&b"+category+"&7] &f"+detail);
        for(Player viewer:Bukkit.getOnlinePlayers())if(isEffectiveAdmin(viewer))viewer.sendMessage(chat);
    }

    @Override public void onEnable(){saveDefaultConfig();getConfig().set("phase-time.lobby",1000L);getConfig().set("phase-time.game",18000L);getConfig().set("final-crimson.player-time",FINAL_SKY_TIME);getConfig().set("final-crimson.blood-sky.optifine-test-player-time",FINAL_SKY_TIME);getConfig().set("chase-bgm.sound","onigame:chase_tatari");getConfig().set("final-phase-bgm.sound","onigame:chase_tatari");getConfig().set("jakutsuki-final-phase-bgm.sound","onigame:chase_tatari");saveConfig();errorNotifyEnabled=getConfig().getBoolean("admin-error-notify.enabled",true);BotDifficulty configuredDifficulty=BotDifficulty.parse(getConfig().getString("bot.difficulty","ELITE"));if(configuredDifficulty!=null)botDifficulty=configuredDifficulty; String forcedOniRaw=getConfig().getString("forced-oni-uuid","");if(forcedOniRaw!=null&&!forcedOniRaw.isBlank())try{forcedOni=UUID.fromString(forcedOniRaw);}catch(IllegalArgumentException ignored){forcedOni=null;} actionKey=new NamespacedKey(this,"action"); equipmentUsesKey=new NamespacedKey(this,"equipment_uses"); equipmentIdKey=new NamespacedKey(this,"equipment_id"); loadEquipmentRegistry(); getServer().getPluginManager().registerEvents(this,this); PluginCommand c=getCommand("onigame"); c.setExecutor(this); c.setTabCompleter(this); restoreLobbyUtilities();setTagShopVisibility(isTagMode());setTerraGemVisibility(isTagMode());refreshLobbyGuideDisplays();Bukkit.getScheduler().runTaskTimer(this,this::updateTagNavigation,5,5);sidebarTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateSidebars,1,20); chaseTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateHorrorState,5,5);repairTicker=Bukkit.getScheduler().runTaskTimer(this,this::updatePlayerActions,2,2);staminaTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateStamina,5,5);botTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateBots,10,10);areaTicker=Bukkit.getScheduler().runTaskTimer(this,this::updateAreaActionPoints,5,5);applyConfiguredLobbyMorning();worldTimeTicker=Bukkit.getScheduler().runTaskTimer(this,this::enforcePhaseTime,20,20);getLogger().info("鬼げぇむ v0.40.80-PvShootingMode enabled");}
    @Override public void onDisable(){clearOniHeartGlowMarkers();removeLobbyGuideDisplays();clearFinalEffectTest();if(ticker!=null)ticker.cancel();if(dawnTicker!=null)dawnTicker.cancel();if(crimsonTicker!=null)crimsonTicker.cancel();resetFinalCrimsonVisuals();if(sidebarTicker!=null)sidebarTicker.cancel();if(chaseTicker!=null)chaseTicker.cancel();if(repairTicker!=null)repairTicker.cancel();if(staminaTicker!=null)staminaTicker.cancel();if(botTicker!=null)botTicker.cancel();if(areaTicker!=null)areaTicker.cancel();if(worldTimeTicker!=null)worldTimeTicker.cancel();for(UUID id:new HashSet<>(fakeNoiseUntil.keySet())){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}fakeNoiseUntil.clear();for(UUID id:new HashSet<>(trainingPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)removeTrainingItems(p);}trainingPlayers.clear();for(BukkitTask task:trainingLeaveTasks.values())task.cancel();trainingLeaveTasks.clear();removeTrainingDummy();removeBlackMirrors();removeJakutsukiSnakes();removeDakkoCloneBots();clearOniDomain();removeKankiSummons();removeAllDisconnectedOniProxies();removeOniBot();restoreFakeHearts();restoreTemporaryTestHearts();restoreLootChests();restoreTestWorldBorder();restoreLobbyUtilities();}

    private String cc(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    private void msg(CommandSender s,String m){s.sendMessage(cc("&8[&c鬼げぇむ&8] &f"+m));}
    private void all(String m){Bukkit.broadcastMessage(cc("&8[&c鬼げぇむ&8] &f"+m));}
    private boolean isPlayerTest(Player p){return p!=null&&playerTestMode.contains(p.getUniqueId());}
    private boolean isEffectiveAdmin(Player p){return p!=null&&!isPlayerTest(p)&&p.hasPermission("onigame.admin");}
    private boolean isUnei(Player p){return p!=null&&p.getScoreboardTags().contains("unei");}
    private boolean isEffectiveMapMaker(Player p){return p!=null&&!isPlayerTest(p)&&(p.getScoreboardTags().contains("map_maker")||isUnei(p));}
    private boolean uneiCanPlay(Player p){return p!=null&&(!isUnei(p)||uneiPlayNextMatch.contains(p.getUniqueId()));}
    private void pruneUneiParticipants(){participants.removeIf(id->{Player p=Bukkit.getPlayer(id);return p!=null&&isUnei(p)&&!uneiPlayNextMatch.contains(id);});}
    private void consumeUneiPlayOverrides(){uneiPlayNextMatch.clear();}
    private boolean isAzakujiLocked(){return getConfig().getBoolean("content-locks.azakuji-hiro",false);}
    private boolean isJakutsukiLocked(){return getConfig().getBoolean("content-locks.jakutsuki",false);}
    private String oniLockKey(OniType type){return switch(type){case DAKKO->"dakko";case KISHIN->"kiou";case SHIKKI->"shikki";case YUUKI->"yuuki";case KANKI->"onigami";case JAKUTSUKI->"jakutsuki";};}
    private boolean isOniTypeLocked(OniType type){
        // v0.40.40: 鬼神（KANKI / オニガミ）は将来DLC用にデータを保存したまま本編から休止。
        if(type==OniType.KANKI)return true;
        return type!=null&&getConfig().getBoolean("content-locks."+oniLockKey(type),false);
    }
    private OniType firstUnlockedOni(){for(OniType t:new OniType[]{OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI,OniType.JAKUTSUKI})if(!isOniTypeLocked(t))return t;return OniType.DAKKO;}
    private List<OniType> unlockedStandardOni(){List<OniType> list=new ArrayList<>();for(OniType t:new OniType[]{OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI})if(!isOniTypeLocked(t))list.add(t);return list;}
    private int oniSkillUnlockHearts(String action){
        return switch(action){
            case "dakko_clone"->getConfig().getInt("dakko-skills.clone.unlock-after-broken-hearts",1);
            case "dakko_fox_fire"->getConfig().getInt("dakko-skills.fox-fire.unlock-after-broken-hearts",2);
            case "dakko_heavenly_arrival"->getConfig().getInt("dakko-skills.heavenly-arrival.unlock-after-broken-hearts",3);
            case "kishin_slam"->getConfig().getInt("kishin-skills.slam.unlock-after-broken-hearts",2);
            case "kishin_roar"->getConfig().getInt("kishin-skills.roar.unlock-after-broken-hearts",4);
            case "kishin_iron_body"->getConfig().getInt("kishin-skills.iron-body.unlock-after-broken-hearts",6);
            case "shikki_hunting_leap"->0;
            case "shikki_blood_run"->getConfig().getInt("shikki-skills.blood-run.unlock-after-broken-hearts",2);
            case "shikki_frenzy"->getConfig().getInt("shikki-skills.frenzy.unlock-after-broken-hearts",4);
            case "shikki_chain_hunt"->getConfig().getInt("shikki-skills.chain-hunt.unlock-after-broken-hearts",6);
            case "yuuki_haze_step"->getConfig().getInt("yuuki-skills.haze-step.unlock-after-broken-hearts",2);
            case "yuuki_shadow_bind"->getConfig().getInt("yuuki-skills.shadow-bind.unlock-after-broken-hearts",4);
            case "yuuki_divine_hide"->getConfig().getInt("yuuki-skills.divine-hide.unlock-after-broken-hearts",6);
            default->0;
        };
    }
    private enum OniProgressionStyle { UNLOCK, CORE_EXPANSION, STAGED_ENHANCEMENT }
    private OniProgressionStyle oniProgressionStyle(OniType type){
        if(type==OniType.SHIKKI)return OniProgressionStyle.CORE_EXPANSION;
        return OniProgressionStyle.UNLOCK;
    }
    private boolean oniSkillUnlocked(String action){return brokenHearts>=oniSkillUnlockHearts(action);}
    private int oniEnhancementStage(OniType type){
        if(type==OniType.SHIKKI){
            if(brokenHearts>=6)return 3;
            if(brokenHearts>=4)return 2;
            if(brokenHearts>=2)return 1;
        }
        return 0;
    }
    private void refreshOniUnlockedSkills(Player p,boolean announce){
        if(p==null)return;
        if(oniType==OniType.DAKKO){
            if(oniSkillUnlocked("dakko_clone")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("dakko_clone","&6分霊","dakko_clone"));if(announce)msg(p,"&6分霊 &fが解放された！");}
            if(oniSkillUnlocked("dakko_fox_fire")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("dakko_fox_fire","&6&l狐火","dakko_fox_fire"));if(announce)msg(p,"&6狐火 &fが解放された！");}
            if(oniSkillUnlocked("dakko_heavenly_arrival")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("dakko_heavenly_arrival","&d&l天来","dakko_heavenly_arrival"));if(announce)msg(p,"&d天来 &fが解放された！");}
        }else if(oniType==OniType.KISHIN){
            if(oniSkillUnlocked("kishin_slam")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("kishin_slam","&4地砕","kishin_slam"));if(announce)msg(p,"&4地砕 &fが解放された！");}
            if(oniSkillUnlocked("kishin_roar")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("kishin_roar","&4&l鬼吼","kishin_roar"));if(announce)msg(p,"&4鬼吼 &fが解放された！");}
            if(oniSkillUnlocked("kishin_iron_body")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("kishin_iron_body","&6&l剛身","kishin_iron_body"));if(announce)msg(p,"&6剛身 &fが解放された！");}
        }else if(oniType==OniType.SHIKKI){
            if(oniSkillUnlocked("shikki_blood_run")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("shikki_blood_run","&c&l血走","shikki_blood_run"));if(announce)msg(p,"&c血走 &fが解放された！");}
            if(oniSkillUnlocked("shikki_frenzy")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("shikki_frenzy","&c&l狂奔","shikki_frenzy"));if(announce)msg(p,"&c狂奔 &fが解放された！ 跳躍狩りが強化される！");}
            if(oniSkillUnlocked("shikki_chain_hunt")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("shikki_chain_hunt","&4&l狩猟連鎖","shikki_chain_hunt"));if(announce)msg(p,"&4狩猟連鎖 &fが解放された！");}
        }else if(oniType==OniType.YUUKI){
            if(oniSkillUnlocked("yuuki_haze_step")&&p.getInventory().getItem(2)==null){p.getInventory().setItem(2,oniSkillIconItem("yuuki_haze_step","&7&l朧渡り","yuuki_haze_step"));if(announce)msg(p,"&7朧渡り &fが解放された！");}
            if(oniSkillUnlocked("yuuki_shadow_bind")&&p.getInventory().getItem(3)==null){p.getInventory().setItem(3,oniSkillIconItem("yuuki_shadow_bind","&5&l影縫い","yuuki_shadow_bind"));if(announce)msg(p,"&5影縫い &fが解放された！");}
            if(oniSkillUnlocked("yuuki_divine_hide")&&p.getInventory().getItem(4)==null){p.getInventory().setItem(4,oniSkillIconItem("yuuki_divine_hide","&8&l神隠し","yuuki_divine_hide"));if(announce)msg(p,"&8神隠し &fが解放された！");}
        }
    }
    private boolean ensureOniUnlocked(CommandSender sender,OniType type){if(!isOniTypeLocked(type))return true;msg(sender,"&c"+type.display+" &fは現在ロックされています。");return false;}
    private void resetLockedAzakujiSelections(){
        for(UUID id:new HashSet<>(selectedPresetNames.keySet()))if("AZAKUJI_HIRO".equals(selectedPresetNames.get(id))){selectedPresetNames.remove(id);selectedSkill.put(id,PlayerSkill.SPRINT);LinkedHashSet<PassiveSkill> ps=selectedPassives.computeIfAbsent(id,k->new LinkedHashSet<>());ps.clear();ps.add(PassiveSkill.LIGHT_FOOTED);ps.add(PassiveSkill.DEEP_BREATH);Player p=Bukkit.getPlayer(id);if(p!=null)msg(p,"&7字九字ひろがロックされたため、構成を通常プリセット相当へ戻しました。");}
    }

    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] a){
        if(a.length==0||a[0].equalsIgnoreCase("help")){help(sender);return true;}
        if(a[0].equalsIgnoreCase("join")&&sender instanceof Player p){if(isUnei(p)&&!uneiPlayNextMatch.contains(p.getUniqueId())){msg(p,"&e運営タグが付与されているため通常参加しません。 &7参加する場合は /og unei play を使用してください。");return true;}if(state!=GameState.WAITING){msg(p,"試合中は参加できません。");return true;} participants.add(p.getUniqueId()); selectedSkill.putIfAbsent(p.getUniqueId(),PlayerSkill.SPRINT);msg(p,"参加しました。現在 &e"+participants.size()+"人 &7――ロビーのスキル設定チェストから構成を選べます。");return true;}
        if(a[0].equalsIgnoreCase("leave")&&sender instanceof Player p){participants.remove(p.getUniqueId());selectedSkill.remove(p.getUniqueId());selectedPresetNames.remove(p.getUniqueId());msg(p,"参加を取り消しました。");return true;}
        if(a[0].equalsIgnoreCase("skill")&&sender instanceof Player p){PlayerSkill sk=a.length>1?PlayerSkill.parse(a[1]):null;if(sk==null){msg(p,"sprint / invisible / smoke / strike / heal / obsession / landing から選択してください。");return true;}selectedSkill.put(p.getUniqueId(),sk);selectedPresetNames.remove(p.getUniqueId());msg(p,"スキルを &b"+sk.display+" &fに設定しました。");return true;}
        if(a[0].equalsIgnoreCase("skills")&&sender instanceof Player p){if(state!=GameState.WAITING){msg(p,"スキル選択はロビーで行ってください。");return true;}openSkillMenu(p);return true;}
        if(a[0].equalsIgnoreCase("training")&&sender instanceof Player p&&a.length>1&&a[1].equalsIgnoreCase("refresh")){if(!isInTrainingArea(p.getLocation())){msg(p,"練習エリア内で使用してください。");return true;}giveTrainingItems(p);msg(p,"練習用スキルとパッシブを更新しました。鬼スキルは心臓破壊数に関係なく全解放です。");return true;}
        if(a[0].equalsIgnoreCase("playertest")&&sender instanceof Player p){
            String sub=a.length>1?a[1].toLowerCase(Locale.ROOT):"status";
            if(sub.equals("off")){
                if(!isPlayerTest(p)){msg(p,"&7一般プレイヤーテストモードは現在OFFです。");return true;}
                playerTestMode.remove(p.getUniqueId());
                msg(p,"&a[TEST] 一般プレイヤーテストモードを解除しました。 &7OniGame内の管理者権限を通常状態へ戻しました。");
                return true;
            }
            if(sub.equals("status")){msg(p,"一般プレイヤーテストモード: "+(isPlayerTest(p)?"&aON":"&cOFF")+" &7※実際のOP権限は変更しません。");return true;}
            if(sub.equals("on")){
                if(!p.hasPermission("onigame.admin")){msg(p,"&cこのテストモードを有効化する権限がありません。");return true;}
                playerTestMode.add(p.getUniqueId());
                msg(p,"&e[TEST] 一般プレイヤーテストモードを有効にしました。 &7OniGame内では一般参加者として扱われます。解除: /og playertest off");
                return true;
            }
            msg(p,"/og playertest <on|off|status>");return true;
        }
        if(a[0].equalsIgnoreCase("unei")&&a.length>=2&&a[1].equalsIgnoreCase("play")&&a.length==2&&sender instanceof Player p&&isUnei(p)){if(state!=GameState.WAITING){msg(p,"&c試合中は次戦参加を変更できません。");return true;}uneiPlayNextMatch.add(p.getUniqueId());participants.add(p.getUniqueId());selectedSkill.putIfAbsent(p.getUniqueId(),PlayerSkill.SPRINT);msg(p,"&a次の1試合だけ参加対象になりました。 &7unei/map_maker権限は維持され、試合後は自動で運営待機へ戻ります。");return true;}
        if(sender instanceof Player p&&isPlayerTest(p)){msg(sender,"権限がありません。");return true;}
        if(!sender.hasPermission("onigame.admin")){msg(sender,"権限がありません。");return true;}
        if(a[0].equalsIgnoreCase("unei")){
            if(a.length<2){msg(sender,"/og unei <add|remove|list|status|play> [player]");return true;}
            String sub=a[1].toLowerCase(Locale.ROOT);
            if(sub.equals("list")){List<String> names=new ArrayList<>();for(Player q:Bukkit.getOnlinePlayers())if(isUnei(q))names.add(q.getName()+(uneiPlayNextMatch.contains(q.getUniqueId())?"(次戦参加)":""));msg(sender,"&6運営: &f"+(names.isEmpty()?"なし":String.join(", ",names)));return true;}
            Player target=null;if(a.length>=3&&!a[2].equalsIgnoreCase("me"))target=Bukkit.getPlayerExact(a[2]);else if(sender instanceof Player self)target=self;
            if(target==null){msg(sender,"&c対象プレイヤーを指定してください。");return true;}UUID tid=target.getUniqueId();
            if(sub.equals("status")){msg(sender,"&6"+target.getName()+" &7| unei: "+(isUnei(target)?"&aON":"&cOFF")+" &7| map_maker: "+(isEffectiveMapMaker(target)?"&aON":"&cOFF")+" &7| 次戦参加: "+(uneiPlayNextMatch.contains(tid)?"&aON":"&cOFF"));return true;}
            if(sub.equals("add")){if(!target.getScoreboardTags().contains("map_maker")){target.addScoreboardTag("map_maker");target.addScoreboardTag("unei_granted_map_maker");}target.addScoreboardTag("unei");uneiPlayNextMatch.remove(tid);participants.remove(tid);msg(sender,"&a"+target.getName()+" に運営タグ unei を付与しました。 &7map_makerを内包し、通常はゲーム参加対象外です。");return true;}
            if(sub.equals("remove")){target.removeScoreboardTag("unei");uneiPlayNextMatch.remove(tid);if(target.getScoreboardTags().contains("unei_granted_map_maker")){target.removeScoreboardTag("unei_granted_map_maker");target.removeScoreboardTag("map_maker");}msg(sender,"&e"+target.getName()+" の運営タグ unei を解除しました。");return true;}
            if(sub.equals("play")){if(!isUnei(target)){msg(sender,"&c"+target.getName()+" にはuneiタグがありません。");return true;}if(state!=GameState.WAITING){msg(sender,"&c試合中は次戦参加を変更できません。");return true;}uneiPlayNextMatch.add(tid);participants.add(tid);selectedSkill.putIfAbsent(tid,PlayerSkill.SPRINT);msg(sender,"&a"+target.getName()+" を次の1試合だけ参加対象にしました。 &7unei/map_maker権限は維持されます。");return true;}
            msg(sender,"/og unei <add|remove|list|status|play> [player]");return true;
        }
        if(a[0].equalsIgnoreCase("sp")){
            if(a.length<2){msg(sender,"/og sp <add|remove|set|reset|check> [player] [amount]");return true;}
            String sub=a[1].toLowerCase(Locale.ROOT); Player target=null; int amount=0;
            if(a.length>=3){target=Bukkit.getPlayerExact(a[2]);if(target==null&&sender instanceof Player self&&isInteger(a[2])){target=self;amount=Integer.parseInt(a[2]);}}
            else if(sender instanceof Player self)target=self;
            if(target==null){msg(sender,"&c対象プレイヤーを指定してください。");return true;}
            if(a.length>=4&&isInteger(a[3]))amount=Integer.parseInt(a[3]);
            UUID tid=target.getUniqueId(); int cur=soulPoints.getOrDefault(tid,0);
            if(sub.equals("check")){msg(sender,"&b"+target.getName()+" &f所持SP: &d"+cur+" &7| 今試合獲得: &f"+matchEarnedSoulPoints.getOrDefault(tid,0));return true;}
            if(sub.equals("reset"))soulPoints.put(tid,0);
            else if(sub.equals("add"))soulPoints.put(tid,Math.max(0,cur+Math.max(0,amount)));
            else if(sub.equals("remove"))soulPoints.put(tid,Math.max(0,cur-Math.max(0,amount)));
            else if(sub.equals("set"))soulPoints.put(tid,Math.max(0,amount));
            else{msg(sender,"/og sp <add|remove|set|reset|check> [player] [amount]");return true;}
            msg(sender,"&e[TEST] &f"+target.getName()+" のSPを &d"+soulPoints.getOrDefault(tid,0)+" &fにしました。 &7（ランキング記録対象外）");return true;
        }
        if(a[0].equalsIgnoreCase("tag")&&a.length>1){
            String sub=a[1].toLowerCase(Locale.ROOT);
            if(sub.equals("start")||sub.equals("forcestart")){if(!isTagMode()){getConfig().set("game-mode","tag");saveConfig();}startTag(sender,sub.equals("forcestart"));return true;}
            if(sub.equals("stop")){if(state==GameState.WAITING){msg(sender,"&7鬼ごっこは開始されていません。");return true;}end(false,"管理者が鬼ごっこを終了しました");return true;}
            if(sub.equals("setup")&&sender instanceof Player p){Location c=p.getLocation().getBlock().getLocation().add(.5,0,.5);LocationStore.set(getConfig(),"tag-mode.center",c);LocationStore.set(getConfig(),"tag-mode.shrine.player",p.getLocation());getConfig().set("tag-mode.border-size",200.0);saveConfig();WorldBorder wb=p.getWorld().getWorldBorder();wb.setCenter(c.getX(),c.getZ());wb.setSize(200.0);msg(p,"&a鬼ごっこマップを一括設定しました。 &7中心/ぷれいやー神社/200x200ボーダー");return true;}
            if(sub.equals("parkour")&&sender instanceof Player p){if(a.length<3){msg(p,"/og tag parkour <start|goal>");return true;}String k=a[2].toLowerCase(Locale.ROOT);if(!k.equals("start")&&!k.equals("goal")){msg(p,"/og tag parkour <start|goal>");return true;}LocationStore.set(getConfig(),"tag-mode.parkour."+k,p.getLocation());saveConfig();msg(p,"&a復活アスレチック "+k+" を設定しました。");return true;}
            if(sub.equals("respawn")&&sender instanceof Player p){LocationStore.set(getConfig(),"tag-mode.respawn",p.getLocation());saveConfig();msg(p,"&a鬼ごっこの復帰地点を設定しました。");return true;}
            if(sub.equals("area")&&sender instanceof Player p){if(a.length<3){msg(p,"/og tag area <add|remove|list> [ID]");return true;}String op=a[2].toLowerCase(Locale.ROOT);if(op.equals("list")){var sec=getConfig().getConfigurationSection("tag-mode.areas");msg(p,"&bTAGエリア: &f"+(sec==null?"なし":String.join(", ",sec.getKeys(false))));return true;}if(a.length<4){msg(p,"/og tag area <add|remove> <ID>");return true;}String id=a[3].replaceAll("[^A-Za-z0-9_ぁ-んァ-ヶ一-龠-]","_");if(op.equals("add")){LocationStore.set(getConfig(),"tag-mode.areas."+id+".location",p.getLocation());getConfig().set("tag-mode.areas."+id+".radius",getConfig().getDouble("tag-mode.area-default-radius",8.0));saveConfig();msg(p,"&aエリア登録: &f"+id);return true;}if(op.equals("remove")){getConfig().set("tag-mode.areas."+id,null);saveConfig();msg(p,"&eエリア削除: &f"+id);return true;}msg(p,"/og tag area <add|remove|list> [ID]");return true;}
            if(sub.equals("terra")&&sender instanceof Player p){
                if(a.length<3){msg(p,"/og tag terra <gem|gate|status>");return true;}
                String op=a[2].toLowerCase(Locale.ROOT);
                if(op.equals("status")){msg(p,"&bTerra &7| 宝石: &f"+terraGemConfigSummary()+" &7| ゲート &f"+terraGateCount()+"/3 &7| 必要霊力 &f4 &7| 解放 &f残り5:00");return true;}
                if(op.equals("gate")){
                    if(a.length<4){msg(p,"/og tag terra gate <add|clear|list>");return true;}
                    String gop=a[3].toLowerCase(Locale.ROOT);
                    if(gop.equals("add")){int n=terraGateCount();if(n>=3){msg(p,"&cTerraのゲートは3つまでです。先に clear してください。");return true;}LocationStore.set(getConfig(),"tag-mode.maps.terra.gates."+(n+1),p.getLocation().getBlock().getLocation());saveConfig();msg(p,"&aTerra脱出ゲート #"+(n+1)+" を現在位置に登録しました。");return true;}
                    if(gop.equals("clear")){getConfig().set("tag-mode.maps.terra.gates",null);saveConfig();msg(p,"&7Terraの脱出ゲートを全削除しました。");return true;}
                    if(gop.equals("list")){msg(p,"&bTerra脱出ゲート: &f"+terraGateCount()+"/3");for(int gi=1;gi<=3;gi++){Location gl=terraGate(gi);if(gl!=null)msg(p,"&7#"+gi+" &f"+gl.getWorld().getName()+" "+gl.getBlockX()+","+gl.getBlockY()+","+gl.getBlockZ());}return true;}
                    msg(p,"/og tag terra gate <add|clear|list>");return true;
                }
                if(op.equals("gem")){
                    if(a.length<4){msg(p,"/og tag terra gem <diamond|gold|emerald|lapis>");return true;}
                    String gem=normalizeTerraGem(a[3]);if(gem==null){msg(p,"&c宝石は diamond / gold / emerald / lapis のいずれかです。");return true;}
                    Block b=p.getTargetBlockExact(8);if(b==null){msg(p,"&c登録する宝石ブロックを見てください。");return true;}
                    Material expected=terraGemMaterial(gem);if(b.getType()!=expected){msg(p,"&c"+terraGemDisplay(gem)+" は "+expected+" を見て登録してください。");return true;}
                    LocationStore.set(getConfig(),"tag-mode.maps.terra.gems."+gem,b.getLocation());getConfig().set("tag-mode.map","terra");saveConfig();setTerraGemVisibility(isTagMode());msg(p,"&aTerraの"+terraGemDisplay(gem)+"を登録しました。");return true;
                }
                msg(p,"/og tag terra gem <diamond|gold|emerald|lapis>  または /og tag terra status");return true;
            }
            if(sub.equals("oni")){if(a.length<3){msg(sender,"/og tag oni <player|me|clear|status> [oni]");return true;}String who=a[2];if(who.equalsIgnoreCase("clear")){forcedOni=null;getConfig().set("forced-oni-uuid",null);saveConfig();msg(sender,"&e鬼ごっこの人間鬼指定を解除しました。");return true;}if(who.equalsIgnoreCase("status")){Player q=forcedOni==null?null:Bukkit.getPlayer(forcedOni);msg(sender,"&dTAG指定鬼: &f"+(q==null?(forcedOni==null?"なし":forcedOni.toString()):q.getName())+" &7| 鬼種=&f"+oniType.display);return true;}Player q=who.equalsIgnoreCase("me")&&sender instanceof Player self?self:Bukkit.getPlayerExact(who);if(q==null){msg(sender,"&cプレイヤーが見つかりません。");return true;}if(a.length>=4){OniType t=OniType.parse(a[3]);if(t==null||isOniTypeLocked(t)){msg(sender,"&c使用できない鬼種です。");return true;}selectedOniType=t;}forcedOni=q.getUniqueId();getConfig().set("forced-oni-uuid",forcedOni.toString());saveConfig();msg(sender,"&a次の鬼ごっこの鬼を &f"+q.getName()+" &aに指定しました。 &7鬼種: &f"+(selectedOniType==null?oniType:selectedOniType).display);return true;}
            if(sub.equals("streamer")){if(a.length<3){msg(sender,"/og tag streamer <player|me|list> [on|off]");return true;}String who=a[2];List<String> raw=new ArrayList<>(getConfig().getStringList("tag-mode.streamer-players"));if(who.equalsIgnoreCase("list")){List<String> names=new ArrayList<>();for(String x:raw){try{UUID u=UUID.fromString(x);Player q=Bukkit.getPlayer(u);names.add(q!=null?q.getName():x);}catch(Exception ignored){}}msg(sender,"&d配信者モード: &f"+(names.isEmpty()?"なし":String.join(", ",names)));return true;}Player q=who.equalsIgnoreCase("me")&&sender instanceof Player self?self:Bukkit.getPlayerExact(who);if(q==null){msg(sender,"&cプレイヤーが見つかりません。");return true;}boolean on=a.length<4||!a[3].equalsIgnoreCase("off");String uid=q.getUniqueId().toString();raw.removeIf(x->x.equalsIgnoreCase(uid));if(on)raw.add(uid);getConfig().set("tag-mode.streamer-players",raw);saveConfig();msg(sender,"&d"+q.getName()+" &fの配信者モードを "+(on?"&aON &7（TAG残機2）":"&cOFF &7（TAG残機1）")+" &fにしました。");return true;}
            if(sub.equals("oniwait")&&sender instanceof Player p){LocationStore.set(getConfig(),"tag-mode.oni-wait",p.getLocation());saveConfig();msg(p,"&a鬼ごっこの鬼待機地点を設定しました。 &7未設定時はロビーを使用します。");return true;}
            if(sub.equals("status")){Location sp=LocationStore.get(getConfig(),"tag-mode.shrine.player"),so=LocationStore.get(getConfig(),"tag-mode.shrine.oni");msg(sender,"&d鬼ごっこ &7| 状態=&f"+state+" &7| 神社ぷれいやー=&f"+(sp==null?"未設定":"設定済み")+" &7| 鬼=&f"+(so==null?"未設定":"設定済み")+" &7| 出現猶予=&e"+getConfig().getInt("tag-mode.oni-release-seconds",45)+"秒");return true;}
            if(sub.equals("shrine")&&sender instanceof Player p){if(a.length<3){msg(p,"/og tag shrine <player|oni>");return true;}String who=a[2].toLowerCase(Locale.ROOT);if(!who.equals("player")&&!who.equals("oni")){msg(p,"/og tag shrine <player|oni>");return true;}LocationStore.set(getConfig(),"tag-mode.shrine."+who,p.getLocation());saveConfig();msg(p,"&a神社の"+(who.equals("player")?"ぷれいやー開始地点":"鬼出現地点")+"を設定しました。");return true;}
        }
        if(a[0].equalsIgnoreCase("tag")&&sender instanceof Player p&&a.length>1&&(a[1].equalsIgnoreCase("spranking")||a[1].equalsIgnoreCase("spshop"))){
            Block b=p.getTargetBlockExact(6);if(b==null){msg(p,"&c6ブロック以内の登録したいブロックへ視点を合わせてください。");return true;}
            if(a[1].equalsIgnoreCase("spranking")){LocationStore.set(getConfig(),"tag-sp.ranking-block",b.getLocation());saveConfig();msg(p,"&aこのブロックをSPランキング表示に登録しました。");}
            else{if(b.getType()!=Material.WHITE_SHULKER_BOX){msg(p,"&cSPショップには白色のシュルカーボックスを使用してください。");return true;}List<String> shops=getConfig().getStringList("tag-sp.shop-blocks");String k=LocationStore.encode(b.getLocation());if(!shops.contains(k))shops.add(k);getConfig().set("tag-sp.shop-blocks",shops);if(getConfig().getStringList("tag-sp.shop-products."+shopKey(k)).isEmpty())getConfig().set("tag-sp.shop-products."+shopKey(k),defaultSpShopProducts());saveConfig();setTagShopVisibility(isTagMode());msg(p,"&aこの白色シュルカーボックスをSPショップに登録しました。 &7商品編集: /og spshop add ...");}return true;
        }
        if(a[0].equalsIgnoreCase("spshop")&&sender instanceof Player p){
            Block b=p.getTargetBlockExact(6);if(b==null){msg(p,"&c登録済みSPショップへ視点を合わせてください。");return true;}String loc=LocationStore.encode(b.getLocation());if(!getConfig().getStringList("tag-sp.shop-blocks").contains(loc)){msg(p,"&cそのブロックはSPショップではありません。");return true;}String path="tag-sp.shop-products."+shopKey(loc);List<String> products=new ArrayList<>(getConfig().getStringList(path));
            if(a.length>=2&&a[1].equalsIgnoreCase("clear")){products.clear();getConfig().set(path,products);saveConfig();msg(p,"&eこのショップの商品を全削除しました。");return true;}
            if(a.length>=5&&a[1].equalsIgnoreCase("add")&&isInteger(a[4])){String kind=a[2].toLowerCase(Locale.ROOT),id=a[3].toUpperCase(Locale.ROOT);int price=Math.max(0,Integer.parseInt(a[4])),uses=a.length>=6&&isInteger(a[5])?Math.max(1,Integer.parseInt(a[5])):1;products.add(kind+":"+id+":"+price+":"+uses);getConfig().set(path,products);saveConfig();msg(p,"&a商品追加: &f"+kind+":"+id+" &d"+price+"SP &7x"+uses);return true;}
            msg(p,"/og spshop add <skill|item> <ID> <price> [uses] &7または /og spshop clear");return true;
        }
        if(a[0].equalsIgnoreCase("armorcheck")&&sender instanceof Player p){EntityEquipment eq=p.getEquipment();msg(p,"&b[OniArmorCheck] &f鬼種=&e"+oniType.name());armorCheckLine(p,"HEAD",eq.getHelmet());armorCheckLine(p,"CHEST",eq.getChestplate());armorCheckLine(p,"LEGS",eq.getLeggings());armorCheckLine(p,"BOOTS",eq.getBoots());return true;}
        if(a[0].equalsIgnoreCase("ikimono")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){
                msg(sender,"異獣（イキモノ）: "+(getConfig().getBoolean("ikimono.enabled",false)?"&aON":"&cOFF")+" &7| 現在個体=&f"+ikimonoIds.size()+" &7| /og ikimono <on|off|status|spawn|clear>");return true;
            }
            String sub=a[1].toLowerCase(Locale.ROOT);
            if(sub.equals("on")){
                getConfig().set("ikimono.enabled",true);saveConfig();
                msg(sender,"&a異獣（イキモノ）をONにしました。 &7設定は再起動後も維持されます。");
                if(state==GameState.RUNNING&&!finalPhase&&ikimonoIds.isEmpty()){Location center=LocationStore.get(getConfig(),"arena-setup.center");if(center==null)center=LocationStore.get(getConfig(),"locations.player-spawn");if(center==null&&sender instanceof Player p)center=p.getLocation();if(center!=null)startIkimonoSystem(center);}
                return true;
            }
            if(sub.equals("off")){
                getConfig().set("ikimono.enabled",false);saveConfig();clearIkimonoSystem();
                msg(sender,"&c異獣（イキモノ）をOFFにしました。 &7現在の異獣も消去しました。設定は再起動後も維持されます。");return true;
            }
            if(sub.equals("spawn")){
                if(!(sender instanceof Player p)){msg(sender,"&cspawn はプレイヤーから実行してください。");return true;}
                Location at=findIkimonoCommandSpawn(p.getLocation());spawnIkimono(at);
                if(state==GameState.RUNNING&&!finalPhase&&ikimonoTicker==null)ikimonoTicker=Bukkit.getScheduler().runTaskTimer(this,this::tickIkimono,20L,10L);
                msg(p,"&5異獣（イキモノ） &fを現在地付近に1体召喚しました。 &7現在個体="+ikimonoIds.size());return true;
            }
            if(sub.equals("clear")){int n=ikimonoIds.size();clearIkimonoEntitiesOnly();msg(sender,"&a異獣を "+n+"体消去しました。 &7ON/OFF設定と発動中の陣営バフは変更しません。");return true;}
            msg(sender,"/og ikimono <on|off|status|spawn|clear>");return true;
        }
        if(a[0].equalsIgnoreCase("playerbotextra")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"ぷれいやーBot EXTRA: "+(getConfig().getBoolean("player-bot.extra.enabled",true)?"&aON":"&cOFF")+" &7/og playerbotextra <on|off|status>");return true;}
            if(a[1].equalsIgnoreCase("on")||a[1].equalsIgnoreCase("off")){boolean enabled=a[1].equalsIgnoreCase("on");getConfig().set("player-bot.extra.enabled",enabled);saveConfig();msg(sender,"ぷれいやーBot EXTRAを "+(enabled?"&aON":"&cOFF")+" &fにしました。 &7スキルチェックと状況判断が変化します。");return true;}
            msg(sender,"/og playerbotextra <on|off|status>");return true;
        }
        if(a[0].equalsIgnoreCase("azakuji")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"字九字ひろ通常戦参戦: "+(getConfig().getBoolean("azakuji-ally.normal-match.enabled",false)?"&aON":"&cOFF")+" &7/ 確率=&e"+getConfig().getDouble("azakuji-ally.normal-match.chance-percent",15.0)+"%");return true;}
            if(a[1].equalsIgnoreCase("on")||a[1].equalsIgnoreCase("off")){boolean enabled=a[1].equalsIgnoreCase("on");getConfig().set("azakuji-ally.normal-match.enabled",enabled);saveConfig();msg(sender,"字九字ひろの通常戦ランダム参戦を "+(enabled?"&aON":"&cOFF")+" &fにしました。");return true;}
            if(a[1].equalsIgnoreCase("chance")){if(a.length<3){msg(sender,"/og azakuji chance <0-100>");return true;}try{double chance=Math.max(0,Math.min(100,Double.parseDouble(a[2])));getConfig().set("azakuji-ally.normal-match.chance-percent",chance);saveConfig();msg(sender,"字九字ひろの通常戦参戦確率を &e"+chance+"% &fに設定しました。");}catch(NumberFormatException ex){msg(sender,"&c0～100の数値を指定してください。");}return true;}
            msg(sender,"/og azakuji <on|off|status|chance 0-100>");return true;
        }
        if(a[0].equalsIgnoreCase("botdifficulty")){if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"鬼Bot難易度: &e"+botDifficulty.name()+" &7(NORMAL / ELITE / EXTRA)");return true;}BotDifficulty d=BotDifficulty.parse(a[1]);if(d==null){msg(sender,"/og botdifficulty <normal|elite|extra|status>");return true;}botDifficulty=d;getConfig().set("bot.difficulty",d.name());saveConfig();msg(sender,"鬼Bot難易度を &e"+d.name()+" &fに変更しました。 &7※EXTRAは判断最適化です");return true;}
        if(a[0].equalsIgnoreCase("errornotify")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"OPエラー通知: "+(errorNotifyEnabled?"&aON":"&cOFF")+" &7※console記録は常時ON");return true;}
            if(a[1].equalsIgnoreCase("on")){errorNotifyEnabled=true;getConfig().set("admin-error-notify.enabled",true);saveConfig();msg(sender,"&aOP向けエラー通知をONにしました。 &7一般プレイヤーには詳細を表示しません。");return true;}
            if(a[1].equalsIgnoreCase("off")){errorNotifyEnabled=false;getConfig().set("admin-error-notify.enabled",false);saveConfig();msg(sender,"&7OP向けエラー通知をOFFにしました。 &fconsoleには引き続き記録されます。");return true;}
            msg(sender,"/og errornotify <on|off|status>");return true;
        }
        if(a[0].equalsIgnoreCase("debug")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"Debug: "+(debugTraceEnabled?"&aON":"&cOFF")+" &7/ category=&e"+debugTraceCategory);return true;}
            String mode=a[1].toLowerCase(Locale.ROOT);
            if(mode.equals("off")){debugTraceEnabled=false;debugTraceCategory="all";msg(sender,"&7OniGameデバッグ表示をOFFにしました。");return true;}
            if(mode.equals("on")||mode.equals("all")){debugTraceEnabled=true;debugTraceCategory="all";msg(sender,"&aOniGameデバッグ表示をONにしました。 &7(category=all)");debugTrace("system","DBG-000","debug trace enabled");return true;}
            if(Set.of("texture","gui","training","decoy","world").contains(mode)){debugTraceEnabled=true;debugTraceCategory=mode;msg(sender,"&aOniGameデバッグ表示をONにしました。 &7category=&e"+mode);debugTrace(mode,"DBG-001","category trace enabled");return true;}
            msg(sender,"/og debug <on|off|status|texture|gui|training|decoy|world>");return true;
        }
        if(a[0].equalsIgnoreCase("training")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og training <pos1|pos2|clear|info|dummy|refresh>");return true;}
            if(a[1].equalsIgnoreCase("pos1")||a[1].equalsIgnoreCase("pos2")){String path="locations.training-"+a[1].toLowerCase(Locale.ROOT);LocationStore.set(getConfig(),path,p.getLocation().getBlock().getLocation());saveConfig();msg(p,"練習エリアの &e"+a[1].toUpperCase(Locale.ROOT)+" &fを現在地に設定しました。");return true;}
            if(a[1].equalsIgnoreCase("clear")){getConfig().set("locations.training-pos1",null);getConfig().set("locations.training-pos2",null);saveConfig();for(UUID id:new HashSet<>(trainingPlayers)){Player q=Bukkit.getPlayer(id);if(q!=null)leaveTraining(q);}removeTrainingDummy();msg(p,"練習エリア設定を解除しました。");return true;}
            if(a[1].equalsIgnoreCase("info")){Location one=LocationStore.get(getConfig(),"locations.training-pos1"),two=LocationStore.get(getConfig(),"locations.training-pos2");msg(p,"練習エリア: "+(one!=null&&two!=null?"&a設定済み &7("+one.getBlockX()+","+one.getBlockY()+","+one.getBlockZ()+" ～ "+two.getBlockX()+","+two.getBlockY()+","+two.getBlockZ()+")":"&c未設定"));return true;}
            if(a[1].equalsIgnoreCase("dummy")){if(a.length>2&&a[2].equalsIgnoreCase("clear")){removeTrainingDummy();getConfig().set("locations.training-dummy",null);saveConfig();msg(p,"練習ダミーを解除しました。");return true;}if(!isInTrainingArea(p.getLocation())){msg(p,"練習エリア内で実行してください。");return true;}Location spawn=p.getLocation().clone();LocationStore.set(getConfig(),"locations.training-dummy",spawn);saveConfig();spawnTrainingDummy(spawn);msg(p,"現在地に練習用鬼ダミーを設置しました。");return true;}
            msg(p,"/og training <pos1|pos2|clear|info|dummy [clear]|refresh>");return true;
        }
        if(a[0].equalsIgnoreCase("automap")&&sender instanceof Player p){
            if(a.length>=2&&a[1].equalsIgnoreCase("maxy")){
                if(a.length==2){msg(p,"&e自動配置上限: &fY="+getConfig().getInt("arena-setup.spawn-max-y",90));return true;}
                try{int maxY=Integer.parseInt(a[2]);int worldMin=p.getWorld().getMinHeight()+2,worldMax=p.getWorld().getMaxHeight()-3;if(maxY<worldMin||maxY>worldMax){msg(p,"&cmax-y は "+worldMin+"～"+worldMax+" の範囲で指定してください。");return true;}getConfig().set("arena-setup.spawn-max-y",maxY);saveConfig();msg(p,"&a自動配置要素すべての上限を &eY="+maxY+" &aに設定しました。");return true;}catch(NumberFormatException ex){msg(p,"&c使用法: /og automap maxy <Y>");return true;}
            }
            msg(p,"/og automap maxy [Y]");return true;
        }
        if(a[0].equalsIgnoreCase("arenasetup")&&sender instanceof Player p){
            if(state!=GameState.WAITING){msg(p,"&c試合中は通常アリーナを再設定できません。");return true;}
            if(a.length>1&&a[1].equalsIgnoreCase("clear")){
                getConfig().set("arena-setup.enabled",false);
                getConfig().set("arena-setup.center",null);
                getConfig().set("locations.player-spawn",null);
                getConfig().set("locations.oni-spawn",null);
                getConfig().set("locations.exit",null);
                getConfig().set("locations.exit2",null);
                getConfig().set("locations.hearts",new ArrayList<String>());
                saveConfig();msg(p,"&7通常アリーナの一括設定を解除しました。");return true;
            }
            if(a.length>1&&a[1].equalsIgnoreCase("info")){
                Location center=LocationStore.get(getConfig(),"arena-setup.center");
                int hearts=getConfig().getStringList("locations.hearts").size();
                Location exit1=LocationStore.get(getConfig(),"locations.exit"),exit2=LocationStore.get(getConfig(),"locations.exit2");
                msg(p,"通常アリーナ: "+(getConfig().getBoolean("arena-setup.enabled",false)?"&a有効":"&c無効")
                    +" &7/ 中心: "+(center==null?"未設定":center.getBlockX()+","+center.getBlockY()+","+center.getBlockZ())
                    +" &7/ 心臓候補: &e"+hearts+" &7/ 出口: &e"+(exit1!=null?1:0)+"/2"+(exit2!=null?" &a設定済み":" &c出口B未設定")+" &7/ ボーダー: &e"+getConfig().getInt("arena-setup.size",200)+"x"+getConfig().getInt("arena-setup.size",200)+" &7/ スポーン上限: &eY="+getConfig().getInt("arena-setup.spawn-max-y",90));
                return true;
            }
            configureNormalArena(p);return true;
        }
        if(a[0].equalsIgnoreCase("ally")){
            if(state!=GameState.WAITING){msg(sender,"&c友軍NPCの設定はゲーム開始前のみ変更できます。");return true;}
            if(a.length<2||a[1].equalsIgnoreCase("status")){
                String preset=getConfig().getString("friendly-npc.preset","");
                msg(sender,"友軍NPC: "+(preset==null||preset.isBlank()?"&7なし":"&a"+friendlyPresetDisplay(preset)+" &7("+preset+")"));
                return true;
            }
            if(a[1].equalsIgnoreCase("clear")||a[1].equalsIgnoreCase("off")){
                getConfig().set("friendly-npc.preset","");saveConfig();msg(sender,"&7友軍NPCを無効にしました。");return true;
            }
            if(a[1].equalsIgnoreCase("list")){
                msg(sender,"友軍NPCプリセット: &eIGAMI_KYOYA, AZANAMI_MISAKI, ARIKAWA_FUUKA, AZANAMI_REN, MEDIC, AKASAKA_HIIRO, KAGAYA_RION, AMANAI_IONA &7/ 字九字ひろはNPC専用: /og azakuji");
                return true;
            }
            String preset=parseFriendlyPreset(a[1]);
            if(preset==null){msg(sender,"&c不明なプリセットです。 &7/og ally list で一覧を確認してください。");return true;}
            if("AZAKUJI_HIRO".equals(preset)){msg(sender,"&6字九字ひろ &fはNPC専用です。通常戦は &e/og azakuji &fで管理してください。");return true;}
            getConfig().set("friendly-npc.preset",preset);saveConfig();
            msg(sender,"次回のゲームに友軍NPC &a"+friendlyPresetDisplay(preset)+" &fを参加させます。");
            return true;
        }
        if(a[0].equalsIgnoreCase("unstuck")&&sender instanceof Player p){
            if(!getConfig().getBoolean("safety-rescue.enabled",true)||!getConfig().getBoolean("safety-rescue.player-command-enabled",true)){msg(p,"&c詰み救済は現在無効です。");return true;}
            if(state!=GameState.RUNNING){msg(p,"&7このコマンドは試合中のみ使用できます。");return true;}
            UUID id=p.getUniqueId();boolean active=players.contains(id)||isOni(id);
            if(!active||dead.contains(id)||escaped.contains(id)||p.getGameMode()==GameMode.SPECTATOR){msg(p,"&c現在は詰み救済を使用できません。");return true;}
            long now=System.currentTimeMillis(),lock=safetyAbuseLockUntil.getOrDefault(id,0L);
            if(now<lock){msg(p,"&c詰み救済は悪用防止ロック中です。 &7残り "+Math.max(1,(lock-now+999)/1000)+"秒");return true;}
            long ready=safetyRescueCooldownUntil.getOrDefault(id,0L);
            if(now<ready){msg(p,"&7詰み救済の再使用まで &e"+Math.max(1,(ready-now+999)/1000)+"秒");return true;}
            String denied=manualUnstuckDeniedReason(p);
            if(denied!=null){registerUnstuckAbuseAttempt(p,denied);return true;}
            if(rescueEntityToSafePlace(p,true)){safetyAbuseAttempts.remove(id);safetyAbuseWindowStartedAt.remove(id);msg(p,"&a安全な場所へ移動しました。");}
            else msg(p,"&c周囲に安全な移動先を見つけられませんでした。");
            return true;
        }
        if(a[0].equalsIgnoreCase("lobbyarea")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og lobbyarea <pos1|pos2|clear|info>");return true;}
            String sub=a[1].toLowerCase(Locale.ROOT);
            if(sub.equals("pos1")||sub.equals("pos2")){LocationStore.set(getConfig(),"locations.lobby-area-"+sub,p.getLocation());saveConfig();msg(p,"ロビーエリア "+sub+" を現在地に設定しました。");return true;}
            if(sub.equals("clear")){getConfig().set("locations.lobby-area-pos1",null);getConfig().set("locations.lobby-area-pos2",null);saveConfig();msg(p,"ロビーエリア境界を解除しました。");return true;}
            if(sub.equals("info")){Location la=LocationStore.get(getConfig(),"locations.lobby-area-pos1"),lb=LocationStore.get(getConfig(),"locations.lobby-area-pos2");msg(p,"ロビー境界: "+(la!=null&&lb!=null?"&a設定済み":"&c未設定"));return true;}
            msg(p,"/og lobbyarea <pos1|pos2|clear|info>");return true;
        }
        if(a[0].equalsIgnoreCase("lobbyblocks")){
            if(a.length<2){msg(sender,"/og lobbyblocks <material|preview|restore|status>");return true;}
            String sub=a[1].toLowerCase(Locale.ROOT);
            if(sub.equals("material")){
                if(a.length<3||a[2].equalsIgnoreCase("list")){msg(sender,"ロビー消去対象: &e"+String.join(", ",getLobbyBlockMaterialNames()));return true;}
                String op=a[2].toLowerCase(Locale.ROOT);
                if(op.equals("addhand")&&sender instanceof Player p){Material m=p.getInventory().getItemInMainHand().getType();if(!m.isBlock()||m==Material.AIR){msg(p,"&cブロックを手に持って実行してください。");return true;}setLobbyBlockMaterial(m,true);msg(p,"&a"+m.name()+" &fをゲーム中の消去対象に追加しました。");return true;}
                if((op.equals("add")||op.equals("remove"))&&a.length>=4){Material m=Material.matchMaterial(a[3]);if(m==null||!m.isBlock()||m==Material.AIR){msg(sender,"&cブロック名を確認してください。");return true;}setLobbyBlockMaterial(m,op.equals("add"));msg(sender,(op.equals("add")?"&a追加: ":"&e解除: ")+m.name());return true;}
                msg(sender,"/og lobbyblocks material <add BLOCK|addhand|remove BLOCK|list>");return true;
            }
            if(sub.equals("preview")){if(!(sender instanceof Player p)){msg(sender,"&cプレイヤーから実行してください。");return true;}int n=previewLobbyMaterialBlocks(p);msg(p,"&bゲーム開始時に消えるブロックを表示しました。 &7対象 "+n+"個");return true;}
            if(sub.equals("restore")){restoreLobbyMaterialBlocks();msg(sender,"&aロビー専用ブロックを復元しました。");return true;}
            if(sub.equals("status")){msg(sender,"ロビー専用ブロック: &e"+getLobbyBlockMaterialNames().size()+"種類 &7/ 保存中="+(getConfig().getBoolean("lobby-material-blocks.snapshot-active",false)?"&aYES":"&fNO"));return true;}
            msg(sender,"/og lobbyblocks <material|preview|restore|status>");return true;
        }
        if(a[0].equalsIgnoreCase("lobbyall")){
            Location lobby=LocationStore.get(getConfig(),"locations.lobby");
            if(lobby==null||lobby.getWorld()==null){msg(sender,"&cロビー地点が未設定です。 &7/og set lobby で設定してください。");return true;}
            int moved=0;
            for(Player target:Bukkit.getOnlinePlayers()){
                if(target.teleport(lobby)){target.setFallDistance(0);moved++;}
            }
            all("&6管理者によりロビーへ集合しました。");
            msg(sender,"&aオンラインプレイヤー "+moved+"人 &fをロビーへ移動しました。");
            return true;
        }
        if(a[0].equalsIgnoreCase("lobbyhide")){
            if(a.length>1&&a[1].equalsIgnoreCase("restore")){restoreLobbyUtilities();refreshLobbyGuideDisplays();msg(sender,"&aロビー設備を保存データから復元しました。");return true;}
            List<Location> targets=lobbyUtilityLocations();
            msg(sender,"ロビー設備自動退避: &e"+targets.size()+"地点 &7/ 保存スナップショット: "+(getConfig().getBoolean("lobby-hide.snapshot-active",false)?"&aあり":"&fなし"));
            return true;
        }
        if(a[0].equalsIgnoreCase("gmbook")&&sender instanceof Player p){p.getInventory().addItem(createGmBook());msg(p,"&6GM操作本 &fを渡しました。");return true;}
        if(a[0].equalsIgnoreCase("onimenu")&&sender instanceof Player p){if(state!=GameState.WAITING){msg(p,"鬼選択はロビーで行ってください。");return true;}openOniMenu(p);return true;}
        if(a[0].equalsIgnoreCase("skillchest")&&sender instanceof Player p){Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内のチェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.skill-chest",block.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"このチェストをロビーのスキル設定チェストに登録しました。上部に案内表示も設置しました。");return true;}
        if(a[0].equalsIgnoreCase("onichest")&&sender instanceof Player p){Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内のチェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.oni-chest",block.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"このチェストを鬼選択・パッシブ設定用に登録しました。上部に案内表示も設置しました。");return true;}
        if(a[0].equalsIgnoreCase("guide")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og guide <rules|refresh|clear>");return true;}
            if(a[1].equalsIgnoreCase("rules")){LocationStore.set(getConfig(),"locations.rules-text",p.getLocation());saveConfig();refreshLobbyGuideDisplays();msg(p,"&a現在地にゲームルール説明を設置しました。");return true;}
            if(a[1].equalsIgnoreCase("refresh")){refreshLobbyGuideDisplays();msg(p,"&aロビー案内表示を再生成しました。");return true;}
            if(a[1].equalsIgnoreCase("clear")){getConfig().set("locations.rules-text",null);saveConfig();refreshLobbyGuideDisplays();msg(p,"&eゲームルール説明を解除しました。");return true;}
            msg(p,"/og guide <rules|refresh|clear>");return true;
        }
        if(a[0].equalsIgnoreCase("exlock")){
            if(a.length==1||a[1].equalsIgnoreCase("status")){msg(sender,"EXロック: 鬼神（オニガミ)="+(isOniTypeLocked(OniType.KANKI)?"&cLOCK":"&aOPEN")+"&f / 蛇窟姫="+(isJakutsukiLocked()?"&cLOCK":"&aOPEN")+"&f / アザクジ="+(isAzakujiLocked()?"&cLOCK":"&aOPEN"));return true;}
            String target=a[1].toLowerCase(Locale.ROOT);if(!target.equals("azakuji")&&!target.equals("jakutsuki")&&!target.equals("onigami")&&!target.equals("kishin")&&!target.equals("kanki")){msg(sender,"/og exlock <azakuji|jakutsuki|onigami> <lock|unlock|status>");return true;}
            if(target.equals("onigami")||target.equals("kishin")||target.equals("kanki")){msg(sender,"&5鬼神（オニガミ）はDLC用に休止中です。現在は解禁できません。");return true;}
            if(a.length<3||a[2].equalsIgnoreCase("status")){boolean locked=target.equals("azakuji")?isAzakujiLocked():(target.equals("jakutsuki")?isJakutsukiLocked():isOniTypeLocked(OniType.KANKI));msg(sender,(target.equals("azakuji")?"字九字ひろ":target.equals("jakutsuki")?"蛇窟姫":"鬼神（オニガミ）")+" は現在 "+(locked?"&cLOCK":"&aOPEN")+" &fです。");return true;}
            boolean lock;if(a[2].equalsIgnoreCase("lock")||a[2].equalsIgnoreCase("on"))lock=true;else if(a[2].equalsIgnoreCase("unlock")||a[2].equalsIgnoreCase("off"))lock=false;else{msg(sender,"lock / unlock / status を指定してください。");return true;}
            String path=target.equals("azakuji")?"content-locks.azakuji-hiro":target.equals("jakutsuki")?"content-locks.jakutsuki":"content-locks.onigami";getConfig().set(path,lock);saveConfig();
            if(target.equals("azakuji")&&lock)resetLockedAzakujiSelections();
            if(target.equals("jakutsuki")&&lock&&selectedOniType==OniType.JAKUTSUKI)selectedOniType=firstUnlockedOni();
            if((target.equals("onigami")||target.equals("kishin")||target.equals("kanki"))&&lock&&selectedOniType==OniType.KANKI)selectedOniType=firstUnlockedOni();
            msg(sender,(target.equals("azakuji")?"EXプリセット 字九字ひろ":target.equals("jakutsuki")?"EX鬼 蛇窟姫（ジャクツキ）":"EX鬼 鬼神（オニガミ）")+" を "+(lock?"&cロック":"&aアンロック")+" &fしました。ロック中は選択UIに表示されません。");return true;
        }
        if(a[0].equalsIgnoreCase("onilock")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){StringBuilder st=new StringBuilder("鬼ロック: ");for(OniType t:new OniType[]{OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI})st.append(t.display).append("=").append(isOniTypeLocked(t)?"LOCK":"OPEN").append(" ");msg(sender,st.toString());return true;}
            OniType t=OniType.parse(a[1]);if(t==null||t==OniType.KANKI||t==OniType.JAKUTSUKI){msg(sender,"/og onilock <dakko|kiou|shikki|yuuki> <lock|unlock|status>");return true;}
            if(a.length<3||a[2].equalsIgnoreCase("status")){msg(sender,t.display+" は現在 "+(isOniTypeLocked(t)?"&cLOCK":"&aOPEN")+" &fです。");return true;}
            boolean lock=a[2].equalsIgnoreCase("lock")||a[2].equalsIgnoreCase("on");if(!lock&&!a[2].equalsIgnoreCase("unlock")&&!a[2].equalsIgnoreCase("off")){msg(sender,"lock / unlock / status を指定してください。");return true;}
            getConfig().set("content-locks."+oniLockKey(t),lock);saveConfig();if(lock&&selectedOniType==t)selectedOniType=firstUnlockedOni();msg(sender,t.display+" を "+(lock?"&cロック":"&aアンロック")+" &fしました。ロック中は選択・ランダム抽選から除外されます。");return true;
        }
        if(a[0].equalsIgnoreCase("forceoni")){if(a.length<2){msg(sender,"/og forceoni <プレイヤー|clear> を指定してください。");return true;}if(a[1].equalsIgnoreCase("clear")){forcedOni=null;getConfig().set("forced-oni-uuid",null);saveConfig();msg(sender,"手動の鬼指定を解除しました。次回は鬼指定ブロック、またはランダム抽選を使用します。");return true;}Player target=Bukkit.getPlayerExact(a[1]);if(target==null){msg(sender,"そのプレイヤーはオンラインではありません。");return true;}forcedOni=target.getUniqueId();getConfig().set("forced-oni-uuid",forcedOni.toString());saveConfig();msg(sender,"次回の鬼を &c"+target.getName()+" &fに指定しました。&7（/og join が必要です）");return true;}
        if(a[0].equalsIgnoreCase("oniblock")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){getConfig().set("locations.oni-selector-block",null);saveConfig();msg(p,"鬼指定ブロックを解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null){msg(p,"6ブロック以内の登録したいブロックへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.oni-selector-block",block.getLocation());saveConfig();msg(p,"&c"+block.getType().name()+" &fを鬼指定ブロックに登録しました。次回開始時、このブロックの上にいる参加者を鬼候補にします。");return true;}
        if(a[0].equalsIgnoreCase("practicechest")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){getConfig().set("locations.practice-loot-chest",null);saveConfig();msg(p,"開錠練習チェストを解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null||(block.getType()!=Material.CHEST&&block.getType()!=Material.TRAPPED_CHEST)){msg(p,"6ブロック以内の練習用チェストへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.practice-loot-chest",block.getLocation());saveConfig();msg(p,"&6このチェストをロビー限定の開錠練習チェストに登録しました。");return true;}
        if(a[0].equalsIgnoreCase("practiceheart")&&sender instanceof Player p){if(a.length>1&&a[1].equalsIgnoreCase("clear")){Location old=LocationStore.get(getConfig(),"locations.practice-heart");if(old!=null){String key=LocationStore.encode(old);heartHp.remove(key);for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player q=Bukkit.getPlayer(id);if(q!=null)stopRepair(q,"&7練習地点が解除されたため中断しました。");}}getConfig().set("locations.practice-heart",null);saveConfig();msg(p,"心臓破壊練習地点を解除しました。");return true;}Block block=p.getTargetBlockExact(6);if(block==null){msg(p,"6ブロック以内の練習用心臓ブロックへ視点を合わせてください。");return true;}LocationStore.set(getConfig(),"locations.practice-heart",block.getLocation());saveConfig();Material heartMat=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN"));if(heartMat==null)heartMat=Material.CRYING_OBSIDIAN;block.setType(heartMat);msg(p,"&4このブロックをロビー限定の心臓破壊練習地点に登録しました。");return true;}
        if(a[0].equalsIgnoreCase("heartmode")&&a.length>1){String mode=a[1].toLowerCase();if(!mode.equals("random")&&!mode.equals("manual")){msg(sender,"random または manual を指定してください。");return true;}getConfig().set("heart-placement-mode",mode);saveConfig();msg(sender,"心臓配置を &e"+(mode.equals("random")?"ランダム":"手動マーカー")+" &fに設定しました。");return true;}
        if(a[0].equalsIgnoreCase("marker")&&sender instanceof Player p){p.getInventory().addItem(createHeartMarker());msg(p,"心臓地点マーカーを渡しました。設置すると自動で透明になります。");return true;}
        if(a[0].equalsIgnoreCase("markers")){int count=scanHeartMarkers(true).size();msg(sender,"有効な心臓地点マーカー: &e"+count+"個");return true;}
        if(a[0].equalsIgnoreCase("finaltest")&&sender instanceof Player p){
            if(a.length>1&&a[1].equalsIgnoreCase("clear")){clearFinalEffectTest();msg(p,"&a最終フェーズ演出テストを解除しました。");return true;}
            startFinalEffectTest(p);msg(p,"&4最終フェーズ演出テストを開始しました。 &7解除: /og finaltest clear");return true;
        }
        if(a[0].equalsIgnoreCase("finalsky")&&sender instanceof Player p){
            if(a.length<2){msg(p,"/og finalsky <test|clear|status>");return true;}
            if(a[1].equalsIgnoreCase("test")){startFinalSkyTest(p);return true;}
            if(a[1].equalsIgnoreCase("clear")){clearFinalSkyTest();msg(p,"&aCustom Sky単体テストを解除しました。");return true;}
            if(a[1].equalsIgnoreCase("status")){msg(p,"Custom Sky: "+(getConfig().getBoolean("final-crimson.blood-sky.optifine-custom-sky",true)?"&aON":"&cOFF")+" &f/ test-time=&e"+getConfig().getLong("final-crimson.blood-sky.optifine-test-player-time",6000L)+" &f/ weather=&e"+"CLEAR");return true;}
            msg(p,"/og finalsky <test|clear|status>");return true;
        }
        if(a[0].equalsIgnoreCase("bgm")&&a.length>1){if(a[1].equalsIgnoreCase("off")){getConfig().set("final-phase-bgm.enabled",false);saveConfig();stopGameStartBgm();stopFinalPhaseBgm();msg(sender,"最終局面BGMを無効にしました。");}else{getConfig().set("final-phase-bgm.enabled",true);getConfig().set("final-phase-bgm.sound",a[1]);saveConfig();msg(sender,"最終局面BGMを &e"+a[1]+" &fに設定しました。");}return true;}
        if(a[0].equalsIgnoreCase("bgmtest")&&sender instanceof Player p){playConfiguredBgm(p);msg(p,"最終局面BGMをテスト再生しました。");return true;}
        if(a[0].equalsIgnoreCase("bgmstop")){stopGameStartBgm();stopFinalPhaseBgm();msg(sender,"BGMを停止しました。");return true;}
        if(a[0].equalsIgnoreCase("testbook")&&sender instanceof Player p){if(!isTestGm(p)){msg(p,"この本は参加者0人のテストプレイ中のみ取得できます。");return true;}giveTestPlayBook(p);return true;}
        if(a[0].equalsIgnoreCase("testdestroy")&&sender instanceof Player p){if(!isTestGm(p)){msg(p,"この操作は参加者0人のテストプレイ中のみ使用できます。");return true;}Block target=p.getTargetBlockExact(getConfig().getInt("test-heart-target-range",20));if(target==null||!heartHp.containsKey(LocationStore.encode(target.getLocation()))){msg(p,"視線の先に未破壊の心臓がありません。");return true;}destroyHeart(target);msg(p,"視線先の心臓をテスト操作で破壊しました。");return true;}
        if(a[0].equalsIgnoreCase("testborder")&&a.length>1){boolean enabled;if(a[1].equalsIgnoreCase("on"))enabled=true;else if(a[1].equalsIgnoreCase("off"))enabled=false;else{msg(sender,"on または off を指定してください。");return true;}getConfig().set("test-arena.world-border-enabled",enabled);saveConfig();msg(sender,"テスト区域のワールドボーダーを "+(enabled?"&a有効":"&c無効")+" &fにしました。次回のテスト開始時に反映されます。");return true;}
        if(a[0].equalsIgnoreCase("botgame")&&sender instanceof Player p){OniType type=a.length>1?OniType.parse(a[1]):selectedOniType;if(type==null||isOniTypeLocked(type))type=OniType.DAKKO;if(a.length>1&&OniType.parse(a[1])!=null&&!ensureOniUnlocked(p,OniType.parse(a[1])))return true;start(type,p,true);return true;}
        if(a[0].equalsIgnoreCase("onibotgame")&&sender instanceof Player p){OniType requested=a.length>1?OniType.parse(a[1]):selectedOniType;if(a.length>1&&requested!=null&&!ensureOniUnlocked(p,requested))return true;OniType type=requested;if(type==null||isOniTypeLocked(type))type=OniType.DAKKO;int count=getConfig().getInt("player-bot.default-count",5);if(a.length>2)try{count=Integer.parseInt(a[2]);}catch(NumberFormatException ignored){}startOniVsBots(type,p,Math.max(1,Math.min(8,count)));return true;}
        if((a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch"))&&sender instanceof Player p){OniType type=a.length>1?requireOniType(p,a[1]):selectedOniType;if(type==null){if(a.length<=1)msg(p,"&c鬼タイプを指定してください: dakko / kio / shikki / yuuki / kishin / jakutsuki");return true;}if(type==OniType.JAKUTSUKI&&!ensureOniUnlocked(p,type))return true;int count=5;if(a.length>2)try{count=Integer.parseInt(a[2]);}catch(NumberFormatException ignored){}selectedOniType=type;msg(p,"&8AI Bot Match: &c"+type.display+" &7を強制指定して開始します。");startFullAiBotMatch(type,p,Math.max(1,Math.min(8,count)));return true;}
        if(a[0].equalsIgnoreCase("onitest")&&sender instanceof Player p){if(a.length<2){msg(p,"&e/og onitest <dakko|kio|shikki|yuuki|kishin|jakutsuki> [1-8]");return true;}OniType type=requireOniType(p,a[1]);if(type==null)return true;if(type==OniType.JAKUTSUKI&&!ensureOniUnlocked(p,type))return true;int count=5;if(a.length>2)try{count=Integer.parseInt(a[2]);}catch(NumberFormatException ignored){}selectedOniType=type;msg(p,"&8鬼テスト: &c"+type.display+" &7を強制指定。あなたが鬼になります。");startOniVsBots(type,p,Math.max(1,Math.min(8,count)));return true;}
        if(a[0].equalsIgnoreCase("set")&&sender instanceof Player p&&a.length>1){
            switch(a[1].toLowerCase()){
                case "lobby" -> LocationStore.set(getConfig(),"locations.lobby",p.getLocation());
                case "player" -> LocationStore.set(getConfig(),"locations.player-spawn",p.getLocation());
                case "oni" -> LocationStore.set(getConfig(),"locations.oni-spawn",p.getLocation());
                case "exit" -> LocationStore.set(getConfig(),"locations.exit",p.getLocation());
                case "exit2" -> LocationStore.set(getConfig(),"locations.exit2",p.getLocation());
                default -> {msg(p,"lobby / player / oni / exit / exit2 を指定してください。");return true;}
            } saveConfig();msg(p,a[1]+"地点を設定しました。");return true;
        }
        if(a[0].equalsIgnoreCase("heart")&&sender instanceof Player p){List<String> list=getConfig().getStringList("locations.hearts");String encoded=LocationStore.encode(p.getLocation());if(list.contains(encoded)){list.remove(encoded);msg(p,"この心臓候補を削除しました。");}else{list.add(encoded);msg(p,"心臓候補を追加しました（"+list.size()+"件）。");}getConfig().set("locations.hearts",list);saveConfig();return true;}
        if(a[0].equalsIgnoreCase("chest")&&sender instanceof Player p){List<String> list=getConfig().getStringList("locations.loot-chests");String encoded=LocationStore.encode(p.getLocation());if(list.contains(encoded)){list.remove(encoded);msg(p,"このルートチェスト候補を削除しました。");}else{list.add(encoded);msg(p,"ルートチェスト候補を追加しました（"+list.size()+"件）。");}getConfig().set("locations.loot-chests",list);saveConfig();return true;}
        if(a[0].equalsIgnoreCase("chests")){msg(sender,"登録済みルートチェスト候補: &e"+getConfig().getStringList("locations.loot-chests").size()+"個");return true;}
        if(a[0].equalsIgnoreCase("start")){OniType type=a.length>1?OniType.parse(a[1]):null;if(type!=null&&!ensureOniUnlocked(sender,type))return true;start(type,sender,false);return true;}
        if(a[0].equalsIgnoreCase("stop")){end(false,"管理者が試合を終了しました");return true;}
        if(a[0].equalsIgnoreCase("version")){msg(sender,"&dOniGame Runtime &f0.37.7 &7| GUI schema: &fskills 2p / passives 4p &7| Expected RP: &fv37");return true;}
        if(a[0].equalsIgnoreCase("duo")&&sender instanceof Player p){
            if(a.length>=2&&a[1].equalsIgnoreCase("botmatch")){
                if(state!=GameState.WAITING){msg(p,"&c試合中は開始できません。");return true;}
                OniType primary=a.length>=3?requireOniType(p,a[2]):selectedOniType;
                if(primary==null||isOniTypeLocked(primary))primary=firstUnlockedOni();
                getConfig().set("game-mode","duo");saveConfig();selectedOniType=primary;duoBotMatchStart=true;
                msg(p,"&4DUO Bot Match &7――あなたはぷれいやー側。鬼Bot2体で開始します。1体目: &c"+primary.display+" &7/ 相方: 別種類から自動選択");
                start(primary,p,true);return true;
            }
            msg(p,"/og duo botmatch [dakko|kio|shikki|yuuki|kishin|onigami|kanki|jakutsuki]");return true;
        }
        if(a[0].equalsIgnoreCase("pv")){
            String sub=a.length>1?a[1].toLowerCase(Locale.ROOT):"status";
            if(sub.equals("start")){startPv(sender);return true;}
            if(sub.equals("stop")){stopPv(sender);return true;}
            if(sub.equals("status")){msg(sender,"&dPV &7| mode="+(isPvMode()?"ON":"OFF")+" | state="+state+" | freeze="+pvFrozen+" | actors="+participants.size()+" | bots="+(playerBots.size()+(oniBot==null?0:1)));return true;}
            if(sub.equals("freeze")){pvFrozen=true;for(UUID id:playerBots)if(Bukkit.getEntity(id) instanceof Mob m)m.setAI(false);if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof Mob m)m.setAI(false);msg(sender,"&bPV役者/Botを静止しました。撮影者は自由に移動できます。");return true;}
            if(sub.equals("unfreeze")){pvFrozen=false;for(UUID id:playerBots)if(Bukkit.getEntity(id) instanceof Mob m)m.setAI(true);if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof Mob m)m.setAI(true);msg(sender,"&aPV静止を解除しました。");return true;}
            if(sub.equals("time")&&sender instanceof Player p){String v=a.length>2?a[2].toLowerCase(Locale.ROOT):"day";pvFixedTime=switch(v){case "noon"->6000L;case "sunset"->12000L;case "night"->14000L;case "midnight"->18000L;default->1000L;};p.getWorld().setTime(pvFixedTime);p.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);msg(p,"&dPV時刻を &f"+v+" &dに固定しました。");return true;}
            if(sub.equals("weather")&&sender instanceof Player p){String v=a.length>2?a[2].toLowerCase(Locale.ROOT):"clear";World w=p.getWorld();w.setStorm(!v.equals("clear"));w.setThundering(v.equals("thunder"));w.setWeatherDuration(Integer.MAX_VALUE);msg(p,"&dPV天候を &f"+v+" &dにしました。");return true;}
            if(sub.equals("heal")){for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null){p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());p.setFoodLevel(20);p.setFireTicks(0);}}msg(sender,"&aPV役者を全回復しました。");return true;}
            if(sub.equals("actor")&&a.length>=3){Player q=Bukkit.getPlayerExact(a[2]);if(q==null){msg(sender,"&cプレイヤーが見つかりません。");return true;}String role=a.length>=4?a[3].toLowerCase(Locale.ROOT):"player";participants.add(q.getUniqueId());if(role.equals("oni")){players.remove(q.getUniqueId());oniTeam.add(q.getUniqueId());oni=q.getUniqueId();setupOni(q,q.getLocation());}else{oniTeam.remove(q.getUniqueId());players.add(q.getUniqueId());setupPlayer(q,q.getLocation());}msg(sender,"&dPV役者: &f"+q.getName()+" &7-> &f"+role);return true;}
            if(sub.equals("bot")&&sender instanceof Player p){if(a.length<3){msg(p,"/og pv bot <player|oni|clear> [oniType]");return true;}String kind=a[2].toLowerCase(Locale.ROOT);if(kind.equals("clear")){removePlayerBots();removeOniBot();msg(p,"&7PV Botを全削除しました。");return true;}if(kind.equals("oni")){if(a.length>=4){OniType t=OniType.parse(a[3]);if(t!=null)oniType=t;}spawnOniBot(p.getLocation());msg(p,"&c鬼Botを現在位置に召喚しました。");return true;}spawnPlayerBot(p.getLocation(),playerBots.size()+1);msg(p,"&bぷれいやーBotを現在位置に召喚しました。");return true;}
            if(sub.equals("effect")&&sender instanceof Player p){String e=a.length>2?a[2].toLowerCase(Locale.ROOT):"";if(e.equals("hyakki")){for(Player q:Bukkit.getOnlinePlayers()){q.sendTitle(cc("&4&l百鬼夜行"),"",5,35,10);msg(q,"&c目的を捨てて、生き延びろ。");}return true;}if(e.equals("final")){startFinalCrimson(p.getWorld());return true;}if(e.equals("clear")){resetFinalCrimsonVisuals();return true;}msg(p,"/og pv effect <hyakki|final|clear>");return true;}
            msg(sender,"/og pv <start|stop|status|actor|bot|time|weather|freeze|unfreeze|heal|effect>");return true;
        }
        if(a[0].equalsIgnoreCase("mode")){if(a.length<2){String cm=getConfig().getString("game-mode","normal").toLowerCase(Locale.ROOT);msg(sender,"現在のモード: &e"+(cm.equals("duo")?"二人鬼":cm.equals("escape")?"ESCAPE（大人数脱出）":cm.equals("tag")?"鬼ごっこ":"通常"));return true;}if(state!=GameState.WAITING){msg(sender,"&cゲーム中はモードを変更できません。");return true;}String m=a[1].toLowerCase(Locale.ROOT);if(!List.of("normal","duo","escape","tag","pv").contains(m)){msg(sender,"/og mode <normal|duo|escape|tag|pv>");return true;}getConfig().set("game-mode",m);saveConfig();setTagShopVisibility(m.equals("tag"));setTerraGemVisibility(m.equals("tag"));all("&eゲームモードを &f"+(m.equals("duo")?"二人鬼（鬼2 / ぷれいやー12）":m.equals("escape")?"ESCAPE（広域イベント脱出）":m.equals("tag")?"鬼ごっこ（大人数ぷれいやーBot対応）":m.equals("pv")?"PV撮影（時間進行・自動Botなし）":"通常")+" &eに変更しました。");return true;}
        if(a[0].equalsIgnoreCase("role")){if(!(sender instanceof Player rp)){msg(sender,"ゲーム内で使用してください。");return true;}if(state!=GameState.WAITING){msg(rp,"&cゲーム中は希望役割を変更できません。");return true;}String pref=a.length<2?rolePreferences.getOrDefault(rp.getUniqueId(),"auto"):a[1].toLowerCase(Locale.ROOT);if(a.length>=2&&!List.of("oni","player","auto").contains(pref)){msg(rp,"/og role <oni|player|auto>");return true;}rolePreferences.put(rp.getUniqueId(),pref);msg(rp,"希望役割: &e"+(pref.equals("oni")?"鬼":pref.equals("player")?"ぷれいやー":"どちらでも"));return true;}
        if(a[0].equalsIgnoreCase("escape")){
            if(a.length<2||a[1].equalsIgnoreCase("status")){msg(sender,"ESCAPE: Phase &e"+escapePhase+"&7/5 &7イベント &a"+escapeEventsCompleted+"&7/&f"+escapeEventsRequired+" &7/ 地域 &b"+getEscapeRegionIds().size());return true;}
            if(a[1].equalsIgnoreCase("next")){if(state!=GameState.RUNNING||!isEscapeMode()){msg(sender,"&cESCAPEモードの試合中のみ使用できます。");return true;}advanceEscapePhase();return true;}
            if(a[1].equalsIgnoreCase("progress")){if(state!=GameState.RUNNING||!isEscapeMode()){msg(sender,"&cESCAPEモードの試合中のみ使用できます。");return true;}int add=1;if(a.length>2)try{add=Math.max(1,Integer.parseInt(a[2]));}catch(NumberFormatException ignored){}escapeEventsCompleted=Math.min(escapeEventsRequired,escapeEventsCompleted+add);all("&6【広域攻略】 &fイベント進行 &a"+escapeEventsCompleted+"&7/&f"+escapeEventsRequired);if(escapeEventsCompleted>=escapeEventsRequired&&escapePhase<4){escapePhase=4;announceEscapePhase();}return true;}
            if(a[1].equalsIgnoreCase("region")&&sender instanceof Player ep){
                if(a.length<3){msg(ep,"/og escape region <pos1|pos2|add|list|remove>");return true;}
                String sub=a[2].toLowerCase(Locale.ROOT);
                if(sub.equals("pos1")||sub.equals("pos2")){LocationStore.set(getConfig(),"escape.region-selection."+sub,ep.getLocation().getBlock().getLocation());saveConfig();msg(ep,"&bESCAPE地域選択 "+sub.toUpperCase(Locale.ROOT)+" &fを設定しました。");return true;}
                if(sub.equals("add")){if(a.length<4){msg(ep,"/og escape region add <id> [表示名]");return true;}Location p1=LocationStore.get(getConfig(),"escape.region-selection.pos1"),p2=LocationStore.get(getConfig(),"escape.region-selection.pos2");if(p1==null||p2==null||p1.getWorld()!=p2.getWorld()){msg(ep,"&c先に同じワールドで pos1 / pos2 を設定してください。");return true;}String id=a[3].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","");if(id.isBlank()){msg(ep,"&cIDは英数字・_・-を使用してください。");return true;}String display=a.length>4?String.join(" ",Arrays.copyOfRange(a,4,a.length)):id;String base="escape.regions."+id;LocationStore.set(getConfig(),base+".pos1",p1);LocationStore.set(getConfig(),base+".pos2",p2);getConfig().set(base+".display",display);saveConfig();msg(ep,"&a地域を登録: &f"+display+" &7("+id+")");return true;}
                if(sub.equals("list")){List<String> ids=getEscapeRegionIds();msg(ep,"&bESCAPE地域 &7("+ids.size()+"): &f"+(ids.isEmpty()?"未登録":String.join(", ",ids)));return true;}
                if(sub.equals("remove")&&a.length>3){getConfig().set("escape.regions."+a[3],null);saveConfig();msg(ep,"&7地域を削除しました: "+a[3]);return true;}
                msg(ep,"/og escape region <pos1|pos2|add|list|remove>");return true;
            }
            msg(sender,"/og escape <status|next|progress|region>");return true;
        }
        if(a[0].equalsIgnoreCase("area")){return handleAreaCommand(sender,a);}
        if(a[0].equalsIgnoreCase("status")){String mode=getConfig().getString("heart-placement-mode","random");Player forcedPlayer=forcedOni==null?null:Bukkit.getPlayer(forcedOni);boolean hasOniBlock=LocationStore.get(getConfig(),"locations.oni-selector-block")!=null;msg(sender,"状態: "+state+" / 参加: "+participants.size()+" / ぷれいやー: "+players.size()+" / ぷれいやーBot: "+playerBots.size()+" / 心臓: "+brokenHearts+"/"+heartGoal()+" (配置"+totalHearts+")"+" / 配置: "+mode+" / 選択鬼: "+(selectedOniType==null?"ランダム":selectedOniType.display)+" / 鬼指定: "+(forcedOni==null?"なし":forcedPlayer==null?forcedOni.toString():forcedPlayer.getName())+" / 鬼指定台: "+(hasOniBlock?"設定済み":"なし"));return true;}
        help(sender);return true;
    }
    private boolean handleAreaCommand(CommandSender sender,String[] a){
        if(!(sender instanceof Player p)){msg(sender,"&cゲーム内から実行してください。");return true;}
        if(!isEffectiveAdmin(p)){msg(p,"&c権限がありません。");return true;}
        if(a.length<2){areaHelp(p);return true;}
        String sub=a[1].toLowerCase(Locale.ROOT);
        if(sub.equals("list")){List<String> ids=getAreaPointIds();msg(p,"&bArea Action Points &7("+ids.size()+"): &f"+(ids.isEmpty()?"未登録":String.join(", ",ids)));return true;}
        if(sub.equals("remove")){if(a.length<3){msg(p,"/og area remove <id>");return true;}getConfig().set("area-points.points."+a[2].toLowerCase(Locale.ROOT),null);saveConfig();msg(p,"&7Area Pointを削除: &f"+a[2]);return true;}
        if(sub.equals("show")){showAreaPoints(p);return true;}
        if(sub.equals("add")){if(a.length<4){msg(p,"/og area add <updraft|jump_pad|spirit_road|safe_drop> <id> [値]");return true;}AreaPointType type=AreaPointType.parse(a[2]);if(type==null){areaHelp(p);return true;}String id=sanitizeAreaId(a[3]);if(id.isBlank()){msg(p,"&cIDは英数字・_・-を使用してください。");return true;}double value=defaultAreaPower(type);if(a.length>4)try{value=Double.parseDouble(a[4]);}catch(NumberFormatException ignored){}saveAreaPoint(id,type,p.getLocation().getBlock().getLocation().add(.5,0,.5),value,yawToCardinalYaw(p.getLocation().getYaw()));msg(p,"&aArea Point登録: &f"+id+" &7["+type.name()+"] 値="+String.format(Locale.ROOT,"%.1f",value));return true;}
        if(sub.equals("autogen")){int radius=getConfig().getInt("area-points.autogen.radius",96);if(a.length>2)try{radius=Math.max(24,Math.min(256,Integer.parseInt(a[2])));}catch(NumberFormatException ignored){}generateAreaCandidates(p,radius);return true;}
        if(sub.equals("accept")){if(a.length<3){msg(p,"/og area accept <番号>");return true;}try{int n=Integer.parseInt(a[2]);AreaCandidate c=areaCandidates.stream().filter(x->x.number()==n).findFirst().orElse(null);if(c==null){msg(p,"&cその候補番号はありません。");return true;}String id="auto_"+c.type().name().toLowerCase(Locale.ROOT)+"_"+n;saveAreaPoint(id,c.type(),c.location(),c.power(),c.extra());msg(p,"&a候補 #"+n+" を登録: &f"+id);return true;}catch(NumberFormatException ex){msg(p,"&c番号を指定してください。");return true;}}
        if(sub.equals("acceptall")){int n=0;for(AreaCandidate c:new ArrayList<>(areaCandidates)){String id="auto_"+c.type().name().toLowerCase(Locale.ROOT)+"_"+c.number();saveAreaPoint(id,c.type(),c.location(),c.power(),c.extra());n++;}msg(p,"&a候補を一括登録しました: &f"+n+"地点");return true;}
        if(sub.equals("clearcandidates")){areaCandidates.clear();msg(p,"&7自動生成候補を消去しました。");return true;}
        areaHelp(p);return true;
    }
    private void areaHelp(CommandSender s){msg(s,"&b/og area add <updraft|jump_pad|spirit_road|safe_drop> <id> [値]");msg(s,"&b/og area <list|show|remove> / autogen [半径] / accept <番号> / acceptall");}
    private String sanitizeAreaId(String raw){return raw.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]","");}
    private double defaultAreaPower(AreaPointType t){return switch(t){case UPDRAFT->getConfig().getDouble("area-points.defaults.updraft-height",18.0);case JUMP_PAD->getConfig().getDouble("area-points.defaults.jump-pad-power",1.45);case SPIRIT_ROAD->getConfig().getDouble("area-points.defaults.spirit-road-speed",1.15);case SAFE_DROP->getConfig().getDouble("area-points.defaults.safe-drop-forward",0.45);};}
    private double yawToCardinalYaw(float yaw){double y=yaw%360;if(y<0)y+=360;return Math.round(y/45.0)*45.0%360;}
    private void saveAreaPoint(String id,AreaPointType type,Location loc,double power,double extra){String b="area-points.points."+id;getConfig().set(b+".type",type.name());LocationStore.set(getConfig(),b+".location",loc);getConfig().set(b+".power",power);getConfig().set(b+".direction-yaw",extra);saveConfig();}
    private List<String> getAreaPointIds(){org.bukkit.configuration.ConfigurationSection sec=getConfig().getConfigurationSection("area-points.points");if(sec==null)return new ArrayList<>();List<String> out=new ArrayList<>(sec.getKeys(false));Collections.sort(out);return out;}
    private void showAreaPoints(Player p){int shown=0;for(String id:getAreaPointIds()){String b="area-points.points."+id;Location l=LocationStore.get(getConfig(),b+".location");if(l==null||l.getWorld()!=p.getWorld())continue;AreaPointType t=AreaPointType.parse(getConfig().getString(b+".type",""));if(t==null)continue;spawnAreaPreview(l,t,true);shown++;}for(AreaCandidate c:areaCandidates)if(c.location().getWorld()==p.getWorld()){spawnAreaPreview(c.location(),c.type(),false);p.sendMessage(cc("&e#"+c.number()+" &7"+c.type().name()+" &f"+c.location().getBlockX()+","+c.location().getBlockY()+","+c.location().getBlockZ()));}msg(p,"&b登録地点 &f"+shown+" &7/ 候補 &e"+areaCandidates.size()+" &7を約数秒可視化します。");for(int i=1;i<=8;i++)Bukkit.getScheduler().runTaskLater(this,()->{for(String id:getAreaPointIds()){String b="area-points.points."+id;Location l=LocationStore.get(getConfig(),b+".location");AreaPointType t=AreaPointType.parse(getConfig().getString(b+".type",""));if(l!=null&&l.getWorld()==p.getWorld()&&t!=null)spawnAreaPreview(l,t,true);}for(AreaCandidate c:areaCandidates)if(c.location().getWorld()==p.getWorld())spawnAreaPreview(c.location(),c.type(),false);},i*10L);}
    private void spawnAreaPreview(Location l,AreaPointType t,boolean registered){Particle particle=registered?Particle.END_ROD:Particle.WAX_ON;for(int i=0;i<10;i++){double y=i*.45;l.getWorld().spawnParticle(particle,l.clone().add(0,y,0),2,.18,.08,.18,0);}}
    private void updateAreaActionPoints(){if(!getConfig().getBoolean("area-points.enabled",true))return;for(String id:getAreaPointIds()){String b="area-points.points."+id;Location l=LocationStore.get(getConfig(),b+".location");if(l==null||l.getWorld()==null)continue;AreaPointType type=AreaPointType.parse(getConfig().getString(b+".type",""));if(type==null)continue;double power=getConfig().getDouble(b+".power",defaultAreaPower(type)),yaw=getConfig().getDouble(b+".direction-yaw",0);renderAreaPoint(l,type,power,yaw);double radius=getConfig().getDouble("area-points.trigger-radius",1.15);for(Player p:l.getWorld().getPlayers()){if(p.getGameMode()==GameMode.SPECTATOR||p.getLocation().distanceSquared(l)>radius*radius)continue;triggerAreaPoint(p,id,type,l,power,yaw);}}}
    private void renderAreaPoint(Location l,AreaPointType t,double power,double yaw){switch(t){case UPDRAFT->{double h=Math.min(power,28);for(double y=.2;y<=h;y+=1.5)l.getWorld().spawnParticle(Particle.CLOUD,l.clone().add(0,y,0),2,.28,.12,.28,.015);l.getWorld().spawnParticle(Particle.END_ROD,l.clone().add(0,.4,0),3,.35,.15,.35,.01);}case JUMP_PAD->l.getWorld().spawnParticle(Particle.END_ROD,l.clone().add(0,.25,0),6,.45,.05,.45,.02);case SPIRIT_ROAD->{Vector d=yawVector(yaw);for(int i=0;i<5;i++)l.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,l.clone().add(d.clone().multiply(i*.8)).add(0,.15,0),1,.12,.05,.12,0);}case SAFE_DROP->l.getWorld().spawnParticle(Particle.PORTAL,l.clone().add(0,.25,0),6,.35,.1,.35,.02);}}
    private Vector yawVector(double yaw){double r=Math.toRadians(yaw);return new Vector(-Math.sin(r),0,Math.cos(r));}
    private void triggerAreaPoint(Player p,String id,AreaPointType t,Location l,double power,double yaw){long now=System.currentTimeMillis();if(now<areaPointCooldownUntil.getOrDefault(p.getUniqueId(),0L))return;areaPointCooldownUntil.put(p.getUniqueId(),now+getConfig().getLong("area-points.retrigger-millis",900));Vector dir=yawVector(yaw);switch(t){case UPDRAFT->{double vy=Math.min(2.15,Math.max(.75,.55+power*.055));p.setVelocity(p.getVelocity().multiply(.2).setY(vy));areaFallProtectionUntil.put(p.getUniqueId(),now+7000);p.sendActionBar(cc("&b↑ 上昇気流 &7―― 高所へ！"));}case JUMP_PAD->{p.setVelocity(dir.multiply(Math.max(.5,power)).setY(Math.max(.55,power*.58)));areaFallProtectionUntil.put(p.getUniqueId(),now+6000);p.sendActionBar(cc("&e➜ 跳躍床"));}case SPIRIT_ROAD->{Vector v=dir.multiply(Math.max(.45,power));v.setY(Math.max(p.getVelocity().getY(),.05));p.setVelocity(v);p.sendActionBar(cc("&3≫ 霊道"));}case SAFE_DROP->{p.setVelocity(dir.multiply(Math.max(.1,power)).setY(-.75));areaFallProtectionUntil.put(p.getUniqueId(),now+9000);p.sendActionBar(cc("&d↓ 安全落下"));}}}
    @EventHandler public void onAreaFallDamage(EntityDamageEvent e){if(!(e.getEntity() instanceof Player p)||e.getCause()!=EntityDamageEvent.DamageCause.FALL)return;if(System.currentTimeMillis()<areaFallProtectionUntil.getOrDefault(p.getUniqueId(),0L)){e.setCancelled(true);p.setFallDistance(0);areaFallProtectionUntil.remove(p.getUniqueId());}}
    private void generateAreaCandidates(Player p,int radius){areaCandidates.clear();World w=p.getWorld();int step=Math.max(6,getConfig().getInt("area-points.autogen.sample-step",8)),max=getConfig().getInt("area-points.autogen.max-candidates",16);int baseX=p.getLocation().getBlockX(),baseZ=p.getLocation().getBlockZ(),num=1;List<Location> chosen=new ArrayList<>();for(int x=baseX-radius;x<=baseX+radius&&areaCandidates.size()<max;x+=step)for(int z=baseZ-radius;z<=baseZ+radius&&areaCandidates.size()<max;z+=step){if((x-baseX)*(x-baseX)+(z-baseZ)*(z-baseZ)>radius*radius)continue;int y=w.getHighestBlockYAt(x,z);Block top=w.getBlockAt(x,y-1,z);if(!top.getType().isSolid())continue;int bestDiff=0,bestDx=0,bestDz=0;int[][] ds={{step,0},{-step,0},{0,step},{0,-step}};for(int[] d:ds){int ny=w.getHighestBlockYAt(x+d[0],z+d[1]);int diff=ny-y;if(diff>bestDiff){bestDiff=diff;bestDx=d[0];bestDz=d[1];}}if(bestDiff<getConfig().getInt("area-points.autogen.min-height-difference",8))continue;Location low=new Location(w,x+.5,y,z+.5);boolean near=false;for(Location q:chosen)if(q.distanceSquared(low)<144){near=true;break;}if(near)continue;chosen.add(low);double yaw=Math.toDegrees(Math.atan2(-bestDx,bestDz));areaCandidates.add(new AreaCandidate(num++,AreaPointType.UPDRAFT,low,Math.min(28,bestDiff+2),yaw));}
        // Add a few traversal candidates near strong elevation transitions, without modifying blocks.
        int extraNum=num;for(AreaCandidate up:new ArrayList<>(areaCandidates)){if(areaCandidates.size()>=max)break;Vector d=yawVector(up.extra());Location jp=up.location().clone().add(d.clone().multiply(-3));areaCandidates.add(new AreaCandidate(extraNum++,AreaPointType.JUMP_PAD,jp,1.35,up.extra()));}
        msg(p,"&b地形解析完了。 &e"+areaCandidates.size()+"地点 &fを候補化しました。 &7/og area show で確認 → /og area accept <番号> または acceptall");showAreaPoints(p);
    }

    private TextDisplay spawnLobbyGuide(Location location,String text){
        if(location==null||location.getWorld()==null)return null;
        TextDisplay display=location.getWorld().spawn(location,TextDisplay.class);
        display.setText(cc(text));
        display.setBillboard(Display.Billboard.CENTER);
        display.setSeeThrough(true);
        display.setShadowed(true);
        display.setDefaultBackground(false);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
        display.setLineWidth(260);
        display.setViewRange(18.0f);
        display.setPersistent(false);
        lobbyGuideDisplays.add(display.getUniqueId());
        return display;
    }
    private void removeLobbyGuideDisplays(){
        for(UUID id:new HashSet<>(lobbyGuideDisplays)){
            Entity entity=Bukkit.getEntity(id);
            if(entity!=null)entity.remove();
        }
        lobbyGuideDisplays.clear();
    }
    private void refreshLobbyGuideDisplays(){
        removeLobbyGuideDisplays();
        if(!getConfig().getBoolean("lobby-guide.enabled",true))return;
        Location skill=LocationStore.get(getConfig(),"locations.skill-chest");
        if(skill!=null)spawnLobbyGuide(skill.clone().add(.5,getConfig().getDouble("lobby-guide.chest-height",1.65),.5),
            getConfig().getString("lobby-guide.skill-chest-text","&b&l【スキル選択】\n&f右クリックでスキル・パッシブを設定"));
        Location oniChest=LocationStore.get(getConfig(),"locations.oni-chest");
        if(oniChest!=null)spawnLobbyGuide(oniChest.clone().add(.5,getConfig().getDouble("lobby-guide.chest-height",1.65),.5),
            getConfig().getString("lobby-guide.oni-chest-text","&c&l【鬼設定】\n&f鬼・鬼パッシブを設定"));
        Location rules=LocationStore.get(getConfig(),"locations.rules-text");
        if(rules!=null)spawnLobbyGuide(rules.clone().add(0,getConfig().getDouble("lobby-guide.rules-height",1.8),0),
            getConfig().getString("lobby-guide.rules-text","&4&l◆ 鬼げぇむ ◆\n&fぷれいやー: 心臓を破壊し、出口から脱出\n&c鬼: 心臓を守り、ぷれいやーを全滅させる\n&6全心臓破壊後は残り3分の最終フェーズ"));
    }
    private List<Location> lobbyUtilityLocations(){
        List<Location> out=new ArrayList<>();
        String[] paths={"locations.skill-chest","locations.oni-chest","locations.practice-loot-chest","locations.practice-heart","locations.oni-selector-block"};
        Set<String> seen=new HashSet<>();
        for(String path:paths){
            Location loc=LocationStore.get(getConfig(),path);
            if(loc==null||loc.getWorld()==null)continue;
            loc=loc.getBlock().getLocation();
            String key=LocationStore.encode(loc);
            if(seen.add(key))out.add(loc);
        }
        return out;
    }
    private List<String> getLobbyBlockMaterialNames(){
        List<String> out=new ArrayList<>();
        for(String raw:getConfig().getStringList("lobby-material-blocks.materials")){Material m=Material.matchMaterial(raw);if(m!=null&&m.isBlock()&&m!=Material.AIR&&!out.contains(m.name()))out.add(m.name());}
        return out;
    }
    private void setLobbyBlockMaterial(Material material,boolean add){
        List<String> names=getLobbyBlockMaterialNames();
        if(add){if(!names.contains(material.name()))names.add(material.name());}else names.remove(material.name());
        getConfig().set("lobby-material-blocks.materials",names);saveConfig();
    }
    private List<Block> findLobbyMaterialBlocks(){
        Location a=LocationStore.get(getConfig(),"locations.lobby-area-pos1"),b=LocationStore.get(getConfig(),"locations.lobby-area-pos2");
        if(a==null||b==null||a.getWorld()==null||b.getWorld()==null||!a.getWorld().equals(b.getWorld()))return List.of();
        Set<Material> mats=new HashSet<>();for(String n:getLobbyBlockMaterialNames()){Material m=Material.matchMaterial(n);if(m!=null)mats.add(m);}if(mats.isEmpty())return List.of();
        int minX=Math.min(a.getBlockX(),b.getBlockX()),maxX=Math.max(a.getBlockX(),b.getBlockX()),minY=Math.max(a.getWorld().getMinHeight(),Math.min(a.getBlockY(),b.getBlockY())),maxY=Math.min(a.getWorld().getMaxHeight()-1,Math.max(a.getBlockY(),b.getBlockY())),minZ=Math.min(a.getBlockZ(),b.getBlockZ()),maxZ=Math.max(a.getBlockZ(),b.getBlockZ());
        long volume=(long)(maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1),cap=Math.max(1000,getConfig().getLong("lobby-material-blocks.max-scan-blocks",500000));
        if(volume>cap){reportAdminError("LobbyBlocks scan","範囲が大きすぎます: "+volume+" blocks (max "+cap+")",null,null);return List.of();}
        List<Block> out=new ArrayList<>();World w=a.getWorld();for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){Block block=w.getBlockAt(x,y,z);if(mats.contains(block.getType()))out.add(block);}return out;
    }
    private void hideLobbyMaterialBlocks(){
        if(!getConfig().getBoolean("lobby-material-blocks.enabled",true))return;
        if(getConfig().getBoolean("lobby-material-blocks.snapshot-active",false))restoreLobbyMaterialBlocks();
        List<Block> blocks=findLobbyMaterialBlocks();getConfig().set("lobby-material-blocks.snapshot",null);int i=0;
        for(Block block:blocks){String base="lobby-material-blocks.snapshot."+i++;getConfig().set(base+".location",LocationStore.encode(block.getLocation()));getConfig().set(base+".block-data",block.getBlockData().getAsString());block.setType(Material.AIR,false);}
        getConfig().set("lobby-material-blocks.snapshot-count",i);getConfig().set("lobby-material-blocks.snapshot-active",i>0);saveConfig();
        if(i>0)getLogger().info("LobbyBlocks: hid "+i+" configured parkour blocks.");
    }
    private void restoreLobbyMaterialBlocks(){
        if(!getConfig().getBoolean("lobby-material-blocks.snapshot-active",false))return;int count=Math.max(0,getConfig().getInt("lobby-material-blocks.snapshot-count",0));
        for(int i=0;i<count;i++){String base="lobby-material-blocks.snapshot."+i;Location loc=LocationStore.decode(getConfig().getString(base+".location",""));if(loc==null||loc.getWorld()==null)continue;String data=getConfig().getString(base+".block-data","");try{loc.getBlock().setBlockData(Bukkit.createBlockData(data),false);}catch(Exception ex){reportAdminError("LobbyBlocks restore",ex.getClass().getSimpleName()+": "+ex.getMessage(),null,loc);}}
        getConfig().set("lobby-material-blocks.snapshot",null);getConfig().set("lobby-material-blocks.snapshot-count",0);getConfig().set("lobby-material-blocks.snapshot-active",false);saveConfig();
    }
    private int previewLobbyMaterialBlocks(Player viewer){List<Block> blocks=findLobbyMaterialBlocks();int step=Math.max(1,blocks.size()/400);for(int i=0;i<blocks.size();i+=step){Location l=blocks.get(i).getLocation().add(.5,.6,.5);viewer.spawnParticle(Particle.END_ROD,l,3,.18,.18,.18,0);}return blocks.size();}
    private void hideLobbyUtilities(){
        hideLobbyMaterialBlocks();
        if(!getConfig().getBoolean("lobby-hide.enabled",true))return;
        // Recover an old interrupted snapshot first instead of overwriting it.
        if(getConfig().getBoolean("lobby-hide.snapshot-active",false))restoreLobbyUtilities();
        removeLobbyGuideDisplays();
        for(UUID id:new HashSet<>(trainingPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)leaveTraining(p);}
        removeTrainingDummy();
        List<Location> targets=lobbyUtilityLocations();
        getConfig().set("lobby-hide.snapshot",null);
        int index=0;
        for(Location loc:targets){
            Block block=loc.getBlock();
            String base="lobby-hide.snapshot."+index++;
            getConfig().set(base+".location",LocationStore.encode(loc));
            getConfig().set(base+".material",block.getType().name());
            getConfig().set(base+".block-data",block.getBlockData().getAsString());
            BlockState state=block.getState();
            if(state instanceof org.bukkit.inventory.InventoryHolder holder){
                getConfig().set(base+".inventory",Arrays.asList(holder.getInventory().getContents()));
                holder.getInventory().clear();
            }
            block.setType(Material.AIR,false);
        }
        getConfig().set("lobby-hide.snapshot-count",index);
        getConfig().set("lobby-hide.snapshot-active",index>0);
        saveConfig();
    }
    private void restoreLobbyUtilities(){
        restoreLobbyMaterialBlocks();
        if(!getConfig().getBoolean("lobby-hide.snapshot-active",false))return;
        int count=Math.max(0,getConfig().getInt("lobby-hide.snapshot-count",0));
        for(int i=0;i<count;i++){
            String base="lobby-hide.snapshot."+i;
            String encoded=getConfig().getString(base+".location","");
            Location loc=LocationStore.decode(encoded);
            if(loc==null||loc.getWorld()==null)continue;
            Material material=Material.matchMaterial(getConfig().getString(base+".material","AIR"));
            if(material==null)material=Material.AIR;
            Block block=loc.getBlock();
            block.setType(material,false);
            String data=getConfig().getString(base+".block-data","");
            if(data!=null&&!data.isBlank()){
                try{block.setBlockData(Bukkit.createBlockData(data),false);}catch(IllegalArgumentException ex){getLogger().warning("Lobby block data restore failed: "+data);}
            }
            List<?> saved=getConfig().getList(base+".inventory");
            BlockState state=block.getState();
            if(saved!=null&&state instanceof org.bukkit.inventory.InventoryHolder holder){
                Inventory inv=holder.getInventory();
                inv.clear();
                for(int slot=0;slot<Math.min(inv.getSize(),saved.size());slot++){
                    Object value=saved.get(slot);
                    if(value instanceof ItemStack item)inv.setItem(slot,item);
                }
            }
        }
        getConfig().set("lobby-hide.snapshot",null);
        getConfig().set("lobby-hide.snapshot-count",0);
        getConfig().set("lobby-hide.snapshot-active",false);
        saveConfig();
    }

    private boolean isInteger(String s){try{Integer.parseInt(s);return true;}catch(Exception e){return false;}}
    private String shopKey(String encoded){return Integer.toUnsignedString(encoded.hashCode(),36);}
    private List<String> defaultSpShopProducts(){return List.of("skill:SPRINT:4:2","skill:SMOKE:5:2","skill:BLINK:7:1","skill:ECHO:6:2","item:HEAL:3:1","item:FLARE:5:1");}
    private void randomizeTagSpShops(){
        List<String> master=new ArrayList<>(List.of("skill:SPRINT:4:2","skill:INVISIBLE:6:1","skill:SMOKE:5:2","skill:BLINK:7:1","skill:ECHO:6:2","skill:HEAL:6:1","skill:CLAIRVOYANCE:5:1","skill:SEALING_CIRCLE:8:1","skill:RESONANCE:7:1","skill:SUBSTITUTE:8:1","skill:DESPERATE_RUN:9:1","item:HEAL:3:1","item:FLARE:5:1"));
        List<String> shops=new ArrayList<>(getConfig().getStringList("tag-sp.shop-blocks"));
        if(shops.isEmpty())return;
        Random r=new Random();Collections.shuffle(shops,r);
        // All shops draw from one shared deck. A product cannot appear in another shop
        // until every product has been distributed once. This prevents every shop from
        // independently rolling popular entries such as SPRINT.
        List<String> deck=new ArrayList<>(master);Collections.shuffle(deck,r);int cursor=0;
        Set<String> previousShop=new HashSet<>();
        for(String raw:shops){
            int count=Math.min(4,master.size());List<String> pick=new ArrayList<>();
            while(pick.size()<count){
                if(cursor>=deck.size()){
                    deck=new ArrayList<>(master);Collections.shuffle(deck,r);cursor=0;
                    // When a second cycle is unavoidable, do not immediately copy the
                    // preceding shop's lineup.
                    if(!previousShop.isEmpty()&&deck.size()>1)deck.sort(Comparator.comparing(previousShop::contains));
                }
                String product=deck.get(cursor++);
                if(!pick.contains(product))pick.add(product);
            }
            getConfig().set("tag-sp.shop-products."+shopKey(raw),pick);
            previousShop=new HashSet<>(pick);
        }
        saveConfig();
    }
    private void awardSoulPoints(Player p,int amount,String reason){if(p==null||amount<=0||!isTagMode()||state!=GameState.RUNNING)return;if(System.currentTimeMillis()<tagSoulRushUntil)amount*=2;UUID id=p.getUniqueId();soulPoints.merge(id,amount,Integer::sum);matchEarnedSoulPoints.merge(id,amount,Integer::sum);p.sendActionBar(cc("&d+"+amount+" SP &7"+reason+"  &8| &f所持 &d"+soulPoints.get(id)));}
    private void commitSoulRanking(){for(var e:matchEarnedSoulPoints.entrySet()){UUID id=e.getKey();int earned=e.getValue();if(earned<=0)continue;String base="tag-sp.ranking.players."+id;getConfig().set(base+".name",Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName()).orElse(id.toString().substring(0,8)));getConfig().set(base+".total",getConfig().getInt(base+".total",0)+earned);getConfig().set(base+".best",Math.max(getConfig().getInt(base+".best",0),earned));}saveConfig();}
    private boolean isSpRankingBlock(Block b){Location l=LocationStore.get(getConfig(),"tag-sp.ranking-block");return l!=null&&b!=null&&LocationStore.encode(l).equals(LocationStore.encode(b.getLocation()));}
    private boolean isSpShopBlock(Block b){return b!=null&&getConfig().getStringList("tag-sp.shop-blocks").contains(LocationStore.encode(b.getLocation()));}
    private void setTagShopVisibility(boolean visible){
        for(String raw:getConfig().getStringList("tag-sp.shop-blocks")){Location l=LocationStore.decode(raw);if(l==null||l.getWorld()==null)continue;Block b=l.getBlock();if(visible){if(b.getType()==Material.AIR)b.setType(Material.WHITE_SHULKER_BOX,false);}else if(b.getType()==Material.WHITE_SHULKER_BOX)b.setType(Material.AIR,false);}
    }
    // v0.40.77: Terra objective gems physically exist only while TAG mode is selected.
    // The registered location is retained in config, so switching back to TAG restores the correct gem block.
    private void setTerraGemVisibility(boolean visible){
        for(String gem:List.of("diamond","gold","emerald","lapis")){
            Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);
            if(l==null||l.getWorld()==null)continue;
            Block b=l.getBlock();
            Material expected=terraGemMaterial(gem);
            if(visible){
                if(b.getType()==Material.AIR)b.setType(expected,false);
            }else if(b.getType()==expected){
                b.setType(Material.AIR,false);
            }
        }
    }
    private ItemStack tagNavCompass(){ItemStack i=item(Material.COMPASS,"&b&lエリアナビ","tag_nav_compass");ItemMeta m=i.getItemMeta();m.setLore(List.of(cc("&7右クリック: 次の発見済みエリア"),cc("&7左クリック: 前の発見済みエリア")));i.setItemMeta(m);return i;}
    private List<String> tagAreaIds(){var sec=getConfig().getConfigurationSection("tag-mode.areas");return sec==null?new ArrayList<>():new ArrayList<>(sec.getKeys(false));}
    private void discoverTagAreas(Player p){if(!isTagMode()||state!=GameState.RUNNING||tagParkourPlayers.contains(p.getUniqueId())||dead.contains(p.getUniqueId()))return;LinkedHashSet<String> found=tagDiscoveredAreas.computeIfAbsent(p.getUniqueId(),k->new LinkedHashSet<>());for(String id:tagAreaIds()){Location l=LocationStore.get(getConfig(),"tag-mode.areas."+id+".location");double r=Math.max(1,getConfig().getDouble("tag-mode.areas."+id+".radius",8));if(l!=null&&l.getWorld()!=null&&l.getWorld().equals(p.getWorld())&&p.getLocation().distanceSquared(l)<=r*r&&found.add(id)){if(!tagSelectedArea.containsKey(p.getUniqueId()))tagSelectedArea.put(p.getUniqueId(),id);p.sendActionBar(cc("&bエリア発見 &f"+id));p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.8f,1.3f);}}}
    private void cycleTagNavigation(Player p,int delta){List<String> found=new ArrayList<>(tagDiscoveredAreas.getOrDefault(p.getUniqueId(),new LinkedHashSet<>()));if(found.isEmpty()){p.sendActionBar(cc("&7まだ発見済みエリアがありません。"));return;}String cur=tagSelectedArea.get(p.getUniqueId());int i=Math.max(0,found.indexOf(cur));i=(i+delta+found.size())%found.size();String id=found.get(i);tagSelectedArea.put(p.getUniqueId(),id);Location l=LocationStore.get(getConfig(),"tag-mode.areas."+id+".location");if(l!=null&&l.getWorld()!=null&&l.getWorld().equals(p.getWorld()))p.setCompassTarget(l);p.sendActionBar(cc("&bナビゲーション：&f"+id));}
    private void updateTagNavigation(){if(!isTagMode()||state!=GameState.RUNNING)return;renderTerraGems();renderTerraGates();updateTerraGemAttunements();for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p==null||dead.contains(id)||tagParkourPlayers.contains(id))continue;discoverTerraGems(p);updateTerraGateSneakEscape(p);discoverTagAreas(p);String area=tagSelectedArea.get(id);if(area==null)continue;Location target=LocationStore.get(getConfig(),"tag-mode.areas."+area+".location");if(target==null||target.getWorld()==null||!target.getWorld().equals(p.getWorld()))continue;p.setCompassTarget(target);Vector d=target.toVector().subtract(p.getLocation().toVector()).setY(0);if(d.lengthSquared()<1)continue;d.normalize();Location base=p.getLocation().clone().add(d.clone().multiply(1.7)).add(0,.15,0);Vector side=new Vector(-d.getZ(),0,d.getX());for(int n=0;n<5;n++)p.spawnParticle(Particle.END_ROD,base.clone().add(d.clone().multiply(n*.22)),1,0,0,0,0);Location tip=base.clone().add(d.clone().multiply(1.0));for(int n=1;n<=3;n++){p.spawnParticle(Particle.END_ROD,tip.clone().subtract(d.clone().multiply(n*.18)).add(side.clone().multiply(n*.13)),1,0,0,0,0);p.spawnParticle(Particle.END_ROD,tip.clone().subtract(d.clone().multiply(n*.18)).subtract(side.clone().multiply(n*.13)),1,0,0,0,0);}}
    }
    private void openSpRanking(Player p){boolean single=spRankingSingleMode.getOrDefault(p.getUniqueId(),false);MenuHolder h=new MenuHolder("sp_ranking");Inventory inv=Bukkit.createInventory(h,54,cc(single?"&5一試合SPランキング":"&d総合SPランキング"));h.inventory=inv;var sec=getConfig().getConfigurationSection("tag-sp.ranking.players");List<String> ids=sec==null?new ArrayList<>():new ArrayList<>(sec.getKeys(false));ids.sort((a,b)->Integer.compare(getConfig().getInt("tag-sp.ranking.players."+b+(single?".best":".total")),getConfig().getInt("tag-sp.ranking.players."+a+(single?".best":".total"))));for(int i=0;i<Math.min(10,ids.size());i++){String id=ids.get(i),base="tag-sp.ranking.players."+id;int val=getConfig().getInt(base+(single?".best":".total"));inv.setItem(10+i+(i>=7?2:0),menuItem(Material.PLAYER_HEAD,"&e"+(i+1)+"位 &f"+getConfig().getString(base+".name","Unknown"),"sp_rank_none","&d"+val+" SP"));}inv.setItem(49,menuItem(Material.COMPARATOR,single?"&d総合ランキングへ":"&5一試合ランキングへ","sp_rank_toggle","&7クリックで表示切替"));p.openInventory(inv);}
    private void spawnTagSkillChests(Location anchor){
        if(anchor==null||anchor.getWorld()==null)return;
        int count=Math.max(1,getConfig().getInt("tag-mode.skill-chests.count",10)); double radius=Math.max(20,getConfig().getDouble("tag-mode.skill-chests.radius",92.0));
        Random r=new Random();int made=0;for(int a=0;a<count*30&&made<count;a++){double ang=r.nextDouble()*Math.PI*2,dist=18+r.nextDouble()*(radius-18);int x=(int)Math.floor(anchor.getX()+Math.cos(ang)*dist),z=(int)Math.floor(anchor.getZ()+Math.sin(ang)*dist);Location c=safeSurfaceAt(anchor.getWorld(),x,z,0);if(c==null)continue;c=c.getBlock().getLocation();String key=LocationStore.encode(c);if(lootChestKeys.contains(key)||tagSkillChestKeys.contains(key))continue;boolean near=false;for(String k:tagSkillChestKeys){Location o=LocationStore.decode(k);if(o!=null&&o.getWorld().equals(c.getWorld())&&o.distanceSquared(c)<144){near=true;break;}}if(near)continue;temporaryLootChestOriginals.putIfAbsent(key,c.getBlock().getType());c.getBlock().setType(Material.TRAPPED_CHEST);tagSkillChestKeys.add(key);made++;}
        getLogger().info("TAG skill chests spawned: "+made+"/"+count);
    }
    private void openTagSkillChest(Player p,String key){if(!isTagMode()||state!=GameState.RUNNING||!players.contains(p.getUniqueId()))return;String ik="tagskill|"+p.getUniqueId()+"|"+key;Inventory inv=personalLootInventories.get(ik);if(inv==null){inv=Bukkit.createInventory(null,27,cc("&b&lTAG スキルチェスト"));List<PlayerSkill> pool=new ArrayList<>(Arrays.asList(PlayerSkill.values()));pool.remove(PlayerSkill.SAFE_LANDING);Collections.shuffle(pool);int n=Math.min(Math.max(1,getConfig().getInt("tag-mode.skill-chests.skills-per-chest",3)),pool.size());for(int i=0;i<n;i++){PlayerSkill sk=pool.get(i);int uses=tagSkillUses(sk);ItemStack it=playerSkillIconItem(sk,"&b&l"+sk.display+" &7[残り"+uses+"回]","skill:"+sk.name(),"&7鬼ごっこ専用・使用回数制");it.setAmount(Math.min(64,uses));inv.setItem(11+i*2,it);}personalLootInventories.put(ik,inv);}p.openInventory(inv);p.playSound(p.getLocation(),Sound.BLOCK_ENDER_CHEST_OPEN,.7f,1.25f);}
    private int tagSkillUses(PlayerSkill sk){return Math.max(1,getConfig().getInt("tag-mode.skill-uses."+sk.name().toLowerCase(Locale.ROOT),switch(sk){case SPRINT,SMOKE,BLINK->3;case INVISIBLE,HEAL,ECHO,CLAIRVOYANCE->2;default->1;}));}
    private void consumeTagSkillUse(Player p,ItemStack stack,PlayerSkill sk){int left=stack.getAmount()-1;if(left<=0){p.getInventory().setItemInMainHand(null);p.sendActionBar(cc("&c"+sk.display+"を使い切った。"));}else{stack.setAmount(left);ItemMeta m=stack.getItemMeta();if(m!=null){m.setDisplayName(cc("&b&l"+sk.display+" &7[残り"+left+"回]"));stack.setItemMeta(m);}p.sendActionBar(cc("&e"+sk.display+" &7残り &f"+left+"回"));}}

    private void openSpShop(Player p,Block b){String loc=LocationStore.encode(b.getLocation());MenuHolder h=new MenuHolder("sp_shop:"+shopKey(loc));Inventory inv=Bukkit.createInventory(h,54,cc("&5ソウルショップ &8| &d"+soulPoints.getOrDefault(p.getUniqueId(),0)+" SP"));h.inventory=inv;List<String> list=getConfig().getStringList("tag-sp.shop-products."+shopKey(loc));int slot=10;for(String raw:list){String[] x=raw.split(":");if(x.length<4)continue;String kind=x[0],id=x[1];int price=Integer.parseInt(x[2]),uses=Integer.parseInt(x[3]);ItemStack it;if(kind.equals("skill")){try{PlayerSkill sk=PlayerSkill.valueOf(id);it=playerSkillIconItem(sk,"&b"+sk.display+" &7["+uses+"回]","sp_buy:"+raw,"&d価格: "+price+" SP");}catch(Exception ex){continue;}}else{Material mat=id.equals("HEAL")?Material.GOLDEN_APPLE:id.equals("FLARE")?Material.FIREWORK_STAR:Material.CHEST;it=menuItem(mat,"&a"+id+" &7x"+uses,"sp_buy:"+raw,"&d価格: "+price+" SP");}inv.setItem(slot++,it);if(slot%9==8)slot+=2;if(slot>=44)break;}p.openInventory(inv);}
    private void buySpProduct(Player p,String raw){String[] x=raw.split(":");if(x.length<4)return;int price=Integer.parseInt(x[2]),uses=Integer.parseInt(x[3]),have=soulPoints.getOrDefault(p.getUniqueId(),0);if(have<price){msg(p,"&cSPが足りません。 &7必要 "+price+" / 所持 "+have);return;}ItemStack give;if(x[0].equals("skill")){try{PlayerSkill sk=PlayerSkill.valueOf(x[1]);give=playerSkillIconItem(sk,"&b&l"+sk.display+" &7[残り"+uses+"回]","skill:"+sk.name(),"&7鬼ごっこ用・使用回数制");give.setAmount(Math.min(64,uses));}catch(Exception ex){return;}}else if(x[1].equals("HEAL"))give=consumable(Material.GOLDEN_APPLE,"&a回復アイテム","item:heal");else if(x[1].equals("FLARE"))give=item(Material.FIREWORK_STAR,"&cフレアガン","equipment:flare_gun");else give=item(Material.PAPER,"&f"+x[1],"item:"+x[1].toLowerCase(Locale.ROOT));give.setAmount(Math.min(64,uses));soulPoints.put(p.getUniqueId(),have-price);p.getInventory().addItem(give);p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.8f,1.4f);msg(p,"&a購入しました。 &7残り &d"+(have-price)+" SP");}

    private void help(CommandSender s){
        boolean admin=!(s instanceof Player p)||isEffectiveAdmin(p);
        StringBuilder h=new StringBuilder("&c&l鬼げぇむ &7- commands\n&e/onigame join|leave / skills &7参加・スキルUI\n&e/onigame role <oni|player|auto> &7次戦の希望陣営\n&e/onigame skill <sprint|invisible|smoke|strike|heal>\n&e/onigame unstuck &7詰み位置から安全な場所へ救済移動");
        if(admin)h.append("\n&e/onigame playertest <on|off|status> &7一般参加者表示・権限の疑似テスト\n&e/onigame mode <normal|duo|escape|tag> &7通常/二人鬼/ESCAPE/鬼ごっこ切替\n&e/onigame duo botmatch [鬼] &7ぷれいやー視点の異種鬼Bot×2 DUO戦\n&e/onigame gmbook / onimenu &7GM本・鬼構成UI\n&e/onigame lobbyall &7オンラインの全員をロビーへ集合\n&e/onigame lobbyarea <pos1|pos2|clear|info> &7ロビー境界設定\n&e/onigame lobbyblocks <material|preview|restore|status> &7試合中だけ消えるロビー専用ブロック\n&e/onigame ally <preset|list|status|clear> &7開始時の友軍NPCを設定\n&e/onigame arenasetup [info|clear] &7通常試合の200x200区域を一括設定\n&e/onigame automap maxy [Y] &7自動配置要素の最大高度\n&e/onigame exlock <azakuji|jakutsuki|onigami> <lock|unlock|status> &7EX解禁管理\n&e/onigame onilock <dakko|kiou|shikki|yuuki> <lock|unlock|status> &7通常鬼ロック管理\n&e/onigame sp <add|remove|set|reset|check> &7SPテスト（ランキング対象外）\n&e/onigame tag <start|stop|setup|shrine|oniwait|parkour|respawn|area|spranking|spshop|terra|oni|streamer> &7鬼ごっこ管理/SP設備\n&e/onigame spshop add <skill|item> <ID> <price> [uses] &7商品追加\n&e/onigame skillchest / onichest &7設定チェスト登録\n&e/onigame guide <rules|refresh|clear> &7ロビー説明表示\n&e/onigame forceoni <player|clear> / oniblock [clear] &7鬼指定\n&e/onigame practicechest / practiceheart &7練習設定\n&e/onigame training <pos1|pos2|clear|info|dummy> &7練習場管理\n&e/onigame ikimono <on|off|status|spawn|clear> &7異獣管理\n&e/onigame azakuji <on|off|status|chance 0-100> &7字九字参戦管理\n&e/onigame finaltest [clear] / bgmtest / bgmstop &7演出テスト\n&e/onigame botgame / onibotgame / aibotmatch &7Botテスト\n&e/onigame set <lobby|player|oni|exit|exit2> / heart / chest &7マップ設定\n&e/onigame start [鬼] / stop / status &7ゲーム管理");
        s.sendMessage(cc(h.toString()));
    }

    private boolean isEscapeMode(){return getConfig().getString("game-mode","normal").equalsIgnoreCase("escape");}
    private String normalizeTerraGem(String raw){if(raw==null)return null;return switch(raw.toLowerCase(Locale.ROOT)){case "diamond","dia","ダイヤ"->"diamond";case "gold","金"->"gold";case "emerald","eme","エメラルド"->"emerald";case "lapis","lapis_lazuli","ラピス","ラピスラズリ"->"lapis";default->null;};}
    private Material terraGemMaterial(String gem){return switch(gem){case "diamond"->Material.DIAMOND_BLOCK;case "gold"->Material.GOLD_BLOCK;case "emerald"->Material.EMERALD_BLOCK;default->Material.LAPIS_BLOCK;};}
    private String terraGemDisplay(String gem){return switch(gem){case "diamond"->"ダイヤの宝石";case "gold"->"金の宝石";case "emerald"->"エメラルドの宝石";default->"ラピスラズリの宝石";};}
    private Color terraGemColor(String gem){return switch(gem){case "diamond"->Color.fromRGB(85,235,235);case "gold"->Color.fromRGB(255,205,35);case "emerald"->Color.fromRGB(35,220,90);default->Color.fromRGB(45,85,230);};}
    private String terraGemConfigSummary(){List<String> out=new ArrayList<>();for(String g:List.of("diamond","gold","emerald","lapis"))out.add(terraGemDisplay(g)+"="+(LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+g)==null?"未設定":"設定済"));return String.join(" / ",out);}
    private Location terraGate(int i){return LocationStore.get(getConfig(),"tag-mode.maps.terra.gates."+i);}
    private int terraGateCount(){int n=0;for(int i=1;i<=3;i++)if(terraGate(i)!=null)n++;return n;}
    private void renderTerraGates(){if(!tagTerraGatesOpened)return;for(int i=1;i<=3;i++){Location l=terraGate(i);if(l==null||l.getWorld()==null)continue;Location c=l.clone().add(.5,.2,.5);for(int y=0;y<16;y++)l.getWorld().spawnParticle(Particle.END_ROD,c.clone().add(0,y*.32,0),1,.16,.05,.16,.01);for(int k=0;k<16;k++){double a=Math.PI*2*k/16.0;l.getWorld().spawnParticle(Particle.PORTAL,c.clone().add(Math.cos(a)*2.0,1.0,Math.sin(a)*2.0),1,0,0,0,0);}}}
    private void openTerraGates(){if(tagTerraGatesOpened)return;tagTerraGatesOpened=true;all("&d&l脱出ゲートが解放された―― &f力の込められたお札を持って向かえ。");for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)){p.playSound(p.getLocation(),Sound.BLOCK_END_PORTAL_SPAWN,1.0f,.75f);p.sendTitle(cc("&d&l脱出ゲート解放"),cc("&f三つのゲートが開いた"),10,45,15);}}for(int i=1;i<=3;i++){Location l=terraGate(i);if(l!=null&&l.getWorld()!=null){l.getWorld().spawnParticle(Particle.EXPLOSION_HUGE,l.clone().add(.5,1,.5),1);l.getWorld().playSound(l,Sound.BLOCK_END_PORTAL_SPAWN,1.5f,.7f);}}}
    private Location nearbyOpenTerraGate(Player p){if(!tagTerraGatesOpened||p==null)return null;Location at=p.getLocation();for(int i=1;i<=3;i++){Location g=terraGate(i);if(g==null||g.getWorld()==null||!g.getWorld().equals(at.getWorld()))continue;double dx=at.getX()-(g.getX()+.5),dz=at.getZ()-(g.getZ()+.5),dy=Math.abs(at.getY()-g.getY());/* Gate registration is a map anchor, not an exact interaction block.  Use a horizontal 5-block radius and tolerate the vertical size of a built gate. */if(dx*dx+dz*dz<=25.0&&dy<=6.0)return g;}return null;}
    private boolean hasTerraTalisman(Player p){if(p==null)return false;for(ItemStack it:p.getInventory().getContents())if("tag_terra_talisman".equals(actionOf(it)))return true;return false;}
    private void updateTerraGateSneakEscape(Player p){UUID id=p.getUniqueId();if(escaped.contains(id)||dead.contains(id)||tagParkourPlayers.contains(id)){tagTerraGateSneakTicks.remove(id);return;}Location gate=nearbyOpenTerraGate(p);int power=tagTerraGems.getOrDefault(id,new LinkedHashSet<>()).size();boolean talisman=hasTerraTalisman(p);if(gate==null){tagTerraGateSneakTicks.remove(id);return;}if(power<4){tagTerraGateSneakTicks.remove(id);p.sendActionBar(cc("&c霊力が足りない &7"+power+"/4"));return;}if(!talisman){tagTerraGateSneakTicks.remove(id);giveTerraTalisman(p);p.sendActionBar(cc("&e力の込められたお札を再発行した。 &7ゲート内で5秒間スニーク"));return;}if(!p.isSneaking()){if(tagTerraGateSneakTicks.remove(id)!=null)p.sendActionBar(cc("&7脱出詠唱が中断された。 &fゲート内で5秒間スニーク"));else p.sendActionBar(cc("&d脱出可能 &7―― &fここで5秒間スニーク"));return;}int ticks=Math.min(100,tagTerraGateSneakTicks.getOrDefault(id,0)+5);tagTerraGateSneakTicks.put(id,ticks);double sec=(100-ticks)/20.0;int bars=Math.min(10,(int)Math.ceil(ticks/10.0));p.sendActionBar(cc("&d脱出詠唱 &f["+"■".repeat(bars)+"□".repeat(10-bars)+"] &e"+String.format(Locale.ROOT,"%.1f",Math.max(0,sec))+"秒"));if(ticks%20==0)p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,.65f,1.0f+(ticks/100f)*.5f);if(ticks>=100){tagTerraGateSneakTicks.remove(id);completeTerraEscape(p);}}
    private boolean completeTerraEscape(Player p){UUID id=p.getUniqueId();if(escaped.contains(id)||dead.contains(id)||nearbyOpenTerraGate(p)==null||tagTerraGems.getOrDefault(id,new LinkedHashSet<>()).size()<4||!hasTerraTalisman(p))return false;escaped.add(id);p.getWorld().spawnParticle(Particle.TOTEM,p.getLocation().clone().add(0,1,0),70,.7,1.0,.7,.12);p.playSound(p.getLocation(),Sound.ITEM_TOTEM_USE,1.0f,1.0f);p.sendTitle(cc("&f&l脱 出"),cc("&dTerraから生還した"),5,45,15);p.setGameMode(GameMode.SPECTATOR);all("&f"+p.getName()+" &bがTerraから脱出した！");if(players.stream().allMatch(x->escaped.contains(x)||dead.contains(x)))end(false,"Terra終了――脱出 "+escaped.size()+"人");return true;}
    private String terraGemAt(Block b){if(b==null)return null;String key=LocationStore.encode(b.getLocation());for(String g:List.of("diamond","gold","emerald","lapis")){Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+g);if(l!=null&&key.equals(LocationStore.encode(l)))return g;}return null;}
    private void renderTerraGems(){if(!getConfig().getString("tag-mode.map","terra").equalsIgnoreCase("terra"))return;double t=(System.currentTimeMillis()%4000L)/4000.0*Math.PI*2;for(String g:List.of("diamond","gold","emerald","lapis")){Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+g);if(l==null||l.getWorld()==null)continue;Location c=l.clone().add(.5,.65,.5);Particle.DustOptions dust=new Particle.DustOptions(terraGemColor(g),1.25f);for(int i=0;i<8;i++){double a=t+i*Math.PI/4;l.getWorld().spawnParticle(Particle.REDSTONE,c.clone().add(Math.cos(a)*.72,(i%2)*.14,Math.sin(a)*.72),1,0,0,0,0,dust);}for(int y=0;y<5;y++)l.getWorld().spawnParticle(Particle.REDSTONE,c.clone().add(0,y*.28,0),1,.05,.02,.05,0,dust);if(((System.currentTimeMillis()/250)%4)==0)l.getWorld().spawnParticle(Particle.END_ROD,c.clone().add(0,.45,0),2,.28,.25,.28,.01);}}
    private void discoverTerraGems(Player p){if(p==null||!getConfig().getString("tag-mode.map","terra").equalsIgnoreCase("terra")||escaped.contains(p.getUniqueId()))return;UUID id=p.getUniqueId();LinkedHashSet<String> found=tagTerraDiscoveredGems.computeIfAbsent(id,k->new LinkedHashSet<>());for(String gem:List.of("diamond","gold","emerald","lapis")){if(found.contains(gem))continue;Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);if(l==null||l.getWorld()==null||!l.getWorld().equals(p.getWorld()))continue;Location center=l.clone().add(.5,.5,.5);if(p.getLocation().distanceSquared(center)>2500.0)continue;found.add(gem);awardSoulPoints(p,1,"宝石発見");p.sendActionBar(cc("&e◆ &f"+terraGemDisplay(gem)+"を発見 &8- &b+1 SP"));p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,.7f,1.45f);Particle.DustOptions dust=new Particle.DustOptions(terraGemColor(gem),1.15f);p.spawnParticle(Particle.REDSTONE,p.getLocation().clone().add(0,1.2,0),12,.45,.55,.45,0,dust);}}
    private double terraGemProgress(UUID id,String gem){return tagTerraGemProgress.getOrDefault(id,Collections.emptyMap()).getOrDefault(gem,0.0);}
    private void setTerraGemProgress(UUID id,String gem,double value){tagTerraGemProgress.computeIfAbsent(id,k->new HashMap<>()).put(gem,Math.max(0,Math.min(100,value)));}
    private void startTerraGemAttunement(Player p,String gem){UUID id=p.getUniqueId();if(!players.contains(id)||dead.contains(id)||escaped.contains(id)||tagParkourPlayers.contains(id))return;LinkedHashSet<String> got=tagTerraGems.computeIfAbsent(id,k->new LinkedHashSet<>());if(got.contains(gem)){p.sendActionBar(cc("&7"+terraGemDisplay(gem)+"の霊力は取得済み。 &f"+got.size()+"&7/4"));return;}String current=tagTerraAttuningGem.get(id);if(gem.equals(current)){stopTerraGemAttunement(p,"&7霊力同調を中断した。",true);return;}if(current!=null)stopTerraGemAttunement(p,null,true);tagTerraAttuningGem.put(id,gem);tagTerraSkillChecks.remove(id);scheduleTerraGemSkillCheck(id,System.currentTimeMillis());p.playSound(p.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.75f,1.15f);p.sendMessage(cc("&d"+terraGemDisplay(gem)+" &7―― "+terraGemTrait(gem)));p.sendActionBar(cc("&d"+terraGemDisplay(gem)+"への霊力同調を開始 &7―― SHIFTでスキルチェック"));}
    private void stopTerraGemAttunement(Player p,String message,boolean retain){UUID id=p.getUniqueId();String gem=tagTerraAttuningGem.remove(id);tagTerraSkillChecks.remove(id);tagTerraNextSkillCheckAt.remove(id);if(gem!=null&&retain){double keep=terraGemProgress(id,gem)*.75;setTerraGemProgress(id,gem,keep);}if(message!=null)p.sendActionBar(cc(message));}
    private long terraGemNormalWindow(String gem){return "gold".equals(gem)?165L:("lapis".equals(gem)?270L:220L);}
    private long terraGemGreatWindow(String gem){return "gold".equals(gem)?55L:("lapis".equals(gem)?105L:90L);}
    private double terraGemBaseGain(String gem){return switch(gem){case "gold"->1.50;case "lapis"->1.38;default->1.67;};}
    private double terraGemSuccessGain(String gem){return switch(gem){case "gold"->3.0;case "lapis"->3.0;default->4.0;};}
    private double terraGemGreatGain(String gem){return switch(gem){case "gold"->16.0;case "lapis"->7.0;default->10.0;};}
    private double terraGemFailLoss(String gem){return "lapis".equals(gem)?6.0:12.0;}
    private String terraGemTrait(String gem){return switch(gem){case "gold"->"&6GREAT高報酬・判定狭め";case "emerald"->"&a同調中は光柱が立ち、鬼に発見されやすい";case "lapis"->"&9判定回数多め・失敗損失小";default->"&b標準型";};}
    private void scheduleTerraGemSkillCheck(UUID id,long now){String gem=tagTerraAttuningGem.get(id);long min="lapis".equals(gem)?1700:2800,max="lapis".equals(gem)?3000:4800;tagTerraNextSkillCheckAt.put(id,now+min+(long)(Math.random()*(max-min)));}
    private void startTerraGemSkillCheck(Player p,long now){String gem=tagTerraAttuningGem.get(p.getUniqueId());long duration="lapis".equals(gem)?1900:2200;double targetRatio=.42+Math.random()*.34;long target=now+(long)(duration*targetRatio);tagTerraSkillChecks.put(p.getUniqueId(),new SkillCheck(now,target,now+duration));p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,.85f,"gold".equals(gem)?1.75f:1.55f);p.sendTitle("",cc("&e&l霊力スキルチェック！ &fSHIFT"),0,14,0);}
    private void showTerraGemSkillCheck(Player p,SkillCheck c,long now){String gem=tagTerraAttuningGem.get(p.getUniqueId());double ratio=Math.max(0,Math.min(1,(now-c.startedAt())/(double)Math.max(1,c.endsAt()-c.startedAt())));int pos=Math.min(19,(int)Math.floor(ratio*20));double target=(c.targetAt()-c.startedAt())/(double)Math.max(1,c.endsAt()-c.startedAt());int great=Math.min(19,Math.max(0,(int)Math.floor(target*20)));int normalCells="gold".equals(gem)?1:("lapis".equals(gem)?3:2);StringBuilder bar=new StringBuilder("&8[");for(int i=0;i<20;i++){if(i==pos)bar.append("&f┃");else if(i==great)bar.append("&6◆");else if(Math.abs(i-great)<=normalCells)bar.append("&a■");else bar.append("&7■");}bar.append("&8] &eSHIFT");p.sendActionBar(cc(bar.toString()));}
    private void resolveTerraGemSkillCheck(Player p){UUID id=p.getUniqueId();SkillCheck c=tagTerraSkillChecks.get(id);String gem=tagTerraAttuningGem.get(id);if(c==null||gem==null)return;long now=System.currentTimeMillis(),window=terraGemNormalWindow(gem),greatWindow=terraGemGreatWindow(gem);long normalStart=c.targetAt()-window,greatStart=c.targetAt()-greatWindow,greatEnd=c.targetAt()+greatWindow,normalEnd=c.targetAt()+window;if(now>=greatStart&&now<=greatEnd){tagTerraSkillChecks.remove(id);scheduleTerraGemSkillCheck(id,now);setTerraGemProgress(id,gem,terraGemProgress(id,gem)+terraGemGreatGain(gem));p.sendTitle("",cc("&6&lGREAT! &d霊力同調加速"),0,16,5);p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.8f,1.7f);recordTagResonance(true,false,gem);}else if(now>=normalStart&&now<=normalEnd){tagTerraSkillChecks.remove(id);scheduleTerraGemSkillCheck(id,now);setTerraGemProgress(id,gem,terraGemProgress(id,gem)+terraGemSuccessGain(gem));p.sendTitle("",cc("&a&l成功"),0,14,4);p.playSound(p.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.8f,1.55f);}else failTerraGemSkillCheck(p,gem,"タイミング失敗");}
    private void failTerraGemSkillCheck(Player p,String gem,String reason){UUID id=p.getUniqueId();recordTagResonance(false,false,gem);tagTerraSkillChecks.remove(id);scheduleTerraGemSkillCheck(id,System.currentTimeMillis());setTerraGemProgress(id,gem,terraGemProgress(id,gem)-terraGemFailLoss(gem));Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);p.sendTitle(cc("&4&l失敗"),cc("&c"+reason),0,22,7);if(l!=null&&l.getWorld()!=null){l.getWorld().playSound(l,Sound.ENTITY_GENERIC_EXPLODE,1.25f,.75f);l.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,l.clone().add(.5,.8,.5),2);Particle.DustOptions dust=new Particle.DustOptions(terraGemColor(gem),1.5f);l.getWorld().spawnParticle(Particle.REDSTONE,l.clone().add(.5,.8,.5),24,.8,.6,.8,0,dust);}}
    private void updateTerraGemAttunements(){if(!isTagMode()||state!=GameState.RUNNING)return;long now=System.currentTimeMillis();for(UUID id:new ArrayList<>(tagTerraAttuningGem.keySet())){Player p=Bukkit.getPlayer(id);String gem=tagTerraAttuningGem.get(id);Location l=gem==null?null:LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);if(p==null||gem==null||l==null||l.getWorld()==null||!l.getWorld().equals(p.getWorld())||dead.contains(id)||escaped.contains(id)||tagParkourPlayers.contains(id)||p.getLocation().distanceSquared(l.clone().add(.5,.5,.5))>25.0){if(p!=null)stopTerraGemAttunement(p,"&c宝石から離れ、霊力同調が中断された。 &7進行の75%を保持",true);else{tagTerraAttuningGem.remove(id);tagTerraSkillChecks.remove(id);tagTerraNextSkillCheckAt.remove(id);}continue;}if(tagTerraGems.getOrDefault(id,new LinkedHashSet<>()).contains(gem)){stopTerraGemAttunement(p,null,false);continue;}SkillCheck c=tagTerraSkillChecks.get(id);if(c!=null){if(now>c.endsAt()){failTerraGemSkillCheck(p,gem,"時間切れ");continue;}showTerraGemSkillCheck(p,c,now);}else if(now>=tagTerraNextSkillCheckAt.getOrDefault(id,Long.MAX_VALUE)){startTerraGemSkillCheck(p,now);continue;}double base=terraGemBaseGain(gem);double progress=terraGemProgress(id,gem)+(System.currentTimeMillis()<tagReimyakuUntil?base*1.5:base);setTerraGemProgress(id,gem,progress);if("emerald".equals(gem)&&l.getWorld()!=null){Location beam=l.clone().add(.5,1,.5);Particle.DustOptions ed=new Particle.DustOptions(terraGemColor(gem),1.35f);for(int y=0;y<=24;y+=2)l.getWorld().spawnParticle(Particle.REDSTONE,beam.clone().add(0,y*.5,0),2,.10,.05,.10,0,ed);if((now/500)%2==0)l.getWorld().spawnParticle(Particle.END_ROD,beam.clone().add(0,6,0),4,.18,3.0,.18,.01);}int pct=(int)Math.round(Math.min(100,progress));if(!tagTerraSkillChecks.containsKey(id))p.sendActionBar(cc("&d霊力同調 &f"+"■".repeat(Math.min(10,pct/10))+"&8"+"□".repeat(Math.max(0,10-pct/10))+" &f"+pct+"% &7―― SHIFT判定に備えろ"));Particle.DustOptions dust=new Particle.DustOptions(terraGemColor(gem),1.15f);Vector d=p.getLocation().clone().add(0,1,0).toVector().subtract(l.clone().add(.5,.8,.5).toVector());Location q=l.clone().add(.5,.8,.5).add(d.multiply(Math.random()));l.getWorld().spawnParticle(Particle.REDSTONE,q,2,.05,.05,.05,0,dust);if(progress>=100){stopTerraGemAttunement(p,null,false);setTerraGemProgress(id,gem,100);collectTerraGem(p,gem);}}}
    private void collectTerraGem(Player p,String gem){UUID id=p.getUniqueId();if(!players.contains(id)||dead.contains(id)||escaped.contains(id)||tagParkourPlayers.contains(id))return;LinkedHashSet<String> got=tagTerraGems.computeIfAbsent(id,k->new LinkedHashSet<>());if(got.contains(gem)){p.sendActionBar(cc("&7"+terraGemDisplay(gem)+"の霊力は取得済み。 &f"+got.size()+"&7/4"));return;}got.add(gem);Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);Color col=terraGemColor(gem);Particle.DustOptions dust=new Particle.DustOptions(col,1.45f);if(l!=null&&l.getWorld()!=null){Location from=l.clone().add(.5,1,.5),to=p.getLocation().clone().add(0,1,0);Vector d=to.toVector().subtract(from.toVector());int n=Math.max(6,(int)(d.length()*2));for(int i=0;i<=n;i++){Location q=from.clone().add(d.clone().multiply(i/(double)n));l.getWorld().spawnParticle(Particle.REDSTONE,q,2,.06,.06,.06,0,dust);}l.getWorld().spawnParticle(Particle.END_ROD,to,18,.35,.55,.35,.04);}p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,1.0f,1.25f);p.sendActionBar(cc("&d霊力を獲得した &f"+got.size()+"&7/4 &8- &f"+terraGemDisplay(gem)));giveTerraTalisman(p);awardSoulPoints(p,2,"宝石の霊力獲得");if(got.size()>=4){p.playSound(p.getLocation(),Sound.ITEM_TOTEM_USE,.8f,1.2f);p.sendTitle(cc("&d&lお札に力が満ちた"),cc("&f霊力 4/4 &7―― ゲート解放を待て"),5,35,10);}}
    private ItemStack terraTalisman(UUID id){int power=Math.min(4,tagTerraGems.getOrDefault(id,new LinkedHashSet<>()).size());ItemStack it=new ItemStack(Material.PAPER);ItemMeta m=it.getItemMeta();m.setDisplayName(cc(power>=4?"&d&l力の込められたお札":"&f&l封印札"));m.setLore(List.of(cc("&7Terra 脱出用のお札"),cc("&d霊力: &f"+"■".repeat(power)+"&8"+"□".repeat(4-power)+" &7"+power+"/4"),cc(power>=4?"&aゲート解放後に使用可能":"&7四つの宝石から霊力を集めよ")));m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"tag_terra_talisman");it.setItemMeta(m);return it;}
    private void giveTerraTalisman(Player p){if(p==null)return;for(int i=0;i<p.getInventory().getSize();i++){ItemStack old=p.getInventory().getItem(i);if("tag_terra_talisman".equals(actionOf(old))){p.getInventory().setItem(i,terraTalisman(p.getUniqueId()));return;}}int slot=p.getInventory().firstEmpty();if(slot>=0)p.getInventory().setItem(slot,terraTalisman(p.getUniqueId()));else p.getInventory().setItem(7,terraTalisman(p.getUniqueId()));}

    private boolean isTagMode(){return getConfig().getString("game-mode","normal").equalsIgnoreCase("tag");}
    private boolean isPvMode(){return getConfig().getString("game-mode","normal").equalsIgnoreCase("pv");}
    private List<String> getEscapeRegionIds(){var sec=getConfig().getConfigurationSection("escape.regions");return sec==null?new ArrayList<>():new ArrayList<>(sec.getKeys(false));}
    private void setupTagPlayer(Player p,Location shrine,int index){
        common(p);playerHitGraceUntil.remove(p.getUniqueId());downEscapeUntil.remove(p.getUniqueId());p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10.0);p.setHealth(10.0);p.setFallDistance(0);p.getInventory().clear();
        double ang=(Math.PI*2.0*(index%12))/12.0,rad=index<12?2.0:4.0;Location at=shrine.clone().add(Math.cos(ang)*rad,0,Math.sin(ang)*rad);p.teleport(at);soulPoints.put(p.getUniqueId(),0);matchEarnedSoulPoints.put(p.getUniqueId(),0);tagDiscoveredAreas.put(p.getUniqueId(),new LinkedHashSet<>());tagSelectedArea.remove(p.getUniqueId());p.getInventory().setItem(8,tagNavCompass());
    }
    private void startTag(CommandSender starter,boolean force){
        if(state!=GameState.WAITING){msg(starter,"&cすでに試合中です。");return;}
        Location shrine=LocationStore.get(getConfig(),"tag-mode.shrine.player"),oniShrine=LocationStore.get(getConfig(),"tag-mode.shrine.oni");
        if(shrine==null||oniShrine==null){msg(starter,"&c先に /og tag shrine player と /og tag shrine oni を設定してください。");return;}
        participants.removeIf(id->Bukkit.getPlayer(id)==null);pruneUneiParticipants();if(participants.isEmpty()&&starter instanceof Player p&&uneiCanPlay(p))participants.add(p.getUniqueId());if(participants.isEmpty()){msg(starter,"&c参加者がいません。");return;}
        resetRuntime();oni=null;oniTeam.clear();players.clear();tagTerraGatesOpened=false;randomizeTagSpShops();oniType=selectedOniType!=null&&!isOniTypeLocked(selectedOniType)?selectedOniType:firstUnlockedOni();List<UUID> ids=new ArrayList<>(participants);Collections.shuffle(ids);
        UUID humanOni=null;if(forcedOni!=null&&ids.contains(forcedOni))humanOni=forcedOni;else if(ids.size()>1){for(UUID id:ids)if("oni".equals(rolePreferences.get(id))){humanOni=id;break;}if(humanOni==null&&!force)for(UUID id:ids)if(!"player".equals(rolePreferences.get(id))){humanOni=id;break;}}
        if(humanOni!=null){oni=humanOni;oniTeam.add(humanOni);ids.remove(humanOni);}players.addAll(ids);for(UUID id:players)tagLives.put(id,tagMaxLives(id));activePlayerSpawn=shrine;activeLobby=LocationStore.get(getConfig(),"locations.lobby");secondsLeft=Math.max(60,getConfig().getInt("tag-mode.game-seconds",1200));tagInitialSeconds=secondsLeft;tagNextRandomEventAt=Math.max(360,secondsLeft-150);tagHyakkiTriggered=false;tagHyakkiUntil=0L;tagReimyakuUntil=0L;tagSoulRushUntil=0L;resetTagResonance();state=GameState.RUNNING;applyFinalSkyTime(shrine.getWorld());hideLobbyUtilities();setTagShopVisibility(true);setTerraGemVisibility(true);Location tc=LocationStore.get(getConfig(),"tag-mode.center");if(tc!=null&&tc.getWorld()!=null){WorldBorder wb=tc.getWorld().getWorldBorder();wb.setCenter(tc.getX(),tc.getZ());wb.setSize(Math.max(16,getConfig().getDouble("tag-mode.border-size",200.0)));}
        int i=0;for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null){setupTagPlayer(p,shrine,i++);tagTerraGems.put(id,new LinkedHashSet<>());tagTerraDiscoveredGems.put(id,new LinkedHashSet<>());giveTerraTalisman(p);}}int target=getPlayerBotTargetCount();int added=fillPlayerBotsToTarget(shrine,target);
        spawnTagSkillChests(shrine);
        if(humanOni!=null){Player op=Bukkit.getPlayer(humanOni);if(op!=null){common(op);Location wait=LocationStore.get(getConfig(),"tag-mode.oni-wait");if(wait==null)wait=activeLobby!=null?activeLobby:oniShrine;op.teleport(wait);op.setGameMode(GameMode.SPECTATOR);msg(op,"&4鬼はまだ解放されていない。 &7解放まで待機してください……");}}
        int delay=Math.max(0,getConfig().getInt("tag-mode.oni-release-seconds",45));all("&d&l鬼ごっこ開始！ &f神社から散開せよ。 &7鬼は一定時間後に現れる……");
        final UUID releaseHuman=humanOni;tagReleaseTask=Bukkit.getScheduler().runTaskLater(this,()->{if(state!=GameState.RUNNING||!isTagMode())return;oniPowerActivated=true;if(releaseHuman!=null){Player op=Bukkit.getPlayer(releaseHuman);if(op!=null){setupOni(op,oniShrine);op.setWalkSpeed((float)Math.min(1.0,getConfig().getDouble("oni-walk-speed",.255)*1.22));renderEmpoweredOniSkill(op);}}else{spawnOniBot(oniShrine);if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity ob&&ob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)ob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",.32)*1.22);LivingEntity ob=getOniEntity();if(ob!=null)renderEmpoweredOniSkill(ob);}all("&4&l鬼が現れた―― &c能力全解放・狂化");for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null)p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,.75f,.65f);}},delay*20L);
        if(added>0)all("&bぷれいやーBot "+added+"体 &fを追加し、ぷれいやー陣営を"+target+"枠まで補充しました。");ticker=Bukkit.getScheduler().runTaskTimer(this,this::tickTag,20,20);
    }
    private void tickTag(){if(state!=GameState.RUNNING||!isTagMode())return;secondsLeft--;tickTagBroadcastEvents();if(!tagTerraGatesOpened&&secondsLeft<=300)openTerraGates();int fs=Math.max(0,getConfig().getInt("tag-mode.final-seconds",180));if(!finalPhase&&secondsLeft<=fs){finalPhase=true;tagParkourGrandfathered.addAll(tagParkourPlayers);all("&4&l【最終フェーズ】 &cこれ以降、倒れた者は復活できない。");}updateLateGameOniPower();if(secondsLeft<=0)end(escaped.isEmpty(),escaped.isEmpty()?"Terra時間切れ――鬼の勝利":"Terra終了――脱出 "+escaped.size()+"人");}

    private void tickTagBroadcastEvents(){
        long now=System.currentTimeMillis();
        int hyakkiAt=Math.max(360,tagInitialSeconds/2);
        if(!tagHyakkiTriggered&&secondsLeft<=hyakkiAt&&secondsLeft>300){startTagHyakki();}
        if(tagHyakkiUntil>0&&now>=tagHyakkiUntil){tagHyakkiUntil=0;all("&f&l百鬼夜行を凌いだ―― &d生存者 +3 SP");int alive=0,total=0;for(UUID id:players){if(playerBots.contains(id))continue;total++;Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!tagParkourPlayers.contains(id)&&!escaped.contains(id)){alive++;awardSoulPoints(p,3,"百鬼夜行生存");}}if(total>0&&alive>=Math.max(1,(int)Math.ceil(total*.65)))triggerTagSurvivalResonance();}
        if(tagNextRandomEventAt>300&&secondsLeft<=tagNextRandomEventAt&&!tagHyakkiTriggered){startTagRandomEvent();tagNextRandomEventAt=secondsLeft-(120+new Random().nextInt(61));}
        if(tagNextRandomEventAt>300&&secondsLeft<=tagNextRandomEventAt&&tagHyakkiUntil==0){startTagRandomEvent();tagNextRandomEventAt=secondsLeft-(120+new Random().nextInt(61));}
    }
    private void startTagHyakki(){tagHyakkiTriggered=true;tagHyakkiUntil=System.currentTimeMillis()+45000L;for(Player p:Bukkit.getOnlinePlayers()){p.sendTitle(cc("&4&l百鬼夜行"),"",10,50,10);p.sendMessage(cc("&c目的を捨てて、生き延びろ。"));p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,1f,.55f);}LivingEntity hunter=getOniEntity();if(hunter!=null)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,20*45,1,false,true));}
    private void resetTagResonance(){tagResonanceGreat=0;tagResonanceFail=0;tagResonanceCooldownUntil=0;tagRecentGreats.clear();tagRecentFails.clear();}
    private int tagResonancePopulation(){int n=0;for(UUID id:players)if(!playerBots.contains(id))n++;return Math.max(1,n);}
    private void recordTagResonance(boolean great,boolean bot,String gem){
        if(!isTagMode()||state!=GameState.RUNNING||secondsLeft<=300)return;long now=System.currentTimeMillis();double w=bot?.5:1.0;Deque<Long> q=great?tagRecentGreats:tagRecentFails;q.addLast(now);while(!q.isEmpty()&&q.peekFirst()<now-12000L)q.removeFirst();
        if(great)tagResonanceGreat+=w;else tagResonanceFail+=w;int pop=tagResonancePopulation();double threshold=Math.max(4.0,pop*.55);
        if(now>=tagResonanceCooldownUntil&&great&&q.size()>=Math.max(4,(int)Math.ceil(pop*.30))){triggerTagPerfectResonance();q.clear();return;}
        if(now>=tagResonanceCooldownUntil&&!great&&q.size()>=Math.max(4,(int)Math.ceil(pop*.30))){triggerTagCalamityResonance(gem);q.clear();return;}
        if(now>=tagResonanceCooldownUntil&&tagResonanceGreat>=threshold){tagResonanceGreat=Math.max(0,tagResonanceGreat-threshold);tagResonanceCooldownUntil=now+45000L;all("&b&l【霊脈共鳴】 &f皆のGREATが霊脈を揺らした――30秒間、同調加速！");tagReimyakuUntil=Math.max(tagReimyakuUntil,now+30000L);for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id))awardSoulPoints(p,1,"霊脈共鳴");}}
        else if(now>=tagResonanceCooldownUntil&&tagResonanceFail>=threshold){tagResonanceFail=Math.max(0,tagResonanceFail-threshold);tagResonanceCooldownUntil=now+45000L;all("&4&l【鬼気増幅】 &c失敗の気配が鬼を強める……");LivingEntity h=getOniEntity();if(h!=null){h.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,20*30,0,false,true));h.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,20*30,0,false,true));}}
    }
    private void triggerTagPerfectResonance(){long now=System.currentTimeMillis();tagResonanceCooldownUntil=now+45000L;all("&f&l【完全共鳴】 &b連続GREAT――進行中の霊力同調が一斉に進む！");for(Map.Entry<UUID,String> e:new ArrayList<>(tagTerraAttuningGem.entrySet()))setTerraGemProgress(e.getKey(),e.getValue(),terraGemProgress(e.getKey(),e.getValue())+8.0);}
    private void triggerTagCalamityResonance(String gem){tagResonanceCooldownUntil=System.currentTimeMillis()+45000L;all("&5&l【禍津共鳴】 &c失敗が連鎖し、鬼へ宝石の方角が伝わった……");Location gl=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);LivingEntity h=getOniEntity();if(gl!=null&&h instanceof Player op){Vector d=gl.toVector().subtract(h.getLocation().toVector());String dir=Math.abs(d.getX())>Math.abs(d.getZ())?(d.getX()>0?"東":"西"):(d.getZ()>0?"南":"北");op.sendMessage(cc("&5禍津共鳴 &7―― &f"+terraGemDisplay(gem)+" &cは"+dir+"の方角"));}if(gl!=null&&gl.getWorld()!=null){gl.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,gl.clone().add(.5,1,.5),80,1.2,2.5,1.2,.04);gl.getWorld().playSound(gl,Sound.ENTITY_WARDEN_SONIC_BOOM,1f,.7f);}}
    private void triggerTagSurvivalResonance(){all("&a&l【生存共鳴】 &f百鬼夜行を大勢で凌いだ―― &d全員 +2 SP");for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!escaped.contains(id))awardSoulPoints(p,2,"生存共鳴");}}

    private void startTagRandomEvent(){
        int roll=new Random().nextInt(4);long now=System.currentTimeMillis();
        if(roll==0){tagReimyakuUntil=now+60000L;all("&b&l【霊脈活性】 &f60秒間、宝石の霊力同調速度が上昇！");for(String gem:List.of("diamond","gold","emerald","lapis")){Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);if(l!=null&&l.getWorld()!=null)l.getWorld().spawnParticle(Particle.END_ROD,l.clone().add(.5,1,.5),70,.8,4,.8,.08);}}
        else if(roll==1){tagSoulRushUntil=now+60000L;all("&d&l【魂の共鳴】 &f60秒間、獲得SPが2倍！");}
        else if(roll==2){all("&e&l【物資投下】 &fTAGスキルチェストが追加出現した！");Location shrine=activePlayerSpawn;if(shrine!=null)spawnTagSkillChests(shrine);}
        else {all("&8&l【濃霧】 &760秒間、Terraが濃い霧に包まれる……");for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id))p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,20*60,0,false,false));}}
        for(Player p:Bukkit.getOnlinePlayers())p.playSound(p.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.7f,.8f);
    }

    private void startEscape(OniType forced,CommandSender starter){
        participants.removeIf(id->Bukkit.getPlayer(id)==null);pruneUneiParticipants();if(participants.isEmpty()&&starter instanceof Player p&&uneiCanPlay(p))participants.add(p.getUniqueId());
        Location ps=LocationStore.get(getConfig(),"locations.player-spawn"),os=LocationStore.get(getConfig(),"locations.oni-spawn"),ex=LocationStore.get(getConfig(),"locations.exit"),ex2=LocationStore.get(getConfig(),"locations.exit2");
        if(ps==null||os==null){msg(starter,"&cESCAPE開始には player / oni の開始地点が必要です。");return;}
        OniType upcoming=forced!=null?forced:(selectedOniType!=null?selectedOniType:OniType.DAKKO);resetRuntime();oniType=upcoming;
        List<UUID> ids=new ArrayList<>(participants);Collections.shuffle(ids);int humans=ids.size();boolean soloEscapeTest=humans==1;int oniCount=humans>=getConfig().getInt("escape.onis.three-from-players",23)?3:humans>=getConfig().getInt("escape.onis.two-from-players",15)?2:1;oniCount=Math.min(oniCount,Math.max(1,ids.size()-1));
        List<UUID> chosen=new ArrayList<>();if(!soloEscapeTest){if(forcedOni!=null&&ids.contains(forcedOni))chosen.add(forcedOni);for(UUID id:ids)if(chosen.size()<oniCount&&"oni".equals(rolePreferences.get(id))&&!chosen.contains(id))chosen.add(id);for(UUID id:ids)if(chosen.size()<oniCount&&!"player".equals(rolePreferences.get(id))&&!chosen.contains(id))chosen.add(id);for(UUID id:ids)if(chosen.size()<oniCount&&!chosen.contains(id))chosen.add(id);}
        oniTeam.addAll(chosen);oni=chosen.isEmpty()?null:chosen.get(0);ids.removeAll(chosen);players.addAll(ids);activeOniPassives.addAll(selectedOniPassives);activePlayerSpawn=ps;activeExit=ex;activeExit2=ex2;escapePhase=1;escapeEventsCompleted=0;escapeEventsRequired=Math.max(1,getConfig().getInt("escape.required-events",5));secondsLeft=Math.max(60,getConfig().getInt("escape.game-seconds",1800));
        state=GameState.RUNNING;applyGameNight(ps.getWorld());startGameStartBgm();startIkimonoSystem(ps);int oi=0;for(UUID id:oniTeam){Player op=Bukkit.getPlayer(id);if(op!=null)setupOni(op,os.clone().add((oi++)*2,0,0));}if(soloEscapeTest){spawnOniBot(os);msg(starter,"&eESCAPEソロテストのため鬼Botを1体配置しました。");}for(UUID id:players){Player pp=Bukkit.getPlayer(id);if(pp!=null)setupPlayer(pp,ps);}
        hideLobbyUtilities();all("&4&l鬼げぇむ ESCAPE 開始！ &7広大なマップ各地のイベントを攻略せよ。");announceEscapePhase();ticker=Bukkit.getScheduler().runTaskTimer(this,this::tick,20,20);
    }
    private void tickEscape(){secondsLeft--;for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendActionBar(cc("&6ESCAPE &7| &ePhase "+escapePhase+"/5 &7| &a攻略 "+escapeEventsCompleted+"/"+escapeEventsRequired+" &7| &f"+(secondsLeft/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60)));}if(escapePhase>=5){renderExitBeacon();checkEscapeExit();}if(secondsLeft<=0)end(true,"時間切れ――鬼の勝利");}
    private void advanceEscapePhase(){if(escapePhase>=5){all("&7ESCAPEはすでに最終脱出段階です。");return;}escapePhase++;announceEscapePhase();}
    private void announceEscapePhase(){String name=switch(escapePhase){case 1->"異変";case 2->"侵食";case 3->"禍刻";case 4->"祭儀";default->"逃刻";};all("&4&l【ESCAPE Phase "+escapePhase+"】 &f"+name);for(Player p:Bukkit.getOnlinePlayers())p.sendTitle(cc("&4&lPhase "+escapePhase),cc("&f"+name),10,50,15);if(escapePhase==5){finalPhase=true;secondsLeft=Math.min(secondsLeft,Math.max(60,getConfig().getInt("escape.final-seconds",240)));all("&c&l脱出口が開いた。逃げろ。");}}
    private void checkEscapeExit(){if(activeExit==null&&activeExit2==null)return;double r=Math.max(.5,getConfig().getDouble("exit-radius",3.0)),r2=r*r;for(UUID id:new HashSet<>(players)){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null)continue;boolean hit=(activeExit!=null&&p.getWorld().equals(activeExit.getWorld())&&p.getLocation().distanceSquared(activeExit)<=r2)||(activeExit2!=null&&p.getWorld().equals(activeExit2.getWorld())&&p.getLocation().distanceSquared(activeExit2)<=r2);if(hit){escaped.add(id);p.setGameMode(GameMode.SPECTATOR);all("&f"+p.getName()+" &aが脱出した！");}}if(!players.isEmpty()&&players.stream().allMatch(id->escaped.contains(id)||dead.contains(id)))end(false,"ESCAPE終了――脱出 "+escaped.size()+"人");}
    private void updateEscapeSidebar(){Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ ESCAPE-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);int sc=10;o.getScore(cc("&ePhase &f"+escapePhase+"&7/5")).setScore(sc--);String name=switch(escapePhase){case 1->"異変";case 2->"侵食";case 3->"禍刻";case 4->"祭儀";default->"逃刻";};o.getScore(cc("&c"+name)).setScore(sc--);o.getScore(cc("&8──────────")).setScore(sc--);o.getScore(cc("&a広域イベント &f"+escapeEventsCompleted+"&7/&f"+escapeEventsRequired)).setScore(sc--);o.getScore(cc("&b登録地域 &f"+getEscapeRegionIds().size())).setScore(sc--);o.getScore(cc("&e残り &f"+(Math.max(0,secondsLeft)/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60))).setScore(sc--);o.getScore(cc("&8──────────&0")).setScore(sc--);long alive=players.stream().filter(id->!dead.contains(id)&&!escaped.contains(id)).count();o.getScore(cc("&a行動中 &f"+alive)).setScore(sc--);o.getScore(cc("&f脱出 &a"+escaped.size())).setScore(sc);for(Player v:Bukkit.getOnlinePlayers())v.setScoreboard(board);}

    private void startPv(CommandSender sender){
        if(state!=GameState.WAITING){msg(sender,"&cすでにゲーム/撮影が進行中です。");return;}
        getConfig().set("game-mode","pv");saveConfig();setTagShopVisibility(false);setTerraGemVisibility(false);resetRuntime();
        List<UUID> ids=new ArrayList<>(participants);for(UUID id:ids){Player p=Bukkit.getPlayer(id);if(p==null)continue;players.add(id);setupPlayer(p,p.getLocation());}
        state=GameState.RUNNING;pvFrozen=false;for(Player p:Bukkit.getOnlinePlayers()){p.getWorld().setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);p.getWorld().setTime(pvFixedTime);}
        all("&d&lPV撮影モード開始 &7―― 時間進行・勝敗判定・Bot自動補充なし");
    }
    private void stopPv(CommandSender sender){
        if(!isPvMode()||state==GameState.WAITING){msg(sender,"&7PV撮影モードは開始されていません。");return;}
        pvFrozen=false;removePlayerBots();removeOniBot();for(UUID id:new HashSet<>(participants)){Player p=Bukkit.getPlayer(id);if(p!=null){p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);common(p);}}
        players.clear();oniTeam.clear();oni=null;state=GameState.WAITING;restoreLobbyUtilities();all("&7PV撮影モードを終了しました。");
    }
    private void start(OniType forced,CommandSender starter,boolean playerTest){
        if(state!=GameState.WAITING){all("すでに試合中です。");return;}
        if(isEscapeMode()&&!playerTest){startEscape(forced,starter);return;}
        participants.removeIf(id->Bukkit.getPlayer(id)==null);pruneUneiParticipants();
        boolean soloTest=participants.isEmpty();
        if(soloTest){if(!(starter instanceof Player gm)){msg(starter,"参加者0人のテスト開始はゲーム内の管理者が実行してください。");return;}if(isUnei(gm)&&!uneiPlayNextMatch.contains(gm.getUniqueId())){msg(gm,"&e運営タグのため自動テスト参加しません。 &7参加する場合は先に /og unei play を実行してください。");return;}testParticipant=gm.getUniqueId();participants.add(testParticipant);msg(gm,playerTest?"&e開始者をぷれいやー、NPCを鬼にしたBotテストを開始します。":"&e参加者0人のため、開始者を鬼にしてBOT補充モードで開始します。");}
        Player testGm=soloTest?Bukkit.getPlayer(testParticipant):null;
        Location ps=LocationStore.get(getConfig(),"locations.player-spawn"), os=LocationStore.get(getConfig(),"locations.oni-spawn"), ex=LocationStore.get(getConfig(),"locations.exit"), ex2=LocationStore.get(getConfig(),"locations.exit2"), lobby=LocationStore.get(getConfig(),"locations.lobby");
        List<OniType> randomPool=unlockedStandardOni();if(randomPool.isEmpty())randomPool=List.of(firstUnlockedOni());OniType upcomingOniType=forced!=null?forced:(selectedOniType!=null&&!isOniTypeLocked(selectedOniType)?selectedOniType:randomPool.get(new Random().nextInt(randomPool.size())));
        oniType=upcomingOniType;
        String placementMode=getConfig().getString("heart-placement-mode","random");
        List<Location> heartCandidates=placementMode.equalsIgnoreCase("manual")?scanHeartMarkers(true):selectRandomHearts();
        Collections.shuffle(heartCandidates);
        boolean duoMode=getConfig().getString("game-mode","normal").equalsIgnoreCase("duo")&&(!playerTest||duoBotMatchStart);
        int requiredRealHearts=oniType==OniType.JAKUTSUKI?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8)):Math.max(1,getConfig().getInt(duoMode?"duo-mode.heart-placement-count":"normal-mode.heart-placement-count",duoMode?14:10));
        int requiredFakeHearts=oniType==OniType.JAKUTSUKI?Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4)):0;
        List<Location> hearts=new ArrayList<>();
        List<Location> fakeHearts=new ArrayList<>();
        if(oniType==OniType.JAKUTSUKI){
            if(heartCandidates.size()>=requiredRealHearts+requiredFakeHearts){
                hearts.addAll(heartCandidates.subList(0,requiredRealHearts));
                fakeHearts.addAll(heartCandidates.subList(requiredRealHearts,requiredRealHearts+requiredFakeHearts));
            }
        }else if(heartCandidates.size()>=requiredRealHearts)hearts.addAll(heartCandidates.subList(0,requiredRealHearts));
        boolean automaticTestHearts=soloTest;
        boolean automaticNormalArenaHearts=!soloTest&&getConfig().getBoolean("arena-setup.enabled",false);
        List<Location> automaticTestLoot=null;
        if(soloTest&&testGm!=null){Location base=testGm.getLocation().clone();TestArenaLayout layout=createRandomTestArena(base);ps=layout.playerSpawn();os=layout.oniSpawn();ex=layout.exit();ex2=layout.exit2();heartCandidates=new ArrayList<>(layout.hearts());Collections.shuffle(heartCandidates);hearts.clear();fakeHearts.clear();if(oniType==OniType.JAKUTSUKI){if(heartCandidates.size()>=requiredRealHearts+requiredFakeHearts){hearts.addAll(heartCandidates.subList(0,requiredRealHearts));fakeHearts.addAll(heartCandidates.subList(requiredRealHearts,requiredRealHearts+requiredFakeHearts));}}else if(heartCandidates.size()>=requiredRealHearts)hearts.addAll(heartCandidates.subList(0,requiredRealHearts));automaticTestLoot=layout.lootChests();lobby=base;msg(testGm,"&a実行地点を中心とした200×200のテスト区域へ、開始地点・心臓・物資・出口A/Bをランダム配置しました。");}
        if(ex!=null&&ex2==null&&getConfig().getBoolean("arena-setup.enabled",false)){
            Location center=LocationStore.get(getConfig(),"arena-setup.center");
            if(center!=null&&center.getWorld()!=null){
                int size=Math.max(40,getConfig().getInt("arena-setup.size",200)),half=size/2-5;
                List<Location> occupied=new ArrayList<>();occupied.add(ex);if(ps!=null)occupied.add(ps);if(os!=null)occupied.add(os);
                ex2=randomTestSurfaceFarFrom(center,new Random(),half,occupied,Math.min(55,size*.275));
                LocationStore.set(getConfig(),"locations.exit2",ex2);saveConfig();
                msg(starter,"&e旧アリーナ設定を検出したため、脱出地点Bを安全な地表へ自動追加しました。");
            }
        }
        if(ps==null||os==null||ex==null||ex2==null||hearts.size()<requiredRealHearts||(oniType==OniType.JAKUTSUKI&&fakeHearts.size()<requiredFakeHearts)){all(oniType==OniType.JAKUTSUKI?"蛇窟姫戦には心臓候補地点が最低 "+(requiredRealHearts+requiredFakeHearts)+" 箇所必要です。（本物"+requiredRealHearts+"＋偽物"+requiredFakeHearts+"）":"開始地点・鬼地点・出口A/B・現在のモードに必要な心臓地点を設定してください。（必要候補: "+requiredRealHearts+"）");if(testParticipant!=null){participants.remove(testParticipant);testParticipant=null;}return;}
        List<Location> lootChests=automaticTestLoot!=null?automaticTestLoot:selectLootChestLocations(hearts,ps);
        resetRuntime(); oniType=upcomingOniType;
        // 字九字ひろはv0.40.30からNPC専用。旧セッションの選択が残っていても使用不可にする。
        for(UUID pid:new HashSet<>(participants))if("AZAKUJI_HIRO".equals(selectedPresetNames.get(pid))){selectedPresetNames.remove(pid);selectedSkill.put(pid,PlayerSkill.SPRINT);LinkedHashSet<PassiveSkill> ps0=selectedPassives.computeIfAbsent(pid,k->new LinkedHashSet<>());ps0.clear();ps0.add(PassiveSkill.LIGHT_FOOTED);ps0.add(PassiveSkill.DEEP_BREATH);Player old=Bukkit.getPlayer(pid);if(old!=null)msg(old,"&6字九字ひろ &fはNPC友軍専用になったため、通常構成へ戻しました。");}
        List<UUID> ids=new ArrayList<>(participants); Collections.shuffle(ids);playerSideTest=playerTest;if(playerTest){oni=null;players.addAll(ids);}else{int oniCount=duoMode?2:1;List<UUID> chosenOnis=new ArrayList<>();if(forcedOni!=null&&ids.contains(forcedOni))chosenOnis.add(forcedOni);for(UUID id:ids)if(chosenOnis.size()<oniCount&&"oni".equals(rolePreferences.get(id))&&!chosenOnis.contains(id))chosenOnis.add(id);for(UUID id:ids)if(chosenOnis.size()<oniCount&&!"player".equals(rolePreferences.get(id))&&!chosenOnis.contains(id))chosenOnis.add(id);for(UUID id:ids)if(chosenOnis.size()<oniCount&&!chosenOnis.contains(id))chosenOnis.add(id);if(chosenOnis.isEmpty()){all("鬼候補を決定できませんでした。");return;}oni=chosenOnis.get(0);oniTeam.addAll(chosenOnis);ids.removeAll(chosenOnis);players.addAll(ids);}activeOniPassives.addAll(selectedOniPassives);
        activeLobby=lobby;activeExit=ex;activeExit2=ex2;activePlayerSpawn=ps;if(soloTest&&getConfig().getBoolean("test-arena.world-border-enabled",true))applyTestWorldBorder(lobby);else if(automaticNormalArenaHearts)applyNormalArenaWorldBorder();
        Material heartMat=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN")); if(heartMat==null)heartMat=Material.CRYING_OBSIDIAN;
        for(Location l:hearts){if(automaticTestHearts||automaticNormalArenaHearts)temporaryTestHeartOriginals.put(LocationStore.encode(l),l.getBlock().getType());l.getBlock().setType(heartMat);heartHp.put(LocationStore.encode(l),getConfig().getInt("heart-max-health",100));}
        if(oniType==OniType.JAKUTSUKI)for(Location l:fakeHearts){String key=LocationStore.encode(l);fakeHeartOriginals.put(key,l.getBlock().getType());fakeHeartKeys.add(key);l.getBlock().setType(heartMat);}
        for(Location l:lootChests){String key=LocationStore.encode(l);temporaryLootChestOriginals.put(key,l.getBlock().getType());l.getBlock().setType(Material.CHEST);lootChestKeys.add(key);}
        totalHearts=heartHp.size();requiredHearts=oniType==OniType.JAKUTSUKI?totalHearts:Math.min(totalHearts,Math.max(1,getConfig().getInt(duoMode?"duo-mode.required-heart-breaks":"normal-mode.required-heart-breaks",duoMode?10:8)));if(oniType==OniType.JAKUTSUKI)all("&8蛇窟姫の領域 &7――鬼の心臓は &c"+totalHearts+"個&7。さらに偽心臓が紛れ込んでいる……");secondsLeft=getConfig().getInt("game-seconds",1200);for(UUID id:new HashSet<>(trainingPlayers)){Player q=Bukkit.getPlayer(id);if(q!=null)leaveTraining(q);}removeTrainingDummy();hideLobbyUtilities();state=GameState.RUNNING;applyGameNight(ps.getWorld());startGameStartBgm();startIkimonoSystem(ps);
        int playerSideTarget=duoMode?Math.max(1,getConfig().getInt("duo-mode.player-count",12)):(oniType==OniType.JAKUTSUKI?8:getPlayerBotTargetCount());
        String oniName;if(playerTest){LivingEntity bot=spawnOniBot(os);oniName="鬼Bot「"+oniType.display+"」";if(duoMode&&duoBotMatchStart){OniType mate=chooseDuoCompanionType(oniType);LivingEntity mateBot=spawnDuoCompanionBot(os.clone().add(2.5,0,0),mate);if(mateBot!=null){oniName+=" &7+ &c鬼Bot「"+mate.display+"」";all("&4二人鬼Bot &7―― &c"+oniType.display+" &7+ &c"+mate.display+" &7が出現した。");}}duoBotMatchStart=false;for(UUID id:players)setupPlayer(Bukkit.getPlayer(id),ps);int added=fillPlayerBotsToTarget(ps,playerSideTarget);if(soloTest)giveTestPlayBook(testGm);if(added>0)all("&bぷれいやーBot "+added+"体 &fを追加し、ぷれいやー陣営を"+playerSideTarget+"枠まで補充しました。");}else{List<String> oniNames=new ArrayList<>();int oi=0;for(UUID oid:oniTeam){Player op=Bukkit.getPlayer(oid);if(op==null)continue;Location spawn=os.clone().add(oi++*2.0,0,0);setupOni(op,spawn);oniNames.add(op.getName());}if(duoMode&&oniTeam.size()<2){OniType mate=chooseDuoCompanionType(oniType);LivingEntity bot=spawnDuoCompanionBot(os.clone().add(Math.max(1,oi)*2.0,0,0),mate);oniNames.add("鬼Bot「"+mate.display+"」");all("&4二人鬼モード &7――相方Botは選択中の &c"+oniType.display+" &7とは別種類の &c"+mate.display+" &7になりました。");}for(UUID id:players)setupPlayer(Bukkit.getPlayer(id),ps);int added=fillPlayerBotsToTarget(ps,playerSideTarget);Player primary=Bukkit.getPlayer(oni);if(soloTest&&primary!=null)giveTestPlayBook(primary);oniName=String.join(" &7+ &c",oniNames);if(added>0)all("&bぷれいやーBot "+added+"体 &fを追加し、ぷれいやー陣営を"+playerSideTarget+"枠まで補充しました。");}
        spawnConfiguredFriendlyNpc(ps);
        maybeSpawnRandomAzakuji(ps);
        all("&4&l鬼げぇむ 開始！ &f鬼は &c"+oniName+" &7(&c"+oniType.display+"&7) &fです。心臓を破壊し、討伐または脱出せよ。");
        showGameStartTitle();
        ticker=Bukkit.getScheduler().runTaskTimer(this,this::tick,20,20);
    }
    private boolean hasHumanAzakujiPreset(){
        return "AZAKUJI_HIRO".equals(parseFriendlyPreset(getConfig().getString("friendly-npc.preset","")));
    }
    private void maybeSpawnRandomAzakuji(Location origin){
        if(state!=GameState.RUNNING||oniType==OniType.JAKUTSUKI||azakujiAllyId!=null||origin==null)return;
        if(!getConfig().getBoolean("azakuji-ally.normal-match.enabled",false))return;
        double chance=Math.max(0.0,Math.min(100.0,getConfig().getDouble("azakuji-ally.normal-match.chance-percent",15.0)));
        if(Math.random()*100.0>=chance)return;
        LivingEntity ally=spawnAzakujiAlly(origin,playerBots.size()+1);
        if(ally!=null){all("&6&l字九字ひろ &fが戦場に姿を現した！ &7【レア友軍参戦】");all("&6字九字ひろ &e「俺が君たちを守る。」");}
    }
    private int activePlayerSideCountExcludingAzakuji(){
        int count=0;
        for(UUID id:players)if(!dead.contains(id)&&!escaped.contains(id)&&Bukkit.getPlayer(id)!=null)count++;
        for(UUID id:playerBots)if(!id.equals(azakujiAllyId)&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id) instanceof LivingEntity e&&e.isValid()&&!e.isDead())count++;
        return count;
    }
    private Location azakujiReinforcementSpawn(){
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null)return p.getLocation().clone().add(2,0,2);}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;Entity e=Bukkit.getEntity(id);if(e instanceof LivingEntity le&&le.isValid()&&!le.isDead())return le.getLocation().clone().add(2,0,2);}
        Location configured=LocationStore.get(getConfig(),"locations.player-spawn");return configured==null?null:configured.clone();
    }
    private void checkAzakujiReinforcement(){
        if(oniType!=OniType.JAKUTSUKI||azakujiAllyId!=null||hasHumanAzakujiPreset())return;
        int threshold=Math.max(1,getConfig().getInt("azakuji-ally.join-conditions.player-count-threshold",4));
        int active=activePlayerSideCountExcludingAzakuji();
        int remaining=Math.max(0,heartGoal()-brokenHearts);
        boolean lowPlayers=active<=threshold;
        boolean lowHearts=remaining<=Math.max(1,getConfig().getInt("azakuji-ally.join-conditions.remaining-hearts",3));
        if(!lowPlayers&&!lowHearts)return;
        Location spawn=azakujiReinforcementSpawn();if(spawn==null)return;
        LivingEntity ally=spawnAzakujiAlly(spawn,playerBots.size()+1);
        all("&6&l字九字ひろ &fが友軍として参戦した！");
        if(lowPlayers&&lowHearts)all("&7戦力減少と心臓の崩壊を察知し、アザクジが戦場へ駆けつけた。");
        else if(lowPlayers)all("&7ぷれいやー陣営が &c"+active+"人 &7まで減少――アザクジが救援に入る。");
        else all("&7鬼の心臓が残り &c"+remaining+"個 &7――アザクジが決戦へ参戦する。");
        all("&6字九字ひろ &e「待たせたな。ここからは俺も前に出るぞ！」");
        if(ally!=null){ally.getWorld().playSound(ally.getLocation(),Sound.ENTITY_LIGHTNING_BOLT_THUNDER,.8f,1.3f);ally.getWorld().spawnParticle(Particle.TOTEM,ally.getLocation().add(0,1,0),70,.8,1.0,.8,.08);}
    }
    private UUID chooseOni(List<UUID> ids){
        if(ids.isEmpty())return null;
        if(forcedOni!=null){
            if(ids.contains(forcedOni)){Player p=Bukkit.getPlayer(forcedOni);all("&c手動指定 &7により、"+(p==null?"指定プレイヤー":p.getName())+" が鬼になります。");return forcedOni;}
            Player p=Bukkit.getPlayer(forcedOni);all("&e手動指定された "+(p==null?"プレイヤー":p.getName())+" は参加していないため、鬼指定ブロックまたはランダム抽選へ切り替えます。");
        }
        Location selector=LocationStore.get(getConfig(),"locations.oni-selector-block");
        if(selector!=null){
            List<UUID> standing=new ArrayList<>();
            for(UUID id:ids){Player p=Bukkit.getPlayer(id);if(p!=null&&isStandingOnBlock(p,selector))standing.add(id);}
            if(!standing.isEmpty()){UUID chosen=standing.get(new Random().nextInt(standing.size()));Player p=Bukkit.getPlayer(chosen);all("&c鬼指定ブロック &7の上にいる "+(p==null?"参加者":p.getName())+" が鬼になります。");return chosen;}
        }
        return ids.get(0);
    }
    private boolean isStandingOnBlock(Player player,Location block){
        if(player==null||block==null||block.getWorld()==null||!player.getWorld().equals(block.getWorld()))return false;
        Location feet=player.getLocation();
        Block below=feet.clone().subtract(0,0.2,0).getBlock();
        return below.getX()==block.getBlockX()&&below.getY()==block.getBlockY()&&below.getZ()==block.getBlockZ();
    }
    private void showGameStartTitle(){if(!getConfig().getBoolean("start-title.enabled",true))return;long fadeIn=Math.max(0,getConfig().getLong("start-title.fade-in-ticks",10))*50L,stay=Math.max(1,getConfig().getLong("start-title.stay-ticks",50))*50L,fadeOut=Math.max(0,getConfig().getLong("start-title.fade-out-ticks",20))*50L;Title title=Title.title(Component.text("\uE001").font(Key.key("onigame","title")),Component.empty(),Title.Times.times(Duration.ofMillis(fadeIn),Duration.ofMillis(stay),Duration.ofMillis(fadeOut)));for(UUID id:participants){Player player=Bukkit.getPlayer(id);if(player!=null)player.showTitle(title);}}
    private OniType requireOniType(CommandSender sender,String raw){
        OniType type=OniType.parse(raw);
        if(type==null){msg(sender,"&c不明な鬼タイプ: &f"+raw+" &7(dakko / kio / shikki / yuuki / kishin / jakutsuki)");return null;}
        if(type==OniType.KANKI){msg(sender,"&5鬼神（オニガミ）は現在DLC用に休止中のため選択できません。");return null;}
        if(isOniTypeLocked(type)){msg(sender,"&cその鬼は現在ロックされています: &f"+type.display);return null;}
        return type;
    }
    private void startOniVsBots(OniType type,Player gm,int count){selectedOniType=type;oniType=type;if(!participants.isEmpty()){msg(gm,"鬼対ぷれいやーBot戦は参加者0人の状態で開始してください。");return;}int oldTarget=getConfig().getInt("player-bot.auto-fill-target",5);getConfig().set("player-bot.auto-fill-target",Math.max(1,Math.min(8,count)));try{start(type,gm,false);}finally{getConfig().set("player-bot.auto-fill-target",oldTarget);}if(state!=GameState.RUNNING||oni==null||!oni.equals(gm.getUniqueId()))return;all("&bぷれいやーBot "+playerBots.size()+"体 &fが心臓の破壊を開始した……");}
    private void startFullAiBotMatch(OniType type,Player observer,int count){
        if(state!=GameState.WAITING){msg(observer,"すでに試合中です。");return;}
        if(!participants.isEmpty()){msg(observer,"AI Botマッチは参加者0人の状態で開始してください。");return;}
        int botCount=Math.max(1,Math.min(8,count));
        int oldTarget=getConfig().getInt("player-bot.auto-fill-target",5);
        String oldFriendly=getConfig().getString("friendly-npc.preset","");
        getConfig().set("player-bot.auto-fill-target",Math.min(9,botCount+1));
        getConfig().set("friendly-npc.preset","");
        selectedOniType=type;oniType=type;
        observer.sendActionBar(cc("&7強制鬼タイプ: &c"+type.display));
        try{start(type,observer,true);}
        finally{getConfig().set("player-bot.auto-fill-target",oldTarget);getConfig().set("friendly-npc.preset",oldFriendly);}
        if(state!=GameState.RUNNING||oniBot==null)return;
        eliteBotMatchIds.clear();eliteBotMatchIds.addAll(playerBots);
        UUID observerId=observer.getUniqueId();
        players.remove(observerId);dead.remove(observerId);escaped.remove(observerId);chased.remove(observerId);
        stopRepair(observer,null);stopHealing(observer,null);cooldowns.keySet().removeIf(k->k.startsWith(observerId+":"));
        observer.setGameMode(GameMode.SPECTATOR);
        observer.sendTitle(cc("&8&lAI BOT MATCH"),cc("&4鬼Bot &7vs &aエリート友軍AI "+playerBots.size()+"人"),5,50,15);
        observer.sendActionBar(cc("&7観戦モード ―― &f/og stop &7で即終了"));
        all("&8&l【AI BOT MATCH】 &4鬼Bot「"+oniType.display+"」 &fvs &aエリート友軍AI "+playerBots.size()+"人");
        all("&7AIだけで心臓破壊・追跡・戦闘・脱出まで進行します。");
    }
    private void resetRuntime(){oniPowerActivated=false;finalFrenzy60Announced=false;finalFrenzy30Announced=false;supportDropTriggered=false;clearIkimonoSystem();clearFinalEffectTest();configuredFriendlyNpcId=null;eliteBotMatchIds.clear();oniBotTacticalTarget=null;safetyLastLocation.clear();safetyLastGoodLocation.clear();safetyStuckChecks.clear();safetyRescueCooldownUntil.clear();safetyAbuseWindowStartedAt.clear();safetyAbuseLockUntil.clear();safetyAbuseAttempts.clear();lastCombatAt.clear();lobbyUtilityGuiQueued.clear();playerHitGraceUntil.clear();downEscapeUntil.clear();extraLifeConsumed.clear();playerBotEscapeEquipment.clear();if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}if(crimsonTicker!=null){crimsonTicker.cancel();crimsonTicker=null;}resetFinalCrimsonVisuals();removeDakkoCloneBots();dakkoFoxMarkUntil.clear();dakkoFoxChain.clear();dakkoFoxChainUntil.clear();clearDakkoFoxLamps();removeKankiSummons();kankiLastSummon.clear();kankiSelectedSummon.clear();stopAllChaseBgm();stopGameStartBgm();stopFinalPhaseBgm();removeOniBot();removePlayerBots();restoreFakeHearts();restoreTemporaryTestHearts();restoreLootChests();restoreTestWorldBorder();players.clear();oniTeam.clear();disconnectGraceUntil.clear();removeAllDisconnectedOniProxies();duoBotSyntheticOwners.clear();duoOniBotPassives.clear();disconnectPlayerProxy.clear();disconnectProxyOwner.clear();disconnectOniProxy.clear();disconnectOniProxyOwner.clear();disconnectOniProxyType.clear();disconnectOniProxySkillReadyAt.clear();disconnectOniProxySkillCycle.clear();escaped.clear();dead.clear();chased.clear();heavenlyArrival.clear();bugeiActive.clear();jakutsukiPierceCharging.clear();removeBlackMirrors();removeJakutsukiSnakes();blackMirrorLocations.clear();exitSealedUntil=0L;predationTriggered.clear();activeOniPassives.clear();lastSeenAt.clear();heartbeatAt.clear();stamina.clear();dashJumpPenaltyAt.clear();passiveLeapReadyAt.clear();sneakStartedAt.clear();parrySneakAt.clear();repairStartedAt.clear();reversalAt.clear();clearFakeNoises();technicalPassiveReadyAt.clear();timedSkillUntil.clear();sealingCircles.clear();sealingCircleUntil.clear();lastMoveDirection.clear();wasSprinting.clear();aftermindPrimed.clear();repairingHeart.clear();repairProgressAt.clear();heartProgressCarry.clear();doubleStakesGreatStreak.clear();threePhaseMask.clear();footstepTokens.clear();deadlineTokens.clear();bloodMarkTokens.clear();footstepNextGainAt.clear();deadlineNearSince.clear();deadlineNextGainAt.clear();threePhaseReady.clear();huntRecordVictims.clear();grudgeResistReady.clear();grudgeTokens.clear();oniChaseLostAt.clear();oniLastHitAt.clear();oniScentTrailLocation.clear();oniScentTrailUntil.clear();oniSkillSealUntil.clear();oniBloodFrenzyHits.clear();oniDisruptionStacks.clear();oniLastAnyChaseAt=0;oniHuntersInstinctAt=0;oniHuntingGroundSince=0;oniHuntingGroundAnchor=null;shikkiFrenzyUntil=0L;shikkiChainVictims.clear();shikkiFinisherReady=false;shikkiLeapAttackUntil.clear();shikkiLeapChainCount.clear();shikkiLeapChainUntil.clear();shikkiLeapAwaitLanding.clear();yuukiVeiled.clear();yuukiAmbushUntil.clear();yuukiDivineHideUntil.clear();nextSkillCheckAt.clear();skillChecks.clear();healingTasks.clear();playerBotWorkingHeart.clear();nextPlayerBotSkillCheck.clear();playerBotNextAttackAt.clear();playerBotSkillChecks.clear();playerBotSkills.clear();playerBotPassives.clear();playerBotPresetNames.clear();playerBotSkillReadyAt.clear();playerBotSprintUntil.clear();playerBotHitGraceUntil.clear();playerBotStamina.clear();playerBotTargetHeart.clear();playerBotLastLocation.clear();playerBotStuckTicks.clear();tagBotRoamTarget.clear();tagBotNextRetargetAt.clear();tagBotVisitedChests.clear();tagBotGateChannelTicks.clear();tagBotGemNextCheckAt.clear();tagParkourPlayers.clear();tagParkourGrandfathered.clear();tagLives.clear();tagTerraGems.clear();tagTerraDiscoveredGems.clear();tagTerraAttuningGem.clear();tagTerraGemProgress.clear();tagTerraNextSkillCheckAt.clear();tagTerraSkillChecks.clear();tagTerraGateSneakTicks.clear();tagInitialSeconds=0;tagNextRandomEventAt=-1;tagHyakkiTriggered=false;tagHyakkiUntil=0L;tagReimyakuUntil=0L;tagSoulRushUntil=0L;resetTagResonance();tagDiscoveredAreas.clear();tagSelectedArea.clear();soulPoints.clear();matchEarnedSoulPoints.clear();tagSpHeartDamageCarry.clear();heartAlertAt.clear();cooldowns.clear();clearOniHeartGlowMarkers();heartHp.clear();brokenHearts=0;requiredHearts=0;oniAwakeningStage=0;clearOniDomain();oniDomainReadyAt=0L;escapePhase=1;escapeEventsCompleted=0;escapeEventsRequired=Math.max(1,getConfig().getInt("escape.required-events",5));finalPhase=false;playerSideTest=false;botStamina=20;botRestUntil=0;botNextMeleeAt=0;botDakkoSkillCycle=0;oniChaseStartedAt=0;nextBloodScentAt=0;if(ticker!=null)ticker.cancel();if(tagReleaseTask!=null){tagReleaseTask.cancel();tagReleaseTask=null;}}
    private boolean isTagStreamer(UUID id){return id!=null&&getConfig().getStringList("tag-mode.streamer-players").contains(id.toString());}
    private int tagMaxLives(UUID id){return isTagStreamer(id)?Math.max(2,getConfig().getInt("tag-mode.lives.streamer",2)):Math.max(1,getConfig().getInt("tag-mode.lives.normal",1));}
    private boolean isDownEscape(UUID id){return System.currentTimeMillis()<downEscapeUntil.getOrDefault(id,0L);}
    private boolean tryConsumeExtraLife(Player p,double lethalDamage){
        if(p==null||state!=GameState.RUNNING||!players.contains(p.getUniqueId())||dead.contains(p.getUniqueId())||escaped.contains(p.getUniqueId()))return false;
        UUID id=p.getUniqueId();
        if(isTagMode()){
            if(finalPhase||tagParkourPlayers.contains(id)||lethalDamage<p.getHealth())return false;
            int lives=tagLives.getOrDefault(id,tagMaxLives(id));if(lives<=0)return false;
            tagLives.put(id,lives-1);
            int ticks=Math.max(20,getConfig().getInt("tag-mode.lives.escape-duration-ticks",100));
            int speed=Math.max(0,getConfig().getInt("tag-mode.lives.escape-speed-amplifier",2));
            double max=p.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?10.0:p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            p.setHealth(max);p.setFireTicks(0);p.setFallDistance(0);p.setCollidable(false);
            p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,ticks,0,false,false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,speed,false,true));
            downEscapeUntil.put(id,System.currentTimeMillis()+ticks*50L);playerHitGraceUntil.remove(id);chased.remove(id);leaveChaseBgmNow(p);
            stopRepair(p,null);stopHealing(p,null);chestOpeningTasks.remove(id);
            for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.hidePlayer(this,p);
            p.sendTitle(cc("&c&l残機消費"),cc("&7完全透明化・高速移動――今のうちに逃げろ！"),3,35,10);
            p.sendActionBar(cc("&c残機消費 &7| 残り &f"+(lives-1)));p.playSound(p.getLocation(),Sound.ITEM_TOTEM_USE,1.0f,.8f);p.getWorld().spawnParticle(Particle.TOTEM,p.getLocation().add(0,1,0),45,.6,.9,.6,.08);
            Bukkit.getScheduler().runTaskLater(this,()->endDownEscape(id),ticks);return true;
        }
        if(!getConfig().getBoolean("player-lives.enabled",true)||extraLifeConsumed.contains(id)||lethalDamage<p.getHealth())return false;
        extraLifeConsumed.add(id);
        int ticks=Math.max(20,getConfig().getInt("player-lives.escape-duration-ticks",160));
        int speed=Math.max(0,getConfig().getInt("player-lives.escape-speed-amplifier",4));
        double max=p.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20.0:p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double ratio=Math.max(.05,Math.min(1.0,getConfig().getDouble("player-lives.revive-health-ratio",.35)));
        p.setHealth(Math.max(1.0,Math.min(max,max*ratio)));
        p.setFireTicks(0);p.setFallDistance(0);p.setCollidable(false);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,ticks,0,false,false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,speed,false,true));
        downEscapeUntil.put(id,System.currentTimeMillis()+ticks*50L);
        playerHitGraceUntil.remove(id);chased.remove(id);leaveChaseBgmNow(p);
        stopRepair(p,null);stopHealing(p,null);chestOpeningTasks.remove(id);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.hidePlayer(this,p);
        p.sendTitle(cc("&c&l残機消費"),cc("&7一度だけ死を免れた――今のうちに逃げろ！"),5,50,15);
        p.sendActionBar(cc("&8完全透明化 &7+ &b高速移動 &7―― &c次に倒されれば死亡"));
        p.playSound(p.getLocation(),Sound.ITEM_TOTEM_USE,1.0f,.75f);
        p.getWorld().spawnParticle(Particle.TOTEM,p.getLocation().add(0,1,0),45,.6,.9,.6,.08);
        Bukkit.getScheduler().runTaskLater(this,()->endDownEscape(id),ticks);
        return true;
    }
    private void endDownEscape(UUID id){
        downEscapeUntil.remove(id);
        Player p=Bukkit.getPlayer(id);if(p==null)return;
        p.setCollidable(true);
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.showPlayer(this,p);
        if(state==GameState.RUNNING&&players.contains(id)&&!dead.contains(id)&&!escaped.contains(id)){
            p.sendTitle("",cc("&c透明化終了 &7―― 次のダウンは即死"),0,35,10);
            p.playSound(p.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE,.7f,.8f);
        }
    }
    private void restoreDownEscapeVisual(Player p){
        if(p==null)return;p.setCollidable(true);p.removePotionEffect(PotionEffectType.INVISIBILITY);
        for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(p.getUniqueId()))viewer.showPlayer(this,p);
    }

    private boolean tryConsumePlayerBotExtraLife(LivingEntity bot,double lethalDamage){
        if(bot==null||state!=GameState.RUNNING||!playerBots.contains(bot.getUniqueId())||bot.getUniqueId().equals(azakujiAllyId)||deadPlayerBots.contains(bot.getUniqueId())||escapedPlayerBots.contains(bot.getUniqueId()))return false;
        UUID id=bot.getUniqueId();
        if(!getConfig().getBoolean("player-lives.enabled",true)||extraLifeConsumed.contains(id)||lethalDamage<bot.getHealth())return false;
        extraLifeConsumed.add(id);
        int ticks=Math.max(20,getConfig().getInt("player-lives.escape-duration-ticks",160));
        int speed=Math.max(0,getConfig().getInt("player-lives.escape-speed-amplifier",4));
        double max=bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20.0:bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double ratio=Math.max(.05,Math.min(1.0,getConfig().getDouble("player-lives.revive-health-ratio",.35)));
        bot.setHealth(Math.max(1.0,Math.min(max,max*ratio)));
        bot.setFireTicks(0);bot.setFallDistance(0);bot.setInvulnerable(true);bot.setCollidable(false);
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,ticks,0,false,false));
        bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,speed,false,true));
        if(bot.getEquipment()!=null){
            ItemStack[] saved=new ItemStack[]{cloneOrNull(bot.getEquipment().getItemInMainHand()),cloneOrNull(bot.getEquipment().getItemInOffHand()),cloneOrNull(bot.getEquipment().getHelmet()),cloneOrNull(bot.getEquipment().getChestplate()),cloneOrNull(bot.getEquipment().getLeggings()),cloneOrNull(bot.getEquipment().getBoots())};
            playerBotEscapeEquipment.put(id,saved);
            bot.getEquipment().setItemInMainHand(null);bot.getEquipment().setItemInOffHand(null);bot.getEquipment().setHelmet(null);bot.getEquipment().setChestplate(null);bot.getEquipment().setLeggings(null);bot.getEquipment().setBoots(null);
        }
        downEscapeUntil.put(id,System.currentTimeMillis()+ticks*50L);
        clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
        bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_TOTEM_USE,.9f,.8f);
        bot.getWorld().spawnParticle(Particle.TOTEM,bot.getLocation().add(0,1,0),35,.55,.8,.55,.07);
        all("&bぷれいやーBot &7が残機を消費し、透明化して逃走した！");
        Bukkit.getScheduler().runTaskLater(this,()->endPlayerBotDownEscape(id),ticks);
        return true;
    }
    private ItemStack cloneOrNull(ItemStack stack){return stack==null?null:stack.clone();}
    private void endPlayerBotDownEscape(UUID id){
        downEscapeUntil.remove(id);
        if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){playerBotEscapeEquipment.remove(id);return;}
        bot.setInvulnerable(false);bot.setCollidable(true);bot.removePotionEffect(PotionEffectType.INVISIBILITY);
        // Bot本来の恒常透明化を残機演出終了後に復元
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
        ItemStack[] saved=playerBotEscapeEquipment.remove(id);
        if(saved!=null&&bot.getEquipment()!=null&&saved.length>=6){bot.getEquipment().setItemInMainHand(saved[0]);bot.getEquipment().setItemInOffHand(saved[1]);bot.getEquipment().setHelmet(saved[2]);bot.getEquipment().setChestplate(saved[3]);bot.getEquipment().setLeggings(saved[4]);bot.getEquipment().setBoots(saved[5]);}
        if(state==GameState.RUNNING&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)){bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_DEACTIVATE,.55f,.85f);bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),16,.4,.6,.4,.03);}
    }
    private void restorePlayerBotDownEscape(UUID id){
        downEscapeUntil.remove(id);
        if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){playerBotEscapeEquipment.remove(id);return;}
        bot.setInvulnerable(false);bot.setCollidable(true);bot.removePotionEffect(PotionEffectType.INVISIBILITY);
        // Bot本来の恒常透明化を残機演出終了後に復元
        bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
        ItemStack[] saved=playerBotEscapeEquipment.remove(id);
        if(saved!=null&&bot.getEquipment()!=null&&saved.length>=6){bot.getEquipment().setItemInMainHand(saved[0]);bot.getEquipment().setItemInOffHand(saved[1]);bot.getEquipment().setHelmet(saved[2]);bot.getEquipment().setChestplate(saved[3]);bot.getEquipment().setLeggings(saved[4]);bot.getEquipment().setBoots(saved[5]);}
    }

    private void common(Player p){restoreDownEscapeVisual(p);p.getInventory().clear();p.getActivePotionEffects().forEach(e->p.removePotionEffect(e.getType()));p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setWalkSpeed((float)getConfig().getDouble("player-walk-speed",0.20));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);p.setHealth(20);p.setFoodLevel(20);p.setFireTicks(0);}
    private void startIkimonoSystem(Location center){
        clearIkimonoSystem();
        if(center==null||!getConfig().getBoolean("ikimono.enabled",false))return;
        int count=Math.max(0,getConfig().getInt("ikimono.count",3));
        int radius=Math.max(12,getConfig().getInt("ikimono.spawn-radius",85));
        Random r=new Random();
        for(int i=0;i<count;i++){
            Location loc=null;
            for(int tries=0;tries<24;tries++){
                double ang=r.nextDouble()*Math.PI*2,dist=18+r.nextDouble()*Math.max(1,radius-18);
                int x=center.getBlockX()+(int)Math.round(Math.cos(ang)*dist),z=center.getBlockZ()+(int)Math.round(Math.sin(ang)*dist);
                Location c=safeSurfaceAt(center.getWorld(),x,z,1);
                if(c!=null){loc=c;break;}
            }
            if(loc!=null)spawnIkimono(loc);
        }
        if(!ikimonoIds.isEmpty())all("&5&l異獣（イキモノ） &7がマップを徘徊している…… &f"+ikimonoIds.size()+"体");
        ikimonoTicker=Bukkit.getScheduler().runTaskTimer(this,this::tickIkimono,20L,10L);
    }
    private Location findIkimonoCommandSpawn(Location base){
        World w=base.getWorld();if(w==null)return base;Random r=new Random();
        for(int tries=0;tries<16;tries++){double ang=r.nextDouble()*Math.PI*2,dist=3+r.nextDouble()*5;int x=base.getBlockX()+(int)Math.round(Math.cos(ang)*dist),z=base.getBlockZ()+(int)Math.round(Math.sin(ang)*dist);Location c=safeSurfaceAt(w,x,z,1);if(c!=null)return c;}
        return base.clone().add(2,0,2);
    }
    private void clearIkimonoEntitiesOnly(){for(UUID id:new HashSet<>(ikimonoIds)){clearIkimonoLight(id);Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}ikimonoIds.clear();ikimonoLastAttacker.clear();ikimonoContribution.clear();}
    private void spawnIkimono(Location at){
        Husk beast=at.getWorld().spawn(at,Husk.class);
        beast.setCustomName(cc("&5&l異獣（イキモノ）"));beast.setCustomNameVisible(true);beast.setPersistent(true);beast.setRemoveWhenFarAway(false);beast.setCanPickupItems(false);
        double hp=Math.max(1,getConfig().getDouble("ikimono.max-health",24.0));beast.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);beast.setHealth(hp);
        beast.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(Math.max(.05,getConfig().getDouble("ikimono.movement-speed",.25)));
        beast.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(Math.max(1,getConfig().getDouble("ikimono.attack-damage",3.0)));
        if(beast.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)!=null)beast.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(Math.max(8,getConfig().getDouble("ikimono.aggro-range",16.0)));
        if(beast.getEquipment()!=null){beast.getEquipment().clear();}
        ikimonoIds.add(beast.getUniqueId());
    }
    private void clearIkimonoLight(UUID id){
        Location old=ikimonoLightLocations.remove(id);if(old==null||old.getWorld()==null)return;
        Block b=old.getBlock();if(b.getType()==Material.LIGHT)b.setType(Material.AIR,false);
    }
    private void updateIkimonoLight(UUID id,LivingEntity beast){
        if(!getConfig().getBoolean("ikimono.ambient-light.enabled",true)){clearIkimonoLight(id);return;}
        Location desired=beast.getLocation().getBlock().getLocation().add(0,1,0);Location old=ikimonoLightLocations.get(id);
        if(old!=null&&old.getWorld()==desired.getWorld()&&old.getBlockX()==desired.getBlockX()&&old.getBlockY()==desired.getBlockY()&&old.getBlockZ()==desired.getBlockZ())return;
        clearIkimonoLight(id);Block b=desired.getBlock();if(!b.getType().isAir())return;
        b.setType(Material.LIGHT,false);if(b.getBlockData() instanceof Light light){light.setLevel(Math.max(1,Math.min(15,getConfig().getInt("ikimono.ambient-light.level",12))));b.setBlockData(light,false);}
        ikimonoLightLocations.put(id,desired);
    }
    private void tickIkimono(){
        if(state!=GameState.RUNNING||finalPhase){clearIkimonoSystem();return;}
        double range=Math.max(8,getConfig().getDouble("ikimono.aggro-range",16.0)),r2=range*range;
        for(UUID id:new HashSet<>(ikimonoIds)){
            Entity raw=Bukkit.getEntity(id);if(!(raw instanceof Mob beast)||!raw.isValid()||raw.isDead()){ikimonoIds.remove(id);clearIkimonoLight(id);continue;}
            updateIkimonoLight(id,beast);
            LivingEntity best=null;double bestD=r2;
            for(UUID pid:participants){Player q=Bukkit.getPlayer(pid);if(q==null||q.getGameMode()==GameMode.SPECTATOR||q.isDead()||!q.getWorld().equals(beast.getWorld()))continue;double d=q.getLocation().distanceSquared(beast.getLocation());if(d<bestD){bestD=d;best=q;}}
            List<UUID> botIds=new ArrayList<>();botIds.addAll(playerBots);if(oniBot!=null)botIds.add(oniBot);botIds.addAll(disconnectOniProxy.values());
            for(UUID bid:botIds){Entity q=Bukkit.getEntity(bid);if(!(q instanceof LivingEntity le)||q.isDead()||!q.getWorld().equals(beast.getWorld()))continue;double d=q.getLocation().distanceSquared(beast.getLocation());if(d<bestD){bestD=d;best=le;}}
            beast.setTarget(best);
        }
    }
    private void clearIkimonoSystem(){if(ikimonoTicker!=null){ikimonoTicker.cancel();ikimonoTicker=null;}for(UUID id:new HashSet<>(ikimonoIds)){clearIkimonoLight(id);Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}for(UUID id:new HashSet<>(ikimonoLightLocations.keySet()))clearIkimonoLight(id);ikimonoIds.clear();ikimonoLastAttacker.clear();ikimonoContribution.clear();ikimonoBotBuffs.clear();ikimonoFactionBuffUntil.clear();}
    private void addIkimonoContribution(UUID beastId,UUID actor,double points){
        if(beastId==null||actor==null||points<=0)return;
        ikimonoContribution.computeIfAbsent(beastId,k->new HashMap<>()).merge(actor,points,Double::sum);
    }
    private void handleIkimonoDeath(EntityDeathEvent e){
        e.getDrops().clear();e.setDroppedExp(0);
        UUID beastId=e.getEntity().getUniqueId();UUID last=ikimonoLastAttacker.remove(beastId);
        if(last!=null)addIkimonoContribution(beastId,last,Math.max(0,getConfig().getDouble("ikimono.contribution.last-hit-bonus",5.0)));
        Map<UUID,Double> scores=ikimonoContribution.remove(beastId);if(scores==null)scores=Map.of();
        double total=scores.values().stream().mapToDouble(Double::doubleValue).sum();
        double minRate=Math.max(0,Math.min(1,getConfig().getDouble("ikimono.contribution.minimum-rate",.10)));
        double minAbs=Math.max(0,getConfig().getDouble("ikimono.contribution.minimum-points",8.0));
        List<Map.Entry<UUID,Double>> qualified=new ArrayList<>();
        for(Map.Entry<UUID,Double> en:scores.entrySet()){double rate=total<=0?0:en.getValue()/total;if(rate>=minRate||en.getValue()>=minAbs)qualified.add(en);}
        if(qualified.isEmpty()&&last!=null)qualified.add(Map.entry(last,Math.max(0,scores.getOrDefault(last,0.0))));
        String type=randomIkimonoBuffType();int rewarded=0;
        for(Map.Entry<UUID,Double> en:qualified){UUID who=en.getKey();double rate=total<=0?0:en.getValue()/total;
            if(isGameBot(who)){ikimonoBotBuffs.computeIfAbsent(who,k->new ArrayDeque<>()).addLast(type);rewarded++;continue;}
            Player p=Bukkit.getPlayer(who);if(p==null||!p.isOnline())continue;
            HashMap<Integer,ItemStack> overflow=p.getInventory().addItem(createIkimonoBuff(type));for(ItemStack item:overflow.values())p.getWorld().dropItemNaturally(p.getLocation(),item);
            msg(p,"&5異獣討伐 &7貢献度: &f"+String.format(Locale.ROOT,"%.0f",rate*100)+"% &7―― &d特殊な加護を獲得");rewarded++;
        }
        all("&5異獣が討伐された。 &d貢献者 "+rewarded+"名 &7が特殊な加護を得た……");
        e.getEntity().getWorld().spawnParticle(Particle.TOTEM,e.getEntity().getLocation().add(0,1,0),28,.5,.8,.5,.08);
        e.getEntity().getWorld().playSound(e.getEntity().getLocation(),Sound.ENTITY_EVOKER_PREPARE_SUMMON,1f,1.25f);
    }
    private String randomIkimonoBuffType(){String[] types={"speed","vitality","stamina","ward"};return types[new Random().nextInt(types.length)];}
    private ItemStack createIkimonoBuff(){return createIkimonoBuff(randomIkimonoBuffType());}
    private ItemStack createIkimonoBuff(String type){
        Material mat=switch(type){case "speed"->Material.RABBIT_FOOT;case "vitality"->Material.GHAST_TEAR;case "stamina"->Material.SUGAR;default->Material.AMETHYST_SHARD;};
        String name=switch(type){case "speed"->"&b異獣の俊足";case "vitality"->"&c異獣の生命核";case "stamina"->"&e異獣の活力";default->"&d異獣の護符";};
        ItemStack it=new ItemStack(mat);ItemMeta m=it.getItemMeta();m.setDisplayName(cc(name));m.setLore(List.of(cc("&7異獣討伐でのみ得られる陣営バフ。"),cc("&f右クリックで使用すると味方全員に発動")));m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"ikimono_buff:"+type);it.setItemMeta(m);return it;
    }
    private boolean useIkimonoBuff(Player p,ItemStack it){
        if(it==null||!it.hasItemMeta())return false;String a=it.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(a==null||!a.startsWith("ikimono_buff:"))return false;
        if(state!=GameState.RUNNING)return false;String type=a.substring("ikimono_buff:".length());boolean oniSide=isOni(p.getUniqueId());if(!oniSide&&!players.contains(p.getUniqueId()))return false;
        applyIkimonoFactionBuff(oniSide,type,p.getName());if(p.getGameMode()!=GameMode.CREATIVE)it.setAmount(it.getAmount()-1);return true;
    }
    private void applyIkimonoFactionBuff(boolean oniSide,String type,String source){
        int sec=Math.max(5,getConfig().getInt("ikimono.buff-duration-seconds",30));String faction=oniSide?"oni":"player";ikimonoFactionBuffUntil.put(faction+":"+type,System.currentTimeMillis()+sec*1000L);
        Collection<LivingEntity> targets=new ArrayList<>();
        if(oniSide){for(UUID id:oniTeam){Player q=Bukkit.getPlayer(id);if(q!=null&&q.isOnline()&&q.getGameMode()!=GameMode.SPECTATOR)targets.add(q);}if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity b)targets.add(b);for(UUID id:disconnectOniProxy.values())if(Bukkit.getEntity(id) instanceof LivingEntity b)targets.add(b);}
        else{for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q!=null&&q.isOnline()&&!dead.contains(id)&&!escaped.contains(id)&&q.getGameMode()!=GameMode.SPECTATOR)targets.add(q);}for(UUID id:playerBots)if(!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id) instanceof LivingEntity b)targets.add(b);}
        for(LivingEntity q:targets)applyIkimonoBuffEffect(q,type,sec);
        all((oniSide?"&4鬼陣営":"&bぷれいやー陣営")+" &7が &d異獣の加護 &7を獲得！ &f"+source+" &8("+sec+"秒)");
        if(!targets.isEmpty()){LivingEntity q=targets.iterator().next();q.getWorld().playSound(q.getLocation(),Sound.ITEM_TOTEM_USE,.7f,1.25f);}
    }
    private void applyIkimonoBuffEffect(LivingEntity q,String type,int sec){
        switch(type){case "speed"->q.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,sec*20,1,false,true));case "vitality"->{q.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,sec*20,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,0,false,true));}case "stamina"->{if(q instanceof Player p){p.setFoodLevel(20);p.setSaturation(20f);stamina.put(p.getUniqueId(),20.0);}if(playerBots.contains(q.getUniqueId()))playerBotStamina.put(q.getUniqueId(),20.0);if(oniBot!=null&&oniBot.equals(q.getUniqueId()))botStamina=20.0;q.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,sec*20,0,false,true));}default->q.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,sec*20,0,false,true));}
    }
    private boolean isGameBot(UUID id){return id!=null&&(playerBots.contains(id)||(oniBot!=null&&oniBot.equals(id))||disconnectOniProxy.containsValue(id));}
    private LivingEntity nearestIkimono(LivingEntity from,double range){LivingEntity best=null;double d2=range*range;for(UUID id:ikimonoIds)if(Bukkit.getEntity(id) instanceof LivingEntity b&&b.isValid()&&!b.isDead()&&b.getWorld().equals(from.getWorld())){double d=b.getLocation().distanceSquared(from.getLocation());if(d<d2){d2=d;best=b;}}return best;}
    private void tickIkimonoBotBuffUse(UUID id,LivingEntity bot,boolean oniSide){
        Deque<String> held=ikimonoBotBuffs.get(id);if(held==null||held.isEmpty())return;String type=held.peekFirst();long now=System.currentTimeMillis();if(now<ikimonoFactionBuffUntil.getOrDefault((oniSide?"oni":"player")+":"+type,0L)-5000)return;
        boolean combat=nearestIkimono(bot,10)!=null||(oniSide?findNearestPlayerSideTarget(bot)!=null:getOniEntity()!=null&&getOniEntity().getWorld().equals(bot.getWorld())&&getOniEntity().getLocation().distanceSquared(bot.getLocation())<144);
        double ratio=bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());boolean use=switch(type){case "vitality"->ratio<=.65||factionNeedsHealing(oniSide);case "stamina"->oniSide?botStamina<=9:playerBotStamina.getOrDefault(id,20.0)<=9;case "speed"->combat;default->combat||ratio<=.70;};
        if(use){held.removeFirst();applyIkimonoFactionBuff(oniSide,type,oniSide?"鬼Bot":"ぷれいやーBot");}
    }
    private boolean factionNeedsHealing(boolean oniSide){if(oniSide){LivingEntity o=getOniEntity();return o!=null&&o.getHealth()/Math.max(1,o.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=.65;}for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&p.getHealth()/Math.max(1,p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=.65)return true;}for(UUID id:playerBots)if(Bukkit.getEntity(id) instanceof LivingEntity b&&b.getHealth()/Math.max(1,b.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=.65)return true;return false;}

    private void setupPlayer(Player p,Location at){common(p);loadFavoriteLoadout(p);if(hasPassive(p.getUniqueId(),PassiveSkill.DURABILITY_BOOST)){double maxHealth=Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);p.setHealth(maxHealth);}if(hasPassive(p.getUniqueId(),PassiveSkill.NINJA_BLOOD))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23));else if(hasPassive(p.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.divine-technique-walk-speed",0.21));p.teleport(at);PlayerSkill sk=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);List<String> skillLore=new ArrayList<>(Arrays.asList(playerSkillEffectLore(sk)));skillLore.add(mainSkillUsage(sk));p.getInventory().setItem(0,playerSkillIconItem(sk,"&b&l"+sk.display,"skill:"+sk.name(),skillLore.toArray(new String[0])));p.getInventory().setItem(7,playerPassiveSummaryItem(p));p.getInventory().setItem(8,item(Material.COMPASS,"&d心臓探知機","tracker"));giveTechnicalPassiveItems(p);}
    private boolean hasPassive(UUID id,PassiveSkill passive){return selectedPassives.getOrDefault(id,new LinkedHashSet<>()).contains(passive);}
    private void giveTechnicalPassiveItems(Player player){/* v0.38.4: 陽動は専用アイテム不要 */}
    private void setupOni(Player p,Location at){common(p);migrateLegacyJakutsukiNames(p);p.teleport(at);p.setWalkSpeed((float)getConfig().getDouble("oni-walk-speed",0.24));if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(hasOniPassive(OniPassiveSkill.ANCHOR)?Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))):0);double oniMax=oniType==OniType.JAKUTSUKI?getConfig().getDouble("jakutsuki-stats.max-health",200.0):60.0;p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(oniMax);p.setHealth(oniMax);p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,Integer.MAX_VALUE,0,false,false));p.getInventory().setItem(0,item(Material.NETHERITE_SWORD,"&4&l鬼の爪","oni_weapon"));equipLegacyOniArmor(p,oniType);if(oniType==OniType.DAKKO){p.getInventory().setItem(1,oniSkillIconItem("dakko_tp","&d狐渡り &7[SHIFT+右:分身体と狐換え]","dakko_tp"));refreshOniUnlockedSkills(p,false);}else if(oniType==OniType.KISHIN){p.getInventory().setItem(1,oniSkillIconItem("kishin_charge","&c鬼突","kishin_charge"));refreshOniUnlockedSkills(p,false); }else if(oniType==OniType.SHIKKI){p.getInventory().setItem(1,oniSkillIconItem("shikki_hunting_leap","&6&l跳躍狩り","shikki_hunting_leap"));refreshOniUnlockedSkills(p,false);}else if(oniType==OniType.YUUKI){p.getInventory().setItem(1,oniSkillIconItem("yuuki_veil","&8&l幽歩","yuuki_veil"));refreshOniUnlockedSkills(p,false);}else if(oniType==OniType.KANKI){p.getInventory().setItem(1,oniSkillIconItem("kanki_summon","&5&l鬼喚び &7[SHIFT+右:召喚先変更]","kanki_summon"));p.getInventory().setItem(2,oniSkillIconItem("kanki_command","&d&l鬼令","kanki_command"));p.getInventory().setItem(3,oniSkillIconItem("kanki_recall","&6&l再臨","kanki_recall"));p.getInventory().setItem(4,oniSkillIconItem("kanki_parade","&4&l百鬼夜行","kanki_parade"));}else{p.getInventory().setItem(1,oniSkillIconItem("jakutsuki_black_mirror","&5&l黒鏡 &7[SHIFT+右:設置 / 右:転移]","jakutsuki_black_mirror"));p.getInventory().setItem(2,oniSkillIconItem("jakutsuki_sweep","&5&l薙ぎ払い","jakutsuki_sweep"));p.getInventory().setItem(3,oniSkillIconItem("jakutsuki_snakefall","&8&l蛇崩","jakutsuki_snakefall"));p.getInventory().setItem(4,oniSkillIconItem("jakutsuki_release","&4&l解放","jakutsuki_release"));p.getInventory().setItem(5,oniSkillIconItem("jakutsuki_piercing_blast","&d&l黒の波動 &7[溜め攻撃]","jakutsuki_piercing_blast"));}p.getInventory().setItem(6,oniPassiveSummaryItem());}
    private LivingEntity spawnOniBot(Location at){Zombie bot=at.getWorld().spawn(at,Zombie.class);bot.setBaby(false);bot.setCustomName(cc("&4鬼Bot「"+oniType.display+"」"));bot.setCustomNameVisible(false);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.setCanPickupItems(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));double botMax=oniType==OniType.JAKUTSUKI?getConfig().getDouble("jakutsuki-stats.max-health",200.0):60.0;bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(botMax);bot.setHealth(botMax);bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",0.32));bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(getConfig().getDouble("bot.attack-damage",7.0));if(bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(hasOniPassive(OniPassiveSkill.ANCHOR)?Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))):0);bot.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));bot.getEquipment().setItemInMainHandDropChance(0);equipLegacyOniArmor(bot,oniType);bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);oniBot=bot.getUniqueId();botNextAbilityAt=System.currentTimeMillis()+5000;return bot;}
    private OniType chooseDuoCompanionType(OniType primary){
        List<OniType> pool=new ArrayList<>();
        for(OniType t:new OniType[]{OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI,OniType.JAKUTSUKI})if(t!=primary&&!isOniTypeLocked(t))pool.add(t);
        if(pool.isEmpty())for(OniType t:OniType.values())if(t!=primary&&!isOniTypeLocked(t)){pool.add(t);break;}
        return pool.isEmpty()?primary:pool.get(new Random().nextInt(pool.size()));
    }
    private LinkedHashSet<OniPassiveSkill> chooseDuoOniPassives(OniType type){
        LinkedHashSet<OniPassiveSkill> out=new LinkedHashSet<>();
        // EXTRAは鬼種の得意行動を伸ばす構築。ELITE/NORMALも腐りにくい構成を持つ。
        if(botDifficulty==BotDifficulty.EXTRA){
            switch(type){
                case DAKKO->{out.add(OniPassiveSkill.MASTERY);out.add(OniPassiveSkill.BEAST_PATH);}
                case KISHIN->{out.add(OniPassiveSkill.ANCHOR);out.add(OniPassiveSkill.BLOOD_FRENZY);}
                case SHIKKI->{out.add(OniPassiveSkill.BEAST_PATH);out.add(OniPassiveSkill.FINISHER_CHASE);}
                case YUUKI->{out.add(OniPassiveSkill.AMBUSH);out.add(OniPassiveSkill.SEE_THROUGH);}
                case JAKUTSUKI->{out.add(OniPassiveSkill.MASTERY);out.add(OniPassiveSkill.PRESSURE);}
                default->{out.add(OniPassiveSkill.MASTERY);out.add(OniPassiveSkill.HUNTERS_INSTINCT);}
            }
        }else if(botDifficulty==BotDifficulty.ELITE){out.add(OniPassiveSkill.MASTERY);out.add(type==OniType.KISHIN?OniPassiveSkill.ANCHOR:OniPassiveSkill.BEAST_PATH);}
        else{out.add(OniPassiveSkill.BLOOD_SCENT);out.add(OniPassiveSkill.MOMENTUM);}
        return out;
    }
    private boolean hasDuoOniPassive(UUID id,OniPassiveSkill passive){return duoOniBotPassives.getOrDefault(id,new LinkedHashSet<>()).contains(passive);}

    private LivingEntity spawnDuoCompanionBot(Location at,OniType type){
        if(at==null||at.getWorld()==null||type==null)return null;Zombie bot=at.getWorld().spawn(at,Zombie.class);bot.setBaby(false);bot.setCustomName(cc("&4鬼Bot「"+type.display+"」 &7【DUO相方】"));bot.setCustomNameVisible(false);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.setCanPickupItems(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));double max=type==OniType.JAKUTSUKI?getConfig().getDouble("jakutsuki-stats.max-health",200.0):60.0;if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);bot.setHealth(max);if(bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",0.32));if(bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!=null)bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(getConfig().getDouble("bot.attack-damage",7.0));bot.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));bot.getEquipment().setItemInMainHandDropChance(0);equipLegacyOniArmor(bot,type);bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);UUID synthetic=UUID.randomUUID(),id=bot.getUniqueId();duoBotSyntheticOwners.add(synthetic);disconnectOniProxy.put(synthetic,id);disconnectOniProxyOwner.put(id,synthetic);disconnectOniProxyType.put(id,type);disconnectOniProxySkillReadyAt.put(id,System.currentTimeMillis()+2500L);disconnectOniProxySkillCycle.put(id,0);
        LinkedHashSet<OniPassiveSkill> duoPassives=chooseDuoOniPassives(type);duoOniBotPassives.put(id,duoPassives);
        if(duoPassives.contains(OniPassiveSkill.ANCHOR)&&bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)bot.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))));
        return bot;
    }
    private LivingEntity getOniEntity(){if(state==GameState.WAITING&&trainingDummy!=null&&Bukkit.getEntity(trainingDummy) instanceof LivingEntity entity)return entity;if(oni!=null){Player human=Bukkit.getPlayer(oni);if(human!=null&&human.isOnline()&&human.getGameMode()!=GameMode.SPECTATOR)return human;}if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity entity)return entity;for(UUID proxyId:disconnectOniProxy.values())if(Bukkit.getEntity(proxyId) instanceof LivingEntity proxy&&proxy.isValid()&&!proxy.isDead())return proxy;return null;}
    private boolean isOni(UUID id){return id!=null&&(oniTeam.contains(id)||(oni!=null&&oni.equals(id))||(oniBot!=null&&oniBot.equals(id))||disconnectOniProxy.containsValue(id));}
    private int heartGoal(){return requiredHearts>0?requiredHearts:totalHearts;}
    private boolean heartGoalReached(){return brokenHearts>=heartGoal();}
    private void removeOniBot(){if(oniBot!=null){Entity entity=Bukkit.getEntity(oniBot);if(entity!=null)entity.remove();oniBot=null;}}
    private String parseFriendlyPreset(String raw){
        if(raw==null)return null;String key=raw.trim().toUpperCase(Locale.ROOT).replace("-","_");
        return switch(key){
            case "IGAMI","KYOYA","IGAMI_KYOYA","伊神京也"->"IGAMI_KYOYA";
            case "MISAKI","AZANAMI_MISAKI","字那美咲"->"AZANAMI_MISAKI";
            case "FUUKA","FUKA","ARIKAWA_FUUKA","有川風香"->"ARIKAWA_FUUKA";
            case "REN","AZANAMI_REN","字那美蓮"->"AZANAMI_REN";
            case "MEDIC","NATSUMI","SAKURA_NATSUMI","佐倉夏海"->"MEDIC";
            case "HIIRO","AKASAKA_HIIRO","赤坂陽彩"->"AKASAKA_HIIRO";
            case "RION","KAGAYA_RION","加賀谷凛音"->"KAGAYA_RION";
            case "IONA","AMANAI_IONA","天内伊御奈"->"AMANAI_IONA";
            case "HIRO","AZAKUJI_HIRO","字九字ひろ"->"AZAKUJI_HIRO";
            default->null;
        };
    }
    private String friendlyPresetDisplay(String id){return switch(id){
        case "IGAMI_KYOYA"->"伊神京也";case "AZANAMI_MISAKI"->"字那美咲";case "ARIKAWA_FUUKA"->"有川風香";case "AZANAMI_REN"->"字那美蓮";
        case "MEDIC"->"佐倉夏海";case "AKASAKA_HIIRO"->"赤坂陽彩";case "KAGAYA_RION"->"加賀谷凛音";case "AMANAI_IONA"->"天内伊御奈";case "AZAKUJI_HIRO"->"字九字ひろ";default->id;
    };}
    private int friendlyPresetHeadModel(String id){return switch(id){
        case "ARIKAWA_FUUKA"->2001;case "IGAMI_KYOYA"->2002;case "AZANAMI_MISAKI"->2003;case "AZANAMI_REN"->2004;case "MEDIC"->2005;
        case "AKASAKA_HIIRO"->2006;case "KAGAYA_RION"->2007;case "AMANAI_IONA"->2008;case "AZAKUJI_HIRO"->2010;default->0;
    };}
    private boolean applyFriendlyPreset(LivingEntity bot,String preset){
        if(bot==null||preset==null)return false;UUID id=bot.getUniqueId();PlayerSkill skill;LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();
        switch(preset){
            case "IGAMI_KYOYA"->{skill=PlayerSkill.ONI_STRIKE;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DURABILITY_BOOST);}
            case "AZANAMI_MISAKI"->{skill=PlayerSkill.SPRINT;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DEEP_BREATH);}
            case "ARIKAWA_FUUKA"->{skill=PlayerSkill.SMOKE;passives.add(PassiveSkill.COWARDICE);passives.add(PassiveSkill.EXORCISM);}
            case "AZANAMI_REN"->{skill=PlayerSkill.OBSESSION;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DURABILITY_BOOST);}
            case "MEDIC"->{skill=PlayerSkill.HEAL;passives.add(PassiveSkill.FOCUS);passives.add(PassiveSkill.BOND);}
            case "AKASAKA_HIIRO"->{skill=PlayerSkill.SAFE_LANDING;passives.add(PassiveSkill.NINJA_BLOOD);passives.add(PassiveSkill.LEAP);}
            case "KAGAYA_RION"->{skill=PlayerSkill.BLINK;passives.add(PassiveSkill.QUICK_TURN);passives.add(PassiveSkill.CORNERED_RAT);}
            case "AMANAI_IONA"->{skill=PlayerSkill.ECHO;passives.add(PassiveSkill.SILENT_BREATH);passives.add(PassiveSkill.DECOY);}
            case "AZAKUJI_HIRO"->{skill=PlayerSkill.BUGEI;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DIVINE_TECHNIQUE);}
            default->{return false;}
        }
        playerBotSkills.put(id,skill);playerBotPassives.put(id,passives);playerBotPresetNames.put(id,friendlyPresetDisplay(preset));
        double max=passives.contains(PassiveSkill.DURABILITY_BOOST)?Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30)):20;
        if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);bot.setHealth(max);
        bot.setCustomName(cc("&a&l"+friendlyPresetDisplay(preset)+" &7【友軍NPC】"));bot.setCustomNameVisible(true);
        if(bot.getEquipment()!=null){
            ItemStack head=new ItemStack(Material.PLAYER_HEAD);ItemMeta meta=head.getItemMeta();int model=friendlyPresetHeadModel(preset);if(model>0)meta.setCustomModelData(model);head.setItemMeta(meta);
            bot.getEquipment().setHelmet(head);bot.getEquipment().setChestplate(plainBotArmor(Material.LEATHER_CHESTPLATE));bot.getEquipment().setLeggings(plainBotArmor(Material.LEATHER_LEGGINGS));bot.getEquipment().setBoots(plainBotArmor(Material.LEATHER_BOOTS));
            bot.getEquipment().setItemInMainHand(skill==PlayerSkill.BUGEI?new ItemStack(Material.GOLDEN_SWORD):new ItemStack(Material.IRON_PICKAXE));
            bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);bot.getEquipment().setItemInMainHandDropChance(0);
        }
        return true;
    }
    private LivingEntity spawnConfiguredFriendlyNpc(Location origin){
        String preset=getConfig().getString("friendly-npc.preset","");
        if(origin==null||preset==null||preset.isBlank())return null;
        preset=parseFriendlyPreset(preset);if(preset==null)return null;
        if("AZAKUJI_HIRO".equals(preset)){
            if(isAzakujiLocked())return null;
            LivingEntity ally=spawnAzakujiAlly(origin,playerBots.size()+1);
            configuredFriendlyNpcId=ally==null?null:ally.getUniqueId();
            all("&6&l字九字ひろ &fが開始時から友軍NPCとして参戦した！");
            return ally;
        }
        LivingEntity ally=spawnPlayerBot(origin,playerBots.size()+1);
        if(!applyFriendlyPreset(ally,preset)){ally.remove();playerBots.remove(ally.getUniqueId());return null;}
        configuredFriendlyNpcId=ally.getUniqueId();
        all("&a&l"+friendlyPresetDisplay(preset)+" &fが友軍NPCとして参戦した！");
        return ally;
    }

    private LivingEntity spawnPlayerBot(Location origin,int number){Location at=origin.clone().add((number-1)%3-1,0,(number-1)/3);Zombie bot=at.getWorld().spawn(at,Zombie.class);bot.setBaby(false);bot.setAI(true);bot.setSilent(true);bot.setCanPickupItems(false);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));assignPlayerBotPreset(bot,number);double max=isTagMode()?10.0:(hasPlayerBotPassive(bot.getUniqueId(),PassiveSkill.DURABILITY_BOOST)?Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30)):20);bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("player-bot.pathfinder-base-movement-speed",.25));bot.setHealth(max);bot.setCustomName("ぷれいやーBot "+number+"【"+playerBotPresetNames.get(bot.getUniqueId())+"】");bot.setCustomNameVisible(false);bot.getEquipment().setHelmet(plainBotArmor(Material.LEATHER_HELMET));bot.getEquipment().setChestplate(plainBotArmor(Material.LEATHER_CHESTPLATE));bot.getEquipment().setLeggings(plainBotArmor(Material.LEATHER_LEGGINGS));bot.getEquipment().setBoots(plainBotArmor(Material.LEATHER_BOOTS));bot.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_PICKAXE));bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);bot.getEquipment().setItemInMainHandDropChance(0);UUID id=bot.getUniqueId();playerBots.add(id);playerBotStamina.put(id,20.0);return bot;}
    private LivingEntity spawnAzakujiAlly(Location origin,int number){
        LivingEntity bot=spawnPlayerBot(origin,number);
        UUID id=bot.getUniqueId();
        azakujiAllyId=id;
        azakujiFightUntil=0L;azakujiEscapeUntil=0L;azakujiTauntUntil=0L;azakujiNextLeapAt=0L;azakujiNextMeleeAt=0L;azakujiLastCombatAt=0L;azakujiDivineUntil=0L;azakujiSpawnedAt=System.currentTimeMillis();azakujiLeapInvulnerableUntil=0L;azakujiNextDivineThunderAt=0L;azakujiNextBlinkAt=0L;azakujiOniBotRetaliateUntil=0L;azakujiDivineThunderUses=0;azakujiDivinePossessionUsed=false;azakujiForcedAttrition=false;
        LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();
        passives.add(PassiveSkill.ATTACK_BOOST);
        passives.add(PassiveSkill.DIVINE_TECHNIQUE);
        playerBotSkills.put(id,PlayerSkill.BUGEI);
        playerBotPassives.put(id,passives);
        playerBotPresetNames.put(id,"字九字ひろ");
        bot.setCustomName(cc("&6&l字九字ひろ &7【友軍NPC】"));
        bot.setCustomNameVisible(true);
        double speed=getConfig().getDouble("player-bot.pathfinder-base-movement-speed",.25)*getConfig().getDouble("azakuji-ally.divine-technique-extreme.speed-multiplier",1.32);
        if(bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
        double allyMaxHealth=Math.max(1.0,getConfig().getDouble("azakuji-ally.max-health",60.0));
        if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(allyMaxHealth);
        bot.setHealth(allyMaxHealth);
        if(bot.getAttribute(Attribute.GENERIC_ARMOR)!=null)bot.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(Math.max(0.0,getConfig().getDouble("azakuji-ally.armor",12.0)));
        // v0.40.41: 字九字ひろ専用の着用外見。ネザライトの標準防具レイヤーを専用テクスチャへ差し替える。
        // 実効防御は GENERIC_ARMOR の字九字専用値で管理するため、各防具の標準Armor属性は除去する。
        bot.getEquipment().setHelmet(azakujiArmorPiece(Material.NETHERITE_HELMET,"helmet","&6字九字ひろの頭装備"));
        bot.getEquipment().setChestplate(azakujiArmorPiece(Material.NETHERITE_CHESTPLATE,"chestplate","&6字九字ひろの装束"));
        bot.getEquipment().setLeggings(azakujiArmorPiece(Material.NETHERITE_LEGGINGS,"leggings","&6字九字ひろの袴"));
        bot.getEquipment().setBoots(azakujiArmorPiece(Material.NETHERITE_BOOTS,"boots","&6字九字ひろの足具"));
        bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);
        bot.getEquipment().setItemInMainHand(new ItemStack(Material.GOLDEN_SWORD));
        updateAzakujiLight(bot);
        return bot;
    }

    private ItemStack azakujiArmorPiece(Material material,String part,String name){
        ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(cc(name));meta.setUnbreakable(true);
        String id="npc.azakuji."+part;
        meta.getPersistentDataContainer().set(equipmentIdKey,PersistentDataType.STRING,id);
        meta.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"equipment:"+id);
        // ネザライト本来の防御/タフネス/KB耐性を無効化。字九字の防御は専用AI側のGENERIC_ARMORだけで決める。
        for(Attribute attribute:List.of(Attribute.GENERIC_ARMOR,Attribute.GENERIC_ARMOR_TOUGHNESS,Attribute.GENERIC_KNOCKBACK_RESISTANCE))
            meta.addAttributeModifier(attribute,new AttributeModifier(UUID.randomUUID(),"azakuji_"+part+"_"+attribute.name().toLowerCase(Locale.ROOT),0.0,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlot.valueOf(part.equals("helmet")?"HEAD":part.equals("chestplate")?"CHEST":part.equals("leggings")?"LEGS":"FEET")));
        item.setItemMeta(meta);return item;
    }
    private void clearAzakujiLight(){
        Location old=azakujiLightLocation;azakujiLightLocation=null;if(old==null||old.getWorld()==null)return;
        Block b=old.getBlock();if(b.getType()==Material.LIGHT)b.setType(Material.AIR,false);
    }
    private void updateAzakujiLight(LivingEntity bot){
        if(bot==null||!bot.isValid()||bot.isDead()||!getConfig().getBoolean("azakuji-ally.ambient-light.enabled",true)){clearAzakujiLight();return;}
        Location desired=bot.getLocation().getBlock().getLocation().add(0,1,0);
        if(azakujiLightLocation!=null&&azakujiLightLocation.getWorld()==desired.getWorld()&&azakujiLightLocation.getBlockX()==desired.getBlockX()&&azakujiLightLocation.getBlockY()==desired.getBlockY()&&azakujiLightLocation.getBlockZ()==desired.getBlockZ())return;
        clearAzakujiLight();Block b=desired.getBlock();if(!b.getType().isAir())return;
        b.setType(Material.LIGHT,false);if(b.getBlockData() instanceof Light light){light.setLevel(Math.max(1,Math.min(15,getConfig().getInt("azakuji-ally.ambient-light.level",12))));b.setBlockData(light,false);}
        azakujiLightLocation=desired;
    }
    private void retreatAzakuji(LivingEntity bot){
        if(bot==null)return;UUID id=bot.getUniqueId();clearAzakujiLight();deadPlayerBots.add(id);clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
        all("&6字九字ひろ &e「…すまない、撤退する」");
        bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),36,.45,.8,.45,.04);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.75f,.65f);bot.remove();
    }

    private LinkedHashSet<PassiveSkill> extraPlayerBotPassives(PlayerSkill skill){
        LinkedHashSet<PassiveSkill> p=new LinkedHashSet<>();
        switch(skill){
            case SPRINT->{p.add(PassiveSkill.LIGHT_FOOTED);p.add(PassiveSkill.DEEP_BREATH);}
            case INVISIBLE->{p.add(PassiveSkill.SILENT_BREATH);p.add(PassiveSkill.FOOTSTEP_THIEF);}
            case SMOKE->{p.add(PassiveSkill.COWARDICE);p.add(PassiveSkill.DECOY);}
            case ONI_STRIKE->{p.add(PassiveSkill.ATTACK_BOOST);p.add(PassiveSkill.DURABILITY_BOOST);}
            case HEAL->{p.add(PassiveSkill.BOND);p.add(PassiveSkill.DURABILITY_BOOST);}
            case OBSESSION->{p.add(PassiveSkill.LIGHT_FOOTED);p.add(PassiveSkill.CORNERED_RAT);}
            case SAFE_LANDING->{p.add(PassiveSkill.NINJA_BLOOD);p.add(PassiveSkill.LEAP);}
            case BLINK->{p.add(PassiveSkill.QUICK_TURN);p.add(PassiveSkill.LAST_RESERVE);}
            case ECHO->{p.add(PassiveSkill.SILENT_BREATH);p.add(PassiveSkill.DECOY);}
            case CLAIRVOYANCE->{p.add(PassiveSkill.FOOTSTEP_THIEF);p.add(PassiveSkill.FOCUS);}
            case UNYIELDING->{p.add(PassiveSkill.DURABILITY_BOOST);p.add(PassiveSkill.PARRY);}
            case SEALING_CIRCLE->{p.add(PassiveSkill.EXORCISM);p.add(PassiveSkill.FOCUS);}
            case RESONANCE->{p.add(PassiveSkill.FOCUS);p.add(PassiveSkill.THREE_PHASE);}
            case SUBSTITUTE->{p.add(PassiveSkill.BOND);p.add(PassiveSkill.DURABILITY_BOOST);}
            case DESPERATE_RUN->{p.add(PassiveSkill.LAST_RESERVE);p.add(PassiveSkill.CORNERED_RAT);}
            case BUGEI->{p.add(PassiveSkill.ATTACK_BOOST);p.add(PassiveSkill.DIVINE_TECHNIQUE);}
        }
        return p;
    }

    private void assignPlayerBotPreset(LivingEntity bot,int number){UUID id=bot.getUniqueId();PlayerSkill skill=PlayerSkill.HEAL;LinkedHashSet<PassiveSkill> passives=new LinkedHashSet<>();String name="佐倉夏海";switch(Math.floorMod(number-1,5)){case 0->{name="伊神京也";skill=PlayerSkill.ONI_STRIKE;passives.add(PassiveSkill.ATTACK_BOOST);passives.add(PassiveSkill.DURABILITY_BOOST);}case 1->{name="字那美咲";skill=PlayerSkill.SPRINT;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DEEP_BREATH);}case 2->{name="有川風香";skill=PlayerSkill.SMOKE;passives.add(PassiveSkill.COWARDICE);passives.add(PassiveSkill.EXORCISM);}case 3->{name="字那美蓮";skill=PlayerSkill.OBSESSION;passives.add(PassiveSkill.LIGHT_FOOTED);passives.add(PassiveSkill.DURABILITY_BOOST);}default->{passives.add(PassiveSkill.FOCUS);passives.add(PassiveSkill.BOND);}}
        if(getConfig().getBoolean("player-bot.extra.enabled",true))passives=extraPlayerBotPassives(skill);
        playerBotSkills.put(id,skill);playerBotPassives.put(id,passives);playerBotPresetNames.put(id,name);}
    private boolean hasPlayerBotPassive(UUID id,PassiveSkill passive){return playerBotPassives.getOrDefault(id,new LinkedHashSet<>()).contains(passive);}
    private boolean playerBotHasSnakeSlash(LivingEntity bot){return bot!=null&&bot.getEquipment()!=null&&isJakutsukiSnakeSlash(bot.getEquipment().getItemInMainHand());}
    private int getPlayerBotTargetCount(){if(!getConfig().getBoolean("player-bot.auto-fill-enabled",true))return 0;if(isTagMode())return Math.max(0,Math.min(40,getConfig().getInt("tag-mode.player-count",20)));return Math.max(0,Math.min(8,getConfig().getInt("player-bot.auto-fill-target",5)));}
    private int fillPlayerBotsToTarget(Location origin,int target){if(origin==null||target<=0)return 0;int humanPlayers=(int)players.stream().filter(id->Bukkit.getPlayer(id)!=null).count();int needed=Math.max(0,target-humanPlayers-playerBots.size());int startNumber=playerBots.size()+1;for(int i=0;i<needed;i++)spawnPlayerBot(origin,startNumber+i);return needed;}

    private ItemStack plainBotArmor(Material material){ItemStack item=new ItemStack(material);ItemMeta meta=item.getItemMeta();meta.setUnbreakable(true);item.setItemMeta(meta);return item;}
    private void removePlayerBots(){clearAzakujiLight();configuredFriendlyNpcId=null;eliteBotMatchIds.clear();for(UUID id:new HashSet<>(playerBots)){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}azakujiAllyId=null;azakujiFightUntil=0L;azakujiEscapeUntil=0L;azakujiTauntUntil=0L;azakujiNextLeapAt=0L;azakujiNextMeleeAt=0L;azakujiLastCombatAt=0L;azakujiDivineUntil=0L;azakujiSpawnedAt=0L;azakujiLeapInvulnerableUntil=0L;azakujiNextDivineThunderAt=0L;azakujiNextBlinkAt=0L;azakujiOniBotRetaliateUntil=0L;azakujiDivineThunderUses=0;azakujiDivinePossessionUsed=false;azakujiForcedAttrition=false;playerBots.clear();escapedPlayerBots.clear();deadPlayerBots.clear();playerBotSkills.clear();playerBotPassives.clear();playerBotPresetNames.clear();playerBotSkillReadyAt.clear();playerBotSprintUntil.clear();playerBotHitGraceUntil.clear();playerBotStamina.clear();playerBotTargetHeart.clear();playerBotLastLocation.clear();playerBotStuckTicks.clear();}
    private Material skillMaterial(PlayerSkill s){return switch(s){case SPRINT->Material.FEATHER;case INVISIBLE->Material.GLASS_BOTTLE;case SMOKE->Material.INK_SAC;case ONI_STRIKE->Material.BLAZE_ROD;case HEAL->Material.GOLDEN_APPLE;case OBSESSION->Material.SPECTRAL_ARROW;case SAFE_LANDING->Material.RABBIT_FOOT;case BLINK->Material.ENDER_PEARL;case ECHO->Material.ARMOR_STAND;case CLAIRVOYANCE->Material.ENDER_EYE;case UNYIELDING->Material.TOTEM_OF_UNDYING;case SEALING_CIRCLE->Material.ENCHANTING_TABLE;case RESONANCE->Material.AMETHYST_SHARD;case SUBSTITUTE->Material.CHORUS_FRUIT;case DESPERATE_RUN->Material.NETHER_STAR;case BUGEI->Material.GOLDEN_SWORD;};}
    private void migrateLegacyJakutsukiNames(Player p){
        for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack stack=p.getInventory().getItem(slot);if(stack==null||!stack.hasItemMeta())continue;String action=actionOf(stack);ItemMeta meta=stack.getItemMeta();String display=meta.hasDisplayName()?ChatColor.stripColor(meta.getDisplayName()):"";if("jakutsuki_piercing_blast".equals(action)||(display!=null&&display.contains("蛇穿"))){meta.setDisplayName(cc("&d&l黒の波動 &7[溜め攻撃]"));stack.setItemMeta(meta);}}
    }
    private ItemStack item(Material m,String name,String action){ItemStack i=new ItemStack(m);ItemMeta im=i.getItemMeta();im.setDisplayName(cc(name));im.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,action);i.setItemMeta(im);return i;}
    private ItemStack menuItem(Material material,String name,String action,String... lore){ItemStack i=item(material,name,action);ItemMeta m=i.getItemMeta();m.setLore(Arrays.stream(lore).map(this::cc).toList());i.setItemMeta(m);return i;}
    private ItemStack consumable(Material material,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();List<String> lines=new ArrayList<>(m.getLore()==null?List.of():m.getLore());lines.add(cc("&6使い切りアイテム"));m.setLore(lines);i.setItemMeta(m);return i;}
    private String actionOf(ItemStack stack){return stack==null||!stack.hasItemMeta()?null:stack.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);}
    private boolean isMainSkillItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("skill:");}
    private boolean isConsumableItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("item:");}
    private boolean isEquipmentItem(ItemStack stack){String action=actionOf(stack);return action!=null&&action.startsWith("equipment:");}
    private int carriedConsumables(Player player){int count=0;for(ItemStack stack:player.getInventory().getContents())if(isConsumableItem(stack))count+=stack.getAmount();return count;}
    private ItemStack equipment(Material material,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();m.setUnbreakable(true);List<String> lines=new ArrayList<>(m.getLore()==null?List.of():m.getLore());lines.add(cc("&b装備品 &7――使い切りアイテム上限の対象外"));m.setLore(lines);i.setItemMeta(m);return i;}
    private ItemStack customModelItem(Material material,int model,String name,String action,String... lore){ItemStack i=menuItem(material,name,action,lore);ItemMeta m=i.getItemMeta();m.setCustomModelData(model);i.setItemMeta(m);return i;}
    private int passiveIconModel(PassiveSkill passive){return 3101+passive.ordinal();}
    private int oniPassiveIconModel(OniPassiveSkill passive){return 3201+passive.ordinal();}
    private ItemStack passiveIconItem(PassiveSkill passive,String name,String action,String... lore){return customModelItem(Material.PAPER,passiveIconModel(passive),name,action,lore);}
    private ItemStack oniPassiveIconItem(OniPassiveSkill passive,String name,String action,String... lore){return customModelItem(Material.PAPER,oniPassiveIconModel(passive),name,action,lore);}
    private int playerSkillIconModel(PlayerSkill skill){return 3301+skill.ordinal();}
    private ItemStack playerSkillIconItem(PlayerSkill skill,String name,String action,String... lore){return customModelItem(Material.PAPER,playerSkillIconModel(skill),name,action,lore);}
    private int oniSkillIconModel(String action){return switch(action){
        case "dakko_tp"->3401;case "dakko_clone"->3402;case "dakko_fox_fire"->3403;case "dakko_heavenly_arrival"->3404;
        case "kishin_charge"->3411;case "kishin_slam"->3412;case "kishin_roar"->3413;case "kishin_iron_body"->3414;
        case "jakutsuki_black_mirror"->3421;case "jakutsuki_sweep"->3422;case "jakutsuki_snakefall"->3423;case "jakutsuki_release"->3424;case "jakutsuki_piercing_blast"->3425;
        case "shikki_frenzy"->3431;case "shikki_blood_run"->3432;case "shikki_hunting_leap"->3433;case "shikki_chain_hunt"->3434;case "kanki_summon"->3451;case "kanki_command"->3452;case "kanki_recall"->3453;case "kanki_parade"->3454;
        case "yuuki_veil"->3441;case "yuuki_haze_step"->3442;case "yuuki_shadow_bind"->3443;case "yuuki_divine_hide"->3444;
        default->0;};}
    private ItemStack oniSkillIconItem(String oniAction,String name,String action,String... lore){int model=oniSkillIconModel(oniAction);debugTrace("texture","TEX-101","oni skill icon action="+oniAction+" CMD="+model+" base=PAPER");List<String> lines=new ArrayList<>();lines.addAll(Arrays.asList(oniSkillLore(oniAction)));lines.addAll(Arrays.asList(lore));return model<=0?menuItem(Material.PAPER,name,action,lines.toArray(new String[0])):customModelItem(Material.PAPER,model,name,action,lines.toArray(new String[0]));}

    private String mainSkillUsage(PlayerSkill skill){return switch(skill){
        case SPRINT,INVISIBLE,SMOKE,ONI_STRIKE,OBSESSION,BLINK,ECHO,CLAIRVOYANCE,UNYIELDING,SEALING_CIRCLE,RESONANCE,DESPERATE_RUN,BUGEI->"&e使用方法: &fゲーム中、専用スキルアイテムを右クリック";
        case HEAL->"&e使用方法: &f専用スキルアイテムを右クリックし、移動せず回復完了まで待つ";
        case SAFE_LANDING->"&e使用方法: &f落下ダメージが発生する着地時に自動発動";
        case SUBSTITUTE->"&e使用方法: &f15m以内の仲間を視線中央に捉えて専用アイテムを右クリック";
    };}
    private String passiveUsage(PassiveSkill passive){return switch(passive){
        case LEAP->"&e使用方法: &f空中でスペースをもう一度押す";
        case PARRY->"&e使用方法: &f鬼の攻撃が当たる直前にSHIFT";
        case QUICK_TURN->"&e使用方法: &f走行方向に対して横を向きながらSHIFT";
        case SILENT_BREATH->"&e使用方法: &fその場で動かずSHIFTを長押し";
        case LAST_RESERVE->"&e使用方法: &fスタミナ切れ直前に走るのを止める";
        case DECOY->"&e使用方法: &fスニークしながら右クリック &7(専用アイテム不要 / CT40秒)";
        case CORNERED_RAT->"&e使用方法: &f鬼の攻撃直前に進行方向を反転する";
        case DOUBLE_STAKES->"&e使用方法: &f心臓損傷中、金色のGREATに重なった瞬間にSHIFT";
        case THREE_PHASE->"&eトークン: &fチェイス離脱=走 / 心臓破壊参加=破 / 仲間の治療支援=援";
        case FOOTSTEP_THIEF->"&eトークン: &f鬼20m以内で未発見のまま10秒ごとに1、最大4";
        case DEADLINE_ACCOUNTING->"&eトークン: &f鬼3.5m以内を約2.5秒、追跡されず無傷で切り抜けると1";
        case AFTERMIND->"&e使用方法: &f心臓損傷のスキルチェックでGREATを出すと自動発動";
        case PRACTICED->"&e使用方法: &f同じ心臓の損傷を中断せず続けると自動発動";
        case COWARDICE->"&e使用方法: &fSHIFTでスニーク中に自動適用";
        case BOND->"&e使用方法: &f治療スキル完了時、周囲の仲間へ自動適用";
        default->"&e使用方法: &f常時または条件成立時に自動発動";
    };}
    private ItemStack oniPassiveSummaryItem(){List<String> lore=new ArrayList<>();lore.add("&7現在有効な鬼パッシブ");LinkedHashSet<OniPassiveSkill> src=activeOniPassives.isEmpty()?selectedOniPassives:activeOniPassives;if(src.isEmpty())lore.add("&8未選択");for(OniPassiveSkill passive:src){lore.add("&5&l"+passive.display);lore.add("&7"+passive.description);lore.add(" ");}lore.add("&8※効果確認用。使用する必要はありません。");return menuItem(Material.ENCHANTED_BOOK,"&5&l鬼パッシブ効果","oni_passive_summary",lore.toArray(new String[0]));}
    private ItemStack playerPassiveSummaryItem(Player p){
        List<String> lore=new ArrayList<>();lore.add("&7現在装備中のパッシブ");
        LinkedHashSet<PassiveSkill> set=selectedPassives.getOrDefault(p.getUniqueId(),new LinkedHashSet<>());
        if(set.isEmpty())lore.add("&8未選択");
        for(PassiveSkill passive:set){lore.add("&a&l"+passive.display);lore.add("&7"+passive.description);lore.add(passiveUsage(passive));lore.add(" ");}
        lore.add("&8※効果確認用。使用する必要はありません。");
        return menuItem(Material.BOOK,"&a&lパッシブ効果","passive_summary",lore.toArray(new String[0]));
    }
    private String[] oniSkillLore(String action){return switch(action){
        case "dakko_tp"->new String[]{"&7指定地点へ狐渡りで高速移動。","&e操作: &f右クリック / SHIFT+右で狐換え"};
        case "dakko_clone"->new String[]{"&7短時間、分身体を召喚して追跡を補助。","&e操作: &f右クリック"};
        case "dakko_fox_fire"->new String[]{"&7狐火を放ち、炎上＋固定ダメージ。","&e操作: &f右クリック"};
        case "dakko_heavenly_arrival"->new String[]{"&7上空から獲物へ飛来する強襲技。","&e操作: &f右クリック"};
        case "kishin_charge"->new String[]{"&7前方へ突進して獲物を捉える。","&e操作: &f右クリック"};
        case "kishin_slam"->new String[]{"&7地面を砕き周囲へ衝撃を与える。","&e操作: &f右クリック"};
        case "kishin_roar"->new String[]{"&7咆哮で周囲のぷれいやーを妨害。","&e操作: &f右クリック"};
        case "kishin_iron_body"->new String[]{"&7一定時間、攻撃性能を強化する。","&e操作: &f右クリック"};
        case "jakutsuki_black_mirror"->new String[]{"&7黒鏡を設置し、設置済みの鏡へ転移。","&e操作: &fSHIFT+右=設置 / 右=転移"};
        case "jakutsuki_sweep"->new String[]{"&7前方扇状を薙ぎ払う範囲攻撃。","&e操作: &f右クリック"};
        case "jakutsuki_snakefall"->new String[]{"&7蛇を大量召喚。命中した相手を発光。","&e操作: &f右クリック"};
        case "jakutsuki_release"->new String[]{"&7終盤に狂化し、追跡性能を高める。","&e操作: &f条件成立時に使用"};
        case "jakutsuki_piercing_blast"->new String[]{"&7溜めた後、貫通する広範囲の波動を放つ。","&e操作: &f右クリック"};
        case "shikki_hunting_leap"->new String[]{"&7獲物へ跳躍して距離を詰める。","&e操作: &f右クリック"};
        case "shikki_blood_run"->new String[]{"&7血走状態で追跡能力を高める。","&e操作: &f右クリック"};
        case "shikki_frenzy"->new String[]{"&7狂奔し、跳躍狩りを強化する。","&e操作: &f右クリック"};
        case "shikki_chain_hunt"->new String[]{"&7連続狩猟を狙う終盤能力。","&e操作: &f右クリック"};
        case "yuuki_veil"->new String[]{"&7幽歩で姿を隠し、奇襲の機会を作る。","&e操作: &f右クリック"};
        case "yuuki_haze_step"->new String[]{"&7朧のように素早く位置を変える。","&e操作: &f右クリック"};
        case "yuuki_shadow_bind"->new String[]{"&7影を縫い、対象の移動を妨害する。","&e操作: &f右クリック"};
        case "yuuki_divine_hide"->new String[]{"&7神隠し状態から強力な奇襲を狙う。","&e操作: &f右クリック"};
        case "kanki_summon"->new String[]{"&7鬼を召喚。SHIFT+右で召喚先を変更。","&e操作: &f右クリック"};
        case "kanki_command"->new String[]{"&7召喚した鬼へ命令を与える。","&e操作: &f右クリック"};
        case "kanki_recall"->new String[]{"&7配下を再配置して戦線へ戻す。","&e操作: &f右クリック"};
        case "kanki_parade"->new String[]{"&7百鬼夜行を展開する大技。","&e操作: &f右クリック"};
        default->new String[]{"&7鬼スキル","&e操作: &f右クリック"};
    };}
    private String favoritePath(UUID id){return "favorite-loadouts."+id;}
    private boolean isFavoriteLocked(UUID id){return getConfig().getBoolean(favoritePath(id)+".enabled",false);}
    private void toggleFavoriteLoadout(Player p){String path=favoritePath(p.getUniqueId());boolean next=!getConfig().getBoolean(path+".enabled",false);getConfig().set(path+".enabled",next);if(next){writeFavoriteLoadout(p);msg(p,"&a★ 現在のスキル構成をお気に入り登録しました。今後は自動復元されます。");}else{saveConfig();msg(p,"&7☆ お気に入り構成を解除しました。現在の選択はそのままです。");}}
    private void saveFavoriteIfLocked(Player p){if(isFavoriteLocked(p.getUniqueId()))writeFavoriteLoadout(p);}
    private void writeFavoriteLoadout(Player p){String path=favoritePath(p.getUniqueId());PlayerSkill skill=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);getConfig().set(path+".enabled",true);getConfig().set(path+".skill",skill.name());getConfig().set(path+".passives",selectedPassives.getOrDefault(p.getUniqueId(),new LinkedHashSet<>()).stream().map(Enum::name).toList());saveConfig();}
    private void loadFavoriteLoadout(Player p){String path=favoritePath(p.getUniqueId());if(!getConfig().getBoolean(path+".enabled",false))return;try{PlayerSkill skill=PlayerSkill.valueOf(getConfig().getString(path+".skill","SPRINT"));selectedSkill.put(p.getUniqueId(),skill);}catch(IllegalArgumentException ignored){}LinkedHashSet<PassiveSkill> set=new LinkedHashSet<>();for(String raw:getConfig().getStringList(path+".passives")){try{set.add(PassiveSkill.valueOf(raw));}catch(IllegalArgumentException ignored){}if(set.size()>=2)break;}selectedPassives.put(p.getUniqueId(),set);selectedPresetNames.remove(p.getUniqueId());}
    private int menuPageFromType(String type){
        int colon=type.indexOf(':');
        if(colon<0)return 1;
        try{return Math.max(1,Integer.parseInt(type.substring(colon+1)));}catch(NumberFormatException ignored){return 1;}
    }
    private String[] playerSkillEffectLore(PlayerSkill skill){
        return switch(skill){
            case SPRINT->new String[]{"&7効果: 5秒間速度上昇III","&7クールダウン: 60秒"};
            case INVISIBLE->new String[]{"&7効果: 6秒間透明化","&7クールダウン: 40秒"};
            case SMOKE->new String[]{"&7鬼の視界を妨害する煙幕を展開","&7クールダウン: 35秒"};
            case ONI_STRIKE->new String[]{"&7鬼を怯ませて距離を作る","&7クールダウン: 45秒"};
            case HEAL->new String[]{"&7回復ゲージ完了後に体力20回復（10♥）","&7絆なしでは自分だけを回復","&7攻撃・移動で中断 / CT: 45秒"};
            case OBSESSION->new String[]{"&7自分と鬼を8秒間発光","&7鬼の輪郭は全員から視認可能","&7クールダウン: 35秒"};
            case SAFE_LANDING->new String[]{"&7落下ダメージが発生する着地時に自動発動","&7落下ダメージ無効＋4秒間速度上昇III","&7クールダウン: 55秒"};
            case BLINK->new String[]{"&7移動方向へ4ブロック高速移動","&7壁抜け不可 / スタミナ4 / CT: 60秒"};
            case ECHO->new String[]{"&7スキン頭＋皮装備の残像と偽走行音を15秒間放つ","&7発動直後は音響感知を回避 / CT: 60秒"};
            case CLAIRVOYANCE->new String[]{"&7心臓・物資・負傷者・出口を色別表示","&7持続8秒 / CT: 45秒"};
            case UNYIELDING->new String[]{"&76秒間、致死ダメージを受けても体力1で耐える","&7CT: 60秒"};
            case SEALING_CIRCLE->new String[]{"&7足元へ10秒間の罠を設置","&7鬼が踏むと3秒鈍足 / CT: 35秒"};
            case RESONANCE->new String[]{"&7自分のスタミナ6を周囲の仲間へ分配","&7半径8m / CT: 30秒"};
            case SUBSTITUTE->new String[]{"&7視線先15m以内の仲間と位置交換","&7自分は発光・仲間は加速 / CT: 50秒"};
            case DESPERATE_RUN->new String[]{"&7体力30%以下限定。8秒間速度・損壊強化","&7終了後スタミナ0＋鈍足 / CT: 80秒"};
            case BUGEI->new String[]{"&7高く跳躍して鬼へ急接近し、強烈な飛び蹴りを叩き込む","&7射程18m / CT: 55秒"};
        };
    }
    private ItemStack playerSkillMenuItem(PlayerSkill skill,PlayerSkill current){
        String[] lore=playerSkillEffectLore(skill);
        List<String> lines=new ArrayList<>(Arrays.asList(lore));
        lines.add(mainSkillUsage(skill));
        lines.add(current==skill?"&a✔ 選択中":"&eクリックして選択");
        return playerSkillIconItem(skill,"&b&l"+skill.display,"menu_skill:"+skill.name(),lines.toArray(new String[0]));
    }
    private void fillMenuBackground(Inventory menu,Material material){
        for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(material," ","menu_decor"));
    }
    private void setRoleHeader(Inventory menu,int slot,Material material,String title,String description){
        menu.setItem(slot,menuItem(material,title,"menu_decor",description));
    }
    private void setSkillGroup(Inventory menu,PlayerSkill current,int startSlot,PlayerSkill... skills){
        for(int i=0;i<skills.length;i++)menu.setItem(startSlot+i,playerSkillMenuItem(skills[i],current));
    }
    private void setPassiveGroup(Inventory menu,LinkedHashSet<PassiveSkill> selected,int startSlot,PassiveSkill... passives){
        for(int i=0;i<passives.length;i++){
            PassiveSkill passive=passives[i];
            menu.setItem(startSlot+i,passiveIconItem(passive,"&a&l"+passive.display,"menu_passive:"+passive.name(),
                "&7"+passive.description,passiveUsage(passive),selected.contains(passive)?"&a✔ 選択中":"&eクリックして選択"));
        }
    }
    private void addPlayerBuildFooter(Player p,Inventory menu,boolean passiveTab,int page,int maxPage){
        PlayerSkill current=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);
        LinkedHashSet<PassiveSkill> passives=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
        menu.setItem(45,menuItem(Material.NETHER_STAR,passiveTab?"&7スキル":"&b&lスキル","menu_page:skill:1","&7メインスキルを1つ選択"));
        menu.setItem(46,menuItem(Material.ARROW,page>1?"&e&l← 前のページ":"&8← 前のページ",page>1?(passiveTab?"menu_page:passive:"+(page-1):"menu_page:skill:"+(page-1)):"menu_decor"));
        menu.setItem(47,playerSkillIconItem(current,"&b選択中スキル","menu_decor","&f"+current.display));
        List<String> selectedLore=new ArrayList<>();selectedLore.add("&7最大2個");
        if(passives.isEmpty())selectedLore.add("&8未選択");
        else for(PassiveSkill passive:passives)selectedLore.add("&a✔ "+passive.display);
        menu.setItem(48,menuItem(Material.TOTEM_OF_UNDYING,"&aパッシブ &f"+passives.size()+"/2","menu_decor",selectedLore.toArray(new String[0])));
        menu.setItem(49,menuItem(Material.TOTEM_OF_UNDYING,passiveTab?"&a&lパッシブ":"&7パッシブ","menu_page:passive:1","&7パッシブを最大2個まで選択"));
        boolean favorite=isFavoriteLocked(p.getUniqueId());menu.setItem(50,menuItem(favorite?Material.LIME_DYE:Material.GRAY_DYE,favorite?"&a&l★ お気に入り構成 ON":"&7☆ お気に入り構成 OFF","menu_favorite_toggle",favorite?"&7現在の構成を保存中です。":"&7クリックで現在の構成を保存します。","&7ON中に構成を変えると保存内容も自動更新。","&7再ログイン・再起動後も復元します。"));
        menu.setItem(51,menuItem(Material.PAPER,"&fページ &e"+page+"&7/&e"+maxPage,"menu_decor"));
        menu.setItem(52,menuItem(Material.ARROW,page<maxPage?"&e&l次のページ →":"&8次のページ →",page<maxPage?(passiveTab?"menu_page:passive:"+(page+1):"menu_page:skill:"+(page+1)):"menu_decor"));
        menu.setItem(53,menuItem(Material.COMPASS,"&e&lプリセット一覧","menu_page:presets","&7おすすめ構成・キャラクタープリセット"));
    }
    private void openSkillMenu(Player p){openSkillPage(p,1);}
    private void openSkillPage(Player p,int page){
        page=Math.max(1,Math.min(2,page));
        MenuHolder holder=new MenuHolder("skills:"+page);
        Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0スキル &7["+page+"/2]"));holder.inventory=menu;
        fillMenuBackground(menu,Material.BLUE_STAINED_GLASS_PANE);
        PlayerSkill current=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);
        menu.setItem(4,menuItem(Material.NETHER_STAR,"&b&lメインスキル &f1/1","menu_decor","&7選択中: &b"+current.display,"&7役割別に整理されています。"));
        if(page==1){
            setRoleHeader(menu,9,Material.FEATHER,"&b&l【機動・逃走】","&7距離を稼ぐ、移動する、危険地帯から離脱する");
            setSkillGroup(menu,current,10,PlayerSkill.SPRINT,PlayerSkill.SAFE_LANDING,PlayerSkill.BLINK,PlayerSkill.DESPERATE_RUN);
            setRoleHeader(menu,27,Material.ENDER_EYE,"&7&l【潜伏・撹乱】","&7鬼から姿や位置を隠し、追跡判断を狂わせる");
            setSkillGroup(menu,current,28,PlayerSkill.INVISIBLE,PlayerSkill.ECHO,PlayerSkill.SMOKE);
        }else{
            setRoleHeader(menu,9,Material.IRON_SWORD,"&c&l【対鬼・制圧】","&7鬼へ直接干渉し、追跡や攻撃を抑える");
            setSkillGroup(menu,current,10,PlayerSkill.ONI_STRIKE,PlayerSkill.BUGEI,PlayerSkill.SEALING_CIRCLE,PlayerSkill.OBSESSION);
            setRoleHeader(menu,27,Material.HEART_OF_THE_SEA,"&a&l【支援・探索】","&7仲間を助け、情報やスタミナを共有する");
            setSkillGroup(menu,current,28,PlayerSkill.HEAL,PlayerSkill.CLAIRVOYANCE,PlayerSkill.RESONANCE,PlayerSkill.SUBSTITUTE);
            setRoleHeader(menu,36,Material.SHIELD,"&6&l【生存】","&7致命的な状況を切り抜ける");
            setSkillGroup(menu,current,37,PlayerSkill.UNYIELDING);
        }
        addPlayerBuildFooter(p,menu,false,page,2);
        p.openInventory(menu);
    }
    private void openPassivePage(Player p,int page){
        page=Math.max(1,Math.min(4,page));
        MenuHolder holder=new MenuHolder("passives:"+page);
        Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0パッシブ &7["+page+"/4]"));holder.inventory=menu;
        fillMenuBackground(menu,Material.LIME_STAINED_GLASS_PANE);
        LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
        menu.setItem(4,menuItem(Material.TOTEM_OF_UNDYING,"&a&lパッシブスキル &f"+selected.size()+"/2","menu_decor","&7最大2個まで選択","&7選択済みを再クリックすると解除","&7役割別に整理されています。"));
        if(page==1){
            setRoleHeader(menu,9,Material.RABBIT_FOOT,"&b&l【機動・スタミナ】","&7走行・跳躍・方向転換など移動性能を強化");
            setPassiveGroup(menu,selected,10,PassiveSkill.LIGHT_FOOTED,PassiveSkill.DEEP_BREATH,PassiveSkill.NINJA_BLOOD,PassiveSkill.LEAP,PassiveSkill.QUICK_TURN,PassiveSkill.LAST_RESERVE);
            setRoleHeader(menu,27,Material.SCULK_SENSOR,"&7&l【隠密・陽動】","&7発見されにくくする、または偽情報で鬼を誘導する");
            setPassiveGroup(menu,selected,28,PassiveSkill.COWARDICE,PassiveSkill.SILENT_BREATH,PassiveSkill.DECOY);
        }else if(page==2){
            setRoleHeader(menu,9,Material.SHIELD,"&c&l【生存・対鬼】","&7耐久・回避・反撃など鬼との接触に強くなる");
            setPassiveGroup(menu,selected,10,PassiveSkill.DURABILITY_BOOST,PassiveSkill.ATTACK_BOOST,PassiveSkill.PARRY,PassiveSkill.CORNERED_RAT,PassiveSkill.DIVINE_TECHNIQUE);
            menu.setItem(27,menuItem(Material.IRON_CHESTPLATE,"&6役割メモ","menu_decor","&7チェイスや救援、鬼への牽制を担当したい人向け。","&7他ページの機動系と組み合わせるのも有効です。"));
        }else if(page==3){
            setRoleHeader(menu,9,Material.CRYING_OBSIDIAN,"&6&l【心臓損壊・作業】","&7心臓の発見・損壊・スキルチェックを強化");
            setPassiveGroup(menu,selected,10,PassiveSkill.FOCUS,PassiveSkill.EXORCISM,PassiveSkill.PRACTICED,PassiveSkill.AFTERMIND,PassiveSkill.DOUBLE_STAKES);
            setRoleHeader(menu,27,Material.HEART_OF_THE_SEA,"&a&l【支援】","&7仲間との連携や回復を強化");
            setPassiveGroup(menu,selected,28,PassiveSkill.BOND);
        }else{
            setRoleHeader(menu,9,Material.AMETHYST_SHARD,"&d&l【トークン系】","&7条件を満たして蓄積し、完成時に大きな効果へ変換");
            setPassiveGroup(menu,selected,10,PassiveSkill.THREE_PHASE,PassiveSkill.FOOTSTEP_THIEF,PassiveSkill.DEADLINE_ACCOUNTING);
            menu.setItem(27,menuItem(Material.BOOK,"&fトークンの特徴","menu_decor","&d三相: &f走・破・援を揃える","&7足跡泥棒: &f鬼の近くで未発見を維持","&c死線勘定: &f鬼の至近距離を無傷で切り抜ける"));
        }
        addPlayerBuildFooter(p,menu,true,page,4);
        p.openInventory(menu);
    }
    private void openPresetMenu(Player p){
        MenuHolder holder=new MenuHolder("presets");Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0プリセット一覧"));holder.inventory=menu;for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(Material.BLACK_STAINED_GLASS_PANE," ","menu_decor"));
        menu.setItem(4,menuItem(Material.BOOK,"&f&l通常プリセット","menu_decor","&7キャラクターを選ぶとメイン・パッシブを一括設定"));
        menu.setItem(11,customModelItem(Material.PLAYER_HEAD,2002,"&6&lプリセット: 伊神京也","menu_preset:IGAMI_KYOYA","&7読み: イガミキョウヤ","","&8メイン: &f破鬼撃","&8パッシブ: &f神喰 / 強靭","&8&m--------------------","&7&o「俺達は屈しない。」","","&eクリックで一括設定"));
        menu.setItem(12,customModelItem(Material.PLAYER_HEAD,2003,"&b&lプリセット: 字那美咲","menu_preset:AZANAMI_MISAKI","&7読み: アザナミサキ","","&8メイン: &f疾走","&8パッシブ: &f軽足 / 深呼吸","&8&m--------------------","&7&o「私に任せて。体力には自信があるんだ。」","","&eクリックで一括設定"));
        menu.setItem(13,customModelItem(Material.PLAYER_HEAD,2001,"&d&lプリセット: 有川風香","menu_preset:ARIKAWA_FUUKA","&7読み: アリカワフウカ","","&8メイン: &f煙幕","&8パッシブ: &f臆病 / 退魔","&8&m--------------------","&7&o「えぇ…あんなのと戦うんですか…？」","","&eクリックで一括設定"));
        menu.setItem(14,customModelItem(Material.PLAYER_HEAD,2004,"&3&lプリセット: 字那美蓮","menu_preset:AZANAMI_REN","&7読み: アザナミレン","","&8メイン: &f執念","&8パッシブ: &f軽足 / 強靭","&8&m--------------------","&7&o「鬼さんこちら…ってな！」","","&eクリックで一括設定"));
        menu.setItem(15,customModelItem(Material.PLAYER_HEAD,2005,"&a&lプリセット: 佐倉夏海","menu_preset:MEDIC","&7読み: サクラナツミ","","&8メイン: &f治療","&8パッシブ: &f集中 / 絆","&8&m--------------------","&7&o「治療は私に任せて！」","","&eクリックで一括設定"));
        menu.setItem(22,menuItem(Material.COMPASS,"&e&l戦術プリセット","menu_decor","&7相性の良いスキルをまとめたおすすめ構成"));
        menu.setItem(27,menuItem(Material.IRON_SWORD,"&c&l返し刃","menu_build:COUNTER_BLADE","&7破鬼撃＋受け流し＋神喰","&8攻撃をいなして反撃する対鬼構成","&eクリックで一括設定"));menu.setItem(28,menuItem(Material.CRYING_OBSIDIAN,"&4&l破壊工作員","menu_build:SABOTEUR","&7霊視＋手馴れ＋退魔","&8心臓発見と高速損壊に特化","&eクリックで一括設定"));menu.setItem(29,menuItem(Material.CHORUS_FRUIT,"&a&l守護走者","menu_build:GUARD_RUNNER","&7身代わり＋強靭＋軽足","&8仲間を救出して追跡を引き受ける構成","&eクリックで一括設定"));menu.setItem(30,menuItem(Material.NETHER_STAR,"&c&l背水","menu_build:LAST_STAND","&7決死行＋土壇場＋手馴れ","&8瀕死状態から心臓を押し切る構成","&eクリックで一括設定"));menu.setItem(31,menuItem(Material.AMETHYST_SHARD,"&5&l輪唱","menu_build:CHORUS","&7共鳴＋深呼吸＋軽足","&8味方のスタミナを支える集団行動構成","&eクリックで一括設定"));menu.setItem(32,menuItem(Material.ENCHANTING_TABLE,"&9&l罠師","menu_build:TRAPPER","&7封鬼陣＋息殺し＋臆病","&8罠設置後に気配を消して離脱する構成","&eクリックで一括設定"));
        menu.setItem(38,menuItem(Material.NETHER_STAR,"&c&lEXプリセット","menu_decor","&7特殊な能力構成を持つプリセット"));
        List<ItemStack> ex=new ArrayList<>();
        ex.add(customModelItem(Material.PLAYER_HEAD,2006,"&c&lEXプリセット: 赤坂陽彩","menu_preset:AKASAKA_HIIRO","&7読み: アカサカヒイロ","","&8メイン: &f安定着地","&8パッシブ: &f忍びの血統 / 跳躍","&8&m--------------------","&7&o「アタシを捕まえられるかな？」","","&eクリックで一括設定"));
        ex.add(customModelItem(Material.PLAYER_HEAD,2007,"&a&lEXプリセット: 加賀谷凛音","menu_preset:KAGAYA_RION","&7読み: カガヤリオン","","&8メイン: &f瞬歩","&8パッシブ: &f急転 / 窮鼠","&8&m--------------------","&7&o「魅せてあげるよ！」","","&eクリックで一括設定"));
        ex.add(customModelItem(Material.PLAYER_HEAD,2008,"&9&lEXプリセット: 天内伊御奈","menu_preset:AMANAI_IONA","&7読み: アマナイイオナ","","&8メイン: &f残響","&8パッシブ: &f息殺し / 陽動","&8&m--------------------","&7&o「残念ね…そっちは偽物よ。」","","&eクリックで一括設定"));
        int[] exSlots=ex.size()==4?new int[]{39,41,43,45}:new int[]{40,42,44};for(int i=0;i<ex.size();i++)menu.setItem(exSlots[i],ex.get(i));
        menu.setItem(49,menuItem(Material.ARROW,"&e&l← スキル設定へ戻る","menu_page:skills"));p.openInventory(menu);
    }
    private Material passiveMaterial(PassiveSkill passive){return switch(passive){case LIGHT_FOOTED->Material.RABBIT_FOOT;case DEEP_BREATH->Material.GHAST_TEAR;case FOCUS->Material.AMETHYST_SHARD;case ATTACK_BOOST->Material.IRON_SWORD;case DURABILITY_BOOST->Material.IRON_CHESTPLATE;case EXORCISM->Material.BLAZE_POWDER;case COWARDICE->Material.FERMENTED_SPIDER_EYE;case BOND->Material.HEART_OF_THE_SEA;case NINJA_BLOOD->Material.NETHERITE_BOOTS;case LEAP->Material.FIREWORK_ROCKET;case AFTERMIND->Material.ECHO_SHARD;case PARRY->Material.SHIELD;case QUICK_TURN->Material.ENDER_PEARL;case SILENT_BREATH->Material.SCULK_SENSOR;case LAST_RESERVE->Material.HONEY_BOTTLE;case PRACTICED->Material.CLOCK;case DECOY->Material.SNOWBALL;case CORNERED_RAT->Material.GOLDEN_CARROT;case DIVINE_TECHNIQUE->Material.NETHER_STAR;case DOUBLE_STAKES->Material.GOLD_INGOT;case THREE_PHASE->Material.AMETHYST_SHARD;case FOOTSTEP_THIEF->Material.RABBIT_FOOT;case DEADLINE_ACCOUNTING->Material.CLOCK;};}
    private void applyTacticalPreset(Player player,String id){PlayerSkill skill;PassiveSkill first,second;String name;switch(id){case "COUNTER_BLADE"->{name="返し刃";skill=PlayerSkill.ONI_STRIKE;first=PassiveSkill.PARRY;second=PassiveSkill.ATTACK_BOOST;}case "SABOTEUR"->{name="破壊工作員";skill=PlayerSkill.CLAIRVOYANCE;first=PassiveSkill.PRACTICED;second=PassiveSkill.EXORCISM;}case "GUARD_RUNNER"->{name="守護走者";skill=PlayerSkill.SUBSTITUTE;first=PassiveSkill.DURABILITY_BOOST;second=PassiveSkill.LIGHT_FOOTED;}case "LAST_STAND"->{name="背水";skill=PlayerSkill.DESPERATE_RUN;first=PassiveSkill.LAST_RESERVE;second=PassiveSkill.PRACTICED;}case "CHORUS"->{name="輪唱";skill=PlayerSkill.RESONANCE;first=PassiveSkill.DEEP_BREATH;second=PassiveSkill.LIGHT_FOOTED;}case "TRAPPER"->{name="罠師";skill=PlayerSkill.SEALING_CIRCLE;first=PassiveSkill.SILENT_BREATH;second=PassiveSkill.COWARDICE;}default->{return;}}selectedSkill.put(player.getUniqueId(),skill);selectedPresetNames.remove(player.getUniqueId());LinkedHashSet<PassiveSkill> passives=selectedPassives.computeIfAbsent(player.getUniqueId(),key->new LinkedHashSet<>());passives.clear();passives.add(first);passives.add(second);msg(player,"戦術プリセット &e"+name+" &fを設定しました。");}
    private int[] centerSlots(int min,int max,int count){if(count<=0)return new int[0];int width=max-min+1,start=min+Math.max(0,(width-count)/2);int[] out=new int[count];for(int i=0;i<count;i++)out[i]=start+i;return out;}
    private void openOniMenu(Player p){
        debugTrace("gui","GUI-201","openOniMenu player="+p.getName()+" selected="+(selectedOniType==null?"null":selectedOniType.name()));
        MenuHolder holder=new MenuHolder("oni");Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &0鬼構成"));holder.inventory=menu;
        for(int slot=0;slot<18;slot++)menu.setItem(slot,menuItem(Material.RED_STAINED_GLASS_PANE,"&4鬼選択","menu_decor"));
        for(int slot=18;slot<36;slot++)menu.setItem(slot,menuItem(Material.GRAY_STAINED_GLASS_PANE,"&8固有スキル説明","menu_decor"));
        for(int slot=36;slot<54;slot++)menu.setItem(slot,menuItem(Material.PURPLE_STAINED_GLASS_PANE,"&5鬼パッシブ枠","menu_decor"));
        if(selectedOniType!=null&&isOniTypeLocked(selectedOniType))selectedOniType=firstUnlockedOni();OniType shown=selectedOniType==null?firstUnlockedOni():selectedOniType;LinkedHashSet<OniPassiveSkill> passives=selectedOniPassives;
        List<ItemStack> normalOni=new ArrayList<>(),exOni=new ArrayList<>();
        if(!isOniTypeLocked(OniType.DAKKO))normalOni.add(customModelItem(Material.CHAINMAIL_HELMET,1001,"&5&l堕狐","menu_oni:DAKKO","&7奇襲・攪乱型","&d狐渡り &7/ &6分霊 &7/ &6狐火 &7/ &d天来",shown==OniType.DAKKO?"&a現在表示・選択中":"&eクリックして選択"));
        if(!isOniTypeLocked(OniType.KISHIN))normalOni.add(customModelItem(Material.CARVED_PUMPKIN,1203,"&4&l鬼王 &7(きおう)","menu_oni:KISHIN","&7正面突破・追跡型","&c鬼突 &7/ &4地砕 &7/ &4鬼吼 &7/ &6剛身",shown==OniType.KISHIN?"&a現在表示・選択中":"&eクリックして選択"));
        if(!isOniTypeLocked(OniType.SHIKKI))normalOni.add(customModelItem(Material.CARVED_PUMPKIN,1201,"&c&l疾鬼","menu_oni:SHIKKI","&7跳躍・高速機動・連続狩猟型","&6跳躍狩り &7/ &c血走 &7/ &c狂奔 &7/ &4狩猟連鎖",shown==OniType.SHIKKI?"&a現在表示・選択中":"&eクリックして選択"));
        if(!isOniTypeLocked(OniType.YUUKI))normalOni.add(customModelItem(Material.CARVED_PUMPKIN,1202,"&8&l幽鬼","menu_oni:YUUKI","&7隠密・奇襲型","&8幽歩 &7/ &7朧渡り &7/ &5影縫い &7/ &8神隠し",shown==OniType.YUUKI?"&a現在表示・選択中":"&eクリックして選択"));
        if(!isOniTypeLocked(OniType.KANKI))exOni.add(customModelItem(Material.TOTEM_OF_UNDYING,0,"&5&lEX鬼 鬼神 &7(オニガミ)","menu_oni:KANKI","&7召喚・指揮型","&5鬼喚び &7/ &d鬼令 &7/ &6再臨 &7/ &4百鬼夜行",shown==OniType.KANKI?"&a現在表示・選択中":"&eクリックして選択"));
        if(!isOniTypeLocked(OniType.JAKUTSUKI))exOni.add(customModelItem(Material.PLAYER_HEAD,2009,"&8&lEX鬼 蛇窟姫 &7(ジャクツキ)","menu_oni:JAKUTSUKI","&7領域制圧・終盤強化型","&5黒鏡 &7/ &5薙ぎ払い &7/ &8蛇崩 &7/ &4解放 &7/ &5黒の波動",shown==OniType.JAKUTSUKI?"&a現在表示・選択中":"&eクリックして選択"));
        menu.setItem(0,menuItem(Material.RED_BANNER,"&c&l通常鬼","menu_decor","&7LOCK中の鬼は非表示。残りを自動中央整列"));
        int[] normalSlots=centerSlots(1,7,normalOni.size());for(int i=0;i<normalOni.size();i++)menu.setItem(normalSlots[i],normalOni.get(i));
        if(!exOni.isEmpty()){menu.setItem(9,menuItem(Material.PURPLE_BANNER,"&5&lEX鬼","menu_decor","&7EX LOCKに連動して自動整列"));int[] exSlots=centerSlots(10,16,exOni.size());for(int i=0;i<exOni.size();i++)menu.setItem(exSlots[i],exOni.get(i));}
        if(shown==OniType.DAKKO){menu.setItem(20,oniSkillIconItem("dakko_tp","&d&l狐渡り","menu_decor","&7視線先へ瞬間移動","&7心臓破壊で射程低下・CT増加"));menu.setItem(24,oniSkillIconItem("dakko_clone","&6&l分霊","menu_decor","&7弱体化した堕狐鬼Botの分身体を2体召喚","&7約5秒だけ追跡・攻撃して消滅","&7心臓2個破壊で解放"));menu.setItem(29,oniSkillIconItem("dakko_fox_fire","&6&l狐火","menu_decor","&7周囲へ固定ダメージ＋炎上","&7対集団用 / 心臓4個破壊で解放"));menu.setItem(33,oniSkillIconItem("dakko_heavenly_arrival","&d&l天来","menu_decor","&7視線先の上空から飛来して範囲攻撃","&7心臓6個破壊で解放"));}
        else if(shown==OniType.KISHIN){menu.setItem(20,oniSkillIconItem("kishin_charge","&c&l鬼突","menu_decor","&7前方へ強力な突進","&7心臓破壊で速度低下・CT増加"));menu.setItem(22,oniSkillIconItem("kishin_slam","&4&l地砕","menu_decor","&7周囲を攻撃し、吹き飛ばして鈍足付与","&7心臓2個破壊で解放"));menu.setItem(24,oniSkillIconItem("kishin_roar","&4&l鬼吼","menu_decor","&7咆哮で周囲を攻撃し、鈍足＋弱体化","&c威力5 / 半径8m / CT28秒","&7心臓4個破壊で解放"));menu.setItem(31,oniSkillIconItem("kishin_iron_body","&6&l剛身","menu_decor","&76秒間、耐性I＋移動速度上昇II＋攻撃力上昇II","&7追跡・接近戦を大きく強化 / CT32秒","&7心臓6個破壊で解放"));}else if(shown==OniType.SHIKKI){menu.setItem(20,oniSkillIconItem("shikki_hunting_leap","&6&l跳躍狩り","menu_decor","&7疾鬼の主力。前方へ高速跳躍","&7CT6秒 / 跳躍後3秒以内の命中で即時再使用","&a最初から使用可能"));menu.setItem(22,oniSkillIconItem("shikki_blood_run","&c&l血走","menu_decor","&7走行中・負傷中の獲物を感知","&7次の跳躍先を選ぶ索敵 / 心臓2個破壊で解放"));menu.setItem(24,oniSkillIconItem("shikki_frenzy","&c&l狂奔","menu_decor","&7約8秒間、跳躍狩りを強化","&7跳躍CT3秒・飛距離増加・跳躍後命中で即時回復","&7心臓4個破壊で解放 / CT32秒"));menu.setItem(31,oniSkillIconItem("shikki_chain_hunt","&4&l狩猟連鎖","menu_decor","&7狂奔を延長し、跳躍狩りを即時回復","&7異なる3人への連鎖命中でフィニッシュ準備","&7心臓6個破壊で解放"));}else if(shown==OniType.KANKI){menu.setItem(20,oniSkillIconItem("kanki_summon","&5&l鬼喚び","menu_decor","&7堕狐・鬼王・疾鬼・幽鬼から1体を約22秒召喚","&7SHIFT+右クリックで召喚先を切替 / CT24秒"));menu.setItem(22,oniSkillIconItem("kanki_command","&d&l鬼令","menu_decor","&7召喚鬼へ視線先付近の獲物を優先命令","&7召喚鬼全体を再索敵 / CT8秒"));menu.setItem(24,oniSkillIconItem("kanki_recall","&6&l再臨","menu_decor","&7直前に喚んだ鬼を弱体状態で再召喚","&7持続15秒 / CT38秒"));menu.setItem(31,oniSkillIconItem("kanki_parade","&4&l百鬼夜行","menu_decor","&7歴代4鬼を同時に短時間召喚","&7持続18秒 / CT70秒"));}else if(shown==OniType.YUUKI){menu.setItem(20,oniSkillIconItem("yuuki_veil","&8&l幽歩","menu_decor","&7透明化＋足音抑制＋移動速度上昇","&7透明中は攻撃不可。再使用で解除し奇襲加速"));menu.setItem(22,oniSkillIconItem("yuuki_haze_step","&7&l朧渡り","menu_decor","&7幽歩中、前方へ短距離高速移動","&7壁抜け不可 / 心臓2個破壊で解放"));menu.setItem(24,oniSkillIconItem("yuuki_shadow_bind","&5&l影縫い","menu_decor","&7幽歩解除時、近距離の獲物を拘束","&7鈍足＋短時間スキル封印 / 心臓4個破壊で解放"));menu.setItem(31,oniSkillIconItem("yuuki_divine_hide","&8&l神隠し","menu_decor","&7幽歩を強化し、奇襲性能を上昇","&7解除後の初撃強化 / 心臓6個破壊で解放"));}else{menu.setItem(20,menuItem(Material.BLACK_STAINED_GLASS,"&5&l黒鏡","menu_decor","&7SHIFT+右クリックで鏡を設置","&7右クリックで設置地点へ転移 / 長CT"));menu.setItem(22,menuItem(Material.NETHERITE_HOE,"&5&l薙ぎ払い","menu_decor","&7前方の扇状範囲を一閃"));menu.setItem(24,menuItem(Material.SILVERFISH_SPAWN_EGG,"&8&l蛇崩","menu_decor","&7大量の蛇を一定時間召喚","&7蛇に噛まれると短時間発光"));menu.setItem(29,menuItem(Material.ECHO_SHARD,"&d&l黒の波動","menu_decor","&7溜めの後、前方へ強力な貫通範囲攻撃","&7複数のぷれいやーを一直線に貫く","&c威力16 / 射程28m / 溜め2.5秒"));menu.setItem(31,menuItem(Material.NETHER_STAR,"&4&l解放","menu_decor","&7心臓が残り1つの時のみ使用可能","&7即狂化＋脱出口を60秒封印"));menu.setItem(33,menuItem(Material.CRYING_OBSIDIAN,"&4&l蛇窟の偽心臓","menu_decor","&7本物8個＋偽物4個が配置される","&7偽物へ接近すると爆発","&cダメージ＋発光 &7/ 地形破壊なし","&7探知機・霊視でも本物と区別できない"));}
        menu.setItem(38,menuItem(Material.COMPASS,"&5&l鬼パッシブ設定","menu_oni_passive_page:1","&7役割別5ページから最大4個選択","&d索敵 / 心臓防衛 / 戦闘 / 妨害 / 機動"));
        int slot=40;for(OniPassiveSkill passive:passives){if(slot>48)break;menu.setItem(slot++,oniPassiveIconItem(passive,"&d"+passive.display,"menu_oni_passive_page:1","&7選択中"));}
        menu.setItem(53,menuItem(Material.NETHER_STAR,"&d&l鬼パッシブ &f"+passives.size()+"/4","menu_oni_passive_page:1","&eクリックして役割別ページを開く"));p.openInventory(menu);
    }
    private void openOniPassivePage(Player p,int page){
        page=Math.max(1,Math.min(5,page));MenuHolder holder=new MenuHolder("oni_passives:"+page);Inventory menu=Bukkit.createInventory(holder,54,cc("&4鬼げぇむ &8- &5鬼パッシブ ["+page+"/5]"));holder.inventory=menu;for(int slot=0;slot<54;slot++)menu.setItem(slot,menuItem(Material.BLACK_STAINED_GLASS_PANE," ","menu_decor"));
        OniPassiveSkill[] group;String title;Material icon;String desc;
        if(page==1){title="【索敵・追跡】";icon=Material.COMPASS;desc="獲物を見失わず、再発見を強化";group=new OniPassiveSkill[]{OniPassiveSkill.BLOOD_SCENT,OniPassiveSkill.HUNT_RECORD,OniPassiveSkill.EXECUTION,OniPassiveSkill.SCENT_TRAIL,OniPassiveSkill.HUNTERS_INSTINCT,OniPassiveSkill.FOOTSTEP_HUNTER,OniPassiveSkill.OBSESSION_HUNT};}
        else if(page==2){title="【心臓防衛】";icon=Material.CRYING_OBSIDIAN;desc="心臓への攻撃へ反応し、終盤を守る";group=new OniPassiveSkill[]{OniPassiveSkill.GUARDIAN,OniPassiveSkill.INTERFERENCE,OniPassiveSkill.PULSE,OniPassiveSkill.BLOOD_MARK,OniPassiveSkill.HEART_EYE,OniPassiveSkill.CURSED_VEIN,OniPassiveSkill.BACKFLOW,OniPassiveSkill.LAST_FORTRESS};}
        else if(page==3){title="【戦闘・制圧】";icon=Material.NETHERITE_AXE;desc="接近戦・連撃・圧力を強化";group=new OniPassiveSkill[]{OniPassiveSkill.PREDATION,OniPassiveSkill.MOMENTUM,OniPassiveSkill.ANCHOR,OniPassiveSkill.BLOOD_FRENZY,OniPassiveSkill.PRESSURE,OniPassiveSkill.HUNTING_GROUND,OniPassiveSkill.FINISHER_CHASE};}
        else if(page==4){title="【妨害・対スキル】";icon=Material.ENCHANTING_TABLE;desc="ぷれいやーのスキルと妨害へ対抗";group=new OniPassiveSkill[]{OniPassiveSkill.GRUDGE_RETURN,OniPassiveSkill.SEE_THROUGH,OniPassiveSkill.SPELL_BREAK,OniPassiveSkill.SKILL_SEAL,OniPassiveSkill.ADAPTATION};}
        else{title="【機動・特殊】";icon=Material.FEATHER;desc="追跡初動・帰還・スキル回転を強化";group=new OniPassiveSkill[]{OniPassiveSkill.CRAVING,OniPassiveSkill.MASTERY,OniPassiveSkill.BEAST_PATH,OniPassiveSkill.AMBUSH,OniPassiveSkill.TERRITORY,OniPassiveSkill.HOMING};}
        menu.setItem(4,menuItem(icon,"&d&l"+title,"menu_decor","&7"+desc));int[] slots={10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};for(int i=0;i<group.length;i++){OniPassiveSkill passive=group[i];menu.setItem(slots[i],oniPassiveIconItem(passive,"&5&l"+passive.display,"menu_oni_passive:"+passive.name(),"&7"+passive.description,selectedOniPassives.contains(passive)?"&a✔ 選択中":"&eクリックして選択"));}
        menu.setItem(45,menuItem(Material.BARRIER,"&c&l← 鬼選択へ","menu_oni_back","&7鬼種・固有スキル画面へ戻る"));menu.setItem(47,menuItem(Material.ARROW,page>1?"&e&l← 前ページ":"&8← 前ページ",page>1?"menu_oni_passive_page:"+(page-1):"menu_decor"));menu.setItem(49,menuItem(Material.NETHER_STAR,"&d&l選択中 &f"+selectedOniPassives.size()+"/4","menu_decor","&7最大4個まで設定可能"));menu.setItem(51,menuItem(Material.ARROW,page<5?"&e&l次ページ →":"&8次ページ →",page<5?"menu_oni_passive_page:"+(page+1):"menu_decor"));menu.setItem(53,menuItem(Material.BOOK,"&f&lページ "+page+"/5","menu_decor","&7役割ごとに鬼パッシブを分類"));p.openInventory(menu);
    }
    private Material oniPassiveMaterial(OniPassiveSkill passive){return switch(passive){
        case CRAVING->Material.ROTTEN_FLESH;case BLOOD_SCENT->Material.REDSTONE;case GUARDIAN->Material.CRYING_OBSIDIAN;case PREDATION->Material.BONE;case INTERFERENCE->Material.TRIPWIRE_HOOK;case MASTERY->Material.CLOCK;case MOMENTUM->Material.SUGAR;case ANCHOR->Material.ANVIL;case PULSE->Material.ECHO_SHARD;case EXECUTION->Material.SPECTRAL_ARROW;case HUNT_RECORD->Material.WRITABLE_BOOK;case GRUDGE_RETURN->Material.AMETHYST_SHARD;case BLOOD_MARK->Material.REDSTONE_TORCH;
        case SCENT_TRAIL->Material.CRIMSON_FUNGUS;case HUNTERS_INSTINCT->Material.COMPASS;case FOOTSTEP_HUNTER->Material.LEATHER_BOOTS;case OBSESSION_HUNT->Material.RED_DYE;case HEART_EYE->Material.ENDER_EYE;case CURSED_VEIN->Material.FERMENTED_SPIDER_EYE;case BACKFLOW->Material.FIREWORK_STAR;case LAST_FORTRESS->Material.OBSIDIAN;case BLOOD_FRENZY->Material.NETHERITE_SWORD;case PRESSURE->Material.SOUL_SAND;case HUNTING_GROUND->Material.TARGET;case FINISHER_CHASE->Material.GOLDEN_AXE;case SEE_THROUGH->Material.SPYGLASS;case SPELL_BREAK->Material.AMETHYST_CLUSTER;case SKILL_SEAL->Material.BARRIER;case ADAPTATION->Material.SCULK_CATALYST;case BEAST_PATH->Material.RABBIT_FOOT;case AMBUSH->Material.FIREWORK_ROCKET;case TERRITORY->Material.RESPAWN_ANCHOR;case HOMING->Material.RECOVERY_COMPASS;};}
    private ItemStack createHeartMarker(){ItemStack i=item(Material.ARMOR_STAND,"&4&l鬼の心臓地点マーカー","heart_marker");ItemMeta m=i.getItemMeta();m.setLore(List.of(cc("&7設置地点に心臓を生成します。"),cc("&7設置後は透明・固定・無敵になります。")));i.setItemMeta(m);return i;}
    private void armorCheckLine(Player p,String slot,ItemStack i){if(i==null||i.getType()==Material.AIR){msg(p,"&c"+slot+": EMPTY");return;}ItemMeta m=i.getItemMeta();Integer cmd=m!=null&&m.hasCustomModelData()?m.getCustomModelData():null;String id=m==null?null:m.getPersistentDataContainer().get(equipmentIdKey,PersistentDataType.STRING);msg(p,"&f"+slot+": &b"+i.getType()+" &7/ CMD="+(cmd==null?"-":cmd)+" / ID="+(id==null?"-":id));}
    private void loadEquipmentRegistry(){
        File file=new File(getDataFolder(),"equipment.yml");
        if(!file.exists())saveResource("equipment.yml",false);
        FileConfiguration y=YamlConfiguration.loadConfiguration(file);equipmentRegistry.clear();Map<Integer,String> used=new HashMap<>();
        var root=y.getConfigurationSection("equipment");if(root==null){getLogger().warning("equipment.yml に equipment セクションがありません。");return;}
        for(String id:root.getKeys(false)){
            String path="equipment."+id+".";Material material=Material.matchMaterial(y.getString(path+"material","DIAMOND_HELMET"));if(material==null){getLogger().warning("Unknown equipment material: "+id);continue;}
            int cmd=y.getInt(path+"custom-model-data",0);String name=y.getString(path+"name",id);List<String> lore=y.getStringList(path+"lore");
            if(cmd>0&&used.containsKey(cmd))getLogger().warning("Equipment CMD conflict: "+cmd+" -> "+used.get(cmd)+" / "+id);else if(cmd>0)used.put(cmd,id);
            equipmentRegistry.put(id,new EquipmentDef(id,material,cmd,name,lore));
        }
        getLogger().info("Equipment registry loaded: "+equipmentRegistry.size()+" entries");
    }
    private ItemStack registeredEquipment(String id){
        EquipmentDef d=equipmentRegistry.get(id);if(d==null){getLogger().warning("Unknown equipment id: "+id);return new ItemStack(Material.AIR);}
        ItemStack i=new ItemStack(d.material());ItemMeta m=i.getItemMeta();m.setDisplayName(cc(d.name()));m.setUnbreakable(true);if(d.customModelData()>0)m.setCustomModelData(d.customModelData());
        String canonicalId=d.id().replaceFirst("^oni_","oni.").replace("_helmet",".helmet").replace("_chestplate",".chestplate").replace("_leggings",".leggings").replace("_boots",".boots");m.getPersistentDataContainer().set(equipmentIdKey,PersistentDataType.STRING,canonicalId);m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"equipment:"+canonicalId);
        if(!d.lore().isEmpty()){List<String> lore=new ArrayList<>();for(String line:d.lore())lore.add(cc(line));m.setLore(lore);}i.setItemMeta(m);return i;
    }
    private String oniArmorId(OniType type,String slot){return "oni_"+type.name().toLowerCase(Locale.ROOT)+"_"+slot;}
    private ItemStack oniArmorPiece(OniType type,String slot,Material fallback,int cmd){ItemStack i=registeredEquipment(oniArmorId(type,slot));if(i==null||i.getType()==Material.AIR){i=new ItemStack(fallback);ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setCustomModelData(cmd);String canonical="oni."+type.name().toLowerCase(Locale.ROOT)+"."+slot;m.getPersistentDataContainer().set(equipmentIdKey,PersistentDataType.STRING,canonical);m.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"equipment:"+canonical);i.setItemMeta(m);getLogger().warning("Armor registry fallback used: "+canonical);}return i;}
    // v0.40.39: worn armor appearance is restored to the pre-registry materials/CMDs.
    // equipment_id (PDC) remains the authoritative game-side identity.
    private ItemStack tagEquipmentId(ItemStack item,String id){
        if(item==null||id==null)return item;ItemMeta m=item.getItemMeta();
        m.getPersistentDataContainer().set(equipmentIdKey,PersistentDataType.STRING,id);item.setItemMeta(m);return item;
    }
    private ItemStack legacyOniPiece(OniType type,String part){
        if(type==null)return null;
        return switch(type){
            case DAKKO -> switch(part){
                case "helmet" -> tagEquipmentId(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"),"oni.dakko.helmet");
                case "chestplate" -> tagEquipmentId(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"),"oni.dakko.chestplate");
                case "leggings" -> tagEquipmentId(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"),"oni.dakko.leggings");
                case "boots" -> tagEquipmentId(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"),"oni.dakko.boots"); default -> null;};
            case SHIKKI -> switch(part){
                case "helmet" -> tagEquipmentId(oniArmorWithModel(Material.DIAMOND_HELMET,"&c疾鬼の兜",1210),"oni.shikki.helmet");
                case "chestplate" -> tagEquipmentId(oniArmorWithModel(Material.DIAMOND_CHESTPLATE,"&4疾鬼の装束",1211),"oni.shikki.chestplate");
                case "leggings" -> tagEquipmentId(oniArmorWithModel(Material.DIAMOND_LEGGINGS,"&8疾鬼の袴",1212),"oni.shikki.leggings");
                case "boots" -> tagEquipmentId(oniArmorWithModel(Material.DIAMOND_BOOTS,"&4疾鬼の足具",1213),"oni.shikki.boots"); default -> null;};
            case YUUKI -> switch(part){
                case "helmet" -> tagEquipmentId(dyedOniArmor(Material.GOLDEN_HELMET,org.bukkit.Color.fromRGB(35,22,48),"&5幽鬼の兜",1220),"oni.yuuki.helmet");
                case "chestplate" -> tagEquipmentId(dyedOniArmor(Material.GOLDEN_CHESTPLATE,org.bukkit.Color.fromRGB(35,22,48),"&5幽鬼の装束",1221),"oni.yuuki.chestplate");
                case "leggings" -> tagEquipmentId(dyedOniArmor(Material.GOLDEN_LEGGINGS,org.bukkit.Color.fromRGB(18,17,24),"&8幽鬼の袴",1222),"oni.yuuki.leggings");
                case "boots" -> tagEquipmentId(dyedOniArmor(Material.GOLDEN_BOOTS,org.bukkit.Color.fromRGB(52,31,67),"&5幽鬼の足具",1223),"oni.yuuki.boots"); default -> null;};
            case JAKUTSUKI -> switch(part){
                case "helmet" -> tagEquipmentId(jakutsukiArmor(Material.IRON_HELMET,"&5蛇窟姫の髪飾り"),"oni.jakutsuki.helmet");
                case "chestplate" -> tagEquipmentId(jakutsukiArmor(Material.IRON_CHESTPLATE,"&5蛇窟姫の装束"),"oni.jakutsuki.chestplate");
                case "leggings" -> tagEquipmentId(jakutsukiArmor(Material.IRON_LEGGINGS,"&5蛇窟姫の袴"),"oni.jakutsuki.leggings");
                case "boots" -> tagEquipmentId(jakutsukiArmor(Material.IRON_BOOTS,"&5蛇窟姫の足袋"),"oni.jakutsuki.boots"); default -> null;};
            // These two types had no dedicated worn armor in the stable pre-registry implementation.
            case KISHIN, KANKI -> null;
        };
    }
    private void equipLegacyOniArmor(Player p,OniType type){
        p.getInventory().setHelmet(legacyOniPiece(type,"helmet"));p.getInventory().setChestplate(legacyOniPiece(type,"chestplate"));
        p.getInventory().setLeggings(legacyOniPiece(type,"leggings"));p.getInventory().setBoots(legacyOniPiece(type,"boots"));
    }
    private void equipLegacyOniArmor(LivingEntity e,OniType type){
        if(e.getEquipment()==null)return;e.getEquipment().setHelmet(legacyOniPiece(type,"helmet"));e.getEquipment().setChestplate(legacyOniPiece(type,"chestplate"));
        e.getEquipment().setLeggings(legacyOniPiece(type,"leggings"));e.getEquipment().setBoots(legacyOniPiece(type,"boots"));
    }
    private void equipDakkoArmor(Player p){equipLegacyOniArmor(p,OniType.DAKKO);}
    private ItemStack oniArmor(Material material,String name){ItemStack i=item(material,name,"dakko_armor");ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setCustomModelData(dakkoArmorModel(material));m.setLore(List.of(cc("&8堕狐専用装備")));i.setItemMeta(m);return i;}
    private int dakkoArmorModel(Material material){return switch(material){case CHAINMAIL_HELMET->1001;case CHAINMAIL_CHESTPLATE->1002;case CHAINMAIL_LEGGINGS->1003;case CHAINMAIL_BOOTS->1004;default->1001;};}
    private boolean isDakkoArmor(ItemStack i){if(i==null||!i.hasItemMeta())return false;return "dakko_armor".equals(i.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING));}
    private ItemStack oniMask(int model,String name){ItemStack i=item(Material.CARVED_PUMPKIN,name,"oni_costume");ItemMeta m=i.getItemMeta();m.setCustomModelData(model);m.setUnbreakable(true);m.setLore(List.of(cc("&8鬼専用外見")));i.setItemMeta(m);return i;}
    private ItemStack dyedOniArmor(Material material,org.bukkit.Color color,String name){return dyedOniArmor(material,color,name,null);}
    private ItemStack dyedOniArmor(Material material,org.bukkit.Color color,String name,Integer customModelData){ItemStack i=item(material,name,"oni_costume");ItemMeta meta=i.getItemMeta();meta.setUnbreakable(true);meta.setLore(List.of(cc("&8鬼専用外見")));if(customModelData!=null)meta.setCustomModelData(customModelData);if(meta instanceof LeatherArmorMeta leather)leather.setColor(color);i.setItemMeta(meta);return i;}
    private ItemStack oniArmorWithModel(Material material,String name,int customModelData){ItemStack i=oniArmor(material,name);ItemMeta meta=i.getItemMeta();meta.setCustomModelData(customModelData);i.setItemMeta(meta);return i;}
    private void equipShikkiArmor(Player p){equipLegacyOniArmor(p,OniType.SHIKKI);}
    private void equipYuukiArmor(Player p){equipLegacyOniArmor(p,OniType.YUUKI);}
    private void equipShikkiArmor(LivingEntity e){equipLegacyOniArmor(e,OniType.SHIKKI);}
    private void equipYuukiArmor(LivingEntity e){equipLegacyOniArmor(e,OniType.YUUKI);}
    private void equipJakutsukiArmor(Player p){equipLegacyOniArmor(p,OniType.JAKUTSUKI);}
    private ItemStack jakutsukiArmor(Material material,String name){ItemStack i=item(material,name,"jakutsuki_armor");ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setCustomModelData(jakutsukiArmorModel(material));m.setLore(List.of(cc("&8蛇窟姫専用装備"),cc("&7従来外見互換")));i.setItemMeta(m);return i;}
    private int jakutsukiArmorModel(Material material){return switch(material){case IRON_HELMET->1101;case IRON_CHESTPLATE->1102;case IRON_LEGGINGS->1103;case IRON_BOOTS->1104;default->1101;};}
    private boolean isJakutsukiArmor(ItemStack i){if(i==null||!i.hasItemMeta())return false;return "jakutsuki_armor".equals(i.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING));}
    private boolean isTestGm(Player p){return state==GameState.RUNNING&&testParticipant!=null&&testParticipant.equals(p.getUniqueId());}
    private void giveTestPlayBook(Player p){p.getInventory().setItem(8,createTestPlayBook());msg(p,"&eテストプレイ本 &fをホットバーへ配布しました。");}
    private ItemStack createGmBook(){
        ItemStack book=new ItemStack(Material.WRITTEN_BOOK);BookMeta m=(BookMeta)book.getItemMeta();
        m.title(Component.text("鬼げぇむ GM操作本",NamedTextColor.DARK_RED));m.author(Component.text("鬼げぇむ"));
        Component page1=Component.text("【心臓の配置方式】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ ランダム配置\n",NamedTextColor.GREEN,"/og heartmode random"))
                .append(Component.text("登録候補から設定個数を抽選\n\n",NamedTextColor.GRAY))
                .append(button("▶ 手動配置\n",NamedTextColor.GOLD,"/og heartmode manual"))
                .append(Component.text("専用マーカーの全地点に生成",NamedTextColor.GRAY));
        Component page2=Component.text("【心臓マーカー】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ マーカーを受け取る\n",NamedTextColor.GOLD,"/og marker"))
                .append(button("▶ マーカー数を確認\n",NamedTextColor.AQUA,"/og markers"))
                .append(Component.text("\n設置したマーカーは自動で透明になります。",NamedTextColor.GRAY));
        Component page3=Component.text("【ゲーム操作】\n\n",NamedTextColor.DARK_RED)
                .append(button("▶ 鬼を選択\n",NamedTextColor.GOLD,"/og onimenu"))
                .append(button("▶ 選択した鬼で開始\n",NamedTextColor.RED,"/og start"))
                .append(button("▶ 状態を確認\n",NamedTextColor.AQUA,"/og status"))
                .append(button("▶ ゲームを停止",NamedTextColor.DARK_RED,"/og stop"));
        Component page4=Component.text("【ぷれいやーBotテスト】\n\n",NamedTextColor.DARK_RED)
                .append(Component.text("GMがぷれいやーになり、鬼Botと対戦します。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 堕狐Bot戦を開始\n",NamedTextColor.LIGHT_PURPLE,"/og botgame dakko"))
                .append(button("▶ 鬼王Bot戦を開始",NamedTextColor.RED,"/og botgame kio"));
        Component page5=Component.text("【鬼対ぷれいやーBot】\n\n",NamedTextColor.DARK_RED)
                .append(Component.text("GMが鬼になり、革装備のぷれいやーBot 5体と対戦します。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 堕狐で開始\n",NamedTextColor.LIGHT_PURPLE,"/og onibotgame dakko 5"))
                .append(button("▶ 鬼王で開始",NamedTextColor.RED,"/og onibotgame kio 5"));
        Component page6=Component.text("【ルートチェスト】\n\n",NamedTextColor.GOLD)
                .append(Component.text("現在地をランダム配置候補として登録します。候補なしなら心臓周辺へ自動配置。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 現在地を追加/削除\n",NamedTextColor.GOLD,"/og chest"))
                .append(button("▶ 候補数を確認",NamedTextColor.AQUA,"/og chests"));
        Component page7=Component.text("【スキル設定チェスト】\n\n",NamedTextColor.BLUE)
                .append(Component.text("ロビーのチェストへ視点を合わせて登録してください。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 視線先チェストを登録",NamedTextColor.AQUA,"/og skillchest"));
        Component page8=Component.text("【鬼設定チェスト】\n\n",NamedTextColor.DARK_PURPLE)
                .append(Component.text("鬼選択・固有スキル説明・鬼パッシブ4枠を扱います。\n\n",NamedTextColor.GRAY))
                .append(button("▶ 視線先チェストを登録\n",NamedTextColor.LIGHT_PURPLE,"/og onichest"))
                .append(button("▶ 鬼構成UIを開く",NamedTextColor.GOLD,"/og onimenu"));
        Component page9=Component.text("【テスト区域の境界】\n\n",NamedTextColor.DARK_AQUA)
                .append(Component.text("200×200の外周にワールドボーダーを表示するか選べます。\n\n",NamedTextColor.GRAY))
                .append(button("▶ ボーダーを有効化\n",NamedTextColor.GREEN,"/og testborder on"))
                .append(button("▶ ボーダーを無効化",NamedTextColor.RED,"/og testborder off"));
        m.pages(List.of(page1,page2,page3,page4,page5,page6,page7,page8,page9));book.setItemMeta(m);return book;
    }
    private ItemStack createTestPlayBook(){ItemStack book=new ItemStack(Material.WRITTEN_BOOK);BookMeta m=(BookMeta)book.getItemMeta();m.title(Component.text("鬼げぇむ テストプレイ",NamedTextColor.GOLD));m.author(Component.text("鬼げぇむ"));Component page1=Component.text("【心臓テスト】\n\n",NamedTextColor.DARK_RED).append(Component.text("破壊したい心臓へ視点を合わせて押してください。\n\n",NamedTextColor.GRAY)).append(button("▶ 視線先の心臓を破壊\n",NamedTextColor.RED,"/og testdestroy")).append(button("▶ 現在状態を確認",NamedTextColor.AQUA,"/og status"));Component page2=Component.text("【BGMテスト】\n\n",NamedTextColor.DARK_PURPLE).append(button("▶ 最終局面BGMを再生\n",NamedTextColor.GREEN,"/og bgmtest")).append(button("▶ BGMを停止",NamedTextColor.RED,"/og bgmstop"));Component page3=Component.text(playerSideTest?"【鬼Botテスト】\n\n":"【鬼スキル確認】\n\n",NamedTextColor.DARK_RED).append(Component.text(playerSideTest?"あなたはぷれいやーです。\n堕狐Botは狐火・天来を含む固有技を使います。":"堕狐\n2: 狐渡り\n3: 分霊\n4: 狐火\n5: 天来\n\n鬼王\n2: 鬼突\n3: 地砕\n4: 鬼吼\n5: 剛身\n\n各アイテムを右クリック",NamedTextColor.GRAY));Component page4=Component.text("【テスト終了】\n\n",NamedTextColor.DARK_RED).append(Component.text("装備・速度・BGM・一時参加状態を解除してロビーへ戻ります。\n\n",NamedTextColor.GRAY)).append(button("▶ テストプレイを終了",NamedTextColor.RED,"/og stop"));m.pages(List.of(page1,page2,page3,page4));book.setItemMeta(m);return book;}
    private Component button(String text,NamedTextColor color,String command){return Component.text(text,color).clickEvent(ClickEvent.runCommand(command));}
    private List<Location> selectRandomHearts(){List<Location> locations=new ArrayList<>(getConfig().getStringList("locations.hearts").stream().map(LocationStore::decode).filter(Objects::nonNull).toList());Collections.shuffle(locations);boolean duo=getConfig().getString("game-mode","normal").equalsIgnoreCase("duo");int wanted=oniType==OniType.JAKUTSUKI?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8))+Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4)):Math.max(1,getConfig().getInt(duo?"duo-mode.heart-placement-count":"normal-mode.heart-placement-count",duo?14:10));int count=Math.min(wanted,locations.size());return new ArrayList<>(locations.subList(0,count));}
    private List<Location> scanHeartMarkers(boolean hide){
        String markerName=getConfig().getString("heart-marker-name","鬼の心臓地点");Map<String,Location> unique=new LinkedHashMap<>();
        for(World world:Bukkit.getWorlds())for(ArmorStand stand:world.getEntitiesByClass(ArmorStand.class))if(markerName.equals(stand.getCustomName())){if(hide)configureMarker(stand);Location l=stand.getLocation().getBlock().getLocation();unique.put(LocationStore.encode(l),l);}
        return new ArrayList<>(unique.values());
    }
    private Vector horizontalDirection(Location base){Vector direction=base.getDirection().setY(0);return direction.lengthSquared()<0.01?new Vector(0,0,1):direction.normalize();}
    private boolean insideConfiguredLobbyArea(Location l){
        if(l==null)return false;Location a=LocationStore.get(getConfig(),"locations.lobby-area-pos1"),b=LocationStore.get(getConfig(),"locations.lobby-area-pos2");
        if(a==null||b==null||a.getWorld()!=l.getWorld()||b.getWorld()!=l.getWorld())return false;
        double minX=Math.min(a.getX(),b.getX()),maxX=Math.max(a.getX(),b.getX())+1,minY=Math.min(a.getY(),b.getY()),maxY=Math.max(a.getY(),b.getY())+1,minZ=Math.min(a.getZ(),b.getZ()),maxZ=Math.max(a.getZ(),b.getZ())+1;
        return l.getX()>=minX&&l.getX()<maxX&&l.getY()>=minY&&l.getY()<maxY&&l.getZ()>=minZ&&l.getZ()<maxZ;
    }
    private boolean unsafePlacementGround(Material m){String n=m.name();return !m.isSolid()||n.contains("LEAVES")||n.contains("MAGMA")||n.contains("CACTUS")||n.contains("FIRE")||n.contains("CAMPFIRE")||n.contains("POWDER_SNOW")||n.contains("ICE");}
    private boolean safeAutomaticSurface(Location feet,int clearanceRadius){
        if(feet==null||feet.getWorld()==null)return false;World w=feet.getWorld();int maxY=Math.min(getConfig().getInt("arena-setup.spawn-max-y",90),w.getMaxHeight()-3);if(feet.getBlockY()>maxY)return false;if(insideConfiguredLobbyArea(feet))return false;
        Block ground=feet.clone().add(0,-1,0).getBlock();if(unsafePlacementGround(ground.getType())||ground.isLiquid())return false;
        if(!feet.getBlock().isPassable()||feet.getBlock().isLiquid()||!feet.clone().add(0,1,0).getBlock().isPassable()||feet.clone().add(0,1,0).getBlock().isLiquid())return false;
        // 浮遊床・空中ロビー対策: 足場の直下に長い空洞がある候補を拒否する。
        int support=Math.max(3,getConfig().getInt("arena-setup.safety.minimum-solid-depth",3));for(int d=1;d<=support;d++){Block b=w.getBlockAt(ground.getX(),ground.getY()-d,ground.getZ());if(!b.getType().isSolid()||b.isLiquid())return false;}
        int r=Math.max(0,clearanceRadius);for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){Location q=feet.clone().add(dx,0,dz);if(q.getBlock().isLiquid()||q.clone().add(0,1,0).getBlock().isLiquid())return false;}
        return true;
    }
    private boolean waterNearSurface(World w,int x,int z,int radius){
        int r=Math.max(0,radius);
        for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(dx*dx+dz*dz>r*r)continue;
            int sx=x+dx,sz=z+dz;
            int y=w.getHighestBlockYAt(sx,sz,HeightMap.MOTION_BLOCKING_NO_LEAVES);
            for(int dy=-1;dy<=2;dy++){Block b=w.getBlockAt(sx,y+dy,sz);if(b.isLiquid()||b.getType()==Material.WATER)return true;}
        }
        return false;
    }
    private Location safeSurfaceAt(World w,int x,int z,int clearanceRadius){return safeSurfaceAt(w,x,z,clearanceRadius,Math.max(0,getConfig().getInt("arena-setup.safety.general-water-distance",4)));}
    private Location safeSurfaceAt(World w,int x,int z,int clearanceRadius,int waterDistance){
        if(w==null)return null;int maxY=Math.min(getConfig().getInt("arena-setup.spawn-max-y",90),w.getMaxHeight()-3);int top=w.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);if(top>=maxY)return null;Location feet=new Location(w,x+.5,top+1,z+.5);if(!safeAutomaticSurface(feet,clearanceRadius))return null;if(waterDistance>0&&waterNearSurface(w,x,z,waterDistance))return null;return feet;
    }
    private void configureNormalArena(Player admin){
        Location center=admin.getLocation().clone();
        int size=Math.max(40,getConfig().getInt("arena-setup.size",200)),half=size/2-5;
        int normalHearts=Math.max(1,getConfig().getInt("normal-mode.heart-placement-count",10));
        int duoHearts=Math.max(1,getConfig().getInt("duo-mode.heart-placement-count",14));
        int jakutsukiHearts=Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8))+Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4));
        int heartCount=Math.max(Math.max(normalHearts,duoHearts),Math.max(jakutsukiHearts,getConfig().getInt("arena-setup.heart-candidate-count",16)));
        Random random=new Random();List<Location> occupied=new ArrayList<>();
        Location playerSpawn=randomSpawnSurface(center,random,half,occupied,0);occupied.add(playerSpawn);
        Location oniSpawn=randomSpawnSurfaceFarFrom(center,random,half,occupied,Math.min(70,size*.35));occupied.add(oniSpawn);
        Location exit=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(38,size*.19));occupied.add(exit);
        Location exit2=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(55,size*.275));occupied.add(exit2);
        List<String> hearts=new ArrayList<>();
        for(int i=0;i<heartCount;i++){Location heart=randomTestSurface(center,random,half,occupied,Math.min(14,size*.07));occupied.add(heart);hearts.add(LocationStore.encode(heart.getBlock().getLocation()));}
        LocationStore.set(getConfig(),"arena-setup.center",center);
        LocationStore.set(getConfig(),"locations.player-spawn",playerSpawn);
        LocationStore.set(getConfig(),"locations.oni-spawn",oniSpawn);
        LocationStore.set(getConfig(),"locations.exit",exit);
        LocationStore.set(getConfig(),"locations.exit2",exit2);
        getConfig().set("locations.hearts",hearts);
        getConfig().set("heart-placement-mode","random");
        getConfig().set("arena-setup.enabled",true);
        saveConfig();
        msg(admin,"&a通常試合用アリーナを一括設定しました。");
        msg(admin,"&7範囲: &e"+size+"x"+size+" &7/ 心臓候補: &e"+hearts.size()+"個");
        msg(admin,"&7ぷれいやー開始・鬼開始・脱出口A/B・心臓候補・ワールドボーダー中心を自動設定しました。");
    }
    private void applyNormalArenaWorldBorder(){
        if(!getConfig().getBoolean("arena-setup.enabled",false)||!getConfig().getBoolean("arena-setup.world-border-enabled",true))return;
        Location center=LocationStore.get(getConfig(),"arena-setup.center");if(center==null||center.getWorld()==null)return;
        WorldBorder border=center.getWorld().getWorldBorder();
        if(savedTestWorldBorder==null)savedTestWorldBorder=new SavedWorldBorder(center.getWorld(),border.getCenter().clone(),border.getSize(),border.getWarningDistance(),border.getWarningTime(),border.getDamageAmount(),border.getDamageBuffer());
        border.setCenter(center.getX(),center.getZ());
        border.setSize(Math.max(40,getConfig().getDouble("arena-setup.size",200)));
        border.setWarningDistance(Math.max(0,getConfig().getInt("arena-setup.world-border-warning-distance",8)));
        border.setWarningTime(0);border.setDamageAmount(0);
    }
    private TestArenaLayout createRandomTestArena(Location center){
        Random random=new Random();
        int size=Math.max(40,getConfig().getInt("test-arena.size",200)),half=size/2-5;
        boolean duo=getConfig().getString("game-mode","normal").equalsIgnoreCase("duo");
        int heartCount=oniType==OniType.JAKUTSUKI
                ?Math.max(1,getConfig().getInt("jakutsuki-hearts.real-count",8))+Math.max(0,getConfig().getInt("jakutsuki-hearts.fake-count",4))
                :Math.max(1,getConfig().getInt(duo?"duo-mode.heart-placement-count":"normal-mode.heart-placement-count",duo?14:10));
        int lootCount=Math.max(1,getConfig().getInt("test-arena.loot-chest-count",5));
        List<Location> occupied=new ArrayList<>();
        Location player=randomSpawnSurface(center,random,half,occupied,0);occupied.add(player);
        Location oni=randomSpawnSurfaceFarFrom(center,random,half,occupied,Math.min(70,size*.35));occupied.add(oni);
        Location exit=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(38,size*.19));occupied.add(exit);
        Location exit2=randomTestSurfaceFarFrom(center,random,half,occupied,Math.min(55,size*.275));occupied.add(exit2);
        List<Location> hearts=new ArrayList<>();
        for(int i=0;i<heartCount;i++){Location location=randomTestSurface(center,random,half,occupied,Math.min(14,size*.07));hearts.add(location);occupied.add(location);}
        List<Location> loot=new ArrayList<>();
        for(int i=0;i<lootCount;i++){Location location=randomTestSurface(center,random,half,occupied,Math.min(8,size*.04));loot.add(location);occupied.add(location);}
        return new TestArenaLayout(player,oni,exit,exit2,hearts,loot);
    }
    private void applyTestWorldBorder(Location center){WorldBorder border=center.getWorld().getWorldBorder();if(savedTestWorldBorder==null)savedTestWorldBorder=new SavedWorldBorder(center.getWorld(),border.getCenter().clone(),border.getSize(),border.getWarningDistance(),border.getWarningTime(),border.getDamageAmount(),border.getDamageBuffer());border.setCenter(center.getX(),center.getZ());border.setSize(Math.max(40,getConfig().getDouble("test-arena.size",200)));border.setWarningDistance(Math.max(0,getConfig().getInt("test-arena.world-border-warning-distance",8)));border.setWarningTime(0);border.setDamageAmount(0);}
    private void restoreTestWorldBorder(){if(savedTestWorldBorder==null)return;SavedWorldBorder saved=savedTestWorldBorder;WorldBorder border=saved.world().getWorldBorder();border.setCenter(saved.center().getX(),saved.center().getZ());border.setSize(saved.size());border.setWarningDistance(saved.warningDistance());border.setWarningTime(saved.warningTime());border.setDamageAmount(saved.damageAmount());border.setDamageBuffer(saved.damageBuffer());savedTestWorldBorder=null;}
    private Location randomSpawnSurfaceFarFrom(Location center,Random random,int half,List<Location> occupied,double minDistance){for(int attempt=0;attempt<140;attempt++){Location location=randomSpawnSurface(center,random,half,List.of(),0);boolean far=true;for(Location other:occupied)if(other.getWorld().equals(location.getWorld())&&other.distanceSquared(location)<minDistance*minDistance){far=false;break;}if(far)return location;}return randomSpawnSurface(center,random,half,occupied,8);}
    private Location randomSpawnSurface(Location center,Random random,int half,List<Location> occupied,double separation){World world=center.getWorld();for(int attempt=0;attempt<260;attempt++){int x=center.getBlockX()+random.nextInt(half*2+1)-half,z=center.getBlockZ()+random.nextInt(half*2+1)-half;Location location=safeSurfaceAt(world,x,z,2,Math.max(8,getConfig().getInt("arena-setup.safety.spawn-water-distance",12)));if(location==null)continue;boolean clear=true;for(Location other:occupied)if(other.getWorld().equals(world)&&other.distanceSquared(location)<separation*separation){clear=false;break;}if(clear){location.setYaw(center.getYaw());location.setPitch(center.getPitch());return location;}}throw new IllegalStateException("安全な自動スポーン地点を確保できませんでした");}
    private Location randomTestSurfaceFarFrom(Location center,Random random,int half,List<Location> occupied,double minDistance){for(int attempt=0;attempt<100;attempt++){Location location=randomTestSurface(center,random,half,List.of(),0);boolean far=true;for(Location other:occupied)if(other.getWorld().equals(location.getWorld())&&other.distanceSquared(location)<minDistance*minDistance){far=false;break;}if(far)return location;}return randomTestSurface(center,random,half,occupied,8);}
    private Location randomTestSurface(Location center,Random random,int half,List<Location> occupied,double separation){World world=center.getWorld();for(int attempt=0;attempt<220;attempt++){int x=center.getBlockX()+random.nextInt(half*2+1)-half,z=center.getBlockZ()+random.nextInt(half*2+1)-half;Location location=safeSurfaceAt(world,x,z,1);if(location==null)continue;boolean clear=true;for(Location other:occupied)if(other.getWorld().equals(world)&&other.distanceSquared(location)<separation*separation){clear=false;break;}if(clear)return location;}throw new IllegalStateException("安全な自動配置地点を確保できませんでした");}
    private List<Location> createAutomaticTestHearts(Location base){Vector forward=horizontalDirection(base),right=new Vector(-forward.getZ(),0,forward.getX());List<Location> result=new ArrayList<>();for(int side=-1;side<=1;side++){Location candidate=base.clone().add(forward.clone().multiply(5)).add(right.clone().multiply(side*3));candidate.setX(Math.floor(candidate.getX()));candidate.setZ(Math.floor(candidate.getZ()));candidate.setY(base.getBlockY()+1);while(candidate.getBlockY()<candidate.getWorld().getMaxHeight()-1&&!candidate.getBlock().isEmpty())candidate.add(0,1,0);if(candidate.getBlock().isEmpty())result.add(candidate.getBlock().getLocation());}return result;}
    private void restoreTemporaryTestHearts(){for(Map.Entry<String,Material> entry:new HashMap<>(temporaryTestHeartOriginals).entrySet()){Location location=LocationStore.decode(entry.getKey());if(location!=null)location.getBlock().setType(entry.getValue());}temporaryTestHeartOriginals.clear();}
    private double oniHeartDamageMultiplier(){
        if(finalPhase||heartGoalReached())return 1.0;
        int remaining=Math.max(0,heartGoal()-brokenHearts);
        if(heartGoal()==8){return switch(remaining){case 8->.03;case 7->.05;case 6->.08;case 5->.13;case 4->.25;case 3->.42;case 2->.62;case 1->.82;default->1.0;};}
        double ratio=remaining/(double)Math.max(1,heartGoal());
        if(ratio>.875)return .03;if(ratio>.75)return .05;if(ratio>.625)return .08;if(ratio>.50)return .13;if(ratio>.375)return .25;if(ratio>.25)return .42;if(ratio>.125)return .62;if(ratio>0)return .82;return 1.0;
    }
    private double finalAttackMultiplier(){if(isTagMode()&&oniPowerActivated)return 1.80;if(!finalPhase)return 1.0;int t=Math.max(0,secondsLeft);if(t<=30)return 1.80;if(t<=60)return 1.55;if(t<=90)return 1.35;if(t<=120)return 1.20;if(t<=150)return 1.10;return 1.0;}
    private double finalSpeedMultiplier(){if(isTagMode()&&oniPowerActivated)return 1.22;if(!finalPhase)return 1.0;int t=Math.max(0,secondsLeft);if(t<=30)return 1.22;if(t<=60)return 1.17;if(t<=90)return 1.12;if(t<=120)return 1.08;if(t<=150)return 1.04;return 1.0;}
    private double finalCooldownMultiplier(){if(isTagMode()&&oniPowerActivated)return .40;if(!finalPhase)return 1.0;int t=Math.max(0,secondsLeft);if(t<=30)return .40;if(t<=60)return .55;if(t<=90)return .70;if(t<=120)return .80;if(t<=150)return .90;return 1.0;}
    private boolean oniPowerActive(){return state==GameState.RUNNING&&(isTagMode()?oniPowerActivated:secondsLeft<=600);}
    private double oniSkillEffectMultiplier(){if(!oniPowerActive())return 1.0;if(isTagMode()&&oniPowerActivated)return 1.60;if(finalPhase){if(secondsLeft<=30)return 1.60;if(secondsLeft<=60)return 1.50;return 1.35;}return 1.20;}
    private void renderEmpoweredOniSkill(LivingEntity caster){if(caster==null||!oniPowerActive())return;double m=oniSkillEffectMultiplier();int count=(int)Math.round(18*m);caster.getWorld().spawnParticle(Particle.REDSTONE,caster.getLocation().add(0,1,0),count,.55,.75,.55,0,new Particle.DustOptions(Color.fromRGB(170,20,35),1.25f));caster.getWorld().spawnParticle(Particle.SMOKE_LARGE,caster.getLocation().add(0,1,0),Math.max(4,count/3),.4,.6,.4,.025);caster.getWorld().playSound(caster.getLocation(),Sound.ENTITY_WITHER_AMBIENT,.35f,finalPhase?.72f:.9f);}
    private void updateLateGameOniPower(){
        if(!oniPowerActivated&&secondsLeft<=600){oniPowerActivated=true;all("&4&l【鬼力活性】 &c鬼の力が強まり始めた……");for(UUID id:oniTeam){Player p=Bukkit.getPlayer(id);if(p!=null)renderEmpoweredOniSkill(p);}LivingEntity b=getOniEntity();if(b!=null)renderEmpoweredOniSkill(b);}
        if(finalPhase){if(!finalFrenzy60Announced&&secondsLeft<=60){finalFrenzy60Announced=true;all("&4&l《最終狂化》 &c鬼の力が限界を超えた。");for(Player p:Bukkit.getOnlinePlayers())p.sendTitle(cc("&4&l最終狂化"),cc(isTagMode()?"&c鬼が最終形態へ移行":"&c残り60秒――鬼が最終形態へ移行"),5,35,10);}
            if(!finalFrenzy30Announced&&secondsLeft<=30){finalFrenzy30Announced=true;all("&4&l《極限》 &c鬼の力が最大まで高まった！");}
            double speed=finalSpeedMultiplier();for(UUID id:oniTeam){Player p=Bukkit.getPlayer(id);if(p!=null)p.setWalkSpeed((float)Math.min(1.0,getConfig().getDouble("oni-walk-speed",.255)*speed));}if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity b&&b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",.32)*speed);for(UUID id:disconnectOniProxy.values())if(Bukkit.getEntity(id) instanceof LivingEntity b&&b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)b.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",.32)*speed);}
    }
    private void spawnMidgameSupportDrop(){
        if(supportDropTriggered||state!=GameState.RUNNING)return;supportDropTriggered=true;int want=Math.max(1,getConfig().getInt("midgame-support-chests.count",3));List<Location> anchors=new ArrayList<>();for(String key:heartHp.keySet()){Location l=LocationStore.decode(key);if(l!=null)anchors.add(l);}if(anchors.isEmpty()&&activePlayerSpawn!=null)anchors.add(activePlayerSpawn);Collections.shuffle(anchors);Random r=new Random();int made=0;for(Location anchor:anchors){for(int a=0;a<12&&made<want;a++){double ang=r.nextDouble()*Math.PI*2,dist=4+r.nextInt(5);int sx=(int)Math.floor(anchor.getX()+Math.cos(ang)*dist),sz=(int)Math.floor(anchor.getZ()+Math.sin(ang)*dist);Location c=safeSurfaceAt(anchor.getWorld(),sx,sz,0);if(c==null)continue;c=c.getBlock().getLocation();String key=LocationStore.encode(c);if(lootChestKeys.contains(key))continue;temporaryLootChestOriginals.putIfAbsent(key,c.getBlock().getType());c.getBlock().setType(Material.CHEST);lootChestKeys.add(key);made++;}}if(made>0){all("&6&l【追加支援物資】 &f鬼の守りが崩れ、戦場に &e"+made+"個 &fの支援チェストが出現した！");World w=anchors.get(0).getWorld();if(w!=null)w.playSound(anchors.get(0),Sound.BLOCK_ENDER_CHEST_OPEN,1.0f,.8f);}
    }

    private List<Location> selectLootChestLocations(List<Location> hearts,Location playerSpawn){int count=Math.max(1,getConfig().getInt("random-loot-chest-count",8));List<Location> registered=new ArrayList<>(getConfig().getStringList("locations.loot-chests").stream().map(LocationStore::decode).filter(Objects::nonNull).toList());Collections.shuffle(registered);if(!registered.isEmpty())return new ArrayList<>(registered.subList(0,Math.min(count,registered.size())));List<Location> anchors=new ArrayList<>(hearts);if(playerSpawn!=null)anchors.add(playerSpawn);Collections.shuffle(anchors);List<Location> result=new ArrayList<>();Random random=new Random();for(Location anchor:anchors){if(result.size()>=count)break;for(int attempt=0;attempt<24;attempt++){double angle=random.nextDouble()*Math.PI*2,distance=3+random.nextInt(7);int x=(int)Math.floor(anchor.getX()+Math.cos(angle)*distance),z=(int)Math.floor(anchor.getZ()+Math.sin(angle)*distance);Location candidate=safeSurfaceAt(anchor.getWorld(),x,z,0);if(candidate==null)continue;Location chestLoc=candidate.getBlock().getLocation();boolean far=true;for(Location o:result)if(o.getWorld().equals(chestLoc.getWorld())&&o.distanceSquared(chestLoc)<16){far=false;break;}if(far){result.add(chestLoc);break;}}}return result;}
    private void restoreLootChests(){for(Map.Entry<String,Material> entry:new HashMap<>(temporaryLootChestOriginals).entrySet()){Location location=LocationStore.decode(entry.getKey());if(location!=null)location.getBlock().setType(entry.getValue());}temporaryLootChestOriginals.clear();lootChestKeys.clear();tagSkillChestKeys.clear();openedLootChests.clear();personalLootInventories.clear();chestOpeningTasks.clear();}
    private void configureMarker(ArmorStand stand){stand.setCustomName(getConfig().getString("heart-marker-name","鬼の心臓地点"));stand.setCustomNameVisible(false);stand.setVisible(false);stand.setGravity(false);stand.setInvulnerable(true);stand.setMarker(true);stand.setPersistent(true);}

    private void tick(){if(state!=GameState.RUNNING)return;if(isPvMode())return;if(finalPhase&&!ikimonoIds.isEmpty())clearIkimonoSystem();if(isEscapeMode()){tickEscape();return;}secondsLeft--;if(isTagMode()){int fs=Math.max(0,getConfig().getInt("tag-mode.final-seconds",180));if(!finalPhase&&secondsLeft<=fs){finalPhase=true;tagParkourGrandfathered.addAll(tagParkourPlayers);all("&4&l【最終フェーズ】 &cこれ以降、倒れた者は復活できない。");}updateLateGameOniPower();updateOniHeartAwareness();renderOniHeartGlow();tickYuukiStealth();renderNearbyHeartParticles();if(secondsLeft<=0)end(false,"鬼ごっこ終了");return;}updateLateGameOniPower();checkAzakujiReinforcement();updateOniHeartAwareness();renderOniHeartGlow();tickYuukiStealth();for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null)continue;String warning=chased.contains(id)?"  &7|  &4&l追跡中":"";p.sendActionBar(cc("&c心臓 "+brokenHearts+"/&f"+heartGoal()+"  &7|  &e残り "+(secondsLeft/60)+":"+String.format("%02d",secondsLeft%60)+warning));}renderNearbyHeartParticles();if(heartGoalReached())renderExitBeacon();checkExit();if(secondsLeft<=0)end(true,"時間切れ――鬼の勝利");}
    private void updateOniHeartAwareness(){
        if(!getConfig().getBoolean("oni-heart-awareness.enabled",true)||oni==null)return;
        Player p=Bukkit.getPlayer(oni);if(p==null||p.getGameMode()==GameMode.SPECTATOR)return;
        Location nearest=heartHp.keySet().stream().map(LocationStore::decode).filter(Objects::nonNull).filter(l->l.getWorld().equals(p.getWorld())).min(Comparator.comparingDouble(l->l.distanceSquared(p.getLocation()))).orElse(null);
        if(nearest==null){p.setCompassTarget(p.getWorld().getSpawnLocation());return;}
        p.setCompassTarget(nearest.clone().add(.5,.5,.5));
        int count=Math.max(1,getConfig().getInt("oni-heart-awareness.particle-count",18));
        float size=(float)Math.max(.5,getConfig().getDouble("oni-heart-awareness.particle-size",1.8));
        Particle.DustOptions dust=new Particle.DustOptions(Color.fromRGB(210,0,35),size);
        for(String key:heartHp.keySet()){
            Location h=LocationStore.decode(key);if(h==null||!h.getWorld().equals(p.getWorld()))continue;
            Location center=h.clone().add(.5,1.2,.5);
            p.spawnParticle(Particle.REDSTONE,center,count,.35,.55,.35,0,dust);
            p.spawnParticle(Particle.SOUL,center.clone().add(0,.35,0),5,.25,.3,.25,.01);
        }
    }
    private void renderOniHeartGlow(){
        if(state!=GameState.RUNNING||oniTeam.isEmpty()){clearOniHeartGlowMarkers();return;}
        for(String stale:new HashSet<>(oniHeartGlowMarkers.keySet()))if(!heartHp.containsKey(stale)){
            Entity old=Bukkit.getEntity(oniHeartGlowMarkers.remove(stale));if(old!=null)old.remove();
        }
        for(String key:heartHp.keySet()){
            Location heart=LocationStore.decode(key);
            if(heart==null||heart.getWorld()==null)continue;
            Entity marker=oniHeartGlowMarkers.containsKey(key)?Bukkit.getEntity(oniHeartGlowMarkers.get(key)):null;
            if(marker==null||!marker.isValid()){
                ArmorStand stand=heart.getWorld().spawn(heart.clone().add(.5,.05,.5),ArmorStand.class,a->{
                    a.setInvisible(true);a.setInvulnerable(true);a.setGravity(false);a.setMarker(false);a.setSilent(true);a.setGlowing(true);a.setCollidable(false);a.setPersistent(false);
                    ItemStack helm=new ItemStack(Material.CRYING_OBSIDIAN);a.getEquipment().setHelmet(helm);
                });
                marker=stand;oniHeartGlowMarkers.put(key,stand.getUniqueId());
            }
            for(Player other:Bukkit.getOnlinePlayers()){
                if(isOni(other.getUniqueId()))other.showEntity(this,marker);
                else other.hideEntity(this,marker);
            }
            Particle.DustOptions glow=new Particle.DustOptions(Color.fromRGB(255,35,35),2.0f);
            for(Player viewer:Bukkit.getOnlinePlayers()){
                if(isOni(viewer.getUniqueId())&&heart.getWorld().equals(viewer.getWorld())){
                    viewer.spawnParticle(Particle.REDSTONE,heart.clone().add(.5,1.15,.5),10,.30,.38,.30,0,glow);
                }
            }
        }
    }
    private void clearOniHeartGlowMarkers(){
        for(UUID id:new HashSet<>(oniHeartGlowMarkers.values())){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}
        oniHeartGlowMarkers.clear();
    }
    private void renderNearbyHeartParticles(){if(!getConfig().getBoolean("heart-proximity-particles.enabled",true))return;double radius=Math.max(2,getConfig().getDouble("heart-proximity-particles.radius",18)),radiusSquared=radius*radius;Particle.DustOptions blood=new Particle.DustOptions(Color.fromRGB(150,0,20),1.35f);for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player viewer=Bukkit.getPlayer(id);if(viewer==null||viewer.getGameMode()==GameMode.SPECTATOR)continue;for(String key:heartHp.keySet()){Location heart=LocationStore.decode(key);if(heart==null||!heart.getWorld().equals(viewer.getWorld())||heart.distanceSquared(viewer.getLocation())>radiusSquared)continue;Location center=heart.clone().add(.5,1.15,.5);viewer.spawnParticle(Particle.REDSTONE,center,12,.32,.45,.32,0,blood);viewer.spawnParticle(Particle.SOUL,center.clone().add(0,.35,0),4,.22,.3,.22,.015);}}}
    private void renderExitBeacon(){if(activeExit==null||!getConfig().getBoolean("exit-beacon.enabled",true))return;renderExitBeaconAt(activeExit);if(activeExit2!=null)renderExitBeaconAt(activeExit2);}
    private void renderExitBeaconAt(Location exit){World world=exit.getWorld();double height=Math.max(16,getConfig().getDouble("exit-beacon.height",80)),step=Math.max(.35,getConfig().getDouble("exit-beacon.particle-step",.5));Location base=exit.getBlock().getLocation().add(.5,1.1,.5);Particle.DustOptions cyan=new Particle.DustOptions(Color.fromRGB(40,245,255),Math.max(1f,(float)getConfig().getDouble("exit-beacon.dust-size",2.2)));for(double y=0;y<=height;y+=step){Location point=base.clone().add(0,y,0);world.spawnParticle(Particle.REDSTONE,point,3,.18,.08,.18,0,cyan);if(((int)(y*2))%6==0)world.spawnParticle(Particle.END_ROD,point,2,.12,.2,.12,.008);}for(int ring=0;ring<3;ring++){double radius=1.5+ring*1.2;for(int i=0;i<24;i++){double angle=Math.PI*2*i/24;world.spawnParticle(Particle.SOUL_FIRE_FLAME,base.clone().add(Math.cos(angle)*radius,.2,Math.sin(angle)*radius),1,0,0,0,0);}}world.spawnParticle(Particle.TOTEM,base,45,2.4,.8,2.4,.06);}
    private void updateStamina(){if(state!=GameState.RUNNING)return;long now=System.currentTimeMillis();double baseDrain=getConfig().getDouble("stamina.drain-per-second",2.0)/4.0,baseRecovery=getConfig().getDouble("stamina.recovery-per-second",1.5)/4.0,baseSneakRecovery=getConfig().getDouble("stamina.sneak-recovery-per-second",2.5)/4.0;for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null||p.getGameMode()==GameMode.SPECTATOR||p.getGameMode()==GameMode.CREATIVE)continue;boolean playerSide=players.contains(id);float walkSpeed=!playerSide?(float)Math.min(1.0,getConfig().getDouble("oni-walk-speed",0.255)*finalSpeedMultiplier()):(isDownEscape(id)?0.55f:(float)getConfig().getDouble("player-walk-speed",0.20));if(playerSide&&!isDownEscape(id)&&hasPassive(id,PassiveSkill.NINJA_BLOOD))walkSpeed=(float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23);else if(playerSide&&!isDownEscape(id)&&hasPassive(id,PassiveSkill.COWARDICE)&&p.isSneaking())walkSpeed=(float)getConfig().getDouble("passive-skills.cowardice-sneak-walk-speed",0.58);p.setWalkSpeed(walkSpeed);if(playerSide&&hasPassive(id,PassiveSkill.LEAP))p.setAllowFlight(now>=passiveLeapReadyAt.getOrDefault(id,0L));else if(p.getAllowFlight()){p.setFlying(false);p.setAllowFlight(false);}double drain=baseDrain,recovery=baseRecovery,sneakRecovery=baseSneakRecovery;if(playerSide&&hasPassive(id,PassiveSkill.LIGHT_FOOTED))drain*=.85;if(playerSide&&hasPassive(id,PassiveSkill.DEEP_BREATH)){recovery*=1.25;sneakRecovery*=1.25;}boolean desperate=playerSide&&updateDesperateState(p,now);double value=stamina.getOrDefault(id,20.0);boolean sprinting=p.isSprinting();if(playerSide&&hasPassive(id,PassiveSkill.LAST_RESERVE)&&wasSprinting.getOrDefault(id,false)&&!sprinting&&value<=getConfig().getDouble("technical-passives.last-reserve-threshold",3.0)&&technicalReady(id,"LAST_RESERVE",getConfig().getLong("technical-passives.last-reserve-cooldown-seconds",25))){value=Math.min(20,value+getConfig().getDouble("technical-passives.last-reserve-recovery",5.0));p.sendActionBar(cc("&e土壇場 &7――スタミナ回復"));}wasSprinting.put(id,sprinting);if(desperate){}else if(p.isSprinting()&&p.getVelocity().setY(0).lengthSquared()>0.005)value-=drain;else value+=p.isSneaking()?sneakRecovery:recovery;value=Math.max(0,Math.min(20,value));stamina.put(id,value);p.setFoodLevel((int)Math.ceil(value));p.setSaturation(0);if(value<=0&&p.isSprinting()){p.setSprinting(false);p.sendActionBar(cc("&cスタミナ切れ &7――歩くか、止まって回復"));}}LivingEntity hunter=getOniEntity();if(hunter!=null&&!heartGoalReached()){double heal=getConfig().getDouble("oni-heart-protection.regeneration-per-second",1.0)/4.0;hunter.setHealth(Math.min(hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),hunter.getHealth()+heal));}}
    private String manualUnstuckDeniedReason(Player p){
        UUID id=p.getUniqueId();long now=System.currentTimeMillis();
        if(getConfig().getBoolean("safety-rescue.deny-while-chased",true)&&chased.contains(id))return "追跡中は使用できません";
        LivingEntity hunter=getOniEntity();
        double radius=Math.max(0,getConfig().getDouble("safety-rescue.deny-near-oni-radius",18.0));
        if(players.contains(id)&&hunter!=null&&hunter.isValid()&&hunter.getWorld()==p.getWorld()&&p.getLocation().distanceSquared(hunter.getLocation())<=radius*radius)return "鬼が近すぎるため使用できません";
        long combat=Math.max(0,getConfig().getLong("safety-rescue.deny-after-combat-seconds",12))*1000L;
        if(now-lastCombatAt.getOrDefault(id,0L)<combat)return "戦闘直後は使用できません";
        if(repairingHeart.containsKey(id)||healingTasks.containsKey(id)||chestOpeningTasks.containsKey(id))return "作業中は使用できません";
        boolean trapped=isEntityEmbedded(p)||p.getLocation().getY()<p.getWorld().getMinHeight()+1||!isSafeStandLocation(p.getLocation());
        if(getConfig().getBoolean("safety-rescue.require-no-escape-space",true))trapped=trapped||!hasEscapeSpace(p.getLocation(),2);
        return trapped?null:"現在地は脱出可能と判定されています";
    }
    private void registerUnstuckAbuseAttempt(Player p,String reason){
        UUID id=p.getUniqueId();long now=System.currentTimeMillis();
        long window=Math.max(10,getConfig().getLong("safety-rescue.abuse-window-seconds",120))*1000L;
        long start=safetyAbuseWindowStartedAt.getOrDefault(id,0L);
        if(start==0||now-start>window){safetyAbuseWindowStartedAt.put(id,now);safetyAbuseAttempts.put(id,0);}
        int attempts=safetyAbuseAttempts.getOrDefault(id,0)+1;safetyAbuseAttempts.put(id,attempts);
        int limit=Math.max(2,getConfig().getInt("safety-rescue.abuse-attempt-limit",3));
        msg(p,"&c詰み救済を拒否しました。 &7"+reason+" &8("+attempts+"/"+limit+")");
        if(attempts<limit)return;
        long lock=Math.max(30,getConfig().getLong("safety-rescue.abuse-lock-seconds",600))*1000L;
        safetyAbuseLockUntil.put(id,now+lock);safetyAbuseAttempts.put(id,0);safetyAbuseWindowStartedAt.put(id,now);
        msg(p,"&4悪用防止: &c詰み救済を一時ロックしました。");
        if(getConfig().getBoolean("safety-rescue.notify-ops-on-lock",true)){
            for(Player op:Bukkit.getOnlinePlayers())if(op.isOp()&&!isPlayerTest(op))msg(op,"&e"+p.getName()+" &7の /og unstuck を悪用疑いで一時ロックしました。");
        }
    }

    private void updateSafetyRescue(){
        if(state!=GameState.RUNNING||!getConfig().getBoolean("safety-rescue.enabled",true))return;
        Set<UUID> checked=new HashSet<>();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)){checked.add(id);checkSafetyRescue(p,false);}}
        if(oni!=null){Player oniPlayer=Bukkit.getPlayer(oni);if(oniPlayer!=null){checked.add(oni);checkSafetyRescue(oniPlayer,false);}}
        if(getConfig().getBoolean("safety-rescue.auto-bots",true)){
            for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity e){checked.add(id);checkSafetyRescue(e,true);}}
            if(oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity e){checked.add(oniBot);checkSafetyRescue(e,true);}
            for(UUID id:new HashSet<>(dakkoCloneBots))if(Bukkit.getEntity(id) instanceof LivingEntity e){checked.add(id);checkSafetyRescue(e,true);}
        }
        safetyLastLocation.keySet().removeIf(id->!checked.contains(id));
        safetyStuckChecks.keySet().removeIf(id->!checked.contains(id));
    }
    private void checkSafetyRescue(LivingEntity entity,boolean bot){
        if(entity==null||!entity.isValid()||entity.isDead())return;
        UUID id=entity.getUniqueId();long now=System.currentTimeMillis();
        Location here=entity.getLocation();
        if(isSafeStandLocation(here)&&hasEscapeSpace(here,2)){
            safetyLastGoodLocation.put(id,here.clone());
        }
        boolean embedded=isEntityEmbedded(entity)||here.getY()<entity.getWorld().getMinHeight()+1;
        if(!bot){
            safetyLastLocation.put(id,here.clone());
            if(embedded&&getConfig().getBoolean("safety-rescue.auto-players-when-embedded",true)&&now>=safetyRescueCooldownUntil.getOrDefault(id,0L))rescueEntityToSafePlace(entity,false);
            return;
        }
        Location last=safetyLastLocation.put(id,here.clone());
        boolean barelyMoved=last!=null&&last.getWorld()==here.getWorld()&&last.distanceSquared(here)<.015;
        int checks=barelyMoved?safetyStuckChecks.getOrDefault(id,0)+1:0;
        safetyStuckChecks.put(id,checks);
        int threshold=Math.max(4,getConfig().getInt("safety-rescue.bot-stuck-checks",10));
        if((embedded||checks>=threshold)&&now>=safetyRescueCooldownUntil.getOrDefault(id,0L)){
            if(rescueEntityToSafePlace(entity,false))safetyStuckChecks.put(id,0);
        }
    }
    private boolean rescueEntityToSafePlace(LivingEntity entity,boolean manual){
        if(entity==null||entity.getWorld()==null)return false;
        UUID id=entity.getUniqueId();Location origin=entity.getLocation().clone();
        Location safe=findNearestSafeRescueLocation(entity,origin);
        if(safe==null){
            Location remembered=safetyLastGoodLocation.get(id);
            if(remembered!=null&&remembered.getWorld()==entity.getWorld()&&isSafeStandLocation(remembered))safe=remembered.clone();
        }
        if(safe==null){
            Location fallback=isOni(id)||id.equals(oniBot)?LocationStore.get(getConfig(),"locations.oni-spawn"):activePlayerSpawn;
            if(fallback!=null&&fallback.getWorld()==entity.getWorld())safe=findNearestSafeRescueLocation(entity,fallback);
        }
        if(safe==null)return false;
        safe.setYaw(origin.getYaw());safe.setPitch(origin.getPitch());
        if(entity instanceof Player p){if(players.contains(id))stopRepair(p,"&7詰み救済のため心臓への干渉を中断した。");chestOpeningTasks.remove(id);stopHealing(p,null);}
        if(playerBots.contains(id)){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);stopPlayerBotNavigation(entity);}
        entity.setVelocity(new Vector(0,0,0));entity.setFallDistance(0);entity.teleport(safe);
        entity.getWorld().spawnParticle(Particle.PORTAL,safe.clone().add(0,1,0),28,.45,.8,.45,.12);
        entity.getWorld().playSound(safe,Sound.ENTITY_ENDERMAN_TELEPORT,.55f,1.35f);
        long cd=Math.max(1,getConfig().getLong("safety-rescue.cooldown-seconds",180))*1000L;
        safetyRescueCooldownUntil.put(id,System.currentTimeMillis()+cd);
        safetyLastGoodLocation.put(id,safe.clone());safetyLastLocation.put(id,safe.clone());safetyStuckChecks.put(id,0);
        if(manual&&entity instanceof Player p)p.sendActionBar(cc("&b詰み救済 &7――安全地点へ復帰"));
        return true;
    }
    private Location findNearestSafeRescueLocation(LivingEntity entity,Location center){
        if(center==null||center.getWorld()==null)return null;World world=center.getWorld();
        int radius=Math.max(3,getConfig().getInt("safety-rescue.search-radius",12));
        int vertical=Math.max(2,getConfig().getInt("safety-rescue.vertical-search",6));
        Location best=null;double bestScore=Double.MAX_VALUE;
        int baseX=center.getBlockX(),baseY=center.getBlockY(),baseZ=center.getBlockZ();
        for(int r=1;r<=radius;r++){
            for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
                for(int dy=vertical;dy>=-vertical;dy--){
                    int y=baseY+dy;if(y<=world.getMinHeight()+1||y>=world.getMaxHeight()-2)continue;
                    Location candidate=new Location(world,baseX+dx+.5,y,baseZ+dz+.5);
                    if(!isSafeStandLocation(candidate)||!hasEscapeSpace(candidate,2))continue;
                    WorldBorder border=world.getWorldBorder();if(border!=null&&!border.isInside(candidate))continue;
                    double score=dx*dx+dz*dz+Math.abs(dy)*1.5;
                    if(score<bestScore){bestScore=score;best=candidate;}
                }
            }
            if(best!=null&&r>=3)break;
        }
        return best;
    }
    private boolean isSafeStandLocation(Location feet){
        if(feet==null||feet.getWorld()==null)return false;
        Block foot=feet.getBlock(),head=feet.clone().add(0,1,0).getBlock(),below=feet.clone().add(0,-1,0).getBlock();
        if(!foot.isPassable()||!head.isPassable()||below.isPassable()||below.isLiquid())return false;
        Material m=below.getType();
        return m!=Material.MAGMA_BLOCK&&m!=Material.CACTUS&&m!=Material.CAMPFIRE&&m!=Material.SOUL_CAMPFIRE&&m!=Material.FIRE&&m!=Material.SOUL_FIRE;
    }
    private boolean hasEscapeSpace(Location feet,int radius){
        if(feet==null)return false;
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(dx==0&&dz==0)continue;
            Location next=feet.clone().add(dx,0,dz);
            if(isSafeStandLocation(next))return true;
            if(isSafeStandLocation(next.clone().add(0,1,0)))return true;
            if(isSafeStandLocation(next.clone().add(0,-1,0)))return true;
        }
        return false;
    }
    private boolean isEntityEmbedded(LivingEntity entity){
        Location feet=entity.getLocation(),body=feet.clone().add(0,.9,0),head=feet.clone().add(0,1.7,0);
        return !feet.getBlock().isPassable()||!body.getBlock().isPassable()||!head.getBlock().isPassable();
    }

    private void updateBots(){updateSafetyRescue();updateOniBot();updateDisconnectedOniProxies();updateDakkoCloneBots();updatePlayerBots();}
    private void updateOniBot(){
        if(state!=GameState.RUNNING||oniBot==null||heavenlyArrival.contains(oniBot))return;if(!(Bukkit.getEntity(oniBot) instanceof Mob bot))return;long now=System.currentTimeMillis();
        tickIkimonoBotBuffUse(oniBot,bot,true);LivingEntity beastTarget=nearestIkimono(bot,12.0);if(beastTarget!=null){bot.setTarget(beastTarget);double bd=bot.getLocation().distance(beastTarget.getLocation());if(bd<=2.5&&now>=botNextMeleeAt){botNextMeleeAt=now+900;bot.swingMainHand();beastTarget.damage(Math.max(3,getConfig().getDouble("bot.attack-damage",7.0)),bot);}return;}
        LivingEntity target=getConfig().getBoolean("bot.hunter-ai.enabled",true)?findTacticalOniBotTarget(bot):findNearestPlayerSideTarget(bot);
        if(target==null){oniBotTacticalTarget=null;bot.setTarget(null);return;}oniBotTacticalTarget=target.getUniqueId();if(now<botRestUntil){bot.setTarget(null);botStamina=Math.min(20,botStamina+getConfig().getDouble("stamina.sneak-recovery-per-second",2.5)/2.0);return;}
        bot.setTarget(target);double distance=bot.getLocation().distance(target.getLocation());Location predictedTarget=predictOniBotTarget(target);if(distance>2.2)botStamina-=getConfig().getDouble("stamina.drain-per-second",2.0)/2.0;else botStamina=Math.min(20,botStamina+getConfig().getDouble("stamina.recovery-per-second",1.5)/2.0);
        if(playerBots.contains(target.getUniqueId())){guideOniBotToPlayerBot(bot,target,distance);if(distance<=getConfig().getDouble("bot.player-bot-attack-range",2.5)&&now>=botNextMeleeAt){botNextMeleeAt=now+Math.max(1,getConfig().getLong("bot.player-bot-attack-cooldown-ticks",20))*50L;bot.swingMainHand();double melee=getConfig().getDouble("bot.attack-damage",7.0);if(isKishinIronBodyActive(bot))melee*=Math.max(1.0,getConfig().getDouble("kishin-skills.iron-body.attack-damage-multiplier",1.35));target.damage(melee,bot);if(oniType==OniType.DAKKO&&botDifficulty!=BotDifficulty.NORMAL)collectDakkoFoxMark(bot,target,"通常攻撃");bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,.7f,.8f);}}
        if(botStamina<=0){botStamina=0;botRestUntil=now+(long)(getConfig().getDouble("bot.rest-seconds",3.0)*1000);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_STUNNED,0.6f,1.2f);return;}if(now<botNextAbilityAt)return;
        if(oniType==OniType.DAKKO){
            double foxRadius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0);
            // 熟練AI: 分身体が獲物の近くにいるなら狐換えで先回りする
            if(botDifficulty!=BotDifficulty.NORMAL&&distance>7&&(botDifficulty==BotDifficulty.EXTRA||botDakkoSkillCycle%2==1)&&tryBotDakkoCloneSwap(bot,target)){botNextAbilityAt=now+oniBotAbilityCooldownMillis(Math.max(1,getConfig().getInt("dakko-mastery.bot-clone-swap-cooldown-seconds",5)-(botDifficulty==BotDifficulty.EXTRA?2:0)));botDakkoSkillCycle++;recoverOniSkillMomentum();return;}
            // 狐印が付いた獲物には予測先回りを優先
            if(botDifficulty!=BotDifficulty.NORMAL&&hasDakkoFoxMark(target)&&distance>5&&distance<30&&(botDifficulty==BotDifficulty.EXTRA||botDakkoSkillCycle%3==2)){Location ambush=predictedTarget.clone().subtract(horizontalDirection(target.getLocation()).multiply(botDifficulty==BotDifficulty.EXTRA?1.2:2.2));ambush.setY(predictedTarget.getY());if(isSafeStandLocation(ambush)){bot.teleport(ambush);bot.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,bot.getLocation().add(0,1,0),24,.4,.7,.4,.04);if(botDifficulty==BotDifficulty.EXTRA&&distance<=7)collectDakkoFoxMark(bot,target,"先回り");botNextAbilityAt=now+oniBotAbilityCooldownMillis(Math.max(2,getConfig().getInt("dakko-mastery.bot-mark-ambush-cooldown-seconds",7)-(botDifficulty==BotDifficulty.EXTRA?3:0)));botDakkoSkillCycle++;recoverOniSkillMomentum();return;}}
            if(oniSkillUnlocked("dakko_clone")&&botDakkoSkillCycle%3==0){spawnDakkoCloneBots(bot,bot.getLocation());botNextAbilityAt=now+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.clone.bot-cooldown-seconds",35));recoverOniSkillMomentum();botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("dakko_fox_fire")&&distance<=foxRadius){useBotFoxFire(bot);botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("dakko_heavenly_arrival")&&distance>=getConfig().getDouble("bot.dakko-heavenly-min-range",8.0)&&distance<=getConfig().getDouble("bot.dakko-heavenly-max-range",24.0)&&botDakkoSkillCycle%2==0){useBotHeavenlyArrival(bot,target);botDakkoSkillCycle++;return;}
            if(brokenHearts<4&&distance>8&&distance<24){Location behind=predictedTarget.clone().subtract(horizontalDirection(target.getLocation()).multiply(3));behind.setY(predictedTarget.getY());bot.teleport(behind);bot.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,bot.getLocation(),30,0.5,1,0.5,0.04);botNextAbilityAt=now+oniBotAbilityCooldownMillis(20+brokenHearts*3);recoverOniSkillMomentum();botDakkoSkillCycle++;}
        }else if(oniType==OniType.KISHIN){
            double roarRadius=getConfig().getDouble("kishin-skills.roar.radius",8.0);
            if(oniSkillUnlocked("kishin_roar")&&distance<=roarRadius&&botDakkoSkillCycle%3==0){useBotKishinRoar(bot);botDakkoSkillCycle++;return;}
            if(oniSkillUnlocked("kishin_iron_body")&&botDakkoSkillCycle%4==1&&(distance<=10||bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=getConfig().getDouble("bot.hunter-ai.skill-save-health-ratio",.45))){useBotKishinIronBody(bot);botDakkoSkillCycle++;return;}
            if(distance>5&&distance<16){Vector charge=predictedTarget.toVector().subtract(bot.getLocation().toVector()).normalize().multiply(Math.max(1.2,2.1-brokenHearts*.15)).setY(.15);bot.setVelocity(charge);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);botNextAbilityAt=now+oniBotAbilityCooldownMillis(18+brokenHearts*3);recoverOniSkillMomentum();botDakkoSkillCycle++;}
        }
        else if(oniType==OniType.SHIKKI){
            boolean frenzy=oniSkillUnlocked("shikki_frenzy")&&botDakkoSkillCycle%4==3;
            if(frenzy){int ticks=Math.max(20,getConfig().getInt("shikki-skills.frenzy.duration-ticks",160));bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,Math.max(0,getConfig().getInt("shikki-skills.frenzy.speed-amplifier",2)),false,true));bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1,0),30,.4,.7,.4,.06);botNextAbilityAt=now+oniBotAbilityCooldownMillis(getConfig().getInt("shikki-skills.frenzy.cooldown-seconds",32));}
            else{useBotShikkiLeapChain(bot,target);botNextAbilityAt=oniPowerActivated?now:now+oniBotAbilityCooldownMillis(getConfig().getInt("shikki-skills.hunting-leap.bot-cooldown-seconds",6));}
            botDakkoSkillCycle++;recoverOniSkillMomentum();return;
        }
        else if(oniType==OniType.JAKUTSUKI&&distance>=getConfig().getDouble("jakutsuki-skills.piercing-blast.bot-min-range",7.0)&&distance<=getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)){useBotJakutsukiPiercingBlast(bot,target);return;}
    }


    private void useBotShikkiLeapChain(Mob bot,LivingEntity initialTarget){
        int jumps=botDifficulty==BotDifficulty.NORMAL?1:(botDifficulty==BotDifficulty.ELITE?2:3);if(oniPowerActivated)jumps=Math.max(jumps,getConfig().getInt("shikki-skills.hunting-leap.empowered-bot-max-chain",9));
        botShikkiLeapStep(bot,initialTarget,1,jumps);
    }
    private void botShikkiLeapStep(Mob bot,LivingEntity target,int step,int max){
        if(bot==null||!bot.isValid()||bot.isDead()||target==null||!target.isValid()||target.isDead())return;
        Location aim=predictOniBotTarget(target);if(botDifficulty==BotDifficulty.EXTRA){Vector motion=target.getVelocity().clone().multiply(.35);aim.add(motion);}
        Vector leap=aim.toVector().subtract(bot.getLocation().toVector());if(leap.lengthSquared()<.01)return;leap.normalize().multiply(getConfig().getDouble("shikki-skills.hunting-leap.bot-power",1.45)*(oniPowerActivated?getConfig().getDouble("shikki-skills.hunting-leap.empowered-power-multiplier",1.15):1.0));leap.setY(Math.max(.48,leap.getY()+.48));bot.setVelocity(leap);bot.setFallDistance(0);bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),18,.3,.15,.3,.05);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.7f,(float)(1.4+step*.1));if(isTagMode())startTagShikkiExplosiveTrail(bot,step,max);
        if(step>=max)return;
        long delay=step==1?Math.max(2,getConfig().getLong("shikki-skills.hunting-leap.bot-chain-delay-1-ticks",10)):Math.max(2,getConfig().getLong("shikki-skills.hunting-leap.bot-chain-delay-2-ticks",6));if(oniPowerActivated)delay=Math.max(2,Math.round(delay*getConfig().getDouble("shikki-skills.hunting-leap.empowered-bot-chain-delay-multiplier",0.8)));
        Bukkit.getScheduler().runTaskLater(this,()->{if(!bot.isValid()||bot.isDead()||bot.isOnGround())return;LivingEntity next=findTacticalOniBotTarget(bot);if(next==null)next=target;botShikkiLeapStep(bot,next,step+1,max);},delay);
    }

    private LivingEntity findTacticalOniBotTarget(LivingEntity hunter){
        if(azakujiAllyId!=null&&System.currentTimeMillis()<azakujiOniBotRetaliateUntil&&!deadPlayerBots.contains(azakujiAllyId)&&!escapedPlayerBots.contains(azakujiAllyId)&&Bukkit.getEntity(azakujiAllyId) instanceof LivingEntity azakuji&&azakuji.isValid()&&!azakuji.isDead()&&azakuji.getWorld().equals(hunter.getWorld()))return azakuji;
        List<LivingEntity> candidates=new ArrayList<>();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)&&!isDownEscape(id)&&p.getWorld()==hunter.getWorld())candidates.add(p);}
        for(UUID id:playerBots){if(id.equals(azakujiAllyId)&&System.currentTimeMillis()>=azakujiOniBotRetaliateUntil)continue;if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity e&&e.isValid()&&!e.isDead()&&e.getWorld()==hunter.getWorld())candidates.add(e);}
        if(candidates.isEmpty())return null;
        LivingEntity best=null;double bestScore=-Double.MAX_VALUE;
        for(LivingEntity target:candidates){
            UUID id=target.getUniqueId();double distance=hunter.getLocation().distance(target.getLocation());
            double score=-distance*Math.max(.1,getConfig().getDouble("bot.hunter-ai.target-distance-weight",1.0));
            if(target instanceof Player)score+=getConfig().getDouble("bot.hunter-ai.target-human-bonus",2.5);
            if(id.equals(configuredFriendlyNpcId))score+=getConfig().getDouble("bot.hunter-ai.target-friendly-npc-bonus",5.0);
            double max=target.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20:target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            double ratio=target.getHealth()/Math.max(1,max);score+=(1-ratio)*getConfig().getDouble("bot.hunter-ai.target-injured-bonus",7.0);
            if(repairingHeart.containsKey(id)||playerBotWorkingHeart.containsKey(id))score+=getConfig().getDouble("bot.hunter-ai.target-heart-worker-bonus",8.0);
            if(isNearRemainingHeart(target.getLocation(),getConfig().getDouble("bot.hunter-ai.heart-defense-radius",16.0)))score+=getConfig().getDouble("bot.hunter-ai.heart-defense-bonus",6.0);
            if(heartGoalReached())score+=getConfig().getDouble("bot.hunter-ai.final-target-bonus",5.0)*(1-ratio);
            if(id.equals(oniBotTacticalTarget)){
                score+=getConfig().getDouble("bot.hunter-ai.retarget-hysteresis",3.0);
                long chasedMs=Math.max(0,System.currentTimeMillis()-oniBotTargetSince);
                double giveUpAfter=Math.max(4,getConfig().getDouble("bot.hunter-ai.time-waste-seconds",14.0))*1000.0;
                boolean closing=oniBotTargetStartDistance>0&&(oniBotTargetStartDistance-distance)>=getConfig().getDouble("bot.hunter-ai.minimum-closing-distance",3.0);
                boolean nearlyCaught=distance<=getConfig().getDouble("bot.hunter-ai.commit-distance",5.0)||ratio<=getConfig().getDouble("bot.hunter-ai.commit-health-ratio",.38);
                if(chasedMs>=giveUpAfter&&!closing&&!nearlyCaught)score-=getConfig().getDouble("bot.hunter-ai.time-waste-penalty",16.0);
                if(nearlyCaught)score+=getConfig().getDouble("bot.hunter-ai.nearly-caught-bonus",10.0);
            }
            if(score>bestScore){bestScore=score;best=target;}
        }
        if(best!=null){
            UUID chosen=best.getUniqueId();double d=hunter.getLocation().distance(best.getLocation());
            if(!chosen.equals(oniBotTacticalTarget)){oniBotTargetSince=System.currentTimeMillis();oniBotTargetStartDistance=d;oniBotTargetBestDistance=d;}
            else oniBotTargetBestDistance=Math.min(oniBotTargetBestDistance,d);
        }
        return best;
    }
    private boolean isNearRemainingHeart(Location at,double radius){
        if(at==null)return false;double r2=radius*radius;
        for(String key:heartHp.keySet()){Location h=LocationStore.decode(key);if(h!=null&&h.getWorld()==at.getWorld()&&h.distanceSquared(at)<=r2)return true;}
        return false;
    }
    private Location predictOniBotTarget(LivingEntity target){
        Location base=target.getLocation().clone();Vector v=target.getVelocity().clone().setY(0);
        double seconds=Math.max(0,getConfig().getDouble("bot.hunter-ai.prediction-seconds",.65));
        Vector lead=v.multiply(20.0*seconds);double max=Math.max(0,getConfig().getDouble("bot.hunter-ai.prediction-max-distance",3.5));
        if(lead.length()>max&&lead.lengthSquared()>0)lead.normalize().multiply(max);
        return base.add(lead);
    }
    private LivingEntity findNearestPlayerSideTarget(LivingEntity hunter){if(azakujiAllyId!=null&&System.currentTimeMillis()<azakujiOniBotRetaliateUntil&&!deadPlayerBots.contains(azakujiAllyId)&&!escapedPlayerBots.contains(azakujiAllyId)&&Bukkit.getEntity(azakujiAllyId) instanceof LivingEntity azakuji&&azakuji.isValid()&&!azakuji.isDead()&&azakuji.getWorld().equals(hunter.getWorld()))return azakuji;List<LivingEntity> targets=new ArrayList<>();for(UUID id:players){Player player=Bukkit.getPlayer(id);if(player!=null&&!dead.contains(id)&&!escaped.contains(id)&&!isDownEscape(id)&&player.getWorld().equals(hunter.getWorld()))targets.add(player);}for(UUID id:playerBots){if(id.equals(azakujiAllyId)&&System.currentTimeMillis()>=azakujiOniBotRetaliateUntil)continue;if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity entity&&entity.isValid()&&!entity.isDead()&&entity.getWorld().equals(hunter.getWorld()))targets.add(entity);}return targets.stream().min(Comparator.comparingDouble(entity->entity.getLocation().distanceSquared(hunter.getLocation()))).orElse(null);}
    private void guideOniBotToPlayerBot(Mob bot,LivingEntity target,double distance){if(distance<=getConfig().getDouble("bot.player-bot-attack-range",2.5))return;Vector direction=target.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);if(direction.lengthSquared()<.01)return;direction.normalize();double speed=getConfig().getDouble("bot.player-bot-chase-velocity",.28);double y=bot.getVelocity().getY();if(bot.isOnGround()&&isPlayerBotBlocked(bot,direction,1.0))y=getConfig().getDouble("player-bot.movement.jump-velocity",.42);bot.setVelocity(new Vector(direction.getX()*speed,y,direction.getZ()*speed));float yaw=(float)Math.toDegrees(Math.atan2(-direction.getX(),direction.getZ()));bot.setRotation(yaw,bot.getLocation().getPitch());}

    private void spawnDakkoCloneBots(LivingEntity owner,Location origin){
        if(owner==null||origin==null||origin.getWorld()==null)return;
        int max=Math.max(1,getConfig().getInt("dakko-skills.clone.max-active",2));
        removeExpiredDakkoCloneBots();
        int room=Math.max(0,max-dakkoCloneBots.size());
        int count=Math.min(room,Math.max(1,getConfig().getInt("dakko-skills.clone.spawn-count",2)));
        if(count<=0)return;
        double baseHealth=owner.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?60.0:owner.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double hpRatio=Math.max(.05,Math.min(.49,getConfig().getDouble("dakko-skills.clone.health-ratio",.40)));
        double attackRatio=Math.max(.05,Math.min(.49,getConfig().getDouble("dakko-skills.clone.attack-ratio",.40)));
        double moveRatio=Math.max(.10,Math.min(1.0,getConfig().getDouble("dakko-skills.clone.movement-ratio",.85)));
        double baseAttack=getConfig().getDouble("bot.attack-damage",7.0);
        if(owner.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!=null)baseAttack=Math.max(1.0,owner.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getValue());
        double baseMove=getConfig().getDouble("bot.movement-speed",.32);
        if(owner.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)baseMove=Math.max(.05,owner.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue());
        long expires=System.currentTimeMillis()+Math.max(1,getConfig().getInt("dakko-skills.clone.duration-seconds",5))*1000L;
        Vector right=new Vector(-horizontalDirection(owner.getLocation()).getZ(),0,horizontalDirection(owner.getLocation()).getX());
        for(int n=0;n<count;n++){
            Location at=origin.clone().add(right.clone().multiply(n==0?-1.2:1.2));
            Zombie clone=at.getWorld().spawn(at,Zombie.class);
            clone.setBaby(false);clone.setPersistent(true);clone.setRemoveWhenFarAway(false);clone.setCanPickupItems(false);clone.setSilent(true);
            clone.setCustomName(cc("&5&l堕狐・分身体"));clone.setCustomNameVisible(false);
            clone.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
            double hp=Math.max(4.0,baseHealth*hpRatio);
            clone.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);clone.setHealth(hp);
            clone.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(Math.max(1.0,baseAttack*attackRatio));
            clone.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(Math.max(.05,baseMove*moveRatio));
            clone.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));clone.getEquipment().setItemInMainHandDropChance(0);
            clone.getEquipment().setHelmet(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"));
            clone.getEquipment().setChestplate(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"));
            clone.getEquipment().setLeggings(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"));
            clone.getEquipment().setBoots(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"));
            clone.getEquipment().setHelmetDropChance(0);clone.getEquipment().setChestplateDropChance(0);clone.getEquipment().setLeggingsDropChance(0);clone.getEquipment().setBootsDropChance(0);
            clone.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"dakko_clone_bot");
            dakkoCloneBots.add(clone.getUniqueId());dakkoCloneExpiresAt.put(clone.getUniqueId(),expires);dakkoCloneNextAttackAt.put(clone.getUniqueId(),0L);
            dakkoCloneOwner.put(clone.getUniqueId(),owner.getUniqueId());
            dakkoCloneDifficulty.put(clone.getUniqueId(),owner instanceof Player?BotDifficulty.EXTRA:botDifficulty);
            clone.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,clone.getLocation().add(0,1,0),40,.55,.9,.55,.05);
            clone.getWorld().spawnParticle(Particle.SMOKE_LARGE,clone.getLocation().add(0,1,0),24,.45,.75,.45,.03);
        }
        origin.getWorld().playSound(origin,Sound.ENTITY_FOX_SCREECH,.8f,.72f);
    }
    private void updateDakkoCloneBots(){
        if(dakkoCloneBots.isEmpty())return;
        long now=System.currentTimeMillis();
        for(UUID id:new HashSet<>(dakkoCloneBots)){
            Entity raw=Bukkit.getEntity(id);
            if(!(raw instanceof Mob clone)||!clone.isValid()||clone.isDead()||now>=dakkoCloneExpiresAt.getOrDefault(id,0L)){
                dismissDakkoCloneBot(id,true);continue;
            }
            BotDifficulty cloneDifficulty=dakkoCloneDifficulty.getOrDefault(id,BotDifficulty.EXTRA);
            LivingEntity target=findDakkoCloneTarget(clone,id,cloneDifficulty);
            if(target==null){clone.setTarget(null);continue;}
            clone.setTarget(target);
            double dist=clone.getLocation().distance(target.getLocation());
            if(dist>2.5){
                Vector d=target.getLocation().toVector().subtract(clone.getLocation().toVector()).setY(0);
                if(d.lengthSquared()>.01){
                    d.normalize();double v=getConfig().getDouble("bot.player-bot-chase-velocity",.28)*getConfig().getDouble("dakko-skills.clone.movement-ratio",.85);
                    double y=clone.getVelocity().getY();if(clone.isOnGround()&&isPlayerBotBlocked(clone,d,1.0))y=getConfig().getDouble("player-bot.movement.jump-velocity",.42);
                    clone.setVelocity(new Vector(d.getX()*v,y,d.getZ()*v));
                }
            }else if(now>=dakkoCloneNextAttackAt.getOrDefault(id,0L)){
                dakkoCloneNextAttackAt.put(id,now+Math.max(8,getConfig().getLong("dakko-skills.clone.attack-cooldown-ticks",22))*50L);
                clone.swingMainHand();
                double damage=Math.max(1.0,getConfig().getDouble("bot.attack-damage",7.0)*Math.min(.49,getConfig().getDouble("dakko-skills.clone.attack-ratio",.40)));
                target.damage(damage,clone);
                clone.getWorld().playSound(clone.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.55f,1.15f);
            }
        }
    }
    private LivingEntity findDakkoCloneTarget(Mob clone,UUID cloneId,BotDifficulty difficulty){
        List<LivingEntity> candidates=new ArrayList<>();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p!=null&&!dead.contains(id)&&!escaped.contains(id)&&!isDownEscape(id)&&p.getWorld()==clone.getWorld())candidates.add(p);}
        for(UUID id:playerBots){if(id.equals(azakujiAllyId)&&System.currentTimeMillis()>=azakujiOniBotRetaliateUntil)continue;if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity e&&e.isValid()&&!e.isDead()&&e.getWorld()==clone.getWorld())candidates.add(e);}
        if(candidates.isEmpty())return null;
        if(difficulty==BotDifficulty.NORMAL)return candidates.stream().min(Comparator.comparingDouble(e->e.getLocation().distanceSquared(clone.getLocation()))).orElse(null);
        long now=System.currentTimeMillis();UUID previous=dakkoCloneTarget.get(cloneId);LivingEntity best=null;double bestScore=-Double.MAX_VALUE;
        for(LivingEntity target:candidates){
            UUID tid=target.getUniqueId();double distance=clone.getLocation().distance(target.getLocation());
            double max=target.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?20:target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();double ratio=target.getHealth()/Math.max(1,max);
            double score=-distance+(1-ratio)*(difficulty==BotDifficulty.EXTRA?9.0:6.0);
            if(target instanceof Player)score+=difficulty==BotDifficulty.EXTRA?3.0:1.5;
            if(repairingHeart.containsKey(tid)||playerBotWorkingHeart.containsKey(tid))score+=difficulty==BotDifficulty.EXTRA?8.0:5.0;
            if(isNearRemainingHeart(target.getLocation(),16.0))score+=difficulty==BotDifficulty.EXTRA?5.0:3.0;
            // 字九字は分霊の優先度を大幅に下げる。報復状態のときだけ通常候補に近づける。
            if(tid.equals(azakujiAllyId)&&now>=azakujiOniBotRetaliateUntil)score-=30.0;
            if(tid.equals(previous)){
                score+=difficulty==BotDifficulty.EXTRA?2.5:3.5;
                long chased=Math.max(0,now-dakkoCloneTargetSince.getOrDefault(cloneId,now));double start=dakkoCloneTargetStartDistance.getOrDefault(cloneId,distance);
                boolean closing=(start-distance)>=2.5,nearlyCaught=distance<=4.5||ratio<=.38;
                double wasteMs=(difficulty==BotDifficulty.EXTRA?8.0:12.0)*1000.0;
                if(chased>=wasteMs&&!closing&&!nearlyCaught)score-=difficulty==BotDifficulty.EXTRA?18.0:12.0;
                if(nearlyCaught)score+=difficulty==BotDifficulty.EXTRA?11.0:7.0;
            }
            // 本体と同じ標的への過集中を少し避け、挟撃・別標的への圧を作る。
            Entity owner=Bukkit.getEntity(dakkoCloneOwner.get(cloneId));if(owner instanceof Mob m&&m.getTarget()!=null&&m.getTarget().getUniqueId().equals(tid)&&candidates.size()>1)score-=difficulty==BotDifficulty.EXTRA?3.5:1.5;
            if(score>bestScore){bestScore=score;best=target;}
        }
        if(best!=null&&!best.getUniqueId().equals(previous)){dakkoCloneTarget.put(cloneId,best.getUniqueId());dakkoCloneTargetSince.put(cloneId,now);dakkoCloneTargetStartDistance.put(cloneId,clone.getLocation().distance(best.getLocation()));}
        return best;
    }
    private void removeExpiredDakkoCloneBots(){
        long now=System.currentTimeMillis();
        for(UUID id:new HashSet<>(dakkoCloneBots))if(now>=dakkoCloneExpiresAt.getOrDefault(id,0L)||Bukkit.getEntity(id)==null)dismissDakkoCloneBot(id,false);
    }
    private void dismissDakkoCloneBot(UUID id,boolean effect){
        Entity e=Bukkit.getEntity(id);
        if(e!=null&&effect&&e.getWorld()!=null){e.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,e.getLocation().add(0,1,0),30,.45,.8,.45,.04);e.getWorld().spawnParticle(Particle.SMOKE_LARGE,e.getLocation().add(0,1,0),22,.4,.65,.4,.03);e.getWorld().playSound(e.getLocation(),Sound.ENTITY_FOX_TELEPORT,.55f,.8f);}
        if(e!=null)e.remove();
        dakkoCloneBots.remove(id);dakkoCloneExpiresAt.remove(id);dakkoCloneNextAttackAt.remove(id);dakkoCloneOwner.remove(id);dakkoCloneDifficulty.remove(id);dakkoCloneTarget.remove(id);dakkoCloneTargetSince.remove(id);dakkoCloneTargetStartDistance.remove(id);
    }
    private void removeDakkoCloneBots(){for(UUID id:new HashSet<>(dakkoCloneBots))dismissDakkoCloneBot(id,false);}
    private void useBotKishinRoar(Mob bot){
        double radius=getConfig().getDouble("kishin-skills.roar.radius",8.0),damage=getConfig().getDouble("kishin-skills.roar.fixed-damage",5.0),knock=getConfig().getDouble("kishin-skills.roar.knockback",.65);int slowTicks=getConfig().getInt("kishin-skills.roar.slowness-ticks",60),weakTicks=getConfig().getInt("kishin-skills.roar.weakness-ticks",80);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1.35f,.55f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,bot.getLocation().add(0,1,0),8,1.4,.8,1.4,.05);bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation().add(0,1,0),90,radius*.55,.8,radius*.55,.08);
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;dealOniSkillFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(bot,q,knock,.32);q.sendActionBar(cc("&4鬼吼 &7――身体が竦んだ！"));}
        for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity q=(LivingEntity)Bukkit.getEntity(id);dealOniSkillFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(bot,q,knock,.32);}
        botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("kishin-skills.roar.cooldown-seconds",28));recoverOniSkillMomentum();
    }
    private void useBotKishinIronBody(Mob bot){int ticks=Math.max(20,getConfig().getInt("kishin-skills.iron-body.duration-ticks",120));bot.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,ticks,0,false,true));bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,true));bot.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,ticks,1,false,true));bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1,0),60,.65,1,.65,.08);bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_SHIELD_BLOCK,1.1f,.65f);botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("kishin-skills.iron-body.cooldown-seconds",35));recoverOniSkillMomentum();}

    private boolean isKishinIronBodyActive(LivingEntity entity){
        if(oniType!=OniType.KISHIN||entity==null)return false;
        PotionEffect effect=entity.getPotionEffect(PotionEffectType.INCREASE_DAMAGE);
        return effect!=null&&effect.getAmplifier()>=1;
    }
    private void useBotFoxFire(Mob bot){double power=oniSkillEffectMultiplier(),radius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0)*power,damage=getConfig().getDouble("dakko-skills.fox-fire.fixed-damage",5.0);int fireTicks=(int)Math.round(getConfig().getInt("dakko-skills.fox-fire.fire-ticks",100)*power);renderEmpoweredOniSkill(bot);bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_FIRECHARGE_USE,1.2f,.65f);bot.getWorld().spawnParticle(Particle.FLAME,bot.getLocation().add(0,1,0),100,radius*.55,1.2,radius*.55,.08);bot.getWorld().spawnParticle(Particle.LAVA,bot.getLocation(),25,radius*.5,.7,radius*.5,.03);for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;q.setFireTicks(Math.max(q.getFireTicks(),fireTicks));dealOniSkillFixedDamage(q,damage);applyDakkoFoxMark(q);q.sendActionBar(cc("&6狐火に焼かれている！"));}for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity target=(LivingEntity)Bukkit.getEntity(id);target.setFireTicks(Math.max(target.getFireTicks(),fireTicks));dealOniSkillFixedDamage(target,damage);applyDakkoFoxMark(target);}botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.fox-fire.cooldown-seconds",30));recoverOniSkillMomentum();}

    private void useBotHeavenlyArrival(Mob bot,LivingEntity target){Location landing=target.getLocation().clone();double height=getConfig().getDouble("dakko-skills.heavenly-arrival.launch-height",10.0);Location sky=landing.clone().add(0,height,0);sky.setY(Math.min(sky.getY(),bot.getWorld().getMaxHeight()-2));heavenlyArrival.add(bot.getUniqueId());bot.setTarget(null);bot.teleport(sky);bot.setFallDistance(0);bot.getWorld().playSound(sky,Sound.ENTITY_PHANTOM_FLAP,1.2f,.55f);int windup=Math.max(4,getConfig().getInt("bot.dakko-heavenly-windup-ticks",12));Bukkit.getScheduler().runTaskLater(this,()->{if(state!=GameState.RUNNING||!bot.isValid()){heavenlyArrival.remove(bot.getUniqueId());return;}Location arrival=isActivePlayerSideTarget(target)?target.getLocation().clone():landing;bot.teleport(arrival);bot.setFallDistance(0);heavenlyArrival.remove(bot.getUniqueId());impactBotHeavenlyArrival(bot);},windup);botNextAbilityAt=System.currentTimeMillis()+oniBotAbilityCooldownMillis(getConfig().getInt("dakko-skills.heavenly-arrival.cooldown-seconds",35));recoverOniSkillMomentum();}

    private void impactBotHeavenlyArrival(Mob bot){double power=oniSkillEffectMultiplier(),radius=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-radius",5.0)*power,damage=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-damage",7.0);renderEmpoweredOniSkill(bot);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.2f,.65f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,bot.getLocation(),5,.8,.4,.8,.05);bot.getWorld().spawnParticle(Particle.FLAME,bot.getLocation(),70,radius*.45,.8,radius*.45,.06);for(UUID id:players){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(bot,q,radius))continue;if(botDifficulty!=BotDifficulty.NORMAL)collectDakkoFoxMark(bot,q,"天来");dealOniSkillFixedDamage(q,damage);pushFrom(bot,q);}for(UUID id:playerBots){if(!validPlayerBotSkillTarget(bot,id,radius))continue;LivingEntity target=(LivingEntity)Bukkit.getEntity(id);if(botDifficulty!=BotDifficulty.NORMAL)collectDakkoFoxMark(bot,target,"天来");dealOniSkillFixedDamage(target,damage);pushFrom(bot,target);}}
    private boolean isActivePlayerSideTarget(LivingEntity target){if(target==null||!target.isValid()||target.isDead())return false;UUID id=target.getUniqueId();return players.contains(id)&&!dead.contains(id)&&!escaped.contains(id)||playerBots.contains(id)&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&!isDownEscape(id);}
    private boolean validPlayerBotSkillTarget(LivingEntity hunter,UUID id,double radius){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id)||!(Bukkit.getEntity(id) instanceof LivingEntity target))return false;return target.isValid()&&!target.isDead()&&target.getWorld().equals(hunter.getWorld())&&target.getLocation().distanceSquared(hunter.getLocation())<=radius*radius;}
    private void pushFrom(LivingEntity source,LivingEntity target){Vector push=target.getLocation().toVector().subtract(source.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);target.setVelocity(push.normalize().multiply(.85).setY(.45));}
    private void updatePlayerBots(){
        if(state!=GameState.RUNNING||playerBots.isEmpty())return;LivingEntity hunter=getOniEntity();
        if(isTagMode()){updateTagPlayerBots(hunter);return;}
        for(UUID id:new HashSet<>(playerBots)){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){deadPlayerBots.add(id);continue;}
            if(bot instanceof Mob mob)mob.setTarget(null);
            tickIkimonoBotBuffUse(id,bot,false);LivingEntity beastTarget=nearestIkimono(bot,10.0);if(beastTarget!=null){double bd=bot.getLocation().distance(beastTarget.getLocation());if(bd<=2.5){stopPlayerBotNavigation(bot);long now=System.currentTimeMillis();if(now>=playerBotNextAttackAt.getOrDefault(id,0L)){playerBotNextAttackAt.put(id,now+900);bot.swingMainHand();beastTarget.damage(3.0,bot);}}else movePlayerBot(bot,beastTarget.getLocation(),getConfig().getDouble("player-bot.hunt-step",.78));continue;}
            if(id.equals(azakujiAllyId)){updateAzakujiAlly(bot,hunter);continue;}
            if(isDownEscape(id)){
                clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                if(hunter!=null&&hunter.getWorld().equals(bot.getWorld())){
                    Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                    if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                    movePlayerBot(bot,bot.getLocation().clone().add(away.normalize().multiply(12)),1.15);
                }
                continue;
            }
            boolean lastLife=extraLifeConsumed.contains(id);
            if(lastLife&&hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()){
                double hd=bot.getLocation().distance(hunter.getLocation());double stealthRadius=getConfig().getDouble("player-bot.last-life.stealth-radius",24.0);
                if(hd<stealthRadius){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                    double danger=getConfig().getDouble("player-bot.last-life.full-flee-radius",12.0);boolean detected=hd<=danger;bot.setSilent(!detected);
                    Location safe=bot.getLocation().clone().add(away.normalize().multiply(detected?10:6));double step=detected?getConfig().getDouble("player-bot.flee-step",.85):getConfig().getDouble("player-bot.last-life.stealth-step",.34);
                    step*=updatePlayerBotStamina(id,detected);movePlayerBot(bot,safe,step);continue;
                }else bot.setSilent(false);
            }else bot.setSilent(false);
            if((id.equals(configuredFriendlyNpcId)||eliteBotMatchIds.contains(id))&&getConfig().getBoolean("friendly-npc.elite-ai.enabled",true)){updateEliteFriendlyNpc(bot,hunter);continue;}
            Location destination=null;boolean fleeing=false,hunting=false;
            if(heartGoalReached()){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);hunting=shouldPlayerBotHuntOni(bot,hunter);if(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot)&&hunter!=null)hunting=true;destination=hunting&&hunter!=null?hunter.getLocation():chooseBotExit(bot,hunter);}
            else if(hunter!=null&&hunter.getWorld().equals(bot.getWorld())&&hunter.getLocation().distanceSquared(bot.getLocation())<Math.pow(getConfig().getDouble("player-bot.flee-radius",10.0),2)){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<0.01)away=new Vector(1,0,0);double side=((id.hashCode()&1)==0?1:-1)*getConfig().getDouble("player-bot.movement.flee-strafe",3.0);Vector lateral=new Vector(-away.getZ(),0,away.getX()).normalize().multiply(side);destination=bot.getLocation().clone().add(away.normalize().multiply(8)).add(lateral);fleeing=true;}
            else destination=selectPlayerBotHeart(id,bot);
            if(destination==null)continue;double distance=bot.getLocation().distance(destination);
            if(hunting&&oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot)){LivingEntity snake=findNearestJakutsukiSnake(bot,getConfig().getDouble("jakutsuki-final.bot-snake-clear-radius",7.0));if(snake!=null&&(hunter==null||bot.getLocation().distanceSquared(hunter.getLocation())>9.0)){double snakeDistance=bot.getLocation().distance(snake.getLocation());if(snakeDistance<=2.6){stopPlayerBotNavigation(bot);bot.swingMainHand();jakutsukiSnakes.remove(snake.getUniqueId());snake.getWorld().spawnParticle(Particle.SWEEP_ATTACK,snake.getLocation().add(0,.5,0),3,.2,.2,.2,0);snake.getWorld().playSound(snake.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.8f,1.25f);snake.remove();continue;}movePlayerBot(bot,snake.getLocation(),getConfig().getDouble("jakutsuki-final.bot-hunt-step",0.92));continue;}}
            usePlayerBotSkill(id,bot,hunter,fleeing,hunting);
            if(!fleeing&&!heartGoalReached()&&distance<=2.6){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);advanceHeartBot(id,bot,destination.getBlock());continue;}if(!fleeing)clearPlayerBotWork(id);
            if(hunting&&hunter!=null&&distance<=getConfig().getDouble("player-bot.hunt-attack-range",2.4)){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);attackOniWithPlayerBot(id,bot,hunter);continue;}

            double step=fleeing?getConfig().getDouble("player-bot.flee-step",0.85):hunting?getConfig().getDouble("player-bot.hunt-step",0.78):getConfig().getDouble("player-bot.move-step",0.65);if(hunting&&oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))step=Math.max(step,getConfig().getDouble("jakutsuki-final.bot-hunt-step",0.92));boolean skillSprint=System.currentTimeMillis()<playerBotSprintUntil.getOrDefault(id,0L);if(skillSprint)step*=getConfig().getDouble("player-bot.skills.sprint-step-multiplier",1.45);if(fleeing&&hasPlayerBotPassive(id,PassiveSkill.COWARDICE))step*=getConfig().getDouble("player-bot.passives.cowardice-flee-step-multiplier",1.08);if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))step*=1.05;step*=updatePlayerBotStamina(id,fleeing||hunting||skillSprint);movePlayerBot(bot,destination,step);
        }
    }

    private Location clampTagLocation(Location l){Location c=LocationStore.get(getConfig(),"tag-mode.center");if(l==null||c==null||c.getWorld()==null||l.getWorld()!=c.getWorld())return l;double half=Math.max(8,getConfig().getDouble("tag-mode.border-size",200.0)/2.0-2.0);l.setX(Math.max(c.getX()-half,Math.min(c.getX()+half,l.getX())));l.setZ(Math.max(c.getZ()-half,Math.min(c.getZ()+half,l.getZ())));return l;}
    // v0.40.73 鬼ごっこ専用AI: 生存 > 脱出/宝石 > 補給 > 探索。
    private void updateTagPlayerBots(LivingEntity hunter){
        long now=System.currentTimeMillis();
        double danger=Math.max(6.0,getConfig().getDouble("tag-mode.player-bot.flee-radius",18.0));
        double fleeStep=getConfig().getDouble("tag-mode.player-bot.flee-step",1.02);
        double roamStep=getConfig().getDouble("tag-mode.player-bot.roam-step",0.72);
        for(UUID id:new HashSet<>(playerBots)){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;
            if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)){deadPlayerBots.add(id);continue;}
            if(bot instanceof Mob mob)mob.setTarget(null);
            if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null&&bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()!=10.0){bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10.0);bot.setHealth(Math.min(bot.getHealth(),10.0));}
            clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
            double oniDist=hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()?hunter.getLocation().distance(bot.getLocation()):Double.MAX_VALUE;
            // 危険時は目的を捨てる。宝石進行は人間同様75%保持。
            if(oniDist<=danger){
                preserveTagBotGemProgress(id);tagBotGateChannelTicks.remove(id);
                usePlayerBotSkill(id,bot,hunter,true,false);
                Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                Vector side=new Vector(-away.getZ(),0,away.getX()).normalize().multiply(((id.hashCode()&1)==0?1:-1)*3.5);
                Location flee=clampTagLocation(bot.getLocation().clone().add(away.normalize().multiply(12)).add(side));
                movePlayerBot(bot,flee,fleeStep*updatePlayerBotStamina(id,true));continue;
            }
            // ゲート解放後、霊力4なら三つのうち鬼から遠いゲートへ。到着後5秒詠唱。
            if(tagTerraGatesOpened&&tagTerraGems.getOrDefault(id,new LinkedHashSet<>()).size()>=4){
                Location gate=bestTagBotGate(bot,hunter);
                if(gate!=null){double d=horizontalDistanceSquared(bot.getLocation(),gate.clone().add(.5,0,.5));
                    if(d<=25.0&&Math.abs(bot.getLocation().getY()-gate.getY())<=6.0){
                        stopPlayerBotNavigation(bot);int ticks=tagBotGateChannelTicks.getOrDefault(id,0)+10;tagBotGateChannelTicks.put(id,ticks);
                        if(ticks>=100){tagBotGateChannelTicks.remove(id);escapedPlayerBots.add(id);bot.getWorld().spawnParticle(Particle.TOTEM,bot.getLocation().add(0,1,0),35,.5,.8,.5,.12);bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_TOTEM_USE,.8f,1.15f);bot.remove();}
                    }else{tagBotGateChannelTicks.remove(id);movePlayerBot(bot,gate,roamStep*1.08*updatePlayerBotStamina(id,false));}continue;
                }
            }
            // 未取得宝石を距離だけでなく鬼の位置も加味して選ぶ。
            String gem=bestTagBotGem(id,bot,hunter);if(gem!=null){Location gl=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+gem);if(gl!=null){double d=bot.getLocation().distanceSquared(gl.clone().add(.5,.5,.5));
                if(d<=30.25){updateTagBotGemAttunement(id,bot,gem,now);continue;}
                preserveTagBotGemProgress(id);movePlayerBot(bot,gl,roamStep*updatePlayerBotStamina(id,false));continue;}}
            // 目的に余裕がある時だけ無料物資/スキルチェストを探索。
            Location chest=nearestTagBotChest(id,bot);if(chest!=null){if(bot.getLocation().distanceSquared(chest)<=6.25){tagBotVisitedChests.computeIfAbsent(id,k->new HashSet<>()).add(LocationStore.encode(chest));tagBotNextRetargetAt.put(id,0L);bot.swingMainHand();bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_CHEST_OPEN,.45f,1.2f);assignTagBotChestSkill(id);}else{movePlayerBot(bot,chest,roamStep*updatePlayerBotStamina(id,false));continue;}}
            Location target=tagBotRoamTarget.get(id);if(target==null||target.getWorld()!=bot.getWorld()||now>=tagBotNextRetargetAt.getOrDefault(id,0L)||bot.getLocation().distanceSquared(target)<9){Random r=new Random(id.getLeastSignificantBits()^now);double radius=Math.max(8,getConfig().getDouble("tag-mode.player-bot.roam-radius",22.0));target=clampTagLocation(bot.getLocation().clone().add((r.nextDouble()*2-1)*radius,0,(r.nextDouble()*2-1)*radius));tagBotRoamTarget.put(id,target);tagBotNextRetargetAt.put(id,now+4000+r.nextInt(5000));}movePlayerBot(bot,target,roamStep*updatePlayerBotStamina(id,false));
        }
    }
    private double horizontalDistanceSquared(Location a,Location b){double x=a.getX()-b.getX(),z=a.getZ()-b.getZ();return x*x+z*z;}
    private String bestTagBotGem(UUID id,LivingEntity bot,LivingEntity hunter){Set<String> got=tagTerraGems.getOrDefault(id,new LinkedHashSet<>());String best=null;double score=Double.MAX_VALUE;for(String g:List.of("diamond","gold","emerald","lapis")){if(got.contains(g))continue;Location l=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems."+g);if(l==null||l.getWorld()!=bot.getWorld())continue;double s=bot.getLocation().distanceSquared(l);if(hunter!=null&&hunter.isValid()&&hunter.getWorld()==l.getWorld()){double od=hunter.getLocation().distanceSquared(l);if(od<900)s+=(900-od)*2.5;}Map<String,Double> pg=tagTerraGemProgress.get(id);if(pg!=null)s-=pg.getOrDefault(g,0.0)*4.0;if(s<score){score=s;best=g;}}return best;}
    private void preserveTagBotGemProgress(UUID id){String g=tagTerraAttuningGem.remove(id);if(g!=null){Map<String,Double> pg=tagTerraGemProgress.computeIfAbsent(id,k->new HashMap<>());pg.put(g,pg.getOrDefault(g,0.0)*.75);}tagBotGemNextCheckAt.remove(id);}
    private void updateTagBotGemAttunement(UUID id,LivingEntity bot,String gem,long now){String active=tagTerraAttuningGem.get(id);if(active!=null&&!active.equals(gem))preserveTagBotGemProgress(id);tagTerraAttuningGem.put(id,gem);Map<String,Double> pg=tagTerraGemProgress.computeIfAbsent(id,k->new HashMap<>());double progress=pg.getOrDefault(gem,0.0);double botBase=switch(gem){case "gold"->3.0;case "lapis"->2.75;default->100.0/30.0;};progress+=botBase; // 0.5秒tick、宝石ごとの個性を反映
        long check=tagBotGemNextCheckAt.getOrDefault(id,0L);if(now>=check){Random r=new Random(id.getMostSignificantBits()^now^gem.hashCode());double roll=r.nextDouble();double failChance="gold".equals(gem)?.17:.12;double greatChance="gold".equals(gem)?.20:("lapis".equals(gem)?.18:.22);if(roll<failChance){recordTagResonance(false,true,gem);progress=Math.max(0,progress-("lapis".equals(gem)?6:12));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_ANVIL_LAND,.8f,.65f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,bot.getLocation().add(0,1,0),1);}else if(roll<failChance+greatChance){recordTagResonance(true,true,gem);progress+=terraGemGreatGain(gem);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.35f,1.7f);}else{progress+=terraGemSuccessGain(gem);}tagBotGemNextCheckAt.put(id,now+("lapis".equals(gem)?1200+r.nextInt(900):1800+r.nextInt(1200)));}
        if("emerald".equals(gem)){Location gl=LocationStore.get(getConfig(),"tag-mode.maps.terra.gems.emerald");if(gl!=null&&gl.getWorld()==bot.getWorld()){Particle.DustOptions ed=new Particle.DustOptions(terraGemColor(gem),1.3f);for(int y=0;y<=20;y+=4)gl.getWorld().spawnParticle(Particle.REDSTONE,gl.clone().add(.5,1+y*.5,.5),2,.1,.05,.1,0,ed);}}
        pg.put(gem,Math.min(100,progress));Color c=terraGemColor(gem);bot.getWorld().spawnParticle(Particle.REDSTONE,bot.getLocation().add(0,1,0),3,.25,.35,.25,new Particle.DustOptions(c,1.0f));if(progress>=100){tagTerraGems.computeIfAbsent(id,k->new LinkedHashSet<>()).add(gem);pg.remove(gem);tagTerraAttuningGem.remove(id);tagBotGemNextCheckAt.remove(id);soulPoints.merge(id,2,Integer::sum);matchEarnedSoulPoints.merge(id,2,Integer::sum);bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.8f,1.45f);}}
    private Location bestTagBotGate(LivingEntity bot,LivingEntity hunter){Location best=null;double score=Double.MAX_VALUE;for(int i=1;i<=3;i++){Location g=terraGate(i);if(g==null||g.getWorld()!=bot.getWorld())continue;double s=bot.getLocation().distanceSquared(g);if(hunter!=null&&hunter.isValid()&&hunter.getWorld()==g.getWorld()){double od=hunter.getLocation().distanceSquared(g);if(od<1600)s+=(1600-od)*3.0;}if(s<score){score=s;best=g;}}return best;}
    private void assignTagBotChestSkill(UUID id){List<PlayerSkill> pool=new ArrayList<>(Arrays.asList(PlayerSkill.values()));pool.remove(PlayerSkill.SAFE_LANDING);if(pool.isEmpty())return;PlayerSkill current=playerBotSkills.get(id),pick=pool.get(Math.floorMod(id.hashCode()+tagBotVisitedChests.getOrDefault(id,Set.of()).size(),pool.size()));if(current==null||new Random().nextBoolean())playerBotSkills.put(id,pick);}
    private Location nearestTagBotChest(UUID id,LivingEntity bot){Set<String> visited=tagBotVisitedChests.computeIfAbsent(id,k->new HashSet<>());Location best=null;double bestD=Double.MAX_VALUE;Set<String> all=new HashSet<>(lootChestKeys);all.addAll(tagSkillChestKeys);for(String key:all){if(visited.contains(key))continue;Location l=LocationStore.decode(key);if(l==null||l.getWorld()!=bot.getWorld())continue;double d=bot.getLocation().distanceSquared(l);if(d<bestD){bestD=d;best=l;}}return best;}

    private void updateEliteFriendlyNpc(LivingEntity bot,LivingEntity hunter){
        UUID id=bot.getUniqueId();if(isDownEscape(id))return;
        String preset=parseFriendlyPreset(playerBotPresetNames.getOrDefault(id,""));
        if(preset==null)preset=parseFriendlyPreset(getConfig().getString("friendly-npc.preset",""));
        if(preset==null)return;
        long now=System.currentTimeMillis();
        double baseStep=getConfig().getDouble("player-bot.move-step",.65)*getConfig().getDouble("friendly-npc.elite-ai.movement-multiplier",1.12);
        double chaseStep=getConfig().getDouble("player-bot.flee-step",.85)*getConfig().getDouble("friendly-npc.elite-ai.chase-movement-multiplier",1.18);
        double danger=getConfig().getDouble("friendly-npc.elite-ai.danger-radius",12.0);
        double oniDistance=hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()?bot.getLocation().distance(hunter.getLocation()):Double.MAX_VALUE;
        Player threatened=findMostThreatenedHuman(hunter);
        LivingEntity injured=findMostInjuredAlly(bot);
        boolean finalPhaseNow=heartGoalReached();

        if(finalPhaseNow){
            clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
            boolean canFight=hunter!=null&&hunter.isValid()&&hunter.getWorld()==bot.getWorld()
                    &&("IGAMI_KYOYA".equals(preset)||"AZANAMI_REN".equals(preset)||"KAGAYA_RION".equals(preset));
            if(canFight&&bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())>.55){
                usePlayerBotSkill(id,bot,hunter,false,true);
                double d=bot.getLocation().distance(hunter.getLocation());
                if(d<=getConfig().getDouble("player-bot.hunt-attack-range",2.4))attackOniWithPlayerBot(id,bot,hunter);
                else movePlayerBot(bot,hunter.getLocation(),baseStep*1.08);
            }else {Location chosenExit=chooseBotExit(bot,hunter);if(chosenExit!=null)movePlayerBot(bot,chosenExit,baseStep*1.10);}
            return;
        }

        switch(preset){
            case "IGAMI_KYOYA" -> {
                if(hunter!=null&&threatened!=null&&hunter.getWorld()==bot.getWorld()
                        &&hunter.getLocation().distanceSquared(threatened.getLocation())<=Math.pow(getConfig().getDouble("friendly-npc.elite-ai.intercept-radius",10.0),2)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    usePlayerBotSkill(id,bot,hunter,false,true);
                    Vector away=threatened.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                    if(away.lengthSquared()<.01)away=new Vector(1,0,0);
                    Location intercept=threatened.getLocation().clone().subtract(away.normalize().multiply(2.2));
                    movePlayerBot(bot,intercept,chaseStep);
                    return;
                }
                if(oniDistance<danger){usePlayerBotSkill(id,bot,hunter,true,false);Location flee=eliteFleeDestination(bot,hunter,9);movePlayerBot(bot,flee,chaseStep);return;}
            }
            case "AZANAMI_MISAKI" -> {
                if(oniDistance<Math.max(danger,15.0)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);usePlayerBotSkill(id,bot,hunter,true,false);
                    movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,14),chaseStep*1.08);return;
                }
            }
            case "ARIKAWA_FUUKA" -> {
                if(oniDistance<danger+2){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);usePlayerBotSkill(id,bot,hunter,true,false);movePlayerBot(bot,eliteFleeDestination(bot,hunter,12),chaseStep);return;}
            }
            case "AZANAMI_REN" -> {
                if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&(oniDistance<18||threatened!=null)){
                    usePlayerBotSkill(id,bot,hunter,oniDistance<10,true);
                    if(oniDistance<8){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,11),chaseStep);return;}
                }
            }
            case "MEDIC" -> {
                double supportRatio=getConfig().getDouble("friendly-npc.elite-ai.support-health-ratio",.70);
                if(injured!=null&&injured.getHealth()/Math.max(1,injured.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())<=supportRatio){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&injured.getLocation().distanceSquared(hunter.getLocation())<danger*danger){
                        movePlayerBot(bot,eliteFleeDestination(bot,hunter,10),chaseStep);return;
                    }
                    double d=bot.getLocation().distance(injured.getLocation());
                    if(d>getConfig().getDouble("friendly-npc.elite-ai.medic-follow-distance",4.5))movePlayerBot(bot,injured.getLocation(),baseStep*1.08);
                    else{stopPlayerBotNavigation(bot);usePlayerBotSkill(id,bot,hunter,false,false);eliteHealTarget(bot,injured);}
                    return;
                }
                if(oniDistance<danger){movePlayerBot(bot,eliteFleeDestination(bot,hunter,10),chaseStep);return;}
            }
            case "AKASAKA_HIIRO" -> {
                if(oniDistance<danger+3){clearPlayerBotWork(id);playerBotTargetHeart.remove(id);bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,30,2,false,true));if(bot.isOnGround())bot.setVelocity(bot.getVelocity().setY(.55));movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,14),chaseStep*1.12);return;}
            }
            case "KAGAYA_RION" -> {
                if(oniDistance<getConfig().getDouble("friendly-npc.elite-ai.rion-blink-trigger-radius",10.0)){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(now>=playerBotSkillReadyAt.getOrDefault(id,0L)&&eliteRionBlink(id,bot,hunter)){return;}
                    movePlayerBot(bot,eliteFleeDestinationAwayFromTeam(bot,hunter,12),chaseStep*1.10);return;
                }
            }
            case "AMANAI_IONA" -> {
                if(hunter!=null&&hunter.getWorld()==bot.getWorld()&&(oniDistance<16||(threatened!=null&&hunter.getLocation().distanceSquared(threatened.getLocation())<144))){
                    clearPlayerBotWork(id);playerBotTargetHeart.remove(id);
                    if(now>=playerBotSkillReadyAt.getOrDefault(id,0L))eliteIonaDecoy(id,bot,hunter,threatened);
                    if(oniDistance<danger)movePlayerBot(bot,eliteFleeDestination(bot,hunter,11),chaseStep);
                    else if(threatened!=null)movePlayerBot(bot,threatened.getLocation(),baseStep);
                    return;
                }
            }
        }

        Location heart=selectEliteFriendlyHeart(id,bot,hunter);
        if(heart==null)return;
        double d=bot.getLocation().distance(heart);
        if(d<=2.6){stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);advanceHeartBot(id,bot,heart.getBlock());}
        else{clearPlayerBotWork(id);double step=baseStep*updatePlayerBotStamina(id,true);movePlayerBot(bot,heart,step);}
    }

    private Player findMostThreatenedHuman(LivingEntity hunter){
        if(hunter==null)return null;Player best=null;double bestScore=Double.MAX_VALUE;
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=hunter.getWorld())continue;double d=p.getLocation().distanceSquared(hunter.getLocation());if(d<bestScore){bestScore=d;best=p;}}
        return best;
    }
    private LivingEntity findMostInjuredAlly(LivingEntity from){
        LivingEntity best=null;double bestRatio=1.01;
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||p.getWorld()!=from.getWorld())continue;double ratio=p.getHealth()/Math.max(1,p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(ratio<bestRatio){bestRatio=ratio;best=p;}}
        for(UUID id:playerBots){if(id.equals(from.getUniqueId())||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity e)||e.getWorld()!=from.getWorld())continue;double ratio=e.getHealth()/Math.max(1,e.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(ratio<bestRatio){bestRatio=ratio;best=e;}}
        return best;
    }
    private Location selectEliteFriendlyHeart(UUID id,LivingEntity bot,LivingEntity hunter){
        Set<String> choices=new HashSet<>(heartHp.keySet());choices.addAll(fakeHeartKeys);String best=null;double bestScore=Double.MAX_VALUE;
        for(String key:choices){
            Location l=LocationStore.decode(key);if(l==null||l.getWorld()!=bot.getWorld())continue;
            double score=l.distanceSquared(bot.getLocation());
            long bots=playerBotTargetHeart.entrySet().stream().filter(e->!e.getKey().equals(id)&&key.equals(e.getValue())).count();
            score+=bots*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-bot-penalty",75.0);
            int humans=0;for(UUID pid:players){Player p=Bukkit.getPlayer(pid);if(p!=null&&!dead.contains(pid)&&!escaped.contains(pid)&&p.getWorld()==l.getWorld()&&p.getLocation().distanceSquared(l)<36)humans++;}
            score+=humans*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-player-penalty",110.0);
            if(hunter!=null&&hunter.getWorld()==l.getWorld()){double oni=Math.min(1600,l.distanceSquared(hunter.getLocation()));score-=oni*getConfig().getDouble("friendly-npc.elite-ai.safe-heart-oni-distance-weight",1.8);}
            if(score<bestScore){bestScore=score;best=key;}
        }
        if(best==null)return null;playerBotTargetHeart.put(id,best);return LocationStore.decode(best);
    }
    private Location eliteFleeDestination(LivingEntity bot,LivingEntity hunter,double distance){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return bot.getLocation();
        Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);
        return bot.getLocation().clone().add(away.normalize().multiply(distance));
    }
    private Location eliteFleeDestinationAwayFromTeam(LivingEntity bot,LivingEntity hunter,double distance){
        Location base=eliteFleeDestination(bot,hunter,distance);Vector push=new Vector();
        for(UUID id:players){Player p=Bukkit.getPlayer(id);if(p==null||dead.contains(id)||escaped.contains(id)||p.getWorld()!=bot.getWorld())continue;Vector away=bot.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(away.lengthSquared()>0&&away.lengthSquared()<100)push.add(away.normalize());}
        if(push.lengthSquared()>0)base.add(push.normalize().multiply(4));return base;
    }
    private boolean eliteRionBlink(UUID id,LivingEntity bot,LivingEntity hunter){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return false;
        Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);if(away.lengthSquared()<.01)away=new Vector(1,0,0);away.normalize();
        double distance=getConfig().getDouble("friendly-npc.elite-ai.rion-blink-distance",4.0);
        Location dest=bot.getLocation().clone().add(away.multiply(distance));
        for(int y=1;y>=-1;y--){Location test=dest.clone().add(0,y,0);if(test.getBlock().isPassable()&&test.clone().add(0,1,0).getBlock().isPassable()&&!test.clone().add(0,-1,0).getBlock().isPassable()){bot.teleport(test);bot.getWorld().spawnParticle(Particle.PORTAL,test,32,.4,.8,.4,.15);bot.getWorld().playSound(test,Sound.ENTITY_ENDERMAN_TELEPORT,.75f,1.25f);playerBotSkillReadyAt.put(id,System.currentTimeMillis()+PlayerSkill.BLINK.cooldown*1000L);return true;}}
        return false;
    }
    private void eliteIonaDecoy(UUID id,LivingEntity bot,LivingEntity hunter,Player threatened){
        if(hunter==null||hunter.getWorld()!=bot.getWorld())return;
        Vector fromHunter=(threatened!=null?threatened.getLocation():bot.getLocation()).toVector().subtract(hunter.getLocation().toVector()).setY(0);if(fromHunter.lengthSquared()<.01)fromHunter=new Vector(1,0,0);
        Vector side=new Vector(-fromHunter.getZ(),0,fromHunter.getX()).normalize().multiply(((id.hashCode()&1)==0?1:-1)*getConfig().getDouble("friendly-npc.elite-ai.iona-decoy-distance",10.0));
        Location at=hunter.getLocation().clone().add(side);Item decoy=bot.getWorld().dropItem(at,new ItemStack(Material.SNOWBALL));decoy.setPickupDelay(Integer.MAX_VALUE);decoy.setVelocity(new Vector(0,.18,0));
        fakeNoiseUntil.put(decoy.getUniqueId(),System.currentTimeMillis()+Math.max(1,getConfig().getLong("friendly-npc.elite-ai.iona-decoy-duration-seconds",8))*1000L);
        bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),28,.5,.8,.5,.03);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_SNOWBALL_THROW,.75f,.7f);
        playerBotSkillReadyAt.put(id,System.currentTimeMillis()+Math.max(1,getConfig().getLong("friendly-npc.elite-ai.iona-decoy-cooldown-seconds",18))*1000L);
    }
    private void eliteHealTarget(LivingEntity healer,LivingEntity target){
        if(target==null||target.isDead()||!target.isValid()||target.getWorld()!=healer.getWorld())return;
        double max=target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(target.getHealth()>=max)return;
        double amount=getConfig().getDouble("player-bot.skills.heal-amount",20.0);target.setHealth(Math.min(max,target.getHealth()+amount));
        target.getWorld().spawnParticle(Particle.HEART,target.getLocation().add(0,1,0),8,.4,.5,.4,.02);target.getWorld().playSound(target.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.5f,1.45f);
    }

    private void updateAzakujiAlly(LivingEntity bot,LivingEntity hunter){
        UUID id=bot.getUniqueId();long now=System.currentTimeMillis();
        updateAzakujiLight(bot);
        if(hunter==null||!hunter.isValid()||hunter.isDead()||hunter.getWorld()!=bot.getWorld()){
            Location ally=nearestActiveHumanLocation(bot);
            if(ally!=null&&bot.getLocation().distanceSquared(ally)>Math.pow(getConfig().getDouble("azakuji-ally.guard-ai.follow-distance",4.5),2))movePlayerBot(bot,ally,getConfig().getDouble("player-bot.move-step",.65)*1.08);
            return;
        }

        double max=Math.max(1.0,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        // 残り10分以下で強制消耗。神性憑依より優先し、再生・高防御・跳躍無敵を失う。
        if(!azakujiForcedAttrition && secondsLeft<=Math.max(0,getConfig().getInt("azakuji-ally.forced-attrition.remaining-seconds",600))){
            azakujiForcedAttrition=true;
            azakujiLeapInvulnerableUntil=0L;
            if(bot.getAttribute(Attribute.GENERIC_ARMOR)!=null){
                double base=Math.max(0.0,getConfig().getDouble("azakuji-ally.armor",16.0));
                double mult=Math.max(0.0,getConfig().getDouble("azakuji-ally.forced-attrition.armor-multiplier",0.5));
                bot.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(base*mult);
            }
            bot.removePotionEffect(PotionEffectType.REGENERATION);
            all("&6字九字ひろ &e「……流石に、少し堪えてきたな。」 &8《神性消耗》");
        }
        double hpRatio=bot.getHealth()/max;
        // 神性憑依: NPC専用。瀕死時に1試合1回だけ超強化する。
        if(!azakujiDivinePossessionUsed&&hpRatio<=getConfig().getDouble("azakuji-ally.divine-possession.trigger-health-ratio",0.30)){
            azakujiDivinePossessionUsed=true;
            azakujiDivineUntil=now+Math.max(1,getConfig().getLong("azakuji-ally.divine-possession.duration-seconds",22))*1000L;
            bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,(int)Math.max(20,getConfig().getLong("azakuji-ally.divine-possession.duration-seconds",22)*20),1,false,false));
            bot.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,(int)Math.max(20,getConfig().getLong("azakuji-ally.divine-possession.duration-seconds",22)*20),1,false,false));
            if(!azakujiForcedAttrition)bot.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,1,false,false));
            all("&6&l《神性憑依》 &e字九字ひろ &f――『……仕方ない。少しだけ、本気を出す。』");
            bot.getWorld().playSound(bot.getLocation(),Sound.ITEM_TOTEM_USE,1.0f,.8f);bot.getWorld().spawnParticle(Particle.TOTEM,bot.getLocation().add(0,1,0),90,.8,1.0,.8,.12);
        }
        boolean divine=now<azakujiDivineUntil;
        long activeSeconds=azakujiSpawnedAt<=0?0:Math.max(0,(now-azakujiSpawnedAt)/1000L);
        long primeSeconds=Math.max(10,getConfig().getLong("azakuji-ally.attrition.prime-seconds",90));
        long wornSeconds=Math.max(primeSeconds,getConfig().getLong("azakuji-ally.attrition.worn-seconds",180));
        double attritionMultiplier=activeSeconds<primeSeconds?1.0:(activeSeconds<wornSeconds?getConfig().getDouble("azakuji-ally.attrition.worn-multiplier",0.82):getConfig().getDouble("azakuji-ally.attrition.exhausted-multiplier",0.66));
        Player threatened=findThreatenedHumanForAzakuji(hunter);
        double nearestHumanThreat=threatened==null?Double.MAX_VALUE:threatened.getLocation().distance(hunter.getLocation());
        double combatDetect=getConfig().getDouble("azakuji-ally.guard-ai.protect-detection-radius",16.0);
        boolean combatNearby=nearestHumanThreat<=combatDetect||bot.getLocation().distance(hunter.getLocation())<=combatDetect||now<azakujiFightUntil||now<azakujiEscapeUntil;
        if(combatNearby)azakujiLastCombatAt=now;
        // 鬼と同等クラスの常時再生。updateBotsは0.5秒周期なので毎回0.5秒分を回復する。
        if(bot.getHealth()<max && !azakujiForcedAttrition){
            double regen=Math.max(0.0,getConfig().getDouble("azakuji-ally.regeneration-per-second",getConfig().getDouble("oni-heart-protection.regeneration-per-second",1.0)));
            bot.setHealth(Math.min(max,bot.getHealth()+regen*0.5));
            if(((now/500L)&3L)==0L)bot.getWorld().spawnParticle(Particle.HEART,bot.getLocation().add(0,1.25,0),1,.18,.22,.18,.01);
        }
        double oniDistance=bot.getLocation().distance(hunter.getLocation());
        double detection=getConfig().getDouble("azakuji-ally.guard-ai.protect-detection-radius",16.0);

        // Only at critical health: make a short evasive leap, then immediately return to combat.
        if(hpRatio<=getConfig().getDouble("azakuji-ally.guard-ai.low-health-ratio",0.15)&&azakujiEscapeUntil<now){
            azakujiEscapeUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.escape-seconds",2.0)*1000L);
            azakujiTauntUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L);
        }
        if(now<azakujiEscapeUntil){
            clearPlayerBotWork(id);
            playerBotTargetHeart.remove(id);
            makeAzakujiTaunt(bot,now);
            Vector away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
            if(away.lengthSquared()<.01)away=new Vector(1,0,0);
            Vector side=new Vector(-away.getZ(),0,away.getX()).normalize().multiply(((now/1000L)&1L)==0L?3.5:-3.5);
            Location escape=bot.getLocation().clone().add(away.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.escape-distance",10.0))).add(side);
            azakujiLeapEscape(bot,hunter,escape,now);
            movePlayerBot(bot,escape,getConfig().getDouble("player-bot.flee-step",.85)*1.18);
            return;
        }

        boolean protecting=threatened!=null&&threatened.getWorld()==bot.getWorld()
                &&hunter.getLocation().distanceSquared(threatened.getLocation())<=detection*detection;
        boolean directEngage=oniDistance<=detection;
        // 遊撃索敵: プレイヤー周辺を巡回し、視認または近距離で鬼を発見したら即座に交戦する。
        double scoutRange=Math.max(detection,getConfig().getDouble("azakuji-ally.scout-ai.detection-radius",28.0));
        boolean scoutSpotted=oniDistance<=scoutRange&&(oniDistance<=getConfig().getDouble("azakuji-ally.scout-ai.close-detection-radius",12.0)||bot.hasLineOfSight(hunter));
        if(scoutSpotted&&now>=azakujiFightUntil)azakujiFightUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.fight-seconds",14.0)*1000L);

        if(protecting||directEngage||scoutSpotted||now<azakujiFightUntil){
            clearPlayerBotWork(id);
            playerBotTargetHeart.remove(id);
            if((protecting||directEngage)&&now>=azakujiFightUntil)azakujiFightUntil=now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.fight-seconds",14.0)*1000L);

            // Interpose himself between the Oni and the endangered player.
            Location guardPoint=threatened!=null?threatened.getLocation().clone():bot.getLocation().clone();
            if(threatened!=null){
                Vector fromOni=threatened.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
                if(fromOni.lengthSquared()<.01)fromOni=new Vector(1,0,0);
                guardPoint=threatened.getLocation().clone().subtract(fromOni.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.intercept-distance",3.0)));
            }

            // 瞬歩: 近距離TPで側面・背後へ回り込み、救援や回避に使う。
            if(shouldAzakujiBlink(bot,hunter,threatened,now,oniDistance)){
                useAzakujiBlink(bot,hunter,threatened,now);
                return;
            }

            // 神雷・縛: 序盤限定・回数制限の救援拘束。赤い粒子の雷で鬼の足を止める。
            if(shouldAzakujiUseDivineThunder(bot,hunter,threatened,now,activeSeconds)){
                useAzakujiDivineThunder(bot,hunter,now);
                return;
            }

            // 武芸 is his first choice when he can safely dive in.
            if(now>=playerBotSkillReadyAt.getOrDefault(id,0L)
                    &&oniDistance<=getConfig().getDouble("skills.bugei.target-range",18.0)
                    &&oniDistance>=3.5){
                azakujiLastCombatAt=now;
                usePlayerBotBugei(id,bot,hunter);
                if(oniBot!=null&&hunter.getUniqueId().equals(oniBot))azakujiOniBotRetaliateUntil=Math.max(azakujiOniBotRetaliateUntil,now+Math.max(1,getConfig().getLong("azakuji-ally.oni-bot-retaliation-seconds",7))*1000L);
                playerBotSkillReadyAt.put(id,now+Math.max(1,PlayerSkill.BUGEI.cooldown)*1000L);
                azakujiTauntUntil=Math.max(azakujiTauntUntil,now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L));
                return;
            }

            if(oniDistance<=getConfig().getDouble("azakuji-ally.guard-ai.melee-range",2.8)){
                stopPlayerBotNavigation(bot);updatePlayerBotStamina(id,false);
                if(now>=azakujiNextMeleeAt){
                    azakujiNextMeleeAt=now+Math.max(1,getConfig().getLong("azakuji-ally.guard-ai.melee-cooldown-ticks",18))*50L;
                    bot.swingMainHand();
                    azakujiLastCombatAt=now;
                    double damage=getConfig().getDouble("azakuji-ally.guard-ai.melee-damage",7.0);
                    damage*=1.20; // 神喰
                    damage*=getConfig().getDouble("azakuji-ally.divine-technique-extreme.attack-multiplier",1.35); // 神技・極
                    if(divine)damage*=getConfig().getDouble("azakuji-ally.divine-possession.attack-multiplier",1.35);
                    damage*=attritionMultiplier;
                    hunter.damage(damage,bot);
                    if(oniBot!=null&&hunter.getUniqueId().equals(oniBot))azakujiOniBotRetaliateUntil=Math.max(azakujiOniBotRetaliateUntil,now+Math.max(1,getConfig().getLong("azakuji-ally.oni-bot-retaliation-seconds",7))*1000L);
                    bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_ATTACK_STRONG,.75f,.9f);
                    azakujiTauntUntil=Math.max(azakujiTauntUntil,now+(long)(getConfig().getDouble("azakuji-ally.guard-ai.taunt-seconds",8.0)*1000L));
                }
            }else{
                Location destination=oniDistance<=7.0?hunter.getLocation():guardPoint;
                if(now>=azakujiNextLeapAt&&bot.isOnGround()&&oniDistance>=3.0&&oniDistance<=11.0){Vector toward=hunter.getLocation().toVector().subtract(bot.getLocation().toVector()).setY(0);if(toward.lengthSquared()>.01){toward.normalize();Vector side=new Vector(-toward.getZ(),0,toward.getX()).multiply(((now/700L)&1L)==0L?0.72:-0.72);Vector leap=toward.multiply(divine?0.98:0.82).add(side);leap.setY(divine?0.76:0.64);bot.setVelocity(leap);bot.setFallDistance(0);grantAzakujiLeapInvulnerability(now);azakujiNextLeapAt=now+(long)(Math.max(5,getConfig().getLong("azakuji-ally.guard-ai.combat-leap-cooldown-ticks",16))*50L/Math.max(0.55,attritionMultiplier));bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),10,.25,.1,.25,.04);}}
                movePlayerBot(bot,destination,getConfig().getDouble("player-bot.hunt-step",.78)*(divine?1.42:getConfig().getDouble("azakuji-ally.divine-technique-extreme.combat-step-multiplier",1.24))*attritionMultiplier);
            }

            makeAzakujiTaunt(bot,now);
            // v0.31.2: fixed-time disengage removed; Azakuji stays in combat unless critically wounded.
            return;
        }

        // 非戦闘時はプレイヤーの近くを遊撃巡回。走行・跳躍・瞬歩で周囲を索敵する。
        Location ally=nearestActiveHumanLocation(bot);
        if(ally==null)ally=nearestActivePlayerSideLocation(bot);
        if(ally!=null){
            double patrolRadius=Math.max(5.0,getConfig().getDouble("azakuji-ally.scout-ai.patrol-radius",14.0));
            double maxFromPlayers=Math.max(patrolRadius,getConfig().getDouble("azakuji-ally.scout-ai.max-distance-from-players",22.0));
            if(bot.getLocation().distanceSquared(ally)>maxFromPlayers*maxFromPlayers){
                azakujiPatrolTarget=ally.clone();
                movePlayerBot(bot,ally,getConfig().getDouble("player-bot.move-step",.65)*1.28);
                return;
            }
            if(azakujiPatrolTarget==null||now>=azakujiNextPatrolRetargetAt||azakujiPatrolTarget.getWorld()!=bot.getWorld()||bot.getLocation().distanceSquared(azakujiPatrolTarget)<4.0){
                double angle=Math.random()*Math.PI*2.0;double radius=patrolRadius*(0.45+Math.random()*0.55);
                Location desired=ally.clone().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                Location safe=findSafeGroundNear(desired,4);azakujiPatrolTarget=safe!=null?safe:ally.clone();
                azakujiNextPatrolRetargetAt=now+Math.max(1,getConfig().getLong("azakuji-ally.scout-ai.retarget-seconds",4))*1000L;
            }
            double patrolDistance=bot.getLocation().distance(azakujiPatrolTarget);
            // 長めの移動では跳躍。戦闘用無敵も付くため、巡回中の事故死を防ぐ。
            if(now>=azakujiNextLeapAt&&bot.isOnGround()&&patrolDistance>=5.0){
                Vector toward=azakujiPatrolTarget.toVector().subtract(bot.getLocation().toVector()).setY(0);
                if(toward.lengthSquared()>.01){toward.normalize().multiply(getConfig().getDouble("azakuji-ally.scout-ai.leap-horizontal-speed",0.92));toward.setY(getConfig().getDouble("azakuji-ally.scout-ai.leap-upward-velocity",0.66));bot.setVelocity(toward);bot.setFallDistance(0);grantAzakujiLeapInvulnerability(now);azakujiNextLeapAt=now+Math.max(8,getConfig().getLong("azakuji-ally.scout-ai.leap-cooldown-ticks",22))*50L;}
            }
            // 巡回中も瞬歩を使う。ただし戦闘用CTと共通なので乱用はしない。
            if(getConfig().getBoolean("azakuji-ally.scout-ai.use-blink",true)&&now>=azakujiNextBlinkAt&&patrolDistance>=getConfig().getDouble("azakuji-ally.scout-ai.blink-min-distance",9.0)){
                Location safe=findSafeGroundNear(azakujiPatrolTarget,3);
                if(safe!=null){Location from=bot.getLocation().clone();bot.teleport(safe);bot.setFallDistance(0);grantAzakujiLeapInvulnerability(now);azakujiNextBlinkAt=now+Math.max(3,getConfig().getLong("azakuji-ally.short-blink.cooldown-seconds",13))*1000L;Particle.DustOptions red=new Particle.DustOptions(org.bukkit.Color.fromRGB(180,20,35),1.0f);from.getWorld().spawnParticle(Particle.REDSTONE,from.add(0,1,0),12,.25,.4,.25,0,red);safe.getWorld().spawnParticle(Particle.REDSTONE,safe.clone().add(0,1,0),16,.25,.4,.25,0,red);safe.getWorld().playSound(safe,Sound.ENTITY_ENDERMAN_TELEPORT,.45f,1.65f);return;}
            }
            movePlayerBot(bot,azakujiPatrolTarget,getConfig().getDouble("player-bot.move-step",.65)*1.18);
        }
    }

    private boolean shouldAzakujiBlink(LivingEntity bot,LivingEntity hunter,Player threatened,long now,double distance){
        if(!getConfig().getBoolean("azakuji-ally.short-blink.enabled",true)||now<azakujiNextBlinkAt)return false;
        double range=Math.max(3.0,getConfig().getDouble("azakuji-ally.short-blink.trigger-range",8.0));
        if(distance>range)return false;
        double hp=bot.getHealth()/Math.max(1.0,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        boolean rescue=threatened!=null&&hunter.getLocation().distanceSquared(threatened.getLocation())<=36.0;
        return rescue||hp<=0.65||distance<=4.0;
    }
    private void useAzakujiBlink(LivingEntity bot,LivingEntity hunter,Player threatened,long now){
        double min=Math.max(2.0,getConfig().getDouble("azakuji-ally.short-blink.min-distance",4.0));
        double max=Math.max(min,getConfig().getDouble("azakuji-ally.short-blink.max-distance",8.0));
        Vector forward=hunter.getLocation().getDirection().setY(0);if(forward.lengthSquared()<.01)forward=new Vector(0,0,1);forward.normalize();
        Vector side=new Vector(-forward.getZ(),0,forward.getX()).multiply(((now/500L)&1L)==0L?1:-1);
        double dist=min+Math.random()*(max-min);
        Location desired=hunter.getLocation().clone().add(forward.clone().multiply(-Math.min(3.0,dist*.45))).add(side.multiply(Math.max(2.5,dist*.65)));
        Location safe=findSafeGroundNear(desired,3);
        if(safe==null)return;
        Location from=bot.getLocation().clone();bot.teleport(safe);bot.setFallDistance(0);
        grantAzakujiLeapInvulnerability(now);
        azakujiNextBlinkAt=now+Math.max(3,getConfig().getLong("azakuji-ally.short-blink.cooldown-seconds",13))*1000L;
        Particle.DustOptions red=new Particle.DustOptions(org.bukkit.Color.fromRGB(180,20,35),1.2f);
        from.getWorld().spawnParticle(Particle.REDSTONE,from.clone().add(0,1,0),18,.35,.6,.35,0,red);
        safe.getWorld().spawnParticle(Particle.REDSTONE,safe.clone().add(0,1,0),24,.35,.6,.35,0,red);
        safe.getWorld().playSound(safe,Sound.ENTITY_ENDERMAN_TELEPORT,.65f,1.55f);
    }
    private Location findSafeGroundNear(Location base,int radius){
        if(base==null||base.getWorld()==null)return null;
        for(int r=0;r<=radius;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            Location q=base.clone().add(dx,0,dz);int y=q.getWorld().getHighestBlockYAt(q);Location feet=new Location(q.getWorld(),q.getBlockX()+.5,y+1,q.getBlockZ()+.5);
            if(feet.getBlock().isPassable()&&feet.clone().add(0,1,0).getBlock().isPassable())return feet;
        }return null;
    }

    private void grantAzakujiLeapInvulnerability(long now){
        if(azakujiForcedAttrition)return;
        long millis=Math.max(250,getConfig().getLong("azakuji-ally.leap-invulnerability-millis",1800));
        azakujiLeapInvulnerableUntil=Math.max(azakujiLeapInvulnerableUntil,now+millis);
    }
    private boolean shouldAzakujiUseDivineThunder(LivingEntity bot,LivingEntity hunter,Player threatened,long now,long activeSeconds){
        if(!getConfig().getBoolean("azakuji-ally.divine-thunder.enabled",true))return false;
        if(azakujiDivineThunderUses>=Math.max(0,getConfig().getInt("azakuji-ally.divine-thunder.max-uses",5)))return false;
        if(activeSeconds>Math.max(1,getConfig().getLong("azakuji-ally.divine-thunder.available-seconds",120)))return false;
        if(now<azakujiNextDivineThunderAt||hunter==null||hunter.getWorld()!=bot.getWorld())return false;
        double range=Math.max(2,getConfig().getDouble("azakuji-ally.divine-thunder.range",11.0));
        if(bot.getLocation().distanceSquared(hunter.getLocation())>range*range)return false;
        boolean rescue=threatened!=null&&hunter.getLocation().distanceSquared(threatened.getLocation())<=49.0;
        double hp=bot.getHealth()/Math.max(1.0,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        return rescue||hp<=0.55||heartGoalReached();
    }
    private void useAzakujiDivineThunder(LivingEntity bot,LivingEntity hunter,long now){
        azakujiDivineThunderUses++;
        azakujiNextDivineThunderAt=now+Math.max(1,getConfig().getLong("azakuji-ally.divine-thunder.cooldown-seconds",30))*1000L;
        int bindTicks=Math.max(10,getConfig().getInt("azakuji-ally.divine-thunder.bind-ticks",50));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,bindTicks,10,false,true));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP,bindTicks,128,false,false));
        hunter.setVelocity(new Vector(0,0,0));
        Location base=hunter.getLocation();
        Particle.DustOptions red=new Particle.DustOptions(org.bukkit.Color.fromRGB(255,20,20),1.65f);
        for(int bolt=0;bolt<5;bolt++){double a=(Math.PI*2.0*bolt/5.0)+(now%1000)/1000.0;double r=bolt==0?0:1.5;for(double y=.15;y<=5.8;y+=.32){Location q=base.clone().add(Math.cos(a)*r+Math.sin(y*3+a)*.12,y,Math.sin(a)*r+Math.cos(y*3+a)*.12);base.getWorld().spawnParticle(Particle.REDSTONE,q,2,.035,.035,.035,0,red);}}
        base.getWorld().playSound(base,Sound.ENTITY_LIGHTNING_BOLT_THUNDER,1.0f,1.35f);
        base.getWorld().playSound(base,Sound.BLOCK_BEACON_POWER_SELECT,.9f,.55f);
        all("&c&l《神雷・縛》 &e字九字ひろ &fが鬼を赤雷で拘束した！");
        if(oniBot!=null&&hunter.getUniqueId().equals(oniBot))azakujiOniBotRetaliateUntil=Math.max(azakujiOniBotRetaliateUntil,now+Math.max(1,getConfig().getLong("azakuji-ally.oni-bot-retaliation-seconds",7))*1000L);
        azakujiTauntUntil=Math.max(azakujiTauntUntil,now+bindTicks*50L);
    }

    private Player findThreatenedHumanForAzakuji(LivingEntity hunter){
        Player best=null;double bestDist=Double.MAX_VALUE;
        for(UUID pid:players){
            if(dead.contains(pid)||escaped.contains(pid))continue;
            Player p=Bukkit.getPlayer(pid);
            if(p==null||!p.isOnline()||p.getWorld()!=hunter.getWorld())continue;
            double d=p.getLocation().distanceSquared(hunter.getLocation());
            if(d<bestDist){bestDist=d;best=p;}
        }
        return best;
    }

    private Location nearestActiveHumanLocation(LivingEntity from){
        Location best=null;double bestDist=Double.MAX_VALUE;
        for(UUID pid:players){
            if(dead.contains(pid)||escaped.contains(pid))continue;
            Player p=Bukkit.getPlayer(pid);
            if(p==null||!p.isOnline()||p.getWorld()!=from.getWorld())continue;
            double d=p.getLocation().distanceSquared(from.getLocation());
            if(d<bestDist){bestDist=d;best=p.getLocation();}
        }
        return best;
    }

    private Location nearestActivePlayerSideLocation(LivingEntity from){
        Location best=nearestActiveHumanLocation(from);double bestDist=best==null?Double.MAX_VALUE:best.distanceSquared(from.getLocation());
        for(UUID other:playerBots){
            if(other.equals(from.getUniqueId())||deadPlayerBots.contains(other)||escapedPlayerBots.contains(other))continue;
            if(!(Bukkit.getEntity(other) instanceof LivingEntity entity)||!entity.isValid()||entity.isDead()||entity.getWorld()!=from.getWorld())continue;
            double d=entity.getLocation().distanceSquared(from.getLocation());
            if(d<bestDist){bestDist=d;best=entity.getLocation();}
        }
        return best;
    }

    private void makeAzakujiTaunt(LivingEntity bot,long now){
        if(now>=azakujiTauntUntil)return;
        bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,Math.max(10,getConfig().getInt("azakuji-ally.guard-ai.aggro-glow-ticks",30)),0,false,false));
        if(now%1000L<550L)bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1.1,0),5,.25,.35,.25,.02);
    }

    private void azakujiLeapEscape(LivingEntity bot,LivingEntity hunter,Location escape,long now){
        if(now<azakujiNextLeapAt||!bot.isOnGround())return;
        azakujiNextLeapAt=now+Math.max(4,getConfig().getLong("azakuji-ally.guard-ai.leap-cooldown-ticks",24))*50L;
        stopPlayerBotNavigation(bot);
        Vector away=escape.toVector().subtract(bot.getLocation().toVector()).setY(0);
        if(away.lengthSquared()<.01)away=bot.getLocation().toVector().subtract(hunter.getLocation().toVector()).setY(0);
        if(away.lengthSquared()<.01)away=new Vector(1,0,0);
        away.normalize().multiply(getConfig().getDouble("azakuji-ally.guard-ai.leap-horizontal-speed",.95));
        away.setY(getConfig().getDouble("azakuji-ally.guard-ai.leap-upward-velocity",.72));
        bot.setVelocity(away);bot.setFallDistance(0);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_HORSE_JUMP,.6f,1.35f);
        bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),16,.3,.12,.3,.05);
    }

    private double updatePlayerBotStamina(UUID id,boolean running){double value=playerBotStamina.getOrDefault(id,20.0),drain=getConfig().getDouble("stamina.drain-per-second",2.0)/2.0,recovery=getConfig().getDouble("stamina.recovery-per-second",1.5)/2.0;if(hasPlayerBotPassive(id,PassiveSkill.LIGHT_FOOTED))drain*=.85;if(hasPlayerBotPassive(id,PassiveSkill.DEEP_BREATH))recovery*=1.25;if(running&&value>0)value-=drain;else value+=recovery;value=Math.max(0,Math.min(20,value));playerBotStamina.put(id,value);return value<=0?getConfig().getDouble("player-bot.stamina-empty-step-multiplier",.55):1.0;}
    private Location selectPlayerBotHeart(UUID id,LivingEntity bot){String current=playerBotTargetHeart.get(id);if(current!=null&&(heartHp.containsKey(current)||fakeHeartKeys.contains(current))){Location location=LocationStore.decode(current);if(location!=null&&location.getWorld().equals(bot.getWorld()))return location;}Set<String> botHeartChoices=new HashSet<>(heartHp.keySet());botHeartChoices.addAll(fakeHeartKeys);String chosen=botHeartChoices.stream().filter(key->{Location l=LocationStore.decode(key);return l!=null&&l.getWorld().equals(bot.getWorld());}).min(Comparator.comparingDouble(key->{Location l=LocationStore.decode(key);long assigned=playerBotTargetHeart.entrySet().stream().filter(e->!e.getKey().equals(id)&&e.getValue().equals(key)).count();return l.distanceSquared(bot.getLocation())+assigned*getConfig().getDouble("player-bot.movement.shared-heart-penalty",64.0);})).orElse(null);if(chosen==null)return null;playerBotTargetHeart.put(id,chosen);return LocationStore.decode(chosen);}
    private void usePlayerBotSkill(UUID id,LivingEntity bot,LivingEntity hunter,boolean fleeing,boolean hunting){
        PlayerSkill skill=playerBotSkills.getOrDefault(id,PlayerSkill.SPRINT);long now=System.currentTimeMillis();if(now<playerBotSkillReadyAt.getOrDefault(id,0L))return;double distance=hunter!=null&&hunter.isValid()&&hunter.getWorld().equals(bot.getWorld())?hunter.getLocation().distance(bot.getLocation()):Double.MAX_VALUE;boolean used=false;
        switch(skill){
            case SPRINT -> {if(fleeing||(hunting&&distance>3)){playerBotSprintUntil.put(id,now+(long)(getConfig().getDouble("player-bot.skills.sprint-seconds",5.0)*1000));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_HORSE_GALLOP,.55f,1.25f);bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),12,.3,.1,.3,.04);used=true;}}
            case SMOKE -> {if(fleeing&&distance<=getConfig().getDouble("player-bot.skills.smoke-range",8.0)){bot.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,bot.getLocation().add(0,1,0),70,2.2,1.0,2.2,.03);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,.65f,1.5f);if(hunter!=null)hunter.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,Math.max(20,getConfig().getInt("player-bot.skills.smoke-blindness-ticks",60)),0,false,false));used=true;}}
            case ONI_STRIKE -> {if(hunter!=null&&distance<=getConfig().getDouble("player-bot.skills.strike-range",4.0)){bot.swingMainHand();double damage=heartGoalReached()?getConfig().getDouble("player-bot.skills.strike-damage-final",12.0):getConfig().getDouble("player-bot.skills.strike-damage-sealed",2.0);if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;hunter.damage(damage,bot);Vector push=hunter.getLocation().toVector().subtract(bot.getLocation().toVector());if(push.lengthSquared()>.01)hunter.setVelocity(push.normalize().multiply(.8).setY(.3));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,1f,.75f);used=true;}}
            case OBSESSION -> {if(hunter!=null&&(fleeing||hunting)&&distance<=getConfig().getDouble("player-bot.skills.obsession-range",18.0)){int ticks=Math.max(20,getConfig().getInt("player-bot.skills.obsession-ticks",160));bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));hunter.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.65f,1.35f);used=true;}}
            case HEAL -> {double max=bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();boolean needsHealing=bot.getHealth()<=max*getConfig().getDouble("player-bot.skills.heal-threshold",.65),allyNeedsHealing=hasPlayerBotPassive(id,PassiveSkill.BOND)&&hasInjuredBondAlly(bot);if((needsHealing||allyNeedsHealing)&&distance>=getConfig().getDouble("player-bot.skills.heal-safe-distance",9.0)){double amount=getConfig().getDouble("player-bot.skills.heal-amount",20.0);bot.setHealth(Math.min(max,bot.getHealth()+amount));if(hasPlayerBotPassive(id,PassiveSkill.BOND))healBondAllies(bot,amount);bot.getWorld().spawnParticle(Particle.HEART,bot.getLocation().add(0,1,0),8,.4,.5,.4,.02);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.6f,1.4f);used=true;}}
            case INVISIBLE -> {if(fleeing){bot.getWorld().spawnParticle(Particle.SMOKE_NORMAL,bot.getLocation().add(0,1,0),25,.5,.8,.5,.03);playerBotSprintUntil.put(id,now+(long)(getConfig().getDouble("player-bot.skills.invisible-evasion-seconds",4.0)*1000));used=true;}}
            case BUGEI -> {if(hunter!=null&&distance<=getConfig().getDouble("skills.bugei.target-range",18.0)){usePlayerBotBugei(id,bot,hunter);used=true;}}
            case SAFE_LANDING,BLINK,ECHO,CLAIRVOYANCE,UNYIELDING,SEALING_CIRCLE,RESONANCE,SUBSTITUTE,DESPERATE_RUN -> {}
        }
        if(used)playerBotSkillReadyAt.put(id,now+Math.max(1,skill.cooldown)*1000L);
    }
    private void usePlayerBotBugei(UUID id,LivingEntity bot,LivingEntity hunter){
        if(bot==null||hunter==null||!bot.isValid()||!hunter.isValid())return;
        stopPlayerBotNavigation(bot);
        if(id.equals(azakujiAllyId)&&!azakujiForcedAttrition)azakujiLeapInvulnerableUntil=Math.max(azakujiLeapInvulnerableUntil,System.currentTimeMillis()+Math.max(1200,getConfig().getLong("azakuji-ally.bugei-invulnerability-millis",3200)));
        bot.setVelocity(new Vector(0,getConfig().getDouble("skills.bugei.launch-upward-velocity",1.05),0));
        bot.setFallDistance(0);
        bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.55f,1.5f);
        bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation(),18,.3,.15,.3,.06);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(state!=GameState.RUNNING||!bot.isValid()||!hunter.isValid())return;
            final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
            task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
                ticks[0]++;
                if(state!=GameState.RUNNING||!bot.isValid()||!hunter.isValid()||bot.getWorld()!=hunter.getWorld()){task[0].cancel();return;}
                Vector to=hunter.getEyeLocation().toVector().subtract(bot.getLocation().toVector());double distance=to.length();
                double speed=getConfig().getDouble("skills.bugei.dive-speed",1.75);
                if(distance<=getConfig().getDouble("skills.bugei.final-boost-distance",5.0))speed*=getConfig().getDouble("skills.bugei.final-boost-multiplier",1.35);
                if(distance>.01)bot.setVelocity(to.normalize().multiply(speed));bot.setFallDistance(0);
                Location trail=bot.getLocation().add(0,.85,0);
                bot.getWorld().spawnParticle(Particle.REDSTONE,trail,2,.1,.1,.1,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,45,20),1.2f));
                bot.getWorld().spawnParticle(Particle.REDSTONE,trail,1,.08,.08,.08,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,190,35),1.0f));
                if(distance<=getConfig().getDouble("skills.bugei.impact-distance",2.2)){
                    double damage=heartGoalReached()?getConfig().getDouble("skills.bugei.final-damage",10.0):getConfig().getDouble("skills.bugei.sealed-damage",6.0);
                    if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;
                    if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10);
                    hunter.damage(damage,bot);Location impact=hunter.getLocation().add(0,1,0);
                    bot.getWorld().playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,.5f,1.55f);
                    bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,impact,2,.15,.15,.15,.01);
                    bot.getWorld().spawnParticle(Particle.CRIT_MAGIC,impact,28,.5,.6,.5,.1);
                    all("&6字九字ひろ &f「ライダーキック！」");
                    task[0].cancel();return;
                }
                if(ticks[0]>=Math.max(8,getConfig().getInt("skills.bugei.max-dive-ticks",32)))task[0].cancel();
            },0L,1L);
        },8L);
    }
    private LivingEntity findNearestJakutsukiSnake(LivingEntity bot,double radius){LivingEntity best=null;double bestDist=radius*radius;for(UUID sid:new HashSet<>(jakutsukiSnakes)){Entity e=Bukkit.getEntity(sid);if(!(e instanceof LivingEntity snake)||!snake.isValid()||snake.isDead()||snake.getWorld()!=bot.getWorld())continue;double d=snake.getLocation().distanceSquared(bot.getLocation());if(d<=bestDist){bestDist=d;best=snake;}}return best;}
    private boolean shouldPlayerBotHuntOni(LivingEntity bot,LivingEntity hunter){if(hunter==null||!hunter.isValid()||hunter.isDead()||!hunter.getWorld().equals(bot.getWorld()))return false;if(oniType==OniType.JAKUTSUKI&&heartGoalReached()&&playerBotHasSnakeSlash(bot))return true;double botRatio=bot.getHealth()/Math.max(1,bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());if(botRatio<getConfig().getDouble("player-bot.hunt-min-health-ratio",0.60))return false;long allies=playerBots.stream().filter(id->!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id)!=null).count();double oniRatio=hunter.getHealth()/Math.max(1,hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());return allies>=getConfig().getInt("player-bot.hunt-min-allies",2)||oniRatio<=getConfig().getDouble("player-bot.hunt-oni-max-health-ratio",0.50);}
    private void attackOniWithPlayerBot(UUID id,LivingEntity bot,LivingEntity hunter){long now=System.currentTimeMillis();long attackTicks=(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))?getConfig().getLong("jakutsuki-final.bot-attack-cooldown-ticks",14):getConfig().getLong("player-bot.hunt-attack-cooldown-ticks",20);if(now<playerBotNextAttackAt.getOrDefault(id,0L))return;playerBotNextAttackAt.put(id,now+Math.max(1,attackTicks)*50L);bot.swingMainHand();double damage=(oniType==OniType.JAKUTSUKI&&playerBotHasSnakeSlash(bot))?Math.max(0,getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0)):Math.max(0,getConfig().getDouble("player-bot.hunt-attack-damage",4.0));if(hasPlayerBotPassive(id,PassiveSkill.ATTACK_BOOST))damage*=1.20;if(hasPlayerBotPassive(id,PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10);hunter.damage(damage,bot);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_ATTACK_STRONG,0.7f,1.0f);}
    private void movePlayerBot(LivingEntity bot,Location destination,double step){if(!(bot instanceof Mob mob))return;Vector separation=new Vector();double radius=getConfig().getDouble("player-bot.movement.separation-radius",2.2);for(UUID otherId:playerBots){if(otherId.equals(bot.getUniqueId())||!(Bukkit.getEntity(otherId) instanceof LivingEntity other)||!other.getWorld().equals(bot.getWorld()))continue;Vector away=bot.getLocation().toVector().subtract(other.getLocation().toVector()).setY(0);double length=away.length();if(length>0&&length<radius)separation.add(away.normalize().multiply((radius-length)/radius));}Location pathTarget=destination.clone();if(separation.lengthSquared()>0)pathTarget.add(separation.normalize().multiply(Math.min(1.5,radius)));double base=Math.max(.01,getConfig().getDouble("player-bot.move-step",.65)),speed=Math.max(.55,Math.min(1.65,step/base));boolean pathStarted=mob.getPathfinder().moveTo(pathTarget,speed);Location last=playerBotLastLocation.put(bot.getUniqueId(),bot.getLocation().clone());boolean stuck=last!=null&&last.getWorld().equals(bot.getWorld())&&last.distanceSquared(bot.getLocation())<.02;int stuckTicks=stuck?playerBotStuckTicks.getOrDefault(bot.getUniqueId(),0)+1:0;playerBotStuckTicks.put(bot.getUniqueId(),stuckTicks);if((!pathStarted||stuckTicks>=getConfig().getInt("player-bot.movement.stuck-updates-before-jump",3))&&bot.isOnGround())bot.setVelocity(bot.getVelocity().setY(getConfig().getDouble("player-bot.movement.jump-velocity",.42)));if(stuckTicks>=getConfig().getInt("player-bot.movement.stuck-updates-before-recovery",12)){recoverStuckPlayerBot(bot,destination);playerBotStuckTicks.put(bot.getUniqueId(),0);}}
    private void stopPlayerBotNavigation(LivingEntity bot){if(bot instanceof Mob mob)mob.getPathfinder().stopPathfinding();bot.setVelocity(new Vector(0,bot.getVelocity().getY(),0));}
    private void recoverStuckPlayerBot(LivingEntity bot,Location destination){Vector direction=destination.toVector().subtract(bot.getLocation().toVector()).setY(0);if(direction.lengthSquared()>.01){direction.normalize();for(double distance:new double[]{1.5,1.0,.5}){Location candidate=bot.getLocation().clone().add(direction.clone().multiply(distance));for(int y=1;y>=-1;y--){Location adjusted=candidate.clone().add(0,y,0);if(isSafeStandLocation(adjusted)){bot.teleport(adjusted);return;}}}}rescueEntityToSafePlace(bot,false);}
    private Vector findPlayerBotClearDirection(LivingEntity bot,Vector wanted){if(!isPlayerBotBlocked(bot,wanted,1.15))return wanted;double[] angles={38,-38,72,-72,110,-110,180};for(double angle:angles){Vector candidate=rotateHorizontal(wanted,angle);if(!isPlayerBotBlocked(bot,candidate,1.15))return candidate;}return wanted.clone().multiply(-1);}
    private Vector rotateHorizontal(Vector vector,double degrees){double r=Math.toRadians(degrees),cos=Math.cos(r),sin=Math.sin(r);return new Vector(vector.getX()*cos-vector.getZ()*sin,0,vector.getX()*sin+vector.getZ()*cos).normalize();}
    private boolean isPlayerBotBlocked(LivingEntity bot,Vector direction,double distance){Location feet=bot.getLocation().clone().add(direction.clone().multiply(distance));Location head=feet.clone().add(0,1,0);return !feet.getBlock().isPassable()||!head.getBlock().isPassable();}
    private void advanceHeartBot(UUID botId,LivingEntity bot,Block block){
        String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;long now=System.currentTimeMillis();bot.swingMainHand();
        if(!key.equals(playerBotWorkingHeart.get(botId))){playerBotWorkingHeart.put(botId,key);heartProgressCarry.remove(botId);doubleStakesGreatStreak.remove(botId);scheduleNextPlayerBotSkillCheck(botId,now);playerBotSkillChecks.remove(botId);}
        BotSkillCheck check=playerBotSkillChecks.get(botId);
        if(check!=null&&now>=check.resolveAt()){
            playerBotSkillChecks.remove(botId);
            if(check.success()){
                if(check.great()){int bonus=Math.max(0,getConfig().getInt("skill-check-great-bonus",5));if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES)){int streak=doubleStakesGreatStreak.getOrDefault(botId,0)+1;doubleStakesGreatStreak.put(botId,streak);bonus+=Math.min(Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-max-extra",10)),streak*Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-step",2)));}applyScaledHeartDamage(botId,key,bonus);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.7f,1.65f);bot.getWorld().spawnParticle(Particle.FIREWORKS_SPARK,bot.getLocation().add(0,1,0),12,0.3,0.4,0.3,0.02);}
                else if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES)){doubleStakesGreatStreak.remove(botId);int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+Math.max(0,getConfig().getInt("skill-check-failure-penalty",8))));forceHeartAlert(block.getLocation());bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.75f);}
                else if(hasOniPassive(OniPassiveSkill.INTERFERENCE)){regressHeart(block,Math.max(0,getConfig().getInt("oni-passive-skills.interference-normal-regression",2)));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,0.7f,0.75f);}
                else{applyScaledHeartDamage(botId,key,Math.max(0,getConfig().getInt("skill-check-success-bonus",3)));bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.7f,1.5f);bot.getWorld().spawnParticle(Particle.VILLAGER_HAPPY,bot.getLocation().add(0,1,0),5,0.3,0.4,0.3,0.02);}
            }else{doubleStakesGreatStreak.remove(botId);int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+Math.max(0,getConfig().getInt("skill-check-failure-penalty",8))));forceHeartAlert(block.getLocation());bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.0f,0.75f);bot.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,block.getLocation().add(.5,.5,.5),1);}
            scheduleNextPlayerBotSkillCheck(botId,now);
        }else if(check==null&&now>=nextPlayerBotSkillCheck.getOrDefault(botId,Long.MAX_VALUE)){
            boolean extra=getConfig().getBoolean("player-bot.extra.enabled",true);double focus=hasPlayerBotPassive(botId,PassiveSkill.FOCUS)?getConfig().getDouble("player-bot.passives.focus-skill-check-bonus",.08):0;double baseChance=extra?getConfig().getDouble("player-bot.extra.skill-check-success-chance",.91):getConfig().getDouble("player-bot.skill-check-success-chance",0.78);double chance=Math.min(.98,baseChance+focus);if(hasOniPassive(OniPassiveSkill.INTERFERENCE))chance*=Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.interference-bot-success-multiplier",0.80)));boolean success=Math.random()<chance;double greatChance=(extra?getConfig().getDouble("player-bot.extra.skill-check-great-chance",.32):getConfig().getDouble("player-bot.skill-check-great-chance",0.15))+(hasPlayerBotPassive(botId,PassiveSkill.FOCUS)?getConfig().getDouble("player-bot.passives.focus-great-bonus",.05):0);boolean great=success&&Math.random()<Math.max(0,Math.min(.75,greatChance));if(hasPlayerBotPassive(botId,PassiveSkill.DOUBLE_STAKES))success=great;double reactionSec=extra?getConfig().getDouble("player-bot.extra.skill-check-reaction-seconds",.42):getConfig().getDouble("player-bot.skill-check-reaction-seconds",0.65);long reaction=(long)(reactionSec*1000);playerBotSkillChecks.put(botId,new BotSkillCheck(key,now+reaction,success,great));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,0.7f,1.4f);bot.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,bot.getLocation().add(0,1.4,0),6,0.2,0.2,0.2,0.02);
        }
        double amount=Math.max(1,getConfig().getInt("heart-progress-per-interval",1))*Math.max(.25,getConfig().getDouble("player-bot.heart-progress-multiplier",.85));if(hasPlayerBotPassive(botId,PassiveSkill.EXORCISM))amount*=Math.max(1,getConfig().getDouble("passive-skills.exorcism-heart-progress-multiplier",1.10));applyScaledHeartDamage(botId,key,amount);int hp=heartHp.getOrDefault(key,0);updateHeartCracks(block,hp);notifyHeartAttack(block.getLocation(),key,now);if(hp<=0)destroyHeart(block);
    }
    private void scheduleNextPlayerBotSkillCheck(UUID id,long now){long min=(long)(getConfig().getDouble("skill-check-min-seconds",4.0)*1000),max=(long)(getConfig().getDouble("skill-check-max-seconds",9.0)*1000);nextPlayerBotSkillCheck.put(id,now+min+(long)(Math.random()*Math.max(1,max-min)));}
    private void clearPlayerBotWork(UUID id){playerBotWorkingHeart.remove(id);nextPlayerBotSkillCheck.remove(id);playerBotSkillChecks.remove(id);heartProgressCarry.remove(id);doubleStakesGreatStreak.remove(id);}
    private void updateHorrorState(){
        if(state!=GameState.RUNNING)return;LivingEntity hunter=getOniEntity();if(hunter==null)return;long now=System.currentTimeMillis();updateOniSoundIndicator(hunter);
        double terror=getConfig().getDouble("oni-terror-radius",28.0), detect=getConfig().getDouble("oni-detection-range",24.0), fov=getConfig().getDouble("oni-field-of-view",110.0);
        long lostMillis=(long)(getConfig().getDouble("chase-lost-seconds",8.0)*1000L);
        for(UUID id:players){
            if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player target=Bukkit.getPlayer(id);if(target==null)continue;
            double distance=target.getWorld().equals(hunter.getWorld())?target.getLocation().distance(hunter.getLocation()):Double.MAX_VALUE;
            if(distance<=terror)playHeartbeat(target,distance,terror,now);
            double detectRange=detect+(hasOniPassive(OniPassiveSkill.FOOTSTEP_HUNTER)&&target.isSprinting()?Math.max(0,getConfig().getDouble("oni-passive-skills.footstep-hunter-extra-range",6.0)):0);
            boolean visible=now>=timedSkillUntil.getOrDefault(id+":FOOTSTEP_HIDE",0L)&&distance<=detectRange&&hunter.hasLineOfSight(target)&&isInOniView(hunter,target,fov);
            if(visible){lastSeenAt.put(id,now);if(chased.add(id)){onExpandedOniChaseStart(hunter,target,now);beginChase(hunter,target);enterChaseBgm(target);}}
            else if(chased.contains(id)&&now-lastSeenAt.getOrDefault(id,now)>lostMillis){chased.remove(id);onExpandedOniChaseLost(target,now);scheduleLeaveChaseBgm(target);target.playSound(target.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_RESONATE,0.65f,0.72f);}
        }
        chased.removeIf(id->dead.contains(id)||escaped.contains(id)||Bukkit.getPlayer(id)==null);updateOniPassiveEffects(hunter,now);
    }
    private void updateOniSoundIndicator(LivingEntity hunter){if(!(hunter instanceof Player oniPlayer)||!isOni(oniPlayer.getUniqueId())||!getConfig().getBoolean("oni-sound-visualizer.enabled",true))return;double radius=Math.max(8,getConfig().getDouble("oni-sound-visualizer.radius",52)),bestScore=0,bestDistance=0;Location best=null;String soundType="物音";for(UUID id:players){if(dead.contains(id)||escaped.contains(id)||isDownEscape(id))continue;Player target=Bukkit.getPlayer(id);if(target==null||target.getGameMode()==GameMode.SPECTATOR||!target.getWorld().equals(hunter.getWorld()))continue;if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(id+":ECHO_SILENT",0L))continue;if(hasPassive(id,PassiveSkill.SILENT_BREATH)&&target.isSneaking()&&System.currentTimeMillis()-sneakStartedAt.getOrDefault(id,System.currentTimeMillis())>=getConfig().getLong("technical-passives.silent-breath-charge-millis",2000)&&target.getVelocity().setY(0).lengthSquared()<.003)continue;double distance=target.getLocation().distance(hunter.getLocation());if(distance>radius)continue;boolean moving=target.getVelocity().setY(0).lengthSquared()>0.006;double intensity=System.currentTimeMillis()<timedSkillUntil.getOrDefault(id+":DESPERATE",0L)?3.6:repairingHeart.containsKey(id)?3.4:chestOpeningTasks.containsKey(id)?2.8:target.isSprinting()?2.4:moving?(target.isSneaking()?.35:1.1):target.getHealth()<target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*.5?.45:0;if(intensity<=0||distance>radius*Math.min(1,intensity/1.1))continue;double score=intensity/(distance+4);if(score>bestScore){bestScore=score;bestDistance=distance;best=target.getLocation();soundType=repairingHeart.containsKey(id)?"心臓損壊":chestOpeningTasks.containsKey(id)?"開錠音":target.isSprinting()?"走る音":"足音";}}for(UUID id:playerBots){if(id.equals(azakujiAllyId)&&System.currentTimeMillis()>=azakujiOniBotRetaliateUntil)continue;if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||isDownEscape(id))continue;Entity entity=Bukkit.getEntity(id);if(!(entity instanceof LivingEntity bot)||!bot.getWorld().equals(hunter.getWorld()))continue;double distance=bot.getLocation().distance(hunter.getLocation());if(distance>radius)continue;double intensity=playerBotWorkingHeart.containsKey(id)?3.4:bot.getVelocity().setY(0).lengthSquared()>0.006?1.25:0;if(intensity<=0||distance>radius*Math.min(1,intensity/1.1))continue;double score=intensity/(distance+4);if(score>bestScore){bestScore=score;bestDistance=distance;best=bot.getLocation();soundType=playerBotWorkingHeart.containsKey(id)?"心臓損壊":"足音";}}for(Map.Entry<UUID,Long> noise:new HashMap<>(fakeNoiseUntil).entrySet()){Entity entity=Bukkit.getEntity(noise.getKey());if(entity==null||System.currentTimeMillis()>noise.getValue()){fakeNoiseUntil.remove(noise.getKey());if(entity!=null)entity.remove();continue;}if(!entity.getWorld().equals(hunter.getWorld()))continue;double distance=entity.getLocation().distance(hunter.getLocation());double fakeStrength=Math.max(3.0,getConfig().getDouble("technical-passives.decoy-noise-strength",4.6));if(distance<=radius&&fakeStrength/(distance+4)>bestScore){bestScore=fakeStrength/(distance+4);bestDistance=distance;best=entity.getLocation();soundType="激しい走る音";}}if(best==null)return;String arrow=soundDirectionArrow(oniPlayer,best),range=bestDistance<12?"&c近":bestDistance<28?"&e中":"&7遠";oniPlayer.sendActionBar(cc("&4▣ 音響感知  &f"+arrow+"  "+range+" &8| &7"+soundType));Vector direction=best.toVector().subtract(oniPlayer.getEyeLocation().toVector()).setY(0);if(direction.lengthSquared()>.01){direction.normalize();Location origin=oniPlayer.getEyeLocation().add(0,-.35,0);for(int i=1;i<=7;i++)oniPlayer.spawnParticle(Particle.SOUL_FIRE_FLAME,origin.clone().add(direction.clone().multiply(i*.55)),1,0,0,0,0);}}
    private String soundDirectionArrow(Player viewer,Location source){double dx=source.getX()-viewer.getLocation().getX(),dz=source.getZ()-viewer.getLocation().getZ(),targetYaw=Math.toDegrees(Math.atan2(-dx,dz)),relative=targetYaw-viewer.getLocation().getYaw();while(relative<=-180)relative+=360;while(relative>180)relative-=360;String[] arrows={"↑","↗","→","↘","↓","↙","←","↖"};int index=Math.floorMod((int)Math.round(relative/45.0),8);return "&b&l"+arrows[index];}
    private void registerOniGrudge(UUID attacker){
        if(attacker==null)return;if(hasOniPassive(OniPassiveSkill.SPELL_BREAK)||hasOniPassive(OniPassiveSkill.ADAPTATION))oniDisruptionStacks.put(attacker,Math.min(4,oniDisruptionStacks.getOrDefault(attacker,0)+1));if(!hasOniPassive(OniPassiveSkill.GRUDGE_RETURN))return;
        int next=grudgeTokens.getOrDefault(attacker,0)+1;
        if(next>=3){
            grudgeTokens.put(attacker,0);
            grudgeResistReady.add(attacker);
            LivingEntity hunter=getOniEntity();
            if(hunter instanceof Player op){
                Player source=Bukkit.getPlayer(attacker);
                op.sendActionBar(cc("&5怨返し &7――"+(source!=null?source.getName():"獲物")+"への耐性準備"));
            }
        }else grudgeTokens.put(attacker,next);
    }

    private boolean consumeGrudgeResistance(UUID attacker){
        return attacker!=null&&hasOniPassive(OniPassiveSkill.GRUDGE_RETURN)&&grudgeResistReady.remove(attacker);
    }

    private boolean hasOniPassive(OniPassiveSkill passive){
        if(activeOniPassives.contains(passive))return true;
        return state==GameState.WAITING&&!trainingPlayers.isEmpty()&&selectedOniPassives.contains(passive);
    }
    private boolean isOniSkillAction(String id){
        return id!=null&&(id.startsWith("dakko_")||id.startsWith("kishin_")||id.startsWith("shikki_")||id.startsWith("yuuki_")||id.startsWith("kanki_")||id.startsWith("jakutsuki_"));
    }
    private long oniBotAbilityCooldownMillis(double seconds){
        double multiplier=hasOniPassive(OniPassiveSkill.MASTERY)?Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.mastery-cooldown-multiplier",.85))):1.0;LivingEntity hunter=getOniEntity();if(hasOniPassive(OniPassiveSkill.TERRITORY)&&hunter!=null&&oniNearAliveHeart(hunter,getConfig().getDouble("oni-passive-skills.territory-radius",8.0)))multiplier*=Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.territory-cooldown-multiplier",.80)));if(finalPhase)multiplier*=finalCooldownMultiplier();return Math.max(finalPhase?3000L:1000L,(long)(seconds*1000.0*multiplier));
    }
    private void recoverOniSkillMomentum(){
        if(!hasOniPassive(OniPassiveSkill.MOMENTUM))return;
        double amount=Math.max(0,getConfig().getDouble("oni-passive-skills.momentum-stamina-recovery",2.0));
        if(oniBot!=null){
            botStamina=Math.min(20,botStamina+amount);
        }else if(oni!=null){
            double value=Math.min(20,stamina.getOrDefault(oni,20.0)+amount);stamina.put(oni,value);
            Player p=Bukkit.getPlayer(oni);if(p!=null){p.setFoodLevel((int)Math.ceil(value));p.sendActionBar(cc("&6余勢 &7――スキルの勢いでスタミナ回復"));}
        }
    }
    private void onExpandedOniChaseStart(LivingEntity hunter,Player target,long now){
        long idle=oniLastAnyChaseAt==0?Long.MAX_VALUE:now-oniLastAnyChaseAt;oniLastAnyChaseAt=now;
        if(hasOniPassive(OniPassiveSkill.BEAST_PATH))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.beast-path-speed-ticks",50)),0,false,true));
        if(hasOniPassive(OniPassiveSkill.AMBUSH)&&idle>=Math.max(1000,getConfig().getLong("oni-passive-skills.ambush-idle-seconds",10)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.ambush-speed-ticks",70)),1,false,true));
        if(hasOniPassive(OniPassiveSkill.OBSESSION_HUNT)&&now-oniChaseLostAt.getOrDefault(target.getUniqueId(),0L)<=Math.max(1000,getConfig().getLong("oni-passive-skills.obsession-reacquire-seconds",20)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.obsession-speed-ticks",50)),0,false,true));
    }
    private void onExpandedOniChaseLost(Player target,long now){
        oniChaseLostAt.put(target.getUniqueId(),now);
        if(hasOniPassive(OniPassiveSkill.SCENT_TRAIL)){oniScentTrailLocation.put(target.getUniqueId(),target.getLocation().clone());oniScentTrailUntil.put(target.getUniqueId(),now+Math.max(1000,getConfig().getLong("oni-passive-skills.scent-trail-seconds",8)*1000L));}
    }
    private boolean oniNearAliveHeart(LivingEntity hunter,double radius){
        double r2=radius*radius;for(String key:heartHp.keySet()){Location loc=LocationStore.decode(key);if(loc!=null&&loc.getWorld()!=null&&loc.getWorld().equals(hunter.getWorld())&&loc.distanceSquared(hunter.getLocation())<=r2)return true;}return false;
    }
    private void updateExpandedOniPassives(LivingEntity hunter,long now){
        if(hasOniPassive(OniPassiveSkill.SCENT_TRAIL)&&hunter instanceof Player op){for(UUID id:new HashSet<>(oniScentTrailUntil.keySet())){long until=oniScentTrailUntil.getOrDefault(id,0L);Location loc=oniScentTrailLocation.get(id);if(now>until||loc==null){oniScentTrailUntil.remove(id);oniScentTrailLocation.remove(id);continue;}if(loc.getWorld()!=null&&loc.getWorld().equals(op.getWorld()))op.spawnParticle(Particle.CRIMSON_SPORE,loc.clone().add(0,1,0),5,.35,.45,.35,.01);}}
        if(hasOniPassive(OniPassiveSkill.HUNTERS_INSTINCT)&&chased.isEmpty()&&now-oniLastAnyChaseAt>=Math.max(1000,getConfig().getLong("oni-passive-skills.hunters-instinct-idle-seconds",8)*1000L)&&now>=oniHuntersInstinctAt){oniHuntersInstinctAt=now+Math.max(1000,getConfig().getLong("oni-passive-skills.hunters-instinct-interval-seconds",12)*1000L);Player nearest=null;double best=Double.MAX_VALUE;for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(hunter.getWorld()))continue;double d=q.getLocation().distanceSquared(hunter.getLocation());if(d<best){best=d;nearest=q;}}if(nearest!=null&&hunter instanceof Player op)op.sendActionBar(cc("&6狩人の勘 &7――獲物は "+soundDirectionArrow(op,nearest.getLocation())+" &7方向"));}
        if(hasOniPassive(OniPassiveSkill.SEE_THROUGH)&&hunter instanceof Player op){for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(op.getWorld()))continue;boolean hidden=q.hasPotionEffect(PotionEffectType.INVISIBILITY)||now<timedSkillUntil.getOrDefault(id+":ECHO_SILENT",0L)||now<timedSkillUntil.getOrDefault(id+":FOOTSTEP_HIDE",0L);if(hidden&&q.getLocation().distanceSquared(op.getLocation())<=900){op.sendActionBar(cc("&5看破 &7――消えた気配 "+soundDirectionArrow(op,q.getLocation())));break;}}}
        if(hasOniPassive(OniPassiveSkill.PRESSURE)){for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(hunter.getWorld()))continue;if(q.getLocation().distanceSquared(hunter.getLocation())<=Math.pow(getConfig().getDouble("oni-passive-skills.pressure-radius",3.5),2))q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,12,0,false,true));}}
        if(hasOniPassive(OniPassiveSkill.LAST_FORTRESS)&&Math.max(0,totalHearts-brokenHearts)<=Math.max(1,getConfig().getInt("oni-passive-skills.last-fortress-heart-threshold",2))){hunter.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,15,0,false,true));hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,15,0,false,true));}
        if(hasOniPassive(OniPassiveSkill.HUNTING_GROUND)){if(oniHuntingGroundAnchor==null||!oniHuntingGroundAnchor.getWorld().equals(hunter.getWorld())||oniHuntingGroundAnchor.distanceSquared(hunter.getLocation())>36){oniHuntingGroundAnchor=hunter.getLocation().clone();oniHuntingGroundSince=now;}else if(now-oniHuntingGroundSince>=Math.max(1000,getConfig().getLong("oni-passive-skills.hunting-ground-seconds",6)*1000L))hunter.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,15,0,false,true));}else{oniHuntingGroundAnchor=null;oniHuntingGroundSince=0;}
        if(hasOniPassive(OniPassiveSkill.HEART_EYE)&&hunter instanceof Player op){for(String key:repairingHeart.values()){Location loc=LocationStore.decode(key);if(loc!=null&&loc.getWorld()!=null&&loc.getWorld().equals(op.getWorld()))op.spawnParticle(Particle.SOUL_FIRE_FLAME,loc.clone().add(.5,1,.5),8,.3,.45,.3,.01);}}
    }
    private void updateOniPassiveEffects(LivingEntity hunter,long now){
        double botChaseRange=Math.pow(getConfig().getDouble("player-bot.flee-radius",10.0),2);boolean chasing=!chased.isEmpty()||playerBots.stream().filter(id->!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)).map(Bukkit::getEntity).filter(Objects::nonNull).anyMatch(entity->entity.getWorld().equals(hunter.getWorld())&&entity.getLocation().distanceSquared(hunter.getLocation())<=botChaseRange);
        updateExpandedOniPassives(hunter,now);
        if(hasOniPassive(OniPassiveSkill.CRAVING)&&chasing){if(oniChaseStartedAt==0)oniChaseStartedAt=now;long step=Math.max(1,getConfig().getLong("oni-passive-skills.craving-step-seconds",5))*1000L;int level=(int)((now-oniChaseStartedAt)/step),max=Math.max(0,getConfig().getInt("oni-passive-skills.craving-max-amplifier",1));if(level>0)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,15,Math.min(max,level-1),false,true));}else{oniChaseStartedAt=0;}
        if(!hasOniPassive(OniPassiveSkill.BLOOD_SCENT)||now<nextBloodScentAt)return;nextBloodScentAt=now+Math.max(1,getConfig().getLong("oni-passive-skills.blood-scent-interval-seconds",10))*1000L;double threshold=Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.blood-scent-health-ratio",0.70)));int ticks=Math.max(1,getConfig().getInt("oni-passive-skills.blood-scent-glow-ticks",40));boolean found=false;
        for(UUID id:players){Player target=Bukkit.getPlayer(id);if(target==null||dead.contains(id)||escaped.contains(id)||target.getHealth()>target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*threshold)continue;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));found=true;}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity target)||target.getHealth()>target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*threshold)continue;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));found=true;}
        if(found){hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_SNIFF,0.8f,0.8f);if(hunter instanceof Player p)p.sendActionBar(cc("&4血嗅 &7――傷ついた獲物の気配を捉えた"));}
    }
    private boolean isInOniView(LivingEntity hunter,Player target,double fov){if(hunter.getLocation().distanceSquared(target.getLocation())<=9)return true;Vector direction=hunter.getEyeLocation().getDirection().normalize();Vector toward=target.getEyeLocation().toVector().subtract(hunter.getEyeLocation().toVector()).normalize();return direction.dot(toward)>=Math.cos(Math.toRadians(fov/2.0));}
    private void beginChase(LivingEntity hunter,Player target){target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,35,0,false,false));target.playSound(target.getLocation(),Sound.BLOCK_SCULK_SHRIEKER_SHRIEK,1.0f,1.0f);hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_ANGRY,0.5f,1.2f);}
    private void playHeartbeat(Player target,double distance,double terror,long now){double ratio=Math.max(0,Math.min(1,distance/terror));long interval=(long)(350+ratio*1200);if(now-heartbeatAt.getOrDefault(target.getUniqueId(),0L)<interval)return;heartbeatAt.put(target.getUniqueId(),now);float volume=(float)(0.35+(1-ratio)*0.9);float pitch=(float)(0.65+(1-ratio)*0.45);target.playSound(target.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,volume,pitch);}
    private void updateSidebars(){if(state==GameState.RUNNING||state==GameState.ENDING)updateGameSidebar();else updateLobbySidebar();}
    private void updateLobbySidebar(){
        for(Player viewer:Bukkit.getOnlinePlayers()){
            Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
            Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);
            boolean training=isInTrainingArea(viewer.getLocation());
            o.getScore(cc("&7──────────")).setScore(training?5:3);
            if(training){o.getScore(cc("&b&lTRAINING MODE")).setScore(4);o.getScore(cc("&fCT &e"+Math.max(1,getConfig().getInt("training.cooldown-seconds",1))+"秒")).setScore(3);}
            o.getScore(cc("&fオンライン数")).setScore(2);
            o.getScore(cc("&a"+Bukkit.getOnlinePlayers().size()+" &7人")).setScore(1);
            viewer.setScoreboard(board);
        }
    }
    private void updateGameSidebar(){
        if(isEscapeMode()){updateEscapeSidebar();return;}
        if(isTagMode()){for(Player viewer:Bukkit.getOnlinePlayers())viewer.setScoreboard(buildTagSidebar(viewer));return;}
        Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score=15;
        o.getScore(cc("&e残り時間 &f"+(Math.max(0,secondsLeft)/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60))).setScore(score--);
        o.getScore(cc("&c心臓 &f"+brokenHearts+"&7/&f"+totalHearts)).setScore(score--);
        o.getScore(cc("&8──────────")).setScore(score--);
        // Each viewer receives their own board so the remaining-life icon can reflect that viewer.
        // Player/bot roster is appended below by buildGameSidebarForViewer().
        for(Player viewer:Bukkit.getOnlinePlayers())viewer.setScoreboard(buildGameSidebarForViewer(viewer));
        return;
    }
    private Scoreboard buildTagSidebar(Player viewer){Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);int sc=15;long alive=players.stream().filter(id->!dead.contains(id)&&!tagParkourPlayers.contains(id)).count()+playerBots.stream().filter(id->!deadPlayerBots.contains(id)).count();boolean oniViewer=isOni(viewer.getUniqueId());boolean escapeCountdown=secondsLeft<=300;if(oniViewer||escapeCountdown)o.getScore(cc("&e残り時間 &f"+(Math.max(0,secondsLeft)/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60))).setScore(sc--);o.getScore(cc("&a生存者 &f"+alive+"人")).setScore(sc--);if(!oniViewer)o.getScore(cc("&dSP &f"+soulPoints.getOrDefault(viewer.getUniqueId(),0))).setScore(sc--);o.getScore(cc("&8──────────")).setScore(sc--);o.getScore(cc("&bエリア状況")).setScore(sc--);Set<String> found=tagDiscoveredAreas.getOrDefault(viewer.getUniqueId(),new LinkedHashSet<>());String selected=tagSelectedArea.get(viewer.getUniqueId());for(String id:tagAreaIds()){if(sc<=0)break;String mark=found.contains(id)?(id.equals(selected)?"&e➤ ":"&a✓ "):"&7? ";o.getScore(cc(mark+id)).setScore(sc--);}return board;}
    private Scoreboard buildGameSidebarForViewer(Player viewer){
        Scoreboard board=Bukkit.getScoreboardManager().getNewScoreboard();
        Objective o=board.registerNewObjective("onigame","dummy",cc("&4&l-鬼げぇむ-"));o.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score=15;
        o.getScore(cc("&e残り時間 &f"+(Math.max(0,secondsLeft)/60)+":"+String.format("%02d",Math.max(0,secondsLeft)%60))).setScore(score--);
        o.getScore(cc("&c心臓 &f"+brokenHearts+"&7/&f"+totalHearts)).setScore(score--);
        if(players.contains(viewer.getUniqueId()))o.getScore(cc("&d残機 &f"+lifeIcons(viewer.getUniqueId(),dead.contains(viewer.getUniqueId()),escaped.contains(viewer.getUniqueId())))).setScore(score--);
        o.getScore(cc("&8──────────")).setScore(score--);
        List<UUID> shown=new ArrayList<>(players);shown.sort(Comparator.comparing(id->{Player p=Bukkit.getPlayer(id);return p==null?"":p.getName();}));
        for(UUID id:shown){
            if(score<=0)break;
            Player target=Bukkit.getPlayer(id);String name=target!=null?target.getName():"退出者";String icon;
            if(escaped.contains(id))icon="&f◆";
            else if(dead.contains(id)||target==null)icon="&c✖";
            else {double max=Objects.requireNonNull(target.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue();icon=target.getHealth()<=max*.30?"&e▲":"&a●";}
            o.getScore(cc(icon+" &f"+trimName(name))).setScore(score--);
        }
        int botNumber=1;for(UUID id:playerBots){
            if(score<=0)break;
            Entity botEntity=Bukkit.getEntity(id);
            String icon=escapedPlayerBots.contains(id)?"&f◆":deadPlayerBots.contains(id)||botEntity==null?"&c✖":"&a●";
            boolean friendly=id.equals(configuredFriendlyNpcId)||id.equals(azakujiAllyId);
            String shownName;
            if(friendly){
                shownName=playerBotPresetNames.getOrDefault(id,"友軍NPC");
                o.getScore(cc(icon+" &a"+trimName(shownName))).setScore(score--);
            }else{
                o.getScore(cc(icon+" &fBot "+botNumber)).setScore(score--);
            }
            botNumber++;
        }
        if(score>0)o.getScore(cc("&8──────────&0")).setScore(score--);
        if(score>0)o.getScore(cc("&a● 通常  &e▲ 瀕死")).setScore(score--);
        if(score>0)o.getScore(cc("&c✖ 脱落  &f◆ 脱出")).setScore(score);
        return board;
    }
    private String lifeIcons(UUID id,boolean isDead,boolean isEscaped){
        if(isDead)return "&8◇ ◇";
        if(isEscaped)return extraLifeConsumed.contains(id)?"&7◆ ◇":"&f◆ ◆";
        return extraLifeConsumed.contains(id)?"&c◆ &8◇":"&a◆ ◆";
    }
    private String trimName(String name){return name.length()>12?name.substring(0,12):name;}
    private Location chooseBotExit(LivingEntity bot,LivingEntity hunter){if(activeExit==null)return activeExit2;if(activeExit2==null)return activeExit;double a=bot.getWorld().equals(activeExit.getWorld())?bot.getLocation().distanceSquared(activeExit):Double.MAX_VALUE;double b=bot.getWorld().equals(activeExit2.getWorld())?bot.getLocation().distanceSquared(activeExit2):Double.MAX_VALUE;if(hunter!=null&&hunter.getWorld().equals(bot.getWorld())){double danger=Math.max(4.0,getConfig().getDouble("duo-mode.exit-oni-danger-radius",14.0));double d2=danger*danger;if(hunter.getLocation().distanceSquared(activeExit)<=d2)a+=10000;if(hunter.getLocation().distanceSquared(activeExit2)<=d2)b+=10000;}return a<=b?activeExit:activeExit2;}
    private void checkExit(){
        if(!heartGoalReached())return;
        if(System.currentTimeMillis()<exitSealedUntil)return;
        Location exit=activeExit!=null?activeExit:LocationStore.get(getConfig(),"locations.exit");
        if(exit==null)return;
        double radius=Math.max(0.5,getConfig().getDouble("exit-radius",3.0));
        double radiusSq=radius*radius;
        for(UUID id:new HashSet<>(players)){
            if(dead.contains(id)||escaped.contains(id))continue;
            Player p=Bukkit.getPlayer(id);
            if(p!=null&&((p.getWorld().equals(exit.getWorld())&&p.getLocation().distanceSquared(exit)<=radiusSq)||(activeExit2!=null&&p.getWorld().equals(activeExit2.getWorld())&&p.getLocation().distanceSquared(activeExit2)<=radiusSq))){
                escaped.add(id);
                p.setGameMode(GameMode.SPECTATOR);
                all("&b"+p.getName()+" &fが脱出しました！");
                debugTrace("training","BOT-ESC-001","human escape id="+id+" distance="+String.format(java.util.Locale.ROOT,"%.2f",p.getLocation().distance(exit)));
            }
        }
        // Player Bots, including elite preset bots, must use the same exit trigger as humans.
        // Previously only standard bots had a private escape check in updatePlayerBots(), so elite bots could reach the exit and remain there forever.
        for(UUID id:new HashSet<>(playerBots)){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;
            Entity entity=Bukkit.getEntity(id);
            if(!(entity instanceof LivingEntity bot)||!bot.isValid()||bot.isDead())continue;
            boolean atExit1=bot.getWorld().equals(exit.getWorld())&&bot.getLocation().distanceSquared(exit)<=radiusSq;boolean atExit2=activeExit2!=null&&bot.getWorld().equals(activeExit2.getWorld())&&bot.getLocation().distanceSquared(activeExit2)<=radiusSq;if(!atExit1&&!atExit2)continue;
            escapedPlayerBots.add(id);
            restorePlayerBotDownEscape(id);
            clearPlayerBotWork(id);
            playerBotTargetHeart.remove(id);
            stopPlayerBotNavigation(bot);
            String shownName=playerBotPresetNames.getOrDefault(id,"ぷれいやーBot");
            all("&b"+shownName+" &fが脱出しました！");
            debugTrace("training","BOT-ESC-002","bot escape id="+id+" name="+shownName+" distance="+String.format(java.util.Locale.ROOT,"%.2f",bot.getLocation().distance(exit)));
            bot.remove();
        }
        checkPlayerOutcome();
    }
    private void checkPlayerOutcome(){if(state!=GameState.RUNNING)return;long active=players.stream().filter(id->!dead.contains(id)&&!escaped.contains(id)).count()+playerBots.stream().filter(id->!id.equals(azakujiAllyId)&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)).count();if(active==0){if(!escaped.isEmpty()||!escapedPlayerBots.isEmpty())end(false,"&b生 還――ぷれいやーの勝利");else end(true,"&4全 滅――鬼の勝利");}}
    // v0.40.17: ロビー=朝(1000) / ゲーム=夜(18000) / FINAL=正午(6000)へ完全分離。
    private long lobbyMorningTime(){return Math.floorMod(getConfig().getLong("phase-time.lobby",1000L),24000L);}
    private long gameNightTime(){return Math.floorMod(getConfig().getLong("phase-time.game",18000L),24000L);}
    private void applyConfiguredLobbyMorning(){
        Location lobby=LocationStore.get(getConfig(),"locations.lobby");
        if(lobby!=null)restoreLobbyDay(lobby.getWorld());
    }
    // v0.40.18: FINALの時刻適用を一本化。テスト/本番ともplayer time + world timeを同時に固定する。
    private static final long FINAL_SKY_TIME = 6000L;
    private long finalSkyTime(){return FINAL_SKY_TIME;}
    private void applyFinalSkyTime(Player p){
        if(p==null)return;
        long time=finalSkyTime();
        World w=p.getWorld();
        w.setStorm(false);w.setThundering(false);w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);w.setTime(time);
        p.setPlayerTime(time,false);p.setPlayerWeather(WeatherType.CLEAR);
    }
    private void applyFinalSkyTime(World world){
        if(world==null)return;
        long time=finalSkyTime();
        world.setStorm(false);world.setThundering(false);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setTime(time);
        for(Player p:world.getPlayers())if(participants.contains(p.getUniqueId())){p.setPlayerTime(time,false);p.setPlayerWeather(WeatherType.CLEAR);}
    }
    private void enforcePhaseTime(){
        // FINAL演出テストはWAITING/RUNNINGより優先。ロビー朝固定に上書きさせない。
        if(finalEffectTestPlayer!=null){
            Player test=Bukkit.getPlayer(finalEffectTestPlayer);
            if(test!=null){applyFinalSkyTime(test);return;}
        }
        World world=null;
        if(activePlayerSpawn!=null)world=activePlayerSpawn.getWorld();
        if(world==null){Location lobby=LocationStore.get(getConfig(),"locations.lobby");if(lobby!=null)world=lobby.getWorld();}
        if(world==null)return;
        world.setStorm(false);world.setThundering(false);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        if(state==GameState.WAITING)world.setTime(lobbyMorningTime());
        else if(state==GameState.RUNNING){
            // FINAL中はワールド時刻そのものもCustom Sky専用時間へ固定する。
            // OptiFine側が個別player timeではなくworld timeを参照する環境でも赤空を確実に発動させる。
            world.setTime(isTagMode() ? finalSkyTime() : (finalPhase ? finalSkyTime() : gameNightTime()));
            if(finalPhase||isTagMode()){
                long finalTime=finalSkyTime();
                for(Player p:world.getPlayers()){
                    if(finalEffectTestPlayer!=null&&p.getUniqueId().equals(finalEffectTestPlayer))continue;
                    if(participants.contains(p.getUniqueId())){p.setPlayerTime(finalTime,false);p.setPlayerWeather(WeatherType.CLEAR);}
                }
            }
        }
    }
    private void applyGameNight(World world){
        if(world==null)return;
        long time=gameNightTime();
        debugTrace("world","WRD-501","apply night world="+world.getName()+" time="+time+" daylightCycle=false");
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(time);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        for(Player p:world.getPlayers())if(finalEffectTestPlayer==null||!p.getUniqueId().equals(finalEffectTestPlayer)){p.resetPlayerTime();p.setPlayerWeather(WeatherType.CLEAR);}
    }
    private void restoreLobbyDay(World world){
        if(world==null)return;
        long time=lobbyMorningTime();
        debugTrace("world","WRD-502","restore lobby morning world="+world.getName()+" time="+time+" daylightCycle=false");
        world.setStorm(false);
        world.setThundering(false);
        world.setTime(time);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);
        for(Player p:world.getPlayers())if(finalEffectTestPlayer==null||!p.getUniqueId().equals(finalEffectTestPlayer)){p.resetPlayerTime();p.setPlayerWeather(WeatherType.CLEAR);}
    }

    private void end(boolean oniWin,String reason){if(isTagMode())commitSoulRanking();if(state==GameState.WAITING)return;state=GameState.ENDING;if(ticker!=null)ticker.cancel();stopGameStartBgm();stopFinalPhaseBgm();all((oniWin?"&4&l":"&b&l")+reason);Bukkit.getScheduler().runTaskLater(this,()->{Location lobby=activeLobby!=null?activeLobby:LocationStore.get(getConfig(),"locations.lobby");World gameWorld=activePlayerSpawn!=null?activePlayerSpawn.getWorld():(lobby!=null?lobby.getWorld():null);restoreTemporaryTestHearts();resetFinalCrimsonVisuals();restoreLobbyDay(gameWorld);for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p==null)continue;p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);common(p);if(lobby!=null)p.teleport(lobby);}if(testParticipant!=null){participants.remove(testParticipant);selectedSkill.remove(testParticipant);selectedPresetNames.remove(testParticipant);testParticipant=null;}activeLobby=null;activeExit=null;activeExit2=null;activePlayerSpawn=null;restoreLobbyUtilities();setTagShopVisibility(isTagMode());setTerraGemVisibility(isTagMode());state=GameState.WAITING;consumeUneiPlayOverrides();resetRuntime();refreshLobbyGuideDisplays();},100);}

    @EventHandler public void onBreak(BlockBreakEvent e){String key=LocationStore.encode(e.getBlock().getLocation());if((state==GameState.RUNNING&&(heartHp.containsKey(key)||lootChestKeys.contains(key)||tagSkillChestKeys.contains(key)))||(state==GameState.WAITING&&(isPracticeHeartKey(key)||isPracticeChest(e.getBlock()))))e.setCancelled(true);}
    private void toggleHeartRepair(Player player,Block block){String key=LocationStore.encode(block.getLocation());boolean practice=state==GameState.WAITING&&isPracticeHeartKey(key);if(!practice&&(!heartHp.containsKey(key)||!players.contains(player.getUniqueId())||dead.contains(player.getUniqueId())||escaped.contains(player.getUniqueId())))return;if(practice&&!repairingHeart.containsValue(key)){heartHp.put(key,getConfig().getInt("heart-max-health",100));updateHeartCracks(block,getConfig().getInt("heart-max-health",100));}if(key.equals(repairingHeart.get(player.getUniqueId()))){stopRepair(player,"&7心臓への干渉を中断した。 ");return;}stopHealing(player,"&7回復を中断した。");chestOpeningTasks.remove(player.getUniqueId());repairingHeart.put(player.getUniqueId(),key);doubleStakesGreatStreak.remove(player.getUniqueId());heartProgressCarry.remove(player.getUniqueId());long now=System.currentTimeMillis();repairStartedAt.put(player.getUniqueId(),now);repairProgressAt.put(player.getUniqueId(),now);scheduleNextSkillCheck(player.getUniqueId(),now);skillChecks.remove(player.getUniqueId());player.sendActionBar(cc(practice?"&d心臓破壊の練習開始 &7――本番と同じくSHIFT判定あり":"&4心臓への干渉を開始 &7――離れると中断"));player.playSound(player.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,0.7f,0.65f);}
    private boolean isPracticeHeartKey(String key){Location l=LocationStore.get(getConfig(),"locations.practice-heart");return l!=null&&LocationStore.encode(l).equals(key);}
    private boolean isPracticeChest(Block block){return matchesConfiguredChest(block,"locations.practice-loot-chest");}
    private void stopRepair(Player player,String message){UUID id=player.getUniqueId();repairingHeart.remove(id);repairStartedAt.remove(id);repairProgressAt.remove(id);nextSkillCheckAt.remove(id);skillChecks.remove(id);heartProgressCarry.remove(id);doubleStakesGreatStreak.remove(id);if(message!=null)player.sendActionBar(cc(message));}
    private void scheduleNextSkillCheck(UUID id,long now){long min=(long)(getConfig().getDouble("skill-check-min-seconds",4.0)*1000),max=(long)(getConfig().getDouble("skill-check-max-seconds",9.0)*1000);nextSkillCheckAt.put(id,now+min+(long)(Math.random()*Math.max(1,max-min)));}
    private int heartWorkerCount(String heartKey){
        if(heartKey==null||state!=GameState.RUNNING)return 1;
        int count=0;
        for(Map.Entry<UUID,String> e:repairingHeart.entrySet()){
            if(!heartKey.equals(e.getValue())||dead.contains(e.getKey())||escaped.contains(e.getKey()))continue;
            Player p=Bukkit.getPlayer(e.getKey());if(p!=null&&p.isOnline())count++;
        }
        for(Map.Entry<UUID,String> e:playerBotWorkingHeart.entrySet()){
            if(!heartKey.equals(e.getValue())||deadPlayerBots.contains(e.getKey())||escapedPlayerBots.contains(e.getKey()))continue;
            if(Bukkit.getEntity(e.getKey()) instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead())count++;
        }
        return Math.max(1,count);
    }
    private double heartGroupMultiplier(String heartKey){
        int workers=heartWorkerCount(heartKey);
        if(workers<=1)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-1",1.00));
        if(workers==2)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-2",.85));
        if(workers==3)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-3",.70));
        if(workers==4)return Math.max(0,getConfig().getDouble("heart-group-scaling.player-4",.60));
        return Math.max(0,getConfig().getDouble("heart-group-scaling.player-5-plus",.50));
    }
    private int applyScaledHeartDamage(UUID source,String key,double amount){
        if(amount<=0||!heartHp.containsKey(key))return 0;
        double total=amount*heartGroupMultiplier(key)+heartProgressCarry.getOrDefault(source,0.0);
        int whole=(int)Math.floor(total);
        heartProgressCarry.put(source,total-whole);
        if(whole>0){heartHp.computeIfPresent(key,(k,hp)->Math.max(0,hp-whole));if(isTagMode()){double c=tagSpHeartDamageCarry.getOrDefault(source,0.0)+whole;int unit=Math.max(1,getConfig().getInt("tag-sp.heart-damage-per-point",10)),gain=(int)(c/unit);tagSpHeartDamageCarry.put(source,c-gain*unit);if(gain>0)awardSoulPoints(Bukkit.getPlayer(source),gain,"心臓損傷");}}
        return whole;
    }
    private int doubleStakesGreatBonus(Player player){
        UUID id=player.getUniqueId();
        if(!hasPassive(id,PassiveSkill.DOUBLE_STAKES))return Math.max(0,getConfig().getInt("skill-check-great-bonus",5));
        int streak=doubleStakesGreatStreak.getOrDefault(id,0)+1;
        doubleStakesGreatStreak.put(id,streak);
        int step=Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-step",2));
        int cap=Math.max(0,getConfig().getInt("passive-skills.double-stakes-great-bonus-max-extra",10));
        int extra=Math.min(cap,streak*step);
        player.sendActionBar(cc("&6賭け金二倍 &7――連続GREAT &e"+streak+" &7/ 追加損傷 &c+"+extra));
        return Math.max(0,getConfig().getInt("skill-check-great-bonus",5))+extra;
    }
    private void updatePlayerActions(){updateHeartRepairs();updateHealing();updateChestOpening();updateSealingCircles();updateFakeHearts();}
    private void updateFakeHearts(){
        if(state!=GameState.RUNNING||oniType!=OniType.JAKUTSUKI||fakeHeartKeys.isEmpty())return;
        double radius=Math.max(.5,getConfig().getDouble("jakutsuki-hearts.fake-trigger-radius",3.0)),radiusSq=radius*radius;
        for(String key:new HashSet<>(fakeHeartKeys)){
            Location heart=LocationStore.decode(key);if(heart==null)continue;Location center=heart.clone().add(.5,.5,.5);boolean trigger=false;
            for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld().equals(center.getWorld())&&p.getLocation().distanceSquared(center)<=radiusSq){trigger=true;break;}}
            if(!trigger)for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead()&&bot.getWorld().equals(center.getWorld())&&bot.getLocation().distanceSquared(center)<=radiusSq){trigger=true;break;}}
            if(trigger)detonateFakeHeart(key,center);
        }
    }
    private void detonateFakeHeart(String key,Location center){
        if(!fakeHeartKeys.remove(key))return;
        double radius=Math.max(1,getConfig().getDouble("jakutsuki-hearts.fake-explosion-radius",4.5)),radiusSq=radius*radius,damage=Math.max(0,getConfig().getDouble("jakutsuki-hearts.fake-damage",7.0));
        int glowTicks=Math.max(1,getConfig().getInt("jakutsuki-hearts.fake-glow-ticks",160));
        World world=center.getWorld();world.playSound(center,Sound.ENTITY_GENERIC_EXPLODE,1.25f,.72f);world.spawnParticle(Particle.EXPLOSION_HUGE,center,1);world.spawnParticle(Particle.EXPLOSION_LARGE,center,5,.7,.7,.7,.08);world.spawnParticle(Particle.SMOKE_LARGE,center,45,1.2,1.0,1.2,.06);world.spawnParticle(Particle.SOUL,center,30,1.1,1.0,1.1,.05);
        for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world)||p.getLocation().distanceSquared(center)>radiusSq)continue;dealFixedDamage(p,damage);p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,glowTicks,0,false,true));p.sendActionBar(cc("&4偽心臓が爆発した！ &e発光状態になった")); }
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.isValid()||bot.isDead()||!bot.getWorld().equals(world)||bot.getLocation().distanceSquared(center)>radiusSq)continue;dealFixedDamage(bot,damage);bot.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,glowTicks,0,false,true));}
        restoreFakeHeartBlock(key);
    }
    private void restoreFakeHeartBlock(String key){Material original=fakeHeartOriginals.remove(key);Location location=LocationStore.decode(key);if(original!=null&&location!=null)location.getBlock().setType(original);}
    private void restoreFakeHearts(){for(String key:new HashSet<>(fakeHeartOriginals.keySet()))restoreFakeHeartBlock(key);fakeHeartKeys.clear();fakeHeartOriginals.clear();}
    private void updateSealingCircles(){long now=System.currentTimeMillis();LivingEntity hunter=getOniEntity();for(UUID id:new HashSet<>(sealingCircles.keySet())){Location location=sealingCircles.get(id);if(location==null||now>sealingCircleUntil.getOrDefault(id,0L)){sealingCircles.remove(id);sealingCircleUntil.remove(id);continue;}location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,location.clone().add(0,.15,0),6,1.5,.1,1.5,.05);if(hunter!=null&&hunter.getWorld().equals(location.getWorld())&&hunter.getLocation().distanceSquared(location)<=2.25){hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,2,false,true));hunter.getWorld().playSound(location,Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,1,.7f);if(isTagMode())awardSoulPoints(Bukkit.getPlayer(id),Math.max(1,getConfig().getInt("tag-sp.oni-restraint-points",2)),"鬼を拘束");sealingCircles.remove(id);sealingCircleUntil.remove(id);}}}
    private void updateHeartRepairs(){
        if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();
        for(Map.Entry<UUID,String> entry:new HashMap<>(repairingHeart).entrySet()){
            Player player=Bukkit.getPlayer(entry.getKey());Location location=LocationStore.decode(entry.getValue());
            if(player==null||location==null||(!heartHp.containsKey(entry.getValue())&&!isPracticeHeartKey(entry.getValue()))||(state==GameState.RUNNING&&(dead.contains(entry.getKey())||escaped.contains(entry.getKey())))||!player.getWorld().equals(location.getWorld())||player.getLocation().distanceSquared(location.clone().add(.5,.5,.5))>12.25){if(player!=null)stopRepair(player,"&7心臓から離れたため作業を中断した。");continue;}
            SkillCheck check=skillChecks.get(entry.getKey());
            if(check!=null){if(now>check.endsAt()){failSkillCheck(player,location,entry.getValue(),"時間切れ");continue;}showSkillCheck(player,check,now);}
            else if(now>=nextSkillCheckAt.getOrDefault(entry.getKey(),Long.MAX_VALUE)){startSkillCheck(player,now);continue;}
            long interval=getConfig().getLong("heart-progress-interval-ticks",10)*50L;if(now-repairProgressAt.getOrDefault(entry.getKey(),0L)>=interval){repairProgressAt.put(entry.getKey(),now);double amount=Math.max(1,getConfig().getInt("heart-progress-per-interval",1));if(hasPassive(player.getUniqueId(),PassiveSkill.EXORCISM))amount*=Math.max(1.0,getConfig().getDouble("passive-skills.exorcism-heart-progress-multiplier",1.10));if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(player.getUniqueId()+":DESPERATE",0L))amount*=Math.max(1.0,getConfig().getDouble("passive-skills.desperate-heart-progress-multiplier",1.20));if(hasPassive(player.getUniqueId(),PassiveSkill.PRACTICED)){long seconds=(now-repairStartedAt.getOrDefault(entry.getKey(),now))/1000;double step=Math.max(0,getConfig().getDouble("passive-skills.practiced-heart-progress-step",.025)),cap=Math.max(0,getConfig().getDouble("passive-skills.practiced-heart-progress-max",.10));amount*=1+Math.min(cap,Math.floor(seconds/5)*step);}advanceHeart(player,location.getBlock(),amount);}
        }
    }
    private boolean canStartHealing(Player player){double max=player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(player.getHealth()>=max-.01){msg(player,"体力はすでに満タンです。");return false;}return !dead.contains(player.getUniqueId())&&!escaped.contains(player.getUniqueId());}
    private void startHealing(Player player,boolean skill){if(!canStartHealing(player))return;if(healingTasks.containsKey(player.getUniqueId())){stopHealing(player,"&7回復を中断した。");return;}if(skill&&!ready(player,"HEAL",PlayerSkill.HEAL.cooldown))return;if(repairingHeart.containsKey(player.getUniqueId()))stopRepair(player,"&7心臓への干渉を中断した。");chestOpeningTasks.remove(player.getUniqueId());double seconds=getConfig().getDouble(skill?"healing.skill-duration-seconds":"healing.item-duration-seconds",skill?5.0:8.0),amount=getConfig().getDouble(skill?"healing.skill-amount":"healing.item-amount",skill?20.0:10.0);long now=System.currentTimeMillis();healingTasks.put(player.getUniqueId(),new HealingTask(player.getLocation().clone(),now,now+(long)(seconds*1000),amount,skill?null:"item:heal"));player.sendActionBar(cc(skill?"&a治癒を開始 &7――動くか攻撃を受けると中断":"&a包帯を巻き始めた &7――動くか攻撃を受けると中断"));player.playSound(player.getLocation(),Sound.ITEM_ARMOR_EQUIP_LEATHER,0.8f,1.1f);}
    private void updateHealing(){if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();for(Map.Entry<UUID,HealingTask> entry:new HashMap<>(healingTasks).entrySet()){Player player=Bukkit.getPlayer(entry.getKey());HealingTask task=entry.getValue();double tolerance=getConfig().getDouble("healing.move-tolerance",0.45)+(hasPassive(entry.getKey(),PassiveSkill.FOCUS)?0.20:0.0);if(player==null||(state==GameState.WAITING&&!isInTrainingArea(player.getLocation()))||dead.contains(entry.getKey())||escaped.contains(entry.getKey())||!player.getWorld().equals(task.start().getWorld())||player.getLocation().distanceSquared(task.start())>tolerance*tolerance){if(player!=null)stopHealing(player,"&c動いたため回復を中断した。");else healingTasks.remove(entry.getKey());continue;}if(now>=task.endsAt()){double max=player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();player.setHealth(Math.min(max,player.getHealth()+task.amount()));healingTasks.remove(entry.getKey());if(task.consumableAction()!=null)consumeInventoryAction(player,task.consumableAction());else if(hasPassive(entry.getKey(),PassiveSkill.BOND)){int healed=healBondAllies(player,task.amount());if(healed>0)player.sendActionBar(cc("&d絆 &7――周囲の仲間 "+healed+"人も回復した"));}player.sendTitle("",cc("&a&l回復完了 &f+"+(int)task.amount()),0,20,8);player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.7f,1.4f);continue;}double ratio=(now-task.startedAt())/(double)Math.max(1,task.endsAt()-task.startedAt());int filled=Math.max(0,Math.min(16,(int)Math.floor(ratio*16)));StringBuilder bar=new StringBuilder("&a回復 &8[");for(int i=0;i<16;i++)bar.append(i<filled?"&a▰":"&7▱");bar.append("&8] &f").append((int)(ratio*100)).append("%");player.sendActionBar(cc(bar.toString()));}}
    private int healBondAllies(LivingEntity healer,double amount){double radius=Math.max(0,getConfig().getDouble("healing.bond-radius",6.0)),shared=Math.max(0,amount*getConfig().getDouble("healing.bond-shared-heal-multiplier",1.0));int healed=0;for(UUID id:players){if(id.equals(healer.getUniqueId())||dead.contains(id)||escaped.contains(id))continue;Player ally=Bukkit.getPlayer(id);if(ally!=null&&healBondTarget(healer,ally,radius,shared)){ally.sendActionBar(cc("&d絆 &7――仲間の治療で回復した"));healed++;}}for(UUID id:playerBots){if(id.equals(healer.getUniqueId())||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity ally))continue;if(healBondTarget(healer,ally,radius,shared))healed++;}return healed;}
    private boolean hasInjuredBondAlly(LivingEntity healer){double radius=Math.max(0,getConfig().getDouble("healing.bond-radius",6.0));for(UUID id:players){Player ally=Bukkit.getPlayer(id);if(!id.equals(healer.getUniqueId())&&ally!=null&&!dead.contains(id)&&!escaped.contains(id)&&isInjuredBondTarget(healer,ally,radius))return true;}for(UUID id:playerBots){if(!id.equals(healer.getUniqueId())&&!deadPlayerBots.contains(id)&&!escapedPlayerBots.contains(id)&&Bukkit.getEntity(id) instanceof LivingEntity ally&&isInjuredBondTarget(healer,ally,radius))return true;}return false;}
    private boolean isInjuredBondTarget(LivingEntity healer,LivingEntity ally,double radius){return ally.isValid()&&!ally.isDead()&&ally.getWorld().equals(healer.getWorld())&&ally.getLocation().distanceSquared(healer.getLocation())<=radius*radius&&ally.getHealth()<ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()-.01;}
    private boolean healBondTarget(LivingEntity healer,LivingEntity ally,double radius,double amount){if(!ally.isValid()||ally.isDead()||!ally.getWorld().equals(healer.getWorld())||ally.getLocation().distanceSquared(healer.getLocation())>radius*radius)return false;double max=ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(ally.getHealth()>=max-.01)return false;ally.setHealth(Math.min(max,ally.getHealth()+amount));ally.getWorld().spawnParticle(Particle.HEART,ally.getLocation().add(0,1,0),5,.35,.45,.35,.02);ally.getWorld().playSound(ally.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,.55f,1.45f);return true;}
    private void stopHealing(Player player,String message){if(healingTasks.remove(player.getUniqueId())!=null&&message!=null)player.sendActionBar(cc(message));}
    private void consumeInventoryAction(Player player,String wanted){for(ItemStack stack:player.getInventory().getContents()){if(stack==null||!stack.hasItemMeta())continue;String action=stack.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(wanted.equals(action)){consumeOne(stack);return;}}}
    private void consumeOne(ItemStack stack){if(stack.getAmount()<=1)stack.setAmount(0);else stack.setAmount(stack.getAmount()-1);}
    private void startChestOpening(Player player,String chestKey){
        UUID id=player.getUniqueId();
        if(state!=GameState.RUNNING){msg(player,"物資箱は試合中のみ使用できます。");return;}
        if(oni!=null&&isOni(id)){msg(player,"&c鬼は物資箱を開けられません。");player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,0.7f);return;}
        // v0.24.2: /og join 済みの非鬼参加者も安全側でぷれいやー陣営として扱う。
        // 通常は start() で players に登録されるが、役割同期がずれた場合でも物資箱だけ無反応にならないようにする。
        if(!players.contains(id)){
            if(participants.contains(id)){players.add(id);getLogger().warning("Recovered missing player-side role for "+player.getName()+" while opening loot chest.");}
            else{msg(player,"&cこの試合の参加者ではありません。ロビーで /og join をしてから次の試合に参加してください。");player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,0.7f);return;}
        }
        if(openedLootChests.getOrDefault(id,Set.of()).contains(chestKey)){openPersonalLoot(player,chestKey);return;}
        ChestOpeningTask current=chestOpeningTasks.get(id);if(current!=null&&current.chestKey().equals(chestKey)){chestOpeningTasks.remove(id);player.sendActionBar(cc("&7チェストの開錠を中断した。"));return;}
        if(repairingHeart.containsKey(id))stopRepair(player,"&7心臓への干渉を中断した。");stopHealing(player,"&7回復を中断した。");
        long now=System.currentTimeMillis(),duration=(long)(getConfig().getDouble("loot-chests.open-seconds",5.0)*1000);
        chestOpeningTasks.put(id,new ChestOpeningTask(chestKey,player.getLocation().clone(),now,now+duration));
        player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,0.8f,1.1f);
        player.sendTitle("",cc("&6&l物資箱を開錠中"),0,12,4);
        player.sendActionBar(cc("&6チェストを開錠中 &7――約 "+String.format("%.1f",duration/1000.0)+"秒静止 / 動くか攻撃を受けると中断"));
    }
    private void startPracticeChestOpening(Player player,String chestKey){if(state!=GameState.WAITING){msg(player,"開錠練習はロビーでのみ使用できます。");return;}ChestOpeningTask current=chestOpeningTasks.get(player.getUniqueId());if(current!=null&&current.chestKey().equals(chestKey)){chestOpeningTasks.remove(player.getUniqueId());player.sendActionBar(cc("&7開錠練習を中断した。"));return;}long now=System.currentTimeMillis(),duration=(long)(getConfig().getDouble("loot-chests.open-seconds",5.0)*1000);chestOpeningTasks.put(player.getUniqueId(),new ChestOpeningTask(chestKey,player.getLocation().clone(),now,now+duration));player.playSound(player.getLocation(),Sound.BLOCK_CHEST_LOCKED,.8f,1.1f);player.sendTitle("",cc("&e&l開錠練習"),0,12,4);player.sendActionBar(cc("&6開錠練習中 &7――動かずに待機"));}
    private boolean isPracticeChestKey(String key){Location l=LocationStore.get(getConfig(),"locations.practice-loot-chest");return l!=null&&LocationStore.encode(l).equals(key);}
    private void updateChestOpening(){if(state!=GameState.RUNNING&&state!=GameState.WAITING)return;long now=System.currentTimeMillis();for(Map.Entry<UUID,ChestOpeningTask> entry:new HashMap<>(chestOpeningTasks).entrySet()){Player player=Bukkit.getPlayer(entry.getKey());ChestOpeningTask task=entry.getValue();Location chest=LocationStore.decode(task.chestKey());double tolerance=getConfig().getDouble("loot-chests.move-tolerance",0.55);boolean practice=isPracticeChestKey(task.chestKey());if(player==null||chest==null||(!lootChestKeys.contains(task.chestKey())&&!practice)||!player.getWorld().equals(task.start().getWorld())||player.getLocation().distanceSquared(task.start())>tolerance*tolerance||player.getLocation().distanceSquared(chest.clone().add(.5,.5,.5))>16){chestOpeningTasks.remove(entry.getKey());if(player!=null)player.sendActionBar(cc("&c離れたためチェストの開錠を中断した。"));continue;}if(now>=task.endsAt()){chestOpeningTasks.remove(entry.getKey());player.playSound(player.getLocation(),Sound.BLOCK_CHEST_OPEN,1,1);player.sendTitle("",cc(practice?"&a&l開錠練習 成功":"&6&l開錠成功"),0,15,5);if(practice){player.sendActionBar(cc("&7もう一度右クリックすると再挑戦できます。"));}else{openedLootChests.computeIfAbsent(entry.getKey(),id->new HashSet<>()).add(task.chestKey());openPersonalLoot(player,task.chestKey());}continue;}double ratio=(now-task.startedAt())/(double)Math.max(1,task.endsAt()-task.startedAt());int filled=Math.max(0,Math.min(16,(int)Math.floor(ratio*16)));StringBuilder bar=new StringBuilder("&6開錠 &8[");for(int i=0;i<16;i++)bar.append(i<filled?"&6▰":"&7▱");bar.append("&8] &f").append((int)(ratio*100)).append("%");player.sendActionBar(cc(bar.toString()));}}
    private void openPersonalLoot(Player player,String chestKey){String inventoryKey=player.getUniqueId()+"|"+chestKey;Inventory inventory=personalLootInventories.computeIfAbsent(inventoryKey,key->createPersonalLoot());player.openInventory(inventory);}
    private Inventory createPersonalLoot(){
        Inventory inventory=Bukkit.createInventory(null,18,cc("&4鬼げぇむ &8- &6物資箱"));
        List<String> items=new ArrayList<>(List.of("item:sprint","item:invisible","item:smoke","item:strike","item:heal"));
        Collections.shuffle(items);
        int min=Math.max(1,getConfig().getInt("loot-chests.min-items",1)),max=Math.max(min,getConfig().getInt("loot-chests.max-items",3)),count=Math.min(items.size(),min+new Random().nextInt(max-min+1));
        List<Integer> itemSlots=new ArrayList<>();for(int i=0;i<9;i++)itemSlots.add(i);Collections.shuffle(itemSlots);
        for(int i=0;i<count;i++)inventory.setItem(itemSlots.get(i),lootItem(items.get(i)));
        List<String> equipment=new ArrayList<>(List.of("equipment:leather_helmet","equipment:leather_chestplate","equipment:leather_leggings","equipment:leather_boots","equipment:shinai","equipment:iron_bat","equipment:flare_gun"));
        Collections.shuffle(equipment);
        int equipmentMin=Math.max(0,getConfig().getInt("loot-chests.min-equipment",1)),equipmentMax=Math.max(equipmentMin,getConfig().getInt("loot-chests.max-equipment",1));
        int equipmentCount=Math.min(equipment.size(),equipmentMin+new Random().nextInt(equipmentMax-equipmentMin+1));
        List<Integer> equipmentSlots=new ArrayList<>();for(int i=9;i<18;i++)equipmentSlots.add(i);Collections.shuffle(equipmentSlots);
        for(int i=0;i<equipmentCount;i++)inventory.setItem(equipmentSlots.get(i),lootEquipment(equipment.get(i)));
        return inventory;
    }
    private ItemStack lootItem(String action){return switch(action){case "item:sprint"->consumable(Material.SUGAR,"&b軽量薬",action,"&7短時間だけ移動速度が上がる");case "item:invisible"->consumable(Material.POTION,"&f薄明薬",action,"&7短時間だけ透明になる");case "item:smoke"->consumable(Material.GRAY_DYE,"&8小型煙玉",action,"&7狭い範囲へ煙幕を張る");case "item:strike"->consumable(Material.BLAZE_POWDER,"&c破鬼符",action,"&7鬼を弱く怯ませる");default->consumable(Material.PAPER,"&a包帯","item:heal","&7時間をかけて体力10（5♥）回復する");};}
    private ItemStack lootEquipment(String action){ItemStack out=switch(action){
        case "equipment:leather_helmet"->equipment(Material.LEATHER_HELMET,"&6革の頭防具",action,"&7軽量な防具。頭に装備できる");
        case "equipment:leather_chestplate"->equipment(Material.LEATHER_CHESTPLATE,"&6革の胴防具",action,"&7軽量な防具。胴に装備できる");
        case "equipment:leather_leggings"->equipment(Material.LEATHER_LEGGINGS,"&6革の脚防具",action,"&7軽量な防具。脚に装備できる");
        case "equipment:leather_boots"->equipment(Material.LEATHER_BOOTS,"&6革の足防具",action,"&7軽量な防具。足に装備できる");
        case "equipment:shinai"->equipment(Material.WOODEN_SWORD,"&e竹刀",action,"&7軽く扱いやすい近接武器","&7鬼への基礎ダメージ: &f"+getConfig().getDouble("equipment.shinai-damage",3.0));
        case "equipment:iron_bat"->equipment(Material.IRON_SWORD,"&7鉄バット",action,"&7重い近接武器","&7鬼への基礎ダメージ: &f"+getConfig().getDouble("equipment.iron-bat-damage",5.0));
        default->equipment(Material.BLAZE_ROD,"&cフレアガン","equipment:flare_gun","&7右クリックで照明弾を発射","&7着弾地点付近の鬼を一時的に発光させる");
    };int uses=switch(action){case "equipment:shinai"->getConfig().getInt("equipment.shinai-uses",10);case "equipment:iron_bat"->getConfig().getInt("equipment.iron-bat-uses",6);case "equipment:flare_gun"->getConfig().getInt("equipment.flare-gun-uses",2);default->0;};if(uses>0)setEquipmentUses(out,uses,uses);return out;}
    private void setEquipmentUses(ItemStack stack,int remaining,int max){if(stack==null||!stack.hasItemMeta())return;ItemMeta m=stack.getItemMeta();m.getPersistentDataContainer().set(equipmentUsesKey,PersistentDataType.INTEGER,Math.max(0,remaining));List<String> lore=new ArrayList<>(m.getLore()==null?List.of():m.getLore());lore.removeIf(line->ChatColor.stripColor(line).startsWith("残り使用回数:"));lore.add(cc((remaining<=1?"&c":"&e")+"残り使用回数: &f"+remaining+" / "+max));m.setLore(lore);stack.setItemMeta(m);}
    private boolean consumeEquipmentUse(Player p,ItemStack stack,String action){if(stack==null||!stack.hasItemMeta())return false;Integer left=stack.getItemMeta().getPersistentDataContainer().get(equipmentUsesKey,PersistentDataType.INTEGER);if(left==null)return false;int max="equipment:shinai".equals(action)?getConfig().getInt("equipment.shinai-uses",10):"equipment:iron_bat".equals(action)?getConfig().getInt("equipment.iron-bat-uses",6):getConfig().getInt("equipment.flare-gun-uses",2);left--;if(left<=0){p.getInventory().setItemInMainHand(null);p.sendActionBar(cc("&c"+("equipment:flare_gun".equals(action)?"フレアガン":"武器")+"を使い切った。"));}else{setEquipmentUses(stack,left,max);p.sendActionBar(cc("&e残り使用回数 &f"+left+" / "+max));}return true;}
    private void startSkillCheck(Player player,long now){boolean aftermind=aftermindPrimed.remove(player.getUniqueId());long duration=(long)(getConfig().getDouble("skill-check-duration-seconds",2.4)*1000)+(aftermind?800:0);double targetRatio=.45+Math.random()*.30;long target=now+(long)(duration*targetRatio);skillChecks.put(player.getUniqueId(),new SkillCheck(now,target,now+duration));player.playSound(player.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,0.8f,1.4f);player.sendTitle("",cc(aftermind?"&b残心 &7――判定位置 "+(int)(targetRatio*100)+"%":"&e&lスキルチェック！ &fSHIFT"),0,15,0);}
    private void showSkillCheck(Player player,SkillCheck check,long now){
        int length=15,cursor=Math.min(length-1,(int)(((now-check.startedAt())/(double)(check.endsAt()-check.startedAt()))*length)),great=Math.min(length-1,(int)(((check.targetAt()-check.startedAt())/(double)(check.endsAt()-check.startedAt()))*length));
        boolean hard=hasPassive(player.getUniqueId(),PassiveSkill.DOUBLE_STAKES);
        StringBuilder bar=new StringBuilder("&8[");
        for(int i=0;i<length;i++){
            if(i==cursor)bar.append(i==great?"&6◆":"&f◆");
            else if(i==great)bar.append("&6▰");
            else if(i==great-1||i==great-2)bar.append(hard?"&c▰":"&a▰");
            else bar.append("&7▱");
        }
        bar.append("&8] &eSHIFT");if(hard)bar.append(" &4GREATのみ");player.sendActionBar(cc(bar.toString()));
    }
    private void resolveSkillCheck(Player player){
        SkillCheck check=skillChecks.get(player.getUniqueId());if(check==null)return;long now=System.currentTimeMillis();
        double focus=hasPassive(player.getUniqueId(),PassiveSkill.FOCUS)?0.12:0.0,interference=hasOniPassive(OniPassiveSkill.INTERFERENCE)?Math.max(0.1,getConfig().getDouble("oni-passive-skills.interference-window-multiplier",0.70)):1.0;
        long successWindow=(long)((getConfig().getDouble("skill-check-success-window-seconds",0.40)+focus)*interference*1000),greatWindow=(long)((getConfig().getDouble("skill-check-great-window-seconds",0.12)+(focus*.25))*interference*1000);
        long greatHalf=Math.max(20,greatWindow/2),greatStart=check.targetAt()-greatHalf,greatEnd=check.targetAt()+greatHalf;
        long normalStart=greatStart-(successWindow*2);
        Location heart=LocationStore.decode(repairingHeart.get(player.getUniqueId()));if(heart==null)return;
        if(now>=greatStart&&now<=greatEnd){skillChecks.remove(player.getUniqueId());scheduleNextSkillCheck(player.getUniqueId(),now);if(hasPassive(player.getUniqueId(),PassiveSkill.AFTERMIND))aftermindPrimed.add(player.getUniqueId());advanceHeart(player,heart.getBlock(),doubleStakesGreatBonus(player));player.sendTitle("",cc("&6&lGREAT!"),0,18,6);player.playSound(player.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,0.8f,1.65f);}
        else if(now>=normalStart&&now<greatStart){if(hasPassive(player.getUniqueId(),PassiveSkill.DOUBLE_STAKES)){failSkillCheck(player,heart,LocationStore.encode(heart),"GREAT以外は失敗");}else{skillChecks.remove(player.getUniqueId());scheduleNextSkillCheck(player.getUniqueId(),now);if(hasOniPassive(OniPassiveSkill.INTERFERENCE)){regressHeart(heart.getBlock(),Math.max(0,getConfig().getInt("oni-passive-skills.interference-normal-regression",2)));player.sendTitle("",cc("&5&l妨害 &c進行後退"),0,18,6);player.playSound(player.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE,0.8f,0.75f);}else{advanceHeart(player,heart.getBlock(),Math.max(0,getConfig().getInt("skill-check-success-bonus",3)));player.sendTitle("",cc("&a&l成功"),0,15,5);player.playSound(player.getLocation(),Sound.ENTITY_EXPERIENCE_ORB_PICKUP,0.8f,1.5f);}}}
        else failSkillCheck(player,heart,LocationStore.encode(heart),"タイミング失敗");
    }
    private void failSkillCheck(Player player,Location heart,String heartKey,String reason){skillChecks.remove(player.getUniqueId());doubleStakesGreatStreak.remove(player.getUniqueId());long now=System.currentTimeMillis();if(hasPassive(player.getUniqueId(),PassiveSkill.PRACTICED))repairStartedAt.put(player.getUniqueId(),now);scheduleNextSkillCheck(player.getUniqueId(),now);int max=getConfig().getInt("heart-max-health",100),penalty=Math.max(0,getConfig().getInt("skill-check-failure-penalty",8));heartHp.computeIfPresent(heartKey,(k,hp)->Math.min(max,hp+penalty));updateHeartCracks(heart.getBlock(),heartHp.getOrDefault(heartKey,max));player.sendTitle(cc("&4&l失敗"),cc("&c"+reason),0,25,8);heart.getWorld().playSound(heart,Sound.ENTITY_GENERIC_EXPLODE,1.2f,0.7f);heart.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,heart.clone().add(.5,.5,.5),2);if(state==GameState.RUNNING)forceHeartAlert(heart);}
    private void regressHeart(Block block,int amount){String key=LocationStore.encode(block.getLocation());int max=getConfig().getInt("heart-max-health",100);heartHp.computeIfPresent(key,(k,hp)->Math.min(max,hp+amount));updateHeartCracks(block,heartHp.getOrDefault(key,max));block.getWorld().spawnParticle(Particle.SMOKE_NORMAL,block.getLocation().add(.5,.7,.5),10,.3,.3,.3,.02);}
    private void advanceHeart(Player player,Block block,double amount){String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;applyScaledHeartDamage(player.getUniqueId(),key,amount);int hp=heartHp.getOrDefault(key,0);updateHeartCracks(block,hp);block.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,block.getLocation().add(.5,.6,.5),2,0.25,0.25,0.25,0.02);int max=getConfig().getInt("heart-max-health",100),percent=(int)Math.round((1-hp/(double)Math.max(1,max))*100);boolean practice=isPracticeHeartKey(key)&&state==GameState.WAITING;if(!skillChecks.containsKey(player.getUniqueId()))player.sendActionBar(cc((practice?"&d練習心臓 損傷 ":"&4心臓損傷 &c")+percent+"% &7――作業中"));if(!practice)notifyHeartAttack(block.getLocation(),key,System.currentTimeMillis());if(hp<=0){if(practice)completePracticeHeart(player,block,key);else destroyHeart(block);}}
    private void completePracticeHeart(Player player,Block block,String key){for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player p=Bukkit.getPlayer(id);if(p!=null)stopRepair(p,null);}for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),0f);player.sendTitle(cc("&d&l心臓破壊 成功"),cc("&7練習完了"),0,30,10);block.getWorld().playSound(block.getLocation(),Sound.ENTITY_WITHER_DEATH,.55f,.8f);heartHp.put(key,getConfig().getInt("heart-max-health",100));Bukkit.getScheduler().runTaskLater(this,()->{Material m=Material.matchMaterial(getConfig().getString("heart-material","CRYING_OBSIDIAN"));if(m==null)m=Material.CRYING_OBSIDIAN;block.setType(m);updateHeartCracks(block,getConfig().getInt("heart-max-health",100));},20L);}
    private void applyExpandedHeartBreakPassives(Location heart){
        LivingEntity hunter=getOniEntity();double radius=Math.max(1,getConfig().getDouble("oni-passive-skills.heart-break-effect-radius",8.0));
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||!q.getWorld().equals(heart.getWorld())||q.getLocation().distanceSquared(heart)>radius*radius)continue;if(hasOniPassive(OniPassiveSkill.CURSED_VEIN)){q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,Math.max(20,getConfig().getInt("oni-passive-skills.cursed-vein-ticks",80)),0,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,40,0,false,true));}if(hasOniPassive(OniPassiveSkill.BACKFLOW)){Vector v=q.getLocation().toVector().subtract(heart.toVector()).setY(.25);if(v.lengthSquared()<.01)v=new Vector(0,.3,0);else v.normalize().multiply(1.0).setY(.35);q.setVelocity(v);}}
        if(hunter!=null&&hasOniPassive(OniPassiveSkill.HOMING)&&hunter.getWorld().equals(heart.getWorld())&&hunter.getLocation().distanceSquared(heart)>400)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.homing-speed-ticks",80)),1,false,true));
    }
    private void giveOniDomainItems(){
        if(oniAwakeningStage<2)return;
        for(UUID id:new HashSet<>(oniTeam)){Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline())continue;p.getInventory().setItem(8,item(Material.CRYING_OBSIDIAN,"&4&l鬼域展開","oni_domain"));if(oniType==OniType.DAKKO)p.getInventory().setItem(7,item(Material.AMETHYST_SHARD,"&d&l分霊 / 狐換え &7[SHIFT+右:配置]","dakko_domain_spirit"));}
    }
    private boolean isInsideOniDomain(Location l){if(oniDomainCenter==null||oniDomainUntil<System.currentTimeMillis()||l==null||l.getWorld()==null||!l.getWorld().equals(oniDomainCenter.getWorld()))return false;double r=getConfig().getDouble("oni-domain.radius",28.0);return l.distanceSquared(oniDomainCenter)<=r*r;}
    private void useOniDomain(Player p){
        if(oniAwakeningStage<2){msg(p,"&c鬼域は第二覚醒（必要心臓の50%破壊）で解放されます。");return;}long now=System.currentTimeMillis();if(now<oniDomainReadyAt){msg(p,"&c鬼域は再展開まであと "+Math.max(1,(oniDomainReadyAt-now+999)/1000)+"秒です。");return;}
        clearOniDomain();oniDomainCenter=p.getLocation().clone();oniDomainOwner=p.getUniqueId();oniDomainType=oniType;int sec=Math.max(10,getConfig().getInt("oni-domain.duration-seconds",60));oniDomainUntil=now+sec*1000L;oniDomainReadyAt=now+Math.max(sec,getConfig().getInt("oni-domain.cooldown-seconds",90))*1000L;
        String name=oniDomainType==OniType.SHIKKI?"狩場":oniDomainType==OniType.DAKKO?"狐境":"鬼域";for(Player q:Bukkit.getOnlinePlayers())q.sendTitle(cc("&4&l――鬼域展開――"),cc("&c「"+name+"」"),5,35,10);all("&4&l【鬼域】 &c"+oniType.display+" &7が &f「"+name+"」 &7を展開した。 &8["+sec+"秒]");p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,1f,.8f);renderOniDomainBoundary();if(oniDomainType==OniType.DAKKO)spawnDakkoFoxLamps();
    }
    private void clearOniDomain(){clearDakkoFoxLamps();for(UUID id:new HashSet<>(dakkoDomainSpirits)){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}dakkoDomainSpirits.clear();for(UUID id:new HashSet<>(shikkiDomainAirJumps.keySet())){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR)p.setAllowFlight(false);}shikkiDomainAirJumps.clear();shikkiDomainWallKicks.clear();oniDomainCenter=null;oniDomainType=null;oniDomainOwner=null;oniDomainUntil=0L;}
    private void renderOniDomainBoundary(){if(oniDomainCenter==null)return;double r=getConfig().getDouble("oni-domain.radius",28.0);for(int i=0;i<48;i++){double a=Math.PI*2*i/48.0;Location l=oniDomainCenter.clone().add(Math.cos(a)*r,.15,Math.sin(a)*r);l.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,l,1,0,0,0,0);}}
    private boolean isShikkiDomainUser(Player p){return state==GameState.RUNNING&&oniDomainType==OniType.SHIKKI&&isOni(p.getUniqueId())&&oniType==OniType.SHIKKI&&oniDomainUntil>=System.currentTimeMillis();}
    private void updateOniDomainMovement(Player p){
        if(oniDomainUntil>0&&oniDomainUntil<System.currentTimeMillis()){clearOniDomain();return;}
        if(!isShikkiDomainUser(p))return;UUID id=p.getUniqueId();boolean inside=isInsideOniDomain(p.getLocation());if(!inside){if(p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR)p.setAllowFlight(false);return;}
        if(p.isOnGround()){shikkiDomainAirJumps.put(id,0);shikkiDomainWallKicks.put(id,0);if(p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR)p.setAllowFlight(true);}else if(p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR&&!p.getAllowFlight())p.setAllowFlight(true);
    }
    private void useDakkoDomainSpirit(Player p){
        if(oniDomainType!=OniType.DAKKO||oniType!=OniType.DAKKO||!isInsideOniDomain(p.getLocation())){msg(p,"&d分霊/狐換え &7は狐境の中でのみ使用できます。");return;}
        if(p.isSneaking()){int max=Math.max(1,getConfig().getInt("oni-domain.dakko.max-spirits",3));dakkoDomainSpirits.removeIf(id->{Entity e=Bukkit.getEntity(id);return e==null||!e.isValid();});if(dakkoDomainSpirits.size()>=max){msg(p,"&d狐境 &7――分霊は最大"+max+"体です。");return;}Zombie z=p.getWorld().spawn(p.getLocation(),Zombie.class);z.setBaby(false);z.setAI(false);z.setSilent(true);z.setPersistent(true);z.setRemoveWhenFarAway(false);z.setCustomName(cc("&5堕狐の分霊"));z.setCustomNameVisible(false);z.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));z.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);z.setHealth(20);z.getEquipment().setHelmet(oniArmor(Material.CHAINMAIL_HELMET,"&5堕狐の面"));z.getEquipment().setChestplate(oniArmor(Material.CHAINMAIL_CHESTPLATE,"&5堕狐の装束"));z.getEquipment().setLeggings(oniArmor(Material.CHAINMAIL_LEGGINGS,"&5堕狐の袴"));z.getEquipment().setBoots(oniArmor(Material.CHAINMAIL_BOOTS,"&5堕狐の足袋"));dakkoDomainSpirits.add(z.getUniqueId());p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,z.getLocation().add(0,1,0),28,.4,.7,.4,.04);p.sendActionBar(cc("&d狐境 &7――分霊を配置 &f"+dakkoDomainSpirits.size()+"/&f"+max));return;}
        LivingEntity best=null;double bestScore=-999;Vector look=p.getEyeLocation().getDirection().normalize();for(UUID id:dakkoDomainSpirits){if(!(Bukkit.getEntity(id) instanceof LivingEntity z)||!z.isValid()||!z.getWorld().equals(p.getWorld()))continue;Vector to=z.getLocation().toVector().subtract(p.getLocation().toVector());double dist=to.length();if(dist<.5||dist>getConfig().getDouble("oni-domain.dakko.swap-range",32))continue;double score=look.dot(to.normalize())-dist*.003;if(score>bestScore){bestScore=score;best=z;}}if(best==null||bestScore<.35){msg(p,"&d狐境 &7――視線方向に狐換えできる分霊がありません。");return;}Location a=p.getLocation().clone(),b=best.getLocation().clone();p.teleport(b.setDirection(a.getDirection()));best.teleport(a);p.setFallDistance(0);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.8f,1.45f);p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,p.getLocation().add(0,1,0),35,.5,.8,.5,.05);p.sendActionBar(cc("&d狐換え &7――分霊と位置を入れ替えた"));
    }

    private int calculateOniAwakeningStage(){int goal=Math.max(1,heartGoal());double ratio=brokenHearts/(double)goal;if(ratio>=1.0)return 4;if(ratio>=.75)return 3;if(ratio>=.50)return 2;if(ratio>=.25)return 1;return 0;}
    private void updateOniAwakening(){int next=calculateOniAwakeningStage();if(next<=oniAwakeningStage)return;oniAwakeningStage=next;String name=switch(next){case 1->"第一覚醒";case 2->"第二覚醒";case 3->"第三覚醒";default->"最終覚醒";};all("&4&l【鬼覚醒】 &c"+name+" &7――心臓の封印が崩れ、鬼の力が解放される。");for(Player q:Bukkit.getOnlinePlayers())q.sendTitle(cc("&4&l鬼覚醒"),cc("&c"+name),5,35,10);if(next>=2)giveOniDomainItems();}
    private void destroyHeart(Block block){String key=LocationStore.encode(block.getLocation());if(!heartHp.containsKey(key))return;UUID glowId=oniHeartGlowMarkers.remove(key);if(glowId!=null){Entity glow=Bukkit.getEntity(glowId);if(glow!=null)glow.remove();}for(UUID id:new HashSet<>(repairingHeart.keySet()))if(key.equals(repairingHeart.get(id))){Player p=Bukkit.getPlayer(id);if(p!=null)stopRepair(p,null);}heartHp.remove(key);for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),0f);block.setType(Material.AIR);brokenHearts++;updateOniAwakening();applyExpandedHeartBreakPassives(block.getLocation());for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q!=null)trackHeart(q);}all("&c鬼の心臓が破壊された！ &f("+brokenHearts+"/"+heartGoal()+") &7[配置残り "+heartHp.size()+"]");block.getWorld().playSound(block.getLocation(),Sound.ENTITY_WITHER_DEATH,0.7f,0.55f);weakenOni();int remainingAfterBreak=Math.max(0,heartGoal()-brokenHearts);if(remainingAfterBreak==4){all("&c鬼の守りが大きく崩れた！ &f――討伐の好機が生まれた。");spawnMidgameSupportDrop();}if(heartGoalReached()){
            finalPhase=true;
            secondsLeft=Math.max(1,getConfig().getInt("final-crimson.remaining-seconds",180));
            all("&4&l鬼の不死性が消滅した――最終局面！");
            all("&c鬼を討伐するか、&b脱出するか。 &f残された時間は &e3分&f。");
            startFinalCrimson(block.getWorld());
            if(oniType==OniType.JAKUTSUKI)grantJakutsukiFinalWeapons();
            startFinalPhaseBgm();
        }}
    private void startFinalCrimson(World world){
        if(world==null)return;
        applyFinalSkyTime(world);
        world.setStorm(false);
        world.setThundering(false);
        world.setWeatherDuration(Integer.MAX_VALUE);
        world.setThunderDuration(0);
        // Custom Skyの判定元がworld timeのクライアントでも確実にFINAL時間になるよう同期。
        world.setTime(finalSkyTime());
        if(crimsonTicker!=null){crimsonTicker.cancel();crimsonTicker=null;}
        if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}
        boolean enabled=getConfig().getBoolean("final-crimson.enabled",true);
        long playerTime=finalSkyTime();
        for(UUID id:participants){
            Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world))continue;
            if(enabled)p.setPlayerTime(playerTime,false);
            if(enabled)p.setPlayerWeather(WeatherType.CLEAR);
            p.sendTitle(cc("&4&l最 終 局 面"),cc("&c鬼を討伐するか、&b脱出するか &7―― &e残り3:00"),10,70,20);
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,.8f,.65f);
        }
        world.playSound(world.getSpawnLocation(),Sound.ENTITY_WITHER_SPAWN,.65f,.55f);
        if(enabled&&getConfig().getBoolean("final-crimson.blood-sky.enabled",true)){
            long delay=Math.max(0,getConfig().getLong("final-crimson.blood-sky.delay-ticks",100));
            Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)showBloodSkyOverlay(world);},delay);
        }
        if(!enabled||!getConfig().getBoolean("final-crimson.red-haze-particles",true))return;
        int count=Math.max(1,getConfig().getInt("final-crimson.red-haze-count",18));
        double height=Math.max(4.0,getConfig().getDouble("final-crimson.red-haze-height",12.0));
        Particle.DustOptions dust=new Particle.DustOptions(Color.fromRGB(180,0,0),2.0f);
        crimsonTicker=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(state!=GameState.RUNNING||!finalPhase){if(crimsonTicker!=null)crimsonTicker.cancel();crimsonTicker=null;return;}
            for(UUID id:participants){
                Player p=Bukkit.getPlayer(id);if(p==null||!p.getWorld().equals(world)||p.getGameMode()==GameMode.SPECTATOR)continue;
                Location base=p.getLocation().clone().add(0,height,0);
                p.spawnParticle(Particle.REDSTONE,base,count,14.0,4.0,14.0,0,dust);
                p.spawnParticle(Particle.ASH,base.clone().add(0,-3,0),Math.max(4,count/2),10.0,5.0,10.0,.01);
            }
        },1L,10L);
    }
    private void startFinalSkyTest(Player p){
        clearFinalEffectTest();
        finalEffectTestPlayer=p.getUniqueId();
        applyFinalSkyTime(p);
        p.sendTitle(cc("&4&lFINAL SKY TEST"),cc("&7OptiFine Custom Sky 単体確認"),5,35,10);
        msg(p,"&4Custom Sky単体テストを開始しました。 &f空だけを確認してください。 &7解除: /og finalsky clear");
        msg(p,"&7OptiFine > Video Settings > Quality > Custom Sky がONであることを確認してください。");
    }
    private void clearFinalSkyTest(){clearFinalEffectTest(); for(Player q:Bukkit.getOnlinePlayers()){ if(state==GameState.WAITING){q.setPlayerTime(lobbyMorningTime(),false);q.setPlayerWeather(WeatherType.CLEAR);} else if(state==GameState.RUNNING&&!finalPhase){q.setPlayerTime(gameNightTime(),false);q.setPlayerWeather(WeatherType.CLEAR);} }}

    private void startFinalEffectTest(Player p){
        clearFinalEffectTest();finalEffectTestPlayer=p.getUniqueId();
        applyFinalSkyTime(p);
        Bukkit.getScheduler().runTaskLater(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q!=null){applyFinalSkyTime(q);msg(q,"&7[FinalSky] world=&e"+q.getWorld().getTime()+" &7player=&e"+q.getPlayerTime()+" &7target=&a"+FINAL_SKY_TIME);}},2L);
        p.sendTitle(cc("&4&l最 終 局 面"),cc("&c演出テスト &7―― &f/og finaltest clear で解除"),5,50,10);p.playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,.8f,.65f);
        String fontRaw=getConfig().getString("final-crimson.blood-sky.font","onigame:final_sky"),glyph=getConfig().getString("final-crimson.blood-sky.glyph","\uE101");if(glyph==null||glyph.isBlank())glyph="\uE101";
        Key font;try{font=Key.key(fontRaw==null?"onigame:final_sky":fontRaw);}catch(IllegalArgumentException ex){font=Key.key("onigame","final_sky");}
        Title overlay=Title.title(Component.text(glyph).font(font),Component.empty(),Title.Times.times(Duration.ofMillis(200),Duration.ofMillis(600000),Duration.ofMillis(200)));
        if(getConfig().getBoolean("final-crimson.blood-sky.enabled",false))Bukkit.getScheduler().runTaskLater(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q!=null)q.showTitle(overlay);},65L);
        p.stopSound("onigame:final_phase",SoundCategory.RECORDS);p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);
        Bukkit.getScheduler().runTaskLater(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q!=null)q.playSound(q.getLocation(),"onigame:chase_tatari",SoundCategory.RECORDS,(float)getConfig().getDouble("final-phase-bgm.volume",.22),(float)getConfig().getDouble("final-phase-bgm.pitch",1.0));},4L);
        finalEffectTestTicker=Bukkit.getScheduler().runTaskTimer(this,()->{Player q=finalEffectTestPlayer==null?null:Bukkit.getPlayer(finalEffectTestPlayer);if(q==null){clearFinalEffectTest();return;}applyFinalSkyTime(q);int count=Math.max(1,getConfig().getInt("final-crimson.red-haze-count",42));double h=Math.max(4,getConfig().getDouble("final-crimson.red-haze-height",16));q.spawnParticle(Particle.REDSTONE,q.getLocation().add(0,h*.55,0),count,12,h*.55,12,0,new Particle.DustOptions(Color.fromRGB(190,0,0),1.8f));q.spawnParticle(Particle.ASH,q.getLocation().add(0,h*.45,0),Math.max(5,count/2),10,h*.45,10,.01);},0L,10L);
    }
    private void clearFinalEffectTest(){
        if(finalEffectTestTicker!=null){finalEffectTestTicker.cancel();finalEffectTestTicker=null;}
        if(finalEffectTestPlayer!=null){Player p=Bukkit.getPlayer(finalEffectTestPlayer);if(p!=null){p.resetPlayerTime();p.resetPlayerWeather();p.sendTitle("","",0,1,0);p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);p.stopSound("onigame:final_phase",SoundCategory.RECORDS);}}finalEffectTestPlayer=null;
    }

    private void showBloodSkyOverlay(World world){
        if(world==null||!getConfig().getBoolean("final-crimson.blood-sky.enabled",true))return;
        String fontRaw=getConfig().getString("final-crimson.blood-sky.font","onigame:final_sky");
        String glyph=getConfig().getString("final-crimson.blood-sky.glyph","\uE101");
        if(glyph==null||glyph.isBlank())glyph="\uE101";
        Key font;try{font=Key.key(fontRaw==null?"onigame:final_sky":fontRaw);}catch(IllegalArgumentException ex){font=Key.key("onigame","final_sky");}
        long fadeIn=Math.max(0,getConfig().getLong("final-crimson.blood-sky.fade-in-ticks",12))*50L;
        long stay=Math.max(20,getConfig().getLong("final-crimson.blood-sky.stay-ticks",3700))*50L;
        long fadeOut=Math.max(0,getConfig().getLong("final-crimson.blood-sky.fade-out-ticks",10))*50L;
        Title overlay=Title.title(Component.text(glyph).font(font),Component.empty(),Title.Times.times(Duration.ofMillis(fadeIn),Duration.ofMillis(stay),Duration.ofMillis(fadeOut)));
        for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world)p.showTitle(overlay);}
    }
    private void resetFinalCrimsonVisuals(){
        for(UUID id:participants){
            Player p=Bukkit.getPlayer(id);
            if(p!=null){p.resetPlayerTime();p.resetPlayerWeather();p.sendTitle("","",0,1,0);}
        }
    }
    private void startLongNightDawn(World world){
        if(world==null||!getConfig().getBoolean("final-dawn.enabled",true))return;
        if(dawnTicker!=null){dawnTicker.cancel();dawnTicker=null;}
        long start=Math.floorMod(getConfig().getLong("final-dawn.start-time",18000L),24000L);
        long endRaw=getConfig().getLong("final-dawn.end-time",1000L);
        long end=endRaw;
        while(end<=start)end+=24000L;
        int durationTicks=Math.max(20,getConfig().getInt("final-dawn.duration-seconds",35)*20);
        int stepTicks=Math.max(1,getConfig().getInt("final-dawn.step-ticks",2));
        world.setStorm(false);world.setThundering(false);world.setTime(start);
        for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world){p.sendTitle(cc("&f&l―― 永い夜が明ける ――"),cc("&7東の空が、ゆっくりと白み始めた。"),10,60,20);p.playSound(p.getLocation(),Sound.BLOCK_BEACON_ACTIVATE,.55f,.70f);}}
        all("&f&l―― 永い夜が明ける。");
        final long begin=start, finish=end;final int[] elapsed={0};final BukkitTask[] ref=new BukkitTask[1];
        ref[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(state!=GameState.RUNNING||world==null){if(ref[0]!=null)ref[0].cancel();dawnTicker=null;return;}
            elapsed[0]+=stepTicks;double progress=Math.min(1.0,elapsed[0]/(double)durationTicks);
            double eased=progress*progress*(3.0-2.0*progress);
            long absolute=begin+Math.round((finish-begin)*eased);world.setTime(Math.floorMod(absolute,24000L));
            if(progress>=1.0){
                world.setTime(Math.floorMod(finish,24000L));
                if(ref[0]!=null)ref[0].cancel();dawnTicker=null;
                applyDawnOniDebuff();
                for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld()==world)p.playSound(p.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.45f,1.25f);}
            }
        },stepTicks,stepTicks);
        dawnTicker=ref[0];
    }
    private void applyDawnOniDebuff(){
        if(!getConfig().getBoolean("final-dawn.oni-debuff.enabled",true))return;
        LivingEntity hunter=getOniEntity();if(hunter==null||!hunter.isValid()||hunter.isDead())return;
        int duration=Math.max(20,getConfig().getInt("final-dawn.oni-debuff.duration-seconds",9999)*20);
        int weakness=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.weakness-amplifier",2));
        int slowness=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.slowness-amplifier",1));
        int fatigue=Math.max(0,getConfig().getInt("final-dawn.oni-debuff.mining-fatigue-amplifier",1));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,duration,weakness,false,true));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,duration,slowness,false,true));
        hunter.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING,duration,fatigue,false,true));
        if(getConfig().getBoolean("final-dawn.oni-debuff.glowing",true))hunter.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,duration,0,false,false));
        double maxLoss=Math.max(0.0,getConfig().getDouble("final-dawn.oni-debuff.max-health-loss",20.0));
        if(maxLoss>0&&hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null){
            double currentMax=hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double minMax=Math.max(1.0,getConfig().getDouble("final-dawn.oni-debuff.minimum-max-health",20.0));
            double reduced=Math.max(minMax,currentMax-maxLoss);
            hunter.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(reduced);
            if(hunter.getHealth()>reduced)hunter.setHealth(reduced);
        }
        all("&6&l朝日が昇る――鬼の力が焼かれていく！");
        if(hunter instanceof Player p){
            p.sendTitle(cc("&6&l朝日の呪い"),cc("&c夜の加護が失われ、力が大きく弱まった"),5,60,15);
            p.playSound(p.getLocation(),Sound.ENTITY_WITHER_HURT,1.0f,1.35f);
        }else{
            hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WITHER_HURT,1.0f,1.35f);
        }
        hunter.getWorld().spawnParticle(Particle.FLAME,hunter.getLocation().add(0,1,0),55,.7,1.0,.7,.04);
        hunter.getWorld().spawnParticle(Particle.END_ROD,hunter.getLocation().add(0,1,0),35,.7,1.0,.7,.05);
    }
    private ItemStack jakutsukiSnakeSlashItem(){
        ItemStack i=item(Material.NETHERITE_SWORD,"&f&l蛇斬","jakutsuki_snake_slash");
        ItemMeta m=i.getItemMeta();m.setUnbreakable(true);m.setLore(List.of(cc("&7蛇窟姫の蛇を一撃で斬り伏せる退魔刀。"),cc("&7最終盤では蛇窟姫への攻撃にも高い威力を発揮する。"),cc("&d対蛇: 一撃撃破 &7/ &c対蛇窟姫: 高威力")));i.setItemMeta(m);return i;
    }
    private boolean isJakutsukiSnakeSlash(ItemStack stack){return "jakutsuki_snake_slash".equals(actionOf(stack));}
    private void grantJakutsukiFinalWeapons(){
        all("&f&l――蛇窟が崩れ、退魔刀『蛇斬』が顕現した。");
        for(UUID id:players){
            if(dead.contains(id)||escaped.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p==null)continue;
            ItemStack blade=jakutsukiSnakeSlashItem();HashMap<Integer,ItemStack> overflow=p.getInventory().addItem(blade);for(ItemStack left:overflow.values())p.getWorld().dropItemNaturally(p.getLocation(),left);
            p.sendTitle(cc("&f&l蛇 斬"),cc("&d反攻の刻――蛇を断ち、蛇窟姫を討て"),5,55,12);p.playSound(p.getLocation(),Sound.ITEM_TRIDENT_RETURN,1.0f,.75f);p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation().add(0,1,0),35,.65,.9,.65,.06);
        }
        for(UUID id:playerBots){
            if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.isValid()||bot.isDead())continue;
            bot.getEquipment().setItemInMainHand(jakutsukiSnakeSlashItem());bot.getEquipment().setItemInMainHandDropChance(0);bot.getWorld().spawnParticle(Particle.END_ROD,bot.getLocation().add(0,1,0),25,.55,.75,.55,.05);
        }
        LivingEntity hunter=getOniEntity();if(hunter!=null){hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WITHER_DEATH,.8f,.55f);hunter.getWorld().spawnParticle(Particle.SQUID_INK,hunter.getLocation().add(0,1,0),85,1.2,1.2,1.2,.08);}
    }
    private void updateHeartCracks(Block block,int hp){int max=getConfig().getInt("heart-max-health",100);float progress=Math.max(0f,Math.min(1f,1f-hp/(float)Math.max(1,max)));for(Player viewer:block.getWorld().getPlayers())viewer.sendBlockDamage(block.getLocation(),progress);}
    private void notifyHeartAttack(Location heart,String heartKey,long now){if(getConfig().getBoolean("bot.hunter-ai.enabled",true)&&oni!=null){Player hunter=Bukkit.getPlayer(oni);if(hunter!=null){int ticks=Math.max(0,getConfig().getInt("bot.hunter-ai.human-oni-heart-alert-speed-ticks",50));int amp=Math.max(0,getConfig().getInt("bot.hunter-ai.human-oni-heart-alert-speed-amplifier",0));if(ticks>0)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amp,false,true));}}double seconds=getConfig().getDouble("heart-alert-cooldown-seconds",5.0);if(hasOniPassive(OniPassiveSkill.GUARDIAN))seconds*=Math.max(0.1,getConfig().getDouble("oni-passive-skills.guardian-alert-interval-multiplier",0.50));long cooldown=(long)(seconds*1000L);if(now-heartAlertAt.getOrDefault(heartKey,0L)<cooldown)return;heartAlertAt.put(heartKey,now);LivingEntity hunter=getOniEntity();if(hunter==null)return;if(hunter instanceof Player p)msg(p,"&4心臓が傷つけられている…… &c"+directionFrom(p.getLocation(),heart));if(hasOniPassive(OniPassiveSkill.HOMING)&&hunter.getWorld().equals(heart.getWorld())&&hunter.getLocation().distanceSquared(heart)>400)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.homing-speed-ticks",80)),1,false,true));if(hasOniPassive(OniPassiveSkill.PULSE)){int ticks=Math.max(20,getConfig().getInt("oni-passive-skills.pulse-speed-ticks",70));int amp=Math.max(0,getConfig().getInt("oni-passive-skills.pulse-speed-amplifier",0));hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amp,false,true));if(hunter instanceof Player p)p.sendActionBar(cc("&d脈動 &7――心臓の悲鳴を辿り加速"));}hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,1.0f,0.55f);}
    private void forceHeartAlert(Location heart){LivingEntity hunter=getOniEntity();if(hunter==null)return;if(hunter instanceof Player p)msg(p,"&4作業失敗の大きな音！ &c"+directionFrom(p.getLocation(),heart));hunter.getWorld().playSound(hunter.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,0.8f,0.7f);}
    private String directionFrom(Location from,Location to){double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();String ns=Math.abs(dz)<2?"":(dz<0?"北":"南"),ew=Math.abs(dx)<2?"":(dx>0?"東":"西");String result=ns+ew;return result.isEmpty()?"すぐ近く":result+"方向";}
    private String chaseBgmSound(){return "onigame:chase_tatari";}
    private void cancelNormalBgmFade(UUID id){List<BukkitTask> tasks=normalBgmFadeTasks.remove(id);if(tasks!=null)for(BukkitTask t:tasks)if(t!=null)t.cancel();}
    private void enterChaseBgm(Player p){
        if(p==null||!getConfig().getBoolean("chase-bgm.enabled",true)||finalPhase)return;
        UUID id=p.getUniqueId();
        BukkitTask release=chaseReleaseTasks.remove(id);if(release!=null)release.cancel();
        cancelNormalBgmFade(id);
        String normal=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");
        p.stopSound(normal,SoundCategory.RECORDS);
        if(chaseBgmPlayers.add(id)){
            String sound=chaseBgmSound();
            float volume=(float)getConfig().getDouble("chase-bgm.volume",0.9),pitch=(float)getConfig().getDouble("chase-bgm.pitch",1.0);
            p.stopSound(sound,SoundCategory.RECORDS);
            p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);
        }
    }
    private void scheduleLeaveChaseBgm(Player p){
        if(p==null)return;UUID id=p.getUniqueId();
        BukkitTask old=chaseReleaseTasks.remove(id);if(old!=null)old.cancel();
        long delay=Math.max(0,getConfig().getLong("chase-bgm.release-delay-ticks",50));
        BukkitTask task=Bukkit.getScheduler().runTaskLater(this,()->{
            chaseReleaseTasks.remove(id);
            if(state!=GameState.RUNNING||finalPhase||chased.contains(id))return;
            leaveChaseBgmNow(p);
        },delay);
        chaseReleaseTasks.put(id,task);
    }
    private void leaveChaseBgmNow(Player p){
        if(p==null)return;UUID id=p.getUniqueId();
        if(!chaseBgmPlayers.remove(id))return;
        p.stopSound(chaseBgmSound(),SoundCategory.RECORDS);
        resumeNormalBgmSoft(p);
    }
    private void resumeNormalBgmSoft(Player p){
        if(p==null||state!=GameState.RUNNING||finalPhase)return;
        UUID id=p.getUniqueId();cancelNormalBgmFade(id);
        long startDelay=Math.max(0,getConfig().getLong("chase-bgm.resume-delay-ticks",12));
        int steps=Math.max(1,getConfig().getInt("chase-bgm.resume-fade-steps",4));
        long stepTicks=Math.max(1,getConfig().getLong("chase-bgm.resume-fade-step-ticks",10));
        float normalVolume=(float)getConfig().getDouble("game-start-bgm.volume",0.8);
        float startVolume=(float)Math.max(0.01,Math.min(normalVolume,getConfig().getDouble("chase-bgm.resume-volume",0.28)));
        String normal=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");
        float pitch=(float)getConfig().getDouble("game-start-bgm.pitch",1.0);
        List<BukkitTask> tasks=new ArrayList<>();
        for(int i=0;i<steps;i++){
            final int idx=i;
            BukkitTask t=Bukkit.getScheduler().runTaskLater(this,()->{
                if(state!=GameState.RUNNING||finalPhase||chased.contains(id)||chaseBgmPlayers.contains(id))return;
                float vol=startVolume+(normalVolume-startVolume)*((idx+1)/(float)steps);
                p.stopSound(normal,SoundCategory.RECORDS);
                p.playSound(p.getLocation(),normal,SoundCategory.RECORDS,vol,pitch);
            },startDelay+idx*stepTicks);
            tasks.add(t);
        }
        normalBgmFadeTasks.put(id,tasks);
    }
    private void stopAllChaseBgm(){
        for(BukkitTask t:chaseReleaseTasks.values())if(t!=null)t.cancel();chaseReleaseTasks.clear();
        for(List<BukkitTask> list:normalBgmFadeTasks.values())for(BukkitTask t:list)if(t!=null)t.cancel();normalBgmFadeTasks.clear();
        String sound=chaseBgmSound();
        for(UUID id:new HashSet<>(chaseBgmPlayers)){Player p=Bukkit.getPlayer(id);if(p!=null)p.stopSound(sound,SoundCategory.RECORDS);}
        chaseBgmPlayers.clear();
    }
    private void startGameStartBgm(){if(!getConfig().getBoolean("game-start-bgm.enabled",true))return;stopGameStartBgm();activeGameStartBgmSound=getConfig().getString("game-start-bgm.sound","onigame:game_start");playGameStartBgmToAll();long replay=Math.max(0,getConfig().getLong("game-start-bgm.replay-seconds",166));if(replay>0)gameStartBgmTicker=Bukkit.getScheduler().runTaskTimer(this,this::playGameStartBgmToAll,replay*20L,replay*20L);}
    private void playGameStartBgmToAll(){if(state!=GameState.RUNNING||finalPhase)return;for(UUID id:participants){if(chased.contains(id)||chaseBgmPlayers.contains(id))continue;Player p=Bukkit.getPlayer(id);if(p!=null)playGameStartBgm(p);}}
    private void playGameStartBgm(Player p){String sound=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");float volume=(float)getConfig().getDouble("game-start-bgm.volume",0.8),pitch=(float)getConfig().getDouble("game-start-bgm.pitch",1.0);p.stopSound(sound,SoundCategory.RECORDS);p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);}
    private void stopGameStartBgm(){if(gameStartBgmTicker!=null){gameStartBgmTicker.cancel();gameStartBgmTicker=null;}String sound=activeGameStartBgmSound!=null?activeGameStartBgmSound:getConfig().getString("game-start-bgm.sound","onigame:game_start");for(Player p:Bukkit.getOnlinePlayers())p.stopSound(sound,SoundCategory.RECORDS);activeGameStartBgmSound=null;}
    private String finalPhaseBgmSection(){return oniType==OniType.JAKUTSUKI?"jakutsuki-final-phase-bgm":"final-phase-bgm";}
    private void startFinalPhaseBgm(){
        stopAllChaseBgm();stopGameStartBgm();
        String section=finalPhaseBgmSection();
        finalPhase=true;
        if(!getConfig().getBoolean(section+".enabled",true))return;
        if(bgmTicker!=null){bgmTicker.cancel();bgmTicker=null;}
        String configured=getConfig().getString(section+".sound","onigame:chase_tatari");
        activeBgmSound=(configured==null||configured.isBlank())?"onigame:chase_tatari":configured;
        // 通常追跡曲と祟りを両方いったん停止し、最終フェーズでは祟りを先頭から確実に再生する。
        for(Player p:Bukkit.getOnlinePlayers()){
            p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);
            p.stopSound("onigame:chase_tatari",SoundCategory.RECORDS);
        }
        Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)playFinalPhaseBgmToAll();},4L);
        Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&finalPhase)playFinalPhaseBgmToAll();},24L);
        long replay=Math.max(0,getConfig().getLong(section+".replay-seconds",271));
        if(replay>0)bgmTicker=Bukkit.getScheduler().runTaskTimer(this,this::playFinalPhaseBgmToAll,replay*20L,replay*20L);
    }
    private void playFinalPhaseBgmToAll(){if(state!=GameState.RUNNING)return;for(UUID id:participants){Player p=Bukkit.getPlayer(id);if(p!=null)playConfiguredBgm(p);}}
    private void playConfiguredBgm(Player p){String section=finalPhaseBgmSection();String sound=activeBgmSound!=null?activeBgmSound:getConfig().getString(section+".sound","onigame:chase_tatari");if(sound==null||sound.isBlank())sound="onigame:chase_tatari";float volume=(float)getConfig().getDouble(section+".volume",0.8),pitch=(float)getConfig().getDouble(section+".pitch",1.0);p.stopSound(sound,SoundCategory.RECORDS);p.playSound(p.getLocation(),sound,SoundCategory.RECORDS,volume,pitch);}
    private void stopFinalPhaseBgm(){if(bgmTicker!=null){bgmTicker.cancel();bgmTicker=null;}String sound=activeBgmSound!=null?activeBgmSound:getConfig().getString(finalPhaseBgmSection()+".sound",oniType==OniType.JAKUTSUKI?"onigame:jakutsuki_final_phase":"onigame:final_phase");for(Player p:Bukkit.getOnlinePlayers())p.stopSound(sound,SoundCategory.RECORDS);activeBgmSound=null;}
    @EventHandler public void onEntityPlace(EntityPlaceEvent e){
        if(!(e.getEntity() instanceof ArmorStand stand)||e.getPlayer()==null)return;
        ItemStack held=e.getHand()==EquipmentSlot.HAND?e.getPlayer().getInventory().getItemInMainHand():e.getPlayer().getInventory().getItemInOffHand();
        if(!held.hasItemMeta())return;String action=held.getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(!"heart_marker".equals(action))return;
        configureMarker(stand);msg(e.getPlayer(),"心臓地点マーカーを設置しました。ゲーム開始時、この位置に心臓が生成されます。");
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent e){
        if(isTagMode()&&state==GameState.RUNNING&&("tag_nav_compass".equals(actionOf(e.getCurrentItem()))||"tag_nav_compass".equals(actionOf(e.getCursor())))){e.setCancelled(true);return;}
        if(personalLootInventories.containsValue(e.getView().getTopInventory())){if(e.getClickedInventory()==null)return;if(e.getClickedInventory().equals(e.getView().getTopInventory())&&isConsumableItem(e.getCurrentItem())&&e.getWhoClicked() instanceof Player player&&carriedConsumables(player)>=getConfig().getInt("loot-chests.max-carried-items",3)){e.setCancelled(true);msg(player,"使い切りアイテムは合計3個まで持てます。");return;}if(e.getClickedInventory().equals(e.getView().getBottomInventory())&&e.isShiftClick()){e.setCancelled(true);return;}if(e.getClickedInventory().equals(e.getView().getTopInventory())&&((e.getCursor()!=null&&!e.getCursor().getType().isAir())||e.getClick().isKeyboardClick()))e.setCancelled(true);return;}
        if(e.getInventory().getHolder() instanceof MenuHolder holder){
            e.setCancelled(true);
            if(!(e.getWhoClicked() instanceof Player p)||e.getCurrentItem()==null||!e.getCurrentItem().hasItemMeta())return;
            String action=e.getCurrentItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
            if(action==null)return;
            if(holder.type.equals("sp_ranking")){if(action.equals("sp_rank_toggle")){spRankingSingleMode.put(p.getUniqueId(),!spRankingSingleMode.getOrDefault(p.getUniqueId(),false));openSpRanking(p);}return;}
            if(holder.type.startsWith("sp_shop:")){if(action.startsWith("sp_buy:")){buySpProduct(p,action.substring("sp_buy:".length()));p.closeInventory();}return;}
            if(action.equals("menu_page:presets")){openPresetMenu(p);return;}
            if(action.equals("menu_page:skills")){openSkillMenu(p);return;}
            if(action.startsWith("menu_page:skill:")){openSkillPage(p,Integer.parseInt(action.substring("menu_page:skill:".length())));return;}
            if(action.startsWith("menu_page:passive:")){openPassivePage(p,Integer.parseInt(action.substring("menu_page:passive:".length())));return;}
            if(action.startsWith("menu_oni_passive_page:")){openOniPassivePage(p,Integer.parseInt(action.substring("menu_oni_passive_page:".length())));return;}
            if(action.equals("menu_oni_back")){openOniMenu(p);return;}
            if(action.equals("menu_favorite_toggle")){toggleFavoriteLoadout(p);if(holder.type.startsWith("passives"))openPassivePage(p,menuPageFromType(holder.type));else openSkillPage(p,menuPageFromType(holder.type));return;}
            if(holder.type.equals("presets")&&action.startsWith("menu_build:")){applyTacticalPreset(p,action.substring("menu_build:".length()));openPresetMenu(p);return;}
            if(holder.type.startsWith("skills")&&action.startsWith("menu_skill:")){
                PlayerSkill skill=PlayerSkill.valueOf(action.substring("menu_skill:".length()));
                selectedSkill.put(p.getUniqueId(),skill);
                selectedPresetNames.remove(p.getUniqueId());
                saveFavoriteIfLocked(p);
                msg(p,"メインスキルを &b"+skill.display+" &fに設定しました。");
                openSkillPage(p,menuPageFromType(holder.type));
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:IGAMI_KYOYA")){
                selectedPresetNames.put(p.getUniqueId(),"IGAMI_KYOYA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.ONI_STRIKE);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.ATTACK_BOOST);
                selected.add(PassiveSkill.DURABILITY_BOOST);
                msg(p,"プリセット &6伊神京也（イガミキョウヤ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZANAMI_MISAKI")){
                selectedPresetNames.put(p.getUniqueId(),"AZANAMI_MISAKI");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SPRINT);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.LIGHT_FOOTED);
                selected.add(PassiveSkill.DEEP_BREATH);
                msg(p,"プリセット &b字那美咲（アザナミサキ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:ARIKAWA_FUUKA")){
                selectedPresetNames.put(p.getUniqueId(),"ARIKAWA_FUUKA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SMOKE);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.COWARDICE);
                selected.add(PassiveSkill.EXORCISM);
                msg(p,"プリセット &d有川風香（アリカワフウカ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZANAMI_REN")){
                selectedPresetNames.put(p.getUniqueId(),"AZANAMI_REN");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.OBSESSION);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.LIGHT_FOOTED);
                selected.add(PassiveSkill.DURABILITY_BOOST);
                msg(p,"プリセット &3字那美蓮（アザナミレン） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:MEDIC")){
                selectedPresetNames.put(p.getUniqueId(),"MEDIC");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.HEAL);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.FOCUS);
                selected.add(PassiveSkill.BOND);
                msg(p,"プリセット &a佐倉夏海（サクラナツミ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AKASAKA_HIIRO")){
                selectedPresetNames.put(p.getUniqueId(),"AKASAKA_HIIRO");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.SAFE_LANDING);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.NINJA_BLOOD);
                selected.add(PassiveSkill.LEAP);
                msg(p,"EXプリセット &c赤坂陽彩（アカサカヒイロ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:KAGAYA_RION")){
                selectedPresetNames.put(p.getUniqueId(),"KAGAYA_RION");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.BLINK);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.QUICK_TURN);
                selected.add(PassiveSkill.CORNERED_RAT);
                msg(p,"EXプリセット &a加賀谷凛音（カガヤリオン） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AMANAI_IONA")){
                selectedPresetNames.put(p.getUniqueId(),"AMANAI_IONA");
                selectedSkill.put(p.getUniqueId(),PlayerSkill.ECHO);
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selected.clear();
                selected.add(PassiveSkill.SILENT_BREATH);
                selected.add(PassiveSkill.DECOY);
                msg(p,"EXプリセット &9天内伊御奈（アマナイイオナ） &fを設定しました。");
                openPresetMenu(p);
            }else if(holder.type.equals("presets")&&action.equals("menu_preset:AZAKUJI_HIRO")){
                msg(p,"&6字九字ひろ &fはNPC友軍専用です。プレイヤーは選択できません。");
                openPresetMenu(p);
            }else if(holder.type.startsWith("passives")&&action.startsWith("menu_passive:")){
                PassiveSkill passive=PassiveSkill.valueOf(action.substring("menu_passive:".length()));
                LinkedHashSet<PassiveSkill> selected=selectedPassives.computeIfAbsent(p.getUniqueId(),id->new LinkedHashSet<>());
                selectedPresetNames.remove(p.getUniqueId());
                if(selected.remove(passive))msg(p,"パッシブ &a"+passive.display+" &fを解除しました。");
                else if(selected.size()>=2)msg(p,"パッシブスキルは2個までです。選択済みを解除してください。");
                else{selected.add(passive);msg(p,"パッシブ &a"+passive.display+" &fを設定しました。");}
                saveFavoriteIfLocked(p);
                openPassivePage(p,menuPageFromType(holder.type));
            }else if(holder.type.equals("oni")&&action.startsWith("menu_oni:")){
                OniType requested=OniType.valueOf(action.substring("menu_oni:".length()));if(isOniTypeLocked(requested)){msg(p,"&c"+requested.display+" &fは現在ロックされています。 ");openOniMenu(p);return;}selectedOniType=requested;
                msg(p,"次の鬼を &c"+selectedOniType.display+" &fに設定しました。");
                openOniMenu(p);
            }else if(holder.type.startsWith("oni_passives")&&action.startsWith("menu_oni_passive:")){
                OniPassiveSkill passive=OniPassiveSkill.valueOf(action.substring("menu_oni_passive:".length()));
                LinkedHashSet<OniPassiveSkill> selected=selectedOniPassives;
                if(selected.remove(passive))msg(p,"鬼パッシブ &5"+passive.display+" &fを解除しました。");
                else if(selected.size()>=4)msg(p,"鬼パッシブは4個までです。選択済みを解除してください。");
                else{selected.add(passive);msg(p,"鬼パッシブ &5"+passive.display+" &fを設定しました。");}
                openOniPassivePage(p,menuPageFromType(holder.type));
            }
            return;
        }
        if(state==GameState.RUNNING&&e.getWhoClicked() instanceof Player player&&(isMainSkillItem(e.getCurrentItem())||isMainSkillItem(e.getCursor())||(e.getHotbarButton()>=0&&isMainSkillItem(player.getInventory().getItem(e.getHotbarButton()))))){e.setCancelled(true);return;}
        if(state!=GameState.RUNNING||oni==null||!e.getWhoClicked().getUniqueId().equals(oni))return;if(e.getSlotType()==InventoryType.SlotType.ARMOR||isDakkoArmor(e.getCurrentItem())||isDakkoArmor(e.getCursor())||isJakutsukiArmor(e.getCurrentItem())||isJakutsukiArmor(e.getCursor()))e.setCancelled(true);
    }
    @EventHandler public void onInventoryDrag(InventoryDragEvent e){if((personalLootInventories.containsValue(e.getView().getTopInventory())||e.getView().getTopInventory().getHolder() instanceof MenuHolder)&&e.getRawSlots().stream().anyMatch(slot->slot<e.getView().getTopInventory().getSize()))e.setCancelled(true);}
    private void throwDecoyStone(Player player){
        debugTrace("decoy","DCY-401","throw requested player="+player.getName()+" state="+state);
        UUID id=player.getUniqueId();
        boolean training=state==GameState.WAITING&&trainingPlayers.contains(id)&&isInTrainingArea(player.getLocation());
        if((!players.contains(id)&&!training)||!hasPassive(id,PassiveSkill.DECOY))return;
        long cd=training?Math.max(1,getConfig().getLong("training.cooldown-seconds",1)):Math.max(1,getConfig().getLong("technical-passives.decoy-cooldown-seconds",40));
        if(!technicalReady(id,"DECOY",cd))return;
        Location eye=player.getEyeLocation();
        Vector velocity=eye.getDirection().normalize().multiply(Math.max(.6,getConfig().getDouble("technical-passives.decoy-throw-power",1.35)));
        velocity.setY(Math.max(.18,velocity.getY()+.12));
        Snowball projectile=player.getWorld().spawn(eye.clone().add(eye.getDirection().multiply(.35)),Snowball.class);
        projectile.setShooter(player);
        projectile.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"decoy_projectile");
        projectile.setVelocity(velocity);
        Vector groundDirection=eye.getDirection().setY(0);
        if(groundDirection.lengthSquared()<.01)groundDirection=new Vector(0,0,1);
        decoyProjectileDirections.put(projectile.getUniqueId(),groundDirection.normalize());
        debugTrace("decoy","DCY-402","projectile spawned uuid="+projectile.getUniqueId()+" at="+projectile.getLocation().getBlockX()+","+projectile.getLocation().getBlockY()+","+projectile.getLocation().getBlockZ());
        player.swingMainHand();
        player.playSound(player.getLocation(),Sound.ENTITY_SNOWBALL_THROW,.8f,.85f);
        player.sendActionBar(cc("&b陽動 &7――偽の逃走者を放った"));
    }
    @EventHandler public void onDecoyHit(ProjectileHitEvent e){
        if(!(e.getEntity() instanceof Snowball projectile))return;
        String action=projectile.getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);
        if(!"decoy_projectile".equals(action))return;
        Vector direction=decoyProjectileDirections.remove(projectile.getUniqueId());
        if(direction==null||direction.lengthSquared()<.01)direction=new Vector(0,0,1);
        Location hit=projectile.getLocation().clone();
        UUID owner=(projectile.getShooter() instanceof Player shooter)?shooter.getUniqueId():null;
        debugTrace("decoy","DCY-403","projectile hit at="+hit.getBlockX()+","+hit.getBlockY()+","+hit.getBlockZ());
        projectile.remove();
        startDecoyRunner(hit,direction.normalize(),owner);
    }
    private void startDecoyRunner(Location start,Vector initialDirection,UUID owner){
        if(start.getWorld()==null)return;
        debugTrace("decoy","DCY-404","runner start at="+start.getBlockX()+","+start.getBlockY()+","+start.getBlockZ());
        ArmorStand marker=start.getWorld().spawn(start,ArmorStand.class);
        marker.setVisible(false);marker.setMarker(true);marker.setSmall(true);marker.setGravity(false);marker.setInvulnerable(true);marker.setSilent(true);
        long duration=Math.max(1,getConfig().getLong("technical-passives.decoy-duration-seconds",12))*1000L;
        long expires=System.currentTimeMillis()+duration;
        fakeNoiseUntil.put(marker.getUniqueId(),expires);
        if(owner!=null)decoyOwners.put(marker.getUniqueId(),owner);
        Vector baseDirection=initialDirection.clone();
        new Runnable(){private Vector direction=baseDirection;private int ticks=0;@Override public void run(){
            if(!marker.isValid()||System.currentTimeMillis()>=expires){fakeNoiseUntil.remove(marker.getUniqueId());decoyOwners.remove(marker.getUniqueId());decoyRewarded.remove(marker.getUniqueId());if(marker.isValid())marker.remove();return;}
            Location here=marker.getLocation();
            double step=Math.max(.15,getConfig().getDouble("technical-passives.decoy-run-step",.62));
            Location next=here.clone().add(direction.clone().multiply(step));
            Block feet=next.getBlock(),head=next.clone().add(0,1,0).getBlock();
            if(feet.getType().isSolid()||head.getType().isSolid()){
                double turn=((ticks/4)%2==0?1:-1)*(Math.PI/2.0);
                direction=direction.clone().rotateAroundY(turn).normalize();
            }else marker.teleport(next);
            marker.getWorld().playSound(marker.getLocation(),Sound.BLOCK_GRASS_STEP,.75f,.95f+(float)(Math.random()*.15));
            if(ticks>0&&ticks%28==0)marker.getWorld().playSound(marker.getLocation(),Sound.ENTITY_PLAYER_ATTACK_NODAMAGE,.45f,1.35f);
            if(ticks>0&&ticks%44==0){direction=direction.clone().rotateAroundY(((ticks/44)%2==0?1:-1)*Math.PI/3.0).normalize();marker.getWorld().playSound(marker.getLocation(),Sound.BLOCK_STONE_STEP,.65f,1.15f);}
            marker.getWorld().spawnParticle(Particle.CLOUD,marker.getLocation().clone().add(0,.08,0),2,.10,.03,.10,.01);
            UUID decoyOwner=decoyOwners.get(marker.getUniqueId());LivingEntity hunter=getOniEntity();
            if(decoyOwner!=null&&hunter!=null&&!decoyRewarded.contains(marker.getUniqueId())&&hunter.getWorld().equals(marker.getWorld())&&hunter.getLocation().distanceSquared(marker.getLocation())<=Math.pow(Math.max(2.0,getConfig().getDouble("technical-passives.decoy-success-radius",5.0)),2)){Player ownerPlayer=Bukkit.getPlayer(decoyOwner);if(ownerPlayer!=null&&players.contains(decoyOwner)&&!dead.contains(decoyOwner)&&!escaped.contains(decoyOwner)){decoyRewarded.add(marker.getUniqueId());ownerPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("technical-passives.decoy-success-speed-ticks",100)),0,false,true));ownerPlayer.sendActionBar(cc("&b陽動成功 &7――鬼を欺いた！ &f移動速度上昇"));debugTrace("decoy","DCY-405","deception success owner="+ownerPlayer.getName());}}
            ticks+=4;
            Bukkit.getScheduler().runTaskLater(OniGamePlugin.this,this,4L);
        }}.run();
    }
    @EventHandler public void onDrop(PlayerDropItemEvent e){
        if(state==GameState.WAITING){
            e.setCancelled(true);
            e.getPlayer().sendActionBar(cc("&7ロビーではアイテムを捨てられません。"));
            return;
        }
        if(state!=GameState.RUNNING)return;
        String action=actionOf(e.getItemDrop().getItemStack());
        if("tag_nav_compass".equals(action)){e.setCancelled(true);e.getPlayer().sendActionBar(cc("&7エリアナビは捨てられません。"));return;}
        if("passive:decoy".equals(action)){
            e.setCancelled(true);
            e.getPlayer().sendActionBar(cc("&7陽動は &e右クリック &7で投げます。"));
            return;
        }
        if(isMainSkillItem(e.getItemDrop().getItemStack())||(isOni(e.getPlayer().getUniqueId())&&(isDakkoArmor(e.getItemDrop().getItemStack())||isJakutsukiArmor(e.getItemDrop().getItemStack()))))e.setCancelled(true);
    }
    @EventHandler public void onPickup(EntityPickupItemEvent e){if(fakeNoiseUntil.containsKey(e.getItem().getUniqueId())){e.setCancelled(true);return;}if(state==GameState.RUNNING&&e.getEntity() instanceof Player player&&isConsumableItem(e.getItem().getItemStack())&&carriedConsumables(player)>=getConfig().getInt("loot-chests.max-carried-items",3))e.setCancelled(true);}
    private void weakenOni(){
        LivingEntity entity=getOniEntity();if(entity==null)return;
        double max;if(oniType==OniType.JAKUTSUKI){double base=Math.max(1.0,getConfig().getDouble("jakutsuki-stats.max-health",200.0)),loss=Math.max(0.0,getConfig().getDouble("jakutsuki-stats.max-health-loss-per-heart",10.0));max=Math.max(1.0,base-(brokenHearts*loss));}else max=Math.max(30,60-(brokenHearts*6));entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);if(entity.getHealth()>max)entity.setHealth(max);if(oniType==OniType.JAKUTSUKI){if(entity instanceof Player jp)jp.sendActionBar(cc("&5蛇窟姫 &7最大体力が &c"+(int)max+" &7まで低下した"));}
        entity.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,120,Math.min(3,brokenHearts-1),false,true));
        if(entity instanceof Player p)refreshOniUnlockedSkills(p,true);
    }

    @EventHandler(priority=EventPriority.LOWEST,ignoreCancelled=false)
    public void onLobbyUtilityInteractEarly(PlayerInteractEvent e){
        if(state!=GameState.WAITING||e.getClickedBlock()==null||!e.getAction().isRightClick()||e.getHand()!=EquipmentSlot.HAND)return;
        Block block=e.getClickedBlock();
        if(isSkillChest(block)){queueLobbyUtilityGui(e.getPlayer(),"skill");return;}
        if(isOniChest(block)){queueLobbyUtilityGui(e.getPlayer(),"oni");return;}
        if(isPracticeChest(block)){queueLobbyUtilityGui(e.getPlayer(),"practice-chest");return;}
        if(isPracticeHeartKey(LocationStore.encode(block.getLocation())))queueLobbyUtilityGui(e.getPlayer(),"practice-heart");
    }
    private void queueLobbyUtilityGui(Player player,String kind){
        UUID id=player.getUniqueId();
        if(!lobbyUtilityGuiQueued.add(id))return;
        Bukkit.getScheduler().runTaskLater(this,()->{
            lobbyUtilityGuiQueued.remove(id);
            if(!player.isOnline()||state!=GameState.WAITING)return;
            switch(kind){
                case "skill"->openSkillMenu(player);
                case "oni"->openOniMenu(player);
                case "practice-chest"->{
                    Location configured=LocationStore.get(getConfig(),"locations.practice-loot-chest");
                    if(configured!=null)startPracticeChestOpening(player,LocationStore.encode(configured));
                }
                case "practice-heart"->{
                    Location configured=LocationStore.get(getConfig(),"locations.practice-heart");
                    if(configured!=null&&configured.getWorld()!=null&&configured.getWorld().equals(player.getWorld())
                            &&player.getLocation().distanceSquared(configured.clone().add(.5,.5,.5))<=7.5625)
                        toggleHeartRepair(player,configured.getBlock());
                }
            }
        },1L);
    }

    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=false) public void onInteract(PlayerInteractEvent e){
        if(isTagMode()&&state==GameState.RUNNING&&e.getHand()==EquipmentSlot.HAND&&"tag_nav_compass".equals(actionOf(e.getItem()))&&(e.getAction().isRightClick()||e.getAction().isLeftClick())){e.setCancelled(true);cycleTagNavigation(e.getPlayer(),e.getAction().isRightClick()?1:-1);return;}
        if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND&&useIkimonoBuff(e.getPlayer(),e.getItem())){e.setCancelled(true);return;}
        if(e.getClickedBlock()!=null&&e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND&&isSpRankingBlock(e.getClickedBlock())){e.setCancelled(true);openSpRanking(e.getPlayer());return;}
        if(e.getClickedBlock()!=null&&e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND&&isSpShopBlock(e.getClickedBlock())){e.setCancelled(true);if(!isTagMode()){msg(e.getPlayer(),"&7SPショップは鬼ごっこモード用です。");return;}openSpShop(e.getPlayer(),e.getClickedBlock());return;}
        if(isTagMode()&&state==GameState.RUNNING&&e.getClickedBlock()!=null&&e.getHand()==EquipmentSlot.HAND&&(e.getAction().isRightClick()||e.getAction().isLeftClick())){String gem=terraGemAt(e.getClickedBlock());if(gem!=null){e.setCancelled(true);startTerraGemAttunement(e.getPlayer(),gem);return;}}
        if(e.getClickedBlock()!=null&&isSkillChest(e.getClickedBlock())){
            // Always intercept the registered skill chest ourselves. This deliberately does
            // not rely on vanilla chest-use permission (spawn protection / region plugins
            // often allow OPs while denying normal players). Open the OniGame GUI on the
            // next server tick so a later vanilla/protection decision cannot replace it.
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND){
                Player player=e.getPlayer();
                if(state==GameState.WAITING)queueLobbyUtilityGui(player,"skill");
                else msg(player,"スキル設定チェストはロビーでのみ使用できます。");
            }
            return;
        }
        if(e.getClickedBlock()!=null&&isOniChest(e.getClickedBlock())){
            e.setCancelled(true);
            e.setUseInteractedBlock(Event.Result.DENY);
            e.setUseItemInHand(Event.Result.DENY);
            if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND){
                Player player=e.getPlayer();
                if(state==GameState.WAITING)queueLobbyUtilityGui(player,"oni");
                else msg(player,"鬼設定チェストはロビーでのみ使用できます。");
            }
            return;
        }
        if(state==GameState.WAITING&&e.getClickedBlock()!=null&&isPracticeChest(e.getClickedBlock())){e.setCancelled(true);e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)queueLobbyUtilityGui(e.getPlayer(),"practice-chest");return;}
        if(state==GameState.WAITING&&e.getClickedBlock()!=null&&isPracticeHeartKey(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)queueLobbyUtilityGui(e.getPlayer(),"practice-heart");return;}
        if(state==GameState.WAITING&&isInTrainingArea(e.getPlayer().getLocation())&&e.getItem()!=null&&e.getAction().isRightClick()&&e.getItem().hasItemMeta()){String trainingAction=e.getItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(trainingAction!=null&&trainingAction.startsWith("training_skill:")){e.setCancelled(true);PlayerSkill skill=PlayerSkill.valueOf(trainingAction.substring("training_skill:".length()));usePlayerSkill(e.getPlayer(),skill);trainingLog(e.getPlayer(),"&b"+skill.display+" &7を使用");return;}if(trainingAction!=null&&trainingAction.startsWith("training_oni:")){e.setCancelled(true);String action=trainingAction.substring("training_oni:".length());useOniSkill(e.getPlayer(),action);trainingLog(e.getPlayer(),"&c鬼スキル &7を使用");return;}}
        Player decoyUser=e.getPlayer();UUID decoyUserId=decoyUser.getUniqueId();boolean decoyTraining=state==GameState.WAITING&&trainingPlayers.contains(decoyUserId)&&isInTrainingArea(decoyUser.getLocation());
        if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND&&decoyUser.isSneaking()&&hasPassive(decoyUserId,PassiveSkill.DECOY)&&(players.contains(decoyUserId)||decoyTraining)){e.setCancelled(true);throwDecoyStone(decoyUser);return;}
        if(state!=GameState.RUNNING)return;
        if(e.getClickedBlock()!=null&&tagSkillChestKeys.contains(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)openTagSkillChest(e.getPlayer(),LocationStore.encode(e.getClickedBlock().getLocation()));return;}
        if(e.getClickedBlock()!=null&&lootChestKeys.contains(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)startChestOpening(e.getPlayer(),LocationStore.encode(e.getClickedBlock().getLocation()));return;}
        if(e.getClickedBlock()!=null&&heartHp.containsKey(LocationStore.encode(e.getClickedBlock().getLocation()))){e.setCancelled(true);if(e.getAction().isRightClick()&&e.getHand()==EquipmentSlot.HAND)toggleHeartRepair(e.getPlayer(),e.getClickedBlock());return;}
        if(e.getItem()==null||!e.getAction().isRightClick()||!e.getItem().hasItemMeta())return;String action=e.getItem().getItemMeta().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(action==null)return;e.setCancelled(true);Player p=e.getPlayer();if(action.startsWith("skill:")){if(!players.contains(p.getUniqueId()))return;PlayerSkill sk=PlayerSkill.valueOf(action.substring(6));String ck=p.getUniqueId()+":"+sk.name();long before=cooldowns.getOrDefault(ck,0L);usePlayerSkill(p,sk);long after=cooldowns.getOrDefault(ck,0L);if(isTagMode()&&e.getItem().getAmount()>0&&(after>before||sk==PlayerSkill.HEAL))consumeTagSkillUse(p,e.getItem(),sk);}else if(action.startsWith("item:")){if(!players.contains(p.getUniqueId()))return;useConsumable(p,action,e.getItem());}else if(action.equals("equipment:flare_gun")){if(!players.contains(p.getUniqueId()))return;useFlareGun(p);}else if(action.startsWith("equipment:")){e.setCancelled(false);}else if(action.equals("passive:decoy")){p.sendActionBar(cc("&b陽動 &7はスニーク＋右クリックで発動します。"));}else if(action.equals("oni_domain")&&isOni(p.getUniqueId()))useOniDomain(p);else if(action.equals("dakko_domain_spirit")&&isOni(p.getUniqueId()))useDakkoDomainSpirit(p);else if(oni!=null&&isOni(p.getUniqueId()))useOniSkill(p,action);else if(action.equals("tracker"))trackHeart(p);
    }
    private boolean isSkillChest(Block block){return matchesConfiguredChest(block,"locations.skill-chest");}
    private boolean isOniChest(Block block){return matchesConfiguredChest(block,"locations.oni-chest");}
    private boolean matchesConfiguredChest(Block block,String path){
        Location configured=LocationStore.get(getConfig(),path);
        if(configured==null||block==null||configured.getWorld()==null||!block.getWorld().equals(configured.getWorld()))return false;
        if(LocationStore.encode(configured).equals(LocationStore.encode(block.getLocation())))return true;
        // A configured chest may later become one half of a large chest.  In that case
        // Bukkit reports the clicked half's block location, so accept either half of the
        // same DoubleChest instead of requiring an exact single-block coordinate match.
        Block configuredBlock=configured.getBlock();
        if(!(configuredBlock.getState() instanceof org.bukkit.block.Chest configuredChest)||!(block.getState() instanceof org.bukkit.block.Chest clickedChest))return false;
        InventoryHolder configuredHolder=configuredChest.getInventory().getHolder();
        InventoryHolder clickedHolder=clickedChest.getInventory().getHolder();
        if(configuredHolder instanceof org.bukkit.block.DoubleChest configuredDouble&&containsChestLocation(configuredDouble,block.getLocation()))return true;
        return clickedHolder instanceof org.bukkit.block.DoubleChest clickedDouble&&containsChestLocation(clickedDouble,configured);
    }
    private boolean containsChestLocation(org.bukkit.block.DoubleChest chest,Location location){
        return chestSideAt(chest.getLeftSide(),location)||chestSideAt(chest.getRightSide(),location);
    }
    private boolean chestSideAt(InventoryHolder holder,Location location){
        return holder instanceof org.bukkit.block.Chest chest&&LocationStore.encode(chest.getLocation()).equals(LocationStore.encode(location));
    }
    @EventHandler public void onSneak(PlayerToggleSneakEvent e){Player player=e.getPlayer();UUID id=player.getUniqueId();boolean practice=state==GameState.WAITING&&repairingHeart.containsKey(id)&&isPracticeHeartKey(repairingHeart.get(id));if(state!=GameState.RUNNING&&!practice)return;if(!practice&&!players.contains(id))return;long now=System.currentTimeMillis();if(!e.isSneaking()){sneakStartedAt.remove(id);return;}sneakStartedAt.put(id,now);parrySneakAt.put(id,now);if(repairingHeart.containsKey(id)&&skillChecks.containsKey(id))resolveSkillCheck(player);if(tagTerraAttuningGem.containsKey(id)&&tagTerraSkillChecks.containsKey(id))resolveTerraGemSkillCheck(player);if(hasPassive(id,PassiveSkill.QUICK_TURN)&&player.isSprinting()){Vector movement=player.getVelocity().setY(0),look=player.getEyeLocation().getDirection().setY(0);if(movement.lengthSquared()>.01&&look.lengthSquared()>.01&&Math.abs(movement.normalize().dot(look.normalize()))<.45&&technicalReady(id,"QUICK_TURN",getConfig().getLong("technical-passives.quick-turn-cooldown-seconds",12))){player.setVelocity(look.multiply(getConfig().getDouble("technical-passives.quick-turn-velocity",.9)).setY(.18));player.getWorld().spawnParticle(Particle.CLOUD,player.getLocation(),12,.25,.1,.25,.04);player.sendActionBar(cc("&b急転 &7――サイドステップ"));}}}
    private boolean technicalReady(UUID id,String key,long cooldownSeconds){long now=System.currentTimeMillis();String mapKey=id+":"+key;if(now<technicalPassiveReadyAt.getOrDefault(mapKey,0L))return false;technicalPassiveReadyAt.put(mapKey,now+Math.max(1,cooldownSeconds)*1000L);return true;}
    private boolean updateDesperateState(Player player,long now){String key=player.getUniqueId()+":DESPERATE";Long until=timedSkillUntil.get(key);if(until==null)return false;if(now<until)return true;timedSkillUntil.remove(key);stamina.put(player.getUniqueId(),0.0);player.setFoodLevel(0);player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,100,1,false,true));player.sendActionBar(cc("&c決死行終了 &7――スタミナ枯渇"));return false;}
    private void clearFakeNoises(){for(UUID id:new HashSet<>(fakeNoiseUntil.keySet())){Entity entity=Bukkit.getEntity(id);if(entity!=null)entity.remove();}fakeNoiseUntil.clear();}
    @EventHandler public void onMove(PlayerMoveEvent e){if(e.getTo()==null)return;if(isPvMode()&&state==GameState.RUNNING&&pvFrozen&&participants.contains(e.getPlayer().getUniqueId())){e.setTo(e.getFrom());return;}updateOniDomainMovement(e.getPlayer());if(state==GameState.WAITING){enforceLobbyBoundary(e.getPlayer(),e.getTo());boolean was=isInTrainingArea(e.getFrom()),now=isInTrainingArea(e.getTo());if(!was&&now){BukkitTask pending=trainingLeaveTasks.remove(e.getPlayer().getUniqueId());if(pending!=null)pending.cancel();enterTraining(e.getPlayer());}else if(was&&!now){UUID id=e.getPlayer().getUniqueId();BukkitTask old=trainingLeaveTasks.remove(id);if(old!=null)old.cancel();trainingLeaveTasks.put(id,Bukkit.getScheduler().runTaskLater(this,()->{trainingLeaveTasks.remove(id);Player q=Bukkit.getPlayer(id);if(q!=null&&!isInTrainingArea(q.getLocation()))leaveTraining(q);},Math.max(1,getConfig().getLong("training.leave-delay-ticks",20L))));}return;}if(state!=GameState.RUNNING||!participants.contains(e.getPlayer().getUniqueId()))return;Player p=e.getPlayer();if(isTagMode()){discoverTagAreas(p);UUID tid=p.getUniqueId();if(tagParkourPlayers.contains(tid)){Location goal=LocationStore.get(getConfig(),"tag-mode.parkour.goal");if(goal!=null&&goal.getWorld()!=null&&goal.getWorld().equals(p.getWorld())&&p.getLocation().distanceSquared(goal)<=Math.pow(Math.max(1,getConfig().getDouble("tag-mode.parkour.goal-radius",2.5)),2)){tagParkourPlayers.remove(tid);tagParkourGrandfathered.remove(tid);tagLives.put(tid,tagMaxLives(tid));Location back=LocationStore.get(getConfig(),"tag-mode.respawn");if(back==null)back=LocationStore.get(getConfig(),"tag-mode.shrine.player");if(back!=null)p.teleport(back);p.setGameMode(GameMode.ADVENTURE);p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);p.setHealth(10);playerHitGraceUntil.put(tid,System.currentTimeMillis()+Math.max(1,getConfig().getLong("tag-mode.respawn-protection-seconds",5))*1000L);p.getInventory().setItem(8,tagNavCompass());p.sendTitle(cc("&a&l復帰"),cc("&f鬼ごっこへ戻った！"),5,30,10);}}}Location from=e.getFrom(),to=e.getTo();double horizontal=Math.pow(to.getX()-from.getX(),2)+Math.pow(to.getZ()-from.getZ(),2);if(horizontal<0.0001)return;long now=System.currentTimeMillis();if(p.getGameMode()!=GameMode.CREATIVE&&p.getGameMode()!=GameMode.SPECTATOR&&p.isSprinting()&&to.getY()-from.getY()>0.12&&now-dashJumpPenaltyAt.getOrDefault(p.getUniqueId(),0L)>700){dashJumpPenaltyAt.put(p.getUniqueId(),now);double value=Math.max(0,stamina.getOrDefault(p.getUniqueId(),20.0)-getConfig().getDouble("stamina.dash-jump-cost",5.0));stamina.put(p.getUniqueId(),value);p.setFoodLevel((int)Math.ceil(value));p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,Math.max(0,getConfig().getInt("stamina.dash-jump-slow-ticks",20)),0,false,false));if(value<=0)p.setSprinting(false);p.sendActionBar(cc("&cダッシュジャンプ &7――スタミナを大きく消費"));return;}if(!getConfig().getBoolean("auto-step.enabled",true)||Math.abs(to.getY()-from.getY())>0.08||!p.isOnGround())return;Vector direction=new Vector(to.getX()-from.getX(),0,to.getZ()-from.getZ());if(direction.lengthSquared()<0.0001)return;direction.normalize();Location probe=to.clone().add(direction.multiply(getConfig().getDouble("auto-step.probe-distance",0.45)));Block obstacle=probe.getWorld().getBlockAt(probe.getBlockX(),from.getBlockY(),probe.getBlockZ());double height=obstacle.getBoundingBox().getHeight();if(!obstacle.getType().isSolid()||height<0.99||height>1.01||!obstacle.getRelative(0,1,0).isPassable()||!obstacle.getRelative(0,2,0).isPassable())return;Location stepped=to.clone();stepped.setX(probe.getX());stepped.setY(obstacle.getY()+1.0);stepped.setZ(probe.getZ());e.setTo(stepped);p.setFallDistance(0);}
    @EventHandler public void onTechnicalMovement(PlayerMoveEvent e){if(state!=GameState.RUNNING||e.getTo()==null||!players.contains(e.getPlayer().getUniqueId()))return;Vector current=e.getTo().toVector().subtract(e.getFrom().toVector()).setY(0);if(current.lengthSquared()<.002)return;UUID id=e.getPlayer().getUniqueId();if(e.getPlayer().isSneaking()&&hasPassive(id,PassiveSkill.SILENT_BREATH))sneakStartedAt.put(id,System.currentTimeMillis());current.normalize();Vector previous=lastMoveDirection.put(id,current.clone());if(previous!=null&&previous.dot(current)<-.65)reversalAt.put(id,System.currentTimeMillis());}
    @EventHandler(priority=EventPriority.HIGH) public void onOniDomainFlight(PlayerToggleFlightEvent e){
        Player p=e.getPlayer(); UUID id=p.getUniqueId();
        if(!isShikkiDomainUser(p))return;
        e.setCancelled(true); p.setFlying(false);
        if(!isInsideOniDomain(p.getLocation())){p.setAllowFlight(false);return;}
        Vector wall=nearbyWallNormal(p.getLocation());
        if(wall!=null&&shikkiDomainWallKicks.getOrDefault(id,0)<Math.max(1,getConfig().getInt("oni-domain.shikki.max-wall-kicks",3))){
            int used=shikkiDomainWallKicks.merge(id,1,Integer::sum);
            Vector look=p.getEyeLocation().getDirection().setY(0);if(look.lengthSquared()<.01)look=wall.clone();else look.normalize();
            Vector v=wall.clone().multiply(getConfig().getDouble("oni-domain.shikki.wall-kick-away",.95)).add(look.multiply(.55));v.setY(getConfig().getDouble("oni-domain.shikki.wall-kick-up",.82));
            p.setVelocity(v);p.setFallDistance(0);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),18,.25,.35,.25,.05);p.playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.7f,1.8f);p.sendActionBar(cc("&c狩場 &7――壁蹴り &f"+used+"/&f"+getConfig().getInt("oni-domain.shikki.max-wall-kicks",3)));return;
        }
        if(shikkiDomainAirJumps.getOrDefault(id,0)>=1){p.sendActionBar(cc("&c狩場 &7――二段跳躍は着地で回復"));return;}
        shikkiDomainAirJumps.put(id,1);Vector d=p.getEyeLocation().getDirection();Vector h=new Vector(d.getX(),0,d.getZ());if(h.lengthSquared()<.01)h=new Vector(0,0,1);h.normalize().multiply(getConfig().getDouble("oni-domain.shikki.double-jump-forward",.85));h.setY(getConfig().getDouble("oni-domain.shikki.double-jump-up",.72));p.setVelocity(h);p.setFallDistance(0);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),16,.25,.15,.25,.04);p.playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.65f,1.55f);p.sendActionBar(cc("&c狩場 &7――二段跳躍"));
    }
    private Vector nearbyWallNormal(Location l){for(Vector d:new Vector[]{new Vector(1,0,0),new Vector(-1,0,0),new Vector(0,0,1),new Vector(0,0,-1)}){Location q=l.clone().add(d.clone().multiply(.65));if(q.getBlock().getType().isSolid()||q.clone().add(0,1,0).getBlock().getType().isSolid())return d.clone().multiply(-1);}return null;}

    @EventHandler public void onPassiveLeap(PlayerToggleFlightEvent e){Player p=e.getPlayer();UUID id=p.getUniqueId();if(state!=GameState.RUNNING||!players.contains(id)||!hasPassive(id,PassiveSkill.LEAP)||p.getGameMode()==GameMode.SPECTATOR)return;e.setCancelled(true);p.setFlying(false);long now=System.currentTimeMillis(),readyAt=passiveLeapReadyAt.getOrDefault(id,0L);if(now<readyAt){p.setAllowFlight(false);return;}p.setAllowFlight(false);passiveLeapReadyAt.put(id,now+Math.max(1,getConfig().getLong("passive-skills.leap-cooldown-seconds",10))*1000L);Vector facing=p.getEyeLocation().getDirection();Vector horizontal=new Vector(facing.getX(),0,facing.getZ());if(horizontal.lengthSquared()<0.0001)horizontal=new Vector(0,0,1);horizontal.normalize().multiply(getConfig().getDouble("passive-skills.leap-forward-velocity",1.25));horizontal.setY(getConfig().getDouble("passive-skills.leap-upward-velocity",0.85));p.setVelocity(horizontal);p.setFallDistance(0);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),22,.35,.12,.35,.05);p.playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.65f,1.55f);p.sendActionBar(cc("&b跳躍 &7――前方へ大きく跳んだ &8(CT "+getConfig().getLong("passive-skills.leap-cooldown-seconds",10)+"秒)"));}
    private boolean isInLobbyArea(Location location){if(location==null)return false;Location a=LocationStore.get(getConfig(),"locations.lobby-area-pos1"),b=LocationStore.get(getConfig(),"locations.lobby-area-pos2");if(a==null||b==null||a.getWorld()!=location.getWorld()||b.getWorld()!=location.getWorld())return true;double margin=Math.max(0,getConfig().getDouble("lobby-boundary.margin",2.5)),ym=Math.max(0,getConfig().getDouble("lobby-boundary.y-margin",12.0));double minX=Math.min(a.getX(),b.getX())-margin,maxX=Math.max(a.getX(),b.getX())+1+margin,minY=Math.min(a.getY(),b.getY())-ym,maxY=Math.max(a.getY(),b.getY())+1+ym,minZ=Math.min(a.getZ(),b.getZ())-margin,maxZ=Math.max(a.getZ(),b.getZ())+1+margin;return location.getX()>=minX&&location.getX()<maxX&&location.getY()>=minY&&location.getY()<maxY&&location.getZ()>=minZ&&location.getZ()<maxZ;}
    private void enforceLobbyBoundary(Player p,Location to){if(state!=GameState.WAITING||p==null||isEffectiveMapMaker(p)||isInTrainingArea(to)||isInLobbyArea(to))return;Location lobby=LocationStore.get(getConfig(),"locations.lobby");if(lobby==null||lobby.getWorld()==null)return;msg(p,"&cロビーエリアの外には移動できません。 &7ロビーへ戻ります。");p.teleport(lobby);p.setFallDistance(0);}
    private boolean isInTrainingArea(Location location){if(location==null||state!=GameState.WAITING||!getConfig().getBoolean("training.enabled",true))return false;Location a=LocationStore.get(getConfig(),"locations.training-pos1"),b=LocationStore.get(getConfig(),"locations.training-pos2");if(a==null||b==null||a.getWorld()!=location.getWorld()||b.getWorld()!=location.getWorld())return false;double minX=Math.min(a.getX(),b.getX()),maxX=Math.max(a.getX(),b.getX())+1,minY=Math.min(a.getY(),b.getY())-getConfig().getDouble("training.y-margin",8.0),maxY=Math.max(a.getY(),b.getY())+1+getConfig().getDouble("training.y-margin",8.0),minZ=Math.min(a.getZ(),b.getZ()),maxZ=Math.max(a.getZ(),b.getZ())+1;return location.getX()>=minX&&location.getX()<maxX&&location.getY()>=minY&&location.getY()<maxY&&location.getZ()>=minZ&&location.getZ()<maxZ;}
    private void enterTraining(Player p){if(!trainingPlayers.add(p.getUniqueId()))return;applyTrainingPassives(p);giveTrainingItems(p);msg(p,"&bトレーニングエリアに入りました。 &7選択中のぷれいやー/鬼パッシブ有効・鬼スキル全解放");Location dl=LocationStore.get(getConfig(),"locations.training-dummy");if(dl!=null&&(trainingDummy==null||Bukkit.getEntity(trainingDummy)==null))spawnTrainingDummy(dl);}
    private void leaveTraining(Player p){trainingPlayers.remove(p.getUniqueId());removeTrainingItems(p);cooldowns.keySet().removeIf(k->k.startsWith(p.getUniqueId()+":"));resetTrainingPassiveStats(p);msg(p,"&7トレーニングエリアを退出しました。");}
    private void applyTrainingPassives(Player p){
        resetTrainingPassiveStats(p);
        if(hasPassive(p.getUniqueId(),PassiveSkill.DURABILITY_BOOST)){double max=Math.max(20,getConfig().getDouble("passive-skills.durability-max-health",30));p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max);p.setHealth(Math.min(max,Math.max(p.getHealth(),max)));}
        if(hasPassive(p.getUniqueId(),PassiveSkill.NINJA_BLOOD))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.ninja-blood-walk-speed",0.23));
        else if(hasPassive(p.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))p.setWalkSpeed((float)getConfig().getDouble("passive-skills.divine-technique-walk-speed",0.21));
        stamina.put(p.getUniqueId(),20.0);p.setFoodLevel(20);
        if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null&&selectedOniPassives.contains(OniPassiveSkill.ANCHOR))p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(Math.max(0,Math.min(1,getConfig().getDouble("oni-passive-skills.anchor-knockback-resistance",.35))));
    }
    private void resetTrainingPassiveStats(Player p){
        if(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null){p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);if(p.getHealth()>20)p.setHealth(20);}
        if(p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)!=null)p.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
        p.setWalkSpeed((float)getConfig().getDouble("player-walk-speed",0.20));
    }
    private void giveTrainingItems(Player p){debugTrace("training","TRN-301","giveTrainingItems player="+p.getName()+" oni="+(selectedOniType==null?"DAKKO":selectedOniType.name()));removeTrainingItems(p);applyTrainingPassives(p);PlayerSkill sk=selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT);p.getInventory().addItem(playerSkillIconItem(sk,"&b&l[練習] "+sk.display,"training_skill:"+sk.name()));OniType type=selectedOniType==null?OniType.DAKKO:selectedOniType;if(isOniTypeLocked(type))type=OniType.DAKKO;if(type==OniType.DAKKO){p.getInventory().addItem(oniSkillIconItem("dakko_tp","&d&l[練習] 狐渡り","training_oni:dakko_tp"),oniSkillIconItem("dakko_clone","&6&l[練習] 分霊","training_oni:dakko_clone"),oniSkillIconItem("dakko_fox_fire","&6&l[練習] 狐火","training_oni:dakko_fox_fire"),oniSkillIconItem("dakko_heavenly_arrival","&d&l[練習] 天来","training_oni:dakko_heavenly_arrival"));}else if(type==OniType.KISHIN){p.getInventory().addItem(oniSkillIconItem("kishin_charge","&c&l[練習] 鬼突","training_oni:kishin_charge"),oniSkillIconItem("kishin_slam","&4&l[練習] 地砕","training_oni:kishin_slam"),oniSkillIconItem("kishin_roar","&4&l[練習] 鬼吼","training_oni:kishin_roar"),oniSkillIconItem("kishin_iron_body","&6&l[練習] 剛身","training_oni:kishin_iron_body"));}else if(type==OniType.SHIKKI){p.getInventory().addItem(oniSkillIconItem("shikki_frenzy","&c&l[練習] 狂奔","training_oni:shikki_frenzy"),oniSkillIconItem("shikki_blood_run","&c&l[練習] 血走","training_oni:shikki_blood_run"),oniSkillIconItem("shikki_hunting_leap","&6&l[練習] 跳躍狩り","training_oni:shikki_hunting_leap"),oniSkillIconItem("shikki_chain_hunt","&4&l[練習] 狩猟連鎖","training_oni:shikki_chain_hunt")); }else if(type==OniType.KANKI){p.getInventory().addItem(oniSkillIconItem("kanki_summon","&5&l[練習] 鬼喚び","training_oni:kanki_summon"),oniSkillIconItem("kanki_command","&d&l[練習] 鬼令","training_oni:kanki_command"),oniSkillIconItem("kanki_recall","&6&l[練習] 再臨","training_oni:kanki_recall"),oniSkillIconItem("kanki_parade","&4&l[練習] 百鬼夜行","training_oni:kanki_parade"));}else if(type==OniType.YUUKI){p.getInventory().addItem(oniSkillIconItem("yuuki_veil","&8&l[練習] 幽歩","training_oni:yuuki_veil"),oniSkillIconItem("yuuki_haze_step","&7&l[練習] 朧渡り","training_oni:yuuki_haze_step"),oniSkillIconItem("yuuki_shadow_bind","&5&l[練習] 影縫い","training_oni:yuuki_shadow_bind"),oniSkillIconItem("yuuki_divine_hide","&8&l[練習] 神隠し","training_oni:yuuki_divine_hide"));}else{p.getInventory().addItem(oniSkillIconItem("jakutsuki_black_mirror","&5&l[練習] 黒鏡","training_oni:jakutsuki_black_mirror"),oniSkillIconItem("jakutsuki_sweep","&5&l[練習] 薙ぎ払い","training_oni:jakutsuki_sweep"),oniSkillIconItem("jakutsuki_snakefall","&8&l[練習] 蛇崩","training_oni:jakutsuki_snakefall"),oniSkillIconItem("jakutsuki_release","&4&l[練習] 解放","training_oni:jakutsuki_release"),oniSkillIconItem("jakutsuki_piercing_blast","&5&l[練習] 黒の波動","training_oni:jakutsuki_piercing_blast"));}}
    private void removeTrainingItems(Player p){for(int i=0;i<p.getInventory().getSize();i++){ItemStack stack=p.getInventory().getItem(i);String action=actionOf(stack);if(action!=null&&(action.startsWith("training_skill:")||action.startsWith("training_oni:")||("passive:decoy".equals(action)&&stack.hasItemMeta()&&stack.getItemMeta().hasDisplayName()&&ChatColor.stripColor(stack.getItemMeta().getDisplayName()).contains("[練習]"))))p.getInventory().setItem(i,null);}}
    private void trainingLog(Player p,String text){if(getConfig().getBoolean("training.show-actionbar-log",true))p.sendActionBar(cc("&8[&bTRAINING&8] "+text));}
    private void spawnTrainingDummy(Location location){removeTrainingDummy();if(location==null||location.getWorld()==null)return;Husk dummy=location.getWorld().spawn(location,Husk.class);dummy.setAI(false);dummy.setSilent(true);dummy.setRemoveWhenFarAway(false);dummy.setCustomName(cc("&c&l鬼ダミー &7[TRAINING]"));dummy.setCustomNameVisible(true);dummy.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(200);dummy.setHealth(200);dummy.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"training_dummy");trainingDummy=dummy.getUniqueId();}
    private void removeTrainingDummy(){if(trainingDummy!=null){Entity e=Bukkit.getEntity(trainingDummy);if(e!=null)e.remove();trainingDummy=null;}}
    private boolean ready(Player p,String id,int cd){if(players.contains(p.getUniqueId())&&System.currentTimeMillis()<oniSkillSealUntil.getOrDefault(p.getUniqueId(),0L)){msg(p,"&5封殺 &7――スキルが封じられている。");return false;}boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());if(training)cd=Math.max(1,getConfig().getInt("training.cooldown-seconds",1));boolean oniSkill=isOniSkillAction(id)&&(training||(oni!=null&&isOni(p.getUniqueId())));if(oniSkill&&finalPhase)cd=Math.max(3,(int)Math.ceil(cd*finalCooldownMultiplier()));if(oniSkill&&hasOniPassive(OniPassiveSkill.MASTERY))cd=Math.max(1,(int)Math.ceil(cd*Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.mastery-cooldown-multiplier",.85)))));if(oniSkill&&hasOniPassive(OniPassiveSkill.TERRITORY)&&oniNearAliveHeart(p,getConfig().getDouble("oni-passive-skills.territory-radius",8.0)))cd=Math.max(1,(int)Math.ceil(cd*Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.territory-cooldown-multiplier",.80)))));String k=p.getUniqueId()+":"+id;long now=System.currentTimeMillis();long until=cooldowns.getOrDefault(k,0L);if(until>now){msg(p,"あと &e"+((until-now+999)/1000)+"秒 &f待ってください。");return false;}cooldowns.put(k,now+cd*1000L);if(oniSkill){recoverOniSkillMomentum();renderEmpoweredOniSkill(p);}return true;}
    private boolean readyAutomatic(Player p,String id,int cd){if(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))cd=Math.max(1,getConfig().getInt("training.cooldown-seconds",1));String k=p.getUniqueId()+":"+id;long now=System.currentTimeMillis();if(cooldowns.getOrDefault(k,0L)>now)return false;cooldowns.put(k,now+cd*1000L);return true;}
    private void usePlayerSkill(Player p,PlayerSkill s){boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());if(s==PlayerSkill.HEAL){if(training&&p.getHealth()>=p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()-.01)p.setHealth(Math.max(1,p.getHealth()-8));startHealing(p,true);return;}if(s==PlayerSkill.SAFE_LANDING){msg(p,"&e安定着地 &fは落下ダメージが発生する着地時に自動発動します。");return;}if(!training&&s==PlayerSkill.DESPERATE_RUN&&p.getHealth()>p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*.30){msg(p,"&c決死行は体力30%以下でのみ使用できます。");return;}if(!training&&s==PlayerSkill.RESONANCE&&stamina.getOrDefault(p.getUniqueId(),20.0)<6){msg(p,"&c共鳴にはスタミナが6必要です。");return;}if(training&&s==PlayerSkill.RESONANCE)stamina.put(p.getUniqueId(),20.0);if(!ready(p,s.name(),s.cooldown))return;if(isTagMode())renderTagPlayerSkillVfx(p,s);switch(s){case SPRINT->p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(1,getConfig().getInt("skills.sprint-duration-ticks",100)),Math.max(0,getConfig().getInt("skills.sprint-amplifier",2)),false,true));case INVISIBLE->p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,120,0,false,true));case SMOKE->{p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,p.getLocation(),160,4,2,4,.03);LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<64){boolean resist=consumeGrudgeResistance(p.getUniqueId());o.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,resist?30:80,0,false,true));registerOniGrudge(p.getUniqueId());if(resist&&o instanceof Player op)op.sendActionBar(cc("&5怨返し &7――煙幕への耐性を発動"));}}case ONI_STRIKE->{LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<=25){boolean resist=consumeGrudgeResistance(p.getUniqueId());double damage=heartGoalReached()?12:3;o.damage(damage,p);o.setVelocity(o.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(resist?.65:1.5).setY(resist?.2:.5));registerOniGrudge(p.getUniqueId());if(resist&&o instanceof Player op)op.sendActionBar(cc("&5怨返し &7――破鬼撃への耐性を発動"));}else msg(p,"鬼が5ブロック以内にいません。");}case OBSESSION->{int ticks=Math.max(1,getConfig().getInt("skills.obsession-glow-seconds",8))*20;p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));LivingEntity o=getOniEntity();if(o!=null)o.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,0.8f,1.25f);p.sendActionBar(cc("&e執念 &7――鬼の輪郭を捉えた"));}case BLINK->useBlink(p);case ECHO->useEcho(p);case CLAIRVOYANCE->useClairvoyance(p);case UNYIELDING->{timedSkillUntil.put(p.getUniqueId()+":UNYIELDING",System.currentTimeMillis()+6000);p.sendTitle("",cc("&6&l不退転 &f6秒"),0,25,5);}case SEALING_CIRCLE->{sealingCircles.put(p.getUniqueId(),p.getLocation().clone());sealingCircleUntil.put(p.getUniqueId(),System.currentTimeMillis()+10000);p.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE,p.getLocation(),80,2,.2,2,.1);p.sendActionBar(cc("&9封鬼陣 &7――足元へ結界を設置"));}case RESONANCE->useResonance(p);case SUBSTITUTE->useSubstitute(p);case DESPERATE_RUN->{timedSkillUntil.put(p.getUniqueId()+":DESPERATE",System.currentTimeMillis()+8000);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,160,1,false,true));p.sendTitle("",cc("&c&l決死行 &f8秒"),0,25,5);}case BUGEI->useBugei(p);case HEAL,SAFE_LANDING->{}}}

    // v0.40.54 鬼ごっこモード専用: 大人数でも視認性を潰しにくい短時間・高密度VFX。
    private void renderTagPlayerSkillVfx(Player p, PlayerSkill s){
        if(p==null||!p.isOnline()||!isTagMode())return;
        World w=p.getWorld(); Location c=p.getLocation().clone().add(0,.15,0);
        switch(s){
            case SPRINT -> { tagRing(w,c,1.15,Particle.CLOUD,22); w.spawnParticle(Particle.SWEEP_ATTACK,c.clone().add(0,.8,0),5,.35,.45,.35,0); w.playSound(c,Sound.ENTITY_PLAYER_ATTACK_SWEEP,.75f,1.55f); tagTrail(p,Particle.CLOUD,18,2); }
            case INVISIBLE -> { tagRing(w,c.clone().add(0,1,0),.9,Particle.PORTAL,28); w.spawnParticle(Particle.REVERSE_PORTAL,c.clone().add(0,1,0),35,.35,.8,.35,.08); w.playSound(c,Sound.BLOCK_AMETHYST_BLOCK_CHIME,.75f,1.65f); }
            case SMOKE -> { tagRing(w,c,2.3,Particle.CLOUD,34); w.spawnParticle(Particle.EXPLOSION_NORMAL,c.clone().add(0,.7,0),20,1.2,.45,1.2,.08); w.playSound(c,Sound.ENTITY_FIREWORK_ROCKET_BLAST,.8f,.75f); }
            case ONI_STRIKE -> { tagRing(w,c.clone().add(0,.7,0),1.6,Particle.CRIT_MAGIC,30); w.spawnParticle(Particle.SWEEP_ATTACK,c.clone().add(0,1,0),8,.6,.5,.6,0); w.playSound(c,Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,.85f,1.5f); }
            case HEAL -> { tagRing(w,c,1.2,Particle.HEART,16); w.spawnParticle(Particle.VILLAGER_HAPPY,c.clone().add(0,1,0),22,.45,.7,.45,.05); }
            case OBSESSION, CLAIRVOYANCE -> { tagRing(w,c,2.0,Particle.SOUL_FIRE_FLAME,30); w.spawnParticle(Particle.END_ROD,c.clone().add(0,1,0),18,.7,.7,.7,.03); }
            case BLINK -> { tagRing(w,c,1.0,Particle.PORTAL,30); w.spawnParticle(Particle.END_ROD,c.clone().add(0,1,0),20,.25,.6,.25,.06); w.playSound(c,Sound.ENTITY_ENDERMAN_TELEPORT,.9f,1.7f); }
            case ECHO -> { tagRing(w,c,1.5,Particle.END_ROD,28); w.spawnParticle(Particle.PORTAL,c.clone().add(0,1,0),38,.65,.8,.65,.08); w.playSound(c,Sound.ENTITY_ALLAY_AMBIENT_WITH_ITEM,.8f,1.35f); }
            case UNYIELDING -> { tagRing(w,c,1.5,Particle.FLAME,30); w.spawnParticle(Particle.CRIT,c.clone().add(0,1,0),28,.55,.8,.55,.12); w.playSound(c,Sound.ENTITY_WARDEN_HEARTBEAT,.9f,1.35f); tagTrail(p,Particle.FLAME,20,3); }
            case SEALING_CIRCLE -> { tagRing(w,c,2.6,Particle.ENCHANTMENT_TABLE,48); tagRing(w,c,1.5,Particle.END_ROD,30); w.playSound(c,Sound.BLOCK_BEACON_ACTIVATE,.8f,1.6f); }
            case RESONANCE -> { tagRing(w,c,3.0,Particle.END_ROD,44); w.spawnParticle(Particle.FIREWORKS_SPARK,c.clone().add(0,1,0),28,1.2,.7,1.2,.05); w.playSound(c,Sound.BLOCK_AMETHYST_CLUSTER_HIT,.8f,1.45f); }
            case SUBSTITUTE -> { tagRing(w,c,1.2,Particle.PORTAL,32); w.spawnParticle(Particle.CLOUD,c.clone().add(0,1,0),20,.4,.8,.4,.06); }
            case DESPERATE_RUN -> { tagRing(w,c,1.8,Particle.FLAME,34); w.spawnParticle(Particle.LAVA,c.clone().add(0,.8,0),10,.55,.45,.55,.02); w.playSound(c,Sound.ENTITY_BLAZE_SHOOT,.75f,1.45f); tagTrail(p,Particle.CRIT,30,2); }
            case BUGEI -> { tagRing(w,c,2.0,Particle.CRIT_MAGIC,40); w.spawnParticle(Particle.SWEEP_ATTACK,c.clone().add(0,1,0),10,.8,.6,.8,0); w.playSound(c,Sound.ENTITY_PLAYER_ATTACK_CRIT,1f,.8f); }
            case SAFE_LANDING -> { }
        }
    }
    private void tagRing(World w,Location c,double radius,Particle particle,int points){
        for(int i=0;i<points;i++){double a=Math.PI*2*i/points;w.spawnParticle(particle,c.clone().add(Math.cos(a)*radius,0,Math.sin(a)*radius),1,0,0,0,0);}
    }
    private void tagTrail(Player p,Particle particle,int ticks,int every){
        final int[] t={0};final BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{if(!p.isOnline()||state!=GameState.RUNNING||!isTagMode()||t[0]>=ticks){task[0].cancel();return;}Location l=p.getLocation().clone().add(0,.15,0);p.getWorld().spawnParticle(particle,l,4,.22,.08,.22,.015);t[0]+=every;},0L,Math.max(1,every));
    }
    private void showBugeiSlotCinematic(Player p,UUID id){
        if(p==null)return;
        int step=Math.max(2,getConfig().getInt("skills.bugei.slot-step-ticks",4));
        // Presentation only: never random. Always stops at 跳躍 / 武芸 / キック.
        // Keep the center text compact by using only the smaller subtitle line.
        p.sendTitle("",cc("&e跳躍 &8│ &7・・・ &8│ &7・・・"),0,step+2,0);
        p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.32f,1.75f);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            p.sendTitle("",cc("&e跳躍 &8│ &6武芸 &8│ &7・・・"),0,step+2,0);
            p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.36f,1.55f);
        },step);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            p.sendTitle("",cc("&e跳躍 &8│ &6武芸 &8│ &cキック"),0,step+4,1);
            p.playSound(p.getLocation(),Sound.BLOCK_ANVIL_LAND,.42f,1.32f);
            p.playSound(p.getLocation(),Sound.ENTITY_PLAYER_LEVELUP,.28f,1.85f);
        },step*2L);
    }
    private void useBugei(Player p){
        LivingEntity target=getOniEntity();
        double range=Math.max(3.0,getConfig().getDouble("skills.bugei.target-range",18.0));
        if(target==null||target.getWorld()!=p.getWorld()||target.getLocation().distanceSquared(p.getLocation())>range*range){msg(p,"&c武芸 &f――鬼が"+(int)range+"m以内にいません。");return;}
        UUID id=p.getUniqueId();bugeiActive.add(id);p.setFallDistance(0);
        p.setVelocity(new Vector(0,getConfig().getDouble("skills.bugei.launch-upward-velocity",1.05),0));
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.75f,1.45f);
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),24,.35,.15,.35,.08);
        showBugeiSlotCinematic(p,id);
        int slotDelay=Math.max(6,getConfig().getInt("skills.bugei.slot-step-ticks",4)*3);
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(!p.isOnline()||!bugeiActive.contains(id))return;
            final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
            task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
                ticks[0]++;LivingEntity oniEntity=getOniEntity();
                boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());
                if((state!=GameState.RUNNING&&!training)||!p.isOnline()||oniEntity==null||oniEntity.getWorld()!=p.getWorld()){
                    bugeiActive.remove(id);task[0].cancel();return;
                }
                Vector to=oniEntity.getEyeLocation().toVector().subtract(p.getLocation().toVector());
                double distance=to.length();
                double speed=getConfig().getDouble("skills.bugei.dive-speed",1.75);
                double boostDistance=getConfig().getDouble("skills.bugei.final-boost-distance",5.0);
                if(distance<=boostDistance)speed*=getConfig().getDouble("skills.bugei.final-boost-multiplier",1.35);
                if(distance>0.01)p.setVelocity(to.normalize().multiply(speed));
                p.setFallDistance(0);

                Location trail=p.getLocation().add(0,.85,0);
                Vector back=p.getVelocity().clone();
                if(back.lengthSquared()>.001)back.normalize().multiply(-.35);
                int density=Math.max(4,getConfig().getInt("skills.bugei.trail-density",10));
                for(int i=0;i<density;i++){
                    double spread=(i/(double)density)*1.6;
                    Location pt=trail.clone().add(back.clone().multiply(spread));
                    p.getWorld().spawnParticle(Particle.REDSTONE,pt,1,0,0,0,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,45,20),1.25f));
                    if(i%2==0)p.getWorld().spawnParticle(Particle.REDSTONE,pt.clone().add(0,.06,0),1,0,0,0,0,new Particle.DustOptions(org.bukkit.Color.fromRGB(255,190,35),1.0f));
                }
                p.getWorld().spawnParticle(Particle.CRIT,trail,4,.12,.12,.12,.02);
                p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),2,.08,.08,.08,.01);
                if(distance<=boostDistance){
                    p.getWorld().spawnParticle(Particle.FLAME,trail,3,.12,.12,.12,.015);
                    if(ticks[0]%4==0)p.playSound(p.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,.35f,1.8f);
                }
                if(distance<=getConfig().getDouble("skills.bugei.impact-distance",2.2)){
                    double damage=heartGoalReached()?getConfig().getDouble("skills.bugei.final-damage",10.0):getConfig().getDouble("skills.bugei.sealed-damage",6.0);
                    oniEntity.damage(damage,p);
                    Vector push=oniEntity.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=p.getEyeLocation().getDirection();oniEntity.setVelocity(push.normalize().multiply(getConfig().getDouble("skills.bugei.knockback",1.35)).setY(.55));
                    Location impact=oniEntity.getLocation().add(0,1,0);
                    p.getWorld().playSound(impact,Sound.ENTITY_PLAYER_ATTACK_CRIT,1.0f,.68f);
                    p.getWorld().playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,.55f,1.55f);
                    p.getWorld().playSound(impact,Sound.BLOCK_ANVIL_LAND,.32f,1.75f);
                    p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,impact,2,.15,.15,.15,.01);
                    p.getWorld().spawnParticle(Particle.CRIT_MAGIC,impact,30,.55,.65,.55,.10);
                    p.getWorld().spawnParticle(Particle.FLAME,impact,18,.45,.45,.45,.05);

                    int ring=Math.max(18,getConfig().getInt("skills.bugei.impact-ring-particles",42));
                    for(int i=0;i<ring;i++){
                        double a=(Math.PI*2.0*i)/ring;
                        double radius=1.7;
                        Location r=impact.clone().add(Math.cos(a)*radius,-.55,Math.sin(a)*radius);
                        Particle.DustOptions dust=(i%2==0)
                            ?new Particle.DustOptions(org.bukkit.Color.fromRGB(255,35,15),1.35f)
                            :new Particle.DustOptions(org.bukkit.Color.fromRGB(255,205,40),1.15f);
                        p.getWorld().spawnParticle(Particle.REDSTONE,r,1,0,0,0,0,dust);
                    }

                    p.setVelocity(p.getVelocity().multiply(-.12).setY(.46));p.setFallDistance(0);
                    p.sendTitle("",cc("&6&lライダーキック &8- &eRIDER KICK"),0,14,5);
                    p.sendActionBar(cc("&6武芸 &f――鬼へ強烈な一撃！"));
                    bugeiActive.remove(id);task[0].cancel();return;
                }
                if(ticks[0]>=Math.max(8,getConfig().getInt("skills.bugei.max-dive-ticks",32))){
                    bugeiActive.remove(id);p.setFallDistance(0);task[0].cancel();p.sendActionBar(cc("&7武芸――攻撃は届かなかった"));
                }
            },0L,1L);
        },slotDelay);
    }

    private void useBlink(Player p){Vector direction=p.getVelocity().setY(0);if(direction.lengthSquared()<.02)direction=p.getEyeLocation().getDirection().setY(0);if(direction.lengthSquared()<.01)return;direction.normalize();Location destination=p.getLocation().clone();for(int i=0;i<8;i++){Location next=destination.clone().add(direction.clone().multiply(.5));if(!next.getBlock().isPassable()||!next.clone().add(0,1,0).getBlock().isPassable())break;destination=next;}p.teleport(destination);double value=Math.max(0,stamina.getOrDefault(p.getUniqueId(),20.0)-4);stamina.put(p.getUniqueId(),value);p.setFoodLevel((int)Math.ceil(value));p.getWorld().spawnParticle(Particle.PORTAL,p.getLocation(),30,.3,.5,.3,.2);if(isTagMode()){tagRing(p.getWorld(),p.getLocation().clone().add(0,.2,0),1.25,Particle.END_ROD,26);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().clone().add(0,.6,0),14,.35,.25,.35,.04);}}
    private void useEcho(Player p){ArmorStand echo=p.getWorld().spawn(p.getLocation(),ArmorStand.class);echo.setVisible(false);echo.setArms(true);echo.setBasePlate(false);echo.setGravity(true);echo.setInvulnerable(true);echo.setCustomName("残響");ItemStack head=new ItemStack(Material.PLAYER_HEAD);SkullMeta skull=(SkullMeta)head.getItemMeta();skull.setOwningPlayer(p);head.setItemMeta(skull);echo.getEquipment().setHelmet(head);echo.getEquipment().setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));echo.getEquipment().setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));echo.getEquipment().setBoots(new ItemStack(Material.LEATHER_BOOTS));echo.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand().clone());Vector initialDirection=p.getEyeLocation().getDirection().setY(0);if(initialDirection.lengthSquared()<.01)initialDirection=new Vector(0,0,1);initialDirection.normalize();long durationMillis=15000;fakeNoiseUntil.put(echo.getUniqueId(),System.currentTimeMillis()+durationMillis);timedSkillUntil.put(p.getUniqueId()+":ECHO_SILENT",System.currentTimeMillis()+1000);Vector startDirection=initialDirection;new Runnable(){private int elapsedTicks=0,nextTurnTicks=20+(int)(Math.random()*31);private Vector direction=startDirection;@Override public void run(){if(!echo.isValid()||elapsedTicks>=300){fakeNoiseUntil.remove(echo.getUniqueId());if(echo.isValid())echo.remove();return;}Location here=echo.getLocation();Block feetAhead=here.clone().add(direction.clone().multiply(.7)).getBlock(),headAhead=feetAhead.getRelative(0,1,0);boolean blocked=feetAhead.getType().isSolid()||headAhead.getType().isSolid();if(blocked||elapsedTicks>=nextTurnTicks){double turn=blocked?(Math.random()<.5?-1:1)*(Math.PI*.55+Math.random()*.7):(Math.random()-.5)*1.25;double cos=Math.cos(turn),sin=Math.sin(turn);direction=new Vector(direction.getX()*cos-direction.getZ()*sin,0,direction.getX()*sin+direction.getZ()*cos).normalize();nextTurnTicks=elapsedTicks+20+(int)(Math.random()*41);}Vector velocity=echo.getVelocity();velocity.setX(direction.getX()*.22).setZ(direction.getZ()*.22);if(echo.isOnGround()&&(blocked||Math.random()<.018))velocity.setY(.36);echo.setVelocity(velocity);float yaw=(float)Math.toDegrees(Math.atan2(-direction.getX(),direction.getZ()));echo.setRotation(yaw,0);double swing=Math.sin(elapsedTicks*.65)*.7;echo.setRightArmPose(new EulerAngle(swing,0,0));echo.setLeftArmPose(new EulerAngle(-swing,0,0));echo.setRightLegPose(new EulerAngle(-swing,0,0));echo.setLeftLegPose(new EulerAngle(swing,0,0));if(elapsedTicks%10==0)echo.getWorld().playSound(echo.getLocation(),Sound.BLOCK_GRASS_STEP,.65f,1.1f);elapsedTicks+=2;Bukkit.getScheduler().runTaskLater(OniGamePlugin.this,this,2);}}.run();p.sendActionBar(cc("&d残響 &7――15秒間、偽の姿と走行音を放った"));}
    private void useClairvoyance(Player p){final BukkitTask[] task=new BukkitTask[1];final int[] count={0};task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{if((state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))||!p.isOnline()||count[0]++>=8){task[0].cancel();return;}Set<String> visibleHearts=new HashSet<>(heartHp.keySet());visibleHearts.addAll(fakeHeartKeys);for(String key:visibleHearts){Location l=LocationStore.decode(key);if(l!=null&&l.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.REDSTONE,l.clone().add(.5,1,.5),12,.3,.5,.3,0,new Particle.DustOptions(Color.RED,1.5f));}for(String key:lootChestKeys){Location l=LocationStore.decode(key);if(l!=null&&l.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.VILLAGER_HAPPY,l.clone().add(.5,1,.5),10,.3,.4,.3,.02);}if(state==GameState.WAITING){Location ph=LocationStore.get(getConfig(),"locations.practice-heart");if(ph!=null&&ph.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.REDSTONE,ph.clone().add(.5,1,.5),12,.3,.5,.3,0,new Particle.DustOptions(Color.RED,1.5f));Location pc=LocationStore.get(getConfig(),"locations.practice-loot-chest");if(pc!=null&&pc.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.VILLAGER_HAPPY,pc.clone().add(.5,1,.5),10,.3,.4,.3,.02);if(trainingDummy!=null&&Bukkit.getEntity(trainingDummy) instanceof LivingEntity dummy&&dummy.getWorld().equals(p.getWorld()))p.spawnParticle(Particle.SOUL_FIRE_FLAME,dummy.getLocation().add(0,2,0),10,.3,.4,.3,.02);}for(UUID id:players){Player ally=Bukkit.getPlayer(id);if(ally!=null&&!id.equals(p.getUniqueId())&&ally.getWorld().equals(p.getWorld())&&ally.getHealth()<ally.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue())p.spawnParticle(Particle.HEART,ally.getLocation().add(0,2,0),5,.2,.2,.2,.01);}if(heartGoalReached()&&activeExit!=null){p.spawnParticle(Particle.END_ROD,activeExit.clone().add(.5,1,.5),20,.5,1,.5,.03);if(activeExit2!=null)p.spawnParticle(Particle.END_ROD,activeExit2.clone().add(.5,1,.5),20,.5,1,.5,.03);}},0,20);}
    private void useResonance(Player p){UUID id=p.getUniqueId();double self=Math.max(0,stamina.getOrDefault(id,20.0)-6);stamina.put(id,self);double radius=64;Collection<UUID> resonanceTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID allyId:resonanceTargets){if(allyId.equals(id))continue;Player ally=Bukkit.getPlayer(allyId);if(ally!=null&&ally.getWorld().equals(p.getWorld())&&ally.getLocation().distanceSquared(p.getLocation())<=radius){double value=Math.min(20,stamina.getOrDefault(allyId,20.0)+3);stamina.put(allyId,value);ally.setFoodLevel((int)Math.ceil(value));ally.sendActionBar(cc("&d共鳴 &7――スタミナ回復"));}}for(UUID botId:playerBots)if(Bukkit.getEntity(botId) instanceof LivingEntity bot&&bot.getWorld().equals(p.getWorld())&&bot.getLocation().distanceSquared(p.getLocation())<=radius)playerBotStamina.put(botId,Math.min(20,playerBotStamina.getOrDefault(botId,20.0)+3));p.setFoodLevel((int)Math.ceil(self));p.getWorld().spawnParticle(Particle.END_ROD,p.getLocation(),50,4,1,4,.05);}
    private void useSubstitute(Player p){Collection<UUID> substituteTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;Player target=substituteTargets.stream().filter(id->!id.equals(p.getUniqueId())&&!dead.contains(id)&&!escaped.contains(id)).map(Bukkit::getPlayer).filter(Objects::nonNull).filter(q->q.getWorld().equals(p.getWorld())&&q.getLocation().distanceSquared(p.getLocation())<=225&&p.hasLineOfSight(q)).min(Comparator.comparingDouble(q->q.getLocation().distanceSquared(p.getLocation()))).orElse(null);if(target==null){msg(p,"&c身代わりにできる仲間が視線内15mにいません。");return;}Location first=p.getLocation().clone(),second=target.getLocation().clone();p.teleport(second);target.teleport(first);p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,60,0,false,false));target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,40,1,false,true));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,1,1);}
    private void useConsumable(Player p,String action,ItemStack stack){switch(action){case "item:sprint"->{p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,60,1,false,true));consumeOne(stack);}case "item:invisible"->{p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,60,0,false,true));consumeOne(stack);}case "item:smoke"->{p.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,p.getLocation(),80,2.5,1.5,2.5,.02);LivingEntity o=getOniEntity();if(o!=null&&o.getWorld().equals(p.getWorld())&&o.getLocation().distanceSquared(p.getLocation())<36)o.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,40,0,false,true));consumeOne(stack);}case "item:strike"->{LivingEntity o=getOniEntity();if(o==null||!o.getWorld().equals(p.getWorld())||o.getLocation().distanceSquared(p.getLocation())>16){msg(p,"鬼が4ブロック以内にいません。");return;}o.damage(heartGoalReached()?7:2,p);o.setVelocity(o.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(.8).setY(.25));consumeOne(stack);}case "item:heal"->startHealing(p,false);}}
    private void useFlareGun(Player player){
        if(!ready(player,"equipment_flare_gun",Math.max(1,getConfig().getInt("equipment.flare-gun-cooldown-seconds",15))))return;
        consumeEquipmentUse(player,player.getInventory().getItemInMainHand(),"equipment:flare_gun");
        Snowball flare=player.launchProjectile(Snowball.class);flare.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(1.45));flare.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"equipment:flare_projectile");
        flare.setGlowing(true);player.getWorld().playSound(player.getLocation(),Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,.9f,1.15f);player.sendActionBar(cc("&cフレアガン &7――照明弾を発射"));
    }
    @EventHandler public void onFlareHit(ProjectileHitEvent e){
        String action=e.getEntity().getPersistentDataContainer().get(actionKey,PersistentDataType.STRING);if(!"equipment:flare_projectile".equals(action))return;
        Location hit=e.getEntity().getLocation();World world=hit.getWorld();if(world==null)return;
        world.spawnParticle(Particle.FIREWORKS_SPARK,hit,70,1.3,1.3,1.3,.12);world.spawnParticle(Particle.END_ROD,hit,30,.8,.8,.8,.06);world.playSound(hit,Sound.ENTITY_FIREWORK_ROCKET_BLAST,1.2f,1.1f);
        LivingEntity oniEntity=getOniEntity();double radius=Math.max(1,getConfig().getDouble("equipment.flare-gun-reveal-radius",8.0));if(oniEntity!=null&&oniEntity.getWorld().equals(world)&&oniEntity.getLocation().distanceSquared(hit)<=radius*radius){int ticks=Math.max(1,getConfig().getInt("equipment.flare-gun-glow-seconds",5))*20;oniEntity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));}
        e.getEntity().remove();
    }
    private void useOniSkill(Player p,String action){
        boolean training=state==GameState.WAITING&&trainingPlayers.contains(p.getUniqueId())&&isInTrainingArea(p.getLocation());
        int heartPenalty=training?0:brokenHearts*3;
        int unlock=oniSkillUnlockHearts(action);
        // 練習場では心臓破壊数に関係なく、選択中の鬼の全スキルを試せる。
        if(!training&&unlock>0&&!oniSkillUnlocked(action)){msg(p,"&7この能力は心臓が &c"+unlock+"個 &7破壊されると解放されます。");return;}
        switch(action){
            case "dakko_tp"->{if(p.isSneaking()&&tryDakkoCloneSwap(p))return;int baseCd=Math.max(8,getConfig().getInt("dakko-skills.fox-walk.cooldown-seconds",16));if(!ready(p,action,baseCd+heartPenalty))return;int range=Math.max(8,getConfig().getInt("dakko-skills.fox-walk.range",30)-brokenHearts*2);Block b=p.getTargetBlockExact(range);if(b!=null){Location from=p.getLocation().clone(),to=b.getLocation().add(.5,1,.5).setDirection(p.getLocation().getDirection());renderDakkoFoxWalkTrail(p,from,to);p.teleport(to);p.setFallDistance(0);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("dakko-skills.fox-walk.after-speed-ticks",60)),1,false,true));p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,p.getLocation().add(0,1,0),28,.45,.7,.45,.05);}}
            case "dakko_clone"->{if(!ready(p,action,35+heartPenalty))return;spawnDakkoCloneBots(p,p.getLocation());}
            case "dakko_fox_fire"->useFoxFire(p);
            case "dakko_heavenly_arrival"->useHeavenlyArrival(p);
            case "kishin_charge"->{if(!ready(p,action,18+heartPenalty))return;p.setVelocity(p.getLocation().getDirection().normalize().multiply(Math.max(1.4,2.6-brokenHearts*.2)).setY(.15));p.getWorld().playSound(p.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);}
            case "kishin_slam"->{if(!ready(p,action,30+heartPenalty))return;p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation(),4);Collection<UUID> slamTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:slamTargets){Player q=Bukkit.getPlayer(id);if(q!=null&&!dead.contains(id)&&q.getWorld().equals(p.getWorld())&&q.getLocation().distanceSquared(p.getLocation())<49){q.damage(6,p);q.setVelocity(q.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.2).setY(.8));q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,1));}}}
            case "kishin_roar"->useKishinRoar(p);
            case "kishin_iron_body"->useKishinIronBody(p);
            case "shikki_frenzy"->useShikkiFrenzy(p);
            case "shikki_blood_run"->useShikkiBloodRun(p);
            case "shikki_hunting_leap"->useShikkiHuntingLeap(p);
            case "shikki_chain_hunt"->useShikkiChainHunt(p);
            case "yuuki_veil"->useYuukiVeil(p);
            case "yuuki_haze_step"->useYuukiHazeStep(p);
            case "yuuki_shadow_bind"->useYuukiShadowBind(p);
            case "yuuki_divine_hide"->useYuukiDivineHide(p);
            case "kanki_summon"->useKankiSummon(p);
            case "kanki_command"->useKankiCommand(p);
            case "kanki_recall"->useKankiRecall(p);
            case "kanki_parade"->useKankiParade(p);
            case "jakutsuki_black_mirror"->useBlackMirror(p);
            case "jakutsuki_sweep"->useJakutsukiSweep(p);
            case "jakutsuki_snakefall"->useSnakefall(p);
            case "jakutsuki_release"->useJakutsukiRelease(p);
            case "jakutsuki_piercing_blast"->useJakutsukiPiercingBlast(p);
        }
    }


    private void useKankiSummon(Player p){
        OniType selected=kankiSelectedSummon.getOrDefault(p.getUniqueId(),OniType.DAKKO);
        if(p.isSneaking()){OniType[] order={OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI};int i=0;for(;i<order.length;i++)if(order[i]==selected)break;selected=order[(i+1)%order.length];kankiSelectedSummon.put(p.getUniqueId(),selected);p.sendActionBar(cc("&5鬼喚び &7――召喚先: &f"+selected.display));return;}
        if(!ready(p,"kanki_summon",Math.max(1,getConfig().getInt("kanki-skills.summon.cooldown-seconds",24))))return;
        spawnKankiSummon(p,selected,Math.max(3,getConfig().getInt("kanki-skills.summon.duration-seconds",22)),1.0);kankiLastSummon.put(p.getUniqueId(),selected);
    }
    private void useKankiCommand(Player p){
        if(!ready(p,"kanki_command",Math.max(1,getConfig().getInt("kanki-skills.command.cooldown-seconds",8))))return;
        LivingEntity target=nearestKankiTarget(p.getLocation(),Math.max(8,getConfig().getDouble("kanki-skills.command.range",36.0)));int commanded=0;
        for(UUID id:new HashSet<>(kankiSummons)){Entity e=Bukkit.getEntity(id);if(e instanceof Mob mob&&mob.isValid()&&mob.getWorld().equals(p.getWorld())){mob.setTarget(target);commanded++;}}
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_EVOKER_PREPARE_SUMMON,1,.8f);p.sendActionBar(cc(target==null?"&d鬼令 &7――召喚鬼を再索敵させた":"&d鬼令 &7――&f"+commanded+"体 &7へ獲物を指示"));
    }
    private void useKankiRecall(Player p){
        OniType last=kankiLastSummon.get(p.getUniqueId());if(last==null){msg(p,"&7まだ再臨させられる鬼を召喚していません。");return;}if(!ready(p,"kanki_recall",Math.max(1,getConfig().getInt("kanki-skills.recall.cooldown-seconds",38))))return;
        spawnKankiSummon(p,last,Math.max(3,getConfig().getInt("kanki-skills.recall.duration-seconds",15)),.72);p.sendActionBar(cc("&6再臨 &7――&f"+last.display+" &7を再び喚んだ"));
    }
    private void useKankiParade(Player p){
        if(!ready(p,"kanki_parade",Math.max(1,getConfig().getInt("kanki-skills.parade.cooldown-seconds",70))))return;int duration=Math.max(3,getConfig().getInt("kanki-skills.parade.duration-seconds",18));OniType[] types={OniType.DAKKO,OniType.KISHIN,OniType.SHIKKI,OniType.YUUKI};for(OniType type:types)spawnKankiSummon(p,type,duration,.78);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,1.2f,.65f);p.sendTitle(cc("&4&l――百鬼夜行――"),cc("&7歴代の鬼が現世へ降り立つ"),5,35,10);
    }
    private LivingEntity spawnKankiSummon(Player owner,OniType type,int durationSeconds,double strength){
        Location at=owner.getLocation().clone().add(owner.getLocation().getDirection().setY(0).normalize().multiply(2));Zombie mob=at.getWorld().spawn(at,Zombie.class);mob.setBaby(false);mob.setPersistent(false);mob.setRemoveWhenFarAway(false);mob.setCanPickupItems(false);mob.setCustomName(cc("&5召喚鬼 &8[&f"+type.display+"&8]"));mob.setCustomNameVisible(true);double hp=Math.max(8,getConfig().getDouble("kanki-skills.summon.health",24.0)*strength);mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(hp);mob.setHealth(hp);mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(Math.max(2,getConfig().getDouble("kanki-skills.summon.attack-damage",5.0)*strength));mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(type==OniType.SHIKKI?.36:type==OniType.KISHIN?.27:.31);mob.getEquipment().setItemInMainHand(new ItemStack(type==OniType.KISHIN?Material.IRON_AXE:Material.IRON_SWORD));if(type==OniType.DAKKO){mob.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));}else if(type==OniType.KISHIN){mob.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));}else if(type==OniType.SHIKKI){mob.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,durationSeconds*20,1,false,true));}else if(type==OniType.YUUKI){mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Math.min(60,durationSeconds*20),0,false,false));}
        kankiSummons.add(mob.getUniqueId());LivingEntity target=nearestKankiTarget(at,40);mob.setTarget(target);at.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,at.clone().add(0,1,0),35,.5,.8,.5,.06);at.getWorld().playSound(at,Sound.ENTITY_EVOKER_CAST_SPELL,1,.75f);
        startKankiSignatureAi(mob,type,strength);
        Bukkit.getScheduler().runTaskLater(this,()->{removeKankiSummon(mob.getUniqueId(),true);},durationSeconds*20L);return mob;
    }
    private void startKankiSignatureAi(Mob mob,OniType type,double strength){
        int interval=Math.max(30,getConfig().getInt("kanki-skills.summon.signature-check-ticks",50));
        BukkitTask task=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(!mob.isValid()||mob.isDead()||!kankiSummons.contains(mob.getUniqueId())){BukkitTask t=kankiSkillTasks.remove(mob.getUniqueId());if(t!=null)t.cancel();return;}
            LivingEntity target=nearestKankiTarget(mob.getLocation(),32);if(target==null)return;mob.setTarget(target);
            if(Math.random()>Math.max(.05,Math.min(1.0,getConfig().getDouble("kanki-skills.summon.signature-use-chance",.55))))return;
            useKankiSignatureSkill(mob,type,target,strength);
        },Math.max(20,getConfig().getInt("kanki-skills.summon.signature-first-delay-ticks",30)),interval);
        kankiSkillTasks.put(mob.getUniqueId(),task);
    }
    private void useKankiSignatureSkill(Mob mob,OniType type,LivingEntity target,double strength){
        Location at=mob.getLocation();
        switch(type){
            case DAKKO -> { // 狐火: dodgeable fire projectile
                SmallFireball ball=mob.launchProjectile(SmallFireball.class);Vector aim=target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector()).normalize();ball.setVelocity(aim.multiply(.85));ball.setIsIncendiary(false);ball.setYield(0);at.getWorld().spawnParticle(Particle.FLAME,at.clone().add(0,1.2,0),18,.25,.35,.25,.03);at.getWorld().playSound(at,Sound.ENTITY_BLAZE_SHOOT,.8f,1.35f);
            }
            case KISHIN -> { // 鬼吼: short-range area disruption
                double radius=Math.max(3,getConfig().getDouble("kanki-skills.summon.kishin-roar-radius",6.0));for(Player q:at.getWorld().getPlayers()){if(!isKankiVictim(q)||q.getLocation().distanceSquared(at)>radius*radius)continue;q.damage(Math.max(1,3.0*strength),mob);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,45,0,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,55,0,false,true));Vector kb=q.getLocation().toVector().subtract(at.toVector()).setY(.25);if(kb.lengthSquared()>0)q.setVelocity(kb.normalize().multiply(.55));}at.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,at.clone().add(0,1,0),2,.4,.3,.4,0);at.getWorld().playSound(at,Sound.ENTITY_RAVAGER_ROAR,1,.8f);
            }
            case SHIKKI -> { // 跳躍狩り: leap directly into chase
                Vector v=target.getLocation().toVector().subtract(at.toVector());if(v.lengthSquared()>0){v.normalize().multiply(Math.max(.8,1.25*strength));v.setY(.48);mob.setVelocity(v);}at.getWorld().spawnParticle(Particle.CRIT,at.clone().add(0,1,0),22,.3,.5,.3,.05);at.getWorld().playSound(at,Sound.ENTITY_ENDER_DRAGON_FLAP,.75f,1.4f);
            }
            case YUUKI -> { // 朧渡り + 影縫い: blink beside prey and briefly bind
                Vector back=target.getLocation().getDirection().setY(0);if(back.lengthSquared()==0)back=new Vector(1,0,0);Location dest=target.getLocation().clone().subtract(back.normalize().multiply(2));dest.setY(target.getLocation().getY());at.getWorld().spawnParticle(Particle.SMOKE_LARGE,at.clone().add(0,1,0),18,.3,.5,.3,.03);mob.teleport(dest);mob.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,30,0,false,false));if(target instanceof Player q&&isKankiVictim(q))q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,35,1,false,true));dest.getWorld().playSound(dest,Sound.ENTITY_ENDERMAN_TELEPORT,.8f,.75f);
            }
            default -> {}
        }
    }
    private boolean isKankiVictim(Player q){return q!=null&&players.contains(q.getUniqueId())&&!dead.contains(q.getUniqueId())&&!escaped.contains(q.getUniqueId())&&!isOni(q.getUniqueId());}
    private LivingEntity nearestKankiTarget(Location from,double radius){LivingEntity best=null;double bestD=radius*radius;for(UUID id:players){if(dead.contains(id)||escaped.contains(id)||isOni(id))continue;Player q=Bukkit.getPlayer(id);if(q!=null&&q.getWorld().equals(from.getWorld())){double d=q.getLocation().distanceSquared(from);if(d<bestD){best=q;bestD=d;}}}return best;}
    private void removeKankiSummon(UUID id,boolean effect){BukkitTask t=kankiSkillTasks.remove(id);if(t!=null)t.cancel();kankiSummons.remove(id);Entity e=Bukkit.getEntity(id);if(e!=null&&e.isValid()){if(effect)e.getWorld().spawnParticle(Particle.SMOKE_LARGE,e.getLocation().add(0,1,0),25,.4,.7,.4,.04);e.remove();}}
    private void removeKankiSummons(){for(UUID id:new HashSet<>(kankiSummons))removeKankiSummon(id,false);for(BukkitTask t:kankiSkillTasks.values())if(t!=null)t.cancel();kankiSkillTasks.clear();kankiSummons.clear();}


    private boolean yuukiVeiled(Player p){return p!=null&&yuukiVeiled.contains(p.getUniqueId());}
    private void useYuukiVeil(Player p){
        UUID id=p.getUniqueId();
        if(yuukiVeiled(p)){endYuukiVeil(p,true);return;}
        if(!ready(p,"yuuki_veil",getConfig().getInt("yuuki-skills.veil.cooldown-seconds",24)))return;
        yuukiVeiled.add(id);p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Integer.MAX_VALUE,Math.max(0,getConfig().getInt("yuuki-skills.veil.speed-amplifier",0)),false,false));
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_SCULK_SENSOR_CLICKING_STOP,.8f,.7f);p.sendActionBar(cc("&8幽歩 &7――気配を断った。再使用で実体化"));
    }
    private void endYuukiVeil(Player p,boolean ambush){
        if(!yuukiVeiled.remove(p.getUniqueId()))return;p.removePotionEffect(PotionEffectType.INVISIBILITY);p.removePotionEffect(PotionEffectType.SPEED);
        int ticks=Math.max(10,getConfig().getInt("yuuki-skills.veil.uncloak-speed-ticks",50));p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,true));
        yuukiAmbushUntil.put(p.getUniqueId(),System.currentTimeMillis()+Math.max(500,getConfig().getLong("yuuki-skills.veil.ambush-window-millis",3000)));
        p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BELL_USE,1.0f,.55f);p.getWorld().spawnParticle(Particle.SMOKE_LARGE,p.getLocation().add(0,1,0),30,.45,.8,.45,.04);
        if(ambush&&oniSkillUnlocked("yuuki_shadow_bind"))applyYuukiShadowBind(p);
    }
    private void useYuukiHazeStep(Player p){
        if(!yuukiVeiled(p)){msg(p,"&7朧渡り &fは幽歩中のみ使用できます。");return;}if(!ready(p,"yuuki_haze_step",getConfig().getInt("yuuki-skills.haze-step.cooldown-seconds",18)))return;
        double range=Math.max(2,getConfig().getDouble("yuuki-skills.haze-step.range",6.0));Vector dir=p.getLocation().getDirection().normalize();Location dest=p.getLocation().clone();
        for(double d=.5;d<=range;d+=.5){Location test=p.getLocation().clone().add(dir.clone().multiply(d));if(!test.getBlock().isPassable()||!test.clone().add(0,1,0).getBlock().isPassable())break;dest=test;}
        p.teleport(dest.setDirection(p.getLocation().getDirection()));p.getWorld().spawnParticle(Particle.SMOKE_NORMAL,p.getLocation().add(0,1,0),20,.3,.6,.3,.03);p.playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.55f,.55f);
    }
    private void useYuukiShadowBind(Player p){
        if(!yuukiVeiled(p)){applyYuukiShadowBind(p);return;}endYuukiVeil(p,true);
    }
    private void applyYuukiShadowBind(Player p){
        if(!ready(p,"yuuki_shadow_bind",getConfig().getInt("yuuki-skills.shadow-bind.cooldown-seconds",30)))return;
        double radius=Math.max(1,getConfig().getDouble("yuuki-skills.shadow-bind.radius",5.0));int slow=Math.max(10,getConfig().getInt("yuuki-skills.shadow-bind.slowness-ticks",50));long seal=Math.max(250,getConfig().getLong("yuuki-skills.shadow-bind.skill-seal-millis",1800));
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||q.getWorld()!=p.getWorld()||q.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slow,1,false,true));oniSkillSealUntil.put(id,System.currentTimeMillis()+seal);q.sendActionBar(cc("&5影縫い &7――足と技を封じられた！"));}
        p.getWorld().spawnParticle(Particle.SQUID_INK,p.getLocation().add(0,.4,0),45,radius*.45,.25,radius*.45,.02);
    }
    private void useYuukiDivineHide(Player p){
        if(!yuukiVeiled(p)){msg(p,"&8神隠し &fは幽歩中のみ使用できます。");return;}if(!ready(p,"yuuki_divine_hide",getConfig().getInt("yuuki-skills.divine-hide.cooldown-seconds",55)))return;
        int ticks=Math.max(20,getConfig().getInt("yuuki-skills.divine-hide.duration-ticks",180));yuukiDivineHideUntil.put(p.getUniqueId(),System.currentTimeMillis()+ticks*50L);p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,false));p.sendTitle("",cc("&8&l神 隠 し"),0,25,8);
    }
    private boolean yuukiDivineHideActive(Player p){return p!=null&&System.currentTimeMillis()<yuukiDivineHideUntil.getOrDefault(p.getUniqueId(),0L);}
    private void tickYuukiStealth(){
        if(state!=GameState.RUNNING||oniType!=OniType.YUUKI||oni==null)return;Player p=Bukkit.getPlayer(oni);if(!yuukiVeiled(p))return;
        double radius=Math.max(1,getConfig().getDouble("yuuki-skills.veil.warning-radius",5.0));if(yuukiDivineHideActive(p))radius=Math.max(1,getConfig().getDouble("yuuki-skills.divine-hide.warning-radius",2.5));
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||q.getWorld()!=p.getWorld()||q.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;q.spawnParticle(Particle.SMOKE_NORMAL,p.getLocation().add(0,1,0),2,.2,.5,.2,.01);if(new Random().nextInt(4)==0)q.playSound(p.getLocation(),Sound.BLOCK_SCULK_SENSOR_CLICKING,.18f,.55f);}
    }

    private boolean shikkiFrenzyActive(){return System.currentTimeMillis()<shikkiFrenzyUntil;}
    private void useShikkiFrenzy(Player p){
        if(!ready(p,"shikki_frenzy",getConfig().getInt("shikki-skills.frenzy.cooldown-seconds",32)))return;
        int ticks=Math.max(20,getConfig().getInt("shikki-skills.frenzy.duration-ticks",160));
        shikkiFrenzyUntil=System.currentTimeMillis()+ticks*50L;shikkiChainVictims.clear();shikkiFinisherReady=false;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,Math.max(0,getConfig().getInt("shikki-skills.frenzy.speed-amplifier",2)),false,true));
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WARDEN_HEARTBEAT,1.0f,1.45f);p.getWorld().spawnParticle(Particle.CRIT,p.getLocation().add(0,1,0),45,.5,.8,.5,.08);
        p.sendTitle("",cc("&c&l狂 奔 &7――跳躍狩り強化"),0,30,8);
    }
    private void useShikkiBloodRun(Player p){
        if(!ready(p,"shikki_blood_run",getConfig().getInt("shikki-skills.blood-run.cooldown-seconds",28)))return;
        double radius=Math.max(5,getConfig().getDouble("shikki-skills.blood-run.radius",36.0));Player best=null;double bestD=Double.MAX_VALUE;
        for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q==null||dead.contains(id)||escaped.contains(id)||q.getWorld()!=p.getWorld())continue;double d=q.getLocation().distanceSquared(p.getLocation());boolean detectable=q.isSprinting()||q.getHealth()<q.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*.75;if(detectable&&d<=radius*radius&&d<bestD){best=q;bestD=d;}}
        if(best==null){p.sendActionBar(cc("&7血走――気配を掴めない"));return;}showShikkiDirection(p,best,"&c血走");best.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,best.getLocation().add(0,1,0),10,.35,.5,.35,.03);
    }
    private void useShikkiHuntingLeap(Player p){
        UUID id=p.getUniqueId();
        // 最大回数まで跳んだ後は、狂化/CT0でも空中再発動を禁止。地面への着地でのみリセットする。
        if(shikkiLeapAwaitLanding.contains(id)){
            if(!p.isOnGround()){p.sendActionBar(cc("&c跳躍狩り &7――最大連鎖後は着地が必要"));return;}
            shikkiLeapAwaitLanding.remove(id);shikkiLeapChainCount.remove(id);shikkiLeapChainUntil.remove(id);
        }
        long now=System.currentTimeMillis();int count=shikkiLeapChainCount.getOrDefault(id,0);long until=shikkiLeapChainUntil.getOrDefault(id,0L);
        int max=Math.max(1,getConfig().getInt("shikki-skills.hunting-leap.max-chain",3));
        if(oniPowerActivated)max=Math.max(max,getConfig().getInt("shikki-skills.hunting-leap.empowered-max-chain",9));
        boolean follow=count>0&&!p.isOnGround()&&now<=until&&count<max;
        if(count>0&&!follow){shikkiLeapChainCount.remove(id);shikkiLeapChainUntil.remove(id);count=0;}
        int cd=oniPowerActivated?0:(shikkiFrenzyActive()?getConfig().getInt("shikki-skills.hunting-leap.frenzy-cooldown-seconds",3):getConfig().getInt("shikki-skills.hunting-leap.cooldown-seconds",6));
        if(!follow&&cd>0&&!ready(p,"shikki_hunting_leap",cd))return;
        if(!follow&&cd<=0)cooldowns.put(p.getUniqueId()+":shikki_hunting_leap",now);
        int next=follow?count+1:1;
        double power=shikkiFrenzyActive()?getConfig().getDouble("shikki-skills.hunting-leap.frenzy-power",1.75):getConfig().getDouble("shikki-skills.hunting-leap.power",1.35);
        if(oniPowerActivated)power*=getConfig().getDouble("shikki-skills.hunting-leap.empowered-power-multiplier",1.15);
        Vector v=p.getEyeLocation().getDirection().normalize().multiply(power);v.setY(Math.max(.48,v.getY()+.48));p.setVelocity(v);p.setFallDistance(0);
        shikkiLeapAttackUntil.put(id,now+Math.max(500,getConfig().getLong("shikki-skills.hunting-leap.attack-window-millis",3000)));
        shikkiLeapChainCount.put(id,next);
        long grace=next==1?getConfig().getLong("shikki-skills.hunting-leap.chain-grace-1-millis",1200):getConfig().getLong("shikki-skills.hunting-leap.chain-grace-2-millis",750);
        if(oniPowerActivated)grace=Math.round(grace*getConfig().getDouble("shikki-skills.hunting-leap.empowered-grace-multiplier",2.0));
        if(next<max)shikkiLeapChainUntil.put(id,now+Math.max(150,grace));else{shikkiLeapChainUntil.remove(id);shikkiLeapAwaitLanding.add(id);}
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDER_DRAGON_FLAP,.8f,(float)(1.45+next*.12));p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation(),24,.35,.15,.35,.07);
        if(isTagMode())startTagShikkiExplosiveTrail(p,next,max);
        if(next<max)p.sendActionBar(cc("&6跳躍狩り &f"+next+"/&f"+max+" &7――空中で追加入力！ &8("+grace+"ms)"));else p.sendActionBar(cc("&c&l最大跳躍 &7――最大連鎖！ 命中で即時回復"));
        // 着地した瞬間に追加入力権を失わせる。猶予時間が残っていても再跳躍不可。
        Bukkit.getScheduler().runTaskLater(this,()->watchShikkiLeapLanding(p,id),1L);
    }
    // v0.40.60 TAG疾鬼: 跳躍狩りの移動軌跡を連続爆発VFXで描く。地形破壊・追加ダメージ・ノックバックは一切発生させない。
    private void startTagShikkiExplosiveTrail(LivingEntity hunter,int chain,int max){
        if(!isTagMode()||hunter==null||!hunter.isValid())return;
        final Location[] last={hunter.getLocation().clone().add(0,.55,0)};final int[] age={0};final BukkitTask[] task=new BukkitTask[1];
        task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(state!=GameState.RUNNING||!isTagMode()||!hunter.isValid()||hunter.isDead()||age[0]++>=14){task[0].cancel();return;}
            Location now=hunter.getLocation().clone().add(0,.55,0);if(now.getWorld()!=last[0].getWorld()){last[0]=now;return;}
            Vector delta=now.toVector().subtract(last[0].toVector());double len=delta.length();
            if(len>.08){Vector dir=delta.clone().normalize();double spacing=chain>=7?.42:(chain>=4?.52:.62);for(double d=0;d<=len;d+=spacing){Location fx=last[0].clone().add(dir.clone().multiply(d));int burst=chain>=7?4:(chain>=4?3:2);fx.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL,fx,burst,.12,.12,.12,.015);fx.getWorld().spawnParticle(Particle.SMOKE_LARGE,fx,Math.max(1,burst-1),.18,.18,.18,.025);if(chain>=4)fx.getWorld().spawnParticle(Particle.FLAME,fx,2,.12,.12,.12,.015);}}
            last[0]=now;
            if(hunter.isOnGround()&&age[0]>2){if(chain>=max){Location impact=hunter.getLocation().clone().add(0,.15,0);impact.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,impact,3,.45,.18,.45,.02);impact.getWorld().spawnParticle(Particle.CLOUD,impact,42,1.05,.18,1.05,.09);tagRing(impact.getWorld(),impact,2.7,Particle.CLOUD,42);impact.getWorld().playSound(impact,Sound.ENTITY_GENERIC_EXPLODE,1.15f,.72f);}task[0].cancel();}
        },0L,1L);
    }

    private void watchShikkiLeapLanding(Player p,UUID id){
        if(!p.isOnline())return;
        if(p.isOnGround()){
            shikkiLeapChainCount.remove(id);shikkiLeapChainUntil.remove(id);shikkiLeapAwaitLanding.remove(id);
            p.sendActionBar(cc("&6跳躍狩り &7――着地、再使用可能"));
            return;
        }
        // 最大連鎖到達後はカウントを保持したまま着地を監視する。
        if(shikkiLeapAwaitLanding.contains(id)){Bukkit.getScheduler().runTaskLater(this,()->watchShikkiLeapLanding(p,id),1L);return;}
        if(!shikkiLeapChainCount.containsKey(id))return;
        long until=shikkiLeapChainUntil.getOrDefault(id,0L);
        if(System.currentTimeMillis()>until){shikkiLeapChainCount.remove(id);shikkiLeapChainUntil.remove(id);return;}
        Bukkit.getScheduler().runTaskLater(this,()->watchShikkiLeapLanding(p,id),1L);
    }
    private void recoverShikkiLeapOnHit(Player hunter){
        long now=System.currentTimeMillis();
        if(now>shikkiLeapAttackUntil.getOrDefault(hunter.getUniqueId(),0L))return;
        shikkiLeapAttackUntil.remove(hunter.getUniqueId());
        String key=hunter.getUniqueId()+":shikki_hunting_leap";
        cooldowns.put(key,now);hunter.sendActionBar(cc("&6狩猟命中 &7――跳躍狩り 即時回復！"));
    }

    private void useShikkiChainHunt(Player p){
        if(!shikkiFrenzyActive()){msg(p,"&4狩猟連鎖 &fは狂奔中のみ使用できます。");return;}
        if(!ready(p,"shikki_chain_hunt",getConfig().getInt("shikki-skills.chain-hunt.cooldown-seconds",45)))return;
        int extend=Math.max(20,getConfig().getInt("shikki-skills.chain-hunt.extend-ticks",80));shikkiFrenzyUntil+=extend*50L;cooldowns.put(p.getUniqueId()+":shikki_hunting_leap",System.currentTimeMillis());p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,(int)Math.max(1,(shikkiFrenzyUntil-System.currentTimeMillis())/50),3,false,true));p.sendActionBar(cc("&4狩猟連鎖 &7――狂奔延長・跳躍即時回復・速度上昇IV"));
    }
    private void showShikkiDirection(Player hunter,Player target,String label){
        Vector to=target.getLocation().toVector().subtract(hunter.getLocation().toVector());double dist=to.length();Vector look=hunter.getEyeLocation().getDirection().setY(0);Vector flat=to.clone().setY(0);String arrow="↑";
        if(look.lengthSquared()>.01&&flat.lengthSquared()>.01){double cross=look.normalize().crossProduct(flat.normalize()).getY(),dot=look.dot(flat.normalize());arrow=dot>.65?"↑":dot<-.65?"↓":cross>0?"←":"→";}
        hunter.sendActionBar(cc(label+" &f"+arrow+" &7約&e"+(int)dist+"m &7―― "+target.getName()));
    }
    private void onShikkiFrenzyHit(Player hunter,Player victim,EntityDamageByEntityEvent e){
        if(oniType!=OniType.SHIKKI||!shikkiFrenzyActive())return;
        if(shikkiFinisherReady){e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("shikki-skills.chain-hunt.finisher-damage-multiplier",1.75)));shikkiFinisherReady=false;shikkiChainVictims.clear();hunter.sendTitle("",cc("&4&lFINISH &7―― 狩猟完遂"),0,20,8);return;}
        e.setDamage(e.getDamage()*Math.max(.1,Math.min(1.0,getConfig().getDouble("shikki-skills.frenzy.attack-damage-multiplier",.55))));
        recoverShikkiLeapOnHit(hunter);
        if(!shikkiChainVictims.add(victim.getUniqueId()))return;
        int chain=shikkiChainVictims.size();int extend=Math.max(0,getConfig().getInt("shikki-skills.frenzy.hit-extend-ticks",30));shikkiFrenzyUntil+=extend*50L;
        Player next=null;double bd=Double.MAX_VALUE;for(UUID id:players){if(shikkiChainVictims.contains(id)||dead.contains(id)||escaped.contains(id))continue;Player q=Bukkit.getPlayer(id);if(q!=null&&q.getWorld()==hunter.getWorld()){double d=q.getLocation().distanceSquared(hunter.getLocation());if(d<bd){bd=d;next=q;}}}
        hunter.sendActionBar(cc("&c狂奔連鎖 &f"+chain+"/3"+(next!=null?" &7――次の獲物 ":"")));if(next!=null)showShikkiDirection(hunter,next,"&c次の獲物");
        if(chain>=3&&oniSkillUnlocked("shikki_chain_hunt")){shikkiFinisherReady=true;hunter.sendTitle("",cc("&4&lフィニッシュ準備完了"),0,25,6);}
    }

    private void useKishinRoar(Player p){
        if(!ready(p,"kishin_roar",getConfig().getInt("kishin-skills.roar.cooldown-seconds",28)+brokenHearts*3))return;
        double radius=getConfig().getDouble("kishin-skills.roar.radius",8.0),damage=getConfig().getDouble("kishin-skills.roar.fixed-damage",5.0),knock=getConfig().getDouble("kishin-skills.roar.knockback",.65);
        int slowTicks=getConfig().getInt("kishin-skills.roar.slowness-ticks",60),weakTicks=getConfig().getInt("kishin-skills.roar.weakness-ticks",80);
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1.35f,.55f);p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation().add(0,1,0),8,1.4,.8,1.4,.05);p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,1,0),90,radius*.55,.8,radius*.55,.08);
        Collection<UUID> ids=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;
        for(UUID id:ids){Player q=Bukkit.getPlayer(id);if(!validSkillTarget(p,q,radius))continue;dealOniSkillFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(p,q,knock,.32);q.sendActionBar(cc("&4鬼吼 &7――身体が竦んだ！"));}
        for(UUID id:playerBots){if(!validPlayerBotSkillTarget(p,id,radius))continue;LivingEntity q=(LivingEntity)Bukkit.getEntity(id);dealOniSkillFixedDamage(q,damage);q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,slowTicks,1,false,true));q.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS,weakTicks,0,false,true));pushAway(p,q,knock,.32);}
    }
    private void useKishinIronBody(Player p){
        if(!ready(p,"kishin_iron_body",getConfig().getInt("kishin-skills.iron-body.cooldown-seconds",35)+brokenHearts*3))return;
        int ticks=Math.max(20,getConfig().getInt("kishin-skills.iron-body.duration-ticks",120));
        p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,ticks,0,false,true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,1,false,true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,ticks,1,false,true));
        p.getWorld().spawnParticle(Particle.CRIT,p.getLocation().add(0,1,0),60,.65,1,.65,.08);
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_SHIELD_BLOCK,1.1f,.65f);
        p.sendActionBar(cc("&6剛身 &7――耐性I＋速度II＋攻撃力上昇II"));
        UUID id=p.getUniqueId();
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(state==GameState.RUNNING&&oni!=null&&oni.equals(id)&&p.isOnline()&&p.isValid()&&!p.isDead())
                p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,Integer.MAX_VALUE,0,false,false));
        },ticks+1L);
    }
    private void pushAway(LivingEntity source,LivingEntity target,double horizontal,double vertical){Vector v=target.getLocation().toVector().subtract(source.getLocation().toVector()).setY(0);if(v.lengthSquared()<.01)v=new Vector(0,0,1);target.setVelocity(v.normalize().multiply(horizontal).setY(vertical));}

    private void useBlackMirror(Player p){
        boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());
        if(p.isSneaking()||!blackMirrorLocations.containsKey(p.getUniqueId())){
            Block target=p.getTargetBlockExact(getConfig().getInt("jakutsuki-skills.black-mirror.place-range",12));
            if(target==null){msg(p,"&5黒鏡 &fを置く地点へ視点を合わせてください。");return;}
            Location placed=target.getLocation().add(.5,1.05,.5);removeBlackMirror(p.getUniqueId());
            ArmorStand stand=p.getWorld().spawn(placed,ArmorStand.class);stand.setVisible(false);stand.setMarker(true);stand.setGravity(false);stand.setInvulnerable(true);stand.setCustomName(cc("&5&l黒鏡"));stand.setCustomNameVisible(true);stand.getEquipment().setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));
            blackMirrorLocations.put(p.getUniqueId(),placed);blackMirrorEntities.put(p.getUniqueId(),stand.getUniqueId());p.getWorld().playSound(placed,Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,.8f,.55f);msg(p,"&5黒鏡を設置した。 &7右クリックで転移できます。");return;
        }
        if(!ready(p,"jakutsuki_black_mirror",getConfig().getInt("jakutsuki-skills.black-mirror.cooldown-seconds",60)))return;
        Location dest=blackMirrorLocations.get(p.getUniqueId());if(dest==null||dest.getWorld()==null){msg(p,"黒鏡が設置されていません。");return;}dest=dest.clone();dest.setDirection(p.getLocation().getDirection());p.getWorld().spawnParticle(Particle.SMOKE_LARGE,p.getLocation(),45,.5,1,.5,.04);p.teleport(dest);p.getWorld().spawnParticle(Particle.PORTAL,dest,60,.6,1,.6,.2);p.getWorld().playSound(dest,Sound.ENTITY_ENDERMAN_TELEPORT,1f,.55f);if(training)trainingLog(p,"&5黒鏡へ転移");
    }
    private void removeBlackMirror(UUID owner){UUID id=blackMirrorEntities.remove(owner);if(id!=null){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}blackMirrorLocations.remove(owner);}
    private void removeBlackMirrors(){for(UUID id:new HashSet<>(blackMirrorEntities.keySet()))removeBlackMirror(id);}
    private void useJakutsukiSweep(Player p){if(!ready(p,"jakutsuki_sweep",getConfig().getInt("jakutsuki-skills.sweep.cooldown-seconds",16)))return;double radius=getConfig().getDouble("jakutsuki-skills.sweep.radius",7.0),damage=getConfig().getDouble("jakutsuki-skills.sweep.damage",7.0),half=Math.toRadians(getConfig().getDouble("jakutsuki-skills.sweep.angle-degrees",100.0)/2.0);Vector forward=p.getEyeLocation().getDirection().setY(0);if(forward.lengthSquared()<.01)forward=new Vector(0,0,1);forward.normalize();for(int i=0;i<=20;i++){double angle=-half+(half*2*i/20.0);Vector v=forward.clone().rotateAroundY(angle);for(double d=1;d<=radius;d+=1.0)p.getWorld().spawnParticle(Particle.SWEEP_ATTACK,p.getLocation().add(0,1,0).add(v.clone().multiply(d)),1,0,0,0,0);}Collection<UUID> targets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:targets){if(id.equals(p.getUniqueId()))continue;Player q=Bukkit.getPlayer(id);if(q==null||!validSkillTarget(p,q,radius))continue;Vector to=q.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(to.lengthSquared()<.01)continue;if(forward.angle(to.normalize())<=half){dealOniSkillFixedDamage(q,damage);q.setVelocity(to.normalize().multiply(.65).setY(.22));}}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity q)||q.getWorld()!=p.getWorld()||q.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;Vector to=q.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0);if(to.lengthSquared()>.01&&forward.angle(to.normalize())<=half)dealOniSkillFixedDamage(q,damage);}p.getWorld().playSound(p.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,1.2f,.6f);}
    private void useSnakefall(Player p){if(!ready(p,"jakutsuki_snakefall",getConfig().getInt("jakutsuki-skills.snakefall.cooldown-seconds",45)))return;int count=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.count",30)),life=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.lifetime-seconds",20));double radius=Math.max(3,getConfig().getDouble("jakutsuki-skills.snakefall.spawn-radius",22.0));List<Location> anchors=new ArrayList<>();for(UUID id:players){Player q=Bukkit.getPlayer(id);if(q!=null&&!dead.contains(id)&&!escaped.contains(id)&&q.getWorld()==p.getWorld())anchors.add(q.getLocation());}for(UUID id:playerBots){Entity e=Bukkit.getEntity(id);if(e!=null&&e.getWorld()==p.getWorld())anchors.add(e.getLocation());}if(anchors.isEmpty())anchors.add(p.getLocation());Random r=new Random();for(int i=0;i<count;i++){Location base=anchors.get(r.nextInt(anchors.size()));Location at=base.clone().add((r.nextDouble()*2-1)*radius,0,(r.nextDouble()*2-1)*radius);int y=at.getWorld().getHighestBlockYAt(at.getBlockX(),at.getBlockZ(),HeightMap.MOTION_BLOCKING_NO_LEAVES);at.setY(Math.max(base.getY()-4,Math.min(base.getY()+4,y+1)));Silverfish snake=at.getWorld().spawn(at,Silverfish.class);snake.setCustomName(cc("&8蛇窟の蛇"));snake.setCustomNameVisible(false);snake.setRemoveWhenFarAway(false);snake.getPersistentDataContainer().set(actionKey,PersistentDataType.STRING,"jakutsuki_snake");jakutsukiSnakes.add(snake.getUniqueId());Bukkit.getScheduler().runTaskLater(this,()->{jakutsukiSnakes.remove(snake.getUniqueId());if(snake.isValid())snake.remove();},life*20L);}p.getWorld().playSound(p.getLocation(),Sound.ENTITY_SILVERFISH_AMBIENT,1.5f,.55f);all("&8蛇窟姫の『蛇崩』――蛇が溢れ出した。");}
    private void removeJakutsukiSnakes(){for(UUID id:new HashSet<>(jakutsukiSnakes)){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}jakutsukiSnakes.clear();}
    private void useJakutsukiPiercingBlast(Player p){
        UUID id=p.getUniqueId();if(jakutsukiPierceCharging.contains(id)){msg(p,"&d黒の波動 &fは現在溜め中です。");return;}
        int cooldown=Math.max(1,getConfig().getInt("jakutsuki-skills.piercing-blast.cooldown-seconds",35));if(!ready(p,"jakutsuki_piercing_blast",cooldown))return;
        int chargeTicks=Math.max(5,getConfig().getInt("jakutsuki-skills.piercing-blast.charge-ticks",50));double range=Math.max(3,getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)),width=Math.max(.5,getConfig().getDouble("jakutsuki-skills.piercing-blast.half-width",2.0)),damage=Math.max(0,getConfig().getDouble("jakutsuki-skills.piercing-blast.damage",16.0));
        Vector direction=p.getEyeLocation().getDirection().normalize();Location origin=p.getEyeLocation().clone();jakutsukiPierceCharging.add(id);p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,chargeTicks+5,3,false,true));p.sendTitle(cc("&5&l蛇 穿"),cc("&7呪力を収束している……"),0,chargeTicks,5);p.getWorld().playSound(p.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1.0f,.55f);
        final int[] elapsed={0};final BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{
            if(!p.isOnline()||(!jakutsukiPierceCharging.contains(id))||(state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))){jakutsukiPierceCharging.remove(id);task[0].cancel();return;}
            elapsed[0]+=2;double progress=Math.min(1.0,elapsed[0]/(double)chargeTicks);Location muzzle=p.getEyeLocation().clone().add(direction.clone().multiply(1.0));p.getWorld().spawnParticle(Particle.REVERSE_PORTAL,muzzle,6,.25,.25,.25,.04);p.getWorld().spawnParticle(Particle.REDSTONE,muzzle,8,.22,.22,.22,0,new Particle.DustOptions(Color.fromRGB(150,20,180),1.4f));
            if(elapsed[0]%10==0)p.getWorld().playSound(p.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.7f,.65f+(float)(progress*.5));
            if(elapsed[0]<chargeTicks)return;task[0].cancel();jakutsukiPierceCharging.remove(id);fireJakutsukiPiercingBlast(p,origin,direction,range,width,damage);
        },0L,2L);
    }
    private void fireJakutsukiPiercingBlast(LivingEntity caster,Location origin,Vector direction,double range,double halfWidth,double damage){
        World world=origin.getWorld();if(world==null)return;Vector dir=direction.clone().normalize();Particle.DustOptions darkPurple=new Particle.DustOptions(Color.fromRGB(105,0,135),2.0f),crimson=new Particle.DustOptions(Color.fromRGB(190,0,65),1.5f);
        for(double d=0;d<=range;d+=.55){Location point=origin.clone().add(dir.clone().multiply(d));world.spawnParticle(Particle.REDSTONE,point,3,.28,.28,.28,0,darkPurple);world.spawnParticle(Particle.REDSTONE,point,2,.18,.18,.18,0,crimson);if(((int)(d*10))%22==0)world.spawnParticle(Particle.SQUID_INK,point,2,.18,.18,.18,.01);}
        world.playSound(caster.getLocation(),Sound.ENTITY_WARDEN_SONIC_BOOM,1.15f,.72f);world.playSound(caster.getLocation(),Sound.ENTITY_WITHER_SHOOT,.9f,.65f);
        Collection<LivingEntity> targets=new ArrayList<>();for(UUID id:players){if(dead.contains(id)||escaped.contains(id))continue;Player q=Bukkit.getPlayer(id);if(q!=null)targets.add(q);}for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;if(Bukkit.getEntity(id) instanceof LivingEntity q&&q.isValid()&&!q.isDead())targets.add(q);}
        for(LivingEntity q:targets){if(q.getUniqueId().equals(caster.getUniqueId())||q.getWorld()!=world)continue;Vector rel=q.getLocation().add(0,1,0).toVector().subtract(origin.toVector());double along=rel.dot(dir);if(along<0||along>range)continue;Vector perpendicular=rel.clone().subtract(dir.clone().multiply(along));if(perpendicular.lengthSquared()>halfWidth*halfWidth)continue;dealOniSkillFixedDamage(q,damage);Vector knock=dir.clone().multiply(getConfig().getDouble("jakutsuki-skills.piercing-blast.knockback",1.15));knock.setY(.22);q.setVelocity(q.getVelocity().add(knock));q.getWorld().spawnParticle(Particle.CRIT_MAGIC,q.getLocation().add(0,1,0),25,.45,.7,.45,.08);if(q instanceof Player qp)qp.sendActionBar(cc("&d黒の波動に貫かれた！"));}
    }
    private void useBotJakutsukiPiercingBlast(Mob bot,LivingEntity target){
        long now=System.currentTimeMillis();int chargeTicks=Math.max(5,getConfig().getInt("jakutsuki-skills.piercing-blast.charge-ticks",50));double range=Math.max(3,getConfig().getDouble("jakutsuki-skills.piercing-blast.range",28.0)),width=Math.max(.5,getConfig().getDouble("jakutsuki-skills.piercing-blast.half-width",2.0)),damage=Math.max(0,getConfig().getDouble("jakutsuki-skills.piercing-blast.damage",16.0));Vector direction=target.getLocation().add(0,1,0).toVector().subtract(bot.getEyeLocation().toVector()).normalize();Location origin=bot.getEyeLocation().clone();UUID id=bot.getUniqueId();jakutsukiPierceCharging.add(id);bot.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,chargeTicks+5,8,false,true));bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_BEACON_POWER_SELECT,1.0f,.55f);
        final int[] elapsed={0};final BukkitTask[] task=new BukkitTask[1];task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{if(!bot.isValid()||bot.isDead()||!jakutsukiPierceCharging.contains(id)||state!=GameState.RUNNING){jakutsukiPierceCharging.remove(id);task[0].cancel();return;}elapsed[0]+=2;Location muzzle=bot.getEyeLocation().clone();bot.getWorld().spawnParticle(Particle.REVERSE_PORTAL,muzzle,6,.25,.25,.25,.04);bot.getWorld().spawnParticle(Particle.REDSTONE,muzzle,8,.22,.22,.22,0,new Particle.DustOptions(Color.fromRGB(150,20,180),1.4f));if(elapsed[0]%10==0)bot.getWorld().playSound(bot.getLocation(),Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,.7f,.75f);if(elapsed[0]<chargeTicks)return;task[0].cancel();jakutsukiPierceCharging.remove(id);fireJakutsukiPiercingBlast(bot,origin,direction,range,width,damage);},0L,2L);
        botNextAbilityAt=now+oniBotAbilityCooldownMillis(Math.max(1,getConfig().getInt("jakutsuki-skills.piercing-blast.cooldown-seconds",35)));recoverOniSkillMomentum();
    }
    private void useJakutsukiRelease(Player p){boolean training=state==GameState.WAITING&&isInTrainingArea(p.getLocation());int remaining=Math.max(0,heartGoal()-brokenHearts);if(!training&&remaining!=1){msg(p,"&4解放 &fは心臓が残り1つの時だけ使用できます。 &7(現在 "+remaining+"つ)");return;}if(!ready(p,"jakutsuki_release",getConfig().getInt("jakutsuki-skills.release.cooldown-seconds",9999)))return;int seconds=Math.max(1,getConfig().getInt("jakutsuki-skills.release.exit-seal-seconds",60));exitSealedUntil=System.currentTimeMillis()+seconds*1000L;int amp=Math.max(0,getConfig().getInt("jakutsuki-skills.release.strength-amplifier",1)),speed=Math.max(0,getConfig().getInt("jakutsuki-skills.release.speed-amplifier",1));p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE,seconds*20,amp,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,seconds*20,speed,false,true));p.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,seconds*20,0,false,true));p.sendTitle(cc("&4&l解 放"),cc("&8蛇窟姫、狂化――脱出口封印 "+seconds+"秒"),5,45,10);all("&4&l蛇窟姫が『解放』した。 &c脱出口は"+seconds+"秒間封印される！");p.getWorld().playSound(p.getLocation(),Sound.ENTITY_WITHER_SPAWN,1.2f,.65f);p.getWorld().spawnParticle(Particle.SQUID_INK,p.getLocation().add(0,1,0),100,1.8,1.4,1.8,.08);Bukkit.getScheduler().runTaskLater(this,()->{if(state==GameState.RUNNING&&System.currentTimeMillis()>=exitSealedUntil)all("&e蛇窟姫の封印が解け、脱出口が再び有効になった。");},seconds*20L);}

    @EventHandler public void onJakutsukiSnakeDamage(EntityDamageByEntityEvent e){if(e.getDamager() instanceof LivingEntity attacker)lastCombatAt.put(attacker.getUniqueId(),System.currentTimeMillis());if(e.getEntity() instanceof LivingEntity victim)lastCombatAt.put(victim.getUniqueId(),System.currentTimeMillis());
        if(e.getEntity() instanceof Silverfish snake&&jakutsukiSnakes.contains(snake.getUniqueId())){
            Entity raw=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();ItemStack held=null;
            if(raw instanceof Player p)held=p.getInventory().getItemInMainHand();else if(raw instanceof LivingEntity le&&playerBots.contains(le.getUniqueId()))held=le.getEquipment().getItemInMainHand();
            if(isJakutsukiSnakeSlash(held)){e.setCancelled(true);jakutsukiSnakes.remove(snake.getUniqueId());snake.getWorld().spawnParticle(Particle.SWEEP_ATTACK,snake.getLocation().add(0,.5,0),3,.25,.25,.25,0);snake.getWorld().spawnParticle(Particle.CRIT_MAGIC,snake.getLocation().add(0,.5,0),18,.35,.35,.35,.08);snake.getWorld().playSound(snake.getLocation(),Sound.ENTITY_PLAYER_ATTACK_SWEEP,.9f,1.25f);snake.remove();return;}
        }
        if(!(e.getDamager() instanceof Silverfish snake)||!jakutsukiSnakes.contains(snake.getUniqueId())||!(e.getEntity() instanceof Player target))return;int ticks=Math.max(1,getConfig().getInt("jakutsuki-skills.snakefall.glow-seconds",4))*20;target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,ticks,0,false,false));target.sendActionBar(cc("&8蛇に噛まれた―― &f短時間、位置が露見する"));
    }

    private boolean tryDakkoCloneSwap(Player p){
        removeExpiredDakkoCloneBots();
        LivingEntity best=null;double bestScore=-999;Vector look=p.getEyeLocation().getDirection().normalize();double max=Math.max(8,getConfig().getDouble("dakko-mastery.clone-swap-range",28.0));
        for(UUID id:dakkoCloneBots){if(!(Bukkit.getEntity(id) instanceof LivingEntity clone)||!clone.isValid()||clone.getWorld()!=p.getWorld())continue;Vector to=clone.getLocation().toVector().subtract(p.getEyeLocation().toVector());double dist=to.length();if(dist<.5||dist>max)continue;double score=look.dot(to.normalize())-dist*.004;if(score>bestScore){bestScore=score;best=clone;}}
        if(best==null||bestScore<.30){p.sendActionBar(cc("&5狐換え &7――視線方向に分身体がいない"));return false;}
        int swapCd=Math.max(1,getConfig().getInt("dakko-mastery.clone-swap-cooldown-seconds",4)-(dakkoChain(p.getUniqueId())>=1?1:0)-(dakkoFoxFeast()?2:0));if(!ready(p,"dakko_clone_swap",swapCd))return true;
        Location a=p.getLocation().clone(),b=best.getLocation().clone();p.teleport(b.setDirection(a.getDirection()));best.teleport(a);p.setFallDistance(0);p.getWorld().playSound(p.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.75f,1.5f);p.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,p.getLocation().add(0,1,0),28,.45,.7,.45,.05);p.sendActionBar(cc("&d狐換え &7――分身体と位置交換"));return true;
    }
    private void applyDakkoFoxMark(LivingEntity target){if(target==null)return;long until=System.currentTimeMillis()+Math.max(1,getConfig().getInt("dakko-mastery.fox-mark-seconds",8))*1000L;dakkoFoxMarkUntil.put(target.getUniqueId(),until);if(target instanceof Player q)q.sendActionBar(cc("&6狐印 &7――堕狐に気配を刻まれた"));target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,target.getLocation().add(0,1,0),14,.3,.5,.3,.03);}
    private boolean hasDakkoFoxMark(LivingEntity target){return target!=null&&System.currentTimeMillis()<dakkoFoxMarkUntil.getOrDefault(target.getUniqueId(),0L);}
    private boolean collectDakkoFoxMark(LivingEntity hunter,LivingEntity target,String method){if(hunter==null||target==null||!hasDakkoFoxMark(target))return false;dakkoFoxMarkUntil.remove(target.getUniqueId());UUID hid=hunter.getUniqueId();long now=System.currentTimeMillis();int chain=now<dakkoFoxChainUntil.getOrDefault(hid,0L)?Math.min(3,dakkoFoxChain.getOrDefault(hid,0)+1):1;dakkoFoxChain.put(hid,chain);dakkoFoxChainUntil.put(hid,now+Math.max(2,getConfig().getInt("dakko-mastery.fox-chain-window-seconds",10))*1000L);if(hunter instanceof Player p)p.sendActionBar(cc("&d狐印回収 &7["+method+"] &6狐連 "+roman(chain)));target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,target.getLocation().add(0,1,0),24,.4,.6,.4,.05);if(oniDomainType==OniType.DAKKO&&isInsideOniDomain(hunter.getLocation()))lightDakkoFoxLamp(hunter);return true;}
    private String roman(int n){return n>=3?"III":n==2?"II":"I";}
    private int dakkoChain(UUID id){if(System.currentTimeMillis()>=dakkoFoxChainUntil.getOrDefault(id,0L)){dakkoFoxChain.remove(id);return 0;}return dakkoFoxChain.getOrDefault(id,0);}
    private boolean dakkoFoxFeast(){return System.currentTimeMillis()<dakkoFoxFeastUntil;}
    private void clearDakkoFoxLamps(){for(UUID id:new HashSet<>(dakkoFoxLamps)){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}dakkoFoxLamps.clear();dakkoFoxLampsLit=0;dakkoFoxFeastUntil=0L;}
    private void spawnDakkoFoxLamps(){clearDakkoFoxLamps();if(oniDomainCenter==null)return;double r=Math.max(5,getConfig().getDouble("oni-domain.dakko.fox-lamp-radius",12.0));for(int i=0;i<3;i++){double a=Math.PI*2*i/3.0;Location l=oniDomainCenter.clone().add(Math.cos(a)*r,.2,Math.sin(a)*r);ArmorStand as=l.getWorld().spawn(l,ArmorStand.class);as.setSmall(true);as.setInvisible(true);as.setGravity(false);as.setCustomName(cc("&b&l狐灯 &7[消灯]"));as.setCustomNameVisible(true);as.getEquipment().setHelmet(new ItemStack(Material.SOUL_LANTERN));dakkoFoxLamps.add(as.getUniqueId());}}
    private void lightDakkoFoxLamp(LivingEntity hunter){if(dakkoFoxLampsLit>=3)return;dakkoFoxLampsLit++;int i=0;for(UUID id:dakkoFoxLamps){if(Bukkit.getEntity(id) instanceof ArmorStand as){i++;as.setCustomName(cc(i<=dakkoFoxLampsLit?"&d&l狐灯 &f[点灯]":"&b&l狐灯 &7[消灯]"));if(i==dakkoFoxLampsLit)as.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,as.getLocation().add(0,1,0),35,.3,.6,.3,.05);}}if(dakkoFoxLampsLit>=3){dakkoFoxFeastUntil=System.currentTimeMillis()+Math.max(3,getConfig().getInt("oni-domain.dakko.fox-feast-seconds",10))*1000L;all("&d&l【狐宴】 &f三つの狐灯が満ちた――堕狐の幻惑が加速する。");if(hunter instanceof Player p)p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-domain.dakko.fox-feast-seconds",10)*20),0,false,true));}}

    private void renderDakkoFoxWalkTrail(Player caster,Location from,Location to){
        if(from==null||to==null||from.getWorld()!=to.getWorld())return;World w=from.getWorld();Vector delta=to.toVector().subtract(from.toVector());double length=delta.length();if(length<.2)return;Vector step=delta.clone().normalize().multiply(.55);int points=Math.max(1,(int)Math.ceil(length/.55));double power=oniSkillEffectMultiplier(),radius=Math.max(.5,getConfig().getDouble("dakko-skills.fox-walk.trail-radius",1.15)*power),damage=Math.max(0,getConfig().getDouble("dakko-skills.fox-walk.trail-damage",4.0));int linger=Math.max(0,(int)Math.round(getConfig().getInt("dakko-skills.fox-walk.trail-linger-ticks",35)*power));Set<UUID> hit=new HashSet<>();List<Location> trail=new ArrayList<>();Location point=from.clone().add(0,.65,0);for(int i=0;i<=points;i++){Location at=point.clone();trail.add(at);w.spawnParticle(Particle.SOUL_FIRE_FLAME,at,3,.22,.22,.22,.015);w.spawnParticle(Particle.SMOKE_NORMAL,at,2,.18,.15,.18,.01);damageDakkoFoxWalkPoint(caster,at,radius,damage,hit);point.add(step);}w.playSound(from,Sound.ENTITY_FOX_TELEPORT,.85f,.72f);if(linger>0)for(int tick=5;tick<=linger;tick+=5){int delay=tick;Bukkit.getScheduler().runTaskLater(this,()->{if(state!=GameState.RUNNING)return;for(Location at:trail){w.spawnParticle(Particle.SOUL_FIRE_FLAME,at,1,.18,.12,.18,.005);damageDakkoFoxWalkPoint(caster,at,radius,damage,hit);}},delay);}}
    private void damageDakkoFoxWalkPoint(Player caster,Location at,double radius,double damage,Set<UUID> hit){if(damage<=0)return;double r2=radius*radius;for(UUID id:players){if(hit.contains(id)||dead.contains(id)||escaped.contains(id))continue;Player q=Bukkit.getPlayer(id);if(q==null||q.equals(caster)||q.getWorld()!=at.getWorld()||q.getLocation().distanceSquared(at)>r2)continue;dealOniSkillFixedDamage(q,damage);q.setFireTicks(Math.max(q.getFireTicks(),30));hit.add(id);}for(UUID id:playerBots){if(hit.contains(id)||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))continue;Entity e=Bukkit.getEntity(id);if(!(e instanceof LivingEntity q)||q.getWorld()!=at.getWorld()||q.getLocation().distanceSquared(at)>r2)continue;dealOniSkillFixedDamage(q,damage);q.setFireTicks(Math.max(q.getFireTicks(),30));hit.add(id);}}
    private boolean tryBotDakkoCloneSwap(Mob bot,LivingEntity target){if(bot==null||target==null||dakkoCloneBots.isEmpty())return false;LivingEntity best=null;double current=bot.getLocation().distanceSquared(target.getLocation()),bestDist=current;for(UUID id:dakkoCloneBots){if(!(Bukkit.getEntity(id) instanceof LivingEntity c)||!c.isValid()||c.getWorld()!=bot.getWorld())continue;double d=c.getLocation().distanceSquared(target.getLocation());if(d+9<bestDist){bestDist=d;best=c;}}if(best==null)return false;Location a=bot.getLocation().clone(),b=best.getLocation().clone();bot.teleport(b);best.teleport(a);bot.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,bot.getLocation().add(0,1,0),22,.4,.7,.4,.04);bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_ENDERMAN_TELEPORT,.65f,1.45f);return true;}

    private void useFoxFire(Player p){
        if(!ready(p,"dakko_fox_fire",getConfig().getInt("dakko-skills.fox-fire.cooldown-seconds",30)))return;
        double power=oniSkillEffectMultiplier(),radius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0)*power,damage=getConfig().getDouble("dakko-skills.fox-fire.fixed-damage",5.0);
        int fireTicks=(int)Math.round(getConfig().getInt("dakko-skills.fox-fire.fire-ticks",100)*power);
        p.getWorld().playSound(p.getLocation(),Sound.ITEM_FIRECHARGE_USE,1.2f,.65f);p.getWorld().spawnParticle(Particle.FLAME,p.getLocation().add(0,1,0),100,radius*.55,1.2,radius*.55,.08);p.getWorld().spawnParticle(Particle.LAVA,p.getLocation(),25,radius*.5,.7,radius*.5,.03);
        Collection<UUID> foxTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:foxTargets){Player q=Bukkit.getPlayer(id);if(id.equals(p.getUniqueId())||!validSkillTarget(p,q,radius))continue;q.setFireTicks(Math.max(q.getFireTicks(),fireTicks));dealOniSkillFixedDamage(q,damage);applyDakkoFoxMark(q);q.sendActionBar(cc("&6狐火に焼かれている！"));}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.getWorld().equals(p.getWorld())||bot.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;bot.setFireTicks(Math.max(bot.getFireTicks(),fireTicks));dealOniSkillFixedDamage(bot,damage);applyDakkoFoxMark(bot);}
    }

    private void useHeavenlyArrival(Player p){
        int range=getConfig().getInt("dakko-skills.heavenly-arrival.target-range",24);
        Block target=p.getTargetBlockExact(range);if(target==null){msg(p,"天来する地点へ視点を合わせてください。");return;}
        if(!ready(p,"dakko_heavenly_arrival",getConfig().getInt("dakko-skills.heavenly-arrival.cooldown-seconds",35)))return;
        Location landing=target.getLocation().add(.5,1.05,.5);double height=getConfig().getDouble("dakko-skills.heavenly-arrival.launch-height",10.0);
        Location sky=landing.clone().add(0,height,0);sky.setY(Math.min(sky.getY(),p.getWorld().getMaxHeight()-2));sky.setDirection(p.getLocation().getDirection());
        heavenlyArrival.add(p.getUniqueId());p.teleport(sky);p.setFallDistance(0);p.setVelocity(new Vector(0,-1.35,0));p.getWorld().playSound(sky,Sound.ENTITY_PHANTOM_FLAP,1.2f,.55f);
        final BukkitTask[] task=new BukkitTask[1];final int[] ticks={0};
        task[0]=Bukkit.getScheduler().runTaskTimer(this,()->{ticks[0]++;if((state!=GameState.RUNNING&&!(state==GameState.WAITING&&isInTrainingArea(p.getLocation())))||!p.isOnline()||!heavenlyArrival.contains(p.getUniqueId())){heavenlyArrival.remove(p.getUniqueId());task[0].cancel();return;}p.getWorld().spawnParticle(Particle.FLAME,p.getLocation(),8,.25,.4,.25,.02);if((ticks[0]>4&&(p.isOnGround()||p.getLocation().getY()<=landing.getY()+.6))||ticks[0]>=60){p.teleport(landing);p.setFallDistance(0);heavenlyArrival.remove(p.getUniqueId());task[0].cancel();impactHeavenlyArrival(p);}},1L,1L);
    }

    private void impactHeavenlyArrival(Player p){
        double power=oniSkillEffectMultiplier(),radius=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-radius",5.0)*power,damage=getConfig().getDouble("dakko-skills.heavenly-arrival.impact-damage",7.0);
        p.getWorld().playSound(p.getLocation(),Sound.ENTITY_GENERIC_EXPLODE,1.2f,.65f);p.getWorld().spawnParticle(Particle.EXPLOSION_LARGE,p.getLocation(),5,.8,.4,.8,.05);p.getWorld().spawnParticle(Particle.FLAME,p.getLocation(),70,radius*.45,.8,radius*.45,.06);
        Collection<UUID> heavenlyTargets=(state==GameState.WAITING&&isInTrainingArea(p.getLocation()))?trainingPlayers:players;for(UUID id:heavenlyTargets){if(id.equals(p.getUniqueId()))continue;Player q=Bukkit.getPlayer(id);if(!validSkillTarget(p,q,radius))continue;boolean foxRecovered=collectDakkoFoxMark(p,q,"天来");dealOniSkillFixedDamage(q,damage);if(foxRecovered){String foxKey=p.getUniqueId()+":dakko_fox_fire";cooldowns.put(foxKey,Math.max(System.currentTimeMillis(),cooldowns.getOrDefault(foxKey,0L)-Math.max(1000,getConfig().getLong("dakko-mastery.heavenly-fox-fire-recovery-millis",12000))));}Vector push=q.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);q.setVelocity(push.normalize().multiply(.85).setY(.45));}
        for(UUID id:playerBots){if(deadPlayerBots.contains(id)||escapedPlayerBots.contains(id)||!(Bukkit.getEntity(id) instanceof LivingEntity bot)||!bot.getWorld().equals(p.getWorld())||bot.getLocation().distanceSquared(p.getLocation())>radius*radius)continue;dealOniSkillFixedDamage(bot,damage);Vector push=bot.getLocation().toVector().subtract(p.getLocation().toVector());if(push.lengthSquared()<.01)push=new Vector(0,0,1);bot.setVelocity(push.normalize().multiply(.85).setY(.45));}
    }

    private boolean validSkillTarget(LivingEntity oniEntity,Player target,double radius){if(target==null||oniEntity==null)return false;UUID targetId=target.getUniqueId();if(targetId.equals(oniEntity.getUniqueId()))return false;/* 鬼スキルは発動者自身と味方鬼を常に除外。練習場でも自爆しない。 */if(isOni(targetId))return false;return !dead.contains(targetId)&&!escaped.contains(targetId)&&!isDownEscape(targetId)&&target.getWorld().equals(oniEntity.getWorld())&&target.getLocation().distanceSquared(oniEntity.getLocation())<=radius*radius;}
    private void dealOniSkillFixedDamage(LivingEntity target,double damage){if(state==GameState.RUNNING&&oniPowerActive())damage*=oniSkillEffectMultiplier();dealFixedDamage(target,damage);}
    private void dealFixedDamage(LivingEntity target,double damage){if(damage<=0)return;if(state==GameState.RUNNING&&target instanceof Player gracePlayer&&players.contains(gracePlayer.getUniqueId())&&hasPlayerHitGrace(gracePlayer.getUniqueId()))return;if(state==GameState.WAITING&&target instanceof Player trainingTarget&&isInTrainingArea(trainingTarget.getLocation())){trainingLog(trainingTarget,"&c被ダメージ想定 "+String.format(Locale.US,"%.1f",damage)+" &7(体力は減りません)");return;}if(playerBots.contains(target.getUniqueId())&&(isDownEscape(target.getUniqueId())||hasPlayerBotHitGrace(target.getUniqueId())))return;if(playerBots.contains(target.getUniqueId())&&target.getHealth()-damage<=0&&tryConsumePlayerBotExtraLife(target,damage))return;if(playerBots.contains(target.getUniqueId())&&hasPlayerBotPassive(target.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))damage*=getConfig().getDouble("passive-skills.divine-technique-damage-taken-multiplier",0.90);if(target instanceof Player player){if(healingTasks.containsKey(player.getUniqueId()))stopHealing(player,"&c攻撃を受け、回復が中断された。");if(System.currentTimeMillis()<timedSkillUntil.getOrDefault(player.getUniqueId()+":UNYIELDING",0L)&&target.getHealth()-damage<=0){target.setHealth(1);player.sendActionBar(cc("&6不退転 &7――致死ダメージを耐えた"));return;}if(isDownEscape(player.getUniqueId()))return;if(tryConsumeExtraLife(player,damage))return;}target.setHealth(Math.max(0,target.getHealth()-damage));if(state==GameState.RUNNING&&target instanceof Player hitPlayer&&players.contains(hitPlayer.getUniqueId())&&hitPlayer.getHealth()>0)grantPlayerHitGrace(hitPlayer);checkPredation(target);}
    private boolean hasPlayerHitGrace(UUID id){return System.currentTimeMillis()<playerHitGraceUntil.getOrDefault(id,0L);}
    private void grantPlayerHitGrace(Player p){
        if(!getConfig().getBoolean("player-hit-grace.enabled",true)||!players.contains(p.getUniqueId())||dead.contains(p.getUniqueId())||escaped.contains(p.getUniqueId()))return;
        int ticks=Math.max(1,getConfig().getInt("player-hit-grace.duration-ticks",60));
        int amplifier=Math.max(0,getConfig().getInt("player-hit-grace.speed-amplifier",1));
        playerHitGraceUntil.put(p.getUniqueId(),System.currentTimeMillis()+ticks*50L);
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amplifier,false,true));
        p.sendActionBar(cc("&b被弾加速 &7――3秒間、加速＋無敵"));
        p.getWorld().spawnParticle(Particle.CLOUD,p.getLocation().add(0,.2,0),16,.35,.12,.35,.04);
    }
    private boolean hasPlayerBotHitGrace(UUID id){return System.currentTimeMillis()<playerBotHitGraceUntil.getOrDefault(id,0L);}
    private void grantPlayerBotHitGrace(LivingEntity bot){
        UUID id=bot.getUniqueId();if(!getConfig().getBoolean("player-hit-grace.enabled",true)||!playerBots.contains(id)||deadPlayerBots.contains(id)||escapedPlayerBots.contains(id))return;
        int ticks=Math.max(1,getConfig().getInt("player-hit-grace.duration-ticks",60)),amp=Math.max(0,getConfig().getInt("player-hit-grace.speed-amplifier",1));
        playerBotHitGraceUntil.put(id,System.currentTimeMillis()+ticks*50L);bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,ticks,amp,false,true));
        bot.getWorld().spawnParticle(Particle.CLOUD,bot.getLocation().add(0,.2,0),16,.35,.12,.35,.04);
    }
    private void checkPredation(LivingEntity victim){if(!hasOniPassive(OniPassiveSkill.PREDATION)||predationTriggered.contains(victim.getUniqueId()))return;double max=victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(victim.getHealth()>max*.30)return;predationTriggered.add(victim.getUniqueId());double amount=Math.max(0,getConfig().getDouble("oni-passive-skills.predation-stamina-recovery",6.0));if(oniBot!=null)botStamina=Math.min(20,botStamina+amount);else if(oni!=null){stamina.put(oni,Math.min(20,stamina.getOrDefault(oni,20.0)+amount));Player oniPlayer=Bukkit.getPlayer(oni);if(oniPlayer!=null){oniPlayer.setFoodLevel((int)Math.ceil(stamina.get(oni)));oniPlayer.sendActionBar(cc("&4捕食 &7――スタミナを回復した"));}}}
    private void trackHeart(Player p){Set<String> trackable=new HashSet<>(heartHp.keySet());trackable.addAll(fakeHeartKeys);Location nearest=trackable.stream().map(LocationStore::decode).filter(Objects::nonNull).filter(l->l.getWorld().equals(p.getWorld())).min(Comparator.comparingDouble(l->l.distanceSquared(p.getLocation()))).orElse(null);if(nearest==null){p.setCompassTarget(p.getWorld().getSpawnLocation());msg(p,"残っている心臓はありません。");return;}p.setCompassTarget(nearest.clone().add(.5,.5,.5));msg(p,"最寄りの心臓 &e"+relativeArrow(p,nearest)+" &f約 &e"+(int)nearest.distance(p.getLocation())+"m &7["+cardinalDirection(p.getLocation(),nearest)+"]");}
    private String relativeArrow(Player p,Location target){double dx=target.getX()-p.getLocation().getX(),dz=target.getZ()-p.getLocation().getZ();if(dx*dx+dz*dz<.0001)return "↑";double targetYaw=Math.toDegrees(Math.atan2(-dx,dz)),diff=targetYaw-p.getLocation().getYaw();while(diff<=-180)diff+=360;while(diff>180)diff-=360;int sector=Math.floorMod((int)Math.round(diff/45.0),8);return switch(sector){case 0->"↑";case 1->"↗";case 2->"→";case 3->"↘";case 4->"↓";case 5->"↙";case 6->"←";default->"↖";};}
    private String cardinalDirection(Location from,Location to){double dx=to.getX()-from.getX(),dz=to.getZ()-from.getZ();double deg=(Math.toDegrees(Math.atan2(-dx,dz))+360)%360;String[] d={"南","南西","西","北西","北","北東","東","南東"};return d[(int)Math.round(deg/45.0)%8];}

    @EventHandler public void onDamage(EntityDamageByEntityEvent e){
        if(dakkoFoxLamps.contains(e.getEntity().getUniqueId())){e.setCancelled(true);Entity a=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity sh?sh:e.getDamager();if(a instanceof Player p&&players.contains(p.getUniqueId())&&dakkoFoxLampsLit>0){dakkoFoxLampsLit--;dakkoFoxFeastUntil=0L;int j=0;for(UUID lid:dakkoFoxLamps)if(Bukkit.getEntity(lid) instanceof ArmorStand as){j++;as.setCustomName(cc(j<=dakkoFoxLampsLit?"&d&l狐灯 &f[点灯]":"&b&l狐灯 &7[消灯]"));}p.sendActionBar(cc("&b狐灯を消した &7――狐宴の進行を後退させた"));}return;}
        Entity kankiRaw=e.getDamager();if(kankiRaw instanceof Projectile kp&&kp.getShooter() instanceof Entity ke)kankiRaw=ke;if(kankiSummons.contains(kankiRaw.getUniqueId())&&e.getEntity() instanceof Player victim&&isOni(victim.getUniqueId())){e.setCancelled(true);return;}
        if(state!=GameState.RUNNING)return;
        Entity rawAttacker=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();
        if(ikimonoIds.contains(e.getEntity().getUniqueId())&&(rawAttacker instanceof Player||isGameBot(rawAttacker.getUniqueId()))){UUID beast=e.getEntity().getUniqueId(),actor=rawAttacker.getUniqueId();ikimonoLastAttacker.put(beast,actor);addIkimonoContribution(beast,actor,Math.max(0,e.getDamage())*Math.max(0,getConfig().getDouble("ikimono.contribution.damage-weight",1.0)));}
        if(ikimonoIds.contains(rawAttacker.getUniqueId())&&(e.getEntity() instanceof Player||isGameBot(e.getEntity().getUniqueId())))addIkimonoContribution(rawAttacker.getUniqueId(),e.getEntity().getUniqueId(),Math.max(0,e.getDamage())*Math.max(0,getConfig().getDouble("ikimono.contribution.tank-weight",.25)));
        boolean attackerIsBot=oniBot!=null&&rawAttacker.getUniqueId().equals(oniBot);
        boolean attackerIsOni=(oni!=null&&isOni(rawAttacker.getUniqueId()))||attackerIsBot;if(attackerIsOni&&(finalPhase||(isTagMode()&&oniPowerActivated)))e.setDamage(e.getDamage()*finalAttackMultiplier());
        boolean executionMarked=attackerIsOni&&hasOniPassive(OniPassiveSkill.EXECUTION)&&e.getEntity() instanceof LivingEntity marked&&marked.hasPotionEffect(PotionEffectType.GLOWING);
        boolean victimIsOni=(oni!=null&&isOni(e.getEntity().getUniqueId()))||(oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot));
        if(attackerIsOni&&oniType==OniType.DAKKO&&rawAttacker instanceof LivingEntity dakkoHunter&&e.getEntity() instanceof LivingEntity dakkoVictim&&hasDakkoFoxMark(dakkoVictim))collectDakkoFoxMark(dakkoHunter,dakkoVictim,"通常攻撃");
        if(attackerIsOni&&rawAttacker instanceof Player shikkiHunter&&oniType==OniType.SHIKKI&&e.getEntity() instanceof Player shikkiVictim&&players.contains(shikkiVictim.getUniqueId())){if(!shikkiFrenzyActive())recoverShikkiLeapOnHit(shikkiHunter);onShikkiFrenzyHit(shikkiHunter,shikkiVictim,e);}
        if(attackerIsOni&&rawAttacker instanceof Player yuukiHunter&&oniType==OniType.YUUKI){
            if(yuukiVeiled(yuukiHunter)){e.setCancelled(true);yuukiHunter.sendActionBar(cc("&8幽歩中は攻撃できない。実体化してください。"));return;}
            if(System.currentTimeMillis()<yuukiAmbushUntil.getOrDefault(yuukiHunter.getUniqueId(),0L)){double mult=yuukiDivineHideActive(yuukiHunter)?getConfig().getDouble("yuuki-skills.divine-hide.ambush-damage-multiplier",1.65):getConfig().getDouble("yuuki-skills.veil.ambush-damage-multiplier",1.30);e.setDamage(e.getDamage()*Math.max(1.0,mult));yuukiAmbushUntil.remove(yuukiHunter.getUniqueId());yuukiDivineHideUntil.remove(yuukiHunter.getUniqueId());yuukiHunter.sendActionBar(cc("&8奇襲成功 &7――初撃強化"));}
        }
        if(attackerIsOni&&e.getEntity() instanceof Player markedPlayer&&players.contains(markedPlayer.getUniqueId())&&hasOniPassive(OniPassiveSkill.BLOOD_MARK)){
            UUID mid=markedPlayer.getUniqueId();int marks=bloodMarkTokens.getOrDefault(mid,0);
            if(marks>=3){bloodMarkTokens.put(mid,0);long cut=Math.max(1000,getConfig().getLong("oni-passive-skills.blood-mark-cooldown-recovery-millis",6000));if(oniBot!=null)botNextAbilityAt=Math.max(System.currentTimeMillis(),botNextAbilityAt-cut);if(oni!=null){String prefix=oni.toString()+":";for(Map.Entry<String,Long> ce:new HashMap<>(cooldowns).entrySet())if(ce.getKey().startsWith(prefix))cooldowns.put(ce.getKey(),Math.max(System.currentTimeMillis(),ce.getValue()-cut));}if(rawAttacker instanceof Player op)op.sendActionBar(cc("&4血印 &7――3印回収、鬼スキルCTを短縮"));markedPlayer.sendActionBar(cc("&4血印が回収された――"));}
            else{marks++;bloodMarkTokens.put(mid,marks);markedPlayer.sendActionBar(cc("&4血印 &7―― "+marks+"/3"));if(rawAttacker instanceof Player op)op.sendActionBar(cc("&4血印 &7――"+markedPlayer.getName()+" "+marks+"/3"));}
        }
        if(playerBots.contains(rawAttacker.getUniqueId())&&!victimIsOni&&!ikimonoIds.contains(e.getEntity().getUniqueId())){e.setCancelled(true);return;}
        if(e.getEntity() instanceof Player victim){
            boolean victimIsPlayer=players.contains(victim.getUniqueId());
            if((attackerIsOni&&victimIsOni)||(!attackerIsOni&&victimIsPlayer&&rawAttacker instanceof Player)&&!getConfig().getBoolean("friendly-fire",false)){
                e.setCancelled(true);
                return;
            }
            if(attackerIsBot){e.setDamage(getConfig().getDouble("bot.attack-damage",7.0)*finalAttackMultiplier());if(rawAttacker instanceof LivingEntity botAttacker&&isKishinIronBodyActive(botAttacker))e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("kishin-skills.iron-body.attack-damage-multiplier",1.35)));}
            if(executionMarked)e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("oni-passive-skills.execution-damage-multiplier",1.15)));
            long now=System.currentTimeMillis();
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.BLOOD_FRENZY)){UUID vid=victim.getUniqueId();long prev=oniLastHitAt.getOrDefault(vid,0L);int hits=now-prev<=Math.max(500,getConfig().getLong("oni-passive-skills.blood-frenzy-window-millis",3500))?Math.min(3,oniBloodFrenzyHits.getOrDefault(vid,0)+1):1;oniBloodFrenzyHits.put(vid,hits);oniLastHitAt.put(vid,now);e.setDamage(e.getDamage()*(1.0+hits*Math.max(0,getConfig().getDouble("oni-passive-skills.blood-frenzy-damage-step",.05))));}
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.SKILL_SEAL)){oniSkillSealUntil.put(victim.getUniqueId(),now+Math.max(500,getConfig().getLong("oni-passive-skills.skill-seal-millis",1500)));victim.sendActionBar(cc("&5封殺 &7――短時間スキル使用不可"));}
            if(attackerIsOni&&victimIsPlayer&&hasOniPassive(OniPassiveSkill.FINISHER_CHASE)&&victim.getHealth()<=victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()*Math.max(.1,Math.min(.9,getConfig().getDouble("oni-passive-skills.finisher-health-ratio",.40)))){LivingEntity hunter=getOniEntity();if(hunter!=null)hunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(20,getConfig().getInt("oni-passive-skills.finisher-speed-ticks",45)),0,false,true));}UUID victimId=victim.getUniqueId();if(attackerIsOni&&victimIsPlayer&&hasPassive(victimId,PassiveSkill.PARRY)&&now-parrySneakAt.getOrDefault(victimId,0L)<=getConfig().getLong("technical-passives.parry-window-millis",320)&&technicalReady(victimId,"PARRY",getConfig().getLong("technical-passives.parry-cooldown-seconds",20))){e.setDamage(e.getDamage()*getConfig().getDouble("technical-passives.parry-damage-multiplier",.5));double parryStamina=Math.max(0,stamina.getOrDefault(victimId,20.0)-getConfig().getDouble("technical-passives.parry-stamina-cost",4.0));stamina.put(victimId,parryStamina);victim.setFoodLevel((int)Math.ceil(parryStamina));Vector side=victim.getEyeLocation().getDirection().setY(0);side=new Vector(-side.getZ(),0,side.getX()).normalize().multiply(.65).setY(.2);victim.setVelocity(side);victim.playSound(victim.getLocation(),Sound.ITEM_SHIELD_BLOCK,1f,1.25f);victim.sendActionBar(cc("&b受け流し &7――攻撃を逸らした"));}if(attackerIsOni&&victimIsPlayer&&hasPassive(victimId,PassiveSkill.CORNERED_RAT)&&now-reversalAt.getOrDefault(victimId,0L)<=getConfig().getLong("technical-passives.cornered-rat-window-millis",550)&&technicalReady(victimId,"CORNERED_RAT",getConfig().getLong("technical-passives.cornered-rat-cooldown-seconds",18))){victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,getConfig().getInt("technical-passives.cornered-rat-speed-ticks",45),1,false,true));victim.sendActionBar(cc("&e窮鼠 &7――反転から加速"));}
            if(attackerIsOni&&repairingHeart.containsKey(victim.getUniqueId()))stopRepair(victim,"&c攻撃を受け、心臓への干渉が中断された。");
        }
        if(executionMarked&&!(e.getEntity() instanceof Player))e.setDamage(e.getDamage()*Math.max(1.0,getConfig().getDouble("oni-passive-skills.execution-damage-multiplier",1.15)));
        if(victimIsOni&&playerBots.contains(rawAttacker.getUniqueId())&&rawAttacker instanceof LivingEntity attackerBot&&isJakutsukiSnakeSlash(attackerBot.getEquipment().getItemInMainHand()))e.setDamage(getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0));
        if(victimIsOni&&rawAttacker instanceof Player attacker){
            if(!players.contains(attacker.getUniqueId())){
                e.setCancelled(true);
                return;
            }
            String heldAction=actionOf(attacker.getInventory().getItemInMainHand());
            if("equipment:shinai".equals(heldAction)){e.setDamage(getConfig().getDouble("equipment.shinai-damage",3.0));}
            else if("equipment:iron_bat".equals(heldAction)){e.setDamage(getConfig().getDouble("equipment.iron-bat-damage",5.0));}
            else if(oniType==OniType.JAKUTSUKI&&"jakutsuki_snake_slash".equals(heldAction))e.setDamage(getConfig().getDouble("jakutsuki-final.snake-slash-damage",10.0));
            if(hasPassive(attacker.getUniqueId(),PassiveSkill.ATTACK_BOOST))e.setDamage(e.getDamage()*1.20);if(hasPassive(attacker.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))e.setDamage(e.getDamage()*getConfig().getDouble("passive-skills.divine-technique-attack-multiplier",1.10));
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onPlayerEquipmentHitResolved(EntityDamageByEntityEvent e){if(state!=GameState.RUNNING||!(e.getDamager() instanceof Player attacker)||!players.contains(attacker.getUniqueId()))return;boolean victimOni=(oni!=null&&isOni(e.getEntity().getUniqueId()))||(oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot));if(!victimOni||e.getFinalDamage()<=0)return;ItemStack held=attacker.getInventory().getItemInMainHand();String action=actionOf(held);if("equipment:shinai".equals(action)||"equipment:iron_bat".equals(action))consumeEquipmentUse(attacker,held,action);}
    @EventHandler public void onPlayerBotTarget(EntityTargetLivingEntityEvent e){if(state==GameState.RUNNING&&playerBots.contains(e.getEntity().getUniqueId())&&e.getTarget()!=null)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void onOniDamageResolved(EntityDamageByEntityEvent e){Entity raw=e.getDamager() instanceof Projectile pr&&pr.getShooter() instanceof Entity shooter?shooter:e.getDamager();boolean fromOni=isOni(raw.getUniqueId());if(!fromOni||!(e.getEntity() instanceof LivingEntity victim))return;Bukkit.getScheduler().runTask(this,()->{if(victim.isValid()&&!victim.isDead()){if(victim instanceof Player p&&players.contains(p.getUniqueId()))grantPlayerHitGrace(p);else if(playerBots.contains(victim.getUniqueId()))grantPlayerBotHitGrace(victim);checkPredation(victim);}});}
    @EventHandler public void onAnyDamage(EntityDamageEvent e){if(e.getEntity() instanceof LivingEntity damaged)lastCombatAt.put(damaged.getUniqueId(),System.currentTimeMillis());if(state==GameState.WAITING&&trainingDummy!=null&&e.getEntity().getUniqueId().equals(trainingDummy)){double amount=Math.max(0,e.getFinalDamage());e.setCancelled(true);for(UUID id:trainingPlayers){Player p=Bukkit.getPlayer(id);if(p!=null&&p.getWorld().equals(e.getEntity().getWorld())&&p.getLocation().distanceSquared(e.getEntity().getLocation())<=625)trainingLog(p,"&fダミーに &c"+String.format(Locale.US,"%.1f",amount)+" &fダメージ");}return;}if(e.getCause()==EntityDamageEvent.DamageCause.FALL&&(heavenlyArrival.contains(e.getEntity().getUniqueId())||bugeiActive.contains(e.getEntity().getUniqueId()))){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(state==GameState.WAITING&&e.getEntity() instanceof Player){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(state!=GameState.RUNNING)return;if(e.getCause()==EntityDamageEvent.DamageCause.FALL&&isOni(e.getEntity().getUniqueId())){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(isTagMode()&&e.getEntity() instanceof Player tp&&tagParkourPlayers.contains(tp.getUniqueId())){e.setCancelled(true);return;}if(isTagMode()&&e.getCause()==EntityDamageEvent.DamageCause.FALL&&(e.getEntity() instanceof Player||playerBots.contains(e.getEntity().getUniqueId()))){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(azakujiAllyId!=null&&e.getEntity().getUniqueId().equals(azakujiAllyId)&&e.getEntity() instanceof LivingEntity azakujiVictim&&e.getFinalDamage()>=azakujiVictim.getHealth()){e.setCancelled(true);retreatAzakuji(azakujiVictim);return;}if(azakujiAllyId!=null&&e.getEntity().getUniqueId().equals(azakujiAllyId)&&!azakujiForcedAttrition&&System.currentTimeMillis()<azakujiLeapInvulnerableUntil){e.setCancelled(true);e.getEntity().setFallDistance(0);return;}if(e.getEntity() instanceof LivingEntity graceBot&&playerBots.contains(graceBot.getUniqueId())&&hasPlayerBotHitGrace(graceBot.getUniqueId())){e.setCancelled(true);graceBot.setFallDistance(0);return;}if(e.getEntity() instanceof LivingEntity escapingBot&&playerBots.contains(escapingBot.getUniqueId())&&isDownEscape(escapingBot.getUniqueId())){e.setCancelled(true);escapingBot.setFallDistance(0);return;}if(e.getEntity() instanceof LivingEntity lifeBot&&playerBots.contains(lifeBot.getUniqueId())&&e.getFinalDamage()>=lifeBot.getHealth()&&tryConsumePlayerBotExtraLife(lifeBot,e.getFinalDamage())){e.setCancelled(true);return;}if(e.getEntity() instanceof Player gracePlayer&&players.contains(gracePlayer.getUniqueId())&&hasPlayerHitGrace(gracePlayer.getUniqueId())){e.setCancelled(true);gracePlayer.setFallDistance(0);return;}if(e.getEntity() instanceof Player escapingPlayer&&players.contains(escapingPlayer.getUniqueId())&&isDownEscape(escapingPlayer.getUniqueId())){e.setCancelled(true);escapingPlayer.setFallDistance(0);return;}if(e.getEntity() instanceof Player divinePlayer&&players.contains(divinePlayer.getUniqueId())&&hasPassive(divinePlayer.getUniqueId(),PassiveSkill.DIVINE_TECHNIQUE))e.setDamage(e.getDamage()*getConfig().getDouble("passive-skills.divine-technique-damage-taken-multiplier",0.90));if(e.getEntity() instanceof Player unyieldingPlayer&&players.contains(unyieldingPlayer.getUniqueId())&&System.currentTimeMillis()<timedSkillUntil.getOrDefault(unyieldingPlayer.getUniqueId()+":UNYIELDING",0L)&&e.getFinalDamage()>=unyieldingPlayer.getHealth()){e.setDamage(Math.max(0,unyieldingPlayer.getHealth()-1));unyieldingPlayer.sendActionBar(cc("&6不退転 &7――致死ダメージを耐えた"));}if(!e.isCancelled()&&e.getCause()==EntityDamageEvent.DamageCause.FALL&&e.getEntity() instanceof Player landingPlayer&&players.contains(landingPlayer.getUniqueId())&&selectedSkill.getOrDefault(landingPlayer.getUniqueId(),PlayerSkill.SPRINT)==PlayerSkill.SAFE_LANDING&&e.getFinalDamage()>0&&readyAutomatic(landingPlayer,"SAFE_LANDING",PlayerSkill.SAFE_LANDING.cooldown)){e.setCancelled(true);landingPlayer.setFallDistance(0);landingPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,Math.max(1,getConfig().getInt("skills.safe-landing-speed-ticks",80)),Math.max(0,getConfig().getInt("skills.safe-landing-speed-amplifier",2)),false,true));landingPlayer.getWorld().spawnParticle(Particle.CLOUD,landingPlayer.getLocation(),24,.45,.12,.45,.06);landingPlayer.playSound(landingPlayer.getLocation(),Sound.ENTITY_HORSE_LAND,.9f,1.35f);landingPlayer.sendTitle("",cc("&e&l安定着地 &7――速度上昇III"),0,24,8);return;}if(e.getEntity() instanceof Player player&&e.getFinalDamage()>0){if(healingTasks.containsKey(player.getUniqueId()))stopHealing(player,"&c攻撃を受け、回復が中断された。");if(chestOpeningTasks.remove(player.getUniqueId())!=null)player.sendActionBar(cc("&c攻撃を受け、チェストの開錠が中断された。"));if(players.contains(player.getUniqueId())&&e.getFinalDamage()>=player.getHealth()&&tryConsumeExtraLife(player,e.getFinalDamage())){e.setCancelled(true);return;}}if(heartGoalReached())return;boolean protectedOni=isOni(e.getEntity().getUniqueId());if(protectedOni)e.setDamage(e.getDamage()*oniHeartDamageMultiplier());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onFinalOniDefense(EntityDamageEvent e){
        if(state!=GameState.RUNNING||!finalPhase||!(e.getEntity() instanceof LivingEntity)||!isOni(e.getEntity().getUniqueId()))return;
        // FINALでは人間鬼・鬼Bot・代行/DUO鬼Botを含め、防具/防具エンチャント由来の軽減を0にする。見た目の装備は維持。
        try{if(e.isApplicable(EntityDamageEvent.DamageModifier.ARMOR))e.setDamage(EntityDamageEvent.DamageModifier.ARMOR,0.0);}catch(UnsupportedOperationException ignored){}
        try{if(e.isApplicable(EntityDamageEvent.DamageModifier.MAGIC))e.setDamage(EntityDamageEvent.DamageModifier.MAGIC,0.0);}catch(UnsupportedOperationException ignored){}
    }
    @EventHandler public void onFoodChange(FoodLevelChangeEvent e){if(!(e.getEntity() instanceof Player p))return;if(state==GameState.WAITING){e.setCancelled(true);p.setFoodLevel(20);p.setSaturation(20);return;}if(state==GameState.RUNNING&&participants.contains(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler public void onJoin(PlayerJoinEvent e){Player p=e.getPlayer();UUID id=p.getUniqueId();migrateLegacyJakutsukiNames(p);if(state==GameState.RUNNING&&participants.contains(id)&&disconnectGraceUntil.remove(id)!=null){restoreDisconnectedPlayerFromProxy(p);p.sendTitle(cc("&a&l再 接 続"),cc("&f代行Botから試合へ復帰しました"),5,35,10);all("&a"+p.getName()+" &7が再接続しました。代行Botから操作を引き継ぎます。");if(isOni(id)||disconnectOniProxy.containsKey(id)){restoreDisconnectedOniFromProxy(p);oniTeam.add(id);if(oni==null||Bukkit.getPlayer(oni)==null)oni=id;}p.setGameMode(GameMode.SURVIVAL);p.setCollidable(true);}if(state==GameState.RUNNING&&isDownEscape(id)){for(Player viewer:Bukkit.getOnlinePlayers())if(!viewer.getUniqueId().equals(id))viewer.hidePlayer(this,p);p.setCollidable(false);return;}if(state!=GameState.WAITING)return;loadFavoriteLoadout(p);restoreLobbyDay(p.getWorld());p.resetPlayerTime();p.setPlayerWeather(WeatherType.CLEAR);p.setFoodLevel(20);p.setSaturation(20);p.setFallDistance(0);p.setFireTicks(0);if(isInTrainingArea(p.getLocation()))Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&isInTrainingArea(p.getLocation()))enterTraining(p);},1);}
    @EventHandler public void onCombust(EntityCombustEvent e){if(state==GameState.RUNNING&&((oniBot!=null&&e.getEntity().getUniqueId().equals(oniBot))||playerBots.contains(e.getEntity().getUniqueId())))e.setCancelled(true);}
    @EventHandler public void onEntityDeath(EntityDeathEvent e){if(state!=GameState.RUNNING)return;UUID id=e.getEntity().getUniqueId();if(ikimonoIds.remove(id)){clearIkimonoLight(id);handleIkimonoDeath(e);return;}if(disconnectOniProxyOwner.containsKey(id)){UUID owner=disconnectOniProxyOwner.remove(id);disconnectOniProxy.remove(owner);disconnectOniProxyType.remove(id);duoOniBotPassives.remove(id);disconnectOniProxySkillReadyAt.remove(id);disconnectOniProxySkillCycle.remove(id);e.getDrops().clear();e.setDroppedExp(0);boolean botAlive=oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity b&&b.isValid()&&!b.isDead();boolean proxyAlive=disconnectOniProxy.values().stream().anyMatch(x->Bukkit.getEntity(x) instanceof LivingEntity b&&b.isValid()&&!b.isDead());boolean humanOniAlive=oniTeam.stream().anyMatch(x->{Player q=Bukkit.getPlayer(x);return q!=null&&q.isOnline()&&q.getGameMode()!=GameMode.SPECTATOR;});boolean duoMate=duoBotSyntheticOwners.remove(owner);all(duoMate?"&7DUOの相方鬼Botが討滅された……":"&7鬼代行Botが討滅された……");if(!botAlive&&!proxyAlive&&!humanOniAlive)end(false,"&c鬼 討 滅――ぷれいやーの勝利");return;}if(oniBot!=null&&id.equals(oniBot)){e.getDrops().clear();e.setDroppedExp(0);oniBot=null;boolean humanOniAlive=oniTeam.stream().anyMatch(x->{Player q=Bukkit.getPlayer(x);return q!=null&&q.isOnline()&&q.getGameMode()!=GameMode.SPECTATOR;});boolean proxyOniAlive=disconnectOniProxy.values().stream().anyMatch(x->Bukkit.getEntity(x) instanceof LivingEntity pb&&pb.isValid()&&!pb.isDead());if(!humanOniAlive&&!proxyOniAlive)end(false,"&c鬼 討 滅――ぷれいやーの勝利");else all("&7鬼Botが討滅された。残る鬼との戦いは続く……");return;}if(azakujiAllyId!=null&&id.equals(azakujiAllyId)){e.getDrops().clear();e.setDroppedExp(0);clearAzakujiLight();deadPlayerBots.add(id);clearPlayerBotWork(id);all("&6字九字ひろ &e「…すまない、撤退する」");return;}if(playerBots.contains(id)){restorePlayerBotDownEscape(id);checkPredation(e.getEntity());e.getDrops().clear();e.setDroppedExp(0);deadPlayerBots.add(id);clearPlayerBotWork(id);all("&7ぷれいやーBot は鬼に喰われた……");checkPlayerOutcome();}}
    @EventHandler public void onDeath(PlayerDeathEvent e){if(state!=GameState.RUNNING||!participants.contains(e.getPlayer().getUniqueId()))return;Player victim=e.getPlayer();Player killer=victim.getKiller();boolean killedByOni=killer!=null&&isOni(killer.getUniqueId());e.getDrops().clear();e.setDeathMessage(null);if(isTagMode()&&!isOni(victim.getUniqueId())&&(!finalPhase||tagParkourGrandfathered.contains(victim.getUniqueId()))){UUID id=victim.getUniqueId();tagParkourPlayers.add(id);chased.remove(id);leaveChaseBgmNow(victim);Bukkit.getScheduler().runTask(this,()->{victim.spigot().respawn();victim.setGameMode(GameMode.ADVENTURE);victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(10);victim.setHealth(10);Location start=LocationStore.get(getConfig(),"tag-mode.parkour.start");if(start!=null)victim.teleport(start);victim.getInventory().setItem(8,tagNavCompass());victim.sendTitle(cc("&e&l復活挑戦"),cc("&fアスレチックを突破せよ"),5,35,10);});return;}if(!isOni(victim.getUniqueId()))checkPredation(victim);Bukkit.getScheduler().runTask(this,()->{Player p=e.getPlayer();p.spigot().respawn();p.setGameMode(GameMode.SPECTATOR);if(isOni(p.getUniqueId())){oniTeam.remove(p.getUniqueId());boolean botAlive=oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity b&&b.isValid()&&!b.isDead();boolean proxyAlive=disconnectOniProxy.values().stream().anyMatch(x->Bukkit.getEntity(x) instanceof LivingEntity pb&&pb.isValid()&&!pb.isDead());if(!botAlive&&!proxyAlive&&oniTeam.stream().noneMatch(x->{Player q=Bukkit.getPlayer(x);return q!=null&&q.isOnline()&&q.getGameMode()!=GameMode.SPECTATOR;}))end(false,"&c鬼 討 滅――ぷれいやーの勝利");}else{dead.add(p.getUniqueId());downEscapeUntil.remove(p.getUniqueId());restoreDownEscapeVisual(p);chased.remove(p.getUniqueId());leaveChaseBgmNow(p);all(killedByOni?"&7"+p.getName()+" は鬼に喰われた……":"&7"+p.getName()+" は異界に飲み込まれた。");checkPlayerOutcome();}});}
    private LivingEntity spawnDisconnectedOniProxy(Player p){
        if(p==null||!isOni(p.getUniqueId()))return null;
        removeDisconnectedOniProxy(p.getUniqueId(),false);
        Zombie bot=p.getWorld().spawn(p.getLocation(),Zombie.class);bot.setBaby(false);bot.setCustomName(cc("&c"+p.getName()+" &7【鬼代行Bot】"));bot.setCustomNameVisible(true);bot.setPersistent(true);bot.setRemoveWhenFarAway(false);bot.setCanPickupItems(false);bot.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,Integer.MAX_VALUE,0,false,false));
        double max=p.getAttribute(Attribute.GENERIC_MAX_HEALTH)==null?60.0:p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1,max));bot.setHealth(Math.max(1,Math.min(max,p.getHealth())));if(bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)!=null)bot.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(getConfig().getDouble("bot.movement-speed",0.32));if(bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE)!=null)bot.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(getConfig().getDouble("bot.attack-damage",7.0));
        if(bot.getEquipment()!=null){bot.getEquipment().setHelmet(p.getInventory().getHelmet()==null?null:p.getInventory().getHelmet().clone());bot.getEquipment().setChestplate(p.getInventory().getChestplate()==null?null:p.getInventory().getChestplate().clone());bot.getEquipment().setLeggings(p.getInventory().getLeggings()==null?null:p.getInventory().getLeggings().clone());bot.getEquipment().setBoots(p.getInventory().getBoots()==null?null:p.getInventory().getBoots().clone());bot.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand().clone());bot.getEquipment().setHelmetDropChance(0);bot.getEquipment().setChestplateDropChance(0);bot.getEquipment().setLeggingsDropChance(0);bot.getEquipment().setBootsDropChance(0);bot.getEquipment().setItemInMainHandDropChance(0);}
        disconnectOniProxy.put(p.getUniqueId(),bot.getUniqueId());disconnectOniProxyOwner.put(bot.getUniqueId(),p.getUniqueId());disconnectOniProxyType.put(bot.getUniqueId(),oniType);disconnectOniProxySkillReadyAt.put(bot.getUniqueId(),System.currentTimeMillis()+2500L);disconnectOniProxySkillCycle.put(bot.getUniqueId(),0);return bot;
    }
    private void updateDisconnectedOniProxies(){
        if(state!=GameState.RUNNING||disconnectOniProxy.isEmpty())return;long now=System.currentTimeMillis();
        for(UUID botId:new ArrayList<>(disconnectOniProxy.values())){
            if(!(Bukkit.getEntity(botId) instanceof Mob bot)||!bot.isValid()||bot.isDead())continue;
            tickIkimonoBotBuffUse(botId,bot,true);LivingEntity beastTarget=nearestIkimono(bot,12.0);if(beastTarget!=null){bot.setTarget(beastTarget);double bd=bot.getLocation().distance(beastTarget.getLocation());if(bd<=2.5){String key=botId+":ikimono_melee";long ready=cooldowns.getOrDefault(key,0L);if(now>=ready){cooldowns.put(key,now+900);bot.swingMainHand();beastTarget.damage(Math.max(3,getConfig().getDouble("bot.attack-damage",7.0)),bot);}}continue;}
            LivingEntity target=findNearestPlayerSideTarget(bot);if(target==null){bot.setTarget(null);continue;}bot.setTarget(target);
            double d=bot.getLocation().distance(target.getLocation());
            if(playerBots.contains(target.getUniqueId())&&d<=getConfig().getDouble("bot.player-bot-attack-range",2.5)){
                String key=botId+":proxy_melee";long ready=cooldowns.getOrDefault(key,0L);if(now>=ready){cooldowns.put(key,now+Math.max(1,getConfig().getLong("bot.player-bot-attack-cooldown-ticks",20))*50L);bot.swingMainHand();target.damage(getConfig().getDouble("bot.attack-damage",7.0),bot);}
            }
            if(now>=disconnectOniProxySkillReadyAt.getOrDefault(botId,0L))useDisconnectedOniSignature(bot,target,d);
        }
    }
    private void useDisconnectedOniSignature(Mob bot,LivingEntity target,double distance){
        UUID id=bot.getUniqueId();OniType type=disconnectOniProxyType.getOrDefault(id,oniType);int cycle=disconnectOniProxySkillCycle.getOrDefault(id,0);long now=System.currentTimeMillis();long cd=8000L;
        if(type==OniType.DAKKO){
            if(distance<=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0)){double radius=getConfig().getDouble("dakko-skills.fox-fire.radius",7.0),damage=getConfig().getDouble("dakko-skills.fox-fire.fixed-damage",5.0);bot.getWorld().spawnParticle(Particle.FLAME,bot.getLocation().add(0,1,0),70,radius*.45,1,radius*.45,.06);for(LivingEntity q:playerSideTargetsNear(bot,radius)){q.setFireTicks(Math.max(q.getFireTicks(),80));dealOniSkillFixedDamage(q,damage);}}else if(distance>=8&&distance<=24){Location landing=target.getLocation().clone().add(horizontalDirection(target.getLocation()).multiply(-2));bot.teleport(landing);bot.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME,landing,35,.5,1,.5,.04);}cd=12000L;
        }else if(type==OniType.KISHIN){
            if(distance<=getConfig().getDouble("kishin-skills.roar.radius",8.0)&&cycle%2==0){double radius=getConfig().getDouble("kishin-skills.roar.radius",8.0);for(LivingEntity q:playerSideTargetsNear(bot,radius)){dealOniSkillFixedDamage(q,getConfig().getDouble("kishin-skills.roar.fixed-damage",5.0));q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,60,1,false,true));Vector kb=q.getLocation().toVector().subtract(bot.getLocation().toVector());if(kb.lengthSquared()>0)q.setVelocity(kb.normalize().multiply(.65).setY(.22));}bot.getWorld().playSound(bot.getLocation(),Sound.ENTITY_RAVAGER_ROAR,1,1);}else if(distance>4&&distance<16){Vector charge=target.getLocation().toVector().subtract(bot.getLocation().toVector()).normalize().multiply(1.8).setY(.15);bot.setVelocity(charge);}cd=10000L;
        }else if(type==OniType.SHIKKI){
            if(cycle%3==2){bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,100,2,false,true));bot.getWorld().spawnParticle(Particle.CRIT,bot.getLocation().add(0,1,0),30,.4,.7,.4,.06);cd=12000L;}else{Vector leap=target.getLocation().toVector().subtract(bot.getLocation().toVector()).normalize().multiply(1.55);leap.setY(Math.max(.5,leap.getY()+.5));bot.setVelocity(leap);bot.setFallDistance(0);cd=6000L;}
        }else if(type==OniType.YUUKI){
            if(cycle%2==0&&distance>3){Vector back=horizontalDirection(target.getLocation()).multiply(-2.0);Location at=target.getLocation().clone().add(back);bot.teleport(at);bot.getWorld().spawnParticle(Particle.SMOKE_LARGE,at.add(0,1,0),35,.5,.8,.5,.03);}else{for(LivingEntity q:playerSideTargetsNear(bot,5.0))q.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,50,2,false,true));bot.getWorld().spawnParticle(Particle.SQUID_INK,bot.getLocation().add(0,1,0),35,.6,.7,.6,.02);}cd=11000L;
        }else if(type==OniType.KANKI){
            spawnDisconnectedOnigamiMinion(bot,type);cd=15000L;
        }else if(type==OniType.JAKUTSUKI){
            if(distance<=9){for(LivingEntity q:playerSideTargetsNear(bot,5.5)){dealOniSkillFixedDamage(q,6.0);Vector kb=q.getLocation().toVector().subtract(bot.getLocation().toVector());if(kb.lengthSquared()>0)q.setVelocity(kb.normalize().multiply(.8).setY(.25));}bot.getWorld().spawnParticle(Particle.SWEEP_ATTACK,bot.getLocation().add(0,1,0),16,1.5,.5,1.5,.02);}else{Vector dir=target.getEyeLocation().toVector().subtract(bot.getEyeLocation().toVector()).normalize();Location at=bot.getEyeLocation().clone();for(int i=0;i<18;i++){at.add(dir.clone().multiply(1.1));bot.getWorld().spawnParticle(Particle.SQUID_INK,at,3,.15,.15,.15,.01);for(LivingEntity q:playerSideTargetsNearLocation(at,1.25))dealOniSkillFixedDamage(q,1.5);}}cd=14000L;
        }
        if(hasDuoOniPassive(id,OniPassiveSkill.MASTERY))cd=(long)(cd*Math.max(.50,Math.min(1.0,getConfig().getDouble("oni-passive-skills.mastery-cooldown-multiplier",.85))));
        if(hasDuoOniPassive(id,OniPassiveSkill.MOMENTUM))bot.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,30,0,false,true));
        disconnectOniProxySkillCycle.put(id,cycle+1);disconnectOniProxySkillReadyAt.put(id,now+Math.max(3000L,cd));
    }
    private List<LivingEntity> playerSideTargetsNear(LivingEntity hunter,double radius){return playerSideTargetsNearLocation(hunter.getLocation(),radius);}
    private List<LivingEntity> playerSideTargetsNearLocation(Location center,double radius){List<LivingEntity> out=new ArrayList<>();double r2=radius*radius;for(UUID pid:players){Player q=Bukkit.getPlayer(pid);if(q!=null&&!dead.contains(pid)&&!escaped.contains(pid)&&!isDownEscape(pid)&&q.getWorld()==center.getWorld()&&q.getLocation().distanceSquared(center)<=r2)out.add(q);}for(UUID pid:playerBots){if(deadPlayerBots.contains(pid)||escapedPlayerBots.contains(pid)||isDownEscape(pid))continue;if(Bukkit.getEntity(pid) instanceof LivingEntity q&&q.isValid()&&!q.isDead()&&q.getWorld()==center.getWorld()&&q.getLocation().distanceSquared(center)<=r2)out.add(q);}return out;}
    private void spawnDisconnectedOnigamiMinion(Mob owner,OniType ignored){
        Location at=owner.getLocation().clone().add(1.2,0,1.2);Zombie minion=at.getWorld().spawn(at,Zombie.class);minion.setBaby(false);minion.setPersistent(false);minion.setRemoveWhenFarAway(false);minion.setCustomName(cc("&5鬼神の代行召喚鬼"));minion.setCustomNameVisible(false);minion.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(14);minion.setHealth(14);minion.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4);minion.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(.31);LivingEntity target=findNearestPlayerSideTarget(minion);if(target!=null)minion.setTarget(target);Bukkit.getScheduler().runTaskLater(this,()->{if(minion.isValid())minion.remove();},200L);owner.getWorld().playSound(owner.getLocation(),Sound.ENTITY_EVOKER_PREPARE_SUMMON,1,.8f);
    }
    private void removeDisconnectedOniProxy(UUID owner,boolean transferToPlayer){UUID botId=disconnectOniProxy.remove(owner);if(botId==null)return;disconnectOniProxyOwner.remove(botId);disconnectOniProxyType.remove(botId);duoOniBotPassives.remove(botId);disconnectOniProxySkillReadyAt.remove(botId);disconnectOniProxySkillCycle.remove(botId);Entity entity=Bukkit.getEntity(botId);Player p=Bukkit.getPlayer(owner);if(transferToPlayer&&p!=null&&entity instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead()){p.teleport(bot.getLocation());if(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)p.setHealth(Math.max(1,Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),bot.getHealth())));}if(entity!=null)entity.remove();}
    private void restoreDisconnectedOniFromProxy(Player p){if(p!=null)removeDisconnectedOniProxy(p.getUniqueId(),true);}
    private void removeAllDisconnectedOniProxies(){for(UUID owner:new ArrayList<>(disconnectOniProxy.keySet()))removeDisconnectedOniProxy(owner,false);disconnectOniProxy.clear();disconnectOniProxyOwner.clear();disconnectOniProxyType.clear();disconnectOniProxySkillReadyAt.clear();disconnectOniProxySkillCycle.clear();}
    private LivingEntity spawnDisconnectedPlayerProxy(Player p){
        if(p==null||!players.contains(p.getUniqueId())||dead.contains(p.getUniqueId())||escaped.contains(p.getUniqueId()))return null;
        removeDisconnectedPlayerProxy(p.getUniqueId(),false);
        LivingEntity bot=spawnPlayerBot(p.getLocation(),playerBots.size()+1);UUID botId=bot.getUniqueId();
        disconnectPlayerProxy.put(p.getUniqueId(),botId);disconnectProxyOwner.put(botId,p.getUniqueId());
        playerBotSkills.put(botId,selectedSkill.getOrDefault(p.getUniqueId(),PlayerSkill.SPRINT));
        playerBotPassives.put(botId,new LinkedHashSet<>(selectedPassives.getOrDefault(p.getUniqueId(),new LinkedHashSet<>())));
        playerBotPresetNames.put(botId,p.getName()+" 代行");bot.setCustomName(cc("&e"+p.getName()+" &7【代行Bot】"));bot.setCustomNameVisible(true);
        if(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null&&p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(Math.max(1,p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
        bot.setHealth(Math.max(1,Math.min(bot.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),p.getHealth())));
        if(bot.getEquipment()!=null){bot.getEquipment().setHelmet(p.getInventory().getHelmet()==null?null:p.getInventory().getHelmet().clone());bot.getEquipment().setChestplate(p.getInventory().getChestplate()==null?null:p.getInventory().getChestplate().clone());bot.getEquipment().setLeggings(p.getInventory().getLeggings()==null?null:p.getInventory().getLeggings().clone());bot.getEquipment().setBoots(p.getInventory().getBoots()==null?null:p.getInventory().getBoots().clone());bot.getEquipment().setItemInMainHand(p.getInventory().getItemInMainHand().clone());}
        return bot;
    }
    private void removeDisconnectedPlayerProxy(UUID owner,boolean transferToPlayer){UUID botId=disconnectPlayerProxy.remove(owner);if(botId==null)return;disconnectProxyOwner.remove(botId);Entity entity=Bukkit.getEntity(botId);Player p=Bukkit.getPlayer(owner);if(transferToPlayer&&p!=null&&entity instanceof LivingEntity bot&&bot.isValid()&&!bot.isDead()){p.teleport(bot.getLocation());if(p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!=null)p.setHealth(Math.max(1,Math.min(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(),bot.getHealth())));}
        if(entity!=null)entity.remove();playerBots.remove(botId);deadPlayerBots.remove(botId);escapedPlayerBots.remove(botId);clearPlayerBotWork(botId);playerBotSkills.remove(botId);playerBotPassives.remove(botId);playerBotPresetNames.remove(botId);playerBotSkillReadyAt.remove(botId);playerBotSprintUntil.remove(botId);playerBotStamina.remove(botId);playerBotTargetHeart.remove(botId);playerBotLastLocation.remove(botId);playerBotStuckTicks.remove(botId);
    }
    private void restoreDisconnectedPlayerFromProxy(Player p){if(p!=null)removeDisconnectedPlayerProxy(p.getUniqueId(),true);}
    private void resolveDisconnectGrace(UUID id,long expectedDeadline){if(state!=GameState.RUNNING)return;Long current=disconnectGraceUntil.get(id);if(current==null||current.longValue()!=expectedDeadline)return;Player online=Bukkit.getPlayer(id);if(online!=null&&online.isOnline()){disconnectGraceUntil.remove(id);restoreDisconnectedPlayerFromProxy(online);return;}disconnectGraceUntil.remove(id);if(isOni(id)||disconnectOniProxy.containsKey(id)){oniTeam.remove(id);UUID proxyId=disconnectOniProxy.get(id);boolean proxyAlive=proxyId!=null&&Bukkit.getEntity(proxyId) instanceof LivingEntity pb&&pb.isValid()&&!pb.isDead();boolean botAlive=oniBot!=null&&Bukkit.getEntity(oniBot) instanceof LivingEntity b&&b.isValid()&&!b.isDead();boolean otherProxyAlive=disconnectOniProxy.entrySet().stream().anyMatch(en->!en.getKey().equals(id)&&Bukkit.getEntity(en.getValue()) instanceof LivingEntity ob&&ob.isValid()&&!ob.isDead());boolean humanOniAlive=oniTeam.stream().anyMatch(x->{Player q=Bukkit.getPlayer(x);return q!=null&&q.isOnline()&&q.getGameMode()!=GameMode.SPECTATOR;});if(proxyAlive)all("&7切断した鬼の猶予が終了しました。以後は &c鬼代行Bot &7がその鬼枠を引き継ぎます。");else all("&7切断した鬼の再接続猶予が終了しました。");if(!humanOniAlive&&!botAlive&&!proxyAlive&&!otherProxyAlive)end(false,"&c鬼が不在となったため、ぷれいやーの勝利");return;}if(players.contains(id)){UUID proxyId=disconnectPlayerProxy.get(id);boolean proxyAlive=proxyId!=null&&Bukkit.getEntity(proxyId) instanceof LivingEntity b&&b.isValid()&&!b.isDead();dead.add(id);downEscapeUntil.remove(id);if(proxyAlive)all("&7切断したぷれいやーの猶予が終了しました。以後は &e代行Bot &7がその枠を引き継ぎます。");else{all("&7切断したぷれいやーの再接続猶予が終了し、脱落扱いになりました。");removeDisconnectedPlayerProxy(id,false);}checkPlayerOutcome();}}

    @EventHandler public void onQuit(PlayerQuitEvent e){if(state!=GameState.RUNNING)return;Player p=e.getPlayer();UUID id=p.getUniqueId();if(!participants.contains(id)||dead.contains(id)||escaped.contains(id))return;int grace=Math.max(5,getConfig().getInt("disconnect.grace-seconds",60));long deadline=System.currentTimeMillis()+grace*1000L;disconnectGraceUntil.put(id,deadline);chased.remove(id);leaveChaseBgmNow(p);if(players.contains(id)){LivingEntity proxy=spawnDisconnectedPlayerProxy(p);all("&e"+p.getName()+" &7が切断しました。 &f"+grace+"秒 &7の間、"+(proxy==null?"再接続を待機します。":"&e代行Bot &7が行動します。"));}else if(isOni(id)){LivingEntity proxy=spawnDisconnectedOniProxy(p);all("&c"+p.getName()+" &7が切断しました。 &f"+grace+"秒 &7の間、"+(proxy==null?"再接続を待機します。":"&c鬼代行Bot &7が鬼枠を守ります。"));}else all("&e"+p.getName()+" &7が切断しました。 &f"+grace+"秒 &7以内に戻れば試合へ復帰できます。");Bukkit.getScheduler().runTaskLater(this,()->resolveDisconnectGrace(id,deadline),grace*20L);}

    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(a.length==1)return List.of("join","leave","mode","duo","role","skills","skill","gmbook","onimenu","lobbyall","lobbyarea","lobbyblocks","ally","arenasetup","automap","skillchest","onichest","lobbyhide","forceoni","oniblock","practicechest","practiceheart","training","heartmode","marker","markers","chest","chests","finaltest","bgm","bgmtest","bgmstop","botgame","onibotgame","aibotmatch","botmatch","onitest","testbook","testdestroy","set","heart","start","stop","status","exlock","onilock","escape","area","botdifficulty","playerbotextra","errornotify","finalsky","ikimono","azakuji","sp","spshop","unei","pv");if(a.length==2&&a[0].equalsIgnoreCase("unei"))return List.of("add","remove","list","status","play");if(a.length==3&&a[0].equalsIgnoreCase("unei")){List<String> n=new ArrayList<>(List.of("me"));for(Player p:Bukkit.getOnlinePlayers())n.add(p.getName());return n;}if(a.length==2&&a[0].equalsIgnoreCase("duo"))return List.of("botmatch");if(a.length==3&&a[0].equalsIgnoreCase("duo")&&a[1].equalsIgnoreCase("botmatch"))return isJakutsukiLocked()?List.of("dakko","kio","shikki","yuuki","kishin","onigami","kanki"):List.of("dakko","kio","shikki","yuuki","kishin","onigami","kanki","jakutsuki");if(a.length==2&&a[0].equalsIgnoreCase("lobbyarea"))return List.of("pos1","pos2","clear","info");if(a.length==2&&a[0].equalsIgnoreCase("lobbyblocks"))return List.of("material","preview","restore","status");if(a.length==3&&a[0].equalsIgnoreCase("lobbyblocks")&&a[1].equalsIgnoreCase("material"))return List.of("add","addhand","remove","list");if(a.length==2&&a[0].equalsIgnoreCase("area"))return List.of("add","remove","list","show","autogen","accept","acceptall","clearcandidates");if(a.length==2&&a[0].equalsIgnoreCase("botdifficulty"))return List.of("normal","elite","extra","status");if(a.length==2&&a[0].equalsIgnoreCase("playerbotextra"))return List.of("on","off","status");if(a.length==2&&a[0].equalsIgnoreCase("errornotify"))return List.of("on","off","status");if(a.length==3&&a[0].equalsIgnoreCase("area")&&a[1].equalsIgnoreCase("add"))return List.of("updraft","jump_pad","spirit_road","safe_drop");if(a.length==2&&a[0].equalsIgnoreCase("mode"))return List.of("normal","duo","escape","tag","pv");if(a.length==2&&a[0].equalsIgnoreCase("pv"))return List.of("start","stop","status","actor","bot","time","weather","freeze","unfreeze","heal","effect");if(a.length==3&&a[0].equalsIgnoreCase("pv")&&a[1].equalsIgnoreCase("bot"))return List.of("player","oni","clear");if(a.length==3&&a[0].equalsIgnoreCase("pv")&&a[1].equalsIgnoreCase("time"))return List.of("day","noon","sunset","night","midnight");if(a.length==3&&a[0].equalsIgnoreCase("pv")&&a[1].equalsIgnoreCase("weather"))return List.of("clear","rain","thunder");if(a.length==3&&a[0].equalsIgnoreCase("pv")&&a[1].equalsIgnoreCase("effect"))return List.of("hyakki","final","clear");if(a.length==2&&a[0].equalsIgnoreCase("tag"))return List.of("start","forcestart","stop","status","setup","shrine","oniwait","parkour","respawn","area","spranking","spshop","terra","oni","streamer");if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("oni")){List<String> n=new ArrayList<>(List.of("me","clear","status"));for(Player p:Bukkit.getOnlinePlayers())n.add(p.getName());return n;}if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("streamer")){List<String> n=new ArrayList<>(List.of("me","list"));for(Player p:Bukkit.getOnlinePlayers())n.add(p.getName());return n;}if(a.length==4&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("streamer"))return List.of("on","off");if(a.length==4&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("oni"))return isJakutsukiLocked()?List.of("dakko","shikki","yuuki"):List.of("dakko","shikki","yuuki","jakutsuki");if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("terra"))return List.of("gem","gate","status");if(a.length==4&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("terra")&&a[2].equalsIgnoreCase("gem"))return List.of("diamond","gold","emerald","lapis");if(a.length==4&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("terra")&&a[2].equalsIgnoreCase("gate"))return List.of("add","clear","list");if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("shrine"))return List.of("player","oni");if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("parkour"))return List.of("start","goal");if(a.length==3&&a[0].equalsIgnoreCase("tag")&&a[1].equalsIgnoreCase("area"))return List.of("add","remove","list");if(a.length==2&&a[0].equalsIgnoreCase("escape"))return List.of("status","next","progress","region");if(a.length==3&&a[0].equalsIgnoreCase("escape")&&a[1].equalsIgnoreCase("region"))return List.of("pos1","pos2","add","list","remove");if(a.length==2&&a[0].equalsIgnoreCase("role"))return List.of("oni","player","auto");if(a.length==2&&a[0].equalsIgnoreCase("finaltest"))return List.of("clear");if(a.length==2&&a[0].equalsIgnoreCase("finalsky"))return List.of("test","clear","status");if(a.length==2&&a[0].equalsIgnoreCase("ikimono"))return List.of("on","off","status","spawn","clear");if(a.length==2&&a[0].equalsIgnoreCase("azakuji"))return List.of("on","off","status","chance");if(a.length==3&&a[0].equalsIgnoreCase("azakuji")&&a[1].equalsIgnoreCase("chance"))return List.of("5","10","15","20","25","50","100");if(a.length==2&&a[0].equalsIgnoreCase("ally")){List<String> presets=new ArrayList<>(List.of("status","list","clear","IGAMI_KYOYA","AZANAMI_MISAKI","ARIKAWA_FUUKA","AZANAMI_REN","MEDIC","AKASAKA_HIIRO","KAGAYA_RION","AMANAI_IONA"));return presets;}if(a.length==2&&a[0].equalsIgnoreCase("arenasetup"))return List.of("info","clear");if(a.length==2&&a[0].equalsIgnoreCase("automap"))return List.of("maxy");if(a.length==3&&a[0].equalsIgnoreCase("automap")&&a[1].equalsIgnoreCase("maxy"))return List.of("60","70","80","90","100");if(a.length==2&&a[0].equalsIgnoreCase("skill"))return List.of("sprint","invisible","smoke","strike","heal","obsession","landing");if(a.length==2&&a[0].equalsIgnoreCase("heartmode"))return List.of("random","manual");if(a.length==2&&a[0].equalsIgnoreCase("bgm"))return List.of("onigame:chase_tatari","off");if(a.length==2&&a[0].equalsIgnoreCase("set"))return List.of("lobby","player","oni","exit","exit2");if(a.length==2&&(a[0].equalsIgnoreCase("start")||a[0].equalsIgnoreCase("botgame")||a[0].equalsIgnoreCase("onibotgame")||a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch")||a[0].equalsIgnoreCase("onitest")))return isJakutsukiLocked()?List.of("dakko","kio","shikki","yuuki","kishin","onigami","kanki"):List.of("dakko","kio","shikki","yuuki","kishin","onigami","kanki","jakutsuki");if(a.length==2&&a[0].equalsIgnoreCase("exlock"))return List.of("azakuji","jakutsuki","onigami","status");if(a.length==2&&a[0].equalsIgnoreCase("onilock"))return List.of("dakko","kiou","shikki","yuuki","status");if(a.length==3&&a[0].equalsIgnoreCase("onilock"))return List.of("lock","unlock","status");if(a.length==3&&a[0].equalsIgnoreCase("exlock"))return List.of("lock","unlock","status");if(a.length==2&&a[0].equalsIgnoreCase("forceoni")){List<String> names=new ArrayList<>();names.add("clear");for(Player p:Bukkit.getOnlinePlayers())names.add(p.getName());return names;}if(a.length==2&&(a[0].equalsIgnoreCase("oniblock")||a[0].equalsIgnoreCase("practicechest")||a[0].equalsIgnoreCase("practiceheart")))return List.of("clear");if(a.length==2&&a[0].equalsIgnoreCase("training"))return List.of("pos1","pos2","clear","info","dummy","refresh");if(a.length==3&&a[0].equalsIgnoreCase("training")&&a[1].equalsIgnoreCase("dummy"))return List.of("clear");if(a.length==3&&(a[0].equalsIgnoreCase("onibotgame")||a[0].equalsIgnoreCase("aibotmatch")||a[0].equalsIgnoreCase("botmatch")||a[0].equalsIgnoreCase("onitest")))return List.of("1","2","3","4","5","6","7","8");return List.of();}
}
