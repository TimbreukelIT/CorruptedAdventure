package nl.timtoys.corruptedadventure.worldgen.border;

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.border.WorldBorder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.RingSettings;
import nl.timtoys.corruptedadventure.config.RingSettingsSnapshot;
import nl.timtoys.corruptedadventure.ring.RingWorld;

/**
 * World border of the {@code corrupted_world} preset (server-side; clients only receive vanilla border packets).
 * <p>
 * The overworld border is set once to the void edge, and only while it is still at the vanilla default, so a border an
 * admin changed later is kept. Vanilla persists only the overworld border; every scaled dimension (the Nether) is
 * derived from it on each start and follows every later change, divided by its {@code coordinate_scale}.
 * Worlds of other presets are left alone.
 */
public final class RingWorldBorder {
    private RingWorldBorder() {
    }

    public static void register(IEventBus gameBus) {
        // ServerStartingEvent: after loadLevel(), which applies the saved border at its very end, and before any join.
        gameBus.addListener(RingWorldBorder::onServerStarting);
        // Vanilla sends the overworld border to every dimension right before these events; correct it afterwards.
        gameBus.addListener(RingWorldBorder::onPlayerLoggedIn);
        gameBus.addListener(RingWorldBorder::onPlayerChangedDimension);
        gameBus.addListener(RingWorldBorder::onPlayerRespawn);
    }

    /** Border diameter (vanilla border size) for a new world: the outer radius of the void edge, on both sides of 0,0. */
    static double initialSize(RingSettings settings) {
        return 2.0 * settings.voidOuterRadius();
    }

    /** Untouched border: vanilla default size and center, not moving. Any admin change keeps it out of our hands. */
    static boolean isVanillaDefault(WorldBorder border) {
        WorldBorder.Settings vanilla = WorldBorder.DEFAULT_SETTINGS;
        return border.getSize() == vanilla.getSize() && border.getLerpRemainingTime() == 0L
                && border.getCenterX() == vanilla.getCenterX() && border.getCenterZ() == vanilla.getCenterZ();
    }

    private static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (!RingWorld.isActive(server)) {
            return;
        }
        WorldBorder overworld = server.overworld().getWorldBorder();
        for (ServerLevel level : server.getAllLevels()) {
            double scale = level.dimensionType().coordinateScale();
            if (level != server.overworld() && scale != 1.0) {
                ScaledBorderListener listener = new ScaledBorderListener(overworld, level.getWorldBorder(), scale,
                        () -> server.getPlayerList().broadcastAll(initializePacket(overworld, level), level.dimension()));
                overworld.addListener(listener);
                listener.derive();
            }
        }
        if (isVanillaDefault(overworld)) {
            overworld.setCenter(0.0, 0.0);
            overworld.setSize(initialSize(RingSettingsSnapshot.current()));
            CorruptedAdventure.LOGGER.info("World border set to {} blocks around 0,0", overworld.getSize());
        }
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sendScaledBorder(event.getEntity());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sendScaledBorder(event.getEntity());
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sendScaledBorder(event.getEntity());
    }

    private static void sendScaledBorder(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        if (level.dimensionType().coordinateScale() != 1.0 && RingWorld.isActive(server)) {
            player.connection.send(initializePacket(server.overworld().getWorldBorder(), level));
        }
    }

    private static ClientboundInitializeBorderPacket initializePacket(WorldBorder overworld, ServerLevel level) {
        return new ClientboundInitializeBorderPacket(ScaledBorderListener.clientView(overworld, level.getWorldBorder()));
    }
}
