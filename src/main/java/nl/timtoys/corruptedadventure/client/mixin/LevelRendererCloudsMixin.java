package nl.timtoys.corruptedadventure.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import nl.timtoys.corruptedadventure.client.weather.Ring4Clouds;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While vanilla draws the clouds, the fog moves out by the height ring 4 raised them (sparring 2026-10-20, C1-3), so the
 * layer at Y 300 fades as vanilla's at Y 192 would instead of vanishing in the vertical fog ({@link Ring4Clouds}).
 * Restored on every return. Client-only mixin ({@code "client"} in the config).
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererCloudsMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "renderClouds", at = @At("HEAD"))
    private void corruptedadventure$cloudFogOut(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
                                                double camX, double camY, double camZ, CallbackInfo ci) {
        float raised = 0.0F;
        if (level != null) {
            float vanilla = ((DimensionSpecialEffectsAccessor) level.effects()).corruptedadventure$cloudLevel();
            raised = Ring4Clouds.cloudHeight(vanilla) - vanilla;
        }
        Ring4Clouds.beforeClouds(raised);
    }

    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void corruptedadventure$cloudFogBack(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
                                                 double camX, double camY, double camZ, CallbackInfo ci) {
        Ring4Clouds.afterClouds();
    }
}
