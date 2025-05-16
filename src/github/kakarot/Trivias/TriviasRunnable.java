package github.kakarot.Trivias;

import github.kakarot.Main;
import github.kakarot.Tools.CC;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class TriviasRunnable {
    public static Set<String> correctAnswers;
    public static Set<String> rewards;
    public static PlayerChatEvent event;
    public static final BukkitRunnable runnableTrivias = new BukkitRunnable() {
        @Override
        public void run() {
            if(Main.activeTrivia) return;
            int totalTrivias = TriviaDataHandler.triviasDataMap.size();
            if(totalTrivias > 0) {
                Map<String, TriviasData> triviasDataMap = TriviaDataHandler.triviasDataMap;
                int chosenTrivia = (getRandomNumber(1, totalTrivias)) - 1;
                Set<String> keySet = triviasDataMap.keySet();
                List<String> keyList = new ArrayList<>(keySet);
                String selectedTriviaID = keyList.get(chosenTrivia);
                TriviasData cTrivia = triviasDataMap.get(selectedTriviaID);
                if(cTrivia.isEnabled()) {
                    event = new PlayerChatEvent();
                    Main.instance.getServer().getPluginManager().registerEvents(event, Main.instance);
                    Main.activeTrivia = true;
                    correctAnswers = cTrivia.getAnswers();
                    rewards = cTrivia.getRewardsCommands();
                    for(Player player : Bukkit.getServer().getOnlinePlayers()) {
                        player.sendMessage(" ");
                        player.sendMessage(CC.translate("&1&l&m---------- &r&9[&bTRIVIA&9] &1&l&m----------"));
                        player.sendMessage(CC.translate("&9Categoría: &b" + cTrivia.getCategory()));
                        player.sendMessage(CC.translate("\n&b" + cTrivia.getQuestion()));
                        player.sendMessage(CC.translate("&1&l&m---------- &r&9[&bTRIVIA&9] &1&l&m----------"));
                    }
                    Bukkit.getScheduler().runTaskLater(Main.instance, TriviasRunnable::reset, (cTrivia.getTimeLimit() * 20L));
                }
            }
        }
    };
    public static void reset() {
        for(Player player : Bukkit.getServer().getOnlinePlayers()) player.sendMessage(CC.translate("&9[&bTRIVIA&9] &9Nadie respondió la pregunta a tiempo!"));
        AsyncPlayerChatEvent.getHandlerList().unregister(event);
        Main.activeTrivia = false;
        correctAnswers = null;
        rewards = null;
    }
    public static int getRandomNumber(int min, int max) { return (int)(Math.random()*((max-min)+1))+min; }
}
