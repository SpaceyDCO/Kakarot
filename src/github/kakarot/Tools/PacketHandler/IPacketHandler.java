package github.kakarot.Tools.PacketHandler;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface IPacketHandler {
    /**
     * Sends a fake block change to a single player.
     *
     * @param player The player to send the change to.
     * @param location The location of the block to change.
     * @param id The id of the block to place
     * @param data The data (number after the :) of the block to place
     */
    void sendBlockChange(Player player, Location location, int id, byte data);

    /**
     * Overload method to be used with customnpcs scripting
     * @param ID String with the player's UUID (Usually from getPlayer().getUniqueID())
     * @param x Coordinates in X dimension
     * @param y Coordinates in Y dimension
     * @param z Coordinates in Z dimension
     * @param blockID The ID of the block to be placed (number)
     * @param blockData The data of the block to be placed (number after the :)
     */
    void sendBlockChange(String ID, double x, double y, double z, int blockID, byte blockData);
    void sendBlockChange(String ID, double x, double y, double z, int blockID);

    /**
     * Restores a single fake block back to its original state for a player.
     *
     * @param player The player to restore the block for.
     * @param location The location of the block to restore.
     */
    void restoreBlock(Player player, Location location);

    /**
     * Overload method to be used with customnpcs scripting
     * @param playerID String with the player's UUID (Usually from getPlayer().getUniqueID())
     * @param x Coordinates in X dimension
     * @param y Coordinates in Y dimension
     * @param z Coordinates in Z dimension
     */
    void restoreBlock(String playerID, double x, double y, double z);

    /**
     * Restores ALL fake blocks for a player.
     *
     * @param player The player whose blocks should be restored.
     */
    void restoreAllBlocks(Player player);

    /**
     * Overload method to be used with customnpcs scripting
     * @param playerID String with the player's UUID (Usually from getPlayer().getUniqueID())
     */
    void restoreAllBlocks(String playerID);
}
