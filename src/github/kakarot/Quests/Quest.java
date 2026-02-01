package github.kakarot.Quests;

import github.kakarot.Quests.Models.NpcTurnInDetails;
import github.kakarot.Quests.Models.QuestObjective;
import github.kakarot.Quests.Models.QuestReward;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class Quest {
    private final int id;
    private final Map<String, String> name;
    private final Map<String, String> description;
    private final List<QuestObjective> objectives;
    private final List<QuestReward> rewards;
    private final Map<String, String> completionMessage;
    private final boolean repeatable;
    private final long repeatCooldown;
    private final boolean turnIn;
    private NpcTurnInDetails npcTurnInDetails;
    public Quest(int id, Map<String, String> name, Map<String, String> description, List<QuestObjective> objectives, List<QuestReward> rewards, Map<String, String> completionMessage, boolean repeatable, long repeatCooldown, boolean turnIn, NpcTurnInDetails details) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.objectives = objectives;
        this.rewards = rewards;
        this.completionMessage = completionMessage;
        this.repeatable = repeatable;
        this.repeatCooldown = repeatCooldown;
        this.turnIn = turnIn;
        if(turnIn) this.npcTurnInDetails = details;
        else npcTurnInDetails = null;
    }
    public Quest(int id, Map<String, String> name, Map<String, String> description, List<QuestObjective> objectives, List<QuestReward> rewards, Map<String, String> completionMessage, boolean repeatable, long repeatCooldown) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.objectives = objectives;
        this.rewards = rewards;
        this.completionMessage = completionMessage;
        this.repeatable = repeatable;
        this.repeatCooldown = repeatCooldown;
        this.turnIn = false;
    }
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
