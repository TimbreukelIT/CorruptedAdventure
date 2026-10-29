package nl.timtoys.corruptedadventure.client.entity.layered;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.entity.nightmare.NightmareRenderer;
import nl.timtoys.corruptedadventure.entity.layered.LayeredPose;
import nl.timtoys.corruptedadventure.entity.layered.TheLayered;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * Draws The Layered: bone legs, the canyon-stone back, the fur belly ({@code the_layered.png}) and, from
 * {@link NightmareRenderer}, Herobrine's white eyes as the only glow. On a wall the whole model turns nose-up onto it
 * around {@link LayeredPose}'s pivot (smoothed by {@link TheLayered#wallTurn}), so its belly lies against the stone and
 * its back is a piece of the canyon wall. The living head follows where it looks; the two dead heads turn toward the
 * nearest player each at their own pace ({@link TheLayered#deadHeadYaw}), on top of their own lolling animation. Under the
 * sand ({@link TheLayered#isBurrowed}) it is not drawn.
 */
public final class TheLayeredRenderer extends NightmareRenderer<TheLayered> {
    private static final String LIVING_HEAD = "head_alive";
    private static final String[] DEAD_HEADS = {"head_dead_left", "head_dead_right"};

    private TheLayeredRenderer(EntityRendererProvider.Context context) {
        super(context, CorruptedAdventure.id("the_layered"), false, 2.2F);
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_LAYERED.get(), TheLayeredRenderer::new);
    }

    @Override
    public void render(TheLayered layered, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        // Under the sand only the moving sand shows (particles from the server).
        if (!layered.isBurrowed()) {
            super.render(layered, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }

    @Override
    protected void applyRotations(TheLayered layered, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        super.applyRotations(layered, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float turn = layered.wallTurn(partialTick);
        if (turn > 0.0F) {
            // In the model's frame (after the yaw) forward is -z: nose-up is a positive turn around x.
            float pivotZ = (float) -LayeredPose.WALL_PIVOT_FORWARD;
            float pivotY = (float) LayeredPose.WALL_PIVOT_UP;
            poseStack.translate(0.0F, pivotY, pivotZ);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F * turn));
            poseStack.translate(0.0F, -pivotY, -pivotZ);
        }
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TheLayered layered, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        float addYaw = 0.0F;
        float addPitch = 0.0F;
        String name = bone.getName();
        if (name.equals(LIVING_HEAD) && !layered.isDeadOrDying() && !layered.isFallen()) {
            addYaw = Mth.clamp(Mth.wrapDegrees(Mth.rotLerp(partialTick, layered.yHeadRotO, layered.yHeadRot)
                    - Mth.rotLerp(partialTick, layered.yBodyRotO, layered.yBodyRot)), -40.0F, 40.0F);
            addPitch = Mth.clamp(Mth.lerp(partialTick, layered.xRotO, layered.getXRot()), -30.0F, 30.0F);
        } else if (name.equals(DEAD_HEADS[0]) || name.equals(DEAD_HEADS[1])) {
            int index = name.equals(DEAD_HEADS[0]) ? 0 : 1;
            addYaw = layered.deadHeadYaw(index);
            addPitch = layered.deadHeadPitch(index);
        }
        if (addYaw == 0.0F && addPitch == 0.0F) {
            super.renderRecursively(poseStack, layered, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                    packedLight, packedOverlay, colour);
            return;
        }
        float rotX = bone.getRotX();
        float rotY = bone.getRotY();
        bone.setRotX(rotX + addPitch * Mth.DEG_TO_RAD);
        bone.setRotY(rotY + addYaw * Mth.DEG_TO_RAD);
        super.renderRecursively(poseStack, layered, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, colour);
        bone.setRotX(rotX);
        bone.setRotY(rotY);
    }
}
