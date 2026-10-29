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
 * The trace of The Unfinished (phase 4 step 2): a raindrop that hangs in mid-air where he passed, as in a standstill. It
 * never moves; it fades in, hangs, and fades out. Its sprites carry their own colour (no tint).
 */
public final class StillRaindropParticle extends TextureSheetParticle {
    private static final int MIN_LIFE = 50;
    private static final int EXTRA_LIFE = 30;
    private static final int FADE = 10;

    private StillRaindropParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = MIN_LIFE + random.nextInt(EXTRA_LIFE + 1);
        this.quadSize *= 1.0F + random.nextFloat() * 0.4F;
        this.alpha = 0.0F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            alpha = Mth.clamp(Math.min(age, lifetime - age) / (float) FADE, 0.0F, 0.85F);
        }
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
            return new StillRaindropParticle(level, x, y, z, sprites);
        }
    }
}
