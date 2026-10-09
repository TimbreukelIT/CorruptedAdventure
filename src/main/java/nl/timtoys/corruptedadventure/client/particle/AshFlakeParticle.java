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
 * sways a little, grey-tinted, and is gone once it lands. As a biome's ambient particle ({@code drifting_ash},
 * package 50; those come without speed) it drifts down slowly by itself.
 */
public final class AshFlakeParticle extends TextureSheetParticle {
    private final float swayPhase;

    private AshFlakeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, boolean drifting) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = drifting ? xd + (random.nextDouble() - 0.5) * 0.01 : xd;
        this.yd = drifting ? yd - 0.015 - random.nextDouble() * 0.02 : yd;
        this.zd = drifting ? zd + (random.nextDouble() - 0.5) * 0.01 : zd;
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
        private final boolean drifting;

        /** @param drifting {@code drifting_ash}: falls by itself on top of the given speed */
        public Provider(SpriteSet sprites, boolean drifting) {
            this.sprites = sprites;
            this.drifting = drifting;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            AshFlakeParticle particle = new AshFlakeParticle(level, x, y, z, xd, yd, zd, drifting);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
