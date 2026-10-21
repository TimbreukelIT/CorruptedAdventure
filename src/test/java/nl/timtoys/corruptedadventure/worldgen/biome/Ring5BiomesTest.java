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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The ring 5 biomes (package 86): bootstrap, climate, spawns, generation, preset, tags and the surface branch. */
class Ring5BiomesTest {
    private static final String GENERATED = "src/generated/resources/";
    private static final String TAGS = GENERATED + "data/corruptedadventure/tags/worldgen/biome/";

    @Test
    void bootstrapRegistersExactlyTheRing5Lists() {
        assertEquals(Ring5Biomes.ALL, List.copyOf(bootstrap().keySet()));
        assertEquals(List.of(ModBiomes.CORRUPTED_WASTES, ModBiomes.GLITCH_FIELDS, ModBiomes.FALSE_MEADOW), Ring5Biomes.INNER);
        assertEquals(List.of(ModBiomes.VOID_SCARS, ModBiomes.INVERTED_HILLS, ModBiomes.SHATTERED_EXPANSE), Ring5Biomes.OUTER);
        assertEquals(6, Set.copyOf(Ring5Biomes.ALL).size());
        assertEquals(List.of(ModBiomes.FALSE_MEADOW), Ring5Biomes.SILENT);
        assertFalse(Ring5Biomes.ALL.contains(ModBiomes.SUNDERED_REACH), "the zone 4→5 stays ring 4's");
        assertThrows(IllegalArgumentException.class, () -> Ring5Biomes.look(ModBiomes.SUNDERED_REACH));
        assertThrows(IllegalArgumentException.class, () -> Ring5Biomes.spawns(ModBiomes.PLACEHOLDER_RING_5));
    }

    /** Never rain or snow (the ring weather renders its own); colours of their own per biome, false_meadow vanilla plains. */
    @Test
    void dryAndEachBiomeItsOwnColours() {
        Set<List<Integer>> looks = new HashSet<>();
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            String name = entry.getKey().toString();
            assertFalse(biome.hasPrecipitation(), name);
            assertEquals(0.5F, biome.getBaseTemperature(), name);
            assertEquals(0.0F, biome.getModifiedClimateSettings().downfall(), name);
            Ring5Biomes.Look look = Ring5Biomes.look(entry.getKey());
            assertEquals(look.colours().sky(), biome.getSkyColor(), name);
            assertEquals(look.colours().fog(), biome.getFogColor(), name);
            assertEquals(look.colours().water(), biome.getWaterColor(), name);
            assertEquals(look.colours().waterFog(), biome.getWaterFogColor(), name);
            assertEquals(Optional.of(look.grass()), biome.getSpecialEffects().getGrassColorOverride(), name);
            assertEquals(Optional.of(look.foliage()), biome.getSpecialEffects().getFoliageColorOverride(), name);
            assertTrue(looks.add(List.of(biome.getSkyColor(), biome.getFogColor(), biome.getWaterColor(), look.grass())), name + ": colours of its own");
            // No sounds, particles or music yet: the atmosphere's (A, 98).
            assertTrue(biome.getSpecialEffects().getAmbientLoopSoundEvent().isEmpty(), name);
            assertTrue(biome.getSpecialEffects().getAmbientParticleSettings().isEmpty(), name);
        }
        Biome meadow = bootstrap().get(ModBiomes.FALSE_MEADOW);
        assertEquals(BiomeAmbience.Palette.RING_1.sky(), meadow.getSkyColor());
        assertEquals(Optional.of(0x91BD59), meadow.getSpecialEffects().getGrassColorOverride(), "plains' grass");
        assertEquals(Optional.of(0x77AB2F), meadow.getSpecialEffects().getFoliageColorOverride(), "plains' foliage");
    }

    /** Contract §2: vanilla's monsters only (no bats, no animals); false_meadow nothing at all. */
    @Test
    void monstersOnlyAndNothingInTheFalseMeadow() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            MobSpawnSettings spawns = entry.getValue().getMobSettings();
            for (MobCategory category : MobCategory.values()) {
                List<MobSpawnSettings.SpawnerData> mobs = spawns.getMobs(category).unwrap();
                if (category == MobCategory.MONSTER && !entry.getKey().equals(ModBiomes.FALSE_MEADOW)) {
                    assertTrue(mobs.stream().anyMatch(data -> data.type == EntityType.ZOMBIE), entry.getKey().toString());
                    assertTrue(mobs.stream().anyMatch(data -> data.type == EntityType.ENDERMAN), entry.getKey().toString());
                } else {
                    assertEquals(List.of(), mobs, entry.getKey() + " " + category);
                }
            }
        }
    }

    /** Nothing generates in golf 1: no carvers (93), no features (94/95). */
    @Test
    void noCarversAndNoFeatures() {
        for (Map.Entry<ResourceKey<Biome>, Biome> entry : bootstrap().entrySet()) {
            Biome biome = entry.getValue();
            for (GenerationStep.Carving step : GenerationStep.Carving.values()) {
                assertEquals(0, StreamSupport.stream(biome.getGenerationSettings().getCarvers(step).spliterator(), false).count(), entry.getKey().toString());
            }
            assertTrue(biome.getGenerationSettings().features().stream().allMatch(set -> set.size() == 0), entry.getKey().toString());
        }
    }

    /** The preset: ring 5's own source; the zone 4→5 keeps sundered_reach and the 5→void boundary stays hard. */
    @Test
    void presetUsesTheRing5Biomes() {
        JsonObject source = WorldgenData.read("world_preset/corrupted_world.json").getAsJsonObject("dimensions")
                .getAsJsonObject("minecraft:overworld").getAsJsonObject("generator").getAsJsonObject("biome_source");
        assertEquals("minecraft:multi_noise", source.getAsJsonObject("rings").getAsJsonObject("ring_5").get("type").getAsString());
        JsonObject transition = source.getAsJsonObject("transitions").getAsJsonObject("ring_4");
        assertEquals("minecraft:fixed", transition.get("type").getAsString());
        assertEquals(id(ModBiomes.SUNDERED_REACH), transition.get("biome").getAsString());
        assertFalse(source.getAsJsonObject("transitions").has("ring_5"), "no transition biome of ring 5's own");
        assertFalse(source.toString().contains("placeholder_ring_5"), "no ring 5 placeholder left in the overworld");
    }

    /** The generated tags (runData). */
    @Test
    void generatedTagsHoldTheRing5Lists() {
        assertEquals(Set.copyOf(ids(Ring5Biomes.ALL)), tagValues(TAGS + "is_ring_5.json"));
        assertEquals(Set.copyOf(ids(Ring5Biomes.INNER)), tagValues(TAGS + "ring_5_inner.json"));
        assertEquals(Set.copyOf(ids(Ring5Biomes.OUTER)), tagValues(TAGS + "ring_5_outer.json"));
        assertTrue(tagValues(TAGS + "silent.json").contains(id(ModBiomes.FALSE_MEADOW)));
        assertFalse(tagValues(TAGS + "silent.json").stream().anyMatch(biome -> ids(Ring5Biomes.ALL).contains(biome) && !biome.equals(id(ModBiomes.FALSE_MEADOW))));
        assertFalse(tagValues(TAGS + "is_ring_4.json").stream().anyMatch(ids(Ring5Biomes.ALL)::contains), "ring 5 is not ring 4");
        String c = GENERATED + "data/c/tags/worldgen/biome/";
        assertTrue(tagValues(c + "is_dry/overworld.json").containsAll(ids(Ring5Biomes.ALL)));
        assertTrue(tagValues(c + "is_void.json").contains(id(ModBiomes.SHATTERED_EXPANSE)));
        assertTrue(tagValues(c + "is_plains.json").contains(id(ModBiomes.FALSE_MEADOW)));
        assertTrue(tagValues(c + "no_default_monsters.json").contains(id(ModBiomes.FALSE_MEADOW)));
        // No minecraft: tags: no vanilla structure (strongholds included) and no vanilla behaviour.
        Path minecraft = root().resolve(GENERATED + "data/minecraft/tags/worldgen/biome");
        if (Files.isDirectory(minecraft)) {
            try (var files = Files.walk(minecraft)) {
                files.filter(Files::isRegularFile).forEach(file -> {
                    String text = WorldgenData.read(file).toString();
                    for (String biome : ids(Ring5Biomes.ALL)) {
                        assertFalse(text.contains("\"" + biome + "\""), file + " holds " + biome);
                    }
                });
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Test
    void generatedLangNamesEveryBiome() {
        for (String lang : new String[]{"en_us", "nl_nl"}) {
            JsonObject names = WorldgenData.read(root().resolve(GENERATED + "assets/corruptedadventure/lang/" + lang + ".json"));
            for (ResourceKey<Biome> biome : Ring5Biomes.ALL) {
                assertTrue(names.has(biome.location().toLanguageKey("biome")), lang + " " + biome);
            }
        }
    }

    /**
     * One ring 5 branch above the preliminary surface, right after the ring 4 branch and before vanilla's rules (written
     * by {@code tools/worldgen/ring5_surface.py}); per biome an unconditional top block and an unconditional block for
     * the surface depth, so vanilla's rules never decide a ring 5 surface; no falling block (nothing guards one yet).
     */
    @Test
    void everyBiomeHasItsOwnSurfaceBranch() {
        JsonArray sequence = WorldgenData.read("noise_settings/overworld.json").getAsJsonObject("surface_rule").getAsJsonArray("sequence");
        JsonArray rules = sequence.asList().stream().map(JsonElement::getAsJsonObject)
                .filter(rule -> rule.getAsJsonObject("if_true").get("type").getAsString().equals("minecraft:above_preliminary_surface"))
                .findFirst().orElseThrow().getAsJsonObject("then_run").getAsJsonArray("sequence");
        Set<String> ring5 = Set.copyOf(ids(Ring5Biomes.ALL));
        Set<String> ring4 = Set.copyOf(ids(Ring4Biomes.WITH_TRANSITION));
        int ring4At = -1;
        int ring5At = -1;
        JsonObject branch = null;
        for (int i = 0; i < rules.size(); i++) {
            JsonObject condition = rules.get(i).getAsJsonObject().getAsJsonObject("if_true");
            if (!condition.get("type").getAsString().equals("minecraft:biome")) {
                continue;
            }
            Set<String> biomes = condition.getAsJsonArray("biome_is").asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
            if (biomes.equals(ring4)) {
                ring4At = i;
            }
            if (biomes.stream().anyMatch(ring5::contains)) {
                assertEquals(ring5, biomes, "one ring 5 branch for all ring 5 biomes");
                assertNull(branch, "a second ring 5 branch");
                ring5At = i;
                branch = rules.get(i).getAsJsonObject().getAsJsonObject("then_run");
            }
        }
        assertNotNull(branch, "ring 5 branch");
        assertEquals(ring4At + 1, ring5At, "right after the ring 4 branch, before vanilla's rules");
        Map<String, JsonObject> branches = new LinkedHashMap<>();
        for (JsonElement element : branch.getAsJsonArray("sequence")) {
            JsonArray biomes = element.getAsJsonObject().getAsJsonObject("if_true").getAsJsonArray("biome_is");
            assertEquals(1, biomes.size());
            assertNull(branches.put(biomes.get(0).getAsString(), element.getAsJsonObject().getAsJsonObject("then_run")));
        }
        assertEquals(ids(Ring5Biomes.ALL), List.copyOf(branches.keySet()));
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
            String text = entry.getValue().toString();
            for (String falling : new String[]{"minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "corruptedadventure:corrupted_sand"}) {
                assertFalse(text.contains("\"" + falling + "\""), entry.getKey() + ": " + falling + " without a ceiling guard");
            }
        }
        // false_meadow is the perfect vanilla meadow: grass on dirt (also after 86b).
        assertEquals("minecraft:grass_block", lastBlock(floorRule(branches.get(id(ModBiomes.FALSE_MEADOW)), false)));
        assertEquals("minecraft:dirt", lastBlock(floorRule(branches.get(id(ModBiomes.FALSE_MEADOW)), true)));
        // The scar walls deepslate, the inverted hills' undersides stone, the wastes' pit floors tuff.
        assertTrue(branches.get(id(ModBiomes.VOID_SCARS)).toString().contains("\"minecraft:deepslate\""));
        JsonObject underside = branches.get(id(ModBiomes.INVERTED_HILLS)).getAsJsonArray("sequence").get(0).getAsJsonObject();
        assertEquals("ceiling", underside.getAsJsonObject("if_true").get("surface_type").getAsString());
        assertEquals("minecraft:stone", lastBlock(underside.getAsJsonObject("then_run")));
        assertTrue(floorRule(branches.get(id(ModBiomes.CORRUPTED_WASTES)), false).toString().contains("\"minecraft:tuff\""));
    }

    /** The then-rule of a branch's floor rule: the top ({@code depth} false) or the surface depth. */
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

    /** The block a rule ends in: itself, or the last rule of its sequence. */
    private static String lastBlock(JsonObject rule) {
        if (rule.get("type").getAsString().equals("minecraft:sequence")) {
            JsonArray sequence = rule.getAsJsonArray("sequence");
            return lastBlock(sequence.get(sequence.size() - 1).getAsJsonObject());
        }
        return rule.getAsJsonObject("result_state").get("Name").getAsString();
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

    private static Set<String> tagValues(String relative) {
        Path file = root().resolve(relative);
        assertTrue(Files.isRegularFile(file), "missing " + file + ", run runData");
        return WorldgenData.read(file).getAsJsonArray("values").asList().stream().map(JsonElement::getAsString).collect(Collectors.toSet());
    }

    /** The project folder, found upwards from the working directory (like WorldgenData.root()). */
    private static Path root() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.isDirectory(dir.resolve("src/generated"))) {
            dir = dir.getParent();
        }
        assertNotNull(dir, "no src/generated above the working directory");
        return dir;
    }

    private static String id(ResourceKey<Biome> key) {
        return key.location().toString();
    }

    private static List<String> ids(List<ResourceKey<Biome>> keys) {
        return keys.stream().map(Ring5BiomesTest::id).toList();
    }

    static Map<ResourceKey<Biome>, Biome> bootstrap() {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        Ring5Biomes.bootstrap(new BootstrapContext<>() {
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
