package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2FlatLandmarks;

import java.util.List;
import java.util.Locale;

/**
 * {@code /ca flatmarks x0 z0 x1 z1}: the eye-catchers of ring 2's flat biomes (G2: salt dome, dune skeleton, giant
 * palm; {@link Ring2FlatLandmarks}) whose anchor lies in the box, one line each: {@code kind x y z} (y: the first open
 * block over the pure ground; the giant palm's anchor is its spring pool). Pure, no chunks needed; for the tests and to
 * find them in play. Each new cell costs a few column iterations of the generator, so the box is kept small.
 */
final class FlatLandmarkListCommand {
    /** The largest box side. */
    private static final int MAX_SPAN = 4096;

    private FlatLandmarkListCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("flatmarks").then(Commands.argument("x0", IntegerArgumentType.integer())
                .then(Commands.argument("z0", IntegerArgumentType.integer())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("z1", IntegerArgumentType.integer())
                                        .executes(context -> list(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "x0"),
                                                IntegerArgumentType.getInteger(context, "z0"),
                                                IntegerArgumentType.getInteger(context, "x1"),
                                                IntegerArgumentType.getInteger(context, "z1")))))));
    }

    private static int list(CommandSourceStack source, int x0, int z0, int x1, int z1) throws CommandSyntaxException {
        if (Math.abs(x1 - x0) > MAX_SPAN || Math.abs(z1 - z0) > MAX_SPAN) {
            throw new SimpleCommandExceptionType(Component.literal("Box too large (at most " + MAX_SPAN + " per side)")).create();
        }
        List<Ring2FlatLandmarks.Site> sites = Ring2FlatLandmarks.centredIn(source.getLevel(), Math.min(x0, x1), Math.min(z0, z1),
                Math.max(x0, x1), Math.max(z0, z1));
        StringBuilder text = new StringBuilder("Flat landmarks: " + sites.size());
        for (Ring2FlatLandmarks.Site site : sites) {
            text.append(String.format(Locale.ROOT, "%n%s %d %d %d", site.kind().getSerializedName(), site.x(), site.ground(), site.z()));
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return sites.size();
    }
}
