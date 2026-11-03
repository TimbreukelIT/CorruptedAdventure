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
import nl.timtoys.corruptedadventure.entity.nightmare.DefaultNightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareArrival;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareSpots;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * How The Layered comes (fase-4-nightmares-concept.md, "Ring 2 — De Gelaagde", komst: the wall comes loose), for the
 * director. Is there a natural wall within {@code arrivalWallRadius} of the target ({@link LayeredWallSpot}), then that
 * is where it comes: during the omens pebbles and dust trickle down that wall, bones rattle somewhere in the canyon and
 * the animals flee; then a piece of the wall unfolds, and that is it ({@link TheLayered#startUnfolding}). Without a wall
 * it bursts up out of the ground {@code arrivalBurstDistance} behind the target, as from its burrow, after the same
 * omens and the heaving sand ({@link TheLayered#startBurst}), never closer to the target than {@link #MIN_BURST_DISTANCE};
 * is there no room for that either, it comes as the default arrival does. No block changes. While it roams it leaves claw
 * marks in a canyon wall now and then ({@link LayeredClawMarks}).
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
    /** It never bursts up closer to its target than this (not under the player's feet). */
    public static final double MIN_BURST_DISTANCE = 4.0;
    private static final int RING_ANGLES = 12;
    private static final int SPOT_DOWN = 6;
    private static final int SPOT_UP = 4;

    /**
     * Per place (one per arrival under way; omen() runs every tick): the exact feet and the wall it unfolds from, or no
     * feet when there was no room at all (then it comes as the default arrival). Rebuilt from the place after a reload;
     * dropped when it appears or the omens stop.
     */
    private record Plan(@Nullable Vec3 feet, @Nullable LayeredWallSpot.Spot wall) {
    }

    private final Map<BlockPos, Plan> plans = new HashMap<>();

    private static LayeredConfig settings() {
        return TheLayered.settings();
    }

    @Override
    public BlockPos place(ServerLevel level, ServerPlayer target) {
        EntityType<TheLayered> type = ModEntityTypes.THE_LAYERED.get();
        Optional<LayeredWallSpot.Spot> wall = LayeredWallSpot.find(level, type, target.position(),
                settings().arrivalWallRadius.get(), level.getRandom());
        if (wall.isPresent()) {
            plans.put(wall.get().pos(), new Plan(wall.get().feet(), wall.get()));
            return wall.get().pos();
        }
        Optional<Vec3> feet = burstSpot(level, type, target);
        BlockPos place = feet.map(BlockPos::containing).orElse(target.blockPosition());
        plans.put(place, new Plan(feet.orElse(null), null));
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
        Plan plan = plans.computeIfAbsent(place, pos -> replan(level, pos));
        LayeredWallSpot.Spot wall = plan.wall();
        Vec3 at = wall != null ? wall.feet() : plan.feet() != null ? plan.feet() : Vec3.atBottomCenterOf(place);
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
        if (plan.feet() == null) {
            // No room to burst up: the default arrival comes, so no heaving sand anywhere.
            return;
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
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
        // Forgotten whatever comes of it: a retry (null) plans again from the place.
        Plan plan = plans.remove(place);
        if (!level.hasChunkAt(place)) {
            return null;
        }
        // Checked again: the wall may have changed since the omens began.
        Optional<LayeredWallSpot.Spot> wall = LayeredWallSpot.at(level, type, place);
        Optional<Vec3> feet = Optional.empty();
        if (wall.isEmpty()) {
            // Where the omens heaved; without a plan (a reload, a retry) the place itself, if not under the target.
            Vec3 planned = plan != null ? plan.feet() : Vec3.atBottomCenterOf(place);
            feet = Optional.ofNullable(planned)
                    .filter(at -> ground(level, type, at) && (plan != null || farEnough(target, at)))
                    .or(() -> burstSpot(level, type, target));
            if (feet.isEmpty()) {
                return DefaultNightmareArrival.INSTANCE.appear(level, type, target, place, strength);
            }
        }
        Entity created = type.create(level);
        if (!(created instanceof TheLayered layered)) {
            return null;
        }
        if (wall.isPresent()) {
            Vec3 at = wall.get().feet();
            layered.moveTo(at.x, at.y, at.z, wall.get().wall().toYRot(), 0.0F);
            layered.startUnfolding(wall.get().wall(), settings().arrivalUnfoldTicks.get());
        } else {
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
        return layered;
    }

    @Override
    public void cancelled(ServerLevel level, BlockPos place) {
        // The omens leave nothing behind (sounds, particles, fled animals).
        plans.remove(place);
    }

    @Override
    public void roamed(ServerLevel level, BlockPos from, BlockPos to) {
        LayeredConfig settings = settings();
        Vec3 origin = Vec3.atBottomCenterOf(to);
        if (settings.traceMax.get() <= 0 || !level.hasChunkAt(to) || level.getRandom().nextDouble() >= settings.traceChance.get()
                || LayeredClawMarks.count(level) >= settings.traceMax.get()) {
            return;
        }
        // Not where a player would see them pop up: measured at the marks, not at the roam position.
        LayeredWallSpot.findFace(level, origin, settings.traceWallRadius.get())
                .filter(face -> !level.hasNearbyAlivePlayer(face.at().x, face.at().y, face.at().z, TRACE_UNSEEN))
                .ifPresent(face -> LayeredClawMarks.place(level, face, settings.traceSeconds.get() * 20));
    }

    /** The plan for a place without one (after a reload): the wall there, else bursting up on the place itself. */
    private static Plan replan(ServerLevel level, BlockPos place) {
        Optional<LayeredWallSpot.Spot> wall = LayeredWallSpot.at(level, ModEntityTypes.THE_LAYERED.get(), place);
        return wall.map(spot -> new Plan(spot.feet(), spot)).orElseGet(() -> new Plan(Vec3.atBottomCenterOf(place), null));
    }

    /**
     * Where it bursts up: {@code arrivalBurstDistance} behind the target, else anywhere round it at that distance or at
     * {@link #MIN_BURST_DISTANCE}; never closer. Empty without room.
     */
    private static Optional<Vec3> burstSpot(ServerLevel level, EntityType<?> type, ServerPlayer target) {
        double distance = Math.max(MIN_BURST_DISTANCE, settings().arrivalBurstDistance.get());
        Optional<Vec3> behind = NightmareSpots.behind(level, type, target, distance)
                .filter(at -> farEnough(target, at) && ground(level, type, at));
        if (behind.isPresent()) {
            return behind;
        }
        float start = target.getYHeadRot() * Mth.DEG_TO_RAD;
        for (double d : new double[] {distance, MIN_BURST_DISTANCE}) {
            for (int i = 0; i < RING_ANGLES; i++) {
                float angle = start + i * Mth.TWO_PI / RING_ANGLES;
                double x = target.getX() + Mth.sin(angle) * d;
                double z = target.getZ() - Mth.cos(angle) * d;
                // At the target's height first, then down, and only last up (not onto a roof over it).
                for (int k = 0; k <= SPOT_DOWN + SPOT_UP; k++) {
                    int dy = k <= SPOT_DOWN ? -k : k - SPOT_DOWN;
                    Vec3 at = new Vec3(x, Mth.floor(target.getY()) + dy, z);
                    if (level.hasChunkAt(BlockPos.containing(at)) && ground(level, type, at)) {
                        return Optional.of(at);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean farEnough(ServerPlayer target, Vec3 at) {
        return Mth.square(at.x - target.getX()) + Mth.square(at.z - target.getZ()) >= MIN_BURST_DISTANCE * MIN_BURST_DISTANCE;
    }

    /** Whether its feet fit at {@code feet}: ground under them, room for its body, dry, inside the world border. */
    private static boolean ground(ServerLevel level, EntityType<?> type, Vec3 feet) {
        BlockPos below = BlockPos.containing(feet).below();
        AABB box = type.getDimensions().makeBoundingBox(feet);
        return level.getWorldBorder().isWithinBounds(below)
                && !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.noBlockCollision(null, box.deflate(0.05)) && !level.containsAnyLiquid(box);
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
