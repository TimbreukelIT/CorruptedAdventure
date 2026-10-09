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
        // Ring 3 weather (package 45): light grey sprites from stream T (ash_flake_0..3, ember_0..3), tinted by the particle.
        spriteSet(ModParticleTypes.ASH_FLAKE.get(), CorruptedAdventure.id("ash_flake"), 4, false);
        spriteSet(ModParticleTypes.EMBER.get(), CorruptedAdventure.id("ember"), 4, false);
        // Package 50-0: steam and ice crystals, light sprites from stream T (57a), tinted by the particle.
        spriteSet(ModParticleTypes.STEAM_PUFF.get(), CorruptedAdventure.id("steam_puff"), 4, false);
        spriteSet(ModParticleTypes.ICE_GLINT.get(), CorruptedAdventure.id("ice_glint"), 4, false);
        // Package 50: the ambient variants of the ring 3 biomes share their base type's sprites.
        spriteSet(ModParticleTypes.DRIFTING_ASH.get(), CorruptedAdventure.id("ash_flake"), 4, false);
        spriteSet(ModParticleTypes.RISING_EMBER.get(), CorruptedAdventure.id("ember"), 4, false);
        spriteSet(ModParticleTypes.SULFUR_STEAM.get(), CorruptedAdventure.id("steam_puff"), 4, false);
    }
}
