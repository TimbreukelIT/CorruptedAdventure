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
 * A small hovering light of ring 4 (package 80-0): the will-o'-the-wisp of the blighted band ({@code will_o_wisp}, a soft
 * slow pulse) and the firefly of the mythical swamp ({@code firefly}, blinks on and off). Wanders on its own, since biome
 * particles come without speed; full bright, so it glows at night. Light grey sprites, tinted by the {@link Glow}.
 */
public final class GlowMoteParticle extends TextureSheetParticle {
    /** Pale, cold green-cyan: a ghost light over the peat. */
    public static final Glow WILL_O_WISP = new Glow(0xA8F0D8, 0.9F, false);
    /** Warm yellow-green, small. */
    public static final Glow FIREFLY = new Glow(0xE6F070, 0.45F, true);

    /** Random push per tick; friction caps the wandering speed. */
    private static final double WANDER = 0.004;

    private final Glow glow;
    private final float phase;

    private GlowMoteParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, Glow glow) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.glow = glow;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.92F;
        this.hasPhysics = true;
        this.lifetime = 80 + random.nextInt(81);
        this.quadSize *= glow.size() * (0.8F + random.nextFloat() * 0.4F);
        this.phase = random.nextFloat() * Mth.TWO_PI;
        setColor((glow.tint() >> 16 & 0xFF) / 255.0F, (glow.tint() >> 8 & 0xFF) / 255.0F, (glow.tint() & 0xFF) / 255.0F);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        xd += (random.nextDouble() - 0.5) * 2 * WANDER;
        yd += (random.nextDouble() - 0.5) * WANDER;
        zd += (random.nextDouble() - 0.5) * 2 * WANDER;
        float life = age / (float) lifetime;
        float fade = Mth.clamp(Math.min(life * 8.0F, (1.0F - life) * 4.0F), 0.0F, 1.0F);
        float wave = Mth.sin(age * (glow.blinks() ? 0.15F : 0.08F) + phase);
        // A firefly is dark most of the time and flashes; a wisp only breathes.
        float light = glow.blinks() ? Mth.clamp(wave * 3.0F - 1.5F, 0.0F, 1.0F) : 0.6F + 0.4F * wave;
        alpha = fade * light;
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
     * How a mote looks.
     *
     * @param tint   RGB the light grey sprite is multiplied with
     * @param size   factor on vanilla's random particle size
     * @param blinks flashes on and off (firefly) instead of a slow pulse (wisp)
     */
    public record Glow(int tint, float size, boolean blinks) {
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Glow glow;

        public Provider(SpriteSet sprites, Glow glow) {
            this.sprites = sprites;
            this.glow = glow;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            GlowMoteParticle particle = new GlowMoteParticle(level, x, y, z, xd, yd, zd, glow);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
