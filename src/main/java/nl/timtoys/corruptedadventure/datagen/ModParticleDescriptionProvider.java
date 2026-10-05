package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;

/** {@code particles/<name>.json}: which sprites a particle type uses (client asset). */
final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper files) {
        super(output, files);
    }

    @Override
    protected void addDescriptions() {
        // Own grains (change 37): light grey sprites sand_gust_0 … sand_gust_3 (small to large), tinted sand by the particle.
        spriteSet(ModParticleTypes.SAND_GUST.get(), ResourceLocation.fromNamespaceAndPath(CorruptedAdventure.MOD_ID, "sand_gust"), 4, false);
    }
}
