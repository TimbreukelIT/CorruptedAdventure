package nl.timtoys.corruptedadventure.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModItems;

import java.util.function.BiConsumer;

/**
 * The Nightmares' drops on their defeat (phase 4, "Fase 4-sparring alle Nightmares": XP plus the material of their
 * ring; unique items later, backlog). The experience is not here but in each Nightmare's config ({@code experience}); a
 * retreat drops nothing (the entity is discarded, no loot table). One line per Nightmare in {@link #generate}, built with
 * {@link #table} and {@link #drop}. Registered beside the block loot ({@link ModBlockLootProvider#create}).
 */
final class NightmareLoot implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    NightmareLoot(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        // Ring 1: its shattered soul (soul crystals) and a few diamonds.
        add(output, ModEntityTypes.THE_HOLLOW.get(), table(
                drop(ModItems.SOUL_CRYSTAL.get(), 6, 10, 2),
                drop(Items.DIAMOND, 2, 4, 0)));
        // Ring 2: sunstone, the ring's material (raw, as it comes from the ore).
        add(output, ModEntityTypes.THE_LAYERED.get(), table(
                drop(ModItems.RAW_SUNSTONE.get(), 4, 7, 2)));
    }

    private static void add(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output, EntityType<?> type,
                            LootTable.Builder table) {
        output.accept(type.getDefaultLootTable(), table);
    }

    /** One guaranteed pool per drop. */
    private static LootTable.Builder table(Drop... drops) {
        LootTable.Builder table = LootTable.lootTable();
        for (Drop drop : drops) {
            table.withPool(drop.pool);
        }
        return table;
    }

    /** {@code min}–{@code max} of an item, plus 0–{@code lootingBonus} per level of Looting (0: none). */
    private Drop drop(ItemLike item, int min, int max, int lootingBonus) {
        LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
        if (lootingBonus > 0) {
            entry.apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, lootingBonus)));
        }
        return new Drop(LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F)).add(entry));
    }

    private record Drop(LootPool.Builder pool) {
    }
}
