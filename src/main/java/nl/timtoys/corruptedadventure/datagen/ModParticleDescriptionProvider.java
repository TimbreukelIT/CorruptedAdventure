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
        // verdant_falls (ring 4 stream A): light 8×8 sprites waterfall_mist_0..3 from stream T, tinted by the particle.
        spriteSet(ModParticleTypes.WATERFALL_MIST.get(), CorruptedAdventure.id("waterfall_mist"), 4, false);
        // Ring 4 and Nether ring 4 (package 80-0): 8×8 sprites <name>_0..3 from stream T (71a).
        spriteSet(ModParticleTypes.WILL_O_WISP.get(), CorruptedAdventure.id("will_o_wisp"), 4, false);
        spriteSet(ModParticleTypes.BLOSSOM.get(), CorruptedAdventure.id("blossom"), 4, false);
        spriteSet(ModParticleTypes.FIREFLY.get(), CorruptedAdventure.id("firefly"), 4, false);
        spriteSet(ModParticleTypes.SWAMP_GAS.get(), CorruptedAdventure.id("swamp_gas"), 4, false);
        spriteSet(ModParticleTypes.TOXIC_BUBBLE.get(), CorruptedAdventure.id("toxic_bubble"), 4, false);
        spriteSet(ModParticleTypes.UPDRAFT_BUBBLE.get(), CorruptedAdventure.id("updraft_bubble"), 4, false);
        spriteSet(ModParticleTypes.GLITCH_SPECK.get(), CorruptedAdventure.id("glitch_speck"), 4, false);
        spriteSet(ModParticleTypes.BONE_DUST.get(), CorruptedAdventure.id("bone_dust"), 4, false);
        // ring 5 — A (package 98): 8×8 sprites from stream T (99a).
        spriteSet(ModParticleTypes.VOID_MOTE.get(), CorruptedAdventure.id("void_mote"), 4, false);
        spriteSet(ModParticleTypes.GLITCH_PIXEL.get(), CorruptedAdventure.id("glitch_pixel"), 4, false);
        spriteSet(ModParticleTypes.REVERSE_RAINDROP.get(), CorruptedAdventure.id("reverse_raindrop"), 2, false);
    }
}
