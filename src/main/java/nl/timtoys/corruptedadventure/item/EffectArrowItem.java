package nl.timtoys.corruptedadventure.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import nl.timtoys.corruptedadventure.entity.ArrowEffect;
import nl.timtoys.corruptedadventure.entity.EffectArrow;

import javax.annotation.Nullable;

/**
 * The fire, poison and glitch arrow items (fase-2-items.md §6.4, §6.7, §7.4): ammo for the bow and the crossbow (through
 * {@code #minecraft:arrows}) and, like vanilla's arrows, shot by a dispenser.
 */
public final class EffectArrowItem extends ArrowItem {
    private final ArrowEffect effect;

    public EffectArrowItem(ArrowEffect effect, Properties properties) {
        super(properties);
        this.effect = effect;
    }

    /** Vanilla registers the dispenser behaviour of its arrows at bootstrap, before any mod item exists. */
    public static void register(IEventBus modBus) {
        modBus.addListener(EffectArrowItem::commonSetup);
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        // The dispenser's behaviour map is a plain map; common setup runs in parallel across mods.
        event.enqueueWork(() -> {
            for (ArrowEffect effect : ArrowEffect.values()) {
                DispenserBlock.registerProjectileBehavior(effect.item());
            }
        });
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, @Nullable ItemStack weapon) {
        return new EffectArrow(effect, level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        EffectArrow arrow = new EffectArrow(effect, level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }
}
