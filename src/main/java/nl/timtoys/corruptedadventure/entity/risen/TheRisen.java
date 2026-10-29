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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.RisenConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmarePacing;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareTargetGoal;
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
    /** The swoop climbs to a point this far back from its target per block of height: a steep, readable line. */
    private static final double SWOOP_SLANT = 0.6;
    /** Height above the ground at which it skims after the swoop. */
    private static final double SKIM_HEIGHT = 1.0;
    /** After a big attack (swoop, ember rain) the quick ones wait at least this long. */
    private static final int BREATHER = 80;
    /** A rest window that backs off: it circles this much wider and higher. */
    private static final double REST_WIDEN = 1.5;
    private static final double REST_CLIMB = 8.0;
    /** Searching, it circles low and tight over the last known place; there it looks around. */
    private static final double SEARCH_CIRCLE = 8.0;
    private static final double SEARCH_HEIGHT = 0.6;
    private static final double SEARCH_ARRIVE = 12.0;
    /** Staggering in the air, it sags (blocks per tick) for this many ticks, but never below this above the ground. */
    private static final double STAGGER_SAG = 0.18;
    private static final int STAGGER_SAG_TICKS = 30;
    private static final double STAGGER_FLOOR = 2.0;

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
    private int stompWindup;
    private int landAfter;
    private boolean landAfterBurst;
    private int groundCooldown;
    private Vec3 diveTarget = Vec3.ZERO;
    private final Set<UUID> diveHits = new HashSet<>();
    private Vec3 landingSpot = Vec3.ZERO;
    private final RisenFire fire = new RisenFire();
    private final RisenEmberRain emberRain = new RisenEmberRain();
    private int nextSwoop;
    private int nextEmberRain;
    private boolean swoopWinding;
    private int swoopWindupStart;
    private Vec3 swoopAim = Vec3.ZERO;
    private Vec3 swoopDirection = Vec3.ZERO;
    @Nullable
    private Vec3 skimFrom;
    private Vec3 skimDirection = Vec3.ZERO;

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
        // Walking on the ground in its ground fight, it steps up like a big mob.
        return nightmareAttributes(RisenConfig.DEFAULTS, 64.0).add(Attributes.STEP_HEIGHT, 1.5);
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
        // It moves itself (customServerAiStep); the shared goal only picks the target, through walls: it sees from the sky.
        targetSelector.addGoal(1, new NightmareTargetGoal(this));
    }

    public RisenState state() {
        return RisenState.byId(entityData.get(DATA_STATE));
    }

    private void setState(RisenState state) {
        entityData.set(DATA_STATE, (byte) state.ordinal());
        stateTicks = 0;
        // A ground attack broken off (stun) does not land later.
        peckWindup = 0;
        buffetWindup = 0;
        stompWindup = 0;
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
        // In its ground fight it hunches, the head low and forward: within a sword's reach.
        boolean hunched = state() == RisenState.LANDED;
        double reach = flying ? 3.5 : hunched ? 3.0 : 1.5;
        place(head, forwardX * reach, flying ? 7.5 : hunched ? 3.5 : 9.5, forwardZ * reach);
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

    /**
     * A hit on a part: the wings take {@code wingDamageFactor} of it. The head is its weak spot: a fighter's hit there
     * staggers it (the shared stagger, with its cooldown); low in the ground fight it is within a sword's reach.
     */
    boolean hurt(RisenPart part, DamageSource source, float amount) {
        // The client hits too (Player.attack); only the server judges, and only it reads the server config.
        if (level().isClientSide()) {
            return false;
        }
        boolean hurt = hurt(source, part.isWing() ? amount * settings().wingDamageFactor.get().floatValue() : amount);
        if (hurt && part == head && source.getEntity() instanceof Player player && isFighter(player)) {
            hitWeakSpot();
        }
        return hurt;
    }

    // --- ticking -----------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        placeParts();
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (pool != null && !pool.isSet() && poolTicks > 0 && --poolTicks == 0) {
            pool.set(serverLevel);
        }
        // Here and not in the AI step: falling embers still land and the fire burns on while it is stunned or dying.
        emberRain.tick(serverLevel, this, fire, settings());
        fire.tick(serverLevel, this, strength(), settings());
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
            case SOARING -> soaring(level);
            case SWOOPING -> swooping(level);
            case EMBER_RAIN -> emberRaining(level);
            case STAGGERED -> staggeredInAir();
        }
    }

    // --- reactions (leven-ronde, shared layer in Nightmare) ----------------------------------------------------------

    @Override
    protected boolean canStagger() {
        RisenState state = state();
        return state != RisenState.RISING && state != RisenState.STUNNED && !isStunned();
    }

    /** The running attack breaks off. On the ground it reels where it stands; in the air it sags, tumbling. */
    @Override
    protected void onStaggered() {
        ashRainTicks = 0;
        // A scream or dive broken off does not come again the moment it has recovered.
        RisenConfig config = settings();
        if (state() == RisenState.SCREAMING) {
            nextScream = tickCount + config.screamCooldownTicks(random.nextInt(1 << 16));
        } else if (state() == RisenState.DIVING) {
            nextDive = tickCount + config.diveCooldownTicks(random.nextInt(1 << 16));
        }
        breathe();
        if (state() == RisenState.LANDED) {
            peckWindup = 0;
            buffetWindup = 0;
            stompWindup = 0;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.STAGGER);
        } else {
            setState(RisenState.STAGGERED);
        }
    }

    @Override
    protected void onStaggerEnd() {
        if (state() == RisenState.STAGGERED) {
            setState(RisenState.TAKING_OFF);
        }
    }

    @Override
    protected void staggerEffects(ServerLevel level) {
        RisenEffects.staggered(level, this);
    }

    /** After a burst in the air it may land for a ground fight ({@code landChance}). */
    @Override
    protected void onRestStart(NightmarePacing.Style style) {
        landAfterBurst = state() != RisenState.LANDED && state() != RisenState.STUNNED
                && random.nextDouble() < settings().landChance.get();
    }

    @Override
    protected void onSniff() {
        triggerAnim(RisenAnimations.ACTION, RisenAnimations.SEARCH);
        if (level() instanceof ServerLevel level) {
            RisenEffects.sniff(level, this);
        }
    }

    /** Sagging a few blocks with the wings out of rhythm; the stagger's end ({@link #onStaggerEnd}) lifts it again. */
    private void staggeredInAir() {
        double floor = groundY(getX(), getZ()) + STAGGER_FLOOR;
        double sink = stateTicks < STAGGER_SAG_TICKS && getY() > floor ? STAGGER_SAG : 0.0;
        setDeltaMovement(0.0, -sink, 0.0);
        if (getY() < floor) {
            setPos(getX(), floor, getZ());
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
        Vec3 lastKnown = lastKnownPosition();
        boolean searching = target != null && isSearching() && lastKnown != null;
        // Out of sight it circles low over where it last saw its prey; resting after a burst it may pull wide and high.
        Vec3 centre = searching ? lastKnown : target != null ? target.position()
                : anchor != null ? Vec3.atBottomCenterOf(anchor) : position();
        boolean raining = ashRainTicks > 0;
        boolean backingOff = isResting() && restStyle() == NightmarePacing.Style.BACK_OFF;
        double radius = raining ? ASH_RAIN_CIRCLE : searching ? SEARCH_CIRCLE
                : config.circleRadius.get() * (backingOff ? REST_WIDEN : 1.0);
        double height = config.flightHeight.get() * (searching ? SEARCH_HEIGHT : 1.0) + (backingOff ? REST_CLIMB : 0.0);
        double speed = config.flySpeed.get();
        if (stateTicks == 1) {
            orbitAngle = Math.atan2(getZ() - centre.z, getX() - centre.x);
        }
        orbitAngle += speed / Math.max(radius, ASH_RAIN_CIRCLE * 2);
        double x = centre.x + Math.cos(orbitAngle) * radius;
        double z = centre.z + Math.sin(orbitAngle) * radius;
        double y = Math.max(groundY(x, z), groundY(centre.x, centre.z)) + height;
        steer(new Vec3(x, y, z), speed, STEER);
        if (searching && position().subtract(lastKnown).horizontalDistanceSqr() < SEARCH_ARRIVE * SEARCH_ARRIVE) {
            arriveAtSearch();
        }
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
            nextSwoop = tickCount + config.swoopCooldownTicks(random.nextInt(1 << 16));
            nextEmberRain = tickCount + config.emberRainCooldownTicks(random.nextInt(1 << 16));
            flightTicks = 0;
            landAfter = config.landIntervalTicks(random.nextInt(1 << 16));
        }
        if (landAfterBurst) {
            // A burst of attacks is over: it comes down for a ground fight, its rest window is the walk there.
            landAfterBurst = false;
            startLanding(target);
            return;
        }
        // The shared gate: no attack while it staggers, rests after a burst or searches.
        if (!mayStartAttack()) {
            return;
        }
        boolean started = true;
        if (tickCount >= nextScream) {
            setState(RisenState.SCREAMING);
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.SCREAM);
            RisenEffects.screamWindup(level, this);
        } else if (flightTicks >= landAfter) {
            startLanding(target);
        } else if (tickCount >= nextSwoop) {
            startSwoop(level);
        } else if (tickCount >= nextDive) {
            diveTarget = target.position();
            diveHits.clear();
            setState(RisenState.DIVING);
            RisenEffects.diveScreech(level, this);
        } else if (tickCount >= nextAshRain) {
            ashRainTicks = config.ashRainSeconds.get() * 20;
            nextAshRain = tickCount + ashRainTicks + config.ashRainIntervalTicks(random.nextInt(1 << 16));
        } else if (tickCount >= nextEmberRain) {
            startEmberRain(level, target);
        } else if (tickCount >= nextRelease) {
            nextRelease = tickCount + config.releaseCooldownTicks(random.nextInt(1 << 16));
            started = release(level, target);
        } else {
            started = false;
        }
        if (started) {
            countAttack();
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

    // --- swoop (leven-ronde) ------------------------------------------------------------------------------------------

    private void startSwoop(ServerLevel level) {
        nextSwoop = tickCount + settings().swoopCooldownTicks(random.nextInt(1 << 16));
        swoopWinding = false;
        setState(RisenState.SOARING);
        RisenEffects.swoopClimb(level, this);
    }

    /** Up to a point high above and behind its target, then hanging there flaring while it takes aim. */
    private void soaring(ServerLevel level) {
        RisenConfig config = settings();
        LivingEntity target = fightTarget();
        if (target != null) {
            swoopAim = target.position();
        } else if (!swoopWinding) {
            setState(RisenState.CIRCLING);
            return;
        }
        if (!swoopWinding) {
            double height = config.swoopHeight.get();
            Vec3 back = position().subtract(swoopAim).multiply(1.0, 0.0, 1.0);
            back = back.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : back.normalize();
            double x = swoopAim.x + back.x * height * SWOOP_SLANT;
            double z = swoopAim.z + back.z * height * SWOOP_SLANT;
            Vec3 top = new Vec3(x, groundY(swoopAim.x, swoopAim.z) + height, z);
            steer(top, config.flySpeed.get() * 1.2, STEER);
            if (stateTicks % 24 == 0) {
                RisenEffects.swoopClimb(level, this);
            }
            if (position().distanceToSqr(top) < 9.0 || stateTicks > 160) {
                swoopWinding = true;
                swoopWindupStart = stateTicks;
                triggerAnim(RisenAnimations.ACTION, RisenAnimations.SWOOP_WINDUP);
                RisenEffects.swoopWindup(level, this);
            }
            return;
        }
        setDeltaMovement(getDeltaMovement().scale(0.7));
        face(swoopAim);
        int held = stateTicks - swoopWindupStart;
        if (held % 4 == 0) {
            RisenEffects.swoopGlow(level, this);
        }
        if (held >= config.swoopWindupTicks.get()) {
            // Aimed now, where its target stands; it no longer steers after this.
            Vec3 line = swoopAim.subtract(position());
            swoopDirection = line.lengthSqr() < 1.0E-4 ? new Vec3(0.0, -1.0, 0.0) : line.normalize();
            skimFrom = null;
            diveHits.clear();
            setState(RisenState.SWOOPING);
            RisenEffects.diveScreech(level, this);
        }
    }

    /** Down the line, then along the ground a while with fire behind it; whoever it passes through is hit once. */
    private void swooping(ServerLevel level) {
        RisenConfig config = settings();
        double speed = config.swoopSpeed.get();
        if (skimFrom == null) {
            setDeltaMovement(swoopDirection.scale(speed));
            face(position().add(swoopDirection));
            boolean past = position().subtract(swoopAim).dot(swoopDirection) >= 0.0;
            if (getY() <= groundY(getX(), getZ()) + SKIM_HEIGHT + 0.5 || past || stateTicks > 100) {
                startSkim(level);
            }
        } else {
            double ground = groundY(getX(), getZ());
            double climb = Mth.clamp((ground + SKIM_HEIGHT - getY()) * 0.5, -speed, speed);
            Vec3 along = skimDirection.scale(speed * 0.75);
            setDeltaMovement(along.x, climb, along.z);
            face(position().add(skimDirection));
            layTrail(level);
            if (position().subtract(skimFrom).horizontalDistance() >= config.swoopSkimBlocks.get() || stateTicks > 160) {
                breathe();
                setState(RisenState.TAKING_OFF);
                return;
            }
        }
        for (Player player : fighters()) {
            if (player.getBoundingBox().intersects(getBoundingBox().inflate(1.0)) && diveHits.add(player.getUUID())) {
                if (player.hurt(damageSources().mobAttack(this), config.swoopDamage.get().floatValue() * strength())) {
                    player.igniteForSeconds(config.fireIgniteSeconds.get());
                    player.knockback(config.swoopKnockback.get(), -swoopDirection.x, -swoopDirection.z);
                    player.hurtMarked = true;
                    RisenEffects.swoopHit(level, player);
                }
            }
        }
    }

    private void startSkim(ServerLevel level) {
        skimFrom = position();
        Vec3 flat = swoopDirection.multiply(1.0, 0.0, 1.0);
        skimDirection = flat.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, getYRot()) : flat.normalize();
        RisenEffects.swoopImpact(level, this);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                lightUnder(level, getX() + dx, getZ() + dz);
            }
        }
    }

    /** Three tiles wide across its path. */
    private void layTrail(ServerLevel level) {
        double sideX = -skimDirection.z;
        double sideZ = skimDirection.x;
        for (int i = -1; i <= 1; i++) {
            lightUnder(level, getX() + sideX * i, getZ() + sideZ * i);
        }
    }

    private void lightUnder(ServerLevel level, double x, double z) {
        BlockPos tile = RisenFire.surfaceTile(level, x, z, getY(), 4.0);
        if (tile != null && !fire.isBurning(tile)) {
            fire.light(level, tile, settings());
        }
    }

    // --- ember rain (leven-ronde) -------------------------------------------------------------------------------------

    private void startEmberRain(ServerLevel level, LivingEntity target) {
        nextEmberRain = tickCount + settings().emberRainCooldownTicks(random.nextInt(1 << 16));
        emberRain.start(new Vec3(target.getX(), groundY(target.getX(), target.getZ()), target.getZ()));
        setState(RisenState.EMBER_RAIN);
        triggerAnim(RisenAnimations.ACTION, RisenAnimations.EMBER_RAIN);
        RisenEffects.screamWindup(level, this);
    }

    /** It hangs over the area while the rim smoulders and the embers fall from its wings. */
    private void emberRaining(ServerLevel level) {
        RisenConfig config = settings();
        Vec3 centre = emberRain.centre();
        steer(new Vec3(centre.x, centre.y + config.flightHeight.get(), centre.z), config.flySpeed.get(), STEER);
        LivingEntity target = fightTarget();
        if (target != null) {
            face(target.position());
        }
        emberRain.rain(level, stateTicks, config, random);
        int windup = config.emberRainWindupTicks.get();
        if (stateTicks > windup && (stateTicks - windup) % 20 == 1) {
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.EMBER_RAIN);
            RisenEffects.emberRainShed(level, this);
        }
        if (stateTicks >= windup + config.emberRainSeconds.get() * 20) {
            breathe();
            setState(RisenState.CIRCLING);
        }
    }

    /** After a big attack the quick ones (dive, ash rain) wait a moment, so they do not follow it at once. */
    private void breathe() {
        if (nextDive < 0) {
            // Not in a fight yet: the first interval of everything is still to be drawn.
            return;
        }
        nextDive = Math.max(nextDive, tickCount + BREATHER);
        nextAshRain = Math.max(nextAshRain, tickCount + BREATHER);
    }

    // --- landing -----------------------------------------------------------------------------------------------------

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

    /**
     * The ground fight (leven-ronde): it walks after its target, hunched with the head low; stomps when someone is
     * close, pecks what is in front of it and beats its wings only when it is surrounded. Long windups: here it is the
     * easiest to hit.
     */
    private void landed(ServerLevel level) {
        RisenConfig config = settings();
        LivingEntity target = fightTarget();
        if (isStaggered()) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            return;
        }
        // Resting after a burst it walks off or around; out of sight it walks to where it last saw its prey.
        Vec3 walkTo = target == null ? null : isResting() ? restPoint() : isSearching() ? lastKnownPosition()
                : inBeakReach(target) ? null : target.position();
        boolean busy = peckWindup > 0 || buffetWindup > 0 || stompWindup > 0;
        if (busy || walkTo == null) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (target != null) {
                face(target.position());
            }
        } else {
            getMoveControl().setWantedPosition(walkTo.x, walkTo.y, walkTo.z, config.walkSpeed.get());
            Vec3 lastKnown = lastKnownPosition();
            if (isSearching() && lastKnown != null && position().distanceToSqr(lastKnown) < 9.0) {
                arriveAtSearch();
            }
        }
        if (stompWindup > 0) {
            if (--stompWindup == 0) {
                stomp(level);
            }
            return;
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
        if (--groundCooldown > 0 || !mayStartAttack()) {
            return;
        }
        if (fighters(config.buffetRadius.get() * 0.6).size() >= 2) {
            buffetWindup = RisenAnimations.BUFFET_WINDUP_TICKS;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.BUFFET);
            groundCooldown = GROUND_ATTACK_COOLDOWN * 2;
        } else if (!fighters(config.stompRadius.get()).isEmpty() && random.nextInt(3) == 0) {
            stompWindup = RisenAnimations.STOMP_WINDUP_TICKS;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.STOMP);
            RisenEffects.stompWindup(level, this, config.stompRadius.get());
            groundCooldown = GROUND_ATTACK_COOLDOWN * 2;
        } else if (inBeakReach(target)) {
            peckWindup = RisenAnimations.PECK_WINDUP_TICKS;
            triggerAnim(RisenAnimations.ACTION, RisenAnimations.PECK);
            RisenEffects.peck(level, this);
            groundCooldown = GROUND_ATTACK_COOLDOWN;
        } else {
            return;
        }
        countAttack();
    }

    private boolean inBeakReach(LivingEntity target) {
        double reach = settings().peckReach.get();
        Vec3 look = Vec3.directionFromRotation(0.0F, getYRot());
        Vec3 to = target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        return getBoundingBox().inflate(reach, 1.0, reach).intersects(target.getBoundingBox())
                && (to.lengthSqr() < 1.0E-4 || look.dot(to.normalize()) > 0.3);
    }

    /** A shock through the ground around its feet: who stands on it is hurt and thrown up; a jump avoids it. */
    private void stomp(ServerLevel level) {
        RisenConfig config = settings();
        double radius = config.stompRadius.get();
        RisenEffects.stomp(level, this, radius);
        for (Player player : fighters(radius)) {
            if (!player.onGround() || Math.abs(player.getY() - getY()) > 1.5) {
                continue;
            }
            if (player.hurt(damageSources().mobAttack(this), config.stompDamage.get().floatValue() * strength())) {
                Vec3 away = player.position().subtract(position()).multiply(1.0, 0.0, 1.0);
                Vec3 push = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize().scale(0.4);
                player.setDeltaMovement(player.getDeltaMovement().add(push.x, config.stompToss.get(), push.z));
                player.hurtMarked = true;
            }
        }
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
            landAfterBurst = false;
            landAfter = settings().landIntervalTicks(random.nextInt(1 << 16));
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

    /** Figures fall from its feathers and walk as ash walkers; false if there are too many already. */
    private boolean release(ServerLevel level, LivingEntity target) {
        RisenConfig config = settings();
        int walkers = level.getEntitiesOfClass(AshWalker.class, getBoundingBox().inflate(WALKER_SEARCH)).size();
        int count = Math.min(config.releaseCount.get(), config.maxWalkers.get() - walkers);
        if (count <= 0) {
            return false;
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
        return true;
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
        fire.clear();
        emberRain.clear();
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
            boolean restart = state.restartsWhenLoaded();
            if (restart) {
                state = RisenState.CIRCLING;
            }
            entityData.set(DATA_STATE, (byte) state.ordinal());
            applyPhysics(state);
            stateTicks = restart ? 0 : tag.getInt("RisenStateTicks");
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
                .triggerableAnim(RisenAnimations.BUFFET, RisenAnimations.BUFFET_ANIM)
                .triggerableAnim(RisenAnimations.STOMP, RisenAnimations.STOMP_ANIM)
                .triggerableAnim(RisenAnimations.STAGGER, RisenAnimations.STAGGER_ANIM)
                .triggerableAnim(RisenAnimations.SEARCH, RisenAnimations.SEARCH_ANIM)
                .triggerableAnim(RisenAnimations.SWOOP_WINDUP, RisenAnimations.SWOOP_WINDUP_ANIM)
                .triggerableAnim(RisenAnimations.EMBER_RAIN, RisenAnimations.EMBER_RAIN_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheRisen> animationState) {
        return animationState.setAndContinue(bodyLoop(animationState.isMoving()));
    }

    private RawAnimation bodyLoop(boolean moving) {
        if (isDeadOrDying()) {
            return RisenAnimations.DEATH;
        }
        return switch (state()) {
            case RISING -> RisenAnimations.RISE;
            case CIRCLING -> RisenAnimations.FLY;
            case DIVING, SWOOPING -> RisenAnimations.DIVE;
            case LANDING, TAKING_OFF, SCREAMING, SOARING, EMBER_RAIN -> RisenAnimations.HOVER;
            case LANDED -> moving ? RisenAnimations.STALK : RisenAnimations.GROUND;
            case STUNNED -> RisenAnimations.STUNNED;
            case STAGGERED -> RisenAnimations.STAGGER_AIR;
        };
    }
}
