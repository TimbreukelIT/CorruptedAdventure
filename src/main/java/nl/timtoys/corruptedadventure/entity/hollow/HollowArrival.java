package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareSpots;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * How The Hollow comes (fase-4-nightmares-concept.md, De Holle "De komst"), for the director: a hairline crack in the
 * air behind the target during the omens ({@link HollowRift.Kind#DIRECTED}, its omens run in the rift itself), then the
 * rift tears open, shows the two white eyes, and The Hollow, already waiting unseen inside, grips the edge and pulls
 * itself through. The rift closes and leaves its scar. Cancelled omens close the crack without a scar. While it roams,
 * a hairline crack hangs for a while where it passed ({@link HollowRift.Kind#TRACE}), only in loaded chunks.
 */
public final class HollowArrival implements NightmareArrival {
    /** Blocks behind the target where the rift opens. */
    private static final double DISTANCE = 9.0;
    /** How far from the chosen place an existing omen rift still counts as this arrival's. */
    private static final double FIND_RADIUS = 4.0;

    @Override
    public BlockPos place(ServerLevel level, ServerPlayer target) {
        Optional<Vec3> spot = NightmareSpots.behind(level, ModEntityTypes.THE_HOLLOW.get(), target, DISTANCE);
        return spot.map(BlockPos::containing).orElse(target.blockPosition());
    }

    @Override
    public int omenTicks() {
        return ServerConfig.HOLLOW.omenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        // The crack itself plays the omens; here it is only made sure it is there (also after a reload).
        if (tick % 20 == 0 && find(level, place) == null && level.hasChunkAt(place)) {
            Vec3 feet = Vec3.atBottomCenterOf(place);
            HollowRift.directed(level, feet, HollowRiftStepGoal.yawToward(feet, target.position()), omenTicks() - tick);
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        if (!level.hasChunkAt(place)) {
            return null;
        }
        Vec3 feet = Vec3.atBottomCenterOf(place);
        HollowRift rift = find(level, place);
        if (rift == null) {
            rift = HollowRift.directed(level, feet, HollowRiftStepGoal.yawToward(feet, target.position()), 1);
        }
        Entity created = type.create(level);
        if (!(created instanceof TheHollow hollow)) {
            return null;
        }
        hollow.moveTo(rift.getX(), rift.getY(), rift.getZ(), rift.getYRot(), 0.0F);
        hollow.setYHeadRot(rift.getYRot());
        hollow.setYBodyRot(rift.getYRot());
        if (!level.addFreshEntity(hollow)) {
            return null;
        }
        hollow.awaitRift(HollowRift.ticksUntilEmerging());
        hollow.setTarget(target);
        rift.open();
        return hollow;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        HollowRift rift = find(level, place);
        if (rift != null) {
            rift.close(false);
        }
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        if (level.hasChunkAt(to) && level.getRandom().nextInt(4) == 0) {
            Vec3 feet = Vec3.atBottomCenterOf(to);
            HollowRift.trace(level, feet, level.getRandom().nextFloat() * 360.0F);
        }
    }

    /** {@code /summon}'s arrival: The Hollow comes through the rift that runs it, facing out. */
    static void emergeFrom(ServerLevel level, HollowRift rift) {
        TheHollow hollow = ModEntityTypes.THE_HOLLOW.get().create(level);
        if (hollow == null) {
            return;
        }
        hollow.moveTo(rift.getX(), rift.getY(), rift.getZ(), rift.getYRot(), 0.0F);
        hollow.setYHeadRot(rift.getYRot());
        hollow.setYBodyRot(rift.getYRot());
        level.addFreshEntity(hollow);
        hollow.startEmerging();
    }

    @Nullable
    private static HollowRift find(ServerLevel level, BlockPos place) {
        List<HollowRift> rifts = level.getEntitiesOfClass(HollowRift.class, new AABB(place).inflate(FIND_RADIUS),
                rift -> rift.kind() == HollowRift.Kind.DIRECTED && rift.phase() == RiftPhase.OMEN);
        return rifts.isEmpty() ? null : rifts.get(0);
    }
}
