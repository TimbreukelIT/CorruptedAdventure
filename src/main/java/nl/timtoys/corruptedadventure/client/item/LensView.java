package nl.timtoys.corruptedadventure.client.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.weather.GlitchVisuals;
import nl.timtoys.corruptedadventure.item.SkyGlassLensItem;
import nl.timtoys.corruptedadventure.network.LensScanPayload;
import nl.timtoys.corruptedadventure.registry.ModItems;

/**
 * The sky glass lens on the client (phase 2 golf 5, §7.2): its own scope texture (vanilla draws the scope, the zoom and
 * the rest of a spyglass), the outlines the server sent ({@link LensMarkers}, {@link LensRenderer}) and, while you look,
 * the glitch visuals of ring 5 damped to {@link #DAMPED} ({@link GlitchVisuals}), fading in and out over a few ticks.
 * Draws only; what is shown is the server's.
 */
public final class LensView {
    private static final ResourceLocation SCOPE = CorruptedAdventure.id("textures/misc/sky_glass_lens_scope.png");
    /** What is left of the glitch fog and the screen's distortion while you look (provisional). */
    static final float DAMPED = 0.25F;
    /** How far the damping moves per tick: full in or out in 5 ticks. */
    static final float FADE = (1.0F - DAMPED) / 5.0F;

    private LensView() {
    }

    public static void register(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(LensView::onRegisterClientExtensions);
        gameBus.addListener(LensView::onClientTick);
        gameBus.addListener(LensRenderer::onRenderLevelStage);
        gameBus.addListener(LensView::onLoggingOut);
    }

    /** The server's latest scan for the local player (empty: they stopped looking). */
    public static void show(LensScanPayload payload) {
        LensMarkers.set(payload);
    }

    /** The glitch visuals' scale one tick on from {@code current}, towards {@link #DAMPED} while looking. Pure. */
    static float nextScale(float current, boolean looking) {
        float target = looking ? DAMPED : 1.0F;
        return current < target ? Math.min(target, current + FADE) : Math.max(target, current - FADE);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public ResourceLocation getScopeOverlayTexture(ItemStack stack) {
                return SCOPE;
            }
        }, ModItems.SKY_GLASS_LENS.get());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        GlitchVisuals.setScale(nextScale(GlitchVisuals.scale(), player != null && SkyGlassLensItem.lookingThrough(player)));
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LensMarkers.clear();
        GlitchVisuals.setScale(1.0F);
    }
}
