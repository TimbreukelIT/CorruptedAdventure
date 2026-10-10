# Ring 3 — golf 3, stroom B: 38c askorst die alleen onder een speler instort

Scope: `blocks`. Branch `ring3/blocks-g3`, basis main `58a7325` (≥ `e2a0366`). Poort n = 0 (niet gebruikt: alleen B + D + unit).

## Wat
- `ash_crust` (bestaand blok, V1 = A) valt niet meer door plaatsen, buurupdates, worldgen-postprocessing, chunk laden of zuigers. Hij stort alleen in (vallend blok, zoals nu) als een speler er direct onder staat of loopt.
- **Detectie (keuze, implementatie):** de bestaande `LooseSandTrigger` (`PlayerTickEvent.Post`, server-side, elke 10 ticks per speler, gespreid op entity-id; vak = voetafdruk ±1, van de hoofdhoogte tot 2 blokken erboven; alleen echte `ServerPlayer`s, geen spectators/fake players/mobs). Waarom: bestaat al, getest en goedkoop (27 blockstate-lookups per speler per 10 ticks), geen random/scheduled ticks op elk korstblok in de wereld (die zouden in elke geladen chunk lopen en het blok zelf laten "kijken"). Op de korst lopen doet niets.
- **Ketting (V2 = A):** omdat de korst buurupdates negeert, plant `AshCrustCollapse` de ketting meteen bij de start: breedte-eerst over aangrenzende askorst (4 zijden + erboven) die vrij hangt (vrij eronder, of rust op een blok van deze instorting), max. 64 blokken, elk blok `15 + 2 × afstand` ticks na de waarschuwing. Blokken met al een geplande tick doen niet mee (die vallen al). Alleen door een speler gestart, dus deterministisch.
- Geland vallend blok: wordt weer een hangende korst (geen tick bij `onPlace`).

## Afspraak voor W (42d)
- Plaats gewoon `corruptedadventure:ash_crust` (default state; er is geen extra blockstate). Geen updates of ticks nodig; het blok valt nooit door worldgen, postprocessing of het laden van chunks, ook niet boven lucht, water of lava.
- Een dak dat moet instorten: askorst met lucht/vloeistof eronder, aaneengesloten (zijden raken elkaar) zodat de ketting het hele stuk meeneemt; een korststuk groter dan 64 blokken stort in delen in (per speler-trigger). Korst op vast gesteente valt niet en breekt de ketting.
- Spelers activeren het binnen 2 blokken boven de hoofdhoogte (een dak 3+ blokken boven de vloer van de gang stort niet in tot de speler hoger komt; bewust: tot ~4 blokken holte onder een dak van 1 werkt).
- De worldgen-tests die "geen `ash_crust` uit worldgen" eisen (`Ring3TerrainJsonTest`, `Ring3TerrainFixesTest`) en de `ash_block`-regel in `ring3_surface.py` zijn van W (42d).

## Bestanden
- Nieuw: `block/AshCrustBlock.java`, `block/AshCrustCollapse.java`; tests `block/AshCrustBlockTest.java`, `block/AshCrustCollapseTest.java`.
- Gewijzigd: `registry/ModBlocks.java` (`ASH_CRUST` → `DeferredBlock<AshCrustBlock>`), `block/LooseSandTrigger.java` (askorst via `AshCrustCollapse.start`), `registry/ModBlocksTest.java`.
- Datagen ongewijzigd (zelfde naam, model, loot, tags).

## Client/server
- Alles server-side (trigger en geplande ticks). Stof en geluid via vanilla-pakketten; `animateTick` (vallend stof onder een hangende korst) draait op het client-level zonder client-class. Geen netwerk.

## Testen
- B: `./gradlew build --rerun-tasks` (unit + `ClientIsolationTest`). D: `runData` + `git diff --exit-code src/generated`.

## Voortgang
- Voortgang: 2026-10-10 13:24 worktree `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-add6d63a5ef90eede`, branch `ring3/blocks-g3` (vanaf main `58a7325`).
- Voortgang: 2026-10-10 13:26 regels, design en code gelezen; V1/V2 aan main gevraagd.
- Voortgang: 2026-10-10 13:40 antwoord gebruiker V1 = A, V2 = A; `AshCrustBlock`, `AshCrustCollapse`, trigger, tests geschreven; B gestart (slot 1).
- Voortgang: 2026-10-10 13:42 B groen (`build --rerun-tasks`, alle tests incl. 9 nieuwe), D groen (`runData`, `src/generated` ongewijzigd); slot vrij; commit en review-doorsturen.

## W-checklist voor INT
- [ ] Askorst door een speler geplaatst boven lucht blijft hangen (ook na een buurupdate, bijv. blok ernaast breken).
- [ ] Speler loopt eronder → stof + kraak, na ~0,75 s valt het blok; een aaneengesloten hangend korstdak volgt als ketting (~2 ticks per stap, max. 64).
- [ ] Op de korst lopen: niets. Mob eronder: niets. Spectator: niets.
- [ ] Na 42d: dunne daken boven buizen/daklichten en vóór buismonden zijn askorst, hangen na het laden (determinisme omgekeerd geladen: geen verschil in `ash_crust`).
- [ ] Dedicated server: geen client-class-fouten.

## CODEBASE-delta
- Mappenlijst `block/`: `AshCrustBlock.java` ✅ (38c) `FallingBlock` die nooit zelf een val plant (`onPlace`/`updateShape` leeg; codec met `falling_dust_color`): valt niet door updates, worldgen-postprocessing, chunk laden of zuigers; alleen via `LooseSandTrigger`. `AshCrustCollapse.java` ✅ (38c) ketting van een askorst-instorting: pure `plan` (breedte-eerst, 4 zijden + erboven, vrij eronder of rustend op de instorting, max. `MAX_BLOCKS` 64) + `start` (geplande tick `delay + 2 × afstand`; blokken met al een tick doen niet mee).
- `LooseSandTrigger.java`-regel: "… → stof + geluid + scheduled tick (15); askorst via `AshCrustCollapse.start` (ketting)".
- Tests: `block/AshCrustBlockTest.java` ✅ (38c) geen tick bij `updateShape`/`onPlace` (level `null`), geen random ticks, stofkleur `8A8580`. `block/AshCrustCollapseTest.java` ✅ (38c) heel dak dichtstbij eerst, korst op rots blijft en knipt de ketting, limiet 64 met de dichtstbijzijnde, gestapelde korst één stap later, bezette korst doet niet mee. `ModBlocksTest`: askorst is `AshCrustBlock`.
- Blokkentabel `ash_crust`: klasse `AshCrustBlock` (38c; `FallingBlock` zonder eigen val); "hangt tot een speler eronder loopt (`collapsing_blocks`, `LooseSandTrigger`), dan stort de aaneengesloten hangende korst in (`AshCrustCollapse`, max. 64); ook door spelers geplaatst blijft hij hangen; worldgen mag hem op dunne daken leggen (42d)". Rest ongewijzigd.
- Tag `collapsing_blocks`: ongewijzigd.
- Valkuil (bij "`loose_sand`-trigger"): askorst valt alleen door een speler eronder; RCON/zonder speler/`setblock` triggert nooit; test met de quick-play-client.

## Beslissingen
- 2026-10-10 · blocks · 38c: het bestaande `ash_crust` wordt de askorst die alleen onder een speler instort (geen nieuw blok); ook door spelers geplaatste askorst blijft hangen tot een speler eronder komt · V1 = A (gebruiker via main)
- 2026-10-10 · blocks · 38c: een instorting neemt de aaneengesloten vrijhangende askorst mee (ketting, ~2 ticks per stap, max. 64 blokken per instorting), alleen door een speler gestart · V2 = A (gebruiker via main); gevoel zoals `loose_sand`
- 2026-10-10 · blocks · 38c: detectie via de bestaande `LooseSandTrigger` (speler-tick, vak boven het hoofd), geen random/scheduled ticks op het blok zelf · goedkoop, server-side, alleen spelers (implementatie)

## Backlog
- [blocks] Backlogregel 38b (`ash_crust` stort alleen in als een speler eronder loopt; van bovenaf beslissen na 4b): **afgehandeld in 38c** (alleen van onderen, niet door updates).
- [worldgen] 42b-regel "een askorst die alleen onder een speler instort: eigen backlogregel (`blocks`)": blokdeel gedaan (38c), plaatsing in 42d (W).
