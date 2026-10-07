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
 * A flake of falling ash ({@code corruptedadventure:ash_flake}, ring 3 weather): falls slowly at the speed it was given,
 * sways a little, grey-tinted, and is gone once it lands.
 */
public final class AshFlakeParticle extends TextureSheetParticle {
    private final float swayPhase;

    private AshFlakeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = true;
        this.lifetime = 60 + random.nextInt(61);
        this.quadSize *= 0.35F + random.nextFloat() * 0.35F;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        float shade = 0.7F + random.nextFloat() * 0.35F;
        setColor(0x8A / 255.0F * shade, 0x85 / 255.0F * shade, 0x80 / 255.0F * shade);
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround) {
            remove();
            return;
        }
        float sway = Mth.sin(age * 0.15F + swayPhase) * 0.01F;
        xd += sway;
        zd -= sway;
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
            AshFlakeParticle particle = new AshFlakeParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
