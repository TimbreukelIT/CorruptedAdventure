package nl.timtoys.corruptedadventure.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import nl.timtoys.corruptedadventure.client.weather.Ring4Clouds;
import nl.timtoys.corruptedadventure.client.weather.StandstillClouds;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While vanilla draws the clouds, the fog moves out by the height ring 4 raised them (sparring 2026-10-20, C1-3), so the
 * layer at Y 300 fades as vanilla's at Y 192 would instead of vanishing in the vertical fog ({@link Ring4Clouds}).
 * Restored on every return. Since package 92 also the cloud clock: a ring 5 standstill slows the clouds to a halt
 * ({@link StandstillClouds}); vanilla's two reads of its tick counter and the partial tick get the slowed clock.
 * Client-only mixin ({@code "client"} in the config).
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererCloudsMixin {
    @Shadow
    private ClientLevel level;

    @Shadow
    private int ticks;

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

    /** First, before vanilla reads the tick counter: the slowed clock's partial tick (and its whole ticks for below). */
    @ModifyVariable(method = "renderClouds", at = @At("HEAD"), argsOnly = true)
    private float corruptedadventure$standstillPartialTick(float partialTick) {
        return StandstillClouds.partialTick(ticks, partialTick);
    }

    @Redirect(method = "renderClouds", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;ticks:I",
            opcode = Opcodes.GETFIELD))
    private int corruptedadventure$standstillTicks(LevelRenderer renderer) {
        return StandstillClouds.ticks();
    }

    @Inject(method = "renderClouds", at = @At("RETURN"))
    private void corruptedadventure$cloudFogBack(PoseStack poseStack, Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick,
                                                 double camX, double camY, double camZ, CallbackInfo ci) {
        Ring4Clouds.afterClouds();
    }
}
