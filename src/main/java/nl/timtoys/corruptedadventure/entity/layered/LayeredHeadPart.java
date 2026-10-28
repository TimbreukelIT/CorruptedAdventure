package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The living head of The Layered as its own hitbox (like the ender dragon's parts): the place to hit to make it let go of
 * a grabbed player. Hits go to {@link TheLayered#hurtHead}. Not saved, not sent: the parent places it every tick on
 * both sides ({@link LayeredPose#HEAD_UP}, {@link LayeredPose#HEAD_FORWARD}) and the client knows it by its id, which is
 * always the parent's id + 1.
 */
public final class LayeredHeadPart extends PartEntity<TheLayered> {
    private static final EntityDimensions SIZE = EntityDimensions.scalable(1.3F, 1.3F);

    LayeredHeadPart(TheLayered parent) {
        super(parent);
        refreshDimensions();
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
        return !isInvulnerableTo(source) && getParent().hurtHead(source, amount);
    }

    @Override
    public boolean is(Entity entity) {
        return this == entity || getParent() == entity;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return SIZE;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
