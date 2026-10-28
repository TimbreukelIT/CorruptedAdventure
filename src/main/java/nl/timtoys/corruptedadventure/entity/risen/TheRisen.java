package nl.timtoys.corruptedadventure.entity.risen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModMobEffects;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * De Herrijzende, the ring 3 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A colossal ash
 * phoenix with a choir of its victims in its chest. It flies with its own flight AI, like the ender dragon: it circles
 * its target, dives at it, lands beside it now and then (beak, wing buffet), rains ash that holds players fast
 * ({@link RisenAshRain}), screams with all the choir's voices ({@link RisenScream}) and lets victims fall from its
 * feathers as ash walkers. Summoned on the ground, it melts the ground and rises from the lava ({@link RisenMeltPool}).
 * The volcanite horn's effect {@code corruptedadventure:stunned} makes it rigid; in the air it falls. Its
 * wings and head are extra hitboxes ({@link RisenPart}). Boss bar, health scaling, retreat and the rest of the shared
 * rules come from {@link Nightmare}; its state ({@link RisenState}) is synced for the animations. Roaming, the arrival
 * from afar and the return come in steps 2 and 3; so does the cast a player held fast in ash leaves on death.
 */
public class TheRisen extends Nightmare {
    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(TheRisen.class, EntityDataSerializers.BYTE);
    /** How fast its velocity turns toward where it wants to go, per tick. */
    private static final double STEER = 0.12;
    private static final double DIVE_STEER = 0.3;
    /** Summoned within this many blocks of the ground, it rises from it; higher up it just flies. */
    private static final double RISE_FROM_GROUND = 3.0;
    /** How deep under the ground it starts to rise: the crest of the model is ~15 blocks up. */
    private static final double RISE_DEPTH = 16.0;
    private static final double ASH_RAIN_CIRCLE = 3.0;
    private static final double LAND_DISTANCE = 5.0;
    private static final int WALKER_SEARCH = 48;
    private static final int GROUND_ATTACK_COOLDOWN = 20;

    private final RisenPart wingLeft;
    private final RisenPart wingRight;
    private final RisenPart head;
    private final RisenPart[] parts;

    private int stateTicks;
    private boolean started;
    @Nullable
    private BlockPos anchor;
    @Nullable
    private RisenMeltPool pool;
    private int poolTicks;

    private double orbitAngle;
    private int flightTicks;
    private int nextDive = -1;
    private int nextAshRain;
    private int nextScream;
    private int nextRelease;
    private int ashRainTicks;
    private int stunImmuneUntil;
    private int stunGraceTicks;
    private int peckWindup;
    private int buffetWindup;
    private int groundCooldown;
    private Vec3 diveTarget = Vec3.ZERO;
    private final Set<UUID> diveHits = new HashSet<>();
    private Vec3 landingSpot = Vec3.ZERO;

    public TheRisen(EntityType<? extends TheRisen> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        wingLeft = new RisenPart(this, 7.0F, 2.5F, true);
        wingRight = new RisenPart(this, 7.0F, 2.5F, true);
        head = new RisenPart(this, 3.0F, 3.0F, false);
        parts = new RisenPart[]{wingLeft, wingRight, head};
        // As the ender dragon: the parts' ids follow the Risen's, so the client makes the same ones.
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        applyPhysics(RisenState.CIRCLING);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return nightmareAttributes(RisenConfig.DEFAULTS, 64.0);
    }

    private static RisenConfig settings() {
        return ServerConfig.RISEN;
    }

    @Override
    protected NightmareConfig config() {
        return settings().common;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) RisenState.CIRCLING.ordinal());
    }

    @Override
    protected void registerGoals() {
        // It moves itself (customServerAiStep); goals only pick the target, through walls: it sees from the sky.
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public RisenState state() {
        return RisenState.byId(entityData.get(DATA_STATE));
    }

    private void setState(RisenState state) {
        entityData.set(DATA_STATE, (byte) state.ordinal());
        stateTicks = 0;
        applyPhysics(state);
        if (state.grounded()) {
            liftOutOfGround();
        }
    }

    /** It flies through hills; landing or stunned there, it comes out on top instead of stuck in the rock. */
    private void liftOutOfGround() {
        double ground = groundY(getX(), getZ());
        if (getY() < ground) {
            setPos(getX(), ground, getZ());
        }
    }

    /** Flying: no gravity and through blocks, like the dragon; on the ground (landed, stunned): as any mob. */
    private void applyPhysics(RisenState state) {
        setNoGravity(!state.grounded());
        noPhysics = !state.grounded();
    }

    // --- parts -------------------------------------------------------------------------------------------------------

    @Override
    public void setId(int id) {
        super.setId(id);
        // The constructor sets the parts' ids itself; this runs once before they exist.
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

    /** Wings out to the sides at shoulder height and the head up front, roughly where the model has them. */
    private void placeParts() {
        boolean flying = !state().grounded() && state() != RisenState.RISING;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double sideX = Mth.cos(yaw);
        double sideZ = Mth.sin(yaw);
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        double span = flying ? 6.5 : 5.5;
        double wingY = flying ? 5.0 : 6.0;
        place(wingLeft, sideX * span, wingY, sideZ * span);
        place(wingRight, -sideX * span, wingY, -sideZ * span);
        double reach = flying ? 3.5 : 1.5;
        place(head, forwardX * reach, flying ? 7.5 : 9.5, forwardZ * reach);
    }

    private void place(RisenPart part, double dx, double dy, double dz) {
        Vec3 old = part.position();
        part.setPos(getX() + dx, getY() + dy, getZ() + dz);
        part.xo = old.x;
        part.yo = old.y;
        part.zo = old.z;
        part.xOld = old.x;
        part.yOld = old.y;
        part.zOld = old.z;
    }

    /** A hit on a part: the wings take {@code wingDamageFactor} of it. */
    boolean hurt(RisenPart part, DamageSource source, float amount) {
        // The client hits too (Player.attack); only the server judges, and only it reads the server config.
        if (level().isClientSide()) {
            return false;
        }
        return hurt(source, part.isWing() ? amount * settings().wingDamageFactor.get().floatValue() : amount);
    }

    // --- ticking -----------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        placeParts();
        if (level() instanceof ServerLevel serverLevel && pool != null && !pool.isSet() && poolTicks > 0 && --poolTicks == 0) {
            pool.set(serverLevel);
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (state().grounded() || isDeadOrDying()) {
            super.travel(input);
            return;
        }
        // Flying: its own steering sets the velocity; no gravity, no friction, through blocks.
        move(MoverType.SELF, getDeltaMovement());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!started) {
            start(level);
        }
        stateTicks++;
        if (state() != RisenState.STUNNED && state() != RisenState.RISING && isStunned() && tickCount >= stunImmuneUntil) {
            stunGraceTicks = 0;
            setState(RisenState.STUNNED);
            RisenEffects.stunned(level, this);
        }
        switch (state()) {
            case RISING -> rising(level);
            case CIRCLING -> circling(level);
            case DIVING -> diving(level);
            case LANDING -> landing(level);
            case LANDED -> landed(level);
            case TAKING_OFF -> takingOff();
            case SCREAMING -> screaming(level);
            case STUNNED -> stunned();
        }
    }

    /** The volcanite horn's effect (stream I); its general part already stops the target and the speed. */
    private boolean isStunned() {
        return hasEffect(ModMobEffects.STUNNED);
    }

    /** First tick: near the ground it rises from it, higher up it just flies. */
    private void start(ServerLevel level) {
        started = true;
        anchor = blockPosition();
        double ground = groundY(getX(), getZ());
        if (getY() - ground > RISE_FROM_GROUND) {
            setState(RisenState.CIRCLING);
            return;
        }
        BlockPos centre = BlockPos.containing(getX(), ground - 1, getZ());
        if (settings().meltPool.get()) {
            pool = RisenMeltPool.plan(level, centre, settings().meltPoolRadius.get());
            pool.glow(level);
        }
        setState(RisenState.RISING);
        setInvisible(true);
        setPos(getX(), ground - RISE_DEPTH, getZ());
        setDeltaMovement(Vec3.ZERO);
    }

    private void rising(ServerLevel level) {
        BlockPos centre = pool != null ? pool.centre() : anchor != null ? anchor.below() : blockPosition();
        if (stateTicks < RisenAnimations.RISE_GLOW_TICKS) {
            if (stateTicks % 10 == 1) {
                RisenEffects.groundGlow(level, centre, settings().meltPoolRadius.get());
            }
            return;
        }
        if (stateTicks == RisenAnimations.RISE_GLOW_TICKS) {
            setInvisible(false);
            if (pool != null) {
                pool.open(level);
            }
            RisenEffects.poolOpens(level, centre);
        }
        setDeltaMovement(0.0, RISE_DEPTH / RisenAnimations.RISE_TICKS, 0.0);
        if (stateTicks >= RisenAnimations.RISE_GLOW_TICKS + RisenAnimations.RISE_TICKS) {
            RisenEffects.risen(level, this);
            poolTicks = settings().meltPoolSeconds.get() * 20;
            setState(RisenState.TAKING_OFF);
        }
    }

    @Nullable
    private LivingEntity fightTarget() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target instanceof Player player && !isFighter(player)) {
            return null;
        }
        return target;
    }

    private void circling(ServerLevel level) {
        RisenConfig config = settings();
        LivingEntity target = fightTarget();
        Vec3 centre = target != null ? target.position() : anchor != null ? Vec3.atBottomCenterOf(anchor) : position();
        boolean raining = ashRainTicks > 0;
        double radius = raining ? ASH_RAIN_CIRCLE : config.circleRadius.get();
        double speed = config.flySpeed.get();
        if (stateTicks == 1) {
            orbitAngle = Math.atan2(getZ() - centre.z, getX() - centre.x);
        }
        orbitAngle += speed / Math.max(radius, ASH_RAIN_CIRCLE * 2);
        double x = centre.x + Math.cos(orbitAngle) * radius;
        double z = centre.z + Math.sin(orbitAngle) * radius;
        double y = Math.max(groundY(x, z), groundY(centre.x, centre.z)) + config.flightHeight.get();
        steer(new Vec3(x, y, z), speed, STEER);
        flightTicks++;
        if (tickCount % 48 == 0) {
            RisenEffects.flap(level, this);
        }
        if (tickCount % 5 == 0) {
            RisenEffects.embers(level, this);
        }
        if (raining) {
            int tick = config.ashRainSeconds.get() * 20 - ashRainTicks;
            if (tick % 20 == 0) {
                triggerAnim(RisenAnimations.ACTION, RisenAnimations.ASH_RAIN);
            }
            RisenAshRain.rain(level, this, tick, config);
            ashRainTicks--;
            return;
        }
        if (target == null) {
            nextDive = -1;
            return;
        }
        if (nextDive < 0) {
            // The first of everything comes an interval into the fight, not at once.
            nextDive = tickCount + config.diveCooldownTicks(random.nextInt(1 << 16));
            nextAshRain = tickCount + config.ashRainIntervalTicks(random.nextInt(1 << 16));
            nextScream = tickCount + config.screamCooldownTicks(random.nextInt(1 << 16));
            nextRelease = tickCount + config.releaseCooldownTicks(random.nextInt(1 << 16));
            flightTicks = 0;
        }
        if (tickCount >= nextScream) {
            setState(RisenState.SCREAMING);
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.SCREAM);
            RisenEffects.screamWindup(level, this);
        } else if (flightTicks >= config.landInterval.get() * 20) {
            startLanding(target);
        } else if (tickCount >= nextDive) {
            diveTarget = target.position();
            diveHits.clear();
            setState(RisenState.DIVING);
            RisenEffects.diveScreech(level, this);
        } else if (tickCount >= nextAshRain) {
            ashRainTicks = config.ashRainSeconds.get() * 20;
            nextAshRain = tickCount + ashRainTicks + config.ashRainIntervalTicks(random.nextInt(1 << 16));
        } else if (tickCount >= nextRelease) {
            nextRelease = tickCount + config.releaseCooldownTicks(random.nextInt(1 << 16));
            release(level, target);
        }
    }

    private void diving(ServerLevel level) {
        RisenConfig config = settings();
        LivingEntity target = fightTarget();
        // It follows its prey until it is close, then commits.
        if (target != null && target.distanceToSqr(this) > 64.0) {
            diveTarget = target.position();
        }
        steer(diveTarget.add(0.0, 0.5, 0.0), config.diveSpeed.get(), DIVE_STEER);
        for (Player player : fighters()) {
            if (player.getBoundingBox().intersects(getBoundingBox().inflate(1.0)) && diveHits.add(player.getUUID())) {
                if (doHurtTarget(player)) {
                    RisenEffects.talonHit(level, player);
                }
            }
        }
        if (position().distanceToSqr(diveTarget) < 4.0 || getY() <= groundY(getX(), getZ()) + 1.0 || stateTicks > 80) {
            nextDive = tickCount + config.diveCooldownTicks(random.nextInt(1 << 16));
            setState(RisenState.CIRCLING);
        }
    }

    private void startLanding(LivingEntity target) {
        Vec3 away = position().subtract(target.position()).multiply(1.0, 0.0, 1.0);
        Vec3 offset = away.lengthSqr() < 1.0E-4 ? new Vec3(LAND_DISTANCE, 0.0, 0.0) : away.normalize().scale(LAND_DISTANCE);
        double x = target.getX() + offset.x;
        double z = target.getZ() + offset.z;
        landingSpot = new Vec3(x, groundY(x, z), z);
        setState(RisenState.LANDING);
    }

    private void landing(ServerLevel level) {
        steer(landingSpot, settings().flySpeed.get() * 0.8, STEER);
        if (position().distanceToSqr(landingSpot) < 2.25 || stateTicks > 200) {
            setDeltaMovement(Vec3.ZERO);
            setState(RisenState.LANDED);
            groundCooldown = GROUND_ATTACK_COOLDOWN;
            RisenEffects.land(level, this);
        }
    }

    private void landed(ServerLevel level) {
        RisenConfig config = settings();
        setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        LivingEntity target = fightTarget();
        if (target != null) {
            face(target.position());
        }
        if (peckWindup > 0) {
            if (--peckWindup == 0 && target != null && inBeakReach(target)) {
                target.hurt(damageSources().mobAttack(this), config.peckDamage.get().floatValue() * strength());
            }
            return;
        }
        if (buffetWindup > 0) {
            if (--buffetWindup == 0) {
                buffet(level);
            }
            return;
        }
        if (stateTicks >= config.landedSeconds.get() * 20 || target == null) {
            setState(RisenState.TAKING_OFF);
            return;
        }
        if (--groundCooldown > 0) {
            return;
        }
        double near = config.buffetRadius.get() * 0.6;
        if (fighters(near).size() >= 2 || !fighters(near).isEmpty() && random.nextInt(3) == 0) {
            buffetWindup = RisenAnimations.BUFFET_WINDUP_TICKS;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.BUFFET);
            groundCooldown = GROUND_ATTACK_COOLDOWN * 2;
        } else if (inBeakReach(target)) {
            peckWindup = RisenAnimations.PECK_WINDUP_TICKS;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.PECK);
            RisenEffects.peck(level, this);
            groundCooldown = GROUND_ATTACK_COOLDOWN;
        }
    }

    private boolean inBeakReach(LivingEntity target) {
        double reach = settings().peckReach.get();
        Vec3 look = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 to = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        return getBoundingBox().inflate(reach, 1.0, reach).intersects(target.getBoundingBox())
                && (to.lengthSqr() < 1.0E-4 || look.dot(to.normalize()) > 0.3);
    }

    private void buffet(ServerLevel level) {
        RisenConfig config = settings();
        double radius = config.buffetRadius.get();
        RisenEffects.buffet(level, this, radius);
        for (Player player : fighters(radius)) {
            player.hurt(damageSources().mobAttack(this), config.buffetDamage.get().floatValue() * strength());
            player.knockback(config.buffetKnockback.get(), getX() - player.getX(), getZ() - player.getZ());
            player.hurtMarked = true;
        }
    }

    private void takingOff() {
        double top = groundY(getX(), getZ()) + settings().flightHeight.get();
        steer(new Vec3(getX(), top, getZ()), 0.5, STEER);
        if (getY() >= top - 1.0 || stateTicks > 80) {
            flightTicks = 0;
            setState(RisenState.CIRCLING);
        }
    }

    private void screaming(ServerLevel level) {
        RisenConfig config = settings();
        setDeltaMovement(getDeltaMovement().scale(0.8));
        int windup = config.screamWindupTicks.get();
        if (stateTicks == windup) {
            RisenScream.scream(level, this, config);
        }
        if (stateTicks >= windup + 16) {
            nextScream = tickCount + config.screamCooldownTicks(random.nextInt(1 << 16));
            setState(RisenState.CIRCLING);
        }
    }

    /** Rigid: it falls if it was flying and lies until the horn's effect and the grace have run out. */
    private void stunned() {
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
        if (isStunned()) {
            stunGraceTicks = 0;
            return;
        }
        if (onGround() && ++stunGraceTicks >= settings().stunGraceSeconds.get() * 20) {
            stunImmuneUntil = tickCount + settings().stunImmunitySeconds.get() * 20;
            setState(RisenState.TAKING_OFF);
        }
    }

    /** Figures fall from its feathers and walk as ash walkers. */
    private void release(ServerLevel level, LivingEntity target) {
        RisenConfig config = settings();
        int walkers = level.getEntitiesOfClass(AshWalker.class, getBoundingBox().inflate(WALKER_SEARCH)).size();
        int count = Math.min(config.releaseCount.get(), config.maxWalkers.get() - walkers);
        if (count <= 0) {
            return;
        }
        triggerAnim(RisenAnimations.ACTION, RisenAnimations.RELEASE);
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        for (int i = 0; i < count; i++) {
            AshWalker walker = ModEntityTypes.ASH_WALKER.get().create(level);
            if (walker == null) {
                continue;
            }
            double side = (i % 2 == 0 ? 1 : -1) * (4.0 + random.nextDouble() * 2.0);
            walker.moveTo(getX() + Mth.cos(yaw) * side, getY() + 4.0, getZ() + Mth.sin(yaw) * side, random.nextFloat() * 360.0F, 0.0F);
            walker.setTarget(target);
            level.addFreshEntity(walker);
            RisenEffects.release(level, walker);
        }
    }

    private void steer(Vec3 point, double speed, double turn) {
        Vec3 to = point.subtract(position());
        double distance = to.length();
        Vec3 wanted = distance < 1.0E-3 ? Vec3.ZERO : to.scale(Math.min(speed, distance) / distance);
        setDeltaMovement(getDeltaMovement().add(wanted.subtract(getDeltaMovement()).scale(turn)));
        Vec3 velocity = getDeltaMovement();
        if (velocity.horizontalDistanceSqr() > 1.0E-3) {
            face(position().add(velocity));
        }
    }

    private void face(Vec3 point) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        if (dx * dx + dz * dz < 1.0E-4) {
            return;
        }
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 8.0F));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private double groundY(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
    }

    // --- death, retreat, saving --------------------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        super.die(source);
        // Its body falls out of the sky.
        setNoGravity(false);
        noPhysics = false;
        liftOutOfGround();
    }

    @Override
    protected void onRetreat(ServerLevel level) {
        RisenEffects.retreat(level, this);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // Dead or gone: its lava never stays behind. Unloaded with its chunk, the pool is saved with it.
        if (reason.shouldDestroy() && level() instanceof ServerLevel serverLevel && pool != null) {
            pool.set(serverLevel);
        }
        super.remove(reason);
    }

    @Override
    protected int deathTicks() {
        return RisenAnimations.DEATH_TICKS;
    }

    @Override
    protected double modelOverhang() {
        // Spanwijdte ~25 at render scale 0.75, the hitbox is 3.5 wide.
        return 11.0;
    }

    @Override
    protected double modelOverhangUp() {
        // The crest reaches ~15 blocks, the hitbox 8.5.
        return 7.0;
    }

    @Override
    protected double deadOverhang() {
        return 12.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("RisenState", (byte) state().ordinal());
        tag.putInt("RisenStateTicks", stateTicks);
        tag.putBoolean("RisenStarted", started);
        if (anchor != null) {
            tag.putLong("RisenAnchor", anchor.asLong());
        }
        if (pool != null) {
            tag.put("RisenPool", pool.save());
            tag.putInt("RisenPoolTicks", poolTicks);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        started = tag.getBoolean("RisenStarted");
        if (tag.contains("RisenState", Tag.TAG_BYTE)) {
            // Not setState: no world lookups while loading.
            RisenState state = RisenState.byId(tag.getByte("RisenState"));
            entityData.set(DATA_STATE, (byte) state.ordinal());
            applyPhysics(state);
            stateTicks = tag.getInt("RisenStateTicks");
        }
        anchor = tag.contains("RisenAnchor", Tag.TAG_LONG) ? BlockPos.of(tag.getLong("RisenAnchor")) : null;
        if (tag.contains("RisenPool", Tag.TAG_COMPOUND)) {
            pool = RisenMeltPool.load(tag.getCompound("RisenPool"));
            poolTicks = tag.getInt("RisenPoolTicks");
        }
    }

    // --- animation ---------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, RisenAnimations.BODY, 6, this::bodyAnimation));
        controllers.add(new AnimationController<>(this, RisenAnimations.ACTION, 3, state -> PlayState.STOP)
                .triggerableAnim(RisenAnimations.SCREAM, RisenAnimations.SCREAM_ANIM)
                .triggerableAnim(RisenAnimations.ASH_RAIN, RisenAnimations.ASH_RAIN_ANIM)
                .triggerableAnim(RisenAnimations.RELEASE, RisenAnimations.RELEASE_ANIM)
                .triggerableAnim(RisenAnimations.PECK, RisenAnimations.PECK_ANIM)
                .triggerableAnim(RisenAnimations.BUFFET, RisenAnimations.BUFFET_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheRisen> animationState) {
        return animationState.setAndContinue(bodyLoop());
    }

    private RawAnimation bodyLoop() {
        if (isDeadOrDying()) {
            return RisenAnimations.DEATH;
        }
        return switch (state()) {
            case RISING -> RisenAnimations.RISE;
            case CIRCLING -> RisenAnimations.FLY;
            case DIVING -> RisenAnimations.DIVE;
            case LANDING, TAKING_OFF, SCREAMING -> RisenAnimations.HOVER;
            case LANDED -> RisenAnimations.PERCHED;
            case STUNNED -> RisenAnimations.STUNNED;
        };
    }
}
