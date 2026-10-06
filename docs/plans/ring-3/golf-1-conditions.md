# Golf 1 — stroom C (condities): 43 hitte, 44 water verdampt

Scope: `conditions` — één commit per pakket; branch `ring3/conditions-g1`.

**Doel:** de overlevingskern van ring 3 (antwoorden 4 en 9):
- **43** een hitte-meter met een sterk dag/nacht-ritme, hittebronnen, pieken en schuilplekken;
- **44** geplaatst water verdampt bovengronds in ring 3.

Bron: `fase-1-wereld.md` Ring 3 "Conditie: hitte", "Water" + "Uitwerking sparring ring 3"; `contract.md` §5; haken in `CODEBASE.md` ("Meter-systeem: haken"); sjabloon `DehydrationRule`, `DehydrationExposure`, `WaterBottleDrink`, `HeatWaveDehydrationModifier`.

## 43 — Hitte
- `Condition.HEAT`: pure regeltabel naar het voorbeeld van `DehydrationRule.evaluate`.
  - **Op:** zon overdag onder open lucht +0,55/s; ×1,5 binnen `sourceRadius` (4) van een blok in `#heat_sources`; ×2 in `#heat_peaks`; de sterkste factor wint, ze stapelen niet (*voorstel; anders melden*).
  - **Af:** 's nachts −0,5; ondergronds/onder een dak −0,5; schaduw overdag −0,25 (dus nooit echt koel overdag); water −2,0; `#heat_refuges` −2,0; buiten ring 3 −0,5.
  - Een hittebron telt ook 's nachts en ondergronds (lava in een grot is heet).
- **Fire resistance:** factor 0,5 op een positieve rate (`RateModifier`).
- **Ringweer-modifiers** (`contract.md` §5): asregen ×1,5 en uitbarsting ×2 als `1 + (m − 1) × intensiteit` op een positieve rate, hard op de ringgrens (sjabloon `HeatWaveDehydrationModifier`). Factoren uit `[weather.ring3]` (A); tot A gemerged is, constanten met dezelfde startwaarden. De aswolk (×3) volgt in golf 2.
- **Volgorde van de modifiers** (backlog r. 95): weer → gear (fase 2) → fire resistance → zuiverend (Nether-4) als laatste. Vastleggen in `ConditionDefinitions` en in de delta.
- **Drempels:** 25 traagheid I, 50 zwakte I, 75 mining fatigue I, 100 in brand (eigen `ThresholdEffect` ignite: `setRemainingFireTicks`, elke seconde ververst, niet in water). Damage type `corruptedadventure:heat` voor het doodsbericht als dat van toepassing is (implementatiedetail).
- **Bronnen zoeken:** een box-scan van 4 blokken elke 20 ticks per speler is ~729 blokken. Goedkoop houden (vroeg stoppen, eerst de blokken direct rond de speler). Meten met 2 spelers.
- **Config** `[conditions.heat]` (`contract.md` §5); HUD-kleur in de enum; lang via `Ring3ConditionText`.
- Een lege `heat_sources`-tag in de worktree is normaal: test met een testdatapack of `/ca condition`.

## 44 — Water verdampt
- In ring 3 (`ringAt(...).ring() == RING_3`, overworld) verdampt geplaatst water direct waar de hemel zichtbaar is (`canSeeSky` op de plaatsing), met vanilla's verdampingseffect (rook + sis, zoals in de Nether).
- **Routes:** emmer (speler), dispenser, gebroken ijs dat water wordt, smeltend ijs (random tick), en een waterfles op modder (vanilla-gedrag) indien relevant.
- Ondergronds en onder een dak werkt water normaal. Buiten ring 3 is er niets anders.
- **Implementatie:** NeoForge-events of een mixin per route (de agent kiest). Elke route krijgt een test; noteer de routes die niet af te vangen zijn.
- Natuurlijke vloeistoffen uit worldgen (W) vallen hier niet onder.

## Client/server-impact
Alles server-side; meter-sync via de bestaande `condition_sync` (geen nieuwe payload). HUD client-only (bestaande laag).

## Tests
- **B**: unit tests regeltabel (dag/nacht/dak/water/bron/piek/refuge), drempels, fire resistance, modifier-volgorde.
- **S** (RCON, 2 clients):
  - meter loopt op in de zon en zakt 's nachts (`/time set`);
  - lava binnen 4 blokken;
  - water;
  - fire resistance halveert;
  - ignite bij 100;
  - verdamping per route bovengronds en geen verdamping onder een dak of ondergronds;
  - buiten ring 3 normaal.
- **W-checklist** voor 4b.

## Bij afronding
`waves.md` §4.
