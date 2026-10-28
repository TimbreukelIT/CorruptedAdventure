package nl.timtoys.corruptedadventure.entity;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.FastColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

import java.util.function.Supplier;

/**
 * What a ring arrow adds to its hit (fase-2-items.md §6.4, §6.7, §7.4). Each effect has its own entity type and item; the
 * registry objects are looked up lazily, since this enum may load while the registers are still being filled.
 */
public enum ArrowEffect {
    /**
     * Ring 3: the target burns 5 s. The arrow itself is never on fire, so it lights no blocks (vanilla lets a burning
     * arrow prime TNT and light campfires and candles).
     */
    FIRE(() -> ModEntityTypes.FIRE_ARROW.get(), () -> ModItems.FIRE_ARROW.get()) {
        @Override
        void apply(LivingEntity target, Entity source) {
            target.igniteForSeconds(SECONDS);
        }

        @Override
        ParticleOptions trail() {
            return ParticleTypes.FLAME;
        }
    },
    /** Ring 4: vanilla Poison I for 5 s, the full time (a tipped arrow's potion lasts an eighth). */
    POISON(() -> ModEntityTypes.POISON_ARROW.get(), () -> ModItems.POISON_ARROW.get()) {
        @Override
        void apply(LivingEntity target, Entity source) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, SECONDS * 20), source);
        }

        @Override
        ParticleOptions trail() {
            return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, FastColor.ARGB32.opaque(MobEffects.POISON.value().getColor()));
        }
    },
    /** Ring 5: a hit mob hops 1–5 blocks to a safe spot ({@link GlitchHop}); a hit player stays, for now. */
    GLITCH(() -> ModEntityTypes.GLITCH_ARROW.get(), () -> ModItems.GLITCH_ARROW.get()) {
        @Override
        void apply(LivingEntity target, Entity source) {
            if (target instanceof Mob mob) {
                GlitchHop.tryHop(mob);
            }
        }

        @Override
        ParticleOptions trail() {
            return ModParticleTypes.GLITCH_PIXEL.get();
        }
    };

    static final int SECONDS = 5;

    private final Supplier<EntityType<EffectArrow>> entityType;
    private final Supplier<Item> item;

    ArrowEffect(Supplier<EntityType<EffectArrow>> entityType, Supplier<Item> item) {
        this.entityType = entityType;
        this.item = item;
    }

    public EntityType<EffectArrow> entityType() {
        return entityType.get();
    }

    public Item item() {
        return item.get();
    }

    /** On the server, after the arrow hurt {@code target}; {@code source} is the shooter, or the arrow without one. */
    abstract void apply(LivingEntity target, Entity source);

    /** The particle the arrow leaves while it flies (client side). */
    abstract ParticleOptions trail();

    static ArrowEffect of(EntityType<?> type) {
        for (ArrowEffect effect : values()) {
            if (effect.entityType() == type) {
                return effect;
            }
        }
        throw new IllegalArgumentException("not a ring arrow: " + type);
    }
}
