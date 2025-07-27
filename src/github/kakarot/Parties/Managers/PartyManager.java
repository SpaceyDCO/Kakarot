package github.kakarot.Parties.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyManager implements IPartyManager {
    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    private final Map<UUID, UUID> pendingInvites = new HashMap<>();
    public static final String PARTY_PREFIX = CC.translate("&7[&9Party&7] &e");
    private final Main plugin;
    public PartyManager(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createParty(Player leader) {
        if(isInParty(leader)) {
            leader.sendMessage(CC.translate("&cYou're already in a party!"));
            return;
        }
        Party newParty = new Party(leader.getUniqueId());
        playerPartyMap.put(leader.getUniqueId(), newParty);
        leader.sendMessage(CC.translate("&aCreated a new party!"));
        leader.sendMessage(CC.translate("use /party invite <player> to invite new players"));
    }

    @Override
    public void disbandParty(Party party) {

    }

    @Override
    public void invitePlayer(Player leader, Player target) {

    }

    @Override
    public void acceptInvite(Player player) {

    }

    @Override
    public void leaveParty(Player player) {

    }

    @Override
    public void kickPlayer(Player player) {

    }

    @Override
    public Party getParty(Player player) {
        return playerPartyMap.getOrDefault(player.getUniqueId(), null);
    }

    @Override
    public boolean isInParty(Player player) {
        return getParty(player) != null;
    }

    @Override
    public void clearInvites(Player player) {
        pendingInvites.remove(player.getUniqueId());
    }
}
