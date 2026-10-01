package nl.timtoys.corruptedadventure.worldgen.biome;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Fake registry lookup: one unbound stand-alone reference per known key (same instance on every call); every tag exists, empty. */
final class StandaloneHolderGetter<T> implements HolderGetter<T> {
    private final HolderOwner<T> owner = new HolderOwner<>() {
    };
    private final Map<ResourceKey<T>, Holder.Reference<T>> references = new HashMap<>();
    private final Predicate<ResourceKey<T>> known;

    StandaloneHolderGetter(Predicate<ResourceKey<T>> known) {
        this.known = known;
    }

    HolderOwner<T> owner() {
        return owner;
    }

    /** JSON registry ops that know only {@code registry}, with a fresh getter for {@code known}. */
    static <T> RegistryOps<JsonElement> ops(ResourceKey<? extends Registry<T>> registry, Predicate<ResourceKey<T>> known) {
        StandaloneHolderGetter<T> getter = new StandaloneHolderGetter<>(known);
        return RegistryOps.create(JsonOps.INSTANCE, new RegistryOps.RegistryInfoLookup() {
            @Override
            @SuppressWarnings("unchecked")
            public <E> Optional<RegistryOps.RegistryInfo<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
                return registryKey.equals(registry)
                        ? Optional.of((RegistryOps.RegistryInfo<E>) (Object) new RegistryOps.RegistryInfo<>(getter.owner(), getter, Lifecycle.stable()))
                        : Optional.empty();
            }
        });
    }

    @Override
    public Optional<Holder.Reference<T>> get(ResourceKey<T> key) {
        return known.test(key)
                ? Optional.of(references.computeIfAbsent(key, k -> Holder.Reference.createStandAlone(owner, k)))
                : Optional.empty();
    }

    @Override
    public Optional<HolderSet.Named<T>> get(TagKey<T> tag) {
        return Optional.of(HolderSet.emptyNamed(owner, tag));
    }
}
