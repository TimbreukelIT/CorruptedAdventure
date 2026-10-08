package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * A drifting ice crystal ({@code corruptedadventure:ice_glint}, Nether ring 3): hangs in the air, sinks very slowly and
 * twinkles; pale blue-white and full bright, so it glints in the dark halls.
 */
public final class IceGlintParticle extends TextureSheetParticle {
    private final float twinklePhase;

    private IceGlintParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd + (random.nextDouble() - 0.5) * 0.004;
        this.yd = yd - 0.003;
        this.zd = zd + (random.nextDouble() - 0.5) * 0.004;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = true;
        this.lifetime = 60 + random.nextInt(61);
        this.quadSize *= 0.3F + random.nextFloat() * 0.3F;
        this.twinklePhase = random.nextFloat() * Mth.TWO_PI;
        setColor(0xD8 / 255.0F, 0xEC / 255.0F, 1.0F);
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
        float twinkle = 0.55F + 0.45F * Mth.sin(age * 0.4F + twinklePhase);
        alpha = Mth.clamp(Math.min(life * 5.0F, (1.0F - life) * 3.0F), 0.0F, 1.0F) * twinkle;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
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
            IceGlintParticle particle = new IceGlintParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
