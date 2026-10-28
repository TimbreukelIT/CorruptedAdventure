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
 * omen flicker. The rift faces along its yaw (its plane is across it). What it does depends on its {@link Kind}.
 * While fully open it pulls players and items toward it ({@link RiftSuction}).
 */
public class HollowRift extends Entity {
    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(HollowRift.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PHASE_TICKS = SynchedEntityData.defineId(HollowRift.class, EntityDataSerializers.INT);
    /** Ticks of the CLOSING phase of every rift. */
    public static final int CLOSING_TICKS = 30;
    /** Ticks a roaming trace (a hairline crack) hangs in the air. */
    static final int TRACE_TICKS = 160;
    /** Ticks a directed omen waits past its length for the director before it closes by itself. */
    private static final int ORPHAN_TICKS = 1200;

    public enum Kind {
        /** {@code /summon}: runs a whole arrival by itself, The Hollow included. */
        SUMMONED,
        /** The director's arrival ({@link HollowArrival}): holds its omen until {@link #open} brings The Hollow through. */
        DIRECTED,
        /** A rift step's way in or out: no omens, no scar, not saved. */
        STEP,
        /** A trace of its roaming: a hairline crack for a while, nothing else. */
        TRACE;

        boolean isArrival() {
            return this == SUMMONED || this == DIRECTED;
        }
    }

    private Kind kind = Kind.SUMMONED;
    private boolean scars = true;
    private int phaseAge;
    private int openTicks;
    /** Client only: {@code tickCount} when the synced phase last changed. */
    private int clientPhaseStart;

    public HollowRift(EntityType<? extends HollowRift> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    private static HollowRift create(ServerLevel level, Vec3 feet, float yaw, Kind kind) {
        HollowRift rift = new HollowRift(ModEntityTypes.HOLLOW_RIFT.get(), level);
        rift.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        rift.kind = kind;
        return rift;
    }

    /** The director's arrival at {@code feet}, facing {@code yaw}: a hairline crack that holds until {@link #open}. */
    public static HollowRift directed(ServerLevel level, Vec3 feet, float yaw, int omenTicks) {
        HollowRift rift = create(level, feet, yaw, Kind.DIRECTED);
        rift.setPhase(RiftPhase.OMEN, omenTicks);
        level.addFreshEntity(rift);
        return rift;
    }

    /** A rift step's rift: opens in {@code openingTicks}, stays open {@code openTicks}, then closes; no omens, no scar. */
    public static HollowRift step(ServerLevel level, Vec3 feet, float yaw, int openingTicks, int openTicks) {
        HollowRift rift = create(level, feet, yaw, Kind.STEP);
        rift.openTicks = openTicks;
        rift.setPhase(RiftPhase.OPENING, openingTicks);
        level.addFreshEntity(rift);
        return rift;
    }

    /** A trace of its roaming: a hairline crack hanging in the air for a while; no omens, no scar. */
    public static HollowRift trace(ServerLevel level, Vec3 feet, float yaw) {
        HollowRift rift = create(level, feet, yaw, Kind.TRACE);
        rift.setPhase(RiftPhase.OMEN, TRACE_TICKS);
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

    public Kind kind() {
        return kind;
    }

    private void setPhase(RiftPhase phase, int ticks) {
        phaseAge = 0;
        entityData.set(DATA_PHASE, phase.ordinal());
        entityData.set(DATA_PHASE_TICKS, Math.max(1, ticks));
    }

    /** Ticks from {@link #open} until The Hollow comes through: the tearing open and the eyes. */
    public static int ticksUntilEmerging() {
        return ServerConfig.HOLLOW.riftOpeningTicks.get() + ServerConfig.HOLLOW.riftEyesTicks.get();
    }

    /** The director's arrival goes on: tears open, shows the eyes, then The Hollow (already waiting in it) comes out. */
    public void open() {
        if (phase() == RiftPhase.OMEN) {
            setPhase(RiftPhase.OPENING, ServerConfig.HOLLOW.riftOpeningTicks.get());
        }
    }

    /** Closes the rift; with {@code scar} false it leaves nothing behind (omens cancelled, a step done). */
    public void close(boolean scar) {
        scars &= scar;
        if (phase() != RiftPhase.CLOSING) {
            setPhase(RiftPhase.CLOSING, CLOSING_TICKS);
        }
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
            // A fresh /summon: start with the omens.
            setPhase(RiftPhase.OMEN, config.omenSeconds.get() * 20);
        }
        RiftPhase phase = phase();
        if (kind.isArrival() && (phase == RiftPhase.OMEN || phase == RiftPhase.OPENING)) {
            RiftOmens.tick(level, this, phaseAge);
        }
        if (phase.isOpen()) {
            RiftSuction.pull(level, this, config.suctionRadius.get(), config.suctionStrength.get());
        }
        if (++phaseAge < phaseTicks()) {
            return;
        }
        switch (phase) {
            case OMEN -> {
                if (kind == Kind.SUMMONED) {
                    setPhase(RiftPhase.OPENING, config.riftOpeningTicks.get());
                } else if (kind != Kind.DIRECTED || phaseAge > phaseTicks() + ORPHAN_TICKS) {
                    // A trace fades; a directed omen holds until the director opens it, unless nobody ever does.
                    close(false);
                }
            }
            case OPENING -> {
                if (kind.isArrival()) {
                    setPhase(RiftPhase.EYES, config.riftEyesTicks.get());
                } else {
                    setPhase(RiftPhase.OPEN, openTicks);
                }
            }
            case EYES -> {
                if (kind == Kind.SUMMONED) {
                    HollowArrival.emergeFrom(level, this);
                }
                setPhase(RiftPhase.EMERGE, HollowAnimations.EMERGE_TICKS);
            }
            case EMERGE, OPEN -> close(true);
            case CLOSING -> {
                if (kind.isArrival() && scars && config.scarEnabled.get()) {
                    HollowScar.place(level, blockPosition(), config.scarRadius.get(), random);
                }
                discard();
            }
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
        // Steps and traces are gone within seconds; only an arrival survives a save.
        return kind.isArrival() && super.shouldBeSaved();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Kind")) {
            kind = Kind.values()[Math.floorMod(tag.getInt("Kind"), Kind.values().length)];
        }
        if (tag.contains("Scars")) {
            scars = tag.getBoolean("Scars");
        }
        if (tag.contains("Phase")) {
            setPhase(RiftPhase.byId(tag.getInt("Phase")), tag.getInt("PhaseTicks"));
            phaseAge = tag.getInt("PhaseAge");
        }
        openTicks = tag.getInt("OpenTicks");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Kind", kind.ordinal());
        tag.putBoolean("Scars", scars);
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
