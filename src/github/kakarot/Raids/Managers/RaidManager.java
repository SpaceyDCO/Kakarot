package github.kakarot.Raids.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Game.GameSession;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.MessageManager;
import noppes.npcs.api.entity.IEntity;
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
     * Attempts to start a new raid
     * This is the main entry point called by commands
     * @param leader The leader of the party
     * @param arenaName The arena to start (must be lowercase)
     */
    public void startGame(Player leader, String arenaName) { //ADD A CHECK TO SEE IF THIS PARTY IS ALREADY IN A GAME + ADD onJoinPartyEvent and onLeavePartyEvent
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
            leader.sendMessage(messageManager.getMessage("arena-not-valid", "arena_name", arenaName));
            plugin.getLogger().severe("-----------------------");
            plugin.getLogger().severe("FATAL ERROR: Scenario " + arena.getScenarioName() + " from arena " + arenaName + " is missing or corrupt!");
            plugin.getLogger().severe("-----------------------");
            return;
        }
        Scenario scenario = scenarioOptional.get();
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
        for(UUID member : gameSession.getParty().getMembers()) {
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
}
