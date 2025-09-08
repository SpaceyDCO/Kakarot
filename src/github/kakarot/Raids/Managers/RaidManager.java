package github.kakarot.Raids.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Game.GameSession;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Tools.MessageManager;
import noppes.npcs.api.entity.IEntity;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RaidManager {
    private final Map<String, GameSession> activeSessionsByArena = new HashMap<>();
    private final Map<UUID, GameSession> activeSessionsByPlayer = new HashMap<>();
    private final Main plugin;
    private final ConfigManager configManager;
    private final IPartyManager partyManager;
    private final MessageManager messageManager;
    public static final String RAID_PREFIX = "&7[&cRaids&7]";
    public RaidManager(Main plugin, ConfigManager configManager, IPartyManager partyManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.partyManager = partyManager;
        this.messageManager = messageManager;
    }

    /**
     * Checks whether a player can enter an arena or not (To be used with CustomNPCs)
     * @param pUUID The player's UUID
     * @param arenaName The arena to check join ability
     * @return False if the arena is in use, or if the player is locked to another instance. True otherwise
     */
    public boolean canPlayerEnter(String pUUID, String arenaName) {
        Player player = Bukkit.getPlayer(UUID.fromString(pUUID));
        if(activeSessionsByArena.containsKey(arenaName.toLowerCase())) return false;
        return !activeSessionsByPlayer.containsKey(player.getUniqueId());
    }
    /**
     * Overload method to use with CustomNPCs scripting tool
     * @param leaderUUID The UUID of the party leader
     * @param arenaName The arena file name to start (must be lowercase)
     */
    public void startGame(String leaderUUID, String arenaName) {
        Player leader = Bukkit.getPlayer(UUID.fromString(leaderUUID));
        if(leader != null) startGame(leader, arenaName);
        else {
            throw new RuntimeException("Can't start raid game. Player for leaderUUID does not exist.");
        }
    }
    /**
     * Attempts to start a new raid
     * This is the main entry point called by commands
     * @param leader The leader of the party
     * @param arenaName The arena to start (must be lowercase)
     */
    public void startGame(Player leader, String arenaName) {
        plugin.getServer().getConsoleSender().sendMessage("Player: " + leader.getName());
        if(!partyManager.isInParty(leader)) {
            leader.sendMessage(messageManager.getMessage("system.not-in-party"));
            return;
        }
        Optional<Party> partyOptional = partyManager.getParty(leader);
        if(!partyOptional.isPresent() || !partyOptional.get().isLeader(leader.getUniqueId())) {
            leader.sendMessage(messageManager.getMessage("system.not-party-leader"));
            return;
        }
        Party party = partyOptional.get();
        for(UUID member : party.getMembers()) {
            if(this.activeSessionsByPlayer.containsKey(member)) { //Member of the party is already in a game
                Player player = Bukkit.getPlayer(member);
                if(player != null) {
                    party.broadcast(messageManager.getMessage("system.already-in-game", "player_name", player.getName()));
                }else {
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member);
                    if(offlinePlayer != null) party.broadcast(messageManager.getMessage("system.already-in-game", "player_name", offlinePlayer.getName()));
                }
                return;
            }
        }
        Optional<Arena> arenaOptional = configManager.getArena(arenaName);
        if(!arenaOptional.isPresent()) {
            leader.sendMessage(messageManager.getMessage("errors.arena-not-found", "arena_name", arenaName));
            return;
        }
        Arena arena = arenaOptional.get();
        if(activeSessionsByArena.containsKey(arenaName)) {
            leader.sendMessage(messageManager.getMessage("system.arena-in-use"));
            return;
        }
        Optional<Scenario> scenarioOptional = configManager.getScenario(arena.getScenarioName());
        if(!scenarioOptional.isPresent()) {
            leader.sendMessage(messageManager.getMessage("errors.arena-not-valid", "arena_name", arenaName));
            plugin.getLogger().severe("-----------------------");
            plugin.getLogger().severe("FATAL ERROR: Scenario " + arena.getScenarioName() + " from arena " + arenaName + " is missing or corrupt!");
            plugin.getLogger().severe("-----------------------");
            return;
        }
        Scenario scenario = scenarioOptional.get();
        if(party.getSize() > arena.getMaxPlayers()) {
            leader.sendMessage(messageManager.getMessage("system.max-players-exceeded", "count", String.valueOf(arena.getMaxPlayers())));
            return;
        }
        plugin.getLogger().info("Starting a new game in arena " + arenaName + " with scenario " + scenario.getScenarioName() + ".\nParty led by: " + leader.getName());
        GameSession gameSession = new GameSession(plugin, arenaName, this, party, arena, scenario, messageManager);
        activeSessionsByArena.put(arenaName, gameSession);
        for(UUID member : party.getMembers()) {
            activeSessionsByPlayer.put(member, gameSession);
        }
        gameSession.initializeGame();
    }

    /**
     * Cleans up a game session
     * Called when a game ends
     * @param gameSession The session to be cleared
     */
    public void endGame(GameSession gameSession) {
        plugin.getLogger().info("Ending game in arena " + gameSession.getArenaFileName() + "...");
        activeSessionsByArena.remove(gameSession.getArenaFileName());
        for(UUID member : gameSession.getPlayers()) {
            activeSessionsByPlayer.remove(member);
        }
    }

    /**
     * Cleans up remaining NPCs in the Arenas
     * Usually called when plugin gets disabled
     */
    public void cleanupArenas() {
        plugin.getLogger().info("Cleaning up npcs from raids...");
        int count = 0;
        for(GameSession session : getAllActiveSessions()) {
            if(session.getAliveNpcs().isEmpty()) continue;
            for(int npcID : session.getAliveNpcs()) {
                IEntity<?> entity = session.getWorld().getEntityByID(npcID);
                if(entity != null) {
                    entity.despawn();
                    count++;
                }
            }
        }
        plugin.getLogger().info("Cleaned up " + count + " npc(s) from arenas.");
    }

    /**
     * Gets a session linked to a player
     * @param player Player whose session is needed
     * @return An optional containing the GameSession
     */
    public Optional<GameSession> getSessionByPlayer(UUID player) {
        return Optional.ofNullable(activeSessionsByPlayer.get(player));
    }

    /**
     * Gets a session linked to an arena
     * @param arenaName Arena whose session is needed
     * @return An optional containing the GameSession
     */
    public Optional<GameSession> getSessionByArena(String arenaName) {
        return Optional.ofNullable(activeSessionsByArena.get(arenaName));
    }

    /**
     * Gets all active sessions
     * @return A collection with all GameSession currently running
     */
    public Collection<GameSession> getAllActiveSessions() {
        return this.activeSessionsByArena.values();
    }

    /**
     * Attempts to reload an arena
     * won't be reloaded if it's being used
     * @param sender The command sender (for messages)
     * @param arenaName The arena file to reload
     */
    public void attemptReloadArena(CommandSender sender, String arenaName) {
        String lowerCaseArenaName = arenaName.toLowerCase();
        if(activeSessionsByArena.containsKey(lowerCaseArenaName)) {
            sender.sendMessage("Can't reload this arena because a session is currently active.");
            return;
        }
        boolean success = configManager.reloadArena(arenaName);
        if(success) sender.sendMessage("Arena " + arenaName + " loaded successfully.");
        else sender.sendMessage("There was an error loading arena " + arenaName);
    }

    /**
     * Attempts to reload a scenario
     * won't be reloaded if it's being used
     * @param sender The command sender (for messages)
     * @param scenarioName The scenario file to reload
     */
    public void attemptReloadScenario(CommandSender sender, String scenarioName) {
        String lowerCaseScenarioName = scenarioName.toLowerCase();
        boolean isScenarioInUse = activeSessionsByArena.values().stream()
                .anyMatch(session -> session.getArena().getScenarioName().equalsIgnoreCase(lowerCaseScenarioName));
        if(isScenarioInUse) {
            sender.sendMessage("Cannot reload scenario " + scenarioName + ". Its currently in use.");
            plugin.getLogger().warning("Tried to reload scenario " + scenarioName + " but failed because it's currently in use.");
            return;
        }
        boolean success = configManager.reloadScenario(lowerCaseScenarioName);
        if(success) sender.sendMessage("Scenario " + scenarioName + " loaded successfully.");
        else sender.sendMessage("There was an error loading scenario " + scenarioName);
    }
}
