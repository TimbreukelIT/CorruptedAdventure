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
- [ ] `/ca tpring 3 nether` (~1.180, 0): bevroren lavazee met ijspegels, ijsplaten, bevroren lavavallen langs de pilaren; warme eilandjes rond de scheuren (gloed, geen rijp).
- [ ] Rijpwoud (`/locate biome corruptedadventure:rime_forest`): rijpzwammen met ijspegels, berijpte vloer.
- [ ] Gletsjerkloven (`/locate biome corruptedadventure:glacier_rifts`): blauw-ijsaders, ijsvallen, ijsbruggen (als gekozen), scheur-adertjes op de bodem.
- [ ] Biomegrenzen volgen de terreinvormen (zee ↔ hallen ↔ gletsjer).
- [ ] Wezens volgens G2-WN4.
- [ ] `/ca tpboundary 2 nether` en r 1.500: geen lavawand, de dam is heel.
- [ ] Ruimte (54): voelt 625 breed met drie biomes goed? (cijfers in het rapport).
- [ ] Levendigheid: 2 min lopen per biome.

## Voortgang

## CODEBASE-delta

## Beslissingen

## Backlog
