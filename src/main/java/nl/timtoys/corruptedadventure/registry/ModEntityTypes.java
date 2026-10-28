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
import nl.timtoys.corruptedadventure.entity.risen.AshWalker;
import nl.timtoys.corruptedadventure.entity.risen.TheRisen;
import nl.timtoys.corruptedadventure.entity.lostkeeper.TheLostKeeper;
import nl.timtoys.corruptedadventure.entity.unfinished.TheUnfinished;

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

    /**
     * Phase 4: The Risen, the ring 3 Nightmare. The hitbox is its body from talons to shoulders (3.5 x 8.5); wings and
     * head are parts of their own ({@code RisenPart}). Fire immune; tracked from far and every other tick, it flies.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TheRisen>> THE_RISEN = ENTITY_TYPES.register("the_risen",
            () -> EntityType.Builder.<TheRisen>of(TheRisen::new, MobCategory.MONSTER).fireImmune()
                    .sized(3.5F, 8.5F).eyeHeight(7.5F).clientTrackingRange(10).updateInterval(2).build("the_risen"));
    /** Phase 4: an ash walker, a victim The Risen lets fall from its feathers. Placeholder (backlog): a zombie. */
    public static final DeferredHolder<EntityType<?>, EntityType<AshWalker>> ASH_WALKER = ENTITY_TYPES.register("ash_walker",
            () -> EntityType.Builder.<AshWalker>of(AshWalker::new, MobCategory.MONSTER).fireImmune()
                    .sized(0.6F, 1.95F).eyeHeight(1.74F).clientTrackingRange(8).build("ash_walker"));
    /**
     * Phase 4: The Lost Keeper, the ring 4 Nightmare. The hitbox is its head (2.6 x 2.5, eyes at its centre); its ~24-block
     * body is a chain of part hitboxes in {@code TheLostKeeper}. Tracked from far.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TheLostKeeper>> THE_LOST_KEEPER = ENTITY_TYPES.register(
            "the_lost_keeper", () -> EntityType.Builder.<TheLostKeeper>of(TheLostKeeper::new, MobCategory.MONSTER)
                    .sized(2.6F, 2.5F).eyeHeight(1.25F).clientTrackingRange(12).build("the_lost_keeper"));

    /**
     * Phase 4: The Unfinished, the ring 5 Nightmare (~20 blocks). Main hitbox 5 x 14 (his torso); the rest of him are
     * parts ({@code UnfinishedPart}). Fire cannot touch a void being; tracked from very far, he is seen over the land.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<TheUnfinished>> THE_UNFINISHED = ENTITY_TYPES.register(
            "the_unfinished", () -> EntityType.Builder.<TheUnfinished>of(TheUnfinished::new, MobCategory.MONSTER)
                    .sized(5.0F, 14.0F).eyeHeight(13.0F).fireImmune().clientTrackingRange(16).build("the_unfinished"));

    private ModEntityTypes() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener(ModEntityTypes::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(THE_HOLLOW.get(), TheHollow.createAttributes().build());
        event.put(THE_RISEN.get(), TheRisen.createAttributes().build());
        event.put(ASH_WALKER.get(), AshWalker.createAttributes().build());
        event.put(THE_LOST_KEEPER.get(), TheLostKeeper.createAttributes().build());
        event.put(THE_UNFINISHED.get(), TheUnfinished.createAttributes().build());
    }

    /** Sized and tracked like vanilla's arrow. */
    private static DeferredHolder<EntityType<?>, EntityType<EffectArrow>> arrow(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<EffectArrow>of(EffectArrow::new, MobCategory.MISC)
                .sized(0.5F, 0.5F).eyeHeight(0.13F).clientTrackingRange(4).updateInterval(20).build(name));
    }
}
