# Golf 2 — stroom A (sfeer): 50-0 (voorpakket), 50 ring 3-sfeer, 56 Nether-3-sfeer

Scope: `atmosphere` — één commit per pakket, branch `ring3/atmosphere-g2`, poort 5 (server 25575, RCON 25585). Opgesteld 2026-10-08 door de plan-agent; **nog niet gesparred** (zie "Open vragen").

**Doel:** ring 3 en Nether-3 krijgen hun definitieve sfeer:
- lucht- en fogkleuren per biome;
- biome-particles (as, vonken, stoom, ijsglinstering);
- ambient-geluid (gerommel, vonken, lava, titanen-zoem, krater, wind, krakend ijs);
- het onregelmatige gerommel met lichte camera-shake buiten het weer;
- eigen gesynthetiseerde audio (`Ring3Sounds.java`), ook voor het ringweer (nu vanilla-verwijzingen) en de blokgeluiden van 46.

Teststop 4b beoordeelt "sfeer en geluid".

**Bron:**
- `fase-1-wereld.md` Ring 3 ("Terrein en sfeer", wow-moment grens 2→3, Horror: gerommel en trillingen), Nether-3 "Sfeer", §1.4/§1.6, "Geluid (productiemethode, change 32)";
- `contract.md` §6 (sound events, particle types);
- `golf-1-atmosphere.md` (45: `Rumble`, `RingWeatherFog`, `WeatherParticles`, `WeatherLoop`, mixin luchtkleur);
- backlog r. 182, 188, 201, 202;
- sjablonen `BiomeAmbience` (paletten, moods), change 32/36 (`Ring2Sounds.java`, `oasis`-additions, `drip_loop`), `tools/sounds/Ring2Sounds.java`.

## 1. Pakketten

### 50-0 — Voorpakket golf 2 (op main, sequentieel, vóór de andere stromen afsplitsen; ~1 uur)
Voorstel van de plan-agent (procesvraag voor main): net als golf 0 een klein, gedrag-neutraal fundament, zodat W-biomes, W-nether en B niet op A hoeven te wachten en niemand in andermans bestand schrijft.
1. **Haken in `BiomeAmbience`:** `ring3(ResourceKey<Biome>)` en `netherRing3(ResourceKey<Biome>)` geven de effecten (palet + later particles/geluid) per biome.
   - In 50-0 alleen de huidige voorlopige kleuren: `Ring3Biomes.PALETTES` verhuist hierheen (gedrag-neutraal: identieke biome-JSON); Nether-3 krijgt een voorlopig blauwwit palet.
   - `Ring3Biomes` roept de haak aan (één regel; met akkoord van W-biomes via main, vóór W-biomes afsplitst).
2. **Registraties** (vanilla-verwijzingen in `sounds.json`, zodat alles meteen werkt):
   - sound events `ambient.ring_3.rumble`, `.embers`, `.lava`, `.titan_hum`, `.caldera`, `ambient.nether_ring_3.wind`, `.ice_creak`, `block.fumarole.puff`, `block.geyser.burst`, `block.boiling_mud.bubble`;
   - particle types `steam_puff`, `ice_glint` met de bestaande sprites van T (57a) en een eenvoudige provider.
3. **Tests:** `runData` geeft voor de biomes **geen diff** (bewijs gedrag-neutraal); B/D groen.

### 50 — Sfeer ring 3 (overworld)
1. **Kleuren per biome** (G2-A2): lucht, fog, (grasbladkleur van `dormant_caldera`) definitief in `BiomeAmbience.ring3`.
   - Het omslaan van de lucht op de grens 2→3 blijft het wow-moment; zone 2→3 mengt via vanilla's biome-blend.
2. **Biome-particles** (`ambient_particle` in de biome-data; dichtheid G2-A3):
   - `ashen_wastes` `ash_flake`;
   - `ember_woods` `ember` (opstijgende vonken);
   - `sulfur_fields` en `steam_fens` `steam_puff`;
   - `volcanic_peaks` as + vonken;
   - `titan_fields` vanilla `soul` (zielvonkjes);
   - `basalt_flows` sporadisch vonken;
   - `dormant_caldera` niets of zachte pollen;
   - `dried_seabed` stof.
3. **Ambient-geluid** (biome-data: `ambient_sound` loops, `mood_sound`, `additions_sound`):
   - `ambient.ring_3.embers` (knisperen, smeulwoud);
   - `.lava` (borrelen, als additions bij pieken/stromen);
   - `.titan_hum` (laag koor/zoem, titanenveld; G2-A2-detail);
   - `.caldera` (slapende krater: rust, zachte wind, een enkel insect; geen vogels, want ring 3 heeft geen dieren behalve geiten).
4. **Gerommel en camera-shake buiten het weer** (`ambient.ring_3.rumble`): client-only, per speler willekeurig, bewust niet gesynct (design). Frequentie en sterkte: G2-A4.
   - Hergebruik `Rumble` (45); "Screen Effects" schaalt de schok.
   - Alleen in ring 3, sterker in `volcanic_peaks` (de client kent de biome, niet de vulkaanposities).
5. **Rookpluim van de vulkanen** (design "kegels met gloed en een rookpluim"): G2-A1. Wordt het A, dan een eigen sub-pakket `50b` met een payload (INT verhoogt `ModNetwork.PROTOCOL_VERSION`).
6. **`tools/sounds/Ring3Sounds.java`** (single-file, alleen JDK + ffmpeg, vaste seed; patroon `Ring2Sounds.java`):
   - eigen audio voor de ambient-events, de drie weertypes van 45 (backlog r. 202) en de drie blokgeluiden;
   - `.ogg` onder `sounds/ambient/ring_3/`, `sounds/weather/ring_3/`, `sounds/block/ring_3/`;
   - de verwijzingen in `ModSoundDefinitionsProvider` wisselen van vanilla naar eigen;
   - valkuil 36: alleen de nieuwe `.ogg`'s committen (het script herschrijft ook oude bytes).
7. **Ondertitels** (`Ring3WeatherText`, en + nl) voor alle nieuwe events.
8. **`steam_puff`-tint:** afspraak met T (backlog r. 182). Voorstel: lichte grijswaarden getint in de particle-klasse, zoals `ash_flake`/`ember`; zwavelvelden iets geelgroen.

### 56 — Sfeer Nether-3
1. **Kleuren** per Nether-3-biome (`BiomeAmbience.netherRing3`): blauwwitte fog, kouder in `glacier_rifts`, iets violet in `rime_forest`.
   - Fog-afstand zit niet in biome-data (valkuil 36): dichter alleen via een client-hook (G2-A5).
2. **Particles:** `ice_glint` (zwevende ijskristallen) per biome; dichtheid G2-A3.
3. **Geluid:** `ambient.nether_ring_3.wind` (loop, `stream`) en `.ice_creak` (additions); `mood_sound` passend.
4. **Extra kou-sfeer** (G2-A5): adem-wolkjes en/of vriesmist.

## 2. Bestanden
- **Eigen:**
  - `ModSoundEvents`, `ModSoundDefinitionsProvider`, `ModParticleTypes` (+ providers), `ModParticleDescriptionProvider`;
  - `client/particle/*` (`SteamPuffParticle`, `IceGlintParticle`), `client/weather/*` (ring 3) en eventueel `client/ambient/*` (gerommel buiten het weer, pluim, adem);
  - `BiomeAmbience` (ring 3- en Nether-3-builders), `tools/sounds/Ring3Sounds.java`, `assets/corruptedadventure/sounds/**` (ring 3), `Ring3WeatherText`;
  - `ServerConfig` `[weather.ring3]` (als er iets bijkomt).
- **Netwerk (alleen bij G2-A1 = A):** een nieuwe payload in `network/` + registratie; INT verhoogt de protocolversie.
- **Raakt niet:** `Ring3Biomes`/`NetherRing3Biomes` behalve de ene haakregel in 50-0 (akkoord via main), `ModBlocks`/blokklassen (B koppelt de blokgeluiden).

## 3. Afhankelijkheden
- **→ B (46-rest):** wacht op de sound events + `steam_puff` (50-0).
- **→ W-biomes/W-nether:** bouwen vanaf 50-0 tegen de haken. De definitieve effecten komen bij de merge van 50/56 vanzelf mee, omdat de haak in `BiomeAmbience` zit.
- **↔ T:** sprites van `steam_puff`/`ice_glint` bestaan (57a); tint afspreken (backlog r. 182).
- **← W-terrein/W-biomes** (alleen voor G2-A1 = A): de pluim-payload leest vulkaanposities uit de pure `VolcanicField`-beslissingen (alleen lezen, server-side). Een publieke accessor voor "vulkanen binnen R" via main aan 39f/W-terrein.

## 3b. Besluit 39f: aswolken
De wolken in ring 3 krijgen een donkere askleur (besluit gebruiker 2026-10-08 bij 39f; de zwevende eilanden zakken onder de wolkenlaag Y 192, alleen de kegel van de zwevende vulkaan steekt erdoor). Hoort bij pakket 50: client-only (wolkenkleur per ring, vloeiend over de grens zoals de lucht), geen payload nodig als de ring al client-side bekend is.

## 4. Open vragen
Aanbevolen optie eerst. **C** = complexiteit, **MP** = multiplayer, **S** = speelgevoel, **P** = prestatie.

**G2-A1 — Rookpluim boven de vulkanen (design: "met gloed en een rookpluim"): hoe?** Eerlijk over 1.21.1: de client kent de seed niet, dus ook geen vulkaanposities. Gewone particles verdwijnen al na ~32 blokken, en `animateTick` draait alleen dichtbij.
- **A. De server stuurt elke speler in ring 3 de vulkanen binnen ~512 blokken (één kleine payload bij binnenkomst en elke ~10 s); de client tekent een pluim als "altijd zichtbare" rook-particles boven de krater tot ~256 blokken ver, dikker bij een uitbarsting (aanbevolen).** C middel-hoog (payload + client-renderer, protocolversie omhoog). MP: per speler, licht verkeer. S: het horizon-wow vanuit `dry_riverbeds` is af. P client: particle-budget per pluim begrenzen.
- **B. Pluim alleen dichtbij (vent-particles op de kraterrand, zichtbaar tot ~32–64 blokken).** C laag. S: dichtbij sfeervol; vanaf de horizon geen pluim.
- **C. Geen pluim; de gloed van lavameren en smeulsteen is genoeg.** C geen. S: het ontwerpbeeld "rookpluim" valt weg.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A2 — Kleurrichting per biome?**
- **A. Eén asrood ringpalet met een duidelijke eigen tint per biome: as grijsbeige, pieken rood-oranje, smeulwoud warm amber, zwavel geelgroen, stromen donker roodbruin, zeekom bleek zandblauw, titanen koel violet (zielvuur-contrast), slapende krater helderder en groener (aanbevolen).** C laag. S: elk biome herkenbaar, de ring blijft één geheel.
- **B. Eén ringpalet met alleen kleine nuances (zoals nu in 39b).** C laag. S: rustig, maar biomes lopen in elkaar over.
- **C. Sterke contrasten (elk biome een eigen lucht).** C laag. S: maximaal herkenbaar; harde, soms storende grenzen.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A3 — Hoeveel biome-particles (as, vonken, stoom, zielvonkjes, ijsglinstering)?**
- **A. Subtiel tot middel (zoals basalt deltas/soul sand valley-licht), dichter in smeulwoud (vonken) en `steam_fens` (stoom) (aanbevolen).** C laag (biome-data). P: client, valt weg bij Particles: Minimal. S: levendig zonder zicht te ontnemen; het weer blijft het piekmoment.
- **B. Dicht overal (zoals crimson forest).** C laag. S: sterk, maar de asregen valt minder op; meer belasting op zwakke clients.
- **C. Alleen in 3–4 biomes (smeulwoud, zwavel, titanen, `steam_fens`).** C laag. S: rust in de andere biomes.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A4 — Gerommel en camera-shake buiten het weer: hoe vaak en hoe sterk?** (client-only, per speler willekeurig)
- **A. Elke 2–5 min een diep gerommel, 1 op 3 met een lichte schok; vaker en sterker in `volcanic_peaks`; schok volgt "Screen Effects" (aanbevolen).** C laag (`Rumble` hergebruiken). S: onrustig, nooit vervelend.
- **B. Vaker (30–90 s).** C laag. S: constante dreiging; kan gaan irriteren.
- **C. Alleen gerommel, geen schok buiten een uitbarsting.** C laag. S: rustiger, minder voelbaar.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

**G2-A5 — Extra kou-sfeer in Nether-3?**
- **A. Adem-wolkjes bij spelers en mobs (client-side, kleine witte puf elke ~2 s), dwarrelende ijsglinstering en wind + krakend ijs (aanbevolen).** C laag/middel (client-tick in `client/`). MP: elke client tekent zelf, voor alle spelers in beeld. S: je voelt de kou zonder HUD.
- **B. Alleen glinstering + wind/kraak (biome-data).** C laag. S: sfeervol, minder lijfelijk.
- **C. A + vriesmist (dichtere fog via een client-hook, zoals bij het ringweer).** C middel. S: beklemmend; de grote hallen worden minder zichtbaar.
- Antwoord: A (aanbevolen; gebruiker 2026-10-08, "alles aanbevolen")

## 5. Client/server-impact
- **Client-only** in `client/`: particles, gerommel/shake, adem, pluim-renderer, eventuele fog-hook. `ClientIsolationTest` bewaakt dat.
- **Data:** biome-effecten en `sounds.json`; de client rendert.
- **Server:** alleen bij G2-A1 = A (vulkanen binnen R per speler, payload); verder geen.
- Twee clients zien hetzelfde weer; gerommel en shake mogen per client verschillen (design).

## 6. Tests
- **B:** `./gradlew build` groen (unit tests, `ClientIsolationTest`).
  - Tests: `BiomeAmbienceTest` (+ ring 3/Nether-3-paletten, ieder biome een eigen palet, 50-0 gedrag-neutraal), payload-codec (G2-A1). `Ring3Sounds.java`: 2× draaien geeft dezelfde PCM (de ffmpeg-bytes mogen verschillen, valkuil 36).
- **D:** `runData` groen (sounds, particles, lang). Voor 50-0: geen biome-diff.
- **S:** server (25575) + 2 clients (quick-play; `options.txt` met `onboardAccessibility:false`, `skipMultiplayerWarning:true`).
  - Server start zonder client-class-fouten.
  - Per biome kleuren, particles en geluid.
  - Gerommel en shake per client verschillend.
  - Particles: Minimal → weinig particles, kleuren en geluid blijven.
  - FPS in smeulwoud 's nachts en in `steam_fens`.
  - Pluim (G2-A1) vanuit `dry_riverbeds`.
- **Review:** "klaar voor review-doorsturen: <pakket>" aan main; main draait `/code-review medium`; daarna committen.

## 7. W-checklist (voor 4b; dedicated server + 2 clients, seed `20260930`)
- [ ] `/ca tpboundary 2`, oostwaarts: de lucht slaat om; vanaf de grens rookpluimen boven de horizonreuzen (als G2-A1 = A).
- [ ] Per biome 1 min staan (coördinaten in `golf-2-biomes.md` §7): eigen lucht- en fogtint, eigen particles en geluid. Smeulwoud (4.750, 0) 's nachts: vonken en knisperen. Titanenveld (4.942, 160): zielvonkjes + titanen-zoem. Slapende krater: rust.
- [ ] Gerommel met lichte schok, vaker op de pieken (4.110, 672); "Screen Effects" 0 = geen schok.
- [ ] Eigen geluiden voor asregen, uitbarsting en aswolk (`/ca weather 3 …`), fumarole, geiser en modder.
- [ ] Nether-3 (`/ca tpring 3 nether`): blauwwitte fog, ijsglinstering, wind en krakend ijs; adem-wolkjes (als G2-A5 = A).
- [ ] Particles: Minimal en FPS acceptabel.
- [ ] **50b (rookpluim; S niet gedaan in de stroom, lichte testafspraak 2026-10-09 → INT2):** dedicated server + **2** clients (quick-play `-PquickPlay=localhost:<poort>`): server start zonder client-class-fouten; client joint (protocolversie 6 aan beide kanten).
- [ ] 50b: vanuit `dry_riverbeds` (`/ca tpboundary 2`, oostwaarts) pluimen boven de horizonvulkanen; beide clients zien dezelfde pluimen op dezelfde plek; geen pluim boven slapende kraters; de zwevende vulkaan heeft een pluim vanaf zijn kraterrand (~205–225).
- [ ] 50b: `/ca weather 3 eruption`: pluimen dikker, sneller, rode gloed aan de voet; daarna weer gewoon.
- [ ] 50b: naar ring 1 of de Nether en terug: pluimen verdwijnen en komen terug (lege lijst / nieuwe lijst); uitloggen en opnieuw joinen zonder oude pluimen.
- [ ] 50b: Particles: Minimal → dunnere pluimen (~0,35×); FPS acceptabel met 8 pluimen in beeld (pieken (4.110, 672)).
- [ ] 50b (review 1): pluimen zichtbaar tot 512 blokken; lopend over 160–288 blokken geen sprong in de pluim (LOD: minder, grotere wolken); een pluim die in beeld komt (grens oversteken, uit de Nether terug, 9e → 8e dichtstbij) staat er meteen als volle kolom, niet groeiend vanaf de voet. **Let op:** voorbij de render distance kleurt vanilla's terreinmist ook particles; daar is de pluim een silhouet in mistkleur. Beoordelen bij render distance 12 en 32.
- [ ] 50b (review 5): vorm: een kolom van ~80 (kegel) / ~110 (grote vulkaan) blokken die bovenin uitwaaiert en hooguit ~10 blokken met de wind meedrijft, geen wolkenbank; bij een uitbarsting hoger (~1,5×).

## Voortgang
Worktree: `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-afb7325c2a1a62d1d`, branch `ring3/atmosphere-g2` (vanaf main `dbb1cb9`).

Voortgang: 2026-10-08 start 50-0 (haken `BiomeAmbience`, sound events, particle types)
Voortgang: 2026-10-08 14:30 50-0 gebouwd en getest (B groen incl. `ClientIsolationTest`; D: geen biome-diff; S: server start, ids bekend); klaar voor review
Voortgang: 2026-10-08 50-0 review (medium, 9 bevindingen) verwerkt: 1–7 opgelost, 8 vastgelegd (delta), 9 bewust (waves §4.8); B/D opnieuw groen, geen biome-diff
Voortgang: 2026-10-08 15:10 50-0 gecommit (`9928e8e`)
Voortgang: 2026-10-08 15:45 50 gebouwd en getest (B groen, BiomeAmbienceTest 15, ClientIsolationTest; D: biomes + sounds.json; `Ring3Sounds` 2× identieke WAV's (28); S: server + 1 client, F2-foto's per biome, wolken ring 2 / zone 2→3 / ring 3); klaar voor review
Voortgang: 2026-10-08 16:30 review 50 (medium, 10 bevindingen): 1–9 opgelost (gedeelde timer, uitdempende schok, hoeken gelijk aan de docs, binnen/ondergronds gedempt, ambient-particle-types i.p.v. biome-lookup en snelheid-0-regel, één `shares`-lookup, `RingWeatherMix.weight`, ring 2-helpers delegeren), 10 bewust (waves §4.8)
Voortgang: 2026-10-08 16:45 50 gecommit (`fc441d4`)
Voortgang: 2026-10-08 17:00 56 gebouwd (B groen: BiomeAmbienceTest 15, ClientIsolationTest; D: sounds.json)
Voortgang: 2026-10-08 16:36 56 getest (S: server + 1 client: adem-wolkjes bij piglin en strider in Nether-ring 3, niet bij de zombified piglin (undead); de drie nieuwe ambient-types van 50 zichtbaar in smeulwoud, asvlakte en zwavelvelden; geen fouten). Nether-3-biome-effecten pas zichtbaar na 53b (W-nether); klaar voor review
Voortgang: 2026-10-08 16:45 review 56 (medium, 8 bevindingen) verwerkt: 1–7 opgelost (ademers-whitelist, spectator, eigen speler in first person, hoofd in lava/poedersneeuw, geen camera-gate meer, fasecheck eerst, test op de echte effects via de codec), 8: de herschreven tijden zijn teruggezet (log is append-only; de tijden 16:30/16:45/17:00 hierboven waren geschat, de werkelijke volgorde is 50-review → 50-commit → 56-bouw → 56-test); B/D groen; S opnieuw: server + 1 client, adem bij piglin en strider
Voortgang: 2026-10-09 start 50b (rookpluim): branch gerebased op main `91af98e` (39f + 46-rest); 50-0 viel weg (al op main), 50 → `7d6acfb`, 56 → `7033ace`
Voortgang: 2026-10-09 50b gebouwd (concepten naar `src/`, echte `VolcanoLocator` op `VolcanicField.volcanoesWithin`, registraties); B groen (`./gradlew build`, 593 tests incl. 11 nieuwe en `ClientIsolationTest`); D groen (`runData`, geen diff). Geen S/client/screenshots (lichte testafspraak gebruiker 2026-10-09; zie W-checklist → INT2); klaar voor review
Voortgang: 2026-10-09 review 50b (medium, 8 bevindingen): 1–6 opgelost (tekenafstand = serverstraal 512 met LOD en direct gevulde kolom; stabiele volgorde (x, z) voor de verandercheck; `near` alleen binnen 512 van ring 3 via `RingLayout.reaches`, spelers gespreid over de 40 ticks; bestaande lookup `RingChunkGenerator.volcanicField` (nu public) i.p.v. eigen `WeakHashMap`; stijging en drift apart gedempt; sprites één keer via de `steam_puff`-`SpriteSet`), 7–8 bewust (protocolversie en S/CODEBASE in INT2); B (`--rerun-tasks`, 595 tests) en D groen

## CODEBASE-delta
- **50-0:** `BiomeAmbience.ring3(ResourceKey<Biome>)` / `netherRing3(...)` → `BiomeSpecialEffects.Builder`: de haken waar de biome-bootstraps alle effecten vandaan halen; ze delegeren naar `worldgen.biome.Ring3Atmosphere` (per biome een `Look`: palet + gras + blad; 39b-kleuren ongewijzigd, + voorlopig `steam_fens`; mood `LEGACY_CAVE_SETTINGS`) en `NetherRing3Atmosphere` (voorlopig blauwwit palet voor `frozen_lava_sea`, `rime_forest`, `glacier_rifts`; mood idem). Onbekende biome → `IllegalArgumentException`. `BiomeAmbience.colours(palette)` (package-private). Alleen effecten: klimaat, spawns en generation blijven in de bootstrap.
- **50-0:** `Ring3Biomes` zonder `PALETTES`/kleurconstanten, roept `BiomeAmbience.ring3` aan via de nieuwe overload `Ring2Biomes.biome(effects, downfall, spawns, generation)` (heet en droog: temperatuur 2,0, geen neerslag). **`steam_fens` (48) is een natte overgang: W-biomes bouwt die met een eigen klimaat en neemt alleen de effecten uit `BiomeAmbience.ring3`.** W-nether (53b): hun `biome()` zet alleen `backgroundMusic` op `BiomeAmbience.netherRing3(key)`.
- **50-0:** sound events `ambient.ring_3.{rumble,embers,lava,titan_hum,caldera}`, `ambient.nether_ring_3.{wind,ice_creak}`, `block.{fumarole.puff,geyser.burst,boiling_mud.bubble}` (vanilla-verwijzingen in `sounds.json` tot de eigen audio van 50/56: rumble = `ambient.basalt_deltas.mood` pitch 0,6; embers = `ambient.basalt_deltas.loop` pitch 1,3; lava = `block.lava.ambient`; titan_hum = `block.beacon.ambient` 0,5; caldera = `item.elytra.flying` 1,2 / 0,15; wind = `ambient.soul_sand_valley.loop` 0,8; ice_creak = `block.powder_snow.break` 0,5; blokgeluiden = precies wat B nu speelt). Ondertitels (en + nl) voor de one-shots in de nieuwe lang-helper `datagen.lang.Ring3AmbienceText` (aangeroepen door beide lang-providers; `Ring3WeatherText` houdt alleen het weer).
- **50-0:** particle types `steam_puff`, `ice_glint` (`client/particle/SteamPuffParticle`: stijgt ook zonder snelheid, zwelt, botst; `IceGlintParticle`: zweeft, fonkelt, `LightTexture.FULL_BRIGHT`; sprites van T). `EmberParticle` gebruikt nu ook `LightTexture.FULL_BRIGHT`.
- **50:** `Ring3Atmosphere` definitief (G2-A2/A3 = A): kleuren per biome (as grijsbeige `9A8E88`/`847A74`, pieken rood-oranje `BC5838`/`94482F`, smeulwoud amber `B07440`/`84522E`, stromen roodbruin `7C5448`/`563C34`, zwavel geelgroen `AAAE62`/`A2A45E`, zeekom bleek zandblauw `A6B2BA`/`B4AA96`, titanen koel violet `8A7CA8`/`6A6290`, slapende krater groener `98C0AA`/`AAC6A2`, `steam_fens` `94A49C`/`A6B0AA`); ambient particle per biome (as `drifting_ash` 0,006; pieken `drifting_ash` 0,004; smeulwoud `rising_ember` 0,008; stromen `rising_ember` 0,0015; zwavel `sulfur_steam` 0,004; zeekom vanilla `white_ash` 0,004; titanen vanilla `soul` 0,0025; krater vanilla `spore_blossom_air` 0,002; `steam_fens` `steam_puff` 0,01); loops smeulwoud `embers`, titanen `titan_hum`, krater `caldera`; additions `lava` op de pieken (0,006) en stromen (0,004). `Look` = palet + gras + blad + particle + loop + additions.
- **50:** eigen audio: `tools/sounds/Ring3Sounds.java` (single-file, alleen JDK + ffmpeg, vaste seed per bestand, `StrictMath`; `--wav-only` zonder ffmpeg; schrijft alleen ring 3-bestanden) → 28 mono `.ogg`'s onder `sounds/weather/ring_3/` (`ash_rain_loop_1..2`, `eruption_1..3`, `ash_cloud_loop_1`), `sounds/ambient/ring_3/` (`rumble_1..3`, `embers_loop_1..2`, `lava_1..3`, `titan_hum_loop_1`, `caldera_loop_1`), `sounds/ambient/nether_ring_3/` (`wind_loop_1..2`, `ice_creak_1..3`; gebruikt in 56), `sounds/block/ring_3/` (`fumarole_puff_1..2`, `geyser_burst_1..2`, `boiling_mud_bubble_1..3`). Loops exact periodiek (sinussen op veelvouden van 1/lengte, events om het einde gevouwen), gestreamd, geen ondertitel. `ModSoundDefinitionsProvider`: ring 3-weer, -ambient en blokgeluiden naar de eigen bestanden (`file`/`loop`); Nether-3 nog vanilla tot 56.
- **50:** client: `client/weather/Ring3Ambience` (gerommel buiten het weer, G2-A4 = A: één timer die in ring 3 altijd doorloopt, ook bij wisselen van biome of weer; de biome op het moment van het gerommel kiest sterkte en volgende pauze: vlaktes 2–5 min, 1 op 3 schok 0,35°; pieken 1–2,5 min, 2 op 3, 0,6°; overgeslagen als een uitbarsting/aswolk gevoeld wordt (één `shares`-lookup); binnen/ondergronds zachter (× 0,5–1 via `SkyExposure`) en schok × blootstelling, dus geen schok zonder lucht; vonken op de pieken 0,6/tick via `WeatherParticles`); nieuwe `client/weather/CameraShake` (schok die uitdempt, × "Screen Effects"), ook gebruikt door `Rumble`; `client/weather/AshClouds` (wolkenkleur × `5C5652` naar rato van `RingWeatherMix.weight(position, RING_3)`, smoothstep over de zones) via een tweede `@Inject` op `ClientLevel#getCloudColor` in `ClientLevelSkyColorMixin` (nog steeds de enige mixin-klasse). `RingWeatherMix.weight(position, ring)` (common, zonder allocatie). Nieuwe particle types `drifting_ash`, `rising_ember`, `sulfur_steam` (ambient-varianten met eigen beweging resp. geelgroene tint `DDE0B0`; sprites van `ash_flake`/`ember`/`steam_puff`; providers met een vlag/tint in `AshFlakeParticle`, `EmberParticle`, `SteamPuffParticle`). `ModSoundDefinitionsProvider`: `netherFile`/`ring2File` delegeren naar `loop`/`file` (zelfde `sounds.json`).
- **56:** `NetherRing3Atmosphere` definitief (G2-A3/A5 = A): fog blauwwit `9CB0C4` (`frozen_lava_sea`), violet `A8A4C4` (`rime_forest`), kouder `88A8CC` (`glacier_rifts`); ambient particle `ice_glint` 0,004 / 0,006 / 0,005; loop `ambient.nether_ring_3.wind`, additions `ice_creak` (0,004), mood `LEGACY_CAVE_SETTINGS`; `Look` = palet + glinsterkans. Geen vriesmist (fog-afstand is geen biome-data; G2-A5 = A). `sounds.json`: wind = eigen `wind_loop_1..2` (gestreamd), krakend ijs = `ice_creak_1..3` (`.ogg`'s sinds 50); de vanilla-helper in `ModSoundDefinitionsProvider` is weg. Client: `client/weather/ColdBreath` (adem-wolkjes: elke ~2 s, eigen fase per entity-id, voor spelers en duidelijke ademers (`Animal`, `AbstractVillager`, `AbstractPiglin`, `AbstractIllager`; geen undead, slijmen, blazes, golems, armor stands) binnen 24 blokken die zelf in Nether-ring 3 staan; niet in spectator, onzichtbaar, met het hoofd in water/lava of in poedersneeuw, en niet de eigen speler in first person; loopt over `entitiesForRendering()` met de fasecheck eerst; vanilla `white_smoke` voor de mond via `addParticle`, dus de Particles-optie geldt).
- **50b:** rookpluim boven de actieve vulkanen (G2-A1 = A). Server: `weather/VolcanoPlumes` (geregistreerd in `RingWeather.register`): per speler in de overworld van een ringwereld één keer per 40 ticks (gespreid over de ticks op entity-id, `due`), alleen als ring 3 binnen 512 blokken ligt (`near`: `RingLayout.reaches(RING_3, effectiveDistance, reachMargin(512, 1))`, grenzen uit config), de actieve vulkanen binnen 512 blokken via `weather/VolcanoLocator` (het veld uit `RingChunkGenerator.volcanicField(randomState)`, **nu public**, thread-safe; de beslissingen zijn per vulkaancel gecachet in het veld; slapende vulkanen geen pluim; geen chunks). De 24 dichtstbijzijnde (`nearest`), in stabiele volgorde (x, z); alleen verstuurd als die lijst verandert (lege lijst bij vertrek, ook naar een andere dimensie), opgeruimd bij uitloggen. Payload `network/VolcanoPlumesPayload` (`volcano_plumes`, lijst van `Plume(x, z, y, radius, great)`, `VAR_INT`/`BOOL`, decoder/encoder weigeren > 24) in `ModNetwork.register`. **`ModNetwork.PROTOCOL_VERSION` blijft "5": integratie 2 verhoogt naar "6" (nieuwe payload).** Client: `client/network/VolcanoPlumesHandler` → `client/weather/VolcanoPlumeRenderer` (handler via `ClientboundHandlers.set`, renderer op `NeoForge.EVENT_BUS`; de `steam_puff`-`SpriteSet` krijgt hij bij de provider-registratie in `CorruptedAdventureClient`, de engine herbindt die bij elke reload): per client-tick de 8 dichtstbijzijnde pluimen tot 512 blokken (= serverstraal), 0,15 (kegel) / 0,25 (grote vulkaan) wolken per tick × Particles-optie (1 / 0,6 / 0,35) × (1 + uitbarsting van ring 3); LOD van 160 naar 288 blokken (smoothstep) naar 0,4× de wolken op 1,6× de grootte; een pluim die in beeld komt krijgt meteen een volle kolom (rate × 400 wolken, elk een willekeurig deel van hun leven vooruitgespoeld met `VolcanoPlumeParticle.prewarm`); leeggemaakt bij uitloggen. `client/particle/VolcanoPlumeParticle`: geen geregistreerd particle type, direct via `particleEngine.add` (zonder vanilla's 32-blokkenlimiet); stijging 0,4 / 0,55 per tick (× 1,5 bij uitbarsting) die per tick × 0,996 afneemt (kolom ~80–110 blokken die bovenin uitwaaiert), horizontaal × 0,985 plus wind die met de levensduur sterker wordt, drift begrensd op 0,05 per tick; asgrijs `3A3634` → `6E6864`, zwelt tot ~3×, levensduur 320–480 ticks, licht = alleen hemellicht (`0xF00000`), gloeiend deel volle helderheid; render-box vergroot met `quadSize`. Tests: `VolcanoPlumesPayloadTest` (rond-trip, 24 past, 25 geweigerd, eigen kopie), `VolcanoPlumesTest` (`near` rondom, stabiele `nearest`, cap, `due`), `VolcanoLocatorTest` (testveld van `VolcanicFieldTest`), `VolcanoPlumeRendererTest` (LOD zonder sprong).

## Beslissingen
- 2026-10-08, atmosphere: loops (embers, titan_hum, caldera, wind) zonder ondertitel, zoals vanilla-loops; one-shots met ondertitel.
- 2026-10-08, atmosphere: de sfeer van ring 3 en Nether-3 staat in eigen klassen (`Ring3Atmosphere`, `NetherRing3Atmosphere`) achter de haken in `BiomeAmbience` (review 50-0: niet alles in één bestand).
- 2026-10-08, atmosphere: particles per biome ~1.300 kansen per tick → 0,004–0,01 (subtiel–middel); `volcanic_peaks` krijgt as uit de biome-data en vonken van de client (een biome heeft maar één ambient particle).
- 2026-10-08, atmosphere: biome-particles die zelf moeten bewegen of een andere tint hebben, krijgen een eigen ambient-type (`drifting_ash`, `rising_ember`, `sulfur_steam`) in plaats van gedrag op snelheid 0 of een biome-lookup per particle (review 50).
- 2026-10-08, atmosphere: aswolken = vanilla-wolkenkleur × donker asgrijs `5C5652`, gemengd met het ring 3-aandeel (smoothstep over de zone), via een tweede inject in de bestaande mixin.
- 2026-10-09, atmosphere: rookpluimen worden getekend zo ver als de server zoekt (512 blokken), met LOD (minder, grotere wolken vanaf 160–288 blokken) en een direct gevulde kolom bij in beeld komen (review 50b).
- 2026-10-09, atmosphere: de pluim-check per speler is gespreid over de 40 ticks (entity-id) en alleen binnen 512 blokken van ring 3; het veld komt uit de bestaande lookup van `RingChunkGenerator` (review 50b).

## Backlog
- `atmosphere`: rookpluimen voorbij de render distance zijn door vanilla's terreinmist alleen een silhouet in mistkleur; als 4b dat te zwak vindt: een eigen render-pass zonder terreinmist voor de verre pluimen (50b).

## Status bij onderbreking
**Bijgewerkt 2026-10-09:** 50b gebouwd (zie Voortgang). Oorspronkelijk: afgesproken stop na 56 (main): een verse agent doet **50b (rookpluim, G2-A1 = A)** na de merge van 39f. Wat 50b moet weten:
- **Branch/worktree:** `ring3/atmosphere-g2` in `P:/Projecten/Java/The Corrupted Adventure/.claude/worktrees/agent-afb7325c2a1a62d1d`; gecommit 50-0 `9928e8e`, 50 `fc441d4`, 56 (zie log). Eerst rebasen/mergen op main met 39f.
- **Concepten (niet in git):** `build/draft50/src/main/java/nl/timtoys/corruptedadventure/` → `network/VolcanoPlumesPayload` (lijst `Plume(x, z, y, radius, great)`, max 24, `VAR_INT`/`BOOL`), `weather/VolcanoPlumes` (server: elke 40 ticks per speler bij ring 2–4, `VolcanoLocator` binnen 512 blokken, dichtstbij eerst, alleen versturen als de lijst verandert, leeg bij vertrek, opruimen bij uitloggen), `weather/VolcanoLocator` (stub `NONE`), `client/network/VolcanoPlumesHandler`, `client/weather/VolcanoPlumeRenderer` (8 dichtstbijzijnde tot 288 blokken, 0,15/0,25 wolken per tick, × Particles-optie, dikker + gloed tijdens een uitbarsting van ring 3), `client/particle/VolcanoPlumeParticle` (direct aangemaakt met een `steam_puff`-sprite uit de particle-atlas, geen registry; licht `0xF00000` = alleen hemellicht; render-box vergroot met `quadSize`). `build/draft50/after39f/VolcanoLocator.java` = de echte bron.
- **39f-signatuur (definitief, via main):** `VolcanicField.VolcanoInfo(int x, int z, int craterY, int craterRadius, int footRadius, boolean great, boolean dormant)` en `List<VolcanoInfo> volcanoesWithin(int x, int z, int radius)`; het veld via `VolcanicField.find(level.getChunkSource().randomState().router().finalDensity())`, per level cachen (het concept doet dat met een `WeakHashMap`). De zwevende vulkaan zit erin met `great = false`; zijn kraterrand zakt in 39f naar ~205–225 (niet op oude hoogtes afstemmen). Slapende vulkanen (`dormant`) krijgen geen pluim. `footRadius` is nog niet gebruikt (eventueel voor de breedte van de pluimvoet).
- **Nog te doen:** de concepten naar `src/` (met `Map`-import weg in de renderer), registreren: `ModNetwork.register` één regel `playToClient(VolcanoPlumesPayload…)`, `ClientboundHandlers.set(VolcanoPlumesPayload.TYPE, VolcanoPlumesHandler::handle)` en `VolcanoPlumeRenderer.register(NeoForge.EVENT_BUS)` in `CorruptedAdventureClient`, `VolcanoPlumes.register(gameBus)` (bijv. naast `SandstormWind.register` in `RingWeather.register`). **`ModNetwork.PROTOCOL_VERSION` niet ophogen** (INT; in de delta zetten). Tests: payload-codec (rond-trip, lijst > 24 geweigerd), `VolcanoPlumes.near`/`nearest`. S: server + **2** clients (quick-play: `runClient` en `runClient2` met `-PquickPlay=localhost:25575`; `run/client2/options.txt` staat klaar), pluimen vanuit `dry_riverbeds` (`/ca tpboundary 2`, oostwaarts), beide clients zien dezelfde pluimen, Particles: Minimal, FPS; daarna `enable-rcon=false`.
- **Hulpmiddelen in `build/` (niet in git):** `wait-slot.sh`, `wait-all.sh <n> <taak>` (lock + n slots tegelijk, nooit wachten met iets vast), `slot.sh status|release <n>`, `rcon.py <poort> <wachtwoord> <cmd>…`, `win.ps1 list|shot|key <Dev|Dev2> F2` (venster-hulp; F2 werkt, PrintWindow geeft wit), `tour.ps1 -Who Dev -Stops "naam:x:z;…"` (spreadplayers + F2 → `build/shots/<naam>.png`). RCON-wachtwoord in `run/server.properties`.
- **Niet beoordeeld:** het geluid (alleen spectrogrammen/niveaus) en Particles: Minimal/FPS: voor de gebruiker in 4b. De Nether-3-biome-effecten worden pas zichtbaar na 53b (W-nether) in de integratie.
