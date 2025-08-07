package github.kakarot.Parties;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Getter
public class Party {
    private final UUID leader;
    private final List<UUID> members;
    public Party(UUID leader) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader);
    }

    public void addMember(UUID member) {
        if(!this.members.contains(member)) this.members.add(member);
    }
    public void removeMember(UUID member) {
        this.members.remove(member);
    }
    public boolean isLeader(UUID player) {
        return player.equals(leader);
    }
    public int getSize() {
        return this.members.size();
    }
    public void broadcast(String message) {
        for(UUID member : this.members) {
            Player player = Bukkit.getPlayer(member);
            if(player != null) {
                if(player.isOnline()) player.sendMessage(message);
            }
        }
    }
    public void playSound(Sound sound, float v, float v1) {
        for(UUID member : this.members) {
            Player player = Bukkit.getPlayer(member);
            if(player != null) {
                if(player.isOnline()) player.playSound(player.getLocation(), sound, v, v1);
            }
        }
    }
    public void spawnRandomFirework() {
        Random random = new Random();
        for(UUID member : this.members) {
            Player player = Bukkit.getPlayer(member);
            if(player != null) {
                if(!player.isOnline()) continue;
                Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
                FireworkMeta fireworkMeta = firework.getFireworkMeta();
                FireworkEffect effect = FireworkEffect.builder()
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .withColor(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .withFade(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .flicker(true)
                        .trail(true)
                        .build();
                fireworkMeta.addEffect(effect);
                fireworkMeta.setPower(0);
                firework.setFireworkMeta(fireworkMeta);
            }
        }
    }
    public void spawnFirework(FireworkMeta meta) {
        for(UUID member : this.members) {
            Player player = Bukkit.getPlayer(member);
            if(player != null) {
                if(!player.isOnline()) continue;
                Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
                firework.setFireworkMeta(meta);
            }
        }
    }
}
