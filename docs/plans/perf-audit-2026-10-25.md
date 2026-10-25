# Prestatiemeting 2026-10-25 (onderbroken) en plan voor 2026-10-26

Gemeten op main 9d49e232, fase 2 golf 1. Er is 1 warme pass gedraaid met `perf.sh` op 31 gebieden; 20 daarvan zijn
gemeten. De run is gestopt omdat ring 5 bleef hangen. Worktree: `P:/Projecten/Java/wt/perf-audit`, met de JFR-dumps in
`build/test/perf-all-20261025/`.

## Uitkomst: noise-stap mod/vanilla (budget ≤ 1,75×)
| Gebied | Noise | Gebied | Noise |
|---|---|---|---|
| ring1 | 0,94 | sulfur | 1,08 |
| z12 | 1,46 | great | 1,11 |
| canyon | 1,43 | ashen | 0,99 |
| hoodoo | 1,04 | titan | 1,12 |
| dunes | 1,22 | seabed | 1,22 |
| mesa | 1,08 | steamfens | 1,30 |
| zone23 | 1,50 | vfbasin | 1,12 |
| ember | 1,00 | vgorge | 0,92 |
| tds | 1,04 | tunmyth | 0,90 |
| tree | 1,05 | **zone45** | **692×** (15,8 s/chunk tegen 23 ms) |

- Ring 1–4 en de zones 1→2 en 2→3 blijven binnen het budget, ook na de G2-opknapbeurt van ring 2 en 3.
- De surface-stap ligt op 1,5–5,2× (canyon 5,2, mesa 5,0, zone23 4,4). Daar geldt geen budget, maar het is wel een
  kandidaat voor optimalisatie.
- **Niet gemeten:** wastes, glitch, meadow en alle Nether-gebieden (n1–n4, halls, cathedral).

## Het ring 5-probleem: hangt af van de wereldgeschiedenis
- Zone45 **los** op een verse wereld (HEAD, 16×16, 1 warm) geeft noise **1,43×**, dus normaal. Een los stuk van 6×6
  chunks duurt 9,4 s.
- De 692× ontstaat pas **nadat dezelfde server eerst 19 andere gebieden** (ring 1–4) heeft gegenereerd. Daarna zijn
  vrijwel alle chunks van zone45 én wastes traag (50–78 s per chunk). De server liep vast in de forceload en moest met
  taskkill gestopt worden.
- Tijdens de hang zaten alle 23 `wgen_fill_noise`-threads in `Ring5Field.terrain`/`column` (de gelaagde tak van de
  glitch-velden), via `Ring5Output.compute` ← `RingBlend.fillArray` ← `ZoneFracture.fillArray`. Die jstack-dumps zijn
  gemaakt zonder DebugNonSafepoints, dus het beeld is grof.
- Uitgesloten: 373787a7 (`Column.pit` in fillInner) is op zich niet de oorzaak, en de vensterverschuiving in
  ColumnTable ook niet (een laagverschuiving van ≤ 6 valt binnen de marge van 8).
- Hypotheses:
  - Een cache of lijst die per wereld onbegrensd groeit, waardoor heap en GC druk krijgen. Kandidaten van deze dagen:
    VolcanicLandmarks (ring 3), Ring2RockLandmarks en Ring2FlatLandmarks PLANS, Ring5PlanCache en Ring5Memories,
    KeyedSlots-contentie.
  - Een wisselwerking tussen ring 4 en ring 5 (gedeelde state in RingBlend/ZoneFracture).
- **Het risico is groot:** elke wereld die lang gespeeld wordt, kan in ring 5 vastlopen.

## Plan voor 2026-10-26: eerst optimalisatie (op verzoek van de gebruiker)
1. **Reproduceren met profiler:** jfr-prof met DebugNonSafepoints op een server die eerst de 19 ring 1–4-gebieden
   genereert en daarna zone45 (6×6). Bekijk jdk.GarbageCollection/GCHeapSummary en `jcmd GC.class_histogram` na een
   paar gebieden.
2. **Repareren:** de oorzaak begrenzen of oplossen, bijvoorbeeld met een LRU zoals `Landmarks.WINNERS`. Daarna een
   test die een lange generatiereeks nabootst (geen groei van de heap, geen vertraging).
3. **Volledige meting opnieuw:** alle 31 gebieden plus de Nether, 2 warm + 1 koud, op een rustige machine.
4. **Optimalisatieronde over alles:**
   - de surface-stap van ring 2 en 3 (3–5×);
   - de duurste noise-gebieden (zone23 1,50, z12 1,46, canyon 1,43);
   - de features;
   - een check op caches die per wereld onbegrensd groeien.
5. Daarna gaat fase 2 verder met golf 2 (ring 2), in een nieuwe sessie met Blockbench.
