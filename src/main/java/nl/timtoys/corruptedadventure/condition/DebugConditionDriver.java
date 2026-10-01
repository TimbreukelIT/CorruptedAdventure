package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives the {@link Condition#DEBUG} meter: per player rise, fall or off, set with {@code /ca condition debug}.
 * Server-side, not saved: forgotten on logout and when the server stops. Without a mode the meter only falls.
 */
public final class DebugConditionDriver {
    public enum Mode {
        RISE, FALL, OFF
    }

    private static final Map<UUID, Mode> MODES = new ConcurrentHashMap<>();

    private DebugConditionDriver() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> MODES.remove(event.getEntity().getUUID()));
        gameBus.addListener(ServerStoppedEvent.class, event -> MODES.clear());
    }

    public static void set(ServerPlayer player, Mode mode) {
        if (mode == Mode.OFF) {
            MODES.remove(player.getUUID());
        } else {
            MODES.put(player.getUUID(), mode);
        }
    }

    public static Mode mode(ServerPlayer player) {
        return MODES.getOrDefault(player.getUUID(), Mode.OFF);
    }

    /** The rule of {@link Condition#DEBUG}: {@code +riseRate} while rising, otherwise {@code -fallRate}. */
    static ConditionRule rule(float riseRate, float fallRate) {
        return context -> mode(context.player()) == Mode.RISE ? riseRate : -fallRate;
    }
}
