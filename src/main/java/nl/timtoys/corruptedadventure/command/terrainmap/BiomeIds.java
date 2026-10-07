package nl.timtoys.corruptedadventure.command.terrainmap;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Turns the sampled biome holders into indices into a sorted id list (so the legend has a stable order). */
final class BiomeIds {
    private BiomeIds() {
    }

    record Indexed(int[] indices, List<String> ids) {
    }

    static Indexed index(Holder<Biome>[] biomes) {
        // Registry holders are shared instances: look each one up once.
        Map<Holder<Biome>, String> idOf = new IdentityHashMap<>();
        for (Holder<Biome> biome : biomes) {
            idOf.computeIfAbsent(biome, BiomeIds::id);
        }
        List<String> ids = new ArrayList<>(new TreeSet<>(idOf.values()));
        Map<Holder<Biome>, Integer> indexOf = new IdentityHashMap<>();
        idOf.forEach((holder, id) -> indexOf.put(holder, ids.indexOf(id)));
        int[] indices = new int[biomes.length];
        for (int i = 0; i < biomes.length; i++) {
            indices[i] = indexOf.get(biomes[i]);
        }
        return new Indexed(indices, List.copyOf(ids));
    }

    private static String id(Holder<Biome> biome) {
        return biome.unwrapKey().map(key -> key.location().toString()).orElse("unregistered");
    }
}
