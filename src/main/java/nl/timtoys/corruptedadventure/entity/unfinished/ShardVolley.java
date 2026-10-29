package nl.timtoys.corruptedadventure.entity.unfinished;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.timtoys.corruptedadventure.config.ServerConfig;
import nl.timtoys.corruptedadventure.config.UnfinishedConfig;
import nl.timtoys.corruptedadventure.registry.ModBlocks;
import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * One memory The Unfinished throws: a cluster of {@link MemoryShard}s from one of the four rings before his. It tears off
 * his chest, hovers above his crown while he winds up, then flies as one rigid lump (every shard gets the same velocity)
 * to where its target stood. The first shard down is the impact: everyone within {@code throwImpactRadius} is hurt and
 * thrown back, and the rest of the lump bursts apart. Each shard that lands on open ground with natural ground (or
 * another shard) under it lies there as a real block for {@code throwLingerSeconds}, then flickers away and the spot is
 * exactly as before ({@link UnfinishedTempBlocks}); anywhere else (a player's build, a fluid, a full spot, someone
 * standing there, too many lying) it bursts into pixels. Server-side; held by its shards, so it ends with them.
 */
public final class ShardVolley {
    /**
     * The memories: ring 1 soil, mossy stone and oak, ring 2 sandstone and cracked clay, ring 3 basalt, ring 4 tidestone.
     * Nothing that falls (sand) or spreads (grass) while it lies.
     */
    private static final List<List<Supplier<BlockState>>> RINGS = List.of(
            List.of(Blocks.DIRT::defaultBlockState, Blocks.COARSE_DIRT::defaultBlockState, Blocks.MOSSY_COBBLESTONE::defaultBlockState,
                    Blocks.OAK_LOG::defaultBlockState),
            List.of(Blocks.SANDSTONE::defaultBlockState, Blocks.SANDSTONE::defaultBlockState, Blocks.RED_SANDSTONE::defaultBlockState,
                    () -> ModBlocks.CRACKED_CLAY.get().defaultBlockState()),
            List.of(Blocks.BASALT::defaultBlockState, Blocks.BLACKSTONE::defaultBlockState, Blocks.SMOOTH_BASALT::defaultBlockState,
                    Blocks.MAGMA_BLOCK::defaultBlockState),
            List.of(() -> ModBlocks.TIDESTONE.get().defaultBlockState(), () -> ModBlocks.MOSSY_TIDESTONE.get().defaultBlockState(),
                    () -> ModBlocks.COBBLED_TIDESTONE.get().defaultBlockState()));
    private static final double HOVER_STEER = 0.3;
    /** Sideways speed the lump bursts apart with on impact. */
    private static final double BURST = 0.18;

    private final TheUnfinished boss;
    private final List<MemoryShard> shards = new ArrayList<>();
    private final List<Vec3> offsets = new ArrayList<>();
    private Vec3 landing = Vec3.ZERO;
    private boolean thrown;
    private boolean impacted;

    private ShardVolley(TheUnfinished boss) {
        this.boss = boss;
    }

    /** Tears {@code count} shards of one random ring's memory off at {@code at} (a rough ball round it). */
    public static ShardVolley tearOff(ServerLevel level, TheUnfinished boss, Vec3 at, int count) {
        ShardVolley volley = new ShardVolley(boss);
        RandomSource random = boss.getRandom();
        List<Supplier<BlockState>> ring = RINGS.get(random.nextInt(RINGS.size()));
        for (int i = 0; i < count; i++) {
            // The first one is the lump's core.
            Vec3 offset = i == 0 ? Vec3.ZERO : new Vec3(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1)
                    .add(random.nextDouble() * 0.2, random.nextDouble() * 0.2, random.nextDouble() * 0.2);
            BlockState block = ring.get(random.nextInt(ring.size())).get();
            MemoryShard shard = new MemoryShard(ModEntityTypes.MEMORY_SHARD.get(), level, block, at.add(offset), volley);
            volley.shards.add(shard);
            volley.offsets.add(offset);
            level.addFreshEntity(shard);
        }
        UnfinishedEffects.shardTear(level, at);
        return volley;
    }

    /** Steers the hovering lump toward {@code centre}. */
    void hover(Vec3 centre) {
        if (thrown) {
            return;
        }
        for (int i = 0; i < shards.size(); i++) {
            shards.get(i).steer(centre.add(offsets.get(i)), HOVER_STEER);
        }
    }

    /** Throws the lump so its middle comes down at {@code to} after about {@code ticks} ticks. */
    void launch(Vec3 to, int ticks) {
        if (thrown || shards.isEmpty()) {
            return;
        }
        thrown = true;
        landing = to;
        Vec3 from = centre();
        double t = Math.max(1, ticks);
        // Per tick the speed drops by g before the move: after t ticks it has fallen g t (t + 1) / 2.
        Vec3 velocity = new Vec3((to.x - from.x) / t, (to.y - from.y + MemoryShard.GRAVITY * t * (t + 1) / 2.0) / t,
                (to.z - from.z) / t);
        for (MemoryShard shard : shards) {
            shard.launch(velocity);
        }
    }

    /** Lets it fall apart where it hovers, harmlessly (his throw was broken off). */
    public void drop() {
        if (thrown) {
            return;
        }
        thrown = true;
        impacted = true;
        RandomSource random = boss.getRandom();
        for (MemoryShard shard : shards) {
            // Barely apart: each falls about where it hung.
            shard.launch(new Vec3((random.nextDouble() - 0.5) * 0.04, 0.05, (random.nextDouble() - 0.5) * 0.04));
        }
    }

    boolean isThrown() {
        return thrown;
    }

    boolean isOver() {
        return shards.stream().allMatch(MemoryShard::isRemoved);
    }

    Vec3 centre() {
        Vec3 sum = Vec3.ZERO;
        int n = 0;
        for (MemoryShard shard : shards) {
            if (!shard.isRemoved()) {
                sum = sum.add(shard.position());
                n++;
            }
        }
        return n == 0 ? landing : sum.scale(1.0 / n);
    }

    /** Each server tick of each shard: the first living one marks the spot it will come down on. */
    void tick(ServerLevel level, MemoryShard shard) {
        if (!thrown || impacted || shard.tickCount % 3 != 0) {
            return;
        }
        for (MemoryShard first : shards) {
            if (!first.isRemoved()) {
                if (first == shard) {
                    UnfinishedEffects.shardMark(level, landing, ServerConfig.UNFINISHED.throwImpactRadius.get());
                }
                return;
            }
        }
    }

    void landed(ServerLevel level, MemoryShard shard) {
        if (!impacted) {
            impacted = true;
            impact(level, shard.position());
        }
        lie(level, shard);
    }

    private void impact(ServerLevel level, Vec3 at) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        double radius = config.throwImpactRadius.get();
        // Grows with his strength (returns) like his slam.
        double base = boss.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
        float damage = config.throwDamage.get().floatValue()
                * (float) (base > 0.0 ? boss.getAttributeValue(Attributes.ATTACK_DAMAGE) / base : 1.0);
        AABB box = new AABB(at, at).inflate(radius, 3.0, radius);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != boss && e.isAlive())) {
            double dx = hit.getX() - at.x;
            double dz = hit.getZ() - at.z;
            if (dx * dx + dz * dz > radius * radius) {
                continue;
            }
            if (hit.hurt(level.damageSources().mobProjectile(shards.get(0), boss), damage)) {
                hit.knockback(0.8, -dx, -dz);
                hit.hurtMarked = true;
            }
        }
        UnfinishedEffects.shardImpact(level, at);
        // The rest of the lump bursts apart.
        RandomSource random = boss.getRandom();
        for (MemoryShard other : shards) {
            if (!other.isRemoved()) {
                Vec3 v = other.getDeltaMovement();
                other.launch(new Vec3(v.x * 0.3 + (random.nextDouble() - 0.5) * BURST * 2.0, 0.25 + random.nextDouble() * 0.2,
                        v.z * 0.3 + (random.nextDouble() - 0.5) * BURST * 2.0));
            }
        }
    }

    private void lie(ServerLevel level, MemoryShard shard) {
        UnfinishedConfig config = ServerConfig.UNFINISHED;
        UnfinishedTempBlocks data = UnfinishedTempBlocks.of(level);
        BlockPos pos = shard.blockPosition();
        if (!canLie(level, data, pos)) {
            pos = pos.above();
        }
        if (!canLie(level, data, pos) || data.count(UnfinishedTempBlocks.Kind.SHARD) >= config.throwMaxLyingBlocks.get()) {
            UnfinishedEffects.shardShatter(level, shard.position());
            return;
        }
        long until = level.getGameTime() + config.throwLingerSeconds.get() * 20L + boss.getRandom().nextInt(20);
        if (!data.swap(level, pos, shard.block(), until, UnfinishedTempBlocks.Kind.SHARD)) {
            UnfinishedEffects.shardShatter(level, shard.position());
        }
    }

    private static boolean canLie(ServerLevel level, UnfinishedTempBlocks data, BlockPos pos) {
        if (!level.isLoaded(pos) || data.isTracked(pos)) {
            return false;
        }
        BlockPos below = pos.below();
        return UnfinishedNature.isOpen(level.getBlockState(pos))
                && (UnfinishedNature.isGround(level.getBlockState(below)) || data.isShard(below))
                && level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty();
    }
}
