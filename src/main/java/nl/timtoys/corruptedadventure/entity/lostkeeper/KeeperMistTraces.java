package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Lost Keeper's traces while it roams ({@link KeeperArrival#roamed}): a wisp of its mist with a faint glow on the
 * water near its virtual position. A small mist bank (synced like its own, {@link KeeperMistBanks#TRACES}) that rises,
 * hangs and lifts after {@code traceSeconds}, plus drifting spray and glow particles. Only in memory and at most
 * {@code maxTraces} at once: nothing is saved and nothing in the world changes. Server-side, on the game bus.
 */
public final class KeeperMistTraces {
    /** Ticks over which a wisp rises at its start and lifts at its end. */
    static final int RISE_TICKS = 100;
    static final int LIFT_TICKS = 400;
    private static final int SYNC_TICKS = 20;
    private static final int PARTICLE_TICKS = 5;
    /** Players within this of a wisp get its mist and see its particles. */
    private static final double RANGE = 96.0;
    private static final int SEARCH_STEP = 2;
    /** Bank ids cycle through this many slots (more than {@code maxTraces} can be). */
    private static final int ID_SLOTS = 16;

    private static final List<Trace> TRACES = new ArrayList<>();
    private static int nextSlot;

    private KeeperMistTraces() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(LevelTickEvent.Post.class, KeeperMistTraces::onLevelTick);
        gameBus.addListener(ServerStoppedEvent.class, event -> clear());
    }

    /**
     * Leaves a wisp on the water at or near {@code at} (within {@code traceWaterSearch} blocks, loaded chunks only); none
     * without water. Whether one was left. A wisp beyond {@code maxTraces} lifts the oldest at once.
     */
    public static boolean leave(ServerLevel level, BlockPos at) {
        LostKeeperConfig config = ServerConfig.LOST_KEEPER;
        Vec3 surface = water(level, at, config.traceWaterSearch.get());
        if (surface == null) {
            return false;
        }
        while (TRACES.size() >= config.maxTraces.get()) {
            Trace oldest = TRACES.remove(0);
            // Lifted where it hangs: its own level, not necessarily the new one's.
            ServerLevel its = level.getServer().getLevel(oldest.dimension);
            if (its != null) {
                oldest.send(its, 0.0F, config);
            }
        }
        TRACES.add(new Trace(level.dimension(), surface, KeeperMistBanks.TRACES - nextSlot, config.traceSeconds.get() * 20));
        nextSlot = (nextSlot + 1) % ID_SLOTS;
        return true;
    }

    /** The wisps hanging now (for the GameTests). */
    public static int count() {
        return TRACES.size();
    }

    /** Lets every wisp vanish at once without a word to the clients (server stop; the GameTests). */
    public static void clear() {
        TRACES.clear();
    }

    /** 0–1: a wisp's mist at {@code age} ticks of {@code life}: rising, hanging, lifting. */
    static float strength(int age, int life) {
        if (age < 0 || age >= life) {
            return 0.0F;
        }
        float rise = Math.min(1.0F, age / (float) RISE_TICKS);
        float lift = Math.min(1.0F, (life - age) / (float) Math.min(LIFT_TICKS, Math.max(1, life / 2)));
        return Math.min(rise, lift);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (TRACES.isEmpty() || !(event.getLevel() instanceof ServerLevel level) || level.tickRateManager().isFrozen()) {
            return;
        }
        LostKeeperConfig config = ServerConfig.LOST_KEEPER;
        TRACES.removeIf(trace -> trace.dimension == level.dimension() && !trace.tick(level, config));
    }

    /** The water's surface at or near {@code at}: the nearest column whose top is water, or null. */
    @Nullable
    private static Vec3 water(ServerLevel level, BlockPos at, int reach) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Vec3 best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (int dx = -reach; dx <= reach; dx += SEARCH_STEP) {
            for (int dz = -reach; dz <= reach; dz += SEARCH_STEP) {
                int distance = dx * dx + dz * dz;
                if (distance > reach * reach || distance >= bestDistance) {
                    continue;
                }
                int x = at.getX() + dx;
                int z = at.getZ() + dz;
                pos.set(x, at.getY(), z);
                if (!level.hasChunkAt(pos)) {
                    continue;
                }
                pos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1);
                if (level.getFluidState(pos).is(FluidTags.WATER)) {
                    best = new Vec3(x + 0.5, pos.getY() + 1.0, z + 0.5);
                    bestDistance = distance;
                }
            }
        }
        return best;
    }

    private static final class Trace {
        private final ResourceKey<Level> dimension;
        private final Vec3 surface;
        private final int id;
        private final int life;
        private int age;

        private Trace(ResourceKey<Level> dimension, Vec3 surface, int id, int life) {
            this.dimension = dimension;
            this.surface = surface;
            this.id = id;
            this.life = life;
        }

        /** Whether it still hangs. */
        private boolean tick(ServerLevel level, LostKeeperConfig config) {
            age++;
            float strength = strength(age, life);
            if (age >= life) {
                send(level, 0.0F, config);
                return false;
            }
            if (age % SYNC_TICKS == 0) {
                send(level, strength, config);
            }
            if (age % PARTICLE_TICKS == 0 && level.hasChunkAt(BlockPos.containing(surface))) {
                KeeperEffects.trace(level, surface, config.traceRadius.get(), strength, age);
            }
            return true;
        }

        private void send(ServerLevel level, float strength, LostKeeperConfig config) {
            KeeperMistBanks.send(level, id, surface, config.traceRadius.get(), config.traceFade.get(),
                    config.traceVisibility.get(), Mth.clamp(strength, 0.0F, 1.0F), RANGE);
        }
    }
}
