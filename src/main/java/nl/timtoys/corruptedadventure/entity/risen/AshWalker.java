package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/**
 * An ash walker: one of The Risen's victims, fallen from its feathers (design doc ring 3, "Slachtoffers loslaten").
 * <b>Minimal placeholder</b> (backlog: the real ring 3 mob, its model and its own spawning): a zombie that does not burn
 * in the sun or drown into a drowned, takes no fall damage (it drops from the sky) and is fire immune (entity type). The
 * client draws it with the vanilla husk renderer.
 */
public class AshWalker extends Zombie {
    public AshWalker(EntityType<? extends AshWalker> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }
}
