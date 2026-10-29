package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One block of a memory The Unfinished tears off himself and throws ({@link UnfinishedThrowGoal}): rendered as its block,
 * tumbling. While it hovers by him ({@link #isNoGravity}) it passes through everything and the server steers it; thrown,
 * it falls under gravity, the same on both sides, and collides with blocks (never with entities; off a wall it drops
 * straight down). Where it comes down its
 * {@link ShardVolley} decides: the first one down is the impact, and each may lie there a while as a real block. Never
 * saved: a shard in a chunk that unloads, or one left when the server stops, is simply gone.
 */
public final class MemoryShard extends Entity {
    private static final EntityDataAccessor<BlockState> DATA_BLOCK =
            SynchedEntityData.defineId(MemoryShard.class, EntityDataSerializers.BLOCK_STATE);
    static final double GRAVITY = 0.04;
    /** A shard that never lands (thrown into the void, stuck) goes after this. */
    private static final int MAX_AGE = 300;

    @Nullable
    private ShardVolley volley;
    private boolean landed;

    public MemoryShard(EntityType<? extends MemoryShard> type, Level level) {
        super(type, level);
    }

    MemoryShard(EntityType<? extends MemoryShard> type, Level level, BlockState block, Vec3 at, ShardVolley volley) {
        this(type, level);
        entityData.set(DATA_BLOCK, block);
        this.volley = volley;
        setNoGravity(true);
        setPos(at.x, at.y, at.z);
        xo = at.x;
        yo = at.y;
        zo = at.z;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
    }

    public BlockState block() {
        return entityData.get(DATA_BLOCK);
    }

    /** Thrown: from now on gravity and the blocks have it. */
    void launch(Vec3 velocity) {
        setNoGravity(false);
        setDeltaMovement(velocity);
        hasImpulse = true;
    }

    /** Steered while it hovers: the velocity that brings it to {@code to} (both sides move it by it). */
    void steer(Vec3 to, double share) {
        setDeltaMovement(to.subtract(position()).scale(share));
        hasImpulse = true;
    }

    @Override
    public void tick() {
        super.tick();
        boolean hovering = isNoGravity();
        noPhysics = hovering;
        if (!hovering) {
            setDeltaMovement(getDeltaMovement().add(0.0, -GRAVITY, 0.0));
        }
        move(MoverType.SELF, getDeltaMovement());
        if (level().isClientSide) {
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (volley != null) {
            volley.tick(level, this);
        }
        if (!hovering && horizontalCollision) {
            // Against a wall it loses its sideways speed and drops down it.
            setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        }
        if (!hovering && !landed && onGround()) {
            landed = true;
            if (volley != null) {
                volley.landed(level, this);
            }
            discard();
            return;
        }
        if (tickCount > MAX_AGE || getY() < level.getMinBuildHeight() - 16 || volley == null) {
            discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
