package github.kakarot.Quests.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Getter @Setter
public class TitleInfo {
    private Map<String, String> title;
    private Map<String, String> subtitle;
    private String titleColor;
    private String subtitleColor;
    private int displayTime;
    private int fadeoutTime;
}
