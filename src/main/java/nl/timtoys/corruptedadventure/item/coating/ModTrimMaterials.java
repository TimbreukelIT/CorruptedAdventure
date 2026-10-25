package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.armortrim.TrimMaterial;

import java.util.Map;

/** The {@code trim_material} entries of the coating materials (datapack registry, written by {@code runData}). */
public final class ModTrimMaterials {
    private ModTrimMaterials() {
    }

    public static void bootstrap(BootstrapContext<TrimMaterial> context) {
        for (CoatingMaterial material : CoatingMaterial.values()) {
            Component description = Component.translatable(Util.makeDescriptionId("trim_material", material.key().location()))
                    .withStyle(Style.EMPTY.withColor(material.color()));
            context.register(material.key(), new TrimMaterial(material.assetName(),
                    BuiltInRegistries.ITEM.wrapAsHolder(material.item()), material.itemModelIndex(), Map.of(), description));
        }
    }
}
