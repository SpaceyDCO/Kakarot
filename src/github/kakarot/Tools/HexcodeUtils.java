package github.kakarot.Tools;

import java.util.Locale;

public class HexcodeUtils {
    public static int parseColor(String value) {
        if (value == null || value.trim().isEmpty()) {
            // Default: opaque white
            return 0xFFFFFFFF;
        }
        String raw = value.trim();
        //Optional alpha suffix: "blue@0.75", "yellow@128", "red@50"
        //0–1: factor (0.0–1.0)
        //1–100: percentage
        //>100: direct 0–255 alpha
        int alphaOverride = -1;
        int atIndex = raw.indexOf('@');
        if (atIndex > 0) {
            String base = raw.substring(0, atIndex);
            String alphaStr = raw.substring(atIndex + 1);
            raw = base;
            try {
                if (alphaStr.contains(".")) {
                    double factor = Double.parseDouble(alphaStr);
                    factor = clamp(factor, 0.0, 1.0);
                    alphaOverride = (int) Math.round(255.0 * factor);
                } else {
                    int n = Integer.parseInt(alphaStr);
                    if (n <= 1) {
                        double factor = clamp(n, 0, 1);
                        alphaOverride = (int) Math.round(255.0 * factor);
                    } else if (n <= 100) {
                        double factor = clamp(n / 100.0, 0.0, 1.0);
                        alphaOverride = (int) Math.round(255.0 * factor);
                    } else {
                        alphaOverride = clamp(n, 0, 255);
                    }
                }
            } catch (NumberFormatException ignored) {
            }
        }
        int argb;
        if (raw.startsWith("0x") || raw.startsWith("0X")) {
            argb = (int) Long.parseLong(raw.substring(2), 16);
        } else if (raw.startsWith("#")) {
            argb = parseHexNoPrefix(raw.substring(1));
        } else if (raw.matches("[0-9a-fA-F]{6,8}")) {
            argb = parseHexNoPrefix(raw);
        } else {
            argb = parseNamedColor(raw);
        }
        if (alphaOverride >= 0) {
            argb = (argb & 0x00FFFFFF) | ((alphaOverride & 0xFF) << 24);
        }
        return argb;
    }
    private static int parseHexNoPrefix(String hex) {
        String h = hex;
        if (h.length() == 6) {
            h = "FF" + h;
        } else if (h.length() != 8) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }
        return (int) Long.parseLong(h, 16);
    }
    private static int parseNamedColor(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        switch (n) {
            case "white":
                return 0xFFFFFFFF;
            case "black":
                return 0xFF000000;
            case "red":
                return 0xFFFF5555;
            case "light_red":
                return 0xBFFF7777;
            case "yellow":
                return 0xFFFFFF55;
            case "light_yellow":
                return 0xBFFFFF77;
            case "blue":
                return 0xFF55AAFF;
            case "light_blue":
                return 0xBF80CCFF;
            default:
                throw new IllegalArgumentException("Unknown color name: " + name);
        }
    }
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
