# Fase 2 golf 3 + 4 — stroom C (verbruiksitems)

Branch `f2g34/consumables` op main `9becd290`, n = 67. Bron: `fase-2-items.md` §6.3, §6.6, op het raamwerk van §5.3
(`golf-2-V.md`).

## Wat
1. **Effecten (`conditions`)**: `RateEffect` remt nu één of meer condities, elk met een eigen factor uit de config.
   `cooled` remt ook een stijgende hitte (×0,5, `[conditions.heat] cooledFactor`); nieuw zijn `cleansed` (gif ×0,5,
   `[conditions.poison] cleansedFactor`) en `warmed` (kou ×0,5, `[conditions.cold] warmedFactor`). Het zijn vanilla
   statuseffecten, niet te brouwen, en de langste tijd wint. De doses staan in `ConsumableDoses`, met hun waarden in de
   sectie van hun conditie.
2. **Items (`items`)**: `dew_draught` (eind van ring 3), `hand_warmer` (eind van Nether-ring 3, dus in tab 3) en
   `purified_water` (eind van ring 4), met recepten, modellen, lang en/nl en placeholders.

Client/server: alle logica (drinken, lading, bijvullen, dosis, effect) draait op de server. De client toont alleen
tooltip, balkje en model (component `corruptedadventure:charges`, door vanilla gesynct). De item-properties staan in
`client/item/ConsumableItemProperties` (hernoemd van `CanteenItemProperties`; `Dist.CLIENT`).

## Texturenamen (voor T)
`item/dew_draught`, `item/purified_water`, `item/hand_warmer` (1–4 ladingen), **extra: `item/hand_warmer_empty`**
(0 ladingen), `mob_effect/cleansed`, `mob_effect/warmed` (18×18). Modellen (datagen): `item/hand_warmer` (leeg, met
override `corruptedadventure:charges` ≥ 1 → `item/hand_warmer_filled`), `item/dew_draught`, `item/purified_water`.
De placeholders zijn herkleurde kopieën (cactussap, veldfles, het icoon van cooled), gemaakt met een kladscript dat niet
in git staat.

## Voortgang
- 2026-10-27 19:20 start; 3 vragen over de handwarmer naar main (bijvullen, gecraft vol/leeg, gebruik).
- 19:43 code af volgens de aanbevolen opties; build loopt.
- 19:50 besluiten main: 1A, **2B (gecraft leeg)**, 3A; code aangepast.

## Voorlopig (bevestigen bij de teststop)
- Dauwdrank: shapeless dauwkristal + waterfles (alleen water) → 1, stack 16, drinkduur 32 ticks.
- Zuiver water: shapeless 1 oerijs + 4 glazen flessen → 4 (zoals een honingblok; oerijs valt alleen met Silk Touch),
  stack 16.
- Handwarmer: recept `LFL` / ` L ` (L = leer, F = vuurkristal) → 1. Bij gebruik hoor je vuurgeknetter, bij bijvullen
  het geluid van een vuursteen. Op een volle handwarmer gebeurt het vanilla-gedrag (de items wisselen). Kleur van het
  balkje `FF8A2A`.
- Kleuren van de effecten: cleansed `B8F0D0`, warmed `FF9A3C`.
- In creative kost een lading en een kristal niets (zoals de veldfles).

## W-checklist voor INT
- [ ] Ring 3 in de zon: met "Verkoeld" (veldfles of dauwdrank) stijgt de hitte ~45 s half zo snel (`/ca condition get`).
      De veldfles haalt geen hitte af, de dauwdrank wel −20, en je krijgt de fles terug.
- [ ] Ring 4 bij een gifpoel: zuiver water −20 gif, "Gezuiverd" 45 s (gif stijgt half zo snel), fles terug.
- [ ] Nether-ring 3: handwarmer gecraft leeg, bijvullen met een vuurkristal, dan 4 ladingen van −20 kou + 45 s "Verwarmd" (kou stijgt half zo snel);
      tooltip, balkje en model (leeg bij 0) lopen mee.
- [ ] Iconen "Gezuiverd"/"Verwarmd" met timer; melk haalt ze weg; relog: ze lopen door.
- [ ] 2 clients: de ladingen van de een veranderen niets bij de ander.

## CODEBASE-delta
- `condition/RateEffect`: `COOLED` (dehydration + heat), `CLEANSED` (poison) en `WARMED` (cold). Elk effect heeft een map
  conditie → factor-supplier; `slows(condition)`, `factor(condition)` (1 als het effect de conditie niet remt) en
  `mobEffect()`. `RateEffects.modify(condition, rate, active, (effect, condition) -> factor)`.
- `registry/ModMobEffects`: `CLEANSED` (`B8F0D0`, icoon `mob_effect/cleansed`) en `WARMED` (`FF9A3C`, `mob_effect/warmed`);
  lang "Cleansed"/"Gezuiverd" en "Warmed"/"Verwarmd".
- `ConsumableDoses`: `dewDraught` (hitte −20 + 45 s cooled), `purifiedWater` (gif −20 + 45 s cleansed),
  `handWarmerCharge` (kou −20 + 45 s warmed).
- `ServerConfig`: `HEAT.cooledFactor` 0,5, `dewDraughtAmount` 20, `dewDraughtCooledSeconds` 45; `POISON.purifiedWaterAmount`
  20, `purifiedWaterCleansedSeconds` 45, `cleansedFactor` 0,5; `COLD.warmedFactor` 0,5, `handWarmerAmount` 20,
  `handWarmerWarmedSeconds` 45.
- `registry/ModDataComponents.CHARGES` (`corruptedadventure:charges`, int 0–4, persistent + netwerk; afwezig = leeg).
- `item/HandWarmerItem`: stack 1, `MAX_CHARGES` 4, `CHARGES_PROPERTY`; gebruik 32 ticks met `UseAnim.NONE`;
  `overrideOtherStackedOnMe` (vuurkristal, rechtsklik) vult hem; balkje en tooltip
  `item.corruptedadventure.hand_warmer.charges` / `.refill`.
- `ModItems`: `DEW_DRAUGHT` (ring 3, `DoseDrinkItem`, stack 16), `HAND_WARMER` (Nether-ring 3), `PURIFIED_WATER` (ring 4,
  `DoseDrinkItem`, stack 16).
- Datagen: `ConsumableRecipes` (dauwdrank met een `DataComponentIngredient` voor de waterfles, zuiver water, handwarmer),
  `ConsumableItemModels.emptyOrFilled` (veldfles + handwarmer), `ConsumableText`.
- `client/item/CanteenItemProperties` → `ConsumableItemProperties` (veldfles + handwarmer).
- Tests: `RateEffectsTest` (alle effecten en hun condities), `HandWarmerItemTest`, `ConditionDoseTest` aangepast;
  `ModBlocksTest` (ring 3–4-materiaalvolgorde) filtert `DoseDrinkItem` en `HandWarmerItem`; GameTests
  `ConsumableGameTests`: dauwdrank + zuiver water, handwarmer gebruiken + bijvullen via het inventarismenu.

## Beslissingen
Besloten door de gebruiker (via main, 2026-10-27):
- 2026-10-27 · items · Handwarmer bijvullen: een vuurkristal in de inventaris op de handwarmer klikken (zoals de bundel);
  1 kristal = 4 ladingen; tooltipregel "Bijvullen: klik er een vuurkristal op" · geen recept nodig.
- 2026-10-27 · items · Een gecrafte handwarmer is leeg (zoals de veldfles).
- 2026-10-27 · items · Een lading gebruik je door 1,6 s ingedrukt te houden (32 ticks, `UseAnim.NONE`) · voorkomt per
  ongeluk verspillen.
- 2026-10-27 · items · Lege staat zoals de veldfles: `item/hand_warmer_empty`, een oranje balkje in 4 stappen, tooltip
  "Ladingen: 3/4".
- 2026-10-27 · items · Akkoord met Voorlopig: zuiver water 1 oerijs + 4 flessen → 4, dauwdrank shapeless, de plaats in
  ModItems en de config per conditie.

## Backlog
- `textures`: definitieve textures van de dauwdrank, het zuiver water, de handwarmer (vol en leeg) en de iconen
  `mob_effect/cleansed` en `mob_effect/warmed` (stroom T).
