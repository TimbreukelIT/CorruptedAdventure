package nl.timtoys.corruptedadventure.client.entity.risen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.risen.RisenState;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;

/**
 * The shadow The Risen casts as it passes over at its arrival ({@link RisenState#PASSING}): its wing silhouette
 * ({@code the_risen_shadow.png}) laid on the top of every block under it, turned with its body, like vanilla's round
 * entity shadow but reaching far down and shaped like the bird. Darkest when it flies low, gone above
 * {@link #FADE_TO} blocks. Client only.
 */
final class RisenShadow {
    private static final ResourceLocation TEXTURE = CorruptedAdventure.id("textures/entity/the_risen_shadow.png");
    /** Half its span (u) and half its length (v) on the ground: ~24 × 12 blocks, the size of the bird. */
    private static final double HALF_SPAN = 12.0;
    private static final double HALF_LENGTH = 6.0;
    private static final float MAX_ALPHA = 0.85F;
    private static final double FADE_FROM = 28.0;
    private static final double FADE_TO = 64.0;
    private static final float LIFT = 0.015F;
    /** A block without collision up to this high is a layer on the ground; higher it is a plant. */
    private static final double LAYER_MAX = 0.5;
    private static final int SURFACE_STEPS = 4;

    private RisenShadow() {
    }

    static void render(TheRisen risen, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        if (risen.state() != RisenState.PASSING || risen.isInvisible()) {
            return;
        }
        Level level = risen.level();
        double x = Mth.lerp(partialTick, risen.xOld, risen.getX());
        double y = Mth.lerp(partialTick, risen.yOld, risen.getY());
        double z = Mth.lerp(partialTick, risen.zOld, risen.getZ());
        double height = y - level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z));
        float alpha = MAX_ALPHA * (float) Mth.clamp((FADE_TO - height) / (FADE_TO - FADE_FROM), 0.0, 1.0);
        if (alpha <= 0.01F) {
            return;
        }
        float yaw = Mth.rotLerp(partialTick, risen.yBodyRotO, risen.yBodyRot) * Mth.DEG_TO_RAD;
        // Forward is its look (-sin, cos); across is the line of its wings.
        Frame frame = new Frame(x, z, -Mth.sin(yaw), Mth.cos(yaw), Mth.cos(yaw), Mth.sin(yaw));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityShadow(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        int reach = Mth.ceil(Math.hypot(HALF_SPAN, HALF_LENGTH));
        int midX = Mth.floor(x);
        int midZ = Mth.floor(z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int bx = midX - reach; bx <= midX + reach; bx++) {
            for (int bz = midZ - reach; bz <= midZ + reach; bz++) {
                if (!frame.touches(bx, bz)) {
                    continue;
                }
                double top = surface(level, pos.set(bx, level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz) - 1, bz));
                if (Double.isNaN(top)) {
                    continue;
                }
                float rel = (float) (top - y) + LIFT;
                vertex(consumer, pose, frame, bx, rel, bz, alpha);
                vertex(consumer, pose, frame, bx, rel, bz + 1, alpha);
                vertex(consumer, pose, frame, bx + 1, rel, bz + 1, alpha);
                vertex(consumer, pose, frame, bx + 1, rel, bz, alpha);
            }
        }
    }

    /**
     * The y the shadow lies on in the column of {@code pos} (the highest non-air block): a thin layer (the ash drifts of
     * ring 3, snow) counts with its own top, plants are passed through to the ground; NaN if nothing is found.
     */
    private static double surface(Level level, BlockPos.MutableBlockPos pos) {
        for (int i = 0; i < SURFACE_STEPS; i++, pos.move(Direction.DOWN)) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                return pos.getY() + fluid.getHeight(level, pos);
            }
            VoxelShape shape = state.getShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            double height = shape.max(Direction.Axis.Y);
            if (state.getCollisionShape(level, pos).isEmpty() && height > LAYER_MAX) {
                continue;
            }
            return pos.getY() + height;
        }
        return Double.NaN;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Frame frame, int wx, float rel, int wz,
                               float alpha) {
        consumer.addVertex(pose, (float) (wx - frame.x), rel, (float) (wz - frame.z))
                .setColor(1.0F, 1.0F, 1.0F, alpha)
                .setUv(frame.u(wx, wz), frame.v(wx, wz))
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** The texture laid out under it: u along its wings, v from its head (0) to its tail (1). */
    private record Frame(double x, double z, double forwardX, double forwardZ, double acrossX, double acrossZ) {
        float u(double wx, double wz) {
            return (float) (0.5 + ((wx - x) * acrossX + (wz - z) * acrossZ) / (2.0 * HALF_SPAN));
        }

        float v(double wx, double wz) {
            return (float) (0.5 - ((wx - x) * forwardX + (wz - z) * forwardZ) / (2.0 * HALF_LENGTH));
        }

        /** Whether block column (bx, bz) overlaps the texture at all. */
        boolean touches(int bx, int bz) {
            float minU = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, minV = Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
            for (int i = 0; i < 4; i++) {
                int cx = bx + (i & 1);
                int cz = bz + (i >> 1);
                float u = u(cx, cz);
                float v = v(cx, cz);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
            return maxU > 0.0F && minU < 1.0F && maxV > 0.0F && minV < 1.0F;
        }
    }
}
