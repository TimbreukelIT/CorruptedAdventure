# D — gedeeld Nightmare-systeem (stap 2+3: dreiging, regisseur, aftocht, terugkeer)

Stroom `D`, n = 67, worktree `P:/Projecten/Java/wt/f4-director`, branch `f4/director`.
Basis (`git merge-base main HEAD`): `d03463e4` (branch afgesplitst van f4/hollow `418460c4` + main `d03463e4`).
Bron van waarheid: `docs/design/fase-4-nightmares-concept.md`, "Fase 4-sparring alle Nightmares" en "Stap 2+3: gedeeld systeem".
Categorie `mobs`; attachment-registratie apart als `infra:`.

## Wat
1. `NightmareKind` (ring 1–5 → registry-naam + `NightmareArrival`); ontbrekende soort = overgeslagen.
2. Interface `NightmareArrival` + `DefaultNightmareArrival` (+ `NightmareSpots.behind`).
3. Dreiging per speler per ring (attachment `nightmare_threat`).
4. Regisseur (`SavedData` `corruptedadventure_nightmares`): status per soort, verslagen-teller, sterkte, virtuele positie.
5. Levenscyclus: aftocht → virtueel, dood → verslagen + terugkeertimer; herstart, uitloggen, chunk-unload.
6. `/ca nightmare status|threat|arrive|reset`, GameTests, unit tests, lang.

## Voortgang
- 2026-10-28 20:03 start; `setup.sh D 67`. User-lock aanwezig: geen JVM, code zonder compiler.
- 20:25 interface `NightmareArrival`, `NightmareKind`, `DefaultNightmareArrival`, `NightmareSpots`, config
  `[nightmares.director]` geschreven (niet gecompileerd).

## Voorlopig (bevestigen bij de teststop)
- Commando's onder de bestaande root **`/ca nightmare …`** (niet `/corruptedadventure …`): alle debugcommando's van de mod
  hangen aan `/ca`.
- Soorten zijn een vaste enum met registry-namen (`the_hollow`, `the_layered`, `the_risen`, `the_lost_keeper`,
  `the_unfinished`); een stroom meldt zijn komst aan met één regel `NightmareKind.X.arrival(new XArrival());` in de
  mod-constructor. Zonder die regel komt hij met de standaardkomst.
- Standaardkomst: 30 s voortekens (laag grotgeluid elke 3 s, as rond het doelwit, de laatste 5 s een hartslag), daarna
  verschijnt hij 10 blokken achter het doelwit uit rook (warden-emerge, lage pitch).
- Dreiging: drempel 100; per speler en ring wordt bij elke start vanaf 0 een tempo getrokken (2–3 ingame-dagen tot de
  drempel). Max. 2× de drempel. Zakt niet buiten de ring. Stijgt alleen in survival/adventure, levend, in de overworld van
  een ringwereld; de void-rand telt als ring 5. Blijft bij de dood behouden.
- Na een bezoek (hij verschijnt) gaat de dreiging van die ring naar 0 voor het doelwit én elke vechter binnen zijn
  `fightRadius`.
- Doelwit moet tijdens de voortekens in de ring blijven, online en levend zijn; anders stoppen de voortekens
  (`cancelled`), zwerft hij verder en blijft de dreiging staan (dus hij probeert het snel opnieuw).
- Geen plek om te verschijnen: elke seconde opnieuw, max. 20 s, dan verder zwerven.
- Zwerven: elke 60 s een stap van 32 blokken; koers draait willekeurig ±45°; buiten zijn ring probeert hij andere
  richtingen, anders blijft hij staan. Start op een willekeurige hoek in het midden van zijn ringband.
- Een aanwezige Nightmare die 15 s niet gevonden wordt of niet tickt (chunk ontladen, herstart zonder speler) gaat terug
  naar virtueel (geheeld, niet verslagen); duikt de oude entity later op, dan verdwijnt hij zelf (check-in bij de regisseur).
- Sterkte = 1 + 0,25 × min(verslagen, 3) op max-HP (ook met de spelerschaal) en `attack_damage`. Eigen aanvallen met
  config-schade vermenigvuldigen zelf met `Nightmare.strength()`.
- `/kill` op een geregisseerde Nightmare telt als verslagen; gebruik `/ca nightmare reset <ring>` om te wissen.
- Tijd = `gameTime` van de overworld (loopt niet mee met `/time set`).

## Beslissingen
- (nog geen nieuwe)

## Backlog
- (nog niets)
