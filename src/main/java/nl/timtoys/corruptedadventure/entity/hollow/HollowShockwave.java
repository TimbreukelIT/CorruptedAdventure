package nl.timtoys.corruptedadventure.entity.hollow;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;
import nl.timtoys.corruptedadventure.registry.ModParticleTypes;
import org.jetbrains.annotations.Nullable;

/**
 * The shock wave of The Hollow's ground slam: a ring that grows along the ground from where it stands, {@code speed}
 * blocks per tick up to {@code radius}, then is gone. Whoever stands on the ground where it passes is hit once and thrown
 * up and away; a jump lets it pass underneath, distance outruns it. The server hits; the client throws up dust from the
 * ground along the ring and draws its glowing front ({@code client/entity/hollow/HollowShockwaveRenderer}) from the
 * synced radius and speed. Leaves nothing in the world and is never saved.
 */
public class HollowShockwave extends Entity {
    private static final EntityDataAccessor<Float> DATA_RADIUS = SynchedEntityData.defineId(HollowShockwave.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(HollowShockwave.class, EntityDataSerializers.FLOAT);
    /** Blocks between two dust spots along the ring (client). */
    private static final double DUST_SPACING = 0.7;

    @Nullable
    private TheHollow owner;
    private float damage = 8.0F;
    private double lift = 0.6;
    private final IntSet hit = new IntOpenHashSet();

    public HollowShockwave(EntityType<? extends HollowShockwave> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** A wave from {@code feet}, its ground, hurting as {@code owner}'s attack. */
    public static HollowShockwave spawn(ServerLevel level, @Nullable TheHollow owner, Vec3 feet, float radius, float speed,
                                        float damage, double lift) {
        HollowShockwave wave = new HollowShockwave(ModEntityTypes.HOLLOW_SHOCKWAVE.get(), level);
        wave.moveTo(feet.x, feet.y, feet.z, 0.0F, 0.0F);
        wave.owner = owner;
        wave.damage = damage;
        wave.lift = lift;
        wave.entityData.set(DATA_RADIUS, radius);
        wave.entityData.set(DATA_SPEED, speed);
        level.addFreshEntity(wave);
        return wave;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 11.0F);
        builder.define(DATA_SPEED, 0.45F);
    }

    public float maxRadius() {
        return entityData.get(DATA_RADIUS);
    }

    /** The front's radius {@code ticks} after the slam (partial ticks allowed). */
    public float radiusAt(float ticks) {
        return Math.min(maxRadius(), Math.max(0.0F, ticks) * entityData.get(DATA_SPEED));
    }

    @Override
    public void tick() {
        super.tick();
        float before = radiusAt(tickCount - 1);
        float now = radiusAt(tickCount);
        if (level().isClientSide()) {
            dust(now);
        } else {
            strike((ServerLevel) level(), before, now);
        }
        if (!level().isClientSide() && before >= maxRadius()) {
            discard();
        }
    }

    private void strike(ServerLevel level, float before, float now) {
        if (now <= before) {
            return;
        }
        AABB box = new AABB(getX() - now - 1.0, getY() - HollowStrikeShapes.WAVE_HEIGHT - 0.5, getZ() - now - 1.0,
                getX() + now + 1.0, getY() + HollowStrikeShapes.WAVE_HEIGHT + 0.5, getZ() + now + 1.0);
        for (LivingEntity victim : HollowStrike.victims(level, box)) {
            if (hit.contains(victim.getId())) {
                continue;
            }
            double edge = Math.max(0.0, Math.sqrt(Mth.square(victim.getX() - getX()) + Mth.square(victim.getZ() - getZ()))
                    - victim.getBbWidth() * 0.5);
            if (HollowStrikeShapes.waveHits(before, now, edge, victim.getY() - getY(), victim.onGround())) {
                hit.add(victim.getId());
                HollowStrike.hit(level, owner, victim, damage, position(), 0.5, lift);
            }
        }
    }

    /** Client: dust and corruption thrown up from the ground along the front. */
    private void dust(float radius) {
        if (radius <= 0.0F || radius >= maxRadius()) {
            return;
        }
        int spots = Math.max(6, (int) (Mth.TWO_PI * radius / DUST_SPACING));
        Level level = level();
        for (int i = 0; i < spots; i++) {
            double angle = (i + random.nextFloat() * 0.6) * Mth.TWO_PI / spots;
            double x = getX() + Math.cos(angle) * radius;
            double z = getZ() + Math.sin(angle) * radius;
            BlockPos ground = BlockPos.containing(x, getY() - 0.5, z);
            BlockState state = level.getBlockState(ground);
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, getY() + 0.1, z,
                        Math.cos(angle) * 0.15, 0.25 + random.nextFloat() * 0.2, Math.sin(angle) * 0.15);
            }
            if (i % 8 == 0) {
                level.addParticle(ModParticleTypes.GLITCH_PIXEL.get(), x, getY() + 0.3, z, 0.0, 0.05, 0.0);
            }
        }
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

    /** Only for {@code /summon} (tests, screenshots): {@code Radius} and {@code Speed}; the wave itself is never saved. */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Radius")) {
            entityData.set(DATA_RADIUS, Math.max(0.5F, tag.getFloat("Radius")));
        }
        if (tag.contains("Speed")) {
            entityData.set(DATA_SPEED, Math.max(0.01F, tag.getFloat("Speed")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(maxRadius() + 1.0, 1.0, maxRadius() + 1.0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
