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
