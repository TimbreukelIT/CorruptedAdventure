package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedArrivalConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.network.UnfinishedStandstillPayload;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * How The Unfinished comes (fase-4-nightmares-concept.md "Stap 2 voor ring 2–5" and "Ring 5 — De Onvoltooide"), for the
 * director. First the standstill falls round the place, for the players near it only ({@link UnfinishedStandstillPayload}:
 * frozen rain and clouds, every sound drained away); after {@code silenceSeconds} you hear him climb, grips that come
 * faster and nearer. In the last {@code tearWarnTicks} the ground over the tear flickers. Then he tears it open down to
 * the void ({@link UnfinishedTear}, open for {@code tearOpenSeconds}; the void throws out whoever falls in) and climbs out,
 * over the rim toward his target ({@link TheUnfinished#climbOut}). When the tear loads back in, a small shallow void scar
 * stays in its middle (voidstone floor and rim, natural ground only, never a build). Cancelled omens lift the standstill;
 * the flicker mends itself. While he roams, the rain hangs still where he passed ({@link UnfinishedTraces}).
 */
public final class UnfinishedArrival implements NightmareArrival {
    private static final int[] ANGLES = {0, 30, -30, 60, -60, 90, -90, 120, -120, 150, -150, 180};
    private static final double[] DISTANCE_FACTORS = {1.0, 0.7, 1.3, 0.5};
    /** Most blocks between the target's feet and the ground of the place. */
    private static final double MAX_HEIGHT_GAP = 16.0;
    /** Blocks beyond the standstill's radius that still get it (it thins out to nothing there on the client). */
    private static final double SEND_MARGIN = 16.0;
    /** Ticks one flickering glitch block stays before the ground under it comes back by itself. */
    private static final int FLICKER_TICKS = 6;
    /** Blocks past the tear's edge where he steps out onto the rim. */
    private static final double RIM = 3.0;
    /** The grips of his climb: one every this many ticks at first, at the end one every {@link #LAST_GRIP}. */
    private static final double FIRST_GRIP = 50.0;
    private static final double LAST_GRIP = 12.0;
    /** Blocks below the ground the first grip sounds from, above his climb's start. */
    private static final double FIRST_GRIP_DEPTH = 12.0;

    /**
     * The tear as the flicker showed it, per place of an arrival under way, so that it opens the same way round; one
     * instance serves every arrival of his kind. Removed when the arrival appears or is cancelled.
     */
    private final Map<BlockPos, UnfinishedTear.Plan> planned = new HashMap<>();

    @Override
    public BlockPos place(ServerLevel level, ServerPlayer target) {
        double distance = config().arrivalDistance.get();
        // Ground that can tear first; then any ground; never no place at all.
        BlockPos spot = spot(level, target, distance, true);
        if (spot == null) {
            spot = spot(level, target, distance, false);
        }
        return spot != null ? spot : target.blockPosition();
    }

    @Override
    public int omenTicks() {
        return config().omenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        UnfinishedArrivalConfig config = config();
        int total = omenTicks();
        if (tick % 20 == 0) {
            standstill(level, target, place, total - tick + config.climbTicks.get() + config.standstillLingerSeconds.get() * 20);
        }
        int silence = Math.min(config.silenceSeconds.get() * 20, total);
        if (tick >= silence && level.hasChunkAt(place)) {
            climbing(level, place, tick - silence, total - silence);
        }
        int warn = Math.min(config.tearWarnTicks.get(), total);
        if (tick >= total - warn && level.hasChunkAt(place)) {
            warn(level, target, place, tick - (total - warn), warn);
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        if (!level.hasChunkAt(place)) {
            return null;
        }
        UnfinishedArrivalConfig config = config();
        Vec3 centre = Vec3.atBottomCenterOf(place);
        UnfinishedTear.Plan shown = planned.get(place);
        Vec3 toward = shown != null ? centre.add(shown.forward()) : target.position();
        settleFlicker(level, place);
        // Planned again now: the ground may have changed since the flicker (a block placed on it stays).
        UnfinishedTear.Plan plan = plan(level, place, toward, config);
        Vec3 forward = plan.forward();
        double out = config.tearWidth.get() / 2.0 + RIM;
        double ex = centre.x + forward.x * out;
        double ez = centre.z + forward.z * out;
        OptionalDouble ground = UnfinishedGround.surface(level, ex, ez);
        Vec3 to = new Vec3(ex, ground.orElse(place.getY()), ez);
        Vec3 from = new Vec3(centre.x, Math.max(level.getMinBuildHeight(), place.getY() - config.climbDepth.get()), centre.z);
        Entity created = type.create(level);
        if (!(created instanceof TheUnfinished boss)) {
            return null;
        }
        float yaw = TheUnfinished.yawTo(centre, target.position());
        boss.moveTo(from.x, from.y, from.z, yaw, 0.0F);
        if (!level.addFreshEntity(boss)) {
            return null;
        }
        boss.climbOut(from, to, yaw, config.climbTicks.get());
        boss.setTarget(target);
        open(level, plan, config);
        UnfinishedEffects.tearOpen(level, centre, config.tearLength.get());
        standstill(level, target, place, config.climbTicks.get() + config.standstillLingerSeconds.get() * 20);
        return boss;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        settleFlicker(level, place);
        UnfinishedStandstillPayload lift = new UnfinishedStandstillPayload(place.getX() + 0.5, place.getZ() + 0.5, 1.0F, 0,
                seed(place));
        for (ServerPlayer player : level.players()) {
            send(player, lift);
        }
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        if (level.hasChunkAt(to) && level.getRandom().nextDouble() < config().traceChance.get()) {
            UnfinishedTraces.add(level, to);
        }
    }

    // ------------------------------------------------------------------------------------------------ omens

    /** The standstill for {@code ticks} round the place, to the players near it (and the target). */
    private static void standstill(ServerLevel level, ServerPlayer target, BlockPos place, int ticks) {
        float radius = config().standstillRadius.get().floatValue();
        double reach = radius + SEND_MARGIN;
        double x = place.getX() + 0.5;
        double z = place.getZ() + 0.5;
        UnfinishedStandstillPayload payload = new UnfinishedStandstillPayload(x, z, radius, ticks, seed(place));
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - x;
            double dz = player.getZ() - z;
            if (player == target || dx * dx + dz * dz <= reach * reach) {
                send(player, payload);
            }
        }
    }

    private static void send(ServerPlayer player, UnfinishedStandstillPayload payload) {
        if (player.connection.hasChannel(UnfinishedStandstillPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** His grips in the silence: slow and deep at first, then faster and nearer, {@code step} of {@code span} ticks. */
    private static void climbing(ServerLevel level, BlockPos place, int step, int span) {
        double now = grips(step, span);
        if (step > 0 && Math.floor(now) == Math.floor(grips(step - 1, span))) {
            return;
        }
        float progress = span <= 0 ? 1.0F : Math.min(1.0F, step / (float) span);
        double depth = Mth.lerp(progress, config().climbDepth.get() + FIRST_GRIP_DEPTH, 2.0);
        Vec3 at = new Vec3(place.getX() + 0.5, place.getY() - depth, place.getZ() + 0.5);
        RandomSource random = level.getRandom();
        boolean heave = (int) Math.floor(now) % 3 == 2;
        UnfinishedEffects.climbGrip(level, at, heave, 0.9F + random.nextFloat() * 0.2F);
    }

    /**
     * Grips done after {@code step} of {@code span} ticks: one per {@link #FIRST_GRIP} ticks at the start, speeding up
     * evenly to one per {@link #LAST_GRIP} at the end. Pure, for tests.
     */
    static double grips(int step, int span) {
        double first = 1.0 / FIRST_GRIP;
        double last = 1.0 / LAST_GRIP;
        double length = Math.max(1, span);
        return step * first + (last - first) * step * (double) step / (2.0 * length);
    }

    /** The last ticks: the tear's top blocks flicker as glitch blocks, more of them toward the end. */
    private void warn(ServerLevel level, ServerPlayer target, BlockPos place, int step, int span) {
        UnfinishedTear.Plan plan = planned.computeIfAbsent(place.immutable(),
                at -> plan(level, at, target.position(), config()));
        Vec3 centre = Vec3.atBottomCenterOf(place);
        double length = config().tearLength.get();
        if (step % 20 == 0) {
            UnfinishedEffects.tearGroan(level, centre);
        }
        if (step % 2 != 0) {
            return;
        }
        UnfinishedEffects.tearWarn(level, centre, length);
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        RandomSource random = level.getRandom();
        float share = 0.25F + 0.6F * step / Math.max(1, span);
        BlockState glitch = ModBlocks.GLITCH_BLOCK.get().defaultBlockState();
        long until = level.getGameTime() + FLICKER_TICKS;
        for (UnfinishedUnload.Column column : plan.columns()) {
            BlockPos top = new BlockPos(column.x(), column.top(), column.z());
            if (random.nextFloat() < share && level.getBlockState(top).isSolidRender(level, top)) {
                data.swap(level, top, glitch, until, UnfinishedTempBlocks.Kind.GROUND);
            }
        }
    }

    /** The flickering glitch blocks of the arrival at {@code place} back to their ground now, and its plan forgotten. */
    private void settleFlicker(ServerLevel level, BlockPos place) {
        UnfinishedTear.Plan plan = planned.remove(place);
        if (plan != null) {
            UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
            for (UnfinishedUnload.Column column : plan.columns()) {
                BlockPos top = new BlockPos(column.x(), column.top(), column.z());
                if (data.isTracked(top) && level.getBlockState(top).is(ModBlocks.GLITCH_BLOCK.get())) {
                    data.restore(level, top);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ the tear

    private static UnfinishedTear.Plan plan(ServerLevel level, BlockPos place, Vec3 toward, UnfinishedArrivalConfig config) {
        int scarLength = config.scarEnabled.get() ? config.scarLength.get() : 0;
        return UnfinishedTear.plan(level, place, toward, config.tearLength.get(), config.tearWidth.get(), scarLength,
                config.scarWidth.get());
    }

    /** The tear opens down to the void for a while and loads back in with the shallow scar in its middle. */
    private static void open(ServerLevel level, UnfinishedTear.Plan plan, UnfinishedArrivalConfig config) {
        if (plan.isEmpty()) {
            return;
        }
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        long openUntil = level.getGameTime() + config.tearOpenSeconds.get() * 20L;
        UnfinishedUnload.RestoreAs restoreAs = plan.scar().isEmpty()
                ? UnfinishedUnload.RestoreAs.SAME : plan.scarShape(config.scarDepth.get(), level.getMinBuildHeight());
        data.addDig(new UnfinishedUnload.Dig(plan.columns(), openUntil, restoreAs));
        int radius = Mth.ceil(config.tearLength.get() / 2.0);
        data.addHole(new UnfinishedTempBlocks.Hole(plan.centre(), radius, plan.surface(), openUntil + 20));
    }

    // ------------------------------------------------------------------------------------------------ place

    /** Ground behind the target (fanning round it, nearer and farther); with {@code tearable} only ground that can tear. */
    @Nullable
    private static BlockPos spot(ServerLevel level, ServerPlayer target, double distance, boolean tearable) {
        float yaw = target.getYHeadRot();
        for (double factor : DISTANCE_FACTORS) {
            double d = distance * factor;
            for (int angle : ANGLES) {
                float rad = (yaw + angle) * Mth.DEG_TO_RAD;
                // The look vector is (-sin, cos); behind is the opposite way.
                double x = target.getX() + Mth.sin(rad) * d;
                double z = target.getZ() - Mth.cos(rad) * d;
                if (!level.getWorldBorder().isWithinBounds(x, z)) {
                    continue;
                }
                OptionalDouble ground = UnfinishedGround.surface(level, x, z);
                if (ground.isEmpty() || Math.abs(ground.getAsDouble() - target.getY()) > MAX_HEIGHT_GAP) {
                    continue;
                }
                int bx = Mth.floor(x);
                int bz = Mth.floor(z);
                if (tearable && UnfinishedUnload.count(level, bx, bz) <= 0) {
                    continue;
                }
                return new BlockPos(bx, Mth.floor(ground.getAsDouble()), bz);
            }
        }
        return null;
    }

    private static int seed(BlockPos place) {
        return Long.hashCode(place.asLong());
    }

    private static UnfinishedArrivalConfig config() {
        return ServerConfig.UNFINISHED.arrival;
    }
}
