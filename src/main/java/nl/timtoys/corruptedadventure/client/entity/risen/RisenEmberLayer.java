package nl.timtoys.corruptedadventure.client.entity.risen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.risen.RisenState;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * The embers on The Risen: an additive, full-bright pass of the model with {@code the_risen_embers.png}, an 8-frame
 * animated texture ({@code .mcmeta}): the furnace breathes, sparks run out along the feathers, the choir's faces sing in
 * turn. Stunned, the embers go out; dying, they go out halfway through.
 */
final class RisenEmberLayer extends GeoRenderLayer<TheRisen> {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/the_risen_embers.png");

    RisenEmberLayer(GeoRenderer<TheRisen> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TheRisen risen, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (risen.isInvisible() || risen.state() == RisenState.STUNNED || risen.deathTime > 30) {
            return;
        }
        AnimatableTexture.setAndUpdate(TEXTURE);
        RenderType embers = RenderType.eyes(TEXTURE);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, risen, embers, bufferSource.getBuffer(embers),
                partialTick, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
