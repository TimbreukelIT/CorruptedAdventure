package nl.timtoys.corruptedadventure.worldgen.biome;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.CavePlacements;
import net.minecraft.data.worldgen.placement.MiscOverworldPlacements;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.feature.RingVanillaOres;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring2BiomesTest {
    @Test
    void bootstrapRegistersExactlyTheRing2List() {
        assertEquals(Set.copyOf(Ring2Biomes.ALL), bootstrap().keySet());
        assertEquals(Set.of(ModBiomes.CANYONLANDS, ModBiomes.OASIS_FLATS, ModBiomes.HOODOO_FIELDS, ModBiomes.SALT_FLATS, ModBiomes.DUNE_SEA),
                Set.copyOf(Ring2Biomes.ALL));
        assertEquals(5, Ring2Biomes.ALL.size());
    }

    @Test
    void dryHotAndTheRing2Palette() {
        BiomeAmbience.Palette palette = BiomeAmbience.Palette.RING_2;
        for (Biome biome : bootstrap().values()) {
            assertFalse(biome.hasPrecipitation());
            assertEquals(2.0F, biome.getBaseTemperature());
            BiomeSpecialEffects effects = biome.getSpecialEffects();
            assertEquals(palette.sky(), effects.getSkyColor());
            assertEquals(palette.fog(), effects.getFogColor());
            assertEquals(palette.water(), effects.getWaterColor());
            assertEquals(palette.waterFog(), effects.getWaterFogColor());
        }
    }

    @Test
    void desertSpawnsCamelsOnlyInTheOasisAndNoAnimalsOnTheSalt() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
            Biome biome = entry.getValue();
            assertTrue(spawns(biome, MobCategory.MONSTER, EntityType.HUSK));
            boolean salt = entry.getKey().equals(ModBiomes.SALT_FLATS);
            assertEquals(!salt, spawns(biome, MobCategory.CREATURE, EntityType.RABBIT), entry.getKey().toString());
            assertEquals(entry.getKey().equals(ModBiomes.OASIS_FLATS), spawns(biome, MobCategory.CREATURE, EntityType.CAMEL));
        }
        Biome salt = biomes.get(ModBiomes.SALT_FLATS);
        assertTrue(salt.getMobSettings().getMobs(MobCategory.CREATURE).isEmpty());
        // Only the animals go: monsters and bats as in the other biomes.
        Biome dunes = biomes.get(ModBiomes.DUNE_SEA);
        for (MobCategory category : MobCategory.values()) {
            if (category != MobCategory.CREATURE) {
                // SpawnerData has no equals: compare type, weight and group sizes (its toString).
                assertEquals(dunes.getMobSettings().getMobs(category).unwrap().toString(), salt.getMobSettings().getMobs(category).unwrap().toString(),
                        category.toString());
            }
        }
    }

    @Test
    void ring2BiomesAreNoVanillaCopiesSoGetNoMinecraftTags() {
        // ModBiomeTagsProvider only mirrors minecraft: tags onto the copies of vanilla biomes.
        for (ResourceKey<Biome> biome : Ring2Biomes.ALL) {
            assertFalse(VanillaBiomeCopies.COPIES.stream().map(VanillaBiomeCopies::copyOf).anyMatch(biome::equals), biome.toString());
        }
    }

    @Test
    void ownFeaturesLastInTheirStepInAFixedOrder() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        Biome canyonlands = biomes.get(ModBiomes.CANYONLANDS);
        List<ResourceKey<PlacedFeature>> rocks = step(canyonlands, GenerationStep.Decoration.LOCAL_MODIFICATIONS);
        assertEquals(Ring2Biomes.CANYONLANDS_ROCKS, rocks.subList(rocks.size() - Ring2Biomes.CANYONLANDS_ROCKS.size(), rocks.size()));
        assertEquals(ModPlacedFeatures.CANYON_SPRINGS, rocks.getLast(), "the spring last: it avoids the boulders of its chunk");
        assertEquals(ModPlacedFeatures.CANYONLANDS_VEGETATION, step(canyonlands, GenerationStep.Decoration.VEGETAL_DECORATION).getLast());

        Biome oasis = biomes.get(ModBiomes.OASIS_FLATS);
        assertEquals(ModPlacedFeatures.OASIS_ROCKS, step(oasis, GenerationStep.Decoration.LOCAL_MODIFICATIONS).getLast());
        // After vanilla's glow lichen only our own plants: vanilla's sugar cane random_patch is replaced by the
        // chunk-local shore plants.
        List<ResourceKey<PlacedFeature>> plants = step(oasis, GenerationStep.Decoration.VEGETAL_DECORATION);
        assertEquals(Ring2Biomes.OASIS_VEGETATION, plants.subList(plants.size() - Ring2Biomes.OASIS_VEGETATION.size(), plants.size()));
        assertFalse(plants.contains(VegetationPlacements.PATCH_SUGAR_CANE_DESERT));
        assertTrue(Ring2Biomes.OASIS_VEGETATION.indexOf(ModPlacedFeatures.OASIS_SHORE_PLANTS)
                < Ring2Biomes.OASIS_VEGETATION.indexOf(ModPlacedFeatures.OASIS_PALM_GROVES));

        // Change 30: the hoodoo fields reuse the canyon rockfall (same place in the step), each biome its own plants.
        Biome hoodoos = biomes.get(ModBiomes.HOODOO_FIELDS);
        assertEquals(ModPlacedFeatures.CANYON_ROCKFALL, step(hoodoos, GenerationStep.Decoration.LOCAL_MODIFICATIONS).getLast());
        assertTrue(Ring2Biomes.CANYONLANDS_ROCKS.contains(ModPlacedFeatures.CANYON_ROCKFALL));
        // Change 30c: the hoodoo flora, then the pillar feet and the cactus groups.
        assertEquals(List.of(ModPlacedFeatures.HOODOO_FIELDS_VEGETATION, ModPlacedFeatures.HOODOO_PILLAR_FOOT_FLORA,
                ModPlacedFeatures.HOODOO_CACTUS_GROUPS), own(hoodoos, GenerationStep.Decoration.VEGETAL_DECORATION));

        // Change 30b: shapes and water of the salt flats and the dune sea, the pools first (later features fail on
        // their water), the crystal fields after the single crystals.
        // Change 30c: fossils and bones after the shapes, the island life after the crystals.
        assertEquals(List.of(ModPlacedFeatures.SALT_FLATS_BRINE_POOLS, ModPlacedFeatures.SALT_FLATS_PILLAR_FIELDS,
                ModPlacedFeatures.SALT_FLATS_PILLARS, ModPlacedFeatures.SALT_ISLAND_ROCKS, ModPlacedFeatures.SALT_ISLAND_FOSSILS,
                ModPlacedFeatures.SALT_ISLAND_BONES), own(biomes.get(ModBiomes.SALT_FLATS), GenerationStep.Decoration.LOCAL_MODIFICATIONS));
        assertEquals(List.of(ModPlacedFeatures.SALT_FLATS_CRYSTALS, ModPlacedFeatures.SALT_FLATS_CRYSTAL_FIELDS,
                ModPlacedFeatures.SALT_ISLAND_FLORA, ModPlacedFeatures.SALT_ISLAND_CACTUS),
                own(biomes.get(ModBiomes.SALT_FLATS), GenerationStep.Decoration.VEGETAL_DECORATION));
        assertEquals(List.of(ModPlacedFeatures.DUNE_SEA_OASES, ModPlacedFeatures.DUNE_SEA_OUTCROPS, ModPlacedFeatures.DUNE_SEA_FOSSILS,
                ModPlacedFeatures.DUNE_SEA_BONES), own(biomes.get(ModBiomes.DUNE_SEA), GenerationStep.Decoration.LOCAL_MODIFICATIONS));
        assertEquals(List.of(ModPlacedFeatures.DUNE_SEA_VEGETATION, ModPlacedFeatures.DUNE_SEA_VALLEY_FLORA, ModPlacedFeatures.DUNE_SEA_VALLEY_GRASS,
                ModPlacedFeatures.DUNE_SEA_CACTUS_GROUPS), own(biomes.get(ModBiomes.DUNE_SEA), GenerationStep.Decoration.VEGETAL_DECORATION));
        // The oasis flowers and shrubs after the palms (a standing palm blocks a shrub, never the other way round).
        List<ResourceKey<PlacedFeature>> oasisPlants = Ring2Biomes.OASIS_VEGETATION;
        for (ResourceKey<PlacedFeature> late : List.of(ModPlacedFeatures.OASIS_FLOWERS_WARM, ModPlacedFeatures.OASIS_FLOWERS_WHITE,
                ModPlacedFeatures.OASIS_FLOWERS_PURPLE, ModPlacedFeatures.OASIS_EDGE_SHRUBS)) {
            for (ResourceKey<PlacedFeature> palm : List.of(ModPlacedFeatures.OASIS_PALM_GROVES, ModPlacedFeatures.OASIS_PALMS_SAND,
                    ModPlacedFeatures.OASIS_PALM_BUSHES)) {
                assertTrue(oasisPlants.indexOf(palm) < oasisPlants.indexOf(late), late + " after " + palm);
            }
        }
        // Every placed feature of ours belongs to one biome only (the canyon rockfall, shared by the hoodoo fields in the
        // same place of the step, aside): no order between biomes (no "Feature order cycle"). Configured features are
        // shared freely (cactus_column, bone_rubble, desert_fossil).
        Map<ResourceKey<PlacedFeature>, ResourceKey<Biome>> owner = new LinkedHashMap<>();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
            for (GenerationStep.Decoration decoration : GenerationStep.Decoration.values()) {
                for (ResourceKey<PlacedFeature> feature : own(entry.getValue(), decoration)) {
                    if (!feature.equals(ModPlacedFeatures.CANYON_ROCKFALL)) {
                        ResourceKey<Biome> before = owner.put(feature, entry.getKey());
                        assertTrue(before == null || before.equals(entry.getKey()), feature + " in two biomes");
                    }
                }
            }
        }
    }

    @Test
    void everyRing2BiomeHasTheSameGlobalFeaturesBeforeItsOwn() {
        // One global order per step (no "Feature order cycle"): the shared base is identical in every ring 2 biome.
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        Biome canyonlands = biomes.get(ModBiomes.CANYONLANDS);
        for (Biome biome : biomes.values()) {
            for (GenerationStep.Decoration decoration : GenerationStep.Decoration.values()) {
                List<ResourceKey<PlacedFeature>> base = step(canyonlands, decoration).stream()
                        .filter(key -> !key.location().getNamespace().equals("corruptedadventure")).toList();
                List<ResourceKey<PlacedFeature>> own = step(biome, decoration).stream()
                        .filter(key -> !key.location().getNamespace().equals("corruptedadventure")).toList();
                assertEquals(base, own, decoration.toString());
            }
        }
    }

    @Test
    void noVanillaOresButUnderwaterMagmaAndTheRing2OresThroughOneModifier() {
        for (Biome biome : bootstrap().values()) {
            List<ResourceKey<PlacedFeature>> ores = step(biome, GenerationStep.Decoration.UNDERGROUND_ORES);
            for (ResourceKey<PlacedFeature> ore : RingVanillaOres.ORES) {
                assertFalse(ores.contains(ore), ore.toString());
            }
            assertTrue(ores.contains(CavePlacements.UNDERWATER_MAGMA));
            // Vanilla's disks write into neighbouring chunks; ring 2 has chunk-local ones in their place, in vanilla's order.
            assertEquals(Ring2Biomes.DISKS, ores.subList(ores.size() - Ring2Biomes.DISKS.size(), ores.size()));
            for (ResourceKey<PlacedFeature> disk : List.of(MiscOverworldPlacements.DISK_SAND, MiscOverworldPlacements.DISK_CLAY,
                    MiscOverworldPlacements.DISK_GRAVEL)) {
                assertFalse(ores.contains(disk), disk.toString());
            }
        }
        List<ResourceKey<PlacedFeature>> added = Ring2BiomeModifiers.ores();
        assertEquals(RingVanillaOres.keys(Ring.RING_2), added.subList(0, RingVanillaOres.ORES.size()));
        assertEquals(List.of(ModPlacedFeatures.SUNSTONE_ORE, ModPlacedFeatures.TURQUOISE_ORE), added.subList(RingVanillaOres.ORES.size(), added.size()));
    }

    @Test
    void noLocalLavaDeepCarversAndWaterSprings() {
        for (Biome biome : bootstrap().values()) {
            List<ResourceKey<PlacedFeature>> all = biome.getGenerationSettings().features().stream()
                    .flatMap(HolderSet::stream).map(holder -> holder.unwrapKey().orElseThrow()).toList();
            for (ResourceKey<PlacedFeature> lava : List.of(MiscOverworldPlacements.LAKE_LAVA_UNDERGROUND, MiscOverworldPlacements.LAKE_LAVA_SURFACE,
                    MiscOverworldPlacements.SPRING_LAVA, MiscOverworldPlacements.SPRING_LAVA_FROZEN)) {
                assertFalse(all.contains(lava), lava.toString());
            }
            assertEquals(List.of(MiscOverworldPlacements.SPRING_WATER), step(biome, GenerationStep.Decoration.FLUID_SPRINGS));
            assertTrue(step(biome, GenerationStep.Decoration.LAKES).isEmpty());
            assertEquals(List.copyOf(Ring2Carvers.COPIES.keySet()),
                    StreamSupport.stream(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).spliterator(), false)
                            .map(holder -> holder.unwrapKey().orElseThrow()).toList());
        }
    }

    private static List<ResourceKey<PlacedFeature>> step(Biome biome, GenerationStep.Decoration step) {
        List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
        return steps.size() <= step.ordinal() ? List.of()
                : steps.get(step.ordinal()).stream().map(holder -> holder.unwrapKey().orElseThrow()).toList();
    }

    /** Our own features of a step, in order. */
    private static List<ResourceKey<PlacedFeature>> own(Biome biome, GenerationStep.Decoration step) {
        return step(biome, step).stream().filter(key -> key.location().getNamespace().equals("corruptedadventure")
                && !key.location().getPath().startsWith("ring_2/")).toList();
    }

    private static boolean spawns(Biome biome, MobCategory category, EntityType<?> type) {
        return biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type);
    }

    private static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring2Biomes.bootstrap(new BootstrapContext<>() {
            @Override
            public Holder.Reference<Biome> register(ResourceKey<Biome> key, Biome value, Lifecycle lifecycle) {
                registered.put(key, value);
                return Holder.Reference.createStandAlone(biomes.owner(), key);
            }

            @Override
            public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> registryKey) {
                return new StandaloneHolderGetter<>(key -> true);
            }
        });
        return registered;
    }
}
