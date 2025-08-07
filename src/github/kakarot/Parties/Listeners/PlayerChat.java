package github.kakarot.Parties.Listeners;

import github.kakarot.Main;
import github.kakarot.Parties.Party;
import github.kakarot.Tools.CC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerChat implements Listener {
    public static List<UUID> partyChatPlayers = new ArrayList<>();
    private final Main main;
    public PlayerChat(Main main) {
        this.main = main;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if(partyChatPlayers.contains(player.getUniqueId())) {
            if(main.getPartyManager().getParty(player).isPresent()) {
                Party party = main.getPartyManager().getParty(player).get();
                event.setCancelled(true);
                party.broadcast(CC.translate("&7[&9Party&7] &b" + player.getName() + ": &7" + event.getMessage()));
            }
        }
    }
}
