package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Terrain;

/**
 * Surface rule condition {@code corruptedadventure:zone_4_5_share} (walk 1, question 2 = A): true where the column lies in
 * the zone 4→5 at least {@code from} of the way across (the zone's {@link RingPosition#blend}: 0 = ring 4's side, 1 =
 * ring 5's side) or beyond it in ring 5 and the void, so ring 5's blocks can bleed into the outer half of
 * {@code sundered_reach} in patches, up to its outer edge. The ring comes from the per-thread {@link RingColumnCache},
 * which the other conditions of the column share. Without a ring 5 field (another dimension) false. The surface pass asks
 * per block; the condition remembers its last column (one condition object per chunk's surface pass, so per thread).
 */
public record Zone45ShareCondition(double from) implements SurfaceRules.ConditionSource {
    public static final MapCodec<Zone45ShareCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.doubleRange(0.0, 1.0).fieldOf("from").forGetter(Zone45ShareCondition::from)
    ).apply(instance, Zone45ShareCondition::new));
    public static final KeyDispatchDataCodec<Zone45ShareCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    /** Whether a ring position lies at least {@code from} across the zone 4→5, or beyond it. */
    static boolean reached(RingPosition position, double from) {
        if (position.inner().ordinal() >= Ring.RING_5.ordinal()) {
            return true;
        }
        return position.inner() == Ring.RING_4 && position.outer() == Ring.RING_5 && position.blend() >= from;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        if (Ring5Terrain.world(context.randomState) == null) {
            return () -> false;
        }
        // The surface pass of a chunk runs on one thread: take its cache once, not per column.
        RingColumnCache columns = RingColumnCache.forThread();
        return new SurfaceRules.Condition() {
            private boolean known;
            private int lastX;
            private int lastZ;
            private boolean result;

            @Override
            public boolean test() {
                int x = context.blockX;
                int z = context.blockZ;
                if (!known || x != lastX || z != lastZ) {
                    known = true;
                    lastX = x;
                    lastZ = z;
                    result = reached(columns.at(Level.OVERWORLD, x, z), from);
                }
                return result;
            }
        };
    }
}
