package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/** {@code particles/<name>.json}: which sprites a particle type uses (client asset). */
final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper files) {
        super(output, files);
    }

    @Override
    protected void addDescriptions() {
        // Vanilla's generic_0 … generic_7 (small to large dots), tinted sand by the particle; own grains in change 37.
        spriteSet(ModParticleTypes.SAND_GUST.get(), ResourceLocation.withDefaultNamespace("generic"), 8, false);
    }
}
