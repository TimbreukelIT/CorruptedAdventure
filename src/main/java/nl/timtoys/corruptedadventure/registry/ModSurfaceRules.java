package nl.timtoys.corruptedadventure.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.timtoys.corruptedadventure.CorruptedAdventure;
import nl.timtoys.corruptedadventure.worldgen.surface.BiomeSwitchRule;
import nl.timtoys.corruptedadventure.worldgen.surface.YBandsRule;

/** Surface rule types (the {@code then_run} side of a surface rule); the conditions are in {@link ModSurfaceConditions}. */
public final class ModSurfaceRules {
    private static final DeferredRegister<MapCodec<? extends SurfaceRules.RuleSource>> RULES =
            DeferredRegister.create(Registries.MATERIAL_RULE, CorruptedAdventure.MOD_ID);

    // opt/surface
    /** {@code corruptedadventure:y_bands} */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.RuleSource>, MapCodec<YBandsRule>> Y_BANDS =
            RULES.register("y_bands", () -> YBandsRule.MAP_CODEC);
    /** {@code corruptedadventure:biome_switch} (put in at load by {@code BiomeSwitches}, not written in the JSON) */
    public static final DeferredHolder<MapCodec<? extends SurfaceRules.RuleSource>, MapCodec<BiomeSwitchRule>> BIOME_SWITCH =
            RULES.register("biome_switch", () -> BiomeSwitchRule.MAP_CODEC);

    private ModSurfaceRules() {
    }

    public static void register(IEventBus modBus) {
        RULES.register(modBus);
    }
}
