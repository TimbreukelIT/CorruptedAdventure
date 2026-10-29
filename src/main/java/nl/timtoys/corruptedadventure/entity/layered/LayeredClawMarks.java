package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * A trace of The Layered's roaming (fase-4-nightmares-concept.md "Stap 2 voor ring 2–5": claw marks in a canyon wall):
 * deep gouges of its claws on a flat piece of natural wall, {@link #SIZE} blocks across, that shed a little dust while a
 * player is near and fade away after {@code traceSeconds}. Only a picture: no block changes, never saved (an unloaded
 * chunk takes them along). The server places and removes them; the client draws them
 * ({@code client/entity/layered/LayeredClawMarksRenderer}) from the synced facing and end time.
 */
public class LayeredClawMarks extends Entity {
    private static final EntityDataAccessor<Direction> DATA_FACING =
            SynchedEntityData.defineId(LayeredClawMarks.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Long> DATA_ENDS =
            SynchedEntityData.defineId(LayeredClawMarks.class, EntityDataSerializers.LONG);
    /** Blocks across the marks (the texture has 16 pixels per block, like the stone). */
    public static final float SIZE = 3.0F;
    /** How far in front of the face the entity stands, so its light is that of the air there. */
    public static final double OUT = 0.03;
    /** The last ticks the marks fade out. */
    public static final int FADE_TICKS = 200;
    private static final int DUST_EVERY = 60;
    private static final double DUST_RANGE = 24.0;
    /** {@code /summon} without {@code Ticks}. */
    private static final int SUMMON_TICKS = 3600;

    public LayeredClawMarks(EntityType<? extends LayeredClawMarks> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** Claw marks on {@code face} for {@code ticks}. */
    public static LayeredClawMarks place(ServerLevel level, LayeredWallSpot.Face face, int ticks) {
        LayeredClawMarks marks = new LayeredClawMarks(ModEntityTypes.LAYERED_CLAW_MARKS.get(), level);
        Vec3 at = face.at().add(Vec3.atLowerCornerOf(face.facing().getNormal()).scale(OUT));
        marks.moveTo(at.x, at.y - SIZE / 2.0, at.z, face.facing().toYRot(), 0.0F);
        marks.entityData.set(DATA_FACING, face.facing());
        marks.entityData.set(DATA_ENDS, level.getGameTime() + ticks);
        level.addFreshEntity(marks);
        return marks;
    }

    /** How many there are in the level now. */
    public static int count(ServerLevel level) {
        return level.getEntities(ModEntityTypes.LAYERED_CLAW_MARKS.get(), marks -> true).size();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FACING, Direction.NORTH);
        builder.define(DATA_ENDS, 0L);
    }

    /** The way the marked face looks out of the wall. */
    public Direction facing() {
        return entityData.get(DATA_FACING);
    }

    /** 1 while fresh, down to 0 over the last {@link #FADE_TICKS} (client: for drawing). */
    public float fade(float partialTick) {
        double left = entityData.get(DATA_ENDS) - (level().getGameTime() + partialTick);
        return Mth.clamp((float) (left / FADE_TICKS), 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();
        if (entityData.get(DATA_ENDS) == 0L) {
            entityData.set(DATA_ENDS, now + SUMMON_TICKS);
        }
        if (now >= entityData.get(DATA_ENDS)) {
            discard();
            return;
        }
        if (tickCount % DUST_EVERY == 0 && level.hasNearbyAlivePlayer(getX(), getY(), getZ(), DUST_RANGE)) {
            BlockPos stone = BlockPos.containing(position().add(0.0, SIZE / 2.0, 0.0)).relative(facing().getOpposite());
            if (LayeredWallSpot.isNatural(level.getBlockState(stone))) {
                LayeredEffects.clawDust(level, position().add(0.0, SIZE / 2.0, 0.0), level.getBlockState(stone));
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
        return false;
    }

    /** Only for {@code /summon} (tests, screenshots): {@code Facing} (a horizontal direction) and {@code Ticks}. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Facing")) {
            Direction facing = Direction.byName(tag.getString("Facing"));
            if (facing != null && facing.getAxis().isHorizontal()) {
                entityData.set(DATA_FACING, facing);
            }
        }
        if (tag.contains("Ticks")) {
            entityData.set(DATA_ENDS, level().getGameTime() + Math.max(1, tag.getInt("Ticks")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(SIZE / 2.0, 0.5, SIZE / 2.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 64.0 * 64.0;
    }
}
