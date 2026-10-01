package nl.timtoys.corruptedadventure.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The ring layout as the client knows it, received from the server on login. Client code always asks this class, never
 * {@code RingLayout.current()}: that one only exists next to a running server, so it seems to work in singleplayer but
 * crashes on a client connected to a dedicated server. Empty before the layout arrived and in worlds without rings.
 */
public final class ClientRingLayout {
    private static volatile RingLayout layout;

    private ClientRingLayout() {
    }

    public static Optional<RingLayout> get() {
        return Optional.ofNullable(layout);
    }

    public static Optional<RingPosition> ringAt(ResourceKey<Level> dimension, double x, double z) {
        return get().map(current -> current.sample(dimension, x, z));
    }

    /** @param received the new layout, or null to forget it */
    public static void set(@Nullable RingLayout received) {
        layout = received;
    }

    /** So the next world (e.g. a vanilla-preset singleplayer world) never sees the layout of the previous one. */
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        layout = null;
    }
}
