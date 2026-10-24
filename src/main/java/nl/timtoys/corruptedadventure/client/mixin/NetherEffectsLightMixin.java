package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import nl.timtoys.corruptedadventure.client.weather.NetherRing5Light;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Nether ring 5's neutral, lighter lightmap ({@link NetherRing5Light}): the Nether's special effects get NeoForge's
 * {@code adjustLightmapColors} hook (a default method of {@code IDimensionSpecialEffectsExtension}, empty for the
 * Nether), which {@code LightTexture.updateLightTexture} calls for every lightmap pixel. Client-only mixin
 * ({@code "client"} in the config).
 */
@Mixin(DimensionSpecialEffects.NetherEffects.class)
abstract class NetherEffectsLightMixin {
    public void adjustLightmapColors(ClientLevel level, float partialTicks, float skyDarken, float blockLightRedFlicker,
                                     float skyLight, int pixelX, int pixelY, Vector3f colors) {
        NetherRing5Light.adjust(colors);
    }
}
