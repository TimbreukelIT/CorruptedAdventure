package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.EffectArrow;

/** Entity types. Their renderers register in {@code client/} (CLAUDE.md §7). */
public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CorruptedAdventure.MOD_ID);

    /** Phase 2 golf 3 (§6.4): the arrow of {@code ArrowEffect.FIRE}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EffectArrow>> FIRE_ARROW = arrow("fire_arrow");
    /** Phase 2 golf 4 (§6.7): the arrow of {@code ArrowEffect.POISON}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EffectArrow>> POISON_ARROW = arrow("poison_arrow");
    /** Phase 2 golf 5 (§7.4): the arrow of {@code ArrowEffect.GLITCH}. */
    public static final DeferredHolder<EntityType<?>, EntityType<EffectArrow>> GLITCH_ARROW = arrow("glitch_arrow");

    private ModEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }

    /** Sized and tracked like vanilla's arrow. */
    private static DeferredHolder<EntityType<?>, EntityType<EffectArrow>> arrow(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<EffectArrow>of(EffectArrow::new, MobCategory.MISC)
                .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(name));
    }
}
