package nl.timtoys.corruptedadventure.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * The enormous shape far down in deep water (Q18 = A, change 80): a dark billboard that fades in, glides slowly
 * sideways for a few seconds and fades out again, a herald of phase 4. Client-only and unregistered: the ambience adds
 * it straight to the particle engine ({@code Ring4Ambience}), so it has no particle type and no server ever knows.
 * <p>
 * Its sprite is {@code corruptedadventure:deep_silhouette} from the particle atlas (stream T); until that exists, the
 * soft vanilla {@code generic_7} blob stands in, so it reads as a dark mass rather than a shape.
 */
public final class DeepSilhouetteParticle extends TextureSheetParticle {
    private static final ResourceLocation SPRITE = CorruptedAdventure.id("deep_silhouette");
    private static final ResourceLocation PLACEHOLDER = ResourceLocation.withDefaultNamespace("generic_7");
    /** Half the width in blocks: some 12 blocks across, seen from 14–20 blocks away. */
    private static final float SIZE = 6.0F;
    /** Glide per tick, blocks. */
    private static final double GLIDE = 0.04;
    private static final float MAX_ALPHA = 0.7F;

    /** @param dx, dz the glide direction (a unit vector) */
    public DeepSilhouetteParticle(ClientLevel level, double x, double y, double z, double dx, double dz) {
        super(level, x, y, z);
        this.xd = dx * GLIDE;
        this.yd = 0.0;
        this.zd = dz * GLIDE;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = 80 + random.nextInt(41);
        this.quadSize = SIZE;
        setSize(SIZE * 2, SIZE * 2);
        setColor(0.02F, 0.04F, 0.06F);
        this.alpha = 0.0F;
        setSprite(sprite());
    }

    private static TextureAtlasSprite sprite() {
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        TextureAtlasSprite sprite = atlas.getSprite(SPRITE);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? atlas.getSprite(PLACEHOLDER) : sprite;
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        alpha = MAX_ALPHA * Mth.clamp(Math.min(life * 3.0F, (1.0F - life) * 3.0F), 0.0F, 1.0F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
