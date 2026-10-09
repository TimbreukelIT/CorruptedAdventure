# Teststop 4b — overleving + sfeer ring 3 + Nether-ring 3 (checklist)

Scope: blokkerende teststop na integratie 2 (`waves.md` §2, "Teststop 4b"). Stand: branch `ring3/int2` (golf 2 compleet: 39f, 42, 46-rest, 57a-r1, 41, 41b, 47, 47b, 47c, 48, 49, 53b, 54, 55, 43, 50, 50b, 56). Golf 3 (fix-ronde, textures 57) begint pas na akkoord.

**Beoordeeld wordt:** het hitte-ritme, water dat verdampt, de drie weertypes, voedsel, de levendigheid per biome, zielvuur/skeletten/afgietsels, lavabuizen als schuilroute, kou in Nether-3, sfeer en geluid, en de tussenronde textures (T r2). **Niet:** de definitieve textures (57, golf 3) en mobs (fase 4).

Opzet: **dedicated server + 2 clients** (zoals integratie 2), twee sessies van ~60 min.
- Sessie 1: overleving (A hitte en water, B weer, C kou Nether-3, D lavabuizen).
- Sessie 2: aankleding en sfeer per biome (E), horror (F), Nether-3-aankleding (G), geluid en prestatie (H), textures (I).

Antwoorden op de afstemvragen komen in `docs/plans/ring-3/teststop-4b-feedback.md` en waar nodig in de beslissingenlog. Bij elke vraag staat de aanbevolen optie eerst.

## Voorbereiding
- **Wereld:** vers, `level-type=corruptedadventure:corrupted_world`, seed **`20260930`**. Oude testwerelden laden niet meer: `placeholder_nether_ring_3` is weg (53b).
- **Server:** `./gradlew runServer` (eigen `run/server.properties`, `online-mode=false`).
- **Clients:** `./gradlew runClient -PquickPlay=localhost:<poort>` (Dev) en `./gradlew runClient2 -PquickPlay=localhost:<poort>` (Dev2, eigen map `run/client2`). In elke `options.txt`: `onboardAccessibility:false` en `skipMultiplayerWarning:true`. Beide kanten draaien protocolversie 6.
- **Tijd en weer:** `/gamerule doDaylightCycle false`, `/time set noon` of `/time set midnight` waar gevraagd; `/ca weather 3 clear 6000` tenzij een weertype getest wordt.
- **Hulpcommando's:** `/ca ring`, `/ca condition get @s` (meter + stijgsnelheid + oorzaak: `sun`, `roof`, `source`, `peak`, `exposed`, `ice`, `warmth`, `outside`), `/ca tpring 3`, `/ca tpring 3 nether`, `/ca tpboundary 2|3`.
- **Screenshots van de agent:** `build/teststop-4b/` in de INT2-worktree (niet in git; 94 stuks, `<onderwerp>_dev.png` / `_dev2.png`, `c_*` = close-ups voor de textures, `w_*` = weer, `p_*` = pluimen, `n3_*` = Nether-3).

## A. Hitte en water (sessie 1, survival)
- [ ] **Hitte-ritme over een dag/nacht** in ring 3 (`/ca tpring 3`, of de vlakte bij (4.750, 0)): overdag in de zon stijgt de meter, 's nachts en onder een dak zakt hij. Bronnen (lava, magma, smeulsteen) binnen 4 blokken: ×1,5.
- [ ] **Pieken** (4.110, 672): heter dan de vlakte (`peak`).
- [ ] **Slapende krater** (−4.905, 541; groen, azalea-baken): de meter zakt (refuge).
- [ ] **Schaduw onder een eiland** (archipel 2.622, −2.738; of −2.410, 159, −2.714): `roof`.
- [ ] **Water verdampt** (alle routes: emmer leeggooien, ijs breken, waterflessen) in ring 3; in `steam_fens` (overgang 3→4) blijven de kookpoelen staan.
- [ ] INT2 gemeten: in de lavabuis −0,50/s (`roof`), naast het lavagootje +0,41/s (`source`), op het maaiveld erboven +0,41/s (`source`).

**Afstemvragen A**
1. Is het ritme goed: overdag oversteken mogelijk met pauzes in de schaduw/buizen, 's nachts adem? Opties: **A. zo laten (aanbevolen)** · B. zon sneller (×1,25) · C. bronnen zwakker (×1,25 i.p.v. ×1,5).
2. Een gedoofd kampvuur telt nog als hittebron (bij de kou niet). **A. gelijktrekken: alleen brandend telt (aanbevolen)** · B. zo laten.

## B. Weer (sessie 1, beide clients)
- [ ] **Asregen** (`/ca weather 3 ash_rain 300`), **uitbarsting** (`eruption`: rode lucht, gerommel, lavabommen, camera-schok) en **aswolk** (`ash_cloud`: eerst waarschuwing, dan donker en hitte ×3): beide clients zien hetzelfde weer en dezelfde bommen.
- [ ] Eigen geluiden voor de drie weertypes (de agents konden niet luisteren).
- [ ] **Rookpluimen** (50b): `/ca tpboundary 2`, oostwaarts: pluimen boven de horizonvulkanen; beide clients dezelfde pluim op dezelfde plek (INT2: ja, `p_b2_east_before_dev*.png`). Geen pluim boven slapende kraters. Tijdens `eruption`: dikker, sneller, rode gloed aan de voet.
- [ ] **Verre pluimen** voorbij de render distance zijn een **silhouet in mistkleur** (vanilla's terreinmist kleurt ook particles). Beoordelen bij render distance 12 en 32.
- [ ] Naar de Nether en terug: pluimen weg en weer terug (INT2: ja, `p_b2_east_back_dev.png`); uitloggen en opnieuw joinen zonder oude pluimen.
- [ ] **Particles: Minimal**: dunnere pluimen (~0,35×), minder ambient particles; kleuren en geluid blijven. FPS met 8 pluimen in beeld (pieken 4.110, 672).
- [ ] Gerommel met lichte schok buiten het weer (vlakte elke 2–5 min, pieken 1–2,5 min); "Screen Effects" op 0 = geen schok.

**Afstemvragen B**
3. Verre pluimen als mistsilhouet: genoeg? **A. ja, zo laten (aanbevolen)** · B. eigen render-pass zonder terreinmist (complexer, client-only, kost FPS).
4. Uitbarsting: is de rode lucht sterk genoeg (op de INT2-shots `w_peaks_eruption_*` nauwelijks verschil met helder)? **A. sterker kleuren (aanbevolen)** · B. zo laten.

## C. Kou in Nether-3 (sessie 1, survival)
- [ ] `/ca tpring 3 nether` (~−1.185, 36, −79, bevroren lavazee): ijsblauwe HUD-balk; stijgt overal (+0,42/s), ×1,5 op ijs en in de open vlakte (`frozen_lava_sea`; INT2: +0,63/s `exposed`).
- [ ] Zakt bij een gloeiende scheur, magma, brandend kampvuur of lava binnen 4 blokken (−2/s, `warmth`).
- [ ] Leren armor: per stuk −15 %; vol leer remt, maakt niet immuun.
- [ ] Bij 100: frost-overlay, bevroren hartjes, 1 schade per 2 s, eigen doodsbericht; de ander ziet het rillen. **Let op: vanilla's bevriezing halveert ook de loopsnelheid** (bovenop traagheid I).
- [ ] Portal terug naar ring 3 (INT2: overworld 4.758, 99, 17 ↔ Nether 1.186, 58, 18): kou zakt, hitte begint, geen rare dubbele meter.
- [ ] **Dichte mist** in Nether-3 (~40 blokken zicht in zee en kloven).

**Afstemvragen C**
5. Bevriezing halveert ook de loopsnelheid. **A. zo laten: bevroren = echt traag (aanbevolen)** · B. alleen de schade, loopsnelheid via traagheid I (vraagt een mixin in `aiStep`).
6. Mist in Nether-3 (~40 blokken): **A. iets lichter, ~64 blokken (aanbevolen)** · B. zo laten (benauwd, sfeervol) · C. veel lichter (vanilla-Nether).
7. Kou-tempo (0 → 100 in ~4 min, ×1,5 in de open vlakte): **A. zo laten (aanbevolen)** · B. trager (~6 min).

## D. Lavabuizen en magmakamers (sessie 1)
- [ ] Buissysteem (12, −2): mond in een 39d-sleuf (4.854, 102, −728); droge hoofdbuis (vloer bijv. (4.861, 80, −783) en (4.851, 60, −869)); gladde smooth-basaltwanden met blackstone-aders; daklichten (4.855, −862) en (4.773, −965).
- [ ] Gloeiende zijtak met lavagootje en korstlipjes (4.863, 63, −818); eindigt in de magmakamer (4.888, −12, −535; meer Y −17). Tweede systeem: mond (−4.724, 93, −338), kamer (−4.510, −12, −282; meer Y −16).
- [ ] Schuilroute-gevoel: in de buis zakt de hitte (`roof`), naast het gootje stijgt hij (`source`).
- [ ] Askorst-dak boven een buis stort in als je eronder loopt (zeldzaam: alleen waar een mondhelling een dak van één blok laat).
- [ ] Geen rechte grotgeulen in kraterranden (horizonreus −3.895, 65; caldera −3.870, −1.790); geen zwevende brokken onder de archipels; geen water boven Y 0 (INT2: water in ring 3 alleen onder Y −16).

**Afstemvragen D**
8. Askorst-daken zijn zeldzaam. **A. een stuk dun dak vóór elke mond (aanbevolen)** · B. zo laten.
9. Alleen de startmond ligt in een 39d-sleuf (17 in de ring). **A. zo laten (aanbevolen)** · B. ook tweede monden in sleuven (meer zoekwerk per systeem).

## E. Aankleding en levendigheid per biome (sessie 2, 2 min lopen per biome)
Coördinaten (x, z): asvlakte (5.262, 480), pieken (4.110, 672), smeulwoud (4.750, 0), basaltstromen (4.814, −288), zwavelvelden (4.270, 480), zeekom (5.710, −32), titanenveld (4.942, 160), slapende krater (−4.905, 541), `steam_fens` (`/ca tpboundary 3` → −5.996, −400, of 6.000, 0).
- [ ] **Smeulwoud** dag en nacht: gloeiende kronen als vlekjes (geen fullbright-kubussen), open plekken met omgevallen stammen, smeulbessen en vuurbloemen, aslaagjes rond de stammen.
- [ ] **Zwavelvelden:** fumarolen op heuveltoppen met zwavelkraag, puffen `steam_puff`; modder midden in de zinkgaten. (INT2: in het 16 × 16-meetgebied bij (3.730, 2.240) maar 2 fumarolen; in het geodegebied 28.)
- [ ] **Basaltstromen:** hornitos met gloeiende kap, blackstone-tongen, smeulventjes.
- [ ] **Pieken:** inslagkraters met obsidiaan en magmakern; geiten.
- [ ] **Asvlakte:** verkoolde dode bomen in de geulen, puimsteenkeien, aslagen in de lijzijde.
- [ ] **Zeekom:** dood koraal en fossielen (zeemonsters).
- [ ] **Titanenveld:** titanen-rotsen (basaltreuzen), schedels/wervels/botten; zielvuur rond de ribvoeten.
- [ ] **Slapende krater:** groen, azalea-baken, varens, bessen aan de voet, glow lichen tegen de wand.
- [ ] **`steam_fens`:** modderpoelen, kookpoelen (alleen boven Y 63, niet gelekt), stoomfumarolen, **geisers** met waarschuwing (borrelen + kleine puf, ~1 s) en dan een duw van ~5 blokken; een veld geisers loopt niet in de maat. INT2: geiser (5.911, 102, −101) lanceert op zijn ritme.
- [ ] **Striders** op lavarivieren en kratermeren, **zonder ruiter** (INT2: 8 striders, 0 met ruiter).
- [ ] **Lavavallen:** van eilanden (39f, 1 blok breed, tot de grond/het kratermeer) en vanuit kraterwanden (47c, in bestaande lava). Archipel (2.622, −2.738): 4 vallen, 2 pilaren.
- [ ] **Eilanden:** smeulende as en aslagen boven, vuurkristallen hangen onder (van onderaf, 's nachts gloeiend).
- [ ] **Riftbogen:** 21 op de testseed (bijv. (−4.071, −4.014), (−1.414, −5.487), (2.293, 3.999), (5.620, 1.003)).
- [ ] **Instortingen/zinkgaten** op de vlaktes (bijv. 4.301, −405).
- [ ] **Geodes** (vuurkristal, 41b): (4.018, 9, 407) straal 6; drie lagen (kristal, blackstone, smooth basalt), gloeiende knoppen en clusters.
- [ ] Algemeen: geen features op naalden, bogen, ribkruinen, brugdekken of lavaoevers; geen ijs in de warme ring.

**Afstemvragen E**
10a. Lava tegen lucht buiten de vallen (randen van lavaplassen, een luchtgeul door een lavameer, een lavawand langs een buiswand; al vóór golf 2 aanwezig, zie backlog): **A. fix-pakket in golf 3 (aanbevolen)** · B. nu eerst fixen · C. accepteren (de lava stroomt bij de eerste update een blok uit).
10. Levendigheid: heeft elk biome iets eigens? Welk biome voelt te kaal? (open vraag per biome; aanbevolen: noteer per biome "goed / te kaal / te vol").
11. Zielvuur: in het meetgebied rond de ribbenkasten (−1.493, 4.144) staan maar 2 zielvuurtjes. **A. meer zielvuur (bijv. 1 op 2 kernblokken en grotere vlekken) (aanbevolen)** · B. zo laten (zeldzaam = bijzonder).
12. Lavavallen zijn 1 blok breed. **A. zo laten (aanbevolen)** · B. tweede kolom (breder gordijn).
13. Riftbogen (21): **A. zo laten (aanbevolen)** · B. meer (~30–40).
14. Fumarolen in de zwavelvelden: genoeg? **A. zo laten als het in-game klopt; anders meer heuveltoppen (aanbevolen)** · B. meer fumarolen buiten de toppen.

## F. Horror (sessie 2, 's nachts)
- [ ] Gloeiende **as-afgietsels** in de asvlakte (smeulende as); ze kijken (op 90° na) naar de dichtstbijzijnde vulkaan.
- [ ] **Voetsporen** die abrupt stoppen (aan een kraterrand/instorting of bij een afgietsel); lopen door over chunkgrenzen, alleen in as op vlakke grond.
- [ ] Zielvuur en botten 's nachts (blauw tegen het bot).

**Afstemvragen F**
15. Afgietsels (~1 groepje per 3–4 chunks): **A. zo laten (aanbevolen)** · B. zeldzamer (sterker effect).

## G. Nether-3-aankleding (sessie 2)
- [ ] **Bevroren lavazee** (−1.250, 0): pilaren met bevroren lavavallen (ribben), ijspegels aan plafond en pilaarkoppen, Nether-ijsplaten en rijpvlekken; warme eilandjes rond de scheuren (gloed, geen ijs).
- [ ] **Rijpwoud** (1.100, 0): rijpzwammen (groot, kap niet afgeplat; klein met dunne stam en ijspegels), berijpte vloer.
- [ ] **Gletsjerkloven** (1.182, −360): blauw-ijsaders, ijsvallen alleen op de wanden, **ijsbruggen** (begaanbaar), **gloeiende naden op de kloofbodem** (= de "scheur-adertjes"; zeldzaam). De kloven reiken niet tot de lava (Y 17–22).
- [ ] Wezens: strays, skeletten, ghasts; geen piglins/hoglins.
- [ ] Biomegrenzen volgen de terreinvormen; ruimte: voelt 625 breed met drie biomes goed?

**Afstemvragen G**
16. "Scheur-adertjes" zijn gloeiende naden op de kloofbodem (Y ~33), niet bij de lava. **A. akkoord (aanbevolen)** · B. de kloven dieper tot de lava (raakt terrein en kou).
17. Ijsbruggen: genoeg en breed genoeg? **A. zo laten (aanbevolen)** · B. meer/breder.

## H. Geluid, particles en prestatie (sessie 2)
- [ ] Per biome 1 min staan: eigen lucht/fog, particles en geluid (smeulwoud knisperen, titanenveld zoem, slapende krater rust; Nether-3 wind en krakend ijs, adem-wolkjes).
- [ ] **Fumarolenveld / modderveld niet dreunend** (begrenzer: één fumarole-puf per 10 ticks, één modderbubbel per 5 ticks per client). **Geiserwaarschuwing** speelt nu het modderbubbelgeluid (toonhoogte 0,7): duidelijk genoeg? (De agents konden niet luisteren.)
- [ ] **Particles: Minimal** en **FPS**: smeulwoud 's nachts, `steam_fens`, pieken met 8 pluimen.
- [ ] **Koude prestatie** titanenveld/smeulwoud: een verse server, direct daarheen (eerste chunks kunnen haperen). 39f mat koud 2,00× en 1,89× (geaccepteerd); INT2 meet koud opnieuw na deze test.

**Afstemvragen H**
18. Geiserwaarschuwing: **A. eigen waarschuwingsgeluid (`block.geyser.warning`) maken (aanbevolen)** · B. modderbubbel houden.

## I. Textures, tussenronde T r2 (sessie 2)
Close-ups in `build/teststop-4b/c_*.png` (dag; `_night` waar gloed telt). Per blok een oordeel: **goed / lichter / rustiger / meer gloed**.

| Blok | Shot | Oordeel |
|---|---|---|
| smeulblad (`ember_leaves`), dag/nacht | `c_ember_leaves_dev`, `c_ember_leaves_night_dev` | |
| verkoolde stam (`charred_log`) | `c_charred_log_dev` | |
| vuurbloem (`fire_bloom`), dag/nacht | `c_fire_bloom_dev`, `_night` | |
| smeulbes (`ember_berry_bush`) | `c_ember_berry_bush_dev`, `_night` | |
| asspruiten (`ash_sprouts`) | `c_ash_sprouts_dev` | |
| aslaag (`ash_layer`) | `c_ash_layer_dev` | |
| smeulende as (afgietsels), dag/nacht | `c_smoldering_ash_dev`, `_night` | |
| smeulsteen, dag/nacht | `c_smoldering_stone_dev`, `_night` | |
| fumarole + zwavelkorst | `c_fumarole_dev`, `c_sulfur_crust_dev` | |
| kokende modder | `c_boiling_mud_dev` | |
| geiser (`steam_fens`) | `c_geyser_dev` | |
| dood koraal (zeekom) | `c_dead_coral_dev` | |
| zielvuur op zielgrond, dag/nacht | `c_soul_fire_dev`, `_night` | |
| titanenbot | `c_bone_titan_dev` | |
| gepolijste puimsteen | `c_polished_pumice_dev` | |
| azalea (slapende krater) | `c_azalea_dormant_dev` | |
| vuurkristal in een geode | `c_fire_geode_cluster_dev`, `fire_geode_dev` | |
| rijpzwam hoed/stam | `c_rimeshroom_cap_dev`, `c_rimeshroom_stem_dev` | |
| ijspegel | `c_ice_stalactite_dev` | |
| Nether-ijs, verpakt Nether-ijs, oerijs | `c_nether_ice_dev`, `c_packed_nether_ice_dev`, `c_primordial_ice_dev` | |
| rijp-netherrack (met vanilla-ertsen) | `c_rime_netherrack_dev` | |
| gloeiende scheur (kloofbodem) | `c_glowing_fissure_dev` | |
| koudlava | `c_cold_lava_dev` | |

- [ ] Grote vlakken as, basalt, puimsteen, zwavelkorst zonder storende tegeling (van hoog en van dichtbij).
- [ ] 's Nachts gloeien alleen de scheuren/vlekjes; het gesteente wordt donker.
- [ ] `steam_puff` en `ice_glint` passen bij hun omgeving.

## Resultaten integratie 2 (al gedaan, ter info)
Zie `docs/plans/ring-3/int2.md` voor de details en bewijzen.
- **B/D:** `./gradlew build --rerun-tasks` groen (665 tests, incl. `ClientIsolationTest`); `runData` groen (alleen de bewuste "missing BiomeFilter"-meldingen).
- **S:** dedicated server + 2 clients (protocol 6), W-smoke, portal, geiser, striders, hitte/kou: zie A–E.
- **Determinisme** (twee werelden, de tweede omgekeerd geladen): Nether-3 1.280/1.280 chunks identiek; ring 3 8.080/8.614, verschillen alleen vloeistof, ertsklodders over chunkgrenzen, obsidiaan, 4 aslagen op een vanilla-ertsklodder en 5 askorstblokken (valblok) boven een buis.
- **Feature-index** ring 1/2 en Nether-1/2 tegen main `6ef5a85`: biomes 100 % identiek, terreinvorm gelijk; decoratie verschuift (verwacht).
- **Prestatie** (JFR, mod/vanilla in één server, 16 × 16 chunks per gebied, warm-up, 4 warme rondes; budget noise ≤ 1,75×). Warme ronde 5 en de koude ronde (titanenveld/smeulwoud) volgen na deze test:

| Gebied | noise mediaan (4 warm) | min–max | surface | features | noise INT1 (mediaan) |
|---|---|---|---|---|---|
| ring 1 | 0,95 | 0,89–1,05 | 1,48 | 1,00 | 1,23 |
| ring 2 | 1,05 | 0,95–1,11 | 2,85 | 0,83 | 1,45 |
| horizonreus | 1,19 | 1,18–1,76 | 2,59 | 0,43 | 1,55 |
| caldera | 1,10 | 1,07–1,46 | 2,24 | 0,48 | 1,14 |
| lavarivieren | 1,02 | 1,00–1,43 | 2,18 | 1,28 | 1,36 |
| asvlakte | 1,00 | 0,97–1,05 | 2,04 | 1,02 | 1,20 |
| zeekom | 1,36 | 1,16–1,41 | 1,77 | 0,87 | 1,35 |
| titanenveld | 1,19 | 1,12–1,47 | 1,93 | 0,93 | 1,20 |
| pieken | 1,06 | 1,03–1,50 | 2,28 | 1,01 | 1,21 |
| basaltstromen | 1,10 | 1,05–1,49 | 2,11 | 0,32 | 1,41 |
| smeulwoud | 1,22 | 1,13–1,60 | 1,94 | 0,87 | 1,44 |
| zwavelvelden | 1,19 | 1,16–1,55 | 2,45 | 0,37 | 1,24 |
| slapende krater | 1,05 | 1,03–1,42 | 1,92 | 0,87 | 1,55 |
| steam_fens | 1,06 | 1,05–1,52 | 1,68 | 0,34 | – |
| lavabuis (12, −2) | 1,08 | 1,00–1,55 | 1,96 | 0,26 | – |
| magmakamer (−12, −1) | 1,12 | 1,06–1,38 | 2,09 | 0,92 | – |
| Nether-1 | 1,12 | 1,05–1,25 | 1,06 | 0,53 | 1,33 |
| Nether-2 | 1,33 | 1,22–1,52 | 0,76 | 0,09 | 1,61 |
| Nether-3 rijpwoud | 1,36 | 1,26–1,63 | 1,41 | 0,52 | 1,47 |
| Nether-3 zee | 1,17 | 1,09–1,44 | 1,06 | 0,21 | 1,21 |
| Nether-3 zuid | 1,32 | 1,18–1,50 | 1,16 | 0,45 | 1,16 |
| Nether-3 noord | 1,15 | 1,09–1,42 | 1,01 | 0,26 | 1,28 |
| Nether-3 kloven (bruggen) | 1,35 | 1,12–1,67 | 0,95 | 0,27 | – |

  Noise overal ≤ 1,36; surface ring 3 1,7–2,6×; features ring 3 ≤ 1,3×.
