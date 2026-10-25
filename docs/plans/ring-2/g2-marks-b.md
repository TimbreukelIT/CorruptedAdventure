# G2 ring 2 — substroom marks-b: blikvangers van de vlakke biomes

Scope: `worldgen`. Branch `g2r2/marks-b` (worktree `wt/g2r2-marks-b`, stroom `g2r2-marks-b`, poort n = 11). Besluit
G2-R2-1 (`fase-1-wereld.md` §G2 "Uitwerking ring 2"; voorstellen S4, D3, O3 in `g2-walk.md` §7): één blikvanger per
biome op een wereldraster van ~384. Deze substroom: zoutkoepel (`salt_flats`), reuzenskelet (`dune_sea`), reuzenpalm bij
een bronpoel (`oasis_flats`). De rotsbrug en de reuzenhoodoo doet marks-a.

## Plan en aanpak
- **Plek per cel:** ring 4's `feature/Landmarks.Grid` (cel 384, marge 40, 8–10 pogingen, kans 0,8/0,75/0,85), per soort
  een eigen zout. De eerste kandidaat die past wint (`Landmarks.reaching`/`cachedInCell`, winnaar per cel onthouden).
- **Puur terrein in ring 2:** ring 2 heeft geen Java-terrein zoals ring 4 (`IslandField`); de hoogte komt uit
  `ChunkGenerator.getBaseHeight` (`WORLD_SURFACE_WG` en `OCEAN_FLOOR_WG`): de eigen kolomiteratie van de noise, dezelfde
  hoogte als de WG-heightmap van de chunk, zonder ooit een blok te lezen. Biomes uit de biome source
  (`Landmarks.biomeAround`). Eerst de biome (goedkoop), dan de kolom, dan ringen rond het anker.
- **Schrijven:** elke chunk schrijft alleen zijn eigen deel van het plan (`LandmarkPlan`, gecachet per site), laatst in
  zijn stap: voeten vanaf de eigen bevroren bodem, blokken over wat de eigen chunk eerder plaatste, planten en kristallen
  alleen in lucht en met steun in de eigen chunk.
- **Volgorde-onafhankelijk:** de features van zout en duin blijven in hun chunk (de palmen van de mini-oases staan ≥ 4 van
  de chunkrand); het skelet spaart water, stammen en bladeren (een mini-oase houdt zijn poel en palm). In de oase reiken de
  palmkronen wél in buurchunks: daarom krijgen de drie oasepalm-plaatsingen van `Ring2Features` een eigen filter
  `ring_2_landmark_clearance` (marge 6 rond de voetafdruk van de reuzenpalm). Zo schrijft niets van een buurchunk in een
  blikvanger.
- **Bronpoel:** een `surface_pool` (zoals de mini-oases) in de chunk van het anker; het anker ligt daarvoor midden in zijn
  chunk (lokaal 7–8), zodat poel + rand nooit verschuiven. De poel komt in `LOCAL_MODIFICATIONS` (vóór de oaseplanten),
  de palm laatst in `VEGETAL_DECORATION`.
- **Vindbaar:** `/ca flatmarks x0 z0 x1 z1` (eigen commando naast `/ca landmarks`, om conflicten met marks-a te
  vermijden).

## Startwaarden
| Blikvanger | Vorm | Plek |
|---|---|---|
| Zoutkoepel | holle ovale koepel 20–30 breed (straal 10–15, rek 0,8–1,0, bultig), 8–15 hoog, schil 2,2 van zoutblok; buiten gepolijst zout in vlekken (~¼) en zoutkorst op de bovenvlakken; 3–5 scheuren (spleten van 1 breed, 2–5 hoog); 1–2 boogingangen 3 breed en 4 hoog; zoutkristallen op de binnenkant van de schil (0,22 per vrije plek) en op de vloer (0,05) | kale korst (Y 65, geen eilandje) op 24 punten rond het anker; 26 binnen de biome |
| Reuzenskelet | 22–30 lang, 0 of 1 in het zand gezakt; ruggengraat langs de grond die naar de staart wegzakt (1 per 3); ribben om de 2 als bogen 6–9 uit en tot ~11 hoog, iets naar achter, 12 % weg, 25 % afgebroken, geen gaten langs een rib; schedel half begraven (3,8 × 2,7 × 2,5) met oogholtes en een kaak; 4–8 losse botten rond | dal of lage flank (eerste open blok ≤ Y 84), ±9 op ringen 8 en 15; 28 binnen de biome |
| Reuzenpalm | stam 2 × 2, 20–28, vanaf een derde 2–3 leunend naar de poel; voet met een ring palmhout (75 %, soms 2 hoog) en 3–5 wortels van 2–3; kroon met een knop en 12 hangende bladwaaiers van 6–8 (zakken 2–3, toppen hangen 1–2), bladeren persistent; rots van 2–4 klompen (zandsteen, glad zandsteen, crème/oranje kloofsteen) op 8 aan de overkant; varens en gras bij de voet. Bronpoel straal 3–4, 2 diep in de kern, grasrand 2 met gras/varens/bloem (0,55) en suikerriet 1–3 (0,4) | droge oasevlakte (≤ Y 72), poel ±2 op straal 6, stam op 9 van de poel op droge vlakke grond (eerste van 8 richtingen); 28 binnen de biome |

## Bouwnotities en metingen (seed 20260930)
- **Gevonden met `/ca flatmarks`** (verse werelden `run/g2mb`, `run/g2mb3`), in vijf vakken van 1.000 × 1.000 rond de
  bekende ring 2-spots:

  | Soort | Ankers (x y z) |
  |---|---|
  | `salt_dome` | −2798 66 −1801, −2602 66 −1582, −2860 66 −1262, −2463 66 −1427, 2743 66 1063, 2445 66 1494, 2850 66 1295 |
  | `dune_skeleton` | −2472 82 −1112, −2424 83 700, −1982 84 652, −2109 71 837, −2129 79 1220, 2554 67 −179 |
  | `giant_palm` (anker = bronpoel) | 1208 64 −2792, 855 70 −2472, 1447 72 −2553, 536 71 −2153, 1111 69 −2185, 1192 71 −2233, −2441 70 1079, −2568 67 1384, 2759 71 −1641, 2232 71 −1465, 2376 69 −1209 |

  Ongeveer één per cel in de eigen biome (de vlakken zijn klein, dus vaak minder; in het oasegebied 500…1.500 / −2.900…−1.900
  zes palmen). Twee palmen staan ~95 uit elkaar (1111 −2185 en 1192 −2233: buurcellen, marge 40).
- **Bronpoel:** in de eerste wereld faalde één poel (−2489 1096: grond 66–69 over de voetafdruk, meer dan de `max_step`
  van de poel). Nu kiest de site een vlakke plek (grond op het anker en ringen 3 en 6 hooguit 2 uit elkaar, `max_step` 3);
  daarna hadden alle 11 palmen hun poel (26–52 waterblokken binnen 6 van het anker) en een stam van ≥ 84 logs.
- **Kosten:** de keuze per cel kost een paar kolomiteraties van de generator (`getBaseHeight`), gemeten via
  `/ca flatmarks` ~11 s voor een vak van 1.000 × 1.000 (≈ 7 cellen × 3 soorten, ~0,1–0,4 s per cel en soort, eenmalig per
  wereld en cel; daarna uit de cache). In de wereldgeneratie draait dat op de werkthread van de eerste chunk die het vraagt.
  Het skeletplan vraagt ~100 kolommen (eenmalig per skelet).
- **Zwevend:** 0 zwevende componenten van landmarkblokken in 7 + 11 gebieden (2 koepels, 3 skeletten, 2 + 11 palmen; 7 × 7 chunks,
  26-buren, steun = andere vaste grond); in de unit tests 40 seeds per soort.
- **Water:** geen stromend water binnen bereik van de koepels en palmen boven Y 62 (grotwater eronder; drie plekjes
  stromend water op Y 62 liggen 30–50 blokken van een anker, aan natuurlijke oasepoelen).
- **Determinisme** (`det.sh --all --settle 10`, `dome1`, `skel1`, `palm1`, 7 × 7 chunks): boven Y 60 blok voor blok gelijk
  op vanilla-ertsklodders na (alleen in `palm1`, 45 blokken steen/dioriet/aarde ↔ steenkool); eronder de bekende
  verschillen van instortend `loose_sand` en stromend grotwater (geen eigen blokken van deze change).
- **Visueel:** geen fotoreeks (geen twee vrije slots tijdens deze change); bekeken via bovenaanzichten en doorsneden uit de
  wereld (`build/test/marks-b/*_top.png`, `*_cut.png`): koepel als witte schijf met kristallen binnen, skelet met
  ruggengraat, ribben en schedel goed leesbaar (~25 lang), palm met een kroon van ~16 breed naast een poel en een rots. De
  eerste poel was te klein (straal 2,5–3,5): naar 3–4.

## Open punten
- **Bronpoel zonder val:** de "rotsbron met een waterbron die in de poel valt" is vereenvoudigd tot een poel met een rots
  ernaast (vallend water vraagt een vanilla-eindtoestand zoals `SpringLayout.settle`). Kan later als de gebruiker een val
  wil.
- **Integratie met g2r2-flats:** als die substroom nieuwe palmplaatsingen in de oase toevoegt (palmbossen, O1), moeten die
  ook `Ring2FlatLandmarkFeatures.palmClearance()` krijgen, anders reikt een kroon uit een buurchunk in de reuzenpalm
  (volgorde-afhankelijk). Nieuwe features van flats in duin/zout moeten chunk-lokaal blijven (dan geldt hetzelfde).
- **Volgorde in `Ring2Biomes`:** de blikvangers moeten laatst in hun stap blijven (na nieuwe flats-features); de
  exacte-lijsten in `Ring2BiomesTest` botsen bij de merge met flats.
- **Clearing:** rond een reuzenpalm staan in een vierkant van ~56 × 56 geen oasepalmen (voetafdruk 22 + marge 6 rond het
  anker); visueel beoordelen in de na-rondgang.
