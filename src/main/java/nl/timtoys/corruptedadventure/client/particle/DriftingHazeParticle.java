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
 * A faint speck of haze that drifts on its own (package 80-0): the subtle yellow of swamp gas ({@code swamp_gas}, rises
 * very slowly, "attentive players can see it"), the bone dust of Nether ring 4 ({@code bone_dust}, sinks slowly) and,
 * since 98, the void mote of ring 5 ({@code void_mote}, rises out of voidstone and the void scars).
 * Light grey sprites, tinted by the {@link Haze}.
 */
public final class DriftingHazeParticle extends TextureSheetParticle {
    /** Subtle yellow, rising very slowly, faint. */
    public static final Haze SWAMP_GAS = new Haze(0xD8D070, 0.004, 0.35F, 0.7F);
    /** Bone white-beige, sinking slowly. */
    public static final Haze BONE_DUST = new Haze(0xE8DFC8, -0.004, 0.6F, 0.5F);
    /** Ring 5's void mote (98): the sprites carry their own black-violet (no tint); rises slowly, nearly opaque. */
    public static final Haze VOID_MOTE = new Haze(0xFFFFFF, 0.006, 0.9F, 0.45F);

    private static final double DRIFT = 0.006;

    private final Haze haze;

    private DriftingHazeParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, Haze haze) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.haze = haze;
        this.xd = xd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.yd = yd + haze.vertical();
        this.zd = zd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.gravity = 0.0F;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.lifetime = 60 + random.nextInt(61);
        this.quadSize *= haze.size() * (0.8F + random.nextFloat() * 0.4F);
        float shade = 0.9F + random.nextFloat() * 0.1F;
        int tint = haze.tint();
        setColor((tint >> 16 & 0xFF) / 255.0F * shade, (tint >> 8 & 0xFF) / 255.0F * shade, (tint & 0xFF) / 255.0F * shade);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        // Keeps its own vertical drift against the friction.
        yd += haze.vertical() * 0.02;
        if (onGround) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        alpha = haze.maxAlpha() * Mth.clamp(Math.min(life * 5.0F, (1.0F - life) * 2.0F), 0.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * How a haze speck moves and looks.
     *
     * @param tint     RGB the light grey sprite is multiplied with
     * @param vertical own vertical speed per tick (positive rises)
     * @param maxAlpha never more opaque than this
     * @param size     factor on vanilla's random particle size
     */
    public record Haze(int tint, double vertical, float maxAlpha, float size) {
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Haze haze;

        public Provider(SpriteSet sprites, Haze haze) {
            this.sprites = sprites;
            this.haze = haze;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            DriftingHazeParticle particle = new DriftingHazeParticle(level, x, y, z, xd, yd, zd, haze);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
