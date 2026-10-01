# Change 30 — Extra ring 2-biomes: `salt_flats`, `hoodoo_fields`, `dune_sea`

Scope: `worldgen` — change 30

**Doel:** ring 2 (overworld, 1.500 – 3.500) krijgt zijn laatste drie biomes. Elk biome krijgt een eigen terreinvorm en een eigen bodem (surface rules). Het gaat om:
- **`salt_flats`**: kaal, wit en vlak, met zoutkorst;
- **`hoodoo_fields`**: zandsteenpilaren, bogen en balancerende rotsen;
- **`dune_sea`**: hoge, golvende duinen waarin je makkelijk verdwaalt.

Ring 2 heeft daarna zijn vijf biomes (§1.10: 4–5 varianten per ring). Deze change bouwt op het kader van change 23: de twee regio-assen `relief`/`moisture`, de `multi_noise` van ring 2, `height.json`, het lagenblok en `Ring2Biomes`. Hij voegt alleen punten, regiohoogtes, één terreintype (hoodoos) en surface-rule-takken toe. Daarnaast komen de nieuwe biomes in **elke lijst die ring 2-biomes opsomt**: tags, ertsen, zandgrotten, het lagenblok en lavameren/carvers.

Bron: `fase-1-wereld.md` §2 Ring 2 (terrein en sfeer: "hoodoos verticaal, duinen golvend, zoutvlakte vlak"; biometabel; zout-set; zonnesteen "hoog in canyonwanden **en hoodoos**"; zingend zand in de duinenzee), §1.10, bouwplan rij 30 (B, D, W). Plannen: `change-23.md` (overdracht "30 extra biomes"), `change-24.md` (risico "zonnesteen in hoodoos hangt af van change 30"), `change-25.md` ("Voor latere changes": change 30 in `#is_ring_2`), `change-26.md` (afspraak: nieuwe ring 2-biomes in de zandgrot-lijst, met vanilla-carvers en zonder lavameren).

Buiten scope:
- overgangsbiome 2→3 `dry_riverbeds` (change 31);
- lucht, fog en ambient, waaronder het zingende zand in de duinenzee (change 32: `Palette.RING_2` en mood per biome in `Ring2Biomes`);
- definitieve textures (change 37);
- zoutblokken zelf (change 29, `blocks`);
- structures zoals halfbegraven ruïnes en kampen (fase 3);
- zandwormen in de duinenzee (fase 4);
- condities, bijv. een zoutvlakte die extra uitdroogt (`conditions` → backlog).

---

## Afstemming (2026-10-01)

Afgestemd met 23–29 en 31 (basis: de aanbevolen opties). De aannames A3 en A4 en de `floor`-invoer van `HoodooPillars` zijn inline bijgewerkt.
- **Zout-set (29) staat vast:** `salt_crust` vol en niet vallend, `salt_crystal` = `AmethystClusterBlock` met standaard `facing=up`.
- **Ertsen (25):** per gastgesteente, incl. `scorched_sandstone`; zonnesteen Y 95–150 (vol 110–140; doorgerekend op 23 vraag 5 B). De hoodoo-pilaren (vloer Y ~88, toppen tot ~135) en kaprotsen vallen grotendeels in dat bereik; "zonnesteen in hoodoos" (design) klopt bij vraag 6 A.
- **Hoogtes (23 vraag 5 = B, keuze gebruiker):** plateau Y 125–140, kloofbodem ~70, vlaktes Y 66–74. Gevolg hier, verhoudingen behouden: hoodoovloer Y 85–91 (basis 88; ligt op ~30 % tussen vlakte en plateau, zoals eerder 81 tussen 70 en ~107), pilaren 15–45 hoog (toppen tot ~plateauhoogte), kaprotsgrens Y 94, steilrand plateau → hoodoovloer met 2–3 treden. Duinen (basis Y 68, kammen tot ~100) en zoutvlak (Y 66) liggen relatief aan de vlakte en veranderen niet. Horizontale maten (cel 12, straal, bogen) blijven gelijk — afstemwaarden voor de W-achteraf.
- **`Ring2Biomes.ALL` bestaat (23):** alleen drie regels erbij. Lagenblok en zandgrot-tak: op de controlelijst van 23 bijwerken.
- **Technische correctie (geen ontwerpkeuze):** `HoodooPillars.floor` = de echte oppervlaktehoogte (`ring_2/overworld/height`) i.p.v. `hoodoo_floor`. Zo begint een pilaar nooit boven lucht, ook niet waar change 31 een bedding uitsnijdt of in partiële gewichten. `HoodooPillarsTest` toetst dan "verbonden met `floor − 2`" op die invoer.
- **Volgorde `height.json`:** 23 (basis) → deze change (regio's) → 31 (beddingen, op het eindresultaat).
- Teststop: geen (W-achteraf in de gebundelde ring 2-eindcheck, teststop 3 ná change 37, vóór fase 3).

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 17, commit `7714b77`). **Changes 18–29 zijn nog niet gebouwd; hun plannen zijn de bron.** Alle plannen 18–37 bestaan inmiddels en zijn op 2026-10-01 op elkaar afgestemd (zie "Afstemming" hierboven). **Bij de start loop je alle aannames hieronder na tegen `CODEBASE.md`, de gebouwde code en de beantwoorde ontwerpvragen in `docs/plans/change-21.md` … `change-29.md`.** Wijkt een naam af, dan volg je de echte naam en noteer je dat onder "Afwijkingen bij de bouw". Wijkt een *vorm* af (bijv. change 23 koos vraag 1 B of C), dan stop je en vraag je het de hoofdsessie.

- **A1 — ring 2-kader uit change 23** (plan, aanbevolen opties). Change 30 bouwt hier rechtstreeks op:
  - **Regio-assen** `density_function/ring_2/overworld/relief.json` en `moisture.json`: `flat_cache` op de noises `corruptedadventure:ring_2_relief` en `ring_2_moisture`, `y_scale` 0.
  - **Router:** `temperature` → `ring_blend/overworld_temperature` (ring 2 = `relief`), `vegetation` → `ring_blend/overworld_vegetation` (ring 2 = `moisture`).
  - **Preset:** ring 2 = `minecraft:multi_noise` met inline `biomes`. `canyonlands` heeft `temperature` [T, 2] en `oasis_flats` heeft [−2, T]. De andere vijf parameters zijn [−2, 2].
  - **Geplande indeling** (vastgelegd in `fase-1-wereld.md` en de beslissingenlog van change 23):
    - rotsig (relief hoog): `canyonlands` (vochtig/midden) en `hoodoo_fields` (droog);
    - vlak (relief laag): `oasis_flats` (vochtig), `dune_sea` (midden) en `salt_flats` (droog).
  - **Terrein:** `relief_weight.json` (spline op `relief` met terrassen), `flats_height.json` (oase: Y 66–74, poelen tot Y ~59), `plateau_height.json` (Y 125–140; 23 vraag 5 B), `canyon_depth.json` en `height.json`. `height.json` = `flats + relief_weight · (plateau − flats) − relief_weight · canyon_depth`; de kloofbodem en de vlakte buiten de poelen blijven ≥ Y 66.
  - **`terrain.json`** = `(height − y) / S` + 3D-wandruis `ring_2_wall`, alleen bij steile wanden. **`ring_2/overworld.json`** = vanilla's grottenopbouw met `sloped_cheese` → `terrain` (vraag 4 A van change 23). **`ring_blend/overworld_initial.json`**: ring 2 = `0,390625 + terrain`.
  - **Ring gate** (`RingGate`, vraag 3 A van change 23): wikkelt alleen `interpolated`-markers.
  - **Datagen:** `Ring2Biomes` met de gedeelde helper `Ring2Biomes.generation(...)` en `BiomeAmbience.Palette.RING_2`. Tags: `corruptedadventure:is_ring_2` (`ModTags.Biomes.IS_RING_2`), `c:is_hot/overworld` en `c:is_dry/overworld`. **Geen** `minecraft:`-tags.
  - **Surface rules:** de ring 2-takken staan bovenaan `above_preliminary_surface`. Het **kloofsteen-lagenblok** is een apart top-level-element ná `above_preliminary_surface` (`biome_is [canyonlands]`, Y 8 tot het oppervlak). Change 23 zei al: "Change 30 zet `hoodoo_fields` in dezelfde `biome_is`-lijst".
  - **Features:** `Ring2Features`, waarin de placed feature `canyonlands_vegetation` (`dry_shrub`, `barrel_cactus`, `desert_flower`, vanilla dode struik) staat. De heightmap voor features wordt gekozen in de bouwstap van change 23 (verwacht `WORLD_SURFACE_WG`/`OCEAN_FLOOR_WG`). Alles chunk-lokaal.
  - **Spawns** volgens change 23 vraag 11 (A: vanilla-woestijn: husks i.p.v. zombies, konijnen).
- **A2 — blokken uit change 21:** `red_`, `orange_` en `cream_canyon_stone` (tag `canyon_stones`), `scorched_sandstone` (+ `smooth_`, `cut_`), `cracked_clay`, `dry_shrub`, `barrel_cactus`, `desert_flower`, en blok-tag `desert_plant_soil`. Kloofsteen staat **niet** in `#base_stone_overworld`.
- **A3 — zout-set uit change 29** (plan, afgestemd): `salt_crust` (vol blok, valt niet, 29 vraag 2 A), `salt_block`, `polished_salt` en `salt_crystal` (vanilla `AmethystClusterBlock`, standaard-state `facing=up`, licht 29 vraag 5).
  - `salt_crust` is een vol blok (bovenlaag).
  - `salt_crystal` is een kleine decoratieplant/-cluster met zwak licht, die op `salt_crust`/`salt_block` kan staan (`canSurvive`). Is het een gericht blok zoals amethist, dan plaats je het met `facing=up`.
  - Heten de blokken anders of mist er een, volg dan change 29. Ontbreekt de zout-set helemaal, dan gebruik je een minimale placeholder (`minecraft:white_concrete_powder` / `calcite`) plus een backlogregel `[blocks]`.
- **A4 — ertsen uit change 24/25:**
  - Zonnesteen-varianten: `red_`, `orange_` en `cream_canyon_sunstone_ore` en `scorched_sandstone_sunstone_ore` (change 24 vraag 1 A).
  - Change 25 plaatst ze met de biome modifier `add_ring_2_ores` op **`#is_ring_2`**, per gastgesteente een `BlockMatchTest` (incl. `scorched_sandstone` → `scorched_sandstone_sunstone_ore`), Y `trapezoid` 95–150 (vol 110–140, absoluut; afgestemd op de diepe hoogtes van 23), discard 0. Turkoois zit in de onderste kloofsteenlagen (Y 10–40). De hoodoo-pilaren (vloer 85–91, toppen tot ~135) en hun kaprotsen vallen dus grotendeels in het zonnesteenbereik (de onderste ~5–10 blokken van een pilaar liggen onder de aanloop).
  - **Gevolg:** een biome in `#is_ring_2` krijgt automatisch de ring 2-ertsen. Zonnesteen verschijnt alleen waar zijn gastgesteente ligt. Of het scorched-doel in de ore-feature zit, hangt af van de bouw van change 25 (zijn plan noemt `TagMatchTest(canyon_stones)`, change 24 per gastgesteente een `BlockMatchTest`); dat bij de start nalopen.
- **A5 — grotten uit change 26:**
  - De **zandgrot-tak** in de surface rules is een top-level-element vóór het lagenblok, met een `biome_is`-lijst.
  - Afspraak: nieuwe ring 2-biomes komen in die lijst als change 26 vraag 7 A koos. Bij 7 B komt alleen `dune_sea` erbij (zandbiome); bij 7 C is er geen lijst.
  - Nieuwe ring 2-biomes krijgen **vanilla-carvers** en **geen** `lake_lava_*`. Dat zou vanzelf moeten gaan via `Ring2Biomes.generation`; controleren.
- **A6 — conditie uitdroging (change 27):** de oase herkent hij aan de key `oasis_flats` (of een tag van change 23). Die verandert niet: de drie nieuwe biomes koelen niet. Er komt **niets** in `conditions`; ideeën gaan naar de backlog.
- **A7 — ringweer (changes 20/28):** werkt per ring en niet per biome. Niets aan te passen.
- **Valkuilen die hier gelden** (`CODEBASE.md`):
  - feature-volgorde per stap gelijk aan vanilla (anders "Feature order cycle");
  - de generatievolgorde (change 16) en dat features alleen in de 3×3 chunks rond hun herkomst schrijven;
  - `RingLayout.current()` niet in een codec of constructor gebruiken;
  - geen veranderlijke instantiestaat in density functions;
  - `flat_cache` sampelt op kwart-resolutie (4 blokken); zie stap 3;
  - surface rules vervangen alleen `default_block`;
  - een surface-rule-tak die geen blok teruggeeft, valt door naar de volgende tak.

---

## Achtergrond (uitgezocht, niet opnieuw doen)

- **Richting in JSON kan niet.** Vanilla density functions hebben geen x- of z-coördinaat als invoer, en `noise`/`shifted_noise` kennen alleen één `xz_scale`. Uitrekken langs één as of draaien gaat dus niet. Duinen in JSON zijn daarom **richtingloos**: ruggen via `1 − |noise|`, organisch gemaakt met domain warp (`shifted_noise` met een ruis-`shift_x`/`shift_z`). Duinen met een vaste windrichting vragen een eigen Java-type (vraag 4 B).
- **Dunne vormen en interpolatie.** Alles binnen een `interpolated`-marker wordt op celhoeken berekend en daartussen geïnterpoleerd (cellen 4 × 8 × 4 blokken). Een pilaar van 3–6 blokken breed wordt dan een vlek of verdwijnt. Hoodoo-pilaren moeten dus **buiten** de interpolatie, per blok, bij de dichtheid opgeteld worden (`max(interpolated(...), hoodoos)`). `flat_cache` is ook ongeschikt (kwart-resolutie); `cache_2d` is per kolom exact.
- **RingGate en termen buiten markers.** De ring gate van change 23 wikkelt alleen `interpolated`-markers. Een term zonder marker rekent `RingBlend.compute` alleen uit in kolommen waar ring 2 gewicht heeft. Dat kost dus niets buiten ring 2 en vraagt geen gate.
- **Smoothstep-menging met ring 1/3** in de overgangszones kapt positieve dichtheid **van boven af** weg zolang de pilaardichtheid binnen de pilaar overal even groot is (bijv. +1). De ring 1-/ring 3-dichtheid wordt naar boven toe negatiever. Een pilaar wordt dan korter maar gaat nooit zweven. Een kap met een grotere dichtheid dan zijn hals zou wél kunnen gaan zweven: daarom heeft de dichtheid binnen de pilaar één vaste waarde.
- **Een seeded hash zonder seed:** een Java density function kan per cel vaste eigenschappen trekken door een vanilla-`NoiseHolder` (eigen noise-key, door `RandomState` geseed via `visitNoise` in `mapAll`) op het celmidden te samplen. Hij heeft dan geen wereldseed of `RingLayout.current()` nodig en is een pure functie van (seed, positie).
- **De `multi_noise` met uitgeschreven bereiken** verdeelt exact: binnen een bereik is de afstand 0, en een biomegrens valt precies op de drempelwaarde (change 23). Vijf biomes op twee assen geven dus een rechthoekige indeling in (relief, moisture). Op de vochtas grenzen alleen buren aan elkaar. Over de reliëfgrens grenst elk vlak biome aan het rotsige biome met hetzelfde vochtbereik.

---

## Plan

Volgorde = bouwvolgorde. Het plan volgt de aanbevolen opties. Bij een ander antwoord verandert alleen de stap van die vraag.

### 1. Biomeverdeling: vijf punten in de `multi_noise` (vraag 1 A, 2 A)

Drempels: `T` op `relief` (rotsig/vlak, uit change 23, wordt opnieuw gekozen), `H_r` op `moisture` binnen rotsig, en `H_s` < `H_o` op `moisture` binnen vlak. In `corrupted_world.json` (ring 2):

| Biome | `temperature` (relief) | `humidity` (moisture) | Doelaandeel ring 2 (vraag 1 A) |
|---|---|---|---|
| `canyonlands` | [T, 2] | [H_r, 2] | 30 % |
| `hoodoo_fields` | [T, 2] | [−2, H_r] | 20 % |
| `salt_flats` | [−2, T] | [−2, H_s] | 13 % |
| `dune_sea` | [−2, T] | [H_s, H_o] | 25 % |
| `oasis_flats` | [−2, T] | [H_o, 2] | 12 % |

- Rotsig : vlak = 50 : 50. `T` schuift dus t.o.v. change 23 (daar ~55 % canyonlands). De oase krimpt van ~45 % naar ~12 %, zoals change 23 aankondigde.
- **Drempels bepalen:**
  1. Een unit test (`Ring2RegionSharesTest`, stap 7) sampelt de twee noises (parameters uit de JSON, 3 seeds, 200.000 punten op een raster van 8 blokken) en controleert de aandelen bij de gekozen drempels (±3 procentpunt).
  2. De drempels kies je eerst uit de percentielen van dezelfde sampling.
  3. Daarna meet je de aandelen in de wereld via RCON (test S).
- Volgorde op de vochtas (vraag 2 A): zout — duin — oase. De oase ligt in de duinen en raakt het zoutvlak nooit. De hoodoos (droog-rotsig) grenzen aan zoutvlak en duinen, de kloven (vochtig-rotsig) aan duinen en oase.
- Vlekgrootte volgt de `firstOctave` van `ring_2_relief`/`ring_2_moisture` (change 23 vraag 2). De drie vlakke biomes delen één vochtband, dus hun vlekken worden kleiner dan die van de oase nu. Dat meet je in S (gemiddelde vlekgrootte). Is het te klein, dan kies je één octaaf grover voor `ring_2_moisture`. Dat raakt de oase/kloof-verdeling van change 23 alleen in schaal.

### 2. Terrein: regiohoogtes en gewichten (handgeschreven JSON, `density_function/ring_2/overworld/`)

De getallen zijn startwaarden (vraag 3 A, 4 A, 7 A). Je stemt ze af in test S (hoogtes) en W-achteraf (uiterlijk).

| Bestand | Inhoud |
|---|---|
| `hoodoo_weight.json` | `spline` op `moisture`: 1 onder H_r − 0,04, 0 boven H_r + 0,04, met 2–3 vlakke treden (dezelfde terrasopbouw als `relief_weight`: een steilrand van ~35–55 blokken tussen plateau en hoodoovloer) |
| `salt_weight.json` | `spline` op `moisture`: 1 onder H_s − 0,03, 0 boven H_s + 0,03, zonder treden (het zoutvlak loopt geleidelijk op in de duinvoet) |
| `dune_weight.json` | `spline` op `moisture`: 0 → 1 rond H_s, 1 → 0 rond H_o (zelfde breedtes); de oase is de rest (`1 − salt − dune`) |
| `salt_height.json` | Y 66 vlak (constant; vraag 3 A). Geen poelen. Ligt ≥ 66, dus geen zeewater |
| `dune_height.json` | basis Y 68 + `amp · ridge`, met `ridge` = spline op `1 − |shifted_noise(ring_2_dunes, warp ring_2_dune_warp)|` (scherpe kam, brede trog) en `amp` = spline op `ring_2_dune_amplitude`, 10 → 32. Kammen tot Y ~100, troggen Y 66–70. Golflengte ~40–80 blokken |
| `hoodoo_floor.json` | hoodoovloer Y 85–91 (basis 88 + `ring_2_hills` ±3): een uitgesleten "amfitheater" onder het plateau Y 125–140 (vraag 7 A; 23 vraag 5 B) |
| `height.json` (gewijzigd) | `flat = salt_w · salt_h + dune_w · dune_h + (1 − salt_w − dune_w) · flats_height`; `rock = hoodoo_w · hoodoo_floor + (1 − hoodoo_w) · (plateau − canyon_depth)`; `height = flat + relief_weight · (rock − flat)`. Daarna dezelfde klem als change 23 (kloofbodem ≥ 66). `flats_height.json` blijft de oase (naam onveranderd, minder churn) |
| `terrain.json` (gewijzigd) | de 3D-wandruis `ring_2_wall` × `(1 − salt_w − dune_w)` (alleen rotsige en oase-delen). Zandduinen en zoutvlak krijgen dus geen ruwe wanden of overhangen |

- Nieuwe noise-parameters (`worldgen/noise/`): `ring_2_dunes`, `ring_2_dune_warp`, `ring_2_dune_amplitude` en `ring_2_hoodoo_cells` (stap 3). `ring_2_hills` wordt hergebruikt.
- **Overgangen:**
  - Duin → oase/zout: de duinen lopen uit tot de vlakte (`dune_w` → 0).
  - Kloof → hoodoo: de steilrand plateau (Y ~132) → hoodoovloer (Y ~88), met terrassen.
  - Hoodoo → vlak: steilrand hoodoovloer → vlakte (via `relief_weight`).
  - De grootste sprong (plateau ↔ zoutvlak) blijft gelijk aan die van change 23 (plateau ↔ oase).
- `ring_blend/overworld_initial.json` blijft ongewijzigd: hij verwijst naar `terrain`, en dat bevat de nieuwe hoogtes al. De hoodoo-pilaren komen er **niet** in (stap 3).

### 3. Hoodoos: density function `corruptedadventure:hoodoo_pillars` (vraag 5 A, 6 A, 7 A)

Nieuw: `worldgen/densityfunction/HoodooPillars.java`, geregistreerd in `ModDensityFunctionTypes`.

- **Codec:**
  - `weight` (DF, 0–1: `relief_weight · hoodoo_weight`);
  - `floor` (DF: **de echte oppervlaktehoogte** `corruptedadventure:ring_2/overworld/height`, na alle verlagingen; afgestemd 2026-10-01 — met `hoodoo_floor` zouden pilaren boven een bedding van change 31 of in partiële gewichten op `floor − 2` boven lucht kunnen beginnen; `hoodoo_floor` blijft wel de invoer van `height.json`);
  - `noise` (`NoiseHolder`, `corruptedadventure:ring_2_hoodoo_cells`; alleen gebruikt als seeded hash per cel);
  - `cell_size` (12);
  - `chance` (0,6: kans op een pilaar per cel);
  - `min_height` / `max_height` (15 / 45; 23 vraag 5 B: toppen tot ~plateauhoogte);
  - `neck_radius` (1,5–2,5);
  - `cap_chance` (0,35: kans op een balancerende kaprots);
  - `cap_radius` (+1–2 t.o.v. de hals);
  - `arch_chance` (0,03: kans dat een cel een boog krijgt i.p.v. een pilaar).

  Alle balansgetallen staan in de JSON (`ring_2/overworld/hoodoos.json`) en niet in config: een wijziging geeft naden in bestaande werelden, net als bij `radial_falloff`.
- **Vorm per cel** (gejitterd raster; per cel worden de eigenschappen getrokken uit `noise` op het celmidden):
  - **Pilaar:** een cirkelvormige doorsnede met een straal die per hoogte verloopt: brede voet (+1 onderaan), hals en eventueel een kap in de bovenste 3–5 blokken (straal hals + 1–2, afgerond).
  - **Hoogte** = `min + (max − min) · r`, × `weight` (pilaren worden kleiner naar de rand van het biome toe; onder `min_height · 0,5` geen pilaar).
  - **Voet:** de pilaar begint op `floor − 2`, zodat hij in de vloer staat.
  - **Boog** (zeldzaam): een verticale schijf van 3 dik en 10–16 lang in een getrokken richting, met een elliptisch gat. Hoogte 10–16 (ongewijzigd: de boog is aan de celbreedte gebonden). De poten lopen tot `floor` op hun eigen x,z.
  - **Dichtheid:** +1 binnen de vorm, anders −1. Eén vaste waarde, zodat menging in een overgangszone van boven af kapt (zie Achtergrond).
  - **Alles binnen de cel:** pilaar en boog passen binnen de cel minus 1 blok marge. `compute` kijkt alleen naar de eigen cel (en bij een jitter tot over de rand naar 3×3 cellen). Elk blok van een vorm hangt via de vorm aan de voet: **nooit zwevend** (unit test).
- **In de dichtheid:** `ring_2/overworld.json` wordt `max(<bestaande stapel>, corruptedadventure:ring_2/overworld/hoodoos)`, **buiten** `interpolated` (exact per blok, zie Achtergrond). Niet in `initial_density`: het preliminary surface blijft de hoodoovloer (aquifers, surface depth), net als de grotten. `ring_gate` hoeft niets te doen (geen marker).
- **Kosten:**
  - Vroege exits: `weight` == 0 → −1.
  - Y buiten [`floor − 2`, `floor + max_height + 2`] → −1.
  - `floor` en `weight` via `cache_2d` (exact per kolom, geen `flat_cache`).
  - Celgegevens per kolom via een ThreadLocal-cache (zoals `RingColumnCache`), zonder veranderlijke instantiestaat (valkuil).
  - `minValue`/`maxValue` = −1/+1.
  - Meten in S (JFR, budget 1,3× vanilla).
- **Materiaal (vraag 6 A):** komt uit de surface rules (stap 4). De pilaren zelf zijn `default_block` (steen) en worden zo kloofsteenlagen met een kaprots van geschroeide zandsteen. Zonnesteen (change 25, Y 95–150) komt dan vanzelf in de pilaarwanden.
- **Carvers** kunnen pilaren aansnijden (vanilla-gedrag; geaccepteerd). Noise-grotten niet: `max` komt ná de grotten.

### 4. Surface rules (`noise_settings/overworld.json`)

Bovenaan `above_preliminary_surface`, ná de takken van change 23 (`withered_expanse`, `canyonlands`, `oasis_flats`). **Elke tak geeft voor `stone_depth floor` altijd zelf een blok terug**, of laat bewust door naar het lagenblok. Anders vallen ze door naar vanilla's standaard (gras/aarde). Bij de bouw nagaan hoe change 23 dat voor de steile kloofwanden heeft opgelost, en hetzelfde doen.

- **`salt_flats`** (vraag 3 A):
  - vloer → `salt_crust`;
  - `add_surface_depth` (2–4 diep, `secondary_depth_range` 2) → `salt_block`;
  - daaronder 3 blokken `sandstone`, daarna steen;
  - `steep` (alleen aan de randen tegen hoodoo-steilranden) → `sandstone`;
  - rand naar de duinen: vlekken `sand` via vanilla's surface-ruis (`minecraft:surface`, drempel ~0,4) zodat de korst niet als een lijn ophoudt.
- **`dune_sea`** (vraag 4 A):
  - vloer en `steep` → `sand`;
  - `add_surface_depth` met `secondary_depth_range` 6 → `sand`;
  - daaronder `sandstone` (de kern van de duin, zoals vanilla-woestijn).
  - Een duin is dus niet tot de basis toe zand. Dat beperkt de hoeveelheid hangend zand boven grotten (change 26, kettingreacties).
- **`hoodoo_fields`** (vraag 6 A):
  - **kaprots:** `stone_depth floor` + `y_above` absoluut 94 (startwaarde: boven de hoodoovloer, Y ≤ 91 + marge) → `scorched_sandstone` (2–3 diep, `add_surface_depth`). Dat zijn de pilaartoppen en de bovenkant van bogen;
  - **onderkant van kappen en bogen:** `stone_depth ceiling` (offset 0, alleen `above_preliminary_surface`) → `scorched_sandstone`;
  - **vloer** (onder Y 94): `red_sand`, met vlekken `cracked_clay` en `gravel` via surface-ruis (zoals de kloofbodem van change 23);
  - **onder de vloer:** `red_sandstone` (1–3), daarna valt het door naar het lagenblok;
  - **`steep` (pilaarwanden):** bewust geen blok, zodat het doorvalt naar het lagenblok (zie de waarschuwing hierboven).
- **Lagenblok** (top-level, change 23): `biome_is` → `[canyonlands, hoodoo_fields]`. De pilaren en de ondergrond van het hoodooveld worden gelaagde kloofsteen van Y 8 tot het oppervlak, met dezelfde vaste banden als in de kloven. Daardoor lopen de lagen door van de kloofwand naar de pilaren.
- **Zandgrot-tak** (top-level, change 26): `biome_is` aanvullen volgens change 26 vraag 7 (A: alle drie; B: alleen `dune_sea`; C: geen lijst).
- De rijen JSON verander je met de hand. Alleen als change 23 het lagenblok met een script maakte, draai je dat script opnieuw.

### 5. Biomes (datagen): `Ring2Biomes`

- **`ModBiomes`:** `SALT_FLATS`, `HOODOO_FIELDS` en `DUNE_SEA`.
- **`Ring2Biomes.ALL`** (bestaat sinds change 23; hier drie regels erbij): de lijst met alle ring 2-biome-keys, en **de enige plek** waar ring 2-biomes opgesomd worden voor de tags (`is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld`) en `Ring2BiomesTest`. Change 31 en latere changes voegen er één regel aan toe.
- Alle drie via `Ring2Biomes.generation(...)`: vanilla-volgorde, de ertsset van change 25 (niet-ertsen uit `addDefaultOres`), vanilla-carvers en geen lavameren (change 26), soft disks. Eigen features achteraan hun stap.
- **Klimaat:** geen neerslag, temperatuur 2,0, downfall 0 (alle drie).
- **Kleuren:** lucht, fog en water = `Palette.RING_2` (change 32 maakt ze definitief). Gras/blad (komt weinig voor): `salt_flats` `C9C08A`/`B5AC6E` (vaal), `dune_sea` en `hoodoo_fields` = de canyonlands-kleuren `BFB755`/`AEA42A`.
- **Mood:** geen eigen mood in deze change. Het zingende zand van `dune_sea` is change 32 (overdracht).
- **Features (vraag 8 A)** (`Ring2Features`, placed features chunk-lokaal: `count`/`rarity_filter` → `in_square` → heightmap van change 23 → `block_predicate_filter`/`would_survive` → `biome`):
  - `salt_flats_crystals`: configured `salt_crystal` (`simple_block`, A3). `rarity_filter` 2 → `count` 2. Eronder `salt_crust`, op de plek lucht. Zeldzame glinsteringen; 's nachts zwak licht. Stap `VEGETAL_DECORATION`.
  - `dune_sea_vegetation`: `dry_shrub` / vanilla dode struik, ~0,2 per chunk. Geen cactus, geen bloemen: kaal, zonder herkenningspunten ("verdwalen").
  - `hoodoo_fields_vegetation`: dezelfde configured feature als `canyonlands_vegetation`, met een lager aantal (~1,5 per chunk) in een eigen placed feature. Hij staat in de stap op dezelfde plek t.o.v. vanilla (geen "Feature order cycle").
  - Geen `random_patch`, `count_on_every_layer` of `environment_scan`: die lezen buurchunks en hangen dus af van de generatievolgorde.
- **Spawns (vraag 8 A):** `dune_sea` en `hoodoo_fields` = de spawns van `canyonlands` (antwoord change 23 vraag 11). `salt_flats` = alleen de monsters, geen dieren (kaal, geen voedsel).
- **Tags** (`ModBiomeTagsProvider`, via `Ring2Biomes.ALL`): alle drie in `is_ring_2`, `c:is_hot/overworld` en `c:is_dry/overworld`. Geen `minecraft:`-tags, dus geen vanilla-structures (fase 3).
  - Gevolg via `#is_ring_2`: `add_ring_2_ores` (change 25) geeft ook deze biomes de ring 2-ertsen. Zonnesteen landt alleen in kloofsteen/scorched sandstone, dus in de hoodoos en niet in duinen of zoutvlak.
- **Lang:** `biome.corruptedadventure.salt_flats` "Salt Flats" / "Zoutvlakte", `hoodoo_fields` "Hoodoo Fields" / "Hoodoovelden", `dune_sea` "Dune Sea" / "Duinenzee".

### 6. Alle lijsten die ring 2-biomes opsommen (controlelijst bij de bouw)

Loop de lijsten hieronder na met `Grep` op `canyonlands` en `oasis_flats` in `src/` (handgeschreven JSON en Java), plus de lijst in `CODEBASE.md`. Elke treffer krijgt de nieuwe biomes, of een vastgelegde reden waarom niet:

| Plek | Actie |
|---|---|
| `corrupted_world.json` ring 2-`multi_noise` | stap 1 |
| `ModBiomes`, `Ring2Biomes(.ALL)`, lang en/nl | stap 5 |
| Tags `is_ring_2`, `c:is_hot/overworld`, `c:is_dry/overworld` | stap 5 (via `ALL`) |
| `add_ring_2_ores` (change 25) | geen wijziging (werkt op `#is_ring_2`); controleren in D |
| Lagenblok (`biome_is`) | + `hoodoo_fields` |
| Zandgrot-tak (`biome_is`, change 26) | volgens change 26 vraag 7 |
| Surface-takken per biome | stap 4 |
| Lavameren/carvers (change 26) | via `Ring2Biomes.generation`; controleren in D |
| `Ring2BiomesTest` | via `ALL` |
| Oase-regel uitdroging (change 27) | **niet** wijzigen (alleen `oasis_flats` koelt) |
| `canyonlands_vegetation` e.d. met een `biome`-filter | geen wijziging (de filter gebruikt de biome-featurelijst) |

### 7. Unit tests (`./gradlew build`)

- **`worldgen/densityfunction/HoodooPillarsTest`:**
  - **Nooit zwevend:** voor 2.000 cellen (10 seeds) is elk vast blok van de vorm via vaste buren verbonden met `floor − 2` (flood fill in de bounding box), ook bij `weight` 0,3, 0,6 en 1,0.
  - **Binnen de cel:** de vorm (+1 marge) blijft binnen de cel.
  - **Hoogtes:** tussen `min · weight` en `max · weight`.
  - **Kaprots:** de kap is breder dan de hals; de hals is nooit 0.
  - **Bogen:** het gat is open, de poten raken de vloer.
  - **Pure functie:** dezelfde invoer geeft dezelfde uitvoer, ook vanuit twee threads. Een nieuwe instantie (`mapAll`) geeft dezelfde waarden.
  - **Codec:** rondreis; fouten bij `min_height` > `max_height` of `chance` buiten [0, 1].
  - `minValue`/`maxValue`.
- **`worldgen/biome/Ring2BiomesTest` (uitbreiden):**
  - alle 5 in `ALL`, met neerslag, temperatuur en palette zoals in stap 5;
  - de tags komen uit `ALL`;
  - geen `minecraft:`-tag;
  - geen `lake_lava_*` en wel vanilla-carvers (als change 26 dat in de helper zette).
- **`worldgen/biome/Ring2RegionSharesTest`** (nieuw): de noises van `relief`/`moisture` uit de JSON-parameters (`NormalNoise` met 3 seeds, 200.000 punten). De aandelen bij de drempels uit de preset liggen binnen ±3 pp van het doel van vraag 1. Zo merk je meteen dat aandelen verschuiven als iemand later de noise aanpast.

### 8. Documentatie

- **`CODEBASE.md`:**
  - **Klassen:** `HoodooPillars` (mappenstructuur en tabel Belangrijke classes) en `Ring2Biomes.ALL`.
  - **Registries:** `hoodoo_pillars`, de biomes-tabel (3 nieuw) en de placed/configured features.
  - **Databestanden:** `ring_2/overworld/*` (nieuw/gewijzigd), noises, preset (5 punten) en surface rules (takken, lagenblok, zandgrotlijst).
  - **Valkuilen:**
    - richting kan niet in JSON;
    - dunne vormen buiten `interpolated` (en niet in `flat_cache`);
    - een vaste dichtheid binnen een vorm voorkomt zwevende delen bij menging;
    - een `NoiseHolder` als seeded cel-hash;
    - de surface-tak moet de vloer afmaken.
  - Meetresultaten (aandelen, hoogtes, performance) en de kopregel.
- **`fase-1-wereld.md` §2 Ring 2:** **Uitwerking (change 30)** met:
  - de indeling op de twee assen (tabel uit stap 1, met de echte drempels);
  - hoogtes per biome;
  - de duinvorm, de hoodoo-vorm en -materialen, en de zoutvlakte;
  - features en spawns;
  - de randvoorwaarde voor zonnesteen in de hoodoos (vervult change 24, risico 2).
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:**
  - de change 25-regel "change 30: `hoodoo_fields` in `#is_ring_2` …" weghalen (afgehandeld);
  - `[conditions] Zoutvlakte: uitdroging sneller (weerkaatsing/zout)? Nu gelijk aan de rest van ring 2. (bron: change 30)`;
  - `[atmosphere] Duinenzee: zandstorm dichter/vaker dan elders in ring 2 (verdwalen), naast het zingende zand van change 32. (bron: change 30)`;
  - `[structures] Hoodoovelden: klifwoningen/uitkijkposten in pilaren; duinenzee: halfbegraven ruïnes (bestaande ring 2-regel aanvullen). (bron: change 30)`;
  - `[worldgen] Change 31: in de zone 2→3 mengt het terrein ook duinen en hoodoo-pilaren met ring 3 (pilaren worden van boven af gekapt); `dry_riverbeds` bepaalt zijn eigen zonevorm. (bron: change 30)`;
  - bij vraag 4 A: `[worldgen] Duinen met windrichting (Java-type `dune_ridges`) als de richtingloze duinen niet overtuigen. (bron: change 30)`.

---

## Overdracht naar latere changes

| Change | Wat er klaarligt |
|---|---|
| 31 `dry_riverbeds` | `Ring2Biomes.ALL` (één regel erbij); de zandgrotlijst en het lagenblok staan op één plek. In de zone 2→3 liggen ring 2-duinen en -pilaren onder de menging. `cracked_clay` op de hoodoovloer als voorbode |
| 32 sfeer | het zingende zand: mood/ambient voor `dune_sea` in `Ring2Biomes`; `Palette.RING_2` voor alle vijf |
| 37 textures | `salt_crust` (zoutkorst-polygonen in de texture), scorched sandstone als kaprots, zand-/rood-zandvloer |
| Fase 3 | de hoodoovelden en duinenzee zonder `has_structure/*`; ruïnes/kampen kiezen hun biomes zelf |
| Fase 4 | zandwormen: `dune_sea` als spawnbiome (backlog) |

---

## Client/server-impact

- **Puur worldgen, server-side en deterministisch per seed.**
  - Alle ruis is geseed via noise-keys.
  - `HoodooPillars` trekt celeigenschappen uit een geseede `NoiseHolder`. Het is een pure functie van (seed, x, y, z): geen `RingLayout.current()`, geen gedeelde staat (ThreadLocal-cache).
  - Features zijn chunk-lokaal, met de heightmap van change 23.
  - Geen netwerk, geen SavedData/attachments en geen client-code.
- **Common registry:** het density-function-type `hoodoo_pillars` (worldgen-registry, niet gesynct; de datapack-JSON verwijst ernaar). Een dedicated server laadt geen client-class.
- **Wat de client krijgt:** biomes gaan via vanilla naar de client (kleuren, geen neerslag). `salt_crystal`-licht is blokgedrag van change 29.
- **Bestaande testwerelden:** de ring 2-indeling verandert (nieuwe punten, ander `T`). Op de rand tussen oude en nieuwe chunks in ring 2 ontstaan naden. Ring 1 en de router blijven gelijk. Maak dus verse werelden voor S en W.

---

## Tests

Bouwplan: **B, D, W**. Aanbevolen ook **S**: preset, surface rules en het nieuwe DF-type worden pas bij het laden gecontroleerd, en alle metingen lopen via RCON.
- Testseed `20260930`, per wereld een nieuwe `level-name` (`smoke-change30-…`).
- Werkwijze uit `CODEBASE.md`: `forceload` ≤ 256 chunks, `execute if loaded` pollen, `execute if biome` alleen in geladen chunks, `fill … replace` met glas als teller.
- Daarna RCON weer uit. Destructieve tellingen alleen in wegwerpwerelden.

### B — `./gradlew build`
- [ ] `HoodooPillarsTest`, `Ring2BiomesTest` (uitgebreid) en `Ring2RegionSharesTest` zijn groen. Bestaande tests (`RingBlendTest`, `RingGateTest`, `RingBiomeSourceTest`) blijven groen.

### D — `./gradlew runData`
- [ ] Gegenereerd en gecommit:
  - `salt_flats.json`, `hoodoo_fields.json`, `dune_sea.json`: carvers aanwezig; geen `lake_lava_*` (A5); geen vanilla `ore_*` (change 25);
  - de placed/configured features;
  - tags `is_ring_2` (5 biomes), `c:is_hot/overworld` en `c:is_dry/overworld`;
  - lang en/nl.
- [ ] `add_ring_2_ores` is ongewijzigd en werkt op `#is_ring_2`.

### S — server + RCON (geautomatiseerd)
- [ ] **Start:** `tools/server-smoke-test.sh` start zonder fouten: geen "Feature order cycle", preset en codecs (`hoodoo_pillars`, `multi_noise`) laden, geen client-class-melding.
- [ ] **Biomes vindbaar:** `locate biome` voor elk van de 5 ring 2-biomes vanaf de `tpring 2`-punten in 4 richtingen → gevonden binnen ring 2.
- [ ] **Aandelen:** langs de cirkels r = 2.000, 2.500 en 3.000, elke 32 blokken, `execute if biome` op Y 80. Per biome het doel van vraag 1 ± 5 pp. Gemiddelde vlekgrootte (aaneengesloten punten op de cirkel) per biome noteren. Buurrelaties: `salt_flats` grenst nooit direct aan `oasis_flats` (vraag 2 A).
- [ ] **Hoogtes** (100 kolommen per biome, `positioned over world_surface` + marker):
  - `salt_flats`: Y 66 (±1);
  - `dune_sea`: Y 66–102, met minstens 20 % van de kolommen ≥ Y 85 ("hoog");
  - `hoodoo_fields`: vloer Y 83–93 (`ocean_floor`-mediaan over een 16×16-raster), met in elke hoodoo-chunk kolommen ≥ vloer + 15 (pilaren).
  - `canyonlands` (controle 23 vraag 5 B): plateau Y 123–142, kloofbodem Y 66–76. Aandeel pilaarkolommen ≈ het doel van vraag 7 (startwaarde 8–15 %).
- [ ] **Geen oppervlaktewater** in `salt_flats`, `dune_sea` en `hoodoo_fields`: `fill <chunk Y 60–140> glass replace water` geeft 0 boven Y 63 in 8 chunks per biome. Water onder 63 in grotten is toegestaan (change 26).
- [ ] **Materialen** (per biome 8 chunks, Y 40–145; telling = blokken):
  - `salt_crust` > 0 alleen in zoutchunks, `salt_block` eronder;
  - `sand` > 0 in duinchunks;
  - in hoodoo-chunks `red_canyon_stone` + `orange_` + `cream_` > 0 (lagen in de pilaren), `scorched_sandstone` > 0 (kappen) en `red_sand` > 0;
  - in zout- en duinchunks kloofsteen = 0.
- [ ] **Zonnesteen in hoodoos** (als change 25 gebouwd is): `#c:ores/sunstone` (of de 4 varianten) in 16 hoodoo-chunks, Y 85–150 → > 0. In zout/duin = 0.
- [ ] **Ring 1 ongewijzigd:** de router is niet aangeraakt. `execute if biome` op 300 ring 1-punten en blokkentellingen in 6 ring 1-chunks zijn identiek aan een wereld van vóór change 30.
- [ ] **Determinisme / geen buurchunk-afhankelijkheid:** twee verse werelden met dezelfde seed, de tweede in **omgekeerde laadvolgorde** (zoals change 16). Vergelijk per chunk (8 chunks per nieuw biome + 8 in zone 1→2) de tellingen van `salt_crust`, `salt_crystal`, `sand`, `scorched_sandstone`, de drie kloofsteentinten, `dry_shrub`, `dead_bush`, `barrel_cactus` en `desert_flower`, en een hoogte-hash (som van 64 `world_surface`-hoogtes) → **identiek**. Elk verschil is een bug in een placement, of gedocumenteerd vanilla-restgedrag.
- [ ] **Overgangszones:** in 4 chunks met hoodoos in de zone 1→2 of 2→3 (via `tpboundary` + `locate`) hebben de pilaren geen vast blok met lucht eronder én erboven in de bovenste 3 blokken van een pilaarkolom. Grove RCON-scan per kolom; zwevende brokken controleer je verder in W.
- [ ] **Performance** (JFR `minecraft.ChunkGeneration`, stap `noise`, 16×16 chunks, tweede ronde telt): midden in `hoodoo_fields`, `dune_sea` en `canyonlands`, vóór en na change 30, en de vanilla-preset. Budget ≤ 1,3× vanilla; getallen in `CODEBASE.md`.

### W — blokkerende handmatige test: **nee** → W-achteraf

**Reden:** alles waar latere changes op bouwen, is automatisch te toetsen via B, D en S:
- biome-keys, tags en lijsten (31: `Ring2Biomes.ALL`; 32: de `dune_sea`-key);
- aandelen, hoogtebereiken en materialen;
- zonnesteen in de hoodoos, droog oppervlak, ring 1 ongewijzigd en determinisme;
- dat pilaren nooit zweven (unit test op de vorm).

Wat overblijft, is het uiterlijk: hoe de duinen golven, of de hoodoos als hoodoos lezen, en de kaprotsen en bogen. Dat zijn **afstemgetallen in de JSON** van deze change. Geen latere fase 1-change bouwt op de exacte vorm: change 31 maakt zijn eigen zonevorm, 32 is ambient en 37 vervangt alleen pixels. Advies: doe de W-achteraf vóór change 37 en vóór fase 3 (structures in deze biomes).

W-achteraf (runClient, testseed, ~15 min):
- [ ] **Hoodoos:** `locate biome corruptedadventure:hoodoo_fields`. De pilaren lezen als hoodoos: lagen rood/oranje/crème, met donkere kaprots. Er zijn balancerende kappen en af en toe een boog. Geen zwevende brokken, ook niet in de overgangszones. Zonnesteen is zichtbaar in pilaarwanden.
- [ ] **Duinenzee:** hoge, golvende duinen; zonder kompas raak je de weg kwijt; kaal. Geen zandwanden of zwevend zand aan het oppervlak.
- [ ] **Zoutvlakte:** wit en vlak tot de horizon. Een geleidelijke rand naar de duinen, steile randen tegen de hoodoo-steilrand. Zoutkristallen glinsteren 's nachts.
- [ ] **Overgangen:** kloof → hoodoo (terrassen), duin → oase (duinen lopen uit op de oasevlakte), duin → zout. `/ca tpboundary 1` en `/ca tpboundary 2`: geen verticale wanden of afgekapte pilaren die zweven.
- [ ] **F3:** de biome klopt bij het terrein; geen regen.
- [ ] **Spawns** volgens vraag 8 ('s nachts husks in duinen en hoodoos, geen dieren op het zout).
- [ ] **Zandgrot onder een duin** (als change 26 `dune_sea` meeneemt): het plafond van los zand (`loose_sand`, 26 vraag 5 B) stort in — ook als je eronder loopt — zonder dat een hele duin wegzakt.
- [ ] **Standaard-W:** midden van elke ring en elke grens (overworld en Nether), en een portal vanuit ring 2 (zoutvlakte of duinen) naar de Nether en terug.
- [ ] Geen fouten of waarschuwingen in de log bij het aanmaken van de wereld.

---

## Risico's / aandachtspunten

- **Change 23 is niet gebouwd** (A1). Bestandsnamen, de vorm van `height.json`, de keuze van de heightmap en het lagenblok kunnen afwijken. Koos change 23 vraag 1 B of C (geen router-assen), dan klopt stap 1 niet en vraag je het eerst de hoofdsessie.
- **De oase krimpt** van ~45 % naar ~12 %. Tests of verwachtingen van changes 27/28 die op "veel oase" rekenden (bijv. `locate biome oasis_flats` binnen bereik), meet je na; `locate` heeft genoeg bereik.
- **`T` verschuift:** de grenzen tussen canyonlands en de vlakke biomes liggen anders dan in change 23. Bestaande ring 2-testwerelden krijgen naden; ring 1 niet.
- **Pilaren per blok rekenen** is duurder dan geïnterpoleerde termen. Daarom de vroege exits en de kolomcache. Meten in S; zit het boven het budget, dan maak je `cell_size` groter of `chance` lager, of beperk je de y-band.
- **Hangend zand:** duinen zijn zand op een zandstenen kern. Carvers onder duinen geven hangend zand, zoals in vanilla-woestijnen. Grote kettingreacties beperkt de kerndiepte (`secondary_depth_range` 6).
- **De surface-tak valt door naar vanilla:** een `steep`-geval zonder blok krijgt vanilla-gras of -aarde, als vanilla's standaard eerder komt dan het lagenblok. Controle in S (geen `grass_block` in hoodoo-chunks boven Y 94).
- **Een vaste Y-grens voor de kaprots (94):** een lage pilaar op een hoge vloer kan een rode-zandtop krijgen, en een heuvel op de vloer boven 94 een kaprots. Stem af met de hoodoovloer (Y 85–91) en `min_height` (15).
- **Hoge, slanke pilaren** (23 vraag 5 B): pilaren tot 45 hoog met hals 1,5–2,5 kunnen als naalden lezen; afstemmen (straal of `max_height`) in de W-achteraf.
- **Feature order cycle:** `hoodoo_fields_vegetation` hergebruikt de configured feature in een eigen placed feature; nieuwe placed features achteraan hun stap (S vangt het).
- **De zout-set ontbreekt of heet anders** (A3): placeholder + backlog, geen eigen blokken in deze change.
- **Zonnesteen in de kaprots** hangt af van het scorched-doel in de ore-feature van change 25 (A4). Zonder dat doel zit zonnesteen alleen in de kloofsteenlagen van de pilaren. Dat is ook goed: "hoog in hoodoos" klopt dan nog steeds.

---

## Ontwerpvragen

Ronde 1 = verdeling en bouwwijze (1, 2, 4, 5). Ronde 2 = uiterlijk en leven (3, 6, 7, 8).

### 1. Hoe groot is elk ring 2-biome?
Aandeel van het ring 2-oppervlak. De vlekgrootte blijft die van change 23 vraag 2; de drie vlakke biomes delen één vochtband.
- **A. Kloven 30 %, hoodoos 20 %, duinen 25 %, zout 13 %, oase 12 % (aanbevolen).** De kloven blijven het kernbiome en de duinen voelen als een "zee". De oase is zeldzaam genoeg om een vondst te zijn ("het enige veilige water"), maar ligt binnen een paar honderd blokken.
- **B. Alle vijf ~20 %.** Eenvoudig en evenwichtig. Wel veel oase en dus water: de uitdroging (change 27) wordt makkelijker, en kernbiomes en "extra" biomes zijn niet meer te onderscheiden.
- **C. Kernbiomes dominant: kloven 40 %, oase 20 %, hoodoos 15 %, duinen 15 %, zout 10 %.** Dicht bij change 23. De nieuwe biomes zijn kleine bijzonderheden; de duinenzee is te klein om in te verdwalen.

Antwoord: **A** — kloven 30 %, hoodoos 20 %, duinen 25 %, zout 13 %, oase 12 %.

### 2. Welke biomes grenzen aan elkaar? (volgorde op de vochtas)
De vlakke biomes liggen op een rij langs `moisture`; alleen buren raken elkaar.
- **A. Zout — duin — oase (aanbevolen).** Een oase ligt altijd in de duinen (klassiek woestijnbeeld) en het zoutvlak ligt aan de droogste kant. De droge hoodoos grenzen aan zout en duinen. Een speler die de oase zoekt, moet door de duinen.
- **B. Duin — zout — oase.** Het zoutvlak ligt tussen duinen en oase, als opgedroogde oase-rand ("waar het water was"). Verhaalmatig sterk (gestolen water), maar de oase ligt dan aan een kale witte vlakte in plaats van tussen de duinen.
- **C. Zout — oase — duin.** De oase ligt in het midden van de vochtas en grenst aan beide. Er is meer contrast, maar de duinenzee raakt het zoutvlak nooit.

Antwoord: **A** — zout — duin — oase op de vochtas.

### 3. Hoe ziet de zoutvlakte eruit?
- **A. Kurkdroog en volledig vlak op Y 66: zoutkorst, daaronder 2–4 zoutblok, dan zandsteen; zeldzame zoutkristallen (aanbevolen).** "Kaal, wit en vlak" letterlijk. Geen water, dus geen tweede bron naast de oase. De polygonen van de zoutkorst zitten in de texture (change 37). Puur JSON.
- **B. Als A, met ondiepe pekelplassen** (de vlakte zakt plaatselijk onder Y 63, zoals de oasepoelen). Mooi spiegelend, maar het is vanilla-water: de uitdroging telt het als koelend (change 27) en het wordt een tweede waterbron. Dat botst met "de oase is het enige veilige water", tenzij `conditions` het later uitzondert (backlog).
- **C. Als A, met een dikke zoutlaag tot ~Y 40** (mijnbare zoutvoorraad). Het zout wordt een grondstof; bij een recept-doel in fase 2 is dit handig, nu vooral meer blokken onder de grond.

Antwoord: **A** — kurkdroog en vlak op Y 66: zoutkorst, 2–4 zoutblok, dan zandsteen; zeldzame zoutkristallen.

### 4. Hoe ontstaan de duinen?
- **A. JSON: richtingloze ruggen (`1 − |noise|` met domain warp), basis Y 68, kammen tot Y ~100, hoogte wisselend per gebied (aanbevolen).** Geen Java. Golvend en hoog, en de duinen lijken overal op elkaar (verdwalen). Nadeel: geen vaste windrichting, en de kammen lopen alle kanten op (organisch, minder "echt").
- **B. Eigen Java-type `dune_ridges` met een vaste windrichting per wereld** (dwarsduinen met een steile lijzijde). Realistischer en leesbaar als "zee"; de windrichting kan aansluiten op de zandstorm (change 28). Wel een extra DF-type, een codec en tests.
- **C. JSON, lager en zachter: kammen 5–15 boven de basis.** Rustig glooiend, makkelijk te doorkruisen; geen "hoge duinen" en minder verdwaalgevoel.

Antwoord: **A** — JSON: richtingloze ruggen met domain warp, basis Y 68, kammen tot Y ~100 (relatief aan de vlakte, die bij 23 vraag 5 B ongewijzigd Y 66–74 blijft).

### 5. Hoe bouwen we de hoodoos?
- **A. Eigen density function `hoodoo_pillars` (Java): gejitterd raster met per cel een pilaar (brede voet, hals, soms een balancerende kap) of zelden een boog; per blok, buiten de interpolatie (aanbevolen).** Alle drie de elementen uit het design. Pilaren zweven aantoonbaar nooit (unit test), en het is een pure functie van seed en positie: geen buurchunk-afhankelijkheid. Lagen en zonnesteen komen vanzelf mee via de surface rules en de ertsen. Wel één nieuwe Java-class met tests, en per-blok-rekenwerk (meten).
- **B. Alleen JSON: pilaren als drempel op een fijne 2D-ruis, recht omhoog; kappen en bogen naar de backlog.** Geen Java. Kappen kunnen in JSON gaan zweven, dus die laat je weg. De pilaren worden vlekkerig van vorm (ruisvlekken, geen ronde zuilen), zeker binnen de interpolatie.
- **C. Feature (Java) die pilaren en bogen op het oppervlak zet.** Vrije vorm, maar max. ~40 blokken (3×3 chunks). De feature leest het oppervlak, en buurchunk-features (zie generatievolgorde) maken dat volgorde-afhankelijk. De lagen moet hij zelf kleuren, en zonnesteen zit niet in pilaren die na de ertsen van de buurchunk komen. Afgeraden.

Antwoord: **A** — eigen density function `hoodoo_pillars` (Java), per blok buiten de interpolatie.

### 6. Waarvan zijn de hoodoos gemaakt?
Design: "zandsteenpilaren"; zonnesteen "hoog in canyonwanden en hoodoos" (change 24 vraag 1 A: varianten in de drie kloofsteentinten en geschroeide zandsteen).
- **A. Kloofsteenlagen (hetzelfde lagenblok als de kloven) met een kaprots van geschroeide zandsteen (aanbevolen).** Het klassieke hoodoobeeld: gelaagd, met een harde donkere kap. De lagen lopen door van de kloofwand naar de pilaren. Zonnesteen komt vanzelf in alle pilaren, zonder nieuwe blokken.
- **B. Volledig geschroeide zandsteen.** Dichtst bij "zandsteenpilaren". Eén bruine tint, minder contrast met de kloven. Zonnesteen alleen als de scorched-variant in de ore-feature van change 25 zit.
- **C. Vanilla (rode) zandsteen.** Vertrouwd en letterlijk "zandsteen", maar er komt **geen zonnesteen** in de hoodoos (er is geen variant). Dat botst met het design, tenzij er een extra `blocks`-change komt (backlog).

Antwoord: **A** — kloofsteenlagen met een kaprots van geschroeide zandsteen.

### 7. Hoe groot en hoe dicht staan de hoodoos?
- **A. Vloer Y ~78–84 (een amfitheater onder het plateau), pilaren 10–30 hoog (toppen tot ~plateauhoogte) — na 23 vraag 5 B doorgerekend naar vloer Y ~85–91, pilaren 15–45, radius 2–4, één per cel van 12 blokken met kans 0,6, 35 % met een kap, af en toe een boog (aanbevolen).** Leest als een uitgesleten plateau en is goed doorloopbaar. Er zijn genoeg pilaren voor de zonnesteen.
- **B. Weinig maar reusachtig: pilaren 25–45 hoog, cel 24, radius 3–6.** Spectaculaire monolieten tot boven het plateau. Ze vallen minder op als "veld" en zijn duurder per blok (hogere y-band).
- **C. Een dicht "bos" van dunne pilaren: 8–20 hoog, cel 8, radius 1–2.** Doolhofgevoel, maar het leest snel als ruis en is lastig te doorlopen (en straks voor mobs, fase 4).

Antwoord: **A** (aangepast aan de diepe hoogtes van 23 vraag 5 B, verhoudingen behouden) — vloer Y ~85–91 (amfitheater onder het plateau Y 125–140), pilaren 15–45 hoog (toppen tot ~plateauhoogte), radius 2–4, één per cel van 12 met kans 0,6, 35 % met kap, af en toe een boog.

### 8. Wat leeft en groeit er in de drie biomes?
- **A. Zoutvlakte: alleen zeldzame zoutkristallen, alleen monsters. Duinenzee: vrijwel kaal (af en toe een dorre of dode struik), spawns als de kloven. Hoodoovelden: de kloofflora wat dunner, spawns als de kloven (aanbevolen).** Elk biome heeft een eigen karakter: dood zout, lege duinen zonder herkenningspunten, rotsflora tussen de pilaren. Weinig nieuwe features.
- **B. Levendiger: cactussen en dode struiken in de duinen (zoals vanilla-woestijn), meer zoutkristallen, konijnen overal.** Herkenbaarder en meer eten, maar de duinen krijgen herkenningspunten (minder verdwalen) en het zout voelt minder dood.
- **C. Helemaal kaal en zonder spawns in de drie nieuwe biomes tot fase 4.** Rustig en leeg; 's nachts gebeurt er niets, dus het voelt onaf.

Antwoord: **A** — zout: zeldzame kristallen, alleen monsters; duinen: vrijwel kaal, spawns als de kloven; hoodoos: dunnere kloofflora, spawns als de kloven.

---

## Beslissingenlog-regels (datum = bouwdatum; invullen na de antwoorden)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-01 | worldgen | Aandelen ring 2: kloven 30 %, hoodoos 20 %, duinen 25 %, zout 13 %, oase 12 %; vijf punten in de ring 2-`multi_noise` op `relief` × `moisture` (drempels T, H_r, H_s, H_o) | Kloven als kern, duinen als zee, oase zeldzaam; het kader van change 23, zonder nieuwe Java voor de verdeling |
| 2026-10-01 | worldgen | Buren op de vochtas: zout — duin — oase | Oase ligt in de duinen; zout aan de droogste kant |
| 2026-10-01 | worldgen | Zoutvlakte: kurkdroog en vlak op Y 66 (korst, 2–4 zoutblok, zandsteen), zeldzame kristallen | "Kaal, wit en vlak"; geen tweede waterbron |
| 2026-10-01 | worldgen | Duinen: JSON, richtingloze ruggen met domain warp, basis Y 68, kammen tot ~100; zand op een zandstenen kern | Geen Java, golvend en hoog; beperkt hangend zand boven grotten |
| 2026-10-01 | worldgen | Hoodoos: eigen density function `hoodoo_pillars`; pilaren per blok buiten `interpolated`, één vaste dichtheid binnen de vorm, niet in de initiële dichtheid | Pilaren, kappen en bogen, nooit zwevend, pure functie; dunne vormen overleven interpolatie niet; menging kapt van boven af, dus niets zweeft |
| 2026-10-01 | worldgen | Hoodoo-materiaal: kloofsteenlagen met kaprots van geschroeide zandsteen | Klassiek hoodoobeeld; zonnesteen in hoodoos (design) |
| 2026-10-01 | worldgen | Hoodoo-maat en -dichtheid: vloer Y 85–91, pilaren 15–45 (toppen tot ~plateau Y 125–140), straal 2–4, cel 12, kans 0,6, 35 % kap, af en toe een boog (verhoudingen van optie A op de diepe hoogtes van change 23) | Uitgesleten plateau, goed doorloopbaar |
| 2026-10-01 | worldgen | Flora en spawns zout/duin/hoodoo: zout alleen kristallen en monsters; duinen vrijwel kaal; hoodoos dunnere kloofflora | Elk biome een eigen karakter |
| 2026-10-01 | worldgen | `Ring2Biomes.ALL` is de enige lijst van ring 2-biomes voor tags en tests; nieuwe ring 2-biomes ook in het lagenblok/de zandgrot-tak waar van toepassing | Eén plek om bij te werken (change 31 en later) |

Commit (na `/code-review`): `worldgen: add salt flats, hoodoo fields and dune sea biomes`.
