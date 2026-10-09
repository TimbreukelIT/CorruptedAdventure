package nl.timtoys.corruptedadventure.weather;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload;
import nl.timtoys.corruptedadventure.network.VolcanoPlumesPayload.Plume;
import nl.timtoys.corruptedadventure.ring.Ring;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The smoke plumes of the ring 3 volcanoes (package 50b, G2-A1 = A), server side: every {@link #CHECK_TICKS} each player
 * within {@link #RADIUS} blocks of ring 3 gets the active volcanoes within that radius ({@link VolcanoPlumesPayload}),
 * but only when that set changed since the last one sent, so walking past the same volcanoes costs no traffic. Leaving
 * the area sends an empty list once. The volcanoes come from the pure worldgen decisions ({@link VolcanoLocator}); the
 * client draws the plumes.
 */
public final class VolcanoPlumes {
    /** How far the server looks for volcanoes, in blocks; the client draws a plume as far (the horizon volcanoes). */
    public static final int RADIUS = 512;
    /** How often a player's list is checked: entering the area shows the plumes within ~2 s. */
    static final int CHECK_TICKS = 40;
    /** The list order: stable while the player walks, so an unchanged set compares equal. */
    private static final Comparator<Plume> STABLE = Comparator.comparingInt(Plume::x).thenComparingInt(Plume::z);

    /** Last list sent per player (absent = nothing sent, which the client treats as no plumes). */
    private static final Map<UUID, List<Plume>> SENT = new ConcurrentHashMap<>();

    private VolcanoPlumes() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(VolcanoPlumes::onPlayerTick);
        gameBus.addListener(VolcanoPlumes::onLoggedOut);
    }

    /**
     * Whether ring 3 lies within {@link #RADIUS} blocks of an overworld position (the ring itself, its transition zones
     * or that far beyond, with the boundary noise): only there can the locator find a volcano.
     */
    static boolean near(RingLayout layout, double x, double z) {
        return layout.reaches(Ring.RING_3, layout.effectiveDistance(x, z), layout.reachMargin(RADIUS, 1));
    }

    /**
     * The plumes to send: the {@link VolcanoPlumesPayload#MAX_PLUMES} nearest, in a stable order (by x, then z) so the
     * same volcanoes give an equal list wherever the player stands.
     */
    static List<Plume> nearest(List<Plume> found, double x, double z) {
        return found.stream()
                .sorted(Comparator.comparingDouble(plume -> distanceSquared(plume, x, z)))
                .limit(VolcanoPlumesPayload.MAX_PLUMES)
                .sorted(STABLE)
                .toList();
    }

    /** Whether a player is checked this tick: each player on their own tick of the cycle, spread by entity id. */
    static boolean due(long gameTime, int entityId) {
        return Math.floorMod(gameTime + entityId, CHECK_TICKS) == 0;
    }

    private static double distanceSquared(Plume plume, double x, double z) {
        double dx = plume.x() - x;
        double dz = plume.z() - z;
        return dx * dx + dz * dz;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!due(level.getGameTime(), player.getId())) {
            return;
        }
        List<Plume> plumes = List.of();
        if (level.dimension() == Level.OVERWORLD && RingWorld.isActive(level.getServer())
                && near(RingLayout.current(), player.getX(), player.getZ())) {
            plumes = nearest(VolcanoLocator.plumesNear(level, player.getBlockX(), player.getBlockZ(), RADIUS),
                    player.getX(), player.getZ());
        }
        List<Plume> previous = SENT.get(player.getUUID());
        if (plumes.equals(previous == null ? List.of() : previous)) {
            return;
        }
        SENT.put(player.getUUID(), plumes);
        PacketDistributor.sendToPlayer(player, new VolcanoPlumesPayload(plumes));
    }

    private static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        SENT.remove(event.getEntity().getUUID());
    }
}
