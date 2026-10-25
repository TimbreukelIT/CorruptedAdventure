# Ring 2 — G2: nulmeting, rondgang en voorstellen

Scope: `worldgen` (opknapbeurt G2, `docs/design/fase-1-wereld.md` §G2). Branch `g2/ring2`, worktree `wt/g2-ring2`,
stroom `g2-ring2` (poort n = 6). Doel van deze ronde: **nulmeting + rondgang + voorstellen, niet bouwen**; de gebruiker
kiest de concrete features in de sparring per ring (CLAUDE.md §3). Lat voor "levendig": ring 4 na de aankleding
(proefstuk + uitrol A–D, §Ring 4 "Aankleding").

## 1. Inventaris per biome (stand `main` `bac7f266`)

Bronnen: `Ring2Biomes`, `Ring2Features`, `Ring2FlatsFeatures`, `Ring2DressingFeatures`, `DryRiverbedsFeatures`,
`density_function/ring_2/overworld/*`, de surface rules in `noise_settings/overworld.json` en de uitwerkingen van changes
23–31 in `fase-1-wereld.md` (dichtheden zijn de daar gemeten waarden, seed 20260930). Alle features zijn chunk-lokaal
(`jittered_grid` → `WORLD_SURFACE_WG` → filters → `biome`).

### `canyonlands` (rotsig + vochtig, ~36 % van de ring)
- **Vormen (terrein, DF):** plateau Y 125–139; getrapte tafelbergen +12/+24/+36 (~32 % van het plateau, toppen tot ~Y 174);
  meanderende hoofdkloven met zijtakken (bodem 10–40 breed, Y 66–89, treden van 6 met droge kolken aan de valwand);
  spleetkloven (cel ~110, 3,5–6 breed, 25–45 diep, overdekte stukken en klemblokken); puinhellingen (~20 % van de wanden);
  kloofwandrichels, nissen en kaprots; plateau-naalden en half begraven keien (~0,45 per plateauchunk), vinnen
  (~0,2 per plateauchunk, ~19 % met een gat = boog), torens in brede kloofbodems (~1 per 120 kloofchunks).
- **Bodem:** rood zand op plateau en treden; kloofbodem rood zand met gebarsten klei ("drooggevallen beekjes") en grind;
  puinhellingen grind + rood zand; wanden de kloofsteenlagen (rood 50 / oranje 22 / crème 28 %).
- **Features:** keienvelden op ~21 % van de kloofbodem (526 keien in 400 chunks), puin op de puinhellingen, rotsval aan
  wandvoeten, plateaupuin (1 per 2 chunks), wandbronnen (~1 per 10 kloofchunks: val, stroompje, kolk met mos/varens/
  suikerriet); flora `canyonlands_vegetation` (4 pogingen per chunk: dorre struik ~2,7, tonnetjescactus ~0,45, dode
  struik, woestijnbloem zeldzaam). Ertsen: zonnesteen (bloot in de wanden, bloklicht 3), turkoois diep.
- **Bezienswaardigheden nu:** de kloven zelf, tafelbergen, vinbogen, torens (zeldzaam), wandbronnen. Allemaal terrein of
  klein; geen zeldzame, eigen "blikvanger" zoals in ring 4.

### `hoodoo_fields` (rotsig + droog, ~18 %)
- **Vormen:** hoodoovloer Y 85–91 onder het plateau, steilrand ~45 met twee treden; twee pilaarrasters (cel 17 en 23):
  naalden 15–45 hoog, 35 % met balancerende kap; bogen (cel 24, kans 0,12; vin 10–16 lang met gat). Pilaarkolommen ~11 %.
  Kaprots van geschroeide zandsteen boven Y 94.
- **Bodem:** vloer rood zand met gebarsten klei en grind.
- **Features:** kloofflora (raster 4, 0,4), flora aan de pilaarvoet (0,6), cactusgroepjes (≥ 0,45), rotsval: ~6,7 planten
  per chunk, 27 % aan een pilaarvoet; zonnesteen in pilaren en kappen.
- **Bezienswaardigheden nu:** bogen en kappen (veel, maar allemaal dezelfde maat); geen grote vorm.

### `salt_flats` (vlak + droog, ~13 %)
- **Vormen:** doodvlak Y 66 (bovenste blok 65); zout-eilandjes +2 (~4,8 % van de vlakte, 30–40 breed).
- **Bodem:** zoutkorst met zeshoekig barstenpatroon van zoutblok (~23 % randkolommen); zandvlekken aan de duinkant;
  eilandjes zand/rood zand.
- **Features:** pekelpoelen (r 2–5, 1 diep, ~1 per 7 chunks), zoutpilaarvelden (~12 % van de vlakte) + losse pilaren,
  zoutkristallen (~1 per chunk) + kristalvelden (~10 %), op de eilandjes rotsjes, flora (~6 per 256 eilandkolommen),
  cactus, fossielen en botten.
- **Bezienswaardigheden nu:** geen; pekelpoelen en pilaren zijn klein (2–6 hoog).

### `dune_sea` (vlak + midden, ~21 %)
- **Vormen:** richtingloze duinruggen (kammen ~70–100 uit elkaar), amplitude 10–32 per gebied (lage en hoge duinen),
  zand op zandstenen kern.
- **Bodem:** zand; harde dalbodem (grove aarde / gebarsten klei, ~12 % van de dalbodem ≤ Y 69).
- **Features:** dalflora (~4,4–5,3 per dalchunk), droog gras op grove aarde, flankflora, kammen kaal; cactusgroepjes
  (34–43 per 256 chunks); fossielen (~1 per 26 chunks), losse botten (~1 per 5); rotsuitstekels (~1 per 10–13 chunks, tot
  12 hoog, soms een kraag); mini-oases bij hoge duinen (~1 per 40 hoge-duinchunks). Zingend zand (ambient).
- **Bezienswaardigheden nu:** rotsuitstekels (vaak genoeg, maar klein), mini-oases (zeldzaam, verborgen).

### `oasis_flats` (vlak + vochtig, ~12 %)
- **Vormen:** zandvlakte Y 66–74; poelen uit `ring_2_pools` (~6,3 % water, plaatselijk breder), vlakke oever op Y 66.
- **Bodem:** zand; brede grasoever (~10), overgangsring gras met grove aarde → zand; zandsteen op steile vlakken.
- **Features:** suikerriet en hoog gras/varens aan het water, kort gras/varens/bloemen in kleurvlekken op de oever, palmen
  in groepjes (~2,3 per poel) + struikpalmen + losse palmen op het zand (~1 per 6 chunks), struiken (azalea/bes) aan de
  rand, overgangsflora, zandsteenkeien. Oase-ambient (vogels).
- **Bezienswaardigheden nu:** de poelen zelf.

### `dry_riverbeds` (overgang 2→3, zone ~110–180 breed)
- **Vormen:** meanderende beddingen (~1 per 200 blokken, bodem 20–30 breed, 5 diep, oevers met twee treden).
- **Bodem:** gebarsten klei met grind, grove aarde, rood zand; geblakerd (blackstone/basalt/geschroeide zandsteen) richting
  ring 3.
- **Features:** fossielen (~1 per 25 chunks), botten, keien op de oevers, dode bomen (dunner richting ring 3), struiken,
  droog gras, cactusgroepjes.

### Zone 1→2 (`withered_expanse`)
Ring 1-kant van de grens (kale bomen, keien, zandvlekken); hoort niet bij G2 ring 2, wel bij de grensspot 1→2.

## 2. Zwakke plekken t.o.v. G2 (vooraf, uit de inventaris; getoetst in §5)
G2 vraagt drie dingen: geen lege vlakken, subzones (vlekken met andere bodem en flora), en ~1 bezienswaardigheid per
300–500 blokken.
- **Algemeen:** ring 2 heeft veel kleine features, maar bijna alles is **uniform over de hele biome** verdeeld (één raster,
  één kans). Alleen de oasebloemen, cactusgroepjes en keienvelden volgen een vlekkenruis. Ring 4 heeft daarentegen
  bosjes/plukken (open en vol afgewisseld) en 1–2 **zeldzame blikvangers per biome** (cel 160–320). Die laatste
  categorie ontbreekt in ring 2 volledig.
- **`canyonlands`:** het plateau is rood zand met puntvormige rotsen en schaarse struiken (één bodem, één flora); de
  kloofbodem heeft keienvelden en bronnen maar geen groene of donkere plekken; geen bomen in de hele biome.
- **`hoodoo_fields`:** één patroon over het hele veld (twee rasters, één pilaarmaat); geen dichte en open stukken, geen
  afwijkende kleur of grootte.
- **`salt_flats`:** tussen de kleine features een doodvlakke witte vlakte; de features zijn ≤ 6 hoog, dus vanaf ooghoogte
  ver weg niets.
- **`dune_sea`:** de kammen zijn bewust kaal; de duinen hebben één zandkleur; landmarks zijn ≤ 12 hoog.
- **`oasis_flats`:** het zand tussen de poelen (≥ 80 % van de biome) heeft alleen losse palmen en overgangsflora.
- **`dry_riverbeds`:** waarschijnlijk in orde (veel soorten), maar zonder eigen herkenningspunt.

## 3. Spotlijst voor de rondgang
`tools/test/areas/g2-ring2.txt` (11 × 11 chunks rond elke spot; posities uit de W-checklists van changes 27, 30, 30b
en 30c, dezelfde biome-indeling sinds change 30; bevestigd met `/locate biome` in de rondgang):

| Spot | Biome | Blok (x z) | Waarom |
|---|---|---|---|
| `canyon` | canyonlands | 1980 356 | kloofbodem met keienvelden, wanden |
| `mesa` | canyonlands | 2952 −954 | tafelbergen en plateau |
| `spring` | canyonlands | 2257 −990 | wandbron, kloof |
| `oasis` | oasis_flats | 918 −2376 | grootste poel, palmen |
| `oasisw` | oasis_flats | −2560 1280 | oase naast de duinen |
| `hoodoo` | hoodoo_fields | 2420 719 | pilaarveld, bogen |
| `hscarp` | hoodoo_fields | 2368 370 | steilrand plateau → hoodoovloer |
| `salt` | salt_flats | −2700 −1560 | barstenpatroon, poelen, pilaren, eilandjes |
| `salte` | salt_flats | 2750 1050 | tweede zoutvlakte |
| `dunes` | dune_sea | −2144 579 | hoge duinen, dalen, rotsuitstekel |
| `duneslow` | dune_sea | −2445 545 | lage duinen |
| `dunese` | dune_sea | 2700 −1408 | hoge duinen oost |
| `duneoases` | dune_sea | 2700 −110 | mini-oases, cactusveld |
| `z12e`, `z12n` | zone 1→2 | 1500 0, 0 −1500 | grens 1→2 |
| `z23e`, `z23s`, `z23sw` | dry_riverbeds | 3500 0, 0 3500, −2475 2475 | grens 2→3 |

Perf: `tools/test/areas/perf-g2-ring2.txt` (16 × 16 chunks, elke biome; `canyonlands` en `dune_sea` twee keer, beide
zones).

## 4. Werkwijze rondgang
- Verse wereld `run/g2walk` (seed 20260930) met 12 van de 19 spots (`world.sh --only`, ingekort op verzoek van main:
  ~10–12 sprekende spots; `duneslow`, `dunese`, `salte`, `z12n`, `z23s` en `z23sw` gaan naar de na-rondgang). Alle spots
  bevestigd met `/locate biome` (0 blokken weg; `hscarp` ligt aan de kloofkant, de hoodoovloer begint 71 blokken verder).
- Poses uit de heightmap (`build/test/g2-ring2/mkposes.py`, kopie van walk 1; `spots.txt`), 41 foto's met `walk.py`
  (`CA_SHOT_F2=1`, client 1, `studio.sh prepare`). Foto's: `build/test/shots/g2-ring2/`, contactblad
  `build/test/g2-ring2/sheet.png`.
- Meting op de bovenblokken (`build/test/g2-ring2/g2stats.py`, 121 chunks per spot): "object" = plant op de grond of een
  bovenblok dat geen grondmateriaal is (kei, bot, cactus, stam); "leeg" = een tegel zonder één object.
- Onbruikbaar (camera in de rots of tegen een helling): `canyon_low`, `canyon_top` (de spot ligt in een smalle
  spleetkloof) en `oasisw_eye_s`. Niet opnieuw geschoten (na-rondgang).

| Spot | Objecten/chunk | Lege 8×8 | Lege chunks | Bovenblokken | Meeste objecten |
|---|---|---|---|---|---|
| `canyon` | 4,0 | 35 % | 1 % | rood zand 78 %, kloofsteen 13 %, gebarsten klei 3 % | dorre struik 2,1, kloofkei 0,8 |
| `mesa` | 4,0 | 36 % | 0 % | rood zand 88 % | dorre struik 2,4 |
| `spring` | 4,9 | 33 % | 0 % | rood zand 61 %, gebarsten klei 28 % | dorre struik 2,3, kloofkei 0,9 |
| `hoodoo` | 7,3 | 12 % | 0 % | rood zand 62 %, klei 19 %, geschroeid 13 % | dorre struik 4,0, dode struik 1,1, cactus 1,5 |
| `hscarp` | 4,5 | 32 % | 2 % | rood zand 80 % | dorre struik 2,5 |
| `salt` | 2,9 | 60 % | 21 % | korst 73 %, zoutblok 23 % | kristal 2,1, bot 0,6 |
| `dunes` | 2,5 | **79 %** | **64 %** | zand 95 % | bot 1,4, struiken 0,7 |
| `duneoases` | 3,1 | **79 %** | **64 %** | zand 97 % | bot 1,5, struiken 0,6 |
| `oasis` | ~19 | 51 % | 30 % | zand 51 %, gras 29 %, water 12 % | gras/varens, palmen |
| `oasisw` | ~22 | 48 % | 26 % | zand 44 %, gras 30 %, water 17 % | gras/varens, palmen |

In de oase is het aantal objecten hoog door het gras en de varens op de oever; de lege tegels zijn het zand ertussen.

## 5. Bevindingen per spot
| Spot | Foto | Oordeel |
|---|---|---|
| `canyon` (1980 356) | `canyon_eye_n.png`, `canyon_eye_s.png` | Smalle spleetkloof: hoge gelaagde wanden en richels werken goed. Vloer = rood zand met grijze kleivlekken en een losse kloofkei; verder leeg. |
| `mesa` (2952 −954) | `mesa_low.png`, `mesa_top.png`, `mesa_eye_s.png` | **Zwak.** Van boven één oranje vlak met puntjes: vinnen en naalden lezen als rotsen, maar het plateau zelf is egaal rood zand (88 %) met struikjes die op afstand wegvallen. Geen bodem- of kleurvariatie, geen begroeide plekken, geen bomen. |
| `spring` (2257 −990) | `spring_eye_e.png`, `spring_eye_n.png`, `spring_low.png` | Kloofbodem met torens en gelaagde wanden: **het mooiste kloofbeeld**. Maar de vloer is één grote grijze pan gebarsten klei (28 % van de bovenblokken) met harde treden; de wandbron met suikerriet en een groen plekje is het enige groen. |
| `hoodoo` (2420 719) | `hoodoo_eye_e.png`, `hoodoo_low.png`, `hoodoo_top.png`, `hoodoo_topn.png` | **Levendig** van dichtbij (pilaren, kappen, cactussen, struiken). Van boven een gelijkmatig "woud" van even hoge staven: overal dezelfde dichtheid, geen open plekken, geen uitschieter in maat. 's Nachts geen lichtpuntje (zonnesteen op afstand onzichtbaar). |
| `hscarp` (2368 370) | `hscarp_low.png`, `hscarp_eye_s.png` | Steilrand met treden, vinnen en naalden leest goed; boven op het plateau hetzelfde lege oranje als `mesa`. |
| `salt` (−2700 −1560) | `salt_eye_n.png`, `salt_eye_e.png`, `salt_low.png`, `salt_top.png`, `salt_topn.png` | Het zeshoekpatroon is van boven sterk. Op ooghoogte: kristallen gelijkmatig verspreid (leest als strooisel), pilaren klein en grijs aan de horizon, pekelpoelen alleen van boven te zien. 60 % lege tegels, niets hoger dan 6. |
| `dunes` (−2144 579) | `dunes_low.png`, `dunes_top.png`, `dunes_eye_n.png`, `dunes_eye_e.png` | **De leegste biome.** Een zee van één beige zandkleur; de rotsuitstekels zijn de enige accenten (klein, oranje), plus een fossiel en wat botten. 64 % van de chunks zonder één object. De vorm van de duinen zelf is goed. |
| `duneoases` (2700 −110) | `duneoases_low.png`, `duneoases_eye_n.png` | Hetzelfde beeld; mini-oase en cactusveld liggen buiten beeld (bewust verborgen). Trappen in de duinflanken zichtbaar (blokraster, bekend). |
| `oasis` (918 −2376) | `oasis_top.png`, `oasis_low.png`, `oasis_eye_n.png` | **Goed**: poel, brede grasoever, palmgroepjes, bloemen. Zwak: het zand tussen de poelen is kaal, en aan de oostkant staat langs het water een steile zandwand van 4–6 met steen erin (de kom snijdt de vlakte aan). |
| `oasisw` (−2560 1280) | `oasisw_low.png` | Als `oasis`: een poel met palmen tussen kaal zand. |
| `z12e` (1500 0) | `z12e_eye_e.png`, `z12e_eye_w.png`, `z12e_low.png` | `withered_expanse` met dode bomen en gras; aan de ring 1-kant een meer (41 % water). Overgang naar de ring 2-steilrand in orde. |
| `z23e` (3500 0) | `z23e_eye_e.png`, `z23e_eye_w.png`, `z23e_low.png` | `dry_riverbeds`: dode bomen, botten, as en basalt: **levendig** en verhalend; harde rand naar de oranje ring 2-kant (bekend). |

**Geen echte bugs gevonden** (geen zwevende blokken of verkeerde bodem op de foto's); niets gerepareerd.

## 6. Perf-baseline
Gezamenlijke run ring 2 + 3 (`tools/test/areas/perf-g2.txt`: ring 2 `canyon`, `hoodoo`, `dunes`; ring 3 `ember`, `sulfur`,
`great`; gedeelde `zone23`; 1 warm + 1 koud). Start op het sein van main (ring 3 bouwt nu met vier substromen, en onder
die last meet perf onzuiver). De uitkomst komt hier. Referentie: ring 2 zat in change 30 op ≈ 1,36× (noise; budget 1,75×).

## 7. Featurevoorstellen per biome
Gemeenschappelijk: alle voorstellen zijn chunk-lokale features (`jittered_grid` → filters → `biome`) of surface rules,
behalve H1; dus geen of verwaarloosbare kosten in de noise-stap (de features-stap kreeg in 30c +0,07 ms voor 15 features;
budget +2 ms). Blikvangers op het generieke raster van ring 4 (`feature/Landmarks.Grid` met `reaching`/`anchoredIn`):
hooguit één per cel van ~384 (≈ 1 per 384 blokken in de eigen biome), alleen als de cel in die biome valt.

| # | Biome | Voorstel | Waar | Dichtheid | Perf | Complexiteit |
|---|---|---|---|---|---|---|
| C1 | canyonlands | Bodemvlekken op plateau en tafelbergen: grind, grove aarde, rode zandsteen ("woestijnplaveisel"), gebarsten klei | plateau Y ≥ 118 | ~25–30 % in vlekken van 10–40 (eigen ruis) | surface rule, ~0 | laag |
| C2 | canyonlands | Bosjes knoestige dode bomen/jeneverbes (hergebruik `bareTree`/dode boom + bladstruik) en struikplukken | plateau, treden, brede kloofbodem | bosjes op ruis (~20 % van het land), 0,3–0,6 boom per bosjechunk | feature, klein | laag |
| C3 | canyonlands | Kleipannen opbreken (rood zand/grind mengen) + groene sijpelplekken rond bronnen en in lage kolken (mos, varens, gras, struikjes) | kloofbodem | ~10–15 % van de kloofbodem | surface rule + features, klein | laag |
| C4 | canyonlands | **Blikvanger:** natuurlijke rotsbrug over een hoofdkloof (span 20–40, gelaagd) | hoofdkloof, cel 448 | ~1 per 448 blokken kloofland | feature over meerdere chunks (`reaching`), klein | middel-hoog (kloofkruising zoeken) |
| H1 | hoodoo_fields | Dichte en open stukken: pilaarkans per vlek (ruis, 0,4 → 1,0) | het hele veld | ~30 % open | noise-stap: één 2D-ruis extra, ~+1 % | laag-middel |
| H2 | hoodoo_fields | Droge geulen met grind/rood zand in de vloer + puin | vloer | 1 geul per ~150 blokken | surface rule | laag |
| H3 | hoodoo_fields | **Blikvanger:** reuzenhoodoo of "kathedraal" (pilaar 55–75 met 2–3 kappen) of grote boog (span 25–35) | cel 384 | ~1 per 384 blokken | extra `rock_pillars`-laag met grote cel (vroege exit), ~0 | laag-middel |
| S1 | salt_flats | Drukruggen: zoutblok 1 hoog op de zeshoekranden, in vlekken | korst | ~15 % van de vlakte | feature leest `PolygonGrid`, klein | laag-middel |
| S2 | salt_flats | Kristallen in clusters (op ruis) i.p.v. gelijkmatig; gebleekte dode bomen (gestript hout) op de eilandjes | korst, eilandjes | zelfde aantal kristallen; 1 boom per ~2 eilandjes | feature, ~0 | laag |
| S3 | salt_flats | Natte spiegelpannen: 1 laag water op de korst in gesloten vlekken | korst | ~8 % | surface rule + kom-check | middel (waterregels, uitdroging) |
| S4 | salt_flats | **Blikvanger:** zoutkoepel (8–15 hoog, 20–30 breed, zoutblok) met kristalholte | cel 384 | ~1 per 384 blokken | feature over meerdere chunks | middel |
| D1 | dune_sea | Rood-zandduinen als subzone | duinen | ~25 % van de duinen, vlekken ~300 | surface rule, ~0 | laag |
| D2 | dune_sea | Dichtere flora in plukken in de dalen; half begraven dode bomen/takken op de flanken | dalen, flanken | dalplukken ~2× de huidige; 1 boom per ~4 chunks | feature, klein | laag |
| D3 | dune_sea | **Blikvanger:** half begraven reuzenskelet (ribben 20–30 lang uit het zand; `BoneCarcassShape` van ring 4) | cel 384 | ~1 per 384 blokken | feature, klein | middel (port) |
| D4 | dune_sea | Grotere rotsuitstekels / inselberg 20–30 hoog (opgeschaalde `dune_rock`) | cel 448 | ~1 per 448 blokken | feature over meerdere chunks | middel |
| O1 | oasis_flats | Palmbossen op ruis (ook buiten de poelen), met gras/grove aarde | zand tussen de poelen | ~15 % van de vlakte | feature, klein | laag |
| O2 | oasis_flats | Plukken droog gras/struiken op het zand + droge poelkuilen (gebarsten klei, 1 diep) | zand | ~1 kuil per 6 chunks | feature/surface rule | laag |
| O3 | oasis_flats | **Blikvanger:** oeroude reuzenpalm/vijg (2 × 2-stam, 20+ hoog) bij een bronmeer met rotsbron | cel 384 | ~1 per 384 blokken | feature (boom via `anchoredIn`) | middel |
| R1 | dry_riverbeds | Niets nieuws nodig; eventueel een droge waterval (rotsdrempel in de bedding) als blikvanger | beddingen | — | — | — |

Volgorde-advies: eerst D1/D2/S1/S2/C1/C2/C3/O1 (laag, groot effect op de lege vlakken), daarna de vijf blikvangers.

## 8. Sparvragen (AskUserQuestion-vorm)
1. **Blikvangers (~1 per 300–500 blokken)?** A *(aanbevolen)* één per biome op een raster van ~384 (C4, H3, S4, D3, O3): middel, alleen features. B twee per biome: meer variatie, dubbel werk. C alleen duin, zout en plateau: minder werk, ongelijk.
2. **Duinen?** A *(aanbevolen)* D1 + D2 (rood-zandduinen ~25 %, dichtere dalplukken, half begraven dode bomen): laag. B alleen meer flora, één zandkleur. C kammen kaal laten, alleen de blikvanger.
3. **Zoutvlakte?** A *(aanbevolen)* S1 + S2 (drukruggen, kristalclusters, gebleekte bomen): laag-middel. B daarbij S3 (natte spiegelpannen): mooi, meer risico (gesloten kom, uitdroging). C alleen de kristallen hergroeperen.
4. **Kloofplateau en tafelbergen?** A *(aanbevolen)* C1 + C2 (bodemvlekken + bosjes dode bomen/struiken): laag. B alleen bodemvlekken. C laten (obstakelrol), focus op de kloofbodem.
5. **Kloofbodem?** A *(aanbevolen)* C3 (kleipannen opbreken + groene sijpelplekken): laag. B alleen sijpelplekken. C laten.
6. **Hoodoovelden?** A *(aanbevolen)* H1 + H2 (dicht/open via ruis + geulen): kleine noise-kosten. B alleen H2 (0 noise-kosten). C laten.
7. **Oase?** A *(aanbevolen)* O1 + O2 (palmbossen, droog gras/struiken, droge poelkuilen): laag. B alleen meer losse struiken. C laten.
8. **Aanpak kleine terreinvormen?** A *(aanbevolen)* alles als chunk-lokale features; de noise-stap blijft gelijk behalve H1. B ook DF-vormen (heuveltjes, geulen) binnen ≤ 1,75×. C geen terreinvormen, alleen aankleding.
