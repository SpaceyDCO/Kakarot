package github.kakarot.Raids.Game;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Raids.Arena;
import github.kakarot.Raids.Managers.RaidManager;
import github.kakarot.Raids.Scenario.Scenario;
import lombok.Getter;
import noppes.npcs.api.entity.ICustomNpc;
import org.bukkit.Location;

import java.util.*;

public class GameSession {
    @Getter private GameState currentState;
    @Getter private int currentWaveNumber;
    @Getter private final Set<UUID> alivePlayers;
    @Getter private final Set<ICustomNpc<?>> aliveNpcs = new HashSet<>();
    @Getter private final Map<UUID, Location> originalLocation = new HashMap<>();
    private final Main plugin;
    private final RaidManager raidManager;
    @Getter private final Party party;
    @Getter private final Arena arena;
    private final Scenario scenario;
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

    }
}
