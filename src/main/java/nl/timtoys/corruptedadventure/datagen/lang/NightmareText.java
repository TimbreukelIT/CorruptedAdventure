package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.registry.ModEntityTypes;

/** The Nightmares (phase 4): entity names, which the boss bars show too. English is mandatory, Dutch welcome. */
public final class NightmareText {
    private NightmareText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(ModEntityTypes.THE_HOLLOW.get().getDescriptionId(), "The Hollow");
        lang.add(ModEntityTypes.HOLLOW_RIFT.get().getDescriptionId(), "Rift of The Hollow");
        lang.add(ModEntityTypes.HOLLOW_SHOCKWAVE.get().getDescriptionId(), "Shock Wave of The Hollow");
        lang.add(ModEntityTypes.HOLLOW_VOID_CLAW.get().getDescriptionId(), "Void Claw of The Hollow");
        lang.add(ModEntityTypes.THE_LOST_KEEPER.get().getDescriptionId(), "The Lost Keeper");
        lang.add(ModEntityTypes.THE_UNFINISHED.get().getDescriptionId(), "The Unfinished");
        lang.add(ModEntityTypes.THE_LAYERED.get().getDescriptionId(), "The Layered");
        lang.add(ModEntityTypes.MEMORY_SHARD.get().getDescriptionId(), "Memory Shard");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(ModEntityTypes.THE_HOLLOW.get().getDescriptionId(), "De Holle");
        lang.add(ModEntityTypes.HOLLOW_RIFT.get().getDescriptionId(), "Scheur van De Holle");
        lang.add(ModEntityTypes.HOLLOW_SHOCKWAVE.get().getDescriptionId(), "Schokgolf van De Holle");
        lang.add(ModEntityTypes.HOLLOW_VOID_CLAW.get().getDescriptionId(), "Void-klauw van De Holle");
        lang.add(ModEntityTypes.THE_LOST_KEEPER.get().getDescriptionId(), "De Verloren Hoeder");
        lang.add(ModEntityTypes.THE_UNFINISHED.get().getDescriptionId(), "De Onvoltooide");
        lang.add(ModEntityTypes.THE_LAYERED.get().getDescriptionId(), "De Gelaagde");
        lang.add(ModEntityTypes.MEMORY_SHARD.get().getDescriptionId(), "Herinneringsbrok");
    }
}
