package nl.timtoys.corruptedadventure.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The blocks of one blighted landmark ({@link BlightedLandmarks}), in world positions, before any chunk writes its
 * part ({@link BlightedLandmarkFeature}). Pure in the site: the same plan in every chunk that asks.
 * <ul>
 *   <li>{@link #objects}: stone, bone, moss and mushroom blocks, written over whatever is there (a ground object,
 *   {@link GroundObjects#placeObject}); {@link #feet}: a column of such a block down into the column's own ground, so
 *   nothing floats where the real ground lies lower than the pure one;</li>
 *   <li>{@link #wood}: logs and wood, merged with the trees by their priority ({@link GroundObjects#placeWood});</li>
 *   <li>{@link #ground}: the top block of a column's ground replaced (mud round a pool); {@link #pool}: the columns of a
 *   toxic pool ({@link SludgePuddle});</li>
 *   <li>{@link #decor}: plants, carpets and vines on a block of the plan, only into air and only when their support is
 *   in the same chunk; {@link #groundDecor}: plants on a column's own ground.</li>
 * </ul>
 */
final class LandmarkPlan {
    /** A plant, carpet or vine at {@code pos} that needs the block at {@code support}. */
    record Decor(BlockPos pos, BlockState state, BlockPos support) {
    }

    /** A column filled with {@code state} from its own ground {@code sink} blocks deep up to under {@code bottom}. */
    record Foot(int x, int z, int bottom, int sink, BlockState state) {
    }

    final Map<BlockPos, BlockState> objects = new LinkedHashMap<>();
    final Map<BlockPos, BlockState> wood = new LinkedHashMap<>();
    final List<Foot> feet = new ArrayList<>();
    /** Per column (x, 0, z): the block that replaces its ground's top. */
    final Map<BlockPos, BlockState> ground = new LinkedHashMap<>();
    /** The columns (x, 0, z) of the toxic pool. */
    final Set<BlockPos> pool = new LinkedHashSet<>();
    final List<Decor> decor = new ArrayList<>();
    /** Per column (x, 0, z): a plant on its own ground. */
    final Map<BlockPos, BlockState> groundDecor = new LinkedHashMap<>();

    /** A vine strand of {@code length} hanging beside {@code support} on its {@code side}, top first. */
    void vine(BlockPos support, Direction side, int length) {
        BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true);
        BlockPos at = support.relative(side);
        BlockPos above = support;
        for (int i = 0; i < length; i++) {
            decor.add(new Decor(at, vine, i == 0 ? support : above));
            above = at;
            at = at.below();
        }
    }

    /** Whether the plan holds a solid block (object or wood) at {@code pos}. */
    boolean solid(BlockPos pos) {
        return objects.containsKey(pos) || wood.containsKey(pos);
    }
}
