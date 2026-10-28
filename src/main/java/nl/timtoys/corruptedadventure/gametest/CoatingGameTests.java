package nl.timtoys.corruptedadventure.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.condition.Condition;
import nl.timtoys.corruptedadventure.condition.ConditionContext;
import nl.timtoys.corruptedadventure.condition.ConditionDefinitions;
import nl.timtoys.corruptedadventure.condition.RateEffect;
import nl.timtoys.corruptedadventure.condition.RateModifier;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.ArrayList;
import java.util.List;

/**
 * The six coatings on a worn set through the real recipes and the real {@link RateModifier} pipeline (fase-2-items.md
 * §4.4, §7.3): four pieces with a condition's own coating make its rising rate 0.25×, a falling rate and the other
 * conditions stay as they are, and with the condition's drink effect on top the factors multiply (0.125×). The void
 * coating takes sky glass, the silence coating the white core. Run like {@link TierGatingGameTests}.
 */
@GameTestHolder(CorruptedAdventure.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CoatingGameTests {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Item[] IRON = {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS};
    private static final float EPSILON = 1.0E-5F;

    private CoatingGameTests() {
    }

    @GameTest(template = "gametest/empty")
    public static void aFullCoatedSetSlowsItsOwnRisingConditionToAQuarter(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestPlayers.survival(helper);
        List<String> failures = new ArrayList<>();
        try {
            for (Coating coating : Coating.values()) {
                Condition condition = coating.condition();
                undress(player);
                float rising = rate(player, condition, 1.0F);
                float falling = rate(player, condition, -1.0F);
                List<Float> others = new ArrayList<>();
                for (Condition other : Condition.values()) {
                    others.add(rate(player, other, 1.0F));
                }
                if (rising <= 0.0F) {
                    failures.add(condition + ": no rising rate to slow (" + rising + ")");
                    continue;
                }
                for (int i = 0; i < SLOTS.length; i++) {
                    ItemStack piece = SmithingLadderGameTests.coat(level, new ItemStack(IRON[i]), coating);
                    if (!SmithingLadderGameTests.hasCoating(piece, coating)) {
                        failures.add(coating.id() + " does not go on " + IRON[i]);
                    }
                    player.setItemSlot(SLOTS[i], piece);
                }
                expect(failures, condition + " rising, 4 coated", rate(player, condition, 1.0F) / rising, 0.25F);
                expect(failures, condition + " falling, 4 coated", rate(player, condition, -1.0F), falling);
                for (Condition other : Condition.values()) {
                    if (other != condition) {
                        expect(failures, other + " under a full " + coating.id(), rate(player, other, 1.0F),
                                others.get(other.ordinal()));
                    }
                }
                for (RateEffect effect : RateEffect.values()) {
                    if (effect.slows(condition)) {
                        player.addEffect(new MobEffectInstance(effect.mobEffect(), 200));
                        expect(failures, condition + " with 4 coated and " + effect, rate(player, condition, 1.0F) / rising,
                                0.25F * effect.factor(condition));
                        player.removeEffect(effect.mobEffect());
                    }
                }
            }
        } finally {
            GameTestPlayers.remove(helper, player);
        }
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    @GameTest(template = "gametest/empty")
    public static void voidTakesSkyGlassAndSilenceTheWhiteCore(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> failures = new ArrayList<>();
        Item skyGlass = ModBlocks.SKY_GLASS.get().asItem();
        expectRecipe(level, failures, Coating.VOID, skyGlass, true);
        expectRecipe(level, failures, Coating.VOID, ModItems.WHITE_CORE.get(), false);
        expectRecipe(level, failures, Coating.SILENCE, ModItems.WHITE_CORE.get(), true);
        expectRecipe(level, failures, Coating.SILENCE, skyGlass, false);
        // A vanilla material on one of our templates does not work either.
        expectRecipe(level, failures, Coating.VOID, Items.DIAMOND, false);
        expectRecipe(level, failures, Coating.SILENCE, Items.QUARTZ, false);
        helper.assertTrue(failures.isEmpty(), String.join("; ", failures));
        helper.succeed();
    }

    private static void expectRecipe(ServerLevel level, List<String> failures, Coating coating, Item material, boolean works) {
        SmithingRecipeInput input = new SmithingRecipeInput(new ItemStack(coating.template()),
                new ItemStack(Items.IRON_HELMET), new ItemStack(material));
        boolean coated = level.getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
                .map(found -> SmithingLadderGameTests.hasCoating(found.value().assemble(input, level.registryAccess()), coating))
                .orElse(false);
        if (coated != works) {
            failures.add(coating.id() + " with " + material + ": " + (coated ? "coats" : "does not coat"));
        }
    }

    /** {@code base} through every registered modifier in pipeline order, as the condition ticker does. */
    private static float rate(ServerPlayer player, Condition condition, float base) {
        ConditionContext context = ConditionContext.of(player);
        float rate = base;
        for (RateModifier modifier : ConditionDefinitions.modifiers()) {
            rate = modifier.modify(condition, context, rate);
        }
        return rate;
    }

    private static void undress(ServerPlayer player) {
        for (EquipmentSlot slot : SLOTS) {
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
    }

    private static void expect(List<String> failures, String what, float actual, float expected) {
        if (Math.abs(actual - expected) > EPSILON) {
            failures.add(what + ": " + actual + ", expected " + expected);
        }
    }
}
