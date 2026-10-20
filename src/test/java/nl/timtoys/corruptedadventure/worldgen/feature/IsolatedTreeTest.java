package nl.timtoys.corruptedadventure.worldgen.feature;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModTags;
import nl.timtoys.corruptedadventure.worldgen.WorldgenData;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.WorldTreePart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tree determinism (option A): what a ring 4 tree sees of another tree's blocks, the merge where two trees want one
 * block, the tree's own random, and that every ring 4 tree is an {@code isolated_tree}. Unit tests have no datapack:
 * the tags the rule reads are bound by hand with the blocks used here. The order independence of the whole is checked
 * in the world ({@code det.sh}).
 */
class IsolatedTreeTest {
    private static final int SEA = 63;
    private static final String GENERATED = "src/generated/resources/data/corruptedadventure/worldgen/configured_feature/ring_4";

    @BeforeEach
    void bindTags() {
        BuiltInRegistries.BLOCK.bindTags(Map.of(
                BlockTags.LOGS, holders(Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG, ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get()),
                ModTags.Blocks.MOONPETAL_LOGS, holders(ModBlocks.MOONPETAL_LOG.get(), ModBlocks.MOONPETAL_WOOD.get()),
                BlockTags.LEAVES, holders(Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES, ModBlocks.MOONPETAL_LEAVES.get()),
                BlockTags.DIRT, holders(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS)));
    }

    @AfterEach
    void resetTags() {
        BuiltInRegistries.BLOCK.resetTags();
    }

    @Test
    void anotherTreesBlocksLookLikeTheTerrainTheyTook() {
        BlockPos low = new BlockPos(0, 60, 0);
        BlockPos high = new BlockPos(0, 70, 0);
        BlockState log = Blocks.MANGROVE_LOG.defaultBlockState();
        assertEquals(Blocks.WATER.defaultBlockState(), terrain(log, low), "a trunk below the sea level stood in the sea");
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(log, high));
        assertEquals(Blocks.WATER.defaultBlockState(), terrain(log, new BlockPos(0, 82, 0)), "a trunk in a lake");
        BlockState updraft = ModBlocks.UPDRAFT.get().defaultBlockState();
        assertEquals(Blocks.WATER.defaultBlockState(), terrain(updraft, low), "a lift column's foot in the sea");
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(updraft, high));
        BlockState leaves = Blocks.MANGROVE_LEAVES.defaultBlockState();
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(leaves, high));
        assertEquals(Blocks.WATER.defaultBlockState(), terrain(leaves.setValue(BlockStateProperties.WATERLOGGED, true), low));
        assertEquals(Blocks.MUD.defaultBlockState(), terrain(Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState(), low));
        assertEquals(Blocks.WATER.defaultBlockState(),
                terrain(Blocks.MANGROVE_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true), low));
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(Blocks.MANGROVE_ROOTS.defaultBlockState(), high));
        BlockState propagule = Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true);
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(propagule, high));
        // Ground plants (also the neighbours' features) are open space; terrain stays.
        for (Block plant : List.of(Blocks.SHORT_GRASS, Blocks.MOSS_CARPET, Blocks.ALLIUM, ModBlocks.GLOWCAP.get())) {
            assertEquals(Blocks.AIR.defaultBlockState(), terrain(plant.defaultBlockState(), high), plant.toString());
        }
        assertEquals(Blocks.WATER.defaultBlockState(), terrain(Blocks.SEAGRASS.defaultBlockState(), low));
        for (Block ground : List.of(Blocks.STONE, Blocks.DIRT, Blocks.MUD, Blocks.GRASS_BLOCK)) {
            assertEquals(ground.defaultBlockState(), terrain(ground.defaultBlockState(), high), ground.toString());
        }
    }

    @Test
    void groundObjectsAreOpenSpaceToATreeAndNeverGiveWay() {
        // Decor proef: the ground under Y 70 everywhere; a boulder's stone from Y 70 up is an object.
        TreeOverlap.Terrain ground = new TreeOverlap.Terrain() {
            @Override
            public WorldTreePart worldTree(int x, int y, int z) {
                return WorldTreePart.NONE;
            }

            @Override
            public boolean water(int x, int y, int z) {
                return y < SEA;
            }

            @Override
            public int surface(int x, int z) {
                return 70;
            }
        };
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockPos onTop = new BlockPos(3, 70, 4);
        BlockPos sunk = new BlockPos(3, 69, 4);
        assertTrue(TreeOverlap.object(stone, onTop, ground));
        assertFalse(TreeOverlap.object(stone, sunk, ground), "the boulder's sunk foot is ground");
        assertEquals(Blocks.AIR.defaultBlockState(), TreeOverlap.terrain(stone, onTop, ground), "a tree grows as if the rock were not there");
        assertEquals(stone, TreeOverlap.terrain(stone, sunk, ground));
        // Wood, leaves and plants above the surface are no objects: they keep the trees' priority.
        assertFalse(TreeOverlap.object(Blocks.SPRUCE_LOG.defaultBlockState(), onTop, ground));
        assertFalse(TreeOverlap.object(Blocks.SPRUCE_LEAVES.defaultBlockState(), onTop, ground));
        assertFalse(TreeOverlap.object(Blocks.MOSS_CARPET.defaultBlockState(), onTop, ground));
        // Without a heightmap (the default) nothing is an object.
        assertFalse(TreeOverlap.object(stone, onTop, world(WorldTreePart.NONE)));
    }

    @Test
    void theWorldTreeIsTerrain() {
        BlockPos pos = new BlockPos(5, 120, 5);
        BlockState log = ModBlocks.MOONPETAL_LOG.get().defaultBlockState();
        assertEquals(log, TreeOverlap.terrain(log, pos, world(WorldTreePart.LOG)));
        assertEquals(Blocks.AIR.defaultBlockState(), terrain(log, pos), "a moonpetal tree's trunk");
        // Another tree's leaf or root in the world tree's wood or crown stands for that block.
        assertEquals(ModBlocks.MOONPETAL_WOOD.get().defaultBlockState(),
                TreeOverlap.terrain(Blocks.MANGROVE_ROOTS.defaultBlockState(), pos, world(WorldTreePart.WOOD)));
        BlockState persistent = ModBlocks.MOONPETAL_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        assertEquals(persistent, terrain(persistent, pos));
        // A moonpetal tree's leaf in the world tree's crown took a crown leaf.
        BlockState leaf = ModBlocks.MOONPETAL_LEAVES.get().defaultBlockState();
        assertEquals(persistent, TreeOverlap.terrain(leaf, pos, world(WorldTreePart.LEAVES)));
        assertEquals(persistent, TreeOverlap.terrain(Blocks.MANGROVE_LOG.defaultBlockState(), pos, world(WorldTreePart.LEAVES)));
        assertEquals(TreeOverlap.Rank.NONE, TreeOverlap.existing(persistent, Blocks.AIR.defaultBlockState()), "a tree may grow into the world tree's crown");
    }

    @Test
    void priorityTrunkRootLeafDecor() {
        BlockState log = Blocks.MANGROVE_LOG.defaultBlockState();
        BlockState root = Blocks.MANGROVE_ROOTS.defaultBlockState();
        BlockState leaf = Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 2);
        BlockState moss = Blocks.MOSS_CARPET.defaultBlockState();
        assertEquals(TreeOverlap.Rank.TRUNK, TreeOverlap.planned(log));
        assertEquals(TreeOverlap.Rank.ROOT, TreeOverlap.planned(Blocks.MUDDY_MANGROVE_ROOTS.defaultBlockState()));
        assertEquals(TreeOverlap.Rank.LEAF, TreeOverlap.planned(leaf));
        assertEquals(TreeOverlap.Rank.DECOR, TreeOverlap.planned(moss));
        assertTrue(wins(log, root));
        assertFalse(wins(root, log));
        assertTrue(wins(root, leaf));
        assertFalse(wins(leaf, root));
        assertTrue(wins(leaf, moss), "a leaf takes a propagule's, moss' or glowcap's place");
        assertFalse(wins(moss, leaf));
        // Terrain and plants always give way: the tree planned the block against the terrain alone.
        assertTrue(wins(moss, Blocks.SHORT_GRASS.defaultBlockState()));
        assertTrue(wins(root, Blocks.MUD.defaultBlockState()));
        // A lift column always stays; the dirt under a trunk takes only terrain.
        assertFalse(wins(log, ModBlocks.UPDRAFT.get().defaultBlockState()));
        assertEquals(TreeOverlap.Rank.GROUND, TreeOverlap.planned(Blocks.DIRT.defaultBlockState()));
        assertFalse(wins(Blocks.DIRT.defaultBlockState(), leaf));
        assertTrue(wins(leaf, Blocks.DIRT.defaultBlockState()));
    }

    /** Two trees' blocks at one place: the same block stays, whichever tree came first. */
    @Test
    void theMergeDoesNotDependOnTheOrder() {
        List<BlockState> blocks = new ArrayList<>();
        for (Block block : List.of(Blocks.MANGROVE_LOG, Blocks.SPRUCE_LOG, ModBlocks.MOONPETAL_LOG.get())) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                blocks.add(block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
        for (Block block : List.of(Blocks.MANGROVE_ROOTS, Blocks.MUDDY_MANGROVE_ROOTS)) {
            blocks.add(block.defaultBlockState());
        }
        blocks.add(Blocks.MANGROVE_ROOTS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true));
        for (int age = 0; age <= 2; age++) {
            blocks.add(ModBlocks.VERDANT_FRUIT_POD.get().defaultBlockState().setValue(CocoaBlock.AGE, age));
        }
        for (Block block : List.of(Blocks.MANGROVE_LEAVES, Blocks.SPRUCE_LEAVES, ModBlocks.MOONPETAL_LEAVES.get())) {
            for (int distance = 1; distance <= 7; distance += 3) {
                blocks.add(block.defaultBlockState().setValue(LeavesBlock.DISTANCE, distance));
                blocks.add(block.defaultBlockState().setValue(LeavesBlock.DISTANCE, distance).setValue(BlockStateProperties.WATERLOGGED, true));
            }
        }
        for (BlockState a : blocks) {
            for (BlockState b : blocks) {
                if (a == b) {
                    continue;
                }
                BlockState aThenB = wins(b, a) ? b : a;
                BlockState bThenA = wins(a, b) ? a : b;
                assertEquals(aThenB, bThenA, a + " / " + b);
            }
        }
        // Two trees' moss, glowcap or propagule: a hanging propagule, or moss/glowcap on a mangrove root, is a tree's.
        BlockState root = Blocks.MANGROVE_ROOTS.defaultBlockState();
        BlockState glowcap = ModBlocks.GLOWCAP.get().defaultBlockState();
        BlockState propagule = Blocks.MANGROVE_PROPAGULE.defaultBlockState().setValue(MangrovePropaguleBlock.HANGING, true);
        BlockState capThenPropagule = TreeOverlap.wins(propagule, glowcap, root) ? propagule : glowcap;
        BlockState propaguleThenCap = TreeOverlap.wins(glowcap, propagule, Blocks.AIR.defaultBlockState()) ? glowcap : propagule;
        assertEquals(capThenPropagule, propaguleThenCap);
        assertEquals(TreeOverlap.Rank.NONE, TreeOverlap.existing(glowcap, Blocks.MUD.defaultBlockState()), "a ground glowcap");
        // Decor A: a swamp tree's strand of moss or glowing root against another tree's propagule, in either order.
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BlockState strand : List.of(ModBlocks.HANGING_SWAMP_MOSS.get().defaultBlockState(),
                ModBlocks.GLOWING_HANGING_ROOTS.get().defaultBlockState())) {
            assertEquals(TreeOverlap.Rank.NONE, TreeOverlap.existing(strand, air), "the island strands hang there too");
            assertEquals(TreeOverlap.Rank.GROUND, TreeOverlap.planned(strand));
            BlockState strandThenPropagule = TreeOverlap.wins(propagule, strand, air) ? propagule : strand;
            BlockState propaguleThenStrand = TreeOverlap.wins(strand, propagule, air) ? strand : propagule;
            assertEquals(strandThenPropagule, propaguleThenStrand, strand.toString());
        }
        BlockState near = Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 1);
        BlockState far = Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE, 5);
        assertTrue(wins(near, far), "the leaf closer to a log stays");
    }

    @Test
    void everyTreeHasItsOwnRandom() {
        long seed = 20260930L;
        BlockPos pos = new BlockPos(-7021, 66, 13);
        long[] first = draws(IsolatedTreeFeature.random(seed, pos));
        assertTrue(Arrays.equals(first, draws(IsolatedTreeFeature.random(seed, pos))), "the same tree in the same world");
        assertFalse(Arrays.equals(first, draws(IsolatedTreeFeature.random(seed, pos.east()))));
        assertFalse(Arrays.equals(first, draws(IsolatedTreeFeature.random(seed, pos.above()))));
        assertFalse(Arrays.equals(first, draws(IsolatedTreeFeature.random(seed + 1, pos))));
        assertNotEquals(0, first[0]);
    }

    /**
     * Every tree of ring 4 is an isolated tree (the fjords' vanilla spruce, pine and dark oak as copies), and the
     * weighted ground plants are ground plants.
     */
    @Test
    void ring4TreesAreIsolatedTrees() throws IOException {
        Path dir = generated();
        for (String tree : List.of("moonpetal_tree", "swamp_tree", "fjord_spruce", "fjord_pine", "fjord_dark_oak",
                "withered_moonpetal_tree", "withered_moonpetal_tree_forked", "verdant_emergent_tree", "verdant_tree_fern")) {
            JsonObject json = WorldgenData.read(dir.resolve(tree + ".json"));
            assertEquals("corruptedadventure:isolated_tree", json.get("type").getAsString(), tree);
        }
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                assertFalse(WorldgenData.read(file).toString().contains("\"minecraft:tree\""), file + " uses vanilla's tree");
            }
        }
        // The weighted ground plants always draw, then fill only open air: a tree on the spot shifts no later plant.
        for (String plant : List.of("meadow_grass", "fjord_ferns", "sky_flowers", "dead_bush")) {
            assertEquals("corruptedadventure:ground_plant", WorldgenData.read(dir.resolve(plant + ".json")).get("type").getAsString(), plant);
        }
        String fjord = WorldgenData.read(dir.resolve("fjord_tree.json")).toString();
        assertFalse(fjord.contains("minecraft:spruce") || fjord.contains("minecraft:pine") || fjord.contains("minecraft:dark_oak\""),
                "the fjords' selector names the isolated copies");
    }

    private static boolean wins(BlockState ours, BlockState existing) {
        return TreeOverlap.wins(ours, existing, Blocks.AIR.defaultBlockState());
    }

    private static BlockState terrain(BlockState state, BlockPos pos) {
        return TreeOverlap.terrain(state, pos, world(WorldTreePart.NONE));
    }

    /** The terrain: the sea below {@link #SEA}, a lake from Y 80 to 83, the world tree's {@code part} everywhere. */
    private static TreeOverlap.Terrain world(WorldTreePart part) {
        return new TreeOverlap.Terrain() {
            @Override
            public WorldTreePart worldTree(int x, int y, int z) {
                return part;
            }

            @Override
            public boolean water(int x, int y, int z) {
                return y < SEA || y >= 80 && y < 84;
            }
        };
    }

    private static long[] draws(RandomSource random) {
        long[] values = new long[8];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextLong();
        }
        return values;
    }

    private static Path generated() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            if (Files.isDirectory(dir.resolve(GENERATED))) {
                return dir.resolve(GENERATED);
            }
        }
        throw new IllegalStateException("Could not find " + GENERATED);
    }

    private static List<Holder<Block>> holders(Block... blocks) {
        return Arrays.stream(blocks).map(BuiltInRegistries.BLOCK::wrapAsHolder).toList();
    }
}
