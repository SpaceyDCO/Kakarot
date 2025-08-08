package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SpawnInfo {
    /**
     * A String containing the Npc's ID (From the clone tab)
     */
    private String npcID;
    /**
     * The tab in which the npc clone is
     */
    private int npcTab;
    /**
     * How many of these npcs will spawn
     */
    private int count;
    /**
     * The name of the spawn point
     */
    private String spawnPointName;
}
