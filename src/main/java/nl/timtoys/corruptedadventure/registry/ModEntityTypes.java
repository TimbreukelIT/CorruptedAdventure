package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.entity.EffectArrow;
import nl.timtoys.corruptedadventure.entity.hollow.HollowRift;
import nl.timtoys.corruptedadventure.entity.hollow.TheHollow;

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

    /**
     * Phase 4: The Hollow, the ring 1 Nightmare. Hitbox 2.5 x 5.5 (the model is bigger, see its culling box); tracked
     * from far, a boss is seen before it is near.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TheHollow>> THE_HOLLOW = ENTITY_TYPES.register("the_hollow",
            () -> EntityType.Builder.<TheHollow>of(TheHollow::new, MobCategory.MONSTER)
                    .sized(2.5F, 5.5F).eyeHeight(4.4F).clientTrackingRange(10).build("the_hollow"));
    /** Phase 4 step 2: The Hollow's dimension rift (~3 × 7 blocks); {@code /summon} starts an arrival. */
    public static final DeferredHolder<EntityType<?>, EntityType<HollowRift>> HOLLOW_RIFT = ENTITY_TYPES.register("hollow_rift",
            () -> EntityType.Builder.<HollowRift>of(HollowRift::new, MobCategory.MISC)
                    .sized(3.0F, 7.0F).clientTrackingRange(10).updateInterval(20).fireImmune().build("hollow_rift"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(ModEntityTypes::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(THE_HOLLOW.get(), TheHollow.createAttributes().build());
    }

    /** Sized and tracked like vanilla's arrow. */
    private static DeferredHolder<EntityType<?>, EntityType<EffectArrow>> arrow(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<EffectArrow>of(EffectArrow::new, MobCategory.MISC)
                .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(name));
    }
}
