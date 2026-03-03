package github.kakarot.Phasing.Cache;

import github.kakarot.Phasing.Models.PhasedNPC;

import java.util.HashMap;
import java.util.Map;

public class PhasingCache {
    private static final Map<String, PhasedNPC> cache = new HashMap<>();
    public static void add(PhasedNPC npc) {
        cache.put(npc.getId(), npc);
    }
    public static Map<String, PhasedNPC> getAll() {
        return cache;
    }
    public static void clear() {
        cache.clear();
    }
}
