package github.kakarot.Raids.Helpers;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.logging.Level;

@Getter
@NoArgsConstructor
public class SerializableLocation {
    private double x, y, z;
    private float yaw, pitch;

    public Location toBukkitLocation(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if(world == null) {
            Bukkit.getLogger().log(Level.SEVERE, "World " + worldName + " is invalid!.");
            return null;
        }
        return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
    }
}
