package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class ObjectiveInfo {
    private final String target;
    private String title;
    public ObjectiveInfo(String target) {
        this.target = target;
    }
}
