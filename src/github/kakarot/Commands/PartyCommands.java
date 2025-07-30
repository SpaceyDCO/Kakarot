package github.kakarot.Commands;

import github.kakarot.Main;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Managers.PartyManager;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.Commands.BaseCommand;
import github.kakarot.Tools.Commands.Command;
import github.kakarot.Tools.Commands.CommandArgs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class PartyCommands extends BaseCommand {
    private static final String PARTY_PREFIX = PartyManager.PARTY_PREFIX;
    @Override
    @Command(name = "party", aliases = "p")
    public void onCommand(CommandArgs command) throws IOException {
        CommandSender sender = command.getSender();
        String[] args = command.getArgs();
        Player player = (Player) sender;
        IPartyManager partyManager = Main.instance.getPartyManager();

        if(args.length == 0) {
            sendHelpMessage(player);
            return;
        }
        switch(args[0].toLowerCase()) { //Add kick command
            case "invite":
                handleInviteCommand(player, args, partyManager);
                break;
            case "accept":
                partyManager.acceptInvite(player);
                break;
            case "deny":
                partyManager.denyInvite(player);
            case "leave":
                partyManager.leaveParty(player);
                break;
            case "list":
                handleListCommand(player, partyManager.getParty(player));
                break;
            case "disband":
                handleDisbandCommand(player, partyManager);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
    }
    private void sendHelpMessage(Player player) {
        player.sendMessage(CC.translate("&9--- Party Commands ---"));
        player.sendMessage(CC.translate("&e/party invite <player> &7Invites a player."));
        player.sendMessage(CC.translate("&e/party accept &7Accepts an invite to join a party."));
        player.sendMessage(CC.translate("&e/party deny &7Denies an invite to join a party."));
        player.sendMessage(CC.translate("&e/party leave &7Leaves your current party."));
        player.sendMessage(CC.translate("&e/party kick <player> &7Kicks the player from the party."));
        player.sendMessage(CC.translate("&e/party disband &7Disbands the party."));
        player.sendMessage(CC.translate("&e/party list &7Lists the party's members"));
    }
    private void handleInviteCommand(Player player, String[] args, IPartyManager manager) {
        if(args.length != 2) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &cUsage: /party invite <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if(target == null) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &cThat player doesn't exist."));
            return;
        }
        if(!manager.isInParty(player)) manager.createParty(player);
        manager.invitePlayer(player, target);
    }
    private void handleListCommand(Player player, Party party) {
        if(party == null) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &9You're not in any party."));
            return;
        }
        List<UUID> members = party.getMembers();
        for(UUID member : members) {
            Player p = Bukkit.getPlayer(member);
            if(p != null) {
                player.sendMessage(CC.translate("&9- &b" + p.getName() + " &9| " + (p.isOnline() ? "&9ONLINE" : "&9OFFLINE") + (party.isLeader(member) ? " &b(Leader)" : "")));
            }
        }
    }
    private void handleDisbandCommand(Player player, IPartyManager manager) {
        Party party = manager.getParty(player);
        if(party == null) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &9You're not in any party."));
            return;
        }
        if(!party.isLeader(player.getUniqueId())) {
            player.sendMessage(CC.translate(PARTY_PREFIX + " &9Only the leader can disband the party."));
            return;
        }
        manager.disbandParty(party);
    }
}
