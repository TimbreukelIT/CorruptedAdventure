package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The committed biome JSON in {@code src/generated} carries exactly the effects of the atmosphere hooks: colours,
 * ambient particle, loop, additions and mood, for every ring 3 biome (with the transition {@code steam_fens}) and every
 * Nether ring 3 biome. Catches generated output that is older than the hook (integration 2 had to regenerate both).
 */
class Ring3GeneratedEffectsTest {
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure/worldgen/biome/";

    @Test
    void ring3BiomeJsonMatchesTheRing3Hook() {
        for (ResourceKey<Biome> key : Ring3Biomes.WITH_TRANSITION) {
            assertMatches(key, BiomeAmbience::ring3);
        }
    }

    @Test
    void netherRing3BiomeJsonMatchesTheNetherRing3Hook() {
        for (ResourceKey<Biome> key : NetherRing3Biomes.ALL) {
            assertMatches(key, BiomeAmbience::netherRing3);
        }
    }

    private static void assertMatches(ResourceKey<Biome> key, Function<ResourceKey<Biome>, BiomeSpecialEffects.Builder> hook) {
        // Encoded the way datagen writes it (registry ids for sounds), then through text so numbers compare as written.
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        JsonElement expected = JsonParser.parseString(
                BiomeSpecialEffects.CODEC.encodeStart(ops, hook.apply(key).build()).getOrThrow().toString());
        JsonObject actual = generated(key).getAsJsonObject("effects").deepCopy();
        // The music is the biome bootstrap's own, not the hook's.
        actual.remove("music");
        assertTrue(actual.has("mood_sound"), key.location() + ": no mood in the generated JSON");
        assertEquals(expected, actual, key.location() + ": src/generated is older than the atmosphere hook, run runData");
    }

    private static JsonObject generated(ResourceKey<Biome> key) {
        // root() is <project>/src/main/resources/data/corruptedadventure/worldgen
        Path project = WorldgenData.root();
        for (int i = 0; i < 6; i++) {
            project = project.getParent();
        }
        Path file = project.resolve(GENERATED + key.location().getPath() + ".json");
        assertTrue(Files.isRegularFile(file), "missing " + file);
        return WorldgenData.read(file);
    }
}
