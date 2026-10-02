package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A grain of blown sand ({@code corruptedadventure:sand_gust}): almost horizontal, short-lived, sand-tinted, without
 * gravity. It collides with blocks and disappears once a wall stops it, so no sand blows through houses.
 */
public final class SandGustParticle extends TextureSheetParticle {
    /** Below this horizontal speed (squared) a wall has stopped the grain. */
    private static final double STOPPED = 1.0E-4;

    private SandGustParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.99F;
        this.hasPhysics = true;
        this.lifetime = 20 + random.nextInt(21);
        this.quadSize *= 0.4F + random.nextFloat() * 0.4F;
        float shade = 0.85F + random.nextFloat() * 0.15F;
        setColor(0xC9 / 255.0F * shade, 0xA4 / 255.0F * shade, 0x6B / 255.0F * shade);
    }

    @Override
    public void tick() {
        super.tick();
        if (xd * xd + zd * zd < STOPPED) {
            remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
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
            SandGustParticle particle = new SandGustParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
