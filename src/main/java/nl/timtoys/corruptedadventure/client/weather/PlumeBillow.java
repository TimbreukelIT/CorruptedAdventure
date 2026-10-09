package nl.timtoys.corruptedadventure.client.weather;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * One billow of a volcano's smoke plume (packages 50b/50c): a huge, slow puff that rises from the crater, swells,
 * drifts with the wind and fades high above. Not a particle: {@link VolcanoPlumeRenderer} ticks and draws the billows
 * itself, without terrain fog, so a plume stays visible beyond the render distance. Dark ash grey, lighter as it thins;
 * during an eruption the lowest part glows red.
 */
final class PlumeBillow {
    /** Share of the life in which an eruption's glow fades from the billow. */
    private static final float GLOW_PART = 0.18F;
    /**
     * The rise slows by this per tick, so a billow climbs about {@code rise / 0.004} blocks in total and the column
     * spreads at the top.
     */
    private static final double RISE_DECAY = 0.996;
    /** Horizontal damping per tick: the start drift dies out, the wind (stronger higher up) takes over. */
    private static final double DRIFT_DECAY = 0.985;
    /** The wind's push per tick at the top of a billow's life; its drift never exceeds {@link #MAX_DRIFT}. */
    private static final double WIND = 0.0006;
    static final double MAX_DRIFT = 0.05;
    private static final float MAX_ALPHA = 0.85F;

    private final TextureAtlasSprite sprite;
    private final int lifetime;
    private final float startSize;
    private final float glow;
    private double x;
    private double y;
    private double z;
    private double xo;
    private double yo;
    private double zo;
    private double xd;
    private double yd;
    private double zd;
    private int age;
    private float size;

    /**
     * @param size  start size in blocks, half the quad's width (it swells to about three times that)
     * @param rise  start speed upwards, blocks per tick
     * @param glow  0–1: how strongly the foot glows (an eruption)
     */
    PlumeBillow(RandomSource random, double x, double y, double z, TextureAtlasSprite sprite, float size, double rise,
                double driftX, double driftZ, float glow) {
        this.sprite = sprite;
        this.x = x;
        this.y = y;
        this.z = z;
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.xd = driftX;
        this.yd = rise;
        this.zd = driftZ;
        this.lifetime = 320 + random.nextInt(160);
        this.startSize = size * (0.8F + random.nextFloat() * 0.4F);
        this.size = startSize;
        this.glow = glow;
    }

    boolean alive() {
        return age < lifetime;
    }

    void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (++age >= lifetime) {
            return;
        }
        x += xd;
        y += yd;
        z += zd;
        float life = age / (float) lifetime;
        size = startSize * (1.0F + 2.0F * Mth.sqrt(life));
        yd *= RISE_DECAY;
        // The wind takes the plume higher up.
        xd = Mth.clamp(xd * DRIFT_DECAY + WIND * life, -MAX_DRIFT, MAX_DRIFT);
        zd = Mth.clamp(zd * DRIFT_DECAY + WIND * 0.5 * life, -MAX_DRIFT, MAX_DRIFT);
    }

    /**
     * Ages a new billow by {@code part} of its life at once (0 ≤ part < 1), so a plume that comes into view stands as a
     * full column instead of growing from its foot.
     */
    void prewarm(float part) {
        int ticks = Math.min((int) (lifetime * part), lifetime - 1);
        for (int i = 0; i < ticks; i++) {
            tick();
        }
    }

    double distanceSquared(Vec3 camera) {
        return Mth.lengthSquared(x - camera.x, y - camera.y, z - camera.z);
    }

    double horizontalDistance(Vec3 camera) {
        return Math.sqrt(Mth.lengthSquared(x - camera.x, z - camera.z));
    }

    double y() {
        return y;
    }

    double horizontalSpeed() {
        return Math.sqrt(xd * xd + zd * zd);
    }

    int age() {
        return age;
    }

    /** Fades in quickly, out over the last 40 % of its life, never fully opaque. */
    static float alphaAt(float life) {
        return Mth.clamp(Math.min(life * 12.0F, (1.0F - life) * 2.5F), 0.0F, MAX_ALPHA);
    }

    private float glowAt(float life) {
        return glow <= 0.0F || life >= GLOW_PART ? 0.0F : glow * (1.0F - life / GLOW_PART);
    }

    /**
     * One camera-facing quad. Beyond {@code reach} (the projection's far plane would cut it off) the billow is drawn
     * pulled in along the line of sight, at the same angle and apparent size. Lit by {@code daylight} (the glowing
     * foot by itself), and towards the fog colour by {@code haze}.
     */
    void render(VertexConsumer buffer, Vec3 camera, Quaternionf rotation, Vector3f scratch, float partialTick, double reach,
                float daylight, float haze, float[] fog) {
        float life = Math.min(1.0F, (age + partialTick) / lifetime);
        float alpha = alphaAt(life);
        if (alpha <= 0.0F) {
            return;
        }
        double px = Mth.lerp(partialTick, xo, x) - camera.x;
        double py = Mth.lerp(partialTick, yo, y) - camera.y;
        double pz = Mth.lerp(partialTick, zo, z) - camera.z;
        float quad = size;
        double distance = Math.sqrt(px * px + py * py + pz * pz);
        if (distance > reach) {
            double scale = reach / distance;
            px *= scale;
            py *= scale;
            pz *= scale;
            quad *= (float) scale;
        }
        // Dark ash grey at the crater (#3A3634) to a lighter grey (#6E6864) high up; red at the foot in an eruption.
        float heat = glowAt(life);
        float light = Mth.lerp(Math.min(1.0F, heat * 2.0F), daylight, 1.0F);
        float red = Mth.lerp(heat, Mth.lerp(life, 0x3A, 0x6E) / 255.0F, 0xC0 / 255.0F) * light;
        float green = Mth.lerp(heat, Mth.lerp(life, 0x36, 0x68) / 255.0F, 0x48 / 255.0F) * light;
        float blue = Mth.lerp(heat, Mth.lerp(life, 0x34, 0x64) / 255.0F, 0x20 / 255.0F) * light;
        red = Mth.lerp(haze, red, fog[0]);
        green = Mth.lerp(haze, green, fog[1]);
        blue = Mth.lerp(haze, blue, fog[2]);
        vertex(buffer, rotation, scratch, px, py, pz, quad, 1.0F, -1.0F, sprite.getU1(), sprite.getV1(), red, green, blue, alpha);
        vertex(buffer, rotation, scratch, px, py, pz, quad, 1.0F, 1.0F, sprite.getU1(), sprite.getV0(), red, green, blue, alpha);
        vertex(buffer, rotation, scratch, px, py, pz, quad, -1.0F, 1.0F, sprite.getU0(), sprite.getV0(), red, green, blue, alpha);
        vertex(buffer, rotation, scratch, px, py, pz, quad, -1.0F, -1.0F, sprite.getU0(), sprite.getV1(), red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, Quaternionf rotation, Vector3f scratch, double px, double py, double pz,
                               float quad, float dx, float dy, float u, float v, float red, float green, float blue, float alpha) {
        scratch.set(dx, dy, 0.0F).rotate(rotation).mul(quad).add((float) px, (float) py, (float) pz);
        buffer.addVertex(scratch.x(), scratch.y(), scratch.z()).setUv(u, v).setColor(red, green, blue, alpha);
    }
}
