package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.RandomSupport;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5MissingChunks;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Terrain;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;

/**
 * Surface rule condition {@code corruptedadventure:ring_5_ground} (94, field {@code part}): what the pure ring 5 ground
 * ({@link Ring5World}) says about the block's column. Per part:
 * <ul>
 *   <li>{@code core}: ring 5 or the void edge beyond it (the cheap guard of the ring 5 rules);</li>
 *   <li>{@code missing_chunk}: the column lies in a clean missing chunk (94, V1-A; the rule makes its stone air).</li>
 * </ul>
 * Only in ring 5 and the void edge beyond it ({@link Ring5World#core}); never in the zone 4→5. Without a ring 5 field
 * (another dimension) false. The surface pass asks per block, column by column: the column's answers are shared per
 * thread ({@link #LAST}), so they are worked out once.
 */
public record Ring5GroundCondition(Part part) implements SurfaceRules.ConditionSource {
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
    record Column(boolean core, boolean missing) {
        static final Column OUTSIDE = new Column(false, false);

        static Column of(Ring5World world, int x, int z) {
            // The cached ring of the column first: rings 1-3 (most of the world) never ask the layout's noise again.
            Ring inner = RingColumnCache.ringAt(Level.OVERWORLD, x, z).inner();
            if (inner != Ring.RING_4 && inner != Ring.RING_5 && inner != Ring.VOID || !world.core(x, z)) {
                return OUTSIDE;
            }
            return new Column(true, Ring5MissingChunks.missing(world, x >> 4, z >> 4));
        }

        boolean test(Part part, int y, long seed, int x, int z) {
            if (!core) {
                return false;
            }
            return switch (part) {
                case CORE -> true;
                case MISSING_CHUNK -> missing;
            };
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
        MISSING_CHUNK("missing_chunk");

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
