package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.border.WorldBorder;

import java.util.Locale;

/**
 * {@code /ca border}: the world border of the source's dimension. Vanilla {@code /worldborder get} always reports the
 * overworld border, also in the Nether.
 */
final class BorderInfoCommand {
    private BorderInfoCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("border").executes(context -> show(context.getSource()));
    }

    private static int show(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        WorldBorder border = level.getWorldBorder();
        String text = format("Border in %s: size %.0f (radius %.0f), center %.1f, %.1f",
                level.dimension().location(), border.getSize(), border.getSize() / 2.0,
                border.getCenterX(), border.getCenterZ());
        if (border.getLerpRemainingTime() > 0L) {
            text += format("\nMoving to size %.0f in %d s", border.getLerpTarget(), border.getLerpRemainingTime() / 1000L);
        }
        String message = text;
        source.sendSuccess(() -> Component.literal(message), false);
        return Mth.floor(border.getSize() + 0.5);
    }

    private static String format(String pattern, Object... arguments) {
        return String.format(Locale.ROOT, pattern, arguments);
    }
}
