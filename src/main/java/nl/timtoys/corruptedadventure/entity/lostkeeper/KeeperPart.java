package nl.timtoys.corruptedadventure.entity.lostkeeper;

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
 * A hitbox of The Lost Keeper besides its head (the entity itself): a stretch of the body on a {@link KeeperBody} node,
 * or the crown. Like vanilla's ender dragon parts: not saved, not sent on its own (the ids follow the keeper's), every
 * hit goes to the keeper ({@link TheLostKeeper#hurtFromPart}). Arrows and melee hit parts too.
 */
public final class KeeperPart extends PartEntity<TheLostKeeper> {
    private final EntityDimensions size;
    private final boolean crown;

    KeeperPart(TheLostKeeper keeper, float width, float height, boolean crown) {
        super(keeper);
        this.size = EntityDimensions.scalable(width, height);
        this.crown = crown;
        refreshDimensions();
    }

    public boolean isCrown() {
        return crown;
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
        return !isInvulnerableTo(source) && getParent().hurtFromPart(this, source, amount);
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
