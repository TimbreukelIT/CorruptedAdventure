package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:sky_ruin} (decor rollout C, 2026-10-19): the other eye-catcher of the sky gardens, a
 * crumbling ring of stone pillars on a floating island, two of them still joined by an arch, an old floor between them,
 * fallen blocks around and a crystal on a stone in the middle (which island: {@link SkyLandmarks}). Mythical, no
 * structure and no loot.
 * <p>
 * A ground object ({@link GroundObjects}): its shape comes from its own random (position and world seed) and the frozen
 * heightmaps, it stays within {@link #REACH} of its anchor in its own chunk, its stone goes over whatever stands there
 * and trees never take it ({@link TreeOverlap#object}); moss, vines and the crystal only go into air. So either order of
 * generation ends the same.
 */
public final class SkyRuinFeature extends Feature<SkyRuinConfiguration> {
    /** How far the ruin reaches from its anchor (the pillars stand on a ring of radius 3–3.6, rubble up to here). */
    static final int REACH = 5;
    /** A pillar stands only where its ground lies within this of the anchor's (no pillar down a slope). */
    private static final int MAX_STEP = 3;
    private static final int SLOTS = 6;
    private static final long SALT = 0x5C7A_1121L;

    public SkyRuinFeature(Codec<SkyRuinConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<SkyRuinConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (SkyLandmarks.Landmark landmark : SkyLandmarks.within(field, level.getSeed(), minX, minZ, minX + 15, minZ + 15)) {
            if (landmark.kind() == SkyLandmarks.Kind.RUIN) {
                placed |= build(level, context.config(), landmark);
            }
        }
        return placed;
    }

    private static boolean build(WorldGenLevel level, SkyRuinConfiguration config, SkyLandmarks.Landmark landmark) {
        int ax = landmark.x();
        int az = landmark.z();
        int ground = GroundObjects.surface(level, ax, az);
        if (!GroundObjects.dry(level, ax, az) || ground < landmark.island().top() - 8
                || !level.getBiome(new BlockPos(ax, ground, az)).is(ModBiomes.SKY_GARDENS)) {
            return false;
        }
        RandomSource random = GroundObjects.random(level.getSeed(), new BlockPos(ax, 0, az), SALT);
        // Every draw first, in a fixed order: the shape is the random's alone.
        double ring = 3.0 + random.nextDouble() * 0.6;
        double turn = random.nextDouble() * Math.PI * 2.0;
        boolean arch = random.nextFloat() < 0.85F;
        int archHeight = 7 + random.nextInt(3);
        int[] heights = new int[SLOTS];
        boolean[] present = new boolean[SLOTS];
        boolean[] whole = new boolean[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            present[i] = random.nextFloat() < 0.85F;
            whole[i] = random.nextFloat() < 0.5F;
            heights[i] = whole[i] ? 6 + random.nextInt(4) : 1 + random.nextInt(5);
        }
        if (arch) {
            present[0] = present[1] = whole[0] = whole[1] = true;
            heights[0] = heights[1] = archHeight;
        }
        Map<BlockPos, BlockState> stone = new LinkedHashMap<>();
        List<BlockPos> tops = new ArrayList<>();
        List<BlockPos> shafts = new ArrayList<>();
        int[][] columns = new int[SLOTS][];
        for (int i = 0; i < SLOTS; i++) {
            double angle = turn + i * Math.PI * 2.0 / SLOTS;
            int x = ax + (int) Math.round(ring * Math.cos(angle));
            int z = az + (int) Math.round(ring * Math.sin(angle));
            int foot = GroundObjects.surface(level, x, z);
            boolean stands = present[i] && GroundObjects.dry(level, x, z) && Math.abs(foot - ground) <= MAX_STEP;
            columns[i] = stands ? new int[]{x, z, foot} : null;
        }
        int lintel = Integer.MIN_VALUE;
        if (arch && columns[0] != null && columns[1] != null) {
            lintel = Math.max(columns[0][2], columns[1][2]) + archHeight;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (columns[i] == null) {
                continue;
            }
            int x = columns[i][0];
            int z = columns[i][1];
            int foot = columns[i][2];
            boolean arched = lintel != Integer.MIN_VALUE && i < 2;
            int top = arched ? lintel - 1 : foot + heights[i] - 1;
            // Sunk one block into the ground, as the boulders: no tree roots on its foot.
            for (int y = foot - 1; y <= top; y++) {
                BlockPos pos = new BlockPos(x, y, z);
                stone.put(pos, config.shaft().getState(random, pos));
                shafts.add(pos);
            }
            if (!arched && whole[i]) {
                BlockPos cap = new BlockPos(x, top + 1, z);
                stone.put(cap, config.capital().getState(random, cap));
            } else if (!arched) {
                tops.add(new BlockPos(x, top + 1, z));
            }
        }
        if (lintel != Integer.MIN_VALUE) {
            // The arch: a beam from one pillar's top to the other's, a keystone over its middle.
            int x0 = columns[0][0];
            int z0 = columns[0][1];
            int x1 = columns[1][0];
            int z1 = columns[1][1];
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
            for (int s = 0; s <= steps; s++) {
                int x = x0 + Math.round((x1 - x0) * s / (float) steps);
                int z = z0 + Math.round((z1 - z0) * s / (float) steps);
                BlockPos pos = new BlockPos(x, lintel, z);
                stone.put(pos, config.shaft().getState(random, pos));
                if (s == steps / 2) {
                    stone.put(pos.above(), config.capital().getState(random, pos.above()));
                }
            }
        }
        // Rubble: fallen blocks on the ground round the ring.
        int pieces = 4 + random.nextInt(4);
        List<BlockPos> rubble = new ArrayList<>();
        for (int p = 0; p < pieces; p++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double out = 1.5 + random.nextDouble() * (REACH - 1.5);
            BlockState piece = config.rubble().getState(random, BlockPos.ZERO);
            int x = ax + (int) Math.round(out * Math.cos(angle));
            int z = az + (int) Math.round(out * Math.sin(angle));
            int foot = GroundObjects.surface(level, x, z);
            BlockPos pos = new BlockPos(x, foot, z);
            if (GroundObjects.dry(level, x, z) && Math.abs(foot - ground) <= MAX_STEP && !stone.containsKey(pos) && !stone.containsKey(pos.below())) {
                stone.put(pos, piece);
                rubble.add(pos);
            }
        }
        boolean altar = random.nextFloat() < 0.7F;
        if (altar) {
            stone.put(new BlockPos(ax, ground, az), config.altar());
        }
        // The old floor between the pillars: the ground's top block in patches.
        Map<BlockPos, BlockState> floor = new LinkedHashMap<>();
        for (int dz = -REACH + 1; dz <= REACH - 1; dz++) {
            for (int dx = -REACH + 1; dx <= REACH - 1; dx++) {
                boolean tile = random.nextFloat() < 0.5F;
                BlockState state = config.floor().getState(random, BlockPos.ZERO);
                if (!tile || dx * dx + dz * dz > (ring + 1.0) * (ring + 1.0)) {
                    continue;
                }
                int x = ax + dx;
                int z = az + dz;
                floor.put(new BlockPos(x, GroundObjects.surface(level, x, z) - 1, z), state);
            }
        }
        List<Boolean> carpets = new ArrayList<>();
        for (int i = 0; i < tops.size() + rubble.size(); i++) {
            carpets.add(random.nextFloat() < 0.5F);
        }
        float[] vines = new float[shafts.size() * 4];
        for (int i = 0; i < vines.length; i++) {
            vines[i] = random.nextFloat();
        }

        floor.forEach((pos, state) -> {
            BlockState there = level.getBlockState(pos);
            if (!stone.containsKey(pos) && (there.is(Blocks.GRASS_BLOCK) || there.is(BlockTags.DIRT)) && GroundObjects.dry(level, pos.getX(), pos.getZ())) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            }
        });
        GroundObjects.placeObject(level, stone);
        if (altar) {
            GroundObjects.placeDecor(level, new BlockPos(ax, ground + 1, az), config.light());
        }
        List<BlockPos> carpeted = new ArrayList<>(tops);
        rubble.forEach(pos -> carpeted.add(pos.above()));
        for (int i = 0; i < carpeted.size(); i++) {
            if (carpets.get(i)) {
                GroundObjects.placeDecor(level, carpeted.get(i), config.moss());
            }
        }
        for (int i = 0; i < shafts.size(); i++) {
            BlockPos shaft = shafts.get(i);
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (vines[i * 4 + side.get2DDataValue()] < 0.12F && shaft.getY() >= GroundObjects.surface(level, shaft.getX(), shaft.getZ())) {
                    // The vine stands beside the shaft and holds on to it (its face towards the shaft).
                    GroundObjects.placeDecor(level, shaft.relative(side),
                            config.vine().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true));
                }
            }
        }
        return true;
    }
}
