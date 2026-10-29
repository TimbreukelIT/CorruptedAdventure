package nl.timtoys.corruptedadventure.client.entity.hollow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.HollowVoidClaw;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws a {@link HollowVoidClaw}. First the warning: purple cracks that run out over the ground from a growing hole of
 * void, pulsing faster toward the bite. Then the hand (GeckoLib, {@code hollow_void_claw}: stone eaten by corruption,
 * hooked void-black talons, emissive only in the seams) claws up out of the hole, clenches and sinks back; below the
 * ground the terrain hides it. The cracks are fixed per claw (seeded by its id); the synced warmup times them.
 */
public final class HollowVoidClawRenderer extends GeoEntityRenderer<HollowVoidClaw> {
    private static final int CRACKS = 6;
    private static final int CRACK_STEPS = 4;
    // The corruption palette: 2B1F38 bed, F800F8 glitch; the glowing core a little more saturated than 9C7AC4.
    private static final int[] RIM = {0xB0, 0x60, 0xF0};
    private static final int[] BED = {0x2B, 0x1F, 0x38};
    private static final int[] GLITCH = {0xF8, 0x00, 0xF8};

    HollowVoidClawRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CorruptedAdventure.id("hollow_void_claw")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HollowVoidClaw claw, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        boolean open = claw.clientSinceBite(partialTick) >= 0.0F;
        float warm = open ? 1.0F : Math.min(1.0F, (claw.tickCount + partialTick) / claw.warmup());
        float time = claw.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-claw.getYRot()));
        PoseStack.Pose pose = poseStack.last();
        float[][] cracks = cracks(RandomSource.create(claw.getId() * 7349241L), warm);
        // A dark purple bed (blended), then a thin glowing core (additive): additive alone washes out to white on grass.
        drawCracks(buffers.getBuffer(RenderType.debugQuads()), pose, cracks, 2.4F, BED, (int) (150 + 80 * warm), time);
        float pulse = 0.5F + 0.5F * Mth.sin(time * (0.3F + 0.9F * warm));
        drawCracks(buffers.getBuffer(RenderType.lightning()), pose, cracks, 1.0F, RIM,
                (int) (90 + 150 * (open ? 1.0F : pulse * warm)), time);
        hole(buffers.getBuffer(RenderType.endPortal()), pose, 0.12F + 0.38F * warm);
        poseStack.popPose();
        super.render(claw, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /**
     * Jagged cracks from the middle outward, longer the nearer the bite: segments {@code {x0, z0, x1, z1, width}}, the
     * same every frame for one claw.
     */
    private static float[][] cracks(RandomSource random, float warm) {
        float[][] segments = new float[CRACKS * CRACK_STEPS][];
        for (int i = 0; i < CRACKS; i++) {
            float angle = (i + random.nextFloat() * 0.7F) * Mth.TWO_PI / CRACKS;
            float length = (0.6F + random.nextFloat() * 0.55F) * (0.25F + 0.75F * warm);
            float x = 0.0F;
            float z = 0.0F;
            for (int step = 0; step < CRACK_STEPS; step++) {
                angle += (random.nextFloat() - 0.5F) * 0.9F;
                float next = length / CRACK_STEPS;
                float nx = x + Mth.cos(angle) * next;
                float nz = z + Mth.sin(angle) * next;
                float width = 0.05F * (1.0F - (float) step / CRACK_STEPS) + 0.012F;
                segments[i * CRACK_STEPS + step] = new float[] {x, z, nx, nz, width};
                x = nx;
                z = nz;
            }
        }
        return segments;
    }

    private static void drawCracks(VertexConsumer consumer, PoseStack.Pose pose, float[][] segments, float widen, int[] colour,
                                   int alpha, float time) {
        for (int i = 0; i < segments.length; i++) {
            float[] c = segments[i];
            int[] tint = colour == RIM && (i * 5 + (int) (time * 0.4F)) % 17 == 0 ? GLITCH : colour;
            strip(consumer, pose, c[0], c[1], c[2], c[3], c[4] * widen, tint, alpha);
        }
    }

    /** A flat strip on the ground from (x0, z0) to (x1, z1), {@code width} to each side, both windings. */
    private static void strip(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float width,
                              int[] colour, int alpha) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = Math.max(1.0E-4F, Mth.sqrt(dx * dx + dz * dz));
        float px = -dz / len * width;
        float pz = dx / len * width;
        float y = 0.03F;
        float[][] p = {{x0 - px, z0 - pz}, {x1 - px, z1 - pz}, {x1 + px, z1 + pz}, {x0 + px, z0 + pz}};
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < 4; k++) {
                float[] v = p[side == 0 ? k : 3 - k];
                consumer.addVertex(pose, v[0], y, v[1]).setColor(colour[0], colour[1], colour[2], alpha);
            }
        }
    }

    /** The hole of void in the middle: a rough disc on the ground. */
    private static void hole(VertexConsumer voidFill, PoseStack.Pose pose, float radius) {
        int sides = 9;
        float y = 0.025F;
        for (int i = 0; i < sides; i++) {
            float a0 = i * Mth.TWO_PI / sides;
            float a1 = (i + 1) * Mth.TWO_PI / sides;
            float r0 = radius * (0.8F + 0.2F * Mth.sin(i * 2.3F));
            float r1 = radius * (0.8F + 0.2F * Mth.sin((i + 1) * 2.3F));
            float[][] p = {{0.0F, 0.0F}, {Mth.cos(a0) * r0, Mth.sin(a0) * r0}, {Mth.cos(a1) * r1, Mth.sin(a1) * r1},
                    {0.0F, 0.0F}};
            for (int side = 0; side < 2; side++) {
                for (int k = 0; k < 4; k++) {
                    float[] v = p[side == 0 ? k : 3 - k];
                    voidFill.addVertex(pose, v[0], y, v[1]);
                }
            }
        }
    }
}
