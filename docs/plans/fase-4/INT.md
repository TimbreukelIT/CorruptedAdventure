# INT — integratie fase 4

Stroom `INT` (door H1), n = 61 (vanaf ronde 3: n = 68, want 61 botst met L1 = 71), worktree `P:/Projecten/Java/wt/f4-int`, branch `f4/int`, vanaf main `d03463e4`.
Niet pushen, niet naar main mergen (dat doet main).

## Ronde 1 (2026-10-28)
Merges in deze volgorde:
1. `f4/hollow` 4dbf35c3 (H1: De Holle stap 1+2, gedeelde Nightmare-basis): schoon.
2. `f4/director` 84cdab88 (D: regisseur, met de 1024-fix 4eb1e1a5): CODEBASE (union; verouderde dubbele D-regel
   weg), gegenereerde lang (opnieuw via runData).
3. `f4/items` b41ddc44 (I: zwakte-items): constructor, ServerConfig, lang-providers, beslissingenlog (union).
4. `f4/r3` 0f6bff6e (R3: De Herrijzende): CODEBASE, backlog, constructor, client-entrypoint, lang-providers (union).

## Ronde 2 (2026-10-28)
5. `f4/r4` 21b47fab (R4: De Verloren Hoeder): gedeelde bestanden (union); een javadoc die de union splitste hersteld.
6. `f4/r5` 2d3e36d0 (R5: De Onvoltooide): gedeelde bestanden (union).
Daarna `PROTOCOL_VERSION` "11" → "12" (`keeper_mist`), imports gesorteerd, runData (5621d39f), build, GameTests 67/67.
Nog: `f4/r2` als laatste.

## Ronde 3 (2026-10-29): `f4/r2`
7. `f4/r2` 0f73883e (R2: De Gelaagde) in `f4/int` 70e40552 → merge a1fc70b8. Conflicten (union): mod-constructor
   (`LayeredGrip.register`), client-entrypoint (renderer), `ServerConfig` (`LAYERED`), `ModEntityTypes`, entity-tags,
   `NightmareLoot`, `NightmareText`, taalproviders (alleen volgorde), CODEBASE (R2-blok na De Holle; R2's oudere
   Holle-regel niet overgenomen), backlog; gegenereerde bestanden van `f4/int` gehouden en via runData opnieuw (9f480c61).
   Design-doc en beslissingenlog zonder conflict (R2 voegde daar alleen het model toe).
   `strengthFactor()`: geen voorkomens. Geen nieuwe payloads in R2 → `PROTOCOL_VERSION` blijft "12".

## Stand bij afronden (2026-10-28 22:00)
- `f4/int` op f21c6b89: H1, D, I, R3, R4, R5 gemerged; protocol "12"; runData 5621d39f; compileJava, build en GameTests
  67/67 groen; 2-clienttest gedaan (zie Voortgang 21:58). Geen eigen processen of slots meer.
- Niet gedaan: merge `f4/r2`, korte check daarna, `/code-review medium`.

## Volgende stappen
1. ~~R2 afmaken~~ (R2, 2026-10-29).
2. ~~`f4/r2` mergen~~ (ronde 3, 2026-10-29).
3. Korte check: server + client, `/summon corruptedadventure:the_layered` + een korte gevechtscheck.
4. `/code-review medium d03463e4..f4/int`, bevindingen fixen, build + GameTests.
5. Overdracht aan main (niet zelf pushen of naar main mergen).

## Taken
- [x] compileJava na elke merge (ronde 1: na items en r3)
- [x] runData (+ commit 09d1b047), build, GameTests 58/58 na ronde 1
- [x] `strengthFactor()` → `strength()` overal (ronde 1: geen voorkomens; ronde 2 opnieuw nagaan)
- [x] `docs/plans/fase-4/voorlopig-fase-4.md` (Voorlopig van alle stromen, R2 erbij: 69 punten)
- [x] CODEBASE, design-doc, beslissingenlog één geheel (ronde 3)
- [x] flaky `adispensershootsglitcharrows` → backlog `[items]`
- [x] server-smoke + 2 clients: per Nightmare `/summon` + korte check; De Holle echte komst via `/ca nightmare threat` + `arrive`
- [ ] `/code-review medium d03463e4..f4/int`

## Voortgang
- 2026-10-29 10:33 ronde 3 (`f4/r2`) klaar: compileJava, runData (9f480c61), build groen, elk in een eigen aanroep.
  GameTests 70/71: alleen `theDirectedArrivalOpensTheRiftAndTheHollowComesThrough` faalt ("Did not expect hollow_rift
  to exist"): bekend, L1 zoekt hem uit. Server-smoke (`server.sh start int-r2 --fresh`): start zonder fouten, GeckoLib
  4.9.3 laadt, `/summon corruptedadventure:the_layered` werkt (450 HP, valt en tickt), 0 ERROR/Exception in het log.
  Voorlopig van R2 in `voorlopig-fase-4.md` (punten 24–35, plus "worp zonder wand is hoog"); CODEBASE bijgewerkt.
- 21:58 clienttest (server + Dev + Dev2, vers wereld `int1`, `build/int-client-test.sh`, niet in git; sheets in
  `build/test/shots/int-report/`): De Holle via `/ca nightmare arrive 1 Dev` met twee survival-spelers: voortekens,
  scheur 9 blokken achter Dev (0,5 62 21,5), max-HP 450 (1,5×), status "here", gevecht. De Herrijzende 600 HP, vliegt,
  as-particles, vuurrode bossbar. De Hoeder 1024 (geplafonneerd, 1200 virtueel) in een meer: lokroep/grijpen trok Dev het
  water in tot de bodem, schade 2 per keer, misselijkheid (gif); staart niet apart gezien. De Onvoltooide 1024 (geplafonneerd),
  witte bossbar, lens-zicht werkt, beweegt naar de speler. Geen ERROR/Exception in server- of clientlogs.
- 21:50 ronde 2 (r4, r5) groen: compileJava, runData, build, GameTests 67/67; protocol 12. Clienttest gestart.
- 2026-10-28 21:42 ronde 1 groen: 4 merges, compileJava, runData, build, GameTests 58/58. CODEBASE fase 4 samengevoegd
  (gedeelde basis + regisseur, De Holle, I, R3), `voorlopig-fase-4.md` (35 punten, ronde 1).
