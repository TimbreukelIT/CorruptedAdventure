package nl.timtoys.corruptedadventure.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * The fire and poison arrows (fase-2-items.md §6.4, §6.7): a plain arrow whose hit adds its {@link ArrowEffect}. One
 * class for both; the effect follows from the entity type, because {@link AbstractArrow}'s constructor already asks
 * for the pickup item, before a field of ours could be set.
 */
public final class EffectArrow extends AbstractArrow {
    public EffectArrow(EntityType<? extends EffectArrow> type, Level level) {
        super(type, level);
    }

    /** Shot by {@code owner} from {@code weapon} (a bow or crossbow). */
    public EffectArrow(ArrowEffect effect, Level level, LivingEntity owner, ItemStack pickup, @Nullable ItemStack weapon) {
        super(effect.entityType(), owner, level, pickup, weapon);
    }

    /** Shot without a shooter (a dispenser). */
    public EffectArrow(ArrowEffect effect, Level level, double x, double y, double z, ItemStack pickup) {
        super(effect.entityType(), x, y, z, level, pickup, null);
    }

    public ArrowEffect effect() {
        return ArrowEffect.of(getType());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && !inGround) {
            level().addParticle(effect().trail(), getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        effect().apply(target, getEffectSource());
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(effect().item());
    }
}
