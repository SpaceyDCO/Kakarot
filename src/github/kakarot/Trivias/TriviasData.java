package github.kakarot.Trivias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TriviasData {
    private String category;
    private String question;
    private int timeLimit;
    private Set<String> answers;
    private Set<String> rewardsCommands;
    private boolean enabled;
}
