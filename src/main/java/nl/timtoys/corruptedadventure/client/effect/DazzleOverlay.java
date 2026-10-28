package nl.timtoys.corruptedadventure.client.effect;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;

/**
 * "Dazzled" on the own screen (phase 4, I): a warm white-out over the world, under the HUD, that holds and then fades
 * over the last {@link #FADE_TICKS} of the effect. Reads only the synced effect; not hidden with F1, like vanilla's
 * blindness.
 */
public final class DazzleOverlay {
    private static final int FADE_TICKS = 40;
    /** Never fully opaque: shapes stay faintly visible, it is a mild blindness. */
    private static final float MAX_ALPHA = 0.85F;
    private static final int COLOUR = 0xFFF6D8;

    private DazzleOverlay() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DazzleOverlay::onRegisterGuiLayers);
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, CorruptedAdventure.id("dazzled"), DazzleOverlay::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        MobEffectInstance dazzled = minecraft.player.getEffect(ModMobEffects.DAZZLED);
        if (dazzled == null) {
            return;
        }
        float remaining = dazzled.getDuration() - deltaTracker.getGameTimeDeltaPartialTick(false);
        float alpha = MAX_ALPHA * Math.clamp(remaining / FADE_TICKS, 0.0F, 1.0F);
        if (alpha > 0.0F) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), Math.round(alpha * 255.0F) << 24 | COLOUR);
        }
    }
}
