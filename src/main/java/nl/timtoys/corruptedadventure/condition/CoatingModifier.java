package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimPattern;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.item.coating.Coating;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coatings slow their condition's rising meter (fase-2-items.md §4): every worn armor piece whose trim is the coating of
 * the condition takes {@code [conditions.coating] reductionPerPiece} (0.1875) off a rising rate, linearly: 1 piece
 * 0.81×, 2 pieces 0.63×, 3 pieces 0.44×, 4 pieces 0.25×. A falling or still meter is left alone. Stage
 * {@link RateModifier.Stage#GEAR}, for every condition; with leather against the cold ({@link LeatherColdModifier},
 * the same stage) the two factors multiply, so their order does not matter.
 */
final class CoatingModifier implements RateModifier {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (rate <= 0.0F) {
            return rate;
        }
        Optional<Coating> coating = Coating.of(condition);
        if (coating.isEmpty()) {
            return rate;
        }
        return rate * factor(coatedPieces(armor(context.player()), coating.get().pattern()),
                ServerConfig.COATING.reductionPerPiece.get().floatValue());
    }

    private static List<ItemStack> armor(Player player) {
        List<ItemStack> armor = new ArrayList<>(ARMOR.length);
        for (EquipmentSlot slot : ARMOR) {
            armor.add(player.getItemBySlot(slot));
        }
        return armor;
    }

    /** The pieces whose trim has {@code pattern}; a piece with another trim or none does not count. */
    static int coatedPieces(List<ItemStack> armor, ResourceKey<TrimPattern> pattern) {
        int pieces = 0;
        for (ItemStack stack : armor) {
            ArmorTrim trim = stack.get(DataComponents.TRIM);
            if (trim != null && trim.pattern().is(pattern)) {
                pieces++;
            }
        }
        return pieces;
    }

    /** {@code 1 - pieces × reduction}, never below 0 (the config caps the reduction at 0.24: four pieces leave 0.04). */
    static float factor(int pieces, float reductionPerPiece) {
        return Math.max(0.0F, 1.0F - pieces * reductionPerPiece);
    }
}
