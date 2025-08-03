package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SpawnInfo {
    /**
     * An String containing the Npc's ID (From the clone tab)
     */
    private String npcID;
    /**
     * How many of these npcs will spawn
     */
    private int count;
    /**
     * The name of the spawn point
     */
    private String spawnPointName;
}
