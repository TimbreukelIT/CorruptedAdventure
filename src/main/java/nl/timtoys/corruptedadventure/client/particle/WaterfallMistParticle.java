package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Fine spray hanging in the air of {@code verdant_falls} ({@code corruptedadventure:waterfall_mist}, its biome particle):
 * a tiny, faint droplet cloud that starts small, drifts sideways, rises slowly, swells a little and fades. Soft sprites
 * ({@code waterfall_mist_0..3}: round blobs with an alpha fall-off, white with a light blue rim) on the translucent sheet,
 * so the edge blends instead of showing pixels. A biome particle comes without speed, so the drift is its own.
 */
public final class WaterfallMistParticle extends TextureSheetParticle {
    /** Near white: the sprites carry the light blue themselves (polish round "mist"). */
    private static final int MIST = 0xF2F8FF;
    /** Sideways drift per tick, and the start of the slow rise. */
    private static final double DRIFT = 0.006;
    private static final double RISE = 0.002;
    /** Negative gravity: a steady lift of 0.00016 a tick, which friction 0.98 caps at ~0.008 a tick (0.16 blocks/s). */
    private static final float LIFT = -0.004F;
    /** Never more than this opaque; the sprite's own fall-off thins the rim further. */
    private static final float MAX_ALPHA = 0.6F;

    private final float startSize;

    private WaterfallMistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.yd = yd + RISE;
        this.zd = zd + (random.nextDouble() - 0.5) * 2 * DRIFT;
        this.gravity = LIFT;
        this.friction = 0.98F;
        this.hasPhysics = true;
        this.lifetime = 40 + random.nextInt(41);
        // Small: vanilla's 0.1..0.2 half-size times 0.2..0.4, so 0.04..0.16 of a block across at the start.
        this.quadSize *= 0.2F + random.nextFloat() * 0.2F;
        this.startSize = quadSize;
        float shade = 0.92F + random.nextFloat() * 0.08F;
        setColor((MIST >> 16 & 0xFF) / 255.0F * shade, (MIST >> 8 & 0xFF) / 255.0F * shade, (MIST & 0xFF) / 255.0F * shade);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (onGround || inWater(level, x, y, z)) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        quadSize = startSize * (1.0F + life * 0.4F);
        // Faint: fades in quickly, then thins out over the larger part of its life.
        alpha = MAX_ALPHA * Mth.clamp(Math.min(life * 6.0F, (1.0F - life) * 1.6F), 0.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Biome particles are also tried inside water, and the biome is mostly lakes: spray belongs above them. */
    private static boolean inWater(ClientLevel level, double x, double y, double z) {
        return !level.getFluidState(BlockPos.containing(x, y, z)).isEmpty();
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
            if (inWater(level, x, y, z)) {
                return null;
            }
            WaterfallMistParticle particle = new WaterfallMistParticle(level, x, y, z, xd, yd, zd);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
