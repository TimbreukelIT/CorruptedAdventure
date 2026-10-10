# Ring 3 golf 3 — stroom W (`worldgen`): 42c, 54c, 39g, 47e, 42d

Scope: `worldgen`. Regels: `build/agent-rules-golf3.md` > `agent-rules-afronding.md` > `agent-rules-golf2.md`. Plan van de golf: `golf-3.md`. Besluiten: `fase-1-wereld.md` ("Sparring golf 3"), beslissingenlog 2026-10-10. Poort n = 1 (niet gebruikt: geen server in deze stroom). Testen licht: B + D + offline/unit-tests per pakket.

## Pakketten
1. **42c — lavalek afgrond:** de luchtschacht naast de lavaband in de afgrond (pit) dichten, algemeen.
2. **54c — ijsblokjes Nether-3:** de warme kring rond de lavaspiegels echt ijsvrij.
3. **39g — lavaoevers:** smaller (plafond in blokken) en magma alleen direct langs de lava. Raakt surface → **perf hermeten in INT (noise/surface)**.
4. **47e — archipelvallen:** geen aslaag in het stroomgebied van een val; vallen eindigen niet in water.
5. **42d — askorst in worldgen** (na 38c op main): op dunne daken boven buizen/daklichten en op een stuk dun dak vóór elke buismond.

## Voortgang
Voortgang: Sat Oct 10 13:24 WEDT 2026 start; worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a0bd600d8a8a60905`, branch `ring3/worldgen-g3` op main `58a7325`.

Voortgang: Sat Oct 10 13:35 WEDT 2026 42c: diagnose (offline, `afr-det-a` + testveld): de schacht ligt waar de afgrond naar zijn eind toe smaller wordt; daar was de band rond de lavazee (`chasm` < 1,3 = 0,3 halve breedte) een blok of minder, dus cellhoeken naast de lava lagen buiten de band en een grot werd ernaast geïnterpoleerd (en carvers mochten er in). Fix: band ook `ABYSS_GUARD` (8) blokken voorbij de zee, eindes inbegrepen (`Landmarks.Abyss.near`). B `--rerun-tasks` groen (701 tests), D zonder diff.

Voortgang: Sat Oct 10 13:42 WEDT 2026 54c: diagnose (offline scan `afr-det-a`, n3rifts, + ruiswaarden op het testzaad): de blootliggende ijsblokken onder Y 30 binnen 3 blokken van een open lavakolom (16 met deze telling, horizontaal; de "6" van INT-afr in 3D) liggen (a) in scheuren aan de rand van `rime_forest` (regio 0,18–0,20: de scheuren lopen door tot het gewicht van de ijszee 0 is, |regio| < 0,22, maar de warme regels keken naar `SEA` = |regio| ≤ 0,18) en (b) bij scheurpatches 0,315 (masker open vanaf 0,3, warme wanden pas vanaf 0,32). Fix in `nether_ring3_surface.py`: `CRACK_ZONE` (|regio| < 0,22) en `CRACKS_FROM` (0,3) voor de warme scheurwanden, de gloeiende scheur bovenop en de warme kring. Alle 16 blokken vallen nu onder de warme scheurwanden (offline nagerekend). B `--rerun-tasks` groen, D zonder diff, script idempotent.

Voortgang: Sat Oct 10 13:48 WEDT 2026 39g: de oever was het hele kanaal (`lava_bank` = 1 voor `along` < 1) plus 3 blokken: de droge helling van een breed kanaal (tot ~0,45 halve breedte, eindpoel 14 halve breedte) werd zwart met magma, en `G` ≥ 0,75 gaf overal in de oever magma. Nu een eigen uitgang `lava_shore` voor de surface: 1 boven de lava, naar 0 over `SHORE_BLOCKS` (3) vanaf de lavarand (`VolcanicField.lavaEdge`: waar het kanaalprofiel onder het lavaniveau zakt, gesloten inverse van smoothstep), los van de breedte; magma alleen in het eerste blok (`lava_shore` ≥ 2/3, met de bestaande vlekruis), de losse magma-regel weg. `lava_bank` blijft voor de features (blijven uit kanaal + 3 blokken; plaatsing ongewijzigd). Offline (5 gebieden): 9.138 oeverkolommen allemaal binnen 4 blokken van zichtbare lava, magma binnen 2. B `--rerun-tasks` groen, D zonder diff, `ring3_surface.py` idempotent. **Perf hermeten in INT (surface; 42c noise).**

Voortgang: Sat Oct 10 14:00 WEDT 2026 47e: diagnose (offline diff `afr-det-a` ↔ `afr-det-b`, gebied arch): de aslaag (2.628, 116, −2.654) is een `ash_drift`-laag in `ember_woods` op de helling onder een archipelval, door de vloeiende lava wel/niet weggenomen; het obsidiaan van INT-afr ligt op Y −55 (2.574–2.593, −2.825…−2.815): vanilla-aquiferwater op vanilla's lavalaag, niet van de vallen (vraag aan main, zie Beslissingen). Fix: `LavaFallFlow` volgt per val waar de lava kan stromen (vanilla-regel ruim genomen: naar alle kanten, 2 minder per blok, opnieuw 8 na een val; grond 1 lager dan het geïnterpoleerde oppervlak; eigen kolomcache), één keer per wereld (`VolcanicField.fallFlow`); `AshDriftFeature` legt niets in een stroomgebied; een val die water kan raken (stroming onder Y 0 of buiten ring 3 zelf) wordt niet gemaakt (bron en val; besluit in de aquifer, `fallStaysDry`/`springStaysDry`). Testzaad: 38 vallen, 37 gemaakt, 0 nat, 0 afgekapt, grootste stroomgebied 412 kolommen; de aslaag van INT-afr ligt in een stroomgebied; puur (twee threads, omgekeerde volgorde, verse velden). B `--rerun-tasks` groen (707 tests), D zonder diff.

Voortgang: Sat Oct 10 14:55 WEDT 2026 main gemerged (`tools/test/`); besluit B (obsidiaan Y −55): ring 3 houdt één blok rots tussen aquiferwater en de lavalaag. B (`slots.sh run`, 708 tests) en D groen, geen diff.

Voortgang: Sat Oct 10 15:08 WEDT 2026 reviewfixes 42c–47e (main, `/code-review medium`, 9 punten):
1. een afgekapte stroming telt als nat: de val wordt niet gemaakt en features zien hem niet (`Flow.made()`); test `aCutOffOrWetFlowIsNotMade`;
2. Nether-3: de warme scheurregels staan nu vóór alle ijsregels (pillaren, bevroren vallen, rift-gletsjer en -ijsvallen), en boven Y 30 zijn de scheurwanden tot de top van de scheur (Y 43) kale netherrack met magma; offline nageteld op `afr-det-a` (n3rifts, 580 blootliggende ijsblokken binnen 3 blokken van open lava): onder Y 30 16 → 0 over; boven Y 30 op een scheurwand 294 → 0 over; de overige 195 boven Y 30 liggen niet aan een scheur (gletsjer en pillaren, vanaf Y 30 zoals ontworpen);
3. javadoc `fallFlowBelow()` hersteld;
4. `ring3Proper` via `RingLayout.sampleOverworld` (één ringfunctie);
5. `RingFluidPicker.RING_3_DRY_FROM` (publiek) i.p.v. een eigen constante;
6. trace goedkoper begrensd: `REACH` 96 → 64, `MAX_NODES` 40.000 → 8.000 (testzaad: verste stroming 20 blokken, hoogstens 982 toestanden); de `fallFlows`-map wordt nooit geleegd (ring 3 is eindig, ~40 vallen), dus geen herberekening in het hete pad; indicatief (verse velden, testzaad): traagste trace 59–61 ms, alle 38 samen ~0,4 s, één keer per val per wereld;
7. oppervlak per kolom gecachet in de trace;
8. eerst de goedkope grens `along < 1 + SHORE_BLOCKS / half`, dan pas `lavaEdge`;
9. kolomsleutels overal via `LavaFallFlow.key`.
Testzaad opnieuw: 38 vallen, 37 met stroming, 0 nat, 0 afgekapt. B (709 tests) en D groen, geen diff, script idempotent.

Voortgang: Sat Oct 10 15:17 WEDT 2026 42d: main gemerged (38c, `678a7e4`). Dunne daken van één blok boven een opening in de korstbiomes (`ashen_wastes`, `volcanic_peaks`, `ember_woods`, `titan_fields`) zijn weer `ash_crust` (i.p.v. `ash_block`); vóór elke buismond een stuk dun dak: waar de mondhelling van een droge buis door de grond breekt en minstens `LID_DROP` (3) diep ligt, wordt het bovenste grondblok een dun vast blok van een eigen soort (`KIND_CRUST`, 6) en eindigt de opening eronder; de surface verft het `ash_crust`, in elk biome. Het ondiepe begin van de geul blijft open, dus de mond blijft zichtbaar. Testzaad: 484 monden, 466 met een korstdak (de rest: helling te kort/ondiep), 10.029 korstkolommen. Determinisme: de korst hangt na het laden (38c), de vorm is puur. B (`slots.sh run`, 721 tests) en D groen, geen diff, `ring3_surface.py` idempotent. **Perf hermeten in INT (surface).**

## W-checklist (INT3)
- [ ] **42c** lavascan (trede/val/lek): afgrond (pit) lek 0; de oude plek (4.409–4.412, Y 5–9, −510…−508) dicht.
- [ ] **54c** Nether-3-kloven (n3rifts, chunks 68…83 × −30…−15): 0 blootliggende ijsblokken onder Y 30 binnen 3 blokken (horizontaal) van een open lavakolom; scheuren aan de rand van `rime_forest` met gloeiende scheur/magma-wanden.
- [ ] **39g** lavarivieren (ook brede en eindpoelen, gebied E/rivieren en B): smalle zwarte oever (~3 blokken vanaf de lava), magma alleen direct langs de lava; features langs de rivieren als vóór 39g.
- [ ] **47e** determinisme omgekeerd geladen, gebied arch/archipel: geen `ash_layer`-verschil onder de vallen; overige verschillen daar alleen vloeiende lava (lucht ↔ lava) en geen obsidiaan meer op Y −55 (besluit B); geen val die in water eindigt.
- [ ] **42d** buismonden (bijv. (4.858, 73–78, 75) en de monden in de basaltstromen): een stuk askorstdak vóór de mond, de mond zelf zichtbaar; dunne daken boven buizen/daklichten in de korstbiomes `ash_crust`; in twee werelden (omgekeerd geladen) gelijk, geen vallende blokken na het laden; een speler eronder laat het dak instorten.
- [ ] Prestatie: surface (39g, 42d) en noise (42c) hermeten na INT3.

## CODEBASE-delta
- `worldgen/densityfunction/Landmarks.java`: 42c — `Abyss.near(px, pz, reach)`: of een positie binnen `reach` blokken van de lavazee kan liggen, ook bij de spitse eindes (bovengrens van de halve breedte).
- `worldgen/densityfunction/VolcanicField.java`: 42c — de band rond de lavazee van de afgrond (`guardBottom`/`guardTop`) reikt ook `ABYSS_GUARD` (8) blokken voorbij de zee, eindes inbegrepen.
- Tests: `Ring3TerrainWave3Test` (nieuw): elke cellhoek naast de afgrondzee ligt in de band.
- Valkuil (aanvulling op "Grond naast lava (42b)"): de band rond lava moet minstens een cel-diagonaal plus één blok voorbij de lava reiken, **ook waar een vorm spits uitloopt**; een band in eenheden van de breedte (0,3 halve breedte) wordt aan de eindes bijna nul.
- `noise_settings/nether.json` (uit `nether_ring3_surface.py`): 54c — de warme scheurregels (wanden Y 14–30, gloeiende scheur bovenop, warme kring, en boven Y 30 tot de top van de scheur kale netherrack met magma) gelden in de hele scheurzone (|regio| < 0,22, ook de randen van `rime_forest` en `glacier_rifts`), vanaf scheurpatches 0,3, en staan vóór alle ijsregels; `tools/worldgen/nether_ring3_surface.py`: `CRACK_ZONE`, `CRACKS_FROM`, `CRACKS_TOP`, `crack_wall()`.
- Tests: `NetherRing3JsonTest` — `theWarmCracksLineEveryOpenCrackBelowTheGlacierLevel` (nieuw: elke open scheur onder Y 30 heeft warme wanden, bemonsterd op het testzaad; de warme regels staan vóór alle ijsregels), de regioband ±0,22 toegestaan.
- Valkuil: de Nether-3-scheuren lopen door tot het gewicht van de ijszee 0 is (|regio| < 0,22), verder dan de biome-drempel 0,18; regels voor scheuren gebruiken die zone, niet `SEA`.
- `worldgen/densityfunction/VolcanicField.java`: 39g — `Column.lavaShore` (oever voor de surface, 3 blokken vanaf de lavarand, `SHORE_BLOCKS`) en `lavaEdge(preHeight, level)` (de lavarand over het kanaal in halve breedtes); `lavaBank` blijft de afstand die features van een rivier houden.
- `VolcanicOutput.java`: 39g — uitgang `lava_shore` (per kolom); `lava_bank` nu beschreven als "features blijven eruit".
- `noise_settings/overworld.json` (uit `ring3_surface.py`): 39g — `lava_banks()` leest `lava_shore`; magma alleen bij `lava_shore` ≥ 2/3 (eerste blok langs de lava), geen losse magma meer in de oever.
- Tests: `Ring3TerrainWave3Test` — `theLavaEdgeIsWhereTheChannelDropsBelowTheLava`, `theScorchedShoreHugsTheLavaWhateverTheRiversWidth`.
- `worldgen/densityfunction/LavaFallFlow.java` (nieuw, 47e): waar de lava van een val (39f) kan stromen, puur (ruime vanilla-stromingsregel over het geïnterpoleerde terrein, eigen kolomcache, `REACH` 96); `Flow` (kolommen, `wet`, `cut`, grenzen).
- `VolcanicField.java`: 47e — `fallFlow(fall)` (één keer per wereld, `Decisions.fallFlows`, nooit geleegd), `fallStaysDry(column)`/`springStaysDry(column)`, `fallFlowsNear(minX, minZ, maxX, maxZ)` (voor features; natte vallen niet), `ring3Proper(x, z)` (via `RingLayout.sampleOverworld`); `Column.springFall()`/`fall()` (de val achter `springY`/`fallTop`).
- `VolcanicCaches.Decisions`: `fallFlows`.
- `worldgen/chunk/RingAquifer.java`: 47e — een val die water kan raken (stroming onder Y 0 of buiten ring 3 zelf) wordt niet gemaakt: geen bron, geen vallende lava.
- `worldgen/chunk/RingAquifer.java`: 47e (besluit B) — in ring 3 wordt aquiferwater op Y −54 rots (zoals in ring 2), dus geen contact met vanilla's lavalaag; test `ring3LeavesStoneBetweenAquiferWaterAndTheLavaLayer`.
- `worldgen/feature/AshDriftFeature.java`: 47e — geen aslaag waar de lava van een val kan stromen (`inFallFlow`).
- Tests: `Ring3LavaFallFlowTest` (nieuw): elke gemaakte val heeft een stroomgebied, niet afgekapt; de aslaag van INT-afr ligt erin; puur (twee threads, omgekeerde volgorde).
- Valkuil: **vloeiende lava en water in worldgen** — wat de lava van een val (of een bron) bereikt, verandert op het moment dat ze stroomt (losse blokken weg, obsidiaan bij water); features die losse blokken leggen blijven uit `VolcanicField.fallFlowsNear`, en vallen die water kunnen raken worden niet gemaakt. Vraag je vanuit code die een kolom van het venster vasthoudt naar andere kolommen, gebruik dan een eigen cache (de `ColumnTable` hergebruikt de kolomobjecten bij het verschuiven).
- `worldgen/densityfunction/LavaTubes.java`: 42d — `LID_DROP`; een droge buis die minstens 3 diep door de grond breekt krijgt daar een dak van één blok (`addSolid(roof, roof, KIND_CRUST)`), de opening eindigt eronder.
- `Landmarks.java`: 42d — `KIND_CRUST` (6); `VolcanicOutput.THIN_KIND` tot 6.
- `noise_settings/overworld.json` (uit `ring3_surface.py`): 42d — `THIN_ROOF` in de korstbiomes → `ash_crust`; dunne vaste blokken van soort 6 → `ash_crust` (elk biome); `THIN_ANY` tot 7. CODEBASE-regel over "worldgen plaatst geen `ash_crust` meer" (42b) vervalt.
- Tests: `Ring3TerrainJsonTest` (korst in de ring 3-tak, niet in de wandregel), `Ring3TerrainWave3Test.everyTubeMouthHasAPieceOfAshCrustRoofInFrontOfIt`.

## Beslissingen
- 2026-10-10 · worldgen · 42d: het stuk dun dak vóór een buismond ligt waar de mondhelling door de grond breekt en minstens 3 blokken diep is (dus 3+ blokken val), in elk biome askorst; het ondiepe begin van de geul blijft open zodat de mond zichtbaar blijft; dunne daken boven buizen/daklichten alleen in de korstbiomes (zoals 39c/42b) · uitvoering van het besluit "askorst op dunne daken én een stuk dun dak vóór elke buismond" (gebruiker 2026-10-10)
- 2026-10-10 · worldgen · Lavaoever (39g): 3 blokken vanaf de lavarand (zelfde breedte als de oude oever náást het kanaal), magma alleen in het eerste blok met de bestaande vlekruis; features houden de oude afstand (`lava_bank`) · uitvoering van het besluit "smaller, plafond in blokken, magma alleen direct langs de lava" (gebruiker 2026-10-10); de features ongewijzigd laten houdt de decoratie langs rivieren gelijk
- 2026-10-10 · worldgen · Archipelvallen (47e): het stroomgebied is een ruime schatting van de vanilla-stroming over het terrein (naar alle kanten, grond 1 lager); "water raken" = stroming onder Y 0 (aquiferwater in ring 3) of buiten ring 3 zelf; net als een val waarvan de stroming afgekapt is; zo'n val wordt helemaal niet gemaakt (bron + val) · uitvoering van het besluit "geen aslaag in het stroomgebied, vallen eindigen niet in water" (gebruiker 2026-10-10)
- 2026-10-10 · worldgen · Ring 3 krijgt de ring 2-regel: één blok steen tussen diep aquiferwater en vanilla's lavalaag (Y −54); ring 1 blijft vanilla · het contact werd obsidiaan op het moment dat het water stroomde, afhankelijk van de laadvolgorde (INT-afr, Y −55 onder de archipel; besluit gebruiker B)

## Backlog
- `worldgen` | Vloeiende lava van vallen onder smeulbomen (`ember_woods`): lava kan smeulblad aansteken (vuur verspreidt op een moment dat van het laden afhangt). Alleen bekijken als het in een determinismescan opduikt. (bron: 47e)
