# Ring 4 golf 2 — stroom W-terrein-b (74)

Scope: `worldgen`. Branch `ring4/terrain-b-g2` (worktree, poort n = 8: server 25578, RCON 25588), basis `main` d1a37f11 (na `vf-perf2`). Plan: `golf-2.md` §2 (74), besluiten Q2-A, Q7-A, Q8-A; contract §4; `waves.md` §4–§5. Eén pakket, commits `worldgen:`.

## Architectuur

Alles is terrein uit het ring 4-veld (`IslandField`), deterministisch per seed en zonder chunkgrens-limiet. De drie onderdelen zitten in eigen klassen achter één coördinator **`IslandLandmarks`** (per wereld onthouden in `IslandField.Decisions.landmarks`, een `Memo`); in `IslandField` staan alleen haken:

| Haak | Wat |
|---|---|
| `candidates(i, j)` | `landmarks.withTrees`: kandidaten die in een boom groeien vallen weg, het boomeiland komt in de cel van zijn midden |
| `details(shape)` | een boomeiland (`IslandShape.TREE`) krijgt zijn vallen naast de wortels (`WorldTrees.details`) |
| einde `ensureIslands` | `landmarks.fill(c)`: boomblokken (`Column.tree`), de kom van een hemelmeer (eilandbereik verlaagd, water, carver-wacht), boogrots (`addIsland`) |
| `thin` | `c.tree != null` → boomblok = +1 |
| accessors | `worldTrees` (gevuld), `islandPairs`, `skyLakes`, `hasTree`, `treePart`; records `WorldTree` (+ `radius`), `IslandPair`, `SkyLake` |
| `Column` | `tree`, `skyLakeTop/Floor`, `skyGuardFrom/To` (+ accessors), `lowerIsland`, `guardSky` |

Verder: `IslandShape.TREE` (soort 3, wint `OUTRANK`), `IslandPlacement.fall`/`outlet` package-private (hergebruik), `RingAquifer` 4 arrays + 2 regels (hemelmeerwater, carver-wacht), surface-conditie `corruptedadventure:world_tree` (`WorldTreeCondition`, `ModSurfaceConditions` blok `// ring 4 — W-terrein-b`), blok `world_tree()` in `tools/worldgen/ring4_surface.py`.

Per kolom kost het niets extra buiten de bomen, meren en bogen: één `KeyedSlots`-opzoeking van de cel (`IslandLandmarks.Cell`, wat in een cel van 128 reikt; meestal `Cell.EMPTY`). Per blok alleen een null-check in `thin`; in boomkolommen vergelijkt een blok zijn Y met de bereiken van de kolom (`WorldTreeShape.Slice`, eenmaal per kolom berekend: elk deel is convex, dus één bereik per kolom). Niets in de zones.

## Wereldboom (Q7 = A)
- **Plaatsing** (`WorldTrees.place`): boogcellen van ~2.500 op de binnenstraal van ring 4 (6.000 → **15 cellen**), per cel max. 16 geseede plekken op r 6.230–6.900, de eerste die past: band ≤ −0,2 op de stam en rond het hele bereik (duidelijk mythisch), ring 4 zelf tot 24 voorbij het bereik (nooit een zone), grond Y 44–96, ≥ 80 blokken boom onder Y 298, en een gat dat tot de rand water houdt (zee, of een rand ≥ 66 voor een eigen meer; anders kan geen val in water landen). Testseed: 15 bomen, hoogte 83–138, stam 8,1–11,9, bereik 47–57, elk 3 vallen.
- **Eiland** (`IslandShape.TREE`): diameter 52–68, uitrekking ≤ 1,12, dikte 0,5 × diameter, onderste lob 15–21 boven de hoogste grond eronder; gat, meer, uitloop en grotband zoals elk eiland (61). Andere eilanden binnen het bereik van boom of gat vallen weg (`WorldTrees.clashes`).
- **Vorm** (`WorldTreeShape`): stam 8–12 breed, taps (−30 %) met een uitlopende voet (+3,5, vervalt over ~5) en vijf ribbels; 4 blokken in het eiland. Kruin: koepel over de stam (rx 0,6 × kroonstraal, kroonstraal 0,32 × hoogte, 26–42) + 6–8 takken (gulden hoek rond de stam, 40–80 % van de stamhoogte, onderste het langst) met aan elk eind een bloesemtros (ellipsoïde + twee kleinere). Buitenste schil (1,6 blok) per blok uitgedund (~59 % blad), zodat de rand rafelig is. Wortels: 7–10 buizen (r 2,4–3,6 × stamfactor → ~1,2) van onder het eiland (30–55 % van de straal) naar buiten en omlaag tot 6–14 voorbij de gatrand, 5 blokken de grond in: ze houden het eiland boven zijn gatmeer.
- **Materiaal** (surface rule, eerst in de ring 4-tak): stam `moonpetal_log`, takken en wortels `moonpetal_wood`, kruin `moonpetal_leaves[persistent=true, distance=7]`. Eén `world_tree`-conditie (`any`) ervoor: buiten bomen één test per blok.
- **Vallen langs de wortels**: tot 3, steeds net naast een wortel (wortelstraal + 3) via `IslandPlacement.fall`; alleen waar boom en wortels de bron en de hele valkolom vrijlaten (`WorldTrees.free`). Ze zitten in `details.falls()` van het boomeiland, dus in `fallsOfChunk`/`waterfallsWithin` (waterscan-vrijstelling, `/ca falls`). **Eigen cap**: de eilandcap 3/chunk; binnen het bereik van een boom bestaan geen andere eilanden, dus die cap telt alleen de boomvallen. Lift: alleen als de boom de liftkolom vrijlaat.

## Hemelmeren (Q8 = A)
- Op elk eiland met top ≥ Y 140 (de hoogte-biomegrens) en diameter ≥ 36 (middel en reus), niet het boomeiland, en niet waar een ander eiland boven of onder de kom ligt (één meer per kolom).
- **Kom** (`SkyLakes`): genormaliseerde eilandafstand < aandeel (reus 0,28–0,40, middel 0,38–0,48), bodem `water − 1 − ⌊diepte · (1 − b²)⌋`, diepte 2–5. **Waterpeil** = één onder de laagste top van alle eilandkolommen naast de kom (exact per kolom), dus elk waterblok heeft water of rots naast en onder zich (waterscan 0).
- **Beek** (alleen nat): naar de bron van een gehouden val (61) waarvan de bron ≤ 4 onder het peil ligt; eerst recht naar binnen (de binnenmuur van de bron), dan stap voor stap naar het midden, ≤ 40 kolommen; het meer zakt naar de bronhoogte; wanden en bodem per kolom gecontroleerd, anders geen beek. Testwig: 26 meren (4 met beek), 58 droge kommen.
- **`withered_gardens`** (band op het eilandmidden bedorven, dezelfde regel als de biomehaak): dezelfde kom, droog.
- **Aquifer**: `RingAquifer` zet stil water in (floor, top); carvers mogen kom, beek en rand (3 blokken) niet snijden.

## Rotsbogen en eilandparen (Q8 = A)
- **Paar** (`IslandArches`): de randkolommen van twee eilanden langs de lijn tussen hun middens, 3–40 uit elkaar, tops ≤ 40 verschil; elk eiland met zijn 3 dichtste eilanden ná hem (x, dan z), dus elk paar één keer. Niet het boomeiland.
- **Boog**: ~1 op 2 paren van twee middel/reuzeneilanden 6–32 uit elkaar met randtoppen ≤ min(12, max(3, 0,4 × gat)) verschil; dek loopt van rand tot rand en stijgt 1–3,6 in het midden, halve breedte 1,6–2,2 (naar de uiteinden ×1,5), dikte 2,6–3,6 (×1,9 aan de uiteinden), afgeronde onderkant, licht gebogen, 3 blokken het eiland in. Vrij van andere eilanden (±2), vallen, bronnen, liften (±2 kolommen), hemelmeren en bomen. Testwig: 190 paren, 8 bogen.
- **Accessor voor W-biomes (75):** `IslandField.islandPairs(minX, minZ, maxX, maxZ)` → `List<IslandPair>`; `IslandPair(Island first, Island second, int firstX, int firstZ, int firstTop, int firstBottom, int secondX, int secondZ, int secondTop, int secondBottom, boolean arch)`: per eiland de randkolom die naar het andere wijst, met het hoogste en laagste blok daar. Paren waarvan een randkolom of de lijn ertussen in het vak valt. Ranken bijvoorbeeld van `firstBottom` naar `secondBottom`; bij `arch` ligt er rots tussen (geen ranken door de boog).
- Verder voor W-biomes: `skyLakes(vak)` → `SkyLake(x, z, radius, waterY, dry, brook)` (planten rond de meren), `worldTrees(vak)` → `WorldTree(x, z, baseY, top, radius)` (niets op/in de boom zetten), `Column.skyLakeTop()/skyLakeFloor()`.

## Tests (licht)
`build` (1.032 tests) en `runData` groen, `src/generated` ongewijzigd. Nieuw `Ring4LandmarksTest`: bomen (aantal/cellen, band, zones, hoogte, stam, eiland in zijn cel), geen eiland in een boom, boom = terrein (log/wood/leaves, `thin`), rafelige schil, vallen naast de wortels vrij; hemelmeren per blok ingesloten (offline waterscan over alle meren van twee wiggen), droog alleen bedorven, beken naar een gehouden val; paren en bogen (dek massief en beloopbaar ≤ 2 per stap); determinisme (tweede veld, omgekeerde volgorde). `RingAquiferTest.ring4SkyLakesHoldStillWater`. Aangepast: `Ring4IslandsTest` (TREE-maat, kom/stam op het midden), `IslandFieldTest` (geen bomen buiten ring 4), `Ring4BiomesTest` (de `world_tree`-regel staat eerst in de ring 4-tak).

## Client/server-impact
Alleen worldgen op de server; geen client-class, geen payload, geen `PROTOCOL_VERSION`.

## Wat INT moet nakijken
- Merge met W-terrein (73): beide raken `IslandField` (hier: veld `landmarks`, constructor, `candidates`, `details`, einde `ensureIslands`, `thin`, accessors, records, `Column`-velden, `Decisions.landmarks`) en `RingAquifer` (4 arrays, `load`, 2 regels vóór de carver-wacht van de gatmeren). Met W-biomes: `ring4_surface.py` (`world_tree()` eerst in `ring_4_branch`) en `Ring4BiomesTest`.
- In-game (teststop 5b): een boom bekijken (`/ca` tp naar een boom: op de testseed o.a. 6.436 2.271, 6.379 −1.114, 382 −6.723), kruin en wortels, vallen naast de wortels; een hemelmeer met beek, een droge kom, een boog.
- **Waterscan** op eilandmeren (hemelmeren + beken) en de boomgebieden: 0 nieuw. Determinisme omgekeerd.
- **Prestatie** bij een boom (meetpunt toevoegen aan `perf.txt`), de buitenste eilandband (hemelmeren, bogen) en de rest ongewijzigd.

## Voortgang
- 2026-10-16: branch aangemaakt op d1a37f11, 74 gebouwd; build + runData groen.

## CODEBASE-delta
- Nieuw `worldgen/densityfunction/IslandLandmarks` (74): coördinator van wereldbomen, hemelmeren en eilandparen achter de haken in `IslandField`; `Memo` (per wereld, in `Decisions.landmarks`: bomen, meren en paren per eiland, `Cell` per cel van 128 in `KeyedSlots`); `withTrees`, `treeDetails`, `fill`, accessors.
- Nieuw `WorldTrees` (plaatsing per boogcel van 2.500 op r 6.000: 15; plekregels; boomeiland `IslandShape.TREE`; vallen naast de wortels, `free`, `clashes`), `WorldTreeShape` (stam, takken, kruin, wortels; `Tube`, `Blob`, `Slice` per kolom), `WorldTreePart` (publiek: `NONE`, `LOG`, `WOOD`, `LEAVES`).
- Nieuw `SkyLakes` (kom, peil, beek, droge kom; `apply` per kolom) en `IslandArches` (paren, bogen; `Edge`, `Pair`, `Arch`).
- `IslandField`: haken (zie tabel), accessors `worldTrees` (gevuld), `islandPairs`, `skyLakes`, `hasTree`, `treePart`; records `WorldTree(x, z, baseY, top, radius)`, `IslandPair`, `SkyLake`; `Column.tree`, `skyLakeTop/Floor`, `skyGuardFrom/To`, `lowerIsland`, `guardSky`; `Decisions.landmarks`.
- `IslandShape.TREE`; `IslandPlacement.fall`/`outlet` package-private.
- `RingAquifer`: hemelmeerwater (stil) en carver-wacht rond hemelmeren.
- Nieuw `worldgen/surface/WorldTreeCondition` (`corruptedadventure:world_tree`, `part` log/wood/leaves/any), geregistreerd in `ModSurfaceConditions` (blok `// ring 4 — W-terrein-b`).
- `tools/worldgen/ring4_surface.py`: `world_tree()` eerst in de ring 4-tak; `noise_settings/overworld.json` opnieuw geschreven.
- Tests: `Ring4LandmarksTest` (nieuw), `RingAquiferTest`, `Ring4IslandsTest`, `IslandFieldTest`, `Ring4BiomesTest`.

## Beslissingen
Implementatiekeuzes binnen Q7-A/Q8-A (niet door de gebruiker besloten; bij teststop 5b bekijken):
- 2026-10-16 | worldgen | Wereldbomen per boogcel van 2.500 op de binnenstraal van ring 4 (15 cellen), r 6.230–6.900, band ≤ −0,2 over het hele bereik, ≥ 24 van elke zone | "~1 per 2.500, ~15, mythisch, nooit in een zone" letterlijk.
- 2026-10-16 | worldgen | "Half losgerukt eiland": een eigen eiland (52–68 breed) laag boven zijn gat (onderste lob 15–21 boven de grond), 7–10 wortels van onder het eiland tot voorbij de gatrand in de grond; plekken zonder water tot de gatrand (dus zonder vallen) worden overgeslagen | de bestaande eilandregels (gat, meer, vallen) blijven gelden.
- 2026-10-16 | worldgen | Kruin: koepel + 6–8 takken met bloesemtrossen, rafelige schil; takken en wortels `moonpetal_wood`, stam `moonpetal_log` | stam als log (verticaal), schuine delen zonder as-probleem.
- 2026-10-16 | worldgen | Eigen valcap van de boom = de eilandcap (3/chunk), omdat binnen het bereik van een boom geen andere eilanden liggen | geen extra haak in `fallsOfChunk`.
- 2026-10-16 | worldgen | Hemelmeren op elk eiland met top ≥ 140 en diameter ≥ 36, niet onder/boven een ander eiland; beek alleen naar een gehouden val, anders meer zonder beek | "grote hoge eilanden"; water alleen waar het per blok sluit.
- 2026-10-16 | worldgen | Rotsboog bij ~1 op 2 geschikte paren (middel/reus, 6–32 uit elkaar, beloopbaar hoogteverschil); paren = 3 dichtste buren per eiland, gat 3–40 | testwig: 8 bogen op 190 paren ("af en toe").

## Backlog
- [worldgen] Teststop 5b: de maat en het silhouet van de wereldboom in-game beoordelen (kroonstraal 0,32 × hoogte, kale stam onder de kruin ~40 % van de hoogte; wortels 1,2–3,6 dik); eventueel hangende bloesem of luchtwortels als aankleding (W-biomes).
- [worldgen] Hemelmeren: beken nu alleen naar een bestaande val (4 van 26 in de testwig); als er meer beken gewenst zijn, een eigen val vanuit het meer (water-tak, cap) — bij teststop beslissen.
- [worldgen] INT-prestatie: meetpunt bij een wereldboom in `tools/test/areas/perf.txt`.
