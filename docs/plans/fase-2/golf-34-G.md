# Fase 2 golf 3 + 4 — stroom G (gear: vulkaniet + getijdezilver)

Categorie `items`. Branch `f2g34/gear` op `9becd290`, n = 66. Bron: `docs/design/fase-2-items.md` §6.1, §6.2, §6.5.

## Wat
1. Curve §6.1 in `GearTier` (alle 4 tiers): vulkaniet 2700 / 10 / 5,5 / toughness 4; getijdezilver 3200 / 11 / 6,5 /
   toughness 4,5; nulliet 3800 / 12 / 7,5; zonnesteen ongewijzigd. `ModArmorMaterials` leest uit de tier (geen
   wijziging). `GearTierTest.theCurveIsTheOneDecided` legt elk getal vast; de bestaande curve-tests blijven.
2. `registry/GearSet` (nieuw): record met de 9 stukken van een tier + `PIECES`; `ModItems.GEAR_SETS` (zonnesteen,
   vulkaniet, getijdezilver) en `ModItems.gear(tier)`. De datagen-helpers van S (`GearItemModels`, `GearNames`,
   `GearItemTags`, `GearRecipes`) lopen nu over alle sets in plaats van over de zonnesteen-velden.
3. 18 items `volcanite_*` (achteraan ring 3, na het hitte-coating-template) en `tidesilver_*` (achteraan ring 4, na het
   gif-coating-template), brandvrij, via `GearItems`.
4. 18 smithing-transforms: zonnesteen-stuk + `volcanite_ingot` → vulkaniet; vulkaniet-stuk + `tidesilver_ingot` →
   getijdezilver (ids `<tier>_<stuk>_smithing`, categorieën als vanilla).
5. Template-recepten: vulkaniet 6 `dew_crystal` + 2 `sunstone_ingot` + 1 `pumice`; getijdezilver 6 `primordial_ice` +
   2 `volcanite_ingot` + 1 `tidestone`; geen kopie-recepten.
6. Placeholder-PNG's (de zonnesteen-textures hergekleurd door een wegwerpscript buiten de repo):
   `item/{volcanite,tidesilver}_{9 stukken}.png`, `models/armor/{volcanite,tidesilver}_layer_{1,2}.png`. **T vervangt ze.**
7. `TierGatingGameTests`: zonnesteen, vulkaniet en getijdezilver met hun echte pickaxe (`ModItems.gear`); nulliet houdt
   de netherite-pickaxe met tool-component.
8. Tests: `item/GearSetsTest` (alle sets: schade = netherite + tierstap, schoffel blijft 1, aanvalssnelheid als
   netherite, mijnsnelheid = tier, harnas = tierstats, brandvrij, duurzaamheid), `datagen/GearRecipesTest` (de 27
   upgrades, 3 templates, geen kopie, harnas in `#trimmable_armor`).

## Voortgang
- 2026-10-27 19:15 start; code, placeholders en tests klaar 19:41, runData loopt.

## Voorlopig (bevestigen bij de teststop)
- Vorm template-recepten `SIS / SCS / SIS` zoals bij zonnesteen (S het Nether-materiaal, I de staaf van de tier
  ervoor, C de ringsteen). Het zonnesteen-recept kreeg sleutel `I` i.p.v. `D` (zelfde vorm en items).
- Nederlandse namen "Vulkanieten …" en "Getijdezilveren …" (bijvoeglijk, zoals "Netherieten zwaard").
- De schoffel slaat bij elke tier voor 1, zoals elke vanilla-schoffel (basisschade = −schadebonus); voor zonnesteen
  verandert er niets (was al −4).
- Brandvrij, zoals netherite en zonnesteen.

## W-checklist voor INT
- Smeedtafel: zonnesteen-stuk + vulkaniet-template + vulkanietstaaf → vulkaniet-stuk; idem vulkaniet → getijdezilver;
  enchantments en coating blijven.
- Templates craften (6 dauwkristal / 2 zonnesteenstaaf / 1 puimsteen; 6 oerijs / 2 vulkanietstaaf / 1 getijdesteen);
  geen kopie-recept.
- Vulkaniet-houweel mijnt getijdezilver- en jade-erts (en blokken), niet nulliet/witte kern; getijdezilver-houweel mijnt
  nulliet en witte kern.
- Harnas op het lichaam: layer zichtbaar; coating zichtbaar op lichaam en in de inventory. Niet brandbaar in lava.
- Ring 3-tab: vulkaniet-set achteraan; ring 4-tab: getijdezilver-set achteraan.

## CODEBASE-delta
- `registry/GearSet` (nieuw): record van de 9 stukken van een tier (`tools()`, `armor()`, `pieces()`, `PIECES`).
- `registry/ModItems`: `VOLCANITE_{SWORD,…,BOOTS}` achteraan ring 3, `TIDESILVER_{…}` achteraan ring 4; `GEAR_SETS`
  en `gear(tier)`.
- `item/GearTier`: de curve van §6.1 (vast).
- `item/GearItems`: schoffel-basisschade = −schadebonus van de tier.
- `datagen/GearItemModels`, `datagen/lang/GearNames`, `datagen/tags/GearItemTags`, `datagen/recipe/GearRecipes`: lopen
  over `ModItems.GEAR_SETS`; `GearRecipes` maakt ook de vulkaniet- en getijdezilver-templates.
- `gametest/TierGatingGameTests`: echte pickaxe per tier via `ModItems.gear`.
- Tests `item/GearSetsTest` (nieuw), `item/GearTierTest` (exacte curve), `datagen/GearRecipesTest` (alle tiers).
- Textures: `item/{volcanite,tidesilver}_*.png` (9 + 9), `models/armor/{volcanite,tidesilver}_layer_{1,2}.png`
  (placeholders, T vervangt).
- Design §4.3-tabel: verwijzen naar §6.1 voor ring 3–5 (de oude voorlopige getallen kloppen niet meer).

## Beslissingen
- (geen nieuwe ontwerpbeslissingen; zie "Voorlopig")

## Backlog
- (geen)
