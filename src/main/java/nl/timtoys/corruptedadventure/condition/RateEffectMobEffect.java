package nl.timtoys.corruptedadventure.condition;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The status effect of a {@link RateEffect} ("cooled"): no behaviour of its own, {@link RateEffects} reads whether a
 * player has it. Being a vanilla effect gives the icon with its timer, the sync, saving with the player and the reset on
 * death for free; milk clears it. There is no potion of it, so it cannot be brewed.
 */
public final class RateEffectMobEffect extends MobEffect {
    public RateEffectMobEffect(int color) {
        super(MobEffectCategory.BENEFICIAL, color);
    }
}
