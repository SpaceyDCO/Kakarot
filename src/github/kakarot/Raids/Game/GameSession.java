package github.kakarot.Raids.Game;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Helpers.SerializableLocation;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Raids.Scenario.SpawnInfo;
import github.kakarot.Raids.Scenario.Wave;
import github.kakarot.Tools.CC;
import lombok.Getter;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.scripted.NpcAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;


import java.util.*;
import java.util.logging.Level;

public class GameSession {
    @Getter private GameState currentState;
    @Getter private int currentWaveNumber;
    @Getter private final Set<UUID> alivePlayers;
    @Getter private final Set<UUID> aliveNpcs = new HashSet<>();
    @Getter private final Map<UUID, Location> originalLocation = new HashMap<>();
    private final Main plugin;
    private final RaidManager raidManager;
    @Getter private final Party party;
    @Getter private final Arena arena;
    private final Scenario scenario;
    private IWorld world;
    private BukkitTask activeTask = null;
    private static final String RAID_PREFIX = "&7[&9Raids&7]";

    public GameSession(Main plugin, RaidManager raidManager, Party party, Arena arena, Scenario scenario) {
        this.plugin = plugin;
        this.raidManager = raidManager;
        this.party = party;
        this.arena = arena;
        this.scenario = scenario;

        this.alivePlayers = new HashSet<>(party.getMembers());
        this.currentWaveNumber = 0;
        this.currentState = GameState.LOBBY;
    }

    public void initializeGame() {
        party.broadcast(CC.translate(RAID_PREFIX + " &9Game starting in arena &b" + arena.getArenaName()));
        Location spawnLocation = arena.getPlayerSpawnLocation().toBukkitLocation(arena.getWorldName());
        if(spawnLocation == null) {
            party.broadcast(CC.translate(RAID_PREFIX + " &9World for this arena could not be found, aborting game...\n&9Report this to an admin."));
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
                party.broadcast(CC.translate(RAID_PREFIX + " &9Error: Could not validate arena's world.\n&9This might have been caused due to players leaving the arena's designed world."));
                endGame(false);
                return;
            }
            NpcAPI api = (NpcAPI) NpcAPI.Instance();
            if(api == null) {
                party.broadcast(CC.translate(RAID_PREFIX + " &9CustomNPCs+ API not found, aborting game...\n&9Report this to an admin."));
                plugin.getLogger().severe("Could not get CustomNpcs+ API, aborting arena " + this.arena.getArenaName());
                endGame(false);
                return;
            }
            this.world = api.getPlayer(refPlayer.getName()).getWorld();
            final int FIRST_WAVE_COOLDOWN_SECONDS = 10;
            party.broadcast(CC.translate(RAID_PREFIX + " &9First wave will start in &b" + FIRST_WAVE_COOLDOWN_SECONDS + " &9seconds..."));
            this.activeTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Optional<Wave> firstWave = this.scenario.getNextWave(0);
                if(firstWave.isPresent()) {
                    startWave(firstWave.get());
                }else {
                    party.broadcast(CC.translate(RAID_PREFIX + " &cERROR: &9This scenario does not have Wave 1 defined. Aborting game...\n&9Report this to an admin."));
                    endGame(false);
                }
            }, 20L * FIRST_WAVE_COOLDOWN_SECONDS);
        }, 5L);
    }

    private void startWave(Wave wave) {
        this.currentState = GameState.WAVE_IN_PROGRESS;
        this.currentWaveNumber = wave.getWaveNumber();
        party.broadcast(CC.translate(wave.getStartMessage()));
        if(this.world == null) {
            party.broadcast(CC.translate(RAID_PREFIX + " &9FATAL ERROR: World context not found. Aborting game...\n&9Report this to an admin."));
            endGame(false);
            return;
        }
        for(SpawnInfo spawnInfo : wave.getSpawns()) {
            SerializableLocation serializableLocation = arena.getNpcSpawnPoint(spawnInfo.getSpawnPointName());
            if(serializableLocation == null) {
                plugin.getLogger().warning("In arena " + arena.getArenaName() + ", scenario " + arena.getScenarioName() + ", wave " + currentWaveNumber + ": Could not find NPC Spawn point named " + spawnInfo.getSpawnPointName() + ". Skipping this spawn.");
                continue;
            }
            Location spawnLoc = serializableLocation.toBukkitLocation(arena.getWorldName());
            for(int i = 0; i < spawnInfo.getCount(); i++) {
                try {
                    IEntity<?> npc = this.world.spawnClone((int) spawnLoc.getX(), (int) spawnLoc.getY(), (int) spawnLoc.getZ(), spawnInfo.getNpcTab(), spawnInfo.getNpcID(), true);
                    if(npc != null) {
                        if(npc instanceof ICustomNpc) {
                            ICustomNpc<?> cNpc = (ICustomNpc<?>) npc;
                            aliveNpcs.add(UUID.fromString(cNpc.getUniqueID()));
                        }else plugin.getLogger().warning("Could not spawn NPC " + spawnInfo.getNpcID() + ". Perhaps it's not a CustomNpc?. Skipping this entry...");
                    }else plugin.getLogger().warning("Could not spawn NPC " + spawnInfo.getNpcID() + " for wave " + this.currentWaveNumber + ". It might not exist in the NPC cloner tab. Skipping this entry...");
                }catch(Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "An unexpected error ocurred while trying to spawn NPC " + spawnInfo.getNpcID() + " in wave " + this.currentWaveNumber, e);
                }
            }
        }
        if(aliveNpcs.isEmpty()) {
            party.broadcast(CC.translate(RAID_PREFIX + " &9No enemies were spawned this wave (maybe there was an error).\n&9Report this to an admin."));
            party.broadcast(CC.translate(RAID_PREFIX + " &9Starting next wave..."));
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
            party.broadcast(CC.translate(RAID_PREFIX + " &9All waves have been cleared..."));
            endGame(true);
            return;
        }
        Wave next = nextWave.get();
        int cooldown = scenario.getWaveCooldownInSeconds();
        party.broadcast(CC.translate(RAID_PREFIX + " &9Wave " + this.currentWaveNumber + " cleared!. Starting next wave..."));
        party.broadcast(CC.translate(RAID_PREFIX + " &9Next wave begins in " + cooldown + " seconds."));
        this.activeTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            startWave(next);
        }, 20L * cooldown);
    }
    private boolean isFinalWave() {
        return this.currentWaveNumber >= this.scenario.getTotalWaves();
    }
    private void endGame(boolean victory) {
        if(this.currentState == GameState.ENDING) return;
        this.currentState = GameState.ENDING;
        if(victory) {
            party.broadcast(CC.translate("&6========== VICTORY! =========="));
            party.broadcast(CC.translate("&eYou have defeated all the enemies!"));
            party.broadcast(CC.translate("&6=================================="));
        }else {
            party.broadcast(CC.translate("&4========== DEFEAT =========="));
            party.broadcast(CC.translate("&7Your party has been overwhelmed. Get stronger and try again."));
            party.broadcast(CC.translate("&4=================================="));
        }
        if(this.activeTask != null) {
            this.activeTask.cancel();
            this.activeTask = null;
        }
        for(UUID npcId : aliveNpcs) {
            net.minecraft.entity.Entity mcEntity = this.world.getMCWorld().func_152378_a(npcId);
            if(mcEntity != null) {
                IEntity<?> npc = NpcAPI.Instance().getIEntity(mcEntity);
                if(npc != null) npc.despawn();
            }
        }
        aliveNpcs.clear();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for(UUID playerID : party.getMembers()) {
                Player player = Bukkit.getPlayer(playerID);
                if(player != null && player.isOnline()) {
                    Location ogLoc = originalLocation.get(playerID);
                    if(ogLoc != null) {
                        player.teleport(ogLoc);
                    }else {
                        player.performCommand("/warp spawn"); //Send player to spawn in case ogLoc wasn't found
                    }
                    originalLocation.remove(playerID);
                }
            }
            raidManager.endGame(this);
        }, 20L * 10);
    }
    //EVENTS
    public void onNpcDied(UUID npcid) {
        if(this.currentState != GameState.WAVE_IN_PROGRESS) return;
        aliveNpcs.remove(npcid);
        if(aliveNpcs.isEmpty()) startWaveCooldown(isFinalWave());
        else party.broadcast(CC.translate(RAID_PREFIX + " &9Enemies remaining: &b" + alivePlayers.size()));
    }
    public void onPlayerDied(Player player) {
        party.broadcast(CC.translate(RAID_PREFIX + " &b" + player.getName() + " &9has been defeated."));
        alivePlayers.remove(player.getUniqueId());
        Location spectatorSpawn = arena.getSpectatorSpawn().toBukkitLocation(arena.getWorldName());
        if(spectatorSpawn != null) player.teleport(spectatorSpawn);
        checkLossCondition();
    }
    public void onPlayerQuit(Player player) {
        party.broadcast(CC.translate(RAID_PREFIX + " &b" + player.getName() + " &9has disconnected."));
        alivePlayers.remove(player.getUniqueId());
        checkLossCondition();
    }
    //EVENTS
    private void checkLossCondition() {
        if(alivePlayers.isEmpty()) {
            party.broadcast(CC.translate(RAID_PREFIX + " &9All players have been defeated..."));
            endGame(false);
        }
    }
}
