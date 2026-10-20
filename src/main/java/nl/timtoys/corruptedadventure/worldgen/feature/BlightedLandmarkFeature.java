package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * {@code corruptedadventure:blighted_landmark} (ring 4 decor B, 2026-10-19): writes this chunk's part of every landmark
 * of {@code kind} ({@link BlightedLandmarks}) that reaches into it. Placed once per chunk. The landmark is the same in
 * every chunk (pure in the seed and the terrain); each chunk writes only its own columns, in this order:
 * <ol>
 *   <li>the feet (stone or bone from the column's own frozen ground down into it, up to the planned block) and the
 *   objects, over whatever is there ({@link GroundObjects#placeObject}: trees never take them above the surface);</li>
 *   <li>the wood, by the trees' priority ({@link GroundObjects#placeWood});</li>
 *   <li>the soil round a pool and the pool itself ({@link SludgePuddle}, all in one chunk by the site's choice);</li>
 *   <li>plants, carpets and vines, only into air, only when their support lies in this chunk.</li>
 * </ol>
 * So the blocks of a chunk depend only on the chunk, whatever was generated before it.
 */
public final class BlightedLandmarkFeature extends Feature<BlightedLandmarkFeature.Config> {
    /** The kind of landmark the feature writes. */
    public record Config(BlightedLandmarks.Kind kind, BlockState sludge) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlightedLandmarks.Kind.CODEC.fieldOf("kind").forGetter(Config::kind),
                BlockState.CODEC.fieldOf("sludge").forGetter(Config::sludge)
        ).apply(instance, Config::new));
    }

    public BlightedLandmarkFeature(Codec<Config> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        List<BlightedLandmarks.Site> sites = BlightedLandmarks.within(level, context.config().kind(), minX, minZ, minX + 15, minZ + 15, 0);
        boolean placed = false;
        for (BlightedLandmarks.Site site : sites) {
            placed |= write(level, BlightedLandmarks.plan(level, site), chunk, context.config().sludge());
        }
        return placed;
    }

    private static boolean write(WorldGenLevel level, LandmarkPlan plan, ChunkPos chunk, BlockState sludge) {
        Predicate<BlockPos> own = pos -> pos.getX() >> 4 == chunk.x && pos.getZ() >> 4 == chunk.z;
        Map<BlockPos, BlockState> objects = new LinkedHashMap<>();
        for (LandmarkPlan.Foot foot : plan.feet) {
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
        Map<BlockPos, BlockState> wood = new LinkedHashMap<>();
        plan.wood.forEach((pos, state) -> {
            if (own.test(pos) && !objects.containsKey(pos)) {
                wood.put(pos, state);
            }
        });
        GroundObjects.placeObject(level, objects);
        GroundObjects.placeWood(level, IsolatedTreeFeature.terrain(level), wood);

        plan.ground.forEach((column, state) -> {
            if (own.test(column) && GroundObjects.dry(level, column.getX(), column.getZ())) {
                BlockPos top = new BlockPos(column.getX(), GroundObjects.surface(level, column.getX(), column.getZ()) - 1, column.getZ());
                if (SludgePuddle.soil(level.getBlockState(top))) {
                    level.setBlock(top, state, Block.UPDATE_CLIENTS);
                }
            }
        });
        Set<BlockPos> pool = plan.pool;
        if (!pool.isEmpty() && pool.stream().allMatch(own)) {
            SludgePuddle.place(level, sludge, pool);
        }

        for (LandmarkPlan.Decor decor : plan.decor) {
            if (own.test(decor.pos()) && own.test(decor.support())) {
                GroundObjects.placeDecor(level, decor.pos(), decor.state());
            }
        }
        plan.groundDecor.forEach((column, state) -> {
            if (own.test(column) && GroundObjects.dry(level, column.getX(), column.getZ())) {
                GroundObjects.placeDecor(level, new BlockPos(column.getX(), GroundObjects.surface(level, column.getX(), column.getZ()),
                        column.getZ()), state);
            }
        });
        return !objects.isEmpty() || !wood.isEmpty();
    }
}
