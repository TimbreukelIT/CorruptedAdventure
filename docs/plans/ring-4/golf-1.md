# Ring 4 — golf 1: vormen, blokken en systemen

Scope per pakket: één categorie (CLAUDE.md §2). Start: `main` na golf 0 (`768cb3b` + de review-fix van 59, 2026-10-13). Namen en sleutels: `contract.md` (vast sinds golf 0). Werkwijze: `waves.md` §2 (golf 1), §4 (worktrees, licht testen), §5 (eigenaarschap), §6 (INT1).

## Stromen en pakketten

| Stroom | Branch | Poort (n) | Pakketten (volgorde) | Categorie | Afhankelijk van |
|---|---|---|---|---|---|
| B | `ring4/blocks-g1` | 0 | **66** natuurblokken → **67** ertsen → **68** Nether-4-blokken (één commit per pakket) | `blocks` | golf 0 |
| W-terrein | `ring4/terrain-g1` | 1 | **60** basisterrein → **61** eilanden → **62** klif 3→4 → **63** `sundered_reach` | `worldgen` | golf 0 |
| W-biomes | `ring4/biomes-g1` | 2 | **64** biome-skelet (8 grond + 2 hoogte + `sundered_reach`), `transitions.ring_4` + `ring_4` uit `switch_at` | `worldgen` | golf 0 (stubs; lokale ruis of rebase op W-terrein, `waves.md` §3) |
| W-nether | `ring4/nether-g1` | 3 | **65** Nether-4-terrein + 3 biomes + ruimte-check; trede 3\|4 afgedekt, `fluid_levels.ring_4` = 10 | `worldgen` | golf 0 |
| C | `ring4/conditions-g1` | 4 | **69** gif (bronnen, drempels 30/60/90, melk/honing, gifregen-modifier) | `conditions` | golf 0 (tags/keys, `Ring4Band`) |
| A | `ring4/atmosphere-g1` | 5 | **70** dikke mist (+ hook fase 4), stormwind (+ duw boven Y ~120), gifregen | `atmosphere` | golf 0 (`Ring4Band`) |
| T | `ring4/textures-g1` | 6 (geen server) | **71a** eerste versies ring 4 + Nether-4 + particle-sprites, contactblad | `textures` | namen uit het contract |

- Elke stroom: eigen agent in een eigen git-worktree, eigen plan `docs/plans/ring-4/golf-1-<stroom>.md` met "CODEBASE-delta", "Beslissingen", "Backlog" onderaan; niet in `CODEBASE.md`, `docs/design/*`, `docs/backlog.md` (INT).
- Licht testen per pakket: B (build), D (`runData` + `git diff --exit-code src/generated` voor andermans output), unit tests. Geen runServer/determinisme/perf per pakket; respecteer `C:/Temp/ca-user-test.lock`.
- `/code-review` per stroom-pakket (main start hem), bevindingen opgelost of vastgelegd vóór de merge.
- Ontwerpvragen buiten het contract: stoppen en via main aan de gebruiker.

## Aandachtspunten per stroom
- **W-terrein:** 61 sluit `IslandField` aan op de final density en plaatst de vallen (water-tak, cap per chunk); review-notities 59 #5/#6 (één eilandopzoeking per kolom in de haak, straal ≤ `cell_size`). 62: #8 (val-kolomregel). 63: afgrondzee Y 0, alleen vastgelegde vallen. Prestatie indicatief (≤ 1,75×).
- **W-biomes:** de hoogte-biomes vervangen de placeholder-ids in `island_biomes`; `is_ring_4` incl. `sundered_reach`; `c:`-tags kiezen; surface voorlopig vanilla.
- **W-nether:** dam-regel trede 3\|4 testen met `tools/nether-lava-walls.py` op r 1.500 en r 2.000.
- **B:** `toxic_pool` en de gameplayblokken zijn golf 2 (72); `bubble_spring` en `giant_lily_pad` (multiblok 2×2) in 66 (besluiten hieronder).
- **C:** test met een eigen testdatapack (lege tags zijn geen fout); band via `Ring4Band` (server).
- **A:** band via `Ring4Band`; geen payload-wijziging; mist-hook-vorm kiezen (contract §6).
- **T:** paden = blok-/itemnamen uit het contract; B maakt tijdelijke, niet-gecommitte kopieën als een PNG ontbreekt.

## Integratie 1 (snel protocol, `waves.md` §6)
Merge B → T → W-terrein → W-biomes → W-nether → C → A; B/D + vangnet `src/generated`; S-smoke (server + 1 client, verse wereld `ring4-wave1`); W-smoke (`/ca tpring 4`, `tpring 4 nether`, `tpboundary 3|4`, portal ring 4 ↔ Nether-4); offline: determinisme omgekeerd geladen, feature-index ring 1–3, lavascan (ring 4 geen lava) + waterscan (water tegen lucht buiten de vastgelegde vallen); review; naar main. Ook in INT1: de ongestagede `sounds.json` (zachtere loops, `atmosphere`) committen. Daarna de echte ring 4-blokken in de surface (W-biomes, lettercode volgt), prestatie, **teststop 5a**.

**Teststop 5-pre** (niet blokkerend): terreinpreview na 61 (`/ca terrainmap` + korte vliegtocht in een verse wereld), vanuit de terrein-worktree.

## Besluiten (sparring golf 1, 2026-10-13)
1. Alle contractnamen met "(voorlopig)" staan vast zoals ze in het contract staan (ook sound events, particles, config-sleutels).
2. `giant_lily_pad`: multiblok 2×2 (vier delen met een `part`-property, breken van één breekt alle; worldgen plaatst het geheel) — 66.
3. `bubble_spring` in 66: alleen `animateTick` (vanilla bubble-particle tot A zijn sprites levert); geluid koppelt A in golf 2.
4. Alle zeven stromen starten tegelijk.
5. Teststop 5-pre na 61 (basisterrein + eilanden), vanuit de terrein-worktree.

## Voortgang
- 2026-10-13: plan opgesteld; besluiten van de gebruiker vastgelegd. Start na de laatste review-fix van 59.
