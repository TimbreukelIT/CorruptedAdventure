# Ring 3 — golf 3, stroom B: 38c askorst die alleen onder een speler instort

Scope: `blocks`. Branch `ring3/blocks-g3`, basis main `58a7325` (≥ `e2a0366`). Poort n = 0 (niet gebruikt: alleen B + D + unit).

## Wat
- `ash_crust` (bestaand blok, V1 = A) valt niet meer door plaatsen, buurupdates, worldgen-postprocessing, chunk laden of zuigers. Hij stort alleen in (vallend blok, zoals nu) als een speler er direct onder staat of loopt.
- **Detectie (keuze, implementatie):** de bestaande `LooseSandTrigger` (`PlayerTickEvent.Post`, server-side, elke 10 ticks per speler, gespreid op entity-id; vak = voetafdruk ±1, van de hoofdhoogte tot 2 blokken erboven; alleen echte `ServerPlayer`s, geen spectators/fake players/mobs). Waarom: bestaat al, getest en goedkoop (27 blockstate-lookups per speler per 10 ticks), geen random/scheduled ticks op elk korstblok in de wereld (die zouden in elke geladen chunk lopen en het blok zelf laten "kijken"). Op de korst lopen doet niets.
- **Ketting (V2 = A; na review):** omdat de korst buurupdates negeert, maakt `AshCrustCollapse` de ketting zelf, in twee delen:
  - *plan bij de start:* breedte-eerst over alle 26 buren (zijden, diagonaal, één lager en één hoger: getrapte en ruisvormige daken), lid als de buur askorst is én vrij hangt; askorst met al een geplande tick (eerdere instorting) is doorgang, wordt niet opnieuw gepland. Max. 64 geplande blokken (max. 256 bekeken), elk `15 + 2 × afstand` ticks na de waarschuwing;
  - *bij elke echte val* (`AshCrustBlock.tick`): de askorst die erop rust volgt na 2 ticks, los van het plan en de limiet. Dus nooit een zwevend restant op een gevallen blok (ook niet voorbij 64 of bij twee instortingen naast elkaar).
  - Elke tick beslist opnieuw (vanilla: alleen vallen met ruimte eronder). Is er na het plan steun onder een blok gekomen, dan blijft alleen dat blok (gestut) staan, met wat erop rust; de rest van het plan valt. Eenvoudigste vorm zonder zwevende resten: er blijft niets hangen dat vóór de instorting niet al hing.
- Geland vallend blok: wordt weer een hangende korst (geen tick bij `onPlace`).
- **Stof:** onder hangende korst af en toe een stofje (`animateTick` 1 op 160, zand 1 op 16); het duidelijke stof + kraak komt van de trigger vlak vóór het instorten.
- **Generiek:** `CollapsingBlock` (interface, `startCollapse`; `CollapsingBlock.start` geeft blokken zonder de interface, zoals `loose_sand`, een gewone geplande tick). `LooseSandTrigger` kent `AshCrustBlock` niet.

## Afspraak voor W (42d)
- Plaats gewoon `corruptedadventure:ash_crust` (default state; er is geen extra blockstate). Geen updates of ticks nodig; het blok valt nooit door worldgen, postprocessing of het laden van chunks, ook niet boven lucht, water of lava.
- Een dak dat moet instorten: askorst met lucht/vloeistof eronder, aaneengesloten (zijden, hoeken of trapjes van één blok raken elkaar) zodat de ketting het hele stuk meeneemt; een stuk groter dan 64 blokken stort in delen in (per speler-trigger), het rest-dak blijft gewoon hangen. Askorst die op askorst rust valt mee zodra het blok eronder valt. Korst op vast gesteente valt niet en breekt de ketting.
- Spelers activeren het binnen 2 blokken boven de hoofdhoogte (een dak 3+ blokken boven de vloer van de gang stort niet in tot de speler hoger komt; bewust: tot ~4 blokken holte onder een dak van 1 werkt).
- De worldgen-tests die "geen `ash_crust` uit worldgen" eisen (`Ring3TerrainJsonTest`, `Ring3TerrainFixesTest`) en de `ash_block`-regel in `ring3_surface.py` zijn van W (42d).

## Bestanden
- Nieuw: `block/AshCrustBlock.java`, `block/AshCrustCollapse.java`, `block/CollapsingBlock.java`; tests `block/AshCrustBlockTest.java`, `block/AshCrustCollapseTest.java`.
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
- Voortgang: 2026-10-10 13:43 commit `022df74`, review aangevraagd.
- Voortgang: 2026-10-10 14:57 review (1–6) verwerkt: `git merge main` (tools/test), ketting over 26 buren met doorgang door geplande korst, korst op een gevallen blok volgt altijd (`fell`), tick beslist opnieuw, stof 1/160, `CollapsingBlock`; tests uitgebreid (simulatie: niets nieuws zwevend). B + D groen via `tools/test/slots.sh`.

## W-checklist voor INT
- [ ] Askorst door een speler geplaatst boven lucht blijft hangen (ook na een buurupdate, bijv. blok ernaast breken).
- [ ] Speler loopt eronder → stof + kraak, na ~0,75 s valt het blok; een aaneengesloten hangend korstdak (ook getrapt/diagonaal) volgt als ketting (~2 ticks per stap, max. 64); gestapelde korst volgt; geen zwevende resten.
- [ ] Onder hangende korst af en toe een stofje (niet als bij zand).
- [ ] Op de korst lopen: niets. Mob eronder: niets. Spectator: niets.
- [ ] Na 42d: dunne daken boven buizen/daklichten en vóór buismonden zijn askorst, hangen na het laden (determinisme omgekeerd geladen: geen verschil in `ash_crust`).
- [ ] Dedicated server: geen client-class-fouten.

## CODEBASE-delta
- Mappenlijst `block/`: `AshCrustBlock.java` ✅ (38c) `FallingBlock` die nooit zelf een val plant (`onPlace`/`updateShape` leeg; codec met `falling_dust_color`): valt niet door updates, worldgen-postprocessing, chunk laden of zuigers; alleen via `LooseSandTrigger`. `AshCrustCollapse.java` ✅ (38c) ketting van een askorst-instorting via `Crusts` (level of testwereld): `plan` (breedte-eerst over 26 buren, vrijhangende askorst, geplande korst = doorgang, max. `MAX_BLOCKS` 64 gepland / 256 bekeken) + `start` (geplande tick `delay + 2 × afstand`) + `fell` (korst op een gevallen blok volgt na 2 ticks, los van de limiet). `CollapsingBlock.java` ✅ (38c) interface voor `collapsing_blocks` met een eigen instorting (`startCollapse`); `CollapsingBlock.start` geeft de rest (`loose_sand`) een gewone geplande tick. `AshCrustBlock`: ook `tick` (na een echte val `fell`) en `animateTick` (stof 1 op 160).
- `LooseSandTrigger.java`-regel: "… → stof + geluid + `CollapsingBlock.start` (15; askorst: ketting via `AshCrustCollapse`, anders scheduled tick)".
- Tests: `block/AshCrustBlockTest.java` ✅ (38c) geen tick bij `updateShape`/`onPlace` (level `null`), geen random ticks, stofkleur `8A8580`. `block/AshCrustCollapseTest.java` ✅ (38c) heel dak dichtstbij eerst, korst op rots blijft en knipt de ketting, limiet 64 met de dichtstbijzijnde, getrapte/diagonale daken, en met een tick-simulatie: gestapelde korst volgt ook voorbij de limiet, eerdere instorting is doorgang, steun na het plan houdt alleen het eigen blok; nooit iets nieuws zwevend. `AshCrustBlockTest` ook stofkans 160. `ModBlocksTest`: askorst is `AshCrustBlock`.
- Blokkentabel `ash_crust`: klasse `AshCrustBlock` (38c; `FallingBlock` zonder eigen val); "hangt tot een speler eronder loopt (`collapsing_blocks`, `LooseSandTrigger`), dan stort de aaneengesloten hangende korst in (`AshCrustCollapse`, max. 64); ook door spelers geplaatst blijft hij hangen; worldgen mag hem op dunne daken leggen (42d)". Rest ongewijzigd.
- Tag `collapsing_blocks`: ongewijzigd.
- Valkuil (bij "`loose_sand`-trigger"): askorst valt alleen door een speler eronder; RCON/zonder speler/`setblock` triggert nooit; test met de quick-play-client.

## Beslissingen
- 2026-10-10 · blocks · 38c: het bestaande `ash_crust` wordt de askorst die alleen onder een speler instort (geen nieuw blok); ook door spelers geplaatste askorst blijft hangen tot een speler eronder komt · V1 = A (gebruiker via main)
- 2026-10-10 · blocks · 38c: een instorting neemt de aaneengesloten vrijhangende askorst mee (ketting, ~2 ticks per stap, max. 64 blokken per instorting), alleen door een speler gestart · V2 = A (gebruiker via main); gevoel zoals `loose_sand`
- 2026-10-10 · blocks · 38c: onder hangende askorst af en toe een stofje (1 op 160, tien keer zeldzamer dan zand); het duidelijke stof + kraak vlak vóór het instorten blijft · gebruiker via main (review 38c)
- 2026-10-10 · blocks · 38c: de ketting loopt over getrapte/diagonale daken en door eerdere instortingen heen; askorst op een gevallen blok valt altijd mee, ook voorbij de limiet; elke tick beslist opnieuw (gestut blijft staan) · review 38c: nooit een zwevend restant
- 2026-10-10 · blocks · 38c: detectie via de bestaande `LooseSandTrigger` (speler-tick, vak boven het hoofd), geen random/scheduled ticks op het blok zelf · goedkoop, server-side, alleen spelers (implementatie)

## Backlog
- [blocks] Backlogregel 38b (`ash_crust` stort alleen in als een speler eronder loopt; van bovenaf beslissen na 4b): **afgehandeld in 38c** (alleen van onderen, niet door updates).
- [worldgen] 42b-regel "een askorst die alleen onder een speler instort: eigen backlogregel (`blocks`)": blokdeel gedaan (38c), plaatsing in 42d (W).
