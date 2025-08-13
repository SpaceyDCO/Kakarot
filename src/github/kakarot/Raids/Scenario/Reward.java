package github.kakarot.Raids.Scenario;

import lombok.Getter;
import lombok.NoArgsConstructor;

@SuppressWarnings("FieldMayBeFinal")
@Getter
@NoArgsConstructor
public class Reward {
    private String command;
    private int repeat = 1;
    private int probability = 100;
    private boolean uniqueReward = false;
}
