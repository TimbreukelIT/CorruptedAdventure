package nl.timtoys.corruptedadventure.client.entity.lostkeeper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.entity.nightmare.NightmareRenderer;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Draws The Lost Keeper: the pearl-and-rot body ({@code the_lost_keeper.png}), from {@link NightmareRenderer} the emissive
 * eyes, crown, glow spots and poison lesions, and the see-through fins and crest ({@link KeeperMembraneLayer}). Every bone
 * is laid along the body chain right before it is drawn ({@link KeeperPose}), so the model follows the same nodes as the
 * hitboxes. No head tracking: the whole head turns with the look through {@code neck_mount}.
 */
public final class TheLostKeeperRenderer extends NightmareRenderer<TheLostKeeper> {
    private TheLostKeeperRenderer(EntityRendererProvider.Context context) {
        super(context, CorruptedAdventure.id("the_lost_keeper"), false, 2.0F);
        addRenderLayer(new KeeperMembraneLayer(this));
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_LOST_KEEPER.get(), TheLostKeeperRenderer::new);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TheLostKeeper keeper, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay, int colour) {
        KeeperPose.apply(keeper, bone, partialTick);
        super.renderRecursively(poseStack, keeper, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
    }
}
