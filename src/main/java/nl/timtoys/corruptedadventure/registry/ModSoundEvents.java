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
    /** Gurgling and groaning of a geyser a second before it erupts (package 50c; stream B plays it). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GEYSER_WARNING = register("block.geyser.warning");
    /** Bubble of boiling mud (stream B plays it, package 46). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILING_MUD_BUBBLE = register("block.boiling_mud.bubble");

    // Ring 4 weather (package 70): loops the client plays while the weather is felt; own audio since 80.
    /** Soft, muffled hum in dense fog (mythical band). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_DENSE_FOG = register("weather.ring_4.dense_fog");
    /** Hard wind of the storm (mythical band). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_STORM_WIND = register("weather.ring_4.storm_wind");
    /** Rain of toxic rain (blighted band). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_TOXIC_RAIN = register("weather.ring_4.toxic_rain");

    // verdant_falls (transition 3→4, stream A): own audio from tools/sounds/Ring4Sounds.java.
    /** Roar of distant waterfalls, the ambient loop of {@code verdant_falls}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VERDANT_FALLS_FALLS = register("ambient.verdant_falls.falls");
    /** Tropical birds and frogs, the additions sound of {@code verdant_falls}. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VERDANT_FALLS_WILDLIFE = register("ambient.verdant_falls.wildlife");

    // Ring 4 and Nether ring 4 (package 80-0, stream A): ids for golf 2; own audio from tools/sounds/Ring4Sounds.java (80).
    /** Water all around: the ambient loop of the mythical ring 4 biomes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_WATER = register("ambient.ring_4.water");
    /** Harp and choir tones at night (Q19: only at night, through a client controller). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_NIGHT_CHOIR = register("ambient.ring_4.night_choir");
    /** Something rustling in the reeds (client-only tension, per player, not synced). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_REEDS = register("ambient.ring_4.reeds");
    /** Bubbles rising from the deep pools. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_BUBBLES = register("ambient.ring_4.bubbles");
    /** Low drone of the emptying zone 4→5 ({@code sundered_reach}). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_4_SUNDERED_DRONE = register("ambient.ring_4.sundered_drone");
    /** Dry wind of Nether ring 4. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_4_WIND = register("ambient.nether_ring_4.wind");
    /** Soft hum of the crystals of Nether ring 4, the additions sound of its biomes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_4_CRYSTAL_HUM = register("ambient.nether_ring_4.crystal_hum");
    /** Swamp gas catching fire (stream B plays it, package 72). */
    public static final DeferredHolder<SoundEvent, SoundEvent> SWAMP_GAS_IGNITE = register("block.swamp_gas.ignite");
    /** Bubble of a bubble spring (stream B plays it). */
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBLE_SPRING_BUBBLE = register("block.bubble_spring.bubble");
    /** Rushing of an updraft (stream B plays it, package 72). */
    public static final DeferredHolder<SoundEvent, SoundEvent> UPDRAFT_FLOW = register("block.updraft.flow");
    /** Slurp of sucking mud, its tell (G1; stream B plays it, package 72). */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUCKING_MUD_SLURP = register("block.sucking_mud.slurp");

    // Ring 5 weather (package 92, stream A): loops the client plays while the weather is felt; own audio from
    // tools/sounds/Ring5Sounds.java since 98.
    /** Crackling, stuttering hum of the glitch storm. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_5_GLITCH_STORM = register("weather.ring_5.glitch_storm");
    /** Low, wrong rain of the reverse rain. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_5_REVERSE_RAIN = register("weather.ring_5.reverse_rain");
    /** The almost silent hum of the standstill, while every other sound drains away. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_5_STANDSTILL = register("weather.ring_5.standstill");

    // ring 5 — A (package 98): ambience of ring 5 and Nether ring 5, own audio from tools/sounds/Ring5Sounds.java.
    /** Low, sucking drone of the void, the loop of the outer band. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_5_VOID_DRONE = register("ambient.ring_5.void_drone");
    /** Short digital crackle: the inner band's rare additions and a void teleport. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RING_5_GLITCH = register("ambient.ring_5.glitch");
    /** Almost silent: the "music" of the silent biomes, so vanilla's game music never plays there. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_5_SILENCE = register("ambient.nether_ring_5.silence");
    /** Heartbeat, the only sound left at the top of the silence meter. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NETHER_RING_5_HEARTBEAT = register("ambient.nether_ring_5.heartbeat");

    // Items (phase 2): played by the server, so everyone nearby hears them.
    /** The echo shell's blow (golf 5): a low conch call that echoes; own audio from tools/sounds/Ring5Sounds.java. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ECHO_SHELL_BLOW = register("item.echo_shell.blow");
    /** The sunstone flash (phase 4, I): a bright crystal ring, vanilla's amethyst resonance pitched up. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SUNSTONE_FLASH = register("item.sunstone_flash.flash");
    /** The volcanite horn's blow (phase 4, I): a goat horn call, lower and heavier. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VOLCANITE_HORN_BLOW = register("item.volcanite_horn.blow");

    private ModSoundEvents() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(CorruptedAdventure.id(name)));
    }
}
