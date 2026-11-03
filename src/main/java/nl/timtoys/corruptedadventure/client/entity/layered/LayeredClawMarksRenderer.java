package nl.timtoys.corruptedadventure.client.entity.layered;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.layered.LayeredClawMarks;

/**
 * Draws {@link LayeredClawMarks}: one quad of gouges ({@code textures/entity/layered_claw_marks.png}, written by
 * {@code tools/textures/layered_claw_marks.py}) flat on the wall face, lit like the air in front of it, fading out with
 * {@link LayeredClawMarks#fade}. Each set is tilted and mirrored by its UUID, so no two look alike.
 */
public final class LayeredClawMarksRenderer extends EntityRenderer<LayeredClawMarks> {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/layered_claw_marks.png");
    /** Just off the stone: no flicker against the face, no visible gap. */
    private static final float OFF_FACE = 0.012F;
    private static final float MAX_TILT = 22.0F;

    LayeredClawMarksRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(LayeredClawMarks marks, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        float fade = marks.fade(partialTick);
        if (fade <= 0.0F) {
            return;
        }
        long seed = marks.getUUID().getLeastSignificantBits();
        float tilt = ((seed & 0xFF) / 255.0F * 2.0F - 1.0F) * MAX_TILT;
        boolean mirrored = (seed & 0x100) != 0;
        float half = LayeredClawMarks.SIZE / 2.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, half, 0.0F);
        // The quad's front (+z) turned to the facing: south 0, east 90, north 180, west 270 around y.
        poseStack.mulPose(Axis.YP.rotationDegrees(-marks.facing().toYRot()));
        poseStack.translate(0.0F, 0.0F, (float) -LayeredClawMarks.OUT + OFF_FACE);
        poseStack.mulPose(Axis.ZP.rotationDegrees(tilt));
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int alpha = (int) (fade * 255.0F);
        float u0 = mirrored ? 1.0F : 0.0F;
        float u1 = mirrored ? 0.0F : 1.0F;
        vertex(consumer, pose, -half, -half, u0, 1.0F, alpha, packedLight);
        vertex(consumer, pose, half, -half, u1, 1.0F, alpha, packedLight);
        vertex(consumer, pose, half, half, u1, 0.0F, alpha, packedLight);
        vertex(consumer, pose, -half, half, u0, 0.0F, alpha, packedLight);
        poseStack.popPose();
        super.render(marks, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int alpha,
                               int light) {
        consumer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, alpha).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LayeredClawMarks marks) {
        return TEXTURE;
    }
}
