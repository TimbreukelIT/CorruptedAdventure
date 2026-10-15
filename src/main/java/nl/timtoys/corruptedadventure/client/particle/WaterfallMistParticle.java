package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Fine spray hanging in the air of {@code verdant_falls} ({@code corruptedadventure:waterfall_mist}, its biome particle):
 * a tiny, faint droplet cloud that drifts sideways, sinks a little, swells slightly and fades. Light sprites
 * ({@code waterfall_mist_0..3}, stream T) tinted white with a light blue hint. A biome particle comes without speed, so the drift is its own.
 */
public final class WaterfallMistParticle extends TextureSheetParticle {
    /** White with a light blue hint (V-T4 A: the sprites are untinted light grey/white). */
    private static final int MIST = 0xEAF4FF;
    /** Sideways drift per tick, and the slow sink. */
    private static final double DRIFT = 0.006;
    private static final double SINK = 0.002;

    private final float startSize;

    private WaterfallMistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.yd = yd - SINK;
        this.zd = zd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.lifetime = 50 + random.nextInt(51);
        this.quadSize *= 0.35F + random.nextFloat() * 0.35F;
        this.startSize = quadSize;
        float shade = 0.92F + random.nextFloat() * 0.08F;
        setColor((MIST >> 16 & 0xFF) / 255.0F * shade, (MIST >> 8 & 0xFF) / 255.0F * shade, (MIST & 0xFF) / 255.0F * shade);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        quadSize = startSize * (1.0F + life * 0.6F);
        // Faint: fades in, hangs, thins out; never more than half opaque.
        alpha = Mth.clamp(Math.min(life * 4.0F, (1.0F - life) * 2.0F), 0.0F, 0.45F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            WaterfallMistParticle particle = new WaterfallMistParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
