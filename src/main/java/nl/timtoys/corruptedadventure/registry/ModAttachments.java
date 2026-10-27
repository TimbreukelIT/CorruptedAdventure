package nl.timtoys.corruptedadventure.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConditionMeters;
import nl.timtoys.corruptedadventure.condition.RateEffectTimers;

/** Data attachments (per player and other holders). */
public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CorruptedAdventure.MOD_ID);

    /**
     * Condition meters of a player. Saved with the player (kept on logout and dimension change); not copied on death,
     * so a respawned player starts at 0.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ConditionMeters>> CONDITION_METERS =
            ATTACHMENT_TYPES.register("condition_meters",
                    () -> AttachmentType.builder(ConditionMeters::new).serialize(ConditionMeters.CODEC).build());
    /**
     * The timed rate effects of consumables (phase 2 golf 2). Saved with the player like the meters and, like them, not
     * copied on death. Server-only.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RateEffectTimers>> RATE_EFFECTS =
            ATTACHMENT_TYPES.register("rate_effects",
                    () -> AttachmentType.builder(RateEffectTimers::new).serialize(RateEffectTimers.CODEC).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
