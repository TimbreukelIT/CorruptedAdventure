package nl.timtoys.corruptedadventure.entity.hollow;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * One void claw of The Hollow's rift rain. First a purple crack opens in the ground for {@code warmup} ticks (the
 * warning: step off it), then a stone hand eaten by corruption claws up out of the void, hits whoever stands in it
 * once, and sinks back. The server decides when it bites; the client draws the crack and the hand
 * ({@code client/entity/hollow/HollowVoidClawRenderer}, GeckoLib: under the ground until the bite event, then "grab"). Leaves nothing in the
 * world and is never saved. Cancelled (The Hollow's attack broken off) before the bite, it closes without biting.
 */
public class HollowVoidClaw extends Entity implements GeoEntity {
    private static final RawAnimation HIDDEN = RawAnimation.begin().thenLoop("animation.hollow_void_claw.hidden");
    private static final RawAnimation GRAB = RawAnimation.begin().thenPlayAndHold("animation.hollow_void_claw.grab");

    private static final EntityDataAccessor<Integer> DATA_WARMUP = SynchedEntityData.defineId(HollowVoidClaw.class, EntityDataSerializers.INT);
    /** Entity event: the claw comes up now (client: the rise starts). */
    private static final byte EVENT_BITE = 4;
    /** Ticks the hand rises, stands and sinks after the bite (the length of its "grab" animation). */
    public static final int LIFE_TICKS = 16;
    /** Half the claw's width and its height, in blocks: whoever's hitbox touches that box is hit. */
    private static final double HALF_WIDTH = 0.6;
    private static final double HEIGHT = 2.2;
    /** How far out the renderer's warning cracks reach (up to 1.15 blocks, plus their width), for the culling box. */
    private static final double CRACK_REACH = 1.3;

    @Nullable
    private TheHollow owner;
    private float damage = 6.0F;
    private boolean bitten;
    /** Client: {@code tickCount} at the bite event, or -1 before it. */
    private int clientBite = -1;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public HollowVoidClaw(EntityType<? extends HollowVoidClaw> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A claw at {@code feet} (its ground) that bites after {@code warmup} ticks of crack, as {@code owner}'s attack. */
    public static HollowVoidClaw spawn(ServerLevel level, @Nullable TheHollow owner, Vec3 feet, float yaw, int warmup,
                                       float damage) {
        HollowVoidClaw claw = new HollowVoidClaw(ModEntityTypes.HOLLOW_VOID_CLAW.get(), level);
        claw.moveTo(feet.x, feet.y, feet.z, yaw, 0.0F);
        claw.owner = owner;
        claw.damage = damage;
        claw.entityData.set(DATA_WARMUP, Math.max(1, warmup));
        level.addFreshEntity(claw);
        return claw;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_WARMUP, 25);
    }

    public int warmup() {
        return entityData.get(DATA_WARMUP);
    }

    public boolean hasBitten() {
        return bitten;
    }

    /** Client: ticks since the claw came up (partial ticks allowed), or a negative number while it is still a crack. */
    public float clientSinceBite(float partialTick) {
        return clientBite < 0 ? -1.0F : tickCount - clientBite + partialTick;
    }

    /** The attack was broken off: a claw that has not come up yet closes without biting. */
    public void cancel() {
        if (!bitten && !isRemoved()) {
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ModParticleTypes.VOID_MOTE.get(), getX(), getY() + 0.2, getZ(), 6, 0.3, 0.05, 0.3, 0.0);
            }
            discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (clientBite < 0 && random.nextInt(3) == 0) {
                level().addParticle(ModParticleTypes.GLITCH_PIXEL.get(), getX() + (random.nextDouble() - 0.5) * 1.2,
                        getY() + 0.05, getZ() + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.03, 0.0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        if (!bitten && tickCount >= warmup()) {
            bite(level);
        } else if (bitten && tickCount >= warmup() + LIFE_TICKS) {
            discard();
        }
    }

    private void bite(ServerLevel level) {
        bitten = true;
        level.broadcastEntityEvent(this, EVENT_BITE);
        HollowEffects.clawBite(level, this);
        AABB box = new AABB(getX() - HALF_WIDTH, getY(), getZ() - HALF_WIDTH, getX() + HALF_WIDTH, getY() + HEIGHT,
                getZ() + HALF_WIDTH);
        for (LivingEntity victim : HollowStrike.victims(level, box)) {
            HollowStrike.hit(level, owner, victim, damage, position(), 0.0, 0.45);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BITE) {
            clientBite = tickCount;
            for (int i = 0; i < 12; i++) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getX() + (random.nextDouble() - 0.5) * 0.8, getY() + 0.2,
                        getZ() + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.3 + random.nextDouble() * 0.2, 0.0);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "claw", 0,
                state -> state.setAndContinue(clientBite < 0 ? HIDDEN : GRAB)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    /** Only for {@code /summon} (tests, screenshots): {@code Warmup}; the claw itself is never saved. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Warmup")) {
            entityData.set(DATA_WARMUP, Math.max(1, tag.getInt("Warmup")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double out = CRACK_REACH - HALF_WIDTH;
        return getBoundingBox().inflate(out, 0.0, out);
    }
}
