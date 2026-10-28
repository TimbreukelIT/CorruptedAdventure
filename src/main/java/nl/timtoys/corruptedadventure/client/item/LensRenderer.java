package nl.timtoys.corruptedadventure.client.item;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.timtoys.corruptedadventure.item.SkyGlassLensItem;

import java.util.OptionalDouble;

/**
 * Draws the sky glass lens's outlines while the local player looks through it: lines of vanilla's width without a depth
 * test, after the whole level (the lens sees through the ground), without fog. Glitch blocks violet, void holes cyan,
 * missing chunks pale white (provisional colours).
 */
final class LensRenderer {
    static final int GLITCH_COLOUR = 0xB15CFF;
    static final int HOLE_COLOUR = 0x4FE3F0;
    static final int CHUNK_COLOUR = 0xEDEBFF;
    private static final float ALPHA = 0.9F;

    /** Vanilla's {@code lines}, but seen through everything and without writing depth. */
    private static final RenderType SEE_THROUGH_LINES = RenderType.create("corruptedadventure_lens_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 1536, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.MAIN_TARGET)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .createCompositeState(false));

    private LensRenderer() {
    }

    static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !SkyGlassLensItem.lookingThrough(minecraft.player) || !LensMarkers.any(minecraft.level)) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(event.getModelViewMatrix());
        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(SEE_THROUGH_LINES);
        draw(lines, pose, camera, LensMarkers.chunks(), CHUNK_COLOUR);
        draw(lines, pose, camera, LensMarkers.holes(), HOLE_COLOUR);
        draw(lines, pose, camera, LensMarkers.glitches(), GLITCH_COLOUR);
        buffers.endBatch(SEE_THROUGH_LINES);
    }

    private static void draw(VertexConsumer lines, PoseStack.Pose pose, Vec3 camera, int[] segments, int colour) {
        int argb = Math.round(ALPHA * 255.0F) << 24 | colour;
        for (int i = 0; i + 5 < segments.length; i += 6) {
            float x0 = (float) (segments[i] - camera.x);
            float y0 = (float) (segments[i + 1] - camera.y);
            float z0 = (float) (segments[i + 2] - camera.z);
            float x1 = (float) (segments[i + 3] - camera.x);
            float y1 = (float) (segments[i + 4] - camera.y);
            float z1 = (float) (segments[i + 5] - camera.z);
            float dx = x1 - x0;
            float dy = y1 - y0;
            float dz = z1 - z0;
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length <= 0.0F) {
                continue;
            }
            dx /= length;
            dy /= length;
            dz /= length;
            lines.addVertex(pose, x0, y0, z0).setColor(argb).setNormal(pose, dx, dy, dz);
            lines.addVertex(pose, x1, y1, z1).setColor(argb).setNormal(pose, dx, dy, dz);
        }
    }
}
