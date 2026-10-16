package nl.timtoys.corruptedadventure.block;

import net.minecraft.world.level.block.grower.TreeGrower;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.registry.ModConfiguredFeatures;

import java.util.Optional;

/**
 * Tree growers of our saplings. A {@link TreeGrower} adds itself to vanilla's global name map (used by
 * {@link TreeGrower#CODEC}), so the name carries our namespace.
 */
public final class ModTreeGrowers {
    /**
     * Grows {@link ModConfiguredFeatures#PALM_TREE}. While that feature does not exist (before change 23),
     * {@code TreeGrower.growTree} finds no holder and returns false: the sapling stays, bonemeal is used up.
     */
    public static final TreeGrower PALM = new TreeGrower(CorruptedAdventure.id("palm").toString(),
            Optional.empty(), Optional.of(ModConfiguredFeatures.PALM_TREE), Optional.empty());

    /**
     * Grows {@link ModConfiguredFeatures#MOONPETAL_TREE}, an ordinary cascade tree (not the world tree). Until the
     * feature exists (75) the sapling stays, like the palm before change 23.
     */
    public static final TreeGrower MOONPETAL = new TreeGrower(CorruptedAdventure.id("moonpetal").toString(),
            Optional.empty(), Optional.of(ModConfiguredFeatures.MOONPETAL_TREE), Optional.empty());

    /**
     * Grows {@link ModConfiguredFeatures#VERDANT_TREE_FERN}, only the tree fern (F11 = A). Until the feature exists (75b)
     * the sapling stays.
     */
    public static final TreeGrower VERDANT = new TreeGrower(CorruptedAdventure.id("verdant").toString(),
            Optional.empty(), Optional.of(ModConfiguredFeatures.VERDANT_TREE_FERN), Optional.empty());

    private ModTreeGrowers() {
    }
}
