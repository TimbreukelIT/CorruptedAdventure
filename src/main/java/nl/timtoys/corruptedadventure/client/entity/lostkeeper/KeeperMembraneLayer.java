package nl.timtoys.corruptedadventure.client.entity.lostkeeper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.Color;

/**
 * The see-through fins: a translucent second pass of the model with {@code the_lost_keeper_membrane.png}, which holds only
 * the vin wings, the crest membrane, the gill fins and the torn tail fin (pastel lilac in the mythical half, torn and
 * green in the rot). The base texture leaves those faces empty (cut out), so they are only drawn here, after the solid
 * body.
 */
final class KeeperMembraneLayer extends GeoRenderLayer<TheLostKeeper> {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/the_lost_keeper_membrane.png");

    KeeperMembraneLayer(TheLostKeeperRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, TheLostKeeper keeper, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (keeper.isInvisible()) {
            return;
        }
        RenderType membrane = RenderType.entityTranslucent(TEXTURE);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, keeper, membrane, bufferSource.getBuffer(membrane),
                partialTick, packedLight, packedOverlay, Color.WHITE.argbInt());
    }
}
