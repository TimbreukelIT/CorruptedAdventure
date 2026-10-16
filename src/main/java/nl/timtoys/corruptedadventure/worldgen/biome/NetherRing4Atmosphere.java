package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

import java.util.Map;

/**
 * The atmosphere of the Nether ring 4 biomes in datagen (stream A, change 80): a near white-beige fog per biome, bone
 * dust drifting in the air ({@code bone_dust}), the dry wind as a loop and the soft hum of the crystals as additions
 * (more often among the spires and in the geodes), vanilla's cave mood. Only effects: the biome bootstrap (stream
 * W-nether) reaches this through {@link BiomeAmbience#netherRing4} and adds only the music. The pulsing crystal light on
 * the fog and the far view are client code ({@code NetherRing4Fog}).
 */
final class NetherRing4Atmosphere {
    /** The bone desert: near white-beige fog; no sky in the Nether. Water as vanilla's. */
    static final BiomeAmbience.Palette NETHER_RING_4 = new BiomeAmbience.Palette(0x000000, 0xD8CBA6, 0x3F76E4, 0x050533);

    private static final Map<ResourceKey<Biome>, Look> LOOKS = Map.of(
            // The base: dunes of bone sand, the most dust.
            ModBiomes.BONE_DUNES, new Look(NETHER_RING_4, 0.004F, 0.001),
            // Whiter and a touch cooler between the crystal pillars; the hum more often.
            ModBiomes.CRYSTAL_SPIRES, new Look(NETHER_RING_4.withFog(0xDCD8CC), 0.002F, 0.004),
            // A faint lilac glow of the geode linings.
            ModBiomes.GEODE_HOLLOWS, new Look(NETHER_RING_4.withFog(0xD4CBD6), 0.0015F, 0.005));

    private NetherRing4Atmosphere() {
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 4 atmosphere */
    static BiomeSpecialEffects.Builder effects(ResourceKey<Biome> biome) {
        Look look = look(biome);
        return BiomeAmbience.colours(look.colours())
                .ambientParticle(new AmbientParticleSettings(ModParticleTypes.BONE_DUST.get(), look.dust()))
                .ambientLoopSound(ModSoundEvents.NETHER_RING_4_WIND)
                .ambientAdditionsSound(new AmbientAdditionsSettings(ModSoundEvents.NETHER_RING_4_CRYSTAL_HUM, look.humChance()))
                .ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS);
    }

    /** @throws IllegalArgumentException for a biome without Nether ring 4 atmosphere */
    static Look look(ResourceKey<Biome> biome) {
        Look look = LOOKS.get(biome);
        if (look == null) {
            throw new IllegalArgumentException("no Nether ring 4 atmosphere for " + biome.location());
        }
        return look;
    }

    /**
     * Fog and water colours, the bone dust's probability per tried spot and how often the crystals hum per tick.
     */
    record Look(BiomeAmbience.Palette colours, float dust, double humChance) {
    }
}
