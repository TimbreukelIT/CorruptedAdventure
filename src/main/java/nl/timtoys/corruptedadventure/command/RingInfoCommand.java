package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.Locale;

/**
 * {@code /ca ring}: ring, transition weight, distance from (0,0) and to the nearest boundary at the source's position.
 * Distances are in blocks of the source's dimension.
 */
final class RingInfoCommand {
    private RingInfoCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ring").executes(context -> show(context.getSource()));
    }

    private static int show(CommandSourceStack source) {
        ResourceKey<Level> dimension = source.getLevel().dimension();
        int scale = RingLayout.scale(dimension);
        Vec3 position = source.getPosition();
        RingLayout layout = RingLayout.current();
        RingPosition ring = layout.sample(dimension, position.x, position.z);
        RingTargets.BoundaryDistance boundary = RingTargets.nearestBoundary(
                layout, RingSettingsSnapshot.current(), position.x * scale, position.z * scale);

        double distance = Math.sqrt(position.x * position.x + position.z * position.z);
        String distanceLine = format("Distance from 0,0: %.0f blocks", distance)
                + (scale == 1 ? "" : format(" (overworld: %.0f)", distance * scale));
        double boundaryDistance = boundary.distance() / scale;
        String boundaryLine = format("Nearest boundary: %d (%s | %s), %+.0f blocks (%s)", boundary.boundary(),
                describe(Ring.byIndex(boundary.boundary() - 1)), describe(Ring.byIndex(boundary.boundary())),
                boundaryDistance, boundaryDistance < 0 ? "inside" : "outside");

        source.sendSuccess(() -> Component.literal(describe(ring) + "\n" + distanceLine + "\n" + boundaryLine), false);
        return ring.ring().number();
    }

    /** E.g. "Ring 2" or "Ring 3 (transition ring 2 -> ring 3, weight 0.63)". */
    static String describe(RingPosition position) {
        String ring = describe(position.ring());
        String text = Character.toUpperCase(ring.charAt(0)) + ring.substring(1);
        if (position.inTransition()) {
            text += format(" (transition %s -> %s, weight %.2f)",
                    describe(position.inner()), describe(position.outer()), position.blend());
        }
        return text;
    }

    static String describe(Ring ring) {
        return ring == Ring.VOID ? "void (6)" : "ring " + ring.number();
    }

    private static String format(String pattern, Object... arguments) {
        return String.format(Locale.ROOT, pattern, arguments);
    }
}
