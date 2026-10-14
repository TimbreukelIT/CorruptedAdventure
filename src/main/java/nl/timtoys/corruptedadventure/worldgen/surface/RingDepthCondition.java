package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

/**
 * Surface rule condition {@code corruptedadventure:ring_depth} (65): true in the columns at least {@code min_depth}
 * blocks inside {@code ring}, measured to the nearest of its two (noisy) boundaries ({@link RingColumnCache#boundaryDepth},
 * the measure of the {@code ring_edge} density function). Lets a surface rule follow the {@code core} weighting of a
 * ring's terrain (= 1 − {@code ring_edge}), e.g. paint a shape only where the terrain has it at full strength, not in the
 * band where it fades out towards the boundaries.
 * <p>
 * The surface pass asks per block; the condition remembers its last column. One condition object per chunk's surface
 * pass, so per thread.
 */
public record RingDepthCondition(ResourceKey<Level> dimension, Ring ring, double minDepth) implements SurfaceRules.ConditionSource {
    public static final MapCodec<RingDepthCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingDepthCondition::dimension),
            Ring.CODEC.fieldOf("ring").forGetter(RingDepthCondition::ring),
            Codec.DOUBLE.fieldOf("min_depth").forGetter(RingDepthCondition::minDepth)
    ).apply(instance, RingDepthCondition::new));

    public static final KeyDispatchDataCodec<RingDepthCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
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
                    result = RingColumnCache.boundaryDepth(dimension, ring, x, z) >= minDepth;
                }
                return result;
            }
        };
    }
}
