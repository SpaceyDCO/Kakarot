package github.kakarot.Parties.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartyManager implements IPartyManager {
    private final int INVITATION_TIMEOUT = 60; //In seconds
    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    private final Map<UUID, BukkitTask> invitedPlayers = new HashMap<>();
    private final Map<UUID, UUID> invitedPlayersMap = new HashMap<>(); //Key: Player invited | Value: Player that sent the invitation
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
        List<UUID> members = party.getMembers();
        party.broadcast(CC.translate(PARTY_PREFIX + " &9The party has been disbanded."));
        for(UUID member : members) {
            this.playerPartyMap.remove(member);
        }
        if(Bukkit.getPlayer(party.getLeader()) != null) plugin.getLogger().info(Bukkit.getPlayer(party.getLeader()).getName() + "'s Party has been disbanded.");
        party.getMembers().clear();
    }

    @Override
    public void invitePlayer(Player leader, Player target) {
        Party party = getParty(leader);
        if(party == null) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9You are not in a party"));
            return;
        }
        if(!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Only the party leader can send invites"));
            return;
        }
        if(target.equals(leader)) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9You cannot invite yourself."));
            return;
        }
        if(isInParty(target)) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &b" + target.getName() + " &9Is already in a party"));
            return;
        }
        UUID targetID = target.getUniqueId();
        if(invitedPlayers.containsKey(targetID)) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &b" + target.getName() + " &9Has a pending invite active"));
            target.sendMessage(CC.translate(PARTY_PREFIX + " &b" + leader.getName() + " &9Invited you to their party, but you have a party join request."));
            return;
        }
        leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Sent party invitation request to &b" + target.getName()));
        target.sendMessage(CC.translate(PARTY_PREFIX + "&b " + leader.getName() + " &9Has invited you to their party"));
        invitedPlayersMap.put(targetID, leader.getUniqueId());
        TextComponent message = new TextComponent("  ");
        TextComponent acceptComponent = new TextComponent(CC.translate("&a[ACCEPT]"));
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {new TextComponent(CC.translate("&7Click to join"))}));
        TextComponent denyComponent = new TextComponent(CC.translate("&c[DENY]"));
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party deny"));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {new TextComponent(CC.translate("&7Click to deny"))}));
        message.addExtra(acceptComponent);
        message.addExtra(denyComponent);
        target.spigot().sendMessage(message);
        long inviteTimeout = INVITATION_TIMEOUT * 20;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            clearInvites(target);
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9Invitation request to player &b" + target.getName() + " &9has expired."));
            target.sendMessage(CC.translate(PARTY_PREFIX + " &9Invitation request from player &b " + leader.getName() + " &9has expired."));
        }, inviteTimeout);
        invitedPlayers.put(targetID, task);
    }

    @Override
    public void acceptInvite(Player player) {
        UUID targetID = player.getUniqueId();
        if(!invitedPlayers.containsKey(targetID)) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &bYou do not have any invitation request pending"));
            return;
        }
        invitedPlayers.get(targetID).cancel();
        invitedPlayers.remove(targetID);
        Player leader = Bukkit.getPlayer(invitedPlayersMap.get(targetID));
        if(leader != null) {
            Party party = getParty(leader);
            if(party != null) {
                party.addMember(targetID);
                playerPartyMap.put(targetID, party);
                party.broadcast(CC.translate(PARTY_PREFIX + " &b" + player.getName() + " &9Has joined the party."));
            }else {
                player.sendMessage(CC.translate(PARTY_PREFIX + " &9The party you tried to join no longer exists."));
            }
        }else {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &9The party you tried to join no longer exists."));
        }
        invitedPlayersMap.remove(targetID);
    }

    @Override
    public void denyInvite(Player player) {
        UUID targetID = player.getUniqueId();
        if(!invitedPlayers.containsKey(targetID)) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &bYou do not have any invitation request pending"));
            return;
        }
        invitedPlayers.get(targetID).cancel();
        player.sendMessage(CC.translate(PARTY_PREFIX + " &9You have denied the party invitation request"));
        Player leader = Bukkit.getPlayer(invitedPlayersMap.get(targetID));
        if(leader != null) {
            if(leader.isOnline()) leader.sendMessage(CC.translate(PARTY_PREFIX + " &b" + player.getName() + " &9Has denied your party invitation request."));
        }
        invitedPlayers.remove(targetID);
        invitedPlayersMap.remove(targetID);
    }

    @Override
    public void leaveParty(Player player) {
        Party party = getParty(player);
        if(party == null) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &9You are not in a party"));
            return;
        }
        if(party.isLeader(player.getUniqueId())) {
            disbandParty(party);
            return;
        }
        party.removeMember(player.getUniqueId());
        playerPartyMap.remove(player.getUniqueId());
        party.broadcast(CC.translate(CC.translate(PARTY_PREFIX + " &b" + player.getName() + " &9Left the party.")));
        player.sendMessage(CC.translate(PARTY_PREFIX + " &9You have left the party."));
    }

    @Override
    public void kickPlayer(Player leader, Player target) {
        Party party = getParty(leader);
        if(party == null) return;
        if(!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &9You are not the leader of this party."));
            return;
        }
        if(party.getMembers().contains(target.getUniqueId())) {
            leaveParty(target);
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &b" + target.getName() + " &9Has been kicked from your party."));
        }else {
            leader.sendMessage(CC.translate(PARTY_PREFIX + " &b" + target.getName() + " &9Is not in your party"));
        }
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
        invitedPlayers.remove(player.getUniqueId());
        invitedPlayersMap.remove(player.getUniqueId());
    }
}
