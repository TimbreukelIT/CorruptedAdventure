package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.VanillaGameEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;

/**
 * The sounds that break the silence (package 97): a note block, a goat horn (any instrument, and the echo shell, which
 * sends the same event), a playing jukebox (vanilla repeats its event every second while the disc plays) and a bell, also
 * when redstone plays them. Heard through
 * NeoForge's {@link VanillaGameEvent}, which every server-side game event fires before vanilla's listeners; nothing on
 * the client. Every player in the Nether within {@code soundReliefRadius} of the sound remembers when they last heard
 * one, in server ticks; {@link SilenceRule} asks whether that is at most {@code soundReliefSeconds} ago. In memory only,
 * not saved (after a login nothing was heard); weak keys, so a player object that is gone is forgotten. Server thread
 * only. Never cancels the event.
 */
final class SilenceSounds {
    private static final Map<ServerPlayer, Integer> LAST_HEARD = new WeakHashMap<>();

    private SilenceSounds() {
    }

    static void onGameEvent(VanillaGameEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.NETHER
                || level.players().isEmpty()) {
            return;
        }
        Vec3 position = event.getEventPosition();
        if (!breaksSilence(event.getVanillaEvent(),
                () -> level.getBlockState(BlockPos.containing(position)).is(Blocks.BELL))) {
            return;
        }
        int radius = ServerConfig.SILENCE.soundReliefRadius.get();
        int now = level.getServer().getTickCount();
        for (ServerPlayer player : level.players()) {
            if (inRange(position, player.position(), radius)) {
                LAST_HEARD.put(player, now);
            }
        }
    }

    /**
     * Whether a game event is one of the four sounds. A bell has no event of its own: it rings with a block change at
     * its position, so {@code isBell} is only asked for a block change (doors, levers and the like are no sound).
     */
    static boolean breaksSilence(Holder<GameEvent> event, BooleanSupplier isBell) {
        return event.is(GameEvent.NOTE_BLOCK_PLAY) || event.is(GameEvent.INSTRUMENT_PLAY)
                || event.is(GameEvent.JUKEBOX_PLAY) || event.is(GameEvent.BLOCK_CHANGE) && isBell.getAsBoolean();
    }

    /** At most {@code radius} blocks between the sound and the player's feet (3D). */
    static boolean inRange(Vec3 sound, Vec3 feet, int radius) {
        return sound.distanceToSqr(feet) <= (double) radius * radius;
    }

    /** Whether the player heard a sound at most {@code reliefTicks} before {@code now}. */
    static boolean relieved(ServerPlayer player, int now, int reliefTicks) {
        return relieved(LAST_HEARD.get(player), now, reliefTicks);
    }

    static boolean relieved(Integer lastHeard, int now, int reliefTicks) {
        return lastHeard != null && now - lastHeard <= reliefTicks;
    }
}
