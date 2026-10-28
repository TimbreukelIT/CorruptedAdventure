# Fase 2 golf 5 — stroom T (textures)

Branch `f2g5/textures`, worktree `P:/Projecten/Java/wt/f2g5-textures`, n = 62, basis `d55ff491` (merge-base bij de start).
Categorie: `textures`. Bron: `docs/design/fase-2-items.md` §7 (nulliet-palet §7.1), namen uit `docs/plans/fase-2/golf-5.md`.
Blockbench MCP niet bereikbaar: single-file Java-scripts in `tools/textures/`, ontworpen als kladversie in Python
(`build/t-preview/scratch/`, niet gecommit); de Java-scripts zijn de bron en geven pixel-identiek dezelfde PNG's.

## Wat
1. Voorbeeld voor het akkoord: `nullite_{ingot,sword,chestplate}`, `nullite_layer_1`, `sky_glass` + `sky_glass_pane_top`
   (vergelijkingsblad `build/t-preview/golf5-T-preview.png`).
2. Rest: nulliet-set (9 + 2 layers), ingot/ruw/blokken herkleurd; `sky_sand`, `glitch_shard_block` (animatie);
   `sky_light_draught`, `echo_shell`, `mob_effect/{anchored,resounding}`; `glitch_arrow` (item + entity);
   `sky_glass_lens` + `misc/sky_glass_lens_scope`; trim-palet `trims/color_palettes/sky_glass`.

## Palet nulliet
Omlijning `#050507`; antraciet `#0D0E11` `#15161A` `#1E2025` `#282A31` `#34373F` `#44474F` `#5A5E68`; paars `#4A2384`
`#7A3FD0` `#B48CFF`; cyaan `#1A7E8C` `#34C8D6` `#9EF2F6`; void-leren greep `#0B0A10` `#1A1624` `#2B2438`.

## Voortgang
- 2026-10-28 16:00 — voorbeeld gemaakt en aan main gemeld (`build/t-preview/golf5-T-preview.png`); intussen door.
- 2026-10-28 16:30 — alle textures + scripts; Java-uitvoer pixel-gelijk aan de kladversie (28 PNG's).
- 2026-10-28 — akkoord gebruiker op het voorbeeld (gat als ontbrekende pixel). H: glas en ruit renderen `translucent`;
  ik houd het goedgekeurde heldere midden (alpha 0/255), een tint kan later zonder codewijziging.

## Voorlopig (bevestigen bij de teststop)
- Nulliet: glitchranden als chroma-split in korte stukjes (cyaan links/boven, paars rechts/onder), één scheurlijn
  (cyaan, rest een rij lager in paars), zwakke scanlines; de "ontbrekende" pixel = een gat (transparant) in items en
  layers, zuiver zwart op de blokken. In het zwaard ontbreekt de kruisgreep-steen. Greep void-leer (bijna zwart-paars).
- Nulliet ruw/blokken: de vormen van golf 1 / ring 5 herkleurd naar antraciet; glinsters → cyaan/paars. Het erts
  (violette kristallen, pulserend emissive) blijft: het botst niet met het antraciet-paars.
- Hemelglas: wit-blauwe rand (licht linksboven), dubbele streep, vierpuntig fonkelkruisje; alleen alpha 0/255 (cutout).
  Ruit-rand: de randtonen in twee kolommen zoals vanilla.
- Hemelzand: de botzand-korrel herkleurd naar hemelwit, de lichtste korrels zuiver wit, enkele crème (zuiverkristal).
- Glitchscherfblok: vergroeide magenta scherven (palet van de glitchscherf), geen licht; frames: stil (meestal), rijen
  verschoven, een band cyaan, rijen weggevallen; `.mcmeta` met frame-volgorde (stil 40/18/30/12 ticks, flikkers 2–3).
- Hemellicht: de ronde fles van de dauwdrank, hemellicht met gloeisteen-spikkels, zuiverkristal op de kurk.
- Galmschelp: spiraalschelp van fossielsteen, de witte kern gloeit in de mond.
- Iconen in de effectkleur van C: `anchored` = bleekgouden anker (F4E6A6); `resounding` = fossielschelpje met drie
  lavendel galmbogen (B9A8F0).
- Glitchpijl: glitchscherf-punt met zwart facet, veren gesplitst cyaan/magenta; entity in de vanilla 32 x 32-indeling.
- Hemelglas-lens: getijdezilveren verrekijker met jade banden, hemelglas-lens bovenaan; scope-overlay = vanilla-
  indeling met een getijdezilveren rand (jade noppen) en vage hemelglas-ruiten (deels transparant, zoals vanilla).
- Void-coating: materiaal `sky_glass` (basis `#9CCAEC`, de waarde van H) in `CoatingTextures`; het void-template toont het embleem nu in
  hemelblauw. Pad `trims/color_palettes/sky_glass` (afspraak main; H bevestigt in pakket 2).

## W-checklist voor INT
- Nulliet-items in hand/inventaris naast getijdezilver; layers 1 + 2 op een speler (ook alleen beenstukken); het gat
  (ontbrekende pixel) zichtbaar maar niet storend; smithing-scherm met het nulliet-template.
- Nulliet ruw/blok/ruw blok in de inventaris en 3 x 3 geplaatst.
- Hemelzand, hemelglas en ruit geplaatst (3 x 3, ruit in een rij en een hoek); glas voor lucht en voor een donkere muur.
- Glitchscherfblok 3 x 3: flikkert onregelmatig, niet te druk.
- Hemellicht, galmschelp, lens in hand/inventaris; `anchored`/`resounding` rechtsboven en in de inventaris.
- Glitchpijl in de hand en afgeschoten (punt, veren, achterkruis); scope-overlay bij het kijken door de lens.
- Void-coating op harnas (hemelblauw) naast de poison-coating (oer-ijs) en stilte (witte kern).

## CODEBASE-delta
- Nieuw `tools/textures/Golf5NulliteTextures.java` (fase 2 golf 5, stroom T; single-file Java, alleen JDK, geen random;
  opzet als `Golf2SunstoneTextures`): `item/nullite_{ingot,sword,axe,pickaxe,shovel,hoe,helmet,chestplate,leggings,
  boots,upgrade_smithing_template}`, `item/raw_nullite`, `block/{nullite_block,raw_nullite_block}`,
  `models/armor/nullite_layer_{1,2}`; contactblad `build/texture-preview/golf5nullitetextures.png`.
- Nieuw `tools/textures/Golf5BlockTextures.java` (zelfde opzet, plus animaties: `addAnimated` met een `.mcmeta`
  frame-volgorde): `block/{sky_sand,sky_glass,sky_glass_pane_top,glitch_shard_block}` (+ `.mcmeta`).
- Nieuw `tools/textures/Golf5ItemTextures.java` (zelfde opzet; maatcheck 18 x 18 voor `mob_effect/`, 32 x 32 voor
  `entity/projectiles/`; de scope procedureel, 256 x 256 met deels transparant glas): `item/{sky_light_draught,
  echo_shell,glitch_arrow,sky_glass_lens}`, `mob_effect/{anchored,resounding}`, `entity/projectiles/glitch_arrow`,
  `misc/sky_glass_lens_scope`.
- `CoatingTextures`: materiaal `sky_glass` (`trims/color_palettes/sky_glass.png`); `void_coating` gebruikt het.
- Weg: `tools/textures/Golf34NulliteTextures.java` (template nu in `Golf5NulliteTextures`, pixel-identiek).
  `Golf1MaterialTextures` schrijft `item/nullite_ingot`, `block/{raw_,}nullite_block` niet meer (methode `ingot`,
  `INGOT`, `NULLITE` weg); `Ring5Textures` schrijft `item/raw_nullite` niet meer (`rawNullite`, `nugget`,
  `RAW_NULLITE` weg; het vak-voorbeeld leest het bestand nog).

## Vervangen bestaande textures (oud script → nieuw)
- `item/nullite_ingot`, `block/nullite_block`, `block/raw_nullite_block`: `Golf1MaterialTextures` → `Golf5NulliteTextures`.
- `item/raw_nullite`: `Ring5Textures` → `Golf5NulliteTextures`.
- `item/nullite_upgrade_smithing_template`: `Golf34NulliteTextures` (weg) → `Golf5NulliteTextures` (ongewijzigd).
- `item/void_coating_smithing_template`: embleem in hemelglas i.p.v. witte kern (`CoatingTextures`).
- Placeholders van G/H/C/A/L op dezelfde paden: mijn versie bij de merge (`--ours`).

## Beslissingen
- 2026-10-28, textures: voorbeeld nulliet (antraciet, korte cyaan/paarse chroma-split-randen, scheurlijn) en hemelglas
  (wit-blauwe rand, dubbele streep, fonkelkruisje, helder midden) goedgekeurd door de gebruiker (via main).
- 2026-10-28, textures: de "ontbrekende" pixel van nulliet is een echt gat (transparant); op dichte blokken zuiver zwart.

## Backlog
- (geen)
