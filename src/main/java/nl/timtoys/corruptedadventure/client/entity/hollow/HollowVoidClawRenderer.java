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
import nl.timtoys.corruptedadventure.entity.hollow.HollowVoidClaw;

/**
 * Draws a {@link HollowVoidClaw}. First the warning: purple cracks that run out over the ground from a growing hole of
 * void, pulsing faster toward the bite. Then the claw: four hooked talons of the End portal's starry void with a glowing
 * sheen of corruption shoot up out of the hole, stand, and sink back. The shape is fixed per claw (seeded by its id);
 * the synced warmup and the bite event time it.
 */
public final class HollowVoidClawRenderer extends EntityRenderer<HollowVoidClaw> {
    private static final int CRACKS = 6;
    private static final int CRACK_STEPS = 4;
    private static final int TALONS = 4;
    private static final int TALON_STEPS = 6;
    private static final float TALON_HEIGHT = 2.1F;
    private static final float SINK_TICKS = 6.0F;
    private static final float SHEEN = 1.45F;
    // The corruption palette: 2B1F38 bed, 45305C deep, F800F8 glitch; the glowing core a little more saturated.
    private static final int[] RIM = {0xB0, 0x60, 0xF0};
    private static final int[] DEEP = {0x45, 0x30, 0x5C};
    private static final int[] BED = {0x2B, 0x1F, 0x38};
    private static final int[] GLITCH = {0xF8, 0x00, 0xF8};

    HollowVoidClawRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HollowVoidClaw claw, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        float sinceBite = claw.clientSinceBite(partialTick);
        boolean open = sinceBite >= 0.0F;
        float warm = open ? 1.0F : Math.min(1.0F, (claw.tickCount + partialTick) / claw.warmup());
        float rise = 0.0F;
        if (open) {
            float up = Math.min(1.0F, sinceBite / HollowVoidClaw.RISE_TICKS);
            float down = Mth.clamp((HollowVoidClaw.LIFE_TICKS - sinceBite) / SINK_TICKS, 0.0F, 1.0F);
            rise = Math.min(up * (2.0F - up), down);
        }
        float time = claw.tickCount + partialTick;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-claw.getYRot()));
        PoseStack.Pose pose = poseStack.last();
        RandomSource random = RandomSource.create(claw.getId() * 7349241L);
        float[][] cracks = cracks(random, warm);
        // A dark purple bed (blended), then a thin glowing core (additive): additive alone washes out to white on grass.
        drawCracks(buffers.getBuffer(RenderType.debugQuads()), pose, cracks, 2.4F, BED, (int) (150 + 80 * warm), time);
        float pulse = 0.5F + 0.5F * Mth.sin(time * (0.3F + 0.9F * warm));
        drawCracks(buffers.getBuffer(RenderType.lightning()), pose, cracks, 1.0F, RIM,
                (int) (90 + 150 * (open ? 1.0F : pulse * warm)), time);
        hole(buffers.getBuffer(RenderType.endPortal()), pose, 0.12F + 0.33F * warm);
        if (rise > 0.0F) {
            talons(buffers, pose, random, rise, time);
        }
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

    /**
     * Four hooked talons round the hole, curving out and then in over the middle, thinning to a point; {@code rise} 0..1
     * is how far they are out of the ground (below it they are hidden). Starry void, with a faint glowing sheen.
     */
    private static void talons(MultiBufferSource buffers, PoseStack.Pose pose, RandomSource random, float rise, float time) {
        float drop = (1.0F - rise) * TALON_HEIGHT;
        float[][][] rings = new float[TALONS][][];
        float[][] directions = new float[TALONS][];
        for (int k = 0; k < TALONS; k++) {
            float angle = (k + 0.5F) * Mth.TWO_PI / TALONS + (random.nextFloat() - 0.5F) * 0.6F;
            float height = TALON_HEIGHT * (0.8F + random.nextFloat() * 0.35F);
            float hook = 0.7F + random.nextFloat() * 0.25F;
            float cos = Mth.cos(angle);
            float sin = Mth.sin(angle);
            directions[k] = new float[] {cos, sin};
            rings[k] = new float[TALON_STEPS + 1][];
            for (int step = 0; step <= TALON_STEPS; step++) {
                float t = (float) step / TALON_STEPS;
                float out = 0.34F + 0.55F * t - hook * t * t;
                float w = 0.21F * (1.0F - 0.92F * t);
                rings[k][step] = new float[] {cos * out, height * t - drop, sin * out, w};
            }
        }
        VertexConsumer voidFill = buffers.getBuffer(RenderType.endPortal());
        for (int k = 0; k < TALONS; k++) {
            for (int step = 0; step < TALON_STEPS; step++) {
                for (float[][] face : faces(rings[k][step], rings[k][step + 1], directions[k], 1.0F)) {
                    for (int side = 0; side < 2; side++) {
                        for (int v = 0; v < 4; v++) {
                            float[] p = face[side == 0 ? v : 3 - v];
                            voidFill.addVertex(pose, p[0], p[1], p[2]);
                        }
                    }
                }
            }
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        for (int k = 0; k < TALONS; k++) {
            for (int step = 0; step < TALON_STEPS; step++) {
                float t = (float) step / TALON_STEPS;
                int alpha = Math.max(0, (int) ((70 + 50 * Mth.sin(time * 0.8F + k)) * (1.0F - t * 0.6F) * rise));
                int[] colour = (k * 3 + step + (int) (time * 0.5F)) % 13 == 0 ? GLITCH : step == TALON_STEPS - 1 ? RIM : DEEP;
                for (float[][] face : faces(rings[k][step], rings[k][step + 1], directions[k], SHEEN)) {
                    for (int side = 0; side < 2; side++) {
                        for (int v = 0; v < 4; v++) {
                            int index = side == 0 ? v : 3 - v;
                            float[] p = face[index];
                            // Bright at the segment's foot, fading toward its top.
                            int a = index < 2 ? alpha : alpha / 2;
                            glow.addVertex(pose, p[0], p[1], p[2]).setColor(colour[0], colour[1], colour[2], a);
                        }
                    }
                }
            }
        }
    }

    /**
     * The four side faces of one talon segment between two cross-sections {@code {x, y, z, half width}}: squares turned
     * to the talon's direction {@code {cos, sin}}, widened by {@code grow}. Each face is foot, foot, top, top.
     */
    private static float[][][] faces(float[] a, float[] b, float[] direction, float grow) {
        float[][] ca = corners(a, direction, grow);
        float[][] cb = corners(b, direction, grow);
        float[][][] faces = new float[4][][];
        for (int f = 0; f < 4; f++) {
            int g = (f + 1) % 4;
            faces[f] = new float[][] {ca[f], ca[g], cb[g], cb[f]};
        }
        return faces;
    }

    private static float[][] corners(float[] c, float[] direction, float grow) {
        float w = c[3] * grow;
        // Across the talon (tangent) and along it (radial).
        float tx = -direction[1] * w;
        float tz = direction[0] * w;
        float rx = direction[0] * w;
        float rz = direction[1] * w;
        return new float[][] {
                {c[0] - tx - rx, c[1], c[2] - tz - rz},
                {c[0] + tx - rx, c[1], c[2] + tz - rz},
                {c[0] + tx + rx, c[1], c[2] + tz + rz},
                {c[0] - tx + rx, c[1], c[2] - tz + rz}};
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(HollowVoidClaw claw) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
