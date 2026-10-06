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

    /** Low hum of the singing sand in the dune sea (change 32), played by the client at night mostly ({@code SingingSandAmbience}). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_2_SINGING_SAND = register("ambient.ring_2.singing_sand");
    /** Birds and insects of the oasis (change 32), the additions sound of {@code oasis_flats}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_2_OASIS = register("ambient.ring_2.oasis");

    /** Seamless damp loop, the ambient sound of both Nether ring 2 biomes (change 36). */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_2_DRIP_LOOP = register("ambient.nether_ring_2.drip_loop");
    /** Single drops, the additions sound of both Nether ring 2 biomes (change 36). */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_2_DRIPS = register("ambient.nether_ring_2.drips");

    /** Soft crackling loop of ash rain (package 45), played by the client while it is felt. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_ASH_RAIN = register("weather.ring_3.ash_rain");
    /** Deep rumble of an eruption (package 45): one-shots at random intervals, also the ash cloud's warning. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_ERUPTION = register("weather.ring_3.eruption");
    /** Roaring loop inside an ash cloud (package 45). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_ASH_CLOUD = register("weather.ring_3.ash_cloud");

    private ModSoundEvents() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CorruptedAdventure.id(name)));
    }
}
