package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * The trace The Unfinished leaves while he roams (phase 4 step 2): a place where the rain hangs still in mid-air for
 * {@code traceSeconds} ({@code still_raindrop} particles, sent by the server to whoever is near). At most {@code traceMax}
 * at once, the oldest goes first; not saved (a restart clears them). Nothing in the world changes. Server-side.
 */
public final class UnfinishedTraces {
    private static final int EVERY = 10;
    private static final int DROPS = 24;
    /** Spread of the drops round the trace (blocks): a column of hanging rain above the ground. */
    private static final double SPREAD = 2.0;
    private static final double HEIGHT = 2.5;
    private static final double LIFT = 3.5;

    private record Trace(ResourceKey<Level> level, BlockPos at, long until) {
    }

    private static final Deque<Trace> TRACES = new ArrayDeque<>();

    private UnfinishedTraces() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LevelTickEvent.Post.class, UnfinishedTraces::onLevelTick);
        gameBus.addListener(ServerStoppedEvent.class, event -> TRACES.clear());
    }

    /** A trace at {@code at} (on the ground) in {@code level}, from now. */
    static void add(ServerLevel level, BlockPos at) {
        int max = ServerConfig.UNFINISHED.arrival.traceMax.get();
        if (max <= 0) {
            return;
        }
        while (TRACES.size() >= max) {
            TRACES.pollFirst();
        }
        long until = level.getGameTime() + ServerConfig.UNFINISHED.arrival.traceSeconds.get() * 20L;
        TRACES.addLast(new Trace(level.dimension(), at.immutable(), until));
    }

    public static int count() {
        return TRACES.size();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (TRACES.isEmpty() || !(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % EVERY != 0) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Trace> it = TRACES.iterator();
        while (it.hasNext()) {
            Trace trace = it.next();
            if (!trace.level().equals(level.dimension())) {
                continue;
            }
            if (now >= trace.until()) {
                it.remove();
                continue;
            }
            BlockPos at = trace.at();
            if (level.hasChunkAt(at)) {
                level.sendParticles(ModParticleTypes.STILL_RAINDROP.get(), at.getX() + 0.5, at.getY() + LIFT,
                        at.getZ() + 0.5, DROPS, SPREAD, HEIGHT, SPREAD, 0.0);
            }
        }
    }
}
