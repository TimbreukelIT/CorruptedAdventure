package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code corruptedadventure:root_tangle} (ring 4 decor rollout group A, 2026-10-19): a knot of small mangrove root bows
 * in shallow water of {@code glowroot_swamp}, 1–3 blocks over the water, mossy on top with a glowing root or a strand
 * of moss under them. A ground object in its own chunk ({@link GroundObjects#fit}, its own random, the frozen
 * heightmaps of its chunk), written as {@link RootArch}.
 */
public final class RootTangleFeature extends Feature<RootArchConfiguration> {
    /** How far a tangle reaches from its centre. */
    static final int REACH = 7;
    /** The deepest water a tangle stands in. */
    static final int MAX_DEPTH = 4;
    private static final long SALT = 0x7A_4C1EL;

    public RootTangleFeature(Codec<RootArchConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RootArchConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = GroundObjects.random(level.getSeed(), context.origin(), SALT);
        BlockPos origin = GroundObjects.fit(context.origin(), REACH);
        int sea = level.getSeaLevel();
        int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        if (GroundObjects.surface(level, origin.getX(), origin.getZ()) != sea || sea - floor > MAX_DEPTH || floor >= sea) {
            return false;
        }
        TreeOverlap.Terrain terrain = IsolatedTreeFeature.terrain(level);
        int count = 3 + random.nextInt(3);
        List<RootArch> arches = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double heading = random.nextDouble() * Math.PI;
            double half = 1.5 + random.nextDouble() * 2.5;
            double cx = origin.getX() + 0.5 + (random.nextDouble() - 0.5) * 4.0;
            double cz = origin.getZ() + 0.5 + (random.nextDouble() - 0.5) * 4.0;
            double crown = sea + 1 + random.nextInt(3);
            double bow = (random.nextDouble() - 0.5) * 3.0;
            double radius = 0.5 + random.nextDouble() * 0.25;
            double x1 = cx - Math.cos(heading) * half;
            double z1 = cz - Math.sin(heading) * half;
            double x2 = cx + Math.cos(heading) * half;
            double z2 = cz + Math.sin(heading) * half;
            arches.add(new RootArch(x1, floorAt(level, terrain, sea, x1, z1), z1, x2, floorAt(level, terrain, sea, x2, z2), z2, crown,
                    bow, radius));
        }
        return RootArch.write(level, context.config(), new ChunkPos(origin), arches, SALT);
    }

    /**
     * The top block of the ground at a foot (its own chunk: the tangle was fitted into it), from the terrain alone
     * ({@link RootArch#groundTop}: an island overhead is no floor).
     */
    private static double floorAt(WorldGenLevel level, TreeOverlap.Terrain terrain, int sea, double x, double z) {
        int bottom = sea - MAX_DEPTH - 4;
        return Math.max(bottom, RootArch.groundTop(level, terrain, Mth.floor(x), Mth.floor(z), sea, bottom)) - 1.0;
    }
}
