package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.worldgen.chunk.Ring5Fields;
import nl.timtoys.corruptedadventure.worldgen.densityfunction.Ring5Field;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;

/**
 * {@code /ca castles}: the four castle sites of ring 5 ({@link Ring5Field#castleSites}, server only), one line each:
 * {@code #<1–4> angle x z radius top belt distance} (distance from the command's source). {@code /ca castles tp <1–4>}
 * puts the player on top of that pinnacle (the landing rule of {@code /ca tpring}). Only in a ring world with ring 5
 * terrain; numbers 1–4 are {@link Ring5Field.CastleSite#index()} + 1.
 */
final class CastlesCommand {
    private CastlesCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("castles")
                .executes(context -> list(context.getSource()))
                .then(Commands.literal("tp").then(Commands.argument("site", IntegerArgumentType.integer(1, 4))
                        .executes(context -> teleport(context.getSource(), IntegerArgumentType.getInteger(context, "site")))));
    }

    @Nullable
    private static List<Ring5Field.CastleSite> sites(CommandSourceStack source) {
        Ring5Field field = Ring5Fields.current(Level.OVERWORLD);
        if (field == null) {
            source.sendFailure(Component.literal("No ring 5 terrain in this world (not a ring world)"));
            return null;
        }
        return field.castleSites();
    }

    private static int list(CommandSourceStack source) {
        List<Ring5Field.CastleSite> sites = sites(source);
        if (sites == null) {
            return 0;
        }
        Vec3 from = source.getPosition();
        source.sendSuccess(() -> Component.literal(describe(sites, from.x, from.z)), false);
        return sites.size();
    }

    /** The listing: a header and one line per site. */
    static String describe(List<Ring5Field.CastleSite> sites, double fromX, double fromZ) {
        StringBuilder text = new StringBuilder("Castle sites: " + sites.size() + " (# angle x z radius top belt distance)");
        for (Ring5Field.CastleSite site : sites) {
            text.append('\n').append(line(site, fromX, fromZ));
        }
        return text.toString();
    }

    /** One site: {@code #n angle° x z r=radius top=Y belt=width d=distance}. */
    static String line(Ring5Field.CastleSite site, double fromX, double fromZ) {
        return String.format(Locale.ROOT, "#%d %.1f° %d %d r=%d top=%d belt=%d d=%d", site.index() + 1, site.angle(), site.x(),
                site.z(), site.radius(), site.top(), site.belt(), Math.round(site.distance(fromX, fromZ)));
    }

    /** The site of a number 1–4, or null if there is none. */
    @Nullable
    static Ring5Field.CastleSite site(List<Ring5Field.CastleSite> sites, int number) {
        return sites.stream().filter(site -> site.index() == number - 1).findFirst().orElse(null);
    }

    private static int teleport(CommandSourceStack source, int number) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        List<Ring5Field.CastleSite> sites = sites(source);
        if (sites == null) {
            return 0;
        }
        Ring5Field.CastleSite site = site(sites, number);
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (site == null || level == null) {
            source.sendFailure(Component.literal("No castle site #" + number));
            return 0;
        }
        OptionalInt safeY = RingTeleportCommand.findSafeY(level, site.x(), site.z());
        int y = safeY.orElse(site.top());
        player.teleportTo(level, site.x() + 0.5, y, site.z() + 0.5, player.getYRot(), player.getXRot());
        source.sendSuccess(() -> Component.literal("Teleported to castle site " + line(site, site.x(), site.z()) + " at Y " + y), true);
        if (safeY.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No safe ground found, placed at Y " + y).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }
}
