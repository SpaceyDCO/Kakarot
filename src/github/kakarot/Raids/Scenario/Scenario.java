package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
@Getter
public class Scenario {
    private String scenarioName;
    private int waveCooldownInSeconds;
    private final List<Wave> waves = new ArrayList<>();

    /**
     * Gets and specific wave
     * @param waveNumber The wave's number (not index)
     * @return An optional containing the wave that matches the specified wave number, empty optional otherwise
     */
    public Optional<Wave> getWave(int waveNumber) {
        return waves.stream().filter(wave -> wave.getWaveNumber() == waveNumber).findFirst();
    }

    /**
     * Gets the total of waves this scenario has
     * @return The total number of waves, 0 if none
     */
    public int getTotalWaves() {
        return waves.stream().mapToInt(Wave::getWaveNumber).max().orElse(0);
    }

    /**
     * Gets an Optional containing the next wave
     * @param currentWaveNumber The current wave
     * @return An Optional with the next Wave, empty Optional if the current wave is invalid (or there's no higher wave)
     */
    public Optional<Wave> getNextWave(int currentWaveNumber) {
        return waves.stream().filter(wave -> wave.getWaveNumber() > currentWaveNumber).min(Comparator.comparingInt(Wave::getWaveNumber));
    }
}
