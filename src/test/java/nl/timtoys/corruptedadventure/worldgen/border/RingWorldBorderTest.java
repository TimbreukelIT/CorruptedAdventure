package nl.timtoys.corruptedadventure.worldgen.border;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import nl.timtoys.corruptedadventure.config.CommonConfig;
import nl.timtoys.corruptedadventure.config.RingSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RingWorldBorderTest {
    private static final double SCALE = 4.0;

    private WorldBorder overworld;
    private WorldBorder nether;
    private int sizeChanges;

    /** Wired like vanilla's createLevels (delegate first, center scaled by the level), plus our listener after it. */
    @BeforeEach
    void setUp() {
        overworld = new WorldBorder();
        nether = new WorldBorder() {
            @Override
            public double getCenterX() {
                return super.getCenterX() / SCALE;
            }

            @Override
            public double getCenterZ() {
                return super.getCenterZ() / SCALE;
            }
        };
        overworld.addListener(new BorderChangeListener.DelegateBorderChangeListener(nether));
        ScaledBorderListener listener = new ScaledBorderListener(overworld, nether, SCALE, () -> sizeChanges++);
        overworld.addListener(listener);
        listener.derive();
    }

    @Test
    void initialSizeIsTheVoidEdgeDiameter() {
        RingSettings settings = new RingSettings(CommonConfig.DEFAULT_OUTER_RADII, 10200, 96, 128,
                CommonConfig.DEFAULT_TRANSITION_WIDTH_OVERRIDES);
        assertEquals(20400.0, RingWorldBorder.initialSize(settings));
    }

    @Test
    void onlyAnUntouchedBorderIsVanillaDefault() {
        assertTrue(RingWorldBorder.isVanillaDefault(new WorldBorder()));
        WorldBorder changed = new WorldBorder();
        changed.setSize(30000.0);
        assertFalse(RingWorldBorder.isVanillaDefault(changed));
        WorldBorder moved = new WorldBorder();
        moved.setCenter(5000.5, 5000.5);
        assertFalse(RingWorldBorder.isVanillaDefault(moved));
    }

    @Test
    void netherFollowsTheOverworldSizeDividedByTheScale() {
        assertEquals(WorldBorder.DEFAULT_SETTINGS.getSize() / SCALE, nether.getSize());
        overworld.setSize(20000.0);
        assertEquals(5000.0, nether.getSize());
        assertEquals(2, sizeChanges);
    }

    @Test
    void netherLerpsToTheScaledTarget() {
        overworld.setSize(20000.0);
        overworld.lerpSizeBetween(20000.0, 8000.0, 60_000L);
        assertEquals(2000.0, nether.getLerpTarget());
        assertTrue(nether.getLerpRemainingTime() > 0L);
        assertEquals(overworld.getSize() / SCALE, nether.getSize(), 1.0);
    }

    @Test
    void clientViewKeepsTheUnscaledCenter() {
        overworld.setSize(20000.0);
        overworld.setCenter(400.0, -800.0);
        assertEquals(100.0, nether.getCenterX());
        assertEquals(-200.0, nether.getCenterZ());

        WorldBorder view = ScaledBorderListener.clientView(overworld, nether);
        assertEquals(400.0, view.getCenterX());
        assertEquals(-800.0, view.getCenterZ());
        assertEquals(5000.0, view.getSize());
        assertEquals(nether.getWarningBlocks(), view.getWarningBlocks());
    }
}
