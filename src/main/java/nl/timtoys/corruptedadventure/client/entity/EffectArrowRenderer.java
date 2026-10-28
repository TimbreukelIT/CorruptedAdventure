package nl.timtoys.corruptedadventure.client.entity;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import nl.timtoys.corruptedadventure.entity.ArrowEffect;
import nl.timtoys.corruptedadventure.entity.EffectArrow;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the fire, poison and glitch arrows like vanilla's arrow, each with
 * {@code textures/entity/projectiles/<type>.png}.
 */
public final class EffectArrowRenderer extends ArrowRenderer<EffectArrow> {
    private final Map<ArrowEffect, ResourceLocation> textures = new EnumMap<>(ArrowEffect.class);

    private EffectArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
        for (ArrowEffect effect : ArrowEffect.values()) {
            ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(effect.entityType());
            textures.put(effect, type.withPath(path -> "textures/entity/projectiles/" + path + ".png"));
        }
    }

    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (ArrowEffect effect : ArrowEffect.values()) {
            event.registerEntityRenderer(effect.entityType(), EffectArrowRenderer::new);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EffectArrow arrow) {
        return textures.get(arrow.effect());
    }
}
