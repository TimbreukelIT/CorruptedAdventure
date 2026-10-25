package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimMaterial;
import nl.timtoys.corruptedadventure.CorruptedAdventure;

/**
 * The materials a coating is applied with: each is its own {@code trim_material} (fase-2-items.md §4.4), so vanilla's
 * smithing trim recipe can put it on armor and render it. None is in {@code #minecraft:trim_materials}: only the coating
 * recipes take them, never a vanilla template. The item model index borrows the vanilla material of the nearest colour,
 * so the existing armor item models show a trim overlay (items show the material, not the pattern).
 */
public enum CoatingMaterial {
    /** Placeholder for the dehydration coating: the cut turquoise gem (golf 2 decides the real material). */
    TURQUOISE("turquoise", "item", "turquoise", 0x4FC6B5, 0.7F),
    DEW_CRYSTAL("dew_crystal", "item", "dew_crystal", 0x8BC4D6, 0.8F),
    /** Placeholder for the cold coating (golf 4 decides the real material). */
    FIRE_CRYSTAL("fire_crystal", "item", "fire_crystal", 0xE07A2A, 0.5F),
    PRIMORDIAL_ICE("primordial_ice", "block", "primordial_ice", 0xBAD8F0, 0.2F),
    WHITE_CORE("white_core", "item", "white_core", 0xE6E4F2, 0.1F);

    private final String name;
    private final ResourceLocation itemId;
    private final String descriptionId;
    private final int color;
    private final float itemModelIndex;

    /**
     * By id rather than through {@code ModItems}: a template's tooltip is built while the items register, before the
     * block items exist (primordial ice).
     */
    CoatingMaterial(String name, String kind, String item, int color, float itemModelIndex) {
        this.name = name;
        this.itemId = CorruptedAdventure.id(item);
        this.descriptionId = Util.makeDescriptionId(kind, itemId);
        this.color = color;
        this.itemModelIndex = itemModelIndex;
    }

    public ResourceKey<TrimMaterial> key() {
        return ResourceKey.create(Registries.TRIM_MATERIAL, CorruptedAdventure.id(name));
    }

    /**
     * The suffix of the armor trim sprites ({@code <pattern>_<assetName>}) and the name of the palette texture
     * {@code textures/trims/color_palettes/<assetName>.png}.
     */
    public String assetName() {
        return name;
    }

    public ResourceLocation palette() {
        return CorruptedAdventure.id("trims/color_palettes/" + name);
    }

    /** The material item; only once the items have registered. */
    public Item item() {
        return BuiltInRegistries.ITEM.getOptional(itemId).orElseThrow(() -> new IllegalStateException("no item " + itemId));
    }

    /** The translation key of the material item (its name). */
    public String descriptionId() {
        return descriptionId;
    }

    /** RGB of the material's name in the tooltip. */
    public int color() {
        return color;
    }

    public float itemModelIndex() {
        return itemModelIndex;
    }
}
