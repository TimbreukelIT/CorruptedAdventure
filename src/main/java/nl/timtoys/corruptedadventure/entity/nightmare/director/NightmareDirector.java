package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.timtoys.corruptedadventure.config.NightmareDirectorConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareKind;
import nl.timtoys.corruptedadventure.ring.RingWorld;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The shared Nightmare system (phase 4 steps 2 and 3, fase-4-nightmares-concept.md "Stap 2+3: gedeeld systeem"). Per
 * {@link NightmareKind} whose entity exists, in the overworld of a ring world:
 * <ul>
 *     <li>roaming: a virtual position wanders through its ring ({@link NightmareRoaming}); it never materialises there by
 *     itself. Once a second it looks for the player in its ring with the most threat at or above the threshold
 *     ({@link NightmareTargets});</li>
 *     <li>coming: the kind's {@link NightmareArrival} plays its omens every tick while the target stays (online, alive, in
 *     the ring), then makes it appear; the director sets the strength and the threat of the visited players drops to 0;</li>
 *     <li>present: the entity is followed by UUID. Missing or frozen (chunk unloaded, restart without its players) for
 *     too long, it goes back to virtual, healed. The entity checks in every second ({@link #isCurrent}); an old copy
 *     that turns up later (from a chunk) removes itself, so there is never more than one;</li>
 *     <li>retreat (everyone gone or dead): back to virtual, not defeated. Defeat (0 HP): defeated until
 *     {@code returnDays} later, one defeat more, so it comes back stronger.</li>
 * </ul>
 * {@code /summon}ed Nightmares are not directed: they never check in and never count. All state lives in
 * {@link NightmareDirectorData}; nothing here is client code.
 */
public final class NightmareDirector {
    private static final int SECOND = 20;

    private static final NightmareDirector SERVER = new NightmareDirector(NightmareRings.LAYOUT,
            level -> level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(NightmareDirectorData.factory(), NightmareDirectorData.FILE_NAME),
            NightmareKind::arrival, true);

    private final NightmareRings rings;
    private final Function<ServerLevel, NightmareDirectorData> storage;
    private final Function<NightmareKind, NightmareArrival> arrivals;
    private final boolean ringWorldOnly;

    private NightmareDirector(NightmareRings rings, Function<ServerLevel, NightmareDirectorData> storage,
                              Function<NightmareKind, NightmareArrival> arrivals, boolean ringWorldOnly) {
        this.rings = rings;
        this.storage = storage;
        this.arrivals = arrivals;
        this.ringWorldOnly = ringWorldOnly;
    }

    /** A director of its own for GameTests: its own data, ring lookup and arrivals, in whatever level it is ticked. */
    public static NightmareDirector forTest(NightmareRings rings, NightmareDirectorData data,
                                            Function<NightmareKind, NightmareArrival> arrivals) {
        return new NightmareDirector(rings, level -> data, arrivals, false);
    }

    /** The server's director (its data in the overworld's storage). */
    public static NightmareDirector server() {
        return SERVER;
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(NightmareDirector::onLevelTick);
        NightmareThreats.register(gameBus);
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        // runsNormally: under /tick freeze the game time stands still, so the once-a-second checks would fire every tick.
        if (event.getLevel() instanceof ServerLevel level && Level.OVERWORLD.equals(level.dimension())
                && level.tickRateManager().runsNormally() && RingWorld.isActive(level.getServer())) {
            SERVER.tick(level, level.getGameTime());
        }
    }

    /** Whether this director works in {@code level}: the server's only in the overworld of a ring world. */
    public boolean worksIn(ServerLevel level) {
        return !ringWorldOnly || (Level.OVERWORLD.equals(level.dimension()) && RingWorld.isActive(level.getServer()));
    }

    /** The saved state; call only where {@link #worksIn} holds. */
    public NightmareDirectorData data(ServerLevel level) {
        return storage.apply(level);
    }

    /** The strength its next arrival (or the present one) has. */
    public static float strength(NightmareState state) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        return NightmareState.strength(state.defeats(), config.strengthPerReturn.get(), config.maxReturns.get());
    }

    /**
     * One tick at game time {@code now} (the overworld's; GameTests pass their own): omens every tick, everything else
     * once a second ({@code now} a multiple of 20).
     */
    public void tick(ServerLevel level, long now) {
        NightmareDirectorData data = data(level);
        boolean second = now % SECOND == 0;
        for (NightmareKind kind : NightmareKind.values()) {
            Optional<EntityType<? extends Nightmare>> type = kind.type();
            if (type.isEmpty()) {
                continue;
            }
            NightmareState state = data.get(kind);
            switch (state.phase()) {
                case ROAMING -> {
                    if (second && (roam(level, kind, state, now) | seek(level, kind, state, now))) {
                        data.setDirty();
                    }
                }
                case COMING -> {
                    come(level, kind, type.get(), state, now);
                    data.setDirty();
                }
                case PRESENT -> {
                    if (second) {
                        watch(level, state);
                        data.setDirty();
                    }
                }
                case DEFEATED -> {
                    if (now >= state.defeatedUntil()) {
                        state.roam();
                        state.nextRoamTick(now);
                        data.setDirty();
                    }
                }
            }
        }
    }

    /** Whether the state changed. */
    private boolean roam(ServerLevel level, NightmareKind kind, NightmareState state, long now) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        long interval = config.roamIntervalSeconds.get() * (long) SECOND;
        if (!state.placed()) {
            NightmareRoaming.Spot start = NightmareRoaming.start(kind.ring(), rings, level.getRandom());
            state.moveTo(start.x(), start.z(), start.heading());
            state.nextRoamTick(now + interval);
            return true;
        }
        if (now < state.nextRoamTick()) {
            return false;
        }
        NightmareRoaming.Spot from = new NightmareRoaming.Spot(state.x(), state.z(), state.heading());
        NightmareRoaming.Spot to = NightmareRoaming.step(from, config.roamStepBlocks.get(), kind.ring(), rings, level.getRandom());
        state.moveTo(to.x(), to.z(), to.heading());
        state.nextRoamTick(now + interval);
        if (to.x() != from.x() || to.z() != from.z()) {
            arrivals.apply(kind).roamed(level, surface(level, from.x(), from.z()), surface(level, to.x(), to.z()));
        }
        return true;
    }

    /** The surface at x, z where the chunk is loaded, else the sea level (an unloaded heightmap reads as empty). */
    private static BlockPos surface(ServerLevel level, double x, double z) {
        BlockPos column = BlockPos.containing(x, level.getSeaLevel(), z);
        if (!level.hasChunkAt(column)) {
            return column;
        }
        return new BlockPos(column.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(),
                column.getZ()), column.getZ());
    }

    /** Whether a coming started. */
    private boolean seek(ServerLevel level, NightmareKind kind, NightmareState state, long now) {
        NightmareDirectorConfig config = ServerConfig.NIGHTMARE_DIRECTOR;
        if (!config.arrivals.get() || isPeaceful(level) || now < state.nextSeekTick()) {
            return false;
        }
        List<NightmareTargets.Candidate> candidates = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (!(player instanceof FakePlayer) && Nightmare.isFighter(player)
                    && rings.ringAt(player.getX(), player.getZ()) == kind.ring()) {
                candidates.add(new NightmareTargets.Candidate(player.getUUID(), NightmareThreats.of(player).get(kind.ring())));
            }
        }
        ServerPlayer target = NightmareTargets.choose(candidates, config.threatThreshold.get().floatValue())
                .map(uuid -> level.getServer().getPlayerList().getPlayer(uuid)).orElse(null);
        if (target == null) {
            return false;
        }
        startComing(level, kind, state, target, false);
        return true;
    }

    private void startComing(ServerLevel level, NightmareKind kind, NightmareState state, ServerPlayer target,
                             boolean forced) {
        state.coming(target.getUUID(), arrivals.apply(kind).place(level, target), forced);
    }

    private void come(ServerLevel level, NightmareKind kind, EntityType<? extends Nightmare> type, NightmareState state,
                      long now) {
        NightmareArrival arrival = arrivals.apply(kind);
        ServerPlayer target = state.target() == null ? null : level.getServer().getPlayerList().getPlayer(state.target());
        // Peaceful would despawn it at once: the omens stop, nothing comes.
        if (isPeaceful(level) || !canBeVisited(level, kind, target, state.forced())) {
            arrival.cancelled(level, state.place());
            state.roam();
            return;
        }
        int omenTicks = Math.max(0, arrival.omenTicks());
        int tick = state.omenTick();
        state.omenTick(tick + 1);
        if (tick < omenTicks) {
            arrival.omen(level, target, state.place(), tick);
            return;
        }
        // After the omens: ask for room once a second.
        if ((tick - omenTicks) % SECOND != 0) {
            return;
        }
        float strength = strength(state);
        Nightmare nightmare = arrival.appear(level, type, target, state.place(), strength);
        if (nightmare == null || nightmare.isRemoved()) {
            state.appearTries(state.appearTries() + 1);
            if (state.appearTries() >= ServerConfig.NIGHTMARE_DIRECTOR.appearRetrySeconds.get()) {
                arrival.cancelled(level, state.place());
                state.roam();
                state.nextSeekTick(now + ServerConfig.NIGHTMARE_DIRECTOR.failedArrivalCooldownSeconds.get() * (long) SECOND);
            }
            return;
        }
        state.present(nightmare.getUUID());
        state.moveTo(nightmare.getX(), nightmare.getZ(), state.heading());
        nightmare.direct(this, strength);
        NightmareThreats.of(target).reset(kind.ring());
        for (Player fighter : nightmare.fighters()) {
            NightmareThreats.of(fighter).reset(kind.ring());
        }
    }

    /** No Nightmares on Peaceful: vanilla would despawn a monster at once. */
    public static boolean isPeaceful(ServerLevel level) {
        return level.getDifficulty() == Difficulty.PEACEFUL;
    }

    /** The target is still there to visit: online, alive, here, and (unless forced) a fighter in the ring. */
    private boolean canBeVisited(ServerLevel level, NightmareKind kind, @Nullable ServerPlayer target, boolean forced) {
        if (target == null || !target.isAlive() || target.level() != level || target.isSpectator()) {
            return false;
        }
        return forced || (Nightmare.isFighter(target) && rings.ringAt(target.getX(), target.getZ()) == kind.ring());
    }

    private void watch(ServerLevel level, NightmareState state) {
        Entity entity = state.entity() == null ? null : level.getEntity(state.entity());
        if (entity instanceof Nightmare nightmare && nightmare.isAlive()
                && level.isPositionEntityTicking(nightmare.blockPosition())) {
            state.missingTicks(0);
            state.moveTo(nightmare.getX(), nightmare.getZ(), state.heading());
            return;
        }
        state.missingTicks(state.missingTicks() + SECOND);
        if (state.missingTicks() >= ServerConfig.NIGHTMARE_DIRECTOR.missingSeconds.get() * SECOND) {
            // A loaded but frozen copy goes now; one in an unloaded chunk removes itself once it loads (isCurrent).
            if (entity != null && !entity.isRemoved()) {
                entity.discard();
            }
            state.roam();
        }
    }

    /** Whether {@code nightmare} is the present entity of its kind here; if not, it is a stale copy and must go. */
    public boolean isCurrent(Nightmare nightmare) {
        NightmareState state = stateOf(nightmare);
        return state != null && state.phase() == NightmareState.Phase.PRESENT && nightmare.getUUID().equals(state.entity());
    }

    /** It retreated (everyone gone or dead): back to virtual where it stood, healed, not defeated. */
    public void retreated(Nightmare nightmare) {
        if (isCurrent(nightmare)) {
            NightmareState state = stateOf(nightmare);
            state.roam();
            state.moveTo(nightmare.getX(), nightmare.getZ(), state.heading());
            state.nextRoamTick(nightmare.level().getGameTime());
            data((ServerLevel) nightmare.level()).setDirty();
        }
    }

    /** It was beaten (0 HP): defeated for {@code returnDays}, one defeat more. */
    public void defeated(Nightmare nightmare) {
        if (isCurrent(nightmare)) {
            NightmareState state = stateOf(nightmare);
            state.defeat(nightmare.level().getGameTime() + ServerConfig.NIGHTMARE_DIRECTOR.returnTicks());
            state.moveTo(nightmare.getX(), nightmare.getZ(), state.heading());
            data((ServerLevel) nightmare.level()).setDirty();
        }
    }

    @Nullable
    private NightmareState stateOf(Nightmare nightmare) {
        if (!(nightmare.level() instanceof ServerLevel level) || !worksIn(level)) {
            return null;
        }
        return NightmareKind.of(nightmare.getType()).map(kind -> data(level).get(kind)).orElse(null);
    }

    /** What {@link #arrive} answers. */
    public enum ArriveResult {
        COMING, NO_ENTITY, ALREADY_COMING, ALREADY_PRESENT, PEACEFUL
    }

    /**
     * {@code /ca nightmare arrive}: its omens start now for {@code target}, whatever the threat, and also from defeated;
     * the target need not stay in the ring.
     */
    public ArriveResult arrive(ServerLevel level, NightmareKind kind, ServerPlayer target) {
        if (kind.type().isEmpty()) {
            return ArriveResult.NO_ENTITY;
        }
        if (isPeaceful(level)) {
            return ArriveResult.PEACEFUL;
        }
        NightmareState state = data(level).get(kind);
        if (state.phase() == NightmareState.Phase.COMING) {
            return ArriveResult.ALREADY_COMING;
        }
        if (state.phase() == NightmareState.Phase.PRESENT) {
            return ArriveResult.ALREADY_PRESENT;
        }
        startComing(level, kind, state, target, true);
        data(level).setDirty();
        return ArriveResult.COMING;
    }

    /**
     * {@code /ca nightmare reset}: removes its entity if loaded (an unloaded one removes itself when it loads), stops its
     * omens and forgets its defeats; it roams on from its virtual position.
     */
    public void reset(ServerLevel level, NightmareKind kind) {
        NightmareState state = data(level).get(kind);
        if (state.phase() == NightmareState.Phase.PRESENT && state.entity() != null) {
            Entity entity = level.getEntity(state.entity());
            if (entity != null) {
                entity.discard();
            }
        } else if (state.phase() == NightmareState.Phase.COMING) {
            arrivals.apply(kind).cancelled(level, state.place());
        }
        state.reset();
        state.nextRoamTick(level.getGameTime());
        data(level).setDirty();
    }
}
