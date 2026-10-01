package nl.timtoys.corruptedadventure.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import nl.timtoys.corruptedadventure.registry.ModPlacementModifierTypes;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.Locale;
import java.util.OptionalDouble;

/**
 * Placement modifier {@code corruptedadventure:ring_chance}: keeps a position with a chance that follows a ring
 * signal {@code t} (0–1) from {@code from_chance} to {@code to_chance}. Below {@code start}, or where the signal does
 * not apply, nothing is placed. The chance is drawn from vanilla's seeded decoration random, so worldgen stays
 * deterministic.
 * <p>
 * Signals: {@code transition} = the blend across the transition zone of the boundary whose inner ring is {@code ring}
 * (0 at the ring side, 1 at the outer ring side); {@code progress} = how far across {@code ring} itself the position
 * lies ({@link RingLayout#ringProgress}: 0 at its inner boundary, 1 at its outer boundary).
 */
public final class RingChanceFilter extends PlacementFilter {
    public static final MapCodec<RingChanceFilter> CODEC = RecordCodecBuilder.<RingChanceFilter>mapCodec(instance -> instance.group(
            Signal.CODEC.fieldOf("signal").forGetter(filter -> filter.signal),
            Ring.CODEC.fieldOf("ring").forGetter(filter -> filter.ring),
            Codec.doubleRange(0.0, 1.0).fieldOf("from_chance").forGetter(filter -> filter.fromChance),
            Codec.doubleRange(0.0, 1.0).fieldOf("to_chance").forGetter(filter -> filter.toChance),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("start", 0.0).forGetter(filter -> filter.start),
            Codec.DOUBLE.optionalFieldOf("exponent", 1.0).forGetter(filter -> filter.exponent)
    ).apply(instance, RingChanceFilter::new)).validate(RingChanceFilter::validate);

    private final Signal signal;
    private final Ring ring;
    private final double fromChance;
    private final double toChance;
    private final double start;
    private final double exponent;

    public RingChanceFilter(Signal signal, Ring ring, double fromChance, double toChance, double start, double exponent) {
        this.signal = signal;
        this.ring = ring;
        this.fromChance = fromChance;
        this.toChance = toChance;
        this.start = start;
        this.exponent = exponent;
    }

    /** Linear chance across the transition zone of the boundary {@code inner} → next ring. */
    public static RingChanceFilter transition(Ring inner, double fromChance, double toChance) {
        return new RingChanceFilter(Signal.TRANSITION, inner, fromChance, toChance, 0.0, 1.0);
    }

    /** Chance across the whole of {@code ring}, from its inner to its outer boundary. */
    public static RingChanceFilter progress(Ring ring, double fromChance, double toChance, double start, double exponent) {
        return new RingChanceFilter(Signal.PROGRESS, ring, fromChance, toChance, start, exponent);
    }

    private static DataResult<RingChanceFilter> validate(RingChanceFilter filter) {
        if (filter.start >= 1.0) {
            return DataResult.error(() -> "ring_chance: start must be below 1, got " + filter.start);
        }
        if (!(filter.exponent > 0.0)) {
            return DataResult.error(() -> "ring_chance: exponent must be positive, got " + filter.exponent);
        }
        if (filter.signal == Signal.TRANSITION && filter.ring == Ring.VOID) {
            return DataResult.error(() -> "ring_chance: there is no transition beyond the void");
        }
        if (filter.signal == Signal.PROGRESS && filter.ring == Ring.VOID) {
            return DataResult.error(() -> "ring_chance: the void has no outer boundary to progress towards");
        }
        return DataResult.success(filter);
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        // Only here, never in the codec: the layout exists once the server has started.
        ResourceKey<Level> dimension = context.getLevel().getLevel().dimension();
        return random.nextFloat() < chance(RingLayout.current(), dimension, pos.getX(), pos.getZ());
    }

    /** Chance at a block position; 0 where the signal does not apply. */
    double chance(RingLayout layout, ResourceKey<Level> dimension, int x, int z) {
        OptionalDouble t = signal(layout, dimension, x, z);
        return t.isPresent() ? curve(fromChance, toChance, start, exponent, t.getAsDouble()) : 0.0;
    }

    private OptionalDouble signal(RingLayout layout, ResourceKey<Level> dimension, int x, int z) {
        return switch (signal) {
            case TRANSITION -> transition(ring, layout.sample(dimension, x, z));
            case PROGRESS -> {
                int scale = RingLayout.scale(dimension);
                yield layout.ringProgress(ring, (double) x * scale, (double) z * scale);
            }
        };
    }

    static OptionalDouble transition(Ring inner, RingPosition position) {
        return position.inTransition() && position.inner() == inner ? OptionalDouble.of(position.blend()) : OptionalDouble.empty();
    }

    /** 0 below {@code start}; from there {@code from} → {@code to} along {@code ((t − start) / (1 − start))^exponent}. */
    static double curve(double from, double to, double start, double exponent, double t) {
        if (t < start) {
            return 0.0;
        }
        double progress = Math.min(1.0, (t - start) / (1.0 - start));
        return from + (to - from) * Math.pow(progress, exponent);
    }

    @Override
    public PlacementModifierType<?> type() {
        return ModPlacementModifierTypes.RING_CHANCE.get();
    }

    public enum Signal implements StringRepresentable {
        TRANSITION,
        PROGRESS;

        public static final Codec<Signal> CODEC = StringRepresentable.fromEnum(Signal::values);

        private final String serializedName = name().toLowerCase(Locale.ROOT);

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
