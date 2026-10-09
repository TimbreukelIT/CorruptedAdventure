# Golf 2 — stroom W-nether: 53b, 54 (Nether-ring 3: biomes, aankleding, `glacier_rifts`, ruimte-check)

Scope: `worldgen` — één commit per pakket, branch `ring3/nether-g2`, poort 3 (server 25573, RCON 25583). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:** de bevroren Nether van ring 3 krijgt zijn drie biomes en een eigen wow, en wordt levendig. Les ring 2: niet kaal. De sparring vroeg om bevroren lavavallen, rijpwoud met ijspegels en gloeiende scheuren als warme eilandjes. Teststop 4b beoordeelt de kou in Nether-3 (C) en de sfeer (A); 4c doet de eindcheck.

**Bron:**
- `fase-1-wereld.md` Nether-ring 3 (terrein 53, surface 39c, blokken 51/52, sfeer, kou);
- `contract.md` §2/§4 (feature-keys `corruptedadventure:nether_ring_3/<naam>`, determinismeregel 35);
- `golf-1-nether.md` (drempels `RIME_THRESHOLD`/`RIFTS_THRESHOLD` ±0,18, dam van trede 3|4, ruimtemeting);
- backlog r. 190/192;
- sjablonen:
  - `NetherRing2Biomes`/`NetherRing2Features` (biome-opzet, vaste featurevolgorde, `GrowthPriority`, `FungalTerrainScan`);
  - `GiantFungusFeature` (stem/cap/lamp/stalactite als config);
  - `SlimeStalactiteFeature` (blok als config);
  - `MoldPatchFeature`, `DewGeodeFeature`;
  - 39b (placeholder weghalen).

## 0. Regels voor features in Nether-3 (change 35 + terrein 53)
- **Determinisme (35):**
  - features lezen alleen terrein (lucht en `#fungal_growths` zijn doorzichtig), schrijven op rang (`GrowthPriority`) en wijken nooit uit;
  - geen `count_on_every_layer`, `random_patch`, `multiface_growth`, `block_column`, `vegetation_patch`, vanilla `geode`/`huge_fungus`;
  - holtes alleen uit density functions.
- **Lava:** niveau Y 22; lava alleen onderin de warme scheuren (Y 17–22) en onder het ijs. Geen feature zet lava of vuur waar het tegen lucht boven Y 22 staat; bevroren "lavavallen" zijn vaste blokken.
- **Trede 3|4 en dam:** niets uithollen binnen 48 blokken van de ringgrenzen (de dam: vast tot Y 33). Carvers: G2-WN5. `NetherRing3JsonTest.nether3BiomesHaveNoCarversYet` faalt bewust zodra er carvers komen (backlog r. 190).
- **Eén featurevolgorde** voor alle Nether-biomes per stap (vaste volgorde zoals `NetherRing2Biomes`). **Feature-indexverschuiving:** de nieuwe features verschuiven de decoratie in Nether-1/2 (valkuil 35); INT vergelijkt Nether-1/2 op biomes en terreinvorm.
- **Oerijs:** blijft de dunne kern van de surface-regel (39c, ≥ 0,66, ~60 pilaren). Een kern in élke pilaar vraagt een feature die het pilaarmidden kent: alleen als de gebruiker dat later vraagt.

## 1. Pakketten

### 53b — Nether-3-biomes + aankleding van `frozen_lava_sea` en `rime_forest`
1. **`NetherRing3Biomes`:** alle drie de biomes worden nu geregistreerd (`frozen_lava_sea`, `rime_forest`, `glacier_rifts`), zodat `placeholder_nether_ring_3` in één keer weg kan (zoals 39b). `glacier_rifts` krijgt zijn aankleding in 54.
   - Klimaat: geen neerslag, lage temperatuur (alleen voor F3 en het gevoel; water verdampt in de Nether toch).
   - Effecten via de haak van A (`BiomeAmbience`, zie Afhankelijkheden), tot dan een voorlopig palet (blauwwit).
   - Spawns: G2-WN4.
2. **Preset:** Nether ring 3-source = `multi_noise` op `temperature` = `ring_3/nether/region` met de drempels ±0,18 (`NetherRing3Features.RIME_THRESHOLD`/`RIFTS_THRESHOLD`): midden zee, hoog rijp, laag kloven. Zelfde drempels als terrein en surface (`NetherRing3JsonTest`).
3. **Surface** (`noise_settings/nether.json`, de ring 3-tak): de placeholder-tak per biome splitsen met dezelfde blokken (backlog r. 192). Geen nieuwe materialen, tenzij de aankleding erom vraagt.
4. **Placeholder weg:** `placeholder_nether_ring_3` (key in `ModBiomes`, `PlaceholderBiomes`, de lang-providers; één regel elk). Werelden van daarvoor laden niet meer.
5. **Tags** (`NetherRing3BiomeTags`): `is_nether_ring_3`, `c:is_cold/nether`. Vraagt C een tag voor "open vlakte" (G2-C1), dan via main in het contract.
6. **Lang** (`NetherRing3BiomeNames`): en + nl (Frozen Lava Sea/Bevroren lavazee, Rime Forest/Rijpwoud, Glacier Rifts/Gletsjerkloven).
7. **Aankleding `frozen_lava_sea`:**
   - ijspegels (`ice_stalactite`, `SlimeStalactiteFeature` met dit blok) aan het plafond en onder de pilaarkoppen;
   - Nether-ijsplaten en rijpvlekken op de koudlava;
   - bevroren lavavallen langs pilaren en plafondranden (G2-WN2);
   - warme eilandjes rond de scheuren (G2-WN3).
8. **Aankleding `rime_forest`:**
   - rijpzwammen (G2-WN1) via `GiantFungusFeature` (stem `rimeshroom_stem`, cap `rimeshroom_cap`, stalactite `ice_stalactite`, lamp volgens G2-WN1);
   - rijp-netherrack-vloeren met kleine zwammen;
   - ijspegels onder de hoeden en de hallenplafonds.

### 54 — `glacier_rifts` + ruimte-check
1. **Aankleding `glacier_rifts`:**
   - blauw-ijsaders in de massa;
   - ijspegels in de kloven en tunnels;
   - ijsvallen langs de kloofwanden;
   - eventueel ijsbruggen over kloven (G2-WN2; een brug is vorm: als DF in `density_function/ring_3/nether/**`, niet als feature);
   - gloeiende scheur-adertjes op de kloofbodem bij de lava (Y 17–22) als warmtepunt.
2. **Ruimte-check** (zoals 53): aandeel per biome, hal- en kloofmaten, vlekgrootte; vergelijken met 53 (zee 41,5 / rijp 35,8 / kloven 22,7 %).
   - Let op: Nether-3 is de overworld-ring ÷ 4. Verbreden kan alleen via de overworld-ringradii (config, alleen nieuwe werelden) en raakt dus ook ring 3/4 in de overworld.
   - Voelt het krap: cijfers + opties aan main, niet zelf aanpassen.

## 2. Bestanden
- **Eigen:**
  - `NetherRing3Biomes`, `NetherRing3BiomeModifiers`, `NetherRing3Features` (+ sub-bootstraps), `NetherRing3Carvers`, `NetherRing3BiomeTags`, `NetherRing3BiomeNames`;
  - preset: de Nether ring 3-source;
  - `noise_settings/nether.json` (de ring 3-tak);
  - `density_function/ring_3/nether/**` + `noise/nether_ring_3_*` (alleen als 54 vorm toevoegt);
  - `PlaceholderBiomes`/`ModBiomes` (alleen de placeholder-regel).
- **Append-blokken:** `ModFeatures`, `ModPlacementModifierTypes` (`// ring 3 — W-nether`).
- **Herbruik zonder wijziging:** `GiantFungusFeature`, `SlimeStalactiteFeature`, `MoldPatchFeature`. Moet zo'n Nether-2-feature generieker worden (bijv. een optionele lamp of een andere grond): dat is gedrag-neutraal voor Nether-2 en wordt in "Beslissingen" vastgelegd. Nether-2-decoratie mag niet veranderen (determinismetest Nether-2).
- **Niet aanraken:** `BiomeAmbience`, `ModSoundEvents`, `ModParticleTypes` (A); `ModBlocks`/tags-inhoud (B).

## 3. Afhankelijkheden
- **← B:** alle Nether-3-blokken bestaan (51/52). Nieuwe blokken alleen via main.
- **← A:**
  - biome-effecten (fog/lucht, `ice_glint`-particles, wind/kraak-ambient) zijn van A (56);
  - voorstel: haak `BiomeAmbience.netherRing3(key)` in voorpakket 50-0 (zie `golf-2-atmosphere.md`). Zonder haak: voorlopig palet in `NetherRing3Biomes`, de integratie zet de ene regel om.
- **→ C (55):** de kou leest de ringzone (`ringAt`, Nether ring 3) en eventueel een biome-tag voor "open vlakte" (G2-C1). Die tag vult W-nether, de naam komt via main in het contract.
- **Overworld-stromen:** geen. W-nether kan direct starten (geen 39f-afhankelijkheid).

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-WN1 — Rijpzwammen in `rime_forest`: hoe groot en hoe "levend"?**
- **A. Bevroren reuzenzwammen 12–22 hoog (reuzenzwam-feature van Nether-2 met rijpzwamblokken), ijspegels onder de hoed, geen lamp (koud en donker), plus kleine rijpzwammen op de vloer (aanbevolen).** C laag (bestaande feature, config). S: herkenbaar "bevroren woud". P zoals Nether-2.
- **B. Als A, maar met een zwak gloeiende lamp onder de hoed (gloeiende scheur als lamp, licht 7).** C laag. S: sfeervoller en makkelijker te navigeren; minder koud gevoel, en elke lamp is een warmtebron (`warmth_sources`).
- **C. Enorme zwammen (25–35) als landmarks, minder vaak.** C middel. S: wow; de hallen (30–100 breed) voelen kleiner.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WN2 — Bevroren water/lava-wow: wat bouwen we?**
- **A. Bevroren lavavallen (koudlava met een huid van Nether-ijs, als gestolde stromen langs pilaren en plafondranden), plus ijsbruggen over de gletsjerkloven (aanbevolen).** C middel (vallen = feature, bruggen = DF in 54). S: het verhaal "de lavazee bevroor midden in de val" in één beeld; bruggen maken de kloven begaanbaar. P: bruggen als DF klein.
- **B. Alleen bevroren lavavallen.** C laag/middel. S: wow bij de pilaren; de kloven blijven kloven.
- **C. Alleen ijsvallen van blauw/verpakt ijs.** C laag. S: mooi maar generiek ijs; minder "lava".
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WN3 — Warme scheuren als "warme eilandjes": hoe ver gaan we?**
- **A. Rond elke scheurvlek een kring van 4–6 blokken zonder rijp (gewoon netherrack, magma, gloeiende scheur in de vloer); daar zakt de kou (warmtebron) (aanbevolen).** C laag (surface + feature). S: duidelijke rustpunten in de vlakte, van ver zichtbaar door de gloed. P laag.
- **B. A + losse gloeiende adertjes verspreid door de vlakte (meer warmtepunten).** C laag. S: makkelijker overleven; minder schaars.
- **C. Alleen de scheuren zelf (huidig).** C geen. S: weinig warmte; Nether-3 wordt een race.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WN4 — Wezens in Nether-3 tot fase 4?** (backlog fase 4: bevroren ghasts, ijs-strays)
- **A. Vanilla strays (pijlen met traagheid, passen bij de kou) + ghasts en skeletten; geen piglins of hoglins (aanbevolen).** C laag (spawns in biome-data). MP geen. S: koud en vijandig; strays kondigen fase 4 aan.
- **B. Het standaard-Nether-spawnpakket (zoals Nether-wastes).** C laag. S: weinig eigen; piglins op ijs.
- **C. Leeg tot fase 4 (alleen ghasts).** C geen. S: stil, maar minder levendig (botst met de levendig-eis).
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-WN5 — Carvers (grotten) in Nether-3?**
- **A. Geen carvers: het terrein heeft al hallen, kloven en tunnels; de dam van trede 3|4 blijft gegarandeerd heel (aanbevolen).** C geen. S: weinig verschil. P iets goedkoper.
- **B. `ring_nether_cave` met een grensmarge van 48 blokken (niet door de dam).** C middel (eigen carverconfig + test). S: meer grotten onder de vlakte.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
Alleen worldgen-data en features (server). Geen netwerk en geen client-code. Biome-effecten zijn data.

## 6. Tests
- **B:** `./gradlew build` groen; `NetherRing3JsonTest`/`Ring3JsonTest` bijgewerkt (biomes in de preset op de drempels, surface per biome, carvers volgens G2-WN5). Nieuwe tests: `NetherRing3BiomesTest` (geen neerslag, palet, featurevolgorde, spawns).
- **D:** `runData` groen; output gecommit.
- **S:** server (25573); `/ca tpring 3 nether`, `/locate biome` per biome, `/place feature corruptedadventure:nether_ring_3/<naam>`.
  - `python -I tools/nether-lava-walls.py <rcon-wachtwoord> 875` en `… 1500` → **0 wanden**.
  - Geen lava boven Y 22 en geen lava naast lucht in 5 gebieden.
  - Portal overworld ring 3 (4.755, 89, 16) ↔ Nether-3 (1.197, 36, 9).
- **Determinisme:** twee verse werelden, de tweede omgekeerd geladen (eerst de Nether). Nether-3 576/576 identiek; Nether-1/2 op de bekende vanilla-ertsruis na.
- **Feature-index:** Nether-1/2 tegen main op biomes en terreinvorm.
- **Prestatie:** 53b/54 raken de noise-stap alleen als 54 een DF toevoegt (ijsbruggen).
  - Dan noise meten (budget "gelijk aan Nether-2", besluit 53: Nether-2 ~1,45×; de Nether-3-mediaan mag daar niet boven).
  - Altijd: de features-stap + wandtijd van Nether-3 tegen vanilla-Nether (16 × 16, warm-up, ≥ 3 rondes). Melden boven 2× vanilla.
- **Review:** "klaar voor review-doorsturen: <pakket>" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (in-game, verse wereld seed `20260930`)
- [x] `/ca tpring 3 nether` (~1.180, 0): bevroren lavazee met ijspegels, ijsplaten, bevroren lavavallen langs de pilaren; warme eilandjes rond de scheuren (gloed, geen rijp). (53b, 2026-10-09; mist zeer dicht, zie voortgang)
- [x] Rijpwoud (`/locate biome corruptedadventure:rime_forest`): rijpzwammen met ijspegels, berijpte vloer. (53b)
- [ ] Gletsjerkloven (`/locate biome corruptedadventure:glacier_rifts`): blauw-ijsaders, ijsvallen, ijsbruggen (als gekozen), scheur-adertjes op de bodem. (54)
- [x] Biomegrenzen volgen de terreinvormen (zee ↔ hallen ↔ gletsjer). (zelfde drempels ±0,18 als terrein en surface)
- [x] Wezens volgens G2-WN4. (47 strays, 22 skeletten, 1 ghast; geen piglins/hoglins)
- [x] `/ca tpboundary 2 nether` en r 1.500: geen lavawand, de dam is heel. (`nether-lava-walls.py`: 0 en 0)
- [ ] Ruimte (54): voelt 625 breed met drie biomes goed? (cijfers in het rapport).
- [ ] Levendigheid: 2 min lopen per biome.

## Voortgang
Voortgang: 2026-10-08 16:40 worktree `.claude/worktrees/agent-a78930bee6d548506`, branch `ring3/nether-g2` vanaf main `f81d76a` (worktree stond op 17c9eb4, teruggezet); poort 25573, RCON 25583
Voortgang: 16:58 53b code + JSON geschreven (biomes, features, preset, surface per biome, vallen/gordijnen als DF, tests); wacht op een JVM-slot (39f meet prestatie met lock + 3 slots)
Voortgang: 2026-10-09 11:47 hervat door een nieuwe agent; werk intact
Voortgang: 11:53 S op verse wereld `n3g2-b`: alle drie de biomes via `/locate`; `nether-lava-walls.py` r 875 → lava 11.948, **0 wanden**; r 1500 → **0 wanden**; 5 gebieden (12×12 chunks): geen lava boven Y 22, geen lava met lucht ernaast, geen stromende lava; alleen open lavaspiegels op Y 22 onderin de warme scheuren (107 in de kloven, 31 in het rijpwoud); portal overworld ring 3 (4755, 90, 16) → Nether (1185, 58, 18, `rime_forest`) en terug werkt (varken)
Voortgang: 11:58 determinisme: `n3g2-det-a` (overworld eerst) tegen `n3g2-det-b` (Nether eerst, omgekeerd): Nether-3 576/576 + strook 156/156 identiek; overworld ring 3 144/144; Nether-1/2 en overworld ring 1 alleen vanilla-featureruis (ertsen, magma, warped fungus, dripstone)
Voortgang: 13:00 prestatie (lock + 3 slots, één server, mod-Nether tegen `perftest:vanilla_nether`, 16×16 chunks per gebied, warm-up over alle Nether-ringen, 3 rondes op verse werelden, volgorde gewisseld). Noise-mediaan mod/vanilla per ronde: Nether-3 1,27–1,50× (rijp 1,45–1,50, zee 1,27–1,41, kloven 1,28–1,45), Nether-2 1,50–1,58×, Nether-1 1,27–1,33× → **onder budget 1,75× en niet boven Nether-2**. Features-stap Nether-3 0,18–0,57× vanilla; surface 0,90–1,42×; wandtijd per gebied 0,90–1,05× vanilla. JFR-bestanden en meetwerelden opgeruimd
Voortgang: 13:08 in-game (eigen client, venster via de eigen client-PID) op `n3g2-b`: 18 screenshots in `build/n3w/shot_*.png` (zee met pilaren en geribde bevroren lavavallen, plafond met ijspegels en gordijnen, rijpwoud met rijpzwammen, kloven, warme scheur met gloeiende-scheurwanden en lavabodem, portal). Wezens na 75 s bij (−1180, 60, 0): 47 strays, 22 skeletten, 1 ghast, geen piglins/hoglins (G2-WN4). Opgevallen: de mist in zee en kloven is zeer dicht (zicht ~40 blokken; sfeer = A/56, niet aangepast). Processen gestopt, `enable-rcon=false`, lock en slots vrij
Voortgang: 13:09 beide surface-scripts (`nether_ring3_surface.py`, `ring3_surface.py`) gedraaid: JSON byte-identiek. **53b klaar tot vóór de rebase**; wacht op het rebase-signaal van main (39f eerst naar main)
Voortgang: 13:21 reviewfixes 53b (main, `/code-review medium`): ruimte per `FreeSize` (`min_gap`), validatie in de compacte constructor (codec → decodeerfout), dunne stam onder kleine hoeden, val-verf alleen op wanden en buiten de warme kring, gordijn-verf alleen in de kern van de band en alleen het gordijn, dode placeholder-kleur uit `MapPalette`; tests erbij (`GiantFungusShapeTest`: ruimte, ijspegels vrij van de stam, validatie; `NetherRing3JsonTest`: gordijn-verf binnen de band). B groen, D groen (alleen `small_rimeshroom`/`rimeshroom` krijgen `min_gap`; Nether-2 ongewijzigd), beide surface-scripts byte-identiek. Indicatief (regiobestanden, wereld `n3g2-c`): geen lava boven Y 22/naast lucht, val- en gordijnstrepen op vlakke plafonds en vloeren weg, meer zichtbare ijspegels
Voortgang: 13:26 nieuwe testafspraak (gebruiker via main): per pakket alleen B + D; in-game check van de reviewfixes gaat naar **INT2**: rijpzwammen (grote met ruimte 29, kleine met dunne stam en ijspegels), bevroren lavavallen en -gordijnen (alleen op wanden/gordijn, geen strepen op vloeren en plafonds), warme scheuren (geen ijs/vallen in de warme kring), plafonds van zee en rijpwoud. Lock en slots vrij; wacht op het rebase-signaal
Voortgang: 13:35 main: 53b nu committen (review gedaan, fixes verwerkt), rebase later op de gecommitte branch; daarna door met 54 (alleen B + D)

## CODEBASE-delta
- **Klassen:**
  - `NetherRing3Biomes` (53b): `ALL` (`frozen_lava_sea`, `rime_forest`, `glacier_rifts`), effecten uit `BiomeAmbience.netherRing3` + muziek, geen neerslag, temperatuur −0,5; spawns strays 40 (2–4), skeletten 20 (2–4), ghasts 30 (4); geen carvers; vegetatie per biome uit `NetherRing3Features.VEGETATION` (één volgorde).
  - `NetherRing3Features` (53b): eigen keys `nether_ring_3/<naam>` (niet in `ModConfiguredFeatures`/`ModPlacedFeatures`): configured `rimeshroom` (vrijstaande reuzenzwam 12–22, hoed straal 4–6, lamp = hoed, ruimte 29), `small_rimeshroom` (3–6, hoed 2–3, dunne stam, ruimte 10), `ice_stalactite` (1–6); placed `frozen_lava_sea_stalactites` (8/chunk, Y 64–106), `rime_forest_rimeshrooms` (3, Y 30–90), `rime_forest_small_rimeshrooms` (4, Y 28–70), `rime_forest_stalactites` (5, Y 50–106); `RIMESHROOM_SIZE`/`SMALL_RIMESHROOM_SIZE`.
  - `GiantFungusShape.FreeSize` (+ `GiantFungusConfiguration.freeSize`, JSON `free_size` met `min_gap`, weggelaten als standaard): hoogte- en hoedbereik van de vrijstaande vorm en de benodigde ruimte vloer–plafond; `FreeSize.of` leidt de ruimte af (max. hoogte + max. hoedstraal + 1, geen hoed in het plafond); `DEFAULT` (8–20, 3–6, ruimte `MIN_FREE_GAP` 18) trekt precies als voorheen (Nether-2 ongewijzigd, test). De compacte constructor valideert (min ≤ max, hoedstraal 2–`MAX_CAP_RADIUS`, ruimte ≥ kleinste zwam); de codec maakt daar decodeerfouten van. Een 3 brede stam alleen onder hoeden met straal ≥ 3 (`thickStem`), zodat kleine hoeden een dunne stam houden en de ijspegels vrij hangen.
  - `GrowthPriority`: rijpzwam-stam/-hyphae (ook gestript) = STEM, `rimeshroom_cap` = CAP, `ice_stalactite` = STALACTITE (de sets raken elkaar niet; Nether-2 ongewijzigd).
  - `BiomeSpawnPlacements`: stray-regel OR "in `#is_nether_ring_3` + gewone monsterregel" (vanilla eist zicht op de lucht).
  - `NetherRing3BiomeTags`: `is_nether_ring_3`, `c:is_nether`, `c:is_cold/nether`, `cold_exposed` (= `frozen_lava_sea`; lokale `TagKey`, INT voegt samen met C's `ModTags.Biomes.COLD_EXPOSED`). `NetherRing3BiomeNames` (en/nl).
  - Weg: `ModBiomes.PLACEHOLDER_NETHER_RING_3`, de registratie in `PlaceholderBiomes` en de twee lang-regels.
- **Databestanden:**
  - preset: Nether `ring_3` = `multi_noise` op `temperature` (zee [−0,18, 0,18], rijp [0,18, 2], kloven [−2, −0,18]).
  - `density_function/ring_3/nether/falls` (`flat_cache`: 0,10 × ramp(`nether_ring_3_falls` vanaf 0,25)) in `pillars` (× gradiënt Y 50→100 van 1 naar 0,6): bevroren lavavallen als ribben langs de pilaren; `drapes` (`flat_cache`: 12 × band(|`nether_ring_3_drapes`| < 0,12) × vlekken(`nether_ring_3_drape_patches` ≥ 0,15) × zee × core) gaat af van `ceiling_height`: bevroren lavagordijnen aan het zeeplafond.
  - noises `nether_ring_3_falls` (−4), `_drapes` (−5), `_drape_patches` (−7), `_frost` (−5, surface).
  - `noise_settings/nether.json`: Nether-3-tak op `biome_is` [de drie biomes]; komt uit **`tools/worldgen/nether_ring3_surface.py`** (eigen script van W-nether; het Nether-deel is uit `ring3_surface.py` gehaald). Vormregels lezen de ruis (vallen, pilaren, gletsjer, scheuren, warme kring, gordijnen), aankleding leest de biome (vloeren, vulling, plafonds). De val-verf alleen op wanden (niet op de vloer- en plafondlaag, `stone_depth`) en nooit in een warme kring; de gordijn-verf alleen in de kern van de band (|`drapes`| < 0,09, vlekken ≥ 0,25: gordijn ≥ ~2 blokken) en alleen het gordijn zelf (≤ 12 boven de lucht eronder).
- **Plekken die Nether-3-biomes opsommen:** `NetherRing3Biomes.ALL`/`VEGETATION`, preset `rings.ring_3`, `nether_ring3_surface.py` (`BIOMES` en de `biome(...)`-regels), `NetherRing3BiomeNames`, `NetherRing3BiomeTags` (`cold_exposed`), `MapPalette` (kleuren bestonden al; de dode placeholder-kleur is weg).

## Beslissingen
- (implementatie) 2026-10-08 · worldgen · Bevroren lavavallen (en in 54 de ijsvallen en ijsbruggen) zijn terreinvorm (2D-ruis in de density functions) + surface-verf, geen feature: een feature die terreinblokken (koudlava, Nether-ijs) zet, maakt de terreinscans van de zwam- en ijspegelfeatures in buurchunks afhankelijk van de laadvolgorde (les review W-biomes 41). Daardoor raakt 53b ook de noise-stap.
- (implementatie) 2026-10-08 · worldgen · Warme kring rond de scheuren (G2-WN3) in de surface: |`nether_ring_3_cracks`| ≤ 0,15 waar `crack_patches` ≥ 0,36 → kale netherrack met magma en gloeiende scheur in de vloer, geen ijs.
- (implementatie) 2026-10-08 · worldgen · "Rijpvlekken" op de koudlava = vlekken berijpt netherrack (ruis `nether_ring_3_frost` ≥ 0,4) waar de korst brak; geen nieuw materiaal.
- (implementatie) 2026-10-08 · worldgen · Kleine rijpzwammen = de reuzenzwam-feature op klein formaat (3–6 hoog, hoed 2–3); er is geen rijpzwam-plantblok.
- (implementatie) 2026-10-08 · worldgen · Surface per biome gesplitst voor de aankleding (vloeren, vulling, plafonds); de vormregels (pilaren, vallen, gletsjer, scheuren, gordijnen) blijven op de ruis, want de biome is per 4 blokken gekwantiseerd en de vorm volgt de ruis blok voor blok.
- (implementatie) 2026-10-08 · worldgen · Strays in Nether-3 via een OR-spawnregel in `BiomeSpawnPlacements` (vanilla eist hemellicht, dat de Nether nooit heeft). Bewust gehouden (2026-10-09): zonder de regel staan strays wel in de spawnlijst (G2-WN4) maar spawnen ze nooit; de regel geldt alleen in `#is_nether_ring_3` en laat vanilla's regel elders ongemoeid (OR). Het bestand is worldgen (`worldgen/biome`) zonder eigenaar in waves §5; raakt `mobs`, daarom in de backlog.
- (implementatie) 2026-10-09 · worldgen · Bewuste lava-uitzondering Nether-3: open lavaspiegels op Y 22 onderin de warme scheuren (lucht erboven, nooit ernaast, geen stromende lava). Het is de warmtebron van G2-WN3 en de bodem van de scheuren uit 53; lavawanden en lava boven Y 22 blijven 0.
- (implementatie) 2026-10-08 · worldgen · Oerijs blijft de dunne kern in de sterkste pilaren (geen kern in elke pilaar; plan §0).
- (implementatie, review 53b) 2026-10-09 · worldgen · Vrijstaande reuzenzwammen vragen ruimte per maat (`FreeSize.minGap`): rijpzwam 29 (22 + 6 + 1), kleine rijpzwam 10; Nether-2 houdt 18 (ongewijzigd). Kleine hoeden (straal < 3) krijgen altijd de dunne stam, zodat de ijspegels (op straal − 1) niet in de stam vallen; de randomvolgorde blijft gelijk.
- (implementatie, review 53b) 2026-10-09 · worldgen · Surface Nether-3: de bevroren-lavaval-verf alleen op wanden en buiten de warme kring (G2-WN3), de gordijn-verf alleen waar het gordijn ≥ ~2 blokken lang is en alleen het gordijn zelf; geen strepen meer op vlakke vloeren en plafonds.
- 2026-10-09 · worldgen · De stray-spawnregel (`BiomeSpawnPlacements`) maakt de spawnlijst van de Nether-3-biomes waar (G2-WN4); hij wordt in fase 4 herzien (backlog `mobs`).

## Backlog
- `mobs` · Stray-spawnregel voor Nether-3 (`BiomeSpawnPlacements.checkNetherRing3Stray`, 53b) herzien in fase 4, als de ijs-strays/bevroren ghasts komen (eigen mobs met eigen spawnregels).
- `worldgen` · INT: lokale `cold_exposed`-TagKey in `NetherRing3BiomeTags` samenvoegen met C's `ModTags.Biomes.COLD_EXPOSED`.

## Status bij onderbreking (2026-10-09 13:26)
- **Worktree/branch:** `.claude/worktrees/agent-a78930bee6d548506`, branch `ring3/nether-g2` op main `f81d76a`. **Niets gecommit** (geen review gedaan). Geen eigen processen meer actief; `run/server.properties` `enable-rcon=false`; lock en slots vrij.
- **53b, ongecommit op schijf (af, nog niet in-game bekeken):**
  - Java: `NetherRing3Biomes`, `NetherRing3Features`, `NetherRing3BiomeTags` (`cold_exposed` als lokale TagKey), `NetherRing3BiomeNames`, `GiantFungusShape.FreeSize` + `GiantFungusConfiguration.freeSize`, `GrowthPriority` (rijpzwam/ijspegel), `BiomeSpawnPlacements` (stray OR-regel), placeholder weg (`ModBiomes`, `PlaceholderBiomes`, lang-providers).
  - JSON: preset Nether `ring_3` = `multi_noise`; `density_function/ring_3/nether/{falls,drapes}.json` (nieuw), `pillars.json`, `ceiling_height.json`; noises `nether_ring_3_{falls,drapes,drape_patches,frost}`; `noise_settings/nether.json` (Nether-3-tak per biome).
  - Scripts: nieuw `tools/worldgen/nether_ring3_surface.py` (bron van de Nether-3-tak); het Nether-deel uit `tools/worldgen/ring3_surface.py` gehaald (alleen dat blok). DF-generator `build/n3w/dfgen.py 53b|54` (niet gecommit, in `build/`).
  - Tests: nieuw `NetherRing3BiomesTest`; `NetherRing3JsonTest` (tak op de drie biomes, drempels vallen/gordijnen), `GiantFungusShapeTest` (FreeSize), `GrowthPriorityTest` (rijp-rangen).
  - `runData` gedraaid: output in `src/generated` (ongecommit).
- **Getest:** B groen (`./gradlew build`, alle tests incl. de nieuwe), D groen, server start zonder fouten. 2026-10-09 (nieuwe agent): S op `n3g2-b` (lavawanden r 875/1500 = 0, lava alleen als open spiegel Y 22 in de warme scheuren, portal heen en terug), determinisme (Nether-3 576/576 + 156/156, overworld ring 3 144/144), prestatie (noise Nether-3 1,27–1,50× vanilla, Nether-2 1,50–1,58×; features 0,18–0,57×), in-game screenshots `build/n3w/shot_*.png`, wezens. **Nog niet:** feature-index Nether-1/2 tegen main (doet INT, plan §0; de 53b-diff in `nether.json`/preset raakt alleen de ring 3-tak), warme kring niet verder gekalibreerd (W = 0,15 oogt goed op de screenshots).
- **Reviewfixes 53b:** gedaan en B/D groen (zie voortgang 13:21); in-game punten staan voor INT2 (voortgang 13:26).
- **Volgende stap:** wachten op het rebase-signaal van main (39f eerst naar main) → rebase op main, bij een conflict in `ring3_surface.py` hun versie houden en alleen het Nether-blok weghalen → beide surface-scripts + `runData` opnieuw → B/D opnieuw → commit (tweede review alleen bij meer dan triviale conflicten).
- **54:** ontwerp staat in `build/n3w/dfgen.py 54` (ijsbruggen per blok buiten de interpolatie via `bridge_mask`, ijsvallen in de kloofsnede, `terrain` = max(`cracked`, `bridges`)); nog niet toegepast. Nog te doen: surface (ijsvallen-verf, gloeiende adertjes op de kloofbodem), blauw-ijsaders (`ORE` packed_nether_ice → blue_ice), ijspegels in de kloven, ruimte-check.
- **Afspraak scripts (main, 2026-10-08):** het Nether-deel staat voortaan alleen in `nether_ring3_surface.py`. 39f (en W-biomes) herschrijven `ring3_surface.py`; 39f merget eerst naar main. **Vóór de commit van 53b rebasen op die merge** (main meldt het), dan beide scripts opnieuw draaien en `runData` opnieuw; gegenereerde output nooit met de hand oplossen. Bij een conflict in `ring3_surface.py` hun versie houden en daarin alleen het Nether-blok weghalen (functie `nether()`, de Nether-constanten en het nether.json-blok in `main()`).
- **Gemeld aan main:** lavavallen/ijsvallen/bruggen als DF (determinisme), de stray-spawnregel, de verhuizing van het script.
