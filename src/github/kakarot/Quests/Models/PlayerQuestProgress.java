package github.kakarot.Quests.Models;

import github.kakarot.Quests.Quest;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.UUID;

@Getter @Setter
public class PlayerQuestProgress {
    private final UUID playerUUID;
    private final int questId;
    private QuestStatus status;
    private long pickedUpAt;
    private long completedAt;
    private long lastCompleted;
    private long nextAvailable;
    private final int[] objectiveProgress;
    public PlayerQuestProgress(UUID playerUUID, int questId, Quest quest) {
        this.playerUUID = playerUUID;
        this.questId = questId;
        this.status = QuestStatus.NOT_PICKED_UP;
        this.objectiveProgress = new int[quest.getObjectiveCount()];
        Arrays.fill(this.objectiveProgress, 0);
    }

    public boolean canRepeat() {
        return System.currentTimeMillis() >= this.nextAvailable;
    }
    public long getTimeUntilRepeat() {
        long remaining = this.nextAvailable - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    public int getTotalProgressPercentage(Quest quest) {
        int totalProgress = 0;
        for(int progress : this.objectiveProgress) {
            totalProgress += progress;
        }
        int totalRequired = quest.getObjectives().stream()
                .mapToInt(QuestObjective::getRequired)
                .sum();
        if(totalRequired == 0) return 0;
        return Math.min(100, (totalProgress * 100) / totalRequired);
    }
    public void markCompleted(Quest quest) {
        this.status = QuestStatus.COMPLETED;
        this.completedAt = System.currentTimeMillis();
        this.lastCompleted = System.currentTimeMillis();
        if(quest.isRepeatable()) this.nextAvailable = System.currentTimeMillis() + quest.getRepeatCooldown();
    }
    public void resetForRepeat(Quest quest) {
        Arrays.fill(this.objectiveProgress, 0);
        this.status = QuestStatus.IN_PROGRESS;
    }
}
