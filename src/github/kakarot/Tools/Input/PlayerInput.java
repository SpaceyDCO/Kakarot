package github.kakarot.Tools.Input;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import github.kakarot.Tools.CC;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerInput implements Listener {
    private final JavaPlugin plugin;
    private final Player player;
    private final boolean followUpInput;
    private Consumer<String> inputCallback;
    private BukkitRunnable timeoutTask;
    private String timeoutMessage;
    private static final ArrayList<UUID> players = new ArrayList<>();
    public PlayerInput(Player player, JavaPlugin plugin, boolean sequenceInput) {
        this.player = player;
        this.plugin = plugin;
        this.followUpInput = sequenceInput;
    }
    public PlayerInput(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.followUpInput = false;
    }
    public void waitForPlayerInput(int delayInSeconds, String timeoutMessage, Consumer<String> callback) {
        if(!players.contains(this.player.getUniqueId())) {
            players.add(this.player.getUniqueId());
        }
        delayInSeconds *= 20;
        this.timeoutMessage = timeoutMessage;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                cancelInputRequest();
            }
        };
        timeoutTask.runTaskLater(plugin, delayInSeconds);
        this.inputCallback = callback;
    }
    private void successfulPlayerInput(String input) {
        if(inputCallback != null) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                inputCallback.accept(input);
                if(this.followUpInput) cleanUp();
                else completeCleanUp();
            });
        }
    }
    private void cancelInputRequest() {
        if(player.isOnline()) player.sendMessage(CC.translate(this.timeoutMessage));
        completeCleanUp();
    }
    private void completeCleanUp() {
        cleanUp();
        for(int i = 0; i < players.size(); i++) {
            UUID cPlayer = players.get(i);
            if(cPlayer.equals(player.getUniqueId())) {
                players.remove(i);
                break;
            }
        }
    }
    private void cleanUp() {
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        if(timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
        this.inputCallback = null;
    }
    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if(!player.equals(this.player)) return;
        if(!players.contains(player.getUniqueId())) {
            Bukkit.getConsoleSender().sendMessage("Not waiting for input from player " + player.getName());
            return;
        }
        successfulPlayerInput(event.getMessage());
        event.setCancelled(true);
    }
}