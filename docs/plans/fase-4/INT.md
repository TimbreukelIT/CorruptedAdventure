# INT — integratie fase 4

Stroom `INT` (door H1), n = 61, worktree `P:/Projecten/Java/wt/f4-int`, branch `f4/int`, vanaf main `d03463e4`.
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

## Taken
- [x] compileJava na elke merge (ronde 1: na items en r3)
- [x] runData (+ commit 09d1b047), build, GameTests 58/58 na ronde 1
- [x] `strengthFactor()` → `strength()` overal (ronde 1: geen voorkomens; ronde 2 opnieuw nagaan)
- [ ] `docs/plans/fase-4/voorlopig-fase-4.md` (Voorlopig van alle stromen)
- [ ] CODEBASE, design-doc, beslissingenlog één geheel
- [x] flaky `adispensershootsglitcharrows` → backlog `[items]`
- [x] server-smoke + 2 clients: per Nightmare `/summon` + korte check; De Holle echte komst via `/ca nightmare threat` + `arrive`
- [ ] `/code-review medium d03463e4..f4/int`

## Voortgang
- 21:58 clienttest (server + Dev + Dev2, vers wereld `int1`, `build/int-client-test.sh`, niet in git; sheets in
  `build/test/shots/int-report/`): De Holle via `/ca nightmare arrive 1 Dev` met twee survival-spelers: voortekens,
  scheur 9 blokken achter Dev (0,5 62 21,5), max-HP 450 (1,5×), status "here", gevecht. De Herrijzende 600 HP, vliegt,
  as-particles, vuurrode bossbar. De Hoeder 1024 (geplafonneerd, 1200 virtueel) in een meer: lokroep/grijpen trok Dev het
  water in tot de bodem, schade 2 per keer, misselijkheid (gif); staart niet apart gezien. De Onvoltooide 1024 (geplafonneerd),
  witte bossbar, lens-zicht werkt, beweegt naar de speler. Geen ERROR/Exception in server- of clientlogs.
- 21:50 ronde 2 (r4, r5) groen: compileJava, runData, build, GameTests 67/67; protocol 12. Clienttest gestart.
- 2026-10-28 21:42 ronde 1 groen: 4 merges, compileJava, runData, build, GameTests 58/58. CODEBASE fase 4 samengevoegd
  (gedeelde basis + regisseur, De Holle, I, R3), `voorlopig-fase-4.md` (35 punten, ronde 1).
