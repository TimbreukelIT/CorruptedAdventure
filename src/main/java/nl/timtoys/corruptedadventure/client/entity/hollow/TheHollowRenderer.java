package nl.timtoys.corruptedadventure.client.entity.hollow;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * Draws The Hollow with GeckoLib: {@code geo/entity/the_hollow.geo.json}, {@code animations/entity/the_hollow.animation.json}
 * and three layers: the stone body ({@code the_hollow.png}), the corruption sliding down ({@link HollowCorruptionLayer})
 * and the emissive eyes, soul remnants and corruption rims ({@code the_hollow_glowmask.png}). Out of phase the body is
 * see-through; the glow stays, so Herobrine's eyes hang in the air. Its death is the animation (it falls backwards),
 * so vanilla's sideways death roll and red death tint are off.
 */
public final class TheHollowRenderer extends GeoEntityRenderer<TheHollow> {
    private static final float PHASED_ALPHA = 0.3F;

    private TheHollowRenderer(EntityRendererProvider.Context context) {
        // No head tracking: its head hangs on a broken neck.
        super(context, new DefaultedEntityGeoModel<>(CorruptedAdventure.id("the_hollow"), false));
        addRenderLayer(new HollowCorruptionLayer(this));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 1.6F;
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_HOLLOW.get(), TheHollowRenderer::new);
    }

    static float alpha(TheHollow hollow) {
        return hollow.isPhased() ? PHASED_ALPHA : 1.0F;
    }

    @Override
    public RenderType getRenderType(TheHollow hollow, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return hollow.isPhased() ? RenderType.entityTranslucent(texture) : super.getRenderType(hollow, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(TheHollow hollow, float partialTick, int packedLight) {
        return Color.ofRGBA(1.0F, 1.0F, 1.0F, alpha(hollow));
    }

    @Override
    protected float getDeathMaxRotation(TheHollow hollow) {
        return 0.0F;
    }

    @Override
    public int getPackedOverlay(TheHollow hollow, float u, float partialTick) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(hollow.hurtTime > 0 && !hollow.isDeadOrDying()));
    }
}
