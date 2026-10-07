package nl.timtoys.corruptedadventure.condition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForgeMod;
import nl.timtoys.corruptedadventure.ring.RingLayout;
import nl.timtoys.corruptedadventure.ring.RingPosition;

/**
 * What every condition rule may look at, built once per player per tick and shared by all conditions. Deliberately
 * minimal: derived checks are added by the first condition that needs them, so their meaning is decided there (sun,
 * sky and water by dehydration, change 27; roof and heat sources by heat, change 43); the ring weather is read by its own {@link RateModifier}, not through
 * this context. The derived checks are computed on every call.
 *
 * @param position the ring at the player's feet ({@link RingLayout#ringAt})
 * @param biome    the biome at the player's feet
 */
public record ConditionContext(ServerPlayer player, ServerLevel level, RingPosition position, Holder<Biome> biome) {
    /** Water this high above the feet (blocks) counts as submerged: above the waist, so a puddle does not count. */
    static final double SUBMERGED_WATER_HEIGHT = 1.0;

    public static ConditionContext of(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        return new ConditionContext(player, level,
                RingLayout.ringAt(level.dimension(), player.getX(), player.getZ()),
                level.getBiome(player.blockPosition()));
    }

    /** The sun is above the horizon ({@link Sun#isUp}); rain and thunder are ignored. */
    public boolean isSunUp() {
        return Sun.isUp(level.dimensionType(), level.getDayTime());
    }

    /**
     * Full sky light (15) at eye height: nothing above dims it. Glass lets it through; leaves, roofs and overhangs
     * (also one block in under an overhang, where the light comes in from the side at 14) do not.
     */
    public boolean canSeeSky() {
        return level.canSeeSky(BlockPos.containing(player.getEyePosition()));
    }

    /** Water reaches at least {@link #SUBMERGED_WATER_HEIGHT} above the feet; swimming counts. */
    public boolean isSubmerged() {
        return isSubmerged(player.getFluidTypeHeight(NeoForgeMod.WATER_TYPE.value()));
    }

    /**
     * A solid block or a fluid somewhere above the head: a roof, an overhang, a floating island, rock overhead
     * (underground), or water resting on glass. Leaves do not count (that is shade); glass does, but under glass
     * {@link #canSeeSky} is already true. Uses the {@code MOTION_BLOCKING_NO_LEAVES} heightmap, so it is exact and
     * costs one lookup.
     */
    public boolean isUnderRoof() {
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        return eye.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, eye.getX(), eye.getZ());
    }

    /** A block in {@code #corruptedadventure:heat_sources} within {@code radius} blocks of the feet or head. */
    public boolean isNearHeatSource(int radius) {
        return HeatSources.near(level, player.blockPosition(), radius);
    }

    static boolean isSubmerged(double waterHeight) {
        return waterHeight >= SUBMERGED_WATER_HEIGHT;
    }
}
