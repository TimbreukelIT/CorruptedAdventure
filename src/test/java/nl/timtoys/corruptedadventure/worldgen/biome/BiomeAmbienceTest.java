package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void netherRing2PalettesAreGreyGreenAndDistinct() {
        assertEquals(0x56645A, BiomeAmbience.Palette.DRIPSHROOM_FOREST.fog());
        assertEquals(0x3E5A50, BiomeAmbience.Palette.SPORE_CAVES.fog());
        assertNotEquals(BiomeAmbience.Palette.DRIPSHROOM_FOREST, BiomeAmbience.Palette.SPORE_CAVES);
        assertNotEquals(BiomeAmbience.Palette.RING_1, BiomeAmbience.Palette.NETHER_RING_2);
        assertNotEquals(BiomeAmbience.Palette.RING_2, BiomeAmbience.Palette.NETHER_RING_2);
        assertEquals(0x000000, BiomeAmbience.Palette.NETHER_RING_2.sky());
    }

    @Test
    void netherRing2EffectsCarryFogSoundsAndParticles() {
        BiomeSpecialEffects forest = BiomeAmbience.dripshroomForestEffects().build();
        BiomeSpecialEffects caves = BiomeAmbience.sporeCavesEffects().build();
        assertEquals(0x56645A, forest.getFogColor());
        assertEquals(0x3E5A50, caves.getFogColor());
        for (BiomeSpecialEffects effects : List.of(forest, caves)) {
            assertSame(ModSoundEvents.NETHER_RING_2_DRIP_LOOP, effects.getAmbientLoopSoundEvent().orElseThrow());
            assertSame(ModSoundEvents.NETHER_RING_2_DRIPS, effects.getAmbientAdditionsSettings().orElseThrow().getSoundEvent());
            assertEquals(Optional.empty(), effects.getAmbientMoodSettings());
            assertEquals(Optional.empty(), effects.getBackgroundMusic());
        }
        assertEquals(0.01, forest.getAmbientAdditionsSettings().orElseThrow().getTickChance());
        assertEquals(0.02, caves.getAmbientAdditionsSettings().orElseThrow().getTickChance());
        assertEquals(ParticleTypes.SPORE_BLOSSOM_AIR, forest.getAmbientParticleSettings().orElseThrow().getOptions());
        assertEquals(ParticleTypes.GLOW, caves.getAmbientParticleSettings().orElseThrow().getOptions());
    }

    @Test
    void ring3HookGivesEveryRing3BiomeItsOwnColoursAndTheCaveMood() {
        List<ResourceKey<Biome>> biomes = new ArrayList<>(Ring3Biomes.ALL);
        biomes.add(ModBiomes.STEAM_FENS);
        Set<Integer> skies = new HashSet<>();
        for (ResourceKey<Biome> biome : biomes) {
            BiomeSpecialEffects effects = BiomeAmbience.ring3(biome).build();
            BiomeAmbience.Palette palette = Ring3Atmosphere.look(biome).colours();
            assertEquals(palette.sky(), effects.getSkyColor(), biome.toString());
            assertEquals(palette.fog(), effects.getFogColor(), biome.toString());
            assertEquals(palette.water(), effects.getWaterColor(), biome.toString());
            assertEquals(palette.waterFog(), effects.getWaterFogColor(), biome.toString());
            assertTrue(effects.getGrassColorOverride().isPresent(), biome.toString());
            assertTrue(effects.getFoliageColorOverride().isPresent(), biome.toString());
            assertEquals(Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), effects.getAmbientMoodSettings(), biome.toString());
            skies.add(palette.sky());
        }
        assertEquals(biomes.size(), skies.size(), "every sky differs");
        // Package 50-0 moved the colours of change 39b unchanged.
        assertEquals(0xB4553C, Ring3Atmosphere.look(ModBiomes.VOLCANIC_PEAKS).colours().sky());
        assertEquals(0xA4B49C, Ring3Atmosphere.look(ModBiomes.DORMANT_CALDERA).colours().fog());
        assertEquals(0x6FA040, BiomeAmbience.ring3(ModBiomes.DORMANT_CALDERA).build().getGrassColorOverride().orElseThrow());
        assertEquals(0x8A8060, BiomeAmbience.ring3(ModBiomes.ASHEN_WASTES).build().getGrassColorOverride().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.ring3(ModBiomes.CANYONLANDS));
    }

    @Test
    void netherRing3HookIsBlueWhiteAndDistinctPerBiome() {
        List<ResourceKey<Biome>> biomes = List.of(ModBiomes.FROZEN_LAVA_SEA, ModBiomes.RIME_FOREST, ModBiomes.GLACIER_RIFTS);
        Set<Integer> fogs = new HashSet<>();
        for (ResourceKey<Biome> biome : biomes) {
            BiomeSpecialEffects effects = BiomeAmbience.netherRing3(biome).build();
            assertEquals(0x000000, effects.getSkyColor(), biome.toString());
            int fog = effects.getFogColor();
            assertTrue((fog & 0xFF) > (fog >> 16 & 0xFF), biome + " fog is bluer than red");
            assertEquals(Optional.of(AmbientMoodSettings.LEGACY_CAVE_SETTINGS), effects.getAmbientMoodSettings(), biome.toString());
            assertEquals(Optional.empty(), effects.getBackgroundMusic(), biome.toString());
            fogs.add(fog);
        }
        assertEquals(biomes.size(), fogs.size(), "every fog differs");
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.netherRing3(ModBiomes.DRIPSHROOM_FOREST));
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
    void darkenMixesTowardsBlack() {
        for (int rgb : new int[]{0x000000, 0xFFFFFF, 0x123456, 0xE4D2A6}) {
            assertEquals(rgb, BiomeAmbience.darken(rgb, 0.0F));
            assertEquals(0x000000, BiomeAmbience.darken(rgb, 1.0F));
        }
        assertEquals(0x808080, BiomeAmbience.darken(0xFFFFFF, 0.5F));
        assertEquals(0xCCCCCC, BiomeAmbience.darken(0xFFFFFF, 0.2F));
    }

    @Test
    void paletteWithersChangeOnlyTheirOwnField() {
        BiomeAmbience.Palette base = new BiomeAmbience.Palette(1, 2, 3, 4);
        assertEquals(new BiomeAmbience.Palette(9, 2, 3, 4), base.withSky(9));
        assertEquals(new BiomeAmbience.Palette(1, 9, 3, 4), base.withFog(9));
        assertEquals(new BiomeAmbience.Palette(1, 2, 8, 9), base.withWater(8, 9));
    }

    @Test
    void ring2PalettesDifferFromEachOtherAndFromRing1() {
        List<BiomeAmbience.Palette> palettes = List.of(BiomeAmbience.Palette.RING_2, BiomeAmbience.Palette.CANYONLANDS,
                BiomeAmbience.Palette.OASIS_FLATS, BiomeAmbience.Palette.SALT_FLATS, BiomeAmbience.Palette.HOODOO_FIELDS,
                BiomeAmbience.Palette.DUNE_SEA, BiomeAmbience.Palette.DRY_RIVERBEDS, BiomeAmbience.Palette.RING_1);
        assertEquals(palettes.size(), palettes.stream().distinct().count());
        assertEquals(new BiomeAmbience.Palette(0xAEC8E0, 0xE4D2A6, 0x32A598, 0x0B3A35), BiomeAmbience.Palette.RING_2);
        assertEquals(new BiomeAmbience.Palette(0xAEC8E0, 0xE4D2A6, 0x32A598, 0x0B3A35).desaturated(0.35F).darkened(0.1F),
                BiomeAmbience.Palette.DRY_RIVERBEDS);
        assertEquals(0x5AC8B8, BiomeAmbience.Palette.SALT_FLATS.water());
        assertEquals(0x1B5048, BiomeAmbience.Palette.SALT_FLATS.waterFog());
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
