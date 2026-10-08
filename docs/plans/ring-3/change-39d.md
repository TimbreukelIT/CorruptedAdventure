# Change 39d — ring 3: reliëf, vorm per biome, meer vulkanen, getraceerde lavarivieren en fantasy-landvormen

Scope: `worldgen`. Fix-change na teststop 4a ("de hele ring voelt plat"; feedback en besluiten in `teststop-4a-feedback.md`). Eigen worktree vanaf main `bf63d17`; de gebruiker test intussen de rest van 4a op main.

## Doel
- Ring 3 krijgt **reliëf**: hooglanden en laagtes, en een eigen vorm per biome.
- **Meer vulkanen**: overal zichtbaar, met middelgrote kegels.
- **Getraceerde lavarivieren**: vanaf de vulkanen bergaf.
- **Fantasy-landvormen**: archipels, een zwevende vulkaan, bogen, naalden en een afgrond.

Alles deterministisch per seed, als vormen in de density (geen neighbour-chunk-placements), binnen het prestatiebudget (≤ 1,75× vanilla in de noise-stap).

## Plan
**Bestanden**
- `VolcanicField` stelt de kolom samen.
- Nieuwe klassen:
  - `Ring3Relief`: hooglanden en de landvorm per biome;
  - `CellShapes`: hoodoos, heuveltjes, zinkgaten, zeepilaren en zeshoekige zuilen;
  - `CinderCones`;
  - `TitanBones`;
  - `LavaRivers`;
  - `Landmarks`: zwevende vulkaan, afgrond, naalden en bogen.
- `VolcanicSettings`, `VolcanicOutput` (nieuwe uitgangen `cliff`, `solid_kind`, `thin`).
- `Volcano`: pilaren en bronnen op eilanden.
- JSON: `field.json` (`relief`-object), 4 nieuwe noises, `ring_3/overworld.json` (`thin` buiten de interpolatie).
- Surface: `ring3_surface.py` (kliffen, dunne vormen, smallere oevers) en de gegenereerde `overworld.json`.
- `RingAquifer`: twee kleine wijzigingen, gemeld aan main (39e raakt dit bestand ook):
  - een lavabron stroomt (scheduled update);
  - geen korst tegen een buurkolom die zelf lava heeft (de lavavalletjes).

**Client/server-impact.** Alleen worldgen (server-side) en data; de client rendert alleen blokken. Geen netwerk, geen client-code.

## Ontwerp (implementatiekeuzes binnen de besluiten)
**Vlakte**
- `plain_y` 89, deining ±16 (`plain_amplitude`), golving ±7 (`rolling_amplitude`).
- Hooglanden `ring_3_highlands` (firstOctave −10) via een gladde curve −15…+45.
- Heuvels tot 40.
- Het reliëf groeit over 240 blokken achter zone 2→3 (rand = +8), zodat de w²-overgang geen klif krijgt.

**Vorm per biome**
- Elke landvorm heeft een "binnengewicht": 0 op de biomegrens, 1 een stukje (0,07 regio-ruis, ~50 blokken) erbinnen. Zo eindigt geen vorm in een muur.
- Niveaus per biome (blokken t.o.v. de vlakte):

  | Biome | Niveau |
  |---|---|
  | pieken | +35 |
  | stromen | +10 |
  | smeulwoud | −6 |
  | zwavel | −14 |

- **Glad** (onderdeel van de vlakte; vulkanen staan erop, dus kammen lopen de flank op):
  - piekkammen (geribde ruis, tot +55);
  - lage titanenribben (bestaand);
  - zeekom met rifruggen (tot 6).
- **Detail** (vervaagt op vulkaanflanken):
  - asduinen 10–25 (wind 0,35 rad, afstand 120, steile lijzijde) met geulen 8–13 diep en hoodoos 5–15;
  - zwavelterrassen (stappen van 3 met randje), fumarole-heuveltjes 3–6 en zinkgaten 8–16;
  - basaltplateaus (3 treden van 12) met zeshoekige zuilen op de stootborden, en lavabuizen (sleuf of tunnel);
  - riftdalen 20–40 diep met getrapte flanken, niet in zeekom, titanenveld of zwavelbekken.
- **Slakkenkegels:** raster 112, 10–40 hoog, in velden. Niet bij vulkanen, rivieren of rifts.
- **Titanenveld:** losse ribben (bogen 20–60, raster 96, een derde gebroken) en zeldzaam een ribbenkast met wervels (raster 384).
- **Ruwheid:**
  - Eerst een basis van 0,4 op de vlakte. Na de eerste prestatiemeting (zie hieronder) vervangen: geen 3D-crags op de vlakte, maar een 2D-microreliëf `Ring3Relief.microRelief` (±2 blokken, `ring_3_detail` op schaal 1,3).
  - Crags alleen bij roughness ≥ 0,15 (steile vulkaanwanden, kliffen, slakkenkegels); daaronder 0, dan slaat de `mul` de 3D-ruis over.
  - Steile wanden krijgen `cliff` (rots in de surface).

**Vulkanen**
- Kans in as/smeulwoud +0,23 en in zwavel +0,28 (was 0,07/0,15).
- Kegels: rand 80–140 boven de vlakte, nooit hoger dan `great_min_y` − 10.
- Reuzen Y 250–285.
- Een kegel staat bij de kraterrand op de vlakte van zijn middelpunt (het kratermeer blijft dicht), aan de voet op de lokale vlakte.

**Lavarivieren** (`LavaRivers`)
- Per actieve vulkaan 1–2 rivieren, vanaf 0,8 × de voetstraal bergaf getraceerd. De ondergrond is die zonder rivieren en slakkenkegels, met vulkanen, rifts en de afgrond.
- Stap 8 blokken, 7 richtingen met draaistraf, meander uit `ring_3_warp`.
- Het niveau is grond − 2 en daalt alleen.
- Einde in een poel:
  - in een kom (10 stappen zonder daling);
  - voor een rug (> 22 kloof);
  - bij zeekom, titanenveld of zone;
  - na 170 stappen.
- Breedte 6 → 16. Dijkjes heffen lagere grond naast de rivier.
- Index per tegel van 128 blokken, gedeeld per wereld.

**Fantasy**
- **Archipel:** 20 % van de actieve caldera's met rand ≤ 165. Hoofdeiland 60–100 breed met 2–3 lavabronnen, 2–5 kleinere eilanden, 1–2 basaltpilaren naar de grond.
- **Zwevende vulkaan:** raster 2.000, kans 0,45. Eiland r 40–55 op Y ~200–212, kegel +38–50 met kratermeer, 3–4 bronnen op de rand.
- **Afgrond:** raster 1.600, kans 0,4. 300–500 lang, 36–60 breed, bodem Y 2, lavazee tot Y 9, 1–3 natuurlijke bruggen.
- **Obsidiaannaalden:** raster 192, kans 0,3, 60–120 hoog.
- **Bogen:** over rivieren (een paar per rivier) en over riftdalen (raster 256, kans 0,5).
- **Zeshoekige zuilen:** op de stootborden van de basaltplateaus.

## W-checklist
- [ ] `./gradlew build` groen (unit tests), `runData` zonder diff na de surface-generator
- [ ] Unit tests: bereiken en determinisme van de nieuwe vormen; rivierniveau daalt nooit; eilanden en archipels vrij boven het terrein; kratermeren dicht
- [ ] Vóór- en ná-kaarten (`build/c39d/before_*`, `after_*`) met de commando's van `teststop-4a.md` §A + detailkaarten pieken/stromen
- [ ] Vóór- en ná-screenshots (pieken, basaltstromen, asvlakte, smeulwoud, zwavel, titanen, zeekom, horizon) + rift, slakkenkegelveld, landmarks
- [ ] Determinisme: twee verse werelden seed 20260930, ring 3 A–E identiek (op vloeistofstroming na)
- [ ] Prestatie: JFR noise-stap mod/vanilla, warm-up, ≥ 3 rondes, mediaan ≤ 1,75×; tracing-tijd rivieren
- [ ] Smoke: runServer + client, `/ca tpring 3`, `tpboundary 2|3`, portal ring 3 → Nether-3
- [ ] Zone 2→3 zonder klif, horizonreuzen boven de hooglanden, eilanden vrij, slapende caldera, `ring3Seal`
- [ ] CODEBASE, design-doc, beslissingenlog, feedback-doc; review; commit

## Resultaten
- **Unit tests:** 552 tests, alle groen. `runData` geeft geen diff in `src/generated`.
- **Model (seed 20260930):**
  - vlakte gemiddeld Y 113, bulk Y 64–160, piekenketen tot ~Y 240;
  - 212 vulkanen (57 reuzen, 105 kegels, 50 caldera's; 57 slapend);
  - 211 rivieren, 84 km, gemiddeld ~400 blokken; tracing van alle rivieren van de ring 0,2 s (één keer per wereld, gedeeld door alle chunks);
  - 26 eilandclusters op vulkanen + 5 boven rivieren;
  - 205 slakkenkegels, 234 naalden, 104 losse ribben, 121 bogen over rivieren, 3 over rifts.
- **Landmarks:**

  | Landmark | Coördinaten |
  |---|---|
  | zwevende vulkanen | (−247, 4276) en (1671, 3659) |
  | afgronden | (4531, −413) en (5120, 2354) |
  | archipels | (2622, −2738), (4993, −2124), (5494, 1413) |
  | ribbenkasten | (−2994, 2546), (−1485, 3911), (−1493, 4144), (−1161, 4327) |
  | boog over rift | (−4047, −4054) |
  | boog over rivier | (−5171, 856) |
  | naald | (−5625, 618) |
  | slakkenkegel | (5523, 399) |

- **Smoke** (`c39d-det-a`, server + client): `tpring 3`, `tpboundary 2` (zone 2→3) en `tpboundary 3` werken. Portal op (4755, 89, 16) in ring 3 → Nether (1197, 36, 9), zoals in integratie 1.
- **Determinisme** (`c39d-det-a` vs. `c39d-det-b`, omgekeerd gegenereerd):
  - ring 3 A–E 2.844/2.880 chunks identiek;
  - de rest alleen lucht/water/lava en 6 obsidiaan (stroming: lava raakt water);
  - integratie 1 had 2.847/2.880.
- **Kaarten:** `build/c39d/before_*`, `after1_*`. **Screenshots:** `before_shot_*`, `after_shot_*`.
  - De landmark-shots vielen weg: de client verloor de verbinding, want de server hing 30 s op `save-all flush` van de parallelle generatie.

## Voortgang
Voortgang: 19:00 vóórkaarten en vóór-screenshots op bf63d17 (`build/c39d/before_*`).
Voortgang: 19:40 code reliëf, biomevormen, vulkanen en rivieren geschreven; fantasy-landvormen geschreven (wachten op een JVM-slot: 39e meet).
Voortgang: 19:57 eerste ná-kaarten (`after1_*`) naar main.
Voortgang: 20:31–20:56 tests + runData, smoke, screenshots, determinisme.

## Afronding (2026-10-08, na rebase op 099967b met 39e)
**Rebase.** Op main 099967b (39e).
- `RingAquifer`: de bron en het hele lavabereik per kolom (`lavaBottom..lavaTop`, `springY`) staan in de per-kolomtabel van 39e. Daarvoor zijn `Column.lavaBottom()`/`springY()` publiek.
- De surface is opnieuw gegenereerd. De `ring_reach`-wrap van 39e blijft staan; de diff t.o.v. main raakt alleen de ring 3-takken.

**Build.** `./gradlew build` groen (volledige suite). `runData` zonder diff.

**Prestatie.** JFR, stap `noise`, 16 × 16 chunks per gebied, mod tegen vanilla-datapack in één server, verse wereld per ronde.
- Vóór = integratie 1 op bf63d17 (`teststop-4a.md` §E: 7 rondes, warm-up).
- Ná = 39d op 099967b: 5 warme rondes (mediaan, min–max) en 1 koude ronde (zonder warm-up).

| Gebied | Vóór (bf63d17) | Ná: mediaan | min–max | koud | Surface ná |
|---|---|---|---|---|---|
| ring 1 | 1,23 | 1,11 | 0,76–1,32 | 0,93 | 1,46 |
| ring 2 | 1,45 | 1,48 | 1,11–1,85 | 1,06 | 2,84 |
| horizonreus | 1,55 | 1,63 | 1,09–1,71 | 1,27 | 2,78 |
| caldera | 1,14 | 1,47 | 1,13–1,61 | 1,27 | 2,20 |
| lavarivieren | 1,36 | 1,41 | 0,96–2,11 | 1,33 | 2,19 |
| asvlakte | 1,20 | 1,50 | 1,00–1,54 | 1,22 | 1,87 |
| zeekom | 1,35 | 1,51 | 1,01–1,54 | 1,14 | 1,55 |
| titanenveld | 1,20 | 1,48 | 1,02–1,51 | 1,37 | 1,73 |
| pieken | 1,21 | 1,56 | 1,02–1,60 | 0,87 | 2,20 |
| basaltstromen | 1,41 | 1,49 | 0,91–1,66 | 1,12 | 1,87 |
| smeulwoud | 1,44 | 1,63 | 1,59–1,69 | 1,36 | 1,91 |
| zwavelvelden | 1,24 | 1,66 | 1,17–2,02 | 1,12 | 2,49 |
| slapende krater | 1,55 | 1,50 | 1,05–1,56 | 1,10 | 1,78 |
| Nether-1 | 1,33 | 1,23 | 1,18–1,57 | 1,19 | 1,10 |
| Nether-2 | 1,61 | 1,46 | 0,87–1,52 | 1,12 | 0,76 |
| Nether-3 (4 gebieden) | 1,16–1,47 | 1,28–1,40 | 0,98–1,85 | 1,01–1,20 | 0,94–1,27 |

- Alle medianen ≤ 1,75 (zwavel 1,66 is het hoogst).
- De meting van gisteren, zonder de optimalisaties en op 10 × 10, gaf ~1,67 (reus en smeulwoud ~1,8). De `cache_once` op het terrein, het 2D-microreliëf in plaats van de crags op de vlakte, het eenmalige tracen van de rivieren en `thin` hebben dat teruggebracht.
- Losse rondes blijven ruizig: ronde 12 was ook voor vanilla traag. De koude ronde zat overal ≤ 1,37.
- De surface-stap ring 3 is 1,5–2,8× vanilla. Die valt buiten het budget en is vergelijkbaar met integratie 1 (2,4–4,2×).

**Smoke** (`c39d-final`, server + client): `tpring 3` (−4686, 100, 779), `tpboundary 2` (zone 2→3) en `tpboundary 3` werken. Portal op (4755, 89, 16) → Nether (1197, 36, 9).

**Determinisme** (`c39d-det2-a`, en `c39d-det2-b` omgekeerd gegenereerd):
- ring 3 A–E 2.855/2.880 chunks identiek;
- de rest alleen lucht/water/lava en 6 obsidiaan (stroming, lava raakt water);
- integratie 1 had 2.847/2.880.

**Beelden** (in `build/c39d/`):
- kaarten `after2_top_*` en `after2_section_*`;
- screenshots `final_shot_*` (23): de 8 biomes, horizon, zone 3→4/reus, zwevende vulkaan, afgrond, archipel, ribbenkast, losse rib, naald, slakkenkegel, bogen over rift en rivier, rivier van boven, as en smeulwoud vanaf de grond. Eerst gegenereerd en daarna gefotografeerd (geen timeout).

**In-game bevindingen, opgelost:**
- een naald stond in een afgrond, en slakkenkegels konden dat ook (`nearAbyss`);
- een ribbenkast stond op een horizonreus; ribben en ribbenkasten staan nu niet meer op vulkanen.

**Traplijnen.** Op de as- en smeulwoudvlakte zijn de hoogtelijnen nu golvend en onderbroken, niet meer recht (`final_shot_ashen_ground`, `final_shot_ember_ground`).

**Landmarks (seed 20260930):**

| Landmark | Coördinaten |
|---|---|
| zwevende vulkanen | (−247, 4276) en (1671, 3659) |
| afgronden | (4531, −413) en (5120, 2354) |
| archipels | (2622, −2738), (4993, −2124), (5494, 1413) |
| ribbenkasten | (−1493, 4144), (−1161, 4327) |
| boog over rift | (−4047, −4054) |
| boog over rivier | (−5168, 829) |
| naald | (−5625, 618) |
| slakkenkegel | (5325, −1030) |

**Reviewbevindingen 2026-10-07:** alle 10 opgelost (zie de status hieronder).

**Review 2 (`/code-review medium`, 2026-10-08), alle 10 verwerkt:**
1. Preliminary surface weer een echte ondergrens: drie ringen (6/12/18) en per monster `floorMargin` (de diepte van smalle insnijdingen). In-game: afgrondwanden en riftbodem hebben hun surface (`check_shot_abyss_floor`, `check_shot_rift_floor`).
2. Rivieren en tegels via een per-sleutel-`FutureTask`, buiten de locks van de map.
3. `nearAbyss` met fysieke afstand en de straal van de vorm (slakkenkegels, naalden).
4. `open()`: lava buiten het bereik van de buur (`y < lavaBottom`) telt als geen lava (korst).
5. Pilaren uit een eigen hash.
6. Dunne vormen eindigen onder Y 296; een naald op een hoge vlakte wordt ingekort met zijn punt (`check_shot_needle_peaks`).
7. Lavabronnen staan op een eigen dun blok met een dunne opening.
8. Scratch-kolommen voor de ongecachete vullingen.
9. `LavaRivers.Cache` is weg; één lavaregel `Column.lavaAt(y, top, bottom, spring)`, ook in `RingAquifer`.
10. `VolcanicField` is gesplitst in `VolcanoPlacement`, `ThinShapes` en `VolcanicCaches` (gedrag-neutraal).

**Ná review 2**
- Determinisme A–E: 2.855/2.880 identiek, de rest alleen vloeistof.
- Prestatie (noise, 16 × 16, warm, mediaan van rondes p21–p23):

  | Gebied | Mediaan |
  |---|---|
  | ring 1 | 1,27 |
  | ring 2 | 1,64 |
  | reus | 1,18 |
  | caldera | 1,59 |
  | rivier | 1,20 |
  | as | 1,45 |
  | zeekom | 1,40 |
  | titan | 1,10 |
  | pieken | 1,10 |
  | basalt | 1,16 |
  | smeulwoud | 1,50 |
  | zwavel | 1,25 |
  | slapend | 1,09 |
  | Nether-1 | 1,20 |
  | Nether-2 | 1,45 |
  | Nether-3 | 1,11–1,23 |

  p21 was ruizig: ook vanilla was traag.

**Open punten**
- Kruinen van bogen en ribben krijgen de biome-surface (`solid_kind` alleen waar de vorm op de grond staat): de ribben zijn donker met bleke voeten.
- De bogen over rifts zijn zeldzaam (3 in de ring).
- De surface-stap is 1,5–2,8×; tips van 39e (goedkope condities eerst) zijn nog niet toegepast.

## Status bij onderbreking (2026-10-07, 21:50; historisch)
39d is **niet gecommit en niet gemerged**: de prestatie lag boven het budget. Besluit gebruiker (b): eerst optimaliseren. Morgen rebaset 39d op de gedrag-neutrale optimalisatie van 39e en meet dan opnieuw, samen met 39e.

**Meetcijfers vóór de optimalisatie.** JFR, stap `noise`, 10 × 10 chunks per gebied, warm-up over alle ringtypes, mod tegen vanilla-datapack in één server.

| Gebied | Ronde 1 | Ronde 2 |
|---|---|---|
| ring 1 | 1,26 | 1,38 |
| ring 2 | 1,43 | 1,48 |
| reus | 1,76 | 1,81 |
| caldera | 1,67 | 1,72 |
| rivier | 1,72 | 1,66 |
| as | 1,55 | 1,68 |
| zeekom | 1,68 | 1,70 |
| titan | 1,71 | 1,63 |
| pieken | 1,47 | 1,33 |
| basalt | 1,66 | 1,68 |
| smeulwoud | 1,80 | 1,81 |
| zwavel | 1,66 | 1,68 |
| slapend | 1,65 | 1,69 |

Ring 3-mediaan ~1,67×; reus en smeulwoud ~1,8× (budget ≤ 1,75×). Integratie 1 mat 1,14–1,55×, maar met 16 × 16 chunks en 7 rondes.

**Al geoptimaliseerd** (alleen unit tests groen, nog niet gemeten):
- `ring_3/overworld/terrain.json` in `cache_once`: de stapel leest het terrein vier keer per celhoek.
- Geen 3D-crags meer op de vlakte, maar een goedkoop 2D-microreliëf. Crags pas vanaf roughness 0,15; daaronder 0, dan slaat de `mul` de ruis over. Smeulwoud betaalde de crags overal; dat was waarschijnlijk de grootste post.
- `initialHeight`: 17 in plaats van 25 kolommen per kwartier (ringen 7 en 14).
- `solids` en `voids` samengevoegd tot één per-blok-uitgang `thin`: één kolomlookup per blok.
- Rivieren en riviertegels via `computeIfAbsent`: elke rivier wordt één keer getraceerd. Eerst traceerden alle worldgen-threads dezelfde rivieren tegelijk, een CPU-piek bij binnenkomst in ring 3. Tracing hergebruikt één kolom.
- Celbeslissingen hebben een memo per thread.

**Reviewbevindingen (`/code-review medium`)**

| # | Bevinding | Status |
|---|---|---|
| 1 | salt-race (determinisme) | opgelost: een onveranderlijke holder |
| 2 | `PAD` van de riviertegels te klein (rechte trede op de 128-tegellijn) | opgelost: `PAD` = 4 × max. halve breedte × 1,8 + 8; `nearest` leest tot `PAD` |
| 3 | `solid_kind` kleurde de grond onder bogen en ribben | opgelost: alleen waar de vorm op de grond staat. Gevolg: de kruin van een boog krijgt de biome-surface (as) i.p.v. calciet/basalt |
| 4 | slakkenkegel over een rivier | opgelost: rivier gecontroleerd op 8 punten van de voet + het midden |
| 5 | brughoogte afgrond | opgelost: per brug de laagste van beide randen (gladde vlakte daar) |
| 6 | bronhoogte zwevende vulkaan zonder warp | opgelost: dezelfde warp |
| 7 | geen korst bij kruisende rivieren | opgelost: `open()` behandelt alleen lava ≤ 1 blok lager als trede |
| 8 | rivieren dubbel getraceerd | opgelost: `computeIfAbsent` + scratch-kolom |
| 9 | archipelpilaren onder een afgewezen eiland | opgelost: alleen als het hoofdeiland blijft |
| 10 | bot-solids handmatig | opgelost: via `addSolid(…, KIND_BONE)` |

**Volgende stappen**
1. Rebase op main met 39e.
2. Volledige `./gradlew test` en `runData`.
3. Prestatie meten (warm-up + ≥ 3 rondes, liefst 16 × 16 zoals integratie 1). Daarna, als het nodig is:
   - landvormen alleen evalueren als hun binnengewicht > 0 (gebeurt al);
   - `plain()` goedkoper: heuvels en asruggen uit één monster;
   - `initialHeight` uit de `TRACE`-modus;
   - de prijs van de reus bekijken: veel hoge cellen, crags op de flanken.
4. In-game:
   - traplijnen in `ashen_wastes` en het smeulwoud opnieuw beoordelen (2D-microreliëf vs. crags);
   - de landmark-screenshots opnieuw maken: ze mislukten door een client-timeout tijdens `save-all flush` van de parallelle generatie;
   - de helling van de piekenketen naar zone 3→4 bekijken (~0,8).
5. Determinisme opnieuw na de wijzigingen. Vóór de fixes: A–E 2.844/2.880 identiek, rest alleen vloeistof.
6. Review-doorsturen van de nieuwe diff, dan commit.

**Open punten**
- De `thin`-kruinen van bogen krijgen de biome-surface (gevolg van fix 3). Een exacte oplossing vraagt een surface-conditie op Y t.o.v. het solid-interval.
- `Scratch39dTest.java` is een kladtest en hoort niet in de commit.

