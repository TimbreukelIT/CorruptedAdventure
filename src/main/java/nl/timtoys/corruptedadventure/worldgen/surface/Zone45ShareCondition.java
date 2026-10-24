package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring4Sundered;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5Terrain;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring5World;

/**
 * Surface rule condition {@code corruptedadventure:zone_4_5_share} (walk 1, question 2 = A): true where the column lies in
 * the zone 4→5 at least {@code from} of the way across (0 = ring 4's side, 1 = ring 5's side; the effective distance
 * between {@link Ring4Sundered#zoneStart} and {@link Ring4Sundered#zoneEnd}), so ring 5's blocks can bleed into the
 * outer half of {@code sundered_reach} in patches. Without a ring 5 field (another dimension) false. The surface pass asks
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

    /** How far across the zone 4→5 an effective distance lies: below 0 before it, above 1 beyond it. */
    public static double share(RingLayout layout, double effective) {
        double start = Ring4Sundered.zoneStart(layout);
        return (effective - start) / (Ring4Sundered.zoneEnd(layout) - start);
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        Ring5World world = Ring5Terrain.world(context.randomState);
        if (world == null) {
            return () -> false;
        }
        RingLayout layout = world.layout();
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
                    double share = share(layout, layout.effectiveDistance(x, z));
                    result = share >= from && share <= 1.0;
                }
                return result;
            }
        };
    }
}
