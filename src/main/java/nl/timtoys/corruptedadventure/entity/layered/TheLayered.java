package nl.timtoys.corruptedadventure.entity.layered;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import nl.timtoys.corruptedadventure.config.LayeredConfig;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmarePaceGoal;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareSearchGoal;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareTargetGoal;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

import java.util.ArrayList;
import java.util.List;

/**
 * De Gelaagde, the ring 2 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A canyon crawler of
 * giant's bone with a back of canyon stone: it walks the canyon floor and climbs its walls and steps (spider-like, with
 * {@link WallClimberNavigation}); on a wall it lies flat against it ({@link #getSurface}, the renderer turns the model).
 * It bites with the beak of its living head, grabs a player and climbs a wall with them ({@link LayeredGrabGoal}), and
 * breaks real rock and loose sand loose above its target ({@link LayeredRockfallGoal}, {@link LayeredRockfall}).
 * <p>
 * Weak points: the armoured back takes {@code backDamageMultiplier} of a hit, the furred belly the full hit (a hit from
 * below it, on the head part, or while it lies on its back). Fire in its fur or the dazzle of a sunstone flash
 * ({@code corruptedadventure:dazzled}, {@link ModMobEffects#DAZZLED}) makes it lose its grip: it lets
 * go, falls from the wall, takes {@code fallImpactDamage} and lies on its back for {@code fallenSeconds}. Its living head
 * is a {@link LayeredHeadPart}; hits there free a grabbed player. Boss bar, health scaling, retreat and the rest of the
 * shared rules come from {@link Nightmare}. Its arrival, roaming and return come in steps 2 and 3.
 * <p>
 * Life round: it also pounces from a wall or a ledge ({@link LayeredPounceGoal}), burrows through sand and gravel and
 * bursts up under its target ({@link LayeredBurrowGoal}; hidden and untouchable meanwhile, synced {@code DATA_BURROWED}),
 * and its dead heads spit dust and bone splinters ({@link LayeredSpitGoal}, {@link LayeredSpit}). After a grab, rock
 * fall, pounce, burrow or spit the next of these waits {@code specialGapTicks}. The shared reactions: every attack passes
 * {@link #mayStartAttack()}; a stagger lets go of a held player, and on a wall it falls (without the impact and the
 * flip of a dazzle); a hit on its belly from below is a hit on its weak spot; in a rest window on a wall it hangs still
 * (an ambush), elsewhere it walks and climbs to the rest point and the search.
 */
public class TheLayered extends Nightmare {
    private static final EntityDataAccessor<Direction> DATA_SURFACE =
            SynchedEntityData.defineId(TheLayered.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_FALLEN =
            SynchedEntityData.defineId(TheLayered.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_BURROWED =
            SynchedEntityData.defineId(TheLayered.class, EntityDataSerializers.BOOLEAN);
    /** A hit counts as from below when the attacker's eyes are this far under its feet. */
    private static final double BELLY_MARGIN = 0.5;
    /** Ticks the model takes to turn onto a wall or back (client only). */
    private static final float WALL_TURN_TICKS = 6.0F;
    private static final float DEAD_HEAD_RANGE = 24.0F;

    private final LayeredHeadPart head;
    private final PartEntity<?>[] parts;
    private boolean climbing;
    private boolean clingRequested;
    private boolean knockedOff;
    /** Whether the fall from the wall ends with the impact and the flip (fire, dazzle) or not (a stagger). */
    private boolean hardFall;
    private boolean leaping;
    /** Hanging still on a wall through a rest window ({@link #moveForReaction}). */
    private boolean ambush;
    private int nextSpecialTick;
    private final List<LayeredSpit> spits = new ArrayList<>();
    private boolean releasing;
    private int fallenTicks;
    private int dazzleFlipAllowedTick;
    private int rockfallUsed;
    private int freeHitsByOthers;
    private int freeHitsBySelf;
    // Client only: how far the model has turned onto a wall, and where the dead heads look.
    private float wallTurn;
    private float wallTurnO;
    private final float[] deadHeadYaw = new float[2];
    private final float[] deadHeadPitch = new float[2];

    public TheLayered(EntityType<? extends TheLayered> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        head = new LayeredHeadPart(this);
        parts = new PartEntity<?>[]{head};
        // The parts' ids follow the parent's (as the ender dragon's do), so the client finds them by id.
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return nightmareAttributes(LayeredConfig.DEFAULTS, 40.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    static LayeredConfig settings() {
        return ServerConfig.LAYERED;
    }

    @Override
    protected NightmareConfig config() {
        return ServerConfig.LAYERED.common;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SURFACE, Direction.DOWN);
        builder.define(DATA_FALLEN, false);
        builder.define(DATA_BURROWED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new NightmareSearchGoal(this));
        goalSelector.addGoal(1, new NightmarePaceGoal(this));
        // The attack choice: the first whose conditions hold (wall, sand, range) and whose cooldown is over.
        goalSelector.addGoal(2, new LayeredGrabGoal(this));
        goalSelector.addGoal(3, new LayeredPounceGoal(this));
        goalSelector.addGoal(4, new LayeredBurrowGoal(this));
        goalSelector.addGoal(5, new LayeredRockfallGoal(this));
        goalSelector.addGoal(6, new LayeredSpitGoal(this));
        goalSelector.addGoal(7, new LayeredBiteGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NightmareTargetGoal(this));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    // --- parts ------------------------------------------------------------------------------------------------------

    @Override
    public void setId(int id) {
        super.setId(id);
        if (parts != null) {
            for (int i = 0; i < parts.length; i++) {
                parts[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return parts;
    }

    private void placeHead() {
        Vec3 offset = LayeredPose.offset(LayeredPose.HEAD_UP, LayeredPose.HEAD_FORWARD, yBodyRot, isOnWall() ? 1.0F : 0.0F);
        Vec3 at = position().add(offset).subtract(0.0, head.getBbHeight() / 2.0, 0.0);
        head.xo = head.getX();
        head.yo = head.getY();
        head.zo = head.getZ();
        head.xOld = head.xo;
        head.yOld = head.yo;
        head.zOld = head.zo;
        head.setPos(at);
    }

    /** Whether the target is within {@code biteReach} of the tip of its beak. */
    boolean inBiteReach(LivingEntity target) {
        double reach = settings().biteReach.get();
        return target.getBoundingBox().distanceToSqr(beakTip()) <= reach * reach;
    }

    /** The tip of the beak in the world: bites are measured from here. */
    public Vec3 beakTip() {
        return position().add(LayeredPose.offset(LayeredPose.BEAK_UP, LayeredPose.BEAK_FORWARD, yBodyRot, isOnWall() ? 1.0F : 0.0F));
    }

    // --- state --------------------------------------------------------------------------------------------------------

    /** {@code DOWN} on the ground; a horizontal direction while it clings to the wall on that side of it. */
    public Direction getSurface() {
        return entityData.get(DATA_SURFACE);
    }

    public boolean isOnWall() {
        return getSurface().getAxis().isHorizontal();
    }

    private void setSurface(Direction surface) {
        if (getSurface() != surface) {
            entityData.set(DATA_SURFACE, surface);
        }
    }

    /** Lying helpless on its back: no attacks, and every hit lands on its belly. */
    public boolean isFallen() {
        return entityData.get(DATA_FALLEN);
    }

    /** Falling from a wall after losing its grip; it lands on its back. */
    public boolean isKnockedOff() {
        return knockedOff;
    }

    /** It can attack and climb: not on its back, not falling, not leaping, not under the sand, not dying. */
    public boolean canAct() {
        return isAlive() && !isFallen() && !knockedOff && !leaping && !isBurrowed();
    }

    /** Under the sand ({@link LayeredBurrowGoal}): hidden, without collision, untouchable. */
    public boolean isBurrowed() {
        return entityData.get(DATA_BURROWED);
    }

    /** In the air on a pounce. */
    public boolean isLeaping() {
        return leaping;
    }

    public boolean isDazzled() {
        return hasEffect(ModMobEffects.DAZZLED);
    }

    /** Asked by a goal: hold still on the wall (no sliding), e.g. while it hammers loose a rock fall. */
    void requestCling(boolean cling) {
        clingRequested = cling;
    }

    boolean isClinging() {
        return isOnWall() && !knockedOff && !leaping
                && (clingRequested || ambush || (zza == 0.0F && xxa == 0.0F && !targetNotAbove()));
    }

    /**
     * Its target is not above it while its beak is free: then it slides down the wall (slowly, as on a ladder) instead
     * of hanging still beside or above it, where neither its bite nor its grab (only on the ground) can reach.
     */
    private boolean targetNotAbove() {
        LivingEntity target = getTarget();
        return target != null && !isVehicle() && target.getY() < getY() + 1.0;
    }

    @Override
    public boolean onClimbable() {
        return canAct() && (climbing || isOnWall());
    }

    @Override
    public void travel(Vec3 input) {
        if (isClinging() || isBurrowed()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(isFallen() || isStaggered() ? Vec3.ZERO : input);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (isAlive()) {
                tickGrip();
                updateSurface();
            }
            if (ambush && (!isResting() || !isOnWall())) {
                ambush = false;
            }
            tickSpits();
        } else {
            tickVisuals();
        }
        if (isOnWall()) {
            // Flat against the wall it faces the wall, whatever its head looks at.
            float yaw = getSurface().toYRot();
            if (!level().isClientSide()) {
                setYRot(yaw);
            }
            yBodyRot = yaw;
            yBodyRotO = yaw;
        }
        placeHead();
    }

    /** Fire in its fur, the dazzle, the fall and lying on its back. */
    private void tickGrip() {
        climbing = horizontalCollision;
        LayeredConfig settings = settings();
        if (fallenTicks > 0 && --fallenTicks == 0) {
            entityData.set(DATA_FALLEN, false);
            dazzleFlipAllowedTick = tickCount + settings.dazzleFlipCooldownSeconds.get() * 20;
            LayeredEffects.getUp((ServerLevel) level(), this);
        }
        if (knockedOff && (onGround() || isInWater() || isInLava())) {
            land();
            return;
        }
        boolean dazzled = isDazzled();
        if (isOnWall() && (isOnFire() || dazzled)) {
            knockOff();
        } else if (dazzled && onGround() && settings.dazzleFlipsOnGround.get() && tickCount >= dazzleFlipAllowedTick) {
            flip();
        }
        if (dazzled && isVehicle()) {
            releaseHeld();
        }
    }

    /** Which surface it clings to: a wall while it climbs against it or holds on, else the ground. */
    private void updateSurface() {
        if (knockedOff || isFallen() || isDeadOrDying() || leaping || isBurrowed()) {
            setSurface(Direction.DOWN);
            return;
        }
        if (horizontalCollision && !onGround()) {
            Direction wall = wallAhead();
            if (wall != null) {
                setSurface(wall);
                return;
            }
        }
        Direction surface = getSurface();
        if (surface != Direction.DOWN && ((onGround() && !horizontalCollision) || !touchesWall(surface))) {
            // Down at the foot of the wall, or over its top edge.
            setSurface(Direction.DOWN);
        }
    }

    @Nullable
    private Direction wallAhead() {
        Direction facing = Direction.fromYRot(getYRot());
        if (touchesWall(facing)) {
            return facing;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (touchesWall(dir)) {
                return dir;
            }
        }
        return null;
    }

    /** Blocks right beside its hitbox on that side, above its lowest half block (so a step does not count). */
    boolean touchesWall(Direction dir) {
        AABB box = getBoundingBox();
        AABB probe = new AABB(box.minX, box.minY + 0.5, box.minZ, box.maxX, box.maxY, box.maxZ)
                .deflate(0.05, 0.0, 0.05).move(dir.getStepX() * 0.2, 0.0, dir.getStepZ() * 0.2);
        return !level().noBlockCollision(this, probe);
    }

    /** The nearest side within {@code radius} blocks with a wall at body height, or null. */
    @Nullable
    Direction findWall(int radius) {
        BlockPos from = BlockPos.containing(getX(), getY() + 1.0, getZ());
        Direction best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i <= radius && i < bestDistance; i++) {
                BlockPos pos = from.relative(dir, i);
                BlockState state = level().getBlockState(pos);
                if (!state.getCollisionShape(level(), pos).isEmpty() && !level().getBlockState(pos.above()).getCollisionShape(level(), pos.above()).isEmpty()) {
                    best = dir;
                    bestDistance = i;
                    break;
                }
            }
        }
        return best;
    }

    /** Loses its grip on the wall: lets go of a held player and falls; {@link #land} when it hits the ground. */
    public void knockOff() {
        knockOff(true);
    }

    /** {@code hard}: the landing hurts it and throws it on its back (fire, dazzle); else it just lands (a stagger). */
    private void knockOff(boolean hard) {
        if (knockedOff) {
            return;
        }
        releaseHeld();
        knockedOff = true;
        hardFall = hard;
        clingRequested = false;
        setSurface(Direction.DOWN);
        getNavigation().stop();
        LayeredEffects.knockedOff((ServerLevel) level(), this);
    }

    private void land() {
        knockedOff = false;
        LayeredEffects.land((ServerLevel) level(), this);
        if (!hardFall) {
            return;
        }
        double impact = settings().fallImpactDamage.get();
        if (impact > 0.0) {
            hurt(damageSources().fall(), (float) impact);
        }
        flip();
    }

    /**
     * Thrown on its back for {@code fallenSeconds}: helpless, its belly up. A dazzle on the ground only does this again
     * {@code dazzleFlipCooldownSeconds} after it got up, so flashes cannot keep it down; a fall from a wall always does.
     */
    public void flip() {
        int ticks = settings().fallenSeconds.get() * 20;
        if (ticks <= 0 || isFallen() || !isAlive()) {
            return;
        }
        releaseHeld();
        fallenTicks = ticks;
        entityData.set(DATA_FALLEN, true);
        getNavigation().stop();
        LayeredEffects.flipped((ServerLevel) level(), this);
    }

    // --- the held player ----------------------------------------------------------------------------------------------

    /** Grabs a player in its beak: the player rides it and cannot get off by sneaking ({@link LayeredGrip}). */
    public boolean grab(Player player) {
        if (isVehicle() || !player.startRiding(this, true)) {
            return false;
        }
        freeHitsByOthers = 0;
        freeHitsBySelf = 0;
        return true;
    }

    @Nullable
    public Player heldPlayer() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    /** Lets go of the held player, wherever it is: they fall from there. */
    public void releaseHeld() {
        if (!isVehicle()) {
            return;
        }
        releasing = true;
        try {
            ejectPassengers();
        } finally {
            releasing = false;
        }
        triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.RELEASE);
        if (level() instanceof ServerLevel serverLevel) {
            LayeredEffects.release(serverLevel, this);
        }
    }

    /** True while it lets go itself; a sneaking player is held on otherwise. */
    public boolean isReleasing() {
        return releasing;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        return LayeredPose.heldOffset(yBodyRot, isOnWall() ? 1.0F : 0.0F);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // Dropped where it hangs: that is the point of being carried up a wall.
        return passenger.position();
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    // --- damage -------------------------------------------------------------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALLING_BLOCK)) {
            // Its own rock fall.
            return false;
        }
        if (untouchableUnderground(source)) {
            return false;
        }
        boolean belly = isBellyHit(source);
        boolean hurt = super.hurt(source, amount * (belly ? 1.0F : settings().backDamageMultiplier.get().floatValue()));
        if (hurt && belly && !level().isClientSide() && isHitFromBelow(source)) {
            // Its soft belly, open because it crawls over you: its weak spot.
            hitWeakSpot();
        }
        return hurt;
    }

    /** Under the sand nothing reaches it, except what bypasses invulnerability ({@code /kill}, the void). */
    private boolean untouchableUnderground(DamageSource source) {
        return isBurrowed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPickable() {
        return !isBurrowed() && super.isPickable();
    }

    /**
     * A hit on its living head (the head part): full damage, and a hit by a player counts toward freeing the held player
     * ({@code freeHitsByOthers}, or {@code freeHitsBySelf} for the held player).
     */
    boolean hurtHead(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALLING_BLOCK) || untouchableUnderground(source) || !super.hurt(source, amount)) {
            return false;
        }
        Player held = heldPlayer();
        if (!level().isClientSide() && held != null && source.getEntity() instanceof Player hitter) {
            LayeredConfig settings = settings();
            boolean free = hitter == held ? ++freeHitsBySelf >= settings.freeHitsBySelf.get()
                    : ++freeHitsByOthers >= settings.freeHitsByOthers.get();
            if (free) {
                releaseHeld();
            }
        }
        return true;
    }

    /**
     * The belly: on its back every hit; otherwise a hit from below it (the attacker's eyes, or the source's position
     * without an attacker, under its feet), or damage without a position (fire, poison, the void).
     */
    boolean isBellyHit(DamageSource source) {
        if (isFallen() || source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return true;
        }
        Vec3 from = hitFrom(source);
        return from == null || from.y < getY() - BELLY_MARGIN;
    }

    /** A hit that comes from under it (not one without a position, not one while it lies on its back). */
    private boolean isHitFromBelow(DamageSource source) {
        Vec3 from = hitFrom(source);
        return !isFallen() && from != null && from.y < getY() - BELLY_MARGIN;
    }

    @Nullable
    private static Vec3 hitFrom(DamageSource source) {
        return source.getEntity() instanceof LivingEntity attacker ? attacker.getEyePosition() : source.getSourcePosition();
    }

    // --- rock fall budget -----------------------------------------------------------------------------------------------

    int rockfallLeft() {
        return Math.max(0, settings().rockfallBlocksPerFight.get() - rockfallUsed);
    }

    void useRockfall(int blocks) {
        rockfallUsed += blocks;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RockfallUsed", rockfallUsed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rockfallUsed = tag.getInt("RockfallUsed");
    }

    // --- life and death -------------------------------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        releaseHeld();
        emerge();
        endLeap();
        spits.clear();
        super.die(source);
    }

    @Override
    protected void onRetreat(ServerLevel level) {
        releaseHeld();
        emerge();
        LayeredEffects.retreat(level, this);
    }

    // --- the shared reactions -------------------------------------------------------------------------------------------

    @Override
    protected boolean canStagger() {
        // On its back, falling or under the sand it is already open, already falling, or out of reach.
        return !isFallen() && !knockedOff && !isBurrowed();
    }

    /** Lets go of a held player; on a wall it loses its grip and falls (without the impact and the flip of a dazzle). */
    @Override
    protected void onStaggered() {
        releaseHeld();
        clingRequested = false;
        endLeap();
        if (isOnWall()) {
            knockOff(false);
        }
    }

    @Override
    protected void staggerEffects(ServerLevel level) {
        LayeredEffects.stagger(level, this);
    }

    @Override
    protected void onSniff() {
        triggerAnim(LayeredAnimations.ACTION, LayeredAnimations.SNIFF);
        if (level() instanceof ServerLevel serverLevel) {
            LayeredEffects.sniff(serverLevel, this);
        }
    }

    /**
     * Walks and climbs there; in a rest window on a wall it hangs still instead, an ambush ({@link #isClinging}). Only
     * through the rest goal, so an attack that is still running in the window (a climb with a held player) climbs on.
     */
    @Override
    protected void moveForReaction(Vec3 point, double speed) {
        ambush = isOnWall() && isResting();
        if (ambush) {
            getNavigation().stop();
            return;
        }
        super.moveForReaction(point, speed);
    }

    // --- attack state ---------------------------------------------------------------------------------------------------

    /** Whether a grab, rock fall, pounce, burrow or spit may start: {@code specialGapTicks} after the last one ended. */
    boolean specialReady() {
        return tickCount >= nextSpecialTick;
    }

    void specialEnded() {
        nextSpecialTick = tickCount + settings().specialGapTicks.get();
    }

    /** Off the wall into a pounce toward {@code landing}: no climbing, no clinging until {@link #endLeap}. */
    void startLeap(Vec3 landing) {
        leaping = true;
        clingRequested = false;
        setSurface(Direction.DOWN);
        getNavigation().stop();
        float yaw = (float) Math.toDegrees(Math.atan2(-(landing.x - getX()), landing.z - getZ()));
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    void endLeap() {
        leaping = false;
    }

    /**
     * Gone under the sand: hidden (no shadow either), without collision, untouchable; {@link #travel} keeps it still (no
     * gravity) while the burrow moves it. Nothing of this is saved, so a Layered unloaded under the sand comes back on it.
     */
    public void submerge() {
        entityData.set(DATA_BURROWED, true);
        setInvisible(true);
        noPhysics = true;
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
    }

    /** Moves its feet to {@code feet} under the sand (see {@link LayeredBurrow#step}). */
    void moveUnderground(Vec3 feet) {
        setPos(feet);
    }

    /** Back up out of the sand (also when a burrow is broken off). */
    public void emerge() {
        if (!isBurrowed()) {
            return;
        }
        entityData.set(DATA_BURROWED, false);
        setInvisible(false);
        noPhysics = false;
    }

    /** A cloud of dust and bone splinters on its way; moved every tick until it bursts. */
    void spit(LayeredSpit cloud) {
        spits.add(cloud);
    }

    private void tickSpits() {
        if (!spits.isEmpty()) {
            ServerLevel level = (ServerLevel) level();
            spits.removeIf(cloud -> !cloud.tick(level, this));
        }
    }

    @Override
    protected int deathTicks() {
        return LayeredAnimations.DEATH_TICKS;
    }

    @Override
    protected double modelOverhang() {
        // Its legs reach ~5 blocks to each side; on a wall its abdomen hangs ~3 below it.
        return 4.0;
    }

    @Override
    protected double modelOverhangUp() {
        return isOnWall() ? 3.5 : 2.5;
    }

    @Override
    protected double deadOverhang() {
        return 4.5;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F + random.nextFloat() * 0.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SKELETON_STEP, 0.6F, 0.6F);
    }

    // --- client visuals ---------------------------------------------------------------------------------------------------

    private void tickVisuals() {
        wallTurnO = wallTurn;
        float goal = isOnWall() ? 1.0F : 0.0F;
        wallTurn = Mth.approach(wallTurn, goal, 1.0F / WALL_TURN_TICKS);
        // The dead heads turn to the nearest player, each at its own pace and never quite together.
        Player player = level().getNearestPlayer(this, DEAD_HEAD_RANGE);
        for (int i = 0; i < 2; i++) {
            float yaw = 0.0F;
            float pitch = 0.0F;
            if (player != null && !isOnWall() && !isFallen()) {
                Vec3 to = player.getEyePosition().subtract(position().add(0.0, LayeredPose.HEAD_UP, 0.0));
                float worldYaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
                yaw = Mth.clamp(Mth.wrapDegrees(worldYaw - yBodyRot), -55.0F, 55.0F);
                pitch = Mth.clamp((float) (-Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG), -30.0F, 30.0F);
            }
            float pace = i == 0 ? 0.12F : 0.07F;
            deadHeadYaw[i] += (yaw - deadHeadYaw[i]) * pace;
            deadHeadPitch[i] += (pitch - deadHeadPitch[i]) * pace;
        }
    }

    /** 0 on the ground .. 1 flat on a wall, smoothed for rendering (client). */
    public float wallTurn(float partialTick) {
        return Mth.lerp(partialTick, wallTurnO, wallTurn);
    }

    /** Where dead head 0 (left) or 1 (right) looks, in degrees from straight ahead (client). */
    public float deadHeadYaw(int index) {
        return deadHeadYaw[index];
    }

    public float deadHeadPitch(int index) {
        return deadHeadPitch[index];
    }

    // --- animation ----------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, LayeredAnimations.BODY, LayeredAnimations.BODY_BLEND_TICKS,
                this::bodyAnimation));
        controllers.add(new AnimationController<>(this, LayeredAnimations.GRIP, 4,
                state -> isVehicle() && !isDeadOrDying() ? state.setAndContinue(LayeredAnimations.HOLD) : PlayState.STOP));
        controllers.add(new AnimationController<>(this, LayeredAnimations.HEADS, 0,
                state -> state.setAndContinue(LayeredAnimations.DEAD_HEADS)));
        controllers.add(new AnimationController<>(this, LayeredAnimations.ACTION, 2, state -> PlayState.STOP)
                .triggerableAnim(LayeredAnimations.GRAB, LayeredAnimations.GRAB_ANIM)
                .triggerableAnim(LayeredAnimations.ROCKFALL, LayeredAnimations.ROCKFALL_ANIM)
                .triggerableAnim(LayeredAnimations.RELEASE, LayeredAnimations.RELEASE_ANIM)
                .triggerableAnim(LayeredAnimations.POUNCE, LayeredAnimations.POUNCE_ANIM)
                .triggerableAnim(LayeredAnimations.POUNCE_LAND, LayeredAnimations.POUNCE_LAND_ANIM)
                .triggerableAnim(LayeredAnimations.BURROW, LayeredAnimations.BURROW_ANIM)
                .triggerableAnim(LayeredAnimations.ERUPT, LayeredAnimations.ERUPT_ANIM)
                .triggerableAnim(LayeredAnimations.SPIT, LayeredAnimations.SPIT_ANIM)
                .triggerableAnim(LayeredAnimations.SNIFF, LayeredAnimations.SNIFF_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheLayered> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(LayeredAnimations.DEATH);
        }
        if (isFallen()) {
            return state.setAndContinue(LayeredAnimations.FALLEN);
        }
        if (isStaggered()) {
            return state.setAndContinue(LayeredAnimations.STAGGER);
        }
        // Climbing gives no limb swing (that only counts horizontal steps), so on a wall any movement counts.
        boolean moving = state.isMoving() || (isOnWall() && position().distanceToSqr(xo, yo, zo) > 1.0E-4);
        return state.setAndContinue(moving ? LayeredAnimations.WALK : LayeredAnimations.IDLE);
    }
}
