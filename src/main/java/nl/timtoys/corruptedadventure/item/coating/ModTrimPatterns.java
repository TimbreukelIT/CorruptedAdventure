package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.Util;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.armortrim.TrimPattern;

/**
 * The {@code trim_pattern} entries of the coatings (datapack registry, written by {@code runData}): one per
 * {@link Coating}, its asset id = its key, no decal (like vanilla's patterns).
 */
public final class ModTrimPatterns {
    private ModTrimPatterns() {
    }

    public static void bootstrap(BootstrapContext<TrimPattern> context) {
        for (Coating coating : Coating.values()) {
            context.register(coating.pattern(), new TrimPattern(coating.pattern().location(),
                    BuiltInRegistries.ITEM.wrapAsHolder(coating.template()), description(coating), false));
        }
    }

    /** {@code trim_pattern.corruptedadventure.<condition>_coating}: the tooltip line and the template's title. */
    public static MutableComponent description(Coating coating) {
        return Component.translatable(Util.makeDescriptionId("trim_pattern", coating.pattern().location()));
    }
}
