# INT2 — integratie fase 4 stap 2 (eigen komst + sporen ring 2–5)

Stroom `INT2`, n = 86, worktree `P:/Projecten/Java/wt/f4-int2`, branch `f4/int2`, vanaf main `c5b878e4` (`<basis>`).
Niet pushen, niet naar main mergen (dat doet main).

## Merges
1. `f4/arrival-layered` a032c1f8 (A2: De Gelaagde) → cb10f622: schoon.
2. `f4/arrival-risen` 36b91bd2 (A3: De Herrijzende) → fbf6610e: beslissingenlog en mod-constructor (union: de
   `arrival(...)`-regel naast die van De Gelaagde).

## Taken
- [x] compileJava, runData (geen diff), build, GameTests na A2+A3
- [ ] A4, A5 mergen (na melding van main); `PROTOCOL_VERSION` "13" → "14" (`unfinished_standstill`)
- [ ] `voorlopig-fase-4.md` (A2/A3: 107–115; A4/A5 volgen), CODEBASE één geheel, backlog
- [ ] `/code-review medium c5b878e4..f4/int2`, per Nightmare
- [ ] 2-clienttest: per Nightmare een echte komst via `/ca nightmare arrive <ring> Dev`

## Voortgang
- 2026-10-29 18:15 A2 + A3 gemerged: compileJava, runData (geen diff), build groen, GameTests 123/123, elk apart.
