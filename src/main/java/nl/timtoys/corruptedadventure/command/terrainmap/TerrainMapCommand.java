package nl.timtoys.corruptedadventure.command.terrainmap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Locale;

/**
 * {@code /ca terrainmap <center> <radius> [scale]}: height and biome map (PNG) of a square around a column;
 * {@code /ca terrainmap section <from> <to>}: vertical section along a line; {@code /ca terrainmap cancel}.
 * Coordinates in the source's dimension ({@code /execute in minecraft:the_nether run ca terrainmap …}). Computed from
 * the world's density functions and biome source, without chunks, off the server thread.
 */
public final class TerrainMapCommand {
    private TerrainMapCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("terrainmap")
                .then(Commands.literal("cancel").executes(context -> cancel(context.getSource())))
                .then(Commands.literal("section")
                        .then(Commands.argument("from", ColumnPosArgument.columnPos())
                                .then(Commands.argument("to", ColumnPosArgument.columnPos())
                                        .executes(context -> section(context.getSource(),
                                                ColumnPosArgument.getColumnPos(context, "from"),
                                                ColumnPosArgument.getColumnPos(context, "to"))))))
                .then(Commands.argument("center", ColumnPosArgument.columnPos())
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, MapArea.MAX_SIZE * MapArea.MAX_SCALE / 2))
                                .executes(context -> topView(context.getSource(),
                                        ColumnPosArgument.getColumnPos(context, "center"),
                                        IntegerArgumentType.getInteger(context, "radius"), MapArea.DEFAULT_SCALE))
                                .then(Commands.argument("scale", IntegerArgumentType.integer(1, MapArea.MAX_SCALE))
                                        .executes(context -> topView(context.getSource(),
                                                ColumnPosArgument.getColumnPos(context, "center"),
                                                IntegerArgumentType.getInteger(context, "radius"),
                                                IntegerArgumentType.getInteger(context, "scale"))))));
    }

    private static int topView(CommandSourceStack source, ColumnPos center, int radius, int scale) {
        String invalid = MapArea.validate(radius, scale);
        if (invalid != null) {
            return fail(source, invalid);
        }
        TerrainSource terrain = TerrainSource.of(source.getLevel());
        if (terrain == null) {
            return fail(source, "Terrain maps need a noise-based world generator");
        }
        MapArea area = new MapArea(center.x(), center.z(), radius, scale);
        String dimension = dimensionName(source.getLevel());
        String name = String.format(Locale.ROOT, "top_%s_%d_%d_r%d_s%d", dimension, center.x(), center.z(), radius, scale);
        List<String> header = List.of(
                "Terrain map, " + source.getLevel().dimension().location(),
                String.format(Locale.ROOT, "center %d, %d  radius %d", center.x(), center.z(), radius),
                String.format(Locale.ROOT, "%d blocks per pixel, north up", scale),
                "seed " + seed(source.getLevel()),
                "from the density functions (no chunks)");
        String description = String.format(Locale.ROOT, "Terrain map started: %d x %d columns around %d, %d at %d blocks per pixel",
                area.size(), area.size(), center.x(), center.z(), scale);
        return start(source, terrain, new TopViewWork(terrain, area, name, header), description, area.columns());
    }

    private static int section(CommandSourceStack source, ColumnPos from, ColumnPos to) {
        String invalid = SectionLine.validate(from.x(), from.z(), to.x(), to.z());
        if (invalid != null) {
            return fail(source, invalid);
        }
        TerrainSource terrain = TerrainSource.of(source.getLevel());
        if (terrain == null) {
            return fail(source, "Terrain maps need a noise-based world generator");
        }
        SectionLine line = new SectionLine(from.x(), from.z(), to.x(), to.z());
        String name = String.format(Locale.ROOT, "section_%s_%d_%d_%d_%d", dimensionName(source.getLevel()),
                from.x(), from.z(), to.x(), to.z());
        List<String> header = List.of(
                "Section, " + source.getLevel().dimension().location(),
                String.format(Locale.ROOT, "from %d, %d (left) to %d, %d", from.x(), from.z(), to.x(), to.z()),
                String.format(Locale.ROOT, "%.0f blocks, %.2f per pixel", line.length(), line.step()),
                "1 pixel per block vertically",
                "seed " + seed(source.getLevel()),
                "from the density functions (no chunks)");
        String description = String.format(Locale.ROOT, "Section started: %d columns from %d, %d to %d, %d",
                line.samples(), from.x(), from.z(), to.x(), to.z());
        return start(source, terrain, new SectionWork(terrain, line, name, header), description, line.samples());
    }

    private static int start(CommandSourceStack source, TerrainSource terrain, MapWork work, String description, int columns) {
        if (!TerrainMapJob.start(source, terrain, work, description)) {
            return fail(source, "A terrain map is already running; /ca terrainmap cancel stops it");
        }
        return columns;
    }

    private static int cancel(CommandSourceStack source) {
        if (!TerrainMapJob.cancelRunning(false)) {
            return fail(source, "No terrain map is running");
        }
        source.sendSuccess(() -> Component.literal("Cancelling the terrain map"), true);
        return 1;
    }

    private static String dimensionName(ServerLevel level) {
        return level.dimension().location().getPath().replace('/', '_');
    }

    private static long seed(ServerLevel level) {
        return level.getSeed();
    }

    private static int fail(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
        return 0;
    }
}
