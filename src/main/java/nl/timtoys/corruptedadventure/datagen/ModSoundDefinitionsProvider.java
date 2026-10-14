package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * {@code sounds.json}. Ring 1 and the sandstorm reuse vanilla's samples through {@code event} entries (pitch and volume
 * multiply those of the vanilla event); the ring 2 ambient sounds are our own {@code .ogg}s ({@code tools/sounds/Ring2Sounds.java}),
 * which must exist before datagen runs: the provider checks the files. Ring 3 has its own as well
 * ({@code tools/sounds/Ring3Sounds.java}, package 50).
 */
final class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    // Ambient volumes (teststop 4c: all ambience somewhat softer). Only sounds the game plays at a fixed volume are set
    // here (biome loops, additions and moods, the ring 2 one-shots); weather loops, rumbles and block sounds get their
    // volume in code (MAX_VOLUME in the weather renderers, Rumble, Ring3Ambience, the blocks).
    /** Biome loops and additions of our own: 30 % under the file's level. */
    private static final float AMBIENT_VOLUME = 0.7F;
    /** Ember woods loop: softer still, it runs through the whole forest and grew tiresome. */
    private static final float EMBER_WOODS_VOLUME = 0.6F;
    /** Ring 2 one-shots (singing sand, oasis birds). */
    private static final float RING_2_VOLUME = 0.6F;
    /** Nether ring 2 single drops. */
    private static final float DRIPS_VOLUME = 0.55F;
    /** The distorted cave sound (ring 1 mood). */
    private static final float STRANGE_CAVE_VOLUME = 0.6F;

    ModSoundDefinitionsProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CorruptedAdventure.MOD_ID, files);
    }

    @Override
    public void registerSounds() {
        // Vanilla's cave sounds, lower and softer: recognisable, but wrong.
        add(ModSoundEvents.RING_1_STRANGE_CAVE, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.ambient.ring_1.strange_cave")
                .with(caveSound(0.5F), caveSound(0.6F), caveSound(0.7F)));
        // Sandstorm wind loop: vanilla's elytra wind, lower. Volume follows the storm on the client (WeatherLoopSound).
        add(ModSoundEvents.RING_2_SANDSTORM, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.weather.ring_2.sandstorm")
                .with(sound(SoundEvents.ELYTRA_FLYING.getLocation(), SoundDefinition.SoundType.EVENT).pitch(0.7F)));
        // Singing sand: a low hum; positional on the client, so it fades with distance (the .ogg's are mono).
        add(ModSoundEvents.RING_2_SINGING_SAND, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.ambient.ring_2.singing_sand")
                .with(ring2File("singing_sand_1"), ring2File("singing_sand_2"), ring2File("singing_sand_3")));
        add(ModSoundEvents.RING_2_OASIS, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.ambient.ring_2.oasis")
                .with(ring2File("oasis_1"), ring2File("oasis_2"), ring2File("oasis_3")));
        // Nether ring 2: our own seamless loop (streamed, no subtitle like vanilla's loops) and vanilla's dripstone drops.
        add(ModSoundEvents.NETHER_RING_2_DRIP_LOOP, SoundDefinition.definition()
                .with(netherFile("drip_loop_1"), netherFile("drip_loop_2")));
        add(ModSoundEvents.NETHER_RING_2_DRIPS, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.ambient.nether_ring_2.drips")
                .with(sound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER.getLocation(), SoundDefinition.SoundType.EVENT)
                                .weight(3).pitch(0.85F).volume(DRIPS_VOLUME),
                        sound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON.getLocation(), SoundDefinition.SoundType.EVENT)
                                .weight(1).pitch(0.75F).volume(DRIPS_VOLUME)));
        // Ring 3 (package 50): our own audio (tools/sounds/Ring3Sounds.java). Loops are streamed and carry no subtitle,
        // like vanilla's; one-shots are mono, so positional ones fade with distance.
        // Weather (package 45): the loops' volume follows the weather on the client (WeatherLoopSound); the eruption's
        // rumbles are played without attenuation (Rumble).
        add(ModSoundEvents.RING_3_ASH_RAIN, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.weather.ring_3.ash_rain")
                .with(loop("weather/ring_3/ash_rain_loop_1"), loop("weather/ring_3/ash_rain_loop_2")));
        add(ModSoundEvents.RING_3_ERUPTION, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.weather.ring_3.eruption")
                .with(file("weather/ring_3/eruption_1"), file("weather/ring_3/eruption_2"), file("weather/ring_3/eruption_3")));
        add(ModSoundEvents.RING_3_ASH_CLOUD, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.weather.ring_3.ash_cloud")
                .with(loop("weather/ring_3/ash_cloud_loop_1")));
        // Ambience: the distant rumble outside the weather (Ring3Ambience), lighter than an eruption's.
        add(ModSoundEvents.RING_3_RUMBLE, SoundDefinition.definition()
                .subtitle(subtitle("ambient.ring_3.rumble"))
                .with(file("ambient/ring_3/rumble_1"), file("ambient/ring_3/rumble_2"), file("ambient/ring_3/rumble_3")));
        add(ModSoundEvents.RING_3_EMBERS, SoundDefinition.definition()
                .with(loop("ambient/ring_3/embers_loop_1").volume(EMBER_WOODS_VOLUME),
                        loop("ambient/ring_3/embers_loop_2").volume(EMBER_WOODS_VOLUME)));
        add(ModSoundEvents.RING_3_LAVA, SoundDefinition.definition()
                .subtitle(subtitle("ambient.ring_3.lava"))
                .with(ambient("ambient/ring_3/lava_1"), ambient("ambient/ring_3/lava_2"), ambient("ambient/ring_3/lava_3")));
        add(ModSoundEvents.RING_3_TITAN_HUM, SoundDefinition.definition()
                .with(ambientLoop("ambient/ring_3/titan_hum_loop_1")));
        add(ModSoundEvents.RING_3_CALDERA, SoundDefinition.definition()
                .with(ambientLoop("ambient/ring_3/caldera_loop_1")));
        // Nether ring 3 (package 56): the cold wind (loop) and creaking ice (additions).
        add(ModSoundEvents.NETHER_RING_3_WIND, SoundDefinition.definition()
                .with(ambientLoop("ambient/nether_ring_3/wind_loop_1"), ambientLoop("ambient/nether_ring_3/wind_loop_2")));
        add(ModSoundEvents.NETHER_RING_3_ICE_CREAK, SoundDefinition.definition()
                .subtitle(subtitle("ambient.nether_ring_3.ice_creak"))
                .with(ambient("ambient/nether_ring_3/ice_creak_1"), ambient("ambient/nether_ring_3/ice_creak_2"),
                        ambient("ambient/nether_ring_3/ice_creak_3")));
        // Ring 4 weather (package 70): vanilla placeholders until the ring 4 sound script (80). The loops' volume follows
        // the weather on the client (WeatherLoopSound).
        add(ModSoundEvents.RING_4_DENSE_FOG, SoundDefinition.definition()
                .subtitle(subtitle("weather.ring_4.dense_fog"))
                .with(sound(SoundEvents.AMBIENT_UNDERWATER_LOOP.getLocation(), SoundDefinition.SoundType.EVENT).pitch(0.6F)));
        add(ModSoundEvents.RING_4_STORM_WIND, SoundDefinition.definition()
                .subtitle(subtitle("weather.ring_4.storm_wind"))
                .with(sound(SoundEvents.ELYTRA_FLYING.getLocation(), SoundDefinition.SoundType.EVENT).pitch(0.5F)));
        add(ModSoundEvents.RING_4_TOXIC_RAIN, SoundDefinition.definition()
                .subtitle(subtitle("weather.ring_4.toxic_rain"))
                .with(sound(SoundEvents.WEATHER_RAIN.getLocation(), SoundDefinition.SoundType.EVENT).pitch(0.8F)));
        // Block sounds, played by stream B (package 46) at the block.
        add(ModSoundEvents.FUMAROLE_PUFF, SoundDefinition.definition()
                .subtitle(subtitle("block.fumarole.puff"))
                .with(file("block/ring_3/fumarole_puff_1"), file("block/ring_3/fumarole_puff_2")));
        add(ModSoundEvents.GEYSER_BURST, SoundDefinition.definition()
                .subtitle(subtitle("block.geyser.burst"))
                .with(file("block/ring_3/geyser_burst_1"), file("block/ring_3/geyser_burst_2")));
        add(ModSoundEvents.GEYSER_WARNING, SoundDefinition.definition()
                .subtitle(subtitle("block.geyser.warning"))
                .with(file("block/ring_3/geyser_warning_1"), file("block/ring_3/geyser_warning_2")));
        add(ModSoundEvents.BOILING_MUD_BUBBLE, SoundDefinition.definition()
                .subtitle(subtitle("block.boiling_mud.bubble"))
                .with(file("block/ring_3/boiling_mud_bubble_1"), file("block/ring_3/boiling_mud_bubble_2"),
                        file("block/ring_3/boiling_mud_bubble_3")));
    }

    /** One of our own one-shots, {@code sounds/<path>.ogg}. */
    private static SoundDefinition.Sound file(String path) {
        return sound(CorruptedAdventure.id(path), SoundDefinition.SoundType.SOUND);
    }

    /** One of our own seamless loops, streamed. */
    private static SoundDefinition.Sound loop(String path) {
        return file(path).stream();
    }

    /** A biome addition of our own at {@link #AMBIENT_VOLUME}. */
    private static SoundDefinition.Sound ambient(String path) {
        return file(path).volume(AMBIENT_VOLUME);
    }

    /** A biome loop of our own at {@link #AMBIENT_VOLUME}. */
    private static SoundDefinition.Sound ambientLoop(String path) {
        return loop(path).volume(AMBIENT_VOLUME);
    }

    private static String subtitle(String event) {
        return "subtitles.corruptedadventure." + event;
    }

    private static SoundDefinition.Sound netherFile(String name) {
        return ambientLoop("ambient/nether_ring_2/" + name);
    }

    private static SoundDefinition.Sound ring2File(String name) {
        return file("ambient/ring_2/" + name)
                .volume(RING_2_VOLUME)
                .attenuationDistance(24);
    }

    private static SoundDefinition.Sound caveSound(float pitch) {
        return sound(SoundEvents.AMBIENT_CAVE.value().getLocation(), SoundDefinition.SoundType.EVENT)
                .pitch(pitch)
                .volume(STRANGE_CAVE_VOLUME);
    }
}
