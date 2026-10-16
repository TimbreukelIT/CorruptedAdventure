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
 * A petal drifting down from {@code moonpetal} blossom ({@code corruptedadventure:blossom}, package 80-0): falls slowly,
 * sways from side to side and turns. The sprites carry their own pink, so no tint; it lies still a moment on the ground
 * before it fades.
 */
public final class BlossomParticle extends TextureSheetParticle {
    private static final double FALL = 0.012;
    private static final double SWAY = 0.02;

    private final float swayPhase;
    private final float spin;

    private BlossomParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd - FALL;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = true;
        this.lifetime = 120 + random.nextInt(81);
        this.quadSize *= 0.5F + random.nextFloat() * 0.3F;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        this.spin = (random.nextFloat() - 0.5F) * 0.1F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
    }

    @Override
    public void tick() {
        float sway = Mth.sin(age * 0.1F + swayPhase);
        xd = sway * SWAY;
        zd = Mth.cos(age * 0.07F + swayPhase) * SWAY * 0.5;
        yd = -FALL;
        oRoll = roll;
        if (!onGround) {
            roll += spin;
        }
        super.tick();
        if (onGround) {
            xd = 0;
            zd = 0;
        }
        float life = age / (float) lifetime;
        alpha = Mth.clamp((1.0F - life) * 5.0F, 0.0F, 1.0F);
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
            BlossomParticle particle = new BlossomParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
