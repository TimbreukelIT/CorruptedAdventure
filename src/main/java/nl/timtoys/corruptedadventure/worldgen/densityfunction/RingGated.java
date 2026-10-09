package nl.timtoys.corruptedadventure.worldgen.densityfunction;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

import javax.annotation.Nullable;

/**
 * A gate that {@link RingBlend#mapAll} puts inside the cache markers of a ring input ({@link RingGate},
 * {@link RingFillGate}): it passes on {@link #argument} where the ring may matter. Code that looks for a density
 * function of its own type through holders and markers ({@code CellEdges}, {@code RockPillars}, {@code VolcanicField})
 * looks through these gates too ({@link #unwrap}, 39f).
 */
public sealed interface RingGated permits RingGate, RingFillGate {
    DensityFunction argument();

    /**
     * The function one wrapper further in: the value of a holder reference, the argument of a marker or of a ring gate;
     * null if {@code function} is none of those.
     */
    @Nullable
    static DensityFunction unwrap(DensityFunction function) {
        if (function instanceof DensityFunctions.HolderHolder holder) {
            return holder.function().value();
        }
        if (function instanceof DensityFunctions.MarkerOrMarked marker) {
            return marker.wrapped();
        }
        if (function instanceof RingGated gate) {
            return gate.argument();
        }
        return null;
    }
}
