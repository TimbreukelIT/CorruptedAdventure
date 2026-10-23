package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A speck of a broken world in zone 4→5 ({@code corruptedadventure:glitch_speck}, package 80-0): hangs still, then jumps
 * a little, switches sprite and flickers. The sprites carry their own magenta, cyan and black, so no tint; full bright,
 * since a glitch ignores the light. Since package 98 also the ring 5 {@code glitch_pixel}: the same behaviour, wilder
 * ({@link #PIXEL}).
 */
public final class GlitchSpeckParticle extends TextureSheetParticle {
    /** The zone 4→5 speck: small jumps now and then. */
    public static final Jitter SPECK = new Jitter(0.15, 6);
    /** The ring 5 pixel (98): twice as far, twice as often. */
    public static final Jitter PIXEL = new Jitter(0.3, 3);

    private final SpriteSet sprites;
    private final Jitter jitter;

    private GlitchSpeckParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, Jitter jitter) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.jitter = jitter;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 20 + random.nextInt(41);
        this.quadSize *= 0.5F + random.nextFloat() * 0.5F;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        if (random.nextInt(jitter.oneIn()) == 0) {
            double jump = jitter.jump();
            setPos(x + (random.nextDouble() - 0.5) * 2 * jump, y + (random.nextDouble() - 0.5) * 2 * jump,
                    z + (random.nextDouble() - 0.5) * 2 * jump);
            // A jump, not a slide: the render must not interpolate from the old position.
            xo = x;
            yo = y;
            zo = z;
            pickSprite(sprites);
        }
        alpha = random.nextInt(4) == 0 ? 0.0F : 1.0F;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * How a glitch jumps.
     *
     * @param jump  furthest a jump goes on each axis (blocks)
     * @param oneIn a jump on about one tick in this many
     */
    public record Jitter(double jump, int oneIn) {
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Jitter jitter;

        public Provider(SpriteSet sprites, Jitter jitter) {
            this.sprites = sprites;
            this.jitter = jitter;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new GlitchSpeckParticle(level, x, y, z, sprites, jitter);
        }
    }
}
