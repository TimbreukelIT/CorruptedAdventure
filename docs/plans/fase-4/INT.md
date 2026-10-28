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

## Ronde 2 (later)
`f4/r2`, `f4/r4`, `f4/r5`; daarna `PROTOCOL_VERSION` "11" → "12" (`keeper_mist`).

## Taken
- [ ] compileJava na elke merge (ronde 1: na items en r3)
- [ ] runData (+ commit), build, GameTests na ronde 1
- [ ] `strengthFactor()` → `strength()` overal
- [ ] `docs/plans/fase-4/voorlopig-fase-4.md` (Voorlopig van alle stromen)
- [ ] CODEBASE, design-doc, beslissingenlog één geheel
- [ ] flaky `adispensershootsglitcharrows` → backlog `[items]`
- [ ] server-smoke + 2 clients: per Nightmare `/summon` + korte check; De Holle echte komst via `/ca nightmare threat` + `arrive`
- [ ] `/code-review medium d03463e4..f4/int`

## Voortgang
