package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.world.level.Level;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;

import java.util.List;

/**
 * What the director asks of the ring layout, in overworld blocks: which ring a position draws (threat, target, roaming)
 * and the radii of a ring's band (where a Nightmare first roams). On a server always {@link #LAYOUT} (via
 * {@code ringAt}); GameTests give their own.
 */
public interface NightmareRings {
    /** The running server's layout; the void edge (up to the world border) counts as ring 5. */
    NightmareRings LAYOUT = of(null);

    /** The ring at an overworld position as the Nightmares count it; {@link Ring#VOID} only beyond the world border. */
    Ring ringAt(double x, double z);

    /** Radius from (0,0) where {@code ring} starts (0 for ring 1). */
    double innerRadius(Ring ring);

    /** Radius from (0,0) where {@code ring} ends. */
    double outerRadius(Ring ring);

    /** A fixed layout ({@code null}: the running server's, read at each call). */
    static NightmareRings of(RingLayout fixed) {
        return new NightmareRings() {
            private RingLayout layout() {
                return fixed != null ? fixed : RingLayout.current();
            }

            @Override
            public Ring ringAt(double x, double z) {
                RingLayout layout = layout();
                Ring ring = layout.sample(Level.OVERWORLD, x, z).ring();
                // Beyond the world border is nowhere, not the void edge.
                if (ring == Ring.VOID && x * x + z * z > square(layout.settings().voidOuterRadius())) {
                    return Ring.VOID;
                }
                return NightmareThreats.countedRing(ring);
            }

            private double square(double value) {
                return value * value;
            }

            @Override
            public double innerRadius(Ring ring) {
                return ring.ordinal() == 0 ? 0.0 : radius(ring.ordinal() - 1);
            }

            @Override
            public double outerRadius(Ring ring) {
                return radius(ring.ordinal());
            }

            private double radius(int index) {
                RingSettings settings = layout().settings();
                List<Integer> radii = settings.ringOuterRadii();
                return index < radii.size() ? radii.get(index) : settings.voidOuterRadius();
            }
        };
    }
}
