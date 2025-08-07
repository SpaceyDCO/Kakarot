package github.kakarot.Raids.Game;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Helpers.CountdownHelper;
import github.kakarot.Raids.Helpers.SerializableLocation;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Raids.Scenario.SpawnInfo;
import github.kakarot.Raids.Scenario.Wave;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.MessageManager;
import lombok.Getter;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.scripted.NpcAPI;
import noppes.npcs.scripted.event.NpcEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import java.util.logging.Level;
import static github.kakarot.Raids.Managers.RaidManager.RAID_PREFIX;

public class GameSession {
    private MessageManager messageManager;
    @Getter private GameState currentState;
    @Getter private int currentWaveNumber;
    @Getter private final String arenaFileName;
    @Getter private final Set<UUID> alivePlayers;
    @Getter private final Set<Integer> aliveNpcs = new HashSet<>();
    @Getter private final Map<UUID, Location> originalLocation = new HashMap<>();
    private long lastNpcKillTime = 0;
    private final Main plugin;
    private final RaidManager raidManager;
    @Getter private final Party party;
    @Getter private final Arena arena;
    private final Scenario scenario;
    @Getter private IWorld world;
    private BukkitTask activeTask = null;
    private BukkitTask watchdogTask = null;
    private final int inactivityCooldownInMinutes = 15;

    public GameSession(Main plugin, String arenaFileName, RaidManager raidManager, Party party, Arena arena, Scenario scenario, MessageManager messageManager) {
        this.plugin = plugin;
        this.arenaFileName = arenaFileName;
        this.raidManager = raidManager;
        this.party = party;
        this.arena = arena;
        this.scenario = scenario;

        this.alivePlayers = new HashSet<>(party.getMembers());
        this.currentWaveNumber = 0;
        this.currentState = GameState.LOBBY;
        this.messageManager = messageManager;
    }

    public void initializeGame() {
        party.broadcast(messageManager.getMessage("game.starting", "arena_name", arena.getArenaName()));
        Location spawnLocation = arena.getPlayerSpawnLocation().toBukkitLocation(arena.getWorldName());
        if(spawnLocation == null) {
            party.broadcast(messageManager.getMessage("errors.world-not-found"));
            raidManager.endGame(this);
            return;
        }
        for(UUID member : party.getMembers()) {
            Player player = Bukkit.getPlayer(member);
            if(player != null && player.isOnline()) {
                originalLocation.put(member, player.getLocation());
                player.teleport(spawnLocation);
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player refPlayer = Bukkit.getPlayer(party.getLeader());
            if(refPlayer == null) {
                refPlayer = party.getMembers().stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .findAny()
                        .orElse(null);
            }
            if(refPlayer == null || !refPlayer.getWorld().getName().equals(this.arena.getWorldName())) {
                party.broadcast(messageManager.getMessage("errors.world-validation-failed"));
                endGame(false);
                return;
            }
            NpcAPI api = (NpcAPI) NpcAPI.Instance();
            if(api == null) {
                party.broadcast(messageManager.getMessage("errors.cnpc-api-not-found"));
                plugin.getLogger().severe("Could not get CustomNpcs+ API, aborting arena " + this.getArenaFileName());
                endGame(false);
                return;
            }
            this.world = api.getPlayer(refPlayer.getName()).getWorld();
            final int FIRST_WAVE_COOLDOWN_SECONDS = 10;
            party.broadcast(messageManager.getMessage("inactivity-warning", "minutes", String.valueOf(this.inactivityCooldownInMinutes)));
            party.broadcast(messageManager.getMessage("wave.first-wave-countdown", "count", String.valueOf(FIRST_WAVE_COOLDOWN_SECONDS)));
            this.activeTask = new CountdownHelper(plugin).startCountdown(FIRST_WAVE_COOLDOWN_SECONDS, (remaining) -> {
                String plural = remaining > 1 ? "s" : "";
                party.broadcast(messageManager.getMessage("wave.countdown-tick", "count", String.valueOf(remaining), "plural_s", plural));
                party.playSound(Sound.NOTE_BASS, 1f, 1f);
            }, () -> {
                Optional<Wave> firstWave = this.scenario.getNextWave(0);
                if(firstWave.isPresent()) {
                    startWave(firstWave.get());
                    party.playSound(Sound.ORB_PICKUP, 1, 1);
                    this.watchdogTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if(currentState == GameState.ENDING) {
                                this.cancel();
                                return;
                            }
                            if(currentState == GameState.WAVE_IN_PROGRESS) {
                                long timeInMillis = inactivityCooldownInMinutes * 60 * 1000;
                                long timeSinceLastKill = System.currentTimeMillis() - lastNpcKillTime;
                                if(timeSinceLastKill > timeInMillis) {
                                    party.broadcast(messageManager.getMessage("system.inactivity-end"));
                                    for(Player player : Bukkit.getOnlinePlayers()) {
                                        if(player != null) player.sendMessage(messageManager.getMessage("inactivity-reset-broadcast", "arena_name", arena.getArenaName()));
                                    }
                                    endGame(false);
                                    this.cancel();
                                }
                            }
                        }
                    }.runTaskTimer(plugin, 20L * 60, 20L * 60);
                }else {
                    party.broadcast(messageManager.getMessage("errors.no-wave-one"));
                    endGame(false);
                }
            });
        }, 5L);
    }

    private void startWave(Wave wave) {
        this.currentState = GameState.WAVE_IN_PROGRESS;
        this.currentWaveNumber = wave.getWaveNumber();
        party.broadcast(CC.translate(wave.getStartMessage()));
        if(this.world == null) {
            party.broadcast(messageManager.getMessage("errors.world-context-not-found"));
            endGame(false);
            return;
        }
        for(SpawnInfo spawnInfo : wave.getSpawns()) {
            SerializableLocation serializableLocation = arena.getNpcSpawnPoint(spawnInfo.getSpawnPointName());
            if(serializableLocation == null) {
                plugin.getLogger().warning("In arena " + this.getArenaFileName() + ", scenario " + arena.getScenarioName() + ", wave " + currentWaveNumber + ": Could not find NPC Spawn point named " + spawnInfo.getSpawnPointName() + ". Skipping this spawn.");
                continue;
            }
            Location spawnLoc = serializableLocation.toBukkitLocation(arena.getWorldName());
            for(int i = 0; i < spawnInfo.getCount(); i++) {
                try {
                    IEntity<?> npc = this.world.spawnClone((int) spawnLoc.getX(), (int) spawnLoc.getY(), (int) spawnLoc.getZ(), spawnInfo.getNpcTab(), spawnInfo.getNpcID(), true);
                    if(npc != null) {
                        if(npc instanceof ICustomNpc) {
                            ICustomNpc<?> cNpc = (ICustomNpc<?>) npc; //IDEAS: SET NPC'S JOB TO CHUNK LOADER TO PREVENT CHUNK UNLOADING
                            cNpc.setStoredData("SPACEY_ARENA_SYSTEM_NPC", this.arenaFileName);
                            aliveNpcs.add(cNpc.getEntityId());
                        }else plugin.getLogger().warning("Could not spawn NPC " + spawnInfo.getNpcID() + ". Perhaps it's not a CustomNpc?. Skipping this entry...");
                    }else plugin.getLogger().warning("Could not spawn NPC " + spawnInfo.getNpcID() + " for wave " + this.currentWaveNumber + ". It might not exist in the NPC cloner tab. Skipping this entry...");
                }catch(Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "An unexpected error ocurred while trying to spawn NPC " + spawnInfo.getNpcID() + " in wave " + this.currentWaveNumber, e);
                }
            }
        }
        this.lastNpcKillTime = System.currentTimeMillis();
        if(aliveNpcs.isEmpty()) {
            party.broadcast(messageManager.getMessage("errors.no-enemies-spawned"));
            startWaveCooldown(isFinalWave());
        }
    }
    private void startWaveCooldown(boolean isFinalWave) {
        this.currentState = GameState.WAVE_COOLDOWN;
        if(isFinalWave) {
            endGame(true);
            return;
        }
        Optional<Wave> nextWave = scenario.getNextWave(this.currentWaveNumber);
        if(!nextWave.isPresent()) { //Just in case
            party.broadcast(messageManager.getMessage("wave.all-cleared"));
            endGame(true);
            return;
        }
        Wave next = nextWave.get();
        int cooldown = scenario.getWaveCooldownInSeconds();
        party.broadcast(messageManager.getMessage("wave.cleared", "wave", String.valueOf(this.currentWaveNumber)));
        party.broadcast(messageManager.getMessage("wave.next-wave-countdown", "count", String.valueOf(cooldown)));
        this.activeTask = new CountdownHelper(plugin).startCountdown(cooldown, (remaining) -> {
            String plural = remaining > 1 ? "s" : "";
            party.broadcast(messageManager.getMessage("wave.countdown-tick", "count", String.valueOf(remaining), "plural_s", plural));
            party.playSound(Sound.NOTE_BASS, 1f, 1f);
        }, () -> {
            startWave(next);
            party.playSound(Sound.ORB_PICKUP, 1f, 1f);
        });
    }
    private boolean isFinalWave() {
        return this.currentWaveNumber >= this.scenario.getTotalWaves();
    }
    private void endGame(boolean victory) {
        if(this.currentState == GameState.ENDING) return;
        this.currentState = GameState.ENDING;
        if(victory) {
            party.broadcast(messageManager.getMessage("game.victory-header"));
            party.broadcast(messageManager.getMessage("game.victory-body"));
            party.broadcast(messageManager.getMessage("game.victory-footer"));
            new CountdownHelper(plugin).startCountdown(10, party::spawnRandomFirework);
        }else {
            party.broadcast(messageManager.getMessage("game.defeat-header"));
            party.broadcast(messageManager.getMessage("game.defeat-body"));
            party.broadcast(messageManager.getMessage("game.defeat-footer"));
            party.playSound(Sound.WITHER_DEATH, 1f, 1f);
        }
        if(this.activeTask != null) {
            this.activeTask.cancel();
            this.activeTask = null;
        }
        if(this.watchdogTask != null) {
            this.watchdogTask.cancel();
            this.watchdogTask = null;
        }
        for(Integer npcID : aliveNpcs) {
            IEntity<?> entity = this.world.getEntityByID(npcID);
            if(entity != null) {
                entity.despawn();
            }
        }
        aliveNpcs.clear();
        plugin.getLogger().info("Attempting to teleport players to their original location...");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for(UUID playerID : party.getMembers()) {
                Player player = Bukkit.getPlayer(playerID);
                if(player != null && player.isOnline()) {
                    Location ogLoc = originalLocation.get(playerID);
                    if(ogLoc != null) player.teleport(ogLoc);
                    else {
                        player.performCommand("/warp spawn"); //Send player to spawn in case ogLoc wasn't found
                        plugin.getLogger().info("Sent player " + player.getName() + " back to spawn. (Original location was not found)");
                    }
                    originalLocation.remove(playerID);
                }
            }
            raidManager.endGame(this);
        }, 20L * 10);
    }
    //EVENTS
    public void onNpcDamaged(NpcEvent.DamagedEvent event) {
        if(!(event.getDamageSource().getTrueSource() instanceof IPlayer)) return;
        Player player = Bukkit.getPlayer(UUID.fromString(event.getDamageSource().getTrueSource().getUniqueID()));
        if(player == null) return;
        if(!alivePlayers.contains(player.getUniqueId())) {
            event.setCanceled(true);
            player.sendMessage(messageManager.getMessage("system.outsider-damage-blocked"));
        }
    }
    public void onNpcDied(int npcid) {
        if(this.currentState != GameState.WAVE_IN_PROGRESS) return;
        aliveNpcs.remove(npcid);
        this.lastNpcKillTime = System.currentTimeMillis();
        if(aliveNpcs.isEmpty()) startWaveCooldown(isFinalWave());
        else party.broadcast(messageManager.getMessage("wave.enemies-remaining", "count", String.valueOf(aliveNpcs.size())));
    }
    public void onPlayerDied(Player player) {
        party.broadcast(messageManager.getMessage("player.defeated", "player_name", player.getName()));
        party.playSound(Sound.AMBIENCE_THUNDER, 1f, 1f);
        alivePlayers.remove(player.getUniqueId());
        Location spectatorSpawn = arena.getSpectatorSpawn().toBukkitLocation(arena.getWorldName());
        if(spectatorSpawn != null) player.teleport(spectatorSpawn);
        checkLossCondition();
    }
    public void onPlayerQuit(Player player) {
        party.broadcast(messageManager.getMessage("player.disconnected", "player_name", player.getName()));
        Location ogLoc = originalLocation.get(player.getUniqueId());
        if(ogLoc != null) player.teleport(ogLoc);
        else {
            player.performCommand("/warp spawn"); //Send player to spawn in case ogLoc wasn't found
            plugin.getLogger().info("Sent player " + player.getName() + " back to spawn. (Original location was not found)");
        }
        alivePlayers.remove(player.getUniqueId());
        originalLocation.remove(player.getUniqueId());
        checkLossCondition();
    }
    public void onPlayerLeftParty(Player player) {
        party.broadcast(messageManager.getMessage("player.left-party", "player_name", player.getName()));
        Location ogLoc = originalLocation.get(player.getUniqueId());
        if(ogLoc != null) player.teleport(ogLoc);
        else {
            player.performCommand("/warp spawn"); //Send player to spawn in case ogLoc wasn't found
            plugin.getLogger().info("Sent player " + player.getName() + " back to spawn. (Original location was not found)");
        }
        alivePlayers.remove(player.getUniqueId());
        originalLocation.remove(player.getUniqueId());
        checkLossCondition();
    }
    //EVENTS
    private void checkLossCondition() {
        if(alivePlayers.isEmpty()) {
            party.broadcast(messageManager.getMessage("all-defeated"));
            endGame(false);
        }
    }
}
