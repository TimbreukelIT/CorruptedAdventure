package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModPlacedFeatures;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Datagen bootstrap of the biome modifiers that add ring 1's own features (horror features in the overworld, soul
 * crystal ore in the Nether) to the vanilla copies. The copies are built by vanilla's own biome code, so features go
 * in through modifiers rather than into the copies. Each modifier appends to the end of its step, in the same order in
 * every biome: no feature order cycle.
 */
public final class Ring1BiomeModifiers {
    public static final ResourceKey<BiomeModifier> ADD_BARE_PATCH = key("add_bare_patch");
    public static final ResourceKey<BiomeModifier> ADD_UNNATURAL_TUNNEL = key("add_unnatural_tunnel");
    public static final ResourceKey<BiomeModifier> ADD_SOUL_CRYSTAL_ORE = key("add_soul_crystal_ore");
    public static final ResourceKey<BiomeModifier> ADD_CRIMSON_SOUL_CRYSTAL_ORE = key("add_crimson_soul_crystal_ore");

    private Ring1BiomeModifiers() {
    }

    public static void bootstrap(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> features = context.lookup(Registries.PLACED_FEATURE);
        context.register(ADD_BARE_PATCH, addFeature(biomes, features, ModTags.Biomes.HAS_BARE_PATCH,
                ModPlacedFeatures.BARE_PATCH, GenerationStep.Decoration.VEGETAL_DECORATION));
        // Last step: ores, springs and vegetation are in place, so springs are seen and the tunnel stays bare.
        context.register(ADD_UNNATURAL_TUNNEL, addFeature(biomes, features, ModTags.Biomes.HAS_UNNATURAL_TUNNEL,
                ModPlacedFeatures.UNNATURAL_TUNNEL, GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        // Same step as vanilla's Nether ores.
        context.register(ADD_SOUL_CRYSTAL_ORE, addFeature(biomes, features, ModTags.Biomes.IS_NETHER_RING_1,
                ModPlacedFeatures.SOUL_CRYSTAL_ORE, GenerationStep.Decoration.UNDERGROUND_DECORATION));
        // Before the fungi (vegetal decoration), so the ore floor stays bare. Its order relative to the veins does not matter.
        context.register(ADD_CRIMSON_SOUL_CRYSTAL_ORE, addFeature(biomes, features, ModTags.Biomes.HAS_CRIMSON_SOUL_CRYSTAL_ORE,
                ModPlacedFeatures.CRIMSON_SOUL_CRYSTAL_ORE, GenerationStep.Decoration.UNDERGROUND_DECORATION));
    }

    private static BiomeModifier addFeature(HolderGetter<Biome> biomes, HolderGetter<PlacedFeature> features, TagKey<Biome> tag,
                                            ResourceKey<PlacedFeature> feature, GenerationStep.Decoration step) {
        return new BiomeModifiers.AddFeaturesBiomeModifier(biomes.getOrThrow(tag), HolderSet.direct(features.getOrThrow(feature)), step);
    }

    private static ResourceKey<BiomeModifier> key(String path) {
        return ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, CorruptedAdventure.id(path));
    }
}
