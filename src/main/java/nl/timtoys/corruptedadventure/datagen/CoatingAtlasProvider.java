package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.client.renderer.texture.atlas.sources.PalettedPermutations;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SpriteSourceProvider;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.item.coating.Coating;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Adds the coating trims to vanilla's {@code minecraft:armor_trims} atlas ({@code assets/minecraft/atlases/armor_trims.json};
 * the atlas sources of all packs add up). One paletted source per coating with only its own material, because a
 * coating recipe takes no other. Our own palette key ({@code coating_palette}, 8 greys), so the pattern textures need no
 * vanilla pixels.
 */
final class CoatingAtlasProvider extends SpriteSourceProvider {
    static final ResourceLocation ARMOR_TRIMS_ATLAS = ResourceLocation.withDefaultNamespace("armor_trims");
    static final ResourceLocation PALETTE_KEY = CorruptedAdventure.id("trims/color_palettes/coating_palette");

    CoatingAtlasProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper files) {
        super(output, lookup, CorruptedAdventure.MOD_ID, files);
    }

    @Override
    protected void gather() {
        SourceList atlas = atlas(ARMOR_TRIMS_ATLAS);
        for (Coating coating : Coating.values()) {
            ResourceLocation texture = coating.pattern().location().withPrefix("trims/models/armor/");
            atlas.addSource(new PalettedPermutations(List.of(texture, texture.withSuffix("_leggings")), PALETTE_KEY,
                    Map.of(coating.material().assetName(), coating.material().palette())));
        }
    }
}
