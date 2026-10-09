package nl.timtoys.corruptedadventure.datagen.tags;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.Tags;
import nl.timtoys.corruptedadventure.condition.Ring3DamageTypes;

import java.util.function.Function;

/**
 * Damage-type tags of the ring 3 conditions (heat, cold), written by {@code ModDamageTypeTagsProvider}. Owned by stream C
 * ({@code docs/plans/ring-3/waves.md}). Heat has the tags of vanilla's {@code on_fire} that matter for players: fire
 * (so fire resistance blocks it and fire protection reduces it), through armour, no knockback; plus the environment tag.
 * Cold has those of vanilla's {@code freeze}: freezing (the freeze hurt sound; the {@code freezeDamage} game rule turns
 * it off), through armour, no knockback, environment.
 */
public final class Ring3DamageTypeTags {
    private Ring3DamageTypeTags() {
    }

    public static void addTags(Function<TagKey<DamageType>, TagsProvider.TagAppender<DamageType>> tag) {
        tag.apply(DamageTypeTags.IS_FIRE).add(Ring3DamageTypes.HEAT);
        tag.apply(DamageTypeTags.BYPASSES_ARMOR).add(Ring3DamageTypes.HEAT);
        tag.apply(DamageTypeTags.NO_KNOCKBACK).add(Ring3DamageTypes.HEAT);
        tag.apply(Tags.DamageTypes.IS_ENVIRONMENT).add(Ring3DamageTypes.HEAT);
        tag.apply(DamageTypeTags.IS_FREEZING).add(Ring3DamageTypes.COLD);
        tag.apply(DamageTypeTags.BYPASSES_ARMOR).add(Ring3DamageTypes.COLD);
        tag.apply(DamageTypeTags.NO_KNOCKBACK).add(Ring3DamageTypes.COLD);
        tag.apply(Tags.DamageTypes.IS_ENVIRONMENT).add(Ring3DamageTypes.COLD);
    }
}
