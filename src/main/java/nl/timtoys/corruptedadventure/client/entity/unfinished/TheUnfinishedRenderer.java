package nl.timtoys.corruptedadventure.client.entity.unfinished;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.entity.nightmare.NightmareRenderer;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * Draws The Unfinished with GeckoLib ({@code the_unfinished}: model, animations, base texture and glowmask through
 * {@link NightmareRenderer}) plus the crawling glitch overlay ({@link UnfinishedGlitchLayer}). Before each frame
 * {@link UnfinishedFlicker} swaps parts of the model (no shaders): unloaded parts flicker between wire frame, missing
 * texture and nothing; loaded by a sky glass lens they turn solid; grafts that fell away are gone.
 */
public final class TheUnfinishedRenderer extends NightmareRenderer<TheUnfinished> {
    private TheUnfinishedRenderer(EntityRendererProvider.Context context) {
        // No head tracking: his four heads move on their own.
        super(context, CorruptedAdventure.id("the_unfinished"), false, 4.0F);
        addRenderLayer(new UnfinishedGlitchLayer(this));
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_UNFINISHED.get(), TheUnfinishedRenderer::new);
    }

    @Override
    public void preRender(PoseStack poseStack, TheUnfinished boss, BakedGeoModel model, @Nullable MultiBufferSource bufferSource,
                          @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        super.preRender(poseStack, boss, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        if (!isReRender) {
            // The baked model is shared by every Unfinished: set every swapped bone for this one, each frame.
            UnfinishedFlicker.apply(model, boss);
        }
    }
}
