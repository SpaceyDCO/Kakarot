package github.kakarot.Commands;

import github.kakarot.Main;
import github.kakarot.Parties.Listeners.PlayerChat;
import github.kakarot.Parties.Managers.IPartyManager;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import github.kakarot.Tools.Commands.BaseCommand;
import github.kakarot.Tools.Commands.Command;
import github.kakarot.Tools.Commands.CommandArgs;
import github.kakarot.Tools.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PartyCommands extends BaseCommand {
    private final MessageManager messageManager = Main.instance.getMessageManager();
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
        switch(args[0].toLowerCase()) { //TO DO: optimize switch
            case "invite":
                handleInviteCommand(player, args, partyManager);
                break;
            case "accept":
                partyManager.acceptInvite(player);
                break;
            case "deny":
                partyManager.denyInvite(player);
                break;
            case "leave":
                partyManager.leaveParty(player);
                break;
            case "kick":
                handleKickCommand(player, args, partyManager);
                break;
            case "list":
                handleListCommand(player, partyManager);
                break;
            case "disband":
                handleDisbandCommand(player, partyManager);
                break;
            case "chat":
                handleChatCommand(player, args, partyManager);
                break;
            case "test":
                Main.instance.getRaidManager().startGame(player, "default_arena");
                break;
            default:
                sendHelpMessage(player);
                break;
        }
    }
    private void sendHelpMessage(Player player) {
        player.sendMessage(CC.translate("&9--- Party Commands ---"));
        player.sendMessage(CC.translate("&e/party invite <player> ") + messageManager.getMessage("party-invite-info", false));
        player.sendMessage(CC.translate("&e/party accept ") + messageManager.getMessage("party-accept-info", false));
        player.sendMessage(CC.translate("&e/party deny ") + messageManager.getMessage("party-deny-info", false));
        player.sendMessage(CC.translate("&e/party leave ") + messageManager.getMessage("party-leave-info", false));
        player.sendMessage(CC.translate("&e/party kick <player> ") + messageManager.getMessage("party-kick-info", false));
        player.sendMessage(CC.translate("&e/party disband ") + messageManager.getMessage("party-disband-info", false));
        player.sendMessage(CC.translate("&e/party list ") + messageManager.getMessage("party-list-info", false));
        player.sendMessage(CC.translate("&e/party chat <message> ") + messageManager.getMessage("party-chat-info", false));
    }
    private void handleInviteCommand(Player player, String[] args, IPartyManager manager) {
        if(args.length != 2) {
            player.sendMessage(messageManager.getMessage("correct-command-usage", "command", "/party invite <player>"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if(target == null) {
            player.sendMessage(messageManager.getMessage("player-invalid"));
            return;
        }
        if(!manager.isInParty(player)) manager.createParty(player);
        manager.invitePlayer(player, target);
    }
    private void handleListCommand(Player player, IPartyManager manager) {
        Optional<Party> optional = manager.getParty(player);
        if(!optional.isPresent()) {
            player.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        List<UUID> members = party.getMembers();
        for(UUID member : members) {
            Player p = Bukkit.getPlayer(member);
            if(p != null) {
                player.sendMessage(CC.translate("&9- &b" + p.getName() + (party.isLeader(member) ? " &9(Leader)" : "")));
            }else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member);
                if(offlinePlayer != null) player.sendMessage(CC.translate("&9- &b" + offlinePlayer.getName() + (party.isLeader(member) ? " &9(Leader)" : "") + " &9| OFFLINE"));
            }
        }
    }
    private void handleDisbandCommand(Player player, IPartyManager manager) {
        Optional<Party> optional = manager.getParty(player);
        if(!optional.isPresent()) {
            player.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(!party.isLeader(player.getUniqueId())) {
            player.sendMessage(messageManager.getMessage("leader-requirement"));
            return;
        }
        manager.disbandParty(party);
    }
    private void handleKickCommand(Player player, String[] args, IPartyManager manager) {
        if(args.length != 2) {
            player.sendMessage(messageManager.getMessage("correct-command-usage", "command", "/party kick <player>"));
            return;
        }
        Player p = Bukkit.getPlayer(args[1]);
        try {
            manager.kickPlayer(player, p);
        }catch(NullPointerException e) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);
            manager.kickOfflinePlayer(player, offlinePlayer);
        }
    }
    private void handleChatCommand(Player player, String[] args, IPartyManager manager) {
        Optional<Party> optional = manager.getParty(player);
        if(!optional.isPresent()) {
            player.sendMessage(messageManager.getMessage("not-in-party"));
            return;
        }
        Party party = optional.get();
        if(args.length > 1) {
            String message = "";
            for(int i = 1; i < args.length; i++) {
                message = message.concat(" " + args[i]);
            }
            party.broadcast(CC.translate("&7[&9Party&7] &b" + player.getName() + ":" + "&7" + message));
        }else {
            if(PlayerChat.partyChatPlayers.contains(player.getUniqueId())) {
                PlayerChat.partyChatPlayers.remove(player.getUniqueId());
                player.sendMessage(messageManager.getMessage("party-chat-disabled"));
            }else {
                PlayerChat.partyChatPlayers.add(player.getUniqueId());
                player.sendMessage(messageManager.getMessage("party-chat-enabled"));
            }
        }
    }
}
