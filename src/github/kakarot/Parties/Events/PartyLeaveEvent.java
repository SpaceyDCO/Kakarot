package github.kakarot.Parties.Events;

import github.kakarot.Parties.Party;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class PartyLeaveEvent extends Event {
    public static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Party party;
    public PartyLeaveEvent(Player player, Party party) {
        this.player = player;
        this.party = party;
    }
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
