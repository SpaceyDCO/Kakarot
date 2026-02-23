package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TrackingInfo {
    private int x;
    private int y;
    private int z;
    private Map<String, String> label;
    private String arrowColor;
    private String labelColor;
}
