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

**Status:** plan (2026-10-03); ontwerpvragen beantwoord (alle A, 2026-10-03). Bouwen ná 30b (eilandjes en hun grond).

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
| _(bouw)_ | worldgen | Begroeiing ring 2 (30c-1): _antwoord_; duindalen begroeid, kammen kaal | Teststop 2b: alles moet levendig aanvoelen |
| _(bouw)_ | worldgen | Oase: bloemen in kleurvlekken (30c-2), dichter gras en varens, struiken en bessen aan de rand (30c-4): _antwoord_ | Teststop 2b: oase kaal; geen waterplanten of bredere gordel (sparring) |
| _(bouw)_ | worldgen | Droog gras in de duindalen (30c-3): _antwoord_ | Kort gras groeit niet op zand |
| _(bouw)_ | worldgen | Fossielen en botten (30c-5): _antwoord_; eigen feature `surface_fossil` op vanilla-sjablonen, halfbegraven, chunk-lokaal | Vanilla `FossilFeature` begraaft diep en schrijft in buurchunks |
| _(bouw)_ | worldgen | Cactusgroepjes (30c-6) en flora aan de pilaarvoeten in de hoodoovelden: _antwoord_ | Leven tussen de pilaren |

Commit (na `/code-review`): `worldgen: add ring 2 vegetation, cactus groups, fossils and oasis flowers`.

---

## In-game checklist (W) — teststop 2c (in te vullen door de bouwer van 30c)

_(coördinaten op seed 20260930; de W-punten van 30b overnemen; ~25–30 min; afstemvragen per biome onderaan)_
