# Fase 2 golf 3 + 4 — stroom T (textures)

Branch `f2g34/textures`, worktree `P:/Projecten/Java/wt/f2g34-textures`, n = 62, basis `9becd290`. Categorie: `textures`.
Bron: `docs/design/fase-2-items.md` §6 (paletten §6.2/§6.5), namen uit `docs/plans/fase-2/golf-34.md`.

## Wat
1. Voorbeeld voor het akkoord: `{volcanite,tidesilver}_{ingot,sword,chestplate}` en `{volcanite,tidesilver}_layer_1`.
2. Na akkoord: rest van beide sets + layer 2, de upgrade-templates van vulkaniet, getijdezilver en nulliet in de stijl
   van het zonnesteen-template, en de namen van C, A, R3 en R4. Extra van C (via main): `item/hand_warmer_empty`
   (leeg; `item/hand_warmer` = gevuld, de ladingen toont de code als oranje balkje).

## Paletten
- **Vulkaniet** (zwart basalt met gloeiende oranjerode aders, geen emissive): omlijning `#0A0809`, basalt `#161214`
  `#231D20` `#30282B` `#40363A` `#554A4D` `#6E6164` `#8A7B7E`; aders `#6E1408` `#A82A0C` `#E0501A` `#FF8A2C` `#FFC860`;
  verkoolde greep `#1C1412` `#34241E` `#4E362A`.
- **Getijdezilver** (helder zilver met jade-inleg op gevesten en randen): omlijning `#1A2830`, zilver `#3A4A54` `#5A6E78`
  `#7E949C` `#A4B8BE` `#C6D6DA` `#E2EEF0` `#FFFFFF`; jade (ring 4-jade) `#0E2E26` `#1C4A3C` `#2E6A54` `#44886C` `#62A688`
  `#8CC4A6` `#C4E6D2`; zeeleren greep `#1A2C34` `#2A444E` `#3C5E68`.

## Voortgang
- 2026-10-27 — voorbeeld gemaakt (kladontwerp in Python, bron als Java: `tools/textures/Golf34VolcaniteTextures.java`,
  `Golf34TidesilverTextures.java`); vergelijkingsblad `build/t-preview/volcanite-tidesilver-preview.png` (naast zonnesteen
  en netherite, layer 1 op een speler). `Golf1MaterialTextures` schrijft de twee ingots niet meer. Wacht op akkoord.
- 2026-10-27 — akkoord gebruiker (via main) op beide sets, plus: `raw_volcanite`, `raw_volcanite_block` en
  `volcanite_block` naar het basalt+gloed-palet; het erts alleen als het klein blijft (gedaan: één paletregel).
- 2026-10-27 — rest van beide sets, layer 2, drie templates, ruw vulkaniet + blokken, erts-overlay; C/A/R3/R4 in een
  fork (zelfde aanpak). De 8 voorbeeld-PNG's zijn pixel-identiek gebleven. Eindblad `build/t-preview/golf34-T-final.png`
  (gear, layers 1 + 2 op een speler, templates, vulkaniet ruw/blokken) en `build/t-preview/golf34-misc.png` (C/A/R3/R4
  naast vanilla-referenties). Ring 3-script opnieuw gedraaid: alleen `volcanite_ore_overlay.png` wijzigt.

## Voorlopig (bevestigen bij de teststop)
- Gereedschap: zelfde silhouetten als zonnesteen; vulkaniet met gloeiende scheuren in de kop, getijdezilver met een
  jade-binding waar kop en steel samenkomen en een jade-pommel. Vulkaniet-greep verkoold (donkerder dan zonnesteen).
- Templates: vulkaniet = vlam uit een gloeiende scheur in een basalttablet; getijdezilver = twee golven (zilveren kam
  over jade) in een getijdesteentablet; nulliet = paarse void-ring om een zwarte kern in een voidstone-tablet.
- Vulkaniet ruw/blokken: de goedgekeurde vormen van ring 3/golf 1 herkleurd (lichtheid → basalttinten); ruw = glinsters
  worden sintels, ruw blok = gloeiende naden tussen de brokken, vulkanietblok = gloeiende putjes + twee scheuren.
- Erts: alleen het palet (`Ring3Textures.VOLCANITE`): basalt, schaduwfacet sintelrood, glinster oranje; vorm gelijk.
- C: dauwdrank en zuiver water in de ronde fles van het cactussap (dauwdrank blauwgroen + dauwkristal op de kurk; zuiver
  water helder met belletjes + ijsschilfer); handwarmer = leren buidel met een venster op het vuurkristal (gloeiend vol,
  asgrijs leeg; onder rij 13 leeg voor het ladingenbalkje); `cleansed` = mintgroene druppel met witte fonkeling,
  `warmed` = vlam met heet hart (silhouet als `cooled`).
- A: brandpijl = zwavelpunt + rode veren, gifpijl = gifkristalpunt + groene veren; entity in de vanilla 32 x 32-indeling.
- R3/R4: zwavelblok (kolenblok-achtig), dauwkristalblok (amethist-achtig), zwavellamp (geelgroen, gloed van binnen);
  inleg: vuurrobijn als gefacetteerde ruit in puimsteen, jade als ovale cabochon in getijdesteen, aquamarijn als
  langwerpig kristal in lagunezandsteen; `block/pumice_raft` = afgerond bovenaanzicht met transparantie (tegen het model
  van R3 controleren).

## W-checklist voor INT
- Gear: vulkaniet- en getijdezilver-items in hand/inventaris naast zonnesteen/netherite; harnas (layer 1 + 2) op een
  speler, ook alleen beenstukken; smithing-scherm met alle vier templates.
- Vulkaniet ruw/blok/ruw blok in de inventaris en geplaatst (3 x 3, naden); vulkanieterts op steen, basalt, deepslate.
- C: drankjes en handwarmer (vol/leeg, ladingenbalk vrij) in de inventaris; `cleansed`/`warmed` rechtsboven en in de
  inventaris.
- A: beide pijlen in hand/inventaris; afgeschoten pijl-entity van dichtbij (punt, veren, achterkruis).
- R3: gloeibes, druipzwamsoep en vlotter in de hand; vlotter op lava van boven; zwavelblok en dauwkristalblok 3 x 3;
  zwavellamp geplaatst.
- R4 + vuurrobijn-puimsteen: ingelegd blok, trap, plaat, muur (naden, bovenkant).

## CODEBASE-delta
- Nieuw `tools/textures/Golf34VolcaniteTextures.java` (fase 2 golf 3 + 4, stroom T; single-file Java, alleen JDK, geen
  random; opzet als `Golf2SunstoneTextures`): `item/volcanite_{ingot,sword,axe,pickaxe,shovel,hoe,helmet,chestplate,
  leggings,boots,upgrade_smithing_template}`, `models/armor/volcanite_layer_{1,2}`, `item/raw_volcanite`,
  `block/raw_volcanite_block`, `block/volcanite_block`; contactblad `build/texture-preview/golf34volcanitetextures.png`.
  Palet zwart basalt + gloeiende aders (zie boven).
- Nieuw `tools/textures/Golf34TidesilverTextures.java` (zelfde opzet): `item/tidesilver_{ingot,sword,axe,pickaxe,shovel,
  hoe,helmet,chestplate,leggings,boots,upgrade_smithing_template}`, `models/armor/tidesilver_layer_{1,2}`. Palet helder
  zilver + jade (ring 4-jade).
- Nieuw `tools/textures/Golf34NulliteTextures.java` (zelfde opzet): `item/nullite_upgrade_smithing_template` (de
  nulliet-gear volgt in zijn golf).
- Nieuw `tools/textures/Golf34ItemTextures.java` (zelfde opzet; maatcheck ook 32 x 32 voor `entity/projectiles/`):
  `item/{dew_draught,purified_water,hand_warmer,hand_warmer_empty,fire_arrow,poison_arrow,ember_berries,dripshroom_stew,
  pumice_raft}`, `mob_effect/{cleansed,warmed}` (18 x 18), `entity/projectiles/{fire,poison}_arrow` (32 x 32).
- Nieuw `tools/textures/Golf34BlockTextures.java` (zelfde opzet; inleg over onze basistextures, dus na de ring 3/4-
  scripts draaien): `block/{sulfur_block,dew_crystal_block,sulfur_lamp,pumice_raft}`,
  `block/fire_ruby_inlaid_pumice{,_top}` (basis `pumice`), `block/jade_inlaid_tidestone{,_top}` (basis `tidestone`),
  `block/aquamarine_inlaid_lagoon_sandstone{,_top}` (basis `lagoon_sandstone{,_top}`).
- Weg: `tools/textures/Golf1TemplateTextures.java` (alle vier templates nu definitief: zonnesteen uit
  `Golf2SunstoneTextures`, de andere drie uit `Golf34*`). `Golf1MaterialTextures` schrijft `item/{volcanite,tidesilver}_
  ingot`, `block/raw_volcanite_block` en `block/volcanite_block` niet meer (constante `VOLCANITE` weg).
- `Ring3Textures`: schrijft `item/raw_volcanite` niet meer (methode `rawVolcanite` + `RAW_VOLCANITE` weg, ook uit het
  vak-voorbeeld van het contactblad); `VOLCANITE` (erts-overlay) = basalt + sintelrood schaduwfacet + oranje glinster.

## Vervangen bestaande textures (oud script → nieuw)
- `item/volcanite_ingot`, `item/tidesilver_ingot`, `block/raw_volcanite_block`, `block/volcanite_block`:
  `Golf1MaterialTextures` → `Golf34VolcaniteTextures` / `Golf34TidesilverTextures`.
- `item/{volcanite,tidesilver,nullite}_upgrade_smithing_template`: `Golf1TemplateTextures` (verwijderd) →
  `Golf34{Volcanite,Tidesilver,Nullite}Textures`.
- `item/raw_volcanite`: `Ring3Textures` → `Golf34VolcaniteTextures`.
- `block/volcanite_ore_overlay`: blijft in `Ring3Textures` (alleen het palet gewijzigd).

## Beslissingen
- 2026-10-27, textures: vulkaniet-voorbeeld (zwart basalt met gloeiende oranjerode scheuren; zwaard met gloeiende kern
  en sintel in de kruisgreep; borstplaat opengescheurd rond een gloeiende kern) en getijdezilver-voorbeeld (helder
  zilver, jade op gevest, pommel, kraag, borstjuweel, wenkbrauwband, riem, randen, manchetten) goedgekeurd.
- 2026-10-27, textures: `raw_volcanite`, `raw_volcanite_block` en `volcanite_block` naar het basalt+gloed-palet
  (gebruiker); het vulkanieterts alleen in kleur aangesloten (klein, één paletregel).

## Backlog
- [textures] `Golf34*Textures` delen met `Golf2*Textures` letterlijk `Texture`/`add`/`palette`/`write`/`contactSheet`
  (bij de bestaande regel "texturescripts ontdubbelen").
