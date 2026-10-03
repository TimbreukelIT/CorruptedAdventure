# Change 30b — Ring 2: zout- en duinelementen en water (pekelpoelen, barstenpatroon, zoutpilaren, eilandjes, rotsuitstekels, mini-oases, minder watergrotten)

Scope: `worldgen` — change 30b (na teststop 2b, vóór change 30c en 31)

**Doel:** de zoutvlakte en de duinenzee voelen na teststop 2b kaal en leeg. Deze change geeft ze **vorm-elementen en water**. De begroeiing en aankleding volgen in change 30c. Het gaat om:
- **zoutvlakte:** pekelpoelen, een zeshoekig barstenpatroon in de korst, zoutpilaren/-schoorstenen, kristalvelden en verhoogde eilandjes van zand en rots;
- **duinenzee:** rotsuitstekels (zandsteen- en kloofsteen-eilandjes) als oriëntatiepunt en schaduw, en verborgen mini-oases in diepe duindalen;
- **grotten:** minder watergrotten, van ~25 % naar ~15 % van de holtes in ring 2.

Bron: `docs/plans/teststop-2b-feedback.md` (sparring 2026-10-03), `fase-1-wereld.md` §2 Ring 2 (uitwerkingen 23c, 26, 29, 30), plannen en bouwnotities `change-23c.md` (placement modifiers, `rubble`, wandbron, determinisme-regels), `change-26.md` (floodedness, meetmethode), `change-29.md` (zout-set), `change-30.md` (biomes, terrein-JSON, surface-takken).

**Splitsing (besloten in dit plan, zie "Waarom twee changes"):**
- **30b (dit plan):** terreinelementen van zout en duinen + water.
- **30c:** begroeiing en aankleding (duindalen, cactusgroepjes, fossielen en botten, oase, hoodoovelden, leven op de zout-eilandjes); plan `change-30c.md`.

Buiten scope:
- begroeiing van alle biomes en fossielen/botten (30c);
- dieren (fase 4, `mobs`);
- watertint van de pekelpoelen en geluid (change 32, `atmosphere`);
- textures van zoutkorst en zoutblok (change 37);
- condities, bijv. of pekel drinkbaar is (`conditions` → backlog).

**Status:** plan (2026-10-03); ontwerpvragen beantwoord (alle A, 2026-10-03).

### Waarom twee changes

- **30b** voegt drie nieuwe Java-types toe (feature `surface_pool`, feature `outcrop`, surface-conditie `polygon_edge` + access transformer), een terreinwijziging (eilandjes in `flat_height`) en de floodedness. Dat zijn vormen met eigen unit tests, een stabiliteitstest voor water en een noise- en surface-meting.
- **30c** is bijna alleen datagen (placed/configured features op de bestaande placement modifiers) plus één feature (`surface_fossil`) en één surface-regel (harde dalbodem in de duinen).
- Samen zou het een change van de omvang van 23b worden. Gesplitst blijft elke review klein, en 30c kan direct leunen op de eilandjes en mini-oases van 30b.
- Beide zijn `worldgen`; er is geen andere categorie nodig.

---

## Afhankelijkheden en aannames

Gecontroleerd in de code op `eacc15b` (change 30). Bij de start opnieuw nalopen tegen `CODEBASE.md`.
- **A1 — terrein (change 30):**
  - `ring_2/overworld/flat_height.json` mengt oase (`flats_height`), zout (constant **66**, `× salt_weight`) en duin (`dune_weight × dune_height`).
  - `dune_height` = 67 ± 2 (`ring_2_hills`) + amplitude 10–32 (spline op `ring_2_dune_amplitude`, `xz_scale` 1) × kamprofiel (spline op |`shifted_noise` `ring_2_dunes`|).
    - **Duindal:** |ruis| ≥ ~0,45, dus hoogte ≤ basis + 0,12 × amplitude ≈ **Y 66–71**, onafhankelijk van de amplitude.
    - **Kam:** |ruis| = 0, op basis + amplitude (Y 77–99).
  - `initial_height` legt zout en duinen op Y 66 (het preliminary surface ligt nooit boven het echte oppervlak).
- **A2 — surface rules (change 30):** de `salt_flats`-tak is: steil → lagen ≥ 74 / zandsteen; plafond zandsteen; zandvlekken aan de duinkant (`noise_threshold` op `ring_2_moisture`); anders `salt_crust`; dan 2–4 `salt_block`, 3 zandsteen, lagen ≥ 74. De `dune_sea`-tak geeft zand tot `add_surface_depth` + 6 op een zandstenen kern.
- **A3 — features (23c/30):** `jittered_grid` (cel 1–16, marge 0–7, kans), `noise_band` (geseede ruis, `snap`), `slope` (`rise`/`relief`, straal ≤ 8), `height_band`, `rubble` (kei van 1–3, half ingegraven, eigen chunk). `SpringLayout.settle` is het voorbeeld van water in vanilla's eindtoestand.
  - Alle ring 2-placements: `jittered_grid` → `WORLD_SURFACE_WG` → filters → `biome`.
  - `WORLD_SURFACE_WG` toont tijdens de features-stap alleen terrein, ook na eigen features in dezelfde chunk. Een latere feature ziet een poel of rots dus **niet** in de heightmap: hij moet de blokken zelf controleren (lucht, grond).
- **A4 — blokken:** de zout-set van 29 (`salt_crust` valt niet en hoort in geen plant- of grondtag; `salt_block`; `salt_crystal` = `AmethystClusterBlock`, `facing` + `waterlogged`, licht 3), `sandstone`/`smooth_sandstone`, de `scorched_sandstone`-set, de drie kloofsteentinten, de palm-set en `palm_tree`/`palm_bush`. Er komen **geen nieuwe blokken**.
- **A5 — floodedness (change 26):** `ring_2/overworld/floodedness.json` = vanilla-ruis + een diepteband van **+0,65** (Y −40…30, naar 0 bij Y 60 en Y −54).
  - Gemeten in change 26: +0,4 → 5,6 %; +0,6 → 23 %; +0,65 → 28,4 % met de dichtere carvers en **25,6 %** met de geschaalde carvers (eindcode); +0,7 → 33 %. Ring 1: 11,0 %.
  - Lineair geschat geeft +0,50 ongeveer **14–16 %**.
- **A6 — prestaties (change 30):** de noise-stap staat op het hoogste meetpunt (x 2.500, duinen) op **1,54×** vanilla (budget ≤ 1,75×); zoutvlakte 1,25×, hoge duinen 1,33×. De features-stap was in 23c ~2,9–3,0 ms per chunk (kloof/oase).
- **A7 — `SurfaceRules`:** vanilla-condities zijn inner classes van `SurfaceRules`. Een eigen conditie heeft toegang nodig tot `SurfaceRules$Context` (`blockX`, `blockZ`, `randomState`) en mogelijk tot de interfaces `Condition`/`ConditionSource`. Registry: `Registries.MATERIAL_CONDITION`. Bouwstap: zichtbaarheid nagaan in de 1.21.1-bron en via `META-INF/accesstransformer.cfg` openen. Lukt dat niet, dan **stop-en-vraag** (terugval: vraag 30b-2 C).

---

## Determinisme-regels (zoals 23c; gelden voor elke feature van 30b)

1. **Plaatsing** begint in de eigen chunk. Ze leest alleen:
   - `WORLD_SURFACE_WG` (ook 3 × 3; vastgezet na de carvers);
   - geseede ruis (`noise_band`);
   - de placement-random.

   Niet gebruiken: `random_patch`, `count_on_every_layer` of `environment_scan`.
2. **Schrijven:** alleen in de eigen chunk, ≥ 1 blok van de chunkrand. Een vorm schuift naar binnen of krimpt. Uitzondering: palmkronen van de mini-oases, volgens de palmregels van 23c (stamoorsprong ≥ 4 van de chunkrand, zie stap 6).
3. **Beslissen:** alleen op terrein of op blokken in de eigen chunk die eerder in dezelfde chunk gezet zijn (vaste volgorde). Nooit iets lezen wat een buurchunk schrijft.
4. **Water** (pekelpoelen, mini-oases): alleen **bronblokken** in een gesloten kom: op waterhoogte rondom vast, eronder vast. Dat is vanilla's eindtoestand. Er worden geen fluid ticks gepland, en er is geen stromend water.
5. **Surface-conditie en terrein-JSON:** pure functies van (seed, x, z).

Toets: twee verse werelden, de tweede in omgekeerde laadvolgorde → blok voor blok gelijk (S).

---

## Plan

### 1. Watergrotten: floodedness-band +0,65 → +0,50 (handgeschreven JSON)

- `ring_2/overworld/floodedness.json`: beide `y_clamped_gradient`s van 0,65 naar **0,50** (startwaarde, vraag 30b-7 A). De vorm van de band (Y −54/−40/30/60) blijft gelijk.
- **Meten** zoals in change 26:
  - seed 20260930, de 8 ring 2-gebieden van 5 × 5 chunks uit change 26 (200 chunks; door change 30 liggen ze nu deels in andere biomes, maar floodedness geldt voor de hele ring);
  - water en lucht in Y −54…62, alleen onder een dak;
  - 60 s laten uitzakken;
  - aandeel = water ÷ (water + lucht).
- **Volgorde:**
  1. eerst de **nulmeting op `eacc15b`**: change 30 veranderde de preliminary surfaces, dus 25,6 % kan verschoven zijn;
  2. dan +0,45 / +0,50 / +0,55;
  3. de waarde kiezen die het dichtst bij **15 % ± 2 pp** komt. Ring 1 (11 %) ter controle.
- Ook meten: de zone 1→2 (was 71 %, meelopen geaccepteerd) en "water onder een dak op Y 60–62" (mag niet stijgen).
- Bij vraag 30b-7 B: alleen de bovenkant van de band (Y 30 → 10) verlagen, en dezelfde meting doen.

### 2. Zeshoekig barstenpatroon: surface-conditie `corruptedadventure:polygon_edge` (Java, vraag 30b-2 A)

- **Nieuw:**
  - `worldgen/surface/PolygonEdgeCondition.java` (`SurfaceRules.ConditionSource`, record + `MapCodec`);
  - registry `registry/ModSurfaceConditions.java` (DeferredRegister op `Registries.MATERIAL_CONDITION`);
  - AT-regels (A7).
- **Codec:**
  - `cell_size` (afstand tussen roosterpunten in blokken, startwaarde **7**);
  - `jitter` (0–0,45; startwaarde **0,3**);
  - `width` (randbreedte in blokken, startwaarde **0,8**);
  - `hash` (noise-key, zoals `ring_2_slot_cells`: nieuwe hash-noise `ring_2_salt_polygons`, firstOctave 0).
- **Vorm:**
  - roosterpunten op een **driehoeksrooster** (elke rij een halve cel verschoven), elk punt verschoven met de hash × `jitter`. Het Voronoi-diagram van zo'n rooster is een **zeshoekig** patroon, met lichte onregelmaat;
  - rand = (afstand tot het op één na dichtste punt − afstand tot het dichtste punt) < `width`;
  - per kolom 7–12 kandidaatpunten.
- **Puur en goedkoop:**
  - pure functie van (seed, x, z): naadloos over chunkgrenzen;
  - de conditie onthoudt de laatste kolom (x, z), want de surface rules vragen per blok;
  - geschat < 0,1 ms per zoutchunk in de surface-stap (meten in S).
- **Surface rule** (`noise_settings/overworld.json`, `salt_flats`-tak): op de plek waar nu `salt_crust` wordt teruggegeven, eerst `polygon_edge` → `corruptedadventure:salt_block`, anders `salt_crust`. Daardoor geldt:
  - de randen liggen alleen in de korst (niet in de zandvlekken aan de duinkant, niet op de eilandjes van stap 3);
  - de randen liggen vlak in de vloer (geen verhoogde richels);
  - het aandeel randkolommen is ~15–25 % (stem `width` af).
- **Unit test `PolygonEdgeConditionTest`** (pure kern):
  - aandeel randkolommen voor de startwaarden;
  - gemiddeld 5–7 buren per cel (zeshoekig);
  - continuïteit over een chunkgrens (functie van wereldcoördinaten);
  - dezelfde seed geeft dezelfde uitvoer;
  - codec-rondreis, fouten buiten de bereiken.

### 3. Zout-eilandjes (JSON + surface rules, vraag 30b-6 A)

- **Nieuwe noise** `worldgen/noise/ring_2_salt_islands.json`: firstOctave −6, amplitudes [1, 0,5]; vlekken van ~15–40 blokken.
- **Terrein:** in `flat_height.json` wordt de zoutterm `66 · salt_weight` vervangen door `(66 + 2 · s(n)) · salt_weight`:
  - `n` = `noise` `ring_2_salt_islands`, `xz_scale` 1 (exact gelijk aan `noise_threshold`);
  - `s` = spline 0 bij n ≤ 0,50 → 1 bij n ≥ 0,62.
  
  Zo ontstaat een eilandje dat **2 blokken** boven de vlakte uitsteekt, met een flauwe rand. Dat is breed genoeg voor de interpolatie.
  - `initial_height` blijft 66: het preliminary surface ligt onder het eilandje, en dat mag.
  - Kosten: één 2D-ruis in een `flat_cache`-keten, dus te verwaarlozen. Toch meten (stap 9).
- **Surface rule** (`salt_flats`-tak, vóór de korst):
  - `noise_threshold` `ring_2_salt_islands` ≥ 0,46 → zand (een zandrand op de korst rond de verhoging);
  - ≥ 0,55 met surface-ruis > 0,3 → `red_sand`;
  - daaronder 1–3 zand, dan zandsteen.
- **Doel:** ~4–6 % van de zoutvlakte is eiland. De drempels stem je af op een meting (raster 4 over 25 zoutchunks).
- **Rotsjes op de eilandjes** (datagen): placed `salt_island_rocks` = `rubble` (zandsteen 3 / geschroeide zandsteen 2 / rode kloofsteen 1; 1–3 groot); `jittered_grid` 8/1/0,3 → `WORLD_SURFACE_WG` → grond zand/rood zand → `biome`. Begroeiing en botten volgen in 30c.

### 4. Feature `corruptedadventure:surface_pool` (Java; pekelpoelen én mini-oases)

Eén feature-type met een config. Dezelfde kern bedient de pekelpoel (stap 5) en de mini-oase (stap 6). De wandbron blijft een eigen type: die is te anders.
- **Nieuw:**
  - `SurfacePoolFeature`, `SurfacePoolConfiguration` (record + codec);
  - pure kern `PoolLayout` (geometrie in → blokken + states uit, zonder wereldtoegang), zoals `SpringLayout`.
- **Config:**
  - `radius` (FloatProvider);
  - `edge_jitter` (rafelige rand: 8 punten rond de cirkel met elk een eigen inkeping, zoals `bare_patch`);
  - `depth` (IntProvider, 1–2; bij 2 alleen de kern ≥ 1,5 van de rand);
  - `ground` (`HolderSet<Block>`: toegestane grond in voetafdruk en rand);
  - `max_step` (grootste hoogteverschil in voetafdruk + rand: 0 voor zout, 2 voor duinen);
  - `rim` (BlockStateProvider + breedte 0–2, vervangt het bovenste grondblok);
  - `rim_plants` (provider + kans per randblok, met `canSurvive`);
  - `shore_column` (optioneel: suikerriet 1–2 naast water, kans);
  - `rim_decoration` en `underwater_decoration` (provider + kans; zoutkristal `facing=up`, onder water `waterlogged=true`);
  - `trees` (optioneel: `HolderSet<ConfiguredFeature>` + aantal 0–2).
- **Algoritme:**
  1. De vorm (pool + rand + `trees`-ruimte) wordt naar binnen geschoven tot alles ≥ 1 van de chunkrand ligt; past het niet, dan krimpt de straal tot minimaal 1,5, anders niets.
  2. Waterspiegel `L` = de laagste `WORLD_SURFACE_WG`-hoogte (bovenste grondblok) in voetafdruk + rand. Elke kolom moet `ground` zijn en ≤ `max_step` boven `L` liggen, anders niets.
  3. Kolommen boven `L` worden tot `L` afgegraven (lucht).
  4. In de voetafdruk wordt Y `L − depth + 1 … L` bronwater.
  5. Controle (in de eigen chunk): rondom elk waterblok op elke waterhoogte vast terrein of water, en eronder vast. Anders niets (alles-of-niets, vóór het schrijven).
  6. Dan rand, planten, decoratie, bomen.
- **Water:** alleen bronnen in een gesloten kom. Dat is per definitie vanilla's eindtoestand: er worden geen ticks gepland, en een blokupdate verandert niets (valkuil "Water in features", 23c).
- **Unit test `SurfacePoolFeatureTest`** (`PoolLayout`), voor 2.000 synthetische vlaktes en dalen:
  - alles in de chunk, ≥ 1 van de rand;
  - water = alleen bronnen, op één niveau;
  - elk waterblok heeft vast of water ernaast op dezelfde Y en vast eronder (geen lek);
  - geen layout bij een gat in de rand of grond buiten `ground`;
  - boomoorsprongen ≥ 4 van de chunkrand en ≥ 5 uit elkaar.

### 5. Pekelpoelen (datagen, vraag 30b-1 A)

- **Configured `brine_pool`:**
  - `radius` 2–5, `edge_jitter` 0,35, `depth` **1** (ondiep);
  - `ground` `salt_crust`/`salt_block`, `max_step` 0;
  - `rim` `salt_crust` breedte 1 (een schone korstrand, ook waar een barstrand loopt);
  - `rim_decoration` zoutkristal (`facing=up`) 0,12;
  - `underwater_decoration` zoutkristal (`facing=up`, `waterlogged=true`) 0,04;
  - geen planten, geen bomen.
  
  De bodem is het `salt_block` dat er al ligt (wit onder water).
- **Placed `salt_flats_brine_pools`** (`LOCAL_MODIFICATIONS`): `jittered_grid` 16/1/**0,15** → `WORLD_SURFACE_WG` → grond `salt_crust`/`salt_block` → `biome`. Startdoel ~1 per 7–10 zoutchunks (de feature valt af op eilandjes en randen).
- **Uitdroging:** water van 1 diep telt niet als water (change 27: ≥ 1 blok boven de voeten). De poel koelt dus niet; de oase blijft de enige veilige plek. Met een fles is het water wel te scheppen (vanilla) → backlog `[conditions]`.

### 6. Mini-oases in diepe duindalen (datagen, vraag 30b-5 A)

- **Configured `dune_oasis`** (`surface_pool`):
  - `radius` 1,5–3 (water ~3 × 3 tot ~6 × 6), `depth` 1–2 (kern 2), `ground` zand, `max_step` 2;
  - `rim` `grass_block` breedte 2;
  - `rim_plants` kort gras 5 / varen 3 / hoog gras 1 / woestijnbloem 1, kans 0,5;
  - `shore_column` suikerriet 1–2, kans 0,3;
  - `trees` 1–2 uit `palm_tree` (3) / `palm_bush` (1) op de buitenrand.
  
  Palmstam ≥ 4 van de chunkrand, ≥ 5 uit elkaar, `would_survive(palm_sapling)`.
- **Waarom de palmen in de feature en niet op het palmraster:** de grasrand is te klein voor een raster van 8: een rasterkandidaat valt maar in ~½ van de oases op gras. Door stamoorsprong ≥ 4 van de rand + overhelling ≤ 2 staan stammen van buurchunks altijd ≥ 3 uit elkaar. Het palmraster van de oase ligt in een ander biome. Kronen in de buurchunk volgen de bestaande regels (zelfde blad, `PalmFoliagePlacer` slaat een log over).
- **Placed `dune_sea_oases`** (`LOCAL_MODIFICATIONS`):
  - `jittered_grid` 16/1/**0,08**;
  - → `noise_band` `ring_2_dune_amplitude` ≥ 0,15 (hoge duinen; schaal 1 = dezelfde ruis als `dune_height`);
  - → `WORLD_SURFACE_WG` → `height_band` ≤ 69 (dalbodem) → `slope` relief ≤ 2 r 4 → grond zand → `biome`.
  
  Startdoel: ~1 per 30–50 chunks met hoge duinen (meten; afstemmen met de kans).
- Water ≥ Y 66 (dalbodems), dus altijd boven zeeniveau (geen aquifer-wisselwerking). Een speler kan erin afkoelen (water ≥ 2 diep in de kern; geen oase-biome).

### 7. Zoutpilaren en rotsuitstekels: feature `corruptedadventure:outcrop` (Java, vragen 30b-3 A, 30b-4 A)

**Keuze feature i.p.v. density function (motivatie):**
- **Zoutpilaren** zijn 1–3 breed en 2–6 hoog. Dat ligt onder de interpolatieschaal (cellen 4 × 8 × 4) en zou per blok in `rock_pillars` moeten.
- Belangrijker: de surface rules kleden elke DF-vorm per diepte aan. Een DF-pilaar in de zoutvlakte wordt korst bovenop, 2–4 zoutblok en dan zandsteen. Een rots in de duinen krijgt 6+ blokken zand op elke vloer. Dat vraagt nieuwe surface-condities die de rots herkennen.
- Een **chunk-lokale feature** zet het materiaal zelf, kost **0 in de noise-stap** (budget!) en past ruim in één chunk.
- **Nadelen:** hooguit ~13 × 13; geen ertsen erin (ertsen komen na `LOCAL_MODIFICATIONS`; zonnesteen in een kloofsteenrots boven Y 95 kan wél, onschuldig).
- Grotere rotsen over chunkgrenzen: zie vraag 30b-4 B.

**Type:**
- **Nieuw:** `OutcropFeature`, `OutcropConfiguration`, pure kern `OutcropShape`.
- **Vorm:** `knobs` (IntProvider) toelopende kolommen rond de oorsprong:
  - spreiding `spread`, straal `radius` (FloatProvider) aan de voet;
  - bovenaan × `taper`;
  - hoogte: de hoogste knob `height` (IntProvider), de andere 0,4–0,8 daarvan;
  - per kolom van grond − `sink` tot de top van de knob, dus elke kolom staat op zijn eigen grond (op een duinhelling ook) en zweeft nooit;
  - `overhang_chance`: één knob krijgt bovenaan een kraag (straal +1–2, 1–2 dik, naar één kant), vast aan de knob, die schaduw geeft (`canSeeSky` = false eronder);
  - `top_decoration` (provider + kans) op de top van een knob.
- **Materiaal:** `palettes` (gewogen lijst van paletten; per outcrop één palet via de placement-random). Een palet = een lijst blokken in banden van 2–3 op absolute Y (zoals de lagen; per outcrop een verschuiving).
- **Chunk-lokaal:** schuift naar binnen, krimpt anders, ≥ 1 van de chunkrand. Schrijft alleen in lucht, vervangbare planten en de grond van de eigen voetafdruk (`sink`). Valt af als de voetafdruk water of gras (een poel van stap 5/6) bevat.
- **Unit test `OutcropShapeTest`** (2.000 vormen per config):
  - alles in de chunk;
  - elk blok via vaste buren verbonden met de grond (flood fill), ook de kraag;
  - hoogtes binnen het bereik;
  - de kraag geeft ≥ 3 kolommen met dak;
  - pure functie.

**Zoutpilaren** (`salt_pillar`):
- knobs 1–3, straal 0,8–1,6, hoogte **2–6**, taper 0,5, sink 1;
- palet `salt_block` (geen banden);
- `top_decoration` zoutkristal 0,3; geen kraag.
- **Placed `salt_flats_pillar_fields`** (`LOCAL_MODIFICATIONS`): nieuwe noise `ring_2_salt_pillars` (firstOctave −6, [1, 1]), `noise_band` ≥ 0,4 (~12 % van de vlakte) → `jittered_grid` 8/2/0,5 (~2 groepjes per veldchunk) → `WORLD_SURFACE_WG` → grond `salt_crust`/`salt_block` → `biome`.
- **Placed `salt_flats_pillars`** (los, buiten de velden): `jittered_grid` 16/1/0,06.

**Rotsuitstekels in de duinen** (`dune_rock`):
- knobs 2–5, straal 1,5–3,5, spread 4, hoogte **4–12**, taper 0,6, sink 2, `overhang_chance` 0,35;
- paletten: zandsteen-palet (zandsteen / glad zandsteen / geschroeide zandsteen, gewicht 3) en kloofsteen-palet (rood / oranje / crème, gewicht 2).
- **Placed `dune_sea_outcrops`** (`LOCAL_MODIFICATIONS`): `jittered_grid` 16/1/**0,1** (~1 per 10 duinchunks) → `WORLD_SURFACE_WG` → grond zand → `biome`. Op kam en dal (zichtbaar vanaf de kammen).

### 8. Kristalvelden (datagen, vraag 30b-3 A)

- **Nieuwe noise** `ring_2_salt_crystal_fields` (firstOctave −5, [1, 1]).
- **Placed `salt_flats_crystal_fields`** (`VEGETAL_DECORATION`, ná `salt_flats_crystals`): `noise_band` ≥ 0,35 (~10 % van de vlakte) → `jittered_grid` 4/0/0,35 (~5–6 per veldchunk) → `WORLD_SURFACE_WG` → lucht + `salt_crust`/`salt_block` eronder → `biome`. Configured `salt_crystal` (bestaand).
- `salt_flats_crystals` (~1 per chunk) blijft.
- 's Nachts gloeien de velden zwak (licht 3). In hun buurt spawnen geen monsters (bloklicht > 0).

### 9. Biomes, volgorde en datagen

- **`Ring2Biomes`:**
  - `salt_flats`: `LOCAL_MODIFICATIONS` = `salt_flats_brine_pools`, `salt_flats_pillar_fields`, `salt_flats_pillars`, `salt_island_rocks` (poelen eerst: pilaren en rotsen vallen af op water); `VEGETAL_DECORATION` = `salt_flats_crystals`, `salt_flats_crystal_fields`.
  - `dune_sea`: `LOCAL_MODIFICATIONS` = `dune_sea_oases`, `dune_sea_outcrops`; `VEGETAL_DECORATION` = `dune_sea_vegetation` (ongewijzigd; 30c herschikt).
  - Alles achteraan zijn stap. Elke placed feature hoort bij één biome, dus er komt geen nieuwe volgorde-eis tussen biomes.
- **Datagen-klasse:** de nieuwe bootstraps komen in een eigen `worldgen/feature/Ring2FlatsFeatures.java` (zout + duin). `Ring2Features` is al ~425 regels. Registratie in de bestaande lambda's van `DataGenerators` (configured vóór placed, vóór de biomes).
- **Registries:**
  - `ModFeatures`: `surface_pool`, `outcrop`;
  - `ModSurfaceConditions`: `polygon_edge` (nieuw);
  - keys in `ModConfiguredFeatures`/`ModPlacedFeatures`.
- **`Ring2BiomesTest`:** featurelijsten per biome in de vaste volgorde.

### 10. Documentatie

- **`CODEBASE.md`:**
  - classes en registries (`SurfacePoolFeature`/`PoolLayout`, `OutcropFeature`/`OutcropShape`, `PolygonEdgeCondition`, `ModSurfaceConditions`, `Ring2FlatsFeatures`);
  - features-tabel, noises (`ring_2_salt_islands`, `ring_2_salt_polygons`, `ring_2_salt_pillars`, `ring_2_salt_crystal_fields`), `flat_height`/`floodedness`/surface-tak;
  - AT-regels;
  - valkuilen: eigen surface-conditie (toegang, kolomcache); water als gesloten kom; latere features zien eigen features niet in `WORLD_SURFACE_WG`;
  - metingen en de kopregel.
- **`fase-1-wereld.md` §2 Ring 2:**
  - "Uitwerking zout- en duinelementen (change 30b)";
  - bij **Grotten** de nieuwe waterfractie;
  - bij de zout-set: de pekelpoel wijkt bewust af van 30-3 A ("geen water").
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:** de kandidaten hieronder.

---

## Gevolgen voor latere plannen (niet nu aanpassen)

| Plan | Wat verandert |
|---|---|
| **30c** | Bouwt op 30b: begroeiing op de zout-eilandjes (grond zand/rood zand), fossielen en botten op eilandjes en in duindalen. De mini-oases hebben al hun eigen planten en palmen; 30c zet daar niets extra. Nieuwe placed features achteraan de lijsten van 30b, in vaste volgorde |
| **31 `dry_riverbeds`** | `polygon_edge` kan het barstenpatroon van gebarsten klei in de beddingen geven (zelfde conditie, ander blok). `surface_pool` zonder water (`depth` 0, bodem `cracked_clay`) geeft drooggevallen poelen; `outcrop` geeft rotsen in de bedding. In de zone 2→3 vallen de features van 30b weg (biomefilter). De lagere floodedness geldt ook onder de beddingen (ringbreed) |
| **32 sfeer** | De pekelpoelen nemen de ring 2-watertint (`Palette.RING_2`, `32A598`). Een eigen, lichtere tint voor `salt_flats` hoort in 32. De mini-oases en kristalvelden zijn kandidaten voor ambient (vogels/insecten bij water, zacht zoemen bij kristallen) |
| **37 textures** | De korst-polygonen zitten nu **in de wereld** (`polygon_edge`). De `salt_crust`-texture moet dus rustiger worden (geen eigen polygonen, die botsen), en de top van `salt_block` moet duidelijk afsteken tegen de korst (randlijnen leesbaar). Glad zandsteen naast geschroeide zandsteen in de rotsuitstekels beoordelen |

---

## Client/server-impact

- **Puur worldgen**, server-side en deterministisch per seed: geseede ruis, pure surface-conditie, chunk-lokale features met de placement-random. Geen netwerk, geen SavedData of attachments, geen client-code.
- **Nieuwe common registries:** feature-types `surface_pool`, `outcrop`; material condition `polygon_edge`. AT-regels gelden voor beide kanten; een dedicated server laadt geen client-class.
- **Water** van pekelpoelen en mini-oases ligt als gesloten kom in zijn eindtoestand. Na een speleringreep rekent vanilla het server-side opnieuw uit.
- **Bestaande werelden:**
  - nieuwe chunks krijgen de elementen;
  - in de zoutvlakte geven de eilandjes (terrein) naden tussen oude en nieuwe chunks;
  - onder de grond geeft de floodedness waternaden.
  
  Verse werelden voor S en W.

---

## Tests

Bouwplan: **B, D, S, W** (W in teststop 2c, zie vraag 30b-8). Testseed `20260930`, werelden `smoke-change30b-…`.

### B — `./gradlew build`
- [ ] Nieuw: `PolygonEdgeConditionTest`, `SurfacePoolFeatureTest`, `OutcropShapeTest` (zie stappen 2, 4 en 7).
- [ ] Uitgebreid: `Ring2BiomesTest` (lijsten en volgorde), `Ring2TerrainJsonTest` (`flat_height` laadt, eilandterm alleen × `salt_weight`).
- [ ] Bestaande tests blijven groen.

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit: configured `brine_pool`, `dune_oasis`, `salt_pillar`, `dune_rock`, `salt_island_rock`; placed features van stap 5–8; `salt_flats.json`, `dune_sea.json` met de nieuwe lijsten.
- [ ] Geen andere biomes gewijzigd.

### S — server + RCON
- [ ] **Rooktest** (`tools/server-smoke-test.sh`): `Done`, geen "Feature order cycle", geen codecfout (ook `polygon_edge`), geen "Detected setBlock in a far chunk", geen client-class.
- [ ] **Watergrotten:** nulmeting `eacc15b`, dan +0,45/+0,50/+0,55 (stap 1). Doel 15 % ± 2 pp in 200 chunks; ring 1 ongewijzigd (~11 %); water Y 60–62 onder een dak niet hoger.
- [ ] **Zoutvlakte** (25 zoutchunks, raster van 1):
  - randkolommen `salt_block` op het oppervlak 15–25 % van de korst;
  - eilanden 4–6 %, op Y 66–68;
  - pekelpoelen: aantal (doel ~1 per 7–10 chunks), alle bronnen, 1 diep, korstrand, 0 water op een chunkrand;
  - pilaren 2–6 hoog, velden ~12 %;
  - kristallen per veldchunk ~5–6.
- [ ] **Duinenzee** (100 duinchunks):
  - rotsuitstekels ~1 per 10, ≤ 13 breed, 4–12 hoog, ~⅓ met kraag;
  - mini-oases ~1 per 30–50 chunks met hoge duinen, alleen in dalen (Y ≤ 69), met grasrand en 1–2 palmen;
  - palmstammen ≥ 5 uit elkaar, ook over chunkgrenzen.
- [ ] **Geen zwevende blokken** (flood fill vanaf Y 60, 15 gebieden zoals change 30): pilaren, rotsen en kragen 0.
- [ ] **Waterstabiliteit** (zoals 23c):
  - 10 pekelpoel- en 5 mini-oasechunks + hun 8 buren dumpen;
  - op elk randwaterblok een blok zetten en weghalen, 15 s wachten;
  - → identiek, 0 nieuw water in de buren.
- [ ] **Determinisme:** twee verse werelden, de tweede omgekeerd en chunk voor chunk geladen, 60 s uitzakken, 3 × 3-gebieden (2 zout, 2 duin met een oase en een rots, 1 zone 1→2) → blok voor blok gelijk op vanilla's ondergrondse klodders/ertsen en de `distance` van bladeren na.
- [ ] **Ring 1:** biomes op 1.681 punten gelijk; steen/deepslate in 5 gebieden gelijk (feature-index: lucht/planten mogen verschuiven, bekend).
- [ ] **Prestaties** (JFR `minecraft.ChunkGeneration`, meetplekken van change 30, tweede ronde, 3 rondes afwisselend, vóór = worktree `eacc15b`; budgetten in vraag 30b-9):
  - **noise-stap** op zoutvlakte, hoge duinen en x 2.500: ≤ 1,75× vanilla en ±0 t.o.v. vóór (verwacht: de eilandterm is te verwaarlozen);
  - **surface-stap** zoutvlakte: ≤ +0,5 ms;
  - **features-stap** zout/duin: ≤ +1,5 ms;
  - **hele chunk**: ≤ +5 %.

### W — in teststop 2c (vraag 30b-8 A), niet los na 30b

De bouwer zoekt plekken op en zet de coördinaten hieronder. De bouwer van 30c neemt de lijst over in de checklist van teststop 2c.
- [ ] **Zoutvlakte:** barstenpatroon leesbaar als zeshoeken (ook van bovenaf en op afstand), niet te druk; pekelpoelen (vorm, korstrand, kristallen onder water); pilaarvelden; eilandjes (verhoging, zandrand, rotsjes); kristalvelden 's nachts.
- [ ] **Duinenzee:** rotsuitstekels als oriëntatiepunt vanaf een kam; schaduw onder een kraag (`/ca condition get` → `shade`); een mini-oase vinden (verborgen genoeg? beloning?).
- [ ] **Grotten:** rondvliegen in spectator onder zout, duinen en kloven: minder volgelopen grotten, reservoirs nog herkenbaar.

---

## Risico's / aandachtspunten

- **Surface-conditie en toegang (A7):** lukt de AT niet of is `SurfaceRules$Context` anders opgebouwd → **stop-en-vraag** (terugval C: dezelfde vorm als feature in `LOCAL_MODIFICATIONS`, vóór de poelen).
- **Zeshoeken lezen als raster:** het driehoeksrooster met jitter 0,3 kan te regelmatig ogen. Dan de jitter verhogen (0,4) of het rooster met de warp-ruis van `ring_2_slot_warp` licht verbuigen (één extra ruis per kolom, goedkoop). Afstemgetal.
- **Barstenpatroon + korst-texture** (37) botsen visueel: in 2c beoordelen; 37 past de texture aan (zie gevolgen).
- **Eilandjes zijn een terreinwijziging** in een biome dat in 2b akkoord was ("vlak"): klein houden (+2, ~5 %). Bij twijfel vraag 30b-6 B (vlak, alleen zand).
- **Mini-oases te vaak of te zichtbaar:** het verdwaalgevoel in de duinen (30-4) mag niet weg. Kans laag beginnen (0,08) en meten.
- **Rotsuitstekels vs. verdwalen:** design 30-8 A wilde "lege duinen zonder herkenningspunten"; de sparring kiest nu bewust voor oriëntatiepunten. Zeldzaam houden (1 per 10).
- **Watergrotten na change 30:** de nulmeting kan afwijken van 25,6 %; kies de bandwaarde op de nieuwe meting, niet op de schatting.
- **Feature-index ring 1:** nieuwe placed features verschuiven vanilla's decoratie-seeds (bekend, geaccepteerd).
- **Een feature die afhaakt** verschuift de latere kandidaten van dezelfde placed feature niet (één kandidaat per cel), maar wel de random van latere features in die chunk. Dat blijft deterministisch.

---

## Backlog-kandidaten (bij de bouw in `docs/backlog.md` zetten)

- `[conditions] Pekelpoelen (change 30b): vanilla laat een glazen fles water scheppen uit elke poel; pekel niet drinkbaar maken (of zout water dat uitdroging verhoogt)? (bron: change 30b)`
- `[atmosphere] Pekelpoelen: eigen watertint voor salt_flats; ambient bij mini-oases en kristalvelden (change 32). (bron: change 30b)`
- `[structures] Mini-oases als kampplek/rustpunt van een karavaan (fase 3). (bron: change 30b)`

---

## Ontwerpvragen

Ronde 1 = zout (1–3, 6). Ronde 2 = duinen, grotten en planning (4, 5, 7, 8, 9). Houd het kort: alleen wat de sparring open liet.

### 30b-1. [Belangrijk] Pekelpoelen: diepte en vorm
- **A. 1 diep, rafelig rond (straal 2–5), witte zoutbodem, korstrand met kristallen, enkele kristallen onder water; ~1 per 7–10 zoutchunks (aanbevolen).** Koelt niet (te ondiep voor de uitdroging), dus de oase blijft de enige veilige plek. Spiegelend en wit-turquoise.
- **B. 1–2 diep (kern 2).** Je kunt erin afkoelen: een tweede koelplek midden in het zout. Minder "dood".
- **C. Groter en zeldzamer (straal 4–8, ~1 per 20 chunks).** Echte meertjes als oriëntatiepunt. Ze passen nét in één chunk (de vorm krimpt vaak).

Antwoord: **A** (gebruiker, 2026-10-03) — 1 diep, rafelig rond (straal 2–5), witte zoutbodem, korstrand met kristallen; ~1 per 7–10 zoutchunks.

### 30b-2. [Belangrijk] Barstenpatroon in de korst
- **A. Eigen surface-conditie: echte zeshoeken (driehoeksrooster met lichte jitter), cellen ~7 breed, randen 1 blok zoutblok, vlak in de vloer (aanbevolen).** Leest als een zoutvlakte uit een foto. Wel Java + access transformer; < 0,1 ms per chunk.
- **B. Alleen JSON-ruis (nul-lijnen van twee ruizen).** Geen Java, maar kronkelige barsten zonder zeshoeken.
- **C. Dezelfde zeshoeken als feature na de surface-stap.** Geen AT. Wel een extra feature-pass per zoutchunk, en de bodemregels kennen de randen niet.

Antwoord: **A** (gebruiker, 2026-10-03) — eigen surface-conditie `polygon_edge`: echte zeshoeken, cellen ~7, randen 1 blok zoutblok, vlak in de vloer.

### 30b-3. (routine) Zoutpilaren en kristalvelden
- **A. Groepjes van 1–3 schoorstenen van zoutblok, 2–6 hoog, toelopend, soms een kristal bovenop; velden op ~12 % van de vlakte + hier en daar één los; kristalvelden op ~10 % met ~5–6 kristallen per chunk (aanbevolen).**
- **B. Losser en hoger: alleen losse pilaren van 4–8, geen velden.** Meer oriëntatie, minder "veld".
- **C. Dichter: pilaar- en kristalvelden op ~20 %.** Drukker; 's nachts veel licht (minder monsters op de vlakte).

Antwoord: **A** (gebruiker, 2026-10-03) — groepjes van 1–3 zoutschoorstenen 2–6 hoog in velden op ~12 % + losse; kristalvelden op ~10 % met ~5–6 per chunk.

### 30b-4. [Belangrijk] Rotsuitstekels in de duinen
- **A. Feature: rotseilandjes tot ~13 breed en 4–12 hoog, zandsteen- of kloofsteenpalet in banden, ~⅓ met een overhangende kraag (schaduw); ~1 per 10 duinchunks (aanbevolen).** 0 kosten in de noise-stap; materiaal precies zoals bedoeld.
- **B. Density function (`rock_pillars`), groter: 10–30 breed, tot ~20 hoog, over chunkgrenzen.** Echte landmarks, maar +1–3 ms noise-stap op de duinen (x 2.500: 1,54× → ~1,6×). Het materiaal volgt de duin-surface-regel: zandkap bovenop, zandsteen eronder, geen kloofsteen.
- **C. Als A, zonder kraag.** Alleen oriëntatie, geen schaduwplek.

Antwoord: **A** (gebruiker, 2026-10-03) — feature `outcrop`: rotseilandjes tot ~13 breed, 4–12 hoog, zandsteen-/kloofsteenpalet, ~⅓ met kraag; ~1 per 10 duinchunks.

### 30b-5. [Belangrijk] Verborgen mini-oases
- **A. ~1 per 30–50 chunks met hoge duinen, alleen in diepe dalen; poel ~3 × 3 tot 6 × 6, kern 2 diep; grasrand van 2 met kort gras, varens, suikerriet; 1–2 palmen (soms een struikpalm) (aanbevolen).** Een echte vondst; je kunt er afkoelen in het water.
- **B. Vaker (~1 per 15).** Makkelijker te vinden; het verdwaalgevoel en "de oase is zeldzaam" worden zwakker.
- **C. Zeldzamer en groter (~1 per 80; poel tot 8 × 8, 2–3 palmen).** Een mini-oase als bestemming.

Antwoord: **A** (gebruiker, 2026-10-03) — ~1 per 30–50 chunks met hoge duinen, alleen in diepe dalen; poel 3 × 3–6 × 6, kern 2 diep; grasrand, 1–2 palmen.

### 30b-6. (routine) Zout-eilandjes
- **A. 2 blokken verhoogd met een flauwe rand, zand met rood-zandvlekken en rotsjes; ~5 % van de vlakte, 15–40 breed (aanbevolen).** Leest als "eiland"; leven volgt in 30c.
- **B. Vlak: alleen zandvlekken in de korst.** Geen terreinwijziging, minder herkenbaar.
- **C. Groter en zeldzamer (40–80 breed, ~3 %).**

Antwoord: **A** (gebruiker, 2026-10-03) — 2 blokken verhoogd met flauwe rand, zand met rood-zandvlekken en rotsjes; ~5 % van de vlakte.

### 30b-7. (routine) Minder watergrotten
- **A. De diepteband omlaag (+0,65 → ~+0,50, gemeten tot ~15 %); overal evenredig minder (aanbevolen).** Eén getal.
- **B. De bovenkant van de band omlaag (Y 30 → 10).** Ondiepe grotten (wat je in spectator ziet) worden droog, diepe reservoirs blijven vol.
- **C. Half A, half B.**

Antwoord: **A** (gebruiker, 2026-10-03) — diepteband omlaag (+0,65 → ~+0,50, gemeten tot ~15 %), overal evenredig.

### 30b-8. [Belangrijk] Teststop
- **A. Geen stop na 30b; na 30c één blokkerende teststop 2c (~25–30 min, zout, duinen, oase, hoodoos, grotten) vóór change 31 (aanbevolen).** "Levendig" is alleen in-game te beoordelen, en 31 hergebruikt features van 30b/30c (polygonen, fossielen). Fouten blijven zo goedkoop.
- **B. Een korte stop na 30b (~10 min, vormen en water) én teststop 2c na 30c.** Eerder bijsturen op de vormen; één extra onderbreking.
- **C. Geen 2c; alles naar teststop 3 (na 37).** Sneller door, maar bijsturen pas na textures en Nether-ring 2.

Antwoord: **A** (gebruiker, 2026-10-03) — geen stop na 30b; na 30c één blokkerende teststop 2c vóór change 31.

### 30b-9. (routine) Prestatiebudget
- **A. Noise-stap ring 2 blijft ≤ 1,75× (30b verwacht ±0); surface-stap zout ≤ +0,5 ms; features-stap ≤ +1,5 ms (30b) en ≤ +2 ms (30c) per chunk op de meetplekken; hele chunk ≤ +5 % (aanbevolen).**
- **B. Alleen de hele chunk: ≤ vanilla + 20 %.** Eenvoudiger, maar ruimer.

Antwoord: **A** (gebruiker, 2026-10-03) — noise ≤ 1,75×; surface zout ≤ +0,5 ms; features ≤ +1,5 ms (30b) / ≤ +2 ms (30c); hele chunk ≤ +5 %.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-03 | worldgen | Change 30 wordt aangevuld in twee changes: 30b (zout- en duinelementen + water) en 30c (begroeiing en aankleding), beide vóór 31 | Teststop 2b: zout, duinen, oase en hoodoos te kaal; gesplitst blijven de reviews klein |
| _(bouw)_ | worldgen | Watergrotten ring 2: diepteband floodedness +0,65 → _x_ (gemeten _y_ % van de holtes; ring 1 ~11 %) | Teststop 2b: te veel volgelopen grotten |
| _(bouw)_ | worldgen | Pekelpoelen in de zoutvlakte (30b-1): _antwoord_; feature `surface_pool`, gesloten kom van bronwater, chunk-lokaal | Leven in het zout; deterministisch water zonder ticks; wijkt bewust af van 30-3 A (geen water) |
| _(bouw)_ | worldgen | Barstenpatroon (30b-2): _antwoord_ | Zeshoekige zoutpolygonen in de wereld i.p.v. alleen in de texture |
| _(bouw)_ | worldgen | Zoutpilaren, kristalvelden en zout-eilandjes (30b-3, 30b-6): _antwoord_ | Aankleding en variatie op de vlakte |
| _(bouw)_ | worldgen | Rotsuitstekels in de duinen (30b-4): _antwoord_; feature `outcrop` i.p.v. density function (materiaal, 0 kosten noise-stap) | Oriëntatiepunt en schaduw; wijkt bewust af van 30-8 A (geen herkenningspunten) |
| _(bouw)_ | worldgen | Mini-oases in diepe duindalen (30b-5): _antwoord_ | Verborgen beloning in de duinen |
| _(bouw)_ | worldgen | Teststop na 30b/30c (30b-8): _antwoord_ | "Levendig" is alleen in-game te beoordelen |

Commit (na `/code-review`): `worldgen: add ring 2 brine pools, salt polygons, pillars, islands, dune outcrops and hidden oases`.
