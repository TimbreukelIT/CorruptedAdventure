package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A separately hittable piece of The Unfinished (like the ender dragon's parts): a body part (the chest, the crown, a
 * graft) or a weak spot. Hits go to {@link TheUnfinished#hurtPart}; it is positioned on the rest pose every tick on both
 * sides ({@link TheUnfinished#placeParts}), so client picking and the server's reach check see the same boxes. Never
 * saved, never sent: the client makes its own with the parent.
 */
public final class UnfinishedPart extends PartEntity<TheUnfinished> {
    private final Spec spec;
    private final EntityDimensions size;

    UnfinishedPart(TheUnfinished parent, Spec spec) {
        super(parent);
        this.spec = spec;
        this.size = EntityDimensions.scalable(spec.width(), spec.height());
        refreshDimensions();
    }

    public Spec spec() {
        return spec;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return getParent().isPartActive(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurtPart(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /**
     * Where a part sits on the rest pose, in blocks from his feet: {@code left} (negative = right), {@code up} (the
     * bottom of the box) and {@code forward}; its size; the graft it belongs to (null: the body itself) and whether it is
     * that graft's weak spot. Taken from the model (tools/textures/unfinished_model.py, 16 units per block).
     */
    public record Spec(String name, double left, double up, double forward, float width, float height,
                       @Nullable Graft graft, boolean weak) {
    }
}
