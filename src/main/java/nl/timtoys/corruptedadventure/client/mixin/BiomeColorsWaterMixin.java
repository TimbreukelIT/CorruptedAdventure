package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import nl.timtoys.corruptedadventure.client.weather.SunderedFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The water colour of zone 4→5 drains to grey with the sky and fog (change 80, {@link SunderedFade}). The blended biome
 * colour stays the starting point; NeoForge has no event for it. Client-only mixin ({@code "client"} in the config).
 */
@Mixin(BiomeColors.class)
abstract class BiomeColorsWaterMixin {
    @Inject(method = "getAverageWaterColor", at = @At("RETURN"), cancellable = true)
    private static void corruptedadventure$greyWater(BlockAndTintGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int colour = cir.getReturnValueI();
        int greyed = SunderedFade.waterColor(colour, pos.getX(), pos.getZ());
        if (greyed != colour) {
            cir.setReturnValue(greyed);
        }
    }
}
