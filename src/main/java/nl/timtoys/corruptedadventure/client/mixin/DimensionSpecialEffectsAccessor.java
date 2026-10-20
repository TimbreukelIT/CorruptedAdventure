package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla's own cloud height, before {@code DimensionSpecialEffectsMixin} raises it (for {@code LevelRendererCloudsMixin}). */
@Mixin(DimensionSpecialEffects.class)
public interface DimensionSpecialEffectsAccessor {
    @Accessor("cloudLevel")
    float corruptedadventure$cloudLevel();
}
