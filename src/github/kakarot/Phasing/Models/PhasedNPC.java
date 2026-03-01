package github.kakarot.Phasing.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class PhasedNPC {
    private final String id;
    private final String name;
    private final String title;
    private final String permission;
    public String getIdentifier() {
        return this.name + "|" + this.title;
    }
}
