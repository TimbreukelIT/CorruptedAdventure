# Ring 4 — golf 3: textures (71), backlog-opruiming en teststop 5c

Scope per pakket: één categorie (CLAUDE.md §2). Start: branch `claude/admiring-cray-2ywybu` na teststop 5b akkoord
(2026-10-21). Besluiten: `02-beslissingenlog.md` (2026-10-21) en `fase-1-wereld.md` → Ring 4 "Golf 3 (2026-10-21)".
Backlog-inventaris: regelnummers verwijzen naar `docs/backlog.md` op `7f31864`.

**Omgeving:** Claude Code-cloudsessie (Linux, 4 cores, 15 GB, geen scherm). Mogelijk: `build`, `runData`, een headless
`runServer` met RCON, de texturescripts, offline regio-scans (Python). Niet mogelijk: `runClient`, screenshots,
vergelijkbare prestatiecijfers. Teststop 5c (in-game) doet de gebruiker lokaal.

## Stromen en pakketten

| Stroom | Branch (lokaal) | Pakketten | Categorie | Start |
|---|---|---|---|---|
| I | `ring4/infra-g3` | **I3** `Ring4Tunnels.WORLDS` wissen bij `ServerStoppedEvent` (r. 356); `Ring4IslandStatsTest` `@Tag("stats")` + uitsluiten in `build.gradle` (r. 430a); `tools/test/` ook op Linux (geen `powershell`/`cygpath`; lock-map standaard per OS) zodat INT de det-check in de cloud kan draaien | `infra` | direct |
| W | `ring4/worldgen-g3` | **74b** eilandpunten (r. 426–429) · **75c** determinisme-keten (r. 420, 367-rest, 415, 421) · daarna **77b** inzet nieuwe blokken (na B) | `worldgen` | direct; 77b na B |
| B | `ring4/blocks-g3` | **66c** `lagoon_sand` + `clawed_stone`/`clawed_deepslate` | `blocks` | direct |
| T | `ring4/textures-g3` | **71** gerichte polijstronde + textures van 66c | `textures` | na 66c (namen) |
| IT | `ring4/blocks-g3` (na 66c) | **66d** smeltrecept `lagoon_sand` → `glass` | `items` | na 66c |

W bouwt zijn pakketten na elkaar (één eigenaar van de worldgen-bestanden). B, T en IT lopen in één stroom na elkaar
(kleine pakketten, elk een eigen commit met eigen categorie).

## Pakketten

### I3 — infra
- `Ring4Tunnels.forgetWorlds()` aanroepen bij `ServerStoppedEvent` (zoals `RingLayout.onServerStopped` / `IslandFields`).
- `Ring4IslandStatsTest`: `@Tag("stats")`; `build.gradle` `test { useJUnitPlatform { excludeTags 'stats' } }`; een losse
  taak of `-PincludeStats` om hem toch te draaien.
- `tools/test/lib.sh` e.a.: OS-detectie; op Linux `kill -0`/`kill`/`pgrep -f` i.p.v. `powershell`, `cygpath` als
  no-op, `CA_LOCK_DIR` standaard `${TMPDIR:-/tmp}/ca-test`, `jfr` i.p.v. `jfr.exe`. Windows-gedrag ongewijzigd.
  Rooktest: `world.sh` met `areas/smoke.txt` + `det.sh` op één gebied in de cloud.

### 74b — eilandpunten (`worldgen`)
- r. 426: features op SURFACE/ON_GROUND (`isolated_tree`, `sky_tree`, `boulder`, `fallen_tree`, `ground_plant`) niet op
  een brok van `IslandDebris`: `IslandField.inDebris(x, z)` en de check in de ON_GROUND-plaatsing en `IsolatedTreeFeature`.
- r. 428: `IslandDebris` toetst de grondvrijte (8) op het maximum van `baseHeight` over hoeken en midden.
- r. 429: `IslandArches.archKept` sluit een boog uit die een hoger gerangschikte gehouden boog kruist (recursie alleen
  omhoog in rang: deterministisch).
- r. 427: hapering meten (`perf-islands.txt`, indicatief in de cloud); alleen bij een duidelijke piek het filter vóór
  `pairs()`.
- r. 430b: `Ring4IslandsTest` toetst de bodem tegen de hoogste grond onder de voetafdruk; r. 435: test dat er geen val
  boven een droog zinkgat landt.

### 75c — determinisme-keten (`worldgen`)
- r. 420: ertsblob in de eilandonderkant tegen `island_dressing`/`island_underside` (geen ertsen in eilandkolommen, of
  de aankleding ook op ertsblokken — de eenvoudigste die deterministisch is).
- r. 367-rest: riet, `island_vines`, reuzenleliebladen eerst trekken, dan plaatsen (zoals `ground_plant`).
- r. 415: reuzenmangrove `glowroot_swamp`: filter op terrein/bevroren heightmap i.p.v. de levende heightmap.
- r. 421: luchtblok onder zee in een `boulder` (6188 59 −68): oorzaak zoeken en dichten.

### 66c — nieuwe blokken (`blocks`)
- `lagoon_sand`: valt als zand (`ColoredFallingBlock`, stofkleur wit-crème), `#sand`, `#mineable/shovel`, eigen drop,
  `ring_4`-tags waar zand hoort (o.a. de plantgrond van `lagoon_glowgrass` als die zand bevat). Lang en/nl.
- `clawed_stone` en `clawed_deepslate`: zoals `clawed_tidestone` (hardheid van het gesteente; drop `cobblestone` /
  `cobbled_deepslate`, silk touch het blok), `#mineable/pickaxe`, deepslate-geluid voor de deepslate-variant.
- Models wijzen naar de paden van T (`block/lagoon_sand`, `block/clawed_stone`, `block/clawed_deepslate`); tot T levert
  staat er een placeholder van T's script (geen vanilla-pixels).

### 71 — textures (`textures`)
Gerichte polijstronde (max. twee rondes, contactblad vóór/na in `build/texture-preview/`, naar de gebruiker):
`withered_moonpetal_log`/`_wood` (rotter, donkerder, niet als grijze steen; r. 390), `blighted_glowcap` ziekelijk
groen/grijs (r. 392), bloesemkruin, tidestone in kliffen, gifpoel naast gifgroen water, fossielsteen op een muur
(r. 272), `bone_block`-fossielen naast `fossil_stone`/bottenzand (r. 343), `particle/deep_silhouette` vorm bij bijna
zwart (r. 341); nieuw: `lagoon_sand`, `clawed_stone`, `clawed_deepslate`.

### 66d — recept (`items`)
`lagoon_sand` smelten → `minecraft:glass` (oven), via datagen.

### 77b — inzet (`worldgen`, na 66c)
- `lagoon_sand` op de zandbanken en oevers van `crystal_lagoon` (i.p.v. vanilla zand in `CrystalLagoonFeatures` en de
  surface waar `crystal_lagoon` zand legt).
- `TunnelLiningFeature`: klauwsporen in de variant van het gesteente eromheen (`stone` → `clawed_stone`, `deepslate` →
  `clawed_deepslate`, tidestone → `clawed_tidestone`).

## Integratie (INT-G3)
Merge I → B (66c, 66d) → T → W in `claude/admiring-cray-2ywybu`; `build`, `runData` + `git diff --exit-code src/generated`;
headless server-smoke (verse wereld, `/ca tpring 4`, `tpring 4 nether`, `tpboundary 3|4`, 0 ERROR, geen client-class);
det-check (wereld b omgekeerd geladen) op de eiland- en tunnelgebieden; `/code-review`; docs (CODEBASE, backlog
afvinken volgens de inventaris hieronder). Daarna **teststop 5c** (`teststop-5c.md`) bij de gebruiker, lokaal.

## Backlog-uitkomst (2026-10-21)
- **Gebouwd in golf 3:** r. 356, 420, 421, 426–430, 367-rest, 415; textures r. 272, 341, 343, 390, 392; nieuwe blokken
  r. 344 (klauwkras), 411 (lagunezand).
- **Al opgelost, afvinken:** r. 255 (`sounds.json`), 363 (liften), 376 (uitrol aankleding), 435 (val boven droog zinkgat).
- **Zo laten (besluit 2026-10-21):** r. 269 (schuim-particle), 378 (riet-oevercheck), 433 (`toxic_pool` in veenkommen);
  r. 422 (`withered_moss`) blijft idee.
- **Naar een infra-opruimronde na ring 4:** r. 256, 260, 261, 274/345, 276, 280, 284, 286, 287, 323, 351, 360, 361, 386;
  prestatie-meten r. 289, 328, 329, 334, 336, 358 (bij 5c lokaal).
- **Latere fase:** fase 2 r. 29, 30, 38, 278, 279, 384, 412; fase 3 r. 31, 362; fase 4 r. 32, 418; fase 5 r. 250, 252;
  ring 5 r. 259, 262, 266, 396, 397.
- **Blijft staan (info / bij klachten / bekend gedrag):** r. 290, 305, 306, 309, 310, 314–317, 327, 330–333, 335, 340,
  350, 352, 357, 366, 368, 374 (Sodium), 375, 379, 391 (blikvangers → checklist 5c), 393, 398, 399, 402–405, 413, 414,
  423, 434.

## Voortgang
- 2026-10-21: plan en besluiten vastgelegd; build in de cloud groen (7m40s, alle tests).
