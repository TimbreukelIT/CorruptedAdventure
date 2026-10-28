package nl.timtoys.corruptedadventure.entity.nightmare.director;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * What the director knows of one Nightmare kind: its phase, its virtual position and heading, the target and place of
 * its coming, the entity while it is here, until when it is defeated, and how often it was defeated. Mutable; only the
 * director and its saved data change it. Unknown or broken saved values load as a roaming Nightmare that still has to
 * be placed.
 */
public final class NightmareState {
    public enum Phase implements StringRepresentable {
        /** Only a virtual position that wanders through its ring. */
        ROAMING,
        /** Its omens play for {@link #target()}; it appears at {@link #place()}. */
        COMING,
        /** Its entity {@link #entity()} is in the world. */
        PRESENT,
        /** Beaten (0 HP); roams again from {@link #defeatedUntil()} on. */
        DEFEATED;

        private final String serializedName = name().toLowerCase(Locale.ROOT);

        static Phase byName(String name) {
            for (Phase phase : values()) {
                if (phase.serializedName.equals(name)) {
                    return phase;
                }
            }
            return ROAMING;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    private Phase phase = Phase.ROAMING;
    private boolean placed;
    private double x;
    private double z;
    private float heading;
    private long nextRoamTick;
    @Nullable
    private UUID target;
    private BlockPos place = BlockPos.ZERO;
    private int omenTick;
    private boolean forced;
    private int appearTries;
    @Nullable
    private UUID entity;
    private int missingTicks;
    private long defeatedUntil;
    private int defeats;

    /** The strength after {@code defeats} defeats: 1 + {@code perReturn} for each, at most {@code maxReturns} of them. */
    public static float strength(int defeats, double perReturn, int maxReturns) {
        return (float) (1.0 + perReturn * Math.min(Math.max(0, defeats), Math.max(0, maxReturns)));
    }

    public Phase phase() {
        return phase;
    }

    public boolean placed() {
        return placed;
    }

    public double x() {
        return x;
    }

    public double z() {
        return z;
    }

    public float heading() {
        return heading;
    }

    public long nextRoamTick() {
        return nextRoamTick;
    }

    @Nullable
    public UUID target() {
        return target;
    }

    public BlockPos place() {
        return place;
    }

    public int omenTick() {
        return omenTick;
    }

    /** A coming started by {@code /ca nightmare arrive}: the target need not stay in the ring. */
    public boolean forced() {
        return forced;
    }

    public int appearTries() {
        return appearTries;
    }

    @Nullable
    public UUID entity() {
        return entity;
    }

    public int missingTicks() {
        return missingTicks;
    }

    public long defeatedUntil() {
        return defeatedUntil;
    }

    public int defeats() {
        return defeats;
    }

    /** Moves the virtual position (and marks it placed). */
    void moveTo(double x, double z, float heading) {
        this.x = x;
        this.z = z;
        this.heading = heading;
        this.placed = true;
    }

    void nextRoamTick(long tick) {
        nextRoamTick = tick;
    }

    void roam() {
        phase = Phase.ROAMING;
        target = null;
        entity = null;
        omenTick = 0;
        forced = false;
        appearTries = 0;
        missingTicks = 0;
    }

    void coming(UUID target, BlockPos place, boolean forced) {
        roam();
        phase = Phase.COMING;
        this.target = target;
        this.place = place;
        this.forced = forced;
    }

    void omenTick(int tick) {
        omenTick = tick;
    }

    void appearTries(int tries) {
        appearTries = tries;
    }

    void present(UUID entity) {
        UUID wasTarget = target;
        roam();
        phase = Phase.PRESENT;
        this.entity = entity;
        this.target = wasTarget;
    }

    void missingTicks(int ticks) {
        missingTicks = ticks;
    }

    void defeat(long until) {
        roam();
        phase = Phase.DEFEATED;
        defeatedUntil = until;
        defeats++;
    }

    /** Forgets everything but the virtual position: roaming, never defeated. */
    void reset() {
        roam();
        defeats = 0;
        defeatedUntil = 0;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("phase", phase.getSerializedName());
        tag.putBoolean("placed", placed);
        tag.putDouble("x", x);
        tag.putDouble("z", z);
        tag.putFloat("heading", heading);
        tag.putLong("next_roam", nextRoamTick);
        if (target != null) {
            tag.putUUID("target", target);
        }
        tag.put("place", NbtUtils.writeBlockPos(place));
        tag.putInt("omen_tick", omenTick);
        tag.putBoolean("forced", forced);
        tag.putInt("appear_tries", appearTries);
        if (entity != null) {
            tag.putUUID("entity", entity);
        }
        tag.putLong("defeated_until", defeatedUntil);
        tag.putInt("defeats", defeats);
        return tag;
    }

    static NightmareState load(CompoundTag tag) {
        NightmareState state = new NightmareState();
        state.phase = Phase.byName(tag.getString("phase"));
        state.placed = tag.getBoolean("placed");
        state.x = tag.getDouble("x");
        state.z = tag.getDouble("z");
        state.heading = tag.getFloat("heading");
        state.nextRoamTick = tag.getLong("next_roam");
        state.target = tag.hasUUID("target") ? tag.getUUID("target") : null;
        state.place = NbtUtils.readBlockPos(tag, "place").orElse(BlockPos.ZERO);
        state.omenTick = Math.max(0, tag.getInt("omen_tick"));
        state.forced = tag.getBoolean("forced");
        state.appearTries = Math.max(0, tag.getInt("appear_tries"));
        state.entity = tag.hasUUID("entity") ? tag.getUUID("entity") : null;
        state.defeatedUntil = tag.getLong("defeated_until");
        state.defeats = Math.max(0, tag.getInt("defeats"));
        // A phase without what it needs (a coming without target, a presence without entity) roams instead.
        if ((state.phase == Phase.COMING && state.target == null) || (state.phase == Phase.PRESENT && state.entity == null)) {
            state.roam();
        }
        return state;
    }
}
