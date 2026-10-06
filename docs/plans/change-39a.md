# Change 39a — Ring 3 regio-assen, router en DF-stubs

Scope: `worldgen` — werkpakket 39a (golf 0, sequentieel, na 37c; eigenaar W-terrein, Nether-deel W-nether)

**Doel:** de gedeelde worldgen-sleutels uit `docs/plans/ring-3/contract.md` §4 laten bestaan, zodat W-terrein, W-biomes en W-nether in golf 1 parallel in eigen worktrees kunnen laden en testen. Dit pakket maakt:
- de twee regio-assen van ring 3 (overworld) en de regio-as van Nether-3 (echte ruis);
- de router-sleutels die ze in `ring_blend` invoeren;
- stubs (constante 0) voor alle andere gedeelde DF's;
- het Nether-3-lavaniveau in de preset.

Geen terreinvorm, geen biomes: de placeholder-biomes en het placeholder-terrein blijven tot 39/39b/53.

Bron: `contract.md` §4; `fase-1-wereld.md` §1.2 (klimaat per ring via de router), §1.10 (vlekken ~500–1.000 blokken); sjabloon `ring_2/overworld/relief`/`moisture` (change 23/30).

## Plan
1. **Noises:**
   - `noise/ring_3_heat.json` en `noise/ring_3_ground.json`: firstOctave −10, amplitudes als `ring_2_moisture` (vlekken ~500–1.000 blokken; ring 3 is breder, dus mogelijk −11; meten). **Gemeten: −11** (zie Uitvoering).
   - `noise/nether_ring_3_region.json` (÷4-schaal zoals Nether-2). **Gemeten: −9** (÷4 van −11).
2. **Regio-assen** (`flat_cache` van de ruis, zonder offset):
   - `density_function/ring_3/overworld/region_heat.json`;
   - `ring_3/overworld/region_ground.json`;
   - `ring_3/nether/region.json`.
3. **Router:**
   - in `ring_blend/overworld_temperature.json` → ring 3 = `region_heat`; in `_vegetation.json` → ring 3 = `region_ground`;
   - in de Nether-router (zoals change 35) → ring 3 = `ring_3/nether/region`.
   - Ring 1/2 blijven blok voor blok gelijk.
   - **Derde as (V8 = A):** een derde router-veld (`weirdness` via een nieuwe `ring_blend/overworld_weirdness`, ring 1 = vanilla letterlijk, ring 3 = `caldera_dormant`, rest 0). Ring 1 moet identiek blijven: test zoals `vein_toggle`.
4. **Stubs** (constante `0`, flat): `ring_3/overworld/height`, `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`; `ring_3/nether/terrain` blijft voorlopig de placeholder (die bestaat al als `ring_3/nether.json`; W-nether verplaatst hem in 53).
5. **Preset:** `fluid_levels.ring_3` (Nether) volgens het contract: geen lavazee. Een niveau onder de vloer (bijv. Y 8); W-nether mag het in 53 bijstellen. **Eerst Y 8; na review 3 en antwoord gebruiker (optie A) Y 22**, gelijk aan ring 2 (zie Review 3).

## Client/server-impact
Geen (alleen worldgen-data, deterministisch per seed).

## Tests
- **B** + bestaande worldgen-tests (`RingBlendTest`, de JSON-tests van ring 2).
- **D** (geen wijziging).
- **S**: verse wereld; `/ca tpring 3` en `tpring 3 nether` werken; terrein in ring 3 is nog het placeholder-terrein.
- Ring 1/2 ongewijzigd: biomes op 300 punten en steen/deepslate in 6 chunks gelijk aan vóór 39a (methode change 23).
- Regio-assen: een korte PNG of tabel van `region_heat`/`region_ground` over r 3.500–6.000 (vlekgrootte), en de verdeling (histogram) als input voor 39b.

## Open
- Niets: V8 = A (2026-10-06). Stap 3 hoort erbij; ring 1 moet blok voor blok gelijk blijven (biomes op 300 punten, ook de vanilla-weirdness-afhankelijke biomes).

Commit: `worldgen: add ring 3 region axes and shared density function stubs`.

## Uitvoering (2026-10-06)

**Gebouwd**
- Noises `ring_3_heat`, `ring_3_ground` (firstOctave −11, [1, 0,5, 0,25]) en `nether_ring_3_region` (−9, [1, 1, 0,5]).
- Regio-assen `ring_3/overworld/region_heat`, `region_ground`, `ring_3/nether/region`: `flat_cache` van de ruis (`xz_scale` 1, `y_scale` 0, geen offset).
- Stubs (`0.0`): `ring_3/overworld/{height,caldera_mask,caldera_dormant,lava_channel,island_mask}`. `ring_3/nether/terrain` niet: de placeholder `ring_3/nether.json` blijft de ring 3-invoer van `ring_blend/nether` tot 53 (zoals gepland; `golf-1-nether.md` stap 1 verplaatst hem).
- Router: `ring_blend/overworld_temperature`/`_vegetation` ring 3 → de regio-assen; `ring_blend/nether_temperature` ring 3 → `ring_3/nether/region`; nieuw `ring_blend/overworld_weirdness` (ring 1 `minecraft:overworld/ridges`, ring 3 `caldera_dormant`, rest 0) in router-`ridges` van `noise_settings/overworld.json`.
- `ring_blend` kreeg een optioneel veld `switch_at` (na de reviews): in de zones van die grenzen geen menging maar de invoer van `RingPosition.ring()`, hard op de middellijn (de regel van `RingBiomeSource`). Gezet op het router-klimaat bij grenzen zonder overgangsbiome: overworld `temperature`/`vegetation`/`weirdness` `[ring_3, ring_4, ring_5]`, Nether `temperature` `[ring_2, ring_3, ring_4, ring_5]`. De constructor valideert nu ook (ontbrekende ring, `void`).
- Preset: Nether `fluid_levels` `{ring_2: 22, ring_3: 22}`.
- `tools/nether-lava-walls.py`: RCON-test op lavawanden aan een Nether-ringgrens (wandtelling als vaste test voor 53).
- Tests `Ring3JsonTest` (bedrading, `switch_at` op precies de grenzen zonder overgangsbiome, bestaan van alle contract-DF's, lavaniveau + dam, vlekgrootte en onafhankelijkheid van de assen), `RingBlendTest` (`switch_at`, codec, validatie, Nether-zone 2→3), `RingColumnCacheTest` (klimaat en biome source zien op elke quart-positie dezelfde ring); testhulp `worldgen/WorldgenData` (ook in `Ring2RegionSharesTest`, `NetherRing2JsonTest`, `VeinToggleJsonTest`).
- Contract §8: `NetherRing3BiomeNames` (W-nether) en de andere 37c-haken per stroom.

**Afwijkingen van het plan**
- **firstOctave −11 i.p.v. −10** (plan: "mogelijk −11; meten"). Maat = gemiddelde loop langs een lijn tot de biome wisselt (seed 20260930, 60 lijnen van 120.000 blokken). Ring 2's vijf biomes (goedgekeurd bij teststop 3) op −10: **497**. Ring 3 met 8 biomes op 2 assen (3×3 terciel / 2×4 / 3+4-indeling): op −10 **311 / 301 / 344**, op −11 **581 / 572 / 652** (tercielen op 0,4307·sd: 311 → 573). −10 zou de vlakken ~35 % kleiner maken dan in ring 2; −11 ligt ~20 % erboven en binnen §1.10 (~500–1.000). Nether-3 op dezelfde voet (÷4 → −9), geseed zoals de Nether (legacy-random): drie banden (tercielen op 0,4307·sd, sd 0,30) **253** Nether-blokken (op −8: 134); Nether-2 (twee biomes, −8, drempel −0,1): **243**. W-biomes/W-nether stemmen de drempels af; de octaaf kan in hun stroom nog via main.
- Verdeling van de assen in ring 3 (r 3.500–6.000, raster 32): heat gemiddeld 0,035, sd 0,246; ground 0,014, sd 0,277; correlatie 0,13 (~100 vlekken in de ring: steekproefruis). Histogram heat (−1..1, stap 0,2): 0,000 · 0,006 · 0,043 · 0,118 · 0,252 · 0,339 · 0,182 · 0,047 · 0,013 · 0,000, dus 99 % binnen ±0,6. **Input voor 39b:** drempels in de orde van ±0,1 (tercielen ≈ ±0,11/±0,12), niet ±0,5.
- Een stub-bestand is een kale `0.0` (zoals vanilla's `minecraft:zero`); geen `flat_cache` nodig.

**Tests**
- B: `./gradlew build` groen (eindstand: 80 testklassen, 420 tests, incl. `ClientIsolationTest`, `Ring3JsonTest`).
- D: `runData` groen; diff in `src/generated` leeg (39a heeft geen datagen).
- S: `tools/server-smoke-test.sh`, wereld `smoke-change39a` (na review 1 `smoke-change39a-b`, na review 2 `smoke-change39a-d`): start zonder fouten (preset, stubs en `ring_blend` met `switch_at` decoderen).
- RCON (wereld `smoke-change39a-a`): `locate biome` `placeholder_ring_3` vanaf 4.750 → 0 blokken, `canyonlands` 295, `salt_flats` 404, `dry_riverbeds` (3.500) 0, `river` vanaf (300, 0) 90; Nether: `placeholder_nether_ring_3` (1.187) 0, `dripshroom_forest` 0, `spore_caves` 45. `ca ring`: 4.750 → ring 3; Nether 1.187/897 → ring 3. Biomes op ring 3-punten (3 gebieden × 25): alles `placeholder_ring_3` (verwacht tot 39b).
- Nether-3-lava: 16 chunks in de ring 3-helft van de zone 2→3 (8 richtingen, r 890/905): lava Y 9–32 **0**, lucht Y 9–32 **0** (alles vast onder Y 33: placeholder + dam van ring 2); lava Y 0–8: 33 blokken in 1 chunk. Het niveau is pas te zien als 53 daar ruimte opent.
- **Determinisme** (methode 37c, vallende/stromende blokken eerst geteld): 15 overworld-chunks (ring 1 ×5, zone 1→2, ring 2 ×5, zone 2→3, ring 3 ×3; 27 bloktypen, Y −64..319) en 11 Nether-chunks (ring 1 ×3, ring 2 ×4, zone 2→3 ×2, ring 3 ×2; 12 bloktypen), plus biomes op 800 punten (21 overworld-gebieden, waarvan 11 in ring 1 met o.a. rivier, strand, savanneplateau, bamboejungle, lush/dripstone caves; 11 Nether-gebieden, incl. de zone 2→3).
  - HEAD (`smoke-39a-base`) ↔ 39a (`smoke-change39a-a`), zelfde laadvolgorde: **1.337/1.337 identiek** (842.929 blokken).
  - Omgekeerde laadvolgorde: 39a-rev ↔ HEAD-rev (`smoke-39a-base-rev`) 1.309/1.337; 39a-rev ↔ 39a vooruit 1.278/1.337. Alle verschillen zitten in vanilla-ertsklodders (andesiet, dioriet, graniet, tuf, kolen, ijzer, aarde, grind, blackstone; steen/deepslate eromheen), vooruit ↔ omgekeerd ook in vanilla-water en één eik. **Dezelfde soort verschillen zit tussen de twee HEAD-werelden**, dus niet van 39a. Alle mod-blokken en alle biomes: identiek.
  - Nether-zone 2→3: biomes aan de ring 2-kant identiek op de gemeten punten. Let op: zonder overgangsbiome kiest ring 2's `multi_noise` daar op de gemengde temperatuur (ring 2-regio ↔ ring 3-regio); dat kan op andere punten verschuiven (zelfde situatie als zone 1→2 sinds 35).
- **Prestatie** (JFR `minecraft.ChunkGeneration`, stap `noise`, 16×16 chunks per gebied, mediaan per ronde in ms/chunk; daarvan de mediaan over de rondes):

  | Gebied | HEAD (r3, r4) | 39a (r1–r4) | vanilla (r1, r2) | 39a / vanilla |
  |---|---|---|---|---|
  | ring 1 (−500, −700) | 46,9 | 49,4 | 38,7 | 1,28× |
  | ring 2 (2.400, 1.200) | 52,4 | 54,2 | 34,9 | 1,55× |
  | ring 3 (4.750, 1.600) | 31,4 | 30,7 | 37,0 | 0,83× |
  | ring 3b (−3.300, −3.300) | 32,1 | 31,7 | 35,5 | 0,89× |
  | Nether-3 (−1.180, 300) | 5,5 | 5,6 | 3,9 | 1,44× (absoluut klein) |

  39a = HEAD binnen de ruis (±4 %): de nieuwe invoer kost alleen een paar `flat_cache`-vullingen per chunk. Alles ≤ 1,75×. Indicatief: op de machine liep een ander proces (python, ~1,3 kern), en de eerste HEAD-ronde lag ~30 % lager dan alle latere rondes. De officiële meting doet INT.

**Na review 1** (tussenstand met `ring_select`, wereld `smoke-change39a-c`; vervangen door `switch_at`, zie review 2):
- c ↔ a (vóór de fix) 1.326/1.337, c ↔ HEAD 1.326/1.337. Verschillen: (1) Nether (875, 0) in de zone 2→3: 3 van 25 biomepunten `dripshroom_forest` → `spore_caves`, `spore_moss` 19 → 53, `soaked_netherrack` 5.059 → 5.025: de bedoelde wijziging (de ring 2-kant volgt nu zijn eigen regio); (2) overworld (900, 900) en (−800, 600) in ring 1: deepslate/tuf/grind/steen en water (60 → 30, 1.511 → 1.496): dezelfde chunks en soorten als in de omgekeerde-volgorde-werelden van HEAD; de overworld-data is tussen a en c byte-gelijk, dus thread-volgorde van vanilla-features, niet de change.
- **Hele Nether-zone 2→3** (8 gebieden van 80 × 80 rond r 875, raster 4: 2.888 punten): ring 2-kant 1.346 punten, waarvan 881 eenduidig (alle 3×3 quart-cellen eromheen aan dezelfde kant van de drempel, ≥ 8 blokken van de middellijn): **881/881** biome = drempel op `ring_2/nether/region` (offline nagerekend met de legacy-random van de Nether, seed 20260930). Ring 3-kant 1.542/1.542 `placeholder_nether_ring_3`.

## Review (/code-review medium, 9 bevindingen)
1. **Nether-zone 2→3: ring 2's biome op een gemengde as** (lerp naar de Nether-3-regio, terwijl het terrein ring 2's `region_weight` volgt). Eerst opgelost met een apart DF-type `ring_select`; na review 2 vervangen door `switch_at` op `ring_blend` (zie daar). Zone 1→2 blijft exact de menging van 35. **Raakt gecommit ring 2-gedrag** in de ring 2-helft van zone 2→3 (vóór 39a `lerp(regio, 0)`): bewust, in de richting van de review; vastgelegd in de beslissingenlog. Zone 1→2 heeft hetzelfde patroon sinds 35 (backlog).
2. **Lavasprong 22 → 8 op de middellijn.** Bestond al sinds 35 als 22 tegen de standaard 32. Nu dicht door de dam van ring 2 (vast tot Y 29–33 op en voorbij de grens; 0 lucht op Y 9–32 in 16 zonechunks). Niet in `RingFluidPicker` geïnterpoleerd: dat verschuift ook ring 1/2-lava in de zones (ring 1-kant van 1→2) en maakt traptreden. Vastgelegd als **harde eis voor 53** in `contract.md` §4 en `golf-1-nether.md` (dam heel, geen open ruimte Y 9–32 over de middellijn, carvers niet door de dam, S-test lava-met-luchtbuur → 0); `Ring3JsonTest` bewaakt dat de dam boven beide niveaus reikt.
3. Nether-3-vlektest met vaste grens ±0,15 → nu 0,4307·sd; octaaf herbevestigd (−9: 269, −8: 121; Nether-2: 237).
4. Afstemdoelen op één seed → nu structureel over een groot gebied: ring 3-vlakken ≥ die van ring 2 (op de echte preset-drempels), Nether-3 ≥ Nether-2; gecentreerd, onafhankelijk (|r| < 0,05), > 95 % binnen ±0,6.
5. Dubbele ruisevaluatie in `meanRun` → één cel-functie per punt.
6. Histogram/printf zonder asserts → weg; "binnen ±0,6" is nu een assert (input 39b staat in dit plan).
7. Gedupliceerde JSON-/noise-hulp → `worldgen/WorldgenData` (testhulp), ook gebruikt in drie bestaande tests.
8. Weirdness 0 in ring 2 en in zones → in `contract.md` §4 vastgelegd waarom ring 2 = 0 veilig is (elke ring 2-biome weirdness [−2, 2], zones 1→2/2→3 onder overgangsbiomes; test in `Ring3JsonTest`) en het aandachtspunt voor 39b (ring 3-kant van zone 3→4 tot `steam_fens`).
9. Deze sectie.

Fix 1 raakt gecommit ring 2-gedrag → nieuwe `/code-review medium` op de diff.

## Review 2 (/code-review medium, 10 bevindingen)
- **Kern (1, 3, 4, 7): één mechanisme i.p.v. een select per slot.** `ring_select` (`RingSelect`, eigen registratie, `nether_temperature_regions.json`) is weg. `ring_blend` kreeg `switch_at` (zie Gebouwd); het slaat in die zones de tweede invoer over (geen dubbele evaluatie) en deelt codec, validatie en `mapAll` met de menging (geen duplicatie). Toegepast op Nether-zone 2→3 en verder, en op het overworld-klimaat vanaf zone 3→4 (punt 1: daar mengde ring 3's klimaat naar 0, waardoor de middelste biome werd overgekozen en `dormant_caldera` langs de buitenrand verdween; nu goed vóór 39b).
- 2 (Nether-zone 1→2 mengt nog): niet gewijzigd (goedgekeurd ring 2-gedrag). Backlog aangevuld: met het mechanisme één regel (`ring_1` in `switch_at`), maar het verandert de generatie van ring 1/2 in die zone: eigen change + teststop.
- 5: `missingRingIsRejected` decodeert nu ook via de codec; `switch_at` met `void` wordt door codec en constructor geweigerd.
- 6: de publieke constructor valideert (ontbrekende ring, `void`).
- 8: Nether-ruis in `Ring3JsonTest` legacy-geseed zoals de game; herbevestigd: Nether-3 253 tegen Nether-2 243 (op −8: 134).
- 9: `RingColumnCacheTest.climateAndBiomeSourceSeeTheSameRingAtEveryQuart`: op elke quart-positie (blokcoördinaat `QuartPos.toBlock`) over zone 2→3, overworld en Nether, geeft de cache van het klimaat dezelfde `ring()` als `RingLayout.sample` van de biome source.
- 10: change-tracking-commentaar uit de nieuwe code en tests gehaald.

**Na review 2** (eindstand, wereld `smoke-change39a-f`; de eerste poging `-e` stopte door geheugengebrek van de machine (JVM `malloc` faalde terwijl een ander proces ~3 GB gebruikte), niet door de mod):
- B groen (80 klassen, 420 tests), D diff leeg, S `smoke-change39a-d` zonder fouten.
- f ↔ HEAD 1.326/1.337: Nether (875, 0) zone 2→3: 3 biomepunten en `spore_moss`/`soaked_netherrack` (de bedoelde wijziging, gelijk aan c); overworld (900, 900) en (−800, 600): deepslate/tuf/grind/steen/water. f ↔ c 1.334/1.337, alleen die overworld-chunks (water 30 → 64, deepslate 10.689 → 10.900): dezelfde code geeft daar per run iets anders, dus thread-volgorde van vanilla-features (ook tussen de twee HEAD-werelden gezien). Alle mod-blokken in ring 1/2 en alle overige biomes identiek.
- Hele Nether-zone 2→3 (2.888 punten): ring 2-kant **881/881** eenduidige punten = drempel op de eigen regio (267 daarvan binnen de zone); ring 3-kant 1.542/1.542 placeholder.
- Overworld-zone 3→4: beide kanten nog placeholder-biomes; de werking van `switch_at` daar is unit-getest (`RingBlendTest`, `RingColumnCacheTest`) en wordt zichtbaar in 39b.

## Review 3 (/code-review medium, 8 bevindingen) en afhandeling
1. **Lavatrede 8 → 32 op de Nether-middellijn 3|4** (nieuw door 39a). Bij het meten bleek ook 2|3 echte wanden te hebben: de carvers van ring 2 snijden door de dam tot in de ring 3-helft. Gemeten in 9 chunks over de middellijn 2|3 (Y 9–32, lava met een horizontale luchtbuur): Y 8 → **195**, standaard 32 (HEAD, sinds 35) → **81**, Y 22 → **0**. Voorgelegd aan main; **antwoord gebruiker: optie A, `fluid_levels.ring_3` = 22** (gelijk aan ring 2). Geen trede op 2|3, en ook de 81 oude wanden zijn weg. Trede 3|4 (22/32) is nu afgedekt door het placeholder-terrein en vastgelegd als harde eis voor 53 (contract §4, `golf-1-nether.md`).
2. **`switch_at` met de hand ↔ `transitions`:** `Ring3JsonTest.climateSwitchesExactlyAtBoundariesWithoutATransitionBiome` vergelijkt nu met de `transitions` van de preset (uitzondering Nether 1|2, backlog), en `contract.md` §4 heeft een harde regel voor 39b/48: wie een overgangsbiome toevoegt, haalt die grens in dezelfde change uit `switch_at`. Afleiden uit één bron in code zou de codecs van `RingBlend` en `RingBiomeSource` aan elkaar koppelen: niet gedaan.
3. `RingColumnCacheTest`: de reikwijdtes komen nu uit `DEFAULT_OUTER_RADII`; elke grens (overworld + Nether) wordt gedekt.
4. Nether-zone 1|2: bewust niet gewijzigd (goedgekeurde ring 2-generatie); backlog en deze sectie zeggen dat.
5. `switch_at`-codec weigert dubbele grenzen (test).
6. `Stats` in één pass (beide assen + correlatie).
7. Ongebruikte `JsonParser`-import weg uit `NetherRing2JsonTest`.
8. Dam-test hing aan de JSON-nesting. Nu `everyNetherLavaStepIsCoveredBySolidGround`: voor elke trede tussen Nether-niveaus (standaard = `sea_level` uit `noise_settings/nether.json`) moet de lage kant vast zijn tot boven het hoge niveau. Ring 2: dam (`to_y` van zijn gradiënt, gezocht in de boom). Ring 3: de placeholder-vloer, gesampled met de echte (legacy-geseede) ruis. De test faalt bewust zodra 53 het placeholder vervangt. In de wereld: `tools/nether-lava-walls.py` (nieuw, gecommit).

**Na review 3** (wereld `smoke-change39a-m`):
- B groen (80 klassen, 420 tests), D diff leeg, S `smoke-change39a-l` zonder fouten.
- `tools/nether-lava-walls.py`: grens 2|3 (r 875, 24 chunks): 11.948 lava op Y 9–32, **0 wanden**. Grens 3|4 (r 1.500): 0 lava, **0 wanden**.
- m ↔ HEAD 1.327/1.337: alleen de bedoelde Nether-zone 2|3 (3 biomepunten, `spore_moss`/`soaked_netherrack`) en de bekende thread-ruis in overworld (900, 900). De Nether-lavatellingen van ring 1/2 zijn gelijk aan HEAD.

## W-checklist (kort; teststop 4a)
- [ ] Verse wereld (seed 20260930): ring 1 ziet er hetzelfde uit als vóór 39a (rivieren, stranden, bergen op dezelfde plek).
- [ ] Ring 2, `dry_riverbeds` en Nether-1/2 ongewijzigd.
- [ ] Ring 3 nog placeholder-terrein en -biome (verwacht tot 39/39b); `/ca tpring 3` en `tpring 3 nether` landen goed.
- [ ] Na 39b: biomevlakken in ring 3 voelen even groot of iets groter dan in ring 2 (doel van −11); anders octaaf/drempels via main.
- [ ] Na 53: geen zichtbare lavazee in Nether-3 (niveau Y 22, onder het ijs), geen lavawand aan de grenzen 2→3 en 3→4 (`tools/nether-lava-walls.py`).

## CODEBASE-delta
Verwerkt in `CODEBASE.md` (golf 0 schrijft zelf in de docs): databestanden (router-klimaat, `overworld_weirdness`, `nether_temperature`, regio-assen + noises, stubs, preset `fluid_levels`), `ring_blend`-registryregel, haken per stroom (W-terrein, W-nether), testregel `Ring3JsonTest`.

## Beslissingen
Verwerkt in `02-beslissingenlog.md` en `fase-1-wereld.md` (§1.10, Ring 3 "Router", Nether-ring 3 "Router en lava"): octaven −11/−9, router-weirdness, Nether-3-lavaniveau Y 22 (gelijk aan ring 2), klimaat hard wisselen bij grenzen zonder overgangsbiome (`switch_at`).

## Backlog
- `worldgen`: Nether-zone 1→2: de ring 2-kant kiest zijn biome op `lerp(vanilla, ring_2/nether/region)` i.p.v. de eigen regio (sinds 35). Met het mechanisme één regel (`ring_1` in `switch_at` van `ring_blend/nether_temperature`); verandert de generatie van ring 1/2 in die zone: eigen change + teststop.
