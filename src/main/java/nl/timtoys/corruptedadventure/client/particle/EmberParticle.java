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
 * A glowing spark ({@code corruptedadventure:ember}, eruption and ash cloud): full bright, from yellow-orange to dark
 * red while it shrinks and fades, flickering a little. No gravity of its own: it drifts at the speed it was given.
 */
public final class EmberParticle extends TextureSheetParticle {
    /** Full block and sky light: glows in the dark. */
    private static final int FULL_BRIGHT = 0xF000F0;

    private final float startSize;

    private EmberParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.lifetime = 30 + random.nextInt(41);
        this.quadSize *= 0.25F + random.nextFloat() * 0.3F;
        this.startSize = quadSize;
        colourAt(0.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        colourAt(life);
        quadSize = startSize * (1.0F - life * 0.6F);
        alpha = Mth.clamp((1.0F - life) * 1.5F, 0.0F, 1.0F) * (0.8F + random.nextFloat() * 0.2F);
    }

    /** Yellow-orange (#FFB040) at birth to dark red (#B02010) at the end. */
    private void colourAt(float life) {
        setColor(Mth.lerp(life, 1.0F, 0xB0 / 255.0F), Mth.lerp(life, 0xB0 / 255.0F, 0x20 / 255.0F),
                Mth.lerp(life, 0x40 / 255.0F, 0x10 / 255.0F));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return FULL_BRIGHT;
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
            EmberParticle particle = new EmberParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
