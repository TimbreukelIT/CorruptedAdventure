package nl.timtoys.corruptedadventure.worldgen.biome;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

/**
 * Spawn placements for vanilla mobs that our biomes spawn but vanilla never spawns naturally. Without one, NeoForge
 * logs an error and the mob spawns without any rule. Camel ({@code oasis_flats}): like other animals, on
 * {@code #animals_spawnable_on} (the grassy shores) in light.
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
    }
}
