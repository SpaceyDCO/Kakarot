package github.kakarot.Raids.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Game.GameSession;
import github.kakarot.Raids.Scenario.Scenario;
import github.kakarot.Tools.CC;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static github.kakarot.Parties.Managers.PartyManager.PARTY_PREFIX;

public class RaidManager {
    private final Map<String, GameSession> activeSessionsByArena = new HashMap<>();
    private final Map<UUID, GameSession> activeSessionsByPlayer = new HashMap<>();
    private final Main plugin;
    private final ConfigManager configManager;
    private final IPartyManager partyManager;
    public RaidManager(Main plugin, ConfigManager configManager, IPartyManager partyManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.partyManager = partyManager;
    }

    /**
     * Attempts to start a new raid
     * This is the main entry point called by commands
     * @param leader The leader of the party
     * @param arenaName The arena to start (must be lowercase)
     */
    public void startGame(Player leader, String arenaName) {
        if(!partyManager.isInParty(leader)) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9You must be in a party to start a game."));
            return;
        }
        Optional<Party> partyOptional = partyManager.getParty(leader);
        if(!partyOptional.isPresent() || partyOptional.get().isLeader(leader.getUniqueId())) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9You must be the party leader to start a game."));
            return;
        }
        Party party = partyOptional.get();
        Optional<Arena> arenaOptional = configManager.getArena(arenaName);
        if(!arenaOptional.isPresent()) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Arena &b" + arenaName + " &9not found."));
            return;
        }
        Arena arena = arenaOptional.get();
        if(activeSessionsByArena.containsKey(arenaName)) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Can't start game.\nThis arena is currently in use."));
            return;
        }
        Optional<Scenario> scenarioOptional = configManager.getScenario(arena.getScenarioName());
        if(!scenarioOptional.isPresent()) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Scenario &b" + arena.getScenarioName() + " &9for arena &b" + arenaName + " &9not found.\n&9Please contact an admin."));
            plugin.getLogger().severe("-----------------------");
            plugin.getLogger().severe("FATAL ERROR: Scenario " + arena.getScenarioName() + " from arena " + arenaName + " is missing or corrupt!");
            plugin.getLogger().severe("-----------------------");
            return;
        }
        Scenario scenario = scenarioOptional.get();
        plugin.getLogger().info("Starting a new game in arena " + arenaName + " with scenario " + scenario.getScenarioName() + ".\nParty led by: " + leader.getName());
        GameSession gameSession = new GameSession(plugin, this, party, arena, scenario);
        activeSessionsByArena.put(arenaName.toLowerCase(), gameSession);
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
        plugin.getLogger().info("Ending game in arena " + gameSession.getArena().getArenaName() + ".");
        activeSessionsByArena.remove(gameSession.getArena().getArenaName().toLowerCase());
        for(UUID member : gameSession.getParty().getMembers()) {
            activeSessionsByPlayer.remove(member);
        }
    }

    /**
     * Gets a session linked to a player
     * @param player Player whose session is needed
     * @return An optional containing the GameSession
     */
    public Optional<GameSession> getSessionByPlayer(UUID player) {
        return Optional.ofNullable(activeSessionsByPlayer.get(player));
    }
}
