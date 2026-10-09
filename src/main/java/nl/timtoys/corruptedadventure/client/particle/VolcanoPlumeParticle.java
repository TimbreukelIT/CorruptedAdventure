package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * One billow of a volcano's smoke plume (package 50b): a huge, slow puff that rises from the crater, swells, drifts with
 * the wind and fades high above. Not a registered particle type: {@code VolcanoPlumeRenderer} creates it directly with
 * a {@code steam_puff} sprite, so no server or registry knows it. Dark ash grey, lighter as it thins; during an eruption
 * the lowest part glows red.
 */
public final class VolcanoPlumeParticle extends TextureSheetParticle {
    private static final int FULL_BRIGHT = 0xF000F0;
    /**
     * Full sky light, no block light: lit like the open sky (dark at night). The plume stands high above the terrain,
     * often above the build height or over unloaded chunks, where the level's own light lookup gives black.
     */
    private static final int SKY_ONLY = 0xF00000;
    /** Share of the life in which an eruption's glow fades from the billow. */
    private static final float GLOW_PART = 0.18F;
    /**
     * The rise slows by this per tick, so a billow climbs about {@code rise / 0.004} blocks in total and the column
     * spreads at the top. Vanilla's friction would brake the rise as hard as the drift: a cloud bank, not a column.
     */
    private static final double RISE_DECAY = 0.996;
    /** Horizontal damping per tick: the start drift dies out, the wind (stronger higher up) takes over. */
    private static final double DRIFT_DECAY = 0.985;
    /** The wind's push per tick at the top of a billow's life; its drift never exceeds {@link #MAX_DRIFT}. */
    private static final double WIND = 0.0006;
    private static final double MAX_DRIFT = 0.05;

    private final float startSize;
    private final float glow;

    /**
     * @param size  start size in blocks (it swells to about three times that)
     * @param rise  start speed upwards, blocks per tick
     * @param glow  0–1: how strongly the foot glows (an eruption)
     */
    public VolcanoPlumeParticle(ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite, float size,
                                double rise, double driftX, double driftZ, float glow) {
        super(level, x, y, z);
        setSprite(sprite);
        this.xd = driftX;
        this.yd = rise;
        this.zd = driftZ;
        this.gravity = 0.0F;
        // Own damping per axis in tick().
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 320 + random.nextInt(160);
        this.quadSize = size * (0.8F + random.nextFloat() * 0.4F);
        this.startSize = quadSize;
        this.glow = glow;
        this.alpha = 0.0F;
        colourAt(0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        quadSize = startSize * (1.0F + 2.0F * Mth.sqrt(life));
        yd *= RISE_DECAY;
        // The wind takes the plume higher up.
        xd = Mth.clamp(xd * DRIFT_DECAY + WIND * life, -MAX_DRIFT, MAX_DRIFT);
        zd = Mth.clamp(zd * DRIFT_DECAY + WIND * 0.5 * life, -MAX_DRIFT, MAX_DRIFT);
        alpha = Mth.clamp(Math.min(life * 12.0F, (1.0F - life) * 2.5F), 0.0F, 0.85F);
        colourAt(life);
    }

    /**
     * Ages a new billow by {@code part} of its life at once (0 ≤ part < 1), so a plume that comes into view stands as a
     * full column instead of growing from its foot.
     */
    public void prewarm(float part) {
        int ticks = Math.min((int) (lifetime * part), lifetime - 1);
        for (int i = 0; i < ticks; i++) {
            tick();
        }
    }

    /** Dark ash grey at the crater (#3A3634) to a lighter grey (#6E6864) high up; red at the foot during an eruption. */
    private void colourAt(float life) {
        float red = Mth.lerp(life, 0x3A / 255.0F, 0x6E / 255.0F);
        float green = Mth.lerp(life, 0x36 / 255.0F, 0x68 / 255.0F);
        float blue = Mth.lerp(life, 0x34 / 255.0F, 0x64 / 255.0F);
        float heat = glowAt(life);
        setColor(Mth.lerp(heat, red, 0xC0 / 255.0F), Mth.lerp(heat, green, 0x48 / 255.0F), Mth.lerp(heat, blue, 0x20 / 255.0F));
    }

    private float glowAt(float life) {
        return glow <= 0.0F || life >= GLOW_PART ? 0.0F : glow * (1.0F - life / GLOW_PART);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return glowAt(age / (float) lifetime) > 0.2F ? FULL_BRIGHT : SKY_ONLY;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        // The quad is far larger than the particle's own box: without this the frustum drops it at the screen edge.
        return super.getRenderBoundingBox(partialTicks).inflate(quadSize);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
