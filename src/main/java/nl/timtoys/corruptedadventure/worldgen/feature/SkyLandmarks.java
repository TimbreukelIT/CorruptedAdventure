package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The rare eye-catchers of the sky gardens (decor rollout C, 2026-10-19): which floating island carries a giant
 * moonpetal ({@link GiantMoonpetalFeature}) or a ruin of stone pillars ({@link SkyRuinFeature}), and where. Pure in the
 * world seed and the island field ({@link IslandField#islandsWithin}, {@link IslandField#skyLakes}), so every chunk
 * agrees and the choice never depends on generation order. At most one landmark per island.
 * <ul>
 *   <li>A giant moonpetal stands at the centre of a middle island of radius {@link #MOONPETAL_MIN_RADIUS} to
 *   {@link #MOONPETAL_MAX_RADIUS} without a sky lake (its crown covers the island), with {@link #MOONPETAL_CHANCE}.</li>
 *   <li>A ruin stands on an island of radius {@link #RUIN_MIN_RADIUS} or more, with {@link #RUIN_CHANCE}: near the
 *   centre, or halfway to the edge beside a sky lake.</li>
 * </ul>
 * The features themselves only stand where their anchor is sky gardens and the ground is dry; the island's biome is
 * not decided here.
 */
public final class SkyLandmarks {
    static final double MOONPETAL_MIN_RADIUS = 12.0;
    static final double MOONPETAL_MAX_RADIUS = 21.0;
    static final double MOONPETAL_CHANCE = 0.16;
    static final double RUIN_MIN_RADIUS = 11.0;
    static final double RUIN_CHANCE = 0.14;
    /** Only islands this high carry a landmark (the height biomes start here, {@code IslandBiomes.min_top}). */
    static final int MIN_TOP = 140;
    private static final long SALT = 0x5C1_1A4DL;

    public enum Kind {
        GIANT_MOONPETAL, RUIN
    }

    /** A landmark: what, on which island, and the column it stands on (its anchor). */
    public record Landmark(Kind kind, IslandField.Island island, int x, int z) {
    }

    private SkyLandmarks() {
    }

    /** The landmarks whose anchor lies in the box (block coordinates, inclusive), by x then z. */
    public static List<Landmark> within(IslandField field, long seed, int minX, int minZ, int maxX, int maxZ) {
        List<Landmark> found = new ArrayList<>();
        for (IslandField.Island island : field.islandsWithin(minX, minZ, maxX, maxZ)) {
            Landmark landmark = of(field, seed, island);
            if (landmark != null && landmark.x() >= minX && landmark.x() <= maxX && landmark.z() >= minZ && landmark.z() <= maxZ) {
                found.add(landmark);
            }
        }
        found.sort(Comparator.comparingInt(Landmark::x).thenComparingInt(Landmark::z));
        return found;
    }

    /** The landmark of an island, or null. */
    static Landmark of(IslandField field, long seed, IslandField.Island island) {
        if (island.top() < MIN_TOP || island.radius() < Math.min(MOONPETAL_MIN_RADIUS, RUIN_MIN_RADIUS)) {
            return null;
        }
        int cx = Mth.floor(island.x());
        int cz = Mth.floor(island.z());
        RandomSource random = GroundObjects.random(seed, new BlockPos(cx, 0, cz), SALT);
        double roll = random.nextDouble();
        double angle = random.nextDouble() * Math.PI * 2.0;
        IslandField.SkyLake lake = lakeOf(field, island);
        if (roll < MOONPETAL_CHANCE) {
            boolean fits = island.radius() >= MOONPETAL_MIN_RADIUS && island.radius() <= MOONPETAL_MAX_RADIUS && lake == null;
            return fits ? anchored(Kind.GIANT_MOONPETAL, island, cx, cz, GiantMoonpetalFeature.MARGIN) : null;
        }
        if (roll < MOONPETAL_CHANCE + RUIN_CHANCE && island.radius() >= RUIN_MIN_RADIUS) {
            // Beside a sky lake halfway to the edge, else a little off the centre (the crown of the island's tree).
            double out = lake != null ? Math.max(lake.radius() + 7.0, 0.5 * island.radius()) : 0.25 * island.radius();
            if (out > island.radius() - 6.0) {
                return null;
            }
            return anchored(Kind.RUIN, island, Mth.floor(island.x() + out * Math.cos(angle)), Mth.floor(island.z() + out * Math.sin(angle)),
                    SkyRuinFeature.REACH);
        }
        return null;
    }

    /** A landmark at (x, z), moved inside its chunk so that everything within {@code margin} of it stays there. */
    private static Landmark anchored(Kind kind, IslandField.Island island, int x, int z, int margin) {
        BlockPos anchor = GroundObjects.fit(new BlockPos(x, 0, z), margin);
        return new Landmark(kind, island, anchor.getX(), anchor.getZ());
    }

    /** The island's sky lake (or dry bowl), or null. */
    private static IslandField.SkyLake lakeOf(IslandField field, IslandField.Island island) {
        int cx = Mth.floor(island.x());
        int cz = Mth.floor(island.z());
        for (IslandField.SkyLake lake : field.skyLakes(cx, cz, cx, cz)) {
            if (Math.abs(lake.x() - cx) <= 1 && Math.abs(lake.z() - cz) <= 1) {
                return lake;
            }
        }
        return null;
    }
}
