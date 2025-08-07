package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Getter
public class Wave {
    /**
     * "waveNumber" value from .json file
     * "waveNumber": 1 -> waveNumber is 1
     * "waveNumber": 7 -> waveNumber is 7
     */
    private int waveNumber;
    /**
     * Message to send the players when this wave starts
     */
    private String startMessage;
    /**
     * A list containing all Spawning information (npc id, coordinates)
     */
    private final List<SpawnInfo> spawns = new ArrayList<>();
}
