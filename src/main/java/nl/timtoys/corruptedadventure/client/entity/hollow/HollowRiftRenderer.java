package nl.timtoys.corruptedadventure.client.entity.hollow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.entity.hollow.HollowRift;
import nl.timtoys.corruptedadventure.entity.hollow.RiftPhase;

/**
 * Draws a {@link HollowRift}: a ragged vertical wound (up to 3 × 7 blocks) filled with the End portal's starry void,
 * with a flickering, glowing rim of corruption (additive, so it glows at night). The outline is fixed per rift (seeded
 * by its id); the phase sets how open it is: a growing hairline for the omen, tearing open, breathing while open, the
 * two white eyes deep inside before The Hollow comes through, closing up at the end. Drawn on both sides.
 */
public final class HollowRiftRenderer extends EntityRenderer<HollowRift> {
    private static final int SEGMENTS = 16;
    private static final float HEIGHT = 7.0F;
    private static final float HALF_WIDTH = 1.5F;
    private static final float HAIRLINE = 0.04F;
    // Rim colours: the corruption palette (9C7AC4 bright, 6A4A8C, F800F8 glitch).
    private static final int[] RIM_INNER = {0x9C, 0x7A, 0xC4};
    private static final int[] RIM_OUTER = {0x6A, 0x4A, 0x8C};
    private static final int[] GLITCH = {0xF8, 0x00, 0xF8};

    HollowRiftRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HollowRift rift, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        RiftPhase phase = rift.phase();
        float progress = rift.phaseProgress(partialTick);
        float time = rift.tickCount + partialTick;
        float open;
        float height;
        switch (phase) {
            case OMEN -> {
                open = 0.0F;
                height = 2.5F + 3.0F * progress;
            }
            case OPENING -> {
                open = progress * progress * (3.0F - 2.0F * progress);
                height = 5.5F + 1.5F * progress;
            }
            case CLOSING -> {
                open = 1.0F - progress;
                height = HEIGHT * (1.0F - 0.3F * progress);
            }
            default -> {
                open = 1.0F + 0.04F * Mth.sin(time * 0.15F);
                height = HEIGHT;
            }
        }
        float[][] edge = outline(rift.getId(), height, open);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-rift.getYRot()));
        PoseStack.Pose pose = poseStack.last();
        if (open > 0.0F) {
            VertexConsumer voidFill = buffers.getBuffer(RenderType.endPortal());
            for (int i = 0; i < SEGMENTS; i++) {
                float[] a = edge[i];
                float[] b = edge[i + 1];
                quad(voidFill, pose, a[0], a[2], b[0], b[2], b[1], a[1]);
            }
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        rim(glow, pose, edge, time, open);
        if (phase == RiftPhase.EYES || phase == RiftPhase.EMERGE && progress < 0.3F) {
            eyes(glow, pose, height);
        }
        poseStack.popPose();
        super.render(rift, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /**
     * The left and right edge per height step: {@code {left x, right x, y}}. A lens that is pointed at both ends, its
     * sides jagged and its middle line wandering, the same every frame for one rift.
     */
    private static float[][] outline(int seed, float height, float open) {
        RandomSource random = RandomSource.create(seed * 341873128712L);
        float[][] edge = new float[SEGMENTS + 1][3];
        float bottom = (HEIGHT - height) * 0.5F + 0.3F;
        for (int i = 0; i <= SEGMENTS; i++) {
            float t = (float) i / SEGMENTS;
            float lens = (float) Math.pow(Mth.sin(t * Mth.PI), 0.65);
            float middle = 0.25F * Mth.sin(t * 5.3F + seed) * lens;
            float jagLeft = 1.0F + (random.nextFloat() - 0.5F) * 0.55F;
            float jagRight = 1.0F + (random.nextFloat() - 0.5F) * 0.55F;
            float half = Math.max(HAIRLINE, HALF_WIDTH * lens * open);
            edge[i][0] = middle - half * jagLeft;
            edge[i][1] = middle + half * jagRight;
            edge[i][2] = bottom + t * height;
        }
        return edge;
    }

    /** A quad from the left edge (x0 at y0, x1 at y1) to the right edge (rx1 at y1, rx0 at y0), both windings. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                             float rx1, float rx0) {
        consumer.addVertex(pose, x0, y0, 0.0F);
        consumer.addVertex(pose, rx0, y0, 0.0F);
        consumer.addVertex(pose, rx1, y1, 0.0F);
        consumer.addVertex(pose, x1, y1, 0.0F);
        consumer.addVertex(pose, x1, y1, 0.0F);
        consumer.addVertex(pose, rx1, y1, 0.0F);
        consumer.addVertex(pose, rx0, y0, 0.0F);
        consumer.addVertex(pose, x0, y0, 0.0F);
    }

    /** The glowing rim along both edges, a flickering band that fades outward; a hairline while the rift is closed. */
    private static void rim(VertexConsumer consumer, PoseStack.Pose pose, float[][] edge, float time, float open) {
        for (int side = 0; side < 2; side++) {
            float out = side == 0 ? -1.0F : 1.0F;
            for (int i = 0; i < SEGMENTS; i++) {
                float[] a = edge[i];
                float[] b = edge[i + 1];
                float flicker = 0.5F + 0.5F * Mth.sin(time * 0.6F + i * 1.7F + side * 3.1F);
                float width = (0.08F + 0.22F * flicker) * Math.max(0.35F, Math.min(1.0F, open + 0.35F));
                boolean glitch = ((int) (time * 0.5F) + i * 7 + side) % 23 == 0;
                int[] inner = glitch ? GLITCH : RIM_INNER;
                int alpha = (int) (150 + 100 * flicker);
                band(consumer, pose, a[side], a[2], b[side], b[2], out * width, inner, alpha);
            }
        }
    }

    private static void band(VertexConsumer consumer, PoseStack.Pose pose, float x0, float y0, float x1, float y1,
                             float out, int[] inner, int alpha) {
        for (float z : new float[] {0.01F, -0.01F}) {
            boolean front = z > 0.0F;
            float[][] corners = {{x0, y0, 1}, {x1, y1, 1}, {x1 + out, y1, 0}, {x0 + out, y0, 0}};
            for (int k = 0; k < 4; k++) {
                float[] c = corners[front == (out > 0.0F) ? k : 3 - k];
                boolean in = c[2] > 0.5F;
                int[] rgb = in ? inner : RIM_OUTER;
                consumer.addVertex(pose, c[0], c[1], z).setColor(rgb[0], rgb[1], rgb[2], in ? alpha : 0);
            }
        }
    }

    /** Herobrine's white eyes, deep in the rift. */
    private static void eyes(VertexConsumer consumer, PoseStack.Pose pose, float height) {
        float y = (HEIGHT - height) * 0.5F + 0.3F + height * 0.66F;
        for (float x : new float[] {-0.3F, 0.3F}) {
            for (float z : new float[] {0.02F, -0.02F}) {
                float s = 0.07F;
                boolean front = z > 0.0F;
                float[][] corners = {{x - s, y - s}, {x + s, y - s}, {x + s, y + s}, {x - s, y + s}};
                for (int k = 0; k < 4; k++) {
                    float[] c = corners[front ? k : 3 - k];
                    consumer.addVertex(pose, c[0], c[1], z).setColor(255, 255, 255, 255);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(HollowRift rift) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
