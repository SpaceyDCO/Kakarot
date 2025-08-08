package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class SoundInfo {
    private String name;
    @SuppressWarnings("FieldMayBeFinal")
    private float volume = 1.0f;
    @SuppressWarnings("FieldMayBeFinal")
    private float pitch = 1.0f;
}
