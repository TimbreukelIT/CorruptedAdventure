# Teststop 4a — terrein-wow ring 3 + Nether-ring 3 (checklist)

Scope: blokkerende teststop na integratie 1 (`waves.md` §2, "Teststop 4a"). Stand: main na 39c (`14d1eb1`) plus de docs van integratie 1. Golf 2 begint pas na akkoord van de gebruiker.

**Beoordeeld wordt:** de terreinvormen, de lucht op de grens, de biomeverdeling met de echte blokken, de ringbreedte en een eerste blik op Nether-3. **Nog niet:** features (bomen, skeletten, flora, ertsen), sfeer, weer en hitte. Die draaien al mee, maar worden in 4b beoordeeld.

Opzet: twee sessies van ~45 min.
- Sessie 1: eerst de kaarten (A), dan horizon, grens en ringbreedte (B).
- Sessie 2: vormen en biomes van dichtbij (C), Nether-3 (D) en prestatie (E).

Elke sectie heeft afstemvragen. Antwoorden komen in `docs/plans/ring-3/teststop-4a-feedback.md` en waar nodig in de beslissingenlog.

## Voorbereiding
- **Wereld:** vers, `level-type=corruptedadventure:corrupted_world`, seed **`20260930`** (singleplayer of `runServer` + `runClient`). Oude werelden van vóór 39b laden niet meer (`placeholder_ring_3` is weg).
- **Verse `run/`:** een `options.txt` met `onboardAccessibility:false` (en voor quick-play `skipMultiplayerWarning:true`), anders hangt de client achter het toegankelijkheidsscherm.
- **Tijd:** `/time set noon` en `/gamerule doDaylightCycle false`. Gloed 's nachts hoort bij 4b, maar mag even.
- **Weer:** `/ca weather 3 clear 6000`, zodat het weer het terrein niet verbergt.
- **Bewegen:** creative, vliegen. `/ca ring` toont ring en zone.

## A. Terreinkaarten (vóór de game; PNG's uit `/ca terrainmap`)
Gemaakt in integratie 1 op seed 20260930 (wereld `int1b-det-b`), uit de density functions, dus zonder chunks. De kaarten staan in **`build/teststop-4a/`** (niet in git; opnieuw te maken met dezelfde commando's via RCON of de chat). Beperkingen (37d):
- geen celinterpolatie (randen ≤ 1 blok anders);
- geen aquifers: lava in kraters en rivieren en open water onder een overhang staan er niet in;
- geen surface en geen features;
- eilanden dunner dan 8 blokken mist de top-view.

| Bestand | Commando | Waar op letten |
|---|---|---|
| `top_overworld_0_0_r6000_s16_height.png` / `_biome.png` | `/ca terrainmap 0 0 6000 16` | **Hele ring 3** (r 3.500–6.000) tussen ring 2 en de placeholder van ring 4. Rode ringen = vulkanen; de dichtste band ligt net achter zone 2→3 (horizonreuzen). Donkerblauw = de zeekom (Y 30–45, geen water). Biome-aandelen in de legenda zijn % van de hele kaart (ring 3 ≈ 51 % ervan); als aandeel van ring 3: ember 19,8 %, sulfur 17,1 %, basalt 14,7 %, peaks 14,3 %, seabed 12,5 %, ashen 11,6 %, titan 9,6 %, dormant 0,4 % (zie vraag 2) |
| `top_overworld_-3900_-600_r900_s3_height.png` / `_biome.png` | `/ca terrainmap -3900 -600 900 3` | Detail west: horizonreus met lavameer (~−3.895, 65), caldera's, flankgeulen, rivieren |
| `top_overworld_-4800_600_r512_s2_height.png` / `_biome.png` | `/ca terrainmap -4800 600 512 2` | Detail asvlakte, zeekom, titanenveld, slapende krater (39c gebied A) |
| `top_overworld_4150_-3500_r512_s2_height.png` / `_biome.png` | `/ca terrainmap 4150 -3500 512 2` | Detail oost: basaltstromen, zwavelvelden, lavarivier (39c gebied B) |
| `section_overworld_3200_0_4400_0.png` | `/ca terrainmap section 3200 0 4400 0` | **Horizon vanuit `dry_riverbeds`**: de vlakte van ring 2/zone op Y ~95–125, dan een kegel tot Y ~245 bij x ~3.850 (boven de wolken, Y 192) |
| `section_overworld_-4400_65_-3400_65.png` | `/ca terrainmap section -4400 65 -3400 65` | Doorsnede van de horizonreus met kratermeer |
| `section_overworld_-5000_885_-4780_885.png` | `/ca terrainmap section -5000 885 -4780 885` | Zwevend eiland (omgekeerde kegel op Y ~183–197) boven `sulfur_fields`; de donkere kolom eronder is de open lucht onder het eiland |
| `section_overworld_4100_-3592_4220_-3592.png` | `/ca terrainmap section 4100 -3592 4220 -3592` | Klein eiland (~12 × 9) op Y ~187–192 boven een lavarivier |
| `top_the_nether_0_0_r1500_s4_height.png` / `_biome.png` | `/execute in minecraft:the_nether run ca terrainmap 0 0 1500 4` | Nether-ring 1–3. In Nether-3 (r 875–1.500): vloer, pilaren, rijpwoud-hallen en gletsjermassa. Nether-top-views tonen de bodem van de hoogste opening |
| `section_the_nether_850_0_1520_0.png` | `/execute in minecraft:the_nether run ca terrainmap section 850 0 1520 0` | Doorsnede van Nether-3: vloer Y ~35, plafond ~108, pilaren van vloer tot plafond, geen lavazee |

**Afstemvragen A**
1. Aantal en verdeling van de vulkanen: is de horizonband net achter de grens dicht genoeg, en de rest van de ring niet te vol of te leeg?
2. Zijn de biomevlakken groot genoeg (~700 blokken), en is `dormant_caldera` (alleen in slapende kraters, 0,4 % van ring 3) zeldzaam genoeg of juist te zeldzaam?
   - **Open punt: scheve verdeling op deze seed.** Ontworpen is elk biome ~1/7 (14,3 %); op seed 20260930 is smeulwoud 19,8 % en titanenveld 9,6 % van ring 3 (zwavel 17,1 %, as 11,6 %). Dat is seed-ruis, geen fout: dezelfde aandelen mat 39b al op deze seed (ember 20,2 %, titan 9,9 %), over veel seeds en een groot vlak is elk biome 14,2–14,4 % (`Ring3RegionSharesTest`), en op seeds 1 en 42 liep het in de ring van 8,5 tot 22,9 %. Oorzaak: de regio-ruis (`ring_3_heat`/`ring_3_ground`, firstOctave −11) heeft vlakken van ~700 blokken, en de ring is maar 2.500 breed, dus per wereld liggen er maar een handvol vlakken per biome. Het terrein dwingt geen biome af, behalve `dormant_caldera` (weirdness uit de slapende kraters). Is dit verschil per wereld goed, of moeten de vlakken kleiner (hogere firstOctave: meer, kleinere vlakken, gelijkere aandelen) of moet de indeling anders?
3. Zeekom (drie kustterrassen) en titanenveld (lage ribben): herkenbaar als eigen vorm?

## B. Horizon, grens en ringbreedte (sessie 1, in-game)
- [ ] **Horizon-vulkanen vanuit `dry_riverbeds`.** `/ca tpboundary 2` (of `/tp @s 3450 ~ 0`), oostwaarts kijken.
  - Verwacht: de reus bij x ~3.850, z ~0, rand Y ~245, boven de wolken.
  - Ook langs de rest van de grens rondkijken: horizonreuzen staan in elke regio behalve de zeekom.
- [ ] **Lucht slaat om op de grens.** Van `dry_riverbeds` naar ring 3 lopen of vliegen.
  - De lucht en fog gaan van de ring 2-tinten naar asrood/grijs. Elk ring 3-biome heeft een eigen voorlopige tint; de definitieve kleuren komen in 50.
  - Geen klif of zwevende brokken in zone 2→3: ring 3 mengt met w² naar de hoogte van ring 2, de beddingen lopen uit vóór ring 3.
- [ ] **Ringbreedte.** `/ca tpring 3` (midden, r 4.750), `/ca tpboundary 3` (grens 3→4, r 6.000).
  - Ring 3 is 2.500 breed (3.500–6.000; standaardradii sinds 37c). Voelt de breedte goed bij deze terreinschaal?
  - Bij 3→4 begint de placeholder van ring 4 (groen, vlak). Ring 3 blijft daar droog: `ring3Seal`, zie backlog.
- [ ] **Grens 3→4.** Geen water dat tegen lucht staat. Een dip onder Y 63 aan de ring 3-kant is rots (bewust, tot ring 4 er is).

**Afstemvragen B**
4. Is het wow-moment er vanuit `dry_riverbeds` (hoogte en afstand van de reuzen)?
5. Is de overgang van de lucht op de grens hard genoeg om "nu ben ik in ring 3" te voelen, maar niet storend?

## C. Vormen en biomes van dichtbij (sessie 2)
Coördinaten op seed 20260930 (x, z; Y = maaiveld). Met `/locate biome corruptedadventure:<naam>` kun je ook zoeken.

**Vulkanen en caldera's**
- [ ] **Horizonreus met lavameer** (~−3.895, 65):
  - donkere kegel met een puimsteenkap boven Y ~196;
  - zwarte kraterwand met magma en smeulsteen rond het meer;
  - voet in zwavel/smeulwoud.
  - Lava staat stil; waar ze aan open lucht zou grenzen, ligt een korst van steen.
  - **Let op trage chunks** bij het aanvliegen van de reus (eerst een tijd in ring 1/2, dan hierheen): in de meting bleef de server hier één keer 22 min hangen op een JIT-deoptimalisatiestorm (`RingFluidPicker.ring3Seal`, zie §E en backlog). Meld haperingen of chunks die lang leeg blijven (F3: ms/tick).
- [ ] **Actieve caldera** (~−3.870, −1.790; 39c gebied D): kraterbodem 20–50 onder de rand, lavameer, rafelige flanken met geulen.
- [ ] **Slapende krater** (~−4.915, 560 of ~−4.868, 496): vlakke, lagere bodem zonder lava, groen (mos/grove aarde), wand van puimsteen/as. Biome `dormant_caldera`.
- [ ] **Pieken** (~−615, −5.695; ook 4.110, 672):
  - basalt-/blackstone-vlakte met as en magmascheurtjes tussen de kegels;
  - geiten (zeldzaam, groepjes 1–3) op basalt, smooth basalt, blackstone, as of puimsteen (`#minecraft:goats_spawnable_on`).

**Lavarivieren**
- [ ] **Lavarivier** (~4.110, −3.425):
  - de rivier volgt de brede deining en snijdt als kloof door heuvels;
  - blackstone-oevers met magma-korrel dicht bij de lava;
  - de treden in de rivier zijn korst (steen) op de rand.

**Zwevende eilanden**
- [ ] Eiland boven zwavelvelden (~−4.890, 180–197, 885): ~25 × 23 breed, onderkant als omgekeerde kegel.
- [ ] Klein eiland boven een lavarivier (~4.160, 187–192, −3.595).
- [ ] Algemeen: alleen boven actieve kraters (rand ≤ 174) en rivieren; ~1 cluster per 3 kraters; geen zwevende grotresten groter dan losse blokjes.

**Vlaktes en biomes met echte blokken (39c)**
- [ ] **Asvlakte** (~−4.750, 650; 39b: 5.262, 480):
  - golvend, lage heuvels en lange asruggen;
  - grijze as met puimsteenvlekken en smooth-basalt-ontsluitingen;
  - nergens gras.
- [ ] **Zeekom** (~−4.665, 830; 39b: 5.710, −32): leeg en droog (Y 30–45), drie oude kustterrassen, zand/grind/kleipannen met asduinen en puimsteenkiezels.
- [ ] **Titanenveld** (39b: 4.942, 160): vlakte met lage ribben (gedraaide as), as met zielgrondvlekken, basaltvlekken.
- [ ] **Smeulwoud** (~−4.915, 680; 39b: 4.750, 0): as en grove aarde met gloeiende aders van smeulende as (nog zonder bomen: 47).
- [ ] **Basaltstromen** (~4.090, −3.570 / ~−4.050, −1.950; 39b: 4.814, −288): getrapte stroomlobben, netwerk van smeulsteen-scheuren (gloed 's nachts).
- [ ] **Zwavelvelden** (~4.260, −3.400; 39b: 4.270, 480): gele zwavelkorst met calciet en een halo van gele terracotta.
- [ ] **Wanden van kloven en kuilen:** as, tuf en basalt; geen grijs steen in de bovenste ~15 blokken (het ondergrondpakket, V1 = A).
- [ ] **Biomeverdeling:** geen biome dat bijna ontbreekt; de vlakken vallen samen met hun terreinvorm (pieken waar de kegels staan, zeekom en titanenveld met hun eigen vorm).

**Afstemvragen C**
6. Lavaoevers: de oevers lopen tot 1,3 halve geulbreedte en hebben veel magma-korrel. Bij brede rivieren wordt dat een brede zwarte band. Smaller of minder magma? (backlog 39c)
7. Lavatreden als korst (steen) op de rand: mooi genoeg, of liever kleine lavavallen (golf 2-feature met cap)? (backlog 39)
8. Zwevende eilanden: hoe vaak en hoe groot? Nu ~1 cluster per 3 kraters, 8–30 breed, Y 150–210 (V4).
9. Surface per biome: te monotoon of te druk? Klopt de verhouding as/puimsteen/basalt/blackstone per biome (zie de meting in `change-39c.md`)?
10. Eerste indruk van de textures (57a) in het echte terrein: smeulsteen, smeulende as, puimsteen, zwavelkorst, verkoold hout (creative). De bijsturing volgt in golf 2 (57a-rondes).

## D. Nether-3 — eerste blik (vorm en ruimte)
- [ ] **Bevroren lavazee.** `/ca tpring 3 nether` (~1.180, 0 in de Nether):
  - open vlakte, vloer Y ~35 van koudlava met ijsplaten;
  - pilaren van vloer tot plafond (zandloper), met een huid van Nether-ijs en een blauwe kern;
  - plafond Y ~108;
  - geen zichtbare lavazee.
- [ ] **Rijpwoud-hallen** (regio hoog): hallen 30–100 breed met wanden en ribben, vloer van berijpt netherrack, ijzig plafond.
- [ ] **Gletsjerkloven** (regio laag): ijsmassa tot Y ~90–100 met een luchtspleet tot het plafond, kloven 8–20 breed, tunnels.
- [ ] **Warme scheuren:** smalle spleten in vlekken met gloeiende scheur en magma in de wanden, lava onderin (Y 17–22).
- [ ] **Oerijs:** dunne kern in de sterkste pilaren (Y 45–95), ~60 in heel Nether-3.
- [ ] **Grenzen:** geen lavawand bij `/ca tpboundary 2 nether` en bij r 1.500 (3|4). Gletsjer en wanden lopen aan de ringranden af.
- [ ] **Portal ring 3 ↔ Nether-3:**
  - een portal in de overworld op (4.755, 93, 16) komt uit op (1.197, 36, 9) in Nether-3;
  - terug kom je bij hetzelfde portal uit (getest in integratie 1).

**Afstemvragen D**
11. Voelt 625 breed met drie vormen genoeg? Aandeel nu zee 41 % / rijp 36 % / kloven 23 %; vlakken ~250 Nether-blokken.
12. Nether-ijs opaak en rookgrijsblauw (V-T2): past dat bij de bevroren zee?

## E. Prestatie (ter info; budget ≤ 1,75× vanilla in de noise-stap)
**Meting.** Officiële meting van integratie 1, 2026-10-07:
- JFR `minecraft.ChunkGeneration`, stap `noise`, 16 × 16 chunks per gebied;
- mod tegen vanilla in één server (vanilla als datapack-dimensie, zelfde seed en coördinaten, volgorde wisselend);
- verse wereld per ronde, 7 bruikbare rondes;
- per ronde de mediaan in ms/chunk, daarover de mediaan van de verhouding.

| Gebied (chunks) | Biome | ms/chunk mod / vanilla | Mediaan | Min–max per ronde | Surface-stap mod / vanilla |
|---|---|---|---|---|---|
| ring 1 (20,20) | vanilla-kopie | 47,1 / 35,6 | 1,23× | 1,03–1,39 | 20,8 / 8,2 |
| ring 2 (148,−8) | ring 2 | 51,9 / 35,4 | 1,45× | 0,73–1,72 | 17,8 / 7,6 |
| horizonreus (−250,−4) | sulfur/ember | 58,6 / 37,4 | 1,55× | 1,18–2,35 | 30,4 / 7,2 |
| caldera (−250,−120) | sulfur/peaks | 39,1 / 36,4 | 1,14× | 1,01–1,84 | 22,6 / 6,6 |
| lavarivieren (−305,−155) | ember_woods | 54,4 / 38,7 | 1,36× | 1,00–1,47 | 20,4 / 6,7 |
| asvlakte (328,28) | ashen_wastes | 40,6 / 34,4 | 1,20× | 1,05–1,58 | 20,3 / 6,4 |
| zeekom (−254,46) | dried_seabed | 39,4 / 33,3 | 1,35× | 1,10–1,56 | 14,1 / 5,8 |
| titanenveld (−322,−38) | titan_fields | 40,7 / 34,8 | 1,20× | 1,11–1,54 | 19,5 / 6,6 |
| pieken (−53,−367) | volcanic_peaks | 45,9 / 37,7 | 1,21× | 1,05–1,80 | 22,0 / 7,6 |
| basaltstromen (295,−36) | basalt_flows | 52,2 / 37,2 | 1,41× | 1,03–2,31 | 21,7 / 7,2 |
| smeulwoud (289,−8) | ember_woods | 50,7 / 35,1 | 1,44× | 1,04–1,68 | 20,3 / 6,9 |
| zwavelvelden (233,140) | sulfur_fields | 40,8 / 36,5 | 1,24× | 1,10–1,75 | 20,1 / 7,6 |
| slapende krater (−109,223) | ashen + dormant | 55,4 / 35,5 | 1,55× | 1,08–1,84 | 23,1 / 7,0 |
| Nether-1 (−8,−8) | | 5,7 / 4,3 | 1,33× | 1,09–1,53 | 12,8 / 11,6 |
| Nether-2 (31,−8) | | 7,7 / 4,7 | 1,61× | 1,35–1,77 | 9,5 / 12,1 |
| Nether-3 oost (66,−8) | | 6,9 / 4,4 | 1,47× | 1,10–1,58 | 9,8 / 7,8 |
| Nether-3 west (−82,−8) | | 4,7 / 4,6 | 1,21× | 0,92–1,41 | 9,2 / 10,2 |
| Nether-3 zuid (−8,66) | | 5,0 / 4,5 | 1,16× | 1,05–1,89 | 9,4 / 9,1 |
| Nether-3 noord (−8,−82) | | 6,2 / 4,6 | 1,28× | 1,11–1,48 | 9,2 / 10,3 |

**Conclusie.**
- Alle medianen zijn binnen het budget: ring 3 1,14–1,55×, Nether-3 1,16–1,47×. Nether-3 is niet duurder dan Nether-2 (1,61×; besluit 53: "gelijk aan Nether-2").
- Ter vergelijking (andere methode, aparte werelden): W-terrein mat indicatief 1,55–1,64×; ring 2 stond bij de vorige officiële meting op 1,54×.

**Spreiding.**
- Losse rondes geven pieken tot 2,35× (horizonreus) en 2,31× (basaltstromen). Oorzaak: JIT-deoptimalisatiestormen op hete per-blok-paden, versterkt door JFR.
- In één (afgekeurde) ronde bleef de horizonreus 22 min hangen. Zie backlog "JIT-deoptimalisatiestorm".
- Met een warm-up over alle ringtypes (rondes 6–8) bleven de pieken ≤ 1,69× in ring 3. Eén Nether-3-gebied gaf 1,89×.

**Surface-stap** (buiten het budget): ring 3 kost 2,4–4,2× die van vanilla, ring 1/2 2,3–2,5×.

- [ ] **In-game:** bij vliegen door ring 3 (elytra of creative) laden de chunks vlot genoeg? Geen haperingen van de server (F3: ms/tick)?

**Afstemvraag E**
13. Is de prestatie zo acceptabel voor golf 2, of eerst een eigen change voor de deopt-stilstand en de surface-stap (backlog)?

## Resultaten integratie 1 (al gedaan, ter info)
- **S** (dedicated server, 2 clients via quick-play, `int1b-s`):
  - W-smoke `/ca tpring 3`, `tpring 3 nether` en `tpboundary 2|3` werkten; portal ring 3 ↔ Nether-3 heen en terug.
  - Uitbarsting: rode lucht op beide clients (mixin). Lavabommen ~12 per speler per minuut; branden op dezelfde plekken voor beide spelers.
  - `mobGriefing false`: de bommen vielen wel, met 0 nieuwe branden.
  - Asregen: grijze fog en as; onder een dak is de fog weg. Particles Minimal (client 2, 103 tegen 553 particles): geen as, fog en lucht blijven.
  - Aswolk: ~20 s opbouw, daarna bijna zwart. Hitte +1,65/s in de zon (0,55 × 3), +0,83/s met fire resistance, −0,5/s onder een dak (geen weerfactor).
  - HUD-balk hitte oranjerood met streepjes.
  - Grenzen 2→3 en 3→4: de lucht mengt over de zone; aan de ring 4-kant geen bommen.
  - FPS 117–118 (vsync-cap 120) tijdens een uitbarsting.
  - Geen fouten in de logs, behalve "moved too quickly" door teleports.
- **Determinisme:**
  - Twee verse werelden, de tweede omgekeerd geladen (eerst de Nether):
    - ring 3 (gebieden A–E, 2.880 chunks): 2.847 identiek, de rest alleen water/lava/lucht (stroming aan de randen);
    - Nether-3: 576/576; Nether-1/2: 63/64 (één lavablok).
  - Ring 1/2 tegen 8e0c637 (`tg1-base`): biomes en terreinvorm (heightmap) in alle 112 chunks identiek. Blokverschillen alleen vanilla-ertsklodders, zeegras/water, sculk in r1c en `loose_sand`. Hetzelfde soort verschil zit ook tussen de twee werelden van deze code, dus het is laadvolgorde-ruis en geen feature-indexverschuiving. Zone 2→3 verandert bewust (39).
