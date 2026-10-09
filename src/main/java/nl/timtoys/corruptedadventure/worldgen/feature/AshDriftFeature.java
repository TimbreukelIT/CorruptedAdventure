package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.VolcanicField;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code corruptedadventure:ash_drift} (change 47, backlog r. 193): ash layers on the open ground of the whole chunk,
 * like snow. Per column of the origin's chunk: a patchy base from a world noise, thicker in the lee of higher ground
 * (the dune wind of the terrain) and in gullies, and a ramp of layers against every one-block step of the plain so the
 * stair lines break (decision 39f). Placed once per chunk; the column's own biome decides whether and how thick.
 * <p>
 * Never on lava banks, cliffs, volcano flanks, craters, thin solids (needles, ribs, arches, islands: the found surface
 * must lie on the terrain height) or within 2 blocks of lava, never on flora or features (the block above the ground
 * must be air). Reads the frozen {@code WORLD_SURFACE_WG} heightmap (up to 4 blocks into the neighbours), the pure
 * terrain columns ({@link Ring3Terrain}), a seeded noise and its own chunk's blocks; writes only its own chunk.
 */
public final class AshDriftFeature extends Feature<AshDriftConfiguration> {
    /** How far around a column the heights are read (lee, gullies, steps). */
    static final int REACH = 4;
    private static final int SIDE = 16 + 2 * REACH;
    /** No ash within this many blocks of the terrain's lava (plan §0.2). */
    static final int LAVA_REACH = 2;
    private static final int LAVA_SIDE = 16 + 2 * LAVA_REACH;

    public AshDriftFeature(Codec<AshDriftConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<AshDriftConfiguration> context) {
        WorldGenLevel level = context.level();
        VolcanicField field = Ring3Terrain.field(level);
        if (field == null) {
            return false;
        }
        AshDriftConfiguration config = context.config();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        int[] heights = new int[SIDE * SIDE];
        for (int dz = 0; dz < SIDE; dz++) {
            for (int dx = 0; dx < SIDE; dx++) {
                heights[dx + dz * SIDE] = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, minX - REACH + dx, minZ - REACH + dz);
            }
        }
        Heights at = (x, z) -> heights[(x - minX + REACH) + (z - minZ + REACH) * SIDE];
        NormalNoise noise = level.getLevel().getChunkSource().randomState().getOrCreateNoise(config.noise());
        Map<ResourceKey<Biome>, AshDriftConfiguration.BiomeAmount> amounts = new HashMap<>();
        config.biomes().forEach(amount -> amounts.put(amount.biome(), amount));
        double windX = Math.cos(config.windAngle());
        double windZ = Math.sin(config.windAngle());

        // The terrain's own lava of every column within LAVA_REACH, read once (a column is the thread's cache slot).
        int[] lava = new int[LAVA_SIDE * LAVA_SIDE * 3];
        for (int dz = 0; dz < LAVA_SIDE; dz++) {
            for (int dx = 0; dx < LAVA_SIDE; dx++) {
                VolcanicField.Column column = field.column(minX - LAVA_REACH + dx, minZ - LAVA_REACH + dz);
                int i = (dx + dz * LAVA_SIDE) * 3;
                lava[i] = column.lavaTop();
                lava[i + 1] = column.lavaBottom();
                lava[i + 2] = column.springY();
            }
        }

        boolean placed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int z = minZ; z < minZ + 16; z++) {
            for (int x = minX; x < minX + 16; x++) {
                int surface = at.height(x, z);
                pos.set(x, surface, z);
                AshDriftConfiguration.BiomeAmount amount = level.getBiome(pos).unwrapKey().map(amounts::get).orElse(null);
                if (amount == null || !GROUND.test(field.column(x, z), surface) || lavaNear(lava, x - minX, z - minZ, surface)) {
                    continue;
                }
                if (amount.flatOnly() && SlopeFilter.measure(SlopeFilter.Measure.RELIEF, 2, x, z, at::height) > 2) {
                    continue;
                }
                double n = noise.getValue(x * config.noiseScale(), 0.0, z * config.noiseScale());
                int layers = layers(amount, n, x, z, windX, windZ, at);
                if (layers <= 0 || !level.getBlockState(pos).isAir()) {
                    continue;
                }
                BlockState ground = level.getBlockState(pos.below());
                if (!ground.is(config.ground())) {
                    continue;
                }
                BlockState layer = config.layer().defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
                if (layer.canSurvive(level, pos)) {
                    level.setBlock(pos, layer, Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /**
     * Whether a column within {@link #LAVA_REACH} of the chunk column (lx, lz) has its lava (or spring) between 2 below
     * and 1 above {@code surface}; {@code lava} holds top, bottom and spring per column of the window.
     */
    static boolean lavaNear(int[] lava, int lx, int lz, int surface) {
        for (int dz = 0; dz <= 2 * LAVA_REACH; dz++) {
            for (int dx = 0; dx <= 2 * LAVA_REACH; dx++) {
                int i = (lx + dx + (lz + dz) * LAVA_SIDE) * 3;
                for (int y = surface - 2; y <= surface + 1; y++) {
                    if (VolcanicField.Column.lavaAt(y, lava[i], lava[i + 1], lava[i + 2])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Plain ground of the terrain (plan §0.2–0.3): no lava bank, cliff, flank, crater or thin solid, the surface on it. */
    private static final VolcanicFilter GROUND = VolcanicFilter.builder().ground().build();

    /**
     * Layers for one column (0–8): the wind drift (patchy base, lee, gully) times the biome's amount, and with steps the
     * ramp against a riser one block higher 1–3 blocks away (6, 4, 2 layers, one more where the noise is high; at most 7).
     */
    static int layers(AshDriftConfiguration.BiomeAmount amount, double noise, int x, int z, double windX, double windZ, Heights heights) {
        int h = heights.height(x, z);
        int upwind = heights.height(x - (int) Math.round(windX * 3.0), z - (int) Math.round(windZ * 3.0));
        double lee = Mth.clamp((upwind - h) / 3.0, 0.0, 1.0);
        int ring = 0;
        int count = 0;
        for (int dz = -REACH; dz <= REACH; dz += REACH) {
            for (int dx = -REACH; dx <= REACH; dx += REACH) {
                if (dx != 0 || dz != 0) {
                    ring += heights.height(x + dx, z + dz);
                    count++;
                }
            }
        }
        double gully = Mth.clamp(((double) ring / count - h) / 3.0, 0.0, 1.0);
        double base = Mth.clamp((noise + 0.15) * 2.5, 0.0, 3.0);
        double drift = amount.amount() * (base + 4.0 * lee + 3.0 * gully);
        int step = amount.steps() ? step(x, z, h, heights) : 0;
        if (step > 0 && noise > 0.2) {
            step = Math.min(7, step + 1);
        }
        return Math.min(SnowLayerBlock.MAX_HEIGHT, Math.max((int) Math.round(drift), step));
    }

    /** 6, 4 or 2 when a column exactly one higher lies 1, 2 or 3 blocks away along an axis over level ground; else 0. */
    static int step(int x, int z, int h, Heights heights) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int d = 1; d <= 3; d++) {
            for (int[] dir : directions) {
                if (heights.height(x + dir[0] * d, z + dir[1] * d) == h + 1 && level(x, z, dir, d, h, heights)) {
                    return 8 - 2 * d;
                }
            }
        }
        return 0;
    }

    private static boolean level(int x, int z, int[] dir, int d, int h, Heights heights) {
        for (int k = 1; k < d; k++) {
            if (heights.height(x + dir[0] * k, z + dir[1] * k) != h) {
                return false;
            }
        }
        return true;
    }

    @FunctionalInterface
    interface Heights {
        int height(int x, int z);
    }
}
