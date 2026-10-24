# Ring 5 — walk 1: fotowandeling binnenband, buitenband en Nether-5

Scope: `worldgen` (de twee fixes); tooling `infra` (`tools/test/py/walk.py`). Stand: `main` na INT3 (`7300b26a`),
2026-10-24. Opdracht van main: ring 5 kritisch bekijken op screenshots (doel van de gebruiker: een **rijk, niet saai**
landschap), wat binnen het ontwerp duidelijk beter kan meteen oplossen, nieuwe ontwerpkeuzes als vraag terugmelden.

## Werkwijze
- Verse wereld seed 20260930 (`run/walk1`, gebieden in `build/test/walk1/areas.txt`: de oostas 8272–9200, `meadow`, `mc1`,
  `glitch`, `csn`, `scars`, `inverted`, buitenband, void-rand, zone 4→5, kasteel #1, Nether `halls`/`cathedral`), stroom
  `photos` (poort n = 12), client 1 met `studio.sh prepare` (FOV 70, geen wolken, kijkafstand 12).
- `tools/test/py/walk.py` (nieuw): poses `label dim day|night x y z yaw pitch` via RCON (spectator, ringweer uit, HUD uit,
  dagcyclus stil) en `client.sh shot` tot het beeld stilstaat (`stable_shot` van de studio). Per plek: hoog schuin
  (`_top`: 42 boven, 52 terug; `_low`: 20 boven, 28 terug), ver (`_far`), ooghoogte N/O/Z/W (`_eye_*`), deels 's nachts.
  Poses uit de heightmap van de wereld (`build/test/walk1/mkposes.py`, `spots.txt`); plekken boven de void schuiven naar
  de dichtstbijzijnde grond.
- Foto's vóór: `build/test/shots/walk1/` (143 poses), na: `build/test/shots/walk1-after/` (de plekken die de fixes raken).
- Valkuilen van de wandeling zelf: (1) de eerste ronde liep zonder `doDaylightCycle false`, waardoor de foto's na ~10 min
  in de schemering/nacht vielen (opnieuw geschoten; `walk.py` zet de dagcyclus nu stil); (2) `_top`/`_far` zijn in de
  binnenband bijna alleen fog (fog 48–112, bedoeld), daarom `_low`; (3) grotten en de onderkant van de omgekeerde heuvels
  zijn met spectator zonder licht zwart (ook met night vision; niet verder uitgezocht, zie open punten).

## Bevindingen per plek (vóór)
| Plek | Oordeel |
|---|---|
| `corrupted_wastes` (8400, 0) | Goed: dode bomen in groepjes, keien, dode struiken, trappen in de heuvels; wel overal hetzelfde grijsgroen gras. |
| `glitch_fields` (0, −8400) | Kaal (besloten V-C) met kubussen en glitchvlekken; leest als glitch. |
| `glitch_fields` bij `mc1` (8420, −520) | **Fout:** 61 % van de bovenkant op exact Y 59/60, een doodvlakke vlakte van ~160 × 160 (de zachte knik van het reliëf loopt naar Y 60 en de kwantisering ×4 maakt hem helemaal plat); daardoor ook 9 kampen op 132 chunks (vlakke grond). |
| `false_meadow` (8464, −832) | Mooi vanilla-weide met spiegeleiken; harde maar duidelijke rand naar `glitch_fields`. |
| Binnenband-toppen (oostas) | **Fout (kleiner):** de toppen lopen vlak uit rond Y 123 (de knik naar `ROLL_HIGH`); 4 % van de oostas op één hoogte, kampen op Y 122–124. |
| Bandlijn / zandstrook (8720–8911, 0) | **Fout:** 57 % `corrupted_sand` in de laatste ~200 blokken vóór de bandlijn; het lichte zand leest als een sneeuwveld met dode bomen, harde overgang. |
| Zone 4→5 (8000, 0) | Bruine grond met hoge rechte grijze breukwanden (ring 4 `sundered_reach`); monotoon bruin/grijs, beige ring 4-fog. Niet aangepast (ring 4-terrein). |
| Corrupte plekken `cse`, `cse2`, `csn` | Goed zichtbaar: opgetilde/verzakte chunkblokken met harde wanden, weggevallen chunk met voidsteen-rand; wanden van de gaten `corrupted_stone` + voidsteen (gemeten, ook over chunkgrenzen). |
| Dubbelganger `cp93` | In fog niet als spiegeling te herkennen vanaf één punt; landschap zelf in orde. |
| Kasteelplateau #1 | Verwoest plateau (puin, obsidiaan, glitchblokken) leest goed; erg druk patroon. |
| Kampen | Tent van wol op hekpalen, kampvuur, kist, bed: herkenbaar; tent leest als getrapt blok. |
| Bordjes / fakkelpatronen | Bordje dichtbij leesbaar ("You were here"), lichtgrijs bord valt op; fakkelpatronen alleen van dichtbij zichtbaar (fog). |
| Herinneringen | Lavaveld in een verzonken vak, zwevende brokken bij de void-rand, struikenveld: herkenbaar als "vreemd stuk". |
| `void_scars`, `inverted_hills`, `shattered_expanse`, buitenband, void-rand | Sterk: zwart met sterren, voidsteen-randen lezen goed, zwevende brokken; harde biome-rand steen/gras tussen `void_scars` en `inverted_hills`. |
| Grotten (`n93`, `ch93`, `ch93b`), omgekeerde bomen van onderen | Niet te beoordelen: zwart (zie werkwijze). |
| Nether `white_halls`, `hollow_cathedral` | Zuilen en stil-lichtbanden lezen; maar wit leest **bruin/sepia** door het Nether-licht (geen hemellicht, warm lichtkaart-tint). Vraag hieronder. |

## Opgelost (`worldgen`)
1. **Vlakke laagtes en toppen** (`Ring5Inner.ground`): de zachte knik (`soft`) liet elke lage strook naar Y 60 en elke hoge
   naar Y ~123 convergeren. Nu op de bodem deiningen tot `FLOOR_SWELL` 9 omhoog en op de toppen dalingen tot 9 (twee eigen
   samples van de heuvelruis op schaal `FLOOR_SCALE` 4, golven ~30–65; ingefaded over `FLOOR_FADE` 14; monotoon, dus geen
   overhangen). Het reliëf blijft binnen 60–146; `false_meadow` (`rolling`) ongewijzigd (bewust te glad).
2. **Zandstrook vóór de bandlijn** (`ring5_surface.py`, `corrupted_wastes`): zand in de lage plekken (onder Y 76) alleen nog
   in grove vlekken van `minecraft:surface_secondary` (≥ 0, ~de helft); de zandvlekken en kale aarde blijven.

## Na
Verse wereld met de fixes (`run/walk1after`, alleen de binnenband-gebieden; de buitenband, kasteel en Nether-5 raakt de
change niet), dezelfde plekken opnieuw (`build/test/shots/walk1-after/`). Meting op de bovenblokken (`build/test/walk1/stats.py`):

| Gebied | Vóór | Na |
|---|---|---|
| strook vóór de bandlijn (8720–8911, z −64…79) | zand 56,6 %, gras 38,5 %; p5 61 | zand 19,3 %, gras 74,9 %, aarde 1,3 %; p5 63 |
| binnenband oostas (8272–8719) | zand 4,3 %; vaakste hoogte Y 123 (4,1 %, vlakke toppen) | zand 1,5 %; vaakste hoogte Y 93 (3,5 %) |
| `glitch_fields` bij `mc1` | 61,2 % op één hoogte (Y 59) | 40,9 % op één hoogte (Y 63), daarbuiten treden van 4 |
| `glitch_fields` (0, −8400), `false_meadow` | p5 71 / 67 | p5 75 / 71 (meadow-grond zelf ongewijzigd; de rand naar de dode grond schuift mee) |

Beeld: de strook vóór de bandlijn is nu een afwisseling van zandvlekken en grijsgroen gras in plaats van één wit veld
(`sand_low`); de vlakte bij `mc1` heeft treden en lage ruggen gekregen, maar het dichtstbijzijnde stuk is nog één niveau
(de kwantisering ×4 van `glitch_fields` vangt een deining van 0–12 in hooguit drie treden). Unit-tests (`Ring5InnerTest`):
p1 66,0, p5 70,7, mediaan 90,4, p95 112,0, max 144,7, in 62–124 99,6 %.

## Gates
Lokaal, seed 20260930, na de fixes:
- `build --offline` groen: 1.371 tests, 1 overgeslagen (`Ring5InnerTest` met de nieuwe reliëfwaarden binnen de grenzen).
- `runData` schoon (`src/generated` ongewijzigd; de surface-tak is handgeschreven JSON uit `ring5_surface.py`).
- Server: verse werelden `walk1after`, `det-walk1-a`/`-b` starten met 0 ERROR, geen client-class-fout; client 1 joint.
- `det.sh --areas ring5.txt`: 1.277/1.277 chunks identiek, own 0, other 0.
- `voidscan.py --strict --rings` op `ring5.txt` (`det-walk1-a`): 0 fouten. `lavascan.py --strict`: lek 0.
- Prestatie (`perf.sh --areas perf-r5.txt --only "wastes glitch meadow zone45 shattered" --cold-only wastes`; de change
  raakt alleen de binnenband): noise t.o.v. vanilla, mediaan van 2 warme rondes: `wastes` 1,23 (koud 1,08), `glitch` 1,03,
  `meadow` 1,02, `shattered` 0,65, `zone45` 1,59 (1,55–1,63). Budget ≤ 1,75×: gehaald (INT3: 1,10 / 1,01 / 0,97 / 0,65 /
  1,53; binnen de ruis van een losse ronde). Ronde 2 viel uit: `perf.py` las JFR-duur `PT1M18.2S` (één chunkstap wachtte
  > 1 min) niet; opgelost (`seconds`, ISO-duur met uren/minuten), de tabel heeft daardoor 2 warme rondes.

## Open vragen (nieuwe ontwerpkeuzes: niet gebouwd)
1. **Nether-5 leest bruin in plaats van wit** (`halls_eye_n`, `cathedral_high`): zonder hemellicht kleurt de Nether-lichtkaart
   het witsteen warm grijsbruin; alleen de stil-lichtbanden zijn echt wit.
   - A *(aanbevolen)*: client-side lichtkaart voor Nether-5 neutraal en iets lichter (zoals de fog van 98, alleen `client/`;
     geen invloed op spawnen of de stilte-meter).
   - B: meer stil licht (tweede band per zuil of lampen in de vloer): echt licht, wit waar het schijnt, de rest blijft bruin.
   - C: witsteen-set met een zwakke emissive laag (zoals de voidsteen-gloed): altijd wit, minder diepte.
   - D: zo laten (het donkere is juist eng).
2. **Zone 4→5 is monotoon** (`zone45_*`): bruine grond met lange rechte grijze breukwanden en beige ring 4-fog.
   - A *(aanbevolen)*: in de buitenste helft van de zone de ring 5-blokken laten inlopen (corrupted gras/steen in vlekken),
     zodat de overgang al "vergaat".
   - B: strata/aders in de breukwanden (minder egaal grijs), grond blijft.
   - C: zo laten (ring 4-terrein, `sundered_reach` was akkoord bij 5c).
3. **`corrupted_sand` leest als sneeuw** (`sand_eye_e`, `bandline_eye_e`): het zand is veel lichter dan het grijsgroene gras.
   - A *(aanbevolen)*: zand een paar tinten donkerder en iets warmer (grijsbeige), textures-change.
   - B: zo laten (nu nog maar ~20 % vóór de bandlijn).
4. **Glitch-vlaktes blijven brede treden** (`mc1_low` na): de kwantisering ×4 maakt van elke laagte een paar grote niveaus.
   - A: zo laten (blokkerig is het karakter).
   - B *(aanbevolen)*: in laagtes vaker verschoven lagen/kubussen (`CUBE_CHANCE`/lagen op de vloer), meer om naar te kijken.

## Ter info / backlog
- Foto's van grotten (`n93`, `ch93`, `ch93b`) en van de omgekeerde bomen van onderen zijn zwart: spectator zonder licht;
  `effect give … night_vision` hielp in deze client niet (niet uitgezocht). Voor een volgende wandeling: fakkels/licht
  plaatsen of `gamma` in `options.txt` omhoog (`infra`, backlog).
- `_top`/`_far` in de binnenband zijn door de dikke fog (bedoeld) niet bruikbaar; `_low` (20 boven, 28 terug) wel.
- De dubbelganger is in fog vanaf één punt niet als spiegeling te herkennen (al bekend als 6b-vraag §K).

## Foto's om te bekijken
- Voor/na naast elkaar: `build/test/shots/walk1-after/compare_sand_low.png` (zandstrook vóór de bandlijn),
  `compare_mc1_low.png` en `compare_mc1_eye_n.png` (vlakke glitch-vlakte), `compare_bandline_eye_s.png`.
- Vóór, sterk: `build/test/shots/walk1/outer_top.png`, `castle1_top.png`, `csn_low.png`, `memfloat_eye_n.png`,
  `wastes_eye_e.png`, `meadow_eye_e.png`.
- Vóór, open vragen: `build/test/shots/walk1/halls_eye_n.png` en `cathedral_high.png` (Nether-5 bruin),
  `zone45_eye_n.png` (zone 4→5), `sand_eye_e.png` (zand als sneeuw).
