package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Surface rule {@code corruptedadventure:biome_switch}: a run of {@code if biome[...] then rule} rules of one sequence,
 * with one biome lookup and one map lookup per block instead of one biome test per rule (opt/surface). The rules whose
 * biomes hold the block's biome run in their order until one gives a block; the others could give nothing, as their
 * biome test fails. A biome without a registry key matches no case, as in vanilla's biome condition. {@link
 * BiomeSwitches} puts these into the surface rules when a generator loads them; the JSON keeps the plain rules.
 */
public final class BiomeSwitchRule implements SurfaceRules.RuleSource {
    /** One {@code if biome[biomes] then rule}. */
    public record Case(List<ResourceKey<Biome>> biomes, SurfaceRules.RuleSource rule) {
        static final Codec<Case> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.BIOME).listOf().fieldOf("biomes").forGetter(Case::biomes),
                SurfaceRules.RuleSource.CODEC.fieldOf("rule").forGetter(Case::rule)
        ).apply(instance, Case::new));
    }

    public static final MapCodec<BiomeSwitchRule> MAP_CODEC = Codec.list(Case.CODEC, 1, Integer.MAX_VALUE)
            .fieldOf("cases").xmap(BiomeSwitchRule::new, BiomeSwitchRule::cases);

    public static final KeyDispatchDataCodec<BiomeSwitchRule> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final List<Case> cases;
    // Per biome the indices of the cases that name it, in order: built once, the surface pass applies the rule per chunk.
    private final Map<ResourceKey<Biome>, int[]> casesByBiome;

    /** @param cases in the order of the sequence */
    public BiomeSwitchRule(List<Case> cases) {
        this.cases = List.copyOf(cases);
        Map<ResourceKey<Biome>, List<Integer>> lists = new HashMap<>();
        for (int i = 0; i < this.cases.size(); i++) {
            for (ResourceKey<Biome> biome : this.cases.get(i).biomes()) {
                List<Integer> list = lists.computeIfAbsent(biome, key -> new ArrayList<>());
                // A biome named twice in one case still runs that case once.
                if (list.isEmpty() || list.get(list.size() - 1) != i) {
                    list.add(i);
                }
            }
        }
        Map<ResourceKey<Biome>, int[]> byBiome = new HashMap<>();
        lists.forEach((biome, list) -> byBiome.put(biome, list.stream().mapToInt(Integer::intValue).toArray()));
        this.casesByBiome = byBiome;
    }

    public List<Case> cases() {
        return cases;
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.RuleSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context context) {
        SurfaceRules.SurfaceRule[] rules = new SurfaceRules.SurfaceRule[cases.size()];
        for (int i = 0; i < rules.length; i++) {
            rules[i] = cases.get(i).rule().apply(context);
        }
        Map<ResourceKey<Biome>, int[]> byBiome = casesByBiome;
        return (x, y, z) -> {
            // Vanilla's biome condition: Holder.is(predicate) tests the key of a reference holder, a direct one never.
            int[] order = context.biome.get() instanceof Holder.Reference<Biome> biome ? byBiome.get(biome.key()) : null;
            if (order != null) {
                for (int i : order) {
                    BlockState state = rules[i].tryApply(x, y, z);
                    if (state != null) {
                        return state;
                    }
                }
            }
            return null;
        };
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BiomeSwitchRule rule && rule.cases.equals(cases);
    }

    @Override
    public int hashCode() {
        return cases.hashCode();
    }

    @Override
    public String toString() {
        return "BiomeSwitchRule[cases=" + cases + "]";
    }
}
