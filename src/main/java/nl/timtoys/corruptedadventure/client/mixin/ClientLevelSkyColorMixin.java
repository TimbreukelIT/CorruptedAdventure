package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.client.weather.AshClouds;
import nl.timtoys.corruptedadventure.client.weather.Ring4Clouds;
import nl.timtoys.corruptedadventure.client.weather.Ring4Dusk;
import nl.timtoys.corruptedadventure.client.weather.Ring4Fog;
import nl.timtoys.corruptedadventure.client.weather.Ring5Sky;
import nl.timtoys.corruptedadventure.client.weather.RingWeatherClientHooks;
import nl.timtoys.corruptedadventure.client.weather.SunderedFade;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The only hook into the sky colour (NeoForge 1.21.1 has no event for it): the ring weather renderers may tint it,
 * e.g. the red sky of an eruption. Vanilla's own colour (biome, time of day, rain) stays the starting point; the fog
 * colour that vanilla derives from it follows automatically. Since package 50 also the cloud colour: the clouds of
 * ring 3 are dark with ash ({@link AshClouds}). Since the ring 4 sky change (sparring 2026-10-20) ring 4's dusk hue,
 * blighted haze and low mist tint the sky ({@link Ring4Dusk}, {@link Ring4Fog}) and its clouds ({@link Ring4Clouds}).
 * Since package 98 ring 5's outer band blacks the sky out and lets the stars shine by day ({@link Ring5Sky}).
 * Client-only mixin ({@code "client"} in the config).
 */
@Mixin(ClientLevel.class)
abstract class ClientLevelSkyColorMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        // Ring 4's own look first (dusk, blighted haze, low mist; sparring 2026-10-20), then the weather on top.
        Vec3 tinted = Ring4Dusk.skyColor(cir.getReturnValue(), level.getTimeOfDay(partialTick));
        tinted = Ring4Fog.skyColor(level, tinted, partialTick);
        tinted = RingWeatherClientHooks.skyColor(level, pos, tinted, partialTick);
        // Ring 5's outer band goes black over the weather (98).
        tinted = Ring5Sky.skyColor(tinted);
        // Zone 4→5 drains to grey after the weather (change 80).
        tinted = SunderedFade.skyColor(level, pos, tinted);
        if (tinted != cir.getReturnValue()) {
            cir.setReturnValue(tinted);
        }
    }

    @Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$ring5Stars(float partialTick, CallbackInfoReturnable<Float> cir) {
        float vanilla = cir.getReturnValueF();
        float stars = Ring5Sky.starBrightness(vanilla);
        if (stars != vanilla) {
            cir.setReturnValue(stars);
        }
    }

    @Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
    private void corruptedadventure$ashClouds(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        ClientLevel level = (ClientLevel) (Object) this;
        Vec3 tinted = AshClouds.cloudColor(level, cir.getReturnValue());
        tinted = Ring4Clouds.cloudColor(level, tinted, partialTick);
        if (tinted != cir.getReturnValue()) {
            cir.setReturnValue(tinted);
        }
    }
}
