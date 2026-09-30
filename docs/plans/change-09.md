# Change 9 — Ring 1-terrein: vanilla terrein + gegarandeerd land rond spawn

Scope: `worldgen` — change 9

Doel: ring 1 krijgt het vanilla overworld-terrein (oceanen, rivieren, bergen, grotten, deep dark) als ring 1-invoer van `ring_blend`, met rond 0,0 een continentalness-boost zodat het midden altijd land is, en de spawn altijd op land nabij 0,0 ligt.

---

## Afhankelijkheden / aannames

- Changes 0–7 zijn gebouwd zoals in `CODEBASE.md`: `ring_blend` (inputs per ring, smoothstep, alleen 1–2 inputs gerekend), `ring_noise`, placeholder-terrein `ring_N/<dim>.json`, noise settings `corruptedadventure:overworld` als vanilla-kopie met `final_density` → `ring_blend/overworld` en `initial_density_without_jaggedness` → `0.390625 + ring_blend/overworld`.
- **Change 8 is gebouwd** (bevestigd door de planning van change 8):
  - de biome-kopieën heten `corruptedadventure:<vanilla_naam>` (voor de tests: `corruptedadventure:ocean`, `deep_ocean`, `jagged_peaks`, `deep_dark`, …);
  - ring 1 in `corrupted_world.json` gebruikt de geneste biome source **`corruptedadventure:vanilla_climate`** (vanilla-klimaatparameters, zonder mushroom fields); `placeholder_ring_1` is weg;
  - `vanilla_climate` leest het klimaat via de `Climate.Sampler` van de noise router. Deze change verandert de router-velden `continents` en `depth`; de biomeverdeling rond spawn schuift daardoor bewust mee (geen oceaanbiomes op het opgetilde land);
  - de surface rules in `noise_settings/overworld.json` verwijzen naar onze keys (`corruptedadventure:…`), dus zand, terracotta enz. kloppen al. Change 9 raakt de surface rules niet; bij het bewerken van dit bestand (stap 5) alleen de genoemde router-velden en `spawn_target` aanpassen.
- Vanilla worldgen-JSON staat alleen in `build/moddev/artifacts/neoforge-21.1.252-client-extra-aka-minecraft-resources.jar` (`data/minecraft/worldgen/density_function/overworld/*.json`, `noise_settings/overworld.json`), niet in de merged- of sources-jar.
- Nether verandert niet (Nether-ring 1-terrein is change 14).

## Achtergrond (uitgezocht, niet opnieuw doen)

- **Vanilla-spawn:** `MinecraftServer.setInitialSpawn` → `Climate.Sampler.findSpawnPosition()` (`Climate.SpawnFinder`): begint op 0,0 en zoekt radiaal (tot 2.048, daarna fijn tot 512 rond de beste) naar het punt met de laagste *fitness* = klimaatafstand tot `spawn_target` (diepte genegeerd) + afstandsboete `1e8 · (r²/2500²)²`. Een punt op 512 blokken kost ~0,18 M, een weirdness-afwijking van 0,1 al 1 M: de spawn schuift dus makkelijk ~1.000 blokken op (en tot ~2.500). Daarna een spiraal over 11×11 chunks (`PlayerRespawnLogic.getSpawnPosInChunk`): eerste kolom met een vaste bovenkant en **geen vloeistof** bovenop. NeoForge biedt daarvóór `LevelEvent.CreateSpawnPosition` (cancelable).
- **Continentalness zit op drie plekken:** router-veld `continents` (klimaat → biomes en spawn-finder) én de splines `overworld/offset`, `overworld/factor`, `overworld/jaggedness` (terreinvorm). `depth` = gradient + `offset`; `sloped_cheese` gebruikt `depth`, `factor`, `jaggedness`; `final_density` gebruikt `sloped_cheese` + grotten; vanilla's `initial_density_without_jaggedness` gebruikt `depth` + `factor`. Een boost alleen op het router-veld geeft landbiomes op oceaanterrein; hij moet dus door de hele keten.
- Vanilla density functions kennen geen x/z-invoer (alleen `y_clamped_gradient`): een radiale afname rond 0,0 vraagt een klein Java-type.

## Plan

### 1. Kopie van de vanilla-terreinketen voor ring 1 (handgeschreven JSON)
Nieuwe bestanden in `src/main/resources/data/corruptedadventure/worldgen/density_function/ring_1/overworld/`:

| Bestand | Inhoud |
|---|---|
| `continents.json` | geboost continentalness (zie stap 3) |
| `offset.json`, `factor.json`, `jaggedness.json` | exacte kopie van vanilla, alleen `minecraft:overworld/continents` → `corruptedadventure:ring_1/overworld/continents` |
| `depth.json` | kopie, `overworld/offset` → `ring_1/overworld/offset` |
| `sloped_cheese.json` | kopie, `depth`/`factor`/`jaggedness` → de `ring_1/overworld/…`-versies |
| `initial_density.json` | vanilla `initial_density_without_jaggedness` (inline uit vanilla noise settings), met `depth`/`factor` → `ring_1/overworld/…` |

En `ring_1/overworld.json` (nu placeholder) wordt de **vanilla `final_density`** (inline uit vanilla noise settings), met `overworld/sloped_cheese` → `ring_1/overworld/sloped_cheese`. Grotten (`overworld/caves/*`), `erosion`, `ridges`, `ridges_folded`, `base_3d_noise`, `shift_x/z` blijven verwijzingen naar vanilla (`minecraft:`) — ongewijzigd, en zo gedeeld met de router.

Werkwijze: eenmalig Python-script in `build/` (niet committen) dat de vanilla-JSON uit de jar leest, de verwijzingen vervangt en wegschrijft. **Controle:** script dat de vervanging terugdraait en genormaliseerd vergelijkt met vanilla → identiek.

Waarom geen `minecraft:overworld/…` overschrijven: dat raakt alle vanilla-werelden als de mod geïnstalleerd is.

### 2. Density-function-type `corruptedadventure:radial_falloff` (Java)
- `worldgen/densityfunction/RadialFalloff.java`: 2D, codec `inner_radius`, `outer_radius` (blokken, afstand tot 0,0 in de eigen dimensie; validatie `0 ≤ inner < outer`). Waarde 1 binnen `inner_radius`, 0 vanaf `outer_radius`, daartussen smoothstep (hergebruik `RingBlend.smoothstep`, zelfde package). `minValue` 0, `maxValue` 1. Geen instantiestaat, geen `RingLayout` (dus geen `current()`-probleem bij het decoderen).
- Registratie in `ModDensityFunctionTypes` (`radial_falloff`).
- Waarden in de JSON, niet in de config: het is geen ringgrens, en een config-waarde zou bij wijziging breuklijnen in bestaande werelden geven (de ring-snapshot dekt dit niet).

### 3. Continentalness-boost (`ring_1/overworld/continents.json`)
`flat_cache( c + w · max(0, F − c) )` met `c` = `minecraft:overworld/continents`, `w` = `radial_falloff` en `F` = ondergrens.
- Land dat al hoog genoeg is blijft ongemoeid; alleen lagere waarden worden (glijdend) opgetild tot `F`. Geen knik aan de rand van de zone (smoothstep in `w`).
- Besloten (vraag 1): `F` = 0,03, `inner_radius` 300, `outer_radius` 450. Valt altijd ruim binnen ring 1 (grens ≥ ~1.400 incl. ruis en overgangszone).

### 4. Aparte `ring_blend` voor de initiële dichtheid
- Nieuw: `density_function/ring_blend/overworld_initial.json` — `ring_blend` met `ring_1` → `corruptedadventure:ring_1/overworld/initial_density`; `ring_2…5` en `void` inline `{add 0.390625, ring_N/overworld}` (gelijk aan het huidige gedrag).
- Reden: vanilla's initiële dichtheid heeft zijn eigen drempel (0,390625) en bevat geen grotten/jaggedness. `0.390625 + final_density` zou in ring 1 het preliminary surface onder grotplafonds leggen (aquifers, `above_preliminary_surface`) en is duur (de surface-zoektocht rekent niet-geïnterpoleerd per kolom).

### 5. Noise settings `noise_settings/overworld.json`
- `noise_router.continents` → `corruptedadventure:ring_1/overworld/continents`
- `noise_router.depth` → `corruptedadventure:ring_1/overworld/depth` (klimaat-`depth` voor grotbiomes/deep dark en de deep-dark-regel van de aquifer blijven zo consistent met het terrein)
- `noise_router.initial_density_without_jaggedness` → `corruptedadventure:ring_blend/overworld_initial`
- `final_density` blijft `ring_blend/overworld`
- **`spawn_target`** → één punt met alle klimaatparameters `[-2, 2]` (grootste bereik van de codec), `depth` 0, `offset` 0. Dan is de klimaat-fitness overal 0, wint 0,0 (strikte vergelijking), en kiest vanilla's 11×11-chunk-spiraal de eerste droge, vaste plek rond 0,0 (max. ~96 blokken per as). De boost garandeert dat daar land is. Geen Java nodig.
- Fallback, alleen als test W toch een spawn in/naast water geeft: listener op `LevelEvent.CreateSpawnPosition` (alleen `ring_noise`-werelden) met een eigen spiraal vanaf 0,0 over een groter gebied. Niet vooraf bouwen.

### 6. Opruimen
- Placeholder-ring 1 overworld verdwijnt (vervangen in stap 1). `noise/placeholder_terrain.json` blijft (ring 2–5).
- Backlog-item "Vanilla kiest het spawnpunt op het klimaat…" verwijderen (opgelost).

### 6b. Alleen als de meting boven budget zit: ring gate
- `RingBlend.mapAll` geeft aan elke ring-invoer een visitor die elke `interpolated`-marker wikkelt in een gate: buiten de ring (inner/outer van `RingColumnCache.ringAt`) plus een marge van één cel (≥ 8 blokken) rond de overgangszone → 0, anders de echte waarde. De marge voorkomt naden (interpolatie tussen gerekende en 0-hoeken valt dan buiten het gebied waar de ring gewicht heeft).
- Unit test: binnen ring + marge gelijk aan de ongegate waarde; erbuiten 0. Opnieuw meten (budget) en W-overgang 1→2 extra controleren op naden.
- Binnen budget: niet bouwen, alleen het backlog-item uit de beslisregel.

### 7. Documentatie
- `CODEBASE.md`: `RadialFalloff`, registry `radial_falloff`, nieuwe bestanden `ring_1/overworld/*`, `ring_blend/overworld_initial`, gewijzigde router-velden en `spawn_target` in de beschrijving van `noise_settings/overworld.json`; valkuilen: (a) vanilla worldgen-JSON staat alleen in de client-extra-resources-jar, (b) continentalness zit in router én splines, (c) `spawn_target` = spawn altijd rond 0,0, (d) meetresultaten van de performancemeting (zie Tests).
- `fase-1-wereld.md` §2 Ring 1 (Terrein): uitwerking change 9 (keten, boost-formule en waarden, spawn-regel); §1.15-tabel: ring 1 → "vanilla (change 9)".
- `02-beslissingenlog.md` (datum 2026-09-30): boost ondergrens 0,03 binnen 300 → vanilla bij 450 + formule; rivieren in de landzone toegestaan; spawn via `spawn_target` (niet via event); straal in JSON i.p.v. config; aparte initiële `ring_blend`; performance-beslisregel (≤ 1,3× vanilla, anders gate) en de uitkomst ervan.
- `docs/backlog.md`: spawn-item weg; bij meting binnen budget het ring-gate-item erbij.

## Client/server-impact

Puur server-side worldgen, deterministisch per seed (seeded vanilla-noise + een pure afstandsfunctie). Geen sync, geen netwerk, geen client-class. `RadialFalloff` is common code (registry `DENSITY_FUNCTION_TYPE`, niet naar de client gesynct). Het spawnpunt is vanilla-staat (`level.dat`) en gaat via vanilla-packets naar de client. Werelden van vóór change 9 krijgen naden op de grens oud/nieuw terrein: altijd een nieuwe wereld gebruiken.

## Tests

Categorie-tests: **B**, **W**; S draait impliciet mee (alle W-checks via `runServer` + RCON). Geen datagen-wijziging, dus geen D.

### B
- `./gradlew build` groen, incl. nieuwe `worldgen/densityfunction/RadialFalloffTest`: 1 op en binnen `inner_radius`, 0 op en buiten `outer_radius`, 0,5 halverwege, monotoon, codec weigert `inner ≥ outer` en negatieve waarden.
- JSON-controle uit stap 1 (terugvervangen = vanilla).

### W — spawn over 10 seeds (server + RCON, zie valkuilen in `CODEBASE.md`)
Seeds: `20260930`, `1`, `2`, `42`, `12345`, `-1`, `987654321`, `-20260930`, `8675309`, `314159`. Per seed een nieuwe `level-name`. Tijdelijk RCON-script in `build/` (niet committen). RCON-commando's draaien op de world spawn (`RconConsoleSource` staat op `getSharedSpawnPos`):
1. `ca ring` → ring 1, afstand tot 0,0 **≤ 136** (11×11-chunkspiraal).
2. `execute if block ~ ~-1 ~ minecraft:water` en `… minecraft:lava` → beide *failed*; `execute if block ~ ~ ~ minecraft:air` → *passed*.
3. `execute positioned 0 64 0 run locate biome corruptedadventure:ocean` (en `deep_ocean`) → dichtstbijzijnde oceaan **≥ 300** blokken.
4. Landstraal: `forceload add` op 8 richtingen × r = 100/200/300; per punt `execute positioned <x> 0 <z> positioned over world_surface if block ~ ~-1 ~ minecraft:water` → noteer; verwacht vrijwel overal land (rivieren toegestaan, besloten bij vraag 2). Daarna `forceload remove all`.
Resultaat per seed in een tabelletje in de PR/commit-notitie.

### W — ring 1-inhoud (testseed `20260930`)
- `locate biome` vanaf 0,0: `corruptedadventure:ocean`, `deep_ocean`, `jagged_peaks` (of andere piek), `deep_dark` → alle gevonden binnen ~1.500 (andere ringen hebben placeholders, dus een vondst ligt per definitie in ring 1).
- Op elke vondst (`/tp` + F3): terrein klopt bij de biome (oceaanbodem diep, piek hoog, deep dark diep onder de grond met sculk-biome).

### W — standaard (runClient, testseed `20260930`) — checklist
- [ ] Nieuwe wereld, preset `corrupted_world`: spawn op land, binnen ~100 blokken van 0,0.
- [ ] Rondkijken rond spawn: geen oceaan binnen ~300 blokken; kust/strand verder weg ziet er natuurlijk uit (geen rechte of cirkelvormige kustlijn, geen trap in het terrein bij r ≈ 300–450).
- [ ] Vanilla-gevoel in ring 1: bergen, rivieren, grotten (spaghetti/cheese/noodle), aquifers, ore veins, dripstone/lush caves.
- [ ] `/ca tpring 1`…`6` en `/ca tpboundary 1`…`5` (overworld én `nether`): elke ring bereikbaar, landingshoogte klopt.
- [ ] Grens 1→2: vanilla-terrein mengt over ~128 blokken naar placeholder ring 2 (Y ~95); steile hellingen mogen, geen verticale wand, geen zwevende grotresten of waterwanden.
- [ ] Portal vanuit elke ring (1–5) naar de Nether en terug: landt in de overeenkomstige ring (÷4).
- [ ] Villages (tags uit change 8) staan netjes op het terrein (beardifier).
- [ ] Geen fouten/waarschuwingen in de log bij het aanmaken van de wereld.

### Performancemeting (verplicht; `CODEBASE.md`-valkuil: alle `interpolated`-inputs van alle ringen worden in elke chunk gevuld)
Doel: vaststellen wat de vanilla-invoer kost in chunks van ring 2–5 (waar hij niet gebruikt wordt) en in ring 1 (waar de placeholders van 2–5 meelopen).
- **Werelden** (zelfde seed `20260930`, zelfde machine, server met RCON, verder niets draaiend):
  - A: stand vóór change 9 (na change 8),
  - B: na change 9,
  - C: vanilla-preset (`minecraft:normal`) als referentie.
- **Per wereld:** `jfr start` (vanilla, RCON = permissieniveau 4) → `forceload add` op twee blokken van 16×16 chunks: ring 1 (rond x = 750, z = 0) en ring 3 (rond x = 4.500, z = 0) → wachten tot beide geladen zijn (`execute if loaded` op de hoeken pollen) → `jfr stop`.
- **Uitlezen:** `jfr print --events minecraft.ChunkGeneration run/debug/<bestand>.jfr` → per status (vooral `minecraft:noise`, ook totaal) gemiddelde, mediaan en p95, gefilterd op `chunkPosX/Z` per blok (spawn-chunks eruit). Elke meting 2× (warm/koud JIT: tweede telt).
- **Budget (besloten, vraag 3):** noise-stap in ring 1 én ring 3 van wereld B ≤ **1,3×** die van C. Verwachting: B-ring 3 ≈ vanilla + placeholders, B-ring 1 ≈ vanilla + placeholders (dus rond 1,1–1,2×).
- **Beslisregel:** binnen budget → geen gate in deze change; backlog-item `[worldgen]` "ring gate bouwen bij de eerste zware ring 2-invoer (change 23)", met de meetgetallen. Boven budget → gate bouwen in deze change (stap 6b in het plan), daarna opnieuw meten.
- Resultaten (getallen) in `CODEBASE.md` onder de density-function-valkuil, zodat latere ringchanges een referentie hebben.

## Risico's / aandachtspunten

- **Kosten stapelen per ring.** Vandaag is alleen ring 1 zwaar; met echte ring 2–5-terreinen rekent elke chunk straks 5 terreinen. Een "ring gate" (bij `RingBlend.mapAll` elke `interpolated` wikkelen in een check die buiten de ring + marge 0 teruggeeft) lost dat op, maar vraagt een marge rond de overgangszone om naden te voorkomen (interpolatie tussen gerekende en 0-hoeken). Besloten: meten en beslisregel (vraag 3, stap 6b).
- **Mengen van verschillende dichtheidsschalen** in de overgangszone 1→2: vanilla (`squeeze`, ×0,64, grotten) vs placeholder-gradient. Kan losse grotresten of overhangen in de zone geven; ring 2-terrein (change 23) en het overgangsbiome (change 11) herzien dit. Visueel checken.
- **Bergen aan de grens:** een piek (Y ~200+) vlak voor grens 1 zakt over 128 blokken naar Y ~95: steil. Hoort bij placeholder-ring 2, geen actie nu.
- **JSON-kopieën lopen niet mee met vanilla-updates** (MC-versie vast op 1.21.1; bij een upgrade het script opnieuw draaien — noteren in `CODEBASE.md`).
- **Spawn op bladeren/boomtop** kan (vanilla-gedrag van `getSpawnPosInChunk`): accepteren.
- **Rivieren** blijven mogelijk binnen de landzone (weirdness/ridges ongemoeid); de spawn-spiraal mijdt water. Besloten zo (vraag 2).
- `spawn_target` breed maken schakelt vanilla's voorkeur voor "geen rivier/vallei" uit; bewust: 0,0 is het midden van de ringen.
- Geen `RingLayout.current()` in de nieuwe code; `RadialFalloff` is stateless en thread-safe.

## Ontwerpvragen (beantwoord 2026-09-30)

**1. Hoe sterk is de landzone rond spawn?** (ondergrens continentalness en straal)
- a) *(aanbevolen)* Ondergrens **0,03** ("mid-inland") volledig binnen **300** blokken, glijdend naar vanilla tussen 300 en 450. Gewone landbiomes rond spawn; kust/zee kan vanaf ~300–450 opduiken. Speelgevoel: vertrouwd vanilla-begin, zee binnen loopafstand.
- b) Ondergrens **0,3** ("far-inland") binnen 300, glijdend tot 500. Meer heuvels, plateaus en bergen rond spawn, zee pas verder weg; spawn kan op een bergflank liggen. Iets "zwaarder" begin.
- c) Ondergrens **−0,11** (net geen kust) binnen 300, glijdend tot 400. Vaak strand/kust in zicht, maritiem begin; het midden kan als een groot eiland voelen.

Antwoord: a) — ondergrens 0,03 binnen 300 blokken, glijdend naar vanilla tussen 300 en 450 (2026-09-30)

**2. Rivieren in de landzone rond spawn?**
- a) *(aanbevolen)* **Toegestaan** (alleen continentalness aangepast, zoals besloten). Vanilla-gevoel; de spawn-spiraal kiest altijd een droge plek. Geen extra werk.
- b) **Geen rivieren binnen ~150 blokken**: ook `ridges`/weirdness bijsturen. Meer JSON-kopieën (ridges, ridges_folded en alles wat ze gebruikt), vlakker terrein rond spawn en verschoven biomevarianten (weirdness kiest ook varianten). Meer complexiteit, minder vanilla.

Antwoord: a) — rivieren toegestaan, alleen continentalness aangepast (2026-09-30)

**3. Wat als de vanilla-invoer chunks te traag maakt?** (alle `interpolated`-inputs worden in elke chunk gevuld)
- a) *(aanbevolen)* **Meten en beslisregel:** binnen budget (noise-stap ≤ 1,3× vanilla) → nu geen gate, wel een backlog-item om de "ring gate" te bouwen bij de eerste zware ring 2-invoer (change 23); boven budget → gate in deze change. Houdt change 9 klein als het kan.
- b) **Gate nu altijd bouwen.** Schaalt meteen voor alle ringen; meer Java in `RingBlend` en risico op naden aan de zonerand (marge nodig), extra testwerk in deze change.
- c) **Niet gaten, accepteren.** Minste werk; in ring 5 rekent elke chunk straks vijf terreinen (merkbaar trager pregen/verkennen, vooral op servers).

Antwoord: a) — meten; ≤ 1,3× vanilla → geen gate nu + backlog-item voor change 23; erboven → gate in deze change (2026-09-30)
