package nl.timtoys.corruptedadventure.client.entity.nightmare;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.util.Color;

/**
 * What every Nightmare's renderer shares: the GeckoLib model by name ({@code geo/entity/<name>.geo.json},
 * {@code animations/entity/<name>.animation.json}, {@code textures/entity/<name>.png}), the emissive layer
 * ({@code textures/entity/<name>_glowmask.png}, GeckoLib {@link AutoGlowingGeoLayer}), see-through rendering when
 * {@link #alpha} is below 1 (the glow stays opaque), and death: the death animation does the falling, so vanilla's
 * sideways roll and red tint are off (a hit still flashes red). The culling box is the entity's
 * ({@code Nightmare.getBoundingBoxForCulling}). A Nightmare's renderer adds its own layers after this constructor.
 */
public class NightmareRenderer<T extends Nightmare> extends GeoEntityRenderer<T> {
    /**
     * @param name       the model's name, e.g. {@code the_hollow}
     * @param turnsHead  whether the {@code head} bone follows where it looks
     * @param shadow     the shadow radius in blocks
     */
    protected NightmareRenderer(EntityRendererProvider.Context context, ResourceLocation name, boolean turnsHead, float shadow) {
        super(context, new DefaultedEntityGeoModel<>(name, turnsHead));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = shadow;
    }

    /** 1 = solid; below 1 the body (and every layer that asks) is drawn see-through. */
    public float alpha(T nightmare) {
        return 1.0F;
    }

    @Override
    public RenderType getRenderType(T nightmare, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return alpha(nightmare) < 1.0F ? RenderType.entityTranslucent(texture)
                : super.getRenderType(nightmare, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(T nightmare, float partialTick, int packedLight) {
        return Color.ofRGBA(1.0F, 1.0F, 1.0F, alpha(nightmare));
    }

    /**
     * Degrees the whole body rocks sideways while it staggers ({@code Nightmare.isStaggered}), on top of any stagger
     * animation of its own; 0 for none.
     */
    protected float staggerSway(T nightmare) {
        return 5.0F;
    }

    @Override
    protected void applyRotations(T nightmare, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick,
                                  float nativeScale) {
        super.applyRotations(nightmare, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        float sway = nightmare.isStaggered() && !nightmare.isDeadOrDying() ? staggerSway(nightmare) : 0.0F;
        if (sway != 0.0F) {
            // A heavy, uneven rock: two sines, so it does not look like a metronome.
            float rock = Mth.sin(ageInTicks * 0.7F) * 0.7F + Mth.sin(ageInTicks * 1.9F) * 0.3F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(sway * rock));
        }
    }

    @Override
    protected float getDeathMaxRotation(T nightmare) {
        return 0.0F;
    }

    @Override
    public int getPackedOverlay(T nightmare, float u, float partialTick) {
        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(nightmare.hurtTime > 0 && !nightmare.isDeadOrDying()));
    }
}
