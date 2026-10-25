package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import nl.timtoys.corruptedadventure.worldgen.feature.BlightedLandmarks;
import nl.timtoys.corruptedadventure.worldgen.feature.Ring2RockLandmarks;

import java.util.List;
import java.util.Locale;

/**
 * {@code /ca landmarks x0 z0 x1 z1}: the blighted landmarks of ring 4 (decor B: drowned giant, stone circle, bone
 * carcass) and the rock landmarks of ring 2 (G2: rock bridge, giant hoodoo) whose centre lies in the box, as the
 * features place them ({@link BlightedLandmarks}, {@link Ring2RockLandmarks}), one line each:
 * {@code kind x y z} (y: the first open block over the ground, under water the floor). Pure, no chunks needed; for the
 * tests and to find them in play.
 */
final class LandmarkListCommand {
    /** The largest box side: every landmark cell in it is decided on the server thread. */
    private static final int MAX_SPAN = 8192;

    private LandmarkListCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("landmarks").then(Commands.argument("x0", IntegerArgumentType.integer())
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
        if (Math.abs((long) x1 - x0) > MAX_SPAN || Math.abs((long) z1 - z0) > MAX_SPAN) {
            throw new SimpleCommandExceptionType(Component.literal("Box too large (at most " + MAX_SPAN + " per side)")).create();
        }
        List<BlightedLandmarks.Site> sites = BlightedLandmarks.centredIn(source.getLevel(), Math.min(x0, x1), Math.min(z0, z1),
                Math.max(x0, x1), Math.max(z0, z1));
        // G2 ring 2: the rock bridges and giant hoodoos (anchor: a point on the crossing / the main pillar's foot).
        List<String> rocks = Ring2RockLandmarks.centredIn(source.getLevel(), Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1),
                Math.max(z0, z1));
        StringBuilder text = new StringBuilder("Landmarks: " + (sites.size() + rocks.size()));
        for (BlightedLandmarks.Site site : sites) {
            text.append(String.format(Locale.ROOT, "%n%s %d %d %d", site.kind().getSerializedName(), site.x(), site.ground(), site.z()));
        }
        for (String rock : rocks) {
            text.append(System.lineSeparator()).append(rock);
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return sites.size() + rocks.size();
    }
}
