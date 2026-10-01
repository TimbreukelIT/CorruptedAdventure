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
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

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
- **Zand** in ring 2, Y 0…(preliminary surface − 8): `corruptedadventure:loose_sand` en `minecraft:sand` > 0 in de zandzones, 0 daarbuiten (de bands boven Y 127 meetellen: plateaus tot Y ~140). Steekproef: plafond = `loose_sand` met lucht eronder (`execute if block … corruptedadventure:loose_sand` + `if block ~ ~-1 ~ air`), vloer = `minecraft:sand`; geen `loose_sand` buiten plafonds.
- **Instorting — RCON-test van `loose_sand` in een echte zandgrot** (geforceloade chunk; scheduled ticks en entities lopen daar, random ticks zijn niet nodig):
  1. *Speler-trigger:* de quick-play-client `Dev` (change 18) joint automatisch; een gemeten zandgrot-plafond zoeken (steekproef hierboven), RCON `gamemode survival Dev` + `tp Dev <x> <y> <z>` op de grotvloer eronder (bovenkant hoofd ≤ 2 blokken onder het plafond) → binnen ~60 ticks is het plafond boven de speler weg (`execute if block … air`) en ligt er zand op de vloer; de kettingreactie neemt het aaneengesloten plafond mee (tellen met `fill … glass replace corruptedadventure:loose_sand` vóór en na). Lukt de automatische client-join niet: deze stap naar W-achteraf.
  2. *Controle:* een ander plafond met een `summon`-de zombie (`NoAI:1b`) eronder en een derde zonder iets eronder hangen na 200 ticks nog (mobs triggeren niet).
  3. *Vanilla-pad:* een plafondblok met `setblock … air` weghalen → kettingreactie, ~200 ticks.
  4. `tick query` tijdens 1 en 3: geen MSPT-piek boven 50 ms.
  Werkt een stap via RCON toch niet, dan in-game (W).
- **Ring 1 ongewijzigd**: water en lava in 5×5 ring 1-chunks zijn gelijk aan dezelfde chunks vóór deze change (floodedness ring 1 = vanilla-definitie).
- **Determinisme**: twee verse werelden met dezelfde seed, de tweede in omgekeerde volgorde geladen. Per chunk dezelfde aantallen water, lava en zand.
- **Performance** (JFR, `minecraft.ChunkGeneration`, stap `noise`, zoals bij change 9): ring 2 vóór en na change 26, budget 1,3× vanilla. Leg de getallen vast in `CODEBASE.md`. Zonder ring gate (A3) ook ring 3 meten, want die betaalt dan mee.

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
- **Blokkerende handmatige test: nee.** Alles waar latere changes op bouwen, is automatisch te toetsen via build, serverstart en RCON-tellingen: het vloeistofgedrag per ring (change 27 rekent op water in ring 2, change 42/62 breiden `waterOnly`/floodedness uit), de surface-rule-tak met biome-lijst (changes 30/31), het determinisme en de instorting via de quick-play-speler (`tp`) en `setblock`. Het speelgevoel (hoe het eruitziet, hoe eng het voelt) is afstemming. → **W-achteraf**, in-game checklist (seed 20260930, preset `corrupted_world`, spectator en survival):
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

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Basisgrotten ring 2: vervallen hier (gebouwd in change 23, vraag 4 A); grotten alleen in `final_density`, nooit in de initiële dichtheid | Eén plek voor de basisgrotten; anders vindt het preliminary surface grotplafonds en kloppen aquifers/surface rules niet |
| 2026-10-01 | worldgen | Waterreservoirs ring 2: hogere floodedness in een diepteband via `ring_blend`; meelopen in de ring 1-helft van zone 1→2 geaccepteerd | Echte vanilla-aquifers, puur JSON, deterministisch |
| 2026-10-01 | worldgen | Lava in ring 2: geen lokale lava boven Y −54, geen lavameren; vloeistofregels per ring staan in `RingFluidPicker` (`noFluid`, `waterOnly`) | Past bij "water onder de grond", contrast met ring 3; lava kan niet via de router (rastercoördinaten) |
| 2026-10-01 | worldgen | Zandgrotten via een surface-rule-tak (2D-zone-noise, `stone_depth` plafond/vloer) | Pure functie van positie; geen placements die buurchunks of de generatievolgorde lezen |
| 2026-10-01 | worldgen | Instorten van zandplafonds (keuze gebruiker): plafonds van `loose_sand` (change 21) die bij graven/updates én bij een speler (geen mob) eronder instorten, met korte waarschuwing | Echte valstrikken; blokgedrag in `blocks`, worldgen zet alleen het blok |
| 2026-10-01 | worldgen | Omvang en materiaal zandgrotten: zones ~25 %, plafond 2–3 `loose_sand`, vloer 1 vanilla-zand | Vaak genoeg om tegen te komen; kettingreacties blijven beperkt |
| 2026-10-01 | worldgen | Zandgrotten in alle ring 2-biomes (`biome_is`-lijst); nieuwe ring 2-biomes komen in die lijst en krijgen vanilla-carvers | Eén regel voor de hele ring |

Commit (na `/code-review`): `worldgen: add ring 2 water reservoirs and collapsing sand caves`.
