package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QuestObjective {
    private final ObjectiveType type;
    private String target;
    private final int required;
    private final boolean shareable;
    public boolean isComplete(int currentProgress) {
        return currentProgress >= this.required;
    }
    public int getProgressCompletedAsPercentage(int currentProgress) {
        return Math.min(100, (currentProgress * 100) / this.required);
    }
}
