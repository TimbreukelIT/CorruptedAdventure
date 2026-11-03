package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.network.KeeperMistPayload;

/**
 * Sends one of The Lost Keeper's mist banks ({@link KeeperMistPayload}) to every player within range. A keeper's own bank
 * has its entity id; the mist of its arrival and the wisps it leaves while it roams have negative ids of their own, so
 * the client keeps them apart from a keeper's and from each other.
 */
final class KeeperMistBanks {
    /** The mist that rolls in during the arrival's omens (one arrival at a time). */
    static final int ARRIVAL = KeeperMistPayload.ARRIVAL_BANK;
    /** The wisps while it roams: {@code TRACES - slot}. */
    static final int TRACES = -2;

    private KeeperMistBanks() {
    }

    static void send(ServerLevel level, int id, Vec3 centre, double radius, double fade, double visibility, float strength,
                     double range) {
        KeeperMistPayload payload = new KeeperMistPayload(id, centre.x, centre.y, centre.z, (float) radius, (float) fade,
                (float) visibility, strength);
        for (ServerPlayer player : level.players()) {
            // A connection without our channel (a GameTest mock player) gets nothing.
            if (player.distanceToSqr(centre) <= range * range && player.connection.hasChannel(KeeperMistPayload.TYPE)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }
}
