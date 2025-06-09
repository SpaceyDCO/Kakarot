package github.kakarot.Trivias;

import github.kakarot.Main;
import github.kakarot.Tools.CC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;

public class PlayerChatEvent implements Listener {
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if(!Main.activeTrivia) return;
        if(TriviasRunnable.correctAnswers == null) return;
        Set<String> answers = TriviasRunnable.correctAnswers;
        String chat = event.getMessage();
        for(String a : answers) {
            if(a.trim().equalsIgnoreCase(chat.trim())) {
                for(Player player : Bukkit.getServer().getOnlinePlayers()) player.sendMessage(CC.translate("&9El usuario &b" + event.getPlayer().getName() + " &9respondió correctamente a la pregunta!"));
                for(String rewards : TriviasRunnable.rewards) {
                    if(rewards.contains("{player}")) rewards = rewards.replace("{player}", event.getPlayer().getName());
                    if(rewards.contains("{p}")) rewards = rewards.replace("{p}", event.getPlayer().getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewards);
                }
                TriviasRunnable.reset(false);
                break;
            }
        }
    }
}
