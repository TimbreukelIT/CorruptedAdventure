package nl.timtoys.corruptedadventure.entity.risen;

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
 * An extra hitbox of The Risen (a wing or its head), like the ender dragon's parts: hits on it go to the Risen, times
 * {@link #damageFactor}. Not saved, not sent on its own: the client makes the same parts with the ids after the Risen's.
 */
final class RisenPart extends PartEntity<TheRisen> {
    private final EntityDimensions size;
    private final boolean wing;

    RisenPart(TheRisen risen, float width, float height, boolean wing) {
        super(risen);
        this.size = EntityDimensions.scalable(width, height);
        this.wing = wing;
        refreshDimensions();
    }

    /** Wings take {@code wingDamageFactor}; the head takes it all. */
    boolean isWing() {
        return wing;
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
        return true;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return getParent().getPickResult();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !isInvulnerableTo(source) && getParent().hurt(this, source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return size;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
