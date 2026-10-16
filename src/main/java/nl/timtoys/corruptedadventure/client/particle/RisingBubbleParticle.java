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
 * A rising bubble of ring 4 (package 80-0): the slow, toxic green bubble of a toxic pool ({@code toxic_bubble}) and the
 * fast air bubble of an updraft ({@code updraft_bubble}). A bubble that starts inside a fluid pops when it leaves it (so
 * it never rises out of a pool); one that starts in the air (an updraft column) rises until its life ends or it bumps a
 * ceiling. Light grey sprites, tinted by the {@link Bubble}.
 */
public final class RisingBubbleParticle extends TextureSheetParticle {
    /** Toxic green, slow and short-lived. */
    public static final Bubble TOXIC = new Bubble(0x9BE03A, 0.012, 20, 20);
    /** Near white with a blue tinge, rushing upward. */
    public static final Bubble UPDRAFT = new Bubble(0xE6F4FF, 0.06, 20, 30);

    private final Bubble bubble;
    private final boolean startedInFluid;

    private RisingBubbleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, Bubble bubble) {
        // The four-argument constructor: the seven-argument one adds vanilla's random spread to the speed.
        super(level, x, y, z);
        this.bubble = bubble;
        this.xd = xd + (random.nextDouble() - 0.5) * 0.01;
        this.yd = yd + bubble.rise();
        this.zd = zd + (random.nextDouble() - 0.5) * 0.01;
        this.gravity = 0.0F;
        this.friction = 0.9F;
        this.hasPhysics = true;
        this.lifetime = bubble.minLife() + random.nextInt(bubble.extraLife() + 1);
        this.quadSize *= 0.4F + random.nextFloat() * 0.4F;
        this.startedInFluid = inFluid();
        float shade = 0.9F + random.nextFloat() * 0.1F;
        int tint = bubble.tint();
        setColor((tint >> 16 & 0xFF) / 255.0F * shade, (tint >> 8 & 0xFF) / 255.0F * shade, (tint & 0xFF) / 255.0F * shade);
    }

    @Override
    public void tick() {
        // Keeps rising against the friction, with a slight wobble.
        yd += bubble.rise() * 0.1;
        xd += (random.nextDouble() - 0.5) * 0.004;
        zd += (random.nextDouble() - 0.5) * 0.004;
        double before = y;
        super.tick();
        if (removed) {
            return;
        }
        // Vanilla's move never clears yd on a collision, so a ceiling shows as a bubble that stopped rising.
        boolean blocked = y <= before;
        if ((startedInFluid && !inFluid()) || (age > 2 && blocked)) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        alpha = Mth.clamp((1.0F - life) * 4.0F, 0.0F, 0.9F);
    }

    private boolean inFluid() {
        return !level.getFluidState(BlockPos.containing(x, y, z)).isEmpty();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /**
     * How a bubble moves and looks.
     *
     * @param tint      RGB the light grey sprite is multiplied with
     * @param rise      upward speed per tick at the start (blocks)
     * @param minLife   shortest life in ticks
     * @param extraLife up to this many ticks more
     */
    public record Bubble(int tint, double rise, int minLife, int extraLife) {
    }

    /** Registered in {@code CorruptedAdventureClient} through {@code RegisterParticleProvidersEvent}. */
    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Bubble bubble;

        public Provider(SpriteSet sprites, Bubble bubble) {
            this.sprites = sprites;
            this.bubble = bubble;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            RisingBubbleParticle particle = new RisingBubbleParticle(level, x, y, z, xd, yd, zd, bubble);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
