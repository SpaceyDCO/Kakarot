package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrackingInfo {
    private int x;
    private int y;
    private int z;
    private String label;
    private String arrowColor;
    private String labelColor;
}
