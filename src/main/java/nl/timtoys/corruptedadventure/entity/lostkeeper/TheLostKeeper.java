package nl.timtoys.corruptedadventure.entity.lostkeeper;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.timtoys.corruptedadventure.config.LostKeeperConfig;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.network.KeeperMistPayload;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

import java.util.ArrayList;
import java.util.List;

/**
 * De Verloren Hoeder, the ring 4 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A fallen
 * mythical sea serpent of ~24 blocks: the entity itself is the head, the body is a chain of {@link KeeperBody nodes} with
 * six {@link KeeperPart hitboxes} and one on the crown. It swims through water and, while its crown of pearl and coral
 * holds, through the air between the islands; it brings its own mist bank. Attacks: the siren's call
 * ({@link KeeperLureGoal}), the poison breath ({@link KeeperBreathGoal}), pulling under water ({@link KeeperGrabGoal}) and
 * the tail whip ({@link KeeperWhipGoal}), and from the life round the bubble trap ({@link KeeperBubbleTrapGoal}), the
 * shrinking ring of its body ({@link KeeperEncircleGoal}) and the dive and burst ({@link KeeperBurstGoal}); between them
 * it circles ({@link KeeperCircleGoal}). Every hit on the crown breaks off a branch; with the last one gone it loses its
 * swimming power in the air and falls.
 * <p>
 * Everything runs on the server; the client runs the same body chain from the head's movement and renders the model,
 * the synced {@link Mode}, crown and ring, and the mist ({@link KeeperMistPayload}).
 */
public class TheLostKeeper extends Nightmare {
    public static final int CROWN_BRANCHES = 6;
    private static final int CROWN_FULL = (1 << CROWN_BRANCHES) - 1;
    /** Node of each body hitbox and its size (blocks): thick behind the head, thin at the tail. */
    private static final int[] PART_NODES = {1, 3, 5, 7, 9, 11};
    private static final float[] PART_SIZES = {2.4F, 2.2F, 2.0F, 1.7F, 1.4F, 1.1F};
    /** The crown's centre from the head's centre: up and back in the head's own frame (blocks; from the model). */
    private static final double CROWN_UP = 1.15;
    private static final double CROWN_BACK = 0.75;
    /** The node the held fighter is coiled in. */
    private static final int HOLD_NODE = 2;
    /** The tail whip swings the nodes behind this one. */
    static final int WHIP_PIVOT = 6;
    static final double WHIP_RADIANS = Math.toRadians(110.0);
    private static final int MIST_SYNC_TICKS = 20;
    private static final int MIST_RISE_TICKS = 100;
    private static final double LOOK_AHEAD = 5.0;
    /** Per tick, how far the body nodes are drawn onto the ring while it coils (the rest of the way the next ticks). */
    private static final double RING_GRIP = 0.35;
    /** Height of each node's axis above the ring's floor while coiling: half the body's thickness there (blocks). */
    private static final double[] RING_LIFT = ringLift();

    private static final EntityDataAccessor<Integer> DATA_CROWN =
            SynchedEntityData.defineId(TheLostKeeper.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_MODE =
            SynchedEntityData.defineId(TheLostKeeper.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_WHIP =
            SynchedEntityData.defineId(TheLostKeeper.class, EntityDataSerializers.INT);
    /** The ring it coils: its centre on the floor (water: a block under the surface) and the radius, signed by direction. */
    private static final EntityDataAccessor<Vector3f> DATA_RING_CENTRE =
            SynchedEntityData.defineId(TheLostKeeper.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_RING_RADIUS =
            SynchedEntityData.defineId(TheLostKeeper.class, EntityDataSerializers.FLOAT);

    /** What the body does, for the looping animations (synced). */
    public enum Mode {
        SWIM, LURE, HOLD, FALL, STUNNED, CRAWL, COIL
    }

    private final KeeperBody body = new KeeperBody();
    private final KeeperPart[] bodyParts = new KeeperPart[PART_NODES.length];
    private final KeeperPart crownPart;
    private final KeeperPart[] parts;
    private final List<KeeperPoisonCloud> clouds = new ArrayList<>();

    @Nullable
    private Vec3 steerTarget;
    private double steerSpeed;
    @Nullable
    private Vec3 faceTarget;
    private boolean luring;
    private int crownCooldown;
    private boolean falling;
    private int stunTicks;
    private int holdTicks;
    private float heldDamage;
    private boolean releasing;
    private double turnScale = 1.0;
    private float damageTaken;

    public TheLostKeeper(EntityType<? extends TheLostKeeper> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        for (int i = 0; i < bodyParts.length; i++) {
            bodyParts[i] = new KeeperPart(this, PART_SIZES[i], PART_SIZES[i], false);
        }
        crownPart = new KeeperPart(this, 1.8F, 1.4F, true);
        parts = new KeeperPart[bodyParts.length + 1];
        System.arraycopy(bodyParts, 0, parts, 0, bodyParts.length);
        parts[bodyParts.length] = crownPart;
        // The parts' ids follow the keeper's (as vanilla's ender dragon), so client and server agree on them.
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        setNoGravity(true);
        // Its pitch is where it swims (setFacing): vanilla's look control would level it every tick, so it could only
        // ever climb or dive at one tick's turn (7°).
        lookControl = new LookControl(this) {
            @Override
            protected boolean resetXRotOnTick() {
                return false;
            }
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return nightmareAttributes(LostKeeperConfig.DEFAULTS, 64.0).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected NightmareConfig config() {
        return ServerConfig.LOST_KEEPER.common;
    }

    static LostKeeperConfig keeperConfig() {
        return ServerConfig.LOST_KEEPER;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CROWN, CROWN_FULL);
        builder.define(DATA_MODE, (byte) Mode.SWIM.ordinal());
        builder.define(DATA_WHIP, 0);
        builder.define(DATA_RING_CENTRE, new Vector3f());
        builder.define(DATA_RING_RADIUS, 0.0F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new KeeperGrabGoal(this));
        goalSelector.addGoal(2, new KeeperBurstGoal(this));
        goalSelector.addGoal(2, new KeeperBreathGoal(this));
        goalSelector.addGoal(3, new KeeperEncircleGoal(this));
        goalSelector.addGoal(3, new KeeperLureGoal(this));
        goalSelector.addGoal(4, new KeeperBubbleTrapGoal(this));
        goalSelector.addGoal(4, new KeeperWhipGoal(this));
        goalSelector.addGoal(6, new KeeperCircleGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // It finds you in its mist without seeing you.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        // The body is the chain; the entity (the head) turns where it swims, see tickHeadTurn.
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
            }
        };
    }

    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        return animStep;
    }

    // --- parts -------------------------------------------------------------------------------------------------------

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

    public KeeperBody body() {
        return body;
    }

    KeeperPart[] bodyParts() {
        return bodyParts;
    }

    public KeeperPart crownPart() {
        return crownPart;
    }

    /** Where the head's centre is (the entity's position is the bottom of its hitbox). */
    public Vec3 headCentre() {
        return position().add(0.0, KeeperBody.AXIS, 0.0);
    }

    /** The mouth: ahead of the head's centre along the look. */
    public Vec3 mouth() {
        return headCentre().add(getLookAngle().scale(1.6));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        tickBody();
    }

    /** The chain after the head, the whip, sagging without swimming power, and the hitboxes onto it (both sides). */
    private void tickBody() {
        Vec3 neck = KeeperBody.neck(position(), getLookAngle());
        if (!body.isPlaced()) {
            body.reset(neck, getYRot());
        }
        body.follow(neck);
        int whip = entityData.get(DATA_WHIP);
        if (whip != 0) {
            int ticks = Math.max(1, keeperConfig().whipTicks.get());
            body.swing(WHIP_PIVOT, Math.signum(whip) * WHIP_RADIANS / ticks);
        }
        float ring = entityData.get(DATA_RING_RADIUS);
        if (ring != 0.0F) {
            Vector3f centre = entityData.get(DATA_RING_CENTRE);
            body.wrap(headCentre(), centre.x, centre.y, centre.z, Math.abs(ring), ring > 0.0F ? 1 : -1, RING_LIFT, RING_GRIP);
        }
        if (isDeadOrDying() || !canSwimHere()) {
            body.settle(level(), 0.3, 0.6);
        }
        for (int i = 0; i < bodyParts.length; i++) {
            Vec3 node = body.node(PART_NODES[i]);
            place(bodyParts[i], node.x, node.y - PART_SIZES[i] / 2.0, node.z);
        }
        Vec3 crown = headCentre().add(KeeperMotion.up(getXRot(), getYRot()).scale(CROWN_UP)).subtract(getLookAngle().scale(CROWN_BACK));
        place(crownPart, crown.x, crown.y - crownPart.getBbHeight() / 2.0, crown.z);
    }

    private static void place(KeeperPart part, double x, double y, double z) {
        part.xo = part.getX();
        part.yo = part.getY();
        part.zo = part.getZ();
        part.xOld = part.xo;
        part.yOld = part.yo;
        part.zOld = part.zo;
        part.setPos(x, y, z);
    }

    /** {@link #RING_LIFT}: the part sizes spread over the nodes, the neck a little thicker, the tail tip thinner. */
    private static double[] ringLift() {
        double[] lift = new double[KeeperBody.SEGMENTS + 1];
        for (int i = 0; i <= KeeperBody.SEGMENTS; i++) {
            double size;
            if (i <= PART_NODES[0]) {
                size = PART_SIZES[0];
            } else if (i >= PART_NODES[PART_NODES.length - 1]) {
                size = PART_SIZES[PART_SIZES.length - 1] - 0.1 * (i - PART_NODES[PART_NODES.length - 1]);
            } else {
                int k = 0;
                while (PART_NODES[k + 1] < i) {
                    k++;
                }
                double t = (i - PART_NODES[k]) / (double) (PART_NODES[k + 1] - PART_NODES[k]);
                size = PART_SIZES[k] + (PART_SIZES[k + 1] - PART_SIZES[k]) * t;
            }
            lift[i] = size / 2.0;
        }
        return lift;
    }

    /** A hit on a part: the crown takes more and loses a branch; the body takes it as the head would. */
    public boolean hurtFromPart(KeeperPart part, DamageSource source, float amount) {
        if (!part.isCrown()) {
            return hurt(source, amount);
        }
        boolean hurt = hurt(source, amount * keeperConfig().crownDamageMultiplier.get().floatValue());
        if (hurt && !level().isClientSide() && source.getEntity() != null) {
            breakBranch();
        }
        return hurt;
    }

    // --- the crown ---------------------------------------------------------------------------------------------------

    /** The branches still on the crown, one bit each (bit k = {@code coral_0(k+1)} in the model). */
    public int crown() {
        return entityData.get(DATA_CROWN);
    }

    public int crownBranches() {
        return Integer.bitCount(crown());
    }

    public boolean hasCrown() {
        return crown() != 0;
    }

    /** Breaks off one of the remaining branches (not more often than the cooldown); the last one makes it fall. */
    public void breakBranch() {
        int crown = crown();
        if (crown == 0 || crownCooldown > 0 || isDeadOrDying()) {
            return;
        }
        crownCooldown = keeperConfig().crownBreakCooldownTicks.get();
        int pick = random.nextInt(Integer.bitCount(crown));
        int bit = 0;
        for (int k = 0; k < CROWN_BRANCHES; k++) {
            if ((crown & (1 << k)) != 0 && pick-- == 0) {
                bit = 1 << k;
                break;
            }
        }
        int left = crown & ~bit;
        entityData.set(DATA_CROWN, left);
        if (level() instanceof ServerLevel serverLevel) {
            KeeperEffects.branchBreaks(serverLevel, this, crownPart.position().add(0.0, crownPart.getBbHeight() / 2.0, 0.0));
            if (left == 0) {
                KeeperEffects.crownDark(serverLevel, this);
            }
        }
        triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.ROAR);
        if (left == 0 && !isInWater()) {
            falling = true;
        }
    }

    /** It swims here: always in water, in the air only while it still has a branch on its crown. */
    public boolean canSwimHere() {
        return isInWater() || hasCrown();
    }

    // --- movement ----------------------------------------------------------------------------------------------------

    /** Swim (or crawl) towards {@code target} at {@code speed} times its swim speed; 0 hovers. */
    void steer(@Nullable Vec3 target, double speed) {
        steerTarget = target;
        steerSpeed = speed;
    }

    /** Turn the head towards {@code point} while hovering (speed 0); null: look where it swims. */
    void face(@Nullable Vec3 point) {
        faceTarget = point;
    }

    void setLuring(boolean luring) {
        this.luring = luring;
    }

    /** Its turning per tick times {@code scale} (the tight ring needs more than the open sea); 1 is the config's. */
    void setTurnScale(double scale) {
        turnScale = scale;
    }

    /** All damage it has taken while alive (only grows): an attack compares it with its value at the start. */
    float damageTaken() {
        return damageTaken;
    }

    public boolean isStunned() {
        return stunTicks > 0;
    }

    public boolean isFalling() {
        return falling;
    }

    /** Free to start an attack: alive, not stunned, not falling, not holding anyone. */
    boolean canAttack() {
        return isAlive() && !isStunned() && !falling && !isHolding();
    }

    /** The fight's bookkeeping, also without AI: crown cooldown, whip, fall and stun, the hold, clouds, mist, mode. */
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying()) {
            return;
        }
        if (crownCooldown > 0) {
            crownCooldown--;
        }
        int whip = entityData.get(DATA_WHIP);
        if (whip != 0) {
            entityData.set(DATA_WHIP, whip - Integer.signum(whip));
        }
        if (stunTicks > 0) {
            stunTicks--;
        }
        tickFall();
        tickHold();
        tickClouds();
        if (tickCount % MIST_SYNC_TICKS == 0) {
            sendMist(mistStrength());
        }
        entityData.set(DATA_MODE, (byte) currentMode().ordinal());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        swimOrCrawl();
    }

    private Mode currentMode() {
        if (isStunned()) {
            return Mode.STUNNED;
        }
        if (falling) {
            return Mode.FALL;
        }
        if (isHolding()) {
            return Mode.HOLD;
        }
        if (isCoiling()) {
            return Mode.COIL;
        }
        if (luring) {
            return Mode.LURE;
        }
        return !canSwimHere() && onGround() ? Mode.CRAWL : Mode.SWIM;
    }

    public Mode mode() {
        byte mode = entityData.get(DATA_MODE);
        return mode >= 0 && mode < Mode.values().length ? Mode.values()[mode] : Mode.SWIM;
    }

    /** Crown gone in the air: it falls until it lands; on the ground it is hurt and stunned, in water only stunned. */
    private void tickFall() {
        if (!falling) {
            return;
        }
        if (isInWater() || onGround() || standsOnBlock()) {
            falling = false;
            LostKeeperConfig config = keeperConfig();
            if (isInWater()) {
                stunTicks = config.stunSeconds.get() * 10;
            } else {
                stunTicks = config.stunSeconds.get() * 20;
                // A share of its virtual max health; hurt() brings it to the real scale.
                hurt(damageSources().fall(), virtualMaxHealth() * config.landingDamage.get().floatValue());
            }
            if (level() instanceof ServerLevel serverLevel) {
                KeeperEffects.landing(serverLevel, this, isInWater());
            }
            triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.ROAR);
        }
    }

    private boolean standsOnBlock() {
        BlockPos below = BlockPos.containing(getX(), getY() - 0.1, getZ());
        return !level().getBlockState(below).getCollisionShape(level(), below).isEmpty();
    }

    /** Sets the velocity and the facing for this tick; {@link #travel} moves it. */
    private void swimOrCrawl() {
        LostKeeperConfig config = keeperConfig();
        if (isStunned() || falling) {
            return;
        }
        Vec3 head = headCentre();
        Vec3 heading = getLookAngle();
        double maxTurn = Math.toRadians(config.turnDegrees.get() * turnScale);
        if (canSwimHere()) {
            Vec3 velocity;
            if (steerTarget != null && steerSpeed > 0.0) {
                Vec3 to = steerTarget.subtract(head);
                double distance = to.length();
                Vec3 desired = distance > 1.0E-3 ? to.scale(1.0 / distance) : heading;
                // No need to look past the target: it may lie just above the sea floor (the burst's dive).
                desired = avoid(head, desired, Math.min(LOOK_AHEAD, distance + 0.5));
                heading = KeeperMotion.turn(heading, desired, maxTurn);
                double speed = config.swimSpeed.get() * steerSpeed * Math.min(1.0, distance / 3.0 + 0.25);
                velocity = heading.scale(speed);
            } else {
                Vec3 look = faceTarget != null ? faceTarget.subtract(head) : heading;
                if (look.lengthSqr() > 1.0E-6) {
                    heading = KeeperMotion.turn(heading, look.normalize(), maxTurn);
                }
                velocity = getDeltaMovement().scale(0.8);
            }
            setDeltaMovement(velocity);
            setFacing(heading);
        } else if (onGround() && steerTarget != null && steerSpeed > 0.0) {
            // Crawling: level, slow, along the ground; the chain drags behind.
            Vec3 to = steerTarget.subtract(head);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            if (flat.lengthSqr() > 1.0E-4) {
                Vec3 level = new Vec3(heading.x, 0.0, heading.z);
                level = level.lengthSqr() > 1.0E-6 ? level.normalize() : flat.normalize();
                heading = KeeperMotion.turn(level, flat.normalize(), maxTurn);
                double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * steerSpeed;
                setDeltaMovement(heading.x * speed, getDeltaMovement().y, heading.z * speed);
                setFacing(heading);
            }
        } else if (faceTarget != null) {
            Vec3 flat = faceTarget.subtract(head).multiply(1.0, 0.0, 1.0);
            if (flat.lengthSqr() > 1.0E-4) {
                setFacing(KeeperMotion.turn(new Vec3(heading.x, 0.0, heading.z).normalize(), flat.normalize(), maxTurn));
            }
        }
    }

    /** A heading that does not run its head into a block within {@code ahead} blocks: up, then sideways, then down. */
    private Vec3 avoid(Vec3 head, Vec3 desired, double ahead) {
        if (clear(head, desired, ahead)) {
            return desired;
        }
        Vec3 side = desired.cross(new Vec3(0.0, 1.0, 0.0));
        side = side.lengthSqr() > 1.0E-6 ? side.normalize() : new Vec3(1.0, 0.0, 0.0);
        Vec3[] tries = {
                desired.add(0.0, 1.2, 0.0).normalize(),
                desired.add(side.scale(1.2)).normalize(),
                desired.add(side.scale(-1.2)).normalize(),
                new Vec3(0.0, 1.0, 0.0),
                desired.add(0.0, -1.2, 0.0).normalize()};
        for (Vec3 attempt : tries) {
            if (clear(head, attempt, ahead)) {
                return attempt;
            }
        }
        return new Vec3(0.0, 1.0, 0.0);
    }

    private boolean clear(Vec3 from, Vec3 direction, double ahead) {
        Vec3 to = from.add(direction.scale(ahead));
        return level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType()
                == HitResult.Type.MISS;
    }

    private void setFacing(Vec3 heading) {
        if (heading.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 unit = heading.normalize();
        setYRot(KeeperMotion.yaw(unit));
        setXRot(KeeperMotion.pitch(unit));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public void travel(Vec3 input) {
        if (!isControlledByLocalInstance()) {
            return;
        }
        boolean swimming = !isDeadOrDying() && canSwimHere() && !falling;
        Vec3 velocity = getDeltaMovement();
        if (!swimming) {
            // Without swimming power: gravity; in water it only sinks slowly.
            velocity = velocity.add(0.0, isInWater() ? -0.01 : -0.08, 0.0);
        }
        move(MoverType.SELF, velocity);
        Vec3 after = getDeltaMovement();
        if (swimming) {
            setDeltaMovement(after.scale(0.9));
        } else if (onGround()) {
            setDeltaMovement(after.multiply(0.6, 0.98, 0.6));
        } else {
            setDeltaMovement(after.scale(isInWater() ? 0.8 : 0.98));
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    // --- holding a fighter under water -------------------------------------------------------------------------------

    public boolean isHolding() {
        return !getPassengers().isEmpty();
    }

    boolean isReleasing() {
        return releasing;
    }

    /** Coils round {@code player} (a passenger that cannot get off by sneaking: {@link KeeperHoldRules}). */
    public boolean hold(Player player) {
        player.stopRiding();
        if (!player.startRiding(this, true)) {
            return false;
        }
        holdTicks = keeperConfig().holdSeconds.get() * 20;
        heldDamage = 0.0F;
        return true;
    }

    /** Lets go of whoever it holds, with a push up and away. */
    public void release() {
        if (!isHolding()) {
            return;
        }
        List<Entity> held = List.copyOf(getPassengers());
        releasing = true;
        try {
            ejectPassengers();
        } finally {
            releasing = false;
        }
        for (Entity entity : held) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.6, 0.0));
            entity.hurtMarked = true;
        }
        holdTicks = 0;
    }

    private void tickHold() {
        if (!isHolding()) {
            return;
        }
        LostKeeperConfig config = keeperConfig();
        if (--holdTicks <= 0 || isDeadOrDying() || isStunned()) {
            release();
            return;
        }
        if (holdTicks % 20 == 0) {
            for (Entity entity : getPassengers()) {
                entity.hurt(damageSources().mobAttack(this), config.squeezeDamage.get().floatValue() * strength());
            }
        }
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        Vec3 coil = body.node(HOLD_NODE);
        callback.accept(passenger, coil.x, coil.y - passenger.getBbHeight() * 0.5, coil.z);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide()) {
            damageTaken += amount;
        }
        if (hurt && !level().isClientSide() && isHolding()) {
            heldDamage += amount;
            if (heldDamage >= keeperConfig().freeDamage.get()) {
                release();
                triggerAnim(KeeperAnimations.ACTION, KeeperAnimations.ROAR);
            }
        }
        return hurt;
    }

    // --- the tail whip -----------------------------------------------------------------------------------------------

    /** Starts the sweep of the rear half: {@code side} 1 swings from +x towards +z, -1 the other way. */
    void startWhip(int side) {
        entityData.set(DATA_WHIP, (side >= 0 ? 1 : -1) * Math.max(1, keeperConfig().whipTicks.get()));
    }

    boolean isWhipping() {
        return entityData.get(DATA_WHIP) != 0;
    }

    // --- the ring of its body ----------------------------------------------------------------------------------------

    /**
     * Coils its body on a ring round {@code centre} (x, z; its y the floor: the ground, or a block under the water's
     * surface) of {@code radius} blocks, swimming round in {@code direction} (1: angle growing, from +x towards +z).
     * Synced, so the client lays its body on the same ring.
     */
    public void coil(Vec3 centre, double radius, int direction) {
        entityData.set(DATA_RING_CENTRE, new Vector3f((float) centre.x, (float) centre.y, (float) centre.z));
        entityData.set(DATA_RING_RADIUS, (float) (direction >= 0 ? radius : -radius));
    }

    public void uncoil() {
        entityData.set(DATA_RING_RADIUS, 0.0F);
    }

    public boolean isCoiling() {
        return entityData.get(DATA_RING_RADIUS) != 0.0F;
    }

    /** The height of a node's axis above the ring's floor while coiling. */
    static double ringLift(int node) {
        return RING_LIFT[node];
    }

    // --- poison clouds -----------------------------------------------------------------------------------------------

    public void addCloud(Vec3 centre) {
        LostKeeperConfig config = keeperConfig();
        if (config.cloudSeconds.get() > 0) {
            clouds.add(new KeeperPoisonCloud(centre, config.cloudRadius.get(), config.cloudSeconds.get() * 20));
        }
    }

    private void tickClouds() {
        if (clouds.isEmpty() || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float dose = keeperConfig().cloudPoison.get().floatValue() * strength();
        clouds.removeIf(cloud -> !cloud.tick(serverLevel, dose));
    }

    // --- the mist ----------------------------------------------------------------------------------------------------

    /** 0–1: the mist rises over the first seconds after it appears. */
    private float mistStrength() {
        return isDeadOrDying() ? 0.0F : Math.min(1.0F, tickCount / (float) MIST_RISE_TICKS);
    }

    /** Its mist bank to every player near enough (the client fades a bank that stops coming). */
    private void sendMist(float strength) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        LostKeeperConfig config = keeperConfig();
        double range = config.mistSyncRange.get();
        Vec3 centre = headCentre();
        KeeperMistPayload payload = new KeeperMistPayload(getId(), centre.x, centre.y, centre.z, config.mistRadius.get().floatValue(),
                config.mistFade.get().floatValue(), config.mistVisibility.get().floatValue(), strength);
        for (ServerPlayer player : serverLevel.players()) {
            // A connection without our channel (a GameTest mock player) gets nothing.
            if (player.distanceToSqr(centre) <= range * range && player.connection.hasChannel(KeeperMistPayload.TYPE)) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide()) {
            release();
            sendMist(0.0F);
        }
        super.remove(reason);
    }

    @Override
    protected void onRetreat(ServerLevel level) {
        KeeperEffects.retreat(level, this);
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) {
            release();
            // Dead, its goals no longer stop: the body falls off the ring here.
            uncoil();
            sendMist(0.0F);
        }
        super.die(source);
    }

    // --- the rest ----------------------------------------------------------------------------------------------------

    @Override
    public AABB getBoundingBoxForCulling() {
        // The whole chain, with room for the crest, the wings and the tail fin.
        AABB box = getBoundingBox();
        if (body.isPlaced()) {
            for (int i = 0; i <= KeeperBody.SEGMENTS; i++) {
                Vec3 node = body.node(i);
                box = box.minmax(new AABB(node, node));
            }
        }
        return box.inflate(3.0);
    }

    @Override
    public boolean isInWall() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Crown", crown());
        tag.putBoolean("Falling", falling);
        tag.putInt("StunTicks", stunTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Crown")) {
            entityData.set(DATA_CROWN, tag.getInt("Crown") & CROWN_FULL);
        }
        falling = tag.getBoolean("Falling");
        stunTicks = Mth.clamp(tag.getInt("StunTicks"), 0, 20 * 60);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, KeeperAnimations.BODY, 6, this::bodyAnimation));
        controllers.add(new AnimationController<>(this, KeeperAnimations.ACTION, 3, state -> PlayState.STOP)
                .triggerableAnim(KeeperAnimations.BREATH, KeeperAnimations.BREATH_ANIM)
                .triggerableAnim(KeeperAnimations.GRAB, KeeperAnimations.GRAB_ANIM)
                .triggerableAnim(KeeperAnimations.TAIL_WHIP, KeeperAnimations.TAIL_WHIP_ANIM)
                .triggerableAnim(KeeperAnimations.ROAR, KeeperAnimations.ROAR_ANIM)
                .triggerableAnim(KeeperAnimations.BUBBLE_CALL, KeeperAnimations.BUBBLE_CALL_ANIM)
                .triggerableAnim(KeeperAnimations.DIVE, KeeperAnimations.DIVE_ANIM)
                .triggerableAnim(KeeperAnimations.BURST, KeeperAnimations.BURST_ANIM)
                .triggerableAnim(KeeperAnimations.SQUEEZE, KeeperAnimations.SQUEEZE_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheLostKeeper> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(KeeperAnimations.DEATH);
        }
        return state.setAndContinue(switch (mode()) {
            case LURE -> KeeperAnimations.LURE;
            case HOLD -> KeeperAnimations.HOLD;
            case FALL -> KeeperAnimations.FALL;
            case STUNNED -> KeeperAnimations.STUNNED;
            case CRAWL -> KeeperAnimations.CRAWL;
            case COIL -> KeeperAnimations.COIL;
            case SWIM -> getDeltaMovement().lengthSqr() > 0.01 ? KeeperAnimations.SWIM : KeeperAnimations.HOVER;
        });
    }
}
