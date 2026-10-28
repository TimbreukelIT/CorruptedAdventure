package nl.timtoys.corruptedadventure.client.entity.hollow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * The corruption of ring 5 sliding down over The Hollow like a liquid: a translucent second pass of the model with
 * {@code the_hollow_corruption.png}, an 8-frame animated texture ({@code .mcmeta}) whose drips run down in world space.
 */
final class HollowCorruptionLayer extends GeoRenderLayer<TheHollow> {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/the_hollow_corruption.png");

    private final TheHollowRenderer hollowRenderer;

    HollowCorruptionLayer(TheHollowRenderer renderer) {
        super(renderer);
        this.hollowRenderer = renderer;
    }

    @Override
    public void render(PoseStack poseStack, TheHollow hollow, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (hollow.isInvisible()) {
            return;
        }
        AnimatableTexture.setAndUpdate(TEXTURE);
        RenderType corruption = RenderType.entityTranslucent(TEXTURE);
        int colour = Color.ofRGBA(1.0F, 1.0F, 1.0F, hollowRenderer.alpha(hollow)).argbInt();
        getRenderer().reRender(bakedModel, poseStack, bufferSource, hollow, corruption, bufferSource.getBuffer(corruption),
                partialTick, packedLight, packedOverlay, colour);
    }
}
