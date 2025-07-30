package github.kakarot.Parties;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class Party {
    private final UUID leader;
    private final List<UUID> members;
    public Party(UUID leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader);
    }

    public void addMember(UUID member) {
        if(!this.members.contains(member)) this.members.add(member);
    }
    public void removeMember(UUID member) {
        this.members.remove(member);
    }
    public boolean isLeader(UUID player) {
        return player.equals(leader);
    }
    public int getSize() {
        return this.members.size();
    }
    public void broadcast(String message) {
        for(UUID member : this.members) {
            Player player = Bukkit.getPlayer(member);
            if(player != null) {
                if(player.isOnline()) player.sendMessage(message);
            }
        }
    }
}
