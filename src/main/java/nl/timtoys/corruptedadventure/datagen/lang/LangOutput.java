package nl.timtoys.corruptedadventure.datagen.lang;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * The part of a language provider the per-stream lang helpers write to, so each stream adds its keys in its own class
 * instead of in the shared providers. A duplicate key fails {@code runData}.
 */
public interface LangOutput {
    void add(String key, String value);

    void addBlock(Supplier<? extends Block> block, String name);

    void addItem(Supplier<? extends Item> item, String name);

    default void biome(ResourceKey<Biome> biome, String name) {
        add(biome.location().toLanguageKey("biome"), name);
    }
}
