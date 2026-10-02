# Change 26 — Grotten ring 2: ondergrondse waterreservoirs + instortende zandgrotten

Scope: `worldgen` — change 26

**Doel:** de ondergrond van ring 2 (overworld) krijgt zijn eigen karakter (§2 Ring 2, "Grotten"):
1. **Ondergrondse waterreservoirs**: in de diepte van de droge ring staan grotten vaker vol water, met een grondwaterspiegel. Ze "voeden de oases" (lore) en zijn het enige water onder een ring zonder neerslag.
2. **Instortende zandgrotten**: in bepaalde zones hebben grotten een plafond van los zand (`loose_sand`, change 21). Dat blijft hangen tot iemand ertegen graaft of eronder loopt, en stort dan in een kettingreactie in (server-side).

Alles wordt met worldgen-middelen gebouwd: noise router, aquifer, surface rules en noise. Er komen in deze change geen nieuwe blokken of blokgedrag: het plafondblok `loose_sand` (met zijn instortingstrigger) komt uit change 21 (`blocks`, vraag 5 B). Er komen geen features die de wereld lezen. Daardoor hangt het resultaat nergens af van buurchunks of van de generatievolgorde. Nether-ring 2 valt hier niet onder: het terrein daarvan is change 35.

Bron: `fase-1-wereld.md` §2 Ring 2 ("Grotten": reservoirs/aquifers die de oases voeden, "oriëntatie en lore"; "Instortende zandgrotten: plafonds van los zand (vanilla-zwaartekracht, server-side)"; client/server-risico's: "Instortend zand … vanilla-mechanismen, server-side"), §1.15 (`ring_noise`, aquifer), bouwplan rij 26 (B, W).

---

## Afstemming (2026-10-01)

Afgestemd met 23, 25, 29, 30, 31 en 35 (basis: de aanbevolen opties).
- **Basisgrotten zitten in change 23 (vraag 4 A):** stap 1 en vraag 1 **vervallen**; ze gelden alleen als 23 vraag 4 B of C koos. De bestandsindeling van 23 is precies die van stap 1.
- **Zandgrot-tak:** top-level, ná `above_preliminary_surface` en **vóór** het kloofsteen-lagenblok van 23. De `biome_is`-lijst komt op de controlelijst "Plekken die ring 2-biomes opsommen" (`CODEBASE.md`, aangemaakt in 23); 30 en 31 vullen hem aan (bij vraag 7 A).
- **Lavameren** haalt deze change uit `Ring2Biomes.generation` (23); 30 en 31 krijgen dat vanzelf via de helper.
- **`RingFluidPicker`:** `waterOnly` komt naast `noFluid`; change 35 voegt in dezelfde class `fluid_levels` (Nether) toe — één plek voor vloeistofregels per ring.
- **Ring 1 — belangrijk:** floodedness via `ring_blend` houdt ring 1 blok voor blok gelijk, maar in de zone 1→2 mengt de ring 2-floodedness mee (ook in de ring 1-helft van `withered_expanse`). Kleine, ondergrondse wijziging aan de goedgekeurde ring 1-zone. **Besloten 2026-10-01 (gebruiker, punt B2): geaccepteerd**, samen met de ore veins van change 25.
- Na de grotten herhaalt deze change de ertsmeting van 25 (zie "Voor latere changes" in plan 25).
- **Vraag 1 vervalt** (23 vraag 4 A, besloten): de basisgrotten zitten in change 23; stap 1 wordt niet gebouwd.
- **Los zand (vraag 5 = B, keuze gebruiker):** zandgrot-plafonds gebruiken `corruptedadventure:loose_sand` uit change 21 in plaats van vanilla-zand; de vloer blijft vanilla-zand. RCON-test van het instorten in S.
- **Hoogtes (23 vraag 5 = B):** onder de plateaus (tot Y ~140) is de kolom van Y 0 tot het oppervlak hoger dan eerder gepland; de zandzone-regel is relatief (`not above_preliminary_surface`) en hoeft niet te veranderen, maar er zijn per kolom meer grotlagen met een zandplafond — meten in S.
- Teststop: geen eigen stop. **Bijgewerkt 2026-10-01:** de W-achteraf-lijst gaat mee in **teststop 2b na change 30** (gebundelde terreincheck ring 2, blokkerend vóór 31; keuze gebruiker 23b-17), niet meer in teststop 3.

## Afstemming na teststop 2 (2026-10-01)

Afgestemd met de plannen 23b en 23c (antwoorden 2026-10-01). Waar de tekst hieronder iets anders zegt, geldt dit blok.
- **Zandgrot-tak raakt de nieuwe vormen nooit:** spleten, nissen, richels, overhangen, tafelbergen en rotsvormen van 23b liggen allemaal **boven** het preliminary surface (dat ligt in het rotsgebied op de laagste kloofbodem). De regel `not above_preliminary_surface` sluit ze dus uit. Dat klopt met "nooit een overhang aan het oppervlak": niets aan te passen, wel als feit in `CODEBASE.md` noteren.
- **Onder tafelbergen** (toppen tot Y ~178) liggen meer grotlagen: meer plafonds en meer noise-tijd. Zandplafonds niet: die blijven onder het preliminary surface, en dat ligt in het hele rotsgebied op de laagste kloofbodem (~Y 69), ook onder plateau en tafelbergen. Meetpunt "grootste plafond" (`tick query` bij instorten) en perf ook in het kloofgebied doen, op een plek met tafelbergen (meetplek uit `CODEBASE.md`, vastgelegd door 23b).
- **Floodedness-band** (Y < 60) verandert niet; 23b houdt alle open ruimte ≥ Y 66 en het preliminary surface op de laagste kloofbodem.
- **Carvers** kunnen bruggen, vinnen en naalden aansnijden (vanilla-gedrag, geaccepteerd zoals bij de hoodoos).
- **Prestatiebudget (23b-9 C, keuze gebruiker):** de ring 2-noise-stap mag **≤ 1,75× vanilla** (gedeeld budget voor 23b + 26 + 30 + 31), niet meer 1,3×. Deze change meet vóór/na in het kloofgebied en op x 2.500; een stijging van meer dan ~0,05× door de floodedness is een melding waard.
- **Oase-water (23b-8):** de oasepoelen blijven ~6–8 % van `oasis_flats` (oppervlak, Y ≤ 63) en zijn geen aquiferwater; de S-meting "boven Y 60 vrijwel geen aquiferwater" telt de poelen apart.

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 17, commit `7714b77`). Changes 18–25 zijn nog niet gebouwd en de plannen van 23–25 bestaan nog niet. **Bij de start van de bouw alle aannames A1–A5 opnieuw nalopen tegen `CODEBASE.md` en de gebouwde code.**

- **Gecontroleerd — noise router overworld** (`data/corruptedadventure/worldgen/noise_settings/overworld.json`): `aquifers_enabled: true`, `sea_level` 63. `fluid_level_floodedness`, `fluid_level_spread`, `barrier` en `lava` zijn vanilla-noises (`minecraft:aquifer_*`), zonder ringafhankelijkheid. `final_density` → `ring_blend/overworld`, `initial_density_without_jaggedness` → `ring_blend/overworld_initial` (ring 2: `0,390625 + ring_2/overworld`).
- **Gecontroleerd — `RingBlend`** accepteert per ring een verwijzing of een inline density function (alle 6 verplicht), rekent alleen de 1–2 nodige inputs en mengt in de overgangszone met smoothstep. Hij werkt op elke `FunctionContext`, dus ook op de losse punten waarop de aquifer sampelt.
- **Gecontroleerd — `RingAquifer`** (`worldgen/chunk/`) wrapt de vanilla-aquifer van elke noise chunk (één instantie per noise chunk, single-threaded). Nu: open ruimte in de void-zone → lucht (`RingFluidPicker.noFluid`). Dit is de enige plek waar een ringregel op aquifer-vloeistof kan aangrijpen. Valkuil uit `CODEBASE.md`: de `lava`-router-noise wordt op rastercoördinaten gesampled (x/64, y/40, z/64), dus een ringregel voor lava **moet** in `RingAquifer` en kan niet in de router.
- **Gecontroleerd — surface rule overworld**: de top-level `sequence` heeft 3 elementen: [0] bedrock-vloer, [1] `above_preliminary_surface` (met de eigen tak van `withered_expanse` bovenaan; change 23 zet de ring 2-oppervlaktetakken ook daar), [2] deepslate-gradient (Y 0–8).
- **Gecontroleerd — placeholder `ring_2/overworld.json`**: `y_clamped_gradient` + 2D-ruis, **zonder grotten**. Placeholder-biomes hebben geen carvers.
- **A1 — change 23 (ring 2-terrein + `canyonlands`/`oasis_flats`), afgestemd 2026-10-01:** het plan van 23 bestaat. Bij 23 vraag 4 A heeft ring 2 al **vanilla noise-grotten + carvers**, met precies de bestandsindeling van stap 1 (`ring_2/overworld.json` met grotten, `ring_2/overworld/terrain.json` zonder, `ring_blend/overworld_initial` → `terrain`). Stap 1 en vraag 1 **vervallen** dan. Biome-bootstrap = `Ring2Biomes` + `Ring2Biomes.generation(...)`; het kloofsteen-lagenblok is een top-level-element (de zandgrot-tak komt daarvóór).
- **A2 — biome-keys uit change 23** (aanname, nog niet gebouwd): `corruptedadventure:canyonlands`, `corruptedadventure:oasis_flats` in `ModBiomes`. Datagen-bootstrap waarschijnlijk een `Ring2Biomes`-achtige class in `worldgen/biome/`. Andere namen betekenen alleen andere verwijzingen.
- **A3 — ring gate (backlog, change 23)**: bouwt change 23 de ring gate in `RingBlend.mapAll`, dan kosten de ring 2-grotten alleen iets in ring 2-chunks. Zonder ring gate: meten (stap 6) en de bevinding doorgeven.
- **A4 — blokken uit change 21** (`docs/plans/change-21.md`): de kloofsteentinten (`red_`, `orange_`, `cream_canyon_stone`, tag `corruptedadventure:canyon_stones`), `canyon_cobblestone`, `scorched_sandstone` en `cracked_clay` staan in `#minecraft:overworld_carver_replaceables`, dus carvers en vanilla-grotten snijden erdoor. Vanilla `minecraft:sand` staat daar al in. Deze change gebruikt voor de plafonds **`corruptedadventure:loose_sand`** uit change 21 (vraag 5 B; in `#overworld_carver_replaceables`, niet in `#minecraft:sand`) en voor de vloeren vanilla-zand. Bestaat `loose_sand` bij de start niet: stoppen en vragen.
- **A5 — changes 24/25 (ertsen ring 2)**: turkoois ligt "diep in kloofsteen" en zonnesteen "hoog in canyonwanden". Daar is geen conflict: ertsen (`minecraft:ore`) vervangen geen zand of water, en de zandplafonds vervangen alleen `stone` (de default block, zie stap 3).
- **Vastgesteld gedrag van vanilla 1.21.1** (bij de bouw verifiëren in de bronnen):
  - Worldgen zet blokken zonder block updates of scheduled ticks. Zand (en `loose_sand`, een `FallingBlock`-subklasse) dat de surface rules of carvers boven lucht zetten, **blijft hangen** tot een buurblok verandert (of, bij `loose_sand`, tot een speler eronder komt — trigger van change 21; mobs triggeren niet) (graven, plaatsen, explosie, stromend water, een vallend blok ernaast). Bij die update plant `FallingBlock#updateShape` een tick, en het zand valt. Elk blok dat valt, geeft zijn buren weer een update. Een aaneengesloten zandplafond met lucht eronder stort zo **als kettingreactie** in, met ~2 ticks per stap. Zolang er lucht onder het zand zit, toont `FallingBlock#animateTick` al vallende stof-particles (client, vanilla). Dat is een natuurlijke waarschuwing.
  - `SurfaceSystem` loopt per kolom over **alle** Y-niveaus. `stone_depth` met `surface_type: ceiling` meet de afstand tot de eerstvolgende lucht of vloeistof eronder, dus het werkt ook op grotplafonds diep onder het oppervlak en boven water. Surface rules vervangen alleen de default block (`stone`), draaien **vóór de carvers** en zien dus alleen noise-grotten.
  - Floodedness in `NoiseBasedAquifer`: sampled op blokcoördinaten van de aquiferceldpunten. Hoog → overstroomd tot het globale niveau (zee, Y 63), middel → een willekeurig lokaal niveau, laag → droog. Dicht onder het preliminary surface wordt de floodedness vanzelf lager, dus grotten vlak onder de grond blijven droog. Lokale lava: aquiferniveau ≤ −10 en `|lava-noise| > 0,3`. Onder Y −54 geeft de globale vloeistof-kiezer lava.

---

## Plan

Volgorde = bouwvolgorde. Het plan volgt de aanbevolen opties; bij een ander antwoord verandert alleen de stap van die vraag.

### 1. Basisgrotten in ring 2 (vraag 1 A; **vervalt bij change 23 vraag 4 A** — afstemming 2026-10-01: change 23 is de enige plek voor de basisgrotten)

Reservoirs en zandplafonds hebben holtes nodig. Ring 1 heeft vanilla-noise-grotten via vanilla's `final_density`, ring 2 (placeholder) heeft geen grotten.

- **Bestandsindeling volgens ring 1** (`ring_1/overworld.json` = final mét grotten, `ring_1/overworld/initial_density` = zonder):
  - `ring_2/overworld/terrain.json`: het ring 2-terrein van change 23, zonder grotten, verplaatst uit `ring_2/overworld.json`.
  - `ring_2/overworld.json`: het grotdeel van vanilla's `final_density`, met `ring_2/overworld/terrain` waar vanilla `sloped_cheese` gebruikt. Dus `range_choice` op het terrein (< 1,5625 → `min(terrein, 5 · entrances)`, anders "underground" = cheese, spaghetti 2D + roughness, entrances, pillars), daarna `min(…, noodle)`. De grotnoises en -functies blijven `minecraft:overworld/caves/*` en worden niet gekopieerd of overschreven. Dezelfde holders als in ring 1 betekent ook dezelfde cache-wrappers in `NoiseChunk`.
  - `ring_blend/overworld_initial.json`: ring 2 → `0,390625 + ring_2/overworld/terrain` (**zonder** grotten, zoals vanilla's initiële dichtheid). Anders vindt het preliminary surface grotplafonds en kloppen aquifers en surface rules niet.
- De grootte van het ring 2-terrein kan afwijken van vanilla's `sloped_cheese` (de cheese-term gebruikt `1,5 − 0,64 · sloped_cheese`). Controleer in W/S dat de grotten niet tot aan het oppervlak openscheuren en dat ze er wel zijn. Stel zo nodig één schaalfactor in de JSON bij.
- **Carvers**: de ring 2-biomes houden of krijgen vanilla's `cave`, `cave_extra_underground` en `canyon` (A1).
- Nether: niets (change 35).

### 2. Waterreservoirs via de aquifer (vraag 2 A, vraag 3 A)

**Floodedness per ring** (router, puur noise):
- Nieuw `density_function/ring_blend/overworld_floodedness.json` (`corruptedadventure:ring_blend`, `dimension` overworld):
  - ring 1, 3, 4, 5, void: inline vanilla-definitie (`minecraft:noise`, `minecraft:aquifer_fluid_level_floodedness`, `xz_scale` 1,0, `y_scale` 0,67). Ring 1 blijft daarmee blok voor blok vanilla. Ring 3–5 krijgen later hun eigen invoer (ring 3 "water gestolen", ring 4 "overstroomde grotten", change 62).
  - ring 2: `ring_2/overworld/floodedness.json` = dezelfde vanilla-noise + een diepteband via `y_clamped_gradient`s. **Startwaarden** (af te stemmen in S): +0 boven Y 60, oplopend tot +0,4 bij Y 30, +0,4 tot Y −40, terug naar 0 bij Y −54.
- In `noise_settings/overworld.json`: router-veld `fluid_level_floodedness` → `corruptedadventure:ring_blend/overworld_floodedness`. Optioneel (alleen als S te veel losse plasjes laat zien): `barrier` via eenzelfde `ring_blend/overworld_barrier` iets lager in ring 2, zodat de reservoirs groter en meer aaneengesloten worden.
- Effect: in ring 2 staan grotten onder de grondwaterspiegel (Y 63, de globale vloeistof) vaak vol, en daarboven zijn er lokale reservoirs. Grotten vlak onder het oppervlak blijven droog (vanilla's oppervlakdemping), dus het oppervlak blijft "de droge ring".

**Geen lokale lava in ring 2** (vraag 3 A):
- `RingFluidPicker`: nieuwe package-private regel naast `noFluid`: `static boolean waterOnly(RingPosition position, int y)` = `position.ring() == Ring.RING_2 && y >= −54`. Het blijft één plek voor de vloeistofregels per ring, en een latere ring (bijv. 3) wordt één extra regel.
- `RingAquifer.computeSubstance`: geeft vanilla lava terug en geldt `waterOnly`, dan wordt het `Blocks.WATER.defaultBlockState()`, en `shouldScheduleFluidUpdate` geeft `true`. De vanilla-lavalaag onder Y −54 (globale kiezer) blijft.
- Lavameren als feature (`lake_lava_underground`, `lake_lava_surface`): hebben de ring 2-biomes van change 23 die (via `globalOverworldGeneration`), dan gaan ze daar uit. Dat gebeurt in de biome-bootstrap van change 23 (datagen → D), met de vanilla-volgorde van de rest intact. Weglaten geeft geen "Feature order cycle".
- De overgang 1↔2 wisselt hard op `ring()` (gewicht 0,5). Een vanilla-lavacel die over de grens loopt, wordt aan de ring 2-kant water. Raken water en lava elkaar daar, dan maakt een fluid-tick steen of obsidiaan. Dat is zeldzaam (alleen onder Y −10) en geaccepteerd (risico's).

### 3. Zandgrotten via een surface rule (vraag 4 A, 6 A, 7 A)

- **Nieuwe noise** `worldgen/noise/sand_cave_zone.json` (handgeschreven, zoals `placeholder_terrain`). Startwaarden: `firstOctave` −8, amplitudes `[1.0, 1.0]`, dus vlekken van ~200–400 blokken.
- **Nieuwe top-level-tak** in de surface rule van `noise_settings/overworld.json`, als **nieuw element [2]** (na `above_preliminary_surface`, vóór de deepslate-gradient). Komen er van change 23 ondergrondse ring 2-takken op top-niveau bij (bijv. kloofsteenlagen), dan komt deze tak **daarvóór**, zodat een plafond in een zandzone zand wordt en geen kloofsteen. Condities (alle waar):
  - `biome`: `corruptedadventure:canyonlands`, `corruptedadventure:oasis_flats` (vraag 7 A; de changes 30/31 voegen hun biomes aan deze lijst toe, dat komt in het design-doc en in `CODEBASE.md`).
  - `not(above_preliminary_surface)`: alleen echt ondergronds, nooit een overhang aan het oppervlak.
  - `y_above` absolute 0 (boven de deepslate-band) en geen bovengrens: die volgt uit de vorige conditie.
  - `noise_threshold` `corruptedadventure:sand_cave_zone`, `min_threshold` ~0,25 (startwaarde, doel ~25 % van het ring 2-oppervlak, vraag 6 A). De conditie sampelt 2D (x, z), dus een zone is een hele kolom.
  - Daarbinnen een `sequence`:
    - `stone_depth` `ceiling`, `offset` 1, `secondary_depth_range` 1 → `corruptedadventure:loose_sand` (vraag 5 B). Het plafond wordt zo 2–3 blokken dik los zand.
    - `stone_depth` `floor`, `offset` 0 → `minecraft:sand` (vanilla; een vloer kan niet instorten): een zandvloer, zodat de grot ook "zandgrot" leest en het ingestorte zand niet opvalt als vreemd.
- Waarom dit deterministisch is: de regel is een pure functie van (seed, x, y, z) plus de noise-kolom van dezelfde chunk. Er wordt niets gelezen uit buurchunks en niets uit features. Carvers die later onder een zandzone door snijden, maken daar ook hangend zand (zoals vanilla in woestijnen). Dat is gewenst en net zo deterministisch.
- Zand boven een reservoir telt mee (`stone_depth` ziet water als "geen steen"): een overstroomde zandgrot kan inzakken in het water.

### 4. Instorten (vraag 5 B, keuze gebruiker)

- **Geen code in deze change:** het gedrag zit in `loose_sand` (change 21). Triggers, allemaal server-side: (1) een speler (geen mob; keuze gebruiker) binnen ~2 blokken onder het plafond → korte waarschuwing (stof-particles, kraakgeluid, ~0,75 s) en daarna valt het blok (`LooseSandTrigger`, change 21); (2) vanilla: graven of plaatsen tegen het plafond, explosies (creepers, TNT), water of lava dat ernaast gaat stromen, en een vallend blok of een ander ingestort plafond. Elk vallend blok geeft zijn buren een update, dus het aaneengesloten plafond stort als kettingreactie in.
- Gevolg voor de speler: vallend zand doet geen schade, maar wie eronder staat, raakt bedolven en stikt. Fakkels op de vloer breken. De stof-particles (vanilla, onder elk hangend zandblok) en de waarschuwing van de trigger gaan vooraf.
- **Mobs** laten het plafond niet instorten (alleen spelers; besloten 2026-10-01), dus een zandgrot is intact als een speler aankomt.
- **Omvang begrenzen (performance):** elk vallend blok is een entity. De grootte van een kettingreactie = de aaneengesloten zandplafonds in één grot, en de dikte (vraag 6) bepaalt dat mee. Meten in S (`/tick query` tijdens een ingestort plafond). Doel: geen MSPT-piek boven 50 ms bij het grootste gevonden plafond. Anders: dunner (offset 0) of kleinere zones.

### 5. Unit tests (`./gradlew build`)

- `worldgen/chunk/RingAquiferTest` (uitbreiden): ring 2 + vanilla-lava op Y ≥ −54 → water, en `shouldScheduleFluidUpdate` is `true`. Ring 2 onder Y −54 houdt lava. Ring 1 houdt lava. Void blijft lucht. Overgangszone: wissel op `ring()`. Bestaande tests blijven groen (regressie).
- `worldgen/chunk/RingFluidPickerTest` (uitbreiden): `waterOnly` alleen voor ring 2 en Y ≥ −54, en `noFluid` is ongewijzigd.
- Geen unit test voor de JSON. Of die laadt, toetst S (een fout in een density function of surface rule laat de server niet starten). De verdeling toetsen de RCON-metingen.

### 6. Metingen (in test S, via RCON; werkwijze uit `CODEBASE.md`)

Seed 20260930, 15×15 chunks (`forceload add`, `execute if loaded` pollen) rond `/ca tpring 2` in 3 richtingen. Referentie: 15×15 chunks in ring 1. Tellen met `fill … replace`, in een verse wereld per meting.
- **Reservoirs**: water en lucht in Y −54…62 per chunk. Doel (startwaarde): in ring 2 is het aandeel water in de holtes 2–3× zo groot als in ring 1. Boven Y 60 vrijwel geen aquiferwater (en wel de oasepoelen van change 23).
- **Lava** in ring 2, Y −54…−10: 0 (vraag 3 A). Ring 1: vanilla-waarden.
- **Zand** in ring 2, Y 0…(preliminary surface − 8): `corruptedadventure:loose_sand` en `minecraft:sand` > 0 in de zandzones, 0 daarbuiten (de bands boven Y 127 meetellen: plateaus tot Y ~140, tafelbergen van 23b tot Y ~178; let op: sinds 23/23b ligt het preliminary surface in het rotsgebied op de laagste kloofbodem, ~Y 69, dus daar verwacht je boven ~Y 61 geen zand — zand hoger in de kolom is een fout). Steekproef: plafond = `loose_sand` met lucht eronder (`execute if block … corruptedadventure:loose_sand` + `if block ~ ~-1 ~ air`), vloer = `minecraft:sand`; geen `loose_sand` buiten plafonds.
- **Instorting — RCON-test van `loose_sand` in een echte zandgrot** (geforceloade chunk; scheduled ticks en entities lopen daar, random ticks zijn niet nodig):
  1. *Speler-trigger:* de quick-play-client `Dev` (change 18) joint automatisch; een gemeten zandgrot-plafond zoeken (steekproef hierboven), RCON `gamemode survival Dev` + `tp Dev <x> <y> <z>` op de grotvloer eronder (bovenkant hoofd ≤ 2 blokken onder het plafond) → binnen ~60 ticks is het plafond boven de speler weg (`execute if block … air`) en ligt er zand op de vloer; de kettingreactie neemt het aaneengesloten plafond mee (tellen met `fill … glass replace corruptedadventure:loose_sand` vóór en na). Lukt de automatische client-join niet: deze stap naar W-achteraf.
  2. *Controle:* een ander plafond met een `summon`-de zombie (`NoAI:1b`) eronder en een derde zonder iets eronder hangen na 200 ticks nog (mobs triggeren niet).
  3. *Vanilla-pad:* een plafondblok met `setblock … air` weghalen → kettingreactie, ~200 ticks.
  4. `tick query` tijdens 1 en 3: geen MSPT-piek boven 50 ms.
  Werkt een stap via RCON toch niet, dan in-game (W).
- **Ring 1 ongewijzigd**: water en lava in 5×5 ring 1-chunks zijn gelijk aan dezelfde chunks vóór deze change (floodedness ring 1 = vanilla-definitie).
- **Determinisme**: twee verse werelden met dezelfde seed, de tweede in omgekeerde volgorde geladen. Per chunk dezelfde aantallen water, lava en zand.
- **Performance** (JFR, `minecraft.ChunkGeneration`, stap `noise`, zoals bij change 9): ring 2 vóór en na change 26 (kloofgebied met tafelbergen en x 2.500), budget **≤ 1,75× vanilla** (23b-9 C, gedeeld met 23b/30/31; was 1,3×). Leg de getallen vast in `CODEBASE.md`. Zonder ring gate (A3) ook ring 3 meten, want die betaalt dan mee.

### 7. Documentatie

- `CODEBASE.md`:
  - `RingAquifer`/`RingFluidPicker` (`waterOnly`, lava → water in ring 2), databestanden (`ring_2/overworld.json` met grotten, `ring_2/overworld/terrain.json`, `ring_2/overworld/floodedness.json`, `ring_blend/overworld_floodedness.json`, noise `sand_cave_zone`, surface-rule-tak [2] met de biome-lijst), tests, meetresultaten, "Laatst bijgewerkt", status change 26.
  - Valkuilen:
    - Worldgen-`loose_sand` hangt tot een block update of tot een speler eronder komt (mobs niet), en dan volgt een kettingreactie.
    - Surface rules draaien vóór de carvers en zien alleen noise-grotten.
    - `stone_depth ceiling` werkt op elke diepte.
    - Floodedness kan per ring via `ring_blend`, lava niet (rastercoördinaten).
    - Grotten nooit in de initiële dichtheid.
- `fase-1-wereld.md` §2 Ring 2 → "Grotten": **Uitwerking (change 26)** met de antwoorden (incl. vraag 5 B: plafonds van `loose_sand` uit change 21), startwaarden en meetresultaten. Ook de afspraak: nieuwe ring 2-biomes (changes 30/31) komen in de `biome_is`-lijst van de zandgrot-tak en krijgen vanilla-carvers en geen lavameren.
- `02-beslissingenlog.md`: de regels hieronder.
- `docs/backlog.md`:
  - `[atmosphere] Zandgrotten: kraak- of ruisgeluid / extra stof als waarschuwing (biome-ambient of client-particles); mogelijk in change 32. (bron: change 26)`
  - `[structures] Reservoirs en lore: oude putten, pompstations of trappen naar de grondwaterspiegel onder oases (fase 3). (bron: change 26)`
  - `[progression] Lore "het water van ring 2 zit onder de grond / is gestolen door ring 3" in quests of teksten (fase 5). (bron: change 26)`

---

## Client/server-impact

- **Puur server-side worldgen** (noise router, aquifer-wrapper, surface rules). Geen netwerk, geen sync, geen client-code, geen nieuwe registry-entries (alleen datapack-JSON plus een regel in een bestaande wrapper). `RingLayout.current()` wordt alleen tijdens worldgen gebruikt, via `RingColumnCache`, zoals nu al.
- **Instorten**: trigger van `loose_sand` (change 21, server-side) + vanilla `FallingBlockEntity`, server-side gesimuleerd en door vanilla naar alle clients gesynct. Twee spelers zien dezelfde instorting; de waarschuwing (particles/geluid) stuurt de server. De vallende-stof-particles zijn vanilla client-code. Geen client-class bereikbaar vanuit deze change.
- **Determinisme**: alles is een functie van seed en positie (noise, aquifer per noise chunk, surface rule per kolom). Er is geen placement of feature die buurchunks of de generatievolgorde leest. Test: twee werelden, omgekeerde laadvolgorde (stap 6).
- Bestaande werelden: chunks die al gegenereerd zijn, veranderen niet. Nieuwe chunks in ring 2 krijgen grotten en reservoirs, dus op de rand tussen oude en nieuwe chunks kunnen grotten en water afgekapt zijn. Testwerelden van vóór change 26 dus opnieuw aanmaken.

---

## Tests

Bouwplan: **B, W**. Extra: **S** (aanbevolen: de JSON moet op een dedicated server laden, en de metingen van stap 6 lopen via RCON) en **D** (alleen als de biome-bootstrap van change 23 verandert: carvers of lavameren).

- [ ] **B**: `./gradlew build` slaagt, `RingAquiferTest` en `RingFluidPickerTest` zijn groen.
- [ ] **D** (voorwaardelijk): `./gradlew runData` slaagt. De ring 2-biome-JSON's hebben carvers en (vraag 3 A) geen `lake_lava_*`. Output gecommit.
- [ ] **S**: `tools/server-smoke-test.sh` met `level-name=smoke-change26`. Server start zonder fouten (density functions, surface rule en noise laden). Daarna de metingen van stap 6 via RCON. Daarna RCON weer uit.
- **Blokkerende handmatige test: nee.** Alles waar latere changes op bouwen, is automatisch te toetsen via build, serverstart en RCON-tellingen: het vloeistofgedrag per ring (change 27 rekent op water in ring 2, change 42/62 breiden `waterOnly`/floodedness uit), de surface-rule-tak met biome-lijst (changes 30/31), het determinisme en de instorting via de quick-play-speler (`tp`) en `setblock`. Het speelgevoel (hoe het eruitziet, hoe eng het voelt) is afstemming. → **W-achteraf, afgelopen in teststop 2b na change 30** (bijgewerkt 2026-10-01; de bouwer van 30 neemt deze lijst op in de gebundelde checklist), in-game checklist (seed 20260930, preset `corrupted_world`, spectator en survival):
  - [ ] `/ca tpring 2` + rondvliegen onder de grond: er zijn grotten (vraag 1). Ze scheuren niet overal open aan het oppervlak, en de ingangen in canyonwanden lezen natuurlijk.
  - [ ] Reservoirs: overstroomde grotten tot de grondwaterspiegel (Y 63) en lokale meren erboven. Vlak onder het oppervlak droog. Geen lava tussen Y −54 en −10 (vraag 3).
  - [ ] Zandgrotten: zones met plafond van los zand en zandvloer, stof-particles onder het plafond. Niet aan het oppervlak, niet in de deepslate-laag.
  - [ ] Survival: onder een zandplafond lopen → kort stof en gekraak, dan stort het plafond als golf in; wie blijft staan raakt bedolven. Eén plafondblok uithakken doet hetzelfde. Geen merkbare lag (F3 / `tick query`). Een creeper-explosie in een zandgrot doet hetzelfde.
  - [ ] Zandgrot boven een reservoir: het zand zakt in het water, zonder rare vloeistofmuren.
  - [ ] Overgang 1→2 (`/ca tpboundary 1`): geen harde naad in grotten of water. Hoogstens een zeldzaam stukje steen of obsidiaan waar lava en water elkaar raken.
  - [ ] Ring 1 en ring 3 onder de grond: ongewijzigd (vanilla-grotten in ring 1; ring 3 nog placeholder).
  - [ ] Standaard-W: midden van elke ring en elke grens (overworld en Nether) en een portal vanuit elke ring. Geen nieuwe fouten in de log.

---

## Risico's / aandachtspunten

- **Change 23 is nog niet gepland of gebouwd** (A1/A2). Bestandsindeling, biome-keys, carvers en de schaal van het terrein kunnen afwijken. Stap 1 en de biome-lijst in stap 3 zijn het gevoeligst. Bij de start opnieuw nalopen.
- **Grotdichtheid op een ander terrein**: vanilla's cheese-grotten zijn afgestemd op `sloped_cheese`. Op het ring 2-terrein kunnen ze te groot uitvallen, te klein, of door het oppervlak breken. Daarom één schaalfactor in de JSON, af te stemmen in S/W.
- **Performance**: grotten in ring 2 kosten in elke chunk zonder ring gate (valkuil "density functions"). Meten (stap 6). Een overschrijding komt in het rapport en in de backlogregel van de ring gate, niet stilletjes doorbouwen.
- **Grote instortingen**: honderden vallende blokken tegelijk kunnen een server laten haperen, en een speler kan een instorting expres bij een ander veroorzaken (multiplayer-griefing via vanilla-mechaniek). Dikte en zonegrootte (vraag 6) houden het beperkt. Meten met `tick query`. Onder de hoge plateaus (23 vraag 5 B) zijn er per kolom meer grotlagen, dus mogelijk meer plafonds — meten.
- **Naad tussen lava en water op de ringgrens** (vraag 3 A): de wissel is hard op gewicht 0,5. Zeldzaam en klein, geaccepteerd. Een vloeiende wissel zou de lava-noise per aquifercel nodig hebben, en die is private in vanilla.
- **Aquifer-water en fluid ticks**: omgezette lava krijgt `shouldScheduleFluidUpdate` = `true`, anders blijft het water "bevroren" naast lagere cellen. Unit test.
- **Volgorde van surface rules**: de nieuwe tak moet vóór deepslate staan en vóór eventuele ondergrondse ring 2-takken van change 23. Een verkeerde volgorde geeft geen fout, alleen geen zand. Controle in S (zandtelling).
- **`noise_threshold` is 2D**: een zandzone geldt voor alle grotlagen in die kolom tussen Y 0 en het oppervlak. Dat is bewust (een "zandgebied"), maar diepe grotten in een zone hebben ook zandplafonds.
- **Bestaande testwerelden** krijgen naden (zie client/server-impact).

---

## Ontwerpvragen

### 1. Welke basisgrotten krijgt ring 2? (alleen als change 23 vraag 4 B of C koos; bij 23-4 A vervalt deze vraag)
- **A. Vanilla-noise-grotten (cheese, spaghetti, noodle) op het ring 2-terrein, plus vanilla-carvers (aanbevolen).** Ring 2 voelt onder de grond net zo rijk als ring 1, en grote cheese-holtes geven ruimte voor echte reservoirs en zandzalen. Alleen JSON (een kopie van vanilla's grotdeel, met verwijzingen naar `minecraft:overworld/caves/*`). Kost noise-tijd in ring 2 (meten). Deterministisch.
- **B. Alleen smalle grotten (spaghetti + noodle, geen cheese) plus carvers.** Minder en kleinere holtes: kleinere reservoirs en zandgrotten, goedkoper. Ring 2 voelt ondergronds kaler.
- **C. Alleen vanilla-carvers, geen noise-grotten.** Goedkoopst. Maar surface rules draaien vóór de carvers, dus de zandplafonds van vraag 4 A bestaan dan niet: vraag 4 wordt B. De reservoirs zijn dan alleen carvergangen.

Antwoord: vervallen — zie change 23 vraag 4 A (vanilla noise-grotten + carvers zitten al in change 23; stap 1 vervalt ook).

### 2. Hoe ontstaan de ondergrondse waterreservoirs?
- **A. Aquifer-afstemming per ring: hogere floodedness in ring 2 in een diepteband (Y ~−40…60), via `ring_blend` op het router-veld (aanbevolen).** Echte vanilla-aquifers: een grondwaterspiegel op Y 63, lokale meren, barrières en droge grotten vlak onder het oppervlak. Alleen JSON, volledig deterministisch, nul kosten. De reservoirs liggen overal onder ring 2 en zijn niet gekoppeld aan de oases ("voeden de oases" is lore: de oases zijn waar het grondwater bovenkomt).
- **B. Als A, maar sterker onder de oases**, door de floodedness te mengen met de klimaatruis waarmee change 23 `oasis_flats` kiest. Reservoirs liggen dan vooral onder de oases, wat de lore letterlijk maakt. Maar het hangt aan de biomekeuze van change 23 (verandert met changes 30/31) en is lastiger af te stemmen.
- **C. Eigen feature `underground_reservoir`** (een koepelgrot met een meer, zandstenen oever) onder `oasis_flats`. Een herkenbare vorm, maar max. ~40 blokken breed (3×3-chunkgrens). Een feature moet de wereld lezen om lekken te voorkomen, en lezen hangt af van de generatievolgorde (valkuil change 16). Ook meer Java.

Antwoord: **A** — aquifer-afstemming per ring: hogere floodedness in ring 2 in een diepteband via `ring_blend` op het router-veld. Het geleidelijk meelopen van de floodedness in de ring 1-helft van zone 1→2 is door de gebruiker geaccepteerd (2026-10-01, punt B2).

### 3. Is er lava in de ondergrond van ring 2?
- **A. Geen lokale lava: aquifer-lava boven Y −54 wordt water, en geen lavameren in de ring 2-biomes. De vanilla-lavalaag onder −54 blijft (aanbevolen).** Dit past bij "de ring met het water onder de grond" en geeft contrast met ring 3 (lava, "water gestolen"). Eén regel in `RingAquifer` + een unit test. Een speler die diep in ring 2 graaft, vindt water in plaats van lava (minder gevaar, meer verkoeling voor de uitdroging van change 27).
- **B. Vanilla: lokale lava onder Y −10, lavameren zoals in de woestijn.** Geen code, en het gevaar blijft vertrouwd. Maar de "waterring" leest minder duidelijk.
- **C. Helemaal geen lava in ring 2, ook niet onder −54.** Het sterkste thema, maar de bodem van de wereld wijkt af van alle andere ringen en vanilla. Iets meer code (ook de globale kiezer).

Antwoord: **A** — geen lokale lava: aquifer-lava boven Y −54 wordt water (`RingFluidPicker.waterOnly`), geen lavameren in de ring 2-biomes; de vanilla-lavalaag onder −54 blijft.

### 4. Hoe ontstaan de zandgrotten?
- **A. Surface rule: in zandzones (eigen 2D-noise) krijgen de plafonds (2–3 dik) en vloeren van noise-grotten zand (aanbevolen).** Alleen JSON (een tak in de surface rule + een noise-bestand). Een pure functie van positie, dus volledig deterministisch, met nul buurchunk-afhankelijkheid. Carvers die erdoor snijden, geven vanzelf extra hangend zand. Werkt alleen op noise-grotten (vraag 1 A/B).
- **B. Eigen carver `sand_cave`**: gangen en kamers die de carver zelf uitsnijdt en met een zandplafond bekleedt. De vorm is volledig in eigen hand en het werkt ook zonder noise-grotten. Deterministisch per seed (carvers lezen geen features). Wel een Java-carver (`WorldCarver`-subklasse + config + registratie) met een deels gekopieerde vanilla-ellipsoïdelogica: duidelijk meer werk.
- **C. Eigen feature** (een zandkamer per chunk). Flexibel, maar een feature moet de wereld lezen (waar is de grot?) en wordt dan afhankelijk van de generatievolgorde. Afgeraden.

Antwoord: **A** — surface rule: in zandzones (eigen 2D-noise) krijgen plafonds (2–3 dik) en vloeren van noise-grotten zand (plafond = `loose_sand`, vloer = vanilla-zand; zie vraag 5).

### 5. Wat laat een zandplafond instorten?
- **A. Alleen vanilla: het zand hangt tot een block update (graven, plaatsen, explosie, water), dan stort het hele aaneengesloten plafond als kettingreactie in. Vallende stof-particles waarschuwen (aanbevolen).** Geen code, past binnen `worldgen`, voorspelbaar in multiplayer (server-side vanilla). De speler veroorzaakt het zelf door te graven, wat eerlijk voelt.
- **B. Eigen "los zand"-blok dat ook vanzelf instort** (bijv. als er een speler onder loopt, of via random ticks). Echte valstrikken, meer horror. Maar het is blokgedrag (`blocks`): in deze change vanilla-zand als placeholder, het blok in een latere `blocks`-change, en daarna de surface rule omzetten. Random ticks of speler-detectie kosten servertijd.
- **C. Vooraf ingestort**: zandhopen en half ingezakte plafonds als decor, het zand ondersteund door zandsteen. Geen gevaar, geen lag, maar ook niet "instortend".

Antwoord: **B (keuze gebruiker)** — eigen los-zandblok `corruptedadventure:loose_sand`, gebouwd in **change 21** (`blocks`): ziet eruit als zand, valt vanilla-gewijs bij graven/buur-updates én stort na een korte waarschuwing (stof, kraakgeluid) in als een speler binnen ~2 blokken eronder loopt (server-side speler-check + scheduled tick, geen random ticks; mobs triggeren niet — vervolgantwoord gebruiker 2026-10-01). Deze change zet het in de surface rule als plafondmateriaal van de zandgrotten.

### 6. Hoe groot en waarvan zijn de zandgrotten?
- **A. Zones op ~25 % van het ring 2-oppervlak, Y 0 tot onder het oppervlak, plafond 2–3 dik, vloer 1 dik, alleen vanilla-zand (aanbevolen).** Vaak genoeg om ze tegen te komen, en een kettingreactie blijft beperkt (honderden blokken, niet duizenden).
- **B. Groter en dikker: zones ~40 %, plafond 4–6.** Spectaculaire instortingen, maar meer kans op lag-pieken en dodelijke verrassingen. Graven in ring 2 wordt echt riskant.
- **C. Zeldzaam en dun: zones ~10 %, plafond 1.** Vooral een waarschuwing en nauwelijks gevaar. Mensen merken het misschien niet.
- **D. Als A, maar zand gemengd met grind** (ruis in de surface rule). Meer variatie, en grind levert vuursteen. Grind past minder bij "zandgrot" en valt hetzelfde.

Antwoord: **A** — zones op ~25 % van het ring 2-oppervlak, Y 0 tot onder het oppervlak, plafond 2–3 dik (`loose_sand`), vloer 1 dik (vanilla-zand); geen grind.

### 7. In welke ring 2-biomes komen zandgrotten?
- **A. Alle ring 2-biomes (nu `canyonlands` en `oasis_flats`, straks ook de biomes van 30/31) (aanbevolen).** Overal in ring 2 kan het plafond onbetrouwbaar zijn: één regel voor de hele ring. Nieuwe biomes moeten in de `biome_is`-lijst (afspraak in het design-doc).
- **B. Alleen de zandbiomes** (`oasis_flats`, later `dune_sea`). Thematisch strak (zand boven, zand onder). Canyons houden stevige stenen grotten, en daar zijn ze minder vaak.
- **C. Overal in ring 2 via een eigen surface-rule-conditie "in ring 2"** (Java, `Registries.MATERIAL_CONDITION`, via `RingColumnCache`) in plaats van een biome-lijst. Geen lijst om bij te houden en ook in de overgangszone, maar een nieuw registry-type plus code.

Antwoord: **A** — alle ring 2-biomes (nu `canyonlands`, `oasis_flats`; 30/31 vullen de `biome_is`-lijst aan).


### 8. (bouw, 2026-10-02) Wat doen we met de vanilla-carvers die hangend zand maken?
Gemeten bij de bouw (seed 20260930, 200 ring 2-chunks, met en zonder carvers): alle hangende zand-, rood-zand- en grindblokken boven Y 60 komen van de vanilla-carvers (cave tot Y 180, canyon tot Y 67). Dat zijn ~10 per oasechunk en 0,3–0,7 per kloofchunk, plus soms een zwevend brokje. Zonder carvers: 0. Op het hangende `loose_sand` van de zandgrotten hebben de carvers bijna geen invloed.
- **A. Carvers in ring 2 alleen diep (aanbevolen):** eigen kopieën van vanilla's cave / cave_extra_underground / canyon met start-Y rond 40. Het oppervlak blijft schoon en diep snijden ze nog door de zandgrotten. Er komen minder carver-grotmonden in kloofwanden en oase; de noise-grotten en hun ingangen blijven.
- **B. Accepteren** zoals in vanilla-woestijnen.
- **C. Geen vanilla-carvers in ring 2:** alleen noise-grotten, geen ravijnen.

Antwoord (keuze gebruiker 2026-10-02): **A**. Carvers in ring 2 alleen diep: `ring_2/cave`, `ring_2/cave_extra_underground`, `ring_2/canyon` met start-Y ≤ 40 (`Ring2Carvers`). Gemeten: 0 hangende blokken boven Y 60.

### 9. (bouw, 2026-10-02) Zandplafonds boven reservoirs storten al bij het laden in
Gemeten bij de bouw: zandplafonds boven aquiferwater zakken in de eerste minuut na het laden deels in het water (oasegebied 0/−2.500: 1.518 → 542 hangende blokken in 60 s; kloofgebieden ongewijzigd). Vanilla geeft aquiferwater aan celgrenzen fluid-updates; dat water stroomt bij FULL en geeft het zand erboven een buur-update, en water telt voor vallende blokken als vrij.
- **A. Accepteren (aanbevolen):** vanilla-gedrag en geen werk. Boven reservoirs vind je vaak een al ingezakte zandgrot; droge zandgrotten blijven intact tot er een speler komt.
- **B. Minder reservoirs onder zandzones:** een floodedness-aftrek in de zandzones (zelfde 2D-ruis).
- **C. Geen plafondzand boven water:** kan niet met surface rules; Java nodig.

Antwoord (keuze gebruiker 2026-10-02): **A**. Vastgelegd als valkuil in `CODEBASE.md` en als W-punt in de checklist.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-02 | worldgen | Basisgrotten ring 2: vervallen hier (gebouwd in change 23, vraag 4 A); grotten alleen in `final_density`, nooit in de initiële dichtheid | Eén plek voor de basisgrotten; anders vindt het preliminary surface grotplafonds en kloppen aquifers/surface rules niet |
| 2026-10-02 | worldgen | Waterreservoirs ring 2: hogere floodedness in een diepteband via `ring_blend` (+0,65 van Y −40 tot 30, naar 0 bij Y 60 en −54; gemeten 26 % water in de holtes, ring 1 11 %); meelopen in de ring 1-helft van zone 1→2 geaccepteerd | Echte vanilla-aquifers, puur JSON, deterministisch; +0,4 gaf maar 5,6 % (ring 2 heeft nergens een preliminary surface onder ~Y 55, dus vanilla's drempels 0,8/0,4) |
| 2026-10-02 | worldgen | Router-`erosion` via `ring_blend`, ring 2 = 0 | Vanilla's deep-dark-check (erosion < −0,225, depth > 0,9) zou de aquifer onder ~Y 0 in een derde van ring 2 uitzetten; ring 1 blijft exact vanilla |
| 2026-10-02 | worldgen | Lava in ring 2: geen lokale lava boven Y −54, geen lavameren en geen lavabronnen (`spring_lava`); vloeistofregels per ring staan in `RingFluidPicker` (`noFluid`, `waterOnly`) | Past bij "water onder de grond", contrast met ring 3; lava kan niet via de router (rastercoördinaten) |
| 2026-10-02 | worldgen | Zandgrotten via een surface-rule-tak (2D-zone-noise `sand_cave_zone` ≥ 0,21 = 25 % van ring 2, `stone_depth` plafond/vloer) | Pure functie van positie; geen placements die buurchunks of de generatievolgorde lezen |
| 2026-10-02 | worldgen | Instorten van zandplafonds (keuze gebruiker): plafonds van `loose_sand` (change 21) die bij graven/updates én bij een speler (geen mob) eronder instorten, met korte waarschuwing | Echte valstrikken; blokgedrag in `blocks`, worldgen zet alleen het blok |
| 2026-10-02 | worldgen | Omvang en materiaal zandgrotten: zones ~25 %, plafond 2–3 `loose_sand`, vloer 1 vanilla-zand | Vaak genoeg om tegen te komen; kettingreacties blijven beperkt |
| 2026-10-02 | worldgen | Zandgrotten in alle ring 2-biomes (`biome_is`-lijst); nieuwe ring 2-biomes komen in die lijst en krijgen `Ring2Biomes.generation` (diepe carvers, geen lokale lava) | Eén regel voor de hele ring |
| 2026-10-02 | worldgen | Vanilla-carvers in ring 2 (keuze gebruiker, optie A): eigen kopieën van `cave`, `cave_extra_underground`, `canyon` met start-Y ≤ 40 (kans meegeschaald: onder Y 40 even veel gangen als vanilla) | De carvers hollen ná de surface rules uit: ~10 hangende zandblokken per oasechunk en hangend rood zand in de kloven; diep snijden ze nog door de zandgrotten |
| 2026-10-02 | worldgen | Vanilla-disks in ring 2 chunk-lokaal (`ring_2/disk_*`: origin ≥ 6 van de chunkrand) | Vanilla's disks schrijven tot 6 blokken in een buurchunk; de laadvolgorde bepaalde dan of de oeverplanten gras of zand zagen (oorzaak van de `oasis_vegetation`-verschillen uit change 25) |
| 2026-10-02 | worldgen | Zandplafonds boven reservoirs die na het laden al in het water zakken (keuze gebruiker, optie A): accepteren | Vanilla-mechanisme (fluid-updates van aquiferwater); droge zandgrotten blijven intact tot er een speler komt |

Commit (na `/code-review`): `worldgen: add ring 2 water reservoirs and collapsing sand caves`.

---

## Bouwnotities (change 26, 2026-10-02)

Aannames gecontroleerd tegen de code na change 25 (`963aabb`). A1: de basisgrotten zitten in change 23; de indeling is sinds 23b `ring_2/overworld.json` = `canyon_details` om `stack`, met de grotten in `stack`, en het preliminary surface komt uit `initial_terrain`. A2: `canyonlands`/`oasis_flats`, `Ring2Biomes.generation`. A3: de ring gate bestaat. A4: `loose_sand` bestaat en staat in `#overworld_carver_replaceables`. A5: klopte. De surface rule had sinds 23 vijf top-level elementen; de zandgrot-tak is nu [2], vóór de twee lagenblokken. Tijdelijke hulpmiddelen zijn niet gecommit: een dev-commando `/cadev dump|zone|df` en een `DevLogPlacement` in `dev/`, scripts in `build/c26/` en een git-worktree van `963aabb` in `../ca-head` voor de vóór-metingen.

**Afwijkingen van het plan (implementatie; punten 1–5 akkoord van de hoofdsessie/gebruiker 2026-10-02):**
1. **`waterOnly(dimension, position, y)`** in plaats van `waterOnly(position, y)`. De `ring_noise`-generator draait ook in de Nether, en daar is Nether-ring 2 ook `RING_2`: zonder dimensie werd de lavazee daar water.
2. **Floodedness-band +0,65** (startwaarde +0,4) van Y −40 tot 30, naar 0 bij Y 60 en bij Y −54. Met +0,4 stond maar 5,6 % van de holtes vol (ring 1: 10,9 %). De aanname "dicht onder het preliminary surface wordt de floodedness vanzelf lager" klopt niet: die demping bestaat alleen als er een preliminary surface onder ~Y 55 in de buurt is, en in ring 2 gelden daardoor overal de hoge drempels 0,8/0,4 (valkuil in `CODEBASE.md`). Gemeten bij +0,6 / +0,65 / +0,7 / +0,8: 23 / 28 / 33 / 38 %; doel 2–3× ring 1.
3. **Router-`erosion` via `ring_blend`** (`ring_blend/overworld_erosion`, ring 2 = 0). Vanilla's deep-dark-check (`erosion` < −0,225 en `depth` > 0,9) zet anders de aquifer onder ~Y 0 in een derde van ring 2 uit. Router-erosion gebruiken alleen het klimaat (ring 2 kijkt alleen naar `temperature`; zone 1→2 = `withered_expanse`) en de aquifer; ring 1 blijft exact vanilla.
4. **Ook `spring_lava` uit ring 2** (alleen `spring_water` blijft), anders klopt het meetdoel "0 lava boven Y −54" niet. Ring 2 gebruikt daarom een eigen variant van `globalOverworldGeneration` (`TransitionBiomes.globalOverworldGeneration` is nu private).
5. **Zandzone-drempel 0,21** (startwaarde 0,25 gaf 21,5 % van ring 2; 0,21 geeft 25,3 %).
6. **Zandgrot-tak vanaf `y_above` 8** (plan: 0), boven de deepslate-gradiënt (Y 0–8); de condities staan in de volgorde stone_depth → zone → biome (goedkoop eerst). Plafond en vloer hebben elk hun eigen keten.
7. **Carvers (vraag 8 A):** `Ring2Carvers` leest vanilla's carvers via `Carvers.bootstrap` op een opnemende context, zet de bovengrens van de start-Y op 40 en schaalt de kans met het behouden startbereik (review). `floorLevel` is package-private en wordt via `ObfuscationReflectionHelper` gelezen; de hele configuratie via de codec encoderen kan in datagen niet, want de blok-tag is dan nog niet gebonden.
8. **Determinisme `oasis_vegetation` (extra taak, oorzaak gevonden).** Met een tijdelijke placement modifier die de kandidaten van `oasis_vegetation` logt, in twee verse werelden in omgekeerde laadvolgorde: in chunk 16, 130 zag kandidaat 4 (257, 63, 2081) in de ene wereld zand onder zich en in de andere gras; in chunk −2, −159 gold hetzelfde voor kandidaat 3 (−30, 64, −2535). Het zand komt van vanilla's `disk_sand` uit een buurchunk: disks schrijven tot hun straal (6) in de buren, en of de buur eerder of later decoreerde, bepaalde wat het filter "gras eronder" zag. Omdat vanilla's placement-stream lui is (per kandidaat eerst modifiers, dan de feature, met één random), schoven daarna alle volgende kandidaten van die chunk op: 9–12 planten op andere plekken. Hetzelfde gebeurde bij suikerriet (zand ↔ grind van twee overlappende disks). **Fix:** ring 2 gebruikt vanilla's configured disks met een chunk-lokale origin (`ring_2/disk_sand|clay|gravel`: `count` 3/1/1 + `jittered_grid` 16 met marge 6 = grootste straal), zodat een disk alleen in zijn eigen chunk schrijft. `oasis_vegetation` zelf is niet veranderd. Resultaat in de determinismetest: oeverplanten, suikerriet, palmen en `oasis_vegetation` blok voor blok gelijk.
9. **Review-fixes (zie Code review):** omgezet water krijgt vanilla's update-vlag in plaats van altijd een update; op Y −54 (direct boven vanilla's lavalaag) blijft aquifervloeistof in ring 2 vast (één blok steen); de carverkans schaalt mee met het behouden startbereik.
10. **Ring 1-ertsmeting / decoratie:** door de nieuwe placed features (disks) verschuift vanilla's feature-index in `UNDERGROUND_ORES`. In ring 1 liggen ertsen, lush-cave-klei/mos en bamboe daardoor anders (bekend, valkuil "Feature-volgorde en ring 1-decoratie"); steen, deepslate, water en lava blijven op enkele blokken na gelijk.

### Testresultaten

- **B:** `./gradlew build` groen (55 testklassen, 263 tests). Nieuw/uitgebreid: `RingAquiferTest` (ring 2-lava → water met vanilla's update-vlag, stenen deksel op Y −54 (ook voor vanilla-water), lavalaag onder −54 blijft, ring 1/3–5 en Nether houden lava, wissel op `ring()`, update-vlag blijft niet hangen), `RingFluidPickerTest` (`waterOnly`, `noFluid` ongewijzigd), `Ring2BiomesTest` (geen lavameren/-bronnen, wel `spring_water`, carvers = `Ring2Carvers`, chunk-lokale disks i.p.v. vanilla's), `Ring2DisksTest` (straal ≤ marge), `Ring2CarversTest` (vanilla-JSON op de start-Y en de geschaalde kans na; `keptShare`).
- **D:** `./gradlew runData`: `canyonlands.json`/`oasis_flats.json` (geen lavameren, geen `spring_lava`, `ring_2/*`-carvers en -disks), nieuw `configured_carver/ring_2/{cave,cave_extra_underground,canyon}`, `placed_feature/ring_2/disk_{sand,clay,gravel}`.
- **S:** rooktest `tools/server-smoke-test.sh` met `level-name=smoke-change26`, preset `corrupted_world`, seed 20260930: `Done (9.137s)`, geen ERROR/WARN van de mod, geen "Feature order cycle", geen codecfout, geen client-class (dedicated server). Metingen via RCON + dumps (14 gebieden van 5 × 5 chunks: ring 1 N/O/Z/W op ~1.000, ring 2 N/O/Z/W op ~2.500, kloof 2.352/512, tafelbergen 2.544/624, plateau met spleten 2.400/−608, oase 224/2.016, zone 1→2, ring 3; vóór = worktree `963aabb`):
  - **Reservoirs:** water in de holtes (Y −54…62, alleen onder een dak; na 45 s uitzakken) ring 2 **1,8 % → 25,6 %** (200 chunks), ring 1 11,0 % → **2,3×**. Per gebied 1–76 %: veel in de oase, weinig in het lage kloofgebied 2.352/512 (daar is de floodedness-ruis zelf laag). Water onder een dak op Y 60–62: 185 → 100 blokken; open water ≤ Y 62 (poelen) 7.593 → 7.544. Zone 1→2: 23,5 % → 71 % (meelopen, geaccepteerd). Afstemming (zie punt 2): +0,6 → 23 %, +0,7 → 33 % (met de eerdere, dichtere carvers).
  - **Lava:** ring 2 boven Y −54 **0** (vóór 492 in 200 chunks); de lavalaag onder −54 blijft. In één kloofgebied (2.384 −55 530) een obsidiaankorst van 57 blokken waar stromend water (vanilla-bron/afvloeiing) na het laden op de lavalaag viel: vanilla-gedrag, in beide laadvolgordes gelijk.
  - **Zand:** zandzones 25,3 % van ring 2; los zand in 61 van de 200 ring 2-chunks (11.984 blokken), plafonds Y 8–58 (onder het preliminary surface op de kloofbodem); vloerzand alleen in de zones. Hangende zand-/rood-zand-/grindblokken boven Y 60: vóór 535, na **0**.
  - **Instorten** (de quick-play-client `Dev` joinde automatisch; eindcode): onder het grootste plafond (1.548 blokken, 775 hangend; `Dev` op 7 31 2510, plafond Y 33) was het blok boven `Dev` na **1,01 s** weg en vielen 548 blokken (de rest rust op steen of was niet ondergraven), tick P99 18,9 ms (in een eerdere ronde 12,9 ms). NoAI-zombie onder een plafond (−12 14 2537): plafond na > 400 ticks intact, zombie nog op zijn plek. Een plafond zonder iets eronder (−7 42 2518): intact. `setblock` lucht in een plafond (5 50 2486): 268 van 329 blokken vielen, P99 29,1 / 52,6 / 57,2 ms in drie rondes (één tick net boven 50 ms; gemiddeld 4,8–8,3 ms). Boven reservoirs zakt een deel al na het laden in (vraag 9, geaccepteerd).
  - **Ring 1 ongewijzigd:** steen/deepslate/water/lava in de 4 ring 1-gebieden gelijk op enkele blokken na (zelfde laadmoment); in het jungle/lush-gebied liggen 1.660 blokken steen anders door de verschoven decoratie (punt 10).
  - **Ertsmeting na de grotten** (per 10.000 gastgesteente, ring 2 na ÷ vóór op hetzelfde terrein, 200 chunks): kolen 0…63 1,14; ijzer −64…−1 0,93, 0…63 0,96; koper −64…−1 0,75 (n 450), 0…63 1,05; goud 1,06 / 1,25 (n 393); redstone 1,03 / 1,31 (n 360); lapis 1,01 / 1,00; diamant −64…−1 1,02; zonnesteen 64…127 1,13, 128…191 0,93; turkoois 1,00. Water in plaats van lucht vermindert de air-exposure-discard (kolen, goud, redstone iets hoger); de afbouwfactoren van change 25 blijven ongeveer gelden.
  - **Determinisme** (eindcode): twee verse werelden met alle 14 gebieden, de tweede omgekeerd en chunk voor chunk geladen, elk gebied 45 s laten uitzakken. Ring 2 (8 gebieden, 200 chunks): blok voor blok gelijk op vanilla's ondergrondse klodders en ertsen na (graniet/dioriet/andesiet/tufsteen, aarde/grind, kolen), plus 2 waterstatussen. **`oasis_vegetation`, oeverplanten, suikerriet, palmen, disks, water en `loose_sand` identiek** (de oorzaak uit change 25 is weg). Ring 3 25/25 identiek. Zonder uitzaktijd verschillen alleen stromend water en vallend `loose_sand` (tijd, niet volgorde). Zone 1→2: daarnaast 17 blokken obsidiaan ↔ lava op de lava/water-naad (risico uit het plan, geaccepteerd). Ring 1 laat vanilla's bekende volgorde-afhankelijkheid zien (gras, bladeren, kelp, bamboe).
  - **Prestaties:** zie `CODEBASE.md` (meting change 26). Noise-stap oase 1,74×, kloof 0,91× vanilla; vóór in dezelfde sessie 1,81× / 1,00×. Change 26 kost dus niets meetbaars (budget ≤ 1,75×; de sessie-spreiding is groter dan de marge).
- RCON stond alleen tijdens de tests aan (via het testharnas, dat `run/server.properties` steeds terugzet); `run/server.properties` staat op `level-name=smoke-change26` met RCON uit.

### Code review (`/code-review medium`)

Drie bevindingen, alle drie opgelost:
- **Medium — omgezet water direct op de lavalaag:** een lokale lavacel die tot in de lavalaag reikt, werd boven Y −54 water en eronder lava, zonder barrière, met obsidiaan en water in de lavalaag als gevolg. Fix: op Y −54 blijft aquifervloeistof in ring 2 vast (één blok steen). Dat geldt ook voor vanilla-water, dat vanilla de lavalaag wel laat raken. Gemeten: 0 contactplekken aquiferwater/lava; de enige obsidiaankorst (57 blokken, één kloofgebied) komt van stromend water na het laden (vanilla).
- **Laag — carverdichtheid:** alleen de bovengrens verlagen propte vanilla's hele aantal gangen in het lage bereik (cave ~2,4×, canyon ~1,9× onder Y 40). Fix: de kans schaalt met het behouden startbereik (cave 0,15 → 0,061, extra 0,07 → 0,065, canyon 0,01 → 0,0053). Daarna water in de holtes 25,6 % (was 28,4 % met de dichtere carvers).
- **Laag — elke omgezette cel kreeg een fluid-update:** duizenden zinloze waterticks per chunk bij het laden. Fix: omgezet water houdt vanilla's update-vlag (alleen bij celgrenzen). Dat wijkt af van plan stap 2 ("`shouldScheduleFluidUpdate` geeft `true`"); vanilla's vlag hangt niet van het vloeistoftype af, dus de celgrenzen die stromen nodig hebben, krijgen hun update nog.
Build, runData, rooktest, determinisme en de instortingstest zijn daarna opnieuw gedraaid (resultaten hierboven zijn van de eindcode).

## In-game checklist (W) — voor teststop 2b (na change 30; overnemen in plan 30)

runClient, nieuwe wereld **Corrupted World**, seed `20260930`, creatief/spectator om te kijken, survival voor het instorten. Coördinaten x y z (y = waar je staat).
- [ ] **Reservoirs:** grote grotmeren bij 39 −23 −2536 (oppervlak Y −24, ~600 blokken), −2534 −20 29 en 269 −20 2027; lokale meren bij 38 34 −2535 (Y 33), −9 17 2472 en 2430 17 −549 (Y 16). Leest het als "het water zit onder de grond"? Te veel of te weinig (nu 26 % van de holtes)?
- [ ] **Geen lava** tussen Y −54 en −10 (rondvliegen rond 0 −30 −2500 en 2400 −30 560); de lavalaag onder −54 is er nog, met een stenen laag op Y −54 waar er water boven staat.
- [ ] **Zandgrotten:** plafond van los zand met een zandvloer bij −22 10 2538 en −8 14 2527 (oase, zuid), −2511 25 −26 (kloof, west), 2394 15 590 (kloofgebied); stof-particles onder het plafond. Niet aan het oppervlak, niet in de deepslate.
- [ ] **Survival-instorting:** ga staan op 7 31 2510 (onder het grootste plafond, ~1.550 blokken): kort stof en gekraak, dan een golf. Blijf staan en kijk of je bedolven raakt. Hak bij 5 47 2486 één plafondblok (Y 50) weg en kijk in F3 of `tick query` of de server hapert (in de RCON-test één tick van ~55 ms). Geen merkbare lag (F3 / `tick query`). Een creeper in een zandgrot (spawn er een) doet hetzelfde.
- [ ] **Zandgrot boven een reservoir:** 39 32 −2479 en 31 20 2468. Vaak is het zand na het laden al in het water gezakt (vraag 9 A, geaccepteerd): leest dat als een ingestorte zandgrot? Geen rare vloeistofmuren?
- [ ] **Oppervlak:** geen hangend zand of rood zand meer boven grotgangen in oase en kloof (carvers alleen diep); er zijn minder carvergrotmonden in de kloofwanden en geen ravijnen boven Y ~40. Ondergronds even veel carvergangen als in ring 1?
- [ ] **Overgang 1 → 2** (`/ca tpboundary 1`), onder de grond: geen harde naad in grotten of water, hoogstens een stukje steen/obsidiaan waar lava en water elkaar raken.
- [ ] **Ring 1 en ring 3 onder de grond:** ongewijzigd (vanilla-grotten en -lava in ring 1).
- [ ] **Oase-oevers:** planten en suikerriet zoals in 23c (geen kale strook aan het water door de chunk-lokale disks).
- [ ] Standaard-W: midden van elke ring en elke grens (overworld en Nether), portal vanuit elke ring; geen nieuwe fouten in de log.
