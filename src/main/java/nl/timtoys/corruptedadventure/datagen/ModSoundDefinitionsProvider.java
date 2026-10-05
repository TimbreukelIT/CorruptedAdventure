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
 * which must exist before datagen runs: the provider checks the files.
 */
final class ModSoundDefinitionsProvider extends SoundDefinitionsProvider {
    ModSoundDefinitionsProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CorruptedAdventure.MOD_ID, files);
    }

    @Override
    public void registerSounds() {
        // Vanilla's cave sounds, lower and softer: recognisable, but wrong.
        add(ModSoundEvents.RING_1_STRANGE_CAVE, SoundDefinition.definition()
                .subtitle("subtitles.corruptedadventure.ambient.ring_1.strange_cave")
                .with(caveSound(0.5F), caveSound(0.6F), caveSound(0.7F)));
        // Sandstorm wind loop: vanilla's elytra wind, lower. Volume follows the storm on the client (SandstormSound).
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
                                .weight(3).pitch(0.85F).volume(0.75F),
                        sound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON.getLocation(), SoundDefinition.SoundType.EVENT)
                                .weight(1).pitch(0.75F).volume(0.75F)));
    }

    private static SoundDefinition.Sound netherFile(String name) {
        return sound(CorruptedAdventure.id("ambient/nether_ring_2/" + name), SoundDefinition.SoundType.SOUND)
                .stream();
    }

    private static SoundDefinition.Sound ring2File(String name) {
        return sound(CorruptedAdventure.id("ambient/ring_2/" + name), SoundDefinition.SoundType.SOUND)
                .volume(0.8F)
                .attenuationDistance(24);
    }

    private static SoundDefinition.Sound caveSound(float pitch) {
        return sound(SoundEvents.AMBIENT_CAVE.value().getLocation(), SoundDefinition.SoundType.EVENT)
                .pitch(pitch)
                .volume(0.8F);
    }
}
