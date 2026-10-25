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
import nl.timtoys.corruptedadventure.worldgen.feature.VolcanicLandmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /ca landmarks x0 z0 x1 z1}: the blighted landmarks of ring 4 (decor B: drowned giant, stone circle, bone
 * carcass; {@link BlightedLandmarks}), the rock landmarks of ring 2 (G2: rock bridge, giant hoodoo; {@link
 * Ring2RockLandmarks}, the anchor: a point on the crossing / the main pillar's foot) and the biome-own landmarks of ring 3
 * (G2: charred giant, obsidian flow, whale skeleton, basalt organ, ember giant, sinter terraces, titan weapon; {@link
 * VolcanicLandmarks}, the anchor: the flow's source, the middle of the others) whose centre lies in the box, as the
 * features place them, one line each:
 * {@code kind x y z} (y: the first open block over the ground, under water the floor). Pure, no chunks needed; for the
 * tests and to find them in play.
 */
final class LandmarkListCommand {
    /** The largest box side: every landmark cell in it is decided on the server thread. */
    private static final int MAX_SPAN = 8192;
    /** The ring 3 landmarks decide every cell of the box on the server thread (build + fit per try): a smaller box. */
    private static final int MAX_SPAN_RING_3 = 2048;

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
        int minX = Math.min(x0, x1);
        int minZ = Math.min(z0, z1);
        int maxX = Math.max(x0, x1);
        int maxZ = Math.max(z0, z1);
        List<String> lines = new ArrayList<>();
        for (BlightedLandmarks.Site site : BlightedLandmarks.centredIn(source.getLevel(), minX, minZ, maxX, maxZ)) {
            lines.add(String.format(Locale.ROOT, "%s %d %d %d", site.kind().getSerializedName(), site.x(), site.ground(), site.z()));
        }
        lines.addAll(Ring2RockLandmarks.centredIn(source.getLevel(), minX, minZ, maxX, maxZ));
        boolean ring3 = (long) maxX - minX <= MAX_SPAN_RING_3 && (long) maxZ - minZ <= MAX_SPAN_RING_3;
        for (VolcanicLandmarks.Site site : ring3 ? VolcanicLandmarks.centredIn(source.getLevel(), minX, minZ, maxX, maxZ)
                : List.<VolcanicLandmarks.Site>of()) {
            lines.add(String.format(Locale.ROOT, "%s %d %d %d", site.kind().getSerializedName(), site.x(), site.ground(), site.z()));
        }
        StringBuilder text = new StringBuilder("Landmarks: " + lines.size()
                + (ring3 ? "" : " (ring 3 skipped: at most " + MAX_SPAN_RING_3 + " per side)"));
        lines.forEach(line -> text.append(System.lineSeparator()).append(line));
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return lines.size();
    }
}
