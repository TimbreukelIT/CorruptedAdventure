package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionMeters;
import nl.timtoys.corruptedadventure.condition.DebugConditionDriver;
import nl.timtoys.corruptedadventure.condition.PlayerConditions;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * {@code /ca condition get [<targets>]}, {@code set <targets> <condition> <0-100>} and
 * {@code debug <targets> rise|fall|off}. Works for every condition, also from RCON (with explicit targets).
 */
final class ConditionCommand {
    private static final DynamicCommandExceptionType UNKNOWN_CONDITION =
            new DynamicCommandExceptionType(name -> Component.literal("Unknown condition: " + name));

    private ConditionCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("condition")
                .then(Commands.literal("get")
                        .executes(context -> get(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> get(context.getSource(), EntityArgument.getPlayers(context, "targets")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("condition", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Condition.values()).map(Condition::getSerializedName), builder))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(ConditionMeters.MIN, ConditionMeters.MAX))
                                                .executes(ConditionCommand::set)))))
                .then(Commands.literal("debug")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(debugMode(DebugConditionDriver.Mode.RISE))
                                .then(debugMode(DebugConditionDriver.Mode.FALL))
                                .then(debugMode(DebugConditionDriver.Mode.OFF))));
    }

    private static int get(CommandSourceStack source, Collection<ServerPlayer> players) {
        String text = players.stream()
                .map(player -> player.getGameProfile().getName() + ": " + Arrays.stream(Condition.values())
                        .map(condition -> String.format(Locale.ROOT, "%s %.1f",
                                condition.getSerializedName(), PlayerConditions.get(player, condition)))
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
        source.sendSuccess(() -> Component.literal(text), false);
        return players.size();
    }

    private static int set(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        Condition condition = condition(StringArgumentType.getString(context, "condition"));
        float value = FloatArgumentType.getFloat(context, "value");
        players.forEach(player -> PlayerConditions.set(player, condition, value));
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Set %s to %.1f for %d player(s)",
                condition.getSerializedName(), value, players.size())), true);
        return players.size();
    }

    private static LiteralArgumentBuilder<CommandSourceStack> debugMode(DebugConditionDriver.Mode mode) {
        String name = mode.name().toLowerCase(Locale.ROOT);
        return Commands.literal(name).executes(context -> {
            Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
            players.forEach(player -> DebugConditionDriver.set(player, mode));
            context.getSource().sendSuccess(() -> Component.literal(
                    "Debug condition " + name + " for " + players.size() + " player(s)"), true);
            return players.size();
        });
    }

    private static Condition condition(String name) throws CommandSyntaxException {
        for (Condition condition : Condition.values()) {
            if (condition.getSerializedName().equals(name)) {
                return condition;
            }
        }
        throw UNKNOWN_CONDITION.create(name);
    }
}
