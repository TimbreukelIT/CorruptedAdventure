package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Biomes;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeAmbienceTest {
    @Test
    void withMoodChangesOnlyTheMood() {
        Map<ResourceKey<Biome>, Biome> vanilla = bootstrap(BiomeData::bootstrap);
        assertTrue(vanilla.size() > 60);
        vanilla.forEach((key, biome) -> {
            Biome silent = BiomeAmbience.withMood(biome, BiomeAmbience.SILENT);
            assertEquals(Optional.empty(), silent.getAmbientMood(), key.toString());
            assertSame(biome.getGenerationSettings(), silent.getGenerationSettings(), key.toString());
            assertSame(biome.getMobSettings(), silent.getMobSettings(), key.toString());
            assertEquals(biome.getModifiedClimateSettings(), silent.getModifiedClimateSettings(), key.toString());
            assertEquals(effectsWithoutMood(biome), effectsWithoutMood(silent), key.toString());
        });
    }

    @Test
    void withMoodSetsTheGivenMood() {
        Biome plains = bootstrap(BiomeData::bootstrap).get(Biomes.PLAINS);
        assertEquals(BiomeAmbience.STRANGE, BiomeAmbience.withMood(plains, BiomeAmbience.STRANGE).getAmbientMood());
    }

    @Test
    void ring1PaletteIsVanillaPlains() {
        BiomeSpecialEffects plains = bootstrap(BiomeData::bootstrap).get(Biomes.PLAINS).getSpecialEffects();
        assertEquals(new BiomeAmbience.Palette(plains.getSkyColor(), plains.getFogColor(), plains.getWaterColor(), plains.getWaterFogColor()),
                BiomeAmbience.Palette.RING_1);
    }

    @Test
    void desaturateMixesTowardsGrey() {
        for (int rgb : new int[]{0x78A7FF, 0x3F76E4, 0x050533, 0xFF0000}) {
            assertEquals(rgb, BiomeAmbience.desaturate(rgb, 0.0F));
            int grey = BiomeAmbience.desaturate(rgb, 1.0F);
            assertEquals(grey >> 16 & 0xFF, grey >> 8 & 0xFF);
            assertEquals(grey >> 8 & 0xFF, grey & 0xFF);
        }
        assertEquals(0x4C4C4C, BiomeAmbience.desaturate(0xFF0000, 1.0F), "luma of pure red is 0.299 · 255");
        assertEquals(0xA62626, BiomeAmbience.desaturate(0xFF0000, 0.5F));
        assertEquals(0x808080, BiomeAmbience.desaturate(0x808080, 0.7F), "grey stays grey");
    }

    @Test
    void witheredExpanseIsStrangeAndFaded() {
        Biome expanse = bootstrap(TransitionBiomes::bootstrap).get(ModBiomes.WITHERED_EXPANSE);
        assertEquals(BiomeAmbience.STRANGE, expanse.getAmbientMood());
        BiomeAmbience.Palette ring1 = BiomeAmbience.Palette.RING_1;
        assertNotEquals(ring1.sky(), expanse.getSkyColor());
        assertEquals(ring1.desaturated(TransitionBiomes.WITHERED_EXPANSE_FADE).sky(), expanse.getSkyColor());
        assertEquals(ring1.desaturated(TransitionBiomes.WITHERED_EXPANSE_FADE).waterFog(), expanse.getWaterFogColor());
    }

    @Test
    void copiesInMoodsAreSilentAndTheRestKeepsVanillasCaveSound() {
        Map<ResourceKey<Biome>, Biome> copies = bootstrap(VanillaBiomeCopies::bootstrap);
        for (ResourceKey<Biome> vanilla : VanillaBiomeCopies.OVERWORLD_COPIES) {
            Optional<AmbientMoodSettings> expected = VanillaBiomeCopies.MOODS.getOrDefault(vanilla, Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS));
            assertEquals(expected, copies.get(VanillaBiomeCopies.copyOf(vanilla)).getAmbientMood(), vanilla.toString());
        }
        assertEquals(BiomeAmbience.SILENT, VanillaBiomeCopies.MOODS.get(Biomes.DRIPSTONE_CAVES));
        assertTrue(copies.get(VanillaBiomeCopies.copyOf(Biomes.DEEP_DARK)).getAmbientMood().isPresent());
        assertTrue(copies.get(VanillaBiomeCopies.copyOf(Biomes.LUSH_CAVES)).getAmbientMood().isPresent());
    }

    private static JsonObject effectsWithoutMood(Biome biome) {
        JsonObject json = BiomeSpecialEffects.CODEC.encodeStart(JsonOps.INSTANCE, biome.getSpecialEffects()).getOrThrow().getAsJsonObject();
        json.remove("mood_sound");
        return json;
    }

    /** Runs a biome bootstrap against stand-alone lookups and returns what it registered. */
    private static Map<ResourceKey<Biome>, Biome> bootstrap(Consumer<BootstrapContext<Biome>> bootstrap) {
        Map<ResourceKey<Biome>, Biome> registered = new LinkedHashMap<>();
        StandaloneHolderGetter<Biome> biomes = new StandaloneHolderGetter<>(key -> true);
        bootstrap.accept(new BootstrapContext<>() {
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
