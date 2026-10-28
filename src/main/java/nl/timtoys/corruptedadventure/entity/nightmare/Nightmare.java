package nl.timtoys.corruptedadventure.entity.nightmare;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.config.NightmareConfig;
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
 * {@link #nightmareAttributes}. Roaming, the arrival and the return come later as a shared system (steps 2 and 3).
 */
public abstract class Nightmare extends Monster implements GeoEntity {
    private static final ResourceLocation PLAYERS_MODIFIER = CorruptedAdventure.id("nightmare_players");
    private static final int CHECK_TICKS = 20;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent;
    private int absentTicks;

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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (!isDeadOrDying() && tickCount % CHECK_TICKS == 0) {
            applyConfig();
            checkPresence();
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
        float newMax = getMaxHealth();
        if (newMax != oldMax) {
            setHealth(ratio * newMax);
        }
        setBase(Attributes.ATTACK_DAMAGE, config.attackDamage.get());
        setBase(Attributes.ATTACK_KNOCKBACK, config.attackKnockback.get());
        setBase(Attributes.MOVEMENT_SPEED, config.movementSpeed.get());
        setBase(Attributes.ARMOR, config.armor.get());
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

    /** Leaves without a drop ({@link #onRetreat} for its effects). Step 3 keeps it in SavedData and heals it. */
    public void retreat() {
        if (level() instanceof ServerLevel serverLevel) {
            onRetreat(serverLevel);
        }
        discard();
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
