package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.particles.ParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.biome.BiomeData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        // The sky still turns at the boundary: no ring 3 sky is that of the faded ring 2 transition.
        assertFalse(skies.contains(BiomeAmbience.Palette.DRY_RIVERBEDS.sky()));
        assertEquals(0x6FA040, BiomeAmbience.ring3(ModBiomes.DORMANT_CALDERA).build().getGrassColorOverride().orElseThrow());
        assertEquals(0x8A8060, BiomeAmbience.ring3(ModBiomes.ASHEN_WASTES).build().getGrassColorOverride().orElseThrow());
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.ring3(ModBiomes.CANYONLANDS));
    }

    /** G2-A3 = A: subtle to medium, densest in the ember woods and the steam fens; every biome has its own particle. */
    @Test
    void ring3ParticlesAreSubtleAndDensestInTheEmberWoodsAndSteamFens() {
        List<ResourceKey<Biome>> biomes = new ArrayList<>(Ring3Biomes.ALL);
        biomes.add(ModBiomes.STEAM_FENS);
        float emberWoods = probability(ModBiomes.EMBER_WOODS);
        float steamFens = probability(ModBiomes.STEAM_FENS);
        for (ResourceKey<Biome> biome : biomes) {
            float probability = probability(biome);
            assertTrue(probability > 0.0F && probability <= 0.01F, biome + " " + probability);
            if (!biome.equals(ModBiomes.EMBER_WOODS) && !biome.equals(ModBiomes.STEAM_FENS)) {
                assertTrue(probability < Math.min(emberWoods, steamFens), biome.toString());
            }
        }
        assertSame(ModParticleTypes.RISING_EMBER.get(), particle(ModBiomes.EMBER_WOODS).getOptions());
        assertSame(ModParticleTypes.DRIFTING_ASH.get(), particle(ModBiomes.ASHEN_WASTES).getOptions());
        assertSame(ModParticleTypes.SULFUR_STEAM.get(), particle(ModBiomes.SULFUR_FIELDS).getOptions());
        assertSame(ModParticleTypes.STEAM_PUFF.get(), particle(ModBiomes.STEAM_FENS).getOptions());
        assertSame(ParticleTypes.SOUL, particle(ModBiomes.TITAN_FIELDS).getOptions());
    }

    @Test
    void ring3SoundsLoopWhereTheBiomeHasAVoiceAndLavaBubblesNearLava() {
        assertSame(ModSoundEvents.RING_3_EMBERS, BiomeAmbience.ring3(ModBiomes.EMBER_WOODS).build().getAmbientLoopSoundEvent().orElseThrow());
        assertSame(ModSoundEvents.RING_3_TITAN_HUM, BiomeAmbience.ring3(ModBiomes.TITAN_FIELDS).build().getAmbientLoopSoundEvent().orElseThrow());
        assertSame(ModSoundEvents.RING_3_CALDERA, BiomeAmbience.ring3(ModBiomes.DORMANT_CALDERA).build().getAmbientLoopSoundEvent().orElseThrow());
        for (ResourceKey<Biome> lava : List.of(ModBiomes.VOLCANIC_PEAKS, ModBiomes.BASALT_FLOWS)) {
            AmbientAdditionsSettings additions = BiomeAmbience.ring3(lava).build().getAmbientAdditionsSettings().orElseThrow();
            assertSame(ModSoundEvents.RING_3_LAVA, additions.getSoundEvent());
            assertTrue(additions.getTickChance() > 0.0 && additions.getTickChance() < 0.0111, "rarer than the basalt deltas'");
        }
        assertEquals(Optional.empty(), BiomeAmbience.ring3(ModBiomes.ASHEN_WASTES).build().getAmbientLoopSoundEvent());
    }

    private static AmbientParticleSettings particle(ResourceKey<Biome> biome) {
        return BiomeAmbience.ring3(biome).build().getAmbientParticleSettings().orElseThrow(() -> new AssertionError(biome.toString()));
    }

    private static float probability(ResourceKey<Biome> biome) {
        return probability(particle(biome));
    }

    /** The settings have no getter for it: read it from their own codec, as it goes into the biome JSON. */
    private static float probability(AmbientParticleSettings settings) {
        return AmbientParticleSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow()
                .getAsJsonObject().get("probability").getAsFloat();
    }

    /**
     * verdant-falls.md §4 and Q8 = A: plan colours (sky and fog deepened by the ring 4 sky sparring, 2026-10-20), light
     * spray, waterfall loop, birds and frogs, cave mood.
     */
    @Test
    void ring4HookGivesVerdantFallsItsColoursSprayAndSounds() {
        BiomeSpecialEffects effects = BiomeAmbience.ring4(ModBiomes.VERDANT_FALLS).build();
        assertEquals(0x64CCE2, effects.getSkyColor());
        assertEquals(0xBCEAD8, effects.getFogColor());
        assertEquals(0x3FD0C0, effects.getWaterColor());
        assertEquals(0x0F4F4A, effects.getWaterFogColor());
        assertEquals(0x6CC24A, effects.getGrassColorOverride().orElseThrow());
        assertEquals(0x55B03A, effects.getFoliageColorOverride().orElseThrow());
        AmbientParticleSettings particle = effects.getAmbientParticleSettings().orElseThrow();
        assertSame(ModParticleTypes.WATERFALL_MIST.get(), particle.getOptions());
        float probability = probability(particle);
        assertTrue(probability > 0.0F && probability < probability(ModBiomes.STEAM_FENS), "a light haze, below the steam fens");
        assertSame(ModSoundEvents.VERDANT_FALLS_FALLS, effects.getAmbientLoopSoundEvent().orElseThrow());
        AmbientAdditionsSettings additions = effects.getAmbientAdditionsSettings().orElseThrow();
        assertSame(ModSoundEvents.VERDANT_FALLS_WILDLIFE, additions.getSoundEvent());
        assertTrue(additions.getTickChance() > 0.0 && additions.getTickChance() < 0.0111, "rarer than the basalt deltas'");
        assertSame(AmbientMoodSettings.LEGACY_CAVE_SETTINGS, effects.getAmbientMoodSettings().orElseThrow());
    }

    /**
     * Change 80: every ring 4 biome has its own colours from the hook, the cave mood, grass and foliage; water laps in
     * the mythical band, the blighted band is quieter (no loop), the zone 4→5 drones; and nothing else is ring 4.
     */
    @Test
    void ring4HookGivesEveryBiomeItsOwnLookAndRejectsOtherBiomes() {
        Set<Integer> skies = new HashSet<>();
        Set<Integer> waters = new HashSet<>();
        for (ResourceKey<Biome> biome : Ring4Biomes.WITH_TRANSITION) {
            BiomeSpecialEffects effects = BiomeAmbience.ring4(biome).build();
            skies.add(effects.getSkyColor());
            waters.add(effects.getWaterColor());
            assertTrue(effects.getGrassColorOverride().isPresent(), biome.toString());
            assertTrue(effects.getFoliageColorOverride().isPresent(), biome.toString());
            assertSame(AmbientMoodSettings.LEGACY_CAVE_SETTINGS, effects.getAmbientMoodSettings().orElseThrow(), biome.toString());
            if (Ring4Biomes.MYTHIC.contains(biome) || biome.equals(ModBiomes.SKY_GARDENS)) {
                assertSame(ModSoundEvents.RING_4_WATER, effects.getAmbientLoopSoundEvent().orElseThrow(), biome.toString());
            }
            if (Ring4Biomes.BLIGHTED_TAG.contains(biome)) {
                assertEquals(Optional.empty(), effects.getAmbientLoopSoundEvent(), biome + ": the blighted band is quieter");
            }
        }
        assertEquals(Ring4Biomes.WITH_TRANSITION.size(), skies.size(), "every sky differs");
        assertEquals(Ring4Biomes.WITH_TRANSITION.size(), waters.size(), "every water differs");
        assertSame(ModSoundEvents.RING_4_SUNDERED_DRONE,
                BiomeAmbience.ring4(ModBiomes.SUNDERED_REACH).build().getAmbientLoopSoundEvent().orElseThrow());
        assertSame(ModParticleTypes.WILL_O_WISP.get(),
                BiomeAmbience.ring4(ModBiomes.PEAT_MOORS).build().getAmbientParticleSettings().orElseThrow().getOptions());
        assertSame(ModParticleTypes.BLOSSOM.get(),
                BiomeAmbience.ring4(ModBiomes.CASCADE_VALE).build().getAmbientParticleSettings().orElseThrow().getOptions());
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.ring4(ModBiomes.CANYONLANDS));
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.ring4(ModBiomes.STEAM_FENS));
    }

    /** Change 80: near white-beige fog per biome, bone dust, the dry wind and the crystals' hum, the cave mood. */
    @Test
    void netherRing4HookIsWhiteBeigeAndDistinctPerBiome() {
        Set<Integer> fogs = new HashSet<>();
        for (ResourceKey<Biome> biome : NetherRing4Biomes.ALL) {
            BiomeSpecialEffects effects = BiomeAmbience.netherRing4(biome).build();
            int fog = effects.getFogColor();
            assertTrue(Math.min(fog >> 16 & 0xFF, Math.min(fog >> 8 & 0xFF, fog & 0xFF)) >= 0xA0, biome + ": near white");
            fogs.add(fog);
            assertSame(ModParticleTypes.BONE_DUST.get(), effects.getAmbientParticleSettings().orElseThrow().getOptions());
            assertSame(ModSoundEvents.NETHER_RING_4_WIND, effects.getAmbientLoopSoundEvent().orElseThrow());
            assertSame(ModSoundEvents.NETHER_RING_4_CRYSTAL_HUM, effects.getAmbientAdditionsSettings().orElseThrow().getSoundEvent());
            assertSame(AmbientMoodSettings.LEGACY_CAVE_SETTINGS, effects.getAmbientMoodSettings().orElseThrow());
        }
        assertEquals(NetherRing4Biomes.ALL.size(), fogs.size());
        assertThrows(IllegalArgumentException.class, () -> BiomeAmbience.netherRing4(ModBiomes.FROZEN_LAVA_SEA));
    }

    /**
     * Ring 5 (98, V98-5 A): no music anywhere; the inner band near-silent with rare glitches (more and with pixels in the
     * glitch fields) and ring 1's cave sound; false_meadow nothing; the outer band the void drone and void motes. The
     * colours are the bootstrap's and pass through unchanged.
     */
    @Test
    void ring5HookAddsSilenceGlitchesAndTheDroneOnTopOfTheColours() {
        for (ResourceKey<Biome> biome : Ring5Biomes.ALL) {
            BiomeSpecialEffects effects = BiomeAmbience.ring5(biome, Ring5Biomes.effects(biome)).build();
            assertEquals(Optional.of(BiomeAmbience.SILENT_MUSIC), effects.getBackgroundMusic(), biome.toString());
            assertEquals(Ring5Biomes.look(biome).colours().fog(), effects.getFogColor(), "colours unchanged");
            boolean outer = Ring5Biomes.OUTER.contains(biome);
            assertEquals(outer, effects.getAmbientLoopSoundEvent().isPresent(), biome + ": the drone only outside");
            if (outer) {
                assertSame(ModSoundEvents.RING_5_VOID_DRONE.get(), effects.getAmbientLoopSoundEvent().orElseThrow().value());
                assertSame(ModParticleTypes.VOID_MOTE.get(), effects.getAmbientParticleSettings().orElseThrow().getOptions());
                assertTrue(effects.getAmbientMoodSettings().isEmpty(), "no cave sound outside");
            }
        }
        BiomeSpecialEffects meadow = BiomeAmbience.ring5(ModBiomes.FALSE_MEADOW, Ring5Biomes.effects(ModBiomes.FALSE_MEADOW)).build();
        assertTrue(meadow.getAmbientAdditionsSettings().isEmpty() && meadow.getAmbientMoodSettings().isEmpty()
                && meadow.getAmbientParticleSettings().isEmpty(), "false_meadow is silent");
        BiomeSpecialEffects wastes = BiomeAmbience.ring5(ModBiomes.CORRUPTED_WASTES, Ring5Biomes.effects(ModBiomes.CORRUPTED_WASTES)).build();
        BiomeSpecialEffects fields = BiomeAmbience.ring5(ModBiomes.GLITCH_FIELDS, Ring5Biomes.effects(ModBiomes.GLITCH_FIELDS)).build();
        assertTrue(fields.getAmbientAdditionsSettings().orElseThrow().getTickChance()
                > wastes.getAmbientAdditionsSettings().orElseThrow().getTickChance(), "more glitches in the glitch fields");
        assertSame(ModParticleTypes.GLITCH_PIXEL.get(), fields.getAmbientParticleSettings().orElseThrow().getOptions());
        assertEquals(BiomeAmbience.STRANGE, wastes.getAmbientMoodSettings(), "ring 1's distorted cave sound inside");
        assertThrows(IllegalArgumentException.class,
                () -> BiomeAmbience.ring5(ModBiomes.SUNDERED_REACH, new BiomeSpecialEffects.Builder()));
        assertThrows(IllegalArgumentException.class,
                () -> BiomeAmbience.netherRing5(ModBiomes.BONE_DUNES, new BiomeSpecialEffects.Builder()));
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
            // Package 56: ice crystals (subtle), the cold wind and creaking ice.
            AmbientParticleSettings particle = effects.getAmbientParticleSettings().orElseThrow();
            assertSame(ModParticleTypes.ICE_GLINT.get(), particle.getOptions());
            float glints = probability(particle);
            assertTrue(glints > 0.0F && glints <= 0.01F, biome + " " + glints);
            assertSame(ModSoundEvents.NETHER_RING_3_WIND, effects.getAmbientLoopSoundEvent().orElseThrow());
            AmbientAdditionsSettings creaks = effects.getAmbientAdditionsSettings().orElseThrow();
            assertSame(ModSoundEvents.NETHER_RING_3_ICE_CREAK, creaks.getSoundEvent());
            assertTrue(creaks.getTickChance() > 0.0 && creaks.getTickChance() < 0.0111, "rarer than the basalt deltas'");
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
