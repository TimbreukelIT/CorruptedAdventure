package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.SurfaceRules;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingColumnCache;

/**
 * Surface rule condition {@code corruptedadventure:ring_reach}: true in the columns where {@code ring} can have weight
 * within {@link #MARGIN} blocks ({@link RingColumnCache#reaches}). A cheap guard in front of
 * {@code minecraft:biome} conditions that hold for whole stone columns (the rock strata of ring 2): a biome condition
 * looks the biome up for every block, through the world-generation region, while this needs one column lookup. Placed
 * only in front of conditions on the ring's own biomes, it changes nothing: they cannot occur where the ring has no
 * weight, and a block's biome comes from a quart corner at most 5 blocks away per axis, about 7 diagonally
 * ({@code BiomeManager}'s zoom), well inside the margin (change 39e).
 * <p>
 * The surface pass asks per block; the condition remembers its last column. One condition object per chunk's surface
 * pass, so per thread.
 */
public record RingReachCondition(ResourceKey<Level> dimension, Ring ring) implements SurfaceRules.ConditionSource {
    /**
     * Blocks of the dimension; twice the terrain gate's 16, because a biome's quart corner lies further from the block
     * (see above) than a cell corner, and the effective distance may run steeper than one block per block.
     */
    static final double MARGIN = 32.0;

    public static final MapCodec<RingReachCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(RingReachCondition::dimension),
            Ring.CODEC.fieldOf("ring").forGetter(RingReachCondition::ring)
    ).apply(instance, RingReachCondition::new));

    public static final KeyDispatchDataCodec<RingReachCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

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
                    result = RingColumnCache.reaches(dimension, ring, x, z, MARGIN);
                }
                return result;
            }
        };
    }
}
