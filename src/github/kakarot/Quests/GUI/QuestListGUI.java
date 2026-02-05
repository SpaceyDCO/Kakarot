package github.kakarot.Quests.GUI;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import github.kakarot.Main;
import github.kakarot.Quests.Models.PlayerQuestProgress;
import github.kakarot.Quests.Quest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestListGUI implements InventoryProvider {
    public static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("questList")
            .provider(new QuestListGUI())
            .size(6, 9)
            .title("§6§lYour Quests")
            .build();
    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        Pagination pagination = inventoryContents.pagination();
        Map<Integer, PlayerQuestProgress> playerQuests = Main.instance.getQuestManager().getPlayerQuests(player.getUniqueId());
        if(playerQuests.isEmpty()) {
            ItemStack noQuestsItem = new ItemStack(Material.PAPER);
            ItemMeta meta = noQuestsItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add("§7You don't have any quests yet."); //TODO: language support
            meta.setLore(lore);
            noQuestsItem.setItemMeta(meta);
            inventoryContents.set(2, 4, ClickableItem.empty(noQuestsItem));
            return;
        }
        List<ClickableItem> items = new ArrayList<>();
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        for(Map.Entry<Integer, PlayerQuestProgress> entry : playerQuests.entrySet()) {
            int questId = entry.getKey();
            PlayerQuestProgress progress = entry.getValue();
            Quest quest = Main.instance.getQuestManager().getQuest(questId);
            if(quest == null) continue;
            items.add(ClickableItem.of(
                    createQuestItem(quest, progress, playerLocale), e -> {
                        if(e.getClick() == ClickType.LEFT) {
                            player.closeInventory();
                            player.performCommand("quest info " + questId);
                        }else if(e.getClick() == ClickType.RIGHT) {
                            player.closeInventory();
                            player.performCommand("quest track " + questId);
                        }
                    }
            ));
        }
        pagination.setItems(items.toArray(new ClickableItem[0]));
        pagination.setItemsPerPage(28);
        SlotIterator iterator = inventoryContents.newIterator(SlotIterator.Type.HORIZONTAL, 1, 1);
        iterator.blacklist(1, 0).blacklist(1, 8);
        iterator.blacklist(2, 0).blacklist(2, 8);
        iterator.blacklist(3, 0).blacklist(3, 8);
        iterator.blacklist(4, 0).blacklist(4, 8);
        pagination.addToIterator(iterator);
        if(!pagination.isFirst()) {
            inventoryContents.set(5, 3, ClickableItem.of(
                    createNavItem("§a◀ Previous", Material.ARROW), e -> INVENTORY.open(player, pagination.previous().getPage())
            ));
        }
        if(!pagination.isLast()) {
            inventoryContents.set(5, 5, ClickableItem.of(
                    createNavItem("§aNext ▶", Material.ARROW), e -> INVENTORY.open(player, pagination.next().getPage())
            ));
        }
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName("§7Page " + (pagination.getPage() + 1) + "/" + (pagination.last().getPage() + 1));
        pageItem.setItemMeta(pageMeta);
        inventoryContents.set(5, 4, ClickableItem.empty(pageItem));
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§lControls");
        List<String> lore = new ArrayList<>();
        lore.add("§7Left-click: §fShow details");
        lore.add("§7Right-click: §fTrack quest");
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        inventoryContents.set(5, 0, ClickableItem.empty(infoItem));
        inventoryContents.set(5, 8, ClickableItem.of(
                createNavItem("§cClose", Material.REDSTONE_BLOCK), e -> player.closeInventory()
        ));
    }
    @Override
    public void update(Player player, InventoryContents inventoryContents) {

    }
    private ItemStack createQuestItem(Quest quest, PlayerQuestProgress progress, String playerLocale) {
        Material material;
        String statusColor;
        String statusText;
        switch(progress.getStatus()) {
            case IN_PROGRESS:
                material = Material.BOOK;
                statusColor = "§e";
                statusText = "In Progress";
                break;
            case COMPLETED:
                if(quest.isRepeatable() && progress.canRepeat()) {
                    material = Material.ENCHANTED_BOOK;
                    statusColor = "§a";
                    if(quest.isRepeatable() && progress.canRepeat()) statusText = "Can repeat";
                    else statusText = "Completed";
                }else {
                    material = Material.WRITTEN_BOOK;
                    statusColor = "§7";
                    statusText = "Completed";
                }
                break;
            default:
                material = Material.PAPER;
                statusColor = "§c";
                statusText = "Unknown";
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l" + quest.getName().getOrDefault(playerLocale, "Misión") + " §7(#" + quest.getId() + ")");
        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription().getOrDefault(playerLocale, ""));
        lore.add("");
        lore.add(statusColor + "● " + statusText);
        int percentage = progress.getTotalProgressPercentage(quest);
        lore.add("§7Progress: " + getProgressBar(percentage));
        lore.add("");
        lore.add("§7Left-click: §fDetails");
        lore.add("§7Right-click: §fTrack");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createNavItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
    private String getProgressBar(int percentage) {
        int bars = 10;
        double filled = ((double) percentage / 100) * bars;
        StringBuilder bar = new StringBuilder("§a");
        for(int i = 0; i < bars; i++) {
            if(i < filled) {
                bar.append("█");
            }else bar.append("§7░");
        }
        bar.append(" §f(").append(percentage).append("%)");
        return bar.toString();
    }
}
