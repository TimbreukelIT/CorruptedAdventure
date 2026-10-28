package nl.timtoys.corruptedadventure.client.entity.hollow;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.entity.nightmare.NightmareRenderer;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/**
 * Draws The Hollow: the stone body ({@code the_hollow.png}), the corruption sliding down ({@link HollowCorruptionLayer})
 * and, from {@link NightmareRenderer}, the emissive eyes, soul remnants and corruption rims. Out of phase the body is
 * see-through; the glow stays, so Herobrine's eyes hang in the air. No head tracking: its head hangs on a broken neck.
 */
public final class TheHollowRenderer extends NightmareRenderer<TheHollow> {
    private static final float PHASED_ALPHA = 0.3F;

    private TheHollowRenderer(EntityRendererProvider.Context context) {
        super(context, CorruptedAdventure.id("the_hollow"), false, 1.6F);
        addRenderLayer(new HollowCorruptionLayer(this));
    }

    /** The Hollow and its rift. */
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.THE_HOLLOW.get(), TheHollowRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.HOLLOW_RIFT.get(), HollowRiftRenderer::new);
    }

    @Override
    public float alpha(TheHollow hollow) {
        return hollow.isPhased() ? PHASED_ALPHA : 1.0F;
    }
}
