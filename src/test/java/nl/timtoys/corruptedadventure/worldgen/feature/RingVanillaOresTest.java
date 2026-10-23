package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import nl.timtoys.corruptedadventure.ring.Ring;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingVanillaOresTest {
    private static final Codec<List<PlacementModifier>> MODIFIERS = PlacementModifier.CODEC.listOf();

    @Test
    void oresAreThoseOfAddDefaultOresInVanillaOrder() {
        BiomeGenerationSettings.Builder builder = new BiomeGenerationSettings.Builder(new Getter<>(), new Getter<>());
        BiomeDefaultFeatures.addDefaultOres(builder);
        List<ResourceKey<PlacedFeature>> vanilla = new ArrayList<>(builder.build().features()
                .get(GenerationStep.Decoration.UNDERGROUND_ORES.ordinal()).stream()
                .map(holder -> holder.unwrapKey().orElseThrow()).toList());
        assertTrue(vanilla.remove(CavePlacements.UNDERWATER_MAGMA), "the one non-ore of addDefaultOres stays untapered");
        assertEquals(vanilla, RingVanillaOres.ORES);
    }

    @Test
    void eachTaperedOreIsVanillaPlusOneRingChanceBeforeTheBiomeFilter() {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> vanilla = record(OrePlacements::bootstrap);
        Map<ResourceKey<PlacedFeature>, PlacedFeature> tapered = record(context -> RingVanillaOres.placed(context, Ring.RING_2, Ring2Features.RING_2_FACTORS));
        assertEquals(RingVanillaOres.keys(Ring.RING_2), List.copyOf(tapered.keySet()));

        for (ResourceKey<PlacedFeature> ore : RingVanillaOres.ORES) {
            PlacedFeature original = vanilla.get(ore);
            PlacedFeature scaled = tapered.get(RingVanillaOres.key(Ring.RING_2, ore));
            String name = ore.location().getPath();
            assertEquals(original.feature().unwrapKey(), scaled.feature().unwrapKey(), name);

            JsonArray modifiers = encode(scaled.placement());
            List<Integer> chances = new ArrayList<>();
            JsonArray rest = new JsonArray();
            for (int i = 0; i < modifiers.size(); i++) {
                if (type(modifiers.get(i)).equals("corruptedadventure:ring_chance")) {
                    chances.add(i);
                } else {
                    rest.add(modifiers.get(i));
                }
            }
            assertEquals(1, chances.size(), name);
            int chance = chances.getFirst();
            assertEquals("minecraft:biome", type(modifiers.get(chance + 1)), name + ": ring_chance right before the biome filter");
            assertEquals(encode(original.placement()), rest, name + ": vanilla's modifiers, in vanilla's order");

            JsonObject filter = modifiers.get(chance).getAsJsonObject();
            double factor = expectedFactor(name);
            assertEquals("progress", filter.get("signal").getAsString(), name);
            assertEquals("ring_2", filter.get("ring").getAsString(), name);
            assertEquals(factor, filter.get("from_chance").getAsDouble(), 1e-12, name);
            assertEquals(factor, filter.get("to_chance").getAsDouble(), 1e-12, name);
        }
    }

    @Test
    void modifiersRoundTripThroughTheCodec() {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> tapered = record(context -> RingVanillaOres.placed(context, Ring.RING_2, Ring2Features.RING_2_FACTORS));
        for (PlacedFeature feature : tapered.values()) {
            JsonArray json = encode(feature.placement());
            assertEquals(json, encode(MODIFIERS.parse(JsonOps.INSTANCE, json).getOrThrow()));
        }
    }

    @Test
    void keysFollowTheRing() {
        assertEquals("corruptedadventure:ring_2/ore_iron_upper", RingVanillaOres.key(Ring.RING_2, OrePlacements.ORE_IRON_UPPER).location().toString());
        assertEquals("corruptedadventure:ring_3/ore_diamond", RingVanillaOres.key(Ring.RING_3, OrePlacements.ORE_DIAMOND).location().toString());
    }

    @Test
    void factorsCoverEveryOreAndStayInRange() {
        Map<ResourceKey<PlacedFeature>, Double> factors = RingVanillaOres.factors(0.6, 0.5, 0.25);
        assertEquals(RingVanillaOres.ORES.size(), factors.size());
        assertEquals(Ring2Features.RING_2_FACTORS, factors);
        for (ResourceKey<PlacedFeature> ore : RingVanillaOres.ORES) {
            assertEquals(expectedFactor(ore.location().getPath()), factors.get(ore), ore.location().toString());
        }
        assertThrows(IllegalArgumentException.class, () -> RingVanillaOres.factors(-0.1, 0.5, 0.25));
        // 95: a factor 0 leaves the ore out (ring 5's diamond): not placed, not among the keys.
        Map<ResourceKey<PlacedFeature>, Double> noDiamond = RingVanillaOres.factors(0.05, 0.03, 0.0);
        List<ResourceKey<PlacedFeature>> keys = RingVanillaOres.keys(Ring.RING_5, noDiamond);
        assertEquals(RingVanillaOres.ORES.stream().filter(ore -> !ore.location().getPath().startsWith("ore_diamond")).count(), keys.size());
        assertTrue(keys.stream().noneMatch(key -> key.location().getPath().contains("diamond")));
        assertThrows(IllegalArgumentException.class, () -> RingVanillaOres.factors(0.6, 1.5, 0.25));
        assertThrows(IllegalArgumentException.class, () -> record(context -> RingVanillaOres.placed(context, Ring.RING_2, Map.of())));
    }

    @Test
    void taperingNeedsExactlyOneBiomeFilter() {
        PlacedFeature withoutBiome = new PlacedFeature(Holder.direct(null), List.of());
        assertThrows(IllegalStateException.class, () -> RingVanillaOres.tapered(withoutBiome, RingChanceFilter.progress(Ring.RING_2, 0.5, 0.5, 0.0, 1.0)));
    }

    /** Coal, iron, copper ×0.6; gold, redstone, lapis ×0.5; diamond ×0.25 (change 25, question 1 A). */
    private static double expectedFactor(String path) {
        if (path.startsWith("ore_diamond")) {
            return 0.25;
        }
        if (path.startsWith("ore_gold") || path.startsWith("ore_redstone") || path.startsWith("ore_lapis")) {
            return 0.5;
        }
        assertTrue(path.startsWith("ore_coal") || path.startsWith("ore_iron") || path.startsWith("ore_copper"), path);
        return 0.6;
    }

    private static String type(JsonElement modifier) {
        return modifier.getAsJsonObject().get("type").getAsString();
    }

    private static JsonArray encode(List<PlacementModifier> modifiers) {
        return MODIFIERS.encodeStart(JsonOps.INSTANCE, modifiers).getOrThrow().getAsJsonArray();
    }

    private static Map<ResourceKey<PlacedFeature>, PlacedFeature> record(Consumer<BootstrapContext<PlacedFeature>> bootstrap) {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> registered = new LinkedHashMap<>();
        bootstrap.accept(new BootstrapContext<>() {
            @Override
            public Holder.Reference<PlacedFeature> register(ResourceKey<PlacedFeature> key, PlacedFeature value, Lifecycle lifecycle) {
                registered.put(key, value);
                return null;
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return new Getter<>();
            }
        });
        return registered;
    }

    /** Every key exists, as an unbound stand-alone reference; every tag is empty. */
    private static final class Getter<T> implements HolderGetter<T> {
        private final HolderOwner<T> owner = new HolderOwner<>() {
        };

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
            return Optional.of(Holder.Reference.createStandAlone(owner, key));
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
            return Optional.of(HolderSet.emptyNamed(owner, tag));
        }
    }
}
