package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * How one Nightmare comes (phase 4 steps 2 and 3, fase-4-nightmares-concept.md "Stap 2+3: gedeeld systeem"): its omens,
 * its appearance and the traces of its roaming. The director ({@code director/NightmareDirector}) decides when and for
 * whom; this decides what it looks like. Server-side only, called on the server thread, always in the overworld.
 * Registered per kind with {@link NightmareKind#arrival(NightmareArrival)}; without one a kind uses
 * {@link DefaultNightmareArrival}.
 */
public interface NightmareArrival {
    /** Where it will come, chosen once as the omens start (the target may walk on); by default where the target stands. */
    default BlockPos place(ServerLevel level, ServerPlayer target) {
        return target.blockPosition();
    }

    /** How many ticks the omens last before it appears. */
    int omenTicks();

    /** One tick of the omens, every tick from {@code tick} 0 to {@link #omenTicks()} - 1. */
    void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick);

    /**
     * Spawns it (added to the level) and plays its coming. {@code strength} is the factor on its health and damage (1,
     * 1.25, … after each defeat); the director sets it on the returned Nightmare, so pass it on only for effects. Null =
     * no room right now: the director asks again a second later, a limited number of times.
     */
    @Nullable
    Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                     float strength);

    /** The omens stopped before it appeared (the target left, logged out or died): clean up what they left. */
    default void cancelled(ServerLevel level, BlockPos place) {
    }

    /**
     * Its virtual position moved from {@code from} to {@code to} while it roams (traces in the world, only where loaded:
     * check {@code level.hasChunkAt}). The y of both is the surface where the chunk is loaded, else the sea level.
     */
    default void roamed(ServerLevel level, BlockPos from, BlockPos to) {
    }
}
