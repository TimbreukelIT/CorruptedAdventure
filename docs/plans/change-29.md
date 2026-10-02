# Change 29 — Zout-set

Scope: `blocks` — change 29

**Doel:** de **zout-set** van ring 2 bestaat als speelbare blokken: **zoutkorst** (bovenlaag van de zoutvlakte), **zoutblok**, **gepolijst zout** (+ bouwvarianten, vraag 1) en **zoutkristal** (decoratie, zwak licht). Met blockstates/models, placeholder-textures van eigen pixels in het ring 2-texturescript, loot, tags, lang (en + nl) en een plek in de creative tab. Zout als losse grondstof is fase 2; deze change levert hooguit een **minimaal placeholder-item** `salt` als drop (vraag 3) en zet het nut ervan in de backlog. Plaatsing in de wereld volgt in change 30 (`salt_flats`: surface rules + kristal-feature), definitieve textures in change 37.

Bron: `fase-1-wereld.md` §2 Ring 2 → "Unieke blokken" ("Zout-set: zoutkorst (bovenlaag zoutvlakte), zoutblok, gepolijst zout, zoutkristal (decoratie, zwak licht)"), biome-tabel (`salt_flats`: "Kaal, wit en vlak met zoutkorst"), §1.4 (textures, creative tab), bouwplan rij 29 (B, D); beslissingenlog 2026-09-30 "Ring 2-bloksets". Sjablonen: change 21 (`docs/plans/change-21.md`, ring 2-blokken, `Ring2Textures.java`, tag-aanpak) en change 15/24 (placeholder-item als drop).

---

## Afstemming (2026-10-01)

Afgestemd met 21, 22, 30, 34 en 37 (basis: de aanbevolen opties).
- **Gedeelde kristal-helper wordt hier aangemaakt:** `ModBlockStateProvider.crystalCluster(block)` = blockstate (`directionalBlock`, rotaties als vanilla `amethyst_cluster`) + kruismodel cutout + itemmodel met parent **`minecraft:item/amethyst_bud`** (vanilla-display-transforms) en `layer0` = bloktexture. Change 34 hergebruikt hem (dauwkristalstadia); change 37 geeft hem een `emissive`-vlag.
- **Vraag 5 optie B (emissive) vervalt hier** en is change 37 vraag 3; deze vraag gaat alleen over het lichtniveau.
- **Vorm kristal** (`AmethystClusterBlock(7, 3)`) is `blocks` en hier definitief; 37 tekent binnen de vorm.
- **`salt_crust_side`-placeholder:** het deel onder de korstband = exact de pixels van `salt_block` (regel §1.4; change 37 controleert dat automatisch).
- **Vraag 6 C** (korst in `desert_plant_soil`) maakt de zoutkorst ook palmgrond (22 gebruikt die tag).
- `Ring2Textures.java` bestaat (21); placeholders `still`.
- **Losse items (24 vraag 3 = B, keuze gebruiker):** change 24 registreert `raw_sunstone` en `raw_turquoise` (geen `sunstone`/`turquoise`); `salt` komt ná `raw_turquoise`.
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17, commit `7714b77`). **Changes 18–28 zijn nog niet gebouwd; hun plannen zijn de bron (aanname).** Bij de start de echte namen en helpers uit `CODEBASE.md` nalopen en dit plan daarop aanpassen; afwijkingen noteren onder "Afwijkingen bij de bouw".

- **Blok-infrastructuur (gebouwd, change 10/15):** `ModBlocks.register(name, supplier)` (blockitem komt vanzelf), registratievolgorde = creative-tab-volgorde; `ModItems.ITEMS.registerSimpleItem` voor losse items, **vóór** de blockitems (valkuil in `CODEBASE.md`); `ModCreativeTabs` toont eerst alle blokken, dan de losse items. Datagen: `ModBlockStateProvider`, `ModItemModelProvider` (`blockItem`, `basicItem`, `wallInventory`), `ModBlockLootProvider` (`getKnownBlocks` = alle `ModBlocks`: elk blok een table), `ModBlockTagsProvider`, `ModItemTagsProvider` (`copy`), `ModTags` (`common(path)`), `EnglishLanguageProvider`/`DutchLanguageProvider`. Test `ModBlocksTest`.
- **Change 21 (aanname, plan):** sectie `// ---- Ring 2 ----` in `ModBlocks`; **`tools/textures/Ring2Textures.java`** bestaat (zelfde opzet als `Ring1Textures`: `TEXTURES`, `still`/`pulsing`, vaste seed per bestandsnaam, `HAND_EDITED`, paletten bovenin, contactblad `build/texture-preview/ring2.png`). Deze change **breidt dat script uit** (afspraak: één script per ring). Tag `corruptedadventure:desert_plant_soil` (`ModTags.Blocks.DESERT_PLANT_SOIL`) bestaat (vraag 7 A van change 21); change 21 noemt expliciet "change 29 kan zoutgrond toevoegen" → vraag 6. `ModCreativeTabs` filtert blokken zonder item (bloempotten); voor deze change niet nodig, alle zoutblokken hebben een item.
- **Change 22 en 24 (aanname, plannen):** palm-set en daarna de ertsen staan achteraan de ring 2-sectie; losse items `raw_sunstone`, `raw_turquoise` (change 24, vraag 3 B) staan na `soul_crystal`. De zoutblokken komen **ná de ertsen**, het item `salt` **ná `raw_turquoise`**. Bestaan die blokken/items bij de bouw niet, dan gewoon achteraan de ring 2-sectie resp. de losse items.
- **Change 23 (aanname, plan):** surface rules van ring 2 staan als losse takken bovenaan `above_preliminary_surface`; change 30 voegt daar de tak `salt_flats` toe en verwijst naar blokken **op registry-naam** (JSON). De registry-namen en blockstates van deze change zijn dus het contract met change 30.
- **Change 26 (aanname):** grotten in ring 2 (waterreservoirs, instortende zandgrotten). Carvers moeten door de zoutlagen kunnen snijden → `#minecraft:overworld_carver_replaceables`.
- **Change 27 (uitdroging, aanname):** geen raakvlak; zout doet niets met uitdroging (idee voor fase 2 → backlog, vraag 3).
- **Recipes horen bij `items` (fase 2)** → backlog (zoals change 21). Gepolijst zout (en zijn varianten) heeft in fase 1 dus geen survival-bron; dat is bewust gelijk aan de kloofbakstenen van change 21.
- Registry-namen met het ring 2-thema `salt_`. "Zoutblok" = `salt_block` (vanilla-patroon `<materiaal>_block`, zoals `clay`/`snow_block`), niet `salt`, omdat `salt` het item wordt.
- `MapColor`: zoutkorst `MapColor.SNOW`, zoutblok en gepolijst zout `MapColor.QUARTZ`, kristal `MapColor.QUARTZ` (alleen kaartkleur; geen ontwerpkeuze, aan te passen in change 37).

---

## Plan

### 1. Blokken (`registry/ModBlocks`, achteraan de sectie `// ---- Ring 2 ----`, ná de ertsen)

Volgorde (= creative tab): korst, zoutblok, gepolijst zout (+ varianten), kristal. Lijst = aanbevolen opties (vraag 1, 2, 4, 5).

| Registry-naam | Class | Basis (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `salt_crust` | `Block` | `sand` (sterkte 0,5, geluid zand) + `MapColor.SNOW` | vraag 2 A: vol blok, **valt niet** (geen `FallingBlock`), geen tool nodig, schop sneller; loot vraag 3 |
| `salt_block` | `Block` | `calcite` (sterkte 0,75, pickaxe nodig, geluid calciet) + `MapColor.QUARTZ` | natuurlijk onder de korst (change 30); dropt zichzelf |
| `polished_salt` | `Block` | `calcite` + `MapColor.QUARTZ` | bouwblok; dropt zichzelf |
| `polished_salt_stairs` / `_slab` / `_wall` | `StairBlock` / `SlabBlock` / `WallBlock` | `polished_andesite_stairs` / `_slab`, `andesite_wall`, maar met de sterkte/geluid van `polished_salt` (`ofFullCopy(polished_salt)`) | **alleen bij vraag 1 A/C**; trap met `polished_salt.defaultBlockState()` als basis |
| `salt_crystal` | vanilla `AmethystClusterBlock(height 7, aabbOffset 3, …)` — **geen eigen class** | `amethyst_cluster` (`noOcclusion`, `forceSolidOn`, `pushReaction DESTROY`, sterkte 1,5) + `MapColor.QUARTZ` + `lightLevel` vraag 5 | vraag 4 A: `facing` (6 richtingen) + `waterlogged`, breekt af als de steun verdwijnt; standaard-state `facing=up` (voor de feature van change 30) |

- **Zoutkorst valt niet** (vraag 2 A): de zoutvlakte ligt boven de grotten van change 26; een vallende korst stort in zodra een speler een blok eronder weghaalt of een carver eronder snijdt. "Instortend" is in ring 2 het thema van de zandgrotten, niet van de vlakte.
- **Hoogtes kristal** (`AmethystClusterBlock`): 7/3 = vorm van vanilla's `amethyst_cluster`. De vorm is hier definitief (vormen zijn `blocks`); change 37 tekent alleen de texture binnen deze vorm.
- **Brandbaarheid:** geen (zout brandt niet); niets in `ModFlammables`.
- Bij vraag 1 B vervallen trap/plaat/muur; bij vraag 1 C/D komen er rijen bij (zie vraag).

### 2. Item (`registry/ModItems`) — alleen bij vraag 3 A of C

- `salt` via `ITEMS.registerSimpleItem("salt")`, **vóór** de blockitems, ná `raw_turquoise` (change 24, vraag 3 B). **Placeholder**, nut en recipes in fase 2 (backlog). Javadoc kort: "placeholder, drop van zoutkorst".
- Conventietag `c:dusts/salt` (`ModTags.Items.DUSTS_SALT` via `common("dusts/salt")`), opgenomen in `c:dusts` via `addTag` (zelfde patroon als `c:gems/soul_crystal`). **Bij de bouw verifiëren** dat `Tags.Items.DUSTS` (`c:dusts`) in NeoForge 21.1.252 bestaat; zo niet, de tag weglaten en in de backlog noteren.

### 3. Tags (`ModBlockTagsProvider`, `ModItemTagsProvider`)

Blok-tags:
- `#minecraft:mineable/shovel`: `salt_crust`.
- `#minecraft:mineable/pickaxe`: `salt_block`, `polished_salt` (+ trap/plaat/muur), `salt_crystal`.
- `#minecraft:stairs`, `#minecraft:slabs`, `#minecraft:walls`: de gepolijste varianten (vraag 1).
- `#minecraft:overworld_carver_replaceables`: `salt_crust`, `salt_block` — grotten, kloven en `unnatural_tunnel` snijden door de zoutvlakte (change 26).
- **Niet** in `#minecraft:sand` (zou cactus, suikerriet en vanilla-woestijnregels op zout toelaten), `#minecraft:dirt`, `#minecraft:base_stone_overworld` (vanilla-ondergrondklodders) of `#minecraft:animals_spawnable_on` (spawns op de zoutvlakte beslist change 30).
- Vraag 6: zoutkorst eventueel in `corruptedadventure:desert_plant_soil` of `#minecraft:dead_bush_may_place_on`.

Item-tags: `copy` van `stairs`/`slabs`/`walls`; bij vraag 3 A/C `c:dusts/salt` (+ `c:dusts`).

Geen nieuwe eigen `TagKey` (bijv. `salt_blocks`): change 30 kan in zijn feature-predicates rechtstreeks op `salt_crust` matchen. Komt er later behoefte aan, dan in die change.

### 4. Loot (`ModBlockLootProvider`)

Bij vraag 3 A (aanbevolen):
- `salt_crust`: `createSingleItemTableWithSilkTouch(salt_crust, SALT, ConstantValue.exactly(4))` — zoals vanilla klei → 4 kleiballen (geen Fortune).
- `salt_block`, `polished_salt`, trap, muur: `dropSelf`; plaat: `createSlabItemTable`.
- `salt_crystal`: `dropSelf` (decoratie die je mee wilt nemen; zoals bloemen, zonder tool).

Bij vraag 3 B: alles `dropSelf` (plaat `createSlabItemTable`), geen item. Bij vraag 3 C: kristal `createSilkTouchDispatchTable` → 1–2 `salt` (+ `ApplyExplosionDecay`).

### 5. Models en blockstates (datagen)

- `ModBlockStateProvider`:
  - `salt_crust`: `cube_bottom_top` — top `salt_crust_top` (zeshoekige korstranden, zoals echte zoutvlaktes), zijkant `salt_crust_side`, bodem `salt_block`.
  - `salt_block`, `polished_salt`: `simpleBlock` (`cube_all`); gepolijste varianten via `stairsBlock`/`slabBlock`/`wallBlock` op `polished_salt`.
  - `salt_crystal`: `directionalBlock(block, models().cross("salt_crystal", modLoc("block/salt_crystal")).renderType("cutout"))` — geeft voor elke `facing` dezelfde x/y-rotaties als vanilla's `amethyst_cluster`-blockstate (up 0, down x180, noord x90, oost x90 y90, zuid x90 y180, west x90 y270; bij de bouw tegen de vanilla-JSON controleren); `waterlogged` krijgt hetzelfde model. **Gedeelde helper (afgestemd 2026-10-01):** dit wordt `ModBlockStateProvider.crystalCluster(block)` (blockstate + kruismodel); change 34 hergebruikt hem voor de dauwkristalstadia en change 37 geeft hem een `emissive`-vlag. Emissive is hier geen optie meer (vraag 5 B → change 37 vraag 3).
- `ModItemModelProvider`: blockitems via `blockItem`, muur via `wallInventory`; `salt_crystal` als plat item met de bloktexture, parent **`minecraft:item/amethyst_bud`** (vanilla-display-transforms, zoals vanilla's `amethyst_cluster`; afgestemd met change 34), `layer0` = `block/salt_crystal`; deel van de helper `crystalCluster`; `salt` (vraag 3 A/C) via `basicItem`.

### 6. Placeholder-textures (`tools/textures/Ring2Textures.java` uitbreiden)

- Nieuwe entries in `TEXTURES`, alle `still`; vaste seed per bestandsnaam; bestaande ring 2-PNG's (change 21/22/24) en `Ring1Textures.java` blijven byte-identiek.
- **Werkpaletten** (constanten bovenin, geen beslissing — definitief in change 37): zout `F7F5EF EFEBE2 E2DCCF CFC7B6 B8AE9A`; korstrand (opgestuwde randjes) `FFFFFF` + schaduw `C9C0AE`; kristal `FFFFFF F1F5F7 DCE6EB BFCFD8` met lichtroze zweem `EBD9D6`.
- Patronen: korst-top = grove zeshoekige polygonen met lichte opstaande randen (hergebruik van de polygoonscheuren van `cracked_clay`, change 21, in andere kleuren); korst-zijkant = 2–3 px korstband bovenaan op zoutblok-korrel; zoutblok = fijne korrel met enkele kristalvlakjes; gepolijst zout = gladde vlakken met rand (vanilla-indeling "gepolijst"); kristal = 2–3 puntige kristallen in kruismodel-vorm (alpha alleen 0/255, cutout).

| Bestand | Voor |
|---|---|
| `block/salt_crust_top.png`, `block/salt_crust_side.png` | zoutkorst (bodem = `salt_block`) |
| `block/salt_block.png` | zoutblok; bodem van de korst |
| `block/polished_salt.png` | gepolijst zout + trap/plaat/muur |
| `block/salt_crystal.png` | kristal (kruismodel + item; cutout) |
| `item/salt.png` | item `salt` (alleen vraag 3 A/C) |

5 PNG's (+1 bij het item), geen animaties. Volgorde: **eerst het script draaien, dan `runData`** (`ExistingFileHelper`).

### 7. Lang (`EnglishLanguageProvider`, `DutchLanguageProvider`)

Commentaarblok "Ring 2 salt". Nederlands volgens de vanilla-`nl_nl`-terminologie voor "gepolijst … trap/plaat/muur" (opzoeken in de asset-cache, valkuil in `CODEBASE.md`).

| Key | en_us | nl_nl (indicatief) |
|---|---|---|
| `salt_crust` | Salt Crust | Zoutkorst |
| `salt_block` | Salt Block | Zoutblok |
| `polished_salt` (+ stairs/slab/wall) | Polished Salt (Stairs/Slab/Wall) | Gepolijst zout (Gepolijste zouttrap/-plaat/-muur) |
| `salt_crystal` | Salt Crystal | Zoutkristal |
| `salt` (item) | Salt | Zout |

### 8. Tests in code

Geen nieuwe unit test nodig: `ModBlocksTest` dekt blockitem + tabvolgorde vanzelf (alle nieuwe blokken hebben een item). Optioneel, klein: `salt_crystal.defaultBlockState()` heeft `facing=up` en `lightEmission` = gekozen waarde (vraag 5) — vangt een verkeerde basis-copy af waar change 30 op leunt.

### 9. Documentatie

- `CODEBASE.md`: blok-tabel (ring 2 zout: class, basis, loot, tags, licht), item-tabel (`salt`: placeholder), `ModTags` (`c:dusts/salt`), textures-regel (`Ring2Textures.java`: zoutbestanden), "Laatst bijgewerkt". Valkuil: `AmethystClusterBlock` hergebruikt speelt bij een projectiel-treffer het amethist-belgeluid (vanilla `AmethystBlock#onProjectileHit`) — bekend, zie risico's.
- `fase-1-wereld.md` §2 Ring 2 → "Unieke blokken": **Uitwerking (change 29)** met de blokkenlijst, gedrag (korst valt niet, kristal aan elke zijde, licht), loot/drop, en de overdracht naar change 30 (zie hieronder).
- `02-beslissingenlog.md`: de regels hieronder.
- `docs/backlog.md`:
  - `[items] Zout (salt): doel en recipes in fase 2 — 4 zout ↔ zoutblok (c:storage_blocks/salt), zoutblok → gepolijst zout (+ steenzaag: trap/plaat/muur); eventueel voedsel/conservering of een rol bij uitdroging. (bron: change 29)` (bij vraag 3 B zonder het item: "zout-item + korst-loot aanpassen").
  - Afgewezen opties als later-idee (bijv. vallende korst, zoutbakstenen-familie, zoutkristal-groei).

### Overdracht naar latere changes

| Change | Wat ligt er klaar |
|---|---|
| 30 `salt_flats` (`worldgen`) | Surface rule: `corruptedadventure:salt_crust` als bovenste blok (vol, valt niet), daaronder `corruptedadventure:salt_block` als subsurface (diepte = keuze change 30). Kristallen via een feature (`simple_block` met `salt_crystal[facing=up]`, filter: korst eronder, lucht erop — `would_survive` werkt want `canSurvive` vraagt een stevige steun). Korst en zoutblok zitten in `#overworld_carver_replaceables`. Korst zit **niet** in `#animals_spawnable_on`/`#sand`: dieren-spawns en eventuele flora (vraag 6) beslist change 30. Monsters spawnen op de korst als op elk vol blok; kristallen (licht vraag 5) blokkeren in hun buurt monster-spawns. |
| 37 textures | Bestandsnamen hierboven zijn definitief; change 37 vervangt alleen pixels (en kiest eventueel emissive voor het kristal als vraag 5 B niet gekozen is). |

---

## Client/server-impact

- Blokken en het item zijn common registry-entries; NeoForge synct de registry-ID's bij het joinen.
- Gedrag in common code: `AmethystClusterBlock` (`canSurvive`/`updateShape`, beide kanten, vanilla), loot server-side, licht = blok-eigenschap (beide kanten, vanilla-lichtengine).
- **Geen client-code:** render type in de model-JSON (`cutout` voor het kristal), kleuren in de PNG's (geen `BlockColor`), eventuele emissive (vraag 5 B) in de model-JSON. Geen netwerk, geen SavedData/attachments, geen common-setup-code.
- Het texturescript staat buiten `src/` en komt niet in de jar.

---

## Tests

Bouwplan: **B, D**. Aanbevolen extra: **S** met RCON-checks (loot en overleven zijn server-side en goedkoop te controleren).

- [ ] **Script** — `java tools/textures/Ring2Textures.java` draait; 5 (6) nieuwe PNG's van 16×16; kristal alleen alpha 0/255; tweede run → `git status` schoon; bestaande ring 2-PNG's en `Ring1Textures.java` ongewijzigd; contactblad `build/texture-preview/ring2.png` toont de zoutset.
- [ ] **B** — `./gradlew build` slaagt, incl. `ModBlocksTest` (nieuwe blokken met blockitem, in de tab ná de ertsen; `salt` bij de losse items ná `raw_turquoise`).
- [ ] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit: blockstates (kristal: 12 states, rotaties = vanilla `amethyst_cluster`), models (kristal `render_type` cutout), item-models (`salt_crystal` plat, `salt`), loot tables (korst Silk Touch-tak → korst, anders 4 `salt` bij vraag 3 A; plaat dubbel → 2), tags (`mineable/shovel`, `mineable/pickaxe`, `stairs`/`slabs`/`walls`, `overworld_carver_replaceables`, `c:dusts/salt`, item-copy), lang en/nl. De bestaande gegenereerde bestanden van ring 1 en eerdere ring 2-changes zijn ongewijzigd (`git diff` alleen nieuwe regels in gedeelde tag-/lang-bestanden).
- [ ] **S** — `tools/server-smoke-test.sh` met `level-name=smoke-change29`: server start zonder fouten of client-class-meldingen. Via RCON (daarna RCON weer uit):
  - tags: per blok `setblock` + `execute if block <pos> #<tag>` (`#minecraft:mineable/shovel` korst, `#minecraft:mineable/pickaxe` zoutblok/kristal, `#minecraft:overworld_carver_replaceables` korst + zoutblok, `#minecraft:walls` muur; korst **niet** in `#minecraft:sand`);
  - loot: `loot insert <kist> mine <korst> minecraft:iron_shovel` → 4 `salt` (vraag 3 A), met Silk Touch → `salt_crust`; zoutblok met `minecraft:wooden_pickaxe` → zichzelf; dubbele plaat → 2; kristal → zichzelf;
  - overleven: `setblock … salt_crystal[facing=north]` tegen een steen, steen weghalen (`setblock … air`) → kristal weg; `salt_crystal[facing=up]` op `salt_crust` blijft staan;
  - korst valt niet: korst op Y+5 boven lucht zetten, buur-update geven → blijft staan (`execute if block`).
- **Blokkerende handmatige test: nee.** Alles waar change 30 en 37 op bouwen (registry-namen, blockstates met `facing=up` als standaard, tags, loot, texturebestandsnamen) is automatisch te controleren (build, runData, RCON). Uiterlijk is placeholder (definitief in change 37); licht en geluid zijn speelgevoel en worden pas in context beoordeeld zodra change 30 de zoutvlakte plaatst. → **W-achteraf**, in-game checklist (runClient, creative + survival):
  - [ ] Creative tab: zoutblokken ná de ring 2-ertsen in de volgorde korst → zoutblok → gepolijst (+ varianten) → kristal; `salt` bij de losse items ná `raw_turquoise`; namen en/nl.
  - [ ] Alle blokken plaatsbaar, geen paars-zwart; korst-top/zijkant/bodem kloppen; muur verbindt met vanilla-muren; trap/plaat in alle vormen.
  - [ ] Kristal plaatsbaar op alle zes zijden (vloer, plafond, wanden), juiste oriëntatie, waterlogged in water; breekt als de steun weg is; geeft zwak licht (F3: bloklicht vraag 5), naast het kristal spawnen 's nachts geen monsters.
  - [ ] Mijnen in survival: korst met de hand/schop → 4 zout (vraag 3), met Silk Touch → korst; zoutblok alleen met pickaxe; kristal → zichzelf.
  - [ ] Korst onder je wegmijnen: de korst erboven blijft hangen (valt niet).
  - [ ] Geluiden passen redelijk (zand voor korst, calciet voor zout, amethist voor kristal; definitief later).

---

## Risico's / aandachtspunten

- **`AmethystClusterBlock` hergebruiken:** de vanilla-class erft van `AmethystBlock` en speelt bij een projectiel-treffer het amethist-belgeluid. Klein, en zout-kristal mag "klinken"; wie dat niet wil kiest vraag 4 B/C (eigen class). Geen eigen codec nodig (de vanilla-class heeft er een).
- **Licht in de overworld** blokkeert monster-spawns (vanaf 1.18 alleen bij bloklicht 0): veel kristallen op de zoutvlakte maken haar veiliger. Dichtheid is een keuze van change 30; licht 3 houdt het effect klein (vraag 5).
- **Korst-drop als item** (vraag 3 A): zonder Silk Touch kun je de korst niet als blok meenemen. Dat is vanilla-gedrag (klei, grasblok) en stuurt spelers naar het zoutblok als bouwmateriaal.
- **Gepolijst zout zonder survival-bron** tot fase 2 (recipes): bewust, gelijk aan change 21.
- **`c:dusts/salt`**: conventienaam bij de bouw verifiëren (`Tags.Items` in NeoForge 21.1.252); bij twijfel weglaten.
- **Afhankelijk van niet-gebouwde changes** (21, 22, 24): als `Ring2Textures.java`, de ring 2-sectie of `desert_plant_soil` bij de bouw anders heten of ontbreken, de echte stand volgen; ontbreekt `Ring2Textures.java` helemaal, dan **stoppen en vragen** (het script hoort bij change 21).

---

## Ontwerpvragen

### 1. Hoe groot wordt de zout-set?
In alle opties: `salt_crust`, `salt_block`, `polished_salt`, `salt_crystal` (de vier uit het design-doc).
- **A. Kern + gepolijst zout met trap/plaat/muur (aanbevolen).** 7 blokken, 5 textures. Gepolijst zout wordt echt bouwmateriaal (zoals vanilla gepolijst andesiet/blackstone); weinig extra werk, geen extra textures.
- **B. Alleen de vier kernblokken.** 4 blokken. Exact het design-doc; gepolijst zout is dan alleen een vol blok, minder bruikbaar om mee te bouwen.
- **C. Kern + trap/plaat/muur voor zoutblok én gepolijst zout.** 10 blokken, geen extra textures. Meer bouwvarianten, ook van het natuurlijke blok; dubbel zoveel datagen-regels voor weinig extra.
- **D. Als A + een zoutbakstenen-familie** (`salt_bricks` + trap/plaat/muur). 11 blokken, 6 textures. Rijker bouwpalet (bijv. voor zoutstructures in fase 3); gaat verder dan het design-doc.

Antwoord: **A** — kern + gepolijst zout met trap/plaat/muur (7 blokken, 5 textures).

### 2. Hoe gedraagt de zoutkorst zich?
- **A. Vol blok dat niet valt; geen tool nodig, schop sneller; sterkte en geluid als zand (aanbevolen).** Eenvoudig `Block`; change 30 kan hem als bovenste blok in een surface rule zetten; de vlakte stort niet in boven de grotten van change 26. Geluid is placeholder.
- **B. Vallend blok zoals zand** (`ColoredFallingBlock`). "Bros" gevoel, maar de zoutvlakte zakt in boven grotten en bij elk gegraven gat; extra risico op zwevende korst na worldgen die bij de eerste update valt.
- **C. Dunne laag** (laagblok zoals een sneeuwlaag) bovenop het zoutblok. Mooier randje, maar surface rules kunnen geen blok in de lucht erboven zetten: change 30 heeft dan een extra feature nodig voor de hele vlakte (duurder, kan per generatievolgorde gaten geven).

Antwoord: **A** — vol blok dat niet valt; geen tool nodig, schop sneller; sterkte en geluid als zand.

### 3. Wat laten de zoutblokken vallen — komt er al een zout-item?
- **A. Placeholder-item `salt` (nut in fase 2, backlog); korst → 4 zout zonder Silk Touch (met Silk Touch → korst), zoutblok/gepolijst/kristal → zichzelf (aanbevolen).** Zoals klei → kleiballen en het zielkristal-placeholderitem van change 15; spelers verzamelen al zout voor fase 2. Eén extra item + texture; `c:dusts/salt` voor mod-compatibiliteit.
- **B. Alles dropt zichzelf, geen item.** Kleinst; fase 2 moet dan de korst-loot aanpassen en het item toevoegen. Tot dan geen "zout-oogst".
- **C. Als A, en ook het kristal → 1–2 zout** (Silk Touch → kristal), zoals amethistclusters → scherven. Meer zout-bronnen; de decoratie is zonder Silk Touch niet mee te nemen.

Antwoord: **A** — placeholder-item `salt` (nut in fase 2); korst → 4 zout (Silk Touch → korst), zoutblok/gepolijst/kristal → zichzelf; `c:dusts/salt`.

### 4. Welke vorm heeft het zoutkristal?
- **A. Vanilla `AmethystClusterBlock` hergebruiken: kruismodel, op alle zes zijden plaatsbaar (vloer, wand, plafond), waterlogbaar, breekt als de steun wegvalt (aanbevolen).** Geen eigen class of codec; vertrouwd gedrag; change 30 plaatst hem met `facing=up`. Bijwerking: amethist-belgeluid bij een projectiel-treffer.
- **B. Alleen staand op de grond** (eigen class `extends BushBlock`, `mayPlaceOn` = zoutkorst/zoutblok). Strikter "groeit uit de korst"; één eigen class + codec, niet tegen wanden of plafonds.
- **C. Vol kristalblok** (zoals een amethistblok). Eenvoudigst te plaatsen en bouwen; minder "decoratie", geen sierlijke vorm.

Antwoord: **A** — vanilla `AmethystClusterBlock(7, 3)` hergebruiken (kruismodel, zes zijden, waterlogbaar).

### 5. Hoe sterk is het licht van het zoutkristal?
Design: "zwak licht". In de overworld blokkeert elk bloklicht > 0 monster-spawns op die plek.
- **A. Licht 3, geen emissive (aanbevolen).** Zwakke gloed zoals het zielkristal-erts; de witte texture valt 's nachts al op. Geen extra modelwerk; change 37 kan emissive alsnog kiezen.
- ~~**B. Licht 3 + emissive kruismodel.**~~ *Vervallen (afstemming 2026-10-01): gloed/emissive is texture- en modelwerk en wordt beslist in change 37 vraag 3; deze vraag gaat alleen over het lichtniveau.*
- **C. Licht 1.** Bijna onzichtbaar licht; vooral een effect op spawns (die blijven rond elk kristal weg).
- **D. Licht 5** (zoals vanilla `amethyst_cluster`). Duidelijk lichtpunt; de zoutvlakte wordt 's nachts merkbaar veiliger.

Antwoord: **A** — licht 3, geen emissive (emissive beslist change 37 vraag 3).

### 6. Mag woestijnflora op de zoutkorst staan?
Design: `salt_flats` is "kaal, wit en vlak". Change 21 hield `desert_plant_soil` open voor zoutgrond.
- **A. Nee: zout in geen enkele plant-tag (aanbevolen).** De zoutvlakte blijft kaal, ook als spelers er flora proberen te planten; het past bij echte zoutvlaktes (zout doodt planten).
- **B. Alleen vanilla-dode struik: korst in `#minecraft:dead_bush_may_place_on`.** Change 30 kan hier en daar een dode struik zetten als enige leven; eigen ring 2-flora niet.
- **C. Korst in `corruptedadventure:desert_plant_soil`** (dorre struik, tonnetjescactus, woestijnbloem). Meeste vrijheid voor change 30 en voor spelers; de vlakte voelt minder kaal en uniek.

Antwoord: **A** — nee: zout in geen enkele plant-tag.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | blocks | Zout-set: `salt_crust`, `salt_block`, `polished_salt`, `salt_crystal` + `polished_salt_stairs`/`_slab`/`_wall` | Gepolijst zout wordt echt bouwmateriaal; geen extra textures |
| 2026-10-01 | blocks | Zoutkorst: vol blok dat niet valt, geluid/sterkte als zand; korst en zoutblok in `#overworld_carver_replaceables`, niet in `#sand`/`#dirt`/`#base_stone_overworld` | Surface rule-bovenlaag voor `salt_flats` (change 30); grotten snijden erdoor; geen vanilla-woestijn- of ondergrondregels op zout |
| 2026-10-01 | blocks | Zout-drops: korst → 4 `salt` (Silk Touch → korst), rest zichzelf; zout-item wel als placeholder (nut fase 2) | Zoals klei → kleiballen; spelers verzamelen al voor fase 2 |
| 2026-10-01 | blocks | Zoutkristal: vanilla `AmethystClusterBlock(7, 3)` | Geen eigen class; vertrouwd gedrag |
| 2026-10-01 | blocks | Zoutkristal-licht: 3, geen emissive (beslist in change 37) | "Zwak licht" uit het design |
| 2026-10-01 | blocks | Woestijnflora op zoutkorst: nee, zout in geen plant-tag | Zoutvlakte blijft kaal |

Commit (na `/code-review`): `blocks: add salt set`.

---

## Bouwnotities (change 29, 2026-10-02)

Aannames gecontroleerd tegen de code na change 28 (`2e7567d`): klopten. Ring 2-sectie in `ModBlocks` eindigt op `cream_canyon_turquoise_ore` (de zoutblokken komen daarna), `raw_turquoise` is het laatste losse item, `Ring2Textures.java` heeft `still`/`renderUnchecked`/`Cells`/`outline`, `desert_plant_soil` bestaat. `Tags.Items.DUSTS` (`c:dusts`) bestaat in NeoForge 21.1.252, dus `c:dusts/salt` gaat erin (zelfde patroon als `c:gems/soul_crystal`). Beslissingenlog-regels met de bouwdatum 2026-10-02.

Afwijkingen en implementatiedetails (door de agent gekozen):
- **Vanilla-itemmodel `amethyst_cluster`:** heeft in 1.21.1 parent `minecraft:item/generated` (alleen een `head`-transform), niet `item/amethyst_bud` zoals het plan stelt. De afgestemde keuze (parent `item/amethyst_bud`, de knop-transforms) is gevolgd; het verschil is alleen hoe groot het kristal in de hand/GUI staat. Valkuil in `CODEBASE.md`.
- **`crystalCluster` schrijft het itemmodel zelf** via `itemModels()` van de `BlockStateProvider` (één helper voor blockstate + model + item, zoals afgesproken); `ModItemModelProvider` slaat `salt_crystal` dus over (anders twee providers op hetzelfde bestand).
- **Muur:** `ofFullCopy(polished_salt).forceSolidOn()` — vanilla-muren hebben `forceSolidOn`, een kopie van een vol blok niet. Trap/plaat: `ofFullCopy(polished_salt)`.
- **nl-namen:** "Gepolijste zouttrap/-plaat/-muur" (vanilla "Gepolijste andesiettrap", "Gepolijste andesietplaat").
- **Texture-details:** korst-top = 5 wrappende cellen met witte opstaande randen (`gap` < 0,7) en een schaduwrand aan de bovenkant van elke plaat; korst-zijkant = de pixels van `salt_block` (via `renderUnchecked`, dus altijd gelijk) met een korstband van 2–3 px waarvan de onderste rij schaduw is; zoutblok = korrel + 4 kristalvlakjes; gepolijst = glad vlak met lichte rand linksboven en donkere dubbele rand rechtsonder; kristal = 3 punten (midden 13 hoog, zijkanten 7–9, naar buiten hellend), roze zweem op ~1/9 van de vlakpixels; item `salt` = hoopje korrels met een donkere omlijning (`SALT_OUTLINE` `8A8070`, extra werkkleur).
- **Extra tests in `ModBlocksTest`** (8 → 11): volgorde zout-set na het laatste erts en `salt` direct na `raw_turquoise`; kristal standaard `facing=up`, droog, licht 3; korst geen `FallingBlock`.

### Testresultaten

- **Script:** `java tools/textures/Ring2Textures.java` schrijft 6 nieuwe PNG's (16×16; kristal en item alpha 0/255, de rest volledig dekkend); tweede run byte-identiek (md5); alle bestaande ring 2-PNG's ongewijzigd (md5 vóór/na); `salt_crust_side` rijen 3–15 = `salt_block` pixel voor pixel.
- **B:** `./gradlew build` geslaagd, 62 testklassen / 301 tests, geen fouten (`ModBlocksTest` 11).
- **D:** `./gradlew runData` geslaagd, geen "texture does not exist". Nieuw: 7 blockstates (kristal 12 varianten, rotaties = vanilla `amethyst_cluster`), 12 block-models (kristal `render_type` cutout), 8 item-models (`salt_crystal` parent `item/amethyst_bud`, `salt`), 7 loot tables (korst: Silk Touch-tak → korst, anders `set_count` 4 + `explosion_decay`), `c:dusts`, `c:dusts/salt`; gewijzigd (alleen nieuwe regels): `mineable/pickaxe`, `mineable/shovel`, `overworld_carver_replaceables`, `stairs`/`slabs`/`walls` (blok + item), lang en/nl.
- **S:** `tools/server-smoke-test.sh` met `level-name=smoke-change29` (preset `corrupted_world`, seed 20260930): `Done (9.450s)`, geen fouten of client-class-meldingen. Daarna server met RCON (`build/tmp/c29_test.py`), **101/101 geslaagd**:
  - alle 7 blokken plaatsbaar; tags: korst in `#mineable/shovel` + `#overworld_carver_replaceables`, zoutblok in `#mineable/pickaxe` + `#overworld_carver_replaceables`, gepolijst + vormen + kristal in `#mineable/pickaxe`, vormen in `#stairs`/`#slabs`/`#walls`; korst en zoutblok **niet** in `#sand`, `#dirt`, `#base_stone_overworld`, `desert_plant_soil`, `#dead_bush_may_place_on`, `#animals_spawnable_on`; gepolijst/kristal niet in `#overworld_carver_replaceables`;
  - kristal-standaardstaat `facing=up,waterlogged=false`;
  - loot: korst + ijzeren schop → 4 `salt`, met de hand → 4, Fortune III 10× → altijd 4, Silk Touch → korst; `salt` in `#c:dusts/salt` en `#c:dusts`, niet `#c:gems`; zoutblok/gepolijst/trap/muur met houten pickaxe en Fortune III → zichzelf; plaat → 1, dubbel → 2; kristal met de hand, houten pickaxe, Silk Touch en Fortune III → zichzelf; vormen als item in `#stairs`/`#slabs`/`#walls`;
  - overleven: kristal `facing=north` aan steen blijft, steen weg → kristal weg; `facing=up` op korst blijft, korst weg → weg; `facing=down` onder zoutblok blijft, zoutblok weg → weg; `waterlogged=true` blijft;
  - korst, zoutblok en gepolijst zout boven lucht + buur-update → blijven staan (vallen niet);
  - licht in een dichte stenen doos: 3 op het kristal, 2 en 1 op 1 en 2 blokken afstand, 2 erboven;
  - flora breekt als de grond (zand/aarde/gras) door korst of zoutblok wordt vervangen: dorre struik, tonnetjescactus, woestijnbloem, palmzaailing, dode struik, klaproos, kort gras, eikenzaailing, cactus; suikerriet aan water op korst ook.
  - Server via RCON `stop` (wereld opgeslagen), geen draaiende server; RCON weer uit, `run/server.properties` op `level-name=smoke-change29`.

### Code review (`/code-review medium`)

Geen bugs gevonden (registry, datagen incl. blockstate-rotaties, loot, tags, itemmodel maar één keer geschreven, test en texturescript nagelopen tegen de gegenereerde output). Niets aangepast.

## In-game checklist (W)

W-achteraf (niet blokkerend; gebundeld in de ring 2-eindcheck, teststop 3 ná change 37). ~10 min. `./gradlew runClient`, nieuwe wereld **Corrupted World**, seed `20260930`, cheats aan, creatief. Testplek in de lucht boven spawn: `/fill -8 150 -8 8 150 8 minecraft:stone` en `/tp @s 0 151 0`.

1. **Creative tab:** direct na `cream_canyon_turquoise_ore` zoutkorst, zoutblok, gepolijst zout, gepolijste zouttrap, -plaat, -muur, zoutkristal; achteraan de losse items `soul_crystal`, `raw_sunstone`, `raw_turquoise`, `salt`. Namen en/nl kloppen (taal wisselen).
2. **Plaatsen:** alle blokken op het platform; geen paars-zwart. Zoutkorst: top met witte korstranden, zijkant zoutblok met een witte band bovenaan, bodem = zoutblok (blok omkeren met `/setblock` onder het platform en van onderen kijken). Muur verbindt met een vanilla-andesietmuur; trap (hoeken) en plaat (onder/boven/dubbel) kloppen.
3. **Kristal:** plaatsen op de vloer, tegen alle vier de wanden en tegen een plafond → steeds juist georiënteerd (punt van de steun af). In een waterbron plaatsen → waterlogged (water blijft). Steun weghalen → kristal breekt en dropt zichzelf. Kristal in de hand/GUI: plat, redelijk formaat (amethist-knop-transforms).
4. **Licht:** `/time set midnight`, dicht kamertje van steen met één kristal → F3 *Light* 3 op het kristal, 2 ernaast; in creatief geen monsters naast het kristal (`/difficulty easy`, even wachten in het donker rondom).
5. **Mijnen (survival):** `/gamemode survival`, `/give @s minecraft:iron_shovel`, `/give @s minecraft:wooden_pickaxe`, `/give @s minecraft:iron_shovel[minecraft:enchantments={levels:{"minecraft:silk_touch":1}}]`. Korst met de hand en met de schop (sneller) → 4 zout; met Silk Touch → korst. Zoutblok/gepolijst: met de hand → niets, met houten pickaxe → zichzelf. Kristal met de hand → zichzelf.
6. **Korst valt niet:** korst als plafond (3 hoog) bouwen, een blok eronder weghalen / eronder staan → korst blijft hangen.
7. **Flora:** in creatief dorre struik, woestijnbloem, palmzaailing, dode struik, cactus en een bloem op korst en zoutblok proberen te plaatsen → lukt niet.
8. **Geluiden:** korst klinkt als zand, zoutblok/gepolijst als calciet, kristal als amethist (ook een pijl erop: belgeluid, bekend). Definitief later.
9. **Item `salt`:** eigen plaatje (hoopje korrels), geen recept (verwacht, fase 2).
