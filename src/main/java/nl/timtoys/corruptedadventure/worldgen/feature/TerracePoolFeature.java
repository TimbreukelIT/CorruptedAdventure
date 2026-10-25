package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

/**
 * {@code corruptedadventure:terrace_pool} (change 47d, decision 4b-2; G2 ring 3, decision 6 = A): the boiling pools of
 * the sulfur fields in basins of travertine: the pool flush with the ground (one colour per pool), a raised lip around
 * it and a band of terrace steps outside the lip. Only columns whose top lies at the pool's level (the lip and terraces
 * one block either way) and is listed ground take part, so the pool follows level ground.
 * <p>
 * G2: the pools lie in clusters of different sizes and ragged, stretched shapes ({@link TerracePoolLayout}), wider
 * than a chunk. As the titan rocks, each pool is a pure function of the seed and its cell; the checks at its centre (the
 * biome from the biome source, plain ground from the pure terrain, level ground and the pool's level from the frozen
 * {@code WORLD_SURFACE_WG}) read nothing a neighbour writes, and a pool reaches at most {@link #MAX_REACH} &lt; 16, so
 * its centre lies within one chunk of every chunk it covers. Every chunk writes only its own columns, from its own
 * blocks. Placed once per chunk, without a biome filter.
 */
public final class TerracePoolFeature extends Feature<TerracePoolConfiguration> {
    public static final int MAX_RADIUS = 6;
    /** The lip and the terraces around the pool, in blocks (before the stretch). */
    static final double LIP = 1.2;
    static final double TERRACE = 2.6;
    /** How far a pool with its terraces reaches from its centre. */
    public static final int MAX_REACH = Mth.ceil((MAX_RADIUS + TERRACE) * TerracePoolLayout.MAX_STRETCH);
    /** The centre stands on plain ground: no lava, crater, flank or cliff. */
    private static final VolcanicFilter PLAIN = VolcanicFilter.builder().ground().build();
    /** Level ground around the centre (as {@code SlopeFilter.flat(3, 1)} of 47d). */
    private static final int FLAT_RADIUS = 3;
    private static final int FLAT_RELIEF = 1;

    public TerracePoolFeature(Codec<TerracePoolConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TerracePoolConfiguration> context) {
        TerracePoolConfiguration config = context.config();
        WorldGenLevel level = context.level();
        VolcanicField field = Ring3Terrain.field(level);
        if (field == null) {
            return false;
        }
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int cell = config.cell();
        boolean placed = false;
        // Pool centres lie inside their cell, so only cells within MAX_REACH of the chunk can reach it.
        for (int i = Math.floorDiv(minX - MAX_REACH, cell); i <= Math.floorDiv(minX + 15 + MAX_REACH, cell); i++) {
            for (int j = Math.floorDiv(minZ - MAX_REACH, cell); j <= Math.floorDiv(minZ + 15 + MAX_REACH, cell); j++) {
                for (TerracePoolLayout.Pool pool : TerracePoolLayout.cluster(level.getSeed(), i, j, cell, config.chance(), config.pools(),
                        config.radius())) {
                    int reach = pool.reach();
                    if (pool.x() + reach < minX || pool.x() - reach > minX + 15 || pool.z() + reach < minZ || pool.z() - reach > minZ + 15) {
                        continue;
                    }
                    if (fits(level, field, config, pool)) {
                        placed |= place(level, config, context.random(), pool, minX, minZ);
                    }
                }
            }
        }
        return placed;
    }

    /** Whether the pool's centre lies in the biome, on plain and level ground; the same answer in every chunk. */
    private static boolean fits(WorldGenLevel level, VolcanicField field, TerracePoolConfiguration config, TerracePoolLayout.Pool pool) {
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pool.x(), pool.z());
        if (!PLAIN.test(field.column(pool.x(), pool.z()), surface)) {
            return false;
        }
        int relief = SlopeFilter.measure(SlopeFilter.Measure.RELIEF, FLAT_RADIUS, pool.x(), pool.z(),
                (x, z) -> level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z));
        if (relief > FLAT_RELIEF) {
            return false;
        }
        RandomState random = level.getLevel().getChunkSource().randomState();
        BiomeSource biomes = level.getLevel().getChunkSource().getGenerator().getBiomeSource();
        return biomes.getNoiseBiome(QuartPos.fromBlock(pool.x()), QuartPos.fromBlock(surface), QuartPos.fromBlock(pool.z()), random.sampler())
                .is(config.biome());
    }

    /** Writes the pool's columns of the chunk with minimum corner {@code minX, minZ}. */
    private static boolean place(WorldGenLevel level, TerracePoolConfiguration config, RandomSource random, TerracePoolLayout.Pool pool,
                                 int minX, int minZ) {
        int radius = pool.radius();
        BlockState fill = config.fill().getState(RandomSource.create(pool.seed()), new BlockPos(pool.x(), 0, pool.z()));
        int level0 = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pool.x(), pool.z()) - 1;
        int reach = pool.reach();
        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = Math.max(minZ, pool.z() - reach); z <= Math.min(minZ + 15, pool.z() + reach); z++) {
            for (int x = Math.max(minX, pool.x() - reach); x <= Math.min(minX + 15, pool.x() + reach); x++) {
                // Distance in blocks to the ragged, stretched edge's scale (≤ radius = pool).
                double blocks = GroundPatchFeature.distance(x - pool.x(), z - pool.z(), radius, pool.stretch(), pool.angle(), pool.edge())
                        * radius;
                if (blocks > radius + TERRACE) {
                    continue;
                }
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                boolean inPool = blocks <= radius;
                if (inPool ? top != level0 : Math.abs(top - level0) > 1 || !level.getBlockState(pos.set(x, top, z)).is(config.ground())) {
                    continue;
                }
                if (inPool) {
                    if (level.getBlockState(pos.set(x, top, z)).is(config.ground())) {
                        level.setBlock(pos, fill, Block.UPDATE_CLIENTS);
                        placed = true;
                    }
                } else if (blocks <= radius + LIP) {
                    level.setBlock(pos.set(x, top, z), config.lip().getState(random, pos), Block.UPDATE_CLIENTS);
                    if (top == level0 && level.getBlockState(pos.set(x, top + 1, z)).isAir()) {
                        level.setBlock(pos, config.lip().getState(random, pos), Block.UPDATE_CLIENTS);
                    }
                    placed = true;
                } else {
                    level.setBlock(pos.set(x, top, z), config.terrace().getState(random, pos), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
