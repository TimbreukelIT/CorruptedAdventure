package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

/** A closed range of doubles; in JSON {@code [min, max]} or a single number. */
record Range(double min, double max) {
    static final Codec<Range> CODEC = Codec.either(Codec.DOUBLE, Codec.DOUBLE.listOf())
            .comapFlatMap(Range::decode, range -> range.min == range.max
                    ? Either.left(range.min) : Either.right(List.of(range.min, range.max)));

    private static DataResult<Range> decode(Either<Double, List<Double>> either) {
        return either.map(value -> DataResult.success(new Range(value, value)), list -> {
            if (list.size() != 2) {
                return DataResult.error(() -> "A range needs [min, max], got " + list);
            }
            if (list.get(0) > list.get(1)) {
                return DataResult.error(() -> "A range needs min <= max, got " + list);
            }
            return DataResult.success(new Range(list.get(0), list.get(1)));
        });
    }

    /** {@code min} at {@code t} = 0 to {@code max} at {@code t} = 1. */
    double at(double t) {
        return min + t * (max - min);
    }

    /** The same codec, but every value must lie in [lo, hi]. */
    static Codec<Range> within(double lo, double hi) {
        return CODEC.validate(range -> range.min >= lo && range.max <= hi
                ? DataResult.success(range)
                : DataResult.error(() -> "Range " + range + " must lie within [" + lo + ", " + hi + "]"));
    }
}
