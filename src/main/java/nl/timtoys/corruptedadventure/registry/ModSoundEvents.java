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

    /** Deep rumble of the volcanoes outside the weather (package 50), played by the client at random intervals. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_RUMBLE = register("ambient.ring_3.rumble");
    /** Crackling of the smouldering wood (package 50), the ambient loop of {@code ember_woods}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_EMBERS = register("ambient.ring_3.embers");
    /** Bubbling lava (package 50), the additions sound of the peaks and flows. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_LAVA = register("ambient.ring_3.lava");
    /** Low choir-like hum of the titan fields (package 50), their ambient loop. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_TITAN_HUM = register("ambient.ring_3.titan_hum");
    /** Quiet of the dormant crater (package 50): soft wind, a single insect; the ambient loop of {@code dormant_caldera}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_3_CALDERA = register("ambient.ring_3.caldera");
    /** Cold wind through the ice halls of Nether ring 3 (package 56), their ambient loop. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_3_WIND = register("ambient.nether_ring_3.wind");
    /** Creaking ice (package 56), the additions sound of Nether ring 3. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_3_ICE_CREAK = register("ambient.nether_ring_3.ice_creak");
    /** Steam puff of a fumarole (stream B plays it, package 46). */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUMAROLE_PUFF = register("block.fumarole.puff");
    /** Eruption of a geyser (stream B plays it, package 46). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GEYSER_BURST = register("block.geyser.burst");
    /** Bubble of boiling mud (stream B plays it, package 46). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILING_MUD_BUBBLE = register("block.boiling_mud.bubble");

    private ModSoundEvents() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CorruptedAdventure.id(name)));
    }
}
