package github.kakarot.Raids;

import github.kakarot.Raids.Helpers.SerializableLocation;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@Getter
public class Arena {
    private String arenaName;
    private String worldName;
    private String scenarioName;
    private int maxPlayers;
    private SerializableLocation playerSpawnLocation;
    private final Map<String, SerializableLocation> npcSpawnPoints = new HashMap<>();
    private SerializableLocation boundaryCorner1;
    private SerializableLocation boundaryCorner2;

    public SerializableLocation getNpcSpawnPoint(String name) {
        return this.npcSpawnPoints.getOrDefault(name, null);
    }
}
