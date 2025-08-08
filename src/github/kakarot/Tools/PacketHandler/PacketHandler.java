package github.kakarot.Tools.PacketHandler;

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
public final class PacketHandler implements IPacketHandler {
    private static final Map<UUID, Map<Location, BlockState>> playerOriginalBlocks = new ConcurrentHashMap<>();

    @SuppressWarnings("deprecation")
    @Override
    public void sendBlockChange(Player player, Location location, int id, byte data) {
        // Store the real block state first.
        Material originalType = location.getBlock().getType();
        byte originalData = location.getBlock().getData();

        playerOriginalBlocks
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(location, new BlockState(originalType.getId(), originalData));
        player.sendBlockChange(location, id, data);
    }

    @Override
    public void sendBlockChange(String ID, double x, double y, double z, int blockID, byte blockData) {
        Player player = Bukkit.getPlayer(UUID.fromString(ID));
        if(player == null) return;
        Location loc = new Location(player.getWorld(), x, y, z);
        sendBlockChange(player, loc, blockID, blockData);
    }
    @Override
    public void sendBlockChange(String ID, double x, double y, double z, int blockID) {
        sendBlockChange(ID, x, y, z, blockID, (byte) 0);
    }

    @SuppressWarnings("deprecation")
    @Override
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

    @Override
    public void restoreBlock(String playerID, double x, double y, double z) {
        Player player = Bukkit.getPlayer(UUID.fromString(playerID));
        if(player == null) return;
        Location location = new Location(player.getWorld(), x, y, z);
        restoreBlock(player, location);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void restoreAllBlocks(Player player) {
        Map<Location, BlockState> playerMap = playerOriginalBlocks.remove(player.getUniqueId());
        if (playerMap == null) return;

        for (Map.Entry<Location, BlockState> entry : playerMap.entrySet()) {
            BlockState originalBlock = entry.getValue();
            player.sendBlockChange(entry.getKey(), originalBlock.getId(), originalBlock.getBlockData());
        }
    }

    @Override
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