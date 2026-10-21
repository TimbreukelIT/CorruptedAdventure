package nl.timtoys.corruptedadventure.condition;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.Ring5Band;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.function.BooleanSupplier;

/**
 * Where a position lies for the void meter: ring 5's inner band, its outer band (the void rim beyond ring 5 counts as
 * outer, {@code golf-1.md} P4, like the weather where the void follows ring 5), or outside the zone. The ring switches at
 * blend 0.5 ({@link RingPosition#ring}), the band at {@link Ring5Band#outer} ({@code value ≥ 0}).
 */
enum VoidZone {
    INNER,
    OUTER,
    OUTSIDE;

    /** The zone at overworld {@code x, z} of {@code dimension}, with the ring already sampled; the band only in ring 5. */
    static VoidZone of(ResourceKey<Level> dimension, RingPosition position, double x, double z) {
        return of(dimension, position, () -> Ring5Band.of(RingLayout.current()).outer(x, z));
    }

    /** As {@link #of(ResourceKey, RingPosition, double, double)}; {@code outerBand} is only asked in ring 5 itself. */
    static VoidZone of(ResourceKey<Level> dimension, RingPosition position, BooleanSupplier outerBand) {
        if (!inZone(dimension, position)) {
            return OUTSIDE;
        }
        return position.ring() == Ring.VOID || outerBand.getAsBoolean() ? OUTER : INNER;
    }

    /** Overworld, ring 5 or the void rim (hard at 0.5, like the biomes and the ring weather). */
    static boolean inZone(ResourceKey<Level> dimension, RingPosition position) {
        Ring ring = position.ring();
        return dimension == Level.OVERWORLD && (ring == Ring.RING_5 || ring == Ring.VOID);
    }
}
