package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;

/**
 * How The Lost Keeper comes (fase-4-nightmares-concept.md, ring 4 "Komst (uit de mist)"), for the director: its own thick
 * mist rolls in during the omens, also in clear weather (its mist bank, {@link KeeperMistBanks#ARRIVAL}); then its glow
 * shimmers through the mist, far away first and ever closer, with its soft song. With big water near the target (deep,
 * and wide round the spot) the place is on that water: the surface churns, and it rises out of it. Otherwise the place is
 * in the air ahead of the target, where its glow ends; it takes shape there and swims through its mist to them. Cancelled
 * omens let the mist sink. While it roams it leaves a wisp of mist on the water now and then ({@link KeeperMistTraces}).
 * Stateless: the place says which way it comes (water or not).
 */
public final class KeeperArrival implements NightmareArrival {
    /** Share of the omens over which the mist rolls in to full strength. */
    static final float MIST_ROLL = 0.5F;
    /** Share of the omens at which the glow is first seen, and at which it has come (in water: then the surface churns). */
    static final float GLOW_START = 0.1F;
    static final float GLOW_END_WATER = 0.85F;
    static final float GLOW_END_AIR = 1.0F;
    private static final int GLOW_EVERY = 4;
    private static final int SONG_EVERY = 20;
    /** The blocks from the target that the water spot it rises from would ideally be (its 24 blocks rise beside them). */
    private static final double PREFERRED_WATER_DISTANCE = 12.0;
    /** Blocks above or below the target's feet the water's surface may be. */
    private static final int WATER_VERTICAL = 24;
    private static final int WATER_STEP = 2;
    /** Its head's centre at most this deep under the surface as it starts to rise. */
    private static final double RISE_DEPTH = 6.0;
    /** It rises to this far above the surface, a little towards the target. */
    private static final double RISE_ABOVE = 6.0;
    private static final double RISE_TOWARD = 3.0;
    /** From the air it swims to this far from the target, a little above them. */
    private static final double AIR_STOP = 8.0;
    private static final double AIR_STOP_UP = 3.0;
    private static final int[] AIR_ANGLES = {0, 30, -30, 60, -60, 90, -90, 135, -135, 180};
    private static final double[] AIR_DISTANCE_FACTORS = {1.0, 0.75, 0.5, 0.3};
    private static final int[] AIR_UP = {0, 4, 8, -3};

    @Override
    public BlockPos place(ServerLevel level, ServerPlayer target) {
        KeeperWater.Column water = bigWater(level, target.position(), config());
        return water != null ? water.top() : airSpot(level, target, config());
    }

    @Override
    public int omenTicks() {
        return config().arrivalOmenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        LostKeeperConfig config = config();
        float progress = tick / (float) Math.max(1, omenTicks());
        boolean water = isWater(level, place);
        Vec3 end = end(place, water);
        Vec3 away = away(end, target.position(), place);
        if (tick % 20 == 0) {
            float roll = mistRoll(progress);
            Vec3 centre = end.add(away.scale(config.arrivalMistDistance.get() * (1.0 - roll)));
            KeeperMistBanks.send(level, KeeperMistBanks.ARRIVAL, centre, config.mistRadius.get(), config.mistFade.get(),
                    config.arrivalMistVisibility.get(), roll, config.mistSyncRange.get());
        }
        float glow = glowShare(progress, water);
        if (glow >= 0.0F && tick % GLOW_EVERY == 0) {
            Vec3 at = glowAt(end, away, target.position(), glow, water, config);
            KeeperEffects.arrivalGlow(level, at, glow, config.mistSyncRange.get());
            if (tick % SONG_EVERY == 0) {
                KeeperEffects.arrivalSong(level, at, at.distanceTo(target.position()), tick);
            }
        }
        if (water && progress >= GLOW_END_WATER && tick % 2 == 0) {
            KeeperEffects.arrivalChurn(level, end, tick);
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        if (!level.hasChunkAt(place)) {
            return null;
        }
        Entity created = type.create(level);
        if (!(created instanceof TheLostKeeper keeper)) {
            return null;
        }
        LostKeeperConfig config = config();
        int ticks = config.arrivalMaxSeconds.get() * 20;
        boolean water = isWater(level, place);
        if (water) {
            KeeperWater.Column column = KeeperWater.column(level, place);
            Vec3 surface = column.surface();
            double headY = Math.max(surface.y - column.depth() + 1.4, surface.y - RISE_DEPTH);
            Vec3 toward = flat(target.position().subtract(surface), place);
            float yaw = KeeperMotion.yaw(toward);
            keeper.moveTo(surface.x, headY - KeeperBody.AXIS, surface.z, yaw, -70.0F);
            keeper.arrive(surface.add(toward.scale(RISE_TOWARD)).add(0.0, RISE_ABOVE, 0.0), ticks);
        } else {
            Vec3 feet = Vec3.atBottomCenterOf(place);
            if (!free(level, feet)) {
                feet = Vec3.atBottomCenterOf(airSpot(level, target, config));
            }
            Vec3 toward = flat(target.position().subtract(feet), place);
            Vec3 to = target.position().subtract(feet.add(0.0, KeeperBody.AXIS, 0.0));
            float pitch = to.lengthSqr() > 1.0E-6 ? KeeperMotion.pitch(to.normalize()) : 0.0F;
            keeper.moveTo(feet.x, feet.y, feet.z, KeeperMotion.yaw(toward), pitch);
            keeper.arrive(target.position().add(toward.scale(-AIR_STOP)).add(0.0, AIR_STOP_UP, 0.0), ticks);
        }
        keeper.setYHeadRot(keeper.getYRot());
        keeper.setYBodyRot(keeper.getYRot());
        if (!level.addFreshEntity(keeper)) {
            return null;
        }
        keeper.setTarget(target);
        if (water) {
            Vec3 surface = KeeperWater.column(level, place).surface();
            level.playSound(null, surface.x, surface.y, surface.z, SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundSource.HOSTILE,
                    3.0F, 0.45F);
        } else {
            KeeperEffects.appearInAir(level, keeper);
        }
        // The arrival's bank holds round it a little longer (the client fades it once it stops coming), its own is full.
        KeeperMistBanks.send(level, KeeperMistBanks.ARRIVAL, keeper.headCentre(), config.mistRadius.get(),
                config.mistFade.get(), config.arrivalMistVisibility.get(), 1.0F, config.mistSyncRange.get());
        return keeper;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        LostKeeperConfig config = config();
        // The mist sinks again; who is out of range sees it fade once it stops coming.
        KeeperMistBanks.send(level, KeeperMistBanks.ARRIVAL, Vec3.atCenterOf(place), config.mistRadius.get(),
                config.mistFade.get(), config.arrivalMistVisibility.get(), 0.0F, config.mistSyncRange.get());
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        LostKeeperConfig config = config();
        if (level.hasChunkAt(to) && level.getRandom().nextDouble() < config.traceChance.get()) {
            KeeperMistTraces.leave(level, to);
        }
    }

    // --- the omens' course (pure) ------------------------------------------------------------------------------------

    /** 0–1: how far the mist has rolled in at {@code progress} (0–1) of the omens, smoothly. */
    static float mistRoll(float progress) {
        float t = Mth.clamp(progress / MIST_ROLL, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** 0–1: how far the glow has come at {@code progress} of the omens; negative before it is seen. */
    static float glowShare(float progress, boolean water) {
        if (progress < GLOW_START) {
            return -1.0F;
        }
        float end = water ? GLOW_END_WATER : GLOW_END_AIR;
        float t = Mth.clamp((progress - GLOW_START) / (end - GLOW_START), 0.0F, 1.0F);
        // Slow far away, faster as it nears.
        return t * t;
    }

    /** The glow: from {@code arrivalGlowDistance} from the target (never nearer than the end) to the end. */
    private static Vec3 glowAt(Vec3 end, Vec3 away, Vec3 target, float share, boolean water, LostKeeperConfig config) {
        double endDistance = Math.sqrt(horizontalSqr(end, target));
        double from = Math.max(config.arrivalGlowDistance.get(), endDistance);
        double distance = Mth.lerp(share, from, endDistance);
        Vec3 flat = new Vec3(target.x, end.y, target.z).add(away.scale(distance));
        // Over the water it hangs a little above the surface and sinks into it at the end.
        double y = water ? end.y + Mth.lerp(share, 2.5, -0.5) : end.y;
        return new Vec3(flat.x, y, flat.z);
    }

    // --- the place ---------------------------------------------------------------------------------------------------

    /**
     * Big water near {@code near}: the surface column within {@code arrivalWaterSearch} blocks, at most
     * {@link #WATER_VERTICAL} above or below, at least {@code arrivalWaterDepth} deep and with enough water round it at the
     * same surface; the one nearest to {@link #PREFERRED_WATER_DISTANCE} from {@code near}. Null without (or unloaded).
     */
    @Nullable
    static KeeperWater.Column bigWater(ServerLevel level, Vec3 near, LostKeeperConfig config) {
        int reach = config.arrivalWaterSearch.get();
        int minDepth = config.arrivalWaterDepth.get();
        int originX = Mth.floor(near.x);
        int originZ = Mth.floor(near.z);
        KeeperWater.Column best = null;
        double bestScore = Double.MAX_VALUE;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -reach; dx <= reach; dx += WATER_STEP) {
            for (int dz = -reach; dz <= reach; dz += WATER_STEP) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                double score = Math.abs(distance - PREFERRED_WATER_DISTANCE);
                if (distance > reach || score >= bestScore) {
                    continue;
                }
                int x = originX + dx;
                int z = originZ + dz;
                pos.set(x, near.y, z);
                if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                    continue;
                }
                pos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1);
                if (Math.abs(pos.getY() - near.y) > WATER_VERTICAL || !level.getFluidState(pos).is(FluidTags.WATER)) {
                    continue;
                }
                KeeperWater.Column column = KeeperWater.column(level, pos);
                if (column.depth() >= minDepth && wide(level, column.top(), config)) {
                    best = column;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    /** Enough of the columns round {@code top} have water at the same surface. */
    private static boolean wide(ServerLevel level, BlockPos top, LostKeeperConfig config) {
        int spread = config.arrivalWaterSpread.get();
        int water = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -spread; dx <= spread; dx++) {
            for (int dz = -spread; dz <= spread; dz++) {
                pos.set(top.getX() + dx, top.getY(), top.getZ() + dz);
                if (level.getFluidState(pos).is(FluidTags.WATER) && !level.getFluidState(pos.above()).is(FluidTags.WATER)) {
                    water++;
                }
            }
        }
        int side = spread * 2 + 1;
        return water >= config.arrivalWaterShare.get() * side * side;
    }

    /**
     * Without big water: a free spot in the air {@code arrivalAirDistance} ahead of the target and {@code arrivalAirHeight}
     * up; else fanned out sideways and behind, nearer, higher or lower. The last fallback is above the target itself: the
     * arrival never fails for want of room.
     */
    static BlockPos airSpot(ServerLevel level, ServerPlayer target, LostKeeperConfig config) {
        double up = config.arrivalAirHeight.get();
        for (double factor : AIR_DISTANCE_FACTORS) {
            double distance = config.arrivalAirDistance.get() * factor;
            for (int angle : AIR_ANGLES) {
                float rad = (target.getYHeadRot() + angle) * Mth.DEG_TO_RAD;
                // The look vector is (-sin, cos).
                double x = target.getX() - Mth.sin(rad) * distance;
                double z = target.getZ() + Mth.cos(rad) * distance;
                for (int dy : AIR_UP) {
                    Vec3 feet = new Vec3(x, Math.floor(target.getY() + up + dy), z);
                    if (free(level, feet)) {
                        return BlockPos.containing(feet);
                    }
                }
            }
        }
        return BlockPos.containing(target.getX(), target.getY() + up, target.getZ());
    }

    /** Room for its head (and a block round it) at {@code feet}: loaded, inside the border, no blocks and no water. */
    private static boolean free(ServerLevel level, Vec3 feet) {
        BlockPos pos = BlockPos.containing(feet);
        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        AABB box = ModEntityTypes.THE_LOST_KEEPER.get().getDimensions().makeBoundingBox(feet).inflate(1.0);
        return level.noCollision(box) && !level.containsAnyLiquid(box);
    }

    // --- the rest ----------------------------------------------------------------------------------------------------

    private static boolean isWater(ServerLevel level, BlockPos place) {
        return level.getFluidState(place).is(FluidTags.WATER);
    }

    /** Where it appears, as the omens see it: the water's surface over the place, or its head's centre in the air. */
    private static Vec3 end(BlockPos place, boolean water) {
        return water ? new Vec3(place.getX() + 0.5, place.getY() + 1.0, place.getZ() + 0.5)
                : Vec3.atBottomCenterOf(place).add(0.0, KeeperBody.AXIS, 0.0);
    }

    /** The level way from the target out past the end, the way the mist and the glow come in. */
    private static Vec3 away(Vec3 end, Vec3 target, BlockPos place) {
        return flat(end.subtract(target), place);
    }

    /** {@code v} level and of length 1; a fixed way per place when it is (nearly) vertical. */
    private static Vec3 flat(Vec3 v, BlockPos place) {
        Vec3 level = new Vec3(v.x, 0.0, v.z);
        if (level.lengthSqr() > 1.0E-4) {
            return level.normalize();
        }
        double angle = Math.floorMod(place.hashCode(), 360) * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    private static double horizontalSqr(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    private static LostKeeperConfig config() {
        return ServerConfig.LOST_KEEPER;
    }
}
