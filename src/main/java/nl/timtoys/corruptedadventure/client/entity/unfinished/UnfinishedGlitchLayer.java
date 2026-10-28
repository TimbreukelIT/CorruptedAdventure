package nl.timtoys.corruptedadventure.client.entity.unfinished;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.texture.AnimatableTexture;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * The glitch crawling over The Unfinished: a translucent, full-bright second pass of the model with
 * {@code the_unfinished_glitch.png}, a 4-frame animated texture ({@code .mcmeta}) of scan-line tears and dead pixels
 * running up his body in world space.
 */
final class UnfinishedGlitchLayer extends GeoRenderLayer<TheUnfinished> {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/the_unfinished_glitch.png");

    UnfinishedGlitchLayer(GeoRenderer<TheUnfinished> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TheUnfinished boss, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (boss.isInvisible()) {
            return;
        }
        AnimatableTexture.setAndUpdate(TEXTURE);
        RenderType glitch = RenderType.entityTranslucentEmissive(TEXTURE);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, boss, glitch, bufferSource.getBuffer(glitch), partialTick,
                packedLight, packedOverlay, 0xFFFFFFFF);
    }
}
