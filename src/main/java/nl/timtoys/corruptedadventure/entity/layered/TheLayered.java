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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
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
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

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
 */
public class TheLayered extends Nightmare {
    private static final EntityDataAccessor<Direction> DATA_SURFACE =
            SynchedEntityData.defineId(TheLayered.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> DATA_FALLEN =
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
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LayeredGrabGoal(this));
        goalSelector.addGoal(2, new LayeredRockfallGoal(this));
        goalSelector.addGoal(3, new LayeredBiteGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
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

    /** It can attack and climb: not on its back, not falling, not dying. */
    public boolean canAct() {
        return isAlive() && !isFallen() && !knockedOff;
    }

    public boolean isDazzled() {
        return hasEffect(ModMobEffects.DAZZLED);
    }

    /** Asked by a goal: hold still on the wall (no sliding), e.g. while it hammers loose a rock fall. */
    void requestCling(boolean cling) {
        clingRequested = cling;
    }

    boolean isClinging() {
        return isOnWall() && !knockedOff && (clingRequested || (zza == 0.0F && xxa == 0.0F));
    }

    @Override
    public boolean onClimbable() {
        return canAct() && (climbing || isOnWall());
    }

    @Override
    public void travel(Vec3 input) {
        if (isClinging()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(isFallen() ? Vec3.ZERO : input);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            if (isAlive()) {
                tickGrip();
                updateSurface();
            }
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
        if (knockedOff || isFallen() || isDeadOrDying()) {
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
        if (knockedOff) {
            return;
        }
        releaseHeld();
        knockedOff = true;
        clingRequested = false;
        setSurface(Direction.DOWN);
        getNavigation().stop();
        LayeredEffects.knockedOff((ServerLevel) level(), this);
    }

    private void land() {
        knockedOff = false;
        double impact = settings().fallImpactDamage.get();
        if (impact > 0.0) {
            hurt(damageSources().fall(), (float) impact);
        }
        LayeredEffects.land((ServerLevel) level(), this);
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
        float factor = isBellyHit(source) ? 1.0F : settings().backDamageMultiplier.get().floatValue();
        return super.hurt(source, amount * factor);
    }

    /**
     * A hit on its living head (the head part): full damage, and a hit by a player counts toward freeing the held player
     * ({@code freeHitsByOthers}, or {@code freeHitsBySelf} for the held player).
     */
    boolean hurtHead(DamageSource source, float amount) {
        if (source.is(DamageTypes.FALLING_BLOCK) || !super.hurt(source, amount)) {
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
        Vec3 from = source.getEntity() instanceof LivingEntity attacker ? attacker.getEyePosition() : source.getSourcePosition();
        return from == null || from.y < getY() - BELLY_MARGIN;
    }

    /**
     * Factor on the damage of its own attacks that do not go through {@code attack_damage} (grab, shake, rock fall).
     * Becomes the base's {@code strength()} (stronger after each return) once the director's hooks are merged.
     */
    float strengthFactor() {
        return 1.0F;
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
        super.die(source);
    }

    @Override
    protected void onRetreat(ServerLevel level) {
        releaseHeld();
        LayeredEffects.retreat(level, this);
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
                .triggerableAnim(LayeredAnimations.RELEASE, LayeredAnimations.RELEASE_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheLayered> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(LayeredAnimations.DEATH);
        }
        if (isFallen()) {
            return state.setAndContinue(LayeredAnimations.FALLEN);
        }
        // Climbing gives no limb swing (that only counts horizontal steps), so on a wall any movement counts.
        boolean moving = state.isMoving() || (isOnWall() && position().distanceToSqr(xo, yo, zo) > 1.0E-4);
        return state.setAndContinue(moving ? LayeredAnimations.WALK : LayeredAnimations.IDLE);
    }
}
