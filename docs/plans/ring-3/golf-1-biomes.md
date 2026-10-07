# Golf 1 — stroom W-biomes: 39b (biome-skelet), daarna 39c (echte surface)

Scope: `worldgen` — 39b in branch `ring3/biomes-g1`; 39c pas na integratie 1 (op main, sequentieel, vóór teststop 4a).

**Doel 39b:** de biomes van ring 3 bestaan en staan op de juiste plek, met kleuren, klimaat, spawns en tags. Het gaat om de 7 sub-biomes + de micro-biome `dormant_caldera` (8e biome, V8 = A); `steam_fens` volgt in golf 2. De `placeholder_ring_3` verdwijnt. Surface tijdelijk met vanilla-blokken.

**Doel 39c:** de surface rules wisselen naar de echte ring 3-blokken van B. Pas na integratie 1, dan kan teststop 4a met echte blokken.

Bron: `fase-1-wereld.md` Ring 3 + "Uitwerking sparring ring 3"; `contract.md` §2, §4; sjablonen `Ring2Biomes` (biomes, `generation`, tags), de controlelijst "Plekken die ring 2-biomes opsommen" in `CODEBASE.md`, `BiomeAmbience` (alleen placeholder-kleuren; sfeer is A in golf 2).

## Werk 39b
1. **`Ring3Biomes`** (bootstrap uit 37c): `ashen_wastes`, `volcanic_peaks`, `ember_woods`, `basalt_flows`, `sulfur_fields`, `dried_seabed`, `titan_fields` en `dormant_caldera` (V8 = A: groen, koel, geen natuurlijk water). Allemaal:
   - droog (geen neerslag), temperatuur hoog;
   - voorlopige lucht-/fogkleuren in asrood/grijs, met verschil per biome;
   - de definitieve kleuren komen van A (golf 2).
   - De lucht moet al op de grens omslaan; dat is een wow-voorwaarde voor 4a.
2. **Indeling:** inline `multi_noise` in de ring 3-source van de preset op `temperature` = `region_heat` en `vegetation` = `region_ground` + de derde as `weirdness` = `ring_3/overworld/caldera_dormant` (39a; V8 = A): `dormant_caldera` op weirdness ≈ 1, de andere biomes op 0. Tot W-terrein `caldera_dormant` invult, is die stub 0 en verschijnt `dormant_caldera` niet. Voorstel als startpunt:
   - heet + mineraal → `volcanic_peaks`/`sulfur_fields`;
   - heet + begroeid → `basalt_flows`/`ember_woods`;
   - koel + mineraal → `ashen_wastes`/`dried_seabed`;
   - koel + vreemd → `titan_fields`.
   - Aandelen meten op r 3.600/4.750/5.900. Afstemmen met de vormen van W-terrein (pieken waar de kegels staan): via main.
3. **Tags:** `is_ring_3`, `heat_peaks` (`volcanic_peaks`), `heat_refuges` (`dormant_caldera`), `c:is_hot/overworld`, `c:is_dry/overworld`; geen `minecraft:has_structure/*`.
4. **Spawns:** geen vee, behalve geiten in `volcanic_peaks` (V7 = A: lage weight, groepjes 1–3); monsters vanilla.
5. **Generatie:** carvers, geen water-lakes, geen vanilla-bomen/gras. Ertsen en features volgen in golf 2 (41, 47*).
6. **Surface (`noise_settings/overworld.json`):** per biome een tak, nu met vanilla-blokken:
   - as = `smooth_basalt`/grind;
   - pieken = basalt/blackstone/magma;
   - zwavel = `yellow_terracotta`;
   - zeekom = zand/grind + dood koraal later;
   - titanen = `soul_soil` + basalt.
   - Steile wanden nooit vanilla-gras (de les van de kloofsteenlagen).
7. **Controlelijst** "Plekken die ring 3-biomes opsommen" maken (zoals ring 2) en als CODEBASE-delta aanleveren.
8. **Lang:** `Ring3BiomeNames` (en + nl).

## Werk 39c (na integratie 1)
De surface naar de echte blokken:
- `ash_block`/`ash_layer` en `ash_crust` boven holtes (alleen waar het terrein dat toelaat);
- puimsteen in de pieken, smeulsteen in de scheuren van `basalt_flows`;
- `sulfur_crust` in `sulfur_fields`;
- de slapende krater groen (moss/coarse dirt).
Geblakerde vlekken in `dry_riverbeds` naar as en verkoold hout (backlog r. 122).

## Afhankelijk van open vragen
- Geen meer: V7 en V8 = A (2026-10-06), verwerkt hierboven.

## Client/server-impact
Geen (data). Biome-kleuren zijn data; de client rendert.

## Tests
- **B**, **D**, **S.**
- Biome-aandelen en vlekgrootte (raster over de ring).
- `/locate biome` per biome.
- Ring 1/2 onveranderd.
- Feature-index-effect op ring 1/2 noteren (de officiële meting doet INT).
- **W-checklist** voor 4a: per biome een coördinaat en de grensovergang.

## Bij afronding
`waves.md` §4.

## Uitvoering 39b (2026-10-06, branch `ring3/biomes-g1`)

Voortgang: 20:40 branch `ring3/biomes-g1` op 8e0c637 (worktree stond op 17c9eb4; teruggezet), gemeld aan main
Voortgang: 20:45 kwantielen regio-assen gemeten, indeling gekozen, drempels aan main gemeld (afstemming W-terrein)
Voortgang: 20:50 `Ring3Biomes`, tags, lang, preset, surface, placeholder weg; tests groen; runData
Voortgang: 20:58 S: `/locate biome` 7/7 (+ `dormant_caldera` met tijdelijke ruis), surface zonder gras/aarde
Voortgang: 21:04 determinisme ring 1/2 tegen 8e0c637; B groen (82 klassen, 428 tests)
Voortgang: 21:08 review (medium) binnen: 8 bevindingen, 4 opgelost, 4 vastgelegd (zie Review)
Voortgang: 21:25 na review: D, B groen (82 klassen, 428 tests), S `biomes-g1-s2` zonder fouten; commit

**Gebouwd**
- `Ring3Biomes`: de 8 biomes (`ALL` = de enige Java-lijst), droog, temperatuur 2,0, eigen voorlopige lucht/fog per biome (`PALETTES`, een nuance van `RING_3` asrood; A vervangt ze in golf 2), grijs-olijf gras/blad, `dormant_caldera` groen (downfall 0,4, alleen kleur). Spawns: de woestijnmonsters van ring 2 (husks, vanilla tot fase 4) en vleermuizen, geen dieren; in `volcanic_peaks` geiten (weight 1, groepjes 1–3) met `creatureGenerationProbability` 0,03 (vanilla 0,1) en een spawn cost (charge 1,0, budget 0,12). Gebouwd met `Ring2Biomes.biome` (nu package-private). Generatie: alleen de diepe carvers van ring 2 (`Ring2Carvers.COPIES`), geen features.
- Indeling (preset, inline `multi_noise` van ring 3, exacte rechthoeken; h = `temperature` = `region_heat`, g = `humidity` = `region_ground`):

  | | g < −0,122 | −0,122 ≤ g < 0 | 0 ≤ g < 0,122 | g ≥ 0,122 |
  |---|---|---|---|---|
  | h ≥ 0,166 (heet) | `volcanic_peaks` | `volcanic_peaks` | `basalt_flows` | `basalt_flows` |
  | −0,051 ≤ h < 0,166 | `sulfur_fields` | `sulfur_fields` | `ember_woods` | `ember_woods` |
  | h < −0,051 (koel) | `dried_seabed` | `ashen_wastes` | `ashen_wastes` | `titan_fields` |

  Alle 7 op `weirdness` [−2; 0,5]; `dormant_caldera` overal op `weirdness` [0,5; 2] (`caldera_dormant` ≈ 1). Continentalness, erosion, depth [−2; 2].
- Tags (`Ring3BiomeTags`): `is_ring_3`, `c:is_hot/overworld`, `c:is_dry/overworld` = `ALL`; `heat_peaks` = `volcanic_peaks`; `heat_refuges` = `dormant_caldera`. Geen `has_structure/*`.
- Lang (`Ring3BiomeNames`, en + nl): Ashen Wastes/Asvlakte, Volcanic Peaks/Vulkaanpieken, Ember Woods/Smeulwoud, Basalt Flows/Basaltstromen, Sulfur Fields/Zwavelvelden, Dried Seabed/Drooggevallen zeebodem, Titan Fields/Titanenveld, Dormant Caldera/Slapende krater.
- Surface (`noise_settings/overworld.json`, 8 takken direct na `dry_riverbeds` in `above_preliminary_surface`; alleen toegevoegd): per biome een bovenblok (ruis `minecraft:surface`) en een onvoorwaardelijk blok over de surface-diepte, zodat vanilla's gras/aarde er nooit komt:
  - `ashen_wastes`: grind / smooth basalt, eronder smooth basalt;
  - `volcanic_peaks`: basalt, blackstone-vlekken, wat magma; eronder blackstone;
  - `ember_woods`: coarse dirt, smooth-basaltvlekken; eronder smooth basalt;
  - `basalt_flows`: basalt met dunne magmascheuren en blackstone; eronder basalt;
  - `sulfur_fields`: yellow terracotta, terracotta- en smooth-basaltvlekken; eronder smooth basalt;
  - `dried_seabed`: zand, grind, kleipannen; eronder en op steile wanden zandsteen;
  - `titan_fields`: soul soil / basalt; eronder basalt;
  - `dormant_caldera`: moss / coarse dirt; eronder coarse dirt; steile wanden basalt.
- `placeholder_ring_3` weg (key in `ModBiomes`, `PlaceholderBiomes`, de twee lang-providers; één regel elk). Werelden van vóór 39b laden niet meer.
- Tests: `Ring3BiomesTest` (lijst, droog/heet, eigen lucht/fog per biome ≠ `dry_riverbeds`, spawns/geiten, alleen diepe carvers en geen features, surface-tak per biome zonder gras/aarde), `Ring3RegionSharesTest` (preset = `ALL`, de 7 dekken het vlak precies één keer bij weirdness 0 en `dormant_caldera` bij weirdness 1, aandelen stationair 1/7 ± 3 punten).

**Metingen**
- Regio-assen stationair (3 seeds): kwantielen h/g vrijwel gelijk (q1/7 −0,31, q3/7 −0,051, q1/2 0,00, q5/7 0,166, q6/7 0,31; sd ≈ 0,29).
- Aandelen stationair: elk 14,2–14,4 %. Seed 20260930 in de ring (r 3.564–5.936): ashen 11,9 · basalt 14,7 · seabed 12,5 · ember 20,2 · sulfur 16,7 · titan 9,9 · peaks 14,0 %. Op de cirkels r 3.600/4.750/5.900 per biome 2–27 % (een cirkel snijdt maar een handvol vlakken; titan 2 % op r 5.900). Seeds 1 en 42 in de ring: alle biomes 8,5–22,9 %.
- Vlekgrootte (gemiddelde loop langs lijnen tot de biome wisselt): middenbiomes (ashen, ember, sulfur) ~540, hoekbiomes ~870, gemiddeld **695** (ring 2: 497).
- S (wereld `biomes-g1-s1`): server start zonder fouten; `/locate biome` vanaf (4.750, 0): ashen (5.262, 480), peaks (4.110, 672), ember (4.750, 0), basalt (4.814, −288), sulfur (4.270, 480), seabed (5.710, −32), titan (4.942, 160); `dormant_caldera` niet (stub = 0, verwacht). Bovenblokken in 63 chunks rond die punten: geen gras of aarde (bijv. ashen grind 64 %/smooth basalt 35 %, sulfur yellow terracotta 77 %, titan basalt 61 %/soul soil 37 %).
- `dormant_caldera` met tijdelijke lokale ruis (`caldera_dormant` = 2 × `ring_3_heat` op xz ×12, niet gecommit; wereld `biomes-g1-cal`): `/locate` vanaf r 3.600/4.750/5.900 → 90/160/45 blokken (ook aan de ring 3-kant van zone 3→4); bovenblok moss 88 %/coarse dirt 10 %.
- **Determinisme ring 1/2** (`det-base` op 8e0c637 ↔ `det-39b`, zelfde laadvolgorde, 36 chunks: ring 1 ×10, zone 1→2 ×4, ring 2 ×10, zone 2→3 ×6, ring 3 ×6): biomes in **alle 30** ring 1/2/zone-chunks identiek; 23/30 ook blok voor blok; de 7 andere verschillen alleen in vanilla-ertsklodders, dripstone, zeegras/water (2–121 blokken). Twee 39b-werelden onderling (`biomes-g1-s1` ↔ `det-39b`) verschillen in dezelfde soort (8 chunks, ook sculk/water/lava): thread-volgorde, niet de change. Ring 3-chunks tussen de twee 39b-werelden identiek.
- Feature-index: ring 3 voegt geen enkele feature toe (test), dus de globale featurevolgorde van ring 1/2 is ongewijzigd.

**Implementatiekeuzes (zelf beslist)**
- Indeling: drie hittebanden (2/7, 2/7, 3/7) en de grondas gesplitst op 0 (heet/midden) en op ±0,122 (koel), volgens het startvoorstel van dit plan; elk biome 1/7. Aan main gemeld voor W-terrein (kegels in h ≥ 0,166 & g < 0, zeekom in h < −0,051 & g < −0,122, titanenveld in h < −0,051 & g ≥ 0,122).
- Carvers: de diepe vanilla-carvers van ring 2 hergebruikt (`Ring3Carvers` blijft leeg tot ring 3 eigen carvers krijgt, 42). Het terrein ligt in ring 3 hoger, dus de grotten komen zelden aan de oppervlakte (geen hangend grind/zand).
- Geen enkele feature (ook geen waterbronnen: geen water boven Y 0) tot golf 2.
- Monsters: de woestijnset van ring 2 (husks), zonder dieren. Geiten: weight 1; omdat ze het enige dier zijn, houden de lagere `creatureGenerationProbability` (0,03, bij generatie) en een spawn cost (1,0/0,12, natuurlijke spawns) ze zeldzaam; de vanilla-spawnregel houdt ze op `#goats_spawnable_on`.
- Voorlopige kleuren in `Ring3Biomes.PALETTES` (niet in `BiomeAmbience`: dat is van A).

**Review (`/code-review medium biomes-g1 39b`, 8 bevindingen) en afhandeling**
1. *Geiten kunnen niet spawnen: basalt/blackstone/magma zitten niet in `#goats_spawnable_on`.* Klopt in deze branch alleen; de tag is van B (contract §3: + basalt, blackstone, `ash_block`), dus na integratie 1 in orde. Niet zelf in de tag (niet mijn bestand). Toegevoegd aan de W-checklist en het rapport: na integratie controleren.
2. *0,03 werkt alleen bij generatie; daarna vullen geiten de creature-cap.* Opgelost: spawn cost voor geiten in `volcanic_peaks` (charge 1,0, budget 0,12; mechanisme van vanilla's soul sand valley), zodat natuurlijke spawns dun blijven. Startwaarden, afstemmen in 4b.
3. *`CODEBASE.md` niet bijgewerkt.* Bewust: `waves.md` §4.8 — stromen schrijven niet in `CODEBASE.md`; de "CODEBASE-delta" hieronder voegt INT samen. De verwijzing in de javadoc van `ALL` klopt na integratie.
4. *Beslissingen/backlog niet in de logs.* Idem (§4.8): secties "Beslissingen" en "Backlog" hieronder, INT zet ze over.
5. *`biome(...)` dubbel met `Ring2Biomes`.* Opgelost: `Ring2Biomes.biome` package-private, ring 3 bouwt ermee (`Optional.empty()` voor additions).
6. *`withGoats` herhaalt de kopieerlus.* Opgelost: één `spawns(boolean goats)` met één lus.
7. *630 regels handgeschreven surface-JSON.* Niet gewijzigd: handgeschreven worldgen-JSON is de afspraak (CLAUDE.md §9) en de takken verschillen per biome; `Ring3BiomesTest` faalt als een biome uit `ALL` geen eigen tak met bovenblok en diepte-blok heeft, en de controlelijst noemt het. 39c vervangt de blokken toch.
8. *`PALETTES.get` zonder controle.* Opgelost: `Objects.requireNonNull` met de biome in de melding.

**W-checklist (kort; teststop 4a)**
- [ ] `/ca tpboundary 2` en oostwaarts lopen: `dry_riverbeds` → een ring 3-biome; de lucht slaat om naar asrood/grijs.
- [ ] Per biome even kijken (seed 20260930): ashen (5.262, 480), peaks (4.110, 672), ember (4.750, 0), basalt (4.814, −288), sulfur (4.270, 480), seabed (5.710, −32), titan (4.942, 160). Elk een eigen lucht/fog; nergens gras.
- [ ] Biomevlakken even groot of groter dan in ring 2; geen biome die bijna ontbreekt.
- [ ] Na W-terrein: `dormant_caldera` in slapende kraters (`/locate biome corruptedadventure:dormant_caldera`), groen; pieken/zeekom/titanenveld vallen samen met hun terreinvorm.
- [ ] Geiten op de pieken (zeldzaam, groepjes 1–3); kan pas na integratie 1 (B zet basalt/blackstone in `#goats_spawnable_on`).

## CODEBASE-delta
- **Bestandsboom `worldgen/biome/`:** `Ring3Biomes.java` ✅ datagen-bootstrap ring 3 (change 39b): `ashen_wastes`, `volcanic_peaks`, `ember_woods`, `basalt_flows`, `sulfur_fields`, `dried_seabed`, `titan_fields`, `dormant_caldera`; `ALL` (de enige Java-lijst van ring 3-biomes), `PALETTES` (voorlopige kleuren tot A), `generation` (alleen de diepe carvers van ring 2, geen features), `spawns` (woestijnmonsters ring 2 zonder dieren; geiten met spawn cost in `volcanic_peaks`); bouwt met `Ring2Biomes.biome` (gedeelde hete, droge builder, nu package-private). `PlaceholderBiomes`: geen overworld ring 3 meer.
- **`datagen/tags/Ring3BiomeTags`:** `is_ring_3`, `c:is_hot/overworld`, `c:is_dry/overworld` (= `Ring3Biomes.ALL`), `heat_peaks` (`volcanic_peaks`), `heat_refuges` (`dormant_caldera`). **`datagen/lang/Ring3BiomeNames`:** en + nl.
- **Biome-tabel:** de 8 ring 3-biomes (rol + plek in de indeling, zie "Uitvoering 39b"); `placeholder_ring_3` weg (rij `placeholder_ring_3 … placeholder_ring_5` → `placeholder_ring_4`, `placeholder_ring_5`; "`placeholder_ring_3` sinds change 39b").
- **Registries/tags-tabel:** `corruptedadventure:is_ring_3`, `heat_peaks`, `heat_refuges` (inhoud nu gevuld); `c:is_hot/overworld`, `c:is_dry/overworld`: ring 2 + ring 3.
- **Databestanden:** `data/corruptedadventure/worldgen/biome/` + 8 ring 3-biomes, − `placeholder_ring_3`; tags `is_ring_3`, `heat_peaks`, `heat_refuges`; preset `rings.ring_3` = `multi_noise` (8 rechthoeken); `noise_settings/overworld.json` 8 ring 3-surfacetakken na `dry_riverbeds`.
- **Testseed/werelden:** werelden van vóór 39b laden niet meer (`placeholder_ring_3`).
- **Tests:** `worldgen/biome/Ring3BiomesTest.java`, `Ring3RegionSharesTest.java` (zie "Uitvoering 39b").
- **Nieuwe controlelijst** (naast die van ring 2):

  ### Plekken die ring 3-biomes opsommen (controlelijst, change 39b)
  Java leest `Ring3Biomes.ALL` (tags `is_ring_3`, `c:is_hot/overworld`, `c:is_dry/overworld`; `Ring3BiomesTest`, `Ring3RegionSharesTest`). Handgeschreven JSON kan dat niet; een nieuwe ring 3-biome (bijv. `steam_fens` in 48) moet hier ook bij:
  - preset `corrupted_world.json`: een rechthoek in de `multi_noise` van ring 3 (de 7 sub-biomes dekken het h/g-vlak bij weirdness < 0,5 precies één keer; `Ring3RegionSharesTest`), of `transitions.ring_3` voor `steam_fens` (en dan `ring_3` uit `switch_at` van `ring_blend/overworld_temperature`, `_vegetation`, `_weirdness`; `Ring3JsonTest`);
  - `noise_settings/overworld.json`: een eigen tak in `above_preliminary_surface` (na `dormant_caldera`), met een onvoorwaardelijk bovenblok en een blok over de surface-diepte (anders valt de kolom door naar vanilla's gras/aarde; `Ring3BiomesTest`);
  - `Ring3BiomeNames` (en + nl);
  - heat-tags als de biome een piek of schuilplek is (`heat_peaks`, `heat_refuges`).
  Nieuwe ring 3-biomes gebruiken ook `Ring3Biomes.generation` (één globale featurevolgorde).

## Beslissingen
- 2026-10-06 · worldgen · Ring 3-biomes in de `multi_noise` op `region_heat` × `region_ground`: heet (h ≥ 0,166) pieken/stromen, midden zwavel/smeulwoud, koel (h < −0,051) zeekom/as/titanen; grondas op 0 resp. ±0,122; elk biome 1/7; `dormant_caldera` op weirdness ≥ 0,5 · het startvoorstel van plan 39b, gelijke aandelen, vlakken ~700 blokken (≥ ring 2).
- 2026-10-06 · worldgen · Ring 3 golf 1: alleen de diepe carvers van ring 2, geen features; monsters = ring 2-woestijnset zonder dieren; geiten weight 1, groepjes 1–3, generatiekans 0,03 en spawn cost 1,0/0,12 · geen invloed op de featurevolgorde van ring 1/2; geiten zijn het enige dier, dus "lage weight" werkt pas met een lagere generatiekans en een spawn cost (anders vullen ze de creature-cap).
- 2026-10-06 · worldgen · Ring 3-monsters voorlopig = ring 2-woestijnset (husks), tot fase 4; wijkt af van contract "vanilla" · besluit gebruiker.
- 2026-10-06 · worldgen · `placeholder_ring_3` verwijderd (39b) · vervangen door de echte biomes; oude werelden laden niet meer.

## Backlog
- `worldgen` (39c): de surface-ruis `minecraft:surface` is grofkorrelig; lokaal kan één variant domineren (bijv. 65 % blackstone in een stuk `volcanic_peaks`). Bij de echte blokken de drempels/ruis per biome bijstellen.
- `worldgen` (39/42): ring 3 heeft onder de surface nog vanilla steen/deepslate; een ring 3-ondergrond (basalt/blackstone-lagen) hoort bij W-terrein of 39c.
- `atmosphere` (50): de voorlopige kleuren in `Ring3Biomes.PALETTES` naar `BiomeAmbience` (ring 3-builders) en definitief maken.
