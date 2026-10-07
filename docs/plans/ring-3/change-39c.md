# Change 39c — echte ring 3-blokken in de surface (ring 3 + Nether-ring 3)

Scope: `worldgen` — werkpakket 39c van W-biomes (`waves.md` §2 "Integratie 1"), op de geïntegreerde main (686ae8b), sequentieel, vóór teststop 4a.

## Doel
De voorlopige vanilla-surface van ring 3 (39b/39) en Nether-ring 3 (53) vervangen door de echte ring 3-blokken van B:
- overworld: as (`ash_block`, `ash_crust`, `smoldering_ash`), puimsteen, smeulsteen, zwavelkorst;
- Nether-3: koudlava, Nether-ijs, berijpt netherrack, gloeiende scheur, oerijs.

Per biome passend en niet monotoon (les ring 2). Ring 1/2 (ook `dry_riverbeds`) blijven blok voor blok gelijk.

Bron: `golf-1-biomes.md` "Werk 39c", `contract.md` §3–4, `fase-1-wereld.md` Ring 3 + Nether-ring 3, backlog W-biomes ("surface-ruis afstemmen in 39c", "ring 3-ondergrond").

## Plan
- `noise_settings/overworld.json`: de 8 ring 3-takken opnieuw, plus twee gedeelde ring 3-takken: vulkaan/oevers ervóór, ondergrond erna.
- `noise_settings/nether.json`: de tak van `placeholder_nether_ring_3` opnieuw (de drempels en noises van 53 blijven).
- Eigen surface-ruis `noise/ring_3_surface_*`.
- Een kleine surface-conditie `corruptedadventure:volcanic_threshold` die de kolomwaarden van het ring 3-terrein leest.
- Client/server-impact: geen (worldgen-data en één worldgen-klasse, server-side; de client rendert alleen blokken).

## Uitvoering

Voortgang: 13:10 basiswereld `det-39c-base` op 686ae8b (ring 1/2-gebieden, 5 ring 3-gebieden, Nether-1/2/3); biome-/hoogtekaart van de hele ring (`/ca terrainmap 0 0 6000 16`).
Voortgang: 13:25 vragen aan main: ondergrond, oerijs; meldingen `dry_riverbeds` en `ash_layer`.
Voortgang: 13:30 eerste versie (v1) gegenereerd en beoordeeld op bovenaanzichten. v2/v3: strepen en randen gebroken, oevers smaller, minder blackstone, korrel-ruis.
Voortgang: 13:50 B groen (96 klassen, 540 tests), D zonder diff.
Voortgang: 14:20 antwoorden V1 = A, V2 = A; wereld `smoke-39c` + omgekeerde wereld `c39c-rev`: determinisme, geiten, lavawanden.
Voortgang: 14:45 oerijs-drempel 0,72 bleek leeg; gekalibreerd (twee testwerelden) en aan main voorgelegd; review gestart.
Voortgang: 15:00 gebruiker: oerijs bij 0,66 (herziene A).
Voortgang: 15:20 review 1 (9 punten): de generator voegde de gedeelde takken bij elke run opnieuw toe, en de oudste (v1) won. Generator idempotent gemaakt, conditie via de noise chunk; zie "Review 1".
Voortgang: 15:45 na de fixes: B groen (96 klassen, 540 tests), D zonder diff, verse `smoke-39c` + `c39c-rev`; metingen, beelden en determinisme opnieuw.
Voortgang: 16:10 review 2 (10 punten) afgehandeld: zie "Review 2". Generator naar `tools/worldgen/ring3_surface.py`.
Voortgang: 16:45 B groen (96 klassen, 540 tests), D zonder diff; verse `smoke-39c` + `c39c-rev`; metingen, beelden en determinisme opnieuw (de cijfers hieronder zijn van deze eindversie).

### Gebouwd
- **`worldgen/surface/VolcanicThresholdCondition`** (`corruptedadventure:volcanic_threshold`, geregistreerd in `ModSurfaceConditions`, blok `// ring 3 — W-biomes`):
  - waar: een kolomuitgang van het `volcanic_field` ligt in `[min, max)`; uitgangen `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`, `roughness`, nieuw `volcano_rise` (hoogte van de vulkanen boven de vlakte) en `lava_bank` (0..1: een lavageul die lava draagt, tot 1,3 halve breedte: de oevers). Andere uitgangen weigert de codec (allow-list `Output.perColumn()`);
  - leest de eigen instantie van het veld van de noise chunk: `RingChunkGenerator` geeft die via een thread-local (`surfaceVolcanicField()`) aan zijn surface- en carver-pass (carvers vragen de surface rules om het bovenblok waar ze door gras snijden). Zo deelt de surface de kolomcache en de zoektocht in de router van de generator, en ziet ze precies de waarden van het terrein;
  - zonder veld onwaar, met één waarschuwing per wereld/dimensie (per random state) in de log; in `smoke-39c` 0 keer;
  - onthoudt zijn laatste kolom en leest de waarde direct uit de kolom (`VolcanicOutput.columnValue`, `Output.perColumn()`).
- **`VolcanicField.Column`**: twee extra kolomwaarden, `rise` (de opheffing door vulkanen, vóór de kraters erin zakken) en `lavaBank` (`1 − along/1,3` waar de geul lava draagt, dezelfde voorwaarde als de lavaregel), alleen opgeslagen; het terrein verandert niet.
- **Noises** `noise/ring_3_surface_patches` (−7, [1, 1, 0,5]: vlekken ~50–150), `_detail` (−4, [1, 1]: vlekjes ~4–10), `_cracks` (−6, [1, 0,6]: dunne lijnen waar \|n\| klein is), `_grain` (−2, [1, 1]: korrel ~2–4).
- **Overworld** (`above_preliminary_surface`, direct na `dry_riverbeds`; ring 1/2-takken ongewijzigd):
  1. **Gedeelde vulkaantak** (alle 8 ring 3-biomes):
     - binnenkant actieve krater (`caldera_mask` ≥ 0,12, `caldera_dormant` < 0,02): blackstone, basalt-vlekken, magma-korrel, smeulsteen-scheurtjes; bij het meer (`caldera_mask` ≥ 0,65) meer magma en smeulsteen;
     - wand van een slapende krater (`caldera_dormant` ≥ 0,02, `caldera_mask` < 0,5): puimsteen met as; de bodem (`caldera_dormant` ≥ 0,3) houdt de eigen surface van de biome (groen in `dormant_caldera`) en krijgt geen kegel- of oeverstijl;
     - kegel (`volcano_rise` ≥ 24, of ≥ 12 in de helft van de vlekken: een rafelige voet): flanken basalt met as, smooth basalt en blackstone-vlekken; Y ≥ 168 een gemengde gordel; Y ≥ 196 een kap van puimsteen met as en blackstone (gedithered met de surface-diepte). Alleen echte vulkanen, geen hoge vlaktes;
     - lavaoevers (`lava_bank` > 0, dus alleen waar de geul lava draagt): blackstone met basalt, magma-korrel dicht bij de lava (`lava_bank` ≥ 0,15).
  2. **Biome-takken** (bovenblok, sublagen, surface-diepte):
     - `ashen_wastes`: as; puimsteenvlekken, smooth-basalt-ontsluitingen, grind- en smooth-basaltspikkels; askorst op daken van één blok boven een holte;
     - `volcanic_peaks` (vlakte tussen de kegels): basalt; as/puimsteen- en blackstone-vlekken, magma in onderbroken scheurtjes, smooth basalt; askorst; eronder blackstone;
     - `ember_woods`: as en grove aarde (plantgrond van 47), smeulende as in onderbroken aders en spikkels, smooth-basaltvlekken; askorst; eronder as, dan grove aarde;
     - `basalt_flows`: basalt; gloeiende scheurlijnen van smeulsteen met magma in het midden, pahoehoe-vlekken smooth basalt, blackstone-vlekken en -korrel, asstof;
     - `sulfur_fields`: zwavelkorst; smooth-basaltvlekken met een gespikkelde halo van gele terracotta (`P` −0,42..−0,3), asvlekken met een gespikkelde calcietrand, calcietspikkels; eronder 2 zwavelkorst, dan smooth basalt;
     - `dried_seabed`: zand, grind, kleipannen (als 39b), plus asduinen en puimsteenkiezels; steil en eronder zandsteen;
     - `titan_fields`: as met zielgrondvlekken (zielvuur in 47), basalt/smooth-basaltvlekken; steil basalt; askorst;
     - `dormant_caldera`: kraterwand (`caldera_mask` < 0,5) puimsteen/as/basalt, bodem mos met grove aarde en asvlekken; steil basalt.
  3. **Gedeelde ondergrondtak** (alle 8): onder de surface-diepte een pakket dat het oppervlak volgt (~13–21 blokken onder de top). Eerst ~4 blokken tuf of smooth basalt, dan basalt met blackstone-aders. Daaronder vanilla steen/deepslate (V1 = A).
- **Nether-3** (tak `placeholder_nether_ring_3`, zelfde regio-drempels ±0,18 en noises):
  - pilaren: huid van Nether-ijs, lijf `packed_ice`, kern `blue_ice` (≥ 0,62), oerijs (≥ 0,66, Y 45–95);
  - gletsjermassa: verpakt Nether-ijs met `blue_ice`/`packed_ice`-aders, vloeren Nether-ijs;
  - scheurwanden: gloeiende scheur met magma-spikkels (Y 14–29); een gloeiende naad op de vloer bij de sterkste scheurvlekken;
  - zeevloer: koudlava (was basalt) met platen Nether-ijs (`nether_state_selector` ≥ 0,25) en verpakt Nether-ijs (≥ 0,1) (was `packed_ice` ≥ 0,0); 6 diep koudlava;
  - rijpwoud: vloeren berijpt netherrack met ijsplekken, plafonds `packed_ice`/Nether-ijs, massa berijpt netherrack;
  - plafond van de zee: berijpt netherrack; de rest netherrack.
- **Tests:**
  - `VolcanicThresholdConditionTest` (codec, weigert per-blok-uitgangen en lege bereiken, elk gebruik in `overworld.json` decodeert, niet in `nether.json`);
  - `Ring3BiomesTest` accepteert gedeelde ring 3-takken: alleen ring 3-biomes, hoogstens één vóór en één na de biome-takken, vóór vanilla, zonder gras/aarde; een biome met twee eigen takken faalt.
- **Gereedschap:** de ring 3-surface-JSON komt uit **`tools/worldgen/ring3_surface.py`** (in git; `python tools/worldgen/ring3_surface.py` vanuit de repo-root, alleen de standaardbibliotheek; idempotent, twee runs en een run vanaf HEAD geven byte-gelijke bestanden). Kaarten en metingen met `build/c39c/{topmap,section,cores,compare,gen}.py` (niet in git).

### Metingen (wereld `smoke-39c`, seed 20260930)
Aandeel van het bovenste blok per biome (gebieden in chunks: A −312,33..−289,56 · B 246,−225..269,−202 · C −250,−8..−227,15 (horizonreus) · D −255,−124..−232,−101 · E −53,−367..−30,−344 (pieken)):

| Biome | Aandelen |
|---|---|
| `ashen_wastes` | as 71–82 %, puimsteen 0–11 %, smooth basalt 7–9 %, grind 4 %, basalt 0–3 % |
| `volcanic_peaks` | basalt 45–57 %, as 5–26 %, blackstone 9–41 %, smooth basalt 3–5 %, puimsteen 2–5 %, magma 2 % |
| `ember_woods` (vlakte, A/B) | as 26–39 %, grove aarde 19–28 %, smeulende as 2–3 %, rest lavarivieren, oevers en kegelvoeten |
| `basalt_flows` | basalt 36–65 %, blackstone 18–41 %, smooth basalt 2–14 %, as 4–5 %, smeulsteen 3 %, magma 2 % |
| `sulfur_fields` (vlakte, B) | zwavelkorst 58 %, blackstone 14 % (oevers), calciet 2 %, gele terracotta 2 % (+ lava); op de flanken van een reus (C/D) zwavelkorst 27–30 % en de rest vulkaanstijl |
| `dried_seabed` | zand 54–63 %, grind 4–33 %, as 2–24 %, klei 4–6 %, puimsteen 2–3 %, basalt 2 % (kegelvoet) |
| `titan_fields` | as 45–56 %, zielgrond 27–43 %, basalt 0–18 %, puimsteen 0–8 % |
| `dormant_caldera` | mos 73 %, grove aarde 23 %, puimsteen 2 % |
| Nether-3 (vloer onder Y 60) | berijpt netherrack 55 %, koudlava 18 %, Nether-ijs 14 %, verpakt Nether-ijs 10 %, `packed_ice` 3 % |

- **Beelden** (zelf bekeken; v1 had strepen langs de randen van de grote vlekken en te brede oevers, opgelost in v2/v3):
  - bovenaanzichten `build/c39c/final-{A,B,C,D,E}.png` (blokkleuren, 2 px per blok, hellingschaduw);
  - Nether-3-vloer `final-N3.png`;
  - vóór/na: `base-*.png`;
  - doorsnede reus `v3-sec-C.png`;
  - biome-/hoogtekaart `top_overworld_0_0_r6000_s16_*.png`.
- **Askorst:** ~0 % van de bovenblokken: daken van één blok boven een ruisgrot zijn zeldzaam, carver-grotten komen pas na de surface (zie Backlog).
- **Geiten:** 18 geiten bij de generatie in gebied E. Ze staan op as 10, blackstone 4 en basalt 3 (één sprong al); as, puimsteen, basalt, blackstone en smooth basalt staan alle in `#goats_spawnable_on` (RCON `if block`).
- **Determinisme:**
  - ring 1/2 tegen `det-39c-base` (686ae8b, 11 gebieden × 16 chunks): 135/176 identiek; de rest alleen vanilla-ruis (ertsklodders, sculk in r1c, water, zeegras, vallend `loose_sand`, bubble columns), geen ring 3-blok. Eerdere testwerelden gaven 133–149/176 met dezelfde soort verschillen: laadvolgorde-ruis;
  - Nether-1/2: 43/48, de rest vanilla-ertsen (kwarts, magma, grind) en lava; zone 2|3 verandert alleen in `placeholder_nether_ring_3`;
  - ring 3 tegen een tweede wereld omgekeerd geladen (`c39c-rev`): 2.846/2.880, de rest alleen water/lava-stroming; Nether-3 592/592.
- **Lavawanden** Nether (`tools/nether-lava-walls.py`): r 875 → 0, r 1500 → 0.
- **Oerijs** (24×24 chunks Nether-3, per Y-band een drempel getest): grote pilaren ~30 per gebied, maar slechts 3–4 halen ruis ≥ 0,62. Kernen per drempel: 0,62 → 4 (3–90 cellen), 0,66 → 2 (5, 36), 0,68 → 1–2 (5–7), 0,70–0,72 → 0. Met 0,72 kwam er vrijwel geen oerijs; met 0,66 in `smoke-39c` 2.066 blokken oerijs in dit gebied (naast 11.730 blauw ijs en 7.773 gloeiende scheur).

### Review 2 (`/code-review medium 39c surface v2`, 10 punten) en afhandeling
1. **Askorst op 2-dikke daken op vast gesteente valt nooit; carver-grotten komen na de surface.** Nu alleen daken van één blok (`stone_depth` ceiling, offset 0). Het echte instorten boven grotten staat in de Backlog (golf 2); de W-checklist-regel wacht daarop.
2. **Koppeling via `noiseChunk.aquifer instanceof RingAquifer` breekbaar; WARN per JVM.** `RingChunkGenerator` geeft nu zelf het veld van de noise chunk aan zijn surface- en carver-pass (thread-local rond `buildSurface`/`applyCarvers`; `surfaceVolcanicField()`). Geen AT op `Context.noiseChunk` meer. WARN één keer per random state (per wereld/dimensie).
3. **`branches.put` overschreef dubbele per-biome-takken.** De test faalt nu bij twee takken voor één biome.
4. **Kegelstijl op hoge vlaktes (Y ≥ 116 hing niet aan een vulkaan).** De trigger is nu de vulkaangeometrie: `volcano_rise` (nieuwe kolomwaarde) ≥ 24, of ≥ 12 in de helft van de vlekken. De Y-trigger en `roughness` zijn weg. Zwavel opnieuw gemeten: vlakte B 58 % zwavelkorst, op de reus 27–30 %.
5. **Vulkaantak sloot `dried_seabed` en `dormant_caldera` uit (scherpe snede op de flank).** Nu alle 8. De bodem van een slapende krater (`caldera_dormant` ≥ 0,3) krijgt geen kegel- of oeverstijl, de wand (`caldera_mask` < 0,5) puimsteen.
6. **Oevers op `lava_channel` ≥ 0,06 vielen niet samen met de lava.** Nieuwe kolomwaarde `lava_bank` met dezelfde voorwaarde als de lavaregel (`depth` ≥ 0,85, boven het niveau), tot 1,3 halve breedte; droge beddingen krijgen geen oevers meer.
7. **CODEBASE.md/beslissingenlog:** deel B; de delta staat hieronder.
8. **Generator niet in git, kegel 3× gekopieerd.** Generator nu `tools/worldgen/ring3_surface.py` (gecommit, met uitleg); de kegel staat nog 2× in de JSON (surface rules hebben geen "of"), uit één functie gegenereerd.
9. **Dode halo-range in `sulfur_fields`.** Nu `P` −0,42..−0,3 (aansluitend op de smooth-basaltvlek).
10. **`perColumn()` als uitsluiting.** Nu een allow-list (`EnumSet`).

### Review 1 (`/code-review medium 39c surface`, 9 punten) en afhandeling
1. + 2. **Gedeelde takken 6× in `overworld.json`; de oudste (v1) won altijd.** De generator voegde ze bij elke run opnieuw in. Opgelost: de generator verwijdert eerst de bestaande gedeelde ring 3-takken (idempotent; twee runs na elkaar geven dezelfde file) en schrijft vanaf de HEAD-versie. Metingen en beelden zijn opnieuw gemaakt met de juiste regels. Smallere oevers, magmakorrel, kegels zonder lijnen en halo's in de zwavel zijn nu echt actief.
3. **Test liet dubbele gedeelde takken door.** `Ring3BiomesTest` faalt nu als een gedeelde tak twee keer voorkomt, en controleert gras, aarde, podzol en mycelium ook in de gedeelde takken.
4. + 5. **Eigen veldkopie (lege kolomcache per chunk) en een JVM-brede lookup naast die van `RingChunkGenerator`.** De conditie leest nu de veldinstantie van de noise chunk via `RingChunkGenerator.volcanicField(NoiseChunk)` (uit de `RingAquifer`): dezelfde instantie en zoektocht als terrein en lava.
6. **Stil onwaar zonder veld.** Nu één WARN in de log; in `smoke-39c` 0 keer.
7. **Context-object + switch per kolom.** Nu `VolcanicOutput.columnValue(column, output)` direct op de kolom (`compute` gebruikt hem ook).
8. **CODEBASE.md:** volgens de opdracht door integratie deel B; de volledige delta staat hieronder.
9. **Plan aangevuld:** Uitvoering, Beslissingen, Backlog, CODEBASE-delta.

## Beslissingen
- 2026-10-07 · worldgen · Ondergrond ring 3: een vulkanisch pakket van ~8–14 blokken dat het oppervlak volgt (as → tuf/smooth basalt → basalt met blackstone-aders); daaronder steen/deepslate tot 42 · besluit gebruiker (V1 = A): geen grijs steen meer in wanden en grotmonden, carvers en ertsen blijven werken.
- 2026-10-07 · worldgen · Oerijs als dunne kern in de sterkste Nether-3-pilaren: ruis `nether_ring_3_pillars` ≥ 0,66, Y 45–95, blauw ijs (≥ 0,62) eromheen; ~60 pilaren in heel Nether-3, kernen meestal 1–3 breed (soms tot ~6) · besluit gebruiker (herziene V2 = A). Vervangt de eerdere keuze 0,72: die bleek vrijwel leeg (3 blokken in 576 chunks), want maar 3–4 van ~30 grote pilaren per gebied halen 0,62 en geen enkele 0,70; een kern in élke pilaar kan een surface-regel niet (geen midden van een pilaar).
- 2026-10-07 · worldgen · Geblakerde vlekken in `dry_riverbeds` naar golf 2 (47*); `ash_layer` wordt een feature (47); `ash_crust` in de surface alleen op daken van één blok boven een holte (review 2: een dikker dak op vast gesteente valt nooit) · akkoord gebruiker via main.
- (implementatie, akkoord main) Surface-conditie `volcanic_threshold` op de kolomwaarden van het ring 3-terrein (krater-binnenkant, lavaoevers via `lava_bank`, kegels via `volcano_rise`); vulkaankegels in elk ring 3-biome dezelfde stijl: horizonreuzen staan ook in zwavel, smeulwoud en tot in de zeekom.
- (implementatie) Gedeelde ring 3-takken vóór (vulkaan) en na (ondergrond) de biome-takken, gegenereerd door `tools/worldgen/ring3_surface.py`; `Ring3BiomesTest` staat er hoogstens één vóór en één na toe.
- (implementatie) Bloksoorten per biome en drempels zoals in "Gebouwd"; buiten de contractlijst gebruikt 39c `tuff`, `calcite`, `gravel`, `clay`, `sand`/`sandstone` en `yellow_terracotta` als vanilla-accenten.

## Backlog
- `worldgen` (47): `ash_layer` als feature (aslagen, voetsporen) in `ashen_wastes`/`ember_woods`; geblakerde vlekken in `dry_riverbeds` naar as/verkoold hout (backlog r. 122).
- `worldgen` (42): grotten binnen de vulkanen en dieper dan het pakket tonen vanilla steen. Lavabuizen en magmakamers (42) bepalen daar het beeld; eventueel het pakket in grotwanden doortrekken.
- `worldgen` (53b): als de Nether-3-biomes komen, de tak van `placeholder_nether_ring_3` per biome splitsen (zelfde blokken). Oerijs "in elke pilaar" kan alleen met een feature die het midden van een pilaar kent; nu alleen in de sterkste pilaren (≥ 0,66).
- `worldgen`: `steep` is in vanilla eenzijdig (alleen noord- en oostflanken). Op kegels gebruikt 39c daarom `volcano_rise` en hoogte, niet `steep`.
- `worldgen` (47/42): instortende askorst boven grotten. De surface kan alleen ruisgrotten zien (carvers komen erna), en alleen een dak van één blok valt; dat is zeldzaam. Askorst boven carver-grotten en lavabuizen moet een feature of een stap na de carvers worden (golf 2). De W-checklist-regel "askorst stort in" wacht daarop.
- `worldgen` (afstemmen 4a): lavaoevers lopen tot 1,3 halve geulbreedte met veel magma-korrel; bij brede rivieren een brede zwarte band. Na teststop 4a bijstellen (breedte, magma-aandeel).

## W-checklist (teststop 4a, seed 20260930)
- [ ] Horizonreus met lavameer (C): ~(−3.895, 65): donkere kegel met puimsteenkap boven Y ~196, zwarte kraterwand met magma/smeulsteen rond het meer, voet in zwavel/smeulwoud.
- [ ] Pieken (E): ~(−615, −5.695): basalt-/blackstone-vlakte met as en magmascheurtjes, kraters; geiten (zeldzaam).
- [ ] Basaltstromen 's nachts (B/D): ~(4.090, −3.570) / ~(−4.050, −1.950): netwerk van gloeiende smeulsteen-scheuren.
- [ ] Lavarivier (B): ~(4.110, −3.425): blackstone-oevers met magma.
- [ ] Smeulwoud (A): ~(−4.915, 680): as/grove aarde met gloeiende aders van smeulende as.
- [ ] Zwavelvelden (B): ~(4.260, −3.400): gele korst met calciet en terracotta-halo's.
- [ ] Asvlakte, zeekom, titanenveld, slapende krater (A): asvlakte ~(−4.750, 650), zeekom ~(−4.665, 830), slapende krater ~(−4.915, 560): grijze as met puimsteenvlekken, zand met asduinen, zielgrond, groene krater met puimsteenwand.
- [ ] (wacht op golf 2, zie Backlog) Askorst boven grotten stort in.
- [ ] Wanden van kloven en kuilen: as/tuf/basalt, geen grijs steen in de bovenste ~15 blokken.
- [ ] Nether-3 (`/ca tpring 3 nether`, ~(1.180, 0)): koudlava-zee met ijsplaten, pilaren met Nether-ijshuid en blauwe kern, gloeiende scheuren (licht), berijpt rijpwoud; oerijs in een sterke pilaar (Y 45–95).

## CODEBASE-delta
- **Belangrijke classes:**
  - `worldgen/surface/VolcanicThresholdCondition` — surface-conditie `corruptedadventure:volcanic_threshold` (`output` = een kolomuitgang: `caldera_mask`, `caldera_dormant`, `lava_channel`, `island_mask`, `roughness`, `volcano_rise`, `lava_bank`; `min`, `max`; waar = `[min, max)`). Leest het veld van de noise chunk via `RingChunkGenerator.surfaceVolcanicField()` en onthoudt de laatste kolom; zonder veld onwaar + één WARN per random state;
  - `RingChunkGenerator`: `surfaceVolcanicField()` (statisch, thread-local): het `volcanic_field` van de noise chunk waarvan deze thread de surface of carvers bouwt (`withSurfaceField` rond `buildSurface`/`applyCarvers`), of null;
  - `VolcanicField.Column`: + `rise`, `lavaBank`; `VolcanicOutput`: uitgangen `volcano_rise` (0–320) en `lava_bank` (0–1), `columnValue(Column, Output)`, `Output.perColumn()` (allow-list).
- **Registries:** `ModSurfaceConditions` blok `// ring 3 — W-biomes`: `volcanic_threshold`.
- **Databestanden:**
  - `noise/ring_3_surface_{patches,detail,cracks,grain}.json` (zie "Gebouwd");
  - `noise_settings/overworld.json`, `above_preliminary_surface` na `dry_riverbeds`:
    - [7] de gedeelde vulkaantak (alle 8): krater-binnenkant, slapende kraterwand, kegel, lavaoevers;
    - [8–15] de 8 ring 3-biometakken;
    - [16] de gedeelde ondergrondtak (alle 8): het vulkanische pakket onder de surface-diepte;
    - gegenereerd met `tools/worldgen/ring3_surface.py` (idempotent: verwijdert bestaande gedeelde ring 3-takken eerst);
  - `noise_settings/nether.json`: tak `placeholder_nether_ring_3` met de ring 3-blokken (vervangt de vanilla-voorlopige surface van 53).
- **Build en configuratie / tools:** `tools/worldgen/ring3_surface.py` (zie "Gereedschap").
- **Controlelijst "Plekken die ring 3-biomes opsommen":** een nieuwe ring 3-biome moet in `RING_3` en `BIOMES` van `tools/worldgen/ring3_surface.py` (gedeelde takken + eigen tak), daarna het script draaien.
- **Tests:** `worldgen/surface/VolcanicThresholdConditionTest`; `Ring3BiomesTest` (gedeelde takken: alleen ring 3-biomes, hoogstens één vóór en één na de biome-takken, vóór vanilla, zonder gras/aarde; geen biome met twee takken).
- **Valkuilen:**
  - de rand van een vlek uit een gladde ruis (een smalle `noise_threshold`-band) leest als een lange lijn; gebruik een halo met korrel-ruis;
  - `minecraft:steep` kijkt alleen naar noord- en oostflanken;
  - een generatorscript dat takken invoegt moet idempotent zijn: bij een tweede run bleef een oude kopie staan die vóór de nieuwe kwam en altijd won (review 39c).
