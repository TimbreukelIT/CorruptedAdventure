# Change 23b — Ring 2-terreinvormen: spleetkloven, tafelbergen, rotsvormen, wanden, droge watervallen, oaseterrein

Scope: `worldgen` — change 23b (vervolg op change 23, vóór change 25)

**Doel:** het terrein van ring 2 wordt avontuurlijker, zodat de kloven echt de hoofdrol spelen. Het plateau is nu te vlak en te makkelijk over te steken, en het kloofgebied voelt leeg (teststop 2, `docs/plans/teststop-2-feedback.md`). Deze change voegt **terreinvormen** toe, allemaal als density functions en surface rules. Ze zijn deterministisch per seed en hangen niet af van buurchunks:
- **spleetkloven** (smalle, diepe kloven): een netwerk dat het plateau in stukken knipt, met overdekte stukken en klemblokken als bruggen;
- **getrapte tafelbergen** op het plateau;
- **rotsnaalden, grote keien en ribben** (vinnen), soms met een gat erin (boog). Dit gebeurt via één generiek density-function-type `rock_pillars`, dat change 30 hergebruikt voor de hoodoos;
- **kloofwanden** met richels, wandnissen/wandgrotten en een overhangende rand van harde kaprots aan de plateaurand;
- **droge watervallen** (hangende zijtakken, treden met een droge kolk in de kloofbodem) en **puinhellingen** als klimroute;
- **oaseterrein**: minder maar grotere poelen, met zones in de surface rules (oever, overgangsring, woestijn).

Alle **aankleding met features** (puin, losse keien, rotsval, bronnetjes, oasebegroeiing, palmgroepjes) staat in **change 23c** (`docs/plans/change-23c.md`).

Bron: `docs/plans/teststop-2-feedback.md` (punten 1, 1b, 2), `change-23.md` (bouwnotities, testresultaten, afstemvragen teststop 2), `fase-1-wereld.md` §2 Ring 2 (Uitwerking change 23), plannen 25, 26, 30 en 31.

Buiten scope:
- features en placement (23c);
- ertsen (25), grotten/reservoirs/zandgrotten (26), extra biomes en hoodoovelden (30), `dry_riverbeds` (31);
- textures, waaronder het palet van `canyon_cobblestone` (37);
- structures zoals klifwoningen in de nissen (fase 3) en mobs (fase 4): backlog.

**Status:** **gebouwd 2026-10-02** (zie Bouwnotities onderaan); plan **afgestemd 2026-10-01** (alle ontwerpvragen beantwoord, zie "Antwoord:"-regels; afwijkende keuzes van de gebruiker: 23b-8, 23b-9 C, 23b-13 B, 23b-17). 23b wordt gebouwd, daarna 23c, daarna direct change 25 (**geen** blokkerende stop tussendoor). De in-game terreincheck van ring 2 is **teststop 2b: één blokkerende stop na change 30, vóór change 31** (gebundeld: 23 + 23b + 23c + 25 + 26 + 30; vraag 23b-17). Teststop 3 na change 37 blijft.

## Afstemming (2026-10-01)

- **23b-8 (keuze gebruiker):** "De oases krijgen meer gras eromheen en de waterpoelen worden een klein beetje groter in de breedte op sommige stukken." Geen ~12 % water: de waterfractie blijft ongeveer zoals nu (licht omhoog, **~6–8 %**), poelen worden **plaatselijk** breder (niet overal), en de grasoever/groene zone wordt duidelijk breder. Uitgewerkt in stap 7 en de tests.
- **23b-9 C (keuze gebruiker):** budget noise-stap ring 2 **≤ 1,75× vanilla**; ring 1 en 3 ongewijzigd. Uitgewerkt in "Prestatiebudget" (drempels en knoppen) en de tests.
- **23b-13 B (keuze gebruiker):** meer crème banden (meer contrast, meer richels). Nieuw lagenpatroon in stap 5b.
- **23b-17 (keuze gebruiker):** teststop 2b pas na change 30, "als alles is ingericht". Geen blokkerende stop na 23b/23c of vóór 25. De W-lijst hieronder gaat naar de gebundelde checklist in plan 30.
- **Gevolg voor change 25:** het zonnesteenbereik wordt opgerekt tot ~Y 180, zodat ook de tafelbergwanden zonnesteen hebben (doorgerekend in plan 25).
- De gevolgen voor latere plannen zijn op 2026-10-01 verwerkt in de plannen 25–28, 30–32 en 37 (sectie "Afstemming na teststop 2 (2026-10-01)" daar).

---

## Splitsing 23b / 23c (beoordeling)

**Ja, splitsen.** 23b = terreinvormen (density functions, noise, surface rules). 23c = aankleding (features en placement modifiers). Redenen:
1. **Andere techniek en andere valkuilen.** 23b gaat over de noise-stap: interpolatie, ring gate, kosten per blok, het preliminary surface en aquifers. 23c gaat over de features-stap: generatievolgorde, schrijven binnen de chunk, de feature-volgorde ("Feature order cycle") en de heightmaps.
2. **23c bouwt op 23b.** Puinhellingen, wandvoeten, nissen, kolken en de oasezones bestaan pas na 23b. Placements in 23c lezen alleen de terrein-heightmap (`WORLD_SURFACE_WG`) en ruis.
3. **Zuiver meten.** Het prestatiebudget van 23b gaat over de noise-stap en is alleen meetbaar zonder featurewijzigingen. Het determinisme van terrein (pure functies) en dat van features (laadvolgorde) toets je apart.
4. **Changes 25, 26, 30 en 31 hangen alleen van 23b af**, niet van 23c. Zo blijft de impact op latere plannen beperkt tot één change.
5. **Kleinere reviews.** 23b is al groot (drie Java-types plus JSON).

Volgorde (vraag 23b-17, keuze gebruiker): 23b → 23c → 25 → 26 → 27 → 28 → 29 → 30 → **teststop 2b** (blokkerend, gebundelde terreincheck ring 2) → 31.

---

## Afhankelijkheden en aannames

Gecontroleerd in de code (stand na change 23, commit `865dd89`; change 24 is inmiddels gecommit, `3a9b560`, en raakt geen worldgen). **Bij de start opnieuw nalopen** (vermijd conflicten in `CODEBASE.md`; 23b raakt de lang-providers niet).

- **Ring 2-terrein** (`density_function/ring_2/overworld/`):
  - `height` = `flat_cache`(vlakte + `relief_weight` · (max(66, plateau + `canyon_depth` · (kloofbodem − plateau)) − vlakte)). De kloofbodem is `69 + 2 · hills(xz 1,7)`, het plateau `plateau_height` (132 ± 7).
  - `terrain` = `height/8 + y-gradiënt (−0,125 per blok) + wall_mask · 0,25 · ring_2_wall(3D)`.
  - `initial_height`/`initial_terrain`: het preliminary surface ligt in het rotsgebied op de **kloofbodemhoogte** en in de oase op de vlakte, met poelen 2× zo diep.
  - `canyon_signal` = min(|kloof|, |zijtak| + 0,15 · |kloof|); `canyon_depth` = een getrapte spline op dat signaal (bodem ≤ 0,022, 4 wanden en 3 treden tot 0,091).
  - `wall_mask` = de kloofwanden en de steilrand.
  - De poel-spline staat in drievoud: `flats_height`, en twee keer in `initial_height`.
- **`ring_2/overworld.json`** = vanilla's `final_density` (inline) met `terrain` op de plek van `sloped_cheese`: `min(squeeze(0,64 · interpolated(blend_density(…))), noodle)`. Eén `interpolated`-marker om de hele stapel. `Ring2TerrainJsonTest` controleert dat dit ring 1 is met alleen `terrain` vervangen.
- **`RingBlend`** rekent alleen de ringen met gewicht. Per-blok-termen buiten een marker kosten dus alleen in ring 2-kolommen.
- **`RingGate`** wikkelt elke `interpolated`-marker van de ring 2-invoer, ook nieuwe.
- **Noise-parameters:** `ring_2_relief`/`moisture` (−10), `canyon` (−9), `canyon_branch` (−8), `hills` (−7), `pools` (−6), `wall` (−4, 3D).
- **Surface rules:**
  - `canyonlands`-tak: vloer rood zand; kloofbodem (`not y_above 78`) rood zand + gebarsten klei + grind; op een plafond en onder de vloer rode zandsteen; steil → de lagen (kopie).
  - `oasis_flats`-tak: steil/plafond zandsteen, onder water zand, boven water en onder Y 66 gras, anders zand.
  - Top-level lagenblok: Y 8 → 4 perioden van 48.
- **Gemeten (change 23):**
  - noise-stap ring 2 = 1,36× vanilla. Het meetpunt x 2.500 ligt grotendeels in `oasis_flats` (zie de checklist van teststop 2), dus pure kloofchunks zijn niet apart gemeten;
  - heightmap voor features: `WORLD_SURFACE_WG`;
  - geen water aan het oppervlak in canyonlands.
- **Plannen die op dit terrein bouwen** (nog niet gebouwd, afgestemd 2026-10-01): 25 (zonnesteen nu Y 95–180), 26 (zandgrot-tak `not above_preliminary_surface`, floodedness), 30 (hoodoos als `rock_pillars`-instantie, `height.json`-regio's), 31 (beddingen op `height`, `pools.json` × (1 − masker)). Zie "Gevolgen voor latere plannen".

---

## Achtergrond (uitgezocht; bij de bouw verifiëren in de 1.21.1-bronnen)

- **Interpolatie (cellen 4 × 8 × 4).** Alles binnen een `interpolated`-marker wordt op celhoeken berekend. Vormen smaller dan ~4–6 blokken of lager dan ~8 blokken verdwijnen of worden vlekken. Dat geldt voor spleten van 3–6 breed, richels van 1–3, bruggen van 3–6 dik, naalden en vinnen. Zulke vormen moeten daarom **per blok, buiten de interpolatie** in de dichtheid (zoals change 30 al vaststelde voor de hoodoos). Brede vormen (tafelbergen, kloofprofiel, puinhellingen, poelen) blijven 2D in `height` (via `flat_cache`, 25 waarden per chunk, vrijwel gratis).
- **`cache_2d` helpt buiten de interpolatie niet** (verwachting, verifiëren). De noise-stap loopt per cel over y → x → z. Daardoor wisselt de kolom bij elk blok, en `NoiseChunk.Cache2D` onthoudt maar één positie. Een 2D-term in een per-blok-functie wordt dus per blok opnieuw berekend. De Java-types van deze change houden daarom zelf een **kolomtabel** bij: per thread, 16 × 16 per chunk, lui gevuld, alleen een cache (zoals `RingColumnCache`). Dat geeft één berekening per kolom in plaats van per blok. Dit is ook een **gevolg voor change 30** (die rekende op `cache_2d`).
- **`flat_cache` is blokkerig** (één waarde per 4 × 4 blokken). Voor `floor` (voet van een vorm) is dat goed genoeg: de voet steekt een paar blokken in de grond. Voor het **spleetsignaal** niet: dat moet exact per kolom.
- **Seeded hash zonder wereldseed:** een `NoiseHolder` (eigen noise-key, geseed via `visitNoise` in `mapAll`) wordt gesampled op een celmidden, en de bits gaan door een mix-functie. Zo krijg je een pure functie van (seed, cel), zoals plan 30 al beschreef.
- **2D-invoer op een andere positie uitrekenen:** een Java-type mag een 2D-invoer (ruis, `flat_cache`, `ring_blend`) op een celmidden of voetpunt uitrekenen met een eigen `SinglePointContext`. `NoiseChunk`-markers vallen buiten hun bereik terug op directe berekening. Een `interpolated`-marker mag zo **niet** gebruikt worden (die is alleen geldig op het eigen blok).
- **Menging in overgangszones:** ring 2 mengt met smoothstep met ring 1/3. Fijne vormen met een vaste dichtheid ±1 worden dan van boven af gekapt (change 30). Richels en nissen hangen aan het terrein. Om zwevende brokken uit te sluiten, krijgen **alle fijne vormen een kernmasker**: een `ring_blend` met constante invoer (ring 2 = 1, de rest 0), dus 1 in ring 2 buiten de zones. Vormen worden kleiner met het masker en vervallen onder een drempel. Tafelbergen (2D) worden met hetzelfde masker afgezwakt.
- **Preliminary surface:** in het rotsgebied ligt het op de kloofbodem. Alles wat hoger ligt (spleetbodems, nissen, richels, tafelbergen, hogere kloofbodemtreden) ligt dus "boven het preliminary surface":
  - de oppervlakteregels van `canyonlands` gelden daar;
  - de zandgrot-tak van change 26 (`not above_preliminary_surface`) raakt ze nooit.
  
  Nieuwe open ruimte mag nergens **onder** het preliminary surface en onder Y 63 liggen, anders zet de aquifer er water (valkuil change 23). De klem blijft: kloofbodems en kolken ≥ Y 66.
- **Surface rules zien het echte blok:** `SurfaceSystem` loopt over het resultaat van de noise-stap. Wanden van spleten, naalden en vinnen zijn dus `steep` (lagen), bovenkanten zijn vloer (rood zand), onderkanten van richels en bruggen zijn plafond. Dunne vloeren die ook plafond zijn, krijgen al rode zandsteen in plaats van rood zand: geen hangend zand.

---

## Plan

Volgorde = bouwvolgorde. Waarden zijn startwaarden voor de aanbevolen antwoorden, af te stemmen met devkaarten (tijdelijk debugcommando zoals change 23, niet gecommit) en in teststop 2b. Bij een ander antwoord verandert alleen de stap van die vraag.

### 0. Voorbereiding (geen gedragswijziging)

- **Verificatie in de bronnen:** de lusvolgorde van de noise-stap, `Cache2D` en `CacheAllInCell` (zie Achtergrond), en `noise_threshold` in surface rules (sampelt die op (x, 0, z), gelijk aan een `noise`-DF met `xz_scale` 1?). Uitkomst als valkuil in `CODEBASE.md`.
- **Ontdubbelen:** de poel-spline uit `flats_height`/`initial_height` naar een eigen `pools.json` (diepte in blokken). Resultaat blok voor blok gelijk (S-vergelijking vóór/na).
- **De stapel losmaken:** de vanilla-stapel van `ring_2/overworld.json` gaat naar `ring_2/overworld/stack.json`. `ring_2/overworld.json` wordt een verwijzing en later de omhulling (stap 6). `Ring2TerrainJsonTest` vergelijkt dan `stack.json` met ring 1.
- **Kernmasker** `ring_2/overworld/core_mask.json` = `ring_blend` met constanten (ring 2 = 1, ring 1, 3–5 en void = 0). Hetzelfde patroon als het zonemasker van plan 31.
- **Basismeting:** JFR (methode change 23) op twee plekken: **kloofgebied** (een 16×16-blok volledig in `canyonlands`, via `locate`) en het meetpunt van change 23 (x 2.500). Plus de vanilla-preset. Zo is "vóór" bekend per plek.

### 1. Tafelbergen (vraag 23b-3 A) — 2D, JSON

- Nieuwe noise `ring_2_mesa` (`firstOctave` −8: vlekken van ~150–300 blokken).
- `mesa_height.json` = getrapte spline op `ring_2_mesa`: niveaus +0 / +12 / +24 / +36. De overgang tussen twee niveaus is een steile wand van 10–16 blokken, met vlakke treden van ≥ 8 blokken breed in het signaal (valkuil interpolatie). Startwaarde: ~30 % van het plateau boven +0.
  - × `mesa_mask` = `relief_weight` ≥ 0,99 (niet op de steilrand) × een afstandsmasker tot de kloven (`canyon_signal` boven ~0,14: ~20 blokken van de kloofrand). Zo blijven de kloofwanden 55–70 hoog en liggen de tafelbergen achter de rand.
  - × `core_mask` (geen tafelbergen in de zones).
- `plateau_surface.json` = `plateau_height + mesa_height`. `height` gebruikt `plateau_surface` in plaats van `plateau_height`.
- `initial_height` verandert niet: het preliminary surface blijft de kloofbodem.
- Lagenblok: 4 perioden vanaf Y 8 dekken tot Y 200, genoeg voor toppen tot ~Y 178.

### 2. Kloofbodem, droge watervallen en puinhellingen (vraag 23b-7 A) — 2D, JSON

- **`canyon_floor.json`** (nieuw; was inline in `height`): `69 + 2 · hills(xz 1,7)` + treden.
  - Treden = getrapte spline op een nieuwe noise `ring_2_canyon_floor` (`firstOctave` −8). Niveaus 0 / +6 / +12 / +18, een steile stap van ~1 blok signaal: een valwand dwars door de kloof, om de ~150–300 blokken.
  - Net aan de lage kant van elke stap een **droge kolk**: −3, nooit onder Y 66.
- **Hangende zijtakken:** `canyon_depth` = max(hoofdprofiel(|kloof|), 0,7 · zijtakprofiel(|zijtak| + 0,15 · |kloof|)). De bodem van een zijtak ligt dan op ~30 % van de wandhoogte (~+18 bij het plateau). Waar hij in de hoofdkloof uitkomt, staat een valwand.
- **Puinhellingen:** nieuwe noise `ring_2_talus` (`firstOctave` −7) → `talus_mask` (0–1, ~15 % van de wandlengte boven een drempel).
  - `canyon_depth` = mix(getrapt profiel, lineaire helling, `talus_mask`). De helling loopt ~1:1 en is bovenaan breder (signaal tot ~0,17).
  - `wall_mask` × (1 − `talus_mask`): geen ruwe overhangen op de helling, zodat hij beloopbaar blijft.
- `height` = vlakte + `relief_weight` · (max(66, `plateau_surface` + `canyon_depth` · (`canyon_floor` − `plateau_surface`)) − vlakte).
- **`initial_height`:** het rotsgebied blijft op de **laagste** kloofbodem (`69 + 2 · hills`, zonder treden). Hogere treden en kolken vanaf 66 liggen boven of vlak onder het preliminary surface. De aquifer en de regels doen dan hetzelfde als nu (S: geen water).
- **Surface rules `canyonlands`:**
  - de kloofbodemregel `not y_above 78` schuift mee met de hoogste trede (`not y_above 96`);
  - puinhellingen: `noise_threshold` op `ring_2_talus` → vloer **grind met rood zand** (surface-ruis), zichtbaar puin.
  
  Losse keien op de hellingen komen uit 23c.

### 3. Spleetkloven (vraag 23b-1 A, 23b-2 A) — Java `cell_edges` + per blok in `canyon_details`

- **Nieuw: `worldgen/densityfunction/CellEdges.java`**, type `corruptedadventure:cell_edges` (2D, puur). Een breuknetwerk:
  - een gejitterd raster (`cell_size` ~110), met domain warp via een `NoiseHolder` (`ring_2_slot_warp`, amplitude ~12) zodat de breuken meanderen;
  - uitvoer = afstand in blokken tot de dichtstbijzijnde **open** rand tussen twee celpunten (exacte afstand tot de middelloodlijn);
  - per rand een hash (`ring_2_slot_cells`): open met kans `open_chance` (0,6), anders dicht. Dichte randen zijn landverbindingen tussen de plateau-eilanden. Per rand ook een getrokken breedte (3–6) en diepte (25–45), plus brugposities;
  - codec: `cell_size`, `jitter`, `open_chance`, `warp`/`warp_amplitude`, `hash`. Validatie: `cell_size` ≥ 32, kansen binnen [0, 1].
- **Uithakken** in `canyon_details` (stap 5), per blok buiten de interpolatie:
  - de kolomtabel bewaart de afstand, de randgegevens en de plateauhoogte;
  - leeg als `afstand < breedte(y)/2` en `bodem < y`. Daarbij is `breedte(y)` = de randbreedte + 1–2 onderin (ondergraven wanden) + een kleine golving uit een eigen `interpolated` 3D-ruis (`ring_2_slot_wobble`, goedkoop via de interpolator);
  - de bodem = `plateau_height` (zonder tafelberg) − de randdiepte, nooit onder max(Y 70, `canyon_floor` + 4). Een spleet hakt dus nooit in een kloofbodem of in de oase.
- **Bruggen en klemblokken:** langs elke open rand, op hash-posities om de ~40–100 blokken, een stuk van 3–6 lang waar de bovenste 3–6 blokken blijven staan (overdekt stuk). Soms een klemblok halverwege de diepte.
- **Waar:** gewicht = `relief_weight` ≥ 0,99 × `core_mask` × (niet in een hoofdkloof: daar is het terrein al open). Bij een hoofdkloof eindigt de spleet als een **hangende monding** in de wand: een droge waterval.
- Spleten snijden ook door tafelbergen (meer diepte, dramatisch). De bodem volgt `plateau_height` − de diepte, niet de tafelberg.

### 4. Rotsvormen: generiek type `rock_pillars` (vraag 23b-4 A, 23b-5 A) — Java, gedeeld met change 30

- **Nieuw: `worldgen/densityfunction/RockPillars.java`**, type `corruptedadventure:rock_pillars`. Het is één type voor naalden, keien, vinnen en bogen. Change 30 maakt er een JSON-instantie `hoodoos.json` van, zonder nieuwe Java (zie Gevolgen).
- **Codec:**
  - `hash` (`NoiseHolder`), `cell_size`;
  - `weight` (2D-DF, 0–1, op het celmidden);
  - `floor` (2D-DF: `ring_2/overworld/height`);
  - `exclude` (optionele 2D-DF + `exclude_below`: een cel vervalt als zijn voetafdruk dichter dan N blokken bij een spleet komt; hier `slot_network`);
  - `max_floor_step` (een cel vervalt als de vloer binnen de voetafdruk meer dan N blokken verschilt: geen vormen op wanden of treden);
  - `chance`;
  - `shapes` = een gewogen lijst vormen:
    - `needle`: hoogte, voetstraal, topstraal, uitlopende voet, en optioneel `cap` {kans, extra straal, dikte}. In 23b is de kans 0; change 30 zet hem aan voor kaprotsen;
    - `boulder`: straal, afplatting, hoe ver ingegraven;
    - `fin`: lengte, dikte, hoogte, richting (`random` of `aligned`: richting uit een grove ruis, zodat vinnen per gebied evenwijdig staan, plus spreiding), en optioneel `hole` {kans, breedte- en hoogteverhouding} = **boog**.
- **Vormregels** (unit tests):
  - één vorm per cel, helemaal binnen de cel minus 1 blok marge: alleen de eigen cel telt, geen 3×3-zoektocht;
  - de dichtheid is +1 binnen de vorm, anders −1;
  - elke kolom van de vorm loopt door tot het laagste vloerpunt van de voetafdruk − 2. Bij een boog lopen de poten zo door; het gat zit alleen tussen de poten, boven de vloer;
  - `weight` schaalt de hoogte (en de straal); onder een minimum geen vorm;
  - **nooit zwevend**: flood fill naar de voet.
- **Instanties (JSON):**
  - `ring_2/overworld/plateau_rocks.json`: cel 24, gewicht = plateaumasker (`relief_weight` ≥ 0,99 en `canyon_depth` = 0) × `core_mask`. Vormen:
    - naalden 8–20 hoog, straal 1,5–3 (gewicht ~35);
    - keien met straal 2–4, half begraven (gewicht ~35);
    - vinnen 12–30 lang, 2–4 dik, 6–18 hoog, 20 % met een gat (gewicht ~30).
    
    Kans per cel ~0,35, dus ~1 vorm per 2 chunks.
  - `ring_2/overworld/canyon_spires.json`: cel 96, kans ~0,1. Alleen waar de kloofbodem breed is (`canyon_depth` = 1 over de hele voetafdruk). Torens 25–50 hoog, straal 3–5, met een uitlopende voet.
- **Kosten:** vroege exit op y (buiten [voet, top] van de eigen cel → −1). De celgegevens en de verticale grenzen per kolom staan in de kolomtabel.
- **Volgorde in de dichtheid:** `max(stapel, plateau_rocks, canyon_spires)`, dus buiten `interpolated`. Rotsvormen vullen eventuele grotjes in hun voetafdruk. Niet in `initial_density`: het preliminary surface blijft de kloofbodem.

### 5. Wanden: richels, nissen en overhangende rand (vraag 23b-6 A) — Java `canyon_details`

- **Nieuw: `worldgen/densityfunction/CanyonDetails.java`**, type `corruptedadventure:canyon_details`. Het is een omhulling: `argument` gaat erin en `min(max(argument, toevoegingen), uithakkingen)` komt eruit. Eén type, omdat alle wanddetails dezelfde kolomtabel en dezelfde vroege exit delen.
- **Invoer:**
  - `argument`;
  - `shell` = `interpolated(ring_2/overworld/terrain)`: een eigen marker, dus een extra interpolator (alleen celhoeken), die de ring gate vanzelf wikkelt;
  - `slot_network`;
  - `plateau_surface`, `canyon_floor`;
  - `wall_weight` (2D: `wall_mask` × (1 − `talus_mask`) × `core_mask`);
  - `bands` (een vaste lijst y-banden);
  - `hash`;
  - de spleetparameters (stap 3).
- **Steilte:** de kolomtabel rekent per kolom de helling van `height` (verschil met de buurkolommen). Richels en nissen komen alleen op steile wanden (helling ≥ ~2), niet op treden of puinhellingen.
- **Richels:** in vaste y-banden, gelijkgelegd met de **crème banden** van de lagen (de "harde laag"). Per periode van 48 blokken: dezelfde absolute Y als het lagenpatroon (na 23b-13 B: **4 crème banden per periode**, zie stap 5b), met de golving van de surface-diepte als kleine afwijking.
  - Vast als `−δ < shell < 0`: alleen buiten het oorspronkelijke oppervlak, dus grotten worden niet dichtgezet.
  - δ wordt uit de helling berekend, zodat de richel 1–3 blokken uitsteekt.
  - Onderbroken via een hash per wandstuk (~50 % van de wandlengte).
- **Nissen (wandgrotten):** in de band **onder** een richel (de zachte laag). 6–16 breed, 4–7 hoog, 3–8 diep, met een vlakke vloer (de bovenkant van de onderliggende laag).
  - Leeg als `0 < shell < ε`, met ε uit de helling zodat de diepte in blokken klopt.
  - Per wandstuk een hash: ~1–2 nissen per 100 blokken wand.
  - De nisvloer ligt boven het preliminary surface en dus ≥ Y 66.
- **Overhangende rand (kaprots):** in de bovenste 3–5 blokken onder `plateau_surface` steekt de wand 2–3 blokken uit (zoals een richel). Daaronder een smalle ondergraven band. Aan beide kloofwanden, op het hele plateau.
- **Vroege exit:** buiten [laagste kloofbodem van de kolom, `plateau_surface` + 2] en zonder spleet in de kolom → `argument` ongewijzigd.

### 5b. Kloofsteenlagen: meer crème banden (vraag 23b-13 B, keuze gebruiker) — surface rules

- **Nu** (change 23, periode 48 vanaf Y 8): rood 28 / oranje 12 / crème 8, met 3 crème banden van 2, 3 en 3 blokken (Y 22–23, 40–42, 53–55 in de eerste periode).
- **Nieuw** (startwaarde): **rood 22 / oranje 12 / crème 14** per 48, met **4 crème banden van 3–4 blokken**. Patroon per periode (offset vanaf Y 8 + k · 48):

  | Offset | 0–4 | 5–7 | 8–11 | 12–14 | 15–18 | 19–22 | 23–25 | 26–30 | 31–33 | 34–39 | 40–43 | 44–47 |
  |---|---|---|---|---|---|---|---|---|---|---|---|---|
  | Tint | rood 5 | **crème 3** | rood 4 | oranje 3 | rood 4 | **crème 4** | rood 3 | oranje 5 | **crème 3** | rood 6 | oranje 4 | **crème 4** |

  Crème ligt zo steeds aan minstens één kant tegen rood (maximaal contrast) en de banden staan ~9–14 blokken uit elkaar. Rood blijft de grootste tint.
- **Waar:** het top-level lagenblok én de lagenkopie in de `canyonlands`-tak (en de oasekant, stap 7) krijgen hetzelfde patroon; 4 perioden vanaf Y 8 dekken tot Y 200. De golving (`surface_depth_multiplier` 1) blijft.
- **Effect op de richels (stap 5):** crème banden per periode 3 → 4 (+33 %). Een kloofwand van ~60 blokken (Y ~70–130) heeft ~5 crème banden in plaats van ~4, dus ~1 extra richelniveau per wand (onderbroken, ~50 % van de wandlengte per band). Dikkere banden (3–4 i.p.v. 2–3) houden een richel ook bij de golving van ±1–2 blokken op zijn band (verkleint het risico "richel naast zijn band"). **Nissen** blijven ~1–2 per 100 blokken wand (de hash per wandstuk kiest één van de banden); ze worden dus niet talrijker, wel gevarieerder in hoogte.
- **Effect elders:** meer crème volume → bij de ertsmeting van 25 meer `cream_canyon_*_ore` (de meting normaliseert per gastgesteente: geen doelwijziging); de hoodoos (30) krijgen hetzelfde patroon; het contrast van de zonnesteen-overlay op crème wordt belangrijker (37).
- Kosten: geen (surface rules; `canyon_details` leest een vaste bandenlijst).

### 6. Samenstelling `ring_2/overworld.json`

```
canyon_details(
  argument = max(ring_2/overworld/stack, ring_2/overworld/plateau_rocks, ring_2/overworld/canyon_spires),
  …)
```
- Bruggen en richels komen ná de rotsvormen en spleten ná de toevoegingen. Een rotsvorm valt nooit in een spleet, want cellen met een spleet vervallen via `exclude`.
- `ring_blend/overworld_initial` verandert niet (verwijst naar `initial_terrain`).
- **Kolomtabel:** nieuw `worldgen/densityfunction/ColumnTable.java` (package-private helper). Per instantie een `ThreadLocal`, 16 × 16 per chunk plus een rand, gesleuteld op de chunkpositie, lui gevuld. Het is alleen een cache, zonder gedeelde staat; de uitkomst is een pure functie. Gebruikt door `RockPillars` en `CanyonDetails`. `CellEdges` zelf is puur en zonder cache: de gebruikers cachen hem.

### 7. Oaseterrein en oasezones (vraag 23b-8, keuze gebruiker; 23b-11 A) — JSON + surface rules

Uitgangspunt (keuze gebruiker): **meer gras rond de oase**, poelen **plaatselijk iets breder**, waterfractie ongeveer zoals nu (~5 % → **~6–8 %**). Geen grote meren.

- **Poelen plaatselijk breder, via één ruis:**
  - `ring_2_pools` krijgt **één extra, grovere octaaf** (`firstOctave` één lager, amplitude van die octaaf ~0,3–0,5 t.o.v. de bestaande; startwaarde 0,4). Waar die grove octaaf hoog staat, wordt een poel breder (een uitstulping of een wat grotere poel); waar hij laag staat, blijft de poel zoals nu of wordt hij iets kleiner. Zo worden poelen "op sommige stukken" breder, niet overal.
  - **Waarom geen tweede ruis** (bijv. een apart verbredingsmasker): de oasezones in de surface rules (`noise_threshold`) en de features van 23c (`noise_band`) kunnen maar één ruis lezen. Met één ruis blijven water, oever en overgangsring exact concentrisch, ook rond een verbreed stuk.
  - De drempel in `pools.json` wordt opnieuw gekozen (de normalisatie van `NormalNoise` verandert met een extra octaaf) zodat **6–8 %** van `oasis_flats` water is. Startwaarden met devkaarten afstemmen.
  - Poelen houden grotendeels hun huidige maat (bij de bouw meten en noteren); op de verbrede stukken worden ze ~1,3–1,5× zo breed (startwaarde). Diepte zoals nu (bodem ≥ Y 59, water tot 63).
  - `initial_height` gebruikt dezelfde `pools.json` (2× diep, zoals nu: de aquifer vult zeker).
- **Bredere groene zone (terrein):** de oeverhelling rond de waterlijn wordt over een bredere band flauw (bijna vlak op Y 64–66), zodat de grasoever niet op een talud ligt maar een vlakke groene rand vormt.
- **Zones in de surface rules** (`oasis_flats`-tak), op `noise_threshold` van **dezelfde** noise `ring_2_pools`. Dat zijn concentrische ringen rond elke poel, onafhankelijk van de y-drempel:
  - water → zand onder water (zoals nu);
  - **oever** (poelruis net onder de waterdrempel) → gras, **6–14 blokken breed** (duidelijk breder dan nu; vervangt "boven water en onder Y 66"; de huidige breedte bij de bouw meten en noteren);
  - **overgangsring** (~8–16 blokken breder) → aan de binnenkant vooral gras met `coarse_dirt`-vlekken, naar buiten toe `coarse_dirt` en zand met spaarzaam gras, via de surface-ruis (`minecraft:surface`). Hier komt in 23c de droge begroeiing;
  - daarbuiten → zand (woestijn).
- **Oasekant van de steilrand** (routinevraag 23b-11 A): in de `oasis_flats`-tak worden steile vlakken boven Y ~74 en hun ondergrond kloofsteenlagen (dezelfde lagenkopie als in `canyonlands`), in plaats van zandsteen op grijze steen. De wand leest dan als één geheel met het plateau; de biomegrens blijft waar hij is.

### 8. Documentatie

- **`CODEBASE.md`:**
  - classes `CellEdges`, `RockPillars` (+ vormrecords), `CanyonDetails`, `ColumnTable`;
  - de registries (3 DF-types);
  - databestanden: nieuw `stack`, `core_mask`, `mesa_height`, `plateau_surface`, `canyon_floor`, `talus_mask`, `pools`, `slot_network`, `plateau_rocks`, `canyon_spires` en `shell`; gewijzigd `height`, `canyon_depth`, `wall_mask`, `flats_height` en `initial_height`; de noises `ring_2_mesa`, `_canyon_floor`, `_talus`, `_slot_warp`, `_slot_cells`, `_slot_wobble`, `_rock_cells`, `_spire_cells`, `_wall_bands`; `ring_2_pools` gewijzigd (extra grove octaaf);
  - surface rules (kloofbodemdrempel, puinhellingen, oasezones, lagen aan de oasekant, nieuw lagenpatroon met 4 crème banden);
  - **valkuilen:** `cache_2d` buiten de interpolatie; per blok vs. geïnterpoleerd; het kernmasker voor fijne vormen; 2D-invoer op een andere positie uitrekenen (geen `interpolated`); de shell-marker; de meetplek van change 23 is oase;
  - de meetresultaten en de kopregel.
- **`fase-1-wereld.md` §2 Ring 2:** **Uitwerking terreinvormen (change 23b)** met alle maten en het gedeelde type `rock_pillars` (voor 30); de lagenverhouding (nu "rood 28, oranje 12, crème 8") wordt "rood 22, oranje 12, crème 14 in 4 crème banden" (23b-13 B).
- **`02-beslissingenlog.md`:** de regels hieronder.
- **`docs/backlog.md`:** de backlogkandidaten staan er al (toegevoegd 2026-10-01); bij de bouw alleen nieuwe vondsten toevoegen.
- **Plannen 25–28, 30–32 en 37:** al afgestemd op 2026-10-01 (sectie "Afstemming na teststop 2" in elk plan). Wijkt de bouw af van dit plan (namen, maten, gemeten budget), dan de betreffende plannen bijwerken en het melden.

---

## Prestatiebudget (vraag 23b-9 = C, keuze gebruiker)

- **Budget (C):**
  - **noise-stap ≤ 1,75× vanilla** in het kloofgebied én op het meetpunt van change 23 (x 2.500);
  - ring 1 en ring 3 ongewijzigd (binnen de meetruis van ±3 %);
  - de hele chunk (alle stappen) in ring 2: meten en rapporteren; verwachting ≤ vanilla (nu ~80 vs ~100 ms, met +4–8,5 ms noise ~85–90 ms).
- **Gedeeld budget:** 1,75× is het budget voor de **hele** ring 2-noise-stap, ook na 26 (floodedness), 30 (hoodoos, regio's) en 31 (beddingen). **Richtwaarde voor 23b zelf: ≤ ~1,6×**, zodat er ruimte blijft voor 26/30/31. Tussen 1,6× en 1,75×: toegestaan, maar eerst knop 1 en 2 (onzichtbaar) proberen en de uitkomst als aandachtspunt voor 30 in `CODEBASE.md` zetten.
- **Meetmethode** (zoals change 23, `CODEBASE.md`):
  - JFR `minecraft.ChunkGeneration`, stap `noise`, 16×16 chunks, de tweede (warme) ronde telt, gemiddelde van twee schone rondes per variant (geen client tegelijk);
  - plekken: kloofgebied (via `locate`, vast vastgelegd in `CODEBASE.md`), x 2.500 (oase-gemengd), x 750 (ring 1) en x 4.500 (ring 3), plus de vanilla-preset;
  - **uitsplitsing** per onderdeel: een tijdelijke JSON-variant waarin telkens één term uit staat (`plateau_rocks`, `canyon_spires`, `canyon_details` (spleten / richels+nissen), tafelbergen). Alleen in `build/c23b/`, niet gecommit.
- **Schatting:**

  | Onderdeel | Extra kosten noise-stap per kloofchunk |
  |---|---|
  | Tafelbergen (meer ondergrond met grotten en aquifer) | +1,5–3 ms |
  | `canyon_details` (kolomtabel + per blok in het y-venster, extra `shell`-interpolator) | +2–4 ms |
  | Twee `rock_pillars`-instanties (vroege exits) | +0,5–1,5 ms |
  | Totaal | **+4–8,5 ms** |

  Dat geeft ~1,45–1,6× vanilla in puur kloofgebied, en ~1,4× op het gemengde meetpunt: binnen het budget van 1,75× en rond de richtwaarde van 1,6×. Oasechunks blijven gelijk, want alle gewichten zijn er 0 (de extra octaaf van `ring_2_pools` zit in `flat_cache`: verwaarloosbaar).
- **Knoppen** (in deze volgorde):
  1. y-vensters krapper (onzichtbaar);
  2. `shell` alleen in kolommen met `wall_weight` > 0 (gate; onzichtbaar);
  3. grotere celmaten of kleinere kansen;
  4. de wiebel van de spleetwand uit;
  5. de hoogste tafelbergtrede weg.
- **Stop-en-vraag-drempels:**
  - noise-stap **> 1,75×** na knop 1 en 2 → knop 3–5 alleen zolang de vorm niet zichtbaar verandert; moet er een element **zichtbaar** weg of kleiner: **stop-en-vraag**;
  - **> 2,0×** in de eerste meting (vóór knoppen) → stop-en-vraag (de schatting klopt dan niet; eerst samen kijken welk onderdeel het is, met de uitsplitsing);
  - ring 1 of ring 3 meer dan de meetruis (±3 %) trager → stop-en-vraag (de ring gate of het kernmasker lekt);
  - de hele chunk in ring 2 trager dan vanilla → melden in het rapport en stop-en-vraag (bij C geaccepteerd: merkbaar trager laden bij snel verkennen, maar niet trager dan vanilla zonder overleg).

---

## Client/server-impact

- Puur worldgen, server-side en deterministisch per seed:
  - alle nieuwe types zijn pure functies van (seed, x, y, z): hashes via geseede `NoiseHolder`s en ringmaskers via `RingLayout` (wereldseed, `RingColumnCache`);
  - kolomtabellen zijn `ThreadLocal`-caches zonder invloed op de uitkomst;
  - geen netwerk, geen SavedData of attachments, geen client-code.
- De density-function-types `cell_edges`, `rock_pillars` en `canyon_details` zijn common worldgen-registries; ze worden niet gesynct. Een dedicated server laadt geen client-class.
- **Bestaande werelden:** het ring 2-terrein verandert overal (tafelbergen, kloofbodem, poelen). Op de rand tussen oude en nieuwe chunks in ring 2 ontstaan naden. Ring 1 verandert niet (router en ring 1-invoer onaangeroerd). Testwerelden opnieuw aanmaken.

---

## Gevolgen voor latere plannen (verwerkt op 2026-10-01 in plannen 25–28, 30–32 en 37)

| Plan | Wat verandert |
|---|---|
| **25 ertsen** | Zonnesteen `trapezoid` Y 95–150 (vol 110–140) dekt de kloofwanden, spleetwanden (Y ~90–140), richels en nissen. De **tafelbergwanden (Y ~132–178) liggen er voor de bovenste helft buiten**. **Besloten 2026-10-01: het bereik oprekken tot ~Y 180** (`trapezoid` Y 95–180, `plateau` 60, vol ~108–168, 14 aders; doorgerekend in plan 25). Meer kloofsteenvolume per chunk (tafelbergen) en meer crème (23b-13 B) = de meting per gastgesteente (doel "als ijzer") opnieuw doen. Turkoois Y 10–40 verandert niet. Testplekken "hoog in de wanden" kunnen nu ook richels/nissen zijn |
| **26 grotten** | De zandgrot-tak (`not above_preliminary_surface`) raakt spleten, nissen en overhangen nooit (ze liggen boven het preliminary surface). Dat klopt met "nooit een overhang aan het oppervlak": niets aan te passen, wel als feit noteren. Onder tafelbergen liggen meer grotlagen (meer plafonds, meer noise-tijd): meetpunt "grootste plafond" en perf in het kloofgebied. Floodedness-band (Y < 60) verandert niet. Vanilla-carvers kunnen bruggen, vinnen en naalden aansnijden (geaccepteerd, zoals bij de hoodoos) |
| **30 extra biomes** | (1) **`hoodoo_pillars` vervalt als eigen Java-type**: hoodoos worden een JSON-instantie `ring_2/overworld/hoodoos.json` van `rock_pillars` (vorm `needle` met `cap`, plus `fin` met `hole` voor de bogen). `HoodooPillars(Test)` wordt een uitbreiding van `RockPillarsTest` met hoodoo-instellingen. Ontbreekt er iets (bijv. een kaprots met een andere vorm), dan breidt 30 `rock_pillars` uit; geen tweede type. (2) **`cache_2d` → kolomtabel** (achtergrond: per blok geen winst). (3) `height.json` is opgesplitst in `plateau_surface`, `canyon_floor` en `canyon_depth` (met hangende zijtakken en puinhellingen). De `rock`-term van 30 gebruikt `plateau_surface − canyon_depth · …` en `hoodoo_weight` moet ook de tafelbergen en spleten uitzetten (`slot_network`-gewicht × (1 − `hoodoo_w`)). (4) Perf-budget naar het budget van 23b-9: **≤ 1,75× vanilla, gedeeld met 23b** (nu 1,3× in 30). (7) Teststop 2b (gebundeld, blokkerend) komt na 30; plan 30 krijgt de gebundelde checklist. (5) De verwijzing in de overgangszones gaat via `core_mask` (fijne vormen vervallen in zones). (6) `ring_2/overworld.json` is nu een omhulling: hoodoos komen in de `max(…)` van stap 6, niet rond de stapel |
| **31 `dry_riverbeds`** | Beddingen komen op het eindresultaat van `height` (met tafelbergen en treden). Een bedding over een tafelberg = een geul in de tafelberg, en "3–6 diep, relatief" blijft kloppen. De **poel-term heet nu `pools.json`** (`× (1 − riverbed_mask)` daar, zodat ook `initial_height` meegaat); `ring_2_pools` heeft een extra grove octaaf (23b-8). De oasezones in de surface rules hangen aan `ring_2_pools`, maar de zone is biome `dry_riverbeds` met een eigen tak, dus geen grasringen rond droge poelen; nalopen. Fijne vormen (spleten, rotsvormen, richels) vervallen in de zone (`core_mask`), dus de zone is vlakker dan het kloofgebied: dat past bij "brede lege beddingen" |
| **27 uitdroging** | Spleten en nissen geven **schaduw** (hemel niet zichtbaar). De regel van 27 werkt daar vanzelf (geen wijziging), maar het speelgevoel verandert (schuilplekken). De wandbron met stroompje van 23c (23c-4 C) is echt water en dus een extra koelplek naast de oase: bij 27 alleen noteren (backlog `[conditions]`) |
| **28 ringweer** | De wind-duw van de zandstorm bij spleten en richels geeft valgevaar. Nalopen of de duw daar beperkt moet worden (gameplay; anders backlog) |
| **32 sfeer** | Optioneel: windgeluid in spleten (backlog-kandidaat, geen wijziging nodig) |
| **37 textures** | `canyon_cobblestone` palet herzien (feedback teststop 2, punt 3). Wordt na 23c veel zichtbaarder (puin, rotsval). Grind op puinhellingen naast de kloofsteentinten in-game beoordelen |
| **Fase 3** | Nissen met een vlakke vloer zijn de plek voor klifwoningen (backlog). Hun ligging is deterministisch per seed, maar een structure moet ze zelf vinden (bijv. via het terrein of door dezelfde `canyon_details`-hash te lezen) |

---

## Tests

Bouwplan: **B, S, W** (+ D als controle: geen datagen-wijziging verwacht). Testseed `20260930`, werelden `smoke-change23b-…`. RCON-werkwijze en devkaarten zoals change 23; daarna RCON weer uit.

### B — `./gradlew build`
- [ ] **`CellEdgesTest`:**
  - de afstand tot een open rand klopt (±0,05) met de exacte middelloodlijn, ook met warp (continu: buurpunten verschillen ≤ 1,05 per blok);
  - aandeel open randen 0,6 ± 0,05 over 5.000 randen;
  - dezelfde seed geeft dezelfde uitvoer, ook vanuit twee threads;
  - codec-rondreis en validatie.
- [ ] **`RockPillarsTest`:**
  - **nooit zwevend** (flood fill naar de voet) voor 2.000 cellen × alle vormen (naald met en zonder kap, kei, vin met en zonder gat), bij `weight` 0,3, 0,6 en 1,0;
  - vorm binnen de cel minus de marge;
  - hoogte binnen het bereik maal `weight`;
  - het gat van een boog is open en de poten raken de vloer;
  - `exclude` en `max_floor_step` laten een cel vervallen;
  - puur (twee threads, nieuwe instantie via `mapAll`);
  - codec-rondreis en validatie (min ≤ max, kansen in [0, 1]);
  - `minValue`/`maxValue`.
- [ ] **`CanyonDetailsTest`** (met een synthetische `shell` en vlakke invoer):
  - buiten het y-venster gelijk aan `argument`;
  - spleetbreedte per y volgens de randgegevens; de bodem nooit onder max(70, kloofbodem + 4);
  - bruggen op de verwachte afstanden;
  - richels alleen waar `shell < 0`, nissen alleen waar `shell > 0` en binnen de diepte;
  - geen richels of nissen bij een flauwe helling;
  - codec.
- [ ] **`ColumnTableTest`:** wisselen van chunk, twee threads, juiste kolom.
- [ ] **`Ring2TerrainJsonTest`** aangepast: `stack.json` = `ring_1/overworld.json` met alleen `terrain`.
- [ ] **`RingBlendTest`/`RingGateTest`** groen; nieuw: de `shell`-marker in de ring 2-invoer wordt precies één keer gegate.

### D — `./gradlew runData`
- [ ] Draait zonder fouten; de gegenereerde output is ongewijzigd (23b heeft geen datagen).

### S — server + RCON (geautomatiseerd)
- [ ] **Rooktest:** `tools/server-smoke-test.sh` start zonder fouten; de codecs van de nieuwe DF-types laden; geen client-class-melding.
- [ ] **Stap 0 neutraal:** na het ontdubbelen van de poelen en het losmaken van de stapel (vóór stap 1) zijn 12 ring 2-chunks blok voor blok gelijk aan change 23.
- [ ] **Ring 1 ongewijzigd:** 300 biomepunten en steen/deepslate in 6 ring 1-chunks identiek aan een wereld van change 23 (lucht/water mag ~1 % afwijken: valkuil feature-index).
- [ ] **Hoogtes** (100 kolommen per biome, `motion_blocking_no_leaves`):
  - canyonlands: plateau Y 123–178; ≥ 15 % van de plateaukolommen ≥ Y 145 (tafelbergen); kloofbodems Y 66–95;
  - oasis_flats: Y 59–92;
  - **0 kolommen met water boven Y 63 in canyonlands.**
- [ ] **Spleten:** devkaart/RCON over 384 × 384 in het kloofgebied:
  - kolommen met een spleet: het oppervlak ligt ≥ 20 lager dan een kolom 4 blokken ernaast;
  - spleetbreedte 3–6 (95 %), spleetbodem ≥ Y 70;
  - ≥ 1 brug per 100 blokken spleet;
  - de plateau-eilanden hangen via dichte randen aan elkaar (flood fill over het plateau zonder spleten: één samenhangend gebied in ≥ 90 % van de vensters).
- [ ] **Rotsvormen:** in 32 kloofchunks het aantal naalden, keien en vinnen (devcommando) binnen ±30 % van de verwachting; bogen > 0 over 256 chunks; torens in brede kloofbodems > 0 over 400 chunks.
- [ ] **Richels en nissen:** in 16 chunks met steile wanden: richels > 0, nissen > 0; nisvloeren ≥ Y 66.
- [ ] **Geen zwevende brokken:** devcommando, flood fill van vaste blokken in 32 kloofchunks + 8 chunks in zone 1→2 + 8 in zone 2→3 (met 1 chunk rand). Geen component zonder verbinding met de ondergrond (Y < 60).
- [ ] **Geen hangend zand/grind:** in dezelfde chunks 0 × `red_sand`/`gravel`/`sand` met lucht er direct onder (aan het oppervlak; grotten onder Y 60 tellen niet).
- [ ] **Oase** (23b-8, keuze gebruiker):
  - water **6–8 %** van de `oasis_flats`-kolommen (tolerantie 5–9 %; r = 2.500-cirkel en 4 vensters), vóór/na vergelijken met de wereld van change 23 (~5 %);
  - verbrede stukken: in de vensters is de grootste poelbreedte ≥ 1,3× de mediaan van change 23 bij ≥ 1 op 4 poelen, maar de mediaan zelf ≤ 1,2× (dus plaatselijk breder, niet overal);
  - grasoever: per poel de afstand van de waterlijn tot het eerste zand-/`coarse_dirt`-blok (8 richtingen) → mediaan 6–14 blokken en ≥ 2× de mediaan van change 23;
  - `coarse_dirt` > 0 in de overgangsring; gras in de binnenste helft van de overgangsring > gras in de buitenste helft.
- [ ] **Lagen (23b-13 B):** in 8 kloofchunks Y 60–145: aandeel crème van de kloofsteen 29 % ± 5 (was ~17 %), rood > crème > 0, oranje 25 % ± 5.
- [ ] **Lagen aan de oasekant:** in 4 steilrandchunks (oasekant) kloofsteen > 0 boven Y 74; geen grijze steen aan steile vlakken boven Y 74.
- [ ] **Determinisme:** twee verse werelden, de tweede in omgekeerde laadvolgorde. Per chunk (16 kloof-, 16 oasechunks) tellingen van steen, de drie tinten, lucht, water, `red_sand`, `gravel`, `coarse_dirt`, `grass_block` en een hoogte-hash → identiek.
- [ ] **Performance** volgens "Prestatiebudget" (budget ≤ 1,75×, richtwaarde 23b ≤ ~1,6×, stop-en-vraag-drempels daar): vóór/na per plek, de uitsplitsing per onderdeel, de hele chunk in ring 2, getallen in `CODEBASE.md`.

### W — **niet blokkerend na deze change; gaat naar teststop 2b** (blokkerend, na change 30, vóór 31; vraag 23b-17, keuze gebruiker)

**Reden:** of het plateau nu moeilijk over te steken is, of het kloofgebied avontuurlijk voelt en of de oases goed zijn, is niet automatisch te meten. De gebruiker wil dat pas beoordelen "als alles is ingericht" (na 25, 26 en 30). Alles wat meetbaar is, staat onder S en is wél verplicht vóór change 23c/25.

**Wat de bouwer van 23b doet:** per punt hieronder een plek zoeken met de devkaarten en de coördinaten (seed `20260930`) onder dit kopje in dit plan zetten, zoals change 23. De bouwer van change 30 neemt deze lijst over in de gebundelde checklist van teststop 2b (plan 30) en controleert of de coördinaten na 25/26/30 nog kloppen.

Lijst voor teststop 2b (runClient, testseed; ~20 min samen met 23c):
- [ ] **Oversteken:** loop in survival ~400 blokken rechtdoor over het plateau. Het moet uitdagend zijn (spleten, tafelbergwanden, vinnen), maar niet onmogelijk (landverbindingen, bruggen, treden).
- [ ] **Spleten:** breedte, diepte, ondergraven wanden; de bruggen/klemblokken lezen als natuurlijk; de hangende monding in de hoofdkloof.
- [ ] **Tafelbergen:** treden, wandhoogte, afstand tot de kloofrand.
- [ ] **Rotsvormen:** naalden, keien, vinnen, bogen, torens in de kloof. Geen zwevende delen, lagen op de wanden.
- [ ] **Wanden:** richels (op de crème banden), nissen (plek voor klifwoningen), overhangende rand; bereikbaarheid.
- [ ] **Kloofbodem:** droge watervallen (treden + kolk), hangende zijtakken, puinhellingen als klimroute.
- [ ] **Oase:** plaatselijk verbrede poelen, duidelijk bredere grasoever, overgangsring, oasekant van de steilrand (23b-8, keuze gebruiker).
- [ ] **Lagen:** meer crème banden (23b-13 B): contrast, richels op de crème banden.
- [ ] De nog open punten van teststop 2 (beantwoord met "laten", nu in-game bevestigen): overgang ring 1 → plateau, kloofbodem, plateaugrotten (vragen 23b-12, 23b-14, 23b-15). De biomeverhouding (23b-16) beoordeelt teststop 2b na change 30 vanzelf.
- [ ] `/ca tpboundary 1` en `2`: geen zwevende brokken of afgekapte vormen in de zones.

W-achteraf (niet blokkerend): portal vanuit een spleet of tafelberg naar de Nether en terug; F3-biome bij de nieuwe vormen; geen fouten in de log.

---

## Risico's / aandachtspunten

- **Prestaties** (budget 23b-9): per-blok-termen zijn de grootste post. Kolomtabel en vroege exits zijn verplicht; uitsplitsing en knoppen staan hierboven.
- **De shell-aanpak voor richels en nissen** kan er onregelmatig uitzien: de wandhelling wisselt en de interpolatie vlakt hem af. **Prototype eerst op devkaarten** (bouwstap 5). Lezen richels of nissen niet als zodanig: stop-en-vraag. Alternatief: nissen als cel-vormen, zoals de rotsvormen, langs de wand.
- **Begaanbaarheid:** spleten + tafelbergen + vinnen kunnen te veel worden. De aanbevolen maten houden landverbindingen (dichte randen), bruggen en puinhellingen. De S-test op samenhang toetst het grof; teststop 2b (na 30) toetst het speelgevoel.
- **Late in-game check (23b-17, keuze gebruiker):** 25, 26 en 30 bouwen op dit terrein vóórdat de gebruiker het in-game heeft gezien. Leidt teststop 2b tot een terreinwijziging, dan moeten de metingen van 25 (ertsen per gastgesteente en per band), 26 (zandplafonds, reservoirs) en 30 (hoogtes, hoodoos) opnieuw. Beperken: alle afstemgetallen blijven in JSON (geen Java-wijziging nodig voor maten), en de bouwer van 23b levert devkaarten (bovenaanzicht + doorsnede) van 4 vaste plekken in het rapport, zodat de hoofdsessie ze de gebruiker kan tonen zonder teststop.
- **Oasezones en de extra octaaf (23b-8):** de waterfractie, de poelbreedte en de oeverbreedte hangen nu alle drie aan dezelfde ruis. Lukt het niet om met één drempelset 6–8 % water én een grasoever van 6–14 te halen (bijv. omdat de grove octaaf de poelen te ongelijk maakt), dan **stop-en-vraag** (alternatief: een tweede ruis voor de verbreding met getrapte zones in de surface rules en een `shift` in `noise_band` van 23c — meer werk en niet exact concentrisch).
- **Zwevende of hangende blokken:** vaste dichtheid ±1, het kernmasker, flood-fill-tests en de "hangend zand"-telling.
- **Water:** nieuwe open ruimte onder het preliminary surface en onder Y 63 vult de aquifer. Daarom zijn alle bodems ≥ Y 66/70 en blijft het preliminary surface op de laagste kloofbodem.
- **Surface-randgevallen:** de bovenkant van naalden en vinnen krijgt rood zand (als vloer), de onderkant van bruggen rode zandsteen. Wordt een dunne top rood zand op 1 blok rots, dan blijft hij liggen (er staat vast blok onder). In W beoordelen.
- **Lagen van de richels:** de golving van de laaggrenzen (surface-diepte) kan een richel 1–2 blokken naast zijn crème band leggen. Met de dikkere crème banden van 23b-13 B (3–4 blokken) kleiner; anders de band in `canyon_details` iets breder maken.
- **Vergelijkbaarheid met change 23:** de oude perf-meting was op een oase-plek; daarom twee meetplekken.
- **Change 24** is gecommit (`3a9b560`): geen overlap meer te verwachten.

---

## Backlog-kandidaten (niet bouwen; **op 2026-10-01 al in `docs/backlog.md` gezet**)

- `[structures] Klifwoningen in de wandnissen van ring 2 (vlakke vloer, 6–16 breed, onder een richel; ligging deterministisch per seed). Ook uitkijkposten op tafelbergen en touwbruggen over spleten. (bron: change 23b, teststop 2)`
- `[mobs] Ring 2-kloofmobs die de nieuwe vormen gebruiken (klimmende schorpioenen op richels, nesten in nissen). Zie de bestaande ring 2-mobsregel. (bron: change 23b, teststop 2)`
- `[atmosphere] Windgeluid/fluiten in spleetkloven (ambient per positie, niet per biome). (bron: change 23b)`
- `[atmosphere] Zandstorm-duw (change 28) bij spleten en richels: valgevaar; nalopen. (bron: change 23b)`
- Het texturepunt (`canyon_cobblestone`-palet) staat niet in de backlog maar als expliciet aandachtspunt in plan 37.

---

## Ontwerpvragen

Ronde 1 = kernvormen (1–5). Ronde 2 = wanden, kloofbodem, oase, budget (6–9). Ronde 3 = routine (10–17): open punten uit teststop 2 die de feedback niet beantwoordt, plus het moment van de teststop. Vraag 1 (diepte en aantal kloven) en vraag 6 (oase) van teststop 2 zijn al beantwoord door de feedback ("canyons zien er goed uit"; oase-feedback → 23b-8 en 23c).

### 23b-1. [Belangrijk] Welk patroon krijgen de spleetkloven?
- **A. Breuknetwerk (eigen Java-type `cell_edges`): gejitterde cellen van ~110 blokken met meanderende breuken, ~60 % open (aanbevolen).** Het plateau valt uiteen in "eilanden" van ~80–200 blokken met doodlopende spleten. Dichte breuken zijn landverbindingen, zodat er altijd een route bestaat. Spleten monden uit als hangende monding (droge waterval) in de hoofdkloven. De breedte is exact te sturen. Wel één extra Java-type (puur, getest).
- **B. Meanderende ruisgeulen (|ruis| onder een drempel, per blok uitgehakt).** Organischer en zonder celstructuur, maar de breedte wisselt sterk (2–10, vlekken waar de ruis vlak loopt). Geen gegarandeerde landverbindingen, en geulen kunnen eindeloos doorlopen.
- **C. Alleen als smal en diep verlengde van de bestaande zijtakken.** Het minste werk; het plateau blijft grotendeels open en vlak, dus de feedback "te makkelijk over te steken" wordt maar deels opgelost.

Antwoord: **A** — breuknetwerk met eigen Java-type `cell_edges` (gejitterde cellen ~110, meanderende breuken, ~60 % open; dichte breuken = landverbindingen; hangende monding in de hoofdkloven).

### 23b-2. [Belangrijk] Hoe breed en diep zijn de spleten, en hoe steek je over?
- **A. 3–6 breed (niet te springen), 25–45 diep (bodem ~Y 90–105), onderin 1–2 breder; om de ~40–100 blokken een overdekt stuk of klemblok als brug (aanbevolen).** Echte obstakels met schaduw beneden. Oversteken via bruggen, landverbindingen of afdalen; geen eindeloze omwegen.
- **B. 2–4 breed (vaak te springen), 15–30 diep, weinig bruggen.** Meer "scheuren" dan kloven: minder obstakel, maar wel gevaarlijk (vallen).
- **C. 5–9 breed, tot de kloofbodem diep (~Y 70), bruggen om de 30–60 blokken.** Spectaculairder, maar lijkt op kleine hoofdkloven en vraagt meer bruggen, anders wordt het plateau onbegaanbaar.

Antwoord: **A** — 3–6 breed, 25–45 diep (bodem ~Y 90–105), onderin 1–2 breder; om de ~40–100 blokken een overdekt stuk of klemblok als brug.

### 23b-3. [Belangrijk] Hoe zien de getrapte tafelbergen eruit?
- **A. 2–3 treden bovenop het plateau (wanden 10–16, treden ≥ 8 breed), toppen Y ~145–178, op ~30 % van het plateau en ≥ ~20 blokken van de kloofranden (aanbevolen).** De kloofwanden blijven 55–70 hoog, de tafelbergen staan achter de rand. Ze breken het zicht en de route en geven uitkijkpunten. Wel meer ondergrond (perf ~+2 ms) en de bovenste helft van de wanden ligt boven het zonnesteenbereik (gevolg voor 25).
- **B. Op en neer: lagere banken (Y ~115–125) naast tafelbergen tot ~Y 155; gemiddelde hoogte gelijk.** Meer reliëf zonder extra volume (goedkoper, zonnesteenbereik klopt). Maar kloofwanden worden plaatselijk lager (minder dramatisch) en de lagere banken liggen dicht bij de spleetbodems.
- **C. Eén trede van 8–12, toppen ≤ Y 150.** Rustig: het plateau wordt een beetje heuvelig. Lost "te plat" maar deels op.

Antwoord: **A** — 2–3 treden bovenop het plateau (wanden 10–16, treden ≥ 8 breed), toppen Y ~145–178, op ~30 % van het plateau en ≥ ~20 blokken van de kloofranden. Gevolg 25: zonnesteen opgerekt tot ~Y 180.

### 23b-4. [Belangrijk] Bogen, ribben en rotsbruggen: waar en hoe vaak?
- **A. Ribben (vinnen 2–4 dik, 12–30 lang, 6–18 hoog) ~1 per 3 chunks op het plateau, waarvan ~20 % met een gat (boog); bruggen alleen over spleten (23b-2), niet over hoofdkloven (aanbevolen).** Alles via `rock_pillars` en `canyon_details`, dat ook de hoodoobogen van 30 levert. Gegarandeerd nooit zwevend.
- **B. Als A, plus zeldzame grote natuurlijke bruggen over hoofdkloven (~1 per km²).** Spectaculaire herkenningspunten. Wel een extra vorm die zijn richting uit de kloofruis afleidt (dwars op de kloof, 30–60 lang): meer Java, meer kans op rare gevallen bij kruisingen, en hogere kosten in een groot y-venster.
- **C. Alleen bruggen over spleten; vrijstaande bogen pas met de hoodoos (30).** Eenvoudiger; het kloofgebied krijgt minder herkenningspunten.

Antwoord: **A** — ribben (vinnen 2–4 dik, 12–30 lang, 6–18 hoog) ~1 per 3 chunks op het plateau, ~20 % met een gat (boog); bruggen alleen over spleten, niet over hoofdkloven.

### 23b-5. [Belangrijk] Rotsnaalden en grote keien (terrein, niet de losse keien van 23c)
- **A. Plateau: naalden 8–20 hoog (straal 1,5–3) en grote half begraven keien (straal 2–4), samen ~1 per 2 chunks; kloofbodem: zeldzame torens van 25–50 hoog (straal 3–5) in brede bodems (~1 per 10×10 chunks) (aanbevolen).** Obstakels en herkenningspunten zonder dat het een hoodooveld wordt (dat blijft het karakter van biome 30).
- **B. Dicht obstakelveld: ~2 vormen per chunk op het plateau.** Het plateau is echt lastig te doorkruisen, maar lijkt op `hoodoo_fields` (30) en kost meer per blok.
- **C. Alleen de torens in de kloven; geen naalden of keien op het plateau.** Kloven worden spectaculairder; het plateau leunt helemaal op spleten en tafelbergen.

Antwoord: **A** — plateau: naalden 8–20 hoog en grote half begraven keien, samen ~1 per 2 chunks; kloofbodem: zeldzame torens van 25–50 hoog in brede bodems (~1 per 10×10 chunks).

### 23b-6. [Belangrijk] Wandgrotten, richels en overhang: hoe vaak, hoe groot, begaanbaar?
- **A. Richels van 1–3 breed op de crème banden ("harde laag", onderbroken, ~50 % van de wandlengte); nissen in de band eronder (6–16 breed, 4–7 hoog, 3–8 diep, vlakke vloer, ~1–2 per 100 blokken wand); overhangende kaprots aan de plateaurand van 2–3 blokken (aanbevolen).** Lagen, richels en nissen vertellen één geologisch verhaal. Richels zijn klimroutes en nissen zijn schaduwplekken. Klaar voor klifwoningen (fase 3). Wel een prototype-risico (zie Risico's).
- **B. Alleen nissen en de overhang, geen richels.** Eenvoudiger en goedkoper, maar de wanden blijven verder glad, zonder klimroutes.
- **C. Groot: wandgrotten van 8–20 diep, soms verbonden met de noise-grotten; richels zoals A.** Meer te ontdekken, maar minder voorspelbaar (open plafonds, gaten naar de grotten) en duurder. Overlapt met de grotten van change 26.

Antwoord: **A** — richels van 1–3 breed op de crème banden (~50 % van de wandlengte), nissen in de band eronder (6–16 × 4–7 × 3–8, vlakke vloer, ~1–2 per 100 blokken wand), overhangende kaprots van 2–3 blokken aan de plateaurand.

### 23b-7. [Belangrijk] Droge watervallen en puinhellingen
- **A. Hangende zijtakken (bodem ~10–20 boven de hoofdkloof, valwand bij de monding), treden van 5–10 in de hoofdkloofbodem om de ~150–300 blokken met een droge kolk eronder, en puinhellingen (~1:1, grind/rood zand) op ~15 % van de wanden als klimroute (aanbevolen).** De kloofbodem krijgt verloop en drama, en je kunt de kloof op vaste plekken in en uit. Puur JSON (2D), vrijwel gratis.
- **B. Alleen hangende zijtakken en puinhellingen; de hoofdkloofbodem blijft vlak.** Rustiger kloofbodem, makkelijker te belopen.
- **C. Alleen puinhellingen (klimroutes).** Lost de begaanbaarheid op, maar voegt weinig avontuur toe.

Antwoord: **A** — volledig: hangende zijtakken met valwand, treden in de hoofdkloofbodem met droge kolk, puinhellingen (~1:1, grind/rood zand) op ~15 % van de wanden.

### 23b-8. [Belangrijk] Hoe groot worden de oases?
- **A. Minder maar grotere poelen: ~12 % van `oasis_flats` is water, poelen van 20–60 breed en tot 4–5 diep; grasoever 4–10 breed; overgangsring van 8–16 breed (grof zand/aarde met grasvlekken) naar de woestijn (aanbevolen).** Leest als echte oase met een zachte overgang. De zones komen uit dezelfde poelruis (puur JSON). 23c zet er de begroeiing op. Kanttekening: na change 30 is `oasis_flats` ~12 % van ring 2, dus de oases blijven zeldzaam.
- **B. Eén groot oasemeer (40–120 breed) per oasevlek, plus verspreide kleine poelen.** Een sterk herkenningspunt. Vraagt een extra cel-achtige vorm (meer werk) en geeft veel water op één plek (uitdroging, change 27).
- **C. De huidige poelen ×1,5 groter, verder niets.** Minimale wijziging; de overgang naar de woestijn blijft hard.

Antwoord: **Eigen antwoord (keuze gebruiker):** "De oases krijgen meer gras eromheen en de waterpoelen worden een klein beetje groter in de breedte op sommige stukken." → géén ~12 % water: waterfractie ongeveer zoals nu, licht omhoog naar ~6–8 %; poelen plaatselijk breder (extra grove octaaf in `ring_2_pools`, niet overal); grasoever duidelijk breder (6–14 blokken, vlak rond de waterlijn) met een overgangsring die binnen vooral groen is. Uitgewerkt in stap 7 en de S-tests.

### 23b-9. [Belangrijk] Prestatiebudget van de noise-stap in ring 2
- **A. ≤ 1,5× vanilla, in het kloofgebied én op het meetpunt van change 23; ring 1/3 ongewijzigd; de hele chunk in ring 2 ≤ vanilla (aanbevolen).** Ruimte voor alle aanbevolen vormen (schatting 1,45–1,6× in puur kloofgebied, dus krap). Bij overschrijding eerst de knoppen uit het plan.
- **B. Strikt ≤ 1,3× (het oude budget).** Haalbaar met kleinere y-vensters en zonder de tafelbergen of de spleetwiebel: minder vormen of kleiner.
- **C. ≤ 1,75×.** Ruim, ook voor 23b-4 B en 23b-6 C. Chunks laden in ring 2 merkbaar trager bij snel vliegen/verkennen (vooral multiplayer).

Antwoord: **C (keuze gebruiker)** — budget noise-stap ring 2 ≤ 1,75× vanilla (gedeeld met 26/30/31; richtwaarde 23b ≤ ~1,6×); ring 1 en 3 ongewijzigd. Stop-en-vraag-drempels in "Prestatiebudget".

### 23b-10. (routine; teststop 2, vraag 2) Treden in kloofwanden en steilranden
- **A. Laten zoals ze zijn; puinhellingen (23b-7) en richels (23b-6) geven de klimroutes (aanbevolen).**
- **B. Bredere/meer treden (minder steil).** De kloven worden minder dramatisch.
- **C. Minder treden (steiler).** Alleen puinhellingen als route.

Antwoord: **A** — treden laten zoals ze zijn; puinhellingen en richels geven de klimroutes.

### 23b-11. (routine; teststop 2, vraag 3) De oasekant van de steilrand (nu grijze steen met zand/zandsteen)
- **A. Kloofsteenlagen ook aan de oasekant: steile vlakken en ondergrond boven Y ~74 in `oasis_flats` (aanbevolen).** De wand leest als één geheel; de biomegrens blijft.
- **B. Zandsteen (vanilla of geschroeid) als ondergrond van de oase.** Een duidelijke naad halverwege de wand.
- **C. De biomegrens naar de voet van de steilrand schuiven.** De oase krimpt; de verdeling van change 30 moet opnieuw afgesteld.

Antwoord: **A** — kloofsteenlagen ook aan de oasekant: steile vlakken en ondergrond boven Y ~74 in `oasis_flats`; de biomegrens blijft.

### 23b-12. (routine; teststop 2, vraag 4) Overgang ring 1 → plateau (tot ~70 blokken stijging over ~50 blokken)
- **A. Laten; tafelbergen en fijne vormen vervallen in de zone (kernmasker) (aanbevolen).**
- **B. Afzwakken: het plateau zakt in de zone (`relief_weight` × een breder masker), dus een lagere en flauwere klim.** Kost een extra masker; de randen van ring 2 worden minder spectaculair.

Antwoord: **A** — overgang ring 1 → plateau laten; tafelbergen en fijne vormen vervallen in de zone (kernmasker).

### 23b-13. (routine; teststop 2, vraag 5) Kloofsteenlagen (rood 28 / oranje 12 / crème 8 per 48 blokken)
- **A. Laten; de richels liggen op de crème banden (aanbevolen).**
- **B. Meer of dikkere crème banden** (meer richels, lichtere wanden).
- **C. Dunnere banden** (drukker beeld).

Antwoord: **B (keuze gebruiker)** — meer crème banden: rood 22 / oranje 12 / crème 14 per 48, in 4 crème banden van 3–4 (was 3 banden van 2–3). Meer contrast; ~1 extra richelniveau per wand (+33 % crème banden), nissen blijven ~1–2 per 100 blokken wand. Uitgewerkt in stap 5b.

### 23b-14. (routine; teststop 2, vraag 8) Kloofbodem en nieuwe bodems
- **A. Laten (rood zand + gebarsten klei + grind); de drempel schuift mee met de treden (Y 96); kolken en spleetbodems krijgen dezelfde regels; puinhellingen grind/rood zand (aanbevolen).**
- **B. Minder grind (de grote grindvlekken van change 23 kleiner).**
- **C. Spleetbodems een eigen uiterlijk (grindvlekken).** Surface rules kennen het spleetsignaal niet, dus dit wordt een feature in 23c (grind op spleetbodems via een slope-filter); iets meer werk daar.

Antwoord: **A** — kloofbodem laten (rood zand + gebarsten klei + grind); drempel schuift mee met de treden (Y 96); kolken en spleetbodems dezelfde regels; puinhellingen grind/rood zand.

### 23b-15. (routine; teststop 2, vraag 9) Grotten in het plateau (veel, soms dunne daken, rood zand op grotbodems)
- **A. Laten; onder tafelbergen komen er wat meer bij (aanbevolen).**
- **B. Minder dunne daken: de grotingangen (`entrances`) in het rotsgebied afzwakken (schaalfactor in `stack.json`).** Ring 1-gelijkheid blijft (alleen ring 2).

Antwoord: **A** — plateaugrotten laten; onder tafelbergen komen er wat meer bij.

### 23b-16. (routine; teststop 2, vraag 10) Verhouding canyonlands ~59 % / oase ~41 %
- **A. Laten tot change 30 (die zet het op 30/12/…) (aanbevolen).**
- **B. Nu al de drempel schuiven naar ~65/35.** Dubbel werk met 30.

Antwoord: **A** — verhouding laten tot change 30.

### 23b-17. (routine) Wanneer is teststop 2b?
- **A. Eén blokkerende stop na 23c (terrein + aankleding samen, ~20 min), vóór change 25 (aanbevolen).** Eén sessie. 23c verandert het terrein niet, dus kan de terreincheck niet ongeldig maken.
- **B. Stop na 23b (alleen terrein, ~15 min); 23c in teststop 3.** Sneller door naar 25, maar de oase/palm-feedback wordt pas in teststop 3 gecheckt.
- **C. Twee korte stops (na 23b en na 23c).** Het veiligst, het meeste werk voor de gebruiker.

Antwoord: **Eigen antwoord (keuze gebruiker):** teststop 2b pas na change 30, "als alles is ingericht". Geen blokkerende stop na 23b/23c of vóór 25; de terreincheck van ring 2 (23 + 23b + 23c + 25 + 26 + 30) wordt één blokkerende teststop na change 30, vóór 31. Teststop 3 na 37 blijft. De gebundelde checklist schrijft de bouwer van 30.

---

## Beslissingenlog-regels (datum = bouwdatum; tekst volgens de antwoorden van 2026-10-01)

| Datum | Categorie | Beslissing | Reden |
|---|---|---|---|
| 2026-10-02 | worldgen | Change 23 gesplitst in 23b (terreinvormen, density functions/surface rules) en 23c (aankleding, features); de in-game terreincheck ring 2 wordt teststop 2b na change 30 (gebundeld 23 + 23b + 23c + 25 + 26 + 30), geen stop vóór 25 (keuze gebruiker) | Andere techniek en valkuilen; de gebruiker beoordeelt het terrein liever één keer "als alles is ingericht" |
| 2026-10-02 | worldgen | Spleetkloven: breuknetwerk (`cell_edges`, cellen ~110, 60 % open), 3–6 breed, 25–45 diep, bruggen/klemblokken om de 40–100 blokken, hangende monding in hoofdkloven | Plateau niet meer vrij over te steken; altijd een route |
| 2026-10-02 | worldgen | Tafelbergen: 2–3 treden (wanden 10–16) bovenop het plateau, toppen tot ~Y 178, ≥ 20 blokken van kloofranden | Reliëf en uitkijkpunten; kloofwanden blijven 55–70 |
| 2026-10-02 | worldgen | Eén generiek DF-type `rock_pillars` (naald, kei, vin met optioneel gat = boog, optionele kap) voor ring 2-rotsvormen; change 30 maakt de hoodoos als JSON-instantie | Eén aanpak, nooit zwevend, puur per seed |
| 2026-10-02 | worldgen | Rotsvormen: plateau naalden/keien/vinnen ~1 per 2 chunks (20 % vinnen met boog), torens in brede kloofbodems; geen bruggen over hoofdkloven | Obstakels en herkenningspunten zonder hoodooveld |
| 2026-10-02 | worldgen | Wanden: richels op de crème banden, nissen eronder (6–16 × 4–7 × 3–8, vlakke vloer), overhangende kaprots aan de plateaurand; `canyon_details` per blok buiten de interpolatie | Eén geologisch verhaal; klimroutes; plek voor klifwoningen |
| 2026-10-02 | worldgen | Kloofbodem: hangende zijtakken, treden met droge kolk, puinhellingen ~1:1 als klimroute | Drama en vaste routes in en uit de kloof |
| 2026-10-02 | worldgen | Oase (keuze gebruiker): waterfractie ~6–8 % (licht omhoog), poelen plaatselijk breder via een extra grove octaaf in `ring_2_pools`, grasoever duidelijk breder (6–14, vlak rond de waterlijn), overgangsring binnen groen naar buiten dor; zones uit dezelfde poelruis; lagen aan de oasekant van de steilrand | "Meer gras eromheen, poelen op sommige stukken iets breder"; één ruis houdt water, oever en ring concentrisch |
| 2026-10-02 | worldgen | Kloofsteenlagen (keuze gebruiker): rood 22 / oranje 12 / crème 14 per 48, in 4 crème banden van 3–4 | Meer contrast; meer richelniveaus op de crème banden |
| 2026-10-02 | worldgen | Fijne terreinvormen per blok buiten de interpolatie met een eigen kolomtabel (geen `cache_2d`), vervallen in de overgangszones (kernmasker) | Dunne vormen overleven interpolatie niet; `cache_2d` cachet per blok niet; geen zwevende brokken |
| 2026-10-02 | worldgen | Prestatiebudget noise-stap ring 2 (keuze gebruiker): ≤ 1,75× vanilla, gedeeld met 26/30/31 (richtwaarde 23b ≤ ~1,6×); ring 1/3 ongewijzigd; hele chunk ring 2 gemeten en gerapporteerd | Ruimte voor alle vormen en de latere ring 2-changes; trager laden bij snel verkennen geaccepteerd |

Commit (na `/code-review`): `worldgen: add ring 2 terrain forms: slot canyons, mesas, rock pillars, canyon walls`.

## Bouwnotities (change 23b, 2026-10-02)

Gebouwd in twee sessies (de eerste stopte halverwege op verzoek van de gebruiker). De JSON van ring 2 komt uit een script (`build/c23b/gen_df23b.py` + `gen_surface23b.py`, niet gecommit); de debughulpen (`/cadev`, `DevProbe`, `density_function/dbg/`) waren tijdelijk en zijn niet gecommit.

- **Gebouwd volgens plan:** stap 0 (ontdubbelen poelen → `pools.json`, stapel → `stack.json`, `core_mask`), tafelbergen, kloofbodem met treden/kolken/hangende zijtakken/puinhellingen, spleetkloven (`cell_edges` + `canyon_details`), rotsvormen (`rock_pillars`), richels/nissen/kaprots, lagen 22/12/14 met 4 crème banden, oaseterrein met zones, lagen aan de oasekant.
- **Zwevende componenten (open punt van de vorige sessie): oorzaken en fix**
  1. *Rotsvormen boven een grotmond:* de voet volgde `height` (vloer − 2), die geen grotten kent; boven een grotingang hing de kei of vin in de lucht. Fix: `rock_pillars.root_y` (50): elke kolom loopt door tot `min(vloer − 2, 50)`, dus als pilaar door de grot.
  2. *Richels en kaprots* (laag `−δ < shell < 0`, δ uit de helling): in het lage, ruwe deel van een wand legt de 3D-wandruis de echte vloer ±2–3 blokken naast `height`, en de band lag dan als losse plaat boven de vloer; ook buurkolommen met een blok verschoven banden, grotmonden (`entrances`) in de wandhuid en de ondergraving van de kaprots lieten stukjes los. Fix in `CanyonDetails`: (a) een richel is de wand naar buiten verschoven (open blok vast als de shell `reach` blokken bergop vast is), met de shell exact nagemaakt via `CellInterpolation` (de `shell`-marker is vervallen); (b) een richelblok moet ook een vast blok richting de wand hebben, gevolgd door de buurkolommen (keten van max. 7 stappen, kolomtabel met rand); (c) de wandhuid (~2 blokken) achter een richel wordt verzegeld tegen grotmonden; (d) geen richels of kaprots waar de grond 3 blokken vóór de wand minder dan 6 lager ligt (`foot_distance`/`min_height`, gemeten waar de wand de richel bereikt, zodat alle kolommen vóór één wandstuk het eens zijn). Een eerste poging met een vaste afstand boven de eigen kolomvloer maakte het erger (knipte de keten door) en is teruggedraaid.
  3. Het hangende rode zand kwam bijna helemaal van **vanilla-carvers** (grotcarver tot Y 180) en lavameren: die hollen ná de surface-stap uit. Gemeten met een testdatapack zonder carvers (zie `CODEBASE.md`). Backlog `[worldgen]`, beslissen bij change 26.
- **Prestaties (werkwijze, geen ontwerpkeuze):** een `ThreadLocal` per density-function-kopie vervuilde de thread-local-maps van de worldgen-threads (+30–40 % noise-stap, ook in vanilla- en ring-code) → `ThreadOwned`. De rotsvormen staan niet in een `max(stack, …)` maar in de lijst `rocks` van `canyon_details` (één opzoeking per blok i.p.v. vier). `height` naast de chunk wordt per thread gecachet (`HeightGrid`).
- **Afwijkingen van het plan (implementatie en afstemming):**
  - Plateau-rotsvormen in twee instanties: `plateau_rocks` (naalden + keien, cel 16, kans 0,8) en `plateau_fins` (vinnen/bogen, cel 24, kans 1, eigen hash `ring_2_fin_cells`); torens `canyon_spires` cel 32. Gemeten (~8.750 plateau-chunks): naalden + keien **0,44–0,46 per plateau-chunk** (doel ~0,5), vinnen **0,20** (doel ~0,33: een vin van 12–30 past niet vaak genoeg in een cel en kleinere cellen maken ze korter; lengte nu meest 5–25), bogen 19 % van de vinnen, torens **~1 per 120 kloof-chunks** (doel ~1 per 100).
  - `cell_edges`: de afstand is per rand gecorrigeerd voor de rek van de warp (anders sprong hij waar de dichtstbijzijnde rand wisselt); continu, helling ≤ ~1,25 per blok bij een breuk (plan-test ≤ 1,05 aangepast: eerste-orde-correctie).
  - Spleten alleen op het plateau (`slot_weight` × `canyon_depth` < 0,12); anders kerfden ze ondiep in de treden van de kloofwanden.
  - Nissen: `min_floor` 66 (een kolkbodem kan op 64–65 liggen).
  - Kloofbodemtreden op `ring_2_canyon_floor` met `firstOctave` −10 (plan −8); puinhellingen op ~20 % van de wanden (plan ~15 %).
  - `canyon_details` leest `terrain` en interpoleert zelf (geen `shell`-marker; de ring gate-test over die marker vervalt).
- **Gevolgen voor latere plannen:** plan 30 bijgewerkt (punt 9: twee plateau-instanties, `root_y`, `rocks`-lijst, geen `shell`). Budget: het oase-meetpunt staat op 1,72× (zie Testresultaten), dus weinig ruimte voor 26/30/31 (aandachtspunt in `CODEBASE.md` en de backlog).

### Testresultaten

- **B:** `./gradlew build` groen, met de nieuwe tests `CellEdgesTest`, `RockPillarsTest`, `CanyonDetailsTest` (onder meer geen zwevende blokken op een synthetische ruwe wand bij helling 2,5/4/7, en rotsvormen via `rocks`), `CellInterpolationTest`, `ColumnTableTest`; `Ring2TerrainJsonTest` aangepast.
- **D:** `./gradlew runData` zonder fouten, gegenereerde output ongewijzigd.
- **S** (seed 20260930, werelden `smoke-change23b-…`; rooktest `smoke-change23b-final`: start schoon, codecs laden, geen client-class, 0 × de "non-overlapping"-waarschuwing; change 23 gaf er duizenden):
  - Stap 0 neutraal: 11 van 12 ring 2-chunks blok voor blok gelijk aan change 23, één chunk 102 blokken graniet/dioriet/andesiet anders (ertsblobs, features).
  - Ring 1: 300/300 biomepunten gelijk; steen/deepslate in 6 chunks gelijk op 1 blok na (features).
  - Hoogtes (raster 32 × 32 over ring 2): plateau mediaan Y 133, 99 % ≤ 170, max ~174; **22 % van de plateaukolommen ≥ Y 145** (≥ 15 %); 32 % van het plateau heeft een tafelberg (+12: 12 %, +24: 9 %, +36: 7 %); kloofbodems Y 66–89; `oasis_flats` Y 59–92 (99 %); **0 kolommen met water boven Y 63 in canyonlands**.
  - Spleten (384 × 384 op het plateau bij x 2.368, z −640): 99,5 % van de spleetkolommen ≥ 20 lager dan een kolom 4 blokken ernaast; breedte 3–6 bij 98 % (mediaan 4); bodem ≥ Y 84; bruggen + klemblokken 1,56 per 100 blokken spleet (800 × 800); samenhangend plateau (≥ 90 % in één stuk) in 5 van 6 vensters van 128 (het zesde 73 %: hoekstukken die buiten het venster aansluiten).
  - Rotsvormen: zie hierboven; bogen > 0 in 256 chunks, torens > 0 in 400 chunks.
  - Richels en nissen in het kloofvenster (65.536 kolommen): 614 kolommen met richel/kaprots, 288 met nissen; laagste nisvloer Y 82 (≥ 66).
  - **Zwevend** (flood fill, vast blok zonder verbinding met Y < 60; 80 + 64 + 64 kloofchunks, 40 in zone 1→2, 40 in zone 2→3): zonder carvers 1 blokje (interpolatie van de wandruis, change 23) en 1 `withered_dirt` (feature): **0 door de vormen van 23b**; met carvers 3–4 brokjes extra (carver). **Hangend zand/grind:** zonder carvers alleen 25 blokken boven één vanilla-lavameer; met carvers ~80–90 per 80 kloofchunks (carver; backlog).
  - Oase: water **6,3 %** van `oasis_flats` (hele ring, raster; was 4,9 %), op r = 2.500 5,9 %; poelen in 4 vensters: mediaan kleiner (10, meer kleine poelen), q75 42 (≥ 1 op 4 poelen ≥ 1,3× de oude mediaan van 18); grasoever **mediaan 10 blokken** (was 2,8); gras 64–72 % tot 12 blokken van het water, daarna aflopend naar 11 % op 36–39 blokken; `coarse_dirt` 7–18 % in de overgangsring.
  - Lagen (8 kloofchunks, Y 60–145): rood 50 %, oranje 22 %, crème 28 %. Oasekant van de steilrand: kloofsteen zichtbaar boven Y 74 (298 vlakken), 64 vlakken grijze steen (randen van grotjes en carvers, geen hele wand).
  - **Determinisme** (twee verse werelden, de tweede in omgekeerde volgorde, 16 kloof- + 16 oasechunks): steen, de drie tinten, lucht, water, rood zand, zand, `coarse_dirt`, gras, zandsteen, gebarsten klei en de hoogte per kolom **identiek in 32/32 chunks**; verschillen alleen in vanilla's ondergrondse blobs en ertsen (graniet/dioriet/andesiet/tufsteen; volgorde-afhankelijk, ook in vanilla).
  - **Prestaties** (noise-stap, gemiddelde van 5 rondes in één sessie; vóór = een git-worktree van `HEAD`): kloofgebied vanilla 59,4 / vóór 43,3 / na **53,8 ms = 0,91×**; meetpunt x 2.500 (100 % oase) vanilla 27,8 / vóór 40,2 / na **47,7 ms = 1,72×** (budget 1,75: gehaald; richtwaarde 1,6: niet, dus aandachtspunt); ring 1 37,1 → 36,6 en ring 3 24,5 → 23,7 (ongewijzigd; spreiding per ronde ±10–15 %). Hele chunk in ring 2 onder vanilla (79 vs 101 ms, 81 vs 85). Uitsplitsing: alleen de stapel (zonder `canyon_details` en rotsvormen) kost hetzelfde (53,4 / 47,7 ms): de fijne vormen kosten ~0–3 ms, de stijging komt van het terrein zelf (tafelbergen, poelen/oever). Knop 1/2 helpen daardoor niet; knop 3–5 zouden zichtbaar zijn en zijn niet gebruikt.
  - Devkaarten (niet gecommit): `build/devmaps/change23b/`: `plateau_slots_mesas_x2368_z-640.png`, `canyon_x2304_z512.png`, `oasis_x256_z2048.png`, `oasis_scarp_x2560_z-256.png`, `section_wall_x2160.png`, `elevation_wall_from_west.png`.

### Code review (`/code-review medium`)

- **Opgelost (hoog):** `rock_pillars` las `weight` en `floor` (beide `flat_cache`) op willekeurige blokposities; in een noise chunk geeft een `flat_cache` binnen de chunk de waarde van het rasterpunt eronder en daarbuiten de exacte waarde, dus elke chunk die een cel overlapt kon de vorm anders plaatsen (afgekapte vormen op chunkgrenzen). Nu alleen op het 4-blokraster gelezen en bilineair geïnterpoleerd (zoals `canyon_details`). Hertest: zwevend gelijk (alleen carver-resten), determinisme terrein identiek in 32/32 chunks (één palmblad verschilt: feature), dichtheden nauwelijks veranderd (naalden + keien 0,44 per plateau-chunk).
- **Geaccepteerd (laag):** de banden van richels/nissen schuiven met `(int)(surface · 2,75 + 3,125)`; vanilla's surface-diepte telt er een willekeurige 0–0,25 bij (positionele random van de wereld, niet beschikbaar in een density function). In een deel van de kolommen ligt een richel daardoor 1 rij naast zijn crème band; de banden zijn 3–4 dik, dus de richel blijft grotendeels op crème. In teststop 2b beoordelen (plan: "anders de band in `canyon_details` iets breder maken").

## In-game checklist (W) — voor teststop 2b (na change 30; overnemen in plan 30)

runClient, testseed `20260930`; creatief/spectator om te kijken, survival voor het oversteken. Coördinaten x, y, z (y = ongeveer de grond).
- [ ] **Oversteken:** start op het plateau bij 2.380 140 −620 en loop ~400 blokken naar het oosten (tot x ~2.780): spleten, tafelbergwanden (+12/+24/+36) en vinnen; uitdagend maar te doen via dichte randen, bruggen en treden.
- [ ] **Spleten:** het netwerk in 2.368–2.752 / −640…−256 (zie `plateau_slots_mesas_x2368_z-640.png`): breedte 3–6, diepte 25–45, ondergraven wanden, bruggen en klemblokken, en een hangende monding waar een spleet de bovenste kloofwand raakt.
- [ ] **Tafelbergen:** +36-top bij 2.584 168 656 (en 2.584 168 680): treden, wandhoogte, afstand tot de kloofrand.
- [ ] **Rotsvormen:** keien 2.506 134 699 en 2.521 133 677; naalden 2.534 133 695 en 2.553 130 633; vinnen 2.506 134 712 en 2.558 131 660; **bogen** 2.530 133 686, 2.604 167 661, 2.411 153 396; **torens** in de kloofbodem 2.453 75 586, 2.482 74 594, 2.450 75 559. Geen zwevende delen; lagen op de wanden; een pilaar door een grot eronder mag.
- [ ] **Wanden:** richels op de crème banden bij 2.388 104 448; nissen bij 2.534 94 449 en 2.527 95 450; overhangende kaprots langs de plateaurand erboven. Bereikbaarheid.
- [ ] **Kloofbodem:** droge waterval (trede met kolk) bij 2.416 67 584 → 2.416 76 576 en bij 2.424 67 568; puinhelling bij 2.456 92 680 (klimroute uit de kloof).
- [ ] **Oase:** grote poel met brede grasoever en overgangsring bij 470 63 2.142 en bij −2.372 63 713; kleinere poelen in `oasis_x256_z2048.png`; oasekant van de steilrand (kloofsteenlagen boven Y 74) bij 2.793 95 −3.
- [ ] **Lagen:** 4 crème banden per 48 blokken op elke kloofwand (bijv. 2.160 80–130 −1.090); richels op de crème banden.
- [ ] Open punten van teststop 2 (laten, nu bevestigen): overgang ring 1 → plateau, kloofbodem, plateaugrotten (23b-12, 23b-14, 23b-15).
- [ ] `/ca tpboundary 1` en `2`: geen zwevende brokken of afgekapte vormen in de zones.

W-achteraf (niet blokkerend): portal vanuit een spleet of tafelberg naar de Nether en terug; F3-biome bij de nieuwe vormen; geen fouten in de log.
