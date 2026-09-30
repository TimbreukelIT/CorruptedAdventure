# Change 16 — Plaatsing zielkristal in Nether-ring 1

Scope: `worldgen` — change 16

**Doel:** het zielkristal-erts (blok uit change 15) wordt deterministisch per seed gegenereerd in de netherrack en de crimson-bodem (crimson nylium) van **alleen Nether-ring 1**: zeldzaam genoeg om een sleutelmateriaal te zijn, vaak genoeg om op elke seed vindbaar te zijn. Puur data (vanilla `minecraft:ore`-feature via datagen), geen eigen Java-feature.

---

## Afhankelijkheden en aannames

- **Change 15** levert twee blokken (via `ModBlocks`, met blockitem, loot en tags): `corruptedadventure:soul_crystal_ore` (netherrack-basis) en `corruptedadventure:crimson_soul_crystal_ore` (crimson-nylium-variant).
- **Change 14** levert de Nether-ring 1-biomes als **eigen kopieën** (`corruptedadventure:…` van nether wastes, crimson forest, warped forest) en gebruikt ze alleen in de geneste biome-source van `RING_1` in de Nether (preset `corrupted_world`). Vanilla Nether-ores (quartz, goud, ancient debris) zitten er al in. De exacte keys lees ik bij de bouw uit `ModBiomes`.
- **Change 8** levert een `BiomeTagsProvider` in datagen (voor de `has_structure/*`-tags). Bestaat die niet, dan maakt deze change een minimale `ModBiomeTagsProvider`.
- **Change 12** levert de eerste eigen configured/placed features (dode bomen, tunnels) en dus de bootstrap-classes in `worldgen/feature/`. Deze change hangt de erts-feature daarin; bestaan ze niet, dan maakt deze change ze. Heeft change 12 features op een andere manier aan biomes gehangen dan met een biome modifier (bijv. direct in de biome-bootstrap), dan volgen we **dat** mechanisme: één manier in het hele project.
- `DataGenerators` gebruikt een `RegistrySetBuilder` voor datapack-registries (sinds change 5); configured features, placed features en biome modifiers gaan daar ook in.
- Nether-dimensietype `corrupted_nether` = kopie van vanilla: `min_y` 0, `height` 256, `logical_height` 128. Nether-ring 1 = 0–375 Nether-blokken, grensruis ±24, overgangszone ~32 (harde biomewissel op gewicht 0,5).

---

## Plan

1. **Configured feature** `corruptedadventure:soul_crystal_ore` (`Registries.CONFIGURED_FEATURE`, bootstrap in `worldgen/feature/`):
   - **Eén** `Feature.ORE` met `OreConfiguration` en twee doelen (zoals vanilla's steen/deepslate-ores: één ader, per vervangen blok de passende variant):
     - `new BlockMatchTest(Blocks.NETHERRACK)` → `soul_crystal_ore`
     - `new BlockMatchTest(Blocks.CRIMSON_NYLIUM)` → `crimson_soul_crystal_ore`
   - Eén feature i.p.v. twee: dezelfde ader loopt naadloos van netherrack door de crimson-bodem, één placed feature, één tarief om te balanceren.
   - Bewust `BlockMatchTest` en niet de tag `#base_stone_nether`: basalt en blackstone horen niet bij het design; warped nylium ook niet (vraag 3).
   - Adergrootte **4**, `discard_chance_on_air_exposure` **0,0** (vragen 1 en 2).
2. **Placed feature** `corruptedadventure:soul_crystal_ore` (`Registries.PLACED_FEATURE`):
   - `CountPlacement.of(2)` (vraag 1; geen `RarityFilter`)
   - `InSquarePlacement.spread()`
   - `HeightRangePlacement.uniform(VerticalAnchor.absolute(10), VerticalAnchor.absolute(117))` — **absolute** ankers: `belowTop(10)` is in de Nether Y 245 (hoogte 256), dus boven het bedrock-dak (vanilla verspilt zo de helft van zijn Nether-erts-pogingen). Y 10–117 dekt de lavazee-bodem, de wanden en alle crimson-vloeren.
   - `BiomeFilter.biome()` — plaatst alleen als de biome op de oorsprong de feature heeft; samen met stap 3 de garantie "alleen Nether-ring 1".
3. **Aan Nether-ring 1 hangen** (datagen):
   - Biome-tag `corruptedadventure:is_nether_ring_1` met de drie Nether-ring 1-kopieën (vraag 3). Herbruikbaar voor later (weer, sfeer, condities per Nether-ring). Bestaat de tag al uit change 14: hergebruiken.
   - Biome modifier `corruptedadventure:add_soul_crystal_ore` (`NeoForgeRegistries.Keys.BIOME_MODIFIERS`, `BiomeModifiers.AddFeaturesBiomeModifier`): biomes = de tag, features = de placed feature, stap `GenerationStep.Decoration.UNDERGROUND_DECORATION` (dezelfde stap als vanilla's Nether-ores).
   - Waarom een biome modifier en niet in de biome-bootstrap: werkt ongeacht hoe change 14 de kopieën bouwt (vanilla's `NetherBiomes`-methodes geven een kant-en-klaar `Biome` terug zonder haakje), en de ringores van ring 2–5 (changes 25, 41, 61, 81) kunnen hetzelfde patroon volgen. Behalve als change 12 al anders koos (zie aannames).
   - Feature-volgorde: de feature komt achteraan in de stap van alle drie de biomes, en nergens anders → geen "feature order cycle".
4. **`DataGenerators`**: `Registries.CONFIGURED_FEATURE`, `Registries.PLACED_FEATURE` en `NeoForgeRegistries.Keys.BIOME_MODIFIERS` in de `RegistrySetBuilder`; biome-tag in de tags-provider. `./gradlew runData`, output committen:
   - `data/corruptedadventure/worldgen/configured_feature/soul_crystal_ore.json`
   - `data/corruptedadventure/worldgen/placed_feature/soul_crystal_ore.json`
   - `data/corruptedadventure/neoforge/biome_modifier/add_soul_crystal_ore.json`
   - `data/corruptedadventure/tags/worldgen/biome/is_nether_ring_1.json`
5. **Documentatie:**
   - `CODEBASE.md`: feature-bootstrap(s), biome modifier en tag in "Overige registries"; generated bestanden; valkuil "Nether: `belowTop` = Y 245, gebruik absolute ankers"; eventueel het telscript (zie tests) bij de RCON-valkuil.
   - `fase-1-wereld.md` Ring 1 → Nether-tegenpool: plaatsing (doelblokken, Y-bereik, aantal, grootte, zichtbaarheid) + gemeten gemiddelde per chunk.
   - `02-beslissingenlog.md`: antwoorden op de open vragen.
   - `docs/backlog.md` (`infra`): debugcommando `/ca count <blok> <straal>` als herbruikbare teltool voor de ore-changes van ring 2–5 (niet in deze change bouwen).

**Determinisme:** vanilla-featureplaatsing gebruikt `WorldgenRandom` met het decoratie-zaad (wereldseed + chunk) plus feature-index en stap. Geen eigen random, dus deterministisch per seed. Let op: een extra feature verschuift de index van latere features in dezelfde stap; ook dat is deterministisch, maar bestaande testwerelden krijgen in nieuwe chunks een andere indeling dan een verse wereld.

---

## Client/server-impact

- Alleen worldgen op de server; de client krijgt de blokken via de gewone chunk-packets. Geen netwerk, geen client-code, geen nieuwe classes buiten datagen-bootstraps.
- Dedicated server: biome modifiers worden server-side toegepast bij het laden van de registries; niets client-only. `runServer` (S) staat niet in de rij, maar de W-test draait via de server (RCON), dus S gaat vanzelf mee.

---

## Tests

**B** — `./gradlew build` slaagt (incl. bestaande unit tests).

**D** — `./gradlew runData` slaagt; de vier JSON-bestanden hierboven bestaan en zijn gecommit. Snelle inhoudscheck: `absolute` 10/117, twee regeltests, stap `underground_decoration`, tag met precies de Nether-ring 1-biomes.

**W** — nieuwe wereld, seed `20260930`, preset `corrupted_world` (dedicated server met RCON, zie valkuilen in `CODEBASE.md`):

1. **Vindbaar (statistisch):** ad-hoc RCON-script (niet gecommit), per chunk: `forceload add`, dan per erts-blok (`soul_crystal_ore` en `crimson_soul_crystal_ore`, apart geteld)
   `execute in minecraft:the_nether run fill <x0> 0 <z0> <x0+15> 127 <z0+15> minecraft:air replace corruptedadventure:<blok>`
   (16×128×16 = 32.768, precies de fill-limiet; het aantal staat in de feedback, "No blocks were filled" = 0). Destructief, dus alleen in een wegwerpwereld.
   - Nether-ring 1: 8 richtingen × 3 stralen (80, 200, 320 Nether-blokken; ruim binnen 375 − 24 ruis − 16 halve zone) = 24 chunks. Verwacht: gemiddelde (beide blokken samen) > 0, in de orde van ~2–4 per chunk; op minstens de helft van de chunks ≥ 1 blok.
   - Crimson-variant: 4 extra chunks midden in een crimson-kopie (`/locate biome`) → `crimson_soul_crystal_ore` > 0 in minstens één ervan; buiten crimson-bossen (wastes/warped) = 0.
2. **Alleen Nether-ring 1:**
   - Nether-ring 2: 8 richtingen × 2 stralen (430, 480) = 16 chunks → **precies 0**.
   - Overworld ring 1: 8 chunks (netherrack komt daar niet voor, maar het bewijst dat de modifier niet op overworld-biomes zit) → **0**.
   - Grens: `/ca tpboundary 1 nether`, F3: erts stopt waar de biome wisselt (hooguit een aderstraal, ~2 blokken, over de grens).
   - Beide blokken tellen in Nether-ring 2 en overworld: allebei 0.
3. **Crimson-bodem:** `/locate biome <crimson-kopie>` in Nether-ring 1 → zichtbaar `crimson_soul_crystal_ore` in de crimson-vloer en `soul_crystal_ore` in de wanden. `/place feature corruptedadventure:soul_crystal_ore` op een crimson-vloer: één ader, netherrack-deel en nylium-deel elk met de juiste variant.
4. **Determinisme:** script op twee verse werelden met dezelfde seed (andere `level-name`) → identieke tellingen per chunk.
5. Portal-test uit ring 1 (standaard W) blijft werken; geen fouten in `latest.log` (feature order cycle, onbekende biome in tag).

**Checklist (concept)**
- [ ] Scope `worldgen` benoemd
- [ ] B, D groen; gegenereerde JSON gecommit
- [ ] W: gemiddelde per chunk in Nether-ring 1 gemeten en vastgelegd in `fase-1-wereld.md`
- [ ] W: 0 in Nether-ring 2 en overworld; grens klopt; zelfde tellingen bij twee verse werelden
- [ ] `crimson_soul_crystal_ore` zichtbaar in de crimson-vloer, `soul_crystal_ore` in netherrack
- [ ] `CODEBASE.md`, design-doc, beslissingenlog, backlog (`/ca count`) bijgewerkt
- [ ] `/code-review` gedaan, bevindingen afgehandeld
- [ ] Commit `worldgen: place soul crystal ore in nether ring 1`

---

## Risico's

- **Nether-hoogte:** `belowTop` rekent vanaf Y 255 → zonder absolute ankers valt de helft van de pogingen boven het dak. Opgelost met absolute 10–117.
- **Veel lucht en lava in de Nether:** een groot deel van de pogingen raakt geen netherrack → het werkelijke aantal ligt ver onder count × grootte. Daarom meten, niet schatten; tarief na de meting bijstellen binnen de gekozen optie.
- **Crimson nylium is één blok dik:** alleen aders die de vloerlaag raken geven `crimson_soul_crystal_ore`; het aandeel is dus klein. Dat is bedoeld (bonus in crimson-bossen, niet de hoofdbron) — de meting in W legt het aandeel vast.
- **Mechanisme-conflict** met change 12/14 (biome modifier vs. direct in de bootstrap): vóór de bouw even nagaan, één mechanisme houden.
- **Blokgedrag van de nylium-variant** (bijv. of hij als nylium telt voor zwambeenderen of terugvalt naar netherrack) hoort bij change 15 (`blocks`); deze change vervangt alleen blokken.
- Bestaande testwerelden: nieuwe chunks krijgen andere featureplaatsing (index-verschuiving). Altijd verse werelden voor W.

---

## Open ontwerpvragen

### 1. Hoe zeldzaam is zielkristal?
- **A — Iets vaker dan ancient debris (aanbevolen):** 2 aders per chunk, grootte 4 (1–4 blokken). Schatting ~2–4 blokken per chunk. Voelt als zoeken, maar een paar stuks vind je in 10–20 minuten; genoeg marge als fase 2 er meerdere per item vraagt.
- **B — Zo zeldzaam als ancient debris:** gemiddeld 1 ader per 2 chunks, grootte 3. Schatting ~0,5–1 blok per chunk. Echte schat, maar risico op frustratie en een lange grind als fase 2 veel stuks vraagt.
- **C — Gewoon, zoals een lichte goudader:** 6 aders per chunk, grootte 6. Schatting ~8–12 per chunk. Geen zoektocht, voelt niet als sleutelmateriaal.

Consequentie voor alle opties: puur data, later bij te stellen zonder code; in multiplayer delen spelers dezelfde (kleine) ring, dus zeldzamer = meer concurrentie.

Antwoord (2026-09-30): **A** — 2 aders per chunk, grootte 4.

### 2. Zichtbaar of verstopt (`discard_chance_on_air_exposure`)?
- **A — Zichtbaar, 0,0 (aanbevolen):** erts ook in wanden en in de crimson-bodem; past letterlijk bij "in de crimson-bodem". Vinden = rondkijken en verkennen.
- **B — Half verstopt, 0,5:** de helft van de blootgestelde blokken valt weg; crimson-bodem-erts wordt schaarser, meer graafwerk.
- **C — Verstopt, 1,0 (zoals ancient debris):** alleen ingesloten erts. De crimson-bodem valt dan feitelijk weg (nylium raakt altijd lucht), dus in strijd met het design tenzij we dat bijstellen. Mijnbouw-gameplay (TNT/bedden, zoals netherite).

Antwoord (2026-09-30): **A** — zichtbaar, `discard_chance_on_air_exposure` 0,0.

### 3. In welke biomes van Nether-ring 1?
- **A — Alle drie de kopieën, doelen netherrack + crimson nylium (aanbevolen):** letterlijk het design; overal in Nether-ring 1 te vinden, extra zichtbaar in crimson-bossen. Geen seed-risico.
- **B — Alle drie, ook warped nylium:** symmetrisch; ook zichtbaar op de warped-bodem. Wijkt licht af van het design ("crimson-bodem").
- **C — Alleen crimson forest (netherrack + nylium):** het crimson-bos wordt dé vindplek (sterk thema), maar het zoekgebied is kleiner en op sommige seeds is crimson forest binnen straal 375 klein of afwezig → "vindbaar" niet gegarandeerd.

Antwoord (2026-09-30): **A** — alle drie de kopieën, doelen netherrack → `soul_crystal_ore` en crimson nylium → `crimson_soul_crystal_ore` (variant uit change 15), in één ore-feature.
