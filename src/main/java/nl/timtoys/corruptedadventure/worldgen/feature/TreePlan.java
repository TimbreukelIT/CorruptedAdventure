package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.common.collect.Iterables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * One tree of {@link IsolatedTreeFeature} before it is written: the blocks it sets (as vanilla's {@code TreeFeature}
 * sets them, in the same four groups) and the level as this tree sees it, its own blocks over the terrain of
 * {@link TreeOverlap#terrain} (another tree's blocks look like the terrain they took). The placers, the decorators
 * and the blocks' survival checks read the plan, so the tree takes the same shape whichever trees stand around it.
 * <p>
 * {@link #settleLeafDistances} is vanilla's leaf pass on the tree's own blocks; {@link #commit} writes them with the
 * priority of {@link TreeOverlap#wins} and then updates the shapes at the edge of what it wrote, as vanilla does at
 * the edge of the tree.
 */
final class TreePlan implements LevelSimulatedReader, LevelReader {
    private static final int UPDATE_FLAGS = 19;
    private static final int MAX_DISTANCE = 7;

    private final WorldGenLevel level;
    private final TreeOverlap.Terrain terrain;
    /** The last state this tree set per position. */
    private final Map<BlockPos, BlockState> own = new HashMap<>();
    final Set<BlockPos> roots = new HashSet<>();
    final Set<BlockPos> logs = new HashSet<>();
    final Set<BlockPos> leaves = new HashSet<>();
    final Set<BlockPos> decorations = new HashSet<>();

    TreePlan(WorldGenLevel level, TreeOverlap.Terrain terrain) {
        this.level = level;
        this.terrain = terrain;
    }

    BiConsumer<BlockPos, BlockState> rootSetter() {
        return (pos, state) -> set(roots, pos, state);
    }

    BiConsumer<BlockPos, BlockState> trunkSetter() {
        return (pos, state) -> set(logs, pos, state);
    }

    BiConsumer<BlockPos, BlockState> decorationSetter() {
        return (pos, state) -> set(decorations, pos, state);
    }

    FoliagePlacer.FoliageSetter foliageSetter() {
        return new FoliagePlacer.FoliageSetter() {
            @Override
            public void set(BlockPos pos, BlockState state) {
                TreePlan.this.set(leaves, pos, state);
            }

            @Override
            public boolean isSet(BlockPos pos) {
                return leaves.contains(pos);
            }
        };
    }

    private void set(Set<BlockPos> group, BlockPos pos, BlockState state) {
        BlockPos at = pos.immutable();
        group.add(at);
        own.put(at, state);
    }

    boolean isEmpty() {
        return logs.isEmpty() && leaves.isEmpty();
    }

    /**
     * Vanilla's {@code TreeFeature.updateLeaves} on the plan: the leaves' distance to the trunk, counted through the
     * tree's own leaves (roots and decorations block the way, as in vanilla). Only the own leaves change.
     */
    void settleLeafDistances() {
        Optional<BoundingBox> bounds = BoundingBox.encapsulatingPositions(Iterables.concat(roots, logs, leaves, decorations));
        if (bounds.isEmpty()) {
            return;
        }
        BoundingBox box = bounds.get();
        DiscreteVoxelShape shape = new BitSetDiscreteVoxelShape(box.getXSpan(), box.getYSpan(), box.getZSpan());
        for (BlockPos pos : Iterables.concat(decorations, roots)) {
            shape.fill(pos.getX() - box.minX(), pos.getY() - box.minY(), pos.getZ() - box.minZ());
        }
        List<Set<BlockPos>> queues = new ArrayList<>();
        for (int i = 0; i < MAX_DISTANCE; i++) {
            queues.add(new HashSet<>());
        }
        queues.getFirst().addAll(logs);
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        int distance = 0;
        while (distance < MAX_DISTANCE) {
            Set<BlockPos> queue = queues.get(distance);
            if (queue.isEmpty()) {
                distance++;
                continue;
            }
            Iterator<BlockPos> iterator = queue.iterator();
            BlockPos pos = iterator.next();
            iterator.remove();
            if (!box.isInside(pos)) {
                continue;
            }
            if (distance != 0) {
                BlockState state = own.get(pos);
                if (state != null && state.hasProperty(BlockStateProperties.DISTANCE)) {
                    own.put(pos, state.setValue(BlockStateProperties.DISTANCE, distance));
                }
            }
            shape.fill(pos.getX() - box.minX(), pos.getY() - box.minY(), pos.getZ() - box.minZ());
            for (Direction direction : Direction.values()) {
                next.setWithOffset(pos, direction);
                if (!box.isInside(next) || shape.isFull(next.getX() - box.minX(), next.getY() - box.minY(), next.getZ() - box.minZ())) {
                    continue;
                }
                OptionalInt at = LeavesBlock.getOptionalDistanceAt(getBlockState(next));
                if (at.isPresent()) {
                    int reached = Math.min(at.getAsInt(), distance + 1);
                    if (reached < MAX_DISTANCE) {
                        queues.get(reached).add(next.immutable());
                        distance = Math.min(distance, reached);
                    }
                }
            }
        }
    }

    /**
     * Writes the plan: per position the planned block if it wins over what was there before this tree
     * ({@link TreeOverlap#wins}, decided for all positions before the first write; never over a ground object,
     * {@link TreeOverlap#object}), then vanilla's shape update on
     * every face between a written block and one that is not (a propagule whose leaf lost to another tree's trunk
     * falls, a tall plant cut in half goes), passed on to the blocks that change (the rest of a giant lily pad).
     */
    void commit() {
        Map<BlockPos, BlockState> winners = new LinkedHashMap<>();
        Set<BlockPos> written = new LinkedHashSet<>();
        for (Map.Entry<BlockPos, BlockState> entry : own.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState ours = entry.getValue();
            BlockState existing = level.getBlockState(pos);
            if (existing == ours) {
                written.add(pos);
            } else if (!TreeOverlap.object(existing, pos, terrain) && TreeOverlap.wins(ours, existing, level.getBlockState(pos.below()))) {
                winners.put(pos, ours);
                written.add(pos);
            }
        }
        winners.forEach((pos, state) -> level.setBlock(pos, state, UPDATE_FLAGS));
        EdgeUpdates.apply(level, written);
    }

    // ---------------------------------------------------------------- the tree's view of the level

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState state = own.get(pos);
        return state != null ? state : TreeOverlap.terrain(level.getBlockState(pos), pos, terrain);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> state) {
        return state.test(getBlockState(pos));
    }

    @Override
    public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) {
        return predicate.test(getFluidState(pos));
    }

    /** None for the tree's own blocks: the plan holds states only (no ring 4 tree has a decorator with a block entity). */
    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return own.containsKey(pos) ? null : level.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return own.containsKey(pos) ? Optional.empty() : level.getBlockEntity(pos, type);
    }

    @Override
    public BlockPos getHeightmapPos(Heightmap.Types heightmapType, BlockPos pos) {
        return level.getHeightmapPos(heightmapType, pos);
    }

    // ---------------------------------------------------------------- the rest as the level

    @Nullable
    @Override
    public ChunkAccess getChunk(int x, int z, ChunkStatus chunkStatus, boolean requireChunk) {
        return level.getChunk(x, z, chunkStatus, requireChunk);
    }

    @Override
    public boolean hasChunk(int chunkX, int chunkZ) {
        return level.hasChunk(chunkX, chunkZ);
    }

    @Override
    public int getHeight(Heightmap.Types heightmapType, int x, int z) {
        return level.getHeight(heightmapType, x, z);
    }

    @Override
    public int getSkyDarken() {
        return level.getSkyDarken();
    }

    @Override
    public BiomeManager getBiomeManager() {
        return level.getBiomeManager();
    }

    @Override
    public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
        return level.getUncachedNoiseBiome(x, y, z);
    }

    @Override
    public boolean isClientSide() {
        return false;
    }

    @Override
    public int getSeaLevel() {
        return level.getSeaLevel();
    }

    @Override
    public DimensionType dimensionType() {
        return level.dimensionType();
    }

    @Override
    public RegistryAccess registryAccess() {
        return level.registryAccess();
    }

    @Override
    public FeatureFlagSet enabledFeatures() {
        return level.enabledFeatures();
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return level.getBlockTint(pos, colorResolver);
    }

    @Override
    public WorldBorder getWorldBorder() {
        return level.getWorldBorder();
    }

    @Override
    public List<VoxelShape> getEntityCollisions(@Nullable Entity entity, AABB collisionBox) {
        return List.of();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }
}
