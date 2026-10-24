package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemDamageFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import nl.timtoys.corruptedadventure.worldgen.feature.AbandonedCampFeature;

import java.util.function.BiConsumer;

/**
 * The chest loot of ring 5 (94, V6-A): the abandoned camps' chest ({@link AbandonedCampFeature#LOOT}) holds a few everyday
 * things a player would carry (bread, torches, coal, string, a worn iron pickaxe). Vanilla items only; phase 3 may widen
 * it (backlog). Registered beside the block loot ({@link ModBlockLootProvider#create}).
 */
final class Ring5ChestLoot implements LootTableSubProvider {
    Ring5ChestLoot(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(AbandonedCampFeature.LOOT, LootTable.lootTable().withPool(LootPool.lootPool()
                .setRolls(UniformGenerator.between(3.0F, 5.0F))
                .add(LootItem.lootTableItem(Items.BREAD).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                .add(LootItem.lootTableItem(Items.TORCH).setWeight(10).apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 6.0F))))
                .add(LootItem.lootTableItem(Items.COAL).setWeight(8).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                .add(LootItem.lootTableItem(Items.STRING).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F))))
                .add(LootItem.lootTableItem(Items.STICK).setWeight(5).apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 4.0F))))
                .add(LootItem.lootTableItem(Items.IRON_PICKAXE).setWeight(2).apply(SetItemDamageFunction.setDamage(UniformGenerator.between(0.1F, 0.4F))))));
    }
}
