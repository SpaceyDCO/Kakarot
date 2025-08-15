package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
     * If false, there'll be no countdown messages
     */
    private boolean showMessages;
    /**
     * Holds a record of what sound to play when this wave starts
     */
    private SoundInfo startWaveSound;
    /**
     * Defines what DBC Settings this wave has
     * These include: maximum release, flying allowed, turbo enabled
     */
    private DBCSettings dbcSettings;
    /**
     * A list containing all the rewards completing this wave should give
     */
    private final List<Reward> rewards = new ArrayList<>();
    /**
     * A list containing all Spawning information (npc id, coordinates)
     */
    private final List<SpawnInfo> spawns = new ArrayList<>();
    /**
     * Gets an optional containing a SoundInfo class
     * @return An optional containing SoundInfo class
     */
    public Optional<SoundInfo> getWaveSound() {
        return Optional.ofNullable(this.startWaveSound);
    }
    public DBCSettings getDBCSettings() {
        return (dbcSettings == null) ? new DBCSettings((byte) 126, true, true) : this.dbcSettings;
    }
}
