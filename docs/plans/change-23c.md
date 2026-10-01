# Change 23c — Ring 2-aankleding: puin, keien, rotsval, wandbronnen, oasebegroeiing en palmgroepjes

Scope: `worldgen` — change 23c (na 23b, vóór change 25; in-game check in teststop 2b na change 30)

**Doel:** het terrein van 23b krijgt zijn aankleding via **features**. Het gaat om:
- losse keien en keienvelden op kloofbodems en puinhellingen;
- rotsval aan wandvoeten;
- zeldzame **wandbronnen**: water uit de kloofwand dat als klein stroompje naar een kolk op de kloofbodem loopt (23c-4 C);
- de oase: begroeiing per zone (waterrand, oever, overgangsring);
- palmen in groepjes met onderlinge afstand in plaats van "knotten";
- struikpalmen.

Alles moet chunk-lokaal en volgorde-onafhankelijk zijn: elke chunk geeft hetzelfde resultaat, in welke volgorde de chunks ook geladen worden.

Bron: `docs/plans/teststop-2-feedback.md` (1b: "droge watervallen en puin … rotsval-hellingen, keienvelden, sporadisch bronnetje"; 2: "oases groter, minder opeengepakte palmen, meer aankleding/overgang naar de woestijn"), `change-23b.md` (terrein, oasezones, puinhellingen, nissen), `change-23.md` (bouwnotities: oeverpalmen 24 pogingen, heightmap `WORLD_SURFACE_WG`).

Buiten scope:
- terreinvormen (23b);
- ertsen (25) en grotten (26);
- textures, waaronder het palet van `canyon_cobblestone` (37);
- structures (fase 3), zoals klifwoningen en waterputten;
- mobs (fase 4);
- condities, zoals of een wandbron koelt (change 27; alleen noteren, backlog `[conditions]`).

**Status:** plan, **afgestemd 2026-10-01** (alle ontwerpvragen beantwoord; afwijkende keuze van de gebruiker: 23c-4 C). Gebouwd ná 23b; daarna direct change 25. De in-game check (W) gaat mee in **teststop 2b na change 30** (vraag 23b-17, keuze gebruiker).

## Afstemming (2026-10-01)

- **23c-4 C (keuze gebruiker): wandbron met stroompje** i.p.v. een stilstaand plasje. Uitgewerkt in stap 2 (eigen feature `canyon_spring`, chunk-lokaal, vooraf "gestroomd" water zonder fluid ticks), stap 3 (plaatsing ~1 per 8–15 kloofchunks), de tests en de risico's (stop-en-vraag-punten).
- **Oase uit 23b (23b-8, keuze gebruiker):** waterfractie ~6–8 % (niet ~12 %), poelen plaatselijk breder via een extra grove octaaf in `ring_2_pools`, grasoever duidelijk breder (6–14). De zones blijven op dezelfde ruis, dus `noise_band` volgt ze exact. Meer gras = meer plek voor oeverbegroeiing en palmgroepjes: aantallen afstemmen (stap 3).
- **Teststop (23b-17, keuze gebruiker):** geen blokkerende stop na 23c; de W-lijst gaat naar de gebundelde checklist van teststop 2b in plan 30.
- **Lagen (23b-13 B):** meer crème; keien en puin in de drie tinten volgen vanzelf.
- De gevolgen voor latere plannen zijn op 2026-10-01 verwerkt in de plannen 25, 27, 30, 31 en 37.

---

## Afhankelijkheden en aannames

- **Change 23b is gebouwd** (bij de start nalopen in `CODEBASE.md`). 23c gebruikt:
  - **oasezones** in de surface rules op `noise_threshold` van `ring_2_pools` (oever = gras, 6–14 breed; overgangsring = binnen vooral gras met `coarse_dirt`, naar buiten `coarse_dirt`/zand; daarbuiten zand). 23c leest **dezelfde ruis** (incl. de extra grove octaaf van 23b-8) voor zijn filters, zodat features en bodem op dezelfde ringen vallen. Waar een poel plaatselijk breder is, schuift de hele ringenset mee (één ruis);
  - **puinhellingen** (`ring_2_talus`, vloer grind/rood zand), **kolken** en treden in de kloofbodem, **nissen** met een vlakke vloer, **spleetbodems** (Y ~90–105), **tafelbergen**.
  
  Wijken namen af, volg dan de echte; wijkt een vorm af (bijv. 23b-6 B: geen richels), dan vallen alleen de bijbehorende plaatsingen weg.
- **Bestaande features** (`Ring2Features`, change 23):
  - `palm_tree` (`PalmTrunkPlacer` 6–10, `PalmFoliagePlacer`);
  - `oasis_palms_shore` (24 pogingen per chunk op gras, de oorzaak van de "knotten"), `oasis_palms_sand` (1 per 4 chunks);
  - `oasis_vegetation` (6 per chunk op gras), `canyonlands_vegetation` (4 per chunk, gewogen);
  - vanilla `patch_sugar_cane_desert` (een `random_patch`).
  
  Alles staat in `VEGETAL_DECORATION`, achteraan na vanilla. Alle placements gebruiken `in_square` + `WORLD_SURFACE_WG`.
- **Valkuilen** (`CODEBASE.md`):
  - een feature schrijft alleen in de 3×3 chunks rond zijn herkomst;
  - `WORLD_SURFACE_WG` toont tijdens de features-stap alleen terrein (volgorde-onafhankelijk);
  - generatievolgorde (change 16): niets lezen wat buren schrijven;
  - feature-volgorde per stap (anders "Feature order cycle");
  - nieuwe features kunnen de globale feature-index verschuiven: ring 1-decoratie wijkt dan ~1 % af (change 23: gemeten en geaccepteerd);
  - `/place feature` slaat de placement over.
- **Blokken:** de kloofsteentinten en `canyon_cobblestone` (21), `cracked_clay`, `dry_shrub`, `barrel_cactus`, `desert_flower`, `scorched_sandstone`, de palm-set (22) en vanilla (`coarse_dirt`, `moss_block`, `fern`, `large_fern`, `tall_grass`, `sugar_cane`, `dead_bush`, `sandstone`). Geen nieuwe blokken: is er toch iets nodig, dan een vanilla-placeholder plus een backlogregel `[blocks]`.

---

## Determinisme-regels (gelden voor elke feature van 23c)

1. **Plaatsing** begint in de eigen chunk. Ze leest alleen:
   - de `WORLD_SURFACE_WG`-heightmap (alleen terrein; vastgezet na de carvers, ook bij de buren in 3×3);
   - geseede ruis via `RandomState` (dezelfde noise-keys als het terrein);
   - de random van de placement-context.
   
   **Geen** `count_on_every_layer`, `random_patch` of `environment_scan`.
2. **Schrijven:** elke feature behalve palmen en struikpalmen schrijft **alleen binnen de eigen chunk**. Een vorm schuift of knipt zich naar binnen. Twee chunks schrijven dus nooit in hetzelfde blok.
3. **Beslissen:** een feature beslist alleen op terreinblokken die geen ring 2-feature verandert (de grond op de heightmap-positie en de wand ernaast). Een gat of plasje kijkt alleen naar blokken in de eigen chunk, met 1 blok marge van de rand.
4. **Palmen** (3×3-schrijvers): posities uit een raster in wereldcoördinaten met een minimale afstand (`jittered_grid`, zie hieronder). Stammen raken elkaar nooit, ook niet over een chunkgrens. Waar kronen overlappen, zetten beide hetzelfde blok (`palm_leaves`), dus de unie is gelijk.
5. **Water** (wandbron): de feature zet water in zijn **eindtoestand** (bron, val, stroom met levels, kolk) en plant **geen** fluid ticks. Water mag nooit door te stromen na de generatie een buurchunk bereiken; geul en kolk sluiten het op in de eigen chunk.

Toets: twee verse werelden, de tweede in omgekeerde laadvolgorde → identieke tellingen per chunk (S).

---

## Plan

### 1. Placement modifiers (Java, `ModPlacementModifierTypes`)

- **`corruptedadventure:jittered_grid`** (`worldgen/feature/JitteredGridPlacement.java`): vervangt `count` + `in_square`.
  - Codec: `cell_size` (een deler van 16: 4, 8 of 16), `margin` en `chance`.
  - Eén kandidaat per cel van een wereldvast raster, verschoven binnen de cel minus `margin`.
  - De minimale afstand tussen twee kandidaten is dus 2 · `margin`, ook over chunkgrenzen (cellen liggen nooit over een chunkgrens).
  - De kans per cel komt uit de placement-random.
- **`corruptedadventure:noise_band`** (`worldgen/feature/NoiseBandFilter.java`): houdt een positie als de waarde van een geseede noise (`RandomState.getOrCreateNoise(key)`, gesampled zoals de `noise`-DF met `xz_scale`, y = 0) binnen [`min`, `max`] valt.
  - Daarmee vallen features precies op de zones van 23b (poelruis), op keienvelden of op palmbosjes.
  - **Bouwstap:** nagaan hoe een placement modifier bij de `RandomState` komt (verwachting: `context.getLevel().getLevel().getChunkSource().randomState()`) en dat de waarde gelijk is aan die van de surface rule (`noise_threshold`).
- **`corruptedadventure:slope`** (`worldgen/feature/SlopeFilter.java`): het grootste verschil in de `WORLD_SURFACE_WG`-hoogte binnen straal `radius` ten opzichte van de positie, binnen [`min`, `max`].
  - Een hoog minimum geeft een **wandvoet** (rotsval, wandbronnen); een laag maximum geeft **vlak** (keienvelden niet tegen een wand).
  - Alleen heightmaps, alleen 3×3; de pure rekenkern is apart testbaar.

### 2. Feature-types (Java, `ModFeatures`)

- **`corruptedadventure:rubble`** (`RubbleFeature` + `RubbleConfiguration`): een kei van 1–3 blokken (een kleine onregelmatige klont) uit een `BlockStateProvider`, half ingegraven op de heightmap-positie.
  - Blijft binnen de eigen chunk (de herkomst schuift naar binnen).
  - Schrijft alleen in lucht en vervangbare planten.
  - Elk blok rust op terrein of op een ander blok van de kei (unit test).
- **`corruptedadventure:canyon_spring`** (`CanyonSpringFeature` + `CanyonSpringConfiguration`; vraag 23c-4 C, keuze gebruiker): een **wandbron met stroompje**. Water komt uit de kloofwand, valt langs de wand naar de kloofbodem en stroomt als klein stroompje naar een kolk.
  - **Vorm** (alles in de **eigen chunk**, ≥ 1 blok van de chunkrand; alleen terrein lezen, alleen in de eigen chunk schrijven):
    1. **Wandvoet:** vanaf de herkomst zoekt de feature in de eigen chunk (begrensd, bijv. ≤ 16 kandidaatkolommen uit de placement-random) een kolom op een kloofbodem of trede (grond rood zand/gebarsten klei/grind) met in één van de 4 richtingen een wand die ≥ 8 hoger is (`WORLD_SURFACE_WG`). De richting `d` wijst van de wand af.
    2. **Bron:** één bronblok water in de wandvlakte, 3–8 boven de voet (`spring_height`). Alleen als erboven, eronder en aan beide zijkanten vast terrein zit en het blok ervoor (richting `d`) lucht is.
    3. **Val:** vallend water vóór de bron, van bronhoogte tot de vloer (een smal watergordijn langs de wand).
    4. **Stroompje:** een geul van 1 breed en 1 diep in de vloer, vanaf de valplek in richting `d` (hooguit één knik), 3–6 lang (`stream_length`). De geul bevat stromend water met de levels van vanilla's eindtoestand. De geulwanden moeten vast terrein zijn (gelezen in de eigen chunk), anders geen bron.
    5. **Eindpunt, kolk:** aan het einde van de geul, binnen het vanilla-vloeibereik (≤ 7 vanaf de valplek), een kuil van 2×2–3×3 en 1–2 diep met **bronwater** (stilstaand). Rand: `moss_block`/gras, varens, suikerriet waar het kan (`would_survive`).
    6. Waterlijn overal ≥ Y 66 (kloofbodems zijn ≥ 66); de kolkbodem dus ≥ Y 64. Geen plaatsing als een stap niet past: dan doet de feature niets.
  - **Hoe het water geplaatst wordt (deterministische route, gekozen):** **vooraf "gestroomd"**. De feature zet bron-, val-, stroom- en kolkblokken direct in hun **eindtoestand** (bron = `level` 0, val = vallend water, geul = `level` 1–7 zoals vanilla ze voor deze geometrie zou uitrekenen, kolk = bronnen) en plant **geen** fluid ticks. De levels komen uit een pure rekenkern `SpringLayout` (geometrie in → blokken + states uit), zonder wereldtoegang en dus apart testbaar.
    - **Niet gekozen: een bronblok met fluid tick** (zoals vanilla's `spring_feature`). Dat water stroomt pas bij het laden, kan dan een buurchunk in lopen en hangt af van wat daar op dat moment staat en wanneer die chunk tikt. Dat breekt determinisme-regel 2 en 5. (Vanilla's `spring_water` heeft kloofsteen ook niet als geldig blok, dus vanilla-bronnen komen in kloofwanden niet voor.)
    - **Bouwstap (verifiëren in de 1.21.1-bronnen):** markeert `WorldGenRegion.setBlock`/`ProtoChunk` vloeistofblokken voor post-processing (dan tikt het water bij status FULL)? Zo ja, dan is dat geen probleem zolang de layout vanilla's eindtoestand is (de tick verandert niets); de S-stabiliteitstest bewijst het. De uitkomst als valkuil in `CODEBASE.md`.
    - Na een blokupdate door een speler rekent vanilla het water gewoon opnieuw uit. Omdat de layout stabiel is en geul en kolk dicht zijn, verandert er niets tot de speler de geul openbreekt; daarna gedraagt het zich als elk vanilla-water (server-side).
- Geen eigen type voor losse stenen of planten: vanilla `simple_block` met een gewogen provider, of `block_column` voor suikerriet.

### 3. Configured en placed features (datagen, `Ring2Features`)

Alle placements: (`jittered_grid` of `count` + `in_square`) → `WORLD_SURFACE_WG` → filters (`noise_band`, `slope`, `block_predicate_filter` op de grond, `would_survive`) → `biome`. Stappen en volgorde: zie stap 4.

**Kloofgebied (`canyonlands`)** — startwaarden voor de aanbevolen opties:
| Placed feature | Wat | Plaatsing |
|---|---|---|
| `canyon_boulder_fields` | `rubble` (de drie tinten + kloofkeien, 1–3 groot) | `noise_band` op een nieuwe noise `ring_2_boulder_fields` (`firstOctave` −6, ~20 % van de bodem) → 6 pogingen, alleen op kloofbodems/treden (`slope` max 2, y onder de plateauhoogte via grond = rood zand/gebarsten klei/grind) |
| `canyon_talus_rubble` | `rubble` (klein, 1–2) | `noise_band` op `ring_2_talus` (de puinhellingen van 23b) → 4 pogingen |
| `canyon_rockfall` | `simple_block` (losse kloofkei of tint) | `slope` min 6 binnen straal 3 (wandvoet) → 3 pogingen |
| `plateau_rubble` | `rubble` (1–2) | 1 per 2 chunks op plateau en tafelbergen |
| `slot_floor_gravel` | alleen bij 23b-14 C | `slope` min 15 binnen straal 3 (spleetbodem) → grindvlek op de bodem |
| `canyon_springs` | `canyon_spring` (vraag 23c-4 C: wandbron met stroompje) | `rarity_filter` 4 (startwaarde, afstemwaarde) → `in_square` → `WORLD_SURFACE_WG` → `slope` min 8 binnen straal 2 (wandvoet) → `biome`; de feature zoekt daarna zelf de exacte voet en kan afhaken. Doel **~1 per 8–15 kloofchunks** ("kloofchunk" = chunk met een kloofwand ≥ 8 hoog op een kloofbodem of trede). Niet in spleten (te smal voor geul en kolk) en niet op het plateau |

`canyonlands_vegetation` (bestaand) blijft; de dorre struiken krijgen er de nissen en kolken gratis bij.

**Oase (`oasis_flats`)** — volgens de zones van 23b (`noise_band` op `ring_2_pools`):
| Placed feature | Wat | Zone en plaatsing |
|---|---|---|
| `oasis_palm_groves` (vervangt `oasis_palms_shore`) | `palm_tree` | buitenrand van de oever + binnenste deel van de overgangsring; `jittered_grid` cel 8, marge 2 (afstand ≥ 4); kans per cel uit een nieuwe noise `ring_2_palm_groves` (`firstOctave` −5): hoog → 0,6, anders 0 → groepjes van 2–4; `would_survive(palm_sapling)` |
| `oasis_palms_sand` (aangepast) | `palm_tree` | buiten de zones, `jittered_grid` cel 16, kans 1/6: een losse palm op het zand, nooit naast een andere |
| `oasis_palm_bushes` | nieuw configured `palm_bush` (`minecraft:tree`: `PalmTrunkPlacer` 1–2 recht, kleine `PalmFoliagePlacer`) | oever + overgangsring, `jittered_grid` cel 8, kans 0,25; onderbegroeiing tussen de groepjes |
| `oasis_shore_plants` (vervangt de `random_patch` van vanilla-suikerriet, vraag 23c-6) | `block_column` suikerriet 1–3 + `simple_block` hoog gras/varen/grote varen | waterrand: grond naast water (`block_predicate_filter`: water binnen 1 blok in de eigen chunk), 8 pogingen |
| `oasis_vegetation` (bestaand, aangepast) | kort gras, varen, woestijnbloem | oever (gras), 6 → ~10 pogingen (startwaarde: de grasoever is na 23b-8 duidelijk breder; afstemmen zodat de dichtheid per grasblok ongeveer gelijk blijft) |
| `oasis_transition_vegetation` | dorre struik, dode struik, kort gras (spaarzaam), `desert_flower` | overgangsring, 4 pogingen |
| `oasis_rocks` | `rubble` (zandsteen/`scorched_sandstone`, 1–2) | overgangsring, 1 per 2 chunks |

### 4. Biomes (`Ring2Biomes`)

- `canyonlands`:
  - `LOCAL_MODIFICATIONS` (vanilla's stap voor rotsen): `canyon_boulder_fields`, `canyon_talus_rubble`, `canyon_rockfall`, `plateau_rubble`, `canyon_springs` (`slot_floor_gravel` vervalt: 23b-14 A). `canyon_springs` staat **als laatste**: de bron controleert dat zijn pad (val, geul, kolk) vrij is van keien uit de eigen chunk en haakt anders af. Zo komt er nooit een kei boven de geul te hangen;
  - `VEGETAL_DECORATION`: `canyonlands_vegetation` (zoals nu).
- `oasis_flats`:
  - `LOCAL_MODIFICATIONS`: `oasis_rocks`;
  - `VEGETAL_DECORATION`: `oasis_shore_plants` (op de plek van vanilla-suikerriet), `oasis_palm_groves`, `oasis_palms_sand`, `oasis_palm_bushes`, `oasis_vegetation`, `oasis_transition_vegetation`.
- Eigen features altijd **achteraan** hun stap, in één vaste volgorde. Change 30 hergebruikt ze in dezelfde volgorde (geen "Feature order cycle").
- `ModConfiguredFeatures`/`ModPlacedFeatures`: nieuwe keys; `OASIS_PALMS_SHORE` wordt hernoemd/vervangen. Vanilla `PATCH_SUGAR_CANE_DESERT` gaat eruit (bij 23c-6 A).

### 5. Documentatie

- **`CODEBASE.md`:**
  - classes (3 placement modifiers, `RubbleFeature`, `CanyonSpringFeature` + `SpringLayout`) en registries;
  - features-tabel, noises (`ring_2_boulder_fields`, `ring_2_palm_groves`);
  - valkuilen: een placement modifier en `RandomState`-ruis; de determinisme-regels hierboven; `jittered_grid` als middel tegen knotten; water in features: eindtoestand zonder fluid tick (en de uitkomst van de post-processing-verificatie);
  - de kopregel.
- **`fase-1-wereld.md` §2 Ring 2:** **Uitwerking aankleding (change 23c)**.
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:** de kandidaten hieronder staan er al (toegevoegd 2026-10-01); bij de bouw alleen nieuwe vondsten toevoegen.

---

## Gevolgen voor latere plannen (verwerkt op 2026-10-01 in plannen 25, 27, 30, 31 en 37)

| Plan | Wat verandert |
|---|---|
| **25 ertsen** | Ertsen (`UNDERGROUND_ORES`) komen ná `LOCAL_MODIFICATIONS`: een `minecraft:ore` kan een kei van kloofsteen op het plateau (nu Y 95–180) in zonnesteenerts veranderen, en kloofsteen naast een wandbron. Dat is "glinsterend puin" en onschuldig, maar bij de telling per gastgesteente meenemen of uitsluiten (ertsdoel = `block_match` op tinten, keien zijn dezelfde tinten) |
| **27 uitdroging** | De wandbron (23c-4 C) is echt water: een tweede, zeldzame koelplek naast de oase (~1 per 8–15 kloofchunks). Met 27 vraag 7 A ("ondergedompeld tot boven het middel") telt het stroompje (1 diep) vermoedelijk niet, de kolk (tot 2 diep) en de val wel; de wandvoet geeft ook schaduw. Bij 27 alleen noteren; de keuze of dat zo mag staat in de backlog (`[conditions]`) |
| **30 extra biomes** | `hoodoo_fields_vegetation` en evt. puin in de hoodoovelden: hergebruik `rubble`/`canyon_rockfall` in **dezelfde volgorde** binnen de stap. `jittered_grid`/`noise_band`/`slope` zijn beschikbaar voor zoutkristallen en duinflora (geen `count` + toeval-knotten) |
| **31 `dry_riverbeds`** | Gestrande botten/resten kunnen `rubble` + `slope` hergebruiken. Oasefeatures vallen in de zone weg (biome-filter) |
| **37 textures** | `canyon_cobblestone` ligt nu in puin en rotsval in het zicht: het palet herzien (teststop 2, punt 3). Ook `moss_block`/`coarse_dirt` naast de palmbladeren in de oase beoordelen |
| **Fase 3** | Oase-aankleding laat ruimte voor dorpen (geen bomen op de hele oever). Wandbronnen kunnen later kampen of putten markeren (backlog) |

---

## Client/server-impact

- Puur worldgen (features-stap), server-side en deterministisch per seed: placements gebruiken de seeded placement-random, de terrein-heightmap en de geseede ruis. Geen netwerk, geen SavedData of attachments, geen client-code.
- Nieuwe common registries: 3 placement-modifier-types en 2 feature-types. Een dedicated server laadt geen client-class.
- Water van wandbronnen (23c-4 C) wordt in zijn eindtoestand gezet, zonder geplande fluid ticks; na een speleringreep rekent vanilla het server-side opnieuw uit. Geen client-code.
- Bestaande werelden: alleen nieuwe chunks krijgen de aankleding.

---

## Tests

Bouwplan: **B, D, S, W**. Testseed `20260930`, werelden `smoke-change23c-…`.

### B — `./gradlew build`
- [ ] **`JitteredGridPlacementTest`:** voor alle celgroottes en 1.000 chunks staan de kandidaten ≥ 2 · `margin` uit elkaar, ook over chunkgrenzen; ze liggen in de eigen chunk; dezelfde random geeft dezelfde posities.
- [ ] **`NoiseBandFilterTest`:** de band klopt voor een vaste noise; de waarde is gelijk aan een `noise`-DF met dezelfde key en schaal.
- [ ] **`SlopeFilterTest`** (pure kern): wandvoet/vlak correct op een synthetische heightmap.
- [ ] **`RubbleFeatureTest`** (vormkern): voor 2.000 herkomsten ligt elk blok binnen de chunk en rust het op grond of op de kei.
- [ ] **`CanyonSpringFeatureTest`** (rekenkern `SpringLayout`, 23c-4 C), voor 2.000 synthetische wanden/vloeren:
  - alle blokken (bron, val, geul, kolk, rand) liggen in de chunk, ≥ 1 van de rand;
  - de levels zijn gelijk aan een kleine referentiesimulatie van vanilla's vloeiregels op dezelfde geometrie (BFS tot stilstand), dus de layout is een **eindtoestand**;
  - geul 3–6 lang, kolk binnen het vloeibereik (≤ 7 vanaf de valplek), kolk = bronnen;
  - elk waterblok heeft vast terrein of water eronder; naast stromend water op geulniveau alleen vast terrein of water (geen lek);
  - geen layout als de wand, de geulwanden of de kolkrand niet vast zijn; waterlijn ≥ Y 66.
- [ ] **`Ring2BiomesTest`** uitgebreid: de nieuwe features staan per biome in de vaste volgorde, achteraan hun stap; geen vanilla-suikerriet-`random_patch` meer (bij 23c-6 A).

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit: configured en placed features, `canyonlands.json` en `oasis_flats.json` met de nieuwe featurelijsten. Geen andere biomes gewijzigd.

### S — server + RCON
- [ ] **Rooktest:** start zonder fouten, geen "Feature order cycle", geen "Detected setBlock in a far chunk" in de log.
- [ ] **Determinisme:** twee verse werelden, de tweede in omgekeerde laadvolgorde. Per chunk (16 kloof-, 16 oasechunks, waarvan 8 met een poel) tellingen van `palm_log`, `palm_leaves`, de drie tinten **boven de heightmap** (keien), `canyon_cobblestone`, `gravel`, `sugar_cane`, `fern`, `dead_bush`, `dry_shrub`, `moss_block`, `sandstone` boven de heightmap en water boven Y 63 → **identiek**.
- [ ] **Palmen:** in 16 oasechunks met een poel:
  - geen twee palmstammen dichter dan 4 bij elkaar (de stamvoeten via `execute if block` op de heightmap-hoogte);
  - gemiddeld 2–6 palmen per poelchunk, in groepjes;
  - 0 palmen in canyonlands.
- [ ] **Puin:** keien in > 50 % van de kloofchunks met een keienveld; rotsval aan wandvoeten > 0 in 16 chunks met een steile wand; 0 keien die zweven (blok boven lucht en zonder keiblok eronder).
- [ ] **Wandbronnen** (23c-4 C):
  - aantal: over 400 kloofchunks **25–50** wandbronnen (~1 per 8–15); waterlijn overal ≥ Y 66;
  - vorm: per bron een bronblok in de wand (`water[level=0]` met vast terrein eromheen), vallend water eronder, stromend water in de geul en bronwater in de kolk;
  - **stabiliteit:** in 10 bronchunks (in een kopie-wereld) water tellen (`fill … glass replace water` per chunk + de 8 buurchunks), dan naast elke geul een blok `setblock` en weer weg (blokupdate), 200 ticks wachten, opnieuw tellen → **identiek**, en 0 nieuw water in de buurchunks;
  - geen "Detected setBlock in a far chunk" (de feature schrijft alleen in de eigen chunk).
- [ ] **Ring 1:** biomes 300/300 en steen/deepslate in 6 ring 1-chunks identiek aan 23b (lucht/water ~1 % mag: valkuil feature-index).
- [ ] **Performance:** JFR, de stap `features` op de twee meetplekken van 23b. Doel ≤ +15 % op de features-stap, en de hele chunk in ring 2 ≤ vanilla.

### W — **niet blokkerend na deze change; gaat naar teststop 2b** (blokkerend, na change 30, vóór 31; vraag 23b-17, keuze gebruiker)

De bouwer van 23c zoekt per punt een plek (seed `20260930`) en zet de coördinaten hieronder; de bouwer van 30 neemt de lijst over in de gebundelde checklist.
- [ ] Oase: bredere groene rand, palmgroepjes in plaats van knotten, struikpalmen, een zachte overgang naar de woestijn (dor gras, struiken, stenen), suikerriet en varens aan het water.
- [ ] Kloof: keienvelden, puin op de hellingen, rotsval aan wandvoeten; voelt niet "leeg" en niet rommelig.
- [ ] Wandbronnen: herkenbaar als bron (water uit de wand, val, stroompje, kolk), zeldzaam genoeg, past in de droge ring; na graven naast de geul geen rare waterstromen.
- [ ] Palm: vorm (routinevraag 23c-7 A: laten).

---

## Risico's / aandachtspunten

- **Rommeligheid:** te veel puin maakt de kloofbodem druk. Startwaarden laag houden en afstemmen in teststop 2b.
- **Palmkronen over chunkgrenzen** (3×3-schrijvers): de rasterafstand voorkomt stam-overlap. Kronen overlappen met hetzelfde blok. De S-telling bewaakt het.
- **`noise_band` moet exact de surface-zones volgen** (dezelfde ruis, dezelfde schaal). Wijkt de sampling af, dan staan planten naast hun zone. Daarom de bouwstap en de unit test.
- **Feature-index verschuift ring 1-decoratie** (~1 %, change 23): geaccepteerd; vergelijken op steen/deepslate/biomes.
- **Suikerriet** vanilla → eigen plaatsing: iets minder dicht aan de waterrand; afstemmen.
- **Water in de droge ring:** de wandbron (23c-4 C, keuze gebruiker) maakt "de oase is het enige veilige water" minder letterlijk. Bewust gekozen; het effect op de uitdroging staat in de backlog (`[conditions]`).
- **Wandbron — stop-en-vraag-punten voor de bouwer:**
  1. **Stabiliteit:** verandert het vooraf gestroomde water toch bij de eerste blokupdate of bij status FULL (layout ≠ vanilla's eindtoestand), of loopt het een buurchunk in → **stop-en-vraag**. Opties dan: (a) kortere geul of alleen bron + val + kolk zonder stroompje; (b) de vanilla-route met fluid tick, met een gedocumenteerde uitzondering op de determinisme-regels; (c) terug naar 23c-4 A (stilstaand plasje).
  2. **Frequentie:** haalt de chunk-lokale zoektocht geen ~1 per 8–15 kloofchunks (te weinig wanden met ruimte voor geul + kolk binnen één chunk), dan eerst de afstemwaarden (`rarity_filter`, `spring_height`, `stream_length` 3–6, kandidaatkolommen); lukt het dan nog niet → **stop-en-vraag** (bijv. lagere frequentie accepteren, of de bron ook aan treden/valwanden laten ontspringen).
  3. **Uiterlijk:** een val van 3–8 blokken langs de wand is een smal gordijn; leest het niet als bron, dan afstemmen in teststop 2b (hoogte, kolkgrootte, randbegroeiing).
  4. **Andere features:** ertsen (`UNDERGROUND_ORES`, later) kunnen kloofsteen naast de bron in zonnesteen veranderen: onschuldig. Vanilla-disks (`disk_*`, ook `UNDERGROUND_ORES`) zoeken water op de `OCEAN_FLOOR_WG`-hoogte en kunnen in buurchunks schrijven: nagaan dat ze de kolk niet zien (de heightmap toont alleen terrein); anders in de determinismetest meenemen en melden.

---

## Backlog-kandidaten (niet bouwen; **op 2026-10-01 al in `docs/backlog.md` gezet**)

- `[structures] Wandbronnen/oases als ankerpunt voor kampen, putten of trappen naar de grondwaterspiegel (zie change 26). (bron: change 23c)`
- `[conditions] Wandbron in de kloof (change 23c, 23c-4 C): koelplek voor de uitdroging (change 27)? ...` (volledige regel in de backlog)
- Het texturepunt (`canyon_cobblestone` in puin en rotsval, `moss_block`/`coarse_dirt` in de oase) staat niet in de backlog maar als expliciet aandachtspunt in plan 37.

---

## Ontwerpvragen

Ronde 1 = oase (1, 2). Ronde 2 = kloof (3, 4). Ronde 3 = routine (5–7).

### 23c-1. [Belangrijk] Hoe staan de palmen?
- **A. Groepjes van 2–4 met ≥ 4 blokken tussen de stammen, op de buitenrand van de oever en het binnenste deel van de overgangsring; een enkele losse palm op het zand; struikpalmen (stam 1–2) als onderbegroeiing (aanbevolen).** Geen knotten meer, en het oogt als een echte palmoase. De groepjes komen uit een eigen ruis, dus ze zijn deterministisch en chunk-onafhankelijk.
- **B. Losse palmen op een raster (≥ 5 uit elkaar) langs de hele oever, geen groepjes of struikpalmen.** Netjes en rustig, maar het kan als een aangeplante rij lezen.
- **C. Als A, maar dichter: de oever als palmbos (afstand ≥ 3, kans hoger).** Weelderig en schaduwrijk (uitdroging, change 27), maar een grotere kans op een vol kroonbeeld.

Antwoord: **A** — groepjes van 2–4 met ≥ 4 blokken tussen de stammen (buitenrand oever + binnenste deel overgangsring), af en toe een losse palm op het zand, struikpalmen als onderbegroeiing.

### 23c-2. [Belangrijk] Wat staat er rond de oase (overgang naar de woestijn)?
- **A. Aan het water suikerriet, hoog gras en (grote) varens; op de oever kort gras, varens en woestijnbloemen; in de overgangsring spaarzaam dor gras, dorre/dode struiken, struikpalmen en hier en daar een zandsteenkei (aanbevolen).** Groen bij het water, uitlopend naar dor. Leest als "het enige veilige water".
- **B. Sober: alleen de oever groen; de overgangsring alleen dorre struiken en stenen.** Kaal en droog; minder "oase-gevoel".
- **C. Weelderig: ook bloemenvelden, waterlelies op de poelen en bessenstruiken.** Levendig, maar minder woestijnachtig. Waterlelies en bessen voelen vanilla en geven extra voedsel (uitdroging/voedsel-balans).

Antwoord: **A** — gelaagd: aan het water suikerriet, hoog gras en (grote) varens; op de oever kort gras, varens, woestijnbloemen; in de overgangsring spaarzaam dor gras, dorre/dode struiken, struikpalmen en af en toe een zandsteenkei.

### 23c-3. [Belangrijk] Puin, keien en rotsval in het kloofgebied
- **A. Keienvelden (vlekken op ~20 % van de kloofbodems met keien van 1–3 blokken), puin op de puinhellingen, rotsval (losse stenen) aan wandvoeten, af en toe een kei op het plateau (aanbevolen).** Verhaal van afbrokkelende wanden; niet overal rommelig.
- **B. Dichter: overal op de kloofbodem keien en veel rotsval.** Ruiger en leest sterk als "instortend", maar lastig te belopen en druk.
- **C. Alleen rotsval aan wandvoeten en puin op de hellingen; geen keienvelden.** Rustig en minimaal.

Antwoord: **A** — matig: keienvelden op ~20 % van de kloofbodems, puin op de puinhellingen, rotsval aan wandvoeten, af en toe een kei op het plateau.

### 23c-4. [Belangrijk] Bronnetjes in de kloven
- **A. Zeldzaam (~1 per 6×6 chunks kloofgebied): een klein stilstaand plasje (2×2–3×3, 1 diep) aan een wandvoet of in een nis, met mos-/grasrand, varens en suikerriet; echt water (aanbevolen).** Een beloning bij het verkennen. Wel een tweede, zeldzame waterbron naast de oase: water laat de uitdroging (change 27) zakken.
- **B. Decoratief zonder water: een vochtige plek (mos, `coarse_dirt`, varens).** "De oase is het enige veilige water" blijft letterlijk waar, maar het is minder nuttig en minder herkenbaar als bron.
- **C. Een wandbron: water spuit uit de kloofwand en vormt beneden een stroompje (eigen spring met kloofsteen).** Spectaculair zichtbaar, maar stromend water in de droge ring en meer water dan A.
- **D. Geen bronnetjes.**

Antwoord: **C (keuze gebruiker)** — wandbron met stroompje: water komt uit de kloofwand, valt langs de wand en stroomt als klein stroompje over de kloofbodem naar een kolk. Uitwerking (eigen feature, chunk-lokaal, vooraf "gestroomd" water zonder fluid ticks, ~1 per 8–15 kloofchunks) in stap 2 en de tests; stop-en-vraag-punten bij de risico's.

### 23c-5. (routine) Materiaal van keien en puin
- **A. Mix van de drie kloofsteentinten + `canyon_cobblestone` (dat in change 37 een nieuw palet krijgt) (aanbevolen).**
- **B. Alleen de drie tinten** (tot 37 het palet van de kloofkeien herziet).

Antwoord: **A** — mix van de drie kloofsteentinten + `canyon_cobblestone` (palet herzien in change 37).

### 23c-6. (routine) Suikerriet aan het water
- **A. Eigen chunk-lokale plaatsing aan de waterrand i.p.v. vanilla's `random_patch` (aanbevolen).** Volgt de determinisme-regel. Vanilla's patch was in change 23 wel gemeten gelijk, maar valt onder "random_patch-achtig".
- **B. Vanilla's `patch_sugar_cane_desert` laten.** Geen werk; het blijft een uitzondering op de regel.

Antwoord: **A** — eigen chunk-lokale suikerrietplaatsing aan de waterrand i.p.v. vanilla's `random_patch`.

### 23c-7. (routine; teststop 2, vraag 7) Palmvorm (kroon ~7 breed, stam 6–10)
- **A. Laten; de knotten waren een plaatsingsprobleem (23c-1) (aanbevolen).**
- **B. Iets voller/groter (stam 7–11, waaiers +1).** Wel opnieuw de bladverval-test (≤ 6 stappen).

Antwoord: **A** — palmvorm laten; de knotten waren een plaatsingsprobleem.

---

## Beslissingenlog-regels (datum = bouwdatum; tekst volgens de antwoorden van 2026-10-01)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-xx | worldgen | Determinisme-regels voor ring 2-features: plaatsing alleen op `WORLD_SURFACE_WG` + geseede ruis; alles behalve palmen schrijft alleen in de eigen chunk; palmen op een wereldvast raster met minimale afstand | Volgorde-onafhankelijk per seed, ook over chunkgrenzen |
| 2026-10-xx | worldgen | Eigen placement modifiers `jittered_grid`, `noise_band`, `slope` | Afstand zonder buren te lezen; features volgen exact de terreinzones van 23b |
| 2026-10-xx | worldgen | Palmen in groepjes van 2–4 (stammen ≥ 4 uit elkaar) aan de oeverrand + struikpalmen; losse palm op het zand | Geen knotten; oogt als een palmoase |
| 2026-10-xx | worldgen | Oase-aankleding per zone: groen aan het water, dor in de overgangsring, zandsteenkeien | Zachte overgang naar de woestijn |
| 2026-10-xx | worldgen | Kloof-aankleding: keienvelden (~20 % van de bodems), puin op puinhellingen, rotsval aan wandvoeten | Afbrokkelende wanden zonder rommeligheid |
| 2026-10-xx | worldgen | Wandbronnen (keuze gebruiker): water uit de kloofwand (3–8 boven de voet), val, stroompje in een geul van 3–6 naar een kolk op de kloofbodem; ~1 per 8–15 kloofchunks; eigen feature `canyon_spring`, chunk-lokaal, water in zijn eindtoestand zonder fluid ticks | Spectaculair zichtbare bron; deterministisch per seed en volgorde-onafhankelijk (vanilla's tick-route stroomt na de generatie buurchunks in) |

Commit (na `/code-review`): `worldgen: add ring 2 dressing: rubble, rockfall, wall springs, oasis vegetation and palm groves`.
