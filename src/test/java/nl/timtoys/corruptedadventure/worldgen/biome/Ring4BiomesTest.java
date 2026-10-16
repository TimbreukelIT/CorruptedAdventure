package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.carver.Ring2Carvers;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4Features;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring4MythicFeatures;
import nl.timtoys.corruptedadventure.worldgen.feature.VerdantFallsFeatures;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ring4BiomesTest {
    private static final String GENERATED_TAGS = "src/generated/resources/data/corruptedadventure/tags/worldgen/biome/";

    @Test
    void bootstrapRegistersExactlyTheRing4List() {
        assertEquals(Ring4Biomes.WITH_TRANSITION, List.copyOf(bootstrap().keySet()));
        assertEquals(12, Set.copyOf(Ring4Biomes.WITH_TRANSITION).size());
        assertEquals(Set.of(ModBiomes.CASCADE_VALE, ModBiomes.CRYSTAL_LAGOON, ModBiomes.LOTUS_SHALLOWS, ModBiomes.GLOWROOT_SWAMP,
                ModBiomes.MIST_FJORDS), Set.copyOf(Ring4Biomes.MYTHIC));
        assertEquals(Set.of(ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH), Set.copyOf(Ring4Biomes.BLIGHTED));
        assertEquals(List.of(ModBiomes.SKY_GARDENS, ModBiomes.WITHERED_GARDENS), Ring4Biomes.HEIGHT);
        assertEquals(List.of(ModBiomes.SUNDERED_REACH, ModBiomes.VERDANT_FALLS), Ring4Biomes.TRANSITIONS);
        for (ResourceKey<Biome> transition : Ring4Biomes.TRANSITIONS) {
            assertFalse(Ring4Biomes.ALL.contains(transition), transition + ": a transition is not in the ring's own source");
        }
        assertEquals(Set.of(ModBiomes.DROWNED_FOREST, ModBiomes.PEAT_MOORS, ModBiomes.REED_MARSH, ModBiomes.WITHERED_GARDENS),
                Set.copyOf(Ring4Biomes.BLIGHTED_TAG));
        assertThrows(IllegalArgumentException.class, () -> Ring4Biomes.climate(ModBiomes.STEAM_FENS));
    }

    /** Mild and wet everywhere; every effect is the atmosphere's hook (change 80). */
    @Test
    void mildWetAndEveryEffectFromTheAtmosphere() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            BiomeSpecialEffects hook = BiomeAmbience.ring4(entry.getKey()).build();
            assertEquals(Ring4Biomes.TEMPERATURE, biome.getBaseTemperature(), entry.getKey().toString());
            assertTrue(Ring4Biomes.TEMPERATURE - 0.3F > 0.15F, "no snow, even 240 blocks above Y 80");
            assertEquals(!entry.getKey().equals(ModBiomes.SUNDERED_REACH), biome.hasPrecipitation(), entry.getKey().toString());
            assertEquals(Ring4Biomes.climate(entry.getKey()).downfall(), biome.getModifiedClimateSettings().downfall());
            assertEquals(hook.getSkyColor(), biome.getSkyColor());
            assertEquals(hook.getFogColor(), biome.getFogColor());
            assertEquals(hook.getWaterColor(), biome.getWaterColor());
            assertEquals(hook.getWaterFogColor(), biome.getWaterFogColor());
            assertEquals(hook.getGrassColorOverride(), biome.getSpecialEffects().getGrassColorOverride());
            assertEquals(hook.getAmbientLoopSoundEvent(), biome.getSpecialEffects().getAmbientLoopSoundEvent());
        }
    }

    @Test
    void monstersEverywhereAnimalsOnlyInTheMythicalBand() {
        Map<ResourceKey<Biome>, Biome> biomes = bootstrap();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : biomes.entrySet()) {
            MobSpawnSettings spawns = entry.getValue().getMobSettings();
            assertTrue(spawns.getMobs(MobCategory.MONSTER).unwrap().stream().anyMatch(data -> data.type == EntityType.ZOMBIE), entry.getKey().toString());
            assertTrue(spawns.getMobs(MobCategory.AMBIENT).unwrap().stream().anyMatch(data -> data.type == EntityType.BAT), entry.getKey().toString());
            Set<EntityType<?>> animals = animals(spawns);
            if (Ring4Biomes.BLIGHTED_TAG.contains(entry.getKey()) || entry.getKey().equals(ModBiomes.SUNDERED_REACH)) {
                assertEquals(Set.of(), animals, entry.getKey() + ": the blighted band is empty");
            }
        }
        assertTrue(animals(biomes.get(ModBiomes.CRYSTAL_LAGOON).getMobSettings())
                .containsAll(Set.of(EntityType.TROPICAL_FISH, EntityType.TURTLE, EntityType.DOLPHIN)));
        assertTrue(animals(biomes.get(ModBiomes.GLOWROOT_SWAMP).getMobSettings()).containsAll(Set.of(EntityType.FROG, EntityType.AXOLOTL)));
        // Vanilla's category for axolotls (lush caves), with its own cap.
        assertTrue(biomes.get(ModBiomes.GLOWROOT_SWAMP).getMobSettings().getMobs(MobCategory.AXOLOTLS).unwrap().stream()
                .anyMatch(data -> data.type == EntityType.AXOLOTL));
        assertTrue(animals(biomes.get(ModBiomes.MIST_FJORDS).getMobSettings()).contains(EntityType.RABBIT));
        assertThrows(IllegalArgumentException.class, () -> Ring4Biomes.spawns(ModBiomes.STEAM_FENS));
        assertTrue(animals(biomes.get(ModBiomes.CASCADE_VALE).getMobSettings()).containsAll(Set.of(EntityType.FOX, EntityType.RABBIT)));
        // Q9 = A: frogs and turtles by the lakes of the descent, no parrots until the own trees.
        assertEquals(Set.of(EntityType.FROG, EntityType.TURTLE), animals(biomes.get(ModBiomes.VERDANT_FALLS).getMobSettings()));
    }

    /** verdant_falls: the wettest of the ring, and rainy. */
    @Test
    void verdantFallsIsTheWettest() {
        assertEquals(new Ring4Biomes.Climate(0.95F, true), Ring4Biomes.climate(ModBiomes.VERDANT_FALLS));
    }

    /** The animals a biome adds to vanilla's common spawns (those bring the cave fauna, glow squids, everywhere). */
    private static Set<EntityType<?>> animals(MobSpawnSettings spawns) {
        MobSpawnSettings.Builder common = new MobSpawnSettings.Builder();
        BiomeDefaultFeatures.commonSpawns(common);
        MobSpawnSettings vanilla = common.build();
        Set<EntityType<?>> animals = new HashSet<>();
        for (MobCategory category : new MobCategory[]{MobCategory.CREATURE, MobCategory.WATER_CREATURE, MobCategory.WATER_AMBIENT,
                MobCategory.UNDERGROUND_WATER_CREATURE, MobCategory.AXOLOTLS}) {
            spawns.getMobs(category).unwrap().forEach(data -> animals.add(data.type));
            vanilla.getMobs(category).unwrap().forEach(data -> animals.remove(data.type));
        }
        return animals;
    }

    /** Deep carvers everywhere; features only from {@link Ring4Features#ORDER} (so far only in verdant_falls). */
    @Test
    void deepCarversAndOnlyTheOrderedFeatures() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            assertEquals(List.copyOf(Ring2Carvers.COPIES.keySet()),
                    StreamSupport.stream(biome.getGenerationSettings().getCarvers(GenerationStep.Carving.AIR).spliterator(), false)
                            .map(holder -> holder.unwrapKey().orElseThrow()).toList(), entry.getKey().toString());
            List<HolderSet<PlacedFeature>> steps = biome.getGenerationSettings().features();
            for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
                List<ResourceKey<PlacedFeature>> placed = step.ordinal() < steps.size()
                        ? steps.get(step.ordinal()).stream().map(holder -> holder.unwrapKey().orElseThrow()).toList() : List.of();
                assertEquals(Ring4Features.features(entry.getKey(), step), placed, entry.getKey() + " " + step);
            }
        }
        // verdant_falls: the moss carpets and the climbing strands first; only its own features (golf 2: no 75–77
        // feature of the ground biomes in verdant_falls).
        assertEquals(List.of(VerdantFallsFeatures.VERDANT_FALLS_MOSS_CARPETS, VerdantFallsFeatures.VERDANT_FALLS_LEDGE_STRANDS),
                Ring4Features.features(ModBiomes.VERDANT_FALLS, GenerationStep.Decoration.VEGETAL_DECORATION).subList(0, 2));
        for (Ring4Features.Entry entry : Ring4Features.ORDER) {
            if (entry.biomes().contains(ModBiomes.VERDANT_FALLS)) {
                assertEquals(Set.of(ModBiomes.VERDANT_FALLS), entry.biomes(), entry.feature().toString());
            }
            assertFalse(entry.biomes().contains(ModBiomes.SUNDERED_REACH), entry.feature() + ": nothing in the zone 4→5");
        }
        // The glow under the islands (teststop 5a) in every ground biome, before what hangs from the islands.
        for (ResourceKey<Biome> ground : Ring4Biomes.GROUND) {
            List<ResourceKey<PlacedFeature>> vegetal = Ring4Features.features(ground, GenerationStep.Decoration.VEGETAL_DECORATION);
            assertEquals(List.of(Ring4MythicFeatures.ISLAND_GLOW_GROUND, Ring4MythicFeatures.ISLAND_UNDERSIDES), vegetal.subList(0, 2), ground.toString());
        }
        // Every feature of the order once.
        assertEquals(Ring4Features.ORDER.size(), Ring4Features.ORDER.stream().map(Ring4Features.Entry::feature).distinct().count());
    }

    /** The preset: the ground biomes in the ring 4 source, the transition 4→5 and the height biomes in the hook. */
    @Test
    void presetUsesTheRing4Biomes() {
        JsonObject source = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source");
        JsonObject transition = source.getAsJsonObject("transitions").getAsJsonObject("ring_4");
        assertEquals("minecraft:fixed", transition.get("type").getAsString());
        assertEquals(id(ModBiomes.SUNDERED_REACH), transition.get("biome").getAsString());
        JsonObject hook = source.getAsJsonObject("island_biomes");
        assertEquals(id(ModBiomes.SKY_GARDENS), hook.get("mythic").getAsString());
        assertEquals(id(ModBiomes.WITHERED_GARDENS), hook.get("blighted").getAsString());
        assertFalse(source.toString().contains("placeholder_ring_4"), "no ring 4 placeholder left in the overworld");
    }

    /** The generated tags (runData): {@code is_ring_4} with the transition, {@code ring_4_blighted} band + withered islands. */
    @Test
    void generatedTagsHoldTheRing4Lists() {
        assertEquals(Set.copyOf(ids(Ring4Biomes.WITH_TRANSITION)), tagValues("is_ring_4"));
        assertEquals(Set.copyOf(ids(Ring4Biomes.BLIGHTED_TAG)), tagValues("ring_4_blighted"));
        // Q2 = A and its follow-up: verdant_falls cools (heat refuge, with the dormant caldera) and keeps placed water.
        assertEquals(Set.of(id(ModBiomes.DORMANT_CALDERA), id(ModBiomes.VERDANT_FALLS)), tagValues("heat_refuges"));
        assertEquals(Set.of(id(ModBiomes.VERDANT_FALLS)), tagValues("evaporation_refuges"));
    }

    private static Set<String> tagValues(String tag) {
        // The project folder, found upwards from the working directory (like WorldgenData.root()).
        String relative = GENERATED_TAGS + tag + ".json";
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src/generated"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "no src/generated above the working directory");
        Path file = dir.resolve(relative);
        assertTrue(Files.isRegularFile(file), "missing " + file + ", run runData");
        return WorldgenData.read(file).getAsJsonArray("values").asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
    }

    /**
     * One ring 4 branch above the preliminary surface, after the ring 3 branch and before vanilla's rules (written by
     * {@code tools/worldgen/ring4_surface.py}); per biome an unconditional top block and an unconditional block for the
     * surface depth, so vanilla's rules never decide a ring 4 surface.
     */
    @Test
    void everyBiomeHasItsOwnSurfaceBranch() {
        JsonArray sequence = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        JsonArray rules = sequence.asList().stream().map(JsonElement::getAsJsonObject)
                .filter(rule -> rule.getAsJsonObject("if_true").get("type").getAsString().equals("minecraft:above_preliminary_surface"))
                .findFirst().orElseThrow().getAsJsonObject("then_run").getAsJsonArray("sequence");
        Set<String> ring4 = Set.copyOf(ids(Ring4Biomes.WITH_TRANSITION));
        Set<String> ring3 = Set.copyOf(ids(Ring3Biomes.WITH_TRANSITION));
        int ring3At = -1;
        int ring4At = -1;
        JsonObject branch = null;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject condition = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (!condition.get("type").getAsString().equals("minecraft:biome")) {
                continue;
            }
            Set<String> biomes = condition.getAsJsonArray("biome_is").asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
            if (biomes.equals(ring3)) {
                ring3At = i;
            }
            if (biomes.stream().anyMatch(ring4::contains)) {
                assertEquals(ring4, biomes, "one ring 4 branch for all ring 4 biomes");
                assertNull(branch, "a second ring 4 branch");
                ring4At = i;
                branch = rules.get(i).getAsJsonObject().getAsJsonObject("then_run");
            }
        }
        assertNotNull(branch, "ring 4 branch");
        assertEquals(ring3At + 1, ring4At, "right after the ring 3 branch, before vanilla's rules");
        Map<String, JsonObject> branches = new LinkedHashMap<>();
        for (JsonElement element : branch.getAsJsonArray("sequence")) {
            JsonObject rule = element.getAsJsonObject();
            JsonArray biomes = rule.getAsJsonObject("if_true").getAsJsonArray("biome_is");
            assertEquals(1, biomes.size());
            assertNull(branches.put(biomes.get(0).getAsString(), rule.getAsJsonObject("then_run")));
        }
        assertEquals(ring4, branches.keySet());
        for (Map.Entry<String, JsonObject> entry : branches.entrySet()) {
            boolean top = false;
            boolean layer = false;
            for (JsonElement element : entry.getValue().getAsJsonArray("sequence")) {
                JsonObject rule = element.getAsJsonObject();
                JsonObject condition = rule.getAsJsonObject("if_true");
                if (!condition.get("type").getAsString().equals("minecraft:stone_depth") || !condition.get("surface_type").getAsString().equals("floor")) {
                    continue;
                }
                boolean ends = endsInBlock(rule.getAsJsonObject("then_run"));
                if (condition.get("add_surface_depth").getAsBoolean()) {
                    layer |= ends;
                } else {
                    top |= ends;
                }
            }
            assertTrue(top, entry.getKey() + ": no unconditional top block");
            assertTrue(layer, entry.getKey() + ": no unconditional block for the surface depth");
            assertFallingBlocksAreGuarded(entry.getValue(), entry.getKey());
        }
        // Axolotls spawn only on clay: clay patches in the swamp's lake beds.
        JsonObject swampTop = floorRule(branches.get(id(ModBiomes.GLOWROOT_SWAMP)), false);
        assertTrue(swampTop.toString().contains("\"minecraft:clay\""), "clay under the swamp's water");
        // Rabbits spawn on grass: the flat fjord tops are grass, the cliffs moss and stone.
        JsonObject fjords = branches.get(id(ModBiomes.MIST_FJORDS));
        JsonObject flatTop = floorRule(fjords, false).getAsJsonArray("sequence").get(0).getAsJsonObject().getAsJsonObject("then_run");
        assertEquals("minecraft:grass_block", flatTop.getAsJsonObject("result_state").get("Name").getAsString());
        assertEquals("minecraft:steep", fjords.getAsJsonArray("sequence").get(0).getAsJsonObject().getAsJsonObject("if_true").get("type").getAsString());
        assertTrue(fjords.getAsJsonArray("sequence").get(0).toString().contains("\"minecraft:moss_block\""), "moss on the cliffs");
        // verdant_falls (Q7 = A): calcite on the rims and lips first, then stone on the exact walls (both from the terrain).
        JsonArray falls = branches.get(id(ModBiomes.VERDANT_FALLS)).getAsJsonArray("sequence");
        JsonObject rim = falls.get(0).getAsJsonObject();
        assertEquals("corruptedadventure:ring4_threshold", rim.getAsJsonObject("if_true").get("type").getAsString());
        assertEquals("corruptedadventure:ring_4/overworld/rim", rim.getAsJsonObject("if_true").get("density_function").getAsString());
        assertTrue(rim.toString().contains("\"minecraft:calcite\""), "calcite on the rims");
        JsonObject cliff = falls.get(1).getAsJsonObject();
        assertEquals("corruptedadventure:ring_4/overworld/cliff", cliff.getAsJsonObject("if_true").get("density_function").getAsString());
        assertTrue(cliff.toString().contains("\"minecraft:stone\""), "stone on the walls");
        // The wall's surface depth ends in stone (else a wall where the slope stays under the slope rules got the treads' dirt).
        JsonArray cliffRules = cliff.getAsJsonObject("then_run").getAsJsonArray("sequence");
        JsonArray face = cliffRules.get(cliffRules.size() - 1).getAsJsonObject().getAsJsonArray("sequence");
        JsonObject faceDepth = face.get(face.size() - 1).getAsJsonObject();
        assertEquals("minecraft:stone_depth", faceDepth.getAsJsonObject("if_true").get("type").getAsString());
        assertTrue(faceDepth.getAsJsonObject("if_true").get("add_surface_depth").getAsBoolean(), "the wall's surface depth");
        assertEquals("minecraft:stone", blockName(faceDepth.getAsJsonObject("then_run")), "stone through the wall's surface depth");
        assertTrue(floorRule(branches.get(id(ModBiomes.VERDANT_FALLS)), false).toString().contains("\"minecraft:moss_block\""), "moss on the treads");
    }

    /** The then-rule of a branch's own (not steep) floor rule: the top ({@code depth} false) or the surface depth. */
    private static JsonObject floorRule(JsonObject branch, boolean depth) {
        for (JsonElement element : branch.getAsJsonArray("sequence")) {
            JsonObject condition = element.getAsJsonObject().getAsJsonObject("if_true");
            if (condition.get("type").getAsString().equals("minecraft:stone_depth") && condition.get("surface_type").getAsString().equals("floor")
                    && condition.get("add_surface_depth").getAsBoolean() == depth) {
                return element.getAsJsonObject().getAsJsonObject("then_run");
            }
        }
        throw new AssertionError("no floor rule");
    }

    /**
     * Every sand or gravel block is the last rule of a sequence whose rule before it puts its own solid stand-in at a
     * ceiling (sand → sandstone, gravel → stone), so it never hangs over air.
     */
    private static void assertFallingBlocksAreGuarded(JsonObject branch, String biome) {
        Map<String, String> standIn = Map.of("minecraft:sand", "minecraft:sandstone", "minecraft:gravel", "minecraft:stone");
        int[] guarded = {0};
        walk(branch, rule -> {
            if (!rule.has("type") || !rule.get("type").getAsString().equals("minecraft:sequence")) {
                return;
            }
            JsonArray sequence = rule.getAsJsonArray("sequence");
            for (int i = 0; i < sequence.size(); i++) {
                String name = blockName(sequence.get(i).getAsJsonObject());
                if (name == null || !standIn.containsKey(name)) {
                    continue;
                }
                assertTrue(i > 0, biome + ": " + name + " without a guard");
                JsonObject guard = sequence.get(i - 1).getAsJsonObject();
                assertEquals("ceiling", guard.getAsJsonObject("if_true").get("surface_type").getAsString(), biome + ": " + name);
                assertEquals(standIn.get(name), blockName(guard.getAsJsonObject("then_run")), biome + ": " + name);
                guarded[0]++;
            }
        });
        String text = branch.toString();
        int falling = count(text, "\"minecraft:sand\"") + count(text, "\"minecraft:gravel\"");
        assertEquals(falling, guarded[0], biome + ": every sand or gravel block sits behind its guard");
    }

    private static void walk(JsonElement element, java.util.function.Consumer<JsonObject> visit) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> walk(e, visit));
        } else if (element.isJsonObject()) {
            visit.accept(element.getAsJsonObject());
            element.getAsJsonObject().entrySet().forEach(e -> walk(e.getValue(), visit));
        }
    }

    private static String blockName(JsonObject rule) {
        return rule.has("type") && rule.get("type").getAsString().equals("minecraft:block")
                ? rule.getAsJsonObject("result_state").get("Name").getAsString() : null;
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }

    private static boolean endsInBlock(JsonObject rule) {
        String type = rule.get("type").getAsString();
        if (type.equals("minecraft:block")) {
            return true;
        }
        if (!type.equals("minecraft:sequence")) {
            return false;
        }
        JsonArray sequence = rule.getAsJsonArray("sequence");
        return endsInBlock(sequence.get(sequence.size() - 1).getAsJsonObject());
    }

    private static String id(ResourceKey<Biome> key) {
        return key.location().toString();
    }

    private static List<String> ids(List<ResourceKey<Biome>> keys) {
        return keys.stream().map(Ring4BiomesTest::id).toList();
    }

    static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring4Biomes.bootstrap(new BootstrapContext<>() {
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
