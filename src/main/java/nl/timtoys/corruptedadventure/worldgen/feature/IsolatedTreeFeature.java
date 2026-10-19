package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.WorldTreePart;

import java.util.List;
import java.util.OptionalInt;

/**
 * {@code corruptedadventure:isolated_tree} (tree determinism, option A, 2026-10-18): vanilla's {@code minecraft:tree}
 * with the same configuration and the same shapes, for the ring 4 trees, but independent of the order in which the
 * chunks decorate. Vanilla's tree reads its footprint, so a trunk or plant of a neighbouring chunk drops a leaf, root or
 * branch and shifts the random every later draw (hanging leaves, root simulation, moss and glowcaps on the roots,
 * propagules), and all trees of a feature in a chunk share that random. Here:
 * <ul>
 *   <li>every tree has its own random, seeded on its position and the world seed;</li>
 *   <li>it grows in a {@link TreePlan}: against the terrain without the other trees ({@link TreeOverlap#terrain}),
 *   the placers and decorators unchanged;</li>
 *   <li>where two trees want one block a fixed priority decides ({@link TreeOverlap#wins}: trunk &gt; root &gt; leaf
 *   &gt; moss, glowcap or propagule).</li>
 * </ul>
 * Outside world generation (a sapling, {@code /place}) it is vanilla's tree in the live world.
 */
public final class IsolatedTreeFeature extends Feature<TreeConfiguration> {
    /** Keeps the trees' randoms apart from other per-position randoms of the same world seed. */
    private static final long SALT = 0x7EE5_150A_7EDL;

    public IsolatedTreeFeature(Codec<TreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        WorldGenLevel level = context.level();
        if (!(level instanceof WorldGenRegion)) {
            return Feature.TREE.place(context);
        }
        BlockPos origin = context.origin();
        TreeConfiguration config = context.config();
        RandomSource random = random(level.getSeed(), origin);
        TreePlan plan = new TreePlan(level, terrain(level));
        if (!grow(plan, random, origin, config) || plan.isEmpty()) {
            return false;
        }
        if (!config.decorators.isEmpty()) {
            TreeDecorator.Context decorations = new TreeDecorator.Context(plan, plan.decorationSetter(), random,
                    plan.logs, plan.leaves, plan.roots);
            config.decorators.forEach(decorator -> decorator.place(decorations));
        }
        plan.settleLeafDistances();
        plan.commit();
        return true;
    }

    /**
     * The tree's own random: the same tree at the same place of the same world, whatever grew before it (the feature
     * is not part of the seed: two tree features never share an origin, one cell holds one tree).
     */
    static RandomSource random(long worldSeed, BlockPos origin) {
        return new XoroshiroRandomSource(RandomSupport.mixStafford13(worldSeed ^ SALT), RandomSupport.mixStafford13(origin.asLong()));
    }

    /** Vanilla's {@code TreeFeature.doPlace} on the plan. */
    private static boolean grow(TreePlan plan, RandomSource random, BlockPos pos, TreeConfiguration config) {
        int height = config.trunkPlacer.getTreeHeight(random);
        int foliageHeight = config.foliagePlacer.foliageHeight(random, height, config);
        int foliageRadius = config.foliagePlacer.foliageRadius(random, height - foliageHeight);
        BlockPos trunkOrigin = config.rootPlacer.map(roots -> roots.getTrunkOrigin(pos, random)).orElse(pos);
        int bottom = Math.min(pos.getY(), trunkOrigin.getY());
        int top = Math.max(pos.getY(), trunkOrigin.getY()) + height + 1;
        if (bottom < plan.getMinBuildHeight() + 1 || top > plan.getMaxBuildHeight()) {
            return false;
        }
        OptionalInt minClipped = config.minimumSize.minClippedHeight();
        int free = maxFreeTreeHeight(plan, height, trunkOrigin, config);
        if (free < height && (minClipped.isEmpty() || free < minClipped.getAsInt())) {
            return false;
        }
        if (config.rootPlacer.isPresent() && !config.rootPlacer.get().placeRoots(plan, plan.rootSetter(), random, pos, trunkOrigin, config)) {
            return false;
        }
        List<FoliagePlacer.FoliageAttachment> attachments = config.trunkPlacer.placeTrunk(plan, plan.trunkSetter(), random, free, trunkOrigin, config);
        FoliagePlacer.FoliageSetter foliage = plan.foliageSetter();
        attachments.forEach(attachment -> config.foliagePlacer.createFoliage(plan, foliage, random, config, free, attachment,
                foliageHeight, foliageRadius));
        return true;
    }

    /** Vanilla's {@code TreeFeature.getMaxFreeTreeHeight} on the plan. */
    private static int maxFreeTreeHeight(TreePlan plan, int trunkHeight, BlockPos top, TreeConfiguration config) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = 0; y <= trunkHeight + 1; y++) {
            int size = config.minimumSize.getSizeAtHeight(trunkHeight, y);
            for (int x = -size; x <= size; x++) {
                for (int z = -size; z <= size; z++) {
                    pos.setWithOffset(top, x, y, z);
                    if (!config.trunkPlacer.isFree(plan, pos) || !config.ignoreVines && plan.getBlockState(pos).is(Blocks.VINE)) {
                        return y - 2;
                    }
                }
            }
        }
        return trunkHeight;
    }

    /**
     * The terrain alone: the world trees of ring 4 (74; none outside ring 4's terrain), and water below the sea level
     * or between a column's floor and its water top on the frozen worldgen heightmaps (lakes; 23c: they show the
     * terrain, also in the neighbouring chunks).
     */
    static TreeOverlap.Terrain terrain(WorldGenLevel level) {
        IslandField field = Ring4Terrain.field(level);
        int seaLevel = level.getSeaLevel();
        return new TreeOverlap.Terrain() {
            @Override
            public int surface(int x, int z) {
                return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            }

            @Override
            public WorldTreePart worldTree(int x, int y, int z) {
                return field != null && field.hasTree(x, z) ? field.treePart(x, y, z) : WorldTreePart.NONE;
            }

            @Override
            public boolean water(int x, int y, int z) {
                return y < seaLevel || y >= level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z)
                        && y < level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            }
        };
    }
}
