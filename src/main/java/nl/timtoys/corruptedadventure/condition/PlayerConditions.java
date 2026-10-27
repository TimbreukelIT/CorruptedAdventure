package nl.timtoys.corruptedadventure.condition;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.network.ConditionSyncPayload;
import nl.timtoys.corruptedadventure.registry.ModAttachments;

/**
 * Server API for a player's meters: instant changes (items, commands) and the sync to that player's own client. Other
 * players never receive someone else's meters.
 * <p>
 * The client gets a full snapshot on login, respawn and dimension change (the client makes a new player object then,
 * with an empty attachment), and otherwise only when a rounded value changed.
 */
public final class PlayerConditions {
    private PlayerConditions() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> syncAll(event.getEntity()));
        gameBus.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> syncAll(event.getEntity()));
        gameBus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> syncAll(event.getEntity()));
    }

    public static ConditionMeters meters(ServerPlayer player) {
        return player.getData(ModAttachments.CONDITION_METERS);
    }

    public static float get(ServerPlayer player, Condition condition) {
        return meters(player).get(condition);
    }

    /** Sets the meter (clamped 0–100) and syncs at once; threshold effects follow on the next handler tick. */
    public static void set(ServerPlayer player, Condition condition, float value) {
        ConditionMeters meters = meters(player);
        meters.set(condition, value);
        syncIfChanged(player, meters);
    }

    /** Adds to the meter (negative: e.g. a water bottle −30) and syncs at once. */
    public static void add(ServerPlayer player, Condition condition, float delta) {
        ConditionMeters meters = meters(player);
        meters.add(condition, delta);
        syncIfChanged(player, meters);
    }

    static void syncIfChanged(ServerPlayer player, ConditionMeters meters) {
        if (meters.roundedChanged()) {
            send(player, meters);
        }
    }

    private static void syncAll(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            send(serverPlayer, meters(serverPlayer));
        }
    }

    private static void send(ServerPlayer player, ConditionMeters meters) {
        // A connection without our channel (a GameTest mock player) would throw; it gets nothing, and stays unsynced.
        if (!player.connection.hasChannel(ConditionSyncPayload.TYPE)) {
            return;
        }
        PacketDistributor.sendToPlayer(player, new ConditionSyncPayload(meters.roundedValues()));
        meters.markSynced();
    }
}
