package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.client.weather.RingWeatherClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only hook into the sky colour (NeoForge 1.21.1 has no event for it): the ring weather renderers may tint it,
 * e.g. the red sky of an eruption. Vanilla's own colour (biome, time of day, rain) stays the starting point; the fog
 * colour that vanilla derives from it follows automatically. Client-only mixin ({@code "client"} in the config).
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelSkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        Vec3 tinted = RingWeatherClientHooks.skyColor((ClientLevel) (Object) this, pos, cir.getReturnValue(), partialTick);
        if (tinted != cir.getReturnValue()) {
            cir.setReturnValue(tinted);
        }
    }
}
