package nl.timtoys.corruptedadventure.client.entity.risen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.entity.nightmare.NightmareRenderer;
import nl.timtoys.corruptedadventure.entity.risen.AshWalker;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * Draws The Risen: the charred bone and burning feathers ({@code the_risen.png}), from {@link NightmareRenderer} the
 * emissive furnace, eyes and hot edges ({@code the_risen_glowmask.png}), and the breathing embers on top
 * ({@link RisenEmberLayer}). The mockup is drawn at {@link #SCALE} (span ~25 blocks). No head tracking: the animations
 * hold the head. At its arrival under the open sky its shadow sweeps the ground ({@link RisenShadow}). Also registers
 * the placeholder ash walker's renderer (vanilla husk) and the roaming trace's (nothing: it is only particles).
 */
public final class TheRisenRenderer extends NightmareRenderer<TheRisen> {
    /** The approved mockup spans ~34 blocks; the design doc says ~24. */
    static final float SCALE = 0.75F;

    private TheRisenRenderer(EntityRendererProvider.Context context) {
        super(context, CorruptedAdventure.id("the_risen"), false, 3.0F);
        withScale(SCALE);
        addRenderLayer(new RisenEmberLayer(this));
    }

    @Override
    public void render(TheRisen risen, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight) {
        super.render(risen, entityYaw, partialTick, poseStack, buffers, packedLight);
        RisenShadow.render(risen, partialTick, poseStack, buffers);
    }

    @SuppressWarnings("unchecked")
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_RISEN.get(), TheRisenRenderer::new);
        // Placeholder (backlog): an ash walker is a zombie, the husk's renderer draws any zombie.
        event.registerEntityRenderer(ModEntityTypes.ASH_WALKER.get(),
                context -> (EntityRenderer<AshWalker>) (EntityRenderer<?>) new HuskRenderer(context));
        event.registerEntityRenderer(ModEntityTypes.RISEN_TRACE.get(), NoopRenderer::new);
    }
}
