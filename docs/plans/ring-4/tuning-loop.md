# Afstemlus (tuning loop) voor ring 4

Sinds 2026-10-19 (sparring aankleding). Geldt voor afstemrondes: aankleding, dichtheden en featureparameters. Nieuw
terrein, nieuwe systemen en formele teststops volgen nog steeds het golf- en INT-protocol (`waves.md`).

## Waarom

Een volledige ronde (stromen, review per stroom, INT met determinisme, waterscan en perf, docs vóór het spelen) kostte
ongeveer 4 uur. De grootste posten waren determinisme ~27 min, waterscan ~27 min, perf 45–65 min en de reviews. Voor
afstemwerk is dat te zwaar. Het doel is 20–40 min per ronde.

## De lus (één agent, eigen worktree)

1. Wijzigen. Featureparameters zijn datagen: `./gradlew runData` duurt ~23 s, er is geen volledige build nodig. Logica
   in Java vraagt wel een build.
2. Een kleine wereld maken: `world.sh <level> <areas> --only "<1–2 gebieden>"`. `areas/smoke.txt` duurt ~35 s. Voeg
   zo nodig een klein gebiedsbestand per biome toe.
3. Kijken:
   - `client.sh shot` + `py/sheet.py` voor contact sheets, met dezelfde camera-posities vóór en na;
   - `/ca terrainmap` voor terreinvorm zonder chunks.
4. Herhalen tot het redelijk is. Daarna vliegt de gebruiker op een live server voor de fijnafstemming.

## Checks per ronde

- `./gradlew build` (unit-tests + ClientIsolationTest).
- `./gradlew runData` + `git diff --exit-code src/generated` na de commit.
- Server-smoke: een verse wereld (seed 20260930), een client joint, `/ca tpring 4`, de log heeft 0 ERROR en geen
  client-class.
- Alleen als een change bomen of plaatsing raakt: een snelle determinismecheck op 1–2 gebieden met
  `world.sh --freeze` + `detcmp.py` (wereld b omgekeerd geladen).
- Eén `/code-review low` over de branch-diff, vóór de merge.

## Alleen vóór een formele teststop

- Volledige determinisme (`det.sh` op de teststopgebieden).
- Perf (`perf.sh`). Tussendoor mag een indicatie: `perf.sh --warm 1 --cold 0 --max-min 5`.
- Waterscan en gifscan (`waterscan.sh`, met `--settle` alleen hier).
- Feature-index en de teststop-checklist.

## Wat blijft

- De lock- en slotregels uit `tools/test/README.md`, inclusief `--no-daemon`: een daemon zou buiten de slottelling
  blijven hangen.
- CLAUDE.md §8 (build, runData, runServer bij gameplay) en de docs (CODEBASE.md, design-doc, beslissingenlog) blijven
  verplicht. De docs mogen na het spelen worden bijgewerkt, zolang het vóór de afsluitende commit van de ronde is.
