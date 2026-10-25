package nl.timtoys.corruptedadventure.datagen.lang;

import net.minecraft.Util;
import nl.timtoys.corruptedadventure.item.coating.Coating;
import nl.timtoys.corruptedadventure.item.coating.CoatingMaterial;
import nl.timtoys.corruptedadventure.item.coating.CoatingTemplates;

import java.util.Map;

/**
 * The coatings (phase 2 golf 1, stream C): the template items, the trim pattern names (tooltip line and template title)
 * and the trim material names, like vanilla's "Quartz Material". English is mandatory, Dutch welcome.
 */
public final class CoatingText {
    /** Per coating: the template item, the pattern. */
    private static final Map<Coating, String[]> ENGLISH = Map.of(
            Coating.DEHYDRATION, new String[]{"Dehydration Coating Template", "Dehydration Coating"},
            Coating.HEAT, new String[]{"Heat Coating Template", "Heat Coating"},
            Coating.COLD, new String[]{"Cold Coating Template", "Cold Coating"},
            Coating.POISON, new String[]{"Poison Coating Template", "Poison Coating"},
            Coating.VOID, new String[]{"Void Coating Template", "Void Coating"},
            Coating.SILENCE, new String[]{"Silence Coating Template", "Silence Coating"});
    private static final Map<Coating, String[]> DUTCH = Map.of(
            Coating.DEHYDRATION, new String[]{"Uitdrogingscoatingsjabloon", "Uitdrogingscoating"},
            Coating.HEAT, new String[]{"Hittecoatingsjabloon", "Hittecoating"},
            Coating.COLD, new String[]{"Koudecoatingsjabloon", "Koudecoating"},
            Coating.POISON, new String[]{"Gifcoatingsjabloon", "Gifcoating"},
            Coating.VOID, new String[]{"Voidcoatingsjabloon", "Voidcoating"},
            Coating.SILENCE, new String[]{"Stiltecoatingsjabloon", "Stiltecoating"});
    private static final Map<CoatingMaterial, String> ENGLISH_MATERIALS = Map.of(
            CoatingMaterial.TURQUOISE, "Turquoise Material",
            CoatingMaterial.DEW_CRYSTAL, "Dew Crystal Material",
            CoatingMaterial.FIRE_CRYSTAL, "Fire Crystal Material",
            CoatingMaterial.PRIMORDIAL_ICE, "Primordial Ice Material",
            CoatingMaterial.WHITE_CORE, "White Core Material");
    private static final Map<CoatingMaterial, String> DUTCH_MATERIALS = Map.of(
            CoatingMaterial.TURQUOISE, "Materiaal: turkoois",
            CoatingMaterial.DEW_CRYSTAL, "Materiaal: dauwkristal",
            CoatingMaterial.FIRE_CRYSTAL, "Materiaal: vuurkristal",
            CoatingMaterial.PRIMORDIAL_ICE, "Materiaal: oerijs",
            CoatingMaterial.WHITE_CORE, "Materiaal: witte kern");

    private CoatingText() {
    }

    public static void addEnglish(LangOutput lang) {
        add(lang, ENGLISH, ENGLISH_MATERIALS);
        lang.add(CoatingTemplates.ADDITIONS_SLOT_KEY, "Add %s");
    }

    public static void addDutch(LangOutput lang) {
        add(lang, DUTCH, DUTCH_MATERIALS);
        lang.add(CoatingTemplates.ADDITIONS_SLOT_KEY, "Voeg %s toe");
    }

    private static void add(LangOutput lang, Map<Coating, String[]> names, Map<CoatingMaterial, String> materials) {
        for (Coating coating : Coating.values()) {
            String[] name = names.get(coating);
            lang.addItem(coating::template, name[0]);
            lang.add(Util.makeDescriptionId("trim_pattern", coating.pattern().location()), name[1]);
        }
        for (CoatingMaterial material : CoatingMaterial.values()) {
            lang.add(Util.makeDescriptionId("trim_material", material.key().location()), materials.get(material));
        }
    }
}
