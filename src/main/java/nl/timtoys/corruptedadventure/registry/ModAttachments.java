package nl.timtoys.corruptedadventure.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.ConditionMeters;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareThreat;

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
     * Phase 4: the Nightmare threat per ring of a player. Saved with the player and kept on death: dying does not make a
     * Nightmare forget you.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NightmareThreat>> NIGHTMARE_THREAT =
            ATTACHMENT_TYPES.register("nightmare_threat",
                    () -> AttachmentType.builder(NightmareThreat::new).serialize(NightmareThreat.CODEC).copyOnDeath().build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }
}
