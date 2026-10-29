package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareSpots;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * How The Layered comes (fase-4-nightmares-concept.md, "Ring 2 — De Gelaagde", komst: the wall comes loose), for the
 * director. Is there a natural wall within {@code arrivalWallRadius} of the target ({@link LayeredWallSpot}), then that
 * is where it comes: during the omens pebbles and dust trickle down that wall, bones rattle somewhere in the canyon and
 * the animals flee; then a piece of the wall unfolds, and that is it ({@link TheLayered#startUnfolding}). Without a wall
 * it bursts up out of the ground {@code arrivalBurstDistance} behind the target, as from its burrow, after the same
 * omens and the heaving sand ({@link TheLayered#startBurst}). No block changes. While it roams it leaves claw marks in a
 * canyon wall now and then ({@link LayeredClawMarks}).
 */
public final class LayeredArrival implements NightmareArrival {
    /** Ticks it heaves under the ground after appearing, so every client tracks it before it bursts up. */
    private static final int BURST_DELAY_TICKS = 6;
    private static final int FLEE_EVERY = 20;
    private static final int OMEN_EVERY = 3;
    /** The last ticks of the omens: deep in the rock it grinds. */
    private static final int GRIND_TICKS = 60;
    private static final double RATTLE_MIN = 8.0;
    private static final double RATTLE_MAX = 16.0;
    /** Roughly once per this many ticks bones rattle (and once early on for sure). */
    private static final int RATTLE_ODDS = 70;
    private static final int FIRST_RATTLE = 30;
    /** Claw marks never appear this close to a player (they would pop up in sight). */
    private static final double TRACE_UNSEEN = 12.0;
    /** From its feet on a wall to the middle of the stone its back lies against: half its height up, half its width on. */
    private static final double FACE_UP = 1.25;
    private static final double FACE_OUT = 1.3;

    // The place chosen last and whether it is on a wall: omen() runs every tick. Recomputed from the place after a reload.
    @Nullable
    private BlockPos rememberedPlace;
    @Nullable
    private LayeredWallSpot.Spot rememberedWall;

    private static LayeredConfig settings() {
        return TheLayered.settings();
    }

    @Override
    public BlockPos place(ServerLevel level, ServerPlayer target) {
        EntityType<TheLayered> type = ModEntityTypes.THE_LAYERED.get();
        Optional<LayeredWallSpot.Spot> wall = LayeredWallSpot.find(level, type, target.position(),
                settings().arrivalWallRadius.get(), level.getRandom());
        BlockPos place = wall.map(LayeredWallSpot.Spot::pos).orElseGet(() -> NightmareSpots.behind(level, type, target,
                settings().arrivalBurstDistance.get()).map(BlockPos::containing).orElse(target.blockPosition()));
        rememberedPlace = place;
        rememberedWall = wall.orElse(null);
        return place;
    }

    @Override
    public int omenTicks() {
        return settings().arrivalOmenSeconds.get() * 20;
    }

    @Override
    public void omen(ServerLevel level, ServerPlayer target, BlockPos place, int tick) {
        if (!level.hasChunkAt(place)) {
            return;
        }
        RandomSource random = level.getRandom();
        LayeredWallSpot.Spot wall = wallAt(level, place);
        Vec3 at = wall != null ? wall.feet() : Vec3.atBottomCenterOf(place);
        int total = omenTicks();
        float progress = Mth.clamp((float) tick / Math.max(1, total), 0.0F, 1.0F);
        if (tick % FLEE_EVERY == 0) {
            fleeAnimals(level, at);
        }
        if (tick == FIRST_RATTLE || random.nextInt(RATTLE_ODDS) == 0) {
            LayeredEffects.boneRattle(level, somewhereInTheCanyon(target, random), random);
        }
        if (total - tick <= GRIND_TICKS && (total - tick) % 20 == 0) {
            LayeredEffects.grind(level, at);
        }
        if (wall != null) {
            if (tick % OMEN_EVERY == 0) {
                BlockState stone = level.getBlockState(wall.pos().above().relative(wall.wall(), 2));
                Vec3 face = at.add(0.0, FACE_UP, 0.0).add(Vec3.atLowerCornerOf(wall.wall().getNormal()).scale(FACE_OUT));
                LayeredEffects.wallOmen(level, face, wall.wall(), stone, progress, random);
            }
            return;
        }
        BlockState ground = level.getBlockState(place.below());
        int warn = settings().burrowWarnTicks.get();
        if (total - tick <= warn) {
            LayeredEffects.heave(level, at, ground, warn - (total - tick), warn);
        } else if (tick % OMEN_EVERY == 0) {
            LayeredEffects.groundOmen(level, at, ground, progress, random);
        }
    }

    @Override
    @Nullable
    public Nightmare appear(ServerLevel level, EntityType<? extends Nightmare> type, ServerPlayer target, BlockPos place,
                            float strength) {
        if (!level.hasChunkAt(place)) {
            return null;
        }
        Entity created = type.create(level);
        if (!(created instanceof TheLayered layered)) {
            return null;
        }
        // Checked again: the wall may have changed since the omens began.
        Optional<LayeredWallSpot.Spot> wall = LayeredWallSpot.at(level, type, place);
        if (wall.isPresent()) {
            Vec3 feet = wall.get().feet();
            layered.moveTo(feet.x, feet.y, feet.z, wall.get().wall().toYRot(), 0.0F);
            layered.startUnfolding(wall.get().wall(), settings().arrivalUnfoldTicks.get());
        } else {
            Optional<Vec3> feet = ground(level, layered, place)
                    .or(() -> NightmareSpots.behind(level, type, target, settings().arrivalBurstDistance.get()));
            if (feet.isEmpty()) {
                return null;
            }
            Vec3 at = feet.get();
            float yaw = (float) Math.toDegrees(Math.atan2(-(target.getX() - at.x), target.getZ() - at.z));
            layered.moveTo(at.x, at.y, at.z, yaw, 0.0F);
            layered.setYHeadRot(yaw);
            layered.setYBodyRot(yaw);
            layered.startBurst(BURST_DELAY_TICKS);
        }
        if (!level.addFreshEntity(layered)) {
            return null;
        }
        layered.setTarget(target);
        forget();
        return layered;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        // The omens leave nothing behind (sounds, particles, fled animals).
        forget();
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        LayeredConfig settings = settings();
        Vec3 origin = Vec3.atBottomCenterOf(to);
        if (settings.traceMax.get() <= 0 || !level.hasChunkAt(to) || level.getRandom().nextDouble() >= settings.traceChance.get()
                || level.hasNearbyAlivePlayer(origin.x, origin.y, origin.z, TRACE_UNSEEN)
                || LayeredClawMarks.count(level) >= settings.traceMax.get()) {
            return;
        }
        LayeredWallSpot.findFace(level, origin, settings.traceWallRadius.get())
                .ifPresent(face -> LayeredClawMarks.place(level, face, settings.traceSeconds.get() * 20));
    }

    @Nullable
    private LayeredWallSpot.Spot wallAt(ServerLevel level, BlockPos place) {
        if (!place.equals(rememberedPlace)) {
            rememberedPlace = place;
            rememberedWall = LayeredWallSpot.at(level, ModEntityTypes.THE_LAYERED.get(), place).orElse(null);
        }
        return rememberedWall;
    }

    private void forget() {
        rememberedPlace = null;
        rememberedWall = null;
    }

    /** Feet at {@code place} if it has ground under it and room for its body, dry. */
    private static Optional<Vec3> ground(ServerLevel level, TheLayered layered, BlockPos place) {
        Vec3 feet = Vec3.atBottomCenterOf(place);
        BlockPos below = place.below();
        AABB box = layered.getDimensions(layered.getPose()).makeBoundingBox(feet);
        if (level.getBlockState(below).getCollisionShape(level, below).isEmpty() || !LayeredBurrow.hasRoom(level, layered, feet)
                || level.containsAnyLiquid(box)) {
            return Optional.empty();
        }
        return Optional.of(feet);
    }

    /** A point 8–16 blocks from the target in any direction: the rattle comes from nowhere in particular. */
    private static Vec3 somewhereInTheCanyon(ServerPlayer target, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = RATTLE_MIN + random.nextDouble() * (RATTLE_MAX - RATTLE_MIN);
        return target.position().add(Math.cos(angle) * distance, 1.0 + random.nextDouble() * 4.0, Math.sin(angle) * distance);
    }

    private static void fleeAnimals(ServerLevel level, Vec3 from) {
        double radius = settings().arrivalFleeRadius.get();
        if (radius <= 0.0) {
            return;
        }
        for (Animal animal : level.getEntitiesOfClass(Animal.class, new AABB(from, from).inflate(radius))) {
            if (animal.isLeashed() || animal.isVehicle() || animal.isPassenger()) {
                continue;
            }
            Vec3 away = DefaultRandomPos.getPosAway(animal, 16, 7, from);
            if (away != null) {
                animal.getNavigation().moveTo(away.x, away.y, away.z, 1.6);
            }
        }
    }
}
