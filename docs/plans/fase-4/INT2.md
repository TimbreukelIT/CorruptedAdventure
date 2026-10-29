# INT2 — integratie fase 4 stap 2 (eigen komst + sporen ring 2–5)

Stroom `INT2`, n = 86, worktree `P:/Projecten/Java/wt/f4-int2`, branch `f4/int2`, vanaf main `c5b878e4` (`<basis>`).
Niet pushen, niet naar main mergen (dat doet main).

## Merges
1. `f4/arrival-layered` a032c1f8 (A2: De Gelaagde) → cb10f622: schoon.
2. `f4/arrival-risen` 36b91bd2 (A3: De Herrijzende) → fbf6610e: beslissingenlog en mod-constructor (union: de
   `arrival(...)`-regel naast die van De Gelaagde).
3. Nog niet: `f4/arrival-keeper` af127093 (A4) en `f4/arrival-unfinished` efb01309 (A5); beide door main als klaar gemeld.

## Stand bij afronden (2026-10-29 18:45, sessie gesloten door de gebruiker)
- `f4/int2` = A2 + A3 + review-fixes De Herrijzende (b325dfc8) + docs. compileJava, runData (geen diff), build en
  GameTests 123/123 groen (na de fixes opnieuw 123/123). Geen JVM's, geen slots; `run/config` teruggezet.
- Gedaan in de docs: `voorlopig-fase-4.md` punten 107–116 (A2, A3, INT2-review), CODEBASE-koppen De Gelaagde (A2) en
  basis (INT2), backlog: 3 niet-opgeloste A3-reviewpunten.

## Open — volgende stappen (in deze volgorde)
1. **A4 en A5 mergen** (`git merge --no-ff f4/arrival-keeper`, `f4/arrival-unfinished`; gedeelde bestanden union).
   - `PROTOCOL_VERSION` "13" → "14" in `network/ModNetwork` + CODEBASE (`unfinished_standstill`, A5). A4: alleen de
     constante `KeeperMistPayload.ARRIVAL_BANK`, geen nieuwe payload. Nieuw particle type `still_raindrop` (A5).
   - A4 raakt `LevelRendererSkyMixin` (zon gedimd in de komst-mist): controleren dat dit niet botst met de dimming van
     ring 5.
   - Daarna compileJava, runData (+ commit), build, GameTests, elk apart. **GameTests vóór de 2-clienttest**:
     `runGameTestServer` gebruikt `level-name` uit `run/server.properties` en overschrijft anders de testwereld
     (daarna "not a ring world"); of de server erna met `--fresh` en een nieuwe levelnaam starten.
2. **Flaky tests oplossen (opdracht main, `mobs:`-commit, 3× achter elkaar groen):**
   `LostKeeperGameTests.aHitOnTheCrownBreaksOneBranchAndHurtsMoreThanOnTheBody` ("the body hit lands", bij A5 3 van de
   6 runs rood) en `NightmareReactionGameTests.aRestWindowOutlastsAShortGapWithoutATarget`. Vermoeden: interactie met
   gelijktijdige tests die grote Nightmares spawnen (komst De Onvoltooide ~25 s) of timing; eigen batch, meer afstand of
   minder timing-afhankelijk. Nog niet onderzocht.
3. **Review De Gelaagde (A2, 10 bevindingen via main) — nog niets opgelost.** Oplossen (`mobs:`, GameTests waar het
   kan): 1 `TheLayered.updateSurface` slaat tijdens het ontvouwen `touchesWall` over (wand weg → tot 10 s in de lucht);
   2 krassen-check "niet binnen 12 blok van een speler" op de roam-origin i.p.v. de echte plek; 3 `place()` valt bij een
   mislukte `behind()` terug op de positie van het doelwit (opbarsten onder de speler) → ring ≥ 4 blok, anders wand of
   standaardkomst; 4 `appear()` kan via een nieuwe random `behind()` elders opbarsten dan de voortekens → exacte Vec3
   bewaren; 5 unfold-animatie (50 ticks) los van `arrivalUnfoldTicks` (10–200) → schalen of config ≥ 50; 8
   `rememberedPlace`/`rememberedWall` niet gewist als `appear()` null geeft → altijd `forget()`, per plek bijhouden.
   Opruimen of backlog: 6 `fleeAnimals` 3× (Holle, Herrijzende, Gelaagde); 7 `count()` scant alle entities (ook
   `RisenTrace`); 9/10 dubbele wallTurn- en yaw-code.
4. **Review De Herrijzende (A3): klaar.** Opgelost in b325dfc8: test-lookup per speler (batch-interferentie), gedeelde
   arrival-instantie in de test, `riseSpot` alleen overdekte vloeren (anders door de lucht, voorlopig 116) en geen
   dubbele afstanden, `passOverhead` ook bij een start recht boven het doelwit, geen schaduw op grond boven hem. Backlog:
   te weinig kopruimte in `floorNear`, poel-plan hergebruiken + `yawToward`-dubbel, geforceerde chunks bij een falende
   test. Bestaande backlog (A3): vliegen door het grotdak na het oprijzen.
5. **Review A4 en A5**: na de merge `/code-review medium c5b878e4..f4/int2` beperkt tot `entity/lostkeeper`,
   `client/entity/lostkeeper`, de mixin, en `entity/unfinished`, `client/.../unfinished`, `network/`, particle.
6. **Voorlopig** van `A4.md` en `A5.md` achteraan in `voorlopig-fase-4.md` (vanaf 117; wat al in het design-doc staat
   weglaten); CODEBASE-koppen De Hoeder/De Onvoltooide met A4/A5; backlog-regels van A4/A5 nalopen.
7. **2-clienttest deel 2** (3 slots toegestaan door main): `build/int2-client-test.sh keeper unfinished` (niet in git;
   eerst `bash -n`; `run/config` tijdelijk `roamIntervalSeconds` 5, `roamStepBlocks` 12, alle `traceChance` 1, daarna
   terugzetten met `build/int2-server.toml.bak`). Ring 4 eerst `/ca weather 4 clear`; ring 5 `void 0` (het script doet
   `calm`). Eventueel De Gelaagde opnieuw om natuurlijke klauwkrassen te tellen (deel 1 wiste ze vóór de telling).
8. Commit, dit plan bijwerken, opruimen (geen JVM's, geen slots); eindrapport aan main.

## Taken
- [x] compileJava, runData (geen diff), build, GameTests na A2+A3
- [ ] A4, A5 mergen; `PROTOCOL_VERSION` "13" → "14"
- [ ] flaky tests oplossen (3× groen)
- [ ] review A2 (10 punten) afhandelen; [x] review A3; [ ] review A4/A5
- [ ] `voorlopig-fase-4.md` A4/A5 (A2/A3/INT2: 107–116 gedaan), CODEBASE één geheel, backlog
- [ ] 2-clienttest: [x] De Gelaagde, [x] De Herrijzende, [ ] De Verloren Hoeder, [ ] De Onvoltooide

## Voortgang
- 2026-10-29 18:45 afgerond op verzoek van main (sessie dicht): zie "Stand bij afronden" en "Open".
- 2026-10-29 18:35 2-clienttest deel 1 (server + Dev + Dev2, verse wereld `int2`, `build/int2-client-test.sh`, niet in
  git; shots `build/test/shots/int2-report/`; `run/config` tijdelijk `roamIntervalSeconds` 5, `roamStepBlocks` 12,
  `traceChance` 1): De Gelaagde in ring 2 (`tpring 2`, zandstenen wand 8 blok west): stofgordijn op de wand, ontvouwd
  aan de wand, daarna gevecht (Dev weggeslingerd). De Herrijzende in ring 3 (open lucht): door de lucht, duik, gevecht;
  na reset 4 natuurlijke sporen (`risen_trace`, telling via RCON). Serverlog 0 ERROR/Exception; clientlogs alleen
  logrotatie. Review-fixes De Herrijzende b325dfc8, GameTests 123/123.
- 2026-10-29 18:15 A2 + A3 gemerged: compileJava, runData (geen diff), build groen, GameTests 123/123, elk apart.
