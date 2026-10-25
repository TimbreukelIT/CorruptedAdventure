package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimPattern;

/** Test helper: our trim patterns and materials in a lookup of their own, built from the same bootstraps as runData. */
public final class CoatingTrims {
    public static final HolderLookup.Provider LOOKUP = new RegistrySetBuilder()
            .add(Registries.TRIM_PATTERN, ModTrimPatterns::bootstrap)
            .add(Registries.TRIM_MATERIAL, ModTrimMaterials::bootstrap)
            .build(RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

    private CoatingTrims() {
    }

    public static Holder<TrimPattern> pattern(Coating coating) {
        return LOOKUP.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(coating.pattern());
    }

    public static Holder<TrimMaterial> material(Coating coating) {
        return LOOKUP.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(coating.material().key());
    }

    /** {@code item} with the trim of {@code coating} (its own pattern and material). */
    public static ItemStack coated(Item item, Coating coating) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.TRIM, new ArmorTrim(material(coating), pattern(coating)));
        return stack;
    }
}
