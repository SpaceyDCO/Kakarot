package github.kakarot.Quests.GUI;

import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import fr.minuskube.inv.content.Pagination;
import fr.minuskube.inv.content.SlotIterator;
import github.kakarot.Main;
import github.kakarot.Quests.Managers.QuestManager;
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
import java.util.UUID;

public class QuestListGUI implements InventoryProvider {
    public static final SmartInventory INVENTORY = SmartInventory.builder()
            .id("questList")
            .provider(new QuestListGUI())
            .size(6, 9)
            .title("§e§lTus Misiones")
            .build();
    @Override
    public void init(Player player, InventoryContents inventoryContents) {
        Pagination pagination = inventoryContents.pagination();
        String playerLocale = Main.instance.getSettingsManager().getPlayerLanguage().getOrDefault(player.getUniqueId(), "es");
        QuestManager questManager = Main.instance.getQuestManager();
        Map<Integer, PlayerQuestProgress> playerQuests = Main.instance.getQuestManager().getPlayerQuests(player.getUniqueId());
        if(playerQuests.isEmpty()) {
            ItemStack noQuestsItem = new ItemStack(Material.PAPER);
            ItemMeta meta = noQuestsItem.getItemMeta();
            List<String> lore = new ArrayList<>();
            lore.add(questManager.getLangMessage(player.getUniqueId(), "gui.no_quests_yet"));
            meta.setLore(lore);
            noQuestsItem.setItemMeta(meta);
            inventoryContents.set(2, 4, ClickableItem.empty(noQuestsItem));
            return;
        }
        List<ClickableItem> items = new ArrayList<>();
        for(Map.Entry<Integer, PlayerQuestProgress> entry : playerQuests.entrySet()) {
            int questId = entry.getKey();
            PlayerQuestProgress progress = entry.getValue();
            Quest quest = Main.instance.getQuestManager().getQuest(questId);
            if(quest == null) continue;
            items.add(ClickableItem.of(
                    createQuestItem(quest, progress, playerLocale, questManager, player.getUniqueId()), e -> {
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
                    createNavItem(questManager.getLangMessage(player.getUniqueId(), "gui.previous_page"), Material.ARROW), e -> INVENTORY.open(player, pagination.previous().getPage())
            ));
        }
        if(!pagination.isLast()) {
            inventoryContents.set(5, 5, ClickableItem.of(
                    createNavItem(questManager.getLangMessage(player.getUniqueId(), "gui.next_page"), Material.ARROW), e -> INVENTORY.open(player, pagination.next().getPage())
            ));
        }
        ItemStack pageItem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageItem.getItemMeta();
        pageMeta.setDisplayName(questManager.getLangMessage(player.getUniqueId(), "gui.page") + " " + (pagination.getPage() + 1) + "/" + (pagination.last().getPage() + 1));
        pageItem.setItemMeta(pageMeta);
        inventoryContents.set(5, 4, ClickableItem.empty(pageItem));
        ItemStack infoItem = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(questManager.getLangMessage(player.getUniqueId(), "gui.controls"));
        List<String> lore = new ArrayList<>();
        lore.add(questManager.getLangMessage(player.getUniqueId(), "gui.info_controls_left"));
        lore.add(questManager.getLangMessage(player.getUniqueId(), "gui.info_controls_right"));
        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        inventoryContents.set(5, 0, ClickableItem.empty(infoItem));
        inventoryContents.set(5, 8, ClickableItem.of(
                createNavItem(questManager.getLangMessage(player.getUniqueId(), "gui.close"), Material.REDSTONE_BLOCK), e -> player.closeInventory()
        ));
    }
    @Override
    public void update(Player player, InventoryContents inventoryContents) {

    }
    private ItemStack createQuestItem(Quest quest, PlayerQuestProgress progress, String playerLocale, QuestManager questManager, UUID playerUUID) {
        Material material;
        String statusColor;
        String statusText;
        switch(progress.getStatus()) {
            case IN_PROGRESS:
                material = Material.BOOK;
                statusColor = "§e";
                statusText = questManager.getLangMessage(playerUUID, "gui.info_in_process");
                break;
            case COMPLETED:
                if(quest.isRepeatable() && progress.canRepeat()) {
                    material = Material.ENCHANTED_BOOK;
                    statusColor = "§a";
                    statusText = questManager.getLangMessage(playerUUID, "gui.info_can_repeat");
                }else {
                    material = Material.WRITTEN_BOOK;
                    statusColor = "§7";
                    statusText = questManager.getLangMessage(playerUUID, "gui.info_completed");
                }
                break;
            default:
                material = Material.PAPER;
                statusColor = "§c";
                statusText = questManager.getLangMessage(playerUUID, "gui.info_unknown");
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l" + quest.getName().getOrDefault(playerLocale, "Misión") + " §7(#" + quest.getId() + ")");
        List<String> lore = new ArrayList<>();
        lore.add("§7" + quest.getDescription().getOrDefault(playerLocale, ""));
        lore.add("");
        lore.add(statusColor + "● " + statusText);
        int percentage = progress.getTotalProgressPercentage(quest);
        lore.add(questManager.getLangMessage(playerUUID, "gui.info_progress_text", "%progress_bar%", getProgressBar(percentage)));
        lore.add("");
        lore.add(questManager.getLangMessage(playerUUID, "gui.info_controls_left"));
        lore.add(questManager.getLangMessage(playerUUID, "gui.info_controls_right"));
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
