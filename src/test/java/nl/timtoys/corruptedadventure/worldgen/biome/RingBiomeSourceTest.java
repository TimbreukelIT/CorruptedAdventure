package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring4Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingBiomeSourceTest {
    private static final List<Integer> RADII = CommonConfig.DEFAULT_OUTER_RADII;
    private static final RingLayout LAYOUT = new RingLayout(new RingSettings(RADII, 10200, 96, 128,
            CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES), 12345L);
    private static final int[] CENTRES = {750, 2500, 4750, 7000, 8850, 9950};
    private static final int REACH = 96 + 64;

    private final Map<Ring, Holder<Biome>> biomes = new EnumMap<>(Ring.class);
    private final Holder<Biome> transitionBiome = Holder.direct(biome());
    private final RingBiomeSource overworld;
    private final RingBiomeSource nether;
    /** Overworld with a transition biome for boundary 1 only. */
    private final RingBiomeSource withTransition;
    private final Holder<Biome> secondTransitionBiome = Holder.direct(biome());
    /** Overworld with a transition biome for boundaries 1 and 2 (change 31). */
    private final RingBiomeSource withTwoTransitions;

    RingBiomeSourceTest() {
        Map<Ring, BiomeSource> sources = new EnumMap<>(Ring.class);
        for (Ring ring : Ring.values()) {
            Holder<Biome> biome = Holder.direct(biome());
            biomes.put(ring, biome);
            sources.put(ring, new FixedBiomeSource(biome));
        }
        overworld = new RingBiomeSource(Level.OVERWORLD, sources, Map.of());
        nether = new RingBiomeSource(Level.NETHER, sources, Map.of());
        withTransition = new RingBiomeSource(Level.OVERWORLD, sources, Map.of(Ring.RING_1, new FixedBiomeSource(transitionBiome)));
        withTwoTransitions = new RingBiomeSource(Level.OVERWORLD, sources, Map.of(Ring.RING_1, new FixedBiomeSource(transitionBiome),
                Ring.RING_2, new FixedBiomeSource(secondTransitionBiome)));
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
            for (int boundary = 1; boundary <= RADII.size(); boundary++) {
                assertHardSwitch(overworld, boundary, angle);
            }
        }
    }

    @Test
    void transitionBiomeFillsTheWholeZoneOfItsBoundary() {
        for (int angle = 0; angle < 360; angle += 15) {
            double lowestBlend = 1.0;
            double highestBlend = 0.0;
            for (int distance = RADII.get(0) - REACH; distance <= RADII.get(0) + REACH; distance++) {
                RingPosition position = positionAt(Level.OVERWORLD, distance, angle);
                if (position.inTransition()) {
                    assertEquals(transitionBiome, biomeAt(withTransition, distance, angle), "zone of boundary 1 at " + distance);
                    lowestBlend = Math.min(lowestBlend, position.blend());
                    highestBlend = Math.max(highestBlend, position.blend());
                } else {
                    assertEquals(biomes.get(position.ring()), biomeAt(withTransition, distance, angle), "outside the zone at " + distance);
                }
            }
            // Quart corners lie up to ~6 blocks apart along a diagonal, so the edges are only reached approximately.
            assertTrue(lowestBlend < 0.1 && highestBlend > 0.9, "zone at angle " + angle + " not crossed: " + lowestBlend + "–" + highestBlend);
        }
    }

    @Test
    void everyBoundaryWithATransitionBiomeFillsItsOwnZoneAndTheRestStaysHard() {
        for (int angle = 0; angle < 360; angle += 15) {
            double lowestBlend = 1.0;
            double highestBlend = 0.0;
            for (int boundary = 1; boundary <= 2; boundary++) {
                Holder<Biome> expectedZone = boundary == 1 ? transitionBiome : secondTransitionBiome;
                for (int distance = RADII.get(boundary - 1) - REACH; distance <= RADII.get(boundary - 1) + REACH; distance++) {
                    RingPosition position = positionAt(Level.OVERWORLD, distance, angle);
                    if (position.inTransition() && position.inner() == Ring.values()[boundary - 1]) {
                        assertEquals(expectedZone, biomeAt(withTwoTransitions, distance, angle), "zone of boundary " + boundary + " at " + distance);
                        if (boundary == 2) {
                            lowestBlend = Math.min(lowestBlend, position.blend());
                            highestBlend = Math.max(highestBlend, position.blend());
                        }
                    }
                }
            }
            assertTrue(lowestBlend < 0.1 && highestBlend > 0.9, "zone 2 not crossed at angle " + angle);
            for (int boundary = 3; boundary <= RADII.size(); boundary++) {
                assertHardSwitch(withTwoTransitions, boundary, angle);
            }
            assertEquals(biomes.get(Ring.RING_2), biomeAt(withTwoTransitions, CENTRES[1], angle));
            assertEquals(biomes.get(Ring.RING_3), biomeAt(withTwoTransitions, CENTRES[2], angle));
        }
        Set<Holder<Biome>> expected = new HashSet<>(biomes.values());
        expected.add(transitionBiome);
        expected.add(secondTransitionBiome);
        assertEquals(expected, withTwoTransitions.possibleBiomes());
        assertTrue(decode(", \"transitions\": {\"ring_1\": " + fixed("a") + ", \"ring_2\": " + fixed("b") + "}").isSuccess());
    }

    @Test
    void boundariesWithoutTransitionStillSwitchHard() {
        for (int angle = 0; angle < 360; angle += 15) {
            for (int boundary = 2; boundary <= RADII.size(); boundary++) {
                assertHardSwitch(withTransition, boundary, angle);
            }
            assertEquals(biomes.get(Ring.RING_1), biomeAt(withTransition, CENTRES[0], angle));
            assertEquals(biomes.get(Ring.RING_2), biomeAt(withTransition, CENTRES[1], angle));
        }
    }

    @Test
    void possibleBiomesIncludeTransitions() {
        Set<Holder<Biome>> expected = new HashSet<>(biomes.values());
        expected.add(transitionBiome);
        assertEquals(expected, withTransition.possibleBiomes());
    }

    @Test
    void transitionsAreOptionalButNeverForTheVoid() {
        assertTrue(decode("").isSuccess());
        assertTrue(decode(", \"transitions\": {\"ring_1\": " + fixed("transition") + "}").isSuccess());
        assertTrue(decode(", \"transitions\": {\"void\": " + fixed("transition") + "}").isError());
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

    /**
     * 59: the height hook. An island of ring 4 with its top at Y 140 or higher gets the height biome of the band at its
     * centre, as a whole and from 4 blocks under its bottom up; lower islands, the ground and other rings keep theirs.
     */
    @Test
    void highRing4IslandsGetTheHeightBiomeOfTheBandAtTheirCentre() {
        Holder<Biome> mythic = Holder.direct(biome());
        Holder<Biome> blighted = Holder.direct(biome());
        Map<Ring, BiomeSource> sources = new EnumMap<>(Ring.class);
        biomes.forEach((ring, biome) -> sources.put(ring, new FixedBiomeSource(biome)));
        RingBiomeSource hooked = new RingBiomeSource(Level.OVERWORLD, sources, Map.of(), new IslandBiomes(mythic, blighted, 140));
        Ring4Band band = Ring4Band.of(LAYOUT);
        int[] mythicCentre = centre(band, false);
        int[] blightedCentre = centre(band, true);
        for (int[] centre : new int[][]{mythicCentre, blightedCentre}) {
            Holder<Biome> expected = band.blighted(centre[0], centre[1]) ? blighted : mythic;
            IslandField.Island high = new IslandField.Island(centre[0], centre[1], 24.0, 120, 140);
            RingBiomeSource.IslandLookup islands = (x, z) -> high.covers(x, z) ? List.of(high) : List.of();
            int qx = QuartPos.fromBlock(centre[0]);
            int qz = QuartPos.fromBlock(centre[1]);
            // From the quart whose middle lies 4 blocks under the bottom (Y 116) up; the ground below keeps its biome.
            assertEquals(expected, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(116), qz, Climate.empty()));
            assertEquals(expected, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(300), qz, Climate.empty()));
            assertEquals(biomes.get(Ring.RING_4), hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(112), qz, Climate.empty()));
            // Within the footprint only.
            assertEquals(biomes.get(Ring.RING_4), hooked.getNoiseBiome(LAYOUT, islands, QuartPos.fromBlock(centre[0] + 40), QuartPos.fromBlock(130), qz, Climate.empty()));
            assertEquals(expected, hooked.getNoiseBiome(LAYOUT, islands, QuartPos.fromBlock(centre[0] + 16), QuartPos.fromBlock(130), qz, Climate.empty()));
            // A top below 140 keeps the ground biome.
            IslandField.Island low = new IslandField.Island(centre[0], centre[1], 24.0, 100, 139);
            assertEquals(biomes.get(Ring.RING_4), hooked.getNoiseBiome(LAYOUT, (x, z) -> low.covers(x, z) ? List.of(low) : List.of(),
                    qx, QuartPos.fromBlock(130), qz, Climate.empty()));
        }
        assertTrue(hooked.possibleBiomes().containsAll(Set.of(mythic, blighted)));
        // Other rings never ask, even with an island over them; nor does a source without the hook.
        IslandField.Island elsewhere = new IslandField.Island(CENTRES[2], 0, 24.0, 120, 200);
        assertEquals(biomes.get(Ring.RING_3), hooked.getNoiseBiome(LAYOUT, (x, z) -> List.of(elsewhere), QuartPos.fromBlock(CENTRES[2]), QuartPos.fromBlock(150),
                0, Climate.empty()));
        IslandField.Island ring4 = new IslandField.Island(mythicCentre[0], mythicCentre[1], 24.0, 120, 200);
        assertEquals(biomes.get(Ring.RING_4), overworld.getNoiseBiome(LAYOUT, (x, z) -> List.of(ring4), QuartPos.fromBlock(mythicCentre[0]),
                QuartPos.fromBlock(150), QuartPos.fromBlock(mythicCentre[1]), Climate.empty()));
        assertTrue(decode(", \"island_biomes\": {\"mythic\": \"test:sky\", \"blighted\": \"test:withered\"}").isSuccess());
    }

    /**
     * Review 59 (#1): where two islands overlap, each block takes the highest island it is not below (from 4 under its
     * bottom up), so the lower island keeps its own height biome under the higher one. Their centres lie in different
     * bands here, so the biome tells which island was taken.
     */
    @Test
    void underAnOverlappingHigherIslandTheLowerKeepsItsOwnHeightBiome() {
        Holder<Biome> mythic = Holder.direct(biome());
        Holder<Biome> blighted = Holder.direct(biome());
        Map<Ring, BiomeSource> sources = new EnumMap<>(Ring.class);
        biomes.forEach((ring, biome) -> sources.put(ring, new FixedBiomeSource(biome)));
        RingBiomeSource hooked = new RingBiomeSource(Level.OVERWORLD, sources, Map.of(), new IslandBiomes(mythic, blighted, 140));
        Ring4Band band = Ring4Band.of(LAYOUT);
        int[] pair = acrossTheLine(band);
        IslandField.Island lower = new IslandField.Island(pair[0], pair[1], 30.0, 110, 150);
        IslandField.Island higher = new IslandField.Island(pair[0] + 40, pair[1], 30.0, 190, 240);
        Holder<Biome> lowerBiome = band.blighted(lower.x(), lower.z()) ? blighted : mythic;
        Holder<Biome> higherBiome = band.blighted(higher.x(), higher.z()) ? blighted : mythic;
        RingBiomeSource.IslandLookup islands = (x, z) -> List.of(lower, higher).stream().filter(island -> island.covers(x, z)).toList();
        // A quart in the overlap, its middle 22 blocks from both centres.
        int qx = QuartPos.fromBlock(pair[0] + 18);
        int qz = QuartPos.fromBlock(pair[1]);
        assertEquals(lowerBiome, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(130), qz, Climate.empty()));
        assertEquals(lowerBiome, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(180), qz, Climate.empty()));
        assertEquals(higherBiome, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(188), qz, Climate.empty()));
        assertEquals(higherBiome, hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(260), qz, Climate.empty()));
        assertEquals(biomes.get(Ring.RING_4), hooked.getNoiseBiome(LAYOUT, islands, qx, QuartPos.fromBlock(100), qz, Climate.empty()));
    }

    /** A quart corner in ring 4 proper whose band differs from the one 40 blocks east (both also in ring 4 proper). */
    private static int[] acrossTheLine(Ring4Band band) {
        for (int z = -4000; z <= 4000; z += 52) {
            for (int x = 5600; x <= 7900; x += 4) {
                RingPosition here = LAYOUT.sample(Level.OVERWORLD, x, z);
                RingPosition east = LAYOUT.sample(Level.OVERWORLD, x + 40, z);
                if (!here.inTransition() && here.ring() == Ring.RING_4 && !east.inTransition() && east.ring() == Ring.RING_4
                        && band.blighted(x, z) != band.blighted(x + 40, z)) {
                    return new int[]{x, z};
                }
            }
        }
        throw new AssertionError("no band line crossed");
    }

    /** An island centre in ring 4 proper (a quart corner) well inside the given band. */
    private static int[] centre(Ring4Band band, boolean blighted) {
        for (int angle = 0; angle < 360; angle += 7) {
            for (int distance = 6200; distance <= 7900; distance += 20) {
                int x = (int) Math.round(distance * Math.cos(Math.toRadians(angle))) & ~3;
                int z = (int) Math.round(distance * Math.sin(Math.toRadians(angle))) & ~3;
                RingPosition position = LAYOUT.sample(Level.OVERWORLD, x, z);
                if (!position.inTransition() && position.ring() == Ring.RING_4 && band.blighted(x, z) == blighted
                        && band.blighted(x + 40, z) == blighted && band.blighted(x - 40, z) == blighted) {
                    return new int[]{x, z};
                }
            }
        }
        throw new AssertionError("no island centre on the " + (blighted ? "blighted" : "mythical") + " side");
    }

    /** Away from the boundary's zone nothing changes; inside it the biome follows the side of blend 0.5. */
    private void assertHardSwitch(RingBiomeSource source, int boundary, int angle) {
        int radius = RADII.get(boundary - 1);
        boolean sawInner = false;
        boolean sawOuter = false;
        for (int distance = radius - REACH; distance <= radius + REACH; distance += 4) {
            RingPosition position = positionAt(Level.OVERWORLD, distance, angle);
            if (!position.inTransition()) {
                continue;
            }
            Ring expected = position.blend() < 0.5 ? position.inner() : position.outer();
            assertEquals(biomes.get(expected), biomeAt(source, distance, angle), "boundary " + boundary + " at " + distance);
            sawInner |= expected == position.inner();
            sawOuter |= expected == position.outer();
        }
        assertTrue(sawInner && sawOuter, "boundary " + boundary + " at angle " + angle + " has no switch");
    }

    /** Decodes an overworld source with every ring; {@code extraFields} is appended to its JSON fields. */
    private static DataResult<RingBiomeSource> decode(String extraFields) {
        String rings = Arrays.stream(Ring.values())
                .map(ring -> "\"" + ring.getSerializedName() + "\": " + fixed(ring.getSerializedName()))
                .collect(Collectors.joining(", "));
        String json = "{\"dimension\": \"minecraft:overworld\", \"rings\": {" + rings + "}" + extraFields + "}";
        return RingBiomeSource.CODEC.codec().parse(StandaloneHolderGetter.ops(Registries.BIOME, key -> true), JsonParser.parseString(json));
    }

    private static String fixed(String biome) {
        return "{\"type\": \"minecraft:fixed\", \"biome\": \"test:" + biome + "\"}";
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
