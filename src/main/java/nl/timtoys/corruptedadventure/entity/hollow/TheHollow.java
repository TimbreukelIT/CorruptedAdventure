package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.registry.ModItems;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * De Holle, the ring 1 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A soulless stone
 * titan: slams with its giant arm, rift-steps behind its target, goes out of phase now and then (transparent and
 * invulnerable), swaps two players. A hit with a soul crystal pins it in phase. Its max health grows with the players in
 * the fight; with no player near for a while it retreats without a drop. Everything runs on the server; the client only
 * renders the synced out-of-phase state and the triggered animations. The rift entity, roaming and the return come in
 * steps 2 and 3.
 */
public class TheHollow extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Boolean> DATA_PHASED =
            SynchedEntityData.defineId(TheHollow.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation PLAYERS_MODIFIER = CorruptedAdventure.id("hollow_players");
    private static final int CHECK_TICKS = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    private int pinnedTicks;
    private int phaseTicks;
    private int nextPhaseTick = -1;
    private int absentTicks;

    public TheHollow(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        // The defaults of [nightmares.hollow]; the server config overrides them from the first tick.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HollowSwapGoal(this));
        goalSelector.addGoal(2, new HollowRiftStepGoal(this));
        goalSelector.addGoal(3, new HollowSlamGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // It is a dimension shifter: it finds you through walls.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** A player who counts in the fight: health scaling, swaps. Creative and spectator players are onlookers. */
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

    public boolean isPhased() {
        return entityData.get(DATA_PHASED);
    }

    public boolean isPinned() {
        return pinnedTicks > 0;
    }

    /** Out of phase for {@code ticks}: transparent and invulnerable. Refused while pinned. */
    public boolean startPhase(int ticks) {
        if (isPinned() || isPhased()) {
            return false;
        }
        phaseTicks = ticks;
        entityData.set(DATA_PHASED, true);
        if (level() instanceof ServerLevel serverLevel) {
            HollowEffects.phaseOut(serverLevel, this);
        }
        return true;
    }

    private void endPhase() {
        phaseTicks = 0;
        if (isPhased()) {
            entityData.set(DATA_PHASED, false);
            if (level() instanceof ServerLevel serverLevel) {
                HollowEffects.phaseIn(serverLevel, this);
            }
        }
    }

    /** Pinned in phase for {@code ticks}: never transparent, no rift step. */
    private void pin(int ticks) {
        pinnedTicks = ticks;
        endPhase();
        if (level() instanceof ServerLevel serverLevel) {
            HollowEffects.pinned(serverLevel, this);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide() && source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player player
                && source.getDirectEntity() == player) {
            ItemStack hand = player.getMainHandItem();
            // A crystal on an already pinned Hollow does nothing and costs nothing (no wasted crystals on spam clicks).
            if (hand.is(ModItems.SOUL_CRYSTAL.get()) && !isPinned()) {
                pin(ServerConfig.HOLLOW.pinSeconds.get() * 20);
                if (!player.getAbilities().instabuild) {
                    hand.shrink(1);
                }
            }
        }
        if (isPhased() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isDeadOrDying()) {
            return;
        }
        if (pinnedTicks > 0) {
            pinnedTicks--;
        }
        if (tickCount % CHECK_TICKS == 0) {
            applyConfig();
            checkPresence();
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isPhased()) {
            if (--phaseTicks <= 0) {
                endPhase();
                nextPhaseTick = tickCount + ServerConfig.HOLLOW.phaseIntervalTicks(random.nextInt(1 << 16));
            }
        } else if (getTarget() == null) {
            nextPhaseTick = -1;
        } else if (nextPhaseTick < 0) {
            // The first spell comes an interval after the fight starts, not at once.
            nextPhaseTick = tickCount + ServerConfig.HOLLOW.phaseIntervalTicks(random.nextInt(1 << 16));
        } else if (tickCount >= nextPhaseTick && !isPinned()) {
            startPhase(ServerConfig.HOLLOW.phaseDurationTicks(random.nextInt(1 << 16)));
        }
    }

    /** The config's numbers onto the attributes, with the health scaled to the fighters (the ratio is kept). */
    private void applyConfig() {
        HollowConfig config = ServerConfig.HOLLOW;
        float oldMax = getMaxHealth();
        float ratio = oldMax > 0.0F ? getHealth() / oldMax : 1.0F;
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        maxHealth.setBaseValue(config.baseHealth.get());
        int fighters = fighters(config.fightRadius.get()).size();
        double extra = HollowScaling.extraHealthFactor(config.healthPerExtraPlayer.get(), fighters);
        AttributeModifier current = maxHealth.getModifier(PLAYERS_MODIFIER);
        if (current == null || current.amount() != extra) {
            maxHealth.addOrReplacePermanentModifier(
                    new AttributeModifier(PLAYERS_MODIFIER, extra, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        float newMax = getMaxHealth();
        if (newMax != oldMax) {
            setHealth(ratio * newMax);
        }
        setBase(Attributes.ATTACK_DAMAGE, config.slamDamage.get());
        setBase(Attributes.ATTACK_KNOCKBACK, config.slamKnockback.get());
        setBase(Attributes.MOVEMENT_SPEED, config.movementSpeed.get());
        setBase(Attributes.ARMOR, config.armor.get());
    }

    private void setBase(Holder<Attribute> attribute, double value) {
        getAttribute(attribute).setBaseValue(value);
    }

    /** Nobody (spectators aside) within the retreat radius for too long: it retreats, without a drop. */
    private void checkPresence() {
        double radius = ServerConfig.HOLLOW.retreatRadius.get();
        boolean someone = false;
        for (Player player : level().players()) {
            if (player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) <= radius * radius) {
                someone = true;
                break;
            }
        }
        absentTicks = someone ? 0 : absentTicks + CHECK_TICKS;
        if (absentTicks >= ServerConfig.HOLLOW.retreatSeconds.get() * 20) {
            retreat();
        }
    }

    /** Leaves without a drop. Step 3 keeps its health in SavedData and lets it come back. */
    public void retreat() {
        if (level() instanceof ServerLevel serverLevel) {
            HollowEffects.retreat(serverLevel, this);
        }
        discard();
    }

    @Override
    protected AABB getAttackBoundingBox() {
        double reach = ServerConfig.HOLLOW.slamReach.get();
        return getBoundingBox().inflate(reach, 0.0, reach);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // The model is bigger than the hitbox: the giant arm hangs out in front, upright it is ~8 blocks tall.
        return getBoundingBox().inflate(2.5, 3.0, 2.5);
    }

    @Override
    protected void tickDeath() {
        // Longer than vanilla's 20 ticks: it falls over backwards.
        deathTime++;
        if (deathTime >= HollowAnimations.DEATH_TICKS && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    protected int getBaseExperienceReward() {
        return ServerConfig.HOLLOW.experience.get();
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
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, HollowAnimations.BODY, 5, this::bodyAnimation));
        controllers.add(new AnimationController<>(this, HollowAnimations.ACTION, 2, state -> PlayState.STOP)
                .triggerableAnim(HollowAnimations.SLAM, HollowAnimations.SLAM_ANIM)
                .triggerableAnim(HollowAnimations.RIFT_STEP, HollowAnimations.RIFT_STEP_ANIM)
                .triggerableAnim(HollowAnimations.SWAP, HollowAnimations.SWAP_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheHollow> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(HollowAnimations.DEATH);
        }
        if (isPhased()) {
            return state.setAndContinue(HollowAnimations.PHASE);
        }
        return state.setAndContinue(state.isMoving() ? HollowAnimations.WALK : HollowAnimations.IDLE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
