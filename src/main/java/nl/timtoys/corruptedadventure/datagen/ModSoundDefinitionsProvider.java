package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinition;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModSoundEvents;

/**
 * {@code sounds.json}. Our sounds reuse vanilla's samples through {@code event} entries (pitch and volume multiply
 * those of the vanilla event), so there are no audio files yet; own {@code .ogg}s can replace them here later.
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
    }

    private static SoundDefinition.Sound caveSound(float pitch) {
        return sound(SoundEvents.AMBIENT_CAVE.value().getLocation(), SoundDefinition.SoundType.EVENT)
                .pitch(pitch)
                .volume(0.8F);
    }
}
