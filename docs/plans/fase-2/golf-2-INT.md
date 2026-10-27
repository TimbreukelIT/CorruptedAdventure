# Fase 2 golf 2 — INT (integratie)

Branch `f2g2/int`, worktree `P:/Projecten/Java/wt/f2g2-int`, n = 65. Categorie `infra` (docs + tests).
Basis: main `3a7c7c7a` (M, V, T), daarna main `4563d144` (S) ingevoegd.

## Wat
- **Fase A (docs):** delta's van M, V, T in `CODEBASE.md`, `docs/backlog.md` en `docs/design/fase-2-items.md` (§4.3-tabel,
  §5.8 "Voorlopig"); §5.3 en §5.6 rechtgezet (besluit main).
- **Fase B:** merge main (S), delta S ingevouwen, tests, checklist `teststop-golf-1-4.md` (deel "Golf 1 + 2"; de
  gebruiker test pas na golf 4).

## Voortgang
- 2026-10-27 ~18:30 fase A: commit `d9007f46`; §5.3/§5.6: `2d855465`.
- 2026-10-27 19:30 main (S) ingevoegd zonder conflicten; de PNG's van T zijn gebleven (`sunstone_*` ongewijzigd sinds `999295f5`).
- 2026-10-27 19:32–19:42 runData, build, GameTests groen; 19:43–19:48 dedicated server + 2 clients.

## Resultaten
| Check | Resultaat |
|---|---|
| `runData` → `git diff --exit-code src/generated` | leeg |
| `build` (unit tests) | groen: 1496 tests, 0 fouten, 1 overgeslagen |
| `runGameTestServer` | 8/8 groen (`TierGatingGameTests` 1, `ConsumableGameTests` 4, `Golf2IntegrationGameTests` 3) |
| dedicated server (n = 65, nieuwe wereld) | start in 9,3 s; geen client-class-fouten (`ClassNotFound`/`NoClassDefFound`/dist: 0), geen ERROR in `latest.log` |
| 2 clients (Dev, Dev2) | beide gejoind; `client.sh shot` van beide |
| RCON op de server | `/give` van `canteen[sips=4]`, `sunstone_pickaxe`, `salted_jerky`, `cactus_juice` werkt (component bewaard in de inventaris); `/effect give Dev2 corruptedadventure:cooled 45` → 900 ticks, `show_icon`, cures melk/totem; `/recipe give Dev *` 1765 recepten |
| Verkoeld per speler (ring 2, zon) | Dev2 (verkoeld) `dehydration +0,20/s`, Dev (niet) `+0,40/s`; icoon alleen bij client 2 rechtsboven |

**`Golf2IntegrationGameTests`** (`gametest/`), door de echte `RecipeManager` van de server met een gevuld rooster:
- recepten (id, item, aantal): zonnesteen-template (`SDS/SCS/SDS`) → 1, uitdrogings-template uit barrel cactus → 1,
  kopie-recept → 2, veldfles → 1, cactussap → 1, jerky (rundvlees, zalm) → 2, zielkristalblok → 1, ingelegd zandsteen → 8,
  trap → 4, plaat → 6, muur → 6, zoutlamp → 1; steenzaag trap 1, plaat 2, muur 1;
- smithing ×9: diamanten stuk + template + `sunstone_ingot` → `sunstone_<stuk>` via `sunstone_<stuk>_smithing`,
  Unbreaking III blijft; een netherite-zwaard is geen basis;
- mijnbaarheid (`isCorrectToolForDrops`): ring 2-ertsen ijzer nee / diamant ja / zonnesteen ja; ring 3-ertsen diamant en
  netherite nee / zonnesteen ja; ring 4-ertsen zonnesteen nee; golf 2-blokken met een houten pickaxe ja; zoutlamp licht 12.

**Veldfles vullen en drinken** draait server-side in `ConsumableGameTests` (GameTest-server: cauldron zakt, dauwbron en
waterbron blijven, slok −25 + 900 ticks `cooled`, cactussap −15 verkort de veldfles-tijd niet). Met de echte clients kan
de tooling geen rechtsklik geven (`client.sh key` kent alleen F1–F3/ESC/T/ENTER): drinken met de muis staat in de
checklist voor de gebruiker.

## Gevonden
- Geen fouten in andermans code.
- `fase-2-items.md` §2 noemt nog "Teststop na golf 1 + 2"; de gebruiker test nu na golf 4 (besluit via main) — main legt
  dat vast in §2 en de beslissingenlog.

## CODEBASE-delta
(direct ingevouwen) `gametest/Golf2IntegrationGameTests`; sectie "Fase 2 golf 2 (per stroom)" met S, M, V, T.

## Beslissingen
_(geen)_

## Backlog
_(geen nieuwe)_
