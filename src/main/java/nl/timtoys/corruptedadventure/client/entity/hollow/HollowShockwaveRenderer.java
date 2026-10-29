package nl.timtoys.corruptedadventure.client.entity.hollow;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.entity.hollow.HollowShockwave;

/**
 * Draws a {@link HollowShockwave}: a glowing front of corruption running out over the ground, a low wall where the wave
 * is with a fading trail behind it (additive, so it reads at night too). It thins out as it nears its radius. The dust
 * comes from the entity's own client tick.
 */
public final class HollowShockwaveRenderer extends EntityRenderer<HollowShockwave> {
    private static final int SEGMENTS = 72;
    private static final float TRAIL = 1.4F;
    private static final float GROUND = 0.06F;
    // The corruption palette: 2B1F38 bed, 6A4A8C, F800F8 glitch; the glowing front a little more saturated.
    private static final int[] FRONT = {0xB0, 0x60, 0xF0};
    private static final int[] BED = {0x2B, 0x1F, 0x38};
    private static final int[] BACK = {0x6A, 0x4A, 0x8C};
    private static final int[] GLITCH = {0xF8, 0x00, 0xF8};

    HollowShockwaveRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public void render(HollowShockwave wave, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        float max = wave.maxRadius();
        float radius = wave.radiusAt(wave.tickCount + partialTick);
        if (radius <= 0.05F || radius >= max) {
            return;
        }
        float strength = 1.0F - radius / max;
        float wall = 0.25F + 0.65F * strength;
        int alpha = (int) (60 + 130 * strength);
        float time = wave.tickCount + partialTick;
        PoseStack.Pose pose = poseStack.last();
        float inner = Math.max(0.0F, radius - TRAIL);
        // A dark purple bed behind the front (blended): additive alone washes out to white on grass.
        VertexConsumer bed = buffers.getBuffer(RenderType.debugQuads());
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            quad(bed, pose,
                    new float[] {Mth.cos(a0) * inner, GROUND - 0.01F, Mth.sin(a0) * inner}, BED, 0,
                    new float[] {Mth.cos(a1) * inner, GROUND - 0.01F, Mth.sin(a1) * inner}, BED, 0,
                    new float[] {Mth.cos(a1) * radius, GROUND - 0.01F, Mth.sin(a1) * radius}, BED, (int) (200 * strength),
                    new float[] {Mth.cos(a0) * radius, GROUND - 0.01F, Mth.sin(a0) * radius}, BED, (int) (200 * strength));
        }
        VertexConsumer glow = buffers.getBuffer(RenderType.lightning());
        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = i * Mth.TWO_PI / SEGMENTS;
            float a1 = (i + 1) * Mth.TWO_PI / SEGMENTS;
            float c0 = Mth.cos(a0);
            float s0 = Mth.sin(a0);
            float c1 = Mth.cos(a1);
            float s1 = Mth.sin(a1);
            // A ragged front: each segment's wall a little higher or lower.
            float jag = 0.75F + 0.5F * Mth.abs(Mth.sin(i * 2.7F + time * 0.9F));
            int[] front = (i * 11 + (int) (time * 0.7F)) % 29 == 0 ? GLITCH : FRONT;
            // The trail on the ground, bright at the front, gone behind.
            quad(glow, pose,
                    new float[] {c0 * inner, GROUND, s0 * inner}, BACK, 0,
                    new float[] {c1 * inner, GROUND, s1 * inner}, BACK, 0,
                    new float[] {c1 * radius, GROUND, s1 * radius}, front, alpha,
                    new float[] {c0 * radius, GROUND, s0 * radius}, front, alpha);
            // The wall, bright at its foot.
            quad(glow, pose,
                    new float[] {c0 * radius, GROUND, s0 * radius}, front, alpha,
                    new float[] {c1 * radius, GROUND, s1 * radius}, front, alpha,
                    new float[] {c1 * radius, wall * jag, s1 * radius}, BACK, 0,
                    new float[] {c0 * radius, wall * jag, s0 * radius}, BACK, 0);
        }
        super.render(wave, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    /** A coloured quad, both windings. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float[] p0, int[] c0, int a0, float[] p1, int[] c1,
                             int a1, float[] p2, int[] c2, int a2, float[] p3, int[] c3, int a3) {
        float[][] p = {p0, p1, p2, p3};
        int[][] c = {c0, c1, c2, c3};
        int[] a = {a0, a1, a2, a3};
        for (int side = 0; side < 2; side++) {
            for (int k = 0; k < 4; k++) {
                int v = side == 0 ? k : 3 - k;
                consumer.addVertex(pose, p[v][0], p[v][1], p[v][2]).setColor(c[v][0], c[v][1], c[v][2], a[v]);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public ResourceLocation getTextureLocation(HollowShockwave wave) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
