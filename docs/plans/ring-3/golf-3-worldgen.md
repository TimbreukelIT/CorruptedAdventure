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

## W-checklist (INT3)
- [ ] **42c** lavascan (trede/val/lek): afgrond (pit) lek 0; de oude plek (4.409–4.412, Y 5–9, −510…−508) dicht.
- [ ] **54c** Nether-3-kloven (n3rifts, chunks 68…83 × −30…−15): 0 blootliggende ijsblokken onder Y 30 binnen 3 blokken (horizontaal) van een open lavakolom; scheuren aan de rand van `rime_forest` met gloeiende scheur/magma-wanden.
- [ ] **39g** lavarivieren (ook brede en eindpoelen, gebied E/rivieren en B): smalle zwarte oever (~3 blokken vanaf de lava), magma alleen direct langs de lava; features langs de rivieren als vóór 39g.
- [ ] Prestatie: surface (39g) en noise (42c) hermeten na INT3.

## CODEBASE-delta
- `worldgen/densityfunction/Landmarks.java`: 42c — `Abyss.near(px, pz, reach)`: of een positie binnen `reach` blokken van de lavazee kan liggen, ook bij de spitse eindes (bovengrens van de halve breedte).
- `worldgen/densityfunction/VolcanicField.java`: 42c — de band rond de lavazee van de afgrond (`guardBottom`/`guardTop`) reikt ook `ABYSS_GUARD` (8) blokken voorbij de zee, eindes inbegrepen.
- Tests: `Ring3TerrainWave3Test` (nieuw): elke cellhoek naast de afgrondzee ligt in de band.
- Valkuil (aanvulling op "Grond naast lava (42b)"): de band rond lava moet minstens een cel-diagonaal plus één blok voorbij de lava reiken, **ook waar een vorm spits uitloopt**; een band in eenheden van de breedte (0,3 halve breedte) wordt aan de eindes bijna nul.
- `noise_settings/nether.json` (uit `nether_ring3_surface.py`): 54c — de warme scheurregels (wanden Y 14–30, gloeiende scheur bovenop, warme kring) gelden in de hele scheurzone (|regio| < 0,22, ook de randen van `rime_forest` en `glacier_rifts`) en vanaf scheurpatches 0,3; `tools/worldgen/nether_ring3_surface.py`: `CRACK_ZONE`, `CRACKS_FROM`.
- Tests: `NetherRing3JsonTest` — `theWarmCracksLineEveryOpenCrackBelowTheGlacierLevel` (nieuw: elke open scheur onder Y 30 heeft warme wanden, bemonsterd op het testzaad), de regioband ±0,22 toegestaan.
- Valkuil: de Nether-3-scheuren lopen door tot het gewicht van de ijszee 0 is (|regio| < 0,22), verder dan de biome-drempel 0,18; regels voor scheuren gebruiken die zone, niet `SEA`.
- `worldgen/densityfunction/VolcanicField.java`: 39g — `Column.lavaShore` (oever voor de surface, 3 blokken vanaf de lavarand, `SHORE_BLOCKS`) en `lavaEdge(preHeight, level)` (de lavarand over het kanaal in halve breedtes); `lavaBank` blijft de afstand die features van een rivier houden.
- `VolcanicOutput.java`: 39g — uitgang `lava_shore` (per kolom); `lava_bank` nu beschreven als "features blijven eruit".
- `noise_settings/overworld.json` (uit `ring3_surface.py`): 39g — `lava_banks()` leest `lava_shore`; magma alleen bij `lava_shore` ≥ 2/3 (eerste blok langs de lava), geen losse magma meer in de oever.
- Tests: `Ring3TerrainWave3Test` — `theLavaEdgeIsWhereTheChannelDropsBelowTheLava`, `theScorchedShoreHugsTheLavaWhateverTheRiversWidth`.

## Beslissingen
- 2026-10-10 · worldgen · Lavaoever (39g): 3 blokken vanaf de lavarand (zelfde breedte als de oude oever náást het kanaal), magma alleen in het eerste blok met de bestaande vlekruis; features houden de oude afstand (`lava_bank`) · uitvoering van het besluit "smaller, plafond in blokken, magma alleen direct langs de lava" (gebruiker 2026-10-10); de features ongewijzigd laten houdt de decoratie langs rivieren gelijk

## Backlog
