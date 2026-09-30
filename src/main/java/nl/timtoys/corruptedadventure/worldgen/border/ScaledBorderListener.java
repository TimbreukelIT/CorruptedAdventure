package nl.timtoys.corruptedadventure.worldgen.border;

import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;

/**
 * Keeps a scaled dimension's border at the overworld border's size divided by the dimension's
 * {@code coordinate_scale}, so it lines up with the overworld border through portals.
 * <p>
 * Vanilla only scales the center (in {@code Level}); its delegate listener copies the size 1:1. This listener must be
 * added after that delegate, so it overrides the unscaled size. Center, warnings and damage stay with the delegate.
 */
final class ScaledBorderListener implements BorderChangeListener {
    private final WorldBorder overworld;
    private final WorldBorder scaled;
    private final double scale;
    private final Runnable onSizeChanged;

    ScaledBorderListener(WorldBorder overworld, WorldBorder scaled, double scale, Runnable onSizeChanged) {
        this.overworld = overworld;
        this.scaled = scaled;
        this.scale = scale;
        this.onSizeChanged = onSizeChanged;
    }

    /** Copies the overworld's current size (or running lerp) to the scaled border, divided by the scale. */
    void derive() {
        long lerpTime = overworld.getLerpRemainingTime();
        if (lerpTime > 0L) {
            scaled.lerpSizeBetween(overworld.getSize() / scale, overworld.getLerpTarget() / scale, lerpTime);
        } else {
            scaled.setSize(overworld.getSize() / scale);
        }
        onSizeChanged.run();
    }

    /**
     * The scaled border as its client expects it: the client's level divides the center by the scale itself, so the
     * center must be the unscaled overworld center (like vanilla, which sends the overworld border to every dimension).
     */
    static WorldBorder clientView(WorldBorder overworld, WorldBorder scaled) {
        WorldBorder view = new WorldBorder();
        view.setCenter(overworld.getCenterX(), overworld.getCenterZ());
        long lerpTime = scaled.getLerpRemainingTime();
        if (lerpTime > 0L) {
            view.lerpSizeBetween(scaled.getSize(), scaled.getLerpTarget(), lerpTime);
        } else {
            view.setSize(scaled.getSize());
        }
        view.setAbsoluteMaxSize(scaled.getAbsoluteMaxSize());
        view.setWarningBlocks(scaled.getWarningBlocks());
        view.setWarningTime(scaled.getWarningTime());
        return view;
    }

    @Override
    public void onBorderSizeSet(WorldBorder border, double size) {
        derive();
    }

    @Override
    public void onBorderSizeLerping(WorldBorder border, double oldSize, double newSize, long time) {
        derive();
    }

    @Override
    public void onBorderCenterSet(WorldBorder border, double x, double z) {
    }

    @Override
    public void onBorderSetWarningTime(WorldBorder border, int warningTime) {
    }

    @Override
    public void onBorderSetWarningBlocks(WorldBorder border, int warningBlocks) {
    }

    @Override
    public void onBorderSetDamagePerBlock(WorldBorder border, double damagePerBlock) {
    }

    @Override
    public void onBorderSetDamageSafeZOne(WorldBorder border, double damageSafeZone) {
    }
}
