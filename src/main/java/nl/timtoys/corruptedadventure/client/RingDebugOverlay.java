package nl.timtoys.corruptedadventure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.List;
import java.util.Locale;

/**
 * The mod's only F3 hook: lines on the right of the debug screen, to compare the client's view with {@code /ca ring}.
 * Later debug lines (ring weather, wind) are added here. Fixed English text, like {@code /ca}.
 */
final class RingDebugOverlay {
    private RingDebugOverlay() {
    }

    static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null) {
            return;
        }
        List<String> right = event.getRight();
        right.add("");
        right.add("CA ring (client): " + ClientRingLayout.ringAt(camera.level().dimension(), camera.getX(), camera.getZ())
                .map(RingDebugOverlay::describe)
                .orElse("—"));
    }

    /** E.g. {@code ring_1} or {@code ring_2 → ring_3 @ 0.43}. */
    static String describe(RingPosition position) {
        if (!position.inTransition()) {
            return position.inner().getSerializedName();
        }
        return String.format(Locale.ROOT, "%s → %s @ %.2f",
                position.inner().getSerializedName(), position.outer().getSerializedName(), position.blend());
    }
}
