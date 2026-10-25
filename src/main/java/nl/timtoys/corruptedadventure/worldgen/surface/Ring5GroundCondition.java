package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5MissingChunks;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Terrain;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;

/**
 * Surface rule condition {@code corruptedadventure:ring_5_ground} (94/95, field {@code part}): what the pure ring 5 ground
 * ({@link Ring5World}) says about the block's column. Per part:
 * <ul>
 *   <li>{@code core}: ring 5 or the void edge beyond it (the cheap guard of the ring 5 rules);</li>
 *   <li>{@code missing_chunk}: the column lies in a clean missing chunk (94, V1-A; the rule makes its stone air);</li>
 *   <li>{@code rim}: a column with ground within its rim width (1–2, ragged per column) of the void (95, V8-C; the rule
 *   narrows it to the top block);</li>
 *   <li>{@code wall_top}: a column beside the void, its top 2–4 blocks (ragged per column) — the upper wall;</li>
 *   <li>{@code vein}: a column beside the void, a block of the wall below that, with a chance growing with depth;</li>
 *   <li>{@code wall}: a column beside the void (the wall itself);</li>
 *   <li>{@code castle}: ground of a castle site (plateau and ruined rim; V9-B);</li>
 *   <li>{@code pit}: a column on the floor of one of {@code corrupted_wastes}' pits (walk 1 review: the pit floors keep
 *   their sand where the open low ground takes it only in blobs);</li>
 *   <li>{@code inverted_hill}: a floe column with an upside-down hill under it ({@code inverted_hills}, V10-A), its lowest
 *   {@link #HILL_UNDERSIDE} blocks (not the ceiling of a void cave in the floe, 93);</li>
 *   <li>{@code hole_rim}, {@code hole_wall_top}, {@code hole_vein}, {@code hole_wall}: the same edge round a void cave's
 *   hole (93, V93-4 A with V8-C; INT3): the rock rim of its floor ({@link Ring5Field#holeRimTop}), the upper 2–4 blocks of
 *   the wall down the hole under it, a vein in the wall below that, the wall itself (2 wide round the hole);</li>
 *   <li>{@code upper}: a block within {@link #UPPER_DEPTH} under the column's pure top ({@link #UPPER_LAYERS} under a
 *   glitch field's shifted layers), or above it (cubes, spans; every block over the void): where the biomes' surfaces go.
 *   Not deeper: 93's caves start right under it, and their floors are no surface (INT3; vanilla's rule reaches about as
 *   deep). Vanilla's {@code above_preliminary_surface} interpolates the
 *   preliminary surface between the chunk's corners, and a corner over a void hole made whole chunk-aligned rectangles of
 *   ground beside the hole bare base rock.</li>
 * </ul>
 * Only in ring 5 and the void edge beyond it ({@link Ring5World#core}); never in the zone 4→5. Without a ring 5 field
 * (another dimension) false. The surface pass asks per block, column by column: the column's answers are shared per
 * thread ({@link #LAST}), so they are worked out once.
 */
public record Ring5GroundCondition(Part part) implements SurfaceRules.ConditionSource {
    /** The widest rim, so the distance scan stops there. */
    static final int MAX_RIM = 2;
    static final int MIN_WALL_TOP = 2;
    static final int MAX_WALL_TOP = 4;
    /** The voidstone veins of a wall: this chance just under the upper wall, growing by {@link #VEIN_RISE} per block deeper, at most {@link #VEIN_MAX}. */
    static final double VEIN_BASE = 0.04;
    static final double VEIN_RISE = 0.0025;
    static final double VEIN_MAX = 0.35;
    /** How deep under the column's top the biomes' surfaces reach ({@code upper}): the top block and the surface depth. */
    static final int UPPER_DEPTH = 8;
    /** The upside-down meadow under an inverted hill: the hill's lowest blocks (the rule's grass and two of dirt). */
    static final int HILL_UNDERSIDE = 3;
    /** The same under a glitch field's shifted layers, whose faces lie up to this far under the top. */
    static final int UPPER_LAYERS = Ring5Field.LAYER_DEPTH;

    public static final MapCodec<Ring5GroundCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Part.CODEC.fieldOf("part").forGetter(Ring5GroundCondition::part)
    ).apply(instance, Ring5GroundCondition::new));
    public static final KeyDispatchDataCodec<Ring5GroundCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);
    private static final ThreadLocal<Last> LAST = ThreadLocal.withInitial(Last::new);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        Ring5World world = Ring5Terrain.world(context.randomState);
        if (world == null) {
            return () -> false;
        }
        Last last = LAST.get();
        return () -> last.of(world, context.blockX, context.blockZ).test(part, context.blockY, world.salt(), context.blockX, context.blockZ);
    }

    /** A pure hash of a block (or a column, y = 0) in [0, 1). */
    static double unit(long seed, int x, int y, int z, long salt) {
        long h = RandomSupport.mixStafford13(seed ^ salt ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L));
        return (h >>> 11) * 0x1.0p-53;
    }

    /** The answers for one column. */
    record Column(boolean core, boolean missing, int voidDistance, int rim, int wallTop, int top, int upper, boolean castle,
                  int hillBottom, int holeRim, boolean pit) {
        static final Column OUTSIDE = new Column(false, false, Ring5World.FAR, 0, 0, Integer.MIN_VALUE, 0, false,
                Integer.MIN_VALUE, Integer.MIN_VALUE, false);

        static Column of(Ring5World world, int x, int z) {
            // The cached ring of the column first: rings 1-3 (most of the world) never ask the layout's noise again.
            Ring inner = RingColumnCache.ringAt(Level.OVERWORLD, x, z).inner();
            return inner != Ring.RING_4 && inner != Ring.RING_5 && inner != Ring.VOID ? OUTSIDE : ofWorld(world, x, z);
        }

        /** {@link #of} without the ring cache (which needs a running server): from the pure ground alone. */
        static Column ofWorld(Ring5World world, int x, int z) {
            if (!world.core(x, z)) {
                return OUTSIDE;
            }
            long seed = world.salt();
            boolean missing = Ring5MissingChunks.missing(world, x >> 4, z >> 4);
            int distance = world.voidDistance(x, z, MAX_RIM);
            int rim = 1 + (unit(seed, x, 0, z, 0x51DEL) < 0.5 ? 1 : 0);
            int wallTop = MIN_WALL_TOP + (int) (unit(seed, x, 0, z, 0x7A11L) * (MAX_WALL_TOP - MIN_WALL_TOP + 1));
            Ring5Field.Column column = world.field().column(x, z);
            boolean solid = distance > 0;
            return new Column(true, missing, distance, rim, wallTop, world.top(x, z),
                    column.layered() ? UPPER_LAYERS : UPPER_DEPTH, solid && column.castle() > 0.0,
                    solid && column.inverted() > 0.0 ? Mth.floor(column.bottom()) + 1 : Integer.MIN_VALUE,
                    world.field().holeRimTop(x, z), solid && world.field().pitFloor(x, z));
        }

        boolean test(Part part, int y, long seed, int x, int z) {
            if (!core) {
                return false;
            }
            return switch (part) {
                case CORE -> true;
                case MISSING_CHUNK -> missing;
                case RIM -> voidDistance > 0 && voidDistance <= rim;
                case WALL_TOP -> voidDistance == 1 && y > top - wallTop;
                case VEIN -> voidDistance == 1 && vein(seed, x, y, z, top - wallTop, 0xFE1AL);
                case WALL -> voidDistance == 1;
                case CASTLE -> castle;
                case PIT -> pit;
                case INVERTED_HILL -> hillBottom != Integer.MIN_VALUE && y < hillBottom + HILL_UNDERSIDE;
                case UPPER -> top == Integer.MIN_VALUE || y > top - upper;
                case HOLE_RIM -> holeRim != Integer.MIN_VALUE && y < holeRim && y >= holeWall();
                case HOLE_WALL_TOP -> holeRim != Integer.MIN_VALUE && y < holeWall() && y >= holeWall() - wallTop;
                case HOLE_VEIN -> holeRim != Integer.MIN_VALUE && y < holeWall() && vein(seed, x, y, z, holeWall() - wallTop, 0x401EL);
                case HOLE_WALL -> holeRim != Integer.MIN_VALUE && y < holeWall();
            };
        }

        /** The top (exclusive) of the wall down a void cave's hole: under its floor's rim. */
        private int holeWall() {
            return holeRim - Ring5Field.HOLE_RIM_DEPTH;
        }

        /** Whether a block of a wall beside the void is voidstone: a chance growing with its depth under {@code from}. */
        private static boolean vein(long seed, int x, int y, int z, int from, long salt) {
            return unit(seed, x, y, z, salt) < Math.min(VEIN_MAX, VEIN_BASE + VEIN_RISE * Math.max(0, from - y));
        }
    }

    /** The last column asked on this thread, by any part, of any world's field. */
    private static final class Last {
        private Ring5World world;
        private int x;
        private int z;
        private Column column;

        Column of(Ring5World world, int x, int z) {
            if (this.world != world || this.x != x || this.z != z || column == null) {
                this.world = world;
                this.x = x;
                this.z = z;
                column = Column.of(world, x, z);
            }
            return column;
        }
    }

    public enum Part implements StringRepresentable {
        CORE("core"),
        MISSING_CHUNK("missing_chunk"),
        RIM("rim"),
        WALL_TOP("wall_top"),
        VEIN("vein"),
        WALL("wall"),
        CASTLE("castle"),
        PIT("pit"),
        INVERTED_HILL("inverted_hill"),
        UPPER("upper"),
        HOLE_RIM("hole_rim"),
        HOLE_WALL_TOP("hole_wall_top"),
        HOLE_VEIN("hole_vein"),
        HOLE_WALL("hole_wall");

        public static final Codec<Part> CODEC = StringRepresentable.fromEnum(Part::values);
        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
