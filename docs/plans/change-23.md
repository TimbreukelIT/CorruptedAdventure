# Change 23 — Ring 2-terrein + kernbiomes `canyonlands` en `oasis_flats` + palm tree feature

Scope: `worldgen` — change 23

**Doel:** ring 2 (overworld, 1.500 – 3.500) krijgt zijn echte terrein en zijn eerste twee biomes. Het placeholder-terrein (Y ~95) en `placeholder_ring_2` verdwijnen. Er komt een gedeelde opzet voor de biomeverdeling en het terrein van ring 2 (twee regio-assen, één hoogtefunctie, één surface-rule-blok voor de kloofsteenlagen) waar de changes 25, 26, 30, 31 en 32 op voortbouwen. De palmzaailing uit change 22 kan daarna groeien: de configured feature `corruptedadventure:palm_tree` bestaat dan. Daarnaast komt er (vraag 3) de **ring gate** uit de backlog, omdat dit de eerste zware ring 2-invoer is.

Bron: `fase-1-wereld.md` §1.2 (terrein-architectuur), §1.3, §1.10, §1.15, §2 Ring 2 (terrein, biometabel, palm-set), bouwplan rij 23; backlog `[worldgen]` ring gate (change 9).

Buiten scope: Nether-ring 2 (change 35), ertsen (25), grotten en reservoirs (26), extra biomes (30), overgangsbiome 2→3 (31), lucht/fog/ambient (32), structures en villagers (fase 3), ring 2-mobs (fase 4).

---

## Afstemming (2026-10-01)

Afgestemd met 21, 22, 24–27, 30–32 en 35 (basis: de aanbevolen opties). Deze change maakt de gedeelde ring 2-bouwstenen aan; latere changes breiden uit.
- **`Ring2Biomes.ALL`** wordt hier aangemaakt: de enige Java-lijst van alle biomes met ring 2-worldgen (nu `canyonlands`, `oasis_flats`; change 30 +3, change 31 + `dry_riverbeds`, ook al staat die bootstrap in `TransitionBiomes`). Tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld` en `Ring2BiomesTest` lezen hem.
- **Controlelijst in `CODEBASE.md`** "Plekken die ring 2-biomes opsommen" (handgeschreven JSON kan niet uit Java komen): preset-`multi_noise`, surface-takken per biome, `biome_is` van het lagenblok, later de zandgrot-tak (26). 26/30/31 werken hem bij.
- **`is_ring_2`** wordt hier aangemaakt; change 25 hergebruikt hem (geen tweede tag).
- **Grotten:** vraag 4 is de enige plek voor de basisgrotten van ring 2. Bij A vervallen stap 1 en vraag 1 van change 26. De bestandsindeling (`ring_2/overworld.json` met grotten, `ring_2/overworld/terrain.json` zonder) is het contract voor 26/30/31. `height.json` wordt daarna uitgebreid door 30 (regio's) en 31 (beddingen).
- **Volgorde top-level surface rules:** bedrock → `above_preliminary_surface` → zandgrot-tak (26) → kloofsteen-lagenblok (deze change) → deepslate-gradient.
- **`Ring2Features`** wordt hier aangemaakt; 25 (ertsen) en 30 (vegetatie, kristallen) breiden hem uit. **`Ring2Biomes.generation(...)`** zo opzetten dat 25 het ertsenslot en 26 de lavameren op één plek aanpast.
- **Oase-aandeel ~45 %** is tijdelijk; change 30 brengt het naar ~12 % (vraag 1 A daar).
- **Ring 1 moet blok voor blok gelijk blijven** (router-`temperature`/`vegetation` verplaatst, ring gate): ring 1 is in-game goedgekeurd; de S-vergelijking A↔B is daarom verplicht.
- **Kloofsteen-placeholders** (21) hebben geen harde laaglijn per blok, zodat de terreincheck representatief is.
- **Teststop (besloten 2026-10-01, teststop 2 van 3):** de **blokkerende terreincheck ring 2 (~15 min)** ná deze change. Hij mag plaatsvinden terwijl change 24 (`blocks`, onafhankelijk) gebouwd wordt, maar moet akkoord zijn **vóór change 25**. De check beoordeelt de diepe hoogtes van vraag 5 B (plateau Y 125–140, kloven 55–70 diep, vlaktes Y 66–74).
  - **Bijgewerkt 2026-10-01 (na de eerste ronde):** de eerste ronde van teststop 2 is gedaan; de feedback staat in `docs/plans/teststop-2-feedback.md` en is uitgewerkt in de changes **23b** (terreinvormen) en **23c** (aankleding). Er volgt **geen akkoord vóór change 25** meer: de rest van de terreincheck (incl. de afstemvragen hieronder die nog open waren) gaat naar **teststop 2b, één blokkerende stop na change 30, vóór 31** (gebundeld 23 + 23b + 23c + 25 + 26 + 30; keuze gebruiker, 23b-17). De gebundelde checklist staat in plan 30.
- **Hoogtes (vraag 5 = B, keuze gebruiker):** plateau Y 125–140, kloofbodem Y ~70, vlaktes Y 66–74. Afhankelijke waarden zijn bijgewerkt in 24 (aannames), 25 (zonnesteen Y 95–150, vol 110–140; turkoois Y 10–40 blijft), 26 (opmerking kolomhoogte zandzones), 30 (hoodoovloer Y 85–91, pilaren tot ~plateauhoogte, kaprotsgrens Y 94; duinen en zoutvlak ongewijzigd want relatief aan de vlakte) en 31 (gemengd terrein in de zone).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 17, commit `7714b77`). Changes 18–22 zijn nog niet gebouwd: hun plannen zijn de bron. **Bij de start opnieuw nalopen** (namen in `CODEBASE.md`, antwoorden in `docs/plans/change-21.md` en `change-22.md`).

- **Terrein-infrastructuur (changes 6, 9, 14):**
  - `RingBlend` (`corruptedadventure:ring_blend`, `dimension` + `rings`, alle 6 verplicht) rekent alleen de 1–2 nodige inputs, maar `mapAll` geeft de visitor door aan alle inputs: `NoiseChunk` vult elke `interpolated`-marker van elke ring in elke chunk (valkuil "Density functions" in `CODEBASE.md`).
  - `ring_blend/overworld.json` → `ring_2/overworld.json`, nu placeholder: `interpolated(y_clamped_gradient(31→159) + 0,125 · noise placeholder_terrain)`. `ring_blend/overworld_initial.json` ring 2 = `0.390625 + ring_2/overworld`.
  - `ring_1/overworld.json` = vanilla `final_density` (inline) met `sloped_cheese` → `ring_1/overworld/sloped_cheese`; grotten blijven `minecraft:overworld/caves/*`. Dit is het sjabloon voor de grottenstapel van ring 2 (vraag 4).
  - Noise settings `corruptedadventure:overworld`: `size_horizontal` 1, `size_vertical` 2 (cellen 4 × 8 × 4), zeeniveau 63, aquifers en ore veins aan. Router: `temperature`/`vegetation` inline vanilla `shifted_noise` (`xz_scale` 0,25), `continents`/`depth` → ring 1-keten, `final_density` → `ring_blend/overworld`, `initial_density_without_jaggedness` → `ring_blend/overworld_initial`.
  - Surface rules: top-level `sequence` = [bedrock, `above_preliminary_surface` → (eerst de tak `withered_expanse`, dan vanilla), deepslate-gradient Y 0–8, …]. Afspraak (`CODEBASE.md`): ring 2-takken als losse blokken bovenaan `above_preliminary_surface`.
  - Valkuilen die hier gelden: vanilla's preliminary surface ligt waar `initial_density_without_jaggedness` > 0,390625; vanilla vult alle open ruimte onder zeeniveau met water; `RingLayout.current()` nooit in codec/constructor; geen veranderlijke staat in density functions (cache via `RingColumnCache`); feature-volgorde per stap gelijk aan vanilla (anders "Feature order cycle"); de generatievolgorde-valkuil (change 16).
- **Biome-infrastructuur:** `RingBiomeSource` geeft de sampler door aan de geneste source per ring; `transitions` (sleutel = binnenring) heeft nu alleen `ring_1` → `withered_expanse`; grens 2→3 wisselt hard op 0,5 tot change 31. `TransitionBiomes.globalOverworldGeneration` (private kopie van vanilla) en `BiomeAmbience.Palette` (`RING_1`, `desaturated`) bestaan. `PlaceholderBiomes` registreert `placeholder_ring_2` (lucht `9FC4E8`, fog `E8D7A8`, water `32A598`, waterfog `0B3A35`, gras `BFB755`, blad `AEA42A`); verder alleen genoemd in `ModBiomes`, de preset en de twee lang-providers.
- **Feature-infrastructuur:** `ModConfiguredFeatures`/`ModPlacedFeatures` (alleen keys), bootstraps per thema (`WitheredExpanseFeatures`, …), `DataGenerators` (features vóór de biomes). `RingChanceFilter` (`ring_chance`, signalen `transition`/`progress`) is beschikbaar.
- **Blokken uit change 21** (plan; namen gelden in alle antwoordopties): `red_canyon_stone`, `orange_canyon_stone`, `cream_canyon_stone`, `canyon_cobblestone`, `cracked_clay`, `dry_shrub`, `barrel_cactus`, `desert_flower`, `scorched_sandstone`; tags `corruptedadventure:canyon_stones` (de drie tinten, in `#overworld_carver_replaceables`) en `corruptedadventure:desert_plant_soil`. Aanname: kloofsteen zit **niet** in `#base_stone_overworld` (change 21 vraag 3 A) — anders zetten vanilla-ondergrondfeatures grijze klodders in de lagen; dan bij de start melden.
- **Palm-set uit change 22** (plan): `palm_log`, `palm_wood`, `palm_leaves`, `palm_sapling`; `ModTreeGrowers.PALM` verwijst naar **`ModConfiguredFeatures.PALM_TREE` = `corruptedadventure:palm_tree`** (alleen de key bestaat). Deze change registreert de feature **onder die key** (geen nieuwe key). Overdracht uit change 22: bladeren vervallen verder dan 6 blokken van een `#logs`-blok; vanilla's `TrunkPlacer` vervangt niet-`#dirt` onder de stam door de `dirt_provider` (op zand dus een zand-provider); de grondregel van de zaailing (change 22, gebouwd: `#dirt`, farmland en `corruptedadventure:desert_plant_soil` = zand, rood zand, terracotta, gebarsten klei) bepaalt het placement-filter via `would_survive(palm_sapling)`.
- **Changes 18–20** (`infra`, `conditions`, `atmosphere`) raken geen worldgen. Change 27 (uitdroging) en 28 (ringweer) herkennen de oase straks aan de biome-key `corruptedadventure:oasis_flats`.

---

## Achtergrond (uitgezocht, niet opnieuw doen)

- **Een biome-source krijgt alleen de `Climate.Sampler`**, niet de `RandomState`. Een eigen biome-source kan dus geen geseede noise-density-functions uitrekenen; biomes en terrein kunnen alleen dezelfde ruis delen via de zes klimaatvelden van de noise router (of via eigen Java-ruis uit de wereldseed, vraag 1 B).
- In vanilla gebruiken alleen het klimaat (biomes, spawn-finder) de router-velden `temperature` en `vegetation`; het terrein niet. De spawn-finder doet er bij ons niets mee (`spawn_target` = alle parameters `[-2, 2]`). Een `ring_blend` in die twee velden met ring 1 = de huidige vanilla-ruis laat ring 1 dus ongewijzigd (buiten de overgangszone, waar `withered_expanse` toch wint).
- `minecraft:multi_noise` kent een inline-vorm (`biomes`: lijst van `biome` + `parameters` met bereiken binnen [−2, 2]). Een parameter met bereik [−2, 2] telt niet mee in de afstand. Met alleen `temperature` en `humidity` als onderscheidende assen valt de biomegrens precies op de drempelwaarde.
- `minecraft:bandlands` (vanilla) hardcodet terracotta: niet bruikbaar voor kloofsteen (overdracht change 21). Surface rules zijn geen registry-entries: een lagenblok kan niet bij naam hergebruikt worden, wel via een `biome_is`-lijst met meerdere biomes.
- Surface rules vervangen alleen het `default_block` (steen): ertsen, ore veins, vloeistof en lucht blijven staan.

---

## Plan

### 1. Gedeelde opzet ring 2: twee regio-assen (vraag 1 A, 2)

- **Twee 2D-regiosignalen** (handgeschreven JSON, `density_function/ring_2/overworld/`):
  - `relief.json` = `flat_cache(noise corruptedadventure:ring_2_relief, y_scale 0)` — hoog = rotsig (kloven, later hoodoos), laag = vlak (oase, later duinen en zout).
  - `moisture.json` = `flat_cache(noise corruptedadventure:ring_2_moisture, y_scale 0)` — nu alleen in de router (geen biome gebruikt hem nog); change 30 deelt er de vlakke en rotsige regio's mee op.
  - Noise-parameters in `worldgen/noise/ring_2_relief.json`, `ring_2_moisture.json` (eigen keys, dus eigen seed-afleiding; `firstOctave` volgens vraag 2).
- **Router** (`noise_settings/overworld.json`):
  - `temperature` → `corruptedadventure:ring_blend/overworld_temperature` = `ring_blend` met ring 1 = de huidige inline vanilla-`shifted_noise` (letterlijk verplaatst), ring 2 = `ring_2/overworld/relief`, ring 3–5 en void = `0`.
  - `vegetation` → `corruptedadventure:ring_blend/overworld_vegetation`, idem met `ring_2/overworld/moisture`.
  - Ring 3–5 vullen later hun eigen invoer in (zelfde patroon); Nether-ring 2 (change 35) kan hetzelfde doen in de Nether-router.
- **Biome-source ring 2** (`corrupted_world.json`): `minecraft:multi_noise` met inline `biomes`:
  - `canyonlands`: `temperature` [T, 2], overige vijf [−2, 2];
  - `oasis_flats`: `temperature` [−2, T], overige vijf [−2, 2].
  - T zo kiezen dat canyonlands ~55 % en oasis_flats ~45 % van ring 2 beslaat (startwaarde, test W).
- **Geplande indeling voor change 30** (vastleggen in `fase-1-wereld.md`, niet bouwen): rotsig (relief hoog) → `canyonlands` (vochtig/midden) en `hoodoo_fields` (droog); vlak (relief laag) → `oasis_flats` (vochtig), `dune_sea` (midden), `salt_flats` (droog). Change 30 voegt punten toe met `humidity`-bereiken; de oase krimpt dan naar het vochtige deel. Tot change 30 ligt er dus naar verhouding veel oase.
- **Gevolg:** terrein en biome lezen dezelfde named density function → de biomegrens valt precies op de terreinovergang, zonder Java.

### 2. Ring 2-terrein (handgeschreven JSON)

Bestanden in `density_function/ring_2/overworld/` (allemaal 2D tenzij vermeld; waarden zijn startwaarden voor vraag 5 A, afstemmen in test W):

| Bestand | Inhoud |
|---|---|
| `relief_weight.json` | `spline` op `relief`: 0 onder T − 0,04, 1 boven T + 0,04, met 2–3 vlakke treden ertussen (vraag 6 A: terrasvormige steilrand tussen vlakte en plateau; bij vraag 5 B is de steilrand ~55–75 blokken hoog). Biomegrens (T) ligt halverwege de steilrand |
| `flats_height.json` | vlakte Y 66–74: basis 70 + zachte heuvelruis (`ring_2_hills`, ±4) − poelen: waar `ring_2_pools` boven een drempel komt zakt de vlakte tot Y ~59 (vraag 9 A; water tot zeeniveau 63 → poelen 1–4 diep) |
| `plateau_height.json` | plateau Y 125–140 (basis 132 + heuvelruis ±7; vraag 5 B) |
| `canyon_depth.json` | kloofnetwerk: `abs(noise ring_2_canyon)` (meanderende geulen) gemengd met een tweede, fijnere geul-ruis voor zijtakken (`min`); `spline` op dat signaal: 0 → volle diepte (bodem Y ~70, dus 55–70 onder het plateau; vraag 5 B), ~0,04 → wand met 3–4 treden (vraag 6 A; treden van ~12–18 blokken hoog), ≥ ~0,08 → 0. Bodembreedte 10–40 blokken; door de diepere wanden met treden wordt de kloof aan de bovenrand breder (startwaarde ~60–110 blokken, afstemmen in de terreincheck) |
| `height.json` | `flat_cache`: `flats + relief_weight · (plateau − flats) − relief_weight · canyon_depth`, geklemd zodat de kloofbodem nooit onder Y 66 komt (geen zeewater in de kloven) |
| `terrain.json` | 3D zonder grotten: `(height − y) / S` + (vraag 6 A) kleine 3D-ruis `ring_2_wall` met amplitude die alleen bij steile wanden telt (ruwe wanden, kleine overhangen ≤ 2 blokken). `S` zo kiezen dat de dichtheidshelling vergelijkbaar is met vanilla's `sloped_cheese` (≈ 0,04–0,08 per blok), zodat de grotformules uit stap 3 kloppen |

- `ring_2/overworld.json` (vervangt de placeholder) = de vanilla-`final_density`-opbouw van `ring_1/overworld.json` (inline kopie) met `sloped_cheese` → `corruptedadventure:ring_2/overworld/terrain` (vraag 4 A: noise-grotten, ingangen, noodles). Zonder grotten (vraag 4 B/C): `interpolated(terrain)`.
- `ring_blend/overworld_initial.json`: ring 2 → `0.390625 + ring_2/overworld/terrain` (zonder grotten; zelfde reden als ring 1 in change 9: geen preliminary surface onder grotplafonds, goedkoper).
- Nieuwe noise-parameters: `ring_2_canyon`, `ring_2_hills`, `ring_2_pools`, `ring_2_wall` (+ de twee regio-assen). `placeholder_terrain` blijft (ring 3–5).
- Overgangen: grens 1→2 mengt vanilla (ring 1) en ring 2 over de zone van ~128 blokken (smoothstep); grens 2→3 mengt ring 2 met placeholder ring 3 (Y ~120). **Met vraag 5 B** is het grootste verschil op grens 1→2 vanilla (Y ~64–90) ↔ plateau (tot Y 140): tot ~75 blokken over ~128 blokken, gemiddeld ~1:1,7 en in het midden van de smoothstep steiler — dat is de "steile overgang (~1:2)" die optie B noemde en door de gebruiker gekozen. Op grens 2→3 is het verschil vlakte Y ~70 ↔ Y ~120 (50) of plateau Y ~132 ↔ Y ~120 (12). Beoordelen in de terreincheck (teststop 2); voelt het te steil, dan **stop-en-vraag** (mogelijke mitigatie: `relief_weight` in de overgangszone afzwakken).
- Werkwijze: de inline kopie van de vanilla-`final_density` met een klein script in `build/` (niet committen), zoals change 9; controle: terugvervangen = `ring_1/overworld.json`.

### 3. Ring gate (vraag 3 A)

- Nieuw: `worldgen/densityfunction/RingGate.java`, type `corruptedadventure:ring_gate` (`dimension`, `ring`, `argument`; registratie in `ModDensityFunctionTypes`, zodat `codec()` geldig is). `compute`: als ring `ring` op deze kolom gewicht kan hebben (binnen de ring, of in een overgangszone met die ring, plus een **marge van 16 blokken effectieve afstand**) → `argument`, anders `0`. Cache per kolom via `RingColumnCache` (eventueel uitgebreid met de effectieve afstand of een "ring-bereik"-check in `RingLayout`); geen veranderlijke instantiestaat.
- `RingBlend.mapAll`: per ring-input een visitor die elke `interpolated`-marker vervangt door `interpolated(ring_gate(ring, argument))` vóór hij de echte visitor aanroept. **Idempotent:** een argument dat al een `RingGate` is niet opnieuw wikkelen (`mapAll` draait zowel bij het bedraden van de noises in `RandomState` als in elke `NoiseChunk`). `flat_cache`-markers alleen gaten als de meting dat zinvol maakt.
- `minValue`/`maxValue` van de gate nemen 0 mee.
- Waarom de marge: de interpolator rekent op celhoeken (4 blokken horizontaal); waar een ring gewicht heeft moeten alle hoeken van de cel echt gerekend zijn. De effectieve afstand kan lokaal steiler lopen dan 1 blok per blok (valkuil), dus 16 i.p.v. 4.
- Effect: in elke chunk rekent alleen het terrein van de 1–2 ringen die er gewicht hebben (ook ring 1's vanilla-stapel verdwijnt uit ring 2–5).
- Vraag 3 B: dit alles alleen als de meting (§Tests) boven 1,3× vanilla komt.

### 4. Biomes (datagen): `worldgen/biome/Ring2Biomes.java`

- `ModBiomes`: `CANYONLANDS`, `OASIS_FLATS` erbij; `PLACEHOLDER_RING_2` weg (ook uit `PlaceholderBiomes` en beide lang-providers). Werelden van vóór change 23 laden daarna niet meer (zoals change 8/14).
- `BiomeAmbience.Palette.RING_2` = de placeholder-kleuren van ring 2 (lucht `9FC4E8`, fog `E8D7A8`, water `32A598`, waterfog `0B3A35`) — **één plek** die change 32 definitief maakt; alle ring 2-biomes gebruiken hem.
- Gedeelde helper `Ring2Biomes.generation(...)` voor alle ring 2-biomes (ook die van change 30): vanilla `globalOverworldGeneration` (de private helper uit `TransitionBiomes` package-private maken en hergebruiken), `addDefaultOres` als tijdelijke stand (change 25 vervangt ze door de afgebouwde set; let op: vanilla-ertsen komen niet in kloofsteen), `addDefaultSoftDisks`; daarna per biome de eigen features, eigen features altijd achteraan hun stap. Vanilla-features in vanilla-volgorde (valkuil "Feature order cycle").
- **`canyonlands`**: geen neerslag, temperatuur 2,0, downfall 0; gras `BFB755`, blad `AEA42A` (savanne-achtig). Features (§6): kloofflora. Spawns: vraag 11.
- **`oasis_flats`**: geen neerslag, temperatuur 2,0, downfall 0,4 (alleen kleur/landbouw, geen regen); gras `6FB33F`, blad `5DA032` (warm groen), water `32A598`. Features: palmen, oase-begroeiing, suikerriet. Spawns: vraag 11.
- **Tags** (`ModTags.Biomes`, `ModBiomeTagsProvider`): nieuw `corruptedadventure:is_ring_2` (beide biomes; changes 25, 26, 30, 31 voegen toe/gebruiken hem voor biome modifiers en filters), daarnaast `c:is_hot/overworld` en `c:is_dry/overworld`. **Geen** `minecraft:`-tags (geen `is_overworld`, geen `has_structure/*`, geen `is_badlands` — die brengt mesa-mineshafts mee): geen vanilla-structures in ring 2 tot fase 3.
- Lang: `biome.corruptedadventure.canyonlands` "Canyonlands" / "Kloofland", `oasis_flats` "Oasis Flats" / "Oasevlakte".

### 5. Surface rules (`noise_settings/overworld.json`)

Bovenaan `above_preliminary_surface`, ná de tak `withered_expanse`:
- **`canyonlands`** — vloer (`stone_depth floor`): `steep` → geen blok (valt door naar de lagen hieronder); onder een plafond → `red_sandstone`; kloofbodem (`not y_above 78`) → `red_sand` met vlekken `cracked_clay` (surface-ruis; drooggevallen beekjes, voorbode van change 31) en `gravel`; plateau → `red_sand`. Onder de vloer (`add_surface_depth`) → `red_sandstone`.
- **`oasis_flats`** — vloer: `steep` → `sandstone`; onder water → `sand`; boven water en `not y_above 66` → `grass_block` (groene oeverrand rond de poelen, vraag 9 A); anders `sand`. Onder de vloer: `sand` (oppervlaktediepte), daaronder `sandstone` (zoals vanilla-woestijn, `secondary_depth_range` 6).

**Kloofsteenlagen** (vraag 7 A, 8 A) als apart blok in de top-level-`sequence`, ná `above_preliminary_surface` en vóór de deepslate-gradient:
- `biome_is [corruptedadventure:canyonlands]` → `y_above(absolute 8)` → reeks `y_above(absolute Yᵢ, surface_depth_multiplier 1)` → tint. Een vast, met de hand gekozen patroon van banden van 2–8 blokken (rood/oranje/crème, rood overheerst) dat zich elke ~48 blokken herhaalt (vraag 5 B: de wanden zijn Y ~70–140 hoog, dus ~1,5 periode per wand i.p.v. ~2 bij ondiepe kloven en minder zichtbare herhaling); de surface-diepte geeft elke grens een golving van ±1–2 blokken. Horizontaal en overal gelijk, zoals echte sedimentlagen (beide kloofwanden tonen dezelfde lagen).
- Dekt alles van Y 8 tot het oppervlak in canyonlands (steile wanden, plateau-binnenste, ondergrond); onder Y 8 blijft deepslate. Change 30 zet `hoodoo_fields` in dezelfde `biome_is`-lijst; change 25 zet turkoois via `#canyon_stones`.
- De rijen JSON genereren met een klein script in `build/` (niet committen) als het met de hand te foutgevoelig is.

### 6. Features (datagen): `worldgen/feature/Ring2Features.java` (+ vraag 10 A: trunk/foliage placers)

- **`palm_tree`** (configured, key `ModConfiguredFeatures.PALM_TREE` uit change 22) = `minecraft:tree`:
  - stam `palm_log`, blad `palm_leaves`, `dirt_provider` = `sand` (vanilla vervangt niet-`#dirt` onder de stam; gras onder een palm wordt zand), `ignore_vines`, geen decorators;
  - vraag 10 A: eigen `PalmTrunkPlacer` (`corruptedadventure:palm_trunk_placer`: hoogte 6–10, eerste derde recht, daarna in één willekeurige richting 1–2 blokken overhellend; registry `ModTrunkPlacerTypes`) en `PalmFoliagePlacer` (`corruptedadventure:palm_foliage_placer`: kroon = topblok + 6–8 bladwaaiers recht en diagonaal, 3–4 lang, het laatste blok 1 lager (hangend); registry `ModFoliagePlacerTypes`). Elk blad ligt ≤ 5 stappen (via bladeren) van de stam, dus binnen vanilla's 6-blokken-verval; de hele boom past binnen ±6 blokken van de herkomst.
- **Placed features** (`ModPlacedFeatures`), allemaal binnen de eigen chunk (`in_square`, geen `random_patch` of `count_on_every_layer` die over chunkgrenzen leest/spreidt):
  - `oasis_palms_shore`: `count` 3 → `in_square` → heightmap (zie onder) → `block_predicate_filter`: blok eronder `grass_block` (de oeverrand) → `would_survive(palm_sapling)` → `biome`.
  - `oasis_palms_sand`: `rarity_filter` 4 → idem, maar zonder grasfilter (verspreide palmen op het zand).
  - `oasis_vegetation`: kort gras, varen en `desert_flower` als `simple_block` per positie (`count` ~6) op gras, `would_survive` + lucht-filter; plus vanilla's suikerriet-patch (`patch_sugar_cane_desert`, vanilla-gedrag, op zijn vanilla-plek in de stap).
  - `canyonlands_vegetation`: `dry_shrub` (~3 per chunk), `barrel_cactus` (~0,5 per chunk), `desert_flower` (zeldzaam, ~0,1), vanilla dode struik; `simple_block`, `would_survive` + lucht-filter.
  - Valt de grondregel van change 22 vraag 1 anders uit dan `#dirt` + `#sand`, dan i.p.v. `would_survive(palm_sapling)` een `block_predicate_filter` op `#sand`/`grass_block` eronder.
- **Heightmap-keuze (bouwstap):** in de 1.21.1-sources nagaan welke heightmaps tijdens de features-stap bijgewerkt worden (`ProtoChunk.setBlockState` → `getPersistedStatus().heightmapsAfter()`). Kies de heightmap die alleen het terrein toont (verwachting `WORLD_SURFACE_WG`/`OCEAN_FLOOR_WG`); anders `MOTION_BLOCKING_NO_LEAVES` (negeert overhangende palmbladeren van buren). Resultaat in `CODEBASE.md` (valkuil).
- Zaailing: `ModTreeGrowers.PALM` vindt nu de feature → bonemeal laat een palm groeien.

### 7. Opruimen en documentatie

- Preset: ring 2 → de `multi_noise` uit stap 1; `placeholder_ring_2` overal weg; `fase-1-wereld.md` §1.15: ring 2 → "change 23".
- `CODEBASE.md`: nieuwe classes (`Ring2Biomes`, `Ring2Features`, `RingGate`, `PalmTrunkPlacer`, `PalmFoliagePlacer`, `ModTrunkPlacerTypes`, `ModFoliagePlacerTypes`), registries (`ring_gate`, placer-types, `palm_tree` en placed features, `is_ring_2`), biomes-tabel (ring 2, placeholder weg), databestanden (`ring_2/overworld/*`, `ring_blend/overworld_temperature`/`_vegetation`, gewijzigde router en surface rules, noise-bestanden), `Palette.RING_2`, meetresultaten (valkuil density functions), valkuilen: biome-source krijgt alleen de sampler (regio's via router), heightmap tijdens features, kloofbodem ≥ 66 (anders zeewater), lagenblok buiten `above_preliminary_surface`. Kopregel: change 23.
- `fase-1-wereld.md` §2 Ring 2: **Uitwerking (change 23)** — regio-assen en de geplande indeling voor change 30, hoogtes, kloofvorm, lagen, oase, palm, spawns, tags; §1.2/§1.10: klimaat per ring via de router.
- `02-beslissingenlog.md`: regels hieronder.
- `docs/backlog.md`: ring gate-regel weg (vraag 3 A) of bijwerken met de meting (3 B); de change 22-regel "palm_tree-key" weg; nieuw: `[structures]` ring 2-biomes hebben geen `has_structure/*`-tags (woestijnpiramide/-dorp, mineshafts, ruined portals: keuze in fase 3); `[worldgen]` change 25: vanilla-ertsen en ore veins in/naast kloofsteen (vanilla-ertsen vervangen alleen `#stone_ore_replaceables`); bij vraag 11 C: `[mobs]` spawns ring 2.

---

## Overdracht naar latere changes

| Change | Wat er klaarligt |
|---|---|
| 25 ores | `#corruptedadventure:is_ring_2` voor biome modifiers of `Ring2Biomes.generation` (ertsen-slot); plateau Y 125–140 en kloofwanden Y 70–140 (zonnesteen "hoog in canyonwanden": bovenste helft Y ~105–140; vraag 5 B); kloofsteen van Y 8 tot het oppervlak in canyonlands (turkoois "diep in kloofsteen" via `#canyon_stones`); de tijdelijke `addDefaultOres` vervangen |
| 26 grotten | vanilla noise-grotten en carvers in ring 2 (vraag 4 A); aquifers aan; open ruimte onder Y 63 loopt vol (zeeniveau); oasepoelen liggen op Y 59–63 (reservoirs eronder sluiten daar verhaalmatig op aan) |
| 30 extra biomes | assen `relief`/`moisture` en de geplande indeling (stap 1); hoogtekader vlakte Y 66–74 / plateau Y 125–140 (hoodoovloer daartussen); `height.json` krijgt extra regio-hoogtes (duinen, zoutvlak) via `relief_weight` en een tweede gewicht op `moisture`; `multi_noise`-punten toevoegen; `hoodoo_fields` in het lagenblok; `Ring2Biomes.generation` |
| 31 `dry_riverbeds` | `transitions.ring_2` in de preset; `cracked_clay` op de kloofbodems als voorbode; terrein mengt al ring 2 → 3 |
| 32 sfeer | `BiomeAmbience.Palette.RING_2` (één plek); mood/ambient per biome in `Ring2Biomes` |
| 35 Nether-ring 2 | zelfde patroon: `ring_blend` in de Nether-router (`temperature`/`vegetation`) + `multi_noise`; ring gate geldt ook in de Nether |

---

## Client/server-impact

- Puur worldgen, server-side en deterministisch per seed: alle ruis is geseed via noise-keys van de wereld; `RingGate` gebruikt `RingLayout` (wereldseed) via `RingColumnCache` (ThreadLocal, geen gedeelde staat). Geen netwerk, geen SavedData, geen client-code.
- Biomes gaan via vanilla naar de client (kleuren, geen neerslag). De client kent alleen de biome, niet het ringterrein; niets nodig.
- Trunk/foliage-placer-types en `ring_gate` zijn common registries (worldgen-registries, niet gesynct; datapack-JSON verwijst ernaar).
- Bestaande testwerelden laden niet meer (`placeholder_ring_2` weg) en ring 1 houdt nieuwe naden alleen als de gate (onterecht) iets verandert — daarvoor de vergelijkingstest.

---

## Tests

Bouwplan: **B, D, W**; aanbevolen ook **S** (preset, codecs, feature-volgorde worden pas bij het laden gecontroleerd). Alle W-metingen die het kunnen via server + RCON (zie valkuilen in `CODEBASE.md`), daarna RCON weer uit. Testseed `20260930`, `level-name` per wereld nieuw (`smoke-change23-…`).

### B — `./gradlew build`
- [ ] `RingGateTest`: binnen ring + marge gelijk aan het argument (ook in beide overgangszones van de ring); daarbuiten 0; marge-rand; codec-rondreis; idempotent (twee keer `mapAll` → één gate).
- [ ] `RingBlendTest` uitgebreid: `mapAll` wikkelt alleen `interpolated`-markers, per ring met de juiste ring.
- [ ] `PalmTreeShapeTest` (vraag 10 A): voor 200 seeds: elk blad bereikt via bladeren in ≤ 6 stappen een stamblok; alle blokken binnen ±6 van de herkomst; stamhoogte en overhelling binnen de grenzen.
- [ ] `Ring2BiomesTest`: geen neerslag, temperatuur 2,0, lucht/fog/water = `Palette.RING_2`; beide in `is_ring_2`; geen `minecraft:`-tag (via `ModBiomeTagsProvider`).
- [ ] Script-controle: inline `final_density` in `ring_2/overworld.json` terugvervangen = `ring_1/overworld.json`.

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit: `canyonlands.json`, `oasis_flats.json`, geen `placeholder_ring_2.json`; `palm_tree` + placed features; tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`; lang en/nl.

### S — server + RCON (geautomatiseerd)
- [ ] `tools/server-smoke-test.sh`: start zonder fouten (geen "Feature order cycle", preset en codecs laden, geen client-class-melding).
- [ ] **Ring 1 ongewijzigd:** wereld A (stand vóór change 23) en wereld B (na change 23), zelfde seed: (1) `execute if biome` op 300 punten in ring 1 (r 0–1.350, verschillende Y) → identiek; (2) per chunk `fill … replace` tellingen (steen, lucht, water, lava, deepslate) in 6 ring 1-chunks → identiek. Toont dat router-wijziging en gate ring 1 niet raken.
- [ ] **Gate verandert niets:** wereld B zonder gate (tijdelijke build) en C met gate: dezelfde tellingen in 4 chunks in zone 1→2, 4 in ring 2, 4 in zone 2→3 → identiek (geen naden). Alleen bij vraag 3 A, of zodra de gate gebouwd wordt.
- [ ] **Biomes:** `locate biome corruptedadventure:canyonlands` en `oasis_flats` vanaf `tpring 2`-punten in 4 richtingen → gevonden binnen ring 2; langs de cirkel r = 2.500 elke 16 blokken `execute if biome` → aandeel canyonlands 45–65 %, gemiddelde vlekgrootte volgens vraag 2.
- [ ] **Hoogtes** (`execute positioned x 0 z positioned over world_surface` + marker, 100 punten per biome): oasis_flats Y 59–76, plateau Y 123–142, aanwezig: kloofbodems Y 66–76 (kloofdiepte t.o.v. het aangrenzende plateau 55–70); **geen water aan het oppervlak in canyonlands**; in oasis_flats-chunks wel water (poelen) op Y ≤ 63.
- [ ] **Lagen:** canyonlands-chunk: `fill … replace red_canyon_stone` (en oranje/crème) Y 60–145 → > 0; oasis-chunk → 0; onder Y 8 → 0.
- [ ] **Palm:** `place feature corruptedadventure:palm_tree` op zand en op gras → `palm_log` 6–10, `palm_leaves` > 0, het blok onder de stam is zand. Bladverval vraagt random ticks en dus een speler (valkuil): naar W-achteraf.
- [ ] **Palmen in het wild:** in 16 oasis-chunks `palm_log` > 0 in de meeste chunks met een poel; in canyonlands-chunks 0.
- [ ] **Determinisme:** twee verse werelden met dezelfde seed, de tweede in omgekeerde volgorde geladen (zoals change 16): per chunk (16 oasis-, 16 canyonlands-chunks) tellingen van `palm_log`, `palm_leaves`, `dry_shrub`, `barrel_cactus`, `desert_flower`, rode kloofsteen → identiek. Verschillen = bug in de placement (of gedocumenteerd vanilla-restgedrag zoals bij suikerriet).
- [ ] **Performance** (zoals change 9, JFR `minecraft.ChunkGeneration`, 16×16 chunks, tweede ronde telt): ring 1 (x 750), ring 2 (x 2.500), ring 3 (x 4.500) in wereld A, B (zonder gate), C (met gate) en de vanilla-preset. Budget noise-stap ≤ 1,3× vanilla in alle drie. Getallen in `CODEBASE.md`.

### W — blokkerende handmatige test: **ja — teststop 2 (terreincheck ring 2)**, ná deze change; mag lopen tijdens de bouw van change 24, akkoord vereist vóór change 25 (en dus vóór 26/30)

> **Bijgewerkt 2026-10-01:** eerste ronde gedaan (feedback → `teststop-2-feedback.md`, uitgewerkt in 23b/23c). Het akkoord verhuist naar **teststop 2b na change 30** (vóór 31); change 25 wacht er niet meer op.
**Reden:** of de kloven, steilranden, lagen en oases er goed uitzien en goed spelen is niet automatisch te meten, en changes 25 (zonnesteen "hoog in canyonwanden", turkoois "diep in kloofsteen": Y-bereiken), 26 (grotten en reservoirs onder dit terrein) en 30 (drie nieuwe regio's in hetzelfde hoogtekader en lagenblok) bouwen direct op deze hoogtes en vormen. Later bijstellen betekent die changes opnieuw afstemmen. Alles wat wél automatisch kan (hoogtes, water, aandelen, determinisme, ring 1 ongewijzigd, naden) staat onder S.

Blokkerend (runClient, testseed, ~15 min; beoordeelt de hoogtes van vraag 5 B):
- [ ] `/ca tpring 2` in 4 richtingen: kloven lezen als kloven (diepte 55–70, breedte, meanderen, zijtakken), plateau Y 125–140 en steilrand (~55–75 hoog) kloppen; terrassen/wanden zien er natuurlijk uit en zijn beklimbaar via de treden (vraag 5 B, 6).
- [ ] Kloofsteenlagen: herkenbare horizontale banden, niet te regelmatig, aan beide wanden gelijk (vraag 7).
- [ ] Oase: poelen met groene oeverrand, palmen vooral aan het water, voelt als "het enige veilige water" (vraag 9).
- [ ] Palm: vorm leest als palm, geen zwevende of losse bladeren (vraag 10).
- [ ] `/ca tpboundary 1` en `/ca tpboundary 2`: overgang zonder verticale wand, zonder zwevende brokken of waterwand; de steile overgang ring 1 → plateau (tot ~75 blokken over de zone) is acceptabel (vraag 5 B; anders stop-en-vraag).

W-achteraf (checklist, niet blokkerend):
- [ ] Bonemeal op `palm_sapling` (zand en gras) → palm; bladeren naast de stam vervallen niet (`/gamerule randomTickSpeed 100`), losse palmbladeren wel.
- [ ] Grotten in ring 2 (vraag 4): ingangen in kloofwanden, geen waterwanden; ondergrond onder canyonlands is kloofsteen tot Y 8.
- [ ] F3: biome klopt bij het terrein; geen regen in ring 2 als het in ring 1 regent; kleuren gras/blad/water.
- [ ] Spawns volgens vraag 11 ('s nachts husks/zombies, overdag konijnen).
- [ ] Portal vanuit ring 2 naar de Nether en terug: landt in Nether-ring 2 (÷4) en terug in ring 2.
- [ ] Geen fouten of waarschuwingen in de log bij het aanmaken van de wereld.

---

## Risico's / aandachtspunten

- **Router-klimaat wijzigen raakt ring 1** als de verplaatste vanilla-ruis niet exact gelijk is (ander `xz_scale`, andere shift): daarom de biome- en blokvergelijking A↔B (S).
- **Naden door de gate** bij een te kleine marge of een fout in de bereik-check: vergelijkingstest B↔C; marge desnoods groter (kost alleen rekenwerk in een smalle band).
- **Gate dubbel gewikkeld** (`mapAll` draait in `RandomState` én `NoiseChunk`): idempotentie + unit test.
- **Zeewater:** alles onder Y 63 dat open ligt loopt vol. Kloofbodem en vlakte buiten de poelen moeten ≥ 66 blijven (klem in `height.json`); grotingangen onder 63 kunnen water geven (vanilla-gedrag, aquifers).
- **Dichtheidsschaal:** wijkt `terrain` sterk af van vanilla's `sloped_cheese`, dan kloppen de grotformules niet (te veel of te weinig grotten, open plafonds). Afstemmen met de W-grottencheck.
- **Feature order cycle:** eigen ring 2-biomes delen vanilla-features met de 52 kopieën; vanilla-helpers in vanilla-volgorde, eigen features achteraan (S vangt het).
- **Generatievolgorde:** palmbladeren hangen tot ~6 blokken in buurchunks; een overhellende stam kan over de chunkgrens gaan. Heightmap-keuze (bouwstap) en de determinismetest dekken het; resterende verschillen documenteren.
- **Oase-aandeel tot change 30** is groot (~45 %): bewust tijdelijk; change 30 verkleint het.
- **Vanilla-ertsen** komen niet in kloofsteen (change 21: niet in `#base_stone_overworld`): canyonlands is boven Y 8 tijdelijk ertsarm tot change 25. Ore veins (koper/ijzer, Y < 50) worden in de noise-stap gezet en blijven staan.
- **Performance** zonder gate: twee volledige vanilla-achtige grottenstapels per chunk; daarom vraag 3.
- **Hoogte-overgang 1→2 en 2→3** (vraag 5 B): ring 1 (Y ~64–90) ↔ plateau tot Y 140 is steil (~1:1,7 gemiddeld); vlakte Y ~70 ↔ placeholder ring 3 Y ~120 idem als voorheen. Ring 3-terrein (change 39) herziet 2→3. Beoordeeld in teststop 2.
- **Diepe kloven** (vraag 5 B): meer klimwerk; spelers zonder ladders/blokken kunnen in een kloof vastzitten tot ze een trede vinden. De treden (vraag 6 A) zijn de natuurlijke paden; beoordelen in teststop 2.

---

## Ontwerpvragen

Ronde 1 = architectuur (1–4), ronde 2 = terrein en bodem (5–8), ronde 3 = oase, palm en leven (9–11).

### 1. Hoe kiezen biomes en terrein dezelfde regio's? (gedeelde aanpak voor ring 2–5)
- **A. Klimaat per ring via de router (aanbevolen).** `temperature`/`vegetation` van de router worden een `ring_blend` (ring 1 = de huidige vanilla-ruis, ring 2 = eigen assen `relief`/`moisture`); ring 2 krijgt `minecraft:multi_noise` met uitgeschreven parameters. Terrein en biome lezen dezelfde named density function, grenzen vallen exact samen. Puur JSON, geen nieuwe Java; change 30 voegt alleen punten en hoogtes toe; herbruikbaar voor ring 3–5 en de Nether (35). Ring 1 moet identiek blijven (RCON-vergelijking in S).
- **B. Eigen Java-types: biome-source `ring_regions` + density function `ring_region`**, die allebei dezelfde ruis uit de wereldseed (`RingLayout`) maken. Volledig los van de router; meer Java, codecs en tests, en het determinisme van eigen ruis zelf bewaken.
- **C. Vanilla-klimaatruis ongewijzigd ook in ring 2 gebruiken** (multi_noise op vanilla `temperature`/`vegetation`, het terrein kopieert de ruisdefinitie). Geen routerwijziging, maar vaste vanilla-schaal (vlekken van 1.000–3.000 blokken: per biome maar een handvol in de hele ring) en ring 2 deelt zijn "klimaatkaart" met ring 1.

Antwoord: **A** — klimaat per ring via de router (`ring_blend` in `temperature`/`vegetation`, ring 2 = `relief`/`moisture`) + inline `multi_noise`; ring 1 moet identiek blijven (S-vergelijking verplicht).

### 2. Hoe groot worden de biomevlekken in ring 2? (geldt ook voor change 30)
Ring 2 is 2.000 blokken breed en ~15.000 blokken rond (midden).
- **A. Middel: vlekken van ~500–1.000 blokken (aanbevolen).** Een biome voelt als een gebied om te verkennen; elke biome komt tientallen keren voor in de ring, ook na change 30 (5 biomes). Een speler die rechtdoor loopt ziet 2–4 regio's.
- **B. Klein: ~200–400 blokken.** Afwisselend, snel van kloof naar oase; minder gevoel van uitgestrektheid, meer steilranden (overgangen) per km.
- **C. Groot: ~1.500–2.500 blokken.** Zoals vanilla; weidse regio's, maar op sommige routes door de ring zie je één biome en mis je de oase of de kloven.

Antwoord: **A** — middel: vlekken van ~500–1.000 blokken.

### 3. Ring gate nu bouwen?
Backlog (change 9): "ring gate bouwen bij de eerste zware ring 2-invoer (change 23)". Met noise-grotten (vraag 4 A) rekent elke chunk zonder gate twee vanilla-achtige stapels.
- **A. Ja, in deze change, met meting vóór/na en een vergelijkingstest op naden (aanbevolen).** Schaalt meteen voor ring 3–5 (straks vijf terreinen); één keer de naadcontrole bij de eerste echte ringgrens. Iets meer Java (`RingGate`) en testwerk in deze change.
- **B. Eerst meten, beslisregel van change 9** (≤ 1,3× vanilla → niet bouwen, backlog bijwerken). Kleinere change als het meevalt; de gate komt dan bij ring 3 met hetzelfde naadrisico.

Antwoord: **A** — ja, ring gate in deze change, met meting vóór/na en vergelijkingstest op naden.

### 4. Krijgt ring 2 nu al grotten?
Change 26 voegt waterreservoirs en instortende zandgrotten toe.
- **A. Ja: vanilla noise-grotten (cheese, spaghetti, noodles, ingangen) in de ring 2-invoer + vanilla carvers (aanbevolen).** Ring 2 is geen massief blok; grotingangen in kloofwanden; change 26 bouwt erop voort. Kost rekenwerk (vraag 3).
- **B. Alleen vanilla carvers (grotten en ravijnen van de biome), geen noise-grotten.** Goedkoper, minder grotten; change 26 beslist over noise-grotten.
- **C. Niets tot change 26.** Eenvoudigst; tot dan is de ondergrond massief, en de dichtheidsschaal van het terrein wordt pas in 26 tegen de grotformules afgestemd.

Antwoord: **A** — ja: vanilla noise-grotten + vanilla carvers in ring 2 (change 26 vraag 1 en stap 1 vervallen).

### 5. Hoogtes en diepte van de kloven
- **A. Plateau Y 100–115, kloofbodem Y ~70 (30–45 diep), vlakte en oase Y 66–74 (aanbevolen).** Indrukwekkend maar beloopbaar; steilrand vlakte → plateau ~35 blokken; past bij placeholder ring 3 (Y ~120) en ring 1 (vanilla).
- **B. Diep: plateau Y 125–140, bodem Y ~70 (55–70 diep).** Spectaculair; steilrand ~60 blokken, de overgang vanuit ring 1 wordt steil (~1:2), veel klimwerk.
- **C. Ondiep en breed: plateau Y 85–95, kloven 15–25 diep, bredere dalen.** Makkelijker te bereizen, minder dramatisch; minder ruimte voor "hoog in de wanden" (zonnesteen, change 25).

Antwoord: **B (keuze gebruiker)** — diep: plateau Y 125–140, kloven ~55–70 diep (kloofbodem Y ~70, geklemd ≥ 66), vlaktes en oase Y 66–74. Alle afhankelijke Y-waarden in dit plan (hoogte-DF's, terrassen, lagenbanden, S-metingen) en in 24, 25, 26, 30 en 31 zijn hierop bijgewerkt; de blokkerende terreincheck (teststop 2) beoordeelt deze hoogtes.

### 6. Hoe zien de kloofwanden en steilranden eruit?
- **A. Steil met 2–3 terrassen (mesa-treden via de splines) + lichte 3D-ruis voor ruwe wanden en kleine overhangen (aanbevolen).** Klassiek canyonbeeld; terrassen zijn natuurlijke paden; 3D-ruis kost weinig (alleen in de terreininvoer).
- **B. Verticale wanden, puur 2,5D.** Strak en goedkoop; leest als een uitgesneden geul, weinig natuurlijk; geen overhangen.
- **C. Sterk 3D (overhangen, bogen, losse rotsen).** Meest spectaculair, maar duur, kans op zwevende brokken en het overlapt met `hoodoo_fields` (change 30).

Antwoord: **A** — steil met terrassen (bij de diepe kloven van vraag 5 B: 3–4 treden in de kloofwand, 2–3 in de steilrand) + lichte 3D-ruis voor ruwe wanden en kleine overhangen.

### 7. Hoe ontstaan de kloofsteenlagen?
- **A. Surface-rule-banden in JSON: vast patroon van `y_above`-grenzen, golvend via de surface-diepte (aanbevolen).** Puur data; horizontale lagen die overal gelijk zijn (zoals echte sedimentlagen, beide wanden kloppen); herhaalt zich elke ~32 blokken; change 30 hergebruikt hem via de `biome_is`-lijst.
- **B. Eigen surface-rule-type `corruptedadventure:strata` (Java), geseed patroon zoals vanilla-badlands.** Minder herhaling, natuurlijker; vraagt een access transformer op `SurfaceRules$Context`, eigen codec en tests.
- **C. Tinten in vlekken (ruis) i.p.v. lagen.** Eenvoudig, maar leest niet als "gelaagde kloofsteen".

Antwoord: **A** — surface-rule-banden in JSON: vast patroon van `y_above`-grenzen, golvend via de surface-diepte (periode ~48 blokken bij de hogere wanden van vraag 5 B).

### 8. Hoe diep gaat de kloofsteen?
- **A. In canyonlands de hele kolom van Y 8 tot het oppervlak (aanbevolen).** Het plateau-binnenste en de ondergrond zijn kloofsteen; turkoois kan "diep in kloofsteen" (change 25); vanilla-ertsen ontbreken daar tot change 25 ze toevoegt.
- **B. Alleen boven Y ~60 (zichtbare wanden en plateau); daaronder gewone steen.** Vanilla-ertsen werken onder Y 60 vanzelf; "diep in kloofsteen" kan dan alleen tussen Y 60 en de kloofbodem.
- **C. De hele ondergrond van ring 2 (alle biomes) boven Y 8.** Sterkste ring-identiteit, ook onder de oase; meeste werk voor change 25 (alle ertsen opnieuw) en de oasebodem wordt kloofsteen.

Antwoord: **A** — in canyonlands de hele kolom van Y 8 tot het oppervlak.

### 9. Waar komt het oasewater vandaan?
- **A. Uit het terrein: de vlakte zakt plaatselijk onder zeeniveau en vult zich met water tot Y 63; gras alleen op de lage oeverrand (aanbevolen).** Natuurlijke, organische poelen zonder feature; groen precies rond het water; sluit aan op het verhaal van de reservoirs (change 26). Poelen zijn ondiep (1–4).
- **B. Meer-features (`minecraft:lake` met water) op de vlakte.** Losse ronde meertjes, aantal goed stuurbaar; voelt meer "geplaatst", vanilla-meren zijn verouderd en hangen af van buurchunks; groen rond het water vraagt een extra feature.
- **C. Beide:** terreinpoelen + af en toe een dieper meertje. Meer variatie, meer afstemwerk.

Antwoord: **A** — uit het terrein: de vlakte zakt plaatselijk tot Y ~59 en loopt vol tot Y 63; gras alleen op de lage oeverrand.

### 10. Hoe bouwen we de palm?
- **A. Vanilla `minecraft:tree` met een eigen `PalmTrunkPlacer` (licht overhellende stam) en `PalmFoliagePlacer` (hangende bladwaaiers) (aanbevolen).** Leest echt als palm; vanilla regelt bladverval, `dirt_provider` en de zaailing; twee kleine Java-classes + registry-types en een vormtest.
- **B. Alleen vanilla-placers** (`bending_trunk_placer` + acacia-/random-spread-blad). Geen Java; lijkt eerder op een scheve acacia of azalea.
- **C. Volledig eigen feature-type `palm_tree`.** Alle vrijheid (kokosnoten, kromme stam), maar bladverval, vrije ruimte en de zaailing-koppeling zelf regelen.

Antwoord: **A** — vanilla `minecraft:tree` met eigen `PalmTrunkPlacer` en `PalmFoliagePlacer`.

### 11. Welke mobs spawnen er in ring 2 tot fase 4?
- **A. Vanilla-woestijn: 's nachts monsters met husks i.p.v. zombies, konijnen; in de oase ook kamelen (aanbevolen).** Voelt meteen droog en levend; ring 2-mobs (schorpioenen, zandwormen) vervangen of vullen aan in fase 4.
- **B. Alleen monsters, geen dieren.** Leger en vijandiger; minder voedsel in een ring met uitdroging (change 27).
- **C. Geen spawns tot fase 4** (zoals de placeholders). Rustig testen, maar de ring voelt dood en monsters ontbreken 's nachts.

Antwoord: **A** — vanilla-woestijn: 's nachts monsters met husks i.p.v. zombies, konijnen; in de oase ook kamelen.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Biomeverdeling ring 2: klimaat per ring via de router (`ring_blend` op `temperature`/`vegetation`) + inline `multi_noise` | Terrein en biome lezen dezelfde functie; puur JSON; herbruikbaar voor ring 3–5 en de Nether |
| 2026-10-01 | worldgen | Ring 2-regio's: assen `relief` (rotsig/vlak) en `moisture`; geplande indeling change 30: canyonlands/hoodoo_fields op rotsig, oasis_flats/dune_sea/salt_flats op vlak | Eén kader waar change 30 alleen punten en hoogtes aan toevoegt |
| 2026-10-01 | worldgen | Biomevlekken ring 2: ~500–1.000 blokken | Gebieden om te verkennen; elke biome komt vaak voor |
| 2026-10-01 | worldgen | Ring gate nu gebouwd (+ meetgetallen) | Schaalt voor ring 3–5; naadcontrole bij de eerste echte grens |
| 2026-10-01 | worldgen | Grotten in ring 2: vanilla noise-grotten + carvers | Ring 2 geen massief blok; change 26 bouwt erop voort |
| 2026-10-01 | worldgen | Hoogtes ring 2 (keuze gebruiker, diep): plateau Y 125–140, kloven 55–70 diep (bodem Y ~70), vlakte Y 66–74; kloofbodem en vlakte ≥ Y 66 (geen zeewater in de kloven) | Spectaculaire canyons; steilere overgang vanuit ring 1 geaccepteerd |
| 2026-10-01 | worldgen | Kloofwanden: steil met terrassen (3–4 treden in de kloof, 2–3 in de steilrand) + lichte 3D-ruis | Klassiek canyonbeeld; treden als natuurlijke paden |
| 2026-10-01 | worldgen | Kloofsteenlagen: vaste `y_above`-banden in JSON (periode ~48), diepte Y 8 tot het oppervlak in canyonlands | Horizontale lagen, beide wanden gelijk; ruimte voor turkoois diep in kloofsteen |
| 2026-10-01 | worldgen | Oasewater uit het terrein (poelen tot Y ~59, water tot 63, groene oeverrand) | Organische poelen zonder feature; sluit aan op de reservoirs |
| 2026-10-01 | worldgen | Palm: vanilla `minecraft:tree` met `PalmTrunkPlacer` en `PalmFoliagePlacer`; feature onder de key `corruptedadventure:palm_tree` uit change 22 | Leest als palm; vanilla regelt bladverval en zaailing |
| 2026-10-01 | worldgen | Spawns ring 2 tot fase 4: vanilla-woestijn (husks, konijnen, kamelen in de oase) | Droog en levend tot de ring 2-mobs van fase 4 |
| 2026-10-01 | worldgen | Ring 2-biomes zonder `minecraft:`-tags (geen vanilla-structures tot fase 3); eigen tag `is_ring_2` + `c:is_hot/overworld`, `c:is_dry/overworld` | Structures zijn fase 3; tag voor ertsen/grotten/biomes van latere changes |
| 2026-10-01 | worldgen | Lucht/fog/water van alle ring 2-biomes uit één `BiomeAmbience.Palette.RING_2` (placeholder-kleuren); definitief in change 32 | Eén plek voor de sfeerchange |

Commit (na `/code-review`): `worldgen: add ring 2 terrain, canyonlands and oasis flats biomes, palm tree`.

---

## Bouwnotities (change 23, 2026-10-01)

Aannames gecontroleerd tegen de code na change 22 (`af38c05`): klopten (kloofsteen niet in `#base_stone_overworld`, `ModConfiguredFeatures.PALM_TREE`, grondregel zaailing = `#dirt`/farmland/`desert_plant_soil`, `RingBlend`, `RingColumnCache`, `TransitionBiomes.globalOverworldGeneration`). Afwijkingen van het plan (implementatiedetails, door de agent gekozen):

- **Drempel T in `relief` gebakken:** `relief` = ruis + 0,035, biomegrens op precies 0 (`canyonlands` [0, 2], `oasis_flats` [−2, 0]). In zone 2→3 mengt de router `relief` met 0; een grens op 0 blijft dan exact (een grens op T ≠ 0 zou verschuiven). 0,035 geeft gemiddeld over seeds ~55 %; seed 20260930: 59 % (r 1.650–3.350), 50 % op r = 2.500.
- **Extra ruis `ring_2_canyon_branch`** (firstOctave −8) voor de zijtakken; `canyon_signal` = min(|kloof|, |zijtak| + 0,15·|kloof|): zijtakken worden ondieper verder van de hoofdkloof en eindigen in het plateau.
- **Afgestemde waarden** (devkaarten): steilrand ±0,025 (plan ±0,04; anders ~130 blokken breed), kloofprofiel bodem ≤ 0,022, 4 wanden van Δ 0,006 en 3 treden van Δ 0,015 tot 0,091 (smallere treden verdwijnen door de interpolatie), poelen via 4 splinepunten met een flauw stuk rond de waterlijn (bredere grasoever), ~5 % van de oase is water.
- **Eigen preliminary surface `initial_terrain`** (plan: `0,390625 + terrain`): kloofbodemhoogte over het hele rotsgebied en poelen 2× zo diep. Zonder dit (1) slaan de oppervlakteregels kloofbodems over (vanilla interpoleert de preliminary surface over 16 blokken) en (2) laat de aquifer ondiepe poelen droog (hij vult alleen zeker nabij een preliminary surface onder ~Y 55). Het echte terrein (poelbodem Y 59, poelen 1–4 diep) is ongewijzigd. Zijeffect: grotbodems in het plateau boven ~Y 64 krijgen de vloerregels (rood zand).
- **Lagen ook als laatste element van de `canyonlands`-tak:** vanilla's regels binnen `above_preliminary_surface` zetten anders gras op steile vlakken. Het top-level lagenblok staat waar afgesproken.
- **Oeverpalmen 24 pogingen per chunk** (plan 3): met 10 kregen maar 2 van de 10 poelchunks een palm; nu 6 van 10 (2–7 palmen per poel).
- **Canyonlands-flora als één gewogen `simple_block`** (4 per chunk: dorre struik 30, dode struik 8, tonnetjescactus 5, woestijnbloem 1) i.p.v. losse features; zelfde tarieven.
- **Kamelen:** vanilla kent geen spawn placement voor kamelen (NeoForge logt een ERROR, kamelen zouden zonder regels spawnen). Minimale registratie in `worldgen/biome/BiomeSpawnPlacements` (als andere dieren: op gras); raakt `mobs`, in de backlog genoteerd.
- **Grotgeluid** van beide biomes: vanilla's standaard (`LEGACY_CAVE_SETTINGS`); change 32 beslist.
- **Ring gate:** marge 16 blokken van de dimensie (Nether ×4 in effectieve afstand); `RingLayout.reaches` + effectieve afstand in `RingColumnCache`; interpolated-markers herkend aan de codec-instantie (vanilla's markertypes zijn niet publiek).
- **Heightmap-keuze (bouwstap):** `WORLD_SURFACE_WG` (een chunk staat tijdens zijn features op `carvers`: alleen de final heightmaps worden bijgewerkt). Vastgelegd in `CODEBASE.md`.
- **Werkwijze:** tijdelijk debugcommando (`/cadev`: PNG-kaarten uit de density functions, bovenaanzicht en doorsnedes van gegenereerde chunks) en scripts in `build/c23/` (niet gecommit); de JSON van de density functions en surface rules komt uit `build/c23/gen_df.py` en `gen_surface.py`.

### Testresultaten

- **B:** `./gradlew build` geslaagd, 40 testklassen / 194 tests, geen fouten. Nieuw: `RingGateTest` (5), `Ring2TerrainJsonTest` (1, script-controle), `PalmTreeShapeTest` (2 × 200 seeds), `Ring2BiomesTest` (4); uitgebreid: `RingBlendTest` (+1 `mapAll`), `RingColumnCacheTest` (effectieve afstand).
- **D:** `./gradlew runData` geslaagd: `canyonlands.json`, `oasis_flats.json`, `placeholder_ring_2.json` weg; `palm_tree`, `oasis_vegetation`, `canyonlands_vegetation` + 4 placed features; tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`; lang en/nl. Geen ring 2-biome in een `minecraft:`-tag.
- **S** (seed 20260930, RCON, `max-tick-time=-1`, werelden `smoke-change23-*`; rooktest `tools/server-smoke-test.sh` met `level-name=smoke-change23`: `Done (6.668s)`, geen ERROR, geen "Feature order cycle", geen client-class-melding):
  - **Ring 1 ongewijzigd:** 300 biomepunten (r 0–1.350, Y −30/20/62/oppervlak) 300/300 gelijk aan de wereld van vóór change 23; 6 chunks: steen en deepslate identiek; lucht/water/lava wijken per chunk tot ~1 % af door vanilla's globale feature-index (nieuwe biomes met features → andere decoratie-seeds; ook vanilla's eigen laadvolgorde geeft 2–4 blokken verschil). Zie `CODEBASE.md`, valkuilen.
  - **Gate:** wereld zonder en met gate, 12 chunks (4 midden zone 1→2, 4 ring 2, 4 op grens 2→3), 12 bloksoorten × 3 lagen: **identiek** (geen naden).
  - **Performance** (noise-stap, gemiddelde van twee schone rondes): met gate 1,28× / 1,36× / 0,93× vanilla in ring 1 / 2 / 3, zonder gate 1,38× / 1,50× / 1,16×. Ring 2 iets boven het budget 1,3× (backlog). Een eerste ronde liep tegelijk met een handmatig gestarte client en is overgedaan.
  - **Biomes:** `locate biome` vanaf (±2.500, 0) en (0, ±2.500): beide biomes gevonden binnen ring 2 (r 2.316–2.500). Op r = 2.500: canyonlands 50 %, vlekken gemiddeld ~950 blokken (max ~2.200).
  - **Hoogtes** (100 punten per biome, `motion_blocking_no_leaves`): canyonlands Y 68–136 (mediaan 130; kloofbodems vanaf 68; één grotingang 56), **0 van 100 met water**; oasis_flats Y 61–92 (mediaan 70; 6 punten in een poel op Y 63; 4 punten 81–92 = de onderste helft van de steilrand, die bij de oase hoort; één grotingang 24). Bovenaanzicht 384 × 384 canyonlands: 0,28 % van de bovenste blokken water (volgelopen grotingangen onder Y 63), 0,03 % lava (vanilla-lavameren).
  - **Lagen:** canyonlands-chunk Y 60–145: oranje 4.420, crème 2.560 (rood geteld in de determinismetest: 123.799 in 16 kloofchunks); oase-chunk 0; onder Y 8 0.
  - **Palm:** `place feature` op zand en op gras: 7–10 stamblokken, 24–35 bladeren, blok onder de stam altijd zand.
  - **Palmen in het wild:** 6 van 10 poelchunks met palmen (2–7 per poel), 1 van 6 oasechunks zonder poel, 0 in 16 kloofchunks.
  - **Determinisme:** tweede verse wereld, chunks in omgekeerde volgorde geladen: 32/32 chunks identiek (palmstam, -bladeren, dorre struik, tonnetjescactus, woestijnbloem, dode struik, kort gras, suikerriet, rode kloofsteen, water).
  - **`tick query`** na de tests: gemiddeld 0,8 ms/tick (P99 10,6 ms) zonder spelers.
  - RCON weer uit, `run/server.properties` op `level-name=smoke-change23`.
- **Let op:** tijdens de eerste testsessie startte de gebruiker per ongeluk `./gradlew runClient` (werelden `New World` en `New World (1)` in `run/saves/`, niet aangeraakt). Alleen die eerste perf-meting kan verstoord zijn; hij is overgedaan. Determinisme, naden en ring 1-vergelijking hangen niet van CPU-belasting af; `run/server.properties` stond steeds op de testinstellingen.

### Code review (`/code-review medium`)

- **Opgelost:** de vaste gate-marge van 16 blokken dekt de celhoeken alleen tot een `boundaryNoise` van ~180 (config staat tot 4.096 toe); daarboven konden hoeken buiten de marge vallen (kleine kuilen/richels bij de zones). Nu `RingLayout.reachMargin`: 16 blokken van de dimensie × max(1, `boundaryNoise` / 96); `RingGateTest` controleert de celhoeken met grensruis 96, 400 en 1.000. Standaardwerelden ongewijzigd (factor 1), de S-resultaten blijven gelden.
- **Verduidelijkt (geen bug):** `flat_cache`-markers in de ring 2-invoer worden niet gegate; de noise chunk vult ze vooraf (5 × 5 per chunk), goedkoop. Commentaar van `RingGate` en `CODEBASE.md` aangepast.
- **Extra controle na de review:** Nether met en zonder gate (12 chunks rond Nether-grens 1, ring 1 en 2): netherrack, lava, bedrock en nylium identiek; één chunk 11 blokken lucht anders (volgorde-afhankelijke Nether-features). Rooktest daarna opnieuw: `Done (5.916s)`, geen fouten.

## In-game checklist (W) — teststop 2 (terreincheck ring 2, blokkerend vóór change 25)

> **Status 2026-10-01: eerste ronde gedaan.** Feedback in `docs/plans/teststop-2-feedback.md`; verwerkt in `change-23b.md`/`change-23c.md` (afstemvragen 1 en 6 beantwoord door de feedback, 2–5 en 7–10 als vragen 23b-10 … 23b-16 en 23c-7). De resterende beoordeling gaat naar **teststop 2b na change 30** (blokkerend, vóór 31; checklist in plan 30). Deze lijst blijft als referentie; plekken en hoogtes kunnen door 23b veranderd zijn.

~15 min. **Opstarten:** `./gradlew runClient` → Singleplayer → Create New World → World Type **Corrupted World**, Seed `20260930`, Allow Cheats **aan** → Create. Dan `/gamemode creative` en F3 (links de biome, rechts `CA ring (client)`). Vliegen met dubbel-spatie.

1. **Overgang 1 → 2 over land:** `/tp @s 0 100 -1400`, kijk naar het noorden (F3 *Facing: north*) en vlieg naar z −1650. Ring 1 (Y ~76) → withered expanse → steile stijging naar het plateau (Y ~130 vanaf z −1500, ~50 blokken omhoog over ~50 blokken). Let op: steilte, zwevende brokken, waterwanden, de grens tussen grijze steen en de lagen.
2. **Overgang 1 → 2 vanaf zee:** `/tp @s 1400 90 0`, kijk naar het oosten: klif van zee (Y 63) naar plateau (Y ~133) bij x 1500–1550.
3. **Kloven:** `/tp @s 2200 140 -720` (kloofbodem Y ~67 recht onder je), `/tp @s 2100 140 -830` (kruising met brede bodem en treden), `/tp @s 2240 140 -650` (plateau tussen kloven). Let op: diepte (55–70) en breedte, meanderen, zijtakken, treden in de wanden (3 per wand) en of je eruit kunt klimmen, ruwe wanden, kloofsteenlagen (horizontale banden rood/oranje/crème, aan beide wanden gelijk), bodem (rood zand, gebarsten klei, grind), flora (dorre struik, tonnetjescactus).
4. **Oase:** `/tp @s 2550 80 -115` (poel, water tot Y 63) en `/tp @s 2480 80 -64`. Let op: poelen met groene oever, palmen vooral aan het water (ook losse op het zand), kort gras, varens, suikerriet; en de steilrand naar het plateau (onderste helft = oase = zand/zandsteen op grijze steen, bovenste helft = lagen).
5. **Palm van dichtbij:** vorm (stam 6–10, licht overhellend, kruisvormige kroon met hangende topjes), geen zwevende of losse bladeren.
6. **Biomes zoeken:** `/tp @s 2500 140 0`, dan `/locate biome corruptedadventure:canyonlands` (verwacht ~2308, 192) en `/locate biome corruptedadventure:oasis_flats` (verwacht: hier, 2500, 0). F3: rotsig = canyonlands, vlak = oasis_flats, grens halverwege de steilrand.
7. **Rondkijken in de ring:** kijk naar het noorden/oosten/zuiden/westen en doe telkens `/ca tpring 2`.
8. **Grenzen:** `/ca tpboundary 1` en `/ca tpboundary 2`. Grens 2 → 3: vloeiend naar placeholder ring 3 (Y ~120); harde biomewissel met een verticale naad in de rotskleur (bekend tot change 31).
9. **Geen regen in ring 2:** in ring 1 `/weather rain`, dan `/ca tpring 2` → droog.

**Afstemvragen:**
1. Kloven: diepte, breedte (bodem ~10–40, bovenrand ~60–110) en aantal (hoofdkloven om de ~300–500 blokken, plus zijtakken) goed? Meer, minder of breder?
2. Treden in kloofwanden en steilranden: duidelijk en beklimbaar genoeg, of breder/meer treden (minder steil)?
3. Onderste helft van de steilrand (oasekant) is grijze steen met zand/zandsteen erop: zo laten, zandsteen erin, ook lagen, of de biomegrens naar de voet van de steilrand?
4. Overgang ring 1 → plateau (tot ~70 blokken stijging over ~50 blokken): acceptabel, of afzwakken?
5. Kloofsteenlagen: dikte, verhouding (rood 28 / oranje 12 / crème 8 per 48 blokken) en golving goed?
6. Oase: aantal en grootte van de poelen (~5 % water), breedte van de grasoever, aantal palmen (oever + verspreid)?
7. Palm: groter of voller (nu kroon ~7 breed)?
8. Kloofbodem: gebarsten klei en (soms grote) grindvlekken goed?
9. Grotten in het plateau: veel en soms vlak onder het oppervlak (dunne daken), rood zand op grotbodems: goed?
10. Verhouding canyonlands ~59 % / oase ~41 % (tijdelijk tot change 30): goed?
