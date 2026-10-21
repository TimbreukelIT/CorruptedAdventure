package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import nl.timtoys.corruptedadventure.client.weather.Ring5SoundDamping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The ring 5 weather's hold on the volume (package 92, {@link Ring5SoundDamping}): the category volume that every
 * sound's volume is multiplied by, at its start, every tick of a tickable sound and on every refresh of the channels.
 * {@code MASTER} (the UI) stays untouched. NeoForge 1.21.1 has no event that reaches sounds that already play.
 * Client-only mixin ({@code "client"} in the config).
 */
@Mixin(SoundEngine.class)
abstract class SoundEngineVolumeMixin {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$ring5Damping(SoundSource category, CallbackInfoReturnable<Float> cir) {
        float factor = Ring5SoundDamping.volume(category);
        if (factor < 1.0F) {
            cir.setReturnValue(cir.getReturnValueF() * factor);
        }
    }
}
