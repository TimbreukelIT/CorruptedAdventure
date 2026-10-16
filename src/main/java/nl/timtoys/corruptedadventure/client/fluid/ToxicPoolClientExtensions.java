package nl.timtoys.corruptedadventure.client.fluid;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModFluidTypes;
import org.joml.Vector3f;

/**
 * The look of the toxic pool (package 72): T's opaque, animated textures with their fixed colour (white tint), and a
 * thick toxic-green fog when the camera is inside it, so the pool is murky unlike the clear water.
 */
public final class ToxicPoolClientExtensions implements IClientFluidTypeExtensions {
    private static final ResourceLocation STILL = CorruptedAdventure.id("block/toxic_pool_still");
    private static final ResourceLocation FLOW = CorruptedAdventure.id("block/toxic_pool_flow");
    /** Fog colour under the surface: the pool's toxic green. */
    private static final Vector3f FOG_COLOR = new Vector3f(0x4E / 255.0F, 0x8A / 255.0F, 0x14 / 255.0F);
    /** You see about this far inside the pool (blocks): murky, but not blind like lava. */
    private static final float FOG_END = 4.0F;

    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new ToxicPoolClientExtensions(), ModFluidTypes.TOXIC_POOL.get());
    }

    @Override
    public ResourceLocation getStillTexture() {
        return STILL;
    }

    @Override
    public ResourceLocation getFlowingTexture() {
        return FLOW;
    }

    @Override
    public int getTintColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                   float darkenWorldAmount, Vector3f fluidFogColor) {
        return new Vector3f(FOG_COLOR);
    }

    @Override
    public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance, float partialTick,
                                float nearDistance, float farDistance, FogShape shape) {
        RenderSystem.setShaderFogStart(0.0F);
        RenderSystem.setShaderFogEnd(FOG_END);
    }
}
