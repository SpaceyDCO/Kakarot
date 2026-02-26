package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class NpcTurnInDetails {
    private final String name;
    private final String title;
    private int x, y, z;
    private String arrowColor;
    public NpcTurnInDetails(String name) {
        this.name = name;
        this.title = "";
    }
}
