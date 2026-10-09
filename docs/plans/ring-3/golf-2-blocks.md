# Golf 2 — stroom B (blokken): 46-rest (geluiden en particles aan fumarole, geiser, modder; afstemming)

Scope: `blocks` — werkpakket 46 (rest), branch `ring3/blocks-g2`, poort 0 (server 25570, RCON 25580). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:**
- de blokken van 46 krijgen hun eigen geluiden en de `steam_puff`-particle van A, in plaats van de vanilla-tijdelijke;
- het gedrag wordt afgestemd na 4a (geiser-ritme, eventueel een waarschuwing).

Klein pakket. Daarna is B beschikbaar voor nieuwe blokken die de golf 2-features via main vragen.

**Bron:**
- `golf-1-blocks.md` (46, overdracht, valkuilen);
- `contract.md` §3 (46) en §6 (sound ids `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble`; particle `steam_puff`);
- backlog r. 177, 179.

## 1. Werk (één commit)
1. **Geluiden** (events van A, contract §6):
   - `FumaroleBlock.animateTick`: af en toe `block.fumarole.puff` (lokaal, client-level, zoals de particles); niet als de opening bedekt is (`GeyserBlock.ventOpen`).
   - `GeyserBlock`: de uitbarsting speelt `block.geyser.burst` server-side (`level.playSound` met `null`-speler, dus voor iedereen hoorbaar) in plaats van `GENERIC_EXTINGUISH_FIRE`.
   - `BoilingMudBlock.animateTick`: `block.boiling_mud.bubble` bij een deel van de bubbels.
   - Volume en kans zo dat een veld van tientallen fumarolen of modderblokken niet gaat dreunen. Een cap per tick per client is een implementatiekeuze, maar meet het in `steam_fens` (48) en de zwavelvelden (47).
2. **Particles:** `steam_puff` (A) vervangt `CAMPFIRE_COSY_SMOKE`/`CLOUD` in fumarole en geiser. Tint of eigen kleur: afspraak A ↔ T (backlog r. 182).
3. **Afstemming geiser** (backlog r. 177): ritme en duw volgens G2-B1. Waarschuwing: server-side, met een extra geplande tick (bijv. 20 ticks vóór de uitbarsting: borrelen en een kleine stoompuf via `sendParticles`), zodat alle clients hetzelfde zien.
4. **Kokende modder:** gedrag volgens G2-B2.
5. **Nieuwe blokken uit golf 2:** geen gepland. Vraagt W-biomes of W-nether er een via main (bijv. een eigen schedelblok of stoomvent), dan komt het als apart pakket (`46b`/`51b`) met een eigen commit.

## 2. Bestanden
- **Eigen:** `block/FumaroleBlock`, `block/GeyserBlock`, `block/BoilingMudBlock` (+ tests), en zo nodig `ModBlocks`/blok-datagen/`Ring3BlockNames`.
- **Leest van A (niet wijzigen):** `ModSoundEvents`, `ModParticleTypes`.

## 3. Afhankelijkheden
- **← A** (hard): de sound events en `steam_puff` moeten bestaan.
  - Voorstel: A registreert ze in voorpakket 50-0 op main, met vanilla-verwijzingen in `sounds.json` en de bestaande sprites van T (zie `golf-2-atmosphere.md`). Dan start B vanaf die commit.
  - Zonder voorpakket wacht B op de merge van A's 50. B is klein en kan ook als laatste.
- **→ W-biomes:** 47 (fumarolen) en 48 (`steam_fens`: geisers, modder) gebruiken deze blokken; het geluid zit in het blok, niet in de feature. Geen wachtrelatie.
- **→ C:** geen (de hittetags veranderen niet).

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-B1 — Geiser: welk ritme en welke duw?** (nu elke 100 ticks vast, ~4,9 blokken)
- **A. Vast ritme per geiser maar verschillend per positie (80–200 ticks, deterministisch uit de positie), 1 s waarschuwing (borrelen + kleine puf), duw ~5 blokken (aanbevolen).** C laag (extra geplande tick). MP: alles server-side, iedereen ziet hetzelfde. S: een veld geisers "leeft" (niet in de maat) en je kunt timen.
- **B. Hoge geisers in `steam_fens` (duw ~10–12 blokken) als lanceerplatform om plateaus en randen te bereiken, met waarschuwing.** C laag. S: wow en mobiliteit; risico op valschade naast de geiser.
- **C. Zoals nu (100 ticks, ~5 blokken, geen waarschuwing).** C geen. S: voorspelbaar, wat mechanisch.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-B2 — Kokende modder: alleen traag, of ook gevaarlijk?** (nu: honing-traagheid + hittebron)
- **A. Zoals nu (traag + hittebron) (aanbevolen).** C geen. S: hinderlijk, niet dodelijk; past bij de schaarste zonder straf.
- **B. Ook kleine brandschade (1 per 2 s, zoals magma; sluipend lopen beschermt).** C laag. S: modderpoelen worden echte obstakels in `steam_fens`.
- **C. Gevallen items zinken weg (worden vernietigd).** C laag/middel. S: frustrerend; weinig winst.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
- **Geluid en particles:** fumarole en modder via `animateTick` (client-level, geen client-class); geiser-geluid en -particles server-side (`playSound`, `sendParticles`), dus voor iedereen hetzelfde.
- Geen nieuwe payloads.

## 6. Tests
- **B:** `./gradlew build` groen; `GeyserBlockTest` + interval/waarschuwing (puur), `ModBlocksTest` ongewijzigd.
- **D:** `runData` groen.
- **S:** server (25570) + client.
  - Geiser: uitbarsting met geluid; waarschuwing vóór de duw; bedekt = stil.
  - Fumarole puft met `steam_puff` + geluid.
  - Modder bubbelt hoorbaar.
  - Twee clients horen/zien de geiser op hetzelfde moment.
- **Review:** "klaar voor review-doorsturen: 46-rest" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (in-game; na 48 ook in `steam_fens`, `/ca tpboundary 3`)
- [x] Fumarole: stoompuf (`steam_puff`) met een zacht sissen; stil als er een blok op ligt. (client: puf + ondertitel; stil-als-bedekt via `ventOpen`, ongewijzigd uit 46)
- [x] Geiser: waarschuwing (borrelen), dan de uitbarsting met geluid; een veld geisers loopt niet in de maat; landen geeft geen valschade. (S via RCON + client: uitbarsting met ondertitel, niet in de maat, bedekte stil)
- [x] Kokende modder: bubbelt hoorbaar; traagheid en hitte zoals in 46 (of G2-B2). (ondertitel "Mud bubbles"; heat-tags ongewijzigd)
- [ ] (gebruiker, teststop 4b — de agent kan niet luisteren) Een veld van tientallen fumarolen of modderblokken klinkt levendig, niet dreunend (Particles: Minimal → minder puffen).

## Voortgang
Worktree: `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a945f84b54317ff61`, branch `ring3/blocks-g2` (basis main `f81d76a`).
Voortgang: 2026-10-08 start 46-rest (plan en bronnen gelezen)
Voortgang: 2026-10-08 17:05 code + unit tests klaar; B wacht op een JVM-slot (39f meet prestatie met alle slots)
Voortgang: 2026-10-08 17:25 B groen (561 tests, 0 fouten, incl. `ClientIsolationTest`), D groen (geen wijziging in `src/generated`)
Voortgang: 2026-10-08 17:28 S (RCON, server 25570, `blocks-g2-s1`) groen, 0 fouten: 4 geisers met intervallen 141/186/121/176 barsten exact op hun interval en op de voorspelde fase uit (Python-port van `interval`/`phase`), eerste uitbarsting ≥ 20 ticks na plaatsen (waarschuwing eerst), duw max y 205,90 (~4,9 blokken), buren niet in de maat, bedekte geiser stil, stand op het buurblok blijft staan; heat-tags ongewijzigd
Voortgang: 2026-10-08 17:35 client (Dev, quick-play 25570) gejoind, testveld gebouwd (27 fumarolen, 78 modder, geisers); gestopt op verzoek van main vóór de screenshots
Voortgang: 2026-10-09 11:47 overgenomen door een nieuwe agent; B opnieuw groen, D groen (geen diff in `src/generated`)
Voortgang: 2026-10-09 12:10 client-check (server 25570 `blocks-g2-c1`, eigen client via PID, lock + 2 slots, daarna vrijgegeven): testveld op y 140 (30 fumarolen, 72 modder, 3 open geisers + 1 bedekte). Fumarolen puffen `steam_puff`; geisers barsten uit met een `steam_puff`-zuil en niet in de maat; de bedekte blijft stil; ondertitels "Fumarole hisses", "Mud bubbles", "Geyser erupts" verschijnen. Over "niet dreunend" zeggen de ondertitels alleen dat er per soort één regel tegelijk staat (de begrenzer werkt); écht luisteren kan de agent niet, dat is aan de gebruiker bij teststop 4b. De geiser-waarschuwing is op de screenshots niet los te onderscheiden van de uitbarsting (S via RCON toonde haar wel: de eerste uitbarsting kwam ≥ 20 ticks na de waarschuwing). Screenshots: `build/c46-overview.png`, `build/c46-fumarole-{1..3}.png`, `build/c46-mud-{1,2}.png`, `build/c46-geyser-{1..8}.png` (in de worktree)
Voortgang: 2026-10-09 12:12 review (main, `/code-review medium`): 9 bevindingen; 1–6 opgelost in code/tests, 7–8 in de delta-secties hieronder, 9 blijft zo (staat al in de backlog)
Voortgang: 2026-10-09 12:20 B groen (564 tests, 0 fouten, incl. `ClientIsolationTest`), D groen (geen diff in `src/generated`), S (RCON, `blocks-g2-s2`, seed 20260930) groen: 4 geisers (intervallen 123/169/157/111 met het gezoute ritme, Python-port in `stest.py`) barsten exact op hun interval en voorspelde fase uit, eerste uitbarsting na een volledige waarschuwing, duw max y 205,90, buren niet in de maat, bedekte stil; heat-tags ongewijzigd. Client-check niet herhaald (geen zichtbare gedragswijziging buiten de geluidsvoorrang)

## Status bij onderbreking (2026-10-08 17:40) — afgehandeld 2026-10-09 (client-check gedaan, zie voortgang)
- **Worktree:** `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a945f84b54317ff61`, branch `ring3/blocks-g2` (basis `f81d76a`). **Niets gecommit** (wacht op review).
- **Af (niet gecommit):** `GeyserBlock` (ritme per positie, waarschuwing, `startCycle`, `steam_puff`, `block.geyser.burst`), `FumaroleBlock` (`steam_puff`, `block.fumarole.puff`), `BoilingMudBlock` (`block.boiling_mud.bubble`), nieuw `AmbientSoundLimiter`, tests `GeyserBlockTest` (uitgebreid) en `AmbientSoundLimiterTest`; plan-secties CODEBASE-delta/Beslissingen/Backlog ingevuld.
- **Getest:** B groen, D groen, S via RCON groen (zie voortgang). Main is al geïnformeerd dat `INTERVAL_TICKS` verdwijnt (`startCycle` voor 48).
- **Nog te doen:** S met client: screenshots met ondertitels (`showSubtitles:true` staat in `run/options.txt`) van fumarole-puf, modderbubbel, geiser-waarschuwing en -uitbarsting met `steam_puff`; veld klinkt niet dreunend. Scripts in `build/blocks-g2/` (`scycle.ps1 -Level <naam> -KeepRunning`, `stest.py`, `rcon.py`, `shot.ps1` — kiest alleen de client van deze worktree). Daarna "klaar voor review-doorsturen: 46-rest" aan main, review verwerken, committen (alleen de 7 paden hierboven + dit plan), eindrapport.
- Processen gestopt (server via RCON `stop`, eigen client), `enable-rcon=false`, lock en slots vrij.

## CODEBASE-delta
- `block/GeyserBlock.java` (vervangt de regel van 46, CODEBASE.md r. 211/564): geiser (46, ritme 46-rest). Ritme in record `Rhythm(interval, offset)`, één keer per tick berekend: `Rhythm.of(WorldGenLevel, pos)` hasht de positie gezouten met de wereldseed en de dimensie (`salt(seed, dimension.location().hashCode())` = `HashCommon.mix(seed + dimHash)`, daarna `HashCommon.mix(pos.asLong() ^ salt)`), dus andere wereld of dimensie = ander ritme, deterministisch. Interval 80..200 ticks (`MIN_INTERVAL_TICKS`/`MAX_INTERVAL_TICKS`), offset per positie; `phase(gameTime)` (0 = uitbarsting), `ticksToWarning(phase)`, `step(gameTime)` → `Step(Action ERUPT|WARN|WAIT, delay)`: één scheduled tick per gebeurtenis, `WARNING_TICKS` (20) vóór de uitbarsting de waarschuwing (`BUBBLE_POP` + 4× `steam_puff` via `sendParticles`, geluid `block.boiling_mud.bubble` toonhoogte 0,7). Een tick op elk ander moment valt terug in het ritme: een late waarschuwing wordt verkort (uitbarsting op tijd), een tick tussen waarschuwing en uitbarsting wacht op de volgende waarschuwing. Geen blockstate of block entity. `startCycle(WorldGenLevel, pos)` (publiek) plant de eerste tick op de volgende waarschuwing (`max(1, …)`: precies op het waarschuwingsmoment 1 tick later, verkorte waarschuwing, geen extra cyclus); `onPlace` (alleen `ServerLevel`) en de random tick (vangnet) gebruiken hem. Uitbarsting: duw zoals 46 (`REACH` 4, `LAUNCH_SPEED` 0,9 ≈ 4,9 blokken, alleen gecentreerd, geen valschade), 24× `steam_puff` + `block.geyser.burst` (server-side, voor iedereen). `animateTick`: dun `steam_puff`-pluimpje (was `WHITE_SMOKE`). `INTERVAL_TICKS`, `interval(pos)` en `phase(pos, t)` zijn weg.
- `block/FumaroleBlock.java`: `animateTick` met `steam_puff` (2/3 kans per bezoek; was `CAMPFIRE_COSY_SMOKE`/`CLOUD`) en `block.fumarole.puff` (1/30, volume 0,3), zolang `GeyserBlock.ventOpen`; geluid via een `AmbientSoundLimiter` (10 ticks).
- `block/BoilingMudBlock.java`: bubbels `BUBBLE_POP` ongewijzigd; geluid `block.boiling_mud.bubble` (1/60, volume 0,4; was `BUBBLE_COLUMN_BUBBLE_POP`) via een `AmbientSoundLimiter` (5 ticks). Gedrag ongewijzigd (G2-B2 A).
- `block/AmbientSoundLimiter.java` (nieuw, package-private): één instantie per geluid, gedeeld door alle blokken van die soort; per client één geluid per venster (`minGapTicks`), maar een blok op minder dan de halve afstand (tot de dichtstbijzijnde speler, `getNearestPlayer`) van het blok dat speelde mag er nog tussendoor, zodat de vent aan je voeten niet wegvalt tegen verre; elke inhaal halveert de afstand, dus een venster bevat hooguit een paar geluiden. Een terugspringende speltijd (andere wereld) geeft hem vrij. Geen client-classes.
- Tests: `GeyserBlockTest` (ritme: bereik/spreiding, seed en dimensie veranderen het ritme, cyclus waarschuwing→uitbarsting, verse start waarschuwt bij de volgende waarschuwing (ook precies op het moment zelf), late ticks, eerste tick op elk moment van de cyclus valt in het ritme, buren barsten zelden samen uit over 2000 ticks); `AmbientSoundLimiterTest` (nieuw, incl. voorrang voor dichterbij).
- **Valkuil voor 48 (`steam_fens`) en andere features die geisers plaatsen:** roep `ModBlocks.GEYSER.get().startCycle(level, pos)` aan (met de `WorldGenLevel` van de feature), niet `scheduleTick(…, INTERVAL_TICKS)` (bestaat niet meer). In 1.21.1 bewaart `ProtoChunkTicks` alleen de tick, niet de delay: hij vuurt zodra de chunk geladen is, op een willekeurig moment van het ritme; `step` vangt dat op (eens per interval is dat moment de uitbarsting zelf, dan zonder waarschuwing). Zonder de aanroep start de geiser bij zijn eerste random tick.
- Valkuil: geluidsvolume/-toonhoogte in de code staan rond 1, de klank zelf regelt A in `sounds.json`.

## Beslissingen
- 2026-10-08 · blocks · Geiser-ritme als functie van positie en speltijd (interval 80–200 en offset uit een hash van de positie), één scheduled tick per gebeurtenis; een tick op een ander moment valt terug in het ritme · implementatiekeuze bij G2-B1 A (geen blockstate/block entity nodig; robuust bij het ontladen van chunks en bij worldgen-ticks zonder delay).
- 2026-10-09 · blocks · De hash van het geiser-ritme is gezouten met de wereldseed en de dimensie · review 46-rest (anders zelfde timing op dezelfde coördinaten in elke wereld en dimensie).
- 2026-10-08 · blocks · Waarschuwing van de geiser speelt `block.boiling_mud.bubble` (toonhoogte 0,7) met bubbels en een kleine `steam_puff`-wolk, server-side · implementatiekeuze (A heeft geen eigen waarschuwingsgeluid; bekend compromis, zie backlog).
- 2026-10-08 · blocks · Geluiden uit `animateTick` (fumarole, modder) begrensd per client: één fumarole-puf per 10 ticks en één modderbubbel per 5 ticks, hoeveel blokken er ook zijn · implementatiekeuze (plan 46-rest §1.1: velden mogen niet dreunen).
- 2026-10-09 · blocks · Binnen dat venster mag een blok op minder dan de halve afstand tot de speler (van het blok dat speelde) er nog tussendoor · review 46-rest (anders bleef een fumarole naast de speler vaak stil terwijl verre puffen speelden).
- 2026-10-08 · blocks · Fumarole, geiser-uitbarsting, -waarschuwing en -pluim gebruiken `steam_puff` (A) in plaats van `CAMPFIRE_COSY_SMOKE`/`CLOUD`/`WHITE_SMOKE`; geluiden `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble` (A) vervangen de vanilla-geluiden · plan 46-rest §1.1–1.2 (één soort stoom).

## Backlog
- [atmosphere] Eigen waarschuwingsgeluid voor de geiser (`block.geyser.warning`, borrelen/grommen); nu speelt de waarschuwing `block.boiling_mud.bubble` (ondertitel "Mud bubbles" bij een geiser). (bron: 46-rest)
- [worldgen] 48 (`steam_fens`): geisers starten via `ModBlocks.GEYSER.get().startCycle(level, pos)` in de feature. De verwijzingen naar `GeyserBlock.INTERVAL_TICKS` in `docs/backlog.md` r. 178 en `docs/plans/ring-3/golf-2-biomes.md` r. 122 vervallen en worden vervangen door `startCycle(level, pos)` (INT2). Zonder die aanroep start een worldgen-geiser bij zijn eerste random tick. Zie de valkuil in de CODEBASE-delta (worldgen-tick zonder delay). (bron: 46-rest)
