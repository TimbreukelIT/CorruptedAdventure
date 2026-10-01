# Change 24 — Zonnesteen-erts en turkoois-erts

Scope: `blocks` — change 24

**Doel:** de twee ertsen van ring 2 bestaan als blokken: **zonnesteen** (hoofdmateriaal en kandidaat-gear-tier van ring 2, gloeiend, hoog in canyonwanden en hoodoos) en **turkoois** (zeldzame edelsteen, diep in kloofsteen). Per gastgesteente een eigen ertsblok (zoals vanilla `iron_ore` / `deepslate_iron_ore`), met tweelaags-model (bestaande bloktexture als basis + eigen overlay), placeholder-overlays van eigen pixels uit het ring 2-texturescript, loot (Fortune, Silk Touch), XP, mining-tags, conventietags (`c:ores`, …), lang (en + nl) en een plek in de creative tab. De drops zijn minimale placeholder-items (nut in fase 2). Plaatsing volgt in change 25 (`worldgen`), definitieve textures (emissive, puls) in change 37 (`textures`, `Ring2Textures.java`).

Bron: `fase-1-wereld.md` §2 Ring 2 → "Ores" (zonnesteen "gloeiend erts hoog in canyonwanden en hoodoos", turkoois "zeldzaam, diep in kloofsteen"), §1.4 (textures, stijl ertsen), §1.11 (ores algemeen), bouwplan rij 24; beslissingenlog 2026-09-30 "Ring 2-ores"; backlog regel 18 (zonnesteen-gear-tier, turkoois als handelswaar). Sjabloon: change 15 (`docs/plans/change-15.md`, zielkristal-erts).

---

## Afstemming (2026-10-01)

Afgestemd met 21, 23, 25, 30 en 37 (basis: de aanbevolen opties).
- **Plan 23 bestaat:** kloofsteenlagen in `canyonlands` van Y 8 tot het oppervlak (23 vraag 8 A), plateau Y 125–140 (23 vraag 5 B, keuze gebruiker), kloofbodem ~70; hoodoos (30 vraag 6 A) = kloofsteenlagen met een kaprots van `scorched_sandstone`. Vraag 1 A en 2 A passen daarop; risico's 1 en 2 zijn bij die antwoorden afgedekt.
- **Contract met 25 bevestigd:** één `minecraft:ore` per erts met per gastgesteente een `BlockMatchTest` → variant (plan 25 is aangepast; er komt geen los `sunstone_ore`/`turquoise_ore`-blok). Tellingen via `#c:ores/sunstone` / `#c:ores/turquoise`.
- **Vraag 2 B ↔ 25 vraag 6:** alleen bij 24-2 B (`deepslate_turquoise_ore`) kan 25 turkoois ook in diepleisteen leggen; anders vervallen 25-6 B/C.
- Placeholders `still` via de API van 21; de puls (met eigen gloeikleur) en de definitieve kleur komen in 37. De `emissive`-vlag op `oreWithOverlay` wordt hier aangemaakt; 37 gebruikt hem.
- **Drops (vraag 3 = B, keuze gebruiker, aangepast):** beide ertsen droppen een ruw materiaal: `raw_sunstone` en `raw_turquoise` (placeholder-items); tags `c:raw_materials/*` i.p.v. `c:gems/*`; smelten en ruwe opslagblokken naar de backlog (fase 2 `items`).
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37). Deze change mag gebouwd worden terwijl teststop 2 (terreincheck na change 23) loopt.

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17, commit `7714b77`). Changes 18–23 zijn nog niet gebouwd; hun plannen zijn de bron. **Bij de start de echte namen uit `CODEBASE.md` nalopen** en dit plan daarop aanpassen.

- **Erts-sjabloon uit change 15 (gebouwd):**
  - `registry/ModBlocks`: ertsen als `DropExperienceBlock(UniformInt.of(min, max), Properties.ofFullCopy(<vanilla-erts>)…)` via `register(name, supplier)` (blockitem komt vanzelf via `ModItems.ITEMS.registerSimpleBlockItem`). Registratievolgorde = creative-tab-volgorde.
  - `registry/ModItems`: losse items via `ITEMS.registerSimpleItem("…")`, **vóór** de blockitems geregistreerd (valkuil in `CODEBASE.md`); `ModCreativeTabs` sorteert zelf (eerst alle blokken, dan de losse items).
  - `datagen/ModBlockStateProvider.oreWithOverlay(Block, ResourceLocation particle, CubeTextures base, CubeTextures overlay)` + private record `CubeTextures(top, side, bottom)` met `all(...)`. De overlay is nu **altijd** emissive (`emissivity(15, 15).ao(false)`) en `render_type` cutout. De basis mag een eigen texture zijn (`modLoc`), niet alleen vanilla (`mcLoc`).
  - `datagen/ModBlockLootProvider`: `createOreDrop(block, item)` (Silk Touch → erts, anders 1 item met `ApplyBonusCount.addOreBonusCount` + `ApplyExplosionDecay`). `getKnownBlocks` = alle `ModBlocks`: elk blok een table.
  - `datagen/ModBlockTagsProvider` / `ModItemTagsProvider` (`copy`), `registry/ModTags` (`common(path)` voor `c:`-tags; nu `ORES_SOUL_CRYSTAL`, `GEMS_SOUL_CRYSTAL`), `c:ores` / `c:gems` via `addTag` op de sub-tag (NeoForge-conventie).
  - `datagen/ModItemModelProvider`: `blockItem(DeferredBlock)`, `basicItem(Item)`.
  - Lang: `EnglishLanguageProvider` / `DutchLanguageProvider` (`addBlock`, `addItem`).
  - Test `registry/ModBlocksTest`: elk blok heeft een blockitem met dezelfde naam; tab = blokken, dan losse items. Geldt vanzelf voor de nieuwe ertsen (ze hebben allemaal een item).
- **Change 21** (`docs/plans/change-21.md`, nog niet gebouwd) levert de gastgesteenten en het script:
  - blokken `red_canyon_stone`, `orange_canyon_stone`, `cream_canyon_stone` (de drie tinten bestaan in **alle** antwoordopties van vraag 1 van change 21) met textures `block/red_canyon_stone.png` enz.; `scorched_sandstone` met `block/scorched_sandstone_top.png`, `block/scorched_sandstone.png` (zijkant), `block/scorched_sandstone_bottom.png`;
  - tag `corruptedadventure:canyon_stones` (`ModTags.Blocks.CANYON_STONES`) = de drie tinten;
  - sectie `// ---- Ring 2 ----` in `ModBlocks` (ná Nether-ring 1);
  - **`tools/textures/Ring2Textures.java`** (zelfde opzet als `Ring1Textures`: `TEXTURES`, `still`/`pulsing`, vaste seed per bestandsnaam, `HAND_EDITED`, paletten bovenin, contactblad `build/texture-preview/ring2.png`). Deze change breidt dat script uit (afspraak: één script per ring).
  - Heet een blok of texture bij de bouw anders: de echte naam volgen en dat onder "Afwijkingen bij de bouw" noteren.
- **Change 22** (palm-set) staat achteraan de ring 2-sectie; de ertsen komen daarna (vraag-onafhankelijk).
- **Change 23** (ring 2-terrein; plan bestaat sinds 2026-10-01: kloofsteenlagen in `canyonlands` van Y 8 tot het oppervlak bij vraag 8 A, plateau Y 125–140 (23 vraag 5 B), kloofbodem ~70) bepaalt waar en hoe diep de kloofsteenlagen liggen. Deze change kiest alleen de **ertsblokken per gastgesteente**; diepte, aantallen en doelblokken zijn change 25. Zie Risico's: turkoois "diep in kloofsteen" vraagt kloofsteen op diepte.
- **Change 25** (plaatsing) gebruikt per gastgesteente een eigen `OreConfiguration.target(new BlockMatchTest(<gesteente>), <variant>)` in één `minecraft:ore`, zoals `NetherRing1Features` (netherrack → `soul_crystal_ore`, crimson nylium → crimson-variant). Een `TagMatchTest(canyon_stones)` kan niet per tint een andere variant kiezen. De registry-namen hieronder zijn het contract met change 25.
- **Change 37** (definitieve textures ring 2) vervangt alleen de PNG's onder dezelfde namen en voegt de puls toe (`.png.mcmeta`, stijl §1.4). Deze change legt het model al definitief aan.
- **Drop-items horen bij `items` (fase 2).** Volgens het precedent van `soul_crystal` (change 15) registreren we ze als **minimaal basis-item** (geen recipes, geen rarity) en noteren dat in de backlog. Vraag 3 = B: het zijn **ruwe** materialen (`raw_sunstone`, `raw_turquoise`), zoals vanilla `raw_iron`; het gesmolten product en het smeltrecept bestaan pas in fase 2.
- `RingLayout`/ringfunctie: niet nodig; de ertsen zijn los van de wereld testbaar.

---

## Plan

### 1. Registry-namen (contract met change 25 en 37)

Naamschema: `<gastgesteente>_<erts>_ore`, zoals vanilla `deepslate_iron_ore`; het ring 2-thema blijft herkenbaar (`canyon`, `scorched`). Lijst = aanbevolen opties (vraag 1, 2); bij een ander antwoord krimpt/groeit de lijst.

**Zonnesteen (vraag 1 A: 4 blokken)**

| Registry-naam | Basis-texture (model) | Properties |
|---|---|---|
| `red_canyon_sunstone_ore` | `corruptedadventure:block/red_canyon_stone` | `ofFullCopy(Blocks.IRON_ORE)` + `MapColor.TERRACOTTA_RED` + licht (vraag 5) |
| `orange_canyon_sunstone_ore` | `…/orange_canyon_stone` | idem, `MapColor.TERRACOTTA_ORANGE` |
| `cream_canyon_sunstone_ore` | `…/cream_canyon_stone` | idem, `MapColor.TERRACOTTA_WHITE` |
| `scorched_sandstone_sunstone_ore` | `…/scorched_sandstone_top` / `scorched_sandstone` / `_bottom` (`CubeTextures` top/side/bottom) | idem, `MapColor.TERRACOTTA_BROWN` |

**Turkoois (vraag 2 A: 3 blokken)**

| Registry-naam | Basis-texture | Properties |
|---|---|---|
| `red_canyon_turquoise_ore` | `…/red_canyon_stone` | `ofFullCopy(Blocks.IRON_ORE)` + `MapColor.TERRACOTTA_RED` |
| `orange_canyon_turquoise_ore` | `…/orange_canyon_stone` | idem, oranje |
| `cream_canyon_turquoise_ore` | `…/cream_canyon_stone` | idem, crème |
| (vraag 2 B) `deepslate_turquoise_ore` | `minecraft:block/deepslate` (+ `deepslate_top` boven/onder, zoals vanilla) | `ofFullCopy(Blocks.DEEPSLATE_IRON_ORE)` (hardheid 4,5, diepleisteen-geluid) |

- Hardheid 3,0 / 3,0 en stone-geluid via `ofFullCopy(IRON_ORE)` (een erts is harder dan zijn gastgesteente; kloofsteen = 1,5, zandsteen = 0,8). `requiresCorrectToolForDrops` zit in de kopie.
- XP en tool-tier: vraag 4 (aanbevolen: zonnesteen `UniformInt.of(2, 5)`, turkoois `UniformInt.of(3, 7)`, beide ijzeren pickaxe).
- Geen eigen block-classes: alles `DropExperienceBlock`.
- Plek in `ModBlocks`: achteraan de ring 2-sectie (ná de palm-set), eerst de zonnesteen-varianten, dan turkoois. Javadoc per erts kort (drop, licht), zoals bij `SOUL_CRYSTAL_ORE`.

### 2. Placeholder-items (`ModItems`, ná `soul_crystal`)

- Vraag 3 B (keuze gebruiker): `raw_sunstone` en `raw_turquoise`, beide `ITEMS.registerSimpleItem(...)`, javadoc "Placeholder raw material: smelting, product, recipes and balance follow in phase 2".
- Geen gesmolten product (bijv. zonnesteen-staaf/geslepen turkoois), geen smeltrecept en geen ruwe opslagblokken (`raw_sunstone_block`, `raw_turquoise_block`) in deze change → backlog.

### 3. Blockstates en models (`ModBlockStateProvider`)

- Alle varianten via `oreWithOverlay` met de basis als **verwijzing** naar de bestaande texture (kloofsteen/zandsteen via `modLoc`, diepleisteen via `mcLoc`), particle = basis (zijkant).
- Overlays (één per erts, gedeeld door alle gastgesteenten): `corruptedadventure:block/sunstone_ore_overlay`, `corruptedadventure:block/turquoise_ore_overlay`, op alle zijden (`CubeTextures.all`).
- **Emissive-vlag (vraag 5 A):** `oreWithOverlay` krijgt een parameter `boolean emissive`; alleen dan `emissivity(15, 15).ao(false)` op het overlay-element. Zielkristal en zonnesteen `true`, turkoois `false`. De bestaande aanroepen geven `true` mee; de gegenereerde zielkristal-models moeten **byte-identiek** blijven (check in test D).
- Item-models: `blockItem(...)` voor alle ertsen, `basicItem(...)` voor `raw_sunstone` en `raw_turquoise`.

### 4. Placeholder-textures (`tools/textures/Ring2Textures.java`)

Eerste eenvoudige versie, `still` (geen animatie; puls en definitieve stijl in change 37), vaste seed per bestandsnaam, alpha alleen 0/255 (cutout). **Werkpaletten, geen beslissing** (definitief in change 37), als constanten bovenin:

- zonnesteen (warm goud/oranje, "zon"): `5A2A08 9A4A0C D9801A F2B23A FFE07A FFF6C8` (omlijning → highlight);
- turkoois (blauwgroen met matrix-adertjes): `0E3B3A 1C6B66 2E9C92 4FC6B5 8FE6D6` + ader `3A2A1E`.

| Bestand | Voor | Opmerking |
|---|---|---|
| `block/sunstone_ore_overlay.png` | alle zonnesteen-varianten | 3–5 hoekige brokjes met 1-px donkere omlijning (kristalstijl §1.4); werkt op rood, oranje, crème en bruin |
| `block/turquoise_ore_overlay.png` | alle turkoois-varianten | 2–3 ronde, matte knollen met donkere adertjes (geen facetten: edelsteen-in-gesteente, geen kristal) |
| `item/raw_sunstone.png` | item | ruwe brok zoals vanilla `raw_iron` (onregelmatig, met gesteenteresten), gouden kern in hetzelfde palet |
| `item/raw_turquoise.png` | item | ruwe, ongepolijste knol met matrix-adertjes en gesteenteresten (geen geslepen edelsteen) |

Itemnamen volgens vraag 3 B: `raw_sunstone`, `raw_turquoise`. **Eerst het script draaien, dan `runData`** (`ExistingFileHelper`). Het contactblad `ring2.png` toont de overlays op alle gastgesteenten (nieuwe previewrij "ores": overlay over elke basis-texture, alleen in de preview, niet als PNG in de repo).

### 5. Loot (`ModBlockLootProvider`)

- Vraag 3 B: alle zonnesteen-varianten `createOreDrop(block, ModItems.RAW_SUNSTONE.get())`, alle turkoois-varianten `createOreDrop(block, ModItems.RAW_TURQUOISE.get())`: Silk Touch → de eigen variant, anders 1 ruw item (Fortune III: 1–4, `ore_drops`), explosion decay — zoals vanilla ijzererts → ruw ijzer.
- XP via `DropExperienceBlock` (Silk Touch geeft in 1.21 vanzelf 0 XP).

### 6. Tags (`ModTags`, `ModBlockTagsProvider`, `ModItemTagsProvider`)

Nieuwe `TagKey`s in `ModTags` (via `common(path)`): `Blocks/Items.ORES_SUNSTONE` = `c:ores/sunstone`, `Blocks/Items.ORES_TURQUOISE` = `c:ores/turquoise`; `Items.RAW_MATERIALS_SUNSTONE` = `c:raw_materials/sunstone`, `Items.RAW_MATERIALS_TURQUOISE` = `c:raw_materials/turquoise` (vraag 3 B; geen `c:gems/*`).

Blok-tags, alle varianten:
- `#minecraft:mineable/pickaxe`; tier-tag volgens vraag 4 (A: `#minecraft:needs_iron_tool` voor alles).
- `c:ores/sunstone` resp. `c:ores/turquoise`; `c:ores` krijgt beide via `addTag` (zoals zielkristal).
- `c:ore_rates/singular` (één ruw item per blok zonder Fortune).
- `c:ores_in_ground/deepslate` alleen voor `deepslate_turquoise_ore` (vraag 2 B). Kloofsteen en zandsteen hebben geen conventietag (zoals de crimson-variant: geen `ores_in_ground`).
- **Niet** in `#minecraft:overworld_carver_replaceables` (ertsfeatures draaien ná de carvers; vanilla zet daar alleen ijzer/koper in vanwege de ore veins uit de noise-stap) en niet in `#base_stone_overworld`.

Item-tags: `copy` van `c:ores/sunstone`, `c:ores/turquoise` (en `c:ores`, `c:ore_rates/*`, `c:ores_in_ground/deepslate` — die `copy`-regels bestaan deels al); `raw_sunstone` in `c:raw_materials/sunstone`, `raw_turquoise` in `c:raw_materials/turquoise`, beide sub-tags via `addTag` in `c:raw_materials` (vraag 3 B; NeoForge-conventie zoals `c:raw_materials/iron`; tagnamen bij de bouw verifiëren in `Tags.Items` van 21.1.252).

### 7. Lang (en + nl)

Commentaarblok "Ring 2 ores". Nederlandse terminologie volgt vanilla (`nl_nl` uit de asset-cache, valkuil in `CODEBASE.md`; vanilla zegt bijv. "Diepleisteen-ijzererts"). Voorstel (indicatief):

| Key | en_us | nl_nl |
|---|---|---|
| `red_/orange_/cream_canyon_sunstone_ore` | Red / Orange / Cream Canyon Sunstone Ore | Zonnesteenerts in rode / oranje / crèmekleurige kloofsteen |
| `scorched_sandstone_sunstone_ore` | Scorched Sandstone Sunstone Ore | Zonnesteenerts in geschroeide zandsteen |
| `red_/orange_/cream_canyon_turquoise_ore` | Red / Orange / Cream Canyon Turquoise Ore | Turkooiserts in rode / oranje / crèmekleurige kloofsteen |
| `deepslate_turquoise_ore` (vraag 2 B) | Deepslate Turquoise Ore | Diepleisteen-turkooiserts |
| `raw_sunstone`, `raw_turquoise` | Raw Sunstone, Raw Turquoise | Ruwe zonnesteen, Ruw turkoois (vanilla-patroon "Ruw ijzer") |

### 8. `./gradlew runData`; gegenereerde output meecommitten.

### 9. Documentatie

- `CODEBASE.md`: blok-tabel (ring 2-ertsen: class, XP, licht, loot, tags, model), item-tabel (`raw_sunstone`, `raw_turquoise`: placeholder, ruw materiaal), `ModTags` (nieuwe `c:`-tags), `oreWithOverlay` met `emissive`-vlag, textures-regel (`Ring2Textures.java`: overlays + items), registries-tabel (conventietags), "Laatst bijgewerkt".
- `fase-1-wereld.md` §2 Ring 2 → "Ores": **Uitwerking (change 24)** met varianten per gastgesteente, drop, tool-tier, XP, licht/gloed, model, en de overdracht naar change 23/25/30 (zie Risico's). Bij vraag 5 A: §1.4 aanvullen ("ertsen emissive, behalve edelstenen zonder gloed, zoals turkoois").
- `02-beslissingenlog.md`: de regels onderaan.
- `docs/backlog.md`:
  - regel 18 aanvullen of nieuwe regel: `[items] Placeholder-items raw_sunstone en raw_turquoise (ruwe drops van de ring 2-ertsen, change 24) zijn minimale basis-items zonder recipes of rarity; het gesmolten product (naam en vorm nog open), het smeltrecept (ruw materiaal en erts → product, oven/hoogoven), zonnesteen-gear-tier en turkoois als handelswaar in fase 2. (bron: change 24)`
  - `[items] Ruwe opslagblokken raw_sunstone_block en raw_turquoise_block (9× ruw materiaal, zoals raw_iron_block) en hun recipes; het blok zelf is blocks-werk in fase 2. (bron: change 24)`
  - eventueel: `[blocks] Kloofsteen-varianten van vanilla-ertsen (ijzer/kolen in kloofsteen), als change 25 vanilla-ores ook in de canyonlagen wil. (bron: change 24)` — alleen als de gebruiker dat bij vraag 1/2 aangeeft.
- Afsluiten met `/code-review`, bevindingen oplossen, commit `blocks: add sunstone and turquoise ores with loot and tags`.

**Niet in deze change:** plaatsing, aantallen en diepte (change 25), definitieve textures/puls (change 37), recipes (ook smelten), het gesmolten product, ruwe opslagblokken, gear (fase 2 / `items`), kloofsteen-varianten van vanilla-ertsen (alleen backlog als gewenst), particles.

---

## Client/server-impact

- Blokken en items zijn common registry-entries; NeoForge synct de registry-ID's bij het joinen. Geen eigen block-classes, geen netwerk, geen SavedData/attachments.
- Licht is een blockstate-property (beide kanten); in de overworld (`monster_spawn_block_light_limit` 0) voorkomt elk bloklicht > 0 monster-spawns op de blokken direct rond de ader (server-side, vanilla-regel).
- Models (cutout, emissive) en textures zijn resources: geen code in `client/`. Emissive is puur render-effect.
- Het texturescript staat buiten `src/` en komt niet in de jar.

---

## Tests

Bouwplan: **B, D**. Aanbevolen extra, goedkoop: **S** (nieuwe registry-entries) met RCON-checks van loot en tags.

- [ ] **Script** — `java tools/textures/Ring2Textures.java` draait; 4 nieuwe PNG's van 16×16, overlays alleen alpha 0/255; tweede run → `git status` toont geen wijzigingen; bestaande ring 2-PNG's (change 21/22) en `Ring1Textures.java` ongewijzigd.
- [ ] **B** — `./gradlew build` slaagt, incl. `ModBlocksTest` (nieuwe ertsen hebben een blockitem en staan in de tab vóór de losse items).
- [ ] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit:
  - blockstates, block-models (twee elementen, `render_type` cutout; overlay van zonnesteen met `neoforge_data` emissive, van turkoois **zonder**) en item-models van alle varianten + `raw_sunstone`, `raw_turquoise`;
  - de modellen `soul_crystal_ore.json` en `crimson_soul_crystal_ore.json` zijn **ongewijzigd** (`git diff` leeg) na de `emissive`-parameter;
  - loot tables met Silk Touch-tak, `apply_bonus` (`ore_drops`) en `explosion_decay`;
  - tags: `mineable/pickaxe`, `needs_iron_tool` (vraag 4), `c:ores` (bevat `#c:ores/sunstone`, `#c:ores/turquoise`), `c:ores/sunstone`, `c:ores/turquoise`, `c:ore_rates/singular`, (`c:ores_in_ground/deepslate` bij vraag 2 B), item-tegenhangers, `c:raw_materials/sunstone`, `c:raw_materials/turquoise` in `c:raw_materials` (geen `c:gems/*`);
  - en_us en nl_nl bevatten alle nieuwe keys.
- [ ] **S** — `tools/server-smoke-test.sh` met `level-name=smoke-change24`: server start zonder fouten of client-class-meldingen. Via RCON (werkwijze in `CODEBASE.md`, daarna RCON weer uit), per variant:
  - `setblock` + `execute if block <pos> #c:ores` en `#minecraft:needs_iron_tool` → slaagt;
  - `loot insert <kist> mine <pos> minecraft:iron_pickaxe` → 1 `raw_sunstone`/`raw_turquoise`; met Fortune III → 1–4; met Silk Touch → de eigen variant (`loot … mine` controleert de tool-tier niet en geeft geen XP).
- **Blokkerende handmatige test: nee.** Alles waar change 25 en 37 op bouwen (registry-namen, tags, loot, modelstructuur, texturebestandsnamen) is automatisch te controleren (build, runData, RCON). Uiterlijk is placeholder (definitief in change 37); tool-tier, XP en licht zijn balans en worden pas in context beoordeeld zodra change 25 de ertsen plaatst. → **W-achteraf**, in-game checklist (runClient, creative + survival):
  - [ ] Creative tab: ertsen achteraan de ring 2-blokken (ná palm), daarna bij de losse items `raw_sunstone` en `raw_turquoise` ná `soul_crystal`; namen en/nl.
  - [ ] Alle varianten zonder paars-zwart; overlay zonder z-fighting of zwarte randen; elke variant valt weg tussen zijn eigen gastgesteente (basis klopt, scorched-variant met top/zijkant/bodem).
  - [ ] Zonnesteen gloeit in het donker (emissive) en geeft licht (vraag 5); turkoois mat, geen licht.
  - [ ] Survival: stenen pickaxe → niets; ijzeren → ruw item + XP (vraag 4); Silk Touch → erts zonder XP.

---

## Risico's / aandachtspunten

- **Turkoois "diep in kloofsteen" hangt af van change 23.** Ligt kloofsteen alleen in de bovenste lagen van de canyonwanden, dan heeft "diep" weinig ruimte. Overdracht naar change 23: kloofsteenlagen reiken in `canyonlands` tot onder de kloofbodem (de turkoois-zone van change 25). Kan change 23 dat niet, dan vraag 2 B (diepleisteen-variant) of een later blocks-change voor een extra variant.
- **Zonnesteen in hoodoos hangt af van change 30.** Met vraag 1 A moeten de hoodoos van change 30 uit kloofsteen-tinten of `scorched_sandstone` bestaan; vanilla-zandsteen krijgt geen variant. Vastleggen in `fase-1-wereld.md` (Ring 2 → Ores) als randvoorwaarde voor change 30.
- **Aantal blokken:** 7 ertsblokken (8 bij vraag 2 B) voor 2 ertsen, maar alleen 2 overlays: het model hergebruikt de bestaande textures. Change 25 heeft per erts één `minecraft:ore` met één target per gastgesteente.
- **`emissive`-parameter** raakt de bestaande zielkristal-models: test D controleert dat ze byte-identiek blijven.
- **Werkpaletten op vier ondergronden:** een gouden overlay op crème en oranje kloofsteen kan wegvallen; de 1-px donkere omlijning houdt hem leesbaar. Definitief in change 37.
- **Drop-items zijn placeholders:** de namen `raw_sunstone`/`raw_turquoise` (vraag 3 B) leggen de fase 2-richting vast: beide worden gesmolten tot een product. Turkoois als "ruw" materiaal wijkt af van vanilla-edelstenen (diamant/smaragd droppen direct de steen); fase 2 kiest het product (bijv. geslepen turkoois). Hernoemen later breekt bestaande werelden.
- **Geen smeltrecept tot fase 2:** de ruwe items en Silk Touch-ertsen zijn tot dan alleen verzamel-/bouwmateriaal.
- **Licht en spawns:** licht op veel ertsblokken in de kloofwanden voorkomt lokaal monster-spawns (vraag 5 B sterker dan A).
- **Vanilla-ores in kloofsteen:** met change 21 vraag 3 A zit kloofsteen niet in `#base_stone_overworld`, dus vanilla-ertsen komen alleen in steen/diepleisteen onder de lagen. Wil change 25 vanilla-ores ook in de canyonlagen, dan zijn kloofsteen-varianten daarvan nodig (eigen blocks-change; backlog).

---

## Ontwerpvragen

### 1. In welke gesteenten komt zonnesteen voor (= welke ertsblokken)?
Ontwerp: "hoog in canyonwanden en hoodoos". De wanden zijn de drie kloofsteen-tinten (change 21); het materiaal van de hoodoos (change 30) ligt nog niet vast.
- **A. Drie kloofsteen-tinten + geschroeide zandsteen (aanbevolen).** 4 blokken, 1 overlay. Elke variant valt weg in zijn eigen laag; hoodoos (change 30) bouwen uit kloofsteen of geschroeide zandsteen en krijgen zo ook zonnesteen. Randvoorwaarde voor change 30.
- **B. Alleen de drie kloofsteen-tinten.** 3 blokken. Hoodoos moeten dan uit kloofsteenlagen bestaan (past bij echte hoodoos: gelaagd sedimentgesteente), anders zit er geen zonnesteen in.
- **C. Eén variant op rode kloofsteen.** 1 blok, minste werk; in oranje en crème lagen valt het erts op als rood vlak (zichtbaar, maar onnatuurlijk). Change 25 plaatst het dan alleen in rood of overal als "vervangt elke tint".
- **D. Als A + vanilla-zandsteen.** 5 blokken; houdt change 30 vrij om hoodoos van gewone zandsteen te maken.

Antwoord: **A** — drie kloofsteen-tinten + geschroeide zandsteen (4 blokken).

### 2. In welke gesteenten komt turkoois voor?
Ontwerp: "zeldzaam, diep in kloofsteen". Hoe diep kloofsteen reikt, beslist change 23.
- **A. De drie kloofsteen-tinten (aanbevolen).** 3 blokken, 1 overlay. Strikt volgens het ontwerp: turkoois hoort bij de canyons. Vraagt van change 23 kloofsteen tot onder de kloofbodem (overdracht, zie Risico's).
- **B. Drie tinten + diepleisteen (`deepslate_turquoise_ore`).** 4 blokken. Turkoois ook diep onder heel ring 2, los van hoe diep change 23 de lagen maakt; minder "canyon-eigen".
- **C. Alleen de diepste tint.** 1 blok. Kleinst, maar vraagt dat change 23 een vaste laagvolgorde heeft (bijv. crème onderaan).
- **D. Steen + diepleisteen, zoals vanilla.** 2 blokken. Eenvoudig te plaatsen, maar wijkt af van "in kloofsteen".

Antwoord: **A** — de drie kloofsteen-tinten (3 blokken); geen `deepslate_turquoise_ore`.

### 3. Wat laten de ertsen vallen?
In alle opties een placeholder-item (nut in fase 2) en Silk Touch → het erts.
- **A. Beide één edelsteen: `sunstone` en `turquoise` (Fortune III: 1–4) (aanbevolen).** Zoals diamant/smaragd/zielkristal; één loot-helper; zonnesteen-gear in fase 2 maakt rechtstreeks van de steen (zoals diamant). `c:gems/*`.
- **B. Zonnesteen als ruw materiaal `raw_sunstone`, turkoois edelsteen.** Zoals ijzer: in fase 2 smelten tot een staaf (meer recipes, ruwe opslagblokken); de naam legt de metaal-richting nu vast.
- **C. Zonnesteen meerdere scherven `sunstone_shard` (2–4, Fortune), turkoois edelsteen.** Zoals lapis/redstone: hoofdmateriaal stroomt sneller binnen; balans in fase 2 moet met grotere aantallen rekenen.

Antwoord: **B (keuze gebruiker, aangepast)** — **beide** ertsen laten een **ruw materiaal** vallen: placeholder-items `raw_sunstone` en `raw_turquoise` (Fortune werkt: 1–4 bij Fortune III; Silk Touch → het erts). Tags `c:raw_materials/sunstone` en `c:raw_materials/turquoise` (in `c:raw_materials`) i.p.v. `c:gems/*`. Smeltrecept en gesmolten product horen bij fase 2 (`items`) → backlog; geen ruwe opslagblokken (9×) nu → backlog.

### 4. Welke pickaxe is nodig, en hoeveel XP?
- **A. Beide ijzer (`needs_iron_tool`); XP zonnesteen 2–5, turkoois 3–7 (aanbevolen).** Gelijk aan zielkristal en vanilla diamant/smaragd; spelers uit ring 1 hebben ijzer. Zonnesteen (vaker) iets minder XP dan het zeldzame turkoois.
- **B. Zonnesteen steen (`needs_stone_tool`), turkoois ijzer.** Zonnesteen direct bereikbaar, ook als ijzer in ring 2 schaars is; een stap minder drempel voor het hoofdmateriaal.
- **C. Zonnesteen ijzer, turkoois diamant (`needs_diamond_tool`).** Turkoois als echt eindmateriaal van ring 2; wie geen diamant uit ring 1 meeneemt, kan het niet delven (diamant is in ring 2 zeldzaam).

Antwoord: **A** — beide ijzeren pickaxe (`needs_iron_tool`); XP zonnesteen 2–5, turkoois 3–7.

### 5. Licht en gloed?
Stijl §1.4: erts-overlays zijn emissive en pulseren (zielkristal: licht 3 + emissive). Zonnesteen heet "gloeiend".
- **A. Zonnesteen licht 3 + emissive overlay; turkoois geen licht en een matte overlay (aanbevolen).** Zonnesteen = zichtbaar klimdoel in de wanden, ook 's nachts; turkoois blijft echt zeldzaam en moet gezocht worden. `oreWithOverlay` krijgt een `emissive`-vlag; §1.4 krijgt een uitzondering voor edelstenen zonder gloed.
- **B. Als A, maar zonnesteen licht 7.** Aders verlichten de kloof zichtbaar; minder monster-spawns rond de aders (overworld: elk bloklicht blokkeert spawns).
- **C. Beide als zielkristal: licht 3 + emissive.** Eén regel voor alle ertsen, geen modelparameter; turkoois valt daardoor even sterk op als zonnesteen.
- **D. Geen bloklicht; alleen zonnesteen emissive.** Gloed alleen als render-effect, geen invloed op spawns of licht in de wereld.

Antwoord: **A** — zonnesteen licht 3 + emissive overlay; turkoois geen licht, matte overlay (`emissive`-vlag op `oreWithOverlay`).

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | blocks | Zonnesteen-erts per gastgesteente: drie kloofsteen-tinten + geschroeide zandsteen | Elke variant valt weg in zijn laag; hoodoos krijgen ook zonnesteen |
| 2026-10-01 | blocks | Turkoois-erts per gastgesteente: de drie kloofsteen-tinten | Strikt "diep in kloofsteen" |
| 2026-10-01 | blocks | Drops ring 2-ertsen (keuze gebruiker): ruwe materialen `raw_sunstone` en `raw_turquoise` (Fortune, Silk Touch → erts), tags `c:raw_materials/*`; placeholder-items, smelten/product/ruwe blokken in fase 2 | Metaal-achtige verwerking voor beide ring 2-materialen |
| 2026-10-01 | blocks | Tool-tier en XP ring 2-ertsen: beide ijzer; XP zonnesteen 2–5, turkoois 3–7 | Gelijk aan zielkristal en vanilla diamant/smaragd |
| 2026-10-01 | blocks | Licht/gloed ring 2-ertsen: zonnesteen licht 3 + emissive, turkoois mat zonder licht | Zonnesteen als zichtbaar klimdoel; turkoois blijft zoekwerk |
| 2026-10-01 | blocks | Ertsnamen `<gastgesteente>_<erts>_ore`; één overlay per erts, gedeeld door alle gastgesteenten; plaatsing per gesteente via `BlockMatchTest` (change 25) | Zoals vanilla `deepslate_*_ore`; weinig textures, het erts valt weg in zijn eigen laag |

Commit (na `/code-review`): `blocks: add sunstone and turquoise ores with loot and tags`.
