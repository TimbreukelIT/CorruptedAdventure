package nl.timtoys.corruptedadventure.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.feature.HangingStrandDecorator;

/** Tree decorator types with their own code, used by the tree features from datagen. */
public final class ModTreeDecoratorTypes {
    private static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATOR_TYPES =
            DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, CorruptedAdventure.MOD_ID);

    // ring 4 — decor rollout group A
    /** {@code corruptedadventure:hanging_strand} (moss strands and glowing roots under the giant mangroves of glowroot_swamp) */
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<HangingStrandDecorator>> HANGING_STRAND =
            TREE_DECORATOR_TYPES.register("hanging_strand", () -> new TreeDecoratorType<>(HangingStrandDecorator.CODEC));

    private ModTreeDecoratorTypes() {
    }

    public static void register(IEventBus modBus) {
        TREE_DECORATOR_TYPES.register(modBus);
    }
}
