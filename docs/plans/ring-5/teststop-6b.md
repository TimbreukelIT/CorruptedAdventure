# Teststop 6b — ring 5 golf 2: binnenband, grotten, aankleding, Nether-5, stilte, sfeer, textures (checklist)

**Deel 1** (A–J, na INT2 van 2026-10-23): **83b** (binnenband: golvend reliëf en corrupte plekken), **96** (aankleding
Nether-5), **97** (stilte-meter Nether-5) en **98** (sfeer ring 5 + Nether-5), plus de INT2-fixes (protocol `"10"`,
voidsteen met `void_mote`). **Deel 2** (K–N, na INT3 van 2026-10-24): **93** (grotten en gespiegelde dubbelgangers),
**94/95** (aankleding en horror-features ring 5: dode bomen, glitchblokken in het terrein, voidsteen-randen, bordjes,
ontbrekende chunks, kampen, herinneringen, omgekeerde bomen, verwoeste kasteelplateaus, ertsen) en **99b** (textures
golf 2), plus de INT3-fixes. Stand: `main` na INT3 (2026-10-24); deel 1 en 2 kunnen in één sessie en in één verse wereld.

Lokaal door de gebruiker: **dedicated server + 2 clients** (multiplayer is een harde eis; stilte en void-teleport hebben
een tweede speler nodig). Elke sectie eindigt met **akkoord / opmerking**.

## Voorbereiding
- **Wereld:** **vers**, seed **`20260930`**, `level-type=corruptedadventure\:corrupted_world`, bijv.
  `level-name=ring5-6b` in `run/server.properties` (`online-mode=false`). Standaardradii zoals 6a (ring 5 = 8.000–9.700,
  void-rand 10.200; Nether ÷ 4: ring 5 = 2.000–2.425).
- **Server en twee clients** (speel-lock `C:/Temp/ca-user-test.lock` zet main):
  ```powershell
  cd "P:\Projecten\Java\The Corrupted Adventure"
  .\gradlew.bat runServer --no-daemon
  .\gradlew.bat runClient --no-daemon          # client 1; Multiplayer → 127.0.0.1:<server-port>
  ```
  Client 2: `bash tools/test/server.sh start ring5-6b --fresh --client both` en `bash tools/test/client.sh start 1` /
  `start 2` (twee spelers, eigen `run/client2`), of een tweede `runClient` met een andere gebruikersnaam. **Beide clients
  en de server moeten dezelfde build draaien** (protocol `"10"`; een oudere client wordt geweigerd).
- `op Dev` (en de tweede naam); `/gamemode creative` om te reizen, `/gamemode survival` voor de meters, difficulty normal.
- **Hulp:** `/ca ring`, `/ca tpring 5 [nether]`, `/ca tpboundary 4|5`, `/ca castles tp <1–4>`, `/ca weather`,
  `/ca condition get`, `/ca condition set @s <void|silence> <0–100>`, `/time set noon|midnight`, F3 (`CA ring (client)`,
  `CA weather (client)`), video-instelling **Screen Effect Scale** (glitch-fog en void-glitch volgen hem).

### Plekken (seed 20260930; blokcoördinaten)
| Plek | x, z | Bron |
|---|---|---|
| binnenband `corrupted_wastes` | 8400, 0 | `perf-r5.txt` `wastes` |
| binnenband `glitch_fields` | 0, −8400 | `glitch` |
| binnenband `false_meadow` | 8464, −832 | `meadow` |
| corrupte plek oost | ~8464–8543, −176…−49 | `ring5.txt` `cse` |
| corrupte plek oost 2 | ~8480–8607, 96–191 | `ring5.txt` `cse2`; doorsnede `section 8460 152 8630 152` (83b) |
| corrupte plek noord | ~−208…−97, −8368…−8257 | `ring5.txt` `csn` |
| bandlijn oost | 8500 → 9200, 0 | `r5le` |
| buitenband `shattered_expanse` / `void_scars` / `inverted_hills` | 8992, 96 / −512, −9056 / 0, −8800 | 6a |
| void-rand | 9696, 0 | `voidedge` |
| Nether `white_halls` | 2192, 0 (Nether) | `halls` |
| Nether `hollow_cathedral` | 2248, 212 (r 90; reuzenzuil r 10) (Nether) | 96; ook (−110, 2174) |

## A. Kaarten vooraf (main, ter info)
INT2-kaarten in `build/test/maps/int2/`: `top_overworld_8900_0_r1400_s4_{height,biome}.png` (oost),
`top_overworld_0_-8900_r1400_s4_{height,biome}.png` (noord), `top_the_nether_2200_0_r400_s2_{height,biome}.png`.
- [ ] Het reliëf van de binnenband en de kleine vierkanten van de corrupte plekken zijn op de oostkaart te zien.

## B. Binnenband: reliëf en corrupte plekken (83b; herziening van 6a)
| Punt | Plek | Let op |
|---|---|---|
| Golvend reliëf | 8400, 0; 0, −8400; 8464, −832 | heuvels en dalen Y ~66–116 (p5–p95, mediaan ~90), af en toe een kam of klif (steile wand, flauwe rug) tot ~145; voelt het als avonturenland i.p.v. plat? |
| Uitloop | `/ca tpboundary 4` → naar binnen; bandlijn oost | na zone 4→5 loopt het reliëf over ~160 blokken op uit de oude lage grond (geen trede); naar de bandlijn toe zakt het zacht naar Y 70–110 (de schotsen van de buitenband) |
| "Net fout"-vormen | `corrupted_wastes`, `false_meadow`, `glitch_fields` | trappen (treden exact 4) lopen bergop; kuilen altijd onder hun laagste rand (ook op een helling); weide = het reliëf met een te regelmatige golf; glitchblokken van 4 volgen het reliëf. Zeldzaam: trappen op een kam tot Y ~170–200 (backlog) |
| Corrupte plekken | `cse`, `cse2`, `csn` (tabel) | ~elke 200–250 blokken een plek van 2–5 chunks: hele chunks 8–30 opgetild of verzakt met **harde verticale wanden op de chunkgrens**; precies één chunk **naar de void weggevallen** (droog, gerafelde rand 1–3, geen bedrock, geen lava); vaak genoeg ("meer corrupte chunks")? te vaak? |
| Zand (6a-punt) | 8400, 0; 8900, 0 | `corrupted_sand` waar de top van `corrupted_wastes` onder Y 76 ligt: nu **18,6 %** (was 24 %, lokaal 84 %), alleen in de dalbodems. Leest het als dode grond met zandige laagtes? |
| Losse scheuren, lava | binnenband | scheuren 2–6 breed tot in de void; lava alleen diep onder Y −54 en nooit naast een scheur of gat (dam 6) |
| Buitenband, kastelen | `/ca castles tp 1` | ongewijzigd t.o.v. 6a (controle) |

## C. Nether-5-aankleding (96)
- [ ] `white_halls` (2192, 0): elke rasterzuil heeft één band van **stil licht** (2 hoog, rondom aan de buitenkant) op een
      eigen hoogte Y 38, 44, 50 of 56; leest het als "stil licht in de zuilen"? Genoeg licht in de hallen?
- [ ] **Witte kern:** ~1 op 10 zuilen heeft binnenin (niet zichtbaar van buiten) 1–3 `white_core_ore` rond Y 66–68;
      hak een paar zuilen open op die hoogte. Is "onzichtbaar tot je zoekt" goed, of wil je een aanwijzing (backlog)?
- [ ] `hollow_cathedral` (2248, 212): de reuzenzuil heeft een verticale witte-kernader in het hart (Y 32–119) en een
      **vloer-afdruk** rond de voet (8 breed: bakstenen randjes binnen en buiten, gepolijst witsteen ertussen); geen
      lichtband op de reuzenzuil. Leest het als bouwwerk?
- [ ] Open 6a-punt: lezen de randen van de witte vlaktes (2–6 hoog, bakstenen zijkanten onder een gepolijste top) goed,
      of liever een trede / lagere randen?
- [ ] Open 6a-punt: boven het plafond van Nether-5 (Y 122–126, 2 blokken boven het kathedraaldak graven) ligt nog vanilla
      netherrack; moet dat witsteen worden?

## D. Stilte-meter Nether-5 (97 server + 98 client)
Survival, difficulty normal, beide spelers in Nether-5. De meter is een witte balk boven de hongerbalk.
- [ ] **Alleen:** `/ca condition get` in `white_halls` op de vloer: label `zone`, ~0,33/s (0 → 100 in ~5 min).
- [ ] **Open ruimte:** op een witte vlakte (gepolijst witsteen) en in `hollow_cathedral` ~0,5/s (label `open`, ~3,3 min).
- [ ] **Samen:** speler 2 binnen 8 blokken → opbouw ×0,5 (label `together`; ~10 min alleen-vloer, ~6,7 min open + samen);
      verder dan 8 weg → weer de gewone snelheid. Een derde speler stapelt niet.
- [ ] **Vier geluidsbronnen** binnen 16 blokken laten de meter 10 s lang met 2/s zakken (label `sound`, wint van alles):
      **bel** luiden, **goat horn** blazen, **note block** (met de hand), **jukebox** met een plaat (blijft laag zolang hij
      speelt); en **via redstone**: een note block of bel aan een klok/knop. Buiten 16 blokken: geen effect. Elk geluid
      telt ook voor speler 2 als die binnen 16 staat.
- [ ] **Drempels** (`/ca condition set @s silence 35`, `65`, `95`):
      30 → wereldgeluid en muziek zakken geleidelijk (100 % → 35 % bij 90; UI/`MASTER` niet);
      60 → HUD weg: hartjes, honger, pantser, lucht, XP, effecten, bossbalk, scorebord; **blijven:** hotbar (+ itemnaam),
      richtkruis, conditiebalken en chat; onder ~58 komt hij terug (hysterese);
      90 → alle geluid weg binnen ~1 s, alleen een **hartslag** (sneller naarmate de meter stijgt), darkness, en
      hartslagschade 1 per 2 s die **stopt op 1 HP** (ook op hard; geen dood, geen doodsbericht).
- [ ] Hartslag en schade stoppen bij 1 HP; de darkness blijft tot de meter zakt. Buiten Nether-5 zakt de meter 1/s
      (in Nether-4 sneller); de hartslag stopt na ~10 s.
- [ ] Speler 2 ziet de darkness van speler 1 niet; zijn eigen meter/HUD volgt alleen zijn eigen waarde.
- [ ] Gevoel: is ~5 min de juiste druk? Werkt "samen sta je sterker"? Is de stilte eng of vooral irritant?

## E. Void-meter: bronnen en teleport-effecten (97 §2, 98)
Overworld ring 5, survival.
- [ ] **Bronnen:** naast een `glitch_block` of `voidstone` (binnen 4) stijgt de meter sneller (label `source`, +0,55/s);
      met fakkels rondom (bloklicht ≥ 12) zakt hij (`light`).
- [ ] `voidstone` blaast nu af en toe een **`void_mote`** uit een open zijde (zwart-violet stofje dat traag stijgt; was
      vanilla reverse-portal).
- [ ] **Drempel 1 (≥ 30):** lichte glitchstorm-ruis die meegroeit met de meter (tot ~60 % van de storm bij 100),
      `glitch_pixel` rond je, geen hotbar-hapering; met Screen Effect Scale 0 geen ruis.
- [ ] **Void-teleport (≥ 60, binnenband):** bij elke sprong een glitch-geluid en een plukje `glitch_pixel` op vertrek- én
      aankomstplek; **speler 2 ernaast** ziet en hoort het.

## F. Lucht, sterren, fog (98, overworld ring 5)
- [ ] **Lucht:** binnenband grijs-violet (biome); van de bandlijn naar buiten steeds zwarter, ~85 % op de ringrand, 100 %
      boven de void; zon en maan (en de zonsopgang-gloed) gedimd tot ~40 %. Overdag en 's nachts.
- [ ] **Sterren:** in de buitenband ook overdag (sterker naar buiten), boven de void ook onder je; binnenband alleen 's nachts.
- [ ] **Fog binnenband:** dik en kleurloos (~0,45 × kijkafstand, 48–112 blokken), dunt naar buiten uit naar zwart; in
      grotten/water vanilla. Navigeren naar de bandlijn en de kastelen nog te doen?
- [ ] "Dubbel zwart" (98-review): geen donkere band tussen fog en lucht in de buitenband?
- [ ] **Glitch-fog:** om de 4–12 s springt de fog-rand 1–3 ticks naar dichtbij (1 op 4 violet-zwart), vaker in
      `glitch_fields`, niet tijdens een glitchstorm; Screen Effect Scale 0 = uit.
- [ ] **Nether-5-fog:** wit en ver (~0,8 × kijkafstand), de zuilrasters lossen op in het wit.

## G. Geluid (98)
- [ ] **Binnenband** bijna stil: zelden een glitch-knetter in `corrupted_wastes`, vaker in `glitch_fields` (+ `glitch_pixel`
      in de lucht); `false_meadow` helemaal stil; grotklank vervormd.
- [ ] **Buitenband:** `void_drone` (lage zuigende loop) in alle drie biomes, `void_mote`-deeltjes (het meest in
      `shattered_expanse`).
- [ ] **Geen muziek** in heel ring 5 en Nether-5 (ook niet na lang wachten); Nether-5 zonder ambient, heel zelden een wit
      stofje.
- [ ] **Echo-voetstappen:** na 1–3 min lopen in ring 5 volgen 4–10 stappen elk met een echo 0,4–0,8 s later, 6–12 blokken
      achter je (stapgeluid van het blok, zachter); stop je, dan nog één echo. Binnen, buiten, in een grot en in
      `false_meadow`. Hoorbaar genoeg op die afstand? Eng?
- [ ] **Weer met eigen audio** (`/ca weather 5 glitch_storm 120`, `reverse_rain 120`, `standstill 60`): eigen loops (geen
      vanilla-geluid meer); glitchstorm met `glitch_pixel`, omgekeerde regen met opstijgende `reverse_raindrop`-druppels
      (stoppen onder een dak). Volume van de nieuwe `.ogg`'s goed?

## H. Client-mixins in een echte client (98; tot nu alleen met `javap` gecontroleerd)
- [ ] Geen fout of mixin-waarschuwing in de client-log bij het laden (`ClientLevelSkyColorMixin` met de nieuwe inject op
      `getStarBrightness`, `DimensionSpecialEffectsMixin`, nieuw `LevelRendererSkyMixin` op `renderSky`, en die van 92:
      `SoundEngineVolumeMixin`, `LevelRendererCloudsMixin`). Een crash hier is blokkerend voor 98.
- [ ] Optioneel met Sodium: lucht, sterren en zon/maan-dimmen werken nog (of netjes niet).

## I. Open punten uit 6a (meedraaien)
- [ ] `void_edge` (9.700–10.050) houdt vanilla steen; naad corrupted/vanilla op biomegrenzen. Corrupted maken?
- [ ] Nether-5: vlakterand en netherrack boven het plafond (zie C).
- [ ] Ringweer open punt (backlog): een korte zwakke bui buiten als binnen stilstand trekt.

## J. INT2-resultaten (ter info)
Lokaal, seed 20260930, op `main` na INT2: `build` groen (1.345 tests, 1 overgeslagen, incl. `ClientIsolationTest`),
`runData` schoon (`src/generated` ongewijzigd); S-smoke (verse wereld `int2-r5`) 0 ERROR en geen client-class-fout,
`/ca ring`, `/ca castles` (de vier plekken van 6a), `/ca weather` werken; `voidscan.py --strict --rings` op `ring5.txt`
(incl. de drie corrupte plekken) 0 fouten; `lavascan.py --strict` lek 0; `det.sh` op `ring5.txt` (omgekeerd geladen)
1.128/1.128 chunks identiek, own 0; feature-index ring 1–4 (`fi3.txt`, tegen de stand vóór INT2) biomes en terreinvorm
gelijk, op de bekende Nether-cel (`n2`, chunk 37, −4; backlog) na; `waterscan.sh` op `ring4.txt` over 0 (gif 0).
Prestatie (`perf.sh --areas perf-r5.txt --cold-only "wastes cathedral"`, 3 warm + 1 koud, noise t.o.v. vanilla, mediaan):
`wastes` 0,52 (koud 0,57), `glitch` 0,54, `meadow` 0,48, `scars` 0,46, `inverted` 0,49, `shattered` 0,43, **`zone45`
1,22** (1,13–1,31), `castle1` 0,37, `voidedge` 0,46, Nether `halls` 1,06, `cathedral` 1,07 (koud 1,13). Budget ≤ 1,75×:
gehaald. Surface het hoogst in de binnenband (1,8–2,1; ter info). `ModNetwork.PROTOCOL_VERSION` `"10"`.

## K. Grotten en dubbelgangers (93)
Plekken (seed 20260930, blokken; uit `tools/test/areas/ring5.txt`):
| Plek | x, z | Wat |
|---|---|---|
| net `n93` | ~8336–8447, 0–95 | een rasternet op Y 0 boven een gespiegelde void-grot (vloer −34) |
| gekopieerd net `cn93` | ~8480–8575, 64–111 | één gang 3 × naast elkaar, elk met een eigen schacht (drie gelijke gaten op een rij) |
| void-grotten `ch93`, `ch93b` | ~8320–8367, −640…−593; ~8528–8575, −240…−193 | kamers met 1–4 gaten tot in de void; `ch93` gespiegeld |
| dubbelganger `cp93` | bron 8700, 189 → doel 8783, 189 | 48 × 48 gespiegeld, zachte rand 8–12 |
| buitenkamers `oc93`, `oc93b` | ~8720–8767, −2192…−2129; ~9040–9087, −800…−753 | een kamer in een dikke schots met een gat naar de void eronder |

- [ ] **Onmogelijke gangen:** rechte 3 × 3- en 2 × 1-gangen op vaste niveaus (Y 48/32/16/0/−16/−32), doodlopers en lussen,
      een rechte schacht 3 × 3 tot het oppervlak (zonder ladder dodelijk: backlog). Voelt het "te perfect"?
- [ ] **Void-grotten:** in het donker sta je aan een gat; de rand is nu **voidsteen** (de vloer binnen 2 van het gat, en de
      wand omlaag 2 breed `corrupted_stone` met voidsteen-aders, zoals de scheuren; INT3). Ziet het er gevaarlijk genoeg uit?
- [ ] **Gekopieerd/gespiegeld:** een gang 2–3 × naast elkaar, een kamer die zichzelf spiegelt; merk je het?
- [ ] **Natuurlijke grotten:** gedempt vanilla (cheese + spaghetti) in de binnenband, droog; onder Y −54 lava alleen in open
      grotruimte, nooit binnen 6 van een gat of scheur. Grotvloeren zijn kale steen (geen gras of zand, INT3); alleen
      vlak onder het oppervlak (binnen 8) krijgen ingangen gras, zoals in vanilla. Genoeg / te veel?
- [ ] **Dubbelgangers:** sta bij de bron en kijk naar het doel: herken je de vorm gespiegeld? (Geen eigen grotten erin:
      besluit 93-review.) De rand zacht genoeg?

## L. Aankleding en horror-features (94/95)
| Plek | x, z | Wat |
|---|---|---|
| schone ontbrekende chunks | ~8384–8495, −560…−449 (`mc1`); ~8528–8639, −288…−177 (`mc2`) | precies één chunk weg tot in de void, voidsteen-rand, droog |
| herinneringen | ~8576–8655, −192…−113 / 32…111 | stukjes van andere ringen, liggend of zwevend |
| `inverted_hills` | ~9200–9279, −32…47 | omgekeerde bomen onder de heuvels, omgekeerde weide eronder |

- [ ] `corrupted_wastes`: dode bomen in groepjes (~30 % met plukjes blad), dode struiken, keien; **kale plekken**
      `corrupted_dirt` + wat `corrupted_sand` (V-A): gemeten **13,1 %** van de top (dirt 9,9 % + zand 3,2 %; doel 10–15 %).
- [ ] `false_meadow`: gras en bloemen ~vanilla, **spiegelparen** van eiken (exact gespiegeld); geen paar meer op een open
      schacht (INT3), heel zelden nog boven een natuurlijke grotingang (backlog).
- [ ] Herobrine-bordjes, fakkelpatronen (richting kasteel), verlaten kampen (tent, kampvuur, bed, kist): vaak genoeg, eng
      genoeg?
- [ ] Voidsteen op elke rand naar de void (scheuren, schotsranden, pinakel, plekgaten, ontbrekende chunks, en nu ook de
      void-gaten van 93); verwoeste kasteelplateaus; nulliet aan void-wanden en diep in de binnenband.
- [ ] `inverted_hills`: omgekeerde bomen van onderen, dode struiken op de schotsen; `glitch_fields` kaal (V-C).
- [ ] `corrupted_wastes` vlak bij de bandlijn (bijv. 8900, 0): daar ligt de grond lager dan Y 76, dus is de top grotendeels
      `corrupted_sand` (gemeten 67 % van de wastes-kolommen in het perf-gebied `shattered`, ook vóór INT3). Leest dat als
      zandige rand naar de schotsen, of is het een te harde overgang?

## M. Textures golf 2 (99b)
- [ ] Voidsteen (32 oriëntaties, nevel, sterren in drie helderheden, kruisster): op een grote wand geen raster meer?
- [ ] Glitchblok: een vlek glitchblokken geen egaal schaakbord meer? De scheurstrepen lopen door de 32 oriëntaties op veel
      vlakken verticaal: stoort dat? `corrupted_stone` onder de schotsen zonder aderraster?
- [ ] Nullieterts (INT3): draaien mee met hun gesteente; het voidsteen-erts heeft nu ook sterren tussen de kristallen. Vallen
      ze nog te veel (of te weinig) op?
- [ ] **Vraag: is het lichtere, rustigere grijs van de bordjes (`SIGN_WOOD`) goed?** Lezen zwarte en donkerrode tekst nu?
- [ ] **Vraag: zijn de bladeren (`corrupted_leaves`) dun genoeg?** Nu ~60 % bedekt (eik ~67 %), helderheid ~ getinte eik;
      omgekeerde bomen van onderen leesbaar?

## N. INT3-resultaten (ter info)
Lokaal, seed 20260930, op `main` na INT3 (2026-10-24): `build` groen (1.371 tests, 1 overgeslagen, incl.
`ClientIsolationTest` en het nieuwe `Ring5GroundConditionTest`), `runData` schoon (`src/generated` ongewijzigd; alleen de
bekende `BiomeFilter`-meldingen); S-smoke (verse wereld `int3-smoke2`) 0 ERROR, geen client-class-fout, client 1 joint,
`/ca tpring 5`, `/ca castles`, `/ca weather` werken; `voidscan.py --strict --rings` op `ring5.txt` (nu met de zeven
grotgebieden van 93) en op de 94-plekken en biome-gebieden 0 fouten; `lavascan.py --strict` lek 0 (ook rond de schone
ontbrekende chunks); `det.sh` op `ring5.txt` (omgekeerd geladen) 1.277/1.277 chunks identiek, own 0; feature-index ring
1–4 (`fi3.txt`, tegen `main` vóór INT3) biomes en terreinvorm gelijk, op de bekende Nether-cel (`n2`, chunk 37, −4) na;
`waterscan.sh` op `ring4.txt` over 0 (gif 0). Prestatie (`perf.sh --areas perf-r5.txt --cold-only "wastes cathedral"`,
3 warm + 1 koud, noise t.o.v. vanilla, mediaan): `wastes` 1,10 (koud 1,26), `glitch` 1,01, `meadow` 0,97, `scars` 0,65,
`inverted` 0,65, `shattered` 0,65, **`zone45` 1,53** (1,45–1,62), `castle1` 0,58, `voidedge` 0,67, Nether `halls` 1,07,
`cathedral` 1,08 (koud 1,15). Budget ≤ 1,75×: gehaald; de binnenband kost nu ~2× zoveel als bij INT2 (de grotten van 93 en
het terrein per blok, zie hieronder). Gemeten vóór de laatste reviewfix (`f32b2726`, alleen planning van herinneringen en
spiegeleiken); daarna opnieuw build, runData, smoke, voidscan, lavascan en det. Na de review van 99b en de laatste INT3-reviewfixes (`21d75e65`, `a2e436ee`, `b4bacffa`) nogmaals: build groen (1.371 tests), runData alleen de bedoelde erts-blockstates/-modellen, verse wereld 0 ERROR + client (muren voidsteen/nullieterts/glitchblokken bij nacht bekeken), `det.sh` 1.277/1.277 own 0, voidscan 0, lavascan lek 0. Kaarten in `build/test/maps/int3/`:
`top_overworld_8900_0_r1400_s4_*`, `top_overworld_0_-8900_r1400_s4_*`, `top_the_nether_2200_0_r400_s2_*` en doorsneden
`section_overworld_8000_0_9000_0`, door het net `8336 48 → 8447 48` en de void-grot `8320 −620 → 8367 −620`.
`ModNetwork.PROTOCOL_VERSION` blijft `"10"` (geen payload gewijzigd).

INT3-fixes (vóór de test):
- **Terrein:** 93 las `ring_5/overworld/terrain` als `cache_once` per blok via een `range_choice`; in de noise chunk gaf dat
  voor alle 128 blokken van een cel de eerste waarde, dus lag de grond van de hele binnenband 1–8 blokken lager, in treden
  van 8 langs de hellingen (97 % van de kolommen). Nu weer exact (gelijk aan 94's wereld, op de grotten na).
- **Grotvloeren:** geen gras, aarde of zand meer op grotvloeren dieper dan 8 onder de top (vóór de fix ~4.000 blokken in de
  meetgebieden, erna 0); ingangen vlak onder het oppervlak houden de biome-surface, zoals vanilla.
- **Void-gaten (93):** voidsteen op de rotsrand van de vloer en de wand omlaag 2 breed `corrupted_stone` met voidsteen.
- **Omgekeerde weide** alleen nog op de onderkant van de heuvel; **spiegeleiken en herinneringen** niet op een schacht of
  gang van 93 (boven een natuurlijke ingang kan een eik nog zweven: 2 stammen in 256 chunks weide, backlog).

## Bekende open punten
- 6b-vragen van 99b: het lichtere bordgrijs en de dunte van de bladeren (§M).
- Zwevende spiegeleik boven een natuurlijke grotingang (zeldzaam), grotvloeren binnen 8 onder de top met biome-surface,
  rechte schachten zonder uitweg (backlog).
- De kleuren van de ring 5-biomes bleven in `Ring5Biomes` (backlog); de witte kern is van buiten onzichtbaar (backlog).

## Uitkomst
*(in te vullen na de test)*

## Terugmelden
Per sectie **akkoord / opmerking**, per bevinding kort:

```
Plek: D <punt>            F3: x y z
Klopt:      …
Mis/lelijk: …
Screenshot: ja/nee
```
