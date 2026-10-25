package nl.timtoys.corruptedadventure.worldgen.surface;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

/**
 * Surface rule condition {@code corruptedadventure:zone_edge} (G2 ring 3, decision 8 = A): true past the frayed inner
 * edge of the overworld transition zone on the outside of {@code ring}. The edge is the zone's start (where a transition
 * biome takes over, a straight line at this scale) moved into {@code ring} by {@code width} · t, with t = (n + 1) / 2
 * of the noise n at the column, clamped to 0–1: true in the whole zone and beyond, and in the outer {@code width}
 * blocks of {@code ring} in patches that thin out inwards. A biome of {@code ring} that paints the zone's surface where
 * this holds blurs the hard line into tongues and islands (the zeekom's sand ↔ the steam fens' mud), without touching
 * the terrain or the biomes.
 * <p>
 * The zone's start comes from the world's own ring settings (radius and transition width of the boundary), so nothing
 * of the layout is written into the rules. The surface pass asks per block; the condition remembers its last column
 * (one condition object per chunk's surface pass, so per thread).
 */
public record ZoneEdgeCondition(Ring ring, ResourceKey<NormalNoise.NoiseParameters> noise, double width) implements SurfaceRules.ConditionSource {
    public static final MapCodec<ZoneEdgeCondition> MAP_CODEC = RecordCodecBuilder.<ZoneEdgeCondition>mapCodec(instance -> instance.group(
            Ring.CODEC.fieldOf("ring").forGetter(ZoneEdgeCondition::ring),
            ResourceKey.codec(Registries.NOISE).fieldOf("noise").forGetter(ZoneEdgeCondition::noise),
            Codec.doubleRange(0.0, 256.0).fieldOf("width").forGetter(ZoneEdgeCondition::width)
    ).apply(instance, ZoneEdgeCondition::new)).validate(ZoneEdgeCondition::validate);
    public static final KeyDispatchDataCodec<ZoneEdgeCondition> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private static DataResult<ZoneEdgeCondition> validate(ZoneEdgeCondition condition) {
        return condition.ring == Ring.VOID ? DataResult.error(() -> "zone_edge: the void has no outer zone")
                : DataResult.success(condition);
    }

    @Override
    public KeyDispatchDataCodec<? extends SurfaceRules.ConditionSource> codec() {
        return CODEC;
    }

    /** Where the zone after {@code ring} starts, in effective distance. */
    static double zoneStart(RingSettings settings, Ring ring) {
        int boundary = ring.ordinal();
        return settings.ringOuterRadii().get(boundary) - settings.transitionWidth(boundary) / 2.0;
    }

    /** Whether a column {@code depth} blocks past the zone's start (negative: inside {@code ring}) lies past the edge. */
    static boolean past(double depth, double width, double noise) {
        return depth >= -width * Mth.clamp((noise + 1.0) / 2.0, 0.0, 1.0);
    }

    @Override
    public SurfaceRules.Condition apply(SurfaceRules.Context context) {
        RingLayout layout = RingLayout.current();
        double start = zoneStart(layout.settings(), ring);
        NormalNoise instance = context.randomState.getOrCreateNoise(noise);
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
                    double depth = layout.effectiveDistance(x, z) - start;
                    // Far from the edge the noise cannot change the answer: skip it.
                    result = depth >= 0.0 || depth >= -width && past(depth, width, instance.getValue(x, 0.0, z));
                }
                return result;
            }
        };
    }
}
