# Change 30c — Ring 2: begroeiing en aankleding (duindalen, cactusgroepjes, fossielen en botten, oase, hoodoovelden, zout-eilandjes)

Scope: `worldgen` — change 30c (na 30b, vóór change 31; daarna teststop 2c volgens vraag 30b-8)

**Doel:** de vlakke en de hoodoo-biomes van ring 2 voelen **levendig**, terwijl de woestijn droog blijft. Het gaat om:
- **duinenzee:** begroeide duindalen (dorre struiken, cactusgroepjes, droog kort gras, af en toe een woestijnbloem) met kale kammen, en halfbegraven fossielen en botten;
- **oase:** veel meer bloemen, dichter gras en varens, struiken en bessen aan de rand;
- **hoodoovelden:** struiken, cactussen en flora tussen en aan de voet van de pilaren;
- **zoutvlakte:** leven op de eilandjes van 30b (struiken, cactus, botten).

Bron: `docs/plans/teststop-2b-feedback.md` (sparring 2026-10-03), plan `change-30b.md` (splitsing, eilandjes, mini-oases), `change-23c.md` (placement modifiers, `rubble`, oasezones op `ring_2_pools`, palmraster), `change-30.md` (duinvorm, flora en spawns).

Buiten scope:
- vorm-elementen en water (30b);
- dieren (fase 4);
- geluid en sfeer (32);
- textures (37);
- waterplanten en een bredere groene gordel rond de oase (niet gekozen in de sparring).

**Status:** gebouwd (2026-10-03), zie Bouwnotities; ontwerpvragen beantwoord (alle A, 2026-10-03). Volgende stap: **teststop 2c** (checklist onderaan), daarna change 31.

---

## Afhankelijkheden en aannames

- **30b is gebouwd** (bij de start nalopen in `CODEBASE.md` en de bouwnotities van 30b):
  - zout-eilandjes (grond zand/rood zand in `salt_flats`);
  - mini-oases (eigen planten en palmen);
  - de featurelijsten van `salt_flats`/`dune_sea` in `Ring2Biomes`;
  - `Ring2FlatsFeatures`.
  
  Wijkt een vorm af (bijv. 30b-6 B: vlakke eilandjes), dan blijft de flora gewoon op de zandgrond.
- **Duindal-hoogte (A1 van 30b):** dalbodems liggen op Y 66–71, onafhankelijk van de duinhoogte; kammen op Y 77–99. Een absolute `height_band` scheidt dal (≤ 71), flank (72–78) en kam (> 78).
- **Oasezones (23b/23c)** op `ring_2_pools`: grasoever ≥ 0,12 (water vanaf ~0,42), overgangsring 0,02–0,12, palmgroepjes 0,05–0,28 op het palmraster (cel 8, marge 2).
- **Planten en hun grond:**
  - `short_grass`, `fern`, `tall_grass`, bloemen en `sweet_berry_bush` hebben `#dirt` nodig (gras, `coarse_dirt`);
  - `azalea` groeit ook op zand (`#azalea_grows_on`);
  - `cactus` alleen op `#sand` (ook rood zand) en zonder vaste buren;
  - `dry_shrub`, `barrel_cactus` en `desert_flower` op `desert_plant_soil` (zand, rood zand, terracotta, `#dirt`, `cracked_clay`);
  - niets groeit op zout (change 29).
- **`simple_block`** controleert zelf `canSurvive` en zet ook dubbele planten (`tall_grass`, `large_fern`).
- **Vanilla-fossielen** (`FossilFeature`) begraven hun sjabloon 15–25 blokken diep en schrijven tot 16 blokken in buurchunks. Ze zijn dus onbruikbaar (niet halfbegraven, niet chunk-lokaal). De **sjablonen** zelf (`minecraft:fossil/spine_1…4`, `skull_1…4`) zijn bruikbaar via de `StructureTemplateManager`, zoals vanilla ze laadt.
- **Determinisme-regels:** als in 30b/23c. Alle placements `jittered_grid` → `WORLD_SURFACE_WG` → filters → `biome`; alles schrijft in de eigen chunk; groepjes komen uit geseede ruis (`noise_band`), niet uit `random_patch`.

---

## Plan

### 1. Duindalen begroeid, kammen kaal

- **Harde dalbodem (surface rule, `dune_sea`-tak, vloer, niet steil):** onder Y 70 (`not y_above 70`):
  - surface-ruis > 0,45 → `coarse_dirt`;
  - surface-ruis < −0,6 → `cracked_clay`;
  - 1 diep, daaronder zand.
  
  Geschat ~10–15 % van de dalbodem. Op `coarse_dirt` groeit vanilla-kort gras, en de **droge grastint** van `dune_sea` (`BFB755`) maakt het "droog gras" (vraag 30c-3 A).
- **Placed features** (`VEGETAL_DECORATION`, `dune_sea`; nieuwe datagen-klasse `Ring2DressingFeatures`):

  | Placed feature | Configured | Plaatsing (startwaarden) |
  |---|---|---|
  | `dune_sea_valley_flora` | nieuw `dune_valley_flora`: dorre struik 6 / dode struik 4 / tonnetjescactus 2 / woestijnbloem 1 | `jittered_grid` 4/0/0,25 → `height_band` ≤ 71 → lucht → `biome` (~4 per dalchunk) |
  | `dune_sea_valley_grass` | nieuw `dry_grass`: kort gras 4 / hoog gras 1 | `jittered_grid` 4/0/0,5 → grond `coarse_dirt` → `biome` |
  | `dune_sea_vegetation` (bestaand, nieuwe rol) | ongewijzigd (dorre/dode struik) | `jittered_grid` 8/0/0,2 → `height_band` 72–78 (flank); boven 78 niets: **kammen kaal** |
  | `dune_sea_cactus_groups` | nieuw `cactus_column`: `block_column` cactus 1–3 (`BiasedToBottomInt`) | nieuwe noise `ring_2_cactus_groups` (firstOctave −4, [1, 1]; vlekken ~15–30), `noise_band` ≥ 0,4 (~10 %) → `jittered_grid` 4/1/0,6 → `height_band` ≤ 74 → `would_survive(cactus)` → `biome` (groepjes van 2–5, ≥ 2 uit elkaar) |

  Marge 1 bij de cactus: alle vier buren liggen in de eigen chunk, dus `would_survive` leest nooit een buurchunk.

### 2. Fossielen en botten: feature `corruptedadventure:surface_fossil` (Java) + botblokken

- **Nieuw:** `SurfaceFossilFeature`, `SurfaceFossilConfiguration`.
- **Config:**
  - `templates` (lijst van sjabloon-ids; vanilla `spine_1…4`, `skull_1…4`);
  - `buried` (FloatProvider: deel van de sjabloonhoogte onder de grond, **0,4–0,7**);
  - `integrity` (0,85: verweerd, vanilla's `BlockRotProcessor`);
  - `ground` (`HolderSet<Block>`).
- **Werking:**
  1. Kies sjabloon en rotatie met de placement-random.
  2. Schuif de bounding box naar binnen tot ze ≥ 1 van de chunkrand ligt. Past ze in geen rotatie (> 14), probeer dan een ander sjabloon, anders niets.
  3. Basis-Y = laagste `WORLD_SURFACE_WG`-hoogte in de voetafdruk (eigen chunk) − `buried` × hoogte. Alle voetafdrukkolommen moeten `ground` zijn.
  4. `placeInWorld` met bounding box = eigen chunk, processors `BlockIgnoreProcessor.STRUCTURE_AND_AIR` + `BlockRotProcessor(integrity)`, random uit de placement-context. Geen kolen-/diamant-overlay.
  5. Alleen botblokken worden geschreven. Ze vervangen zand of lucht; boven de grond staan ze op het begraven deel.
- **Bouwstap:**
  - nagaan dat de vanilla-sjablonen alleen botblokken bevatten (anders filtert de ignore-processor);
  - de maten per rotatie vastleggen;
  - nagaan dat `getStructureManager()` vanuit de worldgen-thread mag (vanilla doet het in `FossilFeature`).
- **Unit test `SurfaceFossilFeatureTest`** (pure plaatsingskern):
  - bounding box in de chunk;
  - het begraven deel klopt met `buried`;
  - geen plaatsing als een kolom geen `ground` is.
- **Botblokken:** configured `bone_rubble` = `rubble` (bestaand type) met `bone_block` `axis` x/y/z (gewogen), grootte 1–2: verspreide botten, half ingegraven.
- **Plaatsing** (`LOCAL_MODIFICATIONS`, achter de lijsten van 30b):

  | Placed feature | Plaatsing |
  |---|---|
  | `dune_sea_fossils` | `jittered_grid` 16/1/**0,04** (~1 per 25 duinchunks) → `height_band` ≤ 76 (dal en flank) → grond zand → `biome` |
  | `dune_sea_bones` | `jittered_grid` 16/1/0,2 → `height_band` ≤ 74 → grond zand → `biome` |
  | `salt_island_fossils` | `jittered_grid` 16/1/0,1 → grond zand/rood zand → `biome` (alleen op eilandjes en de zandvlekken) |
  | `salt_island_bones` | `jittered_grid` 8/1/0,15 → grond zand/rood zand → `biome` |

### 3. Oase: meer bloemen, dichter gras en varens, struiken en bessen

- **`oasis_vegetation`** (bestaand):
  - plaatsing `count` 10 + `in_square` → `count` 2 + `jittered_grid` 4/0/1 (32 kandidaten, gespreid);
  - gewichten kort gras 6 / varen 3 / hoog gras 1 / grote varen 1 (de woestijnbloem gaat naar de bloemen).
- **Bloemen in plekken per kleur:** nieuwe noise `ring_2_flower_patches` (firstOctave −4); drie placed features op de grasoever (`noise_band` `ring_2_pools` ≥ 0,12), elk `jittered_grid` 4/0/0,35 + een band op `ring_2_flower_patches`:
  - `oasis_flowers_warm` (< −0,2): klaproos 3 / paardenbloem 2 / woestijnbloem 2;
  - `oasis_flowers_white` (−0,2…0,2): margriet 3 / korenbloem 2 / woestijnbloem 1;
  - `oasis_flowers_purple` (> 0,2): allium 3 / korenbloem 2.
  
  Samen ~5–6 bloemen per oeverchunk, in kleurvlekken. Een geseede wereldruis, dus de vlekken liggen elke wereld anders (vanilla's `noise_provider` heeft een vaste seed).
- **`oasis_transition_vegetation`:** 4 → 8 pogingen.
- **`oasis_edge_shrubs`** (nieuw): configured azalea 3 / bloeiende azalea 1 / zoete bessenstruik (`age=3`) 2:
  - band `ring_2_pools` 0,05–0,16 (buitenrand oever + binnenste overgangsring);
  - `jittered_grid` 8/1/0,35 → lucht → `biome`;
  - staat **ná** de palmen in de lijst: een palm die al staat, blokkeert de plek; omgekeerd nooit.
- **Volgorde `OASIS_VEGETATION`:** shore_cane, shore_plants, palm_groves, palms_sand, palm_bushes, `oasis_vegetation`, de drie bloemenfeatures, `oasis_edge_shrubs`, `oasis_transition_vegetation`.

### 4. Hoodoovelden: flora tussen de pilaren

- **`hoodoo_fields_vegetation`** (bestaand): `jittered_grid` 8/1/0,375 → **4/0/0,3** (~4,8 per chunk).
- **`hoodoo_pillar_foot_flora`** (nieuw): configured `pillar_foot_flora` (dorre struik 4 / dode struik 2 / tonnetjescactus 2 / woestijnbloem 1):
  - `jittered_grid` 4/0/0,5 → `slope` rise ≥ 8 r 2 (pilaarvoet, schaduwkant) → lucht → `biome`.
- **`hoodoo_cactus_groups`** (nieuw): `cactus_column` op dezelfde ruis als de duinen (`ring_2_cactus_groups` ≥ 0,45) → `jittered_grid` 4/1/0,5 → `would_survive(cactus)` → `biome`. Rood zand; naast een pilaar valt de cactus af.

### 5. Zout-eilandjes: leven

- **`salt_island_flora`** (nieuw): configured dorre struik 5 / dode struik 3 / tonnetjescactus 2:
  - `jittered_grid` 4/0/0,35 → grond zand/rood zand → lucht → `biome`.
- **`salt_island_cactus`:** `cactus_column` (1–2), `jittered_grid` 8/1/0,2 → `would_survive` → `biome`.
- De zandvlekken aan de duinkant krijgen dezelfde flora: de rand naar de duinen leeft mee. Op de korst blijft het kaal.

### 6. Biomes, volgorde, datagen en documentatie

- **`Ring2Biomes`:** nieuwe features achteraan de lijsten van 30b/23c, in de volgorde van stap 1–5.
  - Elke placed feature hoort bij één biome. Configured features worden gedeeld (`cactus_column`, `bone_rubble`), dus er komen geen volgorde-eisen tussen biomes.
  - `Ring2BiomesTest` uitbreiden.
- **Datagen-klasse:** de nieuwe bootstraps voor duinen, hoodoos en zout komen in `Ring2DressingFeatures`. De wijzigingen aan de oase staan in `Ring2Features`, bij de rest van de oase.
- **Registries:** `ModFeatures.SURFACE_FOSSIL`; keys in `ModConfiguredFeatures`/`ModPlacedFeatures`.
- **`CODEBASE.md`:**
  - classes en registries, features-tabel;
  - noises `ring_2_cactus_groups`, `ring_2_flower_patches`;
  - de surface-regel voor de dalbodem;
  - valkuilen: vanilla `FossilFeature` is niet chunk-lokaal; planten en hun grond (kort gras niet op zand);
  - metingen en de kopregel.
- **`fase-1-wereld.md` §2 Ring 2:**
  - "Uitwerking begroeiing en aankleding (change 30c)";
  - flora en spawns van change 30 bijwerken (de duinen zijn niet meer "vrijwel kaal").
- **`02-beslissingenlog.md`** en **`docs/backlog.md`:** de regels hieronder.
- **Checklist teststop 2c** (bij vraag 30b-8 A): onderaan dit plan; de W-lijst van 30b overnemen.

---

## Gevolgen voor latere plannen (niet nu aanpassen)

| Plan | Wat verandert |
|---|---|
| **31 `dry_riverbeds`** | "Gestrande botten" = `surface_fossil` + `bone_rubble` (zelfde configs, eigen placed features). De dalbodem-regel (`coarse_dirt`/`cracked_clay`) is een voorbeeld voor de beddingbodem. In de zone 2→3 vallen de features van 30c weg (biomefilter) |
| **32 sfeer** | Leven komt ook via geluid: ambient voor de oase (insecten, vogels bij water) en de mini-oases; het zingende zand blijft voor de duinen. Bijen/vlinders zijn particles of mobs: `atmosphere` resp. fase 4 |
| **37 textures** | Meer vanilla-planten naast eigen flora in beeld: woestijnbloem naast klaproos/allium, dorre struik naast dode struik, tonnetjescactus naast vanilla-cactus. Beoordelen of de eigen planten genoeg afsteken. `bone_block` en fossielen op zand |
| **Fase 4** | Dieren in de oase en de duindalen (backlog): de begroeiing geeft ze een plek |

---

## Client/server-impact

- **Puur worldgen**, server-side en deterministisch per seed: chunk-lokale features, geseede ruis, de placement-random. Fossielsjablonen komen uit de vanilla-data via de server, zoals bij vanilla's `FossilFeature`.
- **Nieuw common registry-type:** feature `surface_fossil`. Geen netwerk, geen SavedData, geen client-code; een dedicated server laadt geen client-class.
- **Bestaande werelden:** alleen nieuwe chunks krijgen de begroeiing. De dalbodem-regel geeft naden in oude duinchunks.

---

## Tests

Bouwplan: **B, D, S, W** (W in teststop 2c). Testseed `20260930`, werelden `smoke-change30c-…`.

### B — `./gradlew build`
- [ ] Nieuw: `SurfaceFossilFeatureTest`.
- [ ] Uitgebreid: `Ring2BiomesTest` (lijsten en volgorde).
- [ ] Bestaande tests groen.

### D — `./gradlew runData`
- [ ] Configured/placed features van stap 1–5, `dune_sea.json`, `oasis_flats.json`, `hoodoo_fields.json`, `salt_flats.json` met de nieuwe lijsten; geen andere biomes gewijzigd.

### S — server + RCON
- [ ] **Rooktest:** `Done`, geen "Feature order cycle", geen "Detected setBlock in a far chunk", geen fout bij het laden van de fossielsjablonen.
- [ ] **Tellingen per biome** (planten per chunk; doelen = startwaarden):
  - **duinen** (100 chunks): dalen ~4–6 planten, flanken ~0,5–1, kammen (> Y 78) 0; cactusgroepjes in ~10 % van de dalen; fossielen ~1 per 25, botten ~1 per 5; `coarse_dirt`/`cracked_clay` ~10–15 % van de dalbodem;
  - **oase** (64 chunks): planten op de oever ×2–3 t.o.v. change 30; bloemen ~5–6 per oeverchunk in kleurvlekken; struiken/bessen alleen in de band;
  - **hoodoos** (64 chunks): ~6–8 planten per chunk, waarvan een deel aan pilaarvoeten;
  - **zout** (50 chunks): flora alleen op zand/rood zand, 0 op de korst.
- [ ] **Fossielen** (alle exemplaren in 200 duinchunks): volledig in hun chunk, 40–70 % begraven, geen botblok dat zweeft (blok met lucht eronder én geen botblok/grond eronder).
- [ ] **Determinisme:** twee verse werelden, de tweede omgekeerd, 60 s uitzakken; 3 × 3-gebieden in duinen (met fossiel en cactusgroep), oase, hoodoos, zout-eiland → blok voor blok gelijk (bekende uitzonderingen: vanilla's ondergrondse klodders, `distance` van bladeren).
- [ ] **Ring 1:** biomes en steen/deepslate ongewijzigd (feature-index: bekend).
- [ ] **Prestaties** (JFR, meetplekken van change 30, vóór = worktree van 30b): features-stap ≤ +2 ms per chunk op oase, duinen en hoodoos; hele chunk ≤ +5 %; noise-stap ongewijzigd.

### W — teststop 2c (blokkerend bij 30b-8 A/B)

Checklist onderaan (in te vullen door de bouwer, met coördinaten op seed 20260930); de W-punten van 30b overnemen.
- [ ] **Levendig?** Per biome 2–3 minuten rondlopen (survival, middag):
  - duinen: groen-grijze dalen, kale kammen, cactusgroepjes, een fossiel;
  - oase: bloemenvlekken, dicht gras, struiken en bessen aan de rand;
  - hoodoos: flora aan de pilaarvoeten;
  - zout: eilandjes met leven.
  
  Voelt het levendig en nog steeds als woestijn?
- [ ] **Niet te druk:** kammen blijven leeg (verdwalen), het zout blijft wit en dood buiten de eilandjes.
- [ ] **Bessen:** vertragen en schaden (vanilla); storend aan de rand?

---

## Risico's / aandachtspunten

- **Te groen voor een woestijn:** het droge gras op `coarse_dirt` en de bloemen kunnen de duinen/oase vanilla laten ogen. Startwaarden laag; afstemmen in 2c.
- **Zoete bessen** geven voedsel en schade. Ze maken overleven in ring 2 iets makkelijker (backlog `[conditions]`/balans, alleen noteren).
- **Fossielsjablonen** kunnen per rotatie groter zijn dan 14. Dan vallen ze af (meten in de bouwstap; bij te weinig plaatsingen: alleen de schedels, of `buried` hoger).
- **Volgorde in de oase:** de struiken staan ná de palmen. Een struik die eerder kwam, zou een palm (3 × 3-schrijver) laten afhaken, en dan hangt het resultaat van de volgorde af.
- **Cactus naast een rots of pilaar** valt af via `would_survive`: dat is gewenst. Marge 1 houdt de buren in de chunk.
- **Feature-index ring 1:** verschuift (bekend).

---

## Backlog-kandidaten

- `[mobs] Fase 4: dieren in oase en duindalen (konijnen, fennecs/woestijnvos?, hagedissen) nu de begroeiing er is. (bron: change 30c)`
- `[conditions] Zoete bessen in de oase (change 30c): extra voedselbron in ring 2; balans van uitdroging/honger nalopen. (bron: change 30c)`

---

## Ontwerpvragen

Eén ronde.

### 30c-1. [Belangrijk] Hoe dicht wordt de begroeiing?
- **A. Matig: duindalen ~4–6 planten per chunk (kammen kaal), oase-oever ×2–3 + ~5–6 bloemen, hoodoos ~6–8, zout alleen op de eilandjes (aanbevolen).** Duidelijk levendiger, nog steeds een woestijn.
- **B. Weelderig (×2).** Meer leven, maar de duinen en hoodoos ogen dan als savanne.
- **C. Spaarzaam (×0,5).** Kleine stap; het risico dat het opnieuw kaal voelt.

Antwoord: **A** (gebruiker, 2026-10-03) — matig: duindalen ~4–6 planten per chunk (kammen kaal), oase-oever ×2–3 + ~5–6 bloemen, hoodoos ~6–8, zout alleen op de eilandjes.

### 30c-2. (routine) Welke bloemen in de oase?
- **A. Woestijnbloem + klaproos, paardenbloem, margriet, korenbloem, allium, in kleurvlekken (aanbevolen).** Warm palet; de eigen bloem blijft herkenbaar.
- **B. Als A + tulpen en roze bloemblaadjes.** Kleurrijker, maar meer "vanilla-weide".
- **C. Alleen woestijnbloem en klaproos.** Sober.

Antwoord: **A** (gebruiker, 2026-10-03) — woestijnbloem + klaproos, paardenbloem, margriet, korenbloem, allium, in kleurvlekken.

### 30c-3. (routine) Droog gras in de duindalen
- **A. Vlekken grove aarde en gebarsten klei in de dalbodem; kort gras met de droge grastint van de duinen (aanbevolen).**
- **B. Geen gras: alleen struiken, cactussen en bloemen op zand.** Geen bodemwijziging; minder "begroeid".

Antwoord: **A** (gebruiker, 2026-10-03) — vlekken grove aarde en gebarsten klei in de dalbodem; kort gras met de droge duingrastint.

### 30c-4. (routine) Struiken en bessen aan de oaserand
- **A. Azalea (soms bloeiend) + rijpe zoete bessenstruiken (aanbevolen).**
- **B. Bladstruikjes (2–4 vaste eikenbladeren, met de groene oasetint) + bessen.** Meer "struik", wel een eigen kleine feature.
- **C. Alleen bessen.**

Antwoord: **A** (gebruiker, 2026-10-03) — azalea (soms bloeiend) + rijpe zoete bessenstruiken.

### 30c-5. [Belangrijk] Fossielen en botten
- **A. Vanilla-fossielsjablonen halfbegraven (~1 per 25 duinchunks, ook op zout-eilandjes) + losse botblokken (~1 per 5 duinchunks) (aanbevolen).** Herkenbaar, deterministisch (eigen chunk-lokale feature).
- **B. Alleen losse botblokken (`rubble`), geen sjablonen.** Geen Java, minder herkenbaar.
- **C. Vaker (fossiel ~1 per 10).** Meer verhaal ("gestolen water"); minder bijzonder.

Antwoord: **A** (gebruiker, 2026-10-03) — vanilla-fossielsjablonen halfbegraven (~1 per 25 duinchunks, ook op zout-eilandjes) + losse botblokken (~1 per 5 duinchunks).

### 30c-6. (routine) Cactusgroepjes
- **A. Groepjes van 2–5, 1–3 hoog, in ~10 % van de duindalen en verspreid in de hoodoos (aanbevolen).**
- **B. Losse cactussen, geen groepjes.**
- **C. Hoger (1–4) en dichter (~20 %).** Meer herkenningspunten in de dalen.

Antwoord: **A** (gebruiker, 2026-10-03) — groepjes van 2–5, 1–3 hoog, in ~10 % van de duindalen en verspreid in de hoodoos.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-03 | worldgen | Begroeiing ring 2 (30c-1 A): matig — duindalen ~4,4–5,3 planten per 256 dalkolommen (kammen > Y 78 kaal), oase-oever ×2,8, hoodoos ~6,7 per chunk, zout alleen op de eilandjes | Teststop 2b: alles moet levendig aanvoelen |
| 2026-10-03 | worldgen | Oase (30c-2 A, 30c-4 A): bloemen in kleurvlekken op een eigen geseede ruis (warm/wit/paars, ~5 per 256 graskolommen), dichter gras en varens, azalea (soms bloeiend) en rijpe zoete bessen in de band rond de oever ná de palmen | Teststop 2b: oase kaal; geen waterplanten of bredere gordel (sparring) |
| 2026-10-03 | worldgen | Droog gras in de duindalen (30c-3 A): harde dalbodem (grove aarde/gebarsten klei, ~12 % onder Y 70) met kort gras in de droge duintint | Kort gras groeit niet op zand |
| 2026-10-03 | worldgen | Fossielen en botten (30c-5 A): eigen feature `surface_fossil` op vanilla-sjablonen, 0,4–0,7 van de hoogte begraven, verweerd, chunk-lokaal (~1 per 26 duinchunks, ook op de zout-eilandjes) + losse botblokken (~1 per 5 duinchunks) | Vanilla `FossilFeature` begraaft diep en schrijft in buurchunks |
| 2026-10-03 | worldgen | Cactusgroepjes (30c-6 A) en flora aan de pilaarvoeten in de hoodoovelden: groepjes van 2–5 (1–3 hoog) op ~9 % van het land via `ring_2_cactus_groups` (firstOctave −5), soms een groter veld in een breed dal | Leven tussen de pilaren en in de dalen |

Commit (na `/code-review`): `worldgen: add ring 2 vegetation, cactus groups, fossils and oasis flowers`.

---

## Bouwnotities (change 30c, 2026-10-03)

Aannames gecontroleerd tegen de code na 30b (`f347c11`): eilandjes (zand/rood zand in `salt_flats`), mini-oases (`dune_oasis`, grond zand), de lijsten van `Ring2Biomes`, `Ring2FlatsFeatures`, de placement modifiers `jittered_grid`/`noise_band`/`slope`/`height_band` en `rubble` klopten. Twee aannames klopten niet: **azalea groeit in 1.21.1 niet op zand** (`AzaleaBlock.mayPlaceOn` = klei of de `BushBlock`-grond `#dirt`; `#azalea_grows_on` geldt alleen voor de wortels van de azaleaboom) en **de dalen zijn maar een klein deel van de duinen**: bij hoge duinen is ~63 % van de kolommen kam (> Y 78) en ~15–20 % dal (≤ Y 71), zodat filters op dal/flank de meeste pogingen weggooien (zie afwijking 4). Tijdelijke hulpmiddelen zijn niet gecommit: het dev-commando `/cadev` (`dev/DevMapCommand`, uitgebreid met een Y-bereik voor `dump` en `nmap` = ruiswaarden op een raster), scripts in `build/c30c/`, een tijdelijke logregel per geplaatst fossiel en de git-worktree `../ca-head` (`f347c11`) voor de vóór-metingen.

**Afwijkingen van het plan (implementatie en afstemming, binnen de antwoorden):**
1. **Hoogtes op het bovenste blok:** dal ≤ Y 71, flank 72–78, kam > 78 gelden voor het bovenste blok (de herkomst ligt er één boven, zoals `VALLEY_FLOOR` van 30b); cactus ≤ 74, botten ≤ 74, fossielen ≤ 76.
2. **Harde dalbodem: drempels 0,4 / −0,5** (plan 0,45 / −0,6). Gemeten met 0,45/−0,6: ~7,5 % van de dalbodem ≤ Y 69; met 0,4/−0,5 **~12 %** (per gebied 5–20 %: de surface-ruis heeft vlekken van ~64 blokken).
3. **Cactusruis firstOctave −5** (plan −4) en **kans 0,85** (plan 0,6); `jittered_grid` vóór `noise_band` (zoals 30b-afwijking 4). Met −4 waren de vlekken boven 0,4 maar ~7 blokken (mediaan 35 kolommen): bijna alleen losse cactussen. Met −5 ~10–25 breed (de "vlekken ~15–30" van het plan), aandeel ~9 %. Meestal groepjes van 2–5; in een breed dal soms een veld (gemeten één van 29 cactussen bij 2.789 −98; afstemvraag).
4. **Fossielen vaker geprobeerd:** duin raster 16 kans **0,2** (plan 0,04): de hoogtefilter (≤ 76) en de eis "hele voetafdruk zand" laten ~1 op 5 pogingen over; 0,04 gaf ~1 fossiel per 146 duinchunks, 0,2 geeft **~1 per 26**. Losse botten raster **8** kans **0,3** (plan 16 / 0,2: ~1 per 18 → nu ~1 per 5). Zout-eilandjes: fossiel **elke chunk één poging** (plan 0,1: eilandjes zijn ~5 % van de vlakte, 0,1 gaf er vrijwel nooit een; 0,5 gaf 0 in de meetvlakte; 1,0 → 6 per 256 zoutchunks).
5. **`surface_fossil` zonder `placeInWorld`:** de feature neemt de botblokken van het sjabloon (`filterBlocks(…, BONE_BLOCK)` met de rotatie), en de pure kern `FossilLayout` doet de rest: voetafdruk gecentreerd op de herkomst en naar [1, 14] van de chunk, begraven in **hele lagen** (`round(buried × hoogte)`, minstens 1, nooit alles), verwering met de placement-random (zelfde kans als vanilla's `BlockRotProcessor`) en daarna alleen botten die via botten (26-buren: de ribben van vanilla's fossielen raken elkaar alleen diagonaal) met een bot in of op de grond verbonden zijn. Zo kan verwering geen losse brokken laten zweven, en is alles testbaar. De vanilla-sjablonen bestaan alleen uit botblokken (gecontroleerd) en zijn in elke rotatie ≤ 13 breed: ze passen altijd. `getStructureManager()` vanuit de worldgen-thread zoals vanilla's `FossilFeature` (de sjablooncache is een concurrent map).
6. **Configured-namen:** `desert_fossil` (de fossiel-config, gedeeld door duin en zout) en `short_cactus_column` (1–2, zout-eilandjes) naast `cactus_column` (1–3).
7. **Oase afgestemd:** `oasis_vegetation` `count` 2 × raster 4 kans **0,8** (plan 1,0 gaf ×3,4 i.p.v. ×2–3 → nu **×2,8**); bloemen kans **0,42** (plan 0,35 gaf 4,5 per 256 graskolommen → nu **5,4**); struiken raster **4** marge 1 kans **0,25** (plan 8/1/0,35 gaf 26 per 256 oasechunks, de helft viel af op zand → nu 72).
8. **Hoodoos afgestemd:** flora raster 4 kans **0,4** (plan 0,3) en pilaarvoet kans **0,6** (plan 0,5): met de planwaarden 5,0 planten per chunk, nu **6,7** (doel 6–8).
9. **Bloembanden** sluiten op elkaar aan (warm ≤ −0,2, wit −0,2…0,2, paars ≥ 0,2): elke kleur heeft eigen kandidaten, dus een gedeelde grenswaarde kan geen plek dubbel geven.
10. **Unit test met de echte sjablonen:** vanilla-data staat niet op het test-classpath; `SurfaceFossilFeatureTest` leest de sjablonen uit `build/moddev/artifacts/*client-extra*.jar` (zie valkuilen).
11. **Feature-index in ring 2:** de nieuwe placed features verschuiven ook de decoratie-seeds van de zoutfeatures van 30b: pekelpoelen en zoutpilaren liggen op seed 20260930 op andere plekken dan in de 30b-checklist (duinrotsen, kragen en mini-oases niet). De checklist hieronder heeft nieuwe zoutcoördinaten.

### Testresultaten

- **B:** `./gradlew build` groen; `./gradlew test --rerun`: 67 testklassen, 325 tests, 0 fouten. Nieuw: `SurfaceFossilFeatureTest` (6 tests); uitgebreid: `Ring2BiomesTest` (alle nieuwe lijsten en volgordes, bloemen/struiken ná de palmen, elke eigen placed feature in één biome behalve de gedeelde rotsval).
- **D:** `./gradlew runData`: configured `dune_valley_flora`, `dry_grass`, `cactus_column`, `short_cactus_column`, `pillar_foot_flora`, `salt_island_flora`, `desert_fossil`, `bone_rubble`, `oasis_flowers_warm`/`_white`/`_purple`, `oasis_edge_shrubs`, gewijzigd `oasis_vegetation`; 15 nieuwe placed features, gewijzigd `oasis_vegetation`, `oasis_transition_vegetation`, `hoodoo_fields_vegetation`, `dune_sea_vegetation`; `dune_sea.json`, `oasis_flats.json`, `hoodoo_fields.json`, `salt_flats.json`. Geen andere biomes gewijzigd. Handgeschreven: `noise/ring_2_cactus_groups.json`, `noise/ring_2_flower_patches.json`, de dalbodemregel in `noise_settings/overworld.json`.
- **S — rooktest** (`tools/server-smoke-test.sh`, verse `level-name=smoke-change30c`, preset `corrupted_world`, seed 20260930, zonder dev-commando): `Done (6.985s)`, geen "Feature order cycle", geen codecfout, geen "Detected setBlock in a far chunk", geen client-class; alleen de bekende ocelot- en offline-waarschuwingen.
- **S — metingen** (RCON tijdelijk aan via het testharnas, wegwerpwerelden `smoke-change30c-m1…m4`, `-detA3/-detB3`, `-r1new`; vóór = `../ca-head` (`f347c11`), wereld `smoke-c30c-head`; 7 gebieden van 16 × 16 chunks: zout (−176, −104), hoge duinen dA (−144, 32), dB (160, −16), dC (−160, −16), lage duinen (−160, 32), oase (−168, 72), hoodoos (144, 32); eindwaarden = `m4` (zout: `m3`, zelfde zoutcode)):
  - **Duinen:** planten per 256 dalkolommen (bovenste blok ≤ 71) zonder de mini-oases **4,4 / 5,3 / 4,4**, lage duinen **4,7** (vóór 0,24); flank (72–78) **0,6–1,2**; kam (> 78) **0** (vóór 0,21). Harde dalbodem ≤ Y 69: 20 / 5 / 5 / 16 % (gemiddeld ~12 %), daarop kort en hoog gras (4–33 per gebied). Cactussen 34–43 per 256 chunks, 1–3 hoog (60 % 1 hoog), kleinste afstand 3,0, groepjes {1: 26, 2: 9, 3: 6, 4: 6, 5: 6, 9: 1, 29: 1} over vier gebieden. Fossielen (clusters ≥ 20 botblokken) **11 / 11 / 8 / 10 in 1.024 duinchunks (~1 per 26)**, allemaal in hun chunk en ≥ 1 van de rand, 0 brokken die geen grond raken; begraven (tijdelijke logregel, 86 fossielen): 1 van 3 tot 3 van 4 lagen (verdeling 0,33 / 0,4 / 0,5 / 0,6 / 0,67 / 0,75 = 5 / 7 / 37 / 22 / 7 / 8), laagste laag precies `laagste grond + 1 − lagen` in 84 van 86 (2: het laagste bot was weggewerd), ~78 % van de botten blijft staan. Losse botten **201 in 1.024 duinchunks (~1 per 5)**, 0 zwevend.
  - **Oase** (256 chunks, 23.138 graskolommen): planten op gras **31,4 per 256 graskolommen (vóór 11,1: ×2,8)**: kort gras 1.230, varen 592, hoog gras 246, grote varen 196; bloemen **5,4 per 256 graskolommen** (korenbloem 132, margriet 104, woestijnbloem 84, allium 79, klaproos 66, paardenbloem 43; vóór alleen woestijnbloem 1,4); twee bloemen binnen 6 blokken hebben in **71 %** dezelfde kleur (willekeurig 34 %): kleurvlekken. Struiken **72** (azalea 32, bloeiende azalea 14, bes 26), op grove aarde 45 en gras 27, 0 op zand.
  - **Hoodoos:** **6,7 planten per chunk** (vóór 1,3), waarvan 27 % aan een pilaarvoet (≥ 8 hoger binnen 2); 118 cactussen, alle op rood zand.
  - **Zout:** flora alleen op zand/rood zand (**0 op de korst**), ~6 planten per 256 eilandkolommen; 6 fossielen en 6–9 losse botten op de eilandjes.
  - **Geen zwevende blokken** (flood fill vanaf Y 60, 26-buren): 0 van eigen features in alle gebieden; bekend en niet van 30c: 5 zandsteen boven een grotkrater (`-2360 80 -244`, ook vóór), 1 zandsteen in de lage duinen (ook vóór), kaprots op de rand van het gedumpte hoodoogebied (ook vóór). Geen "Detected setBlock in a far chunk" in de serverlogs.
  - **Determinisme:** twee verse werelden, de tweede omgekeerd en chunk voor chunk geladen, 60 s uitzakken, 3 × 3-gebieden (2 duin met fossiel en cactusgroep, oase met bloemen en struiken, hoodoos, zout-eiland met fossiel): **0 andere verschillen** dan vanilla's ondergrondse klodders/ertsen (d1 354, d2 178, o1 47, s1 183 blokken; hoodoos 9/9 chunks identiek).
  - **Ring 1:** biomes op **1.681/1.681** punten gelijk aan `f347c11`; steen/deepslate in 4 van 5 gebieden op ≤ 179 blokken gelijk, in het spawngebied +632 deepslate/−179 steen (verschoven feature-index, decoratie; bekende valkuil).
  - **Prestaties** (JFR `minecraft.ChunkGeneration`, meetplekken van change 30, tweede ronde, vóór = `../ca-head`): 5 rondes per variant afwisselend (de eerste 3 gaven op de duinen +5,1 % door één trage ronde waarin alle stappen trager waren, daarom 2 extra). **Features-stap** (gemiddeld, ms) hoodoos 2,33 → 2,40, x 2.500 (duinen) 2,18 → 2,25, kloven 2,52 → 2,46, hoge duinen 2,08 → 2,15, oase 2,28 → 2,33, zout 2,19 → 2,22: **≤ +0,07 ms** (budget +2 ms). **Hele chunk** hoodoos −1,0 %, x 2.500 −1,2 %, kloven +1,9 %, duinen **+3,9 %** (mediaan +2,9 %; komt uit de noise-stap, die 30c niet raakt en daar 29,6–38,9 ms spreidt), oase −0,4 %, zout +1,2 % (budget +5 %). Noise- en surface-stap binnen de ruis (±3,5 %) van vóór.
- `run/server.properties` staat op `level-name=smoke-change30c`, RCON uit; geen server of client actief. In `../ca-head/run/server.properties` staat RCON ook uit; de worktree is na afloop verwijderd.

### Code review (`/code-review medium`)

Geen correctheidsbugs gevonden (rotatie van de botblokken via `filterBlocks`, chunkgrenzen en klem van `FossilLayout`, begraven lagen en verbondenheid, hoogtebanden dal/flank sluitend, cactusafstand en chunk-lokale `would_survive`, featurevolgorde, bloembanden en overleving van azalea/bes nagelopen). Eén laag punt (afstemming, geen defect): de harde dalbodem ligt alleen onder Y 70 (planregel "onder Y 70"), terwijl de dalflora tot Y 71 reikt; op dalbodems van Y 70–71 komt dus geen droog gras. **Bewust zo gelaten** (de 12 % is op deze grens gemeten; het droog gras hoort bij de laagste dalbodem); het commentaar in `Ring2DressingFeatures` zegt het nu expliciet. Zelf nog nagelopen: de palette-cache van `StructureTemplate` is in NeoForge 21.1 concurrent (MC-271899), dus `filterBlocks` vanaf worldgen-threads is veilig (commentaar in `SurfaceFossilFeature`). Build, tests en `runData` daarna opnieuw groen (geen gegenereerde wijzigingen).

## In-game checklist (W) — teststop 2c

Blokkerend vóór change 31. Tijd ~25–30 min. Kern van de test: **voelen oase, duinenzee, zoutvlakte en hoodoovelden nu levendig — en nog steeds als woestijn?** Coördinaten op seed `20260930`, gecontroleerd tegen de build van 30c (meetwerelden met dezelfde code); `x y z`, y = net boven de grond.

**Opstarten (~2 min)**
1. `./gradlew runClient` → Singleplayer → **Create New World**. Gebruik een **nieuwe** wereld: oude werelden laden mogelijk niet, en alleen nieuwe chunks krijgen de begroeiing (pekelpoelen en zoutpilaren liggen sinds 30c ook op andere plekken).
2. Tab *World*: World Type **Corrupted World**, Seed **20260930**. Tab *Game*: Game Mode **Creative**, **Allow Cheats: ON**. → Create.
3. In de wereld: `/time set noon`, `/gamerule doDaylightCycle false`, `/ca weather 2 clear 3600` (geen zandstorm of hittegolf tijdens de test). Render distance ≥ 12. Vliegen in creatief; `/gamemode survival` alleen waar het gevraagd wordt (daarna terug naar creatief).

**A. Oase (~6 min)** — gebied x −2.690…−2.430, z 1.150…1.410
- [ ] **Bloemen in kleurvlekken:** `/tp @s -2574 75 1366` en rondkijken: paarse en witte vlek (allium, korenbloem, margriet); warme bloemen (klaproos, paardenbloem, woestijnbloem) bij `/tp @s -2450 72 1257`. Leest het als vlekken per kleur, niet als confetti?
- [ ] **Dicht gras en varens:** `/tp @s -2664 66 1211`: kort gras, varens, hoog gras en grote varens op de oever; overgang groen → overgangsring (dorre struiken, grove aarde) → zand nog herkenbaar.
- [ ] **Struiken en bessen aan de rand:** `/tp @s -2475 68 1374` en `-2523 71 1182`: azalea (soms bloeiend) en rijpe zoete bessen aan de buitenrand van het groen. In survival door een bessenstruik lopen: vertraagt en schaadt (vanilla) — storend aan de rand?
- [ ] Te groen, te "vanilla-weide"? Nog steeds de enige groene plek van de ring?

**B. Duinenzee (~9 min)**
- [ ] **Lage duinen, dal van dichtbij:** `/tp @s -2445 74 545`: dalbodem met vlekken gebarsten klei en grove aarde (`-2458 68 535`) met droog kort gras, dorre en dode struiken, tonnetjescactussen, af en toe een woestijnbloem; cactusgroepje bij `-2443 68 550` en `-2551 71 550`; halfbegraven fossiel `-2424 70 550`.
- [ ] **Hoge duinen vanaf de kam:** `/tp @s -2144 98 579` en rondkijken: begroeide, groen-grijze dalen tussen **kale kammen** (op de kam staat niets)? Daarna het dal `-2235 70 610` (dichtste begroeiing op een harde bodem).
- [ ] **Fossielen en botten:** `-2105 69 605` (schedel, met losse botblokken en een cactusgroepje bij `-2111 71 590`), `-2456 71 -171` (groot fossiel, 105 botten), `2732 80 -38` (op een flank). Half begraven, verweerd, herkenbaar als skelet? Losse botblokken in de dalen (~1 per 5 chunks): zie je ze? Bijzonder genoeg (~1 fossiel per 26 duinchunks)?
- [ ] **Cactusveld:** `/tp @s 2789 75 -98`: het grootste gemeten cactusgroepje (29 cactussen in een breed dal; normaal 2–5). Mooi herkenningspunt of te veel?
- [ ] **30b — Rotsuitstekels:** op de kam `/tp @s -2281 103 723` rondkijken (rots `-2247 87 663`) en `/tp @s 2666 101 -156` (rots `2648 78 -136`): zichtbaar als oriëntatiepunt, niet te vaak (~1 per 10–13 chunks)?
- [ ] **30b — Schaduw onder een kraag:** `/tp @s -2249 75 659` (onder de kraag van `-2247 87 663`) of `/tp @s 2652 72 -136`, in survival overdag: `/ca condition get` → `shade`.
- [ ] **30b — Mini-oases:** `-2141 67 755`, `2632 70 -120`, `-2333 66 -168`, `2742 70 -93`: verborgen genoeg (pas te zien vanaf de kam erboven)? Grasrand, suikerriet, 1–2 palmen; in de kern (2 diep) staan in survival → `/ca condition get` → `water`. Beloning genoeg?

**C. Zoutvlakte (~7 min)** — gebied x −2.815…−2.560, z −1.665…−1.410 (pekelpoelen en pilaren liggen op andere plekken dan in de 30b-lijst)
- [ ] **30b — Barstenpatroon:** `/tp @s -2700 90 -1560`, vanaf hoogte en op de grond (`-2700 66 -1560`): leesbaar als zeshoeken (~7–8 blokken), gesloten lijnen van zoutblok, niet te druk? Ook op afstand.
- [ ] **30b — Pekelpoelen:** `-2759 67 -1514`, `-2601 67 -1434`, `-2651 67 -1494`: rafelig rond, wit-turquoise, korstrand met kristallen; een kristal onder water bij `-2631 66 -1656`. Erin staan in survival → `/ca condition get` blijft `sun` (te ondiep).
- [ ] **30b — Pilaarvelden:** `-2605 66 -1581`, `-2651 66 -1565`, `-2805 66 -1421`: groepjes zoutschoorstenen 2–6 hoog, toelopend, soms een kristal erop; niet te regelmatig?
- [ ] **Eilandjes met leven:** `/tp @s -2587 72 -1632` en `/tp @s -2597 72 -1471`: 2 blokken verhoogd met zandrand, rood zand, rotsjes (30b), en nu dorre/dode struiken, tonnetjescactussen, een cactus (`-2576 68 -1648`), losse botten en halfbegraven fossielen (`-2598 69 -1628`, `-2603 68 -1480`). Blijft het zout eromheen wit en dood?
- [ ] **30b — Kristalveld 's nachts:** `/time set midnight`, `/tp @s -2655 67 -1492`: zwak gloeiende vlek, geen monsters vlakbij. Daarna `/time set noon`.

**D. Hoodoovelden (~4 min)** — gebied x 2.300…2.560, z 510…770
- [ ] `/tp @s 2480 95 659` en rondvliegen: dorre struiken, tonnetjescactussen, cactussen en een enkele woestijnbloem tussen de pilaren (~7 per chunk, was ~1,3).
- [ ] **Pilaarvoeten:** `2319 91 530` en `2458 88 722`: flora tegen de voet van de pilaren (schaduwkant)?
- [ ] **Cactusgroepjes op rood zand:** `2398 88 665`, `2537 89 690`.
- [ ] Levendiger dan in 2b, en nog steeds droog en rotsig?

**E. Grotten (30b, ~2 min)**
- [ ] `/gamemode spectator` en rondvliegen onder zout `-2700 30 -1560`, duinen `2600 30 -100` en kloven `2392 40 552`: minder volgelopen grotten dan in 2b (~15 % i.p.v. ~22 % van de holtes), reservoirs nog herkenbaar (bijv. `39 -23 -2536`, `-2534 -20 29`). Daarna `/gamemode creative`.

**F. Algemeen (~1 min)**
- [ ] Niet te druk: de kammen blijven leeg (verdwalen kan nog), het zout buiten de eilandjes blijft wit en dood.
- [ ] Geen zwevende blokken, planten in de lucht of cactussen tegen een rots gezien?
- [ ] Rondvliegen over duinen en oase: geen haperingen bij het laden van nieuwe chunks?

### Afstemvragen (antwoord per nummer; een getal wordt een fix-commit in `worldgen`, een vormwijziging wordt stop-en-vraag vóór change 31)
1. **Begroeiing duindalen** (~5 planten per dalchunk, kammen kaal): goed / meer (×1,5) / minder (×0,5)?
2. **Harde dalbodem en droog gras** (grove aarde + gebarsten klei op ~12 % van de dalbodem, kort gras erop): goed / meer / minder / geen gras?
3. **Oase-oever** (×2,8 gras en varens t.o.v. 2b): goed / dichter / minder?
4. **Oasebloemen** (~5 per oeverchunk in vlekken warm/wit/paars): goed / meer / minder / grotere vlekken (één kleur per vlek)?
5. **Struiken en bessen** aan de oaserand (~72 per 256 oasechunks): goed / meer / minder / geen bessen (schade)?
6. **Fossielen** (~1 per 26 duinchunks; op de zout-eilandjes ~6 per 256 zoutchunks; 40–70 % begraven): goed / vaker / zeldzamer / dieper / minder diep?
7. **Losse botblokken** (~1 per 5 duinchunks): goed / meer / minder?
8. **Cactusgroepjes** (2–5, 1–3 hoog, soms een veld zoals bij `2789 -98`): goed / altijd klein (geen velden) / meer / hoger?
9. **Hoodoovelden** (~7 planten per chunk, ~¼ aan pilaarvoeten): goed / meer / minder?
10. **Zout-eilandjes** (struiken, cactus, botten, fossiel): goed / meer / minder?
11. **Pekelpoelen** (~1 per 8 zoutchunks, straal 2–5, 1 diep): goed / vaker / zeldzamer / groter?
12. **Zeshoekpatroon** (cel ~7,5, randen 1 blok): goed / grotere cellen / minder regelmatig / minder opvallend?
13. **Zoutpilaren** (velden op ~12 %, 2–6 hoog): goed / hoger / vaker / zeldzamer?
14. **Duinrotsen** (~1 per 10–13 duinchunks, ~40 % met kraag): goed / vaker / zeldzamer / groter?
15. **Mini-oases** (~1 per 40 chunks met hoge duinen): goed / vaker / zeldzamer?
16. **Watergrotten** (~15 % van de holtes in ring 2): goed / minder / meer?
17. **Algemeen:** voelt ring 2 nu levendig genoeg om door te gaan naar change 31? Zo nee: welk biome het eerst?
