package github.kakarot.Raids.Helpers;

import github.kakarot.Main;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class CountdownHelper {
    private final Main plugin;
    public CountdownHelper(Main plugin) {
        this.plugin = plugin;
    }

    public BukkitTask startCountdown(int durationInSeconds, Consumer<Integer> onTick, Runnable onFinish) {
        return new BukkitRunnable() {
            final int[] timeRemaining = {durationInSeconds};
            @Override
            public void run() {
                if(timeRemaining[0] <= 0) {
                    onFinish.run();
                    cancel();
                    return;
                }
                onTick.accept(timeRemaining[0]);
                timeRemaining[0]--;
            }
        }.runTaskTimer(this.plugin, 0L, 20L);
    }
}
