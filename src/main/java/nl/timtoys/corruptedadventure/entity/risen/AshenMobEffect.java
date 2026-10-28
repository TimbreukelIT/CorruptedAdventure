package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Ash of The Risen's wings on you: one level per layer ({@link RisenAshRain#MAX_LAYERS} at most). Every layer takes a
 * sixth of your speed and jump; with all six you stand fast. The layers themselves (adding, falling off, knocking off)
 * are {@link RisenAshRain}'s; the effect carries the slowness, the icon, the ash particles and the sync.
 */
public final class AshenMobEffect extends MobEffect {
    private static final double PER_LAYER = -1.0 / RisenAshRain.MAX_LAYERS;

    public AshenMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x6F6763, ParticleTypes.ASH);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, CorruptedAdventure.id("ashen_speed"), PER_LAYER,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, CorruptedAdventure.id("ashen_jump"), PER_LAYER,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
