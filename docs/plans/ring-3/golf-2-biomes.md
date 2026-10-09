# Golf 2 — stroom W-biomes: 41, 47, 47b, 49, 47c, 48, 41b (inrichting ring 3)

Scope: `worldgen` — één commit per pakket, branch `ring3/biomes-g2`, poort 2 (server 25572, RCON 25582). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:** ring 3 wordt bewoonbaar en levendig. Na deze stroom heeft elk biome zijn eigen aankleding met wow, en is er wat te halen (ertsen, kristallen, voedsel). De horror-features staan er ook. Teststop 4b beoordeelt "de levendigheid per biome (2 min lopen per biome)", "zielvuur en skeletten, gloeiende afgietsels 's nachts" en "voedsel (geiten, flora)".

**Bron:**
- `fase-1-wereld.md` Ring 3 (sparring, V1–V8, "Reliëf en vorm per biome (39d)", Ores, Horror);
- `contract.md` §2–§4 (feature-keys `corruptedadventure:ring_3/<naam>`, 3×3-regel, determinismeregels 23c);
- `change-39d.md` (terreinstand en landmarks), `teststop-4a-feedback.md` (39f loopt parallel);
- backlog r. 122/123, 159/160, 178, 184, 193/194;
- sjablonen:
  - `Ring2Features` / `RingVanillaOres` (ertsen);
  - `DewGeodeFeature` (geodes);
  - `SurfaceFossilFeature`/`FossilLayout` (sjablonen);
  - `Ring2DressingFeatures`, `DryRiverbedsFeatures`, `JitteredGridPlacement`, `NoiseBandFilter`, `SlopeFilter`, `HeightBandFilter`;
  - `SpringLayout` (water in eindtoestand), `PalmFoliagePlacer` (kronen over chunkgrenzen).

## 0. Plaatsingsregels na 39d (gelden voor elk pakket)
Het terrein van 39d heeft rifts (20–40 diep), afgronden (tot Y ~2, lavazee Y 9), getraceerde lavarivieren (6–16 breed, 84 km), kratermeren, zwevende eilanden en archipels, en dunne vormen. Die dunne vormen (uitgang `thin`) zijn hoodoos, ribben, zuilen, naalden 60–120, bogen, bruggen en pilaren; verder slakkenkegels, zinkgaten en lavabuis-sleuven. Een feature die "op de grond" staat, moet die plekken kennen.

1. **Terreinfilter.** Nieuwe placement modifier `corruptedadventure:volcanic_filter` (in `ModPlacementModifierTypes`, blok `// ring 3 — W-biomes`). Hij leest de pure kolom van het ring 3-veld (`VolcanicField.column(x, z)`; dezelfde uitgangen als de surface-conditie `volcanic_threshold`: `lava_bank`, `cliff`, `solid_kind`, `volcano_rise`, `caldera_mask`, `caldera_dormant`, `island_mask`, `lava_channel`) met min/max per uitgang, plus een optie `ground_only`.
   - `ground_only` laat alleen plaatsingen toe waar het gevonden oppervlak (`WORLD_SURFACE_WG`) binnen 3 blokken van `column.height()` ligt. Zo komt er niets op kruinen van naalden, bogen, ribben en pilaren, niet op eilanden en niet op brugdekken.
   - **Implementatie, eerst nagaan:** het veld vind je via `ServerChunkCache.randomState()` → `RingChunkGenerator.volcanicField(random)`. Is `column()` buiten een noise chunk thread-safe (`ColumnTable`)? Zo niet, dan per chunk een eigen kopie via `mapAll` (patroon `withSurfaceField`). Nooit een `flat_cache` uitlezen.
   - W-biomes **leest** W-terrein-klassen (`VolcanicField`, `CellShapes`, `TitanBones`, `Landmarks`) alleen. Is er een publieke accessor nodig, bijvoorbeeld voor de toppen van de fumarole-heuveltjes of de posities van de ribbenkasten: vraag die via main aan 39f of W-terrein. Niet zelf toevoegen.
2. **Lava en rivieren.** Geen bomen, flora, aslagen of afgietsels op lavaoevers (`lava_bank` < 0,1) of binnen 2 blokken van een lavakolom. Boomkronen niet boven lava.
3. **Kliffen en flanken.**
   - Niet op steile wanden: `cliff` < 0,3 en/of `SlopeFilter`.
   - Niet op vulkaanflanken (`volcano_rise` > ~20), behalve vulkaan-eigen features.
   - Niet in actieve kraters (`caldera_mask` > 0 en `caldera_dormant` < 0,5).
4. **Afgronden en rifts.** Niets in de afgrond of op de natuurlijke bruggen. Riftbodems mogen wel aankleding krijgen (ze hebben hun surface, `check_shot_rift_floor`).
5. **Carvers.** Geen eigen ring 3-carvers die boven Y ~140 starten (backlog r. 184; anders zwevende brokken onder de eilanden Y 150–210). De diepe carvers van ring 2 blijven (`Ring2Carvers.COPIES`, start ≤ Y 40).
6. **Vloeistoffen** (les 39/39d):
   - nooit stilstaande vloeistof tegen lucht zonder korst;
   - een bewuste val is een bron met een geplande update (`scheduleTick`), met een cap per chunk (besluit 16);
   - water in features in vanilla's eindtoestand (`SpringLayout.settle`).
7. **Determinisme** (regels 23c + 35):
   - plaatsing begint in de eigen chunk;
   - lees alleen `WORLD_SURFACE_WG`, geseede ruis, de placement-random en de pure veldkolom;
   - geen `count_on_every_layer`, `random_patch`, `environment_scan`, vanilla `geode` of vanilla disks;
   - schrijf alleen in de eigen chunk.
   - Uitzonderingen alleen met een rangregel (`GrowthPriority`-patroon): boomkronen (sjabloon `PalmFoliagePlacer`) en sjablonen > 14 breed (titanen-rotsen). Ze worden per uitzondering in "Beslissingen" vastgelegd.
   - Paden over chunkgrenzen (voetsporen) zijn een pure functie van seed + cel; elke chunk schrijft zijn eigen stuk.
8. **Eén globale featurevolgorde.**
   - Alle ring 3-features per generatiestap in één lijst (`Ring3Features`); elk biome krijgt een deelverzameling in dezelfde relatieve volgorde. Anders gooit vanilla "Feature order cycle found".
   - Nieuwe ring 3-biomes gebruiken `Ring3Biomes.generation`.
   - **Feature-indexverschuiving:** elke nieuwe placed feature verschuift de decoratie-seeds in ring 1/2 en ook binnen ring 3 (valkuilen change 30c/35). Coördinaten voor de W-checklist van decoratie pas opzoeken in een wereld met de eindcode van de golf. INT meet ring 1/2 op biomes en terreinvorm, niet op ertstellingen.
9. **Slapend of actief:** aankleding in kraters volgt `caldera_dormant`. Slapende kraters krijgen het oase-pakket, actieve krijgen niets op de lava.

## 1. Pakketten (volgorde; één commit per pakket)

### 41 — Ertsen ring 3 (kan direct)
- **Vulkaniet**: Y −48…40 (trapezium, piek ~Y 0), aders 6–9, ~6 pogingen per chunk; gastgesteenten steen, deepslate, basalt.
  - Een tweede placed feature boven op de eerste: "nabij lava", met meer pogingen waar binnen 4 blokken lava zit. Dat is alleen een terreinscan op de lavalaag en de lava uit de aquifer, dus volgorde-onafhankelijk.
- **Zwavel**: ondiep, 4–30 blokken onder `WORLD_SURFACE_WG` (relatief, want het terrein ligt op Y 70–250); gastgesteenten steen en basalt.
  - Basis in heel ring 3, ~3× zoveel in `sulfur_fields`.
  - In 47 ook onder de fumarolen.
- **Vuurrobijn**: zeldzaam, Y −40…30, aders 3–5, 1–2 pogingen per chunk; gastgesteenten steen, deepslate, basalt.
  - De basaltvariant komt vanzelf in de basaltwanden van de magmakamers van 42. Tot 42 gemerged is, valt hij daar dus niet op.
  - Let op afgrondwanden onder Y 30: daar wordt vuurrobijn zichtbaar. Dat mag (risico = beloning boven de lavazee), maar noteer het.
- **Vanilla afgebouwd:** `RingVanillaOres.placed(context, Ring.RING_3, RING_3_FACTORS)` + biome modifier `add_ring_3_ores` in `Ring3BiomeModifiers` (`#is_ring_3`). Factoren: G2-WB1.
- **`dry_riverbeds`** (backlog r. 123): dezelfde ring 3-ertsset plus de afgebouwde vanilla-set van ring 3 via de biome modifier. `ring_chance` op ring 3 houdt ze in de ring 3-helft; de ring 2-set blijft in de ring 2-helft.
- **Tests:**
  - unit: keys, factoren per erts, doelblokken per gastgesteente, `ring_chance`;
  - S: tellen per Y-band en gastgesteente via de regiobestanden (methode change 25/31), in smeulwoud (4.750, 0), zwavel (4.270, 480) en de ring 3-helft van `dry_riverbeds`; ring 2-helft zonder ring 3-ertsen.

### 47 — Zwavelvelden, basaltstromen, zeekom, `ash_layer`, vlekken in `dry_riverbeds` (kan direct)
- **`sulfur_fields`:**
  - fumarolen op de toppen van de fumarole-heuveltjes van 39d (accessor via main, zie §0.1) en verspreid in de terraskommen;
  - een kraag van `sulfur_crust` en een paar zwavelertsblokken eronder (dezelfde feature, chunk-lokaal);
  - accenten: G2-WB8.
- **`basalt_flows`:**
  - smeulsteen-ventjes op de plateaus en basaltkeien;
  - accenten: G2-WB8.
  - De lavabuis-sleuven en -tunnels van 39d krijgen hier geen aankleding (42 doet de buizen).
- **`dried_seabed`:** de aankleding van G2-WB7.
- **`ash_layer` als feature** (backlog r. 193): eigen feature `ash_drift` legt 1–8 lagen op de grond, volgens een windruis (richting = de duinwind van 39d, 0,35 rad), dikker in de lijzijde en in geulen. **Besluit 39f (2026-10-08):** `ash_drift` legt ook laagjes (1–7) op de treden van de vlakte, zoals sneeuw in vanilla, zodat de traplijnen van 1 blok breken (39f maakt alleen het 2D-microreliëf grilliger).
  - Waar: `ashen_wastes`, randen van `titan_fields`, vlakke delen van `volcanic_peaks` en de rand van de zeekom.
  - `ember_woods` volgt in 47c, onder de bomen.
  - Nooit op lavaoevers, dunne vormen of flora; staat in de volgorde vóór de vegetatie.
- **Vlekken in `dry_riverbeds`** (backlog r. 122): `riverbed_scorched_patch` en `_speck` (`DryRiverbedsFeatures`, een ring 2-bestand dat in golf 2 alleen W-biomes raakt) van blackstone/basalt naar `ash_block`/`ash_layer`, met hier en daar een verkoolde stronk (`charred_log`, 1–2 hoog).
- **Tests:**
  - S: `/place feature` per feature;
  - in het wild: per biome 16 × 16 chunks;
  - determinisme (twee werelden, omgekeerd geladen).

### 47b — `titan_fields` (kan direct)
- **Inhoud volgens G2-WB6.** De 39d-ribben (20–60 hoog, bot-surface aan de voet) en de ribbenkasten zijn al terrein; 47b vult ze aan.
- **Sjablonen** (V1: eigen `.nbt` uit een deterministisch script):
  - script `tools/worldgen/Ring3Templates.java` (single-file, alleen JDK, vaste seed), sjablonen onder `data/corruptedadventure/structure/ring_3/`;
  - `surface_fossil` krijgt een optionele bloklijst i.p.v. alleen `bone_block`, zodat basalt-titanen kunnen;
  - sjablonen > 14 breed volgens de rangregel (§0.7).
- **Zielvuur** (V2: vanilla, geen hittebron): `soul_soil` + `soul_fire` in en rond de ribbenkasten en bij schedels.
  - Vuur alleen op `soul_soil` (blijvend).
  - Nooit naast verkoold hout (niet brandbaar, dus veilig), maar wel ≥ 2 van de randen van de eigen chunk.

### 49 — Horror: as-afgietsels en voetsporen (kan direct)
- **As-afgietsels:** kleine sjablonen (menselijke houdingen: kruipend, knielend, staand met arm omhoog, liggend), materiaal `ash_block` + `smoldering_ash`. Die laatste gloeit 's nachts via de `_glow`-laag en geeft licht 2.
  - Chunk-lokaal; ze kijken naar de dichtstbijzijnde vulkaan (rotatie uit de pure vulkaanbeslissing; implementatiekeuze).
  - Frequentie en verspreiding: G2-WB10.
- **Voetsporen:** een spoor van indrukken in aslagen (een laag 6–7 in een veld van 8, of een lege plek in een laag 2–3) dat abrupt stopt.
  - Het eindigt bij een afgietsel, aan de rand van een krater of afgrond, of nergens.
  - Pad = pure functie van seed + cel (sjabloon `LavaRivers`-index, maar klein); elke chunk schrijft zijn eigen deel; alleen op as-grond.

### 47c — Smeulwoud, slapende krater, lavavallen, geiten, eilanden (na de 39f-merge voor de lavavallen)
- **`ember_woods`:** bomen volgens G2-WB4.
  - Vanilla `minecraft:tree` met `charred_log` + `ember_leaves`, eigen trunk- en foliage-varianten, `dirt_provider` = `coarse_dirt`.
  - Het placed feature filtert zelf de grond (valkuil "boom zonder blad"); plaatsing via `jittered_grid`.
  - Vuurflora (`fire_bloom`, `ash_sprouts`) als chunk-lokale vlekken (geen `random_patch`); aslagen onder de bomen.
  - `ember_berry_bush` zeldzaam in open plekken: `would_survive` + open lucht, want hij groeit alleen terug bij licht ≥ 9 (backlog r. 159/160).
- **`dormant_caldera`:** de oase volgens G2-WB5.
- **Lavavallen:** volgens G2-WB3. Het mechanisme sluit aan op de eilandbronnen van 39d/39f: één regel, niet twee. Daarom bouw je dit pas op de 39f-stand.
- **Geiten:** spawns staan sinds 39b. Controleer na 39d dat de piek-bovenlaag op `#goats_spawnable_on` staat (basalt, smooth basalt, blackstone, as, puimsteen); alleen bijstellen als ze niet spawnen.
- **Zwevende eilanden en archipels:** aankleding volgens G2-WB13 (na 39f: de archipel verandert daar).

### 48 — `steam_fens` (overgang 3→4; na de 39f-merge)
- **Biome:** `steam_fens` in `Ring3Biomes.ALL`, `is_ring_3`, lang; voorlopige kleuren via de haak van A (zie Afhankelijkheden).
- **Preset:** `transitions.ring_3` = `steam_fens`. Volgens de harde regel van het contract (§4) haalt dezelfde change `ring_3` uit `switch_at` van `ring_blend/overworld_temperature`, `_vegetation` en `_weirdness` (`Ring3JsonTest`).
  - Die drie bestanden horen bij W-terrein (39a), maar het contract wijst deze edit aan de stroom die de overgangsbiome toevoegt. 42 raakt ze niet.
- **Surface:** een eigen tak (controlelijst "Plekken die ring 3-biomes opsommen"), via `tools/worldgen/ring3_surface.py`. 39f raakt dat script ook; daarom pas ná de 39f-merge. **Afspraak (main):** W-nether verplaatst het Nether-3-deel (`nether()`, de constanten erboven, de nether.json-regels in `main()`) naar `nether_ring3_surface.py`; dat blok in mijn versie niet aanpassen. 39f herschrijft het overworld-deel en merget eerst; 48 rebaset daarop en draait de scripts opnieuw.
- **Aankleding** volgens G2-WB12: kokende modderpoelen (`boiling_mud`), geisers en stoomkolommen (fumarolen/geisers met `steam_puff` van A).
  - Geisers krijgen bij plaatsing `WorldGenLevel.scheduleTick(pos, GEYSER, GeyserBlock.INTERVAL_TICKS)` (backlog r. 178).
  - **Let op (B, 46-rest, na de merge van B):** `GeyserBlock.INTERVAL_TICKS` verdwijnt (ritme per positie 80–200 ticks). Dan bij plaatsing `ModBlocks.GEYSER.get().startCycle(level, pos)` aanroepen (LevelAccessor, dus ook WorldGenLevel; eerste tick = waarschuwing) i.p.v. `scheduleTick(..., INTERVAL_TICKS)`; zonder aanroep start de geiser bij zijn eerste random tick. Tot die merge geen afhankelijkheid van `INTERVAL_TICKS` toevoegen.
- **Terreinstand:** ring 4 is placeholder. `ring3Seal` vult in de ring 3-helft van zone 3→4 open ruimte onder Y 63 met rots; water alleen in features in de eindtoestand en boven Y 63.
  - Hitte geldt in de ring 3-helft (`HeatRule.inZone` hard op 0,5); geplaatst water verdampt daar ook.

### 41b — Vuurkristal-geodes, bekleding (na de 42-merge)
- **Holte:** de holte is een DF van W-terrein (42, sjabloon `DewGeodes`, pure vorm `FireGeodeShape`). 41b leest dezelfde pure vorm (alleen lezen) en bekleedt hem.
- **Feature `fire_geode`** (sjabloon `DewGeodeFeature`), van buiten naar binnen:
  - schil smooth basalt;
  - blackstone;
  - binnenwand `fire_crystal_block`;
  - knoppen en clusters (licht 1/2/4/5, emissive).
- Geheel binnen de eigen cel (3×3-regel van de dauwgeodes). Gesloten of aangesneden: G2-WB2.

## 2. Bestanden (eigenaarschap golf 2)
- **Eigen:**
  - `Ring3Biomes`, `Ring3BiomeModifiers`, `Ring3Features` (+ sub-bootstraps per pakket, bijv. `Ring3OreFeatures`, `Ring3DressingFeatures`, `TitanFieldsFeatures`, `EmberWoodsFeatures`, `SteamFensFeatures`, `Ring3HorrorFeatures`), `Ring3Carvers` (blijft leeg), `Ring3BiomeTags`, `Ring3BiomeNames`;
  - nieuwe feature-types en -configs in `worldgen/feature/` met ring 3-namen, `VolcanicFilter` (placement);
  - `DryRiverbedsFeatures` (alleen de vlekken);
  - `tools/worldgen/Ring3Templates.java` + `data/corruptedadventure/structure/ring_3/*.nbt`;
  - preset: ring 3-overworld-source en `transitions.ring_3`;
  - `noise_settings/overworld.json` (ring 3-takken) en `tools/worldgen/ring3_surface.py` (gedeeld met 39f en 42: zie Afhankelijkheden).
- **Append-blokken** (waves §5), elk met kopcommentaar `// ring 3 — W-biomes` aan het eind: `ModFeatures`, `ModPlacementModifierTypes`, `ModConfiguredFeatures`/`ModPlacedFeatures` (als die gebruikt worden).
- **Uitzondering contract §4 (48):** `switch_at` in de drie `ring_blend/overworld_*`-bestanden.
- **Niet aanraken:**
  - `VolcanicField` en familie, `density_function/**`, `RingAquifer`/`RingFluidPicker` (W-terrein/39f);
  - `BiomeAmbience` en `ModSoundEvents`/`ModParticleTypes` (A);
  - `ModBlocks`/`ModTags`-inhoud (B; nieuwe blokken of tags via main).

## 3. Afhankelijkheden
- **39f** (loopt parallel, raakt `VolcanicField`, `Landmarks`, `ring3_surface.py`):
  - 41, 47, 47b en 49 kunnen ervóór, maar coördinaten en terreindetails kunnen na 39f verschuiven. Rebase op de 39f-merge vóór 47c, en meet de W-checklist pas op die stand.
  - 47c (lavavallen, eilanden) en 48 (surface-script) pas ná de 39f-merge.
- **42 (W-terrein, ná 39f):**
  - 41b wacht op de geode-DF + `FireGeodeShape`;
  - vuurrobijn in basaltwanden wordt pas zichtbaar na 42 (geen code-afhankelijkheid).
  - 42 voegt eigen takken toe aan `ring3_surface.py` (wanden van buizen en kamers). Wie als tweede merget, draait het script opnieuw (idempotent, valkuil 39c); conflicten in `overworld.json` worden niet met de hand opgelost.
- **A:**
  - de definitieve biome-effecten (kleuren, particles, ambient) komen via `BiomeAmbience` (A, 50);
  - `steam_puff` en de blokgeluiden zijn van A.
  - Voorstel (zie `golf-2-atmosphere.md`, voorpakket 50-0): A zet vóór de golf de haken klaar (`BiomeAmbience.ring3(key)` met de huidige `PALETTES`, gedrag-neutraal), zodat W-biomes er vanaf de start mee bouwt.
  - Zonder voorpakket: W-biomes houdt de voorlopige kleuren en de integratie zet de ene regel om.
  - Tot `steam_puff` bestaat, gebruiken fumarolen en geisers de vanilla-particles van B (zit in de blokken, niet in de features).
- **B:** alle contractblokken bestaan sinds golf 1. Nieuwe blokken (bijv. een eigen schedelblok) alleen via main.
- **T:** geen; T stuurt bij op het in-game beeld van deze features.
- **C:** leest `heat_refuges` (`dormant_caldera`) en `heat_sources`; geen nieuwe tags tenzij G2-C4 dat vraagt.

## 3b. Afhankelijkheid 39f
39f (worldgen, parallel) zet alle zwevende eilanden met hun top op ≤ Y 185 (archipel Y ~150–185, plaat van de zwevende vulkaan Y ~165–175), sluit vanilla-grotingangen in ring 3 ~8 onder het maaiveld af en maakt eigen zinkgaten (~1 per 250–350 blokken), vervangt de coarse_dirt-vlekken van `ember_woods` door ring 3-blokken en zet ~1 boog per riftdal. Features die op eilanden, zinkgaten of de `ember_woods`-surface leunen: pas definitief afstemmen na de merge van 39f (rebase).

## 4. Open vragen (ontwerp; nog niet in design-docs, contract of beslissingenlog)
Aanbevolen optie eerst. Consequenties: **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-WB1 — Hoe ver bouwen we vanilla-ertsen af in ring 3?** (ring 2 = 0,6 / 0,5 / 0,25 voor gewoon / midden / zeldzaam)
- **A. 0,35 / 0,3 / 0,1 (aanbevolen).** C laag (één regel). S: ijzer en kolen voelbaar schaars, diamant bijna weg ("verder afgebouwd dan ring 2"); vulkaniet wordt het hoofdmateriaal. P geen.
- **B. 0,5 / 0,4 / 0,15.** S mild; minder druk om vulkaniet te zoeken.
- **C. 0,2 / 0,2 / 0,05.** S hard; zonder vulkaniet weinig voortgang. Kan frustreren vóór fase 2 (gear).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB2 — Vuurkristal-geodes: hoe vind je ze?**
- **A. Gesloten (wandtest zoals de dauwgeodes), diep (Y −50…20), ~1 per 12 chunks, straal 4–7; soms aangesneden door een lavabuis van 42 (aanbevolen).** C middel (sjabloon dauwgeodes + één snijregel met de buizen). S: echte vondst; de gloed in een buiswand lokt. P: DF-holte, binnen het budget (gemeten bij 42).
- **B. Altijd gesloten, iets vaker (~1 per 8 chunks).** C laag. S: je vindt ze alleen al graven.
- **C. Zeldzaam maar groot (straal 8–12, "kristalkathedraal", ~1 per 40 chunks).** C middel. S: wow-landmark ondergronds; veel spelers missen hem. P: grotere holte, iets duurder.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB3 — Waar komen de "spaarzame lavavallen" (47c)?** 39d/39f hebben al vallen vanaf de eilandbronnen.
- **A. Alleen waar de val in bestaande lava eindigt: kraterwanden boven het meer, rivierkloven, afgrondwanden boven de lavazee; ~1 per 2–4 vulkanen, cap 2 bronnen per chunk; mechanisme gedeeld met de eilandbronnen (aanbevolen).** C middel. MP: stromende lava is server-side en deterministisch na het uitzakken. S: een val wordt een landmark zonder over het land te lopen (geen branden, geen lavaplaten). P: weinig blokupdates.
- **B. Ook van kliffen en plateaus op het land, met een poeltje aan de voet.** C middel. S: meer gevaar en spektakel. Risico: lava loopt over smeulwoud en paden; de poel moet een korst krijgen.
- **C. Geen extra vallen; alleen die van de eilanden.** C geen. S: minder levendig bij de kraters.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB4 — Smeulwoud (`ember_woods`): welk woud?** (V3: stam verkoold, `ember_leaves` emissive, 6–12 hoog, "dicht genoeg voor een woud")
- **A. Gemengd woud, ~6–8 bomen per chunk met open plekken: smeulbomen 6–9 (bolle kroon), smeulreuzen 10–12 (getrapte kroon), kale verkoolde stammen en omgevallen stammen; bes-struiken en vuurbloemen in de open plekken (aanbevolen).** C middel (3 boomvormen + omgevallen stam). S: 's nachts een zee van gloeiende kronen met donkere gaten; voedsel in de open plekken; monsters spawnen onder het dak (spannend). P: features-stap hoger dan ring 2 (bomen); meten.
- **B. Dicht, gelijkmatig woud (~10 per chunk), één boomvorm.** C laag. S: indrukwekkend maar eentonig; weinig licht, dus de bes-struik groeit nergens terug.
- **C. Open, savanne-achtig (~3 per chunk).** C laag. S: rustig, minder "woud"; minder wow 's nachts.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB5 — Slapende krater (`dormant_caldera`, de oase): wat staat erin?** (V8: groen, koel, geen natuurlijk water)
- **A. Mos en grove aarde met varens, azalea-struiken en hangende wortels/glow lichen langs de kraterwand, zeldzaam een levende boom (azalea) als baken, `ember_berry_bush` aan de rand (aanbevolen).** C laag (vanilla-features, chunk-lokaal). S: duidelijk rustpunt, van boven herkenbaar groen. P laag.
- **B. Alleen mos, grove aarde en varens.** C laag. S: sober, minder "beloning".
- **C. Als A + een "verloren tuin": vuurbloem-weide en een kring van verkoolde stenen.** C middel. S: meer fantasy, iets drukker.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB6 — Titanenveld: wat doet 47b nog naast de 39d-ribben (20–60, al met bot-surface)?** (V1 noemde skeletten van 20–40 als features)
- **A. Aanvullen i.p.v. verdubbelen: schedels en losse wervels/botten als kleine sjablonen (chunk-lokaal), zielvuur op zielgrond in en rond de ribbenkasten, en zeldzame titanen-rotsen (15–35 hoog, ~1 per biomevlak; 3×3 met rangregel) (aanbevolen).** C middel. S: de ribben blijven het beeld; zielvuur en schedels geven het verhaal; titanen-rotsen worden het landmark. P laag.
- **B. Zoals V1: ook complete botskeletten van 20–40 tussen de ribben.** C hoog (grote sjablonen over chunkgrenzen, rangregel). S: drukker; botst visueel met de ribben.
- **C. Geen extra botten: alleen zielvuur en titanen-rotsen.** C laag. S: kaler, minder verhaal.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB7 — Drooggevallen zeebodem: welke aankleding?**
- **A. Dode koraalriffen (vanilla dood koraal: blokken, waaiers, planten) op de rifruggen, gedroogde kelpbalen en dode struikjes in de kom, en zeemonsterfossielen (bot, sjablonen) op de kustterrassen (aanbevolen).** C laag/middel. S: "hier was water" leest direct; fossielen geven wow. P laag.
- **B. Alleen dode koraalriffen.** C laag. S: herkenbaar maar kaal.
- **C. A + verdroogde zeeplanten in "zoutpannen" (ring 2-patroon).** C middel. S: rijker; het lijkt op ring 2's zoutvlakte.
- Let op: scheepswrakken en havens zijn structures (fase 3, backlog).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB8 — Zwavelvelden en basaltstromen: welke accenten naast de fumarolen?**
- **A. Zwavel: modderpoeltjes (`boiling_mud`) in de zinkgatbodems. Basalt: hornitos (spatkegeltjes van 2–4 met een kap van magma en vonken), gestolde lava-"tongen" (blackstone) (aanbevolen).** C middel (twee kleine vormfeatures). S: beide biomes worden "actief" (bubbelen, gloeien) zonder vrije lava. P laag.
- **B. Ook af en toe een geiser in de zwavelvelden.** C laag (blok bestaat). S: verrassing; geisers zijn dan niet meer exclusief voor `steam_fens`.
- **C. Alleen fumarolen en smeulsteen-ventjes.** C laag. S: rustiger, minder levendig.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB9 — Asvlakte en pieken: welke accenten?**
- **A. Asvlakte: aslagen in de lijzijde, losse verkoolde dode bomen in de geulen, puimsteenkeien. Pieken: "inslagkraters" van oude lavabommen (kuiltje met obsidiaanscherven en een magmakern) en lage blackstone-rotsen (aanbevolen).** C laag/middel. S: de lavabommen van het weer krijgen een spoor in het landschap; de vlakte blijft open. P laag.
- **B. Alleen aslagen en puimsteenkeien.** C laag. S: leeg, maar wel "woestenij".
- **C. A + verkoolde "spookbossen" (groepjes kale bomen) op de asvlakte.** C laag. S: sfeervol, maar het lijkt op het smeulwoud.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB10 — Horror: hoe vaak afgietsels en voetsporen, en waar?**
- **A. Afgietsels ~1 groepje (1–3) per 3–4 chunks in `ashen_wastes` en zeldzaam in `ember_woods`/`titan_fields`; voetsporen ~1 per 20 chunks, 15–40 stappen, eindigend bij een afgietsel, kraterrand of afgrond; afgietsels kijken naar de dichtstbijzijnde vulkaan (aanbevolen).** C middel. S: geregeld een rilling, niet overal; 's nachts gloeiende figuren. P laag.
- **B. Vaak (overal in ring 3, ~1 per chunk in de asvlakte).** C laag. S: sterk thema, maar het went snel.
- **C. Heel zeldzaam (vondsten, ~1 per 10 chunks).** C laag. S: elk afgietsel is een schrik; veel spelers zien ze nauwelijks.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB11 — Levende elementen: komen er extra vanilla-wezens in ring 3 (naast de geiten)?** (contract: geen vee behalve geiten; monsters = woestijnset tot fase 4)
- **A. Striders op de lavarivieren en kratermeren (zeldzaam, alleen op lava) (aanbevolen).** C laag (spawns in biome-data). MP geen. S: de lava leeft; een rijdbaar "lavapaard" als beloning (met warped fungus on a stick uit de Nether). P verwaarloosbaar.
- **B. A + magmakubussen 's nachts in `basalt_flows`.** C laag. S: meer gevaar; dat is eigenlijk `mobs` (fase 4).
- **C. Niets extra tot fase 4.** C geen. S: ring 3 blijft stil buiten de geiten.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB12 — `steam_fens` nu bouwen terwijl ring 4 nog placeholder is: hoe ver?**
- **A. Biome, surface en aankleding nu (modderpoelen, geisers, stoomkolommen, kleine kookpoelen met water in kommen boven Y 63); de terreinvorm blijft de menging met de ring 4-placeholder tot ring 4 (aanbevolen).** C middel. MP: water in features in eindtoestand. S: de overgang 3→4 is af voor 4b; "het gestolen water kookt" is zichtbaar. Risico: bij ring 4 nog afstemmen (backlog `ring3Seal`).
- **B. Biome en surface nu, aankleding samen met ring 4.** C laag. S: de grens 3→4 blijft kaal in 4b.
- **C. Zoals A, maar zonder water (alleen modder en stoom) tot ring 4.** C laag/middel. S: minder verhaal; geen waterrandgevallen (verdamping).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WB13 — Zwevende eilanden en archipels: aankleden?** (contract §4 noemt "eilandaankleding"; 39f past de archipel aan)
- **A. As en smeulende as op de toppen, 1–3 smeulbomen op grote eilanden, en vuurkristalclusters die aan de onderkant hangen (gloeien 's nachts, van onderaf zichtbaar) (aanbevolen).** C middel (onderkant: plaatsing aan een plafond). S: wow van onderaf, beloning voor wie erop klimt. P laag.
- **B. Kaal basalt (contrast met de kraters).** C geen. S: strak, minder reden om erheen te gaan.
- **C. "Hangende tuin": slapende-krater-groen op de grote eilanden.** C middel. S: fantasy-oase in de lucht; concurreert met `dormant_caldera`.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
Alleen worldgen-data en features (server). Geen netwerk en geen client-code. Biome-effecten zijn data; de client rendert ze. Geisers en stromende lava draaien server-side.

## 6. Tests (per pakket)
- **B:** `./gradlew build` groen (unit tests, `ClientIsolationTest`).
  - Nieuwe unit tests: `VolcanicFilter` (grenzen per uitgang, `ground_only` op een naald/eiland/brug), ertsconfig, featurevolgorde (geen cyclus: alle ring 3-biomes in één `FeatureSorter`-run), sjabloongenerator (2× → identieke `.nbt`), voetspoorpad (puur, chunkstukken sluiten aan), `Ring3BiomesTest` + `Ring3RegionSharesTest` (+ `steam_fens` in 48), `Ring3JsonTest` (`switch_at` in 48).
- **D:** `runData` groen; de output gecommit in de eigen branch.
- **S:** server (poort 25572), RCON; `/place feature corruptedadventure:ring_3/<naam>` per feature; `/locate biome` per biome. Geen fouten "Detected setBlock in a far chunk" of "Feature order cycle".
- **Determinisme:** twee verse werelden (seed `20260930`), de tweede omgekeerd geladen. Per biome 16 × 16 chunks + de landmarkgebieden A–E van 39d.
  - Alle eigen blokken (features) identiek, na 60 s uitzakken.
  - Verschillen alleen in vloeistofstroming en vanilla-ertsklodders.
- **Feature-index:** ring 1/2 tegen main op biomes en terreinvorm (heightmap). Ertsen en decoratie mogen verschuiven: noteren, niet oplossen. De officiële meting doet INT.
- **Prestatie:**
  - W-biomes raakt de noise-stap niet; 48 raakt wel de biomes-stap (router-klimaat zone 3→4).
  - Meet de **features-stap** en de **wandtijd** (16 × 16, warm-up over alle ringtypes, ≥ 3 rondes, JFR `minecraft.ChunkGeneration`) in smeulwoud, titanenveld, asvlakte en `steam_fens` tegen vanilla.
  - Geen vast budget, maar stoppen en melden boven 2× vanilla in de features-stap of als de wandtijd per gebied > 10 % stijgt t.o.v. main.
  - Noise-budget (≤ 1,75×, mediaan per gebied) alleen ter controle in 48.
- **Review:** de agent meldt "klaar voor review-doorsturen: <pakket>" met een korte diff-samenvatting; main draait `/code-review medium` en stuurt de uitslag door. Pas daarna committen (opdracht 05; vervangt waves §4.9 voor golf 2).

## 7. W-checklist (in-game, verse wereld seed `20260930`, na 39f; coördinaten x, z)
Biomepunten uit 39b (de regio-assen zijn niet veranderd): ashen (5.262, 480), peaks (4.110, 672), ember (4.750, 0), basalt (4.814, −288), sulfur (4.270, 480), seabed (5.710, −32), titan (4.942, 160). Landmarks 39d: ribbenkasten (−1.493, 4.144) en (−1.161, 4.327), afgronden (4.531, −413) en (5.120, 2.354), archipels (2.622, −2.738), (4.993, −2.124), (5.494, 1.413), boog over een rivier (−5.168, 829). Coördinaten van decoratie pas na de eindcode opnieuw opzoeken (feature-index).
- [ ] **41:**
  - in een grot of rift onder smeulwoud (4.750, 0): vulkaniet in steen/basalt Y −40…40, meer bij lava;
  - zwavel ondiep in de zwavelvelden (4.270, 480);
  - vuurrobijn zeldzaam en gloeiend;
  - ijzer en kolen zichtbaar schaarser dan in ring 2;
  - `dry_riverbeds`: ring 3-ertsen alleen in de ring 3-helft (`/ca tpboundary 2`, ~50 oost).
- [ ] **47:**
  - fumarolen puffen op de heuveltjes van de zwavelvelden, met een zwavelkraag;
  - koraalriffen en fossielen in de zeekom (5.710, −32);
  - aslagen in de lijzijde van de duinen (5.262, 480), nooit op lavaoevers of naaldtoppen;
  - vlekken in `dry_riverbeds` grijs as + verkoolde stronken.
  - (INT2) basaltstromen: hornitos met gloeiende kap, blackstone-tongen, smeulventjes; pieken: inslagkraters met obsidiaan en magmakern, niet boven vloeistof; asvlakte: verkoolde dode bomen in de geulen, puimsteenkeien;
  - (INT2) een fumarole op (bijna) elke heuveltop van de zwavelvelden, modder midden in de zinkgaten, niets in instortingen;
  - (INT2) aslagen lopen door over chunkgrenzen en stoppen netjes op biomegrenzen (geen rechte chunknaden); modderpoelen alleen op zinkgatbodems;
  - (INT2) `/place feature corruptedadventure:ring_3/<naam>` per feature; geen "far chunk"/"Feature order cycle"; determinisme (omgekeerd geladen).
- [ ] **47b:** ribbenkasten (−1.493, 4.144): zielvuur, schedels/wervels; een titanen-rots in het titanenveld (4.942, 160). 's Nachts blauw zielvuur tegen de botten.
  - (INT2) titanen-rots heel over chunkgrenzen (geen afgesneden stukken, ook omgekeerd geladen), niet zwevend, niet op een rib of in lava; `/place feature corruptedadventure:ring_3/titan_rock` in een titanenveld;
  - (INT2) zielvuur alleen op zielgrond rond de ribvoeten en onder de wervelkolom, nooit aan een chunkrand; schedels in de buurt van de ribbenkasten.
- [ ] **49:** 's nachts gloeiende afgietsels in de asvlakte; een voetspoor dat abrupt stopt.
  - (INT2) een voetspoor loopt ononderbroken over chunkgrenzen (ook omgekeerd geladen), alleen in as; afgietsels staan vlak, nooit half over een chunkrand of in een aslaag;
  - (INT2) afgietsels kijken (op 90° na) naar de dichtstbijzijnde vulkaan; een deel van de sporen stopt aan de rand van een instorting of krater.
- [ ] **47c:**
  - smeulwoud (4.750, 0) dag en nacht (gloeiende kronen, open plekken met bessen en vuurbloemen);
  - slapende krater (~−4.915, 560 of `/locate biome corruptedadventure:dormant_caldera`) groen;
  - lavavallen bij de actieve caldera (~−3.870, −1.790) en de horizonreus (~−3.895, 65);
  - geiten op de pieken (4.110, 672);
  - archipel (2.622, −2.738) aangekleed.
  - (INT2) smeulwoud: geen bladeren die een stam vervangen of half afgesneden kronen op chunkgrenzen; determinisme: bij verschillen alleen `distance` van `ember_leaves` (en vloeistof) toegestaan; open plekken met omgevallen stammen, bessen en vuurflora; aslaagjes rond de stammen;
  - (INT2) slapende krater: azalea-baken, varens, struiken, glow lichen tegen de wand, bessen aan de voet;
  - (INT2) lavavallen: een val loopt vanuit een wand in bestaande lava (kratermeer, rivier, lavazee), niets over de rand; tellen per ~10 actieve kraters; geen lava op het land;
  - (INT2) eilanden: smeulende as + aslagen boven, vuurkristallen hangen onder (van onderaf zichtbaar, 's nachts gloeiend), af en toe een smeulboom op grote eilanden;
  - (INT2) striders op lavarivieren en kratermeren (zeldzaam), geiten nog op de pieken.
- [ ] **48:** `/ca tpboundary 3`: `steam_fens` met modderpoelen, geisers die spelers lanceren (ook direct na het genereren, want de tick is gepland), stoom; geen water tegen lucht; ring 4-kant ongewijzigd.
  - (INT2) `/locate biome corruptedadventure:steam_fens` vindt de zone 3→4 (beide helften); klimaat wisselt niet meer hard op de middellijn van zone 3→4 (`switch_at`); geen gras/aarde van vanilla in de zone; kookpoelen alleen boven Y 63, niet gelekt; ring 3-ertsen in de ring 3-helft (via `is_ring_3`), ring 4-helft beoordelen.
- [ ] **41b:** een geode (coördinaten uit de pure vorm, door de agent opgezocht) met gloeiende clusters; niet door een afgrondwand of in lucht.
- [ ] Algemeen: geen features op naalden, bogen, ribkruinen, brugdekken of lavaoevers; geen zwevende brokken boven Y 140; levendigheid: 2 min lopen per biome, en elk biome heeft iets eigens.

## Voortgang
Worktree: `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-a26041cd066434c4c`, branch `ring3/biomes-g2` (vanaf `dbb1cb9`).
- Voortgang: 2026-10-08 14:00 start; 41 code (ertsen, `add_ring_3_ores` incl. `dry_riverbeds`) geschreven; build wachtte ~20 min op een JVM-slot.
- Voortgang: 2026-10-08 14:30 41: B groen (559 tests), D groen; S + determinisme gedaan (zie hieronder). Klaar voor review.
- **41 — testresultaten** (seed 20260930, 8×8 chunks per gebied, telling uit de regiobestanden):
  - smeulwoud (4.750, 0): vulkaniet 36,6/chunk (steen 11,8 Y 0…40, deepslate 24,8 Y −60…0; basalt 0: basalt ligt alleen in het oppervlaktepakket boven Y 40, dat komt met de magmakamers van 42), zwavel 11,4/chunk (diepte onder het oppervlak mediaan 24 in steen / 11 in basalt, 5–95 % 9…31), vuurrobijn 2,6/chunk (Y −60…30, licht 3).
  - zwavelvelden (4.270, 480): zwavel 26,5/chunk (×2,3 t.o.v. de rest; 3× zoveel pogingen, de klodders vullen minder).
  - vanilla t.o.v. ring 2 (2.500, 0): ijzer 27 vs 46/chunk (×0,58 = 0,35/0,6), diamant 2,1 vs 6,0 (×0,35 ≈ 0,1/0,25), kolen hoger dan ×0,58 in hoog terrein (meer volume voor `ore_coal_upper`).
  - `dry_riverbeds` (strook x 3.392…3.631, 8 rijen): ring 3-ertsen pas vanaf de (ruisende) middellijn, per rij een schone grens; in de ring 2-helft geen ring 3-erts. Zonnesteen/turkoois (ring 2, zonder `ring_chance`, gebonden aan kloofsteen) liggen ook in de ring 3-helft waar kloofsteen doorloopt: zie Backlog.
  - `/place feature` van de drie ertsen: OK. Geen "far chunk", geen "Feature order cycle".
  - Determinisme (wereld B omgekeerd geladen): sulfur 64/64, ashen 62/64 en strip 119/120 identiek (verschil alleen water/lava), ring2 alleen vanilla-klodders; ember 63/64: de ene chunk met mijn `/place feature`-tests.
- Voortgang: 2026-10-08 14:45 review 41 (main, `/code-review medium`, 9 bevindingen) verwerkt:
  1–2, 8. "nabij lava" las wereldblokken in de eigen chunk (`near_blocks`): buurchunks schrijven tijdens hun decoratie ook in deze chunk, dus volgorde-afhankelijk, en de clip op de chunk gaf randen. Vervangen door `terrain_lava` (`TerrainLavaFilter`): de eigen lava van het terrein per kolom (`VolcanicField.Column` lavaTop/lavaBottom/springY via `Ring3Terrain`), puur. `near_blocks` en de sectiescan zijn weg (geen dubbel met `HeatSources` meer).
  3. De lavascan staat nu ná `ring_chance` en de biome-filter.
  4. Javadoc `Ring3BiomeModifiers` gecorrigeerd (zonnesteen/turkoois lopen door in de ring 3-helft, backlog).
  5–6. Nieuwe `FeatureOrderTest`: de echte biomelijsten uit de gegenereerde biome-JSON + de gegenereerde modifiers (op naam, met tags en `neoforge:or`), per dimensie, topologische sortering (vindt ook n-cycli; eigen test met een 3-cyclus). De synthetische test in `Ring3BiomesTest` is weg.
  7. Zwavel hangt nu aan de terreinhoogte zelf (`terrain_height`, `TerrainHeightPlacement`), niet aan `WORLD_SURFACE_WG` (eilanden, bogen, naalden, lava).
  9. CODEBASE/beslissingenlog: via de delta-secties hieronder (waves §4.8).
- Voortgang: 2026-10-08 14:55 hertest 41: B groen, D groen; S: ember/sulfur/abyss/strip (8×8, strook 15×8) zonder far-chunk/cycle; afgrond (4.531, −413) nu basalt-vulkaniet 3,8/chunk en basalt-vuurrobijn in de afgrondwanden (nabij de lavazee). Determinisme: ember, sulfur, abyss 64/64 identiek; strip 113/120 met alleen vanilla-klodders (dioriet/andesiet), water, en 3 blokken `deepslate_volcanite_ore`: een `minecraft:ore`-klodder die over een chunkgrens schrijft, hetzelfde verschil als bij vanilla-ertsen (plan §6: "vanilla-ertsklodders").

- Voortgang: 2026-10-08 15:30 verse agent neemt 47/47b/49 over (zelfde worktree en branch, HEAD a389396); start 47 vanuit `build/draft47/`.
- Voortgang: 2026-10-08 17:35 47 in de werkmap (niet gecommit); `compileJava` groen, `compileTestJava` faalde op een typfout in `Ring3TemplatesTest` (hersteld, nog niet opnieuw gebouwd). Gestopt op verzoek van main (einde dag).
- Voortgang: 2026-10-09 14:00 derde agent neemt over (zelfde worktree/branch). **Nieuwe testafspraak (gebruiker 2026-10-09):** per pakket alleen B + D, commit per pakket; in-game checks (S, determinisme, prestatie, W-checklist) doet INT2 één keer; review één keer per stroom.
- Voortgang: 2026-10-09 14:10 47: D groen (`runData`; datagen meldt "`ring_3/ash_drifts` … is missing BiomeFilter.biome()" — bewust, zie Beslissingen), B groen (571 tests, 0 fouten). Gecommit `d0a9264`.
- Voortgang: 2026-10-09 14:20 47b: sjablonen (titanen) in het script, twee runs byte-identiek; `TitanFieldsFeatures`, `TitanRock*`, `VolcanicNearFilter`. D groen (ook "missing BiomeFilter" voor `titan_fields_rocks`, bewust), B groen (573 tests, 0 fouten). Gecommit `f18db9a`.
- Voortgang: 2026-10-09 14:20 49: afgietsels (5 sjablonen, script twee runs byte-identiek, 47/47b-sjablonen onveranderd), voetsporen (`FootprintPath`), `Ring3HorrorFeatures`, `Ring3Terrain.facing/towards` (nog willekeurige rotatie tot 39f). D groen ("missing BiomeFilter" ook voor `ash_footprint_trails`, bewust), B groen (576 tests, 0 fouten). Gecommit `058fa34`.
- Voortgang: 2026-10-09 14:30 afspraak main over 48: B (`c31a8c5`, `GeyserBlock.startCycle`) merget naar main direct na 39f; de rebase op main brengt beide mee. 48 roept `startCycle` op één plek aan. Wacht op het rebase-signaal.
- Voortgang: 2026-10-09 15:00 rebase op main `91af98e` (39f + B) zonder conflicten (`Ring3BiomesTest`, `VolcanicThresholdConditionTest`, `Ring3TerrainJsonTest` automatisch samengevoegd, groen). 39f haalde de kolomuitgang `solid_kind` weg: 47/47b compileerden niet meer. Omgezet naar de 39f-accessors in één extra commit (de herschreven commits 47/47b compileren los dus niet op main; de branch-tip wel): `TerrainLandmarkPlacement` (fumarolen op elke heuveltop, modder midden in de zinkgaten, zielvuur en schedels rond de ribbenkasten), `ground()` met `collapse` i.p.v. `solid_kind`, afgietsels naar de dichtstbijzijnde vulkaan, voetsporen die aan een instortings- of kraterrand eindigen. `VolcanicNearFilter` weg (geen gebruikers meer). D groen, B groen met `--rerun-tasks` (603 tests, 0 fouten). Gecommit `39740e7`.
- Voortgang: 2026-10-09 15:40 47c: smeulwoud (bomen, omgevallen stammen, vuurflora, bessen, as), slapende krater (azalea, varens, bessen, glow lichen), lavavallen, eilanden (smeulende as, hangende vuurkristallen, smeulbomen), striders; geiten gecontroleerd (zie Beslissingen). D groen ("missing BiomeFilter" ook voor `lava_falls` en `ember_woods_ash`, bewust), B groen (604 tests, 0 fouten). Gecommit `60b6f2f`.
- Voortgang: 2026-10-09 16:10 48: `steam_fens` (biome, tags, namen, preset `transitions.ring_3`, `ring_3` uit `switch_at` van de drie `ring_blend/overworld_*`, surface-tak via `ring3_surface.py` (Nether-blok ongemoeid; `nether.json` en de ruisbestanden onveranderd), aankleding: kookpoelen boven Y 63, modderpoelen, geisers met `startCycle`, stoomfumarolen). D groen, B groen (604 tests, 0 fouten). Gecommit `125a3fc`; "klaar voor review-doorsturen: W-biomes" aan main.
- Voortgang: 2026-10-09 17:00 review W-biomes (main, `/code-review medium` over `ac65e6c..125a3fc`, 7 punten) verwerkt:
  1. Lavavallen alleen in de ring 3-kern: `lava_falls` staat in de lijst van `Ring3Biomes.ALL` (niet `steam_fens`), de config heeft `biomes` en de wand en de valkolom moeten erin liggen; de val moet echt in lava landen (eerste blok onder de open kant in de wereld = lava).
  2. `enclosed()` toetst ook de bovenkant van de bron.
  3. Voetsporen alleen op vlakke grond (`ground()` op de bevroren `WORLD_SURFACE_WG`), en alleen in de grond zelf: volle as net onder die hoogte met lucht erboven, of een aslaag op die hoogte. Een afgietsel of iets anders dat daar staat, wordt nooit ingedrukt; geen live heightmap meer. Het afgietsel aan het eind van een spoor ook alleen op `ground()`.
  4. Voetsporen per kolom in de juiste biome (`TRAIL_BIOMES`: asvlakte, titanenveld, pieken; biome-bron op de pure terreinhoogte, puur): het spoor stopt bij de eerste stap erbuiten, voorgoed (geen afgietsel meer aan dat eind).
  5. `terrain_landmark`: behouden en verplaatsen per punt uit een hash van seed + punt + `salt` (i.p.v. de placement-random per chunk); elk behouden punt landt in precies één chunk. Salts: heuveltoppen 1, zinkgaten 2, schedels 3, zielvuur 4.
  6. `ash_drift` (ook `ember_woods_ash`) stopt vroeg als geen biomecel (4 × 4) van de chunk een drift-biome is.
  7. Striders zonder jockeys (besluit gebruiker): `Ring3StriderSpawns` (FinalizeSpawnEvent, overworld, NATURAL/CHUNK_GENERATION: `finalizeSpawn` overgeslagen).
  D groen, B groen met `--rerun-tasks` (605 tests, 0 fouten).

## CODEBASE-delta
- **41:**
  - `worldgen/feature/Ring3OreFeatures`: ertsen ring 3 (keys `ring_3/volcanite_ore`, `…_near_lava`, `sulfur_ore`, `sulfur_ore_rich`, `fire_ruby_ore`; configured `ring_3/volcanite_ore|sulfur_ore|fire_ruby_ore`), `RING_3_FACTORS` (0,35/0,3/0,1), `OWN`; elk eigen erts met `ring_chance` progress ring 3 = 1 vóór de biome-filter.
  - `worldgen/feature/Ring3Terrain`: het pure ring 3-terrein voor features (`field(level)`: `VolcanicField.find(randomState.router().finalDensity())`, per RandomState onthouden, null buiten ring 3-terrein).
  - `worldgen/feature/TerrainLavaFilter` (`corruptedadventure:terrain_lava`, `ModPlacementModifierTypes.TERRAIN_LAVA`): eigen lava van het terrein binnen een straal (per kolom, puur).
  - `worldgen/feature/TerrainHeightPlacement` (`corruptedadventure:terrain_height`, `TERRAIN_HEIGHT`): positie naar de terreinhoogte van de kolom (nooit op eilanden, bogen, naalden of lava).
  - `worldgen/feature/Ring3Features`: `ORDER` (één globale featurevolgorde per stap, `Entry(feature, step, biomes)`), `features(biome, step)`; roept de sub-bootstraps aan.
  - `worldgen/biome/Ring3Biomes.generation(features, carvers, biome)`: carvers + de eigen features in `ORDER`-volgorde.
  - `worldgen/biome/Ring3BiomeModifiers.ADD_RING_3_ORES` (`add_ring_3_ores`): `neoforge:or` van `#is_ring_3` en `dry_riverbeds`, `UNDERGROUND_ORES`; `ores()`.
  - Tests: `Ring3OreFeaturesTest`, `Ring3BiomesTest` (eigen features in globale volgorde), `worldgen/FeatureOrderTest` (geen featurevolgorde-cyclus per dimensie, uit de gegenereerde biome-JSON + modifiers).
  - Valkuil: features die "nabij X" of "op de grond" moeten weten, lezen het pure terrein (`Ring3Terrain`), geen wereldblokken buiten wat de eigen chunk zelf al schreef: buurchunks schrijven tijdens hun decoratie in deze chunk.
- **47:**
  - `worldgen/feature/VolcanicFilter` (`corruptedadventure:volcanic_filter`, `ModPlacementModifierTypes.VOLCANIC_FILTER`): ranges per kolomuitgang van het pure ring 3-terrein + `ground_only` (`WORLD_SURFACE_WG` binnen `tolerance` van de terreinhoogte); `Builder.ground()` = plan §0.2–0.4.
  - Nieuwe feature-types (`ModFeatures`, blok `// ring 3 — W-biomes g2`): `ash_drift` (`AshDriftFeature`/`AshDriftConfiguration`: één keer per chunk, per kolom de biome, windruis `worldgen/noise/ring_3_ash_drift.json`, lee/geul/treden), `ground_patch` (`GroundPatch*`: vlekken/tongen/riffen op de grond), `ground_vent` (`GroundVent*`: vent + kraag + ertsblokken eronder), `hornito` (`Hornito*`), `impact_crater` (`ImpactCrater*`).
  - `worldgen/feature/Ring3DressingFeatures`: configured + placed van 47 (keys `ring_3/*`), `openGround()`; `Ring3Features.ORDER` gevuld (LOCAL_MODIFICATIONS en VEGETAL_DECORATION).
  - `Ring3BiomeModifiers.ADD_RING_3_RIVERBED_STUMPS` (`add_ring_3_riverbed_stumps`, VEGETAL, alleen `dry_riverbeds`); `DryRiverbedsFeatures`: `riverbed_scorched_patch`/`_speck` via `ground_patch` naar as + aslagen.
  - Sjablonen: `tools/worldgen/Ring3Templates.java` (single-file, alleen JDK, vaste seeds; draaien vanuit de repo-root) → `data/corruptedadventure/structure/ring_3/*.nbt` (47: `sea_serpent_1–3`, `sea_skull_1–2`).
  - Tests: `Ring3DressingShapesTest`, `Ring3TemplatesTest`.
- **47b:**
  - `worldgen/feature/TitanFieldsFeatures`: configured + placed van het titanenveld (`ring_3/titan_skull`, `titan_bones`, `soul_fire_patch`, `titan_rock`; placed `ring_3/titan_fields_skulls|_bones|_soul_fire|_rocks`), via `Ring3Features`.
  - `worldgen/feature/TitanRockFeature`/`TitanRockConfiguration` (`corruptedadventure:titan_rock`, `ModFeatures.TITAN_ROCK`): basaltreuzen 20–29 hoog, tot 29 breed; per cel (256) een pure functie van seed + cel, elke chunk schrijft zijn eigen deel (alleen in lucht/vervangbare planten), bereik ≤ 15 = 3 × 3 chunks; middelpunt op vlakke grond van de biome (pure kolom + `BiomeSource`).
  - `worldgen/feature/VolcanicNearFilter` (`corruptedadventure:volcanic_near`, `ModPlacementModifierTypes.VOLCANIC_NEAR`): een kolom van het pure terrein binnen een straal (schijf, elke `step` blokken) met de uitgang in het bereik (bijv. `solid_kind` = bot).
  - Sjablonen: `titan_skull_1–2`, `titan_vertebra_1–3`, `titan_bone_1–2` (bot, `surface_fossil`), `titan_rock_kneeling|hunched|bust` (basalt/smooth basalt/blackstone).
  - Tests: `TitanRockFeatureTest`, `Ring3TemplatesTest` (titanen).
- **Na 39f (accessors):**
  - `worldgen/feature/TerrainLandmarkPlacement` (`corruptedadventure:terrain_landmark`, `ModPlacementModifierTypes.TERRAIN_LANDMARK`): de sleutelpunten van een landmark van het pure terrein in deze chunk (`mound` top, `sinkhole` bodem, `collapse` midden, `ribcage` ribvoeten + wervels; via `VolcanicField.moundAt/sinkholeAt/collapseAt/ribcageNear`), met `chance`, `spread`, `margin`.
  - `VolcanicNearFilter`/`volcanic_near` (47b) vervallen.
  - `VolcanicFilter.Builder.ground()`: `collapse` < 0,001 i.p.v. `solid_kind` (bestaat niet meer); dunne vormen vallen af via `ground_only`.
  - `Ring3DressingFeatures.SULFUR_FIELDS_MOUND_FUMAROLES` (nieuw, `ring_3/sulfur_fields_mound_fumaroles`).
  - `Ring3Terrain.facing` via `nearestVolcano`; `FootprintPath.Rims`/`Rim` + `AshFootprintsFeature.rim` (instorting via `collapseAt`, krater via `nearestVolcano`).
- **47c:**
  - `worldgen/feature/EmberWoodsFeatures`: `ring_3/ember_tree` (6–9, blob-kroon), `ember_giant` (10–12, spar-kroon), `ember_wood_trees` (selector incl. kale `charred_dead_tree`), `fallen_charred_log_x|_z|s`, `fire_flora`, `ember_berry_bush`, `ember_woods_ash_drift`; placed `ring_3/ember_woods_trees|_fallen_logs|_fire_flora|_berry_bushes|_ash` (VEGETAL). Noises `worldgen/noise/ring_3_ember_clearings.json`, `ring_3_fire_flora.json`.
  - `worldgen/feature/DormantCalderaFeatures`: `ring_3/caldera_fern|_azalea_bush|_glow_lichen`; placed `ring_3/dormant_caldera_azalea_trees` (vanilla `azalea_tree`), `_azalea_bushes`, `_ferns`, `_berry_bushes`, `_glow_lichen` (VEGETAL).
  - `worldgen/feature/LavaFallFeature`/`LavaFallConfiguration` (`corruptedadventure:lava_fall`, `ModFeatures.LAVA_FALL`) + `Ring3LavaFallFeatures` (`ring_3/lava_fall`, placed `ring_3/lava_falls`, LOCAL_MODIFICATIONS, alle ring 3-biomes): een lavabron in een wand boven terreinlava (≥ 10 val), geplande tick, max. 2 per chunk.
  - `worldgen/feature/HangingClusterFeature`/`HangingClusterConfiguration` (`corruptedadventure:hanging_cluster`, `ModFeatures.HANGING_CLUSTER`) + `Ring3IslandFeatures` (`ring_3/island_embers`, `hanging_fire_crystals`; placed `ring_3/island_ember_patches`, `island_hanging_crystals` (LOCAL), `island_ember_trees` (VEGETAL); alle ring 3-biomes, via `island_mask`).
  - `Ring3Features`: helpers `vegetal(...)` en `everywhere(...)` (alle `Ring3Biomes.ALL`).
  - `Ring3Biomes.spawns`: striders (CREATURE, gewicht 1, groep 1–2, spawnkosten 1,0/0,12) in alle ring 3-biomes behalve de pieken; `Ring3BiomesTest` aangepast.
- **48:**
  - `Ring3Biomes.TRANSITION` (= `steam_fens`) en `Ring3Biomes.WITH_TRANSITION` (`ALL` + overgang): bootstrap, tags `is_ring_3` en `c:is_hot/overworld`, de ring 3-surface-tak en `Ring3Features.everywhere` lezen die; `ALL` blijft de 8 biomes van de eigen `multi_noise` (`Ring3RegionSharesTest`). `steam_fens`: downfall 0,9, `c:is_wet/overworld` (niet `is_dry`), namen "Steam Fens"/"Stoomvenen".
  - Preset `corrupted_world.json`: `transitions.ring_3` = `fixed` `steam_fens`; `ring_blend/overworld_temperature|_vegetation|_weirdness`: `switch_at` zonder `ring_3`.
  - `tools/worldgen/ring3_surface.py`: `steam_fens` in `RING_3` en `BIOMES` (modder, packed mud, klei, grind/as, zwavelkorst langs scheuren, grove aarde; tuff op steile wanden); `noise_settings/overworld.json` opnieuw geschreven.
  - `worldgen/feature/SteamFensFeatures`: `ring_3/boiling_pool` (`surface_pool`, water 1 diep, rand packed mud/zwavelkorst/klei), `fens_mud_pool`, `geyser`, `steam_vent`; placed `ring_3/steam_fens_boiling_pools` (boven Y 63), `_mud_pools`, `_geysers`, `_steam_vents` (LOCAL, poelen eerst).
  - `GroundVentFeature`: een `GeyserBlock` als vent krijgt `startCycle(level, pos)` (de enige plek; backlog r. 178).
- **Review W-biomes (na 48):**
  - `TerrainLandmarkPlacement`: veld `salt`; behouden + verplaatsing zijn een hash van seed, punt en salt (niet de placement-random): elk punt in precies één chunk.
  - `LavaFallFeature`/`LavaFallConfiguration`: veld `biomes` (wand en valkolom), echte landing in lava; bovenkant van de bron dicht. `ring_3/lava_falls` alleen in `Ring3Biomes.ALL` (niet in `steam_fens`).
  - `AshFootprintsFeature`/`AshFootprintsConfiguration`: veld `biomes` (`Ring3HorrorFeatures.TRAIL_BIOMES`); spoor stopt bij de eerste stap buiten die biomes; indrukken alleen op `ground()` in de grond (bevroren heightmap), nooit in afgietsels.
  - `AshDriftFeature`: vroeg stoppen zonder drift-biome in de chunk.
  - `worldgen/biome/Ring3StriderSpawns` (game-bus, `CorruptedAdventure` registreert hem): striders in de overworld zonder jockeys.
  - Stand na W-biomes voor CODEBASE (INT2): `steam_fens` bestaat (biome, `transitions.ring_3`); `switch_at` van `ring_blend/overworld_temperature|_vegetation|_weirdness` = `[ring_4, ring_5]`; `VolcanicFilter.Builder.ground()` = `lava_bank` < 0,1, `cliff` < 0,3, `volcano_rise` < 20, `caldera_mask` < 0,001, `collapse` < 0,001, `ground_only` 3; `TitanRockFeature` (cel 256, kans 0,4, bereik ≤ 15); `GroundVentFeature` start geisers.
- **49:**
  - `worldgen/feature/Ring3HorrorFeatures`: `ring_3/ash_cast`, `ring_3/ash_footprints`; placed `ring_3/ashen_wastes_ash_casts` (~1 groep per 3 chunks), `ring_3/rare_ash_casts` (`ember_woods`, `titan_fields`), `ring_3/ash_footprint_trails` (één keer per chunk, ná `ash_drifts`; `ashen_wastes`, `titan_fields`, `volcanic_peaks`).
  - `worldgen/feature/AshCastFeature`/`AshCastConfiguration` (`corruptedadventure:ash_cast`, `ModFeatures.ASH_CAST`): groepje van 1–3 figuren van `ash_block` + `smoldering_ash` op vlakke grond, binnen de eigen chunk.
  - `worldgen/feature/AshFootprintsFeature`/`AshFootprintsConfiguration` (`corruptedadventure:ash_footprints`, `ModFeatures.ASH_FOOTPRINTS`) + `FootprintPath` (puur spoor per cel 72, 15–40 stappen; einde NOTHING/CAST/RIM): elke chunk schrijft zijn eigen indrukken (laag 6–7 in volle as, −2 lagen in een dikke laag, leeg in een dunne).
  - `Ring3Terrain.facing(level, x, z, random)` (nu willekeurig; na 39f naar `nearestVolcano`) en `towards(dx, dz)`.
  - Sjablonen: `ash_cast_crawling|kneeling|reaching|lying|curled`.
  - Tests: `Ring3DressingShapesTest` (voetspoorpaden puur, rotaties), `Ring3TemplatesTest` (afgietsels).

## Beslissingen
- 2026-10-08 · worldgen · 41: vulkaniet aders 9, 6 pogingen (Y −48…40, trapezium) + 10 pogingen "nabij lava" (de eigen lava van het terrein binnen 4 blokken, puur per kolom; review 41); zwavel aders 7, 3 pogingen ringbreed + 6 in `sulfur_fields`, 4–30 onder de terreinhoogte zelf (twee verticale offsets: vanilla staat max. ±16 per modifier toe); vuurrobijn aders 4, 1–2 pogingen Y −40…30; blootliggende ertsen blijven (geen air-discard). Startwaarden, afstemmen in 4b.
- 2026-10-08 · worldgen · 41: ring 3-ertsen in `dry_riverbeds` via één modifier op `neoforge:or(#is_ring_3, dry_riverbeds)` met `ring_chance` progress ring 3 op elk eigen erts (implementatiekeuze; geen aparte modifier).
- 2026-10-09 · worldgen · 47: `ring_3/ash_drifts` draait één keer per chunk zonder `BiomeFilter`: de feature bedekt de chunk en kiest per kolom op de eigen biome (een biome-filter test de chunkhoek onderin de wereld en gaf naden op chunkgrenzen). Datagen logt daarom "missing BiomeFilter.biome()" (alleen de validatie van `VanillaRegistries`, geen runtime-effect). Hoeveelheden van alle 47-features zijn startwaarden, afstemmen in 4b.
- 2026-10-09 · worldgen · 47: sjablonen (zeemonsterfossielen) als eigen `.nbt` uit een deterministisch JDK-script (`tools/worldgen/Ring3Templates.java`), V1.
- 2026-10-09 · worldgen · 47b: titanen-rotsen (> 14 breed) zonder rangregel: elke rots is een pure functie van seed + cel (256 blokken, kans 0,4) en elke chunk die hij raakt schrijft zijn eigen deel; uitzondering op "plaatsing begint in de eigen chunk" (plan §0.7), volgorde-onafhankelijk. Placed zonder `BiomeFilter` (de feature toetst de biome op het middelpunt); datagen logt "missing BiomeFilter", bewust.
- 2026-10-09 · worldgen · 47b: `surface_fossil` krijgt géén bloklijst (plan §1 47b): de basalt-titanen hebben een eigen feature `titan_rock` met `materials`; schedels/wervels/botten zijn bot.
- 2026-10-09 · worldgen · 49: voetsporen als pure functie van seed + cel (72 blokken, kans 0,9 ≈ 1 per 20 chunks), één keer per chunk zonder `BiomeFilter` (datagen-melding bewust); niet in `ember_woods` (boomkronen van 47c zouden de live heightmap volgorde-afhankelijk maken). Een afgietsel aan het eind van een spoor valt weg als daar al een aslaag ligt. Afgietsels ~1 groep (1–3) per 3 chunks in `ashen_wastes`, kans 0,05 per chunk in `ember_woods`/`titan_fields`. Startwaarden, afstemmen in 4b.
- 2026-10-09 · worldgen · 47/47b/49 na 39f: fumarolen op elke heuveltop waarvan de kraag in de chunk past (marge 2) plus de bestaande op de kommen; modder midden in elk zinkgat; niets op instortingen (`collapse`); zielvuur (straal 1–2) rond ribvoeten en wervels (spreiding 3, marge 4); schedels kans 0,1 per ribvoet/wervel, tot 12 blokken ervandaan; afgietsels kijken naar de dichtstbijzijnde vulkaan (geen vulkaan in 3 × 3 cellen: willekeurig); RIM-sporen eindigen 2 blokken buiten de rand van de dichtstbijzijnde instorting of krater als dat punt in hun cel ligt, anders "nergens". Startwaarden.
- 2026-10-09 · worldgen · 47c: smeulwoud op een raster van 4 (marge 1, kans 0,45; stammen ≥ 3 uit elkaar) met open plekken uit een ruis (`ring_3_ember_clearings` ≥ 0,35, per cel), kronen mogen in de buurchunks reiken (uitzondering §0.7, zoals de palmen van ring 2: blad vervangt nooit een stam, een stam vervangt blad; alleen `distance` van bladeren kan per laadvolgorde verschillen tot hun eerste update). Smeulreus = vanilla spar-kroon (getrapt) op een enkele stam, geen 2 × 2. Kale stammen = de rechte `charred_dead_tree` (de gevorkte leunt over chunkgrenzen). Aslaag onder de bomen = een tweede `ash_drift` (0,35) ná de bomen en flora.
- 2026-10-09 · worldgen · 47c: slapende krater: varens/gras, azalea-struiken, vanilla `azalea_tree` (~1 per 20 chunks bodem), bessen aan de voet van de wand, glow lichen tegen de wand (vanilla `multiface_growth`, zoekt maar 1 blok: geplaatst naast de wand, 0–2 boven de voet). Hangende wortels weggelaten (G2-WB5 noemt "hangende wortels/glow lichen").
- 2026-10-09 · worldgen · 47c: lavavallen als feature: een lavabron 3 onder de top van een wandkolom zonder lava, met aan één kant een kolom met terreinlava ≥ 10 lager, alleen naar die kant open, met geplande vloeistoftick (plan §0.6, hetzelfde soort bron als de springs van 39d); kans 1/400 per kolom (hash van seed + kolom), max. 2 per chunk; geldt voor kraterwanden, kloven en afgrondwanden in één regel. De 39f-eilandvallen (vooraf geplaatste vallende lava in `RingAquifer`) blijven terrein; dit raakt W-terrein niet.
- 2026-10-09 · worldgen · 47c: eilanden: vlekken smeulende as met 1–3 aslagen erop, vuurkristallen (cluster, grote/middelgrote knop) aan de onderkant met ≥ 4 open lucht eronder, een smeulboom op vlak midden van grote eilanden (`island_mask` ≥ 0,9). Geiten: de piek-bovenlaag (basalt, smooth basalt, blackstone, as, puimsteen; magma als vlekjes) staat al in `#goats_spawnable_on`, geen wijziging. Striders (G2-WB11) niet op de pieken (ze zouden de pogingen van de geiten innemen). Startwaarden, afstemmen in 4b.
- 2026-10-09 · worldgen · 48: `steam_fens` staat niet in `Ring3Biomes.ALL` maar in een eigen `TRANSITION` + `WITH_TRANSITION` (afwijking van plan §1 48): `ALL` is in tests (`Ring3RegionSharesTest`, de `BiomeAmbienceTest` van A) de lijst van de eigen `multi_noise`; zo hoeft geen test van een andere stroom te veranderen. Kookpoelen alleen boven Y 63 (water in eindtoestand, `surface_pool`); de aankleding van `steam_fens` leest geen pure terreinkolom (in de mengzone is die maar half het terrein). Hoeveelheden startwaarden.
- 2026-10-09 · worldgen/mobs · striders ring 3 zonder jockeys (besluit gebruiker, review W-biomes): in de overworld slaat een natuurlijke of worldgen-strider zijn eigen `finalizeSpawn` over (`FinalizeSpawnEvent` geannuleerd), dus geen zombified piglin en geen baby-strider als ruiter; ook geen groepsbaby's bij het spawnen (baby's alleen door fokken). Spawn eggs, spawners en de Nether blijven vanilla. Bewuste uitzondering op "één categorie" (spawnen is `mobs`); herzien in fase 4.
- 2026-10-09 · worldgen · review W-biomes: lavavallen alleen in de ring 3-kern met echte landing in lava; voetsporen op vlakke grond, per kolom in asvlakte/titanenveld/pieken, stoppen aan de biomerand; landmark-punten per punt deterministisch (hash + salt) i.p.v. per chunk.
- 2026-10-09 · worldgen · 47b: zielvuur = vlekjes `soul_soil` (straal 1–3) met op ~1 op 4 kernblokken `soul_fire`, binnen 12 blokken van terreinbot (`volcanic_near` op `solid_kind` = bot), ≥ 2 van de chunkranden; schedels binnen 16 blokken van de ribben ("bij schedels" = schedels liggen tussen het zielvuur). Na 39f eventueel op `ribcageNear` (zie Status). Startwaarden, afstemmen in 4b.

## Backlog
- [mobs] Striders in ring 3 zonder jockeys en zonder groepsbaby's (`Ring3StriderSpawns`, besluit 2026-10-09): herzien in fase 4 bij de ring 3-monsters. (bron: review W-biomes)
- [worldgen] Backlog r. 178 (geisers in 48 met `INTERVAL_TICKS`) is achterhaald: 48 roept `GeyserBlock.startCycle` aan in `GroundVentFeature`; regel bij INT2 sluiten. (bron: 48)
- [worldgen] `steam_fens` (zone 3→4) krijgt via `is_ring_3` ook de ring 3-ertsen in de ring 4-helft; met het echte ring 4-terrein bekijken of daar een `ring_chance` bij moet. (bron: 48)
- [worldgen] `sunstone_ore`/`turquoise_ore` (Ring2Features) hebben geen `ring_chance`: in `dry_riverbeds` liggen ze ook in de ring 3-helft, waar kloofsteen doorloopt (gemeten 41: tot ~80 blokken voorbij de middellijn). Een `ring_chance` progress ring 2 erbij verschuift de ring 2-decoratie niet. (bron: 41)

## Status bij onderbreking
(2026-10-08, na de commit van 41 en de rebase op main b30963e; een verse agent neemt 47/47b/49 over in deze worktree.)
- **Klaar:** 41 gecommit op `ring3/biomes-g2` (review verwerkt, zie Voortgang). Rebase op b30963e (50-0 van A: `Ring3Biomes` gebruikt nu `BiomeAmbience.ring3(key)`, geen `PALETTES` meer). Na de rebase opnieuw `build` + `runData` draaien.
- **Kladversies 47/47b/49** in `build/draft47/` (niet gecommit, buiten de diff; nog niet gecompileerd in de mod):
  - `feature/`: `VolcanicFilter` (`volcanic_filter`, met `Builder.ground()` = plan §0.1–0.4), `VolcanicNearFilter` (`volcanic_near`), `AshDriftFeature`+config (per chunk, per kolom de biome; lee/geul/treden-ramp 6/4/2), `GroundPatchFeature` (lavatongen, modder, koraalriffen, `dry_riverbeds`-as), `GroundVentFeature` (fumarole + kraag + zwavelerts eronder; smeulventjes), `HornitoFeature`, `ImpactCraterFeature`, `AshCastFeature` (afgietsels), `AshFootprintsFeature` + `FootprintPath` (pure sporen per cel), `TitanRockFeature` (puur per cel: elke chunk schrijft zijn eigen deel, geen rangregel nodig), `Ring3DressingFeatures` (configured + placed van 47, keys `ring_3/*`), en een uitgebreide `Ring3Terrain` (met `facing`/`towards`, nu nog willekeurige rotatie).
  - `Ring3Templates.java` → hoort in `tools/worldgen/`; schrijft 25 `.nbt` (zeemonsters 47, titanen 47b, afgietsels 49) naar `src/main/resources/data/corruptedadventure/structure/ring_3/`; twee runs byte-identiek (getest in `build/tplrun`). Per pakket alleen de eigen vormen meenemen.
  - `patch_riverbeds.py`: `dry_riverbeds`-vlekken naar `ground_patch` met as + aslagen (backlog r. 122); stronken komen via een modifier `add_ring_3_riverbed_stumps` (placed `ring_3/dry_riverbeds_charred_stumps`, al in `Ring3DressingFeatures`).
  - `test/`: `Ring3DressingShapesTest`, `Ring3TemplatesTest` (pure delen, sjablonen).
  - Nog te doen bij integratie: registraties in `ModFeatures` (append-blok `// ring 3 — W-biomes g2`: `ash_drift`, `ground_patch`, `ground_vent`, `hornito`, `impact_crater`, `ash_cast`, `ash_footprints`, `titan_rock`) en `ModPlacementModifierTypes` (`volcanic_filter`, `volcanic_near`); `Ring3Features.ORDER` vullen (LOCAL_MODIFICATIONS: sulfur fumaroles, mud; basalt tongues, hornitos, vents, boulders; seabed reefs, kelp, fossils; ashen pumice, dead trees; peaks craters, rocks; dan casts; dan `ash_drifts` (alle ring 3-biomes); dan footprints en titan rocks (alle ring 3-biomes, ze beslissen zelf per cel); VEGETAL: seabed dead coral, dead bushes); noise `worldgen/noise/ring_3_ash_drift.json` (bv. firstOctave −5, amplitudes [1, 1]).
  - **Let op uit de review van 41:** lees nooit wereldblokken buiten wat de eigen chunk zelf schreef om iets te beslissen; gebruik het pure terrein (`Ring3Terrain`) of `WORLD_SURFACE_WG` (bevroren tijdens features). `GroundVentFeature`/`GroundPatchFeature`/`AshDrift` lezen alleen eigen-chunkblokken; `AshFootprintsFeature` leest de live `WORLD_SURFACE`-heightmap van de eigen chunk.
- **Gedaan 2026-10-09 (na de rebase):** de punten hieronder zijn omgezet naar de 39f-accessors (zie Voortgang 15:00). ~~**Wacht op 39f-accessors** (signaturen van main: `VolcanicField.moundAt`, `sinkholeAt`, `collapseAt`, `ribcageNear`, `nearestVolcano`/`volcanoesWithin`): fumarolen op de heuveltoppen, modder in de zinkgatbodems (nu heuristiek `wallFoot`), zielvuur in de ribbenkasten (nu `volcanic_near` op `solid_kind` 1), afgietsels naar de vulkaan (nu willekeurig), voetsporen die bij een kraterrand eindigen (nu `RIM` → niets). Pas na de rebase op de 39f-merge.~~
- **Open:** striders (G2-WB11) staan in geen pakket; logisch bij 47c (spawns in `Ring3Biomes.spawns`).

### Update 2026-10-08 17:35 (tweede agent, gestopt op verzoek van main)
- **Worktree/branch:** ongewijzigd (`ring3/biomes-g2`, HEAD a389396 = 41). Geen eigen processen actief, `enable-rcon=false`, geen lock of slot in bezit. Oude testwerelden `run/biomes41a–c` verwijderd; `run/biomes41d` bewaard (ring 1/2-vergelijking).
- **47 staat ongecommit in de werkmap (geen review gehad):**
  - nieuw in `worldgen/feature/`: `VolcanicFilter` (forward reference van `DEFAULT_TOLERANCE` hersteld), `AshDrift*` (lava-buurt nu één keer per chunk gelezen in een venster van 20 × 20 kolommen, `lavaNear`), `GroundPatch*`, `GroundVent*`, `Hornito*`, `ImpactCrater*` (extra: geen kuil boven vloeistof binnen 3 blokken, plan §0.6), `Ring3DressingFeatures` (`ash_drifts` zonder biome-filter: die testte de chunkhoek onderin de wereld; de feature beslist per kolom);
  - registraties: `ModFeatures` (append-blok `// ring 3 — W-biomes g2` + imports), `ModPlacementModifierTypes.VOLCANIC_FILTER`;
  - `Ring3Features.ORDER` gevuld (LOCAL_MODIFICATIONS: zwavel, basalt, zeekom, asvlakte, pieken, dan `ash_drifts` in ashen/titan/peaks/seabed; VEGETAL: dood koraal, dode struiken) + `local(...)`-helper; roept `Ring3DressingFeatures` aan;
  - `Ring3BiomeModifiers.ADD_RING_3_RIVERBED_STUMPS` (VEGETAL, alleen `dry_riverbeds`); `DryRiverbedsFeatures`: vlekken via `ground_patch` naar as + aslagen (`patch_riverbeds.py` toegepast);
  - noise `worldgen/noise/ring_3_ash_drift.json` (firstOctave −5, [1, 1]);
  - `tools/worldgen/Ring3Templates.java` met alleen de zeemonsterfossielen (47); 5 `.nbt` in `structure/ring_3/`, twee runs byte-identiek (`build/tplA/run1.md5`). Draaien vanuit de repo-root (relatief pad).
  - tests: `Ring3DressingShapesTest` (zonder voetsporen/rotatie, die horen bij 49), `Ring3TemplatesTest` (alleen zeefossielen, 5 bestanden).
- **Nog te doen voor 47:** B opnieuw (`build/biomes-gradle.sh <label> build`), D (`runData`), S met `build/bt/gen47.txt` (6 biomes 16 × 16 + strook `dry_riverbeds`), `/place feature` per feature, determinisme (tweede wereld omgekeerd, `build/bt/compare.py`), prestatie features-stap; dan "klaar voor review-doorsturen: 47".
- **Inzicht (geverifieerd in de MC-bron):** vanaf status CARVERS werkt een chunk alleen de FINAL-heightmaps bij; `WORLD_SURFACE_WG`/`OCEAN_FLOOR_WG` zijn tijdens features dus echt bevroren, ook in buurchunks. `SlopeFilter` met een straal groter dan de gridmarge is daarom veilig. `WORLD_SURFACE` is wel live (eigen chunk).
- **47b/49:** nog niet begonnen in de mod; drafts in `build/draft47/` (`TitanRock*`, `VolcanicNearFilter`, `AshCast*`, `AshFootprints*`, `FootprintPath`, titan-/afgietselvormen in `build/draft47/Ring3Templates.java` + `patch_titans.py`, tests in `build/draft47/test/`). Voor 47b nog: optionele bloklijst in `SurfaceFossilConfiguration` (basalt-titanen), sjablonen terugzetten in het script. Wacht op de 39f-accessors zoals hierboven beschreven.
- **Notities van main:** `GeyserBlock.INTERVAL_TICKS` verdwijnt met B (zie 48); Nether-blok van `ring3_surface.py` niet aanpassen (zie 48).
