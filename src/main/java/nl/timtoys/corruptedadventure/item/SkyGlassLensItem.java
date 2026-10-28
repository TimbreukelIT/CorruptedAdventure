package nl.timtoys.corruptedadventure.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.SpyglassItem;

/**
 * The sky glass lens (phase 2 golf 5, §7.2): a spyglass that shows the hidden glitches while you look through it. A
 * {@link SpyglassItem}, so vanilla does the zoom, the scope, the hidden hand, the slower mouse, the arm pose and the
 * sounds itself (NeoForge's {@code Player.isScoping} asks the item's spyglass action, which this inherits). The server
 * scans round a looking player ({@code item/lens/LensWatch}); the client draws the outlines.
 */
public class SkyGlassLensItem extends SpyglassItem {
    public SkyGlassLensItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Whether {@code entity} is looking through a sky glass lens right now. */
    public static boolean lookingThrough(LivingEntity entity) {
        return entity.isUsingItem() && entity.getUseItem().getItem() instanceof SkyGlassLensItem;
    }
}
