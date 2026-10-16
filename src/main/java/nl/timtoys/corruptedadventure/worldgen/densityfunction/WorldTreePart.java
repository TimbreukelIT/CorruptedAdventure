package nl.timtoys.corruptedadventure.worldgen.densityfunction;

/**
 * What a block of a world tree of ring 4 is (74): the surface rule {@code corruptedadventure:world_tree} paints it
 * {@code moonpetal_log} (the trunk), {@code moonpetal_wood} (branches and roots: bark on every side) or persistent
 * {@code moonpetal_leaves} (the crown).
 */
public enum WorldTreePart {
    NONE,
    LOG,
    WOOD,
    LEAVES
}
