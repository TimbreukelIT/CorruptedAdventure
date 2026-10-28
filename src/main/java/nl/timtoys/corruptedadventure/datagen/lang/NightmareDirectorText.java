package nl.timtoys.corruptedadventure.datagen.lang;

/** The Nightmare director's test commands ({@code /ca nightmare}, phase 4 steps 2 and 3). English and Dutch. */
public final class NightmareDirectorText {
    private static final String KEY = "commands.corruptedadventure.nightmare.";

    private NightmareDirectorText() {
    }

    public static void addEnglish(LangOutput lang) {
        lang.add(KEY + "not_ring_world", "No Nightmares: this is not a ring world");
        lang.add(KEY + "status.header", "Nightmares:");
        lang.add(KEY + "status.missing", "%1$s (ring %2$s): no entity yet (%3$s)");
        lang.add(KEY + "status.line", "%1$s (ring %2$s): %3$s; defeated %4$s times, strength %5$sx");
        lang.add(KEY + "status.unplaced", "roaming, not placed yet");
        lang.add(KEY + "status.roaming", "roaming at %1$s %2$s, heading %3$s");
        lang.add(KEY + "status.coming", "coming for %1$s at %2$s %3$s %4$s (omen tick %5$s)");
        lang.add(KEY + "status.present", "here at %1$s %2$s (%3$s, missing %4$s s)");
        lang.add(KEY + "status.defeated", "defeated, back in %1$s s");
        lang.add(KEY + "status.threat", "Threat of %1$s (threshold %2$s) per ring: %3$s");
        lang.add(KEY + "threat.set", "Threat of %1$s in ring %2$s is now %3$s");
        lang.add(KEY + "arrive.coming", "%1$s is coming for %2$s");
        lang.add(KEY + "arrive.no_entity", "%1$s does not exist yet");
        lang.add(KEY + "arrive.already_coming", "%1$s is already coming");
        lang.add(KEY + "arrive.already_present", "%1$s is already here");
        lang.add(KEY + "arrive.not_overworld", "%1$s is not in the overworld");
        lang.add(KEY + "reset.done", "%1$s reset: roaming, never defeated");
    }

    public static void addDutch(LangOutput lang) {
        lang.add(KEY + "not_ring_world", "Geen Nightmares: dit is geen ringwereld");
        lang.add(KEY + "status.header", "Nightmares:");
        lang.add(KEY + "status.missing", "%1$s (ring %2$s): nog geen entity (%3$s)");
        lang.add(KEY + "status.line", "%1$s (ring %2$s): %3$s; %4$s keer verslagen, sterkte %5$sx");
        lang.add(KEY + "status.unplaced", "zwervend, nog niet geplaatst");
        lang.add(KEY + "status.roaming", "zwervend op %1$s %2$s, richting %3$s");
        lang.add(KEY + "status.coming", "komt voor %1$s op %2$s %3$s %4$s (voortekentick %5$s)");
        lang.add(KEY + "status.present", "hier op %1$s %2$s (%3$s, %4$s s zoek)");
        lang.add(KEY + "status.defeated", "verslagen, terug over %1$s s");
        lang.add(KEY + "status.threat", "Dreiging van %1$s (drempel %2$s) per ring: %3$s");
        lang.add(KEY + "threat.set", "Dreiging van %1$s in ring %2$s is nu %3$s");
        lang.add(KEY + "arrive.coming", "%1$s komt voor %2$s");
        lang.add(KEY + "arrive.no_entity", "%1$s bestaat nog niet");
        lang.add(KEY + "arrive.already_coming", "%1$s komt al");
        lang.add(KEY + "arrive.already_present", "%1$s is er al");
        lang.add(KEY + "arrive.not_overworld", "%1$s is niet in de overworld");
        lang.add(KEY + "reset.done", "%1$s gereset: zwervend, nooit verslagen");
    }
}
