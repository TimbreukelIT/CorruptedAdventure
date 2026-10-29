package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * The trace The Risen leaves while it roams (design doc "Stap 2 voor ring 2–5", De Herrijzende): a patch of
 * smouldering ash where it passed, smoke rising from it in a few thin columns, sparks and ash drifting over it. Only
 * particles, drawn by each client itself (no blocks, no packets per particle); the server only counts its life down and
 * removes it. Never saved: unloaded, it has burnt out. At most {@code traceMax} at once.
 */
public final class RisenTrace extends Entity {
    private static final EntityDataAccessor<Integer> DATA_LEFT =
            SynchedEntityData.defineId(RisenTrace.class, EntityDataSerializers.INT);
    /** Radius of the smouldering patch. */
    private static final double RADIUS = 3.0;
    private static final int VENTS = 3;
    /** The last this many ticks it dies down. */
    private static final int FADE_TICKS = 600;

    /** Ticks left; -1 until its first tick (a {@code /summon}ed trace smoulders {@code traceSeconds}). */
    private int left = -1;
    private double[] vents;

    public RisenTrace(EntityType<? extends RisenTrace> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /**
     * Leaves a trace on the surface of column {@code at} if that is loaded, dry ground and there are fewer than
     * {@code traceMax}; true if it did.
     */
    public static boolean leave(ServerLevel level, BlockPos at, RisenConfig config) {
        if (!level.hasChunkAt(at) || config.traceMax.get() <= 0
                || level.getEntities(ModEntityTypes.RISEN_TRACE.get(), trace -> true).size() >= config.traceMax.get()) {
            return false;
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
        if (!level.getFluidState(surface.below()).isEmpty() || !level.getFluidState(surface).isEmpty()) {
            return false;
        }
        RisenTrace trace = ModEntityTypes.RISEN_TRACE.get().create(level);
        if (trace == null) {
            return false;
        }
        trace.moveTo(surface.getX() + 0.5, surface.getY(), surface.getZ() + 0.5, 0.0F, 0.0F);
        trace.left = config.traceSeconds.get() * 20;
        trace.entityData.set(DATA_LEFT, trace.left);
        return level.addFreshEntity(trace);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_LEFT, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            smoulder();
            return;
        }
        if (left < 0) {
            left = ServerConfig.RISEN.traceSeconds.get() * 20;
        }
        if (--left <= 0) {
            discard();
        } else if (left % 20 == 0) {
            entityData.set(DATA_LEFT, left);
        }
    }

    /** Client: a few columns of smoke from fixed vents, sparks and ash over the patch, dying down at the end. */
    private void smoulder() {
        RandomSource random = level().getRandom();
        if (vents == null) {
            RandomSource placed = RandomSource.create(getId() * 31L + getBlockX() * 7L + getBlockZ());
            vents = new double[VENTS * 2];
            for (int i = 0; i < VENTS; i++) {
                double angle = placed.nextDouble() * Math.PI * 2.0;
                double distance = placed.nextDouble() * RADIUS * 0.7;
                vents[i * 2] = getX() + Math.cos(angle) * distance;
                vents[i * 2 + 1] = getZ() + Math.sin(angle) * distance;
            }
        }
        // Counted down by the server once a second; between two updates the client counts on.
        float strength = Mth.clamp((entityData.get(DATA_LEFT) - (tickCount % 20)) / (float) FADE_TICKS, 0.15F, 1.0F);
        for (int i = 0; i < VENTS; i++) {
            double x = vents[i * 2];
            double z = vents[i * 2 + 1];
            double y = surface(x, z);
            if (random.nextFloat() < 0.12F * strength) {
                level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.1, z, 0.0, 0.03, 0.0);
            }
            if (random.nextFloat() < 0.3F) {
                level().addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.6, y + 0.1,
                        z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
            }
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = Math.sqrt(random.nextDouble()) * RADIUS;
        double x = getX() + Math.cos(angle) * distance;
        double z = getZ() + Math.sin(angle) * distance;
        double y = surface(x, z);
        if (random.nextFloat() < 0.4F) {
            level().addParticle(random.nextBoolean() ? ParticleTypes.ASH : ParticleTypes.WHITE_ASH, x, y + 0.3, z,
                    0.0, 0.0, 0.0);
        }
        if (random.nextFloat() < 0.1F * strength) {
            level().addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.05, z, 0.0, 0.01, 0.0);
        }
        if (random.nextFloat() < 0.015F * strength) {
            level().addParticle(ParticleTypes.LAVA, x, y + 0.1, z, 0.0, 0.0, 0.0);
        }
    }

    /** The client only has the heightmaps the chunk packet brings; this one counts the ash drifts on the ground too. */
    private double surface(double x, double z) {
        return level().getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z));
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
