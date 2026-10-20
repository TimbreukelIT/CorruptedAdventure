package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:root_arch} (ring 4 decor rollout group A, 2026-10-19): an eye-catcher of
 * {@code glowroot_swamp}, a great bow of mangrove roots from bank to bank over open water, 12–28 long and 5–10 over the
 * water at its crown, often with a thinner twin, moss on top and moss and glowing roots hanging under it.
 * <p>
 * One arch at most per landmark cell ({@link Landmarks}): centred on water near the anchor, feet on land, water under
 * the middle, all from the pure ring 4 terrain ({@link IslandField}) and the pure biome source, so every chunk finds the
 * same arch; each chunk writes its own part ({@link RootArch#write}), merging with the swamp trees by their own priority.
 */
public final class RootArchFeature extends Feature<RootArchConfiguration> {
    /** How far an arch reaches from its centre (half its span, the bow and the thickness). */
    static final int SPAN_REACH = 20;
    /** How far from its anchor an arch looks for water to stand over. */
    static final int SEARCH = 20;
    /** How far an arch reaches from its anchor. */
    static final int REACH = SPAN_REACH + SEARCH;
    static final int MIN_HALF_SPAN = 6;
    static final int MAX_HALF_SPAN = 14;
    /** Headings tried from an anchor in the water. */
    static final int HEADINGS = 8;
    private static final long SALT = 0xA2C4_2007L;

    public RootArchFeature(Codec<RootArchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RootArchConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        RootArchConfiguration config = context.config();
        if (field == null || config.grid().isEmpty()) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        int sea = level.getSeaLevel();
        List<RootArch> arches = new ArrayList<>();
        for (Landmarks.Candidate at : Landmarks.reaching(Landmarks.world(level), level.getSeed(), SALT, config.grid().get(), chunk, REACH,
                candidate -> arches(level, field, config, sea, candidate) != null)) {
            arches.addAll(arches(level, field, config, sea, at));
        }
        return RootArch.write(level, config, chunk, arches, SALT);
    }

    /**
     * The arch (and its twin) at a candidate, or null where it may not stand: both feet on land in the biome, water under
     * most of the span, no island low over it, no lift or fall near.
     */
    static List<RootArch> arches(WorldGenLevel level, IslandField field, RootArchConfiguration config, int sea, Landmarks.Candidate at) {
        RandomSource random = at.random();
        double heading0 = random.nextDouble() * Math.PI;
        double crown = sea + 5 + random.nextInt(6);
        double bow = (random.nextDouble() - 0.5) * 5.0;
        double radius = 0.85 + random.nextDouble() * 0.45;
        boolean twin = random.nextFloat() < 0.6F;
        double twinSide = random.nextBoolean() ? 1.0 : -1.0;
        double twinDrop = 2.0 + random.nextInt(3);
        // The centre: the first water column on a spiral round the anchor (the swamp is mostly land with channels).
        int[] centre = water(field, sea, at.x(), at.z());
        if (centre == null || !clear(field, centre, crown)) {
            return null;
        }
        // From the water at the centre, the first heading (of eight) and span (short to long) that finds a bank both ways.
        for (int k = 0; k < HEADINGS; k++) {
            double heading = heading0 + k * Math.PI / HEADINGS;
            double ux = Math.cos(heading);
            double uz = Math.sin(heading);
            for (int half = MIN_HALF_SPAN; half <= MAX_HALF_SPAN; half += 2) {
                double x1 = centre[0] + 0.5 - ux * half;
                double z1 = centre[1] + 0.5 - uz * half;
                double x2 = centre[0] + 0.5 + ux * half;
                double z2 = centre[1] + 0.5 + uz * half;
                double foot1 = field.column(Mth.floor(x1), Mth.floor(z1)).surface();
                double foot2 = field.column(Mth.floor(x2), Mth.floor(z2)).surface();
                if (!bank(foot1, sea) || !bank(foot2, sea) || !wetBetween(field, sea, x1, z1, x2, z2)
                        || !inBiome(level, config, centre, x1, z1, x2, z2)) {
                    continue;
                }
                List<RootArch> arches = new ArrayList<>();
                // The feet sink one block into the bank (the writer keeps the roots out of the ground itself).
                double y1 = Math.floor(foot1) - 1.0;
                double y2 = Math.floor(foot2) - 1.0;
                arches.add(new RootArch(x1, y1, z1, x2, y2, z2, crown, bow, radius));
                if (twin) {
                    arches.add(new RootArch(x1 - uz * twinSide, y1, z1 + ux * twinSide, x2 - uz * twinSide, y2, z2 + ux * twinSide,
                            crown - twinDrop, bow + twinSide * 1.5, radius * 0.6));
                }
                return arches;
            }
        }
        return null;
    }

    /** The first column under water on a square spiral of step 4 round x, z (up to {@link #SEARCH} away), or null. */
    private static int[] water(IslandField field, int sea, int x, int z) {
        for (int ring = 0; ring * 4 <= SEARCH; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring && field.column(x + dx * 4, z + dz * 4).surface() < sea - 1.0) {
                        return new int[]{x + dx * 4, z + dz * 4};
                    }
                }
            }
        }
        return null;
    }

    /** A foot's ground: a low bank, just over the water. */
    private static boolean bank(double surface, int sea) {
        return surface >= sea - 1.0 && surface <= sea + 6;
    }

    /** Water under most of the span (three of five samples in its middle). */
    private static boolean wetBetween(IslandField field, int sea, double x1, double z1, double x2, double z2) {
        int wet = 0;
        for (int i = 1; i <= 5; i++) {
            double t = 0.2 + i * 0.1;
            if (field.column(Mth.floor(Mth.lerp(t, x1, x2)), Mth.floor(Mth.lerp(t, z1, z2))).surface() < sea - 1.0) {
                wet++;
            }
        }
        return wet >= 3;
    }

    /** No island low over the centre's surroundings, no lift, fall or world tree near. */
    private static boolean clear(IslandField field, int[] centre, double crown) {
        int minX = centre[0] - SPAN_REACH;
        int minZ = centre[1] - SPAN_REACH;
        int maxX = centre[0] + SPAN_REACH;
        int maxZ = centre[1] + SPAN_REACH;
        return field.islandsWithin(minX, minZ, maxX, maxZ).stream().noneMatch(island -> island.bottom() < crown + 8)
                && field.liftsWithin(minX, minZ, maxX, maxZ).isEmpty() && field.waterfallsWithin(minX, minZ, maxX, maxZ).isEmpty()
                && field.worldTrees(minX - 64, minZ - 64, maxX + 64, maxZ + 64).isEmpty();
    }

    /** The centre and both feet in the configured biome. */
    private static boolean inBiome(WorldGenLevel level, RootArchConfiguration config, int[] centre, double x1, double z1,
                                   double x2, double z2) {
        return config.biome().isEmpty() || Landmarks.biome(level, centre[0], centre[1]).is(config.biome().get())
                && Landmarks.biome(level, Mth.floor(x1), Mth.floor(z1)).is(config.biome().get())
                && Landmarks.biome(level, Mth.floor(x2), Mth.floor(z2)).is(config.biome().get());
    }
}
