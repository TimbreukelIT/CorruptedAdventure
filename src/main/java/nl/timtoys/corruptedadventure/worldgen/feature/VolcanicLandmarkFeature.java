package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code corruptedadventure:volcanic_landmark} (G2 ring 3, decision 3 = A): writes this chunk's part of every ring 3
 * landmark ({@link VolcanicLandmarks}) that reaches into it. Placed once per chunk in the seven biomes that have one.
 * The landmark is the same in every chunk (pure in the seed and the terrain); each chunk writes only its own columns, in
 * this order:
 * <ol>
 *   <li>the covers: each column's own ground (its frozen {@code WORLD_SURFACE_WG} height) recoloured and raised, never
 *   over a fluid;</li>
 *   <li>the feet (from the column's own frozen floor down into it, up to the planned block) and the objects, over
 *   whatever is there ({@link GroundObjects#placeObject});</li>
 *   <li>plants and layers, only into air, only when their support lies in this chunk; loose blocks on the ground over
 *   the column's cover.</li>
 * </ol>
 * So the blocks of a chunk depend only on the chunk and the landmark, whatever was generated before it.
 */
public final class VolcanicLandmarkFeature extends Feature<NoneFeatureConfiguration> {
    public VolcanicLandmarkFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        boolean placed = false;
        for (VolcanicLandmarks.Kind kind : VolcanicLandmarks.Kind.values()) {
            for (VolcanicLandmarks.Site site : VolcanicLandmarks.within(level, kind, minX, minZ, minX + 15, minZ + 15, 0)) {
                placed |= write(level, VolcanicLandmarks.plan(level, site), chunk);
            }
        }
        return placed;
    }

    private static boolean write(WorldGenLevel level, VolcanicPlan plan, ChunkPos chunk) {
        Predicate<BlockPos> own = pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        Map<BlockPos, Integer> raised = new LinkedHashMap<>();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        plan.covers.forEach((column, cover) -> {
            if (!own.test(column)) {
                return;
            }
            int top = GroundObjects.surface(level, column.getX(), column.getZ()) - 1;
            if (!level.getFluidState(probe.set(column.getX(), top, column.getZ())).isEmpty()) {
                return;
            }
            for (int d = 0; d < cover.depth(); d++) {
                BlockState state = level.getBlockState(probe.set(column.getX(), top - d, column.getZ()));
                if (state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced()) {
                    break;
                }
                blocks.put(probe.immutable(), cover.fill());
            }
            int up = 0;
            for (int r = 1; r <= cover.raise(); r++) {
                BlockState state = level.getBlockState(probe.set(column.getX(), top + r, column.getZ()));
                if (!state.isAir() && !(state.canBeReplaced() && state.getFluidState().isEmpty())) {
                    break;
                }
                blocks.put(probe.immutable(), cover.raised());
                up = r;
            }
            raised.put(column, up);
            if (cover.cap() != null && level.getBlockState(probe.set(column.getX(), top + up + 1, column.getZ())).isAir()) {
                blocks.put(probe.immutable(), cover.cap());
            }
        });
        GroundObjects.placeObject(level, blocks);

        Map<BlockPos, BlockState> objects = new LinkedHashMap<>();
        for (VolcanicPlan.Foot foot : plan.feet) {
            if (own.test(new BlockPos(foot.x(), 0, foot.z()))) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, foot.x(), foot.z());
                for (int y = floor - foot.sink(); y < foot.bottom(); y++) {
                    objects.put(new BlockPos(foot.x(), y, foot.z()), foot.state());
                }
            }
        }
        plan.objects.forEach((pos, state) -> {
            if (own.test(pos)) {
                objects.put(pos, state);
            }
        });
        GroundObjects.placeObject(level, objects);

        for (VolcanicPlan.Decor decor : plan.decor) {
            if (own.test(decor.pos()) && own.test(decor.support())) {
                GroundObjects.placeDecor(level, decor.pos(), decor.state());
            }
        }
        plan.groundDecor.forEach((column, state) -> {
            if (own.test(column)) {
                int y = GroundObjects.surface(level, column.getX(), column.getZ()) + raised.getOrDefault(column, 0);
                BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
                if (!plan.objects.containsKey(pos) && level.getFluidState(pos.below()).isEmpty()) {
                    GroundObjects.placeDecor(level, pos, state);
                }
            }
        });
        return !blocks.isEmpty() || !objects.isEmpty();
    }
}
