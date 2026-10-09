# Integratie 2 — ring 3, golf 2

Scope: `infra` (integratie). Protocol: `waves.md` §6. Branch `ring3/int2`, worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/int2`, basis main `6ef5a85`.

Mergevolgorde (afwijking van §6, akkoord main): T → W-nether → C → A, daarna W-biomes zodra 41b gecommit en gereviewd is. Al op main: 50-0, 39f, B 46-rest, 42.

## Voortgang
Voortgang: 2026-10-09 start (worktree + branch, regels en waves gelezen)
Voortgang: 2026-10-09 15:22 T, W-nether, C, A gemerged (één conflict: docstring `tools/worldgen/ring3_surface.py`, 42-tekst gehouden, Nether-regel eruit); integratiefixes: `NetherRing3BiomesTest` → `look(key).colours()`, Nether-3-biome-JSON geregenereerd, `PROTOCOL_VERSION` 5 → 6, `cold_exposed` één TagKey (`ModTags.Biomes.COLD_EXPOSED`); beide surface-scripts idempotent; B (`--rerun-tasks`, 637 tests, 0 fouten, incl. `ClientIsolationTest`) en D groen
Voortgang: 2026-10-09 15:45 W-biomes gemerged (`0dbca54`, geen conflicten); beide surface-scripts idempotent; integratiefix: `steam_fens.json` geregenereerd met de definitieve sfeer van 50 (kleuren + `steam_puff`); B (`--rerun-tasks`, 665 tests, 0 fouten) en D groen ("missing BiomeFilter" alleen voor de bewuste per-chunk-features)
Voortgang: 2026-10-09 16:05 docs W-biomes samengevoegd (`b12ee08`); referentie-worktree `.claude/worktrees/int2-ref` (main `6ef5a85`) voor de feature-index; landmarkcoördinaten uit de pure vorm (`build/int2/landmarks.txt`: 21 riftbogen, 312 buissystemen, geodes, eilandvallen); determinismewereld A en referentiewereld genereren
Voortgang: 2026-10-09 16:45 determinisme (int2-det-a/-b, b omgekeerd: Nether eerst): Nether-3 1.280/1.280 identiek; ring 3 8.080/8.614 chunks identiek, verschillen alleen vloeistof, vanilla-ertsklodders (ook mod-ertsklodders over chunkgrenzen, zoals 41), obsidiaan (lava+water), 4 `ash_layer` op een vanilla-ijzerklodder en 5 `ash_crust` (valblok boven een buis, door een buurupdate gevallen); feature-index ring 1/2 + Nether-1/2 tegen main `6ef5a85`: biomes 100 % identiek, terrein gelijk (verschillen alleen op decoratieplekken: ring 2-zand/kloofsteen-features, ring 1-grinddisks); lava-tegen-lucht-scan: wanden bestaan al op main `6ef5a85` (zelfde tellingen), W-biomes voegt een paar lavavallen toe (as 30 → 38)
Voortgang: 2026-10-09 17:34 S (dedicated server 25579 + Dev/Dev2 quick-play, protocol 6, wereld `ring3-wave2`): `tpring 3`, `tpring 3 nether`, `tpboundary 2|3` (3 = `steam_fens`), portal ring 3 (4.758, 99, 17) ↔ Nether-3 (1.186, 58, 18) heen en terug; `nether-lava-walls.py` r 875 en 1.500: 0 wanden; geiser in `steam_fens` (5.911, 102, −101) lanceert ~5 blokken op zijn ritme (worldgen-tick via `startCycle`); 8 striders, 0 met ruiter; hitte in de lavabuis −0,50/s (roof), naast het gootje +0,41/s (source); kou Nether-3 +0,63/s (exposed) en bij 100 `TicksFrozen` 140; pluimen op beide clients gelijk, terug na de Nether; 94 screenshots in `build/teststop-4b/`; serverlog zonder fouten/far chunk (alleen "moved too quickly" door teleports)
Voortgang: 2026-10-09 17:50 `teststop-4b.md` opgezet; officiële prestatiemeting loopt (lock + 3 slots, 25 gebieden, 5 warme + 1 koude ronde)
Voortgang: 2026-10-09 18:55 prestatie: 4 geldige warme rondes (noise-mediaan per gebied 0,95–1,36, alles ≤ 1,75); ronde 5 en de koude ronde 6 afgebroken door een externe RCON-`stop` (18:39:46/18:40:38), lock en slots vrij, gevraagd aan main wanneer opnieuw. Offline: Nether-3 lava alleen open spiegels op Y 22 (0 wanden, 0 boven Y 22); ijsbruggen in de kloven (grootste 25 lang, ~7,7 breed, Y 81 bij (1.106, −468)); geen ijs/sneeuw in ring 3 (overworld); in de kloven 29 blootliggende ijsblokjes binnen 3 blokken van 14 van de 107 open lavakolommen; eilandvallen archipel 4/4 aanwezig; geen eilandval op een daklicht/buismond (dichtstbij 19 blokken)

## Merges en integratiefixes
| Stroom | Branch-tip | Merge | Conflicten | Integratiefix |
|---|---|---|---|---|
| T (57a-r1) | `90d7703` | `ea5cdca` | geen | — |
| W-nether (53b, 54) | `a19fe6d` | `1c05dc3` | docstring `tools/worldgen/ring3_surface.py` (42-tekst gehouden, Nether-regel weg; het Nether-blok staat sinds 53b in `nether_ring3_surface.py`) | — |
| C (55, 43) | `dd3a191` | `2377a0a` | geen | — |
| A (50, 56, 50b) | `efd3989` | `f001529` | geen | `NetherRing3BiomesTest` → `NetherRing3Atmosphere.look(key).colours()`; de drie Nether-3-biome-JSON's geregenereerd met de effecten van 56 (`c93d5ab`) |
| W-biomes (41, 47, 47b, 49, 47c, 48, 41b) | `0dbca54` | `de42e29` | geen | `steam_fens.json` geregenereerd met de definitieve sfeer van 50 (`e79550d`) |

Verder (`c93d5ab`): `ModNetwork.PROTOCOL_VERSION` 5 → 6 (`ConditionSyncPayload` 1 byte langer door `COLD`, nieuwe `VolcanoPlumesPayload`); `cold_exposed` is één TagKey (`NetherRing3BiomeTags` vult `ModTags.Biomes.COLD_EXPOSED`). Beide surface-scripts reproduceren de JSON na elke merge. Docs: `235a53a` (39f, 46-rest, 42, T, W-nether, C, A) en `b12ee08` (W-biomes).

## Testresultaten
- **B:** `./gradlew build --rerun-tasks` groen na de laatste merge (665 tests, 0 fouten, incl. `ClientIsolationTest`).
- **D:** `runData` groen; alleen de bewuste "missing BiomeFilter"-meldingen (`ash_drifts`, `titan_fields_rocks`, `ash_footprint_trails`, `lava_falls`, `ember_woods_ash`, `fire_geodes`).
- **S:** dedicated server (25579) + Dev en Dev2 (quick-play, protocol 6), verse wereld `ring3-wave2`, seed 20260930.
  - W-smoke: `/ca tpring 3` (−4.740, 96, −316), `tpboundary 2` (2.846, 123, −2.088, zone 2→3), `tpboundary 3` (−5.996, 69, −400 = `steam_fens`), `tpring 3 nether` (−1.185, 36, −79). Portal ring 3 (4.758, 99, 17) → Nether-3 (1.186, 58, 18, rijpwoud) en terug.
  - Serverlog: 0 ERROR/Exception, geen "far chunk", geen "Feature order cycle", geen client-class; alleen "moved too quickly" door de teleports.
- **Determinisme** (`int2-det-a` vooruit, `int2-det-b` omgekeerd met de Nether eerst; 26 overworld-gebieden + 8 Nether-gebieden):
  - Nether-3 (zee, rijpwoud, zuid, noord, kloven) 1.280/1.280 chunks identiek; strook N1|N2|N3 346/400 (alleen vanilla-featureruis in N1/N2).
  - Ring 3: 8.080/8.614 identiek. Verschillen: vloeistof (water/lava/lucht), vanilla-ertsklodders en mod-ertsklodders over chunkgrenzen (bekend sinds 41), obsidiaan (lava raakt water), 4 `ash_layer` op een vanilla-ijzerklodder (pit, lavarivieren) en 5 `ash_crust` (valblok boven een lavabuis bij (4.858, 73, 75), door een buurupdate wel of niet gevallen). `steam_fens`, zeekom, ribbenkast en geode 100 % identiek.
- **Feature-index** (ring 1/2, zones, Nether-1/2 tegen main `6ef5a85` in `.claude/worktrees/int2-ref`): biomes 100 % identiek; terreinvorm gelijk (`terrain_shape.py`, decoratie genegeerd); de resterende verschillen liggen alleen op decoratieplekken (ring 2: zand-/kloofsteen-features, ring 1: grinddisks). Decoratie (bomen, ertsen, dripstone) verschuift, zoals verwacht.
- **Prestatie** (JFR `minecraft.ChunkGeneration`, mod tegen vanilla-datapackdimensies in één server, 16 × 16 chunks per gebied, warm-up over alle ringtypes, verse wereld per ronde, volgorde per gebied wisselend). 4 geldige warme rondes; ronde 5 en de koude ronde zijn onderbroken (main, 18:39: de gebruiker ging testen) en volgen na de go van main. Mod/vanilla, mediaan over de rondes:

| Gebied | noise mediaan (4 warm) | min–max | surface | features | noise INT1 (mediaan) |
|---|---|---|---|---|---|
| ring 1 | 0,95 | 0,89–1,05 | 1,48 | 1,00 | 1,23 |
| ring 2 | 1,05 | 0,95–1,11 | 2,85 | 0,83 | 1,45 |
| horizonreus | 1,19 | 1,18–1,76 | 2,59 | 0,43 | 1,55 |
| caldera | 1,10 | 1,07–1,46 | 2,24 | 0,48 | 1,14 |
| lavarivieren | 1,02 | 1,00–1,43 | 2,18 | 1,28 | 1,36 |
| asvlakte | 1,00 | 0,97–1,05 | 2,04 | 1,02 | 1,20 |
| zeekom | 1,36 | 1,16–1,41 | 1,77 | 0,87 | 1,35 |
| titanenveld | 1,19 | 1,12–1,47 | 1,93 | 0,93 | 1,20 |
| pieken | 1,06 | 1,03–1,50 | 2,28 | 1,01 | 1,21 |
| basaltstromen | 1,10 | 1,05–1,49 | 2,11 | 0,32 | 1,41 |
| smeulwoud | 1,22 | 1,13–1,60 | 1,94 | 0,87 | 1,44 |
| zwavelvelden | 1,19 | 1,16–1,55 | 2,45 | 0,37 | 1,24 |
| slapende krater | 1,05 | 1,03–1,42 | 1,92 | 0,87 | 1,55 |
| steam_fens | 1,06 | 1,05–1,52 | 1,68 | 0,34 | – |
| lavabuis (12, −2) | 1,08 | 1,00–1,55 | 1,96 | 0,26 | – |
| magmakamer (−12, −1) | 1,12 | 1,06–1,38 | 2,09 | 0,92 | – |
| Nether-1 | 1,12 | 1,05–1,25 | 1,06 | 0,53 | 1,33 |
| Nether-2 | 1,33 | 1,22–1,52 | 0,76 | 0,09 | 1,61 |
| Nether-3 rijpwoud | 1,36 | 1,26–1,63 | 1,41 | 0,52 | 1,47 |
| Nether-3 zee | 1,17 | 1,09–1,44 | 1,06 | 0,21 | 1,21 |
| Nether-3 zuid | 1,32 | 1,18–1,50 | 1,16 | 0,45 | 1,16 |
| Nether-3 noord | 1,15 | 1,09–1,42 | 1,01 | 0,26 | 1,28 |
| Nether-3 kloven (bruggen) | 1,35 | 1,12–1,67 | 0,95 | 0,27 | – |
- Noise: alle gebieden ≤ 1,36 (budget 1,75); hoogste losse ronde horizonreus 1,76 (ronde 1), daarna 1,18–1,19. Lager dan integratie 1 (1,14–1,55) en 39f (1,18–1,58): de rondes 2–4 waren snel (JIT-spreiding ±30 %, valkuil "Prestatie meten met JFR").
- Surface ring 3 1,7–2,6× (39f: 1,6–2,5×); ring 2 2,85× (niet geraakt). Features ring 3 0,26–1,28× vanilla.
- **Koud** (titanenveld, smeulwoud): nog te doen na de go van main.

## W-checklist (INT2-punten uit de plannen)
OK = gecontroleerd en in orde; **afwijking** = gemeld/backlog; *4b* = alleen de gebruiker kan het beoordelen (geluid, gevoel). Shots in `build/teststop-4b/` (worktree, niet in git).

| Punt | Uitkomst | Bewijs |
|---|---|---|
| Lavawanden Nether r 875 en r 1.500 | OK: 0 en 0 (lava 11.948 bij r 875) | `build/int2/walls-875.txt`, `walls-1500.txt` |
| Nether-3: geen lava boven Y 22, niet naast lucht | OK: alleen open spiegels op Y 22 (kloven 107, zuid 31, noord 12 kolommen), 0 wanden | `n3_lava.py` op `int2-det-a` |
| Ring 3: geen lava tegen lucht (behalve vallen) | **afwijking**: lavablokken met een luchtbuur in vrijwel elk gebied (B 146, D 132, basalt 143, geode 73, caldera 66, C 69, pit 117, E 84); dezelfde tellingen al op main `6ef5a85` → terrein, niet golf 2. Backlog `worldgen` (optie A, main) | `build/int2/lava-ow.txt`, `probe.py` |
| Eilandvallen (39f) aanwezig | OK: archipel 4/4 vallen doorlopend tot meer/grond | regiobestanden `int2-det-a`; `archipelago_*` |
| Lavavallen 1 blok breed | OK (bewust), *4b* | `archipelago_side_dev.png` |
| Eilandval op daklicht/buismond (backlog 42) | OK op de testseed: dichtstbij 19 blokken | `build/int2/landmarks.txt` |
| Pilaren eindigen in het eiland, kwabben niet in de wand | OK op de shots | `archipelago_side_dev.png`, `archipelago_below*_dev.png` |
| 21 riftbogen | OK: 21 in de pure vorm (o.a. (−4.071, −4.014)) | `landmarks.txt`, `rift_arch_dev.png` |
| Instortingen/zinkgaten | OK | `collapse_dev.png` |
| Lavabuizen glad, droog, wanden smooth basalt/blackstone | OK; vloer bijv. (4.861, 80, −783) | `tube_mouth_dev.png`, `tube_skylight_dev.png`; regioscan |
| Lavagootje met korst, kamer met lavameer, geen pilaren/rechte wanden | OK | `magma_chamber_dev.png`, `probe.py` (4.863, 63, −818) |
| Geen basaltrechthoek rond buismonden | OK op de shots | `tube_mouth_dev.png` |
| Hitte in de buis zakt, naast het gootje stijgt hij | OK: −0,50/s `roof`; +0,41/s `source` | RCON `/ca condition get` |
| Geen water boven Y 0 | OK: water in ring 3 alleen onder Y −16 | regioscan 4 gebieden |
| Geodes (41b): drie lagen, gloeiende knoppen | OK: (4.018, 9, 407) en (−4.961, −3, 559) | `fire_geode*_dev.png`, `c_fire_geode_cluster_dev.png` |
| Geen ijs in de warme ring (ring 3 overworld) | OK: 0 ijs/sneeuw in 24 ring 3-gebieden | `inv-ow.json` |
| Warme kring Nether-3 zonder ijs | kleine afwijking: in de kloven 29 blootliggende ijsblokjes binnen 3 blokken van 14 van de 107 open lavakolommen (bijv. 1.227, 28, −283); zee/zuid/noord 0 | regioscan |
| Ijsbruggen bestaan en zijn begaanbaar | OK: 5 dekken ≥ 15 kolommen in het kloofgebied, grootste 25 lang × ~7,7 breed op Y 81 (1.106, −468), treden ≤ 1 | regioscan |
| Rijpzwammen (kap niet afgeplat), ijspegels | OK op de shots | `n3_sea*_dev.png`, `c_rimeshroom_*`, `c_ice_stalactite_dev.png` |
| Gloeiende naden zeldzaam | OK: op de kloofbodem, alleen in vlekken | `c_glowing_fissure_dev.png` |
| Dichte mist Nether-3 (~40 blokken) | bevestigd, *4b* | `n3_*_dev.png` |
| Geisers met waarschuwing, lanceren | OK: (5.911, 102, −101) gooit een armor stand ~4,9 blokken op zijn ritme; worldgen-tick via `startCycle` | RCON |
| Striders zonder ruiter | OK: 8 striders, 0 met passagier | RCON |
| Geiten op de pieken | OK: 1 geit in de geladen pieken | RCON |
| Kou Nether-3, `cold_exposed` | OK: +0,63/s `exposed` in `frozen_lava_sea`; bij 100 `TicksFrozen` 140 | RCON |
| Pluimen: beide clients dezelfde | OK | `b2_east_plumes_dev*.png`, `p_b2_east_before_dev*.png` |
| Pluimen na dimensiewissel | OK: weg in de Nether, terug op dezelfde plek | `p_nether_away_dev.png`, `p_b2_east_back_dev.png` |
| Pluimen bij uitbarsting, Particles: Minimal, FPS | *4b* (op de shots weinig verschil in luchtkleur bij `eruption`) | `w_peaks_eruption*_dev.png` |
| Verre pluimen als mistsilhouet | bevestigd, *4b* | `b2_east_plumes_dev.png` |
| Geluiden (fumarolenveld niet dreunend, geiserwaarschuwing, weer) | *4b* (agents kunnen niet luisteren) | — |
| Zielvuur rond de ribbenkasten | **opvallend weinig**: 2 zielvuurtjes in het 16 × 16-gebied rond (−1.493, 4.144); afstemvraag 4b | `inv-ow.json`, `c_soul_fire*_dev.png` |
| Fumarolen op de heuveltoppen | 2 in het zwavelmeetgebied, 28 in het geodegebied; *4b* | `inv-ow.json`, `c_fumarole_dev.png` |
| Smeulwoud, slapende krater, zeekom, asvlakte, titanenveld, `steam_fens` | OK op de shots; levendigheid *4b* | `ember_*`, `dormant_caldera*`, `dried_seabed`, `ashen_*`, `titan_*`, `steam_fens` |
| T r2-shots per blok | gemaakt: 33 close-ups `c_*` + overzichten | `build/teststop-4b/` |
| Ruimte Nether-3 | ongewijzigd t.o.v. 53 (zelfde drempels ±0,18: zee 41,5 / rijp 35,8 / kloven 22,7 %); gevoel *4b* | — |

## Beslissingen
- 2026-10-09 · infra · Integratie 2 mergede T → W-nether → C → A en W-biomes als laatste (afwijking van waves §6, akkoord main: W-biomes bouwde nog).
- 2026-10-09 · infra · De prestatiemeting van integratie 2 telt voorlopig met 4 warme rondes; warme ronde 5 en de koude ronde (titanenveld/smeulwoud) volgen na de test van de gebruiker (main).

## Backlog
- [worldgen] Lava tegen lucht in ring 3 buiten de bewuste vallen (al op main `6ef5a85`): randen van lavaplassen op hun eigen niveau naast een open plek, een luchtgeul door een lavameer (4.134, 94, −3.458), diepe aquifer-lava in grotten, een diagonale wand van 5 hoog langs buiswanden (4.874, 90–94, −538). Fix-pakket in golf 3 (aquiferkorst langs lavaranden; optie A, main 2026-10-09). (bron: INT2)
- [worldgen] Determinisme ring 3: een aslaag (`ash_drifts`) hangt af van een vanilla-ertsklodder eronder, en een `ash_crust`-valblok boven een buis valt wel of niet door een buurupdate; 9 blokken op 8.614 chunks. (bron: INT2)
- [worldgen] Nether-3-kloven: enkele blootliggende ijsblokjes binnen 3 blokken van de open lavaspiegels (29 blokken bij 14 van 107 kolommen). (bron: INT2)

## Status
Stand 2026-10-09 19:10: alles behalve warme ronde 5, de koude ronde en de review is gedaan. Geen JVM tot de go van main (de gebruiker test in deze worktree).
Voortgang: 2026-10-09 19:30 review INT2 (main): docsfixes 1, 2a, 3–7 gecommit (`476110c`); 2b `Ring3GeneratedEffectsTest` geschreven (gegenereerde biome-effecten = hook, ring 3 + `steam_fens` + Nether-3), nog niet gedraaid: geen JVM tot de go van main
Voortgang: 2026-10-09 19:08 go van main; build `--rerun-tasks` groen (667 tests, incl. `Ring3GeneratedEffectsTest` 2/2 en `ClientIsolationTest`), `runData` + `git diff --exit-code src/generated` leeg
