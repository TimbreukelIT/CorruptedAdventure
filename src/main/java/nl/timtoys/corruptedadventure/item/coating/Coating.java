package nl.timtoys.corruptedadventure.item.coating;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.armortrim.TrimPattern;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The six coatings, one per condition (fase-2-items.md §1.3, §4): each is a vanilla armor trim pattern
 * ({@code corruptedadventure:<name>_coating}) with its own template item and material. A coated armor piece slows the
 * rising meter of its condition ({@code CoatingModifier}); the trim also renders it and names it in the tooltip.
 */
public enum Coating {
    DEHYDRATION(Condition.DEHYDRATION, CoatingMaterial.TURQUOISE, () -> ModItems.DEHYDRATION_COATING_SMITHING_TEMPLATE.get()),
    HEAT(Condition.HEAT, CoatingMaterial.DEW_CRYSTAL, () -> ModItems.HEAT_COATING_SMITHING_TEMPLATE.get()),
    COLD(Condition.COLD, CoatingMaterial.FIRE_CRYSTAL, () -> ModItems.COLD_COATING_SMITHING_TEMPLATE.get()),
    POISON(Condition.POISON, CoatingMaterial.PRIMORDIAL_ICE, () -> ModItems.POISON_COATING_SMITHING_TEMPLATE.get()),
    VOID(Condition.VOID, CoatingMaterial.WHITE_CORE, () -> ModItems.VOID_COATING_SMITHING_TEMPLATE.get()),
    SILENCE(Condition.SILENCE, CoatingMaterial.WHITE_CORE, () -> ModItems.SILENCE_COATING_SMITHING_TEMPLATE.get());

    private final Condition condition;
    private final CoatingMaterial material;
    private final Supplier<Item> template;
    private final ResourceKey<TrimPattern> pattern;

    Coating(Condition condition, CoatingMaterial material, Supplier<Item> template) {
        this.condition = condition;
        this.material = material;
        this.template = template;
        this.pattern = ResourceKey.create(Registries.TRIM_PATTERN, CorruptedAdventure.id(condition.getSerializedName() + "_coating"));
    }

    /** The coating against {@code condition}; empty for the debug condition. */
    public static Optional<Coating> of(Condition condition) {
        for (Coating coating : values()) {
            if (coating.condition == condition) {
                return Optional.of(coating);
            }
        }
        return Optional.empty();
    }

    public Condition condition() {
        return condition;
    }

    public CoatingMaterial material() {
        return material;
    }

    public Item template() {
        return template.get();
    }

    /** {@code corruptedadventure:<condition>_coating}; also the asset id of its textures. */
    public ResourceKey<TrimPattern> pattern() {
        return pattern;
    }

    /** {@code <condition>_coating}. */
    public String id() {
        return pattern.location().getPath();
    }
}
