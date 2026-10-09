package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import nl.timtoys.corruptedadventure.registry.ModTags;

/**
 * Spawn placements for vanilla mobs that our biomes spawn but vanilla never spawns naturally. Without one, NeoForge
 * logs an error and the mob spawns without any rule. Camel ({@code oasis_flats}): like other animals, on
 * {@code #animals_spawnable_on} (the grassy shores) in light. Stray (Nether ring 3, 53b): vanilla's rule wants sky
 * light, which the Nether never has, so in {@code #is_nether_ring_3} the ordinary monster rule is enough (OR with vanilla's).
 */
public final class BiomeSpawnPlacements {
    private BiomeSpawnPlacements() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BiomeSpawnPlacements::onRegisterSpawnPlacements);
    }

    private static void onRegisterSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(EntityType.CAMEL, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(EntityType.STRAY, BiomeSpawnPlacements::checkNetherRing3Stray, RegisterSpawnPlacementsEvent.Operation.OR);
    }

    static boolean checkNetherRing3Stray(EntityType<Stray> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBiome(pos).is(ModTags.Biomes.IS_NETHER_RING_3) && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }
}
