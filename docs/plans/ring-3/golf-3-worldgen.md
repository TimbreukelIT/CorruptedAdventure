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

## W-checklist (INT3)
- [ ] **42c** lavascan (trede/val/lek): afgrond (pit) lek 0; de oude plek (4.409–4.412, Y 5–9, −510…−508) dicht.

## CODEBASE-delta
- `worldgen/densityfunction/Landmarks.java`: 42c — `Abyss.near(px, pz, reach)`: of een positie binnen `reach` blokken van de lavazee kan liggen, ook bij de spitse eindes (bovengrens van de halve breedte).
- `worldgen/densityfunction/VolcanicField.java`: 42c — de band rond de lavazee van de afgrond (`guardBottom`/`guardTop`) reikt ook `ABYSS_GUARD` (8) blokken voorbij de zee, eindes inbegrepen.
- Tests: `Ring3TerrainWave3Test` (nieuw): elke cellhoek naast de afgrondzee ligt in de band.
- Valkuil (aanvulling op "Grond naast lava (42b)"): de band rond lava moet minstens een cel-diagonaal plus één blok voorbij de lava reiken, **ook waar een vorm spits uitloopt**; een band in eenheden van de breedte (0,3 halve breedte) wordt aan de eindes bijna nul.

## Beslissingen

## Backlog
