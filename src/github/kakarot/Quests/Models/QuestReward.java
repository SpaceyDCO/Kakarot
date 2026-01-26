package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class QuestReward {
    private final RewardType type;
    private final String value;
    public String getResolvedValue(String playerName) {
        return this.value.replace("{player}", playerName);
    }
}
