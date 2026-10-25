# G2 ring 2 — substroom flats (duinen, zout, oase)

Scope: `worldgen`. Branch `g2r2/flats` (worktree `wt/g2r2-flats`, stroom `g2r2-flats`, poort n = 8), vanaf `g2/ring2`.
Besluiten G2-R2-2, 3 en 7 (2026-10-25, alle A; `docs/design/fase-1-wereld.md` §G2 "Uitwerking ring 2"), voorstellen D1,
D2, S1, S2, O1, O2 uit `docs/plans/ring-2/g2-walk.md` §7. Alle getallen zijn startwaarden.

## Plan en client/server-impact
Alleen worldgen (datagen-features, één nieuw feature-type, surface rules, ruis): server-side, geen netwerk, geen
client-code. Geen density functions (de noise-stap blijft gelijk). Alles chunk-lokaal en per seed.

| Voorstel | Bouw | Waar |
|---|---|---|
| D1 rood-zandduinen | surface: vóór elke zandregel van `dune_sea` een rode kopie (rood zand / rode zandsteen) waar `ring_2_red_dunes` ≥ 0,2 (~25 %, vlekken ~300; dalbodem en lagen ongewijzigd) | `tools/worldgen/ring2_flats_surface.py` → `noise_settings/overworld.json` |
| D2 dalflora | `dune_sea_thickets`: `dune_valley_flora` op raster 4, kans 0,6 × `ClusterDensity` (`ring_2_dune_thickets` 0,05 → 0,3), tot Y 74 (kammen kaal) | `Ring2FlatsLifeFeatures` |
| D2 half begraven bomen | `dune_sea_buried_trees`: `dead_tree` (ring 4 decor B) met dood hout, stam 1–3 en 2–4 takken laag aan de stam: de kroon steekt uit het zand; raster 16 marge 2 kans 0,3, bovenblok Y 70–88 op zand/rood zand | idem |
| S1 drukruggen | nieuw type `salt_ridge`: per chunk elke barst (bovenblok zoutblok op Y 65, lucht erboven) waar `ring_2_salt_ridges` ≥ 0,35 (~14 %) een zoutblok erop; onderbreking 8 %, kruispunt van 3–4 barsten 2 hoog (35 %) | `SaltRidgeFeature`, `Ring2FlatsLifeFeatures` |
| S2 kristalclusters | `salt_flats_crystals`: raster 4 kans 0,5 × `ClusterDensity` (`ring_2_salt_crystal_clusters` 0,2 → 0,5) i.p.v. raster 8 kans 0,25: gemiddeld ~1 per chunk, maar in klodders | `Ring2Features` |
| S2 gebleekte bomen | `salt_island_bleached_trees`: `dead_tree` van gestript dood hout, 3–6 hoog, 2–4 takken; raster 16 marge 2 kans 0,35 op zand/rood zand (eilandjes) | `Ring2FlatsLifeFeatures` |
| O1 palmbossen | `oasis_palms_sand` op hetzelfde palmraster (cel 8, marge 2: stammen ≥ 5 uit elkaar, de banden van de palmfeatures blijven gescheiden), kans 0,6 × `ClusterDensity` (`ring_2_oasis_groves` 0,3 → 0,45, bodem 1/24 ÷ 0,6): daarbuiten precies als voorheen (1/24); grasvloer met grove-aardevlekjes uit de surface waar de ruis ≥ 0,33 (~15 % van het open zand); het gras krijgt vanzelf de oasebegroeiing (`oasis_vegetation` op gras) | `Ring2Features`, surface |
| O2 struikplukken + droog gras | `oasis_scrub` (`oasis_transition_vegetation`, raster 4 kans 0,5 × `ring_2_oasis_scrub` 0,1 → 0,35) en `oasis_scrub_grass` (`dry_grass` op grove aarde), alleen op open zand (`ring_2_pools` < 0,02); grove-aardevlekjes uit de surface (scrub ≥ 0,1 en `minecraft:surface` ≥ 0,25) | `Ring2FlatsLifeFeatures`, surface |
| O2 droge poelkuilen | `oasis_dry_hollows`: `surface_pool` met het nieuwe veld `dry_floor` (gebarsten klei): rafelig, straal 2,5–5, 1 diep, rand grove aarde/zand met dorre/dode struiken; raster 16 kans 0,2 op zand met `ring_2_pools` < 0 (~1 per 6 oasechunks met open zand) | `SurfacePoolFeature`, `Ring2FlatsLifeFeatures` |

Volgorde (vast, achteraan de stap): zout `LOCAL_MODIFICATIONS` … eilandbotten → drukruggen; `VEGETAL_DECORATION` … eilandcactus →
gebleekte bomen. Duin `VEGETAL_DECORATION` … cactusgroepjes → struikgewas → begraven bomen. Oase `LOCAL_MODIFICATIONS` rotsen →
droge kuilen; `VEGETAL_DECORATION` … overgangsflora → struikplukken → droog gras.

Ruis (nieuw, `worldgen/noise/`): `ring_2_red_dunes` (−8; 1, 0,5), `ring_2_dune_thickets` (−5), `ring_2_salt_ridges` (−6),
`ring_2_salt_crystal_clusters` (−4; 1, 0,5), `ring_2_oasis_groves` (−6), `ring_2_oasis_scrub` (−5). Drempels uit de
stationaire ruis (`Ring2FlatsLifeTest`, 3 seeds): rood 0,2 → 25 %, ruggen 0,35 → 14 %, bossen 0,33 → 15 %.

## Bouwnotities
- Rode duinen: de rode regels staan per regel vóór de gewone (alleen regels met zand krijgen een kopie), zodat de
  volgorde met de kloofsteenlagen exact blijft. Duinfossielen/-botten, mini-oases en rotsuitstekels accepteren nu ook rood
  zand (anders vielen ze in een kwart van de duinen weg).
- De begraven en gebleekte bomen zijn de `dead_tree` van ring 4 (grondobject: eigen random, eigen chunk, hout via
  `TreeOverlap`); geen nieuwe code.
- `salt_ridge` leest alleen de blokken van de eigen chunk (de korst is surface rule; poelen en pilaren van die chunk
  komen eerder in de stap), dus volgorde-onafhankelijk; kruispunten aan de chunkrand tellen geen buren buiten de chunk.
- `surface_pool` heeft een extra constructor zonder `dry_floor`; bestaande configs ongewijzigd (de JSON krijgt het veld
  niet).

## Metingen
(volgt)
