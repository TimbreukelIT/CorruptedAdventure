package nl.timtoys.corruptedadventure.datagen.lang;

import nl.timtoys.corruptedadventure.worldgen.feature.HerobrineSignFeature;

import java.util.List;

/**
 * The translatable texts of the ring 5 horror signs (Herobrine's signs, package 94, V5-A; keys
 * {@link HerobrineSignFeature#key}, one per non-blank entry of {@link HerobrineSignFeature#TEXTS}, in that order). Owned by
 * stream W-biomes ({@code docs/plans/ring-5/waves.md}); English is mandatory, Dutch welcome.
 */
public final class Ring5SignText {
    static final List<String> ENGLISH = List.of("Go back", "He sees you", "Don't turn around", "You were here before",
            "This is not your world", "Wake up", "He was here", "No one is coming", "Stop digging");
    static final List<String> DUTCH = List.of("Ga terug", "Hij ziet je", "Draai je niet om", "Je was hier al eens",
            "Dit is jouw wereld niet", "Word wakker", "Hij was hier", "Er komt niemand", "Stop met graven");

    private Ring5SignText() {
    }

    public static void addEnglish(LangOutput lang) {
        add(lang, ENGLISH);
    }

    public static void addDutch(LangOutput lang) {
        add(lang, DUTCH);
    }

    private static void add(LangOutput lang, List<String> texts) {
        List<String> ids = HerobrineSignFeature.TEXTS.stream().filter(text -> !text.isEmpty()).toList();
        if (ids.size() != texts.size()) {
            throw new IllegalStateException("one text per sign: " + ids.size() + " ids, " + texts.size() + " texts");
        }
        for (int i = 0; i < ids.size(); i++) {
            lang.add(HerobrineSignFeature.key(ids.get(i)), texts.get(i));
        }
    }
}
