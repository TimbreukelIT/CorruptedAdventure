package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.biome.Ring3BiomeModifiers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring3OreFeaturesTest {
    private static final Codec<List<PlacementModifier>> MODIFIERS = PlacementModifier.CODEC.listOf();
    /** Registry-aware ops, as datagen encodes. */
    private static final RegistryOps<JsonElement> OPS = RegistryOps.create(JsonOps.INSTANCE,
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    private static final JsonElement STONE = JsonParser.parseString("{\"predicate_type\":\"minecraft:tag_match\",\"tag\":\"minecraft:stone_ore_replaceables\"}");
    private static final JsonElement DEEPSLATE = JsonParser.parseString("{\"predicate_type\":\"minecraft:tag_match\",\"tag\":\"minecraft:deepslate_ore_replaceables\"}");
    private static final JsonElement BASALT = JsonParser.parseString("{\"predicate_type\":\"minecraft:block_match\",\"block\":\"minecraft:basalt\"}");

    @Test
    void keysLiveUnderRing3() {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> placed = placed();
        List<ResourceKey<PlacedFeature>> expected = new ArrayList<>(RingVanillaOres.keys(Ring.RING_3));
        expected.addAll(List.of(Ring3OreFeatures.VOLCANITE_ORE, Ring3OreFeatures.VOLCANITE_ORE_NEAR_LAVA, Ring3OreFeatures.SULFUR_ORE,
                Ring3OreFeatures.SULFUR_ORE_RICH, Ring3OreFeatures.FIRE_RUBY_ORE));
        assertEquals(expected, List.copyOf(placed.keySet()));
        for (ResourceKey<PlacedFeature> key : placed.keySet()) {
            assertTrue(key.location().toString().startsWith("corruptedadventure:ring_3/"), key.toString());
        }
        for (ResourceKey<ConfiguredFeature<?, ?>> key : configured().keySet()) {
            assertTrue(key.location().toString().startsWith("corruptedadventure:ring_3/"), key.toString());
        }
    }

    /** G2-WB1 = A: coal, iron, copper ×0.35; gold, redstone, lapis ×0.3; diamond ×0.1. */
    @Test
    void vanillaOresAreTaperedFurtherThanInRing2() {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> placed = placed();
        for (ResourceKey<PlacedFeature> ore : RingVanillaOres.ORES) {
            String path = ore.location().getPath();
            double expected = path.startsWith("ore_diamond") ? 0.1
                    : path.startsWith("ore_gold") || path.startsWith("ore_redstone") || path.startsWith("ore_lapis") ? 0.3 : 0.35;
            JsonObject chance = ringChance(placed.get(RingVanillaOres.key(Ring.RING_3, ore)));
            assertEquals("ring_3", chance.get("ring").getAsString(), path);
            assertEquals(expected, chance.get("from_chance").getAsDouble(), 1e-12, path);
            assertEquals(expected, chance.get("to_chance").getAsDouble(), 1e-12, path);
            assertTrue(expected < Ring2Features.RING_2_FACTORS.get(ore), path + " is scarcer than in ring 2");
        }
    }

    /** In dry_riverbeds the ring 3 set stays in the ring 3 half: every own ore passes only in ring 3. */
    @Test
    void everyOwnOreHasTheRing3ChanceRightBeforeTheBiomeFilter() {
        Map<ResourceKey<PlacedFeature>, PlacedFeature> placed = placed();
        for (ResourceKey<PlacedFeature> key : List.of(Ring3OreFeatures.VOLCANITE_ORE, Ring3OreFeatures.VOLCANITE_ORE_NEAR_LAVA,
                Ring3OreFeatures.SULFUR_ORE, Ring3OreFeatures.SULFUR_ORE_RICH, Ring3OreFeatures.FIRE_RUBY_ORE)) {
            JsonArray modifiers = encode(placed.get(key).placement());
            int biome = -1;
            for (int i = 0; i < modifiers.size(); i++) {
                if (type(modifiers, i).equals("minecraft:biome")) {
                    biome = i;
                }
            }
            assertEquals("corruptedadventure:ring_chance", type(modifiers, biome - 1), key.toString());
            // Only the lava scan comes after them: the cheap filters first.
            assertTrue(biome == modifiers.size() - 1 || key.equals(Ring3OreFeatures.VOLCANITE_ORE_NEAR_LAVA) && biome == modifiers.size() - 2,
                    key.toString());
            JsonObject chance = modifiers.get(biome - 1).getAsJsonObject();
            assertEquals("progress", chance.get("signal").getAsString());
            assertEquals("ring_3", chance.get("ring").getAsString());
            assertEquals(1.0, chance.get("from_chance").getAsDouble());
            assertEquals(1.0, chance.get("to_chance").getAsDouble());
            assertEquals(encode(placed.get(key).placement()), encode(MODIFIERS.parse(OPS, modifiers).getOrThrow()), "round trip");
        }
        JsonArray nearLava = encode(placed.get(Ring3OreFeatures.VOLCANITE_ORE_NEAR_LAVA).placement());
        assertEquals("corruptedadventure:terrain_lava", type(nearLava, nearLava.size() - 1));
        String sulfur = encode(placed.get(Ring3OreFeatures.SULFUR_ORE).placement()).toString();
        // Below the terrain's own surface, not a heightmap that may stand on an island, arch or lava.
        assertTrue(sulfur.contains("corruptedadventure:terrain_height") && sulfur.contains("minecraft:random_offset"), sulfur);
        assertFalse(sulfur.contains("minecraft:heightmap"), sulfur);
    }

    /** Tags are not bound in unit tests, so the hosts are compared as encoded rule tests. */
    @Test
    void eachOreHasItsVariantPerHostRockAndNeverBlackstone() {
        Map<ResourceKey<ConfiguredFeature<?, ?>>, ConfiguredFeature<?, ?>> configured = configured();
        assertHosts(configured.get(Ring3OreFeatures.VOLCANITE_ORE_CONFIGURED), Map.of(
                STONE, ModBlocks.VOLCANITE_ORE.get(), DEEPSLATE, ModBlocks.DEEPSLATE_VOLCANITE_ORE.get(),
                BASALT, ModBlocks.BASALT_VOLCANITE_ORE.get()));
        assertHosts(configured.get(Ring3OreFeatures.SULFUR_ORE_CONFIGURED), Map.of(
                STONE, ModBlocks.SULFUR_ORE.get(), BASALT, ModBlocks.BASALT_SULFUR_ORE.get()));
        assertHosts(configured.get(Ring3OreFeatures.FIRE_RUBY_ORE_CONFIGURED), Map.of(
                STONE, ModBlocks.FIRE_RUBY_ORE.get(), DEEPSLATE, ModBlocks.DEEPSLATE_FIRE_RUBY_ORE.get(),
                BASALT, ModBlocks.BASALT_FIRE_RUBY_ORE.get()));
    }

    @Test
    void theModifierAddsVanillaThenTheOwnOres() {
        List<ResourceKey<PlacedFeature>> ores = Ring3BiomeModifiers.ores();
        assertEquals(RingVanillaOres.keys(Ring.RING_3), ores.subList(0, RingVanillaOres.ORES.size()));
        assertEquals(Ring3OreFeatures.OWN, ores.subList(RingVanillaOres.ORES.size(), ores.size()));
        assertFalse(ores.contains(Ring3OreFeatures.SULFUR_ORE_RICH), "the rich sulfur is sulfur_fields' own");
    }

    @Test
    void nearLavaIsTheTerrainsOwnLava() {
        TerrainLavaFilter.Lava sea = new TerrainLavaFilter.Lava(10, 2, Integer.MIN_VALUE);
        // A lava sea from Y 2 to 9 in the column at x 4; nothing elsewhere.
        TerrainLavaFilter.Columns columns = (x, z) -> x == 4 && z == 0 ? sea : TerrainLavaFilter.Lava.NONE;
        assertTrue(TerrainLavaFilter.near(columns, 0, 12, 0, 4, 2), "4 across, 3 above the top");
        assertTrue(TerrainLavaFilter.near(columns, 0, -2, 0, 4, 2), "4 below the bottom");
        assertFalse(TerrainLavaFilter.near(columns, 0, 14, 0, 4, 2), "5 above the top");
        assertFalse(TerrainLavaFilter.near(columns, -2, 5, 0, 4, 2), "6 across");
        // A spring counts at its own Y.
        TerrainLavaFilter.Columns spring = (x, z) -> x == 0 && z == 0 ? new TerrainLavaFilter.Lava(Integer.MIN_VALUE, Integer.MIN_VALUE, 30)
                : TerrainLavaFilter.Lava.NONE;
        assertTrue(TerrainLavaFilter.near(spring, 0, 27, 0, 4, 2));
        assertFalse(TerrainLavaFilter.near(spring, 0, 20, 0, 4, 2));
    }

    private static void assertHosts(ConfiguredFeature<?, ?> feature, Map<JsonElement, Block> expected) {
        assertNotNull(feature);
        OreConfiguration ore = (OreConfiguration) feature.config();
        Map<JsonElement, Block> actual = new LinkedHashMap<>();
        for (OreConfiguration.TargetBlockState target : ore.targetStates) {
            assertNull(actual.put(RuleTest.CODEC.encodeStart(JsonOps.INSTANCE, target.target).getOrThrow(), target.state.getBlock()));
        }
        assertEquals(expected, actual);
        assertFalse(actual.toString().contains("blackstone"));
    }

    private static JsonObject ringChance(PlacedFeature feature) {
        JsonArray modifiers = encode(feature.placement());
        for (int i = 0; i < modifiers.size(); i++) {
            if (type(modifiers, i).equals("corruptedadventure:ring_chance")) {
                return modifiers.get(i).getAsJsonObject();
            }
        }
        throw new AssertionError("no ring_chance in " + modifiers);
    }

    private static String type(JsonArray modifiers, int index) {
        return modifiers.get(index).getAsJsonObject().get("type").getAsString();
    }

    private static JsonArray encode(List<PlacementModifier> modifiers) {
        return MODIFIERS.encodeStart(OPS, modifiers).getOrThrow().getAsJsonArray();
    }

    private static Map<ResourceKey<PlacedFeature>, PlacedFeature> placed() {
        return record(Ring3OreFeatures::placed);
    }

    private static Map<ResourceKey<ConfiguredFeature<?, ?>>, ConfiguredFeature<?, ?>> configured() {
        return record(Ring3OreFeatures::configured);
    }

    private static <T> Map<ResourceKey<T>, T> record(Consumer<BootstrapContext<T>> bootstrap) {
        Map<ResourceKey<T>, T> registered = new LinkedHashMap<>();
        bootstrap.accept(new BootstrapContext<>() {
            @Override
            public Holder.Reference<T> register(ResourceKey<T> key, T value, Lifecycle lifecycle) {
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
