package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.chunk.IslandFields;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.IslandField;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * {@code /ca falls x0 z0 x1 z1}: the planned waterfalls in the box, as the terrain places them
 * ({@link IslandField#waterfallsWithin}). Writes {@code ca_falls/<x0>_<z0>_<x1>_<z1>.tsv} in the world folder, one line
 * per fall: {@code x z top bottom} (falling column, top = spring Y, bottom = first block above the water it falls
 * into). Used by {@code tools/test/py/waterscan.py}.
 */
final class FallListCommand {
    private static final String DIRECTORY = "ca_falls";

    private FallListCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("falls").then(Commands.argument("x0", IntegerArgumentType.integer())
                .then(Commands.argument("z0", IntegerArgumentType.integer())
                        .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("z1", IntegerArgumentType.integer())
                                        .executes(context -> write(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "x0"),
                                                IntegerArgumentType.getInteger(context, "z0"),
                                                IntegerArgumentType.getInteger(context, "x1"),
                                                IntegerArgumentType.getInteger(context, "z1")))))));
    }

    private static int write(CommandSourceStack source, int x0, int z0, int x1, int z1) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        IslandField field = IslandFields.current(level.dimension());
        if (field == null) {
            throw new SimpleCommandExceptionType(Component.literal("No island field for " + level.dimension().location())).create();
        }
        int minX = Math.min(x0, x1);
        int maxX = Math.max(x0, x1);
        int minZ = Math.min(z0, z1);
        int maxZ = Math.max(z0, z1);
        List<IslandField.Waterfall> falls = field.waterfallsWithin(minX, minZ, maxX, maxZ);
        StringBuilder text = new StringBuilder();
        for (IslandField.Waterfall fall : falls) {
            text.append(String.format(Locale.ROOT, "%d\t%d\t%d\t%d%n", fall.x(), fall.z(), fall.top(), fall.bottom()));
        }
        Path directory = level.getServer().getWorldPath(LevelResource.ROOT).resolve(DIRECTORY);
        Path file = directory.resolve(String.format(Locale.ROOT, "%d_%d_%d_%d.tsv", minX, minZ, maxX, maxZ));
        try {
            Files.createDirectories(directory);
            Files.writeString(file, text);
        } catch (IOException failure) {
            throw new SimpleCommandExceptionType(Component.literal("Could not write " + file + ": " + failure.getMessage())).create();
        }
        CorruptedAdventure.LOGGER.info("Fall list written: {} ({} falls)", file.toAbsolutePath(), falls.size());
        int count = falls.size();
        source.sendSuccess(() -> Component.literal("Falls: " + count + " -> " + file.getFileName()), false);
        return count;
    }
}
