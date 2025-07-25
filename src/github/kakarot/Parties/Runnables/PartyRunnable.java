package github.kakarot.Parties.Runnables;

import github.kakarot.Parties.Managers.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PartyRunnable {
    /**
     * Inicia una tarea que guarda las parties en disco cada 5 minutos.
     *
     * @param plugin Instancia principal del plugin
     */
    public static void startAutoSaveTask( JavaPlugin plugin) {
        new BukkitRunnable () {
            @Override
            public void run() {
                PartyManager.savePartiesToFile();
                Bukkit.getLogger().info("[Kakarot] Datos de las parties guardados automáticamente.");
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 5 * 60 * 20L); // 5 minutos en ticks (20 ticks = 1 segundo)
    }
}
