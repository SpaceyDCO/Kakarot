package github.kakarot.Tools;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

public class CC {
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static List<String> translate(List<String> list) {
        return list.stream().map(CC::translate).collect(Collectors.toList());
    }
}