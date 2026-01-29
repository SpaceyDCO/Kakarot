package github.kakarot.Quests.Models;

import lombok.Getter;

import java.util.Map;

@Getter
public class QuestReward {
    private final RewardType type;
    private final String value;
    private final Map<String, String> descriptions;
    public QuestReward(RewardType type, String value, Map<String, String> descriptions) {
        this.type = type;
        this.value = value;
        this.descriptions = descriptions;
    }
    public String getResolvedValue(String playerName) {
        return this.value.replace("{player}", playerName);
    }
}
