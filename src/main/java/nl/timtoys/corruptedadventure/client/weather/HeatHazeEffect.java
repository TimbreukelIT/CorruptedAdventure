package nl.timtoys.corruptedadventure.client.weather;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * The shimmer of the heat wave: our own {@link PostChain} ({@code shaders/post/heat_haze.json}) over the main render
 * target, run after the level and before the hand. Loaded on first use; closed on resource reload (F3+T, packs) and on
 * logout, resized with the window. A chain that fails to load is not retried until the next resource reload.
 * <p>
 * A post pass that writes {@code minecraft:main} clears its depth; the chain's spare target {@code depth} keeps a copy,
 * so later {@code AFTER_LEVEL} renderers of other mods still see the scene's depth.
 */
final class HeatHazeEffect {
    private static final ResourceLocation LOCATION = CorruptedAdventure.id("shaders/post/heat_haze.json");
    /** Below this the effect is not run at all. */
    private static final float MIN_AMOUNT = 0.01F;

    private static @Nullable PostChain chain;
    private static boolean failed;
    private static int width;
    private static int height;

    private HeatHazeEffect() {
    }

    /** Distorts the main target by {@code amount} (0–1: strength × exposure × Distortion Effects). */
    static void render(Minecraft minecraft, float amount, float partialTick) {
        if (amount < MIN_AMOUNT || minecraft.level == null) {
            return;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        PostChain effect = chain(minecraft, main);
        if (effect == null) {
            return;
        }
        if (main.width != width || main.height != height) {
            effect.resize(main.width, main.height);
            width = main.width;
            height = main.height;
        }
        float seconds = (minecraft.level.getGameTime() % 72_000L + partialTick) / 20.0F;
        effect.setUniform("Strength", Math.min(1.0F, amount));
        effect.setUniform("HazeTime", seconds);
        RenderTarget depth = effect.getTempTarget("depth");
        depth.copyDepthFrom(main);
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        effect.process(partialTick);
        main.copyDepthFrom(depth);
        main.bindWrite(true);
        RenderSystem.enableDepthTest();
    }

    private static @Nullable PostChain chain(Minecraft minecraft, RenderTarget main) {
        if (chain == null && !failed) {
            try {
                chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), main, LOCATION);
                width = -1;
                height = -1;
                CorruptedAdventure.LOGGER.debug("Loaded the heat haze shader {}", LOCATION);
            } catch (IOException | JsonSyntaxException exception) {
                failed = true;
                CorruptedAdventure.LOGGER.warn("Could not load the heat haze shader {}; heat waves will not shimmer", LOCATION, exception);
            }
        }
        return chain;
    }

    static void close() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        failed = false;
    }

    static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> close());
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        close();
    }
}
