package nl.timtoys.corruptedadventure.client.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import nl.timtoys.corruptedadventure.client.weather.Ring5Sky;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Ring 5's black sky dims the sun and the moon (package 98, V98-1 A, {@link Ring5Sky#sunFactor}). In
 * {@code LevelRenderer#renderSky} the third {@code RenderSystem.setShaderColor} sets the alpha of the sun and moon
 * quads ({@code 1 − rain}); the stars set their own colour after that, so only the sun and moon change. Checked with
 * {@code javap} against NeoForge 21.1.252 (calls at offsets 203, 280, 576); a different order fails loudly at load
 * ({@code defaultRequire} 1). Client-only mixin ({@code "client"} in the config).
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererSkyMixin {
    @ModifyArg(method = "renderSky", index = 3, at = @At(value = "INVOKE", ordinal = 2,
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"))
    private float corruptedadventure$dimSunAndMoon(float alpha) {
        return alpha * Ring5Sky.sunFactor();
    }
}
