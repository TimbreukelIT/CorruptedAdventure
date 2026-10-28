package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import nl.timtoys.corruptedadventure.entity.nightmare.Nightmare;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/**
 * De Onvoltooide, the ring 5 Nightmare (phase 4 step 1: body and fight, summoned with {@code /summon}). A ~20-block
 * colossus grafted together from the four defeated Nightmares, half loaded. His mosaic body only takes
 * {@code bodyDamageFactor} of a blow; his four weak spots, one per graft ({@link Graft}), take nothing until someone looks
 * at him through a sky glass lens ({@link LensSight}), then they are loaded for everyone and take
 * {@code weakDamageFactor}; a weak spot that took enough tears its graft off, and with it that graft's memory attack.
 * The parts are {@link UnfinishedPart}s (multipart, like the ender dragon). He does not path-find: he strides straight
 * over the land and climbs onto whatever is in the way, and steps through the void when his target is far.
 * <p>
 * Server-side: the parts' boxes (both sides, from the synced position and yaw), the lens state, the lost grafts and the
 * stride are synced; the client renders and swaps the loaded and unloaded parts. Roaming, his coming with the standstill
 * weather and the scar he tears in the ground come in steps 2 and 3.
 */
public class TheUnfinished extends Nightmare {
    private static final EntityDataAccessor<Boolean> DATA_LOADED =
            SynchedEntityData.defineId(TheUnfinished.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DATA_LOST =
            SynchedEntityData.defineId(TheUnfinished.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_STRIDING =
            SynchedEntityData.defineId(TheUnfinished.class, EntityDataSerializers.BOOLEAN);

    /** Where his parts sit on the rest pose (blocks; tools/textures/unfinished_model.py, 16 units per block). */
    static final List<UnfinishedPart.Spec> SPECS = List.of(
            new UnfinishedPart.Spec("chest", 0.0, 6.0, 3.8, 5.0F, 8.5F, null, false),
            new UnfinishedPart.Spec("crown", 0.0, 9.5, 6.2, 8.0F, 10.0F, null, false),
            new UnfinishedPart.Spec("shoulder_r", 5.75, 11.0, 0.5, 5.5F, 3.5F, null, false),
            new UnfinishedPart.Spec("shoulder_l", -5.6, 11.0, 0.4, 5.0F, 3.5F, null, false),
            new UnfinishedPart.Spec("arm", 6.3, 0.0, 2.3, 3.5F, 11.0F, Graft.HOLLE, false),
            new UnfinishedPart.Spec("legs_near", -6.0, 4.5, 0.5, 4.0F, 10.0F, Graft.GELAAGDE, false),
            new UnfinishedPart.Spec("legs_far", -10.0, 0.0, 0.5, 5.0F, 14.0F, Graft.GELAAGDE, false),
            new UnfinishedPart.Spec("wing_inner", 10.8, 14.5, 0.4, 4.0F, 4.5F, Graft.HERRIJZENDE, false),
            new UnfinishedPart.Spec("wing_outer", 14.5, 12.5, 0.4, 4.5F, 6.0F, Graft.HERRIJZENDE, false),
            new UnfinishedPart.Spec("weak_holle", 7.06, 3.0, 3.5, 2.4F, 2.4F, Graft.HOLLE, true),
            new UnfinishedPart.Spec("weak_gelaagde", -10.0, 2.2, 3.3, 2.0F, 2.4F, Graft.GELAAGDE, true),
            new UnfinishedPart.Spec("weak_herrijzende", 7.56, 15.6, -0.38, 2.2F, 2.2F, Graft.HERRIJZENDE, true),
            new UnfinishedPart.Spec("weak_hoeder", 2.11, 4.0, -4.0, 2.4F, 2.4F, Graft.HOEDER, true));
    /** A stride's speed: the movement speed attribute times this, in blocks per tick. */
    private static final double STRIDE_FACTOR = 0.4;
    /** Degrees per tick he turns; he only walks when he faces within {@link #WALK_CONE} of where he goes. */
    private static final float TURN = 5.0F;
    private static final float WALK_CONE = 45.0F;
    private static final int LENS_CHECK_TICKS = 5;
    /** Height of his body's sight point for the lens (the middle of his torso). */
    private static final double BODY_SIGHT = 7.0;

    private final UnfinishedPart[] parts;
    private final float[] graftDamage = new float[Graft.values().length];
    private final UnfinishedAsh ash = new UnfinishedAsh();
    private float desiredYaw;
    private int loadedUntil = -1;
    private int nextAttackTick;
    private boolean attacking;
    private int sunkenFrom = -1;
    private int sunkenUntil = -1;
    private float damageTaken;

    public TheUnfinished(EntityType<? extends TheUnfinished> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        parts = new UnfinishedPart[SPECS.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new UnfinishedPart(this, SPECS.get(i));
        }
        // The part ids follow the parent's (NeoForge, like the ender dragon: MC-158205).
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
        noPhysics = true;
        setNoGravity(true);
        desiredYaw = getYRot();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return nightmareAttributes(UnfinishedConfig.DEFAULTS, 64.0);
    }

    @Override
    protected NightmareConfig config() {
        return ServerConfig.UNFINISHED.common;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LOADED, false);
        builder.define(DATA_LOST, (byte) 0);
        builder.define(DATA_STRIDING, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new UnfinishedVoidPullGoal(this));
        goalSelector.addGoal(2, new UnfinishedRiftStepGoal(this));
        goalSelector.addGoal(3, new UnfinishedGrabGoal(this));
        goalSelector.addGoal(4, new UnfinishedAshRainGoal(this));
        goalSelector.addGoal(5, new UnfinishedLureGoal(this));
        goalSelector.addGoal(6, new UnfinishedSlamGoal(this));
        goalSelector.addGoal(7, new UnfinishedStrideGoal(this));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // He climbs out of the void toward whoever is in his ring; walls do not hide you.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ------------------------------------------------------------------------------------------------ parts

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

    /** The world position of a point given in blocks from his feet: {@code left} (negative = right), up, forward. */
    public Vec3 localToWorld(double left, double up, double forward) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double cos = Mth.cos(yaw);
        double sin = Mth.sin(yaw);
        return new Vec3(getX() + left * cos - forward * sin, getY() + up, getZ() + left * sin + forward * cos);
    }

    /** Every part onto the rest pose, on both sides (picking on the client, the reach check on the server). */
    private void placeParts() {
        for (UnfinishedPart part : parts) {
            UnfinishedPart.Spec spec = part.spec();
            Vec3 old = part.position();
            Vec3 at = localToWorld(spec.left(), spec.up(), spec.forward());
            part.setPos(at.x, at.y, at.z);
            part.xo = old.x;
            part.yo = old.y;
            part.zo = old.z;
            part.xOld = old.x;
            part.yOld = old.y;
            part.zOld = old.z;
        }
    }

    /** A part can be hit while he lives, is above ground and still has its graft. */
    boolean isPartActive(UnfinishedPart part) {
        Graft graft = part.spec().graft();
        return isAlive() && !isSunken() && (graft == null || hasGraft(graft));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        placeParts();
    }

    // ------------------------------------------------------------------------------------------------ damage

    /** A hit on his main hitbox: his mosaic body. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return applyDamage(source, amount);
        }
        return applyDamage(source, amount * ServerConfig.UNFINISHED.bodyDamageFactor.get().floatValue());
    }

    /** A hit on one of his parts: a body part counts as his body; a weak spot only while loaded. */
    boolean hurtPart(UnfinishedPart part, DamageSource source, float amount) {
        if (!isPartActive(part)) {
            return false;
        }
        UnfinishedPart.Spec spec = part.spec();
        if (!spec.weak()) {
            return hurt(source, amount);
        }
        if (!isLoaded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel serverLevel) {
                UnfinishedEffects.deflected(serverLevel, part.getBoundingBox().getCenter());
            }
            return false;
        }
        float before = getHealth();
        boolean hit = applyDamage(source, amount * ServerConfig.UNFINISHED.weakDamageFactor.get().floatValue());
        if (hit && level() instanceof ServerLevel serverLevel && spec.graft() != null) {
            UnfinishedEffects.weakHit(serverLevel, part.getBoundingBox().getCenter());
            Graft graft = spec.graft();
            graftDamage[graft.ordinal()] += before - getHealth();
            double share = ServerConfig.UNFINISHED.graftBreakShare.get();
            if (share > 0.0 && graftDamage[graft.ordinal()] >= share * getMaxHealth() && isAlive()) {
                loseGraft(graft, part.getBoundingBox().getCenter());
            }
        }
        return hit;
    }

    private boolean applyDamage(DamageSource source, float amount) {
        if (isSunken() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        float before = getHealth();
        boolean hit = super.hurt(source, amount);
        damageTaken += Math.max(0.0F, before - getHealth());
        return hit;
    }

    /** Health he has lost in total since he came (for the grab, which a hard enough beating breaks). */
    float damageTaken() {
        return damageTaken;
    }

    private void loseGraft(Graft graft, Vec3 at) {
        entityData.set(DATA_LOST, (byte) (entityData.get(DATA_LOST) | graft.bit()));
        graftDamage[graft.ordinal()] = 0.0F;
        triggerAnim(UnfinishedAnimations.ACTION, UnfinishedAnimations.FLINCH);
        UnfinishedEffects.graftLost((ServerLevel) level(), at);
    }

    public boolean hasGraft(Graft graft) {
        return (entityData.get(DATA_LOST) & graft.bit()) == 0;
    }

    /** The bits of the grafts that fell away ({@link Graft#bit}); the renderer hides them. */
    public int lostGrafts() {
        return entityData.get(DATA_LOST);
    }

    /** Whether his weak spots are loaded (someone looked at him through a sky glass lens lately). */
    public boolean isLoaded() {
        return entityData.get(DATA_LOADED);
    }

    /** Loads his weak spots for {@code ticks}, as a look through the lens does (also for tests and commands). */
    public void load(int ticks) {
        loadedUntil = Math.max(loadedUntil, tickCount + ticks);
        updateLoaded();
    }

    private void updateLoaded() {
        boolean loaded = tickCount < loadedUntil;
        if (loaded != isLoaded()) {
            entityData.set(DATA_LOADED, loaded);
            if (loaded && level() instanceof ServerLevel serverLevel) {
                UnfinishedEffects.loaded(serverLevel, this);
            }
        }
    }

    private void checkLens() {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        List<Vec3> points = new ArrayList<>();
        Vec3 body = position().add(0.0, BODY_SIGHT, 0.0);
        points.add(body);
        for (UnfinishedPart part : parts) {
            if (part.spec().weak() && isPartActive(part)) {
                points.add(part.getBoundingBox().getCenter());
            }
        }
        if (LensSight.anyLooking(level().players(), body, points, config.lensRange.get(), config.lensConeDegrees.get())) {
            // Held at least until the next check, however short the linger is set.
            load(Math.max(config.lensLingerTicks.get(), LENS_CHECK_TICKS));
        }
        updateLoaded();
    }

    // ------------------------------------------------------------------------------------------------ movement

    @Override
    public void tick() {
        // No collision with the world: he stands on what UnfinishedGround says and climbs over the rest.
        noPhysics = true;
        super.tick();
        // His body is one piece: body and head follow the synced yaw (the parts use it too).
        yBodyRot = getYRot();
        yBodyRotO = yRotO;
        yHeadRot = getYRot();
        yHeadRotO = yRotO;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setDeltaMovement(Vec3.ZERO);
        if (!attacking && !isStriding() && getTarget() != null) {
            desiredYaw = yawTo(position(), getTarget().position());
        }
        setYRot(Mth.approachDegrees(getYRot(), desiredYaw, TURN));
        settle();
        if (tickCount % LENS_CHECK_TICKS == 0) {
            checkLens();
        }
        ash.tick((ServerLevel) level(), this);
    }

    /** Onto the ground under him, climbing at most {@code climbPerTick} a tick; over void he hangs where he is. */
    private void settle() {
        if (isSunken()) {
            return;
        }
        OptionalDouble ground = UnfinishedGround.surface(level(), getX(), getZ());
        if (ground.isEmpty()) {
            return;
        }
        double climb = ServerConfig.UNFINISHED.climbPerTick.get();
        double y = getY() + Mth.clamp(ground.getAsDouble() - getY(), -climb * 2.0, climb);
        if (y != getY()) {
            setPos(getX(), y, getZ());
        }
    }

    /**
     * One tick of striding toward {@code goal}: turn toward it, and once he faces it, step {@code movementSpeed x 0.4}
     * blocks. False when he cannot: the ground ahead is void or not loaded.
     */
    boolean strideToward(Vec3 goal) {
        double dx = goal.x - getX();
        double dz = goal.z - getZ();
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3) {
            return false;
        }
        desiredYaw = yawTo(position(), goal);
        if (Math.abs(Mth.wrapDegrees(desiredYaw - getYRot())) > WALK_CONE) {
            return true;
        }
        double step = Math.min(getAttributeValue(Attributes.MOVEMENT_SPEED) * STRIDE_FACTOR, length);
        double x = getX() + dx / length * step;
        double z = getZ() + dz / length * step;
        if (UnfinishedGround.surface(level(), x, z).isEmpty()) {
            return false;
        }
        setPos(x, getY(), z);
        return true;
    }

    /** Turns (at his turning speed) so that the point {@code left, forward} of him comes to lie toward {@code target}. */
    void faceWithLocalPoint(Vec3 target, double left, double forward) {
        float local = (float) (Mth.atan2(forward, left) * Mth.RAD_TO_DEG) - 90.0F;
        desiredYaw = Mth.wrapDegrees(yawTo(position(), target) - local);
    }

    void snapYaw(float yaw) {
        desiredYaw = yaw;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** Vanilla's yaw (degrees) of looking from {@code from} to {@code to}. */
    static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90.0F;
    }

    boolean isStriding() {
        return entityData.get(DATA_STRIDING);
    }

    void setStriding(boolean striding) {
        if (striding != isStriding()) {
            entityData.set(DATA_STRIDING, striding);
        }
    }

    /** Sunken in his void during a rift step: nothing hits him, his parts are gone, he does not settle. */
    void setSunkenWindow(int from, int until) {
        sunkenFrom = from;
        sunkenUntil = until;
    }

    boolean isSunken() {
        return tickCount >= sunkenFrom && tickCount < sunkenUntil;
    }

    boolean canStartAttack() {
        return !attacking && tickCount >= nextAttackTick;
    }

    /** An attack runs: it turns him itself, and no other attack starts. */
    void attackStarted() {
        attacking = true;
    }

    void attackDone() {
        attacking = false;
        nextAttackTick = tickCount + ServerConfig.UNFINISHED.attackGapTicks.get();
    }

    UnfinishedAsh ash() {
        return ash;
    }

    // ------------------------------------------------------------------------------------------------ Nightmare

    @Override
    protected void onRetreat(ServerLevel level) {
        UnfinishedEffects.retreat(level, this);
    }

    @Override
    protected int deathTicks() {
        return UnfinishedAnimations.DEATH_TICKS;
    }

    @Override
    protected double modelOverhang() {
        // The wing reaches ~16 blocks to one side, the Layered's legs ~13 to the other.
        return 14.0;
    }

    @Override
    protected double modelOverhangUp() {
        return 7.0;
    }

    @Override
    protected double deadOverhang() {
        return 14.0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("LostGrafts", entityData.get(DATA_LOST));
        CompoundTag damage = new CompoundTag();
        for (Graft graft : Graft.values()) {
            damage.putFloat(graft.name().toLowerCase(Locale.ROOT), graftDamage[graft.ordinal()]);
        }
        tag.put("GraftDamage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_LOST, tag.getByte("LostGrafts"));
        CompoundTag damage = tag.getCompound("GraftDamage");
        for (Graft graft : Graft.values()) {
            graftDamage[graft.ordinal()] = damage.getFloat(graft.name().toLowerCase(Locale.ROOT));
        }
        desiredYaw = getYRot();
    }

    // ------------------------------------------------------------------------------------------------ GeckoLib

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, UnfinishedAnimations.BODY, 8, this::bodyAnimation));
        controllers.add(new AnimationController<>(this, UnfinishedAnimations.ACTION, 3, state -> PlayState.STOP)
                .triggerableAnim(UnfinishedAnimations.SLAM, UnfinishedAnimations.SLAM_ANIM)
                .triggerableAnim(UnfinishedAnimations.RIFT_SINK, UnfinishedAnimations.RIFT_SINK_ANIM)
                .triggerableAnim(UnfinishedAnimations.RIFT_RISE, UnfinishedAnimations.RIFT_RISE_ANIM)
                .triggerableAnim(UnfinishedAnimations.GRAB, UnfinishedAnimations.GRAB_ANIM)
                .triggerableAnim(UnfinishedAnimations.ASH_RAIN, UnfinishedAnimations.ASH_RAIN_ANIM)
                .triggerableAnim(UnfinishedAnimations.LURE, UnfinishedAnimations.LURE_ANIM)
                .triggerableAnim(UnfinishedAnimations.VOID_PULL, UnfinishedAnimations.VOID_PULL_ANIM)
                .triggerableAnim(UnfinishedAnimations.FLINCH, UnfinishedAnimations.FLINCH_ANIM));
    }

    private PlayState bodyAnimation(AnimationState<TheUnfinished> state) {
        if (isDeadOrDying()) {
            return state.setAndContinue(UnfinishedAnimations.DEATH);
        }
        return state.setAndContinue(isStriding() ? UnfinishedAnimations.WALK : UnfinishedAnimations.IDLE);
    }
}
