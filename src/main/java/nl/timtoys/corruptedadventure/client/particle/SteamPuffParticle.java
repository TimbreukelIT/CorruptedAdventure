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
 * A puff of steam ({@code corruptedadventure:steam_puff}: fumaroles, sulfur fields, steam fens): rises slowly on top of
 * the speed it was given (biome particles get none), swells and fades. Light grey sprites, tinted by the type:
 * near-white ({@code steam_puff}) or the yellow-green steam of the sulfur fields ({@code sulfur_steam}, package 50).
 */
public final class SteamPuffParticle extends TextureSheetParticle {
    /** Own upward drift, so a puff spawned without speed still rises. */
    private static final double RISE = 0.015;
    /** Near-white steam, and the yellow-green steam of the sulfur fields. */
    public static final int STEAM = 0xE8E8E4;
    public static final int SULFUR_STEAM = 0xDDE0B0;

    private final float startSize;

    private SteamPuffParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, int tint) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd + RISE;
        this.zd = zd;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        // Collides, so steam never rises through a ceiling or a roof.
        this.hasPhysics = true;
        this.lifetime = 40 + random.nextInt(41);
        this.quadSize *= 0.8F + random.nextFloat() * 0.6F;
        this.startSize = quadSize;
        float shade = 0.88F + random.nextFloat() * 0.12F;
        setColor((tint >> 16 & 0xFF) / 255.0F * shade, (tint >> 8 & 0xFF) / 255.0F * shade, (tint & 0xFF) / 255.0F * shade);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        yd += RISE * 0.05;
        float life = age / (float) lifetime;
        quadSize = startSize * (1.0F + life * 1.5F);
        // Fades in quickly, then thins out.
        alpha = Mth.clamp(Math.min(life * 6.0F, (1.0F - life) * 1.2F), 0.0F, 0.7F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final int tint;

        /** @param tint {@link #STEAM} or {@link #SULFUR_STEAM} */
        public Provider(SpriteSet sprites, int tint) {
            this.sprites = sprites;
            this.tint = tint;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            SteamPuffParticle particle = new SteamPuffParticle(level, x, y, z, xd, yd, zd, tint);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
