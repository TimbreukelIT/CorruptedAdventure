# Golf 1 — stroom W-terrein: 39 (ring 3-terrein overworld)

Scope: `worldgen` — werkpakket 39 (alleen terrein en vloeistofregel), branch `ring3/terrain-g1`.

**Doel:** het terrein van ring 3 dat het wow-moment draagt (teststop 4a, blokkerend). Het vervangt het placeholder-terrein (`ring_3/overworld.json`) door een eigen ring 3-invoer van `ring_blend`. De vormen:
- horizon-vulkanen boven de wolken;
- caldera's (actief met lavameer, slapend);
- lavarivieren in kanalen;
- zeldzame zwevende basalteilanden;
- golvende asvlaktes;
- de lege zeekom (`dried_seabed`);
- de vorm van het titanenveld (bijv. een vlakte met lage ruggen).

De gedeelde maskers uit `contract.md` §4 worden ingevuld. Surface tijdelijk met vanilla-blokken (basalt, blackstone, magma, smooth basalt); de echte blokken volgen in 39c.

Bron: `fase-1-wereld.md` Ring 3 + "Uitwerking sparring ring 3"; `contract.md` §4; `CODEBASE.md` valkuilen (interpolatie 4×8×4, `core_mask`, `ColumnTable`, geen `ThreadLocal` per instantie, `HeightGrid`, zwevende blokken); sjablonen `CanyonDetails` (rotsvormen per kolom), void-eilanden (`void/overworld.json`), `dry_riverbeds`-beddingen (change 31).

## Werk
1. **Hoogte (2D):** `ring_3/overworld/height` = asvlakte (Y ~80–100, golvend) + kegels + caldera's + zeekom-kom + kanalen. Regio's sturen via `region_heat`/`region_ground` (39a): pieken bij heet, zeekom bij koel.
2. **Kegels en caldera's:**
   - eigen Java-celtype (jittered grid, zoals de hoodoos), deterministisch per seed;
   - kegelprofiel met kraterrand en een calderabodem 20–50 onder de rand;
   - een fractie slapend (`caldera_dormant` = 1, bodem vlak en lager, geen lava);
   - hoogtes (V5 = A): de grootste kegels Y 230–260, gewone Y 140–200.
   - Kegels moeten vanuit `dry_riverbeds` boven de horizon uitsteken: de hoogste kegels liggen niet alleen diep in de ring.
3. **Lavarivieren:** `lava_channel` op eigen ruis (zoals `dry_riverbeds_channel`), bedding 1–3 diep onder de oever.
   - Lava via `RingFluidPicker` (ring 3-regel) of via de surface; kies en meet.
   - Geen stromende lava bij generatie behalve met een cap (spaarzame lavavallen zijn golf 2-features).
4. **Zwevende eilanden** (`island_mask`): alleen boven caldera's en kanalen; Y 150–210, ~1 cluster van 1–4 eilanden (8–30 breed) per ~3 caldera's (V4 = A); lavavalletjes eraf zijn golf 2-features; 3D-ruis alleen waar het masker > 0. Carvers mogen geen zwevende brokken maken (meten, zoals 23b).
5. **Vloeistof:** geen water boven ~Y 0 in ring 3 (de dried seabed blijft leeg). Lava in calderameren en kanalen. Aquifers (V8 = A): water alleen diep ondergronds (onder ~Y 0; floodedness in 42), geen water in de slapende krater; `caldera_dormant` stuurt ook de biome `dormant_caldera` (router-as `weirdness`). In `dry_riverbeds` blijft de regel van ring 2 tot gewicht 0,5.
6. **Zone 2→3:** de beddingsnede van change 31 herzien (backlog r. 122): de beddingen lopen uit vóór de grens. De kegels staan achter de zone (`core_mask`), zodat de smoothstep ze niet afkapt.
7. **Prestatie:** indicatief meten in de eigen worktree; officieel in INT. Budget ≤ 1,75×.

## Afhankelijk van open vragen
- Geen meer: V4, V5 en V8 = A (2026-10-06), verwerkt hierboven.

## Client/server-impact
Geen; puur worldgen, deterministisch.

## Tests
- **B**, inclusief JSON-tests voor `ring_3/overworld*` (zoals `Ring2TerrainJsonTest`).
- **D.**
- **S.**
- Ring 1/2 en zone 1→2 onveranderd (biomes + steen/deepslate).
- Naadcontrole zone 2→3: geen kliffen of zwevende brokken.
- Een terreinkaart (via 37d als die al gemerged is, anders het tijdelijke debugcommando van change 23, niet gecommit).
- **W-checklist** met coördinaten op seed `20260930` voor 4a: horizon-uitzicht vanuit `dry_riverbeds`, een actieve en een slapende caldera, een kanaal, een eilandcluster, de zeekom, het titanenveld.

## Bij afronding
`waves.md` §4: delta-secties onderaan, rapport aan main (met de gemeten hoogtes, frequenties en prestatie).

## Voortgang
- Voortgang: 20:45 branch ring3/terrain-g1 op 8e0c637, run/ (poort 25571/RCON 25581), basiswereld tg1-base (ring 1/2 + zone 2→3, 160 chunks) voor determinisme.
- Voortgang: 21:40 volcanic_field/volcanic_output + JSON-keten ring 3 + lava/droog-regel (RingAquifer/RingFluidPicker, preset fluid_levels.ring_3 = -1); kegels, caldera's, rivieren, eilanden, zeekom, titanenveld staan; modelkaarten + wereldkaarten in build/terrain-g1; tussenstand aan main.
- Voortgang: 22:05 feedback main verwerkt (zie hieronder), wereld tg1-e gegenereerd en gecontroleerd.

## Status bij onderbreking (2026-10-06 22:08)
**Gecommit:** niets (geen review gedaan; branch `ring3/terrain-g1` staat nog op 8e0c637). Alles hieronder staat ongecommit in de worktree.

**Ongecommit klaar (compileert):**
- Java: `worldgen/densityfunction/{VolcanicField,VolcanicOutput,VolcanicSettings,Volcano,VolcanicMath}.java` (DF-types `volcanic_field`, `volcanic_output`, geregistreerd in `ModDensityFunctionTypes` in blok `// ring 3 — W-terrein`); `RingFluidPicker` (`ring3Lava`, `ring3Dry`), `RingAquifer` (lava in kratermeren/rivieren, korst-lip waar lava aan open lucht zou grenzen, geen water vanaf Y 0), `RingChunkGenerator` (zoekt het veld in de router en geeft de aquifer de instantie van de noise chunk via `NoiseChunk.wrap`), AT-regel `NoiseChunk wrap`.
- JSON: `density_function/ring_3/overworld.json` (stapel van ring 2 met ring 3-terrein, top-slide 300→316, grotbewaking, eilanden), `ring_3/overworld/{field,height,initial_height,initial_terrain,terrain,zone_ramp,roughness,guard,islands,caldera_mask,caldera_dormant,lava_channel,island_mask}.json`, `ring_blend/overworld_initial.json` (ring 3 → `initial_terrain`), noises `ring_3_{volcano_cells,plains,rolling,flows,lava_channel,titan_ridges,warp,crags}`, preset overworld `fluid_levels.ring_3 = -1`. De JSON wordt geschreven door `build/tg1/genjson.py` (hulpscript, niet in git).
- Tests aangepast: `Ring2TerrainJsonTest.ringThreeHeightCarriesTheSameCut`, `NetherRing2JsonTest` (overworld heeft nu `fluid_levels.ring_3`). Ontwikkeltool `Ring3TerrainMapRender` (draait alleen met `CA_TERRAIN_MAP=1`; nog beslissen: committen of weglaten).

**Feedback main (3 punten), stand:**
1. Verticale wand in `b-great-sec-z69.png` x≈550: geen terreinwand maar een kleurgrens (biome-surface ring 2 ↔ steen op de harde biomewissel); het terrein loopt daar glad. De trappen rechts zijn de tafelbergtreden van ring 2 zelf. De zoneovergang zelf is verzacht (`zone_ramp` = w², max. helling ~0,75–1,3 i.p.v. ~1,9).
2. Rechte strepen/plateaus op de calderaflank: geulen meanderen nu (warp-ruis) en verschillen in diepte rond de kegel; de "plankjes" zijn grotmonden (vanilla `entrances`; het verschil model ↔ wereld is daar negatief). Nog niet opnieuw beoordeeld in een wereldbeeld (tg1-e bevat het, kaart nog niet gerenderd).
3. Vlakke asvlakte: lage heuvels (tot ~15) en lange asruggen toegevoegd (`HILL_HEIGHT`, `RIDGE_HEIGHT`), golving 3 → 4.
Ook: lavarivieren volgen nu alleen de brede deining (weinig treden, een kloof door heuvels); lava staat nooit tegen open lucht. Gemeten in tg1-e: 0 blootgestelde lava, 0 lava boven lucht, 0 water boven Y 0 in 4 ring 3-gebieden.

**Ontbreekt nog:**
- Kaarten van tg1-e renderen en zelf beoordelen (punt 2/3), eindbeeld voor main.
- Determinisme: tweede wereld met dezelfde gebieden in omgekeerde volgorde laden (`python -I build/tg1/gen.py build/tg1/areas-c.txt reverse`) en vergelijken met tg1-e (`build/tg1/compare.py`); ring 1/2 tegen `tg1-base`. Stand tg1-c: ring 2 alleen vanilla-ertsen/water; ring 1 (900,900) sculk/druipsteen = de bekende volgorde-ruis uit 39a. Nog bevestigen met tg1-e ↔ omgekeerd.
- Zwevende brokken in zone 2→3 vergelijken met een basiswereld (tg1-c had brokjes van 4–16 blokken aan de ring 2-kant van de zone; herkomst nog niet bepaald).
- Unit tests voor `VolcanicField` (zuiverheid, kratermeer onder de laagste rand, kegels alleen in de kern, eilanden Y 150–210 boven het terrein) en een JSON-test voor de ring 3-bedrading; volledige `build`, `runData`, S.
- Prestatie (JFR, noise-stap per ring 3-gebied, ≤ 1,75×).
- `/code-review medium terrain-g1 39`, daarna committen; delta-secties: CODEBASE-delta, Beslissingen (o.a. "horizonreuzen in elke regio behalve de zeekom", optie A van main) en Backlog (o.a. carvers ring 3 max. start-Y ~140 vanwege de eilanden; base-height-queries zien geen lavameren).

**Beelden:** `build/terrain-g1/overview.png`, `detail-*.png`, `section-*.png` (model, seed 20260930), `build/terrain-g1/world/{b,c,d}-*-top.png` / `-sec-*.png` (echte werelden). Werelden: `run/tg1-base` (8e0c637), `run/tg1-c`, `run/tg1-e` (laatste code). Hulpscripts in `build/tg1/` (rcon, gen, region, worldmap, checks, compare, genjson). RCON in `run/server.properties` staat uit; eigen servers zijn gestopt.

## Hervat (2026-10-07)
- Voortgang: 08:20 hervat; branch en status intact. Zwevende brokjes aan de ring 2-kant van zone 2→3 uitgezocht: dezelfde brokjes (4–18 blokken) staan in de basiswereld van 8e0c637 op dezelfde x/z (alleen een paar Y lager/hoger, want de zone ligt anders): ring 2's eigen vormen, niet van 39. Gemeten in z23a/b/c: basis 6/1/21 componenten, 39 7/3/22.
- Voortgang: 08:30 wereldbeelden tg1-e bekeken (punt 2/3). De titanenribben lagen strak noord-zuid (ruis gerekt langs z): nu op een gedraaide as (0,6 rad) en gebogen door de warp-ruis. Unit tests `VolcanicFieldTest`, `Ring3TerrainJsonTest` geschreven; eilanden die een buurvulkaan raken vallen nu weg (`islandClear`, ≥ 10 blokken vrij onder midden en rand).
- Voortgang: 09:00 B groen (434 tests, 0 fouten), D (`runData`) zonder diff. Determinisme: tg1-f en tg1-g (zelfde gebieden, omgekeerde laadvolgorde): caldera 625/625 en rivier 625/625 chunks identiek; vlakte 1017/1024 en reus 1322/1472 alleen door water/lava-stroming onder Y −18 en vanilla-steenklodders (andesiet/dioriet) en aan de ring 2-kant van de zone vallend `loose_sand`; ring 1 (900,900) sculk = zelfde verschil als basis ↔ 39 (volgorde-ruis, ook tussen twee runs van dezelfde code). Ring 2-gebieden tegen 8e0c637: alleen vanilla-ertsen/water/bubble columns.
- Voortgang: 09:20 de "rechte streep" op de calderaflank (punt 2) nagemeten: model en wereld kloppen daar tot op 1 blok, behalve een 10 breed, 10 diep, recht noord-zuid lopend gat: een vanilla-grot (spaghetti-ruis van de stapel) die door de kraterrand op Y ~130 snijdt. Geen masker- of chunkartefact. De "plankjes" zijn grotmonden van dezelfde ruis. Backlog voor 42 (grotten ring 3): grotmonden op kraterranden en kegelflanken dempen.

## Review (/code-review medium terrain-g1 39, 10 bevindingen) en afhandeling
1. **Korst keek alleen naar de kernhoogte** (crags op kraterwanden ±2–3, grotten): `open()` trekt nu `3 · roughness` af (de maximale crag-uitslag), dus bij twijfel korst. Grotten rond meren/rivieren houdt de `guard`-band al buiten; carvers zijn van W-biomes (backlog).
2. **Bereik vulkaan ↔ 3 × 3 cellen:** voetstraal nu begrensd op 0,8 × celgrootte (warp-marge), ook bij andere settings.
3. **`initialHeight` miste smalle laagtes:** nu 3 ringen (6, 11, 16) × 8 punten, onderling gedraaid.
4. **Natte ring 2 ↔ droge ring 3 op de middellijn:** nieuwe regel `RingFluidPicker.ring3Seal`: in de ring 3-helft van zone 2→3 blijft open ruimte onder Y 63 rots (alleen grotten; het oppervlak ligt daar ≥ 66). Geen waterwand meer op de middellijn.
5. **Celbeslissingen per chunk opnieuw:** `VolcanicField.Decisions` (gedeelde, begrensde `ConcurrentHashMap`s voor vulkanen, eilandcellen en eilandcontroles) gaat via `mapAll` mee naar elke kopie met dezelfde ruis; per wereld één keer.
6. **`VolcanicOutput` resolvede per aanroep:** nu een klasse die het veld één keer resolvet (direct bij een gemapte functie, anders bij het eerste gebruik; een holder is bij het decoderen nog niet gebonden, dus een verkeerde verwijzing kan pas dan falen).
7. `copy()` weg (dode code).
8. `ring3Lava` neemt nu het veld en x/z zelf; de ringcheck staat alleen nog in `RingFluidPicker`.
9. **Tests `RingAquifer` ring 3:** stilstaande lava in meer/bedding, korst naast open lucht, geen water vanaf Y 0, zegel in de zone (`RingAquiferTest`, 4 nieuwe tests; de bestaande zonetest volgt het zegel).
10. **`CODEBASE.md` niet bijgewerkt:** bewust; `waves.md` §4.8 laat de stromen alleen een CODEBASE-delta in het eigen plan schrijven (hieronder), INT voegt die samen.

Na de fixes: B groen (437 tests, 0 fouten).

## Prestatie (indicatief, eigen worktree, 2026-10-07)
JFR `minecraft.ChunkGeneration`, stap `noise`, 16 × 16 chunks per gebied, warm-up eerst, verse wereld per ronde, ons en vanilla (`minecraft:normal`, zelfde seed) afwisselend; mediaan per ronde in ms/chunk, daarvan de mediaan over 4 rondes (de laatste met de eindcode). Machine niet rustig: 9–11 andere Java-processen (servers/clients van andere stromen); vanilla spreidde per ronde tot ±25 %.

| Gebied (chunks) | ons | vanilla | ons / vanilla | slechtste ronde |
|---|---|---|---|---|
| ring 2, x 2.500 (148..163, −8..7), referentie | 50,0 | 33,6 | 1,49× (officieel 1,54×) | 1,62× |
| reus (−250..−235, −4..11) | 62,9 | 39,0 | 1,61× | 1,87× |
| caldera (−250..−235, −120..−105) | 56,3 | 34,3 | 1,64× | 1,70× |
| rivieren (−305..−290, −155..−140) | 56,9 | 36,6 | 1,56× | 1,60× |
| asvlakte (−302..−287, 30..45) | 59,0 | 36,4 | 1,62× | 1,71× |
| zeekom (−254..−239, 46..61) | 53,5 | 33,1 | 1,62× | 1,64× |
| titanenveld (−322..−307, −38..−23) | 53,9 | 34,8 | 1,55× | 1,70× |

De medianen liggen ≤ 1,64× (budget 1,75×). De marge is wel klein: in de laatste ronde lag vanilla in het reuzengebied laag (33,5 ms), wat 1,87× gaf. De officiële meting doet INT op een rustige machine. Ons eigen gebied is stabiel (±3 %); de ruis zit in vanilla.
De eerste meetronde (vóór de optimalisatie) gaf 75–83 ms (~2×). Wat hielp:
- de beslissingen per cel gedeeld tussen de noise chunks (`Decisions`, reviewpunt 5);
- het preliminary surface per kolom één keer per wereld onthouden (vanilla vraagt dezelfde aquifer-monsterkolommen vanuit veel chunks);
- in de kern van ring 3 de hoogteketen van ring 2 overslaan (`range_choice` op `zone_ramp` in `height`/`initial_height`);
- de 3 × 3 cellen rond een kolom per thread onthouden (`Neighbourhood`, review 2 punt 8).

Controle op de geoptimaliseerde wereld: 0 water boven Y 0, 0 lava naast open lucht, 0 lava boven lucht, en zwevend alleen de eilanden plus losse blokjes van 1–8 (grotresten, net als in ring 2) in de 6 gebieden.

- Voortgang: 10:20 reviewpunten opgelost, prestatie gemeten, eindbeeld `build/terrain-g1/eindbeeld.png`; tweede review op de diff.
- Voortgang: 10:50 review 2 afgehandeld (zie hieronder); B groen (438 tests); determinisme eindcode: tg1-h ↔ tg1-i (omgekeerd), caldera 625/625, rivieren 625/625, zeekom 256/256 identiek; vlakte 1017/1024, titanen 252/256 en reus 1345/1472 alleen door aquifer-water/lava-stroming onder Y −13 (randchunks), vanilla-steenklodders en vallend `loose_sand` aan de ring 2-kant; 0 water boven Y 0, 0 lava naast lucht, 0 lava boven lucht in 6 gebieden.

## Review 2 (/code-review medium, 10 bevindingen) en afhandeling
1. **Trede van 1 blok rond elke vulkaan** (`smoothMax(0, 0, 4)` = 1): de afronding krimpt nu tot 0 bij de voet (`k = min(4, raise, delta)`), alleen positieve delta's tellen.
2. **Kegel op een hoge vlakte** (straal ≤ kruin, deling door ≤ 0): kegelrand ≥ vlakte + 40, voet ≥ kruin + 30; calderarand altijd 22–44 boven de vlakte (de grens Y 135 vervalt; eilanden alleen bij rand ≤ 174 bleef).
3. **`zone_ramp` mengde ook in zone 3→4 terug naar ring 2:** nieuwe gewicht-DF `ring_3/overworld/zone_weight` (`ring_blend` 0/0/1/1/1/1), dus alleen de 2→3-kant mengt.
4. **Korst t.o.v. het geïnterpoleerde oppervlak:** marge 1,5 blok + 4 × roughness (crag-ruis tot ~1,5).
5. **Niveau ring 3 herschreef de lavalaag** (lava tot Y −1 in diepe grotten): in de overworld blijft de status onder Y −54 die van vanilla (`RingFluidPickerTest.ring3LevelMovesTheSeaButKeepsTheLavaLayer`).
6. **Zone 3→4 had dezelfde water/droog-grens:** `ring3Seal` geldt nu in ring 3's helft van beide zones.
7. **Zegel vult ook oppervlak/carvers onder Y 63:** in zone 2→3 ligt het oppervlak ≥ 66 (beddingen stoppen daar); in zone 3→4 vult hij een dip onder 63 zolang ring 4 placeholder is. Carvers en grotten verdwijnen in die 64 brede strook onder Y 63: geaccepteerd, vastgelegd (beslissing + backlog voor ring 4).
8. **Map-lookups per kolom:** `Neighbourhood` (de 3 × 3 cellen per thread onthouden, kopie per kolom omdat een eilandcontrole andere kolommen uitrekent).
9. **`initialHeights` werd vaak gewist:** sleutel op het 4-blokraster (16× minder ingangen), limiet 100.000; ringen 6/12/18 rond het midden van het kwartier.
10. **`CODEBASE.md`/beslissingenlog:** bewust niet (`waves.md` §4.8: alleen delta-secties in het eigen plan; INT voegt samen).

## Uitvoering (samenvatting)
- **Vormen** (Java-veld, 2D per kolom + twee dunne 3D-lagen): asvlakte Y ~80–100 (deining ±8, golving ±4, lage heuvels tot ~15, lange asruggen tot 5); getrapte stroomlobben in `basalt_flows`; vlakker in `sulfur_fields`; titanenribben (tot 8 hoog, gedraaide as) in `titan_fields`; zeekom Y 30–45 met drie oude kustterrassen, helling geheel binnen `dried_seabed`. Vulkanen op een jittered raster van 320: horizonreuzen (rand Y 230–260, straal 190–256), gewone kegels (Y 140–200), lage caldera's (rand 22–44 boven de vlakte, kraterbodem rond vlakteniveau), kegels ≥ 40 boven de vlakte, voet ≤ 0,8 × cel en ≥ kruin + 30; krater 20–50 onder de rand, wand met helling ~1,8 onder de rand, afgeronde kraterrand, meanderende flankgeulen, rand varieert rond de krater; 30 % kegels en 45 % caldera's slapend (vlakke lagere bodem, geen lava). Lavarivieren: netwerk op `ring_3_lava_channel`, niveau volgt alleen de brede deining, bed 3 onder het lavaniveau, door heuvels als kloof; weg op vulkaanflanken, zeekom, titanenveld en 160 blokken voor beide zones. Eilanden: clusters van 1–4 (8–30 breed, onderkant als omgekeerde kegel) op Y 150–210 boven actieve kraters met rand ≤ 174 en boven rivieren, alleen als ≥ 10 blokken vrij boven het terrein.
- **Regio's** (gedeelde drempels van W-biomes, in `field.json` `settings`): kegelkans 0,85 in `volcanic_peaks` (h ≥ 0,166, g < 0), 0,2 in `basalt_flows`, 0,08 in `sulfur_fields`, 0,07 elders; horizonband (60–480 achter zone 2→3) kans 0,8 met 50 % reuzen in elke regio behalve de zeekom.
- **Zone 2→3:** ring 3's hoogte = `h2 + w² · (veld − bedding − h2)` (`zone_ramp`), zelfde beddingsnede in beide invoeren; de beddingen lopen uit vóór ring 3 (masker van change 31). Gemeten helling in de zone max. ~0,75–1,3.
- **Vloeistof:** preset overworld `fluid_levels.ring_3 = -1` (geen zee boven Y 0); `RingAquifer` zet lava in kratermeren en rivieren (regel `RingFluidPicker.ring3Lava`, geen fluid-update), maakt een korst (steen) waar lava aan open lucht zou grenzen, en maakt aquiferwater vanaf Y 0 lucht (`ring3Dry`). Grotbewaking (`guard`) houdt ruis- en noodlegrotten uit de rots rond meren en rivieren.
- **Metingen model** (seed 20260930, hele ring): zie `build/terrain-g1/stats.txt`.

## CODEBASE-delta
**Belangrijke classes** (`…worldgen.densityfunction` tenzij anders):
| `VolcanicField` | `corruptedadventure:volcanic_field`: het ring 3-terrein. Codec: noises `hash`, `plains`, `rolling`, `flows`, `heat`, `ground`, `channel`, `ridge`, `warp` + `settings` (`VolcanicSettings`). `compute` = kernhoogte; `column(x, z)` = alle 2D-waarden (`Column`: `height`, `preHeight`, `roughness`, `calderaMask`, `dormant`, `channel`, `islandMask`, lava `lavaBottom..lavaTop` (exclusief), `guardBottom..guardTop`, eilandintervallen) via `ColumnTable`; `place(i, j)` = vulkaan van een cel (zuiver, leest ruis op exacte posities, nooit een `flat_cache`); `channelCluster` (eilanden boven rivieren, cel 960); `islandClear` (eiland ≥ 10 vrij boven het terrein); `initialHeight` (laagste kernhoogte op ringen van 6/11/16 × 8 punten − 2, voor het preliminary surface); `guard`/`islands` (3D). `Decisions`: gedeelde, begrensde maps (vulkanen per cel, eilandcellen, eilandcontroles, `initialHeight` per kolom) die `mapAll` doorgeeft aan kopieën met dezelfde ruis (één keer per wereld, niet per chunk). Voetstraal ≤ 0,8 × celgrootte (3 × 3 cellen volstaan). `equals` zonder caches, zodat de memo van de noise chunk één instantie deelt; `find(root)` zoekt het veld in een router, `resolve`/`resolveIfBound` door holders/markers | `ring_3/overworld/*.json`, `RingChunkGenerator` |
| `VolcanicOutput` | `corruptedadventure:volcanic_output` (`field`, `output`; resolvet het veld één keer): `initial_height`, `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`, `roughness`, `guard`, `islands` | `ring_3/overworld/*.json` |
| `VolcanicSettings` | record + codec (alle velden optioneel, standaard `DEFAULT`): vlakte, regiodrempels (`hot`, `cool`, `seabed_ground`, `titan_ground` = de biome-rechthoeken van ring 3), celgrootte, randhoogtes reuzen/kegels, eilandband/-kans, rivierbreedte | `VolcanicField` |
| `Volcano` | record van één vulkaan (soort `GREAT`/`CONE`/`CALDERA`, rand, voet, kruin, kraterbodem, exponenten, slapend, meer, randvariatie, geulen, warp, eilanden) met `surface(r, hoek, vlakte, meander)` (smooth-min van flank en kraterwand), `slope`, `craterMask`, `rimLow`; record `Island` (`distance`, `topAt`, `bottomAt`) | `VolcanicField` |
| `VolcanicMath` | `smoothstep`, `smoothMin`/`smoothMax`, `stair` (trap met zachte stootborden) | ring 3-terrein |
| `RingFluidPicker` (`…worldgen.chunk`) | + `ring3Lava(dim, positie, veld, x, y, z)` (lava in open ruimte onder de lavatop van de kolom, overworld ring 3, hard op `ring()`), `ring3Dry` (geen water vanaf Y 0, `RING_3_DRY_FROM`), `ring3Seal` (ring 3-helft van zone 2→3: open ruimte onder Y 63 blijft rots, geen waterwand op de middellijn) | `RingAquifer` |
| `RingAquifer` (`…worldgen.chunk`) | + optioneel `VolcanicField` van de noise chunk: zegel, dan lava vóór vanilla (geen update); `exposed`: lava naast een kolom zonder lava met een oppervlak (− 3 × roughness voor de crags) eronder wordt steen (korst); ring 3-water vanaf Y 0 → lucht | `RingChunkGenerator` |
| `RingChunkGenerator` (`…worldgen.chunk`) | zoekt het `VolcanicField` één keer per `RandomState` in de router en geeft de aquifer de instantie van de noise chunk (`NoiseChunk.wrap`, AT) | — |

**Registries:** `ModDensityFunctionTypes` blok `// ring 3 — W-terrein`: `volcanic_field`, `volcanic_output`. AT: `NoiseChunk wrap` public.

**Databestanden:**
| `density_function/ring_3/overworld.json` | ring 3-invoer van `ring_blend/overworld`: de stapel van ring 2 (vanilla `final_density`) met `ring_3/overworld/terrain`, bovenste slide 300→316 (vanilla 240→256 kapte de reuzen af), grotten buiten de bewaakte band (`max(X, min(guard, terrain))` vóór de interpolatie, `max(noodle, guard)`), en `max(…, interpolated(islands))` |
| `ring_3/overworld/field.json` | het veld (noises + settings; de regiodrempels = biome-rechthoeken ring 3, samen wijzigen met de preset) |
| `ring_3/overworld/{height,initial_height,initial_terrain,terrain,zone_ramp,zone_weight}.json` | `height` = `flat_cache(range_choice(zone_ramp ≥ 1 → veld, anders h2 + zone_ramp · (veld − riverbed_cut − h2)))` (contract-DF; in de kern wordt ring 2's keten niet berekend); `initial_height` idem met `initial_height` van veld en ring 2 (geen `flat_cache`; `initial_terrain` leest hem via `cache_2d`); `terrain` = `(height − y)/8` + `roughness · 0,3 · ring_3_crags` (3D); `zone_ramp` = spline w² op `ring_3/overworld/zone_weight` (`flat_cache` van `ring_blend` 0/0/1/1/1/1: alleen de 2→3-kant mengt, zone 3→4 niet terug naar ring 2) |
| `ring_3/overworld/{caldera_mask,caldera_dormant,lava_channel,island_mask,roughness}.json` | `flat_cache` van `volcanic_output` (contract-DF's; `caldera_dormant` voedt de router-weirdness) |
| `ring_3/overworld/{guard,islands}.json` | 3D-outputs (per blok) |
| `ring_blend/overworld_initial.json` | ring 3 → `0,390625 + ring_3/overworld/initial_terrain` |
| `noise/ring_3_{volcano_cells (hash),plains (−9),rolling (−6),flows (−7),lava_channel (−9),titan_ridges (−6),warp (−7),crags (−4)}.json` | ruis van het veld |
| preset `corrupted_world.json` | overworld-generator `fluid_levels.ring_3 = -1` |

**Tests:** `RingAquiferTest` (+ ring 3: stilstaande lava, korst, droog vanaf Y 0, zegel in de zone); `VolcanicFieldTest` (zuiverheid, vulkanen in de kern met hun hoogtes, voorkeur `volcanic_peaks`, gesloten lavameren, slapend zonder lava, eilanden vrij in hun band, ~1 cluster per 3 kraters, lava onder de bewaking, buiten ring 3 niets); `Ring3TerrainJsonTest` (bedrading, drempels, slide, bewaking, zone-ramp, zeeniveau); `Ring2TerrainJsonTest.ringThreeHeightCarriesTheSameCut`; `NetherRing2JsonTest` (overworld heeft alleen `fluid_levels.ring_3`).

**Valkuilen:**
- Een vloeistof via de globale `FluidPicker` lekt via de bemonstering van vanilla's aquifer (13 punten op chunkafstand) naar open ruimte ernaast (lavaplaten naast een krater). Lokale lava daarom in `RingAquifer` vóór vanilla, niet in de picker. Hoogtevragen (`getBaseHeight`, structures) zien de lavameren daardoor niet.
- Lava met een per kolom berekend niveau geeft treden; waar lava aan open lucht grenst, stroomt ze bij de eerste blokupdate. Daarom de korstregel (`exposed`) en een rivierniveau dat alleen de brede deining volgt.
- Een veld dat door meerdere DF's gelezen wordt: `equals`/`hashCode` zonder caches, dan geeft de memo van de noise chunk (en van de random state) één instantie met één kolomcache. De aquifer krijgt die instantie via `NoiseChunk.wrap` (AT).
- Een cel-beslissing die zelf kolommen uitrekent (eilandcontrole) mag niet binnen `CellCache.find` van dezelfde cel gebeuren (de cache markeert de cel al als gevuld): die controle zit daarom in de kolomvulling (`islandClear`), niet in `place`.
- Model ↔ wereld vergelijken: verschillen van ~10 blokken in rechte lijnen op hoog terrein zijn vanilla-grotten (spaghetti/entrances) van de stapel, geen maskerranden.
- **Prestatie ring 3:** vanilla vraagt het preliminary surface van dezelfde kolommen (de bemonsteringspunten van de aquifer liggen op een vast raster) vanuit veel chunks, en de noise chunk maakt per chunk een nieuwe kopie van elke DF. Dure, zuivere beslissingen per cel of kolom daarom in een gedeelde cache die `mapAll` doorgeeft (`VolcanicField.Decisions`), niet in een cache per instantie: dat haalde ring 3 van ~2× naar ≤ 1,62× vanilla.
- **Achtergrondtaken op Windows:** een met de taakstop beëindigde achtergrond-bash laat zijn kindprocessen (lussen, `gradlew runServer`) doorlopen. Ruim ze op per PID (commandline met het eigen worktree-pad) en verwijder daarna zelf het eigen `ca-test.lock`.

## Beslissingen
- 2026-10-06 · worldgen · Horizonreuzen staan in elke regio behalve de zeekom en krijgen daar de biome van die regio (optie A, main); anders is vanuit `dry_riverbeds` niet overal een vulkaan te zien.
- 2026-10-06 · worldgen · Lava in ring 3 via `RingAquifer` (regel in `RingFluidPicker`), niet via de surface of de globale picker (lek via de aquifer-bemonstering); een korst van steen waar lava aan open lucht zou grenzen; geen water vanaf Y 0 (preset `fluid_levels.ring_3 = -1` + aquiferzakken).
- 2026-10-06 · worldgen · Zone 2→3: ring 3's hoogte mengt naar ring 2's hoogte met w² (zachtste overgang over de squeeze-verzadiging; ~1,9 → ~1,3 max. helling); de beddingen lopen uit vóór ring 3.
- 2026-10-07 · worldgen · In ring 3's helft van de zones 2→3 en 3→4 blijft open ruimte onder Y 63 rots (`ring3Seal`): de buren houden hun zee en aquiferwater tot de middellijn, ring 3 is droog; zonder zegel stond daar water tegen lucht (review). Grotten en carvers verdwijnen in die strook onder Y 63.
- 2026-10-06 · worldgen · Eilanden ook boven lage kegelkraters (rand ≤ 174), niet alleen boven lage caldera's, om op ~1 cluster per 3 kraters te komen; een eiland vervalt als een buurvulkaan eronder komt.

## Backlog
- [worldgen] 42 (grotten ring 3): grotmonden van de vanilla-stapel snijden rechte geulen in kraterranden en kegelflanken (gezien op Y ~130); daar dempen, samen met floodedness/erosion ring 3.
- [worldgen] W-biomes (39b/47*): carvers in ring 3 niet boven Y ~140 laten starten (eilanden Y 150–210), anders zwevende brokken; caves onder lavameren/rivieren krijgen via de aquifer lava binnen de lavaband.
- [structures] Fase 3: hoogtevragen (`getBaseHeight`) zien geen lavameren/-rivieren van ring 3 (lava komt uit de aquifer, niet uit de globale picker); structure-plaatsing daarop controleren.
- [worldgen] Ring 4 (W-terrein ring 4): `ring3Seal` vult in ring 3's helft van zone 3→4 ook een oppervlaktedip onder Y 63 met rots zolang ring 4 placeholder is; met het echte ring 4-terrein (moeras, water) de zone 3→4 opnieuw bekijken (`steam_fens`).
- [worldgen] Lavatreden in rivieren worden korst (steen) op de rand; bij teststop 4a beoordelen of dat mooi genoeg is of dat de treden als kleine lavavallen (golf 2-feature met cap) moeten.
