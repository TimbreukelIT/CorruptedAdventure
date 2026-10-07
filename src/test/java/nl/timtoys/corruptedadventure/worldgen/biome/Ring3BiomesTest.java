package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring3BiomesTest {
    @Test
    void bootstrapRegistersExactlyTheRing3List() {
        assertEquals(List.copyOf(Ring3Biomes.ALL), List.copyOf(bootstrap().keySet()));
        assertEquals(Set.of(ModBiomes.ASHEN_WASTES, ModBiomes.VOLCANIC_PEAKS, ModBiomes.EMBER_WOODS, ModBiomes.BASALT_FLOWS,
                ModBiomes.SULFUR_FIELDS, ModBiomes.DRIED_SEABED, ModBiomes.TITAN_FIELDS, ModBiomes.DORMANT_CALDERA), Set.copyOf(Ring3Biomes.ALL));
        assertFalse(Ring3Biomes.ALL.contains(ModBiomes.STEAM_FENS), "steam_fens follows in wave 2");
    }

    @Test
    void dryHotAndEveryBiomeHasItsOwnSkyAndFog() {
        Set<Integer> skies = new HashSet<>();
        Set<Integer> fogs = new HashSet<>();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            assertFalse(biome.hasPrecipitation(), entry.getKey().toString());
            assertEquals(2.0F, biome.getBaseTemperature(), entry.getKey().toString());
            BiomeAmbience.Palette palette = Ring3Biomes.PALETTES.get(entry.getKey());
            assertEquals(palette.sky(), biome.getSkyColor(), entry.getKey().toString());
            assertEquals(palette.fog(), biome.getFogColor(), entry.getKey().toString());
            skies.add(biome.getSkyColor());
            fogs.add(biome.getFogColor());
        }
        assertEquals(Ring3Biomes.ALL.size(), skies.size(), "every sky differs");
        assertEquals(Ring3Biomes.ALL.size(), fogs.size(), "every fog differs");
        // The sky turns at the boundary: no ring 3 sky is that of the faded ring 2 transition.
        assertFalse(skies.contains(BiomeAmbience.Palette.DRY_RIVERBEDS.sky()));
    }

    @Test
    void noAnimalsButGoatsOnThePeaks() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        MobSpawnSettings reference = biomes.get(ModBiomes.ASHEN_WASTES).getMobSettings();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
            MobSpawnSettings spawns = entry.getValue().getMobSettings();
            assertTrue(spawns.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(data -> data.type == EntityType.HUSK));
            // Monsters, bats and the rest as everywhere in the ring (SpawnerData has no equals: compare its toString).
            for (MobCategory category : MobCategory.values()) {
                if (category != MobCategory.CREATURE) {
                    assertEquals(reference.getMobs(category).unwrap().toString(), spawns.getMobs(category).unwrap().toString(),
                            entry.getKey() + " " + category);
                }
            }
            List<MobSpawnSettings.SpawnerData> creatures = spawns.getMobs(MobCategory.CREATURE).unwrap();
            if (entry.getKey().equals(ModBiomes.VOLCANIC_PEAKS)) {
                assertEquals(1, creatures.size());
                MobSpawnSettings.SpawnerData goats = creatures.getFirst();
                assertEquals(EntityType.GOAT, goats.type);
                assertEquals(1, goats.minCount);
                assertEquals(3, goats.maxCount);
                assertEquals(Ring3Biomes.GOAT_WEIGHT, goats.getWeight().asInt());
                assertTrue(spawns.getCreatureProbability() < 0.1F, "rarer than vanilla's 0.1 at generation");
                // The only animal of the biome: a spawn cost keeps the natural spawns sparse.
                MobSpawnSettings.MobSpawnCost cost = spawns.getMobSpawnCost(EntityType.GOAT);
                assertNotNull(cost);
                assertEquals(Ring3Biomes.GOAT_CHARGE, cost.charge());
                assertEquals(Ring3Biomes.GOAT_ENERGY_BUDGET, cost.energyBudget());
            } else {
                assertTrue(creatures.isEmpty(), entry.getKey().toString());
            }
        }
    }

    @Test
    void onlyDeepCarversAndNoFeaturesYet() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            assertEquals(List.copyOf(Ring2Carvers.COPIES.keySet()),
                    StreamSupport.stream(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).spliterator(), false)
                            .map(holder -> holder.unwrapKey().orElseThrow()).toList(), entry.getKey().toString());
            // Wave 1 adds no feature, so the global feature order of rings 1 and 2 stays as it was.
            assertTrue(biome.getGenerationSettings().features().stream().allMatch(step -> step.size() == 0), entry.getKey().toString());
        }
    }

    /**
     * Each ring 3 biome has its own branch in {@code above_preliminary_surface}, before vanilla's rules: a top block for
     * the surface and an unconditional block for the whole surface depth, so vanilla's grass and dirt never reach it.
     */
    @Test
    void everyBiomeHasItsOwnSurfaceBranchWithoutGrass() {
        JsonArray rules = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule")
                .getAsJsonArray("sequence").get(1).getAsJsonObject().getAsJsonObject("then_run").getAsJsonArray("sequence");
        Map<String, JsonObject> branches = new LinkedHashMap<>();
        int firstVanilla = -1;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject condition = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (condition.get("type").getAsString().equals("minecraft:biome")) {
                JsonArray biomes = condition.getAsJsonArray("biome_is");
                if (biomes.size() == 1 && biomes.get(0).getAsString().startsWith("corruptedadventure:")) {
                    branches.put(biomes.get(0).getAsString(), rules.get(i).getAsJsonObject().getAsJsonObject("then_run"));
                    assertTrue(firstVanilla < 0, biomes.get(0).getAsString() + " after vanilla's rules");
                    continue;
                }
            }
            if (firstVanilla < 0) {
                firstVanilla = i;
            }
        }
        for (ResourceKey<Biome> biome : Ring3Biomes.ALL) {
            JsonObject branch = branches.get(biome.location().toString());
            assertNotNull(branch, biome.toString());
            String text = branch.toString();
            for (String vanilla : new String[]{"minecraft:grass_block", "minecraft:dirt\"", "minecraft:podzol", "minecraft:mycelium"}) {
                assertFalse(text.contains(vanilla), biome + " uses " + vanilla);
            }
            boolean top = false;
            boolean layer = false;
            for (JsonElement element : branch.getAsJsonArray("sequence")) {
                JsonObject rule = element.getAsJsonObject();
                JsonObject condition = rule.getAsJsonObject("if_true");
                if (!condition.get("type").getAsString().equals("minecraft:stone_depth")) {
                    continue;
                }
                JsonObject then = rule.getAsJsonObject("then_run");
                if (condition.get("add_surface_depth").getAsBoolean()) {
                    layer |= then.get("type").getAsString().equals("minecraft:block");
                } else {
                    JsonArray choices = then.getAsJsonArray("sequence");
                    top |= choices.get(choices.size() - 1).getAsJsonObject().get("type").getAsString().equals("minecraft:block");
                }
            }
            assertTrue(top, biome + ": no unconditional top block");
            assertTrue(layer, biome + ": no unconditional block for the surface depth");
        }
    }

    static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring3Biomes.bootstrap(new BootstrapContext<>() {
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
