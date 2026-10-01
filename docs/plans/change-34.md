# Change 34 — Dauwbron en dauwkristal

Scope: `blocks` — change 34

**Doel:** het sleutelmateriaal van Nether-ring 2 bestaat als speelbaar mechaniek: de **dauwbron** (`dew_source`), een eigen natuurblok dat water-achtig oogt en druppelt maar **geen waterfluid** is, laat via random ticks langzaam **dauwkristal** groeien op zijn zijkanten, in stadia zoals budding amethyst (knop → … → cluster). Met blockstates/models, placeholder-textures van eigen pixels (ring 2-texturescript), loot, tags, lang (en + nl), creative tab en een **minimaal placeholder-item** `dew_crystal` als drop (nut in fase 2 → backlog). Plaatsing in de wereld volgt in change 35 (dauwgeodes in Nether-ring 2, 35 vraag 6 B), definitieve textures in change 37.

Bron: `fase-1-wereld.md` §2 Ring 2 → "Nether-tegenpool — Nether-ring 2" ("Dauwbron: eigen natuurblok dat water-achtig oogt en druppelt; geen echte waterfluid, de Nether blijft `ultrawarm`" en "Sleutelmateriaal: dauwkristal, groeit langzaam aan dauwbronnen (zoals budding amethyst). Nodig voor bewaker 2→3; in fase 2 de basis van hittebescherming voor ring 3"), "Client/server-risico's" ("dauwkristalgroei … random ticks, server-side"), §1.4 (textures, creative tab), §1.12, bouwplan rij 34 (B, D, S); beslissingenlog 2026-09-30 ("Dauwbronnen in Nether-ring 2 als eigen blok", "Sleutelmateriaal Nether-2: dauwkristal"). Sjablonen: change 29 (`salt_crystal` via vanilla `AmethystClusterBlock`), change 15 (placeholder-item als drop, `c:gems/…`), change 10 (`DeadGrassBlock`: eigen class met `CODEC` + `randomTick`).

---

## Afstemming (2026-10-01)

Afgestemd met 29, 33, 35 en 37 (basis: de aanbevolen opties).
- **Registry-naam `dew_source` is het contract.** Plan 35 noemde het blok en de feature `dew_spring`; dat is aangepast. Na 35 vraag 6 = B (keuze gebruiker) plaatst 35 de bronnen via de geode-feature `corruptedadventure:dew_geode` (class `DewGeodeFeature`) en de density function `dew_geodes`; er is geen losse `dew_source`-feature meer.
- **Stadia in `#corruptedadventure:fungal_growths`** (tag van 33), nodig voor de determinismeregel van 35; `dew_source` niet (terrein).
- **Kristal-helper `crystalCluster(block)` uit 29** hergebruiken (blockstate, kruismodel, itemmodel met parent `minecraft:item/amethyst_bud`); geen eigen variant.
- **Vorm** (`AmethystClusterBlock`-maten) is `blocks` en hier definitief; 37 tekent binnen de vorm.
- **Kleur en gloed:** het werkpalet is placeholder; de kleur (onderscheid met zielkristal) is 37 vraag 2, emissive/puls 37 vraag 1. Vraag 6 gaat alleen over bloklicht (optie C verwijst naar 37).
- `Ring2Textures.java` bestaat (21); placeholders `still`.
- **Plaatsing in geodes (35 vraag 6 = B, keuze gebruiker):** change 35 zet dauwbronnen niet los op de vloer, maar in de **binnenwand van eigen deterministische geodes** (`dew_geode`, holle bollen in de wanden, zoals amethist-geodes), met voorgegroeide knoppen naar binnen gericht. Dit plan verandert daardoor niet: `dew_source` is al sturdy op alle zijden en de stadia groeien vanaf elke zijde de holte in (vanilla-amethistpatroon). Wel minder bronnen per gebied, maar meer per vindplaats.
- **Losse items (24 vraag 3 = B):** vóór `dew_crystal` staan `soul_crystal`, `raw_sunstone`, `raw_turquoise`, `salt`.
- Teststop: geen; de groeicheck in-game gaat mee in de gebundelde eindcheck ring 2 + Nether-2 (teststop 3 ná change 37).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op 2026-10-01 (stand na change 17, commit `7714b77`). **Changes 18–33 zijn nog niet gebouwd; hun plannen zijn de bron (aanname).** Bij de start de echte namen en helpers uit `CODEBASE.md` nalopen en dit plan daarop aanpassen; afwijkingen noteren onder "Afwijkingen bij de bouw".

- **Blok-infrastructuur (gebouwd, change 10/15):** `ModBlocks.register(name, supplier)` (blockitem komt vanzelf), registratievolgorde = creative-tab-volgorde; losse items via `ModItems.ITEMS.registerSimpleItem`, **vóór** de blockitems (valkuil in `CODEBASE.md`); `ModCreativeTabs` toont eerst alle blokken, dan de losse items. Datagen: `ModBlockStateProvider`, `ModItemModelProvider` (`blockItem`, `basicItem`), `ModBlockLootProvider` (`getKnownBlocks` = alle `ModBlocks`: **elk blok een table**, ook een lege), `ModBlockTagsProvider`, `ModItemTagsProvider` (`copy`), `ModTags` (`common(path)`), `EnglishLanguageProvider`/`DutchLanguageProvider`. Test `ModBlocksTest`.
- **Eigen block-class met random tick (gebouwd, change 10):** `DeadGrassBlock` — `public static final MapCodec<…> CODEC = simpleCodec(…::new)`, `codec()`-override, `randomTick(BlockState, ServerLevel, BlockPos, RandomSource)`. Zelfde patroon voor `DewSourceBlock`.
- **Change 21 (aanname, plan):** `tools/textures/Ring2Textures.java` bestaat (opzet als `Ring1Textures`: `TEXTURES`, `still`/`pulsing`, vaste seed per bestandsnaam, `HAND_EDITED`, paletten bovenin, contactblad `build/texture-preview/ring2.png`). Change 21 noemt expliciet dat **33/34 (Nether-ring 2) hetzelfde script uitbreiden**. Deze change voegt daar de dauwbestanden toe.
- **Change 29 (aanname, plan):** `salt_crystal` = vanilla `AmethystClusterBlock`, blockstate via `directionalBlock(…, cross-model cutout)`, item als plat model met de bloktexture. Deze change gebruikt **dezelfde aanpak** voor de dauwkristal-stadia. Losse items op dat moment (aanname changes 15/24/29; 24 vraag 3 B): `soul_crystal`, `raw_sunstone`, `raw_turquoise`, `salt`; `dew_crystal` komt **achteraan** de losse items.
- **Change 33 (plan, afgestemd 2026-10-01):** maakt een sectie `// ---- Nether ring 2 ----` in `ModBlocks` (`soaked_netherrack`, `spore_moss`, `dripshroom_*`-set, `dripshroom_cap`, `dripshroom_lamp`, `glow_mold`, `slime_stalactite`) en de blok-tag `corruptedadventure:fungal_growths`. De dauwblokken komen **achteraan die sectie**. Bestaat de sectie niet, dan zelf aanmaken ná de ring 2-blokken. Raakvlak: sporenmos spreidt via random ticks; de dauwbron hoort in **geen** tag waarop sporenmos kan spreiden (`#minecraft:nylium`, `#minecraft:base_stone_nether`, eventuele eigen tag van change 33).
- **Change 35 (aanname, toekomst; 35 vraag 6 B):** plaatst dauwbronnen (en voorgegroeide kristallen) in de binnenwand van eigen dauwgeodes in de biomes reuzenzwamwoud en sporengrotten; verwijst naar blokken **op registry-naam** en op `facing`. Registry-namen en blockstates van deze change zijn dus het contract met change 35.
- **Change 36 (aanname):** Nether-2-sfeer (druppelgeluiden, sporen-particles) als biome-data; los van het druppelen van het blok zelf (vraag 7).
- **Change 37 (aanname):** maakt de textures definitief onder **dezelfde bestandsnamen**; kiest daar eventueel emissive/animatie voor kristal en dauwbron.
- **Items/recipes horen bij `items` (fase 2)** → het item `dew_crystal` is een placeholder (precedent `soul_crystal`, change 15), zonder recipes; backlog.
- Registry-namen met het thema `dew_`. Dauwbron = `dew_source` (letterlijk, en analoog aan de "bron" van budding amethyst). Stadia volgens vanilla-patroon: `small_dew_crystal_bud`, `medium_dew_crystal_bud`, `large_dew_crystal_bud`, `dew_crystal_cluster`. Het item heet `dew_crystal` (= het sleutelmateriaal uit het design-doc), niet `dew_crystal_shard` (dat zou een groter "dauwkristal" suggereren dat niet bestaat).
- `MapColor`: dauwbron `MapColor.WATER`, kristallen `MapColor.COLOR_LIGHT_BLUE` (alleen kaartkleur; geen ontwerpkeuze, aan te passen in change 37).

---

## Plan

### 1. Blokken (`registry/ModBlocks`, achteraan de sectie `// ---- Nether ring 2 ----`)

Volgorde (= creative tab): dauwbron, kleine knop, middelgrote knop, grote knop, cluster. Lijst = aanbevolen opties (vraag 1, 2, 3, 6).

| Registry-naam | Class | Basis (`ofFullCopy`) | Opmerking |
|---|---|---|---|
| `dew_source` | **`DewSourceBlock`** (nieuw, `block/`) | `budding_amethyst` (sterkte 1,5, `requiresCorrectToolForDrops`, `pushReaction DESTROY`) + `MapColor.WATER` + **expliciet `.randomTicks()`** (niet op de copy vertrouwen) | vraag 1 A: vol, opaak blok; vraag 2 A: dropt niets; licht vraag 6; druppelen vraag 7 |
| `small_dew_crystal_bud` | vanilla `AmethystClusterBlock(3, 4, …)` — **geen eigen class** | `small_amethyst_bud` (geluid, `noOcclusion`, `forceSolidOn`, `pushReaction DESTROY`) + `MapColor.COLOR_LIGHT_BLUE` | `facing` (6) + `waterlogged`; breekt af zonder steun; licht vraag 6 |
| `medium_dew_crystal_bud` | `AmethystClusterBlock(4, 3, …)` | `medium_amethyst_bud` + idem | idem |
| `large_dew_crystal_bud` | `AmethystClusterBlock(5, 3, …)` | `large_amethyst_bud` + idem | idem |
| `dew_crystal_cluster` | `AmethystClusterBlock(7, 3, …)` | `amethyst_cluster` + idem | eindstadium; standaard-state `facing=up` |

- Maten (hoogte/offset) = vanilla-amethist; de vorm is hier definitief (vormen zijn `blocks`); change 37 tekent alleen binnen deze vorm.
- Licht expliciet per stadium zetten (`lightLevel(s -> n)`), ook als het gelijk is aan de copy, zodat de waarden in de code zichtbaar zijn (vraag 6).
- **Bij vraag 3 B/C** vervallen stadia (zie vraag); de groeilijst in `DewSourceBlock` volgt.

### 2. `block/DewSourceBlock` (nieuw)

Één verantwoordelijkheid: kristal laten groeien. `extends Block` (niet `BuddingAmethystBlock`/`AmethystBlock`: geen amethist-belgeluid bij een projectiel, en de vanilla-class verwijst hard naar de amethistknoppen).

- `CODEC = simpleCodec(DewSourceBlock::new)` + `codec()`-override (patroon `DeadGrassBlock`).
- `randomTick(state, level, pos, random)` — gedrag als vanilla `BuddingAmethystBlock` (1.21.1; bij de bouw de vanilla-code naast leggen):
  1. `if (random.nextInt(GROWTH_CHANCE) != 0) return;` — `GROWTH_CHANCE` = 5 (vraag 5 A; constante in de class).
  2. `Direction dir = Direction.values()[random.nextInt(6)]`, buurpositie `pos.relative(dir)`.
  3. `BlockState next = grownState(level.getBlockState(neighbour), dir)`; niet `null` → `level.setBlockAndUpdate(neighbour, next)`.
- **Pure functie** `static @Nullable BlockState grownState(BlockState neighbour, Direction dir)` (package-private of public, unit-testbaar zonder level):
  - buur is lucht of een volle waterbron (`BuddingAmethystBlock.canClusterGrowAtState`, public static in vanilla — hergebruiken, bij de bouw verifiëren) → `small_dew_crystal_bud` met `facing=dir`, `waterlogged` = buur was water;
  - buur is een dauwkristal-stadium **met `facing == dir`** → het volgende stadium, `facing` en `waterlogged` behouden;
  - anders (cluster, ander blok, knop met andere `facing`, lava, stromend water) → `null`.
  - Stadia als vaste lijst `List<Supplier<Block>>` in de class (volgorde = groei), via `ModBlocks`-suppliers (geen klasse-init-probleem: alleen opgevraagd in `randomTick`/de functie).
- Geen ring-check (vraag 2 A: de dauwbron is niet verkrijgbaar, dus hij staat alleen waar worldgen hem neerzet); bij vraag 2 C komt er een `RingLayout.ringAt(…)`-check bij.
- Water in de Nether: niet relevant (de Nether laat geen water toe, ook niet in waterlogbare blokken: `BucketItem` verdampt eerst). De waterregel geldt alleen buiten de Nether (creative).
- **Vraag 7 A:** `animateTick(state, level, pos, random)` — met kans 1/10 per frame, alleen als het blok eronder geen sturdy bovenkant heeft: `ParticleUtils.spawnParticleBelow(level, pos, random, ParticleTypes.DRIPPING_WATER)` (patroon vanilla-bladeren in de regen). `animateTick` is een common methode die vanilla alleen client-side aanroept; **geen** client-class, alleen `ParticleTypes` (common registry).

### 3. Item (`registry/ModItems`) — bij vraag 4 A/B

- `dew_crystal` via `ITEMS.registerSimpleItem("dew_crystal")`, **vóór** de blockitems, achteraan de losse items (ná `salt`, aanname change 29). **Placeholder**; javadoc: "Placeholder: use, recipes and balance follow in phase 2".
- Conventietags: `c:gems/dew_crystal` (`ModTags.Items.GEMS_DEW_CRYSTAL` via `common("gems/dew_crystal")`), via `addTag` in `c:gems` (patroon `c:gems/soul_crystal`).

### 4. Tags (`ModBlockTagsProvider`, `ModItemTagsProvider`)

Blok-tags:
- `#minecraft:mineable/pickaxe`: `dew_source` en alle vier stadia (zoals vanilla-amethist).
- NeoForge-conventie (vanilla-amethist zit erin): `c:budding_blocks` ← `dew_source`; `c:buds` ← de drie knoppen; `c:clusters` ← `dew_crystal_cluster`. **Bij de bouw verifiëren** dat `Tags.Blocks.BUDDING_BLOCKS`/`BUDS`/`CLUSTERS` in NeoForge 21.1.252 bestaan; zo niet, weglaten en in de backlog noteren.
- **Niet** in `#minecraft:nylium`, `#minecraft:base_stone_nether`, `#minecraft:nether_carver_replaceables`, `#minecraft:features_cannot_replace` of een sporenmos-tag van change 33 (sporenmos spreidt niet over de bron; carvers zijn al klaar voordat change 35 hem plaatst).
- **Niet** in `#minecraft:crystal_sound_blocks` (amethist-loopgeluid; de dauwbron is "water", geen kristal).

Item-tags: `copy` van `c:budding_blocks`/`c:buds`/`c:clusters` (indien ze bestaan); `c:gems/dew_crystal` + `c:gems` (vraag 4).

Geen nieuwe eigen `TagKey` voor de stadia: `DewSourceBlock` kent zijn eigen lijst, change 35 matcht op registry-naam. **Wel (afgestemd 2026-10-01):** de vier stadia komen in de bestaande blok-tag `corruptedadventure:fungal_growths` van change 33 ("groeisel" voor de determinismeregel van change 35); `dew_source` niet (die is terrein).

### 5. Loot (`ModBlockLootProvider`)

Bij vraag 2 A en 4 A (aanbevolen), 1-op-1 vanilla-amethist:
- `dew_source`: `add(DEW_SOURCE, noDrop())` — lege table (vanilla `budding_amethyst`), ook met Silk Touch niets.
- `small/medium/large_dew_crystal_bud`: `dropWhenSilkTouch(…)` — alleen met Silk Touch zichzelf, anders niets.
- `dew_crystal_cluster`: `createSilkTouchDispatchTable(cluster, LootItem.lootTableItem(DEW_CRYSTAL).apply(SetItemCountFunction.setCount(ConstantValue.exactly(4))).apply(ApplyBonusCount.addOreBonusCount(fortune)).when(MatchTool.toolMatches(ItemPredicate.Builder.item().of(ItemTags.CLUSTER_MAX_HARVESTABLES))).otherwise(applyExplosionDecay(cluster, LootItem.lootTableItem(DEW_CRYSTAL).apply(SetItemCountFunction.setCount(ConstantValue.exactly(2))))))` — pickaxe → 4 (Fortune), anders 2, Silk Touch → cluster.

Varianten: vraag 2 B/C → `dew_source` krijgt `dropWhenSilkTouch`; vraag 4 B → cluster 1 (Fortune, `createOreDrop`-achtig); vraag 4 C → alles `dropSelf`/Silk Touch, geen item.

### 6. Models en blockstates (datagen)

- `ModBlockStateProvider`:
  - `dew_source` (vraag 1 A): `simpleBlock` (`cube_all`, `block/dew_source`). Bij vraag 1 B: `cube_all` met `renderType("translucent")` en een `HalfTransparentBlock`-basis (zie vraag).
  - Stadia: `directionalBlock(block, models().cross(name, modLoc("block/" + name)).renderType("cutout"))` — rotaties gelijk aan vanilla's `amethyst_cluster`-blockstate (up 0, down x180, noord x90, oost x90 y90, zuid x90 y180, west x90 y270; bij de bouw tegen de vanilla-JSON controleren); `waterlogged` krijgt hetzelfde model. Gebruik de helper `crystalCluster(block)` uit change 29 (afgestemd; ontbreekt hij bij de bouw, dan stoppen en vragen).
- `ModItemModelProvider`: `dew_source` via `blockItem`; stadia als plat item met de bloktexture, parent `minecraft:item/amethyst_bud` (vanilla-display-transforms), `layer0` = `block/<naam>` (gelijk aan change 29, via dezelfde helper); `dew_crystal` via `basicItem`.

### 7. Placeholder-textures (`tools/textures/Ring2Textures.java` uitbreiden, Nether-ring 2-deel)

- Nieuwe entries in `TEXTURES`, alle `still` (animatie/emissive = change 37); vaste seed per bestandsnaam; bestaande ring 2-PNG's en `Ring1Textures.java` blijven byte-identiek.
- **Werkpaletten** (constanten bovenin, geen beslissing — definitief in change 37): dauwbron (water-achtig, opaak) `173F52 1F5F78 2E86A6 4FAFC8 86D2E0` met lichte glinsterpixels `D6F4FA`; dauwkristal `2A7F99 4FB8D0 8FDDEB C6F1FA E8FBFF`.
- Patronen: dauwbron = golvende waterlijnen + glinsterpunten op donker blauwgroen (leest als "stilstaand water in een blok"); stadia = 1 / 2 / 3 / 4–5 puntige kristallen in kruismodel-vorm, oplopend in grootte (kristalstijl §1.4: donkere 1-px-omlijning, lichte linkerkant, schaduw rechts, highlight op de punt), alpha alleen 0/255 (cutout); item = één kristal met facetten.

| Bestand | Voor |
|---|---|
| `block/dew_source.png` | dauwbron (alle zijden) |
| `block/small_dew_crystal_bud.png`, `block/medium_dew_crystal_bud.png`, `block/large_dew_crystal_bud.png`, `block/dew_crystal_cluster.png` | stadia (kruismodel + item; cutout) |
| `item/dew_crystal.png` | item `dew_crystal` (vraag 4 A/B) |

6 PNG's (5 bij vraag 4 C; minder bij vraag 3 B/C), geen animaties. Volgorde: **eerst het script draaien, dan `runData`** (`ExistingFileHelper`).

### 8. Lang (`EnglishLanguageProvider`, `DutchLanguageProvider`)

Commentaarblok "Nether ring 2 dew". Nederlands volgens de vanilla-`nl_nl`-terminologie voor de amethiststadia (opzoeken in de asset-cache, valkuil in `CODEBASE.md`; onderstaande nl-namen indicatief).

| Key | en_us | nl_nl (indicatief) |
|---|---|---|
| `dew_source` | Dew Source | Dauwbron |
| `small_dew_crystal_bud` | Small Dew Crystal Bud | Kleine dauwkristalknop |
| `medium_dew_crystal_bud` | Medium Dew Crystal Bud | Middelgrote dauwkristalknop |
| `large_dew_crystal_bud` | Large Dew Crystal Bud | Grote dauwkristalknop |
| `dew_crystal_cluster` | Dew Crystal Cluster | Dauwkristalcluster |
| `dew_crystal` (item) | Dew Crystal | Dauwkristal |

### 9. Tests in code

Nieuw: `src/test/java/nl/timtoys/corruptedadventure/block/DewSourceBlockTest.java` (FML-omgeving, mod-blokken geregistreerd zoals in `ModBlocksTest`):
- `grownState(air, d)` → `small_dew_crystal_bud[facing=d, waterlogged=false]`, voor alle 6 richtingen.
- `grownState(water-bron, d)` → kleine knop met `waterlogged=true`; stromend water (niveau < 8) → `null`.
- keten: klein → middel → groot → cluster, `facing` en `waterlogged` behouden; cluster → `null`.
- knop met `facing ≠ d` → `null` (groeit alleen vanaf de eigen bron-zijde, zoals vanilla).
- lava, steen, `dew_source` → `null`.
- `dew_source.defaultBlockState().isRandomlyTicking()` is `true`; `getPistonPushReaction()` = `DESTROY`.
- stadia: standaard-state `facing=up`; `lightEmission` per stadium = gekozen waarden (vraag 6); dauwbron idem.

`randomTick` zelf (kansrol + richting + `setBlockAndUpdate`) is te dun voor een eigen test en vraagt een `ServerLevel`; die dekt de in-game check (W-achteraf).

`ModBlocksTest` dekt blockitem + tabvolgorde vanzelf (alle nieuwe blokken hebben een item).

### 10. Documentatie

- `CODEBASE.md`: `block/DewSourceBlock` in de mappenstructuur + "Belangrijke classes" (random tick 1/`GROWTH_CHANCE`, `grownState`, stadia-lijst, `animateTick`); blok-tabel (Nether-ring 2: dauwbron + 4 stadia: class, basis, loot, tags, licht), item-tabel (`dew_crystal`: placeholder), `ModTags` (`c:gems/dew_crystal`), textures-regel (`Ring2Textures.java`: dauwbestanden), test `block/DewSourceBlockTest`, "Laatst bijgewerkt". Valkuilen: groei alleen met een speler in de buurt (random ticks, zie bestaande valkuil) — testen met `/gamerule randomTickSpeed`; `AmethystClusterBlock` speelt het amethist-belgeluid bij een projectiel-treffer (zoals change 29).
- `fase-1-wereld.md` §2 Ring 2 → Nether-ring 2: **Uitwerking (change 34)** met blokken, groeiregel en -snelheid, stadia, drops, licht, druppelen en de overdracht naar change 35/37 (zie hieronder).
- `02-beslissingenlog.md`: de regels hieronder.
- `docs/backlog.md`:
  - `[items] Dauwkristal (dew_crystal, placeholder uit change 34): nut en recipes in fase 2 — basis van hittebescherming voor ring 3; eventueel een dauwkristalblok (opslag/bouwblok, zoals amethistblok). (bron: change 34)` — of de bestaande dauwkristal-regel (regel 20) aanvullen.
  - `[items] Idee: fles vullen bij een dauwbron (water in de Nether). (bron: change 34)`.
  - Afgewezen opties als later-idee (bijv. verplaatsbare dauwbron, ring-gebonden groei).
  - Bij vraag 7 B: `[atmosphere] Druppel-particles van de dauwbron (animateTick). (bron: change 34)`.

### Overdracht naar latere changes

| Change | Wat ligt er klaar |
|---|---|
| 35 dauwgeodes (`worldgen`) | `corruptedadventure:dew_source` (vol blok, sturdy op alle zijden, niet verkrijgbaar bij vraag 2 A → elke bron in de wereld komt uit worldgen). Change 35 (vraag 6 B) zet de bronnen in de binnenwand van eigen `dew_geode`-holtes. Kristallen al gegroeid plaatsen kan met `corruptedadventure:<stadium>[facing=<richting van bron af>]` naast een bron (`AmethystClusterBlock.canSurvive` vraagt een sturdy steun aan de achterkant); groei loopt daarna vanzelf zodra een speler in de buurt is. De bron zit in **geen** carver-/stone-/nylium-tag: als feature ná de carvers plaatsen. Geen `RingLayout` in het blok: de verdeling (alleen Nether-ring 2) is volledig aan change 35. |
| 36 sfeer (`atmosphere`) | Druppelgeluid hoort bij de biome-data. Bij vraag 7 A druppelt het blok al zichtbaar (`DRIPPING_WATER`); change 36 hoeft alleen geluid/biome-particles. |
| 37 textures | Bestandsnamen hierboven zijn definitief; change 37 vervangt alleen pixels en kiest eventueel emissive (kristallen, patroon `oreWithOverlay`) en een animatie (`.mcmeta`) voor de dauwbron. |

---

## Client/server-impact

- Blokken en item zijn common registry-entries; NeoForge synct de registry-ID's bij het joinen.
- **Groei is server-side:** `randomTick` krijgt een `ServerLevel` en draait alleen op de server; de nieuwe blockstate gaat via vanilla's blok-update naar de clients. Geen eigen netwerk, geen SavedData/attachments.
- `AmethystClusterBlock` (`canSurvive`/`updateShape`) en loot: vanilla, server-side gezag.
- `animateTick` (vraag 7 A) staat in de common class maar wordt door vanilla alleen client-side aangeroepen; gebruikt alleen `ParticleTypes`/`ParticleUtils`/`Level#addParticle` (common). **Geen** `Minecraft.getInstance()`, geen import uit `client/`; een dedicated server laadt niets extra (test S bevestigt dat).
- Render type staat in de model-JSON (`cutout` voor de stadia; bij vraag 1 B `translucent`), kleuren in de PNG's: geen client-registratie.
- Het texturescript staat buiten `src/` en komt niet in de jar.

---

## Tests

Bouwplan: **B, D, S**.

- [ ] **Script** — `java tools/textures/Ring2Textures.java` draait; 6 nieuwe PNG's van 16×16; stadia alleen alpha 0/255; tweede run → `git status` schoon; bestaande ring 2-PNG's en `Ring1Textures.java` ongewijzigd; contactblad toont de dauwset.
- [ ] **B** — `./gradlew build` slaagt, incl. `DewSourceBlockTest` (groeiregel, random-tick-vlag, push-reactie, licht) en `ModBlocksTest` (nieuwe blokken met blockitem, in de tab achteraan Nether-ring 2; `dew_crystal` achteraan de losse items).
- [ ] **D** — `./gradlew runData` slaagt zonder "texture does not exist"; gegenereerd en gecommit: blockstates (stadia: 12 states elk, rotaties = vanilla `amethyst_cluster`), models (stadia `render_type` cutout), item-models (stadia plat met parent `item/amethyst_bud`, `dew_crystal`), loot tables (`dew_source` leeg; knoppen alleen Silk Touch; cluster: Silk Touch → cluster, `match_tool` `#cluster_max_harvestables` → 4 + `apply_bonus` Fortune, anders 2 + `explosion_decay`), tags (`mineable/pickaxe`, `c:budding_blocks`/`c:buds`/`c:clusters` indien aanwezig, `c:gems/dew_crystal` + `c:gems`, item-copy), lang en/nl. Eerdere gegenereerde bestanden ongewijzigd (`git diff`: alleen nieuwe regels in gedeelde tag-/lang-bestanden).
- [ ] **S** — `tools/server-smoke-test.sh` met `level-name=smoke-change34`: server start zonder fouten of client-class-meldingen (bevestigt dat `animateTick` geen client-class trekt). Via RCON (daarna RCON weer uit):
  - tags: `setblock` + `execute if block <pos> #minecraft:mineable/pickaxe` (bron + stadia), `#c:budding_blocks`/`#c:buds`/`#c:clusters`; bron **niet** in `#minecraft:base_stone_nether`/`#minecraft:nylium`;
  - loot: `loot insert <kist> mine <cluster> minecraft:iron_pickaxe` → 4 `dew_crystal`; met Fortune III → 4–16 (meerdere keren); met `minecraft:stick` → 2; met Silk Touch → `dew_crystal_cluster`; kleine knop met pickaxe → niets, met Silk Touch → knop; `dew_source` met Silk Touch → niets (vraag 2 A);
  - overleven: `setblock … dew_crystal_cluster[facing=north]` tegen een dauwbron, bron weghalen (`setblock … air`) → cluster weg;
  - **groei niet via RCON:** geforceloade chunks zonder speler krijgen geen random ticks (valkuil in `CODEBASE.md`), dus een speler-loze groeitest via `randomTickSpeed` geeft vals-negatief en is **niet betrouwbaar**. De groeiregel dekt de unit test; de echte random tick de in-game check hieronder.
- **Blokkerende handmatige test: nee.** De groeilogica zit in een pure functie met unit tests; de koppeling (random-tick-vlag op het blok, `randomTick`-override met `@Override`) is in de unit test resp. door de compiler gedekt. Change 35 bouwt alleen op registry-namen, `facing` en het overleven van de stadia — allemaal automatisch getest (B, D, S). Uiterlijk is placeholder (change 37), groeisnelheid is balans. Wel aanbevolen: de groeicheck uitvoeren bij de eerstvolgende runClient-sessie, uiterlijk in teststop 3 (na change 37). → **W-achteraf**, in-game checklist (runClient, creative + survival, testwereld seed `20260930`):
  - [ ] Creative tab: dauwbron + 4 stadia achteraan Nether-ring 2; `dew_crystal` achteraan de losse items; namen en/nl.
  - [ ] **Groei:** dauwbron vrij in de lucht zetten (ook in de Nether, bijv. `/ca tpring 2 nether`), `/gamerule randomTickSpeed 4096`, speler ernaast → binnen ~1 minuut knoppen op meerdere zijden, die doorgroeien tot clusters met de juiste oriëntatie (ook onderkant/plafond); een knop die door iets anders geplaatst is met een andere `facing` groeit niet door; volle clusters groeien niet verder. Daarna `/gamerule randomTickSpeed 3`.
  - [ ] Een bron omringd door blokken groeit niet; een blok weghalen → op die zijde begint de groei.
  - [ ] Mijnen in survival: bron met pickaxe → niets (ook Silk Touch); cluster met pickaxe → 4 dauwkristal, met de hand → 2; knoppen zonder Silk Touch → niets.
  - [ ] Piston duwt de bron → bron vernietigd (geen drop); stadia ook.
  - [ ] Druppelen (vraag 7 A): onder een bron met lucht eronder vallen af en toe waterdruppels, ook in de Nether; geen druppels als er direct een vol blok onder zit.
  - [ ] Licht (F3, vraag 6) per stadium; geen paars-zwart; kruismodellen zonder zwarte randen.

---

## Risico's / aandachtspunten

- **Random ticks alleen bij spelers:** groei gebeurt alleen in chunks binnen ~128 blokken van een speler (vanilla). Gewenst ("langzaam, ter plekke oogsten"), maar het maakt geautomatiseerd testen zonder client onmogelijk (zie Tests).
- **`ofFullCopy` en de random-tick-vlag:** of `ofFullCopy(budding_amethyst)` `isRandomlyTicking` meeneemt, niet aannemen: `.randomTicks()` expliciet zetten; de unit test bewaakt het.
- **`BuddingAmethystBlock.canClusterGrowAtState`** is in 1.21.1 public static (verifiëren); anders de twee regels zelf schrijven (lucht of volle waterbron).
- **`AmethystClusterBlock` hergebruiken:** amethist-belgeluid bij een projectiel-treffer (vanilla `AmethystBlock#onProjectileHit`); klein en past bij "kristal". De dauwbron zelf erft niet van `AmethystBlock`, dus geen belgeluid.
- **Dauwbron onverkrijgbaar** (vraag 2 A): een speler die de bron per ongeluk breekt, is hem kwijt (vanilla-gedrag van budding amethyst). Dat houdt het sleutelmateriaal aan Nether-ring 2 gebonden; change 35 moet genoeg bronnen plaatsen (bij geodes, 35 vraag 6 B: zeldzamer, maar meerdere bronnen per geode).
- **Groeisnelheid** (vraag 5): vanilla-tempo betekent gemiddeld ~34 minuten per stadium per zijde met een speler in de buurt (random tick ~elke 68 s × 1/5 kans × 1/6 richting), dus ~2,3 uur tot een cluster op één zijde; met 6 vrije zijden gemiddeld ~1 cluster per ~23 minuten per bron. Balans; bijstellen = één constante.
- **Druppel-particles** (vraag 7 A) zijn strikt genomen sfeer (`atmosphere`), maar het design-doc beschrijft "druppelt" als eigenschap van het blok; `animateTick` is blokgedrag in een common class (vanilla-patroon). Bij twijfel vraag 7 B.
- **Afhankelijk van niet-gebouwde changes** (21, 29, 33): als `Ring2Textures.java`, de Nether-ring 2-sectie of de kristal-helper van change 29 anders heten of ontbreken, de echte stand volgen; ontbreekt `Ring2Textures.java` helemaal, dan **stoppen en vragen** (het script hoort bij change 21).
- **NeoForge-conventietags** `c:budding_blocks`/`c:buds`/`c:clusters`: bij de bouw verifiëren; bij twijfel weglaten.

---

## Ontwerpvragen

### 1. Welke vorm en welk uiterlijk krijgt de dauwbron?
Design: "eigen natuurblok dat water-achtig oogt en druppelt; geen echte waterfluid".
- **A. Vol, opaak blok met een water-achtige texture (aanbevolen).** Eenvoudigst (`cube_all`, geen render-type-gedoe); kristallen groeien op alle zes zijden zoals bij amethist; mobs kunnen erop lopen. Change 37 kan de texture laten golven (`.mcmeta`) zonder code.
- **B. Half-doorzichtig blok zoals ijs/honing** (`HalfTransparentBlock`, `translucent`, `noOcclusion`). Leest sterker als water; wel extra model-/renderwerk (vlakken tussen twee bronnen wegcullen, licht valt erdoor), sorteringsartefacten met andere doorzichtige blokken.
- **C. Lage "plas" (bijv. 14/16 hoog, eigen vorm)** waar kristallen alleen aan de bovenkant/zijkanten groeien. Natuurlijkst als bron, maar eigen vormen, `canSurvive`-randgevallen voor de onderkant en meer werk voor change 35.

Antwoord: **A** — vol, opaak blok met een water-achtige texture (`cube_all`).

### 2. Kan een speler de dauwbron meenemen?
- **A. Nee: dropt niets, ook niet met Silk Touch; een piston vernietigt hem (aanbevolen).** Exact budding amethyst. Dauwkristal blijft gebonden aan Nether-ring 2 (ring-progressie intact, geen kristalfarm naast je overworld-basis). Breken = kwijt.
- **B. Ja, met Silk Touch → zichzelf.** Spelers bouwen een farm waar ze willen (ook in ring 1); ondergraaft het idee dat je het sleutelmateriaal in de Nether-ring moet halen.
- **C. Ja met Silk Touch, maar groeit alleen in Nether-ring 2** (`RingLayout.ringAt` in `randomTick`). Verplaatsbaar binnen de ring, progressie intact; extra logica + test, en spelers begrijpen niet direct waarom een meegenomen bron "dood" is.

Antwoord: **A** — nee: dropt niets (ook niet met Silk Touch), een piston vernietigt hem; exact budding amethyst.

### 3. Hoeveel groeistadia heeft dauwkristal?
- **A. Vier, zoals amethist: kleine, middelgrote, grote knop + cluster — elk een vanilla `AmethystClusterBlock` (aanbevolen).** Herkenbaar voor spelers ("zoals amethist" uit het design), geen eigen class (precedent zoutkristal change 29), 4 textures.
- **B. Twee: knop + cluster.** Minder blokken en textures; groei voelt sneller "klaar" tenzij de kans omlaag gaat; minder zichtbare voortgang.
- **C. Eén blok met een `age`-property (0–3).** Eén registry-entry, maar eigen class (vorm per leeftijd, eigen codec) en één item voor alle stadia; wijkt af van het vertrouwde amethistpatroon.

Antwoord: **A** — vier stadia zoals amethist (kleine, middelgrote, grote knop + cluster), elk vanilla `AmethystClusterBlock`.

### 4. Wat dropt het rijpe dauwkristal — komt er al een item?
- **A. Placeholder-item `dew_crystal` (nut in fase 2, backlog); cluster met pickaxe → 4 (Fortune), anders 2, Silk Touch → cluster; knoppen alleen met Silk Touch (aanbevolen).** 1-op-1 amethist; spelers verzamelen al voor fase 2 en bewaker 2→3; `c:gems/dew_crystal` voor compatibiliteit.
- **B. Placeholder-item, maar 1 per cluster (Fortune 1–4).** Schaarser, voelt meer als zeldzaam sleutelmateriaal; samen met de trage groei kan het grindy worden.
- **C. Geen item: cluster dropt zichzelf.** Kleinst; fase 2 moet de loot aanpassen en het item toevoegen; tot dan geen "oogst".

Antwoord: **A** — placeholder-item `dew_crystal`; cluster met pickaxe → 4 (Fortune), anders 2, Silk Touch → cluster; knoppen alleen met Silk Touch; `c:gems/dew_crystal`.

### 5. Hoe snel groeit dauwkristal?
- **A. Als amethist: kans 1/5 per random tick, één willekeurige zijde (aanbevolen).** Gemiddeld ~34 min per stadium per zijde, ~1 cluster per ~23 min per vrije bron (speler in de buurt). Vertrouwd tempo; één constante.
- **B. Half zo snel (1/10).** "Langzaam" sterker benadrukt; met trage oogst en weinig bronnen kan bewaker 2→3 lang duren.
- **C. Configureerbaar** (`CommonConfig`, standaard 1/5). Balans aanpasbaar per server; extra config-sleutel en documentatie, niet per wereld bevroren (geen invloed op worldgen, dus dat mag).

Antwoord: **A** — als amethist: kans 1/5 per random tick, één willekeurige zijde (`GROWTH_CHANCE` = 5).

### 6. Geven dauwbron en dauwkristal licht?
In de Nether heeft bloklicht geen effect op monster-spawns; het is puur zicht/vindbaarheid.
- **A. Stadia als amethist (klein 1, middel 2, groot 4, cluster 5), dauwbron 0 (aanbevolen).** Rijpe clusters zijn van ver te zien in donkere sporengrotten; de bron zelf blijft "water".
- **B. Als A, en de dauwbron licht 3** (zoals het zielkristal-erts). Bronnen zijn vindbaar, ook zonder kristal; de bron oogt minder als water.
- **C. Geen licht; gloed alleen via emissive textures in change 37.** Geen invloed op lichtberekening; zonder emissive (Sodium/shaders) onzichtbaar in het donker.

Antwoord: **A** — stadia als amethist (klein 1, middel 2, groot 4, cluster 5), dauwbron 0.

### 7. Druppelt de dauwbron zichtbaar, en in welke change?
- **A. Ja, in deze change: `animateTick` met vanilla `DRIPPING_WATER` onder de bron als daar ruimte is (aanbevolen).** Design noemt "druppelt" als eigenschap van het blok; vanilla-patroon (bladeren in de regen), common class, geen client-class, geen netwerk. Druppelgeluid blijft change 36.
- **B. Ja, maar in change 36 (`atmosphere`)** — backlog-regel. Strikte categorie-scheiding; tot change 36 is de bron een stil blok.
- **C. Nee, geen block-particles;** het druppelen komt alleen van biome-particles/-geluid (change 36). Eenvoudigst; de bron zelf is dan niet herkenbaar als "bron".

Antwoord: **A** — ja, in deze change: `animateTick` met vanilla `DRIPPING_WATER` onder de bron als daar ruimte is.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | blocks | Dauwbron (`dew_source`, eigen class `DewSourceBlock`): vol, opaak blok met water-achtige texture; geen waterfluid | Eenvoudigst; groei op alle zes zijden |
| 2026-10-01 | blocks | Dauwbron verkrijgbaar: nee (geen drop, piston vernietigt) | Sleutelmateriaal blijft aan Nether-ring 2 gebonden |
| 2026-10-01 | blocks | Dauwkristal-stadia: vier, zoals amethist (vanilla `AmethystClusterBlock`); groeit via random ticks (server-side) op lucht naast de bron, alleen vanaf de eigen zijde | Herkenbaar; geen eigen class |
| 2026-10-01 | blocks | Dauwkristal-drops: cluster → 4 (Fortune) met pickaxe, anders 2, Silk Touch → cluster; knoppen alleen Silk Touch; item `dew_crystal` wel als placeholder (nut fase 2) | 1-op-1 amethist |
| 2026-10-01 | blocks | Groeisnelheid dauwkristal: als amethist (1/5 per random tick) | Vertrouwd tempo; één constante |
| 2026-10-01 | blocks | Licht dauwbron/dauwkristal: stadia 1/2/4/5, bron 0 | Rijpe clusters vindbaar; bron blijft "water" |
| 2026-10-01 | blocks | Druppelen dauwbron: `animateTick` met `DRIPPING_WATER` in deze change | "Druppelt" is een eigenschap van het blok; vanilla-patroon |

Commit (na `/code-review`): `blocks: add dew source with growing dew crystal`.
