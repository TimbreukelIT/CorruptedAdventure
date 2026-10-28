# Fase 2 golf 5 — stroom G (gear: nulliet)

Categorie `items`. Branch `f2g5/gear` op basis `d55ff491` (`git merge-base main HEAD` bij de start), n = 66.
Bron: `docs/design/fase-2-items.md` §6.1, §7.1. Review: `/code-review low d55ff491..f2g5/gear`.

## Wat
1. 9 items `nullite_{sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots}` via `GearItems`, brandvrij,
   achteraan de ring 5-sectie van `ModItems` (na het void-coating-template; tab 5), en de vierde `GearSet` in
   `ModItems.GEAR_SETS`. Stats komen uit `GearTier.NULLITE` (§6.1, al vastgelegd in golf 3 + 4).
2. `ModItems.gear(tier)` geeft nu direct een `GearSet` (elke tier heeft er een); `TierGatingGameTests` gebruikt voor
   elke tier de echte pickaxe (de netherite-terugval met tool-component is weg).
3. 9 smithing-transforms getijdezilver-stuk + `nullite_upgrade_smithing_template` + `nullite_ingot` → nulliet-stuk
   (vanzelf via `GearRecipes`, ids `nullite_<stuk>_smithing`).
4. Template-recept 6 `sky_glass` + 2 `tidesilver_ingot` + 1 `voidstone` (`SIS / SCS / SIS`) → 1, geen kopie-recept.
   **Wacht op H pakket 1** (`sky_glass`).
5. Namen via `GearNames`: "Nullite …" / "Nullieten …"; tags (`GearItemTags`) en modellen met trim-overrides
   (`GearItemModels`) lopen vanzelf mee.
6. Placeholder-PNG's (getijdezilver hergekleurd naar antraciet met paars-cyane accenten door een wegwerpscript buiten de
   repo): `item/nullite_{9 stukken}.png`, `models/armor/nullite_layer_{1,2}.png`. **T vervangt ze.**
7. Tests: `GearSetsTest` (alle vier tiers hebben een set, in ladder-volgorde), `GearRecipesTest` (nulliet in de ladder;
   template-assert na H), `ModBlocksTest` (materiaalvolgorde ring 5 filtert de gear eruit, zoals de templates).
   `GearTierTest` hoefde niet te veranderen: de nulliet-rij van §6.1 stond er al exact in.

## Voortgang
- 2026-10-28 16:19 start; code, placeholders en tests klaar 16:35; build + runData lopen.

## Voorlopig (bevestigen bij de teststop)
- Nederlandse namen "Nullieten …" (bijvoeglijk, zoals de vorige tiers).
- Brandvrij, zoals de andere tiers.
- Plaats in tab 5: achteraan de ring 5-sectie (na het void-coating-template, vóór de Nether-5-items), zoals getijdezilver
  achteraan ring 4 staat.

## W-checklist voor INT
- Smeedtafel: getijdezilver-stuk + nulliet-template + nullietstaaf → nulliet-stuk; enchantments en coating blijven.
- Template craften: 6 hemelglas / 2 getijdezilverstaaf / 1 voidsteen → 1; geen kopie-recept.
- Nulliet-houweel mijnt alles wat getijdezilver mijnt (nulliet, witte kern, hun blokken).
- Harnas op het lichaam: layer zichtbaar; coating zichtbaar op lichaam en in de inventory. Niet brandbaar in lava.
- Tab 5: nulliet-set na het void-coating-template.

## CODEBASE-delta
- `registry/ModItems`: `NULLITE_{SWORD,AXE,PICKAXE,SHOVEL,HOE,HELMET,CHESTPLATE,LEGGINGS,BOOTS}` achteraan ring 5 (na
  `VOID_COATING_SMITHING_TEMPLATE`); `GEAR_SETS` heeft alle vier tiers; `gear(tier)` geeft een `GearSet` (geen
  `Optional` meer).
- `datagen/lang/GearNames`: nulliet ("Nullite " / "Nullieten ").
- `datagen/recipe/GearRecipes`: nulliet-template uit `sky_glass` + `tidesilver_ingot` + `voidstone`.
- `gametest/TierGatingGameTests`: echte pickaxe voor elke tier.
- Tests `item/GearSetsTest`, `datagen/GearRecipesTest`, `registry/ModBlocksTest` (zie Wat 7).
- Textures: `item/nullite_*.png` (9), `models/armor/nullite_layer_{1,2}.png` (placeholders, T vervangt).

## Beslissingen
- (geen nieuwe ontwerpbeslissingen; zie "Voorlopig")

## Backlog
- (geen)
