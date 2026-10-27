# Fase 2 golf 2 — stroom S (zonnesteen-tier)

Categorie `items`. Branch `f2g2/sunstone` op `81703491`, n = 61. Bron: `docs/design/fase-2-items.md` §5.1.

## Wat
1. Stats zonnesteen in `GearTier.SUNSTONE`: tools 2200 (was 2031), harnasfactor 40 (was 37); de rest = netherite.
   `ModArmorMaterials` leest ze uit de tier (geen eigen getallen). `GearTierTest` blijft groen (curve ongewijzigd).
2. 9 items `sunstone_{sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,boots}` via `item/GearItems`
   (attack damage/speed per soort zoals netherite: zwaard 3/−2,4, bijl 5/−3,0, houweel 1/−2,8, schep 1,5/−3,0,
   schoffel −4/0; harnasduurzaamheid = `type.getDurability(40)`), brandvrij (`fireResistant()`, zoals netherite).
   Achteraan de ring 2-sectie van `ModItems` (na het uitdrogingscoating-template). Lang en/nl (`datagen/lang/GearNames`).
3. 9 smithing-transform-recepten (`datagen/recipe/GearRecipes`): template + diamanten stuk + `sunstone_ingot`;
   categorieën als vanilla (zwaard/harnas COMBAT, tools TOOLS); ids `corruptedadventure:sunstone_<stuk>_smithing`.
4. Template-recept `corruptedadventure:sunstone_upgrade_smithing_template`; geen kopie-recept.
5. Placeholder-PNG's (getinte netherite, door een wegwerpscript buiten de repo): `item/sunstone_*.png` (9) en
   `models/armor/sunstone_layer_{1,2}.png`. **T vervangt ze** (dit zijn van vanilla afgeleide pixels).
6. Itemmodellen (`datagen/GearItemModels`): tools `handheld`; harnas plat met vanilla's trim-overrides
   (`trim_type` 0,1…1,0, overlay `minecraft:trims/items/<stuk>_trim_<materiaal>`), zodat coatings ook in de inventory
   zichtbaar zijn, zoals bij vanilla-harnas.
7. Item-tags (`datagen/tags/GearItemTags`): `#swords/#axes/#pickaxes/#shovels/#hoes`, `#head/chest/leg/foot_armor`,
   `#trimmable_armor` (nodig voor de coatings), `c:tools/mining_tool` (houweel), `c:tools/melee_weapon` (zwaard, bijl).
8. `TierGatingGameTests`: zonnesteen = de echte `sunstone_pickaxe`; de andere tiers houden de netherite-pickaxe met
   tool-component.
9. Tests: `item/SunstoneGearTest` (attributen = netherite-tegenhanger, brandvrij, duurzaamheid 2200 / factor 40),
   `datagen/GearRecipesTest` (9 upgrades, template 6/2/1 → 1, geen kopie, harnas in `#trimmable_armor`);
   `ModBlocksTest` (materiaalvolgorde ring 2 negeert nu ook gear).

## Voortgang
- 2026-10-27 17:17 start; code + placeholders klaar 17:45, build loopt.

## Voorlopig (bevestigen bij de teststop)
- Vorm template-recept: `SDS / SCS / SDS` (S zielkristal, D diamant, C geschroeid zandsteen).
- Nederlandse namen "Zonnestenen zwaard/bijl/houweel/schep/schoffel/helm/kuras/beenstukken/laarzen" (bijvoeglijk,
  zoals vanilla "Netherieten …", "Stenen zwaard").
- Brandvrij zoals netherite (vanilla-conform; de opdracht stond het toe).
- Het smithing-recept neemt het item `sunstone_ingot` (zoals vanilla het netherietstaaf-item), niet `c:ingots/sunstone`.

## W-checklist voor INT
- Survival: diamanten stuk + template + staaf in de smeedtafel → zonnesteen-stuk; enchantments en een coating blijven.
- Template craften (6 zielkristal, 2 diamant, 1 geschroeid zandsteen); geen kopie-recept in het receptenboek.
- Zonnesteen-houweel mijnt vulkaniet/zwavel/vuurrobijn (en hun opslagblokken), niet getijdezilver/jade/nulliet.
- Harnas op het lichaam: layer-texture zichtbaar (placeholder/T); coating zichtbaar op lichaam én in de inventory.
- Item in lava/vuur verbrandt niet.
- Ring 2-tab: de 9 stukken achteraan de ring 2-items.

## CODEBASE-delta
- `item/GearItems` (nieuw): fabrieken `sword/axe/pickaxe/shovel/hoe(tier, props)` en `armor(tier, type, props)` met
  netherites attack damage/speed; harnasduurzaamheid uit `GearTier.durabilityFactor()`; extra props (brandvrij) van de
  aanroeper.
- `item/GearTier`: `SUNSTONE` 2200 / factor 40 (§5.1, vast); de rest voorlopig.
- `registry/ModItems`: `SUNSTONE_{SWORD,AXE,PICKAXE,SHOVEL,HOE,HELMET,CHESTPLATE,LEGGINGS,BOOTS}` achteraan ring 2.
- `datagen/GearItemModels` (aangeroepen uit `ModItemModelProvider`): handheld tools, harnas met vanilla-trim-overrides
  (`trackGenerated` voor de atlas-overlays).
- `datagen/tags/GearItemTags` (aangeroepen uit `ModItemTagsProvider` via `this::tag`).
- `datagen/recipe/GearRecipes` (in `ModRecipeProvider`): 9 `sunstone_<stuk>_smithing` + `sunstone_upgrade_smithing_template`.
- `datagen/lang/GearNames` (en + nl).
- `gametest/TierGatingGameTests`: `tierPickaxe(tier)` = echte zonnesteen-pickaxe.
- Tests `item/SunstoneGearTest`, `datagen/GearRecipesTest`.
- Textures: `item/sunstone_{9 stukken}.png`, `models/armor/sunstone_layer_{1,2}.png` (placeholders, T vervangt).
- Design §4.3-tabel: rij zonnesteen → 2200 / factor 40.

## Beslissingen
- (geen nieuwe ontwerpbeslissingen; zie "Voorlopig")

## Backlog
- (geen)
