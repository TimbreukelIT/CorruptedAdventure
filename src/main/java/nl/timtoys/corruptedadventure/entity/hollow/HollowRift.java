package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * A dimension rift of The Hollow (phase 4 step 2): a ragged vertical wound of ~3 × 7 blocks into Herobrine's void.
 * The server decides where it is, which {@link RiftPhase} it is in and how long that lasts; the client draws it
 * ({@code client/entity/hollow/HollowRiftRenderer}) from the synced phase and its length, and makes the torches near an
 * omen flicker. The rift faces along its yaw (its plane is across it).
 * <p>
 * An arrival (also a plain {@code /summon}) runs the omens ({@link RiftOmens}), opens, shows the eyes, lets The Hollow
 * pull itself through and closes, leaving a scar ({@link HollowScar}). A rift step ({@link #step}) is a short open rift
 * without omens or scar. While fully open it pulls players and items toward it ({@link RiftSuction}).
 */
public class HollowRift extends Entity {
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(HollowRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE_TICKS = SynchedEntityData.defineId(HollowRift.class, EntityDataSerializers.INT);
    /** Ticks of the CLOSING phase of every rift. */
    static final int CLOSING_TICKS = 30;

    private boolean arrival = true;
    private int phaseAge;
    private int openTicks;
    /** Client only: {@code tickCount} when the synced phase last changed. */
    private int clientPhaseStart;

    public HollowRift(EntityType<? extends HollowRift> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** An arrival of The Hollow at {@code feet}, facing {@code yaw}; starts with the omens. */
    public static HollowRift arrival(ServerLevel level, Vec3 feet, float yaw) {
        HollowRift rift = new HollowRift(ModEntityTypes.HOLLOW_RIFT.get(), level);
        rift.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        level.addFreshEntity(rift);
        return rift;
    }

    /** A rift step's rift: opens in {@code openingTicks}, stays open {@code openTicks}, then closes; no omens, no scar. */
    public static HollowRift step(ServerLevel level, Vec3 feet, float yaw, int openingTicks, int openTicks) {
        HollowRift rift = new HollowRift(ModEntityTypes.HOLLOW_RIFT.get(), level);
        rift.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        rift.arrival = false;
        rift.openTicks = openTicks;
        rift.setPhase(RiftPhase.OPENING, openingTicks);
        level.addFreshEntity(rift);
        return rift;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_PHASE, RiftPhase.OMEN.ordinal());
        builder.define(DATA_PHASE_TICKS, 0);
    }

    public RiftPhase phase() {
        return RiftPhase.byId(entityData.get(DATA_PHASE));
    }

    /** Ticks the current phase lasts (0 before the server set the first one). */
    public int phaseTicks() {
        return entityData.get(DATA_PHASE_TICKS);
    }

    /** Client: how far the current phase is, 0..1. */
    public float phaseProgress(float partialTick) {
        int length = phaseTicks();
        return length <= 0 ? 1.0F : Math.min(1.0F, (tickCount - clientPhaseStart + partialTick) / length);
    }

    public boolean isArrival() {
        return arrival;
    }

    private void setPhase(RiftPhase phase, int ticks) {
        phaseAge = 0;
        entityData.set(DATA_PHASE, phase.ordinal());
        entityData.set(DATA_PHASE_TICKS, Math.max(1, ticks));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_PHASE.equals(key)) {
            clientPhaseStart = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        HollowConfig config = ServerConfig.HOLLOW;
        if (phaseTicks() == 0) {
            // A fresh arrival (also from /summon): start with the omens.
            setPhase(RiftPhase.OMEN, config.omenSeconds.get() * 20);
        }
        RiftPhase phase = phase();
        if (phase == RiftPhase.OMEN || phase == RiftPhase.OPENING && arrival) {
            RiftOmens.tick(level, this, phaseAge);
        }
        if (phase.isOpen()) {
            RiftSuction.pull(level, this, config.suctionRadius.get(), config.suctionStrength.get());
        }
        if (++phaseAge < phaseTicks()) {
            return;
        }
        switch (phase) {
            case OMEN -> setPhase(RiftPhase.OPENING, config.riftOpeningTicks.get());
            case OPENING -> {
                if (arrival) {
                    setPhase(RiftPhase.EYES, config.riftEyesTicks.get());
                } else {
                    setPhase(RiftPhase.OPEN, openTicks);
                }
            }
            case EYES -> {
                emerge(level);
                setPhase(RiftPhase.EMERGE, HollowAnimations.EMERGE_TICKS);
            }
            case EMERGE, OPEN -> setPhase(RiftPhase.CLOSING, CLOSING_TICKS);
            case CLOSING -> {
                if (arrival && config.scarEnabled.get()) {
                    HollowScar.place(level, blockPosition(), config.scarRadius.get(), random);
                }
                discard();
            }
        }
    }

    /** The Hollow comes through: it stands in the rift, facing out, and the emerge animation pulls it free. */
    private void emerge(ServerLevel level) {
        TheHollow hollow = ModEntityTypes.THE_HOLLOW.get().create(level);
        if (hollow == null) {
            return;
        }
        hollow.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        hollow.setYHeadRot(getYRot());
        hollow.setYBodyRot(getYRot());
        level.addFreshEntity(hollow);
        hollow.startEmerging();
    }

    /** Closes a step rift early (the step is done). */
    public void close() {
        if (phase() != RiftPhase.CLOSING) {
            setPhase(RiftPhase.CLOSING, CLOSING_TICKS);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        // A step rift is gone within seconds; only an arrival survives a save.
        return arrival && super.shouldBeSaved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Arrival")) {
            arrival = tag.getBoolean("Arrival");
        }
        if (tag.contains("Phase")) {
            setPhase(RiftPhase.byId(tag.getInt("Phase")), tag.getInt("PhaseTicks"));
            phaseAge = tag.getInt("PhaseAge");
        }
        openTicks = tag.getInt("OpenTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Arrival", arrival);
        if (phaseTicks() > 0) {
            tag.putInt("Phase", phase().ordinal());
            tag.putInt("PhaseTicks", phaseTicks());
            tag.putInt("PhaseAge", phaseAge);
        }
        tag.putInt("OpenTicks", openTicks);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        // Seen from far: a wound in the sky is the warning.
        return distance < 160.0 * 160.0;
    }
}
