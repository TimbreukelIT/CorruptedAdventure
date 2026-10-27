package nl.timtoys.corruptedadventure.worldgen.surface;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The surface rules of the shipped noise settings as the datapack load reads them (registry ops: references stay
 * references), with the switches {@link BiomeSwitches} puts in, and the codecs of {@code y_bands} and
 * {@code biome_switch} on them.
 */
class SurfaceRuleCodecTest {
    private static final RegistryOps<JsonElement> OPS = RegistryOps.create(JsonOps.INSTANCE, new StandaloneLookup());

    private static SurfaceRules.RuleSource load(String dimension) {
        JsonElement json = WorldgenData.read("noise_settings/" + dimension + ".json").get("surface_rule");
        return SurfaceRules.RuleSource.CODEC.parse(OPS, json).getOrThrow();
    }

    @Test
    void theShippedRulesLoadWithTheirYBands() {
        assertEquals(13, count(load("overworld"), YBandsRule.class), "the ring 2 strata");
        assertEquals(0, count(load("nether"), YBandsRule.class));
    }

    /** The switched rule, read back as plain biome branches, is the loaded rule: it only regroups them. */
    @Test
    void theSwitchedRuleIsTheLoadedRuleRegrouped() {
        for (String dimension : List.of("overworld", "nether")) {
            SurfaceRules.RuleSource loaded = load(dimension);
            SurfaceRules.RuleSource switched = BiomeSwitches.of(loaded);
            // Above the preliminary surface (rings 2-5), inside rings 4 and 5; the Nether's ring branches.
            assertTrue(count(switched, BiomeSwitchRule.class) >= (dimension.equals("overworld") ? 4 : 1), dimension);
            assertEquals(loaded, plain(switched), dimension);
        }
    }

    /**
     * Encode and decode with registry ops, as a datapack would: the density functions of {@code ring4_threshold} stay
     * keys (not inlined), and the decoded rule is the same rule.
     */
    @Test
    void theNewRulesSurviveACodecRoundTrip() {
        SurfaceRules.RuleSource loaded = load("overworld");
        SurfaceRules.RuleSource switched = BiomeSwitches.of(loaded);
        JsonElement json = SurfaceRules.RuleSource.CODEC.encodeStart(OPS, switched).getOrThrow();
        String text = json.toString();
        assertTrue(text.contains("\"corruptedadventure:biome_switch\""));
        assertTrue(text.contains("\"corruptedadventure:y_bands\""));
        assertTrue(text.contains("\"density_function\":\"corruptedadventure:ring_4/overworld/rim\""));
        SurfaceRules.RuleSource decoded = SurfaceRules.RuleSource.CODEC.parse(OPS, json).getOrThrow();
        assertEquals(switched, decoded);
        assertEquals(json, SurfaceRules.RuleSource.CODEC.encodeStart(OPS, decoded).getOrThrow());
        assertEquals(loaded, plain(decoded));
    }

    /** {@code rule} with every switch back as the biome branches it replaced. */
    private static SurfaceRules.RuleSource plain(SurfaceRules.RuleSource rule) {
        if (rule instanceof SurfaceRules.TestRuleSource test) {
            return SurfaceRules.ifTrue(test.ifTrue(), plain(test.thenRun()));
        }
        if (rule instanceof SurfaceRules.SequenceRuleSource sequence) {
            List<SurfaceRules.RuleSource> out = new ArrayList<>();
            for (SurfaceRules.RuleSource child : sequence.sequence()) {
                if (child instanceof BiomeSwitchRule switched) {
                    for (BiomeSwitchRule.Case c : switched.cases()) {
                        @SuppressWarnings("unchecked")
                        ResourceKey<Biome>[] biomes = c.biomes().toArray(ResourceKey[]::new);
                        out.add(SurfaceRules.ifTrue(SurfaceRules.isBiome(biomes), plain(c.rule())));
                    }
                } else {
                    out.add(plain(child));
                }
            }
            return SurfaceRules.sequence(out.toArray(SurfaceRules.RuleSource[]::new));
        }
        return rule;
    }

    private static int count(SurfaceRules.RuleSource rule, Class<?> type) {
        int found = type.isInstance(rule) ? 1 : 0;
        for (SurfaceRules.RuleSource child : children(rule)) {
            found += count(child, type);
        }
        return found;
    }

    private static List<SurfaceRules.RuleSource> children(SurfaceRules.RuleSource rule) {
        if (rule instanceof SurfaceRules.TestRuleSource test) {
            return List.of(test.thenRun());
        }
        if (rule instanceof SurfaceRules.SequenceRuleSource sequence) {
            return sequence.sequence();
        }
        if (rule instanceof BiomeSwitchRule switched) {
            return switched.cases().stream().map(BiomeSwitchRule.Case::rule).toList();
        }
        return List.of();
    }

    /** Every registry, every key: one unbound stand-alone reference per key (the same one on every lookup). */
    private static final class StandaloneLookup implements RegistryOps.RegistryInfoLookup {
        private final Map<ResourceKey<?>, RegistryOps.RegistryInfo<?>> infos = new HashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
            return Optional.of((RegistryOps.RegistryInfo<E>) infos.computeIfAbsent(registryKey, key -> info()));
        }

        private static <E> RegistryOps.RegistryInfo<E> info() {
            HolderOwner<E> owner = new HolderOwner<>() {
            };
            Map<ResourceKey<E>, Holder.Reference<E>> references = new HashMap<>();
            HolderGetter<E> getter = new HolderGetter<>() {
                @Override
                public Optional<Holder.Reference<E>> get(ResourceKey<E> key) {
                    return Optional.of(references.computeIfAbsent(key, k -> Holder.Reference.createStandAlone(owner, k)));
                }

                @Override
                public Optional<HolderSet.Named<E>> get(TagKey<E> tag) {
                    return Optional.of(HolderSet.emptyNamed(owner, tag));
                }
            };
            return new RegistryOps.RegistryInfo<>(owner, getter, Lifecycle.stable());
        }
    }
}
