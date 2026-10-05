# Change 35 — Nether-ring 2: zwamkathedralen, reuzenzwamwoud en sporengrotten, dauwbronnen

Scope: `worldgen` — change 35

**Doel:** Nether-ring 2 (375 – 875 Nether-blokken, overworld-equivalent 1.500 – 3.500) wordt de **doorweekte schimmel-Nether**:
- eigen terrein als ring 2-invoer van `ring_blend` in de Nether: **zwamkathedralen**, hoge open holtes met een lagere lavazee en sporenmos-eilanden;
- twee eigen biomes: het **reuzenzwamwoud** (hoofdbiome) en de **sporengrotten**;
- **reuzenzwammen als zuilen** van vloer tot plafond, plus vrijstaande reuzenzwammen, slijmstalactieten en lichtgevende schimmel;
- **dauwbronnen in eigen dauwgeodes** (vraag 6 B, keuze gebruiker): holle bollen in de wanden met een binnenwand van dauwbronnen en voorgegroeide dauwkristallen, in beide biomes; deterministisch (holte uit een density function, bekleding uit een feature).

Het placeholder-terrein (vloer Y ~40, dak vanaf Y ~110) en `placeholder_nether_ring_2` verdwijnen.

Bron: `fase-1-wereld.md` §2 "Nether-tegenpool — Nether-ring 2", §1.2, §1.3, §1.12, §1.14, §1.15, bouwplan rij 35 (test B, D, W (Nether)); `docs/plans/change-14.md` (vanilla Nether-terrein, `squeeze`-valkuil), `change-16.md` (Nether-plaatsing, determinisme), `change-23.md` (gedeelde aanpak: klimaat per ring via de router + `multi_noise`, ring gate).

Buiten scope:
- Nether-2-sfeer: fog-kleur, druppelgeluiden, sporen-particles (change 36). Deze change zet alleen placeholder-kleuren op één plek (`BiomeAmbience.Palette.NETHER_RING_2`).
- Definitieve textures (change 37).
- Blokgedrag: sporenmos-spreiding, dauwkristalgroei, stalactietgedrag (changes 33 en 34, `blocks`).
- Mobs van Nether-ring 2 (fase 4, backlog: sporenmonsters, slijmwezens), structures (fase 3), items van dauwkristal (fase 2).

---

## Afstemming (2026-10-01)

Afgestemd met 23, 26, 33, 34, 36 en 37 (basis: de aanbevolen opties). Namen in dit plan zijn inline vervangen.
- **Namen uit 33/34:** `dripcap_*` → `dripshroom_*`, `glowing_mold` → `glow_mold`, `fungus_lamp` → `dripshroom_lamp`, blok `dew_spring` → `dew_source`. **Na vraag 6 = B (keuze gebruiker, 2026-10-01)** vervalt de vloer-feature `dew_source` (`DewSourceFeature`, placed `dew_source_forest`/`dew_source_caves`); daarvoor in de plaats: density function `corruptedadventure:dew_geodes` (class `DewGeodes`), feature `corruptedadventure:dew_geode` (class `DewGeodeFeature`), gedeelde pure vormklasse `DewGeodeShape`, één placed feature `dew_geode`.
- **Biome-namen (vraag 9 A, volgt het prefix van 33):** `dripshroom_forest` "Dripshroom Forest"/"Druipzwamwoud" en `spore_caves` "Spore Caves"/"Sporengrotten". Plannen 36 en 37 gebruiken deze namen (waren `dripping_fungal_forest`/`glowing_spore_caves`).
- **`fungal_growths`-tag komt uit 33 (+ stadia uit 34)**, geen placeholder-tag hier.
- **`RingFluidPicker`** heeft sinds 26 al `waterOnly`; `fluid_levels` komt ernaast (één plek voor vloeistofregels).
- **Gedeelde aanpak 23:** regio-as via de router + `multi_noise`; ring gate (23) geldt ook hier. Nether-ring 1 moet blok voor blok gelijk blijven (S-vergelijking).
- **Teststop (besloten 2026-10-01):** deze change is **W-achteraf**; de vroegere blokkerende visuele Nether-2-check (~15 min) is verhuisd naar **teststop 3: de gebundelde eindcheck ring 2 + Nether-2 ná change 37** (vóór change 38). 36 (biome-sfeer, data) en 37 (pixels) bouwen niet op de exacte terreinvorm; het hergebruik (zuilfeature, lavaniveau) komt pas in Nether-3 (51+).
- **Geodes (vraag 6 = B):** de deterministische uitwerking (holte in de density function, bekleding als feature) is een technische keuze van dit plan om de determinismeregel (stap 6) te houden; zie stap 3b en 7. **Stop-en-vraag-punt voor de bouwer:** blijkt de wandtest of de holte-DF niet haalbaar binnen het prestatiebudget (≤ 1,3× vanilla-Nether), of geeft hij zichtbare problemen (geodes die in hallen hangen, vervormde bollen), dan niet zelf een andere aanpak kiezen (zeker geen vanilla `geode` of een feature die terrein uitholt), maar terug naar de gebruiker.

---

## Afstemming na bouw 18–30c (2026-10-05)

Nagelopen tegen de stand na `3abf6f3` (changes 0–30, 23b, 23c, 30b, 30c gebouwd; 31–34 volgen eerst). Antwoorden blijven staan; dit zijn feiten uit de code en de bouwnotities.
- **Gedeelde aanpak van 23 is gebouwd** zoals gepland: de router-invoer per ring staat als `ring_blend/overworld_temperature.json` / `overworld_vegetation.json` (ring 1 = vanilla's `shifted_noise`, ring 2 = `ring_2/overworld/relief` / `moisture`, de rest 0) en de preset geeft ring 2 een `minecraft:multi_noise` met vijf punten. Stap 2 (Nether: `ring_blend/nether_temperature.json` + `multi_noise` met twee punten) kan dat patroon letterlijk volgen; de ring gate (`RingBlend.mapAll`) geldt vanzelf.
- **`RingFluidPicker`** heeft `noFluid(position)` en `waterOnly(dimension, position, y)` (26; `LAVA_LAYER_TOP` −54). `fluid_levels` komt ernaast; `RingAquifer` blijft zoals hij is (de Nether heeft geen aquifer).
- **Prestatiebudget:** het ring 2-budget voor de overworld is door de gebruiker verhoogd van 1,3× naar **1,75×** vanilla (23b-9 C; hoogste meetpunt nu 1,54×). Dit plan noemt voor de Nether nog **≤ 1,3× vanilla-Nether** (ring gate-beslisregel van 23). Of de Nether hetzelfde ruimere budget krijgt, is vraag 10 (antwoord A: ≤ 1,75×).
- **Determinisme-aanpak van de overworld (23c/30b) is anders dan stap 6 van dit plan:** daar schrijft elke feature **alleen in de eigen chunk**, met `jittered_grid` (wereldvast raster met minimale afstand), `noise_band` (vlekken op geseede ruis) en `slope`/`height_band`; de volgorde-onafhankelijkheid komt van "nooit lezen wat een buurchunk schrijft". Stap 6 (`FungalTerrainScan` + `GrowthPriority`, 3×3-schrijvers) blijft nodig voor zuilen en geodes die tot ±15 blokken reiken en daarom niet in één chunk passen; voor de **kleine** features (schimmel, stalactieten, bodemplanten) mag de bouwer de overworld-aanpak gebruiken (`jittered_grid` met marge, `noise_band` voor vlekken), zodat ze niet als willekeurige knotten komen. De plaatsingsmodifiers `jittered_grid`, `noise_band` en `height_band` (filter op de Y van de herkomst) lezen geen heightmap en zijn in de Nether bruikbaar met absolute Y-ankers; alleen `slope` leest `WORLD_SURFACE_WG`. Geen beslissing: implementatiedetail.
- **Heightmaps in de Nether:** `WORLD_SURFACE_WG` is in de Nether het dak (bedrock); de overworld-modifier die hem leest (`slope`) werkt niet. Vloer en plafond zoek je met `FungalTerrainScan` (stap 6) of met absolute Y-ankers.
- **Levendig voor de Nether:** de woudvloer heeft in dit plan alleen sporenmos, zuilen, stalactieten en schimmel; een kleine bodemplant (33 vraag 9, open) zou hier met een feature in vlekken geplaatst worden: `spore_sprouts` (A) en eventueel `glow_cap` (C), `jittered_grid` 8/1 + `noise_band`. Tot het antwoord er is, niets bouwen.
- **Geen overgangsbiome in de Nether** (§1.14) blijft; de ring-edge-dam (stap 1) is de enige vormvoorziening aan de grenzen.
- **Teststop 3** is ná change 37 en loopt de Nether-2-lijst hieronder als blokkerend af; de ring 2-overworld is dan ook beoordeeld op "levendig" (reden om de Nether niet kaler te laten).

### Gevolgen van de antwoorden 9 (plan 33) en 10 (2026-10-05)
- **35-10 A:** budget voor de noise-stap van Nether-ring 2 = **≤ 1,75× vanilla-Nether**; waar dit plan "≤ 1,3×" noemt (stop-en-vraag-punt geodes, ring gate-beslisregel) geldt nu 1,75×.
- **33-9 A:** nieuwe feature `spore_sprouts_patch` (hergebruik `simple_block`/`block_column`-loos, kruisplant op `spore_moss`) in beide biomes: `jittered_grid` 8/1 → `noise_band` (eigen ruis `nether_ring_2_sprouts`, vlekken ~10–20 blokken) → vaste Y/floor-scan (`FungalTerrainScan`) → `biome`; lage rang in `GrowthPriority` (onder schimmel). Dichter in de sporengrotten. Bonemeal op sporenmos plant ze ook.

## Afhankelijkheden en aannames

De code is gecontroleerd op de stand na change 17 (commit `7714b77`), ververst op 2026-10-05 (zie het blok hierboven). **Changes 18–30c zijn gebouwd; 31–34 volgen nog.** Alles hieronder wat uit 31–34 komt is een aanname en moet bij de start opnieuw worden nagelopen in `CODEBASE.md` en in `docs/plans/change-33.md` en `change-34.md`.

**Bestaat al (gecontroleerd):**
- `ring_blend/nether.json` verwijst naar `ring_2/nether.json`. Dat is nu een placeholder: `interpolated(max(vloer-gradiënt 24→56 + 0,5·ruis, dak-gradiënt 94→126 + 0,25·ruis))` met `placeholder_terrain`.
- `ring_1/nether.json` = vanilla Nether-`final_density` zonder de buitenste `squeeze` en `× 0,64` (change 14). Waarden ±1–2,5. De ring 2-invoer moet een vergelijkbare grootte hebben, anders wordt de overgang te kort (valkuil "Ring-invoer met `squeeze`").
- `noise_settings/nether.json`:
  - noise `min_y` 0, `height` 128, cellen 4 × 8 × 4;
  - `sea_level` 32, `default_fluid` lava, aquifers en ore veins uit, `legacy_random_source` true;
  - router: alleen `final_density` (→ `ring_blend/nether`) en `temperature`/`vegetation` (inline `shifted_noise`, `xz_scale` 1,0) zijn gevuld, de rest is 0.
  - Surface rules (top-level `sequence`): [0] bedrock-vloer, [1] bedrock-dak, [2] netherrack boven `below_top 5`, [3] basalt deltas, [4] soul sand valley, [5] generieke vloer (onder Y 32: `hole` → lava; warped/crimson nylium), [6] nether wastes, [7] netherrack.
- Dimensietype `corrupted_nether`: `ultrawarm`, hoogte 256, `logical_height` 128.
- `RingFluidPicker` kent alleen "geen vloeistof in de void-zone", anders vanilla (lava tot Y 31). `RingAquifer` wrapt de (in de Nether uitgeschakelde) aquifer. `RingChunkGenerator`: codec `biome_source`, `settings`, `dimension`.
- Preset, Nether `rings.ring_2` = `minecraft:fixed` `placeholder_nether_ring_2`; de Nether heeft geen `transitions` (harde wissel op gewicht 0,5).
- `PlaceholderBiomes`: `PLACEHOLDER_NETHER_RING_2` = `nether(fog 0x2E4A3A, water 0x3D8C7A)`. Verder alleen genoemd in `ModBiomes` en de twee lang-providers.
- `ModFeatures` (feature-types met code), `ModConfiguredFeatures`/`ModPlacedFeatures` (alleen keys), bootstraps per thema in `worldgen/feature/`, `DataGenerators` (features vóór de biomes).
- `VoidEdge` is het voorbeeld van een kleine 2D density function op `RingColumnCache`.
- `NetherRing1Features` + `Ring1BiomeModifiers` zijn het voorbeeld van Nether-plaatsing:
  - absolute Y-ankers (`belowTop` = boven het dak);
  - een `environment_scan` die alleen naar een bekende eindstaat zoekt;
  - geen `count_on_every_layer`.

**Change 23 (gebouwd; 2026-10-05 bevestigd, zie het blok "Afstemming na bouw"):**
- Vraag 1 = A: klimaat per ring via de router. `temperature`/`vegetation` worden een `ring_blend`, met ring 1 = de huidige vanilla-ruis, en de ring krijgt een `minecraft:multi_noise` met uitgeschreven parameters. Het overdrachtsblok noemt expliciet: "35 Nether-ring 2: zelfde patroon: `ring_blend` in de Nether-router + `multi_noise`; ring gate geldt ook in de Nether".
- Valt vraag 1 van change 23 anders uit (B: eigen `ring_regions`-source; C: vanilla-klimaat), dan volgt deze change **die** aanpak, zodat er één mechanisme in het project blijft. Stap 2 verschuift dan, de rest niet.
- Ring gate (change 23 vraag 3): gebouwd → hij geldt vanzelf voor de nieuwe Nether-ring 2-invoer (`RingBlend.mapAll`). Meten volgens de beslisregel; het getal waaraan getoetst wordt (≤ 1,3× of ≤ 1,75×) hangt af van vraag 10.

**Uit het plan van change 33 (afgestemd 2026-10-01; bij de start tegen `CODEBASE.md` leggen):**

| Rol | Registry-naam (change 33) | Gedrag |
|---|---|---|
| reuzenzwam-steel (log-achtig) | `dripshroom_stem` (+ `dripshroom_hyphae`, gestripte varianten) | `StrippableLogBlock`, as |
| reuzenzwam-hoed | `dripshroom_cap` | vol blok (zoals nether wart block) |
| planks + houtset | `dripshroom_planks`, … | niet in worldgen |
| doorweekt netherrack | `soaked_netherrack` | vol blok |
| sporenmos | `spore_moss` | zoals nylium: valt terug naar `soaked_netherrack`, bonemeal spreidt |
| slijmstalactiet | `slime_stalactite` (class `StalactiteBlock`) | dripstone-gedrag: states `vertical_direction` + `thickness` (`tip_merge`, tip, frustum, middle, base), valt zonder steun |
| lichtgevende schimmel | `glow_mold` | multiface (zoals glow lichen), licht |
| zwamlamp | `dripshroom_lamp` | vol lichtblok (zoals shroomlight) |
| plantbare zwam (alleen bij change 33 vraag 4 B) | `dripshroom_fungus` → configured-feature-key `corruptedadventure:dripshroom_fungus` | zoals de palmzaailing in change 22: alleen de key bestaat; **deze change registreert de vrijstaande reuzenzwam onder die key** |

**Uit het plan van change 34 (afgestemd 2026-10-01):**
- `dew_source` (dauwbron, class `DewSourceBlock`): vol, opaak natuurblok, oogt als water, druppelt (`animateTick`); geen fluid. Gedraagt zich als budding amethyst: dauwkristal groeit via random ticks op vrije zijden, alleen vanaf de eigen zijde (`facing` = richting van de bron af). Dropt niets (34 vraag 2 A), zit in geen carver-/stone-/nylium-tag: plaatsen ná de carvers.
- Groeistadia `small_dew_crystal_bud`, `medium_dew_crystal_bud`, `large_dew_crystal_bud`, `dew_crystal_cluster` (zoals amethist, met `facing`). Item: `dew_crystal`.
- Heeft change 34 minder stadia (bijv. één `dew_crystal`-blok), dan plaatst de feature alleen die.

**Blok-tag `corruptedadventure:fungal_growths` (afgestemd 2026-10-01):** wordt **niet** hier als placeholder gemaakt, maar in change 33 (`blocks`: steel, hyfen, gestript, hoed, zwamlamp, slijmstalactiet, gloeischimmel); change 34 voegt de vier dauwkristalstadia toe. Deze change gebruikt hem alleen (determinismeregel, stap 6). `dew_source` zit er bewust niet in (is terrein).

---

## Achtergrond (uitgezocht, niet opnieuw doen)

- **De lavazee is globaal.** `sea_level` en `default_fluid` gelden voor de hele dimensie. De Nether heeft geen aquifers: `Aquifer.createDisabled(globalFluidPicker)` geeft onder het niveau lava. Een lager niveau per ring kan dus alleen via `RingFluidPicker`. Die wordt ook gebruikt voor `getBaseHeight` en structure-plaatsing, dus het blijft overal consistent.
- **Verschillende vloeistofniveaus naast elkaar** geven een lava-"wand": een stilstaand lavavlak naast lucht, dat gaat stromen zodra er een blok-update komt. In `ultrawarm` stroomt lava ver en snel. Waar twee niveaus elkaar raken, moet het terrein onder het hoogste niveau dus dicht zijn (een dam).
- Vanilla's Nether-surface rule [5] maakt elk `hole` in de vloer onder Y 32 lava. Een eigen tak voor de ring 2-biomes moet **vóór** [3]–[7] staan, anders komt er lava boven het lagere ring 2-niveau.
- Surface rules lopen over **elk** blok dat gelijk is aan `default_block` (netherrack), niet alleen over de bovenlaag. Een laatste regel "ring 2-biome → `soaked_netherrack`" maakt dus de hele ring doorweekt. Vanilla's Nether-ertsen (`OreFeatures`: `block_match netherrack`) komen dan niet meer in de ring, tenzij er eigen kopieën met een ander doel komen.
- `stone_depth floor`/`ceiling` meet verticaal: in een verticale wand is alleen het bovenste blok "vloer". Wanden krijgen dus geen oppervlaktelaag, alleen de terugvalregel.
- **Legacy-ruis:** met `legacy_random_source` maakt `RandomState` de noises `minecraft:temperature`/`vegetation` op de legacy-Nether-manier. Dat gebeurt op basis van de noise-**key**. Wie de inline `shifted_noise` van ring 1 naar een eigen density-function-bestand verplaatst, houdt dezelfde key en dus dezelfde waarden. Een eigen key (`corruptedadventure:nether_ring_2_region`) is gewone, geseede ruis.
- **Generatievolgorde (valkuil change 16):** een feature die leest wat buurchunks al hebben gezet, geeft per laadvolgorde iets anders. Dat geldt voor `count_on_every_layer`, vanilla `huge_fungus` met die plaatsing, `multiface_growth` (zoekt een plek), `block_column` (kapt af en schuift de punt op), `vegetation_patch` en `geode`.
  - Stapvolgorde helpt niet over chunkgrenzen: buurchunk A kan al zijn stappen draaien vóór chunk B zijn eerste stap draait.
  - Oplossing: elk blok krijgt één eindtoestand, ongeacht de volgorde (stap 6).
- **Schrijfbereik:** features schrijven alleen in de 3×3 chunks rond hun herkomst. Een zuil met hoedstraal ≤ 8 past altijd.
- **Waarom de geodeholte niet uit een feature mag komen:** een feature die terrein uitholt, verandert wat andere features als "terrein" zien (vloer-/plafondscans), afhankelijk van welke chunk eerst decoreert — precies wat de determinismeregel verbiedt. Een holte in de density function bestaat al vóór de carvers en alle features, dus iedereen ziet hetzelfde terrein. Alleen de bekleding (terrein → `dew_source`, lucht → knoppen) is feature-werk, en die valt binnen de bestaande regel.

---

## Plan

### 1. Lavazee per ring (vraag 1 A)

- `RingChunkGenerator`: optioneel codec-veld `fluid_levels` (map `Ring` → Y), standaard leeg (oude presets en `level.dat` decoderen ongewijzigd).
  - `RingFluidPicker` krijgt die map. Het niveau volgt de ring van `position.ring()` (harde wissel op gewicht 0,5, dezelfde regel als de biome).
  - Ring met niveau → `FluidStatus(niveau + 1, default_fluid)`; anders vanilla; void-zone blijft `NO_FLUID`.
- Preset (Nether): `"fluid_levels": { "ring_2": 22 }` (startwaarde: 10 lager dan vanilla). Overworld: geen veld.
- **Dam langs de ringgrenzen.** Nieuw 2D density-function-type `corruptedadventure:ring_edge`, met velden `dimension`, `ring`, `width`:
  - 1 op en buiten de (verruisde) grenzen van `ring`;
  - binnen de ring smoothstep naar 0 op `width` blokken effectieve afstand van de dichtstbijzijnde grens (binnen- of buitengrens);
  - via `RingColumnCache`, zoals `VoidEdge`. Geen `RingLayout.current()` in codec of constructor, geen instantiestaat.
- In de ring 2-invoer: `dam = ring_edge(ring_2, 48) · max(0, 33 − y) / 4`, via `max` met het terrein.
  - Waar ring 2 gewicht ≥ 0,5 heeft en binnen 48 blokken van een grens ligt, is alles onder Y 33 vast.
  - Aan de kant van ring 1 (en ring 3) staat de lava tot Y 31 tegen vaste rots: geen lavawand.
  - Effect in de wereld: een oever van doorweekt netherrack rond de lagere lavazee.
- Unit tests: `RingFluidPickerTest` uitbreiden (per-ringniveau, wissel op 0,5, void wint, overworld ongewijzigd, codec zonder veld); `RingEdgeTest` (1 buiten, 0 op `width`, monotoon met tolerantie, beide grenzen, codec).
- Herbruikbaar: Nether-3 (bevroren lavazee) en Nether-4 (droog) krijgen hun eigen niveau of geen lava via dezelfde map.

### 2. Gedeelde regio-as in de Nether-router (vraag 3 A; aanpak change 23)

- `density_function/ring_2/nether/region.json` = `flat_cache(noise corruptedadventure:nether_ring_2_region, y_scale 0)` (2D).
  - Hoog = reuzenzwamwoud (hoge hallen), laag = sporengrotten.
  - Vlekken van ~150–300 Nether-blokken (de ring is 500 breed).
- Router `temperature` → `corruptedadventure:ring_blend/nether_temperature` = `ring_blend` met:
  - ring 1 = de huidige inline `shifted_noise` (letterlijk verplaatst, key `minecraft:temperature`, `xz_scale` 1,0);
  - ring 2 = `ring_2/nether/region`;
  - ring 3–5 en void = 0.
- `vegetation` blijft ongewijzigd (twee biomes hebben één as nodig; Nether-3 kan hem later op dezelfde manier ombouwen).
- Preset, Nether `rings.ring_2` → `minecraft:multi_noise` (inline):
  - het woud: `temperature` [T, 2];
  - de grotten: `temperature` [−2, T];
  - de andere vijf parameters [−2, 2].
  - T zo kiezen dat het woud ~65 % beslaat (startwaarde, test S).
- Biome en terrein lezen dezelfde named density function, dus de biomegrens valt op de terreinovergang.

### 3. Terrein: zwamkathedralen (vraag 2 A, vraag 5 A)

Handgeschreven JSON in `density_function/ring_2/nether/`. Waarden zijn startwaarden; afstemmen in test W.

| Bestand | Inhoud |
|---|---|
| `region_weight.json` | `spline` op `region`: 0 (grotten) onder T − 0,05, 1 (woud) boven T + 0,05 |
| `floor_height.json` | 2D: vloer Y 28 + heuvelruis ±5 (`nether_ring_2_floor`); **lavabekkens**: waar `nether_ring_2_basin` boven een drempel komt, zakt de vloer naar Y ~12 (lava tot 22). **Sporenmos-eilanden**: binnen een bekken, waar `nether_ring_2_island` boven een drempel komt, rijst de vloer tot Y 23–25 (net boven de lava). Een bekken beslaat ~25 % van de ring |
| `ceiling_height.json` | 2D: woud Y 108 ± 6; grotten Y 70 ± 8; tussenin via `region_weight` |
| `walls.json` | 3D, alleen in het woud: grootschalige, verticaal uitgerekte ruis (`nether_ring_2_walls`, kleine `y_scale`). Waar `abs(ruis)` onder een drempel ligt: vast. Dat geeft dikke wanden en ribben die de hallen in kamers van ~60–150 blokken delen, met bogen en doorgangen door de 3D-variatie |
| `caves.json` | 3D, alleen in de grotten: dichter grottenstelsel (kaas-achtige ruis `nether_ring_2_caves`, kamers 10–30 blokken, tunnels) tussen vloer en plafond |
| `terrain.json` | `max(vloerterm, plafondterm, region_weight · walls, (1 − region_weight) · caves, dam)`. Vloerterm = `(floor_height − y) / S`, plafondterm = `(y − ceiling_height) / S`, `S` zo dat de waarden in het bereik van ring 1 liggen (±1–2,5) |

- `ring_2/nether.json` (vervangt de placeholder) = `min(interpolated(terrain), corruptedadventure:ring_2/nether/dew_geodes)` — de geodeholtes buiten de interpolatie (exacte bollen, zie stap 3b).
- Nieuwe noise-parameters in `worldgen/noise/`: `nether_ring_2_region`, `_floor`, `_basin`, `_island`, `_walls`, `_caves`, `_geode_cells`. `placeholder_terrain` blijft (ring 3–5).
- Dak: het plafond ligt maximaal op Y ~115, en de bedrock-dakregel (vanaf `below_top 5`) blijft vanilla, dus ≥ 8 blokken rots boven elke hal.
- Overgangen:
  - 1 → 2: vanilla ↔ hallen over ~32 blokken (smoothstep), plus de dam;
  - 2 → 3: hallen ↔ placeholder ring 3 (vloer Y ~55), plus de dam.

### 3b. Dauwgeode-holtes: density function `corruptedadventure:dew_geodes` (vraag 6 B, keuze gebruiker)

Nieuw: `worldgen/densityfunction/DewGeodes.java` (type geregistreerd in `ModDensityFunctionTypes`) + gedeelde pure vormklasse `worldgen/feature/DewGeodeShape.java` (ook gebruikt door de feature, stap 7). Patroon: `HoodooPillars` (change 30).

- **Codec:** `terrain` (DF, `corruptedadventure:ring_2/nether/terrain`), `region_weight` (DF), `edge` (DF, `ring_edge(ring_2, 48)`), `noise` (`NoiseHolder` `corruptedadventure:nether_ring_2_geode_cells`, alleen als geseede cel-hash), `cell_size` (32, 2D-raster), `chance_forest` / `chance_caves` (kans op een kandidaat per cel), `inner_radius` (4–7), `shell` (3: minimale dikte vaste rots rond de holte), `min_y`/`max_y` per regio (woud Y 30–100, grotten Y 26–62).
- **Per cel** (gejitterd middelpunt, eigenschappen uit `noise` op het celmidden): kandidaat ja/nee (kans volgens `region_weight` op het middelpunt: woud/grotten), straal, Y.
- **Wandtest (pure functie, één keer per cel, ThreadLocal-cache zoals `RingColumnCache`):** de kandidaat bestaat alleen als `edge` = 0 op het middelpunt (≥ 48 blokken van een ringgrens, dus geen menging met ring 1/3) en `terrain` > 0 (vast) op het middelpunt en op 26 testpunten (6 assen, 12 ribben, 8 hoeken van de richtingsbol) op afstand `inner_radius + shell`, en onder Y 23 nooit (lava). Zo ligt een geode altijd ingesloten in een wand, niet zwevend in een hal.
- **Waarde:** binnen de holte (afstand ≤ straal, licht verruisd met de cel-hash voor een minder perfecte bol) → −1; anders +10 (neutraal onder `min`). Vroege exit: geen kandidaat in de 3×3 cellen rond de kolom → +10.
- Puur van (seed, x, y, z): geen `RingLayout.current()`, geen veranderlijke instantiestaat; `minValue`/`maxValue` = −1/+10.
- **Effect:** de holte bestaat vanaf de noise-stap. Surface rules geven de holtebodem sporenmos en het plafond doorweekt netherrack (zoals elke vloer/plafond in de tak van stap 5); carvers (`nether_cave`) kunnen een geode openbreken (natuurlijke ingang, zoals vanilla-geodes in grotten; deterministisch). De schil is gewoon het omringende `soaked_netherrack`: van buiten onzichtbaar, je vindt een geode door langs wanden te graven of via een opengebroken exemplaar.
- **Zeldzaamheid (afstemwaarde, gemeten in S):** doel woud ~1 geode per 10 chunks (bereik 8–12), sporengrotten ~1 per 5 chunks (bereik 5–6). Startwaarden: cel 32×32 (4 chunks), `chance_forest` 0,6, `chance_caves` 0,95 vóór de wandtest; bijstellen op de gemeten slagingskans van de wandtest. In de dichte sporengrotten faalt de wandtest vaker; daarom daar een hogere kans en een lagere maximale straal (4–5).

### 4. Biomes (datagen): `worldgen/biome/NetherRing2Biomes.java` (namen: vraag 9)

- `ModBiomes`: `DRIPSHROOM_FOREST`, `SPORE_CAVES` erbij. `PLACEHOLDER_NETHER_RING_2` gaat weg, ook uit `PlaceholderBiomes` en beide lang-providers. Werelden van vóór change 35 laden daarna niet meer.
- `BiomeAmbience.Palette.NETHER_RING_2` = de placeholder-kleuren (lucht = Nether-lucht, fog `2E4A3A`, water `3D8C7A`, waterfog vanilla). Dat is **één plek**, die change 36 definitief maakt. Geen mood/particles/eigen geluid (change 36). Muziek als placeholder: die van vanilla nether wastes.
- Beide biomes: geen neerslag, temperatuur 2,0, downfall 0. Carver `nether_cave` (zoals vanilla).
- Generation in vanilla-volgorde per stap (valkuil "Feature order cycle"; de Nether-kopieën van ring 1 delen vanilla-features), eigen features achteraan hun stap:
  - `UNDERGROUND_DECORATION`: ertsen volgens vraag 7. **Geen** lavabronnen (`spring_open`/`spring_closed`), geen vuur, geen glowstone, geen basalt.
    - Waarom: lavabronnen lezen hun buren (generatievolgorde) en passen niet bij het natte thema.
    - Het licht komt van eigen blokken.
  - `UNDERGROUND_DECORATION` (achteraan): `dew_geode` (bekleding van de geodeholtes, stap 7).
  - `VEGETAL_DECORATION` (achteraan): reuzenzwammen, stalactieten, schimmel (stap 5), in een vaste volgorde. Door stap 6 maakt de volgorde voor het resultaat niet uit.
- Spawns: vraag 8.
- **Tags** (`ModTags.Biomes`, `ModBiomeTagsProvider`):
  - nieuw `corruptedadventure:is_nether_ring_2` (beide biomes);
  - `c:is_nether`, `c:is_hot/nether` (ultrawarm), `c:is_wet/nether` (als die conventietag in NeoForge 21.1 bestaat), `c:is_nether_forest` (alleen het woud);
  - **geen** `minecraft:`-tags (geen `is_nether` → geen fortress/ruined portals; geen `has_structure/*`), tenzij vraag 8 C.
- Lang: `biome.corruptedadventure.dripshroom_forest` "Dripshroom Forest" / "Druipzwamwoud", `spore_caves` "Spore Caves" / "Sporengrotten" (volgens vraag 9).
- Unit test `NetherRing2BiomesTest`: geen neerslag, temperatuur 2,0, fog/water = `Palette.NETHER_RING_2`, beide in `is_nether_ring_2`, geen `minecraft:`-tag, geen feature met `count_on_every_layer` in de placed features.

### 5. Surface rules (`noise_settings/nether.json`; vraag 7)

Nieuwe tak als **index 3** van de top-level-`sequence` (na bedrock en de `below_top 5`-regel, vóór basalt deltas en de generieke vloer met `hole` → lava). Voorwaarde: `biome_is [dripshroom_forest, spore_caves]`. Binnen de tak:
- **Vloer** (`stone_depth floor`, 1 diep):
  - woud: `spore_moss`, met vlekken `soaked_netherrack` via surface-ruis (`minecraft:nether_wart`, zoals vanilla nylium/wart);
  - grotten: `spore_moss` in vlekken op `soaked_netherrack`;
  - eilanden en oevers (Y ≤ 26): `spore_moss`.
- **Onder de vloer** (`add_surface_depth`, 3–5 diep) en **plafond** (`stone_depth ceiling`, 2–4 diep): `soaked_netherrack`.
- **Terugval** (vraag 7 A): alle overige rots in de tak → `soaked_netherrack`. De hele ring is doorweekt, ook wanden en de kern.
- Geen `hole` → lava-regel (de lava komt alleen van `fluid_levels`).

### 6. Determinismeregel voor alle eigen Nether-2-features

Eén regel, één plek in de code, zodat het resultaat niet afhangt van de volgorde waarin chunks worden gedecoreerd:
- **Lezen:** features zoeken vloer, plafond en wanden alleen op **terrein**. Lucht en `#corruptedadventure:fungal_growths` zijn doorzichtig. Vaste blokken, lava, `dew_source` en ertsen zijn terrein. Ertsen vervangen terrein door terrein, dus een scan ziet dezelfde hoogte.
  - Class `worldgen/feature/FungalTerrainScan`: `floor(level, x, y, z, max)`, `ceiling(…)`, `isTerrain(state)`.
  - Elke beslissing van een feature hangt alleen af van terrein + de eigen geseede random.
- **Schrijven:** class `worldgen/feature/GrowthPriority`, met een vaste rangorde (hoog → laag):
  - steel/hyphae > hoed > zwamlamp > slijmstalactiet > dauwkristalknop > lichtgevende schimmel.
  - Een feature schrijft een groeisel alleen in lucht of in een **lager** groeisel. Zo is het eindblok op elke positie het hoogste groeisel dat iemand daar wilde, ongeacht de volgorde.
  - Een feature zoekt **nooit** een andere plek als een positie bezet is (geen zoeken naar alternatieven).
- **Terrein vervangen** doet alleen `dew_source` (de geodebekleding, stap 7). Die overschrijft elk terreinblok behalve lava en bedrock, ook ertsen. Ertsen kunnen `dew_source` niet als doel hebben, dus de eindtoestand is altijd `dew_source`. **Terrein uithollen doet geen enkele feature:** de geodeholtes komen uit de density function (stap 3b).
  - Er is **geen** rand van doorweekt netherrack om de bron: ertsen kunnen die als doel hebben, en dan wint de volgorde.
- **Geen** `count_on_every_layer`, `random_patch`, `multiface_growth`, `block_column`, `vegetation_patch`, vanilla `geode` of vanilla `huge_fungus` in Nether-2-placements (de eigen `dew_geode` valt wel binnen de regel). Placement = `count`/`rarity_filter` → `in_square` → vaste Y of `height_range` (absolute ankers) → `biome`. De feature scant zelf.
- Unit test `GrowthPriorityTest`: een lijst schrijfacties van twee overlappende features in alle volgordes (permutaties) op een kleine in-memory wereld geeft hetzelfde resultaat — ook met de geodebekleding (terrein → `dew_source`, lucht → knop) als een van de features.
- Bekende vanilla-rest: overlappende vanilla-ertsaders onderling (zie change 16: quartz verschilde 1 blok in 1 van 24 chunks). Dat wordt gedocumenteerd, niet opgelost.

### 7. Features (Java + datagen): `worldgen/feature/NetherRing2Features.java` (bootstraps)

**`corruptedadventure:giant_fungus`** (vraag 4 A):
- `GiantFungusFeature` + `GiantFungusConfiguration`: block providers voor steel, hoed, lamp en druppel; vorm `pillar` of `free`; afmetingen.
- Pure vormklasse `GiantFungusShape`: geeft (positie, blok, rang) terug en is los te testen.
- **Zuil:**
  - `FungalTerrainScan` vanaf de herkomst (Y 64) omlaag naar de vloer (max. 64) en omhoog naar het plafond (max. 96).
  - Geen vloer, vloer = lava, of plafond − vloer < 20 → het wordt de vrijstaande vorm.
  - Ronde steel, doorsnede 3–7, van vloer tot plafond. Per kolom van de steel loopt die omlaag tot het terrein (max. 6, wortelvoet op een ongelijke vloer).
  - 1–3 hoedschijven langs de steel (straal 4–8, licht hangend), een hoedkraag tegen het plafond, `dripshroom_lamp` verspreid in de hoeden (~8 %).
  - Onder elke hoedschijf 2–5 korte slijmstalactieten. Die komen uit dezelfde feature, dus ook deterministisch: "druipend".
- **Vrijstaand:** steel 8–20 hoog, één koepelhoed (straal 3–6), lampen, korte stalactieten. Herkomst = vloer uit de scan.
  - Geregistreerd als `corruptedadventure:dripshroom_fungus` als change 33 die key heeft (plantbare zwam). Anders onder `giant_fungus_free`.
- Hele vorm binnen ±15 blokken van de herkomst (3×3-chunks-regel, test).
- Herbruikbaar voor `rime_forest` (Nether-3, verstijfde reuzenzwammen): andere block providers.

**`corruptedadventure:slime_stalactite`:**
- `SlimeStalactiteFeature` + config: lengte 1–5 aan een **terreinplafond** (scan omhoog vanaf de herkomst), states base → middle → frustum → tip.
- Per positie via `GrowthPriority` (geen afkappen en opschuiven zoals `block_column`).
- Een stalactiet die halverwege door een hoger groeisel wordt overschreven, blijft zo staan (cosmetisch, geaccepteerd).

**`corruptedadventure:mold_patch`:**
- Lichtgevende schimmel op terreinvlakken binnen straal 2–4 rond de herkomst: vloer, plafond en wanden.
- Multiface-states per vrije zijde, alleen op terreinzijden.

**`corruptedadventure:dew_geode`** (vraag 6 B, keuze gebruiker; class `DewGeodeFeature` + `DewGeodeConfiguration`):
- Herkomst = de minhoek van de chunk (placement `count` 1, geen `in_square`). De feature behandelt alleen geodecellen waarvan het middelpunt in **deze** chunk ligt (geometrie uit `DewGeodeShape`, dezelfde cel-hash als de DF; de holte + bekleding reikt ≤ 7 + 3 blokken, dus binnen de 3×3 chunks).
- **Bestaat de geode?** Lezen alleen terrein (`FungalTerrainScan.isTerrain`): middelpunt is lucht én de testpunten van de wandtest (stap 3b) zijn terrein. Terrein ligt na de noise-stap en de carvers vast en geen feature verandert de vorm ervan, dus deze check is volgorde-onafhankelijk. (Waar een carver de schil heeft opengebroken, kan de check falen; dan geen bekleding — ook deterministisch.)
- **Binnenwand:** elke positie op afstand straal..straal + 1 die terrein is (niet lava/bedrock) wordt met kans ~6 % (afstemwaarde; vanilla-amethist ~8 %) `dew_source` (terrein → terrein, regel stap 6); de rest blijft `soaked_netherrack`/`spore_moss`. Startwaarde: ~15–35 bronnen per geode (straal 4–7).
- **Voorgegroeide knoppen:** op ~25 % van de bronzijden die aan de holte grenzen een knop (`facing` = van de bron af; meestal `small_`/`medium_`, zelden `large_`, geen cluster), via `GrowthPriority` (rang "dauwkristalknop"), alleen in lucht of een lager groeisel.
- Optioneel (afstemwaarde): enkele vlekken `glow_mold` op de binnenwand (laagste rang), zodat een opengebroken geode van ver oplicht.
- Geen andere blokken, geen nieuwe blokken (alleen `dew_source`, de vier stadia, eventueel `glow_mold` uit 33/34).

**Placed features** (`ModPlacedFeatures`; startwaarden, afstemmen in W):

| Placed feature | Biome | Placement |
|---|---|---|
| `dripshroom_forest_pillars` | woud | `rarity_filter` 3 → `in_square` → vaste Y 64 → `biome` |
| `dripshroom_forest_fungi` | woud | `count` 2 → idem |
| `dripshroom_forest_stalactites` | woud | `count` 3 → `in_square` → Y 40–100 → `biome` |
| `dripshroom_forest_mold` | woud | `rarity_filter` 2 → `in_square` → Y 24–110 → `biome` |
| `spore_caves_fungi` | grotten | `count` 1 → `in_square` → Y 24–70 → `biome` |
| `spore_caves_stalactites` | grotten | `count` 6 → idem |
| `spore_caves_mold` | grotten | `count` 4 → idem |
| `dew_geode` | woud + grotten | `count` 1 (herkomst = chunk-minhoek, geen `in_square`, geen `biome`-filter: de feature volgt de holtes uit de DF; die bestaan alleen in de ring 2-invoer) |

Ertsen (vraag 7 A): kopieën van vanilla's Nether-ertsen met dezelfde grootte, tarieven en Y-bereiken als `addNetherDefaultOres`, met doel `soaked_netherrack` in plaats van netherrack:
- `ore_quartz`, `ore_gold`, `ore_ancient_debris` (`large`/`small`, `discard_chance_on_air_exposure` 1,0), `ore_gravel`, `ore_blackstone`, `ore_magma`;
- namen `nether_ring_2_ore_*`; absolute ankers waar vanilla `belowTop` gebruikt (valkuil);
- ancient debris blijft volgens §1.11 "zoals vanilla" in tarief.

Registratie: `ModFeatures` (`giant_fungus`, `slime_stalactite`, `mold_patch`, `dew_geode`); `ModDensityFunctionTypes` (`ring_edge`, `dew_geodes`); keys in `ModConfiguredFeatures`/`ModPlacedFeatures`; `DataGenerators` (bootstraps in de bestaande lambdas, features vóór de biomes).

### 8. Opruimen en documentatie

- Preset: Nether ring 2 → `multi_noise` (stap 2) + `fluid_levels`. `placeholder_nether_ring_2` overal weg. `fase-1-wereld.md` §1.15: rij 2 Nether → "zwamkathedralen (change 35), lava Y 22".
- `CODEBASE.md`:
  - nieuwe classes: `NetherRing2Biomes`, `NetherRing2Features`, `GiantFungusFeature`/`Configuration`/`Shape`, `SlimeStalactiteFeature`, `MoldPatchFeature`, `DewGeodeFeature`, `DewGeodeShape`, `DewGeodes` (DF), `FungalTerrainScan`, `GrowthPriority`, `RingEdge`;
  - registries: feature-types, `ring_edge`, `dew_geodes`, configured/placed features, `is_nether_ring_2`, `fungal_growths`;
  - biomes-tabel (placeholder weg), `RingChunkGenerator`/`RingFluidPicker` (`fluid_levels`), databestanden (`ring_2/nether/*`, `ring_blend/nether_temperature`, noise-bestanden, surface-rule-tak index 3), `Palette.NETHER_RING_2`;
  - meetresultaten (geodes per chunk per biome, bronnen per geode, zuilen, prestatie);
  - valkuilen: lavaniveau per ring + dam; Nether-tak vóór de `hole` → lava-regel; de determinismeregel (lezen alleen terrein, schrijven op rang, nooit uitwijken; holtes alleen uit density functions, nooit uit features); legacy-key bij het verplaatsen van `minecraft:temperature`.
  - Kopregel: change 35.
- `fase-1-wereld.md` §2 Nether-ring 2: **Uitwerking (change 35)**: hoogtes, lava, dam, regio's, zuilen, dauwgeodes (vorm, zeldzaamheid, gemeten tarief; wijkt af van "dauwbron in de vloer" en van vanilla-geodes), ertsen, spawns, tags. §1.12 ongewijzigd.
- `02-beslissingenlog.md`: regels hieronder.
- `docs/backlog.md`:
  - `[blocks]` erts-varianten in doorweekt netherrack (kwarts/goud hebben nu de netherrack-texture in groengrijze rots; alleen bij vraag 7 A);
  - `[structures]` Nether-ring 2 zonder vanilla-structures, keuze in fase 3;
  - `[atmosphere]` change 36: fog-dichtheid afstemmen op de halgrootte (W-notitie).

---

## Overdracht naar latere changes

| Change | Wat er klaarligt |
|---|---|
| 36 Nether-2-sfeer | `Palette.NETHER_RING_2` (één plek); mood/particles/druppelgeluid per biome in `NetherRing2Biomes`; halhoogte woud ~80, grotten ~40 (fog-afstand) |
| 37 textures | alle blokken staan in de wereld (zuilen, bronnen, mos, schimmel); vanilla-ertsen in doorweekt netherrack (vraag 7) |
| Nether-3 (worldgen) | `fluid_levels` (bevroren lavazee = geen lava of een ander niveau), `ring_edge`-dam, `giant_fungus` met rijpzwam-blokken voor `rime_forest`, `FungalTerrainScan`/`GrowthPriority` (eigen tag), regio-as in de Nether-router |
| fase 2 | tarief dauwgeodes en bronnen per geode (gemeten), voorgegroeide kristallen |

---

## Client/server-impact

- Puur worldgen, server-side en deterministisch per seed:
  - alle ruis is geseed via noise-keys;
  - `ring_edge` en `RingFluidPicker` gebruiken `RingLayout` (wereldseed) via `RingColumnCache` (ThreadLocal);
  - features gebruiken alleen vanilla's geseede decoratie-random plus de determinismeregel (stap 6).
- Geen netwerk, geen SavedData, geen client-code.
- Biomes en tags gaan via de vanilla-registry-sync naar de client (kleuren). De client kent het lavaniveau niet en heeft het niet nodig.
- `RingChunkGenerator`-codec: het nieuwe veld is optioneel. Werelden van vóór change 35 laden toch niet (biome `placeholder_nether_ring_2` weg); test W gebruikt altijd een verse wereld.
- Feature-types en `ring_edge` zijn common registries (worldgen, niet gesynct).
- Dedicated server: niets client-only; S controleert dat preset, codecs en feature-volgorde laden.

---

## Tests

Bouwplan: **B, D, W (Nether)**; daarnaast **S**, omdat preset, codecs en de feature-volgorde pas bij het laden worden gecontroleerd en de metingen via de server lopen. Testseed `20260930`; per wereld een nieuwe `level-name` (`smoke-change35-…`). RCON alleen tijdens de test aan (valkuilen in `CODEBASE.md`).

### B — `./gradlew build`
- [ ] `DewGeodesTest` / `DewGeodeShapeTest` (determinisme): dezelfde seed → dezelfde kandidaten, stralen en bekleding; pure functie (twee threads, nieuwe instantie via `mapAll` → gelijke waarden); holte + bekleding binnen de 3×3 chunks rond de chunk van het middelpunt; wandtest faalt bij een testpunt in lucht of op ring 2-rand (`edge` > 0); geen holte onder Y 23; codec-rondreis en fouten bij ongeldige stralen/kansen.
- [ ] `RingFluidPickerTest` (uitgebreid), `RingEdgeTest`, `GrowthPriorityTest` (permutaties, incl. geodebekleding), `GiantFungusShapeTest`:
  - 200 seeds;
  - steel ononderbroken van vloer tot plafond;
  - alle blokken binnen ±15 van de herkomst en binnen de 3×3 chunks;
  - hoedstraal en lampaandeel binnen de grenzen.
- [ ] `NetherRing2BiomesTest`, plus de bestaande `RingBiomeSourceTest`/`VanillaClimateBiomeSourceTest` groen.

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit:
  - `dripshroom_forest.json`, `spore_caves.json`; geen `placeholder_nether_ring_2.json`;
  - configured/placed features;
  - tags `is_nether_ring_2`, `c:*`, `fungal_growths`;
  - lang en/nl.
- [ ] Controle in de output: geen `count_on_every_layer`, `random_patch`, `multiface_growth`, `block_column`, `huge_fungus` of `minecraft:geode` in de Nether-2-placed/configured features (wel `corruptedadventure:dew_geode`); alleen absolute Y-ankers.

### S — server + RCON (geautomatiseerd)
- [ ] `tools/server-smoke-test.sh`: start zonder fouten (geen "Feature order cycle", geen codec-fout, geen client-class-melding).
- [ ] **Nether-ring 1 ongewijzigd** (router-`temperature` verplaatst):
  - wereld A (vóór change 35) en wereld B (na), zelfde seed;
  - `execute if biome` op 200 punten in Nether-ring 1 (r 0–330) → identiek;
  - per chunk tellingen (netherrack, lava, lucht, quartz) in 6 chunks → identiek.
- [ ] **Biomes:**
  - `locate biome corruptedadventure:dripshroom_forest` en `spore_caves` vanaf `ca tpring 2 nether` in 4 richtingen → gevonden binnen Nether-ring 2;
  - langs de cirkel r = 625 elke 8 blokken `execute if biome` → aandeel woud 55–75 %;
  - placeholder bestaat niet meer.
- [ ] **Lavaniveau:**
  - 16 chunks in de kern van ring 2 (8 richtingen × r 520 en 720);
  - `fill <chunk> 23..127 air replace lava` → **0**; `fill <chunk> 0..22 … replace lava` > 0 in chunks met een bekken.
  - Nether-ring 1 (r 200): lava tot Y 31 aanwezig.
- [ ] **Geen lavawanden:**
  - langs `ca tpboundary 1 nether` en `2 nether` in 8 richtingen, per grens 3 chunks over de zone;
  - script: elk lavablok op Y 23–31 met een horizontale buur die lucht is → **0**;
  - bovendien in kolommen met ring 2-gewicht ≥ 0,5 binnen 32 blokken van de grens: Y 23–32 vast.
- [ ] **Terrein:**
  - per biome 100 kolommen: vloer (eerste terreinblok onder Y 64) en plafond;
  - woud: vloer Y 23–40 (bekkens lager), plafond Y 100–116, luchtaandeel Y 23–115 > 50 %;
  - grotten: plafond Y 60–80;
  - op ≥ 8 eilanden: `spore_moss` op Y 23–26 met lava eromheen.
- [ ] **Oppervlak:** `fill … replace netherrack` in 6 ring 2-chunks → 0 (bij vraag 7 A); `spore_moss` > 0 op vloeren; `soaked_netherrack` overheerst.
- [ ] **Zuilen:**
  - `place feature corruptedadventure:giant_fungus` (zuilvorm) in een hal → steel ononderbroken van vloer tot plafond (`execute if block` per Y op de stammidden), hoeden, lampen, stalactieten;
  - in het wild: 64 woudchunks → `dripshroom_stem` in ~1 op 3 chunks (afstemmen); in de sporengrotten geen zuilen (alleen vrijstaande zwammen).
- [ ] **Dauwgeodes:**
  - 225 chunks (15×15) per biome → aantal geodes (aaneengesloten groepen `dew_source`) per chunk, bronnen per geode, knoppen per geode;
  - doel (afstemwaarde): woud ~1 geode per 10 chunks (8–12), sporengrotten ~1 per 5 chunks (5–6); bronnen per geode ~15–35;
  - elke geode ingesloten: per geode het aandeel holteblokken met lucht dat aan een hal grenst ≈ 0 (alleen opengebroken door carvers);
  - geen `dew_source` buiten geodes (geen losse vloerbronnen meer);
  - Nether-ring 1, Nether-ring 3, de 48-blokken-randzone en overworld: **0**.
- [ ] **Ertsen** (vraag 7 A): quartz/goud > 0 in 6 ring 2-chunks; ancient debris over 225 chunks in de orde van vanilla.
- [ ] **Determinisme:**
  - twee verse werelden met dezelfde seed; de tweede laadt de chunks in **omgekeerde volgorde** (zoals change 16: forceload-blokken van ver naar dichtbij en per blok omgekeerd);
  - per chunk (32 woud-, 32 grottenchunks, 8 grenschunks) tellingen van `dripshroom_stem`, `dripshroom_cap`, `dripshroom_lamp`, `slime_stalactite`, `glow_mold`, `dew_source`, kristalknoppen, `spore_moss`, `soaked_netherrack`, lava en lucht → **identiek** (ook in chunks met een geode die over een chunkgrens loopt);
  - toegestaan verschil: alleen vanilla-erts-onderling (documenteren met aantallen).
- [ ] **Prestatie** (zoals change 14 en 9):
  - wandkloktijd voor 256 chunks in Nether-ring 2 en JFR `minecraft.ChunkGeneration` (noise-stap gemiddeld/mediaan/p95), vergeleken met de vanilla-Nether op dezelfde plek en Nether-ring 3;
  - budget ≤ 1,3× vanilla (incl. de per-blok-geode-DF); getallen in `CODEBASE.md`. Overschrijding door `dew_geodes` → stop-en-vraag (zie Afstemming).

### W — handmatige test: **W-achteraf — opgenomen in teststop 3** (gebundelde eindcheck ring 2 + Nether-2 ná change 37, vóór change 38; besloten 2026-10-01). Hieronder de Nether-2-lijst (~15 min) die daar blokkerend wordt afgelopen.

**Reden:**
- Of de zwamkathedralen werken als zwamkathedralen kan RCON niet meten: halgrootte, wanden en doorgangen, zuilen als blikvanger, of je er kunt reizen zonder alles te overbruggen, en of de sporengrotten genoeg licht en herkenbaarheid hebben.
- Change 36 stemt fog-dichtheid en particles af op de halgrootte.
- Nether-3 hergebruikt het zuilfeature en het lavaniveau-mechanisme.
- Later bijstellen betekent die changes opnieuw afstemmen.
- Alles wat wél automatisch kan, staat onder S: hoogtes, lava, wanden, aandelen, tarieven, determinisme en ring 1 ongewijzigd.

Blokkerend in teststop 3 (runClient, testseed):
- [ ] `/ca tpring 2 nether` in 4 richtingen: hoge open hallen met wanden en ribben, geen eindeloze vlakte en geen doolhof (vraag 2).
- [ ] Zuilen lezen als reuzenzwammen van vloer tot plafond; dichtheid goed (niet bos-dicht, niet zeldzaam); vrijstaande zwammen ertussen (vraag 4).
- [ ] Lagere lavazee met sporenmos-eilanden die als stapstenen werken; oever (dam) bij de grens oogt natuurlijk (vragen 1, 5).
- [ ] Sporengrotten: duidelijk anders dan het woud (lager, dichter, meer schimmel), genoeg licht om te navigeren (vraag 3).
- [ ] Een dauwgeode gevonden binnen ~10 min zoeken (langs wanden graven, of een door een carver opengebroken exemplaar); de holte leest als geode: binnenwand met dauwbronnen en kristalknoppen, niet zwevend in een hal (vraag 6 B; zeldzaamheid beoordelen).
- [ ] `/ca tpboundary 1 nether` en `2 nether`: geen lavawand, geen klif, geen zwevende brokken.

W-achteraf (checklist, niet blokkerend):
- [ ] Portal vanuit overworld-ring 2 → landt in Nether-ring 2 (÷4) op een veilige plek, niet in de lava; terug landt in ring 2.
- [ ] Dauwkristal groeit aan een gegenereerde bron in een geode (`/gamerule randomTickSpeed 100`, speler in de buurt; random ticks vragen een speler). Sporenmos spreidt (gedrag change 33/34).
- [ ] Spawns volgens vraag 8 (striders op de lava, endermen en zombified piglins in de hallen, af en toe een ghast).
- [ ] F3: biome klopt bij het terrein; fog-kleur = placeholder; geen fouten of waarschuwingen in de log.
- [ ] Ghast-vuurballen of lava naast zwamblokken: brandt het woud af? (vlambaarheid is change 33; alleen notitie of backlog).

---

## Risico's / aandachtspunten

- **Vlak lavaniveau naast de dam:** een carver (`nether_cave`) uit een chunk van ring 1 kan tot ~8 chunks in de dam snijden. Dat geeft een lavavlak naast lucht (vanilla-gedrag; stroomt bij een update). Gemeten in de wand-test; mag de dam gaten hebben, dan carvers in de zone tolereren en documenteren.
- **Router-verplaatsing raakt Nether-ring 1** als de verplaatste `shifted_noise` niet exact gelijk is (legacy-key, `xz_scale`). Daarom de A↔B-vergelijking in S.
- **Dichtheidsschaal:** wijkt de ring 2-invoer sterk af van ±1–2,5, dan wordt de overgang naar ring 1 te kort of te lang (valkuil change 14).
- **Prestatie:** 3D-wanden en grotten plus de vanilla-stapel van ring 1 in elke chunk (zonder ring gate). Meten; budget 1,3×.
- **Determinisme:** de regel uit stap 6 moet in **elke** eigen feature gelden. Eén feature die uitwijkt of buiten de rangorde schrijft, breekt hem. Daarom de permutatietest, de grep in D en de tweewereldentest in S.
- **Feature order cycle:** de vanilla-Nether-features van de kopieën en de eigen ertskopieën moeten in vanilla-volgorde staan; eigen features achteraan (S vangt het).
- **Ertsen in doorweekt netherrack** (vraag 7 A) houden de netherrack-texture. Dat valt op; backlog `[blocks]`.
- **Namen en gedrag uit change 33/34 wijken af** (multiface-schimmel, stalactiet-states, knopstadia, plantbare zwam). Bij de start tegen `CODEBASE.md` leggen. Valt de schimmel niet als multiface uit, dan wordt `mold_patch` een vloer- en plafond-plaatsing van de echte state.
- **Ruimte:** Nether-ring 2 is 500 blokken breed. Met hallen van 60–150 blokken zijn dat 3–6 hallen radiaal. Voelt het krap, dan liggen de knoppen in de ruisschaal (niet in de config-grenzen).
- **Geodes (vraag 6 B):** de wandtest sampelt 26 punten; een smalle gang tussen twee punten kan een geode toch openen (deterministisch; leest als opengebroken geode). Prestatie: de DF rekent per blok buiten de interpolatie, met vroege exit per kolom en een celcache; meten. Zeldzaamheid hangt af van de slagingskans van de wandtest (vooral in de dichte sporengrotten): afstemmen op de meting, niet gokken. Minder vindplaatsen dan losse vloerbronnen: bewaker 2→3 en fase 2 vragen genoeg dauwkristal — tarief in de beslissingenlog.
- **Lava-niveau en `/ca tpring`/portals:** de landingslogica zoekt een stevige vloer onder het dak. Op een eiland of hoed is dat geldig. Vanilla's portal-plaatsing kan op een hoed landen (geaccepteerd).

---

## Ontwerpvragen

Ronde 1 = architectuur (1–4), ronde 2 = oppervlak en bronnen (5–7), ronde 3 = leven en namen (8–9).

### 1. Hoe komt de lagere lavazee er?
De lavazee is in de hele dimensie globaal (Y 31); lager in één ring vraagt een uitzondering.
- **A. Eigen lavaniveau per ring (Nether-ring 2: Y 22) + een vaste "oever" onder Y 33 langs beide ringgrenzen (aanbevolen).** Dat kost twee kleine Java-stukken: een veld `fluid_levels` in de chunk-generator en een density function `ring_edge` die zegt hoe ver je van een ringgrens zit. Er komen geen lavawanden. Het geeft letterlijk het design ("de lavazee ligt lager") en is herbruikbaar voor Nether-3 (bevroren zee) en Nether-4 (droog). Geen multiplayer-impact.
- **B. Lava blijft op Y 31; de hallen liggen hoger (vloer Y 36–50) met lava alleen in diepe bekkens.** Geen Java. "Lager" betekent dan "onder de halvloer", niet een andere zeespiegel; het wijkt licht af van het design.
- **C. Eigen niveau met natuurlijke lavavallen aan de ringgrens** (vloeistof-updates bij generatie inplannen). Spectaculair, maar gevaarlijk bij het oversteken, meer Java in `RingAquifer`, en het eindbeeld ontstaat pas na fluid-ticks (lastiger deterministisch te testen).

Antwoord: **A** — eigen lavaniveau per ring (`fluid_levels`, Nether-ring 2: Y 22) + dam via `ring_edge` onder Y 33 langs beide ringgrenzen.

### 2. Welke vorm krijgen de zwamkathedralen?
- **A. Hoge hallen (vloer Y ~28, plafond Y ~108) gescheiden door dikke wanden en ribben met bogen en doorgangen (grootschalige 3D-ruis); de zuilen komen als feature (aanbevolen).** Het kathedraal-ritme komt van zaal na zaal, en grote ruimtes blijven verkenbaar. Kost wat 3D-ruis (prestatie meten).
- **B. Eén doorlopende holte** (alleen vloer en plafond, geen wanden). Maximaal open en goedkoop; de zuilen zijn de enige structuur. Ghasts zien je van ver, en het leest eerder als een hal zonder einde dan als kathedralen.
- **C. Vanilla-Nether-ruis, verticaal uitgerekt** (hogere grotten). Vertrouwde Nether-vormen, maar weinig controle over vloer, plafond, lavaniveau en dam, en het verschil met Nether-ring 1 is klein.

Antwoord: **A** — hoge hallen (vloer Y ~28, plafond Y ~108) gescheiden door dikke wanden en ribben (3D-ruis); zuilen als feature.

### 3. Hoe onderscheiden reuzenzwamwoud en sporengrotten zich?
- **A. Twee regio's met eigen terrein via één regio-as in de router (aanpak change 23): het woud = hoge hallen (~65 %), de sporengrotten = lager en dichter grottenstelsel (plafond Y 60–80, kamers en tunnels, meer schimmel en bronnen) (aanbevolen).** De biomegrens valt op de terreinovergang en de naam "grotten" klopt. Puur JSON (behalve de vragen 1 en 4).
- **B. Zelfde terrein, alleen andere decoratie** (eigen as die het terrein niet leest). Eenvoudiger, maar de twee biomes zijn alleen aan begroeiing te onderscheiden.
- **C. In lagen: de sporengrotten onder de halvloer.** Echt "onder", maar in 128 blokken hoogte gaat dat ten koste van de halhoogte, en het botst met de lavazee en de dam.

Antwoord: **A** — twee regio's via één regio-as in de Nether-router: woud (~65 %, hoge hallen) en sporengrotten (plafond Y 60–80, dichter grottenstelsel).

### 4. Hoe bouwen we de reuzenzwammen als zuilen?
- **A. Eigen feature `giant_fungus` met twee vormen: een zuil van vloer tot plafond (steel 3–7 breed, 1–3 hoedschijven, kraag tegen het plafond, zwamlampen, druppelende stalactieten) en een vrijstaande reuzenzwam (8–20 hoog) (aanbevolen).** Leest echt als zwammenzuil en is deterministisch door de lees- en schrijfregel (stap 6). Herbruikbaar voor `rime_forest` (Nether-3). Kost een paar Java-classes en een vormtest.
- **B. Zuilen als terrein** (density function met ronde kolommen op een verschoven raster) en vrijstaande zwammen via vanilla `huge_fungus`. Zuilen gegarandeerd van vloer tot plafond, maar van (doorweekt) netherrack, niet van steel en hoed: het leest als rots.
- **C. Alleen vanilla `huge_fungus` met eigen blokken.** Geen Java, maar max. ~26 hoog (geen zuilen tot het plafond), en vanilla's plaatsing (`count_on_every_layer`) is buurchunk-afhankelijk, dus strijdig met de determinisme-eis.

Antwoord: **A** — eigen feature `giant_fungus` met zuilvorm (vloer tot plafond) en vrijstaande vorm.

### 5. Hoe ontstaan de sporenmos-eilanden op de lavazee?
- **A. Uit het terrein: in de lavabekkens rijst de vloer plaatselijk net boven de lava, en de surface rule legt er sporenmos op (aanbevolen).** Deterministisch, gratis, de eilanden zijn stapstenen door de bekkens en sporenmos kan er later op spreiden.
- **B. Feature: drijvende matten** (1 laag sporenmos direct op de lava). Leest als "deels bedekt", maar de matten hangen nergens aan (spreiding/fallback van sporenmos naar doorweekt netherrack kan vreemd ogen) en het kost een eigen feature.
- **C. Beide.** Meeste variatie, meeste afstemwerk.

Antwoord: **A** — uit het terrein: in de lavabekkens rijst de vloer net boven de lava; surface rule legt er sporenmos op.

### 6. Hoe zien dauwbronnen eruit en hoe vaak komen ze voor?
Dauwkristal is het sleutelmateriaal voor bewaker 2→3 en de basis van hittebescherming (fase 2); het groeit langzaam aan de bronnen.
- **A. Een bron in de vloer: 1–4 dauwbronblokken verzonken in de vloer, met 1–3 voorgegroeide kristalknoppen; woud ~1 per 6 chunks, sporengrotten ~1 per 3 chunks (aanbevolen).** Zichtbaar en vindbaar (binnen ~100 blokken altijd één), de eerste bezoeker vindt meteen een paar knoppen, en farmen = terugkomen. In multiplayer delen spelers de bronnen, dus het tarief is later via data bij te stellen.
- **B. Verborgen dauw-geodes** (vanilla `minecraft:geode` met dauwblokken, zoals amethist). Geen Java en "zoals amethist" letterlijk, maar verstopt (graven), niet "water-achtig en druppelend" in het zicht, en de geode leest buren (generatievolgorde).
- **C. Bronnen in wanden en plafonds** (sijpelend, kristallen hangen eronder). Het meest "druppelend", maar lastiger te bereiken en te farmen, en het vraagt een wandscan.

Antwoord: **B (keuze gebruiker, aangepast)** — dauwbronnen in **geodes**: holle bollen in de wanden (zoals amethist-geodes), zeldzamer en groter. Niet met vanilla `minecraft:geode` (verboden: leest de omgeving, volgorde-afhankelijk), maar deterministisch: de **holte** komt uit een eigen density function (`corruptedadventure:dew_geodes`, puur van seed en positie, met een wandtest op de terreinfunctie), de **bekleding** uit een eigen feature `corruptedadventure:dew_geode` (binnenwand met `dew_source`, voorgegroeide knoppen via `GrowthPriority`); alleen bestaande blokken (schil = `soaked_netherrack`). Zeldzaamheid (afstemwaarde): woud ~1 geode per 10 chunks, sporengrotten ~1 per 5 chunks. Zie stap 3b en 7.

### 7. Hoe diep gaat het doorweekte netherrack, en welke ertsen zitten erin?
- **A. De hele ring doorweekt (ook wanden en kern), met vanilla's Nether-ertsen (kwarts, goud, ancient debris, grind, blackstone, magma) als eigen kopieën met doel `soaked_netherrack` en vanilla-tarieven (aanbevolen).** Sterke ring-identiteit (geen rode wanden in een groengrijze Nether), en netherite blijft volgens §1.11 overal "zoals vanilla". Nadeel: de ertsen houden tot een `blocks`-change de netherrack-texture (backlog).
- **B. Alleen de oppervlaktelagen doorweekt** (vloer 3–5, plafond 2–4); wanden en kern blijven netherrack. Vanilla-ertsen werken zonder kopieën, maar wanden zijn rood, wat in een natte schimmelwereld vreemd oogt.
- **C. De hele ring doorweekt, zonder vanilla-ertsen.** Puur thema; kwarts, goud en ancient debris alleen in Nether-ring 1 (minder reden om ring 2 te mijnen, en netherite is daar niet te vinden).

Antwoord: **A** — de hele ring doorweekt, met kopieën van vanilla's Nether-ertsen (doel `soaked_netherrack`, vanilla-tarieven).

### 8. Welke mobs en structures zijn er in Nether-ring 2 tot fase 3/4?
- **A. Vanilla-mix: zombified piglins, endermen, striders op de lava, af en toe een ghast (de hoge hallen); geen vanilla-structures (geen `minecraft:`-tags) (aanbevolen).** De ring leeft en blijft gevaarlijk, en ghasts passen bij de hallen. Structures volgen in fase 3 (backlog), de eigen mobs in fase 4 (sporenmonsters, slijmwezens).
- **B. Geen spawns en geen structures tot fase 3/4.** Rustig testen, maar de ring voelt dood en het dauwkristal is risicoloos te halen.
- **C. Zoals A, plus vanilla fortress, bastion en ruined portals via `#minecraft:is_nether` / `has_structure/bastion_remnant`.** Meer te vinden (blazes, loot), maar vanilla-structures zijn eigenlijk fase 3, en een fortress van rood netherrack-baksteen botst met het thema.

Antwoord: **A** — vanilla-mix (zombified piglins, endermen, striders, af en toe een ghast); geen vanilla-structures.

### 9. Hoe heten de twee biomes (registry-naam, Engels / Nederlands)?
- **A. `dripshroom_forest` "Dripshroom Forest" / "Druipzwamwoud" en `spore_caves` "Spore Caves" / "Sporengrotten" (aanbevolen).** Sluit aan op de houtset "druipzwam" (aangenomen `dripshroom_*` in change 33); kort en ring-herkenbaar.
- **B. `giant_fungus_forest` / `glowing_spore_caves`.** Beschrijvend, los van de houtnaam; lang.
- **C. `soggy_fungal_forest` / `spore_grottos`.** Benadrukt "doorweekt"; minder direct.

Antwoord: **A** — `dripshroom_forest` "Dripshroom Forest"/"Druipzwamwoud" en `spore_caves` "Spore Caves"/"Sporengrotten".

### 10. Welk prestatiebudget krijgt de noise-stap van Nether-ring 2? (nieuw, 2026-10-05; beantwoord 2026-10-05)
Dit plan zegt ≤ 1,3× vanilla-Nether. Ring 2 in de overworld mocht na 23b van 1,3× naar 1,75× (nu 1,54×). De geode-density function (stap 3b) en de hallen zijn zwaar, en het plan zegt: halen we het budget niet, dan terug naar de gebruiker.
- **A. ≤ 1,75× vanilla-Nether, hetzelfde als ring 2 (aanbevolen).** Eén norm voor "ring 2", meer ruimte voor hallen, wanden, grotten en geodes zonder terugkoppeling; de Nether is kleiner dan de overworld, dus de absolute tijd blijft laag. Risico: de ring voelt zwaarder bij een zwakke server (meer ms per chunk) — in multiplayer meetbaar, niet merkbaar bij normale spelers.
- **B. Blijft ≤ 1,3×.** Strakste norm en de lichtste servers, maar het dwingt kleinere hallen/minder 3D-wanden of een lichtere geode-DF af en vergroot de kans op een stop-en-vraag-punt tijdens de bouw.
- **C. ≤ 1,5×.** Tussenweg: iets meer ruimte dan nu, minder dan overworld ring 2; als het niet haalbaar blijkt, ook weer een stop.

Antwoord: **A** — noise-stap Nether-ring 2 ≤ 1,75× vanilla-Nether, gelijk aan ring 2 in de overworld.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Lavazee Nether-ring 2: eigen niveau per ring via `fluid_levels` + `ring_edge`-dam (startwaarde Y 22; dam onder Y 33 binnen 48 blokken van de ringgrenzen) | Letterlijk het design, geen lavawanden; herbruikbaar voor Nether-3/4 |
| 2026-10-01 | worldgen | Zwamkathedralen: hoge hallen met wanden en ribben (3D-ruis), zuilen als feature; vloer Y ~28, plafond woud Y ~108, grotten Y ~70 | Kathedraal-ritme, verkenbaar |
| 2026-10-01 | worldgen | Biomes Nether-ring 2: woud (hoge hallen) en sporengrotten (lager, dichter) via een regio-as in de Nether-router (`ring_blend/nether_temperature`) + `multi_noise`; woud ~65 % | Zelfde mechanisme als ring 2 in de overworld (change 23); biomegrens = terreinovergang |
| 2026-10-01 | worldgen | Reuzenzwammen: eigen feature `giant_fungus` (zuil + vrijstaand) | Leest als zwammenzuil; deterministisch; herbruikbaar voor Nether-3 |
| 2026-10-01 | worldgen | Determinismeregel voor Nether-2-features: lezen alleen terrein (groeisels doorzichtig), schrijven op vaste rang (steel > hoed > lamp > stalactiet > knop > schimmel), nooit uitwijken; geen `count_on_every_layer`/`random_patch`/`multiface_growth`/`block_column`/vanilla `geode`; holtes alleen uit density functions | Worldgen moet per seed hetzelfde zijn, ongeacht de laadvolgorde (valkuil change 16) |
| 2026-10-01 | worldgen | Sporenmos-eilanden uit het terrein (bekkenvloer net boven de lava) + surface rule | Deterministisch en gratis |
| 2026-10-01 | worldgen | Dauwbronnen (keuze gebruiker): in eigen deterministische dauwgeodes — holte uit density function `dew_geodes` (wandtest op het terrein), bekleding (`dew_source` ~6 % van de binnenwand + knoppen) uit feature `dew_geode`; woud ~1 per 10 chunks, grotten ~1 per 5 (+ gemeten tarief) | "Zoals amethist" en zeldzamer/groter; vanilla `geode` is volgorde-afhankelijk; holtes uit een feature zouden de determinismeregel breken |
| 2026-10-01 | worldgen | Doorweekt netherrack en ertsen: hele ring doorweekt, vanilla-Nether-ertsen als kopieën met doel `soaked_netherrack` | Sterke ring-identiteit; netherite blijft zoals vanilla |
| 2026-10-01 | worldgen | Nether-ring 2 zonder lavabronnen, vuur, glowstone en basalt | Lavabronnen lezen hun buren (generatievolgorde) en passen niet bij het natte thema; licht komt van eigen blokken |
| 2026-10-01 | worldgen | Spawns en structures Nether-ring 2: vanilla-mix, geen vanilla-structures | Ring leeft; structures zijn fase 3 |
| 2026-10-01 | worldgen | Biome-namen Nether-ring 2: `dripshroom_forest`/Druipzwamwoud, `spore_caves`/Sporengrotten | Sluit aan op de houtset; kort |
| 2026-10-01 | worldgen | Fog/water van beide biomes uit één `BiomeAmbience.Palette.NETHER_RING_2` (placeholder-kleuren); definitief in change 36 | Eén plek voor de sfeerchange |

Commit (na `/code-review`): `worldgen: add nether ring 2 fungal cathedrals, dripshroom forest and spore caves, dew geodes`.
