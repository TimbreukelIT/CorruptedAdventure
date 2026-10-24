package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The memories of ring 5 (94, V7-A): rare warped pieces of rings 1–4 swallowed by the corruption (a bit of oak wood, an
 * oasis with a palm, a field of set lava, an islet with a hole), {@link #MIN_RADIUS}–{@link #MAX_RADIUS} blocks round, half
 * of each turned grey from one side. In the inner band they lie on the ground (often tilted up to {@link #MAX_TILT}°, a
 * few hovering over it); in the outer band they float over the void.
 * <p>
 * One memory at most per world cell of {@link #CELL} blocks, a pure plan ({@link #memory}) from the world seed, the cell
 * and the pure terrain ({@link Ring5World}); its blocks ({@link #blocks}) are pure too, so every chunk writes its own part
 * ({@link MemoryFeature}) and a memory across chunk borders is whole whatever generated first. No block of a memory is an
 * ore's target (no stone, no corrupted stone): the ores of a neighbouring chunk never change one. The plans and shapes are
 * remembered per {@link Ring5World} (by identity, weakly), so two worlds of one seed in one session never share them.
 */
public final class Ring5Memories {
    static final int CELL = 560;
    static final int MARGIN = 48;
    static final double CHANCE = 0.9;
    static final int MIN_RADIUS = 5;
    static final int MAX_RADIUS = 15;
    static final double TILT_CHANCE = 0.5;
    static final double MIN_TILT = 10.0;
    static final double MAX_TILT = 25.0;
    /** In the inner band: this share hovers over the ground instead of lying on it. */
    static final double HOVER_CHANCE = 0.15;
    static final int MIN_HOVER = 6;
    static final int MAX_HOVER = 14;
    /** Over the void (outer band): the top soil between these heights. */
    static final int MIN_FLOAT_Y = 60;
    static final int MAX_FLOAT_Y = 100;
    /** The ground under a lying memory varies by at most this much (else it is not placed). */
    static final int MAX_RELIEF = 16;
    /** Over the void: this share of the candidates is kept (so about a third of all memories float). */
    static final double VOID_KEEP = 0.4;
    /** A lying memory's skirt reaches down to the ground over at most this many blocks. */
    static final int MAX_SKIRT = 20;
    /** How far a memory keeps from a castle site's belt, and its decorations reach beyond its radius. */
    static final int CASTLE_CLEARANCE = 24;
    static final int REACH = 3;
    /** How far a memory's ragged edge reaches beyond its radius, as a share of it (the sum of the edge's waves). */
    static final double WOBBLE = 0.26;
    /**
     * How far the ground objects that move into their chunk ({@link GroundObjects#fit}: rocks, dead trees, torch patterns)
     * keep from a memory where they are asked: their reach and the move together.
     */
    static final int OBJECT_CLEARANCE = 8;
    /** Blocks a memory keeps from ring 5's boundaries beyond its reach (a biome's quart corner and a chunk's 3 x 3). */
    static final int RING_MARGIN = 32;
    private static final long SALT = 0x3E30_41E5L;

    private static final Ring5PlanCache<Long, Memory> PLANS = new Ring5PlanCache<>(4096);
    private static final Ring5PlanCache<Memory, Shape> SHAPES = new Ring5PlanCache<>(32);

    private Ring5Memories() {
    }

    public enum Kind implements StringRepresentable {
        OAK_GROVE("oak_grove"),
        OASIS("oasis"),
        LAVA_FIELD("lava_field"),
        HOLED_ISLET("holed_islet");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);
        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    /**
     * One memory: its kind, middle, radius, the Y of its top soil's middle, whether it floats (over the void or hovering)
     * and its tilt (radians, towards {@code tiltDirection}); the corruption comes from {@code corruptDirection}.
     */
    public record Memory(Kind kind, int x, int z, int radius, int baseY, boolean floating, double tilt, double tiltDirection,
                         double corruptDirection, long shapeSeed) {
        /** How far (blocks) the memory's blocks reach from its middle: the ragged edge and what grows on it. */
        int extent() {
            return extent(radius);
        }

        static int extent(int radius) {
            return (int) Math.ceil(radius * (1.0 + WOBBLE)) + REACH;
        }

        boolean touches(int minX, int minZ, int maxX, int maxZ, int margin) {
            int r = extent() + margin;
            return x + r >= minX && x - r <= maxX && z + r >= minZ && z - r <= maxZ;
        }
    }

    /** One block of a memory: its ground (a piece of terrain) or what grows on it (trees and plants: only into the open). */
    record Placed(BlockPos pos, BlockState state, boolean ground) {
    }

    /** A memory's blocks, per chunk ({@link ChunkPos#toLong}), the ground first. */
    record Shape(Map<Long, List<Placed>> chunks) {
        List<Placed> in(ChunkPos chunk) {
            return chunks.getOrDefault(chunk.toLong(), List.of());
        }
    }

    /** The memories whose blocks may reach into the box (blocks, inclusive). */
    static List<Memory> within(Ring5World world, int minX, int minZ, int maxX, int maxZ, int margin) {
        int reach = Memory.extent(MAX_RADIUS) + margin;
        List<Memory> found = new ArrayList<>();
        for (int i = Math.floorDiv(minX - reach, CELL); i <= Math.floorDiv(maxX + reach, CELL); i++) {
            for (int j = Math.floorDiv(minZ - reach, CELL); j <= Math.floorDiv(maxZ + reach, CELL); j++) {
                Memory memory = cached(world, i, j);
                if (memory != null && memory.touches(minX, minZ, maxX, maxZ, margin)) {
                    found.add(memory);
                }
            }
        }
        return found;
    }

    /** Whether a memory lies within {@code margin} blocks (beyond its radius) of (x, z). */
    static boolean near(Ring5World world, int x, int z, int margin) {
        for (Memory memory : within(world, x, z, x, z, margin)) {
            if (Math.hypot(x - memory.x(), z - memory.z()) <= memory.extent() + margin) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static Memory cached(Ring5World world, int i, int j) {
        return PLANS.get(world, ChunkPos.asLong(i, j), k -> memory(world, i, j));
    }

    /** The memory of cell (i, j), or null. Pure in the world and the cell. */
    @Nullable
    static Memory memory(Ring5World world, int i, int j) {
        Ring5Field field = world.field();
        RandomSource r = new XoroshiroRandomSource(RandomSupport.mixStafford13(world.salt() ^ SALT),
                RandomSupport.mixStafford13(((long) i << 32) ^ (j & 0xFFFFFFFFL)));
        if (r.nextDouble() >= CHANCE) {
            return null;
        }
        int x = i * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        int z = j * CELL + MARGIN + r.nextInt(CELL - 2 * MARGIN);
        Kind kind = Kind.values()[r.nextInt(Kind.values().length)];
        int radius = MIN_RADIUS + r.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        double tilt = r.nextDouble() < TILT_CHANCE ? Math.toRadians(MIN_TILT + r.nextDouble() * (MAX_TILT - MIN_TILT)) : 0.0;
        double tiltDirection = r.nextDouble() * Math.PI * 2.0;
        double corruptDirection = r.nextDouble() * Math.PI * 2.0;
        boolean hover = r.nextDouble() < HOVER_CHANCE;
        int hoverBy = MIN_HOVER + r.nextInt(MAX_HOVER - MIN_HOVER + 1);
        int floatY = MIN_FLOAT_Y + r.nextInt(MAX_FLOAT_Y - MIN_FLOAT_Y + 1);
        long shapeSeed = r.nextLong();
        boolean keepOverVoid = r.nextDouble() < VOID_KEEP;
        // Wholly inside ring 5's biomes (whose chunks write the memory), clear of the zone 4->5 and the void edge.
        if (!world.core(x, z) || world.ringDepth(x, z) < Memory.extent(radius) + RING_MARGIN) {
            return null;
        }
        for (Ring5Field.CastleSite site : field.castleSites()) {
            if (site.distance(x, z) < site.radius() + site.belt() + radius + CASTLE_CLEARANCE) {
                return null;
            }
        }
        int reach = radius + 2;
        int solid = 0;
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int k = -1; k < 8; k++) {
            int sx = k < 0 ? x : x + (int) Math.round(reach * Math.cos(Math.PI / 4.0 * k));
            int sz = k < 0 ? z : z + (int) Math.round(reach * Math.sin(Math.PI / 4.0 * k));
            if (!world.core(sx, sz)) {
                return null;
            }
            if (!world.isVoid(sx, sz)) {
                if (world.openTop(sx, sz)) {
                    // A net's shaft or a corridor through the slope (93): the ground is not there.
                    return null;
                }
                solid++;
                int top = world.top(sx, sz);
                lowest = Math.min(lowest, top);
                highest = Math.max(highest, top);
            }
        }
        if (solid == 9) {
            // On the ground (inner band or a floe): level enough, no missing chunk under it.
            if (highest - lowest > MAX_RELIEF || overMissingChunk(world, x, z, reach)) {
                return null;
            }
            int ground = world.top(x, z);
            return new Memory(kind, x, z, radius, hover ? ground + 1 + hoverBy : ground, hover, tilt, tiltDirection,
                    corruptDirection, shapeSeed);
        }
        if (solid == 0 && keepOverVoid && field.bridgesWithin(x - reach, z - reach, x + reach, z + reach).isEmpty()
                && field.fragmentsWithin(x - reach, z - reach, x + reach, z + reach).isEmpty()) {
            // Over the void, clear of every bridge and floating chunk.
            return new Memory(kind, x, z, radius, floatY, true, tilt, tiltDirection, corruptDirection, shapeSeed);
        }
        return null;
    }

    private static boolean overMissingChunk(Ring5World world, int x, int z, int reach) {
        for (int cx = (x - reach) >> 4; cx <= (x + reach) >> 4; cx++) {
            for (int cz = (z - reach) >> 4; cz <= (z + reach) >> 4; cz++) {
                if (Ring5MissingChunks.missing(world, cx, cz)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The blocks of a memory per chunk. Pure. */
    static Shape shape(Memory memory, Ring5World world) {
        return SHAPES.get(world, memory, m -> build(m, world));
    }

    /** All blocks of a memory (world positions), the ground first, then what grows on it. Pure. */
    static Map<BlockPos, BlockState> blocks(Memory memory, Ring5World world) {
        Map<BlockPos, BlockState> all = new LinkedHashMap<>();
        shape(memory, world).chunks().values().forEach(part -> part.forEach(placed -> all.put(placed.pos(), placed.state())));
        return all;
    }

    private static Shape build(Memory m, Ring5World world) {
        RandomSource random = new XoroshiroRandomSource(m.shapeSeed());
        Palette palette = Palette.of(m.kind());
        int r = m.radius();
        double p1 = random.nextDouble() * Math.PI * 2.0;
        double p2 = random.nextDouble() * Math.PI * 2.0;
        double p3 = random.nextDouble() * Math.PI * 2.0;
        int thickness = 3 + random.nextInt(4);
        double slope = Math.tan(m.tilt());
        double tx = Math.cos(m.tiltDirection());
        double tz = Math.sin(m.tiltDirection());
        Map<BlockPos, BlockState> ground = new LinkedHashMap<>();
        Map<BlockPos, BlockState> growth = new LinkedHashMap<>();
        List<int[]> tops = new ArrayList<>();
        for (int lx = -r - 2; lx <= r + 2; lx++) {
            for (int lz = -r - 2; lz <= r + 2; lz++) {
                double d = Math.hypot(lx, lz);
                double angle = Math.atan2(lz, lx);
                double edge = r * (1.0 + 0.12 * Math.sin(2 * angle + p1) + 0.08 * Math.sin(3 * angle + p2) + 0.06 * Math.sin(5 * angle + p3));
                if (d > edge || (m.kind() == Kind.HOLED_ISLET && d < r * 0.3)) {
                    continue;
                }
                boolean corrupt = corrupted(m, lx, lz, random);
                int shift = (int) Math.round(slope * (lx * tx + lz * tz));
                int wx = m.x() + lx;
                int wz = m.z() + lz;
                int topY = m.baseY() + shift;
                boolean basin = m.kind() == Kind.OASIS && d < r * 0.35;
                int depth = m.floating() ? 1 + (int) Math.round((1.0 - (d / edge) * (d / edge)) * thickness) : 2;
                int surface = basin ? topY - 1 : topY;
                for (int k = 0; k < depth; k++) {
                    int y = surface - k;
                    BlockState state = k == 0 ? palette.top(random, basin) : k == depth - 1 && m.floating() ? palette.bottom : palette.body(random);
                    if (m.floating() && k == depth - 1 && state.getBlock() instanceof FallingBlock) {
                        // The thin rim of a floating memory: sand there would drop into the air at the first update.
                        state = palette.bottom;
                    }
                    ground.put(new BlockPos(wx, y, wz), corrupt ? corrupt(state) : state);
                }
                if (!m.floating()) {
                    // The skirt down to the pure ground, so a lying memory never hangs over it.
                    int below = world.top(wx, wz);
                    int lowest = surface - depth + 1;
                    if (below != Integer.MIN_VALUE && lowest - below <= MAX_SKIRT) {
                        for (int y = below + 1; y < lowest; y++) {
                            ground.put(new BlockPos(wx, y, wz), corrupt ? corrupt(palette.body(random)) : palette.body(random));
                        }
                    }
                }
                if (!basin) {
                    tops.add(new int[]{wx, surface, wz, corrupt ? 1 : 0, lx, lz});
                }
            }
        }
        decorate(m, palette, random, tops, ground, growth);
        Map<Long, List<Placed>> chunks = new LinkedHashMap<>();
        ground.forEach((pos, state) -> chunks.computeIfAbsent(ChunkPos.asLong(pos), k -> new ArrayList<>())
                .add(new Placed(pos, state, true)));
        growth.forEach((pos, state) -> {
            if (!ground.containsKey(pos)) {
                chunks.computeIfAbsent(ChunkPos.asLong(pos), k -> new ArrayList<>()).add(new Placed(pos, state, false));
            }
        });
        chunks.replaceAll((k, part) -> List.copyOf(part));
        return new Shape(Collections.unmodifiableMap(chunks));
    }

    /** What grows on a memory: trees (oak grove), a palm (oasis), plants by its kind; grey and dead where it is corrupted. */
    private static void decorate(Memory m, Palette palette, RandomSource random, List<int[]> tops, Map<BlockPos, BlockState> ground,
                                 Map<BlockPos, BlockState> growth) {
        if (tops.isEmpty()) {
            return;
        }
        int trees = switch (m.kind()) {
            case OAK_GROVE -> 2 + random.nextInt(3);
            case OASIS -> 1;
            default -> 0;
        };
        for (int t = 0; t < trees; t++) {
            int[] at = tops.get(random.nextInt(tops.size()));
            if (Math.hypot(at[4], at[5]) > m.radius() - 2) {
                continue;
            }
            boolean corrupt = at[3] == 1;
            List<int[]> shape = m.kind() == Kind.OASIS ? palm(random) : MirroredTreesFeature.oak(random);
            for (int[] b : shape) {
                BlockState state = b[3] == 1 ? palette.log : palette.leaves;
                BlockPos pos = new BlockPos(at[0] + b[0], at[1] + 1 + b[1], at[2] + b[2]);
                if (!ground.containsKey(pos)) {
                    growth.putIfAbsent(pos, corrupt ? corrupt(state) : state);
                }
            }
        }
        for (int[] at : tops) {
            BlockPos pos = new BlockPos(at[0], at[1] + 1, at[2]);
            BlockState plant = palette.plant(random);
            if (plant != null && !ground.containsKey(pos)) {
                growth.putIfAbsent(pos, at[3] == 1 ? Blocks.DEAD_BUSH.defaultBlockState() : plant);
            }
        }
    }

    /** Whether (lx, lz) of the memory lies on its grey half: past the line through its middle, with a ragged edge. */
    private static boolean corrupted(Memory m, int lx, int lz, RandomSource random) {
        double along = (lx * Math.cos(m.corruptDirection()) + lz * Math.sin(m.corruptDirection())) / m.radius();
        return along + 0.35 * (random.nextDouble() - 0.5) > 0.0;
    }

    /** A palm: a trunk 5–7 high and eight fronds that droop at their ends. {@code {dx, dy, dz, isLog}}, logs first. */
    static List<int[]> palm(RandomSource random) {
        int height = 5 + random.nextInt(3);
        List<int[]> blocks = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            blocks.add(new int[]{0, y, 0, 1});
        }
        blocks.add(new int[]{0, height, 0, 0});
        int[][] directions = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};
        for (int[] d : directions) {
            int length = d[0] != 0 && d[1] != 0 ? 2 : 3;
            for (int k = 1; k <= length; k++) {
                blocks.add(new int[]{d[0] * k, k == length ? height - 2 : height - 1, d[1] * k, 0});
            }
        }
        return blocks;
    }

    /** The grey counterpart of a memory's block: the corrupted variants, never stone (no ore's target). */
    static BlockState corrupt(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK)) {
            return ModBlocks.CORRUPTED_GRASS_BLOCK.get().defaultBlockState();
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)) {
            return ModBlocks.CORRUPTED_DIRT.get().defaultBlockState();
        }
        if (state.is(Blocks.SAND)) {
            return ModBlocks.CORRUPTED_SAND.get().defaultBlockState();
        }
        if (state.getBlock() instanceof LeavesBlock) {
            return ModBlocks.CORRUPTED_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        }
        if (state.is(Blocks.OAK_LOG) || state.is(ModBlocks.PALM_LOG.get())) {
            return ModBlocks.CORRUPTED_LOG.get().defaultBlockState();
        }
        if (state.getBlock() instanceof BushBlock) {
            return Blocks.DEAD_BUSH.defaultBlockState();
        }
        return ModBlocks.CORRUPTED_COBBLESTONE.get().defaultBlockState();
    }

    /** The blocks of one kind. */
    private record Palette(BlockState top, BlockState basin, List<BlockState> bodies, BlockState bottom, BlockState log,
                           BlockState leaves, List<BlockState> plants, float plantChance, List<BlockState> topSpecks,
                           float speckChance) {
        static Palette of(Kind kind) {
            BlockState none = Blocks.AIR.defaultBlockState();
            return switch (kind) {
                case OAK_GROVE -> new Palette(Blocks.GRASS_BLOCK.defaultBlockState(), none, List.of(Blocks.DIRT.defaultBlockState()),
                        Blocks.COARSE_DIRT.defaultBlockState(), Blocks.OAK_LOG.defaultBlockState(),
                        Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                        List.of(Blocks.SHORT_GRASS.defaultBlockState(), Blocks.SHORT_GRASS.defaultBlockState(), Blocks.FERN.defaultBlockState(),
                                Blocks.DANDELION.defaultBlockState(), Blocks.POPPY.defaultBlockState()), 0.35F, List.of(), 0.0F);
                case OASIS -> new Palette(Blocks.SAND.defaultBlockState(), ModBlocks.CRACKED_CLAY.get().defaultBlockState(),
                        List.of(Blocks.SANDSTONE.defaultBlockState()), Blocks.SANDSTONE.defaultBlockState(),
                        ModBlocks.PALM_LOG.get().defaultBlockState(),
                        ModBlocks.PALM_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true),
                        List.of(ModBlocks.DRY_SHRUB.get().defaultBlockState(), Blocks.DEAD_BUSH.defaultBlockState()), 0.06F, List.of(), 0.0F);
                case LAVA_FIELD -> new Palette(ModBlocks.COLD_LAVA.get().defaultBlockState(), none,
                        List.of(Blocks.BASALT.defaultBlockState(), Blocks.SMOOTH_BASALT.defaultBlockState()), Blocks.BLACKSTONE.defaultBlockState(),
                        none, none, List.of(), 0.0F,
                        List.of(Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.BASALT.defaultBlockState(), ModBlocks.ASH_BLOCK.get().defaultBlockState(),
                                ModBlocks.ASH_BLOCK.get().defaultBlockState()), 0.2F);
                case HOLED_ISLET -> new Palette(Blocks.GRASS_BLOCK.defaultBlockState(), none,
                        List.of(ModBlocks.TIDESTONE.get().defaultBlockState(), ModBlocks.TIDESTONE.get().defaultBlockState(),
                                ModBlocks.MOSSY_TIDESTONE.get().defaultBlockState()), ModBlocks.COBBLED_TIDESTONE.get().defaultBlockState(),
                        none, none, List.of(ModBlocks.FJORD_MOSS.get().defaultBlockState(), ModBlocks.FJORD_MOSS.get().defaultBlockState(),
                        ModBlocks.MOONPETAL_SHRUB.get().defaultBlockState(), ModBlocks.MIST_FERN.get().defaultBlockState()), 0.3F, List.of(), 0.0F);
            };
        }

        BlockState top(RandomSource random, boolean inBasin) {
            if (inBasin) {
                return basin;
            }
            if (!topSpecks.isEmpty() && random.nextFloat() < speckChance) {
                return topSpecks.get(random.nextInt(topSpecks.size()));
            }
            return top;
        }

        BlockState body(RandomSource random) {
            return bodies.get(random.nextInt(bodies.size()));
        }

        @Nullable
        BlockState plant(RandomSource random) {
            if (plants.isEmpty() || random.nextFloat() >= plantChance) {
                return null;
            }
            return plants.get(random.nextInt(plants.size()));
        }
    }
}
