# Change 10 — Dood gras, verdorde aarde en de dood-hout-set

Scope: `blocks` — change 10

**Doel:** de unieke ring 1-blokken bestaan als speelbare blokken: dood gras en verdorde aarde (grond voor kale plekken en `withered_expanse`) en een complete dood-hout-set (log, wood, planks + houtset, met gestripte varianten). Met blockstates/models, eenvoudige placeholder-textures uit een eigen generatorscript (eigen pixels, geen vanilla-pixels), loot tables, tags, lang (en + nl) en een plek in de eigen creative tab. Plaatsing in de wereld volgt in change 11 (`withered_expanse`) en 12 (kale plekken met dode bomen); definitieve textures in change 17.

Bron: `fase-1-wereld.md` §2 Ring 1 ("Dood gras / verdorde aarde, dood hout (log + planks) voor kale plekken en het overgangsbiome"), §1.4 (eerst getinte vanilla-placeholders), §2 Ring 2 (definitie "houtset" bij de palm-set), bouwplan rij 10.

---

## Afhankelijkheden en aannames

- **Changes 0–7 klaar**, 8 en 9 gebouwd zoals gepland. Beide zijn `worldgen` en raken geen blok-/item-registries. Change 8 voegt waarschijnlijk datagen toe aan `DataGenerators` (biomes, biome-tags); deze change hangt daar alleen extra providers naast.
- **Er bestaan nog geen** `ModBlocks`, `ModItems`, `ModCreativeTabs`, blockstate-/model-/loot-/tag-providers of textures. Deze change legt die basis; change 15 (zielkristal-erts) en latere blok-changes bouwen erop voort (zie `docs/plans/change-15.md`).
- **"Houtset" is al gedefinieerd** in §2 Ring 2 (palm-set): deur, valluik, hek, poort, trap, plaat (slab), knop, drukplaat. Dus **geen borden, hangborden of boot** (borden vragen een geregistreerde `WoodType`, block entities en client-rendering; een boot is item + entity). Staat als "later eventueel" in de backlog.
- **Geen bladeren en geen sapling:** dode bomen zijn bladloos en ontstaan alleen via worldgen (change 12).
- **Recipes horen bij `items` (fase 2)** → backlog. Vanilla-recipes die op tags werken (stokken, werkbank, kist, houten tools, houtskool uit `#logs_that_burn`) werken wel meteen dankzij de tags.
- **Brandbaarheid: zoals vanilla eikenhout** (log/wood 5/5, planks en houtset 5/20, via `FireBlock`). Niet in de docs vastgelegd; als je dood hout extra brandbaar wilt (droog, ring 2-sfeer), zeg het bij de start.
- **Textures krijgen hun definitieve bestandsnamen** al in deze change, zodat change 17 alleen PNG's vervangt (zie `docs/plans/change-17.md`).
- **Afwijking van §1.4 (besloten 2026-09-30, vraag 3):** placeholders zijn **geen getinte vanilla-textures** maar eigen pixels uit een eerste, eenvoudige versie van het generatorscript `tools/textures/Ring1Textures.java` uit het plan van change 17. Change 17 verfijnt dat script tot de definitieve textures (palet, stijl, contactblad, `handEdited`-lijst). §1.4 en de beslissingenlog worden daarop aangepast.
- Registry-namen met het ring 1-thema `dead_` / `withered_`.

---

## Plan

### 1. Registries (`registry/`)
- **`ModBlocks`** — `DeferredRegister.Blocks`; helper `register(name, supplier)` die ook het `BlockItem` in `ModItems` registreert (deur: `DoubleHighBlockItem`). Registratie-volgorde = volgorde in de creative tab.
- **`ModItems`** — `DeferredRegister.Items`; nu alleen blockitems. Losse items volgen in fase 2.
- **`ModCreativeTabs`** — `DeferredRegister<CreativeModeTab>` met **één eigen mod-tab, per ring gesorteerd** (vraag 4). Blokken van ring 1 eerst, latere ringen erachter; binnen een ring de volgorde van registratie (grond, dan houtset in vanilla-volgorde). Mod-blokken komen niet in de vanilla-tabs. Tab `corruptedadventure:main`, titel-key `itemGroup.corruptedadventure.main`, icoon `dead_log`, inhoud = alle `ModItems`-entries in registratievolgorde.
- Aanhaken in `CorruptedAdventure` (mod-bus), naast de bestaande registers.

### 2. Blokken

| Registry-naam | Class | Basis-properties (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `dead_grass_block` | `block/DeadGrassBlock` | `grass_block`, eigen `MapColor` | `snowy`-state; random ticks; loot → `withered_dirt` (Silk Touch → zichzelf) |
| `withered_dirt` | `block/WitheredDirtBlock` | `dirt` | schop → pad, schoffel → farmland |
| `dead_log` | `block/StrippableLogBlock` | `oak_log` | as-state; bijl → `stripped_dead_log` |
| `dead_wood` | `block/StrippableLogBlock` | `oak_wood` | bijl → `stripped_dead_wood` |
| `stripped_dead_log` | `RotatedPillarBlock` | `stripped_oak_log` | |
| `stripped_dead_wood` | `RotatedPillarBlock` | `stripped_oak_wood` | |
| `dead_planks` | `Block` | `oak_planks` | |
| `dead_stairs` | `StairBlock` | `oak_stairs` | basis = `dead_planks` |
| `dead_slab` | `SlabBlock` | `oak_slab` | |
| `dead_fence` | `FenceBlock` | `oak_fence` | |
| `dead_fence_gate` | `FenceGateBlock` | `oak_fence_gate` | `WoodType` `DEAD` |
| `dead_door` | `DoorBlock` | `oak_door` | `BlockSetType` `DEAD` |
| `dead_trapdoor` | `TrapDoorBlock` | `oak_trapdoor` | `BlockSetType` `DEAD` |
| `dead_button` | `ButtonBlock` | `oak_button` | 30 ticks, `BlockSetType` `DEAD` |
| `dead_pressure_plate` | `PressurePlateBlock` | `oak_pressure_plate` | `BlockSetType` `DEAD` |

- **`block/ModWoodTypes`** — `BlockSetType DEAD_SET` (`corruptedadventure:dead`, standaard houtgeluiden) en `WoodType DEAD`. Later komen palm, druipzwam enz. hier ook in. **Niet** `WoodType.register(...)` aanroepen: dat meldt de soort aan voor bord-materialen op de client, en zonder bord-textures kan dat misgaan. `FenceGateBlock` gebruikt de `WoodType` alleen voor geluid.
- **Gedrag (vraag 1 A en 2 A):**
  - `StrippableLogBlock extends RotatedPillarBlock`: override `getToolModifiedState(state, context, ItemAbilities.AXE_STRIP, simulate)` → stripped-variant (via `Supplier<Block>`) met dezelfde `axis`. Herbruikbaar voor alle latere houtsets.
  - `DeadGrassBlock extends SnowyDirtBlock` (gedraagt zich als gras): eigen `randomTick` naar het voorbeeld van `SpreadingSnowyDirtBlock` (vanilla hardcodet `Blocks.DIRT`/`GRASS_BLOCK`, dus niet overerven). Bedekt/geen licht → `withered_dirt`; genoeg licht → spreidt alleen naar `withered_dirt`. Vanilla-gras en dood gras beïnvloeden elkaar niet. `getToolModifiedState`: schop → `dirt_path`, schoffel → `farmland` (alleen als erboven lucht is, zoals vanilla).
  - `WitheredDirtBlock`: alleen `getToolModifiedState` (schop → `dirt_path`, schoffel → `farmland`).
- **Brandbaarheid:** vanilla's `FireBlock#setFlammable` is private. Keuze bij de bouw: één AT-regel in de bestaande `accesstransformer.cfg` + aanroep in `FMLCommonSetupEvent#enqueueWork` (één klein class `block/ModFlammables`), in plaats van brandbaarheids-overrides in elke blokclass. De AT is een minimale placeholder in een `blocks`-change → in de backlog bij het bestaande AT-punt (change 6).

### 3. Placeholder-textures: generatorscript met eigen pixels (vraag 3)
**Eerste, eenvoudige versie** van het script uit `docs/plans/change-17.md` §2 A, met dezelfde naam en opzet, zodat change 17 het alleen hoeft te verfijnen:
- **`tools/textures/Ring1Textures.java`**: single-file Java-programma, draait met `java tools/textures/Ring1Textures.java` vanuit de repo-root. Alleen JDK (`BufferedImage` + `ImageIO`, headless), geen Gradle-koppeling, staat buiten `src/` en komt dus niet in de jar.
- **Eigen pixels, geen vanilla-pixels:** het script leest geen vanilla-bestanden. Vanilla dient hooguit als visuele referentie voor de indeling (waar de plank-naden zitten, hoe een deur is opgebouwd).
- **Deterministisch:** vaste seed per texture (bijv. hash van de bestandsnaam) en `java.util.Random(seed)`. Twee keer draaien geeft byte-identieke PNG's.
- **Eenvoudige opzet (change 10):** per materiaal een klein vast palet (3–4 tinten, als constanten bovenin het script), seeded waarderuis voor korrel en per soort een basispatroon:
  - aarde: ruis over het palet;
  - grasbovenkant: vaal geelgrijs met donkere sprietjes;
  - graszijkant: aarde met een onregelmatige grasrand (2–4 px) die over de rand hangt;
  - sneeuwzijkant: dezelfde aarde met een witte rand;
  - log-zijkant: verticale nerf met schorsbarsten;
  - log-kopse kant: jaarringen met een schorsrand;
  - stripped: lichter hout met nerf, zonder schors;
  - planken: 4 horizontale planken met naden en versprongen stoten;
  - deur en valluik: planken-frame met een paar gaten (alpha alleen 0/255);
  - deur-item: 16×16-silhouet van de deur.
- **Nog niet in change 10** (hoort bij change 17): het definitieve palet en de stijl in §1.4, het contactblad in `build/texture-preview/`, de `handEdited`-lijst en de verfijnde patronen. Deze PNG's zijn werkversies.
- Schrijft direct naar `src/main/resources/assets/corruptedadventure/textures/`; de PNG's worden gecommit.

| Bestand (definitieve naam, zie change 17 §1) | Patroon |
|---|---|
| `block/withered_dirt.png` | aarde (grijsbruin, droog) |
| `block/dead_grass_block_top.png` | vaal gras, vaste kleur (geen biome-tint) |
| `block/dead_grass_block_side.png` | aarde + grasrand, alles in één PNG (geen overlay-model) |
| `block/dead_grass_block_snow.png` | aarde + sneeuwrand (voor `snowy=true`) |
| `block/dead_log.png`, `block/dead_log_top.png` | schors met nerf / jaarringen |
| `block/stripped_dead_log.png`, `block/stripped_dead_log_top.png` | gestript hout / jaarringen zonder schors |
| `block/dead_planks.png` | 4 planken |
| `block/dead_door_top.png`, `block/dead_door_bottom.png` | deurhelften, cutout |
| `block/dead_trapdoor.png` | valluik, cutout |
| `item/dead_door.png` | deur-item |

- `dead_wood` en `stripped_dead_wood` gebruiken de log-zijkant op alle zijden; trap/plaat/hek/poort/knop/drukplaat gebruiken `dead_planks`. Dat zijn 13 PNG's, gelijk aan de kernlijst van change 17 (zonder zielkristal).
- **Geen tintindex en geen `BlockColor`** → geen client-code in deze change. De blockmodels verwijzen alleen naar eigen textures (ook de sneeuwvariant, niet naar `minecraft:block/grass_block_snow`).
- Volgorde: **eerst het script draaien, dan `runData`**. De model-providers controleren via `ExistingFileHelper` (`--existing src/main/resources`) of de textures bestaan.
- Change 15 (zielkristal) breidt hetzelfde script uit in plaats van vanilla te tinten. Die aanname in `change-15.md` wijkt dus af; zie het risico hieronder.

### 4. Datagen (`datagen/`, aanhaken in `DataGenerators`)
- **`ModBlockStateProvider`** (blockstates + blockmodels): `logBlock`, `axisBlock` (wood), `simpleBlock`, `stairsBlock`, `slabBlock`, `fenceBlock`, `fenceGateBlock`, `doorBlockWithRenderType(..., "cutout")`, `trapdoorBlockWithRenderType(..., true, "cutout")`, `buttonBlock`, `pressurePlateBlock`; `dead_grass_block` met `cube_bottom_top` (top / side / bottom = `withered_dirt`) plus de `snowy`-variant. Render type in de model-JSON (NeoForge `render_type`), dus geen client-code.
- **`ModItemModelProvider`**: blockitems met parent op het blockmodel; hek → `fenceInventory`, knop → `buttonInventory`, valluik → `dead_trapdoor_bottom`, deur → plat item (`item/dead_door`).
- **`ModBlockLootProvider`** (`LootTableProvider` + `BlockLootSubProvider`): `dropSelf` voor alles, behalve `dead_slab` (`createSlabItemTable`), `dead_door` (`createDoorTable`), `dead_grass_block` (`createSingleItemTableWithSilkTouch(..., withered_dirt)`). `getKnownBlocks` = alle `ModBlocks`-entries, dus datagen faalt als een blok geen loot table heeft.
- **`ModBlockTagsProvider`**:
  - `corruptedadventure:dead_logs` = `dead_log`, `dead_wood` (+ stripped); in `minecraft:logs_that_burn` (en daarmee `#logs`).
  - `minecraft:planks`, `wooden_stairs`, `wooden_slabs`, `wooden_fences`, `fence_gates`, `wooden_doors`, `wooden_trapdoors`, `wooden_buttons`, `wooden_pressure_plates`.
  - `minecraft:mineable/axe` (hele houtset), `minecraft:mineable/shovel` (gras, aarde).
  - `minecraft:dirt` (gras, aarde): planten, dode struiken en saplings mogen erop; carvers (`#overworld_carver_replaceables` bevat `#dirt`) snijden erdoor. `minecraft:enderman_holdable` zoals vanilla-gras/aarde.
  - Compat (`c:`): `c:stripped_logs`, `c:stripped_woods`, `c:fences/wooden`, `c:fence_gates/wooden`.
  - **Niet** in `minecraft:animals_spawnable_on` → geen dieren op dood gras (past bij "dood"). Spawnregels horen bij `mobs`; alleen noteren.
- **`ModItemTagsProvider`**: `copy` van alle bovenstaande tags naar item-tags (incl. `corruptedadventure:dead_logs`, `minecraft:logs_that_burn`, `minecraft:planks`, `minecraft:dirt`).
- **Lang** (`EnglishLanguageProvider`, `DutchLanguageProvider`):

| Key | en_us | nl_nl |
|---|---|---|
| `dead_grass_block` | Dead Grass Block | Dood grasblok |
| `withered_dirt` | Withered Dirt | Verdorde aarde |
| `dead_log` / `dead_wood` | Dead Log / Dead Wood | Doodhouten stam / Doodhout |
| `stripped_dead_log` / `_wood` | Stripped Dead Log / Wood | Gestripte doodhouten stam / Gestript doodhout |
| `dead_planks` | Dead Planks | Doodhouten planken |
| `dead_stairs` / `_slab` | Dead Stairs / Slab | Doodhouten trap / plaat |
| `dead_fence` / `_fence_gate` | Dead Fence / Fence Gate | Doodhouten hek / poort |
| `dead_door` / `_trapdoor` | Dead Door / Trapdoor | Doodhouten deur / valluik |
| `dead_button` / `_pressure_plate` | Dead Button / Pressure Plate | Doodhouten knop / drukplaat |
| `itemGroup.corruptedadventure.main` | The Corrupted Adventure | The Corrupted Adventure |

- Tags- en loot-providers in NeoForge 21.1 krijgen `lookupProvider` (+ `ExistingFileHelper` voor tags/models). Bij de bouw nagaan wat `GatherDataEvent` in 21.1.252 biedt (`createProvider` wordt al gebruikt; de item-tags-provider heeft de `contentsGetter()` van de block-tags-provider nodig).

### 5. Documentatie
- `CODEBASE.md`: `block/`, `ModBlocks`/`ModItems`/`ModCreativeTabs`, `ModWoodTypes`, nieuwe datagen-providers, blok-/itemtabel, textures ✅ (lijst), `tools/textures/Ring1Textures.java` onder tooling (hoe draaien), eventuele AT-regel; valkuilen (eerst script, dan `runData`; cutout alleen alpha 0/255; `WoodType` niet registreren zonder borden).
- `fase-1-wereld.md`:
  - §1.4: productiemethode is nu **eigen pixels uit een deterministisch generatorscript, geen vanilla-pixels in de repo**; ook placeholders. Dit vervangt "getinte/gerecolorde vanilla-textures".
  - Ring 2 / houtset: definitie "houtset" plus gestripte log en wood als vast onderdeel van elke houtset (sjabloon voor palm, druipzwam, verkoold, rijpzwam, cipres).
  - Ring 1: gedrag van dood gras.
  - §1 of techniek: één eigen creative tab, per ring gesorteerd.
- `02-beslissingenlog.md`, vier regels (2026-09-30):
  - `blocks`: dood gras gedraagt zich als gras;
  - `blocks`: elke houtset krijgt gestripte varianten;
  - `textures`: eigen pixels via script, geen vanilla-pixels (vervangt de eerdere regel over getinte vanilla-placeholders);
  - `infra`/`blocks`: één eigen creative tab, per ring gesorteerd.
- `docs/backlog.md`:
  - `[items]` Recipes dood-hout-set (log → planks, wood, trap, plaat, hek, poort, deur, valluik, knop, drukplaat); generieke vanilla-recipes werken al via tags. (bron: change 10)
  - `[items]` Brandstofwaarde van hek/poort nagaan (vanilla noemt die per blok, niet per tag, in de furnace-fuel-lijst / NeoForge data map `furnace_fuels`). (bron: change 10)
  - `[blocks]` Borden/hangborden en `[items]` boten voor houtsets: vallen buiten de houtset-definitie; eventueel later. (bron: change 10)
  - `[infra]` AT-regel `FireBlock#setFlammable` aanvullen bij het bestaande AT-punt (change 6). (bron: change 10)
  - `[textures]` `tools/textures/Ring1Textures.java` is in change 10 als eenvoudige eerste versie gebouwd; change 17 verfijnt palet, stijl en patronen en voegt het contactblad en de `handEdited`-lijst toe. Het script hangt niet in Gradle. (bron: change 10)
  - `[corruption]` Idee: dood gras dat naar vanilla-gras spreidt ("groeiende dode plekken"). (bron: change 10)

---

## Client/server-impact
- Blokken en items zijn common registries; NeoForge synct registry-ID's bij het joinen. Client en server moeten de mod allebei hebben (was al zo).
- Gedrag (random ticks van dood gras, strippen, schop/schoffel) draait in common code; `getToolModifiedState` wordt op client (voorspelling, `simulate`) en server aangeroepen, zonder client-classes.
- **Geen client-code:** render type via model-JSON, kleuren in de PNG's, geen `BlockColor`/`ItemColor`. Geen netwerk, geen SavedData.
- Het texturescript staat buiten `src/` en draait nooit in het spel. De PNG's zijn assets die alleen de client laadt.
- Brandbaarheid wordt in common setup gezet (beide kanten, alleen serverzijde relevant).

## Tests
- **Script** — `java tools/textures/Ring1Textures.java` draait zonder fouten. Tweede keer draaien → `git status` toont geen gewijzigde PNG's (deterministisch). Alle 13 bestanden zijn 16×16; deur en valluik hebben alleen alpha 0/255.
- **B** — `./gradlew build` slaagt (incl. bestaande unit tests).
- **D** — `./gradlew runData` slaagt; loot-validatie (elk blok een table) en model-providers zonder "texture does not exist"; output gecommit (`blockstates/`, `models/`, `loot_table/blocks/`, `tags/block|item/`, lang).
- **S** (aanbevolen, niet in de bouwplanrij): het gaat om gameplay-gedrag (random ticks, tool-interacties) → `tools/server-smoke-test.sh`: server start zonder fouten of client-class-meldingen.
- Optioneel unit test: elk `ModBlocks`-blok heeft een `BlockItem` en staat in de creative-tab-lijst.
- **In-game checklist (runClient, creative + survival):**
  - [ ] Creative tab "The Corrupted Adventure" bestaat, met alle blokken in logische volgorde (grond, dan houtset); namen in en_us en nl_nl; de blokken staan niet in de vanilla-tabs.
  - [ ] Elk blok plaatsbaar, geen paars-zwart; textures zijn eigen (eenvoudige) pixels en herkenbaar als aarde, gras, schors, planken; log/wood draaien mee met de as; wood heeft rondom schors.
  - [ ] Deur en valluik: transparantie klopt (cutout), openen met de hand, geluid; knop en drukplaat geven redstone.
  - [ ] Hek verbindt met eikenhouten hek en poort (`#wooden_fences`); poort opent.
  - [ ] Bijl op log/wood → stripped-variant, as behouden.
  - [ ] Loot (survival): alles laat zichzelf vallen; dubbele plaat → 2; deur → 1 (ook bij het breken van de bovenhelft); dood gras → verdorde aarde, met Silk Touch → dood gras.
  - [ ] Tags: F3 op het blok toont `#minecraft:logs`/`#minecraft:planks`/`#minecraft:dirt`; werkbank en stokken van doodhouten planken; houtskool uit een dode stam; planken branden als brandstof.
  - [ ] Dood gras: blok erop → wordt verdorde aarde; verdorde aarde naast dood gras in het licht → wordt dood gras (random tick, eventueel `/gamerule randomTickSpeed 100`); vanilla-gras spreidt niet naar verdorde aarde; schop → pad, schoffel → farmland; sneeuwlaag erop → sneeuwzijkant.
  - [ ] Vuur: planken en stam vatten vlam en branden op.

## Risico's / aandachtspunten
- **Kwaliteit van de eerste scriptversie:** eenvoudige patronen kunnen er als "programmer art" uitzien. Dat is genoeg voor fase-1-tests. Houd de tijd erin beperkt: palet- en stijlkeuzes horen bij de sparring van change 17, niet bij deze change.
- **Scriptgrens met change 17:** change 17 gaat uit van hetzelfde bestand. Houd de structuur zoals change 17 §2 beschrijft (per texture een tekenmethode, gedeelde helpers voor ruis en palet in hetzelfde bestand), zodat change 17 verfijnt in plaats van herschrijft.
- **Change 15 gaat nog uit van getinte vanilla** (`change-15.md` stap 5). Door het besluit van vraag 3 moet change 15 het zielkristal-erts ook via het script maken, met eigen pixels. Als onderlaag van het erts kan het model wel naar `minecraft:block/netherrack` verwijzen (dat is geen kopie, zie change 17 §4). Melden aan wie change 15 bouwt.
- **Geen vanilla-pixels:** ook niet via een omweg. Het script leest geen vanilla-jar, en blockmodels verwijzen niet naar vanilla-textures voor de eigen blokken (de sneeuwzijkant is een eigen PNG).
- **`FireBlock#setFlammable`** is (naar verwachting) private → AT-regel nodig; alternatief is overrides in elke blokclass (veel classes). Controleren bij de bouw.
- **`WoodType.register`** niet aanroepen zonder bord-textures (client bouwt dan bord-materialen voor een soort zonder textures). `BlockSetType.register` is wel veilig.
- **Farmland van verdorde aarde** droogt uit tot **vanilla-aarde** (`FarmBlock.turnToDirt` hardcodet `Blocks.DIRT`): een speler kan een dode plek zo "genezen". Acceptabel voor fase 1; anders een eigen farmland (buiten scope).
- **`#minecraft:dirt`** laat ook vanilla-planten, bamboe en saplings op de dode grond groeien. Als dat niet mag, dan een eigen tag i.p.v. `#dirt`, maar dan snijden carvers er alleen nog doorheen via een extra tag.
- **Deze change is het sjabloon** voor alle latere blok-changes (15, 21, 22, …): namen van helpers en providers bewust kiezen en in `CODEBASE.md` zetten, want change 15 en 17 gaan ervan uit (zie hun plannen).
- De data-run heeft al `--existing src/main/resources` (`build.gradle` r. 81): nodig voor de texturecheck.

## Open ontwerpvragen

Alle vier beantwoord op 2026-09-30; het plan hierboven is daarop aangepast.

### 1. Hoe gedraagt dood gras zich?
- **A. Gras-familie (aanbevolen).** Dood gras wordt verdorde aarde als het bedekt raakt, spreidt in het licht naar verdorde aarde (niet naar vanilla-aarde), heeft een sneeuwstate; schop → pad, schoffel → farmland op beide blokken. Twee kleine classes met random ticks (goedkoop, server-side). Voelt vertrouwd; dode plekken blijven dood maar groeien niet.
- **B. Alleen terugvallen.** Bedekt → verdorde aarde, schop/schoffel werkt, maar geen spreiding. Iets minder code; dode grond wordt nooit meer "gras", ook niet naast dood gras.
- **C. Statisch.** Geen random ticks, geen tool-interacties; gras blijft gras onder een blok. Minste code, maar voelt onaf (schop en schoffel doen niets).
- (Spreiding naar vanilla-gras, "groeiende dode plekken", hoort bij `corruption`, fase 5 → alleen als backlog-idee.)

Antwoord (2026-09-30): **A**. Dood gras gedraagt zich als gras (`DeadGrassBlock` + `WitheredDirtBlock`).

### 2. Krijgt de houtset gestripte varianten?
- **A. Ja: `stripped_dead_log` + `stripped_dead_wood` (aanbevolen).** Bijl werkt zoals bij vanilla; één herbruikbare `StrippableLogBlock`. Wordt het sjabloon voor alle houtsets (+2 blokken en 2 textures per set).
- **B. Nee.** Minder blokken en textures, maar een bijl op een dode stam doet niets (wijkt af van wat spelers verwachten). Ook voor latere sets.

Antwoord (2026-09-30): **A**. Ja, via `StrippableLogBlock`; sjabloon voor alle houtsets.

### 3. Hoe maken we de getinte placeholder-textures?
- **A. Klein Java-script in `tools/` (aanbevolen)** (`java tools/PlaceholderTextures.java <client-jar>`, alleen JDK/`javax.imageio`, geen extra installatie). Leest vanilla-PNG's uit de client-jar, tint/voegt samen, schrijft naar `textures/`; PNG's worden gecommit. Herhaalbaar en herbruikbaar voor change 15, 21, 22, …; is wel een stukje tooling (`infra`) als minimale placeholder.
- **B. Eenmalig met de hand** (beeldbewerker, door jou of via een wegwerp-script). Geen tooling in de repo, maar niet reproduceerbaar; elke volgende blok-change opnieuw handwerk.
- **C. Geen eigen PNG's: vanilla-textures + tint via `tintindex` en een `BlockColor` in `client/`.** Geen afgeleide Mojang-bestanden in de repo, maar eigen getinte parent-models voor elk vorm-type (trap, hek, deur…), client-code en test S; alles vervalt weer in change 17.

Antwoord (2026-09-30): **anders dan A–C: eigen pixels, geen vanilla-pixels in de repo.** Change 10 bouwt een eerste, eenvoudige versie van het generatorscript uit het plan van change 17 (`tools/textures/Ring1Textures.java`, alleen JDK, deterministisch, eigen pixels). Change 17 verfijnt dat script tot de definitieve textures. Uitgewerkt in Plan §3.

### 4. Welke creative tab(s)?
- **A. Eén eigen mod-tab "The Corrupted Adventure" (aanbevolen)**, per ring gesorteerd (volgorde van registratie). Alles makkelijk te vinden bij het testen; één plek voor alle latere blokken en items.
- **B. Eigen tab + ook in de vanilla-tabs** (bouwblokken, natuurlijke blokken, redstone) via `BuildCreativeModeTabContentsEvent`. Voelt het meest vanilla voor spelers; iets meer werk per blok (juiste vanilla-tab en positie kiezen).
- **C. Alleen vanilla-tabs.** Geen eigen tab; mod-blokken lastiger terug te vinden tussen vanilla-blokken.
- **D. Eén tab per ring.** Overzichtelijk bij veel blokken, maar vijf-plus tabs; eigenaarschap per ring-change.

Antwoord (2026-09-30): **A**. Eén eigen mod-tab, per ring gesorteerd.
