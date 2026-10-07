# Golf 1 — stroom W-nether: 53 (Nether-ring 3-terrein)

Scope: `worldgen` — werkpakket 53, alleen terrein (de biomes zitten in 53b, golf 2); branch `ring3/nether-g1`.

**Doel:** de vorm van de bevroren Nether in Nether-ring 3 (875–1.500, 625 breed), als eigen ring 3-invoer van `ring_blend/nether`:
- `frozen_lava_sea`-vlakte (gestolde lavazee met ijspilaren van vloer tot plafond);
- de `rime_forest`-hallen;
- de `glacier_rifts`-kloven en -tunnels (vorm alvast, het biome volgt in 54).
De warme scheuren worden als vorm voorbereid (smalle spleten in de vlakte). De levendig-eis en het eigen wow (bevroren lavavallen, ijspegels) zijn features in golf 2. Surface tijdelijk vanilla (`packed_ice`, `blue_ice`, basalt, netherrack).

Bron: `fase-1-wereld.md` Nether-ring 3 + sparring-aanvulling; `contract.md` §4; sjabloon change 35 (`ring_2/nether/*`, `ring_edge`-dam, `fluid_levels`, `ring_nether_cave`, determinismeregel Nether, valkuilen "Lavazee per ring", "Nether-carver zet zelf lava").

## Werk
1. `ring_3/nether/terrain` (+ de verwijzing in `ring_blend/nether`):
   - vlakte op Y ~35 met ijspilaren (3D-ruis of kolommen, zoals `masses` van change 35);
   - plafond ~110;
   - regio's via `ring_3/nether/region` (39a).
2. **Vloeistof:** geen zichtbare lavazee; `fluid_levels.ring_3` = 22 (39a, antwoord 2026-10-06: gelijk aan ring 2, dus geen trede en geen lavawanden op 2→3). De ring 2-dam (`ring_edge`) aan de 2→3-grens moet blijven werken.
   - **Harde eis (39a-review):** trede 3|4 (22/32). Aan de ring 3-kant van zone 3→4 vast tot boven Y 32 (eigen `ring_edge`-dam, carvers niet erdoor). `Ring3JsonTest.everyNetherLavaStepIsCoveredBySolidGround` faalt bewust zodra het placeholder-terrein weg is: vervang die tak door een controle op de eigen dam. Test S: `tools/nether-lava-walls.py` op r 875 en 1500 → 0 wanden. Zie `contract.md` §4.
   - Biomes: de router-`temperature` wisselt vanaf zone 2→3 hard op de middellijn (`switch_at` in `ring_blend/nether_temperature`); Nether-3-biomes lezen tot de middellijn alleen `ring_3/nether/region`.
   - Ring 3-biomes gebruiken `ring_nether_cave`, zodat de vanilla-carver geen lava zet.
   - Grens 3→4: let op het lavaniveau van Nether-4 (later); nu placeholder.
3. **Ruimte-check** (vooruitlopend op 54): voelt 625 breed met biome-wissels genoeg? Meet de hal- en kloofmaten.
4. **Prestatie:** indicatief (Nether-2 kostte ~1,95×; het budget voor ring 3 is ≤ 1,75×).

## Client/server-impact
Geen; puur worldgen.

## Tests
- **B**, **D**, **S** (`/ca tpring 3 nether`, `tpboundary 2|3 nether`).
- Grens 2→3 zonder lavawand.
- Nether-1/2 onveranderd (biomes + terreinvorm).
- **W-checklist** voor 4a (een eerste blik: vorm en ruimte).

## Bij afronding
`waves.md` §4.

## Voortgang
Voortgang: 20:45 branch `ring3/nether-g1` (worktree stond op 17c9eb4, teruggezet naar 8e0c637), worktree gemeld, `run/` klaar (poort 25573, RCON 25583)
Voortgang: 20:55 basiswereld `n3-base` (8e0c637) gegenereerd: Nether-ring 1/2, zones 1|2 en 2|3, strook door ring 3
Voortgang: 21:05 terrein-JSON (`ring_3/nether/*`, 12 noises), surface-tak (index 4), tests `NetherRing3JsonTest` + dam-tak in `Ring3JsonTest`; offline voorbeelden gecontroleerd
Voortgang: 21:45 crack-DF goedkoper (range_choice op Y-band + flat-mask; was ~30 ms/chunk door per-blok-ruis), gletsjer/wanden/pilaren lopen af aan de ringranden (`core` = 1 − edge); B groen
Voortgang: 22:00 S op wereld `n3-b`/`n3-c`/`n3-d`: lavawanden 0, portal-test, determinisme, ruimte-check, prestatie (zie hieronder)
Voortgang: 2026-10-07 08:20 hervat; branch en werk intact. Prestatie opnieuw: één server met datapack-dimensies (`nethertest:vanilla` = vanilla-Nether, `nethertest:r1/r2/r3…` = losse terreinen op de Nether-instellingen van de mod), afwisselend op dezelfde coördinaten
Voortgang: 08:51 eerste meetserver stopte door de watchdog (forceload van 256 chunks > 60 s op de drukke machine); `max-tick-time=-1` in de eigen `run/server.properties`
Voortgang: 09:40 metingen `n3-p2` (wisselend, zelfde coördinaten): Nether-3 (mod) 1,59× (6 gebieden) / 1,82× (3 regiovlakken) / 1,87× (4 gebieden) / 1,68–1,78× (gemengde ronde); Nether-2 (mod) in dezelfde gemengde ronde 1,70–1,88×, eerder die ochtend 1,62×; ring 1 (mod) 1,42×. Alleen het terrein (op vanilla's generator): Nether-3 1,03–1,18×, Nether-2 1,09–1,26×, ring 1 0,90×; zonder scheuren 0,76–0,86×, zonder ruwheid 0,72–0,85×. Het verschil mod ↔ terrein (~0,6×) is het ringraamwerk, gedeeld met elke ring
Voortgang: 10:05 gebruiker: prestatie optie A (gelijk aan Nether-2); scheuren masker-eerst; B groen, D groen (geen diff in `src/generated`)
Voortgang: 10:20 review (`/code-review medium nether-g1 53`, 9 bevindingen) afgehandeld, zie "Review"
Voortgang: 10:35 na de reviewfixes: B groen (81 klassen, 426 tests), S op `n3-e`: start zonder fouten, lavawanden r 875 en r 1500 **0**, lava boven Y 22 en lava naast lucht 0 in 5 gebieden (zichtbare lava alleen onderin de scheuren), ring 1/2 gelijk aan 8e0c637 op de bekende vanilla-featurevolgorde na; commit

## Eindstand tests
- **B:** groen (81 testklassen, 426 tests, incl. `ClientIsolationTest`, `NetherRing3JsonTest`, `Ring3JsonTest`).
- **D:** `runData` groen, geen diff in `src/generated` (53 heeft geen datagen).
- **S:** werelden `n3-b`…`n3-e` (seed 20260930): zie de voortgangsregels; portal overworld ring 3 (4752, 16) ↔ Nether-3 (1197, 36, 9) heen en terug (varken), op `n3-b`.
- **Determinisme:** ring 3 omgekeerd geladen identiek (507/507 regiochunks; strook 174/178, verschillen = vanilla-ertsen in zone 2|3 en de testportal); ring 1/2 gelijk aan 8e0c637 behalve vanilla-features die ook tussen twee werelden met dezelfde code wisselen (ertsen, één crimson-fungus-chunk op r 361).
- **Prestatie:** zie Voortgang 09:40 en de beslissing van 2026-10-07 (gelijk aan Nether-2; officieel in INT).
- **Beelden:** `build/n3/b-*.png`, `e-section-*.png` (doorsneden en bovenaanzichten, zelf gecontroleerd: organisch, geen kubussen).

## W-checklist (kort, teststop 4a: eerste blik Nether-3)
- [ ] `/ca tpring 3 nether`: open bevroren vlakte (vloer Y ~35, basalt met ijsplaten), ijspilaren van vloer tot plafond (zandloper, blauwe kern), plafond Y ~108.
- [ ] Rijpwoud-hallen (regio hoog): hallen 30–100 breed met wanden en ribben, ijzig plafond.
- [ ] Gletsjerkloven (regio laag): ijsmassa tot Y ~90–100 met luchtspleet tot het plafond, kloven 8–20 breed, tunnels.
- [ ] Warme scheuren in de vlakte: smalle spleten met magma in de wanden en lava onderin (Y 17–22), alleen in vlekken.
- [ ] Geen zichtbare lavazee, geen lavawand aan `tpboundary 2|3 nether` en bij r 1.500 (3|4); gletsjer en wanden lopen aan de ringranden af.
- [ ] Ruimte: voelt 625 breed met drie vormen genoeg? (vlakken ~250 Nether-blokken; aandeel 41/36/23 %).

## Review (/code-review medium, 9 bevindingen)
1. **Lavapoelen in lage hallenvloeren** (rijp 33 ± 5 plus de sterkste ruwheid kan onder Y 22 openen): opgelost met `lid` (`gradiënt(23→27, 2,5 → −2,5)` in de `max`): vast t/m Y 23 buiten de scheuren. Test `Ring3JsonTest.nether3IsSolidUpToItsLavaLevelOutsideTheCracks` (sterkte tegen de echte ruwheidsruis, zelfde helper als de dam).
2. **Magma-naden zonder scheur** (de surface-regel kent `core` en de maskersterkte niet): magma nu alleen op Y 14–29 (onder de zeevloer, dus alleen zichtbaar in de scheurwanden) en vanaf `crack_patches` 0,32 (masker > 0,1).
3. **Gletsjertop kon het plafond raken**: `glacier_top` = `min(…, ceiling_height − 10)`.
4. **Plafond = uitgerekte kopie van de vloerruis**: eigen noise `nether_ring_3_ceiling` (−7).
5. **Flat caches van ring 3 vullen in elke Nether-chunk**: bekend (RingGate gate't alleen `interpolated`); in de backlog-regel over de raamwerk-overhead opgenomen.
6. **`dam` las de ongecachete `ring_edge` per celhoek**: `edge` is nu een `flat_cache` (één opzoeking per kolom; `core` en de dam delen hem).
7. **Test pinde de surface-tak op index 4**: zoekt de tak nu op `biome_is` en eist alleen "vóór de eerste `hole`-regel".
8. **IJsschijven rond de pilaren in vloer en plafond** (surface-drempel 0,30 = de verbrede voet): bewust zo gelaten, het ijs loopt van de pilaar over de vloer en het plafond uit (ijsvoet/ijskap); 53b herziet de surface met de echte blokken.
9. **CODEBASE.md noemt nog de placeholder**: bewust, de docs voegt INT samen (waves §4.8); zie "CODEBASE-delta".

## Status bij onderbreking (2026-10-06 22:10) — afgehandeld 2026-10-07, zie hierboven
- **Gecommit:** niets (geen review gedaan, dus volgens §4.9 geen commit). Branch `ring3/nether-g1` staat op 8e0c637.
- **Ongecommit klaar op schijf** (worktree `.claude/worktrees/agent-ae1bf33594de89e50`):
  - `density_function/ring_3/nether/*` (21 bestanden: `terrain` = `min(interpolated(shape), cracks)`, `shape`, `rough`, `core`, `edge`, `dam`, regio-gewichten `rime_weight`/`rifts_weight`/`sea_weight`, `floor_height`, `ceiling_height`, `pillar_noise`/`pillars`, `walls`, `glacier_top`/`rift_noise`/`tunnel_noise`/`tunnel_height`/`glacier`, `crack_mask`/`cracks`); 12 noises `nether_ring_3_*`.
  - `ring_blend/nether.json` ring 3 → `ring_3/nether/terrain`; placeholder `ring_3/nether.json` verwijderd (`git rm`).
  - `noise_settings/nether.json`: ring 3-surfacetak op index 4 (biome `placeholder_nether_ring_3`; vanilla packed/blue ice, basalt, magma langs de scheuren, netherrack-catch-all).
  - `NetherRing3Features.RIME_THRESHOLD`/`RIFTS_THRESHOLD` (±0,18); tests `NetherRing3JsonTest` (nieuw) en `Ring3JsonTest` (dam-tak i.p.v. placeholder, contract-DF `ring_3/nether/terrain`).
  - De JSON is gegenereerd met `build/n3/gen.py` en `build/n3/surface.py` (niet gecommit; `build/` is gitignored). Testgereedschap in `build/n3/` (anvil-lezer, renders, compare, stats, perf, portal).
- **Tests gedaan:** B groen (laatste build vóór de laatste kleine testwijziging `core`-check; die wijziging is nog niet gebouwd). S: server start zonder fouten; `tools/nether-lava-walls.py` r 875 → lava 11.948, **0 wanden**; r 1500 → **0 wanden**; geen lava boven Y 22 en geen lava naast lucht in 5 gebieden (alleen zichtbaar onderin de scheuren: 125 blokken in 144 zeechunks); portal overworld ring 3 (4752, 16) → Nether-3 (1197, 36, 9) en terug werkt (varken); determinisme: ring 3 omgekeerd geladen 507/507 + 174/178 identiek (verschillen = vanilla-ertsen en de portal van de test), ring 1/2 gelijk aan 8e0c637 op vanilla-featurevolgorde na (ertsen, één crimson-fungus-chunk die ook tussen twee werelden met dezelfde code wisselt); zone 2|3 verandert (verwacht).
- **Ruimte-check** (wereld `n3-b`, seed 20260930): aandeel in de ring zee 41,5 % / rijp 35,8 % / kloven 22,7 %. Zee: vloer Y 34 (p10–p90 32–38), plafond 107, luchtaandeel 0,84, pilaren 7–17 breed met 80–135 open tussen; rijp: hallen 30–105 breed, wanden 10–35; kloven: 11–18 breed (Y 40/60), massa tot ~Y 95.
- **Prestatie (indicatief, JFR noise-stap, mediaan ms/chunk, 16×16 chunks):** vanilla-Nether 3,1–3,9 (6 gebieden); Nether-3 6,7–7,9; ring 1 5,3; ring 2 7,3. Nether-3 ≈ **2,1× vanilla** (ring 2 ≈ 2,0×, ring 1 ≈ 1,5×): **boven het budget 1,75×**. Nog te doen: oorzaak meten (framework-overhead van ring 1 is al ~1,5×) en beperken, of voorleggen aan main.
- **Ontbreekt nog:** (1) prestatie onder budget of besluit via main; (2) build na de laatste testwijziging, `runData` (D, verwacht geen diff); (3) `/code-review medium nether-g1 53` + afhandeling; (4) commit `worldgen: add Nether ring 3 terrain`; (5) delta-secties hieronder afmaken.
- **Wachtende review:** geen gestart.

## CODEBASE-delta
- **Klassen:** `NetherRing3Features`: `RIME_THRESHOLD` (0,18) en `RIFTS_THRESHOLD` (−0,18), de drempels op `ring_3/nether/region` voor het terrein, de surface-tak en (53b) de `multi_noise` van de preset.
- **Databestanden (tabel "Databestanden"):**
  - `density_function/ring_3/nether/*` (change 53, vervangt de placeholder `ring_3/nether.json`): `terrain` = `range_choice(crack_mask > 0 ? min(interpolated_shape, cracks) : interpolated_shape)`, de ring 3-invoer van `ring_blend/nether` (beide takken noemen hetzelfde `interpolated_shape`, dus één interpolator); `shape` = `max(vloer, plafond, zee·pilaren, rijp·wanden, kloven·gletsjer, dam, lid) + rough`; `lid` = vast t/m Y 23 (geen lavapoelen buiten de scheuren); gewichten `rime_weight`/`rifts_weight` (`flat_cache`, ramp ±0,04 rond de drempels) en `sea_weight` = 1 − beide; `core` = 1 − `edge`; `floor_height` (Y 35 ± 1,5, rijp 33 ± 5, kloven 34 ± 3), `ceiling_height` (108 ± 7 op eigen ruis, 98–116); `pillar_noise`/`pillars` (2D-ruis ≥ 0,52 in het midden, voet en kop verbreed tot 0,30: zandloper); `walls` (rijp, opzet van ring 2: |3D-ruis| < 0,03 + gaten, ×8); `glacier_top` (90 ± 8, −60 × `edge`, hoogstens plafond − 10), `rift_noise` (|ruis| −7 < 0,05–0,12, naar boven breder), `tunnel_noise`/`tunnel_height` (buizen op Y 56 ± 12), `glacier` = `min(massa, kloof, tunnel)`; `edge` (`flat_cache` van `ring_edge` ring 3, 48), `dam` = `5 × edge × gradiënt(33→37) − 2,5` (vast tot Y 33 op beide grenzen; trede 3|4); `rough` = `(0,7 + 0,5 × (rijp + kloven)) × 3D-ruis − 0,1`; `crack_mask` (`flat_cache`: vlekken × zee × core), `cracks` (per blok, alleen Y 15–42 en waar het masker open is: |ruis| < 0,035 × masker × V-vorm, bodem Y 17 → lava tot 22).
  - `noise/nether_ring_3_*` (13): `floor` (−6), `ceiling` (−7), `pillars` (−5), `walls` (−6), `wall_gaps` (−5), `glacier` (−6), `rifts` (−7), `rift_wobble` (−4), `tunnels` (−7), `tunnel_height` (−6), `rough` (−4, [1, 0,7, 0,5]), `crack_patches` (−7), `cracks` (−6, [1, 0,5]).
  - `noise_settings/nether.json`: surface-tak voor `placeholder_nether_ring_3` op index 4 (na ring 2, vóór `hole`→lava): pilaren (`nether_ring_3_pillars` ≥ 0,30, Y ≥ 30) packed ice met blue-ice-kern (≥ 0,62), gletsjermassa (regio ≤ −0,18, Y 30–112) packed/blue ice, scheurwanden (`crack_patches` ≥ 0,32 en |`cracks`| ≤ 0,05, Y 14–29: alleen zichtbaar in de scheur) magma, vloeren basalt/packed ice (zee) of netherrack/packed ice (rijp), zee-ondergrond basalt (6 diep), rijp-plafonds packed ice, rest netherrack. Tijdelijk vanilla; 39c/53b zetten de ring 3-blokken.
  - `ring_blend/nether.json`: ring 3 → `ring_3/nether/terrain`.
- **Tests:** `NetherRing3JsonTest` (gewichten op de drempels, één interpolatie in `terrain`, noises bestaan, surface-tak index 4 met dezelfde drempels en noises, Nether-3-biomes zonder carvers); `Ring3JsonTest` (contract-DF `ring_3/nether/terrain`; trede 3|4 bewaakt door de dam van ring 3, met de echte ruwheidsruis; scheuren dicht waar `edge` 1 is).
- **Valkuilen:**
  - `cache_2d` helpt niet voor per-blok-ruis buiten de interpolatie: de noise chunk doorloopt een cel kolom voor kolom per Y, dus de laatste-kolom-cache mist steeds. De eerste scheurversie (per blok ruis op elke hoogte) kostte ~30 ms/chunk; met een Y-band en een `flat_cache`-masker in `range_choice` ~1 ms.
  - Een term met gewicht 0 in een `max` moet −2,5 opleveren (`w · (t + 2,5) − 2,5`), niet 0: met 0 zakt de basisdichtheid nergens onder 0 en bepaalt de ruwheid alleen de ruimte (zie backlog over de ring 2-dam).
  - Gereedschap (niet gecommit, `build/n3/`): `gen.py`/`surface.py` (schrijven de JSON), `dfeval.py`/`preview.py` (offline doorsneden), `anvil.py`/`render.py`/`stats.py`/`compare.py` (regiobestanden lezen), `perf2.py` + `perfpack.py` (meten tegen vanilla in één server via datapack-dimensies).

## Beslissingen
- 2026-10-06 · worldgen · Nether-3-regio-as: midden `frozen_lava_sea` (~45 %, drempels ±0,18), hoog `rime_forest`, laag `glacier_rifts` (gebruiker, vraag 1 = A) · hoofdbiome het grootst, elke overgang via de vlakte.
- 2026-10-06 · worldgen · `glacier_rifts`: ijsmassa tot Y ~90–100 met een luchtspleet tot het plafond, kloven 8–20 breed tot ~Y 32, tunnels door de massa (gebruiker, vraag 2) · kloven zichtbaar van bovenaf, niet beklemmend.
- 2026-10-07 · worldgen · Prestatie Nether-3 geaccepteerd als "gelijk aan Nether-2" (gebruiker, optie A): in dezelfde wisselrondes 1,59–1,88× vanilla tegen Nether-2 1,62–1,95× (terrein alleen 0,88–1,05× tegen 1,09–1,26×); de rest is het ringraamwerk. De officiële JFR-meting doet de integratie op een rustige machine.
- (implementatie) Scheuren: masker eerst (`range_choice` op `crack_mask`), dan de Y-band; buiten de scheurvlekken kost een blok één flat-cache-opzoeking (−0,07× vanilla, vorm identiek).
- (implementatie) Dam ring 3 = `5 × edge × gradiënt(33→37) − 2,5` als term van de `max` (vast tot Y 33 op de grens); gletsjer, rijpwanden, pilaren en scheuren lopen binnen 48 blokken van beide ringgrenzen af.

## Backlog
- `worldgen`: overhead van het ringraamwerk in de noise-stap (`ring_blend` per blok, `RingChunkGenerator`, vloeistofkeuze): ~0,5–0,9× vanilla bovenop het terrein, raakt elke ring (ring 1 in de Nether al 1,42× met vanilla's eigen terrein). Meten en verlagen in een eigen change; meetmethode: `build/n3/perf2.py`/`perfpack.py`-aanpak (datapack-dimensies met losse terreinen naast vanilla in één server).
- `worldgen`: 53b: de Nether-3-biomes krijgen `ring_nether_cave`; die mag niet door de dam van trede 3|4 snijden (contract §4). `NetherRing3JsonTest.nether3BiomesHaveNoCarversYet` faalt bewust zodra er carvers komen.
- `worldgen` (observatie): in `ring_2/nether/terrain` is de dam-term 0 (niet −2,5) waar `edge` 0 is, waardoor de basisdichtheid nergens onder 0 komt en de hallen door de ruwheidsruis worden bepaald. Goedgekeurd terrein; alleen ter info.
