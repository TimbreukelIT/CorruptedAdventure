# Change 21 — Kloofsteen-set, scorched sandstone-set en woestijnflora

Scope: `blocks` — change 21

**Doel:** de eerste ring 2-blokken bestaan als speelbare blokken: de **kloofsteen-set** (gelaagde canyonsteen in 3 tinten + keien, bakstenen, trap, plaat, muur), de **scorched sandstone-set** (geschroeide zandsteen), **los zand** (`loose_sand`, instortend plafondzand voor de zandgrotten van change 26) en de **woestijnflora** (gebarsten klei, dorre struik, tonnetjescactus, woestijnbloem). Met blockstates/models, placeholder-textures van eigen pixels uit een nieuw ring 2-texturescript, loot tables, tags, lang (en + nl) en een plek in de creative tab. Plaatsing in de wereld volgt in change 23 (terrein + `canyonlands`/`oasis_flats`), 26 (`loose_sand` als zandgrot-plafond), 30 (extra biomes) en 31 (`dry_riverbeds`); definitieve textures in change 37.

Bron: `fase-1-wereld.md` §2 Ring 2 ("Unieke blokken": kloofsteen-set, woestijnflora + scorched sandstone; biome-tabel: canyonlands met gelaagde kloofsteen rood/oranje/crème, hoodoo_fields, dry_riverbeds met gebarsten klei), §1.4 (textures, creative tab), bouwplan rij 21.

---

## Afstemming (2026-10-01)

Afgestemd met 22, 24, 29, 33, 34 en 37 (basis: de aanbevolen opties).
- **`tools/textures/Ring2Textures.java` wordt hier aangemaakt** en is het enige ring 2-script (ring 2 + Nether-ring 2). 22, 24, 29, 33, 34 en 37 breiden het alleen uit; ontbreekt het bij hun start, dan stoppen en vragen (22 maakt het niet zelf aan).
- **Generieke opzet vanaf het begin** (scheelt herschrijven in 37): record `Animation` (frames, frametime, interpolate, glow) i.p.v. een boolean; `still(name, painter)` en `pulsing(name, glow, painter)` met **gloeikleur per materiaal** (geen vaste `PULSE_GLOW`). Ingebouwde controles: afmeting 16×16 of 16×(16·n) (particles 8×8), alpha alleen 0/255, gelijk alpha-masker per animatieframe. Change 37 voegt alleen `cycling` en de controle "zijkant = grond" toe.
- **Placeholder kloofsteen zonder harde laaglijn per blok** (horizontaal gerekte korrel; richting van 37 vraag 5 A), zodat de blokkerende terreincheck van change 23 met representatieve textures gebeurt.
- **Bloempotten (vraag 8 A):** `ModBlocks.registerWithoutItem`, `block/ModFlowerPots` en het filter in `ModCreativeTabs`/`ModBlocksTest` worden hier aangemaakt; change 22 breidt uit (palmzaailing). Bij vraag 8 B maakt 22 ze aan (als 22 vraag 3 A).
- **`desert_plant_soil`** wordt ook de grondregel van de palmzaailing (change 22 vraag 1 A). Alles wat later in deze tag komt (bijv. zoutkorst bij 29 vraag 6 C) is dus ook palmgrond.
- **Vorm tonnetjescactus is hier definitief** (vormen zijn `blocks`); change 37 tekent binnen de vorm. Een andere vorm later = `[blocks]`-backlog.
- **Los zand (besloten 2026-10-01, change 26 vraag 5 = B, keuze gebruiker):** het eigen instortende zandblok `loose_sand` komt in **deze** change (`blocks`). Change 26 zet het in de surface rule als plafond van de zandgrotten (vloeren blijven vanilla-zand). Zie stap 1 "Los zand" en vraag 10.
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 17, commit `7714b77`):

- **Blok-infrastructuur uit change 10/15** (sjabloon in `CODEBASE.md`, valkuil "Blokken toevoegen"):
  - `registry/ModBlocks`: `register(name, supplier)` registreert ook het blockitem via `ModItems.ITEMS.registerSimpleBlockItem`; `registerDoor` voor deuren. Registratievolgorde = creative-tab-volgorde; secties `// ---- Ring 1 ----` en `// ---- Nether ring 1 ----`. **Elk blok krijgt nu altijd een item**; er is geen helper voor blokken zonder item.
  - `registry/ModCreativeTabs`: `displayItems` roept `output.accept(block.get())` aan voor **elk** `ModBlocks`-entry (een blok zonder item zou daar crashen: lege stack).
  - `registry/ModTags` (`Blocks`, `Items`, `Biomes`, helper `common(path)` voor `c:`-tags).
  - Datagen: `ModBlockStateProvider` (`randomlyRotated`, `oreWithOverlay` + record `CubeTextures`, vanilla-helpers `simpleBlock`/`stairsBlock`/`slabBlock`/…), `ModItemModelProvider` (`blockItem(DeferredBlock)`, `basicItem`, `fenceInventory`/`buttonInventory`), `ModBlockLootProvider` (`getKnownBlocks` = alle `ModBlocks`, dus elk blok een table), `ModBlockTagsProvider`, `ModItemTagsProvider` (`copy`), `EnglishLanguageProvider`/`DutchLanguageProvider` (`addBlock`). Aangehaakt in `DataGenerators`; de data-run heeft `--existing src/main/resources` (texturecheck).
  - `block/ModFlammables`: vuurkansen in `FMLCommonSetupEvent#enqueueWork` via `FireBlock#setFlammable` (al public in NeoForge).
  - Test `registry/ModBlocksTest`: (1) elk blok heeft een `BlockItem` met dezelfde naam, (2) de tab toont eerst alle blokken, dan de losse items, elk item precies één keer.
- **Texturescript ring 1** `tools/textures/Ring1Textures.java` (definitief sinds change 17): single-file Java, alleen JDK, `TEXTURES`-map (naam → tekenmethode, `still`/`pulsing`), vaste seed per bestandsnaam, `HAND_EDITED`, contactblad `build/texture-preview/ring1.png`, generieke helpers (`valueNoise`, `shade`, `mix`, `seeded`, `set`/`setWrapped`, `outline`/`merge`, `pulseStrip`, `writePreview`). **Afspraak (opdracht ring 2): één texturescript per ring.** Ring 2 krijgt dus een eigen script; deze change maakt het aan.
- **Changes 18–20** (`infra`, `conditions`, `atmosphere`) raken geen blok- of item-registries; deze change hangt alleen naast hun registraties in `CorruptedAdventure`.
- **"Drie tinten" = drie aparte blokken** (rood, oranje, crème): change 23 legt er lagen (strata) mee in de canyonwanden. Eén blok met een gestreepte texture zou elke blokhoogte herhalen en leest niet als gesteentelagen. Let op voor change 23: vanilla's surface rule `minecraft:bandlands` hardcodet terracotta-kleuren en is dus niet bruikbaar voor eigen blokken.
- **Recipes horen bij `items` (fase 2)** → backlog (zoals change 10). Item-tags die vanilla-recipes laten werken (bijv. `#stone_tool_materials`) horen wel hier (tags = `blocks`), zie vraag 2.
- Registry-namen met het ring 2-thema: `canyon_`, `scorched_`, `desert_`, `dry_`, `cracked_clay`.
- De eigen houtset van ring 2 (palm) is change 22 en zit hier niet in; palm komt in de creative tab ná deze blokken.

---

## Plan

### 1. Blokken (`registry/ModBlocks`, nieuwe sectie `// ---- Ring 2 ----` ná Nether-ring 1)

Volgorde (= creative tab): kloofsteen-set, scorched sandstone-set, los zand, flora. Lijst hieronder = de besloten opties (vraag 1, 4, 8 = A; los zand = 26 vraag 5 B).

**Kloofsteen-set (vraag 1 A, 11 blokken)**

| Registry-naam | Class | Basis (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `red_canyon_stone` | `Block` | `stone` + `MapColor.TERRACOTTA_RED` | natuurlijke laag; loot vraag 2 |
| `orange_canyon_stone` | `Block` | `stone` + `MapColor.TERRACOTTA_ORANGE` | idem |
| `cream_canyon_stone` | `Block` | `stone` + `MapColor.TERRACOTTA_WHITE` | idem |
| `canyon_cobblestone` | `Block` | `cobblestone` | gemengde tinten (texture) |
| `canyon_cobblestone_stairs` / `_slab` / `_wall` | `StairBlock` / `SlabBlock` / `WallBlock` | `cobblestone_stairs` / `_slab` / `_wall` | basis = `canyon_cobblestone` |
| `canyon_stone_bricks` | `Block` | `stone_bricks` | rode tint (texture) |
| `canyon_stone_brick_stairs` / `_slab` / `_wall` | `StairBlock` / `SlabBlock` / `WallBlock` | `stone_brick_stairs` / `_slab` / `_wall` | basis = `canyon_stone_bricks` |

**Scorched sandstone-set (vraag 4 A, 10 blokken — dezelfde opbouw als vanilla-zandsteen)**

| Registry-naam | Class | Basis (`ofFullCopy`) |
|---|---|---|
| `scorched_sandstone` (+ `_stairs`, `_slab`, `_wall`) | `Block`, `StairBlock`, `SlabBlock`, `WallBlock` | `sandstone` (+ vanilla-tegenhangers) + `MapColor.TERRACOTTA_BROWN` |
| `cut_scorched_sandstone` (+ `_slab`) | `Block`, `SlabBlock` | `cut_sandstone` (+ slab) |
| `chiseled_scorched_sandstone` | `Block` | `chiseled_sandstone` |
| `smooth_scorched_sandstone` (+ `_stairs`, `_slab`) | `Block`, `StairBlock`, `SlabBlock` | `smooth_sandstone` (+ vanilla-tegenhangers) |

Alles dropt zichzelf (zoals zandsteen), houten pickaxe volstaat.

**Los zand (1 blok — keuze gebruiker, change 26 vraag 5 B)**

| Registry-naam | Class | Basis (`ofFullCopy`) | Gedrag |
|---|---|---|---|
| `loose_sand` | `block/LooseSandBlock` (`extends ColoredFallingBlock`, eigen `MapCodec`) | `sand` (`MapColor.SAND`, `SoundType.SAND`, stofkleur als zand) | ziet eruit als zand; **vanilla-valgedrag** (valt bij een buur-update als eronder vrij is: graven, plaatsen, explosie, water, een vallend buurblok → kettingreactie); **plus eigen trigger**: stort in als een **speler** er binnen ~2 blokken onder loopt, na een korte waarschuwing (mobs triggeren niet; keuze gebruiker) |

- **Trigger (server-side, `block/LooseSandTrigger`, game-bus):** op `PlayerTickEvent.Post`, alleen `ServerPlayer` (geen `FakePlayer`, niet spectator) — **alleen spelers, geen mobs** (keuze gebruiker 2026-10-01); gespreid gedrosseld (bijv. elke 10 ticks: `(tickCount + id) % 10 == 0`). Per check een klein vak boven de entity: horizontaal de bounding box ±1 blok, verticaal van de bovenkant van de entity tot +2 blokken (**startwaarden, afstemwaarde**; ≤ ~27 bloklookups). Een `loose_sand` in dat vak waarvan het blok eronder vrij is (`FallingBlock.isFree`) en die nog geen geplande tick heeft (`level.getBlockTicks().hasScheduledTick`) → **waarschuwing + scheduled tick**:
  - waarschuwing: `ServerLevel.sendParticles` (zandstof / `FALLING_DUST` met zand, een handvol rond het blok) en een kraak-/ritselgeluid (`level.playSound(null, …)`, vanilla zand-/grindgeluid, lage toonhoogte) — beide server-side verstuurd en door vanilla naar de clients gesynct; geen client-code;
  - `level.scheduleTick(pos, LOOSE_SAND, WARNING_TICKS)` met `WARNING_TICKS` ~15 (0,75 s, **afstemwaarde**); bij de tick valt het blok via de vanilla `FallingBlock#tick` (alleen als eronder nog vrij is). Het vallende blok geeft zijn buren een update → aangrenzend `loose_sand` valt vanilla-gewijs na (kettingreactie, ~2 ticks per stap).
- **Geen random ticks als trigger**, geen block entity. De trigger heeft een echte speler nodig; de instorting zelf (scheduled tick + vanilla-kettingreactie) loopt in elke tickende chunk.
- **Implementatiekeuzes (deze change, door de agent gekozen):** `loose_sand` **niet** in `#minecraft:sand` (anders groeien cactus/dorre struik erop en telt het als woestijngrond; het is een plafondblok); wel `#minecraft:mineable/shovel` en `#minecraft:overworld_carver_replaceables`. Spectators triggeren niet; creatieve spelers wel (zoals drukplaten). De pure vak-berekening (`scanBox(AABB)` → blokposities) en de beslissing (`shouldTrigger`: spelermodus, `loose_sand`, vrij eronder, nog geen geplande tick) staan los en zijn unit-getest. **Testkeuze (agent, gemarkeerd):** de speler-trigger wordt automatisch getest met de quick-play-client `Dev` van change 18 tegen de dedicated server (RCON `tp`), niet met een fake-player (die tickt niet via `PlayerTickEvent`); lukt dat niet, dan W-achteraf.

**Woestijnflora (4 blokken + vraag 8: 3 potten)**

| Registry-naam | Class | Basis (`ofFullCopy`) | Gedrag |
|---|---|---|---|
| `cracked_clay` | `Block` | vraag 5 (A: `clay`-hardheid, eigen geluid `PACKED_MUD`) | vol blok, willekeurig gedraaid tegen tegeling (`randomlyRotated`) |
| `dry_shrub` | `block/DryShrubBlock` (`extends DeadBushBlock`, eigen `mayPlaceOn`) | `dead_bush` | kruismodel, `replaceable`, XZ-offset (gekopieerd); shears → zichzelf, anders 0–2 stokken (zoals dode struik); brandbaar 60/100 |
| `barrel_cactus` | `block/BarrelCactusBlock` (`extends BushBlock`) | `cactus` (zonder random ticks) | vraag 6 (A: eigen vorm ~10×10×10 px, 1 schade bij aanraking, groeit niet, mobs mijden het); dropt zichzelf; vorm hier definitief (`blocks`) |
| `desert_flower` | `block/DesertFlowerBlock` (`extends FlowerBlock`, eigen `mayPlaceOn`) | `poppy` | kruismodel, XZ-offset; stew-effect vraag 9; dropt zichzelf; brandbaar 60/100 |
| `potted_dry_shrub`, `potted_barrel_cactus`, `potted_desert_flower` | `FlowerPotBlock` (NeoForge-constructor met `() -> Blocks.FLOWER_POT` en de plant) | `flower_pot` | **alleen bij vraag 8 A**; geen blockitem; loot `dropPottedContents` |

- **Grond voor de flora (vraag 7 A):** eigen blok-tag `corruptedadventure:desert_plant_soil` = `#minecraft:sand`, `#minecraft:terracotta`, `#minecraft:dirt`, `cracked_clay`. `DryShrubBlock`, `BarrelCactusBlock` en `DesertFlowerBlock` gebruiken die tag in `mayPlaceOn` (BushBlock breekt de plant af als de grond verdwijnt). Daarnaast `cracked_clay` in vanilla `#minecraft:dead_bush_may_place_on`, zodat ook vanilla-dode struiken op de rivierbeddingen (change 31) kunnen staan.
- **`BarrelCactusBlock` (vraag 6 A):** `getShape`/`getCollisionShape` = één box (~3–13 px breed, 10 px hoog; de vorm is hier definitief — vormen zijn `blocks`; change 37 tekent binnen deze vorm, een andere vorm later = `[blocks]`-backlog). `entityInside` → `entity.hurt(level.damageSources().cactus(), 1.0F)`, **alleen** als de bounding box van de entity de vorm (licht vergroot) echt raakt: vanilla roept `entityInside` aan voor elk blok in de blokcel van de entity, ongeacht de vorm, en de cactus is kleiner dan een blok. Mobs mijden hem via NeoForge `IBlockExtension#getBlockPathType` → `PathType.DAMAGE_OTHER` (en `getAdjacentBlockPathType` → `DANGER_OTHER`; API bij de bouw verifiëren in 21.1.252). Geen random ticks, geen groei.
- **Codecs:** de drie eigen plant-classes overschrijven `codec()` met een eigen `MapCodec` (`simpleCodec`; de bloem met de effect-codec van `FlowerBlock`), zoals vanilla elke blokklasse een codec geeft.
- **Bloempotten (vraag 8 A):** nieuwe helper `ModBlocks.registerWithoutItem(name, supplier)`; nieuw klein class `block/ModFlowerPots` dat in `FMLCommonSetupEvent#enqueueWork` `((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(plantId, pottedHolder)` aanroept (zoals `ModFlammables`; aanhaken in `CorruptedAdventure`). `ModCreativeTabs` slaat blokken zonder item over (`block.get().asItem() != Items.AIR`).
- **Brandbaarheid** (`ModFlammables`): `dry_shrub` en `desert_flower` 60/100 (zoals dode struik en bloemen); stenen, cactus en klei niet.

### 2. Tags (`ModBlockTagsProvider`, `ModItemTagsProvider`, `ModTags`)

Nieuwe `TagKey`s in `ModTags`:
- `Blocks.CANYON_STONES` / `Items.CANYON_STONES` = `corruptedadventure:canyon_stones` (de drie tinten). **Voor latere changes:** change 23 (surface rules), 24/25 (turkoois "diep in kloofsteen": ore-doel via `TagMatchTest`, model `oreWithOverlay` met een kloofsteen-basis via `modLoc`).
- `Blocks.DESERT_PLANT_SOIL` = `corruptedadventure:desert_plant_soil` (vraag 7; change 22 kan hem hergebruiken voor de palm-sapling, change 29 kan zoutgrond toevoegen).

Blok-tags:
- `#minecraft:mineable/pickaxe`: alle steen- en zandsteenblokken (incl. trap/plaat/muur), en `cracked_clay` alleen bij vraag 5 C. `#minecraft:mineable/shovel`: `cracked_clay` (vraag 5 A/B).
- `#minecraft:stairs`, `#minecraft:slabs`, `#minecraft:walls` (muren verbinden en tellen als muur).
- `#minecraft:overworld_carver_replaceables`: `#corruptedadventure:canyon_stones`, `canyon_cobblestone`, de natuurlijke `scorched_sandstone` en `cracked_clay` — grotten, kloven en onze `unnatural_tunnel` snijden erdoor. Vraag 3 bepaalt of de tinten ook in `#minecraft:base_stone_overworld` komen.
- `#minecraft:dead_bush_may_place_on`: `cracked_clay`.
- `loose_sand`: `#minecraft:mineable/shovel` en `#minecraft:overworld_carver_replaceables`; **niet** in `#minecraft:sand` (implementatiekeuze, zie stap 1).
- Flora: de vanilla-tags van het voorbeeldblok spiegelen (bij de bouw nalezen in de vanilla-tagbestanden): `dry_shrub` als `dead_bush` (o.a. `#replaceable`, `#replaceable_by_trees`, `#sword_efficient`), `desert_flower` in `#minecraft:small_flowers` (→ `#flowers`; vraag 9), potten in `#minecraft:flower_pots`. `barrel_cactus` alleen wat vanilla `cactus` ook heeft en zinvol is (geen `#replaceable`).
- Conventietags (namen verifiëren in NeoForge 21.1.252 `Tags.Blocks`): `c:cobblestones` (`canyon_cobblestone`, vraag 2), `c:sandstone/blocks`, `c:sandstone/slabs`, `c:sandstone/stairs` (scorched-blokken; niet in `uncolored`/`red`).

Item-tags: `copy` van `canyon_stones`, `stairs`, `slabs`, `walls`, `small_flowers`, `c:cobblestones`, `c:sandstone/*`; bij vraag 2 A daarnaast `canyon_cobblestone` in `#minecraft:stone_tool_materials` en `#minecraft:stone_crafting_materials` (stenen tools, oven en andere steenrecepten).

### 3. Loot (`ModBlockLootProvider`)

- Tinten: vraag 2 (A: `createSingleItemTableWithSilkTouch(block, CANYON_COBBLESTONE)`, zoals steen → keien).
- Platen: `createSlabItemTable`; overige steen/zandsteen: `dropSelf`.
- `cracked_clay`: vraag 5 (A: `dropSelf`; B: `createSingleItemTableWithSilkTouch` + 4 `minecraft:clay_ball`, zoals klei).
- `dry_shrub`: `createShearsDispatchTable` met 0–2 stokken + `applyExplosionDecay` (zoals vanilla `dead_bush`).
- `barrel_cactus`, `desert_flower`: `dropSelf`. Potten: `dropPottedContents`.
- `loose_sand` (**implementatiekeuze**): `createSingleItemTableWithSilkTouch(LOOSE_SAND, Items.SAND)` — dropt `minecraft:sand`, met Silk Touch zichzelf (spelers bouwen er dus geen valstrikken mee zonder Silk Touch). Een vallend blok dat landt wordt weer `loose_sand` (vanilla `FallingBlockEntity`), dat bij afbreken ook zand dropt.

### 4. Models en blockstates (datagen)

- `ModBlockStateProvider`:
  - tinten: `simpleBlock` (`cube_all`); keien en bakstenen + `stairsBlock`/`slabBlock`/`wallBlock` met hun eigen texture.
  - scorched: `scorched_sandstone` = `cube_bottom_top` (`_top`, `scorched_sandstone` als zijkant, `_bottom`); `cut_` en `chiseled_` = `cube_column` met eigen zijkant en `scorched_sandstone_top` boven/onder; `smooth_` = `cube_all` op `scorched_sandstone_top` (zoals vanilla); trap/plaat/muur met de vanilla-indeling (side/bottom/top).
  - `cracked_clay`: `randomlyRotated`.
  - `loose_sand`: `simpleBlock` (`cube_all`), niet gedraaid (zoals zand).
  - `dry_shrub`, `desert_flower`: `models().cross(...)` met `renderType("cutout")`.
  - `barrel_cactus`: eigen model met één element (zelfde maten als de vorm), textures `barrel_cactus_top` / `_side` / (onder = top), particle = side.
  - potten: parent `minecraft:block/flower_pot_cross` met `plant` (struik, bloem), `renderType("cutout")`; `potted_barrel_cactus`: parent `minecraft:block/flower_pot` + één extra element (kleine cactus op de aarde). Render type in de model-JSON, dus geen client-code.
- `ModItemModelProvider`: blockitems via `blockItem`; muren via `wallInventory`; `dry_shrub` en `desert_flower` als plat item met de bloktexture (`item/generated`, `layer0` = `block/<naam>`, zoals vanilla-bloemen); `barrel_cactus` via zijn blockmodel. Potten hebben geen item.

### 5. Placeholder-textures: nieuw script `tools/textures/Ring2Textures.java`

- **Eén script per ring**, zelfde opzet als `Ring1Textures.java`: single-file Java, alleen JDK, `TEXTURES`-map met `still`/`pulsing`, vaste seed per bestandsnaam, `HAND_EDITED` (leeg), contactblad `build/texture-preview/ring2.png` (niet in git), schrijft naar `src/main/resources/assets/corruptedadventure/textures/`. Eigen pixels, leest geen vanilla-bestanden.
- De generieke helpers worden uit `Ring1Textures.java` **gekopieerd** (Java 21 kan vanuit een single-file-script geen tweede bronbestand laden). `Ring1Textures.java` blijft ongewijzigd; ring 1-PNG's blijven byte-identiek. Ontdubbelen is `infra` → bestaande backlogregel (change 10/17) aanvullen.
- Later breiden change 22 (palm), 24 (zonnesteen/turkoois), 29 (zout), 33/34 (Nether-ring 2) **hetzelfde script** uit; change 37 maakt alles definitief (stijlregels §1.4).
- **Eenvoudige eerste versie** (placeholder-niveau, palet 3–5 tinten per materiaal als constanten bovenin; **werkpaletten, geen beslissing** — definitief in change 37):
  - kloofsteen rood `6E2E1E 8A3B26 A44C30 BC6040 CF7A55`, oranje `8E4A1C A85E24 C2742E D68B3E E4A55A`, crème `B49C7A C7B08C D8C39F E6D4B2 F1E3C6`; horizontaal gerekte korrel en zachte tintvariatie, **geen harde laaglijn per blok** (de lagen komen uit de blokbanden van change 23; richting van change 37 vraag 5 A, zodat de blokkerende terreincheck van 23 representatief is);
  - keien: ronde stenen in alle drie de tinten met donkere voeg; bakstenen: rode tint in vanilla-baksteenindeling;
  - scorched sandstone `4A3A2A 6B5236 8A6B45 A88656 C2A06C` + roet `2A221C`: zijkant met horizontale band zoals zandsteen en roetvlekken, top/bodem korrelig; cut = rand + vlak; chiseled = eenvoudig motief;
  - los zand `C9B98A D6C799 DFD1A5 E8DCB2` (zandkleurig, eigen pixels): placeholder iets donkerder en grover gekorreld dan vanilla-zand (zie change 37);
  - gebarsten klei `7C6A58 92806A A8957C BCA98F`, scheuren `4E4236` (polygonen);
  - dorre struik `5A4630 75603F 917A52`; tonnetjescactus `2F4A2A 3F6136 557A44 6E9455` met stekelpuntjes `E6D9A8`; woestijnbloem blad `A8326E C94C8A E27AAB`, steel `4F6A33 6B8A45`.
- Bestanden (definitieve namen; change 37 vervangt alleen pixels):

| Bestand | Voor |
|---|---|
| `block/red_canyon_stone.png`, `block/orange_canyon_stone.png`, `block/cream_canyon_stone.png` | tinten |
| `block/canyon_cobblestone.png` | keien + trap/plaat/muur |
| `block/canyon_stone_bricks.png` | bakstenen + trap/plaat/muur |
| `block/scorched_sandstone_top.png`, `block/scorched_sandstone.png` (zijkant), `block/scorched_sandstone_bottom.png` | zandsteen, trap/plaat/muur; top ook voor `smooth_` |
| `block/cut_scorched_sandstone.png`, `block/chiseled_scorched_sandstone.png` | zijkanten |
| `block/loose_sand.png` | los zand |
| `block/cracked_clay.png` | gebarsten klei |
| `block/dry_shrub.png`, `block/desert_flower.png` | kruismodellen + item (cutout: alpha 0/255) |
| `block/barrel_cactus_top.png`, `block/barrel_cactus_side.png` | cactus |

  16 PNG's, geen animaties. Volgorde: **eerst het script draaien, dan `runData`** (`ExistingFileHelper`).

### 6. Lang (`EnglishLanguageProvider`, `DutchLanguageProvider`)

Nieuw commentaarblok "Ring 2 blocks". Engels verplicht; Nederlands volgt de vanilla-`nl_nl`-terminologie voor keien/bakstenen/gesneden/gebeitelde/gladde zandsteen en "… in bloempot" (opzoeken in de asset-cache, valkuil in `CODEBASE.md`). Voorstel:

| Key | en_us | nl_nl (indicatief) |
|---|---|---|
| `red_/orange_/cream_canyon_stone` | Red / Orange / Cream Canyon Stone | Rode / Oranje / Crèmekleurige kloofsteen |
| `canyon_cobblestone` (+ stairs/slab/wall) | Canyon Cobblestone (Stairs/Slab/Wall) | Kloofkeien (-trap/-plaat/-muur) |
| `canyon_stone_bricks` (+ brick stairs/slab/wall) | Canyon Stone Bricks (Canyon Stone Brick Stairs/…) | Kloofstenen bakstenen (…) |
| `scorched_sandstone` (+ cut/chiseled/smooth, stairs/slab/wall) | Scorched Sandstone, Cut/Chiseled/Smooth Scorched Sandstone (…) | Geschroeide zandsteen, gesneden/gebeitelde/gladde geschroeide zandsteen (…) |
| `loose_sand` | Loose Sand | Los zand |
| `cracked_clay` | Cracked Clay | Gebarsten klei |
| `dry_shrub` | Dry Shrub | Dorre struik |
| `barrel_cactus` | Barrel Cactus | Tonnetjescactus |
| `desert_flower` | Desert Flower | Woestijnbloem |
| `potted_*` | Potted Dry Shrub / Barrel Cactus / Desert Flower | (vanilla-patroon voor potten) |

### 7. Test-aanpassing

`ModBlocksTest`: (0) `loose_sand` heeft een blockitem en staat in de tab tussen de scorched sandstone-set en de flora; (1) "elk blok heeft een blockitem" geldt voor alle blokken behalve `FlowerPotBlock`-instanties (die hebben er bewust geen; controleer dat ze `asItem() == AIR` geven); (2) de tab-verwachting slaat blokken zonder item over. Optioneel: elke pot verwijst via `getPotted()` naar de juiste plant.

Nieuw: `block/LooseSandTriggerTest` — `scanBox` geeft voor een speler van 0,6×1,8 blokken precies het vak footprint ±1 × (bovenkant … +2); een zandblok 3 blokken boven het hoofd valt erbuiten; `shouldTrigger` is onwaar voor spectator, voor een blok met iets vasts eronder en voor een blok met al een geplande tick.

### 8. Documentatie

- `CODEBASE.md`: blok-tabel (sectie ring 2), nieuwe classes (`DryShrubBlock`, `BarrelCactusBlock`, `DesertFlowerBlock`, `ModFlowerPots`, `LooseSandBlock`, `LooseSandTrigger`), `ModBlocks.registerWithoutItem`, `ModCreativeTabs`-filter, `ModTags` (`canyon_stones`, `desert_plant_soil`), `tools/textures/Ring2Textures.java` (tooling-tabel + textures-regel), valkuilen: blok zonder item en de creative tab; worldgen-`loose_sand` hangt tot een buur-update of een speler eronder (trigger via `PlayerTickEvent`, alleen spelers, niet via random ticks); `entityInside` negeert de vorm; potten via `addPlant` in common setup; "Laatst bijgewerkt".
- `fase-1-wereld.md` §2 Ring 2 → "Unieke blokken": **Uitwerking (change 21)** met de blokkenlijst, gedrag (cactus, flora-grond, loot) en de antwoorden; §1.4: ring 2 heeft een eigen texturescript.
- `02-beslissingenlog.md`: de regels hieronder.
- `docs/backlog.md`:
  - `[items] Recipes kloofsteen-set (keien → trap/plaat/muur, tinten → bakstenen, steenzaag), scorched sandstone-set (incl. smelten → smooth), woestijnbloem → kleurstof. (bron: change 21)`
  - `[infra]` bestaande regel over de texturescripts aanvullen: ring 2 heeft nu een tweede script met gekopieerde helpers (change 21); ontdubbelen kan nu.
  - eventuele afgewezen opties (bijv. cactusgroei, potten) als later-idee.

---

## Client/server-impact

- Blokken en items zijn common registry-entries; NeoForge synct de registry-ID's bij het joinen.
- Gedrag in common code: `mayPlaceOn`/`canSurvive`/`updateShape` (beide kanten), cactusschade via `entityInside` (schade alleen server-side; `hurt` doet op de client niets), path type (server-AI). `LooseSandTrigger` draait alleen server-side (`!level.isClientSide()`); waarschuwingsparticles en -geluid stuurt de server via vanilla (`sendParticles`, `playSound(null, …)`); het vallen is vanilla `FallingBlockEntity` (server gesimuleerd, naar alle clients gesynct). Bloempot-registratie en vuurkansen in `FMLCommonSetupEvent#enqueueWork` (beide kanten, alleen statische maps).
- **Geen client-code:** render type in de model-JSON, kleuren in de PNG's (geen `BlockColor`), geen netwerk, geen SavedData/attachments.
- Het texturescript staat buiten `src/` en komt niet in de jar.

---

## Tests

Bouwplan: **B, D**. Aanbevolen extra: **S** (nieuwe common-setup-code en blokgedrag) + RCON-checks.

- [ ] **Script** — `java tools/textures/Ring2Textures.java` draait; 16 PNG's van 16×16; tweede run → `git status` toont geen wijzigingen; kruismodellen alleen alpha 0/255; `Ring1Textures.java` en ring 1-PNG's ongewijzigd; contactblad `build/texture-preview/ring2.png` bestaat.
- [ ] **B** — `./gradlew build` slaagt, incl. het aangepaste `ModBlocksTest` en `LooseSandTriggerTest`.
- [ ] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit: blockstates/models/item-models van alle nieuwe blokken (kruismodellen en potten `render_type` cutout), loot tables (tinten met Silk Touch-tak → keien bij vraag 2 A, `loose_sand` → `minecraft:sand` / Silk Touch → zichzelf, `dry_shrub` shears/stokken, platen, potten), tags (`mineable/*`, `stairs`/`slabs`/`walls`, `overworld_carver_replaceables`, `dead_bush_may_place_on`, `small_flowers`, `flower_pots`, `canyon_stones`, `desert_plant_soil`, `c:`-tags, item-tags), lang en/nl.
- [ ] **S** — `tools/server-smoke-test.sh` met `level-name=smoke-change21`: server start zonder fouten of client-class-meldingen. Via RCON (daarna RCON weer uit):
  - tags: per blok `setblock` + `execute if block <pos> #<tag>` (o.a. `#minecraft:walls`, `#minecraft:mineable/pickaxe`, `#corruptedadventure:canyon_stones`, `#minecraft:overworld_carver_replaceables`);
  - loot: `loot insert <kist> mine <pos> minecraft:iron_pickaxe` op een tint → `canyon_cobblestone` (vraag 2 A), met Silk Touch → de tint; dubbele plaat → 2; `dry_shrub` met `minecraft:shears` → struik, met de hand → 0–2 stokken; pot → bloempot + plant;
  - overleven: `barrel_cactus`/`desert_flower`/`dry_shrub` met `setblock` op steen zetten en een buur-update geven (blok ernaast plaatsen) → plant verdwijnt; op zand / gebarsten klei → blijft.
  - **los zand** (in een geforceloade chunk, `forceload add`, `execute if loaded` pollen): (1) een 5×5-plafond `loose_sand` op Y+4 boven een luchtkamer bouwen met `fill … minecraft:air` eronder en een stenen steunrand; ~100 ticks wachten → plafond hangt nog (geen trigger zonder speler of update). (2) Mobs triggeren niet: `summon minecraft:zombie … {NoAI:1b,PersistenceRequired:1b}` direct onder het plafond → na 100 ticks hangt het plafond nog. (3) Speler-trigger: de quick-play-client `Dev` (change 18) joint automatisch; RCON `gamemode survival Dev` + `tp Dev <x> <y> <z>` onder het plafond (bovenkant hoofd ≤ 2 blokken onder het zand) → binnen ~40 ticks is het plafond weg (`execute if block … minecraft:air`) en ligt er `loose_sand` op de vloer; `tp Dev` 4 blokken lager onder een tweede plafond → blijft hangen; `gamemode spectator Dev` onder een derde plafond → blijft hangen; geen fouten in server- of client-log. Lukt de client-join niet automatisch: stap 3 naar W-achteraf. (4) Vanilla-pad: plafond zonder speler, één blok ernaast `setblock … air` → kettingreactie. (5) Loot: `loot insert <kist> mine <pos> minecraft:iron_shovel` → `minecraft:sand`; met Silk Touch-schop → `loose_sand`. `tick query` tijdens (3) en (4): geen MSPT-piek.
- **Blokkerende handmatige test: nee.** Alles waar latere changes op bouwen (registry-namen, tags, loot, overleef-regels) is automatisch te controleren (build, runData, RCON). Het uiterlijk is placeholder (definitief in change 37) en cactusschade/potten zijn speelgevoel. → **W-achteraf**, in-game checklist (runClient, creative + survival):
  - [ ] Creative tab: ring 2-blokken ná Nether-ring 1, in de volgorde kloofsteen → scorched sandstone → los zand → flora; geen potten in de tab; namen en/nl.
  - [ ] Alle blokken plaatsbaar, geen paars-zwart; tinten naast elkaar leesbaar als lagen; muren verbinden onderling en met vanilla-muren; trappen/platen in alle vormen.
  - [ ] Scorched sandstone: top/zijkant/bodem kloppen; cut/chiseled/smooth zoals vanilla-zandsteen opgebouwd.
  - [ ] Mijnen in survival: tint met houten pickaxe → kloofkeien (vraag 2), met Silk Touch → tint; zonder pickaxe niets; gebarsten klei met schop (vraag 5).
  - [ ] Flora alleen plaatsbaar op zand, rood zand, terracotta, aarde/gras en gebarsten klei (vraag 7); ondergrond weghalen → plant breekt.
  - [ ] Tonnetjescactus: tegenaan lopen → schade; ernaast staan zonder aanraking → geen schade; een zombie loopt eromheen; items die erop vallen (vraag 6).
  - [ ] Bloempot: alle drie de planten erin en eruit (vraag 8).
  - [ ] Woestijnbloem: bijen bestuiven hem; suspicious stew geeft het gekozen effect (vraag 9).
  - [ ] Dorre struik en woestijnbloem branden; vuur op de stenen niet.
  - [ ] Los zand: lijkt op zand (subtiel anders); een hangend plafond (creative gebouwd met steunen, dan steunen weg via `/setblock`) stort in als je eronder loopt, met eerst stof en een kraakgeluid; graven ernaast laat het ook vallen; dropt zand, met Silk Touch zichzelf.
  - [ ] Stenen tools en een oven van kloofkeien (vraag 2 A).

---

## Risico's / aandachtspunten

- **Aantal blokken** (29 bij de besloten opties, incl. `loose_sand`): veel datagen-regels en 16 textures; daarom placeholders eenvoudig houden, stijl hoort bij change 37.
- **`entityInside` negeert de vorm**: zonder eigen raakcontrole doet de cactus schade aan wie alleen schuin ernaast in dezelfde blokcel staat.
- **Blok zonder item** (potten): de creative tab en `ModBlocksTest` gaan er nu van uit dat elk blok een item heeft; beide aanpassen, anders crasht het opbouwen van de tab ("stack must not be empty").
- **`#base_stone_overworld`** (vraag 3) raakt veel vanilla-worldgen tegelijk (granite/diorite/andesite/tuf-, aarde- en grindklodders, mos, sculk, druipsteen); daarom niet standaard.
- **`#dirt` in de flora-grond** laat de flora ook op gewoon gras staan (ring 1). Dat is alleen een plaatsingsregel; waar ze groeit beslist worldgen (23/30/31).
- **Nederlandse namen**: vanilla-terminologie nazoeken, anders inconsistent naast vanilla-zandsteen en -keien.
- **Change 23** kan `minecraft:bandlands` niet gebruiken voor de kloofsteenlagen (hardcoded terracotta): zelf lagen opbouwen (bijv. `y_above`-condities + ruis) of een eigen surface-rule-type. Niet in deze change; alleen als aandachtspunt doorgeven.
- **Texture-helpers gekopieerd** (ring 1 → ring 2): bewuste duplicatie tot een `infra`-change het ontdubbelt.
- **Los zand, alleen spelers (besloten 2026-10-01):** mobs laten plafonds niet instorten, dus zandgrotten liggen nog intact als een speler aankomt. Gevolg: een mob die onder een plafond loopt doet niets (alleen vanilla-updates zoals een creeper-explosie laten het vallen).
- **Los zand en performance:** de check kost ≤ ~27 bloklookups per speler per 10 ticks; de instorting zelf is de vanilla-kettingreactie (aantal vallende entities = het aaneengesloten plafond). Meten met `tick query` in S (hier) en in change 26 (echte zandgrotten).
- **Valstrik-griefing:** spelers met Silk Touch kunnen `loose_sand`-plafonds bouwen (alleen hangend als het blok bij plaatsen al gesteund is en de steun later verdwijnt zonder buur-update — in de praktijk lastig, want vanilla laat het dan vallen). Geaccepteerd.

---

## Ontwerpvragen

### 1. Hoe groot wordt de kloofsteen-set?
Alle opties: drie natuurlijke tinten (`red_`, `orange_`, `cream_canyon_stone`) als aparte blokken, zodat change 23 er lagen mee kan bouwen.
- **A. Tinten + kloofkeien en kloofbakstenen, elk met trap/plaat/muur (aanbevolen).** 11 blokken, 5 textures. Zelfde opbouw als vanilla-keien en -steenbakstenen; keien in gemengde tinten, bakstenen rood. Genoeg bouwmateriaal, beperkte omvang.
- **B. Minimaal: tinten + keien + bakstenen, alleen de bakstenen met trap/plaat/muur.** 8 blokken. Minder werk; keien zijn dan alleen een vol blok.
- **C. Per tint een eigen bakstenen-familie** (3 × bakstenen + trap/plaat/muur) + keien-familie. 19 blokken, ~9 textures. Meer kleur om mee te bouwen; drie keer zoveel models, loot en lang.
- **D. Ook de tinten zelf met trap/plaat/muur** (zoals zandsteen) + keien- en bakstenen-familie. 20 blokken. Natuurlijke tinten direct verbouwbaar; veel blokken voor weinig extra.

Antwoord: **A** — drie tinten + kloofkeien en kloofbakstenen, elk met trap/plaat/muur (11 blokken).

### 2. Wat laten de tinten vallen, en tellen kloofkeien als steen voor vanilla-recepten?
- **A. Tinten → `canyon_cobblestone` (Silk Touch → de tint), zoals steen → keien; kloofkeien in `#stone_tool_materials`, `#stone_crafting_materials` en `c:cobblestones` (aanbevolen).** Vertrouwd; in ring 2 kun je met lokale steen stenen tools en een oven maken. Hardheid zoals steen (houten pickaxe nodig).
- **B. Zelfde drop, maar geen vanilla-recept-tags.** Kloofkeien zijn tot fase 2 alleen bouwmateriaal; spelers moeten gewone keien meenemen of onder de canyonlagen graven.
- **C. Tinten droppen zichzelf** (zoals zandsteen/terracotta); keien alleen via recepten in fase 2. Kleuren blijven bij het mijnen behouden; keien hebben in fase 1 geen bron.

Antwoord: **A** — tinten → `canyon_cobblestone` (Silk Touch → de tint); kloofkeien in `#stone_tool_materials`, `#stone_crafting_materials` en `c:cobblestones`.

### 3. Telt kloofsteen als "gewone steen" voor vanilla-worldgen?
In alle opties: de tinten in `#overworld_carver_replaceables` (grotten en kloven snijden erdoor) en in de eigen tag `corruptedadventure:canyon_stones`.
- **A. Nee, niet in `#base_stone_overworld` (aanbevolen).** Geen grijze graniet-/dioriet-/andesiet-/tuf-, aarde- en grindklodders in de rode canyonwanden; change 25 kiest zelf welke ertsen (turkoois, afgebouwde vanilla-ores) in kloofsteen komen via `canyon_stones`. Mos, sculk en druipsteen groeien er niet op.
- **B. Ja, in `#base_stone_overworld`.** Alle vanilla-ondergrondfeatures werken vanzelf in kloofsteen (ook mos/lush, sculk); wel grijze klodders in de gekleurde lagen en minder controle voor change 25.

Antwoord: **A** — niet in `#base_stone_overworld`; wel in `#overworld_carver_replaceables` en `corruptedadventure:canyon_stones`.

### 4. Hoe groot wordt de scorched sandstone-set?
- **A. Zoals vanilla-zandsteen (aanbevolen):** `scorched_sandstone` + trap/plaat/muur, `cut_` + plaat, `chiseled_`, `smooth_` + trap/plaat. 10 blokken, 5 textures. Herkenbaar en compleet; change 30/31 (hoodoos, geblakerde rivierbeddingen) en fase 3 (ruïnes) hebben genoeg variatie.
- **B. Kern:** `scorched_sandstone` + trap/plaat/muur, `cut_` en `smooth_` (zonder eigen trap/plaat). 6 blokken. Minder werk, minder bouwvarianten.
- **C. Alleen** `scorched_sandstone` + trap/plaat/muur. 4 blokken. Puur worldgen-materiaal.

Antwoord: **A** — zoals vanilla-zandsteen (10 blokken).

### 5. Hoe gedraagt gebarsten klei zich?
- **A. Droge, harde grond: schop, dropt zichzelf, geluid als packed mud (aanbevolen).** Eenvoudig; dient als bodem voor rivierbeddingen (change 31) en flora. Niet in `#dirt` (geen gras of saplings erop).
- **B. Als klei: dropt 4 kleiballen (Silk Touch → zichzelf).** Geeft een kleibron in een ring zonder water; wel balans-impact (bakstenen, terracotta) en verwarrend dat het blok dan "verdwijnt".
- **C. Als terracotta: pickaxe, dropt zichzelf.** Voelt als steen; past minder bij "klei" maar wel bij "hard gebakken".

Antwoord: **A** — droge, harde grond: schop, dropt zichzelf, geluid als packed mud; niet in `#dirt`.

### 6. Wat doet de tonnetjescactus?
- **A. Klein stekelig blok: 1 schade bij aanraking zoals cactus, groeit niet, mobs mijden hem (aanbevolen).** Eén eigen class (`BarrelCactusBlock`), geen random ticks. Voelt als woestijngevaar; voorspelbaar in multiplayer (server-side schade).
- **B. Puur decoratief** (geen schade, als een bloem met eigen vorm). Eenvoudigst; geen gevaar.
- **C. Als A en hij groeit langzaam** (random tick: nieuwe cactusjes ernaast of een groter stadium). Levendiger, maar meer code, random ticks en kans op wildgroei rond bases.

Antwoord: **A** — klein stekelig blok: 1 schade bij aanraking, groeit niet, mobs mijden hem.

### 7. Waarop groeit de woestijnflora?
- **A. Eén eigen tag `corruptedadventure:desert_plant_soil` = zand (ook rood), terracotta, aarde/gras en gebarsten klei, voor alle drie de planten; gebarsten klei ook in vanilla `#dead_bush_may_place_on` (aanbevolen).** Eén regel, later uitbreidbaar (bijv. palm-sapling in change 22, zoutgrond in 29); vanilla-dode struiken kunnen op de rivierbeddingen.
- **B. Vanilla-regels per plant:** struik als dode struik (+ gebarsten klei), cactus alleen op zand (+ gebarsten klei), bloem als bloem (aarde/gras) + zand. Vertrouwder per plant, maar drie verschillende regels.
- **C. Strikt woestijn: alleen zand en gebarsten klei.** Flora blijft herkenbaar "van ring 2"; spelers kunnen de bloem niet in hun gewone tuin zetten.

Antwoord: **A** — eigen tag `corruptedadventure:desert_plant_soil` (zand, terracotta, aarde/gras, gebarsten klei) voor alle drie de planten; gebarsten klei ook in `#dead_bush_may_place_on`.

### 8. Komen de planten in een bloempot?
- **A. Ja: `potted_dry_shrub`, `potted_barrel_cactus`, `potted_desert_flower` (aanbevolen).** Zoals vanilla dode struik, cactus en bloemen; 3 extra blokken zonder item, een kleine `ModFlowerPots` in common setup en aanpassingen in creative tab en test.
- **B. Nee, later eventueel (backlog).** Minder werk; een speler die de plant in een pot zet krijgt niets (pot blijft leeg).

Antwoord: **A** — ja: `potted_dry_shrub`, `potted_barrel_cactus`, `potted_desert_flower`.

### 9. Is de woestijnbloem een "echte" bloem?
`FlowerBlock` vraagt altijd een stew-effect; in `#small_flowers` doen bijen, suspicious stew en endermen er iets mee. Kleurstof is een recept (fase 2, backlog).
- **A. Ja, in `#small_flowers`, stew-effect Fire Resistance 4 s (zoals allium; knipoog naar de hitte van ring 3) (aanbevolen).** Gedraagt zich als elke vanilla-bloem.
- **B. Ja, in `#small_flowers`, stew-effect Saturation (zoals paardenbloem; past bij uitdroging/honger).** Zelfde gedrag, ander effect.
- **C. Nee, decoratieve plant zonder bloem-tags.** Geen bijen of stew; minder vanilla-integratie.

Antwoord: **A** — ja, in `#small_flowers`, stew-effect Fire Resistance 4 s.

### 10. Los zand voor de zandgrotten (doorgeschoven uit change 26 vraag 5)
- **A. Geen eigen blok; zandgrotten gebruiken vanilla-zand dat alleen op een buur-update valt (aanbevolen in change 26).**
- **B. Eigen blok `loose_sand` in deze change: ziet eruit als zand, stort in (met korte waarschuwing) als een speler binnen ~2 blokken eronder loopt, plus vanilla-valgedrag; server-side via speler-check + scheduled tick.**

Antwoord: **B (keuze gebruiker, 2026-10-01)** — `loose_sand` in change 21 (zie stap 1 "Los zand"); loot `minecraft:sand`, Silk Touch zichzelf (implementatiekeuze); change 26 gebruikt het voor de zandgrot-plafonds. Vervolgantwoord gebruiker (2026-10-01): **alleen spelers** triggeren het instorten, geen mobs.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | blocks | Kloofsteen-set: drie natuurlijke tinten (rood, oranje, crème) als aparte blokken + kloofkeien en kloofbakstenen, elk met trap/plaat/muur | Lagen in de canyonwanden (change 23) vragen aparte blokken; genoeg bouwmateriaal bij beperkte omvang |
| 2026-10-01 | blocks | Kloofsteen-tinten droppen `canyon_cobblestone` (Silk Touch → tint); kloofkeien wel in de vanilla-steenrecept-tags | Vertrouwd; lokale steen geeft stenen tools en een oven |
| 2026-10-01 | blocks | Kloofsteen niet in `#base_stone_overworld`; altijd in `#overworld_carver_replaceables` en `corruptedadventure:canyon_stones` | Geen grijze klodders in de gekleurde lagen; change 25 kiest zelf de ertsen |
| 2026-10-01 | blocks | Scorched sandstone-set zoals vanilla-zandsteen (10 blokken) | Herkenbaar en compleet voor hoodoos, beddingen en ruïnes |
| 2026-10-01 | blocks | Gebarsten klei: schop, dropt zichzelf, packed-mud-geluid, niet in `#dirt` | Eenvoudige droge bodem voor beddingen en flora |
| 2026-10-01 | blocks | Tonnetjescactus: 1 schade bij aanraking, groeit niet, mobs mijden hem; vorm definitief | Woestijngevaar zonder random ticks |
| 2026-10-01 | blocks | Woestijnflora groeit op `corruptedadventure:desert_plant_soil` (zand, terracotta, aarde/gras, gebarsten klei) | Eén uitbreidbare regel, ook voor de palmzaailing |
| 2026-10-01 | blocks | Bloempotten voor de woestijnflora: ja (3 potten) | Zoals vanilla dode struik, cactus en bloemen |
| 2026-10-01 | blocks | Woestijnbloem in `#small_flowers`, stew-effect Fire Resistance 4 s | Gedraagt zich als elke vanilla-bloem; knipoog naar ring 3 |
| 2026-10-01 | blocks | Los zand `loose_sand` (keuze gebruiker, uit change 26 vraag 5 B): valt vanilla-gewijs bij buur-updates én stort na een korte waarschuwing (stof, kraakgeluid, ~0,75 s) in als een **speler** (geen mob; keuze gebruiker) binnen ~2 blokken eronder komt; server-side via `PlayerTickEvent`-check + scheduled tick, geen random ticks | Echte valstrikken voor spelers; grotten blijven intact tot een speler komt |
| 2026-10-01 | blocks | `loose_sand` dropt `minecraft:sand`, Silk Touch zichzelf; niet in `#minecraft:sand` (implementatiekeuze) | Geen valstrikmateriaal zonder Silk Touch; geen plantengrond op plafonds |
| 2026-10-01 | textures | Ring 2 krijgt een eigen texturescript `tools/textures/Ring2Textures.java` (zelfde opzet als ring 1, helpers gekopieerd); placeholders nu, definitief in change 37 | Eén script per ring; ring 1-textures blijven onaangeroerd |

Commit (na `/code-review`): `blocks: add canyon stone, scorched sandstone and desert flora`.
