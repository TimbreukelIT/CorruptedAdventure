package nl.timtoys.corruptedadventure.client.entity.unfinished;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.entity.unfinished.MemoryShard;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * A memory shard of The Unfinished: its block, tumbling on its own axis (the speed and axis from its id), and while it
 * hovers by him not quite loaded yet: now and then a frame it is not there.
 */
public final class MemoryShardRenderer extends EntityRenderer<MemoryShard> {
    private final BlockRenderDispatcher blocks;

    private MemoryShardRenderer(EntityRendererProvider.Context context) {
        super(context);
        blocks = context.getBlockRenderDispatcher();
        shadowRadius = 0.4F;
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.MEMORY_SHARD.get(), MemoryShardRenderer::new);
    }

    @Override
    public void render(MemoryShard shard, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        BlockState state = shard.block();
        if (state.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        int id = shard.getId();
        if (shard.isNoGravity() && ((shard.tickCount + id) * 0x9E3779B1 >>> 28) == 0) {
            return;
        }
        float time = shard.tickCount + partialTick;
        float speed = 6.0F + (id & 7) * 2.0F;
        poseStack.pushPose();
        poseStack.translate(0.0, 0.49, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * speed + id * 37));
        poseStack.mulPose(((id & 1) == 0 ? Axis.XP : Axis.ZP).rotationDegrees(time * speed * 0.7F));
        poseStack.translate(-0.5, -0.5, -0.5);
        blocks.renderSingleBlock(state, poseStack, buffers, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(shard, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(MemoryShard shard) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
