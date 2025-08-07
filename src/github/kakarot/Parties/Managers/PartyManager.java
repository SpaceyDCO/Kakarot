package github.kakarot.Parties.Managers;

import github.kakarot.Main;
import github.kakarot.Parties.Events.PlayerJoinPartyEvent;
import github.kakarot.Parties.Events.PlayerLeavePartyEvent;
import github.kakarot.Parties.Listeners.PlayerChat;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.MessageManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PartyManager implements IPartyManager {
    private final Map<UUID, Party> playerPartyMap = new HashMap<>();
    private final Map<UUID, BukkitTask> invitedPlayers = new HashMap<>();
    private final Map<UUID, UUID> invitedPlayersMap = new HashMap<>(); //Key: Player invited | Value: Player that sent the invitation
    private final MessageManager messageManager;
    private final Main plugin;
    public PartyManager(Main plugin, MessageManager messageManager) {
        this.plugin = plugin;
        this.messageManager = messageManager;
    }

    @Override
    public void createParty(Player leader) {
        if(isInParty(leader)) {
            leader.sendMessage(messageManager.getMessage("already-in-party"));
            return;
        }
        Party newParty = new Party(leader.getUniqueId());
        playerPartyMap.put(leader.getUniqueId(), newParty);
        leader.sendMessage(messageManager.getMessage("party-created"));
    }

    @Override
    public void disbandParty(Party party) {
        List<UUID> members = party.getMembers();
        party.broadcast(messageManager.getMessage("party-disbanded"));
        for(UUID member : members) {
            this.playerPartyMap.remove(member);
        }
        if(Bukkit.getPlayer(party.getLeader()) != null) plugin.getLogger().info(Bukkit.getPlayer(party.getLeader()).getName() + "'s Party has been disbanded.");
        party.getMembers().clear();
    }

    @Override
    public void invitePlayer(Player leader, Player target) {
        Optional<Party> optional = getParty(leader);
        if(!optional.isPresent()) {
            leader.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(messageManager.getMessage("leader-requirement"));
            return;
        }
        if(target.equals(leader)) {
            leader.sendMessage(messageManager.getMessage("self-invite"));
            return;
        }
        if(!target.isOnline()) {
            leader.sendMessage(messageManager.getMessage("player-not-online"));
            return;
        }
        if(isInParty(target)) {
            leader.sendMessage(messageManager.getMessage("target-already-in-party", "target", target.getName()));
            return;
        }
        UUID targetID = target.getUniqueId();
        if(invitedPlayers.containsKey(targetID)) {
            leader.sendMessage(messageManager.getMessage("target-pending-invite", "target", target.getName()));
            target.sendMessage(messageManager.getMessage("invitation-canceled", "player", leader.getName()));
            return;
        }
        leader.sendMessage(messageManager.getMessage("invite-sent", "target", target.getName()));
        target.sendMessage(messageManager.getMessage("invite-received", "player", leader.getName()));
        invitedPlayersMap.put(targetID, leader.getUniqueId());
        TextComponent message = new TextComponent("");
        TextComponent acceptComponent = new TextComponent(messageManager.getMessage("accept-button", false));
        acceptComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        acceptComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {new TextComponent(CC.translate("&7Click to join"))}));
        TextComponent denyComponent = new TextComponent(messageManager.getMessage("deny-button", false));
        denyComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party deny"));
        denyComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent[] {new TextComponent(CC.translate("&7Click to deny"))}));
        message.addExtra(acceptComponent);
        message.addExtra(denyComponent);
        target.spigot().sendMessage(message);
        int INVITATION_TIMEOUT = 60; //In seconds
        long inviteTimeout = INVITATION_TIMEOUT * 20L;
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            clearInvites(target);
            if(leader.isOnline()) leader.sendMessage(messageManager.getMessage("leader-invite-expired", "target", target.getName()));
            if(target.isOnline()) target.sendMessage(messageManager.getMessage("target-invite-expired", "player", leader.getName()));
        }, inviteTimeout);
        invitedPlayers.put(targetID, task);
    }

    @Override
    public void acceptInvite(Player player) { //If leader is offline at the moment of accepting an invite, it'll say the party no longer exists, should be changed to getOfflinePlayer
        UUID targetID = player.getUniqueId();
        if(!invitedPlayers.containsKey(targetID)) {
            player.sendMessage(messageManager.getMessage("no-request-pending"));
            return;
        }
        invitedPlayers.get(targetID).cancel();
        invitedPlayers.remove(targetID);
        Player leader = Bukkit.getPlayer(invitedPlayersMap.get(targetID));
        if(leader != null) {
            Optional<Party> optional = getParty(leader);
            if(optional.isPresent()) {
                Party party = optional.get();
                party.addMember(targetID);
                playerPartyMap.put(targetID, party);
                party.broadcast(messageManager.getMessage("player-joined", "player", player.getName()));
                Bukkit.getPluginManager().callEvent(new PlayerJoinPartyEvent(player, party));
            }else {
                player.sendMessage(messageManager.getMessage("party-invalid"));
            }
        }else {
            player.sendMessage(messageManager.getMessage("party-invalid"));
        }
        invitedPlayersMap.remove(targetID);
    }

    @Override
    public void denyInvite(Player player) {
        UUID targetID = player.getUniqueId();
        if(!invitedPlayers.containsKey(targetID)) {
            player.sendMessage(messageManager.getMessage("no-request-pending"));
            return;
        }
        invitedPlayers.get(targetID).cancel();
        player.sendMessage(messageManager.getMessage("invite-denied"));
        Player leader = Bukkit.getPlayer(invitedPlayersMap.get(targetID));
        if(leader != null) {
            if(leader.isOnline()) leader.sendMessage(messageManager.getMessage("leader-invite-denied", "target", player.getName()));
        }
        invitedPlayers.remove(targetID);
        invitedPlayersMap.remove(targetID);
    }

    @Override
    public void leaveParty(Player player) {
        Optional<Party> optional = getParty(player);
        if(!optional.isPresent()) {
            player.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(party.isLeader(player.getUniqueId())) {
            disbandParty(party);
            return;
        }
        party.removeMember(player.getUniqueId());
        playerPartyMap.remove(player.getUniqueId());
        PlayerChat.partyChatPlayers.remove(player.getUniqueId());
        party.broadcast(messageManager.getMessage("player-left", "player", player.getName()));
        player.sendMessage(messageManager.getMessage("party-left"));
        Bukkit.getPluginManager().callEvent(new PlayerLeavePartyEvent(player, party));
    }

    @Override
    public void kickPlayer(Player leader, Player target) {
        Optional<Party> optional = getParty(leader);
        if(!optional.isPresent()) {
            leader.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(messageManager.getMessage("leader-requirement"));
            return;
        }
        if(leader.equals(target)) {
            leader.sendMessage(messageManager.getMessage("self-kick"));
            return;
        }
        if(target == null) throw new NullPointerException();
        if(party.getMembers().contains(target.getUniqueId())) {
            leaveParty(target);
            leader.sendMessage(messageManager.getMessage("player-kicked", "target", target.getName()));
        }else {
            leader.sendMessage(messageManager.getMessage("player-not-in-party", "target", target.getName()));
        }
    }

    @Override
    public void kickOfflinePlayer(Player leader, OfflinePlayer target) {
        Optional<Party> optional = getParty(leader);
        if(!optional.isPresent()) {
            leader.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(!party.isLeader(leader.getUniqueId())) {
            leader.sendMessage(messageManager.getMessage("leader-requirement"));
            return;
        }
        if(party.getMembers().contains(target.getUniqueId())) {
            party.removeMember(target.getUniqueId());
            playerPartyMap.remove(target.getUniqueId());
            party.broadcast(messageManager.getMessage("player-left", "player", target.getName()));
            leader.sendMessage(messageManager.getMessage("player-kicked", "target", target.getName()));
        }else {
            leader.sendMessage(messageManager.getMessage("player-not-in-party", "target", target.getName()));
        }
    }

    @Override
    public Optional<Party> getParty(Player player) {
        return Optional.ofNullable(playerPartyMap.get(player.getUniqueId()));
    }

    @Override
    public boolean isInParty(Player player) {
        return getParty(player).isPresent();
    }

    @Override
    public void clearInvites(Player player) {
        invitedPlayers.remove(player.getUniqueId());
        invitedPlayersMap.remove(player.getUniqueId());
    }
}
