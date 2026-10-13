package nl.timtoys.corruptedadventure.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingWorld;
import nl.timtoys.corruptedadventure.weather.RingWeather;
import nl.timtoys.corruptedadventure.weather.RingWeatherCycle;
import nl.timtoys.corruptedadventure.weather.RingWeatherData;
import nl.timtoys.corruptedadventure.weather.RingWeatherSettings;
import nl.timtoys.corruptedadventure.weather.RingWeatherState;
import nl.timtoys.corruptedadventure.weather.RingWeatherType;
import nl.timtoys.corruptedadventure.weather.SandstormWind;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@code /ca weather}: the ring weather of the source's dimension. {@code /ca weather <ring> <type> [seconds]} sets it
 * (without seconds: a duration drawn from the config). Works in the source's dimension, so use {@code execute in …}.
 */
final class RingWeatherCommand {
    private static final int MAX_SECONDS = 86_400;
    private static final DynamicCommandExceptionType FAILED = new DynamicCommandExceptionType(message -> Component.literal((String) message));

    private RingWeatherCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("weather")
                .executes(context -> show(context.getSource()))
                .then(Commands.argument("ring", IntegerArgumentType.integer(1, Ring.RING_5.number()))
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(suggestions(context), builder))
                                .executes(context -> set(context, Optional.empty()))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1, MAX_SECONDS))
                                        .executes(context -> set(context,
                                                Optional.of(IntegerArgumentType.getInteger(context, "seconds")))))));
    }

    private static int show(CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        RingWeatherData data = data(level);
        String lines = data.states().entrySet().stream()
                .map(entry -> describe(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
        boolean running = level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE);
        source.sendSuccess(() -> Component.literal("Ring weather in " + level.dimension().location()
                + (running ? "" : " (cycle paused: doWeatherCycle is false)") + ":\n" + lines), false);
        return data.states().size();
    }

    /** E.g. {@code ring_2: sandstorm, 412 s of 600 left, intensity 0.83, wind 225° (from SW)}. */
    static String describe(Ring ring, RingWeatherState state) {
        String wind = SandstormWind.hasWind(state.type()) ? ", wind " + SandstormWind.describe(state) : "";
        return String.format(Locale.ROOT, "%s: %s, %d s of %d left, intensity %.2f%s", ring.getSerializedName(),
                state.type().getSerializedName(), seconds(state.remainingTicks()), seconds(state.durationTicks()),
                state.intensity(), wind);
    }

    private static int set(CommandContext<CommandSourceStack> context, Optional<Integer> seconds) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        data(level);
        Ring ring = Ring.byIndex(IntegerArgumentType.getInteger(context, "ring") - 1);
        String name = StringArgumentType.getString(context, "type");
        RingWeatherType type = RingWeatherType.byName(name);
        if (RingWeatherType.typesFor(level.dimension(), ring).isEmpty()) {
            throw FAILED.create(ring.getSerializedName() + " has no ring weather");
        }
        if (type == null || !type.allowedIn(level.dimension(), ring)) {
            throw FAILED.create(name + " is not a weather of " + ring.getSerializedName() + "; use one of: "
                    + typeNames(level, ring).collect(Collectors.joining(", ")));
        }
        int durationTicks = seconds.map(value -> value * RingWeatherSettings.TICKS_PER_SECOND)
                .orElseGet(() -> RingWeatherCycle.start(type, ServerConfig.ringWeather(ring), level.getRandom()).durationTicks());
        RingWeather.set(level, ring, type, durationTicks);
        RingWeatherState state = RingWeather.state(level, ring);
        source.sendSuccess(() -> Component.literal("Set " + describe(ring, state)), true);
        return 1;
    }

    private static RingWeatherData data(ServerLevel level) throws CommandSyntaxException {
        if (!RingWorld.isActive(level.getServer())) {
            throw FAILED.create("No ring weather: this is not a ring world");
        }
        return RingWeather.data(level).orElseThrow(() -> FAILED.create("No ring weather in this dimension"));
    }

    private static Stream<String> suggestions(CommandContext<CommandSourceStack> context) {
        Ring ring = Ring.byIndex(IntegerArgumentType.getInteger(context, "ring") - 1);
        return typeNames(context.getSource().getLevel(), ring);
    }

    /** {@code clear} and the ring's own types; nothing for a ring without ring weather. */
    private static Stream<String> typeNames(ServerLevel level, Ring ring) {
        return RingWeatherType.typesFor(level.dimension(), ring).isEmpty() ? Stream.empty()
                : Stream.concat(Stream.of(RingWeatherType.CLEAR), RingWeatherType.typesFor(level.dimension(), ring).stream())
                .map(RingWeatherType::getSerializedName);
    }

    private static int seconds(int ticks) {
        return (ticks + RingWeatherSettings.TICKS_PER_SECOND - 1) / RingWeatherSettings.TICKS_PER_SECOND;
    }
}
