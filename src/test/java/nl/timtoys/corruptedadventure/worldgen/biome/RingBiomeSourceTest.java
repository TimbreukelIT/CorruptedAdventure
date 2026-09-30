package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingBiomeSourceTest {
    private static final List<Integer> RADII = List.of(1500, 3500, 5500, 7500, 9500);
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(RADII, 10000, 96, 128), 12345L);
    private static final int[] CENTRES = {750, 2500, 4500, 6500, 8500, 9750};
    private static final int REACH = 96 + 64;

    private final Map<Ring, Holder<Biome>> biomes = new EnumMap<>(Ring.class);
    private final RingBiomeSource overworld;
    private final RingBiomeSource nether;

    RingBiomeSourceTest() {
        Map<Ring, BiomeSource> sources = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            Holder<Biome> biome = Holder.direct(biome());
            biomes.put(ring, biome);
            sources.put(ring, new FixedBiomeSource(biome));
        }
        overworld = new RingBiomeSource(Level.OVERWORLD, sources);
        nether = new RingBiomeSource(Level.NETHER, sources);
    }

    @Test
    void ringCentresGetTheirRingsBiome() {
        for (int angle = 0; angle < 360; angle += 15) {
            for (Ring ring : Ring.values()) {
                int centre = CENTRES[ring.ordinal()];
                assertEquals(biomes.get(ring), biomeAt(overworld, centre, angle), ring + " at angle " + angle);
                assertEquals(biomes.get(ring), biomeAt(nether, centre / RingLayout.NETHER_SCALE, angle), "Nether " + ring + " at angle " + angle);
            }
        }
    }

    @Test
    void transitionZonesSwitchHardAtTheBoundary() {
        for (int angle = 0; angle < 360; angle += 15) {
            for (int i = 0; i < RADII.size(); i++) {
                boolean sawInner = false;
                boolean sawOuter = false;
                for (int distance = RADII.get(i) - REACH; distance <= RADII.get(i) + REACH; distance += 4) {
                    RingPosition position = positionAt(Level.OVERWORLD, distance, angle);
                    if (!position.inTransition()) {
                        continue;
                    }
                    Ring expected = position.blend() < 0.5 ? position.inner() : position.outer();
                    assertEquals(biomes.get(expected), biomeAt(overworld, distance, angle), "boundary " + (i + 1) + " at " + distance);
                    sawInner |= expected == position.inner();
                    sawOuter |= expected == position.outer();
                }
                assertTrue(sawInner && sawOuter, "boundary " + (i + 1) + " at angle " + angle + " has no switch");
            }
        }
    }

    @Test
    void possibleBiomesAreAllRings() {
        assertEquals(Set.copyOf(biomes.values()), overworld.possibleBiomes());
    }

    @Test
    void everyRingIsRequired() {
        Map<Ring, BiomeSource> incomplete = new EnumMap<>(Ring.class);
        incomplete.put(Ring.RING_1, new FixedBiomeSource(biomes.get(Ring.RING_1)));
        assertTrue(RingBiomeSource.validateRings(incomplete).isError());

        Map<Ring, BiomeSource> complete = new EnumMap<>(Ring.class);
        biomes.forEach((ring, biome) -> complete.put(ring, new FixedBiomeSource(biome)));
        assertTrue(RingBiomeSource.validateRings(complete).isSuccess());
    }

    /** Biome at the quart containing the block at {@code distance} along {@code angleDegrees}. */
    private static Holder<Biome> biomeAt(RingBiomeSource source, int distance, int angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        int x = QuartPos.fromBlock((int) Math.floor(distance * Math.cos(angle)));
        int z = QuartPos.fromBlock((int) Math.floor(distance * Math.sin(angle)));
        return source.getNoiseBiome(LAYOUT, x, 64, z, Climate.empty());
    }

    /** Ring position at the corner of the quart that {@link #biomeAt} samples. */
    private static RingPosition positionAt(ResourceKey<Level> dimension, int distance, int angleDegrees) {
        double angle = Math.toRadians(angleDegrees);
        int x = QuartPos.toBlock(QuartPos.fromBlock((int) Math.floor(distance * Math.cos(angle))));
        int z = QuartPos.toBlock(QuartPos.fromBlock((int) Math.floor(distance * Math.sin(angle))));
        return LAYOUT.sample(dimension, x, z);
    }

    private static Biome biome() {
        return new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.5F)
                .downfall(0.0F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(0).fogColor(0).waterColor(0).waterFogColor(0).build())
                .mobSpawnSettings(MobSpawnSettings.EMPTY)
                .generationSettings(BiomeGenerationSettings.EMPTY)
                .build();
    }
}
