package nl.timtoys.corruptedadventure.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

import java.util.List;

/**
 * Players per ring, for payloads and for server-side gameplay. Callers check {@code RingWorld.isActive} first; for one
 * player, a dimension or everyone use NeoForge's {@link PacketDistributor} directly.
 */
public final class RingPacketDistributor {
    /** Who counts as "in a ring". */
    public enum RingMembership {
        /** In the ring after the hard switch at weight 0.5 (same as biomes and {@code /ca ring}). */
        CORE,
        /** In the ring itself or in a transition zone bordering it; a player in zone 2 → 3 counts for both. */
        TOUCHING
    }

    private RingPacketDistributor() {
    }

    public static boolean matches(RingPosition position, Ring ring, RingMembership membership) {
        return switch (membership) {
            case CORE -> position.ring() == ring;
            case TOUCHING -> position.inner() == ring || position.outer() == ring;
        };
    }

    public static List<ServerPlayer> playersInRing(ServerLevel level, Ring ring, RingMembership membership) {
        return level.players().stream()
                .filter(player -> matches(RingLayout.ringAt(level.dimension(), player.getX(), player.getZ()), ring, membership))
                .toList();
    }

    public static void sendToPlayersInRing(ServerLevel level, Ring ring, RingMembership membership, CustomPacketPayload payload) {
        for (ServerPlayer player : playersInRing(level, ring, membership)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
