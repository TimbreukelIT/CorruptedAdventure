# Teststop 4c — eindcheck ring 3 + Nether-ring 3 (checklist)

Scope: blokkerende eindcheck van ring 3 en Nether-ring 3 na integratie 3 (`golf-3.md`, "Teststop 4c"). Stand: main `3ee7f55` (golf 3 compleet: T0, 38c, 57 r1, 42c, 54c, 39g, 47e, 42d; verslag `int3.md`). Ring 4 (pakketten 58–80) begint pas na akkoord.

**Beoordeeld wordt:** ring 3 en Nether-3 als geheel: elke ring en grens, een portal per ring, de golf 3-wijzigingen, en een regressieronde over alles wat in 4a en 4b al is goedgekeurd (terrein, aankleding, overleving, weer, sfeer, Nether-3, textures), plus prestatie en determinisme. **Niet:** ring 4 (placeholder), items/recipes (fase 2), structures (fase 3), mobs (fase 4).

Opzet: **dedicated server + 2 clients** (zoals 4b), twee sessies van ~60 min.
- Sessie 1: rondgang en grenzen (A), golf 3 (B), terrein (C), Nether-3 (G).
- Sessie 2: aankleding (D), overleving (E), weer en sfeer (F), blokken en textures (H), en de cijfers (I, J).

Elke sectie eindigt met een oordeel **akkoord / opmerking**. Afstemvragen staan onderaan; bij elke vraag staat de aanbevolen optie eerst. Antwoorden komen in `docs/plans/ring-3/teststop-4c-feedback.md` en waar nodig in de beslissingenlog.

## Voorbereiding
- **Wereld:** vers, `level-type=corruptedadventure:corrupted_world`, seed **`20260930`**. Standaardradii ring 1–5: 1.500 / 3.500 / 6.000 / 7.800 / 9.500 (de nieuwe ring 4-radii van pakket 58 gelden nog niet). Werelden van vóór golf 3 tonen nog de oude askorst-daken en oevers; gebruik een verse wereld.
- **Server:** `./gradlew runServer` vanuit de hoofdwerkmap (eigen `run/server.properties`, `online-mode=false`).
- **Clients:** `./gradlew runClient -PquickPlay=localhost:<poort>` (Dev) en `./gradlew runClient2 -PquickPlay=localhost:<poort>` (Dev2, eigen map `run/client2`). In elke `options.txt`: `onboardAccessibility:false` en `skipMultiplayerWarning:true`.
- **Tijd en weer:** `/gamerule doDaylightCycle false`, `/time set noon` of `/time set midnight` waar gevraagd; `/ca weather 3 clear 6000` tenzij een weertype getest wordt.
- **Hulpcommando's:** `/ca ring`, `/ca condition get @s`, `/ca tpring <1–5> [nether]`, `/ca tpboundary <1–4> [nether]`, `/ca weather 3 <type> [s]`, `/locate biome corruptedadventure:<naam>`.
- **Shots van INT3** (ter vergelijking): `build/int3/shots/` in de INT3-worktree (`.claude/worktrees/agent-a9d66a2a59f334c4e`, niet in git): Nether-3, grenzen 2|3 en 3|4, buismond met askorst, instorting, lavaoevers op een vulkaanflank, Nether-3-kloof.

## A. Rondgang: elke ring, elke grens, een portal per ring (sessie 1, standaard-W)
**Ringen (overworld):**
- [ ] `/ca tpring 1`: vanilla-terrein en -biomes, niets van ring 2/3.
- [ ] `/ca tpring 2`: ring 2 (canyons, zoutvlaktes, duinen, oase, hoodoos) ongewijzigd t.o.v. teststop 3.
- [ ] `/ca tpring 3`: ring 3 (r ~4.750).
- [ ] `/ca tpring 4` en `/ca tpring 5`: placeholders (vlak/groen resp. corrupted placeholder), geen fouten, geen water tegen lucht.

**Grenzen (overworld):**
- [ ] `/ca tpboundary 1` (1|2): overgang ring 1 → 2 zoals in teststop 3.
- [ ] `/ca tpboundary 2` (2|3, `dry_riverbeds`): de lucht slaat om naar asrood; horizonvulkanen oostwaarts (reus bij x ~3.850, z ~0, rand Y ~245) met rookpluimen; geen klif of zwevende brokken in de zone; ring 3-ertsen alleen in de ring 3-helft.
- [ ] `/ca tpboundary 3` (3|4, `steam_fens`): modderpoelen, kookpoelen alleen boven Y 63, geisers; ring 3-kant droog (een dip onder Y 63 is rots, `ring3Seal`); ring 4-kant placeholder.
- [ ] `/ca tpboundary 4` (4|5): placeholders, geen fouten.

**Nether:**
- [ ] `/ca tpring 1 nether`, `tpring 2 nether` (zwamkathedralen, `dripshroom_forest`/`spore_caves`), `tpring 3 nether` (bevroren lavazee).
- [ ] `/ca tpboundary 1 nether`, `tpboundary 2 nether` en de rand r 1.500 (3|4): geen lavawand, de dam is heel (INT3-lavascan: Nether-3 0 lek).

**Portals (overworld ↔ Nether, `coordinate_scale` 4):**
- [ ] Ring 1 ↔ Nether-1 (bijv. rond (500, 0)): heen en terug bij hetzelfde portal.
- [ ] Ring 2 ↔ Nether-2 (bijv. rond (2.500, 0)): heen en terug.
- [ ] **Ring 3 → Nether-3** en terug. INT3: portal op (4.759, 151, 3) → Nether-3 (1.202, 35, 5) en terug naar hetzelfde portal; INT2: (4.758, 99, 17) ↔ (1.186, 58, 18).
- [ ] Bij elke wissel: de juiste meter (hitte in ring 3, kou in Nether-3), geen dubbele meter; het weer en de pluimen wisselen mee.

## B. Nieuw in golf 3 (sessie 1)
**Askorst (38c, 42d):**
- [ ] Buismond (4.818–4.839, Y 74–76, z 57–79; ook rond (4.858, 73–78, 75)): een stuk dun askorstdak vóór de mond; de mond zelf blijft zichtbaar (het ondiepe begin van de geul is open). INT3: 34 askorstblokken, alle hangend na het laden.
- [ ] Onder het dak lopen (survival of creative, **niet** spectator): stof + kraak, na ~0,75 s valt het blok; de aaneengesloten hangende korst volgt als ketting (~2 ticks per stap, max. 64); gestapelde korst valt mee; geen zwevende resten. INT3: Dev eronder → de hele ketting viel.
- [ ] Bovenop de korst lopen: niets. Een mob eronder: niets. Af en toe een stofje onder hangende korst (1 op 160).
- [ ] Zelf askorst boven lucht plaatsen: blijft hangen, ook als je een blok ernaast breekt; valt pas als je eronder gaat staan.
- [ ] Dunne daken boven buizen en daklichten in de korstbiomes (`ashen_wastes`, `volcanic_peaks`, `ember_woods`, `titan_fields`) zijn askorst.

**Lava (42c, 39g, 47e):**
- [ ] Afgrond (4.531, −413; de oude lekplek 4.409–4.412, Y 5–9, −510…−508): de lavazee ligt overal tegen rots, ook aan de spitse eindes; geen luchtschacht naast de lava.
- [ ] Lavarivieren (bijv. 4.110, −3.425; vulkaanflank bij (−664, −5.700); kanaalgebied rond (−4.760, −2.360)): een smalle zwarte oever van ~3 blokken vanaf de lava, magma alleen in het eerste blok, ook bij brede rivieren en eindpoelen; bomen en aankleding langs de rivier zoals voorheen.
- [ ] Archipel (2.622, −2.738): de vallen lopen tot de grond of in lava, nooit in water; geen aslaag in het stroomgebied van een val.

**Nether-3 (54c):**
- [ ] Gletsjerkloven (bijv. (1.211, 22, −286), n3rifts): de scheurwanden warm (magma, gloeiende scheur), geen ijsblokjes naast de lavaspiegels, ook aan de rand van `rime_forest` (INT3: 0, was 16).

**Textures (57 r1, al goedgekeurd; alleen regressie):**
- [ ] `glowing_fissure`-vloer dag en nacht: scheuren sluiten in elke variant aan, de naad is afgekoeld; `smoldering_stone` 's nachts zonder afgeknipte gloeistukjes aan blokranden.
- [ ] Zwavelvelden: kristallen (mat, stil) en de drie modders (geel, olijfgroen, roestoranje) passen bij korst en travertijn.

## C. Terrein ring 3 (sessie 1, regressie 4a/golf 2/afronding)
- [ ] **Vulkanen:** horizonreus met lavameer (~−3.895, 65), actieve caldera (~−3.870, −1.790), pieken (4.110, 672): kegels met puimsteenkap, kraterwanden met magma en smeulsteen, lava staat stil; geen rechte grotgeulen in kraterranden.
- [ ] **Slapende krater** (−4.905, 541): overwoekerd (mos, azalea-bosjes, varens, dripleaf, ranken, omgevallen stam), geen water.
- [ ] **Zwevende eilanden en archipels** (2.622, −2.738; −4.890, 180–197, 885): onderkant als omgekeerde kegel met vuurkristallen, lavavallen van 1 blok breed; geen zwevende grotresten.
- [ ] **Landmarks:** afgronden (4.531, −413) en (5.120, 2.354) met lavazee en bruggen; ribbenkasten (−1.493, 4.144); riftbogen (bijv. (−4.071, −4.014), (2.293, 3.999)); instortingen (4.301, −405).
- [ ] **Wanden van diepe sneden** (afgrond, rifts, rivierkloven, instortingen): tuf in banden, blackstone-aders, basalt; geen vanilla `stone`-banden (klein residu in de afgrond Y 20–60 bekend).
- [ ] **Lavabuizen en magmakamers:** mond in een 39d-sleuf (4.854, 102, −728), droge hoofdbuis met smooth-basaltwanden en daklichten, gloeiende zijtak met lavagootje en korstlipjes (4.863, 63, −818), magmakamer (4.888, −12, −535); tweede systeem: mond (−4.724, 93, −338), kamer (−4.510, −12, −282). Geen carver snijdt een kamer, gootje of geode aan.
- [ ] **Geode** (vuurkristal, (4.018, 9, 407), straal 6): drie lagen, gloeiende knoppen en clusters.
- [ ] **Water en lava:** geen water boven Y 0; diepe grotten zonder lokale aquiferlava; geen lava tegen open lucht buiten de eilandvallen, de 47c-vallen en de riviertreden. **Nieuw (besluit gebruiker, INT3-review):** open ruimte op Y −54 is in ring 3 altijd rots, dus geen lavameren op de bodem van grotten in ring 3.
- [ ] **Biomeverdeling:** de 8 biomes + `steam_fens` vallen samen met hun terreinvorm; geen biome dat bijna ontbreekt.

## D. Aankleding per biome (sessie 2, 2 min lopen per biome; regressie 4b + afronding)
Coördinaten (x, z): asvlakte (5.262, 480), pieken (4.110, 672), smeulwoud (4.750, 0), basaltstromen (4.814, −288), zwavelvelden (4.270, 480), zeekom (5.710, −32), titanenveld (4.942, 160), slapende krater (−4.905, 541), `steam_fens` (`/ca tpboundary 3`).
- [ ] **Smeulwoud** dag en nacht: gloeiende kronen als vlekjes, open plekken met omgevallen stammen, bessen en vuurbloemen, aslaagjes rond de stammen; geen afgesneden kronen of stammen op chunkgrenzen.
- [ ] **Zwavelvelden:** fumarolen op heuveltoppen met zwavelkraag; kookpoelen met calcietrand en terrassen; gekleurde modder met `sulfur_steam`; zwavelkristallen op de kraag en in groepjes, nooit zwevend.
- [ ] **Basaltstromen:** hornitos met gloeiende kap, blackstone-tongen, smeulventjes.
- [ ] **Pieken:** inslagkraters met obsidiaan en magmakern; geiten.
- [ ] **Asvlakte:** verkoolde dode bomen in de geulen, puimsteenkeien, aslagen in de lijzijde; afgietsels en voetsporen (F).
- [ ] **Zeekom:** dood koraal, fossielen van zeemonsters op de kustterrassen.
- [ ] **Titanenveld:** titanen-rotsen, schedels/wervels/botten, duidelijk meer zielvuur rond de ribvoeten.
- [ ] **`steam_fens`:** modderpoelen, kookpoelen (boven Y 63), stoomfumarolen, geisers met eigen waarschuwingsgeluid en een duw van ~5 blokken; niet in de maat.
- [ ] **Striders** op lavarivieren en kratermeren, zonder ruiter.
- [ ] **Horror** ('s nachts): gloeiende as-afgietsels die naar de dichtstbijzijnde vulkaan kijken, voetsporen die abrupt stoppen; zielvuur en botten.
- [ ] Algemeen: geen features op naalden, bogen, ribkruinen, brugdekken of lavaoevers; aslagen lopen over chunkgrenzen door.

## E. Overleving (sessie 2, survival; regressie 4b + afronding)
- [ ] **Hitte-ritme** over een dag/nacht in ring 3; pieken heter (`peak`); slapende krater koelt (refuge); schaduw onder een eiland (`roof`); in een lavabuis zakt de hitte, naast het gootje stijgt hij.
- [ ] **Water verdampt** bovengronds in ring 3 (emmer, dispenser, gebroken ijs); onder een dak en ondergronds niet.
- [ ] **Ijs smelt** onder open lucht overdag (niet 's nachts, niet in de slapende krater); **natte spons** droogt onder open lucht; gedoofd kampvuur is geen hittebron, brandend wel.
- [ ] **Kou in Nether-3:** stijgt overal, ×1,5 op ijs en in de open vlakte; zakt bij een gloeiende scheur, magma, kampvuur of lava; leer remt per stuk; bij 100 frost-overlay, schade, eigen doodsbericht, zichtbaar voor de ander.

## F. Weer en sfeer (sessie 2, beide clients; regressie 4b + afronding)
- [ ] **Asregen**, **uitbarsting** (donker karmijnrode lucht, al ~15 s na de start; 's nachts rode gloed; lavabommen; gerommel met schok) en **aswolk** (waarschuwing, dan bijna zwart, hitte ×3): beide clients zien hetzelfde weer en dezelfde bommen; met 2 spelers naast elkaar niet 2× zoveel bommen; geen stilstaande vuurballen na wegteleporteren of een herstart.
- [ ] **Rookpluimen:** vanaf `/ca tpboundary 2` oostwaarts bij render distance 6, 12 en 32 tot 512 blokken zichtbaar zonder mistsilhouet; achter bergen correct verborgen; 's nachts donker; tijdens een uitbarsting dikker met gloed; naar de Nether en terug zonder oude pluimen; Particles: Minimal → dunner.
- [ ] **Per biome 1 min staan:** eigen lucht/fog, particles en geluid (smeulwoud knisperen, titanenveld zoem, slapende krater rust; Nether-3 wind, krakend ijs, ademwolkjes). Fumarolen- en modderveld levendig, niet dreunend.
- [ ] Gerommel met lichte schok buiten het weer; "Screen Effects" 0 = geen schok.

## G. Nether-3 (sessie 1)
- [ ] **Bevroren lavazee** (`/ca tpring 3 nether`, ~−840, 35, −840 of 1.180, 0): pilaren met bevroren lavavallen, ijspegels, ijsplaten en rijpvlekken; warme eilandjes rond de scheuren.
- [ ] **Rijpwoud** (1.100, 0): rijpzwammen groot en klein, berijpte vloer.
- [ ] **Gletsjerkloven** (1.182, −360): blauw-ijsaders, ijsvallen alleen op de wanden, ijsbruggen begaanbaar, gloeiende naden op de kloofbodem; warme scheurwanden zonder ijs (B).
- [ ] Wezens: strays, skeletten, ghasts; geen piglins/hoglins. Dichte Nether-mist (akkoord in 4b).
- [ ] Oerijs in de sterkste pilaren; alleen met Silk Touch.

## H. Blokken en textures (sessie 2)
- [ ] Geiser, fumarole, kokende en gekleurde modder (traag + hittebron), zwavelkristallen (vallen zonder steun, amethist-klank, zwak licht; cluster → zwavel, knoppen alleen Silk Touch).
- [ ] Rotaties `glowing_fissure`/`smoldering_stone`: geen tegelraster, gloeilaag precies op de scheuren, geen z-fighting.
- [ ] Grote vlakken as, basalt, puimsteen, zwavelkorst zonder storende tegeling; 's nachts gloeien alleen de scheuren en vlekjes.
- [ ] (Optioneel) Sodium + Iris: gloeilagen werken (57 r1 bevestigd; vuurrobijn en de scheur met shaderpack niet expliciet gezien).

## I. Determinisme en lava (ter info, uit INT3)
Details: `docs/plans/ring-3/int3.md` ("Resultaten").
- **Determinisme** (twee verse werelden, de tweede omgekeerd geladen, Nether eerst; 34 gebieden): 9.431/10.806 chunks identiek; Nether-3 1.280/1.280. Ring 3: alleen erts-klodderwissels over chunkgrenzen en vloeistoftiming (aquiferwater, eilandvallen); geen obsidiaan meer op Y −55, geen smeulboom-/aslaagverschillen, geen askorstverschillen. Ring 2 (`loose_sand` boven aquiferwater) en ring 1/Nether-1 (vanilla-features) zoals eerder.
- **Lavascan** (trede/val/lek): ring 3 lek 0 in alle gebieden (afgrond 0); in arch 2 blokken vloeiende lava van een eilandval op het moment van opslaan. Nether-3 0.
- **Feature-index ring 1/2** (tegen main zonder W): terrein 0 verschil; biomes identiek op één bestaande Nether-2-chunk na (backlog).
- [ ] Na deze teststop: geen nieuwe meting nodig, tenzij een fix het terrein raakt.

## J. Prestatie (wordt ingevuld na de meting)
Meting: `tools/test/perf.sh` (JFR `minecraft.ChunkGeneration`, mod tegen de vanilla-datapackdimensies in één server, 16 × 16 chunks per gebied, warm-up over alle ringtypes, verse wereld per ronde), **3 warme rondes + 1 koude**, harde stop 45 min; tegen INT-afr (`int-afr.md`). Golf 3 raakt de noise-stap (42c, de band rond de afgrondzee) en de surface-stap (39g `lava_shore`, 42d askorst-dak). Budget noise ≤ 1,75× vanilla (mediaan per gebied).

| Gebied | noise (3 warm) | min–max | surface | features | noise koud | surface koud | noise INT-afr | surface INT-afr |
|---|---|---|---|---|---|---|---|---|
| _(volgt)_ | | | | | | | | |

- [ ] Noise binnen budget; surface en features vergeleken met INT-afr.
- [ ] In-game: geen haperingen bij het aanvliegen van de horizonreus, het titanenveld en het smeulwoud (verse server, koud); F3 ms/tick.

## Afstemvragen
1. **Eindoordeel ring 3 + Nether-3:** **A. akkoord, door naar ring 4 (pakket 58) (aanbevolen als A–H zonder blokkerende opmerkingen)** · B. akkoord met een kleine fix-ronde vooraf (welke punten) · C. niet akkoord (welke secties).
2. **Askorst-ketting (38c):** een instorting neemt max. 64 blokken mee. **A. zo laten (aanbevolen)** · B. kleiner (bijv. 32, minder dramatisch) · C. groter.
3. **Tag `#replaceable_by_trees` (INT3, determinisme smeulwoud):** `ash_layer`, `fire_bloom` en `ember_berry_bush` worden nu ook door zelf geplante bomen vervangen. **A. zo laten: klein effect, worldgen is daardoor deterministisch (aanbevolen)** · B. alleen in worldgen vervangbaar maken (eigen regel in de feature i.p.v. de tag; meer code).
4. **Stoomwolkjes** (`steam_puff`/`sulfur_steam`) ogen grof (backlog 57 r1). **A. backlog laten, oppakken in de infra-/opruimronde na ring 3 (aanbevolen)** · B. nu een zachtere sprite (textures) · C. kleinere wolkjes (particle-klasse, atmosphere).
5. **Nether-2-biome laadvolgorde** (één sectie van chunk (37, −4) wisselt `spore_caves` aan/uit; blokken identiek): **A. backlog, onderzoeken in de infra-ronde (aanbevolen)** · B. nu uitzoeken (worldgen Nether-2, raakt teststop 3-terrein niet).

## Voortgang
- 2026-10-10: checklist opgesteld door INT3 (`ring3/int3` op main `3ee7f55`); prestatie volgt na het signaal van main.
