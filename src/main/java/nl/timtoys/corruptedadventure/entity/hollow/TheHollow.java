package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.config.HollowConfig;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmarePaceGoal;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmarePacing;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareSearchGoal;
import nl.timtoys.corruptedadventure.entity.nightmare.NightmareTargetGoal;
import nl.timtoys.corruptedadventure.registry.ModItems;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

/**
 * De Holle, the ring 1 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A soulless stone
 * titan: slams with its giant arm, rift-steps behind its target, goes out of phase now and then (transparent and
 * invulnerable), swaps two players. Between those it uses its extra attacks ({@link HollowExtraAttackGoal}: ground
 * slam, rift rain, sweep). The shared reactions of {@link Nightmare} (life round): every attack waits for the gate and
 * counts toward the pace; a stagger breaks off the running attack and ends a phase, and a soul crystal hit is its weak
 * spot. In a rest window or a search it walks, or now and then rift-steps there ({@link HollowRiftStepGoal#stepTo}). A hit with a soul crystal pins it in phase. Boss bar, health scaling, retreat and
 * the rest of the shared rules come from {@link Nightmare}. The client renders the synced out-of-phase state and the
 * triggered animations. The rift entity, roaming and the return come in steps 2 and 3.
 */
public class TheHollow extends Nightmare {
    private static final EntityDataAccessor<Boolean> DATA_PHASED =
            SynchedEntityData.defineId(TheHollow.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_EMERGING =
            SynchedEntityData.defineId(TheHollow.class, EntityDataSerializers.BOOLEAN);

    private int pinnedTicks;
    private int phaseTicks;
    private int nextPhaseTick = -1;
    private int emergeTicks;
    /** Waiting, unseen and untouchable, inside a rift that is still opening (the director's arrival). */
    private int hiddenTicks;
    private HollowRiftStepGoal riftStep;

    public TheHollow(EntityType<? extends TheHollow> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return nightmareAttributes(HollowConfig.DEFAULTS, 48.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected NightmareConfig config() {
        return ServerConfig.HOLLOW.common;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASED, false);
        builder.define(DATA_EMERGING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(0, new HollowEmergeGoal(this));
        goalSelector.addGoal(1, new HollowSwapGoal(this));
        // Above the rest window and the search: a reaction's rift step takes over from walking there.
        riftStep = new HollowRiftStepGoal(this);
        goalSelector.addGoal(1, riftStep);
        goalSelector.addGoal(2, new NightmareSearchGoal(this));
        goalSelector.addGoal(2, new NightmarePaceGoal(this));
        goalSelector.addGoal(2, new HollowExtraAttackGoal(this));
        goalSelector.addGoal(3, new HollowSlamGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        // It is a dimension shifter: it finds you through walls (the shared target goal does).
        targetSelector.addGoal(1, new NightmareTargetGoal(this));
    }

    public boolean isPhased() {
        return entityData.get(DATA_PHASED);
    }

    public boolean isPinned() {
        return pinnedTicks > 0;
    }

    /** Pulling itself out of its arrival rift: no attacks, no moving; the emerge animation plays. */
    public boolean isEmerging() {
        return entityData.get(DATA_EMERGING);
    }

    /** Called by the arrival rift right after it put The Hollow in the world. */
    public void startEmerging() {
        emergeTicks = HollowAnimations.EMERGE_TICKS;
        entityData.set(DATA_EMERGING, true);
    }

    /**
     * Waits {@code ticks} unseen and untouchable inside a rift that is still tearing open, then emerges. The director
     * needs the entity at once, while the rift shows its eyes first.
     */
    public void awaitRift(int ticks) {
        hiddenTicks = ticks;
        setInvisible(true);
        setInvulnerable(true);
    }

    /** Arriving: waiting in its rift or pulling itself out of it. No attacks, no moving. */
    public boolean isArriving() {
        return hiddenTicks > 0 || isEmerging();
    }

    /** Out of phase for {@code ticks}: transparent and invulnerable. Refused while pinned or staggering. */
    public boolean startPhase(int ticks) {
        if (isPinned() || isPhased() || isStaggered()) {
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
                // Its weak spot: pulled fully here, it reels.
                hitWeakSpot();
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
        if (level().isClientSide()) {
            return;
        }
        // The pin and the emerging run out without AI too.
        if (pinnedTicks > 0) {
            pinnedTicks--;
        }
        if (hiddenTicks > 0 && --hiddenTicks == 0) {
            setInvisible(false);
            setInvulnerable(false);
            startEmerging();
        }
        if (emergeTicks > 0 && --emergeTicks == 0) {
            entityData.set(DATA_EMERGING, false);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (isArriving()) {
            return;
        }
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
        } else if (tickCount >= nextPhaseTick && !isPinned() && !isStaggered()) {
            startPhase(ServerConfig.HOLLOW.phaseDurationTicks(random.nextInt(1 << 16)));
        }
    }

    @Override
    protected boolean canStagger() {
        return !isArriving();
    }

    /** The running goals are already stopped: end a phase (the open window) and cut the attack's animation. */
    @Override
    protected void onStaggered() {
        endPhase();
        stopTriggeredAnim(HollowAnimations.ACTION, null);
    }

    @Override
    protected void staggerEffects(ServerLevel level) {
        super.staggerEffects(level);
        HollowEffects.stagger(level, this);
    }

    /** A rest window that backs off: now and then it rift-steps away instead of walking. */
    @Override
    protected void onRestStart(NightmarePacing.Style style) {
        Vec3 point = restPoint();
        // Not right after a rift step that ended the burst: it would come out behind you and vanish at once.
        if (style == NightmarePacing.Style.BACK_OFF && point != null && !riftStep.isStepping()
                && random.nextDouble() < ServerConfig.HOLLOW.restRiftStepChance.get()) {
            riftStep.stepTo(point);
        }
    }

    /** It lost its target: now and then it rift-steps straight to the last place it knew. */
    @Override
    protected void onSearchStart() {
        Vec3 point = lastKnownPosition();
        if (point != null && random.nextDouble() < ServerConfig.HOLLOW.searchRiftStepChance.get()) {
            riftStep.stepTo(point);
        }
    }

    @Override
    protected void onSniff() {
        if (level() instanceof ServerLevel serverLevel) {
            HollowEffects.sniff(serverLevel, this);
        }
    }

    @Override
    protected void onRetreat(ServerLevel level) {
        HollowEffects.retreat(level, this);
    }

    @Override
    protected AABB getAttackBoundingBox() {
        double reach = ServerConfig.HOLLOW.slamReach.get();
        return getBoundingBox().inflate(reach, 0.0, reach);
    }

    @Override
    protected int deathTicks() {
        return HollowAnimations.DEATH_TICKS;
    }

    @Override
    protected double modelOverhang() {
        // The giant arm hangs out in front.
        return 2.5;
    }

    @Override
    protected double modelOverhangUp() {
        // Upright it is ~8 blocks tall.
        return 3.0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, HollowAnimations.BODY, 5, this::bodyAnimation));
        controllers.add(new AnimationController<>(this, HollowAnimations.ACTION, 2, state -> PlayState.STOP)
                .triggerableAnim(HollowAnimations.SLAM, HollowAnimations.SLAM_ANIM)
                .triggerableAnim(HollowAnimations.RIFT_STEP, HollowAnimations.RIFT_STEP_ANIM)
                .triggerableAnim(HollowAnimations.SWAP, HollowAnimations.SWAP_ANIM)
                .triggerableAnim(HollowAnimations.GROUND_SLAM, HollowAnimations.GROUND_SLAM_ANIM)
                .triggerableAnim(HollowAnimations.RIFT_RAIN, HollowAnimations.RIFT_RAIN_ANIM)
                .triggerableAnim(HollowAnimations.SWEEP, HollowAnimations.SWEEP_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheHollow> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(HollowAnimations.DEATH);
        }
        if (isEmerging()) {
            return state.setAndContinue(HollowAnimations.EMERGE_ANIM);
        }
        if (isStaggered()) {
            return state.setAndContinue(HollowAnimations.STAGGER);
        }
        if (isPhased()) {
            return state.setAndContinue(HollowAnimations.PHASE);
        }
        return state.setAndContinue(state.isMoving() ? HollowAnimations.WALK : HollowAnimations.IDLE);
    }
}
