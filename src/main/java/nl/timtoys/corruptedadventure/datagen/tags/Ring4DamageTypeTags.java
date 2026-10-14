package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.condition.Ring4DamageTypes;

import java.util.function.Function;

/**
 * Damage-type tags of the ring 4 conditions (poison), written by {@code ModDamageTypeTagsProvider}. Owned by stream C
 * ({@code docs/plans/ring-4/waves.md}). Poison has the tags of vanilla's {@code wither} that matter for players: through
 * armour, no knockback; plus NeoForge's poison and environment tags. It also bypasses the hurt cooldown: the vanilla
 * poison of the second threshold hits just before it, and inside the cooldown a hit of equal size is swallowed.
 */
public final class Ring4DamageTypeTags {
    private Ring4DamageTypeTags() {
    }

    public static void addTags(Function<TagKey<DamageType>, TagsProvider.TagAppender<DamageType>> tag) {
        tag.apply(DamageTypeTags.BYPASSES_ARMOR).add(Ring4DamageTypes.POISON);
        tag.apply(DamageTypeTags.NO_KNOCKBACK).add(Ring4DamageTypes.POISON);
        tag.apply(DamageTypeTags.BYPASSES_COOLDOWN).add(Ring4DamageTypes.POISON);
        tag.apply(Tags.DamageTypes.IS_POISON).add(Ring4DamageTypes.POISON);
        tag.apply(Tags.DamageTypes.IS_ENVIRONMENT).add(Ring4DamageTypes.POISON);
    }
}
