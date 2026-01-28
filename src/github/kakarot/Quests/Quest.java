package github.kakarot.Quests;

import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Quests.Models.QuestReward;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class Quest {
    private final int id;
    private final Map<String, String> name;
    private final Map<String, String> description;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final boolean repeatable;
    private final long repeatCooldown;
    private final int npcId; //TODO: NPCID CHANGES ON CHUNK RELOAD, MAYBE USE NAMES?
    public String getName(String locale) {
        return this.name.getOrDefault(locale, this.name.getOrDefault("es", "Misión inexistente"));
    }
    public String getDescription(String locale) {
        return this.description.getOrDefault(locale, this.description.getOrDefault("es", "Sin descripción"));
    }
    public int getObjectiveCount() {
        return this.objectives.size();
    }
    public boolean areAllObjectivesCompletes(int[] objectiveProgress) {
        if(objectiveProgress.length != this.objectives.size()) return false;
        for(int i = 0; i < this.objectives.size(); i++) {
            if(!this.objectives.get(i).isComplete(objectiveProgress[i])) return false;
        }
        return true;
    }
}
