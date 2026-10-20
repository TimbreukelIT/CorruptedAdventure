package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import nl.timtoys.corruptedadventure.registry.ModBiomes;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

/**
 * {@code corruptedadventure:giant_moonpetal} (decor rollout C, 2026-10-19): the rare eye-catcher of the sky gardens, a
 * moonpetal far larger than the cascade tree whose flat blossom crown covers its whole island
 * ({@link GiantMoonpetalShape}; which island: {@link SkyLandmarks}). Placed once per chunk; it grows where the trunk of
 * a landmark of its chunk stands on dry sky-gardens ground of its island, with the trunk free up to its top.
 * <p>
 * A ring 4 tree as {@link IsolatedTreeFeature}: its own random (position and world seed), grown in a {@link TreePlan}
 * against the terrain alone and written with the trees' priority, so the crown that reaches into the neighbouring
 * chunks merges with their trees the same in either order. The shape never goes into water (a waterfall over the
 * island's edge stays whole).
 */
public final class GiantMoonpetalFeature extends Feature<GiantMoonpetalConfiguration> {
    /** The 2 × 2 trunk keeps this far from the chunk's edges (its crown stays inside the decoration window). */
    static final int MARGIN = 3;
    /** The trunk stands at most this far below its island's top (on the island, not in a hollow beside it). */
    private static final int MAX_BELOW_TOP = 6;
    private static final long SALT = 0x61A7_7AEEL;

    public GiantMoonpetalFeature(Codec<GiantMoonpetalConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<GiantMoonpetalConfiguration> context) {
        WorldGenLevel level = context.level();
        IslandField field = Ring4Terrain.field(level);
        if (field == null || !(level instanceof WorldGenRegion)) {
            return false;
        }
        int minX = context.origin().getX() & ~15;
        int minZ = context.origin().getZ() & ~15;
        boolean placed = false;
        for (SkyLandmarks.Landmark landmark : SkyLandmarks.within(field, level.getSeed(), minX, minZ, minX + 15, minZ + 15)) {
            if (landmark.kind() == SkyLandmarks.Kind.GIANT_MOONPETAL) {
                placed |= grow(level, context.config(), landmark);
            }
        }
        return placed;
    }

    private static boolean grow(WorldGenLevel level, GiantMoonpetalConfiguration config, SkyLandmarks.Landmark landmark) {
        int x = landmark.x();
        int z = landmark.z();
        int low = Integer.MAX_VALUE;
        int base = Integer.MIN_VALUE;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                if (!GroundObjects.dry(level, x + dx, z + dz)) {
                    return false;
                }
                int ground = GroundObjects.surface(level, x + dx, z + dz);
                low = Math.min(low, ground);
                base = Math.max(base, ground);
            }
        }
        if (base - low > 2 || base < landmark.island().top() - MAX_BELOW_TOP
                || !level.getBiome(new BlockPos(x, base, z)).is(ModBiomes.SKY_GARDENS)) {
            return false;
        }
        GiantMoonpetalShape shape = GiantMoonpetalShape.grow(GroundObjects.random(level.getSeed(), new BlockPos(x, 0, z), SALT),
                crown(landmark.island()));
        TreePlan plan = new TreePlan(level, IsolatedTreeFeature.terrain(level));
        BlockPos origin = new BlockPos(x, base, z);
        for (BlockPos log : shape.trunk()) {
            if (!free(plan, origin.offset(log))) {
                return false;
            }
        }
        for (BlockPos log : shape.trunk()) {
            plan.trunkSetter().accept(origin.offset(log), config.log());
        }
        // Under the trunk down to each column's own ground (a gentle slope under the 2 × 2).
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                for (int y = GroundObjects.surface(level, x + dx, z + dz); y < base; y++) {
                    plan.trunkSetter().accept(new BlockPos(x + dx, y, z + dz), config.log());
                }
            }
        }
        for (BlockPos part : shape.wood()) {
            BlockPos pos = origin.offset(part);
            if (shape.roots().contains(part)) {
                // A root flare reaches down to the ground of its column.
                int ground = GroundObjects.surface(level, pos.getX(), pos.getZ());
                for (int y = pos.getY(); y >= ground && y >= base - 4 && free(plan, pos.atY(y)); y--) {
                    plan.trunkSetter().accept(pos.atY(y), config.wood());
                }
            } else if (free(plan, pos)) {
                plan.trunkSetter().accept(pos, config.wood());
            }
        }
        for (BlockPos leaf : shape.leaves()) {
            BlockPos pos = origin.offset(leaf);
            if (free(plan, pos)) {
                plan.foliageSetter().set(pos, config.leaves());
            }
        }
        plan.settleLeafDistances();
        plan.commit();
        return true;
    }

    /** The crown's radius for an island: about its radius, so the blossom covers it. */
    static int crown(IslandField.Island island) {
        return Mth.clamp((int) Math.round(island.radius() * 0.75) + 2, GiantMoonpetalShape.MIN_CROWN, GiantMoonpetalShape.MAX_CROWN);
    }

    /** Vanilla's {@code TreeFeature.validTreePos} as the tree sees the level, but never into a fluid. */
    private static boolean free(TreePlan plan, BlockPos pos) {
        return plan.isStateAtPosition(pos, state -> state.getFluidState().isEmpty()
                && (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE_BY_TREES)));
    }
}
