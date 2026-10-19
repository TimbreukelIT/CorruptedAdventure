package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code corruptedadventure:boulder} (ring 4 decor proef, 2026-10-19): a rounded rock on the ground, sunk one block
 * into it, mossy on top. Half an ellipsoid of {@code radius} × {@code height} with a ragged rim; each column stands on
 * its own ground (a slope tilts the rock, no pillar hangs down a drop of more than {@link #MAX_DROP}), only on dry land.
 * A ground object ({@link GroundObjects}): its own random, the frozen heightmaps, its own chunk; trees never take its
 * stone ({@link TreeOverlap#object}), so either order of generation ends the same.
 */
public final class BoulderFeature extends Feature<BoulderConfiguration> {
    static final float MAX_RADIUS = 3.5F;
    /** A column whose ground lies further below the rock's foot is left out. */
    static final int MAX_DROP = 2;
    private static final long SALT = 0xB0_01DE_25L;

    public BoulderFeature(Codec<BoulderConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BoulderConfiguration> context) {
        WorldGenLevel level = context.level();
        BoulderConfiguration config = context.config();
        RandomSource random = GroundObjects.random(level.getSeed(), context.origin(), SALT);
        float rx = config.radius().sample(random);
        float rz = config.radius().sample(random);
        float height = config.height().sample(random);
        int reach = Mth.ceil(Math.max(rx, rz));
        BlockPos origin = GroundObjects.fit(context.origin(), reach);
        int foot = GroundObjects.surface(level, origin.getX(), origin.getZ());
        if (!GroundObjects.dry(level, origin.getX(), origin.getZ())) {
            return false;
        }
        Map<BlockPos, BlockState> stones = new LinkedHashMap<>();
        List<BlockPos> tops = new ArrayList<>();
        for (int dz = -reach; dz <= reach; dz++) {
            for (int dx = -reach; dx <= reach; dx++) {
                // Every column draws the same, wherever it stands: the shape is the random's alone.
                float rim = random.nextFloat();
                float bump = random.nextFloat();
                double d = Mth.square(dx / (double) rx) + Mth.square(dz / (double) rz);
                if (d + 0.3 * rim > 1.0) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int ground = GroundObjects.surface(level, x, z);
                int top = foot - 1 + (int) Math.round(height * Math.sqrt(Math.max(0.0, 1.0 - d)) + 0.6 * bump - 0.3);
                if (ground < foot - MAX_DROP || ground - 1 > top || !GroundObjects.dry(level, x, z)) {
                    continue;
                }
                for (int y = ground - 1; y <= top; y++) {
                    stones.put(new BlockPos(x, y, z), config.stone().getState(random, new BlockPos(x, y, z)));
                }
                tops.add(new BlockPos(x, top, z));
            }
        }
        if (stones.isEmpty()) {
            return false;
        }
        List<Boolean> carpets = new ArrayList<>(tops.size());
        for (BlockPos top : tops) {
            boolean mossy = random.nextFloat() < config.mossChance();
            BlockState moss = config.mossyTop().getState(random, top);
            if (mossy) {
                stones.put(top, moss);
            }
            carpets.add(random.nextFloat() < config.carpetChance());
        }
        GroundObjects.placeObject(level, stones);
        for (int i = 0; i < tops.size(); i++) {
            if (carpets.get(i)) {
                GroundObjects.placeDecor(level, tops.get(i).above(), config.carpet());
            }
        }
        return true;
    }
}
