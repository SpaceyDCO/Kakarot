package github.kakarot.Tools;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@NoArgsConstructor
public final class PacketHandler {
    private static final Map<UUID, Map<Location, BlockState>> playerOriginalBlocks = new ConcurrentHashMap<>();

    /**
     * Sends a fake block change to a single player.
     *
     * @param player The player to send the change to.
     * @param location The location of the block to change.
     * @param id The id of the block to place
     * @param data The data (number after the :) of the block to place
     */
    @SuppressWarnings("deprecation")
    public void sendBlockChange(Player player, Location location, int id, byte data) {
        // Store the real block state first.
        Material originalType = location.getBlock().getType();
        byte originalData = location.getBlock().getData();

        playerOriginalBlocks
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(location, new BlockState(originalType.getId(), originalData));
        player.sendBlockChange(location, id, data);
    }

    /**
     * Overload method to be used with customnpcs scripting
     * @param ID String with the player's UUID (Usually from getPlayer().getUniqueID())
     * @param x Coordinates in X dimension
     * @param y Coordinates in Y dimension
     * @param z Coordinates in Z dimension
     * @param blockID The ID of the block to be placed (number)
     * @param blockData The data of the block to be placed (number after the :)
     */
    public void sendBlockChange(String ID, double x, double y, double z, int blockID, byte blockData) {
        Player player = Bukkit.getPlayer(UUID.fromString(ID));
        if(player == null) return;
        Location loc = new Location(player.getWorld(), x, y, z);
        sendBlockChange(player, loc, blockID, blockData);
    }
    public void sendBlockChange(String ID, double x, double y, double z, int blockID) {
        sendBlockChange(ID, x, y, z, blockID, (byte) 0);
    }

    /**
     * Restores a single fake block back to its original state for a player.
     *
     * @param player The player to restore the block for.
     * @param location The location of the block to restore.
     */
    @SuppressWarnings("deprecation")
    public void restoreBlock(Player player, Location location) {
        Map<Location, BlockState> playerMap = playerOriginalBlocks.get(player.getUniqueId());
        if (playerMap == null) return;

        BlockState originalBlock = playerMap.remove(location);
        if (originalBlock != null) {
            player.sendBlockChange(location, originalBlock.getId(), originalBlock.getBlockData());
        }

        if (playerMap.isEmpty()) {
            playerOriginalBlocks.remove(player.getUniqueId());
        }
    }

    /**
     * Overload method to be used with customnpcs scripting
     * @param playerID String with the player's UUID (Usually from getPlayer().getUniqueID())
     * @param x Coordinates in X dimension
     * @param y Coordinates in Y dimension
     * @param z Coordinates in Z dimension
     */
    public void restoreBlock(String playerID, double x, double y, double z) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerID));
        if(player == null) return;
        Location location = new Location(player.getWorld(), x, y, z);
        restoreBlock(player, location);
    }

    /**
     * Restores ALL fake blocks for a player.
     *
     * @param player The player whose blocks should be restored.
     */
    @SuppressWarnings("deprecation")
    public void restoreAllBlocks(Player player) {
        Map<Location, BlockState> playerMap = playerOriginalBlocks.remove(player.getUniqueId());
        if (playerMap == null) return;

        for (Map.Entry<Location, BlockState> entry : playerMap.entrySet()) {
            BlockState originalBlock = entry.getValue();
            player.sendBlockChange(entry.getKey(), originalBlock.getId(), originalBlock.getBlockData());
        }
    }

    /**
     * Overload method to be used with customnpcs scripting
     * @param playerID String with the player's UUID (Usually from getPlayer().getUniqueID())
     */
    public void restoreAllBlocks(String playerID) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerID));
        if(player == null) return;
        restoreAllBlocks(player);
    }

    @Getter
    private static class BlockState {
        private final int id;
        private final byte blockData;

        public BlockState(int id, byte blockData) {
            this.id = id;
            this.blockData = blockData;
        }

    }
}