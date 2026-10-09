package nl.timtoys.corruptedadventure.condition;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import nl.timtoys.corruptedadventure.config.ServerConfig;

/**
 * Leather slows the rising cold: every worn piece in vanilla's {@code #freeze_immune_wearables} takes
 * {@code leatherReductionPerPiece} (15 % by default, full leather 60 %) off a rising cold rate; a falling rate stays.
 * Stage {@link RateModifier.Stage#GEAR}. Leather slows the cold but never makes immune; vanilla would make one piece
 * enough, which is why the freezing at the top threshold is driven by {@link ColdFreeze}, not by vanilla.
 */
final class LeatherColdModifier implements RateModifier {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @Override
    public float modify(Condition condition, ConditionContext context, float rate) {
        if (!ColdRule.isRisingCold(condition, rate)) {
            return rate;
        }
        return rate * factor(leatherPieces(context.player()),
                ServerConfig.COLD.leatherReductionPerPiece.get().floatValue());
    }

    static int leatherPieces(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (player.getItemBySlot(slot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                pieces++;
            }
        }
        return pieces;
    }

    /** {@code 1 - pieces × reduction}, never below 0 (the config caps the reduction at 0.2: four pieces leave 0.2). */
    static float factor(int pieces, float reductionPerPiece) {
        return Math.max(0.0F, 1.0F - pieces * reductionPerPiece);
    }
}
