# Ring 5 golf 2 — stroom W-biomes: 94 en 95 (aankleding en horror-features ring 5)

Scope: `worldgen` — pakket **94** en daarna **95**, één stroom, achter elkaar op branch `ring5/94`, worktree
`P:/Projecten/Java/wt/r5-94`, poort n = 9 (server 25579, RCON 25589), basis = main `061a20c4`. Parallel: **93** (grotten +
gespiegelde dubbelgangers, W-terrein, `wt/r5-93`); 94/95 raken de W-terrein-bestanden niet.

Bronnen: `fase-1-wereld.md` → Ring 5 (blok "Sparring ring 5", herziening 6a/83b, "Horror-elementen", "Ores en
materialen"); `contract.md` §2, §3 (blokken van 88/89), §4 (feature-keys, vanilla-ertsen, DF-lezers), §8
(`Ring5SignText`); `waves.md` §1, §3, §5; `golf-1-biomes.md` (86/86b/86c); `golf-1-terrain.md` §4b (83b); de
afstemlus `docs/plans/ring-4/tuning-loop.md`; sjablonen ring 4: `Ring4Features.ORDER`, `Ring4Terrain`/`Ring4Filter`,
`GroundObjects`, `BlightedLandmarks`/`LandmarkPlan` (blikvangers over chunkgrenzen), `DeadTreeFeature`,
`Ring4OreFeatures`, `RingVanillaOres`, `NetherRing5DressingCondition` (surface per blok, 96).

**Bezit** (`waves.md` §1/§5): `Ring5Biomes` (generatie), `Ring5BiomeModifiers`, `Ring5Features` (+ nieuwe klassen in
`worldgen/feature/` met een `Ring5`-naam), `Ring5SignText`, de ring 5-tak van `noise_settings/overworld.json` via
`tools/worldgen/ring5_surface.py`, eigen surface-condities en feature-/placement-types in een blok
`// ring 5 — W-biomes` aan het eind van `ModSurfaceConditions`, `ModFeatures`, `ModPlacementModifierTypes`
(alleen toevoegen), eigen loot table(s) onder `data/corruptedadventure/loot_table/chests/ring_5/` (alleen als V6 dat
kiest), de tests van 94/95.
**Niet:** `Ring5Field`/`Ring5Inner`/`Ring5Outer`/`Ring5Castles`/`Ring5CorruptSpots`, `density_function/ring_5/**`,
`ring_blend/*`, `RingFluidPicker`/`RingAquifer` (W-terrein; 93 werkt er nu in), `ModBlocks`/blok-datagen (B),
`BiomeAmbience` (A), `MapPalette`, `CODEBASE.md`, `docs/design/*`, `docs/backlog.md` (INT; hier alleen in dit plan
onder "CODEBASE-delta", "Beslissingen", "Backlog"). Een extra accessor in `Ring5Field` vraag ik via main (§3.4).

---

## 0. Uitgangspunt (wat er al ligt)
- **Biomes** (86): 6 ring 5-biomes zonder features, carvers of biome modifiers; dus ook **geen vanilla-ertsen, -bloemen,
  -bomen, -meren** tot nu. Spawns: monsters vanilla, `false_meadow` niets.
- **Surface** (86b): `corrupted_grass_block`/`corrupted_dirt` (`corrupted_wastes`, bovenkant `inverted_hills`),
  `corrupted_stone` (`glitch_fields`, `void_scars`, `shattered_expanse`, basisgesteente overal boven Y 0–8), kuilbodems
  `corrupted_sand`, `false_meadow` vanilla gras/aarde, scheurwanden `void_scars` nog `deepslate` (vanilla `steep`), onderkant
  van de omgekeerde heuvels `corrupted_stone`.
- **Terrein** (83, 83b, 84): binnenband golvend Y ~66–116 met kammen tot ~145, trappen, kuilen, glitchkubussen en
  -lagen, losse scheuren tot de void; **corrupte plekken** ~elke 210 blokken (2–5 chunks verschoven ±8–30, **precies één
  weggevallen chunk** met een rafelrand van 1–3 tot onder `min_y`); buitenband schotsen, brokken, ruggen, springstenen,
  omgekeerde heuvels (r 12–26, 10–40 lang onder een schots), vier pinakels (r 40–56, top Y 120–140) met een verwoeste rand
  (8–16) en twee bruggen.
- **Lezers die bestaan** (`Ring5Field`, publiek, alleen lezen): `column(x, z)` (`top`, `bottom`, `breakup`, `castle`,
  `scars`, `inverted`, `height`, `glitch`, `isVoid`), `castleSites`, `castleAt`, `bridgesWithin`, `fragmentsWithin`,
  `breakupAt`, `solidGround`, `innerCrackDistance` (telt het gat van een corrupte plek mee); `Ring5Band.of(layout)`;
  `RingChunkGenerator.ring5Field(random)`.
- **Blokken** (88/89): de corrupted set + houtset (`corrupted_sign`/`_wall_sign`/`_hanging_sign`, `corrupted_fence`),
  `glitch_block` (`void_sources`), `voidstone` (`void_sources`, `void_mote`), `nullite_ore` (in `corrupted_stone`),
  `voidstone_nullite_ore`; tags `ring_5_stones`, `ores_in_ground/corrupted_stone`, `ores_in_ground/voidstone`.

## 1. Pakket 94 — binnenband, `false_meadow`, horror-features

| # | Onderdeel | Biomes | Route (implementatie) | Vraag |
|---|---|---|---|---|
| 94.1 | Dode bomen + bodemflora `corrupted_wastes` | `corrupted_wastes` (en spaarzaam `glitch_fields`) | feature `ring_5/dead_tree` (patroon `DeadTreeFeature`: grondobject, eigen random, eigen chunk), clusters via `jittered_grid` + `ClusterDensity`; flora/keien per chunk | V2 |
| 94.2 | Glitchblokken in het oppervlak | `glitch_fields` | **surface rule** (puur per blok; nieuwe conditie `ring_5_speckle`, hash per blok), ook op kubus- en laagranden | V3 |
| 94.3 | `false_meadow`: weide + gespiegelde bomen | `false_meadow` | vanilla gras/bloemen (eigen placed features, **geen bijenkorven**: geen mobs); bomen als paar-plan per cel (beide bomen uit één pure beslissing, elke chunk schrijft zijn eigen kolommen) | V4 |
| 94.4 | Ontbrekende chunks | binnenband | V1-A/C: feature `ring_5/missing_chunk` (de hele eigen chunk leeg tot onder `min_y`, chunk-lokaal); V1-B: geen eigen gat | V1 |
| 94.5 | Herobrine-tekens | binnenband | features `ring_5/herobrine_sign` (bord met vertaalbare tekst, `SignBlockEntity` in de eigen chunk) en `ring_5/torch_pattern` (redstone-fakkels); teksten in `Ring5SignText` (en + nl) | V5 |
| 94.6 | Verlaten kampen | binnenband | feature `ring_5/abandoned_camp` (≤ 9 × 9, op vlakke droge grond in één chunk: `GroundObjects.fit`) | V6 |
| 94.7 | Herinneringen | volgens V7 | features `ring_5/memory_{oak_grove,oasis,lava_field,holed_islet}`: puur plan per wereldcel (patroon `BlightedLandmarks`), elke geraakte chunk schrijft alleen zijn eigen kolommen; vormen uit bestaande blokken van ring 1–4, deels vervangen door de corrupted varianten | V7 |

Gemeenschappelijk voor 94:
- **Waar niet:** nooit binnen 6 van een void-kolom (scheur, gat; `innerCrackDistance`), niet op een verschoven chunk van
  een corrupte plek (harde wand; filter op de hoogtesprong van de bevroren heightmap), niet in zone 4→5
  (`sundered_reach` is ring 4) en niet binnen 32 van de bandlijn als de feature vaste grond nodig heeft.
- **Determinisme** (regels 23c/35, `waves.md` §7): alle plaatsing uit wereldseed + positie + salt, geen decoratie-random
  over chunkgrenzen; features die over chunkgrenzen reiken (gespiegelde bomenparen, herinneringen) zijn pure plannen.
- **Featurevolgorde:** één globale lijst `Ring5Features.ORDER` (zoals ring 4), per biome een deelverzameling.
- **`corrupted_leaves`** altijd `persistent=true` (backlog r. 466).

## 2. Pakket 95 — buitenband, kasteelomgeving, ertsen, omgekeerde bomen

| # | Onderdeel | Biomes | Route | Vraag |
|---|---|---|---|---|
| 95.1 | Voidsteen-randen en scheurwanden | volgens V8 | **surface rule** (nieuwe conditie `ring_5_void_edge`: afstand tot de dichtstbijzijnde void-kolom via `Ring5Field.column(..).isVoid()`, gecachet per kolom per thread, zoals 96); vervangt de `steep`-regel van `void_scars` (backlog r. 473) | V8 |
| 95.2 | Kasteelomgeving: verwoeste grond | `void_scars` (pinakel) | surface rule op `castles` (`Ring5Output`/`column.castle`) + feature `ring_5/castle_rubble` (puin, obsidiaan-accenten) buiten het plateaumidden | V9 |
| 95.3 | Omgekeerde bomen + aankleding schotsen | `inverted_hills`, `void_scars`, `shattered_expanse` | feature `ring_5/inverted_tree` (hangt aan de onderkant van een omgekeerde heuvel: begin = `column.bottom`, groeit omlaag), dode bomen op schotsen (94.1 hergebruikt) | V10 |
| 95.4 | Nulliet | buitenband + diep binnen | eigen ore-features (`nullite_ore` in `corrupted_stone`, `voidstone_nullite_ore` in voidsteen), placement `ring_5_void_face` (alleen kandidaten met void binnen 2 blokken: scheurwanden, schotsranden, brokonderkanten) + diep in de binnenband | V11 |
| 95.5 | Glitchblokken als erts | volgens V3 | ore-feature `ring_5/glitch_cluster` (target `ring_5_stones`) | V3 |
| 95.6 | Vanilla-ertsen afgebouwd | heel ring 5 | `RingVanillaOres.placed(…, RING_5, factors(0,05, 0,03, –))` + biome modifier `add_ring_5_ores`; diamant ×0 = niet registreren (kleine uitbreiding: een factor 0 slaat een erts over) | V11 |

## 3. Techniek

### 3.1 Feature of surface rule
- **Surface rule** voor alles wat alleen "een ander blok op een plek die puur uit de vorm volgt" is (glitchvlekken,
  voidsteen-randen, scheurwanden, verwoeste grond): per blok puur, geen chunkgrensprobleem, `noise` ongewijzigd
  (les 96). Kosten zitten in de surface-stap (gemeten in `perf.sh`, kolom-cache per thread).
- **Feature** voor objecten met vorm en blokentiteiten (bomen, bordjes, fakkels, kampen, herinneringen, puin, ertsen).
- Grond en hoogte lezen features uit de **bevroren WG-heightmaps** en de `Ring5Field`-accessors, nooit uit een eigen
  schatting (les 85/86c). Een nieuwe lezer wordt in een S-smoke gecheckt (valkuil 86c, `HolderHolder`).

### 3.2 Nieuwe klassen (voorstel)
- `worldgen/feature/Ring5Terrain` — het `Ring5Field` per random state (zoals `Ring4Terrain`/`ZoneFractureTerrain`,
  zwakke cache, één WARN bij een misser).
- `worldgen/feature/Ring5Filter` — placement modifier `ring5_filter` (`inner_only`, `outer_only`, `crack_clearance`,
  `castle_clearance`, `band_clearance`, `max_step` tegen de wanden van de corrupte plekken).
- `worldgen/feature/Ring5InnerFeatures` (94), `Ring5HorrorFeatures` (94: tekens, kampen, gat), `Ring5Memories` +
  `MemoryPlan`/vormen (94), `Ring5OuterFeatures` (95), `Ring5OreFeatures` (95); feature-types in `ModFeatures`
  (`// ring 5 — W-biomes`): `herobrine_sign`, `torch_pattern`, `abandoned_camp`, `ring_5_memory`, `mirrored_trees`,
  `inverted_tree`, `castle_rubble` (+ `missing_chunk` als V1 dat kiest).
- `worldgen/surface/Ring5SpeckleCondition` (`ring_5_speckle`), `Ring5VoidEdgeCondition` (`ring_5_void_edge`).

### 3.3 Volgorde in de chunk
`RAW_GENERATION` (alleen `missing_chunk`, bij V1-A/C) → `UNDERGROUND_ORES` (nulliet, glitch, vanilla) →
`LOCAL_MODIFICATIONS` (herinneringen, kampen, puin) → `VEGETAL_DECORATION` (bomen, bloemen, flora) →
`TOP_LAYER_MODIFICATION` (bordjes, fakkels: ná de bomen, zodat niets ze overschrijft). Een gat dat zijn hele chunk
leegmaakt is deterministisch zolang niets van buiten de chunk blokken *toevoegt* in lucht (alleen ertsen reiken over de
rand, en die vervangen alleen gesteente).

### 3.4 Afhankelijkheden van W-terrein (via main)
- Waarschijnlijk geen nieuwe accessor nodig: de randen komen uit `column(..).isVoid()`, scheuren en gaten uit
  `innerCrackDistance`, kastelen uit `castleAt`/`column.castle`, omgekeerde heuvels uit `column.inverted`/`bottom`.
- Mogelijk nodig: een publieke vraag "ligt deze kolom in een corrupte plek / welke verschuiving" (`corruptSpot` is
  package-private). Eerst proberen met de heightmap-sprong; lukt dat niet netjes, dan een verzoek via main.
- **93 (grotten) loopt parallel:** features op de grond lezen de heightmaps, dus grotten die later bijkomen verschuiven
  hooguit een object; ertsen (95) kunnen in 93's holtes vallen of erdoor weggesneden worden. Na de merge van 93 op main
  rebase ik 95 vóór INT en herhaal ik de ertsscan en `det.sh`.

## 4. Client/server-impact
Alles is worldgen (server-side) en datagen; geen payload, geen `PROTOCOL_VERSION`. Bordjes: de server schrijft een
`Component.translatable`, elke client toont hem in zijn eigen taal (ook gemengd in multiplayer). Voidsteen-randen geven
meer `void_mote` (bestaande client-`animateTick`, vaste steekproef rond de speler: geen extra kosten per blok).
`glitch_block`/`voidstone` zijn `void_sources`: meer van die blokken = sneller stijgende void-meter (C, server) — dat is
een speelgevoel-gevolg, zie V3/V8. Geen client-class in de nieuwe code (`ClientIsolationTest`).

## 5. Tests en poorten (per pakket)
- **Unit:** plannen puur per seed (twee keer gelijk, andere seed anders), aandelen/afstanden per feature op seed
  20260930 (gemeten, in dit plan), "nooit binnen 6 van void", "nooit in zone 4→5", spiegelparen exact gespiegeld,
  herinneringen ≤ 30 en binnen de plekregels, `corrupted_leaves` persistent; surface-tak via `ring5_surface.py`
  (idempotent, rest van `overworld.json` byte-gelijk; de bestaande `Ring5BiomesTest`-asserts bijgewerkt); lang: elke
  tekst-key in en + nl.
- **B** `./gradlew build --offline` (incl. `ClientIsolationTest`); **D** `runData --offline`, output gecommit,
  `git diff --exit-code src/generated` na de commit.
- **S-smoke** `tools/test/server.sh start r5-94 --fresh` (seed 20260930), log 0 ERROR, geen client-class; `/ca ring`,
  `/place feature corruptedadventure:ring_5/<naam>` per feature, `/ca terrainmap 8900 0 1400 4` → `build/test/maps/94/`
  (95: `build/test/maps/95/`, ook rond elke `/ca castles`-plek); blokscan echte chunks (`gen.py` + `region.py`): tellingen
  per feature, blokken boven de void, `corrupted_leaves`-vervalcheck (geen bladeren > 6 van een log tenzij persistent).
- **Scans:** `voidscan.py --strict --rings` op `areas/ring5.txt` (0 fouten; bij V1-A/C moet de export de nieuwe gaten
  kennen), `det.sh` op `ring5.txt` (own 0), feature-index `fi3` ring 1–4 ongewijzigd, `perf.sh --areas
  tools/test/areas/perf-r5.txt` (noise ≤ 1,75; surface en features gerapporteerd).
- **Slots/locks:** `tools/test/slots.sh`, `--no-daemon`; bestaat `C:/Temp/ca-user-test.lock`, dan wachten.
- `/code-review medium` op `main...ring5/94` na 94, en opnieuw na 95.
- **Afstemlus** daarna: dichtheden en maten zijn datagen-parameters (`runData` ~25 s, kleine wereld met `world.sh
  --only`), de gebruiker kijkt lokaal bij 6b.

## 6. Wat 99b (textures) straks nodig heeft
Met de aanbevolen antwoorden **geen nieuwe blokken** (alles vanilla of uit 88/89). Wel waarschijnlijk bijsturen:
- `voidstone` wordt op grote vlakken en randen zichtbaar (tegelpatroon, herhaling van de sterrenpixels);
- `glitch_block` in losse vlekken in het oppervlak (contrast met `corrupted_stone`);
- `corrupted_sign`: leesbaarheid van zwarte/rode tekst op het grijze hout;
- `corrupted_leaves` van onderen gezien (omgekeerde bomen) en `corrupted_log` als kale boom.
Alleen bij V2-C: een nieuw flora-blok (bijv. `withered_tuft`, B + 99b; tot dan placeholder `dead_bush` + backlog).

## 7. Vragen aan de gebruiker (fase A)
Aanbevolen optie eerst. De antwoorden komen onder "Beslissingen" en via INT in `fase-1-wereld.md` en het beslissingenlog.

**V1 — Ontbrekende chunks (94).** 83b maakt al per corrupte plek (~elke 210 blokken) één weggevallen chunk tot de void,
met een rafelrand van 1–3.
- **A (aanbevolen):** 83b's weggevallen chunks *zijn* de ontbrekende chunks, plus een zeldzame **schone** variant als
  feature: precies 16 × 16 op het chunkraster, recht tot onder `min_y` (geen bedrock), ~1 per 20 × 20 chunks in de
  binnenband (ook `false_meadow`), los van de plekken, ≥ 32 van bandlijn en kastelen. Chunk-lokaal en deterministisch;
  ~100k blokken per gat (zeldzaam, verwaarloosbaar); `voidscan` moet de gaten kennen (export uitbreiden).
- **B:** alleen 83b's weggevallen chunks; 94 bouwt geen gat. Niets extra, geen risico; het "precies op het
  chunkraster"-beeld ontbreekt (rafelrand).
- **C:** de schone variant alleen in `false_meadow` (de perfecte weide met een perfect gat), ~1 per 10 × 10 chunks
  `false_meadow`. Sterkste contrast, maar `false_meadow` (~13 %) is niet overal.

**V2 — `corrupted_wastes`: dode bomen en bodem (94).**
- **A (aanbevolen):** kale dode bomen van `corrupted_log` (4–9 hoog, 1–3 kale takken, soms scheef, geen bladeren),
  in groepjes via clusterruis (~1 per 1–2 chunks gemiddeld, open vlaktes ertussen), plus vanilla `dead_bush` en losse
  keitjes van `corrupted_cobblestone`. Bestaande blokken, chunk-lokaal.
- **B:** als A, maar ~30 % van de bomen met een paar grijze plukjes `corrupted_leaves` (persistent): iets "levender",
  meer "net fout".
- **C:** als A + een nieuw grijs dood-gras-blok (B + 99b; tot dan placeholder `dead_bush` en backlog). Rijker beeld,
  maar werk buiten deze stroom en later.

**V3 — Glitchblokken (94 oppervlak + 95 "erts").** `glitch_block` is een `void_sources`-blok: in de buurt stijgt de
void-meter sneller.
- **A (aanbevolen):** in `glitch_fields` losse vlekken van 1–4 in het oppervlak (~3 % van de bovenblokken, ook op de
  randen van de kubussen en lagen; surface rule) + clusters van 3–8 als "erts" in `corrupted_stone` in heel ring 5 (alle
  diepten, ongeveer half zo vaak als ijzer). Glitchscherven overal te halen, de meter loopt vooral in `glitch_fields` op.
- **B:** alleen in `glitch_fields` (oppervlak + ondergronds). Sterk biome-karakter; scherven alleen daar.
- **C:** veel zichtbaarder: ~10 % van het oppervlak en hele randen van kubussen. Sterk beeld; de meter in heel
  `glitch_fields` bijna altijd "sneller".

**V4 — `false_meadow`: "gespiegelde bomen" (94).** De weide zelf: vanilla gras en weidebloemen, geen bijenkorven (geen
mobs).
- **A (aanbevolen):** bomen in **spiegelparen**: elke eik heeft op 6–12 blokken een exacte gespiegelde tweeling (zelfde
  vorm, gespiegeld in de lijn ertussen); ~1 paar per 2–3 chunks. Pas bij de tweede blik fout.
- **B:** alle bomen **identiek** (één en dezelfde eik, zelfde draaiing) op een te regelmatig raster. Sneller fout, minder
  subtiel.
- **C:** A + bloemen in te perfecte rijen/patronen. Iets meer "fout", nog steeds zonder gaten of gevaar.

**V5 — Herobrine-tekens: bordjes en redstone-fakkels (94).** Teksten (voorstel, en / nl): "Go back" / "Ga terug", "He
sees you" / "Hij ziet je", "Don't turn around" / "Draai je niet om", "You were here before" / "Je was hier al eens",
"This is not your world" / "Dit is jouw wereld niet", "Wake up" / "Word wakker", "He was here" / "Hij was hier", "No
one is coming" / "Er komt niemand", "Stop digging" / "Stop met graven", plus soms een leeg bordje. Redstone-fakkels
(licht 7) remmen de void-meter niet (≥ 12).
- **A (aanbevolen):** bordjes ~1 per 8–12 chunks in de binnenband, vaker naar de bandlijn toe en bij corrupte plekken;
  staand `corrupted_sign`, soms op een paal `corrupted_fence`, tekst naar de speler die van ring 4 komt (naar binnen
  gekeerd); zwarte tekst, ~1 op 5 rood. Fakkelpatronen (kruis, ring, rij van 5, pijl) ~1 per 12–16 chunks; de **pijl
  wijst naar het dichtstbijzijnde kasteel**.
- **B:** als A, maar **eiken** bordjes (alsof een speler ze zette); valt meer op tussen het grijze hout.
- **C:** alleen als set (bord + fakkelpatroon samen), zeldzamer (~1 per 20 chunks). Meer impact per vondst, minder vaak.

**V6 — Verlaten kampen (94).**
- **A (aanbevolen):** ~1 per 25–35 chunks in `corrupted_wastes`/`glitch_fields` (niet in `false_meadow`), ≤ 9 × 9: tent
  van grijze/witte wol op hekpalen, gedoofd kampvuur, een bed (willekeurige kleur), een kist met een kleine eigen loot
  table van alledaagse vanilla-spullen (brood, fakkels, kolen, een versleten ijzeren houweel), soms een bordje (V5). De
  loot table is eenvoudige data; fase 3 kan hem uitbreiden (backlog r. 439).
- **B:** de kist is leeg (geplunderd): geen loot in fase 1. Eenvoudiger, minder beloning.
- **C:** A + een geschreven boek met een kort vertaalbaar dagboekfragment (lore). Sterk, maar loopt vooruit op de lore
  van fase 5.

**V7 — Herinneringen (94).** Vier soorten (stuk eikenbos, oase met palm, gestold lavaveld, eilandje met gat), 10–30
breed, half gecorrumpeerd.
- **A (aanbevolen):** ~1 per 600 blokken (cellen van ~500) in heel ring 5; binnenband op de grond of scheef (≤ 25°),
  buitenband zwevend boven de void (~1/3 van alle herinneringen zweeft); de corruptie als verloop van één kant (de helft
  grijs: hout → `corrupted_log`, gras → `corrupted_grass_block`, zand → `corrupted_sand`, steen → `corrupted_stone`),
  randen afgebrokkeld. Pure plannen over chunkgrenzen (patroon blikvangers ring 4).
- **B:** alleen in de binnenband, nooit boven de void. Veiliger en eenvoudiger, minder spektakel.
- **C:** vaker (~1 per 300 blokken). Meer kans om ze te vinden, minder bijzonder.

**V8 — Voidsteen-randen en scheurwanden (95).** `voidstone` is ook een `void_sources`-blok (meter ↑ in de buurt) en geeft
`void_mote`.
- **A (aanbevolen):** voidsteen op **elke rand naar de void in de buitenband** (scheuren en kraters van `void_scars`,
  schotsranden, pinakelrand): het bovenblok 1–2 breed landinwaarts (rafelig) + de bovenste 2–4 blokken van de wand; de
  wand eronder `corrupted_stone` (deepslate weg) met voidsteen-aders die naar beneden toe dichter worden. Binnenband niet
  (losse scheuren en 83b-gaten blijven onopvallend).
- **B:** alleen `void_scars` en de pinakels (letterlijk het ontwerp); randen in `inverted_hills`/`shattered_expanse` kaal.
- **C:** A + ook de losse scheuren en de 83b-gaten in de binnenband. Elke afgrond zichtbaar (veiliger), maar de
  void-meter stijgt rond elke scheur sneller en de binnenband "verraadt" zijn gaten.

**V9 — Kasteelomgeving: verwoeste grond (95).** Fase 3 zet het gebouw op het plateau.
- **A (aanbevolen):** plateaumidden blijft schoon `corrupted_stone` (bouwgrond voor fase 3); de verwoeste rand (8–16) en
  de bruggenhoofden: puin van `corrupted_cobblestone`, losse obsidiaan/crying obsidian, glitchblokken, kleine scheurtjes;
  de rand naar de void voidsteen (V8).
- **B:** het hele plateau verwoest (puin en obsidiaan overal); fase 3 bouwt eroverheen en ruimt op.
- **C:** A + radiale brandsporen van obsidiaan/crying obsidian van het midden naar de twee bruggen (wijst de weg, sterker
  baken van ver).

**V10 — Omgekeerde bomen en de rest van de buitenband (95).**
- **A (aanbevolen):** onder elke omgekeerde heuvel (`inverted_hills`) hangen bomen ondersteboven: stam `corrupted_log`
  omlaag met een kroon van `corrupted_leaves` (persistent), ~1 per 1–2 chunks onderkant; waar ze hangen wordt de
  onderkant `corrupted_dirt`/`corrupted_grass_block` (een omgekeerde weide). Op de schotsen van `void_scars` en
  `inverted_hills` spaarzaam dode bomen (V2); `shattered_expanse` blijft kaal (brokken en bruggen, leegte als karakter).
- **B:** omgekeerde bomen zonder bladeren (kaal, zoals de dode binnenband); onderkant blijft steen.
- **C:** A + kleine omgekeerde bomen ook onder grote brokken in `shattered_expanse`.

**V11 — Ertsen: nulliet en vanilla (95).** Nulliet is de kandidaat-gear-tier van fase 2 (needs_diamond_tool).
- **A (aanbevolen):** nulliet in de buitenband alleen in steen met void binnen 2 blokken (scheurwanden, schotsranden,
  brokonderkanten), aders van 3–6, ~4 pogingen per chunk waar zo'n wand is; in voidsteen als `voidstone_nullite_ore`;
  binnenband diep (Y −64…−16) aders 2–4, ~1 per chunk (diamant-achtig). Vanilla-ertsen volgens het contract
  (×0,05 gewoon, ×0,03 midden, diamant ×0), ook in `corrupted_stone` (vanilla-ertsblokken).
- **B:** nulliet als A; vanilla-ertsen alleen in deepslate (onder Y ~0): boven is er niets meer ("de wereld is leeg"),
  kolen en koper vrijwel weg.
- **C:** royaler nulliet (aders 4–8, 2× zo vaak) zodat fase 2-gear zonder lang zoeken haalbaar is; vanilla als A.

## Voortgang
- 2026-10-23 18:32: worktree `P:/Projecten/Java/wt/r5-94` op `ring5/94` vanaf `061a20c4`; `setup.sh W-biomes 9`
  (server 25579, RCON 25589).
- 2026-10-23 18:40: plan geschreven; vragen V1–V11 naar main. Nog niets gebouwd.

## CODEBASE-delta
- **Gedeelde basis (`worldgen/feature`):** `Ring5World` (record: `Ring5Field`, `RingLayout`, `Ring5Band`, salt; `core`
  (ring 5 en de void-rand tot `EDGE_SHARE` 0,7, niet zone 4→5), `isVoid` (veld of schone ontbrekende chunk), `top`,
  `voidDistance` (Chebyshev, ≤ 8), `ringDepth`, `nearestCastle`; `salt(RandomState)` = `salt(seed)`), `Ring5Terrain`
  (`field`/`world` per random state, zwakke cache, WARN bij een misser), `Ring5Placements` (keys `ring_5/…`, `SURFACE`,
  `free`, `register`/`perChunk`; de volgorderegel: alles schrijft alleen in de eigen chunk), `Ring5Filter`
  (`ring5_filter`: `void_clearance`, `max_step`, `memory_clearance`, `on_castle`), `Ring5Features.ORDER` (één globale
  volgorde, `features(biome, step)`; `Ring5Biomes.generation` leest hem).
- **94:** `Ring5MissingChunks` (cel van 20 × 20 chunks, kans 0,9, ≥ 48 binnen de bandlijn, ≥ 176 van de ringgrens, op
  vaste grond; per thread een memo), `MissingChunkFeature` (`missing_chunk`: bodem/bedrock en lava van het gat weg, dam
  van 6 rond het gat onder Y 0), `HerobrineSignFeature` (`herobrine_sign`; `TEXTS`, `key`, `keepChance`, `placeSign`,
  `facingTheMiddle`; tekst als opgeslagen data, gewaxt), `TorchPatternFeature` (`torch_pattern`; kruis/ring/rij/pijl,
  `towardsCastle`), `AbandonedCampFeature` (`abandoned_camp`; `LOOT` `chests/ring_5/abandoned_camp`),
  `MirroredTreesFeature` (`mirrored_trees`; `pair`, `oak`), `Ring5Memories` + `MemoryFeature` (`ring_5_memory`, `Kind`;
  plannen per cel van 560, LRU-caches), `Ring5InnerFeatures` (configured/placed van 94), `datagen/Ring5ChestLoot`
  (sub-provider naast de blokloot in `ModBlockLootProvider.create`), `Ring5SignText` (9 teksten en + nl).
- **95:** `InvertedTreeFeature` (`inverted_tree`), `Ring5VoidFacePlacement` (`ring5_void_face`), `Ring5OuterFeatures`
  (omgekeerde bomen, dode bomen op de schotsen van `inverted_hills`, kasteelpuin via `boulder`), `Ring5OreFeatures`
  (`nullite_faces`, `nullite_deep`, `glitch_clusters`, `FACTORS` 0,05/0,03/0), `Ring5BiomeModifiers.ADD_RING_5_ORES`
  (`add_ring_5_ores`), `RingVanillaOres` (factor 0 = erts weg, `keys(ring, factors)`).
- **Surface:** condities `ring_5_ground` (`Ring5GroundCondition`, `part`: `core`, `missing_chunk`, `rim`, `wall_top`,
  `vein`, `wall`, `castle`, `inverted_hill`; per thread de laatste kolom) en `ring_5_speckle` (`Ring5SpeckleCondition`:
  `salt`, `chance`, `size`), blok `// ring 5 — W-biomes` in `ModSurfaceConditions`; `ring5_surface.py`: één top-level
  ring 5-regel direct na de bedrockvloer en de grotwanden van ring 3 (ontbrekende chunks → lucht, randen en wanden,
  omgekeerde weide, verwoest plateau), `glitch_fields` glitchvlekken, `void_scars` zonder `steep`/deepslate;
  `ring3_surface.py` zoekt het blok boven het voorlopige oppervlak nu op type (byte-gelijke uitvoer).
- **Registries:** `ModFeatures` blok `// ring 5 — W-biomes` (`missing_chunk`, `herobrine_sign`, `torch_pattern`,
  `abandoned_camp`, `mirrored_trees`, `ring_5_memory`, `inverted_tree`), `ModPlacementModifierTypes` (`ring5_filter`,
  `ring5_void_face`).
- **Tests:** `Ring5DressingTest` (nieuw), `Ring5BiomesTest` (features volgens `ORDER`, surface), `RingVanillaOresTest`
  (factor 0).

## Beslissingen
- **Gebruiker 2026-10-23:** V1 A, V2 **B** (~30 % van de dode bomen met plukjes persistente `corrupted_leaves`), V3 A,
  V4 A, V5 A, V6 A, V7 A, V8 **C** (ook de binnenband: losse scheuren, 83b-gaten en de schone ontbrekende chunks krijgen
  voidsteen-randen), V9 **B** (heel plateau verwoest; fase 3 bouwt eroverheen), V10 A, V11 A. In `fase-1-wereld.md`
  (Ring 5, "Aankleding en horror-features") en het beslissingenlog.
- (implementatie) **Volgorderegel:** elke ring 5-feature schrijft alleen in zijn eigen chunk (grondobject, of het eigen
  deel van een puur plan: spiegelbomen, herinneringen), alles leest de pure grond (`Ring5World`) of de bevroren
  heightmaps; daarom weidegras en -bloemen als `simple_block` (chunk-lokaal) in plaats van vanilla's `random_patch`, en
  geen ertsdoel (steen, `corrupted_stone`) in een herinnering. Objecten gaan over lucht en planten (`free`).
- (implementatie) **Ontbrekende chunks als surface + feature**, niet als terrein: de surface maakt alle steen lucht
  (zodat geen feature er nog op of in bouwt en de randen ze als void zien), `missing_chunk` haalt bedrock en de lava
  onder Y −54 weg en zet de dam (6) in de buurchunks. Geen wijziging aan `Ring5Field`/`RingFluidPicker` (W-terrein). De
  regel staat ná de bedrockvloer (de tests van ring 3 houden de bedrockvloer op plek 0 en de grotwanden op plek 1).
- (implementatie) **Herinneringen:** één placed feature per soort (`ring_5/memory_<kind>`, zoals het contract), één
  plan per cel van 560 voor alle soorten; ≥ 32 + straal binnen de ringgrenzen (een chunk buiten de ring 5-biomes zou zijn
  deel niet schrijven); boven de void houdt 0,4 van de kandidaten stand, zodat ~1/3 zweeft.
- (implementatie) **Bordjes gewaxt** (niet te bewerken) en de tekst op regel 2 van de voorkant; de setters van
  `SignBlockEntity` roepen een level aan dat in worldgen ontbreekt, dus de tekst gaat er als opgeslagen data in.
- (implementatie) **Dode bomen op schotsen alleen in `inverted_hills`:** `void_scars` is kale `corrupted_stone` en
  `dead_tree` wortelt alleen in aarde/zand.
- (implementatie) **Omgekeerde weide als top-level regel:** de onderkanten van de omgekeerde heuvels liggen ver onder het
  voorlopige oppervlak (tot Y −50), dus de biome-tak (boven het voorlopige oppervlak) bereikt ze niet.
- (implementatie) **Vanilla-ertsen in `corrupted_stone`:** `corrupted_stone` in `minecraft:stone_ore_replaceables`
  (blok-tag van B; via main), zodat `RingVanillaOres` vanilla's eigen configured features houdt.

## Backlog
- [blocks] `deepslate_nullite_ore` (B + 99b): de diepe nulliet van de binnenband (Y −64…−16) ligt in deepslate; voorlopig
  `nullite_ore` (in `corrupted_stone`-textuur). (bron: 95)
- [worldgen] `voidscan.py` kent de schone ontbrekende chunks niet (ze tellen als `void`, geen fout); als een scan ooit
  void binnen de binnenband als fout telt, de export (`RingColumnsExportTest`) `Ring5MissingChunks` laten lezen. (bron: 94)
- [structures] Fase 3: het kasteelplateau van ring 5 is in fase 1 helemaal verwoest (puin, obsidiaan, glitchblokken; V9-B);
  het kasteelgebouw bouwt eroverheen en ruimt zelf op waar het nodig is. (bron: 95)
