package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * Sound events. Registered on both sides: biome JSON names them, so a dedicated server must know the ids too. What
 * they sound like is client data ({@code sounds.json}, from datagen).
 */
public final class ModSoundEvents {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, CorruptedAdventure.MOD_ID);

    /** Cave mood sound of the strange zones in ring 1 ({@code withered_expanse}). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_1_STRANGE_CAVE = register("ambient.ring_1.strange_cave");
    /** Wind loop of the ring 2 sandstorm (change 28), played by the client while the storm is felt. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_2_SANDSTORM = register("weather.ring_2.sandstorm");

    private ModSoundEvents() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CorruptedAdventure.id(name)));
    }
}
