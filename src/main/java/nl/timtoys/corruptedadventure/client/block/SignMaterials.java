package nl.timtoys.corruptedadventure.client.block;

import net.minecraft.client.renderer.Sheets;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.timtoys.corruptedadventure.block.ModWoodTypes;

/**
 * The sign materials of our sign wood types. {@code Sheets} builds them from {@code WoodType.values()} when it first
 * loads, which is normally after our types exist; adding them in client setup (as NeoForge advises for modded wood types)
 * keeps the signs independent of that class-load order. The vanilla sign renderers do the rest.
 */
public final class SignMaterials {
    private SignMaterials() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        // Sheets' material maps are plain hash maps and client setup runs in parallel across mods.
        event.enqueueWork(() -> Sheets.addWoodType(ModWoodTypes.CORRUPTED));
    }
}
