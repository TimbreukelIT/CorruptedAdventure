package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import nl.timtoys.corruptedadventure.client.weather.Ring4Clouds;
import nl.timtoys.corruptedadventure.client.weather.Ring4Dusk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ring 4's dusk glow and cloud height (sparring 2026-10-20, C1-2 and C1-3): vanilla's sunrise colour (the glow around a
 * rising or setting sun, also mixed into the fog when looking at it) → {@link Ring4Dusk#sunriseColor}, the cloud height
 * → {@link Ring4Clouds#cloudHeight}. Both stay vanilla's outside ring 4. NeoForge has no event for either. Client-only
 * mixin ({@code "client"} in the config).
 */
@Mixin(DimensionSpecialEffects.class)
abstract class DimensionSpecialEffectsMixin {
    @Inject(method = "getSunriseColor", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$ring4Dusk(float timeOfDay, float partialTicks, CallbackInfoReturnable<float[]> cir) {
        float[] vanilla = cir.getReturnValue();
        float[] tinted = Ring4Dusk.sunriseColor(vanilla, timeOfDay);
        if (tinted != vanilla) {
            cir.setReturnValue(tinted);
        }
    }

    @Inject(method = "getCloudHeight", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$ring4Clouds(CallbackInfoReturnable<Float> cir) {
        float vanilla = cir.getReturnValueF();
        float raised = Ring4Clouds.cloudHeight(vanilla);
        if (raised != vanilla) {
            cir.setReturnValue(raised);
        }
    }
}
