# G2 ring 2 — marks-a: rotsbrug en reuzenhoodoo

Scope: `worldgen`. Substroom `g2r2-marks-a` (branch `g2r2/marks-a`, worktree `wt/g2r2-marks-a`, poort n = 10). Besluit
G2-R2-1 (`fase-1-wereld.md` §G2 "Uitwerking ring 2"; voorstellen C4 en H3 in `g2-walk.md` §7): één blikvanger per biome
op een wereldraster; deze substroom doet `canyonlands` (natuurlijke rotsbrug over een hoofdkloof) en `hoodoo_fields`
(reuzenhoodoo). De andere drie blikvangers doet `g2r2-marks-b`.

## Plan
- **Alleen features** (besluit 8: de noise-stap blijft gelijk). Geen surface rules, geen DF-wijzigingen.
- **Puur terrein zonder blokken van buren:** ring 2 heeft geen Java-terreinveld (zoals `VolcanicField`/`IslandField`).
  Nieuw: `Ring2PureTerrain` leest de 2D-oppervlaktehoogte `ring_2/overworld/height` uit het DF-register en bedraadt die
  met de eigen ruisinstanties van de wereld (`RandomState.getOrCreateNoise`, dezelfde instanties als het terrein). Dat is
  de hoogte vóór de fijne vormen van `canyon_details` (spleten, richels, rotsen) en de 3D-ruwheid; de blikvangers vangen
  dat verschil op met een landhoofd (brug) en een wortel (hoodoo) onder de pure hoogte.
- **Kloofsteenlagen** als de surface rules: periode 48 vanaf Y 8 (rood 22 / oranje 12 / crème 14), ondergrens + de
  surface-diepte van de kolom (`minecraft:surface`-ruis × 2,75 + 3), zodat brug en hoodoo dezelfde banden tonen als de
  wanden ernaast (op ±1 na: het kleine random deel van vanilla's surface-diepte ontbreekt).
- **Raster en keuze:** ring 4 `Landmarks.inCell` (eigen random per cel, eerste geaccepteerde kandidaat); de beslissing
  (vorm) wordt per wereld en cel onthouden, want de kloofscan is het dure deel. Elke chunk vraagt welke blikvangers hem
  raken en schrijft alleen zijn eigen deel; één plaatsing per chunk zonder modifiers, achteraan `LOCAL_MODIFICATIONS` van
  de eigen biome (de begroeiing daarna groeit op en rond de vormen).
- **Vinden:** `/ca landmarks x0 z0 x1 z1` noemt nu ook `rock_bridge` en `giant_hoodoo`.

## Startwaarden
| | Rotsbrug (`canyonlands`) | Reuzenhoodoo (`hoodoo_fields`) |
|---|---|---|
| Raster | cel 448, kans 1, 24 pogingen, marge 48 | cel 384, kans 1, 12 pogingen, marge 40 |
| Plek | kandidaat op een kloofbodem in `canyonlands`; 16 richtingen × ±44 blokken pure hoogte; de kortste oversteek | pure vloer Y 82–96, ≤ 4 oneffen over straal 26; `hoodoo_fields` tot 40 rondom |
| Eisen | dek = 1 onder de laagste van de twee hoogste punten, ≥ Y 112 en ≥ 24 boven de bodem; span rand–rand 18–40; achter elke rand 6 blokken grond binnen 3 onder het dek; `canyonlands` elke 8 blokken langs de brug tot 12 voorbij de einden | — |
| Vorm | dek in de lagen, kroon 0–2, dikte 3–5 in het midden + 3–7 naar de randen (kubisch), breedte 3–5,2 (smaller in het midden, rafelig), afgeronde buitenrij, 5 blokken het plateau in met een landhoofd tot 12 onder het dek, 30 % rood zand bovenop | hoofdpilaar 55–75 boven de vloer, 1–2 lagere (0,55–0,8) die tot 0,08 per blok naar buiten hellen; voet 5–6,5 → lijf 3,6–4,4 (bij 15 %) → hals 2,6–3,2, zwelling per band, ruw per blok; kap 5–7 breed, 3–4 dik (zijpilaren 2–3), rafelige rand, geschroeide zandsteen met 2 blokken hals eronder; schacht tot 10 onder de pure vloer; puin (14 % van de kolommen in een ring van 5 rond de voeten, alleen op kale vloer) |

## Bouwnotities
(volgt)
