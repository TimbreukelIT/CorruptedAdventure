package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.director.NightmareDirector;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * What every Nightmare shares (phase 4, fase-4-nightmares-concept.md "Fase 4-sparring alle Nightmares"): a boss bar for
 * everyone who sees it; once a second, also without AI, its {@link NightmareConfig} onto the attributes with the max
 * health scaled to the players in the fight (the health ratio kept), and a check that someone is still near, else it
 * retreats without a drop; no despawn, no pushing, no fall damage, no portals; a death that lasts {@link #deathTicks()}
 * so the death animation can play; a culling box bigger than the hitbox. Server-side; the client renders
 * ({@code client/entity/nightmare/NightmareRenderer}).
 * <p>
 * A Nightmare adds its attacks as goals, its {@code registerControllers} (GeckoLib) and its attribute defaults through
 * {@link #nightmareAttributes}.
 * <p>
 * Steps 2 and 3 (the director, {@code director/NightmareDirector}): one that a director sent ({@link #direct}) has a
 * {@link #strength()} on its max health and attack damage, checks in with the director every second (a stale copy
 * removes itself), and reports its retreat and its defeat. A {@code /summon}ed one has strength 1 and reports nothing.
 * Attacks whose damage comes from their own config, not from {@code attack_damage}, multiply it by {@link #strength()}.
 * <p>
 * Vanilla caps {@code max_health} at 1024. Its full value (base × player scaling × strength, e.g. 1200 for ring 5)
 * is {@link #virtualMaxHealth()}; above the cap every hit is divided by {@link #healthScale()}, so it takes exactly as
 * many hits as the full value and the boss bar ratio stays right. Thresholds ("15% of its health") read
 * {@link #virtualMaxHealth()} and {@link #virtualHealth()}.
 * <p>
 * The shared reactions of the life round (fase-4-nightmares-concept.md "Leven-ronde"), all server-side:
 * <ul>
 *   <li><b>Stagger</b> ({@link #isStaggered()}, synced for the animation): much damage within a few seconds, or
 *   {@link #hitWeakSpot()}, and it staggers for a moment. At its start every running goal stops, goals that move,
 *   look or jump cannot start, and {@link #onStaggered()} breaks off the rest (grabs, flights, own state).</li>
 *   <li><b>Gate</b> {@link #mayStartAttack()}: not staggered, not resting, not searching. Every attack checks it before
 *   it starts, then calls {@link #countAttack()}.</li>
 *   <li><b>Pace</b>: after a burst of attacks a rest window ({@link #isResting()}) in which it backs off or circles,
 *   toward {@link #restPoint()}. Walkers add {@link NightmarePaceGoal}; others move there themselves.</li>
 *   <li><b>Search</b>: its target out of sight for a while, it goes to {@link #lastKnownPosition()}, sniffs
 *   ({@link #onSniff()}) and looks around ({@link #isSearching()}). Walkers add {@link NightmareSearchGoal}.</li>
 *   <li><b>Target</b>: {@link NightmareTargetGoal} picks who hit it hardest lately, or who is near and weak.</li>
 * </ul>
 * Own movement (flying, swimming, climbing, rift steps) overrides {@link #moveForReaction} for the two walker goals,
 * or reads the state in its own {@code customServerAiStep}.
 */
public abstract class Nightmare extends Monster implements GeoEntity {
    private static final ResourceLocation PLAYERS_MODIFIER = CorruptedAdventure.id("nightmare_players");
    private static final ResourceLocation STRENGTH_MODIFIER = CorruptedAdventure.id("nightmare_strength");
    private static final String STRENGTH_TAG = "NightmareStrength";
    private static final String DIRECTED_TAG = "NightmareDirected";
    private static final int CHECK_TICKS = 20;
    /** How often the search looks for its target (a ray per check). */
    private static final int SIGHT_TICKS = 5;
    /** Radians a circling rest window moves on per new point. */
    private static final double CIRCLE_STEP = 0.5;
    private static final double MIN_CIRCLE_RADIUS = 4.0;
    private static final EntityDataAccessor<Boolean> DATA_STAGGERED =
            SynchedEntityData.defineId(Nightmare.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent;
    private int absentTicks;
    /** Factor on max health and attack damage: 1, then +25% per defeat (the director's config). */
    private float strength = 1.0F;
    /** The director that sent it; null for one from {@code /summon} (or a spawn egg). */
    @Nullable
    private NightmareDirector director;
    private boolean checkedIn;
    private final NightmareStagger stagger = new NightmareStagger();
    private final NightmarePacing pacing = new NightmarePacing();
    private final NightmareSearch search = new NightmareSearch();
    private final NightmareAggro aggro = new NightmareAggro();

    protected Nightmare(EntityType<? extends Nightmare> type, Level level, BossEvent.BossBarColor barColor) {
        super(type, level);
        xpReward = 0;
        bossEvent = new ServerBossEvent(getDisplayName(), barColor, BossEvent.BossBarOverlay.PROGRESS);
    }

    /** The shared config block of this Nightmare; read live. */
    protected abstract NightmareConfig config();

    /**
     * Monster attributes with the given defaults; the server config overrides health, armor, speed and the attack from
     * the first second on. Knockback resistance 1: it is never pushed back by hits or explosions.
     */
    public static AttributeSupplier.Builder nightmareAttributes(NightmareConfig.Defaults defaults, double followRange) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, defaults.baseHealth())
                .add(Attributes.ATTACK_DAMAGE, defaults.attackDamage())
                .add(Attributes.ATTACK_KNOCKBACK, defaults.attackKnockback())
                .add(Attributes.MOVEMENT_SPEED, defaults.movementSpeed())
                .add(Attributes.ARMOR, defaults.armor())
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, followRange);
    }

    /** A player who counts in the fight: health scaling, attacks that pick players. Creative and spectators look on. */
    public static boolean isFighter(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    /** The fighters within {@code radius} blocks, a fresh list. */
    public List<Player> fighters(double radius) {
        List<Player> result = new ArrayList<>();
        for (Player player : level().players()) {
            if (isFighter(player) && player.distanceToSqr(this) <= radius * radius) {
                result.add(player);
            }
        }
        return result;
    }

    /** The fighters within the config's fight radius. */
    public List<Player> fighters() {
        return fighters(config().fightRadius.get());
    }

    /**
     * Its full max health: the {@code max_health} attribute before vanilla's cap of 1024 (base, player scaling, strength
     * and any other modifier). Equal to {@link #getMaxHealth()} below the cap.
     */
    public float virtualMaxHealth() {
        AttributeInstance instance = getAttribute(Attributes.MAX_HEALTH);
        return instance == null ? getMaxHealth() : (float) Math.max(getMaxHealth(), uncapped(instance));
    }

    /** Its health on the scale of {@link #virtualMaxHealth()}. */
    public float virtualHealth() {
        return getHealth() * healthScale();
    }

    /** Virtual per real health point: 1 below the cap, else {@link #virtualMaxHealth()} / {@link #getMaxHealth()}. */
    public float healthScale() {
        float max = getMaxHealth();
        return max > 0.0F ? Math.max(1.0F, virtualMaxHealth() / max) : 1.0F;
    }

    /** What {@code AttributeInstance} computes, without the attribute's range. */
    private static double uncapped(AttributeInstance instance) {
        double value = instance.getBaseValue();
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                value += modifier.amount();
            }
        }
        double base = value;
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                value += base * modifier.amount();
            }
        }
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0 + modifier.amount();
            }
        }
        return value;
    }

    /**
     * Above the health cap a hit counts on the virtual scale (see the class comment). The damage it really took feeds
     * the target choice and the stagger.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        float scale = healthScale();
        float before = getHealth();
        boolean hurt = super.hurt(source, amount / scale);
        if (hurt && !level().isClientSide() && !isDeadOrDying()) {
            float dealt = (before - getHealth()) * scale;
            if (dealt > 0.0F) {
                tookDamage(source, dealt);
            }
        }
        return hurt;
    }

    private void tookDamage(DamageSource source, float dealt) {
        if (source.getEntity() instanceof Player player && isFighter(player)) {
            aggro.add(player.getUUID(), dealt);
            if (player == getTarget() && !hasLineOfSight(player)) {
                // Hit from out of sight: it heard where that came from.
                search.heard(player.position());
            }
        }
        NightmareConfig config = config();
        float threshold = (float) (config.staggerDamageShare.get() * virtualMaxHealth());
        if (stagger.addDamage(tickCount, dealt, threshold, config.staggerWindowSeconds.get() * 20)) {
            stagger();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGGERED, false);
    }

    // ------------------------------------------------------------------------------------------------ reactions

    /** Staggering: no attack, no moving goal; the open window. Synced, so the client can animate it. */
    public boolean isStaggered() {
        return entityData.get(DATA_STAGGERED);
    }

    /** Staggers it for the config's ticks; see {@link #stagger(int)}. */
    public boolean stagger() {
        return stagger(config().staggerTicks.get());
    }

    /**
     * Staggers it for {@code ticks}, unless it is already staggering, cooling down from the last one, dying, or
     * {@link #canStagger()} says no. Every running goal stops, {@link #onStaggered()} breaks off the rest. True if it
     * staggers now.
     */
    public boolean stagger(int ticks) {
        if (level().isClientSide() || ticks <= 0 || isDeadOrDying() || !stagger.isReady(tickCount) || !canStagger()) {
            return false;
        }
        stagger.start(tickCount, ticks, config().staggerCooldownSeconds.get() * 20);
        entityData.set(DATA_STAGGERED, true);
        getNavigation().stop();
        for (WrappedGoal goal : goalSelector.getAvailableGoals()) {
            if (goal.isRunning()) {
                goal.stop();
            }
        }
        updateControlFlags();
        onStaggered();
        if (level() instanceof ServerLevel serverLevel) {
            staggerEffects(serverLevel);
        }
        return true;
    }

    /**
     * A hit on its weak spot (a Nightmare calls this from its own hit check): it staggers, unless the cooldown of the
     * last stagger runs. True if it staggers now.
     */
    public boolean hitWeakSpot() {
        return stagger();
    }

    private void endStagger() {
        stagger.end();
        entityData.set(DATA_STAGGERED, false);
        updateControlFlags();
        onStaggerEnd();
    }

    /** While staggering, goals that move, look or jump stay off; vanilla resets the flags every 5 ticks. */
    @Override
    protected void updateControlFlags() {
        super.updateControlFlags();
        if (isStaggered()) {
            goalSelector.disableControlFlag(Goal.Flag.MOVE);
            goalSelector.disableControlFlag(Goal.Flag.LOOK);
            goalSelector.disableControlFlag(Goal.Flag.JUMP);
        }
    }

    /**
     * The gate before every attack: not staggered, not in a rest window, not searching. An attack that passes it and
     * starts calls {@link #countAttack()}.
     */
    public boolean mayStartAttack() {
        return !isDeadOrDying() && !isStaggered() && !isResting() && !isSearching();
    }

    /** An attack started; the last one of a burst starts a rest window ({@link #onRestStart}). */
    public void countAttack() {
        if (level().isClientSide()) {
            return;
        }
        NightmareConfig config = config();
        int rest = NightmareConfig.ticksBetween(config.restMinSeconds, config.restMaxSeconds, random.nextInt(1 << 16));
        if (pacing.count(random, config.burstMinAttacks.get(), config.burstMaxAttacks.get(), rest,
                config.restBackOffChance.get())) {
            onRestStart(pacing.style());
        }
    }

    /** In a rest window after a burst: no new attack; it backs off or circles toward {@link #restPoint()}. */
    public boolean isResting() {
        return pacing.isResting();
    }

    /** How this rest window moves. */
    public NightmarePacing.Style restStyle() {
        return pacing.style();
    }

    /**
     * Where a rest window heads now, at its own height (a flyer adds its altitude): away from the target to at least the
     * rest distance, or on around it at the distance it has (at least 4 blocks, at most the rest distance). Null without a
     * target. Ask again every so often; a circling point moves on.
     */
    @Nullable
    public Vec3 restPoint() {
        LivingEntity target = getTarget();
        if (target == null) {
            return null;
        }
        double ox = getX() - target.getX();
        double oz = getZ() - target.getZ();
        double distance = Math.sqrt(ox * ox + oz * oz);
        if (distance < 0.01) {
            ox = 1.0;
            oz = 0.0;
            distance = 1.0;
        }
        double restDistance = config().restDistance.get();
        if (pacing.style() == NightmarePacing.Style.BACK_OFF) {
            // Already farther away: it stays at that distance, it does not come closer.
            double factor = Math.max(restDistance, distance) / distance;
            return new Vec3(target.getX() + ox * factor, getY(), target.getZ() + oz * factor);
        }
        double radius = Math.max(MIN_CIRCLE_RADIUS, Math.min(restDistance, distance));
        double angle = Math.atan2(oz, ox) + (pacing.clockwise() ? -CIRCLE_STEP : CIRCLE_STEP);
        return new Vec3(target.getX() + Math.cos(angle) * radius, getY(), target.getZ() + Math.sin(angle) * radius);
    }

    /** Searching for its target out of sight (see the class comment). */
    public boolean isSearching() {
        return search.isSearching();
    }

    /** Where it last saw (or heard) its target; null without a target. */
    @Nullable
    public Vec3 lastKnownPosition() {
        return search.lastKnown();
    }

    /** Whether a search has reached the last known place (and sniffs and looks around there). */
    public boolean hasArrivedAtSearch() {
        return search.hasArrived();
    }

    /** A search reached the last known place: {@link #onSniff()}, once per place. */
    public void arriveAtSearch() {
        if (search.isSearching() && !search.hasArrived()) {
            search.arrive();
            onSniff();
        }
    }

    /** Who hit it how hard lately; {@link NightmareTargetGoal} chooses with it. */
    public NightmareAggro aggro() {
        return aggro;
    }

    /** Once a tick on the server: the stagger and rest timers, and every {@link #SIGHT_TICKS} the search. */
    private void tickReactions() {
        if (stagger.tick()) {
            endStagger();
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            pacing.reset();
            if (search.stop()) {
                onSearchEnd();
            }
            search.reset();
            return;
        }
        pacing.tick();
        if (tickCount % SIGHT_TICKS != 0) {
            return;
        }
        if (hasLineOfSight(target)) {
            if (search.seen(target.position())) {
                onSearchEnd();
            }
            return;
        }
        boolean wasSearching = search.isSearching();
        NightmareConfig config = config();
        if (search.unseen(SIGHT_TICKS, target.position(), config.searchAfterSeconds.get() * 20,
                config.searchSeconds.get() * 20)) {
            onSearchStart();
        } else if (wasSearching && !search.isSearching()) {
            onSearchEnd();
        }
    }

    /**
     * Moves toward {@code point} for the walker reaction goals (search, rest window). Default: walking with the
     * navigation at {@code speed} times its speed. Flyers, swimmers and rift steppers override it.
     */
    protected void moveForReaction(Vec3 point, double speed) {
        getNavigation().moveTo(point.x, point.y, point.z, speed);
    }

    /** Whether it can stagger now (e.g. not while arriving); true by default. */
    protected boolean canStagger() {
        return true;
    }

    /**
     * It just started to stagger: break off what the goals do not stop (a held player, a flight, own attack state).
     * The running goals are already stopped. Nothing by default.
     */
    protected void onStaggered() {
    }

    /** The stagger is over. Nothing by default. */
    protected void onStaggerEnd() {
    }

    /** Sound and particles of a stagger; by default a dull stun sound and a burst of crit sparks. */
    protected void staggerEffects(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.CRIT, getX(), getY() + getBbHeight() * 0.8, getZ(), 24,
                getBbWidth() * 0.5, getBbHeight() * 0.25, getBbWidth() * 0.5, 0.3);
    }

    /** A burst is over and a rest window of {@code style} starts (e.g. a rift step away). Nothing by default. */
    protected void onRestStart(NightmarePacing.Style style) {
    }

    /** The target went out of sight long enough: a search starts. Nothing by default. */
    protected void onSearchStart() {
    }

    /** A search reached the last known place: sniff (animation, sound). Nothing by default. */
    protected void onSniff() {
    }

    /** A search ended: the target is back in sight, sniffed out, or gone. Nothing by default. */
    protected void onSearchEnd() {
    }

    /** Its strength (1 for a summoned one); attacks with their own damage numbers multiply by it. */
    public float strength() {
        return strength;
    }

    /** Whether a director sent it (and not {@code /summon}). */
    public boolean isDirected() {
        return director != null;
    }

    /**
     * Called by the director right after the arrival spawned it: the strength onto its attributes at once, at full
     * health, and from now on it checks in and reports its retreat and defeat.
     */
    public void direct(NightmareDirector director, float strength) {
        this.director = director;
        this.strength = Math.max(0.01F, strength);
        checkedIn = true;
        applyConfig();
        setHealth(getMaxHealth());
    }

    @Override
    public void tick() {
        // A directed one checks in at its first tick (also after loading from a chunk) and then every second.
        if (!level().isClientSide() && director != null && !isDeadOrDying()
                && (!checkedIn || tickCount % CHECK_TICKS == 0)) {
            checkedIn = true;
            if (!director.isCurrent(this)) {
                // A stale copy (its chunk unloaded and the director moved on): it goes without a trace or report.
                discard();
                return;
            }
        }
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (isDeadOrDying()) {
            return;
        }
        tickReactions();
        if (tickCount % CHECK_TICKS == 0) {
            applyConfig();
            checkPresence();
            aggro.decay(NightmareAggro.decayFactor(CHECK_TICKS, config().aggroHalfLifeSeconds.get() * 20));
        }
    }

    /** The config's numbers onto the attributes, with the health scaled to the fighters (the ratio is kept). */
    private void applyConfig() {
        NightmareConfig config = config();
        float oldMax = getMaxHealth();
        float ratio = oldMax > 0.0F ? getHealth() / oldMax : 1.0F;
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        maxHealth.setBaseValue(config.baseHealth.get());
        double extra = NightmareScaling.extraHealthFactor(config.healthPerExtraPlayer.get(), fighters().size());
        AttributeModifier current = maxHealth.getModifier(PLAYERS_MODIFIER);
        if (current == null || current.amount() != extra) {
            // Permanent, so it is saved and a reload does not clip the health to the base.
            maxHealth.addOrReplacePermanentModifier(
                    new AttributeModifier(PLAYERS_MODIFIER, extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        applyStrength(maxHealth);
        float newMax = getMaxHealth();
        if (newMax != oldMax) {
            setHealth(ratio * newMax);
        }
        setBase(Attributes.ATTACK_DAMAGE, config.attackDamage.get());
        applyStrength(getAttribute(Attributes.ATTACK_DAMAGE));
        setBase(Attributes.ATTACK_KNOCKBACK, config.attackKnockback.get());
        setBase(Attributes.MOVEMENT_SPEED, config.movementSpeed.get());
        setBase(Attributes.ARMOR, config.armor.get());
    }

    /** The strength as a factor on the total (on top of the player scaling); permanent like the player modifier. */
    private void applyStrength(@Nullable AttributeInstance instance) {
        if (instance == null) {
            return;
        }
        double amount = strength - 1.0;
        AttributeModifier current = instance.getModifier(STRENGTH_MODIFIER);
        if (amount == 0.0) {
            if (current != null) {
                instance.removeModifier(STRENGTH_MODIFIER);
            }
        } else if (current == null || current.amount() != amount) {
            instance.addOrReplacePermanentModifier(
                    new AttributeModifier(STRENGTH_MODIFIER, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    /** Nobody (spectators aside) within the retreat radius for too long: it retreats. */
    private void checkPresence() {
        double radius = config().retreatRadius.get();
        boolean someone = false;
        for (Player player : level().players()) {
            if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                someone = true;
                break;
            }
        }
        absentTicks = someone ? 0 : absentTicks + CHECK_TICKS;
        if (absentTicks >= config().retreatSeconds.get() * 20) {
            retreat();
        }
    }

    /** Leaves without a drop ({@link #onRetreat} for its effects); a directed one goes back to virtual, healed. */
    public void retreat() {
        aggro.clear();
        if (level() instanceof ServerLevel serverLevel) {
            onRetreat(serverLevel);
            if (director != null) {
                director.retreated(this);
            }
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = dead;
        super.die(source);
        if (dead) {
            aggro.clear();
        }
        // Once, and only if the death went through (an event can cancel it).
        if (!wasDead && dead && director != null && !level().isClientSide()) {
            director.defeated(this);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat(STRENGTH_TAG, strength);
        tag.putBoolean(DIRECTED_TAG, director != null);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains(STRENGTH_TAG)) {
            strength = Math.max(0.01F, tag.getFloat(STRENGTH_TAG));
        }
        // Only the server's director saves and loads; it decides at the first tick whether this copy is still current.
        director = tag.getBoolean(DIRECTED_TAG) ? NightmareDirector.server() : null;
        checkedIn = false;
    }

    /** Sounds and particles of the retreat; nothing by default. */
    protected void onRetreat(ServerLevel level) {
    }

    /** Ticks from 0 HP until the body is removed: the length of its death animation (vanilla: 20). */
    protected int deathTicks() {
        return 60;
    }

    /** Blocks the model reaches beyond the hitbox, sideways and up (the culling box); dead, sideways {@link #deadOverhang}. */
    protected double modelOverhang() {
        return 2.0;
    }

    protected double modelOverhangUp() {
        return 2.0;
    }

    /** Sideways reach of the body lying dead (it may fall over). */
    protected double deadOverhang() {
        return 7.0;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double side = deathTime > 0 ? deadOverhang() : modelOverhang();
        return getBoundingBox().inflate(side, modelOverhangUp(), side);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= deathTicks() && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    protected int getBaseExperienceReward() {
        return config().experience.get();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
