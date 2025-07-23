package github.kakarot.Tools.Data.Party;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;
import static github.kakarot.Tools.Handler.Partys.DataManager.PartyManager.updateParty;

@Getter
@Setter
public class Party {
    private final static int maxSize = 5;
    private UUID id;
    private String name;
    private UUID leader;
    private List<UUID> members;
    private Map<UUID, Long> inviteTimestamps;
    private boolean isOpen;

    public boolean addMember(UUID uuid) {
        if (isFull() || isMember(uuid)) return false;
        members.add(uuid);
        updateParty(this);
        return true;
    }

    public boolean removeMember(UUID uuid) {
        if (!isMember(uuid)) return false;
        members.remove(uuid);
        if (uuid.equals(leader)) {
            if (!members.isEmpty()) leader = members.get(0);
            else leader = null;
        }
        updateParty(this);
        return true;
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public void promoteToLeader(UUID newLeader) {
        if (isMember(newLeader)) {
            this.leader = newLeader;
        }
        updateParty(this);
    }

    public boolean isLeader(UUID uuid) {
        return leader != null && leader.equals(uuid);
    }

    public void invite(UUID uuid) {
        inviteTimestamps.put(uuid, System.currentTimeMillis());
        updateParty(this);
    }

    public boolean hasInvite(UUID uuid) {
        Long time = inviteTimestamps.get(uuid);
        if (time == null) return false;
        if (System.currentTimeMillis() - time > 300_000) {
            inviteTimestamps.remove(uuid);
            return false;
        }
        return true;
    }

    public void revokeInvite(UUID uuid) {
        inviteTimestamps.remove(uuid);
        updateParty(this);
    }

    public List<Player> getOnlineMembers() {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                online.add(p);
            }
        }
        return online;
    }

    public int getCurrentSize() {
        return members.size();
    }

    public void broadcast(String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    public void disband() {
        broadcast("§cLa party ha sido disuelta.");
        members.clear();
        inviteTimestamps.clear();
        leader = null;
    }

    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }
}
