package nl.timtoys.corruptedadventure.client.weather;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * Rain streaks of a ring weather (package 70): vanilla's rain sheet ({@code LevelRenderer#renderSnowAndRain}) with its
 * own tint, strength and slant, drawn after vanilla's weather (so also in biomes without precipitation and whatever
 * vanilla's weather does). Columns around the camera from the {@code MOTION_BLOCKING} height up, never under a roof.
 * Toxic rain falls straight and green, the storm's rain slants with the wind.
 */
final class RainSheet {
    private static final ResourceLocation RAIN = ResourceLocation.withDefaultNamespace("textures/environment/rain.png");

    private final float red;
    private final float green;
    private final float blue;
    /** Streak speed relative to vanilla rain. */
    private final float speed;

    RainSheet(int rgb, float speed) {
        this.red = (rgb >> 16 & 0xFF) / 255.0F;
        this.green = (rgb >> 8 & 0xFF) / 255.0F;
        this.blue = (rgb & 0xFF) / 255.0F;
        this.speed = speed;
    }

    /**
     * Draws the sheet at {@code AFTER_WEATHER} (vanilla's own weather pass, also the weather target under "Fabulous!").
     *
     * @param amount 0–1, the alpha of the sheet (strength × sky exposure; the streaks also stop at every roof per column)
     * @param slant  horizontal blocks the streak moves per block of fall, towards the direction the wind blows
     */
    void render(RenderLevelStageEvent event, float amount, Vec3 slant) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || level == null || amount <= 0.0F
                || !RingWeatherRenderer.clearView(event.getCamera())) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        int radius = Minecraft.useFancyGraphics() ? 10 : 5;
        int camX = Mth.floor(camera.x);
        int camY = Mth.floor(camera.y);
        int camZ = Mth.floor(camera.z);
        LightTexture lightTexture = minecraft.gameRenderer.lightTexture();
        lightTexture.turnOnLightLayer();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, RAIN);
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int ticks = event.getRenderTick();
        for (int z = camZ - radius; z <= camZ + radius; z++) {
            for (int x = camX - radius; x <= camX + radius; x++) {
                int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int bottom = Math.max(camY - radius, ground);
                int top = Math.max(camY + radius, ground);
                if (bottom == top) {
                    continue;
                }
                // Vanilla's sheet: one quad per column, turned to face the camera, scrolled per column.
                double dx = x + 0.5 - camera.x;
                double dz = z + 0.5 - camera.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                double halfX = distance < 1.0E-3 ? 0.5 : -dz / distance * 0.5;
                double halfZ = distance < 1.0E-3 ? 0.0 : dx / distance * 0.5;
                int hash = x * x * 3121 + x * 45238971 + z * z * 418711 + z * 13761;
                float columnSpeed = 3.0F + ((hash >>> 8) & 0xFF) / 255.0F;
                float scroll = -((ticks & 131071) + (hash & 0xFF) + partialTick) / 32.0F * columnSpeed * speed % 32.0F;
                float fade = (float) (distance / radius);
                float alpha = ((1.0F - fade * fade) * 0.5F + 0.5F) * amount;
                pos.set(x, Math.max(ground, camY), z);
                int light = LevelRenderer.getLightColor(level, pos);
                double height = top - bottom;
                float topX = (float) (x + 0.5 - camera.x - slant.x * height);
                float topZ = (float) (z + 0.5 - camera.z - slant.z * height);
                float bottomX = (float) (x + 0.5 - camera.x);
                float bottomZ = (float) (z + 0.5 - camera.z);
                float topY = (float) (top - camera.y);
                float bottomY = (float) (bottom - camera.y);
                buffer.addVertex((float) (topX - halfX), topY, (float) (topZ - halfZ))
                        .setUv(0.0F, bottom * 0.25F + scroll).setColor(red, green, blue, alpha).setLight(light);
                buffer.addVertex((float) (topX + halfX), topY, (float) (topZ + halfZ))
                        .setUv(1.0F, bottom * 0.25F + scroll).setColor(red, green, blue, alpha).setLight(light);
                buffer.addVertex((float) (bottomX + halfX), bottomY, (float) (bottomZ + halfZ))
                        .setUv(1.0F, top * 0.25F + scroll).setColor(red, green, blue, alpha).setLight(light);
                buffer.addVertex((float) (bottomX - halfX), bottomY, (float) (bottomZ - halfZ))
                        .setUv(0.0F, top * 0.25F + scroll).setColor(red, green, blue, alpha).setLight(light);
            }
        }
        MeshData mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
        // Vanilla's end state of its weather pass; vanilla only touches the depth mask while its own rain falls, so set it
        // back to the default here (the world border and the rest of the frame set their own).
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        lightTexture.turnOffLightLayer();
    }
}
