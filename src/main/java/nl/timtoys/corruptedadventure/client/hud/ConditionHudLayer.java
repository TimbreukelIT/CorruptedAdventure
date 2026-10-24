package nl.timtoys.corruptedadventure.client.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.client.ConditionExposure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionDefinitions;
import nl.timtoys.corruptedadventure.condition.ConditionMeters;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModAttachments;

/**
 * One bar per condition with a meter above 0, stacked above the food bar (and air bubbles) through
 * {@link Gui#rightHeight}, with a tick at every threshold. Hidden with F1 and, like the food bar, in creative and
 * spectator. Placeholder look ({@code fill} only) until the textures change.
 */
public final class ConditionHudLayer {
    private static final int WIDTH = 81;
    private static final int HEIGHT = 5;
    /** Rows of 6: a 5-pixel bar and a 1-pixel gap; the first bar lines up with the bottom of a vanilla icon row. */
    private static final int ROW_HEIGHT = 6;
    private static final int ICON_ROW_OFFSET = 4;
    private static final int BACKGROUND = 0xC0000000;
    private static final int EMPTY = 0xFF3A3A3A;
    private static final int TICK = 0xC0FFFFFF;

    private ConditionHudLayer() {
    }

    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, CorruptedAdventure.id("conditions"), ConditionHudLayer::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        // Our layer is not inside vanilla's F1 group, so check hideGui ourselves.
        if (player == null || minecraft.options.hideGui || !ConditionExposure.exposed(minecraft)
                || !ServerConfig.SPEC.isLoaded()) {
            return;
        }
        ConditionMeters meters = player.getData(ModAttachments.CONDITION_METERS);
        Gui gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        int left = right - WIDTH;
        for (Condition condition : Condition.values()) {
            int value = meters.rounded(condition);
            if (value <= 0) {
                continue;
            }
            int top = graphics.guiHeight() - gui.rightHeight + ICON_ROW_OFFSET;
            drawBar(graphics, left, top, value, condition.hudColor(), ConditionDefinitions.thresholdLevels(condition));
            gui.rightHeight += ROW_HEIGHT;
        }
    }

    private static void drawBar(GuiGraphics graphics, int left, int top, int value, int color, int[] thresholds) {
        int inner = WIDTH - 2;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, BACKGROUND);
        graphics.fill(left + 1, top + 1, left + 1 + inner, top + HEIGHT - 1, EMPTY);
        graphics.fill(left + 1, top + 1, left + 1 + filled(value, inner), top + HEIGHT - 1, color);
        for (int threshold : thresholds) {
            // The tick is the last pixel the fill covers once the threshold is reached.
            int x = left + Math.max(1, filled(threshold, inner));
            graphics.fill(x, top, x + 1, top + HEIGHT, TICK);
        }
    }

    private static int filled(int value, int width) {
        return Math.round(width * value / 100f);
    }
}
