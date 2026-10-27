# Fase 2 golf 2 — stroom V (verbruiksitems)

Branch `f2g2/consumables` op main `81703491`, n = 62. Bron: `fase-2-items.md` §1.3, §5.3, §5.4.

## Wat
1. **Raamwerk (`conditions`)**, server-side: een verbruiksitem geeft per conditie een *dosis* (`ConditionDose`):
   (a) direct punten eraf of erbij en (b) optioneel een tijdelijk `RateEffect` dat een **stijgende** rate
   vermenigvuldigt (RateModifier `RateEffects`, stage `GEAR`, naast de coatings: de factoren vermenigvuldigen).
   Waarden in `[conditions.dehydration]` van de SERVER-config.
2. **Items (`items`)**: `canteen`, `cactus_juice`, `salted_jerky` + recepten, tag, modellen, lang en/nl, placeholders,
   achteraan in de ring 2-sectie van `ModItems` (dus in tab 2).

Client/server: alle logica (vullen, drinken, dosis, effect) draait op de server; de client toont alleen tooltip,
balkje en model (data component `corruptedadventure:sips`, door vanilla gesynct). De item-property staat in
`client/item/CanteenItemProperties` (`Dist.CLIENT`). Het effect zelf wordt niet gesynct (zie vraag 1).

## Texturenamen (voor T)
`textures/item/canteen_empty.png` (0 slokken), `textures/item/canteen.png` (1–4 slokken),
`textures/item/cactus_juice.png`, `textures/item/salted_jerky.png`. Modellen (datagen): `item/canteen` (leeg, met
override `corruptedadventure:sips` ≥ 1 → `item/canteen_filled`), `item/cactus_juice`, `item/salted_jerky`.
Placeholders uit een kladscript (niet in git).

## Voortgang
- 2026-10-27 17:20 start; vragen 1–3 naar main (zichtbaarheid verkoeld, stapelen, model).
- 17:35 code af in de vorm van de brief (attachment `rate_effects`, langste tijd wint, 2 textures + balkje).
- 17:42 build + runData groen (1 test aangepast: `ModBlocksTest` filtert de verbruiksitems uit de materiaalvolgorde).
- 17:47 GameTests 5/5 groen (4 nieuw); 17:48 server-smoke n=62 groen (geen client-class-fouten).
- 17:50 commit `conditions:` (85b20e7d) en `items:`.

## Voorlopig (bevestigen bij de teststop)
- Recept veldfles: ` S ` / `LBL` / ` L ` (S = string als touw, L = leer, B = glazen fles) → 1 lege veldfles.
- Een gecrafte veldfles is leeg (0 slokken).
- In creative kost een slok niets (zoals een vanilla-drankje); een dosis die een meter verhoogt (jerky) doet in
  creative/spectator niets (de meter stijgt daar nooit).
- Cactussap: shapeless barrel cactus + glazen fles → 1; drinkduur 32 ticks (zoals een drankje), ook met een volle
  hongerbalk.
- Gedroogd vlees: shapeless zout + `#corruptedadventure:salted_jerky_meats` (`#c:foods/cooked_meat` +
  `#c:foods/cooked_fish`) → 2; NL-naam "Gezouten gedroogd vlees".
- "Verkoeld" loopt op de speltijd: offline telt mee (vraag 1).
- Vullen: rechtsklik op een waterbron (ook waterlogged blokken, zoals een fles), een water-cauldron (zakt één niveau,
  stat `use_cauldron`) of een dauwbron; een volle veldfles drinkt in plaats van te vullen.

## W-checklist voor INT
- [ ] Veldfles craften (leeg), vullen aan water/cauldron/dauwbron (Nether), 4 slokken drinken: −25 per slok, tooltip en
      balkje lopen mee, het model wisselt bij 0.
- [ ] Na een slok in de zon van ring 2 stijgt uitdroging ~45 s half zo snel (`/ca condition get`: rate 0,2 i.p.v. 0,4).
- [ ] Cactussap: −15, glazen fles terug, verkort een lopende veldfles-tijd niet.
- [ ] Gedroogd vlees: snel gegeten, +8 uitdroging (survival), 6 honger.
- [ ] Relog binnen 45 s: effect loopt door; na de dood weg.
- [ ] 2 clients: de slokken van de een veranderen niets bij de ander.

## CODEBASE-delta
- `condition/ConditionDose` (record: conditie, directe delta, optioneel `RateEffect` + seconden; `apply(ServerPlayer)`,
  pure `meterChange`: stijgen niet in creative/spectator), `condition/ConsumableDoses` (veldfles −25 + 45 s, cactussap
  −15 + 20 s, jerky +8; live uit de config), `condition/RateEffect` (enum `COOLED` → `DEHYDRATION`, factor
  `cooledFactor`), `condition/RateEffectTimers` (attachment-data: eindtijd in speltijd per effect, lenient codec;
  `start` = de latere eindtijd wint; pure `modify(condition, rate, now, factor)` alleen op stijgende rates),
  `condition/RateEffects` (server-API `start`/`timers` + `RateModifier`, stage `GEAR`, geregistreerd in
  `ConditionTicker.register` na `CoatingModifier`; leest de attachment alleen als de speler hem heeft).
- `ModAttachments.RATE_EFFECTS` (`corruptedadventure:rate_effects`, opgeslagen, geen `copyOnDeath`, server-only).
- `PlayerConditions.send`: geen sync naar een verbinding zonder ons kanaal (GameTest-mockspelers).
- `ServerConfig.DEHYDRATION`: `canteenSipAmount` 25, `canteenCooledSeconds` 45, `cactusJuiceAmount` 15,
  `cactusJuiceCooledSeconds` 20, `cooledFactor` 0,5 (0–1), `saltedJerkyAmount` 8.
- `registry/ModDataComponents` (nieuw, `corruptedadventure:sips`, int 0–4, persistent + netwerk; in `CorruptedAdventure`
  na `ModItems`).
- `item/CanteenItem` (stack 1, `MAX_SIPS` 4, `SIPS_PROPERTY`; `useOn` cauldron/dauwbron, `use` waterbron of drinken,
  drinken 32 ticks, balkje als de bundel, tooltip `item.corruptedadventure.canteen.sips`), `item/DoseDrinkItem`
  (eenmalig drankje met dosis, fles terug; cactussap), `item/DoseFoodItem` (voedsel met dosis; jerky).
- `ModItems` ring 2: `CANTEEN`, `CACTUS_JUICE` (stack 16), `SALTED_JERKY` (6 / 1,2, snel) na het uitdrogingstemplate.
- `ModTags.Items.SALTED_JERKY_MEATS`; datagen `lang/ConsumableText`, `recipe/ConsumableRecipes`,
  `tags/ConsumableItemTags`, `model/ConsumableItemModels` (nieuw package); `client/item/CanteenItemProperties`
  (`FMLClientSetupEvent`).
- Tests: `RateEffectTimersTest`, `ConditionDoseTest`, `CanteenItemTest`; GameTests `ConsumableGameTests` (cauldron,
  dauwbron, waterbron, slok + cactussap); `ModBlocksTest` filtert de verbruiksitems uit de ring 2-materiaalvolgorde.

## Beslissingen
(volgen uit vragen 1–3 aan main)

## Backlog
- `textures`: definitieve textures veldfles (leeg/gevuld), cactussap, gedroogd vlees (stroom T).
