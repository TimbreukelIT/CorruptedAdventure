package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareRoaming;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareState;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareThreat;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareThreats;
import nl.timtoys.corruptedadventure.ring.Ring;

import java.util.Locale;
import java.util.UUID;

/**
 * {@code /ca nightmare status}, {@code threat <player> <ring> <value>}, {@code arrive <ring> [<player>]} and
 * {@code reset <ring>}: the Nightmare director (phase 4 steps 2 and 3) for tests. Always on the server's director in the
 * overworld; only in ring worlds. {@code arrive} skips the threat and the defeat timer; {@code reset} forgets the
 * defeats. Texts are lang keys {@code commands.corruptedadventure.nightmare.*} ({@code datagen/lang/NightmareDirectorText}).
 */
final class NightmareCommand {
    static final String KEY = "commands.corruptedadventure.nightmare.";

    private static final SimpleCommandExceptionType NOT_RING_WORLD =
            new SimpleCommandExceptionType(Component.translatable(KEY + "not_ring_world"));
    private static final DynamicCommandExceptionType FAILED =
            new DynamicCommandExceptionType(message -> (Component) message);

    private NightmareCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("nightmare")
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("threat")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(ringArgument()
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F))
                                                .executes(NightmareCommand::threat)))))
                .then(Commands.literal("arrive")
                        .then(ringArgument()
                                .executes(context -> arrive(context, context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> arrive(context, EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("reset")
                        .then(ringArgument().executes(NightmareCommand::reset)));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> ringArgument() {
        return Commands.argument("ring", IntegerArgumentType.integer(1, Ring.RING_5.number()));
    }

    private static Ring ring(CommandContext<CommandSourceStack> context) {
        return Ring.byIndex(IntegerArgumentType.getInteger(context, "ring") - 1);
    }

    private static NightmareKind kind(CommandContext<CommandSourceStack> context) {
        // Every ring 1-5 has a kind; the argument allows nothing else.
        return NightmareKind.of(ring(context)).orElseThrow();
    }

    private static ServerLevel overworld(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getServer().overworld();
        if (!NightmareDirector.server().worksIn(level)) {
            throw NOT_RING_WORLD.create();
        }
        return level;
    }

    private static Component name(NightmareKind kind) {
        return Component.translatable(kind.entityId().toLanguageKey("entity"));
    }

    private static int status(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = overworld(source);
        NightmareDirector director = NightmareDirector.server();
        long now = level.getGameTime();
        MutableComponent text = Component.translatable(KEY + "status.header");
        for (NightmareKind kind : NightmareKind.values()) {
            text.append("\n").append(line(level, kind, director.data(level).get(kind), now));
        }
        if (source.getPlayer() != null) {
            text.append("\n").append(threatLine(source.getPlayer()));
        }
        source.sendSuccess(() -> text, false);
        return 1;
    }

    private static Component line(ServerLevel level, NightmareKind kind, NightmareState state, long now) {
        if (kind.type().isEmpty()) {
            return Component.translatable(KEY + "status.missing", name(kind), kind.ring().number(),
                    kind.entityId().toString());
        }
        return Component.translatable(KEY + "status.line", name(kind), kind.ring().number(), phase(level, state, now),
                state.defeats(), String.format(Locale.ROOT, "%.2f", NightmareDirector.strength(state)));
    }

    private static Component phase(ServerLevel level, NightmareState state, long now) {
        String x = String.valueOf(Math.round(state.x()));
        String z = String.valueOf(Math.round(state.z()));
        return switch (state.phase()) {
            case ROAMING -> state.placed()
                    ? Component.translatable(KEY + "status.roaming", x, z, NightmareRoaming.compass(state.heading()))
                    : Component.translatable(KEY + "status.unplaced");
            case COMING -> Component.translatable(KEY + "status.coming", playerName(level, state.target()),
                    state.place().getX(), state.place().getY(), state.place().getZ(), state.omenTick());
            case PRESENT -> Component.translatable(KEY + "status.present", x, z,
                    String.valueOf(state.entity()), state.missingTicks() / 20);
            case DEFEATED -> Component.translatable(KEY + "status.defeated",
                    Math.max(0L, (state.defeatedUntil() - now + 19) / 20));
        };
    }

    private static String playerName(ServerLevel level, UUID uuid) {
        ServerPlayer player = uuid == null ? null : level.getServer().getPlayerList().getPlayer(uuid);
        return player != null ? player.getGameProfile().getName() : String.valueOf(uuid);
    }

    private static Component threatLine(ServerPlayer player) {
        NightmareThreat threat = NightmareThreats.of(player);
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < Ring.RING_5.number(); i++) {
            Ring ring = Ring.byIndex(i);
            if (i > 0) {
                values.append(", ");
            }
            values.append(String.format(Locale.ROOT, "%d: %.1f (%.2f d)", ring.number(), threat.get(ring), threat.days(ring)));
        }
        return Component.translatable(KEY + "status.threat", player.getGameProfile().getName(),
                String.format(Locale.ROOT, "%.0f", ServerConfig.NIGHTMARE_DIRECTOR.threatThreshold.get()), values.toString());
    }

    private static int threat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        overworld(context.getSource());
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        Ring ring = ring(context);
        float value = FloatArgumentType.getFloat(context, "value");
        NightmareThreats.of(player).set(ring, value);
        float now = NightmareThreats.of(player).get(ring);
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "threat.set", player.getGameProfile().getName(),
                ring.number(), String.format(Locale.ROOT, "%.1f", now)), true);
        return 1;
    }

    private static int arrive(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerLevel level = overworld(context.getSource());
        NightmareKind kind = kind(context);
        if (target.level() != level) {
            throw FAILED.create(Component.translatable(KEY + "arrive.not_overworld", target.getGameProfile().getName()));
        }
        NightmareDirector.ArriveResult result = NightmareDirector.server().arrive(level, kind, target);
        String key = switch (result) {
            case COMING -> "arrive.coming";
            case NO_ENTITY -> "arrive.no_entity";
            case ALREADY_COMING -> "arrive.already_coming";
            case ALREADY_PRESENT -> "arrive.already_present";
        };
        Component message = Component.translatable(KEY + key, name(kind), target.getGameProfile().getName());
        if (result != NightmareDirector.ArriveResult.COMING) {
            throw FAILED.create(message);
        }
        context.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerLevel level = overworld(context.getSource());
        NightmareKind kind = kind(context);
        NightmareDirector.server().reset(level, kind);
        context.getSource().sendSuccess(() -> Component.translatable(KEY + "reset.done", name(kind)), true);
        return 1;
    }
}
